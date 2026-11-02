// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Rotation.FCFS Simulator/SimulatorEngine.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

import rawr.base.algorithms.CalculationCache;

import java.math.BigDecimal;

class SimulatorEngine {

    private static final CalculationCache<SimulatorParameters, RotationSolution> solutionCache =
            new CalculationCache<SimulatorParameters, RotationSolution>(SimulatorEngine::simulateRotationCore);

    public static RotationSolution simulateRotation(SimulatorParameters rot) {
        return solutionCache.getResult(rot);
    }

    private static RotationSolution simulateRotationCore(SimulatorParameters rot) {
        final int timeUnitsPerSecond = 100000;
        final int meleeAbilityGcd = (int) (1.5 * timeUnitsPerSecond);

        int fightLength = rot.getSimulationTime().multiply(BigDecimal.valueOf(timeUnitsPerSecond)).intValue();
        int spellGcd = rot.getSpellGcd().multiply(BigDecimal.valueOf(timeUnitsPerSecond)).intValue();
        SimulatorAbility.delay = rot.getDelay().multiply(BigDecimal.valueOf(timeUnitsPerSecond)).intValue();
        SimulatorAbility.wait = rot.getWait().multiply(BigDecimal.valueOf(timeUnitsPerSecond)).intValue();

        SimulatorAbility[] abilities = new SimulatorAbility[Ability.values().length];

        abilities[Ability.Judgement.ordinal()] = new SimulatorAbility(
                (10 - rot.getImpJudgements() - (rot.isT7_4pc() ? 1 : 0)) * timeUnitsPerSecond,
                meleeAbilityGcd);
        abilities[Ability.CrusaderStrike.ordinal()] = new SimulatorAbility(
                4 * timeUnitsPerSecond,
                meleeAbilityGcd);
        abilities[Ability.DivineStorm.ordinal()] = new SimulatorAbility(
                rot.getDivineStormCooldown().multiply(BigDecimal.valueOf(timeUnitsPerSecond)).intValue(),
                meleeAbilityGcd);
        abilities[Ability.Consecration.ordinal()] = new SimulatorAbility(
                (rot.isGlyphConsecrate() ? 10 : 8) * timeUnitsPerSecond,
                spellGcd);
        abilities[Ability.Exorcism.ordinal()] = new SimulatorAbility(
                15 * timeUnitsPerSecond,
                spellGcd);
        abilities[Ability.HammerOfWrath.ordinal()] = new SimulatorAbility(
                6 * timeUnitsPerSecond,
                meleeAbilityGcd);

        int gcdFinishTime = 0;
        int currentTime = 0;
        while (currentTime < fightLength) {
            if (currentTime >= gcdFinishTime) {
                for (Ability ability : rot.getPriorities()) {
                    if (abilities[ability.ordinal()].shouldAbilityBeUsedNext(currentTime)) {
                        if (abilities[ability.ordinal()].canAbilityBeUsedNow(currentTime))
                            gcdFinishTime = abilities[ability.ordinal()].useAbility(currentTime);

                        break;
                    }
                }
            }

            int nextTime = fightLength;
            if (currentTime >= gcdFinishTime) {
                for (Ability ability : rot.getPriorities()) {
                    int nextUseTime = abilities[ability.ordinal()].getNextUseTime(currentTime);
                    if (nextUseTime > currentTime)
                        nextTime = Math.min(nextTime, nextUseTime);
                }
            } else {
                nextTime = Math.min(nextTime, gcdFinishTime);
            }

            currentTime = nextTime;
        }

        float fightLengthInSeconds = ((float) fightLength) / timeUnitsPerSecond;
        RotationSolution solution = new RotationSolution();
        for (Ability ability : rot.getPriorities()) {
            solution.setAbilityUsagePerSecond(
                    ability,
                    abilities[ability.ordinal()].uses / fightLengthInSeconds);
            solution.setAbilityEffectiveCooldown(
                    ability,
                    abilities[ability.ordinal()].effectiveCooldown() / timeUnitsPerSecond);
        }

        return solution;
    }
}
