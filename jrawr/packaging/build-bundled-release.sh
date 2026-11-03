#!/usr/bin/env bash
# Build the platform-specific JRawr archives with a bundled Eclipse Temurin JRE.
# Intended for Linux release builds (the GitHub Actions release job). Needs bash,
# curl, tar, unzip, zip and sha256sum.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="${1:-1.0.2}"
PROPS="$ROOT/gradle/temurin.properties"
STAGE="$ROOT/build/release-stage/JRawr-$VERSION"
WORK="$ROOT/build/bundled"
OUT="$ROOT/build/release"

if [ ! -d "$STAGE" ]; then
    echo "Release staging directory not found: $STAGE" >&2
    echo "Run ./gradlew stageRelease first (or use ./gradlew releaseBundled)." >&2
    exit 1
fi
if [ ! -f "$PROPS" ]; then
    echo "Missing Temurin pin file: $PROPS" >&2
    exit 1
fi

read_prop() {
    grep -E "^$1=" "$PROPS" | head -1 | cut -d= -f2-
}

TEMURIN_VERSION="$(read_prop temurin.version)"
LINUX_URL="$(read_prop temurin.linux.url)"
LINUX_SHA="$(read_prop temurin.linux.sha256)"
WINDOWS_URL="$(read_prop temurin.windows.url)"
WINDOWS_SHA="$(read_prop temurin.windows.sha256)"

mkdir -p "$WORK/cache" "$OUT"

fetch() {
    local url="$1" dest="$2" expected="$3"
    if [ ! -f "$dest" ]; then
        echo "Downloading $url"
        curl -fL --retry 3 --retry-delay 2 -o "$dest.part" "$url"
        mv "$dest.part" "$dest"
    fi
    echo "$expected  $dest" | sha256sum -c -
}

LINUX_ARCHIVE="$WORK/cache/temurin-$TEMURIN_VERSION-linux-x64.tar.gz"
WINDOWS_ARCHIVE="$WORK/cache/temurin-$TEMURIN_VERSION-windows-x64.zip"
fetch "$LINUX_URL" "$LINUX_ARCHIVE" "$LINUX_SHA"
fetch "$WINDOWS_URL" "$WINDOWS_ARCHIVE" "$WINDOWS_SHA"

rm -rf "$WORK/linux" "$WORK/windows"
mkdir -p "$WORK/linux" "$WORK/windows"
tar -xzf "$LINUX_ARCHIVE" -C "$WORK/linux"
unzip -q "$WINDOWS_ARCHIVE" -d "$WORK/windows"

LINUX_ROOT="$(find "$WORK/linux" -mindepth 1 -maxdepth 1 -type d | head -1)"
WINDOWS_ROOT="$(find "$WORK/windows" -mindepth 1 -maxdepth 1 -type d | head -1)"
if [ -z "$LINUX_ROOT" ] || [ -z "$WINDOWS_ROOT" ]; then
    echo "Could not find the extracted Temurin runtime directories." >&2
    exit 1
fi

LINUX_STAGE="$WORK/JRawr-$VERSION-linux-x64"
WINDOWS_STAGE="$WORK/JRawr-$VERSION-windows-x64"
rm -rf "$LINUX_STAGE" "$WINDOWS_STAGE"
cp -a "$STAGE" "$LINUX_STAGE"
cp -a "$STAGE" "$WINDOWS_STAGE"
rm -f "$LINUX_STAGE/jrawr.bat"
rm -f "$WINDOWS_STAGE/jrawr.sh"
cp -a "$LINUX_ROOT" "$LINUX_STAGE/runtime"
cp -a "$WINDOWS_ROOT" "$WINDOWS_STAGE/runtime"
# Keep the Windows launcher in the CRLF form cmd.exe expects, even when staging from Linux.
perl -pi -e 's/\r?\n/\r\n/' "$WINDOWS_STAGE/jrawr.bat"

# The tar-extracted runtime keeps its modes; make the documented entry points explicit.
find "$LINUX_STAGE/runtime/bin" -type f -exec chmod 755 {} +
[ -f "$LINUX_STAGE/runtime/lib/jexec" ] && chmod 755 "$LINUX_STAGE/runtime/lib/jexec"
[ -f "$LINUX_STAGE/runtime/lib/jspawnhelper" ] && chmod 755 "$LINUX_STAGE/runtime/lib/jspawnhelper"

check_archive() {
    local archive="$1"
    local listing
    if [[ "$archive" == *.zip ]]; then
        listing="$(unzip -Z1 "$archive")"
    else
        listing="$(tar -tzf "$archive")"
    fi
    if printf '%s\n' "$listing" | grep -Ev '^JRawr-[^/]+/runtime/' | grep -Eq '(^|/)(legacy|tools|oracle|characters|docs/dev)(/|$)|(^|/)AGENTS\.md$|\.(cs|dll|exe)$'; then
        echo "Forbidden path in $archive" >&2
        printf '%s\n' "$listing" | grep -Ev '^JRawr-[^/]+/runtime/' | grep -E '(^|/)(legacy|tools|oracle|characters|docs/dev)(/|$)|(^|/)AGENTS\.md$|\.(cs|dll|exe)$' >&2
        exit 1
    fi
}

cd "$WORK"
rm -f "$OUT/JRawr-$VERSION-linux-x64.zip" "$OUT/JRawr-$VERSION-linux-x64.tar.gz" "$OUT/JRawr-$VERSION-windows-x64.zip"
zip -qr "$OUT/JRawr-$VERSION-linux-x64.zip" "JRawr-$VERSION-linux-x64"
tar -czf "$OUT/JRawr-$VERSION-linux-x64.tar.gz" "JRawr-$VERSION-linux-x64"
zip -qr "$OUT/JRawr-$VERSION-windows-x64.zip" "JRawr-$VERSION-windows-x64"

for archive in \
    "$OUT/JRawr-$VERSION-linux-x64.zip" \
    "$OUT/JRawr-$VERSION-linux-x64.tar.gz" \
    "$OUT/JRawr-$VERSION-windows-x64.zip"; do
    check_archive "$archive"
done

cd "$OUT"
for archive in \
    "JRawr-$VERSION-linux-x64.zip" \
    "JRawr-$VERSION-linux-x64.tar.gz" \
    "JRawr-$VERSION-windows-x64.zip"; do
    sha256sum "$archive" > "$archive.sha256"
done

echo "Bundled release archives written to $OUT:"
for archive in \
    "JRawr-$VERSION-linux-x64.zip" \
    "JRawr-$VERSION-linux-x64.tar.gz" \
    "JRawr-$VERSION-windows-x64.zip"; do
    ls -l "$OUT/$archive" "$OUT/$archive.sha256"
done
