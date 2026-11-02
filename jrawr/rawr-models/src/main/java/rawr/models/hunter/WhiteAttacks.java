// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/Skills.Base.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: only the constructor is ported. The prebuilt Rawr.Hunter.dll calls it once from
// CalculationsHunter.GetCharacterCalculations and immediately discards the result (IL newobj + pop),
// so its only observable effect is constructing the white ranged-weapon AttackTable. Every other
// WhiteAttacks member (InvalidateCache, the Rw*/Oh* damage, rage and DPS caches, targets/overrides)
// is reachable only through a Rawr.Hunter.Rotation instance, and the DLL never instantiates that
// class (no newobj of Rotation::.ctor; see DECISIONS #47/#58), so those members stay omitted.
package rawr.models.hunter;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.talents.HunterTalents;
import rawr.base.stats.Stats;

final class WhiteAttacks {
    private final Character char_;
    private final Stats statS;
    private final HunterTalents talents;
    private final CombatFactors combatFactors;
    private final CalculationOptionsHunter calcOpts;
    private final BossOptions bossOpts;
    final AttackTable rwAtkTable;
    private final float fightDuration;
    private final float targets;
    private final float hsOverridesOverDur;
    private final float clOverridesOverDur;
    private final float steadyFreq;

    WhiteAttacks(Character character, Stats stats, CombatFactors cf, CalculationOptionsHunter calcOpts, BossOptions bossOpts) {
        this.char_ = character;
        this.statS = stats;
        this.talents = character.getHunterTalents() == null ? new HunterTalents() : character.getHunterTalents();
        this.combatFactors = cf;
        this.calcOpts = calcOpts;
        this.bossOpts = bossOpts;
        this.rwAtkTable = new AttackTable(char_, statS, combatFactors, calcOpts, false, false);
        this.fightDuration = (float) calcOpts.duration;
        this.targets = 1f;
        this.hsOverridesOverDur = 0f;
        this.clOverridesOverDur = 0f;
        this.steadyFreq = 0f;
    }
}
