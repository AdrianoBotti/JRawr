// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class SealOfVengeanceDoT extends Skill {

    public SealOfVengeanceDoT(CombatStats combats, float averageStack) {
        super(combats, AbilityType.Spell, DamageType.Holy, false, false);
        averageStackSize = averageStack;
    }

    public final float averageStackSize;

    @Override
    public float abilityDamage() {
        return averageStackSize * (stats.getSpellPower() * 0.065f + stats.getAttackPower() * 0.13f) / 5f * (1f
                + .03f * talents.getSealsOfThePure()
                + stats.getBonusSealOfVengeanceDamageMultiplier()
                + stats.getSealMultiplier());
    }

    @Override
    public float abilityCritChance() {
        return -1f;
    }

    @Override
    public float chanceToLand() {
        return 1f;
    }
}
