// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Enhance/CalculationsEnhance.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: UI-only members omitted (CalculationOptionsPanel and the internal Experimental string
// setter). EnhSim.cs (clipboard export, System.Windows dependency), PriorityQueue.cs (unused dead
// code) and the fully commented-out OldCode.cs/OldPriorities.cs are not ported. The non-Silverlight
// label/color/optimizable/chart/glyph/type tables are ported as getters.
// FRAMEWORK ADAPTATION: deserializeDataObject uses a JDK-XML reader instead of XmlSerializer (same
// approach as CalculationsRetribution); the fixture PriorityList/StatsList are parsed explicitly.
package rawr.models.enhance;

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
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class CalculationsEnhance extends CalculationsBase {

    // C# takes this from the assembly version (Rawr.Enhance.dll, fork build 2.3.23.0).
    private static final String VERSION = "2.3.23.0";

    private List<GemmingTemplate> defaultGemmingTemplates = null;

    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        //Meta
        int chaotic = 41285;
        int relentless = 41398;

        if (this.defaultGemmingTemplates == null) {
            Gemming gemming = new Gemming();
            this.defaultGemmingTemplates = new ArrayList<GemmingTemplate>();
            this.defaultGemmingTemplates.addAll(gemming.addTemplates("Rare", 1, relentless, false));
            this.defaultGemmingTemplates.addAll(gemming.addTemplates("Rare", 1, chaotic, false));
            this.defaultGemmingTemplates.addAll(gemming.addTemplates("Epic", 2, relentless, true));  // Enable Epic gems by default
            this.defaultGemmingTemplates.addAll(gemming.addTemplates("Epic", 2, chaotic, true));     // Enable Epic gems by default
            this.defaultGemmingTemplates.addAll(gemming.addTemplates("Jeweler", 3, relentless, false));
            this.defaultGemmingTemplates.addAll(gemming.addTemplates("Jeweler", 3, chaotic, false));
        }
        return this.defaultGemmingTemplates;
    }

    private String[] characterDisplayCalculationLabels = null;

    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        if (this.characterDisplayCalculationLabels == null)
            this.characterDisplayCalculationLabels = new String[] {
                    "Summary:DPS Points*Your total expected DPS with this kit and selected glyphs and buffs",
                    "Summary:Survivability Points*Assumes basic 2% of total health as Survivability",
                    "Summary:Overall Points*This is the sum of Total DPS and Survivability. If you want sort items by DPS only select DPS from the sort dropdown top right",
                    "Basic Stats:Health",
                    "Basic Stats:Mana",
                    "Basic Stats:Attack Power",
                    "Basic Stats:Agility",
                    "Basic Stats:Intellect",
                    "Basic Stats:Strength",
                    "Basic Stats:Yellow Hit",
                    "Basic Stats:Spell Hit",
                    "Basic Stats:White Hit",
                    "Basic Stats:Melee Crit",
                    "Basic Stats:Spell Crit",
                    "Basic Stats:Spellpower",
                    "Basic Stats:Total Expertise",
                    "Basic Stats:Hit Rating",
                    "Basic Stats:Haste Rating",
                    "Basic Stats:Armour Pen Rating",
                    "Complex Stats:Avoided Attacks*The percentage of your attacks that fail to land.",
                    "Complex Stats:Avg MH Speed",
                    "Complex Stats:Avg OH Speed",
                    "Complex Stats:Armor Mitigation",
                    "Complex Stats:Flurry Uptime",
                    "Complex Stats:ED Uptime*Elemental Devastation Uptime percentage",
                    "Complex Stats:Avg Time to 5 Stack*Average time it takes to get 5 stacks of Maelstrom Weapon.",
                    "Complex Stats:MH Enchant Uptime",
                    "Complex Stats:OH Enchant Uptime",
                    "Complex Stats:Trinket 1 Uptime",
                    "Complex Stats:Trinket 2 Uptime",
                    "Complex Stats:Fire Totem Uptime",
                    "Complex Stats:Tier 10 2 pc Uptime",
                    "Complex Stats:Tier 10 4 pc Uptime",
                    "Attacks:White Damage",
                    "Attacks:Windfury Attack",
                    "Attacks:Flametongue Attack",
                    "Attacks:Stormstrike",
                    "Attacks:Lava Lash",
                    "Attacks:Searing/Magma Totem",
                    "Attacks:Earth Shock",
                    "Attacks:Flame Shock",
                    "Attacks:Lightning Bolt",
                    "Attacks:Fire Nova",
                    "Attacks:Fire Elemental",
                    "Attacks:Lightning Shield",
                    "Attacks:Spirit Wolf",
                    "Attacks:Total DPS",
                    "Module Version:Enhance Version"
            };
        return java.util.Arrays.asList(this.characterDisplayCalculationLabels);
    }

    private String[] optimizableCalculationLabels = null;

    public String[] getOptimizableCalculationLabels() {
        if (this.optimizableCalculationLabels == null)
            this.optimizableCalculationLabels = new String[] {
                    "Hit Rating",
                    "Dodged Attacks",
                    "Parried Attacks",
                    "Avoided Attacks",
                    "Health"
            };
        return this.optimizableCalculationLabels;
    }

    private String[] customChartNames = null;

    public String[] getCustomChartNames() {
        if (this.customChartNames == null)
            this.customChartNames = new String[] {
                    "Combat Table (White)",
                    "Combat Table (Yellow)",
                    "Combat Table (Spell)",
                    "Relative Gem Values",
                    "MH Weapon Speeds",
                    "OH Weapon Speeds",
            };
        return this.customChartNames;
    }

    // C# non-Silverlight SubPointNameColors (System.Drawing.Color.FromArgb(R,G,B), alpha 255).
    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> colors = new LinkedHashMap<String, String>();
        colors.put("DPS", "#A000E0");
        colors.put("Survivability", "#408020");
        return colors;
    }

    // Stage B additive API (web chart panel): same label as the other melee models.
    @Override
    public String getOverallRatingLabel() {
        return "Overall Points";
    }

    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.Shaman;
    }

    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationEnhance();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsEnhance();
    }

    // C# CalculationsEnhance.cs:199-206 DeserializeDataObject (XmlSerializer; JDK-XML reader here).
    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            org.w3c.dom.Element root = document.getDocumentElement();
            CalculationOptionsEnhance calcOpts = new CalculationOptionsEnhance();
            for (org.w3c.dom.Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                org.w3c.dom.Element child = (org.w3c.dom.Element) node;
                String name = child.getTagName();
                String text = child.getTextContent().trim();
                switch (name) {
                    case "BossName": calcOpts.setBossName(text); break;
                    case "TargetLevel": calcOpts.setTargetLevel(text.isEmpty() ? 0 : Integer.parseInt(text)); break;
                    case "TargetArmor": calcOpts.setTargetArmor(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "InBack": calcOpts.setInBack(Boolean.parseBoolean(text)); break;
                    case "InBackPerc": calcOpts.setInBackPerc(text.isEmpty() ? 0 : Integer.parseInt(text)); break;
                    case "AverageLag": calcOpts.setAverageLag(text.isEmpty() ? 0 : Integer.parseInt(text)); break;
                    case "ReactionTime": calcOpts.setReactionTime(text.isEmpty() ? 0 : Integer.parseInt(text)); break;
                    case "MinManaSR": calcOpts.setMinManaSR(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "MainhandImbue": calcOpts.setMainhandImbue(text); break;
                    case "OffhandImbue": calcOpts.setOffhandImbue(text); break;
                    case "CalculationToGraph": calcOpts.setCalculationToGraph(text); break;
                    case "FightLength": calcOpts.setFightLength(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "FightLengthMultiplier": calcOpts.setFightLengthMultiplier(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "TargetFireResistance": calcOpts.setTargetFireResistance(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "TargetNatureResistance": calcOpts.setTargetNatureResistance(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "BaseStatOption": calcOpts.setBaseStatOption(Boolean.parseBoolean(text)); break;
                    case "UseMana": calcOpts.setUseMana(Boolean.parseBoolean(text)); break;
                    case "ShowExportMessageBox": calcOpts.setShowExportMessageBox(Boolean.parseBoolean(text)); break;
                    case "MultipleTargets": calcOpts.setMultipleTargets(Boolean.parseBoolean(text)); break;
                    case "AdditionalTargets": calcOpts.setAdditionalTargets(text.isEmpty() ? 0 : Integer.parseInt(text)); break;
                    case "AdditionalTargetPercent": calcOpts.setAdditionalTargetPercent(text.isEmpty() ? 0f : Float.parseFloat(text)); break;
                    case "StatsList": {
                        List<Boolean> values = new ArrayList<Boolean>();
                        for (org.w3c.dom.Node n = child.getFirstChild(); n != null; n = n.getNextSibling()) {
                            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                            values.add(Boolean.parseBoolean(n.getTextContent().trim()));
                        }
                        boolean[] statsList = new boolean[values.size()];
                        for (int i = 0; i < values.size(); i++) statsList[i] = values.get(i);
                        calcOpts.setStatsList(statsList);
                        break;
                    }
                    case "PriorityList": {
                        LinkedHashMap<EnhanceAbility, Priority> priorityList = new LinkedHashMap<EnhanceAbility, Priority>();
                        for (org.w3c.dom.Node n = child.getFirstChild(); n != null; n = n.getNextSibling()) {
                            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                            org.w3c.dom.Element item = (org.w3c.dom.Element) n;
                            EnhanceAbility ability = null;
                            Priority priority = new Priority();
                            for (org.w3c.dom.Node m = item.getFirstChild(); m != null; m = m.getNextSibling()) {
                                if (m.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                                org.w3c.dom.Element part = (org.w3c.dom.Element) m;
                                if (part.getTagName().equals("key")) {
                                    ability = EnhanceAbility.valueOf(text(part, "EnhanceAbility"));
                                } else if (part.getTagName().equals("value")) {
                                    org.w3c.dom.Element priorityElement = firstElement(part, "Priority");
                                    if (priorityElement == null) priorityElement = part;
                                    for (org.w3c.dom.Node p = priorityElement.getFirstChild(); p != null; p = p.getNextSibling()) {
                                        if (p.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                                        String field = ((org.w3c.dom.Element) p).getTagName();
                                        String value = p.getTextContent().trim();
                                        switch (field) {
                                            case "PriorityName": priority.setPriorityName(value); break;
                                            case "AbilityType": priority.setAbilityType(EnhanceAbility.valueOf(value)); break;
                                            case "Description": priority.setDescription(value); break;
                                            case "PriorityValue": priority.setPriorityValue(value.isEmpty() ? 0 : Integer.parseInt(value)); break;
                                            case "Checked": priority.setChecked(Boolean.parseBoolean(value)); break;
                                            default: break;
                                        }
                                    }
                                }
                            }
                            if (ability != null) priorityList.put(ability, priority);
                        }
                        calcOpts.setPriorityList(priorityList);
                        break;
                    }
                    default: break;
                }
            }
            return calcOpts;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String text(org.w3c.dom.Element parent, String name) {
        for (org.w3c.dom.Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
            if (((org.w3c.dom.Element) n).getTagName().equals(name)) return n.getTextContent().trim();
        }
        return null;
    }

    private static org.w3c.dom.Element firstElement(org.w3c.dom.Element parent, String name) {
        for (org.w3c.dom.Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
            if (((org.w3c.dom.Element) n).getTagName().equals(name)) return (org.w3c.dom.Element) n;
        }
        return null;
    }

    private static final SpecialEffect T10_2P = createT10_2P();
    private static final SpecialEffect T10_4P = createT10_4P();

    private static SpecialEffect createT10_2P() {
        Stats stats = new Stats();
        stats.setBonusDamageMultiplier(.12f);
        return new SpecialEffect(Trigger.Use, stats, 15f, 0f);
    }

    private static SpecialEffect createT10_4P() {
        Stats stats = new Stats();
        return new SpecialEffect(Trigger.Use, stats, 10f, 0);
    }

    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        if (referenceCalculation && significantChange) {
            // Fresh top-level character calculation: drop shared effect interpolation caches so a
            // previous character cannot leak into this one (SUSPECTED_BUGS #15), like
            // Healadin/Elemental/Retribution.
            SpecialEffect.clearInterpolatorCaches();
        }
        //region Applied Stats
        CalculationOptionsEnhance calcOpts = character.getCalculationOptions() instanceof CalculationOptionsEnhance
                ? (CalculationOptionsEnhance) character.getCalculationOptions()
                : null;
        CharacterCalculationsEnhance calculatedStats = new CharacterCalculationsEnhance();
        Stats stats = getCharacterStats(character, additionalItem);
        calculatedStats.setBasicStats(stats);
        calculatedStats.setBuffStats(getBuffsStats(character.getActiveBuffs()));
        Item noBuffs = removeAddedBuffs(calculatedStats.getBuffStats());
        calculatedStats.setEnhSimStats(getCharacterStats(character, noBuffs));
        calculatedStats.setTargetLevel(calcOpts.getTargetLevel());
        calculatedStats.setActiveBuffs(new ArrayList<Buff>(character.getActiveBuffs()));
        float initialAP = stats.getAttackPower();

        // deal with Special Effects - for now add into stats regardless of effect later need to be more precise
        StatsSpecialEffects se = new StatsSpecialEffects(character, stats, calcOpts);
        stats.accumulate(se.getSpecialEffects());
        //Set up some talent variables
        float concussionMultiplier = 1f + .01f * character.getShamanTalents().getConcussion();
        float shieldBonus = 1f + .05f * character.getShamanTalents().getImprovedShields();
        float callofFlameBonus = 1f + .05f * character.getShamanTalents().getCallOfFlame();
        float fireNovaBonus = 1f + .1f * character.getShamanTalents().getImprovedFireNova();
        float mentalQuickness = .1f * character.getShamanTalents().getMentalQuickness();
        float shockBonus = stats.getEnhance4T9() == 1f ? 1.25f : 1f;
        float windfuryWeaponBonus = 1250f + stats.getBonusWFAttackPower();
        float windfuryDamageBonus = 1f;
        switch (character.getShamanTalents().getElementalWeapons()) {
            case 1: windfuryDamageBonus = 1.13f; break;
            case 2: windfuryDamageBonus = 1.27f; break;
            case 3: windfuryDamageBonus = 1.40f; break;
            default: break;
        }
        float weaponMastery = 1f;
        switch (character.getShamanTalents().getWeaponMastery()) {
            case 1: weaponMastery = 1.04f; break;
            case 2: weaponMastery = 1.07f; break;
            case 3: weaponMastery = 1.10f; break;
            default: break;
        }
        float unleashedRage = 0f;
        switch (character.getShamanTalents().getUnleashedRage()) {
            case 1: unleashedRage = .04f; break;
            case 2: unleashedRage = .07f; break;
            case 3: unleashedRage = .10f; break;
            default: break;
        }
        float FTspellpower = (float) Math.floor((float) (211f * (1 + character.getShamanTalents().getElementalWeapons() * .1f)));
        if (calcOpts.getMainhandImbue().equals("Flametongue"))
            stats.setSpellPower(stats.getSpellPower() + FTspellpower);
        if (calcOpts.getOffhandImbue().equals("Flametongue") && character.getShamanTalents().getDualWield() == 1)
            stats.setSpellPower(stats.getSpellPower() + FTspellpower);

        float addedAttackPower = stats.getAttackPower() - initialAP;
        float MQSpellPower = mentalQuickness * addedAttackPower * (1 + stats.getBonusAttackPowerMultiplier());
        // make sure to add in the spellpower from MQ gained from all the bonus AP added in this section
        stats.setSpellPower(stats.getSpellPower() + MQSpellPower * (1 + stats.getBonusSpellPowerMultiplier()));
        // also add in bonus attack power
        stats.setAttackPower(stats.getAttackPower() + addedAttackPower * stats.getBonusAttackPowerMultiplier());
        //endregion

        //region Damage Model
        ////////////////////////////
        // Main calculation Block //
        ////////////////////////////

        CombatStats cs = new CombatStats(character, stats, calcOpts); // calculate the combat stats using modified stats

        // only apply unleashed rage talent if not already applied Unleashed Rage buff.
        if (!character.activeBuffsContains("Unleashed Rage") &&
                !character.activeBuffsContains("Trueshot Aura") &&
                !character.activeBuffsContains("Abomination's Might")) {
            float URattackPower = (calculatedStats.getBuffStats().getBonusAttackPowerMultiplier() == .1f) ? 0f :
                    (stats.getAttackPower() * unleashedRage);
            stats.setAttackPower(stats.getAttackPower() + URattackPower); // no need to multiply by bonus attack power as the whole point is its zero if we need to add Unleashed rage
            stats.setSpellPower(stats.getSpellPower() + mentalQuickness * URattackPower * (1f + stats.getBonusSpellPowerMultiplier()));
        }
        // Tier 10 Bonuses
        if (stats.getEnhance2T10() == 1) {
            calculatedStats.setT10_2Uptime(T10_2P.getAverageUptime(cs.abilityCooldown(EnhanceAbility.ShamanisticRage), 1f) * 100f);
            T10_2P.accumulateAverageStats(stats, cs.abilityCooldown(EnhanceAbility.ShamanisticRage), 1f, 3f, 0f);
        }
        if (stats.getEnhance4T10() == 1) {
            calculatedStats.setT10_4Uptime(T10_4P.getAverageUptime(cs.getSecondsToFiveStack(), .15f) * 100f);
            //T10_4P.AccumulateAverageStats(stats, cs.SecondsToFiveStack, 0.15f);
            stats.setAttackPower(stats.getAttackPower() + stats.getAttackPower() * 0.20f * calculatedStats.getT10_4Uptime() / 100f);
        }

        // assign basic variables for calcs
        float attackPower = stats.getAttackPower();
        float spellPower = stats.getSpellPower();
        ItemInstance mh = character.get(CharacterSlot.MainHand);
        ItemInstance oh = character.get(CharacterSlot.OffHand);
        float wdpsMH = mh == null ? 46.3f : (stats.getWeaponDamage() + (mh.getMinDamage() + mh.getMaxDamage()) / 2f) / mh.getSpeed();
        float wdpsOH = oh == null ? 46.3f : (stats.getWeaponDamage() + (oh.getMinDamage() + oh.getMaxDamage()) / 2f) / oh.getSpeed();
        float AP_SP_Ratio = (spellPower - 274f - 211f) / attackPower;
        float bonusPhysicalDamage = (1f + stats.getBonusDamageMultiplier()) * (1f + stats.getBonusPhysicalDamageMultiplier());
        float bonusFireDamage = (1f + stats.getBonusDamageMultiplier()) * (1f + stats.getBonusFireDamageMultiplier());
        float bonusNatureDamage = (1f + stats.getBonusDamageMultiplier()) * (1f + stats.getBonusNatureDamageMultiplier());
        float bonusLSDamage = 1f + stats.getEnhance2T7(); // 2 piece T7 set bonus
        float Enhance2T8 = 1f + stats.getEnhance2T8() * 0.2f;
        float bonusSSDamage = stats.getBonusSSDamage();
        int baseResistance = Math.max((calcOpts.getTargetLevel() - character.getLevel()) * 5, 0);
        float bossFireResistance = 1f - ((baseResistance + calcOpts.getTargetFireResistance()) / (character.getLevel() * 5f)) * .75f;
        float bossNatureResistance = 1f - ((baseResistance + calcOpts.getTargetNatureResistance()) / (character.getLevel() * 5f)) * .75f;

        //endregion

        //region Individual DPS
        //region Melee DPS
        float APDPS = (attackPower / 14f);
        float adjustedMHDPS = (wdpsMH + APDPS);
        float adjustedOHDPS = 0f;
        float dpsOHMeleeTotal = 0f;
        float dpsMoteOfAnger = 0f;

        float dpsMHMeleeNormal = adjustedMHDPS * cs.getNormalHitModifierMH();
        float dpsMHMeleeCrits = adjustedMHDPS * cs.getCritHitModifierMH();
        float dpsMHMeleeGlances = adjustedMHDPS * cs.getGlancingHitModifier();

        float meleeMultipliers = weaponMastery * cs.getDamageReduction() * bonusPhysicalDamage;
        float dpsMHMeleeTotal = ((dpsMHMeleeNormal + dpsMHMeleeCrits + dpsMHMeleeGlances) * cs.getUnhastedMHSpeed() / cs.getHastedMHSpeed()) * meleeMultipliers;

        if (character.getShamanTalents().getDualWield() == 1 && cs.getHastedOHSpeed() != 0) {
            adjustedOHDPS = (wdpsOH + APDPS) * .5f;
            float dpsOHMeleeNormal = adjustedOHDPS * cs.getNormalHitModifierOH();
            float dpsOHMeleeCrits = adjustedOHDPS * cs.getCritHitModifierOH();
            float dpsOHMeleeGlances = adjustedOHDPS * cs.getGlancingHitModifier();
            dpsOHMeleeTotal = ((dpsOHMeleeNormal + dpsOHMeleeCrits + dpsOHMeleeGlances) * cs.getUnhastedOHSpeed() / cs.getHastedOHSpeed()) * meleeMultipliers;
        }

        // Generic MH & OH damage values used for SS, LL & WF
        float damageMHSwing = adjustedMHDPS * cs.getUnhastedMHSpeed();
        float damageOHSwing = 0f;
        if (character.getShamanTalents().getDualWield() == 1)
            damageOHSwing = adjustedOHDPS * cs.getUnhastedOHSpeed();

        if (character.getShamanTalents().getDualWield() == 1)
            dpsMoteOfAnger = (damageMHSwing + damageOHSwing) / 2 * stats.getMoteOfAnger();
        else
            dpsMoteOfAnger = damageMHSwing * stats.getMoteOfAnger();

        float dpsMelee = dpsMHMeleeTotal + dpsOHMeleeTotal + dpsMoteOfAnger;
        //endregion

        //region Stormstrike DPS
        float dpsSS = 0f;
        float stormstrikeMultiplier = 1f;
        if (character.getShamanTalents().getStormstrike() == 1 && calcOpts.priorityInUse(EnhanceAbility.StormStrike)) {
            stormstrikeMultiplier = 1.2f + (character.getShamanTalents().isGlyphofStormstrike() ? .08f : 0f);
            float swingDPSMH = (damageMHSwing + bonusSSDamage) * cs.getHitsPerSMHSS();
            float swingDPSOH = (damageOHSwing + bonusSSDamage) * cs.getHitsPerSOHSS();
            float SSnormal = (swingDPSMH * cs.getYellowHitModifierMH()) + (swingDPSOH * cs.getYellowHitModifierOH());
            float SScrit = ((swingDPSMH * cs.getYellowCritModifierMH()) + (swingDPSOH * cs.getYellowCritModifierOH())) * cs.getCritMultiplierMelee();
            dpsSS = (SSnormal + SScrit) * cs.getDamageReduction() * weaponMastery * bonusNatureDamage * Enhance2T8 * bossNatureResistance;
        }
        //endregion

        //region Lavalash DPS
        float dpsLL = 0f;
        if (character.getShamanTalents().getLavaLash() == 1 && character.getShamanTalents().getDualWield() == 1 && calcOpts.priorityInUse(EnhanceAbility.LavaLash)) {
            float lavalashDPS = damageOHSwing * cs.getHitsPerSLL();
            float LLnormal = lavalashDPS * cs.getYellowHitModifierOH();
            float LLcrit = lavalashDPS * cs.getYellowCritModifierOH() * cs.getCritMultiplierMelee();
            dpsLL = (LLnormal + LLcrit) * bonusFireDamage * Enhance2T8 * bossFireResistance; //and no armor reduction yeya!
            if (calcOpts.getOffhandImbue().equals("Flametongue")) {  // 25% bonus dmg if FT imbue in OH
                if (character.getShamanTalents().isGlyphofLavaLash())
                    dpsLL *= 1.25f * 1.1f; // +10% bonus dmg if Lava Lash Glyph
                else
                    dpsLL *= 1.25f;
            }
        }
        //endregion

        //region Earth Shock DPS
        float dpsES = 0f;
        if (calcOpts.priorityInUse(EnhanceAbility.EarthShock)) {
            float damageESBase = 872f;
            float coefES = .3858f;
            float damageES = stormstrikeMultiplier * concussionMultiplier * (damageESBase + coefES * spellPower);
            float shockdps = damageES / cs.abilityCooldown(EnhanceAbility.EarthShock);
            float shockNormal = shockdps * cs.getSpellHitModifier();
            float shockCrit = shockdps * cs.getSpellCritModifier() * cs.getCritMultiplierSpell();
            dpsES = (shockNormal + shockCrit) * bonusNatureDamage * bossNatureResistance * shockBonus;
        }
        //endregion

        //region Flame Shock DPS
        float dpsFS = 0f;
        if (calcOpts.priorityInUse(EnhanceAbility.FlameShock)) {
            float damageFSBase = 500f;
            float damageFSDoTBase = 834f;
            float coefFS = 1.5f / 3.5f / 2f;
            float coefFSDoT = .6f;
            float damageFS = (damageFSBase + coefFS * spellPower) * concussionMultiplier;
            float damageFTDoT = (damageFSDoTBase + coefFSDoT * spellPower) * concussionMultiplier;
            float usesCooldown = cs.abilityCooldown(EnhanceAbility.FlameShock);
            float flameShockdps = damageFS / usesCooldown;
            float flameShockDoTdps = damageFTDoT / usesCooldown;
            float flameShockNormal = (flameShockdps + flameShockDoTdps) * cs.getSpellHitModifier();
            float flameShockCrit = (flameShockdps + flameShockDoTdps) * cs.getSpellCritModifier() * cs.getCritMultiplierSpell();
            if (character.getShamanTalents().isGlyphofFlameShock()) {
                float critMultiplierSpell = (1.5f + .1f * character.getShamanTalents().getElementalFury() + 0.6f) * (1 + stats.getBonusSpellCritMultiplier());
                flameShockCrit = (flameShockdps + flameShockDoTdps) * cs.getSpellCritModifier() * critMultiplierSpell;
            }
            dpsFS = (flameShockNormal + flameShockCrit) * bonusFireDamage * bossFireResistance * shockBonus;
        }
        //endregion

        //region Lightning Bolt DPS
        float dpsLB = 0f;
        if (calcOpts.priorityInUse(EnhanceAbility.LightningBolt)) {
            float damageLBBase = 765f;
            float coefLB = .7143f;
            // LightningSpellPower is for totem of hex/the void/ancestral guidance
            float damageLB = stormstrikeMultiplier * concussionMultiplier * (damageLBBase + coefLB * (spellPower + stats.getLightningSpellPower()));
            float lbdps = damageLB / cs.abilityCooldown(EnhanceAbility.LightningBolt);
            float lbNormal = lbdps * cs.getLBHitModifier();
            float lbCrit = lbdps * cs.getLBCritModifier() * cs.getCritMultiplierSpell();
            dpsLB = (lbNormal + lbCrit) * bonusNatureDamage * bossNatureResistance;
            if (character.getShamanTalents().isGlyphofLightningBolt())
                dpsLB *= 1.04f; // 4% bonus dmg if Lightning Bolt Glyph
        }
        //endregion

        //region Windfury DPS
        float dpsWF = 0f;
        if (calcOpts.getMainhandImbue().equals("Windfury")) {
            float damageWFHit = damageMHSwing + (windfuryWeaponBonus / 14 * cs.getUnhastedMHSpeed());
            float WFdps = damageWFHit * cs.getHitsPerSWF();
            float WFnormal = WFdps * cs.getYellowHitModifierMH();
            float WFcrit = WFdps * cs.getYellowCritModifierMH() * cs.getCritMultiplierMelee();
            dpsWF = (WFnormal + WFcrit) * weaponMastery * cs.getDamageReduction() * bonusPhysicalDamage * windfuryDamageBonus;
        }
        //endregion

        //region Lightning Shield DPS
        float dpsLS = 0f;
        if (calcOpts.priorityInUse(EnhanceAbility.LightningShield)) {
            float damageLSBase = 380f;
            float damageLSCoef = 0.267f; // co-efficient from EnhSim
            float damageLS = stormstrikeMultiplier * shieldBonus * (damageLSBase + damageLSCoef * spellPower);
            // no crit needed as LS can't crit
            dpsLS = cs.getChanceSpellHit() * cs.getStaticShockProcsPerS() * damageLS * bonusNatureDamage * bonusLSDamage * bossNatureResistance;
            if (character.getShamanTalents().isGlyphofLightningShield())
                dpsLS *= 1.2f; // 20% bonus dmg if Lightning Shield Glyph
        }
        //endregion

        //region Fire Totem DPS
        float dpsFireTotem = 0f;
        if (calcOpts.priorityInUse(EnhanceAbility.MagmaTotem)) {
            float damageFireTotem = (371f + .1f * spellPower) * callofFlameBonus;
            float FireTotemdps = damageFireTotem / 2f * cs.getFireTotemUptime();
            float FireTotemNormal = FireTotemdps * cs.getSpellHitModifier();
            float FireTotemCrit = FireTotemdps * cs.getSpellCritModifier() * cs.getCritMultiplierSpell();
            dpsFireTotem = (FireTotemNormal + FireTotemCrit) * bonusFireDamage * bossFireResistance * cs.getMultiTargetMultiplier();
        } else if (calcOpts.priorityInUse(EnhanceAbility.SearingTotem)) {
            float damageFireTotem = (105f + .1667f * spellPower) * callofFlameBonus;
            float FireTotemdps = damageFireTotem / 2f * cs.getFireTotemUptime();
            float FireTotemNormal = FireTotemdps * cs.getSpellHitModifier();
            float FireTotemCrit = FireTotemdps * cs.getSpellCritModifier() * cs.getCritMultiplierSpell();
            dpsFireTotem = (FireTotemNormal + FireTotemCrit) * bonusFireDamage * bossFireResistance;
        }
        dpsFireTotem *= (1f - cs.getFireElementalUptime());
        //endregion

        //region Fire Nova DPS
        float dpsFireNova = 0f;
        if (calcOpts.priorityInUse(EnhanceAbility.FireNova)) {
            float damageFireNova = (945.0f + 0.2142f * spellPower) * callofFlameBonus * fireNovaBonus;
            float FireNovadps = (damageFireNova / cs.abilityCooldown(EnhanceAbility.FireNova)) * cs.getFireTotemUptime();
            float FireNovaNormal = FireNovadps * cs.getSpellHitModifier();
            float FireNovaCrit = FireNovadps * cs.getSpellCritModifier() * cs.getCritMultiplierSpell();
            dpsFireNova = (FireNovaNormal + FireNovaCrit) * bonusFireDamage * bossFireResistance * cs.getMultiTargetMultiplier();
        }
        //endregion

        //region Flametongue Weapon DPS
        float dpsFT = 0f;
        if (calcOpts.getMainhandImbue().equals("Flametongue")) {
            float damageFTBase = 274f * cs.getUnhastedMHSpeed() / 4.0f;
            float damageFTCoef = 0.03811f * cs.getUnhastedMHSpeed();
            float damageFT = damageFTBase + damageFTCoef * spellPower;
            float FTdps = damageFT * cs.getHitsPerSMH();
            float FTNormal = FTdps * cs.getSpellHitModifier();
            float FTCrit = FTdps * cs.getSpellCritModifier() * cs.getCritMultiplierSpell();
            dpsFT += (FTNormal + FTCrit) * bonusFireDamage * bossFireResistance;
        }
        if (calcOpts.getOffhandImbue().equals("Flametongue") && character.getShamanTalents().getDualWield() == 1) {
            float damageFTBase = 274f * cs.getUnhastedOHSpeed() / 4.0f;
            float damageFTCoef = 0.03811f * cs.getUnhastedOHSpeed();
            float damageFT = damageFTBase + damageFTCoef * spellPower;
            float FTdps = damageFT * (cs.getHitsPerSOH() - cs.getHitsPerSLL());
            float FTNormal = FTdps * cs.getSpellHitModifier();
            float FTCrit = FTdps * cs.getSpellCritModifier() * cs.getCritMultiplierSpell();
            dpsFT += (FTNormal + FTCrit) * bonusFireDamage * bossFireResistance;
        }
        //endregion

        //region Pet calculations
        // needed for pets - spirit wolves and Fire Elemental
        boolean critDebuff = character.activeBuffsContains("Heart of the Crusader") ||
                character.activeBuffsContains("Master Poisioner") ||
                character.activeBuffsContains("Totem of Wrath");
        boolean critBuff = character.activeBuffsContains("Leader of the Pack") ||
                character.activeBuffsContains("Rampage");
        float critbuffs = (critDebuff ? 0.03f : 0f) + (critBuff ? 0.05f : 0f);
        float meleeHitBonus = stats.getPhysicalHit() + StatConversion.getHitFromRating(stats.getHitRating()) + .02f * character.getShamanTalents().getDualWieldSpecialization();
        float petMeleeMissRate = Math.max(0f, StatConversion.WHITE_MISS_CHANCE_CAP[calcOpts.getTargetLevel() - 80] - meleeHitBonus) + cs.getAverageDodge();
        float petMeleeMultipliers = cs.getDamageReduction() * bonusPhysicalDamage;
        //endregion

        //region Doggies!
        // TTT article suggests 300-450 dps while the dogs are up plus 30% of AP
        // my analysis reveals they get 31% of shaman AP + 2 * their STR and base 206.17 dps.
        float dpsDogs = 0f;
        if (character.getShamanTalents().getFeralSpirit() == 1 && calcOpts.priorityInUse(EnhanceAbility.FeralSpirits)) {
            float FSglyphAP = character.getShamanTalents().isGlyphofFeralSpirit() ? attackPower * .3f : 0f;
            float soeBuff = character.activeBuffsContains("Strength of Earth Totem") ? 155f : 0f;
            float enhTotems = character.activeBuffsContains("Enhancing Totems (Agility/Strength)") ? 23f : 0f;
            float dogsStr = 331f + soeBuff + enhTotems; // base str = 331 plus SoE totem if active giving extra 178 str buff
            float dogsAgi = 113f + soeBuff + enhTotems;
            float dogsAP = ((dogsStr * 2f - 20f) + .31f * attackPower + FSglyphAP) * (1f + unleashedRage);
            float dogsCrit = (StatConversion.getCritFromAgility(dogsAgi, CharacterClass.Shaman) + critbuffs) * (1 + stats.getBonusCritMultiplier());
            float dogsBaseSpeed = 1.5f;
            float dogsHitsPerS = 1f / (dogsBaseSpeed / (1f + stats.getPhysicalHaste()));
            float dogsBaseDamage = (206.17f + dogsAP / 14f) * dogsBaseSpeed;

            float dogsMeleeNormal = dogsBaseDamage * (1 - petMeleeMissRate - dogsCrit - cs.getGlancingRate());
            float dogsMeleeCrits = dogsBaseDamage * dogsCrit * cs.getCritMultiplierMelee();
            float dogsMeleeGlances = dogsBaseDamage * cs.getGlancingHitModifier();

            float dogsTotalDamage = dogsMeleeNormal + dogsMeleeCrits + dogsMeleeGlances;

            dpsDogs = 2 * (45f / 180f) * dogsTotalDamage * dogsHitsPerS * petMeleeMultipliers;
            calculatedStats.setSpiritWolf(new DPSAnalysis(dpsDogs, petMeleeMissRate, cs.getAverageDodge(), cs.getGlancingRate(), dogsCrit, 60f / cs.abilityCooldown(EnhanceAbility.FeralSpirits)));
        } else {
            calculatedStats.setSpiritWolf(new DPSAnalysis(0, 0, 0, 0, 0, 0));
        }
        //endregion

        //region Fire Elemental
        if (calcOpts.priorityInUse(EnhanceAbility.FireElemental)) {
            float spellHitBonus = stats.getSpellHit() + StatConversion.getHitFromRating(stats.getHitRating());
            float petSpellMissRate = Math.max(0f, StatConversion.WHITE_MISS_CHANCE_CAP[calcOpts.getTargetLevel() - 80] - spellHitBonus);
            float petSpellMultipliers = bonusFireDamage * bossFireResistance * callofFlameBonus;
            float petCritRate = critbuffs * (1 + stats.getBonusCritMultiplier());
            calculatedStats.setFireElemental(new FireElemental(attackPower, spellPower, stats.getIntellect(), cs,
                    petCritRate, petMeleeMissRate, petMeleeMultipliers, petSpellMissRate, petSpellMultipliers));
        } else
            calculatedStats.setFireElemental(new FireElemental(0, 0, 0, cs, 0, 0, 0, 0, 0));
        float dpsFireElemental = calculatedStats.getFireElemental().getDPS();
        //endregion

        //region Set CalculatedStats
        calculatedStats.setDPSPoints(dpsMelee + dpsSS + dpsLL + dpsES + dpsFS + dpsLB + dpsWF + dpsLS + dpsFireTotem + dpsFireNova + dpsFT + dpsDogs + dpsFireElemental);
        calculatedStats.setSurvivabilityPoints(stats.getHealth() * 0.02f);
        calculatedStats.setOverallPoints(calculatedStats.getDPSPoints() + calculatedStats.getSurvivabilityPoints());
        calculatedStats.setDodgedAttacks(cs.getAverageDodge() * 100f);
        calculatedStats.setParriedAttacks(cs.getAverageParry() * 100f);
        calculatedStats.setMissedAttacks((1 - cs.getAverageWhiteHitChance()) * 100f);
        calculatedStats.setAvoidedAttacks(calculatedStats.getMissedAttacks() + calculatedStats.getDodgedAttacks() + calculatedStats.getParriedAttacks());
        calculatedStats.setYellowHit((float) Math.floor((float) (cs.getAverageYellowHitChance() * 10000f)) / 100f);
        calculatedStats.setSpellHit((float) Math.floor((float) (cs.getChanceSpellHit() * 10000f)) / 100f);
        calculatedStats.setOverSpellHitCap((float) Math.floor((float) (cs.getOverSpellHitCap() * 10000f)) / 100f);
        calculatedStats.setOverMeleeCritCap((float) Math.floor((float) (cs.getOverMeleeCritCap() * 10000f)) / 100f);
        calculatedStats.setWhiteHit((float) Math.floor((float) (cs.getAverageWhiteHitChance() * 10000f)) / 100f);
        calculatedStats.setMeleeCrit((float) Math.floor((float) ((cs.getDisplayMeleeCrit())) * 10000f) / 100f);
        calculatedStats.setYellowCrit((float) Math.floor((float) ((cs.getDisplayYellowCrit())) * 10000f) / 100f);
        calculatedStats.setSpellCrit((float) Math.floor((float) (cs.getChanceSpellCrit() * 10000f)) / 100f);
        calculatedStats.setGlancingBlows(cs.getGlancingRate() * 100f);
        calculatedStats.setArmorMitigation((1f - cs.getDamageReduction()) * 100f);
        calculatedStats.setAttackPower(attackPower);
        calculatedStats.setSpellPower(spellPower);
        calculatedStats.setAvMHSpeed(cs.getHastedMHSpeed());
        calculatedStats.setAvOHSpeed(cs.getHastedOHSpeed());
        calculatedStats.setEDBonusCrit(cs.getEDBonusCrit() * 100f);
        calculatedStats.setEDUptime(cs.getEDUptime() * 100f);
        calculatedStats.setFlurryUptime(cs.getFlurryUptime() * 100f);
        calculatedStats.setSecondsTo5Stack(cs.getSecondsToFiveStack());
        calculatedStats.setMHEnchantUptime(se.getMHUptime() * 100f);
        calculatedStats.setOHEnchantUptime(se.getOHUptime() * 100f);
        calculatedStats.setTrinket1Uptime(se.getUptime(character.get(CharacterSlot.Trinket1)) * 100f);
        calculatedStats.setTrinket2Uptime(se.getUptime(character.get(CharacterSlot.Trinket2)) * 100f);
        calculatedStats.setFireTotemUptime(cs.getFireTotemUptime() * 100f);

        calculatedStats.setTotalExpertiseMH((float) Math.floor(cs.getExpertiseBonusMH() * 400f));
        calculatedStats.setTotalExpertiseOH((float) Math.floor(cs.getExpertiseBonusOH() * 400f));

        calculatedStats.setSwingDamage(new DPSAnalysis(dpsMelee, 1 - cs.getAverageWhiteHitChance(), cs.getAverageDodge(), cs.getGlancingRate(), cs.getAverageWhiteCritChance(), cs.getMeleePPM()));
        calculatedStats.setStormstrike(new DPSAnalysis(dpsSS, 1 - cs.getAverageYellowHitChance(), cs.getAverageDodge(), -1, cs.getAverageYellowCritChance(), 60f / cs.abilityCooldown(EnhanceAbility.StormStrike)));
        calculatedStats.setLavaLash(new DPSAnalysis(dpsLL, 1 - cs.getChanceYellowHitOH(), cs.getChanceDodgeOH(), -1, cs.getChanceYellowCritOH(), 60f / cs.abilityCooldown(EnhanceAbility.LavaLash)));
        calculatedStats.setEarthShock(new DPSAnalysis(dpsES, 1 - cs.getChanceSpellHit(), -1, -1, cs.getChanceSpellCrit(), 60f / cs.abilityCooldown(EnhanceAbility.EarthShock)));
        calculatedStats.setFlameShock(new DPSAnalysis(dpsFS, 1 - cs.getChanceSpellHit(), -1, -1, cs.getChanceSpellCrit(), 60f / cs.abilityCooldown(EnhanceAbility.FlameShock)));
        calculatedStats.setLightningBolt(new DPSAnalysis(dpsLB, 1 - cs.getChanceSpellHit(), -1, -1, cs.getChanceSpellCrit(), 60f / cs.abilityCooldown(EnhanceAbility.LightningBolt)));
        calculatedStats.setWindfuryAttack(new DPSAnalysis(dpsWF, 1 - cs.getChanceYellowHitMH(), cs.getChanceDodgeMH(), -1, cs.getChanceYellowCritMH(), cs.getWFPPM()));
        calculatedStats.setLightningShield(new DPSAnalysis(dpsLS, 1 - cs.getChanceSpellHit(), -1, -1, -1, 60f / cs.abilityCooldown(EnhanceAbility.LightningShield)));
        calculatedStats.setSearingMagma(new DPSAnalysis(dpsFireTotem, 1 - cs.getChanceSpellHit(), -1, -1, cs.getChanceSpellCrit(),
                calcOpts.getMagma() ? 60f / cs.abilityCooldown(EnhanceAbility.MagmaTotem) : 60f / cs.abilityCooldown(EnhanceAbility.SearingTotem)));
        calculatedStats.setFlameTongueAttack(new DPSAnalysis(dpsFT, 1 - cs.getChanceSpellHit(), -1, -1, cs.getChanceSpellCrit(), cs.getFTPPM()));
        calculatedStats.setFireNova(new DPSAnalysis(dpsFireNova, 1 - cs.getChanceSpellHit(), -1, -1, cs.getChanceSpellCrit(), 60f / cs.abilityCooldown(EnhanceAbility.FireNova)));

        calculatedStats.setVersion(VERSION);
        return calculatedStats;
        //endregion
    }

    //region Get Character Stats
    public Stats getCharacterStats(Character character, Item additionalItem) {
        Stats statsBase = BaseStats.getBaseStats(character);
        Stats statsBaseGear = new Stats();
        accumulateItemStats(statsBaseGear, character, additionalItem);
        Stats statsBuffs = getBuffsStats(character.getActiveBuffs());
        Stats statsGearEnchantsBuffs = Stats.add(statsBaseGear, statsBuffs);

        int AK = character.getShamanTalents().getAncestralKnowledge();
        float agiBase = (float) Math.floor((float) (statsBase.getAgility()));
        float agiBonus = (float) Math.floor((float) (statsGearEnchantsBuffs.getAgility()));
        float strBase = (float) Math.floor((float) (statsBase.getStrength()));
        float strBonus = (float) Math.floor((float) (statsGearEnchantsBuffs.getStrength()));
        float intBase = (float) Math.floor((float) (statsBase.getIntellect() * (1f + .02f * AK)));
        float intBonus = (float) Math.floor((float) (statsGearEnchantsBuffs.getIntellect() * (1f + .02f * AK)));
        float staBase = (float) Math.floor((float) (statsBase.getStamina()));
        float staBonus = (float) Math.floor((float) (statsGearEnchantsBuffs.getStamina()));
        float spiBase = (float) Math.floor((float) (statsBase.getSpirit()));
        float spiBonus = (float) Math.floor((float) (statsGearEnchantsBuffs.getSpirit()));

        Stats statsTotal = getRelevantStats(Stats.add(statsBase, statsGearEnchantsBuffs));
        statsTotal.setBonusIntellectMultiplier(((1 + statsBase.getBonusIntellectMultiplier()) * (1 + statsGearEnchantsBuffs.getBonusIntellectMultiplier())) - 1);
        statsTotal.setBonusSpiritMultiplier(((1 + statsBase.getBonusSpiritMultiplier()) * (1 + statsGearEnchantsBuffs.getBonusSpiritMultiplier())) - 1);
        statsTotal.setBonusAgilityMultiplier(((1 + statsBase.getBonusAgilityMultiplier()) * (1 + statsGearEnchantsBuffs.getBonusAgilityMultiplier())) - 1);
        statsTotal.setBonusStrengthMultiplier(((1 + statsBase.getBonusStrengthMultiplier()) * (1 + statsGearEnchantsBuffs.getBonusStrengthMultiplier())) - 1);
        statsTotal.setBonusStaminaMultiplier(((1 + statsBase.getBonusStaminaMultiplier()) * (1 + statsGearEnchantsBuffs.getBonusStaminaMultiplier())) - 1);
        statsTotal.setBonusAttackPowerMultiplier(((1 + statsBase.getBonusAttackPowerMultiplier()) * (1 + statsGearEnchantsBuffs.getBonusAttackPowerMultiplier())) - 1);
        statsTotal.setBonusSpellPowerMultiplier(((1 + statsBase.getBonusSpellPowerMultiplier()) * (1 + statsGearEnchantsBuffs.getBonusSpellPowerMultiplier())) - 1);
        statsTotal.setBonusCritMultiplier(((1 + statsBase.getBonusCritMultiplier()) * (1 + statsGearEnchantsBuffs.getBonusCritMultiplier())) - 1);
        statsTotal.setBonusSpellCritMultiplier(((1 + statsBase.getBonusSpellCritMultiplier()) * (1 + statsGearEnchantsBuffs.getBonusSpellCritMultiplier())) - 1);
        statsTotal.setBonusPhysicalDamageMultiplier(((1 + statsBase.getBonusPhysicalDamageMultiplier()) * (1 + statsGearEnchantsBuffs.getBonusPhysicalDamageMultiplier())) - 1);
        statsTotal.setBonusNatureDamageMultiplier(((1 + statsBase.getBonusNatureDamageMultiplier()) * (1 + statsGearEnchantsBuffs.getBonusNatureDamageMultiplier())) - 1);
        statsTotal.setBonusFireDamageMultiplier(((1 + statsBase.getBonusFireDamageMultiplier()) * (1 + statsGearEnchantsBuffs.getBonusFireDamageMultiplier())) - 1);
        statsTotal.setBonusHealthMultiplier(((1 + statsBase.getBonusHealthMultiplier()) * (1 + statsGearEnchantsBuffs.getBonusHealthMultiplier())) - 1);
        statsTotal.setBonusManaMultiplier(((1 + statsBase.getBonusManaMultiplier()) * (1 + statsGearEnchantsBuffs.getBonusManaMultiplier())) - 1);
        statsTotal.setPhysicalHaste(((1 + statsBase.getPhysicalHaste()) * (1 + statsGearEnchantsBuffs.getPhysicalHaste())) - 1);
        statsTotal.setSpellHaste(((1 + statsBase.getSpellHaste()) * (1 + statsGearEnchantsBuffs.getSpellHaste())) - 1);

        statsTotal.setAgility((float) Math.floor((float) ((agiBase + agiBonus) * (1 + statsTotal.getBonusAgilityMultiplier()))));
        statsTotal.setStrength((float) Math.floor((float) ((strBase + strBonus) * (1 + statsTotal.getBonusStrengthMultiplier()))));
        statsTotal.setStamina((float) Math.floor((float) ((staBase + staBonus) * (1 + statsTotal.getBonusStaminaMultiplier()))));
        statsTotal.setIntellect((float) Math.floor((float) ((intBase + intBonus) * (1 + statsTotal.getBonusIntellectMultiplier()))));
        statsTotal.setSpirit((float) Math.floor((float) ((spiBase + spiBonus) * (1 + statsTotal.getBonusSpiritMultiplier()))));
        statsTotal.setHealth(statsBase.getHealth() + statsGearEnchantsBuffs.getHealth() + StatConversion.getHealthFromStamina(statsTotal.getStamina()));
        statsTotal.setMana(statsBase.getMana() + statsGearEnchantsBuffs.getMana() + StatConversion.getManaFromIntellect(statsTotal.getIntellect()));
        statsTotal.setHealth((float) Math.floor((float) (statsTotal.getHealth() * (1 + statsTotal.getBonusHealthMultiplier()))));
        statsTotal.setMana((float) Math.floor((float) (statsTotal.getMana() * (1 + statsTotal.getBonusManaMultiplier()))));
        statsTotal.setExpertise(statsTotal.getExpertise() + 3 * character.getShamanTalents().getUnleashedRage());

        float intBonusToAP = 0.0f;
        switch (character.getShamanTalents().getMentalDexterity()) {
            case 1:
                intBonusToAP = .33f * statsTotal.getIntellect();
                break;
            case 2:
                intBonusToAP = .66f * statsTotal.getIntellect();
                break;
            case 3:
                intBonusToAP = 1f * statsTotal.getIntellect();
                break;
            default: break;
        }
        statsTotal.setAttackPower(statsTotal.getAttackPower() + statsTotal.getStrength() + statsTotal.getAgility() + intBonusToAP);
        statsTotal.setAttackPower(statsTotal.getAttackPower() * (1f + statsTotal.getBonusAttackPowerMultiplier()));
        float SPfromAP = statsTotal.getAttackPower() * .1f * character.getShamanTalents().getMentalQuickness();
        statsTotal.setSpellPower(statsTotal.getSpellPower() + SPfromAP);

        statsTotal.setSpellPower(statsTotal.getSpellPower() * (1f + statsTotal.getBonusSpellPowerMultiplier()));

        return statsTotal;
    }
    //endregion

    //region Buff Functions
    public void setDefaults(Character character) {
        // add shaman buffs
        character.activeBuffsAdd("Strength of Earth Totem");
        character.activeBuffsAdd("Heroism/Bloodlust");
        character.activeBuffsAdd("Windfury Totem");

        if (character.getShamanTalents().getImprovedWindfuryTotem() == 2)
            character.activeBuffsAdd("Improved Windfury Totem");
        if (character.getShamanTalents().getEnhancingTotems() == 3) {
            character.activeBuffsAdd("Enhancing Totems (Agility/Strength)"); // add both the Agi Str one
        }

        // add other raid buffs
        character.activeBuffsAdd("Blessing of Wisdom");
        character.activeBuffsAdd("Improved Blessing of Wisdom");
        character.activeBuffsAdd("Blessing of Might");
        character.activeBuffsAdd("Improved Blessing of Might");
        character.activeBuffsAdd("Sanctified Retribution");
        character.activeBuffsAdd("Blessing of Sanctuary");
        character.activeBuffsAdd("Swift Retribution");
        character.activeBuffsAdd("Arcane Intellect");
        character.activeBuffsAdd("Unleashed Rage");
        character.activeBuffsAdd("Commanding Shout");
        character.activeBuffsAdd("Commanding Presence (Health)");
        character.activeBuffsAdd("Leader of the Pack");
        character.activeBuffsAdd("Elemental Oath");
        character.activeBuffsAdd("Wrath of Air Totem");
        character.activeBuffsAdd("Totem of Wrath (Spell Power)");
        character.activeBuffsAdd("Divine Spirit");
        character.activeBuffsAdd("Power Word: Fortitude");
        character.activeBuffsAdd("Improved Power Word: Fortitude");
        character.activeBuffsAdd("Mark of the Wild");
        character.activeBuffsAdd("Improved Mark of the Wild");
        character.activeBuffsAdd("Blessing of Kings");
        character.activeBuffsAdd("Blessing of Kings (Str/Sta Bonus)");
        character.activeBuffsAdd("Sunder Armor");
        character.activeBuffsAdd("Faerie Fire");
        character.activeBuffsAdd("Heart of the Crusader");
        character.activeBuffsAdd("Blood Frenzy");
        character.activeBuffsAdd("Improved Scorch");
        character.activeBuffsAdd("Curse of the Elements");
        character.activeBuffsAdd("Misery");
        character.activeBuffsAdd("Hunting Party");
        character.activeBuffsAdd("Judgement of Wisdom");
        character.activeBuffsAdd("Flask of Endless Rage");
        character.activeBuffsAdd("Potion of Speed");
        character.activeBuffsAdd("Fish Feast");

        if (character.hasProfession(rawr.base.character.Profession.Alchemy)) {
            character.activeBuffsAdd(("Flask of Endless Rage (Mixology)"));
        }

        character.setEnforceGemRequirements(true); // set default to be true for Enhancement Shaman
    }

    private Item removeAddedBuffs(Stats addedBuffs) {
        Item result = new Item();
        result.setStats(Stats.multiply(addedBuffs, -1));
        // to this point works fine for additive stats doesn't work for multiplicative ones.
        Stats s = result.getStats();
        s.setBonusAgilityMultiplier(1 / (1 - s.getBonusAgilityMultiplier()) - 1);
        s.setBonusStrengthMultiplier(1 / (1 - s.getBonusStrengthMultiplier()) - 1);
        s.setBonusIntellectMultiplier(1 / (1 - s.getBonusIntellectMultiplier()) - 1);
        s.setBonusSpiritMultiplier(1 / (1 - s.getBonusSpiritMultiplier()) - 1);
        s.setBonusAttackPowerMultiplier(1 / (1 - s.getBonusAttackPowerMultiplier()) - 1);
        s.setBonusSpellPowerMultiplier(1 / (1 - s.getBonusSpellPowerMultiplier()) - 1);
        s.setBonusCritMultiplier(1 / (1 - s.getBonusCritMultiplier()) - 1);
        s.setBonusSpellCritMultiplier(1 / (1 - s.getBonusSpellCritMultiplier()) - 1);
        s.setBonusDamageMultiplier(1 / (1 - s.getBonusDamageMultiplier()) - 1);
        s.setBonusPhysicalDamageMultiplier(1 / (1 - s.getBonusPhysicalDamageMultiplier()) - 1);
        s.setBonusFireDamageMultiplier(1 / (1 - s.getBonusFireDamageMultiplier()) - 1);
        s.setBonusNatureDamageMultiplier(1 / (1 - s.getBonusNatureDamageMultiplier()) - 1);
        s.setBonusHealthMultiplier(1 / (1 - s.getBonusHealthMultiplier()) - 1);
        s.setBonusManaMultiplier(1 / (1 - s.getBonusManaMultiplier()) - 1);
        s.setPhysicalHaste(1 / (1 - s.getPhysicalHaste()) - 1);
        s.setSpellHaste(1 / (1 - s.getSpellHaste()) - 1);
        return result;
    }
    //endregion

    //region RelevantGlyphs
    private static List<String> relevantGlyphs;

    public List<String> getRelevantGlyphs() {
        if (relevantGlyphs == null) {
            relevantGlyphs = new ArrayList<String>();
            relevantGlyphs.add("Glyph of Feral Spirit");
            relevantGlyphs.add("Glyph of Flametongue Weapon");
            relevantGlyphs.add("Glyph of Lava Lash");
            relevantGlyphs.add("Glyph of Lightning Shield");
            relevantGlyphs.add("Glyph of Lightning Bolt");
            relevantGlyphs.add("Glyph of Shocking");
            relevantGlyphs.add("Glyph of Flame Shock");
            relevantGlyphs.add("Glyph of Stormstrike");
            relevantGlyphs.add("Glyph of Windfury Weapon");
            //             relevantGlyphs.Add("Glyph of Fire Nova");
            relevantGlyphs.add("Glyph of Fire Elemental Totem");
        }
        return relevantGlyphs;
    }
    //endregion

    //region Relevant Stats
    private List<ItemType> relevantItemTypes = null;

    @Override
    public List<ItemType> getRelevantItemTypes() {
        if (this.relevantItemTypes == null) {
            this.relevantItemTypes = new ArrayList<ItemType>(List.of(
                    ItemType.None,
                    ItemType.Cloth,
                    ItemType.Leather,
                    ItemType.Mail,
                    ItemType.Totem,
                    //                  ItemType.Staff,
                    //                  ItemType.TwoHandMace, // Removed two handed options so as not to screw up recommendations
                    //                  ItemType.TwoHandAxe,  // Two handers are simply NOT viable for Enhancement Shamans
                    ItemType.Dagger,
                    ItemType.OneHandAxe,
                    ItemType.OneHandMace,
                    ItemType.FistWeapon));
        }
        return this.relevantItemTypes;
    }

    @Override
    public boolean isItemRelevant(Item item) {
        if ((item.getSlot() == ItemSlot.Ranged && item.getType() != ItemType.Totem))
            return false;
        if (item.getSlot() == ItemSlot.OffHand && item.getType() == ItemType.None)
            return false;
        return super.isItemRelevant(item);
    }

    public Stats getRelevantStats(Stats stats) {
        Stats s = new Stats();
        s.setAgility(stats.getAgility());
        s.setStrength(stats.getStrength());
        s.setIntellect(stats.getIntellect());
        s.setSpirit(stats.getSpirit());
        s.setAttackPower(stats.getAttackPower());
        s.setCritRating(stats.getCritRating());
        s.setHitRating(stats.getHitRating());
        s.setStamina(stats.getStamina());
        s.setHasteRating(stats.getHasteRating());
        s.setExpertise(stats.getExpertise());
        s.setExpertiseRating(stats.getExpertiseRating());
        s.setArmorPenetration(stats.getArmorPenetration());
        s.setArmorPenetrationRating(stats.getArmorPenetrationRating());
        s.setWeaponDamage(stats.getWeaponDamage());
        s.setBonusAgilityMultiplier(stats.getBonusAgilityMultiplier());
        s.setBonusAttackPowerMultiplier(stats.getBonusAttackPowerMultiplier());
        s.setBonusCritMultiplier(stats.getBonusCritMultiplier());
        s.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());
        s.setBonusStrengthMultiplier(stats.getBonusStrengthMultiplier());
        s.setBonusIntellectMultiplier(stats.getBonusIntellectMultiplier());
        s.setBonusSpiritMultiplier(stats.getBonusSpiritMultiplier());
        s.setBonusSpellPowerMultiplier(stats.getBonusSpellPowerMultiplier());
        s.setBonusSpellCritMultiplier(stats.getBonusSpellCritMultiplier());
        s.setBonusPhysicalDamageMultiplier(stats.getBonusPhysicalDamageMultiplier());
        s.setBonusNatureDamageMultiplier(stats.getBonusNatureDamageMultiplier());
        s.setBonusFireDamageMultiplier(stats.getBonusFireDamageMultiplier());
        s.setBonusShadowDamageMultiplier(stats.getBonusShadowDamageMultiplier());
        s.setBonusHealthMultiplier(stats.getBonusHealthMultiplier());
        s.setBonusManaMultiplier(stats.getBonusManaMultiplier());
        s.setHealth(stats.getHealth());
        s.setMana(stats.getMana());
        s.setSpellPower(stats.getSpellPower());
        s.setLightningSpellPower(stats.getLightningSpellPower());
        s.setBonusSSDamage(stats.getBonusSSDamage());
        s.setBonusWFAttackPower(stats.getBonusWFAttackPower());
        s.setHighestStat(stats.getHighestStat());
        s.setParagon(stats.getParagon());
        s.setEnhance2T7(stats.getEnhance2T7());
        s.setEnhance4T7(stats.getEnhance4T7());
        s.setEnhance2T8(stats.getEnhance2T8());
        s.setEnhance4T8(stats.getEnhance4T8());
        s.setEnhance2T9(stats.getEnhance2T9());
        s.setEnhance4T9(stats.getEnhance4T9());
        s.setEnhance2T10(stats.getEnhance2T10());
        s.setEnhance4T10(stats.getEnhance4T10());
        s.setPhysicalHit(stats.getPhysicalHit());
        s.setPhysicalHaste(stats.getPhysicalHaste());
        s.setPhysicalCrit(stats.getPhysicalCrit());
        s.setSpellHit(stats.getSpellHit());
        s.setSpellHaste(stats.getSpellHaste());
        s.setSpellCrit(stats.getSpellCrit());
        s.setSpellCritOnTarget(stats.getSpellCritOnTarget());
        s.setMp5(stats.getMp5());
        s.setManaRestoreFromMaxManaPerSecond(stats.getManaRestoreFromMaxManaPerSecond());
        s.setManaRestoreFromBaseManaPPM(stats.getManaRestoreFromBaseManaPPM());
        s.setDeathbringerProc(stats.getDeathbringerProc());
        s.setMoteOfAnger(stats.getMoteOfAnger());
        for (SpecialEffect effect : stats.specialEffects()) {
            if (hasRelevantTrigger(effect.getTrigger())) {
                if (relevantStats(effect.getStats()))
                    s.addSpecialEffect(effect);
                else {
                    for (SpecialEffect subEffect : effect.getStats().specialEffects())
                        if (hasRelevantTrigger(subEffect.getTrigger()) && relevantStats(subEffect.getStats()))
                            s.addSpecialEffect(effect);
                }
            }
        }
        return s;
    }

    public boolean hasRelevantTrigger(Trigger trigger) {
        return (trigger == Trigger.Use ||
                trigger == Trigger.SpellHit ||
                trigger == Trigger.SpellCrit ||
                trigger == Trigger.SpellCast ||
                trigger == Trigger.DamageSpellHit ||
                trigger == Trigger.DamageSpellCrit ||
                trigger == Trigger.DamageSpellCast ||
                trigger == Trigger.MeleeHit ||
                trigger == Trigger.MeleeCrit ||
                trigger == Trigger.MeleeAttack ||
                trigger == Trigger.PhysicalHit ||
                trigger == Trigger.PhysicalCrit ||
                trigger == Trigger.DamageDone ||
                trigger == Trigger.DamageOrHealingDone ||
                trigger == Trigger.ShamanLightningBolt ||
                trigger == Trigger.ShamanLavaLash ||
                trigger == Trigger.ShamanShock ||
                trigger == Trigger.ShamanStormStrike ||
                trigger == Trigger.ShamanShamanisticRage ||
                trigger == Trigger.ShamanFlameShockDoTTick ||
                trigger == Trigger.DoTTick);
    }

    @Override
    public boolean isBuffRelevant(Buff buff, Character character) {
        if (buff.name.startsWith("Amplify Magic"))
            return false;
        if (buff.getAllowedClasses().contains(CharacterClass.Shaman))
            return super.isBuffRelevant(buff, character);
        else
            return false;
    }

    @Override
    public boolean hasRelevantStats(Stats stats) {
        if (relevantStats(stats))
            return true;
        if (irrelevantStats(stats))
            return false;
        for (SpecialEffect effect : stats.specialEffects()) {
            if (hasRelevantTrigger(effect.getTrigger()))
                for (SpecialEffect subEffect : effect.getStats().specialEffects()) {
                    if (hasRelevantTrigger(subEffect.getTrigger()) && relevantStats(subEffect.getStats()))
                        return true;
                }
            if (relevantStats(effect.getStats()))
                return true;
        }
        return false;
    }

    private boolean relevantStats(Stats stats) {
        return (stats.getAgility() + stats.getIntellect() + stats.getStamina() + stats.getStrength() + stats.getSpirit() +
                stats.getAttackPower() + stats.getSpellPower() + stats.getMana() + stats.getWeaponDamage() + stats.getHealth() +
                stats.getArmorPenetration() + stats.getArmorPenetrationRating() +
                stats.getExpertise() + stats.getExpertiseRating() + stats.getHasteRating() + stats.getCritRating() + stats.getHitRating() +
                stats.getBonusAgilityMultiplier() + stats.getBonusAttackPowerMultiplier() + stats.getBonusCritMultiplier() +
                stats.getBonusStrengthMultiplier() + stats.getBonusSpellPowerMultiplier() + stats.getBonusIntellectMultiplier() +
                stats.getBonusSpiritMultiplier() + stats.getBonusDamageMultiplier() + stats.getBonusPhysicalDamageMultiplier() +
                stats.getBonusNatureDamageMultiplier() + stats.getBonusFireDamageMultiplier() + stats.getBonusSpellCritMultiplier() +
                stats.getBonusHealthMultiplier() + stats.getBonusManaMultiplier() + stats.getDeathbringerProc() +
                stats.getPhysicalCrit() + stats.getPhysicalHaste() + stats.getPhysicalHit() + stats.getParagon() + stats.getBonusShadowDamageMultiplier() +
                stats.getSpellCrit() + stats.getSpellCritOnTarget() + stats.getSpellHaste() + stats.getSpellHit() + stats.getHighestStat() +
                stats.getLightningSpellPower() + stats.getEnhance4T8() + stats.getEnhance4T7() + stats.getBonusWFAttackPower() +
                stats.getEnhance2T9() + stats.getEnhance4T9() + stats.getEnhance2T10() + stats.getEnhance4T10() +
                stats.getMp5() + stats.getManaRestoreFromMaxManaPerSecond() + stats.getManaRestoreFromBaseManaPPM() +
                stats.getEnhance2T7() + stats.getEnhance2T8() + stats.getBonusSSDamage() + stats.getMoteOfAnger()) > 0;
    }

    private boolean irrelevantStats(Stats stats) {
        return (stats.getTigersFuryCooldownReduction() + stats.getBonusRageGen()) > 0;
    }
    //endregion

    //region Custom Chart Data
    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        switch (chartName) {
            case "Combat Table (White)": {
                CharacterCalculationsEnhance currentCalculationsEnhanceWhite = (CharacterCalculationsEnhance) getCharacterCalculations(character);
                ComparisonCalculationEnhance calcMissWhite = new ComparisonCalculationEnhance();
                calcMissWhite.setName("    Miss    ");
                ComparisonCalculationEnhance calcDodgeWhite = new ComparisonCalculationEnhance();
                calcDodgeWhite.setName("   Dodge   ");
                ComparisonCalculationEnhance calcCritWhite = new ComparisonCalculationEnhance();
                calcCritWhite.setName("  Crit  ");
                ComparisonCalculationEnhance calcGlanceWhite = new ComparisonCalculationEnhance();
                calcGlanceWhite.setName(" Glance ");
                ComparisonCalculationEnhance calcHitWhite = new ComparisonCalculationEnhance();
                calcHitWhite.setName("Hit");
                if (currentCalculationsEnhanceWhite != null) {
                    calcMissWhite.setSubPoints(new float[2]);
                    calcMissWhite.setDPSPoints(0);
                    calcMissWhite.setOverallPoints(calcMissWhite.getSubPoints()[1] = 100 - currentCalculationsEnhanceWhite.getWhiteHit());
                    calcDodgeWhite.setOverallPoints(currentCalculationsEnhanceWhite.getDodgedAttacks());
                    calcDodgeWhite.setDPSPoints(currentCalculationsEnhanceWhite.getDodgedAttacks());
                    calcCritWhite.setSubPoints(new float[2]);
                    calcCritWhite.getSubPoints()[1] = currentCalculationsEnhanceWhite.getOverMeleeCritCap();
                    calcCritWhite.setOverallPoints(currentCalculationsEnhanceWhite.getMeleeCrit() - 4.8f);
                    calcCritWhite.setDPSPoints(calcCritWhite.getOverallPoints() - calcCritWhite.getSubPoints()[1]);
                    calcGlanceWhite.setOverallPoints(currentCalculationsEnhanceWhite.getGlancingBlows());
                    calcGlanceWhite.setDPSPoints(currentCalculationsEnhanceWhite.getGlancingBlows());
                    calcHitWhite.setOverallPoints(100f - calcMissWhite.getOverallPoints()
                            - calcDodgeWhite.getOverallPoints()
                            - calcCritWhite.getDPSPoints()
                            - calcGlanceWhite.getOverallPoints());
                    calcHitWhite.setDPSPoints(calcHitWhite.getOverallPoints());
                }
                return new ComparisonCalculationBase[] { calcMissWhite, calcDodgeWhite, calcCritWhite, calcGlanceWhite, calcHitWhite };
            }

            case "Combat Table (Yellow)": {
                CharacterCalculationsEnhance currentCalculationsEnhanceYellow = (CharacterCalculationsEnhance) getCharacterCalculations(character);
                ComparisonCalculationEnhance calcMissYellow = new ComparisonCalculationEnhance();
                calcMissYellow.setName("    Miss    ");
                ComparisonCalculationEnhance calcDodgeYellow = new ComparisonCalculationEnhance();
                calcDodgeYellow.setName("   Dodge   ");
                ComparisonCalculationEnhance calcCritYellow = new ComparisonCalculationEnhance();
                calcCritYellow.setName("  Crit  ");
                ComparisonCalculationEnhance calcHitYellow = new ComparisonCalculationEnhance();
                calcHitYellow.setName("Hit");
                if (currentCalculationsEnhanceYellow != null) {
                    calcMissYellow.setOverallPoints(100f - currentCalculationsEnhanceYellow.getYellowHit());
                    calcMissYellow.setDPSPoints(calcMissYellow.getOverallPoints());
                    calcDodgeYellow.setOverallPoints(currentCalculationsEnhanceYellow.getDodgedAttacks());
                    calcDodgeYellow.setDPSPoints(calcDodgeYellow.getOverallPoints());
                    calcCritYellow.setOverallPoints(currentCalculationsEnhanceYellow.getYellowCrit());
                    calcCritYellow.setDPSPoints(calcCritYellow.getOverallPoints());
                    calcHitYellow.setOverallPoints(100f - calcMissYellow.getOverallPoints() -
                            calcDodgeYellow.getOverallPoints() - calcCritYellow.getOverallPoints());
                    calcHitYellow.setDPSPoints(calcHitYellow.getOverallPoints());
                }
                return new ComparisonCalculationBase[] { calcMissYellow, calcDodgeYellow, calcCritYellow, calcHitYellow };
            }

            case "Combat Table (Spell)": {
                CharacterCalculationsEnhance currentCalculationsEnhanceSpell = (CharacterCalculationsEnhance) getCharacterCalculations(character);
                ComparisonCalculationEnhance calcMissSpell = new ComparisonCalculationEnhance();
                calcMissSpell.setName("    Miss    ");
                ComparisonCalculationEnhance calcCritSpell = new ComparisonCalculationEnhance();
                calcCritSpell.setName("  Crit  ");
                ComparisonCalculationEnhance calcHitSpell = new ComparisonCalculationEnhance();
                calcHitSpell.setName("Hit");
                if (currentCalculationsEnhanceSpell != null) {
                    calcMissSpell.setOverallPoints(100f - currentCalculationsEnhanceSpell.getSpellHit());
                    calcMissSpell.setDPSPoints(calcMissSpell.getOverallPoints());
                    calcCritSpell.setOverallPoints(currentCalculationsEnhanceSpell.getSpellCrit());
                    calcCritSpell.setDPSPoints(calcCritSpell.getOverallPoints());
                    calcHitSpell.setOverallPoints(100f - calcMissSpell.getOverallPoints() - calcCritSpell.getOverallPoints());
                    calcHitSpell.setDPSPoints(calcHitSpell.getOverallPoints());
                }
                return new ComparisonCalculationBase[] { calcMissSpell, calcCritSpell, calcHitSpell };
            }

            case "Relative Gem Values": {
                float dpsBase = getCharacterCalculations(character).getOverallPoints();
                float dpsStr = (getCharacterCalculations(character, statsItem(20f, 0f, 0f, 0f, 0f, 0f)).getOverallPoints() - dpsBase);
                float dpsAgi = (getCharacterCalculations(character, statsItem(0f, 20f, 0f, 0f, 0f, 0f)).getOverallPoints() - dpsBase);
                float dpsAP = (getCharacterCalculations(character, statsItem(0f, 0f, 40f, 0f, 0f, 0f)).getOverallPoints() - dpsBase);
                float dpsInt = (getCharacterCalculations(character, statsItem(0f, 0f, 0f, 20f, 0f, 0f)).getOverallPoints() - dpsBase);
                float dpsCrit = (getCharacterCalculations(character, statsItem(0f, 0f, 0f, 0f, 0f, 0f, 20f, 0f, 0f)).getOverallPoints() - dpsBase);
                float dpsExp = (getCharacterCalculations(character, statsItem(0f, 0f, 0f, 0f, 0f, 0f, 0f, 20f, 0f)).getOverallPoints() - dpsBase);
                float dpsHaste = (getCharacterCalculations(character, statsItem(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 20f)).getOverallPoints() - dpsBase);
                float dpsHit = (getCharacterCalculations(character, statsItem(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 20f)).getOverallPoints() - dpsBase);
                float dpsDmg = (getCharacterCalculations(character, statsItem(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f)).getOverallPoints() - dpsBase);
                float dpsPen = (getCharacterCalculations(character, statsItem(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 20f)).getOverallPoints() - dpsBase);
                float dpsSpd = (getCharacterCalculations(character, statsItem(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 23f)).getOverallPoints() - dpsBase);
                float dpsSta = (getCharacterCalculations(character, statsItem(0f, 0f, 0f, 0f, 30f, 0f)).getOverallPoints() - dpsBase);

                ComparisonCalculationEnhance c30Sta = new ComparisonCalculationEnhance();
                c30Sta.setName("30 Stamina");
                c30Sta.setOverallPoints(dpsAgi);
                c30Sta.setDPSPoints(dpsAgi);
                ComparisonCalculationEnhance c20Agi = new ComparisonCalculationEnhance();
                c20Agi.setName("20 Agility");
                c20Agi.setOverallPoints(dpsAgi);
                c20Agi.setDPSPoints(dpsAgi);
                ComparisonCalculationEnhance c20Str = new ComparisonCalculationEnhance();
                c20Str.setName("20 Strength");
                c20Str.setOverallPoints(dpsStr);
                c20Str.setDPSPoints(dpsStr);
                ComparisonCalculationEnhance c40AP = new ComparisonCalculationEnhance();
                c40AP.setName("40 Attack Power");
                c40AP.setOverallPoints(dpsAP);
                c40AP.setDPSPoints(dpsAP);
                ComparisonCalculationEnhance c20Int = new ComparisonCalculationEnhance();
                c20Int.setName("20 Intellect");
                c20Int.setOverallPoints(dpsInt);
                c20Int.setDPSPoints(dpsInt);
                ComparisonCalculationEnhance c20Crit = new ComparisonCalculationEnhance();
                c20Crit.setName("20 Crit Rating");
                c20Crit.setOverallPoints(dpsCrit);
                c20Crit.setDPSPoints(dpsCrit);
                ComparisonCalculationEnhance c20Exp = new ComparisonCalculationEnhance();
                c20Exp.setName("20 Expertise Rating");
                c20Exp.setOverallPoints(dpsExp);
                c20Exp.setDPSPoints(dpsExp);
                ComparisonCalculationEnhance c20Haste = new ComparisonCalculationEnhance();
                c20Haste.setName("20 Haste Rating");
                c20Haste.setOverallPoints(dpsHaste);
                c20Haste.setDPSPoints(dpsHaste);
                ComparisonCalculationEnhance c20Hit = new ComparisonCalculationEnhance();
                c20Hit.setName("20 Hit Rating");
                c20Hit.setOverallPoints(dpsHit);
                c20Hit.setDPSPoints(dpsHit);
                ComparisonCalculationEnhance c20Pen = new ComparisonCalculationEnhance();
                c20Pen.setName("20 Armor Penetration");
                c20Pen.setOverallPoints(dpsPen);
                c20Pen.setDPSPoints(dpsPen);
                ComparisonCalculationEnhance c23Spd = new ComparisonCalculationEnhance();
                c23Spd.setName("23 Spellpower");
                c23Spd.setOverallPoints(dpsSpd);
                c23Spd.setDPSPoints(dpsSpd);

                return new ComparisonCalculationBase[] {
                        c30Sta, c20Agi, c20Str, c40AP, c20Int, c20Crit, c20Exp, c20Haste, c20Hit, c20Pen, c23Spd
                };
            }

            case "MH Weapon Speeds": {
                if (character.get(CharacterSlot.MainHand) == null)
                    return new ComparisonCalculationBase[0];
                ComparisonCalculationBase MHonePointFour = checkWeaponSpeedEffect(character, 1.4f, true);
                ComparisonCalculationBase MHonePointFive = checkWeaponSpeedEffect(character, 1.5f, true);
                ComparisonCalculationBase MHonePointSix = checkWeaponSpeedEffect(character, 1.6f, true);
                ComparisonCalculationBase MHonePointSeven = checkWeaponSpeedEffect(character, 1.7f, true);
                ComparisonCalculationBase MHonePointEight = checkWeaponSpeedEffect(character, 1.8f, true);
                ComparisonCalculationBase MHtwoPointThree = checkWeaponSpeedEffect(character, 2.3f, true);
                ComparisonCalculationBase MHtwoPointFour = checkWeaponSpeedEffect(character, 2.4f, true);
                ComparisonCalculationBase MHtwoPointFive = checkWeaponSpeedEffect(character, 2.5f, true);
                ComparisonCalculationBase MHtwoPointSix = checkWeaponSpeedEffect(character, 2.6f, true);
                ComparisonCalculationBase MHtwoPointSeven = checkWeaponSpeedEffect(character, 2.7f, true);
                ComparisonCalculationBase MHtwoPointEight = checkWeaponSpeedEffect(character, 2.8f, true);
                return new ComparisonCalculationBase[] { MHonePointFour, MHonePointFive, MHonePointSix, MHonePointSeven, MHonePointEight,
                        MHtwoPointThree, MHtwoPointFour, MHtwoPointFive, MHtwoPointSix, MHtwoPointSeven, MHtwoPointEight };
            }

            case "OH Weapon Speeds": {
                if (character.get(CharacterSlot.OffHand) == null || character.getShamanTalents().getDualWield() != 1)
                    return new ComparisonCalculationBase[0];
                ComparisonCalculationBase OHonePointFour = checkWeaponSpeedEffect(character, 1.4f, false);
                ComparisonCalculationBase OHonePointFive = checkWeaponSpeedEffect(character, 1.5f, false);
                ComparisonCalculationBase OHonePointSix = checkWeaponSpeedEffect(character, 1.6f, false);
                ComparisonCalculationBase OHonePointSeven = checkWeaponSpeedEffect(character, 1.7f, false);
                ComparisonCalculationBase OHonePointEight = checkWeaponSpeedEffect(character, 1.8f, false);
                ComparisonCalculationBase OHtwoPointThree = checkWeaponSpeedEffect(character, 2.3f, false);
                ComparisonCalculationBase OHtwoPointFour = checkWeaponSpeedEffect(character, 2.4f, false);
                ComparisonCalculationBase OHtwoPointFive = checkWeaponSpeedEffect(character, 2.5f, false);
                ComparisonCalculationBase OHtwoPointSix = checkWeaponSpeedEffect(character, 2.6f, false);
                ComparisonCalculationBase OHtwoPointSeven = checkWeaponSpeedEffect(character, 2.7f, false);
                ComparisonCalculationBase OHtwoPointEight = checkWeaponSpeedEffect(character, 2.8f, false);
                return new ComparisonCalculationBase[] { OHonePointFour, OHonePointFive, OHonePointSix, OHonePointSeven, OHonePointEight,
                        OHtwoPointThree, OHtwoPointFour, OHtwoPointFive, OHtwoPointSix, OHtwoPointSeven, OHtwoPointEight };
            }

            default:
                return new ComparisonCalculationBase[0];
        }
    }

    private static Item statsItem(float strength, float agility, float attackPower, float intellect, float stamina, float spirit) {
        Item item = new Item();
        Stats stats = new Stats();
        stats.setStrength(strength);
        stats.setAgility(agility);
        stats.setAttackPower(attackPower);
        stats.setIntellect(intellect);
        stats.setStamina(stamina);
        stats.setSpirit(spirit);
        item.setStats(stats);
        return item;
    }

    private static Item statsItem(float strength, float agility, float attackPower, float intellect, float stamina, float spirit,
                                  float critRating, float expertiseRating, float hasteRating) {
        Item item = statsItem(strength, agility, attackPower, intellect, stamina, spirit);
        item.getStats().setCritRating(critRating);
        item.getStats().setExpertiseRating(expertiseRating);
        item.getStats().setHasteRating(hasteRating);
        return item;
    }

    private static Item statsItem(float strength, float agility, float attackPower, float intellect, float stamina, float spirit,
                                  float critRating, float expertiseRating, float hasteRating, float hitRating) {
        Item item = statsItem(strength, agility, attackPower, intellect, stamina, spirit, critRating, expertiseRating, hasteRating);
        item.getStats().setHitRating(hitRating);
        return item;
    }

    private static Item statsItem(float strength, float agility, float attackPower, float intellect, float stamina, float spirit,
                                  float critRating, float expertiseRating, float hasteRating, float hitRating, float weaponDamage) {
        Item item = statsItem(strength, agility, attackPower, intellect, stamina, spirit, critRating, expertiseRating, hasteRating, hitRating);
        item.getStats().setWeaponDamage(weaponDamage);
        return item;
    }

    private static Item statsItem(float strength, float agility, float attackPower, float intellect, float stamina, float spirit,
                                  float critRating, float expertiseRating, float hasteRating, float hitRating, float weaponDamage, float armorPenetrationRating) {
        Item item = statsItem(strength, agility, attackPower, intellect, stamina, spirit, critRating, expertiseRating, hasteRating, hitRating, weaponDamage);
        item.getStats().setArmorPenetrationRating(armorPenetrationRating);
        return item;
    }

    private static Item statsItem(float strength, float agility, float attackPower, float intellect, float stamina, float spirit,
                                  float critRating, float expertiseRating, float hasteRating, float hitRating, float weaponDamage, float armorPenetrationRating,
                                  float spellPower) {
        Item item = statsItem(strength, agility, attackPower, intellect, stamina, spirit, critRating, expertiseRating, hasteRating, hitRating, weaponDamage, armorPenetrationRating);
        item.getStats().setSpellPower(spellPower);
        return item;
    }

    private ComparisonCalculationBase checkWeaponSpeedEffect(Character character, float newSpeed, boolean mainHand) {
        float baseSpeed = 0f;
        int minDamage = 0;
        int maxDamage = 0;
        Item newWeapon;
        CharacterCalculationsBase baseCalc = getCharacterCalculations(character);
        Character deltaChar = character.clone();

        if (mainHand) {
            newWeapon = deltaChar.get(CharacterSlot.MainHand).getItem().clone();
            baseSpeed = deltaChar.get(CharacterSlot.MainHand).getSpeed();
            minDamage = deltaChar.get(CharacterSlot.MainHand).getMinDamage();
            maxDamage = deltaChar.get(CharacterSlot.MainHand).getMaxDamage();
        } else {
            newWeapon = deltaChar.get(CharacterSlot.OffHand).getItem().clone();
            baseSpeed = deltaChar.get(CharacterSlot.OffHand).getSpeed();
            minDamage = deltaChar.get(CharacterSlot.OffHand).getMinDamage();
            maxDamage = deltaChar.get(CharacterSlot.OffHand).getMaxDamage();
        }
        newWeapon.setMinDamage((int) Math.rint(minDamage / baseSpeed * newSpeed));
        newWeapon.setMaxDamage((int) Math.rint(maxDamage / baseSpeed * newSpeed));
        newWeapon.setSpeed(newSpeed);
        String speed = rawr.models.mage.NetFormat.netFloatToString(newSpeed) + " Speed";
        deltaChar.setIsLoading(true); // forces item instance to avoid invalidating and reloading from cache
        if (mainHand)
            deltaChar.set(CharacterSlot.MainHand, new ItemInstance(newWeapon, character.get(CharacterSlot.MainHand).getGem1(), character.get(CharacterSlot.MainHand).getGem2(), character.get(CharacterSlot.MainHand).getGem3(), character.get(CharacterSlot.MainHand).getEnchant()));
        else
            deltaChar.set(CharacterSlot.OffHand, new ItemInstance(newWeapon, deltaChar.get(CharacterSlot.OffHand).getGem1(), deltaChar.get(CharacterSlot.OffHand).getGem2(), deltaChar.get(CharacterSlot.OffHand).getGem3(), deltaChar.get(CharacterSlot.OffHand).getEnchant()));
        ComparisonCalculationBase result = Calculations.getCharacterComparisonCalculations(baseCalc, deltaChar, speed, baseSpeed == newWeapon.getSpeed());
        deltaChar.setIsLoading(false);
        result.setItem(null);
        return result;
    }
    //endregion

    public static CalculationsEnhance instance;

    public static CalculationsEnhance getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("Enhance", CalculationsEnhance.class);
    }

    public static void register() {
        Calculations.registerModel("Enhance", CalculationsEnhance.class);
    }

    public CalculationsEnhance() {
        instance = this;
    }
}
