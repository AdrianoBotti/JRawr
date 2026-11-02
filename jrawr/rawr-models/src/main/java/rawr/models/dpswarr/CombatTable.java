// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/CombatTable.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The C# `{ get; protected set; }` auto-properties are public fields (the port's convention);
// `levelDif` is computed once per Initialize from the non-Silverlight calcOpts.TargetLevel path.
package rawr.models.dpswarr;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.stats.WeightedStat;

public abstract class CombatTable {
    public static final CombatTable NULL = new NullCombatTable();

    protected Character char_;
    protected CalculationOptionsDPSWarr calcOpts;
    protected BossOptions bossOpts;
    protected CombatFactors combatFactors;
    protected Stats statS;
    protected Ability abil;
    protected boolean useSpellHit = false;

    public boolean isWhite;
    public boolean isMH;

    public int levelDif;

    public float miss;
    public float dodge;
    public float parry;
    public float block;
    public float glance;
    public float crit;
    public float hit;

    private float anyLand = 0f;
    private float anyNotLand = 1f;

    public float anyLand() {
        return this.anyLand;
    }

    public float anyNotLand() {
        return this.anyNotLand;
    }

    private boolean alwaysHit = false;

    protected void calculate() {
        this.anyNotLand = dodge + parry + miss;
        this.anyLand = 1f - this.anyNotLand;
    }

    protected void calculateAlwaysHit() {
        this.alwaysHit = true;
        miss = dodge = parry = block = glance = crit = 0f;
        hit = 1f;
        this.anyLand = 1f;
        this.anyNotLand = 0f;
    }

    protected void initialize(Character character, Stats stats, CombatFactors cf, CalculationOptionsDPSWarr co, BossOptions bo,
            Ability ability, boolean ismh, boolean useSpellHit, boolean alwaysHit) {
        char_ = character;
        statS = stats;
        calcOpts = co;
        bossOpts = bo;
        combatFactors = cf;
        abil = ability;
        isWhite = (abil == null);
        isMH = ismh;
        this.useSpellHit = useSpellHit;
        levelDif = calcOpts.getTargetLevel() - char_.getLevel();
        reset(alwaysHit);
    }

    protected void reset(boolean alwaysHit) {
        if (alwaysHit) calculateAlwaysHit();
        else calculate();
    }

    public void reset() {
        if (this.alwaysHit) return;
        reset(false);
    }
}

class NullCombatTable extends CombatTable {
    public NullCombatTable() {
        block = crit = hit = dodge = glance = miss = parry = 0;
    }
}

class AttackTable extends CombatTable {
    @Override
    protected void calculate() {
        float tableSize = 0f;

        // Miss
        if (useSpellHit) {
            miss = Math.min(1f - tableSize, Math.max(0.17f - (StatConversion.getHitFromRating(statS.getHitRating(), char_.getCharacterClass()) + statS.getSpellHit()), 0f));
        } else {
            miss = Math.min(1f - tableSize, isWhite ? combatFactors.cWMiss : combatFactors.cYMiss);
        }
        tableSize += miss;
        // Dodge
        if (isWhite || abil.canBeDodged) {
            dodge = Math.min(1f - tableSize, isMH ? combatFactors.cMhDodge : combatFactors.cOhDodge);
            tableSize += dodge;
        } else {
            dodge = 0f;
        }
        // Parry
        if (isWhite || abil.canBeParried) {
            parry = Math.min(1f - tableSize, isMH ? combatFactors.cMhParry : combatFactors.cOhParry);
            tableSize += parry;
        } else {
            parry = 0f;
        }
        // Block
        if (isWhite || abil.canBeBlocked) {
            block = Math.min(1f - tableSize, isMH ? combatFactors.cMhBlock : combatFactors.cOhBlock);
            tableSize += block;
        } else {
            block = 0f;
        }
        // Glancing Blow
        if (isWhite) {
            glance = Math.min(1f - tableSize, combatFactors.cGlance);
            tableSize += glance;
        } else {
            glance = 0f;
        }
        // Critical Hit
        crit = 0;
        if (isWhite) {
            float critValueToUse = (isMH ? combatFactors.cMhWCrit : combatFactors.cOhWCrit)
                    + StatConversion.NPC_LEVEL_CRIT_MOD[levelDif];
            for (WeightedStat ws : combatFactors.critProcs) {
                float modCritChance = Math.min(1f - tableSize, critValueToUse + StatConversion.getCritFromRating(ws.value(), char_.getCharacterClass()));
                crit += ws.chance() * modCritChance;
            }
            tableSize += crit;
        } else if (abil.canCrit) {
            float critValueToUse = StatConversion.NPC_LEVEL_CRIT_MOD[levelDif]
                    + (isMH ? combatFactors.cMhYCrit : combatFactors.cOhYCrit)
                    + abil.bonusCritChance;
            for (WeightedStat ws : combatFactors.critProcs) {
                float modCritChance = Math.min(1f - tableSize, (critValueToUse + StatConversion.getCritFromRating(ws.value(), char_.getCharacterClass())) * (1f - dodge - miss));
                crit += ws.chance() * modCritChance;
            }
            tableSize += crit;
        }
        // Normal Hit
        hit = Math.max(0f, 1f - tableSize);
        super.calculate();
    }

    public AttackTable() {
    }

    public AttackTable(Character character, Stats stats, CombatFactors cf, CalculationOptionsDPSWarr co, BossOptions bo, boolean ismh, boolean useSpellHit, boolean alwaysHit) {
        initialize(character, stats, cf, co, bo, null, ismh, useSpellHit, alwaysHit);
    }

    public AttackTable(Character character, Stats stats, CombatFactors cf, CalculationOptionsDPSWarr co, BossOptions bo, Ability ability, boolean ismh, boolean useSpellHit, boolean alwaysHit) {
        initialize(character, stats, cf, co, bo, ability, ismh, useSpellHit, alwaysHit);
    }
}
