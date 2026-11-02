// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class HandOfReckoning extends Skill {

    public HandOfReckoning(CombatStats combats) {
        super(combats, AbilityType.Spell, DamageType.Holy, false, false);
    }

    @Override
    public float abilityDamage() {
        return 1f + stats.getAttackPower() * 0.5f;
    }
}
