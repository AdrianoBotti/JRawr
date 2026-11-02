// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/CalculationsMage.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: UI-only members omitted (CustomChartNames, CustomRenderedChartNames,
// CalculationOptionsPanel, GetCustomChartControl, UpdateCustomChartData, RenderCustomChart, the
// AdvancedSolverChanged/AdvancedSolverLogUpdated events). The C# SubPointNameColors property is
// ported additively as getSubPointNameColors() (rating variant only); the C#
// CharacterDisplayCalculationLabels array is ported additively as
// getCharacterDisplayCalculationLabels() (web display order/grouping only).
// FRAMEWORK ADAPTATION: DeserializeDataObject uses a JDK-XML reflective reader instead of XmlSerializer;
// AccumulateRawStats keeps the C# out-parameters as two List<Buff>[1] holders (the idiom used by Solver).
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterRace;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.items.Enchant;
import rawr.base.items.GemmingTemplate;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.items.Item;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.talents.MageTalents;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalculationsMage extends CalculationsBase {
    private List<GemmingTemplate> defaultGemmingTemplates = null;

    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        if (this.defaultGemmingTemplates == null) {
            this.defaultGemmingTemplates = new ArrayList<GemmingTemplate>();
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Uncommon (Purified)", false, 39911, 39957, 39956, 39959, 39946, 39941);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Uncommon (Royal)", false, 39911, 39957, 39956, 39959, 39946, 39943);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Uncommon (Glowing)", false, 39911, 39957, 39956, 39959, 39946, 39936);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Uncommon (Jewelcrafting)", false, 42144, 39957, 39956, 39959, 39946, 39941);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Rare (Purified)", false, 39998, 40049, 40048, 40051, 40047, 40026);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Rare (Royal)", false, 39998, 40049, 40048, 40051, 40047, 40027);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Rare (Glowing)", false, 39998, 40049, 40048, 40051, 40047, 40025);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Rare (Jewelcrafting)", false, 42144, 40049, 40048, 40051, 40047, 40026);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Epic (Purified)", true, 40113, 40153, 40152, 40155, 40151, 40133);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Epic (Royal)", false, 40113, 40153, 40152, 40155, 40151, 40134);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Epic (Glowing)", false, 40113, 40153, 40152, 40155, 40151, 40132);
            addGemmingTemplateGroup(this.defaultGemmingTemplates, "Epic (Jewelcrafting)", false, 42144, 40153, 40152, 40155, 40151, 40133);
        }
        return this.defaultGemmingTemplates;
    }

    private void addGemmingTemplateGroup(List<GemmingTemplate> list, String name, boolean enabled, int runed, int veiled, int potent, int reckless, int luminous, int blue) {
        list.add(makeGemmingTemplate(name, enabled, runed, runed, runed, runed));
        list.add(makeGemmingTemplate(name, enabled, runed, veiled, blue, runed));
        list.add(makeGemmingTemplate(name, enabled, runed, potent, blue, runed));
        list.add(makeGemmingTemplate(name, enabled, runed, reckless, blue, runed));
        list.add(makeGemmingTemplate(name, enabled, runed, luminous, blue, runed));
    }

    private GemmingTemplate makeGemmingTemplate(String name, boolean enabled, int redId, int yellowId, int blueId, int prismaticId) {
        GemmingTemplate template = new GemmingTemplate();
        template.setModel("Mage");
        template.setGroup(name);
        template.setRedId(redId);
        template.setYellowId(yellowId);
        template.setBlueId(blueId);
        template.setPrismaticId(prismaticId);
        template.setMetaId(41285);
        template.setEnabled(enabled);
        return template;
    }

    public static CalculationsMage instance;

    public static CalculationsMage getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("Mage", CalculationsMage.class);
    }

    public static void register() {
        Calculations.registerModel("Mage", CalculationsMage.class);
    }

    public CalculationsMage() {
        instance = this;
    }

    // C# CalculationsMage.cs:62-91 SubPointNameColors. The character ratings panel uses the "rating"
    // variant ("Dps" = ARGB 0,128,255; "Survivability" = 64,128,32), aligned with getSubPoints()
    // ([0] = dps rating, [1] = survivability). The C# property swaps to the "Mana" variant
    // ("Mana" = 0,0,255) for the Mana Sources/Mana Usage comparison charts; that chart-only variant
    // is intentionally not exposed through this additive API.
    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> colors = new LinkedHashMap<>();
        colors.put("Dps", "#0080FF");
        colors.put("Survivability", "#408020");
        return colors;
    }

    private List<String> characterDisplayCalculationLabels = null;

    // C# CalculationsMage.cs:103-193 CharacterDisplayCalculationLabels, copied verbatim (including
    // the '*' tooltips). The web layer strips the tooltip and matches labels by the part after ':'.
    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        if (characterDisplayCalculationLabels == null) {
            characterDisplayCalculationLabels = List.of(
                    "Basic Stats:Stamina",
                    "Basic Stats:Intellect",
                    "Basic Stats:Spirit",
                    "Basic Stats:Armor",
                    "Basic Stats:Health",
                    "Basic Stats:Mana",
                    "Spell Stats:Crit Rate",
                    "Spell Stats:Hit Rate",
                    "Spell Stats:Spell Penetration",
                    "Spell Stats:Haste",
                    "Spell Stats:Arcane Damage",
                    "Spell Stats:Fire Damage",
                    "Spell Stats:Frost Damage",
                    "Solution:Total Damage",
                    "Solution:Score",
                    "Solution:Dps",
                    "Solution:Tps*Threat per second",
                    "Solution:Spell Cycles",
                    "Solution:By Spell",
                    "Solution:Sequence*Cycle sequence reconstruction based on optimum cycles",
                    "Solution:Minimum Range",
                    "Solution:Threat Reduction",
                    "Spell Info:Wand",
                    "Spell Info:Arcane Missiles",
                    "Spell Info:MBAM*Missile Barrage Arcane Missiles",
                    "Spell Info:Arcane Blast(4)*Full debuff stack",
                    "Spell Info:Arcane Blast(0)*Non-debuffed",
                    "Spell Info:Arcane Barrage*Requires talent points",
                    "Spell Info:Scorch",
                    "Spell Info:Fire Blast",
                    "Spell Info:Pyroblast*Requires talent points",
                    "Spell Info:Fireball",
                    "Spell Info:FBPyro*Pyroblast on Hot Streak",
                    "Spell Info:FBLBPyro*Pyroblast on Hot Streak, maintain Living Bomb dot",
                    "Spell Info:ScLBPyro*Pyroblast on Hot Streak, maintain Living Bomb dot",
                    "Spell Info:FBScPyro*Maintain Scorch and Pyroblast on Hot Streak",
                    "Spell Info:FBScLBPyro*Maintain Scorch, maintain Living Bomb dot and Pyroblast on Hot Streak",
                    "Spell Info:Living Bomb",
                    "Spell Info:Frostfire Bolt",
                    "Spell Info:FFBPyro*Pyroblast on Hot Streak",
                    "Spell Info:FFBLBPyro*Pyroblast on Hot Streak, maintain Living Bomb dot",
                    "Spell Info:FFBScPyro*Maintain Scorch and Pyroblast on Hot Streak",
                    "Spell Info:FFBScLBPyro*Maintain Scorch, maintain Living Bomb dot and Pyroblast on Hot Streak",
                    "Spell Info:Frostbolt",
                    "Spell Info:Deep Freeze*Displayed values include Fingers of Frost benefit",
                    "Spell Info:Ice Lance",
                    "Spell Info:FrBFB*Fireball on Brain Freeze",
                    "Spell Info:FrBIL*Ice Lance on shatter combo",
                    "Spell Info:FrBDFFBIL*Fireball on non-FOF Brain Freeze, on shatter combo Deep Freeze > BF Fireball > Ice Lance",
                    "Spell Info:FrBFBIL*Fireball on Brain Freeze, Ice Lance on shatter combo, use Brain Freeze on shatter combo when available",
                    "Spell Info:FrBILFB*Fireball on Brain Freeze, always Ice Lance on shatter combo",
                    "Spell Info:FrBDFFFB*Frostfire Bolt on non-FOF Brain Freeze, on shatter combo Deep Freeze > BF Frostfire Bolt",
                    "Spell Info:AB2AM*AB-AB-AM regardless of procs",
                    "Spell Info:AB3AM023MBAM*AB-AB-AB-AM, MBAM on 0, 2 or 3 stack",
                    "Spell Info:AB4AM0234MBAM*AB-AB-AB-AB-AM, MBAM on 0, 2, 3 or 4 stack",
                    "Spell Info:ABSpam0234MBAM*Spam AB, MBAM on 0, 2, 3 or 4 stack",
                    "Spell Info:ABSpam024MBAM*Spam AB, MBAM on 0, 2 or 4 stack",
                    "Spell Info:ABSpam034MBAM*Spam AB, MBAM on 0, 3 or 4 stack",
                    "Spell Info:ABSpam04MBAM*Spam AB, MBAM on 0 or 4 stack",
                    "Spell Info:Arcane Explosion",
                    "Spell Info:Blizzard",
                    "Spell Info:Cone of Cold",
                    "Spell Info:Flamestrike*With full dot",
                    "Spell Info:Blast Wave*Requires talent points",
                    "Spell Info:Dragon's Breath*Requires talent points",
                    "Spell Info:Fire Ward*Set incoming fire damage under survivability settings",
                    "Spell Info:Frost Ward*Set incoming frost damage under survivability settings",
                    "Regeneration:MP5",
                    "Regeneration:Mana Regen",
                    "Regeneration:Health Regen",
                    "Survivability:Arcane Resist",
                    "Survivability:Fire Resist",
                    "Survivability:Nature Resist",
                    "Survivability:Frost Resist",
                    "Survivability:Shadow Resist",
                    "Survivability:Physical Mitigation",
                    "Survivability:Resilience",
                    "Survivability:Defense",
                    "Survivability:Crit Reduction",
                    "Survivability:Dodge",
                    "Survivability:Mean Incoming Dps",
                    "Survivability:Chance to Die");
        }
        return characterDisplayCalculationLabels;
    }

    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        xml = xml.replace("ArcaneBlast00", "ArcaneBlast0");
        xml = xml.replace("ArcaneBlast11", "ArcaneBlast1");
        xml = xml.replace("ArcaneBlast22", "ArcaneBlast2");
        xml = xml.replace("ArcaneBlast33", "ArcaneBlast3");
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            CalculationOptionsMage calcOpts = new CalculationOptionsMage();
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
            if (CalculationOptionsMage.isXmlIgnoredField(name)) continue;
            java.lang.reflect.Method setter = findSetter(target.getClass(), "set" + name);
            if (setter == null) continue;
            Class<?> type = setter.getParameterTypes()[0];
            Object value = readXmlValue(child, type, setter.getGenericParameterTypes()[0]);
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

    private static Object readXmlValue(org.w3c.dom.Element element, Class<?> type, java.lang.reflect.Type genericType) {
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
            Class<?> elementType = Object.class;
            if (genericType instanceof java.lang.reflect.ParameterizedType) {
                java.lang.reflect.Type arg = ((java.lang.reflect.ParameterizedType) genericType).getActualTypeArguments()[0];
                if (arg instanceof Class) elementType = (Class<?>) arg;
            }
            List<Object> list = new ArrayList<Object>();
            for (org.w3c.dom.Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                Object item = newXmlInstance(elementType);
                readXmlObject((org.w3c.dom.Element) node, item);
                list.add(item);
            }
            return list;
        }
        if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            List<Object> values = new ArrayList<Object>();
            for (org.w3c.dom.Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                values.add(readXmlValue((org.w3c.dom.Element) node, componentType, componentType));
            }
            Object array = java.lang.reflect.Array.newInstance(componentType, values.size());
            for (int i = 0; i < values.size(); i++) {
                java.lang.reflect.Array.set(array, i, values.get(i));
            }
            return array;
        }
        Object nested = newXmlInstance(type);
        readXmlObject(element, nested);
        return nested;
    }

    private static Object newXmlInstance(Class<?> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public int getMaxDegreeOfParallelism() {
        return ArrayPool.getMaximumPoolSize();
    }

    public CharacterClass getTargetClass() {
        return CharacterClass.Mage;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationMage();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsMage();
    }

    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        if (referenceCalculation && significantChange) {
            // Fresh top-level character calculation: drop shared effect interpolation caches so a
            // previous character cannot leak into this one (SUSPECTED_BUGS #15). Comparisons and
            // internal re-calculations keep the caches, matching the original per-process behavior.
            rawr.base.effects.SpecialEffect.clearInterpolatorCaches();
        }
        CalculationOptionsMage calculationOptions = (CalculationOptionsMage) character.getCalculationOptions();
        boolean computeIncrementalSet = referenceCalculation && calculationOptions.getIncrementalOptimizations();
        boolean useGlobalOptimizations = calculationOptions.getSmartOptimization() && !significantChange;
        boolean useIncrementalOptimizations = calculationOptions.getIncrementalOptimizations() && (!significantChange || calculationOptions.getForceIncrementalOptimizations());
        if (useIncrementalOptimizations && calculationOptions.getIncrementalSetStateIndexes() == null) computeIncrementalSet = true;
        if (computeIncrementalSet) {
            useIncrementalOptimizations = false;
            needsDisplayCalculations = true;
        }
        if (useIncrementalOptimizations && !character.getDisableBuffAutoActivation()) {
            return getCharacterCalculations(character, additionalItem, calculationOptions, calculationOptions.getIncrementalSetArmor(), useIncrementalOptimizations, useGlobalOptimizations, needsDisplayCalculations, computeIncrementalSet);
        } else if (calculationOptions.getAutomaticArmor() && !character.getDisableBuffAutoActivation()) {
            CharacterCalculationsBase mage = getCharacterCalculations(character, additionalItem, calculationOptions, "Mage Armor", useIncrementalOptimizations, useGlobalOptimizations, needsDisplayCalculations, computeIncrementalSet);
            CharacterCalculationsBase molten = getCharacterCalculations(character, additionalItem, calculationOptions, "Molten Armor", useIncrementalOptimizations, useGlobalOptimizations, needsDisplayCalculations, computeIncrementalSet);
            CharacterCalculationsBase calc = (mage.getOverallPoints() > molten.getOverallPoints()) ? mage : molten;
            if (calculationOptions.getMeleeDps() + calculationOptions.getMeleeDot() + calculationOptions.getPhysicalDps() + calculationOptions.getPhysicalDot() + calculationOptions.getFrostDps() + calculationOptions.getFrostDot() > 0) {
                CharacterCalculationsBase ice = getCharacterCalculations(character, additionalItem, calculationOptions, "Ice Armor", useIncrementalOptimizations, useGlobalOptimizations, needsDisplayCalculations, computeIncrementalSet);
                if (ice.getOverallPoints() > calc.getOverallPoints()) calc = ice;
            }
            if (computeIncrementalSet) storeIncrementalSet(character, ((CharacterCalculationsMage) calc).displayCalculations);
            return calc;
        } else {
            CharacterCalculationsBase calc = getCharacterCalculations(character, additionalItem, calculationOptions, null, useIncrementalOptimizations, useGlobalOptimizations, needsDisplayCalculations, computeIncrementalSet);
            if (computeIncrementalSet) storeIncrementalSet(character, ((CharacterCalculationsMage) calc).displayCalculations);
            return calc;
        }
    }

    private void storeIncrementalSet(Character character, DisplayCalculations calculations) {
        CalculationOptionsMage calculationOptions = (CalculationOptionsMage) character.getCalculationOptions();
        List<Integer> cooldownList = new ArrayList<Integer>();
        List<CycleId> spellList = new ArrayList<CycleId>();
        List<Integer> segmentList = new ArrayList<Integer>();
        List<Integer> manaSegmentList = new ArrayList<Integer>();
        List<VariableType> variableTypeList = new ArrayList<VariableType>();
        for (int i = 0; i < calculations.solutionVariable.size(); i++) {
            if (calculations.solution[i] > 0) {
                VariableType type = calculations.solutionVariable.get(i).type;
                int cooldown;
                if (calculations.solutionVariable.get(i).state != null) {
                    cooldown = calculations.solutionVariable.get(i).state.getEffects() & StandardEffect.NonItemBasedMask;
                } else {
                    cooldown = 0;
                }
                CycleId spellId;
                if (calculations.solutionVariable.get(i).type == VariableType.Spell) {
                    spellId = calculations.solutionVariable.get(i).cycle.cycleId;
                } else {
                    spellId = CycleId.None;
                }
                int segment = calculations.solutionVariable.get(i).segment;
                int manaSegment = calculations.solutionVariable.get(i).manaSegment;
                boolean found = false;
                for (int j = 0; j < cooldownList.size(); j++) {
                    if (cooldownList.get(j) == cooldown && spellList.get(j) == spellId && segmentList.get(j) == segment && manaSegmentList.get(j) == manaSegment && variableTypeList.get(j) == type) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    cooldownList.add(cooldown);
                    spellList.add(spellId);
                    segmentList.add(segment);
                    manaSegmentList.add(manaSegment);
                    variableTypeList.add(type);
                }
            }
        }
        calculationOptions.setIncrementalSetStateIndexes(toIntArray(cooldownList));
        calculationOptions.setIncrementalSetSpells(spellList.toArray(new CycleId[0]));
        calculationOptions.setIncrementalSetSegments(toIntArray(segmentList));
        calculationOptions.setIncrementalSetVariableType(variableTypeList.toArray(new VariableType[0]));
        calculationOptions.setIncrementalSetManaSegment(toIntArray(manaSegmentList));
        if (calculationOptions.getAutomaticArmor()) {
            calculationOptions.setIncrementalSetArmor(calculations.getMageArmor());
        } else {
            calculationOptions.setIncrementalSetArmor(null);
        }

        List<Integer> filteredCooldowns = ListUtils.removeDuplicates(cooldownList);
        java.util.Collections.sort(filteredCooldowns);
        calculationOptions.setIncrementalSetSortedStates(toIntArray(filteredCooldowns));
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] result = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, CalculationOptionsMage calculationOptions, String armor, boolean useIncrementalOptimizations, boolean useGlobalOptimizations, boolean needsDisplayCalculations, boolean needsSolutionVariables) {
        return Solver.getCharacterCalculations(character, additionalItem, calculationOptions, this, armor, calculationOptions.getComparisonSegmentCooldowns(), calculationOptions.getComparisonSegmentMana(), calculationOptions.getComparisonIntegralMana(), calculationOptions.getComparisonAdvancedConstraintsLevel(), useIncrementalOptimizations, useGlobalOptimizations, needsDisplayCalculations, needsSolutionVariables);
    }

    public static final Buff ImprovedScorchBuff = Buff.getBuffByName("Improved Scorch");
    public static final Buff WintersChillBuff = Buff.getBuffByName("Winter's Chill");
    public static final Buff MoltenArmorBuff = Buff.getBuffByName("Molten Armor");
    public static final Buff MageArmorBuff = Buff.getBuffByName("Mage Armor");
    public static final Buff IceArmorBuff = Buff.getBuffByName("Ice Armor");

    public void accumulateRawStats(Stats stats, Character character, Item additionalItem, CalculationOptionsMage calculationOptions, List<Buff>[] autoActivatedBuffs, String armor, List<Buff>[] activeBuffs) {
        accumulateItemStats(stats, character, additionalItem);

        boolean activeBuffsCloned = false;
        List<Buff> activeBuffsList = character.getActiveBuffs();
        List<Buff> autoActivatedBuffsList = null;

        if (!character.getDisableBuffAutoActivation()) {
            MageTalents talents = character.getMageTalents();
            if (calculationOptions.getMaintainScorch()) {
                if (talents.getImprovedScorch() > 0) {
                    if (!character.getActiveBuffs().contains(ImprovedScorchBuff) && !character.getActiveBuffs().contains(WintersChillBuff)) {
                        if (!activeBuffsCloned) {
                            activeBuffsList = new ArrayList<Buff>(character.getActiveBuffs());
                            autoActivatedBuffsList = new ArrayList<Buff>();
                            activeBuffsCloned = true;
                        }
                        activeBuffsList.add(ImprovedScorchBuff);
                        autoActivatedBuffsList.add(ImprovedScorchBuff);
                        Calculations.removeConflictingBuffs(activeBuffsList, ImprovedScorchBuff);
                    }
                }
            }
            if (talents.getWintersChill() > 0) {
                if (!character.getActiveBuffs().contains(WintersChillBuff) && !character.getActiveBuffs().contains(ImprovedScorchBuff)) {
                    if (!activeBuffsCloned) {
                        activeBuffsList = new ArrayList<Buff>(character.getActiveBuffs());
                        autoActivatedBuffsList = new ArrayList<Buff>();
                        activeBuffsCloned = true;
                    }
                    activeBuffsList.add(WintersChillBuff);
                    autoActivatedBuffsList.add(WintersChillBuff);
                    Calculations.removeConflictingBuffs(activeBuffsList, WintersChillBuff);
                }
            }
            if (armor != null) {
                Buff armorBuff = null;
                switch (armor) {
                    case "Molten Armor":
                        armorBuff = MoltenArmorBuff;
                        break;
                    case "Mage Armor":
                        armorBuff = MageArmorBuff;
                        break;
                    case "Ice Armor":
                        armorBuff = IceArmorBuff;
                        break;
                }
                if (!character.getActiveBuffs().contains(armorBuff)) {
                    if (!activeBuffsCloned) {
                        activeBuffsList = new ArrayList<Buff>(character.getActiveBuffs());
                        autoActivatedBuffsList = new ArrayList<Buff>();
                        activeBuffsCloned = true;
                    }
                    activeBuffsList.add(armorBuff);
                    autoActivatedBuffsList.add(armorBuff);
                    Calculations.removeConflictingBuffs(activeBuffsList, armorBuff);
                }
            }
        }

        accumulateBuffsStats(stats, activeBuffsList);

        for (int i = 0; i < stats.getRawSpecialEffectDataSize(); i++) {
            SpecialEffect effect = stats.getRawSpecialEffectData()[i];
            if (effect.getMaxStack() > 1 && effect.getChance() == 1f && effect.getCooldown() == 0f && (effect.getTrigger() == Trigger.DamageSpellCast || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.SpellHit)) {
                effect.getStats().generateSparseData();
                stats.accumulate(effect.getStats(), effect.getMaxStack());
            }
        }

        autoActivatedBuffs[0] = autoActivatedBuffsList;
        activeBuffs[0] = activeBuffsList;
    }

    public Stats getCharacterStats(Character character, Item additionalItem) {
        CalculationOptionsMage calculationOptions = (CalculationOptionsMage) character.getCalculationOptions();
        @SuppressWarnings("unchecked")
        List<Buff>[] ignore2 = new List[1];
        @SuppressWarnings("unchecked")
        List<Buff>[] ignore = new List[1];
        Stats stats = new Stats();
        accumulateRawStats(stats, character, additionalItem, calculationOptions, ignore2, null, ignore);
        return getCharacterStats(character, additionalItem, stats, calculationOptions);
    }

    public Stats getCharacterStats(Character character, Item additionalItem, Stats rawStats, CalculationOptionsMage calculationOptions) {
        float statsRaceHealth;
        float statsRaceMana;
        float statsRaceStrength;
        float statsRaceAgility;
        float statsRaceStamina;
        float statsRaceIntellect;
        float statsRaceSpirit;
        switch (calculationOptions.getPlayerLevel()) {
            case 70:
                statsRaceHealth = 3213f;
                statsRaceMana = 1961f;
                switch (character.getRace()) {
                    case BloodElf:
                        statsRaceStrength = 28f;
                        statsRaceAgility = 42f;
                        statsRaceStamina = 49f;
                        statsRaceIntellect = 149f;
                        statsRaceSpirit = 144;
                        break;
                    case Draenei:
                        statsRaceStrength = 28f;
                        statsRaceAgility = 42f;
                        statsRaceStamina = 50f;
                        statsRaceIntellect = 152f;
                        statsRaceSpirit = 147;
                        break;
                    case Human:
                        statsRaceStrength = 28f;
                        statsRaceAgility = 42f;
                        statsRaceStamina = 51f;
                        statsRaceIntellect = 151f;
                        statsRaceSpirit = 145;
                        break;
                    case Troll:
                        statsRaceStrength = 28f;
                        statsRaceAgility = 42f;
                        statsRaceStamina = 52f;
                        statsRaceIntellect = 147f;
                        statsRaceSpirit = 146;
                        break;
                    case Undead:
                        statsRaceStrength = 28f;
                        statsRaceAgility = 42f;
                        statsRaceStamina = 52f;
                        statsRaceIntellect = 149f;
                        statsRaceSpirit = 150;
                        break;
                    case Gnome:
                    default:
                        statsRaceStrength = 28f;
                        statsRaceAgility = 42f;
                        statsRaceStamina = 50f;
                        statsRaceIntellect = 154f;
                        statsRaceSpirit = 145;
                        break;
                }
                break;
            case 71:
                statsRaceHealth = 3308f;
                statsRaceMana = 2063f;
                statsRaceStrength = 28f;
                statsRaceAgility = 42f;
                statsRaceStamina = 51f;
                statsRaceIntellect = 157f;
                statsRaceSpirit = 148f;
                break;
            case 72:
                statsRaceHealth = 3406f;
                statsRaceMana = 2166f;
                statsRaceStrength = 28f;
                statsRaceAgility = 43f;
                statsRaceStamina = 52f;
                statsRaceIntellect = 160f;
                statsRaceSpirit = 151f;
                break;
            case 73:
                statsRaceHealth = 3505f;
                statsRaceMana = 2269f;
                statsRaceStrength = 28f;
                statsRaceAgility = 43f;
                statsRaceStamina = 53f;
                statsRaceIntellect = 163f;
                statsRaceSpirit = 154f;
                break;
            case 74:
                statsRaceHealth = 3623f;
                statsRaceMana = 2372f;
                statsRaceStrength = 29f;
                statsRaceAgility = 44f;
                statsRaceStamina = 53f;
                statsRaceIntellect = 166f;
                statsRaceSpirit = 156f;
                break;
            case 75:
                statsRaceHealth = 3726f;
                statsRaceMana = 2474f;
                statsRaceStrength = 29f;
                statsRaceAgility = 44f;
                statsRaceStamina = 54f;
                statsRaceIntellect = 169f;
                statsRaceSpirit = 159f;
                break;
            case 76:
                statsRaceHealth = 3830f;
                statsRaceMana = 2577f;
                statsRaceStrength = 29f;
                statsRaceAgility = 44f;
                statsRaceStamina = 55f;
                statsRaceIntellect = 172f;
                statsRaceSpirit = 162f;
                break;
            case 77:
                statsRaceHealth = 3937f;
                statsRaceMana = 2680f;
                statsRaceStrength = 30f;
                statsRaceAgility = 45f;
                statsRaceStamina = 56f;
                statsRaceIntellect = 175f;
                statsRaceSpirit = 165f;
                break;
            case 78:
                statsRaceHealth = 4063f;
                statsRaceMana = 2783f;
                statsRaceStrength = 30f;
                statsRaceAgility = 45f;
                statsRaceStamina = 56f;
                statsRaceIntellect = 178f;
                statsRaceSpirit = 168f;
                break;
            case 79:
                statsRaceHealth = 4172f;
                statsRaceMana = 2885f;
                statsRaceStrength = 30f;
                statsRaceAgility = 46f;
                statsRaceStamina = 57f;
                statsRaceIntellect = 181f;
                statsRaceSpirit = 171f;
                break;
            case 80:
            default:
                statsRaceHealth = 6783f;
                statsRaceMana = 2988f;
                switch (character.getRace()) {
                    case BloodElf:
                        statsRaceStrength = 33f;
                        statsRaceAgility = 45f;
                        statsRaceStamina = 57f;
                        statsRaceIntellect = 185f;
                        statsRaceSpirit = 173f;
                        break;
                    case Draenei:
                        statsRaceStrength = 37f;
                        statsRaceAgility = 40f;
                        statsRaceStamina = 58f;
                        statsRaceIntellect = 182f;
                        statsRaceSpirit = 176f;
                        break;
                    case Human:
                        statsRaceStrength = 36f;
                        statsRaceAgility = 43f;
                        statsRaceStamina = 59f;
                        statsRaceIntellect = 181f;
                        statsRaceSpirit = 174f;
                        break;
                    case Troll:
                        statsRaceStrength = 37f;
                        statsRaceAgility = 45f;
                        statsRaceStamina = 60f;
                        statsRaceIntellect = 177f;
                        statsRaceSpirit = 175f;
                        break;
                    case Undead:
                        statsRaceStrength = 35f;
                        statsRaceAgility = 41f;
                        statsRaceStamina = 60f;
                        statsRaceIntellect = 179f;
                        statsRaceSpirit = 179f;
                        break;
                    case Gnome:
                    default:
                        statsRaceStrength = 31f;
                        statsRaceAgility = 45f;
                        statsRaceStamina = 59f;
                        statsRaceIntellect = 184f;
                        statsRaceSpirit = 174f;
                        break;
                }
                break;
            case 81:
                statsRaceHealth = 9551f;
                statsRaceMana = 4287f;
                statsRaceStrength = 31f;
                statsRaceAgility = 46f;
                statsRaceStamina = 60f;
                statsRaceIntellect = 188f;
                statsRaceSpirit = 177f;
                break;
            case 82:
                statsRaceHealth = 13418f;
                statsRaceMana = 6102f;
                statsRaceStrength = 31f;
                statsRaceAgility = 46f;
                statsRaceStamina = 60f;
                statsRaceIntellect = 191f;
                statsRaceSpirit = 180f;
                break;
            case 83:
                statsRaceHealth = 13418f;
                statsRaceMana = 6102f;
                statsRaceStrength = 31f;
                statsRaceAgility = 46f;
                statsRaceStamina = 60f;
                statsRaceIntellect = 191f;
                statsRaceSpirit = 180f;
                break;
            case 84:
                statsRaceHealth = 13418f;
                statsRaceMana = 6102f;
                statsRaceStrength = 31f;
                statsRaceAgility = 46f;
                statsRaceStamina = 60f;
                statsRaceIntellect = 191f;
                statsRaceSpirit = 180f;
                break;
            case 85:
                statsRaceHealth = 13418f;
                statsRaceMana = 6102f;
                statsRaceStrength = 31f;
                statsRaceAgility = 46f;
                statsRaceStamina = 60f;
                statsRaceIntellect = 191f;
                statsRaceSpirit = 180f;
                break;
        }
        MageTalents talents = character.getMageTalents();

        float statsRaceBonusIntellectMultiplier = 0.0f;
        float statsRaceBonusManaMultiplier = 0.0f;
        if (character.getRace() == CharacterRace.Gnome) {
            if (calculationOptions.getBeta()) {
                statsRaceBonusManaMultiplier = 0.05f;
            } else {
                statsRaceBonusIntellectMultiplier = 0.05f;
            }
        }
        float statsTalentBonusIntellectMultiplier = 0.03f * talents.getArcaneMind();
        float statsRaceBonusSpiritMultiplier = 0.0f;
        if (character.getRace() == CharacterRace.Human) {
            statsRaceBonusSpiritMultiplier = 0.03f;
        }

        Stats statsTotal = rawStats;

        float statsTalentBonusSpiritMultiplier = 0.0f;
        if (talents.getStudentOfTheMind() > 0) {
            statsTalentBonusSpiritMultiplier = 0.01f + 0.03f * talents.getStudentOfTheMind();
        }
        if (calculationOptions.getEffectSpiritMultiplier() != 1.0f) {
            statsTotal.setBonusSpiritMultiplier((1 + statsTotal.getBonusSpiritMultiplier()) * calculationOptions.getEffectSpiritMultiplier() - 1);
        }
        statsTotal.setStrength((float) Math.rint((statsRaceStrength + statsTotal.getStrength()) * (1 + statsTotal.getBonusStrengthMultiplier()) - 0.00001));
        statsTotal.setAgility((float) Math.rint((statsRaceAgility + statsTotal.getAgility()) * (1 + statsTotal.getBonusAgilityMultiplier()) - 0.00001));
        statsTotal.setIntellect((float) Math.rint((Math.floor(0.00001 + statsRaceIntellect * (1 + statsRaceBonusIntellectMultiplier) * (1 + statsTalentBonusIntellectMultiplier)) + Math.floor(0.00001 + statsTotal.getIntellect() * (1 + statsRaceBonusIntellectMultiplier) * (1 + statsTalentBonusIntellectMultiplier))) * (1 + statsTotal.getBonusIntellectMultiplier()) - 0.00001));
        statsTotal.setStamina((float) Math.rint((statsRaceStamina + statsTotal.getStamina()) * (1 + statsTotal.getBonusStaminaMultiplier()) - 0.00001));
        statsTotal.setSpirit((float) Math.rint((Math.floor(0.00001 + statsRaceSpirit * (1 + statsRaceBonusSpiritMultiplier) * (1 + statsTalentBonusSpiritMultiplier)) + Math.floor(0.00001 + statsTotal.getSpirit() * (1 + statsRaceBonusSpiritMultiplier) * (1 + statsTalentBonusSpiritMultiplier))) * (1 + statsTotal.getBonusSpiritMultiplier()) - 0.00001));

        statsTotal.setHealth((float) Math.rint((statsTotal.getHealth() + statsRaceHealth + (statsTotal.getStamina() * 10f)) * (character.getRace() == CharacterRace.Tauren ? 1.05f : 1f) * (1 + statsTotal.getBonusHealthMultiplier())));
        statsTotal.setMana((float) Math.rint((statsTotal.getMana() + statsRaceMana + 15f * statsTotal.getIntellect()) * (1 + statsRaceBonusManaMultiplier)));
        if (calculationOptions.getBeta()) {
            statsTotal.setArmor((float) Math.rint(statsTotal.getArmor() + 0.5f * statsTotal.getIntellect() * talents.getArcaneFortitude()));
        } else {
            statsTotal.setArmor((float) Math.rint(statsTotal.getArmor() + statsTotal.getAgility() * 2f + 0.5f * statsTotal.getIntellect() * talents.getArcaneFortitude()));
        }

        if (character.getRace() == CharacterRace.BloodElf) {
            statsTotal.setMp5(statsTotal.getMp5() + 5 * 0.06f * statsTotal.getMana() / 120);
        }

        float magicAbsorption = 0.5f * calculationOptions.getPlayerLevel() * talents.getMagicAbsorption();
        int frostWarding = talents.getFrostWarding();

        if (statsTotal.getMageIceArmor() > 0) {
            statsTotal.setArmor(statsTotal.getArmor() + (float) Math.floor((calculationOptions.getPlayerLevel() < 79 ? 645 : 940) * (1 + 0.25f * frostWarding + (talents.isGlyphOfIceArmor() ? 0.5f : 0.0f) + statsTotal.getMage2T9() * 0.2f)));
            statsTotal.setFrostResistance(statsTotal.getFrostResistance() + (float) Math.floor((calculationOptions.getPlayerLevel() < 79 ? 18 : 40) * (1 + 0.25f * frostWarding + (talents.isGlyphOfIceArmor() ? 0.5f : 0.0f))));
        }
        if (statsTotal.getMageMageArmor() > 0) {
            statsTotal.setSpellCombatManaRegeneration(statsTotal.getSpellCombatManaRegeneration() + (0.5f + (talents.isGlyphOfMageArmor() ? 0.2f : 0.0f) + 0.1f * statsTotal.getMage2T9()));
            magicAbsorption += (calculationOptions.getPlayerLevel() < 71 ? 18f : (calculationOptions.getPlayerLevel() < 79 ? 21f : 40f)) * (1 + talents.getArcaneShielding() * 0.25f);
        }
        if (statsTotal.getMageMoltenArmor() > 0) {
            if (calculationOptions.getBeta()) {
                statsTotal.setSpellCrit(statsTotal.getSpellCrit() + (0.03f + (talents.isGlyphOfMoltenArmor() ? 0.02f : 0.0f)));
            } else {
                statsTotal.setCritRating(statsTotal.getCritRating() + (0.35f + (talents.isGlyphOfMoltenArmor() ? 0.2f : 0.0f) + 0.15f * statsTotal.getMage2T9()) * statsTotal.getSpirit());
            }
        }
        if (calculationOptions.getEffectCritBonus() > 0) {
            statsTotal.setSpellCrit(statsTotal.getSpellCrit() + calculationOptions.getEffectCritBonus());
        }
        if (talents.isGlyphOfManaGem()) {
            statsTotal.setBonusManaGem(statsTotal.getBonusManaGem() + 0.4f);
        }

        switch (talents.getArcaneMeditation()) {
            case 1:
                statsTotal.setSpellCombatManaRegeneration(statsTotal.getSpellCombatManaRegeneration() + 0.17f);
                break;
            case 2:
                statsTotal.setSpellCombatManaRegeneration(statsTotal.getSpellCombatManaRegeneration() + 0.33f);
                break;
            case 3:
                statsTotal.setSpellCombatManaRegeneration(statsTotal.getSpellCombatManaRegeneration() + 0.5f);
                break;
        }
        switch (talents.getPyromaniac()) {
            case 1:
                statsTotal.setSpellCombatManaRegeneration(statsTotal.getSpellCombatManaRegeneration() + 0.17f);
                break;
            case 2:
                statsTotal.setSpellCombatManaRegeneration(statsTotal.getSpellCombatManaRegeneration() + 0.33f);
                break;
            case 3:
                statsTotal.setSpellCombatManaRegeneration(statsTotal.getSpellCombatManaRegeneration() + 0.5f);
                break;
        }

        if (statsTotal.getSpellCombatManaRegeneration() > 1.0f) statsTotal.setSpellCombatManaRegeneration(1.0f);

        float spellDamageFromIntellectPercentage = 0.03f * talents.getMindMastery();

        statsTotal.setSpellPower(statsTotal.getSpellPower() + (float) Math.floor(statsTotal.getBonusSpellPowerDemonicPactMultiplier() * calculationOptions.getWarlockSpellPower()));
        statsTotal.setSpellPower(statsTotal.getSpellPower() + (float) Math.floor(spellDamageFromIntellectPercentage * statsTotal.getIntellect()));
        if (calculationOptions.getBeta()) {
            statsTotal.setSpellPower(statsTotal.getSpellPower() + (statsTotal.getIntellect() - 10));
        }

        statsTotal.setCritBonusDamage(statsTotal.getCritBonusDamage() + calculationOptions.getEffectCritDamageBonus());

        statsTotal.setArcaneResistance(statsTotal.getArcaneResistance() + (magicAbsorption + statsTotal.getArcaneResistanceBuff()));
        statsTotal.setFireResistance(statsTotal.getFireResistance() + (magicAbsorption + statsTotal.getFireResistanceBuff()));
        statsTotal.setFrostResistance(statsTotal.getFrostResistance() + (magicAbsorption + statsTotal.getFrostResistanceBuff()));
        statsTotal.setNatureResistance(statsTotal.getNatureResistance() + (magicAbsorption + statsTotal.getNatureResistanceBuff()));
        statsTotal.setShadowResistance(statsTotal.getShadowResistance() + (magicAbsorption + statsTotal.getShadowResistanceBuff()));

        int playerLevel = calculationOptions.getPlayerLevel();
        float maxHitRate = 1.0f;
        float bossHitRate = Math.min(maxHitRate, ((playerLevel <= calculationOptions.getTargetLevel() + 2) ? (0.96f - (playerLevel - calculationOptions.getTargetLevel()) * 0.01f) : (0.94f - (playerLevel - calculationOptions.getTargetLevel() - 2) * 0.11f)));
        statsTotal.setMp5(statsTotal.getMp5() - 5 * calculationOptions.getEffectShadowManaDrain() * calculationOptions.getEffectShadowManaDrainFrequency() * bossHitRate * (1 - rawr.base.stats.StatConversion.getAverageResistance(calculationOptions.getTargetLevel(), calculationOptions.getPlayerLevel(), statsTotal.getShadowResistance(), 0)));

        float fullResistRate = calculationOptions.getEffectArcaneOtherBinary() * (1 - bossHitRate * (1 - rawr.base.stats.StatConversion.getAverageResistance(calculationOptions.getTargetLevel(), calculationOptions.getPlayerLevel(), statsTotal.getArcaneResistance(), 0)));
        fullResistRate += calculationOptions.getEffectFireOtherBinary() * (1 - bossHitRate * (1 - rawr.base.stats.StatConversion.getAverageResistance(calculationOptions.getTargetLevel(), calculationOptions.getPlayerLevel(), statsTotal.getFireResistance(), 0)));
        fullResistRate += calculationOptions.getEffectFrostOtherBinary() * (1 - bossHitRate * (1 - rawr.base.stats.StatConversion.getAverageResistance(calculationOptions.getTargetLevel(), calculationOptions.getPlayerLevel(), statsTotal.getFrostResistance(), 0)));
        fullResistRate += calculationOptions.getEffectShadowOtherBinary() * (1 - bossHitRate * (1 - rawr.base.stats.StatConversion.getAverageResistance(calculationOptions.getTargetLevel(), calculationOptions.getPlayerLevel(), statsTotal.getShadowResistance(), 0)));
        fullResistRate += calculationOptions.getEffectNatureOtherBinary() * (1 - bossHitRate * (1 - rawr.base.stats.StatConversion.getAverageResistance(calculationOptions.getTargetLevel(), calculationOptions.getPlayerLevel(), statsTotal.getNatureResistance(), 0)));
        fullResistRate += calculationOptions.getEffectHolyOtherBinary() * (1 - bossHitRate);
        fullResistRate += calculationOptions.getEffectArcaneOther() * (1 - bossHitRate);
        fullResistRate += calculationOptions.getEffectFireOther() * (1 - bossHitRate);
        fullResistRate += calculationOptions.getEffectFrostOther() * (1 - bossHitRate);
        fullResistRate += calculationOptions.getEffectShadowOther() * (1 - bossHitRate);
        fullResistRate += calculationOptions.getEffectNatureOther() * (1 - bossHitRate);
        fullResistRate += calculationOptions.getEffectHolyOther() * (1 - bossHitRate);
        fullResistRate += calculationOptions.getEffectShadowManaDrainFrequency() * (1 - bossHitRate);
        fullResistRate += calculationOptions.getEffectShadowSilenceFrequency() * (1 - bossHitRate * rawr.base.stats.StatConversion.getAverageResistance(calculationOptions.getTargetLevel(), calculationOptions.getPlayerLevel(), statsTotal.getShadowResistance(), 0));
        statsTotal.setMp5(statsTotal.getMp5() + 5 * Math.min(1f, fullResistRate) * 0.01f * talents.getMagicAbsorption() * statsTotal.getMana());

        return statsTotal;
    }

    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        List<ComparisonCalculationBase> comparisonList = new ArrayList<ComparisonCalculationBase>();
        ComparisonCalculationBase comparison;
        float[] subPoints;

        CalculationOptionsMage calculationOptions = (CalculationOptionsMage) character.getCalculationOptions();
        MageTalents talents = character.getMageTalents();
        DisplayCalculations calculationResult = calculationOptions.getCalculations();

        switch (chartName) {
            case "Item Budget":
                Stats[] statsList = new Stats[] {
                        statSpellPower(1.17f),
                        statMp5(0.4f),
                        statCritRating(1),
                        statHasteRating(1),
                        statHitRating(1),
                        statIntellect(1),
                        statSpirit(1),
                };

                return evaluateItemBudget(character, statsList);
            case "Mana Sources":
                for (Map.Entry<String, Float> kvp : calculationResult.getManaSources().entrySet()) {
                    if (kvp.getValue() > 0) {
                        comparison = createNewComparisonCalculation();
                        comparison.setName(kvp.getKey());
                        comparison.setEquipped(false);
                        comparison.setOverallPoints(kvp.getValue());
                        subPoints = new float[] { kvp.getValue() };
                        comparison.setSubPoints(subPoints);

                        comparisonList.add(comparison);
                    }
                }
                return comparisonList.toArray(new ComparisonCalculationBase[0]);
            case "Mana Usage":
                for (Map.Entry<String, Float> kvp : calculationResult.getManaUsage().entrySet()) {
                    if (kvp.getValue() > 0) {
                        comparison = createNewComparisonCalculation();
                        comparison.setName(kvp.getKey());
                        comparison.setEquipped(false);
                        comparison.setOverallPoints(kvp.getValue());
                        subPoints = new float[] { kvp.getValue() };
                        comparison.setSubPoints(subPoints);

                        comparisonList.add(comparison);
                    }
                }
                return comparisonList.toArray(new ComparisonCalculationBase[0]);
            default:
                return new ComparisonCalculationBase[0];
        }
    }

    private static Stats statSpellPower(float value) {
        Stats s = new Stats();
        s.setSpellPower(value);
        return s;
    }

    private static Stats statMp5(float value) {
        Stats s = new Stats();
        s.setMp5(value);
        return s;
    }

    private static Stats statCritRating(float value) {
        Stats s = new Stats();
        s.setCritRating(value);
        return s;
    }

    private static Stats statHasteRating(float value) {
        Stats s = new Stats();
        s.setHasteRating(value);
        return s;
    }

    private static Stats statHitRating(float value) {
        Stats s = new Stats();
        s.setHitRating(value);
        return s;
    }

    private static Stats statIntellect(float value) {
        Stats s = new Stats();
        s.setIntellect(value);
        return s;
    }

    private static Stats statSpirit(float value) {
        Stats s = new Stats();
        s.setSpirit(value);
        return s;
    }

    private ComparisonCalculationBase[] evaluateItemBudget(Character character, Stats[] statsList) {
        Stats.StatProperty[] properties = new Stats.StatProperty[statsList.length];
        for (int index = 0; index < statsList.length; index++) {
            for (Stats.StatProperty p : Stats.PROPERTY_INFO) {
                if ( p.accessor().get(statsList[index]) > 0) {
                    properties[index] = p;
                }
            }
        }
        Item item = new Item();
        item.setStats(new Stats());
        ComparisonCalculationBase[] result = new ComparisonCalculationBase[statsList.length];
        for (int index = 0; index < statsList.length; index++) {
            result[index] = CalculationsBase.getRelativeStatValue(character, properties[index], item, properties[index].accessor().get(statsList[index]));
        }

        return result;
    }

    public static String timeFormat(double time) {
        java.time.Duration span = java.time.Duration.ofNanos((long) (round(time, 2) / 0.0000001) * 100L);
        return String.format(Locale.ROOT, "%d:%02d", span.toMinutesPart(), span.toSecondsPart());
    }

    private static double round(double value, int digits) {
        if (value == 0.0 || Double.isInfinite(value) || Double.isNaN(value)) return value;
        if (Math.abs(value) < 1E15) {
            double power = Math.pow(10, digits);
            double temp = value * power;
            if (temp >= 1E16) return value;
            temp = Math.rint(temp);
            return temp / power;
        }
        return value;
    }

    public String[] getOptimizableCalculationLabels() {
        String[] _optimizableCalculationLabels = new String[] {
                "Health",
                "Nature Resistance",
                "Fire Resistance",
                "Frost Resistance",
                "Shadow Resistance",
                "Arcane Resistance",
                "Resilience",
                "Chance to Live",
                "Hit Rating",
                "Haste Rating",
                "PVP Trinket",
                "Movement Speed",
        };
        return _optimizableCalculationLabels;
    }

    public Stats getRelevantStats(Stats stats) {
        Stats s = new Stats();
        s.setArcaneResistance(stats.getArcaneResistance());
        s.setFireResistance(stats.getFireResistance());
        s.setFrostResistance(stats.getFrostResistance());
        s.setNatureResistance(stats.getNatureResistance());
        s.setShadowResistance(stats.getShadowResistance());
        s.setStamina(stats.getStamina());
        s.setIntellect(stats.getIntellect());
        s.setSpirit(stats.getSpirit());
        s.setDefenseRating(stats.getDefenseRating());
        s.setDodgeRating(stats.getDodgeRating());
        s.setHealth(stats.getHealth());
        s.setMp5(stats.getMp5());
        s.setResilience(stats.getResilience());
        s.setCritRating(stats.getCritRating());
        s.setSpellPower(stats.getSpellPower());
        s.setSpellFireDamageRating(stats.getSpellFireDamageRating());
        s.setHasteRating(stats.getHasteRating());
        s.setHitRating(stats.getHitRating());
        s.setBonusIntellectMultiplier(stats.getBonusIntellectMultiplier());
        s.setBonusSpellCritMultiplier(stats.getBonusSpellCritMultiplier());
        s.setBonusStaminaMultiplier(stats.getBonusStaminaMultiplier());
        s.setBonusSpiritMultiplier(stats.getBonusSpiritMultiplier());
        s.setMana(stats.getMana());
        s.setSpellArcaneDamageRating(stats.getSpellArcaneDamageRating());
        s.setSpellPenetration(stats.getSpellPenetration());
        s.setSpellFrostDamageRating(stats.getSpellFrostDamageRating());
        s.setArmor(stats.getArmor());
        s.setHp5(stats.getHp5());
        s.setSpellCombatManaRegeneration(stats.getSpellCombatManaRegeneration());
        s.setBonusArcaneDamageMultiplier(stats.getBonusArcaneDamageMultiplier());
        s.setBonusFireDamageMultiplier(stats.getBonusFireDamageMultiplier());
        s.setBonusFrostDamageMultiplier(stats.getBonusFrostDamageMultiplier());
        s.setManaRestoreFromBaseManaPPM(stats.getManaRestoreFromBaseManaPPM());
        s.setBonusManaGem(stats.getBonusManaGem());
        s.setBonusManaPotion(stats.getBonusManaPotion());
        s.setThreatIncreaseMultiplier(stats.getThreatIncreaseMultiplier());
        s.setThreatReductionMultiplier(stats.getThreatReductionMultiplier());
        s.setInterruptProtection(stats.getInterruptProtection());
        s.setArcaneResistanceBuff(stats.getArcaneResistanceBuff());
        s.setFireResistanceBuff(stats.getFireResistanceBuff());
        s.setFrostResistanceBuff(stats.getFrostResistanceBuff());
        s.setShadowResistanceBuff(stats.getShadowResistanceBuff());
        s.setNatureResistanceBuff(stats.getNatureResistanceBuff());
        s.setPVPTrinket(stats.getPVPTrinket());
        s.setMovementSpeed(stats.getMovementSpeed());
        s.setMageIceArmor(stats.getMageIceArmor());
        s.setMageMageArmor(stats.getMageMageArmor());
        s.setMageMoltenArmor(stats.getMageMoltenArmor());
        s.setManaRestoreFromMaxManaPerSecond(stats.getManaRestoreFromMaxManaPerSecond());
        s.setSpellHit(stats.getSpellHit());
        s.setSpellCrit(stats.getSpellCrit());
        s.setSpellCritOnTarget(stats.getSpellCritOnTarget());
        s.setSpellHaste(stats.getSpellHaste());
        s.setCritBonusDamage(stats.getCritBonusDamage());
        s.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());
        s.setBonusSpellPowerDemonicPactMultiplier(stats.getBonusSpellPowerDemonicPactMultiplier());
        s.setSpellsManaReduction(stats.getSpellsManaReduction());
        s.setMage4T8(stats.getMage4T8());
        s.setMage2T9(stats.getMage2T9());
        s.setMage4T9(stats.getMage4T9());
        s.setMage2T10(stats.getMage2T10());
        s.setMage4T10(stats.getMage4T10());
        s.setBonusHealthMultiplier(stats.getBonusHealthMultiplier());
        s.setMasteryRating(stats.getMasteryRating());
        for (SpecialEffect effect : stats.specialEffects()) {
            if (effect.getMaxStack() == 1) {
                if (effect.getStats().getSpellPower() > 0) {
                    if (effect.getTrigger() == Trigger.Use || effect.getTrigger() == Trigger.DamageSpellCrit || effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellHit || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.DamageSpellCast || effect.getTrigger() == Trigger.SpellMiss || effect.getTrigger() == Trigger.MageNukeCast || effect.getTrigger() == Trigger.DamageDone || effect.getTrigger() == Trigger.DoTTick || effect.getTrigger() == Trigger.DamageOrHealingDone) {
                        s.addSpecialEffect(effect);
                        continue;
                    }
                }
                if (effect.getStats().getArcaneDamage() + effect.getStats().getFireDamage() + effect.getStats().getFrostDamage() + effect.getStats().getNatureDamage() + effect.getStats().getShadowDamage() + effect.getStats().getHolyDamage() + effect.getStats().getValkyrDamage() > 0) {
                    if (effect.getTrigger() == Trigger.DamageSpellCrit || effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellHit || effect.getTrigger() == Trigger.DamageDone || effect.getTrigger() == Trigger.DoTTick || effect.getTrigger() == Trigger.DamageOrHealingDone || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.DamageSpellCast) {
                        s.addSpecialEffect(effect);
                        continue;
                    }
                }
                if (effect.getStats().getHasteRating() > 0) {
                    if (effect.getTrigger() == Trigger.Use) {
                        s.addSpecialEffect(effect);
                        continue;
                    }
                    if (effect.getCooldown() >= effect.getDuration() && (effect.getTrigger() == Trigger.DamageSpellCrit || effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellHit || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.DamageSpellCast)) {
                        s.addSpecialEffect(effect);
                        continue;
                    }
                    if (effect.getCooldown() == 0 && (effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.DamageSpellCrit)) {
                        s.addSpecialEffect(effect);
                        continue;
                    }
                }
                if (effect.getStats().getManaRestore() > 0 || effect.getStats().getMp5() > 0) {
                    if (effect.getTrigger() == Trigger.Use || effect.getTrigger() == Trigger.DamageSpellCast || effect.getTrigger() == Trigger.DamageSpellCrit || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.SpellHit || effect.getTrigger() == Trigger.DamageDone || effect.getTrigger() == Trigger.DoTTick || effect.getTrigger() == Trigger.DamageOrHealingDone) {
                        s.addSpecialEffect(effect);
                        continue;
                    }
                }
                if (effect.getTrigger() == Trigger.ManaGem) {
                    s.addSpecialEffect(effect);
                    continue;
                }
                if (effect.getTrigger() == Trigger.Use) {
                    // check if it is a stacking use effect
                    boolean hasStackingEffect = false;
                    for (SpecialEffect e : effect.getStats().specialEffects()) {
                        if (e.getChance() == 1f && e.getCooldown() == 0f && (e.getTrigger() == Trigger.DamageSpellCast || e.getTrigger() == Trigger.DamageSpellHit || e.getTrigger() == Trigger.SpellCast || e.getTrigger() == Trigger.SpellHit)) {
                            if (e.getStats().getHasteRating() > 0) {
                                hasStackingEffect = true;
                                break;
                            }
                        }
                        if (e.getChance() == 1f && e.getCooldown() == 0f && (e.getTrigger() == Trigger.DamageSpellCrit || e.getTrigger() == Trigger.SpellCrit)) {
                            if (e.getStats().getCritRating() < 0 && effect.getStats().getCritRating() > 0) {
                                hasStackingEffect = true;
                                break;
                            }
                        }
                    }
                    if (hasStackingEffect) {
                        s.addSpecialEffect(effect);
                        continue;
                    }
                }
            } else if (effect.getChance() == 1f && effect.getCooldown() == 0f && (effect.getTrigger() == Trigger.DamageSpellCast || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.SpellHit)) {
                if (hasEffectStats(effect.getStats())) {
                    s.addSpecialEffect(effect);
                    continue;
                }
            }
        }
        return s;
    }

    private boolean hasEffectStats(Stats stats) {
        float commonStats = stats.getCritRating() + stats.getHasteRating() + stats.getHitRating();
        return hasMageStats(stats) || (commonStats > 0);
    }

    private boolean hasMageStats(Stats stats) {
        float mageStats = stats.getIntellect() + stats.getSpirit() + stats.getMp5() + stats.getSpellPower() + stats.getSpellFireDamageRating() + stats.getBonusIntellectMultiplier() + stats.getBonusSpellCritMultiplier() + stats.getBonusSpiritMultiplier() + stats.getSpellFrostDamageRating() + stats.getSpellArcaneDamageRating() + stats.getSpellPenetration() + stats.getMana() + stats.getSpellCombatManaRegeneration() + stats.getBonusArcaneDamageMultiplier() + stats.getBonusFireDamageMultiplier() + stats.getBonusFrostDamageMultiplier() + /*stats.EvocationExtension + stats.BonusMageNukeMultiplier + stats.LightningCapacitorProc + */stats.getManaRestoreFromBaseManaPPM() + stats.getBonusManaGem() + stats.getBonusManaPotion() + stats.getThreatReductionMultiplier() + stats.getArcaneResistance() + stats.getFireResistance() + stats.getFrostResistance() + stats.getNatureResistance() + stats.getShadowResistance() + stats.getInterruptProtection() + stats.getArcaneResistanceBuff() + stats.getFrostResistanceBuff() + stats.getFireResistanceBuff() + stats.getNatureResistanceBuff() + stats.getShadowResistanceBuff() + stats.getMageIceArmor() + stats.getMageMageArmor() + stats.getMageMoltenArmor() + stats.getManaRestoreFromMaxManaPerSecond() + stats.getSpellCrit() + stats.getSpellCritOnTarget() + stats.getSpellHit() + stats.getSpellHaste() + /*stats.PendulumOfTelluricCurrentsProc + stats.ThunderCapacitorProc + */stats.getCritBonusDamage() + stats.getBonusDamageMultiplier() + stats.getBonusSpellPowerDemonicPactMultiplier() + stats.getSpellsManaReduction() + stats.getMage4T8() + stats.getMage2T9() + stats.getMage4T9() + stats.getMage2T10() + stats.getMage4T10();
        return mageStats > 0;
    }

    public boolean hasRelevantStats(Stats stats) {
        boolean mageStats = hasMageStats(stats);
        float commonStats = stats.getCritRating() + stats.getHasteRating() + stats.getHitRating() + stats.getHealth() + stats.getStamina() + stats.getArmor() + stats.getPVPTrinket() + stats.getMovementSpeed() + stats.getResilience() + stats.getBonusHealthMultiplier() + stats.getMasteryRating();
        float ignoreStats = stats.getAgility() + stats.getStrength() + stats.getAttackPower() + +stats.getDefenseRating() + stats.getDefense() + stats.getDodge() + stats.getParry() + stats.getDodgeRating() + stats.getParryRating() + stats.getExpertiseRating() + stats.getBlock() + stats.getBlockRating() + stats.getBlockValue() + stats.getSpellShadowDamageRating() + stats.getSpellNatureDamageRating() + stats.getArmorPenetration() + stats.getArmorPenetrationRating();
        for (SpecialEffect effect : stats.specialEffects()) {
            if (effect.getMaxStack() == 1) {
                if (effect.getStats().getSpellPower() > 0) {
                    if (effect.getTrigger() == Trigger.Use || effect.getTrigger() == Trigger.DamageSpellCrit || effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellHit || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.DamageSpellCast || effect.getTrigger() == Trigger.SpellMiss || effect.getTrigger() == Trigger.MageNukeCast || effect.getTrigger() == Trigger.DamageDone || effect.getTrigger() == Trigger.DoTTick || effect.getTrigger() == Trigger.DamageOrHealingDone) {
                        return true;
                    }
                }
                if (effect.getStats().getArcaneDamage() + effect.getStats().getFireDamage() + effect.getStats().getFrostDamage() + effect.getStats().getNatureDamage() + effect.getStats().getShadowDamage() + effect.getStats().getHolyDamage() + effect.getStats().getValkyrDamage() > 0) {
                    if (effect.getTrigger() == Trigger.DamageSpellCrit || effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellHit || effect.getTrigger() == Trigger.DamageDone || effect.getTrigger() == Trigger.DoTTick || effect.getTrigger() == Trigger.DamageOrHealingDone || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.DamageSpellCast) {
                        return true;
                    }
                }
                if (effect.getStats().getHasteRating() > 0) {
                    if (effect.getTrigger() == Trigger.Use) {
                        return true;
                    }
                    if (effect.getCooldown() >= effect.getDuration() && (effect.getTrigger() == Trigger.DamageSpellCrit || effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellHit || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.DamageSpellCast)) {
                        return true;
                    }
                    if (effect.getCooldown() == 0 && (effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.DamageSpellCrit)) {
                        return true;
                    }
                }
                if (effect.getStats().getManaRestore() > 0 || effect.getStats().getMp5() > 0) {
                    if (effect.getTrigger() == Trigger.Use || effect.getTrigger() == Trigger.DamageSpellCast || effect.getTrigger() == Trigger.DamageSpellCrit || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.SpellHit || effect.getTrigger() == Trigger.DamageDone || effect.getTrigger() == Trigger.DoTTick || effect.getTrigger() == Trigger.DamageOrHealingDone) {
                        return true;
                    }
                }
                if (effect.getTrigger() == Trigger.ManaGem) {
                    return true;
                }
                if (effect.getTrigger() == Trigger.Use) {
                    // check if it is a stacking use effect
                    boolean hasStackingEffect = false;
                    for (SpecialEffect e : effect.getStats().specialEffects()) {
                        if (e.getChance() == 1f && e.getCooldown() == 0f && (e.getTrigger() == Trigger.DamageSpellCast || e.getTrigger() == Trigger.DamageSpellHit || e.getTrigger() == Trigger.SpellCast || e.getTrigger() == Trigger.SpellHit)) {
                            if (e.getStats().getHasteRating() > 0) {
                                hasStackingEffect = true;
                                break;
                            }
                        }
                        if (e.getChance() == 1f && e.getCooldown() == 0f && (e.getTrigger() == Trigger.DamageSpellCrit || e.getTrigger() == Trigger.SpellCrit)) {
                            if (e.getStats().getCritRating() < 0 && effect.getStats().getCritRating() > 0) {
                                hasStackingEffect = true;
                                break;
                            }
                        }
                    }
                    if (hasStackingEffect) {
                        return true;
                    }
                }
            } else if (effect.getChance() == 1f && effect.getCooldown() == 0f && (effect.getTrigger() == Trigger.DamageSpellCast || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.SpellHit)) {
                if (hasEffectStats(effect.getStats())) {
                    return true;
                }
            }
            ignoreStats += effect.getStats().getAgility() + effect.getStats().getStrength() + effect.getStats().getAttackPower() + effect.getStats().getDefenseRating() + effect.getStats().getDefense() + effect.getStats().getDodge() + effect.getStats().getParry() + effect.getStats().getDodgeRating() + effect.getStats().getParryRating() + effect.getStats().getExpertiseRating() + effect.getStats().getBlock() + effect.getStats().getBlockRating() + effect.getStats().getBlockValue() + effect.getStats().getSpellShadowDamageRating() + effect.getStats().getSpellNatureDamageRating() + effect.getStats().getArmorPenetration() + effect.getStats().getArmorPenetrationRating();
        }
        return (mageStats || (commonStats > 0 && ignoreStats == 0.0f));
    }

    @Override
    public boolean enchantFitsInSlot(Enchant enchant, Character character, ItemSlot slot) {
        if (slot == ItemSlot.OffHand || slot == ItemSlot.Ranged) return false;
        return super.enchantFitsInSlot(enchant, character, slot);
    }

    @Override
    public boolean itemFitsInSlot(Item item, Character character, rawr.base.character.CharacterSlot slot, boolean ignoreUnique) {
        if (slot == rawr.base.character.CharacterSlot.OffHand && item.getSlot() == ItemSlot.OneHand) return false;
        return super.itemFitsInSlot(item, character, slot, ignoreUnique);
    }

    public List<ItemType> getRelevantItemTypes() {
        List<ItemType> _relevantItemTypes = new ArrayList<ItemType>(Arrays.asList(
                ItemType.None,
                ItemType.Cloth,
                ItemType.Dagger,
                ItemType.OneHandSword,
                ItemType.Staff,
                ItemType.Wand));
        return _relevantItemTypes;
    }

    public void setDefaults(Character character) {
        character.activeBuffsAdd(("Sanctified Retribution"));
        character.activeBuffsAdd(("Swift Retribution"));
        character.activeBuffsAdd(("Arcane Intellect"));
        character.activeBuffsAdd(("Judgements of the Wise"));
        character.activeBuffsAdd(("Blessing of Wisdom"));
        character.activeBuffsAdd(("Improved Blessing of Wisdom"));
        character.activeBuffsAdd(("Elemental Oath"));
        if (character.getMageTalents().getFocusMagic() == 1) character.activeBuffsAdd(("Focus Magic"));
        character.activeBuffsAdd(("Wrath of Air Totem"));
        character.activeBuffsAdd(("Totem of Wrath (Spell Power)"));
        character.activeBuffsAdd(("Divine Spirit"));
        character.activeBuffsAdd(("Power Word: Fortitude"));
        character.activeBuffsAdd(("Improved Power Word: Fortitude"));
        character.activeBuffsAdd(("Mark of the Wild"));
        character.activeBuffsAdd(("Improved Mark of the Wild"));
        character.activeBuffsAdd(("Blessing of Kings"));
        character.activeBuffsAdd(("Concentration Aura"));
        character.activeBuffsAdd(("Improved Concentration Aura"));
        character.activeBuffsAdd(("Heart of the Crusader"));
        character.activeBuffsAdd(("Judgement of Wisdom"));
        character.activeBuffsAdd(("Improved Scorch"));
        character.activeBuffsAdd(("Ebon Plaguebringer"));
        character.activeBuffsAdd(("Misery"));
        character.activeBuffsAdd(("Flask of the Frost Wyrm"));
        character.activeBuffsAdd(("Fish Feast"));
        character.activeBuffsAdd("Molten Armor");
    }

    @Override
    public String getCharacterStatsString(Character character) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Character:\t\t%s@%s-%s\r\nRace:\t\t%s",
                character.getName(), character.getRegion(), character.getRealm(), character.getRace()));

        CalculationOptionsMage CalculationOptions = (CalculationOptionsMage) character.getCalculationOptions();

        if (CalculationOptions.getCalculations() == null || CalculationOptions.getCalculations().getDisplayCalculationValues() == null) {
            return super.getCharacterStatsString(character);
        }

        Map<String, String> dict = CalculationOptions.getCalculations().getDisplayCalculationValues();
        for (Map.Entry<String, String> kvp : dict.entrySet()) {
            String[] value = kvp.getValue().split("\\*", -1);
            if (value.length == 2) {
                sb.append(String.format(Locale.ROOT, "\r\n%s: %s\r\n%s\r\n", kvp.getKey(), value[0], value[1]));
            } else {
                sb.append(String.format(Locale.ROOT, "\r\n%s: %s", kvp.getKey(), value[0]));
            }
        }

        return sb.toString();
    }

    private static Solver advancedSolver;
    private static StringBuilder advancedSolverLog = new StringBuilder();

    public static String getAdvancedSolverLog() {
        return advancedSolverLog.toString();
    }

    public static void cancelAsync() {
        Solver solver = advancedSolver;
        if (solver != null) {
            solver.cancelAsync();
        }
    }

    public static void log(Solver solver, String line) {
        if (solver == advancedSolver) {
            advancedSolverLog.append(line).append(System.lineSeparator());
        }
    }

    public static boolean isSolverEnabled(Solver solver) {
        return solver == advancedSolver;
    }

    public static void enableSolver(Solver solver) {
        advancedSolver = solver;
        advancedSolverLog.setLength(0);
    }

    public static void disableSolver(Solver solver) {
        synchronized (CalculationsMage.class) {
            if (advancedSolver == solver) {
                advancedSolver = null;
            }
        }
    }
}
