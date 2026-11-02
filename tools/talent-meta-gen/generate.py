#!/usr/bin/env python3
"""Generates jrawr/rawr-web/src/main/resources/talent-meta.json.

The Java talent annotations carry the layout (index/name/tree/row/column/prerequisite) but omit
the C# original's UI-only icon and per-rank description arrays. This tool joins the active Java
annotations with the legacy C# attributes and writes the UI metadata used by the web talent view.

Run from the repository root:
    python3 tools/talent-meta-gen/generate.py
    python3 tools/talent-meta-gen/generate.py --check
"""
import json
import os
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
JAVA_DIR = ROOT / "jrawr/rawr-core/src/main/java/rawr/base/talents"
LEGACY_ROOT = pathlib.Path(os.environ.get("RAWR_LEGACY_DIR", ROOT / "legacy/RAWR_WotLK_3.3.5a"))
LEGACY_DIR = LEGACY_ROOT / "Rawr.Base"
OUTPUT = ROOT / "jrawr/rawr-web/src/main/resources/talent-meta.json"

CLASSES = [
    "Warrior", "Paladin", "Hunter", "Rogue", "Priest", "DeathKnight",
    "Shaman", "Mage", "Warlock", "Druid",
]

JAVA_TALENT_RE = re.compile(
    r'@TalentDataAttribute\(index = (\d+), name = "([^"]+)", maxPoints = \d+, '
    r'tree = (\d+), column = (\d+), row = (\d+), prerequisite = (-?\d+)\)'
)
JAVA_GLYPH_RE = re.compile(
    r'@GlyphDataAttribute\(index = (\d+), name = "([^"]+)", major = (true|false)\)'
)

CS_TALENT_RE = re.compile(
    r'\[TalentData\(\s*(\d+),\s*"([^"]+)",\s*\d+,\s*[^,]+,\s*[^,]+,\s*[^,]+,\s*-?\d+,'
    r'\s*new\[\]\s*\{(.*?)\},\s*"([^"]+)"\)\]',
    re.S,
)
CS_GLYPH_RE = re.compile(
    r'\[GlyphData\(\s*(\d+),\s*"([^"]+)",\s*(true|false),\s*(.*?)\)\]',
    re.S,
)
CS_CLASS_RE = re.compile(r'public partial class (\w+Talents)(?!\w)\s*(?::\s*TalentsBase)?')


def fail(message):
    raise SystemExit("talent-meta-gen: " + message)


def strip_comments(text):
    text = re.sub(r'/\*.*?\*/', '', text, flags=re.S)
    return "\n".join(
        line for line in text.splitlines()
        if not line.lstrip().startswith("//")
    )


STRING_RE = re.compile(r'@"((?:[^"]|"")*)"|"((?:[^"\\]|\\.)*)"')
ESCAPES = {"n": "\n", "r": "\r", "t": "\t", "\\": "\\", '"': '"', "'": "'"}


def unescape_normal(value):
    out = []
    i = 0
    while i < len(value):
        if value[i] == "\\" and i + 1 < len(value):
            out.append(ESCAPES.get(value[i + 1], value[i + 1]))
            i += 2
        else:
            out.append(value[i])
            i += 1
    return "".join(out)


def extract_strings(text):
    values = []
    for verbatim, normal in STRING_RE.findall(text):
        if verbatim:
            values.append(verbatim.replace('""', '"'))
        else:
            values.append(unescape_normal(normal))
    return values


def split_elements(text):
    """Splits a C# array initializer body on top-level commas (strings/escapes respected)."""
    parts = []
    buffer = []
    state = None
    i = 0
    while i < len(text):
        char = text[i]
        if state is None:
            if char == "@" and i + 1 < len(text) and text[i + 1] == '"':
                state = "verbatim"
                buffer.append('@"')
                i += 2
                continue
            if char == '"':
                state = "normal"
            if char == ",":
                parts.append("".join(buffer))
                buffer = []
                i += 1
                continue
            buffer.append(char)
            i += 1
        elif state == "verbatim":
            buffer.append(char)
            if char == '"':
                if i + 1 < len(text) and text[i + 1] == '"':
                    buffer.append('"')
                    i += 2
                    continue
                state = None
            i += 1
        else:
            buffer.append(char)
            if char == "\\" and i + 1 < len(text):
                buffer.append(text[i + 1])
                i += 2
                continue
            if char == '"':
                state = None
            i += 1
    if buffer:
        parts.append("".join(buffer))
    return parts


def descriptions(text):
    return ["".join(extract_strings(element)) for element in split_elements(text) if element.strip()]


def java_annotations(java_class):
    path = JAVA_DIR / (java_class + "Talents.java")
    if not path.is_file():
        fail(f"missing {path.relative_to(ROOT)}")
    text = path.read_text(encoding="utf-8")
    talents = {
        int(index): name
        for index, name, _tree, _column, _row, _prerequisite in JAVA_TALENT_RE.findall(text)
    }
    glyphs = {
        int(index): (name, major == "true")
        for index, name, major in JAVA_GLYPH_RE.findall(text)
    }
    return talents, glyphs


def class_sections(text):
    matches = list(CS_CLASS_RE.finditer(text))
    sections = {}
    for i, match in enumerate(matches):
        end = matches[i + 1].start() if i + 1 < len(matches) else len(text)
        sections[match.group(1)] = strip_comments(text[match.start():end])
    return sections


def build_class(name, talent_sections, glyph_sections):
    java_talents, java_glyphs = java_annotations(name)
    talent_text = talent_sections.get(name + "Talents")
    glyph_text = glyph_sections.get(name + "Talents")
    if talent_text is None:
        fail(f"missing C# section for {name}Talents")
    if glyph_text is None:
        fail(f"missing C# glyph section for {name}Talents")

    cs_talents = {}
    for index, cs_name, desc_text, icon in CS_TALENT_RE.findall(talent_text):
        cs_talents.setdefault(int(index), []).append((cs_name, descriptions(desc_text), icon))
    cs_glyphs = {}
    for index, cs_name, _major, expression in CS_GLYPH_RE.findall(glyph_text):
        cs_glyphs.setdefault(int(index), []).append((cs_name, "".join(extract_strings(expression))))

    problems = []
    talents = {}
    for index in sorted(java_talents):
        java_name = java_talents[index]
        candidates = [entry for entry in cs_talents.get(index, []) if entry[0] == java_name]
        if not candidates:
            problems.append(f"{name} talent {index} {java_name!r}: no matching C# attribute")
            continue
        _, desc, icon = candidates[0]
        talents[str(index)] = {"icon": icon, "descriptions": desc}
    glyphs = {}
    for index in sorted(java_glyphs):
        java_name, _major = java_glyphs[index]
        candidates = [entry for entry in cs_glyphs.get(index, []) if entry[0] == java_name]
        if not candidates:
            problems.append(f"{name} glyph {index} {java_name!r}: no matching C# attribute")
            continue
        _, desc = candidates[0]
        glyphs[str(index)] = {"description": desc}
    return {"talents": talents, "glyphs": glyphs}, problems


def generate_text():
    talent_sections = class_sections((LEGACY_DIR / "ClassTalents.cs").read_text(encoding="utf-8"))
    glyph_sections = class_sections((LEGACY_DIR / "ClassGlyphs.cs").read_text(encoding="utf-8"))
    data = {}
    problems = []
    for name in CLASSES:
        data[name], class_problems = build_class(name, talent_sections, glyph_sections)
        problems.extend(class_problems)
    if problems:
        for problem in problems:
            print("WARN", problem, file=sys.stderr)
    return json.dumps(data, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n"


def main(argv):
    text = generate_text()
    if "--check" in argv:
        current = OUTPUT.read_text(encoding="utf-8") if OUTPUT.exists() else ""
        if current != text:
            fail(f"{OUTPUT.relative_to(ROOT)} is out of date; run tools/talent-meta-gen/generate.py")
        print("up to date:", OUTPUT.relative_to(ROOT))
        return 0
    OUTPUT.write_text(text, encoding="utf-8")
    print("wrote", OUTPUT.relative_to(ROOT), f"({len(text) // 1024} KiB)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
