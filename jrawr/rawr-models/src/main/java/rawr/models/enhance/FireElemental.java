// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Enhance/FireElemental.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.enhance;

public class FireElemental {
    private final float ap;
    private final float sp;
    private final CombatStats cs;
    private final float critbuffs = 0f;
    private final float petMeleeMissRate;
    private final float petMeleeMultipliers;
    private final float petSpellMissRate;
    private final float petSpellMultipliers;

    private float meleeTotaldps = 0f;
    private float novaTotaldps = 0f;
    private float blastTotaldps = 0f;
    private float shieldTotaldps = 0f;
    private float totaldps = 0f;
    private float totalMana = 0f;
    private int novaUses = 0;
    private int blastUses = 0;
    private int meleeUses = 0;

    public FireElemental(float ap, float sp, float intellect, CombatStats cs, float critbuffs,
                         float petMeleeMissRate, float petMeleeMultipliers, float petSpellMissRate, float petSpellMultipliers) {
        this.ap = ap;
        this.sp = sp;
        this.cs = cs;
        // C# never assigns the critbuffs constructor parameter to _critbuffs (the field keeps its
        // 0f initializer); ported faithfully. See SUSPECTED_BUGS.
        this.petMeleeMissRate = petMeleeMissRate;
        this.petMeleeMultipliers = petMeleeMultipliers;
        this.petSpellMissRate = petSpellMissRate;
        this.petSpellMultipliers = petSpellMultipliers;

        meleeTotaldps = 0f;
        novaTotaldps = 0f;
        blastTotaldps = 0f;
        shieldTotaldps = 0f;
        totaldps = 0f;
        totalMana = 4000 + 15 * (0.3f * intellect);
        calculateUses();
        calculateDamage();
    }

    public float getDPS() {
        return totaldps;
    }

    public String getDPSOutput() {
        return String.format(java.util.Locale.ROOT, "%s*%s Melee dps\n%s Fire Blast dps\n%s Fire Nova dps\n%s Fire Shield dps",
                rawr.models.mage.NetFormat.f2(totaldps),
                rawr.models.mage.NetFormat.f2(meleeTotaldps),
                rawr.models.mage.NetFormat.f2(blastTotaldps),
                rawr.models.mage.NetFormat.f2(novaTotaldps),
                rawr.models.mage.NetFormat.f2(shieldTotaldps));
    }

    private void calculateDamage() {
        float meleeBasedps = (180 + .6f * sp + (2f / 14f) * ap) * meleeUses / 120f; //subject to glancing blows
        float blastBasedps = (775f + .2f * sp) * blastUses / 120f;
        float novaBasedps = (987.5f + .5f * sp) * novaUses / 120f;
        float shieldBasedps = (96 + .015f * sp) / 2f;

        float meleeNormal = meleeBasedps * (1 - petMeleeMissRate - critbuffs - cs.getGlancingRate());
        float meleeCrits = meleeBasedps * critbuffs * cs.getCritMultiplierMelee();
        float meleeGlances = meleeBasedps * cs.getGlancingHitModifier();
        meleeTotaldps = (meleeNormal + meleeCrits + meleeGlances) * petMeleeMultipliers * cs.getFireElementalUptime();

        float blastNormal = blastBasedps * (1 - petSpellMissRate - critbuffs);
        float blastCrits = blastBasedps * critbuffs * cs.getCritMultiplierSpell();
        blastTotaldps = (blastNormal + blastCrits) * petSpellMultipliers * cs.getFireElementalUptime();

        float novaNormal = novaBasedps * (1 - petSpellMissRate - critbuffs);
        float novaCrits = novaBasedps * critbuffs * cs.getCritMultiplierSpell();
        novaTotaldps = (novaNormal + novaCrits) * petSpellMultipliers * cs.getFireElementalUptime() * cs.getMultiTargetMultiplier();

        float shieldNormal = shieldBasedps * (1 - petSpellMissRate - critbuffs);
        float shieldCrits = shieldBasedps * critbuffs * cs.getCritMultiplierSpell();
        shieldTotaldps = (shieldNormal + shieldCrits) * petSpellMultipliers * cs.getFireElementalUptime() * cs.getMultiTargetMultiplier();

        totaldps = meleeTotaldps + novaTotaldps + blastTotaldps + shieldTotaldps;
    }

    private void calculateUses() {
        float time = 2f; // he tends to take a couple of seconds to get in range and start
        int sequence = 1;
        novaUses = 0;
        blastUses = 0;
        meleeUses = 0;
        while (totalMana > 0 && time < 120f) {
            switch (sequence) {
                case 1: // nova
                    if (totalMana > 207) {
                        novaUses++;
                        totalMana -= 207;
                        time += 2.5f;
                    }
                    break;
                case 2: // blast
                    if (totalMana > 276) {
                        blastUses++;
                        totalMana -= 276;
                        time += 4f;
                    }
                    break;
                case 3: // melee
                    meleeUses++;
                    time += 2f;
                    break;
            }
            sequence++;
            if (sequence == 4) sequence = 1;
        }
    }
}
