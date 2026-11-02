// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class ArcaneMovementCycleGenerator extends CycleGenerator {
    private static class State extends CycleState {
        public boolean missileBarrageRegistered;
        public float missileBarrageDuration;
        public float arcaneBarrageCooldown;
        public int arcaneBlastStack;
        public float tier10TwoPieceDuration;
        public float movementDuration;
    }

    public Spell[][] AB, ABar, AM, MBAM;

    private float ABMB;
    private float MB;
    private float T8;
    private int maxStack;
    private boolean T10;
    private float channelLatency;

    private boolean aBarAllowed;
    private boolean aBarOnCooldownOnly;
    private boolean mBDurationCollapsed;
    private boolean aBarCooldownCollapsed;
    private boolean tier10TwoPieceCollapsed;

    private float movementEventRate;
    private float movementDuration;
    private boolean instantsOnMovementOnly;

    public ArcaneMovementCycleGenerator(CastingState castingState, float movementEventRate, float movementDuration, boolean ABarAllowed, boolean ABarOnCooldownOnly, boolean instantsOnMovementOnly, boolean MBDurationCollapsed, boolean ABarCooldownCollapsed, boolean Tier10TwoPieceCollapsed) {
        this.aBarAllowed = ABarAllowed;
        this.aBarOnCooldownOnly = ABarOnCooldownOnly;
        this.mBDurationCollapsed = MBDurationCollapsed;
        this.aBarCooldownCollapsed = ABarCooldownCollapsed;
        this.tier10TwoPieceCollapsed = Tier10TwoPieceCollapsed;
        this.movementEventRate = movementEventRate;
        this.movementDuration = movementDuration;
        this.instantsOnMovementOnly = instantsOnMovementOnly;

        Solver calc = castingState.getSolver();
        maxStack = 4;

        AB = new Spell[maxStack + 1][2];
        ABar = new Spell[maxStack + 1][2];
        AM = new Spell[maxStack + 1][2];
        MBAM = new Spell[maxStack + 1][2];

        for (int stack = 0; stack <= maxStack; stack++) {
            AB[stack][0] = calc.getArcaneBlastTemplate().getSpell(castingState, stack);
            AB[stack][0].label = "ArcaneBlast" + stack;
            ABar[stack][0] = calc.getArcaneBarrageTemplate().getSpell(castingState, stack);
            ABar[stack][0].label = "ArcaneBarrage" + stack;
            AM[stack][0] = calc.getArcaneMissilesTemplate().getSpell(castingState, false, stack);
            AM[stack][0].label = "ArcaneMissiles" + stack;
            MBAM[stack][0] = calc.getArcaneMissilesTemplate().getSpell(castingState, true, stack);
            MBAM[stack][0].label = "MBAM" + stack;
            AB[stack][1] = calc.getArcaneBlastTemplate().getSpell(castingState.getTier10TwoPieceState(), stack);
            AB[stack][1].label = "2T10:ArcaneBlast" + stack;
            ABar[stack][1] = calc.getArcaneBarrageTemplate().getSpell(castingState.getTier10TwoPieceState(), stack);
            ABar[stack][1].label = "2T10:ArcaneBarrage" + stack;
            AM[stack][1] = calc.getArcaneMissilesTemplate().getSpell(castingState.getTier10TwoPieceState(), false, stack);
            AM[stack][1].label = "2T10:ArcaneMissiles" + stack;
            MBAM[stack][1] = calc.getArcaneMissilesTemplate().getSpell(castingState.getTier10TwoPieceState(), true, stack);
            MBAM[stack][1].label = "2T10:MBAM" + stack;
        }

        ABMB = 0.08f * castingState.mageTalents.getMissileBarrage();
        MB = 0.04f * castingState.mageTalents.getMissileBarrage();
        T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
        T10 = castingState.baseStats.getMage2T10() > 0;
        channelLatency = castingState.calculationOptions.getLatencyChannel();

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
        AB = this.AB[s.arcaneBlastStack][s.tier10TwoPieceDuration > 0 ? 1 : 0];
        ABar = this.ABar[s.arcaneBlastStack][s.tier10TwoPieceDuration - s.arcaneBarrageCooldown > 0 ? 1 : 0];
        if (s.missileBarrageDuration > 0) {
            AM = this.MBAM[s.arcaneBlastStack][s.tier10TwoPieceDuration > 0 ? 1 : 0];
        } else {
            AM = this.AM[s.arcaneBlastStack][s.tier10TwoPieceDuration > 0 ? 1 : 0];
        }
        if (s.movementDuration == 0.0f) {
            float move = (float) (1.0 - Math.exp(-movementEventRate * AB.castTime));
            if (s.arcaneBarrageCooldown > AB.castTime) {
                move = 0.0f;
            }
            if (MB > 0) {
                CycleControlledStateTransition transition = new CycleControlledStateTransition();
                transition.spell = AB;
                transition.targetState = getState(
                        s.missileBarrageDuration > AB.castTime,
                        15.0f,
                        Math.max(0.0f, s.arcaneBarrageCooldown - AB.castTime),
                        Math.min(maxStack, s.arcaneBlastStack + 1),
                        Math.max(0.0f, s.tier10TwoPieceDuration - AB.castTime),
                        0.0f
                );
                transition.setTransitionProbability(ABMB * (1.0f - move));
                list.add(transition);
                if (move > 0) {
                    transition = new CycleControlledStateTransition();
                    transition.spell = AB;
                    transition.targetState = getState(
                            s.missileBarrageDuration > AB.castTime,
                            15.0f,
                            Math.max(0.0f, s.arcaneBarrageCooldown - AB.castTime),
                            Math.min(maxStack, s.arcaneBlastStack + 1),
                            Math.max(0.0f, s.tier10TwoPieceDuration - AB.castTime),
                            movementDuration
                    );
                    transition.setTransitionProbability(ABMB * move);
                    list.add(transition);
                }
            }
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = AB;
            transition.targetState = getState(
                    s.missileBarrageDuration > AB.castTime,
                    Math.max(0.0f, s.missileBarrageDuration - AB.castTime),
                    Math.max(0.0f, s.arcaneBarrageCooldown - AB.castTime),
                    Math.min(maxStack, s.arcaneBlastStack + 1),
                    Math.max(0.0f, s.tier10TwoPieceDuration - AB.castTime),
                    0.0f
            );
            transition.setTransitionProbability((1.0f - ABMB) * (1.0f - move));
            list.add(transition);
            if (move > 0) {
                transition = new CycleControlledStateTransition();
                transition.spell = AB;
                transition.targetState = getState(
                        s.missileBarrageDuration > AB.castTime,
                        Math.max(0.0f, s.missileBarrageDuration - AB.castTime),
                        Math.max(0.0f, s.arcaneBarrageCooldown - AB.castTime),
                        Math.min(maxStack, s.arcaneBlastStack + 1),
                        Math.max(0.0f, s.tier10TwoPieceDuration - AB.castTime),
                        movementDuration
                );
                transition.setTransitionProbability((1.0f - ABMB) * move);
                list.add(transition);
            }
            move = (float) (1.0 - Math.exp(-movementEventRate * AM.castTime));
            if (s.arcaneBarrageCooldown > AM.castTime) {
                move = 0.0f;
            }
            if (s.missileBarrageDuration > 0 && T8 > 0) {
                transition = new CycleControlledStateTransition();
                transition.spell = AM;
                transition.targetState = getState(
                        s.missileBarrageDuration > AM.castTime,
                        Math.max(0.0f, s.missileBarrageDuration - AM.castTime),
                        Math.max(0.0f, s.arcaneBarrageCooldown - AM.castTime),
                        0,
                        Math.max(0.0f, s.tier10TwoPieceDuration - AM.castTime), // cannot have 4T8 and 2T10 at the same time
                        0.0f
                );
                transition.setTransitionProbability(T8 * (1.0f - move));
                list.add(transition);
                if (move > 0) {
                    transition = new CycleControlledStateTransition();
                    transition.spell = AM;
                    transition.targetState = getState(
                            s.missileBarrageDuration > AM.castTime,
                            Math.max(0.0f, s.missileBarrageDuration - AM.castTime),
                            Math.max(0.0f, s.arcaneBarrageCooldown - AM.castTime),
                            0,
                            Math.max(0.0f, s.tier10TwoPieceDuration - AM.castTime), // cannot have 4T8 and 2T10 at the same time
                            movementDuration
                    );
                    transition.setTransitionProbability(T8 * move);
                    list.add(transition);
                }
            }
            transition = new CycleControlledStateTransition();
            transition.spell = AM;
            transition.targetState = getState(
                    false,
                    0.0f,
                    Math.max(0.0f, s.arcaneBarrageCooldown - AM.castTime),
                    0,
                    Math.max(0.0f, (s.missileBarrageDuration > 0 && T10) ? 5.0f - channelLatency : s.tier10TwoPieceDuration - AM.castTime),
                    0.0f
            );
            transition.setTransitionProbability((s.missileBarrageDuration > 0 ? 1.0f - T8 : 1.0f) * (1.0f - move));
            list.add(transition);
            if (move > 0) {
                transition = new CycleControlledStateTransition();
                transition.spell = AM;
                transition.targetState = getState(
                        false,
                        0.0f,
                        Math.max(0.0f, s.arcaneBarrageCooldown - AM.castTime),
                        0,
                        Math.max(0.0f, (s.missileBarrageDuration > 0 && T10) ? 5.0f - channelLatency : s.tier10TwoPieceDuration - AM.castTime),
                        movementDuration
                );
                transition.setTransitionProbability((s.missileBarrageDuration > 0 ? 1.0f - T8 : 1.0f) * move);
                list.add(transition);
            }
        }
        if (aBarAllowed && (!aBarOnCooldownOnly || s.arcaneBarrageCooldown == 0) && (s.movementDuration > 0 || !instantsOnMovementOnly)) {
            float move = (float) (1.0 - Math.exp(-movementEventRate * ABar.castTime));
            move = 0.0f;
            if (MB > 0) {
                CycleControlledStateTransition transition = new CycleControlledStateTransition();
                transition.spell = ABar;
                transition.pause = s.arcaneBarrageCooldown;
                transition.targetState = getState(
                        true,
                        15.0f - ABar.castTime,
                        3.0f - ABar.castTime,
                        0,
                        Math.max(0.0f, s.tier10TwoPieceDuration - ABar.castTime - s.arcaneBarrageCooldown),
                        Math.max(0.0f, s.movementDuration - ABar.castTime - s.arcaneBarrageCooldown)
                );
                transition.setTransitionProbability(MB * (1.0f - move));
                list.add(transition);
            }
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = ABar;
            transition.pause = s.arcaneBarrageCooldown;
            transition.targetState = getState(
                    s.missileBarrageDuration > ABar.castTime,
                    Math.max(0.0f, s.missileBarrageDuration - ABar.castTime),
                    3.0f - ABar.castTime,
                    0,
                    Math.max(0.0f, s.tier10TwoPieceDuration - ABar.castTime - s.arcaneBarrageCooldown),
                    Math.max(0.0f, s.movementDuration - ABar.castTime - s.arcaneBarrageCooldown)
            );
            transition.setTransitionProbability((1.0f - MB) * (1.0f - move));
            list.add(transition);
        }
        if (s.movementDuration > 0) {
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = null;
            transition.pause = s.movementDuration;
            transition.targetState = getState(
                    s.missileBarrageDuration > s.movementDuration,
                    Math.max(0.0f, s.missileBarrageDuration - s.movementDuration),
                    Math.max(0.0f, s.arcaneBarrageCooldown - s.movementDuration),
                    s.arcaneBlastStack,
                    Math.max(0.0f, s.tier10TwoPieceDuration - s.movementDuration),
                    0.0f
            );
            transition.setTransitionProbability(1.0f);
            list.add(transition);
        }

        return list;
    }

    private LinkedHashMap<String, State> stateDictionary = new LinkedHashMap<String, State>();

    private State getState(boolean missileBarrageRegistered, float missileBarrageDuration, float arcaneBarrageCooldown, int arcaneBlastStack, float tier10TwoPieceDuration, float movementDuration) {
        String name = String.format(Locale.ROOT, "AB%d,ABar%s,MB%s%s,2T10=%s,Move=%s", arcaneBlastStack, arcaneBarrageCooldown, missileBarrageDuration, missileBarrageRegistered ? "+" : "-", tier10TwoPieceDuration, movementDuration);
        State state = stateDictionary.get(name);
        if (state == null) {
            state = new State();
            state.name = name;
            state.missileBarrageRegistered = missileBarrageRegistered;
            state.missileBarrageDuration = missileBarrageDuration;
            state.arcaneBarrageCooldown = arcaneBarrageCooldown;
            state.arcaneBlastStack = arcaneBlastStack;
            state.tier10TwoPieceDuration = tier10TwoPieceDuration;
            state.movementDuration = movementDuration;
            stateDictionary.put(name, state);
        }
        return state;
    }

    @Override
    protected boolean canStatesBeDistinguished(CycleState state1, CycleState state2) {
        State a = (State) state1;
        State b = (State) state2;
        return (a.arcaneBlastStack != b.arcaneBlastStack ||
                (!aBarCooldownCollapsed && a.arcaneBarrageCooldown != b.arcaneBarrageCooldown) ||
                (((a.arcaneBarrageCooldown > 0) != (b.arcaneBarrageCooldown > 0)) && (a.movementDuration > 0 || !instantsOnMovementOnly)) ||
                a.missileBarrageRegistered != b.missileBarrageRegistered
                || (!tier10TwoPieceCollapsed && a.tier10TwoPieceDuration != b.tier10TwoPieceDuration)
                || (!mBDurationCollapsed && a.missileBarrageRegistered == true && b.missileBarrageRegistered == true && a.missileBarrageDuration != b.missileBarrageDuration)
                || (a.movementDuration != b.movementDuration));
    }

    @Override
    public String getStateDescription() {
        return "State Descriptions: ABx,ABary,MBz+-,2T10=w,Move=m\n" +
                "x = number of AB stacks\n" +
                "y = remaining cooldown on Arcane Barrage\n" +
                "z = remaining time on Missile Barrage\n" +
                "+ = Missile Barrage proc visible\n" +
                "- = Missile Barrage proc not visible\n" +
                "w = remaining time on 2T10 effect\n" +
                "m = remaining movement time";
    }
}
