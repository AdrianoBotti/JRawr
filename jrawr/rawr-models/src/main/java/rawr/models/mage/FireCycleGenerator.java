// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataFire.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FireCycleGenerator extends CycleGenerator {
    private static class State extends CycleState {
        public float scorchDuration;
        public float livingBombDuration;
        public int hotStreakCount;
        public float pyroDuration;
        public boolean pyroRegistered;
        public float tier10TwoPieceDuration;
    }

    public Spell[] Pyro, FB, LB, sc;

    private float HS;
    private float T8;
    private boolean T10;

    private boolean maintainScorch = false;
    private boolean livingBombGlyph;

    private float lBDotCritRate;

    public FireCycleGenerator(CastingState castingState) {
        FB = new Spell[2];
        sc = new Spell[2];
        LB = new Spell[2];
        Pyro = new Spell[2];

        FB[0] = castingState.getSpell(SpellId.Fireball);
        sc[0] = castingState.getSpell(SpellId.Scorch);
        LB[0] = castingState.getSpell(SpellId.LivingBomb);
        Pyro[0] = castingState.getSpell(SpellId.PyroblastPOM); // does not account for dot uptime
        FB[0].label = "Fireball";
        sc[0].label = "Scorch";
        LB[0].label = "Living Bomb";
        Pyro[0].label = "Pyroblast";

        FB[1] = castingState.getTier10TwoPieceState().getSpell(SpellId.Fireball);
        sc[1] = castingState.getTier10TwoPieceState().getSpell(SpellId.Scorch);
        LB[1] = castingState.getTier10TwoPieceState().getSpell(SpellId.LivingBomb);
        Pyro[1] = castingState.getTier10TwoPieceState().getSpell(SpellId.PyroblastPOM); // does not account for dot uptime
        FB[1].label = "2T10:Fireball";
        sc[1].label = "2T10:Scorch";
        LB[1].label = "2T10:Living Bomb";
        Pyro[1].label = "2T10:Pyroblast";

        HS = castingState.mageTalents.getHotStreak() / 3.0f;
        T8 = CalculationOptionsMage.SetBonus4T8ProcRate * castingState.baseStats.getMage4T8();
        //maintainScorch = castingState.CalculationOptions.MaintainScorch;
        livingBombGlyph = castingState.mageTalents.isGlyphOfLivingBomb();
        lBDotCritRate = castingState.getFireCritRate();
        T10 = castingState.baseStats.getMage2T10() > 0;

        generateStateDescription();
    }

    @Override
    protected CycleState getInitialState() {
        return getState(0.0f, 0.0f, 0, 0.0f, false, 0.0f);
    }

    @Override
    protected List<CycleControlledStateTransition> getStateTransitions(CycleState state) {
        State s = (State) state;
        List<CycleControlledStateTransition> list = new ArrayList<CycleControlledStateTransition>();
        Spell Sc = this.sc[s.tier10TwoPieceDuration > 0 ? 1 : 0];
        Spell FB = this.FB[s.tier10TwoPieceDuration > 0 ? 1 : 0];
        Spell LB = this.LB[s.tier10TwoPieceDuration > 0 ? 1 : 0];
        Spell Pyro = this.Pyro[s.tier10TwoPieceDuration > 0 ? 1 : 0];
        if (maintainScorch && s.scorchDuration < Sc.castTime) {
            // LB explosion and/or ticks can happen during the cast
            // account for all combinations of hot streak interaction
            float castTime = Sc.castTime;
            int ticksLeft = Math.max(0, (int) (s.livingBombDuration / 3.0f));
            int ticksLeftAfter = Math.max(0, (int) ((s.livingBombDuration - castTime) / 3.0f));
            int hotStreakEvents = ticksLeft - ticksLeftAfter;
            //if (!livingBombGlyph)
            {
                hotStreakEvents = 0;
            }
            boolean explosion = false;
            if (ticksLeft > 0 && ticksLeftAfter == 0) {
                explosion = true;
                hotStreakEvents++;
            }
            for (int i = 0; i < (1 << (hotStreakEvents + 1)); i++) {
                int k = i;
                int hsc = s.hotStreakCount;
                float pd = Math.max(0f, s.pyroDuration - castTime);
                boolean pr = s.pyroDuration > castTime;
                float chance = 1.0f;
                for (int j = 0; j < hotStreakEvents; j++) {
                    float crit = (j == hotStreakEvents - 1 && explosion) ? LB.critRate : lBDotCritRate;
                    if (k % 2 == 1) {
                        chance *= crit;
                        if (hsc == 1) {
                            pd = 10.0f - (s.livingBombDuration - ticksLeft * 3);
                            pr = true;
                        }
                        hsc = (hsc + 1) % 2;
                    } else {
                        chance *= (1 - crit);
                        hsc = 0;
                    }
                    k = k >> 1;
                }
                if (k % 2 == 1) {
                    chance *= Sc.critRate;
                    if (hsc == 1) {
                        pd = 10.0f;
                    }
                    hsc = (hsc + 1) % 2;
                } else {
                    chance *= (1 - Sc.critRate);
                    hsc = 0;
                }
                CycleControlledStateTransition transition = new CycleControlledStateTransition();
                transition.spell = Sc;
                transition.targetState = getState(
                        30.0f + Sc.castTime,
                        Math.max(0f, s.livingBombDuration - castTime),
                        hsc,
                        pd,
                        pr,
                        Math.max(0.0f, s.tier10TwoPieceDuration - Sc.castTime)
                );
                transition.setTransitionProbability(chance);
                list.add(transition);
            }
        } else if (s.livingBombDuration <= 0f) {
            CycleControlledStateTransition transition = new CycleControlledStateTransition();
            transition.spell = LB;
            transition.targetState = getState(
                    Math.max(0f, s.scorchDuration - LB.castTime),
                    12f - LB.castTime,
                    s.hotStreakCount,
                    Math.max(0f, s.pyroDuration - LB.castTime),
                    s.pyroDuration > LB.castTime,
                    Math.max(0.0f, s.tier10TwoPieceDuration - LB.castTime)
            );
            transition.setTransitionProbability(1);
            list.add(transition);
        } else if (s.pyroRegistered) {
            float castTime = Pyro.castTime;
            int ticksLeft = Math.max(0, (int) (s.livingBombDuration / 3.0f));
            int ticksLeftAfter = Math.max(0, (int) ((s.livingBombDuration - castTime) / 3.0f));
            int hotStreakEvents = ticksLeft - ticksLeftAfter;
            //if (!livingBombGlyph)
            {
                hotStreakEvents = 0;
            }
            boolean explosion = false;
            if (ticksLeft > 0 && ticksLeftAfter == 0) {
                explosion = true;
                hotStreakEvents++;
            }
            for (int i = 0; i < (1 << hotStreakEvents); i++) {
                int k = i;
                int hsc = s.hotStreakCount;
                float pd1 = Math.max(0f, s.pyroDuration - castTime);
                boolean pr1 = s.pyroDuration > castTime;
                float pd2 = 0.0f;
                boolean pr2 = false;
                float chance = 1.0f;
                for (int j = 0; j < hotStreakEvents; j++) {
                    float crit = (j == hotStreakEvents - 1 && explosion) ? LB.critRate : lBDotCritRate;
                    if (k % 2 == 1) {
                        chance *= crit;
                        if (hsc == 1) {
                            pd1 = 10.0f - (s.livingBombDuration - ticksLeft * 3);
                            pr1 = true;
                            pd2 = 10.0f - (s.livingBombDuration - ticksLeft * 3);
                            pr2 = true;
                        }
                        hsc = (hsc + 1) % 2;
                    } else {
                        chance *= (1 - crit);
                        hsc = 0;
                    }
                    k = k >> 1;
                }
                if (T8 > 0) {
                    CycleControlledStateTransition transition = new CycleControlledStateTransition();
                    transition.spell = Pyro;
                    transition.targetState = getState(
                            Math.max(0f, s.scorchDuration - castTime),
                            Math.max(0f, s.livingBombDuration - castTime),
                            hsc,
                            pd1,
                            pr1,
                            T10 ? Math.max(0.0f, 5.0f - Pyro.castTime) : 0f
                    );
                    transition.setTransitionProbability(chance * T8);
                    list.add(transition);
                }
                {
                    CycleControlledStateTransition transition = new CycleControlledStateTransition();
                    transition.spell = Pyro;
                    transition.targetState = getState(
                            Math.max(0f, s.scorchDuration - castTime),
                            Math.max(0f, s.livingBombDuration - castTime),
                            hsc,
                            pd2,
                            pr2,
                            T10 ? Math.max(0.0f, 5.0f - Pyro.castTime) : 0f
                    );
                    transition.setTransitionProbability(chance * (1 - T8));
                    list.add(transition);
                }
            }
        } else {
            float castTime = FB.castTime;
            int ticksLeft = Math.max(0, (int) (s.livingBombDuration / 3.0f));
            int ticksLeftAfter = Math.max(0, (int) ((s.livingBombDuration - castTime) / 3.0f));
            int hotStreakEvents = ticksLeft - ticksLeftAfter;
            //if (!livingBombGlyph)
            {
                hotStreakEvents = 0;
            }
            boolean explosion = false;
            if (ticksLeft > 0 && ticksLeftAfter == 0) {
                explosion = true;
                hotStreakEvents++;
            }
            for (int i = 0; i < (1 << (hotStreakEvents + 1)); i++) {
                int k = i;
                int hsc = s.hotStreakCount;
                float pd = Math.max(0f, s.pyroDuration - castTime);
                boolean pr = s.pyroDuration > castTime;
                float chance = 1.0f;
                for (int j = 0; j < hotStreakEvents; j++) {
                    float crit = (j == hotStreakEvents - 1 && explosion) ? LB.critRate : lBDotCritRate;
                    if (k % 2 == 1) {
                        chance *= crit;
                        if (hsc == 1) {
                            pd = 10.0f - (s.livingBombDuration - ticksLeft * 3);
                            pr = true;
                        }
                        hsc = (hsc + 1) % 2;
                    } else {
                        chance *= (1 - crit);
                        hsc = 0;
                    }
                    k = k >> 1;
                }
                if (k % 2 == 1) {
                    chance *= FB.critRate;
                    if (hsc == 1) {
                        pd = 10.0f;
                    }
                    hsc = (hsc + 1) % 2;
                } else {
                    chance *= (1 - FB.critRate);
                    hsc = 0;
                }
                CycleControlledStateTransition transition = new CycleControlledStateTransition();
                transition.spell = FB;
                transition.targetState = getState(
                        Math.max(0f, s.scorchDuration - castTime),
                        Math.max(0f, s.livingBombDuration - castTime),
                        hsc,
                        pd,
                        pr,
                        Math.max(0.0f, s.tier10TwoPieceDuration - FB.castTime)
                );
                transition.setTransitionProbability(chance);
                list.add(transition);
            }
        }

        return list;
    }

    private Map<String, State> stateDictionary = new LinkedHashMap<String, State>();

    private State getState(float scorchDuration, float livingBombDuration, int hotStreakCount, float pyroDuration, boolean pyroRegistered, float tier10TwoPieceDuration) {
        String name = String.format(Locale.ROOT, "Sc%s,LB%s,HS%s,Pyro%s%s,2T10=%s", scorchDuration, livingBombDuration, hotStreakCount, pyroDuration, pyroRegistered ? "+" : "-", tier10TwoPieceDuration);
        State state;
        if (!stateDictionary.containsKey(name)) {
            state = new State();
            state.name = name;
            state.scorchDuration = scorchDuration;
            state.livingBombDuration = livingBombDuration;
            state.hotStreakCount = hotStreakCount;
            state.pyroDuration = pyroDuration;
            state.pyroRegistered = pyroRegistered;
            state.tier10TwoPieceDuration = tier10TwoPieceDuration;
            stateDictionary.put(name, state);
        } else {
            state = stateDictionary.get(name);
        }
        return state;
    }

    @Override
    protected boolean canStatesBeDistinguished(CycleState state1, CycleState state2) {
        State a = (State) state1;
        State b = (State) state2;
        return (a.scorchDuration != b.scorchDuration || a.livingBombDuration != b.livingBombDuration || a.hotStreakCount != b.hotStreakCount || a.pyroDuration != b.pyroDuration || a.pyroRegistered != b.pyroRegistered || a.tier10TwoPieceDuration != b.tier10TwoPieceDuration);
    }
}
