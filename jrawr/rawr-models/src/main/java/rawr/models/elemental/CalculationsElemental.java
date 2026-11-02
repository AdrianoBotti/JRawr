// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/CalculationsElemental.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: UI-only members omitted (CalculationOptionsPanel, RenderCustomChart and the RAWR3/WPF
// branch); the non-Silverlight display label/color tables are ported additively as
// getCharacterDisplayCalculationLabels()/getSubPointNameColors(). GetBuffsStats(Character,
// CalculationOptionsElemental) only calls the base GetBuffsStats(ActiveBuffs) (its auto-fix block
// is commented out in C#) and is therefore not overridden.
// FRAMEWORK ADAPTATION: DeserializeDataObject uses a JDK-XML reflective reader instead of
// XmlSerializer (same approach as CalculationsCat/Bear/Healadin/Moonkin).
package rawr.models.elemental;

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

public class CalculationsElemental extends CalculationsBase {
    private List<GemmingTemplate> defaultGemmingTemplates = null;

    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        if (defaultGemmingTemplates == null) {
            // Meta
            int chaotic = 41285;

            // [0] uncommon
            // [1] perfect uncommon
            // [2] rare
            // [3] epic
            // [4] jewelcrafting

            // Reds
            int[] runed = { 39911, 41438, 39998, 40113, 42144 }; // spell power
            // Blue
            int[] lustrous = { 39927, 41440, 40010, 40121, 42146 }; // mp5
            // Yellow
            int[] quick = { 39918, 41446, 40017, 40128, 42150 }; // haste
            int[] rigid = { 39915, 41447, 40014, 40125, 42156 }; // hit
            // Purple
            int[] royal = { 39943, 41459, 40027, 40134 }; // spell power + mp5
            // Green
            int[] dazzling = { 39984, 41463, 40094, 40175 }; // int + mp5
            int[] lambent = { 39986, 41469, 40100, 40177 }; // hit + mp5
            // Orange
            int[] luminous = { 39946, 41494, 40047, 40151 }; // spell power + int
            int[] reckless = { 39959, 41497, 40051, 40155 }; // spell power + haste
            int[] potent = { 39956, 41495, 40048, 40152 }; // spell power + crit
            int[] veiled = { 39957, 41502, 40049, 40153 }; // spell power + hit

            /*
             * red: runed, royal
             * yellow: reckless, quick
             * blue: royal, dazzling(, lustrous)
             */

            defaultGemmingTemplates = new ArrayList<GemmingTemplate>();
            addGemmingTemplateGroup(defaultGemmingTemplates, "Uncommon", false, runed[0], royal[0], reckless[0], quick[0], dazzling[0], rigid[0], veiled[0], lambent[0], chaotic);
            addGemmingTemplateGroup(defaultGemmingTemplates, "Perfect", false, runed[1], royal[1], reckless[1], quick[1], dazzling[1], rigid[1], veiled[1], lambent[1], chaotic);
            addGemmingTemplateGroup(defaultGemmingTemplates, "Rare", false, runed[2], royal[2], reckless[2], quick[2], dazzling[2], rigid[2], veiled[2], lambent[2], chaotic);
            addGemmingTemplateGroup(defaultGemmingTemplates, "Epic", true, runed[3], royal[3], reckless[3], quick[3], dazzling[3], rigid[3], veiled[3], lambent[3], chaotic);
            addJCGemmingTemplateGroup(defaultGemmingTemplates, "Jewelcrafting", false, runed[4], quick[4], lustrous[4], rigid[4], chaotic);
        }
        return defaultGemmingTemplates;
    }

    private static GemmingTemplate elementalTemplate(String group, boolean enabled, int redId, int yellowId, int blueId, int prismaticId, int metaId) {
        GemmingTemplate template = new GemmingTemplate();
        template.setModel("Elemental");
        template.setGroup(group);
        template.setRedId(redId);
        template.setYellowId(yellowId);
        template.setBlueId(blueId);
        template.setPrismaticId(prismaticId);
        template.setMetaId(metaId);
        template.setEnabled(enabled);
        return template;
    }

    private static void addGemmingTemplateGroup(List<GemmingTemplate> list, String name, boolean enabled, int runed, int royal, int reckless, int quick, int dazzling, int rigid, int veiled, int lambent, int meta) {
        // if no mana problem, runed and rigid are always very good
        list.add(elementalTemplate(name, enabled, runed, runed, runed, runed, meta));
        list.add(elementalTemplate(name, enabled, rigid, rigid, rigid, rigid, meta));
        list.add(elementalTemplate(name, enabled, veiled, veiled, veiled, veiled, meta));
        // if mana problem, dazzling is often very good
        list.add(elementalTemplate(name, enabled, dazzling, dazzling, dazzling, dazzling, meta));

        /*
         * red: runed, royal
         * yellow: reckless, quick
         * blue: royal, dazzling, lambent (, lustrous)
         */
        // +Power and +Crit
        list.add(elementalTemplate(name, enabled, runed, reckless, royal, runed, meta));
        // +Hit
        list.add(elementalTemplate(name, enabled, veiled, rigid, royal, rigid, meta));
        list.add(elementalTemplate(name, enabled, veiled, rigid, lambent, rigid, meta));
        // MP5
        list.add(elementalTemplate(name, enabled, veiled, rigid, dazzling, veiled, meta));
        list.add(elementalTemplate(name, enabled, royal, dazzling, dazzling, royal, meta));
        list.add(elementalTemplate(name, enabled, royal, dazzling, dazzling, dazzling, meta));
        list.add(elementalTemplate(name, enabled, runed, dazzling, royal, royal, meta));
        list.add(elementalTemplate(name, enabled, runed, dazzling, royal, dazzling, meta));
    }

    private static void addJCGemmingTemplateGroup(List<GemmingTemplate> list, String name, boolean enabled, int runed, int quick, int lustrous, int rigid, int meta) {
        // Overrides, only "runed" and "seers"
        list.add(elementalTemplate(name, enabled, runed, quick, runed, runed, meta));
        list.add(elementalTemplate(name, enabled, lustrous, lustrous, lustrous, lustrous, meta));

        list.add(elementalTemplate(name, enabled, runed, quick, lustrous, runed, meta));
    }

    @Override
    public boolean itemFitsInSlot(Item item, Character character, CharacterSlot slot, boolean ignoreUnique) {
        if (
                slot == CharacterSlot.OffHand
                        && character.getShamanTalents().getDualWield() < 1
                        &&
                        (
                                item.getType() == ItemType.Dagger ||
                                        item.getType() == ItemType.FistWeapon ||
                                        item.getType() == ItemType.OneHandAxe ||
                                        item.getType() == ItemType.OneHandMace
                        )
        ) {
            return false;
        }
        return super.itemFitsInSlot(item, character, slot, ignoreUnique);
    }

    private String[] characterDisplayCalculationLabels = null;

    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        if (characterDisplayCalculationLabels == null)
            characterDisplayCalculationLabels = new String[] {
                    "Summary:Overall Points*Sum of burst and sustained points",
                    "Summary:Burst Points*DPS until you go out of mana",
                    "Summary:Sustained Points*Total DPS of the fight",

                    "Basic Stats:Health",
                    "Basic Stats:Mana",
                    "Basic Stats:Stamina",
                    "Basic Stats:Intellect",
                    "Basic Stats:Spell Power",
                    "Basic Stats:Hit Rating",
                    "Basic Stats:Crit Rating",
                    "Basic Stats:Haste Rating",
                    "Basic Stats:Mana Regen",

                    "Combat Stats:Average Health",
                    "Combat Stats:Average Mana",
                    "Combat Stats:Average Stamina",
                    "Combat Stats:Average Intellect",
                    "Combat Stats:Average Spell Power",
                    "Combat Stats:Average Hit Rating",
                    "Combat Stats:Average Crit Rating",
                    "Combat Stats:Average Haste Rating",
                    "Combat Stats:Average Mana Regen",

                    "Attacks:Lightning Bolt",
                    "Attacks:Chain Lightning",
                    "Attacks:Lava Burst",
                    "Attacks:Flame Shock",
                    "Attacks:Earth Shock",
                    "Attacks:Frost Shock",
                    "Attacks:Fire Nova",
                    "Attacks:Searing Totem",
                    "Attacks:Magma Totem",

                    "Simulation:Simulation",
                    "Simulation:Rotation",
            };
        return List.of(characterDisplayCalculationLabels);
    }

    public void setDefaults(Character character) {
        character.activeBuffsAdd(("Sanctified Retribution"));
        character.activeBuffsAdd(("Heroism/Bloodlust"));
        character.activeBuffsAdd(("Swift Retribution"));
        character.activeBuffsAdd(("Arcane Intellect"));
        character.activeBuffsAdd(("Hunting Party"));
        character.activeBuffsAdd(("Blessing of Wisdom"));
        character.activeBuffsAdd(("Improved Blessing of Wisdom"));
        character.activeBuffsAdd(("Moonkin Form"));
        character.activeBuffsAdd(("Wrath of Air Totem"));
        character.activeBuffsAdd(("Totem of Wrath (Spell Power)"));
        character.activeBuffsAdd(("Divine Spirit"));
        character.activeBuffsAdd(("Mark of the Wild"));
        character.activeBuffsAdd(("Improved Mark of the Wild"));
        character.activeBuffsAdd(("Blessing of Kings"));
        character.activeBuffsAdd(("Totem of Wrath"));
        character.activeBuffsAdd(("Judgement of Wisdom"));
        character.activeBuffsAdd(("Improved Shadow Bolt"));
        character.activeBuffsAdd(("Curse of the Elements"));
        character.activeBuffsAdd(("Improved Faerie Fire"));
        character.activeBuffsAdd(("Flask of the Frost Wyrm"));
        character.activeBuffsAdd(("Fish Feast"));
    }

    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> colors = new LinkedHashMap<String, String>();
        colors.put("Burst DPS", "#FF0000");
        colors.put("Sustained DPS", "#0000FF");
        return colors;
    }

    private List<ItemType> relevantItemTypes = null;

    @Override
    public List<ItemType> getRelevantItemTypes() {
        if (relevantItemTypes == null) {
            relevantItemTypes = new ArrayList<ItemType>(List.of(
                    ItemType.None,
                    ItemType.Cloth,
                    ItemType.Leather,
                    ItemType.Mail,
                    ItemType.Dagger,
                    ItemType.FistWeapon,
                    ItemType.OneHandAxe,
                    ItemType.OneHandMace,
                    ItemType.Shield,
                    ItemType.Staff,
                    ItemType.Totem,
                    ItemType.TwoHandAxe,
                    ItemType.TwoHandMace
            ));
        }
        return relevantItemTypes;
    }

    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.Shaman;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationElemental();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsElemental();
    }

    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            CalculationOptionsElemental calcOpts = new CalculationOptionsElemental();
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

    public static CalculationsElemental instance;

    public static CalculationsElemental getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("Elemental", CalculationsElemental.class);
    }

    public static void register() {
        Calculations.registerModel("Elemental", CalculationsElemental.class);
    }

    public CalculationsElemental() {
        instance = this;
    }

    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        if (referenceCalculation && significantChange) {
            // Fresh top-level character calculation: drop shared effect interpolation caches so a
            // previous character cannot leak into this one (SUSPECTED_BUGS #15), like Moonkin and
            // Healadin (DECISIONS #28/#39).
            SpecialEffect.clearInterpolatorCaches();
        }
        CalculationOptionsElemental calcOpts = character.getCalculationOptions() instanceof CalculationOptionsElemental
                ? (CalculationOptionsElemental) character.getCalculationOptions()
                : new CalculationOptionsElemental();
        rawr.base.bosses.BossOptions bossOpts = character.getBossOptions();
        if (bossOpts == null) bossOpts = new rawr.base.bosses.BossOptions();
        Stats stats = getCharacterStats(character, additionalItem);

        CharacterCalculationsElemental calculatedStats = new CharacterCalculationsElemental();
        calculatedStats.basicStats = stats;
        calculatedStats.localCharacter = character;
        calcOpts.calculatedStats = calculatedStats;

        Estimation.solve(calculatedStats, calcOpts, bossOpts);

        return calculatedStats;
    }

    //slowly being moved back into GetCharacterStats to reduce Stats
    public Stats getTalentStats(rawr.base.talents.ShamanTalents talents) {
        Stats statsTalents = new Stats();
        statsTalents.setSpellHit(.01f * talents.getElementalPrecision());
        statsTalents.setBonusIntellectMultiplier(.02f * talents.getAncestralKnowledge());
        statsTalents.setPhysicalCrit(.01f * talents.getThunderingStrikes());
        statsTalents.setSpellCrit(0.01f * talents.getThunderingStrikes());
        statsTalents.setSpellPower(talents.isGlyphofTotemofWrath() ? talents.getTotemOfWrath() * 84 : 0);
        return statsTalents;
    }

    public Stats getCharacterStats(Character character, Item additionalItem) {
        CalculationOptionsElemental calcOpts = character.getCalculationOptions() instanceof CalculationOptionsElemental
                ? (CalculationOptionsElemental) character.getCalculationOptions()
                : null;

        Stats statsRace = BaseStats.getBaseStats(character);
        Stats statsItems = new Stats();
        accumulateItemStats(statsItems, character, additionalItem);
        Stats statsBuffs = getBuffsStats(character.getActiveBuffs());
        Stats statsTalents = getTalentStats(character.getShamanTalents());

        Stats statsTotal = Stats.add(Stats.add(Stats.add(statsRace, statsItems), statsBuffs), statsTalents);

        if (statsTotal.getHighestStat() > 0) {
            if (statsTotal.getSpirit() > statsTotal.getIntellect()) {
                statsTotal.setSpirit(statsTotal.getSpirit() + (statsTotal.getHighestStat() * 15f / 50f));
            } else {
                statsTotal.setIntellect(statsTotal.getIntellect() + (statsTotal.getHighestStat() * 15f / 50f));
            }
        }

        statsTotal.setStrength(statsTotal.getStrength() * (1 + statsTotal.getBonusStrengthMultiplier()));
        statsTotal.setAgility(statsTotal.getAgility() * (1 + statsTotal.getBonusAgilityMultiplier()));
        statsTotal.setStamina(statsTotal.getStamina() * (1 + statsTotal.getBonusStaminaMultiplier()));
        statsTotal.setIntellect(statsTotal.getIntellect() * (1 + statsTotal.getBonusIntellectMultiplier()));
        statsTotal.setSpirit(statsTotal.getSpirit() * (1 + statsTotal.getBonusSpiritMultiplier()));

        statsTotal.setStrength((float) Math.floor(statsTotal.getStrength()));
        statsTotal.setAgility((float) Math.floor(statsTotal.getAgility()));
        statsTotal.setStamina((float) Math.floor(statsTotal.getStamina()));
        statsTotal.setIntellect((float) Math.floor(statsTotal.getIntellect()));
        statsTotal.setSpirit((float) Math.floor(statsTotal.getSpirit()));

        statsTotal.setAttackPower(statsTotal.getAttackPower() + statsTotal.getStrength() + statsTotal.getAgility());
        statsTotal.setSpellPower(statsTotal.getSpellPower() + (float) Math.rint(statsTotal.getAttackPower() * .1f * character.getShamanTalents().getMentalQuickness()));

        statsTotal.setMana(statsTotal.getMana() + StatConversion.getManaFromIntellect(statsTotal.getIntellect()));
        statsTotal.setMana(statsTotal.getMana() * (float) Math.rint(1f + statsTotal.getBonusManaMultiplier()));

        statsTotal.setHealth(statsTotal.getHealth() + StatConversion.getHealthFromStamina(statsTotal.getStamina()));
        statsTotal.setHealth(statsTotal.getHealth() * (float) Math.rint(1f + statsTotal.getBonusHealthMultiplier()));

        statsTotal.setMp5(statsTotal.getMp5() + (float) Math.floor(statsTotal.getIntellect() * .04f * character.getShamanTalents().getUnrelentingStorm()));

        statsTotal.setSpellCrit(statsTotal.getSpellCrit() + StatConversion.getSpellCritFromRating(statsTotal.getCritRating()));
        statsTotal.setSpellCrit(statsTotal.getSpellCrit() + StatConversion.getSpellCritFromIntellect(statsTotal.getIntellect()));
        statsTotal.setSpellCrit(statsTotal.getSpellCrit() + statsTotal.getSpellCritOnTarget());
        statsTotal.setSpellHit(statsTotal.getSpellHit() + StatConversion.getSpellHitFromRating(statsTotal.getHitRating()));

        // Flametongue weapon assumed
        statsTotal.setSpellPower(statsTotal.getSpellPower() + (float) Math.floor(211 * (1f + character.getShamanTalents().getElementalWeapons() * .1f)));
        if (character.getShamanTalents().isGlyphofFlametongueWeapon()) statsTotal.setSpellCrit(statsTotal.getSpellCrit() + .02f);

        // Water shield assumed
        statsTotal.setMp5(statsTotal.getMp5() + 100);
        if (character.getShamanTalents().isGlyphofWaterMastery()) statsTotal.setMp5(statsTotal.getMp5() + 30);

        return statsTotal;
    }

    @Override
    public boolean isItemRelevant(Item item) {
        if ((item.getSlot() == ItemSlot.Ranged && item.getType() != ItemType.Totem))
            return false;
        return super.isItemRelevant(item);
    }

    @Override
    public boolean isBuffRelevant(Buff buff, Character character) {
        String name = buff.name;
        if (!buff.getAllowedClasses().contains(CharacterClass.Shaman)) {
            return false;
        }
        return super.isBuffRelevant(buff, character);
    }

    public Stats getRelevantStats(Stats stats) {
        Stats s = new Stats();
        s.setIntellect(stats.getIntellect());
        s.setMana(stats.getMana());
        s.setSpirit(stats.getSpirit());
        s.setSpellCrit(stats.getSpellCrit());
        s.setSpellCritOnTarget(stats.getSpellCritOnTarget());
        s.setSpellHit(stats.getSpellHit());
        s.setSpellHaste(stats.getSpellHaste());
        s.setSpellPower(stats.getSpellPower());
        s.setCritRating(stats.getCritRating());
        s.setHasteRating(stats.getHasteRating());
        s.setHitRating(stats.getHitRating());
        s.setSpellFireDamageRating(stats.getSpellFireDamageRating());
        s.setSpellNatureDamageRating(stats.getSpellNatureDamageRating());
        s.setSpellFrostDamageRating(stats.getSpellFrostDamageRating());
        s.setMp5(stats.getMp5());
        s.setManaRestoreFromMaxManaPerSecond(stats.getManaRestoreFromMaxManaPerSecond());
        s.setManaRestore(stats.getManaRestore());
        s.setManaRestoreFromBaseManaPPM(stats.getManaRestoreFromBaseManaPPM());
        s.setMovementSpeed(stats.getMovementSpeed());

        s.setBonusIntellectMultiplier(stats.getBonusIntellectMultiplier());
        s.setBonusSpiritMultiplier(stats.getBonusSpiritMultiplier());
        s.setBonusSpellCritMultiplier(stats.getBonusSpellCritMultiplier());
        s.setBonusSpellPowerMultiplier(stats.getBonusSpellPowerMultiplier());
        s.setBonusFireDamageMultiplier(stats.getBonusFireDamageMultiplier());
        s.setBonusNatureDamageMultiplier(stats.getBonusNatureDamageMultiplier());
        s.setBonusFrostDamageMultiplier(stats.getBonusFrostDamageMultiplier());
        s.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());

        s.setLightningSpellPower(stats.getLightningSpellPower());
        s.setLavaBurstBonus(stats.getLavaBurstBonus());

        s.setLightningBoltCostReduction(stats.getLightningBoltCostReduction());
        s.setLightningBoltDamageModifier(stats.getLightningBoltDamageModifier());
        s.setBonusLavaBurstCritDamage(stats.getBonusLavaBurstCritDamage());
        s.setBonusFlameShockDoTDamage(stats.getBonusFlameShockDoTDamage());
        s.setLightningBoltCritDamageModifier(stats.getLightningBoltCritDamageModifier());
        s.setBonusLavaBurstDamageMultiplier(stats.getBonusLavaBurstDamageMultiplier());
        s.setBonusFlameShockDuration(stats.getBonusFlameShockDuration());
        s.setElemental2T10(stats.getElemental2T10());
        s.setElemental4T10(stats.getElemental4T10());

        s.setNatureDamage(stats.getNatureDamage());
        s.setArcaneDamage(stats.getArcaneDamage());
        s.setFireDamage(stats.getFireDamage());
        s.setShadowDamage(stats.getShadowDamage());

        for (SpecialEffect effect : stats.specialEffects()) {
            if (effect.getTrigger() == Trigger.Use ||
                    effect.getTrigger() == Trigger.SpellCast ||
                    effect.getTrigger() == Trigger.SpellHit ||
                    effect.getTrigger() == Trigger.SpellCrit ||
                    effect.getTrigger() == Trigger.SpellMiss ||
                    effect.getTrigger() == Trigger.DamageSpellCast ||
                    effect.getTrigger() == Trigger.DamageSpellCrit ||
                    effect.getTrigger() == Trigger.DamageSpellHit ||
                    effect.getTrigger() == Trigger.DoTTick ||
                    effect.getTrigger() == Trigger.DamageDone ||
                    effect.getTrigger() == Trigger.DamageOrHealingDone ||
                    effect.getTrigger() == Trigger.ShamanLightningBolt ||
                    effect.getTrigger() == Trigger.ShamanFlameShockDoTTick ||
                    effect.getTrigger() == Trigger.ShamanShock) {
                if (hasRelevantStats(effect.getStats())) {
                    s.addSpecialEffect(effect);
                }
            }
        }
        return s;
    }

    @Override
    public boolean hasRelevantStats(Stats stats) {
        float elementalStats = 0;
        elementalStats +=
                stats.getIntellect() +
                        stats.getMana() +
                        stats.getSpirit() +
                        stats.getSpellCrit() +
                        stats.getSpellCritOnTarget() +
                        stats.getSpellHit() +
                        stats.getSpellHaste() +
                        stats.getSpellPower() +
                        stats.getCritRating() +
                        stats.getHasteRating() +
                        stats.getHitRating() +
                        stats.getSpellFireDamageRating() +
                        stats.getSpellNatureDamageRating() +
                        stats.getSpellFrostDamageRating() +
                        stats.getMp5() +
                        stats.getManaRestoreFromMaxManaPerSecond() +
                        stats.getManaRestore() +
                        stats.getManaRestoreFromBaseManaPPM() +
                        stats.getMovementSpeed();
        elementalStats +=
                stats.getBonusIntellectMultiplier() +
                        stats.getBonusSpiritMultiplier() +
                        stats.getBonusSpellCritMultiplier() +
                        stats.getBonusSpellPowerMultiplier() +
                        stats.getBonusFireDamageMultiplier() +
                        stats.getBonusNatureDamageMultiplier() +
                        stats.getBonusFrostDamageMultiplier() +
                        stats.getBonusDamageMultiplier();
        elementalStats +=
                stats.getLightningSpellPower() +
                        stats.getLavaBurstBonus();
        elementalStats +=
                stats.getBonusLavaBurstCritDamage() +
                        stats.getLightningBoltCostReduction() +
                        stats.getLightningBoltDamageModifier() +
                        stats.getLightningBoltCritDamageModifier() +
                        stats.getBonusFlameShockDoTDamage() +
                        stats.getBonusLavaBurstDamageMultiplier() +
                        stats.getBonusFlameShockDuration() +
                        stats.getElemental2T10() +
                        stats.getElemental4T10();
        elementalStats +=
                stats.getNatureDamage() +
                        stats.getArcaneDamage() +
                        stats.getFireDamage() +
                        stats.getShadowDamage();
        boolean relevant = (elementalStats > 0);
        for (SpecialEffect effect : stats.specialEffects()) {
            if (effect.getTrigger() == Trigger.Use ||
                    effect.getTrigger() == Trigger.SpellCast ||
                    effect.getTrigger() == Trigger.SpellHit ||
                    effect.getTrigger() == Trigger.SpellCrit ||
                    effect.getTrigger() == Trigger.SpellMiss ||
                    effect.getTrigger() == Trigger.DamageSpellCast ||
                    effect.getTrigger() == Trigger.DamageSpellCrit ||
                    effect.getTrigger() == Trigger.DamageSpellHit ||
                    effect.getTrigger() == Trigger.DoTTick ||
                    effect.getTrigger() == Trigger.DamageDone ||
                    effect.getTrigger() == Trigger.DamageOrHealingDone ||
                    effect.getTrigger() == Trigger.ShamanLightningBolt ||
                    effect.getTrigger() == Trigger.ShamanFlameShockDoTTick ||
                    effect.getTrigger() == Trigger.ShamanShock) {
                relevant |= hasRelevantStats(effect.getStats());
                if (relevant) break;
            }
        }
        return relevant;
    }

    @Override
    public boolean enchantFitsInSlot(Enchant enchant, Character character, ItemSlot slot) {
        // Filters out Non-Shield Offhand Enchants and Ranged Enchants
        if ((slot == ItemSlot.OffHand && enchant.slot != ItemSlot.OffHand) || slot == ItemSlot.Ranged) return false;
        return super.enchantFitsInSlot(enchant, character, slot);
    }
}
