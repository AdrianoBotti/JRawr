#!/usr/bin/env python3
"""Match the public/protected members of the prebuilt Rawr.Base.dll (see CoreCompleteness.cs)
against the Java port in rawr-core/rawr-data (rawr-models is a fallback for classes ported there).

Usage: python3 match.py [dll-members.tsv] [out-dir]
Writes out/report.tsv (machine readable) and docs/dev/CORE_COMPLETENESS.md (report, unless
--tsv-only). The docs/dev directory is local and gitignored.
"""
import os
import re
import sys
from collections import defaultdict, OrderedDict
from fnmatch import fnmatchcase

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))

JAVA_DIRS = [
    ("rawr-core", os.path.join(ROOT, "jrawr", "rawr-core", "src", "main", "java")),
    ("rawr-data", os.path.join(ROOT, "jrawr", "rawr-data", "src", "main", "java")),
]
FALLBACK_DIRS = [
    ("rawr-models", os.path.join(ROOT, "jrawr", "rawr-models", "src", "main", "java")),
]

TYPE_KEYWORDS = {"class", "interface", "enum", "record"}
MODIFIERS = {
    "public", "protected", "private", "static", "final", "abstract", "synchronized",
    "native", "default", "sealed", "non-sealed", "strictfp", "transient", "volatile",
}
CONTROL_KEYWORDS = {"if", "for", "while", "switch", "catch", "synchronized", "try", "do", "else", "return", "new", "throw"}

TOKEN_RE = re.compile(
    r'[A-Za-z_$][A-Za-z0-9_$]*|[0-9]+(?:\.[0-9]+)?[A-Za-z]*'
    r'|>>>=|>>=|<<=|==|!=|<=|>=|&&|\|\||\+\+|--|->|::|\.\.\.'
    r'|[{}()\[\];,.<>+\-*/%&|^!~?:=@]'
)


def strip_comments_and_literals(src):
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == '/' and i + 1 < n and src[i + 1] == '/':
            j = src.find('\n', i)
            j = n if j < 0 else j
            out.append(' ' * (j - i))
            i = j
        elif c == '/' and i + 1 < n and src[i + 1] == '*':
            j = src.find('*/', i + 2)
            j = n if j < 0 else j + 2
            out.append(''.join(ch if ch == '\n' else ' ' for ch in src[i:j]))
            i = j
        elif c == '"':
            if src.startswith('"""', i):
                j = src.find('"""', i + 3)
                j = n if j < 0 else j + 3
            else:
                j = i + 1
                while j < n:
                    if src[j] == '\\':
                        j += 2
                        continue
                    if src[j] == '"' or src[j] == '\n':
                        break
                    j += 1
                j = min(j + 1, n)
            out.append(''.join(ch if ch == '\n' else ' ' for ch in src[i:j]))
            i = j
        elif c == "'":
            j = i + 1
            while j < n:
                if src[j] == '\\':
                    j += 2
                    continue
                if src[j] == "'" or src[j] == '\n':
                    break
                j += 1
            j = min(j + 1, n)
            out.append(''.join(ch if ch == '\n' else ' ' for ch in src[i:j]))
            i = j
        else:
            out.append(c)
            i += 1
    return ''.join(out)


def tokenize(masked):
    tokens = []
    for m in TOKEN_RE.finditer(masked):
        tokens.append((m.group(0), m.start()))
    return tokens


def line_of(masked, pos):
    return masked.count('\n', 0, pos) + 1


class JMember:
    __slots__ = ("name", "arity", "type", "static", "kind", "line")

    def __init__(self, name, arity, type_, static, kind, line):
        self.name = name
        self.arity = arity
        self.type = type_
        self.static = static
        self.kind = kind
        self.line = line


class JType:
    def __init__(self, package, chain, path, kind, line):
        self.package = package
        self.chain = chain
        self.path = path
        self.kind = kind
        self.line = line
        self.fields = {}
        self.methods = defaultdict(list)
        self.ctors = []
        self.parent = None

    @property
    def qname(self):
        return ".".join([self.package] + self.chain) if self.package else ".".join(self.chain)

    def add_field(self, name, type_, static, line):
        self.fields[name] = JMember(name, 0, type_, static, "field", line)

    def add_method(self, name, arity, type_, static, kind, line):
        self.methods[name].append(JMember(name, arity, type_, static, kind, line))

    def add_ctor(self, name, arity, line):
        self.ctors.append(JMember(name, arity, name, False, "ctor", line))

    def all_names_ci(self):
        names = set()
        for n in list(self.fields) + list(self.methods):
            names.add(n.lower())
        return names


class JavaParser:
    def __init__(self):
        self.types = []
        self.files = 0
        self.lines = 0

    def parse_dirs(self, dirs):
        for _, d in dirs:
            if not os.path.isdir(d):
                continue
            for dirpath, _, files in os.walk(d):
                for f in sorted(files):
                    if f.endswith(".java"):
                        self.parse_file(os.path.join(dirpath, f))

    def parse_file(self, path):
        with open(path, "r", encoding="utf-8", errors="replace") as fh:
            src = fh.read()
        self.files += 1
        self.lines += src.count('\n') + 1
        masked = strip_comments_and_literals(src)
        tokens = tokenize(masked)
        package = ""
        for i, (tok, _) in enumerate(tokens):
            if tok == "package" and i + 1 < len(tokens):
                parts = [tokens[i + 1][0]]
                k = i + 2
                while k + 1 < len(tokens) and tokens[k][0] == "." and re.match(r'^[A-Za-z_$]', tokens[k + 1][0]):
                    parts.append(tokens[k + 1][0])
                    k += 2
                package = ".".join(parts)
                break
        self.parse_types(tokens, masked, package, path, 0, len(tokens))

    def parse_types(self, tokens, masked, package, path, start, end, chain=None):
        chain = chain or []
        i = start
        while i < end:
            tok = tokens[i][0]
            if tok in TYPE_KEYWORDS:
                prev = tokens[i - 1][0] if i > 0 else ""
                if prev == "." or prev == ">":
                    i += 1
                    continue
                name = tokens[i + 1][0] if i + 1 < len(tokens) else "?"
                body_start, parent = self.find_body(tokens, i + 2, end)
                if body_start < 0:
                    i += 1
                    continue
                body_end = self.matching(tokens, body_start, end, "{", "}")
                jt = JType(package, chain + [name], path, tok, line_of(masked, tokens[i][1]))
                jt.parent = parent
                self.types.append(jt)
                if tok == "record":
                    self.parse_record_components(tokens, i + 2, body_start, jt)
                self.parse_members(tokens, masked, body_start + 1, body_end, jt, chain + [name])
                i = body_end + 1
                continue
            i += 1

    def parse_record_components(self, tokens, start, end, jt):
        depth = 0
        open_idx = -1
        for i in range(start, end):
            t = tokens[i][0]
            if t == "(" and depth == 0:
                open_idx = i
                depth = 1
                continue
            if open_idx >= 0:
                if t == "(":
                    depth += 1
                elif t == ")":
                    depth -= 1
                    if depth == 0:
                        break
        if open_idx < 0:
            return
        groups = self.split_top(tokens, open_idx + 1, i, ",")
        for g in groups:
            if not g:
                continue
            name_idx = None
            for k in range(len(g) - 1, -1, -1):
                if re.match(r'^[A-Za-z_$]', g[k][0]) and g[k][0] not in MODIFIERS and g[k][0] not in ("final",):
                    name_idx = k
                    break
            if name_idx is None:
                continue
            name = g[name_idx][0]
            type_ = self.join_type(g[:name_idx])
            line = 0
            jt.add_field(name, type_, False, line)
            jt.add_method(name, 0, type_, False, "method", line)
        jt.add_ctor(jt.chain[-1], len(groups), 0)

    def find_body(self, tokens, start, end):
        depth = 0
        for i in range(start, end):
            t = tokens[i][0]
            if t in "([<":
                depth += 1
            elif t in ")]>":
                depth = max(0, depth - 1)
            elif t == "{" and depth == 0:
                return i, self.find_parent(tokens, start, i)
        return -1, None

    def find_parent(self, tokens, start, end):
        for i in range(start, end):
            if tokens[i][0] == "extends" and i + 1 < end:
                j = i + 1
                name = tokens[j][0]
                while j + 2 < end and tokens[j + 1][0] == ".":
                    name += "." + tokens[j + 2][0]
                    j += 2
                return name
        return None

    def matching(self, tokens, start, end, open_tok, close_tok):
        depth = 0
        for i in range(start, end):
            t = tokens[i][0]
            if t == open_tok:
                depth += 1
            elif t == close_tok:
                depth -= 1
                if depth == 0:
                    return i
        return end

    def split_top(self, tokens, start, end, sep):
        groups = []
        cur = []
        depth = 0
        for i in range(start, end):
            t = tokens[i][0]
            if t in "([{<":
                depth += 1
            elif t in ")]}>":
                depth = max(0, depth - 1)
            if t == sep and depth == 0:
                groups.append(cur)
                cur = []
            else:
                cur.append(tokens[i])
        groups.append(cur)
        return groups

    def join_type(self, toks):
        s = "".join(t[0] for t in toks)
        s = s.replace("?extends", "?").replace("?super", "?")
        return s

    def parse_members(self, tokens, masked, start, end, jt, chain):
        i = start
        enum_constants = jt.kind == "enum"
        while i < end:
            t = tokens[i][0]
            if t in (";", "}"):
                i += 1
                continue
            # nested type? look past annotations and modifiers
            k = i
            if tokens[k][0] == "@":
                k = self.skip_annotation(tokens, k, end)
            while k < end and tokens[k][0] in MODIFIERS:
                k += 1
            if k < end and tokens[k][0] in TYPE_KEYWORDS:
                prev = tokens[k - 1][0] if k > 0 else ""
                if prev not in (".", ">") and (k + 1 < end):
                    name = tokens[k + 1][0]
                    body_start, parent = self.find_body(tokens, k + 2, end)
                    if body_start >= 0:
                        body_end = self.matching(tokens, body_start, end, "{", "}")
                        nested = JType(jt.package, chain + [name], jt.path, tokens[k][0], line_of(masked, tokens[k][1]))
                        nested.parent = parent
                        self.types.append(nested)
                        if tokens[k][0] == "record":
                            self.parse_record_components(tokens, k + 2, body_start, nested)
                        self.parse_members(tokens, masked, body_start + 1, body_end, nested, chain + [name])
                        i = body_end + 1
                        continue
            if t == "@":
                i = self.skip_annotation(tokens, i, end)
                continue
            decl_start = i
            j = i
            depth = 0
            stop = None
            while j < end:
                tj = tokens[j][0]
                if depth == 0 and tj == ";":
                    stop = ";"
                    break
                if depth == 0 and tj == "{":
                    stop = "{"
                    break
                if depth == 0 and tj == "}":
                    stop = "}"
                    break
                if tj in "([{<":
                    depth += 1
                elif tj in ")]}>":
                    depth = max(0, depth - 1)
                j += 1
            decl = tokens[decl_start:j]
            if enum_constants and stop in (";", "}"):
                self.parse_enum_constants(decl, jt)
                enum_constants = False
                i = j + 1
                continue
            if not decl:
                i = j + 1
                continue
            handled = self.parse_decl(decl, tokens, masked, jt, stop, j, end)
            if stop == "{":
                body_end = self.matching(tokens, j, end, "{", "}")
                i = body_end + 1
                if handled == "field" and i < end and tokens[i][0] == ";":
                    i += 1
            else:
                i = j + 1
            if enum_constants and stop == ";" and handled is None:
                enum_constants = False

    def parse_enum_constants(self, decl, jt):
        for g in self.split_top(decl, 0, len(decl), ","):
            if not g:
                continue
            name = g[0][0]
            if re.match(r'^[A-Za-z_$]', name):
                jt.add_field(name, jt.chain[-1], True, 0)

    def skip_annotation(self, tokens, i, end):
        j = i + 1
        if j < end and re.match(r'^[A-Za-z_$]', tokens[j][0]):
            j += 1
            while j < end and tokens[j][0] == ".":
                j += 2
        if j < end and tokens[j][0] == "(":
            depth = 0
            while j < end:
                t = tokens[j][0]
                if t in "([{":
                    depth += 1
                elif t in ")]}":
                    depth -= 1
                    if depth == 0:
                        return j + 1
                j += 1
        return j

    def parse_decl(self, decl, tokens, masked, jt, stop, stop_idx, end):
        # strip leading annotations/modifiers to inspect the signature
        k = 0
        static = False
        while k < len(decl) and decl[k][0] in MODIFIERS:
            if decl[k][0] == "static":
                static = True
            k += 1
        sig = decl[k:]
        if not sig:
            return None
        eq = self.first_top(sig, "=")
        paren = self.first_top(sig, "(")
        if paren > 0 and (eq < 0 or paren < eq):
            name_idx = paren - 1
            if sig[name_idx][0] == ">":
                lt = self.match_back(sig, name_idx, "<", ">")
                if lt > 0:
                    name_idx = lt - 1
            name = sig[name_idx][0]
            if name in CONTROL_KEYWORDS or name in MODIFIERS:
                return None
            ret = self.join_type(sig[:name_idx])
            close = self.matching(list(sig), paren, len(sig), "(", ")")
            arity = 0
            if close > paren + 1:
                arity = len(self.split_top(sig, paren + 1, close, ","))
                if arity == 1 and not [t for t in sig[paren + 1:close]]:
                    arity = 0
            line = line_of(masked, decl[0][1]) if decl else 0
            if name == jt.chain[-1] and ret == "":
                jt.add_ctor(name, arity, line)
            else:
                jt.add_method(name, arity, ret, static, "method", line)
            return "method"
        # field
        line = line_of(masked, decl[0][1]) if decl else 0
        for g in self.split_top(decl, 0, len(decl), ","):
            name_idx = None
            for kk in range(len(g) - 1, -1, -1):
                tk = g[kk][0]
                if tk == "=":
                    name_idx = kk - 1
                    while name_idx > 0 and g[name_idx][0] in "]":
                        while name_idx > 0 and g[name_idx][0] != "[":
                            name_idx -= 1
                        name_idx -= 1
                    break
                if tk == "{":
                    continue
            if name_idx is None:
                for kk in range(len(g) - 1, -1, -1):
                    tk = g[kk][0]
                    if tk == "{":
                        continue
                    if re.match(r'^[A-Za-z_$]', tk) and tk not in MODIFIERS and tk not in ("final",):
                        name_idx = kk
                        break
            if name_idx is None or name_idx >= len(g):
                continue
            name = g[name_idx][0]
            if not re.match(r'^[A-Za-z_$]', name) or name in MODIFIERS:
                continue
            type_ = self.join_type(g[:name_idx]) if name_idx > 0 else ""
            jt.add_field(name, type_, static, line)
        return "field"

    def first_top(self, toks, what):
        depth = 0
        for i, (t, _) in enumerate(toks):
            if t == what and depth == 0:
                return i
            if t in "([{<":
                depth += 1
            elif t in ")]}>":
                depth = max(0, depth - 1)
        return -1

    def match_back(self, toks, idx, open_tok, close_tok):
        depth = 0
        for i in range(idx, -1, -1):
            t = toks[i][0]
            if t == close_tok:
                depth += 1
            elif t == open_tok:
                depth -= 1
                if depth == 0:
                    return i
        return -1


# ---------------------------------------------------------------------------
# matching
# ---------------------------------------------------------------------------

CS_TO_JAVA_TYPE = {
    "String": "String", "Int32": "int", "Int64": "long", "Single": "float",
    "Double": "double", "Boolean": "boolean", "Char": "char", "Object": "Object",
    "Void": "void", "Byte": "byte", "SByte": "byte", "Int16": "short", "UInt16": "int",
    "UInt32": "long", "UInt64": "long", "Decimal": "BigDecimal", "Guid": "String",
    "DateTime": "Instant", "TimeSpan": "Duration",
}

JAVA_TO_CS_TYPE = {v.lower(): k for k, v in CS_TO_JAVA_TYPE.items() if k != "Guid"}


def strip_generics(s):
    out = []
    depth = 0
    for ch in s:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth = max(0, depth - 1)
        elif ch == "," and depth > 0:
            out.append(",")
        elif depth == 0:
            out.append(ch)
    # cut everything after the first top-level comma (multi type params)
    s = "".join(out)
    return s.split(",")[0]


def cs_type_simple(full):
    if not full:
        return ""
    s = strip_generics(full)
    s = s.split("/")[-1]
    s = s.split(".")[-1]
    s = s.split("`")[0]
    s = s.rstrip("[]") .replace("[]", "")
    return CS_TO_JAVA_TYPE.get(s, s)


def java_type_simple(java):
    if not java:
        return ""
    s = strip_generics(java)
    s = s.replace("[]", "")
    s = s.split(".")[-1]
    return s


class Matched:
    def __init__(self, jt, reason):
        self.jt = jt
        self.reason = reason


class Missing:
    def __init__(self, cs_type, row, category, note):
        self.cs_type = cs_type
        self.row = row
        self.category = category
        self.note = note


def lower_first(name):
    return name[0].lower() + name[1:] if name else name


OBJECT_METHODS = {
    "ToString": "toString", "GetHashCode": "hashCode", "Equals": "equals",
    "CompareTo": "compareTo", "Clone": "clone", "Finalize": "finalize",
    "GetType": "getClass", "MemberwiseClone": "clone",
}


def underscore_variants(name):
    core = name.strip("_")
    if "_" not in core:
        return []
    parts = [p for p in core.split("_") if p]
    if not parts:
        return []
    camel = parts[0][:1].lower() + parts[0][1:] + "".join(p.capitalize() for p in parts[1:])
    pascal = "".join(p[:1].upper() + p[1:] for p in parts)
    all_lower = parts[0].lower() + "".join(p.capitalize() for p in parts[1:])
    return [camel, pascal, all_lower] if all_lower != camel else [camel, pascal]


def member_candidates(name, kind):
    cands = []
    if name in OBJECT_METHODS:
        cands.append(OBJECT_METHODS[name])
    if name == "GetClass":
        cands.append("getCharacterClass")
    if name == "MoveNext":
        cands += ["next", "hasNext"]
    if name == "GetEnumerator":
        cands.append("iterator")
    if name == "Char":
        cands.append("character")
    base = name.lstrip("_")
    if base != name:
        cands.append(lower_first(base))
        cands.append(base)
        cands.append("get" + base)
        cands.append("is" + base)
    for v in underscore_variants(name):
        cands.append(v)
        cands.append("get" + v[:1].upper() + v[1:])
        cands.append("is" + v[:1].upper() + v[1:])
    if name == "Class":
        cands.append("CharacterClass")
        cands.append("characterClass")
    cands.append(lower_first(name))
    cands.append(name)
    cands.append("get" + name)
    cands.append("is" + name)
    if kind == "method":
        cands.append("get" + name)
        cands.append("is" + name)
    return cands


def find_property(jt, name, arity=0):
    if arity > 0:
        # C# indexer: Java get(...)/set(...)
        for c in ("get", "set"):
            for m in jt.methods.get(c, []):
                if m.arity == arity or (c == "set" and m.arity == arity + 1):
                    return ("indexer-method", c)
        return None
    for c in member_candidates(name, "property"):
        if c in jt.methods:
            return ("method", c)
        if c in jt.fields:
            return ("field", c)
    low = name.lower()
    for c in jt.methods:
        if c.lower() == low:
            return ("method-ci", c)
    for c in jt.fields:
        if c.lower() == low:
            return ("field-ci", c)
    for c in jt.methods:
        cl = c.lower()
        if cl == "get" + low or cl == "is" + low:
            return ("method-ci-getter", c)
    return None


def find_method(jt, name, arity):
    cands = member_candidates(name, "method")
    for c in cands:
        if c in jt.methods:
            ms = jt.methods[c]
            for m in ms:
                if m.arity == arity:
                    return ("match", c, m)
            return ("arity", c, ms[0])
    low = name.lower()
    for c in jt.methods:
        if c.lower() == low:
            for m in jt.methods[c]:
                if m.arity == arity:
                    return ("match-ci", c, m)
            return ("arity-ci", c, jt.methods[c][0])
    for c in jt.methods:
        cl = c.lower()
        if cl == "get" + low or cl == "is" + low:
            for m in jt.methods[c]:
                if m.arity == arity:
                    return ("match-ci-getter", c, m)
    return None


def find_field(jt, name):
    for c in member_candidates(name, "field"):
        if c in jt.fields:
            return ("field", c)
        if c in jt.methods and jt.methods[c][0].arity == 0:
            return ("method", c)
    low = name.lower()
    for c in jt.fields:
        if c.lower() == low:
            return ("field-ci", c)
    for c in jt.methods:
        cl = c.lower()
        if (cl == low or cl == "get" + low or cl == "is" + low) and jt.methods[c][0].arity == 0:
            return ("method-ci", c)
    return None


def find_event(jt, name):
    low = name.lower()
    for c in jt.methods:
        if low in c.lower():
            return ("method", c)
    for c in jt.fields:
        if low in c.lower():
            return ("field", c)
    return None


def find_ctor(jt, arity):
    if not jt.ctors:
        if arity == 0 and jt.kind == "class":
            return ("implicit", None)
        return None
    for c in jt.ctors:
        if c.arity == arity:
            return ("match", c)
    return ("arity", jt.ctors[0])


def load_dll_tsv(path):
    types = OrderedDict()
    members = defaultdict(list)
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            parts = line.rstrip("\n").split("\t")
            if not parts:
                continue
            if parts[0] == "T":
                tname, vis, flags = parts[1], parts[2], parts[3]
                types[tname] = {"vis": vis, "flags": set(flags.split(",")) if flags else set()}
            elif parts[0] in ("M", "F", "P", "E"):
                members[parts[1]].append(parts)
    return types, members


def type_chain(full):
    segs = []
    for part in full.replace("/", ".").split("."):
        part = part.split("`")[0]
        if part == "Rawr":
            continue
        segs.append(part)
    return segs


def pascal_chain(chain):
    out = []
    for seg in chain:
        if re.match(r'^[A-Z][A-Z0-9_]*$', seg) and "_" in seg:
            out.append("".join(w.capitalize() for w in seg.split("_")))
        else:
            out.append(seg)
    return out


def build_java_index(parser):
    by_suffix = defaultdict(list)
    by_simple = defaultdict(list)
    for jt in parser.types:
        by_simple[jt.chain[-1]].append(jt)
        for n in (1, 2):
            if len(jt.chain) >= n:
                by_suffix[tuple(jt.chain[-n:])].append(jt)
    for jt in parser.types:
        if not jt.parent:
            continue
        pname = jt.parent.split(".")[-1]
        cands = [p for p in by_simple.get(pname, []) if p.path == jt.path]
        if not cands:
            cands = by_simple.get(pname, [])
        if cands:
            JT_BASES[jt.qname] = cands[0]
    return by_suffix


JT_BASES = {}


def chain_find(jt, fn, *args):
    seen = set()
    cur = jt
    while cur is not None and cur.qname not in seen:
        seen.add(cur.qname)
        r = fn(cur, *args)
        if r is not None:
            return r
        cur = JT_BASES.get(cur.qname)
    return None


def pick_java_type(by_suffix, chain, source_rank):
    for n in (2, 1):
        if len(chain) < n:
            continue
        cands = by_suffix.get(tuple(chain[-n:]), [])
        if not cands:
            continue
        cands = sorted(cands, key=lambda jt: source_rank.get(jt.path, 9))
        if len(cands) == 1:
            return cands[0], ""
        # prefer core/data over models
        if source_rank.get(cands[0].path, 9) < source_rank.get(cands[1].path, 9):
            return cands[0], ""
        return cands[0], "ambiguous: " + ", ".join(jt.qname for jt in cands[:4])
    return None, ""


def classify(cs_type, info, row, reach, extra_ui, extra_net):
    flags = info["flags"] if info else set()
    rowtext = "\t".join(row)
    category = None
    note = ""
    if "ui" in flags or extra_ui or ",ui" in rowtext:
        category = "UI/designer"
    elif "network" in flags or extra_net or ",net" in rowtext:
        category = "network/import (parked DECISIONS #30/#31/#32)"
    elif "delegate" in flags:
        category = "delegate (Java: functional interface)"
    elif cs_type.endswith("Attribute"):
        category = "attribute (XML/serialization metadata)"
    elif reach == "dead":
        category = "dead/unreachable (IL evidence)"
    else:
        category = "REAL GAP"
    return category, note


def member_key(row):
    if row[0] == "M":
        return row[3] + "/" + row[4]
    return row[2]


def _pair(*names):
    return tuple(names)


# Curated classifications for members the matcher cannot resolve on its own. Patterns are fnmatch
# expressions on the C# type full name and on `name` (M rows: `name/arity`). First match wins.
OVERRIDES = [
    ("Rawr.Character", "_class", "equivalent (renamed)", "Java field `characterClass`"),
    ("Rawr.Character", "_enforceMetagemRequirements", "equivalent (renamed)", "Java field `enforceGemRequirements`"),
    ("Rawr.Character", "_*", "serialization shim",
     "XmlSerializer save-slot property (GemmedId string); Java writes the same Character XML with a DOM writer and reads it in rawr-data/CharacterFixtureLoader"),
    ("Rawr.Character", "Serializable*Talents", "serialization shim",
     "XmlSerializer talent property; Java serializes the talent tree in its own save/load code"),
    ("Rawr.Character", "CurrentTalentsCata", "serialization shim", "XmlSerializer talent property"),
    ("Rawr.Character", "IsLoading", "serialization shim",
     "XmlSerializer-only load flag (suppresses recalculations while loading); Java loads through CharacterFixtureLoader"),
    ("Rawr.Character", "SaveBuffs/1", "serialization shim", "Java writes buffs in Character.save"),
    ("Rawr.Character", "Load/1", "serialization shim", "Java loads via rawr-data/CharacterFixtureLoader"),
    ("Rawr.Character", "LoadFromXml/1", "serialization shim", "Java loads via rawr-data/CharacterFixtureLoader"),
    ("Rawr.Character", "LoadBuffsFromXml/1", "serialization shim", "Java loads via rawr-data/BuffLoader + CharacterFixtureLoader"),
    ("Rawr.Character", "ArmoryPets", "network/import (parked DECISIONS #30/#31/#32)", "Armory import"),
    ("Rawr.Character", "ArmoryPetsXml", "network/import (parked DECISIONS #30/#31/#32)", "Armory import"),
    ("Rawr.Character", "ToggleItemAvailability/2", "UI/designer",
     "item-picker toggle (context menus/graphs); Java web item picker will need it in Stage B"),
    ("Rawr.Character", "ToggleAvailableItemEnchantRestriction/2", "UI/designer",
     "enchant-picker toggle (ItemEnchantContextualMenu)"),
    ("Rawr.Character", "RaceChanged", "UI/designer", "WinForms event (no listener in the Java port)"),
    ("Rawr.Character", "AvailableItemsChanged", "UI/designer", "WinForms event (no listener in the Java port)"),
    ("Rawr.Character", "TalentChangedEvent", "UI/designer", "WinForms event (no listener in the Java port)"),
    ("Rawr.Character", "ClassChanged", "UI/designer", "WinForms event (no listener in the Java port)"),
    ("Rawr.Character", "OnRaceChanged/0", "UI/designer", "raises the WinForms event above"),
    ("Rawr.Character", "OnAvailableItemsChanged/0", "UI/designer", "raises the WinForms event above"),
    ("Rawr.Character", "OnTalentChange/0", "UI/designer", "raises the WinForms event above"),
    ("Rawr.Character", "OnClassChanged/0", "UI/designer", "raises the WinForms event above"),
    ("Rawr.Character", "Head", "equivalent accessor",
     "Java uses get(CharacterSlot)/set(CharacterSlot,value); getMainHand/getOffHand already exist"),
    ("Rawr.Character", "Neck", "equivalent accessor", "see Head"),
    ("Rawr.Character", "Shoulders", "equivalent accessor", "see Head"),
    ("Rawr.Character", "Back", "equivalent accessor", "see Head"),
    ("Rawr.Character", "Chest", "equivalent accessor", "see Head"),
    ("Rawr.Character", "Shirt", "equivalent accessor", "see Head"),
    ("Rawr.Character", "Tabard", "equivalent accessor", "see Head"),
    ("Rawr.Character", "Wrist", "equivalent accessor", "see Head"),
    ("Rawr.Character", "Hands", "equivalent accessor", "see Head"),
    ("Rawr.Character", "Waist", "equivalent accessor", "see Head"),
    ("Rawr.Character", "Legs", "equivalent accessor", "see Head"),
    ("Rawr.Character", "Feet", "equivalent accessor", "see Head"),
    ("Rawr.Character", "Finger1", "equivalent accessor", "see Head"),
    ("Rawr.Character", "Finger2", "equivalent accessor", "see Head"),
    ("Rawr.Character", "Trinket1", "equivalent accessor", "see Head"),
    ("Rawr.Character", "Trinket2", "equivalent accessor", "see Head"),
    ("Rawr.Character", "Projectile", "equivalent accessor", "see Head"),
    ("Rawr.Character", "ProjectileBag", "equivalent accessor", "see Head"),
    ("Rawr.Character", "BackEnchant", "equivalent accessor", "Java getEnchantBySlot(CharacterSlot.Back)"),
    ("Rawr.Character", "HandsEnchant", "equivalent accessor", "Java getEnchantBySlot(CharacterSlot.Hands)"),
    ("Rawr.Character", "CharacterSlots", "equivalent accessor", "Java iterates CharacterSlot.values()"),
    ("Rawr.Character", "IsMetaGemActive", "UI/designer", "meta-gem warning shown by PaperDoll/FormMain"),
    ("Rawr.Character", "Faction", "REAL GAP",
     "CharacterFaction exists but Character.faction is not ported (see Character.java clone comment); blocks ValidateActiveBuffs/SaveBuffs parity"),
    ("Rawr.Item", "EnglishName", "equivalent (renamed)",
     "XmlSerializer element Name; Java getName/setName do the same"),
    ("Rawr.Item", "SlotString", "UI/designer", "XmlIgnore databinding string for Slot"),
    ("Rawr.Item", "FactionString", "UI/designer", "XmlIgnore databinding string for Faction"),
    ("Rawr.Item", "TypeString", "UI/designer", "XmlIgnore databinding string for Type"),
    ("Rawr.Item", "SocketColor1String", "UI/designer", "XmlIgnore databinding string for SocketColor1"),
    ("Rawr.Item", "SocketColor2String", "UI/designer", "XmlIgnore databinding string for SocketColor2"),
    ("Rawr.Item", "SocketColor3String", "UI/designer", "XmlIgnore databinding string for SocketColor3"),
    ("Rawr.CommonStat", "*", "replaced (documented)",
     "CommonStat attribute replaced by Stats.StatProperty.minRange and CalculationsBase.getRelativeStatValue"),
    ("Rawr.Stats", "IsPercentage/1", "replaced (documented)",
     "PropertyInfo reflection replaced by Stats.StatProperty.percentage"),
    ("Rawr.Stats", "PropertyInfoCache", "replaced (documented)",
     "reflection cache replaced by Stats.PROPERTY_INFO"),
    ("Rawr.BuffList", "*", "replaced (documented)", "C# List<Buff> subclass; Java uses List<Buff>"),
    ("Rawr.EnchantList", "*", "replaced (documented)", "C# List<Enchant> subclass; Java uses List<Enchant>"),
    ("Rawr.ItemList", "*", "replaced (documented)", "C# List<Item> subclass; Java uses List<Item>"),
    ("Rawr.ArrayUtils/CompareOption", "LessThan", "equivalent (renamed)",
     "Java constant LESS_THAN (UPPER_SNAKE style)"),
    ("Rawr.Stats", "BeginUnsafe/3", "replaced (documented)",
     "pointer fast path removed; accumulateUnsafe writes the fields directly (DECISIONS #66)"),
    ("Rawr.Stats", "EndUnsafe/0", "replaced (documented)",
     "pointer fast path removed; accumulateUnsafe writes the fields directly (DECISIONS #66)"),
    ("Rawr.Optimizer.OptimizerCharacter", "DirectUpgradeEntry", "replaced (documented)",
     "dead Java state removed; direct-upgrade results are returned directly (DECISIONS #70)"),
    ("Rawr.SerializableDictionary`2", "*", "replaced (documented)",
     "plain LinkedHashMap; the legacy XML wrapper is rawr.base.util.XmlMap (DECISIONS #70)"),
    ("Rawr.EnumHelper", "*", "replaced (documented)",
     "Java enums expose values()/name(); the dead reflection shim was deleted (TD-001)"),
    ("Rawr.Utilities", "Clone/1", "replaced (documented)",
     "XmlSerializer deep clone; documented omission in Utilities.java header"),
    ("Rawr.SpecialEffect", "GetUptimePlot/4", "UI/designer",
     "GDI+ uptime plot used only by the Mage chart drawing code; Java omits chart rendering"),
    ("Rawr.BossHandler", "OnPropertyChanged/1", "UI/designer", "INotifyPropertyChanged plumbing"),
    ("Rawr.Base.Algorithms.MarkovProcess*", "*", "dead/unreachable (IL evidence)",
     "DPSWarr dev-only Markov subtree, not ported per DECISIONS (UseMarkov)"),
    ("Rawr.Base.Algorithms.State*", "*", "dead/unreachable (IL evidence)",
     "DPSWarr dev-only Markov subtree, not ported per DECISIONS (UseMarkov)"),
    ("Rawr.ItemCacheInstance", "*", "replaced (documented)",
     "folded into the static rawr.base.items.ItemCache registry + rawr-data/ItemCacheLoader; cost IO and the ItemsChanged event stay parked (WinForms FormMain-driven)"),
    ("Rawr.ItemCache", "Instance", "replaced (documented)",
     "Java ItemCache is a static registry (the C# instance + singleton split was folded)"),
    ("Rawr.ItemCache", "SavedFilePath", "replaced (documented)", "rawr-data/ItemCacheLoader chooses the path"),
    ("Rawr.ItemCache", "Save/0", "replaced (documented)",
     "XML file IO moved to rawr-data/ItemCacheLoader (DECISIONS #15)"),
    ("Rawr.ItemCache", "SaveItemCost/1", "UI/designer", "WinForms FormMain item-cost IO (documented parked)"),
    ("Rawr.ItemCache", "LoadItemCost/1", "UI/designer", "WinForms FormMain item-cost IO (documented parked)"),
    ("Rawr.ItemCache", "ResetItemCost/0", "UI/designer", "WinForms FormMain item-cost IO (documented parked)"),
    ("Rawr.ItemCache", "LoadTokenItemCost/1", "UI/designer", "WinForms FormMain item-cost IO (documented parked)"),
    ("Rawr.Exports", "GetLootRankURL/1", "UI/designer", "WinForms clipboard export (documented dropped)"),
    ("Rawr.Calculations", "ClearCache/0", "relocated (documented)", "Java CalculationsBase.clearCache()"),
    ("Rawr.Calculations", "GetItemCalculations/3", "relocated (documented)",
     "Java CalculationsBase.getItemCalculations(Item, Character, CharacterSlot)"),
    ("Rawr.Calculations", "GetItemStats/2", "relocated (documented)",
     "Java CalculationsBase.accumulateItemStats(Stats, Character, Item)"),
    ("Rawr.Calculations", "GetRelevantStats/1", "relocated (documented)", "per-model getRelevantStats(Stats) (20 implementations)"),
    ("Rawr.Calculations", "ValidModel/1", "relocated (documented)", "Java Calculations.isModelRegistered"),
    ("Rawr.Calculations", "GetCustomChartData/2", "relocated (documented)", "per-model getCustomChartData (18 implementations)"),
    ("Rawr.Calculations", "CanUseAmmo", "relocated (documented)", "Java CalculationsBase.canUseAmmo()"),
    ("Rawr.Calculations", "SupportsMultithreading", "replaced (documented)",
     "Java optimizer has its own single/multi-thread design (parallel agent)"),
    ("Rawr.Calculations", "ModelIcons", "UI/designer", "WinForms model-picker icons"),
    ("Rawr.Calculations", "ModelClasses", "UI/designer", "WinForms model-picker registry"),
    ("Rawr.Calculations", "CalculationOptionsPanel", "UI/designer", "WinForms options panel factory"),
    ("Rawr.Calculations", "ModelChanged", "UI/designer", "registry event for UI refresh"),
    ("Rawr.Calculations", "ModelChanging", "UI/designer", "registry event for UI refresh"),
    ("Rawr.Calculations", "OnModelChanged/0", "UI/designer", "raises the registry event above"),
    ("Rawr.Calculations", "OnModelChanging/0", "UI/designer", "raises the registry event above"),
    ("Rawr.Calculations", "GetEnchantCalculations/4", "UI/designer",
     "enchant picker in FormItemSelection; not ported with the UI (Enchant.FindEnchants(slot, character) overload also missing)"),
    ("Rawr.Calculations", "GetBuffCalculations/3", "UI/designer", "buff picker in FormItemSelection; not ported with the UI"),
    ("Rawr.Calculations", "CharacterDisplayCalculationLabels", "relocated (documented)", "per-model getCharacterDisplayCalculationLabels (21 implementations)"),
    ("Rawr.Calculations", "OptimizableCalculationLabels", "relocated (documented)", "per-model getOptimizableCalculationLabels (18 implementations)"),
    ("Rawr.Calculations", "CustomChartNames", "relocated (documented)", "per-model getCustomChartNames (18 implementations)"),
    ("Rawr.Calculations", "CustomRenderedChartNames", "relocated (documented)", "per-model getCustomRenderedChartNames"),
    ("Rawr.CalculationsBase", "GetItemStats/2", "relocated (documented)", "Java accumulateItemStats on the same class"),
    ("Rawr.CalculationsBase", "GetRelevantStats/1", "relocated (documented)", "per-model getRelevantStats(Stats)"),
    ("Rawr.CalculationsBase", "GetRelevantGlyphs/0", "relocated (documented)", "per-model getRelevantGlyphs (15 implementations)"),
    ("Rawr.CalculationsBase", "SetDefaults/1", "relocated (documented)", "per-model setDefaults (17 implementations)"),
    ("Rawr.CalculationsBase", "GetCustomChartData/2", "relocated (documented)", "per-model getCustomChartData"),
    ("Rawr.CalculationsBase", "RemoveConflictingBuffs/2", "relocated (documented)",
     "Java Calculations.removeConflictingBuffs(List<Buff>, Buff)"),
    ("Rawr.CalculationsBase", "SupportsMultithreading", "replaced (documented)",
     "Java optimizer has its own single/multi-thread design (parallel agent)"),
    ("Rawr.CalculationsBase", "StatGraphRenderer", "UI/designer", "WinForms stat graph"),
    ("Rawr.CalculationsBase", "_statGraphRenderer", "UI/designer", "WinForms stat graph backing field"),
    ("Rawr.CalculationsBase", "CalculationOptionsPanel", "UI/designer", "WinForms options panel factory"),
    ("Rawr.CalculationsBase", "CustomChartNames", "relocated (documented)", "per-model getCustomChartNames"),
    ("Rawr.CalculationsBase", "CustomRenderedChartNames", "relocated (documented)", "per-model getCustomRenderedChartNames"),
    ("Rawr.CalculationsBase", "OptimizableCalculationLabels", "relocated (documented)", "per-model getOptimizableCalculationLabels"),
    ("Rawr.CalculationsBase", "GetEnchantCalculations/4", "UI/designer", "FormItemSelection enchant picker"),
    ("Rawr.CalculationsBase", "GetBuffCalculations/3", "UI/designer", "FormItemSelection buff picker"),
    ("Rawr.CalculationsBase", "GetRelativeStatValues/1", "UI/designer",
     "used by Exports (UI, dropped) and Wowhead loot-rank (network, parked DECISIONS #31); singular getRelativeStatValue exists"),
    # Optimizer/batch: the event surface is ported as listener interfaces (BatchCharacter, BatchTools,
    # BatchOptimizer) and the AvailableItemGenerator.FilterList overloads are renamed in Java (see
    # ALIASES below). What remains is the async layer: the singleton ItemInstanceOptimizer async
    # pattern is inlined synchronously by BatchTools (class headers), and OptimizerBase's C# ThreadPool
    # direct-upgrade valuation was replaced by the opt-in deterministic parallel evaluation
    # (DECISIONS #58). ItemOptimizer has no live caller (reach dead).
    ("Rawr.Optimizer.BatchOptimizer", "OptimizeCharacterAsync/1", "equivalent (renamed)",
     "Java synchronous optimizeCharacter(int, BatchValuation[]); the async layer is omitted (class header)"),
    ("Rawr.Optimizer.BatchOptimizer", "ComputeUpgradesAsync/2", "equivalent (renamed)",
     "Java synchronous computeUpgrades(int, Item)"),
    ("Rawr.Optimizer.BatchOptimizer", "ComputeUpgradesAsync/1", "dead/unreachable (IL evidence)",
     "1-arg overload only called by the unused 2-arg async API"),
    ("Rawr.Optimizer.BatchOptimizer", "EvaluateUpgradeAsync/2", "dead/unreachable (IL evidence)",
     "async entry point never called by shipped code (the sync evaluateUpgrade is used)"),
    ("Rawr.Optimizer.ItemInstanceOptimizer", "ComputeUpgradesAsync/4", "dead/unreachable (IL evidence)",
     "4-arg overload only called by the unused 5-arg async API"),
    ("Rawr.Optimizer.ItemInstanceOptimizer", "*Async*", "shim (async event surface inlined)",
     "async entry points dropped; BatchTools inlines the async state machine and calls the sync methods"),
    ("Rawr.Optimizer.ItemInstanceOptimizer", "On*", "shim (async event surface inlined)",
     "protected event raisers omitted with the async pattern (BatchTools inlines the handlers)"),
    ("Rawr.Optimizer.ItemInstanceOptimizer", "OptimizeCharacterCompleted", "shim (async event surface inlined)",
     "event payload class is part of the omitted async pattern"),
    ("Rawr.Optimizer.ItemInstanceOptimizer", "OptimizeCharacterProgressChanged", "shim (async event surface inlined)",
     "the BatchOptimizer equivalent event is ported as a listener interface"),
    ("Rawr.Optimizer.ItemInstanceOptimizer", "ComputeUpgradesProgressChanged", "shim (async event surface inlined)",
     "the BatchOptimizer equivalent event is ported as a listener interface"),
    ("Rawr.Optimizer.ItemInstanceOptimizer", "ComputeUpgradesCompleted", "shim (async event surface inlined)",
     "the BatchOptimizer equivalent event is ported as a listener interface"),
    ("Rawr.Optimizer.ItemInstanceOptimizer", "EvaluateUpgradeProgressChanged", "shim (async event surface inlined)",
     "the BatchOptimizer equivalent event is ported as a listener interface"),
    ("Rawr.Optimizer.ItemInstanceOptimizer", "EvaluateUpgradeCompleted", "shim (async event surface inlined)",
     "the BatchOptimizer equivalent event is ported as a listener interface"),
    ("Rawr.Optimizer.OptimizeCharacterCompletedEventArgs", "*", "shim (async event surface inlined)",
     "payload of the omitted ItemInstanceOptimizer async pattern"),
    ("Rawr.Optimizer.OptimizerBase`3/GeneratorItemSelector", "*", "delegate (Java: functional interface)",
     "Java OptimizerBase.GeneratorItemSelector"),
    ("Rawr.Optimizer.OptimizerBase`3", "*", "replaced (documented)",
     "C# ThreadPool direct-upgrade valuation replaced by the deterministic single/parallel candidate evaluation (DECISIONS #58)"),
]

# C# members Java renames because of type erasure; (C# type, member name, arity, C# return type glob,
# Java method name). Checked after the normal candidate search fails.
ALIASES = [
    ("Rawr.Stats", "Clone", 0, "*", "copy"),
    ("Rawr.Stats", "CompareTo", 1, "*", "compare"),
    ("Rawr.Optimizer.AvailableItemGenerator", "FilterList", 1, "System.Collections.Generic.List`1<Rawr.Buff>", "filterBuffs"),
    ("Rawr.Optimizer.AvailableItemGenerator", "FilterList", 1, "Rawr.Enchant[]", "filterEnchants"),
    ("Rawr.Optimizer.AvailableItemGenerator", "FilterList", 1, "Rawr.Item[]", "filterItems"),
    ("Rawr.Optimizer.AvailableItemGenerator", "FilterList", 2, "System.Collections.Generic.List`1<Rawr.ItemInstance>", "filterItemInstances"),
]

CLOSED = [
    ("rawr.base.items.Item", "getItemSlotByCharacterSlot(CharacterSlot)", "oracle mapping table (CoreCompletenessTest)"),
    ("rawr.base.items.Item", "getDefaultSlotMap()", "oracle mapping table (CoreCompletenessTest)"),
    ("rawr.base.items.Item", "delete()", "unit test (CoreCompletenessTest)"),
    ("rawr.base.items.ItemInstance", "getDisplayId(), getDisplaySlot()", "unit test (CoreCompletenessTest)"),
    ("rawr.base.stats.Stats", "equals(Object), hashCode(), getStatNames()", "oracle (CoreCompletenessTest); 13 pre-existing extra properties documented below"),
    ("rawr.base.effects.SpecialFunction", "normal(double)", "oracle double values (CoreCompletenessTest)"),
    ("rawr.base.character.Character", "isEquipped(ItemInstance), isEquipped(ItemInstance, CharacterSlot), isEquipped(Item), isEquipped(Item, CharacterSlot)",
     "unit tests (CoreCompletenessTest)"),
    ("rawr.base.character.Character", "isUniqueGemEquipped(Item)", "faithful port; covered indirectly by item/gem code paths"),
    ("rawr.base.character.Character", "recalculatePassiveBonuses()", "faithful port (Draenei -> Heroic Presence)"),
    ("rawr.base.character.Character", "getAllEquippedAndAvailableGearIds()", "unit test (CoreCompletenessTest)"),
    ("rawr.base.character.Character", "getCharacterSlotFromId(int)", "oracle mapping table (CoreCompletenessTest)"),
    ("rawr.base.character.Character", "getFaction(), setRace() -> SetFaction, validateActiveBuffs()",
     "faction unit test; faithful port of Character.cs SetFaction/ValidateActiveBuffs"),
    ("rawr.base.character.Character", "invalidateItemInstances(), invalidateItemInstances(CharacterSlot), getRelevantItemInstances(CharacterSlot), "
     "getRelevantItems(CharacterSlot), getRelevantItems(CharacterSlot, ItemSlot), clearRelevantGems(), "
     "getCustomItemInstances(), setCustomItemInstances()",
     "oracle probe against the shipped Rawr.Base.dll: RelevantItemInstancesGoldenTest (sorted gemmed ids / item ids); "
     "cache-invalidation test; ItemCache.relevantItems() added for the C# RelevantItems property"),
    ("rawr.base.character.Character", "onCalculationsInvalidated() now runs InvalidateItemInstances + RecalculateSetBonuses + "
     "RecalculatePassiveBonuses like Character.cs:1804-1821 (IsLoading suppresses them while loading)",
     "golden suites + RelevantItemInstancesGoldenTest"),
    ("rawr.base.character.Character", "setPrimaryProfession()/setSecondaryProfession() now call Calculations.updateProfessions + validateActiveBuffs",
     "faithful port of the C# property setters"),
    ("rawr.base.calculations.Calculations", "updateProfessions(Character)",
     "delegates to CalculationsBase; faithful port of the base virtual method (no model overrides)"),
    ("rawr.base.calculations.CalculationsBase", "updateProfessions(Character)",
     "faithful port of Calculations.cs:662-700 (Mining/Skinning/Engineering buff fixes)"),
    ("rawr.base.optimizer.AvailableItemGenerator",
     "FilterList(List<Buff>) / FilterList(List<Enchant>) / FilterList(List<Item>) / "
     "FilterList(List<ItemInstance>, bool)",
     "renamed equivalents filterBuffs/filterEnchants/filterItems/filterItemInstances (Java type "
     "erasure); matched through the match.py ALIASES table, same stats filtering as Rawr.Base/Optimizer.AvailableItemGenerator.cs"),
    ("rawr.base.batch.BatchCharacter",
     "PropertyChanged",
     "listener interface + add/remove/onPropertyChanged; fired from the ported property setters at the C# raise sites (BatchEventsTest)"),
    ("rawr.base.batch.BatchTools", "StatusUpdated / OperationCompleted / UpgradeListCompleted / PropertyChanged",
     "listener interfaces fired from updateStatusLabel() and the inlined completion paths at the C# raise sites (BatchEventsTest)"),
    ("rawr.base.batch.BatchOptimizer",
     "OptimizeBatchCompleted/ProgressChanged, ComputeUpgradesCompleted/ProgressChanged, "
     "EvaluateUpgradeCompleted/ProgressChanged",
     "listener interfaces; progress fires from reportProgress(), completed from the synchronous entry points "
     "where the async thread start posted them (BatchEventsTest)"),
]


def apply_override(tname, row):
    key = member_key(row)
    for tp, mp, cat, note in OVERRIDES:
        if fnmatchcase(tname, tp) and fnmatchcase(key, mp):
            return cat, note
    return None


def member_alias(tname, name, arity, ret):
    for tp, mp, a, rt, java_name in ALIASES:
        if fnmatchcase(tname, tp) and name == mp and arity == a and fnmatchcase(ret, rt):
            return java_name
    return None


def write_markdown(path, types, cls_meta, results, matched, java_types, fallback_types):
    import datetime
    counts = defaultdict(int)
    for tname, v in results.items():
        for row, cat, note in v["missing"]:
            counts[cat] += 1
    public_types = [t for t in types if "nonpublic" not in types[t]["flags"]]
    lines = []
    lines.append("# CORE completeness — member-level audit vs the shipped Rawr.Base.dll")
    lines.append("")
    lines.append("Audit of `legacy/RAWR_WotLK_3.3.5a/bin/Release/Data/Rawr.Base.dll` (the shipped app's")
    lines.append("assembly, C# namespace `Rawr`) against `jrawr/rawr-core` and `jrawr/rawr-data`, with")
    lines.append("`rawr-models` as a fallback for classes ported there. Generated by")
    lines.append("`tools/core-completeness` (Mono.Cecil member dump + Java source matcher); regenerate with")
    lines.append("`tools/core-completeness/run.sh && python3 tools/core-completeness/match.py`.")
    lines.append("")
    lines.append("Snapshot: " + datetime.date.today().isoformat() + ". Every member of the shipped")
    lines.append("optimizer/batch event surface is either ported as a Java listener interface")
    lines.append("(`BatchCharacter`, `BatchTools`, `BatchOptimizer`) or classified in this report; the")
    lines.append("remaining optimizer rows are the omitted async layer (shim/equivalent) and the C#")
    lines.append("ThreadPool valuation replaced per DECISIONS #58.")
    lines.append("")
    lines.append("## Method")
    lines.append("")
    lines.append("- `CoreCompleteness.cs` (Mono.Cecil 0.11.1) enumerates every public/protected type and")
    lines.append("  member of the DLL (compiler-generated and `value__`/accessor members excluded).")
    lines.append("- Reachability is transitive: seeds are references from every other shipped assembly")
    lines.append("  (`Rawr.exe`, all `Rawr.*.dll`, `Rawr.Base.XmlSerializers.dll`), interface/override")
    lines.append("  dispatch and reflection-name string literals; the graph is walked over the DLL's own IL.")
    lines.append("  `dead` means no path from a live root; it is used only as evidence for omitted members.")
    lines.append("- `match.py` parses the Java sources (types, nested types, fields, methods, constructors,")
    lines.append("  records, enums, inheritance) and matches by class-name suffix and camelCase member name,")
    lines.append("  tolerating getters/setters, underscore renames (e.g. `InBackPerc_Melee` ->")
    lines.append("  `getInBackPercMelee`), ACRONYM types (e.g. `ATTACK_TYPES` -> `AttackTypes`), inherited")
    lines.append("  members and renamed members (e.g. GetClass -> getCharacterClass). Java signatures may")
    lines.append("  differ; all classified rows were checked by hand.")
    lines.append("")
    lines.append("## Summary")
    lines.append("")
    lines.append("| | |")
    lines.append("|---|---|")
    lines.append("| DLL types checked (public/protected) | %d |" % len(public_types))
    lines.append("| DLL members checked | %d |" % sum(
        cls_meta[t].get("member_count", 0) for t in cls_meta))
    lines.append("| Members matched in Java | %d |" % matched)
    lines.append("| Members missing | %d |" % sum(len(v["missing"]) for v in results.values()))
    lines.append("")
    lines.append("| Classification of missing members | Members |")
    lines.append("|---|---|")
    for cat in sorted(counts, key=lambda c: (-counts[c], c)):
        lines.append("| %s | %d |" % (cat.replace("|", "/"), counts[cat]))
    lines.append("")
    lines.append("## REAL GAPs closed in this audit")
    lines.append("")
    lines.append("| Java location | Member(s) | Locked by |")
    lines.append("|---|---|---|")
    for loc, mem, lock in CLOSED:
        lines.append("| `%s` | %s | %s |" % (loc, mem, lock))
    lines.append("")
    lines.append("## REAL GAPs left open")
    lines.append("")
    lines.append("| Class | Member | Reach | Note |")
    lines.append("|---|---|---|---|")
    open_gaps = []
    for tname in sorted(results):
        for row, cat, note in results[tname]["missing"]:
            if cat == "REAL GAP":
                key = member_key(row)
                reach = row[7] if row[0] == "M" else row[6]
                open_gaps.append((tname, key, reach, note))
    for tname, key, reach, note in open_gaps:
        lines.append("| `%s` | %s | %s | %s |" % (tname, key, reach, note))
    lines.append("")
    lines.append("## Reverse gaps (Java has members the DLL does not)")
    lines.append("")
    lines.append("- 13 stat names: the C# `StatsCat`/`StatsBear` subclass properties are modelled in Java as")
    lines.append("  the `rawr.base.stats.SingleSourceStat` combination category (single-writer stats that no")
    lines.append("  merge operation combines), so they are part of `Stats.PROPERTY_INFO` while the DLL's base")
    lines.append("  `typeof(Stats)` surface does not list them; `CoreCompletenessTest` allows exactly these")
    lines.append("  names (see DECISIONS #63). The DLL members themselves match the Java accessors on `Stats`.")
    lines.append("- `Stats.getStatNames()` sorts with Java natural order; the DLL sorts with the .NET")
    lines.append("  culture-aware default comparer. The name sets match modulo the 13 names above (see")
    lines.append("  `CoreCompletenessTest`); display order may differ for punctuation/space-prefixed names.")
    lines.append("")
    lines.append("## Missing members per class")
    lines.append("")
    classes_with_missing = [t for t in sorted(results) if results[t]["missing"]]
    for tname in classes_with_missing:
        v = results[tname]
        cat_counts = defaultdict(int)
        for row, cat, note in v["missing"]:
            cat_counts[cat] += 1
        summary = ", ".join("%s: %d" % (c.replace("parallel optimizer agent-owned (routed)", "optimizer-owned"),
                                        n) for c, n in sorted(cat_counts.items(), key=lambda kv: -kv[1]))
        lines.append("### %s" % tname)
        lines.append("")
        lines.append("%d matched, %d missing (%s). Java: `%s`." % (
            v["matched"], len(v["missing"]), summary,
            (cls_meta[tname]["java"].qname if cls_meta[tname]["java"] else "not found")))
        lines.append("")
        for row, cat, note in sorted(v["missing"], key=lambda r: (r[0][0], member_key(r[0]))):
            key = member_key(row)
            reach = row[7] if row[0] == "M" else row[6]
            kind = {"M": "method", "F": "field", "P": "property", "E": "event"}[row[0]]
            lines.append("- `%s` (%s, reach `%s`) — **%s**%s" % (
                key, kind, reach, cat, (" — " + note) if note else ""))
        lines.append("")
    matched_classes = [t for t in sorted(results) if not results[t]["missing"] and "nonpublic" not in types[t]["flags"]]
    lines.append("## Classes fully matched")
    lines.append("")
    lines.append("%d public/protected types had every visible member matched; e.g. %s." % (
        len(matched_classes), ", ".join("`%s`" % t for t in matched_classes[:12])))
    lines.append("")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines) + "\n")


def main():
    tsv = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "out", "dll-members.tsv")
    out_dir = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, "out")
        doc_path = os.path.join(ROOT, "docs", "dev", "CORE_COMPLETENESS.md")
    tsv_only = "--tsv-only" in sys.argv

    types, members = load_dll_tsv(tsv)
    # inherit ui/network/delegate flags from enclosing types (nested full names use '/')
    changed = True
    while changed:
        changed = False
        for tname in types:
            anc = tname
            while "/" in anc:
                anc = anc.rsplit("/", 1)[0]
                if anc in types:
                    merged = types[tname]["flags"] | {f for f in types[anc]["flags"] if f in ("ui", "network", "delegate")}
                    if merged != types[tname]["flags"]:
                        types[tname]["flags"] = merged
                        changed = True
    parser = JavaParser()
    parser.parse_dirs(JAVA_DIRS)
    fallback = JavaParser()
    fallback.parse_dirs(FALLBACK_DIRS)
    source_rank = {}
    for rank, (label, d) in enumerate(JAVA_DIRS):
        for jt in parser.types:
            if jt.path.startswith(d):
                source_rank[jt.path] = rank
    for jt in fallback.types:
        source_rank[jt.path] = 5

    by_suffix = build_java_index(parser)
    fb_suffix = build_java_index(fallback)

    results = defaultdict(lambda: {"matched": 0, "missing": []})
    cls_meta = {}
    for tname, info in types.items():
        if "nonpublic" in info["flags"]:
            continue
        chain = pascal_chain(type_chain(tname))
        jt, amb = pick_java_type(by_suffix, chain, source_rank)
        fb = None
        if jt is None:
            fb, _ = pick_java_type(fb_suffix, chain, source_rank)
        cls_meta[tname] = {"info": info, "java": jt, "fallback": fb, "ambiguous": amb,
                           "member_count": len(members.get(tname, []))}
        if jt is None:
            for row in members.get(tname, []):
                category, _ = classify(tname, info, row, row[7] if row[0] == "M" else (row[6] if row[0] in ("F", "P", "E") else ""), False, False)
                if fb is not None:
                    category = "ported in rawr-models"
                note = "type not found in core/data"
                ov = apply_override(tname, row)
                if ov:
                    category, note = ov
                results[tname]["missing"].append((row, category, note))
            continue
        for row in members.get(tname, []):
            kind = row[0]
            if kind == "M":
                _, _, _, name, arity, vis, flags, reach, detail, ret = (row + [""] * 10)[:10]
                kind_s = "ctor" if "ctor" in flags.split(",") else "method"
                if kind_s == "ctor":
                    got = find_ctor(jt, int(arity))
                else:
                    got = chain_find(jt, find_method, name, int(arity))
                    if got is None:
                        alias = member_alias(tname, name, int(arity), ret)
                        if alias is not None:
                            got = chain_find(jt, find_method, alias, int(arity))
            elif kind == "F":
                _, _, name, arity, vis, flags, reach, detail, ftype = (row + [""] * 9)[:9]
                got = chain_find(jt, find_field, name)
                ret = ftype
            elif kind == "P":
                _, _, name, arity, vis, flags, reach, detail, ptype = (row + [""] * 9)[:9]
                got = chain_find(jt, find_property, name, int(arity))
                ret = ptype
            else:
                _, _, name, delegate_type, vis, flags, reach, detail = (row + [""] * 8)[:8]
                got = chain_find(jt, find_event, name)
                ret = delegate_type
            if got is None:
                category, note = classify(tname, info, row, reach, False, False)
                ov = apply_override(tname, row)
                if ov:
                    category, note = ov
                results[tname]["missing"].append((row, category, note))
            else:
                results[tname]["matched"] += 1

    # write TSV report
    os.makedirs(out_dir, exist_ok=True)
    report_tsv = os.path.join(out_dir, "report.tsv")
    counts = defaultdict(int)
    with open(report_tsv, "w", encoding="utf-8") as fh:
        fh.write("class\tkind\tmember\tarity\tclassification\treach\tjava\n")
        for tname in types:
            meta = cls_meta.get(tname) or {"java": None, "fallback": None}
            jt = meta.get("java") or meta.get("fallback")
            for row, category, note in results[tname]["missing"]:
                counts[category] += 1
                kind = row[0]
                name = row[3] if kind in ("M", "F", "P", "E") and kind != "M" else (row[3] if kind == "M" else "")
                if kind == "M":
                    name = row[3] + "(" + row[4] + ")"
                    reach = row[7]
                else:
                    name = row[2]
                    reach = row[6]
                fh.write("\t".join([tname, kind, name, category, reach, jt.qname if jt else ""]) + "\n")
    print("report:", report_tsv)
    print("counts:", dict(counts))
    print("types:", len(types), "java types:", len(parser.types), "fallback:", len(fallback.types))
    total_missing = sum(v["missing"].__len__() for v in results.values())
    total_matched = sum(v["matched"] for v in results.values())
    print("matched members:", total_matched, "missing rows:", total_missing)
    if not tsv_only:
    doc_path = os.path.join(ROOT, "docs", "dev", "CORE_COMPLETENESS.md")
        write_markdown(doc_path, types, cls_meta, results, total_matched,
                       len(parser.types), len(fallback.types))
        print("doc:", doc_path)


if __name__ == "__main__":
    main()
