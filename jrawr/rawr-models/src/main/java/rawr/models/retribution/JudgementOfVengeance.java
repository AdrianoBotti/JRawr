// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class JudgementOfVengeance extends Skill {

    public JudgementOfVengeance(CombatStats combats, float averageStack) {
        super(combats, AbilityType.Range, DamageType.Holy, true, true);
        averageStackSize = averageStack;
    }

    @Override
    public Ability getRotationAbility() {
        return Ability.Judgement;
    }

    public final float averageStackSize;

    @Override
    public float abilityDamage() {
        return (1.0f + stats.getSpellPower() * 0.22f + stats.getAttackPower() * 0.14f) * (1f + 0.1f * averageStackSize)
                * (1f + .05f * talents.getTheArtOfWar() + .03f * talents.getSealsOfThePure()
                + (talents.isGlyphOfJudgement() ? 0.1f : 0f) + stats.getJudgementMultiplier());
    }

    @Override
    public float abilityCritChance() {
        return talents.getFanaticism() * .06f + stats.getJudgementCrit();
    }
}
