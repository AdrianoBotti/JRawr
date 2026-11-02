#!/usr/bin/env bash
# Clones the published Rawr port snapshot into legacy/RAWR_WotLK_3.3.5a.
# The snapshot contains the original C# source, the prebuilt Rawr DLLs and the WotLK data caches.
# It is only needed to regenerate the oracle; normal builds and tests use Java alone.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="${RAWR_LEGACY_DIR:-$ROOT/legacy/RAWR_WotLK_3.3.5a}"
SNAPSHOT_REPO="https://github.com/AdrianoBotti/JRawrOriginalRawrSnapshot.git"
SNAPSHOT_TAG="jrawr-oracle-1"
SNAPSHOT_COMMIT="12f9030ca85d62e854581eec340d309d062c8e2b"

if [ -e "$TARGET" ]; then
    if [ -d "$TARGET/.git" ]; then
        actual="$(git -C "$TARGET" rev-parse HEAD)"
        if [ "$actual" = "$SNAPSHOT_COMMIT" ]; then
            echo "Legacy snapshot already present at $TARGET ($actual)."
            exit 0
        fi
        echo "Legacy directory exists at $TARGET but is at $actual, expected $SNAPSHOT_COMMIT." >&2
        echo "Move it away or set RAWR_LEGACY_DIR to a different path." >&2
        exit 1
    fi
    echo "Path exists and is not a git checkout: $TARGET" >&2
    echo "Move it away or set RAWR_LEGACY_DIR to a different path." >&2
    exit 1
fi

command -v git >/dev/null 2>&1 || {
    echo "git is required to clone the legacy snapshot." >&2
    exit 1
}

mkdir -p "$(dirname "$TARGET")"
echo "Cloning $SNAPSHOT_REPO at $SNAPSHOT_TAG into $TARGET"
git clone --depth 1 --branch "$SNAPSHOT_TAG" "$SNAPSHOT_REPO" "$TARGET"

actual="$(git -C "$TARGET" rev-parse HEAD)"
if [ "$actual" != "$SNAPSHOT_COMMIT" ]; then
    echo "Snapshot commit mismatch: expected $SNAPSHOT_COMMIT, found $actual." >&2
    exit 1
fi

echo "Legacy snapshot ready at $TARGET ($actual)."
echo "Regenerate the oracle with: cd oracle && ./build.sh && ./run.sh mage"
