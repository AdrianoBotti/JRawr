// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Rotation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The C# LinkedHashMap<Type, ...> maps become LinkedHashMap<Class<?>, ...> (insertion
// order, matching Dictionary iteration). Display of doubles uses rawr.models.mage.NetFormat.
package rawr.models.elemental;

import rawr.base.talents.ShamanTalents;
import rawr.models.mage.NetFormat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Rotation {
    private final List<Spell> spells;
    private List<Spell> casts;
    private boolean useDpsFireTotem = false;

    public List<Spell> getCasts() {
        if (casts == null)
            calculateCastSpells();
        return casts;
    }

    private LinkedHashMap<Class<?>, Float> cc = null;

    public LinkedHashMap<Class<?>, Float> getClearCasting() {
        if (cc == null)
            calculateProperties();
        return cc;
    }

    private LinkedHashMap<Class<?>, Float> spelldps = null;

    public LinkedHashMap<Class<?>, Float> getSpellDPS() {
        if (spelldps == null)
            calculateProperties();
        return spelldps;
    }

    private LinkedHashMap<Class<?>, Spell> spelltype = null;

    public LinkedHashMap<Class<?>, Spell> getTypeToSpell() {
        if (spelltype == null)
            calculateProperties();
        return spelltype;
    }

    private float dps = Float.POSITIVE_INFINITY;

    /**
     * Damage per second from spell casts.
     */
    public float getDPS() {
        if (dps == Float.POSITIVE_INFINITY)
            calculateProperties();
        return dps;
    }

    public void setDPS(float value) {
        dps = value;
    }

    private float mps = Float.POSITIVE_INFINITY;

    /**
     * Used Mana per Second
     */
    public float getMPS() {
        if (mps == Float.POSITIVE_INFINITY)
            calculateProperties();
        return mps;
    }

    public void setMPS(float value) {
        mps = value;
    }

    private float lag = Float.POSITIVE_INFINITY;

    /**
     * Effective time lost due to latency per spellcast.
     */
    public float getLatencyPerSecond() {
        if (lag == Float.POSITIVE_INFINITY)
            calculateProperties();
        return lag;
    }

    public void setLatencyPerSecond(float value) {
        lag = value;
    }

    public LightningBolt LB;
    public ChainLightning CL;
    public LavaBurst LvB;
    public LavaBurst LvBFS;
    public FlameShock FS;
    public EarthShock ES;
    public FrostShock FrS;
    public FireNova FN;
    public SearingTotem ST;
    public MagmaTotem MT;
    public FireElemental FE;
    public Totem ActiveTotem;

    public ShamanTalents Talents;

    public Rotation() {
        spells = new ArrayList<Spell>(15);
    }

    public Rotation(ShamanTalents talents, SpellBox spellBox, IRotationOptions rotOpt) {
        this();
        Talents = talents;
        LB = spellBox.getLB();
        CL = spellBox.getCL();
        LvB = spellBox.getLvB();
        LvBFS = spellBox.getLvBFS();
        FS = spellBox.getFS();
        ES = spellBox.getES();
        FrS = spellBox.getFrS();
        FN = spellBox.getFN();
        ST = spellBox.getST();
        MT = spellBox.getMT();
        FE = spellBox.getFE();

        useDpsFireTotem = rotOpt.getUseDpsFireTotem();

        calculateRotation(rotOpt.getUseFireNova(), rotOpt.getUseChainLightning(), rotOpt.getUseDpsFireTotem(), rotOpt.getUseFireEle());
    }

    /**
     * Invalidates the cached values.
     */
    private void invalidate() {
        casts = null;
        lastGetTime = Float.POSITIVE_INFINITY;
        lastDuration = Float.POSITIVE_INFINITY;
        lastBaseCastTime = Float.POSITIVE_INFINITY;
        lastWeightedCritchance = Float.POSITIVE_INFINITY;
        lastWeightedHitchance = Float.POSITIVE_INFINITY;
        cc = null;
        mps = Float.POSITIVE_INFINITY;
        dps = Float.POSITIVE_INFINITY;
    }

    /**
     * Calculates a rotation based on the FS>LvB>LB priority.
     */
    public void calculateRotation(boolean useFN, boolean useCL, boolean useDpsFireTotem, boolean useFireEle) {
        if (LB == null || FS == null || LvBFS == null || LvB == null || CL == null || FN == null || ST == null || MT == null)
            return;
        calculateRotation(true, true, useFN, useCL, useDpsFireTotem, useFireEle);
    }

    /**
     * Calculates a rotation based on the FS>LvB>LB priority.
     */
    public void calculateRotation(boolean addlb1, boolean addlb2, boolean useFN, boolean useCL, boolean useDpsFireTotem, boolean useFireEle) {
        if (Talents == null || LB == null || FS == null || LvBFS == null || LvB == null || CL == null || FN == null || ST == null || MT == null)
            return;
        this.useDpsFireTotem = useDpsFireTotem;
        spells.clear();
        invalidate();

        float LvBreadyAt = 0, FSdropsAt = 0, clReadyAt = 0, fnReadyAt = 0, activeTotemDropsAt = 0, fireEleDropsAt = 0, fireEleReadyAt = 0;

        if (useDpsFireTotem) {
            // Reasoning for this block:
            // Magma totem is always higher DpS than searing totem.  The old formula, which used DpCT instead of
            // periodic DpS, always favored searing totem over magma totem, which isn't true in all cases.  The reason
            // searing eventually "outscales" magma is because magma's excessive use of GCDs for 100% uptime makes it
            // less favorable in gear with lots of haste, when the shaman begins approaching the GCD with spells.
            // Losing a GCD when an LB takes 2 seconds to cast is less costly than a GCD with a 1.2 second LB.
            // Theory behind it:
            // The higher DpS spell between CL and LB will be the relative benchmark.  The gap in DpS between magma
            // and searing are taken and multiplied by 20 (the amount of damage gained by using magma in that period).
            // It is then modified by the cast time difference between MT and the relativeDpSValue.  This adjusts
            // magma's value according to how much of a gap there is in between the GCD and the cast time of the
            // benchmark spell.  The larger the gap, the more valuable magma totem's gained DpS.  If the gained DpS
            // isn't more valuable than the direct DpS of the benchmark, searing is selected instead.

            Spell relativeDpSValue = (CL.getDirectDpS() > LB.getDirectDpS()) ? (Spell) CL : (Spell) LB;
            if ((((MT.getPeriodicDpS() - ST.getPeriodicDpS()) * 20) / (MT.getCastTime() / relativeDpSValue.getCastTime()))
                    > relativeDpSValue.getDirectDpS())
                ActiveTotem = MT;
            else
                ActiveTotem = ST;
        } else {
            ActiveTotem = null;
            activeTotemDropsAt = Float.POSITIVE_INFINITY;
        }

        while (true) {
            if (getTime() >= LvBreadyAt) { //LvB is ready
                if (getTime() + LvBFS.getCastTimeWithoutGCD() < FSdropsAt) { //the LvB cast will be finished before FS runs out
                    addSpell(LvBFS);
                    LvBreadyAt = getTime() + LvBFS.getCooldown();
                    if (LvBFS.getElementalT10() && getTime() <= FSdropsAt) {
                        FSdropsAt += FS.getPeriodicTickTime() * FS.addTicks(6); //Research showed that the closest amount of ticks to 6s will be added.
                    }
                } else if (FSdropsAt == 0) { //the first FS
                    FSdropsAt = getTime() + FS.getDuration();
                    addSpell(FS);
                } else //since FS would run out recast FS now -> Rotation end
                    break; //done
            } else { //LvB is not ready
                if (getTime() + LvB.getCastTime() > FSdropsAt) { //FS will run out
                    if (((LB.getDpCT() > (Math.max((useCL && clReadyAt < getTime()) ? CL.getDpCT() : 0, (useFN && fnReadyAt < getTime()) ? FN.getDpCT() : 0)))
                            && (LB.getDpCT() > (((activeTotemDropsAt < getTime()) && useDpsFireTotem) ? ActiveTotem.periodicDamage() : 0)))) {    // LB is the best option available
                        if (LvBreadyAt - (getTime() + FS.getCastTime()) > LB.getCastTime()) //there is enough time to fit in another LB and a FS before LvB is ready
                            addSpell(LB);
                        else { //if (LvBreadyAt - (GetTime() + FS.CastTime) > 0 && addlb2) //FS would still fit in before LvB is ready
                            addSpell(LB);
                            break; //FS recast nescessary -> done
                        }
                    } else if ((((useCL && clReadyAt < getTime()) ? CL.getDpCT() : 0)
                            > ((useFN && fnReadyAt < getTime()) ? FN.getDpCT() : 0))
                            && (CL.getDpCT() > ((activeTotemDropsAt < getTime()) && (useDpsFireTotem) ? ActiveTotem.periodicDamage() : 0)))
                    // CL > FN and CL > Totem [Or totem doesn't need refreshed]
                    {
                        if (LvBreadyAt - (getTime() + FS.getCastTime()) > CL.getCastTime()) { // Can fit another CL and FS
                            addSpell(CL);
                            clReadyAt = getTime() + CL.getCooldown();
                        } else {
                            addSpell(CL);
                            clReadyAt = getTime() + CL.getCooldown();
                            break;
                        }
                    } else if ((useFN && fnReadyAt < getTime()) && (activeTotemDropsAt > getTime()))
                    // FN > CL and FN > Totem [Or totem doesn't need refreshed]
                    {
                        if (LvBreadyAt - (getTime() + FS.getCastTime()) > FN.getCastTime()) { // Can fit another FN and FS
                            addSpell(FN);
                            fnReadyAt = getTime() + FN.getCooldown();
                        } else {
                            addSpell(FN);
                            fnReadyAt = getTime() + FN.getCooldown();
                            break;
                        }
                    } else { // If the totem needs refreshed...
                        addSpell(ActiveTotem);
                        activeTotemDropsAt = ActiveTotem.getDuration() + getTime();
                    }
                } else if (LvBreadyAt - getTime() <= LB.getCastTime() && !addlb1) //time before the next LvB is lower than LB cast time
                    addSpell(new Wait(LvBreadyAt - getTime()));
                else //LvB is on cooldown, FS won't run out soon
                    if (((LB.getDpCT() > (Math.max((useCL && clReadyAt < getTime()) ? CL.getDpCT() : 0, (useFN && fnReadyAt < getTime()) ? FN.getDpCT() : 0)))
                            && (LB.getDpCT() > (((activeTotemDropsAt < getTime()) && useDpsFireTotem) ? ActiveTotem.periodicDamage() : 0)))) {    //Is LB Dmg per cast time bigger than the Dpct of CL or FN and are these spells ready?
                        addSpell(LB);
                    } else if ((((useCL && clReadyAt < getTime()) ? CL.getDpCT() : 0)
                            > ((useFN && fnReadyAt < getTime()) ? FN.getDpCT() : 0))
                            && (CL.getDpCT() > ((activeTotemDropsAt < getTime()) && (useDpsFireTotem) ? ActiveTotem.periodicDamage() : 0)))
                    // CL > FN and CL > Totem [Or totem doesn't need refreshed]
                    {
                        addSpell(CL);
                        clReadyAt = getTime() + CL.getCooldown();
                    } else if ((useFN && fnReadyAt < getTime()) && (activeTotemDropsAt > getTime())) {    //FN > CL
                        addSpell(FN);
                        fnReadyAt = getTime() + FN.getCooldown();
                    } else { // If the totem needs refreshed...
                        addSpell(ActiveTotem);
                        activeTotemDropsAt = ActiveTotem.getDuration() + getTime();
                    }
            }
        }
    }

    public void addSpell(Spell spell) {
        spells.add(spell);
        invalidate();
    }

    public void removeSpellAt(int i) {
        spells.remove(i);
        invalidate();
    }

    private float lastGetTime = Float.POSITIVE_INFINITY;

    /**
     * Gets the time after the last spell has been cast and the GCD is ready.
     */
    public float getTime() {
        if (lastGetTime != Float.POSITIVE_INFINITY)
            return lastGetTime;
        return lastGetTime = getTime(spells.size());
    }

    /**
     * Gets the time before spell number i has been cast (with GCD) counting 0 as the first one.
     */
    public float getTime(int i) {
        float time = 0f;
        for (int j = 0; j < i; j++)
            time += spells.get(mod(j, spells.size())).getCastTime();
        return time;
    }

    private float lastDuration = Float.POSITIVE_INFINITY;

    /**
     * The duration of one rotation pass. The total casting time is modified by totem uptime weighted
     * totem casting times.
     */
    public float getDuration() {
        if (lastDuration != Float.POSITIVE_INFINITY)
            return lastDuration;
        //The weighted totem time was causing an enormous disparity in DPS numbers in favor of not using 4t10.
        //Leaving this block here regardless for the time being, just commenting it out.
        //if (useDpsFireTotem)
        //    for (int i = spells.Count - 1; i >= 0; i--)
        //    {
        //        if (spells[i] is Totem) //last dps totem cast
        //        {
        //            Totem t = (Totem)spells[i];
        //            float overTime = GetTime(i) + t.Duration - GetTime();
        //            return lastDuration = (GetTime() - (t.CastTime * overTime)) / GetTime(); //substract overtime weigthed cast time
        //        }
        //    }
        return lastDuration = getTime();
    }

    private float lastBaseCastTime = Float.POSITIVE_INFINITY;

    public float getBaseCastTime() {
        if (lastBaseCastTime != Float.POSITIVE_INFINITY)
            return lastBaseCastTime;
        float time = 0f;
        for (int j = 0; j < spells.size(); j++)
            time += spells.get(j).getBaseCastTime();
        return lastBaseCastTime = time;
    }

    public float getCastsPerSecond() {
        return getCasts().size() / getDuration();
    }

    public float getCastsPerSecond(Class<?> spellType) {
        return getSpells(spellType).size() / getDuration();
    }

    public float getTicksPerSecond(Class<?> spellType) {
        float ticks = 0;
        for (int i = 0; i < spells.size(); i++) {
            Spell s = spells.get(i);
            if (spellType.isInstance(s)) {
                if (s.getDuration() <= 0)
                    return 0;
                float durationActive = getNextCastTime(i) - (getTime(i) + s.getCastTimeWithoutGCD());
                if (durationActive >= s.getDuration())
                    ticks += s.getPeriodicTicks();
                else
                    ticks += (float) Math.floor(durationActive / s.getPeriodicTickTime());
            }
        }
        return ticks / getDuration();
    }

    public List<Spell> getSpells(Class<?> spellType) {
        List<Spell> result = new ArrayList<Spell>();
        for (Spell spell : spells) {
            if (spellType.isInstance(spell)) result.add(spell);
        }
        return result;
    }

    private float lastWeightedHitchance = Float.POSITIVE_INFINITY;

    public float getWeightedHitchance() {
        if (lastWeightedHitchance != Float.POSITIVE_INFINITY)
            return lastWeightedHitchance;
        float hitchance = 0f;
        for (Spell s : getCasts())
            hitchance += s.getHitChance();
        return lastWeightedHitchance = hitchance / getCasts().size();
    }

    private float lastWeightedCritchance = Float.POSITIVE_INFINITY;

    /**
     * Returns the average Critchance with Hitchance factored in.
     */
    public float getWeightedCritchance() {
        if (lastWeightedCritchance != Float.POSITIVE_INFINITY)
            return lastWeightedCritchance;
        float critLB = LB.getCritChance();
        float critCL = LB.getCritChance() * (1f + .11f * Talents.getLightningOverload());
        float critchance = 0f;
        for (Spell s : getCasts())
            critchance += s.getHitChance() * s.getCCCritChance();
        return lastWeightedCritchance = critchance / getCasts().size();
    }

    public float waitingPercentage() {
        List<Spell> waits = getSpells(Wait.class);
        float d = 0f;
        for (Spell s : waits)
            d += s.getCastTime();
        return d / getTime();
    }

    private void calculateCastSpells() {
        casts = new ArrayList<Spell>();
        for (Spell s : spells) {
            if (!(s instanceof Wait)) casts.add(s);
        }
    }

    /**
     * Calculates Clearcasting, mana usage, dps and lag per second.
     */
    private void calculateProperties() {
        mps = 0f; //summing up total manacost
        dps = 0f; //summing up total damage
        lag = 0f;
        cc = new LinkedHashMap<Class<?>, Float>(); //clear casting
        spelldps = new LinkedHashMap<Class<?>, Float>(); //dps broken up per spell type
        spelltype = new LinkedHashMap<Class<?>, Spell>(); //all used spells by type
        LinkedHashMap<Class<?>, Integer> count = new LinkedHashMap<Class<?>, Integer>(); //counting spells
        Spell prev1 = null, prev2 = null;
        for (int i = -2; i < getCasts().size(); i++) {
            Spell s = getCast(i);
            if (i >= 0) { //the first two are just saved in order to calculate clear casting
                float thisdps = 0f;
                if (!cc.containsKey(s.getClass())) {
                    cc.put(s.getClass(), 0f);
                    spelldps.put(s.getClass(), 0f);
                    spelltype.put(s.getClass(), s);
                    count.put(s.getClass(), 0);
                }
                float ccc = 0f;
                if (Talents.getElementalFocus() > 0)
                    ccc = 1f - (1f - prev1.getCCCritChance()) * (1f - prev2.getCCCritChance());
                mps += s.getManaCost() * ((s instanceof Totem) ? 1f : (1 - .4f * ccc)); //totems are unaffected by cc
                if (s.getDuration() > 0) { //dot
                    int j = getSpellNumber(i);
                    float durationActive = getNextCastTime(j) - (getTime(j) + s.getCastTimeWithoutGCD());
                    thisdps = s.getHitChance() * (s.getAvgDamage() * (1 + .05f * Talents.getElementalOath() * ccc) + s.periodicDamage(durationActive)); //bad for FS ticks.
                } else
                    thisdps = s.getHitChance() * s.getTotalDamage() * (1 + .05f * Talents.getElementalOath() * ccc); //bad for FS ticks.
                dps += thisdps;
                spelldps.put(s.getClass(), spelldps.get(s.getClass()) + thisdps);
                cc.put(s.getClass(), cc.get(s.getClass()) + ccc);
                lag += s.getLatency();
                count.put(s.getClass(), count.get(s.getClass()) + 1);
            }
            if (!(s instanceof Totem)) { //totems are unaffected by cc
                prev2 = prev1;
                prev1 = s;
            }
        }
        for (Class<?> t : count.keySet()) {
            spelldps.put(t, spelldps.get(t) / getTime());
            cc.put(t, cc.get(t) / count.get(t));
        }
        mps /= getDuration(); //divide by rotation time
        dps /= getDuration();
        lag /= getDuration();
    }

    /**
     * Return the i-th spell cast.
     */
    public Spell getCast(int i) {
        return getCasts().get(mod(i, getCasts().size()));
    }

    /**
     * Spells based.
     */
    public float getNextCastTime(int i) {
        Class<?> spellType = spells.get(mod(i, spells.size())).getClass();
        int start = mod(i, spells.size());
        float time = getTime(i);
        i++;
        for (int j = start + 1; true; j = mod(++i, spells.size())) {
            if (!spellType.isInstance(spells.get(j))) {
                if (j == start)
                    return Float.MAX_VALUE;
                else
                    time += spells.get(j).getCastTime();
            } else
                return time;
        }
    }

    /**
     * Return the spell number based on it's cast number. This is ignoring Wait.
     */
    private int getSpellNumber(int castNumber) {
        int i;
        for (i = 0; castNumber >= 0; i++) {
            if (!(spells.get(i) instanceof Wait))
                castNumber--;
            if (castNumber < 0)
                break;
        }
        return i;
    }

    @Override
    public String toString() {
        String r = "";
        String prev = "";
        int count = 1;
        for (Spell s : spells) {
            if (s.toString().equals(prev))
                count++;
            else {
                if (prev.length() > 0)
                    r += "/" + ((count > 1) ? Integer.toString(count) : "") + prev;
                prev = s.toString();
                count = 1;
            }
        }
        if (prev.length() > 0)
            r += "/" + ((count > 1) ? Integer.toString(count) : "") + prev;
        return r.substring(1);
    }

    public String toDetailedString() {
        String r = "";
        for (int i = 0; i < spells.size(); i++) {
            r += "\n" + NetFormat.netDoubleToString(netRound(getTime(i), 2)) + "s\t" + spells.get(i).toString();
        }
        r += "\n" + NetFormat.netDoubleToString(netRound(getTime(), 2)) + "s\t...";
        return r.substring(1);
    }

    /**
     * An always positive Modulo.
     */
    private static int mod(int a, int n) {
        a = a % n;
        if (a < 0)
            a += n;
        return a;
    }

    private static double netRound(double value, int digits) {
        double power = Math.pow(10, digits);
        return Math.rint(value * power) / power;
    }
}
