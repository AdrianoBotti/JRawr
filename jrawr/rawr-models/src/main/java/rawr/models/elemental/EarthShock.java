// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/EarthShock.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the C# `+`/`*` operator overloads are omitted (never used by the model).
package rawr.models.elemental;

public class EarthShock extends Shock {
    public EarthShock() {
        super();
    }

    @Override
    protected void setBaseValues() {
        super.setBaseValues();
        baseMinDamage = 854;
        baseMaxDamage = 900;
        spCoef = 1.5f / 3.5f * .9f;
        manaCost = .18f * Constants.BaseMana;
        cooldown = 6f;
        shortName = "ES";
    }

    @Override
    public void initialize(ISpellArgs args) {
        totalCoef += .01f * args.getTalents().getConcussion();
        manaCost *= 1 - .02f * args.getTalents().getConvection();
        manaCost *= 1 - .45f * args.getTalents().getShamanisticFocus();
        cooldown -= .2f * args.getTalents().getReverberation();
        spellPower += args.getStats().getSpellNatureDamageRating();
        totalCoef *= 1 + args.getStats().getBonusNatureDamageMultiplier();
        if (args.getTalents().isGlyphofShocking())
            gcd -= .5f;

        super.initialize(args);
    }

    public EarthShock(ISpellArgs args) {
        this();
        initialize(args);
    }
}
