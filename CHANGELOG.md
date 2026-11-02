# Changelog

All notable changes to JRawr are recorded here. The project follows a simple
`MAJOR.MINOR.PATCH` version.

## [1.0.0] - 2026-11-02

First public release.

### Engine

- Ported the Rawr v2.3.23.0 calculation engine from C# to Java 25.
- Ported all 20 WotLK class/spec models: Bear, Cat, DPSDK, DPSWarr, Elemental, Enhance,
  HealPriest, Healadin, Hunter, Mage, Moonkin, ProtPaladin, ProtWarr, RestoSham, Retribution,
  Rogue, ShadowPriest, TankDK, Tree and Warlock.
- Ported the gear optimizer, candidate generation, set bonuses, special effects, talents, glyphs,
  buffs, enchants, gems and character XML loading and saving.
- Replaced runtime reflection model discovery with an explicit registry.

### Web application

- New local Spring Boot, Thymeleaf and htmx application.
- Character editor for gear, gems, enchants, talents, glyphs, buffs, professions, boss options and
  per-model calculation options.
- Item, gem and enchant pickers with rating deltas, availability and tooltips.
- Per-slot comparison charts and relative-stat charts.
- Gear optimizer with bounded jobs, server-sent progress, cancellation and save-as.
- Item editor for fetching items by ID, refreshing or removing user items, and creating custom item
  copies with sockets, stats and special effects.
- Item filters, gemming templates, item costs, saved talent specs and custom item pools.
- Original Rawr character XML import and export.
- Item icons are hotlinked; the server does not cache or redistribute them.

### Verification and tooling

- Golden-master verification against the original C# binaries for every model, the fixed-pool
  optimizer and relative-stat calculations.
- Fresh-JVM golden tasks and parallel verification for deterministic models.
- Release packaging for Linux and Windows with a bundled data directory, writable user storage and
  production resource caching.
- Linux and Windows CI, tag-driven release workflow and SHA-256 checksums.
- The original C# port source and oracle tools are published separately in the
  `JRawrOriginalRawrSnapshot` repository at tag `jrawr-oracle-1`.

### Known limitations

- htmx is loaded from a CDN, so the browser interface requires internet access.
- Adding items by ID and fetching missing items require network access to the selected item source.
- The Armory and Wowhead character importers from the original application are not ported.
- The application is a single-user local tool without accounts, TLS or a database.
- Game data is fixed at Warcraft 3.3.5a.
