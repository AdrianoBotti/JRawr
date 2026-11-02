// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Bear/CalculationsBear.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: UI-only members omitted (CalculationOptionsPanel and the RAWR3/WPF branches of the color
// and panel properties); the non-Silverlight display label/color/optimizable/chart/glyph tables are
// ported additively as getCharacterDisplayCalculationLabels()/getSubPointNameColors()/
// getOptimizableCalculationLabels()/getCustomChartNames()/getCustomChartData()/getRelevantGlyphs().
// FRAMEWORK ADAPTATION: DeserializeDataObject uses a JDK-XML reflective reader instead of
// XmlSerializer (same approach as CalculationsCat) plus the original CustomUseMaul nil repair.
package rawr.models.bear;

import rawr.base.stats.BaseStats;
import rawr.base.character.Buff;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterSlot;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.talents.DruidTalents;
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

public class CalculationsBear extends CalculationsBase {
    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        // Relevant Gem IDs for Ferals
        // Red
        int[] bold = { 39900, 39996, 40111, 42142 };
        int[] delicate = { 39905, 39997, 40112, 42143 };

        // Purple
        int[] shifting = { 39935, 40023, 40130 };
        int[] sovereign = { 39934, 40022, 40129 };

        // Blue
        int[] solid = { 39919, 40008, 40119, 36767 };

        // Green
        int[] enduring = { 39976, 40089, 40167 };

        // Yellow
        int[] thick = { 39916, 40015, 40126, 42157 };

        // Orange
        int[] etched = { 39948, 40038, 40143 };
        int[] fierce = { 39951, 40041, 40146 };
        int[] glinting = { 39953, 40044, 40148 };
        int[] stalwart = { 39964, 40056, 40160 };

        // Meta
        int austere = 41380;

        List<GemmingTemplate> list = new ArrayList<GemmingTemplate>();

        // Uncommon
        list.add(bearGemming("Uncommon", false, delicate[0], delicate[0], delicate[0], delicate[0], austere)); // Max Mitigation
        list.add(bearGemming("Uncommon", false, delicate[0], stalwart[0], shifting[0], delicate[0], austere)); // Mitigation Heavier
        list.add(bearGemming("Uncommon", false, delicate[0], glinting[0], shifting[0], delicate[0], austere)); // Mitigation Heavy
        list.add(bearGemming("Uncommon", false, shifting[0], enduring[0], solid[0], solid[0], austere)); // Survivbility Heavy
        list.add(bearGemming("Uncommon", false, solid[0], solid[0], solid[0], solid[0], austere)); // Max Survivbility

        // Rare
        list.add(bearGemming("Rare", false, delicate[1], delicate[1], delicate[1], delicate[1], austere)); // Max Mitigation
        list.add(bearGemming("Rare", false, delicate[1], stalwart[1], shifting[1], delicate[1], austere)); // Mitigation Heavier
        list.add(bearGemming("Rare", false, delicate[1], glinting[1], shifting[1], delicate[1], austere)); // Mitigation Heavy
        list.add(bearGemming("Rare", false, shifting[1], enduring[1], solid[1], solid[1], austere)); // Survivbility Heavy
        list.add(bearGemming("Rare", false, solid[1], solid[1], solid[1], solid[1], austere)); // Max Survivbility

        // Epic
        list.add(bearGemming("Epic", true, delicate[2], delicate[2], delicate[2], delicate[2], austere)); // Max Mitigation
        list.add(bearGemming("Epic", true, delicate[2], stalwart[2], shifting[2], delicate[2], austere)); // Mitigation Heavier
        list.add(bearGemming("Epic", true, delicate[2], glinting[2], shifting[2], delicate[2], austere)); // Mitigation Heavy
        list.add(bearGemming("Epic", true, shifting[2], enduring[2], solid[2], solid[2], austere)); // Survivbility Heavy
        list.add(bearGemming("Epic", true, solid[2], solid[2], solid[2], solid[2], austere)); // Max Survivbility

        // Jeweler
        list.add(bearGemming("Jeweler", false, delicate[3], delicate[3], delicate[3], delicate[3], austere)); // Max Mitigation
        list.add(bearGemming("Jeweler", false, delicate[2], delicate[3], delicate[3], delicate[2], austere)); // Mitigation Heavy
        list.add(bearGemming("Jeweler", false, solid[3], solid[3], solid[2], solid[2], austere)); // Survivbility Heavy
        list.add(bearGemming("Jeweler", false, solid[3], solid[3], solid[3], solid[3], austere)); // Max Survivbility

        return list;
    }

    private static GemmingTemplate bearGemming(String group, boolean enabled, int red, int yellow, int blue,
            int prismatic, int meta) {
        GemmingTemplate template = new GemmingTemplate();
        template.setModel("Bear");
        template.setGroup(group);
        template.setEnabled(enabled);
        template.setRedId(red);
        template.setYellowId(yellow);
        template.setBlueId(blue);
        template.setPrismaticId(prismatic);
        template.setMetaId(meta);
        return template;
    }

    // C# CalculationsBear.cs:156-232 CharacterDisplayCalculationLabels.
    public static final String[] CharacterDisplayCalculationLabels = new String[] {
            "Summary:Overall Points*Overall Points are a sum of Mitigation and Survival Points. \n"
                    + "Overall is typically, but not always, the best way to rate gear. \n"
                    + "For specific encounters, closer attention to Mitigation and \n"
                    + "Survival Points individually may be important.",
            "Summary:Mitigation Points*Mitigation Points represent the amount of damage you mitigate, \n"
                    + "on average, through armor mitigation and avoidance. It is directly \n"
                    + "relational to your Damage Taken. Ideally, you want to maximize \n"
                    + "Mitigation Points, while maintaining 'enough' Survival Points \n"
                    + "(see Survival Points). If you find yourself dying due to healers \n"
                    + "running OOM, or being too busy healing you and letting other \n"
                    + "raid members die, then focus on Mitigation Points.",
            "Summary:Survival Points*Survival Points represents the total raw physical damage \n"
                    + "(pre-mitigation) you can take before dying. Unlike \n"
                    + "Mitigation Points, you should not attempt to maximize this, \n"
                    + "but rather get 'enough' of it, and then focus on Mitigation. \n"
                    + "'Enough' can vary greatly by fight and by your healers, but \n"
                    + "keeping it roughly even with Mitigation Points is a good \n"
                    + "way to maintain 'enough' as you progress. If you find that \n"
                    + "you are being killed by burst damage, focus on Survival Points.",
            "Summary:Threat Points*The TPS of your Highest TPS Rotation, multiplied by\n"
                    + "the Threat Scale defined on the Options tab.",

            "Basic Stats:Armor",
            "Basic Stats:Agility",
            "Basic Stats:Stamina",
            "Basic Stats:Strength",
            "Basic Stats:Attack Power",
            "Basic Stats:Crit Rating",
            "Basic Stats:Hit Rating",
            "Basic Stats:Expertise Rating",
            "Basic Stats:Haste Rating",
            "Basic Stats:Armor Penetration Rating",
            "Basic Stats:Dodge Rating",
            "Basic Stats:Defense Rating",
            "Basic Stats:Resilience",
            "Basic Stats:Nature Resist",
            "Basic Stats:Fire Resist",
            "Basic Stats:Frost Resist",
            "Basic Stats:Shadow Resist",
            "Basic Stats:Arcane Resist",
            "Mitigation Stats:Dodge",
            "Mitigation Stats:Miss",
            "Mitigation Stats:Mitigation",
            "Mitigation Stats:Avoidance PreDR",
            "Mitigation Stats:Avoidance PostDR",
            "Mitigation Stats:Savage Defense",
            "Mitigation Stats:Total Mitigation",
            "Mitigation Stats:Damage Taken",
            "Survival Stats:Health",
            "Survival Stats:Chance to be Crit",
            "Survival Stats:Nature Survival",
            "Survival Stats:Fire Survival",
            "Survival Stats:Frost Survival",
            "Survival Stats:Shadow Survival",
            "Survival Stats:Arcane Survival",
            "Threat Stats:Highest DPS Rotation",
            "Threat Stats:Highest TPS Rotation",
            "Threat Stats:Swipe Rotation",
            "Threat Stats:Custom Rotation",
            "Threat Stats:Melee",
            "Threat Stats:Maul",
            "Threat Stats:Mangle",
            "Threat Stats:Swipe",
            "Threat Stats:Faerie Fire",
            "Threat Stats:Lacerate",
            "Threat Stats:Lacerate DoT Tick",
            "Threat Stats:Avoided Attacks",
    };

    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        return List.of(CharacterDisplayCalculationLabels);
    }

    // C# CalculationsBear.cs:234-271 OptimizableCalculationLabels.
    public static final String[] OptimizableCalculationLabels = new String[] {
            "Health",
            "Hit Rating",
            "Expertise Rating",
            "Haste Rating",
            "Avoided Attacks %",
            "Mitigation % from Armor",
            "Avoidance %",
            "% Chance to be Crit",
            "Nature Survival",
            "Fire Survival",
            "Frost Survival",
            "Shadow Survival",
            "Arcane Survival",
            "Nature Resist",
            "Fire Resist",
            "Frost Resist",
            "Shadow Resist",
            "Arcane Resist",
            "Highest DPS",
            "Highest TPS",
            "Swipe DPS",
            "Swipe TPS",
            "Custom Rotation DPS",
            "Custom Rotation TPS",
    };

    public String[] getOptimizableCalculationLabels() {
        return OptimizableCalculationLabels;
    }

    // C# CalculationsBear.cs:273-290 CustomChartNames.
    public String[] getCustomChartNames() {
        return new String[] {
                "Combat Table",
                "Rotation DPS",
                "Rotation TPS"
        };
    }

    // C# CalculationsBear.cs:311-330 SubPointNameColors (non-Silverlight branch).
    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> colors = new LinkedHashMap<String, String>();
        colors.put("Mitigation", "#FF0000");
        colors.put("Survival", "#0000FF");
        colors.put("Threat", "#008000");
        return colors;
    }

    // C# CalculationsBear.cs:332-354 RelevantItemTypes.
    @Override
    public List<ItemType> getRelevantItemTypes() {
        return List.of(
                ItemType.None,
                ItemType.Leather,
                ItemType.Idol,
                ItemType.Staff,
                ItemType.TwoHandMace,
                ItemType.Polearm);
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
        return new ComparisonCalculationBear();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsBear();
    }

    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        xml = xml.replace("<CustomUseMaul xsi:nil=\"true\" />", "<CustomUseMaul>false</CustomUseMaul>");
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            CalculationOptionsBear calcOpts = new CalculationOptionsBear();
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

    public static CalculationsBear instance;

    public static CalculationsBear getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("Bear", CalculationsBear.class);
    }

    public static void register() {
        Calculations.registerModel("Bear", CalculationsBear.class);
    }

    public CalculationsBear() {
        instance = this;
    }

    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        CalculationOptionsBear calcOpts = character.getCalculationOptions() instanceof CalculationOptionsBear
                ? (CalculationOptionsBear) character.getCalculationOptions()
                : new CalculationOptionsBear();
        int targetLevel = calcOpts.getTargetLevel();
        int characterLevel = character.getLevel();
        Stats stats = getCharacterStats(character, additionalItem);
        float levelDifference = (targetLevel - characterLevel) * 0.002f;
        float TargAttackSpeed = calcOpts.getTargetAttackSpeed() * (1f - stats.getBossAttackSpeedMultiplier());

        CharacterCalculationsBear calculatedStats = new CharacterCalculationsBear();
        calculatedStats.basicStats = stats;
        calculatedStats.targetLevel = targetLevel;

        float hasteBonus = StatConversion.getHasteFromRating(stats.getHasteRating(), CharacterClass.Druid);
        float attackSpeed = ((2.5f) / (1f + hasteBonus)) / (1f + stats.getPhysicalHaste());

        float hitBonus = StatConversion.getPhysicalHitFromRating(stats.getHitRating(), CharacterClass.Druid) + stats.getPhysicalHit();
        float expertiseBonus = StatConversion.getDodgeParryReducFromExpertise(StatConversion.getExpertiseFromRating(stats.getExpertiseRating(), CharacterClass.Druid) + stats.getExpertise(), CharacterClass.Druid);
        float chanceDodge = Math.max(0f, StatConversion.WHITE_DODGE_CHANCE_CAP[targetLevel - 80] - expertiseBonus);
        float chanceParry = Math.max(0f, StatConversion.WHITE_PARRY_CHANCE_CAP[targetLevel - 80] - expertiseBonus);
        float chanceMiss = Math.max(0f, StatConversion.WHITE_MISS_CHANCE_CAP[targetLevel - 80] - hitBonus);

        float chanceAvoided = chanceMiss + chanceDodge + chanceParry;

        float rawChanceCrit = StatConversion.getPhysicalCritFromRating(stats.getCritRating(), CharacterClass.Druid)
                + StatConversion.getPhysicalCritFromAgility(stats.getAgility(), CharacterClass.Druid)
                + stats.getPhysicalCrit()
                + StatConversion.NPC_LEVEL_CRIT_MOD[targetLevel - 80];
        float chanceCrit = rawChanceCrit * (1f - chanceAvoided);
        float chanceCritBleed = (character.getDruidTalents().getPrimalGore() > 0 ? rawChanceCrit : 0f);
        attackSpeed = ((2.5f) / (1f + hasteBonus)) / (1f + stats.getPhysicalHaste());

        float baseAgi = character.getRace() == rawr.base.character.CharacterRace.NightElf ? 87 : 77;

        // Calculate avoidance, considering diminishing returns
        float defSkill = (float) Math.floor(StatConversion.getDefenseFromRating(stats.getDefenseRating(), CharacterClass.Druid));
        float dodgeNonDR = stats.getDodge() - levelDifference + StatConversion.getDodgeFromAgility(baseAgi, CharacterClass.Druid);
        float missNonDR = stats.getMiss() - levelDifference;
        float dodgePreDR = StatConversion.getDodgeFromAgility(stats.getAgility() - baseAgi, CharacterClass.Druid)
                + StatConversion.getDodgeFromRating(stats.getDodgeRating(), CharacterClass.Druid)
                + defSkill * StatConversion.DEFENSE_RATING_AVOIDANCE_MULTIPLIER / 100f;
        float missPreDR = (defSkill * StatConversion.DEFENSE_RATING_AVOIDANCE_MULTIPLIER / 100f);
        float dodgePostDR = 0.01f / (1f / 116.890707f + 0.00972f / dodgePreDR);
        float missPostDR = 0.01f / (1f / 16f + 0.00972f / missPreDR);
        float dodgeTotal = dodgeNonDR + dodgePostDR;
        float missTotal = missNonDR + missPostDR;

        calculatedStats.miss = missTotal;
        calculatedStats.dodge = Math.min(1f - calculatedStats.miss, dodgeTotal);
        calculatedStats.constantDamageReduction = 1f - ((1f - Math.min(0.75f, stats.getArmor() / (stats.getArmor() - 22167.5f + 467.5f * targetLevel))) * (1f + stats.getDamageTakenMultiplier()));
        calculatedStats.avoidancePreDR = dodgeNonDR + dodgePreDR + missNonDR + missPreDR;
        calculatedStats.avoidancePostDR = dodgeTotal + missTotal;
        calculatedStats.critReduction = (defSkill * StatConversion.DEFENSE_RATING_AVOIDANCE_MULTIPLIER / 100f)
                + StatConversion.getCritReductionFromResilience(stats.getResilience(), CharacterClass.Druid)
                + stats.getCritChanceReduction();
        calculatedStats.cappedCritReduction = Math.min(0.05f + levelDifference, calculatedStats.critReduction);

        float targetHitChance = 1f - calculatedStats.avoidancePostDR;
        float autoSpecialAttacksPerSecond = 1f / 1.5f + 1f / attackSpeed;
        float lacerateTicksPerSecond = (character.getDruidTalents().getPrimalGore() > 0 ? 1f : 0f) / 3f;
        float totalAttacksPerSecond = autoSpecialAttacksPerSecond + lacerateTicksPerSecond;
        float averageSDAttackCritChance = (chanceCrit * (autoSpecialAttacksPerSecond / totalAttacksPerSecond) + chanceCritBleed * (lacerateTicksPerSecond / totalAttacksPerSecond));
        float playerAttackSpeed = 1f / totalAttacksPerSecond;
        float blockChance = 1f - targetHitChance * ((float) Math.pow(1f - averageSDAttackCritChance, TargAttackSpeed / playerAttackSpeed)) *
                1f / (1f - (1f - targetHitChance) * (float) Math.pow(1f - averageSDAttackCritChance, TargAttackSpeed / playerAttackSpeed));
        float blockValue = stats.getAttackPower() * 0.25f;
        float blockedPercent = Math.min(1f, (blockValue * blockChance) / ((1f - calculatedStats.constantDamageReduction) * calcOpts.getTargetDamage()));
        calculatedStats.savageDefenseChance = (float) roundHalfEven(blockChance, 5);
        calculatedStats.savageDefenseValue = (float) Math.floor(blockValue);
        calculatedStats.savageDefensePercent = (float) roundHalfEven(blockedPercent, 5);

        float parryHasteDamageMuliplier = 1.0f;
        if (calcOpts.getTargetParryHastes()) {
            float parryableAttacksPerSecond = 7f / 13.5f + 1f / attackSpeed; // In every 13.5sec period (9 GCDs), 2 should be FFF, so only 7 parryable instants per 13.5sec
            // Target parries within the first 40% of their swing timer advances their swing timer by 40%
            // ...Within the next 40%, it advances their swing timer to 20% remaining.
            // So, we average that as a 60% window (since the last 40% only provides half as much haste on average), with a 40% swing advance
            float parryWindow = TargAttackSpeed * 0.6f;
            float parryableAttacksPerParryWindow = parryableAttacksPerSecond * parryWindow;
            float chanceParryPerWindow = 1f - (float) Math.pow(1f - chanceParry, parryableAttacksPerParryWindow);
            float parryBonusDPSMultiplier = 1f / 0.6f - 1f; // When a parry happens, boss DPS gets muliplied by 1/0.6 for that swing
            parryHasteDamageMuliplier = 1f + (parryBonusDPSMultiplier * chanceParryPerWindow);
        }

        // Out of 100 attacks, you'll take...
        float crits = Math.min(Math.max(0f, 1f - calculatedStats.avoidancePostDR), (0.05f + levelDifference) - calculatedStats.cappedCritReduction);
        float hits = Math.max(0f, 1f - (crits + calculatedStats.avoidancePostDR));
        // Apply armor and multipliers for each attack type...
        crits *= (1f - calculatedStats.constantDamageReduction) * 2f;
        hits *= (1f - calculatedStats.constantDamageReduction);
        calculatedStats.damageTaken = (hits + crits) * (1f - blockedPercent) * parryHasteDamageMuliplier * (1f + stats.getBossAttackSpeedMultiplier());
        calculatedStats.totalMitigation = 1f - calculatedStats.damageTaken;

        calculatedStats.survivalPointsRaw = (stats.getHealth() / (1f - calculatedStats.constantDamageReduction));
        double survivalCap = (double) calcOpts.getSurvivalSoftCap() / 1000d;
        double survivalRaw = calculatedStats.survivalPointsRaw / 1000f;

        // Implement Survival Soft Cap
        if (survivalRaw <= survivalCap) {
            calculatedStats.setSurvivalPoints(1000f * (float) survivalRaw);
        } else {
            double x = survivalRaw;
            double cap = survivalCap;
            double fourToTheNegativeFourThirds = Math.pow(4d, -4d / 3d);
            double topLeft = Math.pow(((x - cap) / cap) + fourToTheNegativeFourThirds, 1d / 4d);
            double topRight = Math.pow(fourToTheNegativeFourThirds, 1d / 4d);
            double fracTop = topLeft - topRight;
            double fraction = fracTop / 2d;
            double y = (cap * fraction + cap);
            calculatedStats.setSurvivalPoints(1000f * (float) y);
        }

        calculatedStats.setMitigationPoints((17000f / calculatedStats.damageTaken)); // / (buffs.ShadowEmbrace ? 0.95f : 1f);

        // WotLKResistance
        // Call new resistance formula and apply talent damage reduction
        // As for other survival, only use guaranteed reduction (MinimumResist), no luck
        calculatedStats.natureSurvivalPoints = (float) (stats.getHealth() / ((1f - StatConversion.getMinimumResistance(targetLevel, character.getLevel(), stats.getNatureResistance(), 0)) * (1f - 0.04 * character.getDruidTalents().getProtectorOfThePack())));
        calculatedStats.frostSurvivalPoints = (float) (stats.getHealth() / ((1f - StatConversion.getMinimumResistance(targetLevel, character.getLevel(), stats.getFrostResistance(), 0)) * (1f - 0.04 * character.getDruidTalents().getProtectorOfThePack())));
        calculatedStats.fireSurvivalPoints = (float) (stats.getHealth() / ((1f - StatConversion.getMinimumResistance(targetLevel, character.getLevel(), stats.getFireResistance(), 0)) * (1f - 0.04 * character.getDruidTalents().getProtectorOfThePack())));
        calculatedStats.shadowSurvivalPoints = (float) (stats.getHealth() / ((1f - StatConversion.getMinimumResistance(targetLevel, character.getLevel(), stats.getShadowResistance(), 0)) * (1f - 0.04 * character.getDruidTalents().getProtectorOfThePack())));
        calculatedStats.arcaneSurvivalPoints = (float) (stats.getHealth() / ((1f - StatConversion.getMinimumResistance(targetLevel, character.getLevel(), stats.getArcaneResistance(), 0)) * (1f - 0.04 * character.getDruidTalents().getProtectorOfThePack())));

        // Perform Threat calculations
        calculateThreat(stats, targetLevel, calculatedStats, character);
        calculatedStats.setOverallPoints(calculatedStats.getMitigationPoints()
                + calculatedStats.getSurvivalPoints() + calculatedStats.getThreatPoints());
        return calculatedStats;
    }

    private static double roundHalfEven(double value, int digits) {
        if (!Double.isFinite(value)) return value;
        return new BigDecimal(value).setScale(digits, RoundingMode.HALF_EVEN).doubleValue();
    }

    /**
     * Calculates the threat properties of the Character
     */
    private void calculateThreat(Stats stats, int targetLevel, CharacterCalculationsBear calculatedStats, Character character) {
        CalculationOptionsBear calcOpts = character.getCalculationOptions() instanceof CalculationOptionsBear
                ? (CalculationOptionsBear) character.getCalculationOptions()
                : new CalculationOptionsBear();
        DruidTalents talents = character.getDruidTalents();

        // Establish base multipliers and chances
        int targetArmor = calcOpts.getTargetArmor();
        float modArmor = 1f - StatConversion.getArmorDamageReduction(character.getLevel(), targetArmor, stats.getArmorPenetration(), 0f, stats.getArmorPenetrationRating());

        float critMultiplier = 2f * (1 + stats.getBonusCritMultiplier());
        float spellCritMultiplier = 1.5f * (1 + stats.getBonusCritMultiplier());

        float hasteBonus = StatConversion.getPhysicalHasteFromRating(stats.getHasteRating(), CharacterClass.Druid);
        float attackSpeed = (2.5f) / (1f + hasteBonus);
        attackSpeed = attackSpeed / (1f + stats.getPhysicalHaste());

        float hitBonus = StatConversion.getPhysicalHitFromRating(stats.getHitRating(), CharacterClass.Druid) + stats.getPhysicalHit();
        float expertiseBonus = StatConversion.getDodgeParryReducFromExpertise(StatConversion.getExpertiseFromRating(stats.getExpertiseRating(), CharacterClass.Druid) + stats.getExpertise(), CharacterClass.Druid);

        float chanceDodge = Math.max(0f, StatConversion.WHITE_DODGE_CHANCE_CAP[targetLevel - 80] - expertiseBonus);
        float chanceParry = Math.max(0f, StatConversion.WHITE_PARRY_CHANCE_CAP[targetLevel - 80] - expertiseBonus);
        float chanceMiss = Math.max(0f, StatConversion.WHITE_MISS_CHANCE_CAP[targetLevel - 80] - hitBonus);
        float chanceGlance = StatConversion.WHITE_GLANCE_CHANCE_CAP[targetLevel - 80];
        float glanceMultiplier = 0.7f;
        float chanceAvoided = chanceMiss + chanceDodge + chanceParry;
        float chanceResisted = Math.max(0f, ((new float[] { 0.04f, 0.05f, 0.06f, 0.17f })[targetLevel - 80])
                - (StatConversion.getSpellHitFromRating(stats.getHitRating(), CharacterClass.Druid) + stats.getSpellHit()));

        float rawChanceCrit = StatConversion.getPhysicalCritFromRating(stats.getCritRating(), CharacterClass.Druid)
                + StatConversion.getPhysicalCritFromAgility(stats.getAgility(), CharacterClass.Druid)
                + stats.getPhysicalCrit()
                + StatConversion.NPC_LEVEL_CRIT_MOD[targetLevel - 80];
        float chanceCrit = rawChanceCrit * (1f - chanceAvoided);
        float chanceCritBleed = character.getDruidTalents().getPrimalGore() > 0 ? chanceCrit : 0f;
        float rawChanceCritSpell = StatConversion.getSpellCritFromRating(stats.getCritRating(), CharacterClass.Druid)
                + StatConversion.getSpellCritFromIntellect(stats.getIntellect(), CharacterClass.Druid)
                + stats.getSpellCrit() + stats.getSpellCritOnTarget()
                + StatConversion.NPC_LEVEL_CRIT_MOD[targetLevel - 80];
        float chanceCritSpell = rawChanceCritSpell * (1f - chanceResisted);

        calculatedStats.dodgedAttacks = chanceDodge;
        calculatedStats.parriedAttacks = chanceParry;
        calculatedStats.missedAttacks = chanceMiss;

        float baseDamage = 137f + (stats.getAttackPower() / 14f) * 2.5f;
        float bearThreatMultiplier = (29f / 14f) * (1f + stats.getThreatIncreaseMultiplier());

        // Calculate the raw damage of each ability
        float meleeDamageRaw = baseDamage * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * modArmor;
        float maulDamageRaw = (baseDamage + 578) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + stats.getBonusMaulDamageMultiplier()) * (1f + stats.getBonusBleedDamageMultiplier()) * modArmor;
        float mangleDamageRaw = (baseDamage * 1.15f + 299) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + stats.getBonusMangleDamageMultiplier()) * modArmor;
        float swipeDamageRaw = (stats.getAttackPower() * 0.063f + 108f) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + stats.getBonusSwipeDamageMultiplier()) * (1f + stats.getBonusBearSwipeDamageMultiplier()) * modArmor;
        float faerieFireDamageRaw = (stats.getAttackPower() * 0.15f + 1f) * (1f + stats.getBonusDamageMultiplier()) * (1f + stats.getBonusNatureDamageMultiplier());
        float lacerateDamageRaw = (stats.getAttackPower() * 0.01f + 88f) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * modArmor * (1f + stats.getBonusLacerateDamageMultiplier());
        float lacerateDamageDotRaw = (stats.getAttackPower() * 0.01f + 64f) * 5f /*stack size*/ * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + stats.getBonusBleedDamageMultiplier()) * (1f + stats.getBonusLacerateDamageMultiplier());

        // Calculate the average damage of each ability, including crits
        float meleeDamageAverage = (chanceCrit * (meleeDamageRaw * critMultiplier)) + // Damage from crits
                (chanceGlance * (meleeDamageRaw * glanceMultiplier)) + // Damage from glances
                ((1f - chanceCrit - chanceAvoided - chanceGlance) * (meleeDamageRaw)); // Damage from hits
        float maulDamageAverage = (chanceCrit * (maulDamageRaw * critMultiplier)) + ((1f - chanceCrit - chanceAvoided) * (maulDamageRaw));
        float mangleDamageAverage = (chanceCrit * (mangleDamageRaw * critMultiplier)) + ((1f - chanceCrit - chanceAvoided) * (mangleDamageRaw));
        float swipeDamageAverage = (chanceCrit * (swipeDamageRaw * critMultiplier)) + ((1f - chanceCrit - chanceAvoided) * (swipeDamageRaw));
        float faerieFireDamageAverage = (chanceCritSpell * (faerieFireDamageRaw * spellCritMultiplier)) + ((1f - chanceCritSpell - chanceResisted) * (faerieFireDamageRaw));
        float lacerateDamageAverage = (chanceCrit * (lacerateDamageRaw * critMultiplier)) + ((1f - chanceCrit - chanceAvoided) * (lacerateDamageRaw));
        float lacerateDamageDotAverage = (chanceCritBleed * (lacerateDamageDotRaw * critMultiplier)) + ((1f - chanceCritBleed) * (lacerateDamageDotRaw));

        // Calculate the raw threat of each attack
        float meleeThreatRaw = bearThreatMultiplier * meleeDamageRaw;
        float maulThreatRaw = bearThreatMultiplier * (maulDamageRaw + 424f / 1f); // NOTE! This assumes 1 target. If Maul hits 2 targets, replace 1 with 2.
        float mangleThreatRaw = bearThreatMultiplier * mangleDamageRaw * (1f + stats.getBonusMangleBearThreat());
        float swipeThreatRaw = bearThreatMultiplier * swipeDamageRaw * 1.5f;
        float faerieFireThreatRaw = bearThreatMultiplier * (faerieFireDamageRaw + 632f);
        float lacerateThreatRaw = bearThreatMultiplier * (lacerateDamageRaw + 1031f) / 2f;
        float lacerateDotThreatRaw = bearThreatMultiplier * lacerateDamageDotRaw / 2f;

        // Calculate the average threat of each attack, including crits
        float meleeThreatAverage = bearThreatMultiplier * meleeDamageAverage;
        float maulThreatAverage = bearThreatMultiplier * (maulDamageAverage + (424f * (1f - chanceAvoided)) / 1f); // NOTE! This assumes 1 target. If Maul hits 2 targets, replace 1 with 2.
        float mangleThreatAverage = bearThreatMultiplier * mangleDamageAverage * (1f + stats.getBonusMangleBearThreat());
        float swipeThreatAverage = bearThreatMultiplier * swipeDamageAverage * 1.5f;
        float faerieFireThreatAverage = bearThreatMultiplier * (faerieFireDamageAverage + (632f * (1f - chanceResisted)));
        float lacerateThreatAverage = bearThreatMultiplier * (lacerateDamageAverage + (1031f * (1f - chanceAvoided))) / 2f;
        float lacerateDotThreatAverage = bearThreatMultiplier * lacerateDamageDotAverage / 2f;

        // Calculate effective rage costs for the possible outcomes of each ability, and the average
        float meleeRageHit = meleeDamageRaw / 120f + 35f / 9f;
        float meleeRageGlance = (meleeDamageRaw * glanceMultiplier) / 120f + 35f / 9f;
        float meleeRageCrit = (meleeDamageRaw * critMultiplier) / 120f + 70f / 9f;
        float meleeRageDodgeParry = 9.5f;
        float meleeRageAverage = (chanceCrit * meleeRageCrit) + // Rage from crits
                (chanceGlance * meleeRageGlance) + // Rage from glances
                ((chanceDodge + chanceParry) * meleeRageDodgeParry) + // Rage from dodges/parries
                ((1f - chanceCrit - chanceAvoided - chanceGlance) * meleeRageHit); // Rage from hits

        float maulRageHit = 15f - talents.getFerocity() + meleeRageAverage;
        float maulRageCrit = 15f - talents.getFerocity() - (2.5f * talents.getPrimalFury()) + meleeRageAverage;
        float maulRageAvoided = ((15f - talents.getFerocity()) * 0.2f) + meleeRageAverage;
        float maulRageAverage = (chanceCrit * maulRageCrit) + ((1f - chanceCrit - chanceAvoided) * maulRageHit) + (chanceAvoided * maulRageAvoided);

        float mangleRageHit = 20f - talents.getFerocity();
        float mangleRageCrit = 20f - talents.getFerocity() - (2.5f * talents.getPrimalFury());
        float mangleRageDodgeParry = (20f - talents.getFerocity()) * 0.2f;
        float mangleRageAverage = (chanceCrit * mangleRageCrit) + ((1f - chanceCrit - chanceDodge - chanceParry) * mangleRageHit) + ((chanceDodge + chanceParry) + mangleRageDodgeParry);

        float swipeRageHit = 20f - talents.getFerocity();
        float swipeRageCrit = 20f - talents.getFerocity() - (2.5f * talents.getPrimalFury());
        float swipeRageDodgeParry = (20f - talents.getFerocity()) * 0.2f;
        float swipeRageAverage = (chanceCrit * swipeRageCrit) + ((1f - chanceCrit - chanceDodge - chanceParry) * swipeRageHit) + ((chanceDodge + chanceParry) + swipeRageDodgeParry);

        float lacerateRageHit = 15f - talents.getShreddingAttacks();
        float lacerateRageCrit = 15f - talents.getShreddingAttacks() - (2.5f * talents.getPrimalFury());
        float lacerateRageDodgeParry = (15f - talents.getShreddingAttacks()) * 0.2f;
        float lacerateRageAverage = (chanceCrit * lacerateRageCrit) + ((1f - chanceCrit - chanceDodge - chanceParry) * lacerateRageHit) + ((chanceDodge + chanceParry) + lacerateRageDodgeParry);

        // Calculate the threat per rage of each ability, on average
        float maulTPR = maulThreatAverage / maulRageAverage;
        float maulDPR = maulDamageAverage / maulRageAverage;
        float mangleTPR = mangleThreatAverage / mangleRageAverage;
        float mangleDPR = mangleDamageAverage / mangleRageAverage;
        float swipeTPR = swipeThreatAverage / swipeRageAverage;
        float swipeDPR = swipeDamageAverage / swipeRageAverage;
        float lacerateTPR = lacerateThreatAverage / lacerateRageAverage;
        float lacerateDPR = lacerateDamageAverage / lacerateRageAverage;

        // Use the BearRotationCalculator to model the potential rotations, and get their results
        BearRotationCalculator rotationCalculator = new BearRotationCalculator(meleeDamageAverage, maulDamageAverage, mangleDamageAverage, swipeDamageAverage,
                faerieFireDamageAverage, lacerateDamageAverage, lacerateDamageDotAverage, meleeThreatAverage, maulThreatAverage, mangleThreatAverage, swipeThreatAverage,
                faerieFireThreatAverage, lacerateThreatAverage, lacerateDotThreatAverage, 6f - stats.getMangleCooldownReduction(), attackSpeed);

        BearRotationCalculator.BearRotationCalculation rotationCalculationDPS, rotationCalculationTPS;
        rotationCalculationDPS = rotationCalculationTPS = new BearRotationCalculator.BearRotationCalculation();

        // Loop through the potential rotations...
        for (int useMaul = 0; useMaul < 2; useMaul++)
            for (int useMangle = 0; useMangle < 2; useMangle++)
                for (int useSwipe = 0; useSwipe < 2; useSwipe++)
                    for (int useFaerieFire = 0; useFaerieFire < 2; useFaerieFire++)
                        for (int useLacerate = 0; useLacerate < 2; useLacerate++) {
                            // ...and feed them to the BearRotationCalculator
                            BearRotationCalculator.BearRotationCalculation rotationCalculation =
                                    rotationCalculator.getRotationCalculations(useMaul == 1,
                                            useMangle == 1, useSwipe == 1, useFaerieFire == 1, useLacerate == 1);
                            // Keep track of the best rotation for both DPS and TPS
                            if (rotationCalculation.dps > rotationCalculationDPS.dps)
                                rotationCalculationDPS = rotationCalculation;
                            if (rotationCalculation.tps > rotationCalculationTPS.tps)
                                rotationCalculationTPS = rotationCalculation;
                        }

        float abomDPS = (stats.getMoteOfAnger() * meleeDamageAverage);
        float abomTPS = (stats.getMoteOfAnger() * meleeThreatAverage);

        // Fill the results into the CharacterCalculationsBear object
        calculatedStats.setThreatPoints((rotationCalculationTPS.tps + abomTPS) * calcOpts.getThreatScale());
        calculatedStats.highestDPSRotation = rotationCalculationDPS;
        calculatedStats.highestTPSRotation = rotationCalculationTPS;
        calculatedStats.swipeRotation = rotationCalculator.getRotationCalculations(null, false, true, false, false);
        calculatedStats.customRotation = rotationCalculator.getRotationCalculations(calcOpts.getCustomUseMaul(),
                calcOpts.getCustomUseMangle(), calcOpts.getCustomUseSwipe(), calcOpts.getCustomUseFaerieFire(), calcOpts.getCustomUseLacerate());

        calculatedStats.attackSpeed = attackSpeed;
        calculatedStats.mangleCooldown = 6f - stats.getMangleCooldownReduction();

        calculatedStats.meleeDamageRaw = (float) Math.rint(meleeDamageRaw);
        calculatedStats.meleeDamageAverage = (float) Math.rint(meleeDamageAverage);
        calculatedStats.meleeThreatRaw = (float) Math.rint(meleeThreatRaw);
        calculatedStats.meleeThreatAverage = (float) Math.rint(meleeThreatAverage);

        calculatedStats.maulDamageRaw = (float) Math.rint(maulDamageRaw);
        calculatedStats.maulDamageAverage = (float) Math.rint(maulDamageAverage);
        calculatedStats.maulThreatRaw = (float) Math.rint(maulThreatRaw);
        calculatedStats.maulThreatAverage = (float) Math.rint(maulThreatAverage);

        calculatedStats.mangleDamageRaw = (float) Math.rint(mangleDamageRaw);
        calculatedStats.mangleDamageAverage = (float) Math.rint(mangleDamageAverage);
        calculatedStats.mangleThreatRaw = (float) Math.rint(mangleThreatRaw);
        calculatedStats.mangleThreatAverage = (float) Math.rint(mangleThreatAverage);

        calculatedStats.swipeDamageRaw = (float) Math.rint(swipeDamageRaw);
        calculatedStats.swipeDamageAverage = (float) Math.rint(swipeDamageAverage);
        calculatedStats.swipeThreatRaw = (float) Math.rint(swipeThreatRaw);
        calculatedStats.swipeThreatAverage = (float) Math.rint(swipeThreatAverage);

        calculatedStats.faerieFireDamageRaw = (float) Math.rint(faerieFireDamageRaw);
        calculatedStats.faerieFireDamageAverage = (float) Math.rint(faerieFireDamageAverage);
        calculatedStats.faerieFireThreatRaw = (float) Math.rint(faerieFireThreatRaw);
        calculatedStats.faerieFireThreatAverage = (float) Math.rint(faerieFireThreatAverage);

        calculatedStats.lacerateDamageRaw = (float) Math.rint(lacerateDamageRaw);
        calculatedStats.lacerateDamageAverage = (float) Math.rint(lacerateDamageAverage);
        calculatedStats.lacerateThreatRaw = (float) Math.rint(lacerateThreatRaw);
        calculatedStats.lacerateThreatAverage = (float) Math.rint(lacerateThreatAverage);

        calculatedStats.lacerateDotDamageRaw = (float) Math.rint(lacerateDamageDotRaw);
        calculatedStats.lacerateDotDamageAverage = (float) Math.rint(lacerateDamageDotAverage);
        calculatedStats.lacerateDotThreatRaw = (float) Math.rint(lacerateDotThreatRaw);
        calculatedStats.lacerateDotThreatAverage = (float) Math.rint(lacerateDotThreatAverage);

        calculatedStats.maulTPR = maulTPR;
        calculatedStats.maulDPR = maulDPR;
        calculatedStats.mangleTPR = mangleTPR;
        calculatedStats.mangleDPR = mangleDPR;
        calculatedStats.swipeTPR = swipeTPR;
        calculatedStats.swipeDPR = swipeDPR;
        calculatedStats.lacerateTPR = lacerateTPR;
        calculatedStats.lacerateDPR = lacerateDPR;
    }

    /**
     * Gets the total Stats of the Character
     */
    public Stats getCharacterStats(Character character, Item additionalItem) {
        CalculationOptionsBear calcOpts = character.getCalculationOptions() instanceof CalculationOptionsBear
                ? (CalculationOptionsBear) character.getCalculationOptions()
                : new CalculationOptionsBear();

        DruidTalents talents = character.getDruidTalents();

        Stats statsTotal = new Stats();
        statsTotal.setBonusMangleDamageMultiplier((1f + 0.1f * talents.getSavageFury()) * (talents.isGlyphOfMangle() ? 1.1f : 1.0f) - 1f);
        statsTotal.setBonusMaulDamageMultiplier((1f + 0.1f * talents.getSavageFury()) * (1f + 0.04f * talents.getRendAndTear()) - 1f);
        statsTotal.setBonusEnrageDamageMultiplier(0.05f * talents.getKingOfTheJungle());
        statsTotal.setMangleCooldownReduction((0.5f * talents.getImprovedMangle()));
        statsTotal.setBonusSwipeDamageMultiplier(0.1f * talents.getFeralInstinct());

        statsTotal.setPhysicalCrit(0.02f * talents.getSharpenedClaws()
                + ((character.activeBuffsContains("Leader of the Pack")
                        || character.activeBuffsContains("Rampage"))
                        ? 0f : 0.05f * talents.getLeaderOfThePack()));
        statsTotal.setDodge(0.02f * talents.getFeralSwiftness() + 0.02f * talents.getNaturalReaction());
        statsTotal.setBonusStaminaMultiplier((1f + 0.02f * talents.getHeartOfTheWild()) * (1f + 0.02f * talents.getSurvivalOfTheFittest()) * (1f + 0.01f * talents.getImprovedMarkOfTheWild()) - 1f);
        statsTotal.setBonusAgilityMultiplier((1f + 0.02f * talents.getSurvivalOfTheFittest()) * (1f + 0.01f * talents.getImprovedMarkOfTheWild()) - 1f);
        statsTotal.setBonusStrengthMultiplier((1f + 0.02f * talents.getSurvivalOfTheFittest()) * (1f + 0.01f * talents.getImprovedMarkOfTheWild()) - 1f);
        statsTotal.setCritChanceReduction(0.02f * talents.getSurvivalOfTheFittest());
        statsTotal.setBonusAttackPowerMultiplier(0.02f * talents.getProtectorOfThePack());
        statsTotal.setBonusPhysicalDamageMultiplier((1f + 0.02f * talents.getNaturalist()) * (1f + 0.02f * talents.getMasterShapeshifter()) - 1f);
        statsTotal.setBonusRageOnCrit((2.5f * talents.getPrimalFury()));
        statsTotal.setExpertise(5f * talents.getPrimalPrecision());
        statsTotal.setAttackPower((character.getLevel() / 2f) * talents.getPredatoryStrikes());
        statsTotal.setDamageTakenMultiplier(-0.04f * talents.getProtectorOfThePack());
        statsTotal.setBonusBleedDamageMultiplier((character.activeBuffsContains("Mangle")
                || character.activeBuffsContains("Trauma") ? 0f : 0.3f * talents.getMangle()));
        statsTotal.setBaseArmorMultiplier(4.7f * (1f + 0.1f * talents.getThickHide() / 3f) * (1f + 0.11f * talents.getSurvivalOfTheFittest()) - 1f);

        statsTotal.accumulate(BaseStats.getBaseStats(80, character.getCharacterClass(), character.getRace(), BaseStats.DruidForm.Bear));
        Stats statsItems = new Stats();
        accumulateItemStats(statsItems, character, additionalItem);
        statsTotal.accumulate(statsItems);
        statsTotal.accumulate(getBuffsStats(character, calcOpts));

        float predatoryStrikesAP = 0f;
        float fap = 0f;
        if (character.get(CharacterSlot.MainHand) != null) {
            fap = Math.max(0f, (character.get(CharacterSlot.MainHand).getItem().getDPS() - 54.8f) * 14f); // TODO Find a more accurate number for this?
            predatoryStrikesAP = (fap + character.get(CharacterSlot.MainHand).getItem().getStats().getAttackPower()) * 0.2f * (talents.getPredatoryStrikes() / 3f);
            if (character.get(CharacterSlot.MainHand).getEnchant() != null) {
                predatoryStrikesAP += character.get(CharacterSlot.MainHand).getEnchant().stats.getAttackPower() * 0.2f * (talents.getPredatoryStrikes() / 3f);
            }
        }

        statsTotal.setStamina((float) Math.floor(statsTotal.getStamina() * (1f + statsTotal.getBonusStaminaMultiplier())));
        statsTotal.setStrength((float) Math.floor(statsTotal.getStrength() * (1f + statsTotal.getBonusStrengthMultiplier())));
        statsTotal.setAgility((float) Math.floor(statsTotal.getAgility() * (1f + statsTotal.getBonusAgilityMultiplier())));
        statsTotal.setAttackPower(statsTotal.getAttackPower() + (float) Math.floor(statsTotal.getStrength()) * 2f + fap + predatoryStrikesAP);
        statsTotal.setAttackPower((float) Math.floor(statsTotal.getAttackPower() * (1f + statsTotal.getBonusAttackPowerMultiplier())));
        statsTotal.setHealth(statsTotal.getHealth() + ((statsTotal.getStamina() - 20f) * 10f) + 20f);
        statsTotal.setHealth(statsTotal.getHealth() * (1f + statsTotal.getBonusHealthMultiplier()));
        statsTotal.setArmor(statsTotal.getArmor() * (1f + statsTotal.getBaseArmorMultiplier()));
        statsTotal.setArmor(statsTotal.getArmor() + 2f * (float) Math.floor(statsTotal.getAgility()) + statsTotal.getBonusArmor());
        statsTotal.setArmor((float) Math.floor(statsTotal.getArmor() * (1f + statsTotal.getBonusArmorMultiplier())));
        statsTotal.setNatureResistance(statsTotal.getNatureResistance() + statsTotal.getNatureResistanceBuff());
        statsTotal.setFireResistance(statsTotal.getFireResistance() + statsTotal.getFireResistanceBuff());
        statsTotal.setFrostResistance(statsTotal.getFrostResistance() + statsTotal.getFrostResistanceBuff());
        statsTotal.setShadowResistance(statsTotal.getShadowResistance() + statsTotal.getShadowResistanceBuff());
        statsTotal.setArcaneResistance(statsTotal.getArcaneResistance() + statsTotal.getArcaneResistanceBuff());

        int targetLevel = calcOpts.getTargetLevel();

        float hasteBonus = StatConversion.getHasteFromRating(statsTotal.getHasteRating(), CharacterClass.Druid);
        float attackSpeed = (2.5f / (1f + hasteBonus)) / (1f + statsTotal.getPhysicalHaste());
        float meleeHitInterval = 1f / (1f / attackSpeed + 1f / 1.5f);

        float hitBonus = StatConversion.getPhysicalHitFromRating(statsTotal.getHitRating()) + statsTotal.getPhysicalHit();
        float expertiseBonus = StatConversion.getDodgeParryReducFromExpertise(StatConversion.getExpertiseFromRating(statsTotal.getExpertiseRating(), CharacterClass.Druid) + statsTotal.getExpertise(), CharacterClass.Druid);
        float chanceDodge = Math.max(0f, StatConversion.WHITE_DODGE_CHANCE_CAP[targetLevel - 80] - expertiseBonus);
        float chanceParry = Math.max(0f, StatConversion.WHITE_PARRY_CHANCE_CAP[targetLevel - 80] - expertiseBonus);
        float chanceMiss = Math.max(0f, StatConversion.WHITE_MISS_CHANCE_CAP[targetLevel - 80] - hitBonus);
        float chanceAvoided = chanceMiss + chanceDodge + chanceParry;

        float rawChanceCrit = StatConversion.getPhysicalCritFromRating(statsTotal.getCritRating(), CharacterClass.Druid)
                + StatConversion.getPhysicalCritFromAgility(statsTotal.getAgility(), CharacterClass.Druid)
                + statsTotal.getPhysicalCrit()
                + StatConversion.NPC_LEVEL_CRIT_MOD[targetLevel - 80];
        float chanceCrit = rawChanceCrit * (1f - chanceAvoided);
        float chanceHit = 1f - chanceAvoided;

        float levelDifference = (targetLevel - character.getLevel()) * 0.002f;
        float baseAgi = character.getRace() == rawr.base.character.CharacterRace.NightElf ? 87 : 77;

        float defSkill = (float) Math.floor(StatConversion.getDefenseFromRating(statsTotal.getDefenseRating(), CharacterClass.Druid));
        float dodgeNonDR = statsTotal.getDodge() - levelDifference + StatConversion.getDodgeFromAgility(baseAgi, CharacterClass.Druid);
        float missNonDR = statsTotal.getMiss() - levelDifference;
        float dodgePreDR = StatConversion.getDodgeFromAgility(statsTotal.getAgility() - baseAgi, CharacterClass.Druid)
                + StatConversion.getDodgeFromRating(statsTotal.getDodgeRating(), CharacterClass.Druid)
                + defSkill * StatConversion.DEFENSE_RATING_AVOIDANCE_MULTIPLIER / 100f;
        float missPreDR = (defSkill * StatConversion.DEFENSE_RATING_AVOIDANCE_MULTIPLIER / 100f);
        float dodgePostDR = 0.01f / (1f / 116.890707f + 0.00972f / dodgePreDR);
        float missPostDR = 0.01f / (1f / 16f + 0.00972f / missPreDR);
        float dodgeTotal = dodgeNonDR + dodgePostDR;
        float missTotal = missNonDR + missPostDR;

        float TargAttackSpeed = calcOpts.getTargetAttackSpeed() * (1f - statsTotal.getBossAttackSpeedMultiplier());

        Stats statsProcs = new Stats();
        for (SpecialEffect effect : statsTotal.specialEffects()) {
            switch (effect.getTrigger()) {
                case Use:
                    effect.accumulateAverageStats(statsProcs, 0f, 1f, 2.5f, 0f);
                    break;
                case MeleeHit:
                case PhysicalHit:
                    effect.accumulateAverageStats(statsProcs, meleeHitInterval, chanceHit, 2.5f, 0f);
                    break;
                case MeleeAttack:
                    if (effect.getStats().getMoteOfAnger() > 0) {
                        // When in effect stats, MoteOfAnger is % of melee hits
                        // When in character stats, MoteOfAnger is average procs per second
                        statsProcs.setMoteOfAnger(effect.getStats().getMoteOfAnger() * effect.getAverageProcsPerSecond(meleeHitInterval, 1f, 2.5f, 300f) / effect.getMaxStack());
                    } else {
                        effect.accumulateAverageStats(statsProcs, meleeHitInterval, 1f, 2.5f, 0f);
                    }
                    break;
                case MeleeCrit:
                case PhysicalCrit:
                    effect.accumulateAverageStats(statsProcs, meleeHitInterval, chanceCrit, 2.5f, 0f);
                    break;
                case DoTTick:
                    effect.accumulateAverageStats(statsProcs, 3f, 1f, 2.5f, 0f);
                    break;
                case DamageDone:
                    effect.accumulateAverageStats(statsProcs, meleeHitInterval / 2f, 1f, 2.5f, 0f);
                    break;
                case DamageOrHealingDone:
                    effect.accumulateAverageStats(statsProcs, meleeHitInterval / 2f, 1f, 2.5f, 0f); // also needs healing
                    break;
                case MangleBearHit:
                    if (talents.getMangle() > 0)
                        effect.accumulateAverageStats(statsProcs, 6f - statsTotal.getMangleCooldownReduction(), chanceHit, 2.5f, 0f);
                    break;
                case MangleCatOrShredOrInfectedWoundsHit:
                    if (talents.getMangle() > 0)
                        effect.accumulateAverageStats(statsProcs, 1f
                                / (1f / (6f - statsTotal.getMangleCooldownReduction()) + // Mangles Per Second
                                1f / meleeHitInterval) // Mauls Per Second
                                , chanceHit, 2.5f, 0f);
                    break;
                case SwipeBearOrLacerateHit:
                    effect.accumulateAverageStats(statsProcs, 2.25f, chanceHit, 2.5f, 0f);
                    break;
                case LacerateTick:
                    effect.accumulateAverageStats(statsProcs, 3f, 1f, 2.5f, 0f);
                    break;
                case DamageTaken:
                    effect.accumulateAverageStats(statsProcs, TargAttackSpeed * 0.8f, 1f - 0.8f * (dodgeTotal + missTotal), 3f, 0f); // Assume you get hit by other things, like dots, aoes, etc, making you get targeted with damage 25% more often than the boss, and half the hits you take are unavoidable.
                    break;
                case DamageTakenPhysical:
                    effect.accumulateAverageStats(statsProcs, TargAttackSpeed, 1f - (dodgeTotal + missTotal), 3f, 0f); // Assume you get hit by other things, like dots, aoes, etc, making you get targeted with damage 25% more often than the boss, and half the hits you take are unavoidable.
                    break;
            }
        }

        statsProcs.setAgility(statsProcs.getAgility() + statsProcs.getHighestStat() + statsProcs.getParagon());
        statsProcs.setStamina((float) Math.floor(statsProcs.getStamina() * (1f + statsTotal.getBonusStaminaMultiplier())));
        statsProcs.setStrength((float) Math.floor(statsProcs.getStrength() * (1f + statsTotal.getBonusStrengthMultiplier())));
        statsProcs.setAgility((float) Math.floor(statsProcs.getAgility() * (1f + statsTotal.getBonusAgilityMultiplier())));
        statsProcs.setAttackPower(statsProcs.getAttackPower() + statsProcs.getStrength() * 2f);
        statsProcs.setAttackPower((float) Math.floor(statsProcs.getAttackPower() * (1f + statsTotal.getBonusAttackPowerMultiplier())));
        statsProcs.setHealth(statsProcs.getHealth() + (float) Math.floor(statsProcs.getStamina() * 10f) + (float) Math.floor(statsProcs.getBattlemasterHealth()));
        statsProcs.setHealth(statsProcs.getHealth() * (1f + statsProcs.getBonusHealthMultiplier()));
        statsProcs.setArmor(statsProcs.getArmor() + 2f * (float) Math.floor(statsProcs.getAgility()) + statsProcs.getBonusArmor());
        statsProcs.setArmor((float) Math.floor(statsProcs.getArmor() * (1f + statsTotal.getBonusArmorMultiplier())));
        statsTotal.accumulate(statsProcs);

        return statsTotal;
    }

    // NOTE: This is currently unused, because it doesn't account for procs which are both mitigation and survival (ie armor and agility procs)
    @SuppressWarnings("unused")
    private void accumulateSpecialEffect(Stats statsProcs, Stats statsEffect, float effectUptime, float temporarySurvivalScale) {
        if (temporarySurvivalScale != 1f && statsEffect.getArmor() + statsEffect.getHealth() + statsEffect.getStamina() > 0f) {
            // Subject to Temporary Survival scaling
            if (temporarySurvivalScale < 1f) {
                statsProcs.accumulate(statsEffect, effectUptime * temporarySurvivalScale);
            } else {
                statsProcs.accumulate(statsEffect, 1f - ((2f - temporarySurvivalScale) * (1f - effectUptime)));
            }
        } else {
            statsProcs.accumulate(statsEffect, effectUptime);
        }
    }

    /**
     * Gets data for a custom chart that Rawr.Bear provides
     */
    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        switch (chartName) {
            case "Combat Table":
                CharacterCalculationsBear currentCalculationsBear = (CharacterCalculationsBear) getCharacterCalculations(character);
                ComparisonCalculationBear calcMiss = new ComparisonCalculationBear();
                ComparisonCalculationBear calcDodge = new ComparisonCalculationBear();
                ComparisonCalculationBear calcCrit = new ComparisonCalculationBear();
                ComparisonCalculationBear calcHit = new ComparisonCalculationBear();
                if (currentCalculationsBear != null) {
                    calcMiss.setName("    Miss    ");
                    calcDodge.setName("   Dodge   ");
                    calcCrit.setName("  Crit  ");
                    calcHit.setName("Hit");

                    float crits = 0.02f + (0.002f * (currentCalculationsBear.targetLevel - character.getLevel())) - currentCalculationsBear.cappedCritReduction;
                    float hits = Math.max(1f - (Math.max(0f, crits) + (currentCalculationsBear.avoidancePreDR)), 0f);

                    calcMiss.setMitigationPoints(currentCalculationsBear.miss * 100f);
                    calcMiss.setOverallPoints(currentCalculationsBear.miss * 100f);
                    calcDodge.setOverallPoints(currentCalculationsBear.dodge * 100f);
                    calcDodge.setMitigationPoints(currentCalculationsBear.dodge * 100f);
                    calcCrit.setOverallPoints(crits * 100f);
                    calcCrit.setSurvivalPoints(crits * 100f);
                    calcHit.setOverallPoints(hits * 100f);
                    calcHit.setSurvivalPoints(hits * 100f);
                }
                return new ComparisonCalculationBase[] { calcMiss, calcDodge, calcCrit, calcHit };

            case "Rotation DPS":
                CharacterCalculationsBear calcsDPS = (CharacterCalculationsBear) getCharacterCalculations(character);
                List<ComparisonCalculationBase> comparisonsDPS = new ArrayList<ComparisonCalculationBase>();

                BearRotationCalculator rotationCalculatorDPS = new BearRotationCalculator(calcsDPS.meleeDamageAverage,
                        calcsDPS.maulDamageAverage, calcsDPS.mangleDamageAverage, calcsDPS.swipeDamageAverage,
                        calcsDPS.faerieFireDamageAverage, calcsDPS.lacerateDamageAverage, calcsDPS.lacerateDotDamageAverage, calcsDPS.meleeThreatAverage,
                        calcsDPS.maulThreatAverage, calcsDPS.mangleThreatAverage, calcsDPS.swipeThreatAverage,
                        calcsDPS.faerieFireThreatAverage, calcsDPS.lacerateThreatAverage, calcsDPS.lacerateDotThreatAverage,
                        6f - calcsDPS.basicStats.getMangleCooldownReduction(), calcsDPS.attackSpeed);

                for (int useMaul = 0; useMaul < 3; useMaul++)
                    for (int useMangle = 0; useMangle < 2; useMangle++)
                        for (int useSwipe = 0; useSwipe < 2; useSwipe++)
                            for (int useFaerieFire = 0; useFaerieFire < 2; useFaerieFire++)
                                for (int useLacerate = 0; useLacerate < 2; useLacerate++) {
                                    Boolean[] useMaulValues = new Boolean[] { null, false, true };
                                    BearRotationCalculator.BearRotationCalculation rotationCalculation =
                                            rotationCalculatorDPS.getRotationCalculations(useMaulValues[useMaul],
                                                    useMangle == 1, useSwipe == 1, useFaerieFire == 1, useLacerate == 1);

                                    ComparisonCalculationBear comparison = new ComparisonCalculationBear();
                                    comparison.setCharacterItems(character.getItems());
                                    comparison.setName(rotationCalculation.name.replace("+", " + "));
                                    comparison.setOverallPoints(rotationCalculation.dps);
                                    comparison.setThreatPoints(rotationCalculation.dps);
                                    comparisonsDPS.add(comparison);
                                }

                return comparisonsDPS.toArray(new ComparisonCalculationBase[0]);

            case "Rotation TPS":
                CharacterCalculationsBear calcsTPS = (CharacterCalculationsBear) getCharacterCalculations(character);
                List<ComparisonCalculationBase> comparisonsTPS = new ArrayList<ComparisonCalculationBase>();

                BearRotationCalculator rotationCalculatorTPS = new BearRotationCalculator(calcsTPS.meleeDamageAverage,
                        calcsTPS.maulDamageAverage, calcsTPS.mangleDamageAverage, calcsTPS.swipeDamageAverage,
                        calcsTPS.faerieFireDamageAverage, calcsTPS.lacerateDamageAverage, calcsTPS.lacerateDotDamageAverage, calcsTPS.meleeThreatAverage,
                        calcsTPS.maulThreatAverage, calcsTPS.mangleThreatAverage, calcsTPS.swipeThreatAverage,
                        calcsTPS.faerieFireThreatAverage, calcsTPS.lacerateThreatAverage, calcsTPS.lacerateDotThreatAverage,
                        6f - calcsTPS.basicStats.getMangleCooldownReduction(), calcsTPS.attackSpeed);

                for (int useMaul = 0; useMaul < 3; useMaul++)
                    for (int useMangle = 0; useMangle < 2; useMangle++)
                        for (int useSwipe = 0; useSwipe < 2; useSwipe++)
                            for (int useFaerieFire = 0; useFaerieFire < 2; useFaerieFire++)
                                for (int useLacerate = 0; useLacerate < 2; useLacerate++) {
                                    Boolean[] useMaulValues = new Boolean[] { null, false, true };
                                    BearRotationCalculator.BearRotationCalculation rotationCalculation =
                                            rotationCalculatorTPS.getRotationCalculations(useMaulValues[useMaul],
                                                    useMangle == 1, useSwipe == 1, useFaerieFire == 1, useLacerate == 1);

                                    ComparisonCalculationBear comparison = new ComparisonCalculationBear();
                                    comparison.setCharacterItems(character.getItems());
                                    comparison.setName(rotationCalculation.name.replace("+", " + "));
                                    comparison.setOverallPoints(rotationCalculation.tps);
                                    comparison.setThreatPoints(rotationCalculation.tps);
                                    comparisonsTPS.add(comparison);
                                }

                return comparisonsTPS.toArray(new ComparisonCalculationBase[0]);

            default:
                return new ComparisonCalculationBase[0];
        }
    }

    @Override
    public boolean isItemRelevant(Item item) {
        if (item.getSlot() == ItemSlot.OffHand
                || (item.getSlot() == ItemSlot.Ranged && item.getType() != ItemType.Idol))
            return false;
        return super.isItemRelevant(item);
    }

    public Stats getRelevantStats(Stats stats) {
        Stats s = new Stats();
        s.setArmor(stats.getArmor());
        s.setBonusArmor(stats.getBonusArmor());
        s.setStamina(stats.getStamina());
        s.setAgility(stats.getAgility());
        s.setDodgeRating(stats.getDodgeRating());
        s.setDefenseRating(stats.getDefenseRating());
        s.setResilience(stats.getResilience());
        s.setBonusAgilityMultiplier(stats.getBonusAgilityMultiplier());
        s.setBaseArmorMultiplier(stats.getBaseArmorMultiplier());
        s.setBonusArmorMultiplier(stats.getBonusArmorMultiplier());
        s.setBonusStaminaMultiplier(stats.getBonusStaminaMultiplier());
        s.setBonusStrengthMultiplier(stats.getBonusStrengthMultiplier());
        s.setHealth(stats.getHealth());
        s.setBattlemasterHealth(stats.getBattlemasterHealth());
        s.setBonusHealthMultiplier(stats.getBonusHealthMultiplier());
        s.setMiss(stats.getMiss());
        s.setCritChanceReduction(stats.getCritChanceReduction());
        s.setArcaneResistance(stats.getArcaneResistance());
        s.setNatureResistance(stats.getNatureResistance());
        s.setFireResistance(stats.getFireResistance());
        s.setFrostResistance(stats.getFrostResistance());
        s.setShadowResistance(stats.getShadowResistance());
        s.setArcaneResistanceBuff(stats.getArcaneResistanceBuff());
        s.setNatureResistanceBuff(stats.getNatureResistanceBuff());
        s.setFireResistanceBuff(stats.getFireResistanceBuff());
        s.setFrostResistanceBuff(stats.getFrostResistanceBuff());
        s.setShadowResistanceBuff(stats.getShadowResistanceBuff());
        s.setHighestStat(stats.getHighestStat());
        s.setParagon(stats.getParagon());
        s.setDodge(stats.getDodge());

        s.setStrength(stats.getStrength());
        s.setAttackPower(stats.getAttackPower());
        s.setCritRating(stats.getCritRating());
        s.setPhysicalCrit(stats.getPhysicalCrit());
        s.setHitRating(stats.getHitRating());
        s.setPhysicalHit(stats.getPhysicalHit());
        s.setMoteOfAnger(stats.getMoteOfAnger());
        s.setHasteRating(stats.getHasteRating());
        s.setPhysicalHaste(stats.getPhysicalHaste());
        s.setExpertiseRating(stats.getExpertiseRating());
        s.setArmorPenetration(stats.getArmorPenetration());
        s.setWeaponDamage(stats.getWeaponDamage());
        s.setBonusCritMultiplier(stats.getBonusCritMultiplier());
        s.setBonusMangleBearThreat(stats.getBonusMangleBearThreat());
        s.setBonusLacerateDamageMultiplier(stats.getBonusLacerateDamageMultiplier());
        s.setBonusBearSwipeDamageMultiplier(stats.getBonusBearSwipeDamageMultiplier());
        s.setBonusAttackPowerMultiplier(stats.getBonusAttackPowerMultiplier());
        s.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());
        s.setDamageTakenMultiplier(stats.getDamageTakenMultiplier());
        s.setArmorPenetrationRating(stats.getArmorPenetrationRating());
        s.setBossAttackSpeedMultiplier(stats.getBossAttackSpeedMultiplier());
        s.setSpellCrit(stats.getSpellCrit());
        s.setSpellCritOnTarget(stats.getSpellCritOnTarget());
        s.setIntellect(stats.getIntellect());
        s.setBonusNatureDamageMultiplier(stats.getBonusNatureDamageMultiplier());
        s.setSpellHit(stats.getSpellHit());
        s.setThreatIncreaseMultiplier(stats.getThreatIncreaseMultiplier());
        for (SpecialEffect effect : stats.specialEffects()) {
            if (effect.getTrigger() == Trigger.Use || effect.getTrigger() == Trigger.MeleeCrit || effect.getTrigger() == Trigger.MeleeHit || effect.getTrigger() == Trigger.MeleeAttack
                    || effect.getTrigger() == Trigger.PhysicalCrit || effect.getTrigger() == Trigger.PhysicalHit || effect.getTrigger() == Trigger.DoTTick
                    || effect.getTrigger() == Trigger.DamageDone || effect.getTrigger() == Trigger.MangleBearHit || effect.getTrigger() == Trigger.LacerateTick
                    || effect.getTrigger() == Trigger.SwipeBearOrLacerateHit || effect.getTrigger() == Trigger.DamageTaken || effect.getTrigger() == Trigger.DamageTakenPhysical
                    || effect.getTrigger() == Trigger.MangleCatOrShredOrInfectedWoundsHit || effect.getTrigger() == Trigger.DamageOrHealingDone) {
                if (hasRelevantStats(effect.getStats())) {
                    s.addSpecialEffect(effect);
                }
            }
        }
        return s;
    }

    @Override
    public boolean hasRelevantStats(Stats stats) {
        boolean relevant = (stats.getAgility() + stats.getArmor() + stats.getBonusArmor() + stats.getBonusAgilityMultiplier() + stats.getBonusArmorMultiplier()
                + stats.getBonusStaminaMultiplier() + stats.getDefenseRating() + stats.getDodgeRating() + stats.getHealth() + stats.getBonusHealthMultiplier()
                + stats.getBattlemasterHealth() + stats.getMiss() + stats.getResilience() + stats.getStamina() + stats.getMoteOfAnger()
                + stats.getArcaneResistance() + stats.getNatureResistance() + stats.getFireResistance() + stats.getDodge()
                + stats.getFrostResistance() + stats.getShadowResistance() + stats.getArcaneResistanceBuff()
                + stats.getNatureResistanceBuff() + stats.getFireResistanceBuff() + stats.getPhysicalCrit()
                + stats.getFrostResistanceBuff() + stats.getShadowResistanceBuff() + stats.getCritChanceReduction()
                + stats.getArmorPenetrationRating() + stats.getPhysicalHaste() + stats.getBonusBearSwipeDamageMultiplier()
                + stats.getStrength() + stats.getAttackPower() + stats.getCritRating() + stats.getHitRating() + stats.getHasteRating()
                + stats.getExpertiseRating() + stats.getArmorPenetration() + stats.getWeaponDamage() + stats.getBonusCritMultiplier()
                + stats.getBonusRipDuration() + stats.getHighestStat() + stats.getParagon() + stats.getPhysicalHit()
                + stats.getBonusMangleBearThreat() + stats.getBonusLacerateDamageMultiplier()
                + stats.getBonusAttackPowerMultiplier() + stats.getBonusDamageMultiplier()
                + stats.getDamageTakenMultiplier() + stats.getArmorPenetrationRating() + stats.getBossAttackSpeedMultiplier()
                + stats.getSpellCrit() + stats.getSpellCritOnTarget() + stats.getIntellect() + stats.getBonusNatureDamageMultiplier()
                + stats.getSpellHit() + stats.getThreatIncreaseMultiplier()
        ) != 0;

        for (SpecialEffect effect : stats.specialEffects()) {
            if (effect.getTrigger() == Trigger.Use || effect.getTrigger() == Trigger.MeleeCrit || effect.getTrigger() == Trigger.MeleeHit || effect.getTrigger() == Trigger.MeleeAttack
                    || effect.getTrigger() == Trigger.PhysicalCrit || effect.getTrigger() == Trigger.PhysicalHit
                    || effect.getTrigger() == Trigger.MangleBearHit || effect.getTrigger() == Trigger.SwipeBearOrLacerateHit
                    || effect.getTrigger() == Trigger.DamageTaken || effect.getTrigger() == Trigger.DamageTakenPhysical
                    || effect.getTrigger() == Trigger.MangleCatOrShredOrInfectedWoundsHit || effect.getTrigger() == Trigger.LacerateTick) {
                relevant |= hasRelevantStats(effect.getStats());
                if (relevant) break;
            }
        }
        return relevant;
    }

    public Stats getBuffsStats(Character character, CalculationOptionsBear calcOpts) {
        List<Buff> removedBuffs = new ArrayList<Buff>();
        List<Buff> addedBuffs = new ArrayList<Buff>();

        // Passive Ability Auto-Fixing (commented out in the original)
        Stats statsBuffs = getBuffsStats(character.getActiveBuffs());

        for (Buff b : removedBuffs) {
            character.activeBuffsAdd(b);
        }
        for (Buff b : addedBuffs) {
            character.getActiveBuffs().remove(b);
        }

        return statsBuffs;
    }

    public void setDefaults(Character character) {
        character.activeBuffsAdd("Horn of Winter");
        character.activeBuffsAdd("Devotion Aura");
        character.activeBuffsAdd("Inspiration");
        character.activeBuffsAdd("Battle Shout");
        character.activeBuffsAdd("Unleashed Rage");
        character.activeBuffsAdd("Improved Moonkin Form");
        character.activeBuffsAdd("Commanding Shout");
        character.activeBuffsAdd("Leader of the Pack");
        character.activeBuffsAdd("Improved Icy Talons");
        character.activeBuffsAdd("Power Word: Fortitude");
        character.activeBuffsAdd("Mark of the Wild");
        character.activeBuffsAdd("Blessing of Kings");
        character.activeBuffsAdd("Sunder Armor");
        character.activeBuffsAdd("Faerie Fire");
        character.activeBuffsAdd("Totem of Wrath");
        character.activeBuffsAdd("Flask of Stoneblood");
        character.activeBuffsAdd("Agility Food");
        character.activeBuffsAdd("Heroism/Bloodlust");

        character.getDruidTalents().setGlyphOfMaul(true);
        character.getDruidTalents().setGlyphOfGrowl(true);
        character.getDruidTalents().setGlyphOfFrenziedRegeneration(true);
    }

    private static List<String> relevantGlyphs;

    public List<String> getRelevantGlyphs() {
        if (relevantGlyphs == null) {
            relevantGlyphs = new ArrayList<String>();
            relevantGlyphs.add("Glyph of Maul");
            relevantGlyphs.add("Glyph of Growl");
            relevantGlyphs.add("Glyph of Frenzied Regeneration");
            relevantGlyphs.add("Glyph of Mangle");
            relevantGlyphs.add("Glyph of Berserk");
        }
        return relevantGlyphs;
    }
}
