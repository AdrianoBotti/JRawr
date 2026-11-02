// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Bear/BearRotationCalculator.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Arithmetic order and float widths kept exactly; the large commented-out rotation block in the
// original is not code and is omitted.
package rawr.models.bear;

public class BearRotationCalculator {
    public float meleeDamage;
    public float maulDamage;
    public float mangleDamage;
    public float swipeDamage;
    public float faerieFireDamage;
    public float lacerateDamage;
    public float lacerateDotDamage;

    public float meleeThreat;
    public float maulThreat;
    public float mangleThreat;
    public float swipeThreat;
    public float faerieFireThreat;
    public float lacerateThreat;
    public float lacerateDotThreat;

    public float mangleCooldown;
    public float meleeSpeed;

    public BearRotationCalculator(float meleeDamage, float maulDamage, float mangleDamage, float swipeDamage,
            float faerieFireDamage, float lacerateDamage, float lacerateDotDamage, float meleeThreat, float maulThreat,
            float mangleThreat, float swipeThreat, float faerieFireThreat, float lacerateThreat, float lacerateDotThreat,
            float mangleCooldown, float meleeSpeed) {
        this.meleeDamage = meleeDamage;
        this.maulDamage = maulDamage;
        this.mangleDamage = mangleDamage;
        this.swipeDamage = swipeDamage;
        this.faerieFireDamage = faerieFireDamage;
        this.lacerateDamage = lacerateDamage;
        this.lacerateDotDamage = lacerateDotDamage;

        this.meleeThreat = meleeThreat;
        this.maulThreat = maulThreat;
        this.mangleThreat = mangleThreat;
        this.swipeThreat = swipeThreat;
        this.faerieFireThreat = faerieFireThreat;
        this.lacerateThreat = lacerateThreat;
        this.lacerateDotThreat = lacerateDotThreat;

        this.mangleCooldown = mangleCooldown;
        this.meleeSpeed = meleeSpeed;
    }

    private enum BearAttack {
        None,
        Mangle,
        Swipe,
        FaerieFire,
        Lacerate
    }

    /**
     * Gets calculations about a bear rotation.
     *
     * @param useMaul null = no auto or maul, false = auto, true = maul
     */
    public BearRotationCalculation getRotationCalculations(Boolean useMaul, boolean useMangle, boolean useSwipe,
            boolean useFaerieFire, boolean useLacerate) {
        // 36 GCDs, 54 sec
        BearAttack[] attacks = new BearAttack[36];
        java.util.Arrays.fill(attacks, BearAttack.None); // C# reuses default(BearAttack) == None

        if (useMangle) {
            for (int i = 0; i < attacks.length; i++) {
                if (attacks[i] == BearAttack.None) {
                    attacks[i] = BearAttack.Mangle;
                    i += (int) Math.ceil((double) (mangleCooldown / 1.5f)) - 1;
                }
            }
        }

        if (useFaerieFire) {
            for (int i = 0; i < attacks.length; i++) {
                if (attacks[i] == BearAttack.None) {
                    attacks[i] = BearAttack.FaerieFire;
                    i += 3;
                }
            }
        }

        if (useLacerate) {
            for (int i = 0; i < attacks.length; i++) {
                if (attacks[i] == BearAttack.None) {
                    attacks[i] = BearAttack.Lacerate;
                    if (useSwipe) i += 4;
                }
            }
        }

        if (useSwipe) {
            for (int i = 0; i < attacks.length; i++) {
                if (attacks[i] == BearAttack.None) {
                    attacks[i] = BearAttack.Swipe;
                }
            }
        }

        int countMangle = 0, countFaerieFire = 0, countLacerate = 0, countSwipe = 0, countLacerateDot = useLacerate ? 18 : 0;
        for (int i = 0; i < attacks.length; i++) {
            switch (attacks[i]) {
                case Mangle: countMangle++; break;
                case Swipe: countSwipe++; break;
                case FaerieFire: countFaerieFire++; break;
                case Lacerate: countLacerate++; break;
            }
        }

        float attackDPS = (countMangle * mangleDamage + countSwipe * swipeDamage + countFaerieFire * faerieFireDamage
                + countLacerate * lacerateDamage + countLacerateDot * lacerateDotDamage) / 54f;
        float attackTPS = (countMangle * mangleThreat + countSwipe * swipeThreat + countFaerieFire * faerieFireThreat
                + countLacerate * lacerateThreat + countLacerateDot * lacerateDotThreat) / 54f;

        if (useMaul != null && !useMaul) { // Just melee attacks, no mauls
            attackDPS += (meleeDamage / meleeSpeed);
            attackTPS += (meleeThreat / meleeSpeed);
        } else if (useMaul != null && useMaul) { // Use mauls
            attackDPS += (maulDamage / meleeSpeed);
            attackTPS += (maulThreat / meleeSpeed);
        }

        StringBuilder strRotationName = new StringBuilder();
        if (useMaul != null && !useMaul) strRotationName.append("Melee+");
        if (useMaul != null && useMaul) strRotationName.append("Maul+");
        if (useMangle) strRotationName.append("Mangle+");
        if (useSwipe) strRotationName.append("Swipe+");
        if (useLacerate) strRotationName.append("Lacerate+");
        if (useFaerieFire) strRotationName.append("FaerieFire+");
        if (strRotationName.length() == 0) strRotationName.append("None");
        else strRotationName.setLength(strRotationName.length() - 1);

        BearRotationCalculation result = new BearRotationCalculation();
        result.name = strRotationName.toString();
        result.dps = attackDPS;
        result.tps = attackTPS;
        return result;
    }

    public static class BearRotationCalculation {
        public String name;
        public float dps;
        public float tps;
    }
}
