# Core-format probe (oracle helper)

Generates the bit-exact expected artifacts for the ported core `ToString` methods and the DOM
writers from the prebuilt legacy assembly, so `rawr-core`'s tests can compare against the original.

- Input: `jrawr/rawr-core/src/test/resources/core-format/vectors.txt` (committed; the format is
  documented at the top of that file).
- Output under `jrawr/rawr-core/src/test/resources/core-format/` (committed):
  - `expected.txt`: `Stats.ToString`, `SpecialEffect.ToString`, `Buff.ToString` and
    `Enchant.ToString` for every vector; a line that throws in the original is recorded as
    `X throw <Exception>`.
  - `buff-cache.xml`: `XmlSerializer(typeof(BuffList))` of a constructed buff list.
  - `item-filter.xml`: `XmlSerializer(typeof(ItemFilterData))` of a constructed filter data tree.
  - `item-source.xml`: `XmlSerializer(typeof(ItemLocationDictionary))` of a constructed location
    table (every `xsi:type`, null entries, token maps, notes).
- The probe compiles with Mono `mcs` against `legacy/.../bin/Release/Data/Rawr.Base.dll` and runs
  under `mono` with `MONO_PATH` pointing at the data dir. The legacy tree is only read.

Run (network-free):

```bash
tools/core-format-probe/run.sh
```

The expected files are oracle artifacts: only regenerate them with a documented reason, and commit
the vectors and the regenerated dumps together. `CoreFormatProbeTest` replays `vectors.txt`;
`BuffSerializationTest`/`ItemFilterSerializationTest`/`LocationFactorySerializationTest` compare the
Java writers canonically (attributes sorted, whitespace ignored) against the XML dumps.

Note: the probe emits `bin/CoreFormatProbe.exe`, which is ignored by git.
