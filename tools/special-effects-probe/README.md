# SpecialEffects probe (oracle helper)

Generates the bit-exact expected dump for the ported `Rawr.Base.SpecialEffects` parsers from the
prebuilt legacy assembly, so `rawr-core`'s `SpecialEffectsTest` can compare against the original.

- Input: `jrawr/rawr-core/src/test/resources/special-effects/vectors.txt` (committed; the format is
  documented at the top of that file).
- Output: `jrawr/rawr-core/src/test/resources/special-effects/expected.txt` (committed).
- The probe compiles with Mono `mcs` against `legacy/.../bin/Release/Data/Rawr.Base.dll` and runs
  under `mono` with `MONO_PATH` pointing at the data dir. The legacy tree is only read.

Run (network-free):

```bash
tools/special-effects-probe/run.sh
```

The expected file is an oracle artifact: only regenerate it with a documented reason, and commit
both the vectors and the regenerated dump together. The test compares every non-zero `Stats` float
as IEEE-754 hex bits plus each effect's trigger/duration/cooldown/chance/maxStack and nested stats;
a line that throws in the original is recorded as `X throw` (partial stats included) and must throw
in Java too.

Note: the probe emits `bin/SpecialEffectsProbe.exe`, which is ignored by git.
