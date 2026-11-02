// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Healadin/Spell.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.healadin;

public class HolyShock extends Heal {
    public HolyShock(Rotation rotation) {
        super(rotation);
    }

    @Override
    public float getBaseCastTime() {
        return 1.5f;
    }

    @Override
    public float getBaseMana() {
        return 790f;
    }

    @Override
    protected float abilityCritChance() {
        return getTalents().getSanctifiedLight() * .02f + getStats().getHolyShockCrit();
    }

    @Override
    protected float abilityCostMultiplier() {
        return 1f - .02f * getTalents().getBenediction();
    }

    @Override
    protected float abilityHealed() {
        final float hs_coef = 1.5f / 3.5f * 66f / 35f;
        return (2500f + getStats().getSpellPower() * hs_coef);
    }

    public float usage() {
        return casts() * averageCost();
    }

    public float casts() {
        return getRotation().fightLength * getRotation().getCalcOpts().getHolyShock() / cooldown();
    }

    public float time() {
        return casts() * castTime();
    }

    public float healed() {
        return casts() * averageHealed();
    }

    public float cooldown() {
        return getTalents().isGlyphOfHolyShock() ? 5f : 6f;
    }

    @Override
    protected float abilityCritMultiplier() {
        return 1f + getStats().getHolyShockHoTOnCrit();
    }
}
