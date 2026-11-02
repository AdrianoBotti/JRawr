#!/usr/bin/env python3
"""Map IL closure types/members to source files; resolve provenance to Mage seeds."""
import json
import os
import re
import sys
from collections import defaultdict

ROOT = os.path.abspath(os.environ.get(
    "RAWR_LEGACY_DIR",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "legacy", "RAWR_WotLK_3.3.5a"),
))
TYPE_RE = re.compile(r"\b(?:class|struct|interface|enum|delegate)\s+([A-Za-z_]\w*)")
NS_RE = re.compile(r"\bnamespace\s+([\w.]+)")
PARTIAL_RE = re.compile(r"\bpartial\s+(?:class|struct|interface)\b")


def strip_comments_strings(text):
    out = []
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        nxt = text[i + 1] if i + 1 < n else ""
        if c == "/" and nxt == "/":
            j = text.find("\n", i)
            i = n if j < 0 else j
        elif c == "/" and nxt == "*":
            j = text.find("*/", i + 2)
            i = n if j < 0 else j + 2
        elif c == '"':
            i += 1
            while i < n:
                if text[i] == "\\":
                    i += 2
                    continue
                if text[i] == '"':
                    i += 1
                    break
                i += 1
        elif c == "'":
            i += 1
            while i < n:
                if text[i] == "\\":
                    i += 2
                    continue
                if text[i] == "'":
                    i += 1
                    break
                i += 1
        else:
            out.append(c)
            i += 1
    return "".join(out)


def build_type_map(project):
    """full type name -> (file, raw line count) for a project dir; also simple name list."""
    base = os.path.join(ROOT, project)
    type2file = {}
    file_lines = {}
    for dirpath, dirnames, filenames in os.walk(base):
        dirnames[:] = [d for d in dirnames if d != "Properties" or project == "Rawr.Mage"]
        for f in filenames:
            if not f.endswith(".cs"):
                continue
            p = os.path.join(dirpath, f)
            rel = os.path.relpath(p, ROOT)
            txt = open(p, encoding="utf-8", errors="replace").read()
            raw_lines = txt.count("\n") + 1
            file_lines[rel] = raw_lines
            s = strip_comments_strings(txt)
            ns = NS_RE.search(s)
            nsname = ns.group(1) if ns else ""
            for t in TYPE_RE.findall(s):
                full = (nsname + "." + t) if nsname else t
                type2file.setdefault(full, (rel, raw_lines))
    return type2file, file_lines


def main():
    # parse analyzer output
    needed_types = {}   # fullname -> (ui, methods/fields)
    member_parent = {}
    type_reason = {}
    members_by_type = defaultdict(list)
    for line in open("/tmp/opencode/phase0/closure_il.txt"):
        parts = line.rstrip("\n").split("\t")
        if parts[0] == "T" and len(parts) >= 4:
            needed_types[parts[1]] = (parts[2], parts[3])
        elif parts[0] == "M" and len(parts) >= 3:
            member_parent[parts[1]] = parts[2]
            members_by_type[parts[1].split("::")[0]].append(parts[1])
        elif parts[0] == "R" and len(parts) >= 3:
            type_reason[parts[1]] = parts[2]

    base_types, base_lines = build_type_map("Rawr.Base")
    mage_types, mage_lines = build_type_map("Rawr.Mage")

    def owner(full):
        return full.replace("+", "/").split("/")[0]

    # resolve root seeds for a reason string; returns set of mage seeds (recursive)
    memo = {}

    def roots(entity, kind):
        key = kind + ":" + entity
        if key in memo:
            return memo[key]
        memo[key] = set()  # cycle guard
        if kind == "type":
            r = type_reason.get(entity, "")
        else:
            r = member_parent.get(entity, "")
        out = set()
        if r.startswith("mage:"):
            out.add(r[5:])
        elif r.startswith("type:"):
            out |= roots(owner(r[5:]), "type")
        elif r.startswith("member:"):
            m = r[7:]
            out |= roots(m, "member")
        memo[key] = out
        return out

    # aggregate by Base file
    file_info = defaultdict(lambda: {"types": [], "members": set(), "ui": False, "roots": set()})
    unknown = []
    for t, (ui, cnt) in needed_types.items():
        if t.startswith("<PrivateImplementationDetails>"):
            continue
        base = owner(t)
        entry = base_types.get(base)
        if not entry:
            unknown.append(t)
            continue
        f, lines = entry
        info = file_info[f]
        info["types"].append(t)
        info["ui"] = info["ui"] or (ui == "UI")
        info["members"] |= set(members_by_type.get(t, []))
        info["roots"] |= roots(t, "type")

    print(f"needed Base types: {len(needed_types)-4}, mapped to {len(file_info)} files; unknown: {unknown}\n")
    total = 0
    for f in sorted(file_info):
        info = file_info[f]
        lines = base_lines.get(f, 0)
        total += lines
        seed_kinds = set()
        for r in info["roots"]:
            mf = mage_types.get(owner(r)) or mage_types.get(r.split("::")[0])
            fname = mf[0] if mf else "?"
            seed_kinds.add(os.path.basename(fname))
        print(f"{'UI' if info['ui'] else '  '} {f:60s} {lines:6d} types={len(info['types']):2d} members={len(info['members']):4d}")
        print(f"      seeds: {', '.join(sorted(seed_kinds))[:180]}")
    print(f"\ntotal Base lines in closure files: {total}")

    # mage files with their sizes
    print("\n== Rawr.Mage files ==")
    engine_ui = []
    for f in sorted(mage_lines):
        print(f"   {f:60s} {mage_lines[f]:6d}")


if __name__ == "__main__":
    main()
