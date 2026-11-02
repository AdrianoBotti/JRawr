# Releasing

JRawr releases are built by the Gradle wrapper and published from a Git tag. The version is
`1.0.1` by default and can be overridden with the `jrawr.version` Gradle property.

## Requirements

- Java 25.
- A clean checkout. The release tasks read the game data from `jrawr/data`, the public docs from
  `docs/` and the launchers from `jrawr/packaging/`.
- For the bundled archives: a Unix-like shell with `bash`, `curl`, `tar`, `unzip`, `zip`, `sha256sum`
  and `perl`. The GitHub Actions release job runs this on Ubuntu.

## Build the archives locally

```bash
cd jrawr
./gradlew clean build releaseAll
```

For a version other than the default:

```bash
./gradlew clean build releaseAll -Pjrawr.version=1.0.2
```

- `release` builds the universal archives without a JRE.
- `releaseBundled` builds the Linux and Windows archives with Eclipse Temurin JRE 25.
- `releaseAll` builds both.

The archives and checksums are written to `jrawr/build/release/`:

```
JRawr-1.0.1.zip
JRawr-1.0.1.zip.sha256
JRawr-1.0.1.tar.gz
JRawr-1.0.1.tar.gz.sha256
JRawr-1.0.1-linux-x64.zip
JRawr-1.0.1-linux-x64.zip.sha256
JRawr-1.0.1-linux-x64.tar.gz
JRawr-1.0.1-linux-x64.tar.gz.sha256
JRawr-1.0.1-windows-x64.zip
JRawr-1.0.1-windows-x64.zip.sha256
```

The universal archives contain `jrawr.jar`, both launchers, `data/`, `docs/`, `README.md`,
`CHANGELOG.md`, `LICENSE`, `NOTICE` and `THIRD_PARTY_NOTICES.md` under a top-level
`JRawr-1.0.1/` directory.

The bundled archives use the same layout plus a `runtime/` directory with the Temurin JRE. The
Linux archive contains `jrawr.sh`; the Windows archive contains `jrawr.bat`. The executable jar
also contains `LICENSE`, `NOTICE` and `THIRD_PARTY_NOTICES.md` under `META-INF/`.

The `release` task fails if the universal zip contains a forbidden path: `legacy/`, `tools/`,
`oracle/`, a characters directory, `docs/dev/`, `AGENTS.md`, C# sources, DLLs or executables. The
bundled packaging script applies the same check outside `runtime/`; the JRE legitimately contains
DLLs and the Windows `java.exe`.

## JRE pin

The bundled runtime is pinned in `jrawr/gradle/temurin.properties` with the download URLs and
SHA-256 checksums. The packaging script downloads the pinned files into
`jrawr/build/bundled/cache/` and verifies both checksums before extracting them. Update the pin
only as a deliberate JRE bump, then rebuild and smoke-test both platform archives.

The JRE's own legal files are included in the archive under `runtime/legal/`. The bundled runtime
is Eclipse Temurin 25.0.5+8 (OpenJDK), GPLv2 with the Classpath Exception. See
`THIRD_PARTY_NOTICES.md`.

## Verify a local build

1. Inspect the archives:

   ```bash
   unzip -l jrawr/build/release/JRawr-1.0.1.zip
   unzip -l jrawr/build/release/JRawr-1.0.1-linux-x64.zip
   unzip -l jrawr/build/release/JRawr-1.0.1-windows-x64.zip
   ```

2. Check every checksum:

   ```bash
   cd jrawr/build/release
   sha256sum -c ./*.sha256
   ```

3. Extract the bundled Linux archive, run the launcher from outside that directory and open the
   app:

   ```bash
   mkdir -p /tmp/jrawr-smoke
   unzip -q jrawr/build/release/JRawr-1.0.1-linux-x64.zip -d /tmp/jrawr-smoke
   cd /tmp
   JRAWR_PORT=18080 XDG_DATA_HOME=/tmp/jrawr-smoke/user \
     /tmp/jrawr-smoke/JRawr-1.0.1-linux-x64/jrawr.sh
   ```

   Open <http://127.0.0.1:18080/>. Confirm the data loads, a character can be imported and the
   ratings render. The launcher must create the user data directory and must not write into the
   extracted application directory.

4. Confirm the bundled runtime is being used. The startup log reports the Java version from the
   bundled JRE (25.0.5), and `JRAWR_JAVA` overrides it:

   ```bash
   JRAWR_JAVA=/path/to/your/java25/bin/java \
     /tmp/jrawr-smoke/JRawr-1.0.1-linux-x64/jrawr.sh
   ```

5. Confirm the Windows archive contains `runtime/bin/java.exe` and `jrawr.bat` but no `jrawr.sh`,
   and that `jrawr.bat` uses CRLF line endings.

6. Confirm the published archives contain no source, tool binaries, local characters or developer
   documentation outside the bundled JRE.

## GitHub Actions release

`.github/workflows/release.yml` runs on tags matching `v*`:

- It checks out the tag.
- Builds and tests with Java 25.
- Runs `./gradlew clean build release releaseBundled -Pjrawr.version=<tag without v>`.
- Downloads the pinned Temurin JREs and generates the universal and bundled checksums.
- Smoke-tests the bundled Linux archive and verifies the bundled Java version.
- Creates a GitHub Release and uploads the zip, tar.gz and checksum files.

Create the release with:

```bash
git tag -a v1.0.1 -m "JRawr 1.0.1"
git push origin v1.0.1
```

Do not move or reuse a published tag. If a release is wrong, publish a new patch version.

## Manual fallback

If the release workflow is unavailable, build the archives locally with the commands above, create
the GitHub Release by hand and upload every archive and checksum file. Keep the checksums next to
the archives.

## Windows verification

The CI matrix runs the full build on Linux and Windows. The universal archive is platform
independent because it contains the jar and both launchers. The bundled Windows archive contains a
Windows JRE and must be tested on Windows; before publishing a tag, confirm the Windows CI job is
green and, when possible, run `jrawr.bat` once from the extracted Windows archive.

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
