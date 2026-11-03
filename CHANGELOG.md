# Changelog

All notable changes to JRawr are recorded here. The project follows a simple
`MAJOR.MINOR.PATCH` version.

## [1.0.2] - 2026-11-03

### Web application

- Fixed paper-doll, stats, set-bonus and chart layouts at narrow and short viewport sizes.
- Chart candidate lists now scroll inside the charts card, and item tooltips remain correctly
  positioned.
- Multiple active set bonuses are shown as one clearly aligned chip per set.
- Topbar action labels collapse to icons on narrow windows.

## [1.0.1] - 2026-11-02

Bundled Java runtime release.

- Added `JRawr-1.0.1-linux-x64.zip`, `JRawr-1.0.1-linux-x64.tar.gz` and
  `JRawr-1.0.1-windows-x64.zip`, which include Eclipse Temurin JRE 25.0.5+8 and need no separate
  Java installation.
- Launchers now prefer `JRAWR_JAVA`, then the bundled `runtime/`, then `JAVA_HOME`, then `java`
  on `PATH`.
- The universal archives remain available for users who already have Java 25.
- The bundled JRE is pinned in `jrawr/gradle/temurin.properties`; `./gradlew releaseBundled`
  produces the platform archives.
- Added Temurin/OpenJDK attribution to the notices and documented the bundled runtime in the
  README and release guide.

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
