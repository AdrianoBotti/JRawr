// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class MagicDamage extends Skill {

    private final float amount;

    public MagicDamage(CombatStats combats, float amount) {
        super(combats, AbilityType.Spell, DamageType.Magic, false, false);
        this.amount = amount;
    }

    @Override
    public float abilityDamage() {
        return amount;
    }
}
