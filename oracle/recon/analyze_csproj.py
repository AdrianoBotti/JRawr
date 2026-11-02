#!/usr/bin/env python3
"""Phase 0 recon: parse all .csproj, build reference graph, find linked files."""
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict

ROOT = os.path.abspath(os.environ.get(
    "RAWR_LEGACY_DIR",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "legacy", "RAWR_WotLK_3.3.5a"),
))
NS = "{http://schemas.microsoft.com/developer/msbuild/2003}"


def local(tag):
    return tag.split("}")[-1] if "}" in tag else tag


def find_csproj(root):
    out = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in (".git", "bin", "obj", "_ReSharper")]
        for f in filenames:
            if f.endswith(".csproj"):
                out.append(os.path.join(dirpath, f))
    return sorted(out)


def parse(path):
    tree = ET.parse(path)
    root = tree.getroot()
    proj = {
        "path": os.path.relpath(path, ROOT),
        "dir": os.path.dirname(path),
        "name": os.path.splitext(os.path.basename(path))[0],
        "outputtype": None,
        "targetfw": None,
        "projecttypeguids": [],
        "projectrefs": [],
        "assemblyrefs": [],
        "compile": [],  # (include, link or None)
        "embedded": [],
        "content": [],
    }
    for el in root.iter():
        t = local(el.tag)
        if t == "OutputType":
            proj["outputtype"] = (el.text or "").strip()
        elif t == "TargetFrameworkVersion":
            proj["targetfw"] = (el.text or "").strip()
        elif t == "ProjectTypeGuids":
            proj["projecttypeguids"] = [g.strip() for g in (el.text or "").split(";") if g.strip()]
        elif t == "ProjectReference":
            inc = el.get("Include")
            if inc:
                proj["projectrefs"].append(inc)
        elif t == "Reference":
            inc = (el.get("Include") or "").strip()
            hint = None
            for child in el:
                if local(child.tag) == "HintPath":
                    hint = (child.text or "").strip()
            if inc:
                proj["assemblyrefs"].append({"include": inc, "hintpath": hint})
        elif t == "Compile" and el.get("Include"):
            inc = el.get("Include")
            link = None
            for child in el:
                if local(child.tag) == "Link":
                    link = (child.text or "").strip()
            proj["compile"].append((inc, link))
        elif t == "EmbeddedResource" and el.get("Include"):
            proj["embedded"].append(el.get("Include"))
        elif t == "Content" and el.get("Include"):
            proj["content"].append(el.get("Include"))
    return proj


def norm_win_path(p):
    return p.replace("\\", "/").strip()


def main():
    projects = [parse(p) for p in find_csproj(ROOT)]
    by_name = {}
    for p in projects:
        by_name.setdefault(p["name"], []).append(p)

    # reference graph by resolved path
    edges = {}
    for p in projects:
        resolved = []
        for ref in p["projectrefs"]:
            target = os.path.normpath(os.path.join(p["dir"], norm_win_path(ref)))
            resolved.append(os.path.relpath(target, ROOT))
        edges[p["path"]] = sorted(resolved)

    # which projects reference which
    reverse = defaultdict(list)
    for src, targets in edges.items():
        for t in targets:
            reverse[t].append(src)

    # linked files: Compile items with a Link, plus duplicated physical files across projects
    file_owners = defaultdict(set)
    linked_explicit = []
    for p in projects:
        for inc, link in p["compile"]:
            phys = os.path.normpath(os.path.join(p["dir"], norm_win_path(inc)))
            rel = os.path.relpath(phys, ROOT)
            file_owners[rel].add(p["path"])
            if link:
                linked_explicit.append((p["path"], rel, link))
    duplicated = {f: sorted(owners) for f, owners in file_owners.items() if len(owners) > 1}

    out = {
        "projects": projects,
        "edges": edges,
        "reverse": {k: sorted(v) for k, v in reverse.items()},
        "linked_explicit": linked_explicit,
        "duplicated_compile_files": duplicated,
    }
    with open("/tmp/opencode/phase0/csproj.json", "w") as fh:
        json.dump(out, fh, indent=1)

    print(f"projects: {len(projects)}")
    print(f"explicit <Link> entries: {len(linked_explicit)}")
    print(f"physical .cs files compiled by >1 project: {len(duplicated)}")
    print("\n== reference graph (project -> projectrefs) ==")
    for p in projects:
        print(f"{p['path']}  [{p['outputtype']}, fw={p['targetfw']}] -> {edges[p['path']]}")


if __name__ == "__main__":
    main()
