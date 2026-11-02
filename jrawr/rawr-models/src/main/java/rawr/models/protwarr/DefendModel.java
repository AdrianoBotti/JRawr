// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtWarr/DefendModel.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protwarr;

import rawr.base.character.Character;
import rawr.base.stats.Stats;

public class DefendModel {
    private Character character;
    private CalculationOptionsProtWarr options;
    private Stats stats;

    public final ParryModel parryModel;
    public final DefendTable defendTable;

    private float averageDamagePerAttack;

    public float getAverageDamagePerAttack() {
        return averageDamagePerAttack;
    }

    public void setAverageDamagePerAttack(float value) {
        averageDamagePerAttack = value;
    }

    private float averageDamagePerHit;

    public float getAverageDamagePerHit() {
        return averageDamagePerHit;
    }

    public void setAverageDamagePerHit(float value) {
        averageDamagePerHit = value;
    }

    private float damagePerHit;

    public float getDamagePerHit() {
        return damagePerHit;
    }

    public void setDamagePerHit(float value) {
        damagePerHit = value;
    }

    private float damagePerBlock;

    public float getDamagePerBlock() {
        return damagePerBlock;
    }

    public void setDamagePerBlock(float value) {
        damagePerBlock = value;
    }

    private float damagePerCritBlock;

    public float getDamagePerCritBlock() {
        return damagePerCritBlock;
    }

    public void setDamagePerCritBlock(float value) {
        damagePerCritBlock = value;
    }

    private float damagePerCrit;

    public float getDamagePerCrit() {
        return damagePerCrit;
    }

    public void setDamagePerCrit(float value) {
        damagePerCrit = value;
    }

    private float damagePerSecond;

    public float getDamagePerSecond() {
        return damagePerSecond;
    }

    public void setDamagePerSecond(float value) {
        damagePerSecond = value;
    }

    private float guaranteedReduction;

    public float getGuaranteedReduction() {
        return guaranteedReduction;
    }

    public void setGuaranteedReduction(float value) {
        guaranteedReduction = value;
    }

    private float mitigation;

    public float getMitigation() {
        return mitigation;
    }

    public void setMitigation(float value) {
        mitigation = value;
    }

    private float tankPoints;

    public float getTankPoints() {
        return tankPoints;
    }

    public void setTankPoints(float value) {
        tankPoints = value;
    }

    private float effectiveHealth;

    public float getEffectiveHealth() {
        return effectiveHealth;
    }

    public void setEffectiveHealth(float value) {
        effectiveHealth = value;
    }

    private float burstTime;

    public float getBurstTime() {
        return burstTime;
    }

    public void setBurstTime(float value) {
        burstTime = value;
    }

    public void calculate() {
        float attackSpeed = parryModel.getBossAttackSpeed();
        float armorReduction = (1.0f - Lookup.armorReduction(character, stats, options.getTargetLevel()));
        float baseDamagePerSecond = options.getBossAttackValue() / options.getBossAttackSpeed();
        float guaranteedReduction = (Lookup.stanceDamageReduction(character, stats) * armorReduction);
        float baseAttack = options.getBossAttackValue() * guaranteedReduction;
        float absorbed = stats.getDamageAbsorbed();

        damagePerHit = baseAttack * (1.0f + stats.getPhysicalDamageTakenMultiplier()) - absorbed;
        damagePerCrit = baseAttack * 2.0f * (1.0f + stats.getPhysicalDamageTakenMultiplier()) - absorbed;
        damagePerBlock = Math.max(0.0f, baseAttack - stats.getBlockValue()) * (1.0f + stats.getPhysicalDamageTakenMultiplier()) - absorbed;
        damagePerCritBlock = Math.max(0.0f, baseAttack - (stats.getBlockValue() * 2.0f)) * (1.0f + stats.getPhysicalDamageTakenMultiplier()) - absorbed;

        averageDamagePerHit =
                damagePerHit * (defendTable.getHit() / defendTable.getAnyHit()) +
                        damagePerCrit * (defendTable.getCritical() / defendTable.getAnyHit()) +
                        damagePerBlock * (defendTable.getBlock() / defendTable.getAnyHit()) * (1.0f - character.getWarriorTalents().getCriticalBlock() * 0.2f) +
                        damagePerCritBlock * (defendTable.getBlock() / defendTable.getAnyHit()) * (character.getWarriorTalents().getCriticalBlock() * 0.2f);
        averageDamagePerAttack =
                damagePerHit * defendTable.getHit() +
                        damagePerCrit * defendTable.getCritical() +
                        damagePerBlock * defendTable.getBlock() * (1.0f - character.getWarriorTalents().getCriticalBlock() * 0.2f) +
                        damagePerCritBlock * defendTable.getBlock() * (character.getWarriorTalents().getCriticalBlock() * 0.2f);

        damagePerSecond = averageDamagePerAttack / attackSpeed;
        mitigation = (1.0f - (damagePerSecond / baseDamagePerSecond));
        tankPoints = (stats.getHealth() / (1.0f - mitigation));
        effectiveHealth = (stats.getHealth() / guaranteedReduction);
        this.guaranteedReduction = (1.0f - guaranteedReduction);

        double a = (double) defendTable.getAnyMiss();
        double h = (double) stats.getHealth();
        double H = (double) averageDamagePerHit;
        double s = (double) (parryModel.getBossAttackSpeed() / options.getBossAttackSpeed());
        burstTime = (float) ((1.0d / a) * ((1.0d / Math.pow(1.0d - a, h / H)) - 1.0d) * s);
    }

    public DefendModel(Character character, Stats stats, CalculationOptionsProtWarr options) {
        this.character = character;
        this.stats = stats;
        this.options = options;
        parryModel = new ParryModel(character, stats, options);
        defendTable = new DefendTable(character, stats, options);
        calculate();
    }
}
