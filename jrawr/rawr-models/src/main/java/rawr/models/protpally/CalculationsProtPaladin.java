// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtPaladin/CalculationsProtPaladin.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: UI-only members omitted (CalculationOptionsPanel and the RAWR3/Silverlight color branch);
// the non-Silverlight display label/color/optimizable/chart/glyph tables are ported additively as
// getCharacterDisplayCalculationLabels()/getSubPointNameColors()/getOptimizableCalculationLabels()/
// getCustomChartNames()/getCustomChartData()/getRelevantGlyphs(). The commented-out DPSWarr
// passive-ability auto-fixing block in GetBuffsStats stays commented (dead code in C#).
// FRAMEWORK ADAPTATION: DeserializeDataObject uses a JDK-XML reflective reader instead of
// XmlSerializer (same approach as CalculationsProtWarr/Healadin).
package rawr.models.protpally;

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
import rawr.base.items.ItemInstance;
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

public class CalculationsProtPaladin extends CalculationsBase {
    // C# CalculationsProtPaladin.cs:18-134 DefaultGemmingTemplates.
    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        // Relevant Gem IDs for ProtPaladin
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
        int[] vivid = { 39975, 40088, 40166, 40166 };       // Hit + Stamina

        // Yellow
        int[] thick = { 39916, 40015, 40126, 42157 };       // Defense
        int[] rigid = { 39915, 40014, 40125, 42156 };       // Hit

        // Orange
        int[] champion = { 39949, 40039, 40144, 40144 };    // Strength + Defense
        int[] etched = { 39948, 40038, 40143, 40143 };      // Strength + Hit
        int[] glinting = { 39953, 40044, 40148, 40148 };    // Agility + Hit
        int[] stalwart = { 39964, 40056, 40160, 40160 };    // Dodge + Defense
        int[] glimmering = { 39965, 40057, 40161, 40161 };  // Parry + Defense
        int[] accurate = { 39966, 40058, 40162, 40162 };    // Expertise + Hit
        int[] resolute = { 39967, 40059, 40163, 40163 };    // Expertise + Defense

        // Meta
        int austere = 41380;
        int eternal = 41396;

        String[] qualityGroupNames = new String[] { "Uncommon", "Rare", "Epic", "Jeweler" };
        String[] typeGroupNames = new String[] { "Survivability", "Mitigation (Agility)", "Mitigation (Dodge)", "Mitigation (Parry)", "Threat" };

        int[] metaTemplates = new int[] { austere, eternal };

        //    Red           Yellow      Blue        Prismatic
        int[][][] survivabilityTemplates = new int[][][] { // Survivability
                { solid, solid, solid, solid },
        };

        int[][][] agilityTemplates = new int[][][] { // Mitigation (Agility)
                { delicate, delicate, delicate, delicate },
                { delicate, glinting, shifting, delicate },
                { glinting, thick, enduring, delicate },
                { shifting, enduring, solid, delicate },
        };

        int[][][] dodgeTemplates = new int[][][] { // Mitigation (Dodge)
                { subtle, subtle, subtle, subtle },
                { subtle, stalwart, regal, subtle },
                { stalwart, thick, enduring, subtle },
                { regal, enduring, solid, subtle },
        };

        int[][][] parryTemplates = new int[][][] { // Mitigation (Parry)
                { flashing, flashing, flashing, flashing },
                { flashing, glimmering, defender, flashing },
                { glimmering, thick, enduring, flashing },
                { defender, enduring, solid, flashing },
        };

        int[][][] threatTemplates = new int[][][] { // Threat
                { bold, bold, bold, bold },
                { bold, champion, sovereign, bold },
                { champion, thick, enduring, bold },
                { sovereign, enduring, solid, bold },
        };

        int[][][][] gemmingTemplates = new int[][][][] { survivabilityTemplates, agilityTemplates, dodgeTemplates, parryTemplates, threatTemplates };

        // Generate List of Gemming Templates
        List<GemmingTemplate> gemmingTemplate = new ArrayList<GemmingTemplate>();
        for (int qualityId = 0; qualityId <= qualityGroupNames.length - 1; qualityId++) {
            for (int typeId = 0; typeId <= typeGroupNames.length - 1; typeId++) {
                for (int templateId = 0; templateId <= gemmingTemplates[typeId].length - 1; templateId++) {
                    for (int metaId = 0; metaId <= metaTemplates.length - 1; metaId++) {
                        GemmingTemplate template = new GemmingTemplate();
                        template.setModel("ProtPaladin");
                        template.setGroup(String.format("%s - %s", qualityGroupNames[qualityId], typeGroupNames[typeId]));
                        template.setRedId(gemmingTemplates[typeId][templateId][0][qualityId]);
                        template.setYellowId(gemmingTemplates[typeId][templateId][1][qualityId]);
                        template.setBlueId(gemmingTemplates[typeId][templateId][2][qualityId]);
                        template.setPrismaticId(gemmingTemplates[typeId][templateId][3][qualityId]);
                        template.setMetaId(metaTemplates[metaId]);
                        template.setEnabled(qualityGroupNames[qualityId].equals("Epic") && !typeGroupNames[typeId].equals("Threat"));
                        gemmingTemplate.add(template);
                    }
                }
            }
        }
        return gemmingTemplate;
    }

    // C# CalculationsProtPaladin.cs:152-233 CharacterDisplayCalculationLabels.
    private static final String[] CharacterDisplayCalculationLabels = new String[] {
            "Base Stats:Health",
            "Base Stats:Mana",
            "Base Stats:Strength",
            "Base Stats:Agility",
            "Base Stats:Stamina",
            "Base Stats:Intellect",

            "Defensive Stats:Armor",
            "Defensive Stats:Defense",
            "Defensive Stats:Miss",
            "Defensive Stats:Dodge",
            "Defensive Stats:Parry",
            "Defensive Stats:Block",
            "Defensive Stats:Chance to be Crit",
            "Defensive Stats:...Without Holy Shield",
            "Defensive Stats:Resilience",
            "Defensive Stats:Block Value",
            "Defensive Stats:Avoidance",
            "Defensive Stats:Avoidance + Block",
            "Defensive Stats:Guaranteed Reduction",
            "Defensive Stats:Total Mitigation",
            "Defensive Stats:Attacker Speed",
            "Defensive Stats:Damage Taken",

            "Offensive Stats:Weapon Speed",
            "Offensive Stats:Attack Power",
            "Offensive Stats:Spell Power",
            "Offensive Stats:Hit",
            "Offensive Stats:Spell Hit",
            "Offensive Stats:Physical Haste",
            "Offensive Stats:Effective Target Armor",
            "Offensive Stats:Effective Armor Penetration",
            "Offensive Stats:Crit",
            "Offensive Stats:Spell Crit",
            "Offensive Stats:Expertise",
            "Offensive Stats:Weapon Damage",
            "Offensive Stats:Missed Attacks",
            "Offensive Stats:Glancing Attacks",
            "Offensive Stats:Total Damage/sec",
            "Offensive Stats:Threat/sec",
            //"Offensive Stats:Unlimited Threat/sec*All white damage converted to Heroic Strikes.",

            "Resistances:Nature Resist",
            "Resistances:Fire Resist",
            "Resistances:Frost Resist",
            "Resistances:Shadow Resist",
            "Resistances:Arcane Resist",
            "Complex Stats:Ranking Mode*The currently selected ranking mode. Ranking modes can be changed in the Options tab.",
            "Complex Stats:Overall Points*Overall Points are a sum of Mitigation, Threat and Survival Points. \n"
                    + "Overall is typically, but not always, the best way to rate gear. \n"
                    + "For specific encounters, closer attention to Mitigation and \n"
                    + "Survival Points individually may be important.\n"
                    + "Remember to set your threat scale as needed.",
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
            "Complex Stats:Threat Points*Threat Points represents the average threat per second accumulated scaled by the threat scale.",
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

    // C# CalculationsProtPaladin.cs:235-266 OptimizableCalculationLabels.
    private static final String[] OptimizableCalculationLabels = new String[] {
            "Health",
            "Threat Per Second",
            "% Total Mitigation",
            "% Guaranteed Reduction",
            "Avoidance Points",
            "% Avoid + Block Attacks",
            "% Chance to be Crit",
            "% Chance to be Crit Without Holy Shield",
            "Block Value",
            "% Block Chance",
            "Burst Time",
            "TankPoints",
            "Nature Survival",
            "Fire Survival",
            "Frost Survival",
            "Shadow Survival",
            "Arcane Survival",
            // "% Hit Chance",
            "% Spell Hit Chance",
            "% Chance to be Avoided",
            "% Chance to be Missed",
            "% Chance to be Dodged",
            "% Chance to be Parried",
    };

    public String[] getOptimizableCalculationLabels() {
        return OptimizableCalculationLabels;
    }

    // C# CalculationsProtPaladin.cs:268-282 CustomChartNames.
    private static final String[] CustomChartNames = new String[] {
            "Ability Damage",
            "Ability Threat",
            "Combat Table: Defensive Stats",
            "Combat Table: Offensive Stats",
            "Combat Table: Spell Resistance",
            "Item Budget",
    };

    public String[] getCustomChartNames() {
        return CustomChartNames;
    }

    // C# CalculationsProtPaladin.cs:284-295 SubPointNameColors (non-Silverlight branch).
    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> colors = new LinkedHashMap<String, String>();
        colors.put("Survival", "#0000FF");
        colors.put("Mitigation", "#FF0000");
        colors.put("Threat", "#FFFF00");
        return colors;
    }

    // C# CalculationsProtPaladin.cs:1202-1218 RelevantItemTypes.
    private List<ItemType> relevantItemTypes = null;

    @Override
    public List<ItemType> getRelevantItemTypes() {
        if (this.relevantItemTypes == null) {
            this.relevantItemTypes = new ArrayList<ItemType>(List.of(
                    ItemType.Plate,
                    ItemType.None,
                    ItemType.Shield,
                    ItemType.Libram,
                    ItemType.OneHandAxe,
                    ItemType.OneHandMace,
                    ItemType.OneHandSword));
        }
        return this.relevantItemTypes;
    }

    // C# CalculationsProtPaladin.cs:1220-1233 GetRelevantGlyphs.
    private static List<String> relevantGlyphs;

    public List<String> getRelevantGlyphs() {
        if (relevantGlyphs == null) {
            relevantGlyphs = new ArrayList<String>();
            relevantGlyphs.add("Glyph of Judgement");
            relevantGlyphs.add("Glyph of Exorcism");
            relevantGlyphs.add("Glyph of Sense Undead");
            relevantGlyphs.add("Glyph of Consecration");
            relevantGlyphs.add("Glyph of Seal of Vengeance");
            relevantGlyphs.add("Glyph of Seal of Righteousness");
            relevantGlyphs.add("Glyph of Divine Plea");
        }
        return relevantGlyphs;
    }

    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.Paladin;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationProtPaladin();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsProtPaladin();
    }

    // C# CalculationsProtPaladin.cs:303-308 DeserializeDataObject (XmlSerializer; JDK-XML reader
    // here, same framework adaptation as CalculationsProtWarr/Healadin).
    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            CalculationOptionsProtPaladin calcOpts = new CalculationOptionsProtPaladin();
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

    public static CalculationsProtPaladin instance;

    public static CalculationsProtPaladin getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("ProtPaladin", CalculationsProtPaladin.class);
    }

    public static void register() {
        Calculations.registerModel("ProtPaladin", CalculationsProtPaladin.class);
    }

    public CalculationsProtPaladin() {
        instance = this;
    }

    // C# CalculationsProtPaladin.cs:310-510 GetCharacterCalculations.
    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        CharacterCalculationsProtPaladin calculatedStats = new CharacterCalculationsProtPaladin();

        CalculationOptionsProtPaladin calcOpts = character.getCalculationOptions() instanceof CalculationOptionsProtPaladin
                ? (CalculationOptionsProtPaladin) character.getCalculationOptions()
                : new CalculationOptionsProtPaladin();

        Stats stats = getCharacterStats(character, additionalItem, calcOpts);

        AttackModelMode amm = AttackModelMode.BasicSoV;
        if ("Seal of Righteousness".equals(calcOpts.getSealChoice()))
            amm = AttackModelMode.BasicSoR;

        DefendModel dm = new DefendModel(character, stats, calcOpts, true);
        DefendModel dmWithoutHolyShield = new DefendModel(character, stats, calcOpts, false);
        AttackModel am = new AttackModel(character, stats, amm, calcOpts);

        calculatedStats.basicStats = stats;

        // Target Info
        calculatedStats.targetLevel = calcOpts.getTargetLevel();
        calculatedStats.targetArmor = calcOpts.getTargetArmor();
        calculatedStats.effectiveTargetArmor = Lookup.getEffectiveTargetArmor(character.getLevel(), calculatedStats.targetArmor, stats.getArmorPenetration(), 0f, stats.getArmorPenetrationRating());
        calculatedStats.targetArmorDamageReduction = Lookup.targetArmorReduction(character, stats, calculatedStats.targetArmor);
        calculatedStats.effectiveTargetArmorDamageReduction = Lookup.effectiveTargetArmorReduction(character, stats, calculatedStats.targetArmor, calculatedStats.targetLevel);
        calculatedStats.armorPenetrationCap = Lookup.getArmorPenetrationCap(calculatedStats.targetLevel, calculatedStats.targetArmor, 0.0f, stats.getArmorPenetration(), stats.getArmorPenetrationRating());

        calculatedStats.activeBuffs = new ArrayList<Buff>(character.getActiveBuffs());
        calculatedStats.abilities = am.abilities;

        // Defensive stats
        calculatedStats.miss = dm.defendTable.getMiss();
        calculatedStats.dodge = dm.defendTable.getDodge();
        calculatedStats.parry = dm.defendTable.getParry();
        calculatedStats.block = dm.defendTable.getBlock();

        calculatedStats.defense = stats.getDefense() + (float) Math.floor(StatConversion.getDefenseFromRating(stats.getDefenseRating(), CharacterClass.Paladin));
        calculatedStats.defenseRating = stats.getDefenseRating();
        calculatedStats.staticBlockValue = stats.getBlockValue();
        calculatedStats.activeBlockValue = stats.getBlockValue() + stats.getHolyShieldBlockValue() + stats.getJudgementBlockValue() + stats.getShieldOfRighteousnessBlockValue();

        calculatedStats.dodgePlusMissPlusParry = calculatedStats.dodge + calculatedStats.miss + calculatedStats.parry;
        calculatedStats.dodgePlusMissPlusParryPlusBlock = calculatedStats.dodge + calculatedStats.miss + calculatedStats.parry + calculatedStats.block;
        calculatedStats.critReduction = Lookup.avoidanceChance(character, stats, rawr.base.stats.HitResult.Crit, calculatedStats.targetLevel);
        calculatedStats.critVulnerability = dm.defendTable.getCritical();
        calculatedStats.critVulnerabilityWithoutHolyShield = dmWithoutHolyShield.defendTable.getCritical();
        calculatedStats.usingHolyShield = calcOpts.getUseHolyShield();

        calculatedStats.armorReduction = Lookup.armorReduction(character, stats, calculatedStats.targetLevel);
        calculatedStats.guaranteedReduction = dm.guaranteedReduction;
        calculatedStats.totalMitigation = dm.mitigation;
        calculatedStats.baseAttackerSpeed = calcOpts.getBossAttackSpeed();
        calculatedStats.attackerSpeed = dm.parryModel.bossAttackSpeed;
        calculatedStats.damageTaken = dm.damageTaken;
        calculatedStats.dpsTaken = dm.damagePerSecond;
        calculatedStats.damageTakenPerHit = dm.damagePerHit;
        calculatedStats.damageTakenPerBlock = dm.damagePerBlock;
        calculatedStats.damageTakenPerCrit = dm.damagePerCrit;

        calculatedStats.resistanceTable = StatConversion.getResistanceTable(calculatedStats.targetLevel, character.getLevel(), stats.getFrostResistance(), 0.0f);
        calculatedStats.arcaneReduction = (1.0f - Lookup.magicReduction(character, stats, DamageType.Arcane, calculatedStats.targetLevel));
        calculatedStats.fireReduction = (1.0f - Lookup.magicReduction(character, stats, DamageType.Fire, calculatedStats.targetLevel));
        calculatedStats.frostReduction = (1.0f - Lookup.magicReduction(character, stats, DamageType.Frost, calculatedStats.targetLevel));
        calculatedStats.natureReduction = (1.0f - Lookup.magicReduction(character, stats, DamageType.Nature, calculatedStats.targetLevel));
        calculatedStats.shadowReduction = (1.0f - Lookup.magicReduction(character, stats, DamageType.Shadow, calculatedStats.targetLevel));
        calculatedStats.arcaneSurvivalPoints = stats.getHealth() / Lookup.magicReduction(character, stats, DamageType.Arcane, calculatedStats.targetLevel);
        calculatedStats.fireSurvivalPoints = stats.getHealth() / Lookup.magicReduction(character, stats, DamageType.Fire, calculatedStats.targetLevel);
        calculatedStats.frostSurvivalPoints = stats.getHealth() / Lookup.magicReduction(character, stats, DamageType.Frost, calculatedStats.targetLevel);
        calculatedStats.natureSurvivalPoints = stats.getHealth() / Lookup.magicReduction(character, stats, DamageType.Nature, calculatedStats.targetLevel);
        calculatedStats.shadowSurvivalPoints = stats.getHealth() / Lookup.magicReduction(character, stats, DamageType.Shadow, calculatedStats.targetLevel);

        // Offensive Stats
        calculatedStats.hit = Lookup.hitChance(character, stats, calculatedStats.targetLevel);
        calculatedStats.spellHit = Lookup.spellHitChance(character, stats, calculatedStats.targetLevel);
        calculatedStats.crit = Lookup.critChance(character, stats, calculatedStats.targetLevel);
        calculatedStats.spellCrit = Lookup.spellCritChance(character, stats, calculatedStats.targetLevel);
        calculatedStats.expertise = Lookup.bonusExpertisePercentage(character, stats);
        calculatedStats.physicalHaste = Lookup.bonusPhysicalHastePercentage(character, stats);
        calculatedStats.spellHaste = Lookup.bonusSpellHastePercentage(character, stats);
        calculatedStats.armorPenetration = 1.0f - (1.0f - stats.getArmorPenetration()) * (1.0f - (float) Math.min(1.0f, StatConversion.getArmorPenetrationFromRating(stats.getArmorPenetrationRating())));
        calculatedStats.armorPenetrationFromRating = (float) Math.min(1.0f, Lookup.bonusArmorPenetrationPercentage(character, stats));
        calculatedStats.effectiveArmorPenetration = (calculatedStats.targetArmor == 0) ? 0.0f : 1.0f - calculatedStats.effectiveTargetArmor / calculatedStats.targetArmor;
        if ((calculatedStats.armorPenetrationFromRating * calculatedStats.targetArmor) == 0.0f)
            calculatedStats.effectiveArmorPenetrationRating = 0.0f;
        else
            calculatedStats.effectiveArmorPenetrationRating = (float) Math.max(0.0f,
                    (calculatedStats.armorPenetrationFromRating * calculatedStats.armorPenetrationCap) / (calculatedStats.armorPenetrationFromRating * calculatedStats.targetArmor));
        float test = calculatedStats.effectiveTargetArmorDamageReduction / calculatedStats.targetArmorDamageReduction;
        calculatedStats.avoidedAttacks = am.abilities.get(Ability.None).attackTable.anyMiss();
        calculatedStats.missedAttacks = am.abilities.get(Ability.None).attackTable.getMiss();
        calculatedStats.dodgedAttacks = am.abilities.get(Ability.None).attackTable.getDodge();
        calculatedStats.parriedAttacks = am.abilities.get(Ability.None).attackTable.getParry();
        calculatedStats.glancingAttacks = am.abilities.get(Ability.None).attackTable.getGlance();
        calculatedStats.glancingReduction = Lookup.glancingReduction(character, calculatedStats.targetLevel);
        calculatedStats.blockedAttacks = am.abilities.get(Ability.None).attackTable.getBlock();
        calculatedStats.weaponSpeed = Lookup.weaponSpeed(character, stats);
        calculatedStats.totalDamagePerSecond = am.damagePerSecond;

        // Ranking Points
        //calculatedStats.UnlimitedThreat = am.ThreatPerSecond;
        //am.RageModelMode = RageModelMode.Limited;
        calculatedStats.threatPerSecond = am.threatPerSecond;
        calculatedStats.threatModel = am.name + "\n" + am.description;

        calculatedStats.tankPoints = dm.tankPoints;
        calculatedStats.burstTime = dm.burstTime;
        calculatedStats.rankingMode = calcOpts.getRankingMode();
        calculatedStats.setThreatPoints(calcOpts.getThreatScale() * calculatedStats.threatPerSecond);

        float scale = 0.0f;

        float VALUE_CAP = 1000000000f;

        switch (calcOpts.getRankingMode()) {
            // region Alternative Ranking Modes
            case 2:
                // Tank Points Mode
                calculatedStats.setSurvivalPoints(Math.min(CapSurvival(dm.effectiveHealth, calcOpts), VALUE_CAP));
                calculatedStats.setMitigationPoints(Math.min(dm.tankPoints - dm.effectiveHealth, VALUE_CAP));
                calculatedStats.setThreatPoints(Math.min(calculatedStats.getThreatPoints() * 3.0f, VALUE_CAP));
                calculatedStats.setOverallPoints(calculatedStats.getMitigationPoints() + calculatedStats.getSurvivalPoints() + calculatedStats.getThreatPoints());
                break;
            case 3:
                // Burst Time Mode
                float threatScale = (float) Math.pow((double) calcOpts.getBossAttackValue() / 25000.0d, 4);
                calculatedStats.setSurvivalPoints(Math.min(dm.burstTime * 100.0f, VALUE_CAP));
                calculatedStats.setMitigationPoints(0.0f);
                calculatedStats.setThreatPoints(Math.min((calculatedStats.getThreatPoints() / threatScale) * 2.0f, VALUE_CAP));
                calculatedStats.setOverallPoints(calculatedStats.getMitigationPoints() + calculatedStats.getSurvivalPoints() + calculatedStats.getThreatPoints());
                break;
            case 4:
                // Damage Output Mode
                calculatedStats.setSurvivalPoints(0.0f);
                calculatedStats.setMitigationPoints(0.0f);
                calculatedStats.setThreatPoints(Math.min(calculatedStats.totalDamagePerSecond, VALUE_CAP));
                calculatedStats.setOverallPoints(calculatedStats.getMitigationPoints() + calculatedStats.getSurvivalPoints() + calculatedStats.getThreatPoints());
                break;
            case 5:
                // ProtWarr Model (Average damage mitigated - EvanM Model)
                calculatedStats.setSurvivalPoints(Math.min(CapSurvival(dm.effectiveHealth, calcOpts), VALUE_CAP));
                scale = (calcOpts.getMitigationScale() / 17000.0f) * 0.125f * 100.0f;
                calculatedStats.setMitigationPoints(Math.min(dm.mitigation * calcOpts.getBossAttackValue() * scale, VALUE_CAP));
                calculatedStats.setThreatPoints(Math.min(calculatedStats.getThreatPoints(), VALUE_CAP));
                calculatedStats.setOverallPoints(calculatedStats.getMitigationPoints() + calculatedStats.getSurvivalPoints() + calculatedStats.getThreatPoints());
                break;
            case 6:
                // Damage Taken of Boss Attack Value Mode
                calculatedStats.setSurvivalPoints(Math.min(CapSurvival(dm.effectiveHealth, calcOpts), VALUE_CAP));
                scale = (float) Math.pow(10f, calcOpts.getMitigationScale() / 17000.0f);
                calculatedStats.setMitigationPoints(Math.min(dm.damageTaken * calcOpts.getBossAttackValue() * scale, VALUE_CAP));
                calculatedStats.setThreatPoints(Math.min(calculatedStats.getThreatPoints(), VALUE_CAP));
                calculatedStats.setOverallPoints(-calculatedStats.getMitigationPoints() + calculatedStats.getSurvivalPoints() + calculatedStats.getThreatPoints());
                break;
            case 7:
                // Damage Taken of Boss Attack Value Mode
                // Note: Will crash Rawr when you optimize for Mitigation Points
                calculatedStats.setSurvivalPoints(Math.min(CapSurvival(dm.effectiveHealth, calcOpts), VALUE_CAP));
                calculatedStats.setMitigationPoints(Math.min(-dm.damageTaken * calcOpts.getBossAttackValue() * (float) Math.pow(10f, 17000.0f / calcOpts.getMitigationScale()), VALUE_CAP));
                calculatedStats.setThreatPoints(Math.min(calculatedStats.getThreatPoints(), VALUE_CAP));
                calculatedStats.setOverallPoints(calculatedStats.getMitigationPoints() + calculatedStats.getSurvivalPoints() + calculatedStats.getThreatPoints());
                break;
            case 8:
                // Model 8 Placeholder
                calculatedStats.setSurvivalPoints(0.0f);
                calculatedStats.setMitigationPoints(0.0f);
                calculatedStats.setThreatPoints(0.0f);
                calculatedStats.setOverallPoints(calculatedStats.getMitigationPoints() + calculatedStats.getSurvivalPoints() + calculatedStats.getThreatPoints());
                break;
            // endregion
            default:
                // Mitigation Scale Mode (Bear Model)
                calculatedStats.setSurvivalPoints(Math.min(CapSurvival(dm.effectiveHealth, calcOpts), VALUE_CAP));
                calculatedStats.setMitigationPoints(Math.min(calcOpts.getMitigationScale() / dm.damageTaken, VALUE_CAP));
                calculatedStats.setThreatPoints(Math.min(calculatedStats.getThreatPoints(), VALUE_CAP));
                calculatedStats.setOverallPoints(calculatedStats.getMitigationPoints() + calculatedStats.getSurvivalPoints() + calculatedStats.getThreatPoints());
                break;
        }

        return calculatedStats;
    }

    // Original code from CalculationsBear, thanks Astrylian!
    private float CapSurvival(float survivalScore, CalculationOptionsProtPaladin calcOpts) {
        double survivalCap = (double) calcOpts.getSurvivalSoftCap() / 1000d;
        double survivalRaw = survivalScore / 1000f;

        //Implement Survival Soft Cap
        if (survivalRaw <= survivalCap) {
            return survivalScore;
        } else {
            double x = survivalRaw;
            double cap = survivalCap;
            double fourToTheNegativeFourThirds = Math.pow(4d, -4d / 3d);
            double topLeft = Math.pow(((x - cap) / cap) + fourToTheNegativeFourThirds, 1d / 4d);
            double topRight = Math.pow(fourToTheNegativeFourThirds, 1d / 4d);
            double fracTop = topLeft - topRight;
            double fraction = fracTop / 2d;
            double y = (cap * fraction + cap);
            return 1000f * (float) y;
        }
    }

    public Stats getCharacterStats(Character character, Item additionalItem) {
        CalculationOptionsProtPaladin calcOpts = character.getCalculationOptions() instanceof CalculationOptionsProtPaladin
                ? (CalculationOptionsProtPaladin) character.getCalculationOptions()
                : new CalculationOptionsProtPaladin();
        return getCharacterStats(character, additionalItem, calcOpts);
    }

    // C# CalculationsProtPaladin.cs:566-672 GetCharacterStats.
    public Stats getCharacterStats(Character character, Item additionalItem, CalculationOptionsProtPaladin calcOpts) {
        PaladinTalents talents = character.getPaladinTalents();

        Stats statsBase = BaseStats.getBaseStats(character.getLevel(), CharacterClass.Paladin, character.getRace());
        statsBase.setExpertise(statsBase.getExpertise() + BaseStats.getRacialExpertise(character, ItemSlot.MainHand));
        Stats statsBuffs = getBuffsStats(character, calcOpts);
        Stats statsItems = new Stats();
        accumulateItemStats(statsItems, character, additionalItem);
        Stats statsTalents = new Stats();

        statsTalents.setParry(talents.getDeflection() * 0.01f);
        statsTalents.setDodge(talents.getAnticipation() * 0.01f);
        statsTalents.setBlock(calcOpts.getUseHolyShield() ? talents.getHolyShield() * 0.30f : 0.0f);
        statsTalents.setBonusBlockValueMultiplier(talents.getRedoubt() * 0.1f);
        statsTalents.setBonusDamageMultiplier((1.0f + talents.getCrusade() * 0.01f)
                * (talents.getOneHandedWeaponSpecialization() > 0 ? 1.0f + talents.getOneHandedWeaponSpecialization() * .03f + .01f : 1.0f) - 1.0f);
        statsTalents.setBonusStaminaMultiplier((1.0f + talents.getSacredDuty() * 0.02f) * (1.0f + talents.getCombatExpertise() * 0.02f) - 1.0f);
        statsTalents.setBonusIntellectMultiplier(talents.getDivineIntellect() * 0.03f);
        statsTalents.setExpertise(talents.getCombatExpertise() * 2.0f);
        statsTalents.setBaseArmorMultiplier(talents.getToughness() * 0.02f);
        statsTalents.setPhysicalCrit((talents.getCombatExpertise() * 0.02f) + (talents.getConviction() * 0.01f) + (talents.getSanctityOfBattle() * 0.01f));
        // PhysicalCrit += ((character.ActiveBuffsConflictingBuffContains("Critical Strike Chance Taken") ? 0 : talents.HeartOfTheCrusader * 0.01f)),
        statsTalents.setSpellCrit((talents.getCombatExpertise() * 0.02f) + (talents.getConviction() * 0.01f) + (talents.getSanctityOfBattle() * 0.01f));
        // SpellCrit += ((character.ActiveBuffsConflictingBuffContains("Critical Strike Chance Taken") ? 0 : talents.HeartOfTheCrusader * 0.01f)),
        statsTalents.setBonusStrengthMultiplier(talents.getDivineStrength() * 0.03f);
        // BossAttackSpeedMultiplier = ((character.ActiveBuffsConflictingBuffContains("Boss Attack Speed") ? 0 : talents.JudgementsOfTheJust * -0.1f)),
        // BonusArmor = (character.ActiveBuffsContains("Improved Devotion Aura (Armor)") ? 0 : (talents.ImprovedDevotionAura == 3 ? 0.5f : (talents.ImprovedDevotionAura * 0.17f))) * 1250f,

        Stats statsGearEnchantsBuffs = Stats.add(statsItems, statsBuffs);
        Stats statsTotal = Stats.add(Stats.add(Stats.add(statsBase, statsItems), statsBuffs), statsTalents);

        statsTotal.setIntellect((float) Math.floor(statsBase.getIntellect() * (1.0f + statsTalents.getBonusIntellectMultiplier())));
        statsTotal.setIntellect(statsTotal.getIntellect() + (float) Math.floor((statsItems.getIntellect() + statsBuffs.getIntellect()) * (1.0f + statsTalents.getBonusIntellectMultiplier())));
        statsTotal.setBaseAgility(statsBase.getAgility() + statsTalents.getAgility());
        statsTotal.setStamina((float) Math.floor(statsBase.getStamina() * (1.0f + statsTalents.getBonusStaminaMultiplier())));
        statsTotal.setStamina(statsTotal.getStamina() + (float) Math.floor((statsItems.getStamina() + statsBuffs.getStamina()) * (1.0f + statsTalents.getBonusStaminaMultiplier())));
        statsTotal.setStamina((float) Math.floor(statsTotal.getStamina() * (1.0f + statsBuffs.getBonusStaminaMultiplier()) * (1.0f + statsItems.getBonusStaminaMultiplier())));
        statsTotal.setStrength((float) Math.floor((statsBase.getStrength() + statsTalents.getStrength()) * (1.0f + statsTotal.getBonusStrengthMultiplier())));
        statsTotal.setStrength(statsTotal.getStrength() + (float) Math.floor((statsItems.getStrength() + statsBuffs.getStrength()) * (1.0f + statsTotal.getBonusStrengthMultiplier())));
        //if (statsTotal.GreatnessProc > 0)
        //{
        //    statsTotal.Strength += (float)Math.Floor(statsTotal.GreatnessProc * 15.0f / 48.0f);
        //}
        if (talents.isGlyphOfSealOfVengeance() && "Seal of Vengeance".equals(calcOpts.getSealChoice())) {
            statsTotal.setExpertise(statsTotal.getExpertise() + 10.0f);
        }
        statsTotal.setAgility((float) Math.floor((statsBase.getAgility() + statsTalents.getAgility()) * (1.0f + statsTotal.getBonusAgilityMultiplier())));
        statsTotal.setAgility(statsTotal.getAgility() + (float) Math.floor((statsItems.getAgility() + statsBuffs.getAgility()) * (1.0f + statsTotal.getBonusAgilityMultiplier())));
        statsTotal.setHealth(statsTotal.getHealth() + StatConversion.getHealthFromStamina(statsTotal.getStamina(), CharacterClass.Paladin));

        statsTotal.setHealth(statsTotal.getHealth() * (1f + statsTotal.getBonusHealthMultiplier()));
        statsTotal.setMana(statsTotal.getMana() + StatConversion.getManaFromIntellect(statsTotal.getIntellect(), CharacterClass.Paladin) * (1f + statsTotal.getBonusManaMultiplier()));

        // Armor
        statsTotal.setArmor((float) Math.floor(statsTotal.getArmor() * (1f + statsTotal.getBaseArmorMultiplier())));
        statsTotal.setBonusArmor(statsTotal.getBonusArmor() + statsTotal.getAgility() * 2f);
        statsTotal.setBonusArmor((float) Math.floor(statsTotal.getBonusArmor() * (1f + statsTotal.getBonusArmorMultiplier())));
        statsTotal.setArmor(statsTotal.getArmor() + statsTotal.getBonusArmor());

        statsTotal.setAttackPower(statsTotal.getAttackPower() + (statsTotal.getStrength() - 20f) * 2f + 20f);
        statsTotal.setAttackPower((float) Math.floor(statsTotal.getAttackPower() * (1f + statsTotal.getBonusAttackPowerMultiplier())));
        statsTotal.setSpellPower(statsTotal.getSpellPower() + (float) Math.floor(statsTotal.getStrength() * talents.getTouchedByTheLight() * 0.2f));
        statsTotal.setNatureResistance(statsTotal.getNatureResistance() + statsTotal.getNatureResistanceBuff());
        statsTotal.setFireResistance(statsTotal.getFireResistance() + statsTotal.getFireResistanceBuff());
        statsTotal.setFrostResistance(statsTotal.getFrostResistance() + statsTotal.getFrostResistanceBuff());
        statsTotal.setShadowResistance(statsTotal.getShadowResistance() + statsTotal.getShadowResistanceBuff());
        statsTotal.setArcaneResistance(statsTotal.getArcaneResistance() + statsTotal.getArcaneResistanceBuff());
        statsTotal.setBlockValue(statsTotal.getBlockValue() + (float) Math.floor(StatConversion.getBlockValueFromStrength(statsTotal.getStrength(), CharacterClass.Paladin) - 10f));
        statsTotal.setBlockValue((float) Math.floor(statsTotal.getBlockValue() * (1f + statsTotal.getBonusBlockValueMultiplier())));
        statsTotal.setArmorPenetration(statsBase.getArmorPenetration() + statsGearEnchantsBuffs.getArmorPenetration());
        statsTotal.setBonusCritMultiplier(statsBase.getBonusCritMultiplier() + statsGearEnchantsBuffs.getBonusCritMultiplier());
        statsTotal.setCritRating(statsBase.getCritRating() + statsGearEnchantsBuffs.getCritRating());
        statsTotal.setExpertiseRating(statsBase.getExpertiseRating() + statsGearEnchantsBuffs.getExpertiseRating());
        statsTotal.setHasteRating(statsBase.getHasteRating() + statsGearEnchantsBuffs.getHasteRating());
        statsTotal.setHitRating(statsBase.getHitRating() + statsGearEnchantsBuffs.getHitRating());
        statsTotal.setWeaponDamage(statsTotal.getWeaponDamage() + Lookup.weaponDamage(character, statsTotal, false));
        //statsTotal.ExposeWeakness = statsBase.ExposeWeakness + statsGearEnchantsBuffs.ExposeWeakness; // Nerfed in 3.1

        // * judgement hit chance ? I personally believe the effect triggers even on a miss.
        // either way, TODO: 9696 Rotation trigger intervals, change these values once custom rotations are supported.

        // Calculate Procs and Special Effects
        statsTotal.accumulate(getSpecialEffectStats(character, statsTotal, calcOpts));

        ItemInstance offHand = character.get(CharacterSlot.OffHand);
        if ((calcOpts.getUseHolyShield()) && offHand != null && (offHand.getType() == ItemType.Shield)) {
            statsTotal.setHolyShieldBlockValue(statsTotal.getHolyShieldBlockValue() * (1f + statsTotal.getBonusBlockValueMultiplier()));
            statsTotal.setHolyShieldBlockValue((float) Math.floor(statsTotal.getHolyShieldBlockValue()));
            statsTotal.setJudgementBlockValue(statsTotal.getJudgementBlockValue() * (1f + statsTotal.getBonusBlockValueMultiplier()));
            statsTotal.setJudgementBlockValue((float) Math.floor(statsTotal.getJudgementBlockValue()));
            statsTotal.setShieldOfRighteousnessBlockValue(statsTotal.getShieldOfRighteousnessBlockValue() * (1f + statsTotal.getBonusBlockValueMultiplier()));
            statsTotal.setShieldOfRighteousnessBlockValue((float) Math.floor(statsTotal.getShieldOfRighteousnessBlockValue()));
        } else {
            statsTotal.setHolyShieldBlockValue(0.0f);
            statsTotal.setShieldOfRighteousnessBlockValue(0.0f);
            statsTotal.setJudgementBlockValue(0.0f);
        }

        return statsTotal;
    }

    // C# CalculationsProtPaladin.cs:674-854 GetSpecialEffectStats.
    private Stats getSpecialEffectStats(Character character, Stats stats, CalculationOptionsProtPaladin calcOpts) {
        Stats statsSpecialEffects = new Stats();

        float weaponSpeed = 1.0f;
        ItemInstance mainHand = character.get(CharacterSlot.MainHand);
        if (mainHand != null)
            weaponSpeed = mainHand.getSpeed();

        AttackModelMode amm = AttackModelMode.BasicSoV;
        if ("Seal of Righteousness".equals(calcOpts.getSealChoice()))
            amm = AttackModelMode.BasicSoR;

        AttackModel am = new AttackModel(character, stats, amm, calcOpts);
        // temporary combat table, used for the implementation of special effects.
        float hitBonusPhysical = StatConversion.getPhysicalHitFromRating(stats.getHitRating(), CharacterClass.Paladin) + stats.getPhysicalHit();
        float hitBonusSpell = StatConversion.getSpellHitFromRating(stats.getHitRating(), CharacterClass.Paladin) + stats.getSpellHit();
        float expertiseBonus = StatConversion.getDodgeParryReducFromExpertise(StatConversion.getExpertiseFromRating(stats.getExpertiseRating(), CharacterClass.Paladin) + stats.getExpertise(), CharacterClass.Paladin);
        int targetLevel = calcOpts.getTargetLevel();
        float chanceMissSpell = Math.max(0f, StatConversion.getSpellMiss(character.getLevel() - targetLevel, false) - hitBonusSpell);
        float chanceMissPhysical = Math.max(0f, StatConversion.WHITE_MISS_CHANCE_CAP[targetLevel - 80] - hitBonusPhysical);
        float chanceMissDodge = Math.max(0f, StatConversion.WHITE_DODGE_CHANCE_CAP[targetLevel - 80] - expertiseBonus);
        float chanceMissParry = Math.max(0f, StatConversion.WHITE_PARRY_CHANCE_CAP[targetLevel - 80] - expertiseBonus);
        float chanceMissPhysicalAny = chanceMissPhysical + chanceMissDodge + chanceMissParry;

        float chanceCritPhysical = StatConversion.getPhysicalCritFromRating(stats.getCritRating(), CharacterClass.Paladin)
                + StatConversion.getPhysicalCritFromAgility(stats.getAgility(), CharacterClass.Paladin)
                + stats.getPhysicalCrit()
                + StatConversion.NPC_LEVEL_CRIT_MOD[targetLevel - 80];
        float chanceCritSpell = StatConversion.getSpellCritFromRating(stats.getCritRating(), CharacterClass.Paladin)
                + StatConversion.getSpellCritFromIntellect(stats.getIntellect(), CharacterClass.Paladin)
                + stats.getSpellCrit() + stats.getSpellCritOnTarget()
                - (0.006f * (targetLevel - character.getLevel()) + (targetLevel == 83 ? 0.03f : 0.0f));
        float chanceHitPhysical = 1.0f - chanceMissPhysicalAny;
        float chanceHitSpell = 1.0f - chanceMissSpell;
        float chanceDoTTick = chanceHitSpell * (character.getPaladinTalents().isGlyphOfConsecration() ? 1.0f : 16.0f / 18.0f); // 16 ticks in 18 seconds of 9696 rotation. cba with cons. glyph atm.

        float intervalRotation = 18.0f;
        float intervalDoTTick = 1.0f;
        float intervalPhysical = Lookup.weaponSpeed(character, stats); // + calcOptsTargetsHotR / intervalHotR;
        //float intervalHitPhysical = intervalPhysical / chanceHitPhysical;
        float intervalSpellCast = 1.5f; // 9696 assumes casting a spell every gcd. Changing auras, and casting a blessing is disregarded.
        //float intervalHitSpell = intervalSpellCast / chanceHitSpell;
        float intervalDamageSpellCast = 8.0f / intervalRotation;// 9696 has 8 direct damage spell casts in 18 seconds.
        float intervalDamageDone = 1.0f / (1.0f / intervalPhysical + 1.0f / intervalSpellCast);
        float chanceDamageDone = (intervalPhysical * chanceHitPhysical + intervalSpellCast * chanceHitSpell) / (intervalPhysical + intervalSpellCast);
        float intervalJudgement = (10.0f - character.getPaladinTalents().getImprovedJudgements() * 1.0f);
        float intervalShoR = 6.0f;
        float intervalHotR = 6.0f;
        float intervalHolyShield = 9.0f;
        float intervalDivinePlea = 60.0f;

        Stats effectsToAdd = new Stats();
        for (SpecialEffect effect : stats.specialEffects()) {
            if (effect.getTrigger() == Trigger.Use) {
                if (!"Ignore".equals(calcOpts.getTrinketOnUseHandling())) {
                    if ("Active".equals(calcOpts.getTrinketOnUseHandling())) {
                        statsSpecialEffects.accumulate(effect.getStats());
                    } else {
                        effectsToAdd = new Stats();
                        effectsToAdd.accumulate(effect.getAverageStats(0f, 1f, 3f, 0f));
                        // Health on Use Effects are never averaged.
                        effectsToAdd.setBattlemasterHealth(0.0f);
                        effectsToAdd.setHealth(0.0f);
                        statsSpecialEffects.accumulate(effectsToAdd);
                    }
                }

                // Trial of the Crusader Stacking Use Effect Trinkets
                for (SpecialEffect childEffect : effect.getStats().specialEffects()) {
                    if (childEffect.getTrigger() == Trigger.DamageTaken) {
                        statsSpecialEffects.accumulate(childEffect.getStats(),
                                effect.getAverageUptime(0.0f, 1.0f)
                                        * childEffect.getAverageStackSize(1.0f / am.attackerHitsPerSecond, 1.0f, weaponSpeed, effect.getDuration()));
                    }
                }
            } else {
                switch (effect.getTrigger()) {
                    case MeleeHit:
                    case PhysicalHit:
                        if (effect.getStats().getDeathbringerProc() > 0) {
                            effectsToAdd = effect.getAverageStats(intervalPhysical, 1.0f, weaponSpeed, 0f);
                            effectsToAdd.setStrength(effectsToAdd.getDeathbringerProc());
                            effectsToAdd.setHasteRating(effectsToAdd.getDeathbringerProc());
                            effectsToAdd.setCritRating(effectsToAdd.getDeathbringerProc());
                            effectsToAdd.setDeathbringerProc(0f);
                            statsSpecialEffects.accumulate(effectsToAdd, 1f / 3f);
                        } else {
                            effectsToAdd = new Stats();
                            effectsToAdd.accumulate(effect.getAverageStats(intervalPhysical, chanceHitPhysical, weaponSpeed, 0f));
                            effectsToAdd.setArmor((float) Math.floor(2.0f * effectsToAdd.getAgility())); // mongoose agi
                            statsSpecialEffects.accumulate(effectsToAdd);
                        }
                        break;
                    case MeleeCrit:
                    case PhysicalCrit:
                        statsSpecialEffects.accumulate(effect.getAverageStats(intervalPhysical, chanceCritPhysical, weaponSpeed, 0f));
                        break;
                    case DoTTick:
                        statsSpecialEffects.accumulate(effect.getAverageStats(intervalDoTTick, chanceDoTTick, 3f, 0f));
                        break;
                    case DamageDone:
                        statsSpecialEffects.accumulate(effect.getAverageStats(intervalDamageDone, chanceDamageDone, 3f, 0f));
                        break;
                    case DamageOrHealingDone:
                        // Need to add Self-Heals
                        statsSpecialEffects.accumulate(effect.getAverageStats(intervalDamageDone, chanceDamageDone, 3f, 0f));
                        break;
                    case DamageTaken:
                    case DamageTakenPhysical:
                        statsSpecialEffects.accumulate(effect.getAverageStats((1.0f / am.attackerHitsPerSecond), 1.0f, weaponSpeed, 0f));
                        break;
                    case JudgementHit:
                        statsSpecialEffects.accumulate(effect.getAverageStats(intervalJudgement, 1f, 3f, 0f));
                        break;
                    case ShieldofRighteousness:
                        statsSpecialEffects.accumulate(effect.getAverageStats(intervalShoR, 1f, 3f, 0f));
                        break;
                    case HammeroftheRighteous:
                        statsSpecialEffects.accumulate(effect.getAverageStats(intervalHotR, 1f, 3f, 0f));
                        break;
                    case HolyShield:
                        statsSpecialEffects.accumulate(effect.getAverageStats(intervalHolyShield, 1f, 3f, 0f));
                        break;
                    case DivinePlea:
                        statsSpecialEffects.accumulate(effect.getAverageStats(intervalDivinePlea, 1f, 3f, 0f));
                        break;
                    case SpellCast:
                        statsSpecialEffects.accumulate(effect.getAverageStats(intervalSpellCast, 1f, 3f, 0f));
                        break;
                    case DamageSpellCast:
                        statsSpecialEffects.accumulate(effect.getAverageStats(intervalDamageSpellCast, 1f, 3f, 0f));
                        break;
                    case DamageSpellHit:
                        statsSpecialEffects.accumulate(effect.getAverageStats(intervalDamageSpellCast, chanceHitSpell, 3f, 0f));
                        break;
                    case DamageSpellCrit:
                        statsSpecialEffects.accumulate(effect.getAverageStats(intervalDamageSpellCast, chanceCritSpell, 3f, 0f));
                        break;
                    default:
                        break;
                }
            }
        }

        // Darkmoon card greatness & Paragon procs
        // These should always increase strength, which is going to be the Paladin's top stat outside of stamina
        statsSpecialEffects.setStrength(statsSpecialEffects.getStrength() + statsSpecialEffects.getHighestStat() + statsSpecialEffects.getParagon());
        statsSpecialEffects.setHighestStat(0);
        statsSpecialEffects.setParagon(0);

        // Base Stats
        statsSpecialEffects.setStamina((float) Math.floor(statsSpecialEffects.getStamina() * (1.0f + stats.getBonusStaminaMultiplier())));
        statsSpecialEffects.setStrength((float) Math.floor(statsSpecialEffects.getStrength() * (1.0f + stats.getBonusStrengthMultiplier())));
        statsSpecialEffects.setAgility((float) Math.floor(statsSpecialEffects.getAgility() * (1.0f + stats.getBonusAgilityMultiplier())));
        statsSpecialEffects.setHealth(statsSpecialEffects.getHealth() + (float) Math.floor(statsSpecialEffects.getStamina() * 10.0f) + (float) Math.floor(statsSpecialEffects.getBattlemasterHealth()));

        // Defensive Stats
        statsSpecialEffects.setArmor((float) Math.floor(statsSpecialEffects.getArmor() * (1f + stats.getBaseArmorMultiplier() + statsSpecialEffects.getBaseArmorMultiplier())));
        statsSpecialEffects.setBonusArmor(statsSpecialEffects.getBonusArmor() + statsSpecialEffects.getAgility() * 2.0f);
        statsSpecialEffects.setBonusArmor((float) Math.floor(statsSpecialEffects.getBonusArmor() * (1.0f + stats.getBonusArmorMultiplier() + statsSpecialEffects.getBonusArmorMultiplier())));
        statsSpecialEffects.setArmor(statsSpecialEffects.getArmor() + statsSpecialEffects.getBonusArmor());

        statsSpecialEffects.setBlockValue(statsSpecialEffects.getBlockValue() + (float) Math.floor(StatConversion.getBlockValueFromStrength(statsSpecialEffects.getStrength(), CharacterClass.Paladin)));
        statsSpecialEffects.setBlockValue((float) Math.floor(statsSpecialEffects.getBlockValue() * (1.0f + stats.getBonusBlockValueMultiplier() + statsSpecialEffects.getBonusBlockValueMultiplier())));

        // Offensive Stats
        statsSpecialEffects.setAttackPower(statsSpecialEffects.getAttackPower() + statsSpecialEffects.getStrength() * 2.0f);
        statsSpecialEffects.setAttackPower((float) Math.floor(statsSpecialEffects.getAttackPower() * (1.0f + stats.getBonusAttackPowerMultiplier() + statsSpecialEffects.getBonusAttackPowerMultiplier())));

        return statsSpecialEffects;
    }

    // C# CalculationsProtPaladin.cs:856-1198 GetCustomChartData.
    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        CharacterCalculationsProtPaladin calculations = (CharacterCalculationsProtPaladin) getCharacterCalculations(character);

        switch (chartName) {
            // region Ability Damage/Threat
            case "Ability Damage":
            case "Ability Threat": {
                ComparisonCalculationBase[] comparisons = new ComparisonCalculationBase[calculations.abilities.size()];
                int j = 0;
                for (AbilityModel ability : calculations.abilities.values()) {
                    ComparisonCalculationProtPaladin comparison = new ComparisonCalculationProtPaladin();

                    comparison.setName(ability.getName());
                    if ("Ability Damage".equals(chartName))
                        comparison.setMitigationPoints(ability.getDamage());
                    if ("Ability Threat".equals(chartName))
                        comparison.setThreatPoints(ability.getThreat());

                    comparison.setOverallPoints(comparison.getSurvivalPoints() + comparison.getThreatPoints() + comparison.getMitigationPoints());
                    comparisons[j] = comparison;
                    j++;
                }
                return comparisons;
            }
            // endregion
            // region Combat Table: Defensive Stats
            case "Combat Table: Defensive Stats": {
                ComparisonCalculationProtPaladin calcMiss = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcDodge = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcParry = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcBlock = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcCrit = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcCrush = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcHit = new ComparisonCalculationProtPaladin();
                if (calculations != null) {
                    calcMiss.setName("1 Miss");
                    calcDodge.setName("2 Dodge");
                    calcParry.setName("3 Parry");
                    calcBlock.setName("4 Block");
                    calcCrit.setName("5 Crit");
                    calcCrush.setName("6 Crush");
                    calcHit.setName("7 Hit");

                    calcMiss.setOverallPoints(calculations.miss * 100.0f);
                    calcMiss.setMitigationPoints(calculations.miss * 100.0f);
                    calcDodge.setOverallPoints(calculations.dodge * 100.0f);
                    calcDodge.setMitigationPoints(calculations.dodge * 100.0f);
                    calcParry.setOverallPoints(calculations.parry * 100.0f);
                    calcParry.setMitigationPoints(calculations.parry * 100.0f);
                    calcBlock.setOverallPoints(calculations.block * 100.0f);
                    calcBlock.setMitigationPoints(calculations.block * 100.0f);
                    calcCrit.setOverallPoints(calculations.critVulnerability * 100.0f);
                    calcCrit.setSurvivalPoints(calculations.critVulnerability * 100.0f);
                    calcHit.setOverallPoints((1.0f - (calculations.dodgePlusMissPlusParryPlusBlock + calculations.critVulnerability)) * 100.0f);
                    calcHit.setSurvivalPoints((1.0f - (calculations.dodgePlusMissPlusParryPlusBlock + calculations.critVulnerability)) * 100.0f);
                }
                return new ComparisonCalculationBase[] { calcMiss, calcDodge, calcParry, calcBlock, calcCrit, calcCrush, calcHit };
            }
            // endregion
            // region Combat Table: Offensive Stats
            case "Combat Table: Offensive Stats": {
                ComparisonCalculationProtPaladin calcMiss = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcDodge = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcParry = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcGlance = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcBlock = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcCrit = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcHit = new ComparisonCalculationProtPaladin();
                if (calculations != null) {
                    calcMiss.setName("1 Miss");
                    calcDodge.setName("2 Dodge");
                    calcParry.setName("3 Parry");
                    calcGlance.setName("4 Glancing");
                    calcBlock.setName("5 Block");
                    calcCrit.setName("6 Crit");
                    calcHit.setName("7 Hit");

                    calcMiss.setOverallPoints(calculations.missedAttacks * 100.0f);
                    calcMiss.setMitigationPoints(calculations.missedAttacks * 100.0f);
                    calcDodge.setOverallPoints(calculations.dodgedAttacks * 100.0f);
                    calcDodge.setMitigationPoints(calculations.dodgedAttacks * 100.0f);
                    calcParry.setOverallPoints(calculations.parriedAttacks * 100.0f);
                    calcParry.setMitigationPoints(calculations.parriedAttacks * 100.0f);
                    calcGlance.setOverallPoints(calculations.glancingAttacks * 100.0f);
                    calcGlance.setMitigationPoints(calculations.glancingAttacks * 100.0f);
                    calcBlock.setOverallPoints(calculations.blockedAttacks * 100.0f);
                    calcBlock.setMitigationPoints(calculations.blockedAttacks * 100.0f);
                    calcCrit.setOverallPoints(calculations.crit * 100.0f);
                    calcCrit.setSurvivalPoints(calculations.crit * 100.0f);
                    calcHit.setOverallPoints((1.0f - (calculations.avoidedAttacks + calculations.glancingAttacks + calculations.blockedAttacks + calculations.crit)) * 100.0f);
                    calcHit.setSurvivalPoints((1.0f - (calculations.avoidedAttacks + calculations.glancingAttacks + calculations.blockedAttacks + calculations.crit)) * 100.0f);
                }
                return new ComparisonCalculationBase[] { calcMiss, calcDodge, calcParry, calcGlance, calcBlock, calcCrit, calcHit };
            }
            // endregion
            // region Item Budget
            case "Item Budget": {
                CharacterCalculationsProtPaladin calcBaseValue = (CharacterCalculationsProtPaladin) getCharacterCalculations(character);
                CharacterCalculationsProtPaladin calcDodgeValue = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("DodgeRating", 10f));
                CharacterCalculationsProtPaladin calcParryValue = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("ParryRating", 10f));
                CharacterCalculationsProtPaladin calcBlockValue = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("BlockRating", 10f));
                CharacterCalculationsProtPaladin calcHasteValue = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("HasteRating", 10f));
                CharacterCalculationsProtPaladin calcExpertiseValue = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("ExpertiseRating", 10f));
                CharacterCalculationsProtPaladin calcHitValue = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("HitRating", 10f));
                CharacterCalculationsProtPaladin calcBlockValueValue = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("BlockValue", 10f / 0.65f));
                CharacterCalculationsProtPaladin calcHealthValue = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("Health", (10f * 10f) / 0.667f));
                CharacterCalculationsProtPaladin calcResilValue = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("Resilience", 10f));

                //Differential Calculations for Agi
                CharacterCalculationsProtPaladin calcAtAdd = calcBaseValue;
                float agiToAdd = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtAdd.getOverallPoints() && agiToAdd < 2) {
                    agiToAdd += 0.01f;
                    calcAtAdd = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("Agility", agiToAdd));
                }

                CharacterCalculationsProtPaladin calcAtSubtract = calcBaseValue;
                float agiToSubtract = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtSubtract.getOverallPoints() && agiToSubtract > -2) {
                    agiToSubtract -= 0.01f;
                    calcAtSubtract = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("Agility", agiToSubtract));
                }
                agiToSubtract += 0.01f;

                ComparisonCalculationProtPaladin comparisonAgi = new ComparisonCalculationProtPaladin();
                comparisonAgi.setName("10 Agility");
                comparisonAgi.setOverallPoints(10 * (calcAtAdd.getOverallPoints() - calcBaseValue.getOverallPoints()) / (agiToAdd - agiToSubtract));
                comparisonAgi.setMitigationPoints(10 * (calcAtAdd.getMitigationPoints() - calcBaseValue.getMitigationPoints()) / (agiToAdd - agiToSubtract));
                comparisonAgi.setSurvivalPoints(10 * (calcAtAdd.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()) / (agiToAdd - agiToSubtract));
                comparisonAgi.setThreatPoints(10 * (calcAtAdd.getThreatPoints() - calcBaseValue.getThreatPoints()) / (agiToAdd - agiToSubtract));

                //Differential Calculations for Str
                calcAtAdd = calcBaseValue;
                float strToAdd = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtAdd.getOverallPoints() && strToAdd <= 22) {
                    strToAdd += 0.01f;
                    calcAtAdd = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("Strength", strToAdd));
                }

                calcAtSubtract = calcBaseValue;
                float strToSubtract = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtSubtract.getOverallPoints() && strToSubtract >= -22) {
                    strToSubtract -= 0.01f;
                    calcAtSubtract = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("Strength", strToSubtract));
                }
                strToSubtract += 0.01f;

                ComparisonCalculationProtPaladin comparisonStr = new ComparisonCalculationProtPaladin();
                comparisonStr.setName("10 Strength");
                comparisonStr.setOverallPoints(10 * (calcAtAdd.getOverallPoints() - calcBaseValue.getOverallPoints()) / (strToAdd - strToSubtract));
                comparisonStr.setMitigationPoints(10 * (calcAtAdd.getMitigationPoints() - calcBaseValue.getMitigationPoints()) / (strToAdd - strToSubtract));
                comparisonStr.setSurvivalPoints(10 * (calcAtAdd.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()) / (strToAdd - strToSubtract));
                comparisonStr.setThreatPoints(10 * (calcAtAdd.getThreatPoints() - calcBaseValue.getThreatPoints()) / (strToAdd - strToSubtract));

                //Differential Calculations for Def
                calcAtAdd = calcBaseValue;
                float defToAdd = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtAdd.getOverallPoints() && defToAdd < 20) {
                    defToAdd += 0.01f;
                    calcAtAdd = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("DefenseRating", defToAdd));
                }

                calcAtSubtract = calcBaseValue;
                float defToSubtract = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtSubtract.getOverallPoints() && defToSubtract > -20) {
                    defToSubtract -= 0.01f;
                    calcAtSubtract = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("DefenseRating", defToSubtract));
                }
                defToSubtract += 0.01f;

                ComparisonCalculationProtPaladin comparisonDef = new ComparisonCalculationProtPaladin();
                comparisonDef.setName("10 Defense Rating");
                comparisonDef.setOverallPoints(10 * (calcAtAdd.getOverallPoints() - calcBaseValue.getOverallPoints()) / (defToAdd - defToSubtract));
                comparisonDef.setMitigationPoints(10 * (calcAtAdd.getMitigationPoints() - calcBaseValue.getMitigationPoints()) / (defToAdd - defToSubtract));
                comparisonDef.setSurvivalPoints(10 * (calcAtAdd.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()) / (defToAdd - defToSubtract));
                comparisonDef.setThreatPoints(10 * (calcAtAdd.getThreatPoints() - calcBaseValue.getThreatPoints()) / (defToAdd - defToSubtract));

                //Differential Calculations for AC
                calcAtAdd = calcBaseValue;
                float acToAdd = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtAdd.getOverallPoints() && acToAdd < 2) {
                    acToAdd += 0.01f;
                    calcAtAdd = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("Armor", acToAdd));
                }

                calcAtSubtract = calcBaseValue;
                float acToSubtract = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtSubtract.getOverallPoints() && acToSubtract > -2) {
                    acToSubtract -= 0.01f;
                    calcAtSubtract = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("Armor", acToSubtract));
                }
                acToSubtract += 0.01f;

                ComparisonCalculationProtPaladin comparisonAC = new ComparisonCalculationProtPaladin();
                comparisonAC.setName("100 Armor");
                comparisonAC.setOverallPoints(100f * (calcAtAdd.getOverallPoints() - calcBaseValue.getOverallPoints()) / (acToAdd - acToSubtract));
                comparisonAC.setMitigationPoints(100f * (calcAtAdd.getMitigationPoints() - calcBaseValue.getMitigationPoints()) / (acToAdd - acToSubtract));
                comparisonAC.setSurvivalPoints(100f * (calcAtAdd.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()) / (acToAdd - acToSubtract));
                comparisonAC.setThreatPoints(100f * (calcAtAdd.getThreatPoints() - calcBaseValue.getThreatPoints()) / (acToAdd - acToSubtract));

                //Differential Calculations for Sta
                calcAtAdd = calcBaseValue;
                float staToAdd = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtAdd.getOverallPoints() && staToAdd < 2) {
                    staToAdd += 0.01f;
                    calcAtAdd = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("Stamina", staToAdd));
                }

                calcAtSubtract = calcBaseValue;
                float staToSubtract = 0f;
                while (calcBaseValue.getOverallPoints() == calcAtSubtract.getOverallPoints() && staToSubtract > -2) {
                    staToSubtract -= 0.01f;
                    calcAtSubtract = (CharacterCalculationsProtPaladin) getCharacterCalculations(character, itemWithStat("Stamina", staToSubtract));
                }
                staToSubtract += 0.01f;

                ComparisonCalculationProtPaladin comparisonSta = new ComparisonCalculationProtPaladin();
                comparisonSta.setName("15 Stamina");
                comparisonSta.setOverallPoints((10f / 0.667f) * (calcAtAdd.getOverallPoints() - calcBaseValue.getOverallPoints()) / (staToAdd - staToSubtract));
                comparisonSta.setMitigationPoints((10f / 0.667f) * (calcAtAdd.getMitigationPoints() - calcBaseValue.getMitigationPoints()) / (staToAdd - staToSubtract));
                comparisonSta.setSurvivalPoints((10f / 0.667f) * (calcAtAdd.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()) / (staToAdd - staToSubtract));
                comparisonSta.setThreatPoints((10f / 0.667f) * (calcAtAdd.getThreatPoints() - calcBaseValue.getThreatPoints()) / (staToAdd - staToSubtract));

                ComparisonCalculationProtPaladin comparisonDodge = new ComparisonCalculationProtPaladin();
                comparisonDodge.setName("10 Dodge Rating");
                comparisonDodge.setOverallPoints((calcDodgeValue.getOverallPoints() - calcBaseValue.getOverallPoints()));
                comparisonDodge.setMitigationPoints((calcDodgeValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonDodge.setSurvivalPoints((calcDodgeValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonDodge.setThreatPoints((calcDodgeValue.getThreatPoints() - calcBaseValue.getThreatPoints()));

                ComparisonCalculationProtPaladin comparisonParry = new ComparisonCalculationProtPaladin();
                comparisonParry.setName("10 Parry Rating");
                comparisonParry.setOverallPoints((calcParryValue.getOverallPoints() - calcBaseValue.getOverallPoints()));
                comparisonParry.setMitigationPoints((calcParryValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonParry.setSurvivalPoints((calcParryValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonParry.setThreatPoints((calcParryValue.getThreatPoints() - calcBaseValue.getThreatPoints()));

                ComparisonCalculationProtPaladin comparisonBlock = new ComparisonCalculationProtPaladin();
                comparisonBlock.setName("10 Block Rating");
                comparisonBlock.setOverallPoints((calcBlockValue.getOverallPoints() - calcBaseValue.getOverallPoints()));
                comparisonBlock.setMitigationPoints((calcBlockValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonBlock.setSurvivalPoints((calcBlockValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonBlock.setThreatPoints((calcBlockValue.getThreatPoints() - calcBaseValue.getThreatPoints()));

                ComparisonCalculationProtPaladin comparisonHaste = new ComparisonCalculationProtPaladin();
                comparisonHaste.setName("10 Haste Rating");
                comparisonHaste.setOverallPoints((calcHasteValue.getOverallPoints() - calcBaseValue.getOverallPoints()));
                comparisonHaste.setMitigationPoints((calcHasteValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonHaste.setSurvivalPoints((calcHasteValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonHaste.setThreatPoints((calcHasteValue.getThreatPoints() - calcBaseValue.getThreatPoints()));

                ComparisonCalculationProtPaladin comparisonExpertise = new ComparisonCalculationProtPaladin();
                comparisonExpertise.setName("10 Expertise Rating");
                comparisonExpertise.setOverallPoints((calcExpertiseValue.getOverallPoints() - calcBaseValue.getOverallPoints()));
                comparisonExpertise.setMitigationPoints((calcExpertiseValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonExpertise.setSurvivalPoints((calcExpertiseValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonExpertise.setThreatPoints((calcExpertiseValue.getThreatPoints() - calcBaseValue.getThreatPoints()));

                ComparisonCalculationProtPaladin comparisonHit = new ComparisonCalculationProtPaladin();
                comparisonHit.setName("10 Hit Rating");
                comparisonHit.setOverallPoints((calcHitValue.getOverallPoints() - calcBaseValue.getOverallPoints()));
                comparisonHit.setMitigationPoints((calcHitValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonHit.setSurvivalPoints((calcHitValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonHit.setThreatPoints((calcHitValue.getThreatPoints() - calcBaseValue.getThreatPoints()));

                ComparisonCalculationProtPaladin comparisonBlockValue = new ComparisonCalculationProtPaladin();
                comparisonBlockValue.setName("15.38 Block Value");
                comparisonBlockValue.setOverallPoints((calcBlockValueValue.getOverallPoints() - calcBaseValue.getOverallPoints()));
                comparisonBlockValue.setMitigationPoints((calcBlockValueValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonBlockValue.setSurvivalPoints((calcBlockValueValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonBlockValue.setThreatPoints((calcBlockValueValue.getThreatPoints() - calcBaseValue.getThreatPoints()));

                ComparisonCalculationProtPaladin comparisonHealth = new ComparisonCalculationProtPaladin();
                comparisonHealth.setName("150 Health");
                comparisonHealth.setOverallPoints((calcHealthValue.getOverallPoints() - calcBaseValue.getOverallPoints()));
                comparisonHealth.setMitigationPoints((calcHealthValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonHealth.setSurvivalPoints((calcHealthValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonHealth.setThreatPoints((calcHealthValue.getThreatPoints() - calcBaseValue.getThreatPoints()));

                ComparisonCalculationProtPaladin comparisonResil = new ComparisonCalculationProtPaladin();
                comparisonResil.setName("10 Resilience");
                comparisonResil.setOverallPoints((calcResilValue.getOverallPoints() - calcBaseValue.getOverallPoints()));
                comparisonResil.setMitigationPoints((calcResilValue.getMitigationPoints() - calcBaseValue.getMitigationPoints()));
                comparisonResil.setSurvivalPoints((calcResilValue.getSurvivalPoints() - calcBaseValue.getSurvivalPoints()));
                comparisonResil.setThreatPoints((calcResilValue.getThreatPoints() - calcBaseValue.getThreatPoints()));

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
            }
            // endregion
            // region Spell Resistance
            case "Combat Table: Spell Resistance": {
                ComparisonCalculationProtPaladin calcSpellRes0 = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcSpellRes10 = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcSpellRes20 = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcSpellRes30 = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcSpellRes40 = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcSpellRes50 = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcSpellRes60 = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcSpellRes70 = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcSpellRes80 = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcSpellRes90 = new ComparisonCalculationProtPaladin();
                ComparisonCalculationProtPaladin calcSpellRes100 = new ComparisonCalculationProtPaladin();

                if (calculations != null) {
                    calcSpellRes0.setName("0.00");
                    calcSpellRes10.setName("0.10");
                    calcSpellRes20.setName("0.20");
                    calcSpellRes30.setName("0.30");
                    calcSpellRes40.setName("0.40");
                    calcSpellRes50.setName("0.50");
                    calcSpellRes60.setName("0.60");
                    calcSpellRes70.setName("0.70");
                    calcSpellRes80.setName("0.80");
                    calcSpellRes90.setName("0.90");
                    calcSpellRes100.setName("1.00");

                    calcSpellRes0.setOverallPoints(calculations.resistanceTable[0] * 100.0f);
                    calcSpellRes0.setMitigationPoints(calculations.resistanceTable[0] * 100.0f);
                    calcSpellRes10.setOverallPoints(calculations.resistanceTable[1] * 100.0f);
                    calcSpellRes10.setMitigationPoints(calculations.resistanceTable[1] * 100.0f);
                    calcSpellRes20.setOverallPoints(calculations.resistanceTable[2] * 100.0f);
                    calcSpellRes20.setMitigationPoints(calculations.resistanceTable[2] * 100.0f);
                    calcSpellRes30.setOverallPoints(calculations.resistanceTable[3] * 100.0f);
                    calcSpellRes30.setMitigationPoints(calculations.resistanceTable[3] * 100.0f);
                    calcSpellRes40.setOverallPoints(calculations.resistanceTable[4] * 100.0f);
                    calcSpellRes40.setMitigationPoints(calculations.resistanceTable[4] * 100.0f);
                    calcSpellRes50.setOverallPoints(calculations.resistanceTable[5] * 100.0f);
                    calcSpellRes50.setMitigationPoints(calculations.resistanceTable[5] * 100.0f);
                    calcSpellRes60.setOverallPoints(calculations.resistanceTable[6] * 100.0f);
                    calcSpellRes60.setMitigationPoints(calculations.resistanceTable[6] * 100.0f);
                    calcSpellRes70.setOverallPoints(calculations.resistanceTable[7] * 100.0f);
                    calcSpellRes70.setMitigationPoints(calculations.resistanceTable[7] * 100.0f);
                    calcSpellRes80.setOverallPoints(calculations.resistanceTable[8] * 100.0f);
                    calcSpellRes80.setMitigationPoints(calculations.resistanceTable[8] * 100.0f);
                    calcSpellRes90.setOverallPoints(calculations.resistanceTable[9] * 100.0f);
                    calcSpellRes90.setMitigationPoints(calculations.resistanceTable[9] * 100.0f);
                    calcSpellRes100.setOverallPoints(calculations.resistanceTable[10] * 100.0f);
                    calcSpellRes100.setMitigationPoints(calculations.resistanceTable[10] * 100.0f);
                }
                return new ComparisonCalculationBase[] { calcSpellRes0, calcSpellRes10, calcSpellRes20, calcSpellRes30, calcSpellRes40, calcSpellRes50, calcSpellRes60, calcSpellRes70, calcSpellRes80, calcSpellRes90, calcSpellRes100 };
            }
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

    // C# CalculationsProtPaladin.cs:1235-1242.
    @Override
    public boolean enchantFitsInSlot(Enchant enchant, Character character, ItemSlot slot) {
        // Filters out Non-Shield Offhand Enchants and Ranged Enchants
        if ((slot == ItemSlot.OffHand && enchant.slot != ItemSlot.OffHand) || slot == ItemSlot.Ranged) return false;
        // Filters out Death Knight and Two-Hander Enchants
        if (enchant.name.startsWith("Rune of the") || enchant.slot == ItemSlot.TwoHand) return false;

        return super.enchantFitsInSlot(enchant, character, slot);
    }

    // C# CalculationsProtPaladin.cs:1244-1247.
    @Override
    public boolean itemFitsInSlot(Item item, Character character, CharacterSlot slot, boolean ignoreUnique) {
        if (slot == CharacterSlot.OffHand && item.getType() != ItemType.Shield) return false;
        return super.itemFitsInSlot(item, character, slot, ignoreUnique);
    }

    private boolean isTriggerRelevant(Trigger trigger) {
        return (
                trigger == Trigger.Use ||
                        trigger == Trigger.MeleeCrit ||
                        trigger == Trigger.MeleeHit ||
                        trigger == Trigger.PhysicalCrit ||
                        trigger == Trigger.PhysicalHit ||
                        trigger == Trigger.DoTTick ||
                        trigger == Trigger.DamageDone ||
                        trigger == Trigger.DamageOrHealingDone ||
                        trigger == Trigger.JudgementHit ||
                        trigger == Trigger.HolyShield ||
                        trigger == Trigger.ShieldofRighteousness ||
                        trigger == Trigger.HammeroftheRighteous ||
                        trigger == Trigger.SpellCast ||
                        trigger == Trigger.SpellHit ||
                        trigger == Trigger.DamageSpellHit ||
                        trigger == Trigger.DamageTaken ||
                        trigger == Trigger.DivinePlea ||
                        trigger == Trigger.DamageTakenPhysical
        );
    }

    // C# CalculationsProtPaladin.cs:1264-1338 GetRelevantStats.
    public Stats getRelevantStats(Stats stats) {
        Stats s = new Stats();
        s.setArmor(stats.getArmor());
        s.setBonusArmor(stats.getBonusArmor());
        s.setStamina(stats.getStamina());
        s.setAgility(stats.getAgility());
        s.setDodgeRating(stats.getDodgeRating());
        s.setParryRating(stats.getParryRating());
        s.setBlockRating(stats.getBlockRating());
        s.setBlockValue(stats.getBlockValue());
        s.setDefenseRating(stats.getDefenseRating());
        s.setResilience(stats.getResilience());
        s.setBonusAgilityMultiplier(stats.getBonusAgilityMultiplier());
        s.setBonusStrengthMultiplier(stats.getBonusStrengthMultiplier());
        s.setBonusAttackPowerMultiplier(stats.getBonusAttackPowerMultiplier());
        s.setBaseArmorMultiplier(stats.getBaseArmorMultiplier());
        s.setBonusArmorMultiplier(stats.getBonusArmorMultiplier());
        s.setBonusStaminaMultiplier(stats.getBonusStaminaMultiplier());
        s.setHealth(stats.getHealth());
        s.setBattlemasterHealth(stats.getBattlemasterHealth());
        s.setBonusHealthMultiplier(stats.getBonusHealthMultiplier());
        s.setDamageTakenMultiplier(stats.getDamageTakenMultiplier());
        s.setMiss(stats.getMiss());
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
        s.setDeathbringerProc(stats.getDeathbringerProc());
        s.setHighestStat(stats.getHighestStat());
        s.setParagon(stats.getParagon());

        s.setStrength(stats.getStrength());
        s.setAttackPower(stats.getAttackPower());
        s.setSpellPower(stats.getSpellPower());
        s.setCritRating(stats.getCritRating());
        s.setPhysicalCrit(stats.getPhysicalCrit());
        s.setSpellCrit(stats.getSpellCrit());
        s.setSpellCritOnTarget(stats.getSpellCritOnTarget());
        s.setHitRating(stats.getHitRating());
        s.setPhysicalHit(stats.getPhysicalHit());
        s.setSpellHit(stats.getSpellHit());
        s.setHasteRating(stats.getHasteRating());
        s.setPhysicalHaste(stats.getPhysicalHaste());
        s.setExpertiseRating(stats.getExpertiseRating());
        s.setArmorPenetration(stats.getArmorPenetration());
        s.setArmorPenetrationRating(stats.getArmorPenetrationRating());
        s.setWeaponDamage(stats.getWeaponDamage());
        s.setBonusCritMultiplier(stats.getBonusCritMultiplier());
        s.setThreatIncreaseMultiplier(stats.getThreatIncreaseMultiplier());
        s.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());
        s.setBonusBlockValueMultiplier(stats.getBonusBlockValueMultiplier());

        s.setBonusShieldOfRighteousnessDamage(stats.getBonusShieldOfRighteousnessDamage());
        s.setHolyShieldBlockValue(stats.getHolyShieldBlockValue());
        s.setJudgementBlockValue(stats.getJudgementBlockValue());
        s.setBonusHammerOfTheRighteousMultiplier(stats.getBonusHammerOfTheRighteousMultiplier());
        s.setShieldOfRighteousnessBlockValue(stats.getShieldOfRighteousnessBlockValue());
        s.setBonusSealOfCorruptionDamageMultiplier(stats.getBonusSealOfCorruptionDamageMultiplier());
        s.setBonusSealOfRighteousnessDamageMultiplier(stats.getBonusSealOfRighteousnessDamageMultiplier());
        s.setBonusSealOfVengeanceDamageMultiplier(stats.getBonusSealOfVengeanceDamageMultiplier());
        s.setConsecrationSpellPower(stats.getConsecrationSpellPower());

        for (SpecialEffect effect : stats.specialEffects()) {
            if (isTriggerRelevant(effect.getTrigger()) && hasRelevantStats(effect.getStats())) {
                s.addSpecialEffect(effect);
            }
        }
        return s;
    }

    // C# CalculationsProtPaladin.cs:1340-1342.
    @Override
    public boolean isItemRelevant(Item item) {
        return super.isItemRelevant(item);
    }

    // C# CalculationsProtPaladin.cs:1344-1441 HasRelevantStats.
    @Override
    public boolean hasRelevantStats(Stats stats) {
        boolean relevant = (
                // Basic Stats
                stats.getStamina() +
                        stats.getHealth() +
                        stats.getBattlemasterHealth() +
                        stats.getStrength() +
                        stats.getAgility() +

                        // Tanking Stats
                        stats.getArmor() +
                        stats.getBonusArmor() +
                        stats.getBlock() +
                        stats.getBlockValue() +
                        stats.getBlockRating() +
                        stats.getDefense() +
                        stats.getDefenseRating() +
                        stats.getDodge() +
                        stats.getDodgeRating() +
                        stats.getMiss() +
                        stats.getParry() +
                        stats.getParryRating() +
                        stats.getResilience() +

                        stats.getBaseArmorMultiplier() +
                        stats.getBonusArmorMultiplier() +
                        stats.getBonusBlockValueMultiplier() +

                        // Threat Stats
                        stats.getArmorPenetration() +
                        stats.getArmorPenetrationRating() +
                        stats.getAttackPower() +
                        stats.getSpellPower() +
                        stats.getCritRating() +
                        stats.getPhysicalCrit() +
                        stats.getSpellCrit() +
                        stats.getSpellCritOnTarget() +
                        stats.getHasteRating() +
                        stats.getPhysicalHaste() +
                        stats.getSpellHaste() +
                        stats.getHitRating() +
                        stats.getPhysicalHit() +
                        stats.getSpellHit() +
                        stats.getExpertise() +
                        stats.getExpertiseRating() +
                        stats.getDeathbringerProc() +

                        // Special Stats
                        stats.getBonusShieldOfRighteousnessDamage() +
                        stats.getHolyShieldBlockValue() +
                        stats.getJudgementBlockValue() +
                        stats.getBonusHammerOfTheRighteousMultiplier() +
                        stats.getShieldOfRighteousnessBlockValue() +
                        stats.getBonusSealOfCorruptionDamageMultiplier() +
                        stats.getBonusSealOfRighteousnessDamageMultiplier() +
                        stats.getBonusSealOfVengeanceDamageMultiplier() +
                        stats.getConsecrationSpellPower() +
                        stats.getHighestStat() +
                        stats.getParagon() +
                        stats.getArcaneDamage() +
                        stats.getShadowDamage() +
                        stats.getHealed() +

                        // Multiplier Buffs/Debuffs
                        stats.getBonusStrengthMultiplier() +
                        stats.getBonusAgilityMultiplier() +
                        stats.getBonusStaminaMultiplier() +
                        stats.getBonusHealthMultiplier() +
                        stats.getBossAttackSpeedMultiplier() +
                        stats.getThreatIncreaseMultiplier() +
                        stats.getBaseArmorMultiplier() +
                        stats.getBonusArmorMultiplier() +
                        stats.getBonusAttackPowerMultiplier() +
                        stats.getBonusDamageMultiplier() +
                        stats.getBonusPhysicalDamageMultiplier() +
                        stats.getBonusHolyDamageMultiplier() +
                        stats.getDamageTakenMultiplier() +

                        // Resistances
                        stats.getArcaneResistance() +
                        stats.getNatureResistance() +
                        stats.getFireResistance() +
                        stats.getFrostResistance() +
                        stats.getShadowResistance() +
                        stats.getArcaneResistanceBuff() +
                        stats.getNatureResistanceBuff() +
                        stats.getFireResistanceBuff() +
                        stats.getFrostResistanceBuff() +
                        stats.getShadowResistanceBuff()
        ) != 0;

        for (SpecialEffect effect : stats.specialEffects()) {
            if (isTriggerRelevant(effect.getTrigger())) {
                relevant |= hasRelevantStats(effect.getStats());
            }
        }
        return relevant;
    }

    // C# CalculationsProtPaladin.cs:1443-1452 IsBuffRelevant.
    @Override
    public boolean isBuffRelevant(Buff buff, Character character) {
        Stats stats = buff.stats;
        boolean hasRelevantBuffStats = hasRelevantStats(stats);

        boolean notClassSetBonus = "Set Bonuses".equals(buff.group) && (buff.getAllowedClasses().size() != 1 || (buff.getAllowedClasses().size() == 1 && !buff.getAllowedClasses().contains(CharacterClass.Paladin)));

        boolean relevant = hasRelevantBuffStats && !notClassSetBonus;
        return relevant;
    }

    // C# CalculationsProtPaladin.cs:1454-1510 GetBuffsStats. The passive-ability auto-fixing
    // block is commented out in C#, so removedBuffs/addedBuffs stay empty; the restore loops are
    // kept for fidelity.
    public Stats getBuffsStats(Character character, CalculationOptionsProtPaladin calcOpts) {
        List<Buff> removedBuffs = new ArrayList<Buff>();
        List<Buff> addedBuffs = new ArrayList<Buff>();

        Stats statsBuffs = getBuffsStats(character.getActiveBuffs());

        for (Buff b : removedBuffs) {
            character.activeBuffsAdd(b.name);
        }
        for (Buff b : addedBuffs) {
            character.getActiveBuffs().remove(b);
        }

        return statsBuffs;
    }

    // C# CalculationsProtPaladin.cs:1511.
    public void setDefaults(Character character) {
    }

    @Override
    public String getOverallRatingLabel() {
        return "Overall Points";
    }
}
