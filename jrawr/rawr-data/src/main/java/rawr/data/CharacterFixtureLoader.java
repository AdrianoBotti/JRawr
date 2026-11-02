// New loader code (not a line-by-line port): reads the original character save (fixture) members the
// Mage slice consumes. The matching writer is Character.save(Path) in rawr-core; element
// names/order/defaults follow XmlSerializer output.
package rawr.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import rawr.base.character.Character;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterRace;
import rawr.base.character.CharacterRegion;
import rawr.base.character.CharacterSlot;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.talents.DruidTalents;
import rawr.base.items.GemmingTemplate;
import rawr.base.talents.HunterTalents;
import rawr.base.items.ItemFilterEnabledOverride;
import rawr.base.items.ItemInstance;
import rawr.base.talents.MageTalents;
import rawr.base.talents.PaladinTalents;
import rawr.base.talents.PriestTalents;
import rawr.base.character.Profession;
import rawr.base.calculations.OptimizationRequirement;
import rawr.base.talents.RogueTalents;
import rawr.base.talents.ShamanTalents;
import rawr.base.talents.WarriorTalents;
import rawr.base.talents.WarlockTalents;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CharacterFixtureLoader {
    private CharacterFixtureLoader() {
    }

    public static Character load(Path path) throws Exception {
        Document doc = XmlLoader.parse(path);
        Element root = doc.getDocumentElement();
        Character character = new Character();
        // The C# XmlSerializer sets the fields directly (SetGemmedId does not invalidate while
        // loading); IsLoading suppresses the invalidation side effects until the load is complete.
        character.setIsLoading(true);
        String name = XmlLoader.text(root, "Name");
        if (name != null) character.setName(name);
        String realm = XmlLoader.text(root, "Realm");
        if (realm != null) character.setRealm(realm);
        String region = XmlLoader.text(root, "Region");
        if (region != null) character.setRegion(CharacterRegion.valueOf(region));
        String race = XmlLoader.text(root, "Race");
        if (race != null) character.setRace(CharacterRace.valueOf(race));
        String characterClass = XmlLoader.text(root, "Class");
        if (characterClass != null) character.setCharacterClass(CharacterClass.valueOf(characterClass));
        String currentModel = XmlLoader.text(root, "CurrentModel");
        if (currentModel != null) character.setCurrentModel(currentModel);
        String enforce = XmlLoader.text(root, "EnforceMetagemRequirements");
        if (enforce != null) character.setEnforceGemRequirements(Boolean.parseBoolean(enforce));
        String primaryProfession = XmlLoader.text(root, "PrimaryProfession");
        if (primaryProfession != null) character.setPrimaryProfession(Profession.valueOf(primaryProfession));
        String secondaryProfession = XmlLoader.text(root, "SecondaryProfession");
        if (secondaryProfession != null) character.setSecondaryProfession(Profession.valueOf(secondaryProfession));
        String mageTalents = XmlLoader.text(root, "MageTalents");
        if (mageTalents != null) character.setMageTalents(new MageTalents(mageTalents));
        String warriorTalents = XmlLoader.text(root, "WarriorTalents");
        if (warriorTalents != null) character.setWarriorTalents(new WarriorTalents(warriorTalents));
        String shamanTalents = XmlLoader.text(root, "ShamanTalents");
        if (shamanTalents != null) character.setShamanTalents(new ShamanTalents(shamanTalents));
        String druidTalents = XmlLoader.text(root, "DruidTalents");
        if (druidTalents != null) character.setDruidTalents(new DruidTalents(druidTalents));
        String paladinTalents = XmlLoader.text(root, "PaladinTalents");
        if (paladinTalents != null) character.setPaladinTalents(new PaladinTalents(paladinTalents));
        String priestTalents = XmlLoader.text(root, "PriestTalents");
        if (priestTalents != null) character.setPriestTalents(new PriestTalents(priestTalents));
        String hunterTalents = XmlLoader.text(root, "HunterTalents");
        if (hunterTalents != null) character.setHunterTalents(new HunterTalents(hunterTalents));
        String rogueTalents = XmlLoader.text(root, "RogueTalents");
        if (rogueTalents != null) character.setRogueTalents(new RogueTalents(rogueTalents));
        String deathKnightTalents = XmlLoader.text(root, "DeathKnightTalents");
        if (deathKnightTalents != null) character.setDeathKnightTalents(new DeathKnightTalents(deathKnightTalents));
        String warlockTalents = XmlLoader.text(root, "WarlockTalents");
        if (warlockTalents != null) character.setWarlockTalents(new WarlockTalents(warlockTalents));

        Element calculationOptions = XmlLoader.firstChild(root, "CalculationOptions");
        if (calculationOptions != null) {
            character.setSerializedCalculationOptions(rawr.base.util.XmlMap.readStringMap(calculationOptions));
        }

        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) n;
            if (e.getTagName().equals("ActiveBuffs")) {
                String buffName = e.getTextContent().trim();
                if (!buffName.isEmpty()) character.activeBuffsAdd(buffName);
                continue;
            }
            if (e.getTagName().equals("AvailableItems")) {
                String available = e.getTextContent().trim();
                if (!available.isEmpty()) {
                    character.getAvailableItems().add(available);
                }
                continue;
            }
            if (e.getTagName().equals("CalculationToOptimize")) {
                String calculation = e.getTextContent().trim();
                if (!calculation.isEmpty()) {
                    character.setCalculationToOptimize(calculation);
                }
                continue;
            }
            if (e.getTagName().equals("OptimizationRequirements")) {
                character.setOptimizationRequirements(readOptimizationRequirements(e));
                continue;
            }
            if (e.getTagName().equals("Boss")) {
                character.setBossOptions(BossOptionsLoader.load(e));
                continue;
            }
            CharacterSlot slot;
            try {
                slot = CharacterSlot.valueOf(e.getTagName());
            } catch (IllegalArgumentException ex) {
                continue;
            }
            String gemmedId = e.getTextContent().trim();
            if (!gemmedId.isEmpty()) {
                character.set(slot, new ItemInstance(gemmedId));
            }
        }

        Element customGemmingTemplates = XmlLoader.firstChild(root, "CustomGemmingTemplates");
        if (customGemmingTemplates != null) {
            character.setCustomGemmingTemplates(readGemmingTemplates(customGemmingTemplates));
        }
        Element gemmingTemplateOverrides = XmlLoader.firstChild(root, "GemmingTemplateOverrides");
        if (gemmingTemplateOverrides != null) {
            character.setGemmingTemplateOverrides(readGemmingTemplates(gemmingTemplateOverrides));
        }
        Element customItemInstances = XmlLoader.firstChild(root, "CustomItemInstances");
        if (customItemInstances != null) {
            character.setCustomItemInstances(readCustomItemInstances(customItemInstances));
        }
        Element itemFilterEnabledOverride = XmlLoader.firstChild(root, "ItemFilterEnabledOverride");
        if (itemFilterEnabledOverride != null) {
            List<ItemFilterEnabledOverride> overrides = new ArrayList<ItemFilterEnabledOverride>();
            for (Node n = itemFilterEnabledOverride.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n.getNodeType() != Node.ELEMENT_NODE) continue;
                Element e = (Element) n;
                if (e.getTagName().equals("ItemFilterEnabledOverride")) {
                    overrides.add(readItemFilterEnabledOverride(e));
                }
            }
            character.setItemFilterEnabledOverride(overrides);
        }

        character.setIsLoading(false);
        character.recalculateSetBonuses();
        // C# Character.LoadFromXml marks every loaded custom instance as force-display.
        for (ItemInstance item : character.getCustomItemInstances()) {
            item.setForceDisplay(true);
        }
        // JRawr keeps item filters global (jrawr.item-filters.xml). The C# app applied the
        // character's ItemFilterEnabledOverride snapshot here (FormMain.LoadCharacterIntoForm),
        // but that made opening a character rewrite the global filter state. The block is still
        // parsed for file compatibility and round-tripped, but intentionally not applied.
        // DECISIONS #149 / DEVIATIONS D12.
        return character;
    }

    /** XmlSerializer shape of List<OptimizationRequirement>: one element per requirement. */
    private static List<OptimizationRequirement> readOptimizationRequirements(Element wrapper) {
        List<OptimizationRequirement> requirements = new ArrayList<OptimizationRequirement>();
        for (Node n = wrapper.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) n;
            if (!e.getTagName().equals("OptimizationRequirement")) continue;
            OptimizationRequirement requirement = new OptimizationRequirement();
            String calculation = XmlLoader.text(e, "Calculation");
            if (calculation != null) requirement.calculation = calculation;
            String lessThan = XmlLoader.text(e, "LessThan");
            requirement.lessThan = lessThan != null && Boolean.parseBoolean(lessThan);
            String value = XmlLoader.text(e, "Value");
            if (value != null && !value.isEmpty()) requirement.value = Float.parseFloat(value);
            requirements.add(requirement);
        }
        return requirements;
    }

    // XmlSerializer shape: <GemmingTemplate> items with the fields in GemmingTemplate declaration
    // order (Id, Model, Enabled, Group, RedId, YellowId, BlueId, MetaId, PrismaticId).
    private static List<GemmingTemplate> readGemmingTemplates(Element wrapper) {
        List<GemmingTemplate> templates = new ArrayList<GemmingTemplate>();
        for (Node n = wrapper.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) n;
            if (!e.getTagName().equals("GemmingTemplate")) continue;
            GemmingTemplate template = new GemmingTemplate();
            template.setId(XmlLoader.text(e, "Id"));
            template.setModel(XmlLoader.text(e, "Model"));
            String enabled = XmlLoader.text(e, "Enabled");
            if (enabled != null) template.setEnabled(Boolean.parseBoolean(enabled));
            template.setGroup(XmlLoader.text(e, "Group"));
            template.setRedId(intText(e, "RedId"));
            template.setYellowId(intText(e, "YellowId"));
            template.setBlueId(intText(e, "BlueId"));
            template.setMetaId(intText(e, "MetaId"));
            template.setPrismaticId(intText(e, "PrismaticId"));
            templates.add(template);
        }
        return templates;
    }

    private static int intText(Element parent, String name) {
        String text = XmlLoader.text(parent, name);
        return text == null || text.isEmpty() ? 0 : Integer.parseInt(text);
    }

    // XmlSerializer shape of List<ItemInstance>: <CustomItemInstances><ItemInstance> with the
    // public fields (Id/Gem1Id/Gem2Id/Gem3Id/EnchantId) and the ForceDisplay property. The C#
    // [DefaultValue] attributes only affect the writer (zero/false elements are omitted).
    private static List<ItemInstance> readCustomItemInstances(Element wrapper) {
        List<ItemInstance> instances = new ArrayList<ItemInstance>();
        for (Node n = wrapper.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) n;
            if (!e.getTagName().equals("ItemInstance")) continue;
            ItemInstance instance = new ItemInstance();
            instance.setId(intText(e, "Id"));
            instance.setGem1Id(intText(e, "Gem1Id"));
            instance.setGem2Id(intText(e, "Gem2Id"));
            instance.setGem3Id(intText(e, "Gem3Id"));
            instance.setEnchantId(intText(e, "EnchantId"));
            String forceDisplay = XmlLoader.text(e, "ForceDisplay");
            instance.setForceDisplay(forceDisplay != null && Boolean.parseBoolean(forceDisplay));
            instances.add(instance);
        }
        return instances;
    }

    private static ItemFilterEnabledOverride readItemFilterEnabledOverride(Element element) {
        ItemFilterEnabledOverride override = new ItemFilterEnabledOverride();
        override.setName(XmlLoader.text(element, "Name"));
        String enabled = XmlLoader.text(element, "Enabled");
        if (enabled != null) override.setEnabled(Boolean.parseBoolean(enabled));
        Element sub = XmlLoader.firstChild(element, "SubFilterOverride");
        if (sub != null) {
            List<ItemFilterEnabledOverride> subOverrides = new ArrayList<ItemFilterEnabledOverride>();
            for (Node n = sub.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n.getNodeType() != Node.ELEMENT_NODE) continue;
                Element e = (Element) n;
                if (e.getTagName().equals("ItemFilterEnabledOverride")) {
                    subOverrides.add(readItemFilterEnabledOverride(e));
                }
            }
            override.setSubFilterOverride(subOverrides);
        }
        return override;
    }
}
