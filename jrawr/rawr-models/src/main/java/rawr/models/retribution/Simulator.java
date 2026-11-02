// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Rotation.FCFS Simulator/Simulator.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

import java.util.ArrayList;
import java.util.List;

public class Simulator extends Rotation {

    private static Ability[] removeHammerOfWrathFromRotation(Ability[] rotation) {
        List<Ability> abilities = new ArrayList<Ability>();
        for (Ability ability : rotation) abilities.add(ability);
        abilities.remove(Ability.HammerOfWrath);
        Ability[] result = new Ability[abilities.size()];
        return abilities.toArray(result);
    }

    private static Ability[] removeUnavailableAbilitiesFromRotation(Ability[] rotation, CombatStats combats) {
        Ability[] result = rotation;

        if (combats.getTalents().getCrusaderStrike() == 0) {
            List<Ability> abilities = new ArrayList<Ability>();
            for (Ability ability : result) abilities.add(ability);
            abilities.remove(Ability.CrusaderStrike);
            result = abilities.toArray(new Ability[abilities.size()]);
        }

        if (combats.getTalents().getDivineStorm() == 0) {
            List<Ability> abilities = new ArrayList<Ability>();
            for (Ability ability : result) abilities.add(ability);
            abilities.remove(Ability.DivineStorm);
            result = abilities.toArray(new Ability[abilities.size()]);
        }

        return result;
    }

    /**
     * Calculates the solution with concrete parameters.
     */
    private static RotationSolution getSolution(
            CombatStats combats,
            Ability[] rotation,
            double simulationTime,
            float divineStormCooldown,
            float spellHaste) {
        return SimulatorEngine.simulateRotation(new SimulatorParameters(
                rotation,
                combats.getCalcOpts().getWait(),
                combats.getCalcOpts().getDelay(),
                combats.getStats().getJudgementCDReduction() > 0,
                combats.getTalents().getImprovedJudgements(),
                combats.getTalents().isGlyphOfConsecration(),
                divineStormCooldown,
                spellHaste,
                simulationTime));
    }

    /**
     * Calculates the solution by combining subsolutions with the boss above and under 20% health.
     */
    private static RotationSolution getCombinedSolutionWithUnder20PercentHealth(
            CombatStats combats,
            Ability[] rotation,
            double simulationTime,
            float divineStormCooldown,
            float spellHaste) {
        return RotationSolution.combine(
                () -> getSolution(
                        combats,
                        removeHammerOfWrathFromRotation(rotation),
                        simulationTime,
                        divineStormCooldown,
                        spellHaste),
                1 - combats.getCalcOpts().getTimeUnder20(),
                () -> getSolution(
                        combats,
                        rotation,
                        simulationTime,
                        divineStormCooldown,
                        spellHaste),
                combats.getCalcOpts().getTimeUnder20());
    }

    /**
     * Calculates the solution by combining subsolutions with different Divine Storm cooldowns,
     * if 2 piece T10 bonus is active.
     */
    private static RotationSolution getCombinedSolutionWithDivineStormCooldown(
            CombatStats combats,
            Ability[] rotation,
            double simulationTime,
            float spellHaste,
            float swingTime) {
        final float normalDivineStormCooldown = 10;
        final float cooldownRangeStep = 0.6f;

        if (combats.getStats().getDivineStormRefresh() == 0)
            return getCombinedSolutionWithUnder20PercentHealth(
                    combats,
                    rotation,
                    simulationTime,
                    normalDivineStormCooldown,
                    spellHaste);

        // Calculate solutions for different Divine Storm cooldowns
        // and combine them weighted by their neighbourhood cooldown range probabilities
        final RotationSolution[] result = new RotationSolution[] { null };
        float resultProbability = 0;
        for (
                float cooldownRangeStart = 0;
                cooldownRangeStart < normalDivineStormCooldown;
                cooldownRangeStart += cooldownRangeStep) {
            final float rangeStart = cooldownRangeStart;
            float currentSolutionProbability =
                    getT10DivineStormCooldownProbability(
                            swingTime,
                            rangeStart,
                            Math.min(normalDivineStormCooldown, rangeStart + cooldownRangeStep),
                            combats.getStats().getDivineStormRefresh());
            result[0] = RotationSolution.combine(
                    () -> result[0],
                    resultProbability,
                    () -> getCombinedSolutionWithUnder20PercentHealth(
                            combats,
                            rotation,
                            simulationTime,
                            Math.min(
                                    normalDivineStormCooldown,
                                    rangeStart + rangeStart / 2 + combats.getCalcOpts().getDelay()),
                            spellHaste),
                    currentSolutionProbability);
            resultProbability += currentSolutionProbability;
        }

        // Combine with normal Divine Storm cooldown in cases when T10 doesn't proc
        return RotationSolution.combine(
                () -> result[0],
                resultProbability,
                () -> getCombinedSolutionWithUnder20PercentHealth(
                        combats,
                        rotation,
                        simulationTime,
                        normalDivineStormCooldown,
                        spellHaste),
                1 - resultProbability);
    }

    /**
     * Calculates the solution by combining subsolutions with and without Bloodlust.
     */
    private static RotationSolution getCombinedSolutionWithBloodlust(
            CombatStats combats,
            Ability[] rotation,
            double simulationTime) {
        final float bloodlustDuration = 40f;
        final float secondsPerMinute = 60f;
        final float bloodlustHaste = 0.3f;

        // in seconds
        float fightLengthWithBloodlust = combats.getCalcOpts().getBloodlust() ?
                Math.min(combats.getCalcOpts().getFightLength() * secondsPerMinute, bloodlustDuration) :
                0;
        // in seconds
        float fightLengthWithoutBloodlust =
                Math.max(0, combats.getCalcOpts().getFightLength() * secondsPerMinute - fightLengthWithBloodlust);

        float bloodlustSpellHaste = (1 + combats.getStats().getSpellHaste()) * (1 + bloodlustHaste) - 1;

        float normalSwingTime = combats.baseWeaponSpeed / (1 + combats.getStats().getPhysicalHaste());
        float bloodlustSwingTime = normalSwingTime / (1 + bloodlustHaste);

        return RotationSolution.combine(
                () -> getCombinedSolutionWithDivineStormCooldown(
                        combats,
                        rotation,
                        simulationTime,
                        combats.getStats().getSpellHaste(),
                        normalSwingTime),
                fightLengthWithoutBloodlust,
                () -> getCombinedSolutionWithDivineStormCooldown(
                        combats,
                        rotation,
                        simulationTime,
                        bloodlustSpellHaste,
                        bloodlustSwingTime),
                fightLengthWithBloodlust);
    }

    /**
     * Calculates probability of divine storm cooldown being in the given time range.
     * For cooldown to end early after N swings, (N - 1) swings must not proc and then 1 swing must proc.
     *
     * @param swingSpeed Time between weapon swings
     * @param minCooldown Minimal cooldown wanted
     * @param maxCooldown Maximal cooldown wanted
     * @param procChance Proc chance
     */
    private static float getT10DivineStormCooldownProbability(
            float swingSpeed,
            float minCooldown,
            float maxCooldown,
            float procChance) {
        if (swingSpeed == 0) {
            swingSpeed = 2.0f; // This is to prevent accidental infinite loops
        }

        final float divineStormCooldown = 10;

        for (
                float integerSwingCountTime = 0;
                integerSwingCountTime < divineStormCooldown;
                integerSwingCountTime += swingSpeed)
            if ((minCooldown < (float) integerSwingCountTime) && ((float) integerSwingCountTime < maxCooldown))
                return
                        getT10DivineStormCooldownProbability(
                                swingSpeed,
                                minCooldown,
                                integerSwingCountTime,
                                procChance) +
                                getT10DivineStormCooldownProbability(
                                        swingSpeed,
                                        integerSwingCountTime,
                                        maxCooldown,
                                        procChance);

        int nonProcSwingCount = (int) (minCooldown / swingSpeed);
        return
                getIntegerPower(1 - procChance, nonProcSwingCount) // nonProcSwingCount swings must not proc
                        * procChance                                        // 1 swing must proc
                        * (maxCooldown - minCooldown) / swingSpeed;         // adjust to range size
    }

    private static float getIntegerPower(float x, int power) {
        float result = 1;
        for (int currentPower = 0; currentPower < power; currentPower++)
            result *= x;

        return result;
    }

    public RotationSolution solution;
    public Ability[] rotation;

    public Simulator(CombatStats combats, Ability[] rotation, double simulationTime) {
        super(combats);
        if (rotation == null)
            throw new IllegalArgumentException("rotation");

        Ability[] effectiveRotation = removeUnavailableAbilitiesFromRotation(rotation, combats);

        this.rotation = effectiveRotation;
        this.solution = getCombinedSolutionWithBloodlust(combats, effectiveRotation, simulationTime);
    }

    @Override
    public void setCharacterCalculations(CharacterCalculationsRetribution calc) {
        calc.solution = solution;
        calc.rotation = rotation;
    }

    @Override
    public float getAbilityUsagePerSecond(Skill skill) {
        return solution.getAbilityUsagePerSecond(skill.getRotationAbility());
    }
}
