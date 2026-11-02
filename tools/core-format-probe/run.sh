#!/usr/bin/env bash
# Builds the core-format probe against the prebuilt legacy Rawr.Base.dll (Mono) and regenerates
# jrawr/rawr-core/src/test/resources/core-format/{expected.txt,buff-cache.xml,item-filter.xml,
# item-source.xml,boss-info.txt}. Network-free; the legacy tree is only read.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LEGACY="${RAWR_LEGACY_DIR:-$ROOT/legacy/RAWR_WotLK_3.3.5a}"
DATA="$LEGACY/bin/Release/Data"
OUT="$(dirname "$0")/bin"
RES="$ROOT/jrawr/rawr-core/src/test/resources/core-format"

if [ ! -f "$DATA/Rawr.Base.dll" ]; then
  echo "legacy prebuilt Rawr.Base.dll not found at $DATA" >&2
  exit 1
fi

mkdir -p "$OUT" "$RES"
mcs -langversion:default -r:"$DATA/Rawr.Base.dll" -r:System.Drawing.dll -r:System.Windows.Forms.dll -out:"$OUT/CoreFormatProbe.exe" \
  "$(dirname "$0")/CoreFormatProbe.cs"
MONO_PATH="$DATA" mono "$OUT/CoreFormatProbe.exe" \
  "$RES/vectors.txt" "$RES/expected.txt" "$RES/buff-cache.xml" "$RES/item-filter.xml" "$RES/item-source.xml" "$RES/boss-info.txt"
echo "oracle artifacts written to $RES"
