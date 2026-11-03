# JRawr

[![build](https://github.com/AdrianoBotti/JRawr/actions/workflows/ci.yml/badge.svg)](https://github.com/AdrianoBotti/JRawr/actions/workflows/ci.yml)

JRawr is a port of **Rawr v2.3.23.0**, the World of Warcraft: Wrath of the Lich King (3.3.5a)
gear theorycrafting and optimization tool, from C# to **Java 25**. It runs as a local Spring Boot
web application with a Thymeleaf + htmx interface.

Version 1.0.2 ports the calculation engine, all 20 class/spec models, the gear optimizer and the
character editor. The original desktop, Silverlight and WPF interfaces are not ported; JRawr has a
new browser interface.

See the [User Guide](https://github.com/AdrianoBotti/JRawr/wiki) for installation, the build editor,
charts, optimizer and troubleshooting.

![JRawr build editor](docs/images/build-gear.png)

## Quick start

Download the archive for your platform from
[Releases](https://github.com/AdrianoBotti/JRawr/releases/latest):

- `JRawr-1.0.2-linux-x64.zip` or `JRawr-1.0.2-linux-x64.tar.gz` includes a Temurin JRE 25. No
  separate Java installation is needed.
- `JRawr-1.0.2-windows-x64.zip` includes a Temurin JRE 25. No separate Java installation is
  needed.
- `JRawr-1.0.2.zip` and `JRawr-1.0.2.tar.gz` are the universal archives; they require Java 25 on
  your machine.

1. Extract the archive.
2. Run `./jrawr.sh` on Linux or `jrawr.bat` on Windows.
3. Open <http://127.0.0.1:8080/>.

The game data is bundled in `data/`. Characters, saved talent specs, item filters and item costs
are written to the user data directory:

- Linux: `${XDG_DATA_HOME:-$HOME/.local/share}/jrawr`
- Windows: `%LOCALAPPDATA%\JRawr`

The launcher binds to `127.0.0.1` by default. Use `JRAWR_ADDRESS` and `JRAWR_PORT` to change the
address or port, and `JAVA_OPTS` for JVM options.

## Java runtime

The platform archives bundle Eclipse Temurin JRE 25.0.5+8 under `runtime/`. The launchers choose
Java in this order:

1. `JRAWR_JAVA`, if set.
2. The bundled `runtime/bin/java` (or `runtime\bin\java.exe`).
3. `JAVA_HOME/bin/java`.
4. `java` on `PATH`.

To use your own Java 25 build, point `JRAWR_JAVA` at the `java` executable:

```bash
JRAWR_JAVA=/path/to/java25/bin/java ./jrawr.sh
```

On Windows:

```bat
set JRAWR_JAVA=C:\path\to\java25\bin\java.exe
jrawr.bat
```

You can also replace the `runtime` directory with another Java 25 runtime, or delete it to fall
back to `JAVA_HOME` or `PATH`. The universal archive has no `runtime` and always uses your own
Java 25. The bundled JRE is pinned in `jrawr/gradle/temurin.properties`; JRawr releases pick up JRE
updates. Use `JRAWR_JAVA` if you need a vendor-specific or newer Java 25 build.

## Features

- All 20 WotLK class/spec models: Bear, Cat, DPSDK, DPSWarr, Elemental, Enhance, HealPriest,
  Healadin, Hunter, Mage, Moonkin, ProtPaladin, ProtWarr, RestoSham, Retribution, Rogue,
  ShadowPriest, TankDK, Tree and Warlock.
- Character editor for race, gear, gems, enchants, talents, glyphs, buffs, professions, boss
  options and per-model calculation options.
- Item, gem and enchant pickers with rating deltas, availability checks and socket handling.
- Per-slot comparison charts and a relative-stats view.
- Gear optimizer with selectable slots, item pools, gemming and enchant search, talent and glyph
  searches, consumable search, constraints, bounded jobs, live progress and cancellation.
- Item editor for fetching items by ID, maintaining user items and creating custom item copies.
- Original Rawr character XML import and export, plus a companion in-game exporter addon.
- Golden-master verification against the original C# engine for every model, plus optimizer and
  relative-stats checks.

The application is a single-user local tool. It has no authentication and no database; do not
expose it to untrusted networks.

## Build from source

Requirements: Java 25. The Gradle wrapper is included.

```bash
cd jrawr
./gradlew build
./gradlew :rawr-web:bootRun
```

On Windows use `gradlew.bat`.

The Gradle build also produces the release archives:

```bash
cd jrawr
./gradlew release          # universal archives (no JRE)
./gradlew releaseBundled   # Linux/Windows archives with Temurin JRE 25
./gradlew releaseAll       # both
```

`releaseBundled` downloads the pinned Temurin JREs and needs `bash`, `curl`, `tar`, `unzip`, `zip`,
`sha256sum` and `perl`; it runs on Linux in the release workflow. The archives and their SHA-256 checksums
are written to `jrawr/build/release/`.

Running the application from Gradle reads game data from `jrawr/data` and writes characters to
`jrawr/characters` by default. Both directories can be overridden with the `jrawr.data-dir` and
`jrawr.characters-dir` properties.

The golden-master suite needs the fixtures in `oracle/fixtures` and the expected JSON in
`oracle/golden`; both are part of this repository. Regenerating the oracle additionally needs the
original C# snapshot, Mono and optionally Wine; see [Development](docs/DEVELOPMENT.md).

## Character import

[JRawrCharacterExporter](https://github.com/AdrianoBotti/JRawrCharacterExporter) is a WotLK 3.3.5
addon that exports the active character as original Rawr XML. Open it in game with `/jrawr`, copy
the XML, then use **Character > Import…** in JRawr.

## Data and verification

The item, enchant, buff, talent and filter caches in `jrawr/data` use the original Rawr XML schemas
and are loaded unchanged at runtime. The Java engine is verified against golden JSON produced by
the original C# binaries. The read-only port source and the oracle tools are available in the
[JRawr original Rawr snapshot](https://github.com/AdrianoBotti/JRawrOriginalRawrSnapshot), tag
`jrawr-oracle-1`.

See [Architecture](docs/architecture/README.md) for the engine and web design,
[Development](docs/DEVELOPMENT.md) for the build and test workflow, and
[Releasing](docs/RELEASING.md) for the release process.

## Known limitations

- htmx is loaded from unpkg, so the browser interface needs internet access. Item icons are
  hotlinked from the configured icon site and are not redistributed.
- Adding items by ID and fetching missing items need network access to the selected item source.
  Regenerating the C# oracle requires the legacy snapshot and is a developer task.
- The Armory and Wowhead character importers from the original application are not ported.
- The application is intended for one local user and does not include accounts, TLS or a database.
- Game data is fixed at WotLK 3.3.5a. JRawr does not add new game-data updates or model features.
- The bundled JRE is a pinned Temurin 25.0.5+8; Java security updates require a new JRawr release.
  Use `JRAWR_JAVA` if you manage your own Java runtime. Bundled archives are provided for x64
  Linux and Windows; other platforms can use the universal archive with Java 25.

## AI-assisted development

JRawr was developed with substantial AI assistance. The port, the web layer, the tests, the
release tooling and most of the documentation were written by LLM coding agents under human
direction. The maintainer defined the scope and requirements, chose the architecture and
dependencies, reviewed the generated work, and remains responsible for the result. The
golden-master suite exists to make the engine behavior verifiable independently of how the code was
produced.

## License and attribution

JRawr is licensed under the [Apache License, Version 2.0](LICENSE). It is a port of Rawr, Copyright
2008 Chadd Nervig, "HugeHoss", "Toskk", and the Rawr Developers. See [NOTICE](NOTICE) for the
original attribution, the WotLK fork and snapshot provenance, and the changes made by JRawr. See
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for dependencies and their licenses.

JRawr is not affiliated with or endorsed by Blizzard Entertainment. World of Warcraft, Wrath of the
Lich King, and related names are trademarks of Blizzard Entertainment. Game data included in this
repository (item, enchant, buff and spell statistics) belongs to its respective owners and is
included solely to make the calculator functional. The Apache License does not grant rights to
that game data, the game client, or Blizzard trademarks.
