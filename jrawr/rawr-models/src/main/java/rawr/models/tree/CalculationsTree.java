// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Tree/CalculationsTree.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Omitted members (UI/chart only, no Java consumers): SubPointNameColors color tables beyond the
// ratings set, CustomChartNames/CustomRenderedChartNames, GetCustomChartData, RenderCustomChart,
// OptimizableCalculationLabels, CalculationOptionsPanel. CharacterDisplayCalculationLabels and
// SubPointNameColors/getOverallRatingLabel are kept as the additive web-display API.
package rawr.models.tree;

import rawr.base.stats.AdditiveStat;
import rawr.base.stats.BaseStats;
import rawr.base.character.Buff;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.CharacterClass;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.talents.DruidTalents;
import rawr.base.items.GemmingTemplate;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.items.Item;
import rawr.base.items.ItemType;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;
import rawr.base.stats.WeightedStat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CalculationsTree extends CalculationsBase {

    public static class DiminishingReturns {
        private double C;
        private double D;
        private float multiplier;
        private float factor;

        public DiminishingReturns(float multiplier, int factor) {
            C = Math.pow(multiplier / factor, factor / (factor - 1));
            D = Math.pow(C, 1 / factor);
            this.multiplier = multiplier;
            this.factor = factor;
        }

        public float cap(float value, float cap) {
            if (cap >= value) return value;
            return (float) (cap + multiplier * cap * (Math.pow(value / cap - 1 + C, 1 / factor) - D));
        }

        // DiminishingReturns(multiplier,factor).Cap(value,cap) gives the same result as this function
        public static float cap(float value, float cap, float multiplier, int factor) {
            if (cap >= value) return value;
            double C = Math.pow(multiplier / factor, factor / (factor - 1));
            return (float) (cap + multiplier * cap * (Math.pow(value / cap - 1 + C, 1 / factor) - Math.pow(C, 1 / factor)));
        }

        // max > cap, adviced is cap < max <= 2*cap
        public static float capWithMaximum(float value, float cap, float max) {
            if (cap >= value) return value;
            return (value * max - cap * cap) / (value + max - 2 * cap);
        }

        // with max = 2 * cap
        // This is exactly the same as the one above, actually, but with 2*cap the formula is much simpler
        public static float capWithMaximum2(float value, float cap) {
            if (cap >= value) return value;
            return cap * (2 - cap / value);
        }
    }

    private List<GemmingTemplate> defaultGemmingTemplates = null;

    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        if (this.defaultGemmingTemplates == null) {
            // Meta
            int ember = 41333;
            int revitalizing = 41376;
            int insightful = 41401;

            // [0] uncommon
            // [1] perfect uncommon
            // [2] rare
            // [3] epic
            // [4] jewelcrafting

            // Reds
            int[] runed = { 39911, 41438, 39998, 40113, 42144 }; // ** spell power
            // Blue
            int[] lustrous = { 39927, 41440, 40010, 40121, 42146 }; // mp5 [pure]
            int[] sparkling = { 39920, 41442, 40009, 40120, 42145 }; // spi [pure]
            // Yellow
            int[] brilliant = { 39912, 41444, 40012, 40123, 42148 }; // int [pure]
            // Purple
            int[] purified = { 39941, 41457, 40026, 40133 }; // ** spell power + spirit
            int[] royal = { 39943, 41459, 40027, 40134 }; // spell power + mp5
            // Green
            int[] dazzling = { 39984, 41463, 40094, 40175 }; // int + mp5
            int[] seers = { 39979, 41473, 40092, 40170 }; // int + spi
            // Orange
            int[] luminous = { 39946, 41494, 40047, 40151 }; // int + spell power
            int[] reckless = { 39959, 41497, 40051, 40155 }; // ** spell power + haste

            this.defaultGemmingTemplates = new ArrayList<GemmingTemplate>();
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Uncommon (Revitalizing)", false, runed[0], purified[0], reckless[0], revitalizing);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Uncommon (Ember)", false, runed[0], purified[0], reckless[0], ember);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Uncommon (Insightful)", false, runed[0], purified[0], reckless[0], insightful);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Perfect (Revitalizing)", false, runed[1], purified[1], reckless[1], revitalizing);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Perfect (Ember)", false, runed[1], purified[1], reckless[1], ember);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Perfect (Insightful)", false, runed[1], purified[1], reckless[1], insightful);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Rare (Revitalizing)", false, runed[2], purified[2], reckless[2], revitalizing);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Rare (Ember)", false, runed[2], purified[2], reckless[2], ember);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Rare (Insightful)", false, runed[2], purified[2], reckless[2], insightful);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Epic (Revitalizing)", true, runed[3], purified[3], reckless[3], revitalizing);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Epic (Ember)", true, runed[3], purified[3], reckless[3], ember);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Epic (Insightful)", true, runed[3], purified[3], reckless[3], insightful);
            addJCGemmingTemplateGroup(this.defaultGemmingTemplates, "Jewelcrafting (Revitalizing)", false, runed[4], revitalizing);
            addJCGemmingTemplateGroup(this.defaultGemmingTemplates, "Jewelcrafting (Ember)", false, runed[4], ember);
            addJCGemmingTemplateGroup(this.defaultGemmingTemplates, "Jewelcrafting (Insightful)", false, runed[4], insightful);
        }
        return this.defaultGemmingTemplates;
    }

    private void addGemmingTemplateGroup(List<GemmingTemplate> list, String name, boolean enabled, int runed, int purified, int reckless, int meta) {
        // Overrides, only "runed" and "seers"
        GemmingTemplate t1 = new GemmingTemplate();
        t1.setModel("Tree");
        t1.setGroup(name);
        t1.setRedId(runed);
        t1.setYellowId(runed);
        t1.setBlueId(runed);
        t1.setPrismaticId(runed);
        t1.setMetaId(meta);
        t1.setEnabled(enabled);
        list.add(t1);
        GemmingTemplate t2 = new GemmingTemplate();
        t2.setModel("Tree");
        t2.setGroup(name);
        t2.setRedId(runed);
        t2.setYellowId(reckless);
        t2.setBlueId(purified);
        t2.setPrismaticId(runed);
        t2.setMetaId(meta);
        t2.setEnabled(enabled);
        list.add(t2);
    }

    private void addJCGemmingTemplateGroup(List<GemmingTemplate> list, String name, boolean enabled, int runed, int meta) {
        GemmingTemplate t = new GemmingTemplate();
        t.setModel("Tree");
        t.setGroup(name);
        t.setRedId(runed);
        t.setYellowId(runed);
        t.setBlueId(runed);
        t.setPrismaticId(runed);
        t.setMetaId(meta);
        t.setEnabled(enabled);
        list.add(t);
    }

    private static List<String> relevantGlyphs;

    public List<String> getRelevantGlyphs() {
        if (relevantGlyphs == null) {
            relevantGlyphs = new ArrayList<String>();
            relevantGlyphs.add("Glyph of Healing Touch");
            relevantGlyphs.add("Glyph of Innervate");
            relevantGlyphs.add("Glyph of Lifebloom");
            relevantGlyphs.add("Glyph of Nourish");
            relevantGlyphs.add("Glyph of Rebirth");
            relevantGlyphs.add("Glyph of Regrowth");
            relevantGlyphs.add("Glyph of Rejuvenation");
            relevantGlyphs.add("Glyph of Swiftmend");
            relevantGlyphs.add("Glyph of Wild Growth");
            relevantGlyphs.add("Glyph of Rapid Rejuvenation");
        }
        return relevantGlyphs;
    }

    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> colors = new LinkedHashMap<String, String>();
        colors.put("SingleTarget", "#FF0000");
        colors.put("Sustained", "#0000FF");
        colors.put("Survival", "#008000");
        return colors;
    }

    @Override
    public String getOverallRatingLabel() {
        return "Overall Points";
    }

    private static final String[] CharacterDisplayCalculationLabels = new String[] {
            "Points:Single Target Points",
            "Points:Sustained Points",
            "Points:Survival Points*Survival Points represents the total raw physical damage \n"
                    + "you can take before dying. Calculated based on Health\n"
                    + "and Armor damage reduction. (Survival multiplier is \n"
                    + "applied and result is scaled down by 100)",
            "Points:Overall Points",

            "Base Stats:Base Health",
            "Base Stats:Base Armor",
            "Base Stats:Base Mana",
            "Base Stats:Base Stamina",
            "Base Stats:Base Intellect",
            "Base Stats:Base Spirit",
            "Base Stats:Base Spell Power",
            "Base Stats:Base Spell Crit",
            "Base Stats:Base Spell Haste",
            "Base Stats:Base Global CD",

            "Combat Stats:Health",
            "Combat Stats:Armor",
            "Combat Stats:Mana",
            "Combat Stats:Stamina",
            "Combat Stats:Intellect",
            "Combat Stats:Spirit",
            "Combat Stats:Spell Power",
            "Combat Stats:Spell Crit",
            "Combat Stats:Spell Haste",
            "Combat Stats:Global CD",

            "Model:Total Time",
            "Model:Time until OOM (unreduced)",
            "Model:Time until OOM",
            "Model:Total healing done",
            "Model:Sustained HPS",
            "Model:Single Target HPS",
            "Model:Mana regen per second",
            "Model:Mana from innervates",
            "Model:Average casts per minute",
            "Model:Average crits per minute",
            "Model:Average heals per minute",
            "Model:Rejuvenation casts per minute",
            "Model:Rejuvenation average up",
            "Model:Regrowth casts per minute",
            "Model:Regrowth average up",
            "Model:Lifebloom (stack) casts per minute",
            "Model:Lifebloom (stack) average up",
            "Model:Lifebloom (stack) method",
            "Model:Lifebloom casts per minute",
            "Model:Lifebloom average up",
            "Model:Nourish casts per minute",
            //"Model:Healing Touch casts per minute",
            "Model:Swiftmend casts per minute",
            "Model:Wild Growth casts per minute",
            "Model:Revitalize procs per minute",

            "Rejuvenation:RJ Heal per tick",
            "Rejuvenation:RJ Tick time",
            "Rejuvenation:RJ HPS",
            "Rejuvenation:RJ HPM",
            "Rejuvenation:RJ HPCT",

            "Regrowth:RG Heal",
            "Regrowth:RG Tick",
            "Regrowth:RG HPS",
            "Regrowth:RG HPS (HoT only)",
            "Regrowth:RG HPM",
            "Regrowth:RG HPCT",
            "Regrowth:RG Heal (spam)",
            "Regrowth:RG HPS (spam)",
            "Regrowth:RG HPM (spam)",
            "Regrowth:RG HPCT (spam)",

            "Lifebloom:LB Tick",
            "Lifebloom:LB Bloom Heal",
            "Lifebloom:LB HPS",
            "Lifebloom:LB HPS (HoT only)",
            "Lifebloom:LB HPM",
            "Lifebloom:LB HPCT",

            "Lifebloom Stacked Blooms:LBx2 (fast stack) HPS",
            "Lifebloom Stacked Blooms:LBx2 (fast stack) HPM",
            "Lifebloom Stacked Blooms:LBx2 (fast stack) HPCT",

            "Lifebloom Stacked Blooms:LBx3 (fast stack) HPS",
            "Lifebloom Stacked Blooms:LBx3 (fast stack) HPM",
            "Lifebloom Stacked Blooms:LBx3 (fast stack) HPCT",

            "Lifebloom Stacked Blooms:LBx2 (slow stack) HPS",
            "Lifebloom Stacked Blooms:LBx2 (slow stack) HPM",
            "Lifebloom Stacked Blooms:LBx2 (slow stack) HPCT",

            "Lifebloom Stacked Blooms:LBx3 (slow stack) HPS",
            "Lifebloom Stacked Blooms:LBx3 (slow stack) HPM",
            "Lifebloom Stacked Blooms:LBx3 (slow stack) HPCT",

            "Lifebloom Stacked Blooms:LBx3 (rolling) Tick",
            "Lifebloom Stacked Blooms:LBx3 (rolling) HPS",
            "Lifebloom Stacked Blooms:LBx3 (rolling) HPM",
            "Lifebloom Stacked Blooms:LBx3 (rolling) HPCT",

            "Healing Touch:HT Heal",
            "Healing Touch:HT HPS",
            "Healing Touch:HT HPM",
            "Healing Touch:HT HPCT",

            "Wild Growth:WG first Tick",
            "Wild Growth:WG HPS(single target)",
            "Wild Growth:WG HPM(single target)",
            "Wild Growth:WG HPS(max)",
            "Wild Growth:WG HPM(max)",

            "Nourish:N Heal",
            "Nourish:N HPM",
            "Nourish:N HPS",
            "Nourish:N HPCT",
            "Nourish:N (1 HoT) Heal",
            "Nourish:N (1 HoT) HPM",
            "Nourish:N (1 HoT) HPS",
            "Nourish:N (1 HoT) HPCT",
            "Nourish:N (2 HoTs) Heal",
            "Nourish:N (2 HoTs) HPM",
            "Nourish:N (2 HoTs) HPS",
            "Nourish:N (2 HoTs) HPCT",
            "Nourish:N (3 HoTs) Heal",
            "Nourish:N (3 HoTs) HPM",
            "Nourish:N (3 HoTs) HPS",
            "Nourish:N (3 HoTs) HPCT",
            "Nourish:N (4 HoTs) Heal",
            "Nourish:N (4 HoTs) HPM",
            "Nourish:N (4 HoTs) HPS",
            "Nourish:N (4 HoTs) HPCT",

            "Swiftmend:SM Rejuv Heal",
            "Swiftmend:SM Rejuv HPM",
            "Swiftmend:SM Rejuv Lost Ticks",
            "Swiftmend:SM Regrowth Heal",
            "Swiftmend:SM Regrowth HPM",
            "Swiftmend:SM Regrowth Lost Ticks",
            "Swiftmend:SM Both Heal",
            "Swiftmend:SM Both HPM",
            "Swiftmend:SM Both Rejuv Lost Ticks",
            "Swiftmend:SM Both Regrowth Lost Ticks",
    };

    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        return List.of(CharacterDisplayCalculationLabels);
    }

    private List<ItemType> relevantItemTypes = null;

    @Override
    public List<ItemType> getRelevantItemTypes() {
        if (this.relevantItemTypes == null) {
            this.relevantItemTypes = List.of(
                    ItemType.None,
                    ItemType.Cloth,
                    ItemType.Leather,
                    ItemType.Dagger,
                    ItemType.FistWeapon,
                    ItemType.Idol,
                    ItemType.OneHandMace,
                    ItemType.TwoHandMace,
                    ItemType.Staff
            );
        }
        return this.relevantItemTypes;
    }

    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.Druid;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationTree();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsTree();
    }

    protected RotationSettings getRotationFromCalculationOptions(Stats stats, CalculationOptionsTree calcOpts, CharacterCalculationsTree calculatedStats) {
        RotationSettings settings = new RotationSettings();
        CalculationOptionsTree.SpellProfile profile = calcOpts.getCurrent();

        switch (profile.getLifebloomStackType()) {
            case 0:
                settings.lifeBloomType = LifeBloomType.Slow;
                break;
            case 1:
                settings.lifeBloomType = LifeBloomType.Fast;
                break;
            case 2:
            default:
                settings.lifeBloomType = LifeBloomType.Rolling;
                break;
            case 3:
                settings.lifeBloomType = LifeBloomType.Slow2;
                break;
            case 4:
                settings.lifeBloomType = LifeBloomType.Fast2;
                break;
        }

        settings.averageLifebloomStacks = (float) profile.getLifebloomStackAmount();
        settings.averageRejuv = (float) profile.getRejuvAmount();
        settings.averageRegrowth = (float) profile.getRegrowthAmount();

        settings.SwiftmendPerMin = profile.getSwiftmendPerMinute();
        settings.WildGrowthPerMin = profile.getWildGrowthPerMinute();

        settings.RejuvFraction = (float) profile.getRejuvFrac() / 100.0f;
        settings.LifebloomFraction = (float) profile.getLifebloomFrac() / 100.0f;
        settings.RegrowthFraction = (float) profile.getRegrowthFrac() / 100.0f;
        settings.NourishFraction = (float) profile.getNourishFrac() / 100.0f;

        settings.nourish1 = (float) profile.getNourish1() / 100f;
        settings.nourish2 = (float) profile.getNourish2() / 100f;
        settings.nourish3 = (float) profile.getNourish3() / 100f;
        settings.nourish4 = (float) profile.getNourish4() / 100f;
        settings.nourish0 = 1.0f - (settings.nourish1 + settings.nourish2 + settings.nourish3 + settings.nourish4);

        settings.healTarget = HealTargetTypes.RaidHealing;

        settings.livingSeedEfficiency = (float) profile.getLivingSeedEfficiency() / 100f;

        return settings;
    }

    protected void calculateTriggers(CombatFactors cf, Map<Trigger, Float> triggerIntervals, Map<Trigger, Float> triggerChances) {
        float CastInterval = 1f / cf.CastsPerSecond;
        float HealInterval = 1f / cf.HealsPerSecond;
        float CritsRatio = cf.CritsPerSecond / cf.CastsPerSecond;
        float RejuvInterval = 1f / cf.RejuvTicksPerSecond;

        triggerIntervals.put(Trigger.Use, 0f);
        triggerChances.put(Trigger.Use, 1f);

        triggerIntervals.put(Trigger.SpellCast, CastInterval);
        triggerChances.put(Trigger.SpellCast, 1f);

        triggerIntervals.put(Trigger.HealingSpellCast, CastInterval);
        triggerChances.put(Trigger.HealingSpellCast, 1f);

        triggerIntervals.put(Trigger.HealingSpellHit, HealInterval);
        triggerChances.put(Trigger.HealingSpellHit, 1f);

        triggerIntervals.put(Trigger.SpellCrit, CastInterval);
        triggerChances.put(Trigger.SpellCrit, CritsRatio);

        triggerIntervals.put(Trigger.HealingSpellCrit, CastInterval);
        triggerChances.put(Trigger.HealingSpellCrit, CritsRatio);

        triggerIntervals.put(Trigger.RejuvenationTick, RejuvInterval);
        triggerChances.put(Trigger.RejuvenationTick, 1f);
    }

    public enum ProcCalc {
        None,
        Average,
        Max
    }

    private void doSpecialEffects(Character character, Stats stats, CombatFactors cf, float totalTime, List<Float> hasteRatings, List<Float> hasteRatingUptimes, ProcCalc pc) {
        // Initialize triggers
        Map<Trigger, Float> triggerIntervals = new LinkedHashMap<Trigger, Float>();
        Map<Trigger, Float> triggerChances = new LinkedHashMap<Trigger, Float>();
        calculateTriggers(cf, triggerIntervals, triggerChances);

        // Haste lists (separately handled)
        List<SpecialEffect> tempHasteEffects = new ArrayList<SpecialEffect>();
        List<Float> tempHasteEffectIntervals = new ArrayList<Float>();
        List<Float> tempHasteEffectChances = new ArrayList<Float>();
        List<Float> tempHasteEffectOffsets = new ArrayList<Float>();
        List<Float> tempHasteEffectScales = new ArrayList<Float>();

        float offset = 0;

        List<SpecialEffect> effects = new ArrayList<SpecialEffect>();
        for (SpecialEffect effect : stats.specialEffects()) {
            effect.getStats().generateSparseData();

            // Filter out unhandled effects
            if (!triggerIntervals.containsKey(effect.getTrigger())) continue;

            // Filter out haste effects
            if (effect.getStats().getHasteRating() > 0) {
                tempHasteEffects.add(effect);
                // PATCH for use effects
                if (effect.getTrigger() == Trigger.Use) {
                    tempHasteEffectIntervals.add(effect.getCooldown());
                    tempHasteEffectChances.add(1f);
                    tempHasteEffectOffsets.add(offset);
                    offset += effect.getDuration();
                } else {
                    tempHasteEffectIntervals.add(triggerIntervals.get(effect.getTrigger()));
                    tempHasteEffectChances.add(triggerChances.get(effect.getTrigger()));
                    tempHasteEffectOffsets.add(0f);
                }
                tempHasteEffectScales.add(1f);
                continue;
            }

            effects.add(effect);
        }

        // Calculate haste breakdown
        if (tempHasteEffects.size() == 0 || pc == ProcCalc.None) {
            hasteRatings.add(0.0f);
            hasteRatingUptimes.add(1.0f);
        } else if (pc == ProcCalc.Max) {
            float hasteRating = 0;
            for (int i = 0; i < tempHasteEffects.size(); i++) hasteRating += tempHasteEffects.get(i).getStats().getHasteRating();
            hasteRatings.add(hasteRating);
            hasteRatingUptimes.add(1f);
        } else if (tempHasteEffects.size() == 1) { // Only one, add it to
            SpecialEffect effect = tempHasteEffects.get(0);
            float uptime = effect.getAverageStackSize(tempHasteEffectIntervals.get(0), tempHasteEffectChances.get(0), 0, totalTime);
            hasteRatings.add(effect.getStats().getHasteRating());
            hasteRatingUptimes.add(uptime);
            hasteRatings.add(0.0f);
            hasteRatingUptimes.add(1.0f - uptime);
        } else if (tempHasteEffects.size() > 1) {
            WeightedStat[] HasteRatingWeights = SpecialEffect.getAverageCombinedUptimeCombinations(tempHasteEffects.toArray(new SpecialEffect[0]), toFloatArray(tempHasteEffectIntervals), toFloatArray(tempHasteEffectChances), toFloatArray(tempHasteEffectOffsets), toFloatArray(tempHasteEffectScales), 0, totalTime, AdditiveStat.HasteRating);
            for (int i = 0; i < HasteRatingWeights.length; i++) {
                hasteRatings.add(HasteRatingWeights[i].value());
                hasteRatingUptimes.add(HasteRatingWeights[i].chance());
            }
        }

        accumulateSpecialEffects(character, stats, totalTime, triggerIntervals, triggerChances, effects, 1f, pc);

        // Clear special effects from Stats
        stats.clearSpecialEffects();
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }

    protected void accumulateSpecialEffects(Character character, Stats stats, float fightDuration, Map<Trigger, Float> triggerIntervals, Map<Trigger, Float> triggerChances, List<SpecialEffect> effects, float weight, ProcCalc pc) {
        for (SpecialEffect effect : effects) {
            Stats effectStats = effect.getStats();

            float upTime = 0f;

            // Filter out haste effects
            if (effect.getStats().getHasteRating() > 0f) continue;

            if (effect.getTrigger() == Trigger.Use) {
                if (effect.getStats().getRawSpecialEffectDataSize() >= 1) {
                    upTime = effect.getAverageUptime(0f, 1f, 0, fightDuration);
                    List<SpecialEffect> nestedEffect = new ArrayList<SpecialEffect>();
                    for (SpecialEffect se : effect.getStats().specialEffects()) nestedEffect.add(se);
                    Stats _stats2 = effectStats.copy();
                    accumulateSpecialEffects(character, _stats2, effect.getDuration(), triggerIntervals, triggerChances, nestedEffect, upTime, pc);
                    effectStats = _stats2;
                } else {
                    upTime = effect.getAverageStackSize(0f, 1f, 0, fightDuration);
                }
            } else if (effect.getDuration() == 0f) {
                upTime = effect.getAverageProcsPerSecond(triggerIntervals.get(effect.getTrigger()), triggerChances.get(effect.getTrigger()),
                        0, fightDuration);
            } else if (triggerIntervals.containsKey(effect.getTrigger())) {
                upTime = effect.getAverageStackSize(triggerIntervals.get(effect.getTrigger()), triggerChances.get(effect.getTrigger()),
                        0, fightDuration);
            }

            if (upTime > 0f) {
                if (pc == ProcCalc.None && upTime < 0.5f) {
                    Stats s = new Stats();
                    s.setSpellPower(effectStats.getSpellPower());
                    s.setCritRating(effectStats.getCritRating());
                    //s.HasteRating = effectStats.HasteRating;
                    s.setSpellHaste(effectStats.getSpellHaste());
                    effectStats.setSpellPower(0);
                    effectStats.setCritRating(0);
                    //effectStats.HasteRating = 0;
                    effectStats.setSpellHaste(0);
                    stats.accumulate(effectStats, upTime * weight);
                    effectStats.setSpellPower(s.getSpellPower());
                    effectStats.setCritRating(s.getCritRating());
                    //effectStats.HasteRating = s.HasteRating;
                    effectStats.setSpellHaste(s.getSpellHaste());
                } else if (pc == ProcCalc.Max && upTime < 0.5f) {
                    Stats s = new Stats();
                    s.setSpellPower(effectStats.getSpellPower());
                    s.setCritRating(effectStats.getCritRating());
                    //s.HasteRating = effectStats.HasteRating;
                    s.setSpellHaste(effectStats.getSpellHaste());
                    effectStats.setSpellPower(0);
                    effectStats.setCritRating(0);
                    //effectStats.HasteRating = 0;
                    effectStats.setSpellHaste(0);
                    stats.accumulate(effectStats, upTime * weight);
                    effectStats.setSpellPower(s.getSpellPower());
                    effectStats.setCritRating(s.getCritRating());
                    //effectStats.HasteRating = s.HasteRating;
                    effectStats.setSpellHaste(s.getSpellHaste());
                    stats.accumulate(s, weight);
                } else {
                    stats.accumulate(effectStats, upTime * weight);
                }
            }
        }
    }

    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        if (referenceCalculation && significantChange) {
            SpecialEffect.clearInterpolatorCaches();
        }
        CalculationOptionsTree calcOpts = (CalculationOptionsTree) character.getCalculationOptions();
        CalculationOptionsTree.SpellProfile profile = calcOpts.getCurrent();

        CharacterCalculationsTree calculationResult = new CharacterCalculationsTree();
        calculationResult.setLocalCharacter(character);
        calculationResult.setBasicStats(getCharacterStats(character, additionalItem));

        // Rotations
        Stats stats = calculationResult.getBasicStats();
        stats.setManaRestore(stats.getManaRestore() / profile.getFightDuration());
        float replenish = stats.getManaRestoreFromMaxManaPerSecond() >= 0.002f ? 0.002f : 0;
        stats.setManaRestore(stats.getManaRestore() + (stats.getManaRestoreFromMaxManaPerSecond() - replenish) * stats.getMana());
        stats.setManaRestoreFromMaxManaPerSecond(replenish);

        float ExtraHealing = 0f;
        RotationSettings settings = getRotationFromCalculationOptions(stats, calcOpts, calculationResult);

        // Initial run
        SustainedResult rot = Solver.simulateHealing(calculationResult, stats, calcOpts, settings);

        List<Float> hasteRatings = new ArrayList<Float>();
        List<Float> hasteRatingUptimes = new ArrayList<Float>();

        ProcCalc pc = ProcCalc.None;
        switch (calcOpts.getProcType()) {
            case 0:
                pc = ProcCalc.None;
                break;
            case 1:
                pc = ProcCalc.Average;
                break;
            case 2:
                pc = ProcCalc.Max;
                break;
        }

        CombatFactors cfs = rot.getCombatFactors();
        cfs.compute();

        for (int k = 0; k < 2; k++) {
            // Create new stats instance with procs
            stats = getCharacterStats(character, additionalItem);
            stats.setManaRestore(stats.getManaRestore() / profile.getFightDuration());

            hasteRatings.clear();
            hasteRatingUptimes.clear();
            doSpecialEffects(character, stats, cfs, rot.TotalTime, hasteRatings, hasteRatingUptimes, pc);

            // Add replenish
            replenish = stats.getManaRestoreFromMaxManaPerSecond() >= 0.002f ? 0.002f : 0;
            stats.setManaRestore(stats.getManaRestore() + (stats.getManaRestoreFromMaxManaPerSecond() - replenish) * stats.getMana());
            stats.setManaRestoreFromMaxManaPerSecond(replenish);

            ExtraHealing = stats.getHealed();

            // calcOpts.IgnoreAllHasteEffects is false in the shipped code
            cfs = new CombatFactors();
            float AverageHasteRating = 0;
            for (int i = 0; i < hasteRatings.size(); i++) {
                stats.setHasteRating(stats.getHasteRating() + hasteRatings.get(i));
                SustainedResult r = Solver.simulateHealing(calculationResult, stats, calcOpts, settings);
                cfs.accumulate(r.getCombatFactors(), hasteRatingUptimes.get(i));
                stats.setHasteRating(stats.getHasteRating() - hasteRatings.get(i));
                AverageHasteRating += hasteRatings.get(i) * hasteRatingUptimes.get(i);
                if (i == 0) rot = r;
            }
            cfs.compute();
            stats.setHasteRating(stats.getHasteRating() + AverageHasteRating); // For Burst
        }
        calculationResult.Sustained = rot;
        calculationResult.setCombatStats(stats);

        calculationResult.SingleTarget = Solver.calculateSingleTargetBurst(calculationResult, stats, calcOpts, Solver.singleTargetIndexToRotation(calcOpts.getSingleTargetRotation()));

        calculationResult.SingleTargetHPS = calculationResult.SingleTarget[0].getHPS();
        calculationResult.SustainedHPS = cfs.TotalHealing / rot.TotalTime + ExtraHealing;

        // Survival Points
        float DamageReduction = StatConversion.getArmorDamageReduction(83, stats.getArmor(), 0, 0, 0);
        calculationResult.setSurvivalPoints(stats.getHealth() / (1f - DamageReduction) / 100f * calcOpts.getSurvValuePer100());

        // Apply diminishing returns
        calculationResult.setSingleTargetPoints(DiminishingReturns.capWithMaximum2(calculationResult.SingleTargetHPS, calcOpts.getSingleTarget()));
        calculationResult.setSustainedPoints(DiminishingReturns.capWithMaximum2(calculationResult.SustainedHPS, calcOpts.getSustainedTarget()));

        calculationResult.setOverallPoints(calculationResult.getSingleTargetPoints() + calculationResult.getSustainedPoints() + calculationResult.getSurvivalPoints());

        return calculationResult;
    }

    private static final SpecialEffect[] seNaturesGrace = new SpecialEffect[] {
            null,
            new SpecialEffect(Trigger.HealingSpellCrit, statsWithSpellHaste(0.2f), 3f, 0, 1 * 1f / 3f, 1),
            new SpecialEffect(Trigger.HealingSpellCrit, statsWithSpellHaste(0.2f), 3f, 0, 2 * 1f / 3f, 1),
            new SpecialEffect(Trigger.HealingSpellCrit, statsWithSpellHaste(0.2f), 3f, 0, 3 * 1f / 3f, 1),
    };

    private static Stats statsWithSpellHaste(float spellHaste) {
        Stats s = new Stats();
        s.setSpellHaste(spellHaste);
        return s;
    }

    public Stats getCharacterStats(Character character, Item additionalItem) {
        return getCharacterStats(character, additionalItem, new Stats());
    }

    public Stats getCharacterStats(Character character, Item additionalItem, Stats statsProcs) {
        CalculationOptionsTree calcOpts = (CalculationOptionsTree) character.getCalculationOptions();
        DruidTalents talents = character.getDruidTalents();
        CalculationOptionsTree.SpellProfile profile = calcOpts.getCurrent();

        Stats statsRace = BaseStats.getBaseStats(character); //GetRacialBaseStats(character.Race);
        TreeConstants.BaseMana = statsRace.getMana(); // Setup TreeConstant

        Stats statsTalents = new Stats();
        statsTalents.setBonusAgilityMultiplier((1f + 0.01f * talents.getSurvivalOfTheFittest() * 2f) * (1f + 0.01f * talents.getImprovedMarkOfTheWild()) - 1f);
        statsTalents.setBonusIntellectMultiplier((1f + 0.01f * talents.getSurvivalOfTheFittest() * 2f) * (1f + 0.01f * talents.getImprovedMarkOfTheWild()) * (1f + 0.04f * talents.getHeartOfTheWild()) - 1f);
        statsTalents.setBonusSpiritMultiplier((1f + 0.01f * talents.getSurvivalOfTheFittest() * 2f) * (1f + 0.01f * talents.getImprovedMarkOfTheWild()) * (1f + 0.05f * talents.getLivingSpirit()) - 1f);
        statsTalents.setBonusStaminaMultiplier((1f + 0.01f * talents.getSurvivalOfTheFittest() * 2f) * (1f + 0.01f * talents.getImprovedMarkOfTheWild()) - 1f);
        statsTalents.setBonusStrengthMultiplier((1f + 0.01f * talents.getSurvivalOfTheFittest() * 2f) * (1f + 0.01f * talents.getImprovedMarkOfTheWild()) - 1f);
        statsTalents.setBonusArmorMultiplier(0.80f * talents.getImprovedTreeOfLife());
        statsTalents.setSpellHaste(((1f + 0.01f * talents.getCelestialFocus()) * (1f + 0.02f * talents.getGiftOfTheEarthmother())) - 1f);
        statsTalents.setSpellDamageFromSpiritPercentage(talents.getImprovedTreeOfLife() * 0.05f);
        statsTalents.setSpellCombatManaRegeneration(talents.getIntensity() * 0.5f / 3f);
        statsTalents.setRevitalizeChance(talents.getRevitalize() * 0.05f);

        if (!calcOpts.getIgnoreNaturesGrace() && talents.getNaturesGrace() > 0) {
            statsTalents.addSpecialEffect(seNaturesGrace[talents.getNaturesGrace()]);
        }

        Stats statsBaseGear = new Stats();
        accumulateItemStats(statsBaseGear, character, additionalItem);
        Stats statsBuffs = getBuffsStats(character, calcOpts);
        Stats statsTotal = Stats.add(Stats.add(Stats.add(Stats.add(statsBaseGear, statsBuffs), statsRace), statsTalents), statsProcs);

        statsTotal.setAgility((float) Math.floor(statsTotal.getAgility() * (1f + statsTotal.getBonusAgilityMultiplier())));
        statsTotal.setStamina((float) Math.floor(statsTotal.getStamina() * (1f + statsTotal.getBonusStaminaMultiplier())));
        statsTotal.setIntellect((float) Math.floor(statsTotal.getIntellect() * (1f + statsTotal.getBonusIntellectMultiplier())));
        statsTotal.setSpirit((float) Math.floor(statsTotal.getSpirit() * (1f + statsTotal.getBonusSpiritMultiplier())));
        statsTotal.setArmor((float) Math.floor(statsTotal.getArmor() * (1f + statsTotal.getBonusArmorMultiplier())));

        if (statsTotal.getHighestStat() > 0) {
            if (statsTotal.getSpirit() > statsTotal.getIntellect()) {
                statsTotal.setSpirit(statsTotal.getSpirit() + (float) Math.floor(statsTotal.getHighestStat() * (1f + statsTotal.getBonusSpiritMultiplier())));
            } else {
                statsTotal.setIntellect(statsTotal.getIntellect() + (float) Math.floor(statsTotal.getHighestStat() * (1f + statsTotal.getBonusIntellectMultiplier())));
            }
        }

        // Add spellpower from spirit, intellect and... agility :)
        statsTotal.setSpellPower((float) Math.rint(statsTotal.getSpellPower()
                + (statsTotal.getSpellDamageFromSpiritPercentage() * statsTotal.getSpirit())
                + (statsTotal.getIntellect() * talents.getLunarGuidance() * 0.04)
                + (talents.getNurturingInstinct() * 0.35f * statsTotal.getAgility())));

        statsTotal.setMana(statsTotal.getMana() + StatConversion.getManaFromIntellect(statsTotal.getIntellect()));
        statsTotal.setMana(statsTotal.getMana() * (1f + statsTotal.getBonusManaMultiplier()));

        statsTotal.setHealth((float) Math.rint(statsTotal.getHealth() + StatConversion.getHealthFromStamina(statsTotal.getStamina())));
        statsTotal.setMp5(statsTotal.getMp5() + (float) Math.floor(statsTotal.getIntellect() * (talents.getDreamstate() > 0 ? talents.getDreamstate() * 0.03f + 0.01f : 0f)));

        statsTotal.setSpellCrit((float) TreeFormat.round((StatConversion.getSpellCritFromIntellect(statsTotal.getIntellect())
                + StatConversion.getSpellCritFromRating(statsTotal.getCritRating())
                + (statsTotal.getSpellCrit())
                + 0.01f * talents.getNaturalPerfection()) * 100f, 2));

        return statsTotal;
    }

    public Stats getRelevantStats(Stats stats) {
        Stats s = new Stats();
        // Base stats
        //Stamina = stats.Stamina,
        s.setIntellect(stats.getIntellect());
        s.setSpirit(stats.getSpirit());
        s.setSpellPower(stats.getSpellPower());
        s.setCritRating(stats.getCritRating());
        s.setHasteRating(stats.getHasteRating());
        s.setSpellHaste(stats.getSpellHaste());
        s.setSpellCrit(stats.getSpellCrit());
        //Health = stats.Health,
        s.setMana(stats.getMana());
        s.setMp5(stats.getMp5());
        s.setArmor(stats.getArmor());
        s.setStamina(stats.getStamina());
        s.setManaRestoreFromMaxManaPerSecond(stats.getManaRestoreFromMaxManaPerSecond());
        s.setMovementSpeed(stats.getMovementSpeed());
        s.setBonusHealingDoneMultiplier(stats.getBonusHealingDoneMultiplier());
        // Trinkets
        s.setHighestStat(stats.getHighestStat());
        s.setSpellsManaReduction(stats.getSpellsManaReduction());
        s.setHealingOmenProc(stats.getHealingOmenProc());
        // Idols and sets
        s.setLifebloomCostReduction(stats.getLifebloomCostReduction());  // T7 (2) Bonus
        s.setNourishBonusPerHoT(stats.getNourishBonusPerHoT());          // T7 (4) Bonus
        s.setSwiftmendBonus(stats.getSwiftmendBonus());                  // T8 (2) Bonus
        s.setRejuvenationInstantTick(stats.getRejuvenationInstantTick()); // T8 (4) Bonus
        s.setNourishCritBonus(stats.getNourishCritBonus());              // T9 (2) Bonus
        s.setRejuvenationCrit(stats.getRejuvenationCrit());              // T9 (4) Bonus
        s.setWildGrowthLessReduction(stats.getWildGrowthLessReduction()); // T10 (2) Bonus
        s.setRejuvJumpChance(stats.getRejuvJumpChance());                 // T10 (4) Bonus
        s.setNourishSpellpower(stats.getNourishSpellpower()); // Idol of the Flourishing Life
        s.setRejuvenationHealBonus(stats.getRejuvenationHealBonus()); // Idol of Pure Thoughts (lvl74)
        s.setReduceRejuvenationCost(stats.getReduceRejuvenationCost()); // Idol of Awakening (lvl80)
        s.setLifebloomTickHealBonus(stats.getLifebloomTickHealBonus()); // Idol of Lush Mosh
        s.setHealingTouchFinalHealBonus(stats.getHealingTouchFinalHealBonus()); // Idol of Health
        s.setSwiftmendCdReduc(stats.getSwiftmendCdReduc()); // S7 PvP 4 Pc
        // Gems
        s.setBonusCritHealMultiplier(stats.getBonusCritHealMultiplier());
        s.setBonusManaMultiplier(stats.getBonusManaMultiplier());
        s.setBonusIntellectMultiplier(stats.getBonusIntellectMultiplier());

        for (SpecialEffect effect : stats.specialEffects()) {
            if (effect.getTrigger() == Trigger.Use ||
                    effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.SpellHit
                    || effect.getTrigger() == Trigger.HealingSpellCast || effect.getTrigger() == Trigger.HealingSpellCrit || effect.getTrigger() == Trigger.HealingSpellHit
                    || effect.getTrigger() == Trigger.RejuvenationTick) // Idol of Flaring Growth
            {
                if (hasRelevantSpecialEffectStats(effect.getStats())) {
                    s.addSpecialEffect(effect);
                } else {
                    // Trigger seems relevant, but effect not. Sounds weird, probably DPS bonus on Use or general SpellCast ...
                }
            }
        }
        return s;
    }

    public boolean hasRelevantSpecialEffectStats(Stats stats) {
        for (SpecialEffect effect : stats.specialEffects()) {
            if (effect.getTrigger() == Trigger.Use ||
                    effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.SpellCrit
                    || effect.getTrigger() == Trigger.HealingSpellCast || effect.getTrigger() == Trigger.HealingSpellCrit || effect.getTrigger() == Trigger.HealingSpellHit
                    || effect.getTrigger() == Trigger.RejuvenationTick) // Idol of Flaring Growth
            {
                if (hasRelevantSpecialEffectStats(effect.getStats())) return true;
            }
        }
        return (stats.getIntellect() + stats.getSpirit() + stats.getSpellPower() + stats.getCritRating() + stats.getHasteRating() + stats.getManaRestore()
                + stats.getMp5() + stats.getHealed() + stats.getHighestStat() + stats.getBonusHealingReceived() + stats.getHealingOmenProc()
                + stats.getShieldFromHealed() + stats.getManaRestoreFromMaxManaPerSecond()) > 0;
    }

    @Override
    public boolean hasRelevantStats(Stats stats) {
        if (hasRelevantSpecialEffectStats(stats)) return true;

        if (stats.getIntellect() + stats.getSpirit() + stats.getMp5() + stats.getSpellPower() + stats.getMana() + stats.getCritRating() + stats.getSpellCrit()
                + stats.getHasteRating() + stats.getSpellHaste() + stats.getBonusSpellPowerMultiplier()
                + stats.getBonusSpiritMultiplier() + stats.getBonusIntellectMultiplier() + stats.getBonusStaminaMultiplier()
                + stats.getBonusCritHealMultiplier() + stats.getBonusManaMultiplier()
                /*+ stats.Armor + stats.Stamina*/ + stats.getManaRestoreFromMaxManaPerSecond()
                + stats.getMovementSpeed() + stats.getSpellCombatManaRegeneration() // Bangle of nerfed - might be useful in future
                + stats.getBonusHealingDoneMultiplier()
                // Trinkets
                + stats.getHighestStat() + stats.getSpellsManaReduction() + stats.getHealingOmenProc()
                // Idols and sets
                + stats.getLifebloomCostReduction() + stats.getNourishBonusPerHoT() // T7
                + stats.getSwiftmendBonus() + stats.getRejuvenationInstantTick() // T8
                + stats.getNourishCritBonus() + stats.getRejuvenationCrit() // T9
                + stats.getWildGrowthLessReduction() + stats.getRejuvJumpChance() // T10
                + stats.getNourishSpellpower() // Idol of the Flourishing Life
                + stats.getRejuvenationHealBonus() // Idol of Pure Thoughts (lvl74)
                + stats.getReduceRejuvenationCost() // Idol of Awakening (lvl80)
                + stats.getLifebloomTickHealBonus() // Idol of Lush Mosh
                + stats.getHealingTouchFinalHealBonus() // Idol of Health
                + stats.getSwiftmendCdReduc() // S7 PvP 4 Pc
                > 0)
            return true;

        return false;
    }

    public Stats getBuffsStats(Character character, CalculationOptionsTree calcOpts) {
        List<Buff> removedBuffs = new ArrayList<Buff>();
        List<Buff> addedBuffs = new ArrayList<Buff>();

        Stats statsBuffs = getBuffsStats(character.getActiveBuffs());

        for (Buff b : removedBuffs) {
            character.activeBuffsAdd(b);
        }
        for (Buff b : addedBuffs) {
            character.getActiveBuffs().remove(b);
        }

        return statsBuffs;
    }

    // Wildebees 20090407 : Overload base function to disable all enchants on OffHand for tree druids
    @Override
    public boolean enchantFitsInSlot(rawr.base.items.Enchant enchant, Character character, rawr.base.items.ItemSlot slot) {
        if (slot == rawr.base.items.ItemSlot.OffHand) {
            return false;
        }
        return super.enchantFitsInSlot(enchant, character, slot);
    }

    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            return CalculationOptionsTree.readXml(document.getDocumentElement());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static CalculationsTree instance;

    public static CalculationsTree getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("Tree", CalculationsTree.class);
    }

    public static void register() {
        Calculations.registerModel("Tree", CalculationsTree.class);
    }

    public CalculationsTree() {
        instance = this;
    }
}
