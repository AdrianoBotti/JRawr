# BaseStats probe (oracle helper)

Generates the bit-exact expected dump for the ported `rawr.base.stats.BaseStats` from the prebuilt
legacy assembly, so `rawr-core`'s `BaseStatsGoldenTest` can compare against the original.

- Calls `BaseStats.GetBaseStats(level, class, race, form)` for levels 1 and 80, every class/race
  combination, all four Druid forms (Caster only for the other classes).
- Output under `jrawr/rawr-core/src/test/resources/base-stats/expected.txt` (committed): one
  `== level|class|race|form` header per combination, then every non-zero public float stat as
  `S <name> <IEEE-754 hex>` and every special effect as `E <path> trigger=... duration=...` with
  its nested stats, recursively.
- The probe compiles with Mono `mcs` against `legacy/.../bin/Release/Data/Rawr.Base.dll` and runs
  under `mono` with `MONO_PATH` pointing at the data dir. The legacy tree is only read.
- `bin/BaseStatsProbe.exe` is ignored by git.

Run (network-free):

```bash
tools/base-stats-probe/run.sh
```

The expected file is an oracle artifact: only regenerate it with a documented reason.
