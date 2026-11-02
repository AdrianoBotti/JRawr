// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataArcane.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class ArcaneCycleGenerator extends CycleGenerator {
    private static class State extends CycleState {
        public boolean missileBarrageRegistered;
        public float missileBarrageDuration;
        public boolean arcaneMissilesRegistered;
        public boolean arcaneMissilesProcced;
        public float arcaneBarrageCooldown;
        public int arcaneBlastStack;
        public float tier10TwoPieceDuration;
    }

    public Spell[][] AB, ABar, AM, MBAM;

    private float ABMB;
    private float MB;
    private float aMProc;
    private float T8;
    private int maxStack;
    private boolean T10;
    private float channelLatency;
    private boolean beta;

    private boolean aBarAllowed;
    private boolean aBarOnCooldownOnly;
    private boolean mBDurationCollapsed;
    private boolean aMDurationCollapsed;
    private boolean aBarCooldownCollapsed;
    private boolean tier10TwoPieceCollapsed;

    public ArcaneCycleGenerator(CastingState castingState, boolean ABarAllowed, boolean ABarOnCooldownOnly, boolean MBDurationCollapsed, boolean ABarCooldownCollapsed, boolean Tier10TwoPieceCollapsed, boolean AMDurationCollapsed) {
        this.aBarAllowed = ABarAllowed;
        this.aBarOnCooldownOnly = ABarOnCooldownOnly;
        this.mBDurationCollapsed = MBDurationCollapsed;
        this.aBarCooldownCollapsed = ABarCooldownCollapsed;
        this.tier10TwoPieceCollapsed = Tier10TwoPieceCollapsed;
        this.aMDurationCollapsed = AMDurationCollapsed;

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
        beta = castingState.calculationOptions.getBeta();
        if (beta) {
            aMProc = 0.3f;
        }

        generateStateDescription();
    }

    @Override
    protected CycleState getInitialState() {
        return getState(false, 0.0f, 0.0f, 0, 0.0f, false, false);
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
        if (MB > 0) {
            if (aMProc > 0) {
                CycleControlledStateTransition transition = new CycleControlledStateTransition();
                transition.spell = AB;
                transition.targetState = getState(
                        s.missileBarrageDuration > AB.castTime,
                        15.0f,
                        Math.max(0.0f, s.arcaneBarrageCooldown - AB.castTime),
                        Math.min(maxStack, s.arcaneBlastStack + 1),
                        Math.max(0.0f, s.tier10TwoPieceDuration - AB.castTime),
                        s.arcaneMissilesProcced,
                        true
                );
                transition.setTransitionProbability(ABMB * aMProc);
                list.add(transition);
            }
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = AB;
            transition.targetState = getState(
                    s.missileBarrageDuration > AB.castTime,
                    15.0f,
                    Math.max(0.0f, s.arcaneBarrageCooldown - AB.castTime),
                    Math.min(maxStack, s.arcaneBlastStack + 1),
                    Math.max(0.0f, s.tier10TwoPieceDuration - AB.castTime),
                    s.arcaneMissilesProcced,
                    s.arcaneMissilesProcced
            );
            transition.setTransitionProbability(ABMB * (1 - aMProc));
            list.add(transition);
        }
        if (aMProc > 0) {
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = AB;
            transition.targetState = getState(
                    s.missileBarrageDuration > AB.castTime,
                    Math.max(0.0f, s.missileBarrageDuration - AB.castTime),
                    Math.max(0.0f, s.arcaneBarrageCooldown - AB.castTime),
                    Math.min(maxStack, s.arcaneBlastStack + 1),
                    Math.max(0.0f, s.tier10TwoPieceDuration - AB.castTime),
                    s.arcaneMissilesProcced,
                    true
            );
            transition.setTransitionProbability((1.0f - ABMB) * aMProc);
            list.add(transition);
        }
        CycleControlledStateTransition transition = new CycleControlledStateTransition();
        transition.spell = AB;
        transition.targetState = getState(
                s.missileBarrageDuration > AB.castTime,
                Math.max(0.0f, s.missileBarrageDuration - AB.castTime),
                Math.max(0.0f, s.arcaneBarrageCooldown - AB.castTime),
                Math.min(maxStack, s.arcaneBlastStack + 1),
                Math.max(0.0f, s.tier10TwoPieceDuration - AB.castTime),
                s.arcaneMissilesProcced,
                s.arcaneMissilesProcced
        );
        transition.setTransitionProbability((1.0f - ABMB) * (1 - aMProc));
        list.add(transition);
        if (s.missileBarrageDuration > 0 && T8 > 0) {
            transition = new CycleControlledStateTransition();
            transition.spell = AM;
            transition.targetState = getState(
                    s.missileBarrageDuration > AM.castTime,
                    Math.max(0.0f, s.missileBarrageDuration - AM.castTime),
                    Math.max(0.0f, s.arcaneBarrageCooldown - AM.castTime),
                    0,
                    Math.max(0.0f, s.tier10TwoPieceDuration - AM.castTime), // cannot have 4T8 and 2T10 at the same time
                    false,
                    false
            );
            transition.setTransitionProbability(T8);
            list.add(transition);
        }
        if (!beta || s.arcaneMissilesRegistered || s.missileBarrageRegistered) {
            transition = new CycleControlledStateTransition();
            transition.spell = AM;
            transition.targetState = getState(
                    false,
                    0.0f,
                    Math.max(0.0f, s.arcaneBarrageCooldown - AM.castTime),
                    0,
                    Math.max(0.0f, (s.missileBarrageDuration > 0 && T10) ? 5.0f - channelLatency : s.tier10TwoPieceDuration - AM.castTime),
                    false,
                    false
            );
            transition.setTransitionProbability(s.missileBarrageDuration > 0 ? 1.0f - T8 : 1.0f);
            list.add(transition);
        }
        if (aBarAllowed && (!aBarOnCooldownOnly || s.arcaneBarrageCooldown == 0)) {
            if (MB > 0) {
                if (aMProc > 0) {
                    transition = new CycleControlledStateTransition();
                    transition.spell = ABar;
                    transition.pause = s.arcaneBarrageCooldown;
                    transition.targetState = getState(
                            true,
                            15.0f - ABar.castTime,
                            3.0f - ABar.castTime,
                            0,
                            Math.max(0.0f, s.tier10TwoPieceDuration - ABar.castTime - s.arcaneBarrageCooldown),
                            s.arcaneMissilesProcced,
                            true
                    );
                    transition.setTransitionProbability(MB * aMProc);
                    list.add(transition);
                }
                transition = new CycleControlledStateTransition();
                transition.spell = ABar;
                transition.pause = s.arcaneBarrageCooldown;
                transition.targetState = getState(
                        true,
                        15.0f - ABar.castTime,
                        3.0f - ABar.castTime,
                        0,
                        Math.max(0.0f, s.tier10TwoPieceDuration - ABar.castTime - s.arcaneBarrageCooldown),
                        s.arcaneMissilesProcced,
                        s.arcaneMissilesProcced
                );
                transition.setTransitionProbability(MB * (1 - aMProc));
                list.add(transition);
            }
            if (aMProc > 0) {
                transition = new CycleControlledStateTransition();
                transition.spell = ABar;
                transition.pause = s.arcaneBarrageCooldown;
                transition.targetState = getState(
                        s.missileBarrageDuration > ABar.castTime,
                        Math.max(0.0f, s.missileBarrageDuration - ABar.castTime),
                        3.0f - ABar.castTime,
                        0,
                        Math.max(0.0f, s.tier10TwoPieceDuration - ABar.castTime - s.arcaneBarrageCooldown),
                        s.arcaneMissilesProcced,
                        true
                );
                transition.setTransitionProbability((1.0f - MB) * aMProc);
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
                    Math.max(0.0f, s.tier10TwoPieceDuration - ABar.castTime - s.arcaneBarrageCooldown),
                    s.arcaneMissilesProcced,
                    s.arcaneMissilesProcced
            );
            transition.setTransitionProbability((1.0f - MB) * (1 - aMProc));
            list.add(transition);
        }

        return list;
    }

    private LinkedHashMap<String, State> stateDictionary = new LinkedHashMap<String, State>();

    private State getState(boolean missileBarrageRegistered, float missileBarrageDuration, float arcaneBarrageCooldown, int arcaneBlastStack, float tier10TwoPieceDuration, boolean arcaneMissilesRegistered, boolean arcaneMissilesProcced) {
        String name;
        if (beta) {
            name = String.format(Locale.ROOT, "AB%d,ABar%s,MB%s%s,AM%s%s,2T10=%s", arcaneBlastStack, arcaneBarrageCooldown, missileBarrageDuration, missileBarrageRegistered ? "+" : "-", tier10TwoPieceDuration, arcaneMissilesProcced ? "+" : "-", arcaneMissilesRegistered ? "+" : "-");
        } else {
            name = String.format(Locale.ROOT, "AB%d,ABar%s,MB%s%s,2T10=%s", arcaneBlastStack, arcaneBarrageCooldown, missileBarrageDuration, missileBarrageRegistered ? "+" : "-", tier10TwoPieceDuration);
        }
        State state = stateDictionary.get(name);
        if (state == null) {
            state = new State();
            state.name = name;
            state.missileBarrageRegistered = missileBarrageRegistered;
            state.missileBarrageDuration = missileBarrageDuration;
            state.arcaneBarrageCooldown = arcaneBarrageCooldown;
            state.arcaneBlastStack = arcaneBlastStack;
            state.tier10TwoPieceDuration = tier10TwoPieceDuration;
            state.arcaneMissilesProcced = arcaneMissilesProcced;
            state.arcaneMissilesRegistered = arcaneMissilesRegistered;
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
                ((a.arcaneBarrageCooldown > 0) != (b.arcaneBarrageCooldown > 0)) ||
                a.missileBarrageRegistered != b.missileBarrageRegistered
                || (!tier10TwoPieceCollapsed && a.tier10TwoPieceDuration != b.tier10TwoPieceDuration)
                || (!mBDurationCollapsed && a.missileBarrageRegistered == true && b.missileBarrageRegistered == true && a.missileBarrageDuration != b.missileBarrageDuration)
                || a.arcaneMissilesRegistered != b.arcaneMissilesRegistered
                /*|| (!AMDurationCollapsed && a.ArcaneMissilesRegistered == true && b.ArcaneMissilesRegistered == true && a.ArcaneMissilesDuration != b.ArcaneMissilesDuration)*/);
    }

    @Override
    public String getStateDescription() {
        return "Cycle Code Legend:\n" +
                "State Descriptions: ABx,ABary,MBz+-,2T10=w\n" +
                "x = number of AB stacks\n" +
                "y = remaining cooldown on Arcane Barrage\n" +
                "z = remaining time on Missile Barrage\n" +
                "+ = Missile Barrage proc visible\n" +
                "- = Missile Barrage proc not visible\n" +
                "w = remaining time on 2T10 effect";
    }
}
