// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ShadowPriest/Solver.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.shadowpriest;

import rawr.base.stats.BaseStats;
import rawr.base.character.Character;
import rawr.base.character.CharacterRace;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

class SolverBase {
    private static final SpecialEffect[] spiritTapEffects = new SpecialEffect[] {
            null,
            createSpiritTapEffect(0.05f, 0.17f),
            createSpiritTapEffect(0.10f, 0.33f),
    };

    private static SpecialEffect createSpiritTapEffect(float bonusSpiritMultiplier, float spellCombatManaRegeneration) {
        Stats stats = new Stats();
        stats.setBonusSpiritMultiplier(bonusSpiritMultiplier);
        stats.setSpellCombatManaRegeneration(spellCombatManaRegeneration);
        return new SpecialEffect(Trigger.SpellCrit, stats, 8f, 0f);
    }

    private static final SpecialEffect[] glyphOfShadowEffects = new SpecialEffect[] {
            null,
            new SpecialEffect(Trigger.SpellHit, new Stats(), 10f, 0f),
    };

    private List<Spell> spellPriority;

    public List<Spell> getSpellPriority() {
        return spellPriority;
    }

    protected void setSpellPriority(List<Spell> value) {
        spellPriority = value;
    }

    private List<Spell> spellSimulation;

    public List<Spell> getSpellSimulation() {
        return spellSimulation;
    }

    protected void setSpellSimulation(List<Spell> value) {
        spellSimulation = value;
    }

    private float overallDamage;

    public float getOverallDamage() {
        return overallDamage;
    }

    protected void setOverallDamage(float value) {
        overallDamage = value;
    }

    private float dps;

    public float getDPS() {
        return dps;
    }

    protected void setDPS(float value) {
        dps = value;
    }

    private float overallMana;

    public float getOverallMana() {
        return overallMana;
    }

    protected void setOverallMana(float value) {
        overallMana = value;
    }

    private float sustainDPS;

    public float getSustainDPS() {
        return sustainDPS;
    }

    protected void setSustainDPS(float value) {
        sustainDPS = value;
    }

    private float movementDamageLoss;

    public float getMovementDamageLoss() {
        return movementDamageLoss;
    }

    protected void setMovementDamageLoss(float value) {
        movementDamageLoss = value;
    }

    private LinkedHashMap<Float, Spell> sequence;

    public LinkedHashMap<Float, Spell> getSequence() {
        return sequence;
    }

    protected void setSequence(LinkedHashMap<Float, Spell> value) {
        sequence = value;
    }

    private CalculationOptionsShadowPriest calcOpts;

    public CalculationOptionsShadowPriest getCalcOpts() {
        return calcOpts;
    }

    public void setCalcOpts(CalculationOptionsShadowPriest value) {
        calcOpts = value;
    }

    private BossOptionsRef bossOpts;

    public BossOptionsRef getBossOpts() {
        return bossOpts;
    }

    public void setBossOpts(BossOptionsRef value) {
        bossOpts = value;
    }

    private Stats playerStats;

    public Stats getPlayerStats() {
        return playerStats;
    }

    public void setPlayerStats(Stats value) {
        playerStats = value;
    }

    protected Character character;

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character value) {
        character = value;
    }

    private float hitChance;

    public float getHitChance() {
        return hitChance;
    }

    protected void setHitChance(float value) {
        hitChance = value;
    }

    private List<Trinket> trinkets;

    public List<Trinket> getTrinkets() {
        return trinkets;
    }

    public void setTrinkets(List<Trinket> value) {
        trinkets = value;
    }

    private List<ManaSource> manaSources;

    public List<ManaSource> getManaSources() {
        return manaSources;
    }

    protected void setManaSources(List<ManaSource> value) {
        manaSources = value;
    }

    protected SpecialEffect seSpiritTap;
    protected SpecialEffect seGlyphofShadow;
    protected float spellDamageFromSpiritPercentage;

    private String name;

    public String getName() {
        return name;
    }

    protected void setName(String value) {
        name = value;
    }

    private String rotation;

    public String getRotation() {
        return rotation;
    }

    protected void setRotation(String value) {
        rotation = value;
    }

    public static class Trinket {
        private float damageBonus;

        public float getDamageBonus() {
            return damageBonus;
        }

        public void setDamageBonus(float value) {
            damageBonus = value;
        }

        private float hasteBonus;

        public float getHasteBonus() {
            return hasteBonus;
        }

        public void setHasteBonus(float value) {
            hasteBonus = value;
        }

        private float cooldown;

        public float getCooldown() {
            return cooldown;
        }

        public void setCooldown(float value) {
            cooldown = value;
        }

        private float cooldownTimer;

        public float getCooldownTimer() {
            return cooldownTimer;
        }

        public void setCooldownTimer(float value) {
            cooldownTimer = value;
        }

        private float upTime;

        public float getUpTime() {
            return upTime;
        }

        public void setUpTime(float value) {
            upTime = value;
        }

        private float upTimeTimer;

        public float getUpTimeTimer() {
            return upTimeTimer;
        }

        public void setUpTimeTimer(float value) {
            upTimeTimer = value;
        }
    }

    public static class ManaSource {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String value) {
            name = value;
        }

        private float value;

        public float getValue() {
            return value;
        }

        public void setValue(float v) {
            value = v;
        }

        public ManaSource(String name, float value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class SpellComparerDpCT implements Comparator<Spell> {
        @Override
        public int compare(Spell a, Spell b) {
            if (a == null) {
                if (b == null)
                    return 0;
                return -1;
            } else if (b == null)
                return 1;
            return (int) Math.rint(b.getDpCT() - a.getDpCT());
        }
    }

    public static class SpellComparerDpM implements Comparator<Spell> {
        @Override
        public int compare(Spell a, Spell b) {
            if (a == null) {
                if (b == null)
                    return 0;
                return -1;
            } else if (b == null)
                return 1;
            return (int) Math.rint((b.getDpM() - a.getDpM()) * 100f);
        }
    }

    protected float getDirectDamage(Stats stats) {
        return stats.getArcaneDamage()
                + stats.getFireDamage()
                + stats.getFrostDamage()
                + stats.getHolyDamage()
                + stats.getNatureDamage()
                + stats.getShadowDamage()
                + stats.getValkyrDamage();
    }

    public SolverBase(Stats playerStats, Character _char) {
        character = _char;
        name = "Base";
        rotation = "None";
        spellDamageFromSpiritPercentage = _char.getPriestTalents().getSpiritualGuidance() * 0.05f + _char.getPriestTalents().getTwistedFaith() * 0.04f;

        // USE trinkets & effects.
        Stats twinkets = new Stats();
        for (SpecialEffect se : playerStats.specialEffects()) {
            if (se.getStats().getManaRestore() == 0 && se.getStats().getMp5() == 0 && se.getStats().getSpellPower() == 0) {
                if (se.getTrigger() == Trigger.Use)
                    twinkets = Stats.add(twinkets, se.getAverageStats());
                else if (se.getTrigger() == Trigger.DamageSpellCast
                        || se.getTrigger() == Trigger.SpellCast) {
                    if (se.getStats().getHighestStat() > 0) {
                        float greatnessProc = se.getAverageStats(2f, 1f).getHighestStat();
                        if (playerStats.getSpirit() > playerStats.getIntellect())
                            twinkets.setSpirit(twinkets.getSpirit() + greatnessProc);
                        else
                            twinkets.setIntellect(twinkets.getIntellect() + greatnessProc);
                    }
                }
            }
        }

        twinkets.setSpirit((float) Math.rint(twinkets.getSpirit() * (1 + playerStats.getBonusSpiritMultiplier())));
        twinkets.setIntellect((float) Math.rint(twinkets.getIntellect() * (1 + playerStats.getBonusIntellectMultiplier())));
        twinkets.setSpellPower(twinkets.getSpellPower() + (float) Math.rint(twinkets.getSpirit() * spellDamageFromSpiritPercentage));
        playerStats.accumulate(twinkets);

        calcOpts = (CalculationOptionsShadowPriest) character.getCalculationOptions();
        bossOpts = new BossOptionsRef(character);

        hitChance = playerStats.getSpellHit() * 100f + 100 - (StatConversion.getSpellMiss(-calcOpts.getTargetLevel(), false) * 100);
        if (!character.activeBuffsConflictingBuffContains("Spell Hit Chance Taken"))
            hitChance += character.getPriestTalents().getMisery() * 1f;
        if (character.getRace() == CharacterRace.Draenei && !character.activeBuffsContains("Heroic Presence"))
            hitChance += 1;
        hitChance = (float) Math.min(100f, hitChance);

        trinkets = new ArrayList<Trinket>();
        sequence = new LinkedHashMap<Float, Spell>();
        manaSources = new ArrayList<ManaSource>();

        // Spirit Tap
        if (character.getPriestTalents().getImprovedSpiritTap() > 0 && character.getPriestTalents().getImprovedSpiritTap() < 3)
            seSpiritTap = spiritTapEffects[character.getPriestTalents().getImprovedSpiritTap()];
        // Glyph of Shadow
        if (character.getPriestTalents().isGlyphofShadow())
            seGlyphofShadow = glyphOfShadowEffects[1];

        this.playerStats = playerStats;
    }

    public void calculate(CharacterCalculationsShadowPriest calculatedStats) {
        dps = 0;
        sustainDPS = 0;
    }

    public Spell getCastSpell(float timer) {
        for (Spell spell : spellPriority) {
            if ((spell.getDebuffDuration() > 0) && (spell.getCastTime() > 0) && (spell.getSpellStatistics().cooldownReset < (spell.getCastTime() + timer)))
                return spell;
            if (spell.getSpellStatistics().cooldownReset <= timer)
                return spell;
            if (spell.getSpellStatistics().cooldownReset > 0
                    && (spell.getSpellStatistics().cooldownReset - (spell.getDebuffDuration() > 0 ? spell.getCastTime() : 0) - timer < spell.getGlobalCooldown() * 0.2f))
                return null;
            if (spell.getSpellStatistics().cooldownReset <= timer)
                return spell;
        }
        return null;
    }

    public float oldGetCastSpell(float timer, Spell[] castSpell, Spell swd) {
        castSpell[0] = null;
        for (Spell spell : spellPriority) {
            castSpell[0] = spell;
            if ((spell.getDebuffDuration() > 0) && (spell.getCastTime() > 0) && (spell.getSpellStatistics().cooldownReset < (spell.getCastTime() + timer)))
                return 0;
            if (spell.getSpellStatistics().cooldownReset <= timer)
                return 0;
            if (spell.getSpellStatistics().cooldownReset > 0) {
                float nextCast = spell.getSpellStatistics().cooldownReset - (spell.getDebuffDuration() > 0 ? spell.getCastTime() : 0) - timer;
                if (nextCast < 0.5f)
                    return nextCast;
            }
            if (spell.getSpellStatistics().cooldownReset <= timer)
                return 0;
        }
        castSpell[0] = null;
        return 0;
    }

    public Spell newGetCastSpell(float timer) {
        float[] nextRelativeCastTime = new float[spellPriority.size()];

        int cnt = 0;
        for (Spell spell : spellPriority) {
            if (spell.getDebuffDuration() > 0)
                nextRelativeCastTime[cnt++] = spell.getSpellStatistics().cooldownReset - spell.getCastTime() - timer;
            else if (spell.getCooldown() > 0)
                nextRelativeCastTime[cnt++] = spell.getSpellStatistics().cooldownReset - timer;
            else {   // This is our filler.
                nextRelativeCastTime[cnt++] = 0;
                break;
            }
        }

        for (int x = 0; x < cnt; x++)
            if (nextRelativeCastTime[x] < 0)
                return spellPriority.get(x);

        Spell spellCastNext = spellPriority.get(cnt - 1);
        float fillerDPS = spellCastNext.getDpCT();
        float nextDelayCost = fillerDPS;
        for (int x = 0; x < cnt - 1; x++) {
            float delayCost = spellPriority.get(x).getDpCT() - nextRelativeCastTime[x] * fillerDPS;
            if (delayCost > nextDelayCost) {
                nextDelayCost = delayCost;
                spellCastNext = spellPriority.get(x);
            }
        }
        return spellCastNext;
    }

    public void recalculateHaste(Stats stats, float addedHasteRating) {
        for (Spell spell : spellPriority)
            spell.recalcHaste(stats, addedHasteRating);
    }

    public Spell getSpellByName(String name) {
        for (Spell spell : spellPriority) {
            if (spell.getName().contains(name))
                return spell;
        }
        return null;
    }

    public void upTrinket(float timer) {
        for (Trinket trinket : trinkets)
            if (trinket.getCooldownTimer() <= timer) {
                trinket.setCooldownTimer(timer + trinket.getCooldown());
                trinket.setUpTimeTimer(timer + trinket.getUpTime());
            }
    }

    public void getTrinketBuff(float timer, Stats stats) {
        for (Trinket trinket : trinkets)
            if (trinket.getUpTimeTimer() > timer) {
                stats.setHasteRating(stats.getHasteRating() + trinket.getHasteBonus());
                stats.setSpellPower(stats.getSpellPower() + trinket.getDamageBonus());
            }
    }

    /** Minimal observer of the C# BossOptions members the ShadowPriest solver reads (none). */
    public static class BossOptionsRef {
        public BossOptionsRef(Character character) {
        }
    }
}

class solverShadow extends SolverBase {
    // Models Full Rotation
    private float shadowHitChance;

    public float getShadowHitChance() {
        return shadowHitChance;
    }

    protected void setShadowHitChance(float value) {
        shadowHitChance = value;
    }

    private ShadowWordPain swp;
    private MindFlay mf;
    private VampiricEmbrace ve;
    private ShadowWordDeath swd;
    private MindBlast mb;
    private DevouringPlague dp;
    private boolean bPnS;

    public float newGetCastSpell2(float timer) {
        boolean bDoFlay = true;
        for (Spell spell : getSpellPriority()) {
            if (spell == mf)
                break;
            if ((spell.getDebuffDuration() > 0) && (spell.getCastTime() > 0) && (spell.getSpellStatistics().cooldownReset < (spell.getCastTime() + timer)))
                return 0f;
            if (spell.getSpellStatistics().cooldownReset <= timer)
                return 0f;
            float needToBeCast = (spell.getDebuffDuration() > 0) ?
                    (spell.getSpellStatistics().cooldownReset - spell.getCastTime() - timer) :
                    spell.getSpellStatistics().cooldownReset - timer;
            if (needToBeCast < mf.getCastTime()) {
                float DPSLossDoNothing = spell.getDpS() * needToBeCast;
                float DPSLossDoFillerTime = mf.getCastTime() - needToBeCast;
                float DPSLossDoFiller = mf.getDpS() * DPSLossDoFillerTime - spell.getDpS() * DPSLossDoFillerTime;
                if (DPSLossDoNothing < DPSLossDoFiller)
                    bDoFlay = false;
            }
        }
        if (bDoFlay)
            return 0f;
        else
            return 0f;
    }

    public float newGetCastSpell3(float timer, Spell[] castSpell) {
        castSpell[0] = null;
        for (Spell spell : getSpellPriority())
            if (spell.getDebuffDuration() == 0 && spell.getCooldown() == 0) {
                castSpell[0] = spell;
                break;
            }

        int idx = getSpellPriority().indexOf(castSpell[0]);
        float spellDelay = 0f;

        for (int x = idx - 1; x >= 0; x--) {
            Spell thisSpell = getSpellPriority().get(x);
            if ((thisSpell.getDebuffDuration() > 0) &&
                    (thisSpell.getCastTime() > 0) &&
                    (thisSpell.getSpellStatistics().cooldownReset < (thisSpell.getCastTime() + timer)))
                castSpell[0] = thisSpell;
            else if (thisSpell.getSpellStatistics().cooldownReset <= timer)
                castSpell[0] = thisSpell;
            else {
                float nextTimeToCastThis = (thisSpell.getDebuffDuration() > 0) ?
                        (thisSpell.getSpellStatistics().cooldownReset - thisSpell.getCastTime() - timer) :
                        (thisSpell.getSpellStatistics().cooldownReset - timer);

                if (nextTimeToCastThis < mf.getCastTime()) {
                    float completeTimeNext = (castSpell[0].getCastTime() > 0) ?
                            (castSpell[0].getCastTime()) :
                            (castSpell[0].getGlobalCooldown())
                                    + castSpell[0].getSpellStatistics().cooldownReset;
                    float DPSLossDoNothing = castSpell[0].getDpS() * (float) Math.max(castSpell[0].getGlobalCooldown(), nextTimeToCastThis);
                    float DPSLossNextSpellTime = completeTimeNext - nextTimeToCastThis;
                    float DPSLossDoNext = castSpell[0].getDpS() * DPSLossNextSpellTime - thisSpell.getDpS() * DPSLossNextSpellTime;
                    if (DPSLossDoNothing < DPSLossDoNext) {
                        spellDelay = nextTimeToCastThis;
                        castSpell[0] = thisSpell;
                    }
                }
            }
        }

        return spellDelay;
    }

    public float newGetCastSpell4(float timer, Spell[] castSpell) {
        float spellWait = 0f;
        castSpell[0] = null;
        for (Spell spell : getSpellPriority())
            if (spell.getDebuffDuration() == 0 && spell.getCooldown() == 0) {
                castSpell[0] = spell;
                break;  // We just found us a filler.
            }

        int idx = getSpellPriority().indexOf(castSpell[0]);
        float score = castSpell[0].getGlobalCooldown();
        for (int x = 0; x < idx; x++) {
            Spell thisSpell = getSpellPriority().get(x);

            float nextCast = 0f;
            if (thisSpell.getDebuffDuration() > 0)
                nextCast = thisSpell.getSpellStatistics().cooldownReset - thisSpell.getCastTime() - timer;
            else if (thisSpell.getCooldown() > 0)
                nextCast = thisSpell.getSpellStatistics().cooldownReset - timer;

            if (nextCast <= 0) {
                castSpell[0] = thisSpell;
                return 0f;
            } else if (nextCast >= castSpell[0].getCastTime())
                continue;

            if (nextCast < score) {
                castSpell[0] = thisSpell;
                score = nextCast;
                spellWait = (float) Math.max(0f, nextCast);
            }
        }
        return spellWait;
    }

    public float newGetCastSpell5(float timer, Spell[] castSpell) {
        castSpell[0] = null;
        float castDpC = 0f;
        float spellWait = 0f;
        for (Spell spell : getSpellPriority()) {
            float intervalTime = 0, wait = 0;
            if (spell.getDebuffDuration() > 0) {
                intervalTime = spell.getDebuffDuration();
                wait = spell.getSpellStatistics().cooldownReset - timer - spell.getCastTime();
            } else if (spell.getCooldown() > 0) {
                if (spell.getCastTime() > 0)
                    intervalTime = spell.getCooldown() + spell.getCastTime();
                else
                    intervalTime = spell.getCooldown();
                wait = spell.getSpellStatistics().cooldownReset - timer;
            } else
                intervalTime = spell.getCastTime();

            float damagePerCooldown = spell.getAvgDamage() / intervalTime;

            if (castSpell[0] == null) {
                castSpell[0] = spell;
                spellWait = wait;
                castDpC = damagePerCooldown;
            }
        }
        return spellWait;
    }

    public float newGetCastSpell7(float timer, Spell[] castSpell) {
        // estimates overall dps by estimating impact of collisions
        castSpell[0] = null;
        float bestWaitTime = 0;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (Spell spell : getSpellPriority()) {
            float castTime = spell.getCastTime();
            if (castTime == 0)
                castTime = spell.getGlobalCooldown();
            float timeShare = 1;
            float waitTime = 0;
            if (spell.getDebuffDuration() > 0) {
                waitTime = Math.max(0, spell.getSpellStatistics().cooldownReset - spell.getCastTime() - timer);
                timeShare = (waitTime + castTime) / (spell.getDebuffDuration());
            } else if (spell.getCooldown() > 0) {
                waitTime = Math.max(0, spell.getSpellStatistics().cooldownReset - timer);
                timeShare = (waitTime + castTime) / (spell.getCooldown() + castTime);
            }
            float dps = 0;
            float timeShareLeft = 1;
            for (Spell s : getSpellPriority()) {
                float ct = s.getCastTime();
                float realWait = 0;
                if (ct == 0)
                    ct = s.getGlobalCooldown();
                float t = 1;
                if (s == spell) {
                    t = timeShare;
                    realWait = waitTime;
                } else if (s.getDebuffDuration() > 0) {
                    float sWait = Math.max(0, s.getSpellStatistics().cooldownReset - s.getCastTime() - timer);
                    float delay = Math.max(0, waitTime + castTime - sWait);
                    t = ct / (s.getDebuffDuration() + delay);
                } else if (s.getCooldown() > 0) {
                    float sWait = Math.max(0, s.getSpellStatistics().cooldownReset - timer);
                    float delay = Math.max(0, waitTime + castTime - sWait);
                    t = ct / (s.getCooldown() + delay + ct);
                }
                if (t > timeShareLeft)
                    t = timeShareLeft;
                dps += t * s.getAvgDamage() / (ct + realWait);
                timeShareLeft -= t;
                if (timeShareLeft <= 0)
                    break;
            }
            if (dps > bestScore) {
                bestScore = dps;
                bestWaitTime = waitTime;
                castSpell[0] = spell;
            }
            if (spell.getDebuffDuration() == 0 && spell.getCooldown() == 0)
                break;
        }
        return bestWaitTime;
    }

    public float newGetCastSpell8(float timer, Spell[] castSpell) {
        // this is same as #7, but extended for window > 1
        castSpell[0] = null;
        float bestWaitTime = 0;
        float bestScore = Float.NEGATIVE_INFINITY;
        int N = 0;
        for (int i = 0; i < getSpellPriority().size(); i++) {
            Spell spell = getSpellPriority().get(i);
            if (spell.getDebuffDuration() == 0 && spell.getCooldown() == 0) {
                N = i + 1;
                break;
            }
        }
        int j;
        final int window = 2;
        int[] sequence = new int[window];
        float[] waitTime = new float[window];
        float[] timeShare = new float[window];
        boolean[] used = new boolean[N];
        do {
            boolean valid = true;
            Arrays.fill(used, 0, N, false);
            for (int i = 0; i < window; i++) {
                if (sequence[i] != N - 1 && used[sequence[i]]) {
                    valid = false;
                    break;
                }
                used[sequence[i]] = true;
            }
            if (valid) {
                float miniTimer = timer;
                for (int i = 0; i < window; i++) {
                    Spell spell = getSpellPriority().get(sequence[i]);
                    float castTime = spell.getCastTime();
                    if (castTime == 0)
                        castTime = spell.getGlobalCooldown();
                    timeShare[i] = 1;
                    waitTime[i] = 0;
                    if (spell.getDebuffDuration() > 0) {
                        float minWaitTime = Math.max(0, spell.getSpellStatistics().cooldownReset - spell.getCastTime() - timer);
                        float delay = Math.max(0, miniTimer - timer - minWaitTime);
                        waitTime[i] = Math.max(0, spell.getSpellStatistics().cooldownReset - spell.getCastTime() - miniTimer);
                        timeShare[i] = (waitTime[i] + castTime) / (spell.getDebuffDuration() + delay);
                    } else if (spell.getCooldown() > 0) {
                        float minWaitTime = Math.max(0, spell.getSpellStatistics().cooldownReset - timer);
                        float delay = Math.max(0, miniTimer - timer - minWaitTime);
                        waitTime[i] = Math.max(0, spell.getSpellStatistics().cooldownReset - miniTimer);
                        timeShare[i] = (waitTime[i] + castTime) / (spell.getCooldown() + delay + castTime);
                    }
                    miniTimer += castTime + waitTime[i];
                }
                float dps = 0;
                float timeShareLeft = 1;
                for (int i = 0; i < N; i++) {
                    Spell s = getSpellPriority().get(i);
                    float ct = s.getCastTime();
                    float realWait = 0;
                    if (ct == 0)
                        ct = s.getGlobalCooldown();
                    float t = 1;
                    int index = indexOf(sequence, i);
                    if (index >= 0) {
                        t = timeShare[index];
                        realWait = waitTime[index];
                    } else if (s.getDebuffDuration() > 0) {
                        float sWait = Math.max(0, s.getSpellStatistics().cooldownReset - s.getCastTime() - timer);
                        float delay = Math.max(0, miniTimer - timer - sWait);
                        t = ct / (s.getDebuffDuration() + delay);
                    } else if (s.getCooldown() > 0) {
                        float sWait = Math.max(0, s.getSpellStatistics().cooldownReset - timer);
                        float delay = Math.max(0, miniTimer - timer - sWait);
                        t = ct / (s.getCooldown() + delay + ct);
                    }
                    if (t > timeShareLeft)
                        t = timeShareLeft;
                    dps += t * s.getAvgDamage() / (ct + realWait);
                    timeShareLeft -= t;
                    if (timeShareLeft <= 0)
                        break;
                }
                if (dps > bestScore) {
                    bestScore = dps;
                    bestWaitTime = waitTime[0];
                    castSpell[0] = getSpellPriority().get(sequence[0]);
                }
            }
            j = window - 1;
            sequence[j]++;
            while (sequence[j] >= N) {
                sequence[j] = 0;
                j--;
                if (j < 0)
                    break;
                sequence[j]++;
            }
        } while (j >= 0);
        return bestWaitTime;
    }

    private static int indexOf(int[] array, int value) {
        for (int i = 0; i < array.length; i++)
            if (array[i] == value)
                return i;
        return -1;
    }

    public float newGetCastSpell9(float timer, Spell[] castSpell) {
        castSpell[0] = null;
        float bestWaitTime = 0;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (Spell spell : getSpellPriority()) {
            float castTime = spell.getCastTime();
            if (castTime == 0)
                castTime = spell.getGlobalCooldown();
            float timeShare = 1;
            float waitTime = 0;
            float repeatInterval = 0;
            if (spell.getDebuffDuration() > 0) {
                waitTime = Math.max(0, spell.getSpellStatistics().cooldownReset - spell.getCastTime() - timer);
                timeShare = (waitTime + castTime) / (spell.getDebuffDuration());
                repeatInterval = spell.getDebuffDuration();
            } else if (spell.getCooldown() > 0) {
                waitTime = Math.max(0, spell.getSpellStatistics().cooldownReset - timer);
                timeShare = (waitTime + castTime) / (spell.getCooldown() + castTime);
                repeatInterval = spell.getCooldown() + castTime;
            }
            float dps = 0;
            float timeShareLeft = 1;
            for (Spell s : getSpellPriority()) {
                float ct = s.getCastTime();
                float realWait = 0;
                if (ct == 0)
                    ct = s.getGlobalCooldown();
                float t = 1;
                if (s == spell) {
                    t = timeShare;
                    realWait = waitTime;
                } else if (s.getDebuffDuration() > 0) {
                    float sWait = Math.max(0, s.getSpellStatistics().cooldownReset - s.getCastTime() - timer);
                    float delay = Math.max(0, waitTime + castTime - sWait);
                    float collisionChance = (repeatInterval == 0) ? 1 : Math.min(1, s.getDebuffDuration() / repeatInterval);
                    t = ct / (s.getDebuffDuration() + collisionChance * delay);
                } else if (s.getCooldown() > 0) {
                    float sWait = Math.max(0, s.getSpellStatistics().cooldownReset - timer);
                    float delay = Math.max(0, waitTime + castTime - sWait);
                    float collisionChance = (repeatInterval == 0) ? 1 : Math.min(1, (s.getCooldown() + ct) / repeatInterval);
                    t = ct / (s.getCooldown() + ct + collisionChance * delay);
                }
                if (t > timeShareLeft)
                    t = timeShareLeft;
                dps += t * s.getAvgDamage() / (ct + realWait);
                timeShareLeft -= t;
                if (timeShareLeft <= 0)
                    break;
            }
            if (dps > bestScore) {
                bestScore = dps;
                bestWaitTime = waitTime;
                castSpell[0] = spell;
            }
            if (spell.getDebuffDuration() == 0 && spell.getCooldown() == 0)
                break;
        }
        return bestWaitTime;
    }

    public static class SpellInfo {
        private Spell spell;

        public Spell getSpell() {
            return spell;
        }

        public void setSpell(Spell value) {
            spell = value;
        }

        private float nextCast;

        public float getNextCast() {
            return nextCast;
        }

        public void setNextCast(float value) {
            nextCast = value;
        }

        public SpellInfo(Spell spell, float timer) {
            this.spell = spell;
            if (spell.getDebuffDuration() > 0)
                nextCast = spell.getSpellStatistics().cooldownReset - spell.getCastTime() - timer;
            else
                nextCast = spell.getSpellStatistics().cooldownReset - timer;
        }
    }

    public float newGetCastSpell6(float timer, Spell[] castSpell) {
        castSpell[0] = null;
        float wait = 0f;
        List<SpellInfo> spellList = new ArrayList<SpellInfo>();

        for (Spell spell : getSpellPriority()) {
            if (spell.getDebuffDuration() > 0 || spell.getCooldown() > 0)
                spellList.add(new SpellInfo(spell, timer));
            else {
                castSpell[0] = spell;
                break;
            }
        }

        for (SpellInfo si : spellList) {
            if (si.getNextCast() > castSpell[0].getCastTime()) {
                spellList.remove(si);
                continue;
            } else if (si.getNextCast() <= 0) {
                castSpell[0] = si.getSpell();
                return 0f;
            }
        }

        return wait;
    }

    public solverShadow(Stats BasicStats, Character character) {
        super(BasicStats, character);
        setSpellPriority(new ArrayList<Spell>(getCalcOpts().getSpellPriority().size()));
        float baseMana = BaseStats.getBaseStats(character).getMana();
        for (String spellname : getCalcOpts().getSpellPriority()) {
            Spell spelltmp = SpellFactory.createSpell(spellname, getPlayerStats(), character, baseMana, getCalcOpts().getPTR());
            if (spelltmp != null) getSpellPriority().add(spelltmp);
        }

        swp = (ShadowWordPain) getSpellByName("Shadow Word: Pain");
        mf = (MindFlay) getSpellByName("Mind Flay");
        ve = (VampiricEmbrace) getSpellByName("Vampiric Embrace");
        swd = (ShadowWordDeath) getSpellByName("Shadow Word: Death");
        mb = (MindBlast) getSpellByName("Mind Blast");
        dp = (DevouringPlague) getSpellByName("Devouring Plague");

        if (ve != null) {
            // Functional yet abysmal method of moving VE to bottom of priority.
            getSpellPriority().remove(ve);
            getSpellPriority().add(ve);
        }

        // With Pain and Suffering, Mind Flay has a 33/66/100% chance to refresh SW:P.
        bPnS = (swp != null) && (character.getPriestTalents().getPainAndSuffering() > 0);
        if (bPnS) {
            getSpellPriority().remove(swp);
            getSpellPriority().add(swp);
        }

        setName("Shadow w/Mind Flay");
        setRotation("Priority Based:");
        for (Spell spell : getSpellPriority())
            setRotation(getRotation() + "\r\n- " + spell.getName());
        setShadowHitChance((float) Math.min(100f, getHitChance() + character.getPriestTalents().getShadowFocus() * 1f));
    }

    @Override
    public void calculate(CharacterCalculationsShadowPriest calculatedStats) {
        calculate(calculatedStats, true);
    }

    public void calculate(CharacterCalculationsShadowPriest calculatedStats, boolean bVerbal) {
        if (getSpellPriority().isEmpty())
            return;

        // Make sure calculations are properly reset.
        for (Spell spell : getSpellPriority())
            spell.getSpellStatistics().reset();
        recalculateHaste(getPlayerStats(), 0);

        Stats simStats = getPlayerStats().copy();
        boolean bTwistedFaith = character.getPriestTalents().getTwistedFaith() > 0;
        float timer = 0;
        float hasteProcTimer = 0;
        int sequence = getSpellPriority().size() - 1;
        List<Spell> castList = new ArrayList<Spell>();
        int fightLength = (int) getCalcOpts().getFightLength();

        // Initial SW:Pain application
        if (bPnS) {
            swp.getSpellStatistics().cooldownReset = timer + swp.getDebuffDuration();
            swp.getSpellStatistics().hitCount++;
            swp.getSpellStatistics().manaUsed = swp.getManaCost();
        }

        if (ve != null) {
            ve.getSpellStatistics().hitCount = fightLength;
            ve.getSpellStatistics().manaUsed = fightLength * ve.getManaCost();
        }

        if (dp != null && dp.ImprovedDP != null)
            getSpellPriority().add(dp.ImprovedDP);

        // Pass 1: Create the cast sequence
        boolean cleanBreak = false;
        float timeSequenceReset = 0f;
        while (timer < (60 * fightLength)) {
            if (hasteProcTimer > 0 && hasteProcTimer < timer) {
                hasteProcTimer = 0f;
                recalculateHaste(simStats, 0f);
            }
            Spell[] castSpellHolder = new Spell[1];
            float castWait = oldGetCastSpell(timer, castSpellHolder, swd);
            Spell spell = castSpellHolder[0];
            timer += castWait;
            if (spell == null) {
                timer += 0.1f;
                continue;
            }

            castList.add(spell);
            timer += getCalcOpts().getDelay() / 1000f;
            spell.getSpellStatistics().hitCount++;

            if (bPnS && spell == mf)
                swp.getSpellStatistics().cooldownReset = timer + swp.getDebuffDuration();
            else if (spell.getDebuffDuration() > 0f || spell.getCooldown() > 0f)
                spell.getSpellStatistics().cooldownReset = timer + (spell.getDebuffDuration() > spell.getCooldown() ? spell.getDebuffDuration() : spell.getCooldown()) + spell.getCastTime();

            timer += (spell.getCastTime() > 0) ? spell.getCastTime() : spell.getGlobalCooldown();

            if (simStats.getMindBlastHasteProc() > 0 && spell == mb) {
                recalculateHaste(simStats, simStats.getMindBlastHasteProc());
                hasteProcTimer = timer + 4f;
            }

            if (spell == getSpellPriority().get(sequence))
                sequence++;
            else {
                sequence = 0;
                timeSequenceReset = timer;
            }
            if (getSpellPriority().get(sequence) == mf) {
                // Spell sequence just reset, lets take advantage of that.
                int i = getSpellPriority().indexOf(mf);
                for (int x = castList.size() - i; x < castList.size(); x++)
                    castList.get(x).getSpellStatistics().hitCount--;
                castList.subList(castList.size() - i, castList.size()).clear();
                cleanBreak = true;
                timer = timeSequenceReset;
                break;
            }
        }
        if (!cleanBreak) {
            // Cut down on excess DoT damage.
            for (Spell spell : getSpellPriority()) {
                if (spell == mf) break;
                if (spell.getDebuffDuration() > 0)
                    spell.getSpellStatistics().hitCount -= (spell.getSpellStatistics().cooldownReset - timer) / spell.getDebuffDuration();
                else if (spell.getCooldown() > 0)
                    spell.getSpellStatistics().hitCount -= (spell.getSpellStatistics().cooldownReset - timer) / spell.getCooldown();
            }
        }
        setSpellSimulation(castList);

        // Pass 2: Calculate Statistics for Procs
        float castsPerSecond = 0, hitsPerSecond = 0, critsPerSecond = 0;
        for (Spell spell : castList) {
            castsPerSecond++;
            hitsPerSecond += shadowHitChance / 100f;
            if (spell == swd)
                critsPerSecond += swd.getCritChance();
            else if (spell == mb)
                critsPerSecond += mb.getCritChance();
            else if (spell == mf)
                critsPerSecond += mf.getCritChance() * 3f * 0.5f;    // Only 50% chance to proc
        }
        castsPerSecond /= timer;
        critsPerSecond /= timer;
        hitsPerSecond /= timer;

        // Deal with Spirit Tap
        if (seSpiritTap != null) {
            float uptime = seSpiritTap.getAverageUptime(1f / critsPerSecond, 1f);
            float NewSpirit = simStats.getSpirit() * seSpiritTap.getStats().getBonusSpiritMultiplier() * uptime;
            float NewSPP = NewSpirit * spellDamageFromSpiritPercentage;
            simStats.setSpirit(simStats.getSpirit() + NewSpirit);
            simStats.setSpellPower(simStats.getSpellPower() + NewSPP);
            if (bVerbal)
                setRotation(getRotation() + String.format(java.util.Locale.ROOT, "\r\nImp. Spirit Tap Uptime: %s%%", rawr.base.util.NetFormat.fixed(uptime * 100f, 1)));
        }
        if (seGlyphofShadow != null) {
            float uptime = seGlyphofShadow.getAverageUptime(1f / critsPerSecond, 1f);
            simStats.setSpellPower(simStats.getSpellPower() + simStats.getSpirit() * 0.3f * uptime);
            if (bVerbal)
                setRotation(getRotation() + String.format(java.util.Locale.ROOT, "\r\nGlyph of Shadow Uptime: %s%%", rawr.base.util.NetFormat.fixed(uptime * 100f, 1)));
        }

        // Deal with Twinkets
        for (SpecialEffect se : simStats.specialEffects()) {
            float SpellPower = se.getStats().getSpellPower();
            if (SpellPower > 0) {
                if (se.getTrigger() == Trigger.Use)
                    simStats.setSpellPower(simStats.getSpellPower() + se.getAverageFactor(se.getCooldown(), 1f) * SpellPower);
                else if (se.getTrigger() == Trigger.SpellCast
                        || se.getTrigger() == Trigger.DamageSpellCast)
                    simStats.setSpellPower(simStats.getSpellPower() + se.getAverageFactor(1f / castsPerSecond, 1f) * SpellPower);
                else if (se.getTrigger() == Trigger.SpellHit
                        || se.getTrigger() == Trigger.DamageSpellHit)
                    simStats.setSpellPower(simStats.getSpellPower() + se.getAverageFactor(1f / castsPerSecond, shadowHitChance / 100f) * SpellPower);
                else if (se.getTrigger() == Trigger.SpellCrit
                        || se.getTrigger() == Trigger.DamageSpellCrit)
                    simStats.setSpellPower(simStats.getSpellPower() + se.getAverageFactor(1f / castsPerSecond, simStats.getSpellCrit()) * SpellPower);
                else if (se.getTrigger() == Trigger.DoTTick) {
                    int dots = 0;
                    for (Spell spell : getSpellPriority())
                        if ((spell.getDebuffDuration() > 0) && (spell.getDpS() > 0)) dots++;
                    simStats.setSpellPower(simStats.getSpellPower() + se.getAverageFactor((float) dots / 3, 1f) * SpellPower);
                }
            }
        }

        // Pass3 + Pass 4: Redo Spell Stats and Calculate Damage and Mana Usage
        for (Spell spell : getSpellPriority()) {
            spell.calculate(simStats, character);
            float Damage = spell.getAvgDamage();
            float Cost = spell.getManaCost();
            switch (spell.getName()) {
                case "Vampiric Touch":
                    // Reapplyable DoTs, a resist means you lose 1 GCD to reapply. (~= cost of 1 GCD worth of MF)
                    Damage -= mf.getDpS() * mf.getGlobalCooldown() * (1f - shadowHitChance / 100f);
                    Cost *= (2f - shadowHitChance / 100f);
                    break;
                case "Mind Flay":
                    // Reapplyable DoTs, a resist means you lose 1 GCD to reapply. (~= cost of 1 GCD worth of MF)
                    Damage -= mf.getDpS() * mf.getGlobalCooldown() * (1f - shadowHitChance / 100f);
                    Cost *= (2f - shadowHitChance / 100f);
                    break;
                case "Shadow Word: Pain":
                    // SW:Pain follows same rules as other reapplyable dots, but needs special handling with
                    // Pain and Suffering.
                    if (bPnS)
                        break;
                    Damage -= mf.getDpS() * mf.getGlobalCooldown() * (1f - shadowHitChance / 100f);
                    Cost *= (2f - shadowHitChance / 100f);
                    break;
                case "Mind Blast":
                case "Shadow Word: Death":
                    // Spells that have cooldowns, a resist means full loss of damage.
                    Damage *= shadowHitChance / 100f;
                    break;
                case "Devouring Plague":
                    DevouringPlague dpSpell = (DevouringPlague) spell;
                    if (dpSpell.ImprovedDP != null) {
                        float lDamage = dpSpell.ImprovedDP.getAvgDamage()
                                * (1f + simStats.getBonusShadowDamageMultiplier())
                                * (1f + simStats.getBonusDamageMultiplier())
                                * (getHitChance() / 100f);
                        dpSpell.ImprovedDP.getSpellStatistics().damageDone += lDamage;
                        setOverallDamage(getOverallDamage() + lDamage);
                    }
                    Damage *= shadowHitChance / 100f;
                    break;
                default:
                    break;
            }
            Damage *= spell.getSpellStatistics().hitCount;
            Cost *= spell.getSpellStatistics().hitCount;
            Damage *= (1f + simStats.getBonusShadowDamageMultiplier()) * (1f + simStats.getBonusDamageMultiplier());
            spell.getSpellStatistics().damageDone += Damage;
            spell.getSpellStatistics().manaUsed += Cost;
            setOverallMana(getOverallMana() + Cost);
        }

        if (bVerbal) {
            if (!cleanBreak)
                setRotation(getRotation() + "\r\nWARNING: Did not find a clean rotation!\r\nThis may make Haste inaccurate!");
            setRotation(getRotation() + String.format(java.util.Locale.ROOT, "\r\nRotation reset after %s seconds.", rawr.base.util.NetFormat.fixed(timer, 2)));
        }

        // Pass 5: Do spell statistics & handle movement.
        for (Spell spell : getSpellPriority()) {
            spell.getSpellStatistics().hitCount /= timer;
            spell.getSpellStatistics().critCount = spell.getSpellStatistics().hitCount * spell.getCritChance();
            spell.getSpellStatistics().missCount = spell.getSpellStatistics().hitCount * (1 - shadowHitChance / 100f);
            spell.getSpellStatistics().manaUsed /= timer;
            if (bPnS && spell == swp)
                spell.getSpellStatistics().damageDone = spell.getDpS() * (1f + simStats.getBonusShadowDamageMultiplier()) * (1f + simStats.getBonusDamageMultiplier());
            else {
                if (spell.getCastTime() > 0) {
                    float realMoveDuration = getCalcOpts().getMoveDuration() / (1f + simStats.getMovementSpeed());
                    float spellWait = (spell.getCooldown() + spell.getDebuffDuration()) * 0.5f;
                    if (realMoveDuration > spellWait) {
                        // Apply movement penalty
                        float DPSLossTime = spell.getCastTime() + realMoveDuration - spellWait;
                        float DPSLoss = DPSLossTime / getCalcOpts().getMoveFrequency();
                        setMovementDamageLoss(getMovementDamageLoss() + spell.getSpellStatistics().damageDone * DPSLoss);
                        spell.getSpellStatistics().damageDone *= (1f - DPSLoss);
                    }
                }
                setOverallDamage(getOverallDamage() + spell.getSpellStatistics().damageDone);
                spell.getSpellStatistics().damageDone /= timer;
            }
        }
        setMovementDamageLoss(getMovementDamageLoss() / timer);
        if (bVerbal)
            setRotation(getRotation() + "\r\nDPS lost to movement: " + rawr.base.util.NetFormat.fixed(getMovementDamageLoss(), 2));

        setDPS(getOverallDamage() / timer + (bPnS ? swp.getDpS() * (1f + simStats.getBonusShadowDamageMultiplier()) * (1f + simStats.getBonusDamageMultiplier()) : 0));

        // Finalize Trinkets
        for (SpecialEffect se : simStats.specialEffects()) {
            float DD = getDirectDamage(se.getStats());
            if (DD > 0) {
                float dpr = 0f;
                if (se.getTrigger() == Trigger.Use) {
                    float Factor = se.getAverageFactor(se.getCooldown(), 1f);
                    dpr += DD * Factor;
                } else if (se.getTrigger() == Trigger.DoTTick) {
                    int dots = 0;
                    for (Spell spell : getSpellPriority())
                        if ((spell.getDebuffDuration() > 0) && (spell.getDpS() > 0)) dots++;
                    float Factor = se.getAverageFactor(dots / 3f, 1f);
                    dpr += DD * Factor;
                } else if (se.getTrigger() == Trigger.DamageSpellHit
                        || se.getTrigger() == Trigger.SpellHit) {
                    float Factor = se.getAverageFactor(1f / castsPerSecond, 1f);
                    dpr += DD * Factor;
                } else if (se.getTrigger() == Trigger.DamageSpellCrit
                        || se.getTrigger() == Trigger.SpellCrit) {
                    float Factor = se.getAverageFactor(1f / castsPerSecond, simStats.getSpellCrit());
                    dpr += DD * Factor;
                }
                if (dpr > 0) {
                    dpr *= (1f + simStats.getSpellCrit() * 0.5f);
                    if (se.getStats().getShadowDamage() > 0) {
                        dpr *= (character.getPriestTalents().getShadowWeaving() > 0 ? 1.1f : 1.0f)
                                * (1f + character.getPriestTalents().getDarkness() * 0.02f)
                                * (1f + character.getPriestTalents().getShadowform() * 0.15f)
                                * (shadowHitChance / 100f);
                    } else if (se.getStats().getValkyrDamage() > 0) { }
                    else
                        dpr *= (getHitChance() / 100f);
                    setDPS(getDPS() + dpr * (1f + simStats.getBonusDamageMultiplier()) * (1f + simStats.getBonusShadowDamageMultiplier()));
                }
            }
        }

        if (bVerbal)
            setRotation(getRotation() + "\r\n\r\nMana Buffs:");

        setSustainDPS(getDPS());
        float MPS = getOverallMana() / timer;
        float SpiritRegen = (float) Math.floor(StatConversion.getSpiritRegenSec(simStats.getSpirit(), simStats.getIntellect()));
        float regen = 0, tmpregen = 0;
        tmpregen = SpiritRegen * simStats.getSpellCombatManaRegeneration() * (getCalcOpts().getFSRRatio() / 100f);
        if (tmpregen > 0f) {
            getManaSources().add(new ManaSource("Meditation", tmpregen));
            regen += tmpregen;
        }
        tmpregen = SpiritRegen * (1f - getCalcOpts().getFSRRatio() / 100f);
        if (tmpregen > 0f) {
            getManaSources().add(new ManaSource("OutFSR", tmpregen));
            regen += tmpregen;
        }
        tmpregen = simStats.getMp5() / 5;
        getManaSources().add(new ManaSource("MP5", tmpregen));
        regen += tmpregen;
        if (seSpiritTap != null) {
            tmpregen = SpiritRegen * seSpiritTap.getAverageUptime(1f / critsPerSecond, 1f) * seSpiritTap.getStats().getSpellCombatManaRegeneration();
            if (tmpregen > 0f) {
                getManaSources().add(new ManaSource("Imp. Spirit Tap", tmpregen));
                regen += tmpregen;
            }
        }
        tmpregen = simStats.getMana() / (fightLength * 60f);
        getManaSources().add(new ManaSource("Intellect", tmpregen));
        regen += tmpregen;
        tmpregen = simStats.getMana() * 0.002f * (getCalcOpts().getReplenishment() / 100f);
        getManaSources().add(new ManaSource("Replenishment", tmpregen));
        regen += tmpregen;
        tmpregen = BaseStats.getBaseStats(character).getMana() * (simStats.getManaRestoreFromBaseManaPPM() > 0 ? 0.02f * 0.25f : 0f) * hitsPerSecond * (getCalcOpts().getJoW() / 100f);
        if (tmpregen > 0) {
            getManaSources().add(new ManaSource("Judgement of Wisdom", tmpregen));
            regen += tmpregen;
        }
        if (character.getPriestTalents().isGlyphofShadowWordPain()) {
            tmpregen = BaseStats.getBaseStats(character).getMana() * 0.01f / (swp.getDebuffDuration() / swp.getDebuffTicks());
            if (tmpregen > 0) {
                getManaSources().add(new ManaSource("Glyph of SWP", tmpregen));
                regen += tmpregen;
            }
        }
        for (SpecialEffect se : simStats.specialEffects()) {
            float ManaRestore = se.getStats().getManaRestore();
            float Mp5 = se.getStats().getMp5();
            if (ManaRestore > 0 || Mp5 > 0) {
                if (se.getTrigger() == Trigger.Use)
                    tmpregen = se.getAverageUptime(se.getCooldown(), 1f)
                            * (ManaRestore + Mp5 / 5);
                else if (se.getTrigger() == Trigger.SpellCast
                        || se.getTrigger() == Trigger.DamageSpellCast)
                    tmpregen = se.getAverageUptime(1f / castsPerSecond, 1f)
                            * (ManaRestore + Mp5 / 5);
                else if (se.getTrigger() == Trigger.SpellHit
                        || se.getTrigger() == Trigger.DamageSpellHit)
                    tmpregen = se.getAverageUptime(1f / castsPerSecond, shadowHitChance / 100f)
                            * (ManaRestore + Mp5 / 5);
                else if (se.getTrigger() == Trigger.SpellCrit
                        || se.getTrigger() == Trigger.DamageSpellCrit)
                    tmpregen = se.getAverageUptime(1f / castsPerSecond, simStats.getSpellCrit())
                            * (ManaRestore + Mp5 / 5);
                else
                    tmpregen = 0;
                if (tmpregen > 0) {
                    getManaSources().add(new ManaSource(se.toString(), tmpregen));
                    regen += tmpregen;
                }
            }
        }

        if (MPS > regen && character.getRace() == CharacterRace.BloodElf) {
            // Arcane Torrent is 6% max mana every 2 minutes.
            tmpregen = simStats.getMana() * 0.06f / 120f;
            getManaSources().add(new ManaSource("Arcane Torrent", tmpregen));
            regen += tmpregen;
            if (bVerbal)
                setRotation(getRotation() + "\r\n- Used Arcane Torrent");
        }

        if (MPS > regen && getCalcOpts().getManaAmt() > 0) {
            // Not enough mana, use Mana Potion
            tmpregen = getCalcOpts().getManaAmt() / (fightLength * 60f) * (1f + simStats.getBonusManaPotion());
            getManaSources().add(new ManaSource("Mana Potion", tmpregen));
            regen += tmpregen;
            if (bVerbal)
                setRotation(getRotation() + "\r\n- Used Mana Potion");
        }

        if (MPS > regen) {
            // Not enough mana, use Shadowfiend
            float sf_rat = (getCalcOpts().getShadowfiend() / 100f) / ((5f - character.getPriestTalents().getVeiledShadows() * 1f) * 60f);
            tmpregen = simStats.getMana() * 0.5f * sf_rat;
            getManaSources().add(new ManaSource("Shadowfiend", tmpregen));
            regen += tmpregen;
            if (bVerbal)
                setRotation(getRotation() + "\r\n- Used Shadowfiend");
        }

        if (MPS > regen && character.getPriestTalents().getDispersion() > 0) {
            // Not enough mana, so eat a Dispersion, remove DPS worth of 6 seconds of mindflay.
            float disp_rat = 6f / (60f * 3f - (character.getPriestTalents().isGlyphofDispersion() ? 45 : 0));
            tmpregen = simStats.getMana() * 0.06f * disp_rat;
            getManaSources().add(new ManaSource("Dispersion", tmpregen));
            regen += tmpregen;
            setSustainDPS(getSustainDPS() - mf.getDpS() * disp_rat);
            if (bVerbal)
                setRotation(getRotation() + "\r\n- Used Dispersion");
        }

        setDPS(getDPS() * (1f - StatConversion.getAverageResistance(character.getLevel(), character.getLevel() + getCalcOpts().getTargetLevel(), 0, 0)));
        setSustainDPS((MPS < regen) ? getSustainDPS() : (getSustainDPS() * regen / MPS));
    }
}

class SolverShadow extends SolverBase {
    // A wrapper for solverShadow to handle haste properly.
    public static class SolverInfo {
        private solverShadow solver;

        public solverShadow getSolver() {
            return solver;
        }

        public void setSolver(solverShadow value) {
            solver = value;
        }

        private float duration;
        private float interval;

        public SolverInfo(solverShadow solver, float duration, float interval) {
            this.solver = solver;
            this.duration = duration;
            this.interval = interval;
        }

        public float uptime(float totalTime) {
            if (interval > 0f) {
                float procs = (float) Math.floor(totalTime / interval) + 1f;
                return duration * procs / totalTime;
            }
            return 1.0f;
        }
    }

    List<SolverInfo> ssInfo = new ArrayList<SolverInfo>();

    public SolverShadow(Stats BasicStats, Character character) {
        super(BasicStats, character);
        SolverInfo ssi;
        ssi = new SolverInfo(new solverShadow(BasicStats, character), 0f, 0f);
        ssInfo.add(ssi);
        setName(ssi.getSolver().getName());

        // Create an additional solver for each additional haste effect.
        for (SpecialEffect se : BasicStats.specialEffects()) {
            boolean bRelevant = se.getStats().getHasteRating() > 0 || se.getStats().getSpellHaste() > 0;
            for (SpecialEffect s : se.getStats().specialEffects())
                bRelevant |= (s.getStats().getHasteRating() > 0 || s.getStats().getSpellHaste() > 0);
            if (bRelevant) {
                if (Float.isInfinite(se.getDuration()))
                    continue;
                if (se.getTrigger() == Trigger.Use) {
                    // Heroism, Glove Enchants, Troll Racial + other Use trinkets.
                    Stats statsHasteUse = BasicStats.copy();
                    for (SpecialEffect s : se.getStats().specialEffects())
                        if (s.getStats().getHasteRating() > 0) {
                            float Factor = s.getAverageFactor(1.5f / (1f + statsHasteUse.getSpellHaste()), 1f, 0f, se.getDuration());
                            statsHasteUse.setHasteRating(statsHasteUse.getHasteRating() + s.getStats().getHasteRating() * Factor);
                        }
                    statsHasteUse.setHasteRating(statsHasteUse.getHasteRating() + se.getStats().getHasteRating());
                    statsHasteUse.setSpellHaste((1f + statsHasteUse.getSpellHaste())
                            / (1f + StatConversion.getSpellHasteFromRating(BasicStats.getHasteRating()))
                            * (1f + StatConversion.getSpellHasteFromRating(statsHasteUse.getHasteRating()))
                            * (1f + se.getStats().getSpellHaste())
                            - 1f);
                    ssi = new SolverInfo(new solverShadow(statsHasteUse, character), se.getDuration(), se.getCooldown());
                    ssInfo.add(ssi);
                } else if (se.getTrigger() == Trigger.SpellCast ||
                        se.getTrigger() == Trigger.SpellHit ||
                        se.getTrigger() == Trigger.DamageSpellCast ||
                        se.getTrigger() == Trigger.DamageSpellHit) {
                    // Procs.
                    Stats statsHasteProc = BasicStats.copy();
                    statsHasteProc.setHasteRating(statsHasteProc.getHasteRating() + se.getStats().getHasteRating());
                    statsHasteProc.setSpellHaste((1f + statsHasteProc.getSpellHaste())
                            / (1f + StatConversion.getSpellHasteFromRating(BasicStats.getHasteRating()))
                            * (1f + StatConversion.getSpellHasteFromRating(statsHasteProc.getHasteRating()))
                            * (1f + se.getStats().getSpellHaste())
                            - 1f);
                    float cooldown = se.getCooldown();
                    if (cooldown >= 44f)
                        cooldown += 10f;
                    else
                        cooldown += 15f;
                    ssi = new SolverInfo(new solverShadow(statsHasteProc, character), se.getDuration(), cooldown);
                    ssInfo.add(ssi);
                }
            }
        }
    }

    @Override
    public void calculate(CharacterCalculationsShadowPriest calculatedStats) {
        float fightLen = ssInfo.get(0).getSolver().getCalcOpts().getFightLength() * 60f;
        for (SolverInfo si : ssInfo)
            si.getSolver().calculate(calculatedStats);

        float normalUptime = 1.0f;
        for (int x = 1; x < ssInfo.size(); x++) {
            SolverInfo si = ssInfo.get(x);
            float uptime = si.uptime(fightLen);
            normalUptime -= uptime;
            if (normalUptime < 0) {
                uptime += normalUptime;
                normalUptime = 0;
            }
            setDPS(getDPS() + si.getSolver().getDPS() * uptime);
            setSustainDPS(getSustainDPS() + si.getSolver().getSustainDPS() * uptime);
        }
        setDPS(getDPS() + ssInfo.get(0).getSolver().getDPS() * normalUptime);
        setSustainDPS(getSustainDPS() + ssInfo.get(0).getSolver().getSustainDPS() * normalUptime);

        setSpellSimulation(ssInfo.get(0).getSolver().getSpellSimulation());
        setSpellPriority(ssInfo.get(0).getSolver().getSpellPriority());
        setRotation(ssInfo.get(0).getSolver().getRotation());
        setManaSources(ssInfo.get(0).getSolver().getManaSources());

        calculatedStats.setDpsPoints(getSustainDPS());

        // Lets just say that 15% of resilience scales all health by 150%.
        float Resilience = (float) Math.min(15f, StatConversion.getCritReductionFromResilience(getPlayerStats().getResilience()) * 100f) / 15f;
        calculatedStats.setSurvivalPoints(calculatedStats.getBasicStats().getHealth() * (Resilience * 1.5f + 1f) * getCalcOpts().getSurvivability() / 100f);
    }
}

class SolverHoly extends SolverBase {
    // Models Full Rotation
    private Spell swp;
    private Spell swd;
    private Spell mb;
    private Spell dp;
    private Spell sm;
    private Spell pe;

    public SolverHoly(Stats BasicStats, Character character) {
        super(BasicStats, character);

        setSpellPriority(new ArrayList<Spell>(Spell.HolySpellList.size()));
        SpellComparerDpCT _scDpCT = new SpellComparerDpCT();
        SpellComparerDpM _scDpM = new SpellComparerDpM();
        float baseMana = BaseStats.getBaseStats(character).getMana();
        for (String spellname : Spell.HolySpellList) {
            Spell spelltmp = SpellFactory.createSpell(spellname, getPlayerStats(), character, baseMana, getCalcOpts().getPTR());
            if (spelltmp != null) getSpellPriority().add(spelltmp);
        }

        swp = getSpellByName("Shadow Word: Pain");
        swd = getSpellByName("Shadow Word: Death");
        mb = getSpellByName("Mind Blast");
        dp = getSpellByName("Devouring Plague");
        sm = getSpellByName("Smite");
        pe = getSpellByName("Penance");

        // Sort spells and remove any spell with worse mana efficiency than smite.
        getSpellPriority().sort(_scDpM);
        int i = getSpellPriority().indexOf(sm) + 1;
        getSpellPriority().subList(i, getSpellPriority().size()).clear();

        // Sort spells, and remove any spell with worse damage pr cast time than smite.
        getSpellPriority().sort(_scDpCT);
        i = getSpellPriority().indexOf(sm) + 1;
        getSpellPriority().subList(i, getSpellPriority().size()).clear();

        if (character.getPriestTalents().getPenance() > 0)
            setName("Holy Smite w/Penance");
        else
            setName("Holy Smite");
        setRotation("Priority Based:");
        for (Spell spell : getSpellPriority())
            setRotation(getRotation() + "\r\n- " + spell.getName());
        setRotation(getRotation() + "\r\n\r\nMana Buffs:");
    }

    @Override
    public void calculate(CharacterCalculationsShadowPriest calculatedStats) {
        if (getSpellPriority().isEmpty())
            return;

        Stats simStats = getPlayerStats().copy();
        float timer = 0;
        int sequence = getSpellPriority().size() - 1;
        List<Spell> castList = new ArrayList<Spell>();
        int fightLength = (int) getCalcOpts().getFightLength();

        while (timer < (fightLength * 60f)) {
            Spell spell = getCastSpell(timer);
            if (spell == null) {
                timer += 0.1f;
                continue;
            }

            castList.add(spell);

            if (spell.getDebuffDuration() > 0f || spell.getCooldown() > 0f)
                spell.getSpellStatistics().cooldownReset = timer + (spell.getDebuffDuration() > spell.getCooldown() ? spell.getDebuffDuration() : spell.getCooldown());

            timer += (spell.getCastTime() > 0) ? spell.getCastTime() : spell.getGlobalCooldown();

            if (spell == getSpellPriority().get(sequence))
                sequence++;
            else
                sequence = 0;
            if (getSpellPriority().get(sequence) == sm) {
                // Spell sequence just reset, lets take advantage of that.
                int i = getSpellPriority().indexOf(sm);
                castList.subList(castList.size() - i, castList.size()).clear();
                break;
            }
        }

        float shadowHitChance = (float) Math.min(getHitChance() + character.getPriestTalents().getShadowFocus() * 0.01f, 1f);
        timer = 0;
        for (Spell spell : castList) {
            timer += (spell.getCastTime() > 0) ? spell.getCastTime() : spell.getGlobalCooldown();
            float Damage = spell.getAvgDamage();
            float Cost = spell.getManaCost();
            if (spell.getName().equals("Smite")) {
                Damage -= sm.getDpS() * sm.getGlobalCooldown() * (1f - getHitChance() / 100f);
                Cost *= (2f - getHitChance() / 100f);
                if (character.getPriestTalents().isGlyphofSmite())
                    Damage *= (1f + 0.2f * 7 / 10);
            } else if (spell.getName().equals("Shadow Word: Pain")) {
                Damage -= sm.getDpS() * sm.getGlobalCooldown() * (1f - shadowHitChance / 100f);
                Cost *= (2f - shadowHitChance / 100f);
            } else if (spell.getName().equals("Holy Fire")) {
                Damage *= getHitChance() / 100f;
            } else if (spell.getName().equals("Mind Blast")) {
                Damage *= shadowHitChance / 100f;
            } else if (spell.getName().equals("Shadow Word: Death")) {
                Damage *= shadowHitChance / 100f;
            } else if (spell.getName().equals("Penance")) {
                Damage *= getHitChance() / 100f;
            } else if (spell.getName().equals("Devouring Plague")) {
                Damage *= shadowHitChance / 100f;
                Damage *= (1f + simStats.getBonusDiseaseDamageMultiplier());
            }

            Damage *= (1f + simStats.getBonusShadowDamageMultiplier()) * (1f + simStats.getBonusDamageMultiplier()); // If there is shadow bonus damage there is holy bonus damage.
            spell.getSpellStatistics().damageDone += Damage;
            spell.getSpellStatistics().manaUsed += Cost;
            setOverallDamage(getOverallDamage() + Damage);
            setOverallMana(getOverallMana() + Cost);
        }

        setDPS(getOverallDamage() / timer);

        setSustainDPS(getDPS());
        float MPS = getOverallMana() / timer;
        float TimeInFSR = 1f;
        float regen = (calculatedStats.getRegenInFSR() * TimeInFSR + calculatedStats.getRegenOutFSR() * (1f - TimeInFSR)) / 5;
        regen += simStats.getMana() / (fightLength * 60f);
        regen += simStats.getMana() * 0.002f * (getCalcOpts().getReplenishment() / 100f);

        if (MPS > regen && getCalcOpts().getManaAmt() > 0) {
            // Not enough mana, use Mana Potion
            regen += getCalcOpts().getManaAmt() / (fightLength * 60f) * (1f + simStats.getBonusManaPotion());
            setRotation(getRotation() + "\r\n- Used Mana Potion");
        }

        if (MPS > regen) {
            // Not enough mana, use Shadowfiend
            float sf_rat = (getCalcOpts().getShadowfiend() / 100f) / ((5f - character.getPriestTalents().getVeiledShadows() * 1f) * 60f);
            regen += simStats.getMana() * 0.4f * sf_rat;
            setSustainDPS(getSustainDPS() - sm.getDpS() * sf_rat);
            setRotation(getRotation() + "\r\n- Used Shadowfiend");
        }

        if (MPS > regen) {
            // Not enough still, use Hymn of Hope
            float hh_rat = 8f / (60f * 5f);
            regen += simStats.getMana() * 0.02f * hh_rat;
            setSustainDPS(getSustainDPS() - sm.getDpS() * hh_rat);
            setRotation(getRotation() + "\r\n- Used Hymn of Hope");
        }

        setDPS(getDPS() * (1f - getCalcOpts().getTargetLevel() * 0.02f)); // Level based Partial resists.
        setSustainDPS(getSustainDPS() * (1f - getCalcOpts().getTargetLevel() * 0.02f));

        setSustainDPS((MPS < regen) ? getSustainDPS() : (getSustainDPS() * regen / MPS));

        calculatedStats.setDpsPoints(getSustainDPS());
        // If opponent has 25% crit, each 39.42308044 resilience gives -1% damage from dots and -1% chance to be crit. Also reduces crits by 2%.
        // This effectively means you gain 12.5% extra health from removing 12.5% dot and 12.5% crits at resilience cap (492.5 (39.42308044*12.5))
        // In addition, the remaining 12.5% crits are reduced by 25% (12.5%*200%damage*75% = 18.75%)
        // At resilience cap I'd say that your hp's are scaled by 1.125*1.1875 = ~30%. Probably wrong but good enough.
        calculatedStats.setSurvivalPoints(calculatedStats.getBasicStats().getHealth() * getCalcOpts().getSurvivability() / 100f * (1 + 0.3f * calculatedStats.getBasicStats().getResilience() / 492.7885f));
    }
}

// Decrepated
class Solver extends SolverBase {
    private float shadowHitChance;
    private float shadowCritChance;

    public Solver(Stats BasicStats, Character character) {
        super(BasicStats, character);

        shadowHitChance = (float) Math.min(100f, getHitChance() + character.getPriestTalents().getShadowFocus() * 1f);
    }

    @Override
    public void calculate(CharacterCalculationsShadowPriest calculatedStats) {
        // The original body is fully commented out in C# (Solver.cs:1727-1819); the method is a no-op.
    }
}
