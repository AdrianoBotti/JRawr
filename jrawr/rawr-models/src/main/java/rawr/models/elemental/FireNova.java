// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/FireNova.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.elemental;

public class FireNova extends Spell {
    private int additionalTargets = 0;

    public FireNova() {
        super();
    }

    public FireNova(ISpellArgs args) {
        super();
        initialize(args);
    }

    @Override
    protected void setBaseValues() {
        super.setBaseValues();

        baseMinDamage = 893;
        baseMaxDamage = 997;
        spCoef = .75f / 3.5f;
        manaCost = .22f * Constants.BaseMana;
        cooldown = 10;
    }

    @Override
    public void initialize(ISpellArgs args) {
        additionalTargets = args.getAdditionalTargets();
        if (additionalTargets < 0)
            additionalTargets = 0;
        if (additionalTargets > 9)
            additionalTargets = 9;

        totalCoef *= 1f + additionalTargets;

        shortName = "FN" + (1 + additionalTargets);

        spCoef += .1f * args.getTalents().getImprovedFireNova();

        cooldown -= 2 * args.getTalents().getImprovedFireNova();

        if (args.getTalents().isGlyphofFireNova())
            cooldown -= 3;

        super.initialize(args);
    }

    public int getAdditionalTargets() {
        return additionalTargets;
    }

    @Override
    public float getCCCritChance() {
        return Math.min(1f, getCritChance() * (1 + getAdditionalTargets()));
    }
}
