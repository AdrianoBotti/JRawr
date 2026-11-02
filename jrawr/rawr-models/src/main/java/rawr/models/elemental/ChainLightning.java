// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/ChainLightning.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the C# `+`/`*` operator overloads and the ILightningOverload interface (never referenced
// by type) are omitted.
package rawr.models.elemental;

public class ChainLightning extends Spell {
    private int additionalTargets = 0;
    private float loCoef, lightningSpellpower = 0f, lspCoef;

    public ChainLightning() {
        super();
    }

    @Override
    protected void setBaseValues() {
        super.setBaseValues();

        baseMinDamage = 973;
        baseMaxDamage = 1111;
        baseCastTime = 2f;
        castTime = 2f;
        spCoef = 2f / 3.5f;
        lspCoef = spCoef;
        loCoef = spCoef / 2f;
        manaCost = 0.26f * Constants.BaseMana;
        cooldown = 6f;
        lightningSpellpower = 0f;
    }

    @Override
    public void initialize(ISpellArgs args) {
        additionalTargets = args.getAdditionalTargets();
        // jumps
        if (additionalTargets < 0)
            additionalTargets = 0;
        if (additionalTargets > 3)
            additionalTargets = 3;
        shortName = "CL" + (1 + additionalTargets);
        if (!args.getTalents().isGlyphofChainLightning() && additionalTargets > 2)
            additionalTargets = 2;
        totalCoef *= new float[] { 1f, 1.7f, 2.19f, 2.533f, 2.7731f }[additionalTargets];

        manaCost *= 1f - .02f * args.getTalents().getConvection();
        totalCoef += .01f * args.getTalents().getConcussion();
        crit += .05f * args.getTalents().getCallOfThunder();
        spCoef += .04f * args.getTalents().getShamanism();
        loCoef += .04f * args.getTalents().getShamanism();
        castTime -= .1f * args.getTalents().getLightningMastery();
        cooldown -= new float[] { 0, .75f, 1.5f, 2.5f }[args.getTalents().getStormEarthAndFire()];
        crit += .01f * args.getTalents().getTidalMastery();
        spellPower += args.getStats().getSpellNatureDamageRating();
        lightningSpellpower += args.getStats().getLightningSpellPower();
        totalCoef *= 1 + args.getStats().getBonusNatureDamageMultiplier();

        lightningOverload = args.getTalents().getLightningOverload();

        super.initialize(args);
    }

    public ChainLightning(ISpellArgs args) {
        this();
        initialize(args);
    }

    public int getAdditionalTargets() {
        return additionalTargets;
    }

    private int lightningOverload = 0;

    @Override
    public float getCCCritChance() {
        return Math.min(1f, getCritChance() * (1 + getAdditionalTargets()) * (1 + loChance()));
    }

    @Override
    public float getMinHit() {
        return totalCoef * (baseMinDamage * baseCoef + spellPower * spCoef + lightningSpellpower * lspCoef);
    }

    @Override
    public float getMaxHit() {
        return totalCoef * (baseMaxDamage * baseCoef + spellPower * spCoef + lightningSpellpower * lspCoef);
    }

    public float loChance() {
        return .11f * lightningOverload / 3f * (1 + getAdditionalTargets());
    }

    @Override
    public float getTotalDamage() {
        return super.getTotalDamage() + loChance() * lightningOverloadDamage();
    }

    @Override
    public float getDirectDpS() {
        return (getAvgDamage() + loChance() * lightningOverloadDamage()) / getCastTime();
    }

    public float lightningOverloadDamage() {
        return totalCoef * ((baseMinDamage + baseMaxDamage) / 4f * baseCoef + spellPower * loCoef + lightningSpellpower * lspCoef) * (1 + getCritChance() * critModifier);
    }
}
