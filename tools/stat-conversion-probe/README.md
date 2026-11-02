# StatConversion probe (oracle helper)

Generates the bit-exact expected dump for `rawr.base.stats.StatConversion`'s combat table from the
prebuilt legacy assembly, so `rawr-core`'s `StatConversionGoldenTest` can compare against the
original.

- Calls `StatConversion.GetCombatTablePlayer_vs_NPC` for attacker level 80, target levels 80-83,
  dual-wield/white-or-yellow, behind-time 0/0.5/1, hit 0/0.03, expertise 0/0.02 and crit 0/0.1
  (192 cases).
- Output under `jrawr/rawr-core/src/test/resources/stat-conversion/expected.txt` (committed): one
  `== attacker|target|dw|wy|behind|hit|expertise|crit` header per case, then the seven float fields
  as `F <name> <IEEE-754 hex>` and the two flags as `B <name> 0|1`.
- The probe compiles with Mono `mcs` against `legacy/.../bin/Release/Data/Rawr.Base.dll` and runs
  under `mono` with `MONO_PATH` pointing at the data dir. The legacy tree is only read.
- `bin/StatConversionProbe.exe` is ignored by git.

Run (network-free):

```bash
tools/stat-conversion-probe/run.sh
```

The expected file is an oracle artifact: only regenerate it with a documented reason.
