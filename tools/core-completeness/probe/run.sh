#!/usr/bin/env bash
# Builds the completeness probes against the prebuilt legacy Rawr.Base.dll (Mono) and regenerates
#   jrawr/rawr-core/src/test/resources/core-completeness/expected.txt
#   jrawr/rawr-golden/src/test/resources/core-completeness/relevant-items.txt
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
LEGACY="${RAWR_LEGACY_DIR:-$ROOT/legacy/RAWR_WotLK_3.3.5a}"
DATA="$LEGACY/bin/Release/Data"
OUT="$(cd "$(dirname "$0")" && pwd)/bin"
RES="$ROOT/jrawr/rawr-core/src/test/resources/core-completeness"
RES_GOLDEN="$ROOT/jrawr/rawr-golden/src/test/resources/core-completeness"

if [ ! -f "$DATA/Rawr.Base.dll" ]; then
  echo "legacy prebuilt Rawr.Base.dll not found at $DATA" >&2
  exit 1
fi

mkdir -p "$OUT" "$RES" "$RES_GOLDEN"
mcs -langversion:default -r:"$DATA/Rawr.Base.dll" -out:"$OUT/CompletenessProbe.exe" \
  "$(dirname "$0")/CompletenessProbe.cs"
MONO_PATH="$DATA" mono "$OUT/CompletenessProbe.exe" > "$RES/expected.txt"
echo "oracle artifact written to $RES/expected.txt"

# The relevant-items probe needs Data/ItemCache.xml + Data/ItemFilter.xml relative to the executable
# (ItemCache.SavedFilePath), so run it from a work directory whose Data holds the runtime caches.
# The legacy runtime ItemCache.xml was regenerated without <RequiredClasses>; restore the canonical
# DefaultDataFiles values first so the oracle matches the corrected jrawr/data cache.
mcs -langversion:default -r:"$DATA/Rawr.Base.dll" -r:"$DATA/Rawr.Mage.dll" \
  -out:"$OUT/RelevantItemsProbe.exe" "$(dirname "$0")/RelevantItemsProbe.cs"
WORK="$OUT/relevant-work"
rm -rf "$WORK"
mkdir -p "$WORK/run/Data"
for f in "$DATA"/*; do
  [ "$(basename "$f")" = "ItemCache.xml" ] && continue
  ln -s "$f" "$WORK/run/Data/$(basename "$f")"
done
cp "$DATA/ItemCache.xml" "$WORK/run/Data/ItemCache.xml"
python3 "$(dirname "$0")/merge-required-classes.py" \
  "$LEGACY/DefaultDataFiles/ItemCache.xml" \
  "$WORK/run/Data/ItemCache.xml"
cp "$OUT/RelevantItemsProbe.exe" "$WORK/run/"

FIXTURES="$ROOT/oracle/fixtures/mage"
(cd "$WORK/run" && MONO_PATH="$DATA" mono "$WORK/run/RelevantItemsProbe.exe" \
  "$FIXTURES/arcane_277_raid.xml" "$FIXTURES/fire_277_raid.xml" "$FIXTURES/frost_target80.xml") \
  > "$RES_GOLDEN/relevant-items.txt"
echo "oracle artifact written to $RES_GOLDEN/relevant-items.txt"
