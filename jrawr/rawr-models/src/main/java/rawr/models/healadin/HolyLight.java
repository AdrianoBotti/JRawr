// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Healadin/Spell.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.healadin;

public class HolyLight extends Heal {
    public HolyLight(Rotation rotation) {
        super(rotation);
    }

    @Override
    public float getBaseCastTime() {
        return castTimeReductionFromHolyShock ? 2.5f - getStats().getHolyLightCastTimeReductionFromHolyShock() : 2.5f;
    }

    @Override
    public float getBaseMana() {
        return 1274f;
    }

    private boolean castTimeReductionFromHolyShock;

    public boolean getCastTimeReductionFromHolyShock() {
        return castTimeReductionFromHolyShock;
    }

    public void setCastTimeReductionFromHolyShock(boolean value) {
        castTimeReductionFromHolyShock = value;
    }

    @Override
    protected float abilityCritChance() {
        return getTalents().getSanctifiedLight() * .02f + getStats().getHolyLightCrit();
    }

    @Override
    protected float abilityCostReduction() {
        return getStats().getHolyLightManaCostReduction();
    }

    @Override
    protected float abilityCostMultiplier() {
        return 1f - getStats().getHolyLightPercentManaReduction();
    }

    @Override
    protected float abilityCastTimeReduction() {
        return .5f / 3 * getTalents().getLightsGrace();
    }

    @Override
    protected float abilityHealed() {
        final float hl_coef = 2.5f / 3.5f * 66f / 35f * 1.25f;
        return (5166f + (getStats().getHolyLightSpellPower() + getStats().getSpellPower()) * hl_coef);
    }

    public float glyphOfHolyLight(float hlHealed) {
        return hlHealed * (getTalents().isGlyphOfHolyLight() ? .1f * getRotation().getCalcOpts().getGHL_Targets() : 0f);
    }
}
