// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtPaladin/DefendModel.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protpally;

import rawr.base.character.Character;
import rawr.base.stats.Stats;

class DefendModel {
    private Character character;
    private CalculationOptionsProtPaladin calcOpts;
    private Stats stats;

    public final ParryModel parryModel;
    public final DefendTable defendTable;

    public float averageDamagePerAttack;
    public float averageDamagePerHit;
    public float damagePerHit;
    public float damagePerBlock;
    public float damagePerCrit;
    public float damagePerSecond;
    public float guaranteedReduction;
    public float mitigation;
    public float damageTaken;
    public float[] resistanceTable;
    public float tankPoints;
    public float effectiveHealth;
    public float burstTime;

    public void calculate() {
        float attackSpeed = parryModel.bossAttackSpeed;
        float armorReduction = (1.0f - Lookup.armorReduction(character, stats, calcOpts.getTargetLevel()));
        float baseDamagePerSecond = calcOpts.getBossAttackValue() / calcOpts.getBossAttackSpeed();
        float guaranteedReduction = (Lookup.stanceDamageReduction(character, stats) * armorReduction);
        float absorbed = stats.getDamageAbsorbed();

        damagePerHit = (calcOpts.getBossAttackValue() * guaranteedReduction) - absorbed;
        damagePerCrit = (2.0f * damagePerHit);
        damagePerBlock = Math.max(0.0f, damagePerHit - Lookup.activeBlockReduction(character, stats));

        averageDamagePerHit =
                damagePerHit * (defendTable.getHit() / defendTable.anyHit()) +
                damagePerCrit * (defendTable.getCritical() / defendTable.anyHit()) +
                damagePerBlock * (defendTable.getBlock() / defendTable.anyHit());
        averageDamagePerAttack =
                damagePerHit * defendTable.getHit() +
                damagePerCrit * defendTable.getCritical() +
                damagePerBlock * defendTable.getBlock();

        float reductionAD = 1.0f - Lookup.ardentDefenderReduction(character);
        float healthAD = (0.65f + (0.35f / reductionAD)) * stats.getHealth();

        damagePerSecond = averageDamagePerAttack / attackSpeed;
        damageTaken = damagePerSecond / baseDamagePerSecond;
        mitigation = (1.0f - (damagePerSecond / baseDamagePerSecond));
        tankPoints = (healthAD / (1.0f - mitigation));
        effectiveHealth = (healthAD / guaranteedReduction);
        this.guaranteedReduction = (1.0f - guaranteedReduction);

        double a = (double) defendTable.anyMiss();
        double h = (double) healthAD;
        double H = (double) averageDamagePerHit;
        double s = (double) (parryModel.bossAttackSpeed / calcOpts.getBossAttackSpeed());
        burstTime = (float) ((1.0d / a) * ((1.0d / Math.pow(1.0d - a, h / H)) - 1.0d) * s);
    }

    public DefendModel(Character character, Stats stats, CalculationOptionsProtPaladin calcOpts, boolean useHolyShield) {
        this.character = character;
        this.stats = stats;
        this.calcOpts = calcOpts;

        parryModel = new ParryModel(character, stats, calcOpts);
        defendTable = new DefendTable(character, stats, calcOpts, useHolyShield);
        calculate();
    }
}
