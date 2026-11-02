// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class DivineStorm extends Skill {

    public DivineStorm(CombatStats combats) {
        super(combats, AbilityType.Melee, DamageType.Physical, true, true);
    }

    @Override
    public Ability getRotationAbility() {
        return Ability.DivineStorm;
    }

    @Override
    public float abilityDamage() {
        return (combats.normalWeaponDamage * 1.1f + stats.getDivineStormDamage())
                * (1f + .05f * talents.getTheArtOfWar() + stats.getDivineStormMultiplier());
    }

    @Override
    public float abilityCritChance() {
        return stats.getDivineStormCrit();
    }

    @Override
    public float targets() {
        return (float) Math.min(calcOpts.getTargets(), 3f);
    }
}
