// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ShadowPriest/CalculationsShadowPriest.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// FRAMEWORK ADAPTATION: the System.Drawing.Color chart colors are ported as hex strings
// (LinkedHashMap<String, String>, the same convention as CalculationsHealPriest); the UI-only
// CalculationOptionsPanelShadowPriest is skipped. deserializeDataObject uses a JDK-XML reader
// instead of XmlSerializer (the fixture SpellPriority list is parsed explicitly).
package rawr.models.shadowpriest;

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
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class CalculationsShadowPriest extends CalculationsBase {

    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        // Interesting Gem Choices for a Shadow Priest
        // Red
        int[] runed = { 39911, 39998, 40113, 42144 }; // +spp

        // Orange
        int[] durable = { 39958, 40050, 40154 }; // +spp/+resilience
        int[] luminous = { 39946, 40047, 40151 }; // +spp/+int
        int[] potent = { 39956, 40048, 40152 }; // +spp/+crit
        int[] reckless = { 39959, 40051, 40155 }; // +spp/+haste
        int[] veiled = { 39957, 40049, 40153 }; // +spp/+hit

        // Yellow
        int[] brilliant = { 39912, 40012, 40123, 42148 }; // +int
        int[] mystic = { 39917, 40016, 40127, 42158 }; // +resilience
        int[] quick = { 39918, 40017, 40128, 42150 }; // +haste
        int[] rigid = { 39915, 40014, 40125, 42156 }; // +hit
        int[] smooth = { 39914, 40013, 40124, 42149 }; // +crit

        // Green
        int[] dazzling = { 39984, 40094, 40175 }; // +int/+mp5
        int[] energized = { 39989, 40105, 40179 }; // +haste/+mp5
        int[] forceful = { 39978, 40091, 40169 }; // +haste/+sta
        int[] intricate = { 39983, 40104, 40174 }; // +haste/+spi
        int[] jagged = { 39974, 40086, 40165 }; // +crit/+stamina
        int[] lambent = { 39986, 40100, 40177 }; // +hit/+mp5
        int[] misty = { 39980, 40095, 40171 }; // +crit/+spi
        int[] opaque = { 39988, 40103, 40178 }; // +resilience/+mp5
        int[] seers = { 39979, 40092, 40170 }; // +int/+spi
        int[] shining = { 39981, 40099, 40172 }; // +hit/+spi
        int[] steady = { 39977, 40090, 40168 }; // +resilience/+stamina
        int[] sundered = { 39985, 40096, 40176 }; // +crit/+mp5
        int[] timeless = { 39968, 40085, 40164 }; // +int/+stamina
        int[] turbid = { 39982, 40102, 40173 }; // +resilience/+spi
        int[] vivid = { 39975, 40088, 40166 }; // +hit/+stamina

        // Blue
        int[] lustrous = { 39927, 40010, 40121, 42146 }; // +mp5
        int[] solid = { 39919, 40008, 40119, 36767 }; // +stamina
        int[] sparkling = { 39920, 40009, 40120, 42145 }; // +spirit

        // Purple
        int[] glowing = { 39936, 40025, 40132 }; // +spp/+stamina
        int[] purified = { 39941, 40026, 40133 }; // +spp/+spirit
        int[] royal = { 39943, 40027, 40134 }; // +spp/+mp5

        // Meta
        int[] chaotic = { 41285 }; // +spp/+3% crit damage
        int[] insightful = { 41401 }; // +int/manarestore
        int[] powerful = { 41397 }; // +stamina/-10% stun duration

        return new ArrayList<GemmingTemplate>(List.of(
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Uncommon"); // Forced Hit
                    setRedId(rigid[0]); setYellowId(rigid[0]); setBlueId(rigid[0]); setPrismaticId(rigid[0]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Uncommon"); // Balanced w/Hit
                    setRedId(veiled[0]); setYellowId(rigid[0]); setBlueId(shining[0]); setPrismaticId(rigid[0]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Uncommon"); // Power w/Hit
                    setRedId(runed[0]); setYellowId(veiled[0]); setBlueId(purified[0]); setPrismaticId(runed[0]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Uncommon"); // Full Power
                    setRedId(runed[0]); setYellowId(runed[0]); setBlueId(runed[0]); setPrismaticId(runed[0]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Uncommon"); // Power w/Haste
                    setRedId(runed[0]); setYellowId(reckless[0]); setBlueId(purified[0]); setPrismaticId(runed[0]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Uncommon"); // Power w/Crit
                    setRedId(runed[0]); setYellowId(potent[0]); setBlueId(purified[0]); setPrismaticId(runed[0]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Uncommon"); // Mana
                    setRedId(luminous[0]); setYellowId(brilliant[0]); setBlueId(seers[0]); setPrismaticId(brilliant[0]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Uncommon"); // PvP - Stamina
                    setRedId(glowing[0]); setYellowId(steady[0]); setBlueId(solid[0]); setPrismaticId(solid[0]); setMetaId(powerful[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Uncommon"); // PvP - Resilience
                    setRedId(durable[0]); setYellowId(mystic[0]); setBlueId(steady[0]); setPrismaticId(mystic[0]); setMetaId(powerful[0]); }},

                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Rare"); // Forced Hit
                    setRedId(rigid[1]); setYellowId(rigid[1]); setBlueId(rigid[1]); setPrismaticId(rigid[1]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Rare"); // Balanced w/Hit
                    setRedId(veiled[1]); setYellowId(rigid[1]); setBlueId(shining[1]); setPrismaticId(rigid[1]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Rare"); // Power w/Hit
                    setRedId(runed[1]); setYellowId(veiled[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Rare"); // Full Power
                    setRedId(runed[1]); setYellowId(runed[1]); setBlueId(runed[1]); setPrismaticId(runed[1]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Rare"); // Power w/Haste
                    setRedId(runed[1]); setYellowId(reckless[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Rare"); // Power w/Crit
                    setRedId(runed[1]); setYellowId(potent[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Rare"); // Mana
                    setRedId(luminous[1]); setYellowId(brilliant[1]); setBlueId(seers[1]); setPrismaticId(brilliant[1]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Rare"); // PvP - Stamina
                    setRedId(glowing[1]); setYellowId(steady[1]); setBlueId(solid[1]); setPrismaticId(solid[1]); setMetaId(powerful[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Rare"); // PvP - Resilience
                    setRedId(durable[1]); setYellowId(mystic[1]); setBlueId(steady[1]); setPrismaticId(mystic[1]); setMetaId(powerful[0]); }},

                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Epic"); // Forced Hit
                    setRedId(rigid[2]); setYellowId(rigid[2]); setBlueId(rigid[2]); setPrismaticId(rigid[2]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Epic"); setEnabled(true); // Balanced w/Hit
                    setRedId(veiled[2]); setYellowId(rigid[2]); setBlueId(shining[2]); setPrismaticId(rigid[2]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Epic"); setEnabled(true); // Power w/Hit
                    setRedId(runed[2]); setYellowId(veiled[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Epic"); setEnabled(true); // Full Power
                    setRedId(runed[2]); setYellowId(runed[2]); setBlueId(runed[2]); setPrismaticId(runed[2]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Epic"); setEnabled(true); // Power w/Haste
                    setRedId(runed[2]); setYellowId(reckless[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Epic"); setEnabled(true); // Power w/Crit
                    setRedId(runed[2]); setYellowId(potent[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Epic"); // Mana
                    setRedId(luminous[2]); setYellowId(brilliant[2]); setBlueId(seers[2]); setPrismaticId(brilliant[2]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Epic"); // PvP - Stamina
                    setRedId(glowing[2]); setYellowId(steady[2]); setBlueId(solid[2]); setPrismaticId(solid[2]); setMetaId(powerful[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Epic"); // PvP - Resilience
                    setRedId(durable[2]); setYellowId(mystic[2]); setBlueId(steady[2]); setPrismaticId(mystic[2]); setMetaId(powerful[0]); }},

                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Rare"); // Forced Hit
                    setRedId(rigid[1]); setYellowId(rigid[3]); setBlueId(rigid[1]); setPrismaticId(rigid[1]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Rare"); // Balanced w/Hit
                    setRedId(veiled[1]); setYellowId(rigid[3]); setBlueId(shining[1]); setPrismaticId(rigid[1]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Rare"); // Power w/Hit
                    setRedId(runed[3]); setYellowId(veiled[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Rare"); // Full Power
                    setRedId(runed[3]); setYellowId(runed[3]); setBlueId(runed[3]); setPrismaticId(runed[3]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Rare"); // Power w/Haste
                    setRedId(runed[3]); setYellowId(reckless[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Rare"); // Power w/Crit
                    setRedId(runed[3]); setYellowId(potent[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Rare"); // Mana
                    setRedId(luminous[1]); setYellowId(brilliant[3]); setBlueId(seers[1]); setPrismaticId(brilliant[1]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Rare"); // PvP - Stamina
                    setRedId(glowing[1]); setYellowId(steady[1]); setBlueId(solid[3]); setPrismaticId(solid[1]); setMetaId(powerful[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Rare"); // PvP - Resilience
                    setRedId(durable[1]); setYellowId(mystic[3]); setBlueId(steady[1]); setPrismaticId(mystic[1]); setMetaId(powerful[0]); }},

                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Epic"); // Forced Hit
                    setRedId(rigid[2]); setYellowId(rigid[3]); setBlueId(rigid[2]); setPrismaticId(rigid[2]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Epic"); // Balanced w/Hit
                    setRedId(veiled[2]); setYellowId(rigid[3]); setBlueId(shining[2]); setPrismaticId(rigid[2]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Epic"); // Power w/Hit
                    setRedId(runed[3]); setYellowId(veiled[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Epic"); // Full Power
                    setRedId(runed[3]); setYellowId(runed[3]); setBlueId(runed[3]); setPrismaticId(runed[3]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Epic"); // Power w/Haste
                    setRedId(runed[3]); setYellowId(reckless[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Epic"); // Power w/Crit
                    setRedId(runed[3]); setYellowId(potent[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(chaotic[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Epic"); // Mana
                    setRedId(luminous[2]); setYellowId(brilliant[3]); setBlueId(seers[2]); setPrismaticId(brilliant[2]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Epic"); // PvP - Stamina
                    setRedId(glowing[2]); setYellowId(steady[2]); setBlueId(solid[3]); setPrismaticId(solid[2]); setMetaId(powerful[0]); }},
                new GemmingTemplate() {{ setModel("ShadowPriest"); setGroup("Jeweler Epic"); // PvP - Resilience
                    setRedId(durable[2]); setYellowId(mystic[3]); setBlueId(steady[2]); setPrismaticId(mystic[2]); setMetaId(powerful[0]); }}
        ));
    }

    public void setDefaults(Character character) {
        character.activeBuffsAdd("Inner Fire");
        character.activeBuffsAdd("Improved Moonkin Form");
        character.activeBuffsAdd("Arcane Intellect");
        character.activeBuffsAdd("Vampiric Touch");
        character.activeBuffsAdd("Mana Spring Totem");
        character.activeBuffsAdd("Restorative Totems");
        character.activeBuffsAdd("Moonkin Form");
        character.activeBuffsAdd("Wrath of Air Totem");
        character.activeBuffsAdd("Totem of Wrath (Spell Power)");
        character.activeBuffsAdd("Divine Spirit");
        character.activeBuffsAdd("Power Word: Fortitude");
        character.activeBuffsAdd("Improved Power Word: Fortitude");
        character.activeBuffsAdd("Mark of the Wild");
        character.activeBuffsAdd("Improved Mark of the Wild");
        character.activeBuffsAdd("Blessing of Kings");
        character.activeBuffsAdd("Shadow Protection");
        character.activeBuffsAdd("Heart of the Crusader");
        character.activeBuffsAdd("Sanctified Retribution");
        character.activeBuffsAdd("Improved Scorch");
        character.activeBuffsAdd("Judgement of Wisdom");
        character.activeBuffsAdd("Earth and Moon");
        character.activeBuffsAdd("Misery");
        character.activeBuffsAdd("Crypt Fever");
        character.activeBuffsAdd("Flask of the Frost Wyrm");
        character.activeBuffsAdd("Spell Power Food");
        character.activeBuffsAdd("Heroism/Bloodlust");
    }

    private static List<String> relevantGlyphs = null;

    public List<String> getRelevantGlyphs() {
        if (relevantGlyphs == null) {
            relevantGlyphs = new ArrayList<String>();
            relevantGlyphs.add("Glyph of Dispel Magic");
            relevantGlyphs.add("Glyph of Dispersion");
            relevantGlyphs.add("Glyph of Inner Fire");
            relevantGlyphs.add("Glyph of Mass Dispel");
            relevantGlyphs.add("Glyph of Mind Flay");
            relevantGlyphs.add("Glyph of Mind Sear");
            relevantGlyphs.add("Glyph of Penance");
            relevantGlyphs.add("Glyph of Shadow");
            relevantGlyphs.add("Glyph of Shadow Word: Death");
            relevantGlyphs.add("Glyph of Shadow Word: Pain");
            relevantGlyphs.add("Glyph of Smite");
            relevantGlyphs.add("Glyph of Fading");
        }
        return relevantGlyphs;
    }

    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.Priest;
    }

    public String currentChartName = null;
    public float currentChartTotal = 0;

    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> _subPointNameColors = new LinkedHashMap<String, String>();
        if (currentChartName == null) currentChartName = "";
        switch (currentChartName) {
            case "Mana Sources":
                _subPointNameColors.put(String.format(java.util.Locale.ROOT, "MP5 Sources (%s Total)", rawr.base.util.NetFormat.fixed(currentChartTotal, 0)), "#0000FF");
                break;
            case "DPS Sources":
                _subPointNameColors.put(String.format(java.util.Locale.ROOT, "DPS Sources (%s%% total)", rawr.base.util.NetFormat.fixed(currentChartTotal, 0)), "#FF0000");
                break;
            case "Mana Usage":
                _subPointNameColors.put(String.format(java.util.Locale.ROOT, "Mana Usage (%s total)", rawr.base.util.NetFormat.fixed(currentChartTotal, 0)), "#0000FF");
                break;
            case "Haste Rating Gain":
                _subPointNameColors.put("DPS", "#FF0000");
                break;
            default:
                _subPointNameColors.put("DPS", "#FF0000");
                _subPointNameColors.put("Survivability", "#008000");
                break;
        }
        currentChartName = null;
        return _subPointNameColors;
    }

    private String[] characterDisplayCalculationLabels = null;

    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        if (this.characterDisplayCalculationLabels == null)
            this.characterDisplayCalculationLabels = new String[] {
                    "Basic Stats:Health",
                    "Basic Stats:Resilience",
                    "Basic Stats:Mana",
                    "Basic Stats:Stamina",
                    "Basic Stats:Intellect",
                    "Basic Stats:Spirit",
                    "Basic Stats:Spell Power",
                    "Basic Stats:Regen",
                    "Basic Stats:Crit",
                    "Basic Stats:Hit",
                    "Basic Stats:Haste",
                    "Basic Stats:Armor",
                    "Basic Stats:Resistance",
                    "Simulation:Rotation",
                    "Simulation:Castlist",
                    "Simulation:DPS",
                    "Shadow:Vampiric Touch",
                    "Shadow:SW Pain",
                    "Shadow:Devouring Plague",
                    "Shadow:Imp. Devouring Plague",
                    "Shadow:SW Death",
                    "Shadow:Mind Blast",
                    "Shadow:Mind Flay",
                    "Shadow:Shadowfiend",
                    "Holy:PW Shield",
                    "Holy:Smite",
                    "Holy:Holy Fire",
                    "Holy:Penance"
            };
        return List.of(this.characterDisplayCalculationLabels);
    }

    private String[] optimizableCalculationLabels = null;

    public String[] getOptimizableCalculationLabels() {
        if (this.optimizableCalculationLabels == null)
            this.optimizableCalculationLabels = new String[] {
                    "Health",
                    "Resilience",
                    "Mana",
                    "Haste Rating",
                    "Haste %",
                    "Crit Rating",
                    "MB Crit %",
                    "Hit Rating",
                    "MF cast time (ms)",
                    "Armor",
                    "Arcane Resistance",
                    "Fire Resistance",
                    "Frost Resistance",
                    "Nature Resistance",
                    "Shadow Resistance",
            };
        return this.optimizableCalculationLabels;
    }

    private String[] customChartNames = null;

    public String[] getCustomChartNames() {
        if (this.customChartNames == null)
            this.customChartNames = new String[] { "Mana Sources", "DPS Sources", "Mana Usage", "Haste Rating Gain" };
        return this.customChartNames;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationShadowPriest();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsShadowPriest();
    }

    private List<ItemType> relevantItemTypes = null;

    @Override
    public List<ItemType> getRelevantItemTypes() {
        if (this.relevantItemTypes == null) {
            this.relevantItemTypes = new ArrayList<ItemType>(List.of(
                    ItemType.None,
                    ItemType.Cloth,
                    ItemType.Dagger,
                    ItemType.Wand,
                    ItemType.OneHandMace,
                    ItemType.Staff));
        }
        return this.relevantItemTypes;
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

    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        List<ComparisonCalculationBase> comparisonList = new ArrayList<ComparisonCalculationBase>();
        ComparisonCalculationBase comparison;

        currentChartTotal = 0;
        currentChartName = chartName;

        switch (chartName) {
            case "Mana Sources": {
                CharacterCalculationsShadowPriest mscalcs = (CharacterCalculationsShadowPriest) getCharacterCalculations(character);
                SolverBase mssolver = mscalcs.getSolver(character, mscalcs.getBasicStats());
                mssolver.calculate(mscalcs);
                for (SolverBase.ManaSource Source : mssolver.getManaSources()) {
                    comparison = createNewComparisonCalculation();
                    comparison.setName(Source.getName());
                    comparison.getSubPoints()[0] = Source.getValue() * 5;
                    currentChartTotal += comparison.getSubPoints()[0];
                    comparison.setOverallPoints(comparison.getSubPoints()[0]);
                    comparison.setEquipped(false);
                    comparisonList.add(comparison);
                }
                return comparisonList.toArray(new ComparisonCalculationBase[0]);
            }
            case "DPS Sources": {
                CharacterCalculationsShadowPriest dpscalcs = (CharacterCalculationsShadowPriest) getCharacterCalculations(character);
                SolverBase dpssolver = dpscalcs.getSolver(character, dpscalcs.getBasicStats());
                dpssolver.calculate(dpscalcs);
                float dpstotal = 0;
                for (Spell spell : dpssolver.getSpellPriority())
                    dpstotal += spell.getSpellStatistics().damageDone;
                for (Spell spell : dpssolver.getSpellPriority()) {
                    comparison = createNewComparisonCalculation();
                    comparison.setName(spell.getName());
                    comparison.getSubPoints()[0] = spell.getSpellStatistics().damageDone * 100f / dpstotal;
                    comparison.setOverallPoints(comparison.getSubPoints()[0]);
                    comparison.setEquipped(false);
                    comparisonList.add(comparison);
                }
                currentChartTotal = 100f;
                return comparisonList.toArray(new ComparisonCalculationBase[0]);
            }
            case "Mana Usage": {
                CharacterCalculationsShadowPriest mucalcs = (CharacterCalculationsShadowPriest) getCharacterCalculations(character);
                SolverBase musolver = mucalcs.getSolver(character, mucalcs.getBasicStats());
                musolver.calculate(mucalcs);
                for (Spell spell : musolver.getSpellPriority()) {
                    comparison = createNewComparisonCalculation();
                    comparison.setName(spell.getName());
                    comparison.getSubPoints()[0] = spell.getSpellStatistics().manaUsed * 5;
                    currentChartTotal += comparison.getSubPoints()[0];
                    comparison.setOverallPoints(comparison.getSubPoints()[0]);
                    comparison.setEquipped(false);
                    comparisonList.add(comparison);
                }
                return comparisonList.toArray(new ComparisonCalculationBase[0]);
            }
            case "Haste Rating Gain": {
                CharacterCalculationsShadowPriest hrbase = (CharacterCalculationsShadowPriest) getCharacterCalculations(character);
                for (int x = 0; x < 100; x++) {
                    Item item = new Item();
                    Stats itemStats = new Stats();
                    itemStats.setHasteRating(x);
                    item.setStats(itemStats);
                    CharacterCalculationsShadowPriest hrnew = (CharacterCalculationsShadowPriest) getCharacterCalculations(character, item);
                    comparison = createNewComparisonCalculation();
                    comparison.setName(String.format(java.util.Locale.ROOT, "%s Haste Rating", x));
                    comparison.getSubPoints()[0] = hrnew.getDpsPoints() - hrbase.getDpsPoints();
                    comparison.setEquipped(false);
                    comparisonList.add(comparison);
                }
                return comparisonList.toArray(new ComparisonCalculationBase[0]);
            }
            default:
                currentChartName = null;
                return new ComparisonCalculationBase[0];
        }
    }

    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        if (referenceCalculation && significantChange) {
            // Fresh top-level character calculation: drop shared effect interpolation caches so a
            // previous character cannot leak into this one (DECISIONS #28).
            rawr.base.effects.SpecialEffect.clearInterpolatorCaches();
        }
        Stats stats = getCharacterStats(character, additionalItem);
        CharacterCalculationsShadowPriest calculatedStats = new CharacterCalculationsShadowPriest();
        CalculationOptionsShadowPriest calculationOptions = character.getCalculationOptions() instanceof CalculationOptionsShadowPriest
                ? (CalculationOptionsShadowPriest) character.getCalculationOptions()
                : null;

        calculatedStats.setRace(character.getRace());
        calculatedStats.setBasicStats(stats);
        calculatedStats.setCharacter(character);

        calculatedStats.setSpiritRegen((float) Math.floor(5f * StatConversion.getSpiritRegenSec(calculatedStats.getBasicStats().getSpirit(), calculatedStats.getBasicStats().getIntellect())));
        calculatedStats.setRegenInFSR(calculatedStats.getSpiritRegen() * calculatedStats.getBasicStats().getSpellCombatManaRegeneration() + calculatedStats.getBasicStats().getMp5());
        calculatedStats.setRegenOutFSR(calculatedStats.getSpiritRegen() + calculatedStats.getBasicStats().getMp5());

        SolverBase solver = calculatedStats.getSolver(character, stats);
        solver.calculate(calculatedStats);

        return calculatedStats;
    }

    public Stats getCharacterStats(Character character, Item additionalItem) {
        CalculationOptionsShadowPriest calcOpts = character.getCalculationOptions() instanceof CalculationOptionsShadowPriest
                ? (CalculationOptionsShadowPriest) character.getCalculationOptions()
                : new CalculationOptionsShadowPriest();
        Stats statsRace = BaseStats.getBaseStats(character);
        Stats statsBaseGear = new Stats();
        accumulateItemStats(statsBaseGear, character, additionalItem);
        Stats statsBuffs = getBuffsStats(character, calcOpts);
        float SpellDamageFromSpiritPercentage = character.getPriestTalents().getSpiritualGuidance() * 0.05f + character.getPriestTalents().getTwistedFaith() * 0.04f;

        Stats statsTotal = new Stats();
        statsTotal.setBonusStaminaMultiplier(character.getPriestTalents().getImprovedPowerWordFortitude() * 0.02f);
        statsTotal.setBonusSpiritMultiplier((1 + character.getPriestTalents().getEnlightenment() * 0.02f) * (1f + character.getPriestTalents().getSpiritOfRedemption() * 0.05f) - 1f);
        statsTotal.setBonusIntellectMultiplier(character.getPriestTalents().getMentalStrength() * 0.03f);
        statsTotal.setSpellHaste(character.getPriestTalents().getEnlightenment() * 0.02f);
        statsTotal.setSpellCombatManaRegeneration(character.getPriestTalents().getMeditation() * 0.5f / 3f);
        statsTotal.setSpellCrit(character.getPriestTalents().getFocusedWill() * 0.01f);

        statsTotal.accumulate(statsBaseGear);
        statsTotal.accumulate(statsBuffs);
        statsTotal.accumulate(statsRace);

        statsTotal.setStamina((float) Math.floor((statsTotal.getStamina()) * (1 + statsTotal.getBonusStaminaMultiplier())));
        statsTotal.setIntellect((float) Math.floor(statsTotal.getIntellect() * (1 + statsTotal.getBonusIntellectMultiplier())));
        statsTotal.setSpirit((float) Math.floor((statsTotal.getSpirit()) * (1 + statsTotal.getBonusSpiritMultiplier())));
        statsTotal.setSpellPower(statsTotal.getSpellPower() + (SpellDamageFromSpiritPercentage * statsTotal.getSpirit()
                + (statsTotal.getPriestInnerFire() > 0 ? getInnerFireSpellPowerBonus(character) : 0f)));
        statsTotal.setMana(statsTotal.getMana() + StatConversion.getManaFromIntellect(statsTotal.getIntellect()));
        statsTotal.setHealth(statsTotal.getHealth() + StatConversion.getHealthFromStamina(statsTotal.getStamina()));
        statsTotal.setSpellCrit(statsTotal.getSpellCrit() + ((StatConversion.getSpellCritFromIntellect(statsTotal.getIntellect())
                + StatConversion.getSpellCritFromRating(statsTotal.getCritRating())) + statsTotal.getSpellCritOnTarget()));
        statsTotal.setSpellHaste((1f + statsTotal.getSpellHaste()) * (1f + StatConversion.getSpellHasteFromRating(statsTotal.getHasteRating())) - 1f);
        statsTotal.setSpellHit(statsTotal.getSpellHit() + StatConversion.getSpellHitFromRating(statsTotal.getHitRating()));
        statsTotal.setBonusArmor(statsTotal.getBonusArmor() + (statsTotal.getAgility() * 2f + (statsTotal.getPriestInnerFire() > 0 ? getInnerFireArmorBonus(character) : 0f)));

        return statsTotal;
    }

    public Stats getBuffsStats(Character character, CalculationOptionsShadowPriest calcOpts) {
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

    public static float getInnerFireSpellPowerBonus(Character character) {
        float InnerFireSpellPowerBonus = 0;
        if (character.getLevel() >= 77)
            InnerFireSpellPowerBonus = 120;
        else if (character.getLevel() >= 71)
            InnerFireSpellPowerBonus = 95;
        return InnerFireSpellPowerBonus * (1f + character.getPriestTalents().getImprovedInnerFire() * 0.15f);
    }

    public static float getInnerFireArmorBonus(Character character) {
        float ArmorBonus = 2440 * (character.getPriestTalents().isGlyphofInnerFire() ? 1.5f : 1f);

        return ArmorBonus * (1f + character.getPriestTalents().getImprovedInnerFire() * 0.15f);
    }

    public Stats getRelevantStats(Stats stats) {
        Stats s = new Stats();
        s.setStamina(stats.getStamina());
        s.setHealth(stats.getHealth());
        s.setResilience(stats.getResilience());
        s.setIntellect(stats.getIntellect());
        s.setMana(stats.getMana());
        s.setSpirit(stats.getSpirit());
        s.setMp5(stats.getMp5());
        s.setSpellPower(stats.getSpellPower());
        s.setSpellShadowDamageRating(stats.getSpellShadowDamageRating());
        s.setCritRating(stats.getCritRating());
        s.setSpellCrit(stats.getSpellCrit());
        s.setSpellCritOnTarget(stats.getSpellCritOnTarget());
        s.setHitRating(stats.getHitRating());
        s.setSpellHit(stats.getSpellHit());
        s.setSpellHaste(stats.getSpellHaste());
        s.setHasteRating(stats.getHasteRating());
        s.setBonusSpiritMultiplier(stats.getBonusSpiritMultiplier());
        s.setBonusIntellectMultiplier(stats.getBonusIntellectMultiplier());
        s.setBonusManaPotion(stats.getBonusManaPotion());
        s.setThreatReductionMultiplier(stats.getThreatReductionMultiplier());
        s.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());
        s.setBonusShadowDamageMultiplier(stats.getBonusShadowDamageMultiplier());
        s.setBonusHolyDamageMultiplier(stats.getBonusHolyDamageMultiplier());
        s.setBonusDiseaseDamageMultiplier(stats.getBonusDiseaseDamageMultiplier());
        s.setPriestInnerFire(stats.getPriestInnerFire());
        s.setMovementSpeed(stats.getMovementSpeed());
        s.setSWPDurationIncrease(stats.getSWPDurationIncrease());
        s.setBonusMindBlastMultiplier(stats.getBonusMindBlastMultiplier());
        s.setMindBlastCostReduction(stats.getMindBlastCostReduction());
        s.setShadowWordDeathCritIncrease(stats.getShadowWordDeathCritIncrease());
        s.setWeakenedSoulDurationDecrease(stats.getWeakenedSoulDurationDecrease());
        s.setDevouringPlagueBonusDamage(stats.getDevouringPlagueBonusDamage());
        s.setMindBlastHasteProc(stats.getMindBlastHasteProc());
        s.setPriestDPS_T9_2pc(stats.getPriestDPS_T9_2pc());
        s.setPriestDPS_T9_4pc(stats.getPriestDPS_T9_4pc());
        s.setPriestDPS_T10_2pc(stats.getPriestDPS_T10_2pc());
        s.setPriestDPS_T10_4pc(stats.getPriestDPS_T10_4pc());
        s.setBonusSpellCritMultiplier(stats.getBonusSpellCritMultiplier());
        s.setManaRestore(stats.getManaRestore());
        s.setSpellsManaReduction(stats.getSpellsManaReduction());
        s.setHighestStat(stats.getHighestStat());
        s.setArcaneDamage(stats.getArcaneDamage());
        s.setFireDamage(stats.getFireDamage());
        s.setFrostDamage(stats.getFrostDamage());
        s.setHolyDamage(stats.getHolyDamage());
        s.setNatureDamage(stats.getNatureDamage());
        s.setShadowDamage(stats.getShadowDamage());
        s.setValkyrDamage(stats.getValkyrDamage());

        s.setArmor(stats.getArmor());
        s.setBonusArmor(stats.getBonusArmor());
        s.setAgility(stats.getAgility());
        s.setArcaneResistance(stats.getArcaneResistance());
        s.setArcaneResistanceBuff(stats.getArcaneResistanceBuff());
        s.setFireResistance(stats.getFireResistance());
        s.setFireResistanceBuff(stats.getFireResistanceBuff());
        s.setFrostResistance(stats.getFrostResistance());
        s.setFrostResistanceBuff(stats.getFrostResistanceBuff());
        s.setNatureResistance(stats.getNatureResistance());
        s.setNatureResistanceBuff(stats.getNatureResistanceBuff());
        s.setShadowResistance(stats.getShadowResistance());
        s.setShadowResistanceBuff(stats.getShadowResistanceBuff());

        for (SpecialEffect se : stats.specialEffects())
            if (relevantTrinket(se))
                s.addSpecialEffect(se);

        return s;
    }

    protected boolean relevantTrinket(SpecialEffect se) {
        if (se.getTrigger() == Trigger.DamageSpellCrit
                || se.getTrigger() == Trigger.DamageSpellHit
                || se.getTrigger() == Trigger.DamageSpellCast
                || se.getTrigger() == Trigger.DoTTick
                || se.getTrigger() == Trigger.SpellHit
                || se.getTrigger() == Trigger.SpellCast
                || se.getTrigger() == Trigger.SpellCrit
                || se.getTrigger() == Trigger.SpellMiss
                || se.getTrigger() == Trigger.Use) {
            boolean bRelevant = _hasRelevantStats(se.getStats());
            for (SpecialEffect s : se.getStats().specialEffects())
                bRelevant |= relevantTrinket(s);
            return bRelevant;
        }
        return false;
    }

    protected boolean _hasRelevantStats(Stats stats) {
        boolean Yes = (
                stats.getIntellect() + stats.getMana() + stats.getSpirit() + stats.getMp5() + stats.getSpellPower()
                + stats.getSpellShadowDamageRating() + stats.getCritRating()
                + stats.getSpellCrit() + stats.getHitRating() + stats.getSpellHit() + stats.getSpellCritOnTarget()
                + stats.getSpellHaste() + stats.getHasteRating()

                + stats.getBonusSpiritMultiplier()
                + stats.getBonusIntellectMultiplier() + stats.getBonusManaPotion()
                + stats.getThreatReductionMultiplier() + stats.getBonusDamageMultiplier()
                + stats.getBonusShadowDamageMultiplier() + stats.getBonusHolyDamageMultiplier()
                + stats.getBonusDiseaseDamageMultiplier()
                + stats.getPriestInnerFire() + stats.getMovementSpeed()

                + stats.getSWPDurationIncrease() + stats.getBonusMindBlastMultiplier()
                + stats.getMindBlastCostReduction() + stats.getShadowWordDeathCritIncrease()
                + stats.getWeakenedSoulDurationDecrease()
                + stats.getDevouringPlagueBonusDamage() + stats.getMindBlastHasteProc()
                + stats.getPriestDPS_T9_2pc() + stats.getPriestDPS_T9_4pc()
                + stats.getPriestDPS_T10_2pc() + stats.getPriestDPS_T10_4pc()
                + stats.getManaRestoreFromBaseManaPPM() + stats.getBonusSpellCritMultiplier()
                + stats.getManaRestore() + stats.getSpellsManaReduction() + stats.getHighestStat()
                + stats.getArcaneDamage() + stats.getFireDamage() + stats.getFrostDamage()
                + stats.getHolyDamage() + stats.getNatureDamage() + stats.getShadowDamage()
                + stats.getValkyrDamage()
        ) > 0;

        boolean Maybe = (
                stats.getStamina() + stats.getHealth() + stats.getResilience()
                + stats.getArmor() + stats.getBonusArmor() + stats.getAgility()
                + stats.getArcaneResistance() + stats.getArcaneResistanceBuff()
                + stats.getFireResistance() + stats.getFireResistanceBuff()
                + stats.getFrostResistance() + stats.getFrostResistanceBuff()
                + stats.getNatureResistance() + stats.getNatureResistanceBuff()
                + stats.getShadowResistance() + stats.getShadowResistanceBuff()
        ) > 0;

        boolean No = (
                stats.getStrength() + stats.getAttackPower()
                + stats.getArmorPenetration() + stats.getArmorPenetrationRating()
                + stats.getExpertiseRating()
                + stats.getDodge() + stats.getDodgeRating()
                + stats.getParry() + stats.getParryRating()
                + stats.getDefense() + stats.getDefenseRating()
        ) > 0;

        return Yes || (Maybe && !No);
    }

    @Override
    public boolean hasRelevantStats(Stats stats) {
        boolean isRelevant = _hasRelevantStats(stats);

        for (SpecialEffect se : stats.specialEffects()) {
            isRelevant |= relevantTrinket(se);
            if (isRelevant) break;
        }
        return isRelevant;
    }

    private static CalculationsShadowPriest instance;

    public static CalculationsShadowPriest getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("ShadowPriest", CalculationsShadowPriest.class);
    }

    public static void register() {
        Calculations.registerModel("ShadowPriest", CalculationsShadowPriest.class);
    }

    public CalculationsShadowPriest() {
        instance = this;
    }

    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            org.w3c.dom.Element root = document.getDocumentElement();
            CalculationOptionsShadowPriest calcOpts = new CalculationOptionsShadowPriest();
            for (org.w3c.dom.Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                org.w3c.dom.Element child = (org.w3c.dom.Element) node;
                String name = child.getTagName();
                String text = child.getTextContent().trim();
                switch (name) {
                    case "TargetLevel": calcOpts.setTargetLevel(text.isEmpty() ? 0 : Integer.parseInt(text)); break;
                    case "FightLength": calcOpts.setFightLength(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "MoveFrequency": calcOpts.setMoveFrequency(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "MoveDuration": calcOpts.setMoveDuration(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "FSRRatio": calcOpts.setFSRRatio(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "Delay": calcOpts.setDelay(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "Shadowfiend": calcOpts.setShadowfiend(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "Replenishment": calcOpts.setReplenishment(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "JoW": calcOpts.setJoW(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "Survivability": calcOpts.setSurvivability(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "PTR": calcOpts.setPTR(Boolean.parseBoolean(text)); break;
                    case "SpellPriority": {
                        List<String> priority = new ArrayList<String>();
                        for (org.w3c.dom.Node n = child.getFirstChild(); n != null; n = n.getNextSibling()) {
                            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                            if (n.getNodeName().equals("string")) priority.add(n.getTextContent());
                        }
                        calcOpts.setSpellPriority(priority);
                        break;
                    }
                    case "ManaPot": calcOpts.setManaPot(text.isEmpty() ? 0 : Integer.parseInt(text)); break;
                    default: break;
                }
            }
            return calcOpts;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
