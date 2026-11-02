#!/usr/bin/env bash
# Phase 1 oracle build: symlinks the legacy runtime data next to each tool and
# compiles the C# sources with Mono's mcs. The legacy tree is never modified.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LEGACY="${RAWR_LEGACY_DIR:-$ROOT/legacy/RAWR_WotLK_3.3.5a}"
DATA="$LEGACY/bin/Release/Data"
FILES="Rawr.Base.dll Rawr.Mage.dll Rawr.RestoSham.dll Rawr.Cat.dll Rawr.Bear.dll Rawr.Healadin.dll Rawr.ProtWarr.dll Rawr.Moonkin.dll Rawr.Elemental.dll Rawr.Retribution.dll Rawr.Enhance.dll Rawr.ProtPaladin.dll Rawr.HolyPriest.dll Rawr.ShadowPriest.dll Rawr.Hunter.dll Rawr.Rogue.dll Rawr.Tree.dll Rawr.TankDK.dll Rawr.Warlock.dll Rawr.DPSDK.dll Rawr.DPSWarr.dll Rawr.Base.XmlSerializers.dll ItemCache.xml BuffCache.xml EnchantCache.xml ItemFilter.xml ItemSource.xml Talents.xml BuffSets.xml"

if [ ! -f "$DATA/Rawr.Mage.dll" ]; then
  echo "legacy prebuilt binaries not found at $DATA" >&2
  exit 1
fi

for tool in fixture-gen dump-tool; do
  BIN="$ROOT/oracle/$tool/bin"
  mkdir -p "$BIN/Data"
  for f in $FILES; do
    ln -sfn "$DATA/$f" "$BIN/Data/$f"
  done
  ln -sfn "$LEGACY/DefaultDataFiles/Talents.xml" "$BIN/Data/Talents.Default.xml"
done
mkdir -p "$ROOT/oracle/fixtures/mage" "$ROOT/oracle/golden/mage"
mkdir -p "$ROOT/oracle/fixtures/restosham" "$ROOT/oracle/golden/restosham"
mkdir -p "$ROOT/oracle/fixtures/cat" "$ROOT/oracle/golden/cat"
mkdir -p "$ROOT/oracle/fixtures/bear" "$ROOT/oracle/golden/bear"
mkdir -p "$ROOT/oracle/fixtures/healadin" "$ROOT/oracle/golden/healadin"
mkdir -p "$ROOT/oracle/fixtures/protwarr" "$ROOT/oracle/golden/protwarr"
mkdir -p "$ROOT/oracle/fixtures/moonkin" "$ROOT/oracle/golden/moonkin"
mkdir -p "$ROOT/oracle/fixtures/elemental" "$ROOT/oracle/golden/elemental"
mkdir -p "$ROOT/oracle/fixtures/retribution" "$ROOT/oracle/golden/retribution"
mkdir -p "$ROOT/oracle/fixtures/enhance" "$ROOT/oracle/golden/enhance"
mkdir -p "$ROOT/oracle/fixtures/protpally" "$ROOT/oracle/golden/protpally"
mkdir -p "$ROOT/oracle/fixtures/healpriest" "$ROOT/oracle/golden/healpriest"
mkdir -p "$ROOT/oracle/fixtures/shadowpriest" "$ROOT/oracle/golden/shadowpriest"
mkdir -p "$ROOT/oracle/fixtures/hunter" "$ROOT/oracle/golden/hunter"
mkdir -p "$ROOT/oracle/fixtures/rogue" "$ROOT/oracle/golden/rogue"
mkdir -p "$ROOT/oracle/fixtures/tree" "$ROOT/oracle/golden/tree"
mkdir -p "$ROOT/oracle/fixtures/tankdk" "$ROOT/oracle/golden/tankdk"
mkdir -p "$ROOT/oracle/fixtures/warlock" "$ROOT/oracle/golden/warlock"
mkdir -p "$ROOT/oracle/fixtures/dpsdk" "$ROOT/oracle/golden/dpsdk"
mkdir -p "$ROOT/oracle/fixtures/dpswarr" "$ROOT/oracle/golden/dpswarr"

MCS_ARGS="-r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.Base.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.Mage.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.RestoSham.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.Cat.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.Bear.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.Healadin.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.ProtWarr.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.Moonkin.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.Elemental.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.Retribution.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.Enhance.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.ProtPaladin.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.HolyPriest.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.ShadowPriest.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.Hunter.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.Rogue.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.Tree.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.TankDK.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.Warlock.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.DPSDK.dll -r:$ROOT/oracle/fixture-gen/bin/Data/Rawr.DPSWarr.dll"
mcs $MCS_ARGS -out:"$ROOT/oracle/fixture-gen/bin/FixtureGen.exe" "$ROOT/oracle/fixture-gen/FixtureGen.cs"

if [ -f "$ROOT/oracle/dump-tool/DumpTool.cs" ]; then
  mcs $MCS_ARGS -out:"$ROOT/oracle/dump-tool/bin/DumpTool.exe" "$ROOT/oracle/dump-tool/DumpTool.cs"
fi
echo "build ok"
