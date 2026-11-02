// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Cat/CalculationsCat.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: UI-only members omitted (CalculationOptionsPanel, the RAWR3/WPF branches of the color
// and panel properties); the non-Silverlight display label/color/optimizable/relevant-glyph tables
// are ported additively as getCharacterDisplayCalculationLabels()/getSubPointNameColors()/
// getOptimizableCalculationLabels()/getRelevantGlyphs(). GetBuffsStats(Character,
// CalculationOptionsCat) only called the base GetBuffsStats(ActiveBuffs) (its auto-fix block is
// commented out in C#) and is therefore not overridden; getCharacterStats calls the base directly.
// FRAMEWORK ADAPTATION: DeserializeDataObject uses a JDK-XML reflective reader instead of
// XmlSerializer (same approach as CalculationsRestoSham).
package rawr.models.cat;

import rawr.base.stats.BaseStats;
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
import rawr.base.stats.WeightedStat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CalculationsCat extends CalculationsBase {
    // my insides all turned to ash / so slow
    // and blew away as i collapsed / so cold

    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        // Relevant Gem IDs for Cats
        //Red
        int[] delicate = { 39905, 39997, 40112, 42143 }; // Agi
        int[] fractured = { 39909, 40002, 40117, 42153 }; // ArP

        //Purple
        int[] shifting = { 39935, 40023, 40130, 40130 }; // Agi/Sta
        int[] puissant = { 39933, 40033, 40140, 40140 }; // ArP/Sta
        int[] forceful = { 39978, 40091, 40169, 40169 }; // Haste/Sta

        //Blue

        //Green

        //Yellow

        //Orange
        int[] glinting = { 39953, 40044, 40148, 40148 }; // Agi/Hit
        int[] deadly = { 39952, 40043, 40147, 40147 }; // Agi/Crit
        int[] deft = { 39955, 40046, 40150, 40150 }; // Agi/Haste

        // Prismatic
        int[] nightmare = { 49110, 49110, 49110, 49110 };

        //Meta
        int relentless = 41398;

        List<GemmingTemplate> list = new ArrayList<GemmingTemplate>();
        for (int tier = 0; tier < 4; tier++) {
            list.add(createCatGemmingTemplate(tier, fractured, fractured, fractured, fractured, relentless)); //Max ArP
            list.add(createCatGemmingTemplate(tier, fractured, deadly, puissant, fractured, relentless)); //ArP/Crit
            list.add(createCatGemmingTemplate(tier, fractured, deft, puissant, fractured, relentless)); //ArP/Haste
            list.add(createCatGemmingTemplate(tier, fractured, glinting, puissant, fractured, relentless)); //ArP/Hit
            list.add(createCatGemmingTemplate(tier, fractured, deadly, nightmare, fractured, relentless)); //ArP/Crit/Nightmare
            list.add(createCatGemmingTemplate(tier, fractured, deft, nightmare, fractured, relentless)); //ArP/Haste/Nightmare
            list.add(createCatGemmingTemplate(tier, fractured, glinting, nightmare, fractured, relentless)); //ArP/Hit/Nightmare
            list.add(createCatGemmingTemplate(tier, delicate, delicate, delicate, delicate, relentless)); //Max Agi
            list.add(createCatGemmingTemplate(tier, delicate, deadly, shifting, delicate, relentless)); //Agi/Crit
            list.add(createCatGemmingTemplate(tier, delicate, deft, shifting, delicate, relentless)); //Agi/Haste
            list.add(createCatGemmingTemplate(tier, delicate, glinting, shifting, delicate, relentless)); //Agi/Hit
            list.add(createCatGemmingTemplate(tier, delicate, deadly, nightmare, delicate, relentless)); //Agi/Crit/Nightmare
            list.add(createCatGemmingTemplate(tier, delicate, deft, nightmare, delicate, relentless)); //Agi/Haste/Nightmare
            list.add(createCatGemmingTemplate(tier, delicate, glinting, nightmare, delicate, relentless)); //Agi/Hit/Nightmare
        }

        return list;
    }

    private static final int DEFAULT_GEMMING_TIER = 2;

    private GemmingTemplate createCatGemmingTemplate(int tier, int[] red, int[] yellow, int[] blue, int[] prismatic, int meta) {
        GemmingTemplate template = new GemmingTemplate();
        template.setModel("Cat");
        template.setGroup((new String[] { "Uncommon", "Rare", "Epic", "Jeweler" })[tier]);
        template.setEnabled(tier == DEFAULT_GEMMING_TIER);
        template.setRedId(red[tier]);
        template.setYellowId(yellow[tier]);
        template.setBlueId(blue[tier]);
        template.setPrismaticId(prismatic[tier]);
        template.setMetaId(meta);
        return template;
    }

    // C# CalculationsCat.cs:106-157 CharacterDisplayCalculationLabels.
    public static final String[] CharacterDisplayCalculationLabels = new String[] {
            "Summary:Overall Points*Sum of your DPS Points and Survivability Points",
            "Summary:DPS Points*DPS Points is your theoretical DPS.",
            "Summary:Survivability Points*One hundreth of your health.",

            "Basic Stats:Health",
            "Basic Stats:Attack Power",
            "Basic Stats:Agility",
            "Basic Stats:Strength",
            "Basic Stats:Crit Rating",
            "Basic Stats:Hit Rating",
            "Basic Stats:Expertise Rating",
            "Basic Stats:Haste Rating",
            "Basic Stats:Armor Penetration Rating",
            "Basic Stats:Weapon Damage",

            "Complex Stats:Avoided Attacks",
            "Complex Stats:Crit Chance",
            "Complex Stats:Attack Speed",
            "Complex Stats:Armor Mitigation",

            "Abilities:Optimal Rotation",
            "Abilities:Optimal Rotation DPS",
            "Abilities:Custom Rotation DPS",
            "Abilities:Melee",
            "Abilities:Mangle",
            "Abilities:Shred",
            "Abilities:Rake",
            "Abilities:Rip",
            "Abilities:Bite",
    };

    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        return List.of(CharacterDisplayCalculationLabels);
    }

    // C# CalculationsCat.cs:159-177 OptimizableCalculationLabels.
    public static final String[] OptimizableCalculationLabels = new String[] {
            "Custom Rotation DPS",
            "Health",
            "Avoided Attacks %",
            "Nature Resist",
            "Fire Resist",
            "Frost Resist",
            "Shadow Resist",
            "Arcane Resist",
    };

    public String[] getOptimizableCalculationLabels() {
        return OptimizableCalculationLabels;
    }

    // C# CalculationsCat.cs:179-190 CustomChartNames (all entries commented out).
    public String[] getCustomChartNames() {
        return new String[0];
    }

    // C# CalculationsCat.cs:207-222 SubPointNameColors (non-Silverlight branch).
    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> colors = new LinkedHashMap<String, String>();
        colors.put("DPS", "#A000E0");
        colors.put("Survivability", "#408020");
        return colors;
    }

    @Override
    public String getOverallRatingLabel() {
        return "Overall Points";
    }

    // C# CalculationsCat.cs:224-243 RelevantItemTypes.
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
    public CharacterClass getTargetClass() {
        return CharacterClass.Druid;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationCat();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsCat();
    }

    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            CalculationOptionsCat calcOpts = new CalculationOptionsCat();
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
            Object value = readXmlValue(child, type, setter.getGenericParameterTypes()[0]);
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

    private static Object readXmlValue(org.w3c.dom.Element element, Class<?> type, java.lang.reflect.Type genericType) {
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

    public static CalculationsCat instance;

    public static CalculationsCat getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("Cat", CalculationsCat.class);
    }

    public static void register() {
        Calculations.registerModel("Cat", CalculationsCat.class);
    }

    public CalculationsCat() {
        instance = this;
    }

    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        CalculationOptionsCat calcOpts = character.getCalculationOptions() instanceof CalculationOptionsCat
                ? (CalculationOptionsCat) character.getCalculationOptions()
                : new CalculationOptionsCat();
        int targetLevel = calcOpts.getTargetLevel();
        float targetArmor = calcOpts.getTargetArmor();
        CatStatsContext catContext = computeStats(character, additionalItem);
        Stats stats = catContext.stats;
        float levelDifference = (targetLevel - 80f) * 0.2f;
        CharacterCalculationsCat calculatedStats = new CharacterCalculationsCat();
        calculatedStats.basicStats = stats;
        calculatedStats.targetLevel = targetLevel;
        boolean maintainMangle = stats.getBonusBleedDamageMultiplier() == 0f;
        stats.setBonusBleedDamageMultiplier(0.3f);

        // region Basic Chances and Constants
        float modArmor = 0f;
        for (int i = 0; i < catContext.temporaryArPenRatingUptimes.length; i++) {
            modArmor += catContext.temporaryArPenRatingUptimes[i].chance() * StatConversion.getArmorDamageReduction(character.getLevel(), calcOpts.getTargetLevel(), calcOpts.getTargetArmor(), stats.getArmorPenetration(), 0f, stats.getArmorPenetrationRating() + catContext.temporaryArPenRatingUptimes[i].value());
        }

        modArmor = 1f - modArmor;
        float critMultiplier = 2f * (1f + stats.getBonusCritMultiplier());
        float critMultiplierBleed = 2f * (1f + stats.getBonusCritMultiplier());
        float hasteBonus = StatConversion.getPhysicalHasteFromRating(stats.getHasteRating(), CharacterClass.Druid);
        float attackSpeed = 1f / (1f + hasteBonus);
        attackSpeed = attackSpeed / (1f + stats.getPhysicalHaste());

        float hitBonus = stats.getPhysicalHit() + StatConversion.getPhysicalHitFromRating(stats.getHitRating(), CharacterClass.Druid);
        float expertiseBonus = StatConversion.getDodgeParryReducFromExpertise(StatConversion.getExpertiseFromRating(stats.getExpertiseRating(), CharacterClass.Druid) + stats.getExpertise(), CharacterClass.Druid);

        float chanceDodge = Math.max(0f, StatConversion.WHITE_DODGE_CHANCE_CAP[targetLevel - 80] - expertiseBonus);
        float chanceParry = 0f; //Math.Max(0f, StatConversion.WHITE_PARRY_CHANCE_CAP[targetLevel - 80] - expertiseBonus);
        float chanceMiss = Math.max(0f, StatConversion.WHITE_MISS_CHANCE_CAP[targetLevel - 80] - hitBonus);

        float glanceMultiplier = 0.7f;
        float chanceAvoided = chanceMiss + chanceDodge + chanceParry;
        float chanceNonAvoided = 1f - chanceAvoided;

        ////Crit Chances
        float chanceCritYellow = 0f;
        float chanceHitYellow = 0f;
        float cpPerCPG = 0f;
        float chanceCritBite = 0f;
        float chanceHitBite = 0f;
        float chanceCritBleed = 0f;
        float chanceCritRip = 0f;
        float chanceCritRake = 0f;
        float chanceGlance = 0f;
        float chanceCritWhite = 0f;
        float chanceHitWhite = 0f;

        for (int i = 0; i < catContext.temporaryCritRatingUptimes.length; i++) { //Sum up the weighted chances for each crit value
            WeightedStat iStat = catContext.temporaryCritRatingUptimes[i];
            //Yellow - 2 Roll, so total of X chance to avoid, total of 1 chance to crit and hit when not avoided
            float chanceCritYellowTemp = Math.min(1f, StatConversion.getCritFromRating(stats.getCritRating() + iStat.value(), CharacterClass.Druid)
                    + StatConversion.getCritFromAgility(stats.getAgility(), CharacterClass.Druid)
                    + stats.getPhysicalCrit()
                    + StatConversion.NPC_LEVEL_CRIT_MOD[targetLevel - 80]);
            float chanceHitYellowTemp = 1f - chanceCritYellowTemp;
            float cpPerCPGTemp = chanceHitYellowTemp + chanceCritYellowTemp * (1f + stats.getBonusCPOnCrit());

            //Bite - Identical to Yellow, with higher crit chance
            float chanceCritBiteTemp = Math.min(1f, chanceCritYellowTemp + stats.getBonusFerociousBiteCrit());
            float chanceHitBiteTemp = 1f - chanceCritBiteTemp;

            //Bleeds - 1 Roll, no avoidance, total of 1 chance to crit and hit
            float chanceCritBleedTemp = character.getDruidTalents().getPrimalGore() > 0 ? chanceCritYellowTemp : 0f;
            float chanceCritRipTemp = Math.min(1f, chanceCritBleedTemp > 0f ? chanceCritBleedTemp + stats.getBonusRipCrit() : 0f);
            float chanceCritRakeTemp = stats.getBonusRakeCrit() > 0 ? chanceCritBleedTemp : 0;

            //White
            float chanceGlanceTemp = StatConversion.WHITE_GLANCE_CHANCE_CAP[targetLevel - 80];
            float chanceCritWhiteTemp = Math.min(chanceCritYellowTemp, 1f - chanceGlanceTemp - chanceAvoided);
            float chanceHitWhiteTemp = 1f - chanceCritWhiteTemp - chanceAvoided - chanceGlanceTemp;

            chanceCritYellow += iStat.chance() * chanceCritYellowTemp;
            chanceHitYellow += iStat.chance() * chanceHitYellowTemp;
            cpPerCPG += iStat.chance() * cpPerCPGTemp;
            chanceCritBite += iStat.chance() * chanceCritBiteTemp;
            chanceHitBite += iStat.chance() * chanceHitBiteTemp;
            chanceCritBleed += iStat.chance() * chanceCritBleedTemp;
            chanceCritRip += iStat.chance() * chanceCritRipTemp;
            chanceCritRake += iStat.chance() * chanceCritRakeTemp;
            chanceGlance += iStat.chance() * chanceGlanceTemp;
            chanceCritWhite += iStat.chance() * chanceCritWhiteTemp;
            chanceHitWhite += iStat.chance() * chanceHitWhiteTemp;
        }

        calculatedStats.dodgedAttacks = chanceDodge * 100f;
        calculatedStats.parriedAttacks = chanceParry * 100f;
        calculatedStats.missedAttacks = chanceMiss * 100f;

        float timeToReapplyDebuffs = 1f / (1f - chanceAvoided) - 1f;
        float lagVariance = (float) calcOpts.getLagVariance() / 1000f;
        float mangleDurationUptime = 60f;// (character.DruidTalents.GlyphOfMangle ? 18f : 12f);
        float mangleDurationAverage = mangleDurationUptime - timeToReapplyDebuffs - lagVariance;
        float rakeDurationUptime = 9f + stats.getBonusRakeDuration();
        float rakeDurationAverage = rakeDurationUptime + timeToReapplyDebuffs + lagVariance;
        float ripDurationUptime = 12f + stats.getBonusRipDuration(); //Doesn't include Glyph of Shred
        float ripDurationAverage = ripDurationUptime + timeToReapplyDebuffs + lagVariance; //Doesn't include Glyph of Shred
        float roarBonusDuration = stats.getBonusSavageRoarDuration() - lagVariance;
        float berserkDuration = character.getDruidTalents().getBerserk() > 0 ? (character.getDruidTalents().isGlyphOfBerserk() ? 20f : 15f) : 0f;
        // endregion

        // region Attack Damages
        float baseDamage = 55f + (stats.getAttackPower() / 14f) + stats.getWeaponDamage();
        float meleeDamageRaw = (baseDamage) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * modArmor;
        float mangleDamageRaw = (baseDamage * 2f + 566f) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + stats.getBonusMangleDamageMultiplier()) * modArmor;
        float shredDamageRaw = (baseDamage * 2.25f + 666f + stats.getBonusShredDamage()) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + stats.getBonusShredDamageMultiplier()) * (1f + stats.getBonusBleedDamageMultiplier()) * modArmor;
        float rakeDamageRaw = (176f + stats.getAttackPower() * 0.01f) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + stats.getBonusRakeDamageMultiplier()) * (1f + stats.getBonusBleedDamageMultiplier());
        float rakeDamageDot = (1074f + stats.getAttackPower() * 0.18f) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + stats.getBonusRakeDamageMultiplier()) * (1f + stats.getBonusBleedDamageMultiplier()) * ((9f + stats.getBonusRakeDuration()) / 9f);
        float ripDamageRaw = (3006f + stats.getAttackPower() * 0.3f + (stats.getBonusRipDamagePerCPPerTick() * 5f * 6f)) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + stats.getBonusRipDamageMultiplier()) * (1f + stats.getBonusBleedDamageMultiplier());
        float biteBaseDamageRaw = 190f * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + stats.getBonusFerociousBiteDamageMultiplier()) * modArmor;
        float biteCPDamageRaw = (290f + stats.getAttackPower() * 0.07f) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + stats.getBonusFerociousBiteDamageMultiplier()) * modArmor;

        float meleeDamageAverage = chanceGlance * meleeDamageRaw * glanceMultiplier
                + chanceCritWhite * meleeDamageRaw * critMultiplier
                + chanceHitWhite * meleeDamageRaw;
        float mangleDamageAverage = (1f - chanceCritYellow) * mangleDamageRaw + chanceCritYellow * mangleDamageRaw * critMultiplier;
        float shredDamageAverage = (1f - chanceCritYellow) * shredDamageRaw + chanceCritYellow * shredDamageRaw * critMultiplier;
        float rakeDamageAverage = ((1f - chanceCritYellow) * rakeDamageRaw + chanceCritYellow * rakeDamageRaw * critMultiplier) + ((1f - chanceCritRake) * rakeDamageDot + chanceCritRake * rakeDamageDot * critMultiplierBleed);
        float ripDamageAverage = ((1f - chanceCritRip) * ripDamageRaw + chanceCritRip * ripDamageRaw * critMultiplierBleed);
        float biteBaseDamageAverage = (1f - chanceCritBite) * biteBaseDamageRaw + chanceCritBite * biteBaseDamageRaw * critMultiplier;
        float biteCPDamageAverage = (1f - chanceCritBite) * biteCPDamageRaw + chanceCritBite * biteCPDamageRaw * critMultiplier;
        // endregion

        // region Energy Costs
        float mangleEnergyRaw = 45f - stats.getMangleCatCostReduction();
        float shredEnergyRaw = 60f - stats.getShredCostReduction();
        float rakeEnergyRaw = 40f - stats.getRakeCostReduction();
        float ripEnergyRaw = 30f - stats.getRipCostReduction();
        float biteEnergyRaw = 35f; //Assuming no wasted energy
        float roarEnergyRaw = 25f;

        //[rawCost + ((1/chance_to_land) - 1) * rawCost/5]
        float cpgEnergyCostMultiplier = 1f + ((1f / chanceNonAvoided) - 1f) * 0.2f;
        float finisherEnergyCostMultiplier = 1f + ((1f / chanceNonAvoided) - 1f) * (1f - stats.getFinisherEnergyOnAvoid());
        float mangleEnergyAverage = mangleEnergyRaw * cpgEnergyCostMultiplier;
        float shredEnergyAverage = shredEnergyRaw * cpgEnergyCostMultiplier;
        float rakeEnergyAverage = rakeEnergyRaw * cpgEnergyCostMultiplier;
        float ripEnergyAverage = ripEnergyRaw * finisherEnergyCostMultiplier;
        float biteEnergyAverage = biteEnergyRaw * finisherEnergyCostMultiplier;
        float roarEnergyAverage = roarEnergyRaw;
        // endregion

        // region Ability Stats
        CatMeleeStats meleeStats = new CatMeleeStats();
        meleeStats.damagePerHit = meleeDamageRaw;
        meleeStats.damagePerSwing = meleeDamageAverage;

        CatMangleStats mangleStats = new CatMangleStats();
        mangleStats.damagePerHit = mangleDamageRaw;
        mangleStats.damagePerSwing = mangleDamageAverage;
        mangleStats.durationUptime = mangleDurationUptime;
        mangleStats.durationAverage = mangleDurationAverage;
        mangleStats.energyCost = mangleEnergyAverage;

        CatShredStats shredStats = new CatShredStats();
        shredStats.damagePerHit = shredDamageRaw;
        shredStats.damagePerSwing = shredDamageAverage;
        shredStats.energyCost = shredEnergyAverage;

        CatRakeStats rakeStats = new CatRakeStats();
        rakeStats.damagePerHit = rakeDamageRaw + rakeDamageDot;
        rakeStats.damagePerSwing = rakeDamageAverage;
        rakeStats.durationUptime = rakeDurationUptime;
        rakeStats.durationAverage = rakeDurationAverage;
        rakeStats.energyCost = rakeEnergyAverage;

        CatRipStats ripStats = new CatRipStats();
        ripStats.damagePerHit = ripDamageRaw;
        ripStats.damagePerSwing = ripDamageAverage;
        ripStats.durationUptime = ripDurationUptime;
        ripStats.durationAverage = ripDurationAverage;
        ripStats.energyCost = ripEnergyAverage;

        CatBiteStats biteStats = new CatBiteStats();
        biteStats.damagePerHit = biteBaseDamageRaw;
        biteStats.damagePerSwing = biteBaseDamageAverage;
        biteStats.damagePerHitPerCP = biteCPDamageRaw;
        biteStats.damagePerSwingPerCP = biteCPDamageAverage;
        biteStats.energyCost = biteEnergyAverage;

        CatRoarStats roarStats = new CatRoarStats();
        roarStats.durationUptime = roarBonusDuration;
        roarStats.durationAverage = 9f + roarBonusDuration;
        roarStats.energyCost = roarEnergyAverage;
        roarStats.durationPerCP = 5f;
        // endregion

        // region Rotations
        CatRotationCalculator rotationCalculator = new CatRotationCalculator(stats, calcOpts.getDuration(), cpPerCPG,
                maintainMangle, berserkDuration, attackSpeed, character.getDruidTalents().getOmenOfClarity() > 0,
                character.getDruidTalents().isGlyphOfShred(), chanceAvoided, chanceCritYellow * stats.getBonusCPOnCrit(),
                cpgEnergyCostMultiplier, stats.getClearcastOnBleedChance(), meleeStats, mangleStats, shredStats,
                rakeStats, ripStats, biteStats, roarStats);
        CatRotationCalculator.CatRotationCalculation rotationCalculationDPS = new CatRotationCalculator.CatRotationCalculation();

        for (int roarCP = 1; roarCP < 6; roarCP++)
            for (int biteCP = 0; biteCP < 6; biteCP++)
                for (int useRake = 0; useRake < 2; useRake++)
                    for (int useShred = 0; useShred < 2; useShred++)
                        for (int useRip = 0; useRip < 2; useRip++) {
                            CatRotationCalculator.CatRotationCalculation rotationCalculation =
                                    rotationCalculator.getRotationCalculations(
                                            useRake == 1, useShred == 1, useRip == 1, biteCP, roarCP);
                            if (rotationCalculation.dps > rotationCalculationDPS.dps)
                                rotationCalculationDPS = rotationCalculation;
                        }

        calculatedStats.highestDPSRotation = rotationCalculationDPS;
        calculatedStats.customRotation = rotationCalculator.getRotationCalculations(
                calcOpts.getCustomUseRake(), calcOpts.getCustomUseShred(), calcOpts.getCustomUseRip(), calcOpts.getCustomCPFerociousBite(), calcOpts.getCustomCPSavageRoar());

        if (character.getDruidTalents().isGlyphOfShred() && rotationCalculationDPS.shredCount > 0) {
            ripStats.durationUptime += 6f;
            ripStats.durationAverage += 6f;
        }
        ripStats.damagePerHit *= ripStats.durationUptime / 12f;
        ripStats.damagePerSwing *= ripStats.durationUptime / 12f;
        // endregion

        calculatedStats.avoidedAttacks = chanceAvoided * 100f;
        calculatedStats.dodgedAttacks = chanceDodge * 100f;
        calculatedStats.parriedAttacks = chanceParry * 100f;
        calculatedStats.missedAttacks = chanceMiss * 100f;
        calculatedStats.critChance = chanceCritYellow * 100f;
        calculatedStats.attackSpeed = attackSpeed;
        calculatedStats.armorMitigation = (1f - modArmor) * 100f;
        calculatedStats.duration = calcOpts.getDuration();

        calculatedStats.meleeStats = meleeStats;
        calculatedStats.mangleStats = mangleStats;
        calculatedStats.shredStats = shredStats;
        calculatedStats.rakeStats = rakeStats;
        calculatedStats.ripStats = ripStats;
        calculatedStats.roarStats = roarStats;
        calculatedStats.biteStats = biteStats;

        float magicDPS = (stats.getShadowDamage() + stats.getArcaneDamage()) * (1f + chanceCritYellow);
        float abomDPS = (stats.getMoteOfAnger() * meleeDamageAverage);
        calculatedStats.setDPSPoints(calculatedStats.highestDPSRotation.dps + magicDPS + abomDPS);
        calculatedStats.setSurvivabilityPoints(stats.getHealth() / 100f);
        calculatedStats.setOverallPoints(calculatedStats.getDPSPoints() + calculatedStats.getSurvivabilityPoints());
        return calculatedStats;
    }

    /**
     * Per-calculation holder for the Cat stats plus the two proc-uptime scratch arrays that the C#
     * {@code StatsCat} subclass carried. Keeping them in a local context object avoids
     * per-calculation instance state on the (shared) model object; {@link #getCharacterStats}
     * discards the context, while {@code getCharacterCalculations} uses it directly.
     */
    private static final class CatStatsContext {
        Stats stats;
        WeightedStat[] temporaryCritRatingUptimes;
        WeightedStat[] temporaryArPenRatingUptimes;
    }

    @Override
    public Stats getCharacterStats(Character character, Item additionalItem) {
        return computeStats(character, additionalItem).stats;
    }

    private CatStatsContext computeStats(Character character, Item additionalItem) {
        CatStatsContext context = new CatStatsContext();
        CalculationOptionsCat calcOpts = character.getCalculationOptions() instanceof CalculationOptionsCat
                ? (CalculationOptionsCat) character.getCalculationOptions()
                : new CalculationOptionsCat();
        int targetLevel = calcOpts.getTargetLevel();

        Stats statsItems = new Stats();
        accumulateItemStats(statsItems, character, additionalItem);
        statsItems.setArmor(statsItems.getArmor() * new float[] { 1f, 1.04f, 1.07f, 1.1f }[character.getDruidTalents().getThickHide()]);

        DruidTalents talents = character.getDruidTalents();
        Stats statsTotal = new Stats();
        context.stats = statsTotal;
        statsTotal.setPhysicalCrit(0.02f * talents.getSharpenedClaws()
                + 0.02f * talents.getMasterShapeshifter()
                + ((character.activeBuffsContains("Leader of the Pack")
                        || character.activeBuffsContains("Rampage"))
                        ? 0 : 0.05f * talents.getLeaderOfThePack()));
        statsTotal.setDodge(0.02f * talents.getFeralSwiftness());
        statsTotal.setBonusStaminaMultiplier((1f + 0.02f * talents.getSurvivalOfTheFittest()) * (1f + 0.01f * talents.getImprovedMarkOfTheWild()) - 1f);
        statsTotal.setBonusAgilityMultiplier((1f + 0.02f * talents.getSurvivalOfTheFittest()) * (1f + 0.01f * talents.getImprovedMarkOfTheWild()) - 1f);
        statsTotal.setBonusStrengthMultiplier((1f + 0.02f * talents.getSurvivalOfTheFittest()) * (1f + 0.01f * talents.getImprovedMarkOfTheWild()) - 1f);
        statsTotal.setBonusAttackPowerMultiplier(0.02f * talents.getHeartOfTheWild());
        statsTotal.setCritChanceReduction(0.02f * talents.getSurvivalOfTheFittest());
        statsTotal.setBonusPhysicalDamageMultiplier((1f + 0.02f * talents.getNaturalist()) * (talents.isGlyphOfSavageRoar() ? 1.33f : 1.3f) - 1f);
        statsTotal.setBonusMangleDamageMultiplier((1f + 0.1f * talents.getSavageFury()) * (talents.isGlyphOfMangle() ? 1.1f : 1.0f) - 1f);
        statsTotal.setBonusRakeDamageMultiplier(0.1f * talents.getSavageFury());
        statsTotal.setBonusShredDamageMultiplier(0.04f * talents.getRendAndTear());
        statsTotal.setBonusFerociousBiteCrit(0.05f * talents.getRendAndTear());
        statsTotal.setBonusEnergyOnTigersFury(20f * talents.getKingOfTheJungle());
        statsTotal.setMangleCatCostReduction(1f * talents.getFerocity() + 2f * talents.getImprovedMangle());
        statsTotal.setRakeCostReduction(1f * talents.getFerocity());
        statsTotal.setShredCostReduction(9f * talents.getShreddingAttacks());
        statsTotal.setBonusCPOnCrit(0.5f * talents.getPrimalFury());
        statsTotal.setExpertise(5f * talents.getPrimalPrecision());
        statsTotal.setFinisherEnergyOnAvoid(0.4f * talents.getPrimalPrecision());
        statsTotal.setAttackPower((character.getLevel() / 2f) * talents.getPredatoryStrikes());
        statsTotal.setBonusCritMultiplier(0.1f * ((float) talents.getPredatoryInstincts() / 3f));
        statsTotal.setBonusFerociousBiteDamageMultiplier(0.03f * talents.getFeralAggression());
        statsTotal.setBonusRipDuration(talents.isGlyphOfRip() ? 4f : 0f);

        statsTotal.accumulate(BaseStats.getBaseStats(80, character.getCharacterClass(), character.getRace(), BaseStats.DruidForm.Cat));
        statsTotal.accumulate(statsItems);
        statsTotal.accumulate(getBuffsStats(character.getActiveBuffs()));

        float predatoryStrikesAP = 0f;
        float fap = 0f;
        if (character.get(CharacterSlot.MainHand) != null) {
            fap = Math.max(0f, (character.get(CharacterSlot.MainHand).getItem().getDPS() - 54.8f) * 14f); //TODO Find a more accurate number for this?
            predatoryStrikesAP = (fap + character.get(CharacterSlot.MainHand).getItem().getStats().getAttackPower()) * 0.2f * (talents.getPredatoryStrikes() / 3f);
            if (character.get(CharacterSlot.MainHand).getEnchant() != null) {
                predatoryStrikesAP += character.get(CharacterSlot.MainHand).getEnchant().stats.getAttackPower() * 0.2f * (talents.getPredatoryStrikes() / 3f);
            }
        }

        statsTotal.setStamina((float) Math.floor(statsTotal.getStamina() * (1f + statsTotal.getBonusStaminaMultiplier())));
        statsTotal.setStrength((float) Math.floor((statsTotal.getStrength() + statsTotal.getCatFormStrength()) * (1f + statsTotal.getBonusStrengthMultiplier())));
        statsTotal.setAgility((float) Math.floor(statsTotal.getAgility() * (1f + statsTotal.getBonusAgilityMultiplier())));
        statsTotal.setAttackPower(statsTotal.getAttackPower() + statsTotal.getStrength() * 2f + statsTotal.getAgility() + fap + predatoryStrikesAP);
        statsTotal.setAttackPower((float) Math.floor(statsTotal.getAttackPower() * (1f + statsTotal.getBonusAttackPowerMultiplier())));
        statsTotal.setHealth(statsTotal.getHealth() + (float) Math.floor((statsTotal.getStamina() - 20f) * 10f + 20f));
        statsTotal.setHealth((float) Math.floor(statsTotal.getHealth() * (1f + statsTotal.getBonusHealthMultiplier())));
        statsTotal.setArmor(statsTotal.getArmor() + 2f * statsTotal.getAgility());
        statsTotal.setArmor((float) Math.floor(statsTotal.getArmor() * (1f + statsTotal.getBonusArmorMultiplier())));
        statsTotal.setNatureResistance(statsTotal.getNatureResistance() + statsTotal.getNatureResistanceBuff());
        statsTotal.setFireResistance(statsTotal.getFireResistance() + statsTotal.getFireResistanceBuff());
        statsTotal.setFrostResistance(statsTotal.getFrostResistance() + statsTotal.getFrostResistanceBuff());
        statsTotal.setShadowResistance(statsTotal.getShadowResistance() + statsTotal.getShadowResistanceBuff());
        statsTotal.setArcaneResistance(statsTotal.getArcaneResistance() + statsTotal.getArcaneResistanceBuff());
        statsTotal.setWeaponDamage(statsTotal.getWeaponDamage() + 16f); //Tiger's Fury

        float hasteBonus = StatConversion.getPhysicalHasteFromRating(statsTotal.getHasteRating(), CharacterClass.Druid);
        hasteBonus = (1f + hasteBonus) * (1f + statsTotal.getPhysicalHaste()) - 1f;
        float meleeHitInterval = 1f / ((1f + hasteBonus) + 1f / 3.5f);
        float hitBonus = StatConversion.getPhysicalHitFromRating(statsTotal.getHitRating()) + statsTotal.getPhysicalHit();
        float expertiseBonus = StatConversion.getDodgeParryReducFromExpertise(StatConversion.getExpertiseFromRating(statsTotal.getExpertiseRating(), CharacterClass.Druid) + statsTotal.getExpertise(), CharacterClass.Druid);
        float chanceDodge = Math.max(0f, StatConversion.WHITE_DODGE_CHANCE_CAP[targetLevel - 80] - expertiseBonus);
        float chanceParry = 0f;// Math.Max(0f, StatConversion.WHITE_PARRY_CHANCE_CAP[targetLevel - 80] - expertiseBonus);
        float chanceMiss = Math.max(0f, StatConversion.WHITE_MISS_CHANCE_CAP[targetLevel - 80] - hitBonus);
        float chanceAvoided = chanceMiss + chanceDodge + chanceParry;

        float rawChanceCrit = StatConversion.getPhysicalCritFromRating(statsTotal.getCritRating())
                + StatConversion.getPhysicalCritFromAgility(statsTotal.getAgility(), CharacterClass.Druid)
                + statsTotal.getPhysicalCrit()
                + StatConversion.NPC_LEVEL_CRIT_MOD[targetLevel - 80];
        float chanceCrit = rawChanceCrit * (1f - chanceAvoided);
        float chanceHit = 1f - chanceAvoided;
        boolean usesMangle = (talents.getMangle() > 0 && !character.activeBuffsContains("Mangle") && !character.activeBuffsContains("Trauma"));

        Map<Trigger, Float> triggerIntervals = new LinkedHashMap<Trigger, Float>();
        Map<Trigger, Float> triggerChances = new LinkedHashMap<Trigger, Float>();
        triggerIntervals.put(Trigger.Use, 0f);
        triggerIntervals.put(Trigger.MeleeAttack, meleeHitInterval);
        triggerIntervals.put(Trigger.MeleeHit, meleeHitInterval);
        triggerIntervals.put(Trigger.PhysicalHit, meleeHitInterval);
        triggerIntervals.put(Trigger.MeleeCrit, meleeHitInterval);
        triggerIntervals.put(Trigger.PhysicalCrit, meleeHitInterval);
        triggerIntervals.put(Trigger.DoTTick, 1.5f);
        triggerIntervals.put(Trigger.DamageDone, meleeHitInterval / 2f);
        triggerIntervals.put(Trigger.DamageOrHealingDone, meleeHitInterval / 2f); // Need to Add Self-Heals
        triggerIntervals.put(Trigger.RakeTick, 3f + (float) calcOpts.getLagVariance() / 3000f);
        if (usesMangle)
            triggerIntervals.put(Trigger.MangleCatHit, 60f);
        triggerIntervals.put(Trigger.MangleCatOrShredHit, usesMangle ? 3.76f : 3.87f);
        triggerIntervals.put(Trigger.MangleCatOrShredOrInfectedWoundsHit, triggerIntervals.get(Trigger.MangleCatOrShredHit) / ((talents.getInfectedWounds() > 0) ? 2f : 1f));
        triggerChances.put(Trigger.Use, 1f);
        triggerChances.put(Trigger.MeleeAttack, 1f);
        triggerChances.put(Trigger.MeleeHit, Math.max(0f, chanceHit));
        triggerChances.put(Trigger.PhysicalHit, Math.max(0f, chanceHit));
        triggerChances.put(Trigger.MeleeCrit, Math.max(0f, chanceCrit));
        triggerChances.put(Trigger.PhysicalCrit, Math.max(0f, chanceCrit));
        triggerChances.put(Trigger.DoTTick, 1f);
        triggerChances.put(Trigger.DamageDone, 1f - chanceAvoided / 2f);
        triggerChances.put(Trigger.DamageOrHealingDone, 1f - chanceAvoided / 2f); // Need to Add Self-Heals
        triggerChances.put(Trigger.RakeTick, 1f);
        if (talents.getMangle() > 0 && !character.activeBuffsContains("Mangle") && !character.activeBuffsContains("Trauma"))
            triggerChances.put(Trigger.MangleCatHit, chanceHit);
        triggerChances.put(Trigger.MangleCatOrShredHit, chanceHit);
        triggerChances.put(Trigger.MangleCatOrShredOrInfectedWoundsHit, chanceHit);

        // Handle Trinket procs
        Stats statsProcs = new Stats();
        for (SpecialEffect effect : statsTotal.specialEffects(se -> triggerIntervals.containsKey(se.getTrigger()))) {
            // JOTHAY's NOTE: The following is an ugly hack to add Recursive Effects to Cat
            // so Victor's Call and similar trinkets can be given more appropriate value
            if (effect.getTrigger() == Trigger.Use && effect.getStats().getRawSpecialEffectDataSize() == 1
                    && triggerIntervals.containsKey(effect.getStats().getRawSpecialEffectData()[0].getTrigger())) {
                float upTime = effect.getAverageUptime(triggerIntervals.get(effect.getTrigger()),
                        triggerChances.get(effect.getTrigger()), 1f, calcOpts.getDuration());
                statsProcs.accumulate(effect.getStats().getRawSpecialEffectData()[0].getAverageStats(
                        triggerIntervals.get(effect.getStats().getRawSpecialEffectData()[0].getTrigger()),
                        triggerChances.get(effect.getStats().getRawSpecialEffectData()[0].getTrigger()), 1f, calcOpts.getDuration()),
                        upTime);
            } else if (effect.getStats().getMoteOfAnger() > 0) {
                // When in effect stats, MoteOfAnger is % of melee hits
                // When in character stats, MoteOfAnger is average procs per second
                statsProcs.setMoteOfAnger(effect.getStats().getMoteOfAnger() * effect.getAverageProcsPerSecond(triggerIntervals.get(effect.getTrigger()),
                        triggerChances.get(effect.getTrigger()), 1f, calcOpts.getDuration()) / effect.getMaxStack());
            } else {
                statsProcs.accumulate(effect.getAverageStats(triggerIntervals.get(effect.getTrigger()),
                        triggerChances.get(effect.getTrigger()), 1f, calcOpts.getDuration()),
                        effect.getStats().getDeathbringerProc() > 0 ? 1f / 3f : 1f);
            }
        }

        statsProcs.setAgility(statsProcs.getAgility() + statsProcs.getHighestStat() + statsProcs.getParagon() + statsProcs.getDeathbringerProc());
        statsProcs.setStrength(statsProcs.getStrength() + statsProcs.getDeathbringerProc());
        statsProcs.setStamina((float) Math.floor(statsProcs.getStamina() * (1f + statsTotal.getBonusStaminaMultiplier())));
        statsProcs.setStrength((float) Math.floor(statsProcs.getStrength() * (1f + statsTotal.getBonusStrengthMultiplier())));
        statsProcs.setAgility((float) Math.floor(statsProcs.getAgility() * (1f + statsTotal.getBonusAgilityMultiplier())));
        statsProcs.setAttackPower(statsProcs.getAttackPower() + statsProcs.getStrength() * 2f + statsProcs.getAgility());
        statsProcs.setAttackPower((float) Math.floor(statsProcs.getAttackPower() * (1f + statsTotal.getBonusAttackPowerMultiplier())));
        statsProcs.setHasteRating(statsProcs.getHasteRating() + statsProcs.getDeathbringerProc());
        statsProcs.setHealth(statsProcs.getHealth() + (float) Math.floor(statsProcs.getStamina() * 10f));
        statsProcs.setArmor(statsProcs.getArmor() + 2f * statsProcs.getAgility());
        statsProcs.setArmor((float) Math.floor(statsProcs.getArmor() * (1f + statsTotal.getBonusArmorMultiplier())));

        //Agility is only used for crit from here on out; we'll be converting Agility to CritRating,
        //and calculating CritRating separately, so don't add any Agility or CritRating from procs here.
        //Also calculating ArPen separately, so don't add that either.
        statsProcs.setCritRating(0f);
        statsProcs.setAgility(0f);
        statsProcs.setArmorPenetrationRating(0f);
        statsTotal.accumulate(statsProcs);

        //Handle Crit procs
        context.temporaryCritRatingUptimes = new WeightedStat[0];
        List<SpecialEffect> tempCritEffects = new ArrayList<SpecialEffect>();
        List<Float> tempCritEffectIntervals = new ArrayList<Float>();
        List<Float> tempCritEffectChances = new ArrayList<Float>();
        List<Float> tempCritEffectScales = new ArrayList<Float>();

        for (SpecialEffect effect : statsTotal.specialEffects(se -> triggerIntervals.containsKey(se.getTrigger()) && (se.getStats().getCritRating() + se.getStats().getAgility() + se.getStats().getDeathbringerProc() + se.getStats().getHighestStat() + se.getStats().getParagon()) > 0)) {
            tempCritEffects.add(effect);
            tempCritEffectIntervals.add(triggerIntervals.get(effect.getTrigger()));
            tempCritEffectChances.add(triggerChances.get(effect.getTrigger()));
            tempCritEffectScales.add(effect.getStats().getDeathbringerProc() > 0 ? 1f / 3f : 1f);
        }

        if (tempCritEffects.isEmpty()) {
            context.temporaryCritRatingUptimes = new WeightedStat[] { new WeightedStat(1f, 0f) };
        } else if (tempCritEffects.size() == 1) { //Only one, add it to
            SpecialEffect effect = tempCritEffects.get(0);
            float uptime = effect.getAverageUptime(triggerIntervals.get(effect.getTrigger()), triggerChances.get(effect.getTrigger()), 1f, calcOpts.getDuration()) * tempCritEffectScales.get(0);
            float totalAgi = (float) effect.getMaxStack() * (effect.getStats().getAgility() + effect.getStats().getDeathbringerProc() + effect.getStats().getHighestStat() + effect.getStats().getParagon()) * (1f + statsTotal.getBonusAgilityMultiplier());
            context.temporaryCritRatingUptimes = new WeightedStat[] {
                    new WeightedStat(uptime, effect.getStats().getCritRating() + StatConversion.getCritFromAgility(totalAgi,
                            CharacterClass.Druid) * StatConversion.RATING_PER_PHYSICALCRIT),
                    new WeightedStat(1f - uptime, 0f) };
        } else if (tempCritEffects.size() > 1) {
            List<Float> tempCritEffectsValues = new ArrayList<Float>();
            for (SpecialEffect effect : tempCritEffects) {
                float totalAgi = (float) effect.getMaxStack() * (effect.getStats().getAgility() + effect.getStats().getDeathbringerProc() + effect.getStats().getHighestStat() + effect.getStats().getParagon()) * (1f + statsTotal.getBonusAgilityMultiplier());
                tempCritEffectsValues.add(effect.getStats().getCritRating()
                        + StatConversion.getCritFromAgility(totalAgi, CharacterClass.Druid)
                        * StatConversion.RATING_PER_PHYSICALCRIT);
            }

            float[] intervals = new float[tempCritEffects.size()];
            float[] chances = new float[tempCritEffects.size()];
            float[] offset = new float[tempCritEffects.size()];
            for (int i = 0; i < tempCritEffects.size(); i++) {
                intervals[i] = triggerIntervals.get(tempCritEffects.get(i).getTrigger());
                chances[i] = triggerChances.get(tempCritEffects.get(i).getTrigger());
            }
            if (tempCritEffects.size() >= 2) {
                offset[0] = calcOpts.getTrinketOffset();
            }
            WeightedStat[] critWeights = SpecialEffect.getAverageCombinedUptimeCombinations(tempCritEffects.toArray(new SpecialEffect[0]), intervals, chances, offset, toFloatArray(tempCritEffectScales), 1f, calcOpts.getDuration(), toFloatArray(tempCritEffectsValues));
            context.temporaryCritRatingUptimes = critWeights;
        }

        //Handle ArPen procs
        context.temporaryArPenRatingUptimes = new WeightedStat[0];
        List<SpecialEffect> tempArPenEffects = new ArrayList<SpecialEffect>();
        List<Float> tempArPenEffectIntervals = new ArrayList<Float>();
        List<Float> tempArPenEffectChances = new ArrayList<Float>();
        List<Float> tempArPenEffectScales = new ArrayList<Float>();

        for (SpecialEffect effect : statsTotal.specialEffects(se -> triggerIntervals.containsKey(se.getTrigger()) && se.getStats().getArmorPenetrationRating() > 0)) {
            tempArPenEffects.add(effect);
            tempArPenEffectIntervals.add(triggerIntervals.get(effect.getTrigger()));
            tempArPenEffectChances.add(triggerChances.get(effect.getTrigger()));
            tempArPenEffectScales.add(effect.getStats().getDeathbringerProc() > 0 ? 1f / 3f : 1f);
        }

        if (tempArPenEffects.isEmpty()) {
            context.temporaryArPenRatingUptimes = new WeightedStat[] { new WeightedStat(1f, 0f) };
        } else if (tempArPenEffects.size() == 1) { //Only one, add it to
            SpecialEffect effect = tempArPenEffects.get(0);
            float uptime = effect.getAverageUptime(triggerIntervals.get(effect.getTrigger()), triggerChances.get(effect.getTrigger()), 1f, calcOpts.getDuration()) * tempArPenEffectScales.get(0);
            context.temporaryArPenRatingUptimes = new WeightedStat[] {
                    new WeightedStat(uptime, effect.getStats().getArmorPenetrationRating()),
                    new WeightedStat(1f - uptime, 0f) };
        } else if (tempArPenEffects.size() > 1) {
            List<Float> tempArPenEffectValues = new ArrayList<Float>();
            for (SpecialEffect effect : tempArPenEffects) {
                tempArPenEffectValues.add(effect.getStats().getArmorPenetrationRating());
            }

            float[] intervals = new float[tempArPenEffects.size()];
            float[] chances = new float[tempArPenEffects.size()];
            float[] offset = new float[tempArPenEffects.size()];
            for (int i = 0; i < tempArPenEffects.size(); i++) {
                intervals[i] = triggerIntervals.get(tempArPenEffects.get(i).getTrigger());
                chances[i] = triggerChances.get(tempArPenEffects.get(i).getTrigger());
            }
            if (tempArPenEffects.size() >= 2) {
                offset[0] = calcOpts.getTrinketOffset();
            }
            WeightedStat[] arPenWeights = SpecialEffect.getAverageCombinedUptimeCombinations(tempArPenEffects.toArray(new SpecialEffect[0]), intervals, chances, offset, toFloatArray(tempArPenEffectScales), 1f, calcOpts.getDuration(), toFloatArray(tempArPenEffectValues));
            context.temporaryArPenRatingUptimes = arPenWeights;
        }

        return context;
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }

    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        switch (chartName) {
            case "White Combat Table":
                CharacterCalculationsCat calcs = (CharacterCalculationsCat) getCharacterCalculations(character);
                float[] ct = null;//calcs.MeleeStats.CombatTable;
                ComparisonCalculationCat miss = new ComparisonCalculationCat();
                miss.setName("Miss"); miss.setOverallPoints(ct[0]); miss.setDPSPoints(ct[0]);
                ComparisonCalculationCat dodge = new ComparisonCalculationCat();
                dodge.setName("Dodge"); dodge.setOverallPoints(ct[1]); dodge.setDPSPoints(ct[1]);
                ComparisonCalculationCat parry = new ComparisonCalculationCat();
                parry.setName("Parry"); parry.setOverallPoints(ct[2]); parry.setDPSPoints(ct[2]);
                ComparisonCalculationCat glance = new ComparisonCalculationCat();
                glance.setName("Glance"); glance.setOverallPoints(ct[3]); glance.setDPSPoints(ct[3]);
                ComparisonCalculationCat hit = new ComparisonCalculationCat();
                hit.setName("Hit"); hit.setOverallPoints(ct[4]); hit.setDPSPoints(ct[4]);
                ComparisonCalculationCat crit = new ComparisonCalculationCat();
                crit.setName("Crit"); crit.setOverallPoints(ct[5]); crit.setDPSPoints(ct[5]);
                return new ComparisonCalculationBase[] { miss, dodge, parry, glance, hit, crit };

            default:
                return new ComparisonCalculationBase[0];
        }
    }

    @Override
    public boolean isItemRelevant(Item item) {
        if (item.getSlot() == ItemSlot.OffHand
                || (item.getSlot() == ItemSlot.Ranged && item.getType() != ItemType.Idol)
                || item.getStats().getSpellPower() > 0)
            return false;
        if (item.getStats().containsSpecialEffect(s -> s.getStats().getSpellPower() > 0)) return false;
        return super.isItemRelevant(item);
    }

    public Stats getRelevantStats(Stats stats) {
        Stats s = new Stats();
        s.setAgility(stats.getAgility());
        s.setStrength(stats.getStrength());
        s.setCatFormStrength(stats.getCatFormStrength());
        s.setAttackPower(stats.getAttackPower());
        s.setCritRating(stats.getCritRating());
        s.setHitRating(stats.getHitRating());
        s.setStamina(stats.getStamina());
        s.setHasteRating(stats.getHasteRating());
        s.setExpertiseRating(stats.getExpertiseRating());
        s.setArmorPenetration(stats.getArmorPenetration());
        s.setArmorPenetrationRating(stats.getArmorPenetrationRating());
        s.setBonusShredDamage(stats.getBonusShredDamage());
        s.setBonusRipDamagePerCPPerTick(stats.getBonusRipDamagePerCPPerTick());
        s.setWeaponDamage(stats.getWeaponDamage());
        s.setBonusAgilityMultiplier(stats.getBonusAgilityMultiplier());
        s.setBonusAttackPowerMultiplier(stats.getBonusAttackPowerMultiplier());
        s.setBonusCritMultiplier(stats.getBonusCritMultiplier());
        s.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());
        s.setBonusHealthMultiplier(stats.getBonusHealthMultiplier());
        s.setBonusRipDamageMultiplier(stats.getBonusRipDamageMultiplier());
        s.setBonusStaminaMultiplier(stats.getBonusStaminaMultiplier());
        s.setBonusStrengthMultiplier(stats.getBonusStrengthMultiplier());
        s.setHealth(stats.getHealth());
        s.setMangleCatCostReduction(stats.getMangleCatCostReduction());
        s.setTigersFuryCooldownReduction(stats.getTigersFuryCooldownReduction());
        s.setThreatReductionMultiplier(stats.getThreatReductionMultiplier());
        s.setPhysicalHaste(stats.getPhysicalHaste());
        s.setPhysicalHit(stats.getPhysicalHit());
        s.setBonusBleedDamageMultiplier(stats.getBonusBleedDamageMultiplier());
        s.setPhysicalCrit(stats.getPhysicalCrit());
        s.setBonusSavageRoarDuration(stats.getBonusSavageRoarDuration());
        s.setClearcastOnBleedChance(stats.getClearcastOnBleedChance());
        s.setArcaneDamage(stats.getArcaneDamage());
        s.setShadowDamage(stats.getShadowDamage());
        s.setHighestStat(stats.getHighestStat());
        s.setParagon(stats.getParagon());
        s.setDeathbringerProc(stats.getDeathbringerProc());
        s.setBonusRakeDuration(stats.getBonusRakeDuration());
        s.setBonusRipCrit(stats.getBonusRipCrit());
        s.setBonusRakeCrit(stats.getBonusRakeCrit());
        s.setRipCostReduction(stats.getRipCostReduction());
        s.setMoteOfAnger(stats.getMoteOfAnger());

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
        s.setBonusRipDuration(stats.getBonusRipDuration());

        for (SpecialEffect effect : stats.specialEffects()) {
            if (effect.getTrigger() == Trigger.Use || effect.getTrigger() == Trigger.MeleeCrit || effect.getTrigger() == Trigger.MeleeHit || effect.getTrigger() == Trigger.MeleeAttack
                    || effect.getTrigger() == Trigger.PhysicalCrit || effect.getTrigger() == Trigger.PhysicalHit || effect.getTrigger() == Trigger.DoTTick
                    || effect.getTrigger() == Trigger.DamageDone || effect.getTrigger() == Trigger.MangleCatHit || effect.getTrigger() == Trigger.RakeTick
                    || effect.getTrigger() == Trigger.MangleCatOrShredHit || effect.getTrigger() == Trigger.MangleCatOrShredOrInfectedWoundsHit || effect.getTrigger() == Trigger.DamageOrHealingDone) {
                if (hasRelevantStats(effect.getStats())) {
                    s.addSpecialEffect(effect);
                }
            }
        }
        return s;
    }

    @Override
    public boolean hasRelevantStats(Stats stats) {
        boolean relevant = (stats.getAgility() + stats.getArmorPenetration() + stats.getAttackPower() + stats.getPhysicalCrit()
                + stats.getBonusAgilityMultiplier() + stats.getBonusAttackPowerMultiplier() + stats.getBonusCritMultiplier()
                + stats.getClearcastOnBleedChance() + stats.getBonusSavageRoarDuration() + stats.getBonusRakeCrit() + stats.getRipCostReduction()
                + stats.getBonusDamageMultiplier() + stats.getBonusRipDamageMultiplier() + stats.getBonusShredDamage()
                + stats.getBonusStaminaMultiplier() + stats.getBonusStrengthMultiplier() + stats.getCritRating() + stats.getExpertiseRating()
                + stats.getHasteRating() + stats.getHealth() + stats.getHitRating() + stats.getMangleCatCostReduction() + /*stats.Stamina +*/
                stats.getStrength() + stats.getCatFormStrength() + stats.getWeaponDamage() + stats.getDeathbringerProc()
                + stats.getPhysicalHit() + stats.getBonusRipDamagePerCPPerTick() + stats.getBonusRipCrit() + stats.getMoteOfAnger()
                + stats.getPhysicalHaste() + stats.getArmorPenetrationRating() + stats.getBonusRipDuration() + stats.getBonusRakeDuration()
                + stats.getThreatReductionMultiplier() + stats.getArcaneDamage() + stats.getShadowDamage()
                + stats.getArcaneResistance() + stats.getNatureResistance() + stats.getFireResistance() + stats.getBonusBleedDamageMultiplier() + stats.getParagon()
                + stats.getFrostResistance() + stats.getShadowResistance() + stats.getArcaneResistanceBuff() + stats.getTigersFuryCooldownReduction() + stats.getHighestStat()
                + stats.getNatureResistanceBuff() + stats.getFireResistanceBuff() + stats.getBonusPhysicalDamageMultiplier()
                + stats.getFrostResistanceBuff() + stats.getShadowResistanceBuff()) > 0 || (stats.getStamina() > 0 && stats.getSpellPower() == 0);

        for (SpecialEffect effect : stats.specialEffects()) {
            if (effect.getTrigger() == Trigger.Use || effect.getTrigger() == Trigger.MeleeCrit || effect.getTrigger() == Trigger.MeleeHit || effect.getTrigger() == Trigger.MeleeAttack
                    || effect.getTrigger() == Trigger.PhysicalCrit || effect.getTrigger() == Trigger.PhysicalHit || effect.getTrigger() == Trigger.RakeTick
                    || effect.getTrigger() == Trigger.MangleCatHit || effect.getTrigger() == Trigger.MangleCatOrShredHit
                    || effect.getTrigger() == Trigger.MangleCatOrShredOrInfectedWoundsHit || effect.getTrigger() == Trigger.DamageOrHealingDone) {
                relevant |= hasRelevantStats(effect.getStats());
                if (relevant) break;
            }
        }
        return relevant;
    }

    public void setDefaults(Character character) {
        character.activeBuffsAdd(("Horn of Winter"));
        character.activeBuffsAdd(("Battle Shout"));
        character.activeBuffsAdd(("Unleashed Rage"));
        character.activeBuffsAdd(("Improved Moonkin Form"));
        character.activeBuffsAdd(("Leader of the Pack"));
        character.activeBuffsAdd(("Improved Icy Talons"));
        character.activeBuffsAdd(("Power Word: Fortitude"));
        character.activeBuffsAdd(("Mark of the Wild"));
        character.activeBuffsAdd(("Blessing of Kings"));
        character.activeBuffsAdd(("Sunder Armor"));
        character.activeBuffsAdd(("Faerie Fire"));
        character.activeBuffsAdd(("Totem of Wrath"));
        character.activeBuffsAdd(("Flask of Endless Rage"));
        character.activeBuffsAdd(("Agility Food"));
        character.activeBuffsAdd(("Heroism/Bloodlust"));

        if (character.getPrimaryProfession() == rawr.base.character.Profession.Alchemy
                || character.getSecondaryProfession() == rawr.base.character.Profession.Alchemy)
            character.activeBuffsAdd(("Flask of Endless Rage (Mixology)"));

        character.getDruidTalents().setGlyphOfSavageRoar(true);
        character.getDruidTalents().setGlyphOfShred(true);
        character.getDruidTalents().setGlyphOfRip(true);
    }

    private static final List<String> relevantGlyphs = List.of(
            "Glyph of Mangle",
            "Glyph of Shred",
            "Glyph of Rip",
            "Glyph of Berserk",
            "Glyph of Savage Roar");

    public List<String> getRelevantGlyphs() {
        return relevantGlyphs;
    }
}
