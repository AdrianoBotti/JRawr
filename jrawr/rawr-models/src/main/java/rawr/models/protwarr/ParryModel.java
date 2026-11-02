// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtWarr/ParryModel.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protwarr;

import rawr.base.character.Character;
import rawr.base.stats.Stats;

public class ParryModel {
    private Character character;
    private CalculationOptionsProtWarr options;
    private Stats stats;

    private AttackTable attackTable;
    private DefendTable defendTable;

    private float bossAttackSpeed;

    public float getBossAttackSpeed() {
        return bossAttackSpeed;
    }

    private float weaponSpeed;

    public float getWeaponSpeed() {
        return weaponSpeed;
    }

    private void calculate() {
        float globalCooldownSpeed = Lookup.globalCooldownSpeed(character, true);
        float baseBossAttackSpeed = options.getBossAttackSpeed() * (1.0f - stats.getBossAttackSpeedMultiplier());
        float baseWeaponSpeed = Lookup.weaponSpeed(character, stats);
        float bossAttackHaste = 0.0f;
        float weaponHaste = 0.0f;

        bossAttackSpeed = baseBossAttackSpeed;
        weaponSpeed = baseWeaponSpeed;

        if (options.getUseParryHaste()) {
            // Iterate on this a few times to get a 'stable' result
            for (int j = 0; j < 4; j++) {
                weaponHaste = defendTable.getParry() * 0.24f * (weaponSpeed / bossAttackSpeed);
                bossAttackHaste = attackTable.getParry() * 0.24f * ((bossAttackSpeed / weaponSpeed) + (bossAttackSpeed / globalCooldownSpeed));

                weaponSpeed = baseWeaponSpeed / (1.0f + weaponHaste);
                bossAttackSpeed = baseBossAttackSpeed / (1.0f + bossAttackHaste);
            }
        } else {
            // Simple adjust to the defender's speed if the attacker isn't parry hasted
            weaponSpeed /= (1.0f + (defendTable.getParry() * 0.24f * (weaponSpeed / bossAttackSpeed)));
        }
    }

    public ParryModel(Character character, Stats stats, CalculationOptionsProtWarr options) {
        this.character = character;
        this.stats = stats;
        this.options = options;
        attackTable = new AttackTable(character, stats, options);
        defendTable = new DefendTable(character, stats, options);

        calculate();
    }
}
