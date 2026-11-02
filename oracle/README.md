# Oracle: golden-master fixtures and C# dump tools

This directory holds the input fixtures and the golden JSON used by `rawr-golden` to verify that
the Java engine produces the same numbers as the original C# engine. It also contains the small
C# tools that regenerate the fixtures and golden files.

The oracle covers all 20 class/spec models, the fixed-pool optimizer and relative-stat charts.
All model suites are implemented and passing.

## Contents

```
oracle/
  build.sh              compile FixtureGen.exe and DumpTool.exe with Mono's mcs
  run.sh                regenerate fixtures and/or golden JSON for one model
  fixture-gen/          C# fixture generator (Mage plus the per-model generators)
  dump-tool/            C# dump tool; writes the golden JSON
  recon/                porting reconnaissance tools and outputs
  fixtures/<model>/     input character files in the original Rawr XML format
  golden/<model>/       one golden JSON per fixture
  golden/optimizer/     fixed-pool optimizer goldens (25 fixtures; no fixture XML)
  golden/relative-stats/<model>/  relative-stat chart goldens
```

Current counts: 115 fixture XML files, 145 golden JSON files across the model directories, plus the
optimizer and relative-stat goldens. Mage has 13 fixtures.

## Requirements

The C# tools run against the **prebuilt** legacy assemblies and the WotLK data caches:

- Mono 6.x (`mono`, `mcs`), or a compatible Mono installation.
- The legacy snapshot from
  [JRawrOriginalRawrSnapshot](https://github.com/AdrianoBotti/JRawrOriginalRawrSnapshot), tag
  `jrawr-oracle-1`, checked out at commit
  `12f9030ca85d62e854581eec340d309d062c8e2b`.
- Set `RAWR_LEGACY_DIR` to point at that checkout, or use the default
  `legacy/RAWR_WotLK_3.3.5a`.

Create the checkout with:

```bash
tools/setup-legacy.sh
```

Normal JRawr builds and tests do not need Mono, Wine or this directory. Only oracle regeneration
does.

**Never run MSBuild or xbuild against `legacy/`.** Rebuilding the original projects deletes the
prebuilt `Rawr.Base.dll` and its config from `bin/Release/Data`, which the dump tools need. The C#
sources cannot be rebuilt in this environment (Mono's `mcs` cannot compile every C# 7 construct in
the fork).

## Regenerate

```bash
cd oracle
./build.sh
./run.sh mage
```

The model argument accepts:

`mage`, `restosham`, `cat`, `bear`, `healadin`, `protwarr`, `moonkin`, `elemental`,
`retribution`, `enhance`, `protpally`, `healpriest`, `shadowpriest`, `hunter`, `rogue`, `tree`,
`tankdk`, `warlock`, `dpsdk`, `dpswarr`, `relative-stats`, `optimize`.

Each model refresh regenerates that model's fixtures and golden JSON. `relative-stats` regenerates
the chart goldens and `optimize` regenerates the fixed-pool optimizer goldens (no fixture
regeneration). The optimizer C# search uses a time-based seed, so a re-dump is not byte-identical;
the committed optimizer goldens are the oracle, and the seed in each JSON is recorded for the Java
side.

Oracle artifacts are committed. Regenerate them only for a documented reason and commit the
regenerated files with the change that requires them. Do not regenerate to make a failing test
pass; a mismatch usually indicates a port bug.

## Golden format

The dump tool writes invariant-culture, round-trippable JSON. Model goldens contain the ratings,
the resulting `Stats`, displayed calculation values and a comparison chart. Mage goldens also
contain solver intermediates (candidate cycles, chosen solution, mana segments) so a diff can be
localized. Optimizer goldens contain the fixed item pool and the original best score; the Java
side must match or exceed that score within tolerance, not the chosen item set.
