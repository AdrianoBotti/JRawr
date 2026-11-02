// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/CalculationsDPSDK.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the UI-only CalculationOptionsPanel and the RAWR3/Silverlight color branch are omitted;
// the non-Silverlight display label/color/optimizable/chart/gemming tables are ported additively as
// getCharacterDisplayCalculationLabels()/getSubPointNameColors()/getOptimizableCalculationLabels()
// /getCustomChartNames()/getCustomChartData(). DeserializeDataObject uses a JDK-XML reflective
// reader instead of XmlSerializer (same approach as CalculationsTankDK); the serialized `rotation`
// subtree is read into a Rotation and copied through the C# member-copying setter. The dead
// RotationGene class (genetic rotation search, never instantiated) and its `hawut` static field
// are omitted.
package rawr.models.dpsdk;

import rawr.base.stats.BaseStats;
import rawr.base.character.Buff;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.CharacterClass;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.items.Enchant;
import rawr.base.items.GemmingTemplate;
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
import java.util.Map;

public class CalculationsDPSDK extends CalculationsBase {
    private Rotation.Type rTypeOld = Rotation.Type.Unknown;

    public static CalculationsDPSDK instance;

    public static CalculationsDPSDK getInstance() {
        return instance;
    }

    public CalculationsDPSDK() {
        instance = this;
    }

    static {
        Calculations.registerModel("DPSDK", CalculationsDPSDK.class);
    }

    public static void register() {
        Calculations.registerModel("DPSDK", CalculationsDPSDK.class);
    }

    @Override
    public String getOverallRatingLabel() {
        return "Overall Points";
    }

    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        // Relevant Gem IDs for DPSDKs

        // Red
        int[] bold = { 39900, 39996, 40111, 42142 };
        int[] fractured = { 39909, 40002, 40117, 42153 };

        // Purple
        int[] sovereign = { 39934, 40022, 40129 };

        // Orange
        int[] inscribed = { 39947, 40037, 40142 };

        // Prismatic
        int[] tear = { 42702, 42702, 49110 };

        // Meta
        int chaotic = 41285;
        int relentless = 41398;

        List<GemmingTemplate> templates = new ArrayList<GemmingTemplate>();
        templates.add(gemmingTemplate("Rare", false, bold[1], bold[1], bold[1], bold[1], chaotic));
        templates.add(gemmingTemplate("Rare", false, fractured[1], fractured[1], fractured[1], fractured[1], chaotic));
        templates.add(gemmingTemplate("Rare", false, bold[1], inscribed[1], sovereign[1], bold[1], chaotic));
        templates.add(gemmingTemplate("Rare", false, bold[1], bold[1], bold[1], tear[1], relentless));

        templates.add(gemmingTemplate("Epic", true, bold[2], bold[2], bold[2], bold[2], chaotic));
        templates.add(gemmingTemplate("Rare", false, fractured[2], fractured[2], fractured[2], fractured[2], chaotic));
        templates.add(gemmingTemplate("Epic", true, bold[2], inscribed[2], sovereign[2], bold[2], chaotic));
        templates.add(gemmingTemplate("Epic", true, bold[2], inscribed[2], sovereign[2], tear[2], relentless));

        templates.add(gemmingTemplate("Jeweler", false, bold[3], bold[3], bold[3], bold[3], chaotic));
        templates.add(gemmingTemplate("Rare", false, fractured[3], fractured[3], fractured[3], fractured[3], chaotic));
        templates.add(gemmingTemplate("Jeweler", false, bold[2], bold[3], bold[3], bold[2], chaotic));
        return templates;
    }

    private static GemmingTemplate gemmingTemplate(String group, boolean enabled, int redId, int yellowId, int blueId, int prismaticId, int metaId) {
        GemmingTemplate template = new GemmingTemplate();
        template.setModel("DPSDK");
        template.setGroup(group);
        template.setEnabled(enabled);
        template.setRedId(redId);
        template.setYellowId(yellowId);
        template.setBlueId(blueId);
        template.setPrismaticId(prismaticId);
        template.setMetaId(metaId);
        return template;
    }

    public float BonusMaxRunicPower = 0f;

    public static float AddStatMultiplierStat(float statMultiplier, float newValue) {
        float updatedStatModifier = ((1 + statMultiplier) * (1 + newValue)) - 1f;
        return updatedStatModifier;
    }

    // SubPointColors
    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> colors = new LinkedHashMap<String, String>();
        colors.put("DPS", "#0000FF");
        return colors;
    }

    private static final String[] CharacterDisplayCalculationLabels = new String[] {
            "Basic Stats:Health",
            "Basic Stats:Strength",
            "Basic Stats:Agility",
            "Basic Stats:Attack Power",
            "Basic Stats:Crit Rating",
            "Basic Stats:Hit Rating",
            "Basic Stats:Expertise",
            "Basic Stats:Haste Rating",
            "Basic Stats:Armor Penetration Rating",
            "Basic Stats:Armor",
            "Advanced Stats:Weapon Damage*Damage before misses and mitigation",
            "Advanced Stats:Attack Speed",
            "Advanced Stats:Crit Chance",
            "Advanced Stats:Avoided Attacks",
            "Advanced Stats:Enemy Mitigation",
            "DPS Breakdown:White",
            "DPS Breakdown:BCB*Blood Caked Blade",
            "DPS Breakdown:Necrosis",
            "DPS Breakdown:Death Coil",
            "DPS Breakdown:Icy Touch",
            "DPS Breakdown:Plague Strike",
            "DPS Breakdown:Frost Fever",
            "DPS Breakdown:Blood Plague",
            "DPS Breakdown:Scourge Strike",
            "DPS Breakdown:Unholy Blight",
            "DPS Breakdown:Frost Strike",
            "DPS Breakdown:Howling Blast",
            "DPS Breakdown:Obliterate",
            "DPS Breakdown:Death Strike",
            "DPS Breakdown:Blood Strike",
            "DPS Breakdown:Heart Strike",
            "DPS Breakdown:DRW*Dancing Rune Weapon",
            "DPS Breakdown:Gargoyle",
            "DPS Breakdown:Wandering Plague",
            "DPS Breakdown:Ghoul",
            "DPS Breakdown:Bloodworms",
            "DPS Breakdown:Other",
            "DPS Breakdown:Total DPS",
    };

    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        return java.util.Arrays.asList(CharacterDisplayCalculationLabels);
    }

    private static final String[] CustomChartNames = new String[] {
            "Item Budget (10 point steps)",
            "Item Budget (25 point steps)",
            "Item Budget (50 point steps)",
            "Item Budget (100 point steps)",
            "Presences"
    };

    public String[] getCustomChartNames() {
        return CustomChartNames;
    }

    private static final List<ItemType> RelevantItemTypes = List.of(
            ItemType.None,
            ItemType.Leather,
            ItemType.Mail,
            ItemType.Plate,
            ItemType.Sigil,
            ItemType.Polearm,
            ItemType.TwoHandAxe,
            ItemType.TwoHandMace,
            ItemType.TwoHandSword,
            ItemType.OneHandAxe,
            ItemType.OneHandMace,
            ItemType.OneHandSword);

    @Override
    public List<ItemType> getRelevantItemTypes() {
        return RelevantItemTypes;
    }

    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.DeathKnight;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationDPSDK();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsDPSDK();
    }

    @Override
    public rawr.base.calculations.ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            CalculationOptionsDPSDK calcOpts = new CalculationOptionsDPSDK();
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
            if (setter != null) {
                Class<?> type = setter.getParameterTypes()[0];
                Object value = readXmlValue(child, type);
                if (value == null) continue;
                try {
                    setter.invoke(target, value);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            } else {
                java.lang.reflect.Field field = findField(target.getClass(), name);
                if (field == null) continue;
                Object value = readXmlValue(child, field.getType());
                if (value == null) continue;
                try {
                    field.set(target, value);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static java.lang.reflect.Method findSetter(Class<?> type, String name) {
        for (java.lang.reflect.Method method : type.getMethods()) {
            if (method.getParameterCount() == 1 && method.getName().equalsIgnoreCase(name)) {
                return method;
            }
        }
        return null;
    }

    private static java.lang.reflect.Field findField(Class<?> type, String name) {
        for (Class<?> t = type; t != null; t = t.getSuperclass()) {
            for (java.lang.reflect.Field field : t.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(name)) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        return null;
    }

    private static Object readXmlValue(org.w3c.dom.Element element, Class<?> type) {
        String text = element.getTextContent().trim();
        if (type == String.class) return text;
        if (type == int.class || type == Integer.class) return text.isEmpty() ? Integer.valueOf(0) : Integer.valueOf(Integer.parseInt(text));
        if (type == float.class || type == Float.class) return text.isEmpty() ? Float.valueOf(0f) : Float.valueOf(Float.parseFloat(text));
        if (type == double.class || type == Double.class) return text.isEmpty() ? Double.valueOf(0d) : Double.valueOf(Double.parseDouble(text));
        if (type == boolean.class || type == Boolean.class) return text.isEmpty() ? Boolean.FALSE : Boolean.valueOf(Boolean.parseBoolean(text));
        if (type.isEnum()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object enumValue = text.isEmpty() ? null : Enum.valueOf((Class<Enum>) type, text);
            return enumValue;
        }
        try {
            Object nested = type.getDeclaredConstructor().newInstance();
            readXmlObject(element, nested);
            return nested;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /**
     * GetCharacterCalculations is the primary method of each model.
     */
    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        CalculationOptionsDPSDK calcOpts = character.getCalculationOptions() instanceof CalculationOptionsDPSDK
                ? (CalculationOptionsDPSDK) character.getCalculationOptions()
                : null;

        int targetLevel = calcOpts.getTargetLevel();

        Stats stats = new Stats();

        CharacterCalculationsDPSDK calcs = new CharacterCalculationsDPSDK();
        stats = getCharacterStats(character, additionalItem);
        calcs.basicStats = stats;
        calcs.activeBuffs.clear();
        calcs.activeBuffs.addAll(character.getActiveBuffs());

        CombatTable combatTable = new CombatTable(character, calcs, stats, calcOpts);

        // DPS Subgroups
        float dpsWhite = 0f;
        float dpsBCB = 0f;
        float dpsDancingRuneWeapon = 0f;
        float dpsGargoyle = 0f;
        float dpsGhoul = 0f;
        float dpsOtherShadow = 0f;
        float dpsOtherArcane = 0f;
        float dpsOtherFrost = 0f;
        float dpsOtherFire = 0f;
        float dpsBloodworms = 0f;

        // shared variables
        DeathKnightTalents talents = character.getDeathKnightTalents();
        boolean DW = combatTable.DW;
        float missedSpecial = 0f;

        float dpsWhiteBeforeArmor = 0f;
        float dpsWhiteMinusGlancing = 0f;
        float fightDuration = calcOpts.getFightLength() * 60;
        float mitigation;
        float CinderglacierMultiplier = 1f;

        float MHExpertise = stats.getExpertise();
        float OHExpertise = stats.getExpertise();

        // damage multipliers
        float spellPowerMult = stats.getBonusSpellPowerMultiplier();
        float frostSpellPowerMult = stats.getBonusSpellPowerMultiplier() + Math.max((stats.getBonusFrostDamageMultiplier() - stats.getBonusShadowDamageMultiplier()), 0f);

        float physPowerMult = stats.getBonusPhysicalDamageMultiplier();
        // Covers all % physical damage increases.  Blood Frenzy, FI.
        float partialResist = 0.94f; // Average of 6% damage lost to partial resists on spells
        physPowerMult *= 1f + stats.getBonusDamageMultiplier();
        spellPowerMult *= 1f + stats.getBonusDamageMultiplier();
        frostSpellPowerMult *= 1f + stats.getBonusDamageMultiplier();

        // The Bonus Damage Multiplier needs to get applied to the stats object that gets passed into the Ability Handler.
        stats.setBonusPhysicalDamageMultiplier(physPowerMult);
        stats.setBonusSpellPowerMultiplier(spellPowerMult);
        stats.setBonusFrostDamageMultiplier(frostSpellPowerMult);

        // spell AP multipliers, for diseases its per tick
        float HowlingBlastAPMult = 0.2f;
        float IcyTouchAPMult = 0.1f;
        float FrostFeverAPMult = 0.055f;
        float BloodPlagueAPMult = 0.055f;
        float DeathCoilAPMult = 0.15f;
        float UnholyBlightAPMult = 0.013f;
        float GargoyleAPMult = 0.4f;   // pre 3.0.8 == 0.42f...now probably ~0.3f
        // TESTED on june 27th 2009 and found to be 0.40f
        float BloodwormsAPMult = 0.006f;

        // for estimating rotation pushback

        calcOpts.getRotation().setAvgDiseaseMult(calcOpts.getRotation().getNumDisease() * (calcOpts.getRotation().getDiseaseUptime() / 100));
        float commandMult = 0f;
        Rotation r = new Rotation();
        boolean bChangeRotation = false;
        Rotation.Type rType = r.getRotationType(talents);
        if (rType != this.rTypeOld) bChangeRotation = true;
        r.setRotation(rType);

        {
            float OHMult = 0.5f * (1f + (float) talents.getNervesOfColdSteel() * 0.083333333f);    // an OH multiplier that is useful sometimes

            // Impurity Application
            {
                float impurityMult = 1f + (.04f * (float) talents.getImpurity());

                HowlingBlastAPMult *= impurityMult;
                IcyTouchAPMult *= impurityMult;
                FrostFeverAPMult *= impurityMult;
                BloodPlagueAPMult *= impurityMult;
                DeathCoilAPMult *= impurityMult;
                UnholyBlightAPMult *= impurityMult;
                GargoyleAPMult *= impurityMult;
            }

            // racials
            {
                if (character.getRace() == rawr.base.character.CharacterRace.Orc) {
                    commandMult += .05f;
                }
            }

            // Mitigation
            {
                float targetArmor = calcOpts.getBossArmor();

                float arpenBuffs = talents.getBloodGorged() * 2f / 100;

                mitigation = 1f - StatConversion.getArmorDamageReduction(character.getLevel(), targetArmor,
                        stats.getArmorPenetration(), arpenBuffs, Math.max(0, stats.getArmorPenetrationRating()));

                calcs.enemyMitigation = 1f - mitigation;
                calcs.effectiveArmor = mitigation;
            }

            // White Dmg
            {
                float MHDPS = 0f, OHDPS = 0f;
                // Main Hand
                {
                    float dpsMHglancing = (StatConversion.WHITE_GLANCE_CHANCE_CAP[targetLevel - 80] * combatTable.MH.DPS) * 0.7f;
                    float dpsMHBeforeArmor = ((combatTable.MH.DPS * (1f - calcs.avoidedAttacks - StatConversion.WHITE_GLANCE_CHANCE_CAP[targetLevel - 80])) * (1f + combatTable.physCrits)) + dpsMHglancing;
                    dpsWhiteMinusGlancing = dpsMHBeforeArmor - dpsMHglancing;
                    dpsWhiteBeforeArmor = dpsMHBeforeArmor;
                    MHDPS = dpsMHBeforeArmor * mitigation;
                }

                // Off Hand
                if (DW || (character.getMainHand() == null && character.getOffHand() != null)) {
                    float dpsOHglancing = (StatConversion.WHITE_GLANCE_CHANCE_CAP[targetLevel - 80] * combatTable.OH.DPS) * 0.7f;
                    float dpsOHBeforeArmor = ((combatTable.OH.DPS * (1f - calcs.avoidedAttacks - StatConversion.WHITE_GLANCE_CHANCE_CAP[targetLevel - 80])) * (1f + combatTable.physCrits)) + dpsOHglancing;
                    dpsOHBeforeArmor *= OHMult;
                    dpsWhiteMinusGlancing += dpsOHBeforeArmor - dpsOHglancing;
                    dpsWhiteBeforeArmor += dpsOHBeforeArmor;
                    OHDPS = dpsOHBeforeArmor * mitigation;
                }

                dpsWhite = MHDPS + OHDPS;
            }

            AbilityHandler abilities = new AbilityHandler(stats, combatTable, character, calcOpts);

            if ((calcOpts.getGetRefreshForReferenceCalcs() && referenceCalculation)
                    || (calcOpts.getGetRefreshForDisplayCalcs() && needsDisplayCalculations)
                    || (calcOpts.getGetRefreshForSignificantChange() && significantChange)
                    || bChangeRotation) {
                // TODO: This gets very expensive.
                if (rType == Rotation.Type.Blood) {
                    BloodCycle cycle = new BloodCycle(character, combatTable, stats, calcOpts, abilities);
                    Rotation rot = cycle.GetDamage((int) (calcOpts.getFightLength() * 60 * 1000));
                    rot.setAvgDiseaseMult(2);
                    rot.setNumDisease(2);
                    rot.setCurRotationDuration(calcOpts.getFightLength() * 60);
                    calcOpts.getRotation().copyRotation(rot);
                    r.copyRotation(rot);
                } else if (rType == Rotation.Type.Unholy) {
                    UnholyCycle cycle = new UnholyCycle(character, combatTable, stats, calcOpts, abilities);
                    Rotation rot = cycle.GetDamage((int) (calcOpts.getFightLength() * 60 * 1000));
                    rot.setAvgDiseaseMult(3);
                    rot.setNumDisease(3);
                    rot.setCurRotationDuration(calcOpts.getFightLength() * 60);
                    calcOpts.getRotation().copyRotation(rot);
                    r.copyRotation(rot);
                } else if (rType == Rotation.Type.Frost) {
                    FrostCycle cycle = new FrostCycle(character, combatTable, stats, calcOpts, abilities);
                    Rotation rot = cycle.GetDamage((int) (calcOpts.getFightLength() * 60 * 1000));
                    rot.setAvgDiseaseMult(2);
                    rot.setNumDisease(2);
                    rot.setCurRotationDuration(calcOpts.getFightLength() * 60);
                    calcOpts.getRotation().copyRotation(rot);
                    r.copyRotation(rot);
                } else {
                    r.copyRotation(calcOpts.getRotation());
                }
            } else {
                r.copyRotation(calcOpts.getRotation());
            }

            this.rTypeOld = rType;

            // Blood Caked Blade
            {
                float dpsMHBCB = 0f;
                float dpsOHBCB = 0f;
                if ((combatTable.OH.damage != 0) && (DW || combatTable.MH.damage == 0)) {
                    float OHBCBDmg = (float) (combatTable.OH.damage * (.25f + .125f * calcOpts.getRotation().getAvgDiseaseMult()));
                    dpsOHBCB = OHBCBDmg / combatTable.OH.hastedSpeed;
                    dpsOHBCB *= OHMult;
                }
                if (combatTable.MH.damage != 0) {
                    float MHBCBDmg = (float) (combatTable.MH.damage * (.25f + .125f * calcOpts.getRotation().getAvgDiseaseMult()));
                    dpsMHBCB = MHBCBDmg / combatTable.MH.hastedSpeed;
                }
                dpsBCB = dpsMHBCB + dpsOHBCB;
                dpsBCB *= .1f * (float) talents.getBloodCakedBlade();
            }
            // Bloodworms
            {
                if (talents.getBloodworms() > 0) {
                    float BloodwormSwing = 50f + BloodwormsAPMult * stats.getAttackPower();
                    float BloodwormSwingDPS = BloodwormSwing / 2.0f;    // any haste benefits?
                    float TotalBloodworms = ((fightDuration / combatTable.MH.hastedSpeed) + calcOpts.getRotation().getMeleeSpecialsPerSecond() * fightDuration)
                            * (0.03f * talents.getBloodworms())
                            * 3f; // 3 bloodworms per proc
                    dpsBloodworms = ((TotalBloodworms * BloodwormSwingDPS * 20f) / fightDuration);
                }
            }

            // Trinket direct-damage procs, razorice damage, etc
            {
                dpsOtherArcane = stats.getArcaneDamage();
                dpsOtherShadow = stats.getShadowDamage();
                dpsOtherFire = stats.getFireDamage();
                dpsOtherFrost = stats.getFrostDamage();

                // TODO: Differentiate between MH razorice and OH razorice. Hardly matters though, since razorice hits only based off of base damage
                if (combatTable.MH.baseDamage != 0 && combatTable.MH.hastedSpeed != 0) {
                    dpsOtherFrost += stats.getBonusFrostWeaponDamage() * combatTable.MH.baseDamage / combatTable.MH.hastedSpeed;
                }
                if (combatTable.OH.baseDamage != 0 && combatTable.OH.hastedSpeed != 0) {
                    dpsOtherFrost += stats.getBonusFrostWeaponDamage() * combatTable.OH.baseDamage / combatTable.OH.hastedSpeed;
                }
                if (DW) dpsOtherFrost /= 2f; //razorice only actually effects the weapon its on, not both. this is closer than it would be otherwise.

                float OtherCritDmgMult = .5f * (1f + stats.getBonusCritMultiplier());
                float OtherCrit = 1f + ((combatTable.spellCrits) * OtherCritDmgMult);
                dpsOtherArcane *= OtherCrit;
                //dpsOtherShadow *= OtherCrit;
                dpsOtherFire *= OtherCrit;
                //dpsOtherFrost *= OtherCrit;
            }

            // Gargoyle
            {
                if (calcOpts.getRotation().getGargoyleDuration() > 0f && talents.getSummonGargoyle() > 0f) {
                    float GargoyleCastTime = 2.0f;  //2.0 second base cast time
                    GargoyleCastTime *= combatTable.MH.hastedSpeed / combatTable.MH.baseSpeed;
                    // benefits from all haste effects

                    float GargoyleStrike = 120f + GargoyleAPMult * stats.getAttackPower();
                    float GargoyleStrikeCount = 30f / GargoyleCastTime;
                    float GargoyleDmg = GargoyleStrike * GargoyleStrikeCount;
                    GargoyleDmg *= 1f + (.5f * .05f);  // crit rate is uninfluenced by stats, but roughly crap
                    dpsGargoyle = GargoyleDmg / 180f;
                }
            }

            // Ghoul
            {
                if (calcOpts.getGhoul()) {
                    float uptime = 1f;
                    if (talents.getMasterOfGhouls() == 0) {
                        float timeleft = calcOpts.getFightLength() * 60;
                        float numCDs = timeleft / ((4f - .75f * talents.getNightOfTheDead()) * 60f);
                        float duration = numCDs * 60f;

                        uptime = duration / timeleft;
                    }

                    float GhoulBaseStrength = 331f;
                    float GhoulBaseStrengthMult = 0.7f;
                    float GhoulBaseAP = 836f;
                    float GhoulBaseSpeed = 2f;
                    float GhoulAPMult = 1f;                 // 1 Str = x AP
                    float ClawCD = 4f;
                    float GhoulWeaponBaseDamage = 101.3f;   // I found these values to be fairly correct after a number of tests (v3.0.8)
                    float GhoulAPdivisor = 14f;           //

                    float GlyphofGhoulValue = 0f;
                    if (talents.isGlyphoftheGhoul()) GlyphofGhoulValue = 0.4f;

                    float GhoulStrengthMult = GhoulBaseStrengthMult + (GhoulBaseStrengthMult * (0.2f * (float) talents.getRavenousDead())) + GlyphofGhoulValue;
                    float GhoulStrength = GhoulBaseStrength + stats.getStrength() * GhoulStrengthMult;

                    Stats statsBuffs = getBuffsStats(character.getActiveBuffs());

                    GhoulStrength *= 1f + statsBuffs.getBonusStrengthMultiplier();
                    GhoulStrength += statsBuffs.getStrength();

                    if (character.getActiveBuffs().contains(Buff.getBuffByName("Flask of Chromatic Wonder"))) GhoulStrength -= 18f;
                    if (character.getActiveBuffs().contains(Buff.getBuffByName("Flask of Chromatic Wonder (Mixology)"))) GhoulStrength -= 4f;
                    if (character.getActiveBuffs().contains(Buff.getBuffByName("Elixir of Mighty Strength"))) GhoulStrength -= 50f;
                    if (character.getActiveBuffs().contains(Buff.getBuffByName("Elixir of Mighty Strength (Mixology)"))) GhoulStrength -= 16f;
                    if (character.getActiveBuffs().contains(Buff.getBuffByName("Guru's Elixir"))) GhoulStrength -= 20f;
                    if (character.getActiveBuffs().contains(Buff.getBuffByName("Guru's Elixir (Mixology)"))) GhoulStrength -= 6f;
                    if (character.getActiveBuffs().contains(Buff.getBuffByName("Scroll of Strength VIII"))) GhoulStrength -= 30f;

                    float GhoulAP = GhoulBaseAP + GhoulStrength * GhoulAPMult;

                    GhoulAP *= 1f + statsBuffs.getBonusAttackPowerMultiplier();
                    GhoulAP += statsBuffs.getAttackPower();

                    if (character.getActiveBuffs().contains(Buff.getBuffByName("Flask of Endless Rage"))) GhoulAP -= 180f;
                    if (character.getActiveBuffs().contains(Buff.getBuffByName("Flask of Endless Rage (Mixology)"))) GhoulAP -= 64f;
                    if (character.getActiveBuffs().contains(Buff.getBuffByName("Wrath Elixir"))) GhoulAP -= 90f;
                    if (character.getActiveBuffs().contains(Buff.getBuffByName("Wrath Elixir (Mixology)"))) GhoulAP -= 32f;
                    if (character.getActiveBuffs().contains(Buff.getBuffByName("Attack Power Food"))) GhoulAP -= 80f;
                    if (character.getActiveBuffs().contains(Buff.getBuffByName("Fish Feast"))) GhoulAP -= 80f;

                    float GhoulhastedSpeed = 0f;
                    if (combatTable.MH != null)
                        GhoulhastedSpeed = ((combatTable.MH.hastedSpeed == 0 ? 1.0f : combatTable.MH.hastedSpeed) / (combatTable.MH.baseSpeed == 0 ? 1.0f : combatTable.MH.baseSpeed)) * GhoulBaseSpeed;
                    else
                        GhoulhastedSpeed = GhoulBaseSpeed;

                    if (talents.getGhoulFrenzy() > 0f) {
                        float GhoulFrenzyHaste = (float) (0.25f * (calcOpts.getRotation().getGhoulFrenzy() / combatTable.realDuration) * 30f);
                        GhoulhastedSpeed /= 1f + GhoulFrenzyHaste;
                    }
                    GhoulhastedSpeed /= 1f + statsBuffs.getPhysicalHaste();

                    float dmgSwing = GhoulWeaponBaseDamage + (GhoulAP / GhoulAPdivisor) * GhoulBaseSpeed;
                    float dpsSwing = dmgSwing / GhoulhastedSpeed;
                    float dpsGhoulGlancing = (dpsSwing * 0.24f) * 0.75f;

                    float dpsClaw = (1.5f * dmgSwing) / ClawCD;

                    dpsSwing *= (1f - missedSpecial) - 0.24f;   // the Ghoul only has one weapon || Glancings added further down
                    dpsClaw *= 1f - missedSpecial;

                    dpsSwing *= 1f + .054f;// + stats.LotPCritRating / 4591f; // needs other crit modifiers, but doesn't inherit crit from master
                    dpsSwing += dpsGhoulGlancing;

                    dpsClaw *= 1f + .054f;// + stats.LotPCritRating / 4591f; // needs other crit modifiers, but doesn't inherit crit from master

                    dpsGhoul = dpsSwing + dpsClaw;

                    //dpsGhoul *= 1f + statsBuffs.BonusPhysicalDamageMultiplier;
                    // commented out because ghoul doesn't benefit from most bonus physical damage multipliers (ie blood presence, bloody vengeance, etc)
                    int targetArmor = calcOpts.getBossArmor();
                    float modArmor = 1f - StatConversion.getArmorDamageReduction(character.getLevel(), targetArmor,
                            0f/*stats.ArmorPenetration*/, 0f, 0f * stats.getArmorPenetrationRating());

                    dpsGhoul *= modArmor;
                    dpsGhoul *= 1f - (.0065f * missedSpecial);    // pet expertise now scales with player hit rating. Hopefully goes off of melee rather than spell hit.
                    dpsGhoul *= uptime;
                } else
                    dpsGhoul = 0;
            }

            float BCBMult = 1f;
            float DancingRuneWeaponMult = 1f;
            float GargoyleMult = 1f + commandMult;
            float GhoulMult = 1f + commandMult;
            float BloodwormsMult = 1f + commandMult;
            float WhiteMult = 1f;
            float otherShadowMult = 1f;
            float otherArcaneMult = 1f;
            float otherFrostMult = 1f;

            // Apply Physical Mitigation
            {
                float physMit = mitigation;
                physMit *= 1f + (!DW ? .02f * talents.getTwoHandedWeaponSpecialization() : 0f);

                dpsBCB *= physMit;
                dpsBloodworms *= 1f - StatConversion.getArmorDamageReduction(character.getLevel(), calcOpts.getBossArmor(), 0f/*stats.ArmorPenetration*/, 0f, 0f);

                WhiteMult *= physPowerMult * (1f + (!DW ? .02f * talents.getTwoHandedWeaponSpecialization() : 0f));
                BCBMult *= physPowerMult * (1f + (!DW ? .02f * talents.getTwoHandedWeaponSpecialization() : 0f));
            }

            // Apply Magical Mitigation
            {
                // some of this applies to necrosis, I wonder if it is ever accounted for
                float magicMit = partialResist; // combatTable.spellResist not applied

                otherShadowMult *= spellPowerMult;
                otherArcaneMult *= spellPowerMult;
                otherFrostMult *= frostSpellPowerMult;
            }

            // Cinderglacier multipliers
            {
                if (stats.getCinderglacierProc() > 0f && combatTable.MH != null) {
                    float shadowFrostAbilitiesPerSecond = (float) ((r.getDeathCoil()
                            + r.getFrostStrike() * (1 + talents.getThreatOfThassarian() / 3f) // FS hits once for each weapon.
                            + r.getScourgeStrike()
                            + r.getIcyTouch()
                            + r.getHowlingBlast())
                            / combatTable.realDuration);
                    float CGPercentChance = 1.5f / (60f / combatTable.MH.baseSpeed);
                    float CGPPM = ((60f / combatTable.MH.hastedSpeed) * CGPercentChance) * 1f - combatTable.totalMHMiss; // KM Procs per Minute (Defined "1 per point" by Blizzard) influenced by Phys. Haste
                    CGPPM += (1.5f / (60f / combatTable.MH.baseSpeed)) * ((combatTable.totalMeleeAbilities * (1f - combatTable.missedSpecial) + combatTable.totalSpellAbilities * (1f - combatTable.spellResist)) * 60f / combatTable.realDuration);
                    float ProcsPerAbility = CGPPM / (shadowFrostAbilitiesPerSecond * 60f);

                    CinderglacierMultiplier = 1f + 2f * 0.2f * (ProcsPerAbility);
                }

                abilities.DC.setDamageMod(abilities.DC.getDamageMod() * CinderglacierMultiplier);
                abilities.HB.setDamageMod(abilities.HB.getDamageMod() * CinderglacierMultiplier);
                abilities.IT.setDamageMod(abilities.IT.getDamageMod() * CinderglacierMultiplier);
                abilities.FS.setDamageMod(abilities.FS.getDamageMod() * CinderglacierMultiplier);
                abilities.SS.setDamageMod(abilities.SS.getDamageMod() * CinderglacierMultiplier);
            }

            // Apply Multi-Ability Talent Multipliers
            {
                float BloodyVengeanceMult = .03f * (float) talents.getBloodyVengeance();
                BCBMult *= 1 + BloodyVengeanceMult;
                WhiteMult *= 1 + BloodyVengeanceMult;

                float HysteriaCoeff = .2f / 6f; // current uptime is 16.666...%
                float HysteriaMult = HysteriaCoeff * (float) talents.getHysteria();
                BCBMult *= 1 + HysteriaMult;
                WhiteMult *= 1 + HysteriaMult;

                float BlackIceMult = .02f * (float) talents.getBlackIce();
                otherShadowMult *= 1 + BlackIceMult;
                otherFrostMult *= 1 + BlackIceMult;

                float DesecrationMult = .01f * (float) talents.getDesolation();    // the new desecration is basically a flat 1% per point
                BCBMult *= 1 + DesecrationMult;
                WhiteMult *= 1 + DesecrationMult;
                otherShadowMult *= 1 + DesecrationMult;
                otherArcaneMult *= 1 + DesecrationMult;
                otherFrostMult *= 1 + DesecrationMult;

                if ((float) talents.getBoneShield() >= 1f) {
                    float BoneMult = .02f;
                    BCBMult *= 1 + BoneMult;
                    WhiteMult *= 1 + BoneMult;
                    otherShadowMult *= 1 + BoneMult;
                    otherArcaneMult *= 1 + BoneMult;
                    otherFrostMult *= 1 + BoneMult;
                }
            }

            // Pet uptime modifiers
            {
                BloodwormsMult *= calcOpts.getBloodwormsUptime();
                GhoulMult *= calcOpts.getGhoulUptime();
            }

            //feed variables for output
            calcs.bcbDPS = dpsBCB * BCBMult;
            calcs.drwDPS = dpsDancingRuneWeapon * DancingRuneWeaponMult;
            calcs.gargoyleDPS = dpsGargoyle * GargoyleMult;
            calcs.ghoulDPS = dpsGhoul * GhoulMult;
            calcs.bloodwormsDPS = dpsBloodworms * BloodwormsMult;
            calcs.necrosisDPS = dpsWhite * (.04f * talents.getNecrosis());
            calcs.whiteDPS = dpsWhite * WhiteMult;
            calcs.otherDPS = dpsOtherShadow * otherShadowMult
                    + dpsOtherArcane * otherArcaneMult
                    + dpsOtherFire * otherArcaneMult
                    + dpsOtherFrost * otherFrostMult;

            // TODO: Re-work this to properly evaluate a base rotation and subrotations w/ special strikes.
            // First let's get the common stuff out of the ifs
            // Diseases & the abilities that cause them
            calcs.plagueStrikeDPS = (float) ((abilities.PS.getDamage() * r.getPlagueStrike()) / (calcOpts.getFightLength() * 60));
            calcs.bloodPlagueDPS = (float) ((abilities.BP.getDamage() * r.getBPTick()) / (calcOpts.getFightLength() * 60));
            calcs.icyTouchDPS = (float) ((abilities.IT.getDamage() * r.getIcyTouch()) / (calcOpts.getFightLength() * 60));
            calcs.frostFeverDPS = (float) ((abilities.FF.getDamage() * r.getFFTick()) / (calcOpts.getFightLength() * 60));
            calcs.bloodStrikeDPS = (float) ((abilities.BS.getDamage() * r.getBloodStrike()) / (calcOpts.getFightLength() * 60));
            // RP based
            calcs.deathCoilDPS = (float) ((abilities.DC.getDamage() * r.getDeathCoil()) / (calcOpts.getFightLength() * 60));

            if (rType == Rotation.Type.Frost) {
                calcs.bloodStrikeDPS = (float) ((abilities.BS.getDamage() * r.getBloodStrike()) / (calcOpts.getFightLength() * 60));
                if (talents.getFrostStrike() > 0)
                    calcs.frostStrikeDPS = (float) ((abilities.FS.getDamage() * r.getFrostStrike() + abilities.FS.getSecondaryDamage() * r.getKMFS()) / (calcOpts.getFightLength() * 60));
                else
                    calcs.deathCoilDPS = (float) ((abilities.DC.getDamage() * r.getDeathCoil()) / (calcOpts.getFightLength() * 60));

                if (talents.getHowlingBlast() > 0)
                    calcs.howlingBlastDPS = (float) ((abilities.HB.getDamage() * r.getHowlingBlast() + abilities.HB.getSecondaryDamage() * r.getKMRime()) / (calcOpts.getFightLength() * 60));
                calcs.obliterateDPS = (float) ((abilities.OB.getDamage() * r.getObliterate()) / (calcOpts.getFightLength() * 60));
            } else if (rType == Rotation.Type.Blood) {
                if (talents.getHeartStrike() > 0)
                    calcs.heartStrikeDPS = (float) ((abilities.HS.getDamage() * r.getHeartStrike()) / (calcOpts.getFightLength() * 60));
                else
                    calcs.bloodStrikeDPS = (float) ((abilities.BS.getDamage() * r.getBloodStrike()) / (calcOpts.getFightLength() * 60));
                calcs.deathStrikeDPS = (float) ((abilities.DS.getDamage() * r.getDeathStrike()) / (calcOpts.getFightLength() * 60));
                calcs.deathCoilDPS = (float) ((abilities.DC.getDamage() * r.getDeathCoil()) / (calcOpts.getFightLength() * 60));
            } else if (rType == Rotation.Type.Unholy) {
                if (talents.getScourgeStrike() > 0)
                    calcs.scourgeStrikeDPS = (float) (((abilities.SS.getDamage() + abilities.SS.getSecondaryDamage()) * r.getScourgeStrike()) / (calcOpts.getFightLength() * 60));
                else
                    calcs.obliterateDPS = (float) ((abilities.OB.getDamage() * r.getObliterate()) / (calcOpts.getFightLength() * 60));
                calcs.bloodStrikeDPS = (float) ((abilities.BS.getDamage() * r.getBloodStrike()) / (calcOpts.getFightLength() * 60));
                calcs.deathCoilDPS = (float) ((abilities.DC.getDamage() * r.getDeathCoil()) / (calcOpts.getFightLength() * 60));
                if (talents.getUnholyBlight() > 0) {
                    float modifier = (0.1f * (talents.isGlyphofUnholyBlight() ? 1.4f : 1));
                    calcs.unholyBlightDPS = calcs.deathCoilDPS * modifier;
                }
            }

            calcs.setDPSPoints(calcs.bcbDPS + calcs.bloodPlagueDPS + calcs.bloodStrikeDPS + calcs.deathCoilDPS + calcs.frostFeverDPS + calcs.frostStrikeDPS
                    + calcs.gargoyleDPS + calcs.ghoulDPS + calcs.wanderingPlagueDPS + calcs.heartStrikeDPS + calcs.howlingBlastDPS + calcs.icyTouchDPS
                    + calcs.necrosisDPS + calcs.obliterateDPS + calcs.deathStrikeDPS + calcs.plagueStrikeDPS + calcs.scourgeStrikeDPS + calcs.unholyBlightDPS
                    + calcs.whiteDPS + calcs.otherDPS + calcs.bloodwormsDPS);

            // Dancing Rune Weapon
            {
                if (talents.getDancingRuneWeapon() > 0) {
                    float dpsDRWMaximum = 0f;
                    String effectsStats = "";
                    Stats maxStats = GetCharacterStatsMaximum(character, additionalItem, 90f);
                    DRW drw;
                    Stats tempStats;

                    for (SpecialEffect effect : maxStats.specialEffects()) {
                        if (effect.getTrigger() != Trigger.Use) {
                            tempStats = new Stats();
                            tempStats = Stats.add(tempStats, maxStats);
                            tempStats = Stats.add(tempStats, effect.getStats());

                            tempStats.setStrength(tempStats.getStrength() + tempStats.getHighestStat() + tempStats.getParagon());

                            tempStats.setAgility((float) Math.floor(tempStats.getAgility() * (1 + tempStats.getBonusAgilityMultiplier())));
                            tempStats.setStrength((float) Math.floor(tempStats.getStrength() * (1 + tempStats.getBonusStrengthMultiplier())));
                            tempStats.setStamina((float) Math.floor(tempStats.getStamina() * (1 + tempStats.getBonusStaminaMultiplier())));
                            tempStats.setIntellect((float) Math.floor(tempStats.getIntellect() * (1 + tempStats.getBonusIntellectMultiplier())));
                            tempStats.setSpirit((float) Math.floor(tempStats.getSpirit() * (1 + tempStats.getBonusSpiritMultiplier())));
                            tempStats.setHealth((float) Math.floor(tempStats.getHealth() + (tempStats.getStamina() * 10f)));
                            tempStats.setMana((float) Math.floor(tempStats.getMana() + (tempStats.getIntellect() * 15f)));
                            tempStats.setAttackPower((float) Math.floor(tempStats.getAttackPower() + tempStats.getStrength() * 2));
                            // Copy from TankDK.
                            tempStats.setArmor((float) Math.floor(StatConversion.getArmorFromAgility(tempStats.getAgility())
                                    + StatConversion.applyMultiplier(tempStats.getArmor(), tempStats.getBaseArmorMultiplier())
                                    + StatConversion.applyMultiplier(tempStats.getBonusArmor(), tempStats.getBonusArmorMultiplier())));

                            tempStats.setAttackPower(tempStats.getAttackPower() + (tempStats.getArmor() / 180f) * (float) talents.getBladedArmor());

                            tempStats.setBonusSpellPowerMultiplier(tempStats.getBonusShadowDamageMultiplier());

                            tempStats.setAttackPower(tempStats.getAttackPower() * (1f + tempStats.getBonusAttackPowerMultiplier()));

                            drw = new DRW(combatTable, calcs, calcOpts, tempStats, character, talents);
                            if (drw.dpsDancingRuneWeapon > dpsDRWMaximum) {
                                dpsDRWMaximum = drw.dpsDancingRuneWeapon;
                                effectsStats = DPSDKFormat.specialEffectToString(effect);
                            }
                        }
                    }
                    dpsDancingRuneWeapon = dpsDRWMaximum;
                    float DRWUptime = (12f + (talents.isGlyphofDancingRuneWeapon() ? 5f : 0f)) / 90f;
                    dpsDancingRuneWeapon /= 90f;
                    calcs.setDPSPoints(calcs.getDPSPoints() + dpsDancingRuneWeapon);
                    calcs.drwDPS = dpsDancingRuneWeapon;
                    calcs.drwStats = effectsStats;
                }
            }

            calcs.setOverallPoints(calcs.getDPSPoints());
        }

        return calcs;
    }

    public CharacterCalculationsBase GetCharacterCalculationsExp(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        // Setup what we need.
        CalculationOptionsDPSDK calcOpts = character.getCalculationOptions() instanceof CalculationOptionsDPSDK
                ? (CalculationOptionsDPSDK) character.getCalculationOptions()
                : null;
        int targetLevel = calcOpts.getTargetLevel(); // This needs to get from BossHandler.

        Stats stats = new Stats();

        CharacterCalculationsDPSDK calcs = new CharacterCalculationsDPSDK();

        CombatTable combatTable = new CombatTable(character, calcs, stats, calcOpts);

        // Build up Character Stats to do math

        // Perform the calculations

        // Output the results.

        return calcs;
    }

    private Stats GetRaceStats(Character character) {
        return BaseStats.getBaseStats(character.getLevel(), CharacterClass.DeathKnight, character.getRace());
    }

    private Stats GetPresenceStats(CalculationOptionsDPSDK.Presence p) {
        Stats PresenceStats = new Stats();
        switch (p) {
            case Blood: {
                PresenceStats.setBonusPhysicalDamageMultiplier(.15f);
                PresenceStats.setBonusSpellPowerMultiplier(.15f);
                PresenceStats.setBonusShadowDamageMultiplier(.15f);
                PresenceStats.setBonusFrostDamageMultiplier(.15f);
                break;
            }
            case Unholy: {
                PresenceStats.setPhysicalHaste(0.15f);
                PresenceStats.setSpellHaste(0.15f);
                break;
            }
            case Frost: {
                PresenceStats.setBonusStaminaMultiplier(0.08f);
                PresenceStats.setBaseArmorMultiplier(.60f);
                PresenceStats.setDamageTakenMultiplier(-.08f);
                break;
            }
            default:
                break;
        }
        return PresenceStats;
    }

    private Stats getItemStats(Character character, Item additionalItem) {
        Stats stats = new Stats();
        accumulateItemStats(stats, character, additionalItem);
        return stats;
    }

    /**
     * GetCharacterStats is the 2nd-most calculation intensive method in a model.
     */
    public Stats getCharacterStats(Character character, Item additionalItem) {
        CalculationOptionsDPSDK calcOpts = character.getCalculationOptions() instanceof CalculationOptionsDPSDK
                ? (CalculationOptionsDPSDK) character.getCalculationOptions()
                : null;
        DeathKnightTalents talents = character.getDeathKnightTalents();

        Stats statsRace = GetRaceStats(character);
        Stats statsBaseGear = getItemStats(character, additionalItem);

        // Filter out the duplicate Runes:
        if (character.getOffHandEnchant() == Enchant.findEnchant(3368, ItemSlot.OneHand, character)
                && character.getMainHandEnchant() == character.getOffHandEnchant()) {
            boolean bFC1Found = false;
            boolean bFC2Found = false;
            for (SpecialEffect se1 : statsBaseGear.specialEffects()) {
                // if we've already found them, and we're seeing them again, then remove these repeats.
                if (bFC1Found && seFC1.getStats().structurallyEquals(se1.getStats()) && seFC1.getTrigger() == se1.getTrigger())
                    statsBaseGear.removeSpecialEffect(se1);
                else if (bFC2Found && seFC2.getStats().structurallyEquals(se1.getStats()) && seFC2.getTrigger() == se1.getTrigger())
                    statsBaseGear.removeSpecialEffect(se1);
                else if (seFC1.getStats().structurallyEquals(se1.getStats()) && seFC1.getTrigger() == se1.getTrigger())
                    bFC1Found = true;
                else if (seFC2.getStats().structurallyEquals(se1.getStats()) && seFC2.getTrigger() == se1.getTrigger())
                    bFC2Found = true;
            }
        }

        Stats statsBuffs = getBuffsStats(character.getActiveBuffs());
        Stats statsPresence = GetPresenceStats(calcOpts.getCurrentPresence());
        Stats statsTalents;
        if (calcOpts.getM_bExperimental()) {
            statsTalents = GetTalentStats(talents, character);
        } else {
            statsTalents = new Stats();
            statsTalents.setBonusStrengthMultiplier(.01f * (float) (talents.getAbominationsMight() + talents.getRavenousDead())
                    + .02f * (float) (talents.getVeteranOfTheThirdWar() + talents.getEndlessWinter()));
            statsTalents.setBaseArmorMultiplier(.03f * (float) (talents.getToughness()));
            statsTalents.setBonusStaminaMultiplier(.02f * (float) (talents.getVeteranOfTheThirdWar()));
            statsTalents.setExpertise((float) (talents.getTundraStalker() + talents.getRageOfRivendare()) + 2f * (float) (talents.getVeteranOfTheThirdWar()));
            statsTalents.setPhysicalHaste(0.04f * talents.getIcyTalons() + .05f * talents.getImprovedIcyTalons()
                    // IIT does not stack w/ Windfury Totem or Improved Icy Talons from the Buff pane.
                    + ((character.activeBuffsContains("Improved Icy Talons") || character.activeBuffsContains("Windfury Totem")) ? 0 : .2f));
            if (talents.getUnbreakableArmor() > 0) {
                Stats uaStats = new Stats();
                uaStats.setBonusStrengthMultiplier(0.2f);
                statsTalents.addSpecialEffect(new SpecialEffect(Trigger.Use, uaStats, 20f, 60f));
            }
        }

        Stats statsTotal = new Stats();
        statsTotal.accumulate(statsBaseGear);
        statsTotal.accumulate(statsBuffs);
        statsTotal.accumulate(statsRace);
        statsTotal.accumulate(statsPresence);
        statsTotal.accumulate(statsTalents);

        statsTotal = getRelevantStats(statsTotal);
        statsTotal.setExpertise(statsTotal.getExpertise() + (float) StatConversion.getExpertiseFromRating(statsTotal.getExpertiseRating()));

        StatsSpecialEffects se = new StatsSpecialEffects(character, statsTotal, new CombatTable(character, statsTotal, calcOpts));
        Stats statSE = new Stats();
        for (SpecialEffect e : statsTotal.specialEffects()) {
            // There are some multi-level special effects that need to be factored in.
            for (SpecialEffect ee : e.getStats().specialEffects()) {
                e.setStats(se.getSpecialEffects(calcOpts, ee));
            }
            statSE.accumulate(se.getSpecialEffects(calcOpts, e));
        }

        float tempCap = StatConversion.RATING_PER_ARMORPENETRATION * (1f - statsTotal.getArmorPenetration());

        for (SpecialEffect effect : statsTotal.specialEffects()) {
            if (hasRelevantStats(effect.getStats())) {
                // TODO: This code is very expensive for some reason.
                // Need to isolate and fix what's going on here to improve performance
                if (effect.getStats().getArmorPenetrationRating() > 0f
                        && ((effect.getStats().getArmorPenetrationRating() + statsTotal.getArmorPenetrationRating()) > tempCap)) {
                    SpecialEffect tempEffect = new SpecialEffect(effect.getTrigger(), effect.getStats().copy(), effect.getDuration(), effect.getCooldown(), effect.getChance(), effect.getMaxStack());
                    tempEffect.getStats().setArmorPenetrationRating(Math.max(tempCap - statsTotal.getArmorPenetrationRating(), 0f));
                    se = new StatsSpecialEffects(character, statsTotal, new CombatTable(character, statsTotal, calcOpts));
                    statsTotal.accumulate(se.getSpecialEffects(calcOpts, tempEffect));
                } else {
                    se = new StatsSpecialEffects(character, statsTotal, new CombatTable(character, statsTotal, calcOpts));
                    statsTotal.accumulate(se.getSpecialEffects(calcOpts, effect));
                }
            }
        }

        statsTotal.setStrength(statsTotal.getStrength() + statsTotal.getHighestStat() + statsTotal.getParagon() + statsTotal.getDeathbringerProc() / 3);
        statsTotal.setHasteRating(statsTotal.getHasteRating() + statsTotal.getDeathbringerProc() / 3);
        statsTotal.setCritRating(statsTotal.getCritRating() + statsTotal.getDeathbringerProc() / 3);

        statsTotal.setAgility((float) Math.floor(statsTotal.getAgility() * (1 + statsTotal.getBonusAgilityMultiplier())));
        statsTotal.setStrength((float) Math.floor(statsTotal.getStrength() * (1 + statsTotal.getBonusStrengthMultiplier())));
        statsTotal.setStamina((float) Math.floor(statsTotal.getStamina() * (1 + statsTotal.getBonusStaminaMultiplier())));
        statsTotal.setIntellect((float) Math.floor(statsTotal.getIntellect() * (1 + statsTotal.getBonusIntellectMultiplier())));
        statsTotal.setSpirit((float) Math.floor(statsTotal.getSpirit() * (1 + statsTotal.getBonusSpiritMultiplier())));
        statsTotal.setHealth((float) Math.floor(statsTotal.getHealth() + (statsTotal.getStamina() * 10f)));
        statsTotal.setMana((float) Math.floor(statsTotal.getMana() + (statsTotal.getIntellect() * 15f)));
        statsTotal.setAttackPower((float) Math.floor(statsTotal.getAttackPower() + statsTotal.getStrength() * 2));
        statsTotal.setArmor((float) Math.floor(StatConversion.getArmorFromAgility(statsTotal.getAgility())
                + StatConversion.applyMultiplier(statsTotal.getArmor(), statsTotal.getBaseArmorMultiplier())
                + StatConversion.applyMultiplier(statsTotal.getBonusArmor(), statsTotal.getBonusArmorMultiplier())));

        statsTotal.setAttackPower(statsTotal.getAttackPower() + (statsTotal.getArmor() / 180f) * (float) talents.getBladedArmor());

        statsTotal.setBonusSpellPowerMultiplier(statsTotal.getBonusSpellPowerMultiplier() + 1f);
        statsTotal.setBonusFrostDamageMultiplier(statsTotal.getBonusFrostDamageMultiplier() + 1f);
        statsTotal.setBonusShadowDamageMultiplier(statsTotal.getBonusShadowDamageMultiplier() + 1f);

        statsTotal.setAttackPower(statsTotal.getAttackPower() * (1f + statsTotal.getBonusAttackPowerMultiplier()));

        statsTotal.setBonusPhysicalDamageMultiplier(statsTotal.getBonusPhysicalDamageMultiplier() + 1f);

        if (calcOpts.getCurrentPresence() == CalculationOptionsDPSDK.Presence.Blood) { // a final, multiplicative component
            statsTotal.setBonusPhysicalDamageMultiplier(statsTotal.getBonusPhysicalDamageMultiplier() * 1.15f);
            statsTotal.setBonusSpellPowerMultiplier(statsTotal.getBonusSpellPowerMultiplier() * 1.15f);
            statsTotal.setBonusShadowDamageMultiplier(statsTotal.getBonusShadowDamageMultiplier() * 1.15f);
            statsTotal.setBonusFrostDamageMultiplier(statsTotal.getBonusFrostDamageMultiplier() * 1.15f);
        } else if (calcOpts.getCurrentPresence() == CalculationOptionsDPSDK.Presence.Unholy) { // a final, multiplicative component
            statsTotal.setPhysicalHaste(statsTotal.getPhysicalHaste() + 0.15f);
            statsTotal.setSpellHaste(statsTotal.getSpellHaste() + 0.15f);
        }

        return (statsTotal);
    }

    public Stats GetCharacterStatsMaximum(Character character, Item additionalItem, float abilityCooldown) {
        CalculationOptionsDPSDK calcOpts = character.getCalculationOptions() instanceof CalculationOptionsDPSDK
                ? (CalculationOptionsDPSDK) character.getCalculationOptions()
                : null;
        DeathKnightTalents talents = character.getDeathKnightTalents();
        Stats statsRace = GetRaceStats(character);
        Stats statsBaseGear = getItemStats(character, additionalItem);
        Stats statsBuffs = getBuffsStats(character.getActiveBuffs());
        Stats statsTalents;
        if (calcOpts.getM_bExperimental()) {
            statsTalents = GetTalentStats(talents, character);
        } else {
            statsTalents = new Stats();
            statsTalents.setBonusStrengthMultiplier(.01f * (float) (talents.getAbominationsMight() + talents.getRavenousDead())
                    + .02f * (float) (talents.getVeteranOfTheThirdWar()));
            statsTalents.setBaseArmorMultiplier(.03f * (float) (talents.getToughness()));
            statsTalents.setBonusStaminaMultiplier(.02f * (float) (talents.getVeteranOfTheThirdWar()));
            statsTalents.setExpertise((float) (talents.getTundraStalker() + talents.getRageOfRivendare()) + 2f * (float) (talents.getVeteranOfTheThirdWar()));
            statsTalents.setBonusPhysicalDamageMultiplier(.02f * (float) (talents.getBloodGorged() + talents.getRageOfRivendare()) + 0.03f * talents.getTundraStalker());
            statsTalents.setBonusSpellPowerMultiplier(.02f * (float) (talents.getBloodGorged() + talents.getRageOfRivendare()) + 0.03f * talents.getTundraStalker());
            if (talents.getUnbreakableArmor() > 0) {
                statsTalents.addSpecialEffect(seUnbreakableArmor[character.getDeathKnightTalents().isGlyphofUnbreakableArmor() ? 1 : 0][0]);
            }
        }
        Stats statsTotal = new Stats();
        Stats statsGearEnchantsBuffs = new Stats();

        statsGearEnchantsBuffs = Stats.add(Stats.add(Stats.add(statsBaseGear, statsBuffs), statsRace), statsTalents);

        statsTotal = getRelevantStats(statsGearEnchantsBuffs);
        statsTotal.setExpertise(statsTotal.getExpertise() + (float) StatConversion.getExpertiseFromRating(statsGearEnchantsBuffs.getExpertiseRating()));

        StatsSpecialEffects se = new StatsSpecialEffects(character, statsTotal, new CombatTable(character, statsTotal, calcOpts));
        int temp = 0;
        Stats statsTemp = new Stats();
        for (SpecialEffect effect : statsTotal.specialEffects()) {
            statsTemp.addSpecialEffect(effect);
        }
        Stats tempStats;
        for (SpecialEffect effect : statsTemp.specialEffects()) {
            if (hasRelevantStats(effect.getStats())) {
                if (effect.getTrigger() == Trigger.Use) {
                    float uptimeMult = 0f;
                    if (effect.getCooldown() > abilityCooldown) {
                        for (int i = 0; i * effect.getCooldown() < calcOpts.getFightLength() * 60f; i++) {
                            if (i * effect.getCooldown() % abilityCooldown == 0) {
                                uptimeMult++;
                            }
                        }
                        uptimeMult /= calcOpts.getFightLength() * 60f / abilityCooldown;
                    }
                    float tempCap = StatConversion.RATING_PER_ARMORPENETRATION * (1f - statsTotal.getArmorPenetration());
                    if (effect.getStats().getArmorPenetrationRating() > 0f
                            && effect.getStats().getArmorPenetrationRating() + statsTotal.getArmorPenetrationRating() > tempCap) {
                        tempStats = new Stats();
                        tempStats.accumulate(effect.getStats());
                        tempStats.setArmorPenetrationRating(
                                (tempCap - statsTotal.getArmorPenetrationRating() > 0f ? tempCap - statsTotal.getArmorPenetrationRating() : 0f));
                        statsTotal = Stats.add(statsTotal, Stats.multiply(tempStats, uptimeMult));
                    } else {
                        statsTotal = Stats.add(statsTotal, Stats.multiply(effect.getStats(), uptimeMult));
                    }
                } else {
                    statsTotal.addSpecialEffect(effect);
                    temp++;
                }
            }
        }
        return (statsTotal);
    }

    public Stats GetTalentStats(DeathKnightTalents talents, Character c) {
        // TODO: THis will eventually be in the common area.
        Stats TalentStats = new Stats();

        AccumulateTalents(TalentStats, c);

        return TalentStats;
    }

    /** Build the talent special effects. */
    private void AccumulateTalents(Stats FullCharacterStats, Character character) {
        Stats newStats = new Stats();
        float fDamageDone = 0f;

        // Blood Talents
        // Butchery
        // 1RPp5 per Point
        // TODO: Implement Runic Regen info.
        if (character.getDeathKnightTalents().getButchery() > 0) {
            FullCharacterStats.setRPp5(FullCharacterStats.getRPp5() + 1 * character.getDeathKnightTalents().getButchery());
        }

        // Blade Barrier
        // Reduce damage by 1% per point for 10 sec.
        if (character.getDeathKnightTalents().getBladeBarrier() > 0) {
            // If you don't have your Blood Runes on CD, you're doing it wrong.
            FullCharacterStats.setDamageTakenMultiplier(FullCharacterStats.getDamageTakenMultiplier() - (.01f * character.getDeathKnightTalents().getBladeBarrier()));
        }

        // Dark Conviction
        // Increase Crit w/ weapons, spells, and abilities by 1% per point.
        if (character.getDeathKnightTalents().getDarkConviction() > 0) {
            FullCharacterStats.setPhysicalCrit(FullCharacterStats.getPhysicalCrit() + (0.01f * character.getDeathKnightTalents().getDarkConviction()));
            FullCharacterStats.setSpellCrit(FullCharacterStats.getSpellCrit() + (0.01f * character.getDeathKnightTalents().getDarkConviction()));
        }

        // Veteran of the 3rd War
        // Patch 3.2 from 2% to 1% per point.
        // increases Str and Stam by 1% per point
        // increases expertise by 2 per point.
        if (character.getDeathKnightTalents().getVeteranOfTheThirdWar() > 0) {
            FullCharacterStats.setBonusStrengthMultiplier(FullCharacterStats.getBonusStrengthMultiplier() + (.02f * character.getDeathKnightTalents().getVeteranOfTheThirdWar()));
            FullCharacterStats.setBonusStaminaMultiplier(FullCharacterStats.getBonusStaminaMultiplier() + (.01f * character.getDeathKnightTalents().getVeteranOfTheThirdWar()));
            FullCharacterStats.setExpertise(FullCharacterStats.getExpertise() + (2f * character.getDeathKnightTalents().getVeteranOfTheThirdWar()));
        }

        // Bloody Vengence
        // 1% per point bonus to physical damage for 30 secs after a crit w/ up to 3 stacks.
        if (character.getDeathKnightTalents().getBloodyVengeance() > 0) {
            FullCharacterStats.addSpecialEffect(seBloodyVengeance1[character.getDeathKnightTalents().getBloodyVengeance()]);
            FullCharacterStats.addSpecialEffect(seBloodyVengeance2[character.getDeathKnightTalents().getBloodyVengeance()]);
        }

        // Abominations Might
        // increase AP by 5%/10% of raid.
        // 1% per point increase to str.
        if (character.getDeathKnightTalents().getAbominationsMight() > 0) {
            // This happens no matter what:
            FullCharacterStats.setBonusStrengthMultiplier(FullCharacterStats.getBonusStrengthMultiplier() + (0.01f * character.getDeathKnightTalents().getAbominationsMight()));
            // This happens only if there isn't Trueshot Aura available:
            if (!(character.activeBuffsContains("Trueshot Aura") || character.activeBuffsContains("Unleashed Rage") || character.activeBuffsContains("Abomination's Might"))) {
                FullCharacterStats.setBonusAttackPowerMultiplier(FullCharacterStats.getBonusAttackPowerMultiplier() + (.05f * character.getDeathKnightTalents().getAbominationsMight()));
            }
        }

        // Bloodworms
        // 3% chance per point per hit to cause the target to spawn 2-4 blood worms
        // Healing you 150% of the damage they do for 20 sec.
        if (character.getDeathKnightTalents().getBloodworms() > 0) {
            // TODO: figure out how much damage the worms do.
            fDamageDone = 100f;
            float fBWAttackSpeed = 2f;
            float fBWDuration = 20f;
            float key = (fDamageDone * fBWDuration / fBWAttackSpeed);
            // note, while this only creates one Dictionary entry and may seem like a waste
            // I left it open like this so that your above TODO for figuring out how much damage the worms do will make this part dynamic
            if (!seBloodworms.containsKey(key)) {
                Stats s1 = new Stats();
                s1.setHealed(((fDamageDone * fBWDuration / fBWAttackSpeed) * 1.5f));
                Stats s2 = new Stats();
                s2.setHealed(((fDamageDone * fBWDuration / fBWAttackSpeed) * 1.5f));
                Stats s3 = new Stats();
                s3.setHealed(((fDamageDone * fBWDuration / fBWAttackSpeed) * 1.5f));
                seBloodworms.put(key, new SpecialEffect[] {
                        null,
                        new SpecialEffect(Trigger.PhysicalHit, s1, fBWDuration, 0, .03f * 1),
                        new SpecialEffect(Trigger.PhysicalHit, s2, fBWDuration, 0, .03f * 2),
                        new SpecialEffect(Trigger.PhysicalHit, s3, fBWDuration, 0, .03f * 3),
                });
            }
            FullCharacterStats.addSpecialEffect(seBloodworms.get(key)[character.getDeathKnightTalents().getBloodworms()]);
        }

        // Will of the Necropolis
        // Damage that takes you below 35% health or while at less than 35% is reduced by 5% per point.
        if (character.getDeathKnightTalents().getWillOfTheNecropolis() > 0) {
            // Need to factor in the damage taken aspect of the trigger.
            // Using the assumption that the tank will be at < 35% health about that % of the time.
            FullCharacterStats.addSpecialEffect(seWillOfTheNecropolis[character.getDeathKnightTalents().getWillOfTheNecropolis()]);
        }

        // Blood Gorged
        // when above 75% health, you deal 2% more damage per point
        // when above 75% health, you receive 2% Armor Pen
        if (character.getDeathKnightTalents().getBloodGorged() > 0) {
            // Damage done increase has to be in shot rotation.
            // Assuming a 50% up time
            FullCharacterStats.setArmorPenetration(FullCharacterStats.getArmorPenetration() + (0.02f * character.getDeathKnightTalents().getBloodGorged() * 0.5f));
            FullCharacterStats.setBonusDamageMultiplier(FullCharacterStats.getBonusDamageMultiplier() + (0.02f * character.getDeathKnightTalents().getBloodGorged() * 0.5f));
        }

        // Frost Talents
        // Improved Icy Touch
        // 5% per point additional IT damage
        // 2% per point target haste reduction
        if (character.getDeathKnightTalents().getImprovedIcyTouch() > 0) {
            FullCharacterStats.setBonusIcyTouchDamage(FullCharacterStats.getBonusIcyTouchDamage() + (0.05f * character.getDeathKnightTalents().getImprovedIcyTouch()));
        }

        // Runic Power Mastery
        // Increases Max RP by 15 per point
        if (character.getDeathKnightTalents().getRunicPowerMastery() > 0) {
            BonusMaxRunicPower += 5 * character.getDeathKnightTalents().getRunicPowerMastery();
        }

        // Toughness
        // Patch 3.2: Increases Armor Value from items by 2% per point.
        // Reducing duration of all slowing effects by 6% per point.
        if (character.getDeathKnightTalents().getToughness() > 0) {
            FullCharacterStats.setBaseArmorMultiplier(AddStatMultiplierStat(FullCharacterStats.getBaseArmorMultiplier(), (.02f * character.getDeathKnightTalents().getToughness()))); // Patch 3.2
        }

        // Black Ice
        // Increase Frost & shadow damage by 2% per point
        if (character.getDeathKnightTalents().getBlackIce() > 0) {
            FullCharacterStats.setBonusFrostDamageMultiplier(FullCharacterStats.getBonusFrostDamageMultiplier() + 0.02f * character.getDeathKnightTalents().getBlackIce());
            FullCharacterStats.setBonusShadowDamageMultiplier(FullCharacterStats.getBonusShadowDamageMultiplier() + 0.02f * character.getDeathKnightTalents().getBlackIce());
        }

        // Icy Talons
        // Increase melee attack speed by 4% per point for the next 20 sec.
        if (character.getDeathKnightTalents().getIcyTalons() > 0) {
            FullCharacterStats.addSpecialEffect(seIcyTalons[character.getDeathKnightTalents().getIcyTalons()]);
        }

        // Annihilation
        // +1 % per point melee Crit chance
        // 33% per point that oblit will not consume diseases
        if (character.getDeathKnightTalents().getAnnihilation() > 0) {
            FullCharacterStats.setPhysicalCrit(FullCharacterStats.getPhysicalCrit() + (0.01f * character.getDeathKnightTalents().getAnnihilation()));
        }

        // Endless Winter
        // removes FF from COI
        // Mind Freeze RP cost is reduced by 50% per point.
        if (character.getDeathKnightTalents().getEndlessWinter() > 0) {
            FullCharacterStats.setBonusStrengthMultiplier(FullCharacterStats.getBonusStrengthMultiplier() + (.02f * character.getDeathKnightTalents().getEndlessWinter()));
        }

        // Frigid Dreadplate
        // Melee attacks against you will miss by +1% per point
        if (character.getDeathKnightTalents().getFrigidDreadplate() > 0) {
            FullCharacterStats.setMiss(FullCharacterStats.getMiss() + 0.01f * character.getDeathKnightTalents().getFrigidDreadplate());
        }

        // Glacier Rot
        // Diseased enemies take 7%, 13% , 20% more damage from IT, HB, FS.
        if (character.getDeathKnightTalents().getGlacierRot() > 0) {
            float fBonus = 0f;
            switch (character.getDeathKnightTalents().getGlacierRot()) {
                case 1: fBonus = 0.07f; break;
                case 2: fBonus = 0.13f; break;
                case 3: fBonus = 0.20f; break;
                default: break;
            }
            FullCharacterStats.setBonusIcyTouchDamage(FullCharacterStats.getBonusIcyTouchDamage() + fBonus);
            FullCharacterStats.setBonusFrostStrikeDamage(FullCharacterStats.getBonusFrostStrikeDamage() + fBonus);
            FullCharacterStats.setBonusHowlingBlastDamage(FullCharacterStats.getBonusHowlingBlastDamage() + fBonus);
        }

        // Improved Icy Talons
        // increases the melee haste of the group/raid by 20%
        // increases your haste by 5% all the time.
        if (character.getDeathKnightTalents().getImprovedIcyTalons() > 0) {
            FullCharacterStats.setPhysicalHaste(FullCharacterStats.getPhysicalHaste() + 0.05f);
            // TODO: Factor in raid utility by improving raid haste by 20%
            // As per Blue Post Effect *does* stack w/ existing IcyTalons.
            // However, it will not stack if already included on Buffs tab.
            // Now passive - no longer procs.
            if (character.activeBuffsContains("Improved Icy Talons") != true
                    && !character.activeBuffsContains("Windfury Totem")) {
                FullCharacterStats.setPhysicalHaste(FullCharacterStats.getPhysicalHaste() + .2f);
            }
        }

        // Improved Frost Presence
        // retain the health bonus 5% per point when in non-Frost presence
        // Decrease damage done to you by 1% per point.
        if (character.getDeathKnightTalents().getImprovedFrostPresence() > 0) {
            FullCharacterStats.setDamageTakenMultiplier(FullCharacterStats.getDamageTakenMultiplier() - (0.01f * character.getDeathKnightTalents().getImprovedFrostPresence()));
        }

        // Blood of the North
        // Patch 3.2: BS & FS damage +3% per point
        // Patch 3.2: BS & Pest create DeathRune from Blood 33% per point.
        if (character.getDeathKnightTalents().getBloodOfTheNorth() > 0) {
            float fBonus = 0f;
            switch (character.getDeathKnightTalents().getBloodOfTheNorth()) {
                case 1:
                    fBonus = 0.03f;
                    break;
                case 2:
                    fBonus = 0.06f;
                    break;
                case 3:
                    fBonus = 0.1f;
                    break;
                default:
                    break;
            }
            FullCharacterStats.setBonusFrostStrikeDamage(FullCharacterStats.getBonusFrostStrikeDamage() + fBonus);
            FullCharacterStats.setBonusBloodStrikeDamage(FullCharacterStats.getBonusBloodStrikeDamage() + fBonus);
        }

        // Unbreakable Armor
        // Reinforces your armor with a thick coat of ice, Increasing Armor by 25% and increasing your Strength by 20% for 20 sec.
        if (character.getDeathKnightTalents().getUnbreakableArmor() > 0) {
            // As per wowhead: GlyphofUnbreakableArmor
            // Effect: Apply Aura: Add % Modifier (3) Value: 20
            FullCharacterStats.addSpecialEffect(seUnbreakableArmor[character.getDeathKnightTalents().isGlyphofUnbreakableArmor() ? 1 : 0][0]);
        }

        // Acclimation
        // When hit by a spell, 10% chance per point to boost resistance to that type of magic for 18 sec.
        // up to 3 stacks.
        if (character.getDeathKnightTalents().getAcclimation() > 0) {
            // TODO: SpellHit is not sufficient.  Need to have this be DamageTakenSpell (vs. DamageTakenPhysical)
            FullCharacterStats.addSpecialEffect(seAcclimation[character.getDeathKnightTalents().getAcclimation()]);
        }

        // Guile of Gorefiend
        // Increases CritStrike Damage of BS, FS, HB, Oblit by 15% per point.
        // Increases Duration of IBF by 2 sec per point.
        // HACK: Implenting IceBound Fortitude.
        // Implmenting IBF here because it's affected by GoGF
        // Four T7 increases IBF by 3 sec.
        // Patch 3.2: IBF has a 120 sec CD.
        float fIBFDur = (12.0f + character.getDeathKnightTalents().getGuileOfGorefiend() * 2.0f + FullCharacterStats.getBonusIceboundFortitudeDuration());
        // IBF reduces damage taken by 20% + 3% for each 28 defense over 400.
        float ibfDefense = StatConversion.getDefenseFromRating(FullCharacterStats.getDefenseRating(), character.getCharacterClass());
        float ibfReduction = 0.3f + (ibfDefense * 0.0015f);
        if (character.getDeathKnightTalents().isGlyphofIceboundFortitude()) {
            // The glyph provides a MIN of 30% damage reduction, but doesn't help if your def takes you over that.
            ibfReduction = Math.max(0.4f, ibfReduction);
        }
        Stats ibfStats = new Stats();
        ibfStats.setDamageTakenMultiplier(-1f * ibfReduction);
        FullCharacterStats.addSpecialEffect(new SpecialEffect(Trigger.Use, ibfStats, fIBFDur, 120)); // Patch 3.2

        // Tundra Stalker
        // Your spells & abilities deal 3% per point more damage to targets w/ FF
        // Increases Expertise by 1 per point
        if (character.getDeathKnightTalents().getTundraStalker() > 0) {
            // Assuming FF is always up.
            FullCharacterStats.setBonusDamageMultiplier(FullCharacterStats.getBonusDamageMultiplier() + .03f * character.getDeathKnightTalents().getTundraStalker());
            FullCharacterStats.setExpertise(FullCharacterStats.getExpertise() + 1f * character.getDeathKnightTalents().getTundraStalker());
        }

        // Virulence
        // Increases Spell hit +1% per point
        if (character.getDeathKnightTalents().getVirulence() > 0) {
            FullCharacterStats.setSpellHit(FullCharacterStats.getSpellHit() + 0.01f * character.getDeathKnightTalents().getVirulence());
        }

        // Anticipation
        // Increases dodge by 1% per point
        if (character.getDeathKnightTalents().getAnticipation() > 0) {
            FullCharacterStats.setDodge(FullCharacterStats.getDodge() + 0.01f * character.getDeathKnightTalents().getAnticipation());
        }

        // Ravenous Dead
        // Increases Str +1% per point.
        // Increases contribution of your str & stam to ghoul by 20% per point
        if (character.getDeathKnightTalents().getRavenousDead() > 0) {
            FullCharacterStats.setBonusStrengthMultiplier(FullCharacterStats.getBonusStrengthMultiplier() + (0.01f * character.getDeathKnightTalents().getRavenousDead()));
        }

        // Magic Suppression
        // 2% per point less damage from all magic.
        // AMS absorbs additional 8, 16, 25% of spell damage.
        if (character.getDeathKnightTalents().getMagicSuppression() > 0) {
            FullCharacterStats.setSpellDamageTakenMultiplier(FullCharacterStats.getSpellDamageTakenMultiplier() - 0.02f * character.getDeathKnightTalents().getMagicSuppression());
            // AMS modification factored in above.
        }

        // AntiMagic Zone
        // Creates a zone where party/raid members take 75% less spell damage
        // Lasts 10 secs or X damage.
        if (character.getDeathKnightTalents().getAntiMagicZone() > 0) {
            FullCharacterStats.addSpecialEffect(seAntiMagicZone);
        }

        // Ebon Plaguebringer
        // CF becomes EP - increases magic damage taken by targets 4, 9, 13% in addition to disease damage
        // Increases crit strike chance by 1% per point
        if (character.getDeathKnightTalents().getEbonPlaguebringer() > 0) {
            FullCharacterStats.setPhysicalCrit(FullCharacterStats.getPhysicalCrit() + 0.01f * character.getDeathKnightTalents().getEbonPlaguebringer());
            FullCharacterStats.setSpellCrit(FullCharacterStats.getSpellCrit() + 0.01f * character.getDeathKnightTalents().getEbonPlaguebringer());
            if (!character.activeBuffsContains("Earth and Moon")
                    && !character.activeBuffsContains("Curse of the Elements")
                    && !character.activeBuffsContains("Ebon Plaguebringer")) {
                float fBonus = 0f;
                switch (character.getDeathKnightTalents().getEbonPlaguebringer()) {
                    case 1:
                        fBonus = .04f;
                        break;
                    case 2:
                        fBonus = .09f;
                        break;
                    case 3:
                        fBonus = .13f;
                        break;
                    default:
                        break;
                }
                FullCharacterStats.setBonusArcaneDamageMultiplier(FullCharacterStats.getBonusArcaneDamageMultiplier() + fBonus);
                FullCharacterStats.setBonusFireDamageMultiplier(FullCharacterStats.getBonusFireDamageMultiplier() + fBonus);
                FullCharacterStats.setBonusFrostDamageMultiplier(FullCharacterStats.getBonusFrostDamageMultiplier() + fBonus);
                FullCharacterStats.setBonusHolyDamageMultiplier(FullCharacterStats.getBonusHolyDamageMultiplier() + fBonus);
                FullCharacterStats.setBonusNatureDamageMultiplier(FullCharacterStats.getBonusNatureDamageMultiplier() + fBonus);
                FullCharacterStats.setBonusShadowDamageMultiplier(FullCharacterStats.getBonusShadowDamageMultiplier() + fBonus);
            }
        }

        // Rage of Rivendare
        // 2% per point more damage to targets w/ BP
        // Expertise +1 per point
        if (character.getDeathKnightTalents().getRageOfRivendare() > 0) {
            FullCharacterStats.setExpertise(FullCharacterStats.getExpertise() + character.getDeathKnightTalents().getRageOfRivendare());
            // Assuming BP is always on.
            FullCharacterStats.setBonusDamageMultiplier(FullCharacterStats.getBonusDamageMultiplier() + 0.02f * character.getDeathKnightTalents().getRageOfRivendare());
        }
    }

    // Custom Charts
    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        List<ComparisonCalculationBase> comparisonList = new ArrayList<ComparisonCalculationBase>();
        CharacterCalculationsDPSDK baseCalc, calc;
        ComparisonCalculationBase comparison;
        float[] subPoints;
        float fMultiplier = 1f;

        baseCalc = (CharacterCalculationsDPSDK) getCharacterCalculations(character);

        String[] statList = new String[] {
                "Strength",
                "Agility",
                "Attack Power",
                "Crit Rating",
                "Hit Rating",
                "Expertise Rating",
                "Haste Rating",
                "Armor Penetration Rating",
        };

        switch (chartName) {
            //"Item Budget (10 point steps)","Item Budget (25 point steps)","Item Budget(50 point steps)","Item Budget (100 point steps)"
            case "Item Budget (10 point steps)": {
                fMultiplier = 1f;
                break;
            }
            case "Item Budget (25 point steps)": {
                fMultiplier = 2.5f;
                break;
            }
            case "Item Budget (50 point steps)": {
                fMultiplier = 5f;
                break;
            }
            case "Item Budget (100 point steps)": {
                fMultiplier = 10f;
                break;
            }
            case "Presences": {
                String[] listPresence = new String[] {
                        "None",
                        "Blood",
                        "Unholy",
                        "Frost",
                };

                // Set this to have no presence enabled.
                Character baseCharacter = character.clone();
                ((CalculationOptionsDPSDK) baseCharacter.getCalculationOptions()).setCurrentPresence(CalculationOptionsDPSDK.Presence.None);
                // replacing pre-factored base calc since this is different than the Item budget lists.
                baseCalc = (CharacterCalculationsDPSDK) getCharacterCalculations(baseCharacter, null, true, false, false);

                // Set these to have the key presence enabled.
                for (int index = 1; index < listPresence.length; index++) {
                    ((CalculationOptionsDPSDK) character.getCalculationOptions()).setCurrentPresence(CalculationOptionsDPSDK.Presence.values()[index]);

                    calc = (CharacterCalculationsDPSDK) getCharacterCalculations(character, null, false, true, true);

                    comparison = createNewComparisonCalculation();
                    comparison.setName(listPresence[index]);
                    comparison.setEquipped(false);
                    comparison.setOverallPoints(calc.getOverallPoints() - baseCalc.getOverallPoints());
                    subPoints = new float[calc.getSubPoints().length];
                    for (int i = 0; i < calc.getSubPoints().length; i++) {
                        subPoints[i] = calc.getSubPoints()[i] - baseCalc.getSubPoints()[i];
                    }
                    comparison.setSubPoints(subPoints);

                    comparisonList.add(comparison);
                }
                return comparisonList.toArray(new ComparisonCalculationBase[0]);
            }
            default:
                return new ComparisonCalculationBase[0];
        }

        // Item Budget list. since it's used multiple times.
        Item[] itemList = new Item[] {
                itemWithStats(statItem(fMultiplier * 10, "Strength")),
                itemWithStats(statItem(fMultiplier * 10, "Agility")),
                itemWithStats(statItem(fMultiplier * 20, "AttackPower")),
                itemWithStats(statItem(fMultiplier * 10, "CritRating")),
                itemWithStats(statItem(fMultiplier * 10, "HitRating")),
                itemWithStats(statItem(fMultiplier * 10, "ExpertiseRating")),
                itemWithStats(statItem(fMultiplier * 10, "HasteRating")),
                itemWithStats(statItem(fMultiplier * 10, "ArmorPenetrationRating")),
        };
        // Do the math.
        for (int index = 0; index < itemList.length; index++) {
            calc = (CharacterCalculationsDPSDK) getCharacterCalculations(character, itemList[index]);

            comparison = createNewComparisonCalculation();
            comparison.setName(statList[index]);
            comparison.setEquipped(false);
            comparison.setOverallPoints(calc.getOverallPoints() - baseCalc.getOverallPoints());
            subPoints = new float[calc.getSubPoints().length];
            for (int i = 0; i < calc.getSubPoints().length; i++) {
                subPoints[i] = calc.getSubPoints()[i] - baseCalc.getSubPoints()[i];
            }
            comparison.setSubPoints(subPoints);

            comparisonList.add(comparison);
        }
        return comparisonList.toArray(new ComparisonCalculationBase[0]);
    }

    private static Stats statItem(float value, String property) {
        Stats stats = new Stats();
        Stats.StatProperty p = Stats.PROPERTY_BY_NAME.get(property);
        p.mutator().set(stats, value);
        return stats;
    }

    private static Item itemWithStats(Stats stats) {
        Item item = new Item();
        item.setStats(stats);
        return item;
    }

    // Relevant Stats?
    @Override
    public boolean isItemRelevant(Item item) {
        if (item.getSlot() == ItemSlot.OffHand) {
            return false;
        }
        return super.isItemRelevant(item);
    }

    public Stats getRelevantStats(Stats stats) {
        Stats s = new Stats();
        // Core stats
        s.setStrength(stats.getStrength());
        s.setAgility(stats.getAgility());
        s.setStamina(stats.getStamina());
        s.setArmorPenetrationRating(stats.getArmorPenetrationRating());
        s.setExpertiseRating(stats.getExpertiseRating());
        s.setAttackPower(stats.getAttackPower());
        // Other Base Stats
        s.setHasteRating(stats.getHasteRating());
        s.setHitRating(stats.getHitRating());
        s.setCritRating(stats.getCritRating());
        s.setArmor(stats.getArmor());
        s.setBonusArmor(stats.getBonusArmor());
        s.setResilience(stats.getResilience());

        // Secondary Stats
        s.setHealth(stats.getHealth());
        s.setArmorPenetration(stats.getArmorPenetration());
        s.setSpellHaste(stats.getSpellHaste());
        s.setPhysicalCrit(stats.getPhysicalCrit());
        s.setPhysicalHaste(stats.getPhysicalHaste());
        s.setPhysicalHit(stats.getPhysicalHit());
        s.setSpellCrit(stats.getSpellCrit());
        s.setSpellCritOnTarget(stats.getSpellCritOnTarget());
        s.setSpellHit(stats.getSpellHit());
        s.setSpellPenetration(stats.getSpellPenetration());

        // Damage stats
        s.setWeaponDamage(stats.getWeaponDamage());
        s.setPhysicalDamage(stats.getPhysicalDamage());
        s.setShadowDamage(stats.getShadowDamage());
        s.setArcaneDamage(stats.getArcaneDamage());
        s.setFireDamage(stats.getFireDamage());
        s.setFrostDamage(stats.getFrostDamage());
        s.setHolyDamage(stats.getHolyDamage());
        s.setNatureDamage(stats.getNatureDamage());

        // Bonus to stat
        s.setBonusStrengthMultiplier(stats.getBonusStrengthMultiplier());
        s.setBonusStaminaMultiplier(stats.getBonusStaminaMultiplier());
        s.setBonusAgilityMultiplier(stats.getBonusAgilityMultiplier());
        s.setBonusCritMultiplier(stats.getBonusCritMultiplier());
        s.setBonusAttackPowerMultiplier(stats.getBonusAttackPowerMultiplier());

        // Bonus to Damage
        // *Damage
        s.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());
        s.setBonusPhysicalDamageMultiplier(stats.getBonusPhysicalDamageMultiplier());
        s.setBonusShadowDamageMultiplier(stats.getBonusShadowDamageMultiplier());
        s.setBonusFrostDamageMultiplier(stats.getBonusFrostDamageMultiplier());
        s.setBonusDiseaseDamageMultiplier(stats.getBonusDiseaseDamageMultiplier());
        s.setBonusRuneStrikeMultiplier(stats.getBonusRuneStrikeMultiplier());
        s.setBonusObliterateMultiplier(stats.getBonusObliterateMultiplier());
        s.setBonusHeartStrikeMultiplier(stats.getBonusHeartStrikeMultiplier());
        s.setBonusScourgeStrikeMultiplier(stats.getBonusScourgeStrikeMultiplier());
        // +Damage
        s.setBonusFrostWeaponDamage(stats.getBonusFrostWeaponDamage());
        s.setBonusScourgeStrikeDamage(stats.getBonusScourgeStrikeDamage());
        s.setBonusBloodStrikeDamage(stats.getBonusBloodStrikeDamage());
        s.setBonusDeathCoilDamage(stats.getBonusDeathCoilDamage());
        s.setBonusDeathStrikeDamage(stats.getBonusDeathStrikeDamage());
        s.setBonusFrostStrikeDamage(stats.getBonusFrostStrikeDamage());
        s.setBonusHeartStrikeDamage(stats.getBonusHeartStrikeDamage());
        s.setBonusIcyTouchDamage(stats.getBonusIcyTouchDamage());
        s.setBonusObliterateDamage(stats.getBonusObliterateDamage());
        s.setBonusPerDiseaseBloodStrikeDamage(stats.getBonusPerDiseaseBloodStrikeDamage());
        s.setBonusPerDiseaseHeartStrikeDamage(stats.getBonusPerDiseaseHeartStrikeDamage());
        s.setBonusPerDiseaseObliterateDamage(stats.getBonusPerDiseaseObliterateDamage());
        s.setBonusPerDiseaseScourgeStrikeDamage(stats.getBonusPerDiseaseScourgeStrikeDamage());
        // Crit
        s.setBonusDeathCoilCrit(stats.getBonusDeathCoilCrit());
        s.setBonusDeathStrikeCrit(stats.getBonusDeathStrikeCrit());
        s.setBonusFrostStrikeCrit(stats.getBonusFrostStrikeCrit());
        s.setBonusObliterateCrit(stats.getBonusObliterateCrit());
        s.setBonusPlagueStrikeCrit(stats.getBonusPlagueStrikeCrit());
        s.setBonusScourgeStrikeCrit(stats.getBonusScourgeStrikeCrit());
        // RP
        s.setBonusRPFromDeathStrike(stats.getBonusRPFromDeathStrike());
        s.setBonusRPFromObliterate(stats.getBonusRPFromObliterate());
        s.setBonusRPFromScourgeStrike(stats.getBonusRPFromScourgeStrike());
        // Other
        s.setCinderglacierProc(stats.getCinderglacierProc());
        s.setDiseasesCanCrit(stats.getDiseasesCanCrit());
        s.setHighestStat(stats.getHighestStat());
        s.setParagon(stats.getParagon());
        s.setDeathbringerProc(stats.getDeathbringerProc());

        for (SpecialEffect effect : stats.specialEffects()) {
            if (hasRelevantStats(effect.getStats())) {
                if (effect.getTrigger() == Trigger.DamageDone
                        || effect.getTrigger() == Trigger.DamageOrHealingDone
                        || effect.getTrigger() == Trigger.DamageSpellCast
                        || effect.getTrigger() == Trigger.DamageSpellCrit
                        || effect.getTrigger() == Trigger.DamageSpellHit
                        || effect.getTrigger() == Trigger.SpellCast
                        || effect.getTrigger() == Trigger.SpellCrit
                        || effect.getTrigger() == Trigger.SpellHit
                        || effect.getTrigger() == Trigger.DoTTick
                        || effect.getTrigger() == Trigger.MeleeCrit
                        || effect.getTrigger() == Trigger.MeleeHit
                        || effect.getTrigger() == Trigger.CurrentHandHit
                        || effect.getTrigger() == Trigger.MainHandHit
                        || effect.getTrigger() == Trigger.OffHandHit
                        || effect.getTrigger() == Trigger.PhysicalCrit
                        || effect.getTrigger() == Trigger.PhysicalHit
                        || effect.getTrigger() == Trigger.BloodStrikeHit
                        || effect.getTrigger() == Trigger.HeartStrikeHit
                        || effect.getTrigger() == Trigger.BloodStrikeOrHeartStrikeHit
                        || effect.getTrigger() == Trigger.ScourgeStrikeHit
                        || effect.getTrigger() == Trigger.ObliterateHit
                        || effect.getTrigger() == Trigger.DeathStrikeHit
                        || effect.getTrigger() == Trigger.IcyTouchHit
                        || effect.getTrigger() == Trigger.PlagueStrikeHit
                        || effect.getTrigger() == Trigger.RuneStrikeHit
                        || effect.getTrigger() == Trigger.Use) {
                    s.addSpecialEffect(effect);
                }
            }
        }

        return s;
    }

    @Override
    public boolean hasRelevantStats(Stats stats) {
        boolean bRelevant = false;
        for (SpecialEffect effect : stats.specialEffects()) {
            if (relevantStats(effect.getStats())) {
                if (effect.getTrigger() == Trigger.DamageDone
                        || effect.getTrigger() == Trigger.DamageOrHealingDone
                        || effect.getTrigger() == Trigger.DamageSpellCast
                        || effect.getTrigger() == Trigger.DamageSpellCrit
                        || effect.getTrigger() == Trigger.DamageSpellHit
                        || effect.getTrigger() == Trigger.SpellCast
                        || effect.getTrigger() == Trigger.SpellCrit
                        || effect.getTrigger() == Trigger.SpellHit
                        || effect.getTrigger() == Trigger.DoTTick
                        || effect.getTrigger() == Trigger.MeleeCrit
                        || effect.getTrigger() == Trigger.MeleeHit
                        || effect.getTrigger() == Trigger.CurrentHandHit
                        || effect.getTrigger() == Trigger.MainHandHit
                        || effect.getTrigger() == Trigger.OffHandHit
                        || effect.getTrigger() == Trigger.PhysicalCrit
                        || effect.getTrigger() == Trigger.PhysicalHit
                        || effect.getTrigger() == Trigger.BloodStrikeHit
                        || effect.getTrigger() == Trigger.HeartStrikeHit
                        || effect.getTrigger() == Trigger.BloodStrikeOrHeartStrikeHit
                        || effect.getTrigger() == Trigger.ScourgeStrikeHit
                        || effect.getTrigger() == Trigger.ObliterateHit
                        || effect.getTrigger() == Trigger.DeathStrikeHit
                        || effect.getTrigger() == Trigger.IcyTouchHit
                        || effect.getTrigger() == Trigger.PlagueStrikeHit
                        || effect.getTrigger() == Trigger.RuneStrikeHit
                        || effect.getTrigger() == Trigger.Use) {
                    for (SpecialEffect e : effect.getStats().specialEffects()) {
                        if (!bRelevant) bRelevant = hasRelevantStats(e.getStats());
                    }
                    if (!bRelevant) bRelevant = relevantStats(effect.getStats());
                }
            }
        }
        if (!bRelevant) bRelevant = relevantStats(stats);
        return bRelevant;
    }

    private boolean relevantStats(Stats stats) {
        boolean bResults = false;
        // Core stats
        bResults |= (stats.getStrength() != 0);
        bResults |= (stats.getAgility() != 0);
        bResults |= (stats.getStamina() != 0);
        bResults |= (stats.getArmorPenetrationRating() != 0);
        bResults |= (stats.getExpertiseRating() != 0);
        bResults |= (stats.getAttackPower() != 0);
        boolean bHasCore = bResults; // if the above stats are 0, lets make sure we're not bringing in caster gear below.
        // Other Base Stats
        bResults |= (stats.getHasteRating() != 0);
        bResults |= (stats.getHitRating() != 0);
        bResults |= (stats.getCritRating() != 0);
        bResults |= (stats.getArmor() != 0);
        bResults |= (stats.getBonusArmor() != 0);
        bResults |= (stats.getResilience() != 0);

        // Secondary Stats
        bResults |= (stats.getHealth() != 0);
        bResults |= (stats.getArmorPenetration() != 0);
        bResults |= (stats.getSpellHaste() != 0);
        bResults |= (stats.getPhysicalCrit() != 0);
        bResults |= (stats.getPhysicalHaste() != 0);
        bResults |= (stats.getPhysicalHit() != 0);
        bResults |= (stats.getSpellCrit() != 0);
        bResults |= (stats.getSpellCritOnTarget() != 0);
        bResults |= (stats.getSpellHit() != 0);
        bResults |= (stats.getSpellHaste() != 0);
        bResults |= (stats.getSpellPenetration() != 0);

        // Damage stats
        bResults |= (stats.getWeaponDamage() != 0);
        bResults |= (stats.getPhysicalDamage() != 0);
        bResults |= (stats.getShadowDamage() != 0);
        bResults |= (stats.getArcaneDamage() != 0);
        bResults |= (stats.getFireDamage() != 0);
        bResults |= (stats.getFrostDamage() != 0);
        bResults |= (stats.getHolyDamage() != 0);
        bResults |= (stats.getNatureDamage() != 0);

        // Bonus to stat
        bResults |= (stats.getBonusStrengthMultiplier() != 0);
        bResults |= (stats.getBonusStaminaMultiplier() != 0);
        bResults |= (stats.getBonusAgilityMultiplier() != 0);
        bResults |= (stats.getBonusCritMultiplier() != 0);
        bResults |= (stats.getBonusAttackPowerMultiplier() != 0);

        // Bonus to Damage
        // *Damage
        bResults |= (stats.getBonusDamageMultiplier() != 0);
        bResults |= (stats.getBonusPhysicalDamageMultiplier() != 0);
        bResults |= (stats.getBonusShadowDamageMultiplier() != 0);
        bResults |= (stats.getBonusFrostDamageMultiplier() != 0);
        bResults |= (stats.getBonusDiseaseDamageMultiplier() != 0);
        bResults |= (stats.getBonusRuneStrikeMultiplier() != 0);
        bResults |= (stats.getBonusObliterateMultiplier() != 0);
        bResults |= (stats.getBonusHeartStrikeMultiplier() != 0);
        bResults |= (stats.getBonusScourgeStrikeMultiplier() != 0);
        // +Damage
        bResults |= (stats.getBonusFrostWeaponDamage() != 0);
        bResults |= (stats.getBonusScourgeStrikeDamage() != 0);
        bResults |= (stats.getBonusBloodStrikeDamage() != 0);
        bResults |= (stats.getBonusDeathCoilDamage() != 0);
        bResults |= (stats.getBonusDeathStrikeDamage() != 0);
        bResults |= (stats.getBonusFrostStrikeDamage() != 0);
        bResults |= (stats.getBonusHeartStrikeDamage() != 0);
        bResults |= (stats.getBonusIcyTouchDamage() != 0);
        bResults |= (stats.getBonusObliterateDamage() != 0);
        bResults |= (stats.getBonusPerDiseaseBloodStrikeDamage() != 0);
        bResults |= (stats.getBonusPerDiseaseHeartStrikeDamage() != 0);
        bResults |= (stats.getBonusPerDiseaseObliterateDamage() != 0);
        bResults |= (stats.getBonusPerDiseaseScourgeStrikeDamage() != 0);
        // Crit
        bResults |= (stats.getBonusCritMultiplier() != 0);
        bResults |= (stats.getBonusDeathCoilCrit() != 0);
        bResults |= (stats.getBonusDeathStrikeCrit() != 0);
        bResults |= (stats.getBonusFrostStrikeCrit() != 0);
        bResults |= (stats.getBonusObliterateCrit() != 0);
        bResults |= (stats.getBonusPlagueStrikeCrit() != 0);
        bResults |= (stats.getBonusScourgeStrikeCrit() != 0);
        // RP
        bResults |= (stats.getBonusRPFromDeathStrike() != 0);
        bResults |= (stats.getBonusRPFromObliterate() != 0);
        bResults |= (stats.getBonusRPFromScourgeStrike() != 0);
        // Other
        bResults |= (stats.getCinderglacierProc() != 0);
        bResults |= (stats.getDiseasesCanCrit() != 0);
        bResults |= (stats.getHighestStat() != 0);
        bResults |= (stats.getParagon() != 0);
        bResults |= (stats.getDeathbringerProc() != 0);

        // Filter out caster gear:
        if (!bHasCore & bResults)
        // Let's make sure that if we've got some stats that may be interesting
        {
            bResults = !((stats.getIntellect() != 0)
                    || (stats.getSpirit() != 0)
                    || (stats.getMp5() != 0)
                    || (stats.getSpellPower() != 0)
                    || (stats.getMana() != 0)
            );
        }
        return bResults;
    }

    private String[] optimizableCalculationLabels = null;

    public String[] getOptimizableCalculationLabels() {
        if (this.optimizableCalculationLabels == null)
            this.optimizableCalculationLabels = new String[] {
                    "Health",
                    "Crit Rating",
                    "Expertise Rating",
                    "Hit Rating",
                    "Haste Rating",
                    "Target Miss %",
                    "Target Dodge %",
                    "Resilience",
                    "Spell Penetration"
            };

        return this.optimizableCalculationLabels;
    }

    // Static SpecialEffects
    private static Map<Float, SpecialEffect[]> seSpellDeflection = new LinkedHashMap<Float, SpecialEffect[]>();
    private static final SpecialEffect seT10_4P;
    private static final SpecialEffect seFC1;
    private static final SpecialEffect seFC2;
    private static final SpecialEffect[][] seVampiricBlood;
    private static final SpecialEffect[] seRuneTap;
    private static final SpecialEffect[] seBloodyVengeance1;
    private static final SpecialEffect[] seBloodyVengeance2;
    private static final Map<Float, SpecialEffect[]> seBloodworms = new LinkedHashMap<Float, SpecialEffect[]>();
    private static final SpecialEffect[] seWillOfTheNecropolis;
    private static final SpecialEffect[] seIcyTalons;
    private static final SpecialEffect[][] seUnbreakableArmor;
    private static final SpecialEffect[] seAcclimation;
    private static final SpecialEffect seAntiMagicZone;

    static {
        Stats s = new Stats();
        s.setDamageTakenMultiplier(-0.12f);
        seT10_4P = new SpecialEffect(Trigger.Use, s, 10f, 60f);

        s = new Stats();
        s.setBonusStrengthMultiplier(.15f);
        seFC1 = new SpecialEffect(Trigger.DamageDone, s, 15f, 0f, -2f, 1);

        s = new Stats();
        s.setHealthRestoreFromMaxHealth(.03f);
        seFC2 = new SpecialEffect(Trigger.DamageDone, s, 0, 0f, -2f, 1);

        seVampiricBlood = new SpecialEffect[][] {
                { new SpecialEffect(Trigger.Use, null, 10 + 0 * 5, 60f - (false ? 0 : 10)), new SpecialEffect(Trigger.Use, null, 10 + 0 * 5, 60f - (true ? 0 : 10)), },
                { new SpecialEffect(Trigger.Use, null, 10 + 1 * 5, 60f - (false ? 0 : 10)), new SpecialEffect(Trigger.Use, null, 10 + 1 * 5, 60f - (true ? 0 : 10)), },
        };
        seRuneTap = new SpecialEffect[] {
                new SpecialEffect(Trigger.Use, null, 0, 60f - 10 * 0),
                new SpecialEffect(Trigger.Use, null, 0, 60f - 10 * 1),
                new SpecialEffect(Trigger.Use, null, 0, 60f - 10 * 2),
                new SpecialEffect(Trigger.Use, null, 0, 60f - 10 * 3),
        };
        seBloodyVengeance1 = new SpecialEffect[] {
                null,
                new SpecialEffect(Trigger.DamageSpellCrit, bonusPhysical(.01f * 0), 30, 0, 1, 3),
                new SpecialEffect(Trigger.DamageSpellCrit, bonusPhysical(.01f * 1), 30, 0, 1, 3),
                new SpecialEffect(Trigger.DamageSpellCrit, bonusPhysical(.01f * 2), 30, 0, 1, 3),
                new SpecialEffect(Trigger.DamageSpellCrit, bonusPhysical(.01f * 3), 30, 0, 1, 3),
        };
        seBloodyVengeance2 = new SpecialEffect[] {
                null,
                new SpecialEffect(Trigger.MeleeCrit, bonusPhysical(.01f * 0), 30, 0, 1, 3),
                new SpecialEffect(Trigger.MeleeCrit, bonusPhysical(.01f * 1), 30, 0, 1, 3),
                new SpecialEffect(Trigger.MeleeCrit, bonusPhysical(.01f * 2), 30, 0, 1, 3),
                new SpecialEffect(Trigger.MeleeCrit, bonusPhysical(.01f * 3), 30, 0, 1, 3),
        };
        seWillOfTheNecropolis = new SpecialEffect[] {
                null,
                new SpecialEffect(Trigger.DamageTaken, damageTaken(-(0.05f * 1)), 0, 0, 0.35f),
                new SpecialEffect(Trigger.DamageTaken, damageTaken(-(0.05f * 2)), 0, 0, 0.35f),
                new SpecialEffect(Trigger.DamageTaken, damageTaken(-(0.05f * 3)), 0, 0, 0.35f),
        };
        seIcyTalons = new SpecialEffect[] {
                null,
                new SpecialEffect(Trigger.FrostFeverHit, physicalHaste(0.04f * 1), 20f, 0f),
                new SpecialEffect(Trigger.FrostFeverHit, physicalHaste(0.04f * 2), 20f, 0f),
                new SpecialEffect(Trigger.FrostFeverHit, physicalHaste(0.04f * 3), 20f, 0f),
                new SpecialEffect(Trigger.FrostFeverHit, physicalHaste(0.04f * 4), 20f, 0f),
                new SpecialEffect(Trigger.FrostFeverHit, physicalHaste(0.04f * 5), 20f, 0f),
        };
        seUnbreakableArmor = new SpecialEffect[][] {
                new SpecialEffect[] {
                        new SpecialEffect(Trigger.Use, unbreakableArmor(false), 20f, 60f - 0 * 10f),
                        new SpecialEffect(Trigger.Use, unbreakableArmor(true), 20f, 60f - 0 * 10f),
                },
                new SpecialEffect[] {
                        new SpecialEffect(Trigger.Use, unbreakableArmor(false), 20f, 60f - 1 * 10f),
                        new SpecialEffect(Trigger.Use, unbreakableArmor(true), 20f, 60f - 1 * 10f),
                },
        };
        seAcclimation = new SpecialEffect[] {
                null,
                new SpecialEffect(Trigger.DamageTakenMagical, resistances(), 18f, 0f, (0.10f * 1), 3),
                new SpecialEffect(Trigger.DamageTakenMagical, resistances(), 18f, 0f, (0.10f * 2), 3),
                new SpecialEffect(Trigger.DamageTakenMagical, resistances(), 18f, 0f, (0.10f * 3), 3),
        };
        s = new Stats();
        s.setSpellDamageTakenMultiplier(-0.75f);
        seAntiMagicZone = new SpecialEffect(Trigger.Use, s, 10f, 2f * 60f);
    }

    private static Stats bonusPhysical(float value) {
        Stats s = new Stats();
        s.setBonusPhysicalDamageMultiplier(value);
        return s;
    }

    private static Stats damageTaken(float value) {
        Stats s = new Stats();
        s.setDamageTakenMultiplier(value);
        return s;
    }

    private static Stats physicalHaste(float value) {
        Stats s = new Stats();
        s.setPhysicalHaste(value);
        return s;
    }

    private static Stats unbreakableArmor(boolean glyphed) {
        Stats s = new Stats();
        s.setBonusStrengthMultiplier(0.20f);
        s.setBaseArmorMultiplier(.25f + (glyphed ? .20f : 0f));
        s.setBonusArmorMultiplier(.25f + (glyphed ? .20f : 0f));
        return s;
    }

    private static Stats resistances() {
        Stats s = new Stats();
        s.setFireResistance(50f);
        s.setFrostResistance(50f);
        s.setArcaneResistance(50f);
        s.setShadowResistance(50f);
        s.setNatureResistance(50f);
        return s;
    }
}
