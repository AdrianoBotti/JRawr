// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/SpecialEffect.cs and
// legacy/RAWR_WotLK_3.3.5a/Rawr.Base/SpecialEffectCombination.cs and
// legacy/RAWR_WotLK_3.3.5a/Rawr.Base/SpecialEffectInterpolator.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: GetUptimePlot and ToString are omitted (not needed by the ported models - HealPriest
// only uses ToString for the ManaSource label of the "MP5 Sources" chart, which is UI-only); the
// AccumulateAverageStats/GetAverageStats 5/4-arg pair needed by RestoSham and the
// GetAverageStats()/GetAverageFactor overload chains needed by HealPriest are ported below.
// CombinationEffectMode is fixed to the default 1 (IbetaInterpolated); the setting was 1
// in GeneralSettings.settings.
package rawr.base.effects;

import rawr.base.bosses.Attack;
import rawr.base.character.Buff;
import rawr.base.items.Enchant;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.stats.AdditiveStat;
import rawr.base.stats.MultiplicativeStat;
import rawr.base.stats.Stats;
import rawr.base.stats.WeightedStat;
import rawr.base.util.NetFormat;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A proc, on-use or conditional item/buff effect: "when X happens, there is a chance P that you gain
 * these stats for D seconds, at most once every C seconds, stacking up to N times".
 *
 * <p>Examples: a trinket that grants 500 spell power for 10 s when your spells land a critical
 * strike, once per 45 s; an on-use trinket activated every 2 minutes; a weapon enchant that stacks
 * agility up to 5 times. Because such effects depend on how often the character acts, they cannot be
 * collapsed into a constant stat value when gear is added up. Instead they are stored in the
 * {@link Stats#getRawSpecialEffectData()} list and averaged later, once the model knows the fight
 * length and the character's action rate.</p>
 *
 * <h2>Fields</h2>
 *
 * <ul>
 *   <li>{@link #getTrigger()} &mdash; the event that can trigger the effect
 *       ({@link Trigger#MeleeHit}, {@link Trigger#SpellCrit}, {@link Trigger#Use}, ...). The model
 *       decides the event's frequency.</li>
 *   <li>{@link #getStats()} &mdash; the stat bonus granted while active (for a stacking effect, the
 *       per-stack bonus).</li>
 *   <li>{@link #getDuration()} &mdash; how long the bonus lasts in seconds. A duration of 0 means
 *       the effect is instantaneous (e.g. an extra damage proc) and is valued per proc instead of
 *       per uptime.</li>
 *   <li>{@link #getCooldown()} &mdash; the internal cooldown: the effect cannot trigger again for
 *       this many seconds (may be infinite for "once per fight" effects).</li>
 *   <li>{@link #getChance()} &mdash; the probability per triggering event. A <b>negative</b> value
 *       encodes a PPM (procs per minute) rate: {@link #getChance(float)} converts it to a
 *       per-attack chance using {@code ppm / 60 * attackSpeed}.</li>
 *   <li>{@link #getMaxStack()} &mdash; number of stacks (1 = not stacking; average stack size is
 *       computed by {@link #getAverageStackSize}).</li>
 * </ul>
 *
 * <h2>How the average value is computed</h2>
 *
 * <p>{@link #accumulateAverageStats(Stats, float, float, float, float)} takes four context values
 * supplied by the model &mdash; <i>triggerInterval</i> (seconds between triggering events),
 * <i>triggerChance</i> (probability that an event actually triggers), <i>attackSpeed</i> and
 * <i>fightDuration</i> &mdash; and adds {@code bonus * factor} into the target stats, where the
 * factor is one of:</p>
 *
 * <ul>
 *   <li><b>Uptime</b> (duration &gt; 0, one stack) &mdash; the expected fraction of the fight the
 *       bonus is active. Special cases:
 *       <ul>
 *         <li>{@code cooldown <= duration}: the proc refreshes while active; computed as an
 *             independent-events probability.</li>
 *         <li>{@code cooldown > duration}: a true renewal process; for short fights
 *             ({@code fightDuration < 10*cooldown}) the exact answer comes from an integral of the
 *             incomplete beta function ({@link SpecialFunction#ibeta}) in
 *             {@link CalculationMode#Advanced}, or from a precomputed interpolation grid in
 *             {@link CalculationMode#Interpolation}. Otherwise a closed-form approximation with a
 *             discretization correction is used.</li>
 *         <li>{@code triggerInterval == 0}: the effect is used on cooldown (on-use trinkets).</li>
 *       </ul>
 *   </li>
 *   <li><b>Procs per second</b> (duration == 0) &mdash; how many times per second the effect fires;
 *       the bonus is credited per proc.</li>
 *   <li><b>Average stack size</b> (maxStack &gt; 1) &mdash; the expected number of active stacks,
 *       including stack-reset behavior ({@link #getAverageStackSize(float, float, float, float, int)}).</li>
 * </ul>
 *
 * <p>Overloads of {@code getAverageStats}/{@code getAverageUptime}/{@code getAverageFactor} fill in
 * defaults for the context arguments; the zero-argument forms assume a 1.0 trigger chance, a 3.0 s
 * attack speed and an infinite fight.</p>
 *
 * <h2>Overlapping procs and combined uptimes</h2>
 *
 * <p>When several effects can be active at once, the value contributed is not simply the sum of
 * independent averages. {@link #getAverageCombinedUptime(SpecialEffect[], float[], float[], float[], float, float)}
 * computes the expected total active time of a set of effects, and the
 * {@code getAverageCombinedUptimeCombinations*} methods return the full probability distribution
 * (a {@link WeightedStat}[] of value/chance pairs) that models can use to average nonlinear
 * bonuses correctly. {@link #getAverageStackingCritRate} handles crit granted by a stacking proc.</p>
 *
 * <h2>Caching and thread safety</h2>
 *
 * <p>Each effect owns a per-fight-duration interpolation cache ({@code interpolator}). The cache is
 * only valid for one calculation context, so a top-level calculation must clear it first
 * ({@link #clearInterpolatorCaches()} / {@link #clearInterpolatorCache()}); otherwise a later
 * character could reuse a grid built with earlier inputs. Model-owned static effects must clear
 * their own cache explicitly. The cache is a {@link ConcurrentHashMap}; the rest of the class is
 * immutable after construction.</p>
 *
 * @see Trigger
 * @see SpecialFunction
 */
public class SpecialEffect {
    private Trigger trigger;
    private Stats stats;
    private float duration;
    private float cooldown;
    private float chance;
    private int maxStack;

    public Trigger getTrigger() {
        return trigger;
    }

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public float getCooldown() {
        return cooldown;
    }

    public void setCooldown(float cooldown) {
        this.cooldown = cooldown;
    }

    public float getChance() {
        return chance;
    }

    public void setChance(float chance) {
        this.chance = chance;
    }

    public int getMaxStack() {
        return maxStack;
    }

    public void setMaxStack(int maxStack) {
        this.maxStack = maxStack;
    }

    private final ConcurrentHashMap<Float, Interpolator> interpolator = new ConcurrentHashMap<>();

    public static void clearInterpolatorCaches() {
        for (Item item : ItemCache.allItems()) clearInterpolatorCache(item.getStats());
        for (Buff buff : Buff.getAllBuffs()) clearInterpolatorCache(buff.stats);
        for (Enchant enchant : Enchant.getAllEnchants()) clearInterpolatorCache(enchant.stats);
    }

    private static void clearInterpolatorCache(Stats stats) {
        if (stats == null || stats.getRawSpecialEffectData() == null) return;
        for (SpecialEffect effect : stats.getRawSpecialEffectData()) {
            if (effect != null) {
                effect.interpolator.clear();
            }
        }
    }

    /** Clears this effect's adaptive interpolation grid cache (model-owned static effects). */
    public void clearInterpolatorCache() {
        interpolator.clear();
    }

    public enum CalculationMode {
        Simple,
        Advanced,
        Interpolation
    }

    public enum CombinationMode {
        Linear,
        Interpolated,
        Exact
    }

    private static volatile CalculationMode mode = CalculationMode.Interpolation;
    private static volatile CombinationMode combinationMode = CombinationMode.Interpolated;

    static {
        updateCalculationMode();
    }

    public static void updateCalculationMode() {
        // The original read GeneralSettings.ProcEffectMode, whose default is 3; the port keeps
        // that default until the global settings service applies the user's choice.
        mode = CalculationMode.Interpolation;
    }

    public static CalculationMode getCalculationMode() {
        return mode;
    }

    public static void setCalculationMode(CalculationMode value) {
        mode = value == null ? CalculationMode.Interpolation : value;
    }

    public static CombinationMode getCombinationMode() {
        return combinationMode;
    }

    public static void setCombinationMode(CombinationMode value) {
        combinationMode = value == null ? CombinationMode.Interpolated : value;
    }

    public SpecialEffect() {
        chance = 1.0f;
        maxStack = 1;
    }

    public SpecialEffect(Trigger trigger, Stats stats, float duration, float cooldown) {
        chance = 1.0f;
        maxStack = 1;
        this.trigger = trigger;
        this.duration = duration;
        this.cooldown = cooldown;
        this.stats = stats;
    }

    public SpecialEffect(Trigger trigger, Stats stats, float duration, float cooldown, float chance) {
        this.chance = chance;
        maxStack = 1;
        this.trigger = trigger;
        this.duration = duration;
        this.cooldown = cooldown;
        this.stats = stats;
    }

    public SpecialEffect(Trigger trigger, Stats stats, float duration, float cooldown, float chance, int maxStack) {
        this.chance = chance;
        this.maxStack = maxStack;
        this.trigger = trigger;
        this.duration = duration;
        this.cooldown = cooldown;
        this.stats = stats;
    }

    public boolean usesPpm() {
        return chance < 0;
    }

    public float getChance(float attackspeed) {
        if (chance < 0) {
            return -chance / 60f * attackspeed;
        } else return chance;
    }

    public float getAverageUptime(float triggerInterval, float triggerChance) {
        return getAverageUptime(triggerInterval, triggerChance, 3f, 0f);
    }

    public float getAverageUptime(float triggerInterval, float triggerChance, float attackSpeed) {
        return getAverageUptime(triggerInterval, triggerChance, attackSpeed, 0f);
    }

    public float getAverageUptime(float triggerInterval, float triggerChance, float attackSpeed, float fightDuration) {
        boolean discretizationCorrection = true;
        if (triggerChance == 0f || triggerInterval == Float.POSITIVE_INFINITY) {
            return 0f;
        }

        if (cooldown > duration) {
            if (fightDuration == 0f) {
                if (discretizationCorrection) {
                    return duration / (cooldown - triggerInterval / 2 + triggerInterval / triggerChance / getChance(attackSpeed));
                } else {
                    return duration / (cooldown - triggerInterval + triggerInterval / triggerChance / getChance(attackSpeed));
                }
            } else {
                boolean needsBeta = fightDuration < 10 * cooldown;
                if (triggerInterval > 0 && mode == CalculationMode.Advanced && needsBeta) {
                    return (float) averageUptimeAdvanced(duration, cooldown, triggerInterval, fightDuration, triggerChance * getChance(attackSpeed), discretizationCorrection);
                } else if (triggerInterval > 0 && mode == CalculationMode.Interpolation && needsBeta) {
                    Interpolator i = interpolator.computeIfAbsent(fightDuration,
                            duration -> new UptimeInterpolator(this, duration));
                    return i.get(triggerChance * getChance(attackSpeed), triggerInterval);
                } else if (triggerInterval == 0.0f) {
                    float t = fightDuration;
                    float cc = Float.isInfinite(cooldown) && cooldown > 0 ? fightDuration : cooldown;
                    float total = (float) Math.floor(t / cc) * duration;
                    t -= (float) Math.floor(t / cc) * cc;
                    total += Math.min(t, duration);
                    return total / fightDuration;
                } else {
                    double d = duration / triggerInterval;
                    double n = fightDuration / triggerInterval;
                    double p = triggerChance * getChance(attackSpeed);

                    double c = cooldown / triggerInterval;
                    if (discretizationCorrection) {
                        c += 0.5;
                    }
                    if (c < 1.0) c = 1.0;
                    double K = 1.0 / p + c - 1.0;
                    double ppt = (1 + c / n * (c - 1) / (2 * K)) / K;
                    return (float) (ppt * d);
                }
            }
        } else if (cooldown == 0.0f) {
            if (triggerInterval >= duration) return duration / triggerInterval * getChance(attackSpeed) * triggerChance;
            else return 1.0f - (float) Math.pow(1f - triggerChance * getChance(attackSpeed), duration / triggerInterval);
        } else {
            return 1.0f - 1.0f / (1.0f + cooldown / triggerInterval * triggerChance * getChance(attackSpeed)) * (float) Math.pow(1f - triggerChance * getChance(attackSpeed), (duration - cooldown) / triggerInterval);
        }
    }

    // C# SpecialEffect.cs:491-506.
    public void accumulateAverageStats(Stats stats, float triggerInterval, float triggerChance, float attackSpeed, float fightDuration) {
        this.stats.generateSparseData();
        if (maxStack > 1) {
            stats.accumulate(this.stats, getAverageStackSize(triggerInterval, triggerChance, attackSpeed, fightDuration));
        } else if (duration == 0f) {
            stats.accumulate(this.stats, getAverageProcsPerSecond(triggerInterval, triggerChance, attackSpeed, fightDuration));
        } else {
            stats.accumulate(this.stats, getAverageUptime(triggerInterval, triggerChance, attackSpeed, fightDuration));
        }
    }

    // C# SpecialEffect.cs:474-479.
    public Stats getAverageStats(float triggerInterval, float triggerChance, float attackSpeed, float fightDuration) {
        Stats stats = new Stats();
        accumulateAverageStats(stats, triggerInterval, triggerChance, attackSpeed, fightDuration);
        return stats;
    }

    // C# SpecialEffect.cs:389-392/408-411/433-436/460-463 overload chain.
    public Stats getAverageStats() {
        return getAverageStats(0.0f);
    }

    public Stats getAverageStats(float triggerInterval) {
        return getAverageStats(triggerInterval, 1.0f, 3.0f, 0.0f);
    }

    public Stats getAverageStats(float triggerInterval, float triggerChance) {
        return getAverageStats(triggerInterval, triggerChance, 3.0f, 0.0f);
    }

    public Stats getAverageStats(float triggerInterval, float triggerChance, float attackSpeed) {
        return getAverageStats(triggerInterval, triggerChance, attackSpeed, 0.0f);
    }

    // C# SpecialEffect.cs:548-596 GetAverageFactor overload chain.
    public float getAverageFactor(float triggerInterval, float triggerChance) {
        return getAverageFactor(triggerInterval, triggerChance, 3.0f, 0.0f, 1.0f);
    }

    public float getAverageFactor(float triggerInterval, float triggerChance, float attackSpeed, float fightDuration) {
        return getAverageFactor(triggerInterval, triggerChance, attackSpeed, fightDuration, 1.0f);
    }

    public float getAverageFactor(float triggerInterval, float triggerChance, float attackSpeed, float fightDuration, float scale) {
        float factor;
        if (maxStack > 1) {
            factor = getAverageStackSize(triggerInterval, triggerChance, attackSpeed, fightDuration);
        } else if (duration == 0f) {
            factor = getAverageProcsPerSecond(triggerInterval, triggerChance, attackSpeed, fightDuration);
        } else {
            factor = getAverageUptime(triggerInterval, triggerChance, attackSpeed, fightDuration);
        }
        factor *= scale;
        return factor;
    }

    public float getAverageProcsPerSecond(float triggerInterval, float triggerChance, float attackSpeed, float fightDuration) {
        if (triggerChance == 0f || triggerInterval == Float.POSITIVE_INFINITY) {
            return 0f;
        }

        boolean discretizationCorrection = true;

        if (fightDuration == 0.0f) {
            float pp = triggerChance * getChance(attackSpeed);
            if (pp == 0.0) return 0.0f;
            if (discretizationCorrection) {
                return 1.0f / (cooldown - triggerInterval / 2 + triggerInterval / pp);
            } else {
                return 1.0f / (cooldown - triggerInterval + triggerInterval / pp);
            }
        }
        if (triggerInterval == 0.0f) {
            return (1.0f + (float) Math.floor(fightDuration / cooldown)) / fightDuration;
        }

        boolean needsBeta = fightDuration < cooldown * 10;
        if (mode == CalculationMode.Advanced && triggerInterval > 0 && needsBeta) {
            return (float) averageProcsAdvanced(cooldown, triggerInterval, fightDuration, triggerChance * getChance(attackSpeed), discretizationCorrection);
        } else if (mode == CalculationMode.Interpolation && triggerInterval > 0 && needsBeta) {
            Interpolator i = interpolator.computeIfAbsent(fightDuration,
                    duration -> new ProcsPerSecondInterpolator(this, duration));
            return i.get(triggerChance * getChance(attackSpeed), triggerInterval);
        } else {
            double d = duration / triggerInterval;
            double n = fightDuration / triggerInterval;
            double p = triggerChance * getChance(attackSpeed);

            double c = cooldown / triggerInterval;
            if (discretizationCorrection) {
                c += 0.5;
            }
            if (c < 1.0) c = 1.0;
            double K = 1.0 / p + c - 1.0;
            double ppt = (1 + c / n * (c - 1) / (2 * K)) / K;
            return (float) (ppt / triggerInterval);
        }
    }

    public float getAverageStackSize(float triggerInterval, float triggerChance, float attackSpeed, float fightDuration) {
        return getAverageStackSize(triggerInterval, triggerChance, attackSpeed, fightDuration, 1);
    }

    public float getAverageStackSize(float triggerInterval, float triggerChance, float attackSpeed, float fightDuration, int stackReset) {
        float averageStack = 0;
        if ((maxStack > 1) && (cooldown == 0f)) {
            float p = triggerChance * getChance(attackSpeed);
            float q = 1.0f - p;
            float Q = (float) Math.pow(q, duration / triggerInterval);
            float probToStack = 1.0f - Q;

            if (probToStack > 0.99f) {
                if (fightDuration == 0.0f)
                    averageStack = maxStack;
                else {
                    float buildTime = triggerInterval / triggerChance * maxStack * stackReset;
                    float value;
                    if (fightDuration >= buildTime) {
                        value = buildTime * (maxStack - 1) / 2 + (fightDuration - buildTime) * maxStack;
                    } else {
                        float singleBuild = buildTime / stackReset;
                        int fullBuilds = (int) (fightDuration / singleBuild);
                        value = fullBuilds * singleBuild * (maxStack - 1) / 2;
                        float remaining = fightDuration - fullBuilds * singleBuild;
                        float remainingStacks = remaining / (triggerInterval / triggerChance);
                        value += remaining * Math.max(0, remainingStacks - 1) / 2;
                    }

                    averageStack = value / fightDuration;
                }
            } else if (probToStack > 0) {
                averageStack = probToStack / Q * (1.0f - (float) Math.pow(probToStack, maxStack));
            } else {
                averageStack = duration / (triggerInterval / triggerChance);
            }
            return averageStack;
        } else
            return getAverageUptime(triggerInterval, triggerChance, attackSpeed, fightDuration);
    }

    public static float getAverageStackingCritRate(float triggerInterval, float duration, float hitRate, float baseCritRate, float bonusCritRate, float stackCritRate, int maxStack) {
        if (maxStack <= 0) {
            return Math.min(1.0f, baseCritRate + bonusCritRate) - baseCritRate;
        }

        float[] C = new float[maxStack + 1];
        float[] B = new float[maxStack + 1];
        float[] S = new float[maxStack + 1];
        S[0] = 1.0f;
        for (int j = 0; j <= maxStack; j++) {
            C[j] = hitRate * Math.min(1.0f, baseCritRate + bonusCritRate + j * stackCritRate);
            B[j] = Math.min(1.0f, baseCritRate + bonusCritRate + j * stackCritRate) - baseCritRate;
        }
        int N = (int) (duration / triggerInterval);
        float average = 0.0f;
        for (int i = 0; i < N; i++) {
            average += S[maxStack] * B[maxStack];
            S[maxStack] += S[maxStack - 1] * C[maxStack - 1];
            for (int j = maxStack - 1; j > 0; j--) {
                average += S[j] * B[j];
                S[j] = S[j] * (1.0f - C[j]) + S[j - 1] * C[j - 1];
            }
            average += S[0] * B[0];
            S[0] -= S[0] * C[0];
        }
        return average / N;
    }

    private static final int MAX_RECURSION_DEPTH = 20;

    @FunctionalInterface
    private interface IbetaDelegate {
        float apply(int a, float b, float x);
    }

    private static class Parameters {
        SpecialEffect[] effects;
        float[] d;
        float[] p;
        float[] c;
        float[] o;
        float[] k;
        float[] v;
        float[] triggerInterval;
        float[] uptime;
        float[][] combinedUptime;
        float[][] partialIntegral;
        int N;
        int NC;
        IbetaDelegate Ibeta;
        int I;

        Parameters(SpecialEffect[] effects, float[] triggerInterval, float[] triggerChance, float[] offset, float attackSpeed) {
            this(effects, triggerInterval, triggerChance, offset, attackSpeed, null, null, 0.0f);
        }

        Parameters(SpecialEffect[] effects, float[] triggerInterval, float[] triggerChance, float[] offset, float attackSpeed, float[] scale, float[] value, float fightDuration) {
            d = new float[effects.length];
            p = new float[effects.length];
            c = new float[effects.length];
            o = new float[effects.length];
            k = new float[effects.length];
            v = new float[effects.length];
            this.effects = new SpecialEffect[effects.length];
            this.triggerInterval = new float[effects.length];

            boolean discretizationCorrection = true;

            int j = 0;
            for (int i = 0; i < effects.length; i++) {
                if (effects[i].getCooldown() > effects[i].getDuration()) {
                    I = i;
                    this.effects[j] = effects[i];
                    this.triggerInterval[j] = triggerInterval[i];
                    if (scale == null) {
                        k[j] = 1.0f;
                    } else {
                        k[j] = scale[i];
                    }
                    if (value != null) {
                        v[j] = value[i];
                    }
                    p[j] = triggerChance[i] * effects[i].getChance(attackSpeed);
                    if (triggerInterval[i] == 0.0f) {
                        d[j] = effects[i].getDuration();
                        c[j] = effects[i].getCooldown();
                        o[j] = offset[i];
                    } else {
                        d[j] = effects[i].getDuration() / triggerInterval[i];
                        c[j] = effects[i].getCooldown() / triggerInterval[i];
                        if (discretizationCorrection) {
                            c[j] += 0.5f;
                        }
                        if (c[j] < 1.0f) c[j] = 0.0f;
                        o[j] = offset[i] / triggerInterval[i];
                    }
                    j++;
                }
            }

            N = j;
            NC = (1 << N);

            uptime = new float[effects.length];
            combinedUptime = new float[5 + 2 * MAX_RECURSION_DEPTH][NC];
            partialIntegral = new float[5 + 2 * MAX_RECURSION_DEPTH][NC];

            for (int i = 0; i < effects.length; i++) {
                if (effects[i].getCooldown() <= effects[i].getDuration()) {
                    this.effects[j] = effects[i];
                    if (value != null) {
                        v[j] = value[i];
                    }
                    uptime[j] = effects[i].getAverageUptime(triggerInterval[i], triggerChance[i], attackSpeed, fightDuration);
                    if (scale != null) {
                        uptime[j] *= scale[i];
                    }
                    j++;
                }
            }

            Ibeta = switch (combinationMode) {
                case Linear -> SpecialFunction::ibetaInterpolatedLinear;
                case Exact -> SpecialFunction::ibeta;
                default -> SpecialFunction::ibetaInterpolated;
            };
        }
    }

    public static float getAverageCombinedUptime(SpecialEffect[] effects, float[] triggerInterval, float[] triggerChance, float[] offset, float attackSpeed, float fightDuration) {
        Parameters p = new Parameters(effects, triggerInterval, triggerChance, offset, attackSpeed);

        double totalCombinedUptime = adaptiveSimpsonsMethod(p, fightDuration, 0.001f, 20);

        return (float) (totalCombinedUptime / fightDuration);
    }

    public static WeightedStat[] getAverageCombinedUptimeCombinations(SpecialEffect[] effects, float[] triggerInterval, float[] triggerChance, float[] offset, float attackSpeed, float fightDuration, AdditiveStat stat) {
        return getAverageCombinedUptimeCombinations(effects, triggerInterval, triggerChance, offset, null, attackSpeed, fightDuration, stat);
    }

    public static WeightedStat[] getAverageCombinedUptimeCombinations(SpecialEffect[] effects, float[] triggerInterval, float[] triggerChance, float[] offset, float[] scale, float attackSpeed, float fightDuration, AdditiveStat stat) {
        float[] value = new float[effects.length];
        for (int j = 0; j < effects.length; j++) {
            value[j] = effects[j].getStats().getRawAdditiveData()[stat.value()];
        }
        return getAverageCombinedUptimeCombinations(effects, triggerInterval, triggerChance, offset, scale, attackSpeed, fightDuration, value);
    }

    public static WeightedStat[] getAverageCombinedUptimeCombinations(SpecialEffect[] effects, float[] triggerInterval, float[] triggerChance, float[] offset, float[] scale, float attackSpeed, float fightDuration, float[] value) {
        Parameters p = new Parameters(effects, triggerInterval, triggerChance, offset, attackSpeed, scale, value, fightDuration);

        if (p.N > 1) {
            adaptiveSimpsonsMethodCombinations(p, fightDuration, 0.001f, MAX_RECURSION_DEPTH);
        } else if (p.N == 1) {
            p.partialIntegral[0][1] = fightDuration * p.k[0] * p.effects[0].getAverageUptime(triggerInterval[p.I], triggerChance[p.I], attackSpeed, fightDuration);
            p.partialIntegral[0][0] = fightDuration - p.partialIntegral[0][1];
        }

        WeightedStat[] result = new WeightedStat[1 << effects.length];

        int mask = p.NC - 1;
        for (int i = 0; i < (1 << effects.length); i++) {
            float chance;
            if (p.N > 0) {
                chance = p.partialIntegral[0][i & mask] / fightDuration;
            } else {
                chance = 1.0f;
            }
            float combinedValue = 0f;
            for (int j = 0; j < effects.length; j++) {
                if ((i & (1 << j)) != 0) {
                    combinedValue += p.v[j];
                    if (j >= p.N) {
                        chance *= p.uptime[j];
                    }
                } else {
                    if (j >= p.N) {
                        chance *= (1.0f - p.uptime[j]);
                    }
                }
            }
            result[i] = new WeightedStat(chance, combinedValue);
        }

        return result;
    }

    public static WeightedStat[] getAverageCombinedUptimeCombinationsMultiplicative(SpecialEffect[] effects, float[] triggerInterval, float[] triggerChance, float[] offset, float attackSpeed, float fightDuration, MultiplicativeStat stat) {
        return getAverageCombinedUptimeCombinationsMultiplicative(effects, triggerInterval, triggerChance, offset, null, attackSpeed, fightDuration, stat);
    }

    public static WeightedStat[] getAverageCombinedUptimeCombinationsMultiplicative(SpecialEffect[] effects, float[] triggerInterval, float[] triggerChance, float[] offset, float[] scale, float attackSpeed, float fightDuration, MultiplicativeStat stat) {
        float[] value = new float[effects.length];
        for (int j = 0; j < effects.length; j++) {
            value[j] = effects[j].getStats().getRawMultiplicativeData()[stat.value()];
        }
        return getAverageCombinedUptimeCombinationsMultiplicative(effects, triggerInterval, triggerChance, offset, scale, attackSpeed, fightDuration, value);
    }

    public static WeightedStat[] getAverageCombinedUptimeCombinationsMultiplicative(SpecialEffect[] effects, float[] triggerInterval, float[] triggerChance, float[] offset, float[] scale, float attackSpeed, float fightDuration, float[] value) {
        Parameters p = new Parameters(effects, triggerInterval, triggerChance, offset, attackSpeed, scale, value, fightDuration);

        if (p.N > 1) {
            adaptiveSimpsonsMethodCombinations(p, fightDuration, 0.001f, MAX_RECURSION_DEPTH);
        } else if (p.N == 1) {
            p.partialIntegral[0][1] = fightDuration * p.k[0] * p.effects[0].getAverageUptime(triggerInterval[p.I], triggerChance[p.I], attackSpeed, fightDuration);
            p.partialIntegral[0][0] = fightDuration - p.partialIntegral[0][1];
        }

        WeightedStat[] result = new WeightedStat[1 << effects.length];

        int mask = p.NC - 1;
        for (int i = 0; i < (1 << effects.length); i++) {
            float chance;
            if (p.N > 0) {
                chance = p.partialIntegral[0][i & mask] / fightDuration;
            } else {
                chance = 1.0f;
            }
            float combinedValue = 0f;
            for (int j = 0; j < effects.length; j++) {
                if ((i & (1 << j)) != 0) {
                    combinedValue = (1 + combinedValue) * (1 + p.v[j]) - 1;
                    if (j >= p.N) {
                        chance *= p.uptime[j];
                    }
                } else {
                    if (j >= p.N) {
                        chance *= (1.0f - p.uptime[j]);
                    }
                }
            }
            result[i] = new WeightedStat(chance, combinedValue);
        }

        return result;
    }

    private static float adaptiveSimpsonsAux(Parameters p, float a, float b, float epsilon, float S, float fa, float fb, float fc, int bottom) {
        float c = (a + b) / 2;
        float h = b - a;
        float d = (a + c) / 2;
        float e = (c + b) / 2;
        float fd = getCombinedUptime(p, d);
        float fe = getCombinedUptime(p, e);
        float Sleft = (h / 12) * (fa + 4 * fd + fc);
        float Sright = (h / 12) * (fc + 4 * fe + fb);
        float S2 = Sleft + Sright;
        if (bottom <= 0 || (h < 10 && Math.abs(S2 - S) <= 15 * epsilon)) {
            return S2 + (S2 - S) / 15;
        }
        return adaptiveSimpsonsAux(p, a, c, epsilon / 2, Sleft, fa, fc, fd, bottom - 1) +
                adaptiveSimpsonsAux(p, c, b, epsilon / 2, Sright, fc, fb, fe, bottom - 1);
    }

    private static void adaptiveSimpsonsAuxCombinations(Parameters p, float a, float b, float epsilon, int S, int fa, int fb, int fc, int bottom, int level) {
        float c = (a + b) / 2;
        float h = b - a;
        float d = (a + c) / 2;
        float e = (c + b) / 2;
        int fd = 1 + 2 * level;
        getCombinedUptimeCombinations(p, d, fd);
        int fe = 2 + 2 * level;
        getCombinedUptimeCombinations(p, e, fe);
        int Sleft = 2 * level - 1;
        int Sright = 2 * level;
        boolean terminate = true;
        for (int i = 0; i < p.NC; i++) {
            p.partialIntegral[Sleft][i] = (h / 12) * (p.combinedUptime[fa][i] + 4 * p.combinedUptime[fd][i] + p.combinedUptime[fc][i]);
            p.partialIntegral[Sright][i] = (h / 12) * (p.combinedUptime[fc][i] + 4 * p.combinedUptime[fe][i] + p.combinedUptime[fb][i]);
            if (!(bottom <= 0 || (h < 10 && Math.abs(p.partialIntegral[Sleft][i] + p.partialIntegral[Sright][i] - p.partialIntegral[S][i]) <= 15 * epsilon))) {
                terminate = false;
            }
        }
        if (terminate) {
            for (int i = 0; i < p.NC; i++) {
                p.partialIntegral[S][i] = (p.partialIntegral[Sleft][i] + p.partialIntegral[Sright][i]) * 16.0f / 15.0f - p.partialIntegral[S][i] / 15.0f;
            }
        } else {
            adaptiveSimpsonsAuxCombinations(p, a, c, epsilon / 2, Sleft, fa, fc, fd, bottom - 1, level + 1);
            adaptiveSimpsonsAuxCombinations(p, c, b, epsilon / 2, Sright, fc, fb, fe, bottom - 1, level + 1);
            for (int i = 0; i < p.NC; i++) {
                p.partialIntegral[S][i] = p.partialIntegral[Sleft][i] + p.partialIntegral[Sright][i];
            }
        }
    }

    private static float adaptiveSimpsonsMethod(Parameters p, float fightDuration, float epsilon, int maxRecursionDepth) {
        float a = 0.0f;
        float b = fightDuration;
        float c = (a + b) / 2;
        float h = b - a;
        float fa = getCombinedUptime(p, a);
        float fb = getCombinedUptime(p, b);
        float fc = getCombinedUptime(p, c);
        float S = (h / 6) * (fa + 4 * fc + fb);
        return adaptiveSimpsonsAux(p, a, b, epsilon, S, fa, fb, fc, maxRecursionDepth);
    }

    private static void adaptiveSimpsonsMethodCombinations(Parameters p, float fightDuration, float epsilon, int maxRecursionDepth) {
        float a = 0.0f;
        float b = fightDuration;
        float c = (a + b) / 2;
        float h = b - a;
        int fa = 0;
        getCombinedUptimeCombinations(p, a, fa);
        int fb = 1;
        getCombinedUptimeCombinations(p, b, fb);
        int fc = 2;
        getCombinedUptimeCombinations(p, c, fc);
        int S = 0;
        for (int i = 0; i < p.NC; i++) {
            p.partialIntegral[0][i] = (h / 6) * (p.combinedUptime[fa][i] + 4 * p.combinedUptime[fc][i] + p.combinedUptime[fb][i]);
        }
        adaptiveSimpsonsAuxCombinations(p, a, b, epsilon, S, fa, fb, fc, maxRecursionDepth, 1);
    }

    private static float getCombinedUptime(Parameters p, float t) {
        float combinedUptime = 1.0f;

        for (int i = 0; i < p.effects.length; i++) {
            float x = t / p.triggerInterval[i] - p.o[i];

            float uptime = 0.0f;
            int r = 1;
            while (x > 0) {
                uptime += p.Ibeta.apply(r, x, p.p[i]);
                float xd = x - p.d[i];
                if (xd > 0) {
                    uptime -= p.Ibeta.apply(r, xd, p.p[i]);
                }
                r++;
                x -= p.c[i];
            }
            combinedUptime *= uptime;
        }

        return combinedUptime;
    }

    private static void getCombinedUptimeCombinations(Parameters p, float t, int index) {
        for (int i = 0; i < p.N; i++) {
            if (p.triggerInterval[i] == 0.0f) {
                float x = t - p.o[i];
                if (x >= 0 && x % p.c[i] < p.d[i]) {
                    p.uptime[i] = 1.0f;
                } else {
                    p.uptime[i] = 0.0f;
                }
            } else {
                float x = t / p.triggerInterval[i] - p.o[i];

                if (p.c[i] == 0.0f) {
                    if (p.d[i] <= 1.0f) {
                        p.uptime[i] = p.d[i] * p.p[i];
                    } else {
                        p.uptime[i] = 1.0f - (float) Math.pow(1f - p.p[i], Math.min(x, p.d[i]));
                    }
                } else {
                    p.uptime[i] = 0.0f;
                    int r = 1;
                    while (x > 0) {
                        p.uptime[i] += p.Ibeta.apply(r, x, p.p[i]);
                        float xd = x - p.d[i];
                        if (xd > 0) {
                            p.uptime[i] -= p.Ibeta.apply(r, xd, p.p[i]);
                        }
                        r++;
                        x -= p.c[i];
                    }
                }
            }
        }

        for (int i = 0; i < p.NC; i++) {
            p.combinedUptime[index][i] = 1.0f;
            for (int j = 0; j < p.N; j++) {
                if ((i & (1 << j)) == 0) {
                    p.combinedUptime[index][i] *= (1.0f - p.uptime[j] * p.k[j]);
                } else {
                    p.combinedUptime[index][i] *= p.uptime[j] * p.k[j];
                }
            }
        }
    }

    public abstract static class Interpolator {
        protected float[][] grid;
        protected float procChanceMin;
        protected float procChanceMax;
        protected static final int procChanceN = 100;
        protected float intervalMin;
        protected float intervalMax;
        protected static final int intervalN = 100;
        protected float fightDuration;
        protected boolean discretizationCorrection;
        protected float procFactor;
        protected float intervalFactor;

        public Interpolator(float fightDuration, boolean discretizationCorrection) {
            this.fightDuration = fightDuration;
            this.discretizationCorrection = discretizationCorrection;
        }

        public float get(float procChance, float interval) {
            boolean updateGrid = false;
            if (grid == null) {
                procChanceMin = 0.5f * procChance;
                procChanceMax = Math.min(1.0f, 1.5f * procChance);
                intervalMin = 0.5f * interval;
                intervalMax = 1.5f * interval;
                grid = new float[procChanceN + 1][intervalN + 1];
                updateGrid = true;
            }
            if (procChance < procChanceMin) {
                procChanceMin = 0.5f * procChance;
                updateGrid = true;
            }
            if (procChance > procChanceMax) {
                procChanceMax = Math.min(1.0f, 1.5f * procChance);
                updateGrid = true;
            }
            if (interval < intervalMin) {
                intervalMin = 0.5f * interval;
                updateGrid = true;
            }
            if (interval >= intervalMax) {
                intervalMax = 1.5f * interval;
                updateGrid = true;
            }
            if (updateGrid) {
                procFactor = procChanceN / (procChanceMax - procChanceMin);
                intervalFactor = intervalN / (intervalMax - intervalMin);
                updateGrid();
            }
            float x = (procChance - procChanceMin) * procFactor;
            int i = (int) x;
            x -= i;
            float y = (interval - intervalMin) * intervalFactor;
            int j = (int) y;
            y -= j;
            if (i >= procChanceN) {
                return grid[procChanceN][j] + y * (grid[procChanceN][j + 1] - grid[procChanceN][j]);
            } else {
                float v0 = grid[i][j] + x * (grid[i + 1][j] - grid[i][j]);
                float v1 = grid[i][j + 1] + x * (grid[i + 1][j + 1] - grid[i][j + 1]);
                return v0 + y * (v1 - v0);
            }
        }

        private void updateGrid() {
            for (int i = 0; i <= procChanceN; i++) {
                for (int j = 0; j <= intervalN; j++) {
                    grid[i][j] = evaluate(procChanceMin + (float) i / procChanceN * (procChanceMax - procChanceMin), intervalMin + (float) j / intervalN * (intervalMax - intervalMin));
                }
            }
        }

        protected abstract float evaluate(float procChance, float interval);
    }

    /** Simpson/ibeta integration shared by the direct and interpolated advanced uptime paths. */
    private double averageUptimeAdvanced(float duration, float cooldown, float triggerInterval, float fightDuration, double p, boolean discretizationCorrection) {
        double d = duration / triggerInterval;
        double d2 = d * 0.5;
        double n = fightDuration / triggerInterval;
        double c = cooldown / triggerInterval;
        if (discretizationCorrection) {
            c += 0.5;
        }
        if (c < 1.0) c = 1.0;
        double x = n;

        final double w1 = 5.0 / 9.0;
        final double w2 = 8.0 / 9.0;
        final double k = 0.77459666924148337703585307995648;
        double dx = k * d2;

        double averageUptime = 0.0;
        int r = 1;
        while (x > 0) {
            if (x - d > 0) {
                double tmid = x - d2;
                averageUptime += (w1 * SpecialFunction.ibeta(r, tmid - dx, p) + w2 * SpecialFunction.ibeta(r, tmid, p) + w1 * SpecialFunction.ibeta(r, tmid + dx, p)) * d2;
            } else {
                double tmid = x * 0.5;
                double dt = k * tmid;
                averageUptime += (w1 * SpecialFunction.ibeta(r, tmid - dt, p) + w2 * SpecialFunction.ibeta(r, tmid, p) + w1 * SpecialFunction.ibeta(r, tmid + dt, p)) * tmid;
            }
            r++;
            x -= c;
        }
        return averageUptime / n;
    }

    /** Discrete proc-count integration shared by the direct and interpolated advanced proc paths. */
    private double averageProcsAdvanced(float cooldown, float triggerInterval, float fightDuration, double p, boolean discretizationCorrection) {
        double c = cooldown / triggerInterval;
        if (discretizationCorrection) {
            c += 0.5;
        }
        if (c < 1.0) c = 1.0;
        double n = fightDuration / triggerInterval;
        double x = n;

        double averageProcs = 0.0;
        int r = 1;
        while (x > 0) {
            averageProcs += SpecialFunction.ibeta(r, x, p);
            r++;
            x -= c;
        }
        return averageProcs / fightDuration;
    }

    private class UptimeInterpolator extends Interpolator {
        protected SpecialEffect effect;

        public UptimeInterpolator(SpecialEffect effect, float fightDuration) {
            super(fightDuration, true);
            this.effect = effect;
        }

        @Override
        protected float evaluate(float procChance, float interval) {
            return (float) averageUptimeAdvanced(effect.duration, effect.cooldown, interval, fightDuration, procChance, discretizationCorrection);
        }
    }

    private class ProcsPerSecondInterpolator extends Interpolator {
        protected SpecialEffect effect;

        public ProcsPerSecondInterpolator(SpecialEffect effect, float fightDuration) {
            super(fightDuration, true);
            this.effect = effect;
        }

        @Override
        protected float evaluate(float procChance, float interval) {
            return (float) averageProcsAdvanced(effect.cooldown, interval, fightDuration, procChance, discretizationCorrection);
        }
    }

    // C# SpecialEffect.cs:1518-1670 StackString/DurationString/ChanceString/TriggerString and ToString.
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        if (maxStack > 1) {
            s.append(maxStack).append("x").append(" ");
        }

        s.append(stats.toString());
        s.append(" (");

        boolean needsSpace = false;
        if (duration > 0 && !Float.isInfinite(duration)) {
            s.append(((int) duration)).append(" Sec");
            needsSpace = true;
        }
        if (chance < 1) {
            if (needsSpace) s.append(" ");
            s.append(chanceString());
            needsSpace = true;
        }

        if (trigger != Trigger.Use) {
            if (needsSpace) s.append(" ");
            s.append(triggerString());
            needsSpace = true;
        }
        if (cooldown > 0) {
            if (needsSpace) s.append("/");
            s.append(cooldownString());
        }
        s.append(")");
        return s.toString();
    }

    private String cooldownString() {
        if (Float.isInfinite(cooldown) && cooldown > 0) {
            return "Once per Fight";
        }
        int rounded = (int) cooldown;
        if (rounded % 60 == 0) {
            return (rounded / 60) + " Min";
        }
        return rounded + " Sec";
    }

    private String chanceString() {
        if (chance < 0) {
            return NetFormat.grouped(-chance, 2) + " PPM";
        }
        return NetFormat.grouped(chance * 100, 0) + "%";
    }

    private String triggerString() {
        switch (trigger) {
            case DamageSpellCast:
                return "on Damaging Spell Cast";
            case DamageDone:
                return "on Damage Dealt";
            case DamageSpellCrit:
                return "on Damaging Spell Crit";
            case DamageSpellHit:
                return "on Damaging Spell Hit";
            case HealingSpellCast:
                return "on Healing Cast";
            case HealingSpellCrit:
                return "on Healing Crit";
            case HealingSpellHit:
                return "on Healing Hit";
            case MeleeCrit:
                return "on Melee Crit";
            case MeleeHit:
                return "on Melee Hit";
            case CurrentHandHit:
                return "on Current Hand Melee Hit";
            case MainHandHit:
                return "on Main Hand Melee Hit";
            case OffHandHit:
                return "on Off Hand Melee Hit";
            case MeleeAttack:
                return "on Melee Attack";
            case SpellCast:
                return "on Spell Cast";
            case SpellCrit:
                return "on Spell Crit";
            case SpellHit:
                return "on Spell Hit";
            case SpellMiss:
                return "on Spell Miss";
            case Use:
                return "";
            case PhysicalHit:
                return "on Physical Hit";
            case PhysicalCrit:
                return "on Physical Crit";
            case ManaGem:
                return "on Mana Gem";
            case MageNukeCast:
                return "on Mage Nuke Cast";
            case JudgementHit:
                return "on Judgement";
            case HolyShield:
                return "on Holy Shield cast";
            case HammeroftheRighteous:
                return "on HotR cast";
            case ShieldofRighteousness:
                return "on ShoR cast";
            case CrusaderStrikeHit:
                return "on Crusader Strike";
            case InsectSwarmTick:
                return "on Insect Swarm Tick";
            case ShamanLightningBolt:
                return "on Lightning Bolt Cast";
            case ShamanLavaLash:
                return "on Lava Lash Hit";
            case ShamanShock:
                return "on Shock Hit";
            case ShamanStormStrike:
                return "on Stormstrike Hit";
            case BloodStrikeHit:
                return "on Blood Strike";
            case HeartStrikeHit:
                return "on Heart Strike";
            case BloodStrikeOrHeartStrikeHit:
                return "on Blood Strike or Heart Strike";
            case IcyTouchHit:
                return "on Icy Touch";
            case PlagueStrikeHit:
                return "on Plague Strike";
            case RuneStrikeHit:
                return "on Rune Strike";
            default:
                return trigger.toString();
        }
    }
}
