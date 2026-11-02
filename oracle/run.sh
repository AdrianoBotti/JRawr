#!/usr/bin/env bash
# Regenerate fixtures and golden JSON for one model.
#   ./run.sh            -> mage (default, full oracle refresh, ~4 minutes)
#   ./run.sh restosham  -> restosham (Phase 6)
#   ./run.sh cat        -> cat (Phase 6)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODEL="${1:-mage}"

"$ROOT/oracle/build.sh"

case "$MODEL" in
  mage)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen "$ROOT/oracle/fixtures/mage"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/mage/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/mage/$name.json" api
    done
    ;;
  restosham)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-resto "$ROOT/oracle/fixtures/restosham"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/restosham/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/restosham/$name.json" api restosham
    done
    ;;
  cat)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-cat "$ROOT/oracle/fixtures/cat"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/cat/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/cat/$name.json" api cat
    done
    ;;
  bear)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-bear "$ROOT/oracle/fixtures/bear"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/bear/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/bear/$name.json" api bear
    done
    ;;
  healadin)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-healadin "$ROOT/oracle/fixtures/healadin"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/healadin/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/healadin/$name.json" api healadin
    done
    ;;
  protwarr)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-protwarr "$ROOT/oracle/fixtures/protwarr"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/protwarr/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/protwarr/$name.json" api protwarr
    done
    ;;
  moonkin)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-moonkin "$ROOT/oracle/fixtures/moonkin"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/moonkin/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/moonkin/$name.json" api moonkin
    done
    ;;
  elemental)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-elemental "$ROOT/oracle/fixtures/elemental"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/elemental/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/elemental/$name.json" api elemental
    done
    ;;
  retribution)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-ret "$ROOT/oracle/fixtures/retribution"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/retribution/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/retribution/$name.json" api retribution
    done
    ;;
  enhance)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-enhance "$ROOT/oracle/fixtures/enhance"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/enhance/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/enhance/$name.json" api enhance
    done
    ;;
  protpally)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-protpally "$ROOT/oracle/fixtures/protpally"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/protpally/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/protpally/$name.json" api protpally
    done
    ;;
  healpriest)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-healpriest "$ROOT/oracle/fixtures/healpriest"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/healpriest/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/healpriest/$name.json" api healpriest
    done
    ;;
  shadowpriest)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-shadowpriest "$ROOT/oracle/fixtures/shadowpriest"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/shadowpriest/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/shadowpriest/$name.json" api shadowpriest
    done
    ;;
  hunter)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-hunter "$ROOT/oracle/fixtures/hunter"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/hunter/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/hunter/$name.json" api hunter
    done
    ;;
  rogue)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-rogue "$ROOT/oracle/fixtures/rogue"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/rogue/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/rogue/$name.json" api rogue
    done
    ;;
  tree)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-tree "$ROOT/oracle/fixtures/tree"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/tree/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/tree/$name.json" api tree
    done
    ;;
  tankdk)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-tankdk "$ROOT/oracle/fixtures/tankdk"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/tankdk/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/tankdk/$name.json" api tankdk
    done
    ;;
  warlock)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-warlock "$ROOT/oracle/fixtures/warlock"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/warlock/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/warlock/$name.json" api warlock
    done
    ;;
  dpsdk)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-dpsdk "$ROOT/oracle/fixtures/dpsdk"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/dpsdk/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/dpsdk/$name.json" api dpsdk
    done
    ;;
  dpswarr)
    cd "$ROOT/oracle/fixture-gen/bin"
    MONO_PATH="$PWD/Data" mono FixtureGen.exe gen-dpswarr "$ROOT/oracle/fixtures/dpswarr"

    cd "$ROOT/oracle/dump-tool/bin"
    for f in "$ROOT"/oracle/fixtures/dpswarr/*.xml; do
      name="$(basename "$f" .xml)"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe "$f" "$ROOT/oracle/golden/dpswarr/$name.json" api dpswarr
    done
    ;;
  relative-stats)
    # Relative Stats chart goldens (B-016): a representative set, not every fixture. The dump
    # covers the models without/with base-stat normalization (Mage/Cat vs Enhance).
    mkdir -p "$ROOT/oracle/golden/relative-stats/mage"
    mkdir -p "$ROOT/oracle/golden/relative-stats/cat"
    mkdir -p "$ROOT/oracle/golden/relative-stats/enhance"
    cd "$ROOT/oracle/dump-tool/bin"
    for spec in "mage arcane_277_raid" "mage fire_277_raid" "mage frost_264_raid" \
                "cat cat_277_raid" "enhance enhance_277_procs"; do
      set -- $spec
      model="$1"; name="$2"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe \
        "$ROOT/oracle/fixtures/$model/$name.xml" \
        "$ROOT/oracle/golden/relative-stats/$model/$name.json" relative "$model"
    done
    ;;
  optimize)
    # Phase 7 / stabilization: fixed-pool optimizer goldens for every model (no fixture
    # regeneration). The C# GA seed is time-based, so a re-dump is not byte-identical; the
    # committed files are the oracle. The Java seed is recorded in each JSON and is a Java-side
    # determinism knob only (DECISIONS #48); seed 1 is used where the default constant seed parks
    # the Java search on a local optimum (DPSDK unholy, Enhance, DPSWarr fury).
    mkdir -p "$ROOT/oracle/golden/optimizer"
    cd "$ROOT/oracle/dump-tool/bin"
    run_optimizer() {
      fixture_dir="$1"; name="$2"; model="$3"; seed="${4:-123456789}"
      MONO_PATH="$PWD/Data" mono --interp DumpTool.exe \
        "$ROOT/oracle/fixtures/$fixture_dir/$name.xml" \
        "$ROOT/oracle/golden/optimizer/$name.json" optimize "$model" "$seed"
    }
    run_optimizer mage arcane_277_raid Mage
    run_optimizer mage fire_277_raid Mage
    run_optimizer mage frost_264_raid Mage
    run_optimizer restosham resto_264_raid RestoSham
    run_optimizer restosham resto_277_procs RestoSham
    run_optimizer dpsdk dpsdk_frost_264 DPSDK
    run_optimizer dpsdk dpsdk_unholy_277 DPSDK 1
    run_optimizer protwarr protwarr_277_raid ProtWarr
    run_optimizer protwarr protwarr_264_nobuffs ProtWarr
    run_optimizer bear bear_277_raid Bear
    run_optimizer cat cat_277_raid Cat
    run_optimizer dpswarr dpswarr_fury_277 DPSWarr 1
    run_optimizer elemental elemental_277_procs Elemental
    run_optimizer enhance enhance_277_procs Enhance 1
    run_optimizer healadin healadin_277_procs Healadin
    run_optimizer healpriest healpriest_277_disc HealPriest
    run_optimizer hunter hunter_277_procs Hunter
    run_optimizer moonkin moonkin_277_procs Moonkin
    run_optimizer protpally protpally_277_raid ProtPaladin
    run_optimizer retribution ret_277_procs Retribution
    run_optimizer rogue rogue_combat_277 Rogue
    run_optimizer shadowpriest shadowpriest_277_procs ShadowPriest
    run_optimizer tankdk tankdk_blood_277 TankDK
    run_optimizer tree tree_277_procs Tree
    run_optimizer warlock warlock_demonology_277 Warlock
    ;;
  *)
    echo "usage: $0 [mage|restosham|cat|bear|healadin|protwarr|moonkin|elemental|retribution|enhance|protpally|healpriest|shadowpriest|hunter|rogue|tree|tankdk|warlock|dpsdk|dpswarr|relative-stats|optimize]" >&2
    exit 2
    ;;
esac
echo "oracle refresh complete ($MODEL)"
