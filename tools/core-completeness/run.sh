#!/usr/bin/env bash
# Member-level completeness audit of the prebuilt legacy Rawr.Base.dll (Mono.Cecil 0.11.1).
# Reads only; writes out/dll-members.tsv.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LEGACY="${RAWR_LEGACY_DIR:-$ROOT/legacy/RAWR_WotLK_3.3.5a}"
DATA="$LEGACY/bin/Release/Data"
OUT="$(dirname "$0")/out"
CECIL=/usr/lib/mono/gac/Mono.Cecil/0.11.1.0__0738eb9f132ed756/Mono.Cecil.dll

if [ ! -f "$DATA/Rawr.Base.dll" ]; then
  echo "legacy prebuilt Rawr.Base.dll not found at $DATA" >&2
  exit 1
fi
if [ ! -f "$CECIL" ]; then
  echo "Mono.Cecil 0.11.1 not found at $CECIL" >&2
  exit 1
fi

mkdir -p "$OUT"
mcs -langversion:default -r:"$CECIL" -out:"$OUT/CoreCompleteness.exe" \
  "$(dirname "$0")/CoreCompleteness.cs"
mono "$OUT/CoreCompleteness.exe" "$DATA" "$OUT/dll-members.tsv"
