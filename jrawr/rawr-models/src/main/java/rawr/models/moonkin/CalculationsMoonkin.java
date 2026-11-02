// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Moonkin/CalculationsMoonkin.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: UI-only members omitted (CalculationOptionsPanel, the RAWR3/WPF color branch and
// RenderCustomChart); the non-Silverlight display label/color/optimizable/chart/glyph tables are
// ported additively as getCharacterDisplayCalculationLabels()/getSubPointNameColors()/
// getOptimizableCalculationLabels()/getCustomChartNames()/getCustomChartData()/getRelevantGlyphs().
// GetBuffsStats(Character, CalculationOptionsMoonkin) only called the base GetBuffsStats(ActiveBuffs)
// (its auto-fix block is commented out in C#) and is therefore not overridden; getCharacterStats
// calls the base directly.
// FRAMEWORK ADAPTATION: DeserializeDataObject uses a JDK-XML reflective reader instead of
// XmlSerializer (same approach as CalculationsCat/Bear/Healadin).
package rawr.models.moonkin;

import rawr.base.character.Buff;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterRace;
import rawr.base.character.CharacterSlot;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Enchant;
import rawr.base.items.GemmingTemplate;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.items.Item;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class CalculationsMoonkin extends CalculationsBase {
    /**
     * Set default buffs.
     */
    public void setDefaults(Character character) {
        character.activeBuffsAdd(("Sanctified Retribution"));
        character.activeBuffsAdd(("Improved Moonkin Form"));
        character.activeBuffsAdd(("Arcane Intellect"));
        character.activeBuffsAdd(("Judgements of the Wise"));
        character.activeBuffsAdd(("Blessing of Wisdom"));
        character.activeBuffsAdd(("Improved Blessing of Wisdom"));
        character.activeBuffsAdd(("Moonkin Form"));
        character.activeBuffsAdd(("Wrath of Air Totem"));
        character.activeBuffsAdd(("Totem of Wrath (Spell Power)"));
        character.activeBuffsAdd(("Divine Spirit"));
        character.activeBuffsAdd(("Power Word: Fortitude"));
        character.activeBuffsAdd(("Improved Power Word: Fortitude"));
        character.activeBuffsAdd(("Mark of the Wild"));
        character.activeBuffsAdd(("Improved Mark of the Wild"));
        character.activeBuffsAdd(("Blessing of Kings"));
        character.activeBuffsAdd(("Heart of the Crusader"));
        character.activeBuffsAdd(("Judgement of Wisdom"));
        character.activeBuffsAdd(("Improved Scorch"));
        character.activeBuffsAdd(("Earth and Moon"));
        character.activeBuffsAdd(("Faerie Fire"));
        character.activeBuffsAdd(("Improved Faerie Fire"));
        character.activeBuffsAdd(("Sunder Armor"));
        character.activeBuffsAdd(("Heroism/Bloodlust"));
        character.activeBuffsAdd(("Flask of the Frost Wyrm"));
        character.activeBuffsAdd(("Fish Feast"));
    }

    private String[] optimizableCalculationLabels = null;

    /**
     * Labels of the stats available to the Optimizer
     */
    public String[] getOptimizableCalculationLabels() {
        if (this.optimizableCalculationLabels == null)
            this.optimizableCalculationLabels = new String[] {
                    "Hit Rating",
                    "Haste Rating",
                    "Crit Rating"
            };
        return this.optimizableCalculationLabels;
    }

    /**
     * List of gemming templates available to Rawr.
     */
    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        List<GemmingTemplate> list = new ArrayList<GemmingTemplate>();
        // Uncommon gems
        // All sockets = Runed Bloodstone
        // Meta = Chaotic
        list.add(moonkinTemplate("Uncommon", 39911, 39911, 39911, 39911, 41285, false));
        // Perfect uncommon gems
        // All sockets = Perfect Runed Bloodstone
        // Meta = Chaotic
        list.add(moonkinTemplate("Uncommon", 41438, 41438, 41438, 41438, 41285, false));
        // Uncommon gems
        // All but blue = Runed Bloodstone
        // Blue = Purified Shadow Crystal (spirit/spell power)
        // Meta = Chaotic
        list.add(moonkinTemplate("Uncommon", 39911, 39911, 39941, 39911, 41285, false));
        // Perfect uncommon gems
        // All but blue = Perfect Runed Bloodstone
        // Blue = Perfect Purified Shadow Crystal (spirit/spell power)
        // Meta = Chaotic
        list.add(moonkinTemplate("Uncommon", 41438, 41438, 41457, 41438, 41285, false));
        // Rare gems
        // All but blue = Runed Scarlet Ruby
        // Blue = Purified Twilight Opal (spirit/spell power)
        // Meta = Chaotic
        list.add(moonkinTemplate("Rare", 39998, 39998, 40026, 39998, 41285, false));
        // Rare gems
        // All sockets = Runed Scarlet Ruby
        // Meta = Chaotic
        list.add(moonkinTemplate("Rare", 39998, 39998, 39998, 39998, 41285, false));
        // Red = Runed Scarlet Ruby
        // Blue = Purified Twilight Opal (spirit/spell power)
        // Yellow = Reckless Monarch Topaz (spell power/haste)
        // Meta = Chaotic
        list.add(moonkinTemplate("Rare", 39998, 40051, 40026, 39998, 41285, false));
        // All sockets but yellow = Runed Scarlet Ruby
        // Yellow = Reckless Monarch Topaz (spell power/haste)
        // Meta = Chaotic
        list.add(moonkinTemplate("Rare", 39998, 40051, 39998, 39998, 41285, false));
        // Red = Runed Scarlet Ruby
        // Blue = Purified Twilight Opal (spirit/spell power)
        // Yellow = Veiled Monarch Topaz (spell power/hit)
        // Meta = Chaotic
        list.add(moonkinTemplate("Rare", 39998, 40049, 40026, 39998, 41285, false));
        // All sockets but yellow = Runed Scarlet Ruby
        // Yellow = Veiled Monarch Topaz (spell power/hit)
        // Meta = Chaotic
        list.add(moonkinTemplate("Rare", 39998, 40049, 39998, 39998, 41285, false));
        // Red = Runed Scarlet Ruby
        // Blue = Purified Twilight Opal (spirit/spell power)
        // Yellow = Potent Monarch Topaz (spell power/crit)
        // Meta = Chaotic
        list.add(moonkinTemplate("Rare", 39998, 40048, 40026, 39998, 41285, false));
        // Rare gems
        // All sockets but yellow = Runed Scarlet Ruby
        // Yellow = Potent Monarch Topaz (spell power/crit)
        // Meta = Chaotic
        list.add(moonkinTemplate("Rare", 39998, 40048, 39998, 39998, 41285, false));
        // Epic gems
        // All but blue = Runed Cardinal Ruby
        // Blue = Purified Dreadstone (spirit/spell power)
        // Meta = Chaotic
        list.add(moonkinTemplate("Epic", 40113, 40113, 40133, 40113, 41285, true));
        // Epic gems
        // All sockets = Runed Cardinal Ruby
        // Meta = Chaotic
        list.add(moonkinTemplate("Epic", 40113, 40113, 40113, 40113, 41285, true));
        // Red = Runed Cardinal Ruby
        // Blue = Purified Dreadstone (spirit/spell power)
        // Yellow = Reckless Ametrine (spell power/haste)
        // Meta = Chaotic
        list.add(moonkinTemplate("Epic", 40113, 40155, 40133, 40113, 41285, true));
        // All sockets but yellow = Runed Cardinal Ruby
        // Yellow = Reckless Ametrine (spell power/haste)
        // Meta = Chaotic
        list.add(moonkinTemplate("Epic", 40113, 40155, 40113, 40113, 41285, true));
        // Red = Runed Cardinal Ruby
        // Blue = Purified Dreadstone (spirit/spell power)
        // Yellow = Veiled Ametrine (spell power/hit)
        // Meta = Chaotic
        list.add(moonkinTemplate("Epic", 40113, 40153, 40133, 40113, 41285, true));
        // All sockets but yellow = Runed Cardinal Ruby
        // Yellow = Veiled Ametrine (spell power/hit)
        // Meta = Chaotic
        list.add(moonkinTemplate("Epic", 40113, 40153, 40113, 40113, 41285, true));
        // Red = Runed Cardinal Ruby
        // Blue = Purified Dreadstone (spirit/spell power)
        // Yellow = Potent Ametrine (spell power/crit)
        // Meta = Chaotic
        list.add(moonkinTemplate("Epic", 40113, 40152, 40133, 40113, 41285, true));
        // All sockets but yellow = Runed Cardinal Ruby
        // Yellow = Potent Ametrine (spell power/crit)
        // Meta = Chaotic
        list.add(moonkinTemplate("Epic", 40113, 40152, 40113, 40113, 41285, true));
        // Jewelcrafting gems
        // Red sockets = Runed Dragon's Eye
        // All other sockets = Runed Cardinal Ruby
        // Meta = Chaotic
        list.add(moonkinTemplate("Jeweler", 42144, 40113, 40113, 40113, 41285, false));
        // Jewelcrafting gems
        // Red sockets = Runed Dragon's Eye
        // Blue sockets = Purified Dreadstone
        // All other sockets = Runed Cardinal Ruby
        // Meta = Chaotic
        list.add(moonkinTemplate("Jeweler", 42144, 40113, 40133, 40113, 41285, false));
        // Jewelcrafting gems
        // Red, Yellow sockets = Runed Cardinal Ruby
        // Blue sockets = Runed Dragon's Eye
        // Meta = Chaotic
        list.add(moonkinTemplate("Jeweler", 40113, 40113, 42144, 40113, 41285, false));
        // Jewelcrafting gems
        // Yellow sockets = Runed Dragon's Eye
        // All other sockets = Runed Cardinal Ruby
        // Meta = Chaotic
        list.add(moonkinTemplate("Jeweler", 40113, 42144, 40113, 40113, 41285, false));
        // Jewelcrafting gems
        // Yellow sockets = Runed Dragon's Eye
        // Blue sockets = Purified Dreadstone
        // All other sockets = Runed Cardinal Ruby
        // Meta = Chaotic
        list.add(moonkinTemplate("Jeweler", 40113, 42144, 40133, 40113, 41285, false));
        // Jewelcrafting gems
        // All sockets = Runed Dragon's Eye
        // Meta = Chaotic
        list.add(moonkinTemplate("Jeweler", 42144, 42144, 42144, 42144, 41285, false));
        return list;
    }

    private static GemmingTemplate moonkinTemplate(String group, int redId, int yellowId, int blueId, int prismaticId, int metaId, boolean enabled) {
        GemmingTemplate template = new GemmingTemplate();
        template.setModel("Moonkin");
        template.setGroup(group);
        template.setRedId(redId);
        template.setYellowId(yellowId);
        template.setBlueId(blueId);
        template.setPrismaticId(prismaticId);
        template.setMetaId(metaId);
        template.setEnabled(enabled);
        return template;
    }

    @Override
    public boolean enchantFitsInSlot(Enchant enchant, Character character, ItemSlot slot) {
        if (slot == ItemSlot.OffHand || slot == ItemSlot.Ranged) return false;
        return super.enchantFitsInSlot(enchant, character, slot);
    }

    @Override
    public boolean itemFitsInSlot(Item item, Character character, CharacterSlot slot, boolean ignoreUnique) {
        if (slot == CharacterSlot.OffHand && item.getSlot() == ItemSlot.OneHand) return false;
        return super.itemFitsInSlot(item, character, slot, ignoreUnique);
    }

    private static List<String> relevantGlyphs;

    public List<String> getRelevantGlyphs() {
        if (relevantGlyphs == null) {
            relevantGlyphs = new ArrayList<String>();
            relevantGlyphs.add("Glyph of Innervate");
            relevantGlyphs.add("Glyph of Insect Swarm");
            relevantGlyphs.add("Glyph of Moonfire");
            relevantGlyphs.add("Glyph of Starfire");
            relevantGlyphs.add("Glyph of Starfall");
            relevantGlyphs.add("Glyph of Focus");
        }
        return relevantGlyphs;
    }

    public static float BaseMana = 3496.0f;

    // C# CalculationsMoonkin.cs:481-495 SubPointNameColors (non-Silverlight branch).
    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> colors = new LinkedHashMap<String, String>();
        colors.put("Sustained Damage", "#0000FF");
        colors.put("Burst Damage", "#FF0000");
        return colors;
    }

    private String[] characterDisplayCalculationLabels = null;

    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        if (characterDisplayCalculationLabels == null) {
            characterDisplayCalculationLabels = new String[] {
                    "Basic Stats:Health",
                    "Basic Stats:Mana",
                    "Basic Stats:Agility",
                    "Basic Stats:Stamina",
                    "Basic Stats:Intellect",
                    "Basic Stats:Spirit",
                    "Basic Stats:Armor",
                    "Spell Stats:Spell Power",
                    "Spell Stats:Spell Hit",
                    "Spell Stats:Spell Crit",
                    "Spell Stats:Spell Haste",
                    "Regen:MP5 Not Casting",
                    "Regen:MP5 While Casting",
                    "Solution:Total Score",
                    "Solution:Selected Rotation",
                    "Solution:Selected DPS",
                    "Solution:Selected Time To OOM",
                    "Solution:Selected Cycle Length",
                    "Solution:Selected Spell Breakdown",
                    "Solution:Burst Rotation",
                    "Solution:Burst DPS",
                    "Solution:Burst Cycle Length",
                    "Solution:Burst Spell Breakdown",
                    "Spell Info:Starfire",
                    "Spell Info:Wrath",
                    "Spell Info:Moonfire",
                    "Spell Info:Insect Swarm",
                    "Spell Info:Starfall",
                    "Spell Info:Treants"
            };
        }
        return List.of(characterDisplayCalculationLabels);
    }

    // C# CalculationsMoonkin.cs:540-553 CustomChartNames.
    private String[] customChartNames = null;

    public String[] getCustomChartNames() {
        if (customChartNames == null) {
            customChartNames = new String[] {
                    "Talent DPS Comparison",
                    "Talent MP5 Comparison",
                    "Mana Gains",
                    "Damage per Cast Time"
            };
        }
        return customChartNames;
    }

    @Override
    public String getOverallRatingLabel() {
        return "Overall Points";
    }

    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.Druid;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationMoonkin();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsMoonkin();
    }

    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            CalculationOptionsMoonkin calcOpts = new CalculationOptionsMoonkin();
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

    public static CalculationsMoonkin instance;

    public static CalculationsMoonkin getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("Moonkin", CalculationsMoonkin.class);
    }

    public static void register() {
        Calculations.registerModel("Moonkin", CalculationsMoonkin.class);
    }

    public CalculationsMoonkin() {
        instance = this;
    }

    public static CharacterCalculationsMoonkin getInnerCharacterCalculations(Character character, Stats stats, Item additionalItem) {
        CharacterCalculationsMoonkin calcs = new CharacterCalculationsMoonkin();
        CalculationOptionsMoonkin calcOpts = character.getCalculationOptions() instanceof CalculationOptionsMoonkin
                ? (CalculationOptionsMoonkin) character.getCalculationOptions()
                : new CalculationOptionsMoonkin();
        calcs.basicStats = stats;

        calcs.spellCrit = 0.0185f + StatConversion.getSpellCritFromIntellect(stats.getIntellect()) + StatConversion.getSpellCritFromRating(stats.getCritRating()) + stats.getSpellCrit() + stats.getSpellCritOnTarget();
        calcs.spellHit = StatConversion.getSpellHitFromRating(stats.getHitRating()) + stats.getSpellHit();
        calcs.spellHaste = (1 + StatConversion.getSpellHasteFromRating(stats.getHasteRating())) * (1 + stats.getSpellHaste()) - 1;

        // All spells: Spell Power + (0.5 * Imp Moonkin) * Spirit
        float spellDamageFromSpiritPercent = 0.0f;
        if (character.getDruidTalents().getMoonkinForm() > 0 && character.activeBuffsContains("Moonkin Form")) {
            spellDamageFromSpiritPercent = 0.1f * character.getDruidTalents().getImprovedMoonkinForm();
        }
        // All spells: Damage +(0.04 * Lunar Guidance * Int)
        float spellDamageFromIntPercent = 0.04f * character.getDruidTalents().getLunarGuidance();
        // Fix for rounding error in converting partial points of int/spirit to spell power
        float spellPowerFromStats = (float) Math.floor(spellDamageFromIntPercent * stats.getIntellect()) +
                (float) Math.floor(spellDamageFromSpiritPercent * stats.getSpirit());
        calcs.spellPower = stats.getSpellPower() + spellPowerFromStats;

        calcs.latency = calcOpts.getLatency();

        calcs.fightLength = calcOpts.getFightLength();
        calcs.targetLevel = calcOpts.getTargetLevel();

        // 3.1 spirit regen
        float spiritRegen = StatConversion.getSpiritRegenSec(calcs.basicStats.getSpirit(), calcs.basicStats.getIntellect());
        calcs.manaRegen = spiritRegen + stats.getMp5() / 5f;
        calcs.manaRegen5SR = spiritRegen * stats.getSpellCombatManaRegeneration() + stats.getMp5() / 5f;

        return calcs;
    }

    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        if (referenceCalculation && significantChange) {
            // Fresh top-level character calculation: drop shared effect interpolation caches so a
            // previous character cannot leak into this one (SUSPECTED_BUGS #15), like Healadin
            // (DECISIONS #28/#39).
            rawr.base.effects.SpecialEffect.clearInterpolatorCaches();
        }
        Stats stats = getCharacterStats(character, additionalItem);
        CharacterCalculationsMoonkin calcs = CalculationsMoonkin.getInnerCharacterCalculations(character, stats, additionalItem);
        // Run the solver to do final calculations
        new MoonkinSolver().solve(character, calcs);

        return calcs;
    }

    public Stats getCharacterStats(Character character, Item additionalItem) {
        CalculationOptionsMoonkin calcOpts = character.getCalculationOptions() instanceof CalculationOptionsMoonkin
                ? (CalculationOptionsMoonkin) character.getCalculationOptions()
                : new CalculationOptionsMoonkin();
        // Start off with a slightly modified form of druid base character stats calculations
        Stats statsRace;
        if (character.getRace() == CharacterRace.NightElf) {
            statsRace = new Stats();
            statsRace.setHealth(7416f);
            statsRace.setMana(3496f);
            statsRace.setStamina(97f);
            statsRace.setAgility(87f);
            statsRace.setIntellect(143f);
            statsRace.setSpirit(157f);
        } else {
            statsRace = new Stats();
            statsRace.setHealth(7416f);
            statsRace.setMana(3496f);
            statsRace.setStamina(99f);
            statsRace.setAgility(78f);
            statsRace.setIntellect(139f);
            statsRace.setSpirit(161f);
        }

        // Get the gear/enchants/buffs stats loaded in
        Stats statsBaseGear = new Stats();
        accumulateItemStats(statsBaseGear, character, additionalItem);
        Stats statsBuffs = getBuffsStats(character.getActiveBuffs());

        Stats statsGearEnchantsBuffs = Stats.add(statsBaseGear, statsBuffs);

        // Talented bonus multipliers

        Stats statsHotW = new Stats();
        statsHotW.setBonusIntellectMultiplier(0.04f * character.getDruidTalents().getHeartOfTheWild());

        Stats statsSotF = new Stats();
        statsSotF.setBonusStaminaMultiplier(0.02f * character.getDruidTalents().getSurvivalOfTheFittest());
        statsSotF.setBonusIntellectMultiplier(0.02f * character.getDruidTalents().getSurvivalOfTheFittest());
        statsSotF.setBonusSpiritMultiplier(0.02f * character.getDruidTalents().getSurvivalOfTheFittest());
        statsSotF.setBonusAgilityMultiplier(0.02f * character.getDruidTalents().getSurvivalOfTheFittest());

        Stats statsImpMotW = new Stats();
        statsImpMotW.setBonusStaminaMultiplier(0.01f * character.getDruidTalents().getImprovedMarkOfTheWild());
        statsImpMotW.setBonusIntellectMultiplier(0.01f * character.getDruidTalents().getImprovedMarkOfTheWild());
        statsImpMotW.setBonusSpiritMultiplier(0.01f * character.getDruidTalents().getImprovedMarkOfTheWild());
        statsImpMotW.setBonusAgilityMultiplier(0.01f * character.getDruidTalents().getImprovedMarkOfTheWild());

        Stats statsLivingSpirit = new Stats();
        statsLivingSpirit.setBonusSpiritMultiplier(0.05f * character.getDruidTalents().getLivingSpirit());

        Stats statsFuror = new Stats();
        statsFuror.setBonusIntellectMultiplier(character.activeBuffsContains("Moonkin Form") ? 0.02f * character.getDruidTalents().getFuror() : 0.0f);

        Stats statsEarthAndMoon = new Stats();
        statsEarthAndMoon.setBonusSpellPowerMultiplier(0.02f * character.getDruidTalents().getEarthAndMoon());
        Stats statsMoonkinForm = new Stats();
        statsMoonkinForm.setBaseArmorMultiplier(character.activeBuffsContains("Moonkin Form") && character.getDruidTalents().getMoonkinForm() > 0 ? 3.7f : 0.0f);
        Stats statsMasterSS = new Stats();
        statsMasterSS.setBonusSpellPowerMultiplier((character.getDruidTalents().getMoonkinForm() > 0 && character.activeBuffsContains("Moonkin Form"))
                ? 0.02f * character.getDruidTalents().getMasterShapeshifter() : 0.0f);

        Stats statsTalents = Stats.add(Stats.add(Stats.add(Stats.add(Stats.add(Stats.add(Stats.add(statsHotW, statsImpMotW), statsLivingSpirit), statsSotF), statsFuror), statsEarthAndMoon), statsMoonkinForm), statsMasterSS);

        // Create the total stats object
        Stats statsTotal = Stats.add(Stats.add(statsGearEnchantsBuffs, statsRace), statsTalents);

        // Base stats: Intellect, Stamina, Spirit, Agility
        statsTotal.setStamina((float) Math.floor(statsRace.getStamina() * (1 + statsTotal.getBonusStaminaMultiplier())));
        statsTotal.setStamina(statsTotal.getStamina() + (float) Math.floor(statsGearEnchantsBuffs.getStamina() * (1 + statsTotal.getBonusStaminaMultiplier())));
        statsTotal.setIntellect((float) Math.floor(statsRace.getIntellect() * (1 + statsTotal.getBonusIntellectMultiplier())));
        statsTotal.setIntellect(statsTotal.getIntellect() + (float) Math.floor(statsGearEnchantsBuffs.getIntellect() * (1 + statsTotal.getBonusIntellectMultiplier())));
        statsTotal.setAgility((float) Math.floor(statsRace.getAgility() * (1 + statsTotal.getBonusAgilityMultiplier())));
        statsTotal.setAgility(statsTotal.getAgility() + (float) Math.floor(statsGearEnchantsBuffs.getAgility() * (1 + statsTotal.getBonusAgilityMultiplier())));
        statsTotal.setSpirit((float) Math.floor(statsRace.getSpirit() * (1 + statsTotal.getBonusSpiritMultiplier())));
        statsTotal.setSpirit(statsTotal.getSpirit() + (float) Math.floor(statsGearEnchantsBuffs.getSpirit() * (1 + statsTotal.getBonusSpiritMultiplier())));

        // Derived stats: Health, mana pool, armor
        statsTotal.setHealth((float) Math.rint(((statsRace.getHealth() * (character.getRace() == CharacterRace.Tauren ? 1.05f : 1f) + statsGearEnchantsBuffs.getHealth() + statsTotal.getStamina() * 10f))) - 180);
        statsTotal.setHealth((float) Math.floor(statsTotal.getHealth() * (1f + statsTotal.getBonusHealthMultiplier())));
        statsTotal.setMana((float) Math.rint(statsRace.getMana() + 15f * statsTotal.getIntellect()) - 280);
        // Armor
        statsTotal.setArmor(statsTotal.getArmor() * (1f + statsTotal.getBaseArmorMultiplier()));
        statsTotal.setBonusArmor(statsTotal.getBonusArmor() + statsTotal.getAgility() * 2f);
        statsTotal.setBonusArmor(statsTotal.getBonusArmor() * (1f + statsTotal.getBonusArmorMultiplier()));
        statsTotal.setArmor(statsTotal.getArmor() + statsTotal.getBonusArmor());
        statsTotal.setArmor((float) Math.rint(statsTotal.getArmor()));

        // Regen mechanic: mp5 +((0.1 * Intensity) * Spiritmp5())
        statsTotal.setSpellCombatManaRegeneration(statsTotal.getSpellCombatManaRegeneration() + round2(character.getDruidTalents().getIntensity() / 6.0f));
        // Regen mechanic: mp5 +(0.04/0.07/0.10) * Int)
        statsTotal.setMp5(statsTotal.getMp5() + (int) (statsTotal.getIntellect() * Math.ceil(character.getDruidTalents().getDreamstate() * 10 / 3.0f) / 100.0f));

        // Hit rating
        // All spells: Hit% +(0.02 * Balance of Power)
        statsTotal.setSpellHit(statsTotal.getSpellHit() + 0.02f * character.getDruidTalents().getBalanceOfPower());

        // Crit rating
        // Application order: Stats, Talents, Gear
        // All spells: Crit% + (0.01 * Natural Perfection)
        statsTotal.setSpellCrit(statsTotal.getSpellCrit() + 0.01f * character.getDruidTalents().getNaturalPerfection());
        // All spells: Haste% + (0.01 * Celestial Focus)
        statsTotal.setSpellHaste(statsTotal.getSpellHaste() + 0.01f * character.getDruidTalents().getCelestialFocus());
        // All spells: Crit% + (0.01 * Improved Faerie Fire)
        if (character.activeBuffsContains("Faerie Fire")) {
            statsTotal.setSpellCrit(statsTotal.getSpellCrit() + 0.01f * character.getDruidTalents().getImprovedFaerieFire());
        }

        return statsTotal;
    }

    /** C# Math.Round(value, 2) (banker's rounding to 2 decimals) for the Intensity regen term. */
    private static float round2(float value) {
        return new BigDecimal((double) value).setScale(2, RoundingMode.HALF_EVEN).floatValue();
    }

    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {

        switch (chartName) {
            case "Talent DPS Comparison": {
                CharacterCalculationsMoonkin calcsTalentBase = (CharacterCalculationsMoonkin) getCharacterCalculations(character);
                List<ComparisonCalculationBase> comps = new ArrayList<ComparisonCalculationBase>();
                for (int i = 0; i < character.getDruidTalents().getData().length; ++i) {
                    if (character.getDruidTalents().getData()[i] > 0) {
                        int oldValue = character.getDruidTalents().getData()[i];
                        character.getDruidTalents().getData()[i] = 0;
                        CharacterCalculationsMoonkin calcsTalented = (CharacterCalculationsMoonkin) getCharacterCalculations(character);
                        ComparisonCalculationMoonkin comp = new ComparisonCalculationMoonkin();
                        comp.setName(lookupDruidTalentName(i));
                        comp.setOverallPoints(calcsTalentBase.getOverallPoints() - calcsTalented.getOverallPoints());
                        comp.setDamagePoints(calcsTalentBase.getSubPoints()[0] - calcsTalented.getSubPoints()[0]);
                        comp.setRawDamagePoints(calcsTalentBase.getSubPoints()[1] - calcsTalented.getSubPoints()[1]);
                        comps.add(comp);

                        character.getDruidTalents().getData()[i] = oldValue;
                    }
                }
                comps.removeIf(val -> val.getOverallPoints() <= 0);
                return comps.toArray(new ComparisonCalculationBase[0]);
            }
            case "Talent MP5 Comparison": {
                CharacterCalculationsMoonkin calcsMP5TalentBase = (CharacterCalculationsMoonkin) getCharacterCalculations(character);
                List<ComparisonCalculationBase> compsMP5 = new ArrayList<ComparisonCalculationBase>();
                SpellRotation maxRot = calcsMP5TalentBase.selectedRotation;
                for (int i = 0; i < character.getDruidTalents().getData().length; ++i) {
                    if (character.getDruidTalents().getData()[i] > 0) {
                        int oldValue = character.getDruidTalents().getData()[i];
                        character.getDruidTalents().getData()[i] = 0;
                        CharacterCalculationsMoonkin calcsTalented = (CharacterCalculationsMoonkin) getCharacterCalculations(character);
                        float mp5 = 0.0f;
                        float manaGain = 0.0f;
                        for (java.util.Map.Entry<String, RotationData> pairs : calcsTalented.rotations.entrySet()) {
                            if (pairs.getKey().equals(maxRot.name.replace("Filler", "SF")) ||
                                    pairs.getKey().equals(maxRot.name.replace("Filler", "W"))) {
                                mp5 = (pairs.getValue().manaUsed - maxRot.manaUsed) / maxRot.duration * 5.0f;
                                manaGain = (maxRot.manaGained - pairs.getValue().manaGained) / maxRot.duration * 5.0f;
                            }
                        }
                        ComparisonCalculationMoonkin comp = new ComparisonCalculationMoonkin();
                        comp.setName(lookupDruidTalentName(i));
                        comp.setOverallPoints((mp5 >= 0 ? mp5 : 0.0f) + manaGain);
                        comp.setRawDamagePoints((mp5 >= 0 ? mp5 : 0.0f));
                        comp.setDamagePoints(manaGain);
                        compsMP5.add(comp);

                        character.getDruidTalents().getData()[i] = oldValue;
                    }
                }
                compsMP5.removeIf(val -> val.getOverallPoints() <= 0);
                return compsMP5.toArray(new ComparisonCalculationBase[0]);
            }
            case "Mana Gains": {
                CharacterCalculationsMoonkin calcsManaBase = (CharacterCalculationsMoonkin) getCharacterCalculations(character);
                SpellRotation manaGainsRot = calcsManaBase.selectedRotation;
                Character c2 = character.clone();

                List<ComparisonCalculationMoonkin> manaGainsList = new ArrayList<ComparisonCalculationMoonkin>();

                // Moonkin form
                c2.getDruidTalents().setMoonkinForm(0);
                CharacterCalculationsMoonkin calcsManaMoonkin = (CharacterCalculationsMoonkin) getCharacterCalculations(c2);
                c2.getDruidTalents().setMoonkinForm(1);
                for (java.util.Map.Entry<String, RotationData> pairs : calcsManaMoonkin.rotations.entrySet()) {
                    if (pairs.getKey().equals(manaGainsRot.name.replace("Filler", "SF"))) {
                        ComparisonCalculationMoonkin comp = new ComparisonCalculationMoonkin();
                        comp.setName("Moonkin Form");
                        comp.setOverallPoints((manaGainsRot.manaGained - pairs.getValue().manaGained) / manaGainsRot.duration * calcsManaBase.fightLength * 60.0f);
                        comp.setDamagePoints((manaGainsRot.manaGained - pairs.getValue().manaGained) / manaGainsRot.duration * calcsManaBase.fightLength * 60.0f);
                        comp.setRawDamagePoints(0);
                        manaGainsList.add(comp);
                    }
                }

                // JoW
                Buff jow = null;
                for (Buff b : c2.getActiveBuffs()) {
                    if (b.name.equals("Judgement of Wisdom")) {
                        jow = b;
                        break;
                    }
                }
                if (character.activeBuffsContains("Judgement of Wisdom")) {
                    c2.getActiveBuffs().remove(jow);
                }
                CharacterCalculationsMoonkin calcsManaJoW = (CharacterCalculationsMoonkin) getCharacterCalculations(c2);
                if (character.activeBuffsContains("Judgement of Wisdom")) {
                    c2.activeBuffsAdd(jow);
                }
                for (java.util.Map.Entry<String, RotationData> pairs : calcsManaJoW.rotations.entrySet()) {
                    if (pairs.getKey().equals(manaGainsRot.name.replace("Filler", "SF"))) {
                        ComparisonCalculationMoonkin comp = new ComparisonCalculationMoonkin();
                        comp.setName("Judgement of Wisdom");
                        comp.setOverallPoints((manaGainsRot.manaGained - pairs.getValue().manaGained) / manaGainsRot.duration * calcsManaBase.fightLength * 60.0f);
                        comp.setDamagePoints((manaGainsRot.manaGained - pairs.getValue().manaGained) / manaGainsRot.duration * calcsManaBase.fightLength * 60.0f);
                        comp.setRawDamagePoints(0);
                        manaGainsList.add(comp);
                    }
                }

                // Replenishment
                CalculationOptionsMoonkin calcOpts = c2.getCalculationOptions() instanceof CalculationOptionsMoonkin
                        ? (CalculationOptionsMoonkin) c2.getCalculationOptions()
                        : new CalculationOptionsMoonkin();
                float oldReplenishmentUptime = calcOpts.getReplenishmentUptime();
                calcOpts.setReplenishmentUptime(0.0f);
                CharacterCalculationsMoonkin calcsManaReplenishment = (CharacterCalculationsMoonkin) getCharacterCalculations(c2);
                calcOpts.setReplenishmentUptime(oldReplenishmentUptime);
                for (java.util.Map.Entry<String, RotationData> pairs : calcsManaReplenishment.rotations.entrySet()) {
                    if (pairs.getKey().equals(manaGainsRot.name.replace("Filler", "SF"))) {
                        ComparisonCalculationMoonkin comp = new ComparisonCalculationMoonkin();
                        comp.setName("Replenishment");
                        comp.setOverallPoints((manaGainsRot.manaGained - pairs.getValue().manaGained) / manaGainsRot.duration * calcsManaBase.fightLength * 60.0f);
                        comp.setDamagePoints((manaGainsRot.manaGained - pairs.getValue().manaGained) / manaGainsRot.duration * calcsManaBase.fightLength * 60.0f);
                        comp.setRawDamagePoints(0);
                        manaGainsList.add(comp);
                    }
                }

                // Innervate
                boolean innervate = calcOpts.getInnervate();
                calcOpts.setInnervate(false);
                CharacterCalculationsMoonkin calcsManaInnervate = (CharacterCalculationsMoonkin) getCharacterCalculations(c2);
                calcOpts.setInnervate(innervate);
                for (java.util.Map.Entry<String, RotationData> pairs : calcsManaInnervate.rotations.entrySet()) {
                    if (pairs.getKey().equals(manaGainsRot.name.replace("Filler", "SF"))) {
                        ComparisonCalculationMoonkin comp = new ComparisonCalculationMoonkin();
                        comp.setName("Innervate");
                        comp.setOverallPoints((manaGainsRot.manaGained - pairs.getValue().manaGained) / manaGainsRot.duration * calcsManaBase.fightLength * 60.0f);
                        comp.setDamagePoints((manaGainsRot.manaGained - pairs.getValue().manaGained) / manaGainsRot.duration * calcsManaBase.fightLength * 60.0f);
                        comp.setRawDamagePoints(0);
                        manaGainsList.add(comp);
                    }
                }

                // Mana spring
                Buff manaSpring = null;
                for (Buff b : c2.getActiveBuffs()) {
                    if (b.name.equals("Mana Spring Totem")) {
                        manaSpring = b;
                        break;
                    }
                }
                if (character.activeBuffsContains("Mana Spring Totem")) {
                    c2.getActiveBuffs().remove(manaSpring);
                }
                CharacterCalculationsMoonkin calcsManaManaSpring = (CharacterCalculationsMoonkin) getCharacterCalculations(c2);
                if (character.activeBuffsContains("Mana Spring Totem")) {
                    c2.activeBuffsAdd(manaSpring);
                }
                for (java.util.Map.Entry<String, RotationData> pairs : calcsManaManaSpring.rotations.entrySet()) {
                    if (pairs.getKey().equals(manaGainsRot.name.replace("Filler", "SF"))) {
                        ComparisonCalculationMoonkin comp = new ComparisonCalculationMoonkin();
                        comp.setName("Mana Spring Totem");
                        comp.setOverallPoints((manaGainsRot.manaGained - pairs.getValue().manaGained) / manaGainsRot.duration * calcsManaBase.fightLength * 60.0f);
                        comp.setDamagePoints((manaGainsRot.manaGained - pairs.getValue().manaGained) / manaGainsRot.duration * calcsManaBase.fightLength * 60.0f);
                        comp.setRawDamagePoints(0);
                        manaGainsList.add(comp);
                    }
                }

                // mp5
                ComparisonCalculationMoonkin mp5Comp = new ComparisonCalculationMoonkin();
                mp5Comp.setName("MP5");
                mp5Comp.setOverallPoints(calcsManaBase.fightLength * 60.0f * calcsManaBase.manaRegen5SR);
                mp5Comp.setDamagePoints(calcsManaBase.fightLength * 60.0f * calcsManaBase.manaRegen5SR);
                mp5Comp.setRawDamagePoints(0);
                manaGainsList.add(mp5Comp);

                return manaGainsList.toArray(new ComparisonCalculationBase[0]);
            }
            case "Damage per Cast Time": {
                CharacterCalculationsMoonkin calcsBase = (CharacterCalculationsMoonkin) getCharacterCalculations(character);
                ComparisonCalculationMoonkin sf = new ComparisonCalculationMoonkin();
                sf.setName("Starfire");
                ComparisonCalculationMoonkin mf = new ComparisonCalculationMoonkin();
                mf.setName("Moonfire");
                ComparisonCalculationMoonkin iSw = new ComparisonCalculationMoonkin();
                iSw.setName("Insect Swarm ");
                ComparisonCalculationMoonkin wr = new ComparisonCalculationMoonkin();
                wr.setName("Wrath");
                sf.setDamagePoints(calcsBase.selectedRotation.starfireAvgHit / calcsBase.selectedRotation.starfireAvgCast);
                sf.setRawDamagePoints(calcsBase.burstDPSRotation.starfireAvgHit / calcsBase.burstDPSRotation.starfireAvgCast);
                sf.setOverallPoints(sf.getDamagePoints() + sf.getRawDamagePoints());
                mf.setDamagePoints(calcsBase.selectedRotation.moonfireAvgHit / calcsBase.selectedRotation.solver.getMoonfire().castTime);
                mf.setRawDamagePoints(calcsBase.burstDPSRotation.moonfireAvgHit / calcsBase.burstDPSRotation.solver.getMoonfire().castTime);
                mf.setOverallPoints(mf.getDamagePoints() + mf.getRawDamagePoints());
                iSw.setDamagePoints(calcsBase.selectedRotation.insectSwarmAvgHit / calcsBase.selectedRotation.solver.getInsectSwarm().castTime);
                iSw.setRawDamagePoints(calcsBase.burstDPSRotation.insectSwarmAvgHit / calcsBase.burstDPSRotation.solver.getInsectSwarm().castTime);
                iSw.setOverallPoints(iSw.getDamagePoints() + iSw.getRawDamagePoints());
                wr.setDamagePoints(calcsBase.selectedRotation.wrathAvgHit / calcsBase.selectedRotation.wrathAvgCast);
                wr.setRawDamagePoints(calcsBase.burstDPSRotation.wrathAvgHit / calcsBase.burstDPSRotation.wrathAvgCast);
                wr.setOverallPoints(wr.getDamagePoints() + wr.getRawDamagePoints());
                return new ComparisonCalculationMoonkin[] { sf, mf, iSw, wr };
            }
        }
        return new ComparisonCalculationBase[0];
    }

    private String lookupDruidTalentName(int index) {
        switch (index) {
            case 0:
                return "Starlight Wrath";
            case 1:
                return "Genesis";
            case 2:
                return "Moonglow";
            case 3:
                return "Nature's Majesty";
            case 4:
                return "Improved Moonfire";
            case 5:
                return "Brambles";
            case 6:
                return "Nature's Grace";
            case 7:
                return "Nature's Splendor";
            case 9:
                return "Vengeance";
            case 10:
                return "Celestial Focus";
            case 11:
                return "Lunar Guidance";
            case 12:
                return "Insect Swarm";
            case 13:
                return "Improved Insect Swarm";
            case 14:
                return "Dreamstate";
            case 15:
                return "Moonfury";
            case 16:
                return "Balance of Power";
            case 17:
                return "Moonkin Form";
            case 18:
                return "Improved Moonkin Form";
            case 19:
                return "Improved Faerie Fire";
            case 21:
                return "Wrath of Cenarius";
            case 22:
                return "Eclipse";
            case 24:
                return "Force of Nature";
            case 26:
                return "Earth and Moon";
            case 27:
                return "Starfall";
            case 58:
                return "Improved Mark of the Wild";
            case 60:
                return "Furor";
            case 63:
                return "Natural Shapeshifter";
            case 64:
                return "Intensity";
            case 65:
                return "Omen of Clarity";
            case 66:
                return "Master Shapeshifter";
            default:
                return "Not implemented";
        }
    }

    // region Relevancy Methods

    /**
     * List of itemtypes that are relevant for moonkin
     */
    private List<ItemType> relevantItemTypes = null;

    @Override
    public List<ItemType> getRelevantItemTypes() {
        if (this.relevantItemTypes == null) {
            this.relevantItemTypes = new ArrayList<ItemType>(List.of(
                    ItemType.None,
                    ItemType.Cloth,
                    ItemType.Leather,
                    ItemType.Dagger,
                    ItemType.Staff,
                    ItemType.FistWeapon,
                    ItemType.OneHandMace,
                    ItemType.TwoHandMace,
                    ItemType.Idol));
        }
        return this.relevantItemTypes;
    }

    /**
     * List of SpecialEffect Triggers that are relevant for moonkin model
     * Every trigger listed here needs an implementation in ProcessSpecialEffects()
     * A trigger not listed here should not appear in ProcessSpecialEffects()
     */
    static List<Trigger> _RelevantTriggers = null;

    static List<Trigger> getRelevantTriggers() {
        if (_RelevantTriggers == null) {
            _RelevantTriggers = new ArrayList<Trigger>(List.of(
                    Trigger.Use,
                    Trigger.DamageSpellCast,
                    Trigger.DamageSpellCrit,        // Black magic enchant ?
                    Trigger.DamageSpellHit,
                    Trigger.SpellCast,
                    Trigger.SpellCrit,
                    Trigger.SpellHit,
                    Trigger.SpellMiss,
                    Trigger.DoTTick,
                    Trigger.DamageDone,
                    Trigger.DamageOrHealingDone,    // Darkmoon Card: Greatness
                    Trigger.InsectSwarmOrMoonfireTick,
                    Trigger.InsectSwarmTick,
                    Trigger.MoonfireTick,
                    Trigger.MoonfireCast));
        }
        return _RelevantTriggers;
    }

    @Override
    public boolean isItemRelevant(Item item) {
        // First we let normal rules (profession, class, relevant stats) decide
        boolean relevant = super.isItemRelevant(item);

        // Next we use our special stat relevancy filtering.
        if (relevant)
            relevant = hasPrimaryStats(item.getStats()) || (hasSecondaryStats(item.getStats()) && !hasUnwantedStats(item.getStats()));

        return relevant;
    }

    @Override
    public boolean isBuffRelevant(Buff buff, Character character) {
        // First we let normal rules (profession, class, relevant stats) decide
        boolean relevant = super.isBuffRelevant(buff, character);

        // Temporary FIX (?): buf.AllowedClasses is not currently being tested as part of base.IsBuffRelevant(). So we'll do it ourselves.
        if (relevant && !buff.getAllowedClasses().contains(CharacterClass.Druid))
            relevant = false;

        // Next we use our special stat relevancy filtering on consumables. (party buffs only need filtering on relevant stats)
        if (relevant && (buff.group.equals("Elixirs and Flasks") || buff.group.equals("Potion") || buff.group.equals("Food") || buff.group.equals("Scrolls") || buff.group.equals("Temporary Buffs")))
            relevant = hasPrimaryStats(buff.getTotalStats()) || (hasSecondaryStats(buff.getTotalStats()) && !hasUnwantedStats(buff.getTotalStats()));

        return relevant;
    }

    @Override
    public boolean isEnchantRelevant(Enchant enchant, Character character) {
        // First we let the normal rules (profession, class, relevant stats) decide
        boolean relevant = super.isEnchantRelevant(enchant, character);

        // Next we use our special stat relevancy filtering.
        if (relevant)
            relevant = hasPrimaryStats(enchant.stats) || (hasSecondaryStats(enchant.stats) && !hasUnwantedStats(enchant.stats));

        return relevant;
    }

    public Stats getRelevantStats(Stats stats) {
        Stats s = new Stats();
        // -- State Properties --
        // Base Stats
        s.setHealth(stats.getHealth());
        s.setMana(stats.getMana());
        s.setAgility(stats.getAgility());
        s.setStamina(stats.getStamina());
        s.setIntellect(stats.getIntellect());
        s.setSpirit(stats.getSpirit());
        s.setArmor(stats.getArmor());
        s.setHasteRating(stats.getHasteRating());
        s.setHitRating(stats.getHitRating());
        s.setCritRating(stats.getCritRating());
        s.setSpellPower(stats.getSpellPower());
        // s.setSpellPenetration(stats.getSpellPenetration());
        s.setMp5(stats.getMp5());
        s.setBonusArmor(stats.getBonusArmor());

        // Buffs / Debuffs
        s.setManaRestoreFromBaseManaPPM(stats.getManaRestoreFromBaseManaPPM());
        s.setManaRestoreFromMaxManaPerSecond(stats.getManaRestoreFromMaxManaPerSecond());

        // Combat Values
        s.setSpellCrit(stats.getSpellCrit());
        s.setSpellCritOnTarget(stats.getSpellCritOnTarget());
        s.setSpellHit(stats.getSpellHit());
        s.setSpellCombatManaRegeneration(stats.getSpellCombatManaRegeneration());

        // Spell Combat Ratings
        s.setSpellArcaneDamageRating(stats.getSpellArcaneDamageRating());
        s.setSpellNatureDamageRating(stats.getSpellNatureDamageRating());

        // Equipment Effects
        s.setManaRestore(stats.getManaRestore());
        s.setShadowDamage(stats.getShadowDamage());
        s.setNatureDamage(stats.getNatureDamage());
        s.setFireDamage(stats.getFireDamage());
        s.setValkyrDamage(stats.getValkyrDamage());

        // Moonkin
        s.setStarfireDmg(stats.getStarfireDmg());
        s.setUnseenMoonDamageBonus(stats.getUnseenMoonDamageBonus());
        s.setInsectSwarmDmg(stats.getInsectSwarmDmg());
        s.setMoonfireDmg(stats.getMoonfireDmg());
        s.setWrathDmg(stats.getWrathDmg());
        s.setInnervateCooldownReduction(stats.getInnervateCooldownReduction());
        s.setStarfireBonusWithDot(stats.getStarfireBonusWithDot());
        s.setMoonfireExtension(stats.getMoonfireExtension());
        s.setStarfireCritChance(stats.getStarfireCritChance());
        s.setBonusInsectSwarmDamage(stats.getBonusInsectSwarmDamage());
        s.setBonusNukeCritChance(stats.getBonusNukeCritChance());
        s.setEclipseBonus(stats.getEclipseBonus());
        s.setStarfireProc(stats.getStarfireProc());
        s.setMoonfireDotCrit(stats.getMoonfireDotCrit());
        s.setBonusMoonkinNukeDamage(stats.getBonusMoonkinNukeDamage());
        s.setMoonkinT10CritDot(stats.getMoonkinT10CritDot());

        // -- MultiplicativeStats --
        // Buffs / Debuffs
        s.setArmorPenetration(stats.getArmorPenetration());
        s.setBonusHealthMultiplier(stats.getBonusHealthMultiplier());
        s.setBonusManaMultiplier(stats.getBonusManaMultiplier());
        s.setBonusAgilityMultiplier(stats.getBonusAgilityMultiplier());
        s.setBonusStaminaMultiplier(stats.getBonusStaminaMultiplier());
        s.setBonusIntellectMultiplier(stats.getBonusIntellectMultiplier());
        s.setBonusSpiritMultiplier(stats.getBonusSpiritMultiplier());
        s.setBaseArmorMultiplier(stats.getBaseArmorMultiplier());
        s.setBonusArmorMultiplier(stats.getBonusArmorMultiplier());
        s.setSpellHaste(stats.getSpellHaste());
        s.setBonusCritMultiplier(stats.getBonusCritMultiplier());
        s.setBonusSpellCritMultiplier(stats.getBonusSpellCritMultiplier());
        s.setBonusSpellPowerMultiplier(stats.getBonusSpellPowerMultiplier());
        s.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());
        s.setBonusArcaneDamageMultiplier(stats.getBonusArcaneDamageMultiplier());
        s.setBonusNatureDamageMultiplier(stats.getBonusNatureDamageMultiplier());

        // -- NoStackStats
        s.setMovementSpeed(stats.getMovementSpeed());
        s.setBonusManaPotion(stats.getBonusManaPotion());
        s.setHighestStat(stats.getHighestStat());

        for (SpecialEffect effect : stats.specialEffects()) {
            if (getRelevantTriggers().contains(effect.getTrigger()) && hasRelevantStats(effect.getStats()))
                s.addSpecialEffect(effect);
        }
        return s;
    }

    @Override
    public boolean hasRelevantStats(Stats stats) {
        // These 3 calls should amount to the same list of stats as used in GetRelevantStats()
        return hasPrimaryStats(stats) || hasSecondaryStats(stats) || hasExtraStats(stats);
    }

    /**
     * HasPrimaryStats() should return true if the Stats object has any stats that define the item
     * as being 'for your class/spec'. For melee classes this is typical melee stats like Strength,
     * Agility, AP, Expertise... For casters it would be spellpower, intellect, ...
     */
    public boolean hasPrimaryStats(Stats stats) {
        boolean primaryStats =
                // -- State Properties --
                // Base Stats
                stats.getIntellect() > 0 ||
                        stats.getSpirit() > 0 ||
                        stats.getSpellPower() > 0 ||
                        // stats.getSpellPenetration() > 0 ||

                        // Combat Values
                        stats.getSpellCrit() > 0 ||
                        stats.getSpellCritOnTarget() > 0 ||
                        stats.getSpellHit() > 0 ||

                        // Spell Combat Ratings
                        stats.getSpellArcaneDamageRating() > 0 ||
                        stats.getSpellNatureDamageRating() > 0 ||

                        // Moonkin
                        stats.getStarfireDmg() > 0 ||
                        stats.getUnseenMoonDamageBonus() > 0 ||
                        stats.getInsectSwarmDmg() > 0 ||
                        stats.getMoonfireDmg() > 0 ||
                        stats.getWrathDmg() > 0 ||
                        stats.getInnervateCooldownReduction() > 0 ||
                        stats.getStarfireBonusWithDot() > 0 ||
                        stats.getMoonfireExtension() > 0 ||
                        stats.getStarfireCritChance() > 0 ||
                        stats.getBonusInsectSwarmDamage() > 0 ||
                        stats.getBonusNukeCritChance() > 0 ||
                        stats.getEclipseBonus() > 0 ||
                        stats.getStarfireProc() > 0 ||
                        stats.getMoonfireDotCrit() > 0 ||
                        stats.getBonusMoonkinNukeDamage() > 0 ||
                        stats.getMoonkinT10CritDot() > 0 ||

                        // -- MultiplicativeStats --
                        // Buffs / Debuffs
                        stats.getBonusIntellectMultiplier() > 0 ||
                        stats.getBonusSpiritMultiplier() > 0 ||
                        stats.getSpellHaste() > 0 ||
                        stats.getBonusSpellCritMultiplier() > 0 ||
                        stats.getBonusSpellPowerMultiplier() > 0 ||
                        stats.getBonusArcaneDamageMultiplier() > 0 ||
                        stats.getBonusNatureDamageMultiplier() > 0;

        if (!primaryStats) {
            for (SpecialEffect effect : stats.specialEffects()) {
                if (getRelevantTriggers().contains(effect.getTrigger()) && hasPrimaryStats(effect.getStats())) {
                    primaryStats = true;
                    break;
                }
            }
        }

        return primaryStats;
    }

    /**
     * HasSecondaryStats() should return true if the Stats object has any stats that are relevant for
     * the model but only to a smaller degree, so small that you wouldn't typically consider the item.
     */
    public boolean hasSecondaryStats(Stats stats) {
        boolean secondaryStats =
                // -- State Properties --
                // Base Stats
                stats.getMana() > 0 ||
                        stats.getHasteRating() > 0 ||
                        stats.getHitRating() > 0 ||
                        stats.getCritRating() > 0 ||
                        stats.getMp5() > 0 ||

                        // Buffs / Debuffs
                        stats.getManaRestoreFromBaseManaPPM() > 0 ||
                        stats.getManaRestoreFromMaxManaPerSecond() > 0 ||

                        // Combat Values
                        stats.getSpellCombatManaRegeneration() > 0 ||

                        // Equipment Effects
                        stats.getManaRestore() > 0 ||
                        stats.getShadowDamage() > 0 ||
                        stats.getNatureDamage() > 0 ||
                        stats.getFireDamage() > 0 ||
                        stats.getValkyrDamage() > 0 ||

                        // -- MultiplicativeStats --
                        // Buffs / Debuffs
                        stats.getArmorPenetration() > 0 ||       // benefits trees
                        stats.getBonusManaMultiplier() > 0 ||
                        stats.getBonusCritMultiplier() > 0 ||
                        stats.getBonusDamageMultiplier() > 0 ||

                        // -- NoStackStats
                        stats.getMovementSpeed() > 0 ||
                        stats.getBonusManaPotion() > 0 ||
                        stats.getHighestStat() > 0;

        if (!secondaryStats) {
            for (SpecialEffect effect : stats.specialEffects()) {
                if (getRelevantTriggers().contains(effect.getTrigger()) && hasSecondaryStats(effect.getStats())) {
                    secondaryStats = true;
                    break;
                }
            }
        }

        return secondaryStats;
    }

    /**
     * Return true if the Stats object has any stats that don't influence the model but that you do want
     * to display in tooltips and in calculated summary values.
     */
    public boolean hasExtraStats(Stats stats) {
        boolean extraStats =
                stats.getHealth() > 0 ||
                        stats.getAgility() > 0 ||
                        stats.getStamina() > 0 ||
                        stats.getArmor() > 0 ||
                        stats.getBonusArmor() > 0 ||
                        stats.getBonusHealthMultiplier() > 0 ||
                        stats.getBonusAgilityMultiplier() > 0 ||
                        stats.getBonusStaminaMultiplier() > 0 ||
                        stats.getBaseArmorMultiplier() > 0 ||
                        stats.getBonusArmorMultiplier() > 0;

        if (!extraStats) {
            for (SpecialEffect effect : stats.specialEffects()) {
                if (getRelevantTriggers().contains(effect.getTrigger()) && hasExtraStats(effect.getStats())) {
                    extraStats = true;
                    break;
                }
            }
        }

        return extraStats;
    }

    /**
     * Return true if the Stats object contains any stats that are making the item undesired.
     * Any item having only Secondary stats would be removed if it also has one of these.
     */
    public boolean hasUnwantedStats(Stats stats) {
        // List of stats that will filter out some buffs (Flasks, Elixirs & Scrolls), Enchants and Items.
        boolean unwantedStats =
                stats.getStrength() > 0 ||
                        stats.getAgility() > 0 ||
                        stats.getAttackPower() > 0 ||
                        stats.getArmorPenetrationRating() > 0 ||
                        stats.getExpertiseRating() > 0 ||
                        stats.getDefenseRating() > 0 ||
                        stats.getDodgeRating() > 0 ||
                        stats.getParryRating() > 0 ||
                        stats.getBlockRating() > 0 ||
                        stats.getResilience() > 0 ||
                        stats.getBlockValue() > 0;

        if (!unwantedStats) {
            for (SpecialEffect effect : stats.specialEffects()) {
                if (/*getRelevantTriggers().contains(effect.getTrigger()) && */hasUnwantedStats(effect.getStats())) {    // An unwanted stat could be behind a trigger we don't model.
                    unwantedStats = true;
                    break;
                }
            }
        }

        return unwantedStats;
    }
    // endregion
}
