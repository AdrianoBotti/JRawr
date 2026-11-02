// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/FireElemental.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.elemental;

// Thanks to Levva for help with Fire Elemental modelling.
// Elemental version currently only about 35% complete
// This will likely go through many iterations, hopefully live
// by 2.3.23 at the latest.
public class FireElemental extends Totem {
    private float ap = 0f;
    private float sp = 0f;
    private float petMeleeMissRate = 0f;
    private float petMeleeMultipliers = 0f;
    private float petSpellMissRate = 0f;
    private float petSpellMultipliers = 0f;

    private float meleeTotalDps;
    private float novaTotalDps;
    private float blastTotalDps;
    private float shieldTotalDps;
    private float totalDps;
    private float totalMana;
    private int novaUses;
    private int blastUses;
    private int meleeUses;
    private int additionalTargets;

    public FireElemental() {
        super();
    }

    public FireElemental(ISpellArgs args) {
        super();
        initialize(args);
    }

    @Override
    protected void setBaseValues() {
        super.setBaseValues();

        cooldown = 600;

        periodicTick = 0f;
        periodicTicks = 120f;
        periodicTickTime = 1f;
        manaCost = .23f * Constants.BaseMana;
        shortName = "FE";
        gcd = 1;
        castTime = 0;
    }

    @Override
    public void initialize(ISpellArgs args) {
        additionalTargets = args.getAdditionalTargets();
        if (additionalTargets < 0)
            additionalTargets = 0;
        else if (additionalTargets > 9)
            additionalTargets = 9;

        if (args.getTalents().isGlyphofFireElementalTotem())
            cooldown = 300;

        ap = args.getStats().getAttackPower();
        sp = args.getStats().getSpellPower();
        petMeleeMissRate = args.getStats().getHitRating();
        petMeleeMultipliers = args.getStats().getBonusPetDamageMultiplier();
        petSpellMissRate = args.getStats().getSpellHit();
        petSpellMultipliers = args.getStats().getBonusPetDamageMultiplier();

        meleeTotalDps = 0f;
        novaTotalDps = 0f;
        blastTotalDps = 0f;
        shieldTotalDps = 0f;
        totalDps = 0f;
        totalMana = 4000 + (15 * (0.3f * args.getStats().getIntellect()));
        calculateUses();
        calculateDamage();
        periodicTick = totalDps;
    }

    private void calculateDamage() {
        float meleeBaseDps = (180 + (.6f * sp) + (2f / 14f) * ap) * meleeUses / 120f; // Subject to glancing blows
        float blastBaseDps = (775f + (.2f * sp)) * blastUses / 120f;
        float novaBaseDps = (987.5f + (.5f * sp)) * novaUses / 120f;
        float shieldBaseDps = (96 + (.015f * sp)) / 2f;

        float meleeNormal = meleeBaseDps * (1 - petMeleeMissRate - crit - .24f);
        float meleeCrits = meleeBaseDps * getCritChance() * 1.5f;
        float meleeGlances = meleeBaseDps * .75f;
        meleeTotalDps = (meleeNormal + meleeNormal + meleeGlances) * petMeleeMultipliers;

        float blastNormal = blastBaseDps * (1 - petSpellMissRate - getCritChance());
        float blastCrits = blastBaseDps * 2f * getCritChance();
        blastTotalDps = (blastNormal + blastCrits) * petSpellMultipliers;

        float novaNormal = novaBaseDps * (1 - petSpellMissRate - getCritChance());
        float novaCrits = novaBaseDps * getCritChance() * 2f;
        novaTotalDps = (novaNormal + novaCrits) * petSpellMultipliers * (additionalTargets + 1);

        float shieldNormal = shieldBaseDps * (1 - petSpellMissRate - getCritChance());
        float shieldCrits = shieldBaseDps * getCritChance() * 2f;
        shieldTotalDps = (shieldNormal + shieldCrits) * petSpellMultipliers * (additionalTargets + 1);

        totalDps = meleeTotalDps + novaTotalDps + blastTotalDps + shieldTotalDps;
    }

    private void calculateUses() {
        float time = 2f;
        int sequence = 1;
        novaUses = 0;
        blastUses = 0;
        meleeUses = 0;

        while (totalMana > 0 && time < 120f) {
            switch (sequence) {
                case ABILITY_NOVA:
                    if (totalMana > 207) {
                        novaUses++;
                        totalMana -= 207;
                        time += 2.5f;
                    }
                    break;
                case ABILITY_BLAST:
                    if (totalMana > 276) {
                        blastUses++;
                        totalMana -= 276;
                        time += 4f;
                    }
                    break;
                case ABILITY_MELEE:
                    meleeUses++;
                    time += 2f;
                    break;
            }
            sequence++;
            if (sequence == 4) sequence = 1;
        }
    }

    // C# private enum AbilitySequence { nova = 1, blast = 2, melee = 3 }.
    private static final int ABILITY_NOVA = 1;
    private static final int ABILITY_BLAST = 2;
    private static final int ABILITY_MELEE = 3;
}
