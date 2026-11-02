#!/usr/bin/env bash
# Builds the SpecialEffects probe against the prebuilt legacy Rawr.Base.dll (Mono) and
# regenerates jrawr/rawr-core/src/test/resources/special-effects/expected.txt.
# Network-free; the legacy tree is only read.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LEGACY="${RAWR_LEGACY_DIR:-$ROOT/legacy/RAWR_WotLK_3.3.5a}"
DATA="$LEGACY/bin/Release/Data"
OUT="$(dirname "$0")/bin"
VECTORS="$ROOT/jrawr/rawr-core/src/test/resources/special-effects/vectors.txt"
EXPECTED="$ROOT/jrawr/rawr-core/src/test/resources/special-effects/expected.txt"

if [ ! -f "$DATA/Rawr.Base.dll" ]; then
  echo "legacy prebuilt Rawr.Base.dll not found at $DATA" >&2
  exit 1
fi

mkdir -p "$OUT"
mcs -langversion:default -r:"$DATA/Rawr.Base.dll" -out:"$OUT/SpecialEffectsProbe.exe" \
  "$(dirname "$0")/SpecialEffectsProbe.cs"
MONO_PATH="$DATA" mono "$OUT/SpecialEffectsProbe.exe" "$VECTORS" "$EXPECTED"
echo "expected written to $EXPECTED"
