// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Healadin/CalculationsHealadin.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: UI-only members omitted (CalculationOptionsPanel and the RAWR3/WPF color branches);
// the non-Silverlight display label/color/optimizable/chart/glyph tables are ported additively as
// getCharacterDisplayCalculationLabels()/getSubPointNameColors()/getOptimizableCalculationLabels()/
// getCustomChartNames()/getCustomChartData()/getRelevantGlyphs().
// FRAMEWORK ADAPTATION: DeserializeDataObject uses a JDK-XML reflective reader instead of
// XmlSerializer (same approach as CalculationsCat/CalculationsBear).
package rawr.models.healadin;

import rawr.base.stats.BaseStats;
import rawr.base.character.Buff;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterSlot;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Enchant;
import rawr.base.items.GemmingTemplate;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.items.Item;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.talents.PaladinTalents;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class CalculationsHealadin extends CalculationsBase {
    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        // Relevant Gem IDs for Healadin
        // Red
        int[] runed = { 39911, 39998, 40113, 42144 };       // Spell Power

        // Purple
        int[] royal = { 39943, 40027, 40134, 40134 };       // Spell Power + MP5

        // Blue
        int[] lustrous = { 39927, 40010, 40121, 42146 };    // MP5

        // Green
        int[] dazzling = { 39984, 40094, 40175, 40175 };    // Intellect + MP5
        int[] energized = { 39989, 40105, 40179, 40179 };   // Haste + MP5
        int[] sundered = { 39985, 40096, 40176, 40176 };    // Crit + MP5

        // Yellow
        int[] brilliant = { 39912, 40012, 40123, 42148 };   // Intellect
        int[] quick = { 39918, 40017, 40128, 42150 };       // Haste
        int[] smooth = { 39914, 40013, 40124, 42149 };      // Crit

        // Orange
        int[] luminous = { 39946, 40047, 40151, 40151 };    // Spell Power + Intellect
        int[] potent = { 39956, 40048, 40152, 40152 };      // Spell Power + Crit
        int[] reckless = { 39959, 40051, 40155, 40155 };    // Spell Power + Haste

        // Prismatic
        int[] allStats = { 42701, 42702, 49110, 49110 };    // All Stats

        // Meta
        int insightful = 41401;
        int revitalizing = 41376;

        String[] qualityGroupNames = new String[] { "Uncommon", "Rare", "Epic", "Jeweler" };
        String[] typeGroupNames = new String[] { "MP5", "Spell Power", "Intellect", "Haste", "Critical" };

        int[] metaTemplates = new int[] { insightful, revitalizing };

        //    Red           Yellow      Blue        Prismatic
        int[][][] mp5Templates = { // MP5
                { lustrous, lustrous, lustrous, lustrous },
                { royal, dazzling, lustrous, lustrous },
        };

        int[][][] spellPowerTemplates = { // Spell Power
                { runed, runed, runed, runed },
                { runed, luminous, royal, runed },
        };

        int[][][] intellectTemplates = { // Intellect
                { brilliant, brilliant, brilliant, brilliant },
                { allStats, brilliant, brilliant, brilliant },
                { brilliant, allStats, brilliant, brilliant },
                { brilliant, brilliant, allStats, brilliant },
                { brilliant, brilliant, brilliant, allStats },
                { luminous, brilliant, dazzling, brilliant },
        };

        int[][][] hasteTemplates = { // Haste
                { quick, quick, quick, quick },
                { reckless, quick, energized, quick },
        };

        int[][][] critTemplates = { // Crit
                { smooth, smooth, smooth, smooth },
                { potent, smooth, sundered, smooth },
        };

        int[][][][] gemmingTemplates = new int[][][][] { mp5Templates, spellPowerTemplates, intellectTemplates, hasteTemplates, critTemplates };

        // Generate List of Gemming Templates
        List<GemmingTemplate> gemmingTemplate = new ArrayList<GemmingTemplate>();
        for (int qualityId = 0; qualityId <= 3; qualityId++) {
            for (int typeId = 0; typeId <= 4; typeId++) {
                for (int templateId = 0; templateId <= gemmingTemplates[typeId].length - 1; templateId++) {
                    for (int metaId = 0; metaId <= 1; metaId++) {
                        GemmingTemplate template = new GemmingTemplate();
                        template.setModel("Healadin");
                        template.setGroup(qualityGroupNames[qualityId] + " - " + typeGroupNames[typeId]);
                        template.setRedId(gemmingTemplates[typeId][templateId][0][qualityId]);
                        template.setYellowId(gemmingTemplates[typeId][templateId][1][qualityId]);
                        template.setBlueId(gemmingTemplates[typeId][templateId][2][qualityId]);
                        template.setPrismaticId(gemmingTemplates[typeId][templateId][3][qualityId]);
                        template.setMetaId(metaTemplates[metaId]);
                        template.setEnabled("Epic".equals(qualityGroupNames[qualityId]) && "Intellect".equals(typeGroupNames[typeId]));
                        gemmingTemplate.add(template);
                    }
                }
            }
        }
        return gemmingTemplate;
    }

    public void setDefaults(Character character) {
        character.activeBuffsAdd("Swift Retribution");
        character.activeBuffsAdd("Arcane Intellect");
        character.activeBuffsAdd("Judgements of the Wise");
        character.activeBuffsAdd("Blessing of Wisdom");
        character.activeBuffsAdd("Improved Blessing of Wisdom");
        character.activeBuffsAdd("Elemental Oath");
        character.activeBuffsAdd("Wrath of Air Totem");
        character.activeBuffsAdd("Totem of Wrath (Spell Power)");
        character.activeBuffsAdd("Power Word: Fortitude");
        character.activeBuffsAdd("Improved Power Word: Fortitude");
        character.activeBuffsAdd("Mark of the Wild");
        character.activeBuffsAdd("Improved Mark of the Wild");
        character.activeBuffsAdd("Blessing of Kings");
        character.activeBuffsAdd("Flask of the Frost Wyrm");
        character.activeBuffsAdd("Fish Feast");
        character.activeBuffsAdd("Tree of Life Aura");
    }

    // C# CalculationsHealadin.cs:184-192 OptimizableCalculationLabels.
    private String[] optimizableCalculationLabels = null;

    public String[] getOptimizableCalculationLabels() {
        if (this.optimizableCalculationLabels == null)
            this.optimizableCalculationLabels = new String[] {
                    "Health",
                    "Holy Light Cast Time",
                    "Holy Light HPS",
                    "Holy Light Time",
                    "Flash of Light Cast Time",
                    "Flash of Light HPS",
                    "Flash of Light Time",
            };
        return this.optimizableCalculationLabels;
    }

    // C# CalculationsHealadin.cs:216-258 CharacterDisplayCalculationLabels.
    private String[] characterDisplayCalculationLabels = null;

    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        if (this.characterDisplayCalculationLabels == null)
            this.characterDisplayCalculationLabels = new String[] {
                    "Basic Stats:Health",
                    "Basic Stats:Mana",
                    "Basic Stats:Stamina",
                    "Basic Stats:Intellect",
                    "Basic Stats:Spell Power",
                    "Basic Stats:Mp5",
                    "Basic Stats:Spell Crit",
                    "Basic Stats:Spell Haste",

                    "Cycle Stats:Total Healed",
                    "Cycle Stats:Total Mana",
                    "Cycle Stats:Average Healing per sec",
                    "Cycle Stats:Average Healing per mana",

                    "Rotation Info:Holy Light Time",
                    "Rotation Info:Flash of Light Time",
                    "Rotation Info:Holy Shock Time",
                    "Rotation Info:Sacred Shield Time",
                    "Rotation Info:Beacon of Light Time",
                    "Rotation Info:Judgement Time",

                    "Healing Breakdown:Holy Light Healed",
                    "Healing Breakdown:Flash of Light Healed",
                    "Healing Breakdown:Holy Shock Healed",
                    "Healing Breakdown:Sacred Shield Healed",
                    "Healing Breakdown:Beacon of Light Healed",
                    "Healing Breakdown:Glyph of HL Healed",
                    "Healing Breakdown:Other Healed*From trinket procs",

                    "Spell Information:Holy Light",
                    "Spell Information:Flash of Light",
                    "Spell Information:Holy Shock",
                    "Spell Information:Sacred Shield",
            };
        return List.of(this.characterDisplayCalculationLabels);
    }

    // C# CalculationsHealadin.cs:261-274 CustomChartNames.
    private String[] customChartNames = null;

    public String[] getCustomChartNames() {
        if (this.customChartNames == null)
            this.customChartNames = new String[] {
                    "Mana Pool Breakdown",
                    "Mana Usage Breakdown",
                    "Healing Breakdown",
                    "Rotation Breakdown",
            };
        return this.customChartNames;
    }

    // C# CalculationsHealadin.cs:293-306 SubPointNameColors (non-Silverlight branch).
    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> colors = new LinkedHashMap<String, String>();
        colors.put("Fight Healing", "#FF0000");
        colors.put("Burst Healing", "#6495ED");
        return colors;
    }

    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.Paladin;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationHealadin();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsHealadin();
    }

    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            CalculationOptionsHealadin calcOpts = new CalculationOptionsHealadin();
            readXmlObject(document.getDocumentElement(), calcOpts);
            return calcOpts;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void readXmlObject(org.w3c.dom.Element element, Object target) {
        for (org.w3c.dom.Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
            org.w3c.dom.Element child = (org.w3c.dom.Element) node;
            String name = child.getTagName();
            java.lang.reflect.Method setter = findSetter(target.getClass(), "set" + name);
            if (setter == null) continue;
            Class<?> type = setter.getParameterTypes()[0];
            Object value = readXmlValue(child, type);
            if (value == null) continue;
            try {
                setter.invoke(target, value);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static java.lang.reflect.Method findSetter(Class<?> type, String name) {
        for (java.lang.reflect.Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1) {
                return method;
            }
        }
        return null;
    }

    private static Object readXmlValue(org.w3c.dom.Element element, Class<?> type) {
        if (type == String.class) return element.getTextContent().trim();
        String text = element.getTextContent().trim();
        if (type == int.class || type == Integer.class) return text.isEmpty() ? Integer.valueOf(0) : Integer.valueOf(Integer.parseInt(text));
        if (type == float.class || type == Float.class) return text.isEmpty() ? Float.valueOf(0f) : Float.valueOf(Float.parseFloat(text));
        if (type == double.class || type == Double.class) return text.isEmpty() ? Double.valueOf(0d) : Double.valueOf(Double.parseDouble(text));
        if (type == boolean.class || type == Boolean.class) return text.isEmpty() ? Boolean.FALSE : Boolean.valueOf(Boolean.parseBoolean(text));
        if (type.isEnum()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object enumValue = text.isEmpty() ? null : Enum.valueOf((Class<Enum>) type, text);
            return enumValue;
        }
        if (List.class.isAssignableFrom(type)) {
            return new ArrayList<Object>();
        }
        return null;
    }

    public static CalculationsHealadin instance;

    public static CalculationsHealadin getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("Healadin", CalculationsHealadin.class);
    }

    public static void register() {
        Calculations.registerModel("Healadin", CalculationsHealadin.class);
    }

    public CalculationsHealadin() {
        instance = this;
    }

    // C# CalculationsHealadin.cs:323-349 GetCharacterCalculations.
    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        if (referenceCalculation && significantChange) {
            // Fresh top-level character calculation: drop shared effect interpolation caches so a
            // previous character cannot leak into this one (SUSPECTED_BUGS #15). Comparisons and
            // internal re-calculations keep the caches, matching the original per-process behavior.
            rawr.base.effects.SpecialEffect.clearInterpolatorCaches();
        }
        if (character == null) return new CharacterCalculationsHealadin();

        Stats stats;
        CharacterCalculationsHealadin calc = null;
        PaladinTalents talents = character.getPaladinTalents();

        for (int i = 0; i < 5; i++) {
            float oldBurst = 0;
            if (i > 0) oldBurst = calc.getBurstPoints();

            stats = getCharacterStats(character, additionalItem, true, calc);
            calc = new CharacterCalculationsHealadin();

            Rotation rot = new Rotation(character, stats);

            if (i > 0) calc.setBurstPoints(oldBurst);
            else calc.setBurstPoints(rot.calculateBurstHealing(calc));
            calc.setFightPoints(rot.calculateFightHealing(calc));
            calc.setOverallPoints(calc.getBurstPoints() + calc.getFightPoints());
        }
        calc.basicStats = getCharacterStats(character, additionalItem, false, null);

        return calc;
    }

    // region Stat Calculation
    public Stats getCharacterStats(Character character, Item additionalItem) {
        return getCharacterStats(character, additionalItem, true, null);
    }

    // C# CalculationsHealadin.cs:357-440.
    public Stats getCharacterStats(Character character, Item additionalItem, boolean computeAverageStats, CharacterCalculationsHealadin calc) {
        PaladinTalents talents = character.getPaladinTalents();

        CalculationOptionsHealadin calcOpts = character.getCalculationOptions() instanceof CalculationOptionsHealadin
                ? (CalculationOptionsHealadin) character.getCalculationOptions()
                : new CalculationOptionsHealadin();

        float fightLength = calcOpts.getLength() * 60f;

        Stats statsRace = BaseStats.getBaseStats(character.getLevel(), CharacterClass.Paladin, character.getRace());
        Stats statsBaseGear = new Stats();
        accumulateItemStats(statsBaseGear, character, additionalItem);
        Stats statsBuffs = getBuffsStats(character, calcOpts);
        Stats stats = Stats.add(Stats.add(statsBaseGear, statsBuffs), statsRace);

        convertRatings(stats, talents, calcOpts);
        if (computeAverageStats) {
            Stats statsAverage = new Stats();

            for (SpecialEffect effect : stats.specialEffects()) {
                float trigger = 0f;
                if (calc == null) {
                    trigger = 1.5f / calcOpts.getActivity() / (1f + stats.getSpellHaste());
                    if (effect.getTrigger() == Trigger.HealingSpellCrit || effect.getTrigger() == Trigger.SpellCrit)
                        trigger *= stats.getSpellCrit();
                    else if (effect.getTrigger() == Trigger.HolyShockCast)
                        trigger = 6f / calcOpts.getHolyShock();
                } else {
                    if (effect.getTrigger() == Trigger.HealingSpellCast || effect.getTrigger() == Trigger.HealingSpellHit)
                        trigger = 1f / Rotation.getHealingCastsPerSec(calc);
                    else if (effect.getTrigger() == Trigger.HealingSpellCrit || effect.getTrigger() == Trigger.SpellCrit)
                        trigger = 1f / Rotation.getHealingCritsPerSec(calc);
                    else if (effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.SpellHit)
                        trigger = 1f / Rotation.getSpellCastsPerSec(calc);
                    else if (effect.getTrigger() == Trigger.DamageOrHealingDone)
                        trigger = 1f / Rotation.getHealingCastsPerSec(calc);
                    else if (effect.getTrigger() == Trigger.HolyShockCast)
                        trigger = 6f / calcOpts.getHolyShock();
                    else if (effect.getTrigger() == Trigger.Use) {
                        trigger = 0f;
                        for (SpecialEffect childEffect : effect.getStats().specialEffects()) {
                            float childTrigger = 0f;

                            if (effect.getTrigger() == Trigger.HealingSpellCast || effect.getTrigger() == Trigger.HealingSpellHit)
                                childTrigger = 1f / Rotation.getHealingCastsPerSec(calc);
                            else if (effect.getTrigger() == Trigger.HealingSpellCrit || effect.getTrigger() == Trigger.SpellCrit)
                                childTrigger = 1f / Rotation.getHealingCritsPerSec(calc);
                            else if (effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.SpellHit)
                                childTrigger = 1f / Rotation.getSpellCastsPerSec(calc);

                            statsAverage.accumulate(childEffect.getStats(),
                                    effect.getAverageUptime(0.0f, 1.0f)
                                            * childEffect.getAverageStackSize(childTrigger, 1f, 1.5f, effect.getDuration()));
                        }
                    } else if (effect.getTrigger() == Trigger.HolyLightCast) trigger = 1f / Rotation.getHolyLightCastsPerSec(calc);
                    else continue;
                }
                statsAverage.accumulate(effect.getAverageStats(trigger, 1f, 1.5f, fightLength));
            }
            statsAverage.setManaRestore(statsAverage.getManaRestore() * fightLength);
            statsAverage.setHealed(statsAverage.getHealed() * fightLength);

            stats = Stats.add(Stats.add(Stats.add(statsBaseGear, statsBuffs), statsRace), statsAverage);
            convertRatings(stats, talents, calcOpts);
        }

        return stats;
    }

    // C# CalculationsHealadin.cs:442-479 ConvertRatings.
    private void convertRatings(Stats stats, PaladinTalents talents, CalculationOptionsHealadin calcOpts) {
        stats.setStamina(stats.getStamina() * (1f + stats.getBonusStaminaMultiplier()));

        // Intellect is used to calculate initial mana pool.
        // To avoid temporary intellect from highest stat procs changing initial mana pool
        // we track temporary intellect separatly in HighestStat property and combine it with intellect
        // when needed.
        stats.setIntellect(stats.getIntellect() * ((1f + stats.getBonusIntellectMultiplier()) * (1f + talents.getDivineIntellect() * .02f)));
        stats.setHighestStat(stats.getHighestStat() * ((1f + stats.getBonusIntellectMultiplier()) * (1f + talents.getDivineIntellect() * .02f)));

        stats.setSpellPower(stats.getSpellPower() + 0.04f * (stats.getIntellect() + stats.getHighestStat()) * talents.getHolyGuidance());
        stats.setSpellCrit(stats.getSpellCrit() +
                StatConversion.getSpellCritFromIntellect(
                        stats.getIntellect() + stats.getHighestStat(),
                        CharacterClass.Paladin) +
                StatConversion.getSpellCritFromRating(stats.getCritRating(), CharacterClass.Paladin) +
                talents.getSanctityOfBattle() * .01f +
                talents.getConviction() * .01f);

        stats.setSpellHaste((1f + talents.getJudgementsOfThePure() * (calcOpts.getJotP() ? .03f : 0f)) *
                (1f + stats.getSpellHaste()) *
                (1f + StatConversion.getSpellHasteFromRating(stats.getHasteRating(), CharacterClass.Paladin))
                - 1f);

        // GetManaFromIntellect/GetHealthFromStamina account for the fact
        // that the first 20 Int/Sta only give 1 Mana/Health each.
        stats.setMana(stats.getMana() + StatConversion.getManaFromIntellect(stats.getIntellect(), CharacterClass.Paladin) *
                (1f + stats.getBonusManaMultiplier()));
        stats.setHealth(stats.getHealth() + StatConversion.getHealthFromStamina(stats.getStamina(), CharacterClass.Paladin));

        stats.setPhysicalHit(stats.getPhysicalHit() + StatConversion.getPhysicalHitFromRating(
                stats.getHitRating(),
                CharacterClass.Paladin));
    }
    // endregion

    // region Custom Charts
    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        if ("Mana Pool Breakdown".equals(chartName)) {
            CharacterCalculationsHealadin calc = (CharacterCalculationsHealadin) getCharacterCalculations(character);
            if (calc == null) calc = new CharacterCalculationsHealadin();

            ComparisonCalculationHealadin Base = new ComparisonCalculationHealadin();
            Base.setName("Base");
            ComparisonCalculationHealadin Mp5 = new ComparisonCalculationHealadin();
            Mp5.setName("Mp5");
            ComparisonCalculationHealadin Replenishment = new ComparisonCalculationHealadin();
            Replenishment.setName("Replenishment");
            ComparisonCalculationHealadin ArcaneTorrent = new ComparisonCalculationHealadin();
            ArcaneTorrent.setName("Arcane Torrent");
            ComparisonCalculationHealadin DivinePlea = new ComparisonCalculationHealadin();
            DivinePlea.setName("Divine Plea");
            ComparisonCalculationHealadin LoH = new ComparisonCalculationHealadin();
            LoH.setName("Lay on Hands");
            ComparisonCalculationHealadin Other = new ComparisonCalculationHealadin();
            Other.setName("Potion & Other");

            Base.setThroughputPoints(calc.manaBase);
            Base.setOverallPoints(Base.getThroughputPoints());
            Mp5.setThroughputPoints(calc.manaMp5);
            Mp5.setOverallPoints(Mp5.getThroughputPoints());
            LoH.setThroughputPoints(calc.manaLayOnHands);
            LoH.setOverallPoints(LoH.getThroughputPoints());
            Replenishment.setThroughputPoints(calc.manaReplenishment);
            Replenishment.setOverallPoints(Replenishment.getThroughputPoints());
            ArcaneTorrent.setThroughputPoints(calc.manaArcaneTorrent);
            ArcaneTorrent.setOverallPoints(ArcaneTorrent.getThroughputPoints());
            DivinePlea.setThroughputPoints(calc.manaDivinePlea);
            DivinePlea.setOverallPoints(DivinePlea.getThroughputPoints());
            Other.setThroughputPoints(calc.manaOther);
            Other.setOverallPoints(Other.getThroughputPoints());

            return new ComparisonCalculationBase[] { Base, Mp5, Replenishment, LoH, ArcaneTorrent, DivinePlea, Other };
        } else if ("Mana Usage Breakdown".equals(chartName)) {
            CharacterCalculationsHealadin calc = (CharacterCalculationsHealadin) getCharacterCalculations(character);
            if (calc == null) calc = new CharacterCalculationsHealadin();

            ComparisonCalculationHealadin FoL = new ComparisonCalculationHealadin();
            FoL.setName("Flash of Light");
            ComparisonCalculationHealadin HL = new ComparisonCalculationHealadin();
            HL.setName("Holy Light");
            ComparisonCalculationHealadin HS = new ComparisonCalculationHealadin();
            HS.setName("Holy Shock");
            ComparisonCalculationHealadin JotP = new ComparisonCalculationHealadin();
            JotP.setName("Judgements and Seals");
            ComparisonCalculationHealadin BoL = new ComparisonCalculationHealadin();
            BoL.setName("Beacon of Light");
            ComparisonCalculationHealadin SS = new ComparisonCalculationHealadin();
            SS.setName("Sacred Shield");

            FoL.setThroughputPoints(calc.usageFoL);
            FoL.setOverallPoints(FoL.getThroughputPoints());
            HL.setThroughputPoints(calc.usageHL);
            HL.setOverallPoints(HL.getThroughputPoints());
            HS.setThroughputPoints(calc.usageHS);
            HS.setOverallPoints(HS.getThroughputPoints());
            JotP.setThroughputPoints(calc.usageJotP);
            JotP.setOverallPoints(JotP.getThroughputPoints());
            BoL.setThroughputPoints(calc.usageBoL);
            BoL.setOverallPoints(BoL.getThroughputPoints());
            SS.setThroughputPoints(calc.usageSS);
            SS.setOverallPoints(SS.getThroughputPoints());

            return new ComparisonCalculationBase[] { FoL, HL, HS, JotP, BoL, SS };
        } else if ("Healing Breakdown".equals(chartName)) {
            CharacterCalculationsHealadin calc = (CharacterCalculationsHealadin) getCharacterCalculations(character);
            if (calc == null) calc = new CharacterCalculationsHealadin();

            ComparisonCalculationHealadin FoL = new ComparisonCalculationHealadin();
            FoL.setName("Flash of Light");
            ComparisonCalculationHealadin HL = new ComparisonCalculationHealadin();
            HL.setName("Holy Light");
            ComparisonCalculationHealadin HS = new ComparisonCalculationHealadin();
            HS.setName("Holy Shock");
            ComparisonCalculationHealadin GHL = new ComparisonCalculationHealadin();
            GHL.setName("Glyph of Holy Light");
            ComparisonCalculationHealadin BoL = new ComparisonCalculationHealadin();
            BoL.setName("Beacon of Light");
            ComparisonCalculationHealadin SS = new ComparisonCalculationHealadin();
            SS.setName("Sacred Shield");

            FoL.setThroughputPoints(calc.healedFoL / calc.fightLength);
            FoL.setOverallPoints(FoL.getThroughputPoints());
            HL.setThroughputPoints(calc.healedHL / calc.fightLength);
            HL.setOverallPoints(HL.getThroughputPoints());
            HS.setThroughputPoints(calc.healedHS / calc.fightLength);
            HS.setOverallPoints(HS.getThroughputPoints());
            GHL.setThroughputPoints(calc.healedGHL / calc.fightLength);
            GHL.setOverallPoints(GHL.getThroughputPoints());
            BoL.setThroughputPoints(calc.healedBoL / calc.fightLength);
            BoL.setOverallPoints(BoL.getThroughputPoints());
            SS.setThroughputPoints(calc.healedSS / calc.fightLength);
            SS.setOverallPoints(SS.getThroughputPoints());

            return new ComparisonCalculationBase[] { FoL, HL, HS, GHL, BoL, SS };
        } else if ("Rotation Breakdown".equals(chartName)) {
            CharacterCalculationsHealadin calc = (CharacterCalculationsHealadin) getCharacterCalculations(character);
            if (calc == null) calc = new CharacterCalculationsHealadin();

            ComparisonCalculationHealadin FoL = new ComparisonCalculationHealadin();
            FoL.setName("Flash of Light");
            ComparisonCalculationHealadin HL = new ComparisonCalculationHealadin();
            HL.setName("Holy Light");
            ComparisonCalculationHealadin HS = new ComparisonCalculationHealadin();
            HS.setName("Holy Shock");
            ComparisonCalculationHealadin JotP = new ComparisonCalculationHealadin();
            JotP.setName("Judgements and Seals");
            ComparisonCalculationHealadin BoL = new ComparisonCalculationHealadin();
            BoL.setName("Beacon of Light");
            ComparisonCalculationHealadin SS = new ComparisonCalculationHealadin();
            SS.setName("Sacred Shield");

            FoL.setThroughputPoints(calc.rotationFoL);
            FoL.setOverallPoints(FoL.getThroughputPoints());
            HL.setThroughputPoints(calc.rotationHL);
            HL.setOverallPoints(HL.getThroughputPoints());
            HS.setThroughputPoints(calc.rotationHS);
            HS.setOverallPoints(HS.getThroughputPoints());
            JotP.setThroughputPoints(calc.rotationJotP);
            JotP.setOverallPoints(JotP.getThroughputPoints());
            BoL.setThroughputPoints(calc.rotationBoL);
            BoL.setOverallPoints(BoL.getThroughputPoints());
            SS.setThroughputPoints(calc.rotationSS);
            SS.setOverallPoints(SS.getThroughputPoints());

            return new ComparisonCalculationBase[] { FoL, HL, HS, JotP, BoL, SS };
        }
        return new ComparisonCalculationBase[] {};
    }
    // endregion Custom Charts

    // region Relevancy Methods

    private static boolean isSpiritIrrelevant = true;

    public static boolean getIsSpiritIrrelevant() {
        return isSpiritIrrelevant;
    }

    public static void setIsSpiritIrrelevant(boolean value) {
        isSpiritIrrelevant = value;
    }

    private static boolean isHitIrrelevant = true;

    public static boolean getIsHitIrrelevant() {
        return isHitIrrelevant;
    }

    public static void setIsHitIrrelevant(boolean value) {
        isHitIrrelevant = value;
    }

    // C# CalculationsHealadin.cs:591-614 RelevantItemTypes.
    private List<ItemType> relevantItemTypes = null;

    @Override
    public List<ItemType> getRelevantItemTypes() {
        if (this.relevantItemTypes == null) {
            this.relevantItemTypes = new ArrayList<ItemType>(List.of(
                    ItemType.Plate,
                    ItemType.Mail,
                    ItemType.Leather,
                    ItemType.Cloth,
                    ItemType.None,
                    ItemType.Shield,
                    ItemType.Libram,
                    ItemType.OneHandAxe,
                    ItemType.OneHandMace,
                    ItemType.OneHandSword));
        }
        return this.relevantItemTypes;
    }

    private static List<String> relevantGlyphs;

    public List<String> getRelevantGlyphs() {
        if (relevantGlyphs == null) {
            relevantGlyphs = new ArrayList<String>();
            relevantGlyphs.add("Glyph of Seal of Wisdom");
            relevantGlyphs.add("Glyph of Seal of Light");
            relevantGlyphs.add("Glyph of Holy Light");
            relevantGlyphs.add("Glyph of Flash of Light");
            relevantGlyphs.add("Glyph of Holy Shock");
            relevantGlyphs.add("Glyph of Divinity");
            relevantGlyphs.add("Glyph of Beacon of Light");
            relevantGlyphs.add("Glyph of the Wise");
            relevantGlyphs.add("Glyph of Lay on Hands");
        }
        return relevantGlyphs;
    }

    @Override
    public boolean enchantFitsInSlot(Enchant enchant, Character character, ItemSlot slot) {
        // Filters out Non-Shield Offhand Enchants and Ranged Enchants
        if ((slot == ItemSlot.OffHand && enchant.slot != ItemSlot.OffHand) || slot == ItemSlot.Ranged) return false;
        // Filters out Death Knight and Two-Hander Enchants
        if (enchant.name.startsWith("Rune of the") || enchant.slot == ItemSlot.TwoHand) return false;

        return super.enchantFitsInSlot(enchant, character, slot);
    }

    @Override
    public boolean itemFitsInSlot(Item item, Character character, CharacterSlot slot, boolean ignoreUnique) {
        if (slot == CharacterSlot.OffHand && !(item.getType() == ItemType.Shield || item.getType() == ItemType.None)) return false;
        return super.itemFitsInSlot(item, character, slot, ignoreUnique);
    }

    private boolean isTriggerRelevant(Trigger trigger) {
        return (
                trigger == Trigger.Use                 || trigger == Trigger.HolyLightCast     ||
                trigger == Trigger.SpellCast           || trigger == Trigger.SpellCrit         ||
                trigger == Trigger.SpellHit            || trigger == Trigger.HealingSpellCast  ||
                trigger == Trigger.HealingSpellCrit    || trigger == Trigger.HealingSpellHit   ||
                trigger == Trigger.DamageOrHealingDone || trigger == Trigger.HolyShockCast
        );
    }

    public Stats getRelevantStats(Stats stats) {
        Stats s = new Stats();

        // region These aren't really relevant stats, but they should still appear in the tooltip

        s.setStamina(stats.getStamina());
        s.setHealth(stats.getHealth());
        s.setSpirit(stats.getSpirit());
        s.setHitRating(stats.getHitRating());

        // endregion

        s.setIntellect(stats.getIntellect());
        s.setMp5(stats.getMp5());
        s.setSpellPower(stats.getSpellPower());
        s.setCritRating(stats.getCritRating());
        s.setHasteRating(stats.getHasteRating());
        s.setMana(stats.getMana());
        s.setManaRestore(stats.getManaRestore());
        s.setBonusIntellectMultiplier(stats.getBonusIntellectMultiplier());
        s.setBonusManaPotion(stats.getBonusManaPotion());
        s.setSpellCrit(stats.getSpellCrit());
        s.setSpellHaste(stats.getSpellHaste());
        s.setFlashOfLightCrit(stats.getFlashOfLightCrit());
        s.setFlashOfLightSpellPower(stats.getFlashOfLightSpellPower());
        s.setFlashOfLightMultiplier(stats.getFlashOfLightMultiplier());
        s.setHolyShockCrit(stats.getHolyShockCrit());
        s.setHolyLightSpellPower(stats.getHolyLightSpellPower());
        s.setHolyLightCrit(stats.getHolyLightCrit());
        s.setHolyLightManaCostReduction(stats.getHolyLightManaCostReduction());
        s.setHolyLightPercentManaReduction(stats.getHolyLightPercentManaReduction());
        s.setHealingReceivedMultiplier(stats.getHealingReceivedMultiplier());
        s.setBonusHealingDoneMultiplier(stats.getBonusHealingDoneMultiplier());
        s.setBonusJudgementDuration(stats.getBonusJudgementDuration());
        s.setFlashOfLightHoTMultiplier(stats.getFlashOfLightHoTMultiplier());
        s.setDivineIlluminationHealingMultiplier(stats.getDivineIlluminationHealingMultiplier());
        s.setHolyLightCastTimeReductionFromHolyShock(stats.getHolyLightCastTimeReductionFromHolyShock());
        s.setMovementSpeed(stats.getMovementSpeed());
        s.setShieldFromHealed(stats.getShieldFromHealed());

        // Gear Procs
        s.setManaRestoreFromMaxManaPerSecond(stats.getManaRestoreFromMaxManaPerSecond());
        s.setBonusManaMultiplier(stats.getBonusManaMultiplier());
        s.setBonusCritHealMultiplier(stats.getBonusCritHealMultiplier());
        s.setSpellsManaReduction(stats.getSpellsManaReduction());
        s.setSacredShieldICDReduction(stats.getSacredShieldICDReduction());
        s.setHolyShockHoTOnCrit(stats.getHolyShockHoTOnCrit());

        // Ony Shiny Shard of the Flame
        s.setHealed(stats.getHealed());

        for (SpecialEffect effect : stats.specialEffects()) {
            if (isTriggerRelevant(effect.getTrigger()) && hasRelevantStats(effect.getStats())) {
                s.addSpecialEffect(effect);
            }
        }
        return s;
    }

    boolean useIrrelevancy = false;

    @Override
    public boolean isItemRelevant(Item item) {
        if (!item.isGem()) {
            useIrrelevancy = true;
        }
        boolean result = super.isItemRelevant(item);
        useIrrelevancy = false;
        return result;
    }

    @Override
    public boolean hasRelevantStats(Stats stats) {
        if (useIrrelevancy) {
            if (isSpiritIrrelevant && stats.getSpirit() > 0) return false;
            if (isHitIrrelevant && stats.getHitRating() > 0) return false;
        }

        boolean relevant = (
                stats.getIntellect() +
                stats.getMp5() +
                stats.getSpellPower() +
                stats.getCritRating() +
                stats.getHasteRating() +
                stats.getMana() +
                stats.getManaRestore() +
                stats.getBonusIntellectMultiplier() +
                stats.getBonusManaPotion() +
                stats.getSpellCrit() +
                stats.getSpellHaste() +
                stats.getFlashOfLightCrit() +
                stats.getFlashOfLightSpellPower() +
                stats.getFlashOfLightMultiplier() +
                stats.getHolyShockCrit() +
                stats.getHolyLightSpellPower() +
                stats.getHolyLightCrit() +
                stats.getHolyLightManaCostReduction() +
                stats.getHolyLightPercentManaReduction() +
                stats.getHealingReceivedMultiplier() +
                stats.getBonusHealingDoneMultiplier() +
                stats.getBonusJudgementDuration() +
                stats.getFlashOfLightHoTMultiplier() +
                stats.getDivineIlluminationHealingMultiplier() +
                stats.getHolyLightCastTimeReductionFromHolyShock() +
                stats.getMovementSpeed() +
                stats.getShieldFromHealed() +
                stats.getHighestStat() +

                stats.getManaRestoreFromMaxManaPerSecond() +
                stats.getBonusManaMultiplier() +
                stats.getBonusCritHealMultiplier() +
                stats.getSpellsManaReduction() +
                stats.getSacredShieldICDReduction() +
                stats.getHolyShockHoTOnCrit() +

                stats.getHealed()
                ) != 0;

        boolean hasTrigger = false;
        boolean hasRelevantTrigger = false;
        boolean triggerHasRelevantStats;

        for (SpecialEffect effect : stats.specialEffects()) {
            hasTrigger = true;
            if (isTriggerRelevant(effect.getTrigger())) {
                triggerHasRelevantStats = hasRelevantStats(effect.getStats());
                relevant |= triggerHasRelevantStats;
                hasRelevantTrigger |= triggerHasRelevantStats;
            }
        }
        return relevant && (!hasTrigger || hasRelevantTrigger);
    }

    @Override
    public boolean isBuffRelevant(Buff buff, Character character) {
        Stats stats = buff.getTotalStats();
        boolean hasRelevantBuffStats = hasRelevantStats(stats);

        boolean notClassSetBonus = "Set Bonuses".equals(buff.group) && (buff.getAllowedClasses().size() != 1 || (buff.getAllowedClasses().size() == 1 && !buff.getAllowedClasses().contains(CharacterClass.Paladin)));

        boolean relevant = hasRelevantBuffStats && !notClassSetBonus;
        return relevant;
    }

    public Stats getBuffsStats(Character character, CalculationOptionsHealadin calcOpts) {
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
    // endregion
}
