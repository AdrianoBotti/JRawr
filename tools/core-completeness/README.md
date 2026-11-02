# core-completeness — member-level audit vs the shipped Rawr.Base.dll

Extracts every public/protected type and member of the prebuilt legacy `Rawr.Base.dll` (the shipped
app's assembly, C# namespace `Rawr`) and matches it against the Java port in `rawr-core` /
`rawr-data` (`rawr-models` as fallback). The result is `docs/dev/CORE_COMPLETENESS.md` (local,
gitignored) plus `out/report.tsv` (machine readable). Files:

| File | What it is |
|---|---|
| `CoreCompleteness.cs` | Mono.Cecil 0.11.1 tool: member dump + transitive reachability (external references from every shipped assembly, interface/override dispatch, reflection-name strings), UI/network classification. |
| `run.sh` | builds and runs `CoreCompleteness.cs` -> `out/dll-members.tsv`. Read-only over `legacy/`. |
| `match.py` | Java source parser/matcher; writes `out/report.tsv` and `docs/dev/CORE_COMPLETENESS.md`. |
| `probe/CompletenessProbe.cs` + `probe/run.sh` | oracle that produces `jrawr/rawr-core/src/test/resources/core-completeness/expected.txt` for the members closed by the audit (SpecialFunction.Normal values, Stats.StatNames, Item slot maps, Character slot ids). |
| `out/` | generated (committed for traceability): `dll-members.tsv`, `report.tsv`. |

## Usage

```bash
tools/core-completeness/run.sh                 # DLL -> out/dll-members.tsv
python3 tools/core-completeness/match.py       # -> out/report.tsv + docs/dev/CORE_COMPLETENESS.md
tools/core-completeness/probe/run.sh           # regenerate the oracle expected.txt (documented reason only)
```

`match.py` tolerates Java-specific naming: camelCase renames, `_name` -> `name`, `InBackPerc_Melee`
-> `getInBackPercMelee`, `ATTACK_TYPES` -> `AttackTypes`, `GetClass` -> `getCharacterClass`,
inherited members, getters/setters vs public fields, records and enums. Members it cannot resolve
are classified with a curated override table in `match.py` (UI/designer, network parked, dead with
IL evidence, serialization shims, relocated/replaced API, `parallel optimizer agent-owned`), and
anything left is a `REAL GAP`.

Known limits: reachability is conservative (virtual/interface/potential-reflection members are never
called dead); return types are not compared member-by-member; the Java sources are parsed textually,
so a member the parser mis-reads would show up as `REAL GAP` for manual review.
