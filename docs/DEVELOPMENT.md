# Development

JRawr is a Java 25 multi-module Gradle build. The Gradle wrapper is included, so no system Gradle
installation is needed.

## Requirements

- Java 25 (Temurin is the tested distribution). The build uses a Java toolchain and `--release 25`.
- Git, to clone the repository.
- Optional: Mono (`mcs`, `mono`) and optionally Wine, only to regenerate the C# oracle.
- Optional: Python 3 for some helper tools.

Normal build, test and run need only Java.

## Repository layout

```
JRawr/
  jrawr/                  Gradle multi-module build
    rawr-core/            ported Rawr.Base engine (no Spring, no UI)
    rawr-models/          all 20 class/spec models
    rawr-data/            XML loaders and persistent stores
    rawr-golden/          golden-master, optimizer and relative-stat tests
    rawr-web/             Spring Boot, Thymeleaf and htmx application
    data/                 shipped WotLK 3.3.5a XML caches
    packaging/            release launchers
  oracle/                 fixtures, golden JSON and the C# oracle tools
  tools/                  development probes and data generators
  docs/                   public documentation
    architecture/         public architecture handbook
    dev/                  local-only development notes (gitignored)
```

`legacy/` is not part of the repository. It is a local, read-only checkout of the C# port source,
created with `tools/setup-legacy.sh`. See [Oracle regeneration](#oracle-regeneration).

## Build and test

Run from the `jrawr/` directory:

```bash
./gradlew build
```

On Windows use `gradlew.bat`. The `build` task compiles every module, runs the unit and web tests,
and runs every golden-master, optimizer and relative-stat fixture through `goldenAllParallel`.
Each fixture runs in a fresh JVM because the models keep static and carry-over state.

Useful targeted commands:

```bash
./gradlew :rawr-core:test
./gradlew :rawr-data:test
./gradlew :rawr-models:test
./gradlew :rawr-web:test
./gradlew :rawr-golden:goldenAllParallel -Pjrawr.golden.workers=4
./gradlew :rawr-golden:mageGolden_arcane_277_raid
```

The individual golden tasks are named `<model>Golden_<fixture>`; run `./gradlew :rawr-golden:tasks`
to list them. `-Pjrawr.golden.workers=N` controls the parallel fixture process pool (default 4).

The golden comparison uses relative error `1e-9` for doubles, absolute `1e-12` when the expected
value is near zero, and exact comparison for integers, enums and strings. A diff in a model is
localized from the first differing value in dependency order: item stats, `Stats`, spell or ability
values, cycles, chosen solution, then ratings.

Do not weaken a tolerance to make a test pass. If the original behavior is unclear, add a fixture
that exercises it and document the reason.

## Run the application

```bash
./gradlew :rawr-web:bootRun
```

Then open <http://127.0.0.1:8080/>. When started with `bootRun`, the working directory is the
`jrawr/` project directory, so the defaults resolve to:

- game data: `jrawr/data`
- saved characters and side-cars: `jrawr/characters`

Both are ignored by git. The application fails fast at startup if `ItemCache.xml`,
`BuffCache.xml` or `EnchantCache.xml` is missing.

Common configuration properties:

| Property | Default | Purpose |
|---|---|---|
| `server.address` | `0.0.0.0` | Bind address. The release launcher sets `127.0.0.1`. |
| `server.port` | `8080` | HTTP port. |
| `jrawr.data-dir` | `data` | Directory containing the XML game-data caches. |
| `jrawr.characters-dir` | `characters` | Directory for saved characters and local side-car files. |
| `jrawr.items-file` | empty | User item store. Empty means `jrawr.items.xml` in the characters directory. |
| `jrawr.item-filters-file` | empty | Item filter side-car. Empty means `jrawr.item-filters.xml` in the characters directory. |
| `jrawr.item-costs-file` | empty | Item cost side-car. Empty means `jrawr.item-costs.xml` in the characters directory. |
| `jrawr.talents-file` | empty | Saved talent specs. Empty means `jrawr.talents.xml` in the characters directory. |
| `jrawr.icon-base-url` | `https://wow.zamimg.com` | Fallback site for hotlinked item icons. |
| `spring.thymeleaf.cache` | `false` | Template cache. The release launcher enables it. |
| `spring.web.resources.cache.period` | `0` | Static resource cache period. The release launcher sets 365 days. |

Example:

```bash
./gradlew :rawr-web:bootRun --args='--server.port=9000 --jrawr.data-dir=/path/to/data'
```

## Data loading

`rawr-data` reads the original Rawr XML schemas without translation:

- `ItemCache.xml`, `BuffCache.xml` and `EnchantCache.xml` are required.
- `ItemSource.xml` and `ItemFilter.xml` are optional.
- `Talents.xml` supplies the shipped talent specs when present.
- Saved character XML uses the original Rawr character format.
- User items, saved talent specs, item filters and item costs are stored in side-car files in the
  characters directory. The shipped `ItemCache.xml` is never rewritten.

The full data set is under `jrawr/data` and is loaded only at runtime. Do not open the large XML
files in an editor or feed them to a language model; use targeted `grep`, `wc` and `head` commands.

## Oracle regeneration

The golden JSON in `oracle/golden` was produced by the original C# engine. Regenerating it needs the
legacy source and the prebuilt C# binaries:

```bash
tools/setup-legacy.sh
cd oracle
./build.sh
./run.sh mage          # one model
./run.sh optimize      # optimizer goldens
```

`setup-legacy.sh` clones the published snapshot
[JRawrOriginalRawrSnapshot](https://github.com/AdrianoBotti/JRawrOriginalRawrSnapshot) at tag
`jrawr-oracle-1` into `legacy/RAWR_WotLK_3.3.5a` and verifies the commit. The directory is
read-only for this project. Never run MSBuild or xbuild against it: an xbuild rebuild deletes the
prebuilt `Rawr.Base.dll` and its config from the snapshot's `bin/Release/Data` directory.

Oracle runs use Mono, not .NET:

```bash
mono --version
mcs --version
```

Wine is optional and only needed for the original Windows-only pieces. The Java build never calls
Mono, Wine or the C# tools, and tests never call third-party services.

## Release packaging

```bash
cd jrawr
./gradlew release          # universal archives without a JRE
./gradlew releaseBundled   # Linux/Windows archives with Temurin JRE 25
./gradlew releaseAll       # both
```

The bundled task downloads the pinned Temurin JREs from
`jrawr/gradle/temurin.properties` and requires `bash`, `curl`, `tar`, `unzip`, `zip`, `sha256sum` and
`perl`. It is run on Linux in the release workflow. The launchers prefer `JRAWR_JAVA`, then
the bundled `runtime/`, then `JAVA_HOME`, then `java` on `PATH`. See
[Releasing](RELEASING.md) for artifact names, checksums and smoke-test steps.

## Tools

`tools/` contains development probes and generators (base stats, stat conversion, special effects,
set-bonus text, talent metadata and core completeness). See [tools/README.md](../tools/README.md).
They are not required to build or run JRawr.

## Development notes

The local `docs/dev/` directory holds the session log, decision record, backlog, porting closure,
performance notes and other development-only material. It is gitignored and is not published. The
public documentation is the set listed in [docs/README.md](README.md).

## Troubleshooting

- `Data directory ... does not contain ...`: check `jrawr.data-dir` and the required cache files.
- Java version errors: install JDK 25, point `JAVA_HOME` at it, or set `JRAWR_JAVA` to the java
  executable you want the launcher to use.
- Port already in use: pass `--server.port=<port>` or set `JRAWR_PORT` when using the launcher.
- Blank or missing item icons: the icon site is unreachable, or the configured icon base is wrong.
  Icons are hotlinked in the browser and are never cached by the server.
- Golden tests fail after changing model code: find the first differing field and compare against
  the matching fixture in `oracle/fixtures` before changing anything else.
