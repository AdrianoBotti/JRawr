// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class SealOfVengeance extends Skill {

    public SealOfVengeance(CombatStats combats, float averageStack) {
        super(combats, AbilityType.Melee, DamageType.Holy, true, false);
        averageStackSize = averageStack;
    }

    public final float averageStackSize;

    @Override
    public float abilityDamage() {
        return (combats.weaponDamage * 0.066f * averageStackSize) * (1f
                + talents.getSealsOfThePure() * .03f
                + stats.getBonusSealOfVengeanceDamageMultiplier()
                + stats.getSealMultiplier());
    }

    @Override
    public float chanceToLand() {
        return 1f;
    }
}
