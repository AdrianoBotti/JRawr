// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/CalculationsRetribution.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: UI-only members omitted (CalculationOptionsPanel, the RAWR3/Silverlight color branch and
// the internal Experimental string setter - it parses experiment flags for the options panel and has
// no other readers). The non-Silverlight label/color/optimizable/chart/glyph tables are ported as
// getCharacterDisplayCalculationLabels()/getSubPointNameColors()/getOptimizableCalculationLabels()/
// getCustomChartNames()/getCustomChartData()/getRelevantGlyphs().
// FRAMEWORK ADAPTATION: DeserializeDataObject uses a JDK-XML reflective reader instead of
// XmlSerializer (same approach as CalculationsCat/Bear/Healadin/ProtWarr); the Rotations list of
// Ability[] is parsed explicitly.
package rawr.models.retribution;

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
import rawr.base.util.Utilities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class CalculationsRetribution extends CalculationsBase {

    private static final double simulationTime = 20000d;
    private static final double allRotationsSimulationTime = 1000d;
    private static final String rotationsChartName = "Rotations";
    private static final String allRotationsChartName = "All Rotations (less precise)";

    // C# CalculationsRetribution.cs:19-151 DefaultGemmingTemplates.
    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        ////Relevant Gem IDs

        //Prismatic
        int[] tear = { 42701, 42702, 49110 }; //+stats

        //Yellow
        int[] rigid = { 39915, 40014, 40125, 42156 };  // +hit
        int[] quick = { 39918, 40017, 40128, 42150 };  // +haste

        //Red
        int[] bold = { 39900, 39996, 40111, 42142 };  // +str
        int[] precise = { 39910, 40003, 40118, 42154 }; // +exp

        //Orange (dual color gems don't have a JC variant)
        int[] inscribed = { 39947, 40037, 40142 };  // +str,+crit
        int[] etched = { 39948, 40038, 40143 };  // +str,+hit
        int[] fierce = { 39951, 40041, 40146 };  // +str,+haste
        // int[] expcrit = { ????, ????, ????, } // +exp,+crit  Doesn't exist
        int[] accurate = { 39966, 40058, 40162 };  // +exp,+hit
        // int[] exphaste = { ????, ????, ????, } // +exp,+haste  Doesn't exist

        //Purple
        int[] sovereign = { 39934, 40022, 40129 }; // +str,+stam
        int[] guardians = { 39940, 40034, 40141 }; // +exp, +stam

        //Green
        int[] vivid = { 39975, 40088, 40166 }; // +hit,+stam

        //Meta
        int relentless = 41398;
        //int chaotic = 41285;

        List<GemmingTemplate> retval = new ArrayList<GemmingTemplate>();

        String[] groupName = { "Uncommon", "Rare", "Epic" };
        for (int i = 0; i < 3; i++) {
            retval.add(gemmingTemplate("Retribution", groupName[i], bold[i], bold[i], bold[i], bold[i], relentless, i == 2)); // Strength every slot - Don't match colors
            retval.add(gemmingTemplate("Retribution", groupName[i], bold[i], inscribed[i], tear[i], bold[i], relentless, i == 2)); // Strength in every slot - Match colors (Blue socket is least desired and gets the (unique) nightmare tear).
            retval.add(gemmingTemplate("Retribution", groupName[i], bold[i], tear[i], sovereign[i], bold[i], relentless, i == 2)); // Strength in every slot - Match colors (This is a catch in case we don't have blue sockets, in that case, yellow gets the tear to activate the meta).  Putting sovereign here in blue is just filler.
            retval.add(gemmingTemplate("Retribution", groupName[i], bold[i], inscribed[i], sovereign[i], bold[i], relentless, i == 2)); // Strength in every slot - match colors - no nightmare tear (in case we need to activate a 2nd bonus dependant on a blue)
            retval.add(gemmingTemplate("Retribution", groupName[i], etched[i], rigid[i], vivid[i], rigid[i], relentless, i == 2)); // Hit in every slot - Match colors (catch for hit deficiency)
            retval.add(gemmingTemplate("Retribution", groupName[i], precise[i], accurate[i], guardians[i], precise[i], relentless, i == 2)); // Expertise in every slot - Match colors (catch for expertise deficiency)
        }

        retval.add(gemmingTemplate("Retribution", "Jeweler", bold[3], bold[3], bold[3], bold[3], relentless, false)); // Strength in every slot - Don't match colors, Bold JC gems everywhere.
        retval.add(gemmingTemplate("Retribution", "Jeweler", bold[3], inscribed[2], tear[2], bold[3], relentless, false)); // Strength in every slot - match colors (Only red/prismatic gets JC gems)

        return retval;
    }

    private static GemmingTemplate gemmingTemplate(String model, String group, int redId, int yellowId, int blueId, int prismaticId, int metaId, boolean enabled) {
        GemmingTemplate template = new GemmingTemplate();
        template.setModel(model);
        template.setGroup(group);
        template.setRedId(redId);
        template.setYellowId(yellowId);
        template.setBlueId(blueId);
        template.setPrismaticId(prismaticId);
        template.setMetaId(metaId);
        template.setEnabled(enabled);
        return template;
    }

    // C# CalculationsRetribution.cs:156-185 SetDefaults.
    public void setDefaults(Character character) {
        character.activeBuffsAdd("Strength of Earth Totem");
        character.activeBuffsAdd("Blessing of Might");
        character.activeBuffsAdd("Improved Blessing of Might");
        character.activeBuffsAdd("Unleashed Rage");
        character.activeBuffsAdd("Sanctified Retribution");
        character.activeBuffsAdd("Swift Retribution");
        character.activeBuffsAdd("Arcane Intellect");
        character.activeBuffsAdd("Commanding Shout");
        character.activeBuffsAdd("Leader of the Pack");
        character.activeBuffsAdd("Windfury Totem");
        character.activeBuffsAdd("Elemental Oath");
        character.activeBuffsAdd("Power Word: Fortitude");
        character.activeBuffsAdd("Improved Power Word: Fortitude");
        character.activeBuffsAdd("Mark of the Wild");
        character.activeBuffsAdd("Improved Mark of the Wild");
        character.activeBuffsAdd("Sunder Armor");
        character.activeBuffsAdd("Faerie Fire");
        character.activeBuffsAdd("Heart of the Crusader");
        character.activeBuffsAdd("Blood Frenzy");
        character.activeBuffsAdd("Improved Scorch");
        character.activeBuffsAdd("Curse of the Elements");
        character.activeBuffsAdd("Misery");
        character.activeBuffsAdd("Blessing of Kings");
        character.activeBuffsAdd("Totem of Wrath (Spell Power)");
        character.activeBuffsAdd("Blessing of Kings (Str/Sta Bonus)");
        character.activeBuffsAdd("Flask of Endless Rage");
        character.activeBuffsAdd("Fish Feast");
    }

    // C# CalculationsRetribution.cs:187-209 GetRelevantGlyphs.
    private static List<String> relevantGlyphs;

    public List<String> getRelevantGlyphs() {
        if (relevantGlyphs == null) {
            relevantGlyphs = new ArrayList<String>();
            relevantGlyphs.add("Glyph of Judgement");
            relevantGlyphs.add("Glyph of Exorcism");
            relevantGlyphs.add("Glyph of Sense Undead");
            relevantGlyphs.add("Glyph of Consecration");
            relevantGlyphs.add("Glyph of Seal of Blood");
            relevantGlyphs.add("Glyph of Seal of Command");
            relevantGlyphs.add("Glyph of Seal of Vengeance");
            relevantGlyphs.add("Glyph of Seal of Righteousness");
            relevantGlyphs.add("Glyph of Crusader Strike");
            relevantGlyphs.add("Glyph of Hammer of Wrath");
            relevantGlyphs.add("Glyph of Avenging Wrath");
        }
        return relevantGlyphs;
    }

    // C# CalculationsRetribution.cs:211-231 OptimizableCalculationLabels.
    private String[] optimizableCalculationLabels = null;

    public String[] getOptimizableCalculationLabels() {
        if (this.optimizableCalculationLabels == null)
            this.optimizableCalculationLabels = new String[] {
                    "Health",
                    "% Chance to Miss (Melee)",
                    "% Chance to Miss (Spells)",
                    "% Chance to be Dodged",
                    "% Chance to be Parried",
                    "% Chance to be Avoided (Melee/Dodge)",
            };
        return this.optimizableCalculationLabels;
    }

    // C# CalculationsRetribution.cs:307-351 CharacterDisplayCalculationLabels.
    private String[] characterDisplayCalculationLabels = null;

    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        if (this.characterDisplayCalculationLabels == null) {
            List<String> labels = new ArrayList<String>(List.of(
                    "Basic Stats:Health",
                    "Basic Stats:Mana",
                    "Basic Stats:Strength",
                    "Basic Stats:Agility",
                    "Basic Stats:Attack Power",
                    "Basic Stats:Crit Chance",
                    "Basic Stats:Miss Chance",
                    "Basic Stats:Dodge Chance",
                    "Basic Stats:Melee Haste",
                    "Basic Stats:Weapon Damage",
                    "Basic Stats:Attack Speed",
                    "DPS Breakdown:Total DPS",
                    "DPS Breakdown:White",
                    "DPS Breakdown:Seal",
                    "DPS Breakdown:Crusader Strike",
                    "DPS Breakdown:Judgement",
                    "DPS Breakdown:Consecration",
                    "DPS Breakdown:Exorcism",
                    "DPS Breakdown:Divine Storm",
                    "DPS Breakdown:Hammer of Wrath",
                    "DPS Breakdown:Hand of Reckoning",
                    "DPS Breakdown:Other*From trinket procs",
                    "Rotation Info:Chosen Rotation",
                    "Rotation Info:Average SoV Stack",
                    "Rotation Info:SoV Overtake*How long you need to dps a target for SoV to do more DPS then SoR",
                    "Rotation Info:Crusader Strike CD",
                    "Rotation Info:Judgement CD",
                    "Rotation Info:Consecration CD",
                    "Rotation Info:Exorcism CD",
                    "Rotation Info:Divine Storm CD",
                    "Rotation Info:Hammer of Wrath CD"));
            this.characterDisplayCalculationLabels = labels.toArray(new String[labels.size()]);
        }
        return List.of(this.characterDisplayCalculationLabels);
    }

    // C# CalculationsRetribution.cs:257-269 SubPointNameColors (non-Silverlight branch).
    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> colors = new LinkedHashMap<String, String>();
        colors.put("DPS", "#FF0000");
        return colors;
    }

    // Stage B additive API (web chart panel): same label as the other melee models.
    @Override
    public String getOverallRatingLabel() {
        return "Overall Points";
    }

    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.Paladin;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationRetribution();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsRetribution();
    }

    // C# CalculationsRetribution.cs:365-372 DeserializeDataObject (XmlSerializer; JDK-XML reader here).
    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            CalculationOptionsRetribution calcOpts = new CalculationOptionsRetribution();
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
            Object value = readXmlValue(child, type, name);
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

    private static Object readXmlValue(org.w3c.dom.Element element, Class<?> type, String name) {
        if (name.equals("Rotations") && List.class.isAssignableFrom(type)) {
            List<Ability[]> rotations = new ArrayList<Ability[]>();
            for (org.w3c.dom.Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                org.w3c.dom.Element arrayElement = (org.w3c.dom.Element) node;
                List<Ability> abilities = new ArrayList<Ability>();
                for (org.w3c.dom.Node abilityNode = arrayElement.getFirstChild(); abilityNode != null; abilityNode = abilityNode.getNextSibling()) {
                    if (abilityNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                    String text = abilityNode.getTextContent().trim();
                    if (!text.isEmpty()) abilities.add(Ability.valueOf(text));
                }
                rotations.add(abilities.toArray(new Ability[abilities.size()]));
            }
            return rotations;
        }
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

    public static CalculationsRetribution instance;

    public static CalculationsRetribution getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("Retribution", CalculationsRetribution.class);
    }

    public static void register() {
        Calculations.registerModel("Retribution", CalculationsRetribution.class);
    }

    public CalculationsRetribution() {
        instance = this;
    }

    // C# CalculationsRetribution.cs:426-437 GetCharacterCalculations.
    @Override
    public CharacterCalculationsBase getCharacterCalculations(
            Character character,
            Item additionalItem,
            boolean referenceCalculation,
            boolean significantChange,
            boolean needsDisplayCalculations) {
        if (referenceCalculation && significantChange) {
            // Fresh top-level character calculation: drop shared effect interpolation caches so a
            // previous character cannot leak into this one (SUSPECTED_BUGS #15), like Healadin/Elemental.
            SpecialEffect.clearInterpolatorCaches();
        }
        return getCharacterCalculations(
                character,
                additionalItem,
                getCharacterRotation(character, additionalItem));
    }

    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, Rotation rot) {
        CalculationOptionsRetribution calcOpts = character.getCalculationOptions() instanceof CalculationOptionsRetribution
                ? (CalculationOptionsRetribution) character.getCalculationOptions()
                : null;
        float fightLength = calcOpts.getFightLength() * 60f;
        PaladinTalents talents = character.getPaladinTalents();
        CombatStats combats = rot.combats;
        Stats stats = combats.getStats();

        CharacterCalculationsRetribution calc = new CharacterCalculationsRetribution();
        calc.basicStats = getCharacterStats(character, additionalItem, false, null, 0);

        calc.attackSpeed = combats.attackSpeed;
        calc.weaponDamage = combats.weaponDamage;
        calc.toMiss = combats.getMeleeMissChance();
        calc.toBeDodged = combats.getToBeDodgedChance();
        calc.toBeParried = combats.getToBeParriedChance();
        calc.toBeResisted = combats.getSpellMissChance();

        calc.otherDPS = new MagicDamage(combats, stats.getArcaneDamage()).averageDamage()
                + new MagicDamage(combats, stats.getFireDamage()).averageDamage()
                + new MagicDamage(combats, stats.getShadowDamage()).averageDamage()
                + new MagicDamage(combats, stats.getFrostDamage()).averageDamage()
                + new MagicDamage(combats, stats.getNatureDamage()).averageDamage()
                + new MagicDamage(combats, stats.getHolyDamage()).averageDamage();
        rot.setDPS(calc);
        calc.setOverallPoints(calc.getDPSPoints());

        return calc;
    }

    public Rotation getCharacterRotation(Character character, Item additionalItem) {
        CalculationOptionsRetribution options =
                (CalculationOptionsRetribution) character.getCalculationOptions();

        if (!options.getSimulateRotation())
            return getCharacterRotation(character, additionalItem, null, 0);

        if (options.getRotations().isEmpty())
            return getCharacterRotation(
                    character,
                    additionalItem,
                    SimulatorParameters.defaultRotation(),
                    simulationTime);

        return findBestRotation(
                character,
                additionalItem,
                options.getRotations(),
                simulationTime);
    }

    private Rotation findBestRotation(
            Character character,
            Item additionalItem,
            List<Ability[]> rotations,
            double simulationTime) {
        float maxDPS = 0;
        Rotation bestRotation = null;
        for (Ability[] rotation : rotations) {
            Rotation currentRotation = getCharacterRotation(character, additionalItem, rotation, simulationTime);
            float currentDPS = currentRotation.dps();
            if (currentDPS > maxDPS) {
                maxDPS = currentDPS;
                bestRotation = currentRotation;
            }
        }

        return bestRotation;
    }

    public Rotation getCharacterRotation(
            Character character,
            Item additionalItem,
            Ability[] rotation,
            double simulationTime) {
        return createRotation(
                new CombatStats(
                        character,
                        getCharacterStats(character, additionalItem, true, rotation, simulationTime)),
                rotation,
                simulationTime);
    }

    // C# CalculationsRetribution.cs:539-542 GetCharacterStats (override).
    public Stats getCharacterStats(Character character, Item additionalItem) {
        return getCharacterRotation(character, additionalItem).combats.getStats();
    }

    public Rotation createRotation(CombatStats combats, Ability[] rotation, double simulationTime) {
        return combats.getCalcOpts().getSimulateRotation() ?
                (Rotation) new Simulator(combats, rotation, simulationTime) :
                (Rotation) new EffectiveCooldown(combats);
    }

    // rotation and simulationTime are only required when computeAverageStats == true
    // C# CalculationsRetribution.cs:552-637.
    public Stats getCharacterStats(
            Character character,
            Item additionalItem,
            boolean computeAverageStats,
            Ability[] rotation,
            double simulationTime) {
        PaladinTalents talents = character.getPaladinTalents();
        CalculationOptionsRetribution calcOpts = character.getCalculationOptions() instanceof CalculationOptionsRetribution
                ? (CalculationOptionsRetribution) character.getCalculationOptions()
                : null;

        Stats statsRace = BaseStats.getBaseStats(character.getLevel(), CharacterClass.Paladin, character.getRace());
        Stats statsBaseGear = new Stats();
        accumulateItemStats(statsBaseGear, character, additionalItem);
        Stats statsBuffs = getBuffsStats(character, calcOpts);

        // Adjust expertise for racial passive and for using Seal of Vengeance combined with the SoV glyph.
        statsRace.setExpertise(statsRace.getExpertise() + BaseStats.getRacialExpertise(character, ItemSlot.MainHand));
        if (talents.isGlyphOfSealOfVengeance() && calcOpts.getSeal() == SealOf.Vengeance)
            statsRace.setExpertise(statsRace.getExpertise() + 10f);

        // Combine stats
        Stats stats = Stats.add(Stats.add(statsBaseGear, statsBuffs), statsRace);
        // If wanted, Average out any Proc and OnUse effects into the stats
        if (computeAverageStats) {
            Stats statsTmp = stats.copy();
            convertRatings(statsTmp, talents, calcOpts.getTargetLevel());                // Convert ratings so we have right value for haste, weaponspeed and talents etc.

            float fightLength = calcOpts.getFightLength() * 60f;
            CombatStats combats = new CombatStats(character, statsTmp);
            Rotation rot = createRotation(combats, rotation, simulationTime);

            // Average out proc effects, and add to global stats.
            Stats statsAverage = new Stats();
            for (SpecialEffect effect : stats.specialEffects()) {
                statsAverage.accumulate(
                        processSpecialEffect(
                                effect,
                                rot,
                                calcOpts.getSeal(),
                                combats.baseWeaponSpeed,
                                fightLength,
                                calcOpts.getStackTrinketReset()));
            }
            stats.accumulate(statsAverage);

            // Death's Verdict/Vengeance (TOTC)
            // Known issue: We haven't yet accounted for bonus multipliers on str and agi
            if (stats.getStrength() > stats.getAgility())
                stats.setStrength(stats.getStrength() + (stats.getHighestStat() + stats.getParagon()));
            else
                stats.setAgility(stats.getAgility() + (stats.getHighestStat() + stats.getParagon()));

            // Deathbringer's Will (ICC, Saurfang)
            // Paladins get str, hasterating and crit procs.
            // They're all equally likely so we'll divide the proc by 3
            stats.setStrength(stats.getStrength() + stats.getDeathbringerProc() / 3f);
            stats.setHasteRating(stats.getHasteRating() + stats.getDeathbringerProc() / 3f);
            stats.setCritRating(stats.getCritRating() + stats.getDeathbringerProc() / 3f);
        }

        // No negative values (from possible charts)
        if (stats.getStrength() < 0)
            stats.setStrength(0);
        if (stats.getAgility() < 0)
            stats.setAgility(0);
        if (stats.getAttackPower() < 0)
            stats.setAttackPower(0);
        if (stats.getArmorPenetrationRating() < 0)
            stats.setArmorPenetrationRating(0);
        if (stats.getExpertiseRating() < 0)
            stats.setExpertiseRating(0);
        if (stats.getHitRating() < 0)
            stats.setHitRating(0);
        if (stats.getCritRating() < 0)
            stats.setCritRating(0);
        if (stats.getHasteRating() < 0)
            stats.setHasteRating(0);
        if (stats.getSpellPower() < 0)
            stats.setSpellPower(0);

        // ConvertRatings needs to be done AFTER accounting for the averaged stats, since stat multipliers
        // should affect the averaged stats also.
        convertRatings(stats, talents, calcOpts.getTargetLevel());

        return stats;
    }

    // C# CalculationsRetribution.cs:639-765 ProcessSpecialEffect.
    private Stats processSpecialEffect(
            SpecialEffect effect,
            Rotation rot,
            SealOf seal,
            float baseWeaponSpeed,
            float fightLength,
            int stackTrinketReset) {
        float trigger = 0f;
        float procChance = 1f;

        switch (effect.getTrigger()) {
            case MeleeCrit:
                trigger = 1f / rot.getMeleeCritsPerSec();
                break;

            case MeleeHit:
                trigger = 1f / rot.getMeleeAttacksPerSec();
                break;

            // Experimental.
            case MeleeAttack: { // [Tiny Abomination in a Jar] and [Shadowmourne]
                float MeleeAttackPerSec = rot.getMeleeAttacksPerSec() +                   // Meleehit
                        rot.sealProcsPerSec(rot.seal) +                 // Seal hit
                        rot.getAbilityCritsPerSecond(rot.cs) +          // -+
                        rot.getAbilityCritsPerSecond(rot.ds) +          //  +- Righteous Vengeance application/refresh (100% on CS/DS/Judge crit)
                        rot.getAbilityCritsPerSecond(rot.judge) +       // -+
                        rot.getAbilityHitsPerSecond(rot.judge);         // Judgement debuf application (100% on J Hit)
                if (seal == SealOf.Vengeance)
                    MeleeAttackPerSec += rot.white.chanceToLand() / rot.combats.attackSpeed; // Holy Vengeance (100% on melee hit when using SoV)

                trigger = 1f / MeleeAttackPerSec;
                break;
            }

            case PhysicalCrit:
                trigger = 1f / rot.getPhysicalCritsPerSec();
                break;

            case PhysicalHit:
                trigger = 1f / rot.getPhysicalAttacksPerSec();
                break;

            case DamageDone:
                trigger = 1f / rot.getAttacksPerSec();
                break;

            case SpellHit:                  // This technically counts heals also, but we aren't modelling ret doing heals
            case DamageSpellHit:
                trigger = 1f / rot.getSpellAttacksPerSec();
                break;

            case SpellCrit:                 // This technically counts heals also, but we aren't modelling ret doing heals
            case DamageSpellCrit:
                trigger = 1f / rot.getSpellCritsPerSec();
                break;

            case DamageOrHealingDone:
                // Need to add Self-heals
                trigger = 1f / rot.getAttacksPerSec();
                break;

            case CrusaderStrikeHit:
                trigger = rot.getCrusaderStrikeCD();
                procChance = rot.cs.chanceToLand();
                break;

            case JudgementHit:
                trigger = rot.getJudgementCD();
                procChance = rot.judge.chanceToLand();
                break;

            case DoTTick:                   // SoV is also a dot tick.
            case SealOfVengeanceTick:
                if (seal == SealOf.Vengeance) {
                    trigger = 3f;
                    procChance = 1f;
                } else {
                    return new Stats();
                }
                break;

            case Use:
                trigger = 0f;
                procChance = 1f;
                break;

            default:
                return new Stats();
        }

        if (effect.getTrigger() == Trigger.Use && effect.getStats().getRawSpecialEffectData() != null) {
            // Run Recursive Effects (like Victor's Call)
            Stats SubStats = processSpecialEffect(effect.getStats().getRawSpecialEffectData()[0], rot, seal, baseWeaponSpeed, fightLength, stackTrinketReset);
            float upTime = effect.getAverageUptime(trigger, procChance);
            Stats retVal = new Stats();
            retVal.accumulate(SubStats, upTime);
            return retVal;
        } else if (effect.getMaxStack() > 1) {
            if (effect.getStats().getMoteOfAnger() > 0) {
                // When in effect stats, MoteOfAnger is % of melee hits
                // When in character stats, MoteOfAnger is average procs per second
                Stats moteStats = new Stats();
                moteStats.setMoteOfAnger(effect.getStats().getMoteOfAnger() * effect.getAverageProcsPerSecond(trigger, procChance, baseWeaponSpeed, fightLength) / effect.getMaxStack());
                return moteStats;
            } else {
                Stats tempStats = null;
                for (SpecialEffect subeffect : effect.getStats().specialEffects()) {
                    tempStats = processSpecialEffect(subeffect, rot, seal, baseWeaponSpeed, effect.getDuration(), 0);
                }

                if (tempStats != null)
                    return Stats.multiply(tempStats, effect.getAverageStackSize(trigger, procChance, baseWeaponSpeed, fightLength, stackTrinketReset));
                else
                    return Stats.multiply(effect.getStats(), effect.getAverageStackSize(trigger, procChance, baseWeaponSpeed, fightLength, stackTrinketReset));
            }
        } else return effect.getAverageStats(trigger, procChance, baseWeaponSpeed, fightLength);
    }

    // Combine talents and buffs into primary and secondary stats.
    // Convert ratings into their percentage values.
    // C# CalculationsRetribution.cs:767-816 ConvertRatings.
    private void convertRatings(Stats stats, PaladinTalents talents, int targetLevel) {
        // Primary stats
        stats.setStrength(stats.getStrength() * ((1 + stats.getBonusStrengthMultiplier()) * (1f + talents.getDivineStrength() * .03f)));
        stats.setAgility(stats.getAgility() * (1 + stats.getBonusAgilityMultiplier()));
        stats.setStamina(stats.getStamina() * ((1 + stats.getBonusStaminaMultiplier()) *
                (1f + talents.getSacredDuty() * .04f) *
                (1f + talents.getCombatExpertise() * .02f)));
        stats.setIntellect(stats.getIntellect() * ((1 + stats.getBonusIntellectMultiplier()) * (1f + talents.getDivineIntellect() * .02f)));

        // Secondary stats
        // GetManaFromIntellect/GetHealthFromStamina account for the fact
        // that the first 20 Int/Sta only give 1 Mana/Health each.
        stats.setMana(stats.getMana() + StatConversion.getManaFromIntellect(stats.getIntellect(), CharacterClass.Paladin) * (1f + stats.getBonusManaMultiplier()));
        stats.setHealth(stats.getHealth() + StatConversion.getHealthFromStamina(stats.getStamina(), CharacterClass.Paladin));
        stats.setAttackPower((stats.getAttackPower() + stats.getStrength() * 2) * (1 + stats.getBonusAttackPowerMultiplier()));

        // Combat ratings
        stats.setExpertise(stats.getExpertise() + (talents.getCombatExpertise() * 2 + StatConversion.getExpertiseFromRating(stats.getExpertiseRating(), CharacterClass.Paladin)));
        stats.setPhysicalHit(stats.getPhysicalHit() + StatConversion.getPhysicalHitFromRating(stats.getHitRating(), CharacterClass.Paladin));
        stats.setSpellHit(stats.getSpellHit() + StatConversion.getSpellHitFromRating(stats.getHitRating(), CharacterClass.Paladin));

        float talentCrit =
                talents.getCombatExpertise() * .02f +
                        talents.getConviction() * .01f +
                        talents.getSanctityOfBattle() * .01f;
        stats.setPhysicalCrit(stats.getPhysicalCrit() +
                (StatConversion.getPhysicalCritFromRating(stats.getCritRating(), CharacterClass.Paladin) +
                        StatConversion.getPhysicalCritFromAgility(stats.getAgility(), CharacterClass.Paladin) +
                        talentCrit +
                        StatConversion.NPC_LEVEL_CRIT_MOD[targetLevel - 80])); // Mob crit suppression
        stats.setSpellCrit(stats.getSpellCrit() +
                (stats.getSpellCritOnTarget() +
                        StatConversion.getSpellCritFromRating(stats.getCritRating(), CharacterClass.Paladin) +
                        StatConversion.getSpellCritFromIntellect(stats.getIntellect(), CharacterClass.Paladin) +
                        talentCrit +
                        StatConversion.NPC_LEVEL_CRIT_MOD[targetLevel - 80])); // Mob crit suppression

        stats.setPhysicalHaste((1f + stats.getPhysicalHaste()) *
                (1f + StatConversion.getPhysicalHasteFromRating(stats.getHasteRating(), CharacterClass.Paladin))
                - 1f);
        stats.setSpellHaste((1f + stats.getSpellHaste()) *
                (1f + StatConversion.getSpellHasteFromRating(stats.getHasteRating(), CharacterClass.Paladin))
                - 1f);

        stats.setSpellPower(stats.getSpellPower() +
                (stats.getStrength() * talents.getTouchedByTheLight() * .2f +
                        stats.getAttackPower() * talents.getSheathOfLight() * .1f));
    }

    // C# CalculationsRetribution.cs:818-863 GetBuffsStats(Character, CalculationOptionsRetribution).
    public Stats getBuffsStats(Character character, CalculationOptionsRetribution calcOpts) {
        List<Buff> buffs = new ArrayList<Buff>(character.getActiveBuffs());

        Stats buffStats = getBuffsStats(buffs);

        // If the character itself has any rank of Swift Retribution.
        // Improved Moonkin Form and different ranks of Swift Retribution don't stack.
        // Only the strongest one must be in ActiveBuffs
        if ((character.getPaladinTalents().getSwiftRetribution() != 0) &&
                !character.getActiveBuffs().contains(Buff.getBuffByName("Swift Retribution")) &&
                !character.getActiveBuffs().contains(Buff.getBuffByName("Improved Moonkin Form"))) {
            Stats additionalStats = new Stats();
            additionalStats.setPhysicalHaste(character.getPaladinTalents().getSwiftRetribution() * 0.01f);
            additionalStats.setRangedHaste(character.getPaladinTalents().getSwiftRetribution() * 0.01f);
            additionalStats.setSpellHaste(character.getPaladinTalents().getSwiftRetribution() * 0.01f);

            buffStats = Stats.add(buffStats, additionalStats);
        }

        if ((character.getPaladinTalents().getHeartOfTheCrusader() != 0) &&
                !character.getActiveBuffs().contains(Buff.getBuffByName("Heart of the Crusader")) &&
                !character.getActiveBuffs().contains(Buff.getBuffByName("Master Poisoner")) &&
                !character.getActiveBuffs().contains(Buff.getBuffByName("Totem of Wrath"))) {
            Stats additionalStats = new Stats();
            additionalStats.setPhysicalCrit(character.getPaladinTalents().getHeartOfTheCrusader() * 0.01f);
            additionalStats.setSpellCritOnTarget(character.getPaladinTalents().getHeartOfTheCrusader() * 0.01f);

            buffStats = Stats.add(buffStats, additionalStats);
        }

        if ((character.getPaladinTalents().getSanctifiedRetribution() != 0) &&
                !character.getActiveBuffs().contains(Buff.getBuffByName("Sanctified Retribution")) &&
                !character.getActiveBuffs().contains(Buff.getBuffByName("Arcane Empowerment")) &&
                !character.getActiveBuffs().contains(Buff.getBuffByName("Ferocious Inspiration"))) {
            Stats additionalStats = new Stats();
            additionalStats.setBonusDamageMultiplier(character.getPaladinTalents().getSanctifiedRetribution() * 0.03f);

            buffStats = Stats.add(buffStats, additionalStats);
        }

        return buffStats;
    }

    // C# CalculationsRetribution.cs:871-889 RelevantItemTypes.
    private List<ItemType> relevantItemTypes = null;

    @Override
    public List<ItemType> getRelevantItemTypes() {
        if (this.relevantItemTypes == null) {
            this.relevantItemTypes = new ArrayList<ItemType>(List.of(
                    ItemType.None,
                    ItemType.Leather,
                    ItemType.Mail,
                    ItemType.Plate,
                    ItemType.Libram,
                    ItemType.Polearm,
                    ItemType.TwoHandAxe,
                    ItemType.TwoHandMace,
                    ItemType.TwoHandSword));
        }
        return this.relevantItemTypes;
    }

    // List of SpecialEffect Triggers that are relevant for retribution model
    // Ever trigger listed here needs an implementation in ProcessSpecialEffects()
    // A trigger not listed here should not appear in ProcessSpecialEffects()
    // C# CalculationsRetribution.cs:896-923.
    private static List<Trigger> relevantTriggers = null;

    private static List<Trigger> relevantTriggers() {
        if (relevantTriggers == null) {
            relevantTriggers = new ArrayList<Trigger>(List.of(
                    Trigger.Use,
                    Trigger.SpellCrit,
                    Trigger.SpellHit,
                    Trigger.DamageSpellCrit,
                    Trigger.DamageSpellHit,         // Black magic enchant ?
                    Trigger.PhysicalCrit,
                    Trigger.PhysicalHit,
                    Trigger.MeleeCrit,
                    Trigger.MeleeHit,
                    Trigger.MeleeAttack,
                    Trigger.DamageDone,
                    Trigger.DamageOrHealingDone,    // Darkmoon Card: Greatness
                    Trigger.DoTTick,
                    //Trigger.DamageTaken,
                    //Trigger.DamageAvoided,
                    Trigger.JudgementHit,
                    Trigger.CrusaderStrikeHit,
                    Trigger.SealOfVengeanceTick));
        }
        return relevantTriggers;
    }

    // C# CalculationsRetribution.cs:926-936 IsItemRelevant.
    @Override
    public boolean isItemRelevant(Item item) {
        // First we let normal rules (profession, class, relevant stats) decide
        boolean relevant = super.isItemRelevant(item);

        // Next we use our special stat relevancy filtering.
        if (relevant)
            relevant = hasPrimaryStats(item.getStats()) || (hasSecondaryStats(item.getStats()) && !hasUnwantedStats(item.getStats()));

        return relevant;
    }

    // C# CalculationsRetribution.cs:938-956 IsBuffRelevant.
    @Override
    public boolean isBuffRelevant(Buff buff, Character character) {
        // First we let normal rules (profession, class, relevant stats) decide
        boolean relevant = super.isBuffRelevant(buff, character);

        // Temporary FIX (?): buf.AllowedClasses is not currently being tested as part of base.IsBuffRelevant(). So we'll do it ourselves.
        if (relevant && !buff.getAllowedClasses().contains(CharacterClass.Paladin))
            relevant = false;

        // Next we use our special stat relevancy filtering on consumables. (party buffs only need filtering on relevant stats)
        if (relevant && (buff.group.equals("Elixirs and Flasks") || buff.group.equals("Potion") || buff.group.equals("Food") || buff.group.equals("Scrolls") || buff.group.equals("Temporary Buffs")))
            relevant = hasPrimaryStats(buff.stats) || (hasSecondaryStats(buff.stats) && !hasUnwantedStats(buff.stats));

        // Remove bloodlust, we have our own processing for it.
        if (relevant && buff.name.equals("Heroism/Bloodlust"))
            relevant = false;

        return relevant;
    }

    // C# CalculationsRetribution.cs:958-968 IsEnchantRelevant.
    @Override
    public boolean isEnchantRelevant(Enchant enchant, Character character) {
        // First we let the normal rules (profession, class, relevant stats) decide
        boolean relevant = super.isEnchantRelevant(enchant, character);

        // Next we use our special stat relevancy filtering.
        if (relevant)
            relevant = hasPrimaryStats(enchant.stats) || (hasSecondaryStats(enchant.stats) && !hasUnwantedStats(enchant.stats));

        return relevant;
    }

    // C# CalculationsRetribution.cs:970-1040 GetRelevantStats.
    public Stats getRelevantStats(Stats stats) {
        Stats s = new Stats();
        s.setHealth(stats.getHealth());
        s.setMana(stats.getMana());
        s.setStrength(stats.getStrength());
        s.setAgility(stats.getAgility());
        s.setIntellect(stats.getIntellect());
        s.setStamina(stats.getStamina());
        s.setAttackPower(stats.getAttackPower());
        s.setHitRating(stats.getHitRating());
        s.setCritRating(stats.getCritRating());
        s.setArmorPenetration(stats.getArmorPenetration());
        s.setArmorPenetrationRating(stats.getArmorPenetrationRating());
        s.setExpertiseRating(stats.getExpertiseRating());
        s.setHasteRating(stats.getHasteRating());
        s.setSpellCrit(stats.getSpellCrit());
        s.setSpellCritOnTarget(stats.getSpellCritOnTarget());
        s.setPhysicalCrit(stats.getPhysicalCrit());
        s.setPhysicalHaste(stats.getPhysicalHaste());
        s.setPhysicalHit(stats.getPhysicalHit());
        s.setSpellHit(stats.getSpellHit());
        s.setExpertise(stats.getExpertise());
        s.setParagon(stats.getParagon());
        s.setSpellPower(stats.getSpellPower());
        s.setBonusStrengthMultiplier(stats.getBonusStrengthMultiplier());
        s.setBonusStaminaMultiplier(stats.getBonusStaminaMultiplier());
        s.setBonusAgilityMultiplier(stats.getBonusAgilityMultiplier());
        s.setBonusIntellectMultiplier(stats.getBonusIntellectMultiplier());
        s.setBonusAttackPowerMultiplier(stats.getBonusAttackPowerMultiplier());
        s.setBonusCritMultiplier(stats.getBonusCritMultiplier());
        s.setBonusSpellCritMultiplier(stats.getBonusSpellCritMultiplier());
        s.setBonusPhysicalDamageMultiplier(stats.getBonusPhysicalDamageMultiplier());
        s.setBonusHolyDamageMultiplier(stats.getBonusHolyDamageMultiplier());
        s.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());
        s.setDivineStormMultiplier(stats.getDivineStormMultiplier());
        s.setBonusSealOfCorruptionDamageMultiplier(stats.getBonusSealOfCorruptionDamageMultiplier());
        s.setBonusSealOfRighteousnessDamageMultiplier(stats.getBonusSealOfRighteousnessDamageMultiplier());
        s.setBonusSealOfVengeanceDamageMultiplier(stats.getBonusSealOfVengeanceDamageMultiplier());
        s.setCrusaderStrikeDamage(stats.getCrusaderStrikeDamage());
        s.setConsecrationSpellPower(stats.getConsecrationSpellPower());
        s.setJudgementCDReduction(stats.getJudgementCDReduction());
        s.setDivineStormDamage(stats.getDivineStormDamage());
        s.setDivineStormCrit(stats.getDivineStormCrit());
        s.setCrusaderStrikeCrit(stats.getCrusaderStrikeCrit());
        s.setExorcismMultiplier(stats.getExorcismMultiplier());
        s.setHammerOfWrathMultiplier(stats.getHammerOfWrathMultiplier());
        s.setCrusaderStrikeMultiplier(stats.getCrusaderStrikeMultiplier());
        s.setJudgementCrit(stats.getJudgementCrit());
        s.setRighteousVengeanceCanCrit(stats.getRighteousVengeanceCanCrit());
        s.setSealMultiplier(stats.getSealMultiplier());
        s.setJudgementMultiplier(stats.getJudgementMultiplier());
        s.setDivineStormRefresh(stats.getDivineStormRefresh());
        s.setDeathbringerProc(stats.getDeathbringerProc());
        s.setMoteOfAnger(stats.getMoteOfAnger());
        s.setFireDamage(stats.getFireDamage());
        s.setFrostDamage(stats.getFrostDamage());
        s.setArcaneDamage(stats.getArcaneDamage());
        s.setShadowDamage(stats.getShadowDamage());
        s.setNatureDamage(stats.getNatureDamage());
        s.setHolyDamage(stats.getHolyDamage());
        s.setMovementSpeed(stats.getMovementSpeed());
        for (SpecialEffect effect : stats.specialEffects()) {
            if (relevantTriggers().contains(effect.getTrigger()) && hasRelevantStats(effect.getStats()))
                s.addSpecialEffect(effect);
        }
        return s;
    }

    // C# CalculationsRetribution.cs:1042-1046 HasRelevantStats.
    @Override
    public boolean hasRelevantStats(Stats stats) {
        // These 3 calls should amount to the same list of stats as used in GetRelevantStats()
        return hasPrimaryStats(stats) || hasSecondaryStats(stats) || hasExtraStats(stats);
    }

    /**
     * HasPrimaryStats() should return true if the Stats object has any stats that define the item
     * as being 'for your class/spec'. For melee classes this is typical melee stats like Strength,
     * Agility, AP, Expertise... For casters it would be spellpower, intellect, ...
     * As soon as an item/enchant/buff has any of the stats listed here, it will be assumed to be
     * relevant unless explicitely filtered out.
     * Stats that could be usefull for both casters and melee such as HitRating, CritRating and Haste
     * don't belong here, but are SecondaryStats. Specific melee versions of these do belong here
     * for melee, spell versions would fit here for casters.
     * C# CalculationsRetribution.cs:1058-1116.
     */
    public boolean hasPrimaryStats(Stats stats) {
        boolean PrimaryStats = // Base stats
                stats.getStrength() > 0 ||
                        stats.getAgility() > 0 ||
                        stats.getAttackPower() > 0 ||
                        stats.getArmorPenetration() > 0 ||
                        stats.getExpertise() > 0 || //?
                        // Combat ratings
                        stats.getArmorPenetrationRating() > 0 ||
                        stats.getExpertiseRating() > 0 ||
                        stats.getPhysicalHit() > 0 ||
                        stats.getPhysicalCrit() > 0 ||
                        stats.getPhysicalHaste() > 0 ||
                        // Stat and damage multipliers
                        stats.getBonusStrengthMultiplier() > 0 ||
                        stats.getBonusAgilityMultiplier() > 0 ||
                        stats.getBonusAttackPowerMultiplier() > 0 ||
                        stats.getBonusPhysicalDamageMultiplier() > 0 ||
                        stats.getBonusHolyDamageMultiplier() > 0 ||
                        stats.getBonusDamageMultiplier() > 0 ||
                        // Paladin specific stats (set bonusses)
                        stats.getDivineStormMultiplier() > 0 ||
                        stats.getBonusSealOfCorruptionDamageMultiplier() > 0 ||
                        stats.getBonusSealOfRighteousnessDamageMultiplier() > 0 ||
                        stats.getBonusSealOfVengeanceDamageMultiplier() > 0 ||
                        stats.getCrusaderStrikeDamage() > 0 ||
                        stats.getConsecrationSpellPower() > 0 ||
                        stats.getJudgementCDReduction() > 0 ||
                        stats.getDivineStormDamage() > 0 ||
                        stats.getDivineStormCrit() > 0 ||
                        stats.getCrusaderStrikeCrit() > 0 ||
                        stats.getExorcismMultiplier() > 0 ||
                        stats.getHammerOfWrathMultiplier() > 0 ||
                        stats.getCrusaderStrikeMultiplier() > 0 ||
                        stats.getJudgementCrit() > 0 ||
                        stats.getRighteousVengeanceCanCrit() > 0 ||
                        stats.getSealMultiplier() > 0 ||
                        stats.getJudgementMultiplier() > 0 ||
                        stats.getDivineStormRefresh() > 0 ||
                        // Item proc effects
                        stats.getParagon() > 0 ||            // Highest of Str or Agi. (Death's Verdict, TotC25/TotGC25)
                        stats.getDeathbringerProc() > 0 ||   // Chance to proc one of several subeffects. Paladins can get Str/Haste/Crit. (Deathbringer's Will, ICC25)
                        stats.getMoteOfAnger() > 0;           // Stacking buf, causes a weapon swing when full. (Tiny Abomination in a Jar, ICC25)

        if (!PrimaryStats) {
            for (SpecialEffect effect : stats.specialEffects()) {
                if (relevantTriggers().contains(effect.getTrigger()) && hasPrimaryStats(effect.getStats())) {
                    PrimaryStats = true;
                    break;
                }
            }
        }

        return PrimaryStats;
    }

    /**
     * HasSecondaryStats() should return true if the Stats object has any stats that are relevant for the
     * model but only to a smaller degree, so small that you wouldn't typically consider the item.
     * Stats that are usefull to both melee and casters (HitRating, CritRating & Haste) fit in here also.
     * An item/enchant/buff having these stats would be considered only if it doesn't have any of the
     * unwanted stats.  Group/Party buffs are slighly different, they would be considered regardless if
     * they have unwanted stats.
     * Note that a stat may be listed here since it impacts the model, but may also be listed as an unwanted stat.
     * C# CalculationsRetribution.cs:1127-1166.
     */
    public boolean hasSecondaryStats(Stats stats) {
        boolean SecondaryStats = // Caster stats
                stats.getIntellect() > 0 ||                // Intellect increases spellcrit, so it contributes to DPS.
                        stats.getSpellCrit() > 0 ||                // Exorcism can crit
                        stats.getSpellCritOnTarget() > 0 ||        // Exorcism
                        stats.getSpellHit() > 0 ||                 // Exorcism & Consecration (1st tick)
                        stats.getSpellPower() > 0 ||               // All holy damage effects benefit from spellpower
                        stats.getBonusIntellectMultiplier() > 0 || // See intellect
                        stats.getBonusSpellCritMultiplier() > 0 || // See spellcrit
                        // Generic DPS stats, useful for casters and melee.
                        stats.getHitRating() > 0 ||
                        stats.getCritRating() > 0 ||
                        stats.getHasteRating() > 0 ||
                        stats.getBonusCritMultiplier() > 0 ||
                        // Damage procs
                        stats.getFireDamage() > 0 ||
                        stats.getFrostDamage() > 0 ||
                        stats.getArcaneDamage() > 0 ||
                        stats.getShadowDamage() > 0 ||
                        stats.getNatureDamage() > 0 ||
                        stats.getHolyDamage() > 0 ||
                        // Special (unmodelled)
                        stats.getMovementSpeed() > 0;

        if (!SecondaryStats) {
            for (SpecialEffect effect : stats.specialEffects()) {
                if (relevantTriggers().contains(effect.getTrigger()) && hasSecondaryStats(effect.getStats())) {
                    SecondaryStats = true;
                    break;
                }
            }
        }

        return SecondaryStats;
    }

    /**
     * Return true if the Stats object has any stats that don't influence the model but that you do want
     * to display in tooltips and in calculated summary values.
     * C# CalculationsRetribution.cs:1172-1192.
     */
    public boolean hasExtraStats(Stats stats) {
        boolean ExtraStats = stats.getHealth() > 0 ||
                stats.getMana() > 0 ||
                stats.getStamina() > 0 ||
                stats.getBonusStaminaMultiplier() > 0;

        if (!ExtraStats) {
            for (SpecialEffect effect : stats.specialEffects()) {
                if (relevantTriggers().contains(effect.getTrigger()) && hasExtraStats(effect.getStats())) {
                    ExtraStats = true;
                    break;
                }
            }
        }

        return ExtraStats;
    }

    /**
     * Return true if the Stats object contains any stats that are making the item undesired.
     * Any item having only Secondary stats would be removed if it also has one of these.
     * C# CalculationsRetribution.cs:1198-1224.
     */
    public boolean hasUnwantedStats(Stats stats) {
        // List of stats that will filter out some buffs (Flasks, Elixirs & Scrolls), Enchants and Items.
        boolean UnwantedStats = stats.getSpellPower() > 0 ||
                stats.getIntellect() > 0 ||
                stats.getSpirit() > 0 ||
                stats.getMp5() > 0 ||
                stats.getDefenseRating() > 0 ||
                stats.getParryRating() > 0 ||
                stats.getDodgeRating() > 0 ||
                stats.getBlockRating() > 0 ||
                stats.getBlockValue() > 0;

        if (!UnwantedStats) {
            for (SpecialEffect effect : stats.specialEffects()) {
                if (/*RelevantTriggers.Contains(effect.Trigger) && */hasUnwantedStats(effect.getStats())) {    // An unwanted stat could be behind a trigger we don't model.
                    UnwantedStats = true;
                    break;
                }
            }
        }

        return UnwantedStats;
    }

    // Custom charts are extra charts which can be defined per model.
    // C# CalculationsRetribution.cs:1232-1249 CustomChartNames.
    private String[] customChartNames = null;

    public String[] getCustomChartNames() {
        if (this.customChartNames == null) {
            this.customChartNames = new String[] {
                    "Seals",
                    "Weapon Speed",
                    rotationsChartName,
                    allRotationsChartName
            };
        }
        return this.customChartNames;
    }

    // C# CalculationsRetribution.cs:1251-1336 GetCustomChartData.
    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        if (chartName.equals("Seals")) {
            CalculationOptionsRetribution initOpts = (CalculationOptionsRetribution) character.getCalculationOptions();

            Character baseChar = character.clone();
            CalculationOptionsRetribution baseOpts = initOpts.clone();
            baseChar.setCalculationOptions(baseOpts);
            baseOpts.setSeal(SealOf.None);
            CharacterCalculationsBase baseCalc = Calculations.getCharacterCalculations(baseChar);

            Character deltaChar = character.clone();
            CalculationOptionsRetribution deltaOpts = initOpts.clone();
            deltaChar.setCalculationOptions(deltaOpts);

            ComparisonCalculationBase Command;
            deltaOpts.setSeal(SealOf.Command);
            Command = Calculations.getCharacterComparisonCalculations(baseCalc, deltaChar, "Seal of Command", initOpts.getSeal() == SealOf.Command);
            Command.setItem(null);

            ComparisonCalculationBase Righteousness;
            deltaOpts.setSeal(SealOf.Righteousness);
            Righteousness = Calculations.getCharacterComparisonCalculations(baseCalc, deltaChar, "Seal of Righteousness", initOpts.getSeal() == SealOf.Righteousness);
            Righteousness.setItem(null);

            ComparisonCalculationBase Vengeance;
            deltaOpts.setSeal(SealOf.Vengeance);
            Vengeance = Calculations.getCharacterComparisonCalculations(baseCalc, deltaChar, "Seal of Vengeance", initOpts.getSeal() == SealOf.Vengeance);
            Vengeance.setItem(null);

            return new ComparisonCalculationBase[] { Command, Righteousness, Vengeance };
        }
        boolean isRotationsChart = chartName.equals(rotationsChartName);
        boolean isAllRotationsChart = chartName.equals(allRotationsChartName);
        if (isRotationsChart || isAllRotationsChart) {
            List<ComparisonCalculationBase> comparisons = new ArrayList<ComparisonCalculationBase>();
            Character baseChar = character.clone();
            CalculationOptionsRetribution options =
                    (CalculationOptionsRetribution) character.getCalculationOptions();

            Ability[] selectedRotation =
                    ((CharacterCalculationsRetribution) Calculations.getCharacterCalculations(character))
                            .rotation;

            Iterable<Ability[]> rotations =
                    isAllRotationsChart ? Rotation.getAllRotations() : options.getRotations();
            for (Ability[] rotation : rotations) {
                ComparisonCalculationBase compare = Calculations.getCharacterComparisonCalculations(
                        getCharacterCalculations(
                                baseChar,
                                null,
                                getCharacterRotation(
                                        baseChar,
                                        null,
                                        rotation,
                                        isAllRotationsChart ? allRotationsSimulationTime : simulationTime)),
                        SimulatorParameters.rotationString(rotation),
                        Utilities.areArraysEqual(rotation, selectedRotation));
                compare.setItem(null);
                comparisons.add(compare);
            }

            return comparisons.toArray(new ComparisonCalculationBase[comparisons.size()]);
        }
        if (chartName.equals("Weapon Speed")) {
            if (character.get(CharacterSlot.MainHand) == null)
                return new ComparisonCalculationBase[] { };

            return new ComparisonCalculationBase[] {
                    getWeaponSpeedComparison(character, 3.3f),
                    getWeaponSpeedComparison(character, 3.4f),
                    getWeaponSpeedComparison(character, 3.5f),
                    getWeaponSpeedComparison(character, 3.6f),
                    getWeaponSpeedComparison(character, 3.7f)
            };
        } else {
            return new ComparisonCalculationBase[0];
        }
    }

    // C# CalculationsRetribution.cs:1339-1361 GetWeaponSpeedComparison.
    private ComparisonCalculationBase getWeaponSpeedComparison(Character character, float speed) {
        Character adjustedCharacter = character.clone();
        adjustedCharacter.setIsLoading(true);
        ItemInstance mainHand = character.get(CharacterSlot.MainHand);
        adjustedCharacter.set(CharacterSlot.MainHand, new ItemInstance(
                adjustWeaponSpeed(mainHand.getItem(), speed),
                mainHand.getGem1(),
                mainHand.getGem2(),
                mainHand.getGem3(),
                mainHand.getEnchant()));

        ComparisonCalculationBase comparison = Calculations.getCharacterComparisonCalculations(
                Calculations.getCharacterCalculations(character),
                adjustedCharacter,
                String.format(Locale.ROOT, "%s Speed", NetFormatOneDecimal(speed)),
                mainHand.getItem().getSpeed() == speed);
        comparison.setItem(null);

        return comparison;
    }

    // C# CalculationsRetribution.cs:1363-1371 AdjustWeaponSpeed.
    private Item adjustWeaponSpeed(Item weapon, float speed) {
        Item adjustedWeapon = weapon.clone();
        adjustedWeapon.setMinDamage((int) Math.rint(weapon.getMinDamage() / weapon.getSpeed() * speed));
        adjustedWeapon.setMaxDamage((int) Math.rint(weapon.getMaxDamage() / weapon.getSpeed() * speed));
        adjustedWeapon.setSpeed(speed);

        return adjustedWeapon;
    }

    /** .NET custom format `0.0` on a Single parameter of string.Format. */
    private static String NetFormatOneDecimal(float value) {
        return rawr.base.util.NetFormat.fixed(value, 1);
    }
}
