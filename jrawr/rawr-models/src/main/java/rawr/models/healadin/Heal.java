// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Healadin/Spell.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// (the Heal hierarchy lives in Spell.cs in the original).
// Display ToString() uses the .NET N0/N2 formats of rawr.base.util.NetFormat.
package rawr.models.healadin;

import rawr.base.util.NetFormat;
import rawr.base.talents.PaladinTalents;
import rawr.base.stats.Stats;

public abstract class Heal {
    private Rotation rotation;

    public Rotation getRotation() {
        return this.rotation;
    }

    public void setRotation(Rotation value) {
        this.rotation = value;
        this.stats = this.rotation.getStats();
        this.talents = this.rotation.getTalents();
    }

    private Stats stats;

    protected Stats getStats() {
        return this.stats;
    }

    private PaladinTalents talents;

    protected PaladinTalents getTalents() {
        return this.talents;
    }

    public Heal(Rotation rotation) {
        setRotation(rotation);
    }

    public float hps() {
        return averageHealed() / castTime();
    }

    public float mps() {
        return averageCost() / castTime();
    }

    public float hpm() {
        return averageHealed() / averageCost();
    }

    public float castTime() {
        return (float) Math.max(1f, (getBaseCastTime() - abilityCastTimeReduction()) / (1f + getStats().getSpellHaste()));
    }

    public float averageCost() {
        return (float) Math.floor((getBaseMana() * (divineIllumination ? 0.5f : 1f) - costReduction())
                * (abilityCostMultiplier() - (getTalents().isGlyphOfSealOfWisdom() ? .05f : 0f)))
                - getBaseMana() * .06f * getTalents().getIllumination() * chanceToCrit();
    }

    public float averageHealed() {
        return baseHealed() * (1f - chanceToCrit()) + critHealed() * chanceToCrit();
    }

    public float critHealed() {
        return baseHealed() * 1.5f * (1f + getStats().getBonusCritHealMultiplier()) * abilityCritMultiplier();
    }

    public float baseHealed() {
        float heal = abilityHealed();

        heal *= getTalents().isGlyphOfSealOfLight() ? 1.05f : 1f;
        heal *= 1f + getStats().getHealingReceivedMultiplier();
        heal *= 1f - this.rotation.divinePleas * 15f / this.rotation.fightLength * .5f;
        heal *= 1f + .01f * getTalents().getDivinity();
        heal *= abilityHealedMultiplier();
        heal *= (1f + getTalents().getHealingLight() * .04f);
        heal *= (1f + getStats().getBonusHealingDoneMultiplier());
        if (divineIllumination) {
            heal *= (1f + getStats().getDivineIlluminationHealingMultiplier());
        }

        return heal;
    }

    public float chanceToCrit() {
        return (float) Math.max(0f, (float) Math.min(1f, getStats().getSpellCrit() + abilityCritChance() + extraCritChance + getTalents().getHolyPower() * .01f));
    }

    public float costReduction() {
        return getStats().getSpellsManaReduction() + abilityCostReduction();
    }

    private float extraCritChance;

    public float getExtraCritChance() {
        return extraCritChance;
    }

    public void setExtraCritChance(float value) {
        extraCritChance = value;
    }

    private boolean divineIllumination;

    public boolean getDivineIllumination() {
        return divineIllumination;
    }

    public void setDivineIllumination(boolean value) {
        divineIllumination = value;
    }

    protected abstract float abilityHealed();

    protected float abilityCritChance() {
        return 0f;
    }

    protected float abilityCostReduction() {
        return 0f;
    }

    protected float abilityCastTimeReduction() {
        return 0f;
    }

    protected float abilityCostMultiplier() {
        return 1f;
    }

    protected float abilityHealedMultiplier() {
        return 1f;
    }

    protected float abilityCritMultiplier() {
        return 1f;
    }

    public abstract float getBaseCastTime();

    public abstract float getBaseMana();

    @Override
    public String toString() {
        return "Average Heal: " + NetFormat.grouped(averageHealed())
                + "\nAverage Cost: " + NetFormat.grouped(averageCost())
                + "\nHPS: " + NetFormat.grouped(hps())
                + "\nHPM: " + NetFormat.grouped(hpm(), 2)
                + "\nCast Time: " + NetFormat.grouped(castTime(), 2) + " sec"
                + "\nCrit Chance: " + NetFormat.grouped(chanceToCrit() * 100, 2) + "%";
    }
}
