// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Healadin/Spell.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.healadin;

import rawr.base.util.NetFormat;

public class FlashOfLight extends Heal {
    public FlashOfLight(Rotation rotation) {
        super(rotation);
    }

    @Override
    public float getBaseCastTime() {
        return infusionOfLight ? 1.5f - 0.75f * getTalents().getInfusionOfLight() : 1.5f;
    }

    @Override
    public float getBaseMana() {
        return 307f;
    }

    private boolean infusionOfLight;

    public boolean getInfusionOfLight() {
        return infusionOfLight;
    }

    public void setInfusionOfLight(boolean value) {
        infusionOfLight = value;
    }

    @Override
    protected float abilityCritChance() {
        return (getTalents().isGlyphOfFlashOfLight() ? .05f : 0f) + getStats().getFlashOfLightCrit();
    }

    @Override
    protected float abilityHealedMultiplier() {
        return 1f + getStats().getFlashOfLightMultiplier();
    }

    public float baseAverageHealed() {
        return averageHealed() * (1f - ssPercentage);
    }

    private float ssPercentage = 0f;
    private float folHoTUptime = 0f;

    @Override
    protected float abilityHealed() {
        final float fol_coef = 1.5f / 3.5f * 66f / 35f * 1.25f;
        float baseHealed = (835.5f + (getStats().getSpellPower() + getStats().getFlashOfLightSpellPower()) * fol_coef);

        // Infusion of Light's Flash of Light HoT for Sacred Shield
        // Max HoT Uptime is 12 seconds.  If user is casting FoL more often than every 12 seconds, the HoT gets overridden
        // so we must calculate how long the HoT will be active.

        folHoTUptime = getRotation().foLCasts == 0f ? 12f : Math.min(getRotation().fightLength / (getRotation().foLCasts * getRotation().getCalcOpts().getFoLOnTank()), 12f);
        float ssHealed = baseHealed * (0.5f * getTalents().getInfusionOfLight()) * (folHoTUptime / 12f) * (1f + getStats().getFlashOfLightHoTMultiplier()) * getRotation().getCalcOpts().getSSUptime() * getRotation().getCalcOpts().getFoLOnTank();
        ssPercentage = ssHealed / (baseHealed + ssHealed);
        return baseHealed + ssHealed;
    }

    @Override
    public String toString() {
        if (getTalents().getInfusionOfLight() > 0 && getRotation().getCalcOpts().getSSUptime() > 0) {
            float averageHealed = averageHealed();
            float averageSSHealed = averageHealed * ssPercentage;
            return "Average Heal: " + NetFormat.grouped(averageHealed)
                    + "\nAverage Base Heal: " + NetFormat.grouped(averageHealed - averageSSHealed)
                    + "\nAverage Sacred Shield HoT Uptime: " + NetFormat.grouped(folHoTUptime, 2) + " sec"
                    + "\nAverage Sacred Shield Total HoT: " + NetFormat.grouped(averageSSHealed)
                    + "\nAverage Cost: " + NetFormat.grouped(averageCost())
                    + "\nHPS: " + NetFormat.grouped(hps())
                    + "\nHPM: " + NetFormat.grouped(hpm(), 2)
                    + "\nCast Time: " + NetFormat.grouped(castTime(), 2) + " sec"
                    + "\nCrit Chance: " + NetFormat.grouped(chanceToCrit() * 100, 2) + "%";
        } else {
            return super.toString();
        }
    }
}
