// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.HealPriest/CalculationsHealPriest.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: UI-only members omitted (CalculationOptionsPanel and the RAWR3/WPF color branches);
// the non-Silverlight display label/color/optimizable/chart/glyph tables are ported additively as
// getCharacterDisplayCalculationLabels()/getSubPointNameColors()/getOptimizableCalculationLabels()/
// getCustomChartNames()/getCustomChartData()/getRelevantGlyphs().
// FRAMEWORK ADAPTATION: DeserializeDataObject uses a JDK-XML reflective reader instead of
// XmlSerializer (same approach as CalculationsHealadin).
package rawr.models.healpriest;

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

public class CalculationsHealPriest extends CalculationsBase {
    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        // Interesting Gem Choices for a Holy & Discipline Priest
        // Red
        int[] runed = { 39911, 39998, 40113, 42144 }; // +spp

        // Orange
        int[] durable = { 39958, 40050, 40154 }; // +spp/+resilience
        int[] luminous = { 39946, 40047, 40151 }; // +spp/+int
        int[] potent = { 39956, 40048, 40152 }; // +spp/+crit
        int[] reckless = { 39959, 40051, 40155 }; // +spp/+haste

        // Yellow
        int[] brilliant = { 39912, 40012, 40123, 42148 }; // +int
        int[] mystic = { 39917, 40016, 40127, 42158 }; // +resilience
        int[] quick = { 39918, 40017, 40128, 42150 }; // +haste
        int[] smooth = { 39914, 40013, 40124, 42149 }; // +crit

        // Green
        int[] dazzling = { 39984, 40094, 40175 }; // +int/+mp5
        int[] energized = { 39989, 40105, 40179 }; // +haste/+mp5
        int[] forceful = { 39978, 40091, 40169 }; // +haste/+sta
        int[] intricate = { 39983, 40104, 40174 }; // +haste/+spi
        int[] jagged = { 39974, 40086, 40165 }; // +crit/+stamina
        int[] misty = { 39980, 40095, 40171 }; // +crit/+spi
        int[] opaque = { 39988, 40103, 40178 }; // +resilience/+mp5
        int[] seers = { 39979, 40092, 40170 }; // +int/+spi
        int[] steady = { 39977, 40090, 40168 }; // +resilience/+stamina
        int[] sundered = { 39985, 40096, 40176 }; // +crit/+mp5
        int[] timeless = { 39968, 40085, 40164 }; // +int/+stamina
        int[] turbid = { 39982, 40102, 40173 }; // +resilience/+spi

        // Blue
        int[] lustrous = { 39927, 40010, 40121, 42146 }; // +mp5
        int[] solid = { 39919, 40008, 40119, 36767 }; // +stamina
        int[] sparkling = { 39920, 40009, 40120, 42145 }; // +spirit

        // Purple
        int[] glowing = { 39936, 40025, 40132 }; // +spp/+stamina
        int[] purified = { 39941, 40026, 40133 }; // +spp/+spirit
        int[] royal = { 39943, 40027, 40134 }; // +spp/+mp5

        // Meta
        int[] beaming = { 41389 }; // +crit rating/+2% mana
        int[] ember = { 41333 }; // +spp/+2% int
        int[] insightful = { 41401 }; // +int/manarestore
        int[] revitalizing = { 41376 }; // +mp5/+3% heal effect
        int[] powerful = { 41397 }; // +stamina/-10% stun duration

        return new ArrayList<GemmingTemplate>(List.of(
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Uncommon"); // Max Mana
                    setRedId(luminous[0]); setYellowId(brilliant[0]); setBlueId(seers[0]); setPrismaticId(brilliant[0]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Uncommon"); // Max Mana Regen
                    setRedId(purified[0]); setYellowId(seers[0]); setBlueId(sparkling[0]); setPrismaticId(sparkling[0]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Uncommon"); // Max SPP
                    setRedId(runed[0]); setYellowId(luminous[0]); setBlueId(purified[0]); setPrismaticId(runed[0]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Uncommon"); // Max SPP w/ember
                    setRedId(runed[0]); setYellowId(luminous[0]); setBlueId(purified[0]); setPrismaticId(runed[0]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Uncommon"); // Max SPP w/revitalizing
                    setRedId(runed[0]); setYellowId(luminous[0]); setBlueId(purified[0]); setPrismaticId(runed[0]); setMetaId(revitalizing[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Uncommon"); // Max Haste
                    setRedId(runed[0]); setYellowId(reckless[0]); setBlueId(intricate[0]); setPrismaticId(runed[0]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Uncommon"); // Max Crit
                    setRedId(runed[0]); setYellowId(potent[0]); setBlueId(misty[0]); setPrismaticId(runed[0]); setMetaId(beaming[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Uncommon"); // Max Blast
                    setRedId(runed[0]); setYellowId(runed[0]); setBlueId(runed[0]); setPrismaticId(runed[0]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Uncommon"); // Max Blast w/ember
                    setRedId(runed[0]); setYellowId(runed[0]); setBlueId(runed[0]); setPrismaticId(runed[0]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Uncommon"); // PvP - Stamina
                    setRedId(glowing[0]); setYellowId(steady[0]); setBlueId(solid[0]); setPrismaticId(solid[0]); setMetaId(powerful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Uncommon"); // PvP - Resilience
                    setRedId(durable[0]); setYellowId(mystic[0]); setBlueId(steady[0]); setPrismaticId(mystic[0]); setMetaId(powerful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Uncommon"); // Max Mana w/MP5
                    setRedId(luminous[0]); setYellowId(brilliant[0]); setBlueId(dazzling[0]); setPrismaticId(brilliant[0]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Uncommon"); // Max Mana Regen
                    setRedId(royal[0]); setYellowId(dazzling[0]); setBlueId(lustrous[0]); setPrismaticId(lustrous[0]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Uncommon"); // Max SPP
                    setRedId(runed[0]); setYellowId(luminous[0]); setBlueId(royal[0]); setPrismaticId(runed[0]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Uncommon"); // Max SPP w/revitalizing
                    setRedId(runed[0]); setYellowId(luminous[0]); setBlueId(royal[0]); setPrismaticId(runed[0]); setMetaId(revitalizing[0]); }},

                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Rare"); // Max Mana
                    setRedId(luminous[1]); setYellowId(brilliant[1]); setBlueId(seers[1]); setPrismaticId(brilliant[1]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Rare"); // Max Mana Regen
                    setRedId(purified[1]); setYellowId(seers[1]); setBlueId(sparkling[1]); setPrismaticId(sparkling[1]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Rare"); // Max SPP
                    setRedId(runed[1]); setYellowId(luminous[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Rare"); // Max SPP w/ember
                    setRedId(runed[1]); setYellowId(luminous[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Rare"); // Max SPP w/revitalizing
                    setRedId(runed[1]); setYellowId(luminous[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(revitalizing[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Rare"); // Max Haste
                    setRedId(runed[1]); setYellowId(reckless[1]); setBlueId(intricate[1]); setPrismaticId(runed[1]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Rare"); // Max Crit
                    setRedId(runed[1]); setYellowId(potent[1]); setBlueId(misty[1]); setPrismaticId(runed[1]); setMetaId(beaming[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Rare"); // Max Blast
                    setRedId(runed[1]); setYellowId(runed[1]); setBlueId(runed[1]); setPrismaticId(runed[1]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Rare"); // Max Blast w/ember
                    setRedId(runed[1]); setYellowId(runed[1]); setBlueId(runed[1]); setPrismaticId(runed[1]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Rare"); // PvP - Stamina
                    setRedId(glowing[1]); setYellowId(steady[1]); setBlueId(solid[1]); setPrismaticId(solid[1]); setMetaId(powerful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Rare"); // PvP - Resilience
                    setRedId(durable[1]); setYellowId(mystic[1]); setBlueId(steady[1]); setPrismaticId(mystic[1]); setMetaId(powerful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Rare"); // Max Mana w/MP5
                    setRedId(luminous[1]); setYellowId(brilliant[1]); setBlueId(dazzling[1]); setPrismaticId(brilliant[1]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Rare"); // Max Mana Regen
                    setRedId(royal[1]); setYellowId(dazzling[1]); setBlueId(lustrous[1]); setPrismaticId(lustrous[1]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Rare"); // Max SPP
                    setRedId(runed[1]); setYellowId(luminous[1]); setBlueId(royal[1]); setPrismaticId(runed[1]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Rare"); // Max SPP w/revitalizing
                    setRedId(runed[1]); setYellowId(luminous[1]); setBlueId(royal[1]); setPrismaticId(runed[1]); setMetaId(revitalizing[0]); }},

                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Epic"); setEnabled(true); // Max Mana
                    setRedId(luminous[2]); setYellowId(brilliant[2]); setBlueId(seers[2]); setPrismaticId(brilliant[2]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Epic"); // Max Mana Regen
                    setRedId(purified[2]); setYellowId(seers[2]); setBlueId(sparkling[2]); setPrismaticId(sparkling[2]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Epic"); setEnabled(true); // Max SPP
                    setRedId(runed[2]); setYellowId(luminous[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Epic"); setEnabled(true); // Max SPP w/ember
                    setRedId(runed[2]); setYellowId(luminous[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Epic"); setEnabled(true); // Max SPP w/revitalizing
                    setRedId(runed[2]); setYellowId(luminous[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(revitalizing[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Epic"); // Max Haste
                    setRedId(runed[2]); setYellowId(reckless[2]); setBlueId(intricate[2]); setPrismaticId(runed[2]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Epic"); // Max Crit
                    setRedId(runed[2]); setYellowId(potent[2]); setBlueId(misty[2]); setPrismaticId(runed[2]); setMetaId(beaming[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Epic"); setEnabled(true); // Max Blast
                    setRedId(runed[2]); setYellowId(runed[2]); setBlueId(runed[2]); setPrismaticId(runed[2]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Epic"); // Max Blast w/ember
                    setRedId(runed[2]); setYellowId(runed[2]); setBlueId(runed[2]); setPrismaticId(runed[2]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Epic"); // PvP - Stamina
                    setRedId(glowing[2]); setYellowId(steady[2]); setBlueId(solid[2]); setPrismaticId(solid[2]); setMetaId(powerful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Epic"); // PvP - Resilience
                    setRedId(durable[2]); setYellowId(mystic[2]); setBlueId(steady[2]); setPrismaticId(mystic[2]); setMetaId(powerful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Epic"); // Max Mana w/MP5
                    setRedId(luminous[2]); setYellowId(brilliant[2]); setBlueId(dazzling[2]); setPrismaticId(brilliant[2]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Epic"); // Max Mana Regen
                    setRedId(royal[2]); setYellowId(dazzling[2]); setBlueId(lustrous[2]); setPrismaticId(lustrous[2]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Epic"); // Max SPP
                    setRedId(runed[2]); setYellowId(luminous[2]); setBlueId(royal[2]); setPrismaticId(runed[2]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Epic"); // Max SPP w/revitalizing
                    setRedId(runed[2]); setYellowId(luminous[2]); setBlueId(royal[2]); setPrismaticId(runed[2]); setMetaId(revitalizing[0]); }},

                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Rare"); // Max Mana
                    setRedId(luminous[1]); setYellowId(brilliant[3]); setBlueId(seers[1]); setPrismaticId(brilliant[1]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Rare"); // Max Mana Regen
                    setRedId(purified[1]); setYellowId(seers[1]); setBlueId(sparkling[3]); setPrismaticId(sparkling[1]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Rare"); // Max SPP
                    setRedId(runed[3]); setYellowId(luminous[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Rare"); // Max SPP w/ember
                    setRedId(runed[3]); setYellowId(luminous[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Rare"); // Max SPP w/revitalizing
                    setRedId(runed[3]); setYellowId(luminous[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(revitalizing[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Rare"); // Max Haste
                    setRedId(runed[3]); setYellowId(reckless[1]); setBlueId(intricate[1]); setPrismaticId(runed[1]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Rare"); // Max Crit
                    setRedId(runed[3]); setYellowId(potent[1]); setBlueId(misty[1]); setPrismaticId(runed[1]); setMetaId(beaming[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Rare"); // Max Blast
                    setRedId(runed[3]); setYellowId(runed[3]); setBlueId(runed[3]); setPrismaticId(runed[3]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Rare"); // Max Blast w/ember
                    setRedId(runed[3]); setYellowId(runed[3]); setBlueId(runed[3]); setPrismaticId(runed[3]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Rare"); // PvP - Stamina
                    setRedId(glowing[1]); setYellowId(steady[1]); setBlueId(solid[3]); setPrismaticId(solid[1]); setMetaId(powerful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Rare"); // PvP - Resilience
                    setRedId(durable[1]); setYellowId(mystic[3]); setBlueId(steady[1]); setPrismaticId(mystic[1]); setMetaId(powerful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Rare"); // Max Mana w/MP5
                    setRedId(luminous[1]); setYellowId(brilliant[3]); setBlueId(dazzling[1]); setPrismaticId(brilliant[1]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Rare"); // Max Mana Regen
                    setRedId(royal[1]); setYellowId(dazzling[1]); setBlueId(lustrous[3]); setPrismaticId(lustrous[1]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Rare"); // Max SPP
                    setRedId(runed[3]); setYellowId(luminous[1]); setBlueId(royal[1]); setPrismaticId(runed[1]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Rare"); // Max SPP w/revitalizing
                    setRedId(runed[3]); setYellowId(luminous[1]); setBlueId(royal[1]); setPrismaticId(runed[1]); setMetaId(revitalizing[0]); }},

                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Epic"); // Max Mana
                    setRedId(luminous[2]); setYellowId(brilliant[3]); setBlueId(seers[2]); setPrismaticId(brilliant[2]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Epic"); // Max Mana Regen
                    setRedId(purified[2]); setYellowId(seers[2]); setBlueId(sparkling[3]); setPrismaticId(sparkling[2]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Epic"); // Max SPP
                    setRedId(runed[3]); setYellowId(luminous[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Epic"); // Max SPP w/ember
                    setRedId(runed[3]); setYellowId(luminous[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Epic"); // Max SPP w/revitalizing
                    setRedId(runed[3]); setYellowId(luminous[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(revitalizing[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Epic"); // Max Haste
                    setRedId(runed[3]); setYellowId(reckless[2]); setBlueId(intricate[2]); setPrismaticId(runed[2]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Epic"); // Max Crit
                    setRedId(runed[3]); setYellowId(potent[2]); setBlueId(misty[2]); setPrismaticId(runed[2]); setMetaId(beaming[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Epic"); // Max Blast
                    setRedId(runed[3]); setYellowId(runed[3]); setBlueId(runed[3]); setPrismaticId(runed[3]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Epic"); // Max Blast w/ember
                    setRedId(runed[3]); setYellowId(runed[3]); setBlueId(runed[3]); setPrismaticId(runed[3]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Epic"); // PvP - Stamina
                    setRedId(glowing[2]); setYellowId(steady[2]); setBlueId(solid[3]); setPrismaticId(solid[2]); setMetaId(powerful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Epic"); // PvP - Resilience
                    setRedId(durable[2]); setYellowId(mystic[3]); setBlueId(steady[2]); setPrismaticId(mystic[2]); setMetaId(powerful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Epic"); // Max Mana w/MP5
                    setRedId(luminous[2]); setYellowId(brilliant[3]); setBlueId(dazzling[2]); setPrismaticId(brilliant[2]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Epic"); // Max Mana Regen
                    setRedId(royal[2]); setYellowId(dazzling[2]); setBlueId(lustrous[3]); setPrismaticId(lustrous[2]); setMetaId(insightful[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Epic"); // Max SPP
                    setRedId(runed[3]); setYellowId(luminous[2]); setBlueId(royal[2]); setPrismaticId(runed[2]); setMetaId(ember[0]); }},
                new GemmingTemplate() {{ setModel("HealPriest"); setGroup("Jeweler Epic"); // Max SPP w/revitalizing
                    setRedId(runed[3]); setYellowId(luminous[2]); setBlueId(royal[2]); setPrismaticId(runed[2]); setMetaId(revitalizing[0]); }}
        ));
    }

    public void setDefaults(Character character) {
        character.activeBuffsAdd("Inner Fire");
        character.activeBuffsAdd("Improved Moonkin Form");
        character.activeBuffsAdd("Tree of Life Aura");
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
        character.activeBuffsAdd("Flask of the Frost Wyrm");
        character.activeBuffsAdd("Spell Power Food");
    }

    private static List<String> relevantGlyphs = null;

    public List<String> getRelevantGlyphs() {
        if (relevantGlyphs == null) {
            relevantGlyphs = new ArrayList<String>();
            relevantGlyphs.add("Glyph of Circle of Healing");
            relevantGlyphs.add("Glyph of Flash Heal");
            relevantGlyphs.add("Glyph of Guardian Spirit");
            relevantGlyphs.add("Glyph of Holy Nova");
            relevantGlyphs.add("Glyph of Hymn of Hope");
            relevantGlyphs.add("Glyph of Inner Fire");
            relevantGlyphs.add("Glyph of Lightwell");
            relevantGlyphs.add("Glyph of Mass Dispel");
            relevantGlyphs.add("Glyph of Penance");
            relevantGlyphs.add("Glyph of Power Word: Shield");
            relevantGlyphs.add("Glyph of Prayer of Healing");
            relevantGlyphs.add("Glyph of Renew");
            relevantGlyphs.add("Glyph of Fading");
        }
        return relevantGlyphs;
    }

    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.Priest;
    }

    // C# CalculationsHealPriest.cs:314-354 CharacterDisplayCalculationLabels.
    private String[] characterDisplayCalculationLabels = null;

    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        if (this.characterDisplayCalculationLabels == null)
            this.characterDisplayCalculationLabels = new String[] {
                    "Basic Stats:Health",
                    "Basic Stats:Mana",
                    "Basic Stats:Stamina",
                    "Basic Stats:Resilience",
                    "Basic Stats:Intellect",
                    "Basic Stats:Spirit",
                    "Basic Stats:Spell Power",
                    "Basic Stats:In FSR MP5",
                    "Basic Stats:Spell Crit",
                    "Basic Stats:Healing Crit",
                    "Basic Stats:Spell Haste",
                    "Basic Stats:Armor",
                    "Basic Stats:Resistance",
                    "Simulation:Role",
                    "Simulation:Burst*This is the HPS you are expected to have if you are not limited by Mana.\r\nIn Custom Role, this displays your HPS when you dump all spells in 1 stream.",
                    "Simulation:Sustained*This is the HPS are expected to have when restricted by Mana.\r\nIf this value is lower than your Burst HPS, you are running out of mana in the simulation.\r\nIn Custom Role, this displays your HPS over the length of the fight, adjusted by the amount of mana available.",
                    "Spells:Greater Heal",
                    "Spells:Flash Heal",
                    "Spells:Binding Heal",
                    "Spells:Renew",
                    "Spells:Prayer of Mending",
                    "Spells:Power Word Shield",
                    "Spells:PoH",
                    "Spells:Holy Nova",
                    "Spells:Lightwell",
                    "Spells:CoH",
                    "Spells:Penance",
                    "Spells:Gift of the Naaru",
                    "Spells:Divine Hymn",
                    "Spells:Resurrection",
            };
        return List.of(this.characterDisplayCalculationLabels);
    }

    // C# CalculationsHealPriest.cs:356-385 OptimizableCalculationLabels.
    private String[] optimizableCalculationLabels = null;

    public String[] getOptimizableCalculationLabels() {
        if (this.optimizableCalculationLabels == null)
            this.optimizableCalculationLabels = new String[] {
                    "Health",
                    "Resilience",
                    "Mana",
                    "InFSR Regen",
                    "OutFSR Regen",
                    "Haste Rating",
                    "Haste %",
                    "Crit Rating",
                    "Healing Crit %",
                    "PW:Shield",
                    "GHeal Avg",
                    "FHeal Avg",
                    "CoH Avg",
                    "Armor",
                    "Arcane Resistance",
                    "Fire Resistance",
                    "Frost Resistance",
                    "Nature Resistance",
                    "Shadow Resistance",
            };
        return this.optimizableCalculationLabels;
    }

    // C# CalculationsHealPriest.cs:287-312 SubPointNameColors (non-Silverlight default branch).
    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> colors = new LinkedHashMap<String, String>();
        colors.put("HPS-Burst", "#FF0000");
        colors.put("HPS-Sustained", "#0000FF");
        colors.put("Survivability", "#008000");
        return colors;
    }

    // C# CalculationsHealPriest.cs:405-414 CustomChartNames.
    private String[] customChartNames = null;

    public String[] getCustomChartNames() {
        if (this.customChartNames == null)
            this.customChartNames = new String[] { "MP5 Sources", "Spell HpS", "Spell HpM", "Spell AoE HpS", "Spell AoE HpM" };
        return this.customChartNames;
    }

    public String currentChartName = null;
    public float currentChartTotal = 0;

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationHealPriest();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsHealPriest();
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

    // C# CalculationsHealPriest.cs:449-474 GetCharacterCalculations.
    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        if (referenceCalculation && significantChange) {
            // Fresh top-level character calculation: drop shared effect interpolation caches so a
            // previous character cannot leak into this one (SUSPECTED_BUGS #15).
            rawr.base.effects.SpecialEffect.clearInterpolatorCaches();
        }
        Stats stats = getCharacterStats(character, additionalItem);
        CharacterCalculationsHealPriest calculatedStats = new CharacterCalculationsHealPriest();
        CalculationOptionsHealPriest calculationOptions = character.getCalculationOptions() instanceof CalculationOptionsHealPriest
                ? (CalculationOptionsHealPriest) character.getCalculationOptions()
                : null;
        if (calculationOptions == null)
            return null;

        calculatedStats.setRace(character.getRace());
        calculatedStats.setBasicStats(stats);
        calculatedStats.setCharacter(character);

        calculatedStats.setSpiritRegen((float) Math.floor(5 * StatConversion.getSpiritRegenSec(calculatedStats.getBasicStats().getSpirit(), calculatedStats.getBasicStats().getIntellect())));
        calculatedStats.setRegenInFSR(calculatedStats.getSpiritRegen() * calculatedStats.getBasicStats().getSpellCombatManaRegeneration());
        calculatedStats.setRegenOutFSR(calculatedStats.getSpiritRegen());

        BaseSolver solver;
        if (calculationOptions.getRole() == eRole.CUSTOM)
            solver = new AdvancedSolver(stats, character);
        else
            solver = new Solver(stats, character);
        solver.calculate(calculatedStats);

        return calculatedStats;
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

    // C# CalculationsHealPriest.cs:493-528 GetCharacterStats.
    public Stats getCharacterStats(Character character, Item additionalItem) {
        CalculationOptionsHealPriest calcOpts = character.getCalculationOptions() instanceof CalculationOptionsHealPriest
                ? (CalculationOptionsHealPriest) character.getCalculationOptions()
                : new CalculationOptionsHealPriest();
        Stats statsRace = BaseStats.getBaseStats(character);
        Stats statsBaseGear = new Stats();
        accumulateItemStats(statsBaseGear, character, additionalItem);
        Stats statsBuffs = getBuffsStats(character, calcOpts);

        Stats statsTalents = new Stats();
        statsTalents.setBonusStaminaMultiplier(character.getPriestTalents().getImprovedPowerWordFortitude() * 0.02f);
        statsTalents.setBonusSpiritMultiplier((1 + character.getPriestTalents().getEnlightenment() * 0.02f) * (1f + character.getPriestTalents().getSpiritOfRedemption() * 0.05f) - 1f);
        statsTalents.setBonusIntellectMultiplier(character.getPriestTalents().getMentalStrength() * 0.03f);
        statsTalents.setSpellDamageFromSpiritPercentage(character.getPriestTalents().getSpiritualGuidance() * 0.05f + character.getPriestTalents().getTwistedFaith() * 0.02f);
        statsTalents.setSpellHaste(character.getPriestTalents().getEnlightenment() * 0.02f);
        statsTalents.setSpellCombatManaRegeneration(character.getPriestTalents().getMeditation() * 0.5f / 3f);
        statsTalents.setSpellCrit(character.getPriestTalents().getFocusedWill() * 0.01f);

        Stats statsTotal = Stats.add(Stats.add(Stats.add(statsBaseGear, statsBuffs), statsRace), statsTalents);

        statsTotal.setStamina((float) Math.floor((statsTotal.getStamina()) * (1 + statsTotal.getBonusStaminaMultiplier())));
        statsTotal.setIntellect((float) Math.floor(statsTotal.getIntellect() * (1 + statsTotal.getBonusIntellectMultiplier())));
        statsTotal.setSpirit((float) Math.floor((statsTotal.getSpirit()) * (1 + statsTotal.getBonusSpiritMultiplier())));
        statsTotal.setSpellPower(statsTotal.getSpellPower() + (statsTotal.getSpellDamageFromSpiritPercentage() * statsTotal.getSpirit()
                + (statsTotal.getPriestInnerFire() > 0 ? getInnerFireSpellPowerBonus(character) : 0f)));
        statsTotal.setMana(statsTotal.getMana() + StatConversion.getManaFromIntellect(statsTotal.getIntellect()));
        statsTotal.setMana(statsTotal.getMana() * (1f + statsTotal.getBonusManaMultiplier()));
        statsTotal.setHealth(statsTotal.getHealth() + StatConversion.getHealthFromStamina(statsTotal.getStamina()));
        statsTotal.setHealth((float) Math.floor(statsTotal.getHealth() * (1f + statsTotal.getBonusHealthMultiplier())));
        statsTotal.setSpellCrit(statsTotal.getSpellCrit() + (StatConversion.getSpellCritFromIntellect(statsTotal.getIntellect())
                + StatConversion.getSpellCritFromRating(statsTotal.getCritRating())));
        statsTotal.setSpellHaste((1f + statsTotal.getSpellHaste()) * (1f + StatConversion.getSpellHasteFromRating(statsTotal.getHasteRating())) - 1f);
        statsTotal.setBonusArmor(statsTotal.getBonusArmor() + (statsTotal.getAgility() * 2f + (statsTotal.getPriestInnerFire() > 0 ? getInnerFireArmorBonus(character) : 0f)));
        return statsTotal;
    }

    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        List<ComparisonCalculationBase> comparisonList = new ArrayList<ComparisonCalculationBase>();
        ComparisonCalculationBase comparison;
        CharacterCalculationsHealPriest p;
        List<Spell> spellList;

        currentChartTotal = 0;
        switch (chartName) {
            case "MP5 Sources": {
                currentChartName = chartName;
                CharacterCalculationsHealPriest mscalcs = (CharacterCalculationsHealPriest) getCharacterCalculations(character);
                BaseSolver mssolver;
                if (((CalculationOptionsHealPriest) character.getCalculationOptions()).getRole() == eRole.CUSTOM)
                    mssolver = new AdvancedSolver(mscalcs.getBasicStats(), character);
                else
                    mssolver = new Solver(mscalcs.getBasicStats(), character);
                mssolver.calculate(mscalcs);
                for (BaseSolver.ManaSource Source : mssolver.ManaSources) {
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
            case "Spell AoE HpS": {
                currentChartName = chartName;
                p = (CharacterCalculationsHealPriest) getCharacterCalculations(character);
                spellList = new ArrayList<Spell>();
                spellList.add(new Renew(p.getBasicStats(), character));
                spellList.add(new FlashHeal(p.getBasicStats(), character));
                spellList.add(new Heal(p.getBasicStats(), character));
                spellList.add(new PrayerOfHealing(p.getBasicStats(), character, 5));
                spellList.add(new BindingHeal(p.getBasicStats(), character));
                spellList.add(new PrayerOfMending(p.getBasicStats(), character, 5));
                spellList.add(new CircleOfHealing(p.getBasicStats(), character, 5));
                spellList.add(new HolyNova(p.getBasicStats(), character, 5));
                spellList.add(new Penance(p.getBasicStats(), character));
                spellList.add(new PowerWordShield(p.getBasicStats(), character));

                for (Spell spell : spellList) {
                    if (spell.getAvgHeal() == 0)
                        continue;

                    comparison = createNewComparisonCalculation();
                    comparison.setName(spell.getName());
                    comparison.setEquipped(false);
                    comparison.getSubPoints()[0] = spell.getHpS();
                    comparison.setOverallPoints(comparison.getSubPoints()[0]);
                    comparisonList.add(comparison);
                }

                return comparisonList.toArray(new ComparisonCalculationBase[0]);
            }
            case "Spell AoE HpM": {
                currentChartName = chartName;
                p = (CharacterCalculationsHealPriest) getCharacterCalculations(character);
                spellList = new ArrayList<Spell>();
                spellList.add(new Renew(p.getBasicStats(), character));
                spellList.add(new FlashHeal(p.getBasicStats(), character));
                spellList.add(new Heal(p.getBasicStats(), character));
                spellList.add(new PrayerOfHealing(p.getBasicStats(), character, 5));
                spellList.add(new BindingHeal(p.getBasicStats(), character));
                spellList.add(new PrayerOfMending(p.getBasicStats(), character, 5));
                spellList.add(new CircleOfHealing(p.getBasicStats(), character, 5));
                spellList.add(new HolyNova(p.getBasicStats(), character, 5));
                spellList.add(new Penance(p.getBasicStats(), character));
                spellList.add(new PowerWordShield(p.getBasicStats(), character));

                for (Spell spell : spellList) {
                    if (spell.getAvgHeal() == 0)
                        continue;

                    comparison = createNewComparisonCalculation();
                    comparison.setName(spell.getName());
                    comparison.setEquipped(false);
                    comparison.getSubPoints()[0] = spell.getHpM();
                    comparison.setOverallPoints(comparison.getSubPoints()[0]);
                    comparisonList.add(comparison);
                }

                return comparisonList.toArray(new ComparisonCalculationBase[0]);
            }
            case "Spell HpS": {
                currentChartName = chartName;
                p = (CharacterCalculationsHealPriest) getCharacterCalculations(character);
                spellList = new ArrayList<Spell>();
                spellList.add(new Renew(p.getBasicStats(), character));
                spellList.add(new FlashHeal(p.getBasicStats(), character));
                spellList.add(new Heal(p.getBasicStats(), character));
                spellList.add(new PrayerOfHealing(p.getBasicStats(), character, 1));
                spellList.add(new BindingHeal(p.getBasicStats(), character));
                spellList.add(new PrayerOfMending(p.getBasicStats(), character, 1));
                spellList.add(new CircleOfHealing(p.getBasicStats(), character, 1));
                spellList.add(new HolyNova(p.getBasicStats(), character, 1));
                spellList.add(new Penance(p.getBasicStats(), character));
                spellList.add(new PowerWordShield(p.getBasicStats(), character));

                for (Spell spell : spellList) {
                    if (spell.getAvgHeal() == 0)
                        continue;

                    comparison = createNewComparisonCalculation();
                    comparison.setName(spell.getName());
                    comparison.setEquipped(false);
                    comparison.getSubPoints()[0] = spell.getHpS();
                    comparison.setOverallPoints(comparison.getSubPoints()[0]);
                    comparisonList.add(comparison);
                }

                return comparisonList.toArray(new ComparisonCalculationBase[0]);
            }
            case "Spell HpM": {
                currentChartName = chartName;
                p = (CharacterCalculationsHealPriest) getCharacterCalculations(character);
                spellList = new ArrayList<Spell>();
                spellList.add(new Renew(p.getBasicStats(), character));
                spellList.add(new FlashHeal(p.getBasicStats(), character));
                spellList.add(new Heal(p.getBasicStats(), character));
                spellList.add(new PrayerOfHealing(p.getBasicStats(), character, 1));
                spellList.add(new BindingHeal(p.getBasicStats(), character));
                spellList.add(new PrayerOfMending(p.getBasicStats(), character, 1));
                spellList.add(new CircleOfHealing(p.getBasicStats(), character, 1));
                spellList.add(new HolyNova(p.getBasicStats(), character, 1));
                spellList.add(new Penance(p.getBasicStats(), character));
                spellList.add(new PowerWordShield(p.getBasicStats(), character));

                for (Spell spell : spellList) {
                    if (spell.getAvgHeal() == 0)
                        continue;

                    comparison = createNewComparisonCalculation();
                    comparison.setName(spell.getName());
                    comparison.setEquipped(false);
                    comparison.getSubPoints()[0] = spell.getHpM();
                    comparison.setOverallPoints(comparison.getSubPoints()[0]);
                    comparisonList.add(comparison);
                }

                return comparisonList.toArray(new ComparisonCalculationBase[0]);
            }

            case "Relative Stat Values": {
                currentChartName = chartName;
                CharacterCalculationsHealPriest calcsBase = (CharacterCalculationsHealPriest) getCharacterCalculations(character);
                CharacterCalculationsHealPriest calcsIntellect = (CharacterCalculationsHealPriest) getCharacterCalculations(character, new Item() {{ setStats(new Stats() {{ setIntellect(50); }}); }});
                CharacterCalculationsHealPriest calcsSpirit = (CharacterCalculationsHealPriest) getCharacterCalculations(character, new Item() {{ setStats(new Stats() {{ setSpirit(50); }}); }});
                CharacterCalculationsHealPriest calcsMP5 = (CharacterCalculationsHealPriest) getCharacterCalculations(character, new Item() {{ setStats(new Stats() {{ setMp5(50); }}); }});
                CharacterCalculationsHealPriest calcsSpellPower = (CharacterCalculationsHealPriest) getCharacterCalculations(character, new Item() {{ setStats(new Stats() {{ setSpellPower(50); }}); }});
                CharacterCalculationsHealPriest calcsHaste = (CharacterCalculationsHealPriest) getCharacterCalculations(character, new Item() {{ setStats(new Stats() {{ setHasteRating(50); }}); }});
                CharacterCalculationsHealPriest calcsCrit = (CharacterCalculationsHealPriest) getCharacterCalculations(character, new Item() {{ setStats(new Stats() {{ setCritRating(50); }}); }});
                CharacterCalculationsHealPriest calcsSta = (CharacterCalculationsHealPriest) getCharacterCalculations(character, new Item() {{ setStats(new Stats() {{ setStamina(50); }}); }});
                CharacterCalculationsHealPriest calcsRes = (CharacterCalculationsHealPriest) getCharacterCalculations(character, new Item() {{ setStats(new Stats() {{ setResilience(50); }}); }});

                ComparisonCalculationHealPriest c1 = new ComparisonCalculationHealPriest();
                c1.setName("1 Intellect");
                c1.setOverallPoints((calcsIntellect.getOverallPoints() - calcsBase.getOverallPoints()) / 50);
                c1.setHealPoints((calcsIntellect.getSubPoints()[0] - calcsBase.getSubPoints()[0]) / 50);
                c1.setRegenPoints((calcsIntellect.getSubPoints()[1] - calcsBase.getSubPoints()[1]) / 50);
                c1.setHastePoints((calcsIntellect.getSubPoints()[2] - calcsBase.getSubPoints()[2]) / 50);
                ComparisonCalculationHealPriest c2 = new ComparisonCalculationHealPriest();
                c2.setName("1 Spirit");
                c2.setOverallPoints((calcsSpirit.getOverallPoints() - calcsBase.getOverallPoints()) / 50);
                c2.setHealPoints((calcsSpirit.getSubPoints()[0] - calcsBase.getSubPoints()[0]) / 50);
                c2.setRegenPoints((calcsSpirit.getSubPoints()[1] - calcsBase.getSubPoints()[1]) / 50);
                c2.setHastePoints((calcsSpirit.getSubPoints()[2] - calcsBase.getSubPoints()[2]) / 50);
                ComparisonCalculationHealPriest c3 = new ComparisonCalculationHealPriest();
                c3.setName("1 MP5");
                c3.setOverallPoints((calcsMP5.getOverallPoints() - calcsBase.getOverallPoints()) / 50);
                c3.setHealPoints((calcsMP5.getSubPoints()[0] - calcsBase.getSubPoints()[0]) / 50);
                c3.setRegenPoints((calcsMP5.getSubPoints()[1] - calcsBase.getSubPoints()[1]) / 50);
                c3.setHastePoints((calcsMP5.getSubPoints()[2] - calcsBase.getSubPoints()[2]) / 50);
                ComparisonCalculationHealPriest c4 = new ComparisonCalculationHealPriest();
                c4.setName("1 Spell Power");
                c4.setOverallPoints((calcsSpellPower.getOverallPoints() - calcsBase.getOverallPoints()) / 50);
                c4.setHealPoints((calcsSpellPower.getSubPoints()[0] - calcsBase.getSubPoints()[0]) / 50);
                c4.setRegenPoints((calcsSpellPower.getSubPoints()[1] - calcsBase.getSubPoints()[1]) / 50);
                c4.setHastePoints((calcsSpellPower.getSubPoints()[2] - calcsBase.getSubPoints()[2]) / 50);
                ComparisonCalculationHealPriest c5 = new ComparisonCalculationHealPriest();
                c5.setName("1 Haste");
                c5.setOverallPoints((calcsHaste.getOverallPoints() - calcsBase.getOverallPoints()) / 50);
                c5.setHealPoints((calcsHaste.getSubPoints()[0] - calcsBase.getSubPoints()[0]) / 50);
                c5.setRegenPoints((calcsHaste.getSubPoints()[1] - calcsBase.getSubPoints()[1]) / 50);
                c5.setHastePoints((calcsHaste.getSubPoints()[2] - calcsBase.getSubPoints()[2]) / 50);
                ComparisonCalculationHealPriest c6 = new ComparisonCalculationHealPriest();
                c6.setName("1 Crit");
                c6.setOverallPoints((calcsCrit.getOverallPoints() - calcsBase.getOverallPoints()) / 50);
                c6.setHealPoints((calcsCrit.getSubPoints()[0] - calcsBase.getSubPoints()[0]) / 50);
                c6.setRegenPoints((calcsCrit.getSubPoints()[1] - calcsBase.getSubPoints()[1]) / 50);
                c6.setHastePoints((calcsCrit.getSubPoints()[2] - calcsBase.getSubPoints()[2]) / 50);
                ComparisonCalculationHealPriest c7 = new ComparisonCalculationHealPriest();
                c7.setName("1 Stamina");
                c7.setOverallPoints((calcsSta.getOverallPoints() - calcsBase.getOverallPoints()) / 50);
                c7.setHealPoints((calcsSta.getSubPoints()[0] - calcsBase.getSubPoints()[0]) / 50);
                c7.setRegenPoints((calcsSta.getSubPoints()[1] - calcsBase.getSubPoints()[1]) / 50);
                c7.setHastePoints((calcsSta.getSubPoints()[2] - calcsBase.getSubPoints()[2]) / 50);
                ComparisonCalculationHealPriest c8 = new ComparisonCalculationHealPriest();
                c8.setName("1 Resilience");
                c8.setOverallPoints((calcsRes.getOverallPoints() - calcsBase.getOverallPoints()) / 50);
                c8.setHealPoints((calcsRes.getSubPoints()[0] - calcsBase.getSubPoints()[0]) / 50);
                c8.setRegenPoints((calcsRes.getSubPoints()[1] - calcsBase.getSubPoints()[1]) / 50);
                c8.setHastePoints((calcsRes.getSubPoints()[2] - calcsBase.getSubPoints()[2]) / 50);
                return new ComparisonCalculationBase[] { c1, c2, c3, c4, c5, c6, c7, c8 };
            }
            default:
                currentChartName = null;
                return new ComparisonCalculationBase[0];
        }
    }

    // C# CalculationsHealPriest.cs:744-830 GetRelevantStats.
    public Stats getRelevantStats(Stats stats) {
        Stats s = new Stats();
        s.setStamina(stats.getStamina());
        s.setIntellect(stats.getIntellect());
        s.setSpirit(stats.getSpirit());
        s.setHealth(stats.getHealth());
        s.setMana(stats.getMana());
        s.setMp5(stats.getMp5());
        s.setSpellPower(stats.getSpellPower());

        s.setSpellHaste(stats.getSpellHaste());
        s.setSpellCrit(stats.getSpellCrit());

        s.setResilience(stats.getResilience());
        s.setCritRating(stats.getCritRating());
        s.setHasteRating(stats.getHasteRating());

        s.setBonusIntellectMultiplier(stats.getBonusIntellectMultiplier());
        s.setBonusSpiritMultiplier(stats.getBonusSpiritMultiplier());
        s.setBonusManaMultiplier(stats.getBonusManaMultiplier());
        s.setBonusCritHealMultiplier(stats.getBonusCritHealMultiplier());

        s.setSpellDamageFromSpiritPercentage(stats.getSpellDamageFromSpiritPercentage());
        s.setHealingReceivedMultiplier(stats.getHealingReceivedMultiplier());
        s.setBonusHealingDoneMultiplier(stats.getBonusHealingDoneMultiplier());
        s.setBonusHealthMultiplier(stats.getBonusHealthMultiplier());
        s.setBonusManaPotion(stats.getBonusManaPotion());
        s.setSpellCombatManaRegeneration(stats.getSpellCombatManaRegeneration());
        s.setManaRestoreFromMaxManaPerSecond(stats.getManaRestoreFromMaxManaPerSecond());
        s.setPriestInnerFire(stats.getPriestInnerFire());
        s.setHealed(stats.getHealed());

        s.setManaRestore(stats.getManaRestore());
        s.setSpellsManaReduction(stats.getSpellsManaReduction());
        s.setHighestStat(stats.getHighestStat());
        s.setShieldFromHealed(stats.getShieldFromHealed());
        s.setManaGainOnGreaterHealOverheal(stats.getManaGainOnGreaterHealOverheal());
        s.setRenewDurationIncrease(stats.getRenewDurationIncrease());
        s.setBonusPoHManaCostReductionMultiplier(stats.getBonusPoHManaCostReductionMultiplier());
        s.setBonusGHHealingMultiplier(stats.getBonusGHHealingMultiplier());
        s.setPrayerOfMendingExtraJumps(stats.getPrayerOfMendingExtraJumps());
        s.setGreaterHealCostReduction(stats.getGreaterHealCostReduction());
        s.setWeakenedSoulDurationDecrease(stats.getWeakenedSoulDurationDecrease());
        s.setPrayerOfHealingExtraCrit(stats.getPrayerOfHealingExtraCrit());
        s.setPWSBonusSpellPowerProc(stats.getPWSBonusSpellPowerProc());
        s.setPriestHeal_T9_2pc(stats.getPriestHeal_T9_2pc());
        s.setPriestHeal_T9_4pc(stats.getPriestHeal_T9_4pc());
        s.setPriestHeal_T10_2pc(stats.getPriestHeal_T10_2pc());
        s.setPriestHeal_T10_4pc(stats.getPriestHeal_T10_4pc());

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
        if (se.getTrigger() == Trigger.HealingSpellCast
                || se.getTrigger() == Trigger.HealingSpellCrit
                || se.getTrigger() == Trigger.HealingSpellHit
                || se.getTrigger() == Trigger.SpellCast
                || se.getTrigger() == Trigger.SpellCrit
                || se.getTrigger() == Trigger.Use) {
            return hasRelevantStatsInternal(se.getStats()) || (se.getStats().getRawSpecialEffectDataSize() > 0 && hasRelevantStatsInternal(se.getStats().getRawSpecialEffectData()[0].getStats()));
        }
        return false;
    }

    protected boolean hasRelevantStatsInternal(Stats stats) {
        boolean Yes = (
                stats.getIntellect() + stats.getSpirit() + stats.getMana() + stats.getMp5() + stats.getSpellPower()
                + stats.getSpellHaste() + stats.getSpellCrit()
                + stats.getHasteRating() + stats.getCritRating()
                + stats.getBonusIntellectMultiplier() + stats.getBonusSpiritMultiplier() + stats.getBonusManaMultiplier() + stats.getBonusCritHealMultiplier()
                + stats.getSpellDamageFromSpiritPercentage() + stats.getHealingReceivedMultiplier() + stats.getBonusHealingDoneMultiplier() + stats.getBonusManaPotion()
                + stats.getSpellCombatManaRegeneration() + stats.getManaRestoreFromMaxManaPerSecond() + stats.getPriestInnerFire()
                + stats.getHealed()

                + stats.getManaGainOnGreaterHealOverheal() + stats.getRenewDurationIncrease()
                + stats.getPrayerOfHealingExtraCrit() + stats.getPWSBonusSpellPowerProc()

                + stats.getBonusPoHManaCostReductionMultiplier() + stats.getBonusGHHealingMultiplier()
                + stats.getPrayerOfMendingExtraJumps() + stats.getGreaterHealCostReduction()
                + stats.getWeakenedSoulDurationDecrease()
                + stats.getPriestHeal_T9_2pc() + stats.getPriestHeal_T9_4pc()
                + stats.getPriestHeal_T10_2pc() + stats.getPriestHeal_T10_4pc()

                + stats.getManaRestore() + stats.getSpellsManaReduction() + stats.getHighestStat()
                + stats.getShieldFromHealed()
        ) > 0;

        boolean Maybe = (
                stats.getStamina() + stats.getHealth() + stats.getResilience()
                + stats.getArmor() + stats.getBonusArmor() + stats.getAgility()
                + stats.getSpellHit() + stats.getHitRating()
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
                + stats.getPhysicalHit()
        ) > 0;

        return Yes || (Maybe && !No);
    }

    @Override
    public boolean hasRelevantStats(Stats stats) {
        boolean isRelevant = hasRelevantStatsInternal(stats);

        for (SpecialEffect se : stats.specialEffects())
            isRelevant |= relevantTrinket(se);
        return isRelevant;
    }

    public Stats getBuffsStats(Character character, CalculationOptionsHealPriest calcOpts) {
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

    private static CalculationsHealPriest instance;

    public static CalculationsHealPriest getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("HealPriest", CalculationsHealPriest.class);
    }

    public static void register() {
        Calculations.registerModel("HealPriest", CalculationsHealPriest.class);
    }

    public CalculationsHealPriest() {
        instance = this;
    }

    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            String adjusted = xml.replace("CalculationOptionsPriest", "CalculationOptionsHealPriest").replace("CalculationOptionsHolyPriest", "CalculationOptionsHealPriest");
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(adjusted)));
            CalculationOptionsHealPriest calcOpts = new CalculationOptionsHealPriest();
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
}
