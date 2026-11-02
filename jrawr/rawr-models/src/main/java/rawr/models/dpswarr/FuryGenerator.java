// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/Markov/FuryGenerator.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The TRANSTYPE debug define is off in the shipped DLL, so only the non-TRANSTYPE format path is
// ported. `Math.Round(x, 3)` (banker's) uses a BigDecimal HALF_EVEN helper per section 8. The
// `string.Format("{2:0.000}")` state keys use a HALF_UP helper matching the .NET custom-format
// rounding (the keys only partition the diagnostic state space).
package rawr.models.dpswarr;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.stats.Stats;
import rawr.base.talents.WarriorTalents;
import rawr.base.algorithms.State;
import rawr.base.algorithms.StateSpaceGenerator;
import rawr.base.algorithms.StateTransition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class FuryGenerator extends StateSpaceGenerator<Ability> {
    static final int NUM_DEC = 3;
    static final String FORMAT = "0.000";

    public FuryGenerator(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo) {
        WarriorTalents talents = c.getWarriorTalents();
        rot = new FuryRotation(c, s, cf, wa, co, bo);
        rot.initialize();
        latentGcd = roundHalfEven(1.5 + co.getFullLatency(), NUM_DEC);
        hastedGcd = roundHalfEven(1.0 + co.getFullLatency(), NUM_DEC);

        hsPerc = .8;

        bs1Proc = c.getWarriorTalents().getBloodsurge() / 15f; // 7/13/20%
        bs2Proc = 0f;

        if (s.getBonusWarrior_T10_4P_BSSDProcChange() > 0) {
            bs2Proc = 0.2;
        }

        bt = rot.getWrapper(BloodThirst.class).ability;
        ww = rot.getWrapper(WhirlWind.class).ability;
        bs = rot.getWrapper(BloodSurge.class).ability;
        hs = rot.getWrapper(HeroicStrike.class).ability;
        whiteInterval = wa.mhEffectiveSpeed();

        setChances();
    }

    protected double latentGcd;
    protected double hastedGcd;
    public double hsPerc;

    public FuryRotation rot = null;

    protected Ability bt, ww, bs, hs;

    protected double bs1Proc;
    protected double bs2Proc;

    protected double hsProcChance;

    protected BloodSurgeMatrix wwMatrix, btMatrix;

    protected double whiteInterval;

    private void setChances() {
        // HS matrix is commented out in the original

        // WW
        wwMatrix = new BloodSurgeMatrix(bs1Proc, bs2Proc);
        wwMatrix.addAbility(ww.mhAtkTable().anyLand());
        wwMatrix.addAbility(ww.ohAtkTable().anyLand());

        // BT
        btMatrix = new BloodSurgeMatrix(bs1Proc, bs2Proc);
        btMatrix.addAbility(bt.mhAtkTable().anyLand());

        hsProcChance = hs.mhAtkTable().anyLand() * bs1Proc;
    }

    public class StateFury extends State<Ability> {
        public double btCooldown;
        public double wwCooldown;
        public int bsProcced;
        public boolean bsHasted;
    }

    public class DelegateStateTransition extends StateTransition<Ability> {
        @FunctionalInterface
        public interface NewTransitionChance {
            double invoke();
        }

        public NewTransitionChance myDel;

        @Override
        public double getTransitionProbability() {
            return myDel.invoke();
        }
    }

    @Override
    protected State<Ability> getInitialState() {
        return getState(0, false, 0, 0, 0);
    }

    @Override
    protected List<StateTransition<Ability>> getStateTransitions(State<Ability> state) {
        StateFury s = (StateFury) state;
        List<StateTransition<Ability>> list = new ArrayList<StateTransition<Ability>>();

        Rotation.AbilWrapper btWrapper = rot.getWrapper(BloodThirst.class);
        Rotation.AbilWrapper wwWrapper = rot.getWrapper(WhirlWind.class);
        Rotation.AbilWrapper bsWrapper = rot.getWrapper(BloodSurge.class);
        Rotation.AbilWrapper hsWrapper = rot.getWrapper(HeroicStrike.class);

        if (s.wwCooldown <= 0) {
            // Whirlwind
            double transDuration = latentGcd;
            double newBtCd = Math.max(0, s.btCooldown - transDuration);
            double newWwCd = wwWrapper.ability.cd - transDuration;

            double numHSes = transDuration / whiteInterval;

            BloodSurgeMatrix mat = new BloodSurgeMatrix(this.bs1Proc, this.bs2Proc);
            mat.setBaseChance(wwMatrix.procChances(1) + wwMatrix.procChances(2));
            mat.addAbility(hsWrapper.ability.mhAtkTable().anyLand() * this.hsPerc, numHSes);

            double chanceProc = (1 - wwWrapper.ability.mhAtkTable().anyLand() * bs1Proc) * (1 - wwWrapper.ability.ohAtkTable().anyLand() * bs1Proc);

            DelegateStateTransition.NewTransitionChance del0 = () ->
                    chanceProc * Math.pow(1 - hsProcChance * hsPerc, numHSes);
            DelegateStateTransition.NewTransitionChance del1 = () ->
                    (1 - chanceProc * Math.pow(1 - hsProcChance * hsPerc, numHSes)) * (1 - bs2Proc);
            DelegateStateTransition.NewTransitionChance del2 = () ->
                    (1 - chanceProc * Math.pow(1 - hsProcChance * hsPerc, numHSes)) * bs2Proc;

            // if WW is up, use it

            DelegateStateTransition t2 = new DelegateStateTransition();
            t2.ability = wwWrapper.ability;
            t2.transitionDuration = transDuration;
            t2.targetState = getState(2, true, newBtCd, newWwCd, 2);
            t2.myDel = del2;
            list.add(t2);

            // if BS procs 1
            DelegateStateTransition t1 = new DelegateStateTransition();
            t1.ability = wwWrapper.ability;
            t1.transitionDuration = transDuration;
            t1.targetState = getState((s.bsHasted ? 2 : 1), s.bsHasted, newBtCd, newWwCd, 1);
            t1.myDel = del1;
            list.add(t1);

            // if BS doesn't proc
            DelegateStateTransition t0 = new DelegateStateTransition();
            t0.ability = wwWrapper.ability;
            t0.transitionDuration = transDuration;
            t0.targetState = getState(s.bsProcced, s.bsHasted, newBtCd, newWwCd, 0);
            t0.myDel = del0;
            list.add(t0);
        } else if (s.btCooldown <= 0) {
            // BloodThirst
            double transDuration = latentGcd;
            double newBtCd = btWrapper.ability.cd - transDuration;
            double newWwCd = Math.max(0, s.wwCooldown - transDuration);

            double numHSes = transDuration / whiteInterval;

            double chanceProc = (1 - btWrapper.ability.mhAtkTable().anyLand() * bs1Proc);
            DelegateStateTransition.NewTransitionChance del0 = () ->
                    chanceProc * Math.pow(1 - hsProcChance * hsPerc, numHSes);
            DelegateStateTransition.NewTransitionChance del1 = () ->
                    (1 - chanceProc * Math.pow(1 - hsProcChance * hsPerc, numHSes)) * (1 - bs2Proc);
            DelegateStateTransition.NewTransitionChance del2 = () ->
                    (1 - chanceProc * Math.pow(1 - hsProcChance * hsPerc, numHSes)) * bs2Proc;

            // if BS procs 2
            DelegateStateTransition t2 = new DelegateStateTransition();
            t2.ability = btWrapper.ability;
            t2.transitionDuration = transDuration;
            t2.targetState = getState(2, true, newBtCd, newWwCd, 2);
            t2.myDel = del2;
            list.add(t2);

            // if BS procs 1
            DelegateStateTransition t1 = new DelegateStateTransition();
            t1.ability = btWrapper.ability;
            t1.transitionDuration = transDuration;
            t1.targetState = getState((s.bsHasted ? 2 : 1), s.bsHasted, newBtCd, newWwCd, 1);
            t1.myDel = del1;
            list.add(t1);

            // if BS doesn't proc
            DelegateStateTransition t0 = new DelegateStateTransition();
            t0.ability = btWrapper.ability;
            t0.transitionDuration = transDuration;
            t0.targetState = getState(s.bsProcced, s.bsHasted, newBtCd, newWwCd, 0);
            t0.myDel = del0;
            list.add(t0);
        } else if (s.bsProcced > 0) {
            // Slam

            double transDuration = (s.bsHasted ? hastedGcd : latentGcd);
            double newBtCd = Math.max(0, s.btCooldown - transDuration);
            double newWwCd = Math.max(0, s.wwCooldown - transDuration);

            double numHSes = transDuration / whiteInterval;

            double chanceProc = 1;
            DelegateStateTransition.NewTransitionChance del0 = () ->
                    chanceProc * Math.pow(1 - hsProcChance * hsPerc, numHSes);
            DelegateStateTransition.NewTransitionChance del1 = () ->
                    (1 - chanceProc * Math.pow(1 - hsProcChance * hsPerc, numHSes)) * (1 - bs2Proc);
            DelegateStateTransition.NewTransitionChance del2 = () ->
                    (1 - chanceProc * Math.pow(1 - hsProcChance * hsPerc, numHSes)) * bs2Proc;

            // if HS procs 2
            DelegateStateTransition t2 = new DelegateStateTransition();
            t2.ability = bsWrapper.ability;
            t2.transitionDuration = transDuration;
            t2.targetState = getState(2, true, newBtCd, newWwCd, 2);
            t2.myDel = del2;
            list.add(t2);

            // if HS procs 1
            DelegateStateTransition t1 = new DelegateStateTransition();
            t1.ability = bsWrapper.ability;
            t1.transitionDuration = transDuration;
            t1.targetState = getState(s.bsProcced, // if we had 2 and then procced, we're at 2. If we had 1 and then procced, we're at 1
                    (s.bsProcced == 2), // if we're at 2 before, we're still hasted. If we're at 1 before, we lost the haste before we procced
                    newBtCd, newWwCd, 1);
            t1.myDel = del1;
            list.add(t1);

            // if HS doesn't proc
            DelegateStateTransition t0 = new DelegateStateTransition();
            t0.ability = bsWrapper.ability;
            t0.transitionDuration = transDuration;
            t0.targetState = getState(s.bsProcced - 1,    // num procs decreases
                    (s.bsProcced == 2), // haste falls off if we were at 1
                    newBtCd, newWwCd, 0);
            t0.myDel = del0;
            list.add(t0);
        } else {
            double transDuration = Math.min(s.btCooldown, s.wwCooldown);

            double numHSes = transDuration / whiteInterval;
            double chanceProc = 1;
            DelegateStateTransition.NewTransitionChance del0 = () ->
                    chanceProc * Math.pow(1 - hsProcChance * hsPerc, numHSes);
            DelegateStateTransition.NewTransitionChance del1 = () ->
                    (1 - chanceProc * Math.pow(1 - hsProcChance * hsPerc, numHSes)) * (1 - bs2Proc);
            DelegateStateTransition.NewTransitionChance del2 = () ->
                    (1 - chanceProc * Math.pow(1 - hsProcChance * hsPerc, numHSes)) * bs2Proc;

            DelegateStateTransition t2 = new DelegateStateTransition();
            t2.ability = null;
            t2.transitionDuration = transDuration;
            t2.targetState = getState(2, true, s.btCooldown - transDuration, s.wwCooldown - transDuration, 2);
            t2.myDel = del2;
            list.add(t2);

            // if HS procs 1
            DelegateStateTransition t1 = new DelegateStateTransition();
            t1.ability = null;
            t1.transitionDuration = transDuration;
            t1.targetState = getState((s.bsHasted ? 2 : 1), s.bsHasted, s.btCooldown - transDuration, s.wwCooldown - transDuration, 1);
            t1.myDel = del1;
            list.add(t1);

            // if HS doesn't proc
            DelegateStateTransition t0 = new DelegateStateTransition();
            t0.ability = null;
            t0.transitionDuration = transDuration;
            t0.targetState = getState(s.bsProcced, s.bsHasted, s.btCooldown - transDuration, s.wwCooldown - transDuration, 0);
            t0.myDel = del0;
            list.add(t0);
        }

        return list;
    }

    private Map<String, StateFury> stateDictionary = new LinkedHashMap<String, StateFury>();

    public StateFury getState(int bsProc, boolean bsHasted, double btCd, double wwCd, int transition) {
        String name = String.format("BSProc %d%s,BTCD %s,WWCD %s",
                bsProc,
                bsHasted ? "+" : "-",
                format000(btCd),
                format000(wwCd));
        StateFury state = stateDictionary.get(name);
        if (state == null) {
            state = new StateFury();
            state.name = name;
            state.btCooldown = btCd;
            state.wwCooldown = wwCd;
            state.bsHasted = bsHasted;
            state.bsProcced = bsProc;
            stateDictionary.put(name, state);
        }
        return state;
    }

    public StateFury getState(int bsProc, boolean bsHasted, float btCd, float wwCd, int transition) {
        return getState(bsProc, bsHasted, (double) btCd, (double) wwCd, transition);
    }

    void walkStateSpace(List<State<Ability>> stateSpace) {
        double wwProc = (1 - bs1Proc * ww.mhAtkTable().anyLand()) * (1 - bs1Proc * ww.ohAtkTable().anyLand());
        double btProc = (1 - bs1Proc * bt.mhAtkTable().anyLand());
        double hsConst = bs1Proc * hs.mhAtkTable().anyLand();

        double transChanceBoth, innerParenth;

        boolean caughtYet = false;
        for (State<Ability> s : stateSpace) {
            for (StateTransition<Ability> t : s.transitions) {
                if (t.targetState.name.endsWith("2") && t.ability == ww && !caughtYet) {
                    caughtYet = true;
                    System.out.println(t.getTransitionProbability());
                }

                if (t.targetState.name.endsWith("2")) {
                    transChanceBoth = t.getTransitionProbability() / bs2Proc;
                } else if (t.targetState.name.endsWith("1")) {
                    transChanceBoth = t.getTransitionProbability() / (1 - bs2Proc);
                } else {
                    transChanceBoth = 1 - t.getTransitionProbability();
                }

                if (t.ability == ww) {
                    innerParenth = (1 - transChanceBoth) / wwProc;
                } else if (t.ability == bt) {
                    innerParenth = (1 - transChanceBoth) / btProc;
                } else if (t.ability == bs) {
                    innerParenth = 1 - transChanceBoth;
                } else {
                    innerParenth = 1 - transChanceBoth;
                }
                double perc = (1 - Math.pow(innerParenth, whiteInterval / t.getTransitionDuration())) / hsConst;
                //Console.WriteLine(perc);
            }
        }
    }

    static double roundHalfEven(double value, int digits) {
        if (!Double.isFinite(value)) return value;
        return new BigDecimal(value).setScale(digits, RoundingMode.HALF_EVEN).doubleValue();
    }

    static String format000(double value) {
        if (!Double.isFinite(value)) return Double.toString(value);
        return new BigDecimal(value).setScale(3, RoundingMode.HALF_UP).toPlainString();
    }
}
