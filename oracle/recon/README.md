# oracle/recon — porting reconnaissance tooling

Analysis tools and their outputs, kept for reference and for re-running the same checks on a model.

| File | What it is |
|---|---|
| `ClosureAnalyzer.cs` | C# tool (Mono.Cecil 0.11) that computes the member-level transitive dependency closure of a model assembly over `Rawr.Base.dll`. Build: `mcs -r:/usr/lib/mono/gac/Mono.Cecil/0.11.1.0__0738eb9f132ed756/Mono.Cecil.dll ClosureAnalyzer.cs`. Run: `mono ClosureAnalyzer.exe <bin/Release/Data> [skipTypesCsv] [pruneMemberPrefixesCsv]` |
| `mage_closure_members.txt` | Output used for the Mage porting closure: engine closure (UI panels and `Item.LoadFromId` fallback pruned). Lines `T` types, `M` members with provenance, `R` type reasons. |
| `mage_closure_full_ui.txt` | Same tool without pruning (all Mage code including UI): 102 Base types. |
| `analyze_csproj.py` | Parses all 76 `.csproj`, builds the reference graph and the linked/duplicate file map. |
| `classify_closure.py` | Maps closure types/members to source files and resolves provenance to Mage seed methods. |
| `pattern_counts.py` | Counts the risky C# patterns listed in the porting rules. |

The scripts read `RAWR_LEGACY_DIR` and otherwise use `legacy/RAWR_WotLK_3.3.5a` under the
repository root. Create that checkout with `tools/setup-legacy.sh`. The expected bin path is
`<legacy>/bin/Release/Data/`.

The detailed porting notes are development-only and are not part of the published documentation.
