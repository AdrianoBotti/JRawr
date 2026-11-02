// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtPaladin/ParryModel.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protpally;

import rawr.base.character.Character;
import rawr.base.stats.Stats;

public class ParryModel {
    private Character character;
    private CalculationOptionsProtPaladin calcOpts;
    private Stats stats;

    private AttackTable attackTable;
    private DefendTable defendTable;

    public float bossAttackSpeed;
    public float weaponSpeed;

    private void calculate() {
        float baseBossAttackSpeed = calcOpts.getBossAttackSpeed() * (1f - stats.getBossAttackSpeedMultiplier());
        float baseWeaponSpeed = Lookup.weaponSpeed(character, stats);
        float bossAttackHaste = 0.0f;
        float weaponHaste = 0.0f;

        bossAttackSpeed = baseBossAttackSpeed;
        weaponSpeed = baseWeaponSpeed;

        if (calcOpts.getUseParryHaste()) {
            // Iterate on this a few times to get a 'stable' result
            for (int j = 0; j < 4; j++) {
                bossAttackHaste = attackTable.getParry() * 0.24f * ((bossAttackSpeed / weaponSpeed) + (bossAttackSpeed / 1.5f));
                weaponHaste = defendTable.getParry() * 0.24f * (weaponSpeed / bossAttackSpeed);

                bossAttackSpeed = baseBossAttackSpeed / (1.0f + bossAttackHaste);
                weaponSpeed = baseWeaponSpeed / (1.0f + weaponHaste);
            }
        } else {
            // Simple adjust to the defender's speed if the attacker isn't parry hasted
            weaponSpeed /= (1.0f + (defendTable.getParry() * 0.24f * (weaponSpeed / bossAttackSpeed)));
            bossAttackSpeed = baseBossAttackSpeed;
        }
    }

    public ParryModel(Character character, Stats stats, CalculationOptionsProtPaladin calcOpts) {
        this.character = character;
        this.stats = stats;
        this.calcOpts = calcOpts;
        attackTable = new AttackTable(character, stats, calcOpts);
        defendTable = new DefendTable(character, stats, calcOpts, true);
        calculate();
    }
}
