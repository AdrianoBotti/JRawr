// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/CalculationsDPSWarr.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// `calcOpts.UseMarkov` runs the ported Markov diagnostic (markov package) when set; the result is
// discarded exactly like the original (only console output), and the oracle fixture
// dpswarr_fury_markov_277 covers it. The Arms generator subtree that the DLL never calls stays
// omitted (DECISIONS #59).
// UI-only members omitted: CalculationOptionsPanel, the System.Drawing Color sub-point palettes
// (labels kept), the DEBUG stopwatch. ErrorBox try/catch wrappers are omitted (exceptions propagate).
// The C# `statType == (StatType.Buffed | StatType.Unbuffed)` trick equals `statType == StatType.Buffed`.
package rawr.models.dpswarr;

import rawr.base.stats.AdditiveStat;
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
import rawr.base.items.Item;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.effects.SpecialDamageProcs;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;
import rawr.base.talents.WarriorTalents;
import rawr.base.stats.WeightedStat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CalculationsDPSWarr extends CalculationsBase {
    public static class DPSWarrCharacter {
        public Character Char;
        public Rotation Rot;
        public CombatFactors combatFactors;
        public CalculationOptionsDPSWarr calcOpts;
        public BossOptions bossOpts;
    }

    private static CalculationsDPSWarr instance;

    public static CalculationsDPSWarr getInstance() {
        return instance;
    }

    public CalculationsDPSWarr() {
        instance = this;
    }

    static {
        Calculations.registerModel("DPSWarr", CalculationsDPSWarr.class);
    }

    public static void register() {
        Calculations.registerModel("DPSWarr", CalculationsDPSWarr.class);
    }

    @Override
    public String getOverallRatingLabel() {
        return "DPS Rating";
    }

    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        // Relevant Gem IDs for DPSWarrs
        //                rare    epic  jewel
        // Red slots
        int[] red_str = { 39996, 40111, 42142 };
        int[] red_arp = { 40002, 40117, 42153 };
        int[] red_exp = { 40003, 40118, 42154 };
        // Blue slots -- All the stat+sta, No haste because str or arp should always be better
        int[] blu_str = { 40022, 40129, 40129 };
        int[] blu_arp = { 40033, 40140, 40140 };
        int[] blu_exp = { 40034, 40141, 40141 };
        int[] blu_hit = { 40088, 40166, 40166 };
        // Yellow slots
        int[] ylw_str = { 40037, 40142, 40142 }; // 10str/10crit
        int[][] ylw_hit = { new int[] { 40014, 40125, 42156 }, // 20hit
                new int[] { 40038, 40143, 40143 }, // 10hit/10str
                new int[] { 40058, 40162, 40162 } }; // 10hit/10exp
        int[] ylw_has = { 40041, 40146, 40146 }; // 10haste/10str

        String group;
        boolean enabled;
        List<GemmingTemplate> templates = new ArrayList<GemmingTemplate>();

        // Strength
        enabled = true;
        group = "Strength";
        // Straight
        addTemplates(templates, red_str, red_str, red_str, red_str, group, enabled);
        // Socket Bonus
        addTemplates(templates, red_str, blu_str, ylw_str, red_str, group, enabled);

        // Armor Pen
        enabled = true;
        group = "ArPen";
        // Straight
        addTemplates(templates, red_arp, red_arp, red_arp, red_arp, group, enabled);
        // Socket Bonus
        addTemplates(templates, red_arp, blu_arp, ylw_str, red_arp, group, enabled);

        // Hit/Exp-gemming
        group = "Hit";
        enabled = false;
        // Hit
        for (int k = 0; k < ylw_hit.length - 1; k++) { // not doing hit/exp here
            // Straight
            addTemplates(templates, ylw_hit[k], ylw_hit[k], ylw_hit[k], ylw_hit[k], group, enabled);
            // Socket Bonus w/Str
            addTemplates(templates, red_str, blu_hit, ylw_hit[k], red_str, group, enabled);
            // Socket Bonus w/Arp
            addTemplates(templates, red_arp, blu_hit, ylw_hit[k], red_arp, group, enabled);
        }
        // Exp
        group = "Expertise";
        enabled = false;
        // Straight
        addTemplates(templates, red_exp, red_exp, red_exp, red_exp, group, enabled);
        // Socket Bonus
        addTemplates(templates, red_exp, blu_exp, ylw_hit[2], red_exp, group, enabled);

        // Crit-capped
        group = "Crit-capped";
        enabled = false;
        // Strength
        addTemplates(templates, red_str, blu_str, ylw_has, red_str, group, enabled);
        // ArP
        addTemplates(templates, red_arp, blu_arp, ylw_has, red_arp, group, enabled);

        templates.sort(new Comparator<GemmingTemplate>() {
            @Override
            public int compare(GemmingTemplate first, GemmingTemplate second) {
                char[] splitters = { ' ' };
                String[] group1 = splitNoEmpty(first.getGroup(), splitters);
                String[] group2 = splitNoEmpty(second.getGroup(), splitters);
                int temp = group1[0].compareTo(group2[0]);
                if (temp != 0) {
                    if (group1[0].equals("Rare")) return -1;
                    if (group2[0].equals("Rare")) return 1;
                    if (group1[0].equals("Jewelcrafter")) return 1;
                    return -1;
                } else {
                    temp = group1[1].compareTo(group2[1]);
                    if (temp != 0) {
                        switch (group1[1]) {
                            case "Strength": return -1;
                            case "ArPen": return (group2[1].equals("Strength") ? 1 : -1);
                            case "Hit": return (group2[1].equals("Strength") || group2[1].equals("ArPen") ? 1 : -1);
                            case "Expertise": return (group2[1].equals("Crit-capped") ? -1 : 1);
                            default: return 1;
                        }
                    } else {
                        int val = Integer.compare(first.getRedId(), second.getRedId());
                        if (val != 0) return val;
                        val = Integer.compare(first.getYellowId(), second.getYellowId());
                        if (val != 0) return val;
                        val = Integer.compare(first.getBlueId(), second.getBlueId());
                        if (val != 0) return val;
                        return Integer.compare(first.getMetaId(), second.getMetaId());
                    }
                }
            }
        });

        return templates;
    }

    private static String[] splitNoEmpty(String value, char[] splitters) {
        List<String> parts = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean isSplitter = false;
            for (char s : splitters) if (c == s) { isSplitter = true; break; }
            if (isSplitter) {
                if (sb.length() > 0) {
                    parts.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) parts.add(sb.toString());
        return parts.toArray(new String[0]);
    }

    private static void addTemplates(List<GemmingTemplate> templates, int[] red, int[] blu, int[] ylw, int[] pris, String group, boolean enabled) {
        // Meta
        final int chaotic = 41285;
        final int relent = 41398;
        final int nightmare = 49110;
        String[] quality = new String[] { "Rare", "Epic", "Jewelcrafter" };
        for (int j = 0; j < 3; j++) {
            // Check to make sure we're not adding the same gem template twice due to repeating JC gems
            if (j != 2 || !(red[j] == red[j - 1] && blu[j] == blu[j - 1] && ylw[j] == ylw[j - 1])) {
                String groupStr = quality[j] + " " + group;
                templates.add(gemmingTemplate(groupStr, (enabled && j == 1), red[j], ylw[j], blu[j], pris[j], chaotic));
                templates.add(gemmingTemplate(groupStr, (enabled && j == 1), red[j], ylw[j], blu[j], pris[j], relent));
                // Nightmare tear, only when not going for the socket bonus
                if (red[j] != blu[j] && blu[j] != ylw[j]) {
                    templates.add(gemmingTemplate(groupStr, (enabled && j == 1), red[j], ylw[j], nightmare, pris[j], chaotic));
                    templates.add(gemmingTemplate(groupStr, (enabled && j == 1), red[j], ylw[j], nightmare, pris[j], relent));
                }
            }
        }
    }

    private static GemmingTemplate gemmingTemplate(String group, boolean enabled, int redId, int yellowId, int blueId, int prismaticId, int metaId) {
        GemmingTemplate template = new GemmingTemplate();
        template.setModel("DPSWarr");
        template.setGroup(group);
        template.setEnabled(enabled);
        template.setRedId(redId);
        template.setYellowId(yellowId);
        template.setBlueId(blueId);
        template.setPrismaticId(prismaticId);
        template.setMetaId(metaId);
        return template;
    }

    private static final String[] CharacterDisplayCalculationLabels = new String[] {
            "Base Stats:Health and Stamina",
            "Base Stats:Armor",
            "Base Stats:Strength",
            "Base Stats:Attack Power",
            "Base Stats:Agility",
            "Base Stats:Crit",
            "Base Stats:Haste",
            "Base Stats:Armor Penetration*Rating to Cap with bonuses applied\n(but not trinkets)\n1400-None\n1261-Battle(140)\n1177-Battle(140)+T92P(084)\n1051-Battle(140)+Mace(210)\n0967-Battle(140)+T92P(084)+Mace(210)",
            "Base Stats:Hit*8.00% chance to miss base for Yellow Attacks (LVL 83 Targ)\nPrecision 0- 8%-0%=8%=264 Rating soft cap\nPrecision 1- 8%-1%=7%=230 Rating soft cap\nPrecision 2- 8%-2%=6%=197 Rating soft cap\nPrecision 3- 8%-3%=5%=164 Rating soft cap",
            "Base Stats:Expertise*Base 6.50% chance to be Dodged (LVL 83 Targ)\nX Axis is Weapon Mastery\nY Axis is Strength of Arms\nx>| 0  |  1  |  2\n0 |213|180|147\n1 |197|164|131\n2 |180|147|115\n\n0/2 in each the cap is 213 Rating\n2/2 in each the cap is 115 Rating\n\nBase 13.75% chance to be Parried (LVL 83 Targ)\nStrength of Arms\n0 |459\n1 |443\n2 |426\n\nThese numbers to do not include racial bonuses.",
            "DPS Breakdown (Fury):Description*1st Number is per second or per tick\n2nd Number is the average damage (factoring mitigation, hit/miss ratio and crits) per hit\n3rd Number is number of times activated over fight duration",
            "DPS Breakdown (Fury):Bloodsurge",
            "DPS Breakdown (Fury):Bloodthirst",
            "DPS Breakdown (Fury):Whirlwind",
            "DPS Breakdown (Arms):Bladestorm*Bladestorm only uses 1 GCD to activate but it is channeled for a total of 4 GCD's",
            "DPS Breakdown (Arms):Mortal Strike",
            "DPS Breakdown (Arms):Rend",
            "DPS Breakdown (Arms):Overpower",
            "DPS Breakdown (Arms):Taste for Blood*Perform an Overpower",
            "DPS Breakdown (Arms):Sudden Death*Perform an Execute",
            "DPS Breakdown (Arms):Slam*If this number is zero, it most likely means that your other abilities are proc'g often enough that you are rarely, if ever, having to resort to Slamming your target.",
            "DPS Breakdown (Arms):Sword Specialization",
            "DPS Breakdown (Maintenance):Thunder Clap",
            "DPS Breakdown (Maintenance):Shattering Throw",
            "DPS Breakdown (General):Deep Wounds",
            "DPS Breakdown (General):Heroic Strike",
            "DPS Breakdown (General):Cleave",
            "DPS Breakdown (General):White DPS",
            "DPS Breakdown (General):Execute*<20% Spamming only",
            "DPS Breakdown (General):Special DMG Procs*Such as Bandit's Insignia or Hand Mounted Pyro Rocket",
            "DPS Breakdown (General):Total DPS*1st number is total DPS\n2nd number is total DMG over Duration",
            "Rage Details:Total Generated Rage",
            "Rage Details:Needed Rage for Abilities",
            "Rage Details:Available Free Rage*For Heroic Strikes and Cleaves",
    };

    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        return java.util.Arrays.asList(CharacterDisplayCalculationLabels);
    }

    private static final String[] OptimizableCalculationLabels = new String[] {
            "Health",
            "Armor",
            "Strength",
            "Attack Power",
            "Agility",
            "Crit %",
            "Haste %",
            "ArP %",
            "% Chance to Miss (White)",
            "% Chance to Miss (Yellow)",
            "% Chance to be Dodged",
            "% Chance to be Parried",
            "% Chance to be Avoided (Yellow/Dodge)",
    };

    public String[] getOptimizableCalculationLabels() {
        return OptimizableCalculationLabels;
    }

    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> colors = new LinkedHashMap<String, String>();
        colors.put("DPS", "#FFFF0000");
        colors.put("Survivability", "#FF408020");
        return colors;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationDPSWarr();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsDPSWarr();
    }

    @Override
    public rawr.base.calculations.ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            CalculationOptionsDPSWarr calcOpts = new CalculationOptionsDPSWarr();
            org.w3c.dom.Element root = document.getDocumentElement();
            for (org.w3c.dom.Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                org.w3c.dom.Element e = (org.w3c.dom.Element) n;
                String name = e.getTagName();
                String text = e.getTextContent() == null ? "" : e.getTextContent().trim();
                switch (name) {
                    case "AllowFlooring": calcOpts.setAllowFlooring(Boolean.parseBoolean(text)); break;
                    case "SE_UseDur": calcOpts.setSE_UseDur(Boolean.parseBoolean(text)); break;
                    case "UseMarkov": calcOpts.setUseMarkov(Boolean.parseBoolean(text)); break;
                    case "PTRMode": calcOpts.setPTRMode(Boolean.parseBoolean(text)); break;
                    case "HideBadItems_Def": calcOpts.setHideBadItems_Def(Boolean.parseBoolean(text)); break;
                    case "HideBadItems_Spl": calcOpts.setHideBadItems_Spl(Boolean.parseBoolean(text)); break;
                    case "HideBadItems_PvP": calcOpts.setHideBadItems_PvP(Boolean.parseBoolean(text)); break;
                    case "SurvScale": calcOpts.setSurvScale(Float.parseFloat(text)); break;
                    case "StatsList": calcOpts.setStatsList(readBooleanArray(e)); break;
                    case "StatsIncrement": calcOpts.setStatsIncrement(Integer.parseInt(text)); break;
                    case "CalculationToGraph": calcOpts.setCalculationToGraph(text); break;
                    case "Maintenance": calcOpts.setMaintenance(readBooleanArray(e)); break;
                    case "Lag": calcOpts.setLag(Float.parseFloat(text)); break;
                    case "React": calcOpts.setReact(Float.parseFloat(text)); break;
                    case "FilterType": calcOpts.setFilterType(text); break;
                    case "Filter": calcOpts.setFilter(text); break;
                    case "BossName": calcOpts.setBossName(text); break;
                    case "TargetLevel": calcOpts.setTargetLevel(Integer.parseInt(text)); break;
                    case "TargetArmor": calcOpts.setTargetArmor(Float.parseFloat(text)); break;
                    case "TargetHP": calcOpts.setTargetHP(Float.parseFloat(text)); break;
                    case "Duration": calcOpts.setDuration(Float.parseFloat(text)); break;
                    case "Under20Perc": calcOpts.setUnder20Perc(Float.parseFloat(text)); break;
                    case "InBack": calcOpts.setInBack(Boolean.parseBoolean(text)); break;
                    case "InBackPerc": calcOpts.setInBackPerc(Integer.parseInt(text)); break;
                    case "MultipleTargets": calcOpts.setMultipleTargets(Boolean.parseBoolean(text)); break;
                    case "MultipleTargetsPerc": calcOpts.setMultipleTargetsPerc(Integer.parseInt(text)); break;
                    case "MultipleTargetsMax": calcOpts.setMultipleTargetsMax(Float.parseFloat(text)); break;
                    case "MovingTargets": calcOpts.setMovingTargets(Boolean.parseBoolean(text)); break;
                    case "StunningTargets": calcOpts.setStunningTargets(Boolean.parseBoolean(text)); break;
                    case "FearingTargets": calcOpts.setFearingTargets(Boolean.parseBoolean(text)); break;
                    case "RootingTargets": calcOpts.setRootingTargets(Boolean.parseBoolean(text)); break;
                    case "DisarmingTargets": calcOpts.setDisarmingTargets(Boolean.parseBoolean(text)); break;
                    case "AoETargets": calcOpts.setAoETargets(Boolean.parseBoolean(text)); break;
                    case "AoETargetsFreq": calcOpts.setAoETargetsFreq(Integer.parseInt(text)); break;
                    case "AoETargetsDMG": calcOpts.setAoETargetsDMG(Float.parseFloat(text)); break;
                    case "Stuns": calcOpts.setStuns(readImpedances(e)); break;
                    case "Moves": calcOpts.setMoves(readImpedances(e)); break;
                    case "Fears": calcOpts.setFears(readImpedances(e)); break;
                    case "Roots": calcOpts.setRoots(readImpedances(e)); break;
                    case "Disarms": calcOpts.setDisarms(readImpedances(e)); break;
                    default: break;
                }
            }
            return calcOpts;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CalculationOptionsDPSWarr XML", e);
        }
    }

    private static boolean[] readBooleanArray(org.w3c.dom.Element parent) {
        List<Boolean> values = new ArrayList<Boolean>();
        for (org.w3c.dom.Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
            org.w3c.dom.Element e = (org.w3c.dom.Element) n;
            if (e.getTagName().equals("boolean")) {
                values.add(Boolean.parseBoolean(e.getTextContent().trim()));
            }
        }
        boolean[] result = new boolean[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = values.get(i);
        return result;
    }

    private static List<rawr.base.bosses.Impedance> readImpedances(org.w3c.dom.Element parent) {
        List<rawr.base.bosses.Impedance> list = new ArrayList<rawr.base.bosses.Impedance>();
        for (org.w3c.dom.Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
            org.w3c.dom.Element e = (org.w3c.dom.Element) n;
            if (!e.getTagName().equals("Impedance")) continue;
            rawr.base.bosses.Impedance imp = new rawr.base.bosses.Impedance();
            for (org.w3c.dom.Node c = e.getFirstChild(); c != null; c = c.getNextSibling()) {
                if (c.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                org.w3c.dom.Element ce = (org.w3c.dom.Element) c;
                String text = ce.getTextContent() == null ? "" : ce.getTextContent().trim();
                switch (ce.getTagName()) {
                    case "Frequency": imp.frequency = Float.parseFloat(text); break;
                    case "Duration": imp.duration = Float.parseFloat(text); break;
                    case "Chance": imp.chance = Float.parseFloat(text); break;
                    case "Breakable": imp.breakable = Boolean.parseBoolean(text); break;
                    default: break;
                }
            }
            list.add(imp);
        }
        return list;
    }

    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.Warrior;
    }

    private static final List<ItemType> RelevantItemTypes = List.of(
            ItemType.None,
            ItemType.Leather,
            ItemType.Mail,
            ItemType.Plate,
            ItemType.Bow,
            ItemType.Crossbow,
            ItemType.Gun,
            ItemType.Thrown,
            ItemType.Dagger,
            ItemType.FistWeapon,
            ItemType.OneHandMace,
            ItemType.OneHandSword,
            ItemType.OneHandAxe,
            ItemType.Polearm,
            ItemType.TwoHandMace,
            ItemType.TwoHandSword,
            ItemType.TwoHandAxe
    );

    @Override
    public List<ItemType> getRelevantItemTypes() {
        return RelevantItemTypes;
    }

    @Override
    public boolean enchantFitsInSlot(Enchant enchant, Character character, ItemSlot slot) {
        // Hide the ranged weapon enchants. None of them apply to melee damage at all.
        if (enchant.slot == ItemSlot.Ranged) { return false; }
        // Disallow Shield enchants, all shield enchants are ItemSlot.OffHand and nothing else is according to Astry
        if (enchant.slot == ItemSlot.OffHand) { return false; }
        // Allow offhand Enchants for two-handers if toon has Titan's Grip
        // If not, turn off all enchants for the offhand
        if (character != null
                && character.getWarriorTalents().getTitansGrip() > 0
                && enchant.slot == ItemSlot.TwoHand
                && slot == ItemSlot.OffHand) {
            return true;
        } else if (character != null
                && character.getWarriorTalents().getTitansGrip() == 0
                && (enchant.slot == ItemSlot.TwoHand || enchant.slot == ItemSlot.OneHand)
                && slot == ItemSlot.OffHand) {
            return false;
        }
        // If all the above is ok, return base version
        return enchant.fitsInSlot(slot);
    }

    @Override
    public boolean itemFitsInSlot(Item item, Character character, CharacterSlot slot, boolean ignoreUnique) {
        // We need specilized handling due to Titan's Grip
        if (item == null || character == null) {
            return false;
        }

        // Covers all TG weapons
        if (character.getWarriorTalents().getTitansGrip() > 0) {
            // Polearm can't go in OH, can't go in MH if there's an OH, but can go in MH if there's no OH
            if (item.getType() == ItemType.Polearm) {
                if (slot == CharacterSlot.OffHand || character.getOffHand() != null) return false;
                if (slot == CharacterSlot.MainHand) return true;
                return false;
            }
            // If there's a polearm in the MH, nothing can go in OH
            if (slot == CharacterSlot.OffHand && character.getMainHand() != null && character.getMainHand().getItem() != null
                    && character.getMainHand().getItem().getType() == ItemType.Polearm) {
                return false;
            }
            // Else, if it's a 2h weapon it can go in OH or MH
            if (item.getSlot() == ItemSlot.TwoHand && (slot == CharacterSlot.OffHand || slot == CharacterSlot.MainHand)) return true;
        }

        // Not TG, so can't dual-wield with a 2H in the MH
        if (slot == CharacterSlot.OffHand && character.getMainHand() != null && character.getMainHand().getItem() != null
                && character.getMainHand().getItem().getSlot() == ItemSlot.TwoHand) {
            return false;
        }

        return super.itemFitsInSlot(item, character, slot, ignoreUnique);
    }

    private static List<String> relevantGlyphs = null;

    public List<String> getRelevantGlyphs() {
        if (relevantGlyphs == null) {
            relevantGlyphs = new ArrayList<String>();
            // ===== MAJOR GLYPHS =====
            relevantGlyphs.add("Glyph of Bladestorm");
            relevantGlyphs.add("Glyph of Bloodthirst");
            relevantGlyphs.add("Glyph of Cleaving");
            relevantGlyphs.add("Glyph of Enraged Regeneration");
            relevantGlyphs.add("Glyph of Execution");
            relevantGlyphs.add("Glyph of Hamstring");
            relevantGlyphs.add("Glyph of Heroic Strike");
            relevantGlyphs.add("Glyph of Mortal Strike");
            relevantGlyphs.add("Glyph of Overpower");
            relevantGlyphs.add("Glyph of Rapid Charge");
            relevantGlyphs.add("Glyph of Rending");
            relevantGlyphs.add("Glyph of Resonating Power");
            relevantGlyphs.add("Glyph of Sweeping Strikes");
            relevantGlyphs.add("Glyph of Victory Rush");
            relevantGlyphs.add("Glyph of Whirlwind");
            // ===== MINOR GLYPHS =====
            relevantGlyphs.add("Glyph of Battle");
            relevantGlyphs.add("Glyph of Bloodrage");
            relevantGlyphs.add("Glyph of Charge");
            relevantGlyphs.add("Glyph of Enduring Victory");
            relevantGlyphs.add("Glyph of Thunder Clap");
            relevantGlyphs.add("Glyph of Command");
        }
        return relevantGlyphs;
    }

    private static boolean HidingBadStuff() {
        return HidingBadStuff_Def || HidingBadStuff_Spl || HidingBadStuff_PvP;
    }

    static boolean HidingBadStuff_Def = false;
    static boolean HidingBadStuff_Spl = false;
    static boolean HidingBadStuff_PvP = false;

    private static List<Trigger> RelevantTriggers = null;

    static List<Trigger> getRelevantTriggers() {
        if (RelevantTriggers == null) {
            RelevantTriggers = new ArrayList<Trigger>();
            RelevantTriggers.add(Trigger.Use);
            RelevantTriggers.add(Trigger.MeleeCrit);
            RelevantTriggers.add(Trigger.MeleeHit);
            RelevantTriggers.add(Trigger.PhysicalCrit);
            RelevantTriggers.add(Trigger.PhysicalHit);
            RelevantTriggers.add(Trigger.DoTTick);
            RelevantTriggers.add(Trigger.DamageDone);
            RelevantTriggers.add(Trigger.DamageTaken);
            RelevantTriggers.add(Trigger.DamageAvoided);
            RelevantTriggers.add(Trigger.HSorSLHit);
            RelevantTriggers.add(Trigger.DamageOrHealingDone);
        }
        return RelevantTriggers;
    }

    public Stats getRelevantStats(Stats stats) {
        Stats relevantStats = new Stats();
        // Base Stats
        relevantStats.setStamina(stats.getStamina());
        relevantStats.setHealth(stats.getHealth());
        relevantStats.setAgility(stats.getAgility());
        relevantStats.setStrength(stats.getStrength());
        relevantStats.setAttackPower(stats.getAttackPower());
        relevantStats.setArmor(stats.getArmor());
        // Ratings
        relevantStats.setCritRating(stats.getCritRating());
        relevantStats.setHitRating(stats.getHitRating());
        relevantStats.setHasteRating(stats.getHasteRating());
        relevantStats.setExpertiseRating(stats.getExpertiseRating());
        relevantStats.setArmorPenetrationRating(stats.getArmorPenetrationRating());
        relevantStats.setResilience(stats.getResilience());
        // Bonuses
        relevantStats.setBonusArmor(stats.getBonusArmor());
        relevantStats.setWeaponDamage(stats.getWeaponDamage());
        relevantStats.setArmorPenetration(stats.getArmorPenetration());
        relevantStats.setPhysicalCrit(stats.getPhysicalCrit());
        relevantStats.setPhysicalHaste(stats.getPhysicalHaste());
        relevantStats.setPhysicalHit(stats.getPhysicalHit());
        relevantStats.setSpellHit(stats.getSpellHit());
        relevantStats.setMovementSpeed(stats.getMovementSpeed());
        relevantStats.setStunDurReduc(stats.getStunDurReduc());
        relevantStats.setSnareRootDurReduc(stats.getSnareRootDurReduc());
        relevantStats.setFearDurReduc(stats.getFearDurReduc());
        // Target Debuffs
        relevantStats.setBossAttackPower(stats.getBossAttackPower());
        relevantStats.setBossAttackSpeedMultiplier(stats.getBossAttackSpeedMultiplier());
        // Procs
        relevantStats.setDarkmoonCardDeathProc(stats.getDarkmoonCardDeathProc());
        relevantStats.setHighestStat(stats.getHighestStat());
        relevantStats.setParagon(stats.getParagon());
        relevantStats.setDeathbringerProc(stats.getDeathbringerProc());
        relevantStats.setManaorEquivRestore(stats.getManaorEquivRestore());
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
        // Multipliers
        relevantStats.setBonusStaminaMultiplier(stats.getBonusStaminaMultiplier());
        relevantStats.setBonusHealthMultiplier(stats.getBonusHealthMultiplier());
        relevantStats.setBonusAgilityMultiplier(stats.getBonusAgilityMultiplier());
        relevantStats.setBonusStrengthMultiplier(stats.getBonusStrengthMultiplier());
        relevantStats.setBonusAttackPowerMultiplier(stats.getBonusAttackPowerMultiplier());
        relevantStats.setBonusBleedDamageMultiplier(stats.getBonusBleedDamageMultiplier());
        relevantStats.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());
        relevantStats.setDamageTakenMultiplier(stats.getDamageTakenMultiplier());
        relevantStats.setBonusPhysicalDamageMultiplier(stats.getBonusPhysicalDamageMultiplier());
        relevantStats.setBonusCritMultiplier(stats.getBonusCritMultiplier());
        relevantStats.setBonusCritChance(stats.getBonusCritChance());
        relevantStats.setBaseArmorMultiplier(stats.getBaseArmorMultiplier());
        relevantStats.setBonusArmorMultiplier(stats.getBonusArmorMultiplier());
        // Set Bonuses
        relevantStats.setBonusWarrior_T7_2P_SlamDamage(stats.getBonusWarrior_T7_2P_SlamDamage());
        relevantStats.setBonusWarrior_T7_4P_RageProc(stats.getBonusWarrior_T7_4P_RageProc());
        relevantStats.setBonusWarrior_T8_2P_HasteProc(stats.getBonusWarrior_T8_2P_HasteProc());
        relevantStats.setBonusWarrior_T8_4P_MSBTCritIncrease(stats.getBonusWarrior_T8_4P_MSBTCritIncrease());
        relevantStats.setBonusWarrior_T9_2P_Crit(stats.getBonusWarrior_T9_2P_Crit());
        relevantStats.setBonusWarrior_T9_2P_ArP(stats.getBonusWarrior_T9_2P_ArP());
        relevantStats.setBonusWarrior_T9_4P_SLHSCritIncrease(stats.getBonusWarrior_T9_4P_SLHSCritIncrease());
        relevantStats.setBonusWarrior_T10_2P_DWAPProc(stats.getBonusWarrior_T10_2P_DWAPProc());
        relevantStats.setBonusWarrior_T10_4P_BSSDProcChange(stats.getBonusWarrior_T10_4P_BSSDProcChange());
        relevantStats.setBonusWarrior_PvP_4P_InterceptCDReduc(stats.getBonusWarrior_PvP_4P_InterceptCDReduc());
        // Special
        relevantStats.setBonusRageGen(stats.getBonusRageGen());
        relevantStats.setBonusRageOnCrit(stats.getBonusRageOnCrit());
        relevantStats.setHealthRestore(stats.getHealthRestore());
        relevantStats.setHealthRestoreFromMaxHealth(stats.getHealthRestoreFromMaxHealth());
        relevantStats.setBonusHealingDoneMultiplier(stats.getBonusHealingDoneMultiplier());
        for (SpecialEffect effect : stats.specialEffects()) {
            if (getRelevantTriggers().contains(effect.getTrigger()) && (hasRelevantStats(effect.getStats()) || hasSurvivabilityStats(effect.getStats()))) {
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
        boolean relevant = (
                // Base Stats
                stats.getAgility() +
                        stats.getStrength() +
                        stats.getAttackPower() +
                        stats.getArmor() +
                        // Ratings
                        stats.getCritRating() +
                        stats.getHitRating() +
                        stats.getHasteRating() +
                        stats.getExpertiseRating() +
                        stats.getArmorPenetrationRating() +
                        stats.getResilience() +
                        // Bonuses
                        stats.getBonusArmor() +
                        stats.getWeaponDamage() +
                        stats.getArmorPenetration() +
                        stats.getPhysicalCrit() +
                        stats.getPhysicalHaste() +
                        stats.getPhysicalHit() +
                        stats.getSpellHit() + // used for TClap/Demo Shout maintenance
                        stats.getMovementSpeed() +
                        stats.getStunDurReduc() +
                        stats.getSnareRootDurReduc() +
                        stats.getFearDurReduc() +
                        // Target Debuffs
                        stats.getBossAttackPower() +
                        stats.getBossAttackSpeedMultiplier() +
                        // Procs
                        stats.getDarkmoonCardDeathProc() +
                        stats.getHighestStat() +
                        stats.getParagon() +
                        stats.getDeathbringerProc() +
                        stats.getManaorEquivRestore() +
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
                        // Multipliers
                        stats.getBonusAgilityMultiplier() +
                        stats.getBonusStrengthMultiplier() +
                        stats.getBonusAttackPowerMultiplier() +
                        stats.getBonusBleedDamageMultiplier() +
                        stats.getBonusDamageMultiplier() +
                        stats.getDamageTakenMultiplier() +
                        stats.getBonusPhysicalDamageMultiplier() +
                        stats.getBonusCritMultiplier() +
                        stats.getBonusCritChance() +
                        stats.getBaseArmorMultiplier() +
                        stats.getBonusArmorMultiplier() +
                        // Set Bonuses
                        stats.getBonusWarrior_T7_2P_SlamDamage() +
                        stats.getBonusWarrior_T7_4P_RageProc() +
                        stats.getBonusWarrior_T8_2P_HasteProc() +
                        stats.getBonusWarrior_T8_4P_MSBTCritIncrease() +
                        stats.getBonusWarrior_T9_2P_Crit() +
                        stats.getBonusWarrior_T9_2P_ArP() +
                        stats.getBonusWarrior_T9_4P_SLHSCritIncrease() +
                        stats.getBonusWarrior_T10_2P_DWAPProc() +
                        stats.getBonusWarrior_T10_4P_BSSDProcChange() +
                        stats.getBonusWarrior_PvP_4P_InterceptCDReduc() +
                        // Special
                        stats.getBonusRageGen() +
                        stats.getBonusRageOnCrit()
        ) != 0;
        for (SpecialEffect effect : stats.specialEffects()) {
            if (getRelevantTriggers().contains(effect.getTrigger())) {
                relevant |= hasWantedStats(effect.getStats());
            }
            if (relevant) break;
        }
        return relevant;
    }

    private boolean hasSurvivabilityStats(Stats stats) {
        boolean relevant = false;
        if ((stats.getHealth()
                + stats.getStamina()
                + stats.getBonusHealthMultiplier()
                + stats.getBonusStaminaMultiplier()
                + stats.getHealthRestore()
                + stats.getHealthRestoreFromMaxHealth()
        ) > 0) {
            relevant = true;
        }
        for (SpecialEffect effect : stats.specialEffects()) {
            if (getRelevantTriggers().contains(effect.getTrigger())) {
                relevant |= hasSurvivabilityStats(effect.getStats());
            }
            if (relevant) break;
        }
        return relevant;
    }

    private boolean hasIgnoreStats(Stats stats) {
        if (!HidingBadStuff()) { return false; }
        boolean retVal = false;
        retVal = (
                // Remove Spellcasting Stuff
                (HidingBadStuff_Spl ? stats.getMp5() + stats.getSpellPower() + stats.getMana() + stats.getManaRestore() + stats.getSpirit()
                        + stats.getBonusSpiritMultiplier() + stats.getBonusIntellectMultiplier()
                        + stats.getSpellPenetration() + stats.getBonusManaMultiplier()
                        : 0f)
                        // Remove Defensive Stuff (until we do that special modelling)
                        + (HidingBadStuff_Def ? stats.getDefenseRating() + stats.getDefense() + stats.getDodge() + stats.getParry()
                        + stats.getDodgeRating() + stats.getParryRating() + stats.getBlockRating() + stats.getBlock()
                        : 0f)
                        // Remove PvP Items
                        + (HidingBadStuff_PvP ? stats.getResilience() : 0f)
        ) > 0;
        for (SpecialEffect effect : stats.specialEffects()) {
            retVal |= !getRelevantTriggers().contains(effect.getTrigger());
            retVal |= hasIgnoreStats(effect.getStats());
            if (retVal) break;
        }

        return retVal;
    }

    @Override
    public boolean isItemRelevant(Item item) {
        if ( // Manual override for +X to all Stats gems
                item.getName().equals("Nightmare Tear")
                        || item.getName().equals("Enchanted Tear")
                        || item.getName().equals("Enchanted Pearl")
                        || item.getName().equals("Chromatic Sphere")
        ) {
            return true;
        } else {
            Stats stats = item.getStats();
            boolean wantedStats = hasWantedStats(stats);
            boolean survstats = hasSurvivabilityStats(stats);
            boolean ignoreStats = hasIgnoreStats(stats);
            return (wantedStats || survstats) && !ignoreStats && super.isItemRelevant(item);
        }
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
                || name.contains("Insane Strength Potion")
        ) {
            return true;
        }
        // Force some buffs to go away
        else if (!buff.getAllowedClasses().contains(CharacterClass.Warrior)) {
            return false;
        }
        boolean haswantedStats = hasWantedStats(buff.stats);
        boolean hassurvStats = hasSurvivabilityStats(buff.stats);
        boolean hasbadstats = hasIgnoreStats(buff.stats);
        boolean retVal = haswantedStats || (hassurvStats && !hasbadstats);
        return retVal;
    }

    public Stats getBuffsStats(Character character, CalculationOptionsDPSWarr calcOpts) {
        List<Buff> removedBuffs = new ArrayList<Buff>();
        List<Buff> addedBuffs = new ArrayList<Buff>();

        List<Buff> buffGroup = new ArrayList<Buff>();
        // Maintenance Auto-Fixing
        // Removes the Sunder Armor if you are maintaining it yourself
        if (calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.SunderArmor_.index]) {
            buffGroup.clear();
            buffGroup.add(Buff.getBuffByName("Sunder Armor"));
            buffGroup.add(Buff.getBuffByName("Acid Spit"));
            buffGroup.add(Buff.getBuffByName("Expose Armor"));
            maintBuffHelper(buffGroup, character, removedBuffs);
        }

        // Removes the Shattering Throw Buff if you are maintaining it yourself
        if (calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.ShatteringThrow_.index]) {
            buffGroup.clear();
            buffGroup.add(Buff.getBuffByName("Shattering Throw"));
            maintBuffHelper(buffGroup, character, removedBuffs);
        }

        // Removes the Thunder Clap & Improved Buffs if you are maintaining it yourself
        if (calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.ThunderClap_.index]) {
            buffGroup.clear();
            buffGroup.add(Buff.getBuffByName("Thunder Clap"));
            buffGroup.add(Buff.getBuffByName("Improved Thunder Clap"));
            buffGroup.add(Buff.getBuffByName("Judgements of the Just"));
            buffGroup.add(Buff.getBuffByName("Infected Wounds"));
            buffGroup.add(Buff.getBuffByName("Frost Fever"));
            buffGroup.add(Buff.getBuffByName("Improved Icy Touch"));
            maintBuffHelper(buffGroup, character, removedBuffs);
        }

        // Removes the Demoralizing Shout & Improved Buffs if you are maintaining it yourself
        if (calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.DemoralizingShout_.index]) {
            buffGroup.clear();
            buffGroup.add(Buff.getBuffByName("Demoralizing Shout"));
            buffGroup.add(Buff.getBuffByName("Improved Demoralizing Shout"));
            maintBuffHelper(buffGroup, character, removedBuffs);
        }

        // Removes the Battle Shout & Commanding Presence Buffs if you are maintaining it yourself
        if (calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.BattleShout_.index]) {
            buffGroup.clear();
            buffGroup.add(Buff.getBuffByName("Commanding Presence (Attack Power)"));
            buffGroup.add(Buff.getBuffByName("Battle Shout"));
            buffGroup.add(Buff.getBuffByName("Improved Blessing of Might"));
            buffGroup.add(Buff.getBuffByName("Blessing of Might"));
            maintBuffHelper(buffGroup, character, removedBuffs);
        }

        // Removes the Commanding Shout & Commanding Presence Buffs if you are maintaining it yourself
        if (calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.CommandingShout_.index]) {
            buffGroup.clear();
            buffGroup.add(Buff.getBuffByName("Commanding Presence (Health)"));
            buffGroup.add(Buff.getBuffByName("Commanding Shout"));
            buffGroup.add(Buff.getBuffByName("Improved Imp"));
            buffGroup.add(Buff.getBuffByName("Blood Pact"));
            maintBuffHelper(buffGroup, character, removedBuffs);
        }

        // Passive Ability Auto-Fixing
        // Removes the Trauma Buff and it's equivalent Mangle if you are maintaining it yourself
        if (character.getWarriorTalents().getTrauma() > 0) {
            buffGroup.clear();
            buffGroup.add(Buff.getBuffByName("Trauma"));
            buffGroup.add(Buff.getBuffByName("Mangle"));
            maintBuffHelper(buffGroup, character, removedBuffs);
        }

        // Removes the Blood Frenzy Buff and it's equivalent of Savage Combat if you are maintaining it yourself
        if (character.getWarriorTalents().getBloodFrenzy() > 0) {
            buffGroup.clear();
            buffGroup.add(Buff.getBuffByName("Blood Frenzy"));
            buffGroup.add(Buff.getBuffByName("Savage Combat"));
            maintBuffHelper(buffGroup, character, removedBuffs);
        }

        // Removes the Rampage Buff and it's equivalent of Leader of the Pack if you are maintaining it yourself
        if (character.getWarriorTalents().getRampage() > 0) {
            buffGroup.clear();
            buffGroup.add(Buff.getBuffByName("Rampage"));
            buffGroup.add(Buff.getBuffByName("Leader of the Pack"));
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
            if (character.getActiveBuffs().remove(b)) { removedBuffs.add(b); }
        }
    }

    public void setDefaults(Character character) {
        // The original body is entirely commented out in C#.
    }

    @Override
    public boolean includeOffHandInCalculations(Character character) {
        if (character.getOffHand() == null) { return false; }
        if (character.getCurrentTalents() instanceof WarriorTalents) {
            WarriorTalents talents = (WarriorTalents) character.getCurrentTalents();
            if (talents.getTitansGrip() > 0) {
                return true;
            } else {
                return super.includeOffHandInCalculations(character);
            }
        }
        // C# also handles WarriorTalentsCata here; that tree is not wired into the Java Character.
        return false;
    }

    // Special Comparison Charts
    private static final String[] CustomChartNames = new String[] {
            "Ability DPS+Damage",
            "Rage Cost per Damage",
            "Execute Spam",
    };

    public String[] getCustomChartNames() {
        return CustomChartNames;
    }

    float getDPS(DPSWarrCharacter dpswarchar, int Iter, boolean with) {
        dpswarchar.calcOpts.getMaintenance()[Iter] = with;
        CharacterCalculationsDPSWarr calculations = (CharacterCalculationsDPSWarr) getCharacterCalculations(dpswarchar.Char.clone());
        return calculations.getTotalDPS();
    }

    float getSurv(DPSWarrCharacter dpswarchar, int Iter, boolean with) {
        dpswarchar.calcOpts.getMaintenance()[Iter] = with;
        CharacterCalculationsDPSWarr calculations = (CharacterCalculationsDPSWarr) getCharacterCalculations(dpswarchar.Char.clone());
        return calculations.totalHPS;
    }

    ComparisonCalculationDPSWarr getComp(DPSWarrCharacter dpswarchar, String Name, int Iter) {
        ComparisonCalculationDPSWarr comparison = new ComparisonCalculationDPSWarr();
        comparison.setName(Name);
        float with = getDPS(dpswarchar, Iter, true);
        float without = getDPS(dpswarchar, Iter, false);
        comparison.setDPSPoints(with - without);
        with = getSurv(dpswarchar, Iter, true);
        without = getSurv(dpswarchar, Iter, false);
        comparison.setSurvPoints(with - without);
        return comparison;
    }

    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        Character zeOriginal = character.clone();
        Character zeClone = character.clone();
        CharacterCalculationsDPSWarr calculations = (CharacterCalculationsDPSWarr) getCharacterCalculations(zeOriginal);
        CalculationOptionsDPSWarr calcOpts = (CalculationOptionsDPSWarr) zeOriginal.getCalculationOptions();
        boolean[] origMaints = calcOpts.getMaintenance().clone();
        DPSWarrCharacter dpswarchar = new DPSWarrCharacter();
        dpswarchar.Char = zeOriginal;
        dpswarchar.calcOpts = (CalculationOptionsDPSWarr) zeOriginal.getCalculationOptions();
        dpswarchar.bossOpts = zeOriginal.getBossOptions();

        switch (chartName) {
            // Ability DPS+Damage
            case "Ability DPS+Damage": {
                List<ComparisonCalculationBase> comparisons = new ArrayList<ComparisonCalculationBase>();
                for (Rotation.AbilWrapper aw : calculations.rot.getAbilityList()) {
                    if (aw.ability.damageOnUse() == 0) { continue; }
                    ComparisonCalculationDPSWarr comparison = new ComparisonCalculationDPSWarr();
                    comparison.setName(aw.ability.name);
                    comparison.setDescription(aw.ability.description);
                    comparison.setDPSPoints(aw.allDPS());
                    comparison.setSurvPoints(aw.ability.damageOnUse());
                    comparisons.add(comparison);
                }
                for (ComparisonCalculationDPSWarr comp : castComparisons(comparisons)) {
                    comp.setOverallPoints(comp.getDPSPoints() + comp.getSurvPoints());
                }
                calcOpts.setMaintenance(origMaints);
                return comparisons.toArray(new ComparisonCalculationBase[0]);
            }
            // Ability Maintenance Changes
            case "Ability Maintenance Changes": {
                List<ComparisonCalculationBase> comparisons = new ArrayList<ComparisonCalculationBase>();
                // Rage Generators
                comparisons.add(getComp(dpswarchar, "Berserker Rage", CalculationOptionsDPSWarr.Maintenances.BerserkerRage_.index));
                comparisons.add(getComp(dpswarchar, "Bloodrage", CalculationOptionsDPSWarr.Maintenances.Bloodrage_.index));
                // Maintenance
                comparisons.add(getComp(dpswarchar, "Battle Shout", CalculationOptionsDPSWarr.Maintenances.BattleShout_.index));
                comparisons.add(getComp(dpswarchar, "Commanding Shout", CalculationOptionsDPSWarr.Maintenances.CommandingShout_.index));
                comparisons.add(getComp(dpswarchar, "Demoralizing Shout", CalculationOptionsDPSWarr.Maintenances.DemoralizingShout_.index));
                comparisons.add(getComp(dpswarchar, "Sunder Armor", CalculationOptionsDPSWarr.Maintenances.SunderArmor_.index));
                comparisons.add(getComp(dpswarchar, "Thunder Clap", CalculationOptionsDPSWarr.Maintenances.ThunderClap_.index));
                comparisons.add(getComp(dpswarchar, "Hamstring", CalculationOptionsDPSWarr.Maintenances.Hamstring_.index));
                // Periodics
                comparisons.add(getComp(dpswarchar, "Shattering Throw", CalculationOptionsDPSWarr.Maintenances.ShatteringThrow_.index));
                comparisons.add(getComp(dpswarchar, "Sweeping Strikes", CalculationOptionsDPSWarr.Maintenances.SweepingStrikes_.index));
                comparisons.add(getComp(dpswarchar, "Death Wish", CalculationOptionsDPSWarr.Maintenances.DeathWish_.index));
                comparisons.add(getComp(dpswarchar, "Recklessness", CalculationOptionsDPSWarr.Maintenances.Recklessness_.index));
                comparisons.add(getComp(dpswarchar, "Enraged Regeneration", CalculationOptionsDPSWarr.Maintenances.EnragedRegeneration_.index));
                // Damage Dealers
                if (calculations.rot instanceof FuryRotation) {
                    comparisons.add(getComp(dpswarchar, "Bloodsurge", CalculationOptionsDPSWarr.Maintenances.Bloodsurge_.index));
                    comparisons.add(getComp(dpswarchar, "Bloodthirst", CalculationOptionsDPSWarr.Maintenances.Bloodthirst_.index));
                    comparisons.add(getComp(dpswarchar, "Whirlwind", CalculationOptionsDPSWarr.Maintenances.Whirlwind_.index));
                } else if (calculations.rot instanceof ArmsRotation) {
                    comparisons.add(getComp(dpswarchar, "Bladestorm", CalculationOptionsDPSWarr.Maintenances.Bladestorm_.index));
                    comparisons.add(getComp(dpswarchar, "Mortal Strike", CalculationOptionsDPSWarr.Maintenances.MortalStrike_.index));
                    comparisons.add(getComp(dpswarchar, "Rend", CalculationOptionsDPSWarr.Maintenances.Rend_.index));
                    comparisons.add(getComp(dpswarchar, "Overpower", CalculationOptionsDPSWarr.Maintenances.Overpower_.index));
                    comparisons.add(getComp(dpswarchar, "Taste for Blood", CalculationOptionsDPSWarr.Maintenances.TasteForBlood_.index));
                    comparisons.add(getComp(dpswarchar, "Sudden Death", CalculationOptionsDPSWarr.Maintenances.SuddenDeath_.index));
                    comparisons.add(getComp(dpswarchar, "Slam", CalculationOptionsDPSWarr.Maintenances.Slam_.index));
                }
                comparisons.add(getComp(dpswarchar, "<20% Execute Spamming", CalculationOptionsDPSWarr.Maintenances.ExecuteSpam_.index));
                // Rage Dumps
                comparisons.add(getComp(dpswarchar, "Heroic Strike", CalculationOptionsDPSWarr.Maintenances.HeroicStrike_.index));
                comparisons.add(getComp(dpswarchar, "Cleave", CalculationOptionsDPSWarr.Maintenances.Cleave_.index));
                for (ComparisonCalculationDPSWarr comp : castComparisons(comparisons)) {
                    comp.setOverallPoints(comp.getDPSPoints() + comp.getSurvPoints());
                }
                calcOpts.setMaintenance(origMaints);
                return comparisons.toArray(new ComparisonCalculationBase[0]);
            }
            // Rage Cost per Damage
            case "Rage Cost per Damage": {
                List<ComparisonCalculationBase> comparisons = new ArrayList<ComparisonCalculationBase>();
                float DeepWoundsDamage = calculations.rot.DW.tickSize() * 6f;

                for (Rotation.AbilWrapper aw : calculations.rot.getAbilityList()) {
                    if (aw.ability.damageOnUse() == 0) { continue; }
                    ComparisonCalculationDPSWarr comparison = new ComparisonCalculationDPSWarr();
                    comparison.setName(aw.ability.name);
                    comparison.setDescription(DPSWarrFormat.format("Costs {0} Rage\r\n{1}", aw.ability.rageCost, aw.ability.description));
                    comparison.getSubPoints()[0] = (aw.ability.damageOnUse() * aw.ability.avgTargets()) / aw.ability.rageCost;
                    comparison.getSubPoints()[1] = (aw.ability.mhAtkTable().crit * DeepWoundsDamage) / aw.ability.rageCost;
                    comparisons.add(comparison);
                }
                for (ComparisonCalculationDPSWarr comp : castComparisons(comparisons)) {
                    comp.setOverallPoints(comp.getSubPoints()[0] + comp.getSubPoints()[1]);
                }
                calcOpts.setMaintenance(origMaints);
                return comparisons.toArray(new ComparisonCalculationBase[0]);
            }
            // Execute Spam
            case "Execute Spam": {
                List<ComparisonCalculationBase> comparisons = new ArrayList<ComparisonCalculationBase>();
                {
                    boolean orig = ((CalculationOptionsDPSWarr) zeClone.getCalculationOptions()).getM_ExecuteSpam();
                    ((CalculationOptionsDPSWarr) zeClone.getCalculationOptions()).setM_ExecuteSpam(true);
                    CharacterCalculationsDPSWarr bah = (CharacterCalculationsDPSWarr) getCharacterCalculations(zeClone);
                    ComparisonCalculationDPSWarr comparison = new ComparisonCalculationDPSWarr();
                    comparison.setName("With Execute Spam");
                    comparison.setDescription("Turning <20% Execute Spam on on the options pane will change your DPS to this");
                    comparison.getSubPoints()[0] = getCharacterCalculations(zeClone).getSubPoints()[0];
                    comparison.getSubPoints()[1] = getCharacterCalculations(zeClone).getSubPoints()[1];
                    comparison.setEquipped(orig == true);
                    comparisons.add(comparison);
                    ((CalculationOptionsDPSWarr) zeClone.getCalculationOptions()).setM_ExecuteSpam(orig);
                }
                {
                    boolean orig = ((CalculationOptionsDPSWarr) zeClone.getCalculationOptions()).getM_ExecuteSpam();
                    ((CalculationOptionsDPSWarr) zeClone.getCalculationOptions()).setM_ExecuteSpam(false);
                    CharacterCalculationsDPSWarr bah = (CharacterCalculationsDPSWarr) getCharacterCalculations(zeClone);
                    ComparisonCalculationDPSWarr comparison = new ComparisonCalculationDPSWarr();
                    comparison.setName("Without Execute Spam");
                    comparison.setDescription("Turning <20% Execute Spam off on the options pane will change your DPS to this");
                    comparison.getSubPoints()[0] = getCharacterCalculations(zeClone).getSubPoints()[0];
                    comparison.getSubPoints()[1] = getCharacterCalculations(zeClone).getSubPoints()[1];
                    comparison.setEquipped(orig == false);
                    comparisons.add(comparison);
                    ((CalculationOptionsDPSWarr) zeClone.getCalculationOptions()).setM_ExecuteSpam(orig);
                }
                for (ComparisonCalculationDPSWarr comp : castComparisons(comparisons)) {
                    comp.setOverallPoints(comp.getSubPoints()[0] + comp.getSubPoints()[1]);
                }
                calcOpts.setMaintenance(origMaints);
                return comparisons.toArray(new ComparisonCalculationBase[0]);
            }
            default: {
                calcOpts.setMaintenance(origMaints);
                return new ComparisonCalculationBase[0];
            }
        }
    }

    private static List<ComparisonCalculationDPSWarr> castComparisons(List<ComparisonCalculationBase> list) {
        List<ComparisonCalculationDPSWarr> result = new ArrayList<ComparisonCalculationDPSWarr>();
        for (ComparisonCalculationBase c : list) result.add((ComparisonCalculationDPSWarr) c);
        return result;
    }

    // Character Calcs
    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        // DECISIONS #28: invalidate the shared SpecialEffect interpolation caches for a
        // user-initiated top-level calculation so the adaptive interpolator grids start fresh,
        // like they did in the oracle's per-fixture dump process.
        if (referenceCalculation && significantChange) {
            SpecialEffect.clearInterpolatorCaches();
            clearModelInterpolatorCaches();
        }

        CharacterCalculationsDPSWarr calculatedStats = new CharacterCalculationsDPSWarr();
        CalculationOptionsDPSWarr calcOpts = character.getCalculationOptions() instanceof CalculationOptionsDPSWarr
                ? (CalculationOptionsDPSWarr) character.getCalculationOptions()
                : null;
        if (calcOpts == null) calcOpts = new CalculationOptionsDPSWarr();

        BossOptions bossOpts = character.getBossOptions();
        if (bossOpts == null) bossOpts = new BossOptions();

        CombatFactors[] cfOut = new CombatFactors[1];
        WhiteAttacks[] waOut = new WhiteAttacks[1];
        Rotation[] rotOut = new Rotation[1];

        Stats stats = getCharacterStats(character, additionalItem, StatType.Average, calcOpts, bossOpts, cfOut, waOut, rotOut);
        CombatFactors combatFactors = cfOut[0];
        WhiteAttacks whiteAttacks = waOut[0];
        Rotation Rot = rotOut[0];

        DPSWarrCharacter charStruct = new DPSWarrCharacter();
        charStruct.calcOpts = calcOpts;
        charStruct.bossOpts = bossOpts;
        charStruct.Rot = Rot;
        charStruct.combatFactors = combatFactors;
        charStruct.Char = character;

        WarriorTalents talents = character.getWarriorTalents();

        if (calcOpts.getUseMarkov()) {
            // Diagnostic only: the C# subscribes the returned value nowhere (see DECISIONS #59).
            StateSpaceGeneratorFuryTest test = new StateSpaceGeneratorFuryTest();
            test.stateSpaceGeneratorFuryTest1(character, stats, combatFactors, whiteAttacks, calcOpts, bossOpts, needsDisplayCalculations);
        }

        Stats statsRace = BaseStats.getBaseStats(character.getLevel(), character.getCharacterClass(), character.getRace());

        calculatedStats.duration = calcOpts.getDuration();

        calculatedStats.averageStats = stats;
        if (needsDisplayCalculations) {
            calculatedStats.unbuffedStats = getCharacterStats(character, additionalItem, StatType.Unbuffed, calcOpts, bossOpts);
            calculatedStats.buffedStats = getCharacterStats(character, additionalItem, StatType.Buffed, calcOpts, bossOpts);
            calculatedStats.buffsStats = getBuffsStats(character, calcOpts);
            calculatedStats.maximumStats = getCharacterStats(character, additionalItem, StatType.Maximum, calcOpts, bossOpts);
        }

        calculatedStats.combatFactors = combatFactors;
        calculatedStats.rot = Rot;
        calculatedStats.targetLevel = calcOpts.getTargetLevel();
        calculatedStats.baseHealth = statsRace.getHealth();
        { // == Attack Table ==
            // Miss
            calculatedStats.miss = stats.getMiss();
            calculatedStats.hitRating = stats.getHitRating();
            calculatedStats.expertiseRating = stats.getExpertiseRating();
            calculatedStats.expertise = StatConversion.getExpertiseFromRating(stats.getExpertiseRating(), CharacterClass.Warrior) + stats.getExpertise();
            calculatedStats.mhExpertise = combatFactors.cMhExpertise;
            calculatedStats.ohExpertise = combatFactors.cOhExpertise;
            calculatedStats.weapMastPerc = character.getWarriorTalents().getWeaponMastery() / 100f;
            calculatedStats.agilityCritBonus = StatConversion.getCritFromAgility(stats.getAgility(), CharacterClass.Warrior);
            calculatedStats.critRating = stats.getCritRating();
            calculatedStats.critPercent = stats.getPhysicalCrit();
            calculatedStats.mhCrit = combatFactors.cMhYCrit;
            calculatedStats.ohCrit = combatFactors.cOhYCrit;
        }
        // Offensive
        calculatedStats.teethBonus = (stats.getArmor() * talents.getArmoredToTheTeeth() / 108f);
        calculatedStats.bonusCritPercPoleAxeSpec = ((character.getMainHand() != null && (combatFactors.cMhItemType == ItemType.TwoHandAxe || combatFactors.cMhItemType == ItemType.Polearm)) ? character.getWarriorTalents().getPoleaxeSpecialization() * 0.01f : 0.00f);
        calculatedStats.armorPenetrationMaceSpec = ((character.getMainHand() != null && combatFactors.cMhItemType == ItemType.TwoHandMace) ? character.getWarriorTalents().getMaceSpecialization() * 0.03f : 0.00f);
        calculatedStats.armorPenetrationStance = ((!combatFactors.furyStance()) ? (0.10f + stats.getBonusWarrior_T9_2P_ArP()) : 0.00f);
        calculatedStats.armorPenetrationRating = stats.getArmorPenetrationRating();
        calculatedStats.armorPenetrationRating2Perc = StatConversion.getArmorPenetrationFromRating(stats.getArmorPenetrationRating());
        calculatedStats.armorPenetration = calculatedStats.armorPenetrationMaceSpec
                + calculatedStats.armorPenetrationStance
                + calculatedStats.armorPenetrationRating2Perc;
        calculatedStats.hasteRating = stats.getHasteRating();
        calculatedStats.hastePercent = stats.getPhysicalHaste();

        // DPS
        Rot.initialize(calculatedStats);

        // Neutral
        // Defensive
        calculatedStats.armor = stats.getArmor();

        calculatedStats.floorstring = calcOpts.getAllowFlooring() ? "000" : "000.00";

        Rot.makeRotationAndDoDps(true, needsDisplayCalculations);

        // Special Damage Procs, like Bandit's Insignia or Hand-mounted Pyro Rockets
        Map<Trigger, Float> triggerIntervals = new LinkedHashMap<Trigger, Float>();
        Map<Trigger, Float> triggerChances = new LinkedHashMap<Trigger, Float>();
        calculateTriggers(charStruct, triggerIntervals, triggerChances);
        calculatedStats.specProcDPS = 0f;
        if (stats.getRawSpecialEffectData() != null && character.getMainHand() != null) {
            List<SpecialEffect> effects = new ArrayList<SpecialEffect>();
            for (SpecialEffect e : stats.specialEffects()) effects.add(e);
            SpecialDamageProcs SDP = new SpecialDamageProcs(character, stats, calculatedStats.targetLevel - character.getLevel(),
                    effects,
                    triggerIntervals, triggerChances,
                    calcOpts.getDuration(),
                    combatFactors.damageReduction());

            calculatedStats.specProcDPS = SDP.calculateAll();
        }
        calculatedStats.setTotalDPS(calculatedStats.getTotalDPS() + calculatedStats.specProcDPS);

        // Survivability
        if (stats.getHealthRestoreFromMaxHealth() > 0) {
            stats.setHealthRestore(stats.getHealthRestore() + stats.getHealthRestoreFromMaxHealth() / 100f * stats.getHealth() * calcOpts.getDuration());
        }

        float Health2Surv = (stats.getHealth()) / 100f;
        Health2Surv += (stats.getHealthRestore()) / 1000f;
        float DmgTakenMods2Surv = (1f - stats.getDamageTakenMultiplier()) * 100f;
        float BossAttackPower2Surv = stats.getBossAttackPower() / 14f * -1f;
        float BossAttackSpeedMods2Surv = (1f - stats.getBossAttackSpeedMultiplier()) * 100f;
        calculatedStats.totalHPS = Rot.hpsTtl;
        calculatedStats.setSurvivability(calcOpts.getSurvScale() * (calculatedStats.totalHPS
                + Health2Surv
                + DmgTakenMods2Surv
                + BossAttackPower2Surv
                + BossAttackSpeedMods2Surv
                + stats.getResilience() / 10));
        calculatedStats.setOverallPoints(calculatedStats.getTotalDPS() + calculatedStats.getSurvivability());

        if (needsDisplayCalculations) {
            calculatedStats.buffedStats = getCharacterStats(character, additionalItem, StatType.Buffed, calcOpts, bossOpts);

            float maxArp = calculatedStats.buffedStats.getArmorPenetrationRating();
            for (SpecialEffect effect : calculatedStats.buffedStats.specialEffects(s -> s.getStats().getArmorPenetrationRating() > 0f)) {
                maxArp += effect.getStats().getArmorPenetrationRating();
            }
            calculatedStats.maxArmorPenetration = calculatedStats.armorPenetrationMaceSpec
                    + calculatedStats.armorPenetrationStance
                    + StatConversion.getArmorPenetrationFromRating(maxArp);
        }

        return calculatedStats;
    }

    private enum StatType { Unbuffed, Buffed, Average, Maximum }

    public Stats getCharacterStats(Character character, Item additionalItem) {
        CalculationOptionsDPSWarr calcOpts = character.getCalculationOptions() instanceof CalculationOptionsDPSWarr
                ? (CalculationOptionsDPSWarr) character.getCalculationOptions()
                : new CalculationOptionsDPSWarr();
        return getCharacterStats(character, additionalItem, StatType.Average, calcOpts, character.getBossOptions());
    }

    // Talents That are handled as SpecialEffects
    // We need these to be static so they aren't re-created 50 bajillion times
    private static final SpecialEffect[] seWreckingCrew;

    static {
        seWreckingCrew = new SpecialEffect[6];
        seWreckingCrew[0] = null;
        for (int i = 1; i <= 5; i++) {
            Stats s = new Stats();
            s.setBonusDamageMultiplier(i * 0.02f);
            seWreckingCrew[i] = new SpecialEffect(Trigger.MeleeCrit, s, 12, 0);
        }
    }

    private static final SpecialEffect[] seTrauma = {
            null,
            traumaEffect(1),
            traumaEffect(2),
    };

    private static SpecialEffect traumaEffect(int pts) {
        Stats s = new Stats();
        s.setBonusBleedDamageMultiplier(pts * 0.15f);
        return new SpecialEffect(Trigger.MeleeCrit, s, 15, 0);
    }

    private static final SpecialEffect[] seDeathWish;

    static {
        seDeathWish = new SpecialEffect[4];
        for (int i = 0; i <= 3; i++) {
            Stats s = new Stats();
            s.setBonusDamageMultiplier(0.20f);
            s.setDamageTakenMultiplier(0.05f);
            seDeathWish[i] = new SpecialEffect(Trigger.Use, s, 30f, 3f * 60f * (1f - 1f / 9f * i));
        }
    }

    private static void clearModelInterpolatorCaches() {
        for (SpecialEffect e : seWreckingCrew) if (e != null) e.clearInterpolatorCache();
        for (SpecialEffect e : seTrauma) if (e != null) e.clearInterpolatorCache();
        for (SpecialEffect e : seDeathWish) if (e != null) e.clearInterpolatorCache();
        Rotation.clearStaticEffectCaches();
    }

    private Stats getItemStats(Character character, Item additionalItem) {
        Stats stats = new Stats();
        accumulateItemStats(stats, character, additionalItem);
        return stats;
    }

    private Stats getCharacterStats_Buffed(DPSWarrCharacter dpswarchar, Item additionalItem, boolean isBuffed) {
        if (dpswarchar.calcOpts == null) { dpswarchar.calcOpts = (CalculationOptionsDPSWarr) dpswarchar.Char.getCalculationOptions(); }
        if (dpswarchar.bossOpts == null) { dpswarchar.bossOpts = dpswarchar.Char.getBossOptions(); }
        if (dpswarchar.combatFactors == null) {
            dpswarchar.combatFactors = new CombatFactors(dpswarchar.Char, new Stats(), dpswarchar.calcOpts, dpswarchar.bossOpts);
        }
        WarriorTalents talents = dpswarchar.Char.getWarriorTalents();

        // From Race
        Stats statsRace = BaseStats.getBaseStats(dpswarchar.Char.getLevel(), CharacterClass.Warrior, dpswarchar.Char.getRace());
        // From Gear/Buffs
        Stats statsBuffs = (isBuffed ? getBuffsStats(dpswarchar.Char, dpswarchar.calcOpts) : new Stats());
        Stats statsItems = getItemStats(dpswarchar.Char, additionalItem);
        // From Options
        Stats statsOptionsPanel = new Stats();
        statsOptionsPanel.setBonusStrengthMultiplier((dpswarchar.combatFactors.furyStance() ? talents.getImprovedBerserkerStance() * 0.04f : 0f));
        statsOptionsPanel.setPhysicalCrit((dpswarchar.combatFactors.furyStance() ? 0.03f + statsBuffs.getBonusWarrior_T9_2P_Crit() : 0f));

        statsOptionsPanel.setDamageTakenMultiplier((dpswarchar.combatFactors.furyStance() ? 0.05f : 0f));

        // Battle Shout
        statsOptionsPanel.setAttackPower((dpswarchar.calcOpts.getM_BattleShout() ? (548f * (1f + talents.getCommandingPresence() * 0.05f)) : 0f));
        // Commanding Shout
        statsOptionsPanel.setHealth((dpswarchar.calcOpts.getM_CommandingShout() ? (2255f * (1f + talents.getCommandingPresence() * 0.05f)) : 0f));
        // Demo Shout
        statsOptionsPanel.setBossAttackPower((dpswarchar.calcOpts.getM_DemoralizingShout() ? (-411f * (1f + talents.getImprovedDemoralizingShout() * 0.08f)) : 0f));
        // Sunder Armor
        statsOptionsPanel.setArmorPenetration((dpswarchar.calcOpts.getM_SunderArmor() ? 0.04f * 5f : 0f));
        // Thunder Clap
        statsOptionsPanel.setBossAttackSpeedMultiplier((dpswarchar.calcOpts.getM_ThunderClap() ? -0.20f * (1f + talents.getImprovedThunderClap() * (10f / 3f)) : 0f));
        // From Talents
        Stats statsTalents = new Stats();
        // Offensive
        statsTalents.setBonusDamageMultiplier((dpswarchar.Char.getMainHand() == null ? 0f :
                // Two Handed Weapon Spec
                ((dpswarchar.Char.getMainHand().getItem().getSlot() == ItemSlot.TwoHand) ? 1f + talents.getTwoHandedWeaponSpecialization() * 0.02f : 1f)
                        *
                        // Titan's Grip Penalty
                        ((talents.getTitansGrip() > 0 && dpswarchar.Char.getOffHand() != null && (dpswarchar.Char.getOffHand().getItem().getSlot() == ItemSlot.TwoHand || dpswarchar.Char.getMainHand().getItem().getSlot() == ItemSlot.TwoHand) ? 0.90f : 1f))
                        // Convert it back a simple mod number
                        - 1f
        ));
        statsTalents.setBonusPhysicalDamageMultiplier((dpswarchar.calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.Rend_.index] // Have Rend up
                || talents.getDeepWounds() > 0 // Have Deep Wounds
                ? talents.getBloodFrenzy() * 0.02f : 0f));
        statsTalents.setPhysicalCrit(talents.getCruelty() * 0.01f);
        statsTalents.setBonusStaminaMultiplier(talents.getVitality() * 0.02f + talents.getStrengthOfArms() * 0.02f);
        statsTalents.setBonusStrengthMultiplier(talents.getVitality() * 0.02f + talents.getStrengthOfArms() * 0.02f);
        statsTalents.setExpertise(talents.getVitality() * 2.0f + talents.getStrengthOfArms() * 2.0f);
        statsTalents.setPhysicalHit(talents.getPrecision() * 0.01f);
        statsTalents.setPhysicalHaste(talents.getBloodFrenzy() * 0.05f);
        statsTalents.setStunDurReduc(talents.getIronWill() / 15f);
        // Defensive
        statsTalents.setParry(talents.getDeflection() * 0.01f);
        statsTalents.setDodge(talents.getAnticipation() * 0.01f);
        statsTalents.setBlock(talents.getShieldSpecialization() * 0.01f);
        statsTalents.setBonusBlockValueMultiplier(talents.getShieldMastery() * 0.15f);
        statsTalents.setBonusShieldSlamDamage(talents.getGagOrder() * 0.05f);
        statsTalents.setDevastateCritIncrease(talents.getSwordAndBoard() * 0.05f);
        statsTalents.setBaseArmorMultiplier(talents.getToughness() * 0.02f);
        // Add Talents that give SpecialEffects
        if (talents.getRampage() > 0 && isBuffed) { statsTalents.setPhysicalCrit(statsTalents.getPhysicalCrit() + 0.05f); }
        if (talents.getWreckingCrew() > 0 && dpswarchar.Char.getMainHand() != null) { statsTalents.addSpecialEffect(seWreckingCrew[talents.getWreckingCrew()]); }
        if (talents.getTrauma() > 0 && dpswarchar.Char.getMainHand() != null) { statsTalents.addSpecialEffect(seTrauma[talents.getTrauma()]); }
        if (talents.getDeathWish() > 0 && dpswarchar.calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.DeathWish_.index]) { statsTalents.addSpecialEffect(seDeathWish[talents.getIntensifyRage()]); }

        Stats statsTotal = new Stats();
        statsTotal.accumulate(statsRace);
        statsTotal.accumulate(statsItems);
        statsTotal.accumulate(statsBuffs);
        statsTotal.accumulate(statsTalents);
        statsTotal.accumulate(statsOptionsPanel);
        statsTotal = updateStatsAndAdd(statsTotal, null, dpswarchar.Char);

        // Dodge (your dodging incoming attacks)
        statsTotal.setDodge(statsTotal.getDodge() + StatConversion.getDodgeFromAgility(statsTotal.getAgility(), dpswarchar.Char.getCharacterClass()));
        statsTotal.setDodge(statsTotal.getDodge() + StatConversion.getDodgeFromRating(statsTotal.getDodgeRating(), dpswarchar.Char.getCharacterClass()));

        // Parry (your parrying incoming attacks)
        statsTotal.setParry(statsTotal.getParry() + StatConversion.getParryFromRating(statsTotal.getParryRating(), dpswarchar.Char.getCharacterClass()));

        return statsTotal;
    }

    private Stats getCharacterStats(Character character, Item additionalItem, StatType statType, CalculationOptionsDPSWarr calcOpts, BossOptions bossOpts) {
        CombatFactors[] temp = new CombatFactors[1];
        WhiteAttacks[] temp2 = new WhiteAttacks[1];
        Rotation[] temp3 = new Rotation[1];
        return getCharacterStats(character, additionalItem, statType, calcOpts, bossOpts, temp, temp2, temp3);
    }

    private Stats getCharacterStats(Character character, Item additionalItem, StatType statType, CalculationOptionsDPSWarr calcOpts, BossOptions bossOpts,
            CombatFactors[] combatFactorsOut, WhiteAttacks[] whiteAttacksOut, Rotation[] RotOut) {
        DPSWarrCharacter dpswarchar = new DPSWarrCharacter();
        dpswarchar.Char = character;
        dpswarchar.calcOpts = calcOpts;
        dpswarchar.bossOpts = bossOpts;
        Stats statsTotal = getCharacterStats_Buffed(dpswarchar, additionalItem, statType != StatType.Unbuffed);
        CombatFactors combatFactors = new CombatFactors(character, statsTotal, calcOpts, bossOpts);
        WhiteAttacks whiteAttacks = new WhiteAttacks(character, statsTotal, combatFactors, calcOpts, bossOpts);
        Rotation Rot;
        if (combatFactors.furyStance()) Rot = new FuryRotation(character, statsTotal, combatFactors, whiteAttacks, calcOpts, bossOpts);
        else Rot = new ArmsRotation(character, statsTotal, combatFactors, whiteAttacks, calcOpts, bossOpts);
        combatFactorsOut[0] = combatFactors;
        whiteAttacksOut[0] = whiteAttacks;
        RotOut[0] = Rot;

        if (statType == StatType.Buffed) {
            return statsTotal;
        }
        // SpecialEffects: Supposed to handle all procs such as Berserking, Mirror of Truth, Grim Toll, etc.
        WarriorTalents talents = character.getWarriorTalents();
        Rot.initialize();
        Rot.makeRotationAndDoDps(false, false);
        Rot.addValidatedSpecialEffects(statsTotal, talents);

        DPSWarrCharacter charStruct = new DPSWarrCharacter();
        charStruct.calcOpts = calcOpts;
        charStruct.bossOpts = bossOpts;
        charStruct.Char = character;
        charStruct.combatFactors = combatFactors;
        charStruct.Rot = Rot;

        float fightDuration = calcOpts.getDuration();

        List<SpecialEffect> bersMainHand = new ArrayList<SpecialEffect>();
        List<SpecialEffect> bersOffHand = new ArrayList<SpecialEffect>();

        if (character.getMainHandEnchant() != null) { // 3789 = Berserker Enchant ID, but now supporting other proc effects as well
            java.util.Iterator<SpecialEffect> mhEffects = character.getMainHandEnchant().stats.specialEffects().iterator();
            if (mhEffects.hasNext()) {
                bersMainHand.add(mhEffects.next());
            }
        }
        if (character.getMainHand() != null && character.getMainHand().getItem().getStats().getRawSpecialEffectData() != null) {
            java.util.Iterator<SpecialEffect> mhEffects = character.getMainHand().getItem().getStats().specialEffects().iterator();
            if (mhEffects.hasNext()) { bersMainHand.add(mhEffects.next()); }
        }
        if (combatFactors.useOH && character.getOffHandEnchant() != null) {
            java.util.Iterator<SpecialEffect> ohEffects = character.getOffHandEnchant().stats.specialEffects().iterator();
            if (ohEffects.hasNext()) { bersOffHand.add(ohEffects.next()); }
        }
        if (character.getOffHand() != null && character.getOffHand().getItem().getStats().getRawSpecialEffectData() != null) {
            java.util.Iterator<SpecialEffect> ohEffects = character.getOffHand().getItem().getStats().specialEffects().iterator();
            if (ohEffects.hasNext()) { bersOffHand.add(ohEffects.next()); }
        }
        if (statType == StatType.Average) {
            doSpecialEffects(charStruct, bersMainHand, bersOffHand, statsTotal);
        } else { // if (statType == StatType.Maximum)
            Stats maxSpecEffects = new Stats();
            for (SpecialEffect effect : statsTotal.specialEffects()) maxSpecEffects.accumulate(effect.getStats());
            return updateStatsAndAdd(maxSpecEffects, combatFactors.StatS, character);
        }

        // special case for dual wielding w/ berserker enchant on one/both weapons, as they act independently
        Stats bersStats = new Stats();
        for (SpecialEffect e : bersMainHand) {
            if (e.getDuration() == 0) {
                bersStats.setShadowDamage(e.getAverageProcsPerSecond(fightDuration / Rot.attemptedAtksOverDurMH(), Rot.landedAtksOverDurMH() / Rot.attemptedAtksOverDurMH(), combatFactors.cMhItemSpeed, calcOpts.getSE_UseDur() ? fightDuration : 0));
            } else {
                // berserker enchant id
                float f = e.getAverageUptime(fightDuration / Rot.attemptedAtksOverDurMH(), Rot.landedAtksOverDurMH() / Rot.attemptedAtksOverDurMH(), combatFactors.cMhItemSpeed, calcOpts.getSE_UseDur() ? fightDuration : 0);
                bersStats.accumulate(e.getStats(), f);
            }
        }
        for (SpecialEffect e : bersOffHand) {
            if (e.getDuration() == 0) {
                bersStats.setShadowDamage(bersStats.getShadowDamage() + e.getAverageProcsPerSecond(fightDuration / Rot.attemptedAtksOverDurOH(), Rot.landedAtksOverDurOH() / Rot.attemptedAtksOverDurOH(), combatFactors.cOhItemSpeed, calcOpts.getSE_UseDur() ? fightDuration : 0));
            } else {
                float f = e.getAverageUptime(fightDuration / Rot.attemptedAtksOverDurOH(), Rot.landedAtksOverDurOH() / Rot.attemptedAtksOverDurOH(), combatFactors.cOhItemSpeed, calcOpts.getSE_UseDur() ? fightDuration : 0);
                bersStats.accumulate(e.getStats(), f);
            }
        }
        combatFactors.StatS = updateStatsAndAdd(bersStats, combatFactors.StatS, character);
        combatFactors.invalidateCache();
        return combatFactors.StatS;
    }

    private void doSpecialEffects(DPSWarrCharacter charStruct,
            List<SpecialEffect> bersMainHand, List<SpecialEffect> bersOffHand,
            Stats statsTotal) {
        // Initialize Triggers
        Map<Trigger, Float> triggerIntervals = new LinkedHashMap<Trigger, Float>();
        Map<Trigger, Float> triggerChances = new LinkedHashMap<Trigger, Float>();

        calculateTriggers(charStruct, triggerIntervals, triggerChances);

        // ArPen Lists
        List<Float> tempArPenRatings = new ArrayList<Float>();
        List<Float> tempArPenRatingUptimes = new ArrayList<Float>();
        List<SpecialEffect> tempArPenEffects = new ArrayList<SpecialEffect>();
        List<Float> tempArPenEffectIntervals = new ArrayList<Float>();
        List<Float> tempArPenEffectChances = new ArrayList<Float>();
        List<Float> tempArPenEffectScales = new ArrayList<Float>();

        List<SpecialEffect> critEffects = new ArrayList<SpecialEffect>();

        List<SpecialEffect> firstPass = new ArrayList<SpecialEffect>();
        List<SpecialEffect> secondPass = new ArrayList<SpecialEffect>();
        boolean doubleExecutioner = false;
        for (SpecialEffect effect : statsTotal.specialEffects()) {
            effect.getStats().generateSparseData();

            if (!triggerIntervals.containsKey(effect.getTrigger())) continue;
            else if (effect.getStats().getCritRating() + effect.getStats().getDeathbringerProc() > 0f) {
                critEffects.add(effect);
                if (effect.getStats().getDeathbringerProc() > 0f) secondPass.add(effect); // for strength only
            } else if (effect.getStats().getArmorPenetrationRating() > 0f) {
                if (doubleExecutioner) continue;
                Trigger realTrigger;
                if (bersMainHand.contains(effect)) {
                    bersMainHand.remove(effect);
                    doubleExecutioner = true;
                    if (bersOffHand.contains(effect)) {
                        bersOffHand.remove(effect);
                        realTrigger = Trigger.MeleeHit;
                    } else realTrigger = Trigger.MainHandHit;
                } else if (bersOffHand.contains(effect)) {
                    bersOffHand.remove(effect);
                    realTrigger = Trigger.OffHandHit;
                } else realTrigger = effect.getTrigger();
                tempArPenEffects.add(effect);
                tempArPenEffectIntervals.add(triggerIntervals.get(realTrigger));
                tempArPenEffectChances.add(triggerChances.get(realTrigger));
                tempArPenEffectScales.add(1f);
            } else if (!bersMainHand.contains(effect) && !bersOffHand.contains(effect) &&
                    (effect.getStats().getDeathbringerProc() > 0f ||
                            effect.getStats().getAgility() > 0f ||
                            effect.getStats().getHasteRating() > 0f ||
                            effect.getStats().getHitRating() > 0f ||
                            effect.getStats().getCritRating() > 0f ||
                            effect.getStats().getPhysicalHaste() > 0f ||
                            effect.getStats().getPhysicalCrit() > 0f ||
                            effect.getStats().getPhysicalHit() > 0f)) {
                firstPass.add(effect);
            } else if (!bersMainHand.contains(effect) && !bersOffHand.contains(effect)) {
                secondPass.add(effect);
            }
        }

        if (tempArPenEffects.isEmpty()) {
            // tempArPenRatings.Add(0.0f);
            // tempArPenRatingUptimes.Add(1.0f);
        } else if (tempArPenEffects.size() == 1) { // Only one, add it to
            SpecialEffect effect = tempArPenEffects.get(0);
            float uptime = effect.getAverageStackSize(tempArPenEffectIntervals.get(0), tempArPenEffectChances.get(0), charStruct.combatFactors.cMhItemSpeed, (charStruct.calcOpts.getSE_UseDur()
                    ? charStruct.calcOpts.getDuration()
                    : 0f)) * tempArPenEffectScales.get(0);
            tempArPenRatings.add(effect.getStats().getArmorPenetrationRating());
            tempArPenRatingUptimes.add(uptime);
            tempArPenRatings.add(0.0f);
            tempArPenRatingUptimes.add(1.0f - uptime);
        } else if (tempArPenEffects.size() > 1) {
            WeightedStat[] arPenWeights = SpecialEffect.getAverageCombinedUptimeCombinations(
                    tempArPenEffects.toArray(new SpecialEffect[0]),
                    toFloatArray(tempArPenEffectIntervals), toFloatArray(tempArPenEffectChances),
                    new float[tempArPenEffectChances.size()], toFloatArray(tempArPenEffectScales),
                    charStruct.combatFactors.cMhItemSpeed,
                    charStruct.calcOpts.getDuration(),
                    AdditiveStat.ArmorPenetrationRating);
            for (int i = 0; i < arPenWeights.length; i++) {
                tempArPenRatings.add(arPenWeights[i].value());
                tempArPenRatingUptimes.add(arPenWeights[i].chance());
            }
        }
        // Get the average Armor Pen Rating across all procs
        if (tempArPenRatings.size() > 0f) {
            Stats originalStats = charStruct.combatFactors.StatS;
            int LevelDif = charStruct.calcOpts.getTargetLevel() - charStruct.Char.getLevel();

            float arpenBuffs =
                    ((charStruct.combatFactors.cMhItemType == ItemType.TwoHandMace) ? charStruct.Char.getWarriorTalents().getMaceSpecialization() * 0.03f : 0.00f) +
                            (!charStruct.combatFactors.furyStance() ? (0.10f + originalStats.getBonusWarrior_T9_2P_ArP()) : 0.0f);

            float OriginalArmorReduction = StatConversion.getArmorDamageReduction(charStruct.Char.getLevel(), (int) StatConversion.NPC_ARMOR[LevelDif],
                    originalStats.getArmorPenetration(), arpenBuffs, originalStats.getArmorPenetrationRating());
            float ProccedArmorReduction = 0f;
            for (int i = 0; i < tempArPenRatings.size(); i++) {
                ProccedArmorReduction += tempArPenRatingUptimes.get(i) *
                        StatConversion.getArmorDamageReduction(charStruct.Char.getLevel(),
                                (int) StatConversion.NPC_ARMOR[LevelDif],
                                originalStats.getArmorPenetration(), arpenBuffs,
                                originalStats.getArmorPenetrationRating() + tempArPenRatings.get(i));
            }

            float procArp = StatConversion.getRatingFromArmorReduction(charStruct.Char.getLevel(), (int) StatConversion.NPC_ARMOR[LevelDif],
                    originalStats.getArmorPenetration(), arpenBuffs, ProccedArmorReduction);
            originalStats.setArmorPenetrationRating(originalStats.getArmorPenetrationRating() + (procArp - originalStats.getArmorPenetrationRating()));
        }

        iterativeSpecialEffectsStats(charStruct, firstPass, critEffects, triggerIntervals, triggerChances, 0f, true, new Stats(), charStruct.combatFactors.StatS);
        iterativeSpecialEffectsStats(charStruct, secondPass, critEffects, triggerIntervals, triggerChances, 0f, false, null, charStruct.combatFactors.StatS);
    }

    private static float[] toFloatArray(List<Float> list) {
        float[] result = new float[list.size()];
        for (int i = 0; i < result.length; i++) result[i] = list.get(i);
        return result;
    }

    private static void calculateTriggers(DPSWarrCharacter charStruct, Map<Trigger, Float> triggerIntervals, Map<Trigger, Float> triggerChances) {
        String addInfo = "No Additional Info";
        float fightDuration = charStruct.calcOpts.getDuration();
        addInfo = "FightDur Passed";
        float fightDuration2Pass = charStruct.calcOpts.getSE_UseDur() ? fightDuration : 0;

        float attemptedMH = charStruct.Rot.attemptedAtksOverDurMH();
        float attemptedOH = charStruct.Rot.attemptedAtksOverDurOH();
        float attempted = attemptedMH + attemptedOH;

        float landMH = charStruct.Rot.landedAtksOverDurMH();
        float landOH = charStruct.Rot.landedAtksOverDurOH();
        float land = landMH + landOH;

        float crit = charStruct.Rot.criticalAtksOverDur();

        float avoidedAttacks = charStruct.combatFactors.StatS.getDodge() + charStruct.combatFactors.StatS.getParry();

        float dwbleed = 0;
        addInfo += "\r\nbig if started";
        if (charStruct.Char.getWarriorTalents().getDeepWounds() > 0 && crit != 0f) {
            float dwTicks = 1f;
            for (Rotation.AbilWrapper aw : charStruct.Rot.getDamagingAbilities()) {
                if (aw.allNumActivates() > 0f && aw.ability.canCrit) {
                    if (aw.ability.swingsOffHand) {
                        dwTicks *= (float) Math.pow(1f - aw.ability.mhAtkTable().crit * (1f - aw.ability.ohAtkTable().crit), aw.allNumActivates() / fightDuration);
                        dwTicks *= (float) Math.pow(1f - aw.ability.ohAtkTable().crit, aw.allNumActivates() / fightDuration);
                    } else {
                        // to fix this breaking apart when you're close to yellow crit cap for these abilities, namely OP
                        if (aw.ability instanceof OverPower || aw.ability instanceof TasteForBlood)
                            dwTicks *= (1f - aw.allNumActivates() / fightDuration * aw.ability.mhAtkTable().crit);
                        else dwTicks *= (float) Math.pow(1f - aw.ability.mhAtkTable().crit, aw.allNumActivates() / fightDuration);
                    }
                }
            }
            dwbleed = fightDuration * dwTicks;
        }
        addInfo += "\r\nBuncha Floats started";
        float bleed = dwbleed + fightDuration * (charStruct.combatFactors.furyStance() || !charStruct.calcOpts.getMaintenance()[CalculationOptionsDPSWarr.Maintenances.Rend_.index] ? 0f : 1f / 3f);

        float bleedHitInterval = fightDuration / bleed;
        float dwbleedHitInterval = fightDuration / dwbleed;
        float attemptedAtkInterval = fightDuration / attempted;
        float attemptedAtksIntervalMH = fightDuration / attemptedMH;
        float attemptedAtksIntervalOH = fightDuration / attemptedOH;
        float landedAtksInterval = fightDuration / land;
        float dmgDoneInterval = fightDuration / (land + bleed);
        float dmgTakenInterval = fightDuration / charStruct.calcOpts.getAoETargetsFreq();
        addInfo += "\r\nAoETargsFreq Passed";
        float hitRate = 1, hitRateMH = 1, hitRateOH = 1, critRate = 1;
        if (attempted != 0f) {
            hitRate = land / attempted;
            critRate = crit / attempted;
        }
        if (attemptedMH != 0f)
            hitRateMH = landMH / attemptedMH;
        if (attemptedOH != 0f)
            hitRateOH = landOH / attemptedOH;
        addInfo += "\r\nTriggers started";
        triggerIntervals.put(Trigger.Use, 0f);
        triggerChances.put(Trigger.Use, 1f);

        triggerIntervals.put(Trigger.MeleeHit, attemptedAtkInterval);
        triggerIntervals.put(Trigger.PhysicalHit, attemptedAtkInterval);
        triggerChances.put(Trigger.MeleeHit, hitRate);
        triggerChances.put(Trigger.PhysicalHit, hitRate);

        triggerIntervals.put(Trigger.PhysicalCrit, attemptedAtkInterval);
        triggerIntervals.put(Trigger.MeleeCrit, attemptedAtkInterval);
        triggerChances.put(Trigger.PhysicalCrit, critRate);
        triggerChances.put(Trigger.MeleeCrit, critRate);

        triggerIntervals.put(Trigger.MainHandHit, attemptedAtksIntervalMH);
        triggerChances.put(Trigger.MainHandHit, hitRateMH);
        triggerIntervals.put(Trigger.OffHandHit, attemptedAtksIntervalOH);
        triggerChances.put(Trigger.OffHandHit, hitRateOH);

        triggerIntervals.put(Trigger.DoTTick, bleedHitInterval);
        triggerChances.put(Trigger.DoTTick, 1f);

        triggerIntervals.put(Trigger.DamageDone, dmgDoneInterval);
        triggerChances.put(Trigger.DamageDone, 1f);

        triggerIntervals.put(Trigger.DamageOrHealingDone, dmgDoneInterval); // Need to add Self Heals
        triggerChances.put(Trigger.DamageOrHealingDone, 1f);

        triggerIntervals.put(Trigger.DamageTaken, dmgTakenInterval);
        triggerChances.put(Trigger.DamageTaken, 1f);

        triggerIntervals.put(Trigger.DamageAvoided, dmgTakenInterval);
        triggerChances.put(Trigger.DamageAvoided, avoidedAttacks);

        triggerIntervals.put(Trigger.HSorSLHit, fightDuration / charStruct.Rot.critHsSlamOverDur());
        triggerChances.put(Trigger.HSorSLHit, 1f);

        triggerIntervals.put(Trigger.DeepWoundsTick, dwbleedHitInterval);
        triggerChances.put(Trigger.DeepWoundsTick, 1f);
        addInfo += "\r\nFinished";
    }

    private Stats iterativeSpecialEffectsStats(DPSWarrCharacter charStruct, List<SpecialEffect> specialEffects, List<SpecialEffect> critEffects,
            Map<Trigger, Float> triggerIntervals, Map<Trigger, Float> triggerChances, float oldFlurryUptime,
            boolean iterate, Stats iterateOld, Stats originalStats) {
        WarriorTalents talents = charStruct.Char.getWarriorTalents();
        float fightDuration = charStruct.calcOpts.getDuration();
        Stats statsProcs = new Stats();
        float dmgTakenInterval = fightDuration / charStruct.calcOpts.getAoETargetsFreq();

        float attempted = charStruct.Rot.attemptedAtksOverDur();
        float land = charStruct.Rot.landedAtksOverDur();
        float crit = charStruct.Rot.criticalAtksOverDur();

        int LevelDif = charStruct.calcOpts.getTargetLevel() - charStruct.Char.getLevel();
        List<Trigger> critTriggers = new ArrayList<Trigger>();
        List<Float> critWeights = new ArrayList<Float>();
        boolean needsHitTableReset = false;
        for (SpecialEffect effect : critEffects) {
            needsHitTableReset = true;

            critTriggers.add(effect.getTrigger());
            critWeights.add(1f / (effect.getStats().getDeathbringerProc() > 0f ? 3f : 1f));

        }
        for (SpecialEffect effect : specialEffects) {
            float numProcs = 0;
            if (effect.getStats().getManaorEquivRestore() > 0f && effect.getStats().getHealthRestoreFromMaxHealth() > 0f) {
                // effect.Duration = 0, so GetAverageStats won't work
                float value1 = effect.getStats().getManaorEquivRestore();
                float value2 = effect.getStats().getHealthRestoreFromMaxHealth();
                Stats dummyStats = new Stats();
                dummyStats.setManaorEquivRestore(value1);
                dummyStats.setHealthRestoreFromMaxHealth(value2);
                SpecialEffect dummy = new SpecialEffect(effect.getTrigger(), dummyStats, effect.getDuration(), effect.getCooldown(), effect.getChance());
                numProcs = dummy.getAverageProcsPerSecond(dmgTakenInterval, originalStats.getDodge() + originalStats.getParry(), 0f, 0f) * fightDuration;
                statsProcs.setManaorEquivRestore(statsProcs.getManaorEquivRestore() + dummy.getStats().getManaorEquivRestore() * numProcs);
                dummy.getStats().setManaorEquivRestore(0f);
                applySpecialEffect(dummy, charStruct, triggerIntervals, triggerChances, statsProcs);
            } else if (effect.getStats().getManaorEquivRestore() > 0f) {
                // effect.Duration = 0, so GetAverageStats won't work
                numProcs = effect.getAverageProcsPerSecond(dmgTakenInterval, originalStats.getDodge() + originalStats.getParry(), 0f, 0f) * fightDuration;
                statsProcs.setManaorEquivRestore(statsProcs.getManaorEquivRestore() + effect.getStats().getManaorEquivRestore() * numProcs);
            } else {
                applySpecialEffect(effect, charStruct, triggerIntervals, triggerChances, statsProcs);
            }
        }

        WeightedStat[] critProcs;
        if (critEffects.size() == 0) {
            critProcs = new WeightedStat[] { new WeightedStat(1f, 0f) };
        } else if (critEffects.size() == 1) {
            float interval = triggerIntervals.get(critEffects.get(0).getTrigger());
            float chance = triggerChances.get(critEffects.get(0).getTrigger());
            float upTime = critEffects.get(0).getAverageStackSize(interval, chance, charStruct.combatFactors.cMhItemSpeed, (charStruct.calcOpts.getSE_UseDur()
                    ? charStruct.calcOpts.getDuration()
                    : 0f));
            upTime *= critWeights.get(0);
            critProcs = new WeightedStat[] { new WeightedStat(upTime, critEffects.get(0).getStats().getCritRating() + critEffects.get(0).getStats().getDeathbringerProc()),
                    new WeightedStat(1f - upTime, 0f) };
        } else {
            float[] intervals = new float[critEffects.size()];
            float[] chances = new float[critEffects.size()];
            float[] offset = new float[critEffects.size()];
            for (int i = 0; i < critEffects.size(); i++) {
                intervals[i] = triggerIntervals.get(critEffects.get(i).getTrigger());
                chances[i] = triggerChances.get(critEffects.get(i).getTrigger());
                if (critEffects.get(i).getStats().getDeathbringerProc() > 0f) {
                    critEffects.get(i).getStats().setCritRating(critEffects.get(i).getStats().getDeathbringerProc());
                }
            }

            critProcs = SpecialEffect.getAverageCombinedUptimeCombinations(critEffects.toArray(new SpecialEffect[0]), intervals, chances, offset, toFloatArray(critWeights), charStruct.combatFactors.cMhItemSpeed,
                    charStruct.calcOpts.getDuration(),
                    AdditiveStat.CritRating);
            for (SpecialEffect se : critEffects) {
                if (se.getStats().getDeathbringerProc() > 0f) se.getStats().setCritRating(0f);
            }
        }
        charStruct.combatFactors.critProcs = critProcs;
        float flurryUptime = 0f;
        if (iterate && talents.getFlurry() > 0f && charStruct.Char.getMainHand() != null && charStruct.Char.getMainHand().getItem() != null) {
            float numFlurryHits = 3f; // default
            float mhPerc = 1f; // 100% by default
            float flurryHaste = 0.05f * talents.getFlurry();
            boolean useOffHand = false;

            float flurryHitsPerSec = charStruct.combatFactors.totalHaste() * (1f + flurryHaste) / (1f + flurryHaste * oldFlurryUptime);
            float temp = 1f / charStruct.Char.getMainHand().getItem().getSpeed();
            if (charStruct.Char.getOffHand() != null && charStruct.Char.getOffHand().getItem() != null) {
                useOffHand = true;
                temp += 1f / charStruct.Char.getOffHand().getItem().getSpeed();
                mhPerc = (charStruct.Char.getMainHand().getSpeed() / charStruct.Char.getOffHand().getSpeed()) / (1f + charStruct.Char.getMainHand().getSpeed() / charStruct.Char.getOffHand().getSpeed());
                if (charStruct.Char.getOffHand().getSpeed() == charStruct.Char.getMainHand().getSpeed()) numFlurryHits = 4f;
            }

            flurryHitsPerSec *= temp;
            float flurryDuration = numFlurryHits / flurryHitsPerSec;
            flurryUptime = 1f;
            for (Rotation.AbilWrapper aw : charStruct.Rot.getDamagingAbilities()) {
                if (aw.ability.canCrit && aw.allNumActivates() > 0f) {
                    if (aw.ability instanceof OnAttack) {
                        float tempFactor = (float) Math.pow(1f - aw.ability.mhAtkTable().crit, numFlurryHits * mhPerc * aw.allNumActivates() / charStruct.Rot.whiteAtks.mhActivatesNoHS());
                        flurryUptime *= tempFactor;
                    } else {
                        float tempFactor = (float) Math.pow(1f - aw.ability.mhAtkTable().crit, flurryDuration * (aw.allNumActivates() / fightDuration));
                        flurryUptime *= tempFactor;
                        if (aw.ability.swingsOffHand && useOffHand) {
                            flurryUptime *= (float) Math.pow(1f - aw.ability.ohAtkTable().crit, flurryDuration * (aw.allNumActivates() / fightDuration));
                        }
                    }
                }
            }
            flurryUptime *= (float) Math.pow(1f - charStruct.Rot.whiteAtks.mhAtkTable.crit, numFlurryHits * mhPerc * charStruct.Rot.whiteAtks.mhActivates() / charStruct.Rot.whiteAtks.mhActivatesNoHS());
            flurryUptime *= (float) Math.pow(1f - charStruct.Rot.whiteAtks.ohAtkTable.crit, numFlurryHits * (1f - mhPerc));
            flurryUptime = 1 - flurryUptime;
            statsProcs.setPhysicalHaste((1f + statsProcs.getPhysicalHaste()) * (1f + flurryHaste * flurryUptime) - 1f);
        }

        charStruct.combatFactors.StatS = updateStatsAndAdd(statsProcs, originalStats, charStruct.Char);
        charStruct.combatFactors.invalidateCache();
        if (iterate) {

            final float precisionWhole = 0.01f;
            final float precisionDec = 0.0001f;
            if (statsProcs.getAgility() - iterateOld.getAgility() > precisionWhole ||
                    statsProcs.getHasteRating() - iterateOld.getHasteRating() > precisionWhole ||
                    statsProcs.getHitRating() - iterateOld.getHitRating() > precisionWhole ||
                    statsProcs.getCritRating() - iterateOld.getCritRating() > precisionWhole ||
                    statsProcs.getPhysicalHaste() - iterateOld.getPhysicalHaste() > precisionDec ||
                    statsProcs.getPhysicalCrit() - iterateOld.getPhysicalCrit() > precisionDec ||
                    statsProcs.getPhysicalHit() - iterateOld.getPhysicalHit() > precisionDec) {
                if (needsHitTableReset) charStruct.Rot.resetHitTables();
                charStruct.Rot.doIterations();
                calculateTriggers(charStruct, triggerIntervals, triggerChances);
                return iterativeSpecialEffectsStats(charStruct,
                        specialEffects, critEffects, triggerIntervals, triggerChances, flurryUptime, true, statsProcs, originalStats);
            }
        }

        return statsProcs;
    }

    private float applySpecialEffect(SpecialEffect effect, DPSWarrCharacter charStruct, Map<Trigger, Float> triggerIntervals, Map<Trigger, Float> triggerChances, Stats applyTo) {
        float fightDuration = charStruct.calcOpts.getDuration();
        float fightDuration2Pass = charStruct.calcOpts.getSE_UseDur() ? fightDuration : 0;

        Stats effectStats = effect.getStats();

        float upTime = 0f;
        if (effect.getTrigger() == Trigger.Use) {
            if (effect.getStats().getRawSpecialEffectDataSize() == 1) {
                upTime = effect.getAverageUptime(0f, 1f, charStruct.combatFactors.cMhItemSpeed, fightDuration2Pass);
                Stats _stats2 = new Stats();
                applySpecialEffect(effect.getStats().getRawSpecialEffectData()[0], charStruct, triggerIntervals, triggerChances, _stats2);
                effectStats = _stats2;
            } else {
                upTime = effect.getAverageStackSize(0f, 1f, charStruct.combatFactors.cMhItemSpeed, fightDuration2Pass);
            }
        } else if (effect.getDuration() == 0f) {
            upTime = effect.getAverageProcsPerSecond(triggerIntervals.get(effect.getTrigger()),
                    triggerChances.get(effect.getTrigger()),
                    charStruct.combatFactors.cMhItemSpeed,
                    fightDuration2Pass);
        } else if (triggerIntervals.containsKey(effect.getTrigger())) {
            upTime = effect.getAverageStackSize(triggerIntervals.get(effect.getTrigger()),
                    triggerChances.get(effect.getTrigger()),
                    charStruct.combatFactors.cMhItemSpeed,
                    fightDuration2Pass);
        }

        if (effect.getStats().getDeathbringerProc() > 0)
            upTime /= 3;
        if (upTime > 0f) {
            if (effect.getDuration() == 0f)
                applyTo.setShadowDamage(upTime);
            else if (upTime <= effect.getMaxStack())
                applyTo.accumulate(effectStats, upTime);

            return upTime;
        }
        return 0f;
    }

    private static Stats updateStatsAndAdd(Stats statsToAdd, Stats baseStats, Character character) {
        Stats retVal;
        float newStaMult = 1f + statsToAdd.getBonusStaminaMultiplier();
        float newStrMult = 1f + statsToAdd.getBonusStrengthMultiplier();
        float newAgiMult = 1f + statsToAdd.getBonusAgilityMultiplier();
        float newArmMult = 1f + statsToAdd.getBonusArmorMultiplier();
        float newBaseArmMult = 1f + statsToAdd.getBaseArmorMultiplier();
        float newAtkMult = 1f + statsToAdd.getBonusAttackPowerMultiplier();
        float newHealthMult = 1f + statsToAdd.getBonusHealthMultiplier();
        if (baseStats != null) {
            retVal = baseStats.copy();

            newStaMult *= (1f + retVal.getBonusStaminaMultiplier());
            newStrMult *= (1f + retVal.getBonusStrengthMultiplier());
            newAgiMult *= (1f + retVal.getBonusAgilityMultiplier());
            newArmMult *= (1f + retVal.getBonusArmorMultiplier());
            newBaseArmMult *= (1f + retVal.getBaseArmorMultiplier());
            newAtkMult *= (1f + retVal.getBonusAttackPowerMultiplier());
            newHealthMult *= (1f + retVal.getBonusHealthMultiplier());

            // normalize retVal with its old base stat values, since we're updating them below
            // This essentially reverses what gets done to statsToAdd, but only things that
            // are affected by multipliers (like base stats, armor, AP, etc)

            retVal.setHealth(retVal.getHealth() - retVal.getStamina() * 10f); // Stamina is affected by a multiplier

            // Since AP is set to (RawAP + 2*STR + A2T + BonusAP)*APMult, and Str/A2T are affected by mults too,
            // we need to rewind the Str and Armor components out.  We will add them after we've updated Str/Armor, below
            retVal.setAttackPower(retVal.getAttackPower() / (1f + retVal.getBonusAttackPowerMultiplier()));
            retVal.setAttackPower(retVal.getAttackPower() - ((retVal.getStrength() * 2f) +
                    (retVal.getArmor() / 108f * character.getWarriorTalents().getArmoredToTheTeeth())));

            // This is reversing the Armor = (Armor*BaseMult + Bonus)*BonusMult
            retVal.setArmor(retVal.getArmor() / (1f + retVal.getBonusArmorMultiplier()));
            retVal.setArmor(retVal.getArmor() - retVal.getBonusArmor());
            retVal.setArmor(retVal.getArmor() / (1f + retVal.getBaseArmorMultiplier()));
            retVal.setBonusArmor(retVal.getBonusArmor() - retVal.getAgility() * 2f);

            // Agi is multed, remove it from PhysicalCrit for now
            retVal.setPhysicalCrit(retVal.getPhysicalCrit() - StatConversion.getCritFromAgility(retVal.getAgility(), character.getCharacterClass()));
        } else {
            retVal = null;
        }

        // Deal with the deathbringer proc before doing anything with mults -- Crit and Arp are handled separately due to being capped
        statsToAdd.setStrength(statsToAdd.getStrength() + statsToAdd.getDeathbringerProc());
        statsToAdd.setHasteRating(statsToAdd.getHasteRating() + statsToAdd.getDeathbringerProc());

        // Base Stats
        statsToAdd.setStamina(statsToAdd.getStamina() * newStaMult);
        statsToAdd.setStrength(statsToAdd.getStrength() * newStrMult);
        statsToAdd.setAgility(statsToAdd.getAgility() * newAgiMult);

        if (retVal != null) {
            // change retvals to use the new mults.  Combines Stat/=oldMult; Stat*=newMult
            retVal.setStamina(retVal.getStamina() * (newStaMult / (1f + retVal.getBonusStaminaMultiplier())));
            retVal.setStrength(retVal.getStrength() * (newStrMult / (1f + retVal.getBonusStrengthMultiplier())));
            retVal.setAgility(retVal.getAgility() * (newAgiMult / (1f + retVal.getBonusAgilityMultiplier())));
        }

        // Health
        statsToAdd.setHealth(statsToAdd.getHealth() * newHealthMult);
        statsToAdd.setHealth(statsToAdd.getHealth() + (statsToAdd.getStamina() * 10f));
        if (retVal != null) {
            // Combines rollback of oldmult and addition of newmult
            retVal.setHealth(retVal.getHealth() * (newHealthMult / (1f + retVal.getBonusHealthMultiplier())));
            retVal.setHealth(retVal.getHealth() + retVal.getStamina() * 10f);
        }

        // Armor
        statsToAdd.setBonusArmor(statsToAdd.getBonusArmor() + statsToAdd.getAgility() * 2f);
        statsToAdd.setArmor((statsToAdd.getArmor() * newBaseArmMult + statsToAdd.getBonusArmor()) * newArmMult);
        if (retVal != null) {
            retVal.setBonusArmor(retVal.getBonusArmor() + retVal.getAgility() * 2f);
            retVal.setArmor((retVal.getArmor() * newBaseArmMult + retVal.getBonusArmor()) * newArmMult);
        }

        // Attack Power
        // stats to add
        statsToAdd.setAttackPower(statsToAdd.getAttackPower() + ((statsToAdd.getStrength() * 2f) +
                (statsToAdd.getArmor() / 108f * character.getWarriorTalents().getArmoredToTheTeeth())));
        statsToAdd.setAttackPower(statsToAdd.getAttackPower() * newAtkMult);
        // reset retval
        if (retVal != null) {
            // already rolled back AP's oldmult, so not combining
            retVal.setAttackPower(retVal.getAttackPower() + ((retVal.getStrength() * 2f) +
                    (retVal.getArmor() / 108f * character.getWarriorTalents().getArmoredToTheTeeth())));
            retVal.setAttackPower(retVal.getAttackPower() * newAtkMult);
        }

        // Crit
        statsToAdd.setPhysicalCrit(statsToAdd.getPhysicalCrit() + StatConversion.getCritFromAgility(statsToAdd.getAgility(), character.getCharacterClass()));
        statsToAdd.setPhysicalCrit(statsToAdd.getPhysicalCrit() + StatConversion.getCritFromRating(statsToAdd.getCritRating(), character.getCharacterClass()));
        if (retVal != null) {
            retVal.setPhysicalCrit(retVal.getPhysicalCrit() + StatConversion.getCritFromAgility(retVal.getAgility(), character.getCharacterClass()));
        }
        // Haste
        statsToAdd.setPhysicalHaste((1f + statsToAdd.getPhysicalHaste())
                * (1f + StatConversion.getPhysicalHasteFromRating(Math.max(0, statsToAdd.getHasteRating()), character.getCharacterClass()))
                - 1f);

        // If we're adding two, then return the .Accumulate
        if (retVal != null) {
            retVal.accumulate(statsToAdd);

            // Paragon and its friends
            if (retVal.getParagon() > 0f || retVal.getHighestStat() > 0f) {
                float paragonValue = retVal.getParagon() + retVal.getHighestStat(); // how much paragon to add
                retVal.setParagon(0f);
                retVal.setHighestStat(0f); // remove Paragon stat, since it's not needed
                if (retVal.getStrength() > retVal.getAgility()) { // Now that we've added the two stats, we run UpdateStatsAndAdd again for paragon
                    Stats paragonStats = new Stats();
                    paragonStats.setStrength(paragonValue);
                    return updateStatsAndAdd(paragonStats, retVal, character);
                } else {
                    Stats paragonStats = new Stats();
                    paragonStats.setAgility(paragonValue);
                    return updateStatsAndAdd(paragonStats, retVal, character);
                }
            } else {
                return retVal;
            }
        } else { // Just processing one, not adding two
            return statsToAdd;
        }
    }
}
