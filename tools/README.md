# Development tools

These tools are not needed to build, test or run JRawr. They regenerate checked-in oracle artifacts
and development data, or audit the Java port against the original C# assembly. Most of them read the
legacy snapshot and run under Mono.

## Legacy snapshot

Create the read-only C# checkout first:

```bash
tools/setup-legacy.sh
```

It clones
[JRawrOriginalRawrSnapshot](https://github.com/AdrianoBotti/JRawrOriginalRawrSnapshot) at tag
`jrawr-oracle-1` into `legacy/RAWR_WotLK_3.3.5a` and verifies commit
`12f9030ca85d62e854581eec340d309d062c8e2b`.

The shell and Python tools read `RAWR_LEGACY_DIR` when it is set; otherwise they use
`legacy/RAWR_WotLK_3.3.5a` under the repository root. `legacy/` is read-only: never edit it and
never run MSBuild or xbuild in it, because a rebuild deletes the prebuilt DLLs the oracle needs.

Requirements for these tools: Mono (`mcs`, `mono`), optionally Wine, and Python 3 for the auditing
scripts. Normal JRawr builds and tests need only Java 25.

## Tools

| Tool | Purpose |
|---|---|
| `setup-legacy.sh` | Clone and verify the C# port snapshot used by every oracle tool. |
| `base-stats-probe` | Regenerate `rawr-core`'s `BaseStats` expected dump (levels, classes, races, Druid forms). |
| `stat-conversion-probe` | Regenerate the combat-table expected dump for `StatConversion`. |
| `special-effects-probe` | Regenerate the special-effect parser expected dump. |
| `core-format-probe` | Regenerate the `ToString` and XML-writer expected artifacts. |
| `talent-meta-gen` | Generate and validate `rawr-web`'s talent icon and per-rank description metadata from the Java and C# talent annotations. |
| `set-bonus-gen` | Regenerate the committed set-bonus prose lookup used by item tooltips. It needs network access to an AoWoW-style item database. |
| `core-completeness` | Member-level audit of the Java port against the shipped `Rawr.Base.dll`; writes the TSV report and the completeness docs. |

Each tool has its own README with the exact command and output paths.

## Regenerating oracle artifacts

The expected files under `jrawr/*/src/test/resources/` and the golden JSON under `oracle/golden/`
are oracle artifacts. Regenerate them only for a documented reason, and commit the regenerated
artifact together with the change that requires it. Do not regenerate them just to make a failing
test pass; a mismatch is usually a port bug.

The full golden refresh is driven by `oracle/`:

```bash
cd oracle
./build.sh
./run.sh mage
./run.sh cat
./run.sh optimize
```

See [oracle/README.md](../oracle/README.md) for the per-model commands.
