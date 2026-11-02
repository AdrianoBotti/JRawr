// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/CalculationsHunter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// FRAMEWORK ADAPTATIONS:
//  * System.Drawing.Color chart colors are ported as hex strings (LinkedHashMap<String, String>,
//    the same convention as CalculationsHealPriest/ShadowPriest).
//  * The UI-only CalculationOptionsPanelHunter, the WinForms ErrorBox catches and the Skills-based
//    calculator classes (Rotation/Skills.Base/Skills.Shots, dead code in the original model) are
//    omitted; see the hunter port notes for the omitted members.
//  * deserializeDataObject uses a JDK-XML reader instead of XmlSerializer.
package rawr.models.hunter;

import rawr.base.stats.BaseStats;
import rawr.base.bosses.BossOptions;
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
import rawr.base.talents.HunterTalents;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.items.Item;
import rawr.base.items.ItemDamageType;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemQuality;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.effects.SpecialDamageProcs;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;
import rawr.models.hunter.HunterEnums.Aspect;
import rawr.models.hunter.HunterEnums.AspectUsage;
import rawr.models.hunter.HunterEnums.PetAttacks;
import rawr.models.hunter.HunterEnums.PetFamily;
import rawr.models.hunter.HunterEnums.PetHappiness;
import rawr.models.hunter.HunterEnums.Shots;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CalculationsHunter extends CalculationsBase {

    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        // == Relevant Gem IDs for Hunters ==
        // Red
        int[] deli = { 41434, 39997, 40112, 42143 }; // Agi
        int[] brit = { 39906, 39999, 40114, 36766 }; // AP
        int[] frac = { 39909, 40002, 40117, 42153 }; // ArP
        // Purple
        int[] shif = { 41460, 40023, 40130, 40130 }; // Agi  /Stam
        int[] bala = { 41450, 40029, 40136, 40136 }; // AP   /Stam
        int[] puis = { 41456, 40033, 40140, 40140 }; // ArP  /Stam
        int[] infd = { 41454, 40030, 40137, 40137 }; // AP   /Mp5
        int[] tenu = { 41462, 40024, 40131, 40131 }; // Agi  /Mp5
        // Blue
        int[] lust = { 41440, 40010, 40121, 42146 }; // Mp5
        // Green
        int[] vivd = { 41481, 40088, 40166, 40166 }; // Hit  /Stam
        int[] jagd = { 41468, 40086, 40165, 40165 }; // Crit /Stam
        int[] forc = { 41466, 40091, 40169, 40169 }; // Haste/Stam
        int[] dazl = { 41463, 40094, 40175, 40175 }; // Int  /Mp5
        int[] lmbt = { 41469, 40100, 40177, 40177 }; // Hit  /Mp5
        int[] sndr = { 41477, 40096, 40176, 40176 }; // Crit /Mp5
        int[] enrg = { 41465, 40105, 40179, 40179 }; // Haste/Mp5
        // Yellow
        int[] rigd = { 41447, 40014, 40125, 42156 }; // Hit
        int[] smth = { 41448, 40013, 40124, 42149 }; // Crit
        int[] quik = { 41446, 40017, 40128, 42150 }; // Haste
        // Orange
        int[] glnt = { 41491, 40044, 40148, 40148 }; // Agi  /Hit
        int[] ddly = { 41484, 40043, 40147, 40147 }; // Agi  /Crit
        int[] deft = { 41485, 40046, 40150, 40150 }; // Agi  /Haste
        int[] prst = { 41496, 40053, 40157, 40157 }; // AP   /Hit
        int[] wckd = { 41429, 40052, 40156, 40156 }; // AP   /Crit
        int[] strk = { 41501, 40055, 40159, 40159 }; // AP   /Haste
        // Meta
        int relentless = 41398; // 21 Agi  3% Crit DMG
        int chaotic = 41285; // 21 Crit 3% Crit DMG
        int[] metas = { relentless, chaotic };

        String[] groups = new String[] { "Uncommon", "Rare", "Epic", "Jeweler" };
        List<GemmingTemplate> templates = new ArrayList<GemmingTemplate>();
        int i = 0;
        String m = "Hunter";
        for (String s : groups) {
            boolean e = s.equals("Epic");
            for (int j = 0; j < metas.length; j++) {
                // Purity
                templates.add(gem(m, s, e, deli[i], deli[i], deli[i], deli[i], metas[j]));
                templates.add(gem(m, s, e, brit[i], brit[i], brit[i], brit[i], metas[j]));
                templates.add(gem(m, s, e, frac[i], frac[i], frac[i], frac[i], metas[j]));
                templates.add(gem(m, s, e, smth[i], smth[i], smth[i], smth[i], metas[j]));
                templates.add(gem(m, s, e, quik[i], quik[i], quik[i], quik[i], metas[j]));
                templates.add(gem(m, s, e, lust[i], lust[i], lust[i], lust[i], metas[j]));
                templates.add(gem(m, s, e, rigd[i], rigd[i], rigd[i], rigd[i], metas[j]));
                // Consider Socket Bonuses/Meta Activation
                templates.add(gem(m, s, e, deli[i], ddly[i], shif[i], deli[i], metas[j]));
                templates.add(gem(m, s, e, brit[i], wckd[i], bala[i], brit[i], metas[j]));
                templates.add(gem(m, s, e, frac[i], ddly[i], puis[i], frac[i], metas[j]));
                templates.add(gem(m, s, e, ddly[i], smth[i], jagd[i], smth[i], metas[j]));
                templates.add(gem(m, s, e, deft[i], quik[i], forc[i], quik[i], metas[j]));
                templates.add(gem(m, s, e, glnt[i], rigd[i], vivd[i], rigd[i], metas[j]));
                // Consider Hit
                templates.add(gem(m, s, e, deli[i], rigd[i], vivd[i], glnt[i], metas[j]));
                templates.add(gem(m, s, e, brit[i], rigd[i], vivd[i], prst[i], metas[j]));
                templates.add(gem(m, s, e, frac[i], rigd[i], vivd[i], frac[i], metas[j]));
                templates.add(gem(m, s, e, ddly[i], rigd[i], vivd[i], smth[i], metas[j]));
                templates.add(gem(m, s, e, deft[i], rigd[i], vivd[i], quik[i], metas[j]));
                templates.add(gem(m, s, e, tenu[i], rigd[i], lust[i], lust[i], metas[j]));
                // Consider Mp5
                templates.add(gem(m, s, false, deli[i], ddly[i], tenu[i], lust[i], metas[j]));
                templates.add(gem(m, s, false, brit[i], wckd[i], infd[i], lust[i], metas[j]));
                templates.add(gem(m, s, false, frac[i], ddly[i], tenu[i], lust[i], metas[j]));
                templates.add(gem(m, s, false, tenu[i], smth[i], sndr[i], lust[i], metas[j]));
                templates.add(gem(m, s, false, deft[i], quik[i], enrg[i], lust[i], metas[j]));
                // dazl, lmbt and strk keep their arrays defined above even though no template uses them
            }
            i++;
        }

        return templates;
    }

    private static GemmingTemplate gem(String model, String group, boolean enabled, int red, int yellow, int blue, int prismatic, int meta) {
        GemmingTemplate t = new GemmingTemplate();
        t.setModel(model);
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
        if (this.characterDisplayCalculationLabels == null) {
            this.characterDisplayCalculationLabels = new String[] {
                    "Basic Stats:Health and Stamina",
                    "Basic Stats:Mana",
                    "Basic Stats:Armor",
                    "Basic Stats:Agility",
                    "Basic Stats:Ranged Attack Power",
                    "Basic Stats:Intellect",
                    "Basic Stats:Hit*8.00% chance to miss base for Yellow Attacks\n"
                            + "Focused Aim 0 - 8%-0%=8%=263 Rating soft cap\n"
                            + "Focused Aim 1 - 8%-1%=7%=230 Rating soft cap\n"
                            + "Focused Aim 2 - 8%-2%=6%=197 Rating soft cap\n"
                            + "Focused Aim 3 - 8%-3%=5%=164 Rating soft cap",
                    "Basic Stats:Crit",
                    "Basic Stats:Armor Penetration*Rating Cap 1400",
                    "Basic Stats:Haste",

                    "Pet Stats:Pet Attack Power",
                    "Pet Stats:Pet Hit %",
                    "Pet Stats:Pet Dodge %",
                    "Pet Stats:Pet Melee Crit %",
                    "Pet Stats:Pet Specials Crit %",
                    "Pet Stats:Pet White DPS",
                    "Pet Stats:Pet Kill Command DPS",
                    "Pet Stats:Pet Specials DPS",

                    "Shot Stats:Aimed Shot",
                    "Shot Stats:Arcane Shot",
                    "Shot Stats:Multi Shot",
                    "Shot Stats:Silencing Shot",
                    "Shot Stats:Steady Shot",
                    "Shot Stats:Kill Shot",
                    "Shot Stats:Explosive Shot",
                    "Shot Stats:Black Arrow",
                    "Shot Stats:Volley",
                    "Shot Stats:Chimera Shot",

                    "Sting Stats:Serpent Sting",
                    "Sting Stats:Scorpid Sting",
                    "Sting Stats:Viper Sting",

                    "Trap Stats:Immolation Trap",
                    "Trap Stats:Explosive Trap",
                    "Trap Stats:Freezing Trap",
                    "Trap Stats:Frost Trap",

                    "Mana:Mana Usage Per Second",
                    "Mana:Mana Regen Per Second",
                    "Mana:Normal Change",
                    "Mana:Change during Viper",
                    "Mana:Time to OOM",
                    "Mana:Time to Full",
                    "Mana:Viper Damage Penalty",
                    "Mana:Viper Uptime",
                    "Mana:No Mana Damage Penalty",

                    "Hunter DPS:Autoshot DPS",
                    "Hunter DPS:Priority Rotation DPS",
                    "Hunter DPS:Wild Quiver DPS",
                    "Hunter DPS:Kill Shot low HP gain",
                    "Hunter DPS:Aspect Loss",
                    "Hunter DPS:Piercing Shots DPS",
                    "Hunter DPS:Special DMG Procs DPS*Like Bandit's Insignia or Hand-Mounted Pyro Rockets",

                    "Combined DPS:Hunter DPS",
                    "Combined DPS:Pet DPS",
                    "Combined DPS:Total DPS"
            };
        }
        return List.of(this.characterDisplayCalculationLabels);
    }

    private String[] optimizableCalculationLabels = null;

    public String[] getOptimizableCalculationLabels() {
        if (this.optimizableCalculationLabels == null) {
            this.optimizableCalculationLabels = new String[] {
                    "Health",
                    "Attack Power",
                    "Agility",
                    "Mana",
                    "Crit %",
                    "Haste %",
                    "Armor Penetration %",
                    "% Chance to Miss (Yellow)",
            };
        }
        return this.optimizableCalculationLabels;
    }

    private LinkedHashMap<String, String> subPointNameColors = null;
    private LinkedHashMap<String, String> subPointNameColorsDps = null;
    private LinkedHashMap<String, String> subPointNameColorsMps = null;
    private LinkedHashMap<String, String> subPointNameColorsDpm = null;

    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        if (subPointNameColorsDps == null) {
            subPointNameColorsDps = new LinkedHashMap<String, String>();
            subPointNameColorsDps.put("Hunter DPS", "#FF0000");
            subPointNameColorsDps.put("Pet DPS", "#FF6400");
            subPointNameColorsDps.put("Hunter Survivability", "#408020");
            subPointNameColorsDps.put("Pet Survivability", "#1D8357");
        }
        if (subPointNameColorsMps == null) {
            subPointNameColorsMps = new LinkedHashMap<String, String>();
            subPointNameColorsMps.put("MPS", "#0000FF");
        }
        if (subPointNameColorsDpm == null) {
            subPointNameColorsDpm = new LinkedHashMap<String, String>();
            subPointNameColorsDpm.put("Damage per Mana", "#0000FF");
        }
        if (subPointNameColors == null) {
            subPointNameColors = subPointNameColorsDps;
        }
        return subPointNameColors;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationHunter();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsHunter();
    }

    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            org.w3c.dom.Element root = document.getDocumentElement();
            CalculationOptionsHunter calcOpts = new CalculationOptionsHunter();
            for (org.w3c.dom.Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                org.w3c.dom.Element child = (org.w3c.dom.Element) node;
                String name = child.getTagName();
                String text = child.getTextContent().trim();
                switch (name) {
                    case "_TimeSpentSub20": case "TimeSpentSub20": calcOpts.timeSpentSub20 = intOr(text, 0); break;
                    case "_TimeSpent35To20": case "TimeSpent35To20": calcOpts.timeSpent35To20 = intOr(text, 0); break;
                    case "_BossHPPerc": case "BossHPPerc": calcOpts.bossHPPerc = floatOr(text, 0f); break;
                    case "_CDCutoff": case "CDCutoff": calcOpts.cdCutoff = intOr(text, 0); break;
                    case "_AspectUsage": case "AspectUsage": calcOpts.aspectUsage = AspectUsage.valueOf(text); break;
                    case "_UseBeastDuringBestialWrath": case "UseBeastDuringBestialWrath": calcOpts.useBeastDuringBestialWrath = Boolean.parseBoolean(text); break;
                    case "_UseRotationTest": case "UseRotationTest": calcOpts.useRotationTest = Boolean.parseBoolean(text); break;
                    case "_RandomizeProcs": case "RandomizeProcs": calcOpts.randomizeProcs = Boolean.parseBoolean(text); break;
                    case "_PriorityIndex1": case "PriorityIndex1": calcOpts.priorityIndex1 = intOr(text, 0); break;
                    case "_PriorityIndex2": case "PriorityIndex2": calcOpts.priorityIndex2 = intOr(text, 0); break;
                    case "_PriorityIndex3": case "PriorityIndex3": calcOpts.priorityIndex3 = intOr(text, 0); break;
                    case "_PriorityIndex4": case "PriorityIndex4": calcOpts.priorityIndex4 = intOr(text, 0); break;
                    case "_PriorityIndex5": case "PriorityIndex5": calcOpts.priorityIndex5 = intOr(text, 0); break;
                    case "_PriorityIndex6": case "PriorityIndex6": calcOpts.priorityIndex6 = intOr(text, 0); break;
                    case "_PriorityIndex7": case "PriorityIndex7": calcOpts.priorityIndex7 = intOr(text, 0); break;
                    case "_PriorityIndex8": case "PriorityIndex8": calcOpts.priorityIndex8 = intOr(text, 0); break;
                    case "_PriorityIndex9": case "PriorityIndex9": calcOpts.priorityIndex9 = intOr(text, 0); break;
                    case "_PriorityIndex10": case "PriorityIndex10": calcOpts.priorityIndex10 = intOr(text, 0); break;
                    case "_PetPriority1": case "PetPriority1": calcOpts.petPriority1 = PetAttacks.valueOf(text); break;
                    case "_PetPriority2": case "PetPriority2": calcOpts.petPriority2 = PetAttacks.valueOf(text); break;
                    case "_PetPriority3": case "PetPriority3": calcOpts.petPriority3 = PetAttacks.valueOf(text); break;
                    case "_PetPriority4": case "PetPriority4": calcOpts.petPriority4 = PetAttacks.valueOf(text); break;
                    case "_PetPriority5": case "PetPriority5": calcOpts.petPriority5 = PetAttacks.valueOf(text); break;
                    case "_PetPriority6": case "PetPriority6": calcOpts.petPriority6 = PetAttacks.valueOf(text); break;
                    case "_PetPriority7": case "PetPriority7": calcOpts.petPriority7 = PetAttacks.valueOf(text); break;
                    case "petActiveBuffs": {
                        List<String> names = new ArrayList<String>(calcOpts.petActiveBuffsXml);
                        names.add(text);
                        calcOpts.petActiveBuffsXml = names;
                        break;
                    }
                    case "waitForCooldown": calcOpts.waitForCooldown = floatOr(text, 0f); break;
                    case "interleaveLAL": calcOpts.interleaveLAL = Boolean.parseBoolean(text); break;
                    case "prioritiseArcAimedOverSteady": calcOpts.prioritiseArcAimedOverSteady = Boolean.parseBoolean(text); break;
                    case "debugShotRotation": calcOpts.debugShotRotation = Boolean.parseBoolean(text); break;
                    case "TargetLevel": calcOpts.targetLevel = intOr(text, 0); break;
                    case "TargetArmor": calcOpts.targetArmor = floatOr(text, 0f); break;
                    case "Duration": calcOpts.duration = intOr(text, 0); break;
                    case "MultipleTargets": calcOpts.multipleTargets = Boolean.parseBoolean(text); break;
                    case "MultipleTargetsPerc": calcOpts.multipleTargetsPerc = floatOr(text, 0f); break;
                    case "Lag": calcOpts.lag = floatOr(text, 0f); break;
                    case "React": calcOpts.react = floatOr(text, 0f); break;
                    case "SelectedAspect": calcOpts.selectedAspect = Aspect.valueOf(text); break;
                    case "PetLevel": calcOpts.petLevel = intOr(text, 0); break;
                    case "SelectedArmoryPet": calcOpts.selectedArmoryPet = intOr(text, 0); break;
                    case "PetHappinessLevel": calcOpts.petHappinessLevel = PetHappiness.valueOf(text); break;
                    case "petTalents": calcOpts.petTalents = text; break;
                    case "PetFamily": calcOpts.petFamily = PetFamily.valueOf(text); break;
                    case "HideBadItems_Spl": calcOpts.hideBadItemsSpl = Boolean.parseBoolean(text); break;
                    case "HideBadItems_PvP": calcOpts.hideBadItemsPvP = Boolean.parseBoolean(text); break;
                    case "PTRMode": calcOpts.ptrMode = Boolean.parseBoolean(text); break;
                    case "SurvScale": calcOpts.survScale = floatOr(text, 0f); break;
                    case "SE_UseDur": calcOpts.seUseDur = Boolean.parseBoolean(text); break;
                    case "StatsList": {
                        List<Boolean> values = new ArrayList<Boolean>();
                        for (org.w3c.dom.Node n = child.getFirstChild(); n != null; n = n.getNextSibling()) {
                            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                            values.add(Boolean.parseBoolean(n.getTextContent().trim()));
                        }
                        boolean[] list = new boolean[values.size()];
                        for (int k = 0; k < values.size(); k++) list[k] = values.get(k);
                        calcOpts.statsList = list;
                        break;
                    }
                    case "StatsIncrement": calcOpts.statsIncrement = intOr(text, 0); break;
                    case "CalculationToGraph": calcOpts.calculationToGraph = text; break;
                    case "useKillCommand": calcOpts.useKillCommand = Boolean.parseBoolean(text); break;
                    case "gcdsToLayImmoTrap": calcOpts.gcdsToLayImmoTrap = floatOr(text, 0f); break;
                    case "gcdsToLayExploTrap": calcOpts.gcdsToLayExploTrap = floatOr(text, 0f); break;
                    case "gcdsToVolley": calcOpts.gcdsToVolley = floatOr(text, 0f); break;
                    case "LALShotToUse": calcOpts.lalShotToUse = Shots.valueOf(text); break;
                    case "LALShotsReplaced": calcOpts.lalShotsReplaced = intOr(text, 0); break;
                    case "LALProcChance": calcOpts.lalProcChance = floatOr(text, 0f); break;
                    default: break;
                }
            }

            // convert buffs here!
            List<Buff> buffs = new ArrayList<Buff>();
            for (String buffName : calcOpts.petActiveBuffsXml) {
                Buff buff = Buff.getBuffByName(buffName);
                if (buff != null) buffs.add(buff);
            }
            calcOpts.setPetActiveBuffs(buffs);
            // NOTE: the C# XmlSerializer path stores <petTalents> in the string property only; the
            // PetTalentTreeData object is built by the options panel, not by DeserializeDataObject,
            // so the calculation sees an all-zero pet talent tree (this matches the oracle dump).

            return calcOpts;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int intOr(String text, int fallback) {
        try {
            return text.isEmpty() ? fallback : Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float floatOr(String text, float fallback) {
        try {
            return text.isEmpty() ? fallback : Float.parseFloat(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // Relevancy

    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.Hunter;
    }

    private List<ItemType> relevantItemTypes = null;

    @Override
    public List<ItemType> getRelevantItemTypes() {
        if (this.relevantItemTypes == null) {
            this.relevantItemTypes = new ArrayList<ItemType>(List.of(
                    ItemType.None,
                    ItemType.AmmoPouch,
                    ItemType.Arrow,
                    ItemType.Bow,
                    ItemType.Bullet,
                    ItemType.Crossbow,
                    ItemType.Dagger,
                    ItemType.FistWeapon,
                    ItemType.Gun,
                    ItemType.Leather,
                    ItemType.Mail,
                    ItemType.OneHandAxe,
                    ItemType.OneHandSword,
                    ItemType.Polearm,
                    ItemType.Quiver,
                    ItemType.Staff,
                    ItemType.TwoHandAxe,
                    ItemType.TwoHandSword));
        }
        return this.relevantItemTypes;
    }

    private boolean hidingBadStuff() {
        return hidingBadStuffSpl || hidingBadStuffPvP;
    }

    private static boolean hidingBadStuffSpl = true;

    static boolean isHidingBadStuffSpl() {
        return hidingBadStuffSpl;
    }

    static void setHidingBadStuffSpl(boolean value) {
        hidingBadStuffSpl = value;
    }

    private static boolean hidingBadStuffPvP = true;

    static boolean isHidingBadStuffPvP() {
        return hidingBadStuffPvP;
    }

    static void setHidingBadStuffPvP(boolean value) {
        hidingBadStuffPvP = value;
    }

    public Stats getRelevantStats(Stats stats) {
        Stats relevantStats = new Stats();
        // Basic Stats
        relevantStats.setStamina(stats.getStamina());
        relevantStats.setHealth(stats.getHealth());
        relevantStats.setAgility(stats.getAgility());
        relevantStats.setStrength(stats.getStrength());
        relevantStats.setIntellect(stats.getIntellect());
        relevantStats.setMana(stats.getMana());
        relevantStats.setMp5(stats.getMp5());
        relevantStats.setArmor(stats.getArmor());
        relevantStats.setBonusArmor(stats.getBonusArmor());
        // Ratings
        relevantStats.setAttackPower(stats.getAttackPower());
        relevantStats.setRangedAttackPower(stats.getRangedAttackPower());
        relevantStats.setPetAttackPower(stats.getPetAttackPower());
        relevantStats.setPhysicalCrit(stats.getPhysicalCrit());
        relevantStats.setCritRating(stats.getCritRating());
        relevantStats.setRangedCritRating(stats.getRangedCritRating());
        relevantStats.setPhysicalHit(stats.getPhysicalHit());
        relevantStats.setHitRating(stats.getHitRating());
        relevantStats.setRangedHitRating(stats.getRangedHitRating());
        relevantStats.setPhysicalHaste(stats.getPhysicalHaste());
        relevantStats.setHasteRating(stats.getHasteRating());
        relevantStats.setRangedHasteRating(stats.getRangedHasteRating());

        relevantStats.setArmorPenetrationRating(stats.getArmorPenetrationRating());
        relevantStats.setArmorPenetration(stats.getArmorPenetration());
        relevantStats.setMiss(stats.getMiss());
        relevantStats.setScopeDamage(stats.getScopeDamage());

        // Special
        relevantStats.setHighestStat(stats.getHighestStat());
        relevantStats.setParagon(stats.getParagon());
        relevantStats.setDeathbringerProc(stats.getDeathbringerProc());
        relevantStats.setMovementSpeed(stats.getMovementSpeed());
        relevantStats.setStunDurReduc(stats.getStunDurReduc());
        relevantStats.setSnareRootDurReduc(stats.getSnareRootDurReduc());
        relevantStats.setFearDurReduc(stats.getFearDurReduc());
        relevantStats.setDarkmoonCardDeathProc(stats.getDarkmoonCardDeathProc());
        relevantStats.setManaRestoreFromMaxManaPerSecond(stats.getManaRestoreFromMaxManaPerSecond());
        relevantStats.setManaRestoreFromBaseManaPPM(stats.getManaRestoreFromBaseManaPPM());
        relevantStats.setManaorEquivRestore(stats.getManaorEquivRestore());
        relevantStats.setManaRestore(stats.getManaRestore());

        // Survivability
        relevantStats.setHealthRestore(stats.getHealthRestore());
        relevantStats.setHealthRestoreFromMaxHealth(stats.getHealthRestoreFromMaxHealth());
        relevantStats.setDodge(stats.getDodge());
        relevantStats.setDodgeRating(stats.getDodgeRating());
        relevantStats.setParry(stats.getParry());
        relevantStats.setParryRating(stats.getParryRating());

        // Set Bonuses
        relevantStats.setBonusHunter_PvP_4pc(stats.getBonusHunter_PvP_4pc());
        relevantStats.setBonusHunter_T7_4P_ViperSpeed(stats.getBonusHunter_T7_4P_ViperSpeed());
        relevantStats.setBonusHunter_T8_2P_SerpDmg(stats.getBonusHunter_T8_2P_SerpDmg());
        relevantStats.setBonusHunter_T9_2P_SerpCanCrit(stats.getBonusHunter_T9_2P_SerpCanCrit());
        relevantStats.setBonusHunter_T9_4P_SteadyShotPetAPProc(stats.getBonusHunter_T9_4P_SteadyShotPetAPProc());

        // Multipliers
        relevantStats.setBonusStaminaMultiplier(stats.getBonusStaminaMultiplier());
        relevantStats.setBonusAgilityMultiplier(stats.getBonusAgilityMultiplier());
        relevantStats.setBonusIntellectMultiplier(stats.getBonusIntellectMultiplier());
        relevantStats.setBonusAttackPowerMultiplier(stats.getBonusAttackPowerMultiplier());
        relevantStats.setBonusRangedAttackPowerMultiplier(stats.getBonusRangedAttackPowerMultiplier());
        relevantStats.setBonusPetAttackPowerMultiplier(stats.getBonusPetAttackPowerMultiplier());

        relevantStats.setBonusManaPotion(stats.getBonusManaPotion());
        relevantStats.setDamageTakenMultiplier(stats.getDamageTakenMultiplier());
        relevantStats.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());
        relevantStats.setBaseArmorMultiplier(stats.getBaseArmorMultiplier());
        relevantStats.setBonusArmorMultiplier(stats.getBonusArmorMultiplier());
        relevantStats.setBonusCritMultiplier(stats.getBonusCritMultiplier());
        relevantStats.setBonusSpiritMultiplier(stats.getBonusSpiritMultiplier());
        relevantStats.setBonusPetDamageMultiplier(stats.getBonusPetDamageMultiplier());
        relevantStats.setBonusPetCritChance(stats.getBonusPetCritChance());
        relevantStats.setBonusManaMultiplier(stats.getBonusManaMultiplier());
        relevantStats.setBonusBleedDamageMultiplier(stats.getBonusBleedDamageMultiplier());
        relevantStats.setBonusPhysicalDamageMultiplier(stats.getBonusPhysicalDamageMultiplier());

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

        relevantStats.setZodProc(stats.getZodProc());

        for (SpecialEffect effect : stats.specialEffects()) {
            if ((effect.getTrigger() == Trigger.Use
                    || effect.getTrigger() == Trigger.PhysicalCrit
                    || effect.getTrigger() == Trigger.PhysicalHit
                    || effect.getTrigger() == Trigger.DoTTick
                    || effect.getTrigger() == Trigger.DamageDone
                    || effect.getTrigger() == Trigger.DamageOrHealingDone
                    || effect.getTrigger() == Trigger.DamageTaken
                    // Hunter Specific
                    || effect.getTrigger() == Trigger.RangedHit
                    || effect.getTrigger() == Trigger.RangedCrit
                    || effect.getTrigger() == Trigger.SteadyShotHit
                    || effect.getTrigger() == Trigger.PetClawBiteSmackCrit
                    || effect.getTrigger() == Trigger.HunterAutoShotHit
                    || effect.getTrigger() == Trigger.SerpentWyvernStingsDoDamage)
                    && hasRelevantStats(effect.getStats())) {
                relevantStats.addSpecialEffect(effect);
            }
        }
        return relevantStats;
    }

    @Override
    public boolean hasRelevantStats(Stats stats) {
        boolean relevant = hasWantedStats(stats) && !hasIgnoreStats(stats);
        return relevant;
    }

    private boolean hasWantedStats(Stats stats) {
        boolean isRelevant = (
                // Base Stats
                stats.getAgility() +
                        stats.getStrength() +
                        stats.getAttackPower() + stats.getRangedAttackPower() + stats.getPetAttackPower() +
                        stats.getIntellect() +
                        // Ratings
                        stats.getMp5() +
                        stats.getArmorPenetrationRating() +
                        stats.getCritRating() + stats.getRangedCritRating() +
                        stats.getHasteRating() + stats.getRangedHasteRating() +
                        stats.getHitRating() + stats.getRangedHitRating() +
                        // Bonuses
                        stats.getArmorPenetration() +
                        stats.getPhysicalCrit() +
                        stats.getRangedHaste() +
                        stats.getPhysicalHit() +
                        stats.getMovementSpeed() + stats.getStunDurReduc() + stats.getSnareRootDurReduc() + stats.getFearDurReduc() +
                        stats.getManaRestore() + // Mana Pots
                        stats.getManaRestoreFromMaxManaPerSecond() + // Replenishment Buffs
                        stats.getManaRestoreFromBaseManaPPM() + // Judgement of Wisdom Buff
                        // Target Debuffs
                        // Procs
                        stats.getDarkmoonCardDeathProc() +
                        stats.getHighestStat() +
                        stats.getParagon() +
                        stats.getManaorEquivRestore() +
                        stats.getDeathbringerProc() +
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
                        stats.getBonusFireDamageMultiplier() +
                        stats.getZodProc() +
                        // Multipliers
                        stats.getBonusAgilityMultiplier() +
                        stats.getBonusAttackPowerMultiplier() +
                        stats.getBonusRangedAttackPowerMultiplier() +
                        stats.getBonusPetAttackPowerMultiplier() +
                        stats.getBonusCritMultiplier() +
                        stats.getBonusIntellectMultiplier() +
                        stats.getBonusPetDamageMultiplier() +
                        stats.getBonusDamageMultiplier() +
                        stats.getBonusSpiritMultiplier() +
                        stats.getDamageTakenMultiplier() +
                        stats.getBaseArmorMultiplier() +
                        stats.getBonusArmorMultiplier() +
                        stats.getBonusBleedDamageMultiplier() +
                        stats.getBonusPhysicalDamageMultiplier() +
                        stats.getBonusManaMultiplier() +
                        // Set Bonuses
                        stats.getBonusHunter_T7_4P_ViperSpeed() +
                        stats.getBonusHunter_T8_2P_SerpDmg() +
                        stats.getBonusHunter_T9_2P_SerpCanCrit() +
                        stats.getBonusHunter_T9_4P_SteadyShotPetAPProc() +
                        stats.getBonusHunter_PvP_4pc() +
                        // Special
                        stats.getScopeDamage() +
                        stats.getBonusManaPotion() +
                        stats.getBonusPetCritChance()
        ) != 0;

        for (SpecialEffect e : stats.specialEffects()) {
            if (e.getTrigger() == Trigger.DamageDone
                    || e.getTrigger() == Trigger.DamageOrHealingDone
                    || e.getTrigger() == Trigger.DoTTick
                    || e.getTrigger() == Trigger.PhysicalCrit
                    || e.getTrigger() == Trigger.PhysicalHit
                    || e.getTrigger() == Trigger.Use
                    || e.getTrigger() == Trigger.DamageTaken
                    // Hunter Specific
                    || e.getTrigger() == Trigger.RangedHit
                    || e.getTrigger() == Trigger.RangedCrit
                    || e.getTrigger() == Trigger.SteadyShotHit
                    || e.getTrigger() == Trigger.PetClawBiteSmackCrit
                    || e.getTrigger() == Trigger.HunterAutoShotHit
                    || e.getTrigger() == Trigger.SerpentWyvernStingsDoDamage) {
                isRelevant |= hasRelevantStats(e.getStats());
            }
        }

        return isRelevant;
    }

    private boolean hasSurvivabilityStats(Stats stats) {
        boolean retVal = false;
        if ((stats.getHealth()
                + stats.getStamina()
                + stats.getBonusHealthMultiplier()
                + stats.getBonusStaminaMultiplier()
                + stats.getHealthRestore()
                + stats.getHealthRestoreFromMaxHealth()
        ) > 0) {
            retVal = true;
        }
        return retVal;
    }

    private boolean hasIgnoreStats(Stats stats) {
        if (!hidingBadStuff()) {
            return false;
        }
        return (
                // Remove Spellcasting only Stuff
                (hidingBadStuffSpl ? stats.getSpellPower() + stats.getSpellPenetration()
                        : 0f)
                        // Remove PvP Items
                        + (hidingBadStuffPvP ? stats.getResilience()
                                : 0f)
        ) > 0;
    }

    @Override
    public boolean isEnchantRelevant(Enchant enchant, Character character) {
        return isEnchantAllowedForClass(enchant, character.getCharacterClass())
                && isProfEnchantRelevant(enchant, character)
                && (hasWantedStats(enchant.stats)
                || (hasSurvivabilityStats(enchant.stats) && !hasIgnoreStats(enchant.stats)));
    }

    @Override
    public boolean isBuffRelevant(Buff buff, Character character) {
        String name = buff.name;
        // Force some buffs to active
        if (name.contains("Potion of Wild Magic")
                || name.contains("Insane Strength Potion")) {
            return true;
        }
        // Force some buffs to go away
        else if (!buff.getAllowedClasses().contains(CharacterClass.Hunter)
                // these four foods give stam, which is the only useful part of their buff.
                || name.contains("Spirit Food")
                || name.contains("Strength Food")
                || name.contains("Expertise Food")
                || name.contains("Spell Power Food")) {
            return false;
        }
        boolean haswantedStats = hasWantedStats(buff.stats);
        boolean hassurvStats = hasSurvivabilityStats(buff.stats);
        boolean hasbadstats = hasIgnoreStats(buff.stats);
        boolean retVal = haswantedStats || (hassurvStats && !hasbadstats);
        return retVal;
    }

    public boolean canUseAmmo() {
        return true;
    }

    @Override
    public boolean isItemRelevant(Item item) {
        if ( // Manual override for +X to all Stats gems
                item.getName().equals("Nightmare Tear")
                        || item.getName().equals("Enchanted Tear")
                        || item.getName().equals("Enchanted Pearl")
                        || item.getName().equals("Chromatic Sphere")) {
            return true;
        } else if (item.getSlot() == ItemSlot.Ranged && item.getType() == ItemType.Idol) {
            return false;
        } else if (item.getSlot() == ItemSlot.Projectile
                || (item.getSlot() == ItemSlot.Ranged && (item.getType() == ItemType.Gun || item.getType() == ItemType.Bow || item.getType() == ItemType.Crossbow))) {
            return true;
        } else {
            Stats stats = item.getStats();
            boolean wantedStats = hasWantedStats(stats);
            boolean survstats = hasSurvivabilityStats(stats);
            boolean ignoreStats = hasIgnoreStats(stats);
            return (wantedStats || survstats) && !ignoreStats && super.isItemRelevant(item);
        }
    }

    private static List<Buff> relevantPetBuffs = new ArrayList<Buff>();

    public static List<Buff> getRelevantPetBuffs() {
        if (relevantPetBuffs.isEmpty()) {
            relevantPetBuffs = new ArrayList<Buff>();
            for (Buff buff : Buff.getAllBuffs()) {
                if (isPetBuffRelevant(buff)) {
                    relevantPetBuffs.add(buff);
                }
            }
        }
        return relevantPetBuffs;
    }

    private static boolean isPetBuffRelevant(Buff buff) {
        if (buff.group.equals("Elixirs and Flasks")
                || buff.group.equals("Food")
                || buff.group.equals("Set Bonuses")
                || buff.group.equals("Profession Buffs")
                || buff.group.equals("Temporary Buffs")
                || buff.group.equals("Potion")) {
            return false;
        }

        // Greater Blessing of Kings
        if (buff.stats.getBonusAgilityMultiplier() != 0) return true;
        if (buff.stats.getBonusStrengthMultiplier() != 0) return true;
        if (buff.stats.getBonusStaminaMultiplier() != 0) return true;

        // Commanding Shout & Blood Pact
        if (buff.stats.getHealth() != 0) return true;

        // Greater Blessing of Might & Battle Shout
        if (buff.stats.getAttackPower() != 0) return true;
        if (buff.stats.getPetAttackPower() != 0) return true;

        // True Shot Aura (not you) & Abo. Might & Unl. Rage
        if (buff.stats.getBonusAttackPowerMultiplier() != 0) return true;
        if (buff.stats.getBonusPetAttackPowerMultiplier() != 0) return true;

        // Leader of the Pack/Rampage
        if (buff.stats.getPhysicalCrit() != 0) return true;

        // Strength of Earth Totem & Horn of Winter &  Gift of the Wild & Prayer of Fortitude & Scrolls
        if (buff.stats.getStrength() != 0) return true;
        if (buff.stats.getAgility() != 0) return true;
        if (buff.stats.getStamina() != 0) return true;

        // WF Totem & Imp. Icy Talons & Swift Ret & Moonkin Aura
        if (buff.stats.getPhysicalHaste() != 0) return true;

        // Sunder Armor, Sting
        if (buff.stats.getArmorPenetration() != 0) return true;

        // Ret Aura & Feroc. Insp.
        if (buff.stats.getBonusDamageMultiplier() != 0) return true;

        // Pet Food
        if (buff.stats.getPetStamina() != 0) return true;
        if (buff.stats.getPetStrength() != 0) return true;

        if (buff.stats.getRawSpecialEffectData() != null && buff.stats.getRawSpecialEffectData().length > 0) {
            Buff tmp = new Buff();
            tmp.group = buff.group;
            tmp.stats = buff.stats.getRawSpecialEffectData()[0].getStats();
            return isPetBuffRelevant(tmp);
        }

        return false;
    }

    public Stats getBuffsStats(Character character, CalculationOptionsHunter calcOpts) {
        List<Buff> removedBuffs = new ArrayList<Buff>();
        List<Buff> addedBuffs = new ArrayList<Buff>();

        List<Buff> buffGroup = new ArrayList<Buff>();
        // Passive Ability Auto-Fixing
        // Removes the Trueshot Aura Buff and it's equivalents Unleashed Rage and Abomination's Might if you are
        // maintaining it yourself. We are now calculating this internally for better accuracy and to provide
        // value to relevant talents
        if (character.getHunterTalents().getTrueshotAura() > 0) {
            buffGroup.clear();
            buffGroup.add(Buff.getBuffByName("Trueshot Aura"));
            buffGroup.add(Buff.getBuffByName("Unleashed Rage"));
            buffGroup.add(Buff.getBuffByName("Abomination's Might"));
            maintBuffHelper(buffGroup, character, removedBuffs);
        }

        // Removes the Hunter's Mark Buff and it's Children 'Glyphed', 'Improved' and 'Both' if you are
        // maintaining it yourself. We are now calculating this internally for better accuracy and to provide
        // value to relevant talents
        if (character.getHunterTalents().getImprovedHuntersMark() + (character.getHunterTalents().isGlyphOfHuntersMark() ? 1 : 0) > 0) {
            buffGroup.clear();
            buffGroup.add(Buff.getBuffByName("Hunter's Mark"));
            buffGroup.add(Buff.getBuffByName("Glyphed Hunter's Mark"));
            buffGroup.add(Buff.getBuffByName("Improved Hunter's Mark"));
            buffGroup.add(Buff.getBuffByName("Improved and Glyphed Hunter's Mark"));
            maintBuffHelper(buffGroup, character, removedBuffs);
        }

        Stats statsBuffs = getBuffsStats(character.getActiveBuffs());

        for (Buff b : removedBuffs) {
            character.activeBuffsAdd(b);
        }
        for (Buff b : addedBuffs) {
            character.getActiveBuffs().remove(b);
        }

        return statsBuffs;
    }

    private void maintBuffHelper(List<Buff> buffGroup, Character character, List<Buff> removedBuffs) {
        for (Buff b : buffGroup) {
            if (character.getActiveBuffs().remove(b)) {
                removedBuffs.add(b);
            }
        }
    }

    public void setDefaults(Character character) {
    }

    public Stats getPetBuffsStats(Character character, CalculationOptionsHunter calcOpts) {
        if (calcOpts == null) return new Stats();
        Stats statsBuffs;
        try {
            List<Buff> removedBuffs = new ArrayList<Buff>();
            List<Buff> addedBuffs = new ArrayList<Buff>();

            List<Buff> buffGroup = new ArrayList<Buff>();

            // Passive Ability Auto-Fixing
            if (character.getHunterTalents().getTrueshotAura() > 0) {
                buffGroup.clear();
                buffGroup.add(Buff.getBuffByName("Trueshot Aura"));
                buffGroup.add(Buff.getBuffByName("Unleashed Rage"));
                buffGroup.add(Buff.getBuffByName("Abomination's Might"));
                maintPetBuffHelper(buffGroup, calcOpts, removedBuffs);
            }

            statsBuffs = getBuffsStats(calcOpts.getPetActiveBuffs());

            for (Buff b : removedBuffs) {
                calcOpts.getPetActiveBuffs().add(b);
            }
            for (Buff b : addedBuffs) {
                calcOpts.getPetActiveBuffs().remove(b);
            }
        } catch (Exception ex) {
            statsBuffs = new Stats();
        }

        return statsBuffs;
    }

    private void maintPetBuffHelper(List<Buff> buffGroup, CalculationOptionsHunter calcOpts, List<Buff> removedBuffs) {
        for (Buff b : buffGroup) {
            if (calcOpts.getPetActiveBuffs().remove(b)) {
                removedBuffs.add(b);
            }
        }
    }

    // Special Comparison Charts

    private String[] customChartNames = null;

    public String[] getCustomChartNames() {
        if (this.customChartNames == null) {
            this.customChartNames = new String[] {
                    "Pet Talents",
                    "Spammed Shots DPS",
                    "Spammed Shots MPS",
                    "Rotation DPS",
                    "Rotation MPS",
                    "Shot Damage per Mana",
                    "Item Budget",
            };
        }
        return this.customChartNames;
    }

    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        CharacterCalculationsHunter calculations = (CharacterCalculationsHunter) getCharacterCalculations(character);

        switch (chartName) {
            case "Pet Talents": {
                subPointNameColors = subPointNameColorsDps;
                return getPetTalentChart(character, calculations);
            }
            case "Pet Buffs": {
                subPointNameColors = subPointNameColorsDps;
                CalculationOptionsHunter calcOpts = (CalculationOptionsHunter) character.getCalculationOptions();
                List<ComparisonCalculationBase> buffCalcs = getPetBuffCalculations(character, calcOpts, calculations, "All");
                return buffCalcs.toArray(new ComparisonCalculationBase[0]);
            }
            case "Spammed Shots DPS": {
                subPointNameColors = subPointNameColorsDps;
                return new ComparisonCalculationBase[] {
                        comparisonFromShotSpammedDPS(calculations.aimedShot),
                        comparisonFromShotSpammedDPS(calculations.arcaneShot),
                        comparisonFromShotSpammedDPS(calculations.multiShot),
                        comparisonFromShotSpammedDPS(calculations.serpentSting),
                        comparisonFromShotSpammedDPS(calculations.scorpidSting),
                        comparisonFromShotSpammedDPS(calculations.viperSting),
                        comparisonFromShotSpammedDPS(calculations.silencingShot),
                        comparisonFromShotSpammedDPS(calculations.steadyShot),
                        comparisonFromShotSpammedDPS(calculations.killShot),
                        comparisonFromShotSpammedDPS(calculations.explosiveShot),
                        comparisonFromShotSpammedDPS(calculations.blackArrow),
                        comparisonFromShotSpammedDPS(calculations.immolationTrap),
                        comparisonFromShotSpammedDPS(calculations.explosiveTrap),
                        comparisonFromShotSpammedDPS(calculations.freezingTrap),
                        comparisonFromShotSpammedDPS(calculations.frostTrap),
                        comparisonFromShotSpammedDPS(calculations.volley),
                        comparisonFromShotSpammedDPS(calculations.chimeraShot),
                };
            }
            case "Spammed Shots MPS": {
                subPointNameColors = subPointNameColorsMps;
                return new ComparisonCalculationBase[] {
                        comparisonFromShotSpammedMPS(calculations.aimedShot),
                        comparisonFromShotSpammedMPS(calculations.arcaneShot),
                        comparisonFromShotSpammedMPS(calculations.multiShot),
                        comparisonFromShotSpammedMPS(calculations.serpentSting),
                        comparisonFromShotSpammedMPS(calculations.scorpidSting),
                        comparisonFromShotSpammedMPS(calculations.viperSting),
                        comparisonFromShotSpammedMPS(calculations.silencingShot),
                        comparisonFromShotSpammedMPS(calculations.steadyShot),
                        comparisonFromShotSpammedMPS(calculations.killShot),
                        comparisonFromShotSpammedMPS(calculations.explosiveShot),
                        comparisonFromShotSpammedMPS(calculations.blackArrow),
                        comparisonFromShotSpammedMPS(calculations.immolationTrap),
                        comparisonFromShotSpammedMPS(calculations.explosiveTrap),
                        comparisonFromShotSpammedMPS(calculations.freezingTrap),
                        comparisonFromShotSpammedMPS(calculations.frostTrap),
                        comparisonFromShotSpammedMPS(calculations.volley),
                        comparisonFromShotSpammedMPS(calculations.chimeraShot),
                        comparisonFromShotSpammedMPS(calculations.rapidFire),
                        comparisonFromShotSpammedMPS(calculations.readiness),
                        comparisonFromShotSpammedMPS(calculations.bestialWrath),
                };
            }
            case "Rotation DPS": {
                subPointNameColors = subPointNameColorsDps;
                return new ComparisonCalculationBase[] {
                        comparisonFromShotRotationDPS(calculations.aimedShot),
                        comparisonFromShotRotationDPS(calculations.arcaneShot),
                        comparisonFromShotRotationDPS(calculations.multiShot),
                        comparisonFromShotRotationDPS(calculations.serpentSting),
                        comparisonFromShotRotationDPS(calculations.scorpidSting),
                        comparisonFromShotRotationDPS(calculations.viperSting),
                        comparisonFromShotRotationDPS(calculations.silencingShot),
                        comparisonFromShotRotationDPS(calculations.steadyShot),
                        comparisonFromShotRotationDPS(calculations.killShot),
                        comparisonFromShotRotationDPS(calculations.explosiveShot),
                        comparisonFromShotRotationDPS(calculations.blackArrow),
                        comparisonFromShotRotationDPS(calculations.immolationTrap),
                        comparisonFromShotRotationDPS(calculations.explosiveTrap),
                        comparisonFromShotRotationDPS(calculations.freezingTrap),
                        comparisonFromShotRotationDPS(calculations.frostTrap),
                        comparisonFromShotRotationDPS(calculations.volley),
                        comparisonFromShotRotationDPS(calculations.chimeraShot),
                        comparisonFromDoubles("Autoshot", calculations.getAutoshotDPS(), 0),
                        comparisonFromDoubles("WildQuiver", calculations.getWildQuiverDPS(), 0),
                        comparisonFromDoubles("KillShotSub20", calculations.killShotSub20FinalGain, 0),
                        comparisonFromDoubles("AspectBeastLoss", calculations.aspectBeastLostDPS, 0),
                        comparisonFromDoubles("PetAutoAttack", 0, calculations.petWhiteDPS),
                        comparisonFromDoubles("PetSkills", 0, calculations.petSpecialDPS),
                        comparisonFromDoubles("KillCommand", 0, calculations.petKillCommandDPS),
                };
            }
            case "Rotation MPS": {
                subPointNameColors = subPointNameColorsMps;
                return new ComparisonCalculationBase[] {
                        comparisonFromShotRotationMPS(calculations.aimedShot),
                        comparisonFromShotRotationMPS(calculations.arcaneShot),
                        comparisonFromShotRotationMPS(calculations.multiShot),
                        comparisonFromShotRotationMPS(calculations.serpentSting),
                        comparisonFromShotRotationMPS(calculations.scorpidSting),
                        comparisonFromShotRotationMPS(calculations.viperSting),
                        comparisonFromShotRotationMPS(calculations.silencingShot),
                        comparisonFromShotRotationMPS(calculations.steadyShot),
                        comparisonFromShotRotationMPS(calculations.killShot),
                        comparisonFromShotRotationMPS(calculations.explosiveShot),
                        comparisonFromShotRotationMPS(calculations.blackArrow),
                        comparisonFromShotRotationMPS(calculations.immolationTrap),
                        comparisonFromShotRotationMPS(calculations.explosiveTrap),
                        comparisonFromShotRotationMPS(calculations.freezingTrap),
                        comparisonFromShotRotationMPS(calculations.frostTrap),
                        comparisonFromShotRotationMPS(calculations.volley),
                        comparisonFromShotRotationMPS(calculations.chimeraShot),
                        comparisonFromShotRotationMPS(calculations.rapidFire),
                        comparisonFromShotRotationMPS(calculations.readiness),
                        comparisonFromShotRotationMPS(calculations.bestialWrath),
                        comparisonFromDouble("KillCommand", calculations.petKillCommandMPS),
                };
            }
            case "Shot Damage per Mana": {
                subPointNameColors = subPointNameColorsDpm;
                return new ComparisonCalculationBase[] {
                        comparisonFromShotDPM(calculations.aimedShot),
                        comparisonFromShotDPM(calculations.arcaneShot),
                        comparisonFromShotDPM(calculations.multiShot),
                        comparisonFromShotDPM(calculations.serpentSting),
                        comparisonFromShotDPM(calculations.scorpidSting),
                        comparisonFromShotDPM(calculations.viperSting),
                        comparisonFromShotDPM(calculations.silencingShot),
                        comparisonFromShotDPM(calculations.steadyShot),
                        comparisonFromShotDPM(calculations.killShot),
                        comparisonFromShotDPM(calculations.explosiveShot),
                        comparisonFromShotDPM(calculations.blackArrow),
                        comparisonFromShotDPM(calculations.immolationTrap),
                        comparisonFromShotDPM(calculations.explosiveTrap),
                        comparisonFromShotDPM(calculations.freezingTrap),
                        comparisonFromShotDPM(calculations.frostTrap),
                        comparisonFromShotDPM(calculations.volley),
                        comparisonFromShotDPM(calculations.chimeraShot),
                };
            }
            case "Item Budget": {
                subPointNameColors = subPointNameColorsDps;
                Stats int10 = new Stats();
                int10.setIntellect(10f);
                Stats agi10 = new Stats();
                agi10.setAgility(10f);
                Stats mp5 = new Stats();
                mp5.setMp5(4f);
                Stats crit10 = new Stats();
                crit10.setCritRating(10f);
                Stats hit10 = new Stats();
                hit10.setHitRating(10f);
                Stats arp10 = new Stats();
                arp10.setArmorPenetrationRating(10f);
                Stats ap20 = new Stats();
                ap20.setAttackPower(20f);
                Stats rap25 = new Stats();
                rap25.setRangedAttackPower(25f);
                Stats haste10 = new Stats();
                haste10.setHasteRating(10f);
                return new ComparisonCalculationBase[] {
                        comparisonFromStat(character, calculations, int10, "10 Intellect"),
                        comparisonFromStat(character, calculations, agi10, "10 Agility"),
                        comparisonFromStat(character, calculations, mp5, "4 MP5"),
                        comparisonFromStat(character, calculations, crit10, "10 Crit Rating"),
                        comparisonFromStat(character, calculations, hit10, "10 Hit Rating"),
                        comparisonFromStat(character, calculations, arp10, "10 Armor Penetration Rating"),
                        comparisonFromStat(character, calculations, ap20, "20 Attack Power"),
                        comparisonFromStat(character, calculations, rap25, "25 Ranged Attack Power"),
                        comparisonFromStat(character, calculations, haste10, "10 Haste Rating"),
                };
            }
            default:
                return new ComparisonCalculationBase[0];
        }
    }

    public List<ComparisonCalculationBase> getPetBuffCalculations(Character character, CalculationOptionsHunter calcOpts, CharacterCalculationsBase currentCalcs, String filter) {
        List<ComparisonCalculationBase> buffCalcs = new ArrayList<ComparisonCalculationBase>();
        CharacterCalculationsBase calcsOpposite = null;
        Character charAutoActivated = character.clone();
        for (Buff autoBuff : currentCalcs.getAutoActivatedBuffs()) {
            if (!charAutoActivated.getActiveBuffs().contains(autoBuff)) {
                charAutoActivated.activeBuffsAdd(autoBuff);
                Calculations.removeConflictingBuffs(charAutoActivated.getActiveBuffs(), autoBuff);
            }
        }
        charAutoActivated.setDisableBuffAutoActivation(true);

        String[] multiFilter = filter.split("\\|");

        List<Buff> relevantBuffs = new ArrayList<Buff>();
        for (Buff buff : getRelevantPetBuffs()) {
            boolean isinMultiFilter = false;
            if (multiFilter.length > 0) {
                for (String mFilter : multiFilter) {
                    if (buff.group.equalsIgnoreCase(mFilter)) {
                        isinMultiFilter = true;
                        break;
                    }
                }
            }
            if (filter == null || filter.equals("All") || filter.equals("Current")
                    || buff.group.equalsIgnoreCase(filter)
                    || isinMultiFilter) {
                relevantBuffs.add(buff);
                relevantBuffs.addAll(buff.improvements);
            }
        }

        for (Buff buff : relevantBuffs) {
            if (!"Current".equalsIgnoreCase(filter) || ((CalculationOptionsHunter) charAutoActivated.getCalculationOptions()).getPetActiveBuffs().contains(buff)) {
                Character charOpposite = charAutoActivated.clone();
                CalculationOptionsHunter calcOptsOpposite = (CalculationOptionsHunter) charOpposite.getCalculationOptions();
                boolean which = ((CalculationOptionsHunter) charAutoActivated.getCalculationOptions()).getPetActiveBuffs().contains(buff);
                charOpposite.setDisableBuffAutoActivation(true);
                if (which) {
                    calcOptsOpposite.getPetActiveBuffs().remove(buff);
                } else {
                    calcOptsOpposite.getPetActiveBuffs().add(buff);
                }

                Calculations.removeConflictingBuffs(calcOptsOpposite.getPetActiveBuffs(), buff);

                calcsOpposite = getCharacterCalculations(charOpposite, null, false, false, false);

                ComparisonCalculationBase buffCalc = createNewComparisonCalculation();
                buffCalc.setName(buff.name);
                Item item = new Item();
                item.setName(buff.name);
                item.setStats(buff.stats);
                item.setQuality(ItemQuality.Temp);
                buffCalc.setItem(item);
                buffCalc.setEquipped(which);
                buffCalc.setOverallPoints((which ? currentCalcs.getOverallPoints() - calcsOpposite.getOverallPoints()
                        : calcsOpposite.getOverallPoints() - currentCalcs.getOverallPoints()));
                float[] subPoints = new float[calcsOpposite.getSubPoints().length];
                for (int i = 0; i < calcsOpposite.getSubPoints().length; i++) {
                    subPoints[i] = (which ? currentCalcs.getSubPoints()[i] - calcsOpposite.getSubPoints()[i]
                            : calcsOpposite.getSubPoints()[i] - currentCalcs.getSubPoints()[i]);
                }
                buffCalc.setSubPoints(subPoints);
                buffCalcs.add(buffCalc);
                // Revert, cuz it's evil
                if (!which) {
                    calcOptsOpposite.getPetActiveBuffs().remove(buff);
                } else {
                    calcOptsOpposite.getPetActiveBuffs().add(buff);
                }
            }
        }
        return buffCalcs;
    }

    private ComparisonCalculationHunter[] getPetTalentChart(Character character, CharacterCalculationsHunter calcs) {
        List<ComparisonCalculationHunter> talentCalculations = new ArrayList<ComparisonCalculationHunter>();
        Character baseChar = character.clone();
        CalculationOptionsHunter baseCalcOpts = (CalculationOptionsHunter) baseChar.getCalculationOptions();
        Character newChar = character.clone();
        CalculationOptionsHunter newCalcOpts = (CalculationOptionsHunter) newChar.getCalculationOptions();
        CharacterCalculationsHunter currentCalc;
        CharacterCalculationsHunter newCalc;
        ComparisonCalculationHunter compare;
        currentCalc = (CharacterCalculationsHunter) getCharacterCalculations(baseChar, null, false, true, false);

        for (PetTalent pi : baseCalcOpts.getPetTalents().getTalentTree()) {
            int index = pi.id;
            int orig = 0;
            PetTalent talentData = pi;
            orig = pi.value;
            if (talentData.max == pi.value) {
                newCalcOpts.getPetTalents().getTalentTree().get(index).value--;
                newCalc = (CharacterCalculationsHunter) getCharacterCalculations(newChar, null, false, true, false);
                compare = (ComparisonCalculationHunter) getCharacterComparisonCalculations(newCalc, currentCalc, talentData.name, talentData.max == orig, orig != 0 && orig != talentData.max);
            } else {
                newCalcOpts.getPetTalents().getTalentTree().get(index).value++;
                newCalc = (CharacterCalculationsHunter) getCharacterCalculations(newChar, null, false, true, false);
                compare = (ComparisonCalculationHunter) getCharacterComparisonCalculations(currentCalc, newCalc, talentData.name, talentData.max == orig, orig != 0 && orig != talentData.max);
            }
            String text = String.format("Current Rank %s/%s\r\n\r\n", orig, talentData.max);
            if (orig == 0) {
                // We originally didn't have it, so first rank is next rank
                text += "Next Rank:\r\n";
                text += talentData.desc[0];
            } else if (orig >= talentData.max) {
                // We originally were at max, so there isn't a next rank, just show the capped one
                text += talentData.desc[talentData.max];
            } else {
                // We aren't at 0 or MaxPoints originally, so it's just a point in between
                text += talentData.desc[orig];
                text += "\r\n\r\nNext Rank:\r\n";
                text += talentData.desc[orig + 1];
            }
            compare.setDescription(text);
            compare.setItem(null);
            talentCalculations.add(compare);
            newCalcOpts.getPetTalents().getTalentTree().get(index).value = orig;
        }
        return talentCalculations.toArray(new ComparisonCalculationHunter[0]);
    }

    private ComparisonCalculationHunter comparisonFromShotSpammedDPS(ShotData shot) {
        ComparisonCalculationHunter comp = new ComparisonCalculationHunter();

        float shotWait = shot.duration > shot.cd ? shot.duration : shot.cd;
        float dps = shotWait > 0 ? (float) (shot.damage / shotWait) : 0;

        comp.setName(shot.type.name());
        comp.setHunterDPSPoints(dps);
        comp.setOverallPoints(dps);
        return comp;
    }

    private ComparisonCalculationHunter comparisonFromShotSpammedMPS(ShotData shot) {
        ComparisonCalculationHunter comp = new ComparisonCalculationHunter();

        float shotWait = shot.duration > shot.cd ? shot.duration : shot.cd;
        float mps = shotWait > 0 ? (float) (shot.manaCost / shotWait) : 0;

        comp.setName(shot.type.name());
        comp.setSubPoints(new float[] { mps });
        comp.setOverallPoints(mps);
        return comp;
    }

    private ComparisonCalculationHunter comparisonFromShotRotationDPS(ShotData shot) {
        ComparisonCalculationHunter comp = new ComparisonCalculationHunter();
        comp.setName(shot.type.name());
        comp.setSubPoints(new float[] { (float) shot.dps });
        comp.setOverallPoints((float) shot.dps);
        return comp;
    }

    private ComparisonCalculationHunter comparisonFromShotRotationMPS(ShotData shot) {
        ComparisonCalculationHunter comp = new ComparisonCalculationHunter();
        comp.setName(shot.type.name());
        comp.setSubPoints(new float[] { (float) shot.mps });
        comp.setOverallPoints((float) shot.mps);
        return comp;
    }

    private ComparisonCalculationHunter comparisonFromShotDPM(ShotData shot) {
        ComparisonCalculationHunter comp = new ComparisonCalculationHunter();

        float dpm = shot.manaCost > 0 ? (float) (shot.damage / shot.manaCost) : 0;

        comp.setName(shot.type.name());
        comp.setSubPoints(new float[] { dpm });
        comp.setOverallPoints(dpm);
        return comp;
    }

    private ComparisonCalculationHunter comparisonFromStat(Character character, CharacterCalculationsHunter calcBase, Stats stats, String label) {
        ComparisonCalculationHunter comp = new ComparisonCalculationHunter();

        Item item = new Item();
        item.setStats(stats);
        CharacterCalculationsHunter calcStat = (CharacterCalculationsHunter) getCharacterCalculations(character, item);

        comp.setName(label);
        comp.setHunterDPSPoints(calcStat.getHunterDpsPoints() - calcBase.getHunterDpsPoints());
        comp.setPetDPSPoints(calcStat.getPetDpsPoints() - calcBase.getPetDpsPoints());
        comp.setOverallPoints(calcStat.getOverallPoints() - calcBase.getOverallPoints());

        return comp;
    }

    private ComparisonCalculationHunter comparisonFromDouble(String label, float value) {
        ComparisonCalculationHunter comp = new ComparisonCalculationHunter();
        comp.setName(label);
        comp.setSubPoints(new float[] { (float) value });
        comp.setOverallPoints((float) value);
        return comp;
    }

    private ComparisonCalculationHunter comparisonFromDoubles(String label, float value1, float value2) {
        ComparisonCalculationHunter comp = new ComparisonCalculationHunter();
        comp.setName(label);
        comp.setSubPoints(new float[] { (float) value1, (float) value2 });
        comp.setOverallPoints((float) (value1 + value2));
        return comp;
    }

    // CalculationsBase Overrides
    private void genPrioRotation(CharacterCalculationsHunter calculatedStats, CalculationOptionsHunter calcOpts, HunterTalents talents) {
        calculatedStats.priorityRotation = new ShotPriority(calcOpts);
        calculatedStats.priorityRotation.priorities[0] = getShotByIndex(calcOpts.priorityIndex1, calculatedStats);
        calculatedStats.priorityRotation.priorities[1] = getShotByIndex(calcOpts.priorityIndex2, calculatedStats);
        calculatedStats.priorityRotation.priorities[2] = getShotByIndex(calcOpts.priorityIndex3, calculatedStats);
        calculatedStats.priorityRotation.priorities[3] = getShotByIndex(calcOpts.priorityIndex4, calculatedStats);
        calculatedStats.priorityRotation.priorities[4] = getShotByIndex(calcOpts.priorityIndex5, calculatedStats);
        calculatedStats.priorityRotation.priorities[5] = getShotByIndex(calcOpts.priorityIndex6, calculatedStats);
        calculatedStats.priorityRotation.priorities[6] = getShotByIndex(calcOpts.priorityIndex7, calculatedStats);
        calculatedStats.priorityRotation.priorities[7] = getShotByIndex(calcOpts.priorityIndex8, calculatedStats);
        calculatedStats.priorityRotation.priorities[8] = getShotByIndex(calcOpts.priorityIndex9, calculatedStats);
        calculatedStats.priorityRotation.priorities[9] = getShotByIndex(calcOpts.priorityIndex10, calculatedStats);
        calculatedStats.priorityRotation.validateShots(talents);
    }

    private void genAbilityCds(Character character, CharacterCalculationsHunter calculatedStats, CalculationOptionsHunter calcOpts, BossOptions bossOpts, HunterTalents talents) {
        calculatedStats.serpentSting.cd = 1.5f;
        calculatedStats.serpentSting.duration = talents.isGlyphOfSerpentSting() ? 21 : 15;

        calculatedStats.aimedShot.cd = talents.isGlyphOfAimedShot() ? 8 : 10;

        calculatedStats.explosiveShot.cd = 6;

        calculatedStats.chimeraShot.cd = talents.isGlyphOfChimeraShot() ? 9 : 10;

        calculatedStats.arcaneShot.cd = 6;

        calculatedStats.multiShot.cd = talents.isGlyphOfMultiShot() ? 9 : 10;

        calculatedStats.blackArrow.cd = 30 - (talents.getResourcefulness() * 2);
        calculatedStats.blackArrow.duration = 15;

        calculatedStats.killShot.cd = talents.isGlyphOfKillShot() ? 9 : 15;

        calculatedStats.silencingShot.cd = 20;

        calculatedStats.scorpidSting.cd = 20;
        calculatedStats.scorpidSting.duration = 15;

        calculatedStats.viperSting.cd = 15;
        calculatedStats.viperSting.duration = 8;

        calculatedStats.immolationTrap.cd = 30 - talents.getResourcefulness() * 2;
        calculatedStats.immolationTrap.duration = talents.isGlyphOfImmolationTrap() ? 9 : 15;

        calculatedStats.explosiveTrap.cd = 30 - talents.getResourcefulness() * 2;
        calculatedStats.explosiveTrap.duration = 20;

        calculatedStats.freezingTrap.cd = 30 - talents.getResourcefulness() * 2;
        calculatedStats.freezingTrap.duration = 20;

        calculatedStats.frostTrap.cd = 30 - talents.getResourcefulness() * 2;
        calculatedStats.frostTrap.duration = 30;

        if (calcOpts.multipleTargets && calcOpts.multipleTargetsPerc > 0) {
            // Good to go, now change the cooldown based on the multitargs uptime
            calculatedStats.volley.duration = 6f;
            calculatedStats.volley.cd = (1f / ((calcOpts.multipleTargetsPerc * calcOpts.duration) / calculatedStats.volley.duration)) * calcOpts.duration;
        } else {
            // invalidate it
            calculatedStats.volley.cd = -1f;
            calculatedStats.volley.duration = -1f;
        }

        if (calculatedStats.priorityRotation.containsShot(Shots.Readiness)) {
            calculatedStats.rapidFire.cd = 157.5f - (30f * talents.getRapidKilling());
        } else {
            calculatedStats.rapidFire.cd = (5 - talents.getRapidKilling()) * 60f;
        }
        calculatedStats.rapidFire.duration = 15;

        // We will set the correct value for this later, after we've calculated haste
        calculatedStats.steadyShot.cd = 2;

        calculatedStats.readiness.cd = 180;

        calculatedStats.bestialWrath.cd = (talents.isGlyphOfBestialWrath() ? 100f : 120f) * (1f - talents.getLongevity() * 0.10f);
        calculatedStats.bestialWrath.duration = calcOpts.petFamily == PetFamily.None ? 0 : 10;

        // We can calculate the rough frequencies now
        calculatedStats.priorityRotation.initializeTimings();
        if (!calcOpts.useRotationTest) {
            calculatedStats.priorityRotation.calculateFrequencies();
            calculatedStats.priorityRotation.calculateLALProcs(character);
            calculatedStats.priorityRotation.calculateFrequencies();
            calculatedStats.priorityRotation.calculateFrequencySums();
        }
    }

    private static class GenRotationResult {
        float rangedWeaponSpeed;
        float rangedAmmoDPS;
        float rangedWeaponDamage;
        float autoShotSpeed;
        float autoShotsPerSecond;
        float specialShotsPerSecond;
        float totalShotsPerSecond;
        float shotsPerSecondWithoutHawk;
        RotationTest rotationTest;
    }

    private static GenRotationResult genRotation(Character character, Stats stats, CharacterCalculationsHunter calculatedStats,
            CalculationOptionsHunter calcOpts, BossOptions bossOpts, HunterTalents talents) {
        GenRotationResult result = new GenRotationResult();
        // Ranged Weapon Stats
        result.rangedWeaponDamage = 0;
        result.rangedWeaponSpeed = 0;
        result.rangedAmmoDPS = 0;

        if (character.getRanged() != null) {
            result.rangedWeaponDamage = (float) (character.getRanged().getItem().getMinDamage() + character.getRanged().getItem().getMaxDamage()) / 2f;
            result.rangedWeaponSpeed = (float) Math.rint(character.getRanged().getItem().getSpeed() * 10f) / 10f;
        }
        if (character.get(CharacterSlot.Projectile) != null) {
            result.rangedAmmoDPS = (float) (character.get(CharacterSlot.Projectile).getItem().getMaxDamage() + character.get(CharacterSlot.Projectile).getItem().getMinDamage()) / 2f;
        }

        // Static Haste Calcs
        // default quiver speed
        calculatedStats.hasteFromBase = 0.15f;
        // haste from haste rating
        calculatedStats.hasteFromRating = StatConversion.getHasteFromRating(stats.getHasteRating(), character.getCharacterClass());
        // serpent swiftness
        calculatedStats.hasteFromTalentsStatic = 0.04f * talents.getSerpentsSwiftness();
        // haste buffs
        calculatedStats.hasteFromRangedBuffs = stats.getRangedHaste();
        // total hastes
        calculatedStats.hasteStaticTotal = stats.getPhysicalHaste();
        // Needed by the rotation test
        calculatedStats.autoShotStaticSpeed = result.rangedWeaponSpeed / (1f + calculatedStats.hasteStaticTotal);

        // Rotation Test
        // Using the rotation test will get us better frequencies
        result.rotationTest = new RotationTest(character, calculatedStats, calcOpts, bossOpts);

        if (calcOpts.useRotationTest) {
            // The following properties of CalculatedStats must be ready by this call:
            //  * priorityRotation (shot order, durations, cooldowns)
            //  * quickShotsEffect
            //  * hasteStaticTotal
            //  * autoShotStaticSpeed

            result.rotationTest.runTest();
        }

        // Dynamic Haste Calcs
        // Now we have the haste, we can calculate steady shot cast time,
        // then rebuild other various stats.
        // (future enhancement: we only really need to rebuild steady shot)
        calculatedStats.steadyShot.cd = 2f / (1f + calculatedStats.hasteStaticTotal);
        if (calcOpts.useRotationTest) {
            calculatedStats.priorityRotation.initializeTimings();
            calculatedStats.priorityRotation.recalculateRatios();
            calculatedStats.priorityRotation.calculateFrequencySums();
        } else {
            calculatedStats.priorityRotation.initializeTimings();
            calculatedStats.priorityRotation.calculateFrequencies();
            calculatedStats.priorityRotation.calculateFrequencySums();
        }
        result.autoShotSpeed = result.rangedWeaponSpeed / (1f + calculatedStats.hasteStaticTotal);

        // Shots Per Second
        float baseAutoShotsPerSecond = result.autoShotSpeed > 0 ? 1f / result.autoShotSpeed : 0;
        result.autoShotsPerSecond = baseAutoShotsPerSecond;
        result.specialShotsPerSecond = calculatedStats.priorityRotation.specialShotsPerSecond;
        result.totalShotsPerSecond = result.autoShotsPerSecond + result.specialShotsPerSecond;

        float crittingSpecialsPerSecond = calculatedStats.priorityRotation.critSpecialShotsPerSecond;
        float crittingShotsPerSecond = result.autoShotsPerSecond + crittingSpecialsPerSecond;

        result.shotsPerSecondWithoutHawk = result.specialShotsPerSecond + baseAutoShotsPerSecond;

        calculatedStats.setBaseAttackSpeed((float) result.autoShotSpeed);
        calculatedStats.shotsPerSecondCritting = crittingShotsPerSecond;
        return result;
    }

    public float constrainCrit(float lvlDifMOD, float chance) {
        return Math.min(1f + lvlDifMOD, Math.max(0f, chance));
    }

    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem,
            boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        CharacterCalculationsHunter calculatedStats = new CharacterCalculationsHunter();
        if (character == null) {
            return calculatedStats;
        }
        if (referenceCalculation && significantChange) {
            // Fresh top-level character calculation: drop shared effect interpolation caches so a
            // previous character cannot leak into this one (DECISIONS #28).
            rawr.base.effects.SpecialEffect.clearInterpolatorCaches();
        }
        calculatedStats.character = character;
        CalculationOptionsHunter calcOpts = (CalculationOptionsHunter) character.getCalculationOptions();
        calculatedStats.calcOpts = calcOpts;
        BossOptions bossOpts = character.getBossOptions();
        calculatedStats.bossOpts = bossOpts;
        Stats stats = getCharacterStats(character, additionalItem);
        HunterTalents talents = character.getHunterTalents();
        CombatFactors combatFactors = new CombatFactors(character, stats, calcOpts, bossOpts);
        // The prebuilt DLL constructs a Skills.WhiteAttacks here and immediately pops it (IL newobj +
        // pop); the result is never read. Kept for control-flow parity with the oracle (DECISIONS #59).
        new WhiteAttacks(character, stats, combatFactors, calcOpts, bossOpts);

        Stats statsItems = new Stats();
        accumulateItemStats(statsItems, character, additionalItem);
        Stats statsBuffs = getBuffsStats(character, calcOpts);
        Stats statsRace = BaseStats.getBaseStats(character.getLevel(), CharacterClass.Hunter, character.getRace());

        calculatedStats.setBasicStats(stats);
        calculatedStats.baseHealth = statsRace.getHealth();

        calculatedStats.pet = new PetCalculations(character, calculatedStats, calcOpts, bossOpts, stats, getPetBuffsStats(character, calcOpts));

        if (character.getRanged() == null || (character.getRanged().getItem().getType() != ItemType.Bow
                && character.getRanged().getItem().getType() != ItemType.Gun
                && character.getRanged().getItem().getType() != ItemType.Crossbow)) {
            // skip all the calculations if there is no ranged weapon
            return calculatedStats;
        }
        int levelDifI = calcOpts.targetLevel - character.getLevel();
        float levelDifF = (float) levelDifI;

        float critMOD = StatConversion.NPC_LEVEL_CRIT_MOD[levelDifI];

        genPrioRotation(calculatedStats, calcOpts, talents);
        genAbilityCds(character, calculatedStats, calcOpts, bossOpts, talents);

        GenRotationResult rotation = genRotation(character, stats, calculatedStats, calcOpts, bossOpts, talents);
        float rangedWeaponSpeed = rotation.rangedWeaponSpeed;
        float rangedAmmoDPS = rotation.rangedAmmoDPS;
        float rangedWeaponDamage = rotation.rangedWeaponDamage;
        float autoShotSpeed = rotation.autoShotSpeed;
        float autoShotsPerSecond = rotation.autoShotsPerSecond;
        float specialShotsPerSecond = rotation.specialShotsPerSecond;
        float totalShotsPerSecond = rotation.totalShotsPerSecond;
        float shotsPerSecondWithoutHawk = rotation.shotsPerSecondWithoutHawk;
        RotationTest rotationTest = rotation.rotationTest;

        // Hits
        // Hit vs Miss Chance
        float ChanceToMiss = (float) Math.max(0f, StatConversion.WHITE_MISS_CHANCE_CAP[levelDifI] - stats.getPhysicalHit());

        // Crits
        // Crit Chance
        float totalAGI = (float) Math.ceil(stats.getAgility());
        float baseAGI = (float) Math.ceil(statsRace.getAgility() * (1f + stats.getBonusAgilityMultiplier()));
        calculatedStats.critFromAgi = constrainCrit(critMOD, StatConversion.getCritFromAgility(stats.getAgility(), character.getCharacterClass()) - 0.01536f);
        calculatedStats.critFromRating = constrainCrit(critMOD, StatConversion.getCritFromRating(stats.getCritRating(), character.getCharacterClass()));

        calculatedStats.critRateOverall = stats.getPhysicalCrit();

        // Bonus Crit Chance
        //Improved Barrage
        float improvedBarrageCritModifier = 0.04f * talents.getImprovedBarrage();
        // Survival instincts
        float survivalInstinctsCritModifier = 0.02f * talents.getSurvivalInstincts();
        // Explosive Shot Glyph
        float glyphOfExplosiveShotCritModifier = talents.isGlyphOfExplosiveShot() ? 0.04f : 0;
        // Sniper Training
        float sniperTrainingCritModifier = 0.05f * talents.getSniperTraining();
        // Shot Crit Chances
        calculatedStats.steadyShot.setCritChance(constrainCrit(critMOD, stats.getPhysicalCrit() + survivalInstinctsCritModifier));
        calculatedStats.aimedShot.setCritChance(constrainCrit(critMOD, stats.getPhysicalCrit()
                + (talents.isGlyphOfTrueshotAura() && talents.getTrueshotAura() > 0 ? 0.10f : 0f)
                + improvedBarrageCritModifier));
        calculatedStats.explosiveShot.setCritChance(constrainCrit(critMOD, stats.getPhysicalCrit() + glyphOfExplosiveShotCritModifier + survivalInstinctsCritModifier));
        calculatedStats.serpentSting.setCritChance(constrainCrit(critMOD, stats.getPhysicalCrit()));
        calculatedStats.chimeraShot.setCritChance(constrainCrit(critMOD, stats.getPhysicalCrit()));
        calculatedStats.arcaneShot.setCritChance(constrainCrit(critMOD, stats.getPhysicalCrit() + survivalInstinctsCritModifier));
        calculatedStats.multiShot.setCritChance(constrainCrit(critMOD, stats.getPhysicalCrit() + improvedBarrageCritModifier));
        calculatedStats.killShot.setCritChance(constrainCrit(critMOD, stats.getPhysicalCrit() + sniperTrainingCritModifier));
        calculatedStats.silencingShot.setCritChance(constrainCrit(critMOD, stats.getPhysicalCrit()));
        calculatedStats.priorityRotation.calculateCrits();

        // pet - part 1
        // Pet MPS/Timing Calculations
        // this first block needs to run before the mana adjustments code,
        // since kill command effects mana usage.
        float baseMana = statsRace.getMana();
        calculatedStats.baseMana = statsRace.getMana();
        calculatedStats.pet.genPetStats();

        // target debuffs
        // Target Debuffs
        // The pet debuffs deal with stacking correctly themselves
        float targetDebuffsArmor = 1f - (1f - calculatedStats.petArmorDebuffs)
                * (1f - statsBuffs.getArmorPenetration()); // Buffs!G77

        float targetDebuffsMP5JudgmentOfWisdom = 0;
        if (stats.getManaRestoreFromBaseManaPPM() > 0) {
            // Note: we ignore the value stored in Buff.cs and calculate it as the spreadsheet
            // does, using shots per second and a derived 50% proc chance.
            float jowAvgShotTime = autoShotsPerSecond + specialShotsPerSecond > 0f ? 1f / (autoShotsPerSecond + specialShotsPerSecond) : 0f;
            float jowProcChance = 0.5f;
            float jowTimeToProc = jowProcChance > 0 ? 0.25f + jowAvgShotTime / jowProcChance : 0f;
            float jowManaGained = statsRace.getMana() * 0.02f;
            float jowMPSGained = jowTimeToProc > 0f ? jowManaGained / jowTimeToProc : 0f;
            targetDebuffsMP5JudgmentOfWisdom = jowTimeToProc > 0f ? jowManaGained / jowTimeToProc * 5f : 0f;
        }
        float targetDebuffsMP5 = targetDebuffsMP5JudgmentOfWisdom; // Buffs!H77

        float targetDebuffsFire = statsBuffs.getBonusFireDamageMultiplier();       // Buffs!I77
        float targetDebuffsArcane = statsBuffs.getBonusArcaneDamageMultiplier();   // Buffs!J77
        float targetDebuffsNature = statsBuffs.getBonusNatureDamageMultiplier();   // Buffs!K77
        float targetDebuffsShadow = statsBuffs.getBonusShadowDamageMultiplier();

        float targetDebuffsPetDamage = statsBuffs.getBonusPhysicalDamageMultiplier();

        calculatedStats.targetDebuffsArmor = 1f - targetDebuffsArmor;
        calculatedStats.targetDebuffsNature = 1f + targetDebuffsNature;
        calculatedStats.targetDebuffsPetDamage = 1f + targetDebuffsPetDamage;

        //29-10-2009 Drizz: For PiercingShots
        float targetDebuffBleed = statsBuffs.getBonusBleedDamageMultiplier();

        // Mana Consumption
        // Mana Adjustments
        float efficiencyManaAdjust = 1f - (talents.getEfficiency() * 0.03f);
        float thrillOfTheHuntManaAdjust = 1f - (calculatedStats.priorityRotation.critsCompositeSum * 0.40f * (talents.getThrillOfTheHunt() / 3f));
        float masterMarksmanManaAdjust = 1f - (talents.getMasterMarksman() * 0.05f);
        float glyphOfArcaneShotManaAdjust = 1f;
        if (calculatedStats.priorityRotation.containsShot(Shots.SerpentSting)
                || calculatedStats.priorityRotation.containsShot(Shots.ScorpidSting)) {
            glyphOfArcaneShotManaAdjust = talents.isGlyphOfArcaneShot() ? 0.8f : 1f;
        }

        // Improved Steady Shot
        float resourcefullnessManaAdjust = 1f - talents.getResourcefulness() * 0.2f;

        float ISSAimedShotManaAdjust = 1f;
        float ISSArcaneShotManaAdjust = 1f;
        float ISSChimeraShotManaAdjust = 1f;

        float ISSChimeraShotDamageAdjust = 1f;
        float ISSArcaneShotDamageAdjust = 1f;
        float ISSAimedShotDamageAdjust = 1f;

        float ISSProcChance = 0.05f * talents.getImprovedSteadyShot();
        if (ISSProcChance > 0f) {
            if (calcOpts.useRotationTest) {
                ISSChimeraShotDamageAdjust = 1f + rotationTest.issChimeraUptime * 0.15f;
                ISSArcaneShotDamageAdjust = 1f + rotationTest.issArcaneUptime * 0.15f;
                ISSAimedShotDamageAdjust = 1f + rotationTest.issAimedUptime * 0.15f;

                ISSChimeraShotManaAdjust = 1f - rotationTest.issChimeraUptime * 0.2f;
                ISSArcaneShotManaAdjust = 1f - rotationTest.issArcaneUptime * 0.2f;
                ISSAimedShotManaAdjust = 1f - rotationTest.issAimedUptime * 0.2f;
            } else {
                float ISSRealProcChance = 0f; // N120
                if (calculatedStats.steadyShot.freq > 0f) {
                    float ISSSteadyFreq = calculatedStats.steadyShot.freq;
                    float ISSOtherFreq = calculatedStats.arcaneShot.freq
                            + calculatedStats.chimeraShot.freq
                            + calculatedStats.aimedShot.freq;

                    ISSRealProcChance = 1f - (float) Math.pow(1f - ISSProcChance, ISSOtherFreq / ISSSteadyFreq);
                }
                float ISSProcFreqChimera = ISSRealProcChance > 0f ? calculatedStats.chimeraShot.freq / ISSRealProcChance : 0f; // N121
                float ISSProcFreqArcane = ISSRealProcChance > 0f ? calculatedStats.arcaneShot.freq / ISSRealProcChance : 0f; // N122
                float ISSProcFreqAimed = ISSRealProcChance > 0f ? calculatedStats.aimedShot.freq / ISSRealProcChance : 0f; // N123

                float ISSProcFreqSumInverse = (ISSProcFreqChimera > 0f ? 1f / ISSProcFreqChimera : 0f)
                        + (ISSProcFreqArcane > 0f ? 1f / ISSProcFreqArcane : 0f)
                        + (ISSProcFreqAimed > 0f ? 1f / ISSProcFreqAimed : 0f);
                float ISSProcFreqCombined = ISSProcFreqSumInverse > 0f ? 1f / ISSProcFreqSumInverse : 0f; // N124

                ISSChimeraShotDamageAdjust = ISSProcFreqChimera > 0f ? 1f + ISSRealProcChance * ISSProcFreqCombined / ISSProcFreqChimera * 0.15f : 1f;
                ISSArcaneShotDamageAdjust = ISSProcFreqArcane > 0f ? 1f + ISSRealProcChance * ISSProcFreqCombined / ISSProcFreqArcane * 0.15f : 1f;
                ISSAimedShotDamageAdjust = ISSProcFreqAimed > 0f ? 1f + ISSRealProcChance * ISSProcFreqCombined / ISSProcFreqAimed * 0.15f : 1f;

                ISSChimeraShotManaAdjust = ISSProcFreqChimera > 0f ? 1f - ISSRealProcChance * ISSProcFreqCombined / ISSProcFreqChimera * 0.2f : 1f;
                ISSArcaneShotManaAdjust = ISSProcFreqArcane > 0f ? 1f - ISSRealProcChance * ISSProcFreqCombined / ISSProcFreqArcane * 0.2f : 1f;
                ISSAimedShotManaAdjust = ISSProcFreqAimed > 0f ? 1f - ISSRealProcChance * ISSProcFreqCombined / ISSProcFreqAimed * 0.2f : 1f;
            }
        }

        float resourcefulnessManaAdjust = 1f - (talents.getResourcefulness() * 0.2f);

        // Shot Mana Usage

        // we do this ASAP so that we can get the MPS.
        // this allows us to calculate viper/aspect bonuses & penalties

        calculatedStats.steadyShot.manaCost = (baseMana * 0.05f) * efficiencyManaAdjust * thrillOfTheHuntManaAdjust * masterMarksmanManaAdjust;
        calculatedStats.serpentSting.manaCost = (baseMana * 0.09f) * efficiencyManaAdjust;
        calculatedStats.aimedShot.manaCost = (baseMana * 0.08f) * efficiencyManaAdjust * thrillOfTheHuntManaAdjust * masterMarksmanManaAdjust * ISSAimedShotManaAdjust;
        calculatedStats.explosiveShot.manaCost = (baseMana * 0.07f) * efficiencyManaAdjust * thrillOfTheHuntManaAdjust;
        calculatedStats.chimeraShot.manaCost = (baseMana * 0.12f) * efficiencyManaAdjust * thrillOfTheHuntManaAdjust * masterMarksmanManaAdjust * ISSChimeraShotManaAdjust;
        calculatedStats.arcaneShot.manaCost = (baseMana * 0.05f) * efficiencyManaAdjust * thrillOfTheHuntManaAdjust * ISSArcaneShotManaAdjust * glyphOfArcaneShotManaAdjust;
        calculatedStats.multiShot.manaCost = (baseMana * 0.09f) * efficiencyManaAdjust * thrillOfTheHuntManaAdjust;
        calculatedStats.blackArrow.manaCost = (baseMana * 0.06f) * efficiencyManaAdjust * resourcefulnessManaAdjust;
        calculatedStats.killShot.manaCost = (baseMana * 0.07f) * efficiencyManaAdjust * thrillOfTheHuntManaAdjust;
        calculatedStats.silencingShot.manaCost = (baseMana * 0.06f) * efficiencyManaAdjust * thrillOfTheHuntManaAdjust;
        calculatedStats.scorpidSting.manaCost = (baseMana * 0.11f) * efficiencyManaAdjust;
        calculatedStats.viperSting.manaCost = (baseMana * 0.08f) * efficiencyManaAdjust;
        calculatedStats.immolationTrap.manaCost = (baseMana * 0.13f) * resourcefullnessManaAdjust;
        calculatedStats.explosiveTrap.manaCost = (baseMana * 0.19f) * resourcefullnessManaAdjust;
        calculatedStats.freezingTrap.manaCost = (baseMana * 0.03f) * resourcefullnessManaAdjust;
        calculatedStats.frostTrap.manaCost = (baseMana * 0.02f) * resourcefullnessManaAdjust;
        calculatedStats.volley.manaCost = (baseMana * 0.17f) * (1f - (talents.isGlyphOfVolley() ? 0.20f : 0.00f));
        calculatedStats.rapidFire.manaCost = (baseMana * 0.03f);

        calculatedStats.priorityRotation.calculateRotationMPS();

        // Mana Regen
        // Mp5
        calculatedStats.manaRegenGearBuffs = stats.getMp5() / 5f; // Convert to per sec

        // Viper Regen if viper is up 100%
        calculatedStats.manaRegenConstantViper = 0;
        if (calcOpts.selectedAspect == Aspect.Viper) {
            float viperGlyphAdjust = talents.isGlyphOfAspectOfTheViper() ? 1.1f : 1;
            float viperRegenShots = calculatedStats.getBasicStats().getMana() * rangedWeaponSpeed / 100f * totalShotsPerSecond * viperGlyphAdjust;
            float viperRegenPassive = calculatedStats.getBasicStats().getMana() * 0.04f / 3f;
            calculatedStats.manaRegenConstantViper = viperRegenShots + viperRegenPassive;
        }

        // Rapid Recuperation
        // You gain (2*Pts)% of your mana every 3 sec while under the effect
        // of Rapid Fire, and you gain 2% of your mana every 2 sec for
        // 6 sec when you gain Rapid Killing.
        calculatedStats.manaRegenRapidRecuperation = 0;
        if (calculatedStats.rapidFire.freq > 0) {
            float rapidRecuperationManaGain = (((0.02f * talents.getRapidRecuperation()) * calculatedStats.getBasicStats().getMana()) / 3f) * 15f;
            calculatedStats.manaRegenRapidRecuperation = rapidRecuperationManaGain / calculatedStats.rapidFire.freq;
        }

        // Chimera shot refreshing Viper
        calculatedStats.manaRegenChimeraViperProc = 0;
        if (calculatedStats.priorityRotation.chimeraRefreshesViper) {
            if (calculatedStats.chimeraShot.freq > 0) {
                //29-10-2009 Drizz: Comment, 3092 is fetched from the Viper Sting Table on the SpellValues sheet (v92b). The 0.6 comes from ChimeraShotEffect.
                calculatedStats.manaRegenChimeraViperProc = 0.6f * 3092f / calculatedStats.chimeraShot.freq;
            }
        }

        // Hunting Party
        float huntingPartyProc = (float) talents.getHuntingParty() / 3.0f;

        float huntingPartyArcaneFreq = calculatedStats.arcaneShot.freq;
        float huntingPartyArcaneCrit = calculatedStats.arcaneShot.getCritChance();
        float huntingPartyArcaneUptime = huntingPartyArcaneFreq > 0 ? 1f - (float) Math.pow(1f - huntingPartyArcaneCrit * huntingPartyProc, 15f / huntingPartyArcaneFreq) : 0;

        float huntingPartyExplosiveFreq = calculatedStats.explosiveShot.freq; // spreadsheet divides by 3, but doesn't use that value?
        float huntingPartyExplosiveCrit = calculatedStats.explosiveShot.getCritChance();
        float huntingPartyExplosiveUptime = huntingPartyExplosiveFreq > 0 ? 1f - (float) Math.pow(1f - huntingPartyExplosiveCrit * huntingPartyProc, 15f / huntingPartyExplosiveFreq) : 0;

        float huntingPartySteadyFreq = calculatedStats.steadyShot.freq;
        float huntingPartySteadyCrit = calculatedStats.steadyShot.getCritChance();
        float huntingPartySteadyUptime = huntingPartySteadyFreq > 0 ? 1f - (float) Math.pow(1f - huntingPartySteadyCrit * huntingPartyProc, 15f / huntingPartySteadyFreq) : 0;

        float huntingPartyCumulativeUptime = huntingPartyArcaneUptime + ((1f - huntingPartyArcaneUptime) * huntingPartyExplosiveUptime);
        float huntingPartyUptime = huntingPartyCumulativeUptime + ((1f - huntingPartyCumulativeUptime) * huntingPartySteadyUptime);

        calculatedStats.manaRegenHuntingParty = 0.002f * calculatedStats.getBasicStats().getMana() * huntingPartyUptime;

        // If we've got a replenishment buff up, use that instead of our own Hunting Party
        float manaRegenReplenishment = stats.getManaRestoreFromMaxManaPerSecond() * calculatedStats.getBasicStats().getMana();
        if (manaRegenReplenishment > 0) {
            calculatedStats.manaRegenHuntingParty = manaRegenReplenishment;
        }

        calculatedStats.manaRegenFromPots = stats.getManaRestore() / (float) calcOpts.duration;

        // Target Debuffs
        calculatedStats.manaRegenTargetDebuffs = targetDebuffsMP5 / 5f;

        // Total
        calculatedStats.manaRegenTotal =
                calculatedStats.manaRegenGearBuffs +
                        calculatedStats.manaRegenConstantViper +
                        calculatedStats.manaRegenRoarOfRecovery +
                        calculatedStats.manaRegenRapidRecuperation +
                        calculatedStats.manaRegenChimeraViperProc +
                        calculatedStats.manaRegenInvigoration +
                        calculatedStats.manaRegenHuntingParty +
                        calculatedStats.manaRegenTargetDebuffs +
                        calculatedStats.manaRegenFromPots;

        // Aspect Usage
        float manaRegenTier7ViperBonus = stats.getBonusHunter_T7_4P_ViperSpeed() > 0 ? 1.2f : 1f;
        float glpyhOfAspectOfTheViperBonus = talents.isGlyphOfAspectOfTheViper() ? 1.1f : 1f;

        calculatedStats.manaRegenViper = calculatedStats.getBasicStats().getMana() * (float) HunterFormat.roundHalfEven(rangedWeaponSpeed, 1) / 100f * shotsPerSecondWithoutHawk
                * manaRegenTier7ViperBonus * glpyhOfAspectOfTheViperBonus
                + stats.getMana() * 0.04f / 3f;

        calculatedStats.manaUsageKillCommand = calculatedStats.petKillCommandMPS * (stats.getManaCostPerc());
        calculatedStats.manaUsageRotation = calculatedStats.priorityRotation.mps;

        calculatedStats.manaUsageTotal = calculatedStats.manaUsageRotation
                + calculatedStats.manaUsageKillCommand;

        calculatedStats.manaChangeDuringViper = calculatedStats.manaRegenTotal - calculatedStats.manaUsageTotal + calculatedStats.manaRegenViper;
        calculatedStats.manaChangeDuringNormal = calculatedStats.manaRegenTotal - calculatedStats.manaUsageTotal;

        calculatedStats.manaTimeToFull = calculatedStats.manaChangeDuringViper > 0f ? stats.getMana() / calculatedStats.manaChangeDuringViper : -1f;
        calculatedStats.manaTimeToOOM = calculatedStats.manaChangeDuringNormal < 0f ? stats.getMana() / (0f - calculatedStats.manaChangeDuringNormal) : -1f;

        float PercTimeNoDPSforNoMana = 0f,
                viperTimeNeededToLastFight = 0f,
                aspectUptimeHawk = 0f,
                aspectUptimeViper = 0f;

        if (calculatedStats.manaTimeToOOM >= 0f && calculatedStats.manaTimeToOOM < calcOpts.duration && calculatedStats.manaRegenViper > 0f) {
            viperTimeNeededToLastFight = (((0f - calculatedStats.manaChangeDuringNormal) * calcOpts.duration) - calculatedStats.getBasicStats().getMana()) / calculatedStats.manaRegenViper;
        }

        if (calculatedStats.manaTimeToOOM >= 0 && calculatedStats.manaTimeToOOM < calcOpts.duration) {
            if (calcOpts.aspectUsage == AspectUsage.ViperRegen) {
                aspectUptimeViper = Math.min(calcOpts.duration - calculatedStats.manaTimeToOOM, calculatedStats.manaTimeToFull) / (calculatedStats.manaTimeToFull + calculatedStats.manaTimeToOOM);
            } else if (calcOpts.aspectUsage == AspectUsage.ViperToOOM && viperTimeNeededToLastFight > 0f) {
                aspectUptimeViper = Math.min(calcOpts.duration - calculatedStats.manaTimeToOOM, viperTimeNeededToLastFight) / calcOpts.duration;
            } else if (calcOpts.aspectUsage == AspectUsage.None) {
                PercTimeNoDPSforNoMana = (calcOpts.duration - calculatedStats.manaTimeToOOM) / calcOpts.duration;
            }
        }

        float aspectUptimeBeast = calcOpts.useBeastDuringBestialWrath && calculatedStats.bestialWrath.freq > 0
                ? (calculatedStats.bestialWrath.duration * (calcOpts.duration / calculatedStats.bestialWrath.cd)) / calcOpts.duration : 0;

        switch (calcOpts.selectedAspect) {
            case Viper:
                aspectUptimeViper = calcOpts.useBeastDuringBestialWrath ? 1f - aspectUptimeBeast : 1f;
                break;
            case Beast:
                aspectUptimeBeast = (calcOpts.aspectUsage == AspectUsage.None) ? 1f : 1f - aspectUptimeViper;
                break;
            case Hawk:
            case Dragonhawk:
                aspectUptimeHawk = 1f - aspectUptimeViper - aspectUptimeBeast;
                break;
            default:
                break;
        }

        // we now know aspect uptimes - calculate bonuses and penalties
        float viperDamageEffect = talents.getAspectMastery() > 0 ? 0.40f : 0.50f;
        float viperDamagePenalty = aspectUptimeViper * viperDamageEffect;

        float beastStaticAPBonus = talents.isGlyphOfTheBeast() ? 0.12f : 0.10f;
        float beastAPBonus = aspectUptimeBeast * beastStaticAPBonus;

        float tier7ViperDamageAdjust = 1.0f + stats.getBonusHunter_T7_4P_ViperSpeed() * aspectUptimeViper;

        calculatedStats.aspectUptimeHawk = aspectUptimeHawk;
        calculatedStats.aspectUptimeBeast = aspectUptimeBeast;
        calculatedStats.aspectUptimeViper = aspectUptimeViper;
        calculatedStats.aspectViperPenalty = viperDamagePenalty;
        calculatedStats.aspectBonusAPBeast = beastAPBonus;
        calculatedStats.noManaDPSDownTimePerc = PercTimeNoDPSforNoMana;

        // damage
        // Ranged Attack Power
        calculatedStats.apFromBase = character.getLevel() * 2f - 20f;
        calculatedStats.apFromAGI = stats.getAgility();
        calculatedStats.apFromGear = stats.getAttackPower()
                - calculatedStats.apFromBase
                - calculatedStats.apFromAGI;

        // use for pet calculations
        calculatedStats.apSelfBuffed = stats.getAttackPower();

        // used for hunter calculations
        calculatedStats.apTotal = calculatedStats.apSelfBuffed;

        float RAP = calculatedStats.apTotal;

        // Armor Penetration
        float ArmorDamageReduction = getArmorDamageReduction(character, stats, calcOpts, bossOpts);
        calculatedStats.damageReductionFromArmor = (1f - ArmorDamageReduction);

        // Damage Adjustments
        // Partial Resists
        float averageResist = (levelDifF) * 0.02f;
        float resist10 = 5.0f * averageResist;
        float resist20 = 2.5f * averageResist;
        float partialResistDamageAdjust = 1f - (resist10 * 0.1f + resist20 * 0.2f);

        // Focused Fire
        float focusedFireDamageAdjust = 1f + 0.01f * talents.getFocusedFire();

        // Black Arrow Damage Multiplier
        float blackArrowUptime = 0;
        if (calculatedStats.priorityRotation.containsShot(Shots.BlackArrow)) {
            SpecialEffect blackarrow = new SpecialEffect(Trigger.Use, new Stats(),
                    calculatedStats.blackArrow.duration, calculatedStats.blackArrow.freq);
            blackArrowUptime = blackarrow.getAverageUptime(0f, 1f, calculatedStats.autoShotStaticSpeed, (float) calcOpts.duration);
        }
        float blackArrowAuraDamageAdjust = 1f + (0.06f * blackArrowUptime);
        float blackArrowSelfDamageAdjust = 1f + (RAP / 225000f);

        // Noxious Stings
        float noxiousStingsSerpentUptime = 0;
        if (calculatedStats.serpentSting.freq > 0) {
            noxiousStingsSerpentUptime = calculatedStats.serpentSting.duration / calculatedStats.serpentSting.freq;
        }
        if (calculatedStats.priorityRotation.chimeraRefreshesSerpent) {
            noxiousStingsSerpentUptime = 1;
        }
        float noxiousStingsDamageAdjust = 1f + (0.01f * talents.getNoxiousStings() * noxiousStingsSerpentUptime);
        float noxiousStingsSerpentDamageAdjust = 1f + (0.01f * talents.getNoxiousStings());

        // Ferocious Inspiration (calculated by pet model)
        float ferociousInspirationDamageAdjust = calculatedStats.ferociousInspirationDamageAdjust;
        float ferociousInspirationArcaneDamageAdjust = 1f + (0.03f * talents.getFerociousInspiration());

        // Improved Tracking
        float improvedTrackingDamageAdjust = 1f + 0.01f * talents.getImprovedTracking();

        // Ranged Weapon Specialization
        float rangedWeaponSpecializationDamageAdjust = 1;
        if (talents.getRangedWeaponSpecialization() == 1) rangedWeaponSpecializationDamageAdjust = 1.01f;
        if (talents.getRangedWeaponSpecialization() == 2) rangedWeaponSpecializationDamageAdjust = 1.03f;
        if (talents.getRangedWeaponSpecialization() == 3) rangedWeaponSpecializationDamageAdjust = 1.05f;

        // Marked For Death (assume hunter's mark is on target)
        float markedForDeathDamageAdjust = 1f + 0.01f * talents.getMarkedForDeath();

        // DamageTakenDebuffs
        float targetPhysicalDebuffsDamageAdjust = (1f + statsBuffs.getBonusPhysicalDamageMultiplier());

        // Barrage
        float barrageDamageAdjust = 1f + 0.04f * talents.getBarrage();

        // Sniper Training
        float sniperTrainingDamageAdjust = 1f + 0.02f * talents.getSniperTraining();

        // Improve Stings
        float improvedStingsDamageAdjust = 1f + 0.1f * talents.getImprovedStings();

        // Trap Mastery
        float trapMasteryDamageAdjust = 1f + 0.1f * talents.getTrapMastery();

        // T.N.T.
        float TNTDamageAdjust = 1f + 0.02f * talents.getTNT();

        // These intermediates group the two common sets of adjustments
        float talentDamageAdjust = focusedFireDamageAdjust
                * blackArrowAuraDamageAdjust
                * noxiousStingsDamageAdjust
                * ferociousInspirationDamageAdjust
                * improvedTrackingDamageAdjust
                * rangedWeaponSpecializationDamageAdjust
                * markedForDeathDamageAdjust;

        float talentDamageStingAdjust = focusedFireDamageAdjust
                * blackArrowAuraDamageAdjust
                * noxiousStingsDamageAdjust
                * ferociousInspirationDamageAdjust;

        // Full Bonus Damage Adjust
        float BonusDamageAdjust = 1f + stats.getBonusDamageMultiplier();

        // Bonus Crit Damage
        // MortalShots
        float mortalShotsCritDamage = 0.06f * talents.getMortalShots();
        // CritDamageMetaGems
        float metaGemCritDamage = 1f + (statsItems.getBonusCritMultiplier() * 2);
        // Marked For Death
        float markedForDeathCritDamage = 0.02f * talents.getMarkedForDeath();
        float baseCritDamage = (1f + mortalShotsCritDamage) * metaGemCritDamage; // CriticalHitDamage
        float specialCritDamage = (1f + mortalShotsCritDamage + markedForDeathCritDamage) * metaGemCritDamage; // SpecialCritDamage

        // pet - part 2
        // Pet DPS Calculations
        calculatedStats.pet.calculateDPS();

        // shot damage calcs
        // AutoShot
        // scope damage only applies to autoshot, so is not added to the normalized damage
        float rangedAmmoDamage = rangedAmmoDPS * rangedWeaponSpeed;
        float rangedAmmoDamageNormalized = rangedAmmoDPS * 2.8f;

        float damageFromRAP = RAP / 14f * rangedWeaponSpeed;
        float damageFromRAPNormalized = RAP / 14f * 2.8f;

        float autoShotDamage = rangedWeaponDamage
                + rangedAmmoDamage
                + stats.getWeaponDamage()
                + damageFromRAP
                + stats.getScopeDamage();
        float autoShotDamageNormalized = rangedWeaponDamage
                + rangedAmmoDamageNormalized
                + stats.getWeaponDamage()
                + damageFromRAPNormalized
                + stats.getScopeDamage();

        float autoShotDamageAdjust = talentDamageAdjust
                * targetPhysicalDebuffsDamageAdjust
                * ArmorDamageReduction
                * BonusDamageAdjust;
        float autoShotCritAdjust = 1f * metaGemCritDamage;

        float autoShotDamageReal = calcEffectiveDamage(autoShotDamage,
                ChanceToMiss,
                constrainCrit(critMOD, stats.getPhysicalCrit()),
                autoShotCritAdjust,
                autoShotDamageAdjust);

        float hunterAutoDPS = autoShotsPerSecond
                * autoShotDamageReal
                * (1f - calculatedStats.aspectViperPenalty)
                * tier7ViperDamageAdjust;

        float QSBaseFrequencyIncrease = 0f;
        if ((calcOpts.selectedAspect == Aspect.Hawk || calcOpts.selectedAspect == Aspect.Dragonhawk)
                && talents.getImprovedAspectOfTheHawk() > 0) {
            float quickShotsEffect = 0.03f * talents.getImprovedAspectOfTheHawk();
            if (talents.isGlyphOfTheHawk()) {
                quickShotsEffect += 0.06f;
            }
            Stats quickShotsStats = new Stats();
            quickShotsStats.setRangedHaste(quickShotsEffect);
            SpecialEffect QuickShots = new SpecialEffect(Trigger.PhysicalHit, quickShotsStats, 12f, 0f, 0.10f);
            QSBaseFrequencyIncrease = QuickShots.getAverageStats(1f / totalShotsPerSecond, (1f - stats.getPhysicalHit()), rangedWeaponSpeed, calcOpts.duration).getRangedHaste();
        }

        calculatedStats.aspectBeastLostDPS = (0f - QSBaseFrequencyIncrease) * (1f - calculatedStats.aspectUptimeHawk) * hunterAutoDPS;

        calculatedStats.setAutoshotDPS(hunterAutoDPS);

        // August 2009 Wild Quiver
        calculatedStats.setWildQuiverDPS(0);
        if (talents.getWildQuiver() > 0) {
            float wildQuiverProcChance = talents.getWildQuiver() * 0.04f;
            float wildQuiverProcFrequency = (autoShotSpeed / wildQuiverProcChance);
            float wildQuiverDamageNormal = 0.8f * (rangedWeaponDamage + statsItems.getWeaponDamage() + damageFromRAP);
            float wildQuiverDamageAdjust = talentDamageAdjust * partialResistDamageAdjust * (1f + targetDebuffsNature) * BonusDamageAdjust;

            float wildQuiverDamageReal = calcEffectiveDamage(
                    wildQuiverDamageNormal,
                    ChanceToMiss,
                    constrainCrit(critMOD, stats.getPhysicalCrit()),
                    1f,
                    wildQuiverDamageAdjust);

            //29-10-2009 Drizz: Added the ViperUpTIme penalty
            calculatedStats.setWildQuiverDPS((wildQuiverDamageReal / wildQuiverProcFrequency) * (1f - calculatedStats.aspectViperPenalty));
        }

        // August 2009 Steady Shot

        // base = shot_base + gear_weapon_damage + normalized_ammo_dps + (RAP * 0.1)
        //        + (rangedWeaponDamage / ranged_weapon_speed * 2.8)
        float steadyShotDamageNormal = 252f
                + statsItems.getWeaponDamage()
                + rangedAmmoDamageNormalized
                + (RAP * 0.1f)
                + (rangedWeaponDamage / rangedWeaponSpeed * 2.8f);
        // adjust = talent_adjust * gronnstalker_bonus * glyph_of_steadyshot
        //          * sniper_training * physcial_debuffs
        // to-maybe-do: Gronnstalker set bonus
        float steadyShotDamageAdjust = talentDamageAdjust
                * targetPhysicalDebuffsDamageAdjust
                * sniperTrainingDamageAdjust
                * (talents.isGlyphOfSteadyShot() ? 1.1f : 1f)
                * BonusDamageAdjust;

        // Drizz: 31-10-2009 Aligned the calculations with spreadsheet v92b
        // Also moved the armorReduction adjust to be multiplied after DamageReal Calc
        // Corrected from Spreadsheet changelog 91e "T9 2-set bonus only crits for spell-crit bonus damage (i.e. 50% instead of 100%), not affected by Mortal Shots"
        // This is the reason for the 0.5 multiplier and that markedForDeath is kept outside
        float steadyShotCritAdjust = metaGemCritDamage + 0.5f * mortalShotsCritDamage * (1f + metaGemCritDamage) + markedForDeathCritDamage;

        float steadyShotDamageReal = calcEffectiveDamage(
                steadyShotDamageNormal,
                ChanceToMiss,
                calculatedStats.steadyShot.getCritChance(),
                steadyShotCritAdjust,
                steadyShotDamageAdjust);

        steadyShotDamageReal *= ArmorDamageReduction;

        calculatedStats.steadyShot.damage = steadyShotDamageReal;

        float steadyShotAvgNonCritDamage = steadyShotDamageNormal * steadyShotDamageAdjust * ArmorDamageReduction;
        float steadyShotAvgCritDamage = steadyShotAvgNonCritDamage * (1f + steadyShotCritAdjust);
        //021109 Drizz: Have to add the Mangle/Trauma buff effect.
        float steadyShotPiercingShots = (1f + targetDebuffBleed)
                * (talents.getPiercingShots() * 0.1f)
                * calculatedStats.steadyShot.getCritChance() * steadyShotAvgCritDamage;

        //Drizz: Add the piercingShots effect
        calculatedStats.steadyShot.damage = steadyShotDamageReal + steadyShotPiercingShots;

        // August 2009 Serpent Sting

        // base_damage = 1210 + (0.2 * RAP)
        float serpentStingDamageBase = (float) HunterFormat.roundHalfEven(1210 + (RAP * 0.2f), 1);

        // T9 2-piece bonus
        float serpentStingT9CritAdjust = 1;
        float serpentStingInterimBonus;
        float serpentStingCriticalHitDamage;
        // 29-10-2009 Drizz: The name in the buff have not switched from Battlegear (i.e. the name is of the Horde buff)
        // if (character.ActiveBuffsContains("Windrunner's Pursuit 2 Piece Bonus"))
        if (stats.getBonusHunter_T9_2P_SerpCanCrit() > 0) {
            // Drizz : aligned with v92b
            serpentStingInterimBonus = 0.5f + 0.5f * mortalShotsCritDamage + 0.5f;
            serpentStingCriticalHitDamage = serpentStingInterimBonus * (1f + (1f + 0.5f) * (metaGemCritDamage - 1f) / 2f + (1f + 0.5f) * (metaGemCritDamage - 1) / 2);
            serpentStingT9CritAdjust = 1f + constrainCrit(critMOD, stats.getPhysicalCrit()) * serpentStingCriticalHitDamage;
        }

        double serpentStingCritAdjustment = serpentStingT9CritAdjust;

        // damage_adjust = (sting_talent_adjusts ~ noxious stings) * improved_stings * improved_tracking
        //                  + partial_resists * tier-8_2-piece_bonus * target_nature_debuffs * 100%_noxious_stings
        float serpentStingDamageAdjust = focusedFireDamageAdjust
                * blackArrowAuraDamageAdjust
                * ferociousInspirationDamageAdjust
                * noxiousStingsSerpentDamageAdjust
                * improvedStingsDamageAdjust
                * improvedTrackingDamageAdjust
                * partialResistDamageAdjust
                * serpentStingT9CritAdjust
                * (1f + targetDebuffsNature)
                * BonusDamageAdjust;

        // T8 2-piece bonus
        serpentStingDamageAdjust += statsBuffs.getBonusHunter_T8_2P_SerpDmg();

        float serpentStingTicks = calculatedStats.serpentSting.duration / 3f;
        float serpentStingDamagePerTick = (float) HunterFormat.roundHalfEven(serpentStingDamageBase * serpentStingDamageAdjust / 5f, 1);
        float serpentStingDamageReal = serpentStingDamagePerTick * serpentStingTicks;

        calculatedStats.serpentSting.type = Shots.SerpentSting;
        calculatedStats.serpentSting.damage = serpentStingDamageReal;

        // August 2009 Aimed Shot
        // Drizz: 31-10-2009 Aligned the calculations with spreadsheet v92b
        // Also moved the armorReduction adjust to be multiplied after DamageReal Calc

        // base_damage = normalized_shot + 408 (but ammo is not normalized!)
        float aimedShotDamageNormal = (rangedWeaponDamage + rangedAmmoDamage + statsItems.getWeaponDamage() + damageFromRAPNormalized) + 408f;

        // Corrected from Spreadsheet changelog 91e "T9 2-set bonus only crits for spell-crit bonus damage (i.e. 50% instead of 100%), not affected by Mortal Shots"
        // This is the reason for the 0.5 multiplier and that markedForDeath is kept outside
        float aimedShotCritAdjust = metaGemCritDamage + 0.5f * mortalShotsCritDamage * (1f + metaGemCritDamage) + markedForDeathCritDamage;

        // damage_adjust = talent_adjust * barrage_adjust * target_debuff_adjust * sniper_training_adjust * improved_ss_adjust
        float aimedShotDamageAdjust = talentDamageAdjust * barrageDamageAdjust * targetPhysicalDebuffsDamageAdjust
                * sniperTrainingDamageAdjust * ISSAimedShotDamageAdjust * BonusDamageAdjust;

        float aimedShotDamageReal = calcEffectiveDamage(
                aimedShotDamageNormal,
                ChanceToMiss,
                calculatedStats.aimedShot.getCritChance(),
                aimedShotCritAdjust,
                aimedShotDamageAdjust);

        calculatedStats.aimedShot.damage = aimedShotDamageReal;

        aimedShotDamageReal *= ArmorDamageReduction;

        //Drizz: added for piercing shots
        float aimedShotAvgNonCritDamage = aimedShotDamageNormal * aimedShotDamageAdjust * ArmorDamageReduction;
        float aimedShotAvgCritDamage = aimedShotAvgNonCritDamage * (1f + aimedShotCritAdjust);
        //021109 Drizz: Have to add the Mangle/Trauma buff effect.
        float aimedShotPiercingShots = (1f + targetDebuffBleed)
                * (character.getHunterTalents().getPiercingShots() * 0.1f)
                * calculatedStats.aimedShot.getCritChance() * aimedShotAvgCritDamage;

        //Drizz: Trying out...
        calculatedStats.aimedShot.damage = aimedShotDamageReal + aimedShotPiercingShots;

        // August 2009 Explosive Shot

        // base_damage = 425 + 14% of RAP
        float explosiveShotDamageNormal = 425f + (RAP * 0.14f);

        // crit_damage = 1 + mortal_shots + gem-crit
        float explosiveShotCritAdjust = (1f + mortalShotsCritDamage) * metaGemCritDamage;

        // damage_adjust = talent_adjust * tnt * fire_debuffs * sinper_training * partial_resist
        float explosiveShotDamageAdjust = talentDamageAdjust * TNTDamageAdjust * sniperTrainingDamageAdjust
                * partialResistDamageAdjust * (1f + targetDebuffsFire) * BonusDamageAdjust;

        float explosiveShotDamageReal = calcEffectiveDamage(
                explosiveShotDamageNormal,
                ChanceToMiss,
                calculatedStats.explosiveShot.getCritChance(),
                explosiveShotCritAdjust,
                explosiveShotDamageAdjust);

        float explosiveShotDamagePerShot = explosiveShotDamageReal * 3f;

        calculatedStats.explosiveShot.damage = explosiveShotDamagePerShot;

        // August 2009 Chimera Shot

        // base_damage = normalized_autoshot * 125%
        // Drizz: Making Changes
        float chimeraShotDamageNormal = (rangedAmmoDamage + (RAP / 14f * 2.8f) + rangedWeaponDamage) * 1.25f;

        // Drizz:
        // Corrected from Spreadsheet changelog 91e "T9 2-set bonus only crits for spell-crit bonus damage (i.e. 50% instead of 100%), not affected by Mortal Shots"
        // This is the reason for the 0.5 multiplier and that markedForDeath is kept outside
        float chimeraShotCritAdjust = metaGemCritDamage + 0.5f * mortalShotsCritDamage * (1f + metaGemCritDamage) + markedForDeathCritDamage;

        // damage_adjust = talent_adjust * nature_debuffs * ISS_cs_bonus * partial_resist
        float chimeraShotDamageAdjust = talentDamageAdjust * ISSChimeraShotDamageAdjust
                * partialResistDamageAdjust * (1f + targetDebuffsNature) * BonusDamageAdjust;

        float chimeraShotDamageReal = calcEffectiveDamage(
                chimeraShotDamageNormal,
                ChanceToMiss,
                constrainCrit(critMOD, stats.getPhysicalCrit()),
                chimeraShotCritAdjust,
                chimeraShotDamageAdjust);

        //Drizz: added for piercing shots
        float chimeraShotAvgNonCritDamage = chimeraShotDamageNormal * talentDamageAdjust * ISSChimeraShotDamageAdjust * (1f + targetDebuffsNature);
        float chimeraShotAvgCritDamage = chimeraShotAvgNonCritDamage * (1f + chimeraShotCritAdjust);
        // 021109 - Drizz: Had to add the Bleed Damage Multiplier
        float chimeraShotPiercingShots = (1f + targetDebuffBleed)
                * (character.getHunterTalents().getPiercingShots() * 0.1f)
                * constrainCrit(critMOD, stats.getPhysicalCrit())
                * chimeraShotAvgCritDamage;

        calculatedStats.chimeraShot.damage = chimeraShotDamageReal + chimeraShotPiercingShots;

        // calculate damage from serpent sting
        // Drizz: Adding
        float chimeraShotSerpentMultiplier = improvedStingsDamageAdjust
                * improvedTrackingDamageAdjust
                * noxiousStingsDamageAdjust
                * partialResistDamageAdjust
                * (1f + targetDebuffsNature)
                * (1f + stats.getBonusHunter_T8_2P_SerpDmg())
                * focusedFireDamageAdjust
                * blackArrowAuraDamageAdjust
                * ferociousInspirationArcaneDamageAdjust;

        float chimeraShotSerpentStingDamage = (float) HunterFormat.roundHalfEven(serpentStingDamageBase * chimeraShotSerpentMultiplier / 5f, 1) * serpentStingTicks;

        float chimeraShotEffect;
        if (calculatedStats.serpentSting.failReasonAlreadyUsed) {
            chimeraShotEffect = chimeraShotSerpentStingDamage * 0.4f;
        } else {
            chimeraShotEffect = 0;
        }

        // Drizz: Updates
        float chimeraShotSerpentCritAdjust = metaGemCritDamage + (0.5f * metaGemCritDamage + 0.5f) * mortalShotsCritDamage;
        float chimeraShotSerpentDamageAdjust = (1f - ChanceToMiss) * (1f + constrainCrit(critMOD, stats.getPhysicalCrit()) * chimeraShotSerpentCritAdjust);

        float chimeraShotSerpentTotalAdjust = chimeraShotSerpentDamageAdjust * talentDamageAdjust * (1f + targetDebuffsNature);

        calculatedStats.chimeraShot.damage += chimeraShotEffect * chimeraShotSerpentTotalAdjust;

        // August 2009 Arcane Shot

        // base_damage = 492 + weapon_damage_gear + (RAP * 15%)
        float arcaneShotDamageNormal = 492f + statsItems.getWeaponDamage() + (RAP * 0.15f);

        // Drizz:
        // Corrected from Spreadsheet changelog 91e "T9 2-set bonus only crits for spell-crit bonus damage (i.e. 50% instead of 100%), not affected by Mortal Shots"
        // This is the reason for the 0.5 multiplier and that markedForDeath is kept outside
        float arcaneShotCritAdjust = metaGemCritDamage + 0.5f * mortalShotsCritDamage * (1f + metaGemCritDamage) + markedForDeathCritDamage;

        float arcaneShotDamageAdjust = talentDamageAdjust * partialResistDamageAdjust * (1f + 0.05f * talents.getImprovedArcaneShot())
                * ferociousInspirationArcaneDamageAdjust * ISSArcaneShotDamageAdjust * BonusDamageAdjust; // missing arcane_debuffs!

        float arcaneShotDamageReal = calcEffectiveDamage(
                arcaneShotDamageNormal,
                ChanceToMiss,
                calculatedStats.arcaneShot.getCritChance(),
                arcaneShotCritAdjust,
                arcaneShotDamageAdjust);

        calculatedStats.arcaneShot.damage = arcaneShotDamageReal;

        // August 2009 Multi Shot

        float multiShotDamageNormal = rangedWeaponDamage + statsItems.getWeaponDamage() + rangedAmmoDamage
                + calculatedStats.getBasicStats().getScopeDamage() + 408f + (RAP * 0.2f);
        float multiShotDamageAdjust = talentDamageAdjust * barrageDamageAdjust * targetPhysicalDebuffsDamageAdjust
                * ArmorDamageReduction * BonusDamageAdjust; // missing: pvp gloves bonus

        float multiShotDamageReal = calcEffectiveDamage(
                multiShotDamageNormal,
                ChanceToMiss,
                calculatedStats.multiShot.getCritChance(),
                1f,
                multiShotDamageAdjust);

        calculatedStats.multiShot.damage = multiShotDamageReal;

        // August 2009 Volley
        float volleyDamageNormal = 353f + RAP * 0.0837f; // * AvgNumTargets
        float volleyDamageAdjust = talentDamageAdjust
                * partialResistDamageAdjust
                * barrageDamageAdjust
                * BonusDamageAdjust;
        float numvolleyTicks = 6f;
        float volleyDamageReal = volleyDamageNormal * volleyDamageAdjust * numvolleyTicks;

        calculatedStats.volley.damage = volleyDamageReal;

        // August 2009 Black Arrow
        float blackArrowDamageNormal = 2765f + (RAP * 0.1f);

        // this is a long list...
        float blackArrowDamageAdjust = partialResistDamageAdjust * focusedFireDamageAdjust
                * noxiousStingsDamageAdjust
                * ferociousInspirationDamageAdjust * improvedTrackingDamageAdjust
                * rangedWeaponSpecializationDamageAdjust * markedForDeathDamageAdjust
                * (sniperTrainingDamageAdjust + trapMasteryDamageAdjust + TNTDamageAdjust - 2f)
                * blackArrowSelfDamageAdjust * (1f + targetDebuffsShadow) * BonusDamageAdjust;

        float blackArrowDamage = blackArrowDamageNormal * blackArrowDamageAdjust;

        calculatedStats.blackArrow.damage = blackArrowDamage;

        // August 2009 Kill Shot
        // Drizz: 31-10-2009 Aligned the calculations with spreadsheet v92b
        // Also moved the armorReduction adjust to be multiplied after DamageReal Calc

        float killShotDamageNormal = (autoShotDamage * 2f) + statsItems.getWeaponDamage() + 650f + (RAP * 0.4f);

        // Corrected from Spreadsheet changelog 91e "T9 2-set bonus only crits for spell-crit bonus damage (i.e. 50% instead of 100%), not affected by Mortal Shots"
        // This is the reasone for the 0.5 multiplier and that markedForDeath is kept outside
        float killShotCritAdjust = metaGemCritDamage + 0.5f * mortalShotsCritDamage * (1f + metaGemCritDamage) + markedForDeathCritDamage;

        float killShotDamageAdjust = talentDamageAdjust * targetPhysicalDebuffsDamageAdjust * ArmorDamageReduction * BonusDamageAdjust;

        float killShotDamageReal = calcEffectiveDamage(
                killShotDamageNormal,
                ChanceToMiss,
                calculatedStats.killShot.getCritChance(),
                killShotCritAdjust,
                killShotDamageAdjust);

        calculatedStats.killShot.damage = killShotDamageReal;

        // August 2009 Silencing Shot

        float silencingShotDamageNormal = (rangedWeaponDamage + rangedAmmoDamage + damageFromRAPNormalized) * 0.5f;
        float silencingShotDamageAdjust = talentDamageAdjust * targetPhysicalDebuffsDamageAdjust * ArmorDamageReduction * BonusDamageAdjust;
        float silencingShotCritAdjust = 1f * metaGemCritDamage;

        float silencingShotDamageReal = calcEffectiveDamage(
                silencingShotDamageNormal,
                ChanceToMiss,
                constrainCrit(critMOD, stats.getPhysicalCrit()),
                silencingShotCritAdjust,
                silencingShotDamageAdjust);

        calculatedStats.silencingShot.damage = silencingShotDamageReal;

        // August 2009 Immolation Trap
        float immolationTrapDamage = 1885f + (0.1f * RAP);
        float immolationTrapDamageAdjust = (1f - targetDebuffsFire) * partialResistDamageAdjust * trapMasteryDamageAdjust
                * TNTDamageAdjust * talentDamageStingAdjust * BonusDamageAdjust;
        float immolationTrapProjectedDamage = immolationTrapDamage * immolationTrapDamageAdjust;
        float immolationTrapDamagePerTick = immolationTrapProjectedDamage / (talents.isGlyphOfImmolationTrap() ? 2.5f : 5f);
        float immolationTrapTicks = talents.isGlyphOfImmolationTrap() ? 3f : 5f;

        calculatedStats.immolationTrap.damage = immolationTrapDamagePerTick * immolationTrapTicks;

        // Explosive Trap
        float explosiveTrapDamage = (((0.1f * RAP) + 523f) + ((0.1f * RAP) + 671f)) / 2f + 900f;
        float explosiveTrapDamageAdjust = (1f - targetDebuffsFire)
                * partialResistDamageAdjust
                * trapMasteryDamageAdjust
                * TNTDamageAdjust
                * talentDamageStingAdjust
                * BonusDamageAdjust;
        float explosiveTrapCritAdjust = 1f * metaGemCritDamage;
        float explosiveTrapProjectedDamage = explosiveTrapDamage * explosiveTrapDamageAdjust;
        float explosiveTrapDamagePerTick = explosiveTrapProjectedDamage / 20f;
        float explosiveTrapTicks = 20f;

        if (talents.isGlyphOfExplosiveTrap()) {
            calculatedStats.explosiveTrap.damage = calcEffectiveDamage(
                    explosiveTrapDamagePerTick,
                    0f,
                    constrainCrit(critMOD, stats.getPhysicalCrit()),
                    explosiveShotCritAdjust,
                    explosiveShotDamageAdjust)
                    * explosiveTrapTicks;
        } else {
            calculatedStats.explosiveTrap.damage = explosiveTrapDamagePerTick * explosiveTrapTicks;
        }

        // Freezing Trap
        calculatedStats.freezingTrap.damage = 0f;

        // Frost Trap
        calculatedStats.frostTrap.damage = 0f;

        // Rapid Fire
        calculatedStats.rapidFire.damage = 0;

        // Piercing Shots
        calculatedStats.setPiercingShotsDPS(0);
        calculatedStats.setPiercingShotsDPSSteadyShot(0);
        calculatedStats.setPiercingShotsDPSAimedShot(0);
        calculatedStats.setPiercingShotsDPSChimeraShot(0);

        if (talents.getPiercingShots() > 0) {
            float piercingShotsDamageDone = talents.getPiercingShots() * 0.1f;
            float piercingShotsMangleOnTarget = targetDebuffBleed;
            float piercingShotsTotalModifier = piercingShotsDamageDone * piercingShotsMangleOnTarget;
            float piercingShotsSteadyShotFrequency = calculatedStats.steadyShot.freq;
            float piercingShotsSteadyShotDamageAdded = steadyShotPiercingShots;

            float piercingShotsAimedShotFrequency = calculatedStats.aimedShot.freq;
            float piercingShotsAimedShotDamageAdded = aimedShotPiercingShots;
            float piercingShotsChimeraShotFrequency = calculatedStats.chimeraShot.freq;
            float piercingShotsChimeraShotDamageAdded = chimeraShotPiercingShots;

            if (piercingShotsSteadyShotFrequency > 0) {
                calculatedStats.setPiercingShotsDPS(calculatedStats.getPiercingShotsDPS() + piercingShotsSteadyShotDamageAdded / piercingShotsSteadyShotFrequency);
            }
            if (piercingShotsAimedShotFrequency > 0) {
                calculatedStats.setPiercingShotsDPS(calculatedStats.getPiercingShotsDPS() + piercingShotsAimedShotDamageAdded / piercingShotsAimedShotFrequency);
            }
            if (piercingShotsChimeraShotFrequency > 0) {
                calculatedStats.setPiercingShotsDPS(calculatedStats.getPiercingShotsDPS() + piercingShotsChimeraShotDamageAdded / piercingShotsChimeraShotFrequency);
            }

            calculatedStats.setPiercingShotsDPSSteadyShot(piercingShotsSteadyShotDamageAdded);
            calculatedStats.setPiercingShotsDPSAimedShot(piercingShotsAimedShotDamageAdded);
            calculatedStats.setPiercingShotsDPSChimeraShot(piercingShotsChimeraShotDamageAdded);
        }

        // August 2009 Shot Rotation
        calculatedStats.priorityRotation.viperDamagePenalty = calculatedStats.aspectViperPenalty;
        calculatedStats.priorityRotation.noManaPenalty = PercTimeNoDPSforNoMana;
        calculatedStats.priorityRotation.calculateRotationDPS();
        calculatedStats.setCustomDPS(calculatedStats.priorityRotation.dps);

        // August 2009 Kill Shot Sub-20% Usage

        float killShotCurrentFreq = calculatedStats.killShot.freq;
        float killShotPossibleFreq = calcOpts.useRotationTest ? calculatedStats.killShot.freq : calculatedStats.killShot.startFreq;
        float steadyShotCurrentFreq = calculatedStats.steadyShot.freq;

        float steadyShotNewFreq = steadyShotCurrentFreq;
        if (killShotCurrentFreq == 0f && steadyShotCurrentFreq > 0f && killShotPossibleFreq > 0f) {
            steadyShotNewFreq = 1f / (1f / steadyShotCurrentFreq - 1f / killShotPossibleFreq);
        }

        float oldKillShotDPS = calculatedStats.killShot.dps;
        float newKillShotDPS = killShotPossibleFreq > 0 ? calculatedStats.killShot.damage / killShotPossibleFreq : 0f;
        newKillShotDPS *= (1f - calculatedStats.aspectViperPenalty);

        float oldSteadyShotDPS = calculatedStats.steadyShot.dps;
        float newSteadyShotDPS = steadyShotNewFreq > 0 ? calculatedStats.steadyShot.damage / steadyShotNewFreq : 0f;
        newSteadyShotDPS *= (1f - calculatedStats.aspectViperPenalty);

        float killShotDPSGain = newKillShotDPS > 0f ? (newKillShotDPS + newSteadyShotDPS) - (oldKillShotDPS + oldSteadyShotDPS) : 0f;

        float timeSpentSubTwenty = 0;
        if (calcOpts.duration > 0 && calcOpts.timeSpentSub20 > 0) timeSpentSubTwenty = (float) calcOpts.timeSpentSub20 / (float) calcOpts.duration;
        if (calcOpts.bossHPPerc < 0.2f) timeSpentSubTwenty = 1f;

        float killShotSubGain = timeSpentSubTwenty * killShotDPSGain;

        calculatedStats.killShotSub20NewSteadyFreq = steadyShotNewFreq;
        calculatedStats.killShotSub20NewDPS = newKillShotDPS;
        calculatedStats.killShotSub20NewSteadyDPS = newSteadyShotDPS;
        calculatedStats.killShotSub20Gain = killShotDPSGain;
        calculatedStats.killShotSub20TimeSpent = timeSpentSubTwenty;
        calculatedStats.killShotSub20FinalGain = killShotSubGain;

        // Survivability
        float Health2SurvHunter = (stats.getHealth()) / 100f;
        Health2SurvHunter += (stats.getHealthRestore()) / 1000f;
        float Health2SurvPet = (calculatedStats.pet.petStats.getHealth()) / 100f;
        Health2SurvPet += (calculatedStats.pet.petStats.getHealthRestore()) / 1000f;
        float DmgTakenMods2SurvHunter = (1f - stats.getDamageTakenMultiplier()) * 100f;
        float DmgTakenMods2SurvPet = (1f - calculatedStats.pet.petStats.getDamageTakenMultiplier()) * 100f;
        float BossAttackPower2Surv = stats.getBossAttackPower() / 14f * -1f;
        float BossAttackSpeedMods2Surv = (1f - stats.getBossAttackSpeedMultiplier()) * 100f;
        float AvoidanceHunter = stats.getDodge() + stats.getParry();
        float AvoidancePet = calculatedStats.pet.petStats.getDodge() + calculatedStats.pet.petStats.getParry(); // should be pet stats
        float Armor2SurvHunter = (stats.getArmor()) / 100f;
        float Armor2SurvPet = (calculatedStats.pet.petStats.getArmor()) / 100f;
        float HealsPerSecHunter = ((talents.getSpiritBond() * 0.01f * stats.getHealth()) * (calcOpts.duration / 10f)) / calcOpts.duration;
        float HealsPerSecPet = ((talents.getSpiritBond() * 0.01f * calculatedStats.pet.petStats.getHealth()) * (calcOpts.duration / 10f)) / calcOpts.duration;

        // Zod's Repeating Longbow
        // Equip: Your ranged attacks have a [x]% chance to cause you
        // to instantly attack with this weapon for 50% weapon damage.
        // iLevel 264 | x = 4
        // iLevel 277 | x = 5
        calculatedStats.setBonusAttackProcsDPS(0f);
        if (stats.getZodProc() > 0) {
            float Chance = stats.getZodProc();
            float ProcDamage = rangedWeaponDamage
                    + rangedAmmoDamage
                    + stats.getWeaponDamage()
                    + stats.getScopeDamage();
            float ProcDamageReal = calcEffectiveDamage(ProcDamage * 0.50f,
                    ChanceToMiss,
                    0f,
                    1f,
                    autoShotDamageAdjust);
            SpecialEffect zod = new SpecialEffect(Trigger.RangedHit, new Stats(), 0f, 0f, Chance);
            float numProcs = calcOpts.duration * zod.getAverageProcsPerSecond(totalShotsPerSecond, 1f, autoShotSpeed, calcOpts.duration);
            float totalDamage = numProcs * ProcDamageReal;
            calculatedStats.setBonusAttackProcsDPS(totalDamage / calcOpts.duration);
        }

        // Special Damage Procs, like Bandit's Insignia or Hand-mounted Pyro Rockets
        Map<Trigger, Float> triggerIntervals = new LinkedHashMap<Trigger, Float>();
        Map<Trigger, Float> triggerChances = new LinkedHashMap<Trigger, Float>();
        calculateTriggers(character, calculatedStats, stats, calcOpts, bossOpts, triggerIntervals, triggerChances);
        SpecialDamageProcs SDP;
        calculatedStats.specProcDPS = 0f;
        if (stats.getRawSpecialEffectData() != null) {
            List<SpecialEffect> rawEffects = new ArrayList<SpecialEffect>(stats.getRawSpecialEffectDataSize());
            for (int i = 0; i < stats.getRawSpecialEffectDataSize(); i++) {
                rawEffects.add(stats.getRawSpecialEffectData()[i]);
            }
            SDP = new SpecialDamageProcs(character, stats,
                    calcOpts.targetLevel - character.getLevel(), rawEffects,
                    triggerIntervals, triggerChances, calcOpts.duration, combatFactors.getDamageReduction());
            calculatedStats.specProcDPS += SDP.calculate(ItemDamageType.Physical);
            calculatedStats.specProcDPS += SDP.calculate(ItemDamageType.Shadow);
            calculatedStats.specProcDPS += SDP.calculate(ItemDamageType.Holy);
            calculatedStats.specProcDPS += SDP.calculate(ItemDamageType.Arcane);
            calculatedStats.specProcDPS += SDP.calculate(ItemDamageType.Nature);
            calculatedStats.specProcDPS += SDP.calculate(ItemDamageType.Fire);
            calculatedStats.specProcDPS += SDP.calculate(ItemDamageType.Frost);
        }

        calculatedStats.setHunterDpsPoints((float) (calculatedStats.getAutoshotDPS()
                + calculatedStats.getWildQuiverDPS()
                + calculatedStats.getCustomDPS()
                + calculatedStats.killShotSub20FinalGain
                + calculatedStats.aspectBeastLostDPS
                + calculatedStats.getBonusAttackProcsDPS()
                + calculatedStats.specProcDPS));
        calculatedStats.setHunterSurvPoints(calcOpts.survScale *
                (0 // TotalHPSOnHunter
                        + Health2SurvHunter
                        + DmgTakenMods2SurvHunter
                        + BossAttackPower2Surv
                        + BossAttackSpeedMods2Surv
                        + AvoidanceHunter * 100f
                        + Armor2SurvHunter
                        + HealsPerSecHunter));
        calculatedStats.setPetSurvPoints(calcOpts.survScale *
                (0 // TotalHPSOnPet
                        + Health2SurvPet
                        + DmgTakenMods2SurvPet
                        + BossAttackPower2Surv
                        + BossAttackSpeedMods2Surv
                        + AvoidancePet * 100f
                        + Armor2SurvPet
                        + HealsPerSecPet));
        calculatedStats.setOverallPoints(calculatedStats.getHunterDpsPoints()
                + calculatedStats.getPetDpsPoints()
                + calculatedStats.getHunterSurvPoints()
                + calculatedStats.getPetSurvPoints());

        return calculatedStats;
    }

    public Stats getCharacterStats(Character character, Item additionalItem) {
        try {
            CalculationOptionsHunter calcOpts = (CalculationOptionsHunter) character.getCalculationOptions();
            if (calcOpts == null) {
                calcOpts = new CalculationOptionsHunter();
                character.setCalculationOptions(calcOpts);
            }
            BossOptions bossOpts = character.getBossOptions();
            HunterTalents talents = character.getHunterTalents();
            PetTalentTreeData petTalents = calcOpts.getPetTalents();
            int levelDif = calcOpts.targetLevel - character.getLevel();

            // From Race
            Stats statsRace = BaseStats.getBaseStats(character.getLevel(), CharacterClass.Hunter, character.getRace());
            statsRace.setPhysicalCrit(statsRace.getPhysicalCrit() + ((character.getRanged() != null
                    && ((character.getRace() == rawr.base.character.CharacterRace.Dwarf && character.getRanged().getItem().getType() == ItemType.Gun)
                    || (character.getRace() == rawr.base.character.CharacterRace.Troll && character.getRanged().getItem().getType() == ItemType.Bow))) ?
                    0.01f : 0.00f));

            // From Gear/Buffs
            Stats statsBuffs = getBuffsStats(character, calcOpts);
            Stats statsItems = new Stats();
            accumulateItemStats(statsItems, character, additionalItem);

            // From Options
            Stats statsOptionsPanel = new Stats();
            statsOptionsPanel.setPhysicalCrit(StatConversion.NPC_LEVEL_CRIT_MOD[levelDif]);
            statsOptionsPanel.setPhysicalHaste(0.15f); // This is from what Bags used to give that got rolled into the class
            CharacterCalculationsHunter calculatedStats = new CharacterCalculationsHunter();
            genPrioRotation(calculatedStats, calcOpts, talents);
            genAbilityCds(character, calculatedStats, calcOpts, bossOpts, talents);
            if (calculatedStats.priorityRotation.containsShot(Shots.RapidFire)) {
                Stats rapidFireStats = new Stats();
                rapidFireStats.setRangedHaste(talents.isGlyphOfRapidFire() ? 0.48f : 0.40f);
                statsOptionsPanel.addSpecialEffect(new SpecialEffect(Trigger.Use,
                        rapidFireStats,
                        15f, (5 - talents.getRapidKilling()) * 60f));
            }

            // From Talents
            Stats statsTalents = new Stats();
            statsTalents.setPhysicalHit((talents.getFocusedAim() * 0.01f));
            statsTalents.setPhysicalCrit((0.01f * talents.getLethalShots())
                    + (0.01f * talents.getMasterMarksman())
                    + (0.01f * talents.getKillerInstinct()));
            statsTalents.setBonusAgilityMultiplier((1f + 0.02f * talents.getCombatExperience())
                    * (1f + 0.03f * talents.getLightningReflexes())
                    * (1f + 0.01f * talents.getHuntingParty())
                    - 1f);
            statsTalents.setBonusIntellectMultiplier(0.02f * talents.getCombatExperience());
            statsTalents.setBonusStaminaMultiplier(0.02f * talents.getSurvivalist());
            statsTalents.setBaseArmorMultiplier((talents.getThickHide() / 3f) * 0.10f);
            statsTalents.setPhysicalHaste(0.04f * talents.getSerpentsSwiftness());
            statsTalents.setBonusAttackPowerMultiplier(0.10f * talents.getTrueshotAura());
            statsTalents.setBonusDamageMultiplier(0.10f * talents.getTheBeastWithin());
            statsTalents.setBonusPetDamageMultiplier(-1f * 0.10f * talents.getTheBeastWithin());
            statsTalents.setDodge(talents.getCatlikeReflexes() * 0.01f);
            statsTalents.setParry(talents.getDeflection() * 0.01f);
            statsTalents.setBonusHealthMultiplier(talents.getEnduranceTraining() * 0.01f);
            statsTalents.setDamageTakenMultiplier(-0.02f * talents.getSurvivalInstincts());
            if (talents.getMasterTactician() > 0) {
                Stats masterTacticianStats = new Stats();
                masterTacticianStats.setPhysicalCrit(talents.getMasterTactician() * 0.02f);
                SpecialEffect mt = new SpecialEffect(Trigger.PhysicalHit,
                        masterTacticianStats, 8f, 0f, 0.10f);
                statsTalents.addSpecialEffect(mt);
            }
            if ((calcOpts.selectedAspect == Aspect.Hawk || calcOpts.selectedAspect == Aspect.Dragonhawk)
                    && talents.getImprovedAspectOfTheHawk() > 0) {
                float quickShotsEffect = 0.03f * talents.getImprovedAspectOfTheHawk();
                if (talents.isGlyphOfTheHawk()) {
                    quickShotsEffect += 0.06f;
                }
                Stats quickShotsStats = new Stats();
                quickShotsStats.setRangedHaste(quickShotsEffect);
                SpecialEffect QuickShots = new SpecialEffect(Trigger.PhysicalHit,
                        quickShotsStats,
                        12f, 0f, 0.10f);
                statsTalents.addSpecialEffect(QuickShots);
            }
            if (calcOpts.selectedAspect == Aspect.Hawk || (calcOpts.selectedAspect == Aspect.Dragonhawk && talents.getAspectMastery() > 0)) {
                statsOptionsPanel.setRangedAttackPower(statsOptionsPanel.getRangedAttackPower() + 155f * (1f + talents.getAspectMastery() * 0.30f));
            }
            if (petTalents.callOfTheWild.value > 0) {
                Stats callOfTheWildStats = new Stats();
                callOfTheWildStats.setBonusRangedAttackPowerMultiplier(0.10f);
                callOfTheWildStats.setBonusPetAttackPowerMultiplier(0.10f);
                SpecialEffect callofthewild = new SpecialEffect(Trigger.Use,
                        callOfTheWildStats,
                        20f, (5f * 60f) * (1f - talents.getLongevity() * 0.10f));
                statsTalents.addSpecialEffect(callofthewild);
            }
            if (calcOpts.petFamily != PetFamily.None
                    && calculatedStats.priorityRotation.containsShot(Shots.BestialWrath)
                    && talents.getBestialWrath() > 0) {
                float cooldown = (talents.isGlyphOfBestialWrath() ? 100f : 120f) * (1f - talents.getLongevity() * 0.10f);
                float val1 = talents.getTheBeastWithin() > 0 ? 0.10f : 0f;
                float val2 = talents.getTheBeastWithin() > 0 ? -0.50f : 0f;
                Stats wrathStats = new Stats();
                wrathStats.setBonusPetDamageMultiplier((0.50f /* / (1f + val1)*/));
                wrathStats.setBonusDamageMultiplier(val1);
                wrathStats.setManaCostPerc(val2);
                SpecialEffect WrathBeastWithin = new SpecialEffect(Trigger.Use,
                        wrathStats,
                        10f, cooldown);
                statsTalents.addSpecialEffect(WrathBeastWithin);
            }
            if (petTalents.cullingTheHerd.value > 0) {
                float val1 = petTalents.cullingTheHerd.value * 0.01f;
                Stats cullingStats = new Stats();
                cullingStats.setBonusDamageMultiplier(val1);
                cullingStats.setBonusPetDamageMultiplier(val1);
                SpecialEffect CullingTheHerd = new SpecialEffect(Trigger.PetClawBiteSmackCrit,
                        cullingStats,
                        10f, 0f);
                statsTalents.addSpecialEffect(CullingTheHerd);
            }

            // Totals
            Stats statsGearEnchantsBuffs = new Stats();
            statsGearEnchantsBuffs.accumulate(statsItems);
            statsGearEnchantsBuffs.accumulate(statsBuffs);
            Stats statsTotal = new Stats();
            statsTotal.accumulate(statsRace);
            statsTotal.accumulate(statsItems);
            statsTotal.accumulate(statsBuffs);
            statsTotal.accumulate(statsTalents);
            statsTotal.accumulate(statsOptionsPanel);
            Stats statsProcs = new Stats();

            // Stamina & Health
            float totalBSTAM = statsTotal.getBonusStaminaMultiplier();
            float staBase = (float) Math.floor((1f + totalBSTAM) * statsRace.getStamina());
            float staBonus = (float) Math.floor((1f + totalBSTAM) * statsGearEnchantsBuffs.getStamina());
            statsTotal.setStamina(staBase + staBonus);

            // Health
            statsTotal.setHealth(statsTotal.getHealth() + StatConversion.getHealthFromStamina(statsTotal.getStamina()));
            statsTotal.setHealth(statsTotal.getHealth() * (1f + statsTotal.getBonusHealthMultiplier()));

            // Strength
            float totalBSTRM = statsTotal.getBonusStrengthMultiplier();
            float strBase = (float) Math.floor((1f + totalBSTRM) * statsRace.getStrength());
            float strBonus = (float) Math.floor((1f + totalBSTRM) * statsGearEnchantsBuffs.getStrength());
            statsTotal.setStrength(strBase + strBonus);

            //  Agility
            float totalBAGIM = statsTotal.getBonusAgilityMultiplier();
            float agiBase = (1f + totalBAGIM) * statsRace.getAgility();
            float agiBonus = (1f + totalBAGIM) * statsGearEnchantsBuffs.getAgility();
            statsTotal.setAgility(Math.max(0f, agiBase + agiBonus));

            if (talents.getExposeWeakness() > 0) {
                Stats exposeWeaknessStats = new Stats();
                exposeWeaknessStats.setAttackPower(statsTotal.getAgility() * 0.25f);
                SpecialEffect ExposeWeakness = new SpecialEffect(Trigger.RangedCrit,
                        exposeWeaknessStats,
                        7f, 0f, (1f / 3f * talents.getExposeWeakness()));
                statsTotal.addSpecialEffect(ExposeWeakness);
            }

            //  Intellect
            float totalBINTM = statsTotal.getBonusIntellectMultiplier();
            float intBase = (float) Math.floor((1f + totalBINTM) * statsRace.getIntellect());
            float intBonus = (float) Math.floor((1f + totalBINTM) * statsGearEnchantsBuffs.getIntellect());
            statsTotal.setIntellect(intBase + intBonus);

            // Armor
            statsTotal.setArmor(statsTotal.getArmor() * (1f + statsTotal.getBaseArmorMultiplier()));
            statsTotal.setBonusArmor(statsTotal.getBonusArmor() + statsTotal.getAgility() * 2f);
            statsTotal.setBonusArmor(statsTotal.getBonusArmor() * (1f + statsTotal.getBonusArmorMultiplier()));
            statsTotal.setArmor(statsTotal.getArmor() + statsTotal.getBonusArmor());

            // Attack Power
            statsTotal.setBonusAttackPowerMultiplier(statsTotal.getBonusAttackPowerMultiplier() * (1f + statsTotal.getBonusRangedAttackPowerMultiplier()));
            float totalBAPM = statsTotal.getBonusAttackPowerMultiplier();
            float apBase = (1f + totalBAPM) * (statsRace.getAttackPower() + statsRace.getRangedAttackPower());
            float apFromAGI = (1f + totalBAPM) * (statsTotal.getAgility());
            float apFromHvW = (1f + totalBAPM) * (statsTotal.getStamina() * (0.10f) * talents.getHunterVsWild());
            float apFromCAim = (1f + totalBAPM) * (statsTotal.getIntellect() * (1f / 3f) * talents.getCarefulAim());
            float apFromHM = (1f + totalBAPM) * (500f * (1f + talents.getImprovedHuntersMark() * 0.10f) * (talents.isGlyphOfHuntersMark() ? 1.20f : 1f));
            float apBonusOther = (1f + totalBAPM) * (statsGearEnchantsBuffs.getAttackPower() + statsGearEnchantsBuffs.getRangedAttackPower()
                    + statsOptionsPanel.getAttackPower() + statsOptionsPanel.getRangedAttackPower());
            statsTotal.setAttackPower(Math.max(0f, apBase + apFromAGI + apFromHvW + apFromCAim + apFromHM + apBonusOther));
            statsTotal.setRangedAttackPower(statsTotal.getAttackPower());

            // Spell Power
            float totalBSPM = statsTotal.getBonusSpellPowerMultiplier();
            float spBase = (1f + totalBSPM) * (statsRace.getSpellPower());
            float spBonusOther = (1f + totalBSPM) * (statsGearEnchantsBuffs.getSpellPower());
            statsTotal.setSpellPower((float) Math.floor(spBase + spBonusOther));

            // Crit
            statsTotal.setCritRating(statsTotal.getCritRating() + statsTotal.getRangedCritRating());
            statsTotal.setPhysicalCrit(statsTotal.getPhysicalCrit()
                    + (StatConversion.getCritFromAgility(statsTotal.getAgility(), character.getCharacterClass())
                    + -0.01536f
                    + StatConversion.getCritFromRating(Math.max(0f, statsTotal.getCritRating()), character.getCharacterClass())));

            // Haste
            statsTotal.setHasteRating(statsTotal.getHasteRating() + statsTotal.getRangedHasteRating());
            float ratingHasteBonus = StatConversion.getPhysicalHasteFromRating(Math.max(0f, statsTotal.getHasteRating()), character.getCharacterClass());
            statsTotal.setPhysicalHaste((1f + statsRace.getPhysicalHaste()) *
                    (1f + statsItems.getPhysicalHaste()) *
                    (1f + statsBuffs.getPhysicalHaste()) *
                    (1f + statsTalents.getPhysicalHaste()) *
                    (1f + statsOptionsPanel.getPhysicalHaste()) *
                    (1f + statsTotal.getRangedHaste()) *
                    (1f + ratingHasteBonus)
                    - 1f);

            // Hit
            statsTotal.setPhysicalHit(statsTotal.getPhysicalHit() + StatConversion.getHitFromRating(Math.max(0f, statsTotal.getHitRating() + statsTotal.getRangedHitRating())));
            statsTotal.setSpellHit(statsTotal.getSpellHit() + StatConversion.getHitFromRating(Math.max(0f, statsTotal.getHitRating())));

            // Mana
            // The first 20 Int = 20 Mana, while each subsequent Int = 15 Mana
            // (20-(20/15)) = 18.66666
            // spreadsheet uses 18.7, so we will too :)
            statsTotal.setMana((float) (statsRace.getMana() + 15f * (statsTotal.getIntellect() - 18.7f) + statsGearEnchantsBuffs.getMana()));

            // ArP
            if (statsTotal.getArmorPenetrationRating() < 0) statsTotal.setArmorPenetrationRating(0);

            // Handle Special Effects
            calculatedStats.pet = new PetCalculations(character, calculatedStats, calcOpts, bossOpts, statsTotal,
                    getPetBuffsStats(character, calcOpts));
            calculatedStats.pet.genPetStats();

            Map<Trigger, Float> triggerIntervals = new LinkedHashMap<Trigger, Float>();
            Map<Trigger, Float> triggerChances = new LinkedHashMap<Trigger, Float>();

            calculateTriggers(character, calculatedStats, statsTotal, calcOpts, bossOpts, triggerIntervals, triggerChances);

            if (calcOpts.petFamily == PetFamily.Wolf
                    && calculatedStats.pet.priorityRotation.getSkillFrequency(PetAttacks.FuriousHowl) > 0) {
                statsTotal.addSpecialEffect(FuriousHowl);
            }

            statsProcs.accumulate(getSpecialEffectsStats(character, triggerIntervals, triggerChances, statsTotal, null));

            // Handle Results of Special Effects
            // Base Stats
            statsProcs.setStamina((float) Math.floor(statsProcs.getStamina() * (1f + totalBSTAM) * (1f + statsProcs.getBonusStaminaMultiplier())));
            statsProcs.setStrength((float) Math.floor(statsProcs.getStrength() * (1f + totalBSTRM) * (1f + statsProcs.getBonusStrengthMultiplier())));
            statsProcs.setAgility(statsProcs.getAgility() * (1f + totalBAGIM) * (1f + statsProcs.getBonusAgilityMultiplier()));
            statsProcs.setAgility(statsProcs.getAgility() + statsProcs.getHighestStat() * (1f + totalBAGIM) * (1f + statsProcs.getBonusAgilityMultiplier()));
            statsProcs.setAgility(statsProcs.getAgility() + statsProcs.getParagon() * (1f + totalBAGIM) * (1f + statsProcs.getBonusAgilityMultiplier()));
            statsProcs.setHighestStat(0f);
            statsProcs.setParagon(0f); // we've added them into agi so kill it
            statsProcs.setHealth(statsProcs.getHealth() + (float) Math.floor(statsProcs.getStamina() * 10f));

            // Armor
            statsProcs.setArmor(statsProcs.getArmor() * (1f + statsTotal.getBaseArmorMultiplier() + statsProcs.getBaseArmorMultiplier()));
            statsProcs.setBonusArmor(statsProcs.getBonusArmor() + statsProcs.getAgility() * 2f);
            statsProcs.setBonusArmor(statsProcs.getBonusArmor() * (1f + statsTotal.getBonusArmorMultiplier() + statsProcs.getBonusArmorMultiplier()));
            statsProcs.setArmor(statsProcs.getArmor() + statsProcs.getBonusArmor());
            statsProcs.setBonusArmor(0); //it's been added to Armor so kill it

            // Attack Power
            statsProcs.setBonusAttackPowerMultiplier(statsProcs.getBonusAttackPowerMultiplier() * (1f + statsProcs.getBonusRangedAttackPowerMultiplier()));
            statsProcs.setBonusRangedAttackPowerMultiplier(0); //it's been added to Armor so kill it
            float totalBAPMProcs = (1f + totalBAPM) * (1f + statsProcs.getBonusAttackPowerMultiplier()) - 1f;
            float apFromAGIProcs = (1f + totalBAPMProcs) * (statsProcs.getAgility());
            float apFromSTRProcs = (1f + totalBAPMProcs) * (statsProcs.getStrength());
            float apBonusOtherProcs = (1f + totalBAPMProcs) * (statsProcs.getAttackPower() + statsProcs.getRangedAttackPower());
            statsProcs.setAttackPower(Math.max(0f, apFromAGIProcs + apFromSTRProcs + apBonusOtherProcs));
            statsProcs.setRangedAttackPower(statsProcs.getAttackPower());
            statsTotal.setAttackPower(statsTotal.getAttackPower() * (1f + statsProcs.getBonusAttackPowerMultiplier())); // Make sure the originals get your AP% procs

            // Crit
            statsProcs.setPhysicalCrit(statsProcs.getPhysicalCrit() + StatConversion.getCritFromAgility(statsProcs.getAgility(), character.getCharacterClass()));
            statsProcs.setPhysicalCrit(statsProcs.getPhysicalCrit() + StatConversion.getCritFromRating(statsProcs.getCritRating() + statsProcs.getRangedCritRating(), character.getCharacterClass()));

            // Haste
            statsProcs.setPhysicalHaste((1f + statsProcs.getPhysicalHaste())
                    * (1f + statsProcs.getRangedHaste())
                    * (1f + StatConversion.getPhysicalHasteFromRating(statsProcs.getHasteRating(), character.getCharacterClass()))
                    - 1f);

            // Add it back into the fold
            statsTotal.accumulate(statsProcs);

            statsTotal.setMana((float) Math.floor((1f + statsTotal.getBonusManaMultiplier()) * statsTotal.getMana()));
            return statsTotal;
        } catch (Exception ex) {
            return new Stats();
        }
    }

    private static final SpecialEffect FuriousHowl = new SpecialEffect(Trigger.Use, new Stats() {
        {
            setAttackPower(320f);
            setPetAttackPower(320f);
        }
    }, 20f, 40f);

    private static void calculateTriggers(Character character, CharacterCalculationsHunter calculatedStats, Stats statsTotal,
            CalculationOptionsHunter calcOpts, BossOptions bossOpts,
            Map<Trigger, Float> triggerIntervals, Map<Trigger, Float> triggerChances) {
        int levelDif = calcOpts.targetLevel - character.getLevel();
        float critMOD = StatConversion.NPC_LEVEL_CRIT_MOD[levelDif];
        HunterTalents talents = character.getHunterTalents();
        GenRotationResult rotation = genRotation(character, statsTotal, calculatedStats, calcOpts, bossOpts, talents);
        float autoShotsPerSecond = rotation.autoShotsPerSecond;
        float totalShotsPerSecond = rotation.totalShotsPerSecond;
        float autoShotSpeed = rotation.autoShotSpeed;

        triggerIntervals.put(Trigger.Use, 0f);
        triggerIntervals.put(Trigger.MeleeHit, Math.max(0f, calculatedStats.pet.petCompInterval()));
        triggerIntervals.put(Trigger.RangedHit, 1f / totalShotsPerSecond);
        triggerIntervals.put(Trigger.PhysicalHit, 1f / totalShotsPerSecond);
        triggerIntervals.put(Trigger.MeleeCrit, Math.max(0f, calculatedStats.pet.petCompInterval()));
        triggerIntervals.put(Trigger.RangedCrit, 1f / totalShotsPerSecond);
        triggerIntervals.put(Trigger.PhysicalCrit, 1f / totalShotsPerSecond);
        triggerIntervals.put(Trigger.DoTTick, talents.getPiercingShots() > 0 ? 1f : 0f);
        triggerIntervals.put(Trigger.DamageDone, Math.max(0f, 1f / (totalShotsPerSecond + ((talents.getPiercingShots() > 0 ? 1f : 0f) > 0 ? 1f / (talents.getPiercingShots() > 0 ? 1f : 0f) : 0f))));
        triggerIntervals.put(Trigger.DamageOrHealingDone, Math.max(0f, 1f / (totalShotsPerSecond + ((talents.getPiercingShots() > 0 ? 1f : 0f) > 0 ? 1f / (talents.getPiercingShots() > 0 ? 1f : 0f) : 0f)))); // Need to add Self-Heals
        triggerIntervals.put(Trigger.HunterAutoShotHit, 1f / autoShotsPerSecond);
        triggerIntervals.put(Trigger.SteadyShotHit, calculatedStats.steadyShot.cd);
        triggerIntervals.put(Trigger.PetClawBiteSmackCrit, Math.max(0f, calculatedStats.pet.petClawBiteSmackInterval));
        triggerIntervals.put(Trigger.SerpentWyvernStingsDoDamage, (calculatedStats.serpentSting.freq > 0 || calculatedStats.serpentSting.isRefreshed ? 3f : 0f));

        float chanceToMiss = Math.max(0f, StatConversion.WHITE_MISS_CHANCE_CAP[levelDif] - statsTotal.getPhysicalHit());
        float chanceToSpellMiss = Math.max(0f, StatConversion.getSpellMiss(levelDif, false) - statsTotal.getSpellHit());

        triggerChances.put(Trigger.Use, 1f);
        triggerChances.put(Trigger.MeleeHit, calculatedStats.pet.whAtkTable.getAnyLand());
        triggerChances.put(Trigger.RangedHit, (1f - chanceToMiss));
        triggerChances.put(Trigger.PhysicalHit, (1f - chanceToMiss));
        triggerChances.put(Trigger.MeleeCrit, Math.min(1f + critMOD, Math.max(0f, calculatedStats.pet.whAtkTable.crit)));
        triggerChances.put(Trigger.RangedCrit, Math.min(1f + critMOD, Math.max(0f, statsTotal.getPhysicalCrit())));
        triggerChances.put(Trigger.PhysicalCrit, Math.min(1f + critMOD, Math.max(0f, statsTotal.getPhysicalCrit())));
        triggerChances.put(Trigger.DoTTick, 1f);
        triggerChances.put(Trigger.DamageDone, 1f);
        triggerChances.put(Trigger.DamageOrHealingDone, 1f);
        triggerChances.put(Trigger.HunterAutoShotHit, (1f - chanceToMiss));
        triggerChances.put(Trigger.SteadyShotHit, (1f - chanceToMiss));
        triggerChances.put(Trigger.PetClawBiteSmackCrit, Math.min(1f + critMOD, Math.max(0f, calculatedStats.pet.whAtkTable.crit)));
        triggerChances.put(Trigger.SerpentWyvernStingsDoDamage, 1f);
    }

    private Stats getSpecialEffectsStats(Character character,
            Map<Trigger, Float> triggerIntervals, Map<Trigger, Float> triggerChances,
            Stats statsTotal, Stats statsToProcess) {
        CalculationOptionsHunter calcOpts = (CalculationOptionsHunter) character.getCalculationOptions();
        ItemInstance rangeWeap = character.get(CharacterSlot.MainHand);
        float speed = (rangeWeap != null ? rangeWeap.getSpeed() : 2.4f);
        Stats statsProcs = new Stats();
        float fightDuration_M = calcOpts.duration;
        Stats _stats;
        //
        for (SpecialEffect effect : (statsToProcess != null ? statsToProcess.specialEffects() : statsTotal.specialEffects())) {
            float fightDuration = (effect.getStats().getDeathbringerProc() == 1 ? 0f : fightDuration_M);
            float arpToHardCap = StatConversion.RATING_PER_ARMORPENETRATION;
            if (effect.getStats().getArmorPenetrationRating() > 0) {
                float arpenBuffs = 0.0f;
                float currentArp = arpenBuffs + StatConversion.getArmorPenetrationFromRating(statsTotal.getArmorPenetrationRating()
                        + (statsToProcess != null ? statsToProcess.getArmorPenetrationRating() : 0f));
                arpToHardCap *= (1f - currentArp);
            }
            if (triggerIntervals.containsKey(effect.getTrigger()) && (triggerIntervals.get(effect.getTrigger()) > 0f || effect.getTrigger() == Trigger.Use)) {
                float weight = 1f;
                switch (effect.getTrigger()) {
                    case Use: {
                        _stats = new Stats();
                        if (effect.getStats().getRawSpecialEffectDataSize() == 1 && statsToProcess == null) {
                            float uptime = effect.getAverageUptime(0f, 1f, speed, fightDuration);
                            _stats.addSpecialEffect(effect.getStats().getRawSpecialEffectData()[0]);
                            Stats _stats2 = getSpecialEffectsStats(character,
                                    triggerIntervals, triggerChances,
                                    statsTotal, _stats);
                            _stats = Stats.multiply(_stats2, uptime);
                        } else {
                            if (effect.getStats().getArmorPenetrationRating() > 0 && arpToHardCap < effect.getStats().getArmorPenetrationRating()) {
                                float uptime = effect.getAverageUptime(0f, 1f, speed, fightDuration);
                                weight = uptime;
                                _stats.setArmorPenetrationRating(arpToHardCap);
                            } else {
                                _stats = effect.getAverageStats(0f, 1f, speed, fightDuration);
                            }
                        }
                        statsProcs.accumulate(_stats, weight);
                        _stats = null;
                        break;
                    }
                    case RangedHit:
                    case PhysicalHit: {
                        _stats = new Stats();
                        weight = 1.0f;
                        if (effect.getStats().getDeathbringerProc() > 0) {
                            _stats = effect.getAverageStats(triggerIntervals.get(effect.getTrigger()), triggerChances.get(effect.getTrigger()), speed, fightDuration);
                            _stats.setAgility(_stats.getDeathbringerProc());
                            _stats.setCritRating(_stats.getDeathbringerProc());
                            _stats.setAttackPower(_stats.getDeathbringerProc() * 2f);
                            _stats.setDeathbringerProc(0f);
                            weight = 1f / 3f;
                        } else {
                            if (effect.getStats().getArmorPenetrationRating() > 0 && arpToHardCap < effect.getStats().getArmorPenetrationRating()) {
                                float uptime = effect.getAverageUptime(triggerIntervals.get(effect.getTrigger()), triggerChances.get(effect.getTrigger()), speed, fightDuration);
                                weight = uptime;
                                _stats.setArmorPenetrationRating(arpToHardCap);
                            } else {
                                _stats = effect.getAverageStats(triggerIntervals.get(effect.getTrigger()), triggerChances.get(effect.getTrigger()), speed, fightDuration);
                            }
                        }
                        statsProcs.accumulate(_stats, weight);
                        _stats = null;
                        break;
                    }
                    case MeleeHit: // Pets Only
                    case MeleeCrit: // Pets Only
                    case RangedCrit:
                    case PhysicalCrit:
                    case DoTTick:
                    case DamageDone: // physical and dots
                    case DamageOrHealingDone: // physical and dots
                    case HunterAutoShotHit:
                    case SteadyShotHit:
                    case PetClawBiteSmackCrit: {
                        _stats = new Stats();
                        weight = 1.0f;
                        if (effect.getStats().getArmorPenetrationRating() > 0 && arpToHardCap < effect.getStats().getArmorPenetrationRating()) {
                            float uptime = effect.getAverageUptime(triggerIntervals.get(effect.getTrigger()), triggerChances.get(effect.getTrigger()), speed, fightDuration);
                            weight = uptime;
                            _stats.setArmorPenetrationRating(arpToHardCap);
                        } else {
                            _stats = effect.getAverageStats(triggerIntervals.get(effect.getTrigger()), triggerChances.get(effect.getTrigger()), speed, fightDuration);
                        }
                        statsProcs.accumulate(_stats, weight);
                        break;
                    }
                    case SerpentWyvernStingsDoDamage: {
                        _stats = new Stats();
                        weight = 1.0f;
                        if (effect.getStats().getArmorPenetrationRating() > 0 && arpToHardCap < effect.getStats().getArmorPenetrationRating()) {
                            float uptime = effect.getAverageUptime(triggerIntervals.get(effect.getTrigger()), triggerChances.get(effect.getTrigger()), speed, fightDuration);
                            weight = uptime;
                            _stats.setArmorPenetrationRating(arpToHardCap);
                        } else {
                            _stats = effect.getAverageStats(triggerIntervals.get(effect.getTrigger()), triggerChances.get(effect.getTrigger()), speed, fightDuration);
                        }
                        statsProcs.accumulate(_stats, weight);
                        break;
                    }
                    default:
                        break;
                }
            }
        }

        return statsProcs;
    }

    // Private Functions
    private ShotData getShotByIndex(int index, CharacterCalculationsHunter calculatedStats) {
        if (index == 1) return calculatedStats.aimedShot;
        if (index == 2) return calculatedStats.arcaneShot;
        if (index == 3) return calculatedStats.multiShot;
        if (index == 4) return calculatedStats.serpentSting;
        if (index == 5) return calculatedStats.scorpidSting;
        if (index == 6) return calculatedStats.viperSting;
        if (index == 7) return calculatedStats.silencingShot;
        if (index == 8) return calculatedStats.steadyShot;
        if (index == 9) return calculatedStats.killShot;
        if (index == 10) return calculatedStats.explosiveShot;
        if (index == 11) return calculatedStats.blackArrow;
        if (index == 12) return calculatedStats.immolationTrap;
        if (index == 13) return calculatedStats.explosiveTrap;
        if (index == 14) return calculatedStats.freezingTrap;
        if (index == 15) return calculatedStats.frostTrap;
        if (index == 16) return calculatedStats.volley;
        if (index == 17) return calculatedStats.chimeraShot;
        if (index == 18) return calculatedStats.rapidFire;
        if (index == 19) return calculatedStats.readiness;
        if (index == 20) return calculatedStats.bestialWrath;
        return null;
    }

    public static float calcEffectiveDamage(float damageNormal, float missChance, float critChance, float critAdjust, float damageAdjust) {
        float dmg = damageNormal; // Base Damage
        dmg *= damageAdjust;

        // Work the Attack Table
        float dmgDrop = (1f
                - missChance // no damage when being missed
                - critChance // crits   handled below
        );

        float dmgCrit = dmg
                * critChance // MHAtkTable.Crit
                * (1f
                + critAdjust // Bonus Damage when critting
        );

        dmg *= dmgDrop;

        dmg += dmgCrit;

        return dmg;
    }

    public float getArmorDamageReduction(Character character, Stats statS, CalculationOptionsHunter calcOpts, BossOptions bossOpts) {
        float armorReduction;
        float arpenBuffs = 0.0f;

        if (bossOpts == null) {
            armorReduction = Math.max(0f, 1f - StatConversion.getArmorDamageReduction(character.getLevel(), (int) StatConversion.NPC_ARMOR[3], statS.getArmorPenetration(), arpenBuffs, statS.getArmorPenetrationRating())); // default is vs raid boss
        } else {
            armorReduction = Math.max(0f, 1f - StatConversion.getArmorDamageReduction(character.getLevel(), calcOpts.targetArmor, statS.getArmorPenetration(), arpenBuffs, statS.getArmorPenetrationRating()));
        }

        return armorReduction;
    }

    private static CalculationsHunter instance;

    public static CalculationsHunter getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("Hunter", CalculationsHunter.class);
    }

    public static void register() {
        Calculations.registerModel("Hunter", CalculationsHunter.class);
    }

    public CalculationsHunter() {
        instance = this;
    }
}
