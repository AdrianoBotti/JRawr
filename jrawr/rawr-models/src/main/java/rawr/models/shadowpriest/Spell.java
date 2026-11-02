// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ShadowPriest/Spell.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the System.Drawing.Color GraphColor and the per-spell color arguments are UI-only and
// omitted (the same way the other ported models drop chart colors).
package rawr.models.shadowpriest;

import rawr.base.character.Character;
import rawr.base.stats.MagicSchool;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

import java.util.List;

final class SpellFactory {
    static Spell createSpell(String name, Stats stats, Character character, float baseMana, boolean ptr) {
        rawr.base.talents.PriestTalents talents = character.getPriestTalents();
        switch (name) {
            case "Vampiric Embrace": return (talents.getVampiricEmbrace() > 0 ? new VampiricEmbrace(stats, character, baseMana, ptr) : null);
            case "Vampiric Touch": return (talents.getVampiricTouch() > 0 ? new VampiricTouch(stats, character, baseMana, ptr) : null);
            case "Shadow Word: Pain": return new ShadowWordPain(stats, character, baseMana, ptr);
            case "Devouring Plague": return new DevouringPlague(stats, character, baseMana, ptr);
            case "Mind Blast": return new MindBlast(stats, character, baseMana, ptr);
            case "Shadow Word: Death": return new ShadowWordDeath(stats, character, baseMana, ptr);
            case "Mind Flay": return (talents.getMindFlay() > 0 ? new MindFlay(stats, character, baseMana, ptr) : null);
            case "Smite": return new Smite(stats, character, baseMana, ptr);
            case "Holy Fire": return new HolyFire(stats, character, baseMana, ptr);
            case "Penance": return (talents.getPenance() > 0 ? new Penance(stats, character, baseMana, ptr) : null);
            default: return null;
        }
    }
}

class SpellStatistics {
    public float critCount;
    public float missCount;
    public float hitCount;
    public float cooldownReset;
    public float damageDone;
    public float manaUsed;

    public void reset() {
        critCount = 0;
        missCount = 0;
        hitCount = 0;
        cooldownReset = 0;
        damageDone = 0;
        manaUsed = 0;
    }
}

public class Spell {
    public static class SpellData {
        private final int rank;
        private final int level;
        private final int minDamage;
        private final int maxDamage;

        public SpellData(int rank, int level, int minDamage, int maxDamage) {
            this.rank = rank;
            this.level = level;
            this.minDamage = minDamage;
            this.maxDamage = maxDamage;
        }

        public int getRank() {
            return rank;
        }

        public int getLevel() {
            return level;
        }

        public int getMinDamage() {
            return minDamage;
        }

        public int getMaxDamage() {
            return maxDamage;
        }
    }

    public static final List<String> ShadowSpellList = List.of(
            "Vampiric Touch", "Devouring Plague", "Mind Blast", "Shadow Word: Pain", "Mind Flay", "Shadow Word: Death");
    public static final List<String> HolySpellList = List.of(
            "Penance", "Holy Fire", "Devouring Plague", "Shadow Word: Pain", "Mind Blast", "Shadow Word: Death", "Smite");

    private String name;

    public String getName() {
        return name;
    }

    protected void setName(String value) {
        name = value;
    }

    private MagicSchool magicSchool;

    public MagicSchool getMagicSchool() {
        return magicSchool;
    }

    protected void setMagicSchool(MagicSchool value) {
        magicSchool = value;
    }

    private int rank;

    public int getRank() {
        return rank;
    }

    protected void setRank(int value) {
        rank = value;
    }

    private float baseMinDamage;

    public float getBaseMinDamage() {
        return baseMinDamage;
    }

    protected void setBaseMinDamage(float value) {
        baseMinDamage = value;
    }

    private float minDamage;

    public float getMinDamage() {
        return minDamage;
    }

    protected void setMinDamage(float value) {
        minDamage = value;
    }

    private float baseMaxDamage;

    public float getBaseMaxDamage() {
        return baseMaxDamage;
    }

    protected void setBaseMaxDamage(float value) {
        baseMaxDamage = value;
    }

    private float maxDamage;

    public float getMaxDamage() {
        return maxDamage;
    }

    protected void setMaxDamage(float value) {
        maxDamage = value;
    }

    private float baseDamageCoef;

    public float getBaseDamageCoef() {
        return baseDamageCoef;
    }

    protected void setBaseDamageCoef(float value) {
        baseDamageCoef = value;
    }

    private float damageCoef;

    public float getDamageCoef() {
        return damageCoef;
    }

    protected void setDamageCoef(float value) {
        damageCoef = value;
    }

    private float baseRange;

    public float getBaseRange() {
        return baseRange;
    }

    protected void setBaseRange(float value) {
        baseRange = value;
    }

    private int range;

    public int getRange() {
        return range;
    }

    protected void setRange(int value) {
        range = value;
    }

    private int baseManaCost;

    public int getBaseManaCost() {
        return baseManaCost;
    }

    protected void setBaseManaCost(int value) {
        baseManaCost = value;
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

    private float critChance;

    public float getCritChance() {
        return critChance;
    }

    protected void setCritChance(float value) {
        critChance = value;
    }

    private float baseCritCoef;

    public float getBaseCritCoef() {
        return baseCritCoef;
    }

    protected void setBaseCritCoef(float value) {
        baseCritCoef = value;
    }

    private float critCoef;

    public float getCritCoef() {
        return critCoef;
    }

    protected void setCritCoef(float value) {
        critCoef = value;
    }

    private float baseDebuffDuration;

    public float getBaseDebuffDuration() {
        return baseDebuffDuration;
    }

    protected void setBaseDebuffDuration(float value) {
        baseDebuffDuration = value;
    }

    private float debuffDuration;

    public float getDebuffDuration() {
        return debuffDuration;
    }

    protected void setDebuffDuration(float value) {
        debuffDuration = value;
    }

    private int baseDebuffTicks;

    public int getBaseDebuffTicks() {
        return baseDebuffTicks;
    }

    protected void setBaseDebuffTicks(int value) {
        baseDebuffTicks = value;
    }

    private int debuffTicks;

    public int getDebuffTicks() {
        return debuffTicks;
    }

    protected void setDebuffTicks(int value) {
        debuffTicks = value;
    }

    private int targets;

    public int getTargets() {
        return targets;
    }

    protected void setTargets(int value) {
        targets = value;
    }

    private float cooldown;

    public float getCooldown() {
        return cooldown;
    }

    protected void setCooldown(float value) {
        cooldown = value;
    }

    private float baseCooldown;

    public float getBaseCooldown() {
        return baseCooldown;
    }

    protected void setBaseCooldown(float value) {
        baseCooldown = value;
    }

    private float globalCooldown;

    public float getGlobalCooldown() {
        return globalCooldown;
    }

    protected void setGlobalCooldown(float value) {
        globalCooldown = value;
    }

    private SpellStatistics spellStatistics;

    public SpellStatistics getSpellStatistics() {
        return spellStatistics;
    }

    protected void setSpellStatistics(SpellStatistics value) {
        spellStatistics = value;
    }

    protected float baseMana;
    protected boolean ptr;

    public float getAvgHit() {
        return (minDamage + maxDamage) / 2f;
    }

    public float getAvgDamage() {
        return getAvgHit() * (1f - critChance) + getAvgCrit() * critChance;
    }

    public float getDpCT() {
        return getAvgDamage() / (castTime > 0 ? castTime : globalCooldown);
    }

    public float getDpS() {
        return getAvgDamage() / (debuffDuration > 0 ? debuffDuration : (castTime > 0 ? castTime : globalCooldown));
    }

    public float getDpM() {
        return getAvgDamage() / manaCost;
    }

    public float getAvgCrit() {
        return (getMinCrit() + getMaxCrit()) / 2f;
    }

    public float getMaxCrit() {
        return maxDamage * critCoef;
    }

    public float getMinCrit() {
        return minDamage * critCoef;
    }

    public Spell(String name, Stats stats, Character character, List<SpellData> spellRankTable, int manaCost, float castTime, float critCoef, float dotDuration, float damageCoef, int range, float cooldown, MagicSchool magicSchool, float baseMana, boolean ptr) {
        this.damageCoef = damageCoef;
        this.debuffDuration = dotDuration;
        for (SpellData sd : spellRankTable) {
            if (character.getLevel() >= sd.getLevel()) {
                rank = sd.getRank();
                baseMinDamage = minDamage = sd.getMinDamage();
                baseMaxDamage = maxDamage = sd.getMaxDamage();
            }
        }
        this.name = name;
        baseManaCost = manaCost;
        this.manaCost = manaCost;
        baseCastTime = castTime;
        this.castTime = castTime;
        baseDebuffDuration = dotDuration;
        baseDebuffTicks = debuffTicks = (int) (dotDuration / 3f);
        baseDamageCoef = damageCoef;
        baseRange = range;
        this.range = range;
        critChance = 0f;
        baseCritCoef = this.critCoef = critCoef;
        globalCooldown = (float) Math.max(1f, 1.5f / (1 + stats.getSpellHaste()));
        baseCooldown = this.cooldown = cooldown;
        spellStatistics = new SpellStatistics();
        this.magicSchool = magicSchool;
        this.baseMana = baseMana;
        this.ptr = ptr;
    }

    public Spell(String name, Stats stats, Character character, List<SpellData> spellRankTable, int manaCost, float castTime, float critCoef, float dotDuration, float damageCoef, int range, float cooldown, float baseMana, boolean ptr) {
        this(name, stats, character, spellRankTable, manaCost, castTime, critCoef, dotDuration, damageCoef, range, cooldown, MagicSchool.Shadow, baseMana, ptr);
    }

    public void calculate(Stats stats, Character character) {
    }

    public void recalcHaste(Stats stats, float addedHasteRating) {
        float newHaste = 1f + stats.getSpellHaste();
        if (addedHasteRating > 0f) {
            newHaste /= (1f + StatConversion.getSpellHasteFromRating(stats.getHasteRating()));
            newHaste *= (1f + StatConversion.getSpellHasteFromRating(stats.getHasteRating() + addedHasteRating));
        }
        if (castTime > 0f)
            castTime = (float) Math.max(1f, baseCastTime / newHaste);
        globalCooldown = (float) Math.max(1f, 1.5f / newHaste);
    }

    public float waitTime(float timer) {
        if (debuffDuration > 0)
            return spellStatistics.cooldownReset - castTime - timer;
        return spellStatistics.cooldownReset - timer;
    }

    @Override
    public String toString() {
        if (debuffDuration > 0f) {
            return String.format(java.util.Locale.ROOT, "%s *DpS: %s\r\nDpCT: %s\r\nDpM: %s\r\nTick: %s-%s%s\r\nDuration: %ss\r\nCost: %s\r\n%s",
                    NetFormat.fixed(getAvgDamage(), 0),
                    NetFormat.fixed(getDpS(), 2),
                    NetFormat.fixed(getDpCT(), 2),
                    NetFormat.fixed(getDpM(), 2),
                    NetFormat.fixed((float) Math.floor(getAvgHit() / debuffDuration * 3), 0),
                    NetFormat.fixed((float) Math.ceil(getAvgHit() / debuffDuration * 3), 0),
                    (critCoef > 1f ? String.format(java.util.Locale.ROOT, "\r\nTick Crit: %s-%s\r\nCrit Chance: %s%%",
                            NetFormat.fixed((float) Math.floor(getAvgCrit() / debuffDuration * 3), 0),
                            NetFormat.fixed((float) Math.floor(getAvgCrit() / debuffDuration * 3), 0),
                            NetFormat.fixed(critChance * 100f, 2)) : ""),
                    NetFormat.fixed(debuffDuration, 2),
                    NetFormat.fixed(manaCost, 0),
                    name);
        }
        return String.format(java.util.Locale.ROOT, "%s *DpS: %s\r\nDpCT: %s\r\nDpM: %s\r\nHit: %s-%s, Avg %s\r\nCrit: %s-%s, Avg %s\r\nCrit Chance: %s%%\r\nCast: %s\r\nCost: %s\r\n%s",
                NetFormat.fixed(getAvgDamage(), 0),
                NetFormat.fixed(getDpS(), 2),
                NetFormat.fixed(getDpCT(), 2),
                NetFormat.fixed(getDpM(), 2),
                NetFormat.fixed(minDamage, 0), NetFormat.fixed(maxDamage, 0), NetFormat.fixed(getAvgHit(), 0),
                NetFormat.fixed(getMinCrit(), 0), NetFormat.fixed(getMaxCrit(), 0), NetFormat.fixed(getAvgCrit(), 0),
                NetFormat.fixed(critChance * 100f, 2),
                NetFormat.fixed(castTime, 2),
                NetFormat.fixed(manaCost, 0),
                name);
    }
}

class ShadowWordPain extends Spell {
    static final List<SpellData> SpellRankTable = List.of(
            new SpellData(1, 4, 30, 30),
            new SpellData(2, 10, 60, 60),
            new SpellData(3, 18, 120, 120),
            new SpellData(4, 26, 210, 210),
            new SpellData(5, 34, 330, 330),
            new SpellData(6, 42, 462, 462),
            new SpellData(7, 50, 606, 606),
            new SpellData(8, 58, 768, 768),
            new SpellData(9, 65, 906, 906),
            new SpellData(10, 70, 1116, 1116),
            new SpellData(11, 75, 1176, 1176),
            new SpellData(12, 80, 1380, 1380)
    );

    public ShadowWordPain(Stats stats, Character character, float baseMana, boolean ptr) {
        super("Shadow Word: Pain", stats, character, SpellRankTable, 22, 0, 0, 18f, 1.098f, 30, 0f, baseMana, ptr);
        calculate(stats, character);
    }

    @Override
    public void calculate(Stats stats, Character character) {
        float modifiers
                = (1f + character.getPriestTalents().getTwinDisciplines() * 0.01f
                    + character.getPriestTalents().getDarkness() * 0.02f
                    + character.getPriestTalents().getImprovedShadowWordPain() * 0.03f
                    + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1f + ((character.getPriestTalents().getShadowWeaving() > 0) ? 0.1f : 0.0f))
                * (1f + character.getPriestTalents().getShadowform() * 0.15f);

        setMaxDamage((getBaseMinDamage()
                + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef())
                * modifiers);
        setMinDamage(getMaxDamage());

        setDebuffDuration(getBaseDebuffDuration() + stats.getSWPDurationIncrease());
        setMinDamage(getMinDamage() * (getDebuffDuration() / getBaseDebuffDuration()));
        setMaxDamage(getMinDamage());
        setDebuffTicks(getBaseDebuffTicks() + (int) (stats.getSWPDurationIncrease() / 3f));

        setManaCost((int) Math.floor((getBaseManaCost() / 100f * baseMana - stats.getSpellsManaReduction())
                * (1f - character.getPriestTalents().getShadowFocus() * 0.02f)
                * (1f - character.getPriestTalents().getMentalAgility() * 0.1f / 3f)));

        if (character.getPriestTalents().getShadowform() > 0) {
            setCritChance(stats.getSpellCrit()
                    + character.getPriestTalents().getMindMelt() * 0.03f
                    + stats.getPriestDPS_T10_2pc());
            for (rawr.base.effects.SpecialEffect se : stats.specialEffects()) {
                if (se.getStats().getCritRating() > 0 && se.getTrigger() == rawr.base.effects.Trigger.Use)
                    setCritChance(getCritChance() + StatConversion.getSpellCritFromRating(se.getStats().getCritRating()));
            }
            setCritCoef((1.5f * (1f + stats.getBonusSpellCritMultiplier()) - 1f) * 2f + 1f);
        }

        setRange((int) Math.rint(getBaseRange() * (1 + character.getPriestTalents().getShadowReach() * 0.1f)));
    }
}

class VampiricTouch extends Spell {
    static final List<SpellData> SpellRankTable = List.of(
            new SpellData(1, 50, 450, 450),
            new SpellData(2, 60, 600, 600),
            new SpellData(3, 70, 650, 650),
            new SpellData(4, 75, 735, 735),
            new SpellData(5, 80, 935, 935)
    );

    public VampiricTouch(Stats stats, Character character, float baseMana, boolean ptr) {
        super("Vampiric Touch", stats, character, SpellRankTable, 16, 1.5f, 0, 15f, 1.9685f, 30, 0f, baseMana, ptr);
        calculate(stats, character);
    }

    @Override
    public void calculate(Stats stats, Character character) {
        if (character.getPriestTalents().getVampiricTouch() == 0) {
            setMinDamage(0);
            setMaxDamage(0);
            return;
        }

        float modifiers
                = (1f + character.getPriestTalents().getDarkness() * 0.02f)
                * (1f + ((character.getPriestTalents().getShadowWeaving() > 0) ? 0.1f : 0.0f))
                * (1f + character.getPriestTalents().getShadowform() * 0.15f);

        setMinDamage((getBaseMinDamage() + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef())
                * modifiers);

        setDebuffDuration(getBaseDebuffDuration() + stats.getPriestDPS_T9_2pc());
        setDebuffTicks(getBaseDebuffTicks() + (int) (stats.getPriestDPS_T9_2pc() / 3f));

        setMinDamage(getMinDamage() * (getDebuffDuration() / getBaseDebuffDuration()));
        setMaxDamage(getMinDamage());

        setManaCost((int) Math.floor((getBaseManaCost() / 100f * baseMana - stats.getSpellsManaReduction())
                * (1f - character.getPriestTalents().getShadowFocus() * 0.02f)));

        setCastTime((float) Math.max(1f, getBaseCastTime() / (1f + stats.getSpellHaste())));

        if (character.getPriestTalents().getShadowform() > 0) {
            setCritChance(stats.getSpellCrit()
                    + character.getPriestTalents().getMindMelt() * 0.03f
                    + stats.getPriestDPS_T10_2pc());
            setCritCoef((1.5f * (1f + stats.getBonusSpellCritMultiplier()) - 1f) * 2f + 1f);
            setDebuffDuration(getDebuffDuration() / (1f + stats.getSpellHaste()));
        }

        setRange((int) Math.rint(getBaseRange() * (1f + character.getPriestTalents().getShadowReach() * 0.1f)));
    }
}

class MindBlast extends Spell {
    static final List<SpellData> SpellRankTable = List.of(
            new SpellData(1, 10, 39, 43),
            new SpellData(2, 16, 72, 78),
            new SpellData(3, 22, 116, 120),
            new SpellData(4, 28, 167, 177),
            new SpellData(5, 34, 217, 231),
            new SpellData(6, 40, 279, 297),
            new SpellData(7, 46, 346, 366),
            new SpellData(8, 52, 425, 449),
            new SpellData(9, 58, 503, 531),
            new SpellData(10, 63, 557, 587),
            new SpellData(11, 69, 711, 752),
            new SpellData(12, 74, 837, 883),
            new SpellData(13, 79, 992, 1048)
    );

    public MindBlast(Stats stats, Character character, float baseMana, boolean ptr) {
        super("Mind Blast", stats, character, SpellRankTable, 17, 1.5f, 1.5f, 0, 1.5f / 3.5f, 30, 8f, baseMana, ptr);
        calculate(stats, character);
    }

    @Override
    public void calculate(Stats stats, Character character) {
        rawr.base.talents.PriestTalents talents = character.getPriestTalents();

        float modifiers
                = (1f + talents.getDarkness() * 0.02f
                    + talents.getFocusedPower() * 0.02f)
                * (1f + ((talents.getShadowWeaving() > 0) ? 0.1f : 0f))
                * (1f + stats.getBonusMindBlastMultiplier())
                * (1f + talents.getTwistedFaith() * 0.02f)
                * (1f + talents.getShadowform() * 0.15f);

        setDamageCoef(getBaseDamageCoef() * (1f + talents.getMisery() * 0.05f));

        setMinDamage((getBaseMinDamage() + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef())
                * modifiers);

        setMaxDamage((getBaseMaxDamage() + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef())
                * modifiers);

        setCastTime((float) Math.max(1f, getBaseCastTime() / (1 + stats.getSpellHaste())));

        setCritCoef((getBaseCritCoef() * (1f + stats.getBonusSpellCritMultiplier()) - 1f) * (1f + character.getPriestTalents().getShadowPower() * 0.2f) + 1f);

        setCritChance(stats.getSpellCrit() + character.getPriestTalents().getMindMelt() * 0.02f);

        setCooldown(getBaseCooldown() - talents.getImprovedMindBlast() * 0.5f);

        setManaCost((int) Math.floor((getBaseManaCost() / 100f * baseMana - stats.getSpellsManaReduction())
                * (1f - character.getPriestTalents().getShadowFocus() * 0.02f)
                * (1f - character.getPriestTalents().getFocusedMind() * 0.05f)
                * (1f - stats.getMindBlastCostReduction())));

        setRange((int) Math.rint(getBaseRange() * (1 + character.getPriestTalents().getShadowReach() * 0.1f)));
    }
}

class ShadowWordDeath extends Spell {
    static final List<SpellData> SpellRankTable = List.of(
            new SpellData(1, 62, 450, 522),
            new SpellData(2, 70, 572, 664),
            new SpellData(3, 75, 639, 741),
            new SpellData(4, 80, 750, 870)
    );

    public ShadowWordDeath(Stats stats, Character character, float baseMana, boolean ptr) {
        super("Shadow Word: Death", stats, character, SpellRankTable, 12, 0, 1.5f, 0, 1.5f / 3.5f, 30, 12f, baseMana, ptr);
        calculate(stats, character);
    }

    @Override
    public void calculate(Stats stats, Character character) {
        float modifiers
                = (1f + character.getPriestTalents().getTwinDisciplines() * 0.01f
                    + character.getPriestTalents().getDarkness() * 0.02f
                    + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1f + ((character.getPriestTalents().getShadowWeaving() > 0) ? 0.1f : 0.0f))
                * (1f + character.getPriestTalents().getShadowform() * 0.15f);

        setMinDamage((getBaseMinDamage() + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef())
                * modifiers);

        setMaxDamage((getBaseMaxDamage() + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef())
                * modifiers);

        setCritChance(stats.getSpellCrit() + stats.getShadowWordDeathCritIncrease());

        setCritCoef((getBaseCritCoef() * (1f + stats.getBonusSpellCritMultiplier()) - 1f) * (1f + character.getPriestTalents().getShadowPower() * 0.2f) + 1f);

        setManaCost((int) Math.floor((getBaseManaCost() / 100f * baseMana - stats.getSpellsManaReduction())
                * (1f - character.getPriestTalents().getShadowFocus() * 0.02f)
                * (1f - character.getPriestTalents().getMentalAgility() * 0.1f / 3f)));

        setRange((int) Math.rint(getBaseRange() * (1 + character.getPriestTalents().getShadowReach() * 0.1f)));
    }
}

class MindFlay extends Spell {
    static final List<SpellData> SpellRankTable = List.of(
            new SpellData(1, 20, 45, 45),
            new SpellData(2, 28, 108, 108),
            new SpellData(3, 36, 159, 159),
            new SpellData(4, 44, 222, 222),
            new SpellData(5, 52, 282, 282),
            new SpellData(6, 60, 363, 363),
            new SpellData(7, 68, 450, 450),
            new SpellData(8, 74, 492, 492),
            new SpellData(9, 80, 588, 588)
    );

    public MindFlay(Stats stats, Character character, float baseMana, boolean ptr) {
        super("Mind Flay", stats, character, SpellRankTable, 9, 3f, 1.5f, 0, 0.771f, 30, 0f, baseMana, ptr);
        calculate(stats, character);
        int ticks = (int) getBaseDebuffDuration();
        setBaseDebuffTicks(ticks);
        setDebuffTicks(ticks);
    }

    @Override
    public void recalcHaste(Stats stats, float addedHasteRating) {
        float newHaste = 1f + stats.getSpellHaste();
        if (addedHasteRating > 0f) {
            newHaste /= (1f + StatConversion.getSpellHasteFromRating(stats.getHasteRating()));
            newHaste *= (1f + StatConversion.getSpellHasteFromRating(stats.getHasteRating() + addedHasteRating));
        }
        if (getCastTime() > 0f)
            setCastTime((float) Math.max(1f, (getBaseCastTime() - stats.getPriestDPS_T10_4pc()) / newHaste));
        setGlobalCooldown((float) Math.max(1f, 1.5f / newHaste));
    }

    @Override
    public void calculate(Stats stats, Character character) {
        rawr.base.talents.PriestTalents talents = character.getPriestTalents();

        if (talents.getMindFlay() == 0) {
            setMinDamage(0);
            setMaxDamage(0);
            return;
        }

        float modifiers
                = (1f + talents.getTwinDisciplines() * 0.01f
                    + talents.getDarkness() * 0.02f
                    + talents.getFocusedPower() * 0.02f)
                * (1f + ((talents.getShadowWeaving() > 0) ? 0.1f : 0.0f))
                * (1f + character.getPriestTalents().getTwistedFaith() * 0.02f
                    + (character.getPriestTalents().isGlyphofMindFlay() ? 0.1f : 0.0f))
                * (1f + talents.getShadowform() * 0.15f);

        setDamageCoef(getBaseDamageCoef() * (1f + talents.getMisery() * 0.05f));

        setMaxDamage((getBaseMinDamage() + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef())
                * modifiers);
        setMinDamage(getMaxDamage());

        setManaCost((int) Math.floor((getBaseManaCost() / 100f * baseMana - stats.getSpellsManaReduction())
                * (1f - talents.getShadowFocus() * 0.02f)
                * (1f - talents.getFocusedMind() * 0.05f)));

        setCritChance(stats.getSpellCrit() + talents.getMindMelt() * 0.02f + stats.getPriestDPS_T9_4pc());

        setCritCoef((getBaseCritCoef() * (1f + stats.getBonusSpellCritMultiplier()) - 1f) * (1f + talents.getShadowPower() * 0.2f) + 1f);

        setCastTime((float) Math.max(1f, (getBaseCastTime() - stats.getPriestDPS_T10_4pc()) / (1 + stats.getSpellHaste())));
        setRange((int) Math.rint(getBaseRange() * (1f + talents.getShadowReach() * 0.1f)));
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s *DpS: %s\r\nDpCT: %s\r\nDpM: %s\r\nTick Hit: %s-%s\r\nTick Crit: %s-%s\r\nCrit Chance: %s%%\r\nCast: %s\r\nCost: %s\r\n%s",
                NetFormat.fixed(getAvgDamage(), 0),
                NetFormat.fixed(getDpS(), 2),
                NetFormat.fixed(getDpCT(), 2),
                NetFormat.fixed(getDpM(), 2),
                NetFormat.fixed((float) Math.floor(getAvgHit() / 3), 0), NetFormat.fixed((float) Math.ceil(getAvgHit() / 3), 0),
                NetFormat.fixed((float) Math.floor(getAvgCrit() / 3), 0), NetFormat.fixed((float) Math.ceil(getAvgCrit() / 3), 0),
                NetFormat.fixed(getCritChance() * 100f, 2),
                NetFormat.fixed(getCastTime(), 2),
                NetFormat.fixed(getManaCost(), 0),
                getName());
    }
}

class PowerWordShield extends Spell {
    static final List<SpellData> SpellRankTable = List.of(
            new SpellData(1, 6, 44, 44),
            new SpellData(2, 12, 88, 88),
            new SpellData(3, 18, 158, 158),
            new SpellData(4, 24, 234, 234),
            new SpellData(5, 30, 301, 301),
            new SpellData(6, 36, 381, 381),
            new SpellData(7, 42, 484, 484),
            new SpellData(8, 48, 605, 605),
            new SpellData(9, 54, 763, 763),
            new SpellData(10, 60, 942, 942),
            new SpellData(11, 65, 1125, 1125),
            new SpellData(12, 70, 1265, 1265),
            new SpellData(13, 75, 1920, 1920),
            new SpellData(14, 80, 2230, 2230)
    );

    public PowerWordShield(Stats stats, Character character, float baseMana, boolean ptr) {
        super("Power Word: Shield", stats, character, SpellRankTable, 23, 0, 0, 0, 1.5f / 3.5f, 30, 4f, baseMana, ptr);
        calculate(stats, character);
    }

    @Override
    public void calculate(Stats stats, Character character) {
        setMaxDamage((getBaseMinDamage() * (1 + character.getPriestTalents().getImprovedPowerWordShield() * 0.05f)
                + stats.getSpellPower() * 1.88f * getDamageCoef()
                + stats.getSpellPower() * character.getPriestTalents().getBorrowedTime() * 0.04f)
                * (1f + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1f + character.getPriestTalents().getTwinDisciplines() * 0.01f)
                * (1f + character.getPriestTalents().getImprovedPowerWordShield() * 0.05f));
        setMinDamage(getMaxDamage());

        setManaCost((int) Math.floor((getBaseManaCost() / 100f * baseMana - stats.getSpellsManaReduction())
                * (1f - character.getPriestTalents().getMentalAgility() * 0.1f / 3f
                      - character.getPriestTalents().getSoulWarding() * 0.15f)));
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s *Cost: %s\r\n%s",
                NetFormat.fixed(getMinDamage(), 0),
                NetFormat.fixed(getManaCost(), 0),
                getName());
    }
}

class VampiricEmbrace extends Spell {
    static final List<SpellData> SpellRankTable = List.of(
            new SpellData(1, 30, 0, 0)
    );

    private float healthConvertionCoef;

    public float getHealthConvertionCoef() {
        return healthConvertionCoef;
    }

    protected void setHealthConvertionCoef(float value) {
        healthConvertionCoef = value;
    }

    public VampiricEmbrace(Stats stats, Character character, float baseMana, boolean ptr) {
        super("Vampiric Embrace", stats, character, SpellRankTable, 2, 0, 0, 60f, 0, 30, 10f, baseMana, ptr);
        healthConvertionCoef = 0.15f;
        calculate(stats, character);
    }

    @Override
    public void calculate(Stats stats, Character character) {
        if (character.getPriestTalents().getVampiricEmbrace() == 0) {
            healthConvertionCoef = 0;
            return;
        }
        healthConvertionCoef *= (1 + character.getPriestTalents().getImprovedVampiricEmbrace() / 3f);
        setManaCost((int) Math.floor((getBaseManaCost() / 100f * baseMana - stats.getSpellsManaReduction())
                * (1f - character.getPriestTalents().getShadowFocus() * 0.02f)
                * (1f - character.getPriestTalents().getMentalAgility() * 0.1f / 3f)));
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s *Cost: %s\r\n%s",
                NetFormat.fixed(healthConvertionCoef * 100, 0) + "%",
                NetFormat.fixed(getManaCost(), 0),
                getName());
    }
}

class DevouringPlague extends Spell {
    static final List<SpellData> SpellRankTable = List.of(
            new SpellData(1, 20, 152, 152),
            new SpellData(2, 28, 272, 272),
            new SpellData(3, 36, 400, 400),
            new SpellData(4, 44, 544, 544),
            new SpellData(5, 52, 712, 712),
            new SpellData(6, 60, 904, 904),
            new SpellData(7, 68, 1088, 1088),
            new SpellData(8, 73, 1144, 1144),
            new SpellData(9, 79, 1376, 1376)
    );

    public ImprovedDevouringPlague ImprovedDP;

    public DevouringPlague(Stats stats, Character character, float baseMana, boolean ptr) {
        super("Devouring Plague", stats, character, SpellRankTable, 25, 0, 0, 24, 1.48f, 30, 0, baseMana, ptr);
        if (character.getPriestTalents().getImprovedDevouringPlague() > 0)
            ImprovedDP = new ImprovedDevouringPlague(stats, character, SpellRankTable, baseMana, ptr);

        calculate(stats, character);
    }

    @Override
    public void calculate(Stats stats, Character character) {
        if (ImprovedDP != null)
            ImprovedDP.calculate(stats, character);

        float modifiers
                = (1f + character.getPriestTalents().getTwinDisciplines() * 0.01f
                    + character.getPriestTalents().getDarkness() * 0.02f
                    + character.getPriestTalents().getFocusedPower() * 0.02f
                    + character.getPriestTalents().getImprovedDevouringPlague() * 0.05f
                    + stats.getDevouringPlagueBonusDamage())
                * (1f + ((character.getPriestTalents().getShadowWeaving() > 0) ? 0.1f : 0f))
                * (1f + stats.getBonusDiseaseDamageMultiplier())
                * (1f + character.getPriestTalents().getShadowform() * 0.15f);

        setMaxDamage((getBaseMinDamage()
                + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef())
                * modifiers);
        setMinDamage(getMaxDamage());

        setDebuffDuration(getBaseDebuffDuration());
        setDebuffTicks(getBaseDebuffTicks());

        setManaCost((int) Math.floor((getBaseManaCost() / 100f * baseMana - stats.getSpellsManaReduction())
                * (1f - character.getPriestTalents().getShadowFocus() * 0.02f)
                * (1f - character.getPriestTalents().getMentalAgility() * 0.1f / 3f)));

        if (character.getPriestTalents().getShadowform() > 0) {
            setCritChance(stats.getSpellCrit()
                    + character.getPriestTalents().getMindMelt() * 0.03f
                    + stats.getPriestDPS_T10_2pc());
            setCritCoef((1.5f * (1f + stats.getBonusSpellCritMultiplier()) - 1f) * 2f + 1f);
            setDebuffDuration(getDebuffDuration() / (1f + stats.getSpellHaste()));
        }

        setRange((int) Math.rint(getBaseRange() * (1 + character.getPriestTalents().getShadowReach() * 0.1f)));
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s *DpS: %s\r\nDpCT: %s\r\nDpM: %s\r\nTick: %s-%s%s\r\nDuration: %ss\r\nCost: %s\r\n%s",
                NetFormat.fixed(getAvgDamage(), 0),
                NetFormat.fixed(getDpS(), 2),
                NetFormat.fixed(getDpCT(), 2),
                NetFormat.fixed(getDpM(), 2),
                NetFormat.fixed((float) Math.floor(getAvgHit() / getDebuffDuration() * 3), 0),
                NetFormat.fixed((float) Math.ceil(getAvgHit() / getDebuffDuration() * 3), 0),
                (getCritCoef() > 1f ? String.format(java.util.Locale.ROOT, "\r\nTick Crit: %s-%s\r\nCrit Chance: %s%%",
                        NetFormat.fixed((float) Math.floor(getAvgCrit() / getDebuffDuration() * 3), 0),
                        NetFormat.fixed((float) Math.floor(getAvgCrit() / getDebuffDuration() * 3), 0),
                        NetFormat.fixed(getCritChance() * 100f, 2)) : ""),
                NetFormat.fixed(getDebuffDuration(), 2),
                NetFormat.fixed(getManaCost(), 0),
                getName());
    }
}

class ImprovedDevouringPlague extends Spell {
    public ImprovedDevouringPlague(Stats stats, Character character, List<SpellData> spellRankTable, float baseMana, boolean ptr) {
        super("Imp. Devouring Plague", stats, character, spellRankTable, 25, 0, 0, 24, 1.48f, 30, 24, baseMana, ptr);
        calculate(stats, character);
    }

    @Override
    public void calculate(Stats stats, Character character) {
        float modifiers
                = (1f + character.getPriestTalents().getTwinDisciplines() * 0.01f
                    + character.getPriestTalents().getDarkness() * 0.02f
                    + character.getPriestTalents().getFocusedPower() * 0.02f
                    + character.getPriestTalents().getImprovedDevouringPlague() * 0.1f)
                * (1f + ((character.getPriestTalents().getShadowWeaving() > 0) ? 0.05f : 0f))
                * (1f + stats.getBonusDiseaseDamageMultiplier())
                * (1f + character.getPriestTalents().getShadowform() * 0.15f);

        setMaxDamage((getBaseMinDamage()
                + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef())
                * (character.getPriestTalents().getImprovedDevouringPlague() * 0.1f)
                * modifiers
                * (1f + stats.getDevouringPlagueBonusDamage()));
        setMinDamage(getMaxDamage());

        setCritChance(stats.getSpellCrit());
        setCritCoef(1.5f * (1f + stats.getBonusSpellCritMultiplier()));

        setManaCost(0);
        setDebuffDuration(0f);

        setRange(0);
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s *Hit: %s\r\nCrit: %s\r\nCritChance: %s%%",
                NetFormat.fixed(getAvgDamage(), 0),
                NetFormat.fixed(getAvgHit(), 0),
                NetFormat.fixed(getAvgCrit(), 0),
                NetFormat.fixed(getCritChance() * 100f, 2),
                getName());
    }
}

class TimbalProc extends Spell {
    static final List<SpellData> SpellRankTable = List.of(
            new SpellData(1, 0, 285, 475)
    );

    public TimbalProc(Stats stats, Character character, float baseMana, boolean ptr) {
        super("TimbalProc", stats, character, SpellRankTable, 0, 0f, 1.5f, 0, 0f, 0, 0f, baseMana, ptr);
        calculate(stats, character);
    }

    @Override
    public void calculate(Stats stats, Character character) {
        setMinDamage(getBaseMinDamage()
                * (1f + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1f + character.getPriestTalents().getDarkness() * 0.02f)
                * (1f + ((character.getPriestTalents().getShadowWeaving() > 0) ? 0.1f : 0f))
                * (1f + character.getPriestTalents().getShadowform() * 0.15f));

        setMaxDamage(getBaseMaxDamage()
                * (1f + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1f + character.getPriestTalents().getDarkness() * 0.02f)
                * (1f + ((character.getPriestTalents().getShadowWeaving() > 0) ? 0.1f : 0f))
                * (1f + character.getPriestTalents().getShadowform() * 0.15f));

        setCritChance(stats.getSpellCrit());

        setRange((int) Math.rint(getBaseRange() * (1 + character.getPriestTalents().getShadowReach() * 0.1f)));
    }
}

class ExtractProc extends Spell {
    static final List<SpellData> SpellRankTable = List.of(
            new SpellData(1, 0, 788, 1312)
    );

    public ExtractProc(Stats stats, Character character, float baseMana, boolean ptr) {
        super("ExtractProc", stats, character, SpellRankTable, 0, 0f, 1.5f, 0, 0f, 0, 0f, baseMana, ptr);
        calculate(stats, character);
    }

    @Override
    public void calculate(Stats stats, Character character) {
        setMinDamage(getBaseMinDamage()
                * (1f + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1f + character.getPriestTalents().getDarkness() * 0.02f)
                * (1f + ((character.getPriestTalents().getShadowWeaving() > 0) ? 0.1f : 0.0f))
                * (1f + character.getPriestTalents().getShadowform() * 0.15f));

        setMaxDamage(getBaseMaxDamage()
                * (1f + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1f + character.getPriestTalents().getDarkness() * 0.02f)
                * (1f + ((character.getPriestTalents().getShadowWeaving() > 0) ? 0.1f : 0.0f))
                * (1f + character.getPriestTalents().getShadowform() * 0.15f));

        setCritChance(stats.getSpellCrit());

        setRange((int) Math.rint(getBaseRange() * (1 + character.getPriestTalents().getShadowReach() * 0.1f)));
    }
}

class PendulumProc extends Spell {
    static final List<SpellData> SpellRankTable = List.of(
            new SpellData(1, 0, 1168, 1752)
    );

    public PendulumProc(Stats stats, Character character, float baseMana, boolean ptr) {
        super("PendulumProc", stats, character, SpellRankTable, 0, 0f, 1.5f, 0, 0f, 0, 0f, baseMana, ptr);
        calculate(stats, character);
    }

    @Override
    public void calculate(Stats stats, Character character) {
        setMinDamage(getBaseMinDamage()
                * (1f + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1f + character.getPriestTalents().getDarkness() * 0.02f)
                * (1f + ((character.getPriestTalents().getShadowWeaving() > 0) ? 0.1f : 0.0f))
                * (1f + character.getPriestTalents().getShadowform() * 0.15f));

        setMaxDamage(getBaseMaxDamage()
                * (1f + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1f + character.getPriestTalents().getDarkness() * 0.02f)
                * (1f + ((character.getPriestTalents().getShadowWeaving() > 0) ? 0.1f : 0.0f))
                * (1f + character.getPriestTalents().getShadowform() * 0.15f));

        setCritChance(stats.getSpellCrit());

        setRange((int) Math.rint(getBaseRange() * (1f + character.getPriestTalents().getShadowReach() * 0.1f)));
    }
}

class Smite extends Spell {
    static final List<SpellData> SpellRankTable = List.of(
            new SpellData(1, 1, 13, 17),
            new SpellData(2, 6, 25, 31),
            new SpellData(3, 14, 54, 62),
            new SpellData(4, 22, 91, 104),
            new SpellData(5, 30, 150, 170),
            new SpellData(6, 38, 212, 240),
            new SpellData(7, 46, 287, 323),
            new SpellData(8, 54, 371, 415),
            new SpellData(9, 61, 405, 453),
            new SpellData(10, 69, 549, 616),
            new SpellData(11, 74, 604, 676),
            new SpellData(12, 79, 707, 793)
    );

    public Smite(Stats stats, Character character, float baseMana, boolean ptr) {
        super("Smite", stats, character, SpellRankTable, 15, 2.5f, 1.5f, 0, 2.5f / 3.5f, 30, 0f, baseMana, ptr);
        calculate(stats, character);
    }

    @Override
    public void calculate(Stats stats, Character character) {
        setMinDamage((getBaseMinDamage() + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef())
                * (1f + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1f + character.getPriestTalents().getSearingLight() * 0.05f));

        setMaxDamage((getBaseMaxDamage() + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef())
                * (1f + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1f + character.getPriestTalents().getSearingLight() * 0.05f));

        setCastTime((float) Math.max(1.0f, (getBaseCastTime() - character.getPriestTalents().getDivineFury() * 0.1f) / (1 + stats.getSpellHaste())));

        setCritCoef(getBaseCritCoef() * (1f + stats.getBonusSpellCritMultiplier()));

        setCritChance(stats.getSpellCrit() + character.getPriestTalents().getHolySpecialization() * 0.01f);

        setManaCost((int) Math.floor((getBaseManaCost() / 100f * baseMana - stats.getSpellsManaReduction())));

        setRange((int) Math.rint(getBaseRange() * (1f + character.getPriestTalents().getHolyReach() * 0.1f)));
    }
}

class HolyFire extends Spell {
    static class SpellDataDot extends SpellData {
        private final int dotDamage;

        public SpellDataDot(int rank, int level, int minDamage, int maxDamage, int dotDamage) {
            super(rank, level, minDamage, maxDamage);
            this.dotDamage = dotDamage;
        }

        public int getDotDamage() {
            return dotDamage;
        }
    }

    static final List<SpellData> SpellRankTable = List.of(
            new SpellDataDot(1, 20, 102, 128, 21),
            new SpellDataDot(2, 24, 137, 173, 28),
            new SpellDataDot(3, 30, 200, 252, 42),
            new SpellDataDot(4, 36, 267, 339, 56),
            new SpellDataDot(5, 42, 348, 440, 70),
            new SpellDataDot(6, 48, 430, 546, 91),
            new SpellDataDot(7, 54, 529, 671, 112),
            new SpellDataDot(8, 60, 639, 811, 126),
            new SpellDataDot(9, 66, 705, 895, 147),
            new SpellDataDot(10, 72, 732, 928, 287),
            new SpellDataDot(11, 78, 890, 1130, 350)
    );

    private float baseDotDamage;

    public float getBaseDotDamage() {
        return baseDotDamage;
    }

    protected void setBaseDotDamage(float value) {
        baseDotDamage = value;
    }

    private float dotDamage;

    public float getDotDamage() {
        return dotDamage;
    }

    protected void setDotDamage(float value) {
        dotDamage = value;
    }

    @Override
    public float getAvgDamage() {
        return getAvgHit() * (1f - getCritChance()) + getAvgCrit() * getCritChance() + dotDamage;
    }

    @Override
    public float getDpCT() {
        return getAvgDamage() / getCastTime();
    }

    @Override
    public float getDpS() {
        return getAvgDamage() / getCastTime();
    }

    public HolyFire(Stats stats, Character character, float baseMana, boolean ptr) {
        super("Holy Fire", stats, character, SpellRankTable, 11, 2.0f, 1.5f, 7f, 2f / 3.5f, 30, 10f, baseMana, ptr);
        calculate(stats, character);
        for (SpellData sd : SpellRankTable) {
            if (character.getLevel() > sd.getLevel()) {
                baseDotDamage = dotDamage = ((SpellDataDot) sd).getDotDamage();
            }
        }
    }

    @Override
    public void calculate(Stats stats, Character character) {
        setMinDamage((getBaseMinDamage() + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef())
                * (1f + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1f + character.getPriestTalents().getSearingLight() * 0.05f));

        setMaxDamage((getBaseMaxDamage() + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef())
                * (1f + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1f + character.getPriestTalents().getSearingLight() * 0.05f));

        dotDamage = (baseDotDamage + stats.getSpellPower() * 1f / 6f)
                * (1f + character.getPriestTalents().getSearingLight() * 0.05f);

        setCastTime((float) Math.max(1f, (getBaseCastTime() - character.getPriestTalents().getDivineFury() * 0.1f) / (1f + stats.getSpellHaste())));

        setCritCoef(getBaseCritCoef() * (1f + stats.getBonusSpellCritMultiplier()));

        setCritChance(stats.getSpellCrit() + character.getPriestTalents().getHolySpecialization() * 0.01f);

        setManaCost((int) Math.floor((getBaseManaCost() / 100f * baseMana - stats.getSpellsManaReduction())));

        setRange((int) Math.rint(getBaseRange() * (1 + character.getPriestTalents().getHolyReach() * 0.1f)));
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s *DpS: %s\r\nDpCT: %s\r\nDpM: %s\r\nHit: %s-%s, Avg %s\r\nCrit: %s-%s, Avg %s\r\nTick: %s-%s\r\nCrit Chance: %s%%\r\nCast: %s\r\nCost: %s\r\n%s",
                NetFormat.fixed(getAvgDamage(), 0),
                NetFormat.fixed(getDpS(), 2),
                NetFormat.fixed(getDpCT(), 2),
                NetFormat.fixed(getDpM(), 2),
                NetFormat.fixed(getMinDamage(), 0), NetFormat.fixed(getMaxDamage(), 0), NetFormat.fixed(getAvgHit(), 0),
                NetFormat.fixed(getMinCrit(), 0), NetFormat.fixed(getMaxCrit(), 0), NetFormat.fixed(getAvgCrit(), 0),
                NetFormat.fixed((float) Math.floor(dotDamage / 7f), 0), NetFormat.fixed((float) Math.ceil(dotDamage / 7f), 0),
                NetFormat.fixed(getCritChance() * 100f, 2),
                NetFormat.fixed(getCastTime(), 2),
                NetFormat.fixed(getManaCost(), 0),
                getName());
    }
}

class Penance extends Spell {
    static final List<SpellData> SpellRankTable = List.of(
            new SpellData(1, 60, 552, 552),
            new SpellData(2, 70, 672, 672),
            new SpellData(3, 75, 768, 768),
            new SpellData(4, 80, 864, 864)
    );

    public Penance(Stats stats, Character character, float baseMana, boolean ptr) {
        super("Penance", stats, character, SpellRankTable, 16, 2f, 1.5f, 0, 2.4f / 3.5f, 30, 10f, baseMana, ptr);
        calculate(stats, character);
    }

    @Override
    public void calculate(Stats stats, Character character) {
        if (character.getPriestTalents().getPenance() == 0) {
            setMinDamage(0);
            setMaxDamage(0);
            return;
        }

        setMaxDamage((getBaseMinDamage() + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef())
                * (1f + character.getPriestTalents().getTwinDisciplines() * 0.01f)
                * (1f + character.getPriestTalents().getFocusedPower() * 0.02f)
                * (1f + character.getPriestTalents().getSearingLight() * 0.05f));
        setMinDamage(getMaxDamage());

        setCastTime((float) Math.max(1.0f, getBaseCastTime() / (1 + stats.getSpellHaste())));

        setCritCoef(getBaseCritCoef() * (1f + stats.getBonusSpellCritMultiplier()));

        setCritChance(stats.getSpellCrit() + character.getPriestTalents().getHolySpecialization() * 0.01f);

        setCooldown((getBaseCooldown() - (character.getPriestTalents().isGlyphofPenance() ? 2 : 0)) * (1f - character.getPriestTalents().getAspiration() * 0.1f));

        setManaCost((int) Math.floor((getBaseManaCost() / 100f * baseMana - stats.getSpellsManaReduction())
                * (1f - character.getPriestTalents().getImprovedHealing() * 0.05f)));

        setRange((int) Math.rint(getBaseRange() * (1 + character.getPriestTalents().getHolyReach() * 0.1f)));
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s *DpS: %s\r\nDpCT: %s\r\nDpM: %s\r\nTick Hit: %s-%s, Total %s\r\nTick Crit: %s-%s, Total %s\r\nCrit Chance: %s%%\r\nCast: %s\r\nCost: %s\r\n%s",
                NetFormat.fixed(getAvgDamage(), 0),
                NetFormat.fixed(getDpS(), 2),
                NetFormat.fixed(getDpCT(), 2),
                NetFormat.fixed(getDpM(), 2),
                NetFormat.fixed((float) Math.floor(getMinDamage() / 3), 0), NetFormat.fixed((float) Math.ceil(getMinDamage() / 3), 0), NetFormat.fixed(getAvgHit(), 0),
                NetFormat.fixed((float) Math.floor(getMinCrit() / 3), 0), NetFormat.fixed((float) Math.ceil(getMinCrit() / 3), 0), NetFormat.fixed(getAvgCrit(), 0),
                NetFormat.fixed(getCritChance() * 100f, 2),
                NetFormat.fixed(getCastTime(), 2),
                NetFormat.fixed(getManaCost(), 0),
                getName());
    }
}

class Shadowfiend extends Spell {
    static final List<SpellData> SpellRankTable = List.of(
            new SpellData(1, 66, 181, 220)
    );

    private float swings = 10f;

    @Override
    public float getDpCT() {
        return getAvgDamage() * swings / getGlobalCooldown();
    }

    @Override
    public float getDpS() {
        return getAvgDamage() * swings / getDebuffDuration();
    }

    public Shadowfiend(Stats stats, Character character, float baseMana, boolean ptr) {
        super("Shadowfiend", stats, character, SpellRankTable, 0, 0f, 2.0f, 10f, 1.5f / 3.5f * 0.9f, 30, (5f - character.getPriestTalents().getVeiledShadows() * 1f) * 60f, baseMana, ptr);
        calculate(stats, character);
    }

    @Override
    public void calculate(Stats stats, Character character) {
        float shadowCrawl = 0.15f * 13f / 15f;

        setMinDamage((getBaseMinDamage() + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef()) * (1 + shadowCrawl));

        setMaxDamage((getBaseMaxDamage() + (stats.getSpellPower() + stats.getSpellShadowDamageRating()) * getDamageCoef()) * (1 + shadowCrawl));

        setCritChance(0.05f);

        setManaCost(0);

        setRange((int) Math.rint(getBaseRange() * (1 + character.getPriestTalents().getShadowReach() * 0.1f)));
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%s *DpS: %s\r\nDpCT: %s\r\nSwing: %s\r\nSwing Crit: %s\r\nCrit Chance: %s%%\r\n%s",
                NetFormat.fixed(getAvgDamage() * swings, 0),
                NetFormat.fixed(getDpS(), 2),
                NetFormat.fixed(getDpCT(), 2),
                NetFormat.fixed(getAvgHit(), 2),
                NetFormat.fixed(getAvgCrit(), 2),
                NetFormat.fixed(getCritChance() * 100f, 2),
                getName());
    }
}
