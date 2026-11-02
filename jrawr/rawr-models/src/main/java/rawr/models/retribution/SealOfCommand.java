// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class SealOfCommand extends Skill {

    public SealOfCommand(CombatStats combats) {
        super(combats, AbilityType.Melee, DamageType.Holy, true, false);
    }

    @Override
    public float abilityDamage() {
        return (combats.weaponDamage * .36f) * (1f + stats.getSealMultiplier());
    }

    @Override
    public float targets() {
        return (float) Math.min(3f, calcOpts.getTargets());
    }
}
