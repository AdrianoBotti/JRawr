# set-bonus-gen

Dev-time generator for the committed artifact
`jrawr/rawr-web/src/main/resources/set-bonus-text.json`.

## What this is

`BuffCache.xml` only stores the set thresholds and internal stat flags of a set bonus (for example
`Mage2T10 = 1`), not the real in-game tooltip prose. The web item tooltip wants the prose
("Your Hot Streak, Missile Barrage, and Brain Freeze talents also grant you 12% haste ...").

`GenSetBonusText.java` builds a lookup `{ "<set name>": { "<threshold>": "<prose>" } }` by fetching
one item tooltip per set from an AoWoW database. The result is committed, so the application,
tests and build never touch the network.

## Provenance

- Generator run on 2026-10-10 from the JRawr repo root (Java 25.0.4.1).
- Default source `https://wotlkdb.com` was tried first: it answers `?item=<id>&xml` with a valid
  AoWoW tooltip but without any set-bonus lines (its itemset pages do not exist).
- Per the documented fallback, generation used `https://wotlk.evowow.com` (same AoWoW format) for
  all sets. It resolved 84 of 86 sets; `Garona's Battlegear` and `Koltira's Plate` failed because
  no item with that set name exists in `ItemCache.xml`.
- `ItemTooltipService` falls back to the bonus buff's name whenever the artifact lacks a set or a
  threshold, so the two failed sets still render (as buff names) if an item ever appears.

## Regenerate

```bash
# from the JRawr repo root, after: (cd jrawr && ./gradlew :rawr-core:classes :rawr-data:classes)
java -cp jrawr/rawr-core/build/classes/java/main:jrawr/rawr-data/build/classes/java/main \
     tools/set-bonus-gen/GenSetBonusText.java [baseUrl] [outFile]
```

- `baseUrl` defaults to `https://wotlkdb.com`, `outFile` to
  `jrawr/rawr-web/src/main/resources/set-bonus-text.json`.
- Game data comes from `-Djrawr.data-dir=<dir>` (default
  `legacy/RAWR_WotLK_3.3.5a/bin/Release/Data`).
- Network requests use a browser User-Agent with a ~400 ms delay and one retry. Raw responses are
  cached under `/tmp/opencode/set-bonus-cache/`, so re-runs are fast and offline; delete that
  directory to force refetching.
- The command prints sets total / resolved (primary vs. fallback) / failed with the failed names,
  plus three sample lines for the final message.

## Offline guarantee

The generator is not part of the Gradle build and is never invoked by tests or the build. Only the
generated `set-bonus-text.json` is committed and loaded from the classpath by `rawr-web`
(DECISIONS #36).
