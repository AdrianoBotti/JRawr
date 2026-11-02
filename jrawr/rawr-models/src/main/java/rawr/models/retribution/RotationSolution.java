// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/RotationSolution.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

import rawr.base.util.Utilities;

public class RotationSolution {

    // C# delegate RotationSolutionSource.
    public interface RotationSolutionSource {
        RotationSolution get();
    }

    public static RotationSolution combine(
            RotationSolution solution1,
            float solution1Weight,
            RotationSolution solution2,
            float solution2Weight) {
        RotationSolution result = new RotationSolution();
        for (int ability = 0; ability <= Ability.values().length - 1; ability++) {
            result.abilityUsagesPerSecond[ability] = Utilities.getWeightedSum(
                    solution1.abilityUsagesPerSecond[ability],
                    solution1Weight,
                    solution2.abilityUsagesPerSecond[ability],
                    solution2Weight);

            // Zero CD means the ability is not in rotation
            if (solution1.abilityEffectiveCooldowns[ability] == 0)
                result.abilityEffectiveCooldowns[ability] = solution2.abilityEffectiveCooldowns[ability];
            else if (solution2.abilityEffectiveCooldowns[ability] == 0)
                result.abilityEffectiveCooldowns[ability] = solution1.abilityEffectiveCooldowns[ability];
            else
                result.abilityEffectiveCooldowns[ability] = 1 / Utilities.getWeightedSum(
                        1 / solution1.abilityEffectiveCooldowns[ability],
                        solution1Weight,
                        1 / solution2.abilityEffectiveCooldowns[ability],
                        solution2Weight);
        }

        return result;
    }

    public static RotationSolution combine(
            RotationSolutionSource solution1,
            float solution1Weight,
            RotationSolutionSource solution2,
            float solution2Weight) {
        if (solution1Weight == 0) {
            if (solution2Weight == 0)
                return new RotationSolution();

            return solution2.get();
        }

        if (solution2Weight == 0)
            return solution1.get();

        return combine(solution1.get(), solution1Weight, solution2.get(), solution2Weight);
    }

    private final float[] abilityUsagesPerSecond = new float[Ability.values().length];
    private final float[] abilityEffectiveCooldowns = new float[Ability.values().length];

    public float getAbilityUsagePerSecond(Ability ability) {
        return abilityUsagesPerSecond[ability.ordinal()];
    }

    public void setAbilityUsagePerSecond(Ability ability, float usagePerSecond) {
        abilityUsagesPerSecond[ability.ordinal()] = usagePerSecond;
    }

    public float getAbilityEffectiveCooldown(Ability ability) {
        return abilityEffectiveCooldowns[ability.ordinal()];
    }

    public void setAbilityEffectiveCooldown(Ability ability, float effectiveCooldown) {
        abilityEffectiveCooldowns[ability.ordinal()] = effectiveCooldown;
    }
}
