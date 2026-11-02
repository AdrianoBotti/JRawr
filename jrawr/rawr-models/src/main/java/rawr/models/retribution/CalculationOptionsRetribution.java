// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/CalculationOptionsRetribution.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port); getXml writes the
// XmlSerializer element order by hand (same approach as CalculationOptionsProtWarr/Healadin).
package rawr.models.retribution;

import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.util.NetFormat;

import java.util.ArrayList;
import java.util.List;

public class CalculationOptionsRetribution implements ICalculationOptionBase {

    // Tab - Fight Parameters
    private float fightLength;
    private MobType mob;
    private SealOf seal;
    private int targetLevel;
    private float timeUnder20;
    private int stackTrinketReset;
    private float targets;
    private float inFront;
    private float consEff;
    private boolean bloodlust;
    private float hoREff;
    private float targetSwitches;

    // Tab - Rotation
    private boolean simulateRotation;
    // Tab - Rotation - FCFS
    private List<Ability[]> rotations;
    private float delay;
    private float wait;

    // Tab - Rotation - Effective CD's
    private float[] effectiveAbilityCooldowns = new float[Ability.values().length];
    private float[] effectiveAbilityCooldownsAfter20PercentHealth = new float[Ability.values().length];

    // Tab - Misc
    private String experimental;

    public CalculationOptionsRetribution() {
        // Tab - Fight Parameters
        fightLength = 5f;
        mob = MobType.Humanoid;
        seal = SealOf.Vengeance;
        targetLevel = 83;
        timeUnder20 = .18f;
        stackTrinketReset = 0;
        targets = 1f;
        inFront = 0f;
        consEff = 1f;
        bloodlust = true;
        hoREff = 0f;
        targetSwitches = 0f;

        // Tab - Rotation
        simulateRotation = true;
        // Tab - Rotation - FCFS
        rotations = new ArrayList<Ability[]>();
        delay = .05f;
        wait = .05f;
        // Tab - Rotation - Effective CD's
        setEffectiveAbilityCooldown(Ability.Judgement, 7.1f);
        setEffectiveAbilityCooldown(Ability.CrusaderStrike, 7.1f);
        setEffectiveAbilityCooldown(Ability.DivineStorm, 10.5f);
        setEffectiveAbilityCooldown(Ability.Consecration, 10.5f);
        setEffectiveAbilityCooldown(Ability.Exorcism, 18f);

        setEffectiveAbilityCooldownAfter20PercentHealth(Ability.Judgement, 7.1f);
        setEffectiveAbilityCooldownAfter20PercentHealth(Ability.CrusaderStrike, 7.1f);
        setEffectiveAbilityCooldownAfter20PercentHealth(Ability.DivineStorm, 12.5f);
        setEffectiveAbilityCooldownAfter20PercentHealth(Ability.Consecration, 12.5f);
        setEffectiveAbilityCooldownAfter20PercentHealth(Ability.Exorcism, 25f);
        setEffectiveAbilityCooldownAfter20PercentHealth(Ability.HammerOfWrath, 6.4f);

        // Tab - Misc
        experimental = "";
    }

    // C# CalculationOptionsRetribution.Clone.
    public CalculationOptionsRetribution clone() {
        CalculationOptionsRetribution clone = new CalculationOptionsRetribution();

        // Tab - Fight Parameters
        clone.fightLength = fightLength;
        clone.mob = mob;
        clone.seal = seal;
        clone.targetLevel = targetLevel;
        clone.timeUnder20 = timeUnder20;
        clone.stackTrinketReset = stackTrinketReset;
        clone.targets = targets;
        clone.inFront = inFront;
        clone.consEff = consEff;
        clone.bloodlust = bloodlust;
        clone.hoREff = hoREff;
        clone.targetSwitches = targetSwitches;

        // Tab - Rotation
        clone.simulateRotation = simulateRotation;

        // Tab - Rotation - FCFS
        clone.rotations = new ArrayList<Ability[]>();
        for (Ability[] rotation : rotations) {
            clone.rotations.add(rotation.clone());
        }

        clone.delay = delay;
        clone.wait = wait;
        // Tab - Rotation - Effective CD's
        clone.effectiveAbilityCooldowns = effectiveAbilityCooldowns.clone();
        clone.effectiveAbilityCooldownsAfter20PercentHealth = effectiveAbilityCooldownsAfter20PercentHealth.clone();

        // Tab - Misc
        clone.experimental = experimental;

        return clone;
    }

    public float getFightLength() {
        return fightLength;
    }

    public void setFightLength(float value) {
        fightLength = value;
    }

    public MobType getMob() {
        return mob;
    }

    public void setMob(MobType value) {
        mob = value;
    }

    public SealOf getSeal() {
        return seal;
    }

    public void setSeal(SealOf value) {
        seal = value;
    }

    public int getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(int value) {
        targetLevel = value;
    }

    public float getTimeUnder20() {
        return timeUnder20;
    }

    public void setTimeUnder20(float value) {
        timeUnder20 = value;
    }

    public int getStackTrinketReset() {
        return stackTrinketReset;
    }

    public void setStackTrinketReset(int value) {
        stackTrinketReset = value;
    }

    public float getTargets() {
        return targets;
    }

    public void setTargets(float value) {
        targets = value;
    }

    public float getInFront() {
        return inFront;
    }

    public void setInFront(float value) {
        inFront = value;
    }

    public float getConsEff() {
        return consEff;
    }

    public void setConsEff(float value) {
        consEff = value;
    }

    public boolean getBloodlust() {
        return bloodlust;
    }

    public void setBloodlust(boolean value) {
        bloodlust = value;
    }

    public float getHoREff() {
        return hoREff;
    }

    public void setHoREff(float value) {
        hoREff = value;
    }

    public float getTargetSwitches() {
        return targetSwitches;
    }

    public void setTargetSwitches(float value) {
        targetSwitches = value;
    }

    public boolean getSimulateRotation() {
        return simulateRotation;
    }

    public void setSimulateRotation(boolean value) {
        simulateRotation = value;
    }

    public List<Ability[]> getRotations() {
        return rotations;
    }

    public void setRotations(List<Ability[]> value) {
        rotations = value;
    }

    public float getDelay() {
        return delay;
    }

    public void setDelay(float value) {
        delay = value;
    }

    public float getWait() {
        return wait;
    }

    public void setWait(float value) {
        wait = value;
    }

    // [XmlIgnore] in C#: EffectiveCD = !SimulateRotation.
    public boolean getEffectiveCD() {
        return !simulateRotation;
    }

    public void setEffectiveCD(boolean value) {
        simulateRotation = !value;
    }

    public float getJudgeCD() {
        return getEffectiveAbilityCooldown(Ability.Judgement);
    }

    public void setJudgeCD(float value) {
        setEffectiveAbilityCooldown(Ability.Judgement, value);
    }

    public float getCSCD() {
        return getEffectiveAbilityCooldown(Ability.CrusaderStrike);
    }

    public void setCSCD(float value) {
        setEffectiveAbilityCooldown(Ability.CrusaderStrike, value);
    }

    public float getDSCD() {
        return getEffectiveAbilityCooldown(Ability.DivineStorm);
    }

    public void setDSCD(float value) {
        setEffectiveAbilityCooldown(Ability.DivineStorm, value);
    }

    public float getConsCD() {
        return getEffectiveAbilityCooldown(Ability.Consecration);
    }

    public void setConsCD(float value) {
        setEffectiveAbilityCooldown(Ability.Consecration, value);
    }

    public float getExoCD() {
        return getEffectiveAbilityCooldown(Ability.Exorcism);
    }

    public void setExoCD(float value) {
        setEffectiveAbilityCooldown(Ability.Exorcism, value);
    }

    public float getJudgeCD20() {
        return getEffectiveAbilityCooldownAfter20PercentHealth(Ability.Judgement);
    }

    public void setJudgeCD20(float value) {
        setEffectiveAbilityCooldownAfter20PercentHealth(Ability.Judgement, value);
    }

    public float getCSCD20() {
        return getEffectiveAbilityCooldownAfter20PercentHealth(Ability.CrusaderStrike);
    }

    public void setCSCD20(float value) {
        setEffectiveAbilityCooldownAfter20PercentHealth(Ability.CrusaderStrike, value);
    }

    public float getDSCD20() {
        return getEffectiveAbilityCooldownAfter20PercentHealth(Ability.DivineStorm);
    }

    public void setDSCD20(float value) {
        setEffectiveAbilityCooldownAfter20PercentHealth(Ability.DivineStorm, value);
    }

    public float getConsCD20() {
        return getEffectiveAbilityCooldownAfter20PercentHealth(Ability.Consecration);
    }

    public void setConsCD20(float value) {
        setEffectiveAbilityCooldownAfter20PercentHealth(Ability.Consecration, value);
    }

    public float getExoCD20() {
        return getEffectiveAbilityCooldownAfter20PercentHealth(Ability.Exorcism);
    }

    public void setExoCD20(float value) {
        setEffectiveAbilityCooldownAfter20PercentHealth(Ability.Exorcism, value);
    }

    public float getHoWCD20() {
        return getEffectiveAbilityCooldownAfter20PercentHealth(Ability.HammerOfWrath);
    }

    public void setHoWCD20(float value) {
        setEffectiveAbilityCooldownAfter20PercentHealth(Ability.HammerOfWrath, value);
    }

    public String getExperimental() {
        return experimental;
    }

    public void setExperimental(String value) {
        experimental = value;
    }

    public float getEffectiveAbilityCooldown(Ability ability) {
        return effectiveAbilityCooldowns[ability.ordinal()];
    }

    public float getEffectiveAbilityCooldownAfter20PercentHealth(Ability ability) {
        return effectiveAbilityCooldownsAfter20PercentHealth[ability.ordinal()];
    }

    private void setEffectiveAbilityCooldown(Ability ability, float effectiveCooldown) {
        effectiveAbilityCooldowns[ability.ordinal()] = effectiveCooldown;
    }

    private void setEffectiveAbilityCooldownAfter20PercentHealth(Ability ability, float effectiveCooldown) {
        effectiveAbilityCooldownsAfter20PercentHealth[ability.ordinal()] = effectiveCooldown;
    }

    // C# CalculationOptionsRetribution.GetXml (XmlSerializer on the public properties, declaration order).
    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsRetribution xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<FightLength>").append(NetFormat.floatR(fightLength)).append("</FightLength>");
        sb.append("<Mob>").append(mob.name()).append("</Mob>");
        sb.append("<Seal>").append(seal.name()).append("</Seal>");
        sb.append("<TargetLevel>").append(targetLevel).append("</TargetLevel>");
        sb.append("<TimeUnder20>").append(NetFormat.floatR(timeUnder20)).append("</TimeUnder20>");
        sb.append("<StackTrinketReset>").append(stackTrinketReset).append("</StackTrinketReset>");
        sb.append("<Targets>").append(NetFormat.floatR(targets)).append("</Targets>");
        sb.append("<InFront>").append(NetFormat.floatR(inFront)).append("</InFront>");
        sb.append("<ConsEff>").append(NetFormat.floatR(consEff)).append("</ConsEff>");
        sb.append("<Bloodlust>").append(bloodlust ? "true" : "false").append("</Bloodlust>");
        sb.append("<HoREff>").append(NetFormat.floatR(hoREff)).append("</HoREff>");
        sb.append("<TargetSwitches>").append(NetFormat.floatR(targetSwitches)).append("</TargetSwitches>");
        sb.append("<SimulateRotation>").append(simulateRotation ? "true" : "false").append("</SimulateRotation>");
        if (rotations.isEmpty()) {
            sb.append("<Rotations />");
        } else {
            sb.append("<Rotations>");
            for (Ability[] rotation : rotations) {
                sb.append("<ArrayOfAbility>");
                for (Ability ability : rotation) {
                    sb.append("<Ability>").append(ability.name()).append("</Ability>");
                }
                sb.append("</ArrayOfAbility>");
            }
            sb.append("</Rotations>");
        }
        sb.append("<Delay>").append(NetFormat.floatR(delay)).append("</Delay>");
        sb.append("<Wait>").append(NetFormat.floatR(wait)).append("</Wait>");
        sb.append("<JudgeCD>").append(NetFormat.floatR(getJudgeCD())).append("</JudgeCD>");
        sb.append("<CSCD>").append(NetFormat.floatR(getCSCD())).append("</CSCD>");
        sb.append("<DSCD>").append(NetFormat.floatR(getDSCD())).append("</DSCD>");
        sb.append("<ConsCD>").append(NetFormat.floatR(getConsCD())).append("</ConsCD>");
        sb.append("<ExoCD>").append(NetFormat.floatR(getExoCD())).append("</ExoCD>");
        sb.append("<JudgeCD20>").append(NetFormat.floatR(getJudgeCD20())).append("</JudgeCD20>");
        sb.append("<CSCD20>").append(NetFormat.floatR(getCSCD20())).append("</CSCD20>");
        sb.append("<DSCD20>").append(NetFormat.floatR(getDSCD20())).append("</DSCD20>");
        sb.append("<ConsCD20>").append(NetFormat.floatR(getConsCD20())).append("</ConsCD20>");
        sb.append("<ExoCD20>").append(NetFormat.floatR(getExoCD20())).append("</ExoCD20>");
        sb.append("<HoWCD20>").append(NetFormat.floatR(getHoWCD20())).append("</HoWCD20>");
        sb.append("<Experimental />");
        sb.append("</CalculationOptionsRetribution>");
        return sb.toString();
    }
}
