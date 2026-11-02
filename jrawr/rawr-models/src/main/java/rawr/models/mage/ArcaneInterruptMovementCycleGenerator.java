// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class ArcaneInterruptMovementCycleGenerator extends CycleGenerator {
    private static class State extends CycleState {
        public boolean missileBarrageRegistered;
        public float missileBarrageDuration;
        public float arcaneBarrageCooldown;
        public int arcaneBlastStack;
        public float arcaneBlastDuration;
        public float movementDuration;
    }

    public Spell AB0, AB1, AB2, AB3, AB4, ABar0, ABar1, ABar2, ABar3, ABar4, AM0, AM1, AM2, AM3, AM4, MBAM0, MBAM1, MBAM2, MBAM3, MBAM4;
    public Spell[][] AMT = new Spell[5][5];
    public Spell[][] MBAMT = new Spell[5][5];
    public Spell ABNull, FB;

    private float ABMB;
    private float MB;
    private float T8;
    private int maxStack;

    private float movementEventRate;
    private float movementDuration;
    private CastingState castingState;

    private float ABI;
    private float ABI2;
    private float ABI0;
    private float aBarI;
    private float aBarI0;
    private float[] AMI = new float[6];
    private float[] MBAMI = new float[6];
    private float AMI0, MBAMI0;

    public ArcaneInterruptMovementCycleGenerator(CastingState castingState, float movementEventRate, float movementDuration) {
        this.castingState = castingState;

        AB0 = castingState.getSpell(SpellId.ArcaneBlast0);
        AB1 = castingState.getSpell(SpellId.ArcaneBlast1);
        AB2 = castingState.getSpell(SpellId.ArcaneBlast2);
        AB3 = castingState.getSpell(SpellId.ArcaneBlast3);
        AB4 = castingState.getSpell(SpellId.ArcaneBlast4);
        ABar0 = castingState.getSpell(SpellId.ArcaneBarrage);
        ABar1 = castingState.getSpell(SpellId.ArcaneBarrage1);
        ABar2 = castingState.getSpell(SpellId.ArcaneBarrage2);
        ABar3 = castingState.getSpell(SpellId.ArcaneBarrage3);
        ABar4 = castingState.getSpell(SpellId.ArcaneBarrage4);
        AM0 = castingState.getSpell(SpellId.ArcaneMissiles);
        AM1 = castingState.getSpell(SpellId.ArcaneMissiles1);
        AM2 = castingState.getSpell(SpellId.ArcaneMissiles2);
        AM3 = castingState.getSpell(SpellId.ArcaneMissiles3);
        AM4 = castingState.getSpell(SpellId.ArcaneMissiles4);
        MBAM0 = castingState.getSpell(SpellId.ArcaneMissilesMB);
        MBAM1 = castingState.getSpell(SpellId.ArcaneMissilesMB1);
        MBAM2 = castingState.getSpell(SpellId.ArcaneMissilesMB2);
        MBAM3 = castingState.getSpell(SpellId.ArcaneMissilesMB3);
        MBAM4 = castingState.getSpell(SpellId.ArcaneMissilesMB4);

        for (int ab = 0; ab < 5; ab++) {
            for (int t = 0; t < 5; t++) {
                AMT[ab][t] = castingState.getSolver().getArcaneMissilesTemplate().getSpell(castingState, false, ab, t);
                MBAMT[ab][t] = castingState.getSolver().getArcaneMissilesTemplate().getSpell(castingState, true, ab, t);
            }
        }
        ABNull = new Spell(castingState.getSolver().getArcaneBlastTemplate());
        FB = castingState.getSpell(SpellId.FireBlast);

        this.movementEventRate = movementEventRate;
        this.movementDuration = movementDuration;

        ABI = (float) (1.0 - Math.exp(-movementEventRate * (AB0.castTime - castingState.calculationOptions.getLatencyCast())));
        ABI2 = (float) ((1 - ABI) * (1.0 - Math.exp(-movementEventRate * castingState.calculationOptions.getLatencyCast())));
        ABI0 = (float) Math.exp(-movementEventRate * AB0.castTime);

        aBarI = (float) (1.0 - Math.exp(-movementEventRate * ABar0.castTime));
        aBarI0 = (float) Math.exp(-movementEventRate * ABar0.castTime);

        AMI[0] = (float) (1.0 - Math.exp(-movementEventRate * (AM0.castTime - castingState.calculationOptions.getLatencyChannel()) / 5.0));
        MBAMI[0] = (float) (1.0 - Math.exp(-movementEventRate * (MBAM0.castTime - castingState.calculationOptions.getLatencyChannel()) / 5.0));
        for (int t = 1; t < 5; t++) {
            AMI[t] = (float) (Math.exp(-movementEventRate * (AM0.castTime - castingState.calculationOptions.getLatencyChannel()) * t / 5.0) * (1.0 - Math.exp(-movementEventRate * (AM0.castTime - castingState.calculationOptions.getLatencyChannel()) / 5.0)));
            MBAMI[t] = (float) (Math.exp(-movementEventRate * (MBAM0.castTime - castingState.calculationOptions.getLatencyChannel()) * t / 5.0) * (1.0 - Math.exp(-movementEventRate * (MBAM0.castTime - castingState.calculationOptions.getLatencyChannel()) / 5.0)));
        }
        AMI[5] = (float) (Math.exp(-movementEventRate * (AM0.castTime - castingState.calculationOptions.getLatencyChannel())) * (1.0 - Math.exp(-movementEventRate * castingState.calculationOptions.getLatencyChannel())));
        MBAMI[5] = (float) (Math.exp(-movementEventRate * (AM0.castTime - castingState.calculationOptions.getLatencyChannel())) * (1.0 - Math.exp(-movementEventRate * castingState.calculationOptions.getLatencyChannel())));
        AMI0 = (float) Math.exp(-movementEventRate * AM0.castTime);
        MBAMI0 = (float) Math.exp(-movementEventRate * MBAM0.castTime);

        ABMB = 0.08f * castingState.mageTalents.getMissileBarrage();
        maxStack = 4;
        MB = 0.04f * castingState.mageTalents.getMissileBarrage();
        T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();

        generateStateDescription();
    }

    @Override
    protected CycleState getInitialState() {
        return getState(false, 0.0f, 0.0f, 0, 0.0f, 0.0f);
    }

    @Override
    protected List<CycleControlledStateTransition> getStateTransitions(CycleState state) {
        State s = (State) state;
        List<CycleControlledStateTransition> list = new ArrayList<CycleControlledStateTransition>();
        Spell AB = null;
        Spell AM = null;
        Spell ABar = null;
        switch (s.arcaneBlastStack) {
            case 0:
                AB = AB0;
                ABar = ABar0;
                AM = (s.missileBarrageDuration > 0) ? MBAM0 : AM0;
                break;
            case 1:
                AB = AB1;
                ABar = ABar1;
                AM = (s.missileBarrageDuration > 0) ? MBAM1 : AM1;
                break;
            case 2:
                AB = AB2;
                ABar = ABar2;
                AM = (s.missileBarrageDuration > 0) ? MBAM2 : AM2;
                break;
            case 3:
                AB = AB3;
                ABar = ABar3;
                AM = (s.missileBarrageDuration > 0) ? MBAM3 : AM3;
                break;
            case 4:
                AB = AB4;
                ABar = ABar4;
                AM = (s.missileBarrageDuration > 0) ? MBAM4 : AM4;
                break;
        }
        if (s.movementDuration == 0.0f) {
            float d = (AB.castTime - castingState.calculationOptions.getLatencyCast()) / 2.0f;
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = ABNull;
            transition.pause = d;
            transition.targetState = getState(
                    s.missileBarrageDuration > d,
                    Math.max(0.0f, s.missileBarrageDuration - d),
                    Math.max(0.0f, s.arcaneBarrageCooldown - d),
                    s.arcaneBlastDuration - d > 0 ? s.arcaneBlastStack : 0,
                    Math.max(s.arcaneBlastDuration - d, 0.0f),
                    movementDuration
            );
            transition.setTransitionProbability(ABI);
            list.add(transition);
            d = AB.castTime - castingState.calculationOptions.getLatencyCast() / 2.0f;
            if (MB > 0) {
                transition = new CycleControlledStateTransition();
                transition.spell = AB;
                transition.pause = -castingState.calculationOptions.getLatencyCast() / 2.0f;
                transition.targetState = getState(
                        s.missileBarrageDuration > AB.castTime,
                        15.0f,
                        Math.max(0.0f, s.arcaneBarrageCooldown - AB.castTime),
                        Math.min(maxStack, s.arcaneBlastStack + 1),
                        6.0f,
                        movementDuration
                );
                transition.setTransitionProbability(ABI2 * ABMB);
                list.add(transition);
                transition = new CycleControlledStateTransition();
                transition.spell = AB;
                transition.targetState = getState(
                        s.missileBarrageDuration > AB.castTime,
                        15.0f,
                        Math.max(0.0f, s.arcaneBarrageCooldown - AB.castTime),
                        Math.min(maxStack, s.arcaneBlastStack + 1),
                        6.0f,
                        0.0f
                );
                transition.setTransitionProbability(ABI0 * ABMB);
                list.add(transition);
            }
            transition = new CycleControlledStateTransition();
            transition.spell = AB;
            transition.pause = -castingState.calculationOptions.getLatencyCast() / 2.0f;
            transition.targetState = getState(
                    s.missileBarrageDuration > AB.castTime,
                    Math.max(0.0f, s.missileBarrageDuration - AB.castTime),
                    Math.max(0.0f, s.arcaneBarrageCooldown - AB.castTime),
                    Math.min(maxStack, s.arcaneBlastStack + 1),
                    6.0f,
                    movementDuration
            );
            transition.setTransitionProbability(ABI2 * (1.0f - ABMB));
            list.add(transition);
            transition = new CycleControlledStateTransition();
            transition.spell = AB;
            transition.targetState = getState(
                    s.missileBarrageDuration > AB.castTime,
                    Math.max(0.0f, s.missileBarrageDuration - AB.castTime),
                    Math.max(0.0f, s.arcaneBarrageCooldown - AB.castTime),
                    Math.min(maxStack, s.arcaneBlastStack + 1),
                    6.0f,
                    0.0f
            );
            transition.setTransitionProbability(ABI0 * (1.0f - ABMB));
            list.add(transition);
            //if (s.ArcaneBarrageCooldown == 0)
            {
                float p = (float) (1.0 - Math.exp(-movementEventRate * (ABar0.castTime + s.arcaneBarrageCooldown)));
                if (MB > 0) {
                    transition = new CycleControlledStateTransition();
                    transition.spell = ABar;
                    transition.pause = s.arcaneBarrageCooldown;
                    transition.targetState = getState(
                            true,
                            15.0f - ABar.castTime,
                            3.0f - ABar.castTime,
                            0,
                            0.0f,
                            movementDuration - (ABar.castTime + s.arcaneBarrageCooldown) / 2.0f
                    );
                    transition.setTransitionProbability(p * MB);
                    list.add(transition);
                    transition = new CycleControlledStateTransition();
                    transition.spell = ABar;
                    transition.pause = s.arcaneBarrageCooldown;
                    transition.targetState = getState(
                            true,
                            15.0f - ABar.castTime,
                            3.0f - ABar.castTime,
                            0,
                            0.0f,
                            0.0f
                    );
                    transition.setTransitionProbability((1 - p) * MB);
                    list.add(transition);
                }
                transition = new CycleControlledStateTransition();
                transition.spell = ABar;
                transition.pause = s.arcaneBarrageCooldown;
                transition.targetState = getState(
                        s.missileBarrageDuration > ABar.castTime,
                        Math.max(0.0f, s.missileBarrageDuration - ABar.castTime),
                        3.0f - ABar.castTime,
                        0,
                        0.0f,
                        movementDuration - (ABar.castTime + s.arcaneBarrageCooldown) / 2.0f
                );
                transition.setTransitionProbability(p * (1.0f - MB));
                list.add(transition);
                transition = new CycleControlledStateTransition();
                transition.spell = ABar;
                transition.pause = s.arcaneBarrageCooldown;
                transition.targetState = getState(
                        s.missileBarrageDuration > ABar.castTime,
                        Math.max(0.0f, s.missileBarrageDuration - ABar.castTime),
                        3.0f - ABar.castTime,
                        0,
                        0.0f,
                        0.0f
                );
                transition.setTransitionProbability((1 - p) * (1.0f - MB));
                list.add(transition);
            }
            float[] I = (s.missileBarrageDuration > 0) ? MBAMI : AMI;
            Spell[][] T = (s.missileBarrageDuration > 0) ? MBAMT : AMT;
            for (int t = 0; t < 5; t++) {
                d = (t + 0.5f) * (AM.castTime - castingState.calculationOptions.getLatencyChannel()) / 5.0f;
                if (s.missileBarrageDuration > 0 && T8 > 0) {
                    transition = new CycleControlledStateTransition();
                    transition.spell = T[s.arcaneBlastStack][t];
                    transition.pause = -castingState.calculationOptions.getLatencyChannel() + (AM.castTime - castingState.calculationOptions.getLatencyChannel()) / 10.0f;
                    transition.targetState = getState(
                            s.missileBarrageDuration > d,
                            Math.max(0.0f, s.missileBarrageDuration - d),
                            Math.max(0.0f, s.arcaneBarrageCooldown - d),
                            0,
                            0.0f,
                            movementDuration
                    );
                    transition.setTransitionProbability(I[t] * T8);
                    list.add(transition);
                }
                transition = new CycleControlledStateTransition();
                transition.spell = T[s.arcaneBlastStack][t];
                transition.pause = -castingState.calculationOptions.getLatencyChannel() + (AM.castTime - castingState.calculationOptions.getLatencyChannel()) / 10.0f;
                transition.targetState = getState(
                        false,
                        0.0f,
                        Math.max(0.0f, s.arcaneBarrageCooldown - d),
                        0,
                        0.0f,
                        movementDuration
                );
                transition.setTransitionProbability(I[t] * (s.missileBarrageDuration > 0 ? 1.0f - T8 : 1.0f));
                list.add(transition);
            }
            d = AM.castTime - castingState.calculationOptions.getLatencyChannel() / 2.0f;
            if (s.missileBarrageDuration > 0 && T8 > 0) {
                transition = new CycleControlledStateTransition();
                transition.spell = AM;
                transition.pause = -castingState.calculationOptions.getLatencyChannel() / 2;
                transition.targetState = getState(
                        s.missileBarrageDuration > d,
                        Math.max(0.0f, s.missileBarrageDuration - d),
                        Math.max(0.0f, s.arcaneBarrageCooldown - d),
                        0,
                        0.0f,
                        movementDuration
                );
                transition.setTransitionProbability(I[5] * T8);
                list.add(transition);
            }
            transition = new CycleControlledStateTransition();
            transition.spell = AM;
            transition.pause = -castingState.calculationOptions.getLatencyChannel() / 2;
            transition.targetState = getState(
                    false,
                    0.0f,
                    Math.max(0.0f, s.arcaneBarrageCooldown - d),
                    0,
                    0.0f,
                    movementDuration
            );
            transition.setTransitionProbability(I[5] * (s.missileBarrageDuration > 0 ? 1.0f - T8 : 1.0f));
            list.add(transition);
            if (s.missileBarrageDuration > 0 && T8 > 0) {
                transition = new CycleControlledStateTransition();
                transition.spell = AM;
                transition.targetState = getState(
                        s.missileBarrageDuration > AM.castTime,
                        Math.max(0.0f, s.missileBarrageDuration - AM.castTime),
                        Math.max(0.0f, s.arcaneBarrageCooldown - AM.castTime),
                        0,
                        0.0f,
                        0.0f
                );
                transition.setTransitionProbability(T8);
                list.add(transition);
            }
            transition = new CycleControlledStateTransition();
            transition.spell = AM;
            transition.targetState = getState(
                    false,
                    0.0f,
                    Math.max(0.0f, s.arcaneBarrageCooldown - AM.castTime),
                    0,
                    0.0f,
                    0.0f
            );
            transition.setTransitionProbability(s.missileBarrageDuration > 0 ? 1.0f - T8 : 1.0f);
            list.add(transition);
        } else {
        }

        return list;
    }

    private LinkedHashMap<String, State> stateDictionary = new LinkedHashMap<String, State>();

    private State getState(boolean missileBarrageRegistered, float missileBarrageDuration, float arcaneBarrageCooldown, int arcaneBlastStack, float arcaneBlastDuration, float movementDuration) {
        String name = String.format(Locale.ROOT, "AB%d(%s),ABar%s,MB%s%s,M%s", arcaneBlastStack, arcaneBlastDuration, arcaneBarrageCooldown, missileBarrageDuration, missileBarrageRegistered ? "+" : "-", movementDuration);
        State state = stateDictionary.get(name);
        if (state == null) {
            state = new State();
            state.name = name;
            state.missileBarrageRegistered = missileBarrageRegistered;
            state.missileBarrageDuration = missileBarrageDuration;
            state.arcaneBarrageCooldown = arcaneBarrageCooldown;
            state.arcaneBlastStack = arcaneBlastStack;
            state.arcaneBlastDuration = arcaneBlastDuration;
            state.movementDuration = movementDuration;
            stateDictionary.put(name, state);
        }
        return state;
    }

    @Override
    protected boolean canStatesBeDistinguished(CycleState state1, CycleState state2) {
        State a = (State) state1;
        State b = (State) state2;
        return (a.arcaneBlastStack != b.arcaneBlastStack || a.arcaneBarrageCooldown != b.arcaneBarrageCooldown || a.missileBarrageRegistered != b.missileBarrageRegistered || a.arcaneBlastDuration != b.arcaneBlastDuration || a.movementDuration != b.movementDuration /* || (a.MissileBarrageRegistered == true && b.MissileBarrageRegistered == true && a.MissileBarrageDuration != b.MissileBarrageDuration)*/);
    }

    @Override
    public String getStateDescription() {
        return "Cycle Code Legend: 0 = AB, 1 = ABar, 2 = AM\n" +
                "State Descriptions: ABx,ABary,MBz+-\n" +
                "x = number of AB stacks\n" +
                "y = remaining cooldown on Arcane Barrage\n" +
                "z = remaining time on Missile Barrage\n" +
                "+ = Missile Barrage proc visible\n" +
                "- = Missile Barrage proc not visible";
    }
}
