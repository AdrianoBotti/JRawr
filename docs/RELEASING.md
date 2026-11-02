# Releasing

JRawr releases are built by the Gradle wrapper and published from a Git tag. The version is
`1.0.0` by default and can be overridden with the `jrawr.version` Gradle property.

## Requirements

- Java 25.
- A clean checkout. The release task reads the game data from `jrawr/data`, the public docs from
  `docs/` and the launchers from `jrawr/packaging/`.

## Build the archives locally

```bash
cd jrawr
./gradlew clean build release
```

For a version other than the default:

```bash
./gradlew clean build release -Pjrawr.version=1.0.1
```

The archives and checksums are written to `jrawr/build/release/`:

```
JRawr-1.0.0.zip
JRawr-1.0.0.zip.sha256
JRawr-1.0.0.tar.gz
JRawr-1.0.0.tar.gz.sha256
```

Each archive contains a top-level `JRawr-1.0.0/` directory:

```
JRawr-1.0.0/
  jrawr.jar
  jrawr.sh
  jrawr.bat
  data/
  docs/
  README.md
  CHANGELOG.md
  LICENSE
  NOTICE
  THIRD_PARTY_NOTICES.md
```

The executable jar also contains `LICENSE`, `NOTICE` and `THIRD_PARTY_NOTICES.md` under
`META-INF/`.

The `release` task fails if the zip contains a forbidden path: `legacy/`, `tools/`, `oracle/`, a
characters directory, `docs/dev/`, `AGENTS.md`, C# sources, DLLs or executables.

## Verify a local build

1. Inspect the archive contents:

   ```bash
   unzip -l jrawr/build/release/JRawr-1.0.0.zip
   tar -tzf jrawr/build/release/JRawr-1.0.0.tar.gz
   ```

2. Check the checksums:

   ```bash
   cd jrawr/build/release
   sha256sum -c JRawr-1.0.0.zip.sha256
   sha256sum -c JRawr-1.0.0.tar.gz.sha256
   ```

3. Extract it to a temporary directory, run the launcher from outside that directory and open the
   app:

   ```bash
   mkdir -p /tmp/jrawr-smoke
   unzip -q jrawr/build/release/JRawr-1.0.0.zip -d /tmp/jrawr-smoke
   cd /tmp
   JRAWR_PORT=18080 XDG_DATA_HOME=/tmp/jrawr-smoke/user /tmp/jrawr-smoke/JRawr-1.0.0/jrawr.sh
   ```

   Open <http://127.0.0.1:18080/>. Confirm the data loads, a character can be imported and the
   ratings render. The launcher must create the user data directory and must not write into the
   extracted application directory.

4. Confirm the published archive contains no source, tool binaries, local characters or developer
   documentation.

## GitHub Actions release

`.github/workflows/release.yml` runs on tags matching `v*`:

- It checks out the tag.
- Builds and tests with Java 25.
- Runs `./gradlew clean build release -Pjrawr.version=<tag without v>`.
- Generates the SHA-256 files.
- Creates a GitHub Release and uploads the zip, tar.gz and checksums.

Create the release with:

```bash
git tag -a v1.0.0 -m "JRawr 1.0.0"
git push origin v1.0.0
```

Do not move or reuse a published tag. If a release is wrong, publish a new patch version.

## Manual fallback

If the release workflow is unavailable, build the archives locally with the commands above, create
the GitHub Release by hand and upload all four files. Keep the checksums next to the archives.

## Windows verification

The CI matrix runs the full build on Linux and Windows. The release archive itself is
platform-independent because the jar and both launchers are included. Before publishing a tag,
confirm the Windows CI job is green and, when possible, run `jrawr.bat` once from an extracted
archive.

## Snapshot and cross-links

The C# port source and oracle tools are published separately at
<https://github.com/AdrianoBotti/JRawrOriginalRawrSnapshot>, tag `jrawr-oracle-1`. The snapshot is
needed only to regenerate the oracle. The main repository and the snapshot should link to each
other, and the companion addon
[JRawrCharacterExporter](https://github.com/AdrianoBotti/JRawrCharacterExporter) should link to the
JRawr release page.

Update the snapshot and addon READMEs to link to the main repository only after it is public, so
those external pages never point at a dead URL. The links inside JRawr to the snapshot and addon
can be added now because both repositories already exist.
