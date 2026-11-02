// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Rotation.FCFS Simulator/SimulatorParameters.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The C# decimal fields (Wait/Delay/DivineStormCooldown/SpellGCD/SimulationTime) are BigDecimal so
// the * timeUnitsPerSecond truncations in SimulatorEngine see the same exact values as C# decimal.
package rawr.models.retribution;

import rawr.base.util.Utilities;

import java.math.BigDecimal;

public class SimulatorParameters {

    private final boolean t7_4pc;
    private final int impJudgements;
    private final boolean glyphConsecrate;
    private final Ability[] priorities;
    private final BigDecimal wait;
    private final BigDecimal delay;
    private final BigDecimal divineStormCooldown;
    private final BigDecimal spellGcd;
    private final BigDecimal simulationTime;

    public SimulatorParameters(
            Ability[] priorities,
            float wait,
            float delay,
            boolean t7_4pc,
            int impJudgements,
            boolean glyphConsecrate,
            float divineStormCooldown,
            float spellHaste,
            double simulationTime) {
        this.priorities = priorities.clone();
        this.t7_4pc = t7_4pc;
        this.glyphConsecrate = glyphConsecrate;
        this.wait = toDecimal(netRound5(wait));
        this.delay = toDecimal(netRound5(delay));
        this.impJudgements = impJudgements;
        this.divineStormCooldown = toDecimal(netRound5(divineStormCooldown));
        this.spellGcd = toDecimal(netRound5(Math.max(1f, 1.5f / (1 + spellHaste))));
        this.simulationTime = toDecimal(simulationTime);
    }

    /** C# Math.Round(double, 5) on the float-to-double widened argument (banker's rounding). */
    private static double netRound5(double value) {
        return Math.rint(value * 1e5) / 1e5;
    }

    private static BigDecimal toDecimal(double value) {
        return BigDecimal.valueOf(value);
    }

    public boolean isT7_4pc() {
        return t7_4pc;
    }

    public int getImpJudgements() {
        return impJudgements;
    }

    public boolean isGlyphConsecrate() {
        return glyphConsecrate;
    }

    public Ability[] getPriorities() {
        return priorities;
    }

    public BigDecimal getWait() {
        return wait;
    }

    public BigDecimal getDelay() {
        return delay;
    }

    public BigDecimal getDivineStormCooldown() {
        return divineStormCooldown;
    }

    public BigDecimal getSpellGcd() {
        return spellGcd;
    }

    public BigDecimal getSimulationTime() {
        return simulationTime;
    }

    public static String shortAbilityString(Ability ability) {
        if (ability == Ability.Consecration) return "Con";
        if (ability == Ability.CrusaderStrike) return "CS";
        if (ability == Ability.Judgement) return "Jud";
        if (ability == Ability.DivineStorm) return "DS";
        if (ability == Ability.Exorcism) return "Exo";
        if (ability == Ability.HammerOfWrath) return "HoW";
        return "?";
    }

    public static String rotationString(Ability[] rotation) {
        String s = "";
        for (int i = 0; i < rotation.length; i++) {
            s += shortAbilityString(rotation[i]);
            if (i + 1 < rotation.length) s += " > ";
        }
        return s;
    }

    public static String shortRotationString(Ability[] rotation) {
        String s = "";
        for (int i = 0; i < rotation.length; i++) {
            s += shortAbilityString(rotation[i]);
            if (i + 1 < rotation.length) s += "-";
        }
        return s;
    }

    public static String abilityString(Ability ability) {
        if (ability == Ability.Consecration) return "Consecration";
        if (ability == Ability.CrusaderStrike) return "Crusader Strike";
        if (ability == Ability.Judgement) return "Judgement";
        if (ability == Ability.DivineStorm) return "Divine Storm";
        if (ability == Ability.Exorcism) return "Exorcism";
        if (ability == Ability.HammerOfWrath) return "Hammer of Wrath";
        return "?";
    }

    public static Ability[] defaultRotation() {
        return new Ability[] { Ability.CrusaderStrike, Ability.HammerOfWrath, Ability.Judgement,
                Ability.Consecration, Ability.DivineStorm, Ability.Exorcism };
    }

    @Override
    public String toString() {
        String ret = "";
        for (Ability a : priorities) {
            ret += shortAbilityString(a) + ", ";
        }
        return ret;
    }

    @Override
    public boolean equals(Object obj) {
        return equals(obj instanceof SimulatorParameters ? (SimulatorParameters) obj : null);
    }

    public boolean equals(SimulatorParameters other) {
        if (other == null)
            return false;

        // Default comparer is slow for enums
        if (!Utilities.areArraysEqual(priorities, other.priorities))
            return false;

        return (t7_4pc == other.t7_4pc) &&
                (divineStormCooldown.equals(other.divineStormCooldown)) &&
                (glyphConsecrate == other.glyphConsecrate) &&
                (delay.equals(other.delay)) &&
                (wait.equals(other.wait)) &&
                (impJudgements == other.impJudgements) &&
                (spellGcd.equals(other.spellGcd)) &&
                (simulationTime.equals(other.simulationTime));
    }

    @Override
    public int hashCode() {
        return Utilities.getCombinedHashCode(
                t7_4pc,
                impJudgements,
                glyphConsecrate,
                wait,
                delay,
                divineStormCooldown,
                spellGcd,
                Utilities.getArrayHashCode(priorities),
                simulationTime);
    }
}
