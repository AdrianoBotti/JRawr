// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/FlameShock.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the C# `+`/`*` operator overloads are omitted (never used by the model).
package rawr.models.elemental;

public class FlameShock extends Shock {
    public FlameShock() {
        super();
    }

    @Override
    protected void setBaseValues() {
        super.setBaseValues();

        baseMinDamage = 500;
        baseMaxDamage = 500;
        spCoef = 1.5f / 3.5f / 2f;
        dotSpCoef = .1f;
        periodicTick = 556f / 4f;
        periodicTicks = 6f;
        periodicTickTime = 3f;
        manaCost = .17f * Constants.BaseMana;
        cooldown = 6f;
        shortName = "FS";
    }

    @Override
    public void initialize(ISpellArgs args) {
        //for reference
        //dotTick = totalCoef * (periodicTick * dotBaseCoef + spellPower * dotSpCoef) * (1 + dotCanCrit * critModifier * CritChance)

        totalCoef += .01f * args.getTalents().getConcussion();
        directCoefBonus += .1f * args.getTalents().getBoomingEchoes();
        manaCost *= 1 - .02f * args.getTalents().getConvection();
        dotBaseCoef *= 1 + .2f * args.getTalents().getStormEarthAndFire();
        dotSpCoef *= 1 + .2f * args.getTalents().getStormEarthAndFire();
        dotBaseCoef *= 1 + args.getStats().getBonusFlameShockDoTDamage();
        dotSpCoef *= 1 + args.getStats().getBonusFlameShockDoTDamage();
        manaCost *= 1 - .45f * args.getTalents().getShamanisticFocus();
        cooldown -= .2f * args.getTalents().getReverberation();
        cooldown -= 1f * args.getTalents().getBoomingEchoes();
        spellPower += args.getStats().getSpellFireDamageRating();
        totalCoef *= 1 + args.getStats().getBonusFireDamageMultiplier();
        periodicTicks += args.getStats().getBonusFlameShockDuration() / periodicTickTime; // t9 2 piece

        if (args.getTalents().isGlyphofFlameShock())
            dotCritModifier += .6f;
        if (args.getTalents().isGlyphofShocking())
            gcd -= .5f;

        dotCanCrit = 1;

        applyDotHaste(args);
        super.initialize(args);
    }

    /**
     * Adds the closest amount of ticks to the passed time in seconds. Adds at least 2 ticks.
     *
     * @param seconds added time in seconds
     * @return Number of added DoT ticks
     */
    public float addTicks(float seconds) {
        float addTicks = (float) Math.rint(seconds / periodicTickTime);
        if (addTicks < 2)
            addTicks = 2;
        setPeriodicTicks(getPeriodicTicks() + addTicks);
        return addTicks;
    }

    public FlameShock(ISpellArgs args) {
        this();
        initialize(args);
    }
}
