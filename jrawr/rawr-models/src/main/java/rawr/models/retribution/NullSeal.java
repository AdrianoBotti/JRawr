// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class NullSeal extends Skill {

    public NullSeal(CombatStats combats) {
        super(combats, AbilityType.Melee, DamageType.Holy, true, false);
    }

    @Override
    public float abilityDamage() {
        return 0;
    }
}
