// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class White extends Skill {

    public White(CombatStats combats) {
        super(combats, AbilityType.Melee, DamageType.Physical, true, false);
    }

    @Override
    public float abilityDamage() {
        return combats.weaponDamage;
    }

    @Override
    public float averageDamage() {
        final float glanceChance = .24f;
        final float glancingAmount = 1f - 0.25f;
        return hitDamage() *
                (glanceChance * glancingAmount +
                        critChance() * critBonus() +
                        (chanceToLand() - critChance() - glanceChance));
    }

    public float whiteDPS() {
        return averageDamage() / combats.attackSpeed + stats.getMoteOfAnger() * averageDamage();
    }
}
