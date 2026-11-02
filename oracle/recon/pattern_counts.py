#!/usr/bin/env python3
"""Phase 0 step 8: count risky C# patterns repo-wide and in the Mage closure."""
import os
import re
import sys

ROOT = os.path.abspath(os.environ.get(
    "RAWR_LEGACY_DIR",
    os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "legacy", "RAWR_WotLK_3.3.5a"),
))

MAGE_ENGINE = [
    "Rawr.Mage/ArrayPool.cs", "Rawr.Mage/CalculationOptionsMage.cs", "Rawr.Mage/CalculationsMage.cs",
    "Rawr.Mage/CastingState.cs", "Rawr.Mage/CharacterCalculationsMage.cs", "Rawr.Mage/ComparisonCalculationMage.cs",
    "Rawr.Mage/Encounter.cs", "Rawr.Mage/FSRCalc.cs", "Rawr.Mage/Heap.cs", "Rawr.Mage/LP.cs",
    "Rawr.Mage/LU.cs", "Rawr.Mage/ListUtils.cs", "Rawr.Mage/Sequence.cs", "Rawr.Mage/SequenceGroup.cs",
    "Rawr.Mage/SequenceItem.cs", "Rawr.Mage/Solver.MIP.cs", "Rawr.Mage/Solver.cs", "Rawr.Mage/SolverLP.cs",
    "Rawr.Mage/SparseMatrix.cs", "Rawr.Mage/SpellInfo/Cycle.cs", "Rawr.Mage/SpellInfo/CycleDataArcane.cs",
    "Rawr.Mage/SpellInfo/CycleDataDeprecated.cs", "Rawr.Mage/SpellInfo/CycleDataFire.cs",
    "Rawr.Mage/SpellInfo/CycleDataFrost.cs", "Rawr.Mage/SpellInfo/Spell.cs", "Rawr.Mage/SpellInfo/SpellData.cs",
    "Rawr.Mage/SpellInfo/SpellTemplate.cs", "Rawr.Mage/StateDescriptionParseTree.cs",
    "Rawr.Mage/StateDescriptionParser.cs", "Rawr.Mage/StateDescriptionScanner.cs",
]
# closure_engine2.txt mapped 19 files + SerializableDictionary (needed via Character.LoadCalculationOptions)
BASE_CLOSURE = [
    "Rawr.Base/ArrayUtils.cs", "Rawr.Base/Buff.cs", "Rawr.Base/Calculations.cs", "Rawr.Base/Character.cs",
    "Rawr.Base/ClassGlyphs.cs", "Rawr.Base/ClassTalents.cs", "Rawr.Base/Enchant.cs", "Rawr.Base/EnumHelper.cs",
    "Rawr.Base/Enums.cs", "Rawr.Base/GemmingTemplate.cs", "Rawr.Base/ICharacterCalculationOptions.cs",
    "Rawr.Base/Item.cs", "Rawr.Base/Log.cs", "Rawr.Base/SpecialEffect.cs", "Rawr.Base/SpecialEffectCombination.cs",
    "Rawr.Base/SpecialEffectInterpolator.cs", "Rawr.Base/SpecialFunction.cs", "Rawr.Base/StatConversion.cs",
    "Rawr.Base/Stats.cs", "Rawr.Base/SerializableDictionary.cs", "Rawr.Base/Properties/GeneralSettings.Designer.cs",
]

PATTERNS = {
    "Math.Round(": r"Math\.Round\s*\(",
    "MidpointRounding": r"MidpointRounding",
    "decimal": r"\bdecimal\b",
    "uint|ulong|ushort|byte": r"\b(?:uint|ulong|ushort|byte)\b",
    "struct decl": r"\b(?:public|internal|private|protected)?\s*(?:partial\s+)?struct\s+[A-Za-z_]",
    "Dictionary<": r"\bDictionary\s*<",
    "HashSet<": r"\bHashSet\s*<",
    "foreach": r"\bforeach\s*\(",
    ".Sort(": r"\.Sort\s*\(",
    "Parallel.": r"\bParallel\.",
    "Task/Thread": r"\bTask\b|\bThread\b",
    "lock (": r"\block\s*\(",
    "static field-ish": r"^\s*(?:public|private|protected|internal|static|readonly|volatile|\s)*\bstatic\b\s+(?!class|void|bool\s+\w+\s*\(|[A-Za-z_<>\[\],\.]+\s+\w+\s*\()[A-Za-z_]",
    "static readonly": r"\bstatic\s+readonly\b",
    "Assembly": r"\bAssembly\b",
    "Activator": r"\bActivator\b",
    "GetType(": r"\.GetType\s*\(",
    "XmlSerializer": r"\bXmlSerializer\b",
    "unsafe": r"\bunsafe\b",
    "fixed (": r"\bfixed\s*\(",
    "stackalloc": r"\bstackalloc\b",
    "double* / float*": r"\b(?:double|float|int|byte)\s*\*",
    "DllImport": r"\bDllImport\b",
    "Marshal.": r"\bMarshal\.",
}


def count_files(files, regexes):
    total = {k: 0 for k in regexes}
    per_file = {}
    for rel in files:
        path = os.path.join(ROOT, rel)
        if not os.path.exists(path):
            print("MISSING", rel)
            continue
        txt = open(path, encoding="utf-8", errors="replace").read()
        c = {k: len(re.findall(rx, txt, re.M)) for k, rx in regexes.items()}
        per_file[rel] = c
        for k in total:
            total[k] += c[k]
    return total, per_file


def walk_all_cs():
    out = []
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames if d not in (".git", "bin", "obj")]
        for f in filenames:
            if f.endswith(".cs"):
                out.append(os.path.relpath(os.path.join(dirpath, f), ROOT))
    return sorted(out)


def main():
    all_cs = walk_all_cs()
    print(f"whole repo: {len(all_cs)} .cs files")
    t, _ = count_files(all_cs, PATTERNS)
    tc, _ = count_files(MAGE_ENGINE + BASE_CLOSURE, PATTERNS)
    print(f"\n{'pattern':28s} {'whole repo':>11s} {'mage closure':>13s}")
    for k in PATTERNS:
        print(f"{k:28s} {t[k]:11d} {tc[k]:13d}")
    # per-file top static/Parallel for solver risk
    _, pf = count_files(MAGE_ENGINE, PATTERNS)
    print("\n== Mage engine per-file highlights (Parallel/Task, lock, unsafe, double*, static) ==")
    for rel in MAGE_ENGINE:
        c = pf[rel]
        if c["Parallel."] or c["lock ("] or c["Task/Thread"] or c["unsafe"] or c["double* / float*"] or c["static field-ish"]:
            print(f"  {rel:45s} par={c['Parallel.']:2d} lock={c['lock (']:2d} task={c['Task/Thread']:2d} unsafe={c['unsafe']:3d} ptr={c['double* / float*']:3d} static={c['static field-ish']:3d}")


if __name__ == "__main__":
    main()
