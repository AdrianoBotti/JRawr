#!/usr/bin/env bash
# Builds the BaseStats probe against the prebuilt legacy Rawr.Base.dll (Mono) and regenerates
# jrawr/rawr-core/src/test/resources/base-stats/expected.txt.
# Network-free; the legacy tree is only read.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LEGACY="${RAWR_LEGACY_DIR:-$ROOT/legacy/RAWR_WotLK_3.3.5a}"
DATA="$LEGACY/bin/Release/Data"
OUT="$(dirname "$0")/bin"
EXPECTED="$ROOT/jrawr/rawr-core/src/test/resources/base-stats/expected.txt"

if [ ! -f "$DATA/Rawr.Base.dll" ]; then
  echo "legacy prebuilt Rawr.Base.dll not found at $DATA" >&2
  exit 1
fi

mkdir -p "$OUT" "$(dirname "$EXPECTED")"
mcs -langversion:default -r:"$DATA/Rawr.Base.dll" -out:"$OUT/BaseStatsProbe.exe" \
  "$(dirname "$0")/BaseStatsProbe.cs"
MONO_PATH="$DATA" mono "$OUT/BaseStatsProbe.exe" "$EXPECTED"
echo "expected written to $EXPECTED"
