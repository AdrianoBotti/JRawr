// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/CalculationsTankDK.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the UI-only CalculationOptionsPanel and the RAWR3/Silverlight color branch are omitted;
// the non-Silverlight display label/color/optimizable/chart/gemming tables are ported additively
// as getCharacterDisplayCalculationLabels()/getSubPointNameColors()/getOptimizableCalculationLabels()
// /getCustomChartNames()/getCustomChartData(). DeserializeDataObject uses a JDK-XML reflective
// reader instead of XmlSerializer (same approach as CalculationsProtPaladin); the serialized
// m_Rotation and hCurrentBoss subtrees are ignored (see CalculationOptionsTankDK).
package rawr.models.tankdk;

import rawr.base.stats.BaseStats;
import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterSlot;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.items.Enchant;
import rawr.base.items.GemmingTemplate;
import rawr.base.stats.HitResult;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.items.Item;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class CalculationsTankDK extends CalculationsBase {
    public static class TankDKChar {
        public Character Char;
        public CalculationOptionsTankDK opts;
        public Rotation Rot;
    }

    // Gems
    private enum GemQuality {
        Uncommon,
        Rare,
        Epic,
        Jewelcraft,

        NUM_Quality
    }

    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        // Relevant Gem IDs for TankDKs
        // Red
        //                    UC     Rare   Epic   JC
        int[] subtle = { 39907, 40000, 40115, 42151 }; // +Dodge

        // Purple
        int[] regal = { 39938, 40031, 40138, }; // +dodge, Stam

        // Blue
        int[] solid = { 39919, 40008, 40119, 36767 }; // +Stam

        // Green
        int[] enduring = { 39976, 40089, 40167, }; // +Def +Stam

        // Yellow
        int[] thick = { 39916, 40015, 40126, 42157 }; // +def

        // Orange
        int[] stalwart = { 39964, 40056, 40160 }; // +Dodge +Def

        // Meta
        int austere = 41380;

        // Prismatic:
        int nightmare = 49110;

        List<GemmingTemplate> templates = new ArrayList<GemmingTemplate>();
        templates.add(gemmingTemplate("Uncommon", false, stalwart[0], thick[0], enduring[0], thick[0], austere));
        templates.add(gemmingTemplate("Uncommon", false, subtle[0], stalwart[0], regal[0], subtle[0], austere));
        templates.add(gemmingTemplate("Uncommon", false, solid[0], solid[0], solid[0], solid[0], austere));
        templates.add(gemmingTemplate("Uncommon", false, regal[0], enduring[0], solid[0], solid[0], austere));

        templates.add(gemmingTemplate("Rare", false, stalwart[1], thick[1], enduring[1], thick[1], austere));
        templates.add(gemmingTemplate("Rare", false, subtle[1], stalwart[1], regal[1], subtle[1], austere));
        templates.add(gemmingTemplate("Rare", false, solid[1], solid[1], solid[1], solid[1], austere));
        templates.add(gemmingTemplate("Rare", false, regal[1], enduring[1], solid[1], solid[1], austere));

        templates.add(gemmingTemplate("Epic", true, stalwart[2], thick[2], enduring[2], thick[2], austere));
        templates.add(gemmingTemplate("Epic", true, subtle[2], stalwart[2], regal[2], subtle[2], austere));
        templates.add(gemmingTemplate("Epic", true, solid[2], solid[2], solid[2], solid[2], austere));
        templates.add(gemmingTemplate("Epic", true, regal[2], enduring[2], solid[2], nightmare, austere));

        templates.add(gemmingTemplate("Jeweler", false, thick[3], thick[3], thick[3], thick[3], austere));
        templates.add(gemmingTemplate("Jeweler", false, subtle[3], subtle[3], subtle[3], subtle[3], austere));
        templates.add(gemmingTemplate("Jeweler", false, solid[3], solid[3], solid[3], solid[3], austere));
        return templates;
    }

    private static GemmingTemplate gemmingTemplate(String group, boolean enabled, int redId, int yellowId, int blueId, int prismaticId, int metaId) {
        GemmingTemplate template = new GemmingTemplate();
        template.setModel("TankDK");
        template.setGroup(group);
        template.setEnabled(enabled);
        template.setRedId(redId);
        template.setYellowId(yellowId);
        template.setBlueId(blueId);
        template.setPrismaticId(prismaticId);
        template.setMetaId(metaId);
        return template;
    }

    // Intermediate Values
    public float BonusMaxRunicPower = 0f;
    public boolean m_bT9_4PC = false;

    public static int HitResultCount = HitResult.values().length;

    // SubPointColors
    private LinkedHashMap<String, String> subPointNameColors = null;
    private LinkedHashMap<String, String> subPointNameColorsSmt = new LinkedHashMap<String, String>();
    private LinkedHashMap<String, String> subPointNameColorsBurst = new LinkedHashMap<String, String>();

    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        if (subPointNameColors == null) {
            return subPointNameColorsSmt;
        }
        return subPointNameColors;
    }

    public CalculationsTankDK() {
        instance = this;
        subPointNameColorsSmt.put("Survival", "#0000FF");
        subPointNameColorsSmt.put("Mitigation", "#FF0000");
        subPointNameColorsSmt.put("Threat", "#00FF00");

        subPointNameColorsBurst.put("BurstTime", "#0000FF");
        subPointNameColorsBurst.put("ReactionTime", "#FF0000");

        subPointNameColors = subPointNameColorsSmt;
    }

    private static final String[] CharacterDisplayCalculationLabels = new String[] {
            "Summary:Survival Points*Survival Points represents the total raw damage \n(pre-Mitigation) you can take before dying. Unlike \nMitigation Points, you should not attempt to maximize this, \nbut rather get 'enough' of it, and then focus on Mitigation. \n'Enough' can vary greatly by fight and by your healers, but \nkeeping it roughly even with Mitigation Points is a good \nway to maintain 'enough' as you progress. If you find that \nyou are being killed by burst damage, focus on Survival Points.",
            "Summary:Mitigation Points*Mitigation Points represent the amount of damage you avoid, \non average per second, through avoidance stats (Miss, Dodge, Parry) along \nwith ways to improve survivablity, +heal or self healing, ability \ncooldowns.  It is directly relative to your Damage Taken. \nIdeally, you want to maximize Mitigation Points, while maintaining \n'enough' Survival Points (see Survival Points). If you find \nyourself dying due to healers running OOM, or being too busy \nhealing you and letting other raid members die, then focus on \nMitigation Points.  Represented in Damage per Second multiplied\nby MitigationWeight (seconds).",
            "Summary:Threat Points*Threat Points represent how much threat is capable for the current \ngear/talent/rotation setup.  Threat points are represented in Threat per second.",
            "Summary:Overall Points*Overall Points are a sum of Mitigation, Survival and Threat Points. \nOverall is typically, but not always, the best way to rate gear. \nFor specific encounters, closer attention to Mitigation or Survival \nPoints individually may be important.",

            "Basic Stats:Strength*Should Match in-game value.",
            "Basic Stats:Agility*Should Match in-game value.",
            "Basic Stats:Stamina*Should Match in-game value.",
            "Basic Stats:Attack Power",
            "Basic Stats:Crit Rating",
            "Basic Stats:Hit Rating*Should Match in-game value.",
            "Basic Stats:Expertise",
            "Basic Stats:Haste Rating",
            "Basic Stats:Armor Penetration",
            "Basic Stats:Armor Penetration Rating",
            "Basic Stats:Health*Including Frost Presence",
            "Basic Stats:Armor*Including Frost Presence",

            "Defense:Crit*Enemy's crit chance on you. When using the optimizer, set a secondary \ncriteria to this <= 0 to ensure that you stay defense-soft capped.",
            "Defense:Defense Rating",
            "Defense:Defense",
            "Defense:Resilience",
            "Defense:Defense Rating needed*Including Resilience to ensure being uncrittable.",

            "Advanced Stats:Miss*After Diminishing Returns",
            "Advanced Stats:Dodge*After Diminishing Returns",
            "Advanced Stats:Parry*After Diminishing Returns. Includes Str bonus from Unbreakable Armor's average uptime.",
            "Advanced Stats:Total Avoidance*Miss + Dodge + Parry",
            "Advanced Stats:Armor Damage Reduction",
            "Advanced Stats:Magic Damage Reduction*Currently Magic Resistance Only.",
            "Advanced Stats:Reaction Time*The time healers have to react to a particularly high damage burst before the next potential burst.",
            "Advanced Stats:Burst Time*Enhanced time-to-live calculation that factors avoidance and survival.",

            "Threat Stats:Target Miss*Chance to miss the target",
            "Threat Stats:Target Dodge*Chance the target dodges",
            "Threat Stats:Target Parry*Chance the target parries",
            "Threat Stats:Total Threat*[experimental] Raw Total Threat Generated by the specified rotation",
            "Threat Stats:Threat*Threat Per Second: Total Threat / Rotation Duration",

            "Damage Data:DPS*[experimental] DPS done for given rotation",
            "Damage Data:Rotation Time*[experimental] Duration of the total rotation cycle",
            "Damage Data:RS Limited*[experimental] Is the number of runestrikes specified in the rotation limited in any way",
            "Damage Data:Blood*[experimental] Number of Runes consumed",
            "Damage Data:Frost*[experimental] Number of Runes consumed",
            "Damage Data:Unholy*[experimental] Number of Runes consumed",
            "Damage Data:Death*[experimental] Number of Runes consumed",
            "Damage Data:Runic Power*[experimental] Amount of Runic Power consumed.\nNegative values mean more generated than used.",
    };

    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        return Arrays.asList(CharacterDisplayCalculationLabels);
    }

    private static final String[] CustomChartNames = new String[] { };

    public String[] getCustomChartNames() {
        return CustomChartNames;
    }

    private static final List<ItemType> RelevantItemTypes = List.of(
            ItemType.None,
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
        return new ComparisonCalculationTankDK();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsTankDK();
    }

    public String[] getOptimizableCalculationLabels() {
        return new String[] {
                "Chance to be Crit",
                "Avoidance %",
                "Damage Reduction %",
                "% Chance to Hit",
                "Target Parry %",
                "Target Dodge %",
                "Armor",
                "Health",
                "Hit Rating",
                "Reaction Time",
                "Burst Time",
                "Resilience",
                "Spell Penetration"
        };
    }

    // Static SpecialEffects
    private static final LinkedHashMap<Float, SpecialEffect[]> seSpellDeflection = new LinkedHashMap<Float, SpecialEffect[]>();
    private static final SpecialEffect seT10_4P;
    private static final SpecialEffect seFC1;
    private static final SpecialEffect seFC2;
    private static final SpecialEffect[][] seVampiricBlood;
    private static final SpecialEffect[] seRuneTap;
    private static final SpecialEffect[] seBloodyVengeance1;
    private static final SpecialEffect[] seBloodyVengeance2;
    private static final LinkedHashMap<Float, SpecialEffect[]> seBloodworms = new LinkedHashMap<Float, SpecialEffect[]>();
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
                { new SpecialEffect(Trigger.Use, null, 10 + 0 * 5, 60f - (false ? 10 : 0)), new SpecialEffect(Trigger.Use, null, 10 + 0 * 5, 60f - (true ? 10 : 0)), },
                { new SpecialEffect(Trigger.Use, null, 10 + 1 * 5, 60f - (false ? 10 : 0)), new SpecialEffect(Trigger.Use, null, 10 + 1 * 5, 60f - (true ? 10 : 0)), },
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

    /**
     * GetCharacterCalculations is the primary method of each model.
     */
    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        // DECISIONS #28: a user-initiated top-level calculation clears the shared SpecialEffect
        // interpolation caches so shared-JVM results match the oracle's per-process values.
        if (referenceCalculation && significantChange) {
            SpecialEffect.clearInterpolatorCaches();
        }
        // Setup what we need and validate.
        // Since calcs is what we return at the end.  And the caller can't handle null value returns -
        // Lets only return null if calcs is null, otherwise, let's return an empty calcs on other fails.
        CharacterCalculationsTankDK calcs = new CharacterCalculationsTankDK();

        TankDKChar TDK = new TankDKChar();
        TDK.Char = character;
        // Ok, this is the initial gathering of our information... we haven't processed the multipliers or anything.
        Stats stats = getCharacterStats(TDK.Char, additionalItem);
        // validate that we get a stats object;
        if (null == stats) { return calcs; }

        // Apply the Multipliers
        processStatModifiers(stats, TDK.Char.getDeathKnightTalents().getBladedArmor());

        // Import the option values from the options tab on the UI.
        TDK.opts = character.getCalculationOptions() instanceof CalculationOptionsTankDK
                ? (CalculationOptionsTankDK) character.getCalculationOptions()
                : null;
        // Validate opts
        if (null == TDK.opts) { return calcs; }
        // Get the shotrotation/combat model here.
        if (TDK.opts.getMRotation() == null) { return calcs; }

        // Level differences.
        int iTargetLevel = TDK.opts.getTargetLevel();
        if (TDK.opts.getBExperimental()) {
            iTargetLevel = TDK.opts.getHCurrentBoss().getLevel();
        }
        int iLevelDiff = iTargetLevel - character.getLevel();
        float fLevelDiffModifier = iLevelDiff * 0.2f;

        // Apply the ratings to actual stats.
        processRatings(stats);

        // Need to save off the base avoidance stats before having their ratings applied to them.
        float fBaseDodge = stats.getDodge();
        float fBaseParry = stats.getParry();
        float fBaseDef = stats.getDefense();
        float fBaseMiss = stats.getMiss();

        processAvoidance(stats, iTargetLevel);

        float fChanceToGetHit = 1f - (stats.getMiss() + stats.getDodge());
        if (TDK.Char.getMainHand() != null || TDK.Char.getOffHand() != null) {
            fChanceToGetHit -= stats.getParry();
        }

        // TargetDodge/Parry/Miss & Expertise - finish populating totalstats.
        boolean bDualWielding = false;
        float f2hWeaponDamageMultiplier = 0f;
        float hitChance = 0;
        float chanceTargetParry = StatConversion.WHITE_PARRY_CHANCE_CAP[iLevelDiff];
        float chanceTargetDodge = StatConversion.WHITE_DODGE_CHANCE_CAP[iLevelDiff];
        float chanceTargetMiss = StatConversion.WHITE_MISS_CHANCE_CAP[iLevelDiff];
        if (character.getMainHand() != null) {
            // 2-hander weapon specialization.
            if (character.getMainHand().getSlot() == ItemSlot.TwoHand) {
                f2hWeaponDamageMultiplier = (0.02f * TDK.Char.getDeathKnightTalents().getTwoHandedWeaponSpecialization());
            } else {
                // Toon is not using a 2h, meaning that he's DW if he's got something in his off hand.
                bDualWielding = (TDK.Char.getOffHand() != null && TDK.Char.getMainHand() != null);
            }
            // 8% default miss rate vs lvl 83
            chanceTargetMiss = Math.max(0f, StatConversion.WHITE_MISS_CHANCE_CAP[iLevelDiff] - stats.getPhysicalHit());
            if (bDualWielding) {
                // Talent: Nerves of Cold Steel
                // +hit changes only.  See damage buff change further down.
                chanceTargetMiss = (StatConversion.WHITE_MISS_CHANCE_CAP_DW[iLevelDiff]
                        - (0.01f * TDK.Char.getDeathKnightTalents().getNervesOfColdSteel()) - stats.getPhysicalHit());
            }
            if (TDK.Char.getRace() == rawr.base.character.CharacterRace.Dwarf &&
                    (TDK.Char.getMainHand().getItem().getType() == ItemType.TwoHandMace || TDK.Char.getMainHand().getItem().getType() == ItemType.OneHandMace)) {
                stats.setExpertise(stats.getExpertise() + 5);
            }
            if (TDK.Char.getRace() == rawr.base.character.CharacterRace.Human &&
                    (TDK.Char.getMainHand().getItem().getType() == ItemType.TwoHandMace || TDK.Char.getMainHand().getItem().getType() == ItemType.OneHandMace ||
                            TDK.Char.getMainHand().getItem().getType() == ItemType.TwoHandSword || TDK.Char.getMainHand().getItem().getType() == ItemType.OneHandSword)) {
                stats.setExpertise(stats.getExpertise() + 3);
            }
            if (TDK.Char.getRace() == rawr.base.character.CharacterRace.Orc &&
                    (TDK.Char.getMainHand().getItem().getType() == ItemType.TwoHandAxe || TDK.Char.getMainHand().getItem().getType() == ItemType.OneHandAxe)) {
                stats.setExpertise(stats.getExpertise() + 5);
            }
        }

        chanceTargetParry = Math.max(0.0f, StatConversion.WHITE_PARRY_CHANCE_CAP[iLevelDiff] - StatConversion.getDodgeParryReducFromExpertise(stats.getExpertise()));
        chanceTargetDodge = Math.max(0.0f, StatConversion.WHITE_DODGE_CHANCE_CAP[iLevelDiff] - StatConversion.getDodgeParryReducFromExpertise(stats.getExpertise()));
        hitChance = 1.0f - (chanceTargetMiss + chanceTargetDodge + chanceTargetParry);
        // Can't have more than 100% hit chance.
        hitChance = Math.min(1f, hitChance);

        // need to calculate the rotation after we have the DR values for Dodge/Parry/Miss/haste.
        TDK.opts.getMRotation().m_fDodge = stats.getDodge();
        TDK.opts.getMRotation().m_fParry = stats.getParry();
        TDK.opts.getMRotation().m_fPhysicalHaste = stats.getPhysicalHaste();

        if (character.getDeathKnightTalents().getSpellDeflection() > 0) {
            float key = round(stats.getParry(), 5);
            if (!seSpellDeflection.containsKey(key)) {
                seSpellDeflection.put(key, new SpecialEffect[] {
                        null, // this array point will never get selected because the opening If statement prevents it
                        new SpecialEffect(Trigger.DamageSpellHit, spellDamageTaken(-0.15f * 1), 0f, 0f, stats.getParry()),
                        new SpecialEffect(Trigger.DamageSpellHit, spellDamageTaken(-0.15f * 2), 0f, 0f, stats.getParry()),
                        new SpecialEffect(Trigger.DamageSpellHit, spellDamageTaken(-0.15f * 3), 0f, 0f, stats.getParry()),
                });
            }
            stats.addSpecialEffect(seSpellDeflection.get(key)[TDK.Char.getDeathKnightTalents().getSpellDeflection()]);
        }

        // This is the point that SHOULD have the right values according to the paper-doll.
        Stats sPaperDoll = stats.copy();

        CombatTable ct = new CombatTable(TDK.Char, calcs, stats, TDK.opts);
        // Setup for new combat table using the new ability objects.
        CombatTable2 ct2 = new CombatTable2(TDK.Char, stats, calcs, TDK.opts);
        // Now that we have the combat table, we should be able to integrate the Special effects.
        // However, the special effects will modify the incoming stats for all aspects, so we have
        // ensure that as we iterate, we don't count whole sets of stats twice.

        // T10 4PC
        if (stats.getTankDK_T10_4pc() != 0) {
            // Blood Tap:
            // 6% of base health Instant 1 min cooldown
            // Turns a blood rune to Death rune
            // Blood Armor:
            // When you activate Blood Tap, you gain 12% damage reduction from all attacks for 10 sec.
            // For now, we're going to assume that Blood Tap is used at every opportunity.
            stats.addSpecialEffect(seT10_4P);
        }

        // Filter out the duplicate Runes:
        if (character.getOffHandEnchant() == Enchant.findEnchant(3368, ItemSlot.OneHand, character)
                && character.getMainHandEnchant() == character.getOffHandEnchant()) {
            boolean bFC1Found = false;
            boolean bFC2Found = false;
            for (SpecialEffect se1 : stats.specialEffects()) {
                // if we've already found them, and we're seeing them again, then remove these repeats.
                if (bFC1Found && se1 == seFC1)
                    stats.removeSpecialEffect(se1);
                else if (bFC2Found && se1 == seFC2)
                    stats.removeSpecialEffect(se1);
                else if (se1 == seFC1)
                    bFC1Found = true;
                else if (se1 == seFC2)
                    bFC2Found = true;
            }
        }

        // Special Effects
        // For now we just factor them in once.
        StatsSpecialEffects sse = new StatsSpecialEffects(character, stats, ct);
        Stats statSE = new Stats();
        for (SpecialEffect e : stats.specialEffects()) {
            // There are some multi-level special effects that need to be factored in.
            for (SpecialEffect ee : e.getStats().specialEffects()) {
                e.setStats(sse.getSpecialEffects(TDK.opts, ee));
            }
            statSE.accumulate(sse.getSpecialEffects(TDK.opts, e));
        }
        // Darkmoon card greatness procs
        if (statSE.getHighestStat() > 0 || statSE.getParagon() > 0) {
            if (statSE.getStrength() >= statSE.getAgility()) {
                statSE.setStrength(statSE.getStrength() + statSE.getHighestStat() + statSE.getParagon());
            } else if (statSE.getAgility() > statSE.getStrength()) {
                statSE.setAgility(statSE.getAgility() + statSE.getHighestStat() + statSE.getParagon());
            }
            statSE.setHighestStat(0);
            statSE.setParagon(0);
        }

        // Any Modifiers from stats need to be applied to statSE
        statSE.setStrength(StatConversion.applyMultiplier(statSE.getStrength(), stats.getBonusStrengthMultiplier()));
        statSE.setAgility(StatConversion.applyMultiplier(statSE.getAgility(), stats.getBonusAgilityMultiplier()));
        statSE.setStamina(StatConversion.applyMultiplier(statSE.getStamina(), stats.getBonusStaminaMultiplier()));
        statSE.setArmor(StatConversion.applyMultiplier(statSE.getArmor(), stats.getBaseArmorMultiplier()));
        statSE.setAttackPower(StatConversion.applyMultiplier(statSE.getAttackPower(), stats.getBonusAttackPowerMultiplier()));
        statSE.setBonusArmor(StatConversion.applyMultiplier(statSE.getBonusArmor(), stats.getBonusArmorMultiplier()));

        float AgiArmor = StatConversion.getArmorFromAgility(statSE.getAgility()); // Don't multiply the armor from agility.
        statSE.setArmor(statSE.getArmor() + statSE.getBonusArmor() + AgiArmor);
        statSE.setHealth(statSE.getHealth() + StatConversion.getHealthFromStamina(statSE.getStamina()) + statSE.getBattlemasterHealth());
        StatConversion.applyMultiplier(statSE.getHealth(), stats.getBonusHealthMultiplier());
        if (character.getDeathKnightTalents().getBladedArmor() > 0) {
            statSE.setAttackPower(statSE.getAttackPower() + (statSE.getArmor() / 180f) * (float) character.getDeathKnightTalents().getBladedArmor());
        }
        statSE.setAttackPower(statSE.getAttackPower() + StatConversion.applyMultiplier((statSE.getStrength() * 2), stats.getBonusAttackPowerMultiplier()));
        statSE.setParryRating(statSE.getParryRating() + statSE.getStrength() * 0.25f);

        // Any Modifiers from statSE need to be applied to stats
        stats.setStrength(StatConversion.applyMultiplier(stats.getStrength(), statSE.getBonusStrengthMultiplier()));
        stats.setAgility(StatConversion.applyMultiplier(stats.getAgility(), statSE.getBonusAgilityMultiplier()));
        stats.setStamina(StatConversion.applyMultiplier(stats.getStamina(), statSE.getBonusStaminaMultiplier()));
        stats.setArmor(StatConversion.applyMultiplier(stats.getArmor(), statSE.getBaseArmorMultiplier()));
        stats.setAttackPower(StatConversion.applyMultiplier(stats.getAttackPower(), statSE.getBonusAttackPowerMultiplier()));
        stats.setBonusArmor(StatConversion.applyMultiplier(stats.getBonusArmor(), statSE.getBonusArmorMultiplier()));

        // Refresh the base avoidance values
        stats.setDodge(fBaseDodge);
        stats.setParry(fBaseParry);
        stats.setDefense(fBaseDef);
        stats.setMiss(fBaseMiss);

        stats.accumulate(statSE);

        // refresh avoidance w/ the new stats.
        float[] fAvoidance = new float[HitResultCount];
        for (int i = 0; i < HitResultCount; i++) {
            // GetDRAvoidanceChance returns a dec. percentage.
            // Since CurrentAvoidance is a percent, need to multiply by 100.
            fAvoidance[i] = (StatConversion.getDRAvoidanceChance(character, stats, HitResult.values()[i], iTargetLevel));
        }

        // So let's populate the miss, dodge and parry values pulling them out of the avoidance number.
        fChanceToGetHit = 1f;
        stats.setMiss(Math.min((StatConversion.CAP_MISSED[CharacterClass.DeathKnight.value()] / 100), fAvoidance[HitResult.Miss.ordinal()]));
        stats.setMiss(Math.max(0, stats.getMiss()));
        fChanceToGetHit -= stats.getMiss();
        // Dodge needs to be factored in here.
        stats.setDodge(Math.min((StatConversion.CAP_DODGE[CharacterClass.DeathKnight.value()] / 100), fAvoidance[HitResult.Dodge.ordinal()]));
        stats.setDodge(Math.max(stats.getDodge(), 0));
        fChanceToGetHit -= stats.getDodge();
        // Pary factors
        stats.setParry(Math.min((StatConversion.CAP_PARRY[CharacterClass.DeathKnight.value()] / 100), fAvoidance[HitResult.Parry.ordinal()]));
        stats.setParry(Math.max(stats.getParry(), 0));

        if (character.getMainHand() != null || character.getOffHand() != null) {
            fChanceToGetHit -= stats.getParry();
        }

        float fChanceToGetCrit = fAvoidance[HitResult.Crit.ordinal()];
        // The next call expect Defense rating to NOT be factored into the defense stat
        calcs.DefenseRatingNeeded = StatConversion.getDefenseRatingNeeded(character, stats, iTargetLevel);

        stats.setDefense(stats.getDefense() + StatConversion.getDefenseFromRating(stats.getDefenseRating(), character.getCharacterClass()));

        // 5% + Level difference crit chance.
        // Level difference is already factored in above.
        float fBaseCritChance = .05f;
        // Be sure that we don't have a negative chance to get crit.
        fChanceToGetCrit = Math.max(0.0f, (fBaseCritChance - fChanceToGetCrit));
        float fPercentCritMitigation = 1f - (fChanceToGetCrit / fBaseCritChance);

        // refresh Combat table w/ the new stats.
        ct = new CombatTable(character, calcs, stats, TDK.opts);
        if (TDK.opts.getBExperimental()) {
            // Setup for new combat table using the new ability objects.
            ct2 = new CombatTable2(character, stats, calcs, TDK.opts);
        }

        // Talents with general reach that aren't already in stats.
        // Talent: Bone Shield
        float bsDR = 0.0f;
        float bsUptime = 0f;
        if (character.getDeathKnightTalents().getBoneShield() > 0) {
            int BSStacks = 3;  // The number of bones by default.
            if (character.getDeathKnightTalents().isGlyphofBoneShield() == true) { BSStacks += 2; }

            float fBSCD = 60f;
            if (m_bT9_4PC) fBSCD -= 10f;

            bsUptime = Math.min(1f,                         // Can't be up for longer than 100% of the time.
                    (BSStacks * 2f)                   // 2 sec internal cooldown on loosing bones so the DK can't get spammed to death.
                            / (1 - fChanceToGetHit)   // Loose a bone every time we get hit.
                            / fBSCD);                          // 60 sec cooldown.
            // 20% damage reduction while active.
            bsDR = 0.2f * bsUptime;
        }
        stats.setDamageTakenMultiplier(stats.getDamageTakenMultiplier() - bsDR);

        // Talent: Vampiric Blood
        if (character.getDeathKnightTalents().getVampiricBlood() > 0) {
            Stats VBStats = new Stats();
            VBStats.setHealth(stats.getHealth() * 0.15f);
            VBStats.setHealingReceivedMultiplier(0.35f);
            float uptime = seVampiricBlood[character.getDeathKnightTalents().isGlyphofVampiricBlood() ? 1 : 0][m_bT9_4PC ? 1 : 0].getAverageUptime(0f, 1f);
            stats.accumulate(VBStats, uptime);
        }

        // Talent: RuneTap
        if (character.getDeathKnightTalents().getRuneTap() > 0) {
            // Improved Rune Tap.
            // increases the health provided by RT by 33% per point. and lowers the CD by 10 sec per point
            Stats newStats = new Stats();
            newStats.setHealed((stats.getHealth() * 0.10f) * (1f + (character.getDeathKnightTalents().getImprovedRuneTap() / 3f)));
            float uptime = seRuneTap[character.getDeathKnightTalents().getImprovedRuneTap()].getAverageUptime(0f, 1f);
            stats.accumulate(newStats, uptime);
        }

        // Assuming the Boss has no ArPen
        // From http://www.skeletonjack.com/2009/05/14/dk-tanking-armor-cap/#comments
        // 75% armor cap.  Not sure if this is for DK or for all Tanks.  So I'm just going to handle it here.
        // I'll do more research and see if it needs to go into the general function.
        float ArmorDamageReduction = (float) Math.min(0.75f, StatConversion.getArmorDamageReduction(iTargetLevel, stats.getArmor(), 0f, 0f, 0f));

        // Setup Fight parameters

        float fFightDuration = TDK.opts.getFightLength();
        if (fFightDuration == 0f) {
            TDK.opts.setFightLength(10f);
            fFightDuration = 10f;
        }
        // Does the boss have parry haste?
        boolean bParryHaste = TDK.opts.getBParryHaste();

        // Get the values of each type of damage in %.
        // So first we get each type of damage in the same units: DPS.
        float fPhyDamageDPS = getDPS(TDK.opts.getIncomingDamage(), TDK.opts.getBossAttackSpeed() * 1.14f);
        float fBleedDamageDPS = getDPS(TDK.opts.getIncomingBleedDamage(), TDK.opts.getBleedTickFrequency());
        float fMagicDamageDPS = getDPS(TDK.opts.getIncomingMagicDamage(), TDK.opts.getIncomingFromMagicFrequency());
        // Get the total DPS.
        float fTotalDPS = fPhyDamageDPS + fBleedDamageDPS + fMagicDamageDPS;
        // Factor the segments out.
        float fPhyDamPercent = fPhyDamageDPS / fTotalDPS;
        float fBleedDamPercent = fBleedDamageDPS / fTotalDPS;
        float fMagicDamPercent = fMagicDamageDPS / fTotalDPS;

        float fTotalMitigation = 0f;

        // We want to start getting the Boss Handler stuff going on.
        // ***** Boss Handler *****
        // Setup initial Boss data.
        rawr.base.bosses.BossHandler hCurrentBoss = new rawr.base.bosses.BossHandler();
        if (TDK.opts.getBExperimental()) {
            hCurrentBoss = TDK.opts.getHCurrentBoss();
            // How much of what kind of damage does this boss deal with?
            // ** Incoming Boss Damage **
            int uAttackCount = hCurrentBoss.getAttacks().size();
            if (uAttackCount <= 0) {
                // Error
            } else {
                fPhyDamageDPS = hCurrentBoss.getDpsByType(rawr.base.bosses.AttackTypes.AT_MELEE, 0, 0, stats.getMiss(), stats.getDodge(), stats.getParry(), 0, 0);
                for (rawr.base.bosses.Attack a : hCurrentBoss.getAttacks()) {
                    if (a.isIgnoresAllTanks() == false) {
                        // Bleeds vs Magic vs Physical
                        if (a.damageType == rawr.base.items.ItemDamageType.Physical) {
                            // Bleed or Physical
                            if (a.isAvoidable()) {
                                fPhyDamageDPS = getDPS(a.getDamagePerHit(), a.attackSpeed);
                            } else {
                                fBleedDamageDPS = getDPS(a.getDamagePerHit(), a.attackSpeed);
                            }
                        } else {
                            // Magic
                            fMagicDamageDPS = getDPS(a.getDamagePerHit(), a.attackSpeed);
                        }
                    }
                }
            }

            // Fight Settings
            // Set the Fight Duration to no larger than the Berserk Timer
            fFightDuration = Math.min(hCurrentBoss.getBerserkTimer(), fFightDuration);
            bParryHaste = hCurrentBoss.getDefaultMeleeAttack() != null ? hCurrentBoss.getDefaultMeleeAttack().useParryHaste : false;
        }

        // ***** Survival Rating *****
        // For right now Survival Rating == Effective Health will be HP + Armor/Resistance mitigation values.
        // Everything else is really mitigating damage based on RNG.

        // The health bonus from Frost presence is now include in the character by default.
        float fPhysicalSurvival = stats.getHealth();
        float fBleedSurvival = stats.getHealth();
        float fMagicalSurvival = stats.getHealth();

        // Physical damage:
        fPhysicalSurvival = getEffectiveHealth(stats.getHealth(), ArmorDamageReduction, fPhyDamPercent);

        // Bleed damage:
        fBleedSurvival = getEffectiveHealth(stats.getHealth(), 0, fBleedDamPercent);

        // Magical damage:
        // if there is a max resistance, then it's likely they are stacking for that resistance.
        // So factor in that Max resistance.
        float fMaxResist = Math.max(stats.getArcaneResistance(), stats.getFireResistance());
        fMaxResist = Math.max(fMaxResist, stats.getFrostResistance());
        fMaxResist = Math.max(fMaxResist, stats.getNatureResistance());
        fMaxResist = Math.max(fMaxResist, stats.getShadowResistance());

        float fMagicDR = StatConversion.getAverageResistance(iTargetLevel, character.getLevel(), fMaxResist, 0f);
        calcs.MagicDamageReduction = fMagicDR;
        fMagicalSurvival = getEffectiveHealth(stats.getHealth(), fMagicDR, fMagicDamPercent);

        float fEffectiveHealth = fPhysicalSurvival + fBleedSurvival + fMagicalSurvival;
        // EffHealth is used further down for Burst/Reaction Times.
        calcs.PhysicalSurvival = fPhysicalSurvival;
        calcs.BleedSurvival = fBleedSurvival;
        calcs.MagicSurvival = fMagicalSurvival;
        calcs.Survival = fEffectiveHealth;
        calcs.SurvivalWeight = TDK.opts.getSurvivalWeight();

        // ***** Threat Rating *****
        float fRotDuration = ct.calcOpts.getMRotation().getRotationDuration();
        float DSperSec = 0;
        float fThreatTotal = 0f;
        float fThreatPS = 0f;

        fThreatTotal = ct.getTotalThreat();
        if (TDK.opts.getBExperimental()) {
            // Setup for new combat table using the new ability objects.
            fThreatTotal = ct2.getMTps();
            fRotDuration = (float) ct2.getMRotationDuration() / 1000;
            calcs.RotationTime = ct2.getMRotationDuration();
            fThreatPS = ct2.getMTps();
            calcs.DPS = ct2.getMDps();
            DSperSec = ct2.m_Rotation.DeathStrike / fRotDuration;
        } else if (fRotDuration > 0) {
            calcs.RotationTime = fRotDuration * 1000;
            DSperSec = ct.calcOpts.getMRotation().DeathStrike / fRotDuration;
            fThreatPS = fThreatTotal / fRotDuration;
        }

        calcs.Threat = fThreatPS;
        // Improved Blood Presence
        if (character.getDeathKnightTalents().getImprovedBloodPresence() > 0) {
            if (TDK.opts.getBExperimental()) {
                float fDamageDone = ct2.getMDps();
                stats.setHealed(stats.getHealed() + (fDamageDone * 0.02f * character.getDeathKnightTalents().getImprovedBloodPresence()));
            } else {
                float fDamageDone = fThreatPS / 2.035f; // reducing the TPS by the multiplier for Frost presence for basic DPS number - not the most accurate, but it gets us closer.
                stats.setHealed(stats.getHealed() + (fDamageDone * 0.02f * character.getDeathKnightTalents().getImprovedBloodPresence()));
            }
        }

        // Factor in damage procs.
        float fDamageFromProcs = stats.getArcaneDamage() + stats.getFireDamage() + stats.getFrostDamage() + stats.getShadowDamage() + stats.getNatureDamage() + stats.getHolyDamage();
        calcs.Threat += fDamageFromProcs;

        if (TDK.opts.getBExperimental()) {
            calcs.DPS += fDamageFromProcs;
            calcs.RSLimit = ct2.m_iRSState;

            calcs.Blood = ct2.getMBloodRunes();
            calcs.Frost = ct2.getMFrostRunes();
            calcs.Unholy = ct2.getMUnholyRunes();
            calcs.Death = ct2.getMDeathRunes();
            calcs.RP = ct2.getMRunicPower();
            calcs.TotalThreat = (int) ct2.getTotalThreat();
        } else {
            calcs.Blood = ct.getMBloodRunes();
            calcs.Frost = ct.getMFrostRunes();
            calcs.Unholy = ct.getMUnholyRunes();
            calcs.Death = ct.getMDeathRunes();
        }

        // Threat buffs.
        calcs.Threat *= 1f + (stats.getThreatIncreaseMultiplier() - stats.getThreatReductionMultiplier());
        calcs.ThreatWeight = TDK.opts.getThreatWeight();

        // ***** Mitigation Rating *****
        float fSegmentMitigation = 0f;

        // ** Crit Mitigation **
        // Crit mitigation:
        // Crit mitigation works for Magical as well as Physical damage so take care of that first.
        float fCritMultiplier = 1;
        // Bleeds can't crit.
        // Neither can spells from bosses.  (As per a Loading screen ToolTip.)
        float fCritDPS = (fPhyDamageDPS) * fCritMultiplier;
        fSegmentMitigation = (fCritDPS * fPercentCritMitigation);
        // Add in the value of crit mitigation.
        fTotalMitigation += fSegmentMitigation;
        // The max damage at this point needs to include crit.
        float fMaxIncDPS = fTotalDPS + fCritDPS - fSegmentMitigation;

        // How much damage per shot normal shot?
        float fPerShotPhysical = TDK.opts.getIncomingDamage();
        // The Haste Mitigation block below shares this local: the parry haste code rewrites it and
        // Mark of Blood / Burst & Reaction Time read the rewritten value (C# method scope).
        float fBossAverageAttackSpeed = TDK.opts.getBossAttackSpeed() * 1.14f * (1f + (.02f * character.getDeathKnightTalents().getImprovedIcyTouch()));

        // ** Haste Mitigation **
        // Placeholder for comparing differing DPS values related to haste.
        float fNewIncPhysDPS = 0;
        // Let's just look at Imp Icy Touch
        // Improved Icy Touch
        {
            // Get the new slowed AttackSpeed based on ImpIcyTouch
            // Factor in the base slow caused by FF (14% base).
            fBossAverageAttackSpeed = TDK.opts.getBossAttackSpeed() * 1.14f * (1f + (.02f * character.getDeathKnightTalents().getImprovedIcyTouch()));
            // Figure out what the new Physical DPS should be based on that.
            fNewIncPhysDPS = getDPS(fPerShotPhysical, fBossAverageAttackSpeed);
            // Send the difference to the Mitigation value.
            fSegmentMitigation = fPhyDamageDPS - fNewIncPhysDPS;
            fTotalMitigation += fSegmentMitigation;
            if (TDK.opts.getAdditiveMitigation()) {
                // Lets' remove the Damage that was avoided.
                fPhyDamageDPS -= fSegmentMitigation;
            }
        }

        // we don't have to do this work unless we are working out parry haste since we already have the current DPS.
        // Parry Haste
        if (bParryHaste) {
            if (fFightDuration == 0f) {
                TDK.opts.setFightLength(10f);
                fFightDuration = 10f;
            }
            float fNumRotations = 0f;

            // How many shots over the length of the fight?
            float fTotalBossAttacksPerFight = (fFightDuration * 60f) / (TDK.opts.getBossAttackSpeed() * 1.14f * (1f + (.02f * character.getDeathKnightTalents().getImprovedIcyTouch())));
            // Integrate Expertise values to prevent additional physical damage coming in:
            // Each parry reducing swing timer by up to 40% so we'll average that damage increase out.
            // Each parry is factored by weapon speed - the faster the weapons, the more likely the boss can parry.
            // Figure out how many shots there are.  Right now, just calculating white damage.
            // How fast is a hasted shot? up to 40% faster.
            // average based on parry haste being equal to Math.Min(Math.Max(timeRemaining-0.4,0.2),timeRemaining)
            float fBossShotCountPerRot = 0f;
            if (fRotDuration > 0) {
                fNumRotations = (fFightDuration * 60f) / fRotDuration;
                // How many shots does the boss take over a given rotation period.
                fBossShotCountPerRot = fRotDuration / fBossAverageAttackSpeed;
                float fCharacterShotCount = 0f;
                if (character.getMainHand() != null && ct.MH.hastedSpeed > 0f) {
                    fCharacterShotCount += (fRotDuration / ct.MH.hastedSpeed);
                }
                if (ct.dw || character.getMainHand() == null && character.getOffHand() != null && ct.OH.hastedSpeed > 0f) {
                    fCharacterShotCount += (fRotDuration / ct.OH.hastedSpeed);
                }
                fCharacterShotCount += ct.totalParryableAbilities;

                // Max Parry-Hasted Damage
                float fMaxHastedBossAttackSpeed = getParryHastedAttackSpeed(StatConversion.WHITE_PARRY_CHANCE_CAP[iLevelDiff], fCharacterShotCount, fBossAverageAttackSpeed, fRotDuration);
                float fMaxPhyDamageDPS = getDPS(fPerShotPhysical, fMaxHastedBossAttackSpeed);

                // Actual Parry-haste for this character
                // Now, what's the actual expertise-based hasted damage?
                fBossAverageAttackSpeed = getParryHastedAttackSpeed(chanceTargetParry, fCharacterShotCount, fBossAverageAttackSpeed, fRotDuration);
                fNewIncPhysDPS = getDPS(fPerShotPhysical, fBossAverageAttackSpeed);

                // Still need to translate this to how much is mitigated by Expertise.
                fSegmentMitigation = fMaxPhyDamageDPS - fNewIncPhysDPS;

                fTotalMitigation += fSegmentMitigation;
                if (TDK.opts.getAdditiveMitigation()) {
                    // Lets' remove the Damage that was avoided.
                    fPhyDamageDPS -= fSegmentMitigation;
                }
            }
        }

        // ** Mark of Blood **
        // Mark of blood
        // Cast on the enemy
        // buff that lasts 20 secs or 20 hits
        // heals the target for 4% of max health for each damage dealing hit from that enemy to the target of that enemy.
        // 3 Min CD.
        if (character.getDeathKnightTalents().getMarkOfBlood() > 0) {
            // Now that we have the Avg. Boss Attack speed, let's figure how many attacks in 20 secs.
            float AttacksFor20 = Math.min(20f, 20f / fBossAverageAttackSpeed);
            float MOBhealing = stats.getHealth() * .04f * (AttacksFor20 * fChanceToGetHit); // how many attacks get through avoidance.
            float MoB_HPS = (MOBhealing * Math.max(1f, fFightDuration / 3)) / (fFightDuration * 60); // Fire it off every time we can and at least once per fight.
            stats.setHealed(stats.getHealed() + MoB_HPS);
        }

        // ** Avoidance Mitigation **
        // Let's see how much damage was avoided.
        float fAvoidanceTotal = 1 - fChanceToGetHit;
        // Raise the total mitgation by that amount.
        fSegmentMitigation = fPhyDamageDPS * Math.min(1f, fAvoidanceTotal);
        fTotalMitigation += fSegmentMitigation;
        if (TDK.opts.getAdditiveMitigation()) {
            // Lets' remove the Damage that was avoided.
            fPhyDamageDPS -= fSegmentMitigation;
        }

        // ** Anti-Magic Shell **
        // Anti-Magic Shell. ////////////////////////////////////////////////////////
        // Talent: MagicSuppression increases AMS by 8/16/25% per point.
        // Glyph: GlyphofAntiMagicShell increases AMS by 2 sec.
        // AMS has a 45 sec CD.
        float amsDuration = (5f + (character.getDeathKnightTalents().isGlyphofAntiMagicShell() == true ? 2f : 0f));
        float amsUptimePct = amsDuration / 45f;
        // AMS reduces damage taken by 75% up to a max of 50% health.
        float amsReduction = 0.75f * (1f + character.getDeathKnightTalents().getMagicSuppression() * 0.08f + (character.getDeathKnightTalents().getMagicSuppression() == 3 ? 0.01f : 0f));
        float amsReductionMax = stats.getHealth() * 0.5f;
        // up to 50% of health means that the amdDRvalue equates to the raw damage points removed.
        // This means that toon health and INC damage values from the options pane are going to affect this quite a bit.
        float amsDRvalue = (Math.min(amsReductionMax, (fMagicDamageDPS * amsDuration) * amsReduction) * amsUptimePct);
        // Raise the TotalMitigation by that amount.
        fTotalMitigation += amsDRvalue;
        if (TDK.opts.getAdditiveMitigation()) {
            // lower the Magical DPS by the AMSDRValue
            fPhyDamageDPS -= amsDRvalue;
        }

        // ** Armor Damage Mitigation **
        // For any physical only damage reductions.
        // Factor in armor Damage Reduction
        fTotalMitigation += fPhyDamageDPS * ArmorDamageReduction;
        if (TDK.opts.getAdditiveMitigation()) {
            fPhyDamageDPS -= fPhyDamageDPS * ArmorDamageReduction;
        }

        // ** Resistance Damage Mitigation **
        // For any physical only damage reductions.
        // Factor in armor Damage Reduction
        fTotalMitigation += fMagicDamageDPS * fMagicDR;
        if (TDK.opts.getAdditiveMitigation()) {
            fMagicDamageDPS -= fMagicDamageDPS * fMagicDR;
        }

        // Four T8 : AMS grants 10% damage reduction.
        stats.setDamageTakenMultiplier(stats.getDamageTakenMultiplier() - (stats.getBonusAntiMagicShellDamageReduction() * amsUptimePct));

        // ** Damage Taken Mitigation **
        fSegmentMitigation = Math.abs(fMagicDamageDPS * stats.getSpellDamageTakenMultiplier());
        fSegmentMitigation += Math.abs(fMagicDamageDPS * stats.getDamageTakenMultiplier());
        fSegmentMitigation += Math.abs(fBleedDamageDPS * stats.getDamageTakenMultiplier());
        fSegmentMitigation += Math.abs(fPhyDamageDPS * stats.getDamageTakenMultiplier());
        fTotalMitigation += fSegmentMitigation;

        // ** Damage Absorbed Mitigation **
        fTotalMitigation += stats.getDamageAbsorbed();

        // Let's make sure we don't go into negative damage here
        fMagicDamageDPS = Math.max(0f, fMagicDamageDPS);
        fPhyDamageDPS = Math.max(0f, fPhyDamageDPS);

        // ** Burst/Reaction Time **
        // The next 2 returns are in swing count.
        float fReactionSwingCount = getReactionTime(fAvoidanceTotal);
        float fBurstSwingCount = getBurstTime(fAvoidanceTotal, fEffectiveHealth, TDK.opts.getIncomingDamage());

        // Get how long that actually will be on Average.
        calcs.ReactionTime = fReactionSwingCount * fBossAverageAttackSpeed;
        calcs.BurstTime = fBurstSwingCount * fBossAverageAttackSpeed;

        // Mitigation is the difference between what damage would have been before and what it is once you factor in mitigation effects.
        fSegmentMitigation = 0;
        fSegmentMitigation += StatConversion.applyMultiplier(stats.getHealed(), stats.getHealingReceivedMultiplier());
        fSegmentMitigation += (StatConversion.applyMultiplier(stats.getHp5(), stats.getHealingReceivedMultiplier()) / 5);
        fSegmentMitigation += StatConversion.applyMultiplier(stats.getHealthRestore(), stats.getHealingReceivedMultiplier());
        // Health Returned by DS and other sources:
        if (DSperSec > 0)
            fSegmentMitigation += StatConversion.applyMultiplier((stats.getHealthRestoreFromMaxHealth() * stats.getHealth()) * DSperSec, stats.getHealingReceivedMultiplier());
        if (stats.getHealthRestoreFromMaxHealth() > 0)
            fSegmentMitigation += StatConversion.applyMultiplier((stats.getHealthRestoreFromMaxHealth() * stats.getHealth()), stats.getHealingReceivedMultiplier());
        fTotalMitigation += fSegmentMitigation;

        calcs.Mitigation = fTotalMitigation;
        calcs.MitigationWeight = TDK.opts.getMitigationWeight();

        // Key Data Validation
        if (Float.isNaN(calcs.Threat) ||
                Float.isNaN(calcs.Survival) ||
                Float.isNaN(calcs.Mitigation) ||
                Float.isNaN(calcs.BurstTime) ||
                Float.isNaN(calcs.ReactionTime) ||
                Float.isNaN(calcs.getOverallPoints())) {
            // DEBUG-only throw in the original; the Java port ignores it.
        }

        // Display only work
        calcs.cType = TDK.opts.getCType();
        if (TDK.opts.getCType() == CalculationType.Burst) {
            subPointNameColors = subPointNameColorsBurst;
        } else {
            subPointNameColors = subPointNameColorsSmt;
        }

        calcs.BasicStats = sPaperDoll;
        // The full character data.
        calcs.TargetLevel = iTargetLevel;

        calcs.Miss = stats.getMiss() * 100f;
        calcs.Dodge = stats.getDodge() * 100f;
        calcs.Parry = stats.getParry() * 100f;
        calcs.Crit = fChanceToGetCrit * 100f;

        calcs.DefenseRating = stats.getDefenseRating();
        calcs.Defense = stats.getDefense();
        calcs.Resilience = stats.getResilience();

        calcs.TargetDodge = chanceTargetDodge;
        calcs.TargetMiss = chanceTargetMiss;
        calcs.TargetParry = chanceTargetParry;
        calcs.Expertise = stats.getExpertise();

        calcs.ArmorDamageReduction = ArmorDamageReduction;

        return calcs;
    }

    private static Stats spellDamageTaken(float value) {
        Stats s = new Stats();
        s.setSpellDamageTakenMultiplier(value);
        return s;
    }

    /** C# Math.Round(x, 5) (banker's rounding) without the extra scaling error. */
    private static float round(float value, int digits) {
        double scale = Math.pow(10, digits);
        return (float) (Math.rint((double) value * scale) / scale);
    }

    /**
     * GetCharacterStats is the 2nd-most calculation intensive method in a model. Here the model will
     * combine all of the information about the character, including race, gear, enchants, buffs,
     * calculationoptions, etc., to form a single combined Stats object.
     */
    public Stats getCharacterStats(Character character, Item additionalItem) {
        Stats statsTotal = new Stats();

        // Validate that character.CalculationOptions != NULL
        if (null == character.getCalculationOptions()) {
            // Possibly put some error text here.
            return statsTotal;
        }
        CalculationOptionsTankDK calcOpts = character.getCalculationOptions() instanceof CalculationOptionsTankDK
                ? (CalculationOptionsTankDK) character.getCalculationOptions()
                : null;

        // Filter out the duplicate Runes:
        if (character.getMainHandEnchant() == Enchant.findEnchant(3368, ItemSlot.OneHand, character)
                && character.getMainHandEnchant() == character.getOffHandEnchant()) {
            // Remove one of the enchants.
            character.setOffHandEnchant(null);
        }

        // Start populating data w/ Basic racial & class baseline.
        statsTotal = BaseStats.getBaseStats(character);
        statsTotal.setBaseAgility(BaseStats.getBaseStats(character).getAgility());

        if (statsTotal.getDefense() < 400f)
            // Adding in the base 400 Defense skill all tanks are expected to have.
            // There are too many places where this just kinda stuck in.  It should be attached to the toon.
            statsTotal.setDefense(400f);
        accumulateItemStats(statsTotal, character, additionalItem);
        accumulateBuffsStats(statsTotal, character.getActiveBuffs()); // includes set bonuses.
        // Except the 4 piece T9 - improves CD of VB, UA, and BS by 10 sec.  That has to get handled elsewhere.
        if (character.activeBuffsContains("Thassarian's Plate 4 Piece Bonus") ||
                character.activeBuffsContains("Koltira's Plate 4 Piece Bonus")) {
            // Set the character as having the T9_4pc bonus
            m_bT9_4PC = true;
        }

        // Stack only the info we care about.
        accumulateTalents(statsTotal, character);
        statsTotal = getRelevantStats(statsTotal);
        accumulateFrostPresence(statsTotal);

        /* At this point, we're combined all the data from gear and talents and all that happy jazz.
         * However, we haven't applied any special effects nor have we applied any multipliers.
         */

        return (statsTotal);
    }

    public Stats getBuffsStats(Character character, CalculationOptionsTankDK calcOpts) {
        List<Buff> removedBuffs = new ArrayList<Buff>();
        List<Buff> addedBuffs = new ArrayList<Buff>();

        Stats statsBuffs = getBuffsStats(character.getActiveBuffs());

        for (Buff b : removedBuffs) {
            character.activeBuffsAdd(b.name);
        }
        for (Buff b : addedBuffs) {
            character.getActiveBuffs().remove(b);
        }

        return statsBuffs;
    }

    /** Process the Stat modifier values. */
    private void processStatModifiers(Stats statsTotal, int iBladedArmor) {
        statsTotal.setStrength(StatConversion.applyMultiplier(statsTotal.getStrength(), statsTotal.getBonusStrengthMultiplier()));
        statsTotal.setAgility(StatConversion.applyMultiplier(statsTotal.getAgility(), statsTotal.getBonusAgilityMultiplier()));
        // The stamina value is floor in game for the calculation
        statsTotal.setStamina(StatConversion.applyMultiplier(statsTotal.getStamina(), statsTotal.getBonusStaminaMultiplier()));
        statsTotal.setStamina((float) Math.floor(statsTotal.getStamina()));
        statsTotal.setArmor(StatConversion.applyMultiplier(statsTotal.getArmor(), statsTotal.getBaseArmorMultiplier()));
        statsTotal.setAttackPower(StatConversion.applyMultiplier(statsTotal.getAttackPower(), statsTotal.getBonusAttackPowerMultiplier()));
        statsTotal.setBonusArmor(StatConversion.applyMultiplier(statsTotal.getBonusArmor(), statsTotal.getBonusArmorMultiplier()));

        float AgiArmor = StatConversion.getArmorFromAgility(statsTotal.getAgility()); // Don't multiply the armor from agility.
        statsTotal.setArmor(statsTotal.getArmor() + statsTotal.getBonusArmor() + AgiArmor);
        statsTotal.setHealth(statsTotal.getHealth() + StatConversion.getHealthFromStamina(statsTotal.getStamina()));

        StatConversion.applyMultiplier(statsTotal.getHealth(), statsTotal.getBonusHealthMultiplier());

        // Talent: BladedArmor
        if (iBladedArmor > 0) {
            statsTotal.setAttackPower(statsTotal.getAttackPower() + (statsTotal.getArmor() / 180f) * (float) iBladedArmor);
        }
        // AP, crit, etc.  already being factored in w/ multiplier.
        statsTotal.setAttackPower(statsTotal.getAttackPower() + StatConversion.applyMultiplier((statsTotal.getStrength() * 2), statsTotal.getBonusAttackPowerMultiplier()));

        // Parry from str. is only available to DKs.
        statsTotal.setParryRating(statsTotal.getParryRating() + statsTotal.getStrength() * 0.25f);
    }

    /** Process All the ratings score to their base values. */
    private void processRatings(Stats statsTotal) {
        statsTotal.setPhysicalCrit(StatConversion.applyMultiplier(statsTotal.getPhysicalCrit()
                + StatConversion.getCritFromAgility(statsTotal.getAgility(), CharacterClass.DeathKnight)
                + StatConversion.getCritFromRating(statsTotal.getCritRating()), statsTotal.getBonusCritMultiplier()));
        statsTotal.setSpellCrit(StatConversion.applyMultiplier(statsTotal.getSpellCrit() + statsTotal.getSpellCritOnTarget()
                + StatConversion.getCritFromRating(statsTotal.getCritRating()), statsTotal.getBonusSpellCritMultiplier()));

        statsTotal.setPhysicalHit(statsTotal.getPhysicalHit() + StatConversion.getHitFromRating(statsTotal.getHitRating(), CharacterClass.DeathKnight));
        statsTotal.setSpellHit(statsTotal.getSpellHit() + StatConversion.getSpellHitFromRating(statsTotal.getHitRating()));

        // Expertise Rating -> Expertise:
        statsTotal.setExpertise(statsTotal.getExpertise() + StatConversion.getExpertiseFromRating(statsTotal.getExpertiseRating()));

        statsTotal.setArmorPenetration(statsTotal.getArmorPenetration() + StatConversion.getArmorPenetrationFromRating(statsTotal.getArmorPenetrationRating()));
    }

    private void processAvoidance(Stats statsTotal, int iTargetLevel) {
        // Get all the character avoidance numbers including deminishing returns.
        // Iterate through each hit type. and use fAvoidance array w/ the hitresult enum.
        float[] fAvoidance = new float[HitResultCount];
        Character c = new Character();
        c.setCharacterClass(CharacterClass.DeathKnight);
        for (int i = 0; i < HitResultCount; i++) {
            // GetDRAvoidanceChance returns a dec. percentage.
            // Since CurrentAvoidance is a percent, need to multiply by 100.
            fAvoidance[i] = (StatConversion.getDRAvoidanceChance(c, statsTotal, HitResult.values()[i], iTargetLevel));
        }

        // So let's populate the miss, dodge and parry values for the UI display as well as pulling them out of the avoidance number.
        statsTotal.setMiss(Math.min((StatConversion.CAP_MISSED[CharacterClass.DeathKnight.value()] / 100), fAvoidance[HitResult.Miss.ordinal()]));
        statsTotal.setDodge(Math.min((StatConversion.CAP_DODGE[CharacterClass.DeathKnight.value()] / 100), fAvoidance[HitResult.Dodge.ordinal()]));
        statsTotal.setParry(Math.min((StatConversion.CAP_PARRY[CharacterClass.DeathKnight.value()] / 100), fAvoidance[HitResult.Parry.ordinal()]));
    }

    /** Pass in the total stats object we're working with and add specific stat modifiers. */
    private void accumulateFrostPresence(Stats s) {
        s.setBaseArmorMultiplier(addStatMultiplierStat(s.getBaseArmorMultiplier(), .6f)); // Bonus armor for Frost Presence down from 80% to 60% as of 3.1.3
        s.setBonusStaminaMultiplier(addStatMultiplierStat(s.getBonusStaminaMultiplier(), .08f)); // Bonus 8% Stamina
        s.setDamageTakenMultiplier(addStatMultiplierStat(s.getDamageTakenMultiplier(), -.08f));// Bonus of 8% damage reduced for frost presence. up from 5% for 3.2.2
    }

    /** Build the talent special effects. */
    private void accumulateTalents(Stats FullCharacterStats, Character character) {
        Stats newStats = new Stats();
        float fDamageDone = 0f;

        // Blood Talents
        // Butchery
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
            newStats = new Stats();
            newStats.setBonusPhysicalDamageMultiplier(.01f * character.getDeathKnightTalents().getBloodyVengeance());
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
            // I left it open like this so that your above TODO for figuring out how much
            // the worms do will make this part dynamic
            if (!seBloodworms.containsKey(key)) {
                seBloodworms.put(key, new SpecialEffect[] {
                        null,
                        new SpecialEffect(Trigger.PhysicalHit, healed(((fDamageDone * fBWDuration / fBWAttackSpeed) * 1.5f)), fBWDuration, 0, .03f * 1),
                        new SpecialEffect(Trigger.PhysicalHit, healed(((fDamageDone * fBWDuration / fBWAttackSpeed) * 1.5f)), fBWDuration, 0, .03f * 2),
                        new SpecialEffect(Trigger.PhysicalHit, healed(((fDamageDone * fBWDuration / fBWAttackSpeed) * 1.5f)), fBWDuration, 0, .03f * 3),
                });
            }
            FullCharacterStats.addSpecialEffect(seBloodworms.get(key)[character.getDeathKnightTalents().getBloodworms()]);
        }

        // Sudden Doom
        // BS & HS have a 5% per point chance to launch a DC at target
        // Implemented in CombatTable.cs

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
        if (character.getDeathKnightTalents().getToughness() > 0) {
            FullCharacterStats.setBaseArmorMultiplier(addStatMultiplierStat(FullCharacterStats.getBaseArmorMultiplier(), (.02f * character.getDeathKnightTalents().getToughness()))); // Patch 3.2
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
            }
            FullCharacterStats.setBonusFrostStrikeDamage(FullCharacterStats.getBonusFrostStrikeDamage() + fBonus);
            FullCharacterStats.setBonusBloodStrikeDamage(FullCharacterStats.getBonusBloodStrikeDamage() + fBonus);
        }

        // Unbreakable Armor
        // Reinforces your armor with a thick coat of ice, Increasing Armor by 25% and increasing your Strength by 20% for 20 sec.
        if (character.getDeathKnightTalents().getUnbreakableArmor() > 0) {
            // As per wowhead: GlyphofUnbreakableArmor
            // Effect: Apply Aura: Add % Modifier (3) Value: 20
            FullCharacterStats.addSpecialEffect(seUnbreakableArmor[character.getDeathKnightTalents().isGlyphofUnbreakableArmor() ? 1 : 0][m_bT9_4PC ? 1 : 0]);
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
        newStats = new Stats();
        newStats.setDamageTakenMultiplier(-1f * ibfReduction);
        FullCharacterStats.addSpecialEffect(new SpecialEffect(Trigger.Use, newStats, fIBFDur, 120)); // Patch 3.2

        // Tundra Stalker
        // Your spells & abilities deal 3% per point more damage to targets w/ FF
        // Increases Expertise by 1 per point
        if (character.getDeathKnightTalents().getTundraStalker() > 0) {
            // Assuming FF is always up.
            FullCharacterStats.setBonusDamageMultiplier(FullCharacterStats.getBonusDamageMultiplier() + .03f * character.getDeathKnightTalents().getTundraStalker());
            FullCharacterStats.setExpertise(FullCharacterStats.getExpertise() + 1f * character.getDeathKnightTalents().getTundraStalker());
        }

        // UnHoly Talents
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
            // Ghouls don't help tank here.
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

    private static Stats healed(float value) {
        Stats s = new Stats();
        s.setHealed(value);
        return s;
    }

    private float getCurrentHealth(Stats FullCharacterStats) {
        return StatConversion.applyMultiplier((FullCharacterStats.getHealth() + StatConversion.getHealthFromStamina(FullCharacterStats.getStamina())), FullCharacterStats.getBonusHealthMultiplier());
    }

    /**
     * Gets data to fill a custom chart, based on the chart name, as defined in CustomChartNames.
     */
    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        return new ComparisonCalculationBase[0];
    }

    public Stats getRelevantStats(Stats stats) {
        Stats s = new Stats();
        s.setStrength(stats.getStrength());
        s.setAgility(stats.getAgility());
        s.setBaseAgility(stats.getBaseAgility());
        s.setStamina(stats.getStamina());
        s.setArmor(stats.getArmor());
        s.setBonusArmor(stats.getBonusArmor());
        s.setHealth(stats.getHealth());
        s.setBattlemasterHealth(stats.getBattlemasterHealth());

        s.setHighestStat(stats.getHighestStat());
        s.setParagon(stats.getParagon());
        s.setDeathbringerProc(stats.getDeathbringerProc());

        s.setDefenseRating(stats.getDefenseRating());
        s.setParryRating(stats.getParryRating());
        s.setDodgeRating(stats.getDodgeRating());

        s.setDefense(stats.getDefense());
        s.setDodge(stats.getDodge());
        s.setParry(stats.getParry());
        s.setMiss(stats.getMiss());

        s.setResilience(stats.getResilience());
        s.setSpellPenetration(stats.getSpellPenetration());

        s.setDamageAbsorbed(stats.getDamageAbsorbed());
        s.setAttackPower(stats.getAttackPower());
        s.setHitRating(stats.getHitRating());
        s.setCritRating(stats.getCritRating());
        s.setArmorPenetration(stats.getArmorPenetration());
        s.setArmorPenetrationRating(stats.getArmorPenetrationRating());
        s.setExpertiseRating(stats.getExpertiseRating());
        s.setExpertise(stats.getExpertise());
        s.setHasteRating(stats.getHasteRating());
        s.setWeaponDamage(stats.getWeaponDamage());
        s.setPhysicalCrit(stats.getPhysicalCrit());
        s.setPhysicalHaste(stats.getPhysicalHaste());
        s.setPhysicalHit(stats.getPhysicalHit());
        s.setSpellHit(stats.getSpellHit());

        s.setHealed(stats.getHealed());
        s.setHealthRestore(stats.getHealthRestore());
        s.setHealthRestoreFromMaxHealth(stats.getHealthRestoreFromMaxHealth());
        s.setHp5(stats.getHp5());

        s.setBonusHealthMultiplier(stats.getBonusHealthMultiplier());
        s.setBonusStrengthMultiplier(stats.getBonusStrengthMultiplier());
        s.setBonusStaminaMultiplier(stats.getBonusStaminaMultiplier());
        s.setBonusAgilityMultiplier(stats.getBonusAgilityMultiplier());
        s.setBonusAttackPowerMultiplier(stats.getBonusAttackPowerMultiplier());
        s.setBonusCritMultiplier(stats.getBonusCritMultiplier());
        s.setBonusSpellCritMultiplier(stats.getBonusSpellCritMultiplier());
        s.setBonusDamageMultiplier(stats.getBonusDamageMultiplier());
        s.setBonusPhysicalDamageMultiplier(stats.getBonusPhysicalDamageMultiplier());
        s.setBaseArmorMultiplier(stats.getBaseArmorMultiplier());
        s.setBonusArmorMultiplier(stats.getBonusArmorMultiplier());
        s.setDamageTakenMultiplier(stats.getDamageTakenMultiplier());

        s.setThreatIncreaseMultiplier(stats.getThreatIncreaseMultiplier());
        s.setThreatReductionMultiplier(stats.getThreatReductionMultiplier());

        // General Damage Mods.
        s.setBonusShadowDamageMultiplier(stats.getBonusShadowDamageMultiplier());
        s.setBonusFrostDamageMultiplier(stats.getBonusFrostDamageMultiplier());
        s.setBonusDiseaseDamageMultiplier(stats.getBonusDiseaseDamageMultiplier());
        s.setBonusRuneStrikeMultiplier(stats.getBonusRuneStrikeMultiplier());
        s.setBonusHeartStrikeDamageMultiplier(stats.getBonusHeartStrikeDamageMultiplier());
        s.setBonusBloodStrikeDamageMultiplier(stats.getBonusBloodStrikeDamageMultiplier());

        // Ability mods.
        s.setBonusBloodStrikeDamage(stats.getBonusBloodStrikeDamage());
        s.setBonusDeathCoilDamage(stats.getBonusDeathCoilDamage());
        s.setBonusDeathStrikeDamage(stats.getBonusDeathStrikeDamage());
        s.setBonusFrostStrikeDamage(stats.getBonusFrostStrikeDamage());
        s.setBonusHeartStrikeDamage(stats.getBonusHeartStrikeDamage());
        s.setBonusIcyTouchDamage(stats.getBonusIcyTouchDamage());
        s.setBonusObliterateDamage(stats.getBonusObliterateDamage());
        s.setBonusScourgeStrikeDamage(stats.getBonusScourgeStrikeDamage());
        s.setBonusHowlingBlastDamage(stats.getBonusHowlingBlastDamage());
        s.setBonusFrostWeaponDamage(stats.getBonusFrostWeaponDamage());

        s.setBonusPerDiseaseBloodStrikeDamage(stats.getBonusPerDiseaseBloodStrikeDamage());
        s.setBonusPerDiseaseHeartStrikeDamage(stats.getBonusPerDiseaseHeartStrikeDamage());
        s.setBonusPerDiseaseObliterateDamage(stats.getBonusPerDiseaseObliterateDamage());
        s.setBonusPerDiseaseScourgeStrikeDamage(stats.getBonusPerDiseaseScourgeStrikeDamage());

        s.setBonusDeathCoilCrit(stats.getBonusDeathCoilCrit());
        s.setBonusDeathStrikeCrit(stats.getBonusDeathStrikeCrit());
        s.setBonusFrostStrikeCrit(stats.getBonusFrostStrikeCrit());
        s.setBonusObliterateCrit(stats.getBonusObliterateCrit());
        s.setBonusPlagueStrikeCrit(stats.getBonusPlagueStrikeCrit());
        s.setBonusScourgeStrikeCrit(stats.getBonusScourgeStrikeCrit());

        s.setBonusIceboundFortitudeDuration(stats.getBonusIceboundFortitudeDuration());
        s.setBonusAntiMagicShellDamageReduction(stats.getBonusAntiMagicShellDamageReduction());

        s.setBonusHealingReceived(stats.getBonusHealingReceived());
        s.setRPp5(stats.getRPp5());
        s.setTankDK_T10_2pc(stats.getTankDK_T10_2pc());
        s.setTankDK_T10_4pc(stats.getTankDK_T10_4pc());

        // Resistances
        s.setArcaneResistance(stats.getArcaneResistance());
        s.setFireResistance(stats.getFireResistance());
        s.setFrostResistance(stats.getFrostResistance());
        s.setNatureResistance(stats.getNatureResistance());
        s.setShadowResistance(stats.getShadowResistance());

        // Damage Procs
        s.setArcaneDamage(stats.getArcaneDamage());
        s.setFireDamage(stats.getFireDamage());
        s.setFrostDamage(stats.getFrostDamage());
        s.setShadowDamage(stats.getShadowDamage());
        s.setHolyDamage(stats.getHolyDamage());
        s.setNatureDamage(stats.getNatureDamage());

        // Also bringing in the trigger events from DPSDK -
        // Since I'm going to move the +Def bonus for the Sigil of the Unfaltering Knight
        // To a special effect.  Also there are alot of OnUse and OnEquip special effects
        // That probably aren't being taken into effect.
        for (SpecialEffect effect : stats.specialEffects()) {
            if (hasRelevantStats(effect.getStats())) {
                s.addSpecialEffect(effect);
            }
        }
        return s;
    }

    /**
     * Tests whether there are positive relevant stats in the Stats object.
     */
    @Override
    public boolean hasRelevantStats(Stats stats) {
        for (SpecialEffect effect : stats.specialEffects()) {
            if (relevantStats(effect.getStats())) {
                if (effect.getTrigger() == Trigger.DamageDone ||
                        effect.getTrigger() == Trigger.DamageOrHealingDone ||
                        effect.getTrigger() == Trigger.DamageTaken ||
                        effect.getTrigger() == Trigger.DamageTakenMagical ||
                        effect.getTrigger() == Trigger.DamageTakenPhysical ||
                        effect.getTrigger() == Trigger.DamageSpellCast ||
                        effect.getTrigger() == Trigger.DamageSpellCrit ||
                        effect.getTrigger() == Trigger.DamageSpellHit ||
                        effect.getTrigger() == Trigger.SpellCast ||
                        effect.getTrigger() == Trigger.SpellCrit ||
                        effect.getTrigger() == Trigger.SpellHit ||
                        effect.getTrigger() == Trigger.DoTTick ||
                        effect.getTrigger() == Trigger.MeleeCrit ||
                        effect.getTrigger() == Trigger.MeleeHit ||
                        effect.getTrigger() == Trigger.OffHandHit ||
                        effect.getTrigger() == Trigger.CurrentHandHit ||
                        effect.getTrigger() == Trigger.MainHandHit ||
                        effect.getTrigger() == Trigger.PhysicalCrit ||
                        effect.getTrigger() == Trigger.PhysicalHit ||
                        effect.getTrigger() == Trigger.Use) {
                    return relevantStats(effect.getStats());
                }
                // if it has a DK specific trigger, then just return true.
                else if (
                        effect.getTrigger() == Trigger.BloodStrikeHit ||
                                effect.getTrigger() == Trigger.HeartStrikeHit ||
                                effect.getTrigger() == Trigger.BloodStrikeOrHeartStrikeHit ||
                                effect.getTrigger() == Trigger.IcyTouchHit ||
                                effect.getTrigger() == Trigger.PlagueStrikeHit ||
                                effect.getTrigger() == Trigger.RuneStrikeHit ||
                                effect.getTrigger() == Trigger.ObliterateHit ||
                                effect.getTrigger() == Trigger.ScourgeStrikeHit) {
                    return true;
                }
            }
        }
        return relevantStats(stats);
    }

    /** Helper function for HasRelevantStats() function of the base class. */
    private boolean relevantStats(Stats stats) {
        boolean bResults = false;
        // Core stats
        bResults |= (stats.getStrength() != 0);
        bResults |= (stats.getAgility() != 0);
        bResults |= (stats.getStamina() != 0);
        bResults |= (stats.getArmor() != 0);
        bResults |= (stats.getBonusArmor() != 0);
        bResults |= (stats.getHealth() != 0);
        bResults |= (stats.getBattlemasterHealth() != 0);

        bResults |= (stats.getHighestStat() != 0);
        bResults |= (stats.getParagon() != 0);
        bResults |= (stats.getDeathbringerProc() != 0);

        // Defense stats
        bResults |= (stats.getDodgeRating() != 0);
        bResults |= (stats.getDefenseRating() != 0);
        bResults |= (stats.getParryRating() != 0);

        bResults |= (stats.getDodge() != 0);
        bResults |= (stats.getParry() != 0);
        bResults |= (stats.getMiss() != 0);
        bResults |= (stats.getDefense() != 0);
        bResults |= (stats.getDamageAbsorbed() != 0);

        bResults |= (stats.getResilience() != 0);
        bResults |= (stats.getSpellPenetration() != 0);

        // Offense stats
        bResults |= (stats.getAttackPower() != 0);
        bResults |= (stats.getHitRating() != 0);
        bResults |= (stats.getCritRating() != 0);
        bResults |= (stats.getArmorPenetration() != 0);
        bResults |= (stats.getArmorPenetrationRating() != 0);
        bResults |= (stats.getExpertiseRating() != 0);
        bResults |= (stats.getExpertise() != 0);
        bResults |= (stats.getHasteRating() != 0);
        bResults |= (stats.getWeaponDamage() != 0);
        bResults |= (stats.getPhysicalCrit() != 0);
        bResults |= (stats.getPhysicalHaste() != 0);
        bResults |= (stats.getPhysicalHit() != 0);
        bResults |= (stats.getSpellHit() != 0);

        bResults |= (stats.getHealed() != 0);
        bResults |= (stats.getHealthRestore() != 0);
        bResults |= (stats.getHealthRestoreFromMaxHealth() != 0);
        bResults |= (stats.getHp5() != 0);

        // Bonus to stats
        bResults |= (stats.getBonusArmorMultiplier() != 0);
        bResults |= (stats.getBaseArmorMultiplier() != 0);
        bResults |= (stats.getBonusHealthMultiplier() != 0);
        bResults |= (stats.getBonusStrengthMultiplier() != 0);
        bResults |= (stats.getBonusStaminaMultiplier() != 0);
        bResults |= (stats.getBonusAgilityMultiplier() != 0);
        bResults |= (stats.getBonusCritMultiplier() != 0);
        bResults |= (stats.getBonusSpellCritMultiplier() != 0);
        bResults |= (stats.getBonusAttackPowerMultiplier() != 0);
        bResults |= (stats.getBonusPhysicalDamageMultiplier() != 0);
        bResults |= (stats.getBonusDamageMultiplier() != 0);
        bResults |= (stats.getDamageTakenMultiplier() != 0);
        bResults |= (stats.getThreatIncreaseMultiplier() != 0);
        bResults |= (stats.getThreatReductionMultiplier() != 0);

        // Damage Multipliers:
        bResults |= (stats.getBonusShadowDamageMultiplier() != 0);
        bResults |= (stats.getBonusFrostDamageMultiplier() != 0);
        bResults |= (stats.getBonusDiseaseDamageMultiplier() != 0);
        bResults |= (stats.getBonusRuneStrikeMultiplier() != 0);
        bResults |= (stats.getBonusBloodStrikeDamageMultiplier() != 0);
        bResults |= (stats.getBonusHeartStrikeDamageMultiplier() != 0);

        // Bulk Damage:
        bResults |= (stats.getBonusBloodStrikeDamage() != 0);
        bResults |= (stats.getBonusDeathCoilDamage() != 0);
        bResults |= (stats.getBonusDeathStrikeDamage() != 0);
        bResults |= (stats.getBonusFrostStrikeDamage() != 0);
        bResults |= (stats.getBonusHeartStrikeDamage() != 0);
        bResults |= (stats.getBonusIcyTouchDamage() != 0);
        bResults |= (stats.getBonusObliterateDamage() != 0);
        bResults |= (stats.getBonusScourgeStrikeDamage() != 0);
        bResults |= (stats.getBonusHowlingBlastDamage() != 0);
        bResults |= (stats.getBonusFrostWeaponDamage() != 0);

        bResults |= (stats.getBonusPerDiseaseBloodStrikeDamage() != 0);
        bResults |= (stats.getBonusPerDiseaseHeartStrikeDamage() != 0);
        bResults |= (stats.getBonusPerDiseaseObliterateDamage() != 0);
        bResults |= (stats.getBonusPerDiseaseScourgeStrikeDamage() != 0);

        // Others
        bResults |= (stats.getBonusDeathCoilCrit() != 0);
        bResults |= (stats.getBonusDeathStrikeCrit() != 0);
        bResults |= (stats.getBonusFrostStrikeCrit() != 0);
        bResults |= (stats.getBonusObliterateCrit() != 0);
        bResults |= (stats.getBonusPlagueStrikeCrit() != 0);
        bResults |= (stats.getBonusScourgeStrikeCrit() != 0);
        bResults |= (stats.getBonusIceboundFortitudeDuration() != 0);
        bResults |= (stats.getBonusAntiMagicShellDamageReduction() != 0);
        bResults |= (stats.getBonusHealingReceived() != 0);
        bResults |= (stats.getRPp5() != 0);
        bResults |= (stats.getTankDK_T10_2pc() != 0);
        bResults |= (stats.getTankDK_T10_4pc() != 0);

        // Resistances
        bResults |= (stats.getArcaneResistance() != 0);
        bResults |= (stats.getFireResistance() != 0);
        bResults |= (stats.getFrostResistance() != 0);
        bResults |= (stats.getNatureResistance() != 0);
        bResults |= (stats.getShadowResistance() != 0);

        // Damage Procs
        bResults |= (stats.getArcaneDamage() != 0);
        bResults |= (stats.getFireDamage() != 0);
        bResults |= (stats.getFrostDamage() != 0);
        bResults |= (stats.getShadowDamage() != 0);
        bResults |= (stats.getHolyDamage() != 0);
        bResults |= (stats.getNatureDamage() != 0);

        return bResults;
    }

    @Override
    public boolean isItemRelevant(Item item) {
        if (item.getSlot() == ItemSlot.Ranged && item.getType() != ItemType.Sigil) { return false; }
        return super.isItemRelevant(item);
    }

    /** Evaluate how many swings until the tank is next hit. */
    private float getReactionTime(float PercAvoidance) {
        float fReactionTime = 0f;
        // check args.
        if (PercAvoidance < 0f || PercAvoidance > 1f) { return 0f; }// error
        fReactionTime = 1f / (1f - PercAvoidance);
        return fReactionTime;
    }

    /** Evaluate how many swings until the tank dies. */
    private float getBurstTime(float PercAvoidance, float EffectiveHealth, float RawPerHit) {
        float fBurstTime = 0f;
        // check args.
        if (PercAvoidance < 0 || PercAvoidance > 1) { return 0f; } // error

        float fHvH = (EffectiveHealth / RawPerHit);

        fBurstTime = (1f / PercAvoidance) * ((1f / (float) Math.pow((1f - PercAvoidance), fHvH)) - 1f);

        return fBurstTime;
    }

    /** Get the value for a sub-component of Survival. */
    private float getEffectiveHealth(float fHealth, float fDR, float PercValue) {
        // TotalSurvival == sum(Survival for each school)
        // Survival = (Health / (1 - DR)) * % damage inc from that school
        if (0f <= PercValue && PercValue <= 1f && fDR < 1f)
            return (fHealth / (1 - fDR)) * PercValue;
        else
            return 0;
    }

    /** Get the MitigationRating of the current setup. */
    private float getMitigationRating() {
        return 0f;
    }

    /** Get the Threat Rating of the current setup. */
    private float getThreatRating() {
        return 0f;
    }

    private float getDPS(float fPerUnitDamage, float fDamFrequency) {
        if (fDamFrequency > 0)
            return fPerUnitDamage / fDamFrequency;
        return 0f;
    }

    public static float addStatMultiplierStat(float statMultiplier, float newValue) {
        float updatedStatModifier = ((1 + statMultiplier) * (1 + newValue)) - 1f;
        return updatedStatModifier;
    }

    private float getParryHastedDPS(float fParryChance, float fCharacterShotCount, float fBossAverageAttackSpeed, float fRotDuration, float fPerShotPhysical) {
        float fPhyDamageDPS = 0f;
        // What was the max POTENTIAL hasted damage?
        // The number of shots taken * the chance to be parried.
        // can't be higher than cap.
        float localParryChance = Math.min(fParryChance, StatConversion.WHITE_PARRY_CHANCE_CAP[83 - 80]);
        // can't be lower than 0
        localParryChance = Math.max(localParryChance, 0);
        float fShotsParried = fParryChance * fCharacterShotCount;
        float fBossParryHastedSpeed = fBossAverageAttackSpeed * (1f - 0.24f);

        float fTimeHasted = fShotsParried * fBossParryHastedSpeed;
        float fTimeNormal = fRotDuration - fTimeHasted;
        // Update the shot count w/ the new # of normal shots + the number of hasted shots.
        float fBossShotCountPerRot = (fTimeNormal / fBossAverageAttackSpeed) + fShotsParried;
        // How much DPS is the hasted amount?
        fPhyDamageDPS = (float) Math.floor((fBossShotCountPerRot * fPerShotPhysical) / fRotDuration);
        return fPhyDamageDPS;
    }

    private float getParryHastedAttackSpeed(float fParryChance, float fCharacterShotCount, float fBossAverageAttackSpeed, float fRotDuration) {
        // What was the max POTENTIAL hasted damage?
        // The number of shots taken * the chance to be parried.
        // can't be higher than cap.
        float localParryChance = Math.min(fParryChance, StatConversion.WHITE_PARRY_CHANCE_CAP[83 - 80]);
        // can't be lower than 0
        localParryChance = Math.max(localParryChance, 0);
        float fShotsParried = fParryChance * fCharacterShotCount;
        float fBossParryHastedSpeed = fBossAverageAttackSpeed * (1f - 0.24f);

        float fTimeHasted = fShotsParried * fBossParryHastedSpeed;
        float fTimeNormal = fRotDuration - fTimeHasted;
        // Update the shot count w/ the new # of normal shots + the number of hasted shots.
        float fBossShotCountPerRot = (fTimeNormal / fBossAverageAttackSpeed) + fShotsParried;
        // How much DPS is the hasted amount?
        return (fRotDuration / fBossShotCountPerRot);
    }

    /** Deserializes the model's CalculationOptions data object from xml. */
    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            CalculationOptionsTankDK calcOpts = new CalculationOptionsTankDK();
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
            if (method.getParameterCount() == 1 && method.getName().equalsIgnoreCase(name)) {
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
        return null;
    }

    public static CalculationsTankDK instance;

    public static CalculationsTankDK getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("TankDK", CalculationsTankDK.class);
    }

    public static void register() {
        Calculations.registerModel("TankDK", CalculationsTankDK.class);
    }

    @Override
    public String getOverallRatingLabel() {
        return "Overall Points";
    }
}
