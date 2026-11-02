// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Rotation.ECD Table/EffectiveCooldown.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class EffectiveCooldown extends Rotation {

    public EffectiveCooldown(CombatStats combats) {
        super(combats);
    }

    @Override
    public void setCharacterCalculations(CharacterCalculationsRetribution calc) {
        calc.solution = new RotationSolution();

        for (Skill skill : new Skill[] { judge, cs, ds, cons, exo, how }) {
            float effectiveCooldown;
            if (skill.isUsableAfter20PercentHealth()) {
                if (skill.isUsableBefore20PercentHealth())
                    effectiveCooldown =
                            combats.getCalcOpts().getEffectiveAbilityCooldown(skill.getRotationAbility()) *
                                    (1f - combats.getCalcOpts().getTimeUnder20()) +
                                    combats.getCalcOpts().getEffectiveAbilityCooldownAfter20PercentHealth(
                                            skill.getRotationAbility()) *
                                            combats.getCalcOpts().getTimeUnder20();
                else
                    effectiveCooldown = combats.getCalcOpts().getEffectiveAbilityCooldownAfter20PercentHealth(
                            skill.getRotationAbility());
            } else {
                if (skill.isUsableBefore20PercentHealth())
                    effectiveCooldown = combats.getCalcOpts().getEffectiveAbilityCooldown(
                            skill.getRotationAbility());
                else
                    effectiveCooldown = 0;
            }

            calc.solution.setAbilityEffectiveCooldown(skill.getRotationAbility(), effectiveCooldown);
        }

        calc.rotation = null;
    }

    @Override
    public float getAbilityUsagePerSecond(Skill skill) {
        return
                (skill.isUsableBefore20PercentHealth() ?
                        (1 - combats.getCalcOpts().getTimeUnder20())
                                / combats.getCalcOpts().getEffectiveAbilityCooldown(skill.getRotationAbility()) :
                        0) +
                        (skill.isUsableAfter20PercentHealth() ?
                                combats.getCalcOpts().getTimeUnder20()
                                        / combats.getCalcOpts().getEffectiveAbilityCooldownAfter20PercentHealth(
                                                skill.getRotationAbility()) :
                                0);
    }
}
