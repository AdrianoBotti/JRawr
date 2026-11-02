// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Rogue/CalculationsRogue.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// FRAMEWORK ADAPTATIONS:
//  * System.Drawing.Color sub-point colors are ported as hex strings (LinkedHashMap<String, String>,
//    the same convention as CalculationsHunter/HealPriest).
//  * The UI-only CalculationOptionsPanelRogue property is omitted; deserializeDataObject uses a
//    JDK-XML reader instead of XmlSerializer.
//  * GetCharacterStatsWithTemporaryEffects returns its two out parameters in a small holder class.
package rawr.models.rogue;

import rawr.base.stats.BaseStats;
import rawr.base.character.Buff;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterRace;
import rawr.base.character.CharacterSlot;
import rawr.base.calculations.ComparisonCalculationBase;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class CalculationsRogue extends CalculationsBase {

    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        // Relevant Gem IDs for Rogues
        // Red
        int[] delicate = { 39905, 39997, 40112, 42143 }; // Agi
        int[] fractured = { 39909, 40002, 40117, 42153 }; // ArP
        int[] precise = { 39910, 40003, 40118, 42154 }; // Exp
        int[] bright = { 39906, 39999, 40114, 36766 }; // AP

        // Purple
        int[] shifting = { 39935, 40023, 40130, 40130 }; // Agi/Sta
        int[] puissant = { 39933, 40033, 40140, 40140 }; // ArP/Sta
        int[] balanced = { 39937, 40029, 40136, 40136 }; // AP/Sta
        int[] guardian = { 39940, 40034, 40141, 40141 }; // Exp/Sta

        // Blue
        int[] solid = { 39919, 40008, 40119, 36767 }; // Sta

        // Green
        int[] forceful = { 39978, 40091, 40169, 40169 }; // Haste/Sta
        int[] vivid = { 39975, 40088, 40166, 40166 }; // Hit/Sta

        // Yellow
        int[] quick = { 39918, 40017, 40128, 42150 }; // Haste
        int[] rigid = { 39915, 40014, 40125, 40156 }; // Hit

        // Orange
        int[] glinting = { 39953, 40044, 40148, 40148 }; // Agi/Hit
        int[] deadly = { 39952, 40043, 40147, 40147 }; // Agi/Crit
        int[] deft = { 39955, 40046, 40150, 40150 }; // Agi/Haste
        int[] pristine = { 39961, 40053, 40157, 40157 }; // AP/Hit
        int[] wicked = { 39960, 40052, 40156, 40156 }; // AP/Crit
        int[] stark = { 39963, 40055, 40159, 40159 }; // AP/Haste
        int[] accurate = { 39966, 40058, 40162, 40162 }; // Exp/Hit

        // Prismatic
        int[] nightmare = { 49110, 49110, 49110, 49110 }; // All stats

        // Meta
        int relentless = 41398; // Agi/Crit dmg

        List<GemmingTemplate> templates = new ArrayList<GemmingTemplate>();
        templates.add(gem("Uncommon", false, fractured[0], fractured[0], fractured[0], fractured[0], relentless)); // Max ArP
        templates.add(gem("Uncommon", false, fractured[0], quick[0], puissant[0], fractured[0], relentless)); // ArP/Crit
        templates.add(gem("Uncommon", false, fractured[0], quick[0], forceful[0], fractured[0], relentless)); // ArP/Haste
        templates.add(gem("Uncommon", false, delicate[0], delicate[0], delicate[0], delicate[0], relentless)); // Max Agility
        templates.add(gem("Uncommon", false, delicate[0], deadly[0], shifting[0], delicate[0], relentless)); // Agi/Crit
        templates.add(gem("Uncommon", false, delicate[0], quick[0], forceful[0], delicate[0], relentless)); // Agi/Haste
        templates.add(gem("Uncommon", false, bright[0], bright[0], bright[0], bright[0], relentless)); // Max Attack Power
        templates.add(gem("Uncommon", false, bright[0], wicked[0], balanced[0], bright[0], relentless)); // AP/Crit
        templates.add(gem("Uncommon", false, bright[0], quick[0], forceful[0], bright[0], relentless)); // AP/Haste
        templates.add(gem("Uncommon", false, precise[0], precise[0], precise[0], precise[0], relentless)); // Max Expertise

        templates.add(gem("Uncommon", false, delicate[0], glinting[0], shifting[0], delicate[0], relentless)); // Agi/Hit
        templates.add(gem("Uncommon", false, bright[0], pristine[0], balanced[0], bright[0], relentless)); // AP/Hit
        templates.add(gem("Uncommon", false, precise[0], accurate[0], guardian[0], precise[0], relentless)); // Exp+/Hit
        templates.add(gem("Uncommon", false, accurate[0], rigid[0], vivid[0], rigid[0], relentless)); // Exp/Hit+

        templates.add(gem("Rare", false, fractured[1], fractured[1], fractured[1], fractured[1], relentless)); // Max ArP
        templates.add(gem("Rare", false, fractured[1], quick[1], puissant[1], fractured[1], relentless)); // ArP/Crit
        templates.add(gem("Rare", false, fractured[1], quick[1], forceful[1], fractured[1], relentless)); // ArP/Haste
        templates.add(gem("Rare", false, delicate[1], delicate[1], delicate[1], delicate[1], relentless)); // Max Agility
        templates.add(gem("Rare", false, delicate[1], deadly[1], shifting[1], delicate[1], relentless)); // Agi/Crit
        templates.add(gem("Rare", false, delicate[1], quick[1], forceful[1], delicate[1], relentless)); // Agi/Haste
        templates.add(gem("Rare", false, bright[1], bright[1], bright[1], bright[1], relentless)); // Max Attack Power
        templates.add(gem("Rare", false, bright[1], wicked[1], balanced[1], bright[1], relentless)); // AP/Crit
        templates.add(gem("Rare", false, bright[1], quick[1], forceful[1], bright[1], relentless)); // AP/Haste
        templates.add(gem("Rare", false, precise[1], precise[1], precise[1], precise[1], relentless)); // Max Expertise

        templates.add(gem("Rare", false, fractured[1], fractured[1], nightmare[1], fractured[1], relentless)); // Max ArP
        templates.add(gem("Rare", false, fractured[1], quick[1], nightmare[1], fractured[1], relentless)); // ArP/Crit
        templates.add(gem("Rare", false, fractured[1], quick[1], nightmare[1], fractured[1], relentless)); // ArP/Haste
        templates.add(gem("Rare", false, delicate[1], delicate[1], nightmare[1], delicate[1], relentless)); // Max Agility
        templates.add(gem("Rare", false, delicate[1], deadly[1], nightmare[1], delicate[1], relentless)); // Agi/Crit
        templates.add(gem("Rare", false, delicate[1], quick[1], nightmare[1], delicate[1], relentless)); // Agi/Haste
        templates.add(gem("Rare", false, bright[1], bright[1], nightmare[1], bright[1], relentless)); // Max Attack Power
        templates.add(gem("Rare", false, bright[1], wicked[1], nightmare[1], bright[1], relentless)); // AP/Crit
        templates.add(gem("Rare", false, bright[1], quick[1], nightmare[1], bright[1], relentless)); // AP/Haste
        templates.add(gem("Rare", false, precise[1], precise[1], nightmare[1], precise[1], relentless)); // Max Expertise

        templates.add(gem("Rare", false, delicate[1], glinting[1], shifting[1], delicate[1], relentless)); // Agi/Hit
        templates.add(gem("Rare", false, bright[1], pristine[1], balanced[1], bright[1], relentless)); // AP/Hit
        templates.add(gem("Rare", false, precise[1], accurate[1], guardian[1], precise[1], relentless)); // Exp+/Hit
        templates.add(gem("Rare", false, accurate[1], rigid[1], vivid[1], rigid[1], relentless)); // Exp/Hit+

        templates.add(gem("Epic", true, fractured[2], fractured[2], fractured[2], fractured[2], relentless)); // Max ArP
        templates.add(gem("Epic", true, fractured[2], quick[2], puissant[2], fractured[2], relentless)); // ArP/Crit
        templates.add(gem("Epic", true, fractured[2], quick[2], forceful[2], fractured[2], relentless)); // ArP/Haste
        templates.add(gem("Epic", true, delicate[2], delicate[2], delicate[2], delicate[2], relentless)); // Max Agility
        templates.add(gem("Epic", true, delicate[2], deadly[2], shifting[2], delicate[2], relentless)); // Agi/Crit
        templates.add(gem("Epic", true, delicate[2], quick[2], forceful[2], delicate[2], relentless)); // Agi/Haste
        templates.add(gem("Epic", true, bright[2], bright[2], bright[2], bright[2], relentless)); // Max Attack Power
        templates.add(gem("Epic", true, bright[2], wicked[2], balanced[2], bright[2], relentless)); // AP/Crit
        templates.add(gem("Epic", true, bright[2], quick[2], forceful[2], bright[2], relentless)); // AP/Haste
        templates.add(gem("Epic", true, precise[2], precise[2], precise[2], precise[2], relentless)); // Max Expertise

        templates.add(gem("Epic", true, fractured[2], fractured[2], nightmare[2], fractured[2], relentless)); // Max ArP
        templates.add(gem("Epic", true, fractured[2], quick[2], nightmare[2], fractured[2], relentless)); // ArP/Crit
        templates.add(gem("Epic", true, fractured[2], quick[2], nightmare[2], fractured[2], relentless)); // ArP/Haste
        templates.add(gem("Epic", true, delicate[2], delicate[2], nightmare[2], delicate[2], relentless)); // Max Agility
        templates.add(gem("Epic", true, delicate[2], deadly[2], nightmare[2], delicate[2], relentless)); // Agi/Crit
        templates.add(gem("Epic", true, delicate[2], quick[2], nightmare[2], delicate[2], relentless)); // Agi/Haste
        templates.add(gem("Epic", true, bright[2], bright[2], nightmare[2], bright[2], relentless)); // Max Attack Power
        templates.add(gem("Epic", true, bright[2], wicked[2], nightmare[2], bright[2], relentless)); // AP/Crit
        templates.add(gem("Epic", true, bright[2], quick[2], nightmare[2], bright[2], relentless)); // AP/Haste
        templates.add(gem("Epic", true, precise[2], precise[2], nightmare[2], precise[2], relentless)); // Max Expertise

        templates.add(gem("Epic", true, delicate[2], glinting[2], shifting[2], delicate[2], relentless)); // Agi/Hit
        templates.add(gem("Epic", true, bright[2], pristine[2], balanced[2], bright[2], relentless)); // AP/Hit
        templates.add(gem("Epic", true, precise[2], accurate[2], guardian[2], precise[2], relentless)); // Exp+/Hit
        templates.add(gem("Epic", true, accurate[2], rigid[2], vivid[2], rigid[2], relentless)); // Exp/Hit+

        templates.add(gem("Jeweler", false, fractured[3], fractured[3], fractured[3], fractured[3], relentless)); // Max ArP
        templates.add(gem("Jeweler", false, fractured[3], quick[3], puissant[2], fractured[3], relentless)); // ArP/Crit
        templates.add(gem("Jeweler", false, fractured[3], quick[3], forceful[2], fractured[3], relentless)); // ArP/Haste
        templates.add(gem("Jeweler", false, delicate[3], delicate[3], delicate[3], delicate[3], relentless)); // Max Agility
        templates.add(gem("Jeweler", false, delicate[3], quick[3], delicate[3], delicate[3], relentless)); // Agi/Crit
        templates.add(gem("Jeweler", false, delicate[3], quick[3], forceful[3], delicate[3], relentless)); // Agi/Haste
        templates.add(gem("Jeweler", false, bright[3], bright[3], bright[3], bright[3], relentless)); // Max Attack Power
        templates.add(gem("Jeweler", false, bright[3], quick[3], balanced[3], bright[3], relentless)); // AP/Crit
        templates.add(gem("Jeweler", false, bright[3], quick[3], forceful[3], bright[3], relentless)); // AP/Haste
        templates.add(gem("Jeweler", false, precise[3], precise[3], precise[3], precise[3], relentless)); // Max Expertise

        templates.add(gem("Jeweler", false, fractured[3], fractured[3], nightmare[3], fractured[3], relentless)); // Max ArP
        templates.add(gem("Jeweler", false, fractured[3], quick[3], nightmare[3], fractured[3], relentless)); // ArP/Crit
        templates.add(gem("Jeweler", false, fractured[3], quick[3], nightmare[3], fractured[3], relentless)); // ArP/Haste
        templates.add(gem("Jeweler", false, delicate[3], delicate[3], nightmare[3], delicate[3], relentless)); // Max Agility
        templates.add(gem("Jeweler", false, delicate[3], quick[3], nightmare[3], delicate[3], relentless)); // Agi/Crit
        templates.add(gem("Jeweler", false, delicate[3], quick[3], nightmare[3], delicate[3], relentless)); // Agi/Haste
        templates.add(gem("Jeweler", false, bright[3], bright[3], nightmare[3], bright[3], relentless)); // Max Attack Power
        templates.add(gem("Jeweler", false, bright[3], quick[3], nightmare[3], bright[3], relentless)); // AP/Crit
        templates.add(gem("Jeweler", false, bright[3], quick[3], nightmare[3], bright[3], relentless)); // AP/Haste
        templates.add(gem("Jeweler", false, precise[3], precise[3], nightmare[3], precise[3], relentless)); // Max Expertise

        templates.add(gem("Jeweler", false, delicate[2], delicate[3], delicate[3], delicate[2], relentless)); // Agility Heavy
        templates.add(gem("Jeweler", false, bright[2], bright[3], bright[3], bright[2], relentless)); // Attack Power Heavy
        templates.add(gem("Jeweler", false, precise[2], precise[3], precise[3], precise[2], relentless)); // Expertise Heavy
        templates.add(gem("Jeweler", false, precise[3], accurate[3], guardian[3], precise[3], relentless)); // Exp+/Hit
        templates.add(gem("Jeweler", false, accurate[3], rigid[3], vivid[3], rigid[3], relentless)); // Exp/Hit+
        return templates;
    }

    private static GemmingTemplate gem(String group, boolean enabled, int red, int yellow, int blue, int prismatic, int meta) {
        GemmingTemplate t = new GemmingTemplate();
        t.setModel("Rogue");
        t.setGroup(group);
        t.setEnabled(enabled);
        t.setRedId(red);
        t.setYellowId(yellow);
        t.setBlueId(blue);
        t.setPrismaticId(prismatic);
        t.setMetaId(meta);
        return t;
    }

    private String[] characterDisplayCalculationLabels = null;

    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        if (characterDisplayCalculationLabels == null)
            characterDisplayCalculationLabels = new String[] {
                    "Summary:Overall Points*Sum of your DPS Points and Survivability Points.",
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

                    "Complex Stats:Avoided White Attacks",
                    "Complex Stats:Avoided Yellow Attacks",
                    "Complex Stats:Avoided Poison Attacks",
                    "Complex Stats:Crit Chance",
                    "Complex Stats:MainHand Speed",
                    "Complex Stats:OffHand Speed",
                    "Complex Stats:Armor Mitigation MainHand",
                    "Complex Stats:Armor Mitigation OffHand",

                    "Abilities:Optimal Rotation",
                    "Abilities:Optimal Rotation DPS",
                    "Abilities:Custom Rotation DPS",
                    "Abilities:MainHand",
                    "Abilities:OffHand",
                    "Abilities:Backstab",
                    "Abilities:Hemorrhage",
                    "Abilities:Sinister Strike",
                    "Abilities:Mutilate",
                    "Abilities:Slice and Dice",
                    "Abilities:Rupture",
                    "Abilities:Eviscerate",
                    "Abilities:Envenom",
                    "Abilities:Instant Poison",
                    "Abilities:Deadly Poison",
                    "Abilities:Wound Poison",
                    "Abilities:Anesthetic Poison",
            };
        return Arrays.asList(characterDisplayCalculationLabels);
    }

    private String[] optimizableCalculationLabels = null;

    public String[] getOptimizableCalculationLabels() {
        if (optimizableCalculationLabels == null)
            optimizableCalculationLabels = new String[] {
                    "Health",
                    "Avoided Yellow Attacks %",
                    "Avoided Poison Attacks %",
                    "Avoided White Attacks %",
                    "Custom Rotation DPS",
            };
        return optimizableCalculationLabels;
    }

    private String[] customChartNames = null;

    public String[] getCustomChartNames() {
        if (customChartNames == null) {
            customChartNames = new String[] {
                    "Combat Table",
            };
        }
        return customChartNames;
    }

    private LinkedHashMap<String, String> subPointNameColors = null;

    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        if (subPointNameColors == null) {
            subPointNameColors = new LinkedHashMap<String, String>();
            subPointNameColors.put("DPS", "#FF0000");
            subPointNameColors.put("Survivability", "#408020");
        }
        return subPointNameColors;
    }

    @Override
    public String getOverallRatingLabel() {
        return "Overall Points";
    }

    private List<ItemType> relevantItemTypes = null;

    @Override
    public List<ItemType> getRelevantItemTypes() {
        if (relevantItemTypes == null) {
            relevantItemTypes = new ArrayList<ItemType>(Arrays.asList(
                    ItemType.None,
                    ItemType.Leather,
                    ItemType.Bow,
                    ItemType.Crossbow,
                    ItemType.Gun,
                    ItemType.Thrown,
                    ItemType.Dagger,
                    ItemType.FistWeapon,
                    ItemType.OneHandAxe,
                    ItemType.OneHandMace,
                    ItemType.OneHandSword));
        }
        return relevantItemTypes;
    }

    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.Rogue;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationsRogue();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsRogue();
    }

    public boolean ptrMode = false;

    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            org.w3c.dom.Element root = document.getDocumentElement();
            CalculationOptionsRogue calcOpts = new CalculationOptionsRogue();
            for (org.w3c.dom.Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                org.w3c.dom.Element child = (org.w3c.dom.Element) node;
                String name = child.getTagName();
                String text = child.getTextContent().trim();
                switch (name) {
                    case "TargetLevel": calcOpts.setTargetLevel(Integer.parseInt(text)); break;
                    case "TargetArmor": calcOpts.setTargetArmor(Integer.parseInt(text)); break;
                    case "TargetPoisonable": calcOpts.setTargetPoisonable(Boolean.parseBoolean(text)); break;
                    case "BleedIsUp": calcOpts.setBleedIsUp(Boolean.parseBoolean(text)); break;
                    case "Duration": calcOpts.setDuration(Integer.parseInt(text)); break;
                    case "LagVariance": calcOpts.setLagVariance(Integer.parseInt(text)); break;
                    case "CustomCPG": calcOpts.setCustomCPG(Integer.parseInt(text)); break;
                    case "ForceCustom": calcOpts.setForceCustom(Boolean.parseBoolean(text)); break;
                    case "CustomUseRupt": calcOpts.setCustomUseRupt(Boolean.parseBoolean(text)); break;
                    case "CustomUseExpose": calcOpts.setCustomUseExpose(Boolean.parseBoolean(text)); break;
                    case "CustomUseTotT": calcOpts.setCustomUseTotT(Boolean.parseBoolean(text)); break;
                    case "CustomFinisher": calcOpts.setCustomFinisher(Integer.parseInt(text)); break;
                    case "CustomCPFinisher": calcOpts.setCustomCPFinisher(Integer.parseInt(text)); break;
                    case "CustomCPSnD": calcOpts.setCustomCPSnD(Integer.parseInt(text)); break;
                    case "CustomMHPoison": calcOpts.setCustomMHPoison(Integer.parseInt(text)); break;
                    case "CustomOHPoison": calcOpts.setCustomOHPoison(Integer.parseInt(text)); break;
                    case "EnableMuti": calcOpts.setEnableMuti(Boolean.parseBoolean(text)); break;
                    case "EnableSS": calcOpts.setEnableSS(Boolean.parseBoolean(text)); break;
                    case "EnableBS": calcOpts.setEnableBS(Boolean.parseBoolean(text)); break;
                    case "EnableHemo": calcOpts.setEnableHemo(Boolean.parseBoolean(text)); break;
                    case "EnableIP": calcOpts.setEnableIP(Boolean.parseBoolean(text)); break;
                    case "EnableDP": calcOpts.setEnableDP(Boolean.parseBoolean(text)); break;
                    case "EnableWP": calcOpts.setEnableWP(Boolean.parseBoolean(text)); break;
                    case "EnableRupt": calcOpts.setEnableRupt(Boolean.parseBoolean(text)); break;
                    case "EnableEvis": calcOpts.setEnableEvis(Boolean.parseBoolean(text)); break;
                    case "EnableEnvenom": calcOpts.setEnableEnvenom(Boolean.parseBoolean(text)); break;
                    case "TrinketOffset": calcOpts.setTrinketOffset(Float.parseFloat(text)); break;
                    case "PTRMode": calcOpts.setPTRMode(Boolean.parseBoolean(text)); break;
                    default: break;
                }
            }
            return calcOpts;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem,
            boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        ICalculationOptionBase options = character.getCalculationOptions();
        CalculationOptionsRogue calcOpts = options instanceof CalculationOptionsRogue ? (CalculationOptionsRogue) options : null;
        if (calcOpts == null) calcOpts = new CalculationOptionsRogue();
        ptrMode = calcOpts.getPTRMode();
        int targetLevel = calcOpts.getTargetLevel();
        float targetArmor = calcOpts.getTargetArmor();
        boolean targetPoisonable = calcOpts.getTargetPoisonable();
        boolean maintainBleed = false;
        if (referenceCalculation && significantChange) {
            // Fresh top-level character calculation: drop shared effect interpolation caches so a
            // previous character cannot leak into this one (DECISIONS #28).
            SpecialEffect.clearInterpolatorCaches();
        }
        TemporaryEffects temp = getCharacterStatsWithTemporaryEffects(character, additionalItem);
        Stats stats = temp.stats;
        WeightedStat[] arPenUptimes = temp.armorPenetrationUptimes;
        WeightedStat[] critRatingUptimes = temp.critRatingUptimes;
        float levelDifference = (targetLevel - 80f) * 0.2f;
        CharacterCalculationsRogue calculatedStats = new CharacterCalculationsRogue();
        calculatedStats.basicStats = stats;
        calculatedStats.targetLevel = targetLevel;
        Item mainHand = character.get(CharacterSlot.MainHand) == null ? new Knuckles() : character.get(CharacterSlot.MainHand).getItem();
        Item offHand = character.get(CharacterSlot.OffHand) == null ? new Knuckles() : character.get(CharacterSlot.OffHand).getItem();

        // region Basic Chances and Constants
        // region Constants from talents
        float ambushBackstabCostReduction = 4 * character.getRogueTalents().getSlaughterFromTheShadows();
        float armorReduction = 0.03f * character.getRogueTalents().getSerratedBlades();
        float bonusBackstabCrit = 0.1f * character.getRogueTalents().getPuncturingWounds();
        float bonusBackstabDamageMultiplier = 0.03f * character.getRogueTalents().getAggression() + 0.05f * character.getRogueTalents().getBladeTwisting() + 0.1f * character.getRogueTalents().getSurpriseAttacks() + 0.1f * character.getRogueTalents().getOpportunity() + 0.02f * character.getRogueTalents().getSinisterCalling();
        float bonusCPGCritDamageMultiplier = 0.06f * character.getRogueTalents().getLethality();
        float bonusCPGCrit = 0.02f * character.getRogueTalents().getTurnTheTables();
        float bonusEnvenomDamageMultiplier = 0.07f * character.getRogueTalents().getVilePoisons();
        float bonusEvisCrit = character.getRogueTalents().isGlyphOfEviscerate() ? 0.1f : 0f;
        float bonusEvisDamageMultiplier = (character.getRogueTalents().getImprovedEviscerate() == 3 ? 0.2f : 0.07f * character.getRogueTalents().getImprovedEviscerate()) + 0.03f * character.getRogueTalents().getAggression();
        float bonusFlurryHaste = 0.2f * character.getRogueTalents().getBladeFlurry();
        float bonusHemoDamageMultiplier = 0.1f * character.getRogueTalents().getSurpriseAttacks() + 0.02f * character.getRogueTalents().getSinisterCalling();
        float bonusMaceArP = 0.03f * character.getRogueTalents().getMaceSpecialization();
        float bonusMainHandCrit = (character.get(CharacterSlot.MainHand) != null) ? ((character.get(CharacterSlot.MainHand).getItem().getType() == ItemType.Dagger || character.get(CharacterSlot.MainHand).getItem().getType() == ItemType.FistWeapon) ? 0.01f * character.getRogueTalents().getCloseQuartersCombat() : 0f) : 0f;
        float bonusMutiCrit = 0.05f * character.getRogueTalents().getPuncturingWounds();
        float bonusMutiDamageMultiplier = 0.1f * character.getRogueTalents().getOpportunity();
        float bonusOffHandCrit = (character.get(CharacterSlot.OffHand) != null) ? ((character.get(CharacterSlot.OffHand).getItem().getType() == ItemType.Dagger || character.get(CharacterSlot.OffHand).getItem().getType() == ItemType.FistWeapon) ? 0.01f * character.getRogueTalents().getCloseQuartersCombat() : 0f) : 0f;
        float bonusOffHandDamageMultiplier = 0.1f * character.getRogueTalents().getDualWieldSpecialization();
        float bonusPoisonDamageMultiplier = character.getRogueTalents().getVilePoisons() == 3 ? 0.2f : 0.07f * character.getRogueTalents().getVilePoisons();
        float bonusRuptDamageMultiplier = 0.15f * character.getRogueTalents().getBloodSpatter() + 0.1f * character.getRogueTalents().getSerratedBlades();
        float bonusRuptDuration = character.getRogueTalents().isGlyphOfRupture() ? 4 : 0;
        float bonusSnDDuration = character.getRogueTalents().isGlyphOfSliceandDice() ? 3 : 0;
        float bonusSnDDurationMultiplier = 0.25f * character.getRogueTalents().getImprovedSliceAndDice();
        float bonusSStrikeDamageMultiplier = 0.03f * character.getRogueTalents().getAggression() + 0.05f * character.getRogueTalents().getBladeTwisting() + 0.1f * character.getRogueTalents().getSurpriseAttacks();
        float bonusYellowDamageMultiplier = 0.02f * character.getRogueTalents().getFindWeakness() + 0.35f * 0.2f * character.getRogueTalents().getDirtyDeeds();
        float chanceOnEnergyOnCrit = 2f * (character.getRogueTalents().getFocusedAttacks() > 2 ? 1f : (0.33f * character.getRogueTalents().getFocusedAttacks()));
        float garrCostReduction = 10 * character.getRogueTalents().getDirtyDeeds();
        float hemoCostReduction = 1 * character.getRogueTalents().getSlaughterFromTheShadows();
        float mutiCostReduction = character.getRogueTalents().isGlyphOfMutilate() ? 5 : 0;
        float sStrikeCostReduction = 3 * character.getRogueTalents().getImprovedSinisterStrike();
        // endregion

        float modArmor = 0f;
        for (int i = 0; i < arPenUptimes.length; i++) {
            modArmor += arPenUptimes[i].chance() * StatConversion.getArmorDamageReduction(character.getLevel(), calcOpts.getTargetArmor(), 1f - (1f - stats.getArmorPenetration()) * (1f - armorReduction) * (mainHand.getType() == ItemType.OneHandMace ? 1f - bonusMaceArP : 1), 0f, stats.getArmorPenetrationRating() + arPenUptimes[i].value());
        }
        float mainHandModArmor = 1f - modArmor;
        modArmor = 0f;
        for (int i = 0; i < arPenUptimes.length; i++) {
            modArmor += arPenUptimes[i].chance() * StatConversion.getArmorDamageReduction(character.getLevel(), calcOpts.getTargetArmor(), 1f - (1f - stats.getArmorPenetration()) * (1f - armorReduction) * (offHand.getType() == ItemType.OneHandMace ? 1f - bonusMaceArP : 1), 0f, stats.getArmorPenetrationRating() + arPenUptimes[i].value());
        }
        float offHandModArmor = 1f - modArmor;
        float critMultiplier = 2f * (1f + stats.getBonusCritMultiplier());
        float critMultiplierBleed = 2f * (1f + stats.getBonusCritMultiplier());
        float critMultiplierPoison = 1.5f * (1f + stats.getBonusCritMultiplier());
        float hasteBonus = StatConversion.getPhysicalHasteFromRating(stats.getHasteRating(), CharacterClass.Rogue);
        hasteBonus = (1f + hasteBonus) * (1f + bonusFlurryHaste * 15f / 120f) - 1f;
        float speedModifier = 1f / (1f + hasteBonus) / (1f + stats.getPhysicalHaste());
        float mainHandSpeed = mainHand == null ? 0f : mainHand.getSpeed() * speedModifier;
        float offHandSpeed = offHand == null ? 0f : offHand.getSpeed() * speedModifier;

        float mainHandSpeedNorm = mainHand.getType() == ItemType.Dagger ? 1.7f : 2.4f;
        float offHandSpeedNorm = mainHand.getType() == ItemType.Dagger ? 1.7f : 2.4f;

        float hitBonus = stats.getPhysicalHit() + StatConversion.getPhysicalHitFromRating(stats.getHitRating(), CharacterClass.Rogue);
        float spellHitBonus = stats.getSpellHit() + StatConversion.getSpellHitFromRating(stats.getHitRating(), CharacterClass.Rogue);
        float expertiseMHBonus = ((character.getRace() == CharacterRace.Human && (mainHand.getType() == ItemType.OneHandSword || mainHand.getType() == ItemType.OneHandMace)) ? 3 : 0) +
                ((character.getRace() == CharacterRace.Dwarf && (mainHand.getType() == ItemType.OneHandMace)) ? 5 : 0) +
                ((character.getRace() == CharacterRace.Orc && (mainHand.getType() == ItemType.OneHandAxe || mainHand.getType() == ItemType.FistWeapon)) ? 5 : 0);
        float expertiseOHBonus = ((character.getRace() == CharacterRace.Human && (offHand.getType() == ItemType.OneHandSword || offHand.getType() == ItemType.OneHandMace)) ? 3 : 0) +
                ((character.getRace() == CharacterRace.Dwarf && (offHand.getType() == ItemType.OneHandMace)) ? 5 : 0) +
                ((character.getRace() == CharacterRace.Orc && (offHand.getType() == ItemType.OneHandAxe || offHand.getType() == ItemType.FistWeapon)) ? 5 : 0);
        expertiseMHBonus = StatConversion.getDodgeParryReducFromExpertise(StatConversion.getExpertiseFromRating(stats.getExpertiseRating(), CharacterClass.Rogue) + stats.getExpertise() + expertiseMHBonus, CharacterClass.Rogue);
        expertiseOHBonus = StatConversion.getDodgeParryReducFromExpertise(StatConversion.getExpertiseFromRating(stats.getExpertiseRating(), CharacterClass.Rogue) + stats.getExpertise() + expertiseOHBonus, CharacterClass.Rogue);

        float chanceMHDodge = Math.max(0f, StatConversion.WHITE_DODGE_CHANCE_CAP[targetLevel - 80] - expertiseMHBonus);
        float chanceOHDodge = Math.max(0f, StatConversion.WHITE_DODGE_CHANCE_CAP[targetLevel - 80] - expertiseOHBonus);
        float chanceParry = 0f; //Math.Max(0f, StatConversion.WHITE_PARRY_CHANCE_CAP[targetLevel - 80] - expertiseBonus);
        float chanceWhiteMiss = Math.max(0f, StatConversion.WHITE_MISS_CHANCE_CAP_DW[targetLevel - 80] - hitBonus);
        float chanceMiss = Math.max(0f, StatConversion.YELLOW_MISS_CHANCE_CAP[targetLevel - 80] - hitBonus);
        float chancePoisonMiss = Math.max(0f, StatConversion.getSpellMiss(80 - targetLevel, false) - spellHitBonus);

        float glanceMultiplier = 0.75f;
        float chanceWhiteMHAvoided = chanceWhiteMiss + chanceMHDodge + chanceParry;
        float chanceWhiteOHAvoided = chanceWhiteMiss + chanceOHDodge + chanceParry;
        float chanceMHAvoided = chanceMiss + chanceMHDodge + chanceParry;
        float chanceOHAvoided = chanceMiss + chanceOHDodge + chanceParry;
        float chanceFinisherAvoided = chanceMiss + chanceMHDodge * (1f - character.getRogueTalents().getSurpriseAttacks()) + chanceParry;
        float chancePoisonAvoided = chancePoisonMiss;
        float chanceWhiteMHNonAvoided = 1f - chanceWhiteMHAvoided;
        float chanceWhiteOHNonAvoided = 1f - chanceWhiteOHAvoided;
        float chanceMHNonAvoided = 1f - chanceMHAvoided;
        float chanceOHNonAvoided = 1f - chanceOHAvoided;
        float chancePoisonNonAvoided = 1f - chancePoisonAvoided;

        ////Crit Chances
        float chanceCritYellow = 0f;
        float chanceHitYellow = 0f;
        float chanceCritBackstab = 0f;
        float chanceHitBackstab = 0f;
        float cpPerBackstab = 0f;
        float chanceCritMuti = 0f;
        float chanceHitMuti = 0f;
        float cpPerMuti = 0f;
        float chanceCritSStrike = 0f;
        float chanceHitSStrike = 0f;
        float cpPerSStrike = 0f;
        float chanceCritHemo = 0f;
        float chanceHitHemo = 0f;
        float cpPerHemo = 0f;
        float chanceCritEvis = 0f;
        float chanceHitEvis = 0f;
        //float chanceCritBleed = 0f;
        float chanceGlance = StatConversion.WHITE_GLANCE_CHANCE_CAP[targetLevel - 80];
        float chanceCritWhiteMainTotal = 0f;
        float chanceCritWhiteMain = 0f;
        float chanceHitWhiteMain = 0f;
        float chanceCritWhiteOffTotal = 0f;
        float chanceCritWhiteOff = 0f;
        float chanceHitWhiteOff = 0f;
        float chanceCritPoison = 0f;
        float chanceHitPoison = 0f;

        for (int i = 0; i < critRatingUptimes.length; i++) { //Sum up the weighted chances for each crit value
            WeightedStat iStat = critRatingUptimes[i];
            //Yellow - 2 Roll, so total of X chance to avoid, total of 1 chance to crit and hit when not avoided
            float chanceCritYellowTemp = Math.min(1f, StatConversion.getCritFromRating(stats.getCritRating() + iStat.value(), CharacterClass.Rogue)
                    + StatConversion.getCritFromAgility(stats.getAgility() - 10f, CharacterClass.Rogue)
                    + stats.getPhysicalCrit()
                    + StatConversion.NPC_LEVEL_CRIT_MOD[targetLevel - 80]
                    + bonusMainHandCrit);
            float chanceHitYellowTemp = 1f - chanceCritYellowTemp;
            float chanceCritPoisonTemp = Math.min(1f, StatConversion.getSpellCritFromRating(stats.getCritRating() + iStat.value())
                    + stats.getSpellCrit()
                    + stats.getSpellCritOnTarget()
                    + StatConversion.NPC_LEVEL_CRIT_MOD[targetLevel - 80]);
            float chanceHitPoisonTemp = 1f - chanceCritPoisonTemp;

            //Backstab - Identical to Yellow, with higher crit chance
            float chanceCritBackstabTemp = Math.min(1f, chanceCritYellowTemp + bonusBackstabCrit + stats.getBonusCPGCritChance() + bonusCPGCrit);
            float chanceHitBackstabTemp = 1f - chanceCritBackstabTemp;
            float cpPerBackstabTemp = (chanceHitBackstabTemp + chanceCritBackstabTemp * (1f + 0.2f * character.getRogueTalents().getSealFate()));

            //Mutilate - Identical to Yellow, with higher crit chance
            float chanceCritMutiTemp = Math.min(1f, chanceCritYellowTemp + bonusMutiCrit + stats.getBonusCPGCritChance() + bonusCPGCrit);
            float chanceHitMutiTemp = 1f - chanceCritMutiTemp;
            float cpPerMutiTemp = (1 + chanceHitMutiTemp * chanceHitMutiTemp + (1 - chanceHitMutiTemp * chanceHitMutiTemp) * (1f + 0.2f * character.getRogueTalents().getSealFate()));

            //Sinister Strike - Identical to Yellow, with higher crit chance
            float chanceCritSStrikeTemp = Math.min(1f, chanceCritYellowTemp + stats.getBonusCPGCritChance() + bonusCPGCrit);
            float chanceHitSStrikeTemp = 1f - chanceCritSStrikeTemp;
            float cpPerSStrikeTemp = (chanceHitSStrikeTemp + chanceCritSStrikeTemp * (1f + 0.2f * character.getRogueTalents().getSealFate() + (character.getRogueTalents().isGlyphOfSinisterStrike() ? 0.5f : 0f)));

            //Hemorrhage - Identical to Yellow, with higher crit chance
            float chanceCritHemoTemp = Math.min(1f, chanceCritYellowTemp + stats.getBonusCPGCritChance() + bonusCPGCrit);
            float chanceHitHemoTemp = 1f - chanceCritHemoTemp;
            float cpPerHemoTemp = (chanceHitHemoTemp + chanceCritHemoTemp * (1f + 0.2f * character.getRogueTalents().getSealFate()));

            //Eviscerate - Identical to Yellow, with higher crit chance
            float chanceCritEvisTemp = Math.min(1f, chanceCritYellowTemp + bonusEvisCrit);
            float chanceHitEvisTemp = 1f - chanceCritEvisTemp;

            //White Mainhand
            float chanceCritWhiteMainTotalTemp = chanceCritYellowTemp;
            float chanceCritWhiteMainTemp = Math.min(chanceCritYellowTemp, 1f - chanceGlance - chanceWhiteMHAvoided);
            float chanceHitWhiteMainTemp = 1f - chanceCritWhiteMainTemp - chanceWhiteMHAvoided - chanceGlance;
            //White Offhand
            float chanceCritWhiteOffTotalTemp = chanceCritYellowTemp - bonusMainHandCrit + bonusOffHandCrit;
            float chanceCritWhiteOffTemp = Math.min(chanceCritYellowTemp - bonusMainHandCrit + bonusOffHandCrit, 1f - chanceGlance - chanceWhiteOHAvoided);
            float chanceHitWhiteOffTemp = 1f - chanceCritWhiteOffTemp - chanceWhiteOHAvoided - chanceGlance;

            chanceCritYellow += iStat.chance() * chanceCritYellowTemp;
            chanceHitYellow += iStat.chance() * chanceHitYellowTemp;
            chanceCritBackstab += iStat.chance() * chanceCritBackstabTemp;
            chanceHitBackstab += iStat.chance() * chanceHitBackstabTemp;
            cpPerBackstab += iStat.chance() * cpPerBackstabTemp;
            chanceCritMuti += iStat.chance() * chanceCritMutiTemp;
            chanceHitMuti += iStat.chance() * chanceHitMutiTemp;
            cpPerMuti += iStat.chance() * cpPerMutiTemp;
            chanceCritSStrike += iStat.chance() * chanceCritSStrikeTemp;
            chanceHitSStrike += iStat.chance() * chanceHitSStrikeTemp;
            cpPerSStrike += iStat.chance() * cpPerSStrikeTemp;
            chanceCritHemo += iStat.chance() * chanceCritHemoTemp;
            chanceHitHemo += iStat.chance() * chanceHitHemoTemp;
            cpPerHemo += iStat.chance() * cpPerHemoTemp;
            chanceCritEvis += iStat.chance() * chanceCritEvisTemp;
            chanceHitEvis += iStat.chance() * chanceHitEvisTemp;
            chanceCritWhiteMainTotal += iStat.chance() * chanceCritWhiteMainTotalTemp;
            chanceCritWhiteMain += iStat.chance() * chanceCritWhiteMainTemp;
            chanceHitWhiteMain += iStat.chance() * chanceHitWhiteMainTemp;
            chanceCritWhiteOffTotal += iStat.chance() * chanceCritWhiteOffTotalTemp;
            chanceCritWhiteOff += iStat.chance() * chanceCritWhiteOffTemp;
            chanceHitWhiteOff += iStat.chance() * chanceHitWhiteOffTemp;
            chanceCritPoison += iStat.chance() * chanceCritPoisonTemp;
            chanceHitPoison += iStat.chance() * chanceHitPoisonTemp;
        }

        calculatedStats.dodgedMHAttacks = chanceMHDodge * 100f;
        calculatedStats.dodgedOHAttacks = chanceOHDodge * 100f;
        calculatedStats.parriedAttacks = chanceParry * 100f;
        calculatedStats.missedWhiteAttacks = chanceWhiteMiss * 100f;
        calculatedStats.missedAttacks = chanceMiss * 100f;
        calculatedStats.missedPoisonAttacks = chancePoisonMiss * 100f;

        float timeToReapplyDebuffs = 1f / (1f - chanceMHAvoided) - 1f;
        float lagVariance = (float) calcOpts.getLagVariance() / 1000f;
        float ruptDurationUptime = 16f + bonusRuptDuration;
        float ruptDurationAverage = ruptDurationUptime + timeToReapplyDebuffs + lagVariance;
        float snDBonusDuration = bonusSnDDuration - lagVariance;
        // endregion

        // region Attack Damages
        float baseDamage = (mainHand == null ? 0f : mainHand.getSpeed()) * stats.getAttackPower() / 14f + stats.getWeaponDamage() + (mainHand.getMinDamage() + mainHand.getMaxDamage()) / 2f;
        float baseDamageNorm = mainHandSpeedNorm * stats.getAttackPower() / 14f + stats.getWeaponDamage() + (mainHand.getMinDamage() + mainHand.getMaxDamage()) / 2f;
        float baseOffDamage = (((offHand == null ? 0f : offHand.getSpeed()) * stats.getAttackPower() / 14f + stats.getWeaponDamage() + (offHand.getMinDamage() + offHand.getMaxDamage()) / 2f)) * (0.5f * (1f + bonusOffHandDamageMultiplier));
        float baseOffDamageNorm = (offHandSpeedNorm * stats.getAttackPower() / 14f + stats.getWeaponDamage() + (offHand.getMinDamage() + offHand.getMaxDamage()) / 2f) * (0.5f * (1f + bonusOffHandDamageMultiplier));
        float meleeDamageRaw = baseDamage * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * mainHandModArmor;
        float meleeOffDamageRaw = baseOffDamage * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * offHandModArmor;
        float backstabDamageRaw = (baseDamageNorm * 1.5f + 465f) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + bonusBackstabDamageMultiplier + bonusYellowDamageMultiplier) * mainHandModArmor;
        backstabDamageRaw *= (mainHand.getType() == ItemType.Dagger ? 1f : 0f);
        float hemoDamageRaw = (baseDamageNorm * 1.1f) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + bonusHemoDamageMultiplier + bonusYellowDamageMultiplier) * mainHandModArmor;
        hemoDamageRaw *= (mainHand.getType() == ItemType.Dagger ? 1.6f / 1.1f : 1f);
        hemoDamageRaw *= character.getRogueTalents().getHemorrhage() > 0 ? 1f : 0f;
        float sStrikeDamageRaw = (baseDamageNorm * 1f + 180f) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + bonusSStrikeDamageMultiplier + bonusYellowDamageMultiplier) * mainHandModArmor;
        float mutiDamageMainRaw = (baseDamageNorm * 1f + 181f) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + bonusMutiDamageMultiplier + bonusYellowDamageMultiplier) * (1f + (targetPoisonable ? 0.2f : 0f)) * mainHandModArmor;
        float mutiDamageOffRaw = (baseOffDamageNorm * 1f + 181f * (1f + bonusOffHandDamageMultiplier)) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + bonusMutiDamageMultiplier + bonusYellowDamageMultiplier) * (1f + (targetPoisonable ? 0.2f : 0f)) * offHandModArmor;
        mutiDamageMainRaw *= (character.getRogueTalents().getMutilate() > 0 && mainHand.getType() == ItemType.Dagger && offHand.getType() == ItemType.Dagger ? 1f : 0f);
        mutiDamageOffRaw *= (character.getRogueTalents().getMutilate() > 0 && mainHand.getType() == ItemType.Dagger && offHand.getType() == ItemType.Dagger ? 1f : 0f);
        float mutiDamageRaw = mutiDamageMainRaw + mutiDamageOffRaw;
        float ruptDamageRaw = (127f + 18f * 5f + 0.06f * stats.getAttackPower() * 5f / (3f + 5f)) * (3f + 5f + bonusRuptDuration / 2f) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + bonusRuptDamageMultiplier + bonusYellowDamageMultiplier) * (1f + stats.getBonusBleedDamageMultiplier());
        float evisBaseDamageRaw = (127f + 381f) / 2f * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + bonusEvisDamageMultiplier + bonusYellowDamageMultiplier) * mainHandModArmor;
        float evisCPDamageRaw = (370f + stats.getAttackPower() * 0.07f) * (1f + stats.getBonusPhysicalDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + bonusEvisDamageMultiplier + bonusYellowDamageMultiplier) * mainHandModArmor;
        float envenomBaseDamageRaw = 0f;
        float envenomCPDamageRaw = (215f + stats.getAttackPower() * 0.09f) * (1f + stats.getBonusNatureDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) * (1f + bonusEnvenomDamageMultiplier + bonusYellowDamageMultiplier);
        float iPDamageRaw = ((300f + 400f) / 2f + stats.getAttackPower() * 0.09f) * (1f + bonusPoisonDamageMultiplier) * (1f + stats.getBonusNatureDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier());
        float dPDamageRaw = (296f + stats.getAttackPower() * 0.108f) * (1f + bonusPoisonDamageMultiplier) * (1f + stats.getBonusNatureDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier()) / 4;
        float wPDamageRaw = ((78f + 231f) / 2f + stats.getAttackPower() * 0.036f) * (1f + bonusPoisonDamageMultiplier) * (1f + stats.getBonusNatureDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier());
        float aPDamageRaw = ((218f + 280f) / 2f) + (1f + bonusPoisonDamageMultiplier) * (1f + stats.getBonusNatureDamageMultiplier()) * (1f + stats.getBonusDamageMultiplier());

        float meleeDamageAverage = (chanceGlance * glanceMultiplier + chanceCritWhiteMain * critMultiplier + chanceHitWhiteMain) * meleeDamageRaw;
        float meleeOffDamageAverage = (chanceGlance * glanceMultiplier + chanceCritWhiteOff * critMultiplier + chanceHitWhiteOff) * meleeOffDamageRaw;
        float backstabDamageAverage = (1f - chanceCritBackstab) * backstabDamageRaw + chanceCritBackstab * backstabDamageRaw * (critMultiplier + bonusCPGCritDamageMultiplier);
        float hemoDamageAverage = (1f - chanceCritHemo) * hemoDamageRaw + chanceCritHemo * hemoDamageRaw * (critMultiplier + bonusCPGCritDamageMultiplier);
        float sStrikeDamageAverage = (1f - chanceCritSStrike) * sStrikeDamageRaw + chanceCritSStrike * sStrikeDamageRaw * (critMultiplier + bonusCPGCritDamageMultiplier);
        float mutiDamageAverage = chanceHitMuti * chanceHitMuti * mutiDamageRaw +
                chanceCritMuti * chanceHitMuti * (mutiDamageMainRaw * (critMultiplier + bonusCPGCritDamageMultiplier) + mutiDamageOffRaw) +
                chanceHitMuti * chanceCritMuti * (mutiDamageMainRaw + mutiDamageOffRaw * (critMultiplier + bonusCPGCritDamageMultiplier)) +
                chanceCritMuti * chanceCritMuti * (mutiDamageMainRaw + mutiDamageOffRaw) * (critMultiplier + bonusCPGCritDamageMultiplier);
        float ruptDamageAverage = ((1f - chanceCritYellow) * ruptDamageRaw + chanceCritYellow * ruptDamageRaw * critMultiplierBleed);
        float evisBaseDamageAverage = (1f - chanceCritEvis) * evisBaseDamageRaw + chanceCritEvis * evisBaseDamageRaw * critMultiplier;
        float evisCPDamageAverage = (1f - chanceCritEvis) * evisCPDamageRaw + chanceCritEvis * evisCPDamageRaw * critMultiplier;
        float envenomBaseDamageAverage = (1f - chanceCritYellow) * envenomBaseDamageRaw + chanceCritYellow * envenomBaseDamageRaw * critMultiplier;
        float envenomCPDamageAverage = (1f - chanceCritYellow) * envenomCPDamageRaw + chanceCritYellow * envenomCPDamageRaw * critMultiplier;
        float iPDamageAverage = (1f - chanceCritPoison) * iPDamageRaw + chanceCritPoison * iPDamageRaw * critMultiplierPoison;
        float dPDamageAverage = dPDamageRaw;
        float wPDamageAverage = (1f - chanceCritPoison) * wPDamageRaw + chanceCritPoison * wPDamageRaw * critMultiplierPoison;
        float aPDamageAverage = (1f - chanceCritPoison) * aPDamageRaw + chanceCritPoison * aPDamageRaw * critMultiplierPoison;
        // endregion

        // region Energy Costs
        float ambushEnergyRaw = 60f - ambushBackstabCostReduction - chanceOnEnergyOnCrit * chanceCritYellow;
        float garrEnergyRaw = 50f - garrCostReduction;
        float backstabEnergyRaw = 60f - ambushBackstabCostReduction - chanceOnEnergyOnCrit * chanceCritBackstab;
        float hemoEnergyRaw = 35f - hemoCostReduction - chanceOnEnergyOnCrit * chanceCritHemo;
        float sStrikeEnergyRaw = 45f - sStrikeCostReduction - chanceOnEnergyOnCrit * chanceCritSStrike;
        float mutiEnergyRaw = 60f - mutiCostReduction - 2 * chanceOnEnergyOnCrit * chanceCritMuti;
        float ruptEnergyRaw = 25f;
        float evisEnergyRaw = 35f - chanceOnEnergyOnCrit * chanceCritEvis;
        float envenomEnergyRaw = 35f - chanceOnEnergyOnCrit * chanceCritYellow;
        float snDEnergyRaw = 25f;

        //[rawCost + ((1/chance_to_land) - 1) * rawCost/5]
        float cpgEnergyCostMultiplier = 1f + ((1f / chanceMHNonAvoided) - 1f) * 0.2f;
        float finisherEnergyCostMultiplier = 1f + ((1f / chanceMHNonAvoided) - 1f) * (1f - 0.4f * character.getRogueTalents().getQuickRecovery());
        float backstabEnergyAverage = backstabEnergyRaw * cpgEnergyCostMultiplier;
        float hemoEnergyAverage = hemoEnergyRaw * cpgEnergyCostMultiplier;
        float sStrikeEnergyAverage = sStrikeEnergyRaw * cpgEnergyCostMultiplier;
        float mutiEnergyAverage = mutiEnergyRaw * cpgEnergyCostMultiplier;
        float ruptEnergyAverage = ruptEnergyRaw * finisherEnergyCostMultiplier;
        float evisEnergyAverage = evisEnergyRaw * finisherEnergyCostMultiplier;
        float envenomEnergyAverage = envenomEnergyRaw * finisherEnergyCostMultiplier;
        float snDEnergyAverage = snDEnergyRaw * finisherEnergyCostMultiplier;
        // endregion

        // region Ability Stats
        RogueAbilityStats mainHandStats = new RogueMHStats();
        mainHandStats.setDamagePerHit(meleeDamageRaw);
        mainHandStats.setDamagePerSwing(meleeDamageAverage);
        mainHandStats.setWeapon(mainHand);
        mainHandStats.setCritChance(chanceCritWhiteMain);
        RogueAbilityStats offHandStats = new RogueOHStats();
        offHandStats.setDamagePerHit(meleeOffDamageRaw);
        offHandStats.setDamagePerSwing(meleeOffDamageAverage);
        offHandStats.setWeapon(offHand);
        offHandStats.setCritChance(chanceCritWhiteOff);
        RogueAbilityStats backstabStats = new RogueBackstabStats();
        backstabStats.setDamagePerHit(backstabDamageRaw);
        backstabStats.setDamagePerSwing(backstabDamageAverage);
        backstabStats.setEnergyCost(backstabEnergyAverage);
        backstabStats.setCritChance(chanceCritBackstab);
        backstabStats.setCPPerSwing(cpPerBackstab);
        RogueAbilityStats hemoStats = new RogueHemoStats();
        hemoStats.setDamagePerHit(hemoDamageRaw);
        hemoStats.setDamagePerSwing(hemoDamageAverage);
        hemoStats.setEnergyCost(hemoEnergyAverage);
        hemoStats.setCritChance(chanceCritYellow);
        hemoStats.setCPPerSwing(cpPerHemo);
        RogueAbilityStats sStrikeStats = new RogueSStrikeStats();
        sStrikeStats.setDamagePerHit(sStrikeDamageRaw);
        sStrikeStats.setDamagePerSwing(sStrikeDamageAverage);
        sStrikeStats.setEnergyCost(sStrikeEnergyAverage);
        sStrikeStats.setCritChance(chanceCritYellow);
        sStrikeStats.setCPPerSwing(cpPerSStrike);
        RogueAbilityStats mutiStats = new RogueMutiStats();
        mutiStats.setDamagePerHit(mutiDamageRaw);
        mutiStats.setDamagePerSwing(mutiDamageAverage);
        mutiStats.setEnergyCost(mutiEnergyAverage);
        mutiStats.setCritChance(chanceCritMuti);
        mutiStats.setCPPerSwing(cpPerMuti);
        RogueAbilityStats ruptStats = new RogueRuptStats();
        ruptStats.setDamagePerHit(ruptDamageRaw);
        ruptStats.setDamagePerSwing(ruptDamageAverage);
        ruptStats.setDurationUptime(ruptDurationUptime);
        ruptStats.setDurationAverage(ruptDurationAverage);
        ruptStats.setDurationPerCP(2f);
        ruptStats.setEnergyCost(ruptEnergyAverage);
        RogueAbilityStats evisStats = new RogueEvisStats();
        evisStats.setDamagePerHit(evisBaseDamageRaw);
        evisStats.setDamagePerSwing(evisBaseDamageAverage);
        evisStats.setDamagePerHitPerCP(evisCPDamageRaw);
        evisStats.setDamagePerSwingPerCP(evisCPDamageAverage);
        evisStats.setEnergyCost(evisEnergyAverage);
        evisStats.setCritChance(chanceCritEvis);
        RogueAbilityStats envenomStats = new RogueEnvenomStats();
        envenomStats.setDamagePerHit(envenomBaseDamageRaw);
        envenomStats.setDamagePerSwing(envenomBaseDamageAverage);
        envenomStats.setDamagePerHitPerCP(envenomCPDamageRaw);
        envenomStats.setDamagePerSwingPerCP(envenomCPDamageAverage);
        envenomStats.setEnergyCost(envenomEnergyAverage);
        envenomStats.setCritChance(chanceCritYellow);
        RogueAbilityStats snDStats = new RogueSnDStats();
        snDStats.setDurationUptime(snDBonusDuration * (1f + bonusSnDDurationMultiplier));
        snDStats.setDurationAverage((6f + snDBonusDuration) * (1f + bonusSnDDurationMultiplier));
        snDStats.setEnergyCost(snDEnergyAverage);
        snDStats.setDurationPerCP(3f);
        RogueAbilityStats iPStats = new RogueIPStats();
        iPStats.setDamagePerHit(iPDamageRaw);
        iPStats.setDamagePerSwing(iPDamageAverage);
        RogueAbilityStats dPStats = new RogueDPStats();
        dPStats.setDamagePerHit(dPDamageRaw);
        dPStats.setDamagePerSwing(dPDamageAverage);
        RogueAbilityStats wPStats = new RogueWPStats();
        wPStats.setDamagePerHit(wPDamageRaw);
        wPStats.setDamagePerSwing(wPDamageAverage);
        RogueAbilityStats aPStats = new RogueAPStats();
        aPStats.setDamagePerHit(aPDamageRaw);
        aPStats.setDamagePerSwing(aPDamageAverage);
        // endregion

        // region Rotations
        RogueRotationCalculator rotationCalculator = new RogueRotationCalculator(character, stats, calcOpts,
                maintainBleed, mainHandSpeed, offHandSpeed, mainHandSpeedNorm, offHandSpeedNorm,
                chanceWhiteMHAvoided, chanceWhiteOHAvoided, chanceMHAvoided, chanceOHAvoided, chanceFinisherAvoided, chancePoisonAvoided, chanceCritYellow * 0.2f * character.getRogueTalents().getSealFate(), (1f - chanceHitMuti * chanceHitMuti) * 0.2f * character.getRogueTalents().getSealFate(),
                mainHandStats, offHandStats, backstabStats, hemoStats, sStrikeStats, mutiStats,
                ruptStats, evisStats, envenomStats, snDStats, iPStats, dPStats, wPStats, aPStats);
        RogueRotationCalculator.RogueRotationCalculation rotationCalculationDPS = new RogueRotationCalculator.RogueRotationCalculation();

        boolean bleedIsUp = calcOpts.getBleedIsUp();
        if (calcOpts.getForceCustom() == false)
            for (int snDCP = 1; snDCP < 6; snDCP++)
                for (int finisher = 1; finisher < 3; finisher++) {
                    if ((finisher == 1 && calcOpts.getEnableEvis() == false) ||
                            (finisher == 2 && calcOpts.getEnableEnvenom() == false)) continue;
                    for (int finisherCP = 1; finisherCP < 6; finisherCP++)
                        for (int CPG = 0; CPG < 4; CPG++) {
                            if ((CPG == 0 && (calcOpts.getEnableMuti() == false || mutiStats.getDamagePerSwing() == 0)) ||
                                    (CPG == 1 && calcOpts.getEnableSS() == false) ||
                                    (CPG == 2 && (calcOpts.getEnableBS() == false || backstabStats.getDamagePerSwing() == 0)) ||
                                    (CPG == 3 && (calcOpts.getEnableHemo() == false || hemoStats.getDamagePerSwing() == 0))) continue;
                            for (int mHPoison = 0; mHPoison < 5; mHPoison++) {
                                if (!targetPoisonable || mainHand == null) break;
                                if ((mHPoison == 1 && (calcOpts.getEnableIP() == false || character.getRogueTalents().getImprovedPoisons() < 4)) ||
                                        (mHPoison == 2 && calcOpts.getEnableDP() == false) ||
                                        (mHPoison == 3 && (calcOpts.getEnableWP() == false || character.getRogueTalents().getImprovedPoisons() > 3))) continue;
                                for (int oHPoison = 0; oHPoison < 5; oHPoison++) {
                                    if (!targetPoisonable || offHand == null) break;
                                    if ((oHPoison == 1 && (calcOpts.getEnableIP() == false || character.getRogueTalents().getImprovedPoisons() < 4)) ||
                                            (oHPoison == 2 && calcOpts.getEnableDP() == false) ||
                                            (oHPoison == 3 && (calcOpts.getEnableWP() == false || character.getRogueTalents().getImprovedPoisons() > 3))) continue;
                                    for (int useRupt = 0; useRupt < 2; useRupt++) {
                                        if (useRupt == 1 && calcOpts.getEnableRupt() == false) continue;
                                        boolean useTotT = stats.getBonusToTTEnergy() > 0;
                                        RogueRotationCalculator.RogueRotationCalculation rotationCalculation =
                                                rotationCalculator.getRotationCalculations(
                                                        CPG, useRupt == 1, finisher, finisherCP, snDCP, mHPoison, oHPoison, bleedIsUp, useTotT, ptrMode);
                                        if (rotationCalculation.dps > rotationCalculationDPS.dps)
                                            rotationCalculationDPS = rotationCalculation;
                                    }
                                }
                            }
                        }
                }

        calculatedStats.customRotation = rotationCalculator.getRotationCalculations(
                calcOpts.getCustomCPG(), calcOpts.getCustomUseRupt(), calcOpts.getCustomFinisher(), calcOpts.getCustomCPFinisher(), calcOpts.getCustomCPSnD(), calcOpts.getCustomMHPoison(), calcOpts.getCustomOHPoison(), bleedIsUp, calcOpts.getCustomUseTotT(), ptrMode);
        calculatedStats.highestDPSRotation = calcOpts.getForceCustom() == false ? rotationCalculationDPS : calculatedStats.customRotation;

        if (character.getRogueTalents().isGlyphOfBackstab() && rotationCalculationDPS.backstabCount > 0) {
            ruptStats.setDurationUptime(ruptStats.getDurationUptime() + 6f);
            ruptStats.setDurationAverage(ruptStats.getDurationAverage() + 6f);
        }
        ruptStats.setDamagePerHit(ruptStats.getDamagePerHit() * ruptStats.getDurationUptime() / 16f);
        ruptStats.setDamagePerSwing(ruptStats.getDamagePerSwing() * ruptStats.getDurationUptime() / 16f);
        // endregion

        calculatedStats.avoidedWhiteMHAttacks = chanceWhiteMHAvoided * 100f;
        calculatedStats.avoidedWhiteOHAttacks = chanceWhiteOHAvoided * 100f;
        calculatedStats.avoidedAttacks = chanceMHAvoided * 100f;
        calculatedStats.avoidedFinisherAttacks = chanceFinisherAvoided * 100f;
        calculatedStats.avoidedPoisonAttacks = chancePoisonAvoided * 100f;
        calculatedStats.dodgedMHAttacks = chanceMHDodge * 100f;
        calculatedStats.parriedAttacks = chanceParry * 100f;
        calculatedStats.missedAttacks = chanceMiss * 100f;
        calculatedStats.critChanceYellow = chanceCritYellow * 100f;
        calculatedStats.critChanceMHTotal = chanceCritWhiteMainTotal * 100f;
        calculatedStats.critChanceMH = chanceCritWhiteMain * 100f;
        calculatedStats.critChanceOHTotal = chanceCritWhiteOffTotal * 100f;
        calculatedStats.critChanceOH = chanceCritWhiteOff * 100f;
        calculatedStats.mainHandSpeed = mainHandSpeed;
        calculatedStats.offHandSpeed = offHandSpeed;
        calculatedStats.armorMitigationMH = (1f - mainHandModArmor) * 100f;
        calculatedStats.armorMitigationOH = (1f - offHandModArmor) * 100f;
        calculatedStats.duration = calcOpts.getDuration();

        calculatedStats.mainHandStats = mainHandStats;
        calculatedStats.offHandStats = offHandStats;
        calculatedStats.backstabStats = backstabStats;
        calculatedStats.hemoStats = hemoStats;
        calculatedStats.sStrikeStats = sStrikeStats;
        calculatedStats.mutiStats = mutiStats;
        calculatedStats.ruptStats = ruptStats;
        calculatedStats.snDStats = snDStats;
        calculatedStats.evisStats = evisStats;
        calculatedStats.envenomStats = envenomStats;
        calculatedStats.iPStats = iPStats;
        calculatedStats.dPStats = dPStats;
        calculatedStats.wPStats = wPStats;
        calculatedStats.aPStats = aPStats;

        float magicDPS = (stats.getShadowDamage() + stats.getArcaneDamage()) * (1f + chanceCritYellow);
        calculatedStats.setDPSPoints(calculatedStats.highestDPSRotation.dps + magicDPS);
        calculatedStats.setSurvivabilityPoints(stats.getHealth() / 100f);
        calculatedStats.setOverallPoints(calculatedStats.getDPSPoints() + calculatedStats.getSurvivabilityPoints());
        return calculatedStats;
    }

    // Out-parameter holder for GetCharacterStatsWithTemporaryEffects (framework adaptation).
    private static class TemporaryEffects {
        Stats stats;
        WeightedStat[] armorPenetrationUptimes;
        WeightedStat[] critRatingUptimes;
    }

    private TemporaryEffects getCharacterStatsWithTemporaryEffects(Character character, Item additionalItem) {
        ICalculationOptionBase options = character.getCalculationOptions();
        CalculationOptionsRogue calcOpts = options instanceof CalculationOptionsRogue ? (CalculationOptionsRogue) options : null;
        int targetLevel = calcOpts.getTargetLevel();
        boolean targetPoisonable = calcOpts.getTargetPoisonable();

        Stats statsRace = BaseStats.getBaseStats(80, character.getCharacterClass(), character.getRace());
        statsRace.setPhysicalHaste(0.4f); //Slice and Dice

        Stats statsItems = new Stats();
        accumulateItemStats(statsItems, character, additionalItem);
        Stats statsBuffs = getBuffsStats(character, calcOpts);

        rawr.base.talents.RogueTalents talents = character.getRogueTalents();

        Stats statsTalents = new Stats();
        statsTalents.setBonusAgilityMultiplier(0.03f * talents.getSinisterCalling());
        statsTalents.setBonusAttackPowerMultiplier((1f + 0.02f * talents.getSavageCombat()) * (1f + 0.02f * talents.getDeadliness()) - 1f);
        statsTalents.setBonusCritMultiplier(0.04f * talents.getPreyOnTheWeak());
        statsTalents.setBonusDamageMultiplier((1f + 0.02f * talents.getMurder()) *
                (1f + 0.01f * talents.getSlaughterFromTheShadows()) *
                (1f + (0.05f + (talents.isGlyphOfHungerforBlood() ? 0.03f : 0f)) * talents.getHungerForBlood()) - 1f);
        statsTalents.setBonusStaminaMultiplier(0.02f * talents.getEndurance());
        statsTalents.setDodge(0.02f * talents.getLightningReflexes());
        statsTalents.setExpertise(5 * talents.getWeaponExpertise());
        statsTalents.setPhysicalCrit(0.01f * talents.getMalice() + ((targetPoisonable &&
                !(character.activeBuffsContains("Master Poisoner") || character.activeBuffsContains("Heart of the Crusader") || character.activeBuffsContains("Totem of Wrath")))
                ? 0.01f * talents.getMasterPoisoner() : 0));
        statsTalents.setPhysicalHit(0.01f * talents.getPrecision());
        statsTalents.setPhysicalHaste((talents.getLightningReflexes() == 1 ? 0.04f : (talents.getLightningReflexes() == 2 ? 0.07f : (talents.getLightningReflexes() == 3 ? 0.1f : 0))));
        statsTalents.setSpellCrit(0.01f * talents.getMalice() + ((targetPoisonable &&
                !(character.activeBuffsContains("Master Poisoner") || character.activeBuffsContains("Heart of the Crusader") || character.activeBuffsContains("Totem of Wrath")))
                ? 0.01f * talents.getMasterPoisoner() : 0));
        statsTalents.setSpellHit(0.01f * talents.getPrecision());

        Stats statsGearEnchantsBuffs = Stats.add(statsItems, statsBuffs);
        Stats statsTotal = Stats.add(statsRace, statsItems);
        statsTotal.accumulate(statsBuffs);
        statsTotal.accumulate(statsTalents);

        statsTotal.setStamina((float) Math.floor(statsTotal.getStamina() * (1f + statsTotal.getBonusStaminaMultiplier())));
        statsTotal.setStrength((float) Math.floor(statsTotal.getStrength() * (1f + statsTotal.getBonusStrengthMultiplier())));
        statsTotal.setAgility((float) Math.floor(statsTotal.getAgility() * (1f + statsTotal.getBonusAgilityMultiplier())));
        statsTotal.setAttackPower(statsTotal.getAttackPower() + statsTotal.getStrength() + statsTotal.getAgility());
        statsTotal.setAttackPower((float) Math.floor(statsTotal.getAttackPower() * (1f + statsTotal.getBonusAttackPowerMultiplier())));
        statsTotal.setHealth(statsTotal.getHealth() + (float) Math.floor((statsTotal.getStamina() - 20f) * 10f + 20f));
        statsTotal.setArmor(statsTotal.getArmor() + 2f * statsTotal.getAgility());
        statsTotal.setArmor((float) Math.floor(statsTotal.getArmor() * (1f + statsTotal.getBonusArmorMultiplier())));
        statsTotal.setNatureResistance(statsTotal.getNatureResistance() + statsTotal.getNatureResistanceBuff());
        statsTotal.setFireResistance(statsTotal.getFireResistance() + statsTotal.getFireResistanceBuff());
        statsTotal.setFrostResistance(statsTotal.getFrostResistance() + statsTotal.getFrostResistanceBuff());
        statsTotal.setShadowResistance(statsTotal.getShadowResistance() + statsTotal.getShadowResistanceBuff());
        statsTotal.setArcaneResistance(statsTotal.getArcaneResistance() + statsTotal.getArcaneResistanceBuff());

        float hasteBonus = StatConversion.getPhysicalHasteFromRating(statsTotal.getHasteRating(), CharacterClass.Rogue);
        hasteBonus = (1f + hasteBonus) * (1f + statsTotal.getPhysicalHaste()) - 1f;
        float meleeHitInterval = 1f / ((character.get(CharacterSlot.MainHand) == null ? 2 : character.get(CharacterSlot.MainHand).getSpeed() / hasteBonus) + (character.get(CharacterSlot.OffHand) == null ? 2 : character.get(CharacterSlot.OffHand).getSpeed() / hasteBonus));

        //To calculate the Poison hit interval only white attacks are taken into account, IP is assumed on mainhand and DP on offhand
        float iPPS = 8.53f * (1f + 0.1f * talents.getImprovedPoisons()) / 60f;
        float dPPS = calcOpts.getDuration() / (character.get(CharacterSlot.OffHand) == null ? 2 : character.get(CharacterSlot.OffHand).getSpeed() / hasteBonus) * 0.3f;
        float poisonHitInterval = 1 / (iPPS + dPPS);

        float hitBonus = StatConversion.getPhysicalHitFromRating(statsTotal.getHitRating()) + statsTotal.getPhysicalHit();
        float spellHitBonus = statsTotal.getSpellHit() + StatConversion.getHitFromRating(statsTotal.getHitRating(), CharacterClass.Rogue);
        float expertiseBonus = StatConversion.getDodgeParryReducFromExpertise(StatConversion.getExpertiseFromRating(statsTotal.getExpertiseRating(), CharacterClass.Rogue) + statsTotal.getExpertise(), CharacterClass.Rogue);
        float chanceDodge = Math.max(0f, StatConversion.WHITE_DODGE_CHANCE_CAP[targetLevel - 80] - expertiseBonus);
        float chanceParry = 0f; //Math.Max(0f, StatConversion.WHITE_PARRY_CHANCE_CAP[targetLevel - 80] - expertiseBonus);
        float chanceMiss = Math.max(0f, StatConversion.WHITE_MISS_CHANCE_CAP[targetLevel - 80] - hitBonus);
        float chanceAvoided = chanceMiss + chanceDodge + chanceParry;
        float chancePoisonAvoided = Math.max(0f, StatConversion.getSpellMiss(80 - targetLevel, false) - spellHitBonus);

        float rawChanceCrit = StatConversion.getPhysicalCritFromRating(statsTotal.getCritRating())
                + StatConversion.getPhysicalCritFromAgility(statsTotal.getAgility(), CharacterClass.Rogue)
                + statsTotal.getPhysicalCrit()
                + StatConversion.NPC_LEVEL_CRIT_MOD[targetLevel - 80];
        float chanceCrit = rawChanceCrit * (1f - chanceAvoided);
        float chanceHit = 1f - chanceAvoided;

        LinkedHashMap<Trigger, Float> triggerIntervals = new LinkedHashMap<Trigger, Float>();
        LinkedHashMap<Trigger, Float> triggerChances = new LinkedHashMap<Trigger, Float>();
        triggerIntervals.put(Trigger.Use, 0f);
        triggerIntervals.put(Trigger.MeleeHit, meleeHitInterval);
        triggerIntervals.put(Trigger.PhysicalHit, meleeHitInterval);
        triggerIntervals.put(Trigger.MeleeAttack, meleeHitInterval);
        triggerIntervals.put(Trigger.MeleeCrit, meleeHitInterval);
        triggerIntervals.put(Trigger.PhysicalCrit, meleeHitInterval);
        triggerIntervals.put(Trigger.DoTTick, 0f);
        triggerIntervals.put(Trigger.DamageDone, meleeHitInterval / 2f);
        triggerIntervals.put(Trigger.DamageOrHealingDone, meleeHitInterval / 2f); // Need to add Self-heals
        triggerIntervals.put(Trigger.SpellHit, poisonHitInterval);
        triggerChances.put(Trigger.Use, 1f);
        triggerChances.put(Trigger.MeleeHit, Math.max(0f, chanceHit));
        triggerChances.put(Trigger.PhysicalHit, Math.max(0f, chanceHit));
        triggerChances.put(Trigger.MeleeAttack, Math.max(0f, chanceHit));
        triggerChances.put(Trigger.MeleeCrit, Math.max(0f, chanceCrit));
        triggerChances.put(Trigger.PhysicalCrit, Math.max(0f, chanceCrit));
        triggerChances.put(Trigger.DoTTick, 1f);
        triggerChances.put(Trigger.DamageDone, 1f - chanceAvoided / 2f);
        triggerChances.put(Trigger.DamageOrHealingDone, 1f - chanceAvoided / 2f); // Need to add Self-heals
        triggerChances.put(Trigger.SpellHit, Math.max(0f, 1f - chancePoisonAvoided));

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
        statsProcs.setCritRating(0);
        statsProcs.setAgility(0);
        statsProcs.setArmorPenetrationRating(0);
        statsTotal.accumulate(statsProcs);

        //Handle Crit procs
        WeightedStat[] critRatingUptimes = new WeightedStat[0];
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

        if (tempCritEffects.size() == 0) {
            critRatingUptimes = new WeightedStat[] { new WeightedStat(1f, 0f) };
        } else if (tempCritEffects.size() == 1) { //Only one, add it to
            SpecialEffect effect = tempCritEffects.get(0);
            float uptime = effect.getAverageUptime(triggerIntervals.get(effect.getTrigger()), triggerChances.get(effect.getTrigger()), 1f, calcOpts.getDuration()) * tempCritEffectScales.get(0);
            float totalAgi = (effect.getStats().getAgility() + effect.getStats().getDeathbringerProc() + effect.getStats().getHighestStat() + effect.getStats().getParagon()) * (1f + statsTotal.getBonusAgilityMultiplier());
            critRatingUptimes = new WeightedStat[] {
                    new WeightedStat(uptime,
                            effect.getStats().getCritRating() + StatConversion.getCritFromAgility(totalAgi - 10,
                                    CharacterClass.Rogue) * StatConversion.RATING_PER_PHYSICALCRIT),
                    new WeightedStat(1f - uptime, 0f) };
        } else if (tempCritEffects.size() > 1) {
            List<Float> tempCritEffectsValues = new ArrayList<Float>();
            for (SpecialEffect effect : tempCritEffects) {
                float totalAgi = (float) effect.getMaxStack() * (effect.getStats().getAgility() + effect.getStats().getDeathbringerProc() + effect.getStats().getHighestStat() + effect.getStats().getParagon()) * (1f + statsTotal.getBonusAgilityMultiplier());
                tempCritEffectsValues.add(effect.getStats().getCritRating() +
                        StatConversion.getCritFromAgility(totalAgi - 10, CharacterClass.Rogue) *
                                StatConversion.RATING_PER_PHYSICALCRIT);
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
            critRatingUptimes = critWeights;
        }

        //Handle ArPen procs
        WeightedStat[] armorPenetrationUptimes = new WeightedStat[0];
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

        if (tempArPenEffects.size() == 0) {
            armorPenetrationUptimes = new WeightedStat[] { new WeightedStat(1f, 0f) };
        } else if (tempArPenEffects.size() == 1) { //Only one, add it to
            SpecialEffect effect = tempArPenEffects.get(0);
            float uptime = effect.getAverageUptime(triggerIntervals.get(effect.getTrigger()), triggerChances.get(effect.getTrigger()), 1f, calcOpts.getDuration()) * tempArPenEffectScales.get(0);
            armorPenetrationUptimes = new WeightedStat[] {
                    new WeightedStat(uptime, effect.getStats().getArmorPenetrationRating() + effect.getStats().getDeathbringerProc()),
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
            armorPenetrationUptimes = arPenWeights;
        }

        TemporaryEffects result = new TemporaryEffects();
        result.stats = statsTotal;
        result.armorPenetrationUptimes = armorPenetrationUptimes;
        result.critRatingUptimes = critRatingUptimes;
        return result;
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] result = new float[list.size()];
        for (int i = 0; i < list.size(); i++) result[i] = list.get(i);
        return result;
    }

    public Stats getCharacterStats(Character character, Item additionalItem) {
        return getCharacterStatsWithTemporaryEffects(character, additionalItem).stats;
    }

    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        switch (chartName) {
            case "Combat Table":
                CharacterCalculationsRogue currentCalculationsRogue = (CharacterCalculationsRogue) getCharacterCalculations(character);
                ComparisonCalculationsRogue calcMiss = new ComparisonCalculationsRogue();
                ComparisonCalculationsRogue calcDodge = new ComparisonCalculationsRogue();
                ComparisonCalculationsRogue calcParry = new ComparisonCalculationsRogue();
                ComparisonCalculationsRogue calcBlock = new ComparisonCalculationsRogue();
                ComparisonCalculationsRogue calcGlance = new ComparisonCalculationsRogue();
                ComparisonCalculationsRogue calcCrit = new ComparisonCalculationsRogue();
                ComparisonCalculationsRogue calcHit = new ComparisonCalculationsRogue();

                if (currentCalculationsRogue != null) {
                    calcMiss.setName("    Miss    ");
                    calcDodge.setName("   Dodge   ");
                    calcGlance.setName(" Glance ");
                    calcCrit.setName("  Crit  ");
                    calcHit.setName("Hit");

                    calcMiss.setOverallPoints(0f);
                    calcDodge.setOverallPoints(0f);
                    calcParry.setOverallPoints(0f);
                    calcBlock.setOverallPoints(0f);
                    calcGlance.setOverallPoints(0f);
                    calcCrit.setOverallPoints(0f);
                    calcHit.setOverallPoints(0f);
                }
                return new ComparisonCalculationBase[] { calcMiss, calcDodge, calcParry, calcGlance, calcBlock, calcCrit, calcHit };

            default:
                return new ComparisonCalculationBase[0];
        }
    }

    public Stats getRelevantStats(Stats stats) {
        Stats relevantStats = new Stats();
        relevantStats.setAgility(stats.getAgility());
        relevantStats.setStrength(stats.getStrength());
        relevantStats.setAttackPower(stats.getAttackPower());
        relevantStats.setCritRating(stats.getCritRating());
        relevantStats.setHitRating(stats.getHitRating());
        relevantStats.setStamina(stats.getStamina());
        relevantStats.setHasteRating(stats.getHasteRating());
        relevantStats.setExpertiseRating(stats.getExpertiseRating());
        relevantStats.setArmorPenetration(stats.getArmorPenetration());
        relevantStats.setArmorPenetrationRating(stats.getArmorPenetrationRating());
        relevantStats.setWeaponDamage(stats.getWeaponDamage());
        relevantStats.setBonusAgilityMultiplier(stats.getBonusAgilityMultiplier());
        relevantStats.setBonusAttackPowerMultiplier(stats.getBonusAttackPowerMultiplier());
        relevantStats.setBonusCritMultiplier(stats.getBonusCritMultiplier());
        relevantStats.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());
        relevantStats.setBonusStaminaMultiplier(stats.getBonusStaminaMultiplier());
        relevantStats.setBonusStrengthMultiplier(stats.getBonusStrengthMultiplier());
        relevantStats.setHealth(stats.getHealth());
        relevantStats.setThreatReductionMultiplier(stats.getThreatReductionMultiplier());
        relevantStats.setPhysicalHaste(stats.getPhysicalHaste());
        relevantStats.setPhysicalHit(stats.getPhysicalHit());
        relevantStats.setPhysicalCrit(stats.getPhysicalCrit());
        relevantStats.setHighestStat(stats.getHighestStat());
        relevantStats.setMoteOfAnger(stats.getMoteOfAnger());

        relevantStats.setRuptureDamageBonus(stats.getRuptureDamageBonus());
        relevantStats.setComboMoveEnergyReduction(stats.getComboMoveEnergyReduction());
        relevantStats.setBonusEnergyFromDP(stats.getBonusEnergyFromDP());
        relevantStats.setRuptureCrit(stats.getRuptureCrit());
        relevantStats.setReduceEnergyCostFromRupture(stats.getReduceEnergyCostFromRupture());
        relevantStats.setBonusCPGCritChance(stats.getBonusCPGCritChance());
        relevantStats.setBonusToTTEnergy(stats.getBonusToTTEnergy());
        relevantStats.setChanceOn3CPOnFinisher(stats.getChanceOn3CPOnFinisher());

        relevantStats.setBonusPhysicalDamageMultiplier(stats.getBonusPhysicalDamageMultiplier());
        relevantStats.setBonusBleedDamageMultiplier(stats.getBonusBleedDamageMultiplier());
        relevantStats.setSpellHit(stats.getSpellHit());
        relevantStats.setSpellCrit(stats.getSpellCrit());
        relevantStats.setSpellCritOnTarget(stats.getSpellCritOnTarget());

        // Damage Procs
        relevantStats.setShadowDamage(stats.getShadowDamage());
        relevantStats.setArcaneDamage(stats.getArcaneDamage());
        relevantStats.setHolyDamage(stats.getHolyDamage());
        relevantStats.setNatureDamage(stats.getNatureDamage());
        relevantStats.setFrostDamage(stats.getFrostDamage());
        relevantStats.setFireDamage(stats.getFireDamage());
        relevantStats.setBonusShadowDamageMultiplier(stats.getBonusShadowDamageMultiplier());
        relevantStats.setBonusArcaneDamageMultiplier(stats.getBonusArcaneDamageMultiplier());
        relevantStats.setBonusHolyDamageMultiplier(stats.getBonusHolyDamageMultiplier());
        relevantStats.setBonusNatureDamageMultiplier(stats.getBonusNatureDamageMultiplier());
        relevantStats.setBonusFrostDamageMultiplier(stats.getBonusFrostDamageMultiplier());
        relevantStats.setBonusFireDamageMultiplier(stats.getBonusFireDamageMultiplier());

        for (SpecialEffect effect : stats.specialEffects()) {
            if (effect.getTrigger() == Trigger.Use || effect.getTrigger() == Trigger.MeleeCrit || effect.getTrigger() == Trigger.MeleeHit
                    || effect.getTrigger() == Trigger.PhysicalCrit || effect.getTrigger() == Trigger.PhysicalHit || effect.getTrigger() == Trigger.DoTTick
                    || effect.getTrigger() == Trigger.DamageDone || effect.getTrigger() == Trigger.DamageOrHealingDone || effect.getTrigger() == Trigger.SpellHit
                    || effect.getTrigger() == Trigger.MeleeAttack) {
                if (hasRelevantStats(effect.getStats())) {
                    relevantStats.addSpecialEffect(effect);
                }
            }
        }

        return relevantStats;
    }

    @Override
    public boolean hasRelevantStats(Stats stats) {
        boolean relevant = (
                stats.getAgility() +
                        stats.getStrength() +
                        stats.getAttackPower() +
                        stats.getCritRating() +
                        stats.getHitRating() +
                        //stats.Stamina +
                        stats.getHasteRating() +
                        stats.getExpertiseRating() +
                        stats.getArmorPenetration() +
                        stats.getArmorPenetrationRating() +
                        stats.getWeaponDamage() +
                        stats.getBonusAgilityMultiplier() +
                        stats.getBonusAttackPowerMultiplier() +
                        stats.getBonusCritMultiplier() +
                        stats.getBonusDamageMultiplier() +
                        stats.getBonusStaminaMultiplier() +
                        stats.getBonusStrengthMultiplier() +
                        //stats.Health +
                        stats.getThreatReductionMultiplier() +
                        stats.getPhysicalHaste() +
                        stats.getPhysicalHit() +
                        stats.getPhysicalCrit() +
                        stats.getHighestStat() +

                        stats.getRuptureDamageBonus() +
                        stats.getComboMoveEnergyReduction() +
                        stats.getBonusEnergyFromDP() +
                        stats.getRuptureCrit() +
                        stats.getReduceEnergyCostFromRupture() +
                        stats.getBonusCPGCritChance() +
                        stats.getBonusToTTEnergy() +
                        stats.getChanceOn3CPOnFinisher() +

                        stats.getBonusPhysicalDamageMultiplier() +
                        stats.getBonusBleedDamageMultiplier() +
                        stats.getSpellHit() +
                        stats.getSpellCrit() +
                        stats.getSpellCritOnTarget() +

                        // Trinket Procs
                        stats.getParagon() +
                        stats.getDeathbringerProc() +
                        stats.getMoteOfAnger() +

                        // Damage Procs
                        stats.getShadowDamage() +
                        stats.getArcaneDamage() +
                        stats.getHolyDamage() +
                        stats.getNatureDamage() +
                        stats.getFrostDamage() +
                        stats.getFireDamage() +
                        stats.getBonusShadowDamageMultiplier() +
                        stats.getBonusArcaneDamageMultiplier() +
                        stats.getBonusHolyDamageMultiplier() +
                        stats.getBonusNatureDamageMultiplier() +
                        stats.getBonusFrostDamageMultiplier() +
                        stats.getBonusFireDamageMultiplier()
        ) > 0 || (stats.getStamina() > 0 && stats.getSpellPower() == 0);

        for (SpecialEffect effect : stats.specialEffects()) {
            if (effect.getTrigger() == Trigger.Use
                    || effect.getTrigger() == Trigger.MeleeHit
                    || effect.getTrigger() == Trigger.MeleeCrit
                    || effect.getTrigger() == Trigger.MeleeAttack
                    || effect.getTrigger() == Trigger.PhysicalHit
                    || effect.getTrigger() == Trigger.PhysicalCrit
                    || effect.getTrigger() == Trigger.DoTTick
                    || effect.getTrigger() == Trigger.DamageDone
                    || effect.getTrigger() == Trigger.DamageOrHealingDone
                    || effect.getTrigger() == Trigger.SpellHit) // For Poison Hits
            {
                relevant |= hasRelevantStats(effect.getStats());
                if (relevant) { break; }
            }
        }
        return relevant;
    }

    @Override
    public boolean isItemRelevant(Item item) {
        if (item.getSlot() == rawr.base.items.ItemSlot.OffHand && item.getType() == ItemType.None)
            return false;
        return super.isItemRelevant(item);
    }

    public Stats getBuffsStats(Character character, CalculationOptionsRogue calcOpts) {
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

        if (character.getPrimaryProfession() == rawr.base.character.Profession.Alchemy ||
                character.getSecondaryProfession() == rawr.base.character.Profession.Alchemy)
            character.activeBuffsAdd(("Flask of Endless Rage (Mixology)"));
    }

    private static List<String> relevantGlyphs = null;

    public List<String> getRelevantGlyphs() {
        if (relevantGlyphs == null) {
            relevantGlyphs = new ArrayList<String>();
            relevantGlyphs.add("Glyph of Backstab");
            relevantGlyphs.add("Glyph of Eviscerate");
            relevantGlyphs.add("Glyph of Mutilate");
            relevantGlyphs.add("Glyph of Hunger For Blood");
            relevantGlyphs.add("Glyph of Sinister Strike");
            relevantGlyphs.add("Glyph of Slice and Dice");
            relevantGlyphs.add("Glyph of Feint");
            relevantGlyphs.add("Glyph of Rupture");
            relevantGlyphs.add("Glyph of Blade Flurry");
            relevantGlyphs.add("Glyph of Adrenaline Rush");
            relevantGlyphs.add("Glyph of Killing Spree");
            relevantGlyphs.add("Glyph of Vigor");
            relevantGlyphs.add("Glyph of Fan of Knives");
            relevantGlyphs.add("Glyph of Expose Armor");
            relevantGlyphs.add("Glyph of Ghostly Strike");
            relevantGlyphs.add("Glyph of Tricks of the Trade");
        }
        return relevantGlyphs;
    }

    private static CalculationsRogue instance;

    public static CalculationsRogue getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("Rogue", CalculationsRogue.class);
    }

    public static void register() {
        Calculations.registerModel("Rogue", CalculationsRogue.class);
    }

    public CalculationsRogue() {
        instance = this;
    }
}
