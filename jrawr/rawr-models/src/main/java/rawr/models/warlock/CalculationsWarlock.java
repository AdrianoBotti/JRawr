// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Warlock/CalculationsWarlock.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// FRAMEWORK ADAPTATION: the UI options panel is skipped; the default gemming templates keep the
// original model/group/enabled flags. deserializeDataObject uses a JDK-XML reader instead of
// XmlSerializer (same approach as CalculationsShadowPriest).
package rawr.models.warlock;

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
import rawr.base.talents.WarlockTalents;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Core class representing the Rawr.Warlock model
 */
public class CalculationsWarlock extends CalculationsBase {

    public static final float AVG_UNHASTED_CAST_TIME = 2f; // total SWAG

    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.Warlock;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationWarlock();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsWarlock();
    }

    public void setDefaults(Character character) {
        character.activeBuffsAdd("Fel Armor");
        character.activeBuffsAdd("Grand Spellstone");
    }

    private List<String> characterDisplayCalculationLabels = null;

    /**
     * Labels of the stats to display on the Stats tab of the main form
     */
    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        if (characterDisplayCalculationLabels == null) {
            characterDisplayCalculationLabels = new ArrayList<String>(List.of(
                    "Simulation:Personal DPS",
                    "Simulation:Pet DPS",
                    "Simulation:Total DPS",
                    "Basic Stats:Health",
                    "Basic Stats:Mana",
                    "Basic Stats:Spirit",
                    "Basic Stats:Bonus Damage",
                    "Basic Stats:Hit Rating",
                    "Basic Stats:Crit Chance",
                    "Basic Stats:Average Haste",
                    "Pet Stats:Pet Stamina",
                    "Pet Stats:Pet Intellect",
                    "Pet Stats:Pet Health",
                    "Affliction:Corruption",
                    "Affliction:Curse Of Agony",
                    "Affliction:Curse Of Doom",
                    "Affliction:Curse Of The Elements",
                    "Affliction:Haunt",
                    "Affliction:Life Tap",
                    "Affliction:Unstable Affliction",
                    "Demonology:Immolation Aura",
                    "Destruction:Chaos Bolt",
                    "Destruction:Conflagrate",
                    "Destruction:Immolate",
                    "Destruction:Incinerate",
                    "Destruction:Incinerate (Under Backdraft)",
                    "Destruction:Incinerate (Under Molten Core)",
                    "Destruction:Soul Fire",
                    "Destruction:Shadow Bolt",
                    "Destruction:Shadow Bolt (Instant)"));
        }
        return characterDisplayCalculationLabels;
    }

    private String[] optimizableCalculationLabels = null;

    /**
     * Labels of the additional constraints available to the Optimizer
     */
    public String[] getOptimizableCalculationLabels() {
        if (optimizableCalculationLabels == null) {
            optimizableCalculationLabels = new String[] { "Miss Chance" };
        }
        return optimizableCalculationLabels;
    }

    private String[] customChartNames = null;

    /**
     * Names of the custom charts that Rawr.Warlock provides
     */
    public String[] getCustomChartNames() {
        if (customChartNames == null) {
            customChartNames = new String[] {};
        }
        return customChartNames;
    }

    private LinkedHashMap<String, String> subPointNameColors = null;

    /**
     * Names and colors for the SubPoints that Rawr.Warlock uses
     */
    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        if (subPointNameColors == null) {
            subPointNameColors = new LinkedHashMap<String, String>();
            subPointNameColors.put("DPS", "#FF0000");
            subPointNameColors.put("Pet DPS", "#0000FF");
            subPointNameColors.put("Raid Buff", "#00FF00");
        }
        return subPointNameColors;
    }

    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            org.w3c.dom.Element root = document.getDocumentElement();
            CalculationOptionsWarlock calcOpts = new CalculationOptionsWarlock();
            for (org.w3c.dom.Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                org.w3c.dom.Element child = (org.w3c.dom.Element) node;
                String name = child.getTagName();
                String text = child.getTextContent().trim();
                switch (name) {
                    case "Pet": calcOpts.setPet(text); break;
                    case "Imbue": calcOpts.setImbue(text); break;
                    case "UseInfernal": calcOpts.setUseInfernal(Boolean.parseBoolean(text)); break;
                    case "TargetLevel": calcOpts.setTargetLevel(text.isEmpty() ? 0 : Integer.parseInt(text)); break;
                    case "Duration": calcOpts.setDuration(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "Latency": calcOpts.setLatency(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "ThirtyFive": calcOpts.setThirtyFive(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "TwentyFive": calcOpts.setTwentyFive(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "Rotations": {
                        List<Rotation> rotations = new ArrayList<Rotation>();
                        for (org.w3c.dom.Node r = child.getFirstChild(); r != null; r = r.getNextSibling()) {
                            if (r.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                            if (!r.getNodeName().equals("Rotation")) continue;
                            Rotation rotation = new Rotation();
                            for (org.w3c.dom.Node f = r.getFirstChild(); f != null; f = f.getNextSibling()) {
                                if (f.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                                org.w3c.dom.Element field = (org.w3c.dom.Element) f;
                                switch (field.getTagName()) {
                                    case "Name": rotation.name = field.getTextContent(); break;
                                    case "Filler": rotation.filler = field.getTextContent(); break;
                                    case "Execute": rotation.execute = field.getTextContent(); break;
                                    case "SpellPriority": {
                                        List<String> priority = new ArrayList<String>();
                                        for (org.w3c.dom.Node n = field.getFirstChild(); n != null; n = n.getNextSibling()) {
                                            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                                            if (n.getNodeName().equals("string")) priority.add(n.getTextContent());
                                        }
                                        rotation.spellPriority = priority;
                                        break;
                                    }
                                    default: break;
                                }
                            }
                            rotations.add(rotation);
                        }
                        calcOpts.setRotations(rotations);
                        break;
                    }
                    case "ActiveRotationIndex": calcOpts.setActiveRotationIndex(text.isEmpty() ? 0 : Integer.parseInt(text)); break;
                    case "PerSP": calcOpts.setPerSP(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "ConvertTotem": calcOpts.setConvertTotem(Boolean.parseBoolean(text)); break;
                    case "PerFlametongue": calcOpts.setPerFlametongue(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "PerMagicBuff": calcOpts.setPerMagicBuff(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "PerCritBuff": calcOpts.setPerCritBuff(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "PerInt": calcOpts.setPerInt(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "PerSpi": calcOpts.setPerSpi(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "PerHealth": calcOpts.setPerHealth(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "NoProcs": calcOpts.setNoProcs(Boolean.parseBoolean(text)); break;
                    default: break;
                }
            }
            return calcOpts;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CharacterCalculationsBase getCharacterCalculations(
            Character character,
            Item additionalItem,
            boolean referenceCalculation,
            boolean significantChange,
            boolean needsDisplayCalculations) {

        // DECISIONS #28: invalidate the shared SpecialEffect interpolation caches for a
        // user-initiated top-level calculation so the adaptive interpolator grids start fresh,
        // like they did in the oracle's per-fixture dump process.
        if (referenceCalculation && significantChange) {
            SpecialEffect.clearInterpolatorCaches();
        }

        return new CharacterCalculationsWarlock(
                character,
                getCharacterStats(character, additionalItem),
                getPetBuffStats(character));
    }

    private Stats getPetBuffStats(Character character) {
        List<Buff> buffs = new ArrayList<Buff>();
        for (Buff buff : character.getActiveBuffs()) {
            String group = buff.group;
            if (!"Profession Buffs".equals(group)
                    && !"Set Bonuses".equals(group)
                    && !"Food".equals(group)
                    && !"Potion".equals(group)
                    && !"Elixirs and Flasks".equals(group)
                    && !"Focus Magic, Spell Critical Strike Chance".equals(group)) {

                buffs.add(buff);
            }
        }
        Stats stats = getBuffsStats(buffs);
        applyPetsRaidBuff(
                stats,
                ((CalculationOptionsWarlock) character.getCalculationOptions()).getPet(),
                character.getWarlockTalents(),
                character.getActiveBuffs());
        return stats;
    }

    /**
     * Gets the total Stats of the Character
     */
    public Stats getCharacterStats(Character character, Item additionalItem) {
        WarlockTalents talents = character.getWarlockTalents();
        CalculationOptionsWarlock options = (CalculationOptionsWarlock) character.getCalculationOptions();
        Stats stats = rawr.base.stats.BaseStats.getBaseStats(character);

        // Items
        accumulateItemStats(stats, character, additionalItem);

        // Buffs
        accumulateBuffsStats(stats, character.getActiveBuffs());
        if (options.getImbue().equals("Grand Spellstone")) {
            stats.setHasteRating(stats.getHasteRating() + 60f * (1f + talents.getMasterConjuror() * 1.5f));
        } else {
            stats.setCritRating(stats.getCritRating() + 49f * (1f + talents.getMasterConjuror() * 1.5f));
        }
        applyPetsRaidBuff(stats, options.getPet(), talents, character.getActiveBuffs());
        float aegis = 1f + talents.getDemonicAegis() * 0.10f;
        stats.setSpellPower(stats.getSpellPower() + 180f * aegis); // fel armor
        stats.setSpellDamageFromSpiritPercentage(stats.getSpellDamageFromSpiritPercentage() + .3f * aegis); // fel armor

        // Talents
        float[] talentValues = { 0f, .04f, .07f, .1f };
        Stats statsTalents = new Stats();

        //Demonic Embrace: increases your stamina by 4/7/10%
        statsTalents.setBonusStaminaMultiplier(talentValues[talents.getDemonicEmbrace()]);

        //Fel Vitality: increases your maximum Health & Mana by 1/2/3%
        statsTalents.setBonusHealthMultiplier(talents.getFelVitality() * 0.01f);
        statsTalents.setBonusManaMultiplier(talents.getFelVitality() * 0.01f);

        //Suppression: increases your chance to hit with spells by 1/2/3%
        statsTalents.setSpellHit(talents.getSuppression() * 0.01f);

        //Demonic Tactics: increases your spell crit chance by 2/4/6/8/10%
        //Backlash: increases your spell crit chance by 1/2/3%
        statsTalents.setBonusCritChance(
                talents.getDemonicTactics() * 0.02f + talents.getBacklash() * 0.01f);

        if (talents.getEradication() > 0) {
            talentValues = new float[] { 0f, .06f, .12f, .20f };
            Stats eradicationStats = new Stats();
            eradicationStats.setSpellHaste(talentValues[talents.getEradication()]);
            statsTalents.addSpecialEffect(
                    new SpecialEffect(
                            Trigger.CorruptionTick,
                            eradicationStats,
                            6f,
                            0f,
                            .06f));
        }
        stats.accumulate(statsTalents);
        stats.setManaRestoreFromMaxManaPerSecond(
                Math.max(
                        stats.getManaRestoreFromMaxManaPerSecond(),
                        .002f * Spell.calcUprate(
                                talents.getImprovedSoulLeech() * .5f,
                                15f,
                                options.getDuration() * 1.1f)));

        return stats;
    }

    private void applyPetsRaidBuff(
            Stats stats,
            String pet,
            WarlockTalents talents,
            List<Buff> activeBuffs) {

        stats.setHealth(stats.getHealth() + calcPetHealthBuff(pet, talents, activeBuffs));
        stats.setIntellect(stats.getIntellect() + calcPetIntBuff(pet, talents, activeBuffs));
        stats.setSpirit(stats.getSpirit() + calcPetSpiBuff(pet, talents, activeBuffs));
    }

    public static float calcPetHealthBuff(
            String pet, WarlockTalents talents, List<Buff> activeBuffs) {
        if (!pet.equals("Imp")) {
            return 0f;
        }

        return StatUtils.getBuffEffect(
                activeBuffs,
                1330f * (1 + talents.getImprovedImp() * .1f),
                "Health",
                s -> s.getHealth());
    }

    public static float calcPetIntBuff(
            String pet, WarlockTalents talents, List<Buff> activeBuffs) {
        if (!pet.equals("Felhunter")) {
            return 0f;
        }

        return StatUtils.getBuffEffect(
                activeBuffs,
                48f * (1 + talents.getImprovedFelhunter() * .05f),
                "Intellect",
                s -> s.getIntellect());
    }

    public static float calcPetSpiBuff(
            String pet, WarlockTalents talents, List<Buff> activeBuffs) {
        if (!pet.equals("Felhunter")) {
            return 0f;
        }

        return StatUtils.getBuffEffect(
                activeBuffs,
                64f * (1 + talents.getImprovedFelhunter() * .05f),
                "Spirit",
                s -> s.getSpirit());
    }

    /**
     * Gets data for a custom chart that Rawr.Warlock provides
     */
    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        return new ComparisonCalculationBase[0];
    }

    private List<ItemType> relevantItemTypes = null;

    /**
     * ItemTypes that are relevant to Rawr.Warlock
     */
    @Override
    public List<ItemType> getRelevantItemTypes() {
        if (relevantItemTypes == null) {
            relevantItemTypes = new ArrayList<ItemType>(List.of(
                    ItemType.None,
                    ItemType.Cloth,
                    ItemType.Dagger,
                    ItemType.Wand,
                    ItemType.OneHandSword,
                    ItemType.Staff));
        }
        return relevantItemTypes;
    }

    /**
     * GemmingTemplates to be used by default, when none are defined
     */
    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        //Relevant Gem IDs for Warlocks
        //Red
        int[] runed = { 39911, 39998, 40113, 42144 };

        //Purple
        int[] purified = { 39941, 40026, 40133 };

        //Orange
        int[] reckless = { 39959, 40051, 40155 };
        int[] veiled = { 39957, 40049, 40153 };

        //Meta
        final int ember = 41333;
        final int chaotic = 41285;

        return new ArrayList<GemmingTemplate>(List.of(
                // region uncommon
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Uncommon"); // Max SP - Ember
                    setRedId(runed[0]); setYellowId(runed[0]); setBlueId(runed[0]); setPrismaticId(runed[0]); setMetaId(ember); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Uncommon"); // SP/Hit - Ember
                    setRedId(runed[0]); setYellowId(veiled[0]); setBlueId(purified[0]); setPrismaticId(runed[0]); setMetaId(ember); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Uncommon"); // SP/Haste - Ember
                    setRedId(runed[0]); setYellowId(reckless[0]); setBlueId(purified[0]); setPrismaticId(runed[0]); setMetaId(ember); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Uncommon"); // Max SP - Chaotic
                    setRedId(runed[0]); setYellowId(runed[0]); setBlueId(runed[0]); setPrismaticId(runed[0]); setMetaId(chaotic); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Uncommon"); // SP/Hit - Chaotic
                    setRedId(runed[0]); setYellowId(veiled[0]); setBlueId(purified[0]); setPrismaticId(runed[0]); setMetaId(chaotic); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Uncommon"); // SP/Haste - Chaotic
                    setRedId(runed[0]); setYellowId(reckless[0]); setBlueId(purified[0]); setPrismaticId(runed[0]); setMetaId(chaotic); }},
                // endregion

                // region rare
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Rare"); // Max SP - Ember
                    setRedId(runed[1]); setYellowId(runed[1]); setBlueId(runed[1]); setPrismaticId(runed[1]); setMetaId(ember); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Rare"); // SP/Hit - Ember
                    setRedId(runed[1]); setYellowId(veiled[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(ember); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Rare"); // SP/Haste - Ember
                    setRedId(runed[1]); setYellowId(reckless[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(ember); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Rare"); // Max SP - Chaotic
                    setRedId(runed[1]); setYellowId(runed[1]); setBlueId(runed[1]); setPrismaticId(runed[1]); setMetaId(chaotic); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Rare"); // SP/Hit - Chaotic
                    setRedId(runed[1]); setYellowId(veiled[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(chaotic); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Rare"); // SP/Haste - Chaotic
                    setRedId(runed[1]); setYellowId(reckless[1]); setBlueId(purified[1]); setPrismaticId(runed[1]); setMetaId(chaotic); }},
                // endregion

                // region epic
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Epic"); setEnabled(true); // Max SP - Ember
                    setRedId(runed[2]); setYellowId(runed[2]); setBlueId(runed[2]); setPrismaticId(runed[2]); setMetaId(ember); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Epic"); setEnabled(true); // SP/Hit - Ember
                    setRedId(runed[2]); setYellowId(veiled[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(ember); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Epic"); setEnabled(true); // SP/Haste - Ember
                    setRedId(runed[2]); setYellowId(reckless[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(ember); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Epic"); setEnabled(true); // Max SP - Chaotic
                    setRedId(runed[2]); setYellowId(runed[2]); setBlueId(runed[2]); setPrismaticId(runed[2]); setMetaId(chaotic); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Epic"); setEnabled(true); // SP/Hit - Chaotic
                    setRedId(runed[2]); setYellowId(veiled[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(chaotic); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Epic"); setEnabled(true); // SP/Haste - Chaotic
                    setRedId(runed[2]); setYellowId(reckless[2]); setBlueId(purified[2]); setPrismaticId(runed[2]); setMetaId(chaotic); }},
                // endregion

                // region jeweler
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Jeweler"); // Max SP - Ember
                    setRedId(runed[3]); setYellowId(runed[3]); setBlueId(runed[3]); setPrismaticId(runed[3]); setMetaId(ember); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Jeweler"); // SP/Hit - Ember
                    setRedId(runed[2]); setYellowId(runed[3]); setBlueId(runed[3]); setPrismaticId(runed[2]); setMetaId(ember); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Jeweler"); // Max SP - Chaotic
                    setRedId(runed[3]); setYellowId(runed[3]); setBlueId(runed[3]); setPrismaticId(runed[3]); setMetaId(chaotic); }},
                new GemmingTemplate() {{ setModel("Warlock"); setGroup("Jeweler"); // SP/Hit - Chaotic
                    setRedId(runed[2]); setYellowId(runed[3]); setBlueId(runed[3]); setPrismaticId(runed[2]); setMetaId(chaotic); }}
                // endregion
        ));
    }

    public Stats getRelevantStats(Stats stats) {
        Stats s = new Stats();

        //primary stats
        s.setSpellPower(stats.getSpellPower());
        s.setIntellect(stats.getIntellect());
        s.setSpirit(stats.getSpirit());
        s.setHitRating(stats.getHitRating());
        s.setSpellHit(stats.getSpellHit());
        s.setHasteRating(stats.getHasteRating());
        s.setSpellHaste(stats.getSpellHaste());
        s.setCritRating(stats.getCritRating());
        s.setSpellCrit(stats.getSpellCrit());
        s.setSpellCritOnTarget(stats.getSpellCritOnTarget());

        s.setShadowDamage(stats.getShadowDamage());
        s.setSpellShadowDamageRating(stats.getSpellShadowDamageRating());
        s.setFireDamage(stats.getFireDamage());
        s.setSpellFireDamageRating(stats.getSpellFireDamageRating());

        s.setBonusIntellectMultiplier(stats.getBonusIntellectMultiplier());
        s.setBonusSpiritMultiplier(stats.getBonusSpiritMultiplier());
        s.setBonusSpellCritMultiplier(stats.getBonusSpellCritMultiplier());
        s.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());
        s.setBonusShadowDamageMultiplier(stats.getBonusShadowDamageMultiplier());
        s.setBonusFireDamageMultiplier(stats.getBonusFireDamageMultiplier());
        s.setSpellDamageFromSpiritPercentage(stats.getSpellDamageFromSpiritPercentage());

        //set bonuses
        s.setWarlock2T7(stats.getWarlock2T7());
        s.setWarlock4T7(stats.getWarlock4T7());
        s.setWarlock2T8(stats.getWarlock2T8());
        s.setWarlock4T8(stats.getWarlock4T8());
        s.setWarlock2T9(stats.getWarlock2T9());
        s.setWarlock4T9(stats.getWarlock4T9());
        s.setWarlock2T10(stats.getWarlock2T10());
        s.setWarlock4T10(stats.getWarlock4T10());

        //These stats can be used by warlocks, but they dont affect our dps calculations at all.
        //Included for display purposes only.
        s.setStamina(stats.getStamina());
        s.setHealth(stats.getHealth());
        s.setMana(stats.getMana());
        s.setMp5(stats.getMp5());

        //The following are custom stat properties belonging to buffs, items (or procs) that can be used/applied to warlocks.
        s.setHighestStat(stats.getHighestStat());                                    //trinket - darkmoon card: greatness
        s.setManaRestoreFromBaseManaPPM(stats.getManaRestoreFromBaseManaPPM());      //paladin buff: judgement of wisdom
        s.setManaRestoreFromMaxManaPerSecond(stats.getManaRestoreFromMaxManaPerSecond()); //replenishment
        s.setBonusManaPotion(stats.getBonusManaPotion());                            //triggered when a mana pot is consumed
        s.setThreatReductionMultiplier(stats.getThreatReductionMultiplier());        //Bracing Eathsiege Diamond (metagem) effect
        s.setManaRestore(stats.getManaRestore());                                    //quite a few items that restore mana on spell cast or crit. Also used to model replenishment.
        // SpellsManaReduction = stats.SpellsManaReduction,                          //spark of hope -> http://www.wowhead.com/?item=45703

        for (SpecialEffect effect : stats.specialEffects()) {
            if (relevantTrinket(effect)) {
                s.addSpecialEffect(effect);
            }
        }
        return s;
    }

    @Override
    public boolean isBuffRelevant(Buff buff, Character character) {
        if (!buff.getAllowedClasses().contains(CharacterClass.Warlock)
                || "Spell Sensitivity".equals(buff.group)) {

            return false;
        }
        if (character != null
                && getHideProfEnchants()
                && !character.hasProfession(buff.getProfessions())) {

            return false;
        }
        Stats stats = buff.getTotalStats();
        return hasRelevantStats(stats)
                || stats.getStrength() > 0
                || stats.getAgility() > 0
                || stats.getAttackPower() > 0
                || stats.getBonusAttackPowerMultiplier() > 0
                || stats.getPhysicalCrit() > 0
                || stats.getPhysicalHaste() > 0
                || stats.getArmorPenetration() > 0
                || stats.getBonusPhysicalDamageMultiplier() > 0;
    }

    protected boolean relevantTrinket(SpecialEffect effect) {
        if (effect.getTrigger() == Trigger.Use
                || effect.getTrigger() == Trigger.DamageSpellCast
                || effect.getTrigger() == Trigger.DamageSpellCrit
                || effect.getTrigger() == Trigger.DamageSpellHit
                || effect.getTrigger() == Trigger.SpellCast
                || effect.getTrigger() == Trigger.SpellCrit
                || effect.getTrigger() == Trigger.SpellHit
                || effect.getTrigger() == Trigger.SpellMiss
                || effect.getTrigger() == Trigger.DoTTick
                || effect.getTrigger() == Trigger.DamageDone
                || effect.getTrigger() == Trigger.DamageOrHealingDone) {
            return hasRelevantStats(effect.getStats());
        }
        return false;
    }

    @Override
    public boolean hasRelevantStats(Stats stats) {
        boolean isRelevant = hasRelevantStatsInternal(stats);
        for (SpecialEffect se : stats.specialEffects()) {
            isRelevant |= relevantTrinket(se);
            if (isRelevant) break;
        }
        return isRelevant;
    }

    private boolean hasRelevantStatsInternal(Stats stats) {
        boolean yes = (
                //our primary stats
                stats.getSpellPower()
                        + stats.getIntellect()
                        + stats.getSpirit()
                        + stats.getHitRating() + stats.getSpellHit()
                        + stats.getHasteRating() + stats.getSpellHaste()
                        + stats.getCritRating() + stats.getSpellCrit() + stats.getSpellCritOnTarget()
                        + stats.getShadowDamage() + stats.getSpellShadowDamageRating()
                        + stats.getFireDamage() + stats.getSpellFireDamageRating()

                        //multipliers
                        + stats.getBonusIntellectMultiplier()
                        + stats.getBonusSpiritMultiplier() + stats.getSpellDamageFromSpiritPercentage()
                        + stats.getBonusSpellCritMultiplier()
                        + stats.getBonusDamageMultiplier() + stats.getBonusShadowDamageMultiplier() + stats.getBonusFireDamageMultiplier()

                        //set bonuses
                        + stats.getWarlock2T7()
                        + stats.getWarlock4T7()
                        + stats.getWarlock2T8()
                        + stats.getWarlock4T8()
                        + stats.getWarlock2T9()
                        + stats.getWarlock4T9()
                        + stats.getWarlock2T10()
                        + stats.getWarlock4T10()
        ) > 0;

        boolean maybe = (
                stats.getStamina() + stats.getHealth()
                        + stats.getMana() + stats.getMp5()

                        //miscellaneous stats belonging to items (or trinket procs) that can be used/applied to warlocks
                        //these stats are listed here so that those items (which supply them) can be listed
                        //(these are terrible trinkets anyway - I should probably just remove them...)
                        + stats.getHighestStat()                //darkmoon card: greatness
                        + stats.getSpellsManaReduction()        //spark of hope -> http://www.wowhead.com/?item=45703

                        + stats.getBonusManaPotion()            //triggered when a mana pot is consumed
                        + stats.getManaRestoreFromBaseManaPPM() //judgement of wisdom
                        + stats.getManaRestoreFromMaxManaPerSecond() //replenishment sources
                        + stats.getManaRestore()                //quite a few items that restore mana on spell cast or crit. Also used to model replenishment.
        ) > 0;

        boolean no = (
                //ignore items with any of these stats
                + stats.getResilience()
                        + stats.getArmor() + stats.getBonusArmor() + stats.getAgility()
                        + stats.getArmorPenetration() + stats.getArmorPenetrationRating()
                        + stats.getStrength() + stats.getAttackPower()
                        + stats.getExpertise() + stats.getExpertiseRating()
                        + stats.getDodge() + stats.getDodgeRating()
                        + stats.getParry() + stats.getParryRating()
                        + stats.getDefense() + stats.getDefenseRating()
                        + stats.getThreatReductionMultiplier()  //bracing earthsiege diamond (metagem) effect
        ) > 0;

        return yes || (maybe && !no);
    }

    @Override
    public boolean enchantFitsInSlot(Enchant enchant, Character character, ItemSlot slot) {
        if (slot == ItemSlot.OffHand || slot == ItemSlot.Ranged) {
            return false;
        }
        return super.enchantFitsInSlot(enchant, character, slot);
    }

    @Override
    public boolean itemFitsInSlot(Item item, Character character, CharacterSlot slot, boolean ignoreUnique) {
        if (slot == CharacterSlot.OffHand && item.getSlot() == ItemSlot.OneHand) {
            return false;
        }
        return super.itemFitsInSlot(item, character, slot, ignoreUnique);
    }

    private static List<String> relevantGlyphs;

    public List<String> getRelevantGlyphs() {
        if (relevantGlyphs == null) {
            relevantGlyphs = new ArrayList<String>(List.of(
                    "Glyph of Metamorphosis",
                    "Glyph of Quick Decay",
                    "Glyph of Corruption",
                    "Glyph of Life Tap",
                    "Glyph of Curse of Agony",
                    "Glyph of Unstable Affliction",
                    "Glyph of Haunt",
                    "Glyph of Chaos Bolt",
                    "Glyph of Immolate",
                    "Glyph of Incinerate",
                    "Glyph of Conflagrate",
                    "Glyph of Imp",
                    "Glyph of Felguard"));
        }
        return relevantGlyphs;
    }

    private static CalculationsWarlock instance;

    public static CalculationsWarlock getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("Warlock", CalculationsWarlock.class);
    }

    public static void register() {
        Calculations.registerModel("Warlock", CalculationsWarlock.class);
    }

    public CalculationsWarlock() {
        instance = this;
    }
}
