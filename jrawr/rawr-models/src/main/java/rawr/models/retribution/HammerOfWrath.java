// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class HammerOfWrath extends Skill {

    public HammerOfWrath(CombatStats combats) {
        super(combats, AbilityType.Range, DamageType.Holy, false, false);
    }

    @Override
    public Ability getRotationAbility() {
        return Ability.HammerOfWrath;
    }

    @Override
    public boolean isUsableBefore20PercentHealth() {
        return false;
    }

    @Override
    public float abilityDamage() {
        return (1198f + .15f * stats.getSpellPower() + .15f * stats.getAttackPower())
                * (1f + stats.getHammerOfWrathMultiplier());
    }

    @Override
    public float abilityCritChance() {
        return .25f * talents.getSanctifiedWrath();
    }
}
