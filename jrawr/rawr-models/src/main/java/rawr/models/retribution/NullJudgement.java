// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class NullJudgement extends Skill {

    public NullJudgement(CombatStats combats) {
        super(combats, AbilityType.Melee, DamageType.Holy, true, false);
    }

    @Override
    public Ability getRotationAbility() {
        return Ability.Judgement;
    }

    @Override
    public boolean isUsableBefore20PercentHealth() {
        return false;
    }

    @Override
    public boolean isUsableAfter20PercentHealth() {
        return false;
    }

    @Override
    public float abilityDamage() {
        return 0;
    }
}
