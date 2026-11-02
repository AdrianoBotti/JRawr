// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/LightningBolt.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the C# `+`/`*` operator overloads and the ILightningOverload interface (never referenced
// by type) are omitted.
package rawr.models.elemental;

public class LightningBolt extends Spell {
    public LightningBolt() {
        super();
    }

    @Override
    protected void setBaseValues() {
        super.setBaseValues();

        baseMinDamage = 719;
        baseMaxDamage = 819;
        baseCastTime = 2.5f;
        castTime = 2.5f;
        spCoef = 2.5f / 3.5f;
        lspCoef = spCoef;
        loCoef = spCoef / 2f;
        manaCost = 0.1f * Constants.BaseMana;
        lightningSpellpower = 0f;
        shortName = "LB";
    }

    @Override
    public void initialize(ISpellArgs args) {
        castTime -= .1f * args.getTalents().getLightningMastery();
        manaCost *= 1f - .02f * args.getTalents().getConvection();
        totalCoef += .01f * args.getTalents().getConcussion();
        crit += .05f * args.getTalents().getCallOfThunder();
        spCoef += .04f * args.getTalents().getShamanism();
        loCoef += .04f * args.getTalents().getShamanism();
        crit += .05f * args.getTalents().getTidalMastery();
        manaCost *= 1 - args.getStats().getLightningBoltCostReduction() / 100f; // T7 2 piece
        spellPower += args.getStats().getSpellNatureDamageRating(); // Nature SP
        lightningSpellpower += args.getStats().getLightningSpellPower(); // Totem (relic) is not affected by shamanism
        if (args.getTalents().isGlyphofLightningBolt()) totalCoef += .04f;
        totalCoef *= 1 + args.getStats().getBonusNatureDamageMultiplier();
        totalCoef *= 1 + args.getStats().getLightningBoltDamageModifier() / 100f; // T6 4 piece

        lightningOverload = args.getTalents().getLightningOverload();

        super.initialize(args);
        critModifier *= 1 + args.getStats().getLightningBoltCritDamageModifier(); // T8 4 piece
    }

    public LightningBolt(ISpellArgs args) {
        this();
        initialize(args);
    }

    private int lightningOverload = 0;
    private float loCoef, lightningSpellpower = 0f, lspCoef;

    @Override
    public float getCCCritChance() {
        return Math.min(1f, getCritChance() * (1f + loChance()));
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
        return .11f * lightningOverload;
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
