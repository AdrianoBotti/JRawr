// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class JudgementOfCommand extends Skill {

    public JudgementOfCommand(CombatStats combats) {
        super(combats, AbilityType.Range, DamageType.Holy, true, true);
    }

    @Override
    public Ability getRotationAbility() {
        return Ability.Judgement;
    }

    @Override
    public float abilityDamage() {
        return (combats.weaponDamage * .19f + stats.getSpellPower() * .13f + stats.getAttackPower() * .08f)
                * (1f + .05f * talents.getTheArtOfWar() + (talents.isGlyphOfJudgement() ? 0.1f : 0f + stats.getJudgementMultiplier()));
    }

    @Override
    public float abilityCritChance() {
        return talents.getFanaticism() * .06f + stats.getJudgementCrit();
    }
}
