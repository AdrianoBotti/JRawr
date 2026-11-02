// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtWarr/CalculationsProtWarr.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: UI-only members omitted (CalculationOptionsPanel, the RAWR3/Silverlight color branch);
// the non-Silverlight display label/color/optimizable/chart/glyph tables are ported additively as
// getCharacterDisplayCalculationLabels()/getSubPointNameColors()/getOptimizableCalculationLabels()/
// getCustomChartNames()/getCustomChartData()/getRelevantGlyphs().
// FRAMEWORK ADAPTATION: DeserializeDataObject uses a JDK-XML reflective reader instead of
// XmlSerializer (same approach as CalculationsCat/Bear/Healadin).
package rawr.models.protwarr;

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
import rawr.base.stats.HitResult;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;
import rawr.base.talents.WarriorTalents;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class CalculationsProtWarr extends CalculationsBase {
    // C# CalculationsProtWarr.cs:12-87 DefaultGemmingTemplates.
    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        // Relevant Gem IDs for ProtWarr
        // Red
        int[] bold = { 39900, 39996, 40111, 42142 };        // Strength
        int[] delicate = { 39905, 39997, 40112, 42143 };    // Agility
        int[] subtle = { 39907, 40000, 40115, 42151 };      // Dodge
        int[] flashing = { 39908, 40001, 40116, 42152 };    // Parry
        int[] precise = { 39910, 40003, 40118, 42154 };     // Expertise

        // Purple
        int[] shifting = { 39935, 40023, 40130, 40130 };    // Agility + Stamina
        int[] sovereign = { 39934, 40022, 40129, 40129 };   // Strength + Stamina
        int[] regal = { 39938, 40031, 40138, 40138 };       // Dodge + Stamina
        int[] defender = { 39939, 40032, 40139, 40139 };    // Parry + Stamina
        int[] guardian = { 39940, 40034, 40141, 40141 };    // Expertise + Stamina

        // Blue
        int[] solid = { 39919, 40008, 40119, 36767 };       // Stamina

        // Green
        int[] enduring = { 39976, 40089, 40167, 40167 };    // Defense + Stamina

        // Yellow
        int[] thick = { 39916, 40015, 40126, 42157 };       // Defense

        // Orange
        int[] etched = { 39948, 40038, 40143, 40143 };      // Strength + Hit
        int[] champion = { 39949, 40039, 40144, 40144 };    // Strength + Defense
        int[] stalwart = { 39964, 40056, 40160, 40160 };    // Dodge + Defense
        int[] glimmering = { 39965, 40057, 40161, 40161 };  // Parry + Defense
        int[] accurate = { 39966, 40058, 40162, 40162 };    // Expertise + Hit
        int[] resolute = { 39967, 40059, 40163, 40163 };    // Expertise + Defense

        // Meta
        int[] austere = { 41380 };

        String[] groupNames = new String[] { "Uncommon", "Rare", "Epic", "Jeweler" };
        int[][][] gemmingTemplates = new int[][][] {
                //  Red       Yellow      Blue        Prismatic   Meta
                { thick, thick, thick, thick, austere },          // Max Avoidance (Defense)
                { stalwart, enduring, enduring, enduring, austere }, // Avoidance Heavy (Dodge/Defense)
                { flashing, glimmering, defender, flashing, austere }, // Avoidance Heavy (Parry)
                { subtle, stalwart, regal, subtle, austere },     // Avoidance Heavy (Dodge)
                { defender, enduring, solid, solid, austere },    // Balanced Avoidance (Parry)
                { regal, enduring, solid, solid, austere },       // Balanced Avoidance (Dodge)
                { guardian, enduring, solid, solid, austere },    // Balanced TPS
                { solid, solid, solid, solid, austere },          // Max Health
        };

        // Generate List of Gemming Templates
        // groupNames[] index starts at 1 to ignore Uncommon gems currently
        List<GemmingTemplate> gemmingTemplate = new ArrayList<GemmingTemplate>();
        for (int j = 1; j <= groupNames.length - 1; j++) {
            for (int i = 0; i <= gemmingTemplates.length - 1; i++) {
                GemmingTemplate template = new GemmingTemplate();
                template.setModel("ProtWarr");
                template.setGroup(groupNames[j]);
                template.setRedId(gemmingTemplates[i][0][j]);
                template.setYellowId(gemmingTemplates[i][1][j]);
                template.setBlueId(gemmingTemplates[i][2][j]);
                template.setPrismaticId(gemmingTemplates[i][3][j]);
                template.setMetaId(gemmingTemplates[i][4][0]);
                template.setEnabled(j == 2);
                gemmingTemplate.add(template);
            }
        }
        return gemmingTemplate;
    }

    // C# CalculationsProtWarr.cs:109-184 CharacterDisplayCalculationLabels.
    private static final String[] CharacterDisplayCalculationLabels = new String[] {
            "Base Stats:Health",
            "Base Stats:Strength",
            "Base Stats:Agility",
            "Base Stats:Stamina",

            "Defensive Stats:Armor",
            "Defensive Stats:Defense",
            "Defensive Stats:Dodge",
            "Defensive Stats:Parry",
            "Defensive Stats:Block",
            "Defensive Stats:Miss",
            "Defensive Stats:Block Value",
            "Defensive Stats:Resilience",
            "Defensive Stats:Chance to be Crit",
            "Defensive Stats:Guaranteed Reduction",
            "Defensive Stats:Avoidance",
            "Defensive Stats:Total Mitigation",
            "Defensive Stats:Attacker Speed",
            "Defensive Stats:Damage Taken",

            "Offensive Stats:Weapon Speed",
            "Offensive Stats:Attack Power",
            "Offensive Stats:Hit",
            "Offensive Stats:Haste",
            "Offensive Stats:Armor Penetration",
            "Offensive Stats:Crit",
            "Offensive Stats:Expertise",
            // Never really used in current WoW itemization, just taking up space
            //"Offensive Stats:Weapon Damage",
            "Offensive Stats:Missed Attacks",
            "Offensive Stats:Total Damage/sec",
            "Offensive Stats:Total Threat/sec",
            "Offensive Stats:Rotation",

            "Resistances:Nature Resist",
            "Resistances:Fire Resist",
            "Resistances:Frost Resist",
            "Resistances:Shadow Resist",
            "Resistances:Arcane Resist",
            "Complex Stats:Ranking Mode*The currently selected ranking mode. Ranking modes can be changed in the Options tab.",
            "Complex Stats:Overall Points*Overall Points are a sum of Mitigation and Survival Points. \n"
                    + "Overall is typically, but not always, the best way to rate gear. \n"
                    + "For specific encounters, closer attention to Mitigation and \n"
                    + "Survival Points individually may be important.",
            "Complex Stats:Mitigation Points*Mitigation Points represent the amount of damage you mitigate, \n"
                    + "on average, through armor mitigation and avoidance. It is directly \n"
                    + "relational to your Damage Taken. Ideally, you want to maximize \n"
                    + "Mitigation Points, while maintaining 'enough' Survival Points \n"
                    + "(see Survival Points). If you find yourself dying due to healers \n"
                    + "running OOM, or being too busy healing you and letting other \n"
                    + "raid members die, then focus on Mitigation Points.",
            "Complex Stats:Survival Points*Survival Points represents the total raw physical damage \n"
                    + "(pre-avoidance/block) you can take before dying. Unlike \n"
                    + "Mitigation Points, you should not attempt to maximize this, \n"
                    + "but rather get 'enough' of it, and then focus on Mitigation. \n"
                    + "'Enough' can vary greatly by fight and by your healers.\n"
                    + "If you find that you are being killed by burst damage,\n"
                    + "focus on Survival Points.",
            "Complex Stats:Threat Points*Threat Points represents the average between unlimited\n"
                    + "threat and limited threat scaled by the threat scale.",
            "Complex Stats:Nature Survival",
            "Complex Stats:Fire Survival",
            "Complex Stats:Frost Survival",
            "Complex Stats:Shadow Survival",
            "Complex Stats:Arcane Survival",
    };

    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        return List.of(CharacterDisplayCalculationLabels);
    }

    // C# CalculationsProtWarr.cs:186-214 OptimizableCalculationLabels.
    private static final String[] OptimizableCalculationLabels = new String[] {
            "Health",
            "% Total Mitigation",
            "% Guaranteed Reduction",
            "% Chance to be Crit",
            "% Avoidance",
            "% Avoidance+Block",
            "Block Value",
            "Threat/sec",
            "% Chance to be Avoided",
            "% Chance to be Dodged",
            "% Chance to be Parried",
            "% Chance to Miss",
            "Burst Time",
            "Nature Survival",
            "Fire Survival",
            "Frost Survival",
            "Shadow Survival",
            "Arcane Survival",
    };

    public String[] getOptimizableCalculationLabels() {
        return OptimizableCalculationLabels;
    }

    // C# CalculationsProtWarr.cs:216-232 CustomChartNames.
    private static final String[] CustomChartNames = new String[] {
            "Ability Damage",
            "Ability Threat",
            //"Rotation Damage",
            //"Rotation Threat",
            "Combat Table",
            "Item Budget",
    };

    public String[] getCustomChartNames() {
        return CustomChartNames;
    }

    // C# CalculationsProtWarr.cs:250-266 SubPointNameColors (non-Silverlight branch).
    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> colors = new LinkedHashMap<String, String>();
        colors.put("Survival", "#0000FF");
        colors.put("Mitigation", "#FF0000");
        colors.put("Threat", "#008000");
        return colors;
    }

    // C# CalculationsProtWarr.cs:268-295 RelevantItemTypes.
    private List<ItemType> relevantItemTypes = null;

    @Override
    public List<ItemType> getRelevantItemTypes() {
        if (this.relevantItemTypes == null) {
            this.relevantItemTypes = new ArrayList<ItemType>(List.of(
                    ItemType.None,
                    ItemType.Plate,
                    ItemType.Bow,
                    ItemType.Crossbow,
                    ItemType.Gun,
                    ItemType.Thrown,
                    ItemType.Arrow,
                    ItemType.Bullet,
                    ItemType.FistWeapon,
                    ItemType.Dagger,
                    ItemType.OneHandAxe,
                    ItemType.OneHandMace,
                    ItemType.OneHandSword,
                    ItemType.Shield));
        }
        return this.relevantItemTypes;
    }

    // C# CalculationsProtWarr.cs:297-332 SetDefaults.
    public void setDefaults(Character character) {
        character.activeBuffsAdd("Strength of Earth Totem");
        character.activeBuffsAdd("Enhancing Totems (Agility/Strength)");
        character.activeBuffsAdd("Devotion Aura");
        character.activeBuffsAdd("Improved Devotion Aura (Armor)");
        character.activeBuffsAdd("Inspiration");
        character.activeBuffsAdd("Blessing of Might");
        character.activeBuffsAdd("Improved Blessing of Might");
        character.activeBuffsAdd("Unleashed Rage");
        character.activeBuffsAdd("Sanctified Retribution");
        character.activeBuffsAdd("Renewed Hope");
        character.activeBuffsAdd("Swift Retribution");
        character.activeBuffsAdd("Commanding Shout");
        character.activeBuffsAdd("Commanding Presence (Health)");
        character.activeBuffsAdd("Leader of the Pack");
        character.activeBuffsAdd("Windfury Totem");
        character.activeBuffsAdd("Improved Windfury Totem");
        character.activeBuffsAdd("Power Word: Fortitude");
        character.activeBuffsAdd("Improved Power Word: Fortitude");
        character.activeBuffsAdd("Mark of the Wild");
        character.activeBuffsAdd("Improved Mark of the Wild");
        character.activeBuffsAdd("Blessing of Kings");
        character.activeBuffsAdd("Blessing of Kings (Str/Sta Bonus)");
        character.activeBuffsAdd("Sunder Armor");
        character.activeBuffsAdd("Faerie Fire");
        character.activeBuffsAdd("Trauma");
        character.activeBuffsAdd("Heart of the Crusader");
        character.activeBuffsAdd("Insect Swarm");
        character.activeBuffsAdd("Thunder Clap");
        character.activeBuffsAdd("Improved Thunder Clap");
        character.activeBuffsAdd("Flask of Stoneblood");
        if (character.getPrimaryProfession() == rawr.base.character.Profession.Alchemy || character.getSecondaryProfession() == rawr.base.character.Profession.Alchemy)
            character.activeBuffsAdd("Flask of Stoneblood (Mixology)");
        character.activeBuffsAdd("Fish Feast");
    }

    // C# CalculationsProtWarr.cs:334-367 GetRelevantGlyphs.
    private static List<String> relevantGlyphs;

    public List<String> getRelevantGlyphs() {
        if (relevantGlyphs == null) {
            relevantGlyphs = new ArrayList<String>();
            relevantGlyphs.add("Glyph of Barbaric Insults");
            relevantGlyphs.add("Glyph of Blocking");
            relevantGlyphs.add("Glyph of Cleaving");
            relevantGlyphs.add("Glyph of Devastate");
            relevantGlyphs.add("Glyph of Enraged Regeneration");
            relevantGlyphs.add("Glyph of Heroic Strike");
            relevantGlyphs.add("Glyph of Intervene");
            relevantGlyphs.add("Glyph of Last Stand");
            relevantGlyphs.add("Glyph of Rapid Charge");
            relevantGlyphs.add("Glyph of Rending");
            relevantGlyphs.add("Glyph of Resonating Power");
            relevantGlyphs.add("Glyph of Revenge");
            relevantGlyphs.add("Glyph of Shield Wall");
            relevantGlyphs.add("Glyph of Shockwave");
            relevantGlyphs.add("Glyph of Spell Reflection");
            relevantGlyphs.add("Glyph of Sunder Armor");
            relevantGlyphs.add("Glyph of Taunt");
            relevantGlyphs.add("Glyph of Vigilance");

            relevantGlyphs.add("Glyph of Battle");
            relevantGlyphs.add("Glyph of Bloodrage");
            relevantGlyphs.add("Glyph of Charge");
            relevantGlyphs.add("Glyph of Command");
            relevantGlyphs.add("Glyph of Mocking Blow");
            relevantGlyphs.add("Glyph of Thunder Clap");
        }
        return relevantGlyphs;
    }

    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.Warrior;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationProtWarr();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsProtWarr();
    }

    // C# CalculationsProtWarr.cs:373-380 DeserializeDataObject (XmlSerializer; JDK-XML reader here,
    // same framework adaptation as CalculationsHealadin/Bear/Cat).
    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            CalculationOptionsProtWarr calcOpts = new CalculationOptionsProtWarr();
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

    public static CalculationsProtWarr instance;

    public static CalculationsProtWarr getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("ProtWarr", CalculationsProtWarr.class);
    }

    public static void register() {
        Calculations.registerModel("ProtWarr", CalculationsProtWarr.class);
    }

    public CalculationsProtWarr() {
        instance = this;
    }

    // C# CalculationsProtWarr.cs:383-489 GetCharacterCalculations.
    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        CharacterCalculationsProtWarr calculatedStats = new CharacterCalculationsProtWarr();
        CalculationOptionsProtWarr options = character.getCalculationOptions() instanceof CalculationOptionsProtWarr
                ? (CalculationOptionsProtWarr) character.getCalculationOptions()
                : new CalculationOptionsProtWarr();
        Stats stats = getCharacterStats(character, additionalItem, options);

        DefendModel dm = new DefendModel(character, stats, options);
        AttackModel am = new AttackModel(character, stats, options, AttackModelMode.Optimal);

        if (needsDisplayCalculations) {
            calculatedStats.defense = (float) Math.floor(stats.getDefense() + StatConversion.getDefenseFromRating(stats.getDefenseRating(), CharacterClass.Warrior));
            calculatedStats.critReduction = Lookup.avoidanceChance(character, stats, HitResult.Crit, options.getTargetLevel());
            calculatedStats.defenseRatingNeeded = StatConversion.getDefenseRatingNeeded(character, stats, options.getTargetLevel());
            calculatedStats.armorReduction = Lookup.armorReduction(character, stats, options.getTargetLevel());

            calculatedStats.baseAttackerSpeed = options.getBossAttackSpeed();
            calculatedStats.attackerSpeed = dm.parryModel.getBossAttackSpeed();
            calculatedStats.damageTakenPerHit = dm.getDamagePerHit();
            calculatedStats.damageTakenPerBlock = dm.getDamagePerBlock();
            calculatedStats.damageTakenPerCritBlock = dm.getDamagePerCritBlock();
            calculatedStats.damageTakenPerCrit = dm.getDamagePerCrit();
            calculatedStats.damageTaken = dm.getDamagePerSecond();

            calculatedStats.arcaneReduction = (1.0f - Lookup.magicReduction(character, stats, DamageType.Arcane, options.getTargetLevel()));
            calculatedStats.fireReduction = (1.0f - Lookup.magicReduction(character, stats, DamageType.Fire, options.getTargetLevel()));
            calculatedStats.frostReduction = (1.0f - Lookup.magicReduction(character, stats, DamageType.Frost, options.getTargetLevel()));
            calculatedStats.natureReduction = (1.0f - Lookup.magicReduction(character, stats, DamageType.Nature, options.getTargetLevel()));
            calculatedStats.shadowReduction = (1.0f - Lookup.magicReduction(character, stats, DamageType.Shadow, options.getTargetLevel()));

            calculatedStats.crit = am.abilities.get(Ability.None).attackTable.getCritical();
            calculatedStats.expertise = Lookup.bonusExpertisePercentage(character, stats);
            calculatedStats.haste = Lookup.bonusHastePercentage(character, stats);
            calculatedStats.armorPenetration = Lookup.bonusArmorPenetrationPercentage(character, stats);
            calculatedStats.weaponSpeed = Lookup.weaponSpeed(character, stats);
        }

        calculatedStats.targetLevel = options.getTargetLevel();
        calculatedStats.activeBuffs = new ArrayList<Buff>(character.getActiveBuffs());
        calculatedStats.abilities = am.abilities;
        calculatedStats.basicStats = stats;

        calculatedStats.miss = dm.defendTable.getMiss();
        calculatedStats.dodge = dm.defendTable.getDodge();
        calculatedStats.parry = dm.defendTable.getParry();
        calculatedStats.block = dm.defendTable.getBlock();
        calculatedStats.dodgePlusMissPlusParry = calculatedStats.dodge + calculatedStats.miss + calculatedStats.parry;
        calculatedStats.dodgePlusMissPlusParryPlusBlock = calculatedStats.dodgePlusMissPlusParry + calculatedStats.block;
        calculatedStats.blockValue = stats.getBlockValue();
        calculatedStats.critVulnerability = dm.defendTable.getCritical();
        calculatedStats.guaranteedReduction = dm.getGuaranteedReduction();
        calculatedStats.totalMitigation = dm.getMitigation();

        calculatedStats.arcaneSurvivalPoints = stats.getHealth() / Lookup.magicReduction(character, stats, DamageType.Arcane, options.getTargetLevel());
        calculatedStats.fireSurvivalPoints = stats.getHealth() / Lookup.magicReduction(character, stats, DamageType.Fire, options.getTargetLevel());
        calculatedStats.frostSurvivalPoints = stats.getHealth() / Lookup.magicReduction(character, stats, DamageType.Frost, options.getTargetLevel());
        calculatedStats.natureSurvivalPoints = stats.getHealth() / Lookup.magicReduction(character, stats, DamageType.Nature, options.getTargetLevel());
        calculatedStats.shadowSurvivalPoints = stats.getHealth() / Lookup.magicReduction(character, stats, DamageType.Shadow, options.getTargetLevel());

        calculatedStats.hit = Lookup.bonusHitPercentage(character, stats);
        calculatedStats.avoidedAttacks = am.abilities.get(Ability.None).attackTable.getAnyMiss();
        calculatedStats.dodgedAttacks = am.abilities.get(Ability.None).attackTable.getDodge();
        calculatedStats.parriedAttacks = am.abilities.get(Ability.None).attackTable.getParry();
        calculatedStats.missedAttacks = am.abilities.get(Ability.None).attackTable.getMiss();

        calculatedStats.heroicStrikeFrequency = options.getHeroicStrikeFrequency();
        calculatedStats.threatPerSecond = am.getThreatPerSecond();
        calculatedStats.threatModelName = am.getShortName();
        calculatedStats.threatModel = am.getName() + "\n" + am.getDescription();
        calculatedStats.totalDamagePerSecond = am.getDamagePerSecond();

        calculatedStats.tankPoints = dm.getTankPoints();
        calculatedStats.burstTime = dm.getBurstTime();
        calculatedStats.rankingMode = options.getRankingMode();
        calculatedStats.setThreatPoints(options.getThreatScale() * am.getThreatPerSecond());
        switch (options.getRankingMode()) {
            case 2:
                // Tank Points Mode
                calculatedStats.setSurvivalPoints(dm.getEffectiveHealth());
                calculatedStats.setMitigationPoints(dm.getTankPoints() - dm.getEffectiveHealth());
                calculatedStats.setThreatPoints(calculatedStats.getThreatPoints() * 3.0f);
                break;
            case 3:
                // Burst Time Mode
                float threatScale = (float) Math.pow((double) options.getBossAttackValue() / 25000.0d, 4);
                calculatedStats.setSurvivalPoints(dm.getBurstTime() * 100.0f);
                calculatedStats.setMitigationPoints(0.0f);
                calculatedStats.setThreatPoints((calculatedStats.getThreatPoints() / threatScale) * 2.0f);
                break;
            case 4:
                // Damage Output Mode
                calculatedStats.setSurvivalPoints(0.0f);
                calculatedStats.setMitigationPoints(0.0f);
                calculatedStats.setThreatPoints(calculatedStats.totalDamagePerSecond);
                break;
            default:
                // Mitigation Scale Mode
                calculatedStats.setSurvivalPoints((dm.getEffectiveHealth()) / 10.0f);
                calculatedStats.setMitigationPoints(dm.getMitigation() * options.getBossAttackValue() * options.getMitigationScale() * 10.0f);
                calculatedStats.setThreatPoints(calculatedStats.getThreatPoints() / 10.0f);
                break;
        }
        //calculatedStats.OverallPoints = calculatedStats.MitigationPoints + calculatedStats.SurvivalPoints + calculatedStats.ThreatPoints;

        return calculatedStats;
    }

    public Stats getCharacterStats(Character character, Item additionalItem) {
        CalculationOptionsProtWarr options = character.getCalculationOptions() instanceof CalculationOptionsProtWarr
                ? (CalculationOptionsProtWarr) character.getCalculationOptions()
                : new CalculationOptionsProtWarr();
        return getCharacterStats(character, additionalItem, options);
    }

    // C# CalculationsProtWarr.cs:497-586 GetCharacterStats.
    public Stats getCharacterStats(Character character, Item additionalItem, CalculationOptionsProtWarr options) {
        WarriorTalents talents = character.getWarriorTalents();
        Stats statsTotal = new Stats();

        // Items and Buffs
        Stats statsItemsBuffs = new Stats();
        accumulateItemStats(statsItemsBuffs, character, additionalItem);
        accumulateBuffsStats(statsItemsBuffs, character.getActiveBuffs());
        statsTotal.accumulate(statsItemsBuffs);

        // Race Stats
        Stats statsRace = BaseStats.getBaseStats(character.getLevel(), CharacterClass.Warrior, character.getRace());
        statsRace.setExpertise(statsRace.getExpertise() + BaseStats.getRacialExpertise(character, ItemSlot.MainHand));
        statsTotal.accumulate(statsRace);

        // Talents
        Stats statsTalents = new Stats();
        statsTalents.setParry(talents.getDeflection() * 0.01f);
        statsTalents.setDodge(talents.getAnticipation() * 0.01f);
        statsTalents.setBlock(talents.getShieldSpecialization() * 0.01f);
        statsTalents.setBonusBlockValueMultiplier(talents.getShieldMastery() * 0.15f + (talents.isGlyphOfBlocking() ? 0.1f : 0.0f));
        ItemInstance mainHand = character.get(CharacterSlot.MainHand);
        statsTalents.setPhysicalCrit(talents.getCruelty() * 0.01f +
                ((mainHand != null && (mainHand.getType() == ItemType.OneHandAxe))
                        ? talents.getPoleaxeSpecialization() * 0.01f : 0.0f));
        statsTalents.setBonusCritMultiplier(
                ((mainHand != null && (mainHand.getType() == ItemType.OneHandAxe))
                        ? talents.getPoleaxeSpecialization() * 0.01f : 0.0f));
        statsTalents.setBonusDamageMultiplier(
                ((mainHand != null &&
                        (mainHand.getType() == ItemType.OneHandAxe ||
                                mainHand.getType() == ItemType.OneHandMace ||
                                mainHand.getType() == ItemType.OneHandSword ||
                                mainHand.getType() == ItemType.Dagger ||
                                mainHand.getType() == ItemType.FistWeapon))
                        ? talents.getOneHandedWeaponSpecialization() * 0.02f : 0f));
        statsTalents.setBonusStaminaMultiplier(talents.getVitality() * 0.03f + talents.getStrengthOfArms() * 0.02f);
        statsTalents.setBonusStrengthMultiplier(talents.getVitality() * 0.02f + talents.getStrengthOfArms() * 0.02f);
        statsTalents.setExpertise(talents.getVitality() * 2.0f + talents.getStrengthOfArms() * 2.0f);
        statsTalents.setBonusShieldSlamDamage(talents.getGagOrder() * 0.05f);
        statsTalents.setDevastateCritIncrease(talents.getSwordAndBoard() * 0.05f);
        statsTalents.setBaseArmorMultiplier(talents.getToughness() * 0.02f);
        statsTalents.setPhysicalHaste(talents.getBloodFrenzy() * 0.03f);
        statsTalents.setPhysicalHit(talents.getPrecision() * 0.01f);
        statsTotal.accumulate(statsTalents);

        // Base Stats
        statsTotal.setBaseAgility(statsRace.getAgility() + statsTalents.getAgility());
        statsTotal.setStamina((float) Math.floor((statsRace.getStamina() + statsTalents.getStamina()) * (1.0f + statsTotal.getBonusStaminaMultiplier())));
        statsTotal.setStamina(statsTotal.getStamina() + (float) Math.floor(statsItemsBuffs.getStamina() * (1.0f + statsTotal.getBonusStaminaMultiplier())));
        statsTotal.setStrength((float) Math.floor((statsRace.getStrength() + statsTalents.getStrength()) * (1.0f + statsTotal.getBonusStrengthMultiplier())));
        statsTotal.setStrength(statsTotal.getStrength() + (float) Math.floor(statsItemsBuffs.getStrength() * (1.0f + statsTotal.getBonusStrengthMultiplier())));
        statsTotal.setAgility((float) Math.floor((statsRace.getAgility() + statsTalents.getAgility()) * (1.0f + statsTotal.getBonusAgilityMultiplier())));
        statsTotal.setAgility(statsTotal.getAgility() + (float) Math.floor(statsItemsBuffs.getAgility() * (1.0f + statsTotal.getBonusAgilityMultiplier())));
        statsTotal.setHealth(statsTotal.getHealth() + StatConversion.getHealthFromStamina(statsTotal.getStamina(), CharacterClass.Warrior));
        statsTotal.setHealth((float) Math.floor(statsTotal.getHealth() * (1.0f + statsTotal.getBonusHealthMultiplier())));

        // Calculate Procs and Special Effects
        statsTotal.accumulate(getSpecialEffectStats(character, statsTotal, options));

        // Defensive Stats
        statsTotal.setArmor((float) Math.ceil(statsTotal.getArmor() * (1.0f + statsTotal.getBaseArmorMultiplier())));
        statsTotal.setBonusArmor(statsTotal.getBonusArmor() + statsTotal.getAgility() * 2.0f);
        statsTotal.setArmor(statsTotal.getArmor() + (float) Math.ceil(statsTotal.getBonusArmor() * (1.0f + statsTotal.getBonusArmorMultiplier())));

        statsTotal.setBlockValue(statsTotal.getBlockValue() + (float) Math.floor(StatConversion.getBlockValueFromStrength(statsTotal.getStrength(), CharacterClass.Warrior) - 10.0f));
        statsTotal.setBlockValue((float) Math.floor(statsTotal.getBlockValue() * (1 + statsTotal.getBonusBlockValueMultiplier())));

        statsTotal.setNatureResistance(statsTotal.getNatureResistance() + statsTotal.getNatureResistanceBuff());
        statsTotal.setFireResistance(statsTotal.getFireResistance() + statsTotal.getFireResistanceBuff());
        statsTotal.setFrostResistance(statsTotal.getFrostResistance() + statsTotal.getFrostResistanceBuff());
        statsTotal.setShadowResistance(statsTotal.getShadowResistance() + statsTotal.getShadowResistanceBuff());
        statsTotal.setArcaneResistance(statsTotal.getArcaneResistance() + statsTotal.getArcaneResistanceBuff());

        // Highest Stat Effects
        if (statsTotal.getStrength() > statsTotal.getAgility())
            statsTotal.setStrength(statsTotal.getStrength() + (float) Math.floor((statsTotal.getHighestStat() + statsTotal.getParagon()) * (1.0f + statsTotal.getBonusStrengthMultiplier())));
        else
            statsTotal.setAgility(statsTotal.getAgility() + (float) Math.floor((statsTotal.getHighestStat() + statsTotal.getParagon()) * (1.0f + statsTotal.getBonusAgilityMultiplier())));

        // Final Attack Power
        statsTotal.setAttackPower(statsTotal.getAttackPower() + statsTotal.getStrength() * 2.0f + (float) Math.floor(talents.getArmoredToTheTeeth() * statsTotal.getArmor() / 108.0f));
        statsTotal.setAttackPower((float) Math.floor(statsTotal.getAttackPower() * (1.0f + statsTotal.getBonusAttackPowerMultiplier())));

        return statsTotal;
    }

    // C# CalculationsProtWarr.cs:588-644 GetSpecialEffectStats.
    private Stats getSpecialEffectStats(Character character, Stats stats, CalculationOptionsProtWarr options) {
        Stats statsSpecialEffects = new Stats();

        float weaponSpeed = 1.0f;
        ItemInstance mainHand = character.get(CharacterSlot.MainHand);
        if (mainHand != null)
            weaponSpeed = mainHand.getSpeed();

        AttackModel am = new AttackModel(character, stats, options, AttackModelMode.Optimal);

        for (SpecialEffect effect : stats.specialEffects()) {
            switch (effect.getTrigger()) {
                case Use:
                    effect.accumulateAverageStats(statsSpecialEffects, 0.0f, 1.0f, weaponSpeed, 0f);
                    // Trial of the Crusader Stacking Use Effect Trinkets
                    for (SpecialEffect childEffect : effect.getStats().specialEffects()) {
                        if (childEffect.getTrigger() == Trigger.DamageTaken) {
                            statsSpecialEffects.accumulate(Stats.multiply(childEffect.getStats(), (effect.getAverageUptime(0.0f, 1.0f) *
                                    childEffect.getAverageStackSize((1.0f / am.getAttackerSwingsPerSecond()), (am.getAttackerHitsPerSecond() / am.getAttackerSwingsPerSecond()), weaponSpeed, effect.getDuration()))));
                        }
                    }
                    break;
                case MeleeHit:
                case PhysicalHit:
                    effect.accumulateAverageStats(statsSpecialEffects, (1.0f / am.getWeaponAttacksPerSecond()), (am.getHitsPerSecond() / am.getWeaponAttacksPerSecond()), weaponSpeed, 0f);
                    break;
                case MeleeCrit:
                case PhysicalCrit:
                    effect.accumulateAverageStats(statsSpecialEffects, (1.0f / am.getWeaponAttacksPerSecond()), (am.getCritsPerSecond() / am.getWeaponAttacksPerSecond()), weaponSpeed, 0f);
                    break;
                case DoTTick:
                    if (character.getWarriorTalents().getDeepWounds() > 0)
                        effect.accumulateAverageStats(statsSpecialEffects, 2.0f, 1.0f, weaponSpeed, 0f);
                    break;
                case DamageDone:
                case DamageOrHealingDone:
                    effect.accumulateAverageStats(statsSpecialEffects, (1.0f / am.getWeaponAttacksPerSecond()), (am.getHitsPerSecond() / am.getWeaponAttacksPerSecond()), weaponSpeed, 0f);
                    break;
                case DamageTaken:
                    effect.accumulateAverageStats(statsSpecialEffects, (1.0f / am.getAttackerSwingsPerSecond()), (am.getAttackerHitsPerSecond() / am.getAttackerSwingsPerSecond()), 3.0f, 0f);
                    break;
            }
        }

        // Base Stats
        statsSpecialEffects.setStamina((float) Math.floor(statsSpecialEffects.getStamina() * (1.0f + stats.getBonusStaminaMultiplier())));
        statsSpecialEffects.setStrength((float) Math.floor(statsSpecialEffects.getStrength() * (1.0f + stats.getBonusStrengthMultiplier())));
        statsSpecialEffects.setAgility((float) Math.floor(statsSpecialEffects.getAgility() * (1.0f + stats.getBonusAgilityMultiplier())));
        statsSpecialEffects.setHealth(statsSpecialEffects.getHealth() + (statsSpecialEffects.getStamina() * 10.0f) + statsSpecialEffects.getBattlemasterHealth());
        statsSpecialEffects.setHealth((float) Math.floor(statsSpecialEffects.getHealth() * (1.0f + stats.getBonusHealthMultiplier())));

        return statsSpecialEffects;
    }

    // C# CalculationsProtWarr.cs:646-968 GetCustomChartData.
    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        CharacterCalculationsProtWarr calculations = (CharacterCalculationsProtWarr) getCharacterCalculations(character);

        switch (chartName) {
            /*
            #region Rotation Damage and Threat
            ...
            #endregion
            */
            // region Ability Damage and Threat
            case "Ability Damage":
            case "Ability Threat": {
                // Vigilance will need to be ignores as it is outside the scope of this
                ComparisonCalculationBase[] comparisons = new ComparisonCalculationBase[calculations.abilities.getCount() - 1];
                // Index Deep Wounds for including in everything that can crit
                AbilityModel deepWounds = calculations.abilities.get(Ability.DeepWounds);

                int j = 0;
                for (AbilityModel ability : calculations.abilities.values()) {
                    if (ability.getAbility() != Ability.Vigilance) {
                        ComparisonCalculationProtWarr comparison = new ComparisonCalculationProtWarr();
                        comparison.setName(ability.getName());

                        // Deep Wounds gets colored red to make the integrated parts easier to understand
                        if (ability.getAbility() == Ability.DeepWounds) {
                            if ("Ability Damage".equals(chartName))
                                comparison.setMitigationPoints(ability.getDamage());
                            else
                                comparison.setMitigationPoints(ability.getThreat());
                        } else {
                            if ("Ability Damage".equals(chartName)) {
                                comparison.setThreatPoints(ability.getDamage());
                                comparison.setMitigationPoints(ability.getCritPercentage() * deepWounds.getDamage());
                            } else {
                                comparison.setThreatPoints(ability.getThreat());
                                comparison.setMitigationPoints(ability.getCritPercentage() * deepWounds.getThreat());
                            }
                        }

                        comparison.setOverallPoints(comparison.getSurvivalPoints() + comparison.getThreatPoints() + comparison.getMitigationPoints());
                        comparisons[j] = comparison;
                        j++;
                    }
                }
                return comparisons;
            }
            // endregion
            // region Combat Table
            case "Combat Table": {
                ComparisonCalculationProtWarr calcMiss = new ComparisonCalculationProtWarr();
                ComparisonCalculationProtWarr calcDodge = new ComparisonCalculationProtWarr();
                ComparisonCalculationProtWarr calcParry = new ComparisonCalculationProtWarr();
                ComparisonCalculationProtWarr calcBlock = new ComparisonCalculationProtWarr();
                ComparisonCalculationProtWarr calcCrit = new ComparisonCalculationProtWarr();
                ComparisonCalculationProtWarr calcCrush = new ComparisonCalculationProtWarr();
                ComparisonCalculationProtWarr calcHit = new ComparisonCalculationProtWarr();
                if (calculations != null) {
                    calcMiss.setName("Miss");
                    calcDodge.setName("Dodge");
                    calcParry.setName("Parry");
                    calcBlock.setName("Block");
                    calcCrit.setName("Crit");
                    calcCrush.setName("Crush");
                    calcHit.setName("Hit");

                    calcMiss.setOverallPoints(calcMiss.getMitigationPoints());
                    calcMiss.setMitigationPoints(calculations.miss * 100.0f);
                    calcMiss.setOverallPoints(calcMiss.getMitigationPoints());
                    calcDodge.setMitigationPoints(calculations.dodge * 100.0f);
                    calcDodge.setOverallPoints(calcDodge.getMitigationPoints());
                    calcParry.setMitigationPoints(calculations.parry * 100.0f);
                    calcParry.setOverallPoints(calcParry.getMitigationPoints());
                    calcBlock.setMitigationPoints(calculations.block * 100.0f);
                    calcBlock.setOverallPoints(calcBlock.getMitigationPoints());
                    calcCrit.setSurvivalPoints(calculations.critVulnerability * 100.0f);
                    calcHit.setSurvivalPoints((1.0f - calculations.dodgePlusMissPlusParry - calculations.block - calculations.critVulnerability) * 100.0f);
                }
                return new ComparisonCalculationBase[] { calcMiss, calcDodge, calcParry, calcBlock, calcCrit, calcCrush, calcHit };
            }
            // endregion
            // region Item Budget
            case "Item Budget":
                CharacterCalculationsProtWarr calcBaseValue = (CharacterCalculationsProtWarr) getCharacterCalculations(character);
                CharacterCalculationsProtWarr calcDodgeValue = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("DodgeRating", 10f));
                CharacterCalculationsProtWarr calcParryValue = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("ParryRating", 10f));
                CharacterCalculationsProtWarr calcBlockValue = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("BlockRating", 10f));
                CharacterCalculationsProtWarr calcHasteValue = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("HasteRating", 10f));
                CharacterCalculationsProtWarr calcExpertiseValue = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("ExpertiseRating", 10f));
                CharacterCalculationsProtWarr calcHitValue = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("HitRating", 10f));
                CharacterCalculationsProtWarr calcBlockValueValue = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("BlockValue", (10f / 0.65f) * 2.0f));
                CharacterCalculationsProtWarr calcHealthValue = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("Health", (10f * 10f) / 0.667f));
                CharacterCalculationsProtWarr calcResilValue = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("Resilience", 10f));

                //Differential Calculations for Agi
                CharacterCalculationsProtWarr calcAtAdd = calcBaseValue;
                float agiToAdd = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtAdd.getOverallPoints() && agiToAdd < 2) {
                    agiToAdd += 0.01f;
                    calcAtAdd = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("Agility", agiToAdd));
                }

                CharacterCalculationsProtWarr calcAtSubtract = calcBaseValue;
                float agiToSubtract = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtSubtract.getOverallPoints() && agiToSubtract > -2) {
                    agiToSubtract -= 0.01f;
                    calcAtSubtract = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("Agility", agiToSubtract));
                }
                agiToSubtract += 0.01f;

                ComparisonCalculationProtWarr comparisonAgi = new ComparisonCalculationProtWarr();
                comparisonAgi.setName("10 Agility");
                comparisonAgi.setMitigationPoints(10 * (calcAtAdd.getMitigationPoints() - calcBaseValue.getMitigationPoints()) / (agiToAdd - agiToSubtract));
                comparisonAgi.setSurvivalPoints(10 * (calcAtAdd.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()) / (agiToAdd - agiToSubtract));
                comparisonAgi.setThreatPoints(10 * (calcAtAdd.getThreatPoints() - calcBaseValue.getThreatPoints()) / (agiToAdd - agiToSubtract));
                comparisonAgi.setOverallPoints(10 * (calcAtAdd.getOverallPoints() - calcBaseValue.getOverallPoints()) / (agiToAdd - agiToSubtract));

                //Differential Calculations for Str
                calcAtAdd = calcBaseValue;
                float strToAdd = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtAdd.getOverallPoints() && strToAdd < 2) {
                    strToAdd += 0.01f;
                    calcAtAdd = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("Strength", strToAdd));
                }

                calcAtSubtract = calcBaseValue;
                float strToSubtract = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtSubtract.getOverallPoints() && strToSubtract > -2) {
                    strToSubtract -= 0.01f;
                    calcAtSubtract = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("Strength", strToSubtract));
                }
                strToSubtract += 0.01f;

                ComparisonCalculationProtWarr comparisonStr = new ComparisonCalculationProtWarr();
                comparisonStr.setName("10 Strength");
                comparisonStr.setMitigationPoints(10 * (calcAtAdd.getMitigationPoints() - calcBaseValue.getMitigationPoints()) / (strToAdd - strToSubtract));
                comparisonStr.setSurvivalPoints(10 * (calcAtAdd.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()) / (strToAdd - strToSubtract));
                comparisonStr.setThreatPoints(10 * (calcAtAdd.getThreatPoints() - calcBaseValue.getThreatPoints()) / (strToAdd - strToSubtract));
                comparisonStr.setOverallPoints(10 * (calcAtAdd.getOverallPoints() - calcBaseValue.getOverallPoints()) / (strToAdd - strToSubtract));

                //Differential Calculations for Def
                calcAtAdd = calcBaseValue;
                float defToAdd = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtAdd.getOverallPoints() && defToAdd < 20) {
                    defToAdd += 0.01f;
                    calcAtAdd = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("DefenseRating", defToAdd));
                }

                calcAtSubtract = calcBaseValue;
                float defToSubtract = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtSubtract.getOverallPoints() && defToSubtract > -20) {
                    defToSubtract -= 0.01f;
                    calcAtSubtract = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("DefenseRating", defToSubtract));
                }
                defToSubtract += 0.01f;

                ComparisonCalculationProtWarr comparisonDef = new ComparisonCalculationProtWarr();
                comparisonDef.setName("10 Defense Rating");
                comparisonDef.setMitigationPoints(10 * (calcAtAdd.getMitigationPoints() - calcBaseValue.getMitigationPoints()) / (defToAdd - defToSubtract));
                comparisonDef.setSurvivalPoints(10 * (calcAtAdd.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()) / (defToAdd - defToSubtract));
                comparisonDef.setThreatPoints(10 * (calcAtAdd.getThreatPoints() - calcBaseValue.getThreatPoints()) / (defToAdd - defToSubtract));
                comparisonDef.setOverallPoints(10 * (calcAtAdd.getOverallPoints() - calcBaseValue.getOverallPoints()) / (defToAdd - defToSubtract));

                //Differential Calculations for AC
                calcAtAdd = calcBaseValue;
                float acToAdd = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtAdd.getOverallPoints() && acToAdd < 2) {
                    acToAdd += 0.01f;
                    calcAtAdd = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("Armor", acToAdd));
                }

                calcAtSubtract = calcBaseValue;
                float acToSubtract = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtSubtract.getOverallPoints() && acToSubtract > -2) {
                    acToSubtract -= 0.01f;
                    calcAtSubtract = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("Armor", acToSubtract));
                }
                acToSubtract += 0.01f;

                ComparisonCalculationProtWarr comparisonAC = new ComparisonCalculationProtWarr();
                comparisonAC.setName("100 Armor");
                comparisonAC.setMitigationPoints(100f * (calcAtAdd.getMitigationPoints() - calcBaseValue.getMitigationPoints()) / (acToAdd - acToSubtract));
                comparisonAC.setSurvivalPoints(100f * (calcAtAdd.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()) / (acToAdd - acToSubtract));
                comparisonAC.setThreatPoints(100f * (calcAtAdd.getThreatPoints() - calcBaseValue.getThreatPoints()) / (acToAdd - acToSubtract));
                comparisonAC.setOverallPoints(100f * (calcAtAdd.getOverallPoints() - calcBaseValue.getOverallPoints()) / (acToAdd - acToSubtract));

                //Differential Calculations for Sta
                calcAtAdd = calcBaseValue;
                float staToAdd = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtAdd.getOverallPoints() && staToAdd < 2) {
                    staToAdd += 0.01f;
                    calcAtAdd = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("Stamina", staToAdd));
                }

                calcAtSubtract = calcBaseValue;
                float staToSubtract = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtSubtract.getOverallPoints() && staToSubtract > -2) {
                    staToSubtract -= 0.01f;
                    calcAtSubtract = (CharacterCalculationsProtWarr) getCharacterCalculations(character, itemWithStat("Stamina", staToSubtract));
                }
                staToSubtract += 0.01f;

                ComparisonCalculationProtWarr comparisonSta = new ComparisonCalculationProtWarr();
                comparisonSta.setName("15 Stamina");
                comparisonSta.setMitigationPoints((10f / 0.667f) * (calcAtAdd.getMitigationPoints() - calcBaseValue.getMitigationPoints()) / (staToAdd - staToSubtract));
                comparisonSta.setSurvivalPoints((10f / 0.667f) * (calcAtAdd.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()) / (staToAdd - staToSubtract));
                comparisonSta.setThreatPoints((10f / 0.667f) * (calcAtAdd.getThreatPoints() - calcBaseValue.getThreatPoints()) / (staToAdd - staToSubtract));
                comparisonSta.setOverallPoints((10f / 0.667f) * (calcAtAdd.getOverallPoints() - calcBaseValue.getOverallPoints()) / (staToAdd - staToSubtract));

                ComparisonCalculationProtWarr comparisonDodge = new ComparisonCalculationProtWarr();
                comparisonDodge.setName("10 Dodge Rating");
                comparisonDodge.setMitigationPoints((calcDodgeValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonDodge.setSurvivalPoints((calcDodgeValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonDodge.setThreatPoints((calcDodgeValue.getThreatPoints() - calcBaseValue.getThreatPoints()));
                comparisonDodge.setOverallPoints((calcDodgeValue.getOverallPoints() - calcBaseValue.getOverallPoints()));

                ComparisonCalculationProtWarr comparisonParry = new ComparisonCalculationProtWarr();
                comparisonParry.setName("10 Parry Rating");
                comparisonParry.setMitigationPoints((calcParryValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonParry.setSurvivalPoints((calcParryValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonParry.setThreatPoints((calcParryValue.getThreatPoints() - calcBaseValue.getThreatPoints()));
                comparisonParry.setOverallPoints((calcParryValue.getOverallPoints() - calcBaseValue.getOverallPoints()));

                ComparisonCalculationProtWarr comparisonBlock = new ComparisonCalculationProtWarr();
                comparisonBlock.setName("10 Block Rating");
                comparisonBlock.setMitigationPoints((calcBlockValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonBlock.setSurvivalPoints((calcBlockValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonBlock.setThreatPoints((calcBlockValue.getThreatPoints() - calcBaseValue.getThreatPoints()));
                comparisonBlock.setOverallPoints((calcBlockValue.getOverallPoints() - calcBaseValue.getOverallPoints()));

                ComparisonCalculationProtWarr comparisonHaste = new ComparisonCalculationProtWarr();
                comparisonHaste.setName("10 Haste Rating");
                comparisonHaste.setMitigationPoints((calcHasteValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonHaste.setSurvivalPoints((calcHasteValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonHaste.setThreatPoints((calcHasteValue.getThreatPoints() - calcBaseValue.getThreatPoints()));
                comparisonHaste.setOverallPoints((calcHasteValue.getOverallPoints() - calcBaseValue.getOverallPoints()));

                ComparisonCalculationProtWarr comparisonExpertise = new ComparisonCalculationProtWarr();
                comparisonExpertise.setName("10 Expertise Rating");
                comparisonExpertise.setMitigationPoints((calcExpertiseValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonExpertise.setSurvivalPoints((calcExpertiseValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonExpertise.setThreatPoints((calcExpertiseValue.getThreatPoints() - calcBaseValue.getThreatPoints()));
                comparisonExpertise.setOverallPoints((calcExpertiseValue.getOverallPoints() - calcBaseValue.getOverallPoints()));

                ComparisonCalculationProtWarr comparisonHit = new ComparisonCalculationProtWarr();
                comparisonHit.setName("10 Hit Rating");
                comparisonHit.setMitigationPoints((calcHitValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonHit.setSurvivalPoints((calcHitValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonHit.setThreatPoints((calcHitValue.getThreatPoints() - calcBaseValue.getThreatPoints()));
                comparisonHit.setOverallPoints((calcHitValue.getOverallPoints() - calcBaseValue.getOverallPoints()));

                ComparisonCalculationProtWarr comparisonBlockValue = new ComparisonCalculationProtWarr();
                comparisonBlockValue.setName("30.76 Block Value");
                comparisonBlockValue.setMitigationPoints((calcBlockValueValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonBlockValue.setSurvivalPoints((calcBlockValueValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonBlockValue.setThreatPoints((calcBlockValueValue.getThreatPoints() - calcBaseValue.getThreatPoints()));
                comparisonBlockValue.setOverallPoints((calcBlockValueValue.getOverallPoints() - calcBaseValue.getOverallPoints()));

                ComparisonCalculationProtWarr comparisonHealth = new ComparisonCalculationProtWarr();
                comparisonHealth.setName("150 Health");
                comparisonHealth.setMitigationPoints((calcHealthValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonHealth.setSurvivalPoints((calcHealthValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonHealth.setThreatPoints((calcHealthValue.getThreatPoints() - calcBaseValue.getThreatPoints()));
                comparisonHealth.setOverallPoints((calcHealthValue.getOverallPoints() - calcBaseValue.getOverallPoints()));

                ComparisonCalculationProtWarr comparisonResil = new ComparisonCalculationProtWarr();
                comparisonResil.setName("10 Resilience");
                comparisonResil.setMitigationPoints((calcResilValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonResil.setSurvivalPoints((calcResilValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonResil.setThreatPoints((calcResilValue.getThreatPoints() - calcBaseValue.getThreatPoints()));
                comparisonResil.setOverallPoints((calcResilValue.getOverallPoints() - calcBaseValue.getOverallPoints()));

                return new ComparisonCalculationBase[] {
                        comparisonStr,
                        comparisonAgi,
                        comparisonAC,
                        comparisonSta,
                        comparisonDef,
                        comparisonDodge,
                        comparisonParry,
                        comparisonBlock,
                        comparisonHaste,
                        comparisonExpertise,
                        comparisonHit,
                        comparisonBlockValue,
                        comparisonHealth,
                        comparisonResil,
                };
            // endregion
            default:
                return new ComparisonCalculationBase[0];
        }
    }

    private static Item itemWithStat(String property, float value) {
        Item item = new Item();
        Stats stats = new Stats();
        Stats.StatProperty statProperty = Stats.PROPERTY_BY_NAME.get(property);
        statProperty.mutator().set(stats, value);
        item.setStats(stats);
        return item;
    }

    // C# CalculationsProtWarr.cs:970-987.
    @Override
    public boolean enchantFitsInSlot(Enchant enchant, Character character, ItemSlot slot) {
        // Filters out Non-Shield Offhand Enchants and Ranged Enchants
        if ((slot == ItemSlot.OffHand && enchant.slot != ItemSlot.OffHand) || slot == ItemSlot.Ranged) return false;
        // Filters out Death Knight and Two-Hander Enchants
        if (enchant.name.startsWith("Rune of the") || enchant.slot == ItemSlot.TwoHand) return false;

        return super.enchantFitsInSlot(enchant, character, slot);
    }

    // C# CalculationsProtWarr.cs:980-987.
    @Override
    public boolean itemFitsInSlot(Item item, Character character, CharacterSlot slot, boolean ignoreUnique) {
        // Only Displays Shields in Off-Hand Slot as Devastate is Shield-Only Post-3.2
        if (slot == CharacterSlot.OffHand && item.getType() != ItemType.Shield)
            return false;

        return super.itemFitsInSlot(item, character, slot, ignoreUnique);
    }

    // C# CalculationsProtWarr.cs:989-996.
    @Override
    public boolean isItemRelevant(Item item) {
        // Fishing Poles, they love to muck up the list!
        if (item.getId() == 45991 || item.getId() == 25978)
            return false;

        return super.isItemRelevant(item);
    }

    // C# CalculationsProtWarr.cs:998-1010.
    @Override
    public boolean isBuffRelevant(Buff buff, Character character) {
        boolean notClassSetBonus =
                (("Set Bonuses".equals(buff.group)) && !(
                        buff.name.startsWith("Dreadnaught") ||
                                buff.name.startsWith("Siegebreaker") ||
                                buff.name.startsWith("Wrynn") ||
                                buff.name.startsWith("Ymirjar") ||
                                buff.name.contains("Gladiator")
                ));

        return super.isBuffRelevant(buff, character) && !notClassSetBonus;
    }

    // C# CalculationsProtWarr.cs:1012-1095 GetRelevantStats.
    public Stats getRelevantStats(Stats stats) {
        Stats relevantStats = new Stats();
        relevantStats.setArmor(stats.getArmor());
        relevantStats.setBonusArmor(stats.getBonusArmor());
        relevantStats.setStamina(stats.getStamina());
        relevantStats.setAgility(stats.getAgility());
        relevantStats.setDodge(stats.getDodge());
        relevantStats.setDodgeRating(stats.getDodgeRating());
        relevantStats.setParryRating(stats.getParryRating());
        relevantStats.setBlockRating(stats.getBlockRating());
        relevantStats.setBlockValue(stats.getBlockValue());
        relevantStats.setDefenseRating(stats.getDefenseRating());
        relevantStats.setResilience(stats.getResilience());
        relevantStats.setBonusAgilityMultiplier(stats.getBonusAgilityMultiplier());
        relevantStats.setBonusStrengthMultiplier(stats.getBonusStrengthMultiplier());
        relevantStats.setBonusAttackPowerMultiplier(stats.getBonusAttackPowerMultiplier());
        relevantStats.setBaseArmorMultiplier(stats.getBaseArmorMultiplier());
        relevantStats.setBonusArmorMultiplier(stats.getBonusArmorMultiplier());
        relevantStats.setBonusStaminaMultiplier(stats.getBonusStaminaMultiplier());
        relevantStats.setHealth(stats.getHealth());
        relevantStats.setBattlemasterHealth(stats.getBattlemasterHealth());
        relevantStats.setBonusHealthMultiplier(stats.getBonusHealthMultiplier());
        relevantStats.setDamageTakenMultiplier(stats.getDamageTakenMultiplier());
        relevantStats.setPhysicalDamageTakenMultiplier(stats.getPhysicalDamageTakenMultiplier());
        relevantStats.setMiss(stats.getMiss());
        relevantStats.setArcaneResistance(stats.getArcaneResistance());
        relevantStats.setNatureResistance(stats.getNatureResistance());
        relevantStats.setFireResistance(stats.getFireResistance());
        relevantStats.setFrostResistance(stats.getFrostResistance());
        relevantStats.setShadowResistance(stats.getShadowResistance());
        relevantStats.setArcaneResistanceBuff(stats.getArcaneResistanceBuff());
        relevantStats.setNatureResistanceBuff(stats.getNatureResistanceBuff());
        relevantStats.setFireResistanceBuff(stats.getFireResistanceBuff());
        relevantStats.setFrostResistanceBuff(stats.getFrostResistanceBuff());
        relevantStats.setShadowResistanceBuff(stats.getShadowResistanceBuff());

        relevantStats.setStrength(stats.getStrength());
        relevantStats.setAttackPower(stats.getAttackPower());
        relevantStats.setCritRating(stats.getCritRating());
        relevantStats.setPhysicalCrit(stats.getPhysicalCrit());
        relevantStats.setHitRating(stats.getHitRating());
        relevantStats.setPhysicalHit(stats.getPhysicalHit());
        relevantStats.setHasteRating(stats.getHasteRating());
        relevantStats.setPhysicalHaste(stats.getPhysicalHaste());
        relevantStats.setExpertiseRating(stats.getExpertiseRating());
        relevantStats.setArmorPenetration(stats.getArmorPenetration());
        relevantStats.setArmorPenetrationRating(stats.getArmorPenetrationRating());
        relevantStats.setWeaponDamage(stats.getWeaponDamage());
        relevantStats.setBonusCritMultiplier(stats.getBonusCritMultiplier());
        relevantStats.setThreatIncreaseMultiplier(stats.getThreatIncreaseMultiplier());
        relevantStats.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());
        relevantStats.setBonusBlockValueMultiplier(stats.getBonusBlockValueMultiplier());
        relevantStats.setBonusBleedDamageMultiplier(stats.getBonusBleedDamageMultiplier());
        relevantStats.setBossAttackSpeedMultiplier(stats.getBossAttackSpeedMultiplier());

        relevantStats.setHighestStat(stats.getHighestStat());
        relevantStats.setParagon(stats.getParagon());
        relevantStats.setDeathbringerProc(stats.getDeathbringerProc());
        relevantStats.setBonusShieldSlamDamage(stats.getBonusShieldSlamDamage());
        relevantStats.setBonusShockwaveDamage(stats.getBonusShockwaveDamage());
        relevantStats.setDevastateCritIncrease(stats.getDevastateCritIncrease());
        relevantStats.setBonusDevastateDamage(stats.getBonusDevastateDamage());

        for (SpecialEffect effect : stats.specialEffects()) {
            if ((effect.getTrigger() == Trigger.Use ||
                    effect.getTrigger() == Trigger.MeleeCrit ||
                    effect.getTrigger() == Trigger.MeleeHit ||
                    effect.getTrigger() == Trigger.PhysicalCrit ||
                    effect.getTrigger() == Trigger.PhysicalHit ||
                    effect.getTrigger() == Trigger.DoTTick ||
                    effect.getTrigger() == Trigger.DamageDone ||
                    effect.getTrigger() == Trigger.DamageOrHealingDone ||
                    effect.getTrigger() == Trigger.DamageTaken) && hasRelevantStats(effect.getStats())) {
                relevantStats.addSpecialEffect(effect);
            }
        }

        return relevantStats;
    }

    // C# CalculationsProtWarr.cs:1097-1155 HasRelevantStats.
    @Override
    public boolean hasRelevantStats(Stats stats) {
        // Stats that will automatically mark the item as relevant
        boolean superRelevant =
                (stats.getBonusArmor() + stats.getBonusArmorMultiplier() +
                        stats.getBonusStaminaMultiplier() + stats.getDefenseRating() + stats.getDodge() + stats.getDodgeRating() + stats.getParryRating() +
                        stats.getBlockRating() + stats.getBonusHealthMultiplier() +
                        stats.getDamageTakenMultiplier() + stats.getPhysicalDamageTakenMultiplier() + stats.getMiss() +
                        stats.getArcaneResistance() + stats.getNatureResistance() + stats.getFireResistance() +
                        stats.getFrostResistance() + stats.getShadowResistance() + stats.getArcaneResistanceBuff() +
                        stats.getNatureResistanceBuff() + stats.getFireResistanceBuff() + stats.getFrostResistanceBuff() + stats.getShadowResistanceBuff() +
                        stats.getThreatIncreaseMultiplier() + stats.getBonusBlockValueMultiplier() +
                        stats.getBonusShieldSlamDamage() + stats.getBonusShockwaveDamage() + stats.getDevastateCritIncrease() + stats.getBonusDevastateDamage()
                ) != 0;

        // Stats which are potentially relevant
        boolean relevant =
                (stats.getAgility() + stats.getArmor() +
                        stats.getBonusAgilityMultiplier() + stats.getBonusStrengthMultiplier() + stats.getBonusAttackPowerMultiplier() +
                        stats.getHealth() + stats.getBattlemasterHealth() + stats.getStamina() + stats.getResilience() + stats.getBlockValue() +
                        stats.getStrength() + stats.getAttackPower() + stats.getCritRating() + stats.getHitRating() + stats.getHasteRating() +
                        stats.getPhysicalHit() + stats.getPhysicalHaste() + stats.getPhysicalCrit() +
                        stats.getExpertiseRating() + stats.getArmorPenetration() + stats.getArmorPenetrationRating() + stats.getWeaponDamage() +
                        stats.getBonusCritMultiplier() +
                        stats.getBonusDamageMultiplier() +
                        stats.getBonusBleedDamageMultiplier() + stats.getBossAttackSpeedMultiplier() +
                        stats.getHighestStat() + stats.getParagon() + stats.getDeathbringerProc() +
                        stats.getMovementSpeed() + stats.getFearDurReduc() + stats.getStunDurReduc() + stats.getSnareRootDurReduc()
                ) != 0;

        // Stats which mark the item as irrelevant if not super-relevant
        boolean notRelevant =
                (stats.getMp5() + stats.getSpellPower() + stats.getMana() + stats.getSpellPenetration() +
                        stats.getBonusSpiritMultiplier() + stats.getBonusIntellectMultiplier() + stats.getBonusManaMultiplier()
                ) != 0;

        if (!superRelevant) { // No reason to check effects if it's already known as relevant
            for (SpecialEffect effect : stats.specialEffects()) {
                if (effect.getTrigger() == Trigger.Use ||
                        effect.getTrigger() == Trigger.MeleeCrit ||
                        effect.getTrigger() == Trigger.MeleeHit ||
                        effect.getTrigger() == Trigger.PhysicalCrit ||
                        effect.getTrigger() == Trigger.PhysicalHit ||
                        effect.getTrigger() == Trigger.DoTTick ||
                        effect.getTrigger() == Trigger.DamageDone ||
                        effect.getTrigger() == Trigger.DamageOrHealingDone ||
                        effect.getTrigger() == Trigger.DamageTaken) {
                    relevant |= hasRelevantStats(effect.getStats());
                    if (relevant) break;
                }
            }
        }

        return (superRelevant || (relevant && !notRelevant));
    }

    @Override
    public String getOverallRatingLabel() {
        return "Overall Points";
    }
}
