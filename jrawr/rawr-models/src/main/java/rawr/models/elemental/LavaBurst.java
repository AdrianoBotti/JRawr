// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/LavaBurst.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the C# `+`/`*` operator overloads are omitted (never used by the model).
package rawr.models.elemental;

public class LavaBurst extends Spell {
    private float fs = 0f;
    private boolean elementalT10 = false;

    public LavaBurst() {
        super();
    }

    @Override
    protected void setBaseValues() {
        super.setBaseValues();

        baseMinDamage = 1192;
        baseMaxDamage = 1518;
        baseCastTime = 2f;
        castTime = 2f;
        spCoef = 2f / 3.5f;
        manaCost = .1f * Constants.BaseMana;
        cooldown = 8f;
        shortName = "LvB";
    }

    @Override
    public void initialize(ISpellArgs args) {
        initialize(args, fs);
    }

    public void initialize(ISpellArgs args, float fs) {
        manaCost *= 1f - .02f * args.getTalents().getConvection();
        totalCoef += .01f * args.getTalents().getConcussion();
        totalCoef += .02f * args.getTalents().getCallOfFlame();
        totalCoef += args.getStats().getBonusLavaBurstDamageMultiplier(); // t9 4 piece
        castTime -= .1f * args.getTalents().getLightningMastery();
        spCoef += .05f * args.getTalents().getShamanism();
        critModifier += new float[] { 0f, 0.06f, 0.12f, 0.24f }[args.getTalents().getLavaFlows()];
        critModifier += args.getStats().getBonusLavaBurstCritDamage() / 100f; // t7 4 piece
        baseMinDamage += args.getStats().getLavaBurstBonus(); // Totem (relic)
        baseMaxDamage += args.getStats().getLavaBurstBonus(); // Totem (relic)
        spellPower += args.getStats().getSpellFireDamageRating();
        if (args.getTalents().isGlyphofLava())
            spCoef += .1f;
        totalCoef *= 1 + args.getStats().getBonusFireDamageMultiplier();
        if (args.getStats().getElemental4T10() > 0) elementalT10 = true;

        super.initialize(args);

        this.fs = fs;
        crit = (1 - fs) * crit + fs;
    }

    public LavaBurst(ISpellArgs args, float fs) {
        this();
        this.fs = fs;
        initialize(args, fs);
    }

    /**
     * Adds the closest amount of hasted FS ticks to 6s to the already running FS.
     */
    public boolean getElementalT10() {
        return elementalT10;
    }
}
