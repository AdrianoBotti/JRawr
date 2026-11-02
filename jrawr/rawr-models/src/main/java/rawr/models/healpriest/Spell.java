// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.HealPriest/Spell.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the System.Drawing.Color GraphColor and the per-target color tables are UI-only and
// omitted (the same way the other ported models drop chart colors).
package rawr.models.healpriest;

import rawr.base.stats.BaseStats;
import rawr.base.character.Character;
import rawr.base.util.NetFormat;
import rawr.base.stats.Stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Spell {
    public static class SpellData {
        private final int rank;
        private final int level;
        private final int minHeal;
        private final int maxHeal;
        private final float castTime;

        public SpellData(int rank, int level, int minHeal, int maxHeal, float castTime) {
            this.rank = rank;
            this.level = level;
            this.minHeal = minHeal;
            this.maxHeal = maxHeal;
            this.castTime = castTime;
        }

        public int getRank() {
            return rank;
        }

        public int getLevel() {
            return level;
        }

        public int getMinHeal() {
            return minHeal;
        }

        public int getMaxHeal() {
            return maxHeal;
        }

        public float getCastTime() {
            return castTime;
        }
    }

    public static final float SP2HP = (0.855f / 0.455f);

    private String name;

    public String getName() {
        return name;
    }

    protected void setName(String value) {
        name = value;
    }

    private int rank;

    public int getRank() {
        return rank;
    }

    protected void setRank(int value) {
        rank = value;
    }

    private float minHeal;

    public float getMinHeal() {
        return minHeal;
    }

    protected void setMinHeal(float value) {
        minHeal = value;
    }

    private float maxHeal;

    public float getMaxHeal() {
        return maxHeal;
    }

    protected void setMaxHeal(float value) {
        maxHeal = value;
    }

    private float baseMana;

    public float getBaseMana() {
        return baseMana;
    }

    protected void setBaseMana(float value) {
        baseMana = value;
    }

    private int manaCost;

    public int getManaCost() {
        return manaCost;
    }

    protected void setManaCost(int value) {
        manaCost = value;
    }

    private float baseCastTime;

    public float getBaseCastTime() {
        return baseCastTime;
    }

    protected void setBaseCastTime(float value) {
        baseCastTime = value;
    }

    private float castTime;

    public float getCastTime() {
        return castTime;
    }

    protected void setCastTime(float value) {
        castTime = value;
    }

    private float hotDuration;

    public float getHotDuration() {
        return hotDuration;
    }

    protected void setHotDuration(float value) {
        hotDuration = value;
    }

    private float critChance;

    public float getCritChance() {
        return critChance;
    }

    protected void setCritChance(float value) {
        critChance = value;
    }

    private float critCoef;

    public float getCritCoef() {
        return critCoef;
    }

    protected void setCritCoef(float value) {
        critCoef = value;
    }

    private float healingCoef;

    public float getHealingCoef() {
        return healingCoef;
    }

    protected void setHealingCoef(float value) {
        healingCoef = value;
    }

    private float rankCoef;

    public float getRankCoef() {
        return rankCoef;
    }

    protected void setRankCoef(float value) {
        rankCoef = value;
    }

    private int range;

    public int getRange() {
        return range;
    }

    protected void setRange(int value) {
        range = value;
    }

    private int targets;

    public int getTargets() {
        return targets;
    }

    protected void setTargets(int value) {
        targets = value;
    }

    private float globalCooldown;

    public float getGlobalCooldown() {
        return globalCooldown;
    }

    protected void setGlobalCooldown(float value) {
        globalCooldown = value;
    }

    private float cooldown;

    public float getCooldown() {
        return cooldown;
    }

    protected void setCooldown(float value) {
        cooldown = value;
    }

    public boolean isInstant() {
        return castTime == 0;
    }

    public boolean isHot() {
        return hotDuration > 0;
    }

    public float getAvgHeal() {
        return (minHeal + maxHeal) / 2;
    }

    public float getAvgTotHeal() {
        return getAvgHeal() * (1 - critChance) + getAvgCrit() * critChance;
    }

    public float getHpS() {
        if (isHot())
            return getAvgTotHeal() / hotDuration;

        return getAvgTotHeal() / castTime;
    }

    public float getHpM() {
        return getAvgTotHeal() / manaCost;
    }

    public float getAvgCrit() {
        return (minHeal * critCoef + getMaxCrit()) / 2;
    }

    public float getMaxCrit() {
        return maxHeal * critCoef;
    }

    public Spell(String name, Stats stats, Character character, List<SpellData> spellRankTable, int manaCost) {
        this(name, stats, character, spellRankTable, manaCost, 0);
    }

    public Spell(String name, Stats stats, Character character, List<SpellData> spellRankTable, int manaCost, float coef) {
        this(name, stats, character, spellRankTable, manaCost, coef, 0);
    }

    public Spell(String name, Stats stats, Character character, List<SpellData> spellRankTable, int manaCost, float coef, float hotDuration) {
        for (SpellData sd : spellRankTable)
            if (character.getLevel() >= sd.getLevel()) {
                rank = sd.getRank();
                minHeal = sd.getMinHeal();
                maxHeal = sd.getMaxHeal();
                baseCastTime = sd.getCastTime();
            }
        this.name = name;
        globalCooldown = Math.max(1.5f / (1 + stats.getSpellHaste()), 1.0f);
        if (baseCastTime == 0)
            castTime = globalCooldown;
        else
            castTime = Math.max(baseCastTime / (1 + stats.getSpellHaste()), 1.0f);
        cooldown = castTime;
        this.manaCost = manaCost;
        healingCoef = coef;
        this.hotDuration = hotDuration;
        critChance = stats.getSpellCrit();
        critCoef = 1.5f * (1f + stats.getBonusCritHealMultiplier());
        baseMana = BaseStats.getBaseStats(character.getLevel(), character.getCharacterClass(), character.getRace()).getMana();
    }

    @Override
    public String toString() {
        return NetFormat.fixed(getAvgHeal(), 0)
                + " *HpS: " + NetFormat.fixed(getHpS(), 2)
                + "\r\nHpM: " + NetFormat.fixed(getHpM(), 2)
                + "\r\nMin Heal: " + NetFormat.fixed(minHeal, 0)
                + "\r\nMax Heal: " + NetFormat.fixed(maxHeal, 0)
                + "\r\nAvg Crit: " + NetFormat.fixed(getAvgCrit(), 0)
                + "\r\nMax Crit: " + NetFormat.fixed(getMaxCrit(), 0)
                + "\r\nCast: " + NetFormat.fixed(castTime, 2)
                + "\r\nCost: " + NetFormat.fixed(manaCost, 0)
                + "\r\n" + name;
    }
}

class Renew extends Spell {
    public float InstantHealEffect = 0f;

    private static final List<SpellData> SpellRankTable = new ArrayList<SpellData>(Arrays.asList(
        new SpellData( 1,  8,   45,   45, 0),
        new SpellData( 2, 14,  100,  100, 0),
        new SpellData( 3, 20,  175,  175, 0),
        new SpellData( 4, 26,  245,  245, 0),
        new SpellData( 5, 32,  315,  315, 0),
        new SpellData( 6, 38,  400,  400, 0),
        new SpellData( 7, 44,  510,  510, 0),
        new SpellData( 8, 50,  650,  650, 0),
        new SpellData( 9, 56,  810,  810, 0),
        new SpellData(10, 60,  970,  970, 0),
        new SpellData(11, 65, 1010, 1010, 0),
        new SpellData(12, 70, 1110, 1110, 0),
        new SpellData(13, 75, 1235, 1235, 0),
        new SpellData(14, 80, 1400, 1400, 0)
    ));

    @Override
    public float getAvgTotHeal() {
        return getAvgHeal() + InstantHealEffect * (1f - getCritChance()) + InstantHealEffect * getCritCoef() * getCritChance();
    }

    public Renew(Stats stats, Character character) {
        super("Renew", stats, character, SpellRankTable, 17, 15f / 15f, 15f);
        calculate(stats, character);
    }

    protected void calculate(Stats stats, Character character) {
        setMinHeal((getMinHeal()
                + stats.getSpellPower() * (SP2HP * getHealingCoef() * (1 - getRankCoef())
                + character.getPriestTalents().getEmpoweredRenew() * 0.05f))
                * (1 + character.getPriestTalents().getImprovedRenew() * 0.05f)
                * (1 + character.getPriestTalents().getTwinDisciplines() * 0.01f)
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f));

        if (stats.getRenewDurationIncrease() > 0) {
            setMinHeal(getMinHeal() * (getHotDuration() + stats.getRenewDurationIncrease()) / getHotDuration());
            setHotDuration(getHotDuration() + stats.getRenewDurationIncrease());
        }
        if (character.getPriestTalents().isGlyphofRenew())
            setHotDuration(getHotDuration() - 3);

        InstantHealEffect = character.getPriestTalents().getEmpoweredRenew() * 0.05f * getMinHeal()
                * (1f + stats.getPriestHeal_T9_4pc());
        setMaxHeal(getMinHeal());

        setManaCost((int) Math.floor((getManaCost() / 100f * getBaseMana() - stats.getSpellsManaReduction())
                * (1 - character.getPriestTalents().getMentalAgility() * 0.1f / 3f)));
        setCastTime(0);
    }

    @Override
    public String toString() {
        return NetFormat.fixed(getMinHeal(), 0)
                + " *HpS: " + NetFormat.fixed(getHpS(), 2)
                + "\r\nHpM: " + NetFormat.fixed(getHpM(), 2)
                + "\r\nTick: " + NetFormat.fixed(getMinHeal() / getHotDuration() * 3, 0)
                + "\r\nInstant Heal: " + NetFormat.fixed(InstantHealEffect, 0)
                + "\r\nCost: " + NetFormat.fixed(getManaCost(), 0)
                + "\r\n" + getName();
    }
}

class FlashHeal extends Spell {
    private static final List<SpellData> SpellRankTable = new ArrayList<SpellData>(Arrays.asList(
        new SpellData( 1, 20,  193,  237, 1.5f),
        new SpellData( 1, 26,  258,  314, 1.5f),
        new SpellData( 1, 32,  329,  393, 1.5f),
        new SpellData( 1, 38,  400,  478, 1.5f),
        new SpellData( 1, 44,  518,  616, 1.5f),
        new SpellData( 1, 50,  644,  764, 1.5f),
        new SpellData( 1, 56,  812,  958, 1.5f),
        new SpellData( 1, 61,  913, 1059, 1.5f),
        new SpellData( 1, 67, 1116, 1295, 1.5f),
        new SpellData( 1, 73, 1578, 1832, 1.5f),
        new SpellData( 1, 79, 1887, 2193, 1.5f)
    ));

    public FlashHeal(Stats stats, Character character) {
        super("Flash Heal", stats, character, SpellRankTable, 18, 1.5f / 3.5f);
        calculate(stats, character);
    }

    protected void calculate(Stats stats, Character character) {
        CalculationOptionsHealPriest calcOpts = character.getCalculationOptions() instanceof CalculationOptionsHealPriest
                ? (CalculationOptionsHealPriest) character.getCalculationOptions()
                : new CalculationOptionsHealPriest();

        setMinHeal((getMinHeal()
                + stats.getSpellPower() * ((1 - getRankCoef()) * getHealingCoef() * SP2HP
                + character.getPriestTalents().getEmpoweredHealing() * 0.04f))
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f));

        setMaxHeal((getMaxHeal()
                + stats.getSpellPower() * ((1 - getRankCoef()) * getHealingCoef() * SP2HP
                + character.getPriestTalents().getEmpoweredHealing() * 0.04f))
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f));

        setCastTime(Math.max(1.0f, getBaseCastTime() / (1 + stats.getSpellHaste())));

        setCritChance(stats.getSpellCrit() + character.getPriestTalents().getHolySpecialization() * 0.01f
                + character.getPriestTalents().getImprovedFlashHeal() * 0.04f * calcOpts.getTestOfFaith() / 100f);
        setManaCost((int) Math.floor((getManaCost() / 100f * getBaseMana() - stats.getSpellsManaReduction())
                * (1 - character.getPriestTalents().getImprovedFlashHeal() * 0.05f - (character.getPriestTalents().isGlyphofFlashHeal() ? 0.1f : 0.0f))));
    }

    public void SurgeOfLight() {
        setCritChance(0f);
        setCritCoef(1.0f);
        setManaCost(0);
        setCastTime(0);
    }
}

class Heal extends Spell {
    private static final List<SpellData> SpellRankTable = new ArrayList<SpellData>(Arrays.asList(
        new SpellData( 1,  1,   46,   56, 1.5f),
        new SpellData( 2,  4,   71,   85, 2.0f),
        new SpellData( 3, 10,  135,  157, 2.5f),
        new SpellData( 1, 16,  295,  341, 3.0f),
        new SpellData( 2, 22,  429,  491, 3.0f),
        new SpellData( 3, 28,  566,  642, 3.0f),
        new SpellData( 4, 34,  712,  804, 3.0f),
        new SpellData( 1, 40,  899, 1013, 3.0f),
        new SpellData( 2, 46, 1149, 1289, 3.0f),
        new SpellData( 3, 52, 1437, 1609, 3.0f),
        new SpellData( 4, 58, 1798, 2006, 3.0f),
        new SpellData( 5, 60, 1966, 2194, 3.0f),
        new SpellData( 6, 63, 2074, 2410, 3.0f),
        new SpellData( 7, 40, 2414, 2803, 3.0f),
        new SpellData( 8, 40, 3395, 3945, 3.0f),
        new SpellData( 9, 40, 3950, 4590, 3.0f)
    ));

    public Heal(Stats stats, Character character) {
        super("Heal", stats, character, SpellRankTable, 32, 3f / 3.5f);
        if (character.getLevel() < 16)
            setName("Lesser " + getName());
        else if (character.getLevel() >= 40)
            setName("Greater " + getName());
        calculate(stats, character);
    }

    protected void calculate(Stats stats, Character character) {
        setMinHeal((getMinHeal()
                + stats.getSpellPower() * ((1 - getRankCoef()) * getHealingCoef() * SP2HP
                + character.getPriestTalents().getEmpoweredHealing() * 0.08f))
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f)
                * (1 + stats.getBonusGHHealingMultiplier()));

        setMaxHeal((getMaxHeal()
                + stats.getSpellPower() * ((1 - getRankCoef()) * getHealingCoef() * SP2HP
                + character.getPriestTalents().getEmpoweredHealing() * 0.08f))
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f)
                * (1 + stats.getBonusGHHealingMultiplier()));

        setManaCost((int) Math.floor((getManaCost() / 100f * getBaseMana() - stats.getSpellsManaReduction())
                * (1 - character.getPriestTalents().getImprovedHealing() * 0.05f)
                * (1 - stats.getGreaterHealCostReduction())));

        setCritChance(stats.getSpellCrit() + character.getPriestTalents().getHolySpecialization() * 0.01f);
        setCastTime(Math.max(1.0f, (getBaseCastTime() - character.getPriestTalents().getDivineFury() * 0.1f)
                / (1 + stats.getSpellHaste())));
    }
}

class PrayerOfHealing extends Spell {
    private static final List<SpellData> SpellRankTable = new ArrayList<SpellData>(Arrays.asList(
        new SpellData(1, 30,  301,  321, 3.0f),
        new SpellData(2, 40,  444,  472, 3.0f),
        new SpellData(3, 50,  657,  695, 3.0f),
        new SpellData(4, 60,  939,  991, 3.0f),
        new SpellData(5, 60,  997, 1053, 3.0f),
        new SpellData(6, 68, 1251, 1322, 3.0f),
        new SpellData(7, 76, 2091, 2209, 3.0f)
    ));

    @Override
    public float getAvgTotHeal() {
        return (getAvgHeal() * (1 - getCritChance()) + getAvgCrit() * getCritChance()) * getTargets();
    }

    @Override
    public float getHpS() {
        return getAvgTotHeal() / getCastTime();
    }

    @Override
    public float getHpM() {
        return getAvgTotHeal() / getManaCost();
    }

    public PrayerOfHealing(Stats stats, Character character, int targets, float haste) {
        super(String.format(java.util.Locale.ROOT, "Prayer of Healing (%d targets)", targets), stats, character, SpellRankTable, 48, 2f / 3.5f * 0.5f);
        calculate(stats, character, targets, haste);
    }

    public PrayerOfHealing(Stats stats, Character character) {
        this(stats, character, 5, 0f);
    }

    public PrayerOfHealing(Stats stats, Character character, int targets) {
        this(stats, character, targets, 0f);
    }

    protected void calculate(Stats stats, Character character, int targets, float haste) {
        setTargets(targets);
        setRange(30);

        setMinHeal((getMinHeal()
                + stats.getSpellPower() * SP2HP * getHealingCoef() * (1 - getRankCoef()))
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getDivineProvidence() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f));
        setMaxHeal((getMaxHeal()
                + stats.getSpellPower() * SP2HP * getHealingCoef() * (1 - getRankCoef()))
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getDivineProvidence() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f));

        setManaCost((int) Math.floor((getManaCost() / 100f * getBaseMana() - stats.getSpellsManaReduction())
                * (1 - character.getPriestTalents().getHealingPrayers() * 0.1f)
                * (1 - stats.getBonusPoHManaCostReductionMultiplier())));

        setCastTime(Math.max(1.0f, (getBaseCastTime() * (1f - haste)) / (1 + stats.getSpellHaste())));
        setCritChance(stats.getSpellCrit()
                + character.getPriestTalents().getHolySpecialization() * 0.01f
                + stats.getPrayerOfHealingExtraCrit());
        setRange((int) Math.round(getRange() * (1 + character.getPriestTalents().getHolyReach() * 0.1f)));
    }

    @Override
    public String toString() {
        return NetFormat.fixed(getAvgHeal(), 0)
                + " *HpS(3): " + NetFormat.fixed(getAvgHeal() * 3 / getCastTime(), 2)
                + "\r\nHpS(4): " + NetFormat.fixed(getAvgHeal() * 4 / getCastTime(), 2)
                + "\r\nHpS(5): " + NetFormat.fixed(getAvgHeal() * 5 / getCastTime(), 2)
                + "\r\nHpM(3): " + NetFormat.fixed(getAvgHeal() * 3 / getManaCost(), 2)
                + "\r\nHpM(4): " + NetFormat.fixed(getAvgHeal() * 4 / getManaCost(), 2)
                + "\r\nHpM(5): " + NetFormat.fixed(getAvgHeal() * 5 / getManaCost(), 2)
                + "\r\nMin Heal: " + NetFormat.fixed(getMinHeal(), 0)
                + "\r\nMax Heal: " + NetFormat.fixed(getMaxHeal(), 0)
                + "\r\nAvg Crit: " + NetFormat.fixed(getAvgCrit(), 0)
                + "\r\nMax Crit: " + NetFormat.fixed(getMaxCrit(), 0)
                + "\r\nCast: " + NetFormat.fixed(getCastTime(), 2)
                + "\r\nCost: " + NetFormat.fixed(getManaCost(), 0)
                + "\r\nRange: " + getRange()
                + "\r\n" + NetFormat.fixed(getAvgHeal() * 4 / getCastTime(), 2);
    }
}

class CircleOfHealing extends Spell {
    private static final List<SpellData> SpellRankTable = new ArrayList<SpellData>(Arrays.asList(
        new SpellData(1, 50,  343,  379, 0f),
        new SpellData(1, 56,  403,  445, 0f),
        new SpellData(1, 60,  458,  506, 0f),
        new SpellData(1, 65,  518,  572, 0f),
        new SpellData(1, 70,  572,  632, 0f),
        new SpellData(1, 75,  825,  911, 0f),
        new SpellData(1, 80,  958, 1058, 0f)
    ));

    @Override
    public float getAvgTotHeal() {
        return (getAvgHeal() * (1 - getCritChance()) + getAvgCrit() * getCritChance()) * getTargets();
    }

    @Override
    public float getHpS() {
        return getAvgTotHeal() / getGlobalCooldown();
    }

    @Override
    public float getHpM() {
        return getAvgTotHeal() / getManaCost();
    }

    public CircleOfHealing(Stats stats, Character character, int targets) {
        super(String.format(java.util.Locale.ROOT, "Circle of Healing (%d targets)", targets), stats, character, SpellRankTable, 21, 1.5f / 3.5f * 0.5f);
        setTargets(targets);
        calculate(stats, character);
    }

    public CircleOfHealing(Stats stats, Character character) {
        this(stats, character, character.getPriestTalents().isGlyphofCircleOfHealing() ? 6 : 5);
    }

    protected void calculate(Stats stats, Character character) {
        if (character.getPriestTalents().getCircleOfHealing() == 0) {
            setMinHeal(0);
            setMaxHeal(0);
            return;
        }
        setRange(15);
        setMinHeal((getMinHeal()
                + stats.getSpellPower() * SP2HP * getHealingCoef() * (1 - getRankCoef()))
                * (1 + character.getPriestTalents().getTwinDisciplines() * 0.01f)
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getDivineProvidence() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f)
                * (1 + stats.getPriestHeal_T10_4pc() * 2f));
        setMaxHeal((getMaxHeal()
                + stats.getSpellPower() * SP2HP * getHealingCoef() * (1 - getRankCoef()))
                * (1 + character.getPriestTalents().getTwinDisciplines() * 0.01f)
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getDivineProvidence() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f)
                * (1 + stats.getPriestHeal_T10_4pc() * 2f));

        setCritChance(stats.getSpellCrit() + character.getPriestTalents().getHolySpecialization() * 0.01f);

        setManaCost((int) Math.floor((getManaCost() / 100f * getBaseMana() - stats.getSpellsManaReduction())
                * (1 - character.getPriestTalents().getMentalAgility() * 0.1f / 3f)));

        setRange((int) Math.round(getRange() * (1 + character.getPriestTalents().getHolyReach() * 0.1f)));
        setCastTime(0);
    }

    @Override
    public String toString() {
        return NetFormat.fixed(getAvgHeal(), 0)
                + " *HpS(3): " + NetFormat.fixed(getAvgHeal() * 3 / getGlobalCooldown(), 2)
                + "\r\nHpS(4): " + NetFormat.fixed(getAvgHeal() * 4 / getGlobalCooldown(), 2)
                + "\r\nHpS(5): " + NetFormat.fixed(getAvgHeal() * 5 / getGlobalCooldown(), 2)
                + "\r\nHpM(3): " + NetFormat.fixed(getAvgHeal() * 3 / getManaCost(), 2)
                + "\r\nHpM(4): " + NetFormat.fixed(getAvgHeal() * 4 / getManaCost(), 2)
                + "\r\nHpM(5): " + NetFormat.fixed(getAvgHeal() * 5 / getManaCost(), 2)
                + "\r\nMin Heal: " + NetFormat.fixed(getMinHeal(), 0)
                + "\r\nMax Heal: " + NetFormat.fixed(getMaxHeal(), 0)
                + "\r\nAvg Crit: " + NetFormat.fixed(getAvgCrit(), 0)
                + "\r\nMax Crit: " + NetFormat.fixed(getMaxCrit(), 0)
                + "\r\nCost: " + NetFormat.fixed(getManaCost(), 0)
                + "\r\nRange: " + getRange()
                + "\r\n" + NetFormat.fixed(getAvgHeal() * 4 / getGlobalCooldown(), 2);
    }
}

class HolyNova extends Spell {
    private static final List<SpellData> SpellRankTable = new ArrayList<SpellData>(Arrays.asList(
        new SpellData(1, 20,  52,  60, 0f),
        new SpellData(2, 28,  86,  98, 0f),
        new SpellData(3, 36, 121, 139, 0f),
        new SpellData(4, 44, 161, 187, 0f),
        new SpellData(5, 52, 235, 271, 0f),
        new SpellData(6, 60, 302, 350, 0f),
        new SpellData(7, 68, 384, 446, 0f),
        new SpellData(8, 75, 611, 709, 0f),
        new SpellData(9, 80, 713, 827, 0f)
    ));

    @Override
    public float getAvgTotHeal() {
        return (getAvgHeal() * (1 - getCritChance()) + getAvgCrit() * getCritChance()) * getTargets();
    }

    @Override
    public float getHpS() {
        return getAvgTotHeal() / getGlobalCooldown();
    }

    @Override
    public float getHpM() {
        return getAvgTotHeal() / getManaCost();
    }

    public HolyNova(Stats stats, Character character, int targets) {
        super(String.format(java.util.Locale.ROOT, "Holy Nova (%d targets)", targets), stats, character, SpellRankTable, 20, 0.16f);
        setTargets(targets);
        calculate(stats, character);
    }

    public HolyNova(Stats stats, Character character) {
        this(stats, character, 5);
    }

    protected void calculate(Stats stats, Character character) {
        setRange(10);

        setMinHeal((getMinHeal()
                + stats.getSpellPower() * SP2HP * getHealingCoef() * (1 - getRankCoef()))
                * (1 + character.getPriestTalents().getTwinDisciplines() * 0.01f)
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getDivineProvidence() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f)
                * (character.getPriestTalents().isGlyphofHolyNova() ? 1.4f : 1.0f));
        setMaxHeal((getMaxHeal()
                + stats.getSpellPower() * SP2HP * getHealingCoef() * (1 - getRankCoef()))
                * (1 + character.getPriestTalents().getTwinDisciplines() * 0.01f)
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getDivineProvidence() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f)
                * (character.getPriestTalents().isGlyphofHolyNova() ? 1.4f : 1.0f));

        setCritChance(stats.getSpellCrit() + character.getPriestTalents().getHolySpecialization() * 0.01f);

        setManaCost((int) Math.floor((getManaCost() / 100f * getBaseMana() - stats.getSpellsManaReduction())
                * (1 - character.getPriestTalents().getMentalAgility() * 0.1f / 3f)));

        setRange((int) Math.round(getRange() * (1 + character.getPriestTalents().getHolyReach() * 0.1f)));
        setCastTime(0);
    }

    @Override
    public String toString() {
        return NetFormat.fixed(getAvgHeal(), 0)
                + " *HpS(3): " + NetFormat.fixed(getAvgHeal() * 3 / getGlobalCooldown(), 2)
                + "\r\nHpS(4): " + NetFormat.fixed(getAvgHeal() * 4 / getGlobalCooldown(), 2)
                + "\r\nHpS(5): " + NetFormat.fixed(getAvgHeal() * 5 / getGlobalCooldown(), 2)
                + "\r\nHpM(3): " + NetFormat.fixed(getAvgHeal() * 3 / getManaCost(), 2)
                + "\r\nHpM(4): " + NetFormat.fixed(getAvgHeal() * 4 / getManaCost(), 2)
                + "\r\nHpM(5): " + NetFormat.fixed(getAvgHeal() * 5 / getManaCost(), 2)
                + "\r\nMin Heal: " + NetFormat.fixed(getMinHeal(), 0)
                + "\r\nMax Heal: " + NetFormat.fixed(getMaxHeal(), 0)
                + "\r\nAvg Crit: " + NetFormat.fixed(getAvgCrit(), 0)
                + "\r\nMax Crit: " + NetFormat.fixed(getMaxCrit(), 0)
                + "\r\nCost: " + NetFormat.fixed(getManaCost(), 0)
                + "\r\nRange: " + getRange()
                + "\r\n" + NetFormat.fixed(getAvgHeal() * 4 / getGlobalCooldown(), 2);
    }
}

class BindingHeal extends Spell {
    private static final List<SpellData> SpellRankTable = new ArrayList<SpellData>(Arrays.asList(
        new SpellData(1, 64, 1042, 1338, 1.5f),
        new SpellData(2, 72, 1619, 2081, 1.5f),
        new SpellData(3, 78, 1952, 2508, 1.5f)
    ));

    @Override
    public float getAvgTotHeal() {
        return (getAvgHeal() * (1 - getCritChance()) + getAvgCrit() * getCritChance()) * 2;
    }

    @Override
    public float getHpS() {
        return getAvgTotHeal() / getCastTime();
    }

    @Override
    public float getHpM() {
        return getAvgTotHeal() / getManaCost();
    }

    public BindingHeal(Stats stats, Character character) {
        super("Binding Heal", stats, character, SpellRankTable, 27, 1.5f / 3.5f);
        calculate(stats, character);
    }

    protected void calculate(Stats stats, Character character) {
        setMinHeal((getMinHeal()
                + stats.getSpellPower() * ((1 - getRankCoef()) * getHealingCoef() * SP2HP
                + character.getPriestTalents().getEmpoweredHealing() * 0.04f))
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getDivineProvidence() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f));

        setMaxHeal((getMaxHeal()
                + stats.getSpellPower() * ((1 - getRankCoef()) * getHealingCoef() * SP2HP
                + character.getPriestTalents().getEmpoweredHealing() * 0.04f))
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getDivineProvidence() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f));

        setManaCost((int) Math.floor((getManaCost() / 100f * getBaseMana() - stats.getSpellsManaReduction())));

        setCritChance(stats.getSpellCrit() + character.getPriestTalents().getHolySpecialization() * 0.01f);
        setCastTime(Math.max(1.0f, getBaseCastTime() / (1 + stats.getSpellHaste())));
    }

    @Override
    public String toString() {
        return NetFormat.fixed(getAvgHeal(), 0)
                + " *HpS(2): " + NetFormat.fixed(getHpS(), 2)
                + "\r\nHpM (2): " + NetFormat.fixed(getHpM(), 2)
                + "\r\nMin Heal: " + NetFormat.fixed(getMinHeal(), 0)
                + "\r\nMax Heal: " + NetFormat.fixed(getMaxHeal(), 0)
                + "\r\nAvg Crit: " + NetFormat.fixed(getAvgCrit(), 0)
                + "\r\nMax Crit: " + NetFormat.fixed(getMaxCrit(), 0)
                + "\r\nCast Time: " + NetFormat.fixed(getCastTime(), 2)
                + "\r\nCost: " + NetFormat.fixed(getManaCost(), 0)
                + "\r\n" + getName();
    }
}

class PrayerOfMending extends Spell {
    private static final List<SpellData> SpellRankTable = new ArrayList<SpellData>(Arrays.asList(
        new SpellData(1, 68,  800,  800, 0),
        new SpellData(2, 74,  905,  905, 0),
        new SpellData(3, 79, 1043, 1043, 0)
    ));

    @Override
    public float getAvgTotHeal() {
        return (getAvgHeal() * (1 - getCritChance()) + getAvgCrit() * getCritChance()) * getTargets();
    }

    @Override
    public float getHpS() {
        return getAvgTotHeal() / getGlobalCooldown();
    }

    @Override
    public float getHpM() {
        return getAvgTotHeal() / getManaCost();
    }

    public PrayerOfMending(Stats stats, Character character, int targets) {
        super(String.format(java.util.Locale.ROOT, "Prayer of Mending (%d targets)", targets), stats, character, SpellRankTable, 15, 1.5f / 3.5f);
        setTargets(targets);
        calculate(stats, character);
    }

    public PrayerOfMending(Stats stats, Character character) {
        this(stats, character, 5 + (int) stats.getPrayerOfMendingExtraJumps());
    }

    protected void calculate(Stats stats, Character character) {
        setRange(15);

        setMinHeal((getMinHeal()
                + stats.getSpellPower() * SP2HP * getHealingCoef() * (1 - getRankCoef()))
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getTwinDisciplines() * 0.01f)
                * (1 + character.getPriestTalents().getDivineProvidence() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f)
                * (1 + stats.getPriestHeal_T9_2pc()));
        setMaxHeal((getMaxHeal()
                + stats.getSpellPower() * SP2HP * getHealingCoef() * (1 - getRankCoef()))
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getTwinDisciplines() * 0.01f)
                * (1 + character.getPriestTalents().getDivineProvidence() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f)
                * (1 + stats.getPriestHeal_T9_2pc()));

        setManaCost((int) Math.floor((getManaCost() / 100f * getBaseMana() - stats.getSpellsManaReduction())
                * (1 - character.getPriestTalents().getHealingPrayers() * 0.1f)
                * (1 - character.getPriestTalents().getMentalAgility() * 0.1f / 3f)));
        setRange((int) Math.round(getRange() * (1 + character.getPriestTalents().getHolyReach() * 0.1f)));

        setCritChance(stats.getSpellCrit() + character.getPriestTalents().getHolySpecialization() * 0.01f);
        setCastTime(0);
        setCooldown(10.0f - (character.getPriestTalents().getDivineProvidence() * 0.6f));
    }

    @Override
    public String toString() {
        return NetFormat.fixed(getAvgHeal(), 0)
                + " *HpM(1): " + NetFormat.fixed(getAvgHeal() / getManaCost(), 2)
                + "\r\nHpM(2): " + NetFormat.fixed(getAvgHeal() * 2 / getManaCost(), 2)
                + "\r\nHpM 3): " + NetFormat.fixed(getAvgHeal() * 3 / getManaCost(), 2)
                + "\r\nHpM(4): " + NetFormat.fixed(getAvgHeal() * 4 / getManaCost(), 2)
                + "\r\nHpM(5): " + NetFormat.fixed(getAvgHeal() * 5 / getManaCost(), 2)
                + "\r\nCrit: " + NetFormat.fixed(getAvgCrit(), 0)
                + "\r\nCost: " + NetFormat.fixed(getManaCost(), 0)
                + "\r\nRange: " + getRange()
                + "\r\n" + getName();
    }
}

class PowerWordShield extends Spell {
    static final List<SpellData> SpellRankTable = new ArrayList<SpellData>(Arrays.asList(
        new SpellData( 1,  6,   44,   44, 0f),
        new SpellData( 2, 12,   88,   88, 0f),
        new SpellData( 3, 18,  158,  158, 0f),
        new SpellData( 4, 24,  234,  234, 0f),
        new SpellData( 5, 30,  301,  301, 0f),
        new SpellData( 6, 36,  381,  381, 0f),
        new SpellData( 7, 42,  484,  484, 0f),
        new SpellData( 8, 48,  605,  605, 0f),
        new SpellData( 9, 54,  763,  763, 0f),
        new SpellData(10, 60,  942,  942, 0f),
        new SpellData(11, 65, 1125, 1125, 0f),
        new SpellData(12, 70, 1265, 1265, 0f),
        new SpellData(13, 75, 1920, 1920, 0f),
        new SpellData(14, 80, 2230, 2230, 0f)
    ));

    @Override
    public float getHpS() {
        return getAvgHeal() / getGlobalCooldown();
    }

    public PowerWordShield(Stats stats, Character character) {
        super("Power Word Shield", stats, character, SpellRankTable, 23, 1.5f / 3.5f);
        calculate(stats, character);
    }

    protected void calculate(Stats stats, Character character) {
        setMinHeal((getMinHeal()
                + stats.getSpellPower() * SP2HP * getHealingCoef() * (1 - getRankCoef())
                + stats.getSpellPower() * character.getPriestTalents().getBorrowedTime() * 0.08f)
                * (1 + character.getPriestTalents().getTwinDisciplines() * 0.01f
                    + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getImprovedPowerWordShield() * 0.05f)
                * (1 + stats.getPriestHeal_T10_4pc()));
        setMaxHeal(getMinHeal());

        setManaCost((int) Math.floor((getManaCost() / 100f * getBaseMana() - stats.getSpellsManaReduction())
                * (1 - character.getPriestTalents().getMentalAgility() * 0.1f / 3f
                     - character.getPriestTalents().getSoulWarding() * 0.15f)));

        setCritChance(0.0f);
        setCritCoef(1.0f);

        if (character.getPriestTalents().getSoulWarding() > 0)
            setCooldown(0.0f);
        else
            setCooldown(4f);

        setCastTime(0.0f);
    }

    @Override
    public String toString() {
        return NetFormat.fixed(getMinHeal(), 0)
                + " *HpM: " + NetFormat.fixed(getHpM(), 2)
                + "\r\nCost: " + NetFormat.fixed(getManaCost(), 0)
                + "\r\n" + getName();
    }
}

class Lightwell extends Spell {
    private static final List<SpellData> SpellRankTable = new ArrayList<SpellData>(Arrays.asList(
        new SpellData(1, 40,  801,  801, 0.5f),
        new SpellData(2, 50, 1164, 1164, 0.5f),
        new SpellData(3, 60, 1599, 1599, 0.5f),
        new SpellData(4, 70, 2361, 2361, 0.5f),
        new SpellData(5, 75, 3915, 3915, 0.5f),
        new SpellData(6, 80, 4620, 4620, 0.5f)
    ));

    @Override
    public float getHpS() {
        return getAvgHeal() / getHotDuration();
    }

    public Lightwell(Stats stats, Character character) {
        super("Lightwell", stats, character, SpellRankTable, 17, 1f, 6);
        calculate(stats, character);
    }

    protected void calculate(Stats stats, Character character) {
        setMinHeal((getMinHeal()
                + stats.getSpellPower() * SP2HP * getHealingCoef() * (1 - getRankCoef()))
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (character.getPriestTalents().isGlyphofLightwell() ? 1.2f : 1.0f));
        setMaxHeal(getMinHeal());

        setCritChance(0f);
        setCritCoef(1.0f);

        setManaCost((int) Math.floor((getManaCost() / 100f * getBaseMana() - stats.getSpellsManaReduction())));

        setCastTime(0);
        setCooldown(3.0f * 60.0f);
    }

    @Override
    public String toString() {
        return NetFormat.fixed(getMinHeal(), 0)
                + " *HpS: " + NetFormat.fixed(getHpS(), 2)
                + "\r\nHpM: " + NetFormat.fixed(getHpM(), 2)
                + "\r\nTick: " + NetFormat.fixed(getMinHeal() / getHotDuration() * 2, 0)
                + "\r\nCost: " + NetFormat.fixed(getManaCost(), 0)
                + "\r\n" + getName();
    }
}

class Penance extends Spell {
    private static final List<SpellData> SpellRankTable = new ArrayList<SpellData>(Arrays.asList(
        new SpellData(1, 60,  670*3,  756*3, 2f),
        new SpellData(2, 70,  805*3,  909*3, 2f),
        new SpellData(3, 75, 1278*3, 1442*3, 2f),
        new SpellData(4, 80, 1484*3, 1676*3, 2f)
    ));

    public Penance(Stats stats, Character character) {
        super("Penance", stats, character, SpellRankTable, 16, 3f / 3.5f);
        calculate(stats, character);
    }

    protected void calculate(Stats stats, Character character) {
        if (character.getPriestTalents().getPenance() == 0) {
            setMinHeal(0);
            setMaxHeal(0);
            return;
        }

        setMinHeal((getMinHeal()
                + stats.getSpellPower() * SP2HP * (1 - getRankCoef()) * getHealingCoef())
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getTwinDisciplines() * 0.01f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f));

        setMaxHeal((getMaxHeal()
                + stats.getSpellPower() * SP2HP * (1 - getRankCoef()) * getHealingCoef())
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getTwinDisciplines() * 0.01f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f));

        setManaCost((int) Math.floor((getManaCost() / 100f * getBaseMana() - stats.getSpellsManaReduction())
                * (1 - character.getPriestTalents().getImprovedHealing() * 0.05f)));

        setCastTime(Math.max(1.0f, getBaseCastTime() / (1 + stats.getSpellHaste())));

        setCritChance(stats.getSpellCrit() + character.getPriestTalents().getHolySpecialization() * 0.01f);
        setCooldown((12.0f - (character.getPriestTalents().isGlyphofPenance() ? 2 : 0)) * (1f - character.getPriestTalents().getAspiration() * 0.1f));
    }

    @Override
    public String toString() {
        return NetFormat.fixed(getAvgHeal(), 0)
                + " *HpS: " + NetFormat.fixed(getHpS(), 2)
                + "\r\nHpM: " + NetFormat.fixed(getHpM(), 2)
                + "\r\nMin Tick: " + NetFormat.fixed(getMinHeal() / 3, 2)
                + "\r\nMax Tick: " + NetFormat.fixed(getMaxHeal() / 3, 2)
                + "\r\nAvg Tick: " + NetFormat.fixed(getAvgHeal() / 3, 2)
                + "\r\nMax Crit Tick: " + NetFormat.fixed(getMaxCrit() / 3, 2)
                + "\r\nAvg Crit Tick: " + NetFormat.fixed(getAvgCrit() / 3, 2)
                + "\r\nAvg Heal: " + NetFormat.fixed(getAvgHeal(), 2)
                + "\r\nMin Heal: " + NetFormat.fixed(getMinHeal(), 0)
                + "\r\nMax Heal: " + NetFormat.fixed(getMaxHeal(), 0)
                + "\r\nAvg Crit: " + NetFormat.fixed(getAvgCrit(), 0)
                + "\r\nMax Crit: " + NetFormat.fixed(getMaxCrit(), 0)
                + "\r\nCast: " + NetFormat.fixed(getCastTime(), 2)
                + "\r\nCost: " + NetFormat.fixed(getManaCost(), 0)
                + "\r\n" + getName();
    }
}

class GiftOfTheNaaru extends Spell {
    public GiftOfTheNaaru(Stats stats, Character character) {
        super("Gift of the Naaru", stats, character, new ArrayList<SpellData>(Arrays.asList(
                new SpellData(1, 1, character.getLevel() * 15 + 35, character.getLevel() * 15 + 35, 1.5f))), 0, 15f / 15f, 15f);
        calculate(stats, character);
    }

    protected void calculate(Stats stats, Character character) {
        setMinHeal((getMinHeal()
                + stats.getSpellPower() * SP2HP * getHealingCoef())
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f));
        setMaxHeal(getMinHeal());

        setManaCost((int) Math.floor(getManaCost() / 100f * getBaseMana()));

        setCritChance(0.0f);
        setCritCoef(1.0f);

        setCastTime(Math.max(1.0f, getBaseCastTime() / (1 + stats.getSpellHaste())));

        setCooldown(3.0f * 60.0f);
    }

    @Override
    public String toString() {
        return NetFormat.fixed(getMinHeal(), 0)
                + " *HpS: " + NetFormat.fixed(getHpS(), 2)
                + "\r\nHpM: " + NetFormat.fixed(getHpM(), 2)
                + "\r\nTick: " + NetFormat.fixed(getMinHeal() / getHotDuration() * 3, 0)
                + "\r\n" + getName();
    }
}

class DivineHymn extends Spell {
    private static final List<SpellData> SpellRankTable = new ArrayList<SpellData>(Arrays.asList(
        new SpellData(1, 80,  3000*4, 3850*4, 8f)
    ));

    public DivineHymn(Stats stats, Character character) {
        super("Divine Hymn", stats, character, SpellRankTable, 63, 8f / 3.5f, 8f);
        calculate(stats, character);
    }

    @Override
    public float getAvgTotHeal() {
        return (getAvgHeal() * (1 - getCritChance()) + getAvgCrit() * getCritChance()) * 3;
    }

    protected void calculate(Stats stats, Character character) {
        setMinHeal((getMinHeal()
                + stats.getSpellPower() * SP2HP * getHealingCoef() * (1 - getRankCoef()))
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getDivineProvidence() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f));
        setMaxHeal((getMaxHeal()
                + stats.getSpellPower() * SP2HP * getHealingCoef() * (1 - getRankCoef()))
                * (1 + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1 + character.getPriestTalents().getDivineProvidence() * 0.02f)
                * (1 + character.getPriestTalents().getSpiritualHealing() * 0.02f)
                * (1 + character.getPriestTalents().getBlessedResilience() * 0.01f));

        setManaCost((int) Math.floor((getManaCost() / 100f * getBaseMana() - stats.getSpellsManaReduction())
                * (1 - character.getPriestTalents().getMentalAgility() * 0.1f / 3f)));

        setCastTime(Math.max(1.0f, getBaseCastTime() / (1 + stats.getSpellHaste())));
        setCritChance(stats.getSpellCrit() + character.getPriestTalents().getHolySpecialization() * 0.01f);
        setRange((int) Math.round(getRange() * (1 + character.getPriestTalents().getHolyReach() * 0.1f)));
    }

    @Override
    public String toString() {
        return NetFormat.fixed(getAvgTotHeal(), 0)
                + " *HpS: " + NetFormat.fixed(getHpS(), 2)
                + "\r\nHpM: " + NetFormat.fixed(getHpM(), 2)
                + "\r\nTick: " + NetFormat.fixed(getAvgTotHeal() / getHotDuration() * 2 / 3, 0)
                + "\r\nCost: " + NetFormat.fixed(getManaCost(), 0)
                + "\r\n" + getName();
    }
}

class Dispel extends Spell {
    public Dispel(Stats stats, Character character) {
        super("Dispel", stats, character, new ArrayList<SpellData>(Arrays.asList(new SpellData(1, 30, 0, 0, 0))), 12, 0f);
        calculate(stats, character);
    }

    protected void calculate(Stats stats, Character character) {
        setMinHeal(0);
        setMaxHeal(0);
        setManaCost((int) Math.floor((getManaCost() / 100f * getBaseMana() - stats.getSpellsManaReduction())
                * (1 - character.getPriestTalents().getMentalAgility() * 0.1f / 3f)
                * (1 - character.getPriestTalents().getAbsolution() * 0.05f)));
        setCritChance(0.0f);
        setCritCoef(1.0f);

        setCastTime(0.0f);
    }

    @Override
    public String toString() {
        return getName();
    }
}

class MassDispel extends Spell {
    public MassDispel(Stats stats, Character character) {
        super("MassDispel", stats, character, new ArrayList<SpellData>(Arrays.asList(new SpellData(1, 30, 0, 0, 1.5f))), 36, 0f);
        calculate(stats, character);
    }

    protected void calculate(Stats stats, Character character) {
        setMinHeal(0);
        setMaxHeal(0);
        setManaCost((int) Math.floor((getManaCost() / 100f * getBaseMana() - stats.getSpellsManaReduction())
                * (character.getPriestTalents().isGlyphofMassDispel() ? 0.7f : 1.0f)));
        setCritChance(0.0f);
        setCritCoef(1.0f);
    }

    @Override
    public String toString() {
        return getName();
    }
}

class Resurrection extends Spell {
    public Resurrection(Stats stats, Character character) {
        super("Resurrection", stats, character, new ArrayList<SpellData>(Arrays.asList(new SpellData(1, 10, 0, 0, 10f))), 60, 0);
        calculate(stats, character);
    }

    protected void calculate(Stats stats, Character character) {
        setMinHeal(0);
        setMaxHeal(0);
        setManaCost((int) Math.floor((getManaCost() / 100f * getBaseMana() - stats.getSpellsManaReduction())));
        setCritChance(0.0f);
        setCritCoef(1.0f);
        setCastTime(Math.max(1.0f, getBaseCastTime() / (1 + stats.getSpellHaste())));
    }

    @Override
    public String toString() {
        return "- *Cast Time: " + NetFormat.fixed(getCastTime(), 2)
                + "\r\nCost: " + NetFormat.fixed(getManaCost(), 0)
                + "\r\n" + getName();
    }
}
