// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.RestoSham/CalculationsRestoSham.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: UI-only members omitted (CalculationOptionsPanel, RenderCustomChart); non-Silverlight
// GetCustomChartData/CustomChartNames are ported. The C# RestoShamConfiguration.SubPointNameColors
// table is ported additively as getSubPointNameColors(); RestoShamConfiguration's
// CharacterDisplayCalculationLabels array is ported additively as
// getCharacterDisplayCalculationLabels() (web display order/grouping only).
// FRAMEWORK ADAPTATION: DeserializeDataObject uses a JDK-XML reflective reader instead of XmlSerializer
// (same approach as CalculationsMage, without the Mage-specific replacements).
package rawr.models.restosham;

import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterRace;
import rawr.base.character.CharacterSlot;
import rawr.base.calculations.ComparisonCalculationBase;
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

public class CalculationsRestoSham extends CalculationsBase {
    //  Base mana at level 80
    static final float BaseMana = 4396f;

    // Carry over calculations
    private float healPerSec;

    public float getHealPerSec() {
        return healPerSec;
    }

    public void setHealPerSec(float value) {
        healPerSec = value;
    }

    private float healHitPerSec;

    public float getHealHitPerSec() {
        return healHitPerSec;
    }

    public void setHealHitPerSec(float value) {
        healHitPerSec = value;
    }

    private float critPerSec;

    public float getCritPerSec() {
        return critPerSec;
    }

    public void setCritPerSec(float value) {
        critPerSec = value;
    }

    private float fightSeconds;

    public float getFightSeconds() {
        return fightSeconds;
    }

    public void setFightSeconds(float value) {
        fightSeconds = value;
    }

    private float castingActivity;

    public float getCastingActivity() {
        return castingActivity;
    }

    public void setCastingActivity(float value) {
        castingActivity = value;
    }

    public static CalculationsRestoSham instance;

    public static CalculationsRestoSham getInstance() {
        return instance;
    }

    static {
        Calculations.registerModel("RestoSham", CalculationsRestoSham.class);
    }

    public static void register() {
        Calculations.registerModel("RestoSham", CalculationsRestoSham.class);
    }

    public CalculationsRestoSham() {
        instance = this;
    }

    // C# RestoSham.Base.cs:128-133 RestoShamConfiguration.SubPointNameColors, aligned with
    // getSubPoints() ([0] = Burst, [1] = Sustained, [2] = Survival).
    @Override
    public LinkedHashMap<String, String> getSubPointNameColors() {
        LinkedHashMap<String, String> colors = new LinkedHashMap<>();
        colors.put("Burst", "#FF0000");
        colors.put("Sustained", "#0000FF");
        colors.put("Survival", "#008000");
        return colors;
    }

    private List<GemmingTemplate> defaultGemmingTemplates = null;

    // region Setup Character Defaults
    /**
     * Sets the defaults for a RestoShaman character
     */
    public void setDefaults(Character character) {
        character.activeBuffsAdd("Improved Moonkin Form");
        character.activeBuffsAdd("Tree of Life Aura");
        character.activeBuffsAdd("Arcane Intellect");
        character.activeBuffsAdd("Vampiric Touch");
        character.activeBuffsAdd("Mana Spring Totem");
        character.activeBuffsAdd("Restorative Totems");
        character.activeBuffsAdd("Moonkin Form");
        character.activeBuffsAdd("Wrath of Air Totem");
        character.activeBuffsAdd("Totem of Wrath (Spell Power)");
        character.activeBuffsAdd("Power Word: Fortitude");
        character.activeBuffsAdd("Improved Power Word: Fortitude");
        character.activeBuffsAdd("Mark of the Wild");
        character.activeBuffsAdd("Improved Mark of the Wild");
        character.activeBuffsAdd("Blessing of Kings");
        character.activeBuffsAdd("Flask of the Frost Wyrm");
        character.activeBuffsAdd("Spell Power Food");
        character.activeBuffsAdd("Earthliving Weapon");
    }

    public List<String> getRelevantGlyphs() {
        return Relevants.RelevantGlyphs;
    }

    @Override
    public List<GemmingTemplate> getDefaultGemmingTemplates() {
        if (this.defaultGemmingTemplates == null) {
            ////Relevant Gem IDs for Resto Shaman - Goes Normal, Rare, Epic, and JC
            //Red
            int[] Runed = { 39911, 39998, 40113, 42144 };

            //Purple
            int[] Royal = { 39943, 40027, 40134 };
            int[] Glowing = { 39936, 40025, 40132 };

            //Blue
            int[] Lustrous = { 39927, 40010, 40121, 42146 };

            //Green
            int[] Dazzling = { 39984, 40094, 40175 };
            int[] Energized = { 39989, 40105, 40179 };
            int[] Sundered = { 39985, 40096, 40176 };

            //Yellow
            int[] Brilliant = { 39912, 40012, 40123, 42148 };
            int[] Quick = { 39918, 40017, 40128, 42150 };
            int[] Smooth = { 39914, 40013, 40124, 42149 };

            //Orange
            int[] Luminous = { 39946, 40047, 40151 };
            int[] Reckless = { 39959, 40051, 40155 };
            int[] Potent = { 39956, 40048, 40152 };

            //Meta
            int Ember = 41333;
            int Insightful = 41401;
            int Revitalizing = 41376;

            this.defaultGemmingTemplates = new ArrayList<GemmingTemplate>();

            this.defaultGemmingTemplates.add(makeGemmingTemplate("Uncommon", false, Runed[0], Runed[0], Runed[0], Runed[0], Ember));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Uncommon", false, Runed[0], Luminous[0], Glowing[0], Runed[0], Ember));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Uncommon", false, Royal[0], Dazzling[0], Dazzling[0], Lustrous[0], Insightful));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Uncommon", false, Runed[0], Dazzling[0], Royal[0], Runed[0], Insightful));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Uncommon", false, Runed[0], Reckless[0], Energized[0], Quick[0], Revitalizing));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Uncommon", false, Runed[0], Potent[0], Sundered[0], Smooth[0], Revitalizing));

            this.defaultGemmingTemplates.add(makeGemmingTemplate("Rare", false, Runed[1], Runed[1], Runed[1], Runed[1], Ember));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Rare", false, Runed[1], Luminous[1], Glowing[1], Runed[1], Ember));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Rare", false, Royal[1], Dazzling[1], Dazzling[1], Lustrous[1], Insightful));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Rare", false, Runed[1], Dazzling[1], Royal[1], Runed[1], Insightful));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Rare", false, Runed[1], Reckless[1], Energized[1], Quick[1], Revitalizing));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Rare", false, Runed[1], Potent[1], Sundered[1], Smooth[1], Revitalizing));

            this.defaultGemmingTemplates.add(makeGemmingTemplate("Epic", true, Quick[2], Quick[2], Quick[2], Quick[2], Insightful));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Epic", true, Luminous[2], Luminous[2], Dazzling[2], Luminous[2], Insightful));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Epic", true, Reckless[2], Quick[2], Energized[2], Quick[2], Insightful));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Epic", true, Runed[2], Potent[2], Sundered[2], Smooth[2], Insightful));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Epic", true, Quick[2], Quick[2], Quick[2], Quick[2], Revitalizing));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Epic", true, Luminous[2], Luminous[2], Dazzling[2], Luminous[2], Revitalizing));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Epic", true, Reckless[2], Quick[2], Energized[2], Quick[2], Revitalizing));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Epic", true, Runed[2], Potent[2], Sundered[2], Smooth[2], Revitalizing));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Epic", false, Runed[2], Dazzling[2], Royal[2], Runed[2], Insightful));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Epic", false, Royal[2], Dazzling[2], Dazzling[2], Lustrous[2], Insightful));

            this.defaultGemmingTemplates.add(makeGemmingTemplate("Jeweler", false, Quick[3], Quick[3], Quick[3], Quick[3], Insightful));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Jeweler", false, Runed[3], Brilliant[3], Dazzling[2], Luminous[2], Insightful));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Jeweler", false, Reckless[2], Quick[3], Energized[2], Quick[3], Insightful));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Jeweler", false, Runed[3], Smooth[3], Sundered[2], Smooth[3], Insightful));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Jeweler", false, Quick[3], Quick[3], Quick[3], Quick[3], Revitalizing));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Jeweler", false, Runed[3], Brilliant[3], Dazzling[2], Luminous[2], Revitalizing));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Jeweler", false, Reckless[2], Quick[3], Energized[2], Quick[3], Revitalizing));
            this.defaultGemmingTemplates.add(makeGemmingTemplate("Jeweler", false, Runed[3], Smooth[3], Sundered[2], Smooth[3], Revitalizing));
        }
        return this.defaultGemmingTemplates;
    }

    private GemmingTemplate makeGemmingTemplate(String group, boolean enabled, int redId, int yellowId, int blueId, int prismaticId, int metaId) {
        GemmingTemplate template = new GemmingTemplate();
        template.setModel("RestoSham");
        template.setGroup(group);
        template.setEnabled(enabled);
        template.setRedId(redId);
        template.setYellowId(yellowId);
        template.setBlueId(blueId);
        template.setPrismaticId(prismaticId);
        template.setMetaId(metaId);
        return template;
    }

    @Override
    public List<ItemType> getRelevantItemTypes() {
        return Relevants.RelevantItemTypes;
    }
    // endregion

    // region Labels and Charts Overrides
    // C# RestoSham.Base.cs:92-122 RestoShamConfiguration.CharacterDisplayCalculationLabels, kept
    // verbatim in RestoShamConfiguration; this additive override exposes it as a List for the web
    // display order/grouping (tooltips included).
    @Override
    public List<String> getCharacterDisplayCalculationLabels() {
        return List.of(RestoShamConfiguration.CharacterDisplayCalculationLabels);
    }

    public String[] getOptimizableCalculationLabels() {
        return RestoShamConfiguration.OptimizableCalculationLabels;
    }

    public String[] getCustomChartNames() {
        return CustomCharts.getCustomChartNames();
    }

    // for RAWR3 include all charts in CustomChartNames
    public String[] getCustomRenderedChartNames() {
        return CustomCharts.CustomRenderedChartNames;
    }
    // endregion

    @Override
    public boolean itemFitsInSlot(Item item, Character character, CharacterSlot slot, boolean ignoreUnique) {
        if (slot == CharacterSlot.OffHand && item.getSlot() == ItemSlot.OneHand) return false;
        return super.itemFitsInSlot(item, character, slot, ignoreUnique);
    }

    // region Model Verification and prepare Calculations
    //
    // This model is for shammies!
    //
    @Override
    public CharacterClass getTargetClass() {
        return CharacterClass.Shaman;
    }

    //
    // Get instances of our calculation classes:
    //
    @Override
    public ComparisonCalculationBase createNewComparisonCalculation() {
        return new ComparisonCalculationRestoSham();
    }

    public CharacterCalculationsBase createNewCharacterCalculations() {
        return new CharacterCalculationsRestoSham();
    }
    // endregion

    // region Calculations Begin
    @Override
    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
        return getCharacterCalculations(character, additionalItem, null);
    }

    public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, Stats statModifier) {
        Stats stats = getCharacterStats(character, additionalItem, statModifier);
        CharacterCalculationsRestoSham calcStats = new CharacterCalculationsRestoSham();
        calcStats.setBasicStats(stats);
        CalculationOptionsRestoSham options = character.getCalculationOptions() instanceof CalculationOptionsRestoSham
                ? (CalculationOptionsRestoSham) character.getCalculationOptions()
                : null;
        if (options == null)
            return calcStats;

        fightSeconds = options.getFightLength() * 60f;
        castingActivity = options.getActivityPerc() * .01f;
        // region Spell Power and Haste Based Calcs
        stats.setSpellPower(stats.getSpellPower() + stats.getEarthliving() * ((1 + character.getShamanTalents().getElementalWeapons() * .1f) * 150f));
        if (options.getSustStyle().contains("CH"))
            stats.setSpellPower(stats.getSpellPower() + stats.getRestoShamRelicT9());
        if (options.getSustStyle().contains("RT"))
            stats.setSpellPower(stats.getSpellPower() + stats.getRestoShamRelicT10());
        stats.setSpellHaste((1 + StatConversion.getSpellHasteFromRating(stats.getHasteRating())) * (1 + stats.getSpellHaste()) - 1);
        //  FIXME: you can't really model Hero/BL as a flat haste increase since with decent amounts of haste from gear/buffs you'll begin hitting the GCD cap, which will skew a lot of computations
        /*
        if (options.Heroism.Equals("Me") || options.Heroism.Equals("Yes"))
            stats.SpellHaste = (1 + stats.SpellHaste) * (1 + .3f / FightSeconds) - 1;
        if (options.Heroism.Equals("Me"))
            stats.Mp5 -= .19f * BaseMana * 5 / FightSeconds;
         * */
        calcStats.setSpellHaste(stats.getSpellHaste());
        float Healing = 1.88f * stats.getSpellPower();
        // endregion
        // region Overhealing/Latency/Interval/CH jumps values
        float GcdLatency = options.getLatency() / 1000;
        float Latency = Math.max(Math.min(GcdLatency, 0.275f) - 0.14f, 0) + Math.max(GcdLatency - 0.275f, 0);
        float ESOverheal = 0;
        float ELWOverheal = 0;
        float CHOverheal = 0;
        float RTOverheal = 0;
        float HWOverheal = 0;
        float HWSelfOverheal = 0;
        float LHWOverheal = 0;
        float AAOverheal = 0;
        float HSTOverheal = 0;
        float ESInterval = 0;
        boolean TankHeal = options.getTargets().equals("Tank");
        boolean RaidHeal = options.getTargets().equals("Heavy Raid");
        boolean SelfHeal = RaidHeal || options.getTargets().equals("Self");
        float ELWOverwriteScale = RaidHeal ? 0.875f : TankHeal ? 0.5f : 0.6f;
        float CHRTConsumption = RaidHeal ? 0.07f : TankHeal ? 0.5f : 0.19f;
        float CHJumps = character.getShamanTalents().isGlyphofChainHeal()
                ? (RaidHeal ? 4 : SelfHeal ? 1.73f : TankHeal ? 1.86f : 2.5f)
                : (RaidHeal ? 3 : SelfHeal ? 1.3f : TankHeal ? 1.41f : 1.8f);
        float HSTTargets = RaidHeal ? 5f : 1f;
        // endregion
        // region Intellect and MP5 Based Calcs
        float CritPenalty = 1f - (((CHOverheal + RTOverheal + HWOverheal + HWSelfOverheal + LHWOverheal + AAOverheal) / 6f) / 2f);
        stats.setMp5(stats.getMp5() + (float) Math.rint((stats.getIntellect() * ((character.getShamanTalents().getUnrelentingStorm() / 3) * .1f))));
        stats.setSpellCrit(.022f + StatConversion.getSpellCritFromIntellect(stats.getIntellect())
                + StatConversion.getSpellCritFromRating(stats.getCritRating()) + stats.getSpellCrit()
                + (.01f * (character.getShamanTalents().getTidalMastery() + character.getShamanTalents().getThunderingStrikes()
                + (character.getShamanTalents().getBlessingOfTheEternals() * 2))));
        calcStats.setSpellCrit(stats.getSpellCrit());
        float CriticalScale = 1.5f * (1 + stats.getBonusCritHealMultiplier());
        float CriticalChance = calcStats.getSpellCrit();
        float Critical = 1f + ((CriticalChance * Math.min(CriticalScale - 1, 1)) * (CritPenalty));  //  The penalty is set to ensure that while no crit will ever be valued less then a full heal, it will however be reduced more so due to overhealing.  The average currently works out close to current HEP reports and combat logs.
        float ChCritical = 1f + (((CriticalChance + (stats.getRestoSham4T9() * .05f)) * Math.min(CriticalScale - 1, 1)) * (CritPenalty));

        // endregion
        // region Healing Bonuses and scales
        //  Cost scale
        float CostScale = 1f - character.getShamanTalents().getTidalFocus() * .01f;
        //  Healing scale from Purification
        float PurificationScale = 1 + character.getShamanTalents().getPurification() * .02f;
        //  AA scale
        float AAScale = CriticalScale * character.getShamanTalents().getAncestralAwakening() * .1f * PurificationScale;
        //  TW chance
        float TWChance = character.getShamanTalents().getTidalWaves() * 0.2f;
        // endregion
        // region Water Shield and Mana Calculations
        //// Code Flag: Dodger = Re-cast not needed in 3.2
        //float WSC = (float)Math.Max((1.5 * (1 - (calcStats.SpellHaste))), 1f) / 4;
        float Orb;
        if (options.getWaterShield()) {
            stats.setMp5(stats.getMp5() + (character.getShamanTalents().isGlyphofWaterMastery() ? 130 : 100)
                    + stats.getTotemThunderhead() * 2
                    + 100f * stats.getWaterShieldIncrease());
            Orb = (428 * (1 + (character.getShamanTalents().getImprovedShields() * .05f))) * (1 + stats.getWaterShieldIncrease())
                    + stats.getTotemThunderhead() * 27;
            Orb = Orb * character.getShamanTalents().getImprovedWaterShield() / 3;
        } else {
            Orb = 0;
        }
        // endregion
        // region Earthliving Weapon Calculations
        //  ELW bonus healing = spell power
        float ELWBonusHealing = stats.getSpellPower();
        //  ... * generic healing scale * HoT scale
        ELWBonusHealing *= 1.88f * 0.3636f;
        //  ELW healing scale = purification scale
        float ELWHealingScale = PurificationScale;
        float ELWHPS = ((652 + ELWBonusHealing) * ELWHealingScale / 12) * (1 + stats.getBonusHealingDoneMultiplier());
        float ELWChance = stats.getEarthliving() * (0.2f + (character.getShamanTalents().isGlyphofEarthlivingWeapon() ? .05f : 0)) * ELWOverwriteScale;
        // endregion
        // region Earth Shield Calculations
        boolean UseES = (options.getEarthShield() && character.getShamanTalents().getEarthShield() > 0); // Wether or not to use ES at all - Make sure the option and the talent are on.
        float ESBonusHealing = stats.getSpellPower();  //  ES bonus healing = spell power
        ESBonusHealing *= 1.88f * (1f / 3.5f);  //  ... * generic healing scale * HoT scale
        float ESHealingScale = PurificationScale;  //  ES healing scale = purification scale
        //  ... + 5%/10% Improved Earth Shield + 5%/10%/15% Improved Shields
        ESHealingScale *= 1 + character.getShamanTalents().getImprovedEarthShield() * 0.05f + character.getShamanTalents().getImprovedShields() * 0.05f;
        //  ... + 20% if Glyphed
        if (character.getShamanTalents().isGlyphofEarthShield())
            ESHealingScale *= 1.2f;
        float ESChargeHeal = (337 + ESBonusHealing) * ESHealingScale * (UseES ? 1 : 0); //  Heal per ES Charge
        float ESHeal = ESChargeHeal * (6 + character.getShamanTalents().getImprovedEarthShield());  //  ES if all charges heal
        float ESCost = (float) Math.rint(.15 * BaseMana) * CostScale * (UseES ? 1 : 0);
        float ESTimer = (6 + character.getShamanTalents().getImprovedEarthShield()) * Math.max(ESInterval, 4);
        calcStats.setESHPS(((ESHeal * Critical) / ESTimer) * (1 + stats.getBonusHealingDoneMultiplier()));
        float ESMPS = ESCost / ESTimer;
        // endregion
        // region Base Variables ( Heals per sec and Crits per sec )
        float RTPerSec = 0;
        float RTTicksPerSec = 0;
        float HWPerSec = 0;
        float CHPerSec = 0;
        float CHHitsPerSec = 0;
        float LHWPerSec = 0;
        float LHWCPerSec = 0f;
        float CHCPerSec = 0f;
        float CHCHitsPerSec = 0;
        float HWCPerSec = 0f;
        float RTCPerSec = 0f;
        float AAsPerSec = 0f;
        float ELWTicksPerSec = 0;
        // endregion
        // region Base Speeds ( Hasted / RTCast / LHWCast / HWCast / CHCast )
        float HasteScale = 1f / (1f + calcStats.getSpellHaste());
        float RTHaste = stats.getRestoSham2T10() * 0.2f;
        float RTCast = (float) Math.max(1.5f * HasteScale + Latency, 1f + GcdLatency);
        float RTCD = 6 - stats.getRTCDDecrease();
        float RTCDCast = RTCD + GcdLatency;
        float RTDuration = 15 + (character.getShamanTalents().isGlyphofRiptide() ? 6 : 0);
        float ELWDuration = stats.getEarthliving() * 12;
        float HWCastBase = 3.0f - (character.getShamanTalents().getImprovedHealingWave() * .1f);
        calcStats.setRealHWCast(HWCastBase * HasteScale);
        float HWCast = (float) Math.max(HWCastBase * HasteScale + Latency, 1f + GcdLatency);
        float HWCastTWLatency = (Latency * 0.25f + GcdLatency * 0.75f) * TWChance + (Latency * 0.5f + GcdLatency * 0.5f) * (1 - TWChance);
        float HWCastTW = (float) Math.max(HWCastBase * HasteScale * 0.7f + HWCastTWLatency, 1f + GcdLatency);
        float HWCast_RT = (float) Math.max(HWCastBase / (1f + calcStats.getSpellHaste() + RTHaste), 1f) + GcdLatency;
        float HWCastTW_RT = (float) Math.max(HWCastBase / (1f + calcStats.getSpellHaste() + RTHaste) * 0.7f + HWCastTWLatency, 1f + GcdLatency);
        calcStats.setRealLHWCast(1.5f * HasteScale);
        float LHWCast = (float) Math.max(1.5f * HasteScale, 1f) + GcdLatency;
        float LHWCast_RT = (float) Math.max(1.5f / (1f + calcStats.getSpellHaste() + RTHaste), 1f) + GcdLatency;
        float CHCastBase = 2.5f - stats.getCHCTDecrease();
        calcStats.setRealCHCast(CHCastBase * HasteScale);
        float CHCast = (float) Math.max(CHCastBase * HasteScale + Latency, 1f + GcdLatency);
        float CHCast_RT = (float) Math.max(CHCastBase / (1f + calcStats.getSpellHaste() + RTHaste), 1f) + GcdLatency;
        // This totally heals the boss backwards! Yeah! :D
        // Don't worry about this messing with procs or anything, it's just to show on the stats page. :)
        calcStats.setLBCast((float) Math.max((2.5f - 0.1 * character.getShamanTalents().getLightningMastery()) * HasteScale, 1f));
        // endregion
        // region Base Spells ( TankCH / RTHeal / LHWHeal / HWHeal / CHHeal )
        //  RT bonus healing = spell power
        float RTBonusHealing = stats.getSpellPower();
        //  ... * generic healing scale * HoT scale
        RTBonusHealing *= 1.88f * (1f / 3.5f);
        //  RT healing scale = purification scale
        float RTHealingScale = PurificationScale;
        //  ... + 20% 2pc T9 bonus
        RTHealingScale *= 1 + stats.getRestoSham2T9() * .2f;
        //  ... set to zero if RT talent is not taken
        float RTHeal = (1670 + RTBonusHealing) * RTHealingScale * character.getShamanTalents().getRiptide();
        //  RT HoT bonus healing = spell power
        float RTHotBonusHealing = stats.getSpellPower();
        //  ... * generic healing scale * HoT scale
        RTHotBonusHealing *= 1.88f * 0.5f;
        float RTHotHeal = (1670 + RTHotBonusHealing) * RTHealingScale * character.getShamanTalents().getRiptide();
        float RTHotTickHeal = RTHotHeal / 5;
        RTHotHeal = RTDuration / 3 * RTHotTickHeal;
        float RTHotHPS = RTHotTickHeal / 3;
        //  LHW bonus healing = spell power + totem spell power bonus
        float LHWBonusHealing = stats.getSpellPower() + stats.getTotemLHWSpellpower();
        //  ... * generic healing scale + bonus from TW
        LHWBonusHealing *= 1.88f * (1.5f / 3.5f) + character.getShamanTalents().getTidalWaves() * .02f;
        //  LHW healing scale = purification scale
        float LHWHealingScale = PurificationScale;
        //  ... + 20% if Glyphed, tank healing and ES is enabled
        if (character.getShamanTalents().isGlyphofLesserHealingWave() && TankHeal && UseES)
            LHWHealingScale *= 1.2f;
        float LHWHeal = (1720 + LHWBonusHealing) * LHWHealingScale;
        //  HW bonus healing = spell power + totem spell power bonus
        float HWBonusHealing = stats.getSpellPower() + stats.getTotemHWSpellpower();
        //  ... * generic healing scale + bonus from TW
        HWBonusHealing *= 1.88f * (3.0f / 3.5f) + character.getShamanTalents().getTidalWaves() * .04f;
        //  HW healing scale = purification scale
        float HWHealingScale = PurificationScale;
        //  ... + 8%/16%/25% Healing Way bonus
        HWHealingScale *= 1 + character.getShamanTalents().getHealingWay() * .25f / 3f;
        //  ... + 5% 4pc T7 bonus
        HWHealingScale *= 1 + stats.getCHHWHealIncrease();
        float HWHeal = (3250 + HWBonusHealing) * HWHealingScale;
        //  HW self-healing scale = 20% if w/Glyph (no longer benefits from Purification since patch 3.2)
        float HWSelfHealingScale = SelfHeal && character.getShamanTalents().isGlyphofHealingWave() ? 0.2f : 0;
        //      * correction due to the fact it's just not smart to use GoHW for self-healing if you're _really_ hammered down
        HWSelfHealingScale *= 1f / HWHealingScale;
        //  CH bonus healing = spell power + totem spell power bonus
        float CHBonusHealing = stats.getSpellPower() + stats.getTotemCHBaseHeal();
        //  ... * generic healing scale
        CHBonusHealing *= 1.88f * (2.5f / 3.5f);
        //  CH healing scale = purification scale
        float CHHealingScale = PurificationScale;
        //  ... + 5/10/15% Improved Chain Heal
        CHHealingScale *= 1f + character.getShamanTalents().getImprovedChainHeal() * .1f;
        //  ... + 5% 4pc T7 + HoT 4pc T10
        CHHealingScale *= 1f + stats.getCHHWHealIncrease() + .25f * CriticalChance * stats.getRestoSham4T10();
        //  NOTE: stats.CHHealIncrease isn't handled since WotLK items don't have it
        float CHHeal = (1130 + CHBonusHealing) * CHHealingScale;
        float CHJumpHeal = 0;
        float scale = 1f;
        int jump;
        for (jump = 0; jump < CHJumps; jump++) {
            CHJumpHeal += scale;
            scale *= 0.6f;
        }
        CHJumpHeal += scale * (CHJumps - jump);
        CHJumpHeal *= CHHeal;
        //  HST bonus healing = spell power
        float HSTBonusHealing = stats.getSpellPower();
        //      * generic healing scale * HoT scale
        HSTBonusHealing *= 1.88f * 0.044f;
        //  HST healing scale = purification scale
        float HSTHealingScale = PurificationScale;
        //      + 15%/30%/45% Restorative Totems + 20% w/Glyph
        HSTHealingScale *= 1 + character.getShamanTalents().getRestorativeTotems() * 0.15f + (character.getShamanTalents().isGlyphofHealingStreamTotem() ? 0.2f : 0f);
        // endregion
        // region Base Costs ( Preserve / RTCost / LHWCost / CHCost )
        float Preserve = stats.getManacostReduceWithin15OnHealingCast() * .02f;
        float RTCost = ((float) Math.rint(BaseMana * .18f) - Preserve) * CostScale;
        float LHWCost = ((float) Math.rint(BaseMana * .15f) - Preserve) * CostScale;
        float HWCost = ((float) Math.rint(BaseMana * .25f) - Preserve - stats.getTotemHWBaseCost()) * CostScale;
        float DecurseCost = ((float) Math.rint(BaseMana * .07f) - Preserve) * CostScale;
        //  NOTE: stats.CHManaReduction isn't handled since WotLK items don't have it
        float CHCost = ((float) Math.rint(BaseMana * .19f) - Preserve - stats.getTotemCHBaseCost()) * CostScale;
        // endregion
        // region RT + LHW Rotation (RTLHWMPS / RTLHWHPS / RTLHWTime)  (Adjusted based on Casting Activity)
        if (character.getShamanTalents().getRiptide() != 0) {
            float RTLHWTime = RTCast;
            float RTLHWRemainingTime = RTCDCast - RTLHWTime;
            int RTLHWLHWCasts = 0;
            if (RTLHWRemainingTime > GcdLatency) {
                RTLHWRemainingTime -= LHWCast_RT;
                RTLHWTime += LHWCast_RT;
                ++RTLHWLHWCasts;
                if (RTLHWRemainingTime > GcdLatency) {
                    int RTLHWLHWRemainingCasts = (int) Math.ceil((RTLHWRemainingTime - GcdLatency) / LHWCast);
                    RTLHWTime += RTLHWLHWRemainingCasts * LHWCast;
                    RTLHWLHWCasts += RTLHWLHWRemainingCasts;
                }
            }
            float RTLHWLHWCrits = Math.min(2, RTLHWLHWCasts) * Math.min(CriticalChance + TWChance * 0.25f, 1) + (RTLHWLHWCasts - 2) * CriticalChance;
            float RTLHWRTHeal = RTHeal * Critical;
            float RTLHWLHWCritHeal = LHWHeal * Critical;
            float RTLHWLHWHeal = (LHWHeal * (RTLHWLHWCasts - RTLHWLHWCrits)) + (RTLHWLHWCrits * RTLHWLHWCritHeal);
            float RTLHWAA = (RTHeal * CriticalChance + LHWHeal * RTLHWLHWCrits) * AAScale;
            float RTTargets = TankHeal ? 1 : RTDuration / RTLHWTime;
            float RTLHWELWTargets = ELWChance * (TankHeal ? 1 : RTLHWLHWCasts * ELWDuration / RTLHWTime);
            calcStats.setRTLHWHPS((((RTLHWRTHeal * (1 - RTOverheal) + RTLHWLHWHeal * (1 - LHWOverheal) + RTLHWAA * (1 - AAOverheal)) / RTLHWTime + RTTargets * RTHotHPS * (1 - RTOverheal) + RTLHWELWTargets * ELWHPS * (1 - ELWOverheal)) * castingActivity) * (1 + stats.getBonusHealingDoneMultiplier()));
            calcStats.setRTLHWMPS(((RTCost + (LHWCost * RTLHWLHWCasts)) / RTLHWTime) * castingActivity);
            if (options.getSustStyle().equals("RT+LHW")) {
                RTPerSec = 1f / RTLHWTime;
                RTTicksPerSec = RTTargets / 3;
                LHWPerSec = RTLHWLHWCasts / RTLHWTime;
                RTCPerSec = RTPerSec * CriticalChance;
                LHWCPerSec = RTLHWLHWCrits / RTLHWTime;
                AAsPerSec += (CriticalChance + RTLHWLHWCrits) / RTLHWTime;
                ELWTicksPerSec += RTLHWELWTargets;
            }
        }
        // endregion
        // region RT + HW Rotation (RTHWMPS / RTHWHPS / RTHWTime) (Adjusted based on Casting Activity)
        if (character.getShamanTalents().getRiptide() != 0) {
            float RTHWTime = RTCast;
            float RTHWRemainingTime = RTCDCast - RTHWTime;
            int RTHWHWCasts = 0;
            if (RTHWRemainingTime > GcdLatency) {
                float RTHWHWCastTW_RT = HWCastTW_RT * TWChance + HWCast_RT * (1 - TWChance);
                RTHWRemainingTime -= RTHWHWCastTW_RT;
                RTHWTime += RTHWHWCastTW_RT;
                ++RTHWHWCasts;
                if (RTHWRemainingTime > GcdLatency) {
                    float RTHWHWCastTW = HWCastTW * TWChance + HWCast * (1 - TWChance);
                    RTHWRemainingTime -= RTHWHWCastTW;
                    RTHWTime += RTHWHWCastTW;
                    ++RTHWHWCasts;
                    if (RTHWRemainingTime > GcdLatency) {
                        int RTHWHWRemainingCasts = (int) Math.ceil((RTHWRemainingTime - GcdLatency) / HWCast);
                        RTHWTime += RTHWHWRemainingCasts * HWCast;
                        RTHWHWCasts += RTHWHWRemainingCasts;
                    }
                }
            }
            float RTHWRTHeal = RTHeal * Critical;
            float RTHWHWHCrits = RTHWHWCasts * CriticalChance;
            float RTHWHWHeal = HWHeal * RTHWHWCasts * Critical;
            float RTHWHWSelfHeal = RTHWHWHeal * HWSelfHealingScale * Critical;
            float RTHWAA = (RTHeal * CriticalChance + HWHeal * RTHWHWHCrits) * AAScale;
            //  Multi-target ELW handling not in yet due to low priority
            float RTTargets = TankHeal ? 1 : RTDuration / RTHWTime;
            float RTHWELWTargets = ELWChance * (TankHeal ? 1 : RTHWHWCasts * ELWDuration / RTHWTime);
            calcStats.setRTHWHPS((((RTHWRTHeal * (1 - RTOverheal) + RTHWHWHeal * (1 - HWOverheal) + RTHWHWSelfHeal * (1 - HWSelfOverheal) + RTHWAA * (1 - AAOverheal)) / RTHWTime + RTTargets * RTHotHPS * (1 - RTOverheal) + RTHWELWTargets * ELWHPS * (1 - ELWOverheal)) * castingActivity) * (1 + stats.getBonusHealingDoneMultiplier()));
            calcStats.setRTHWMPS(((RTCost + (HWCost * RTHWHWCasts)) / RTHWTime) * castingActivity);
            if (options.getSustStyle().equals("RT+HW")) {
                RTPerSec = 1f / RTHWTime;
                RTTicksPerSec = RTTargets / 3;
                HWPerSec = RTHWHWCasts / RTHWTime;
                RTCPerSec = RTPerSec * CriticalChance;
                HWCPerSec = RTHWHWHCrits / RTHWTime;
                AAsPerSec += (CriticalChance + RTHWHWHCrits) / RTHWTime;
                ELWTicksPerSec += RTHWELWTargets;
            }
        }
        // endregion
        // region RT + CH Rotation (RTCHMPS / RTCHHPS / RTCHTime / TankCH) (Adjusted based on Casting Activity)
        if (character.getShamanTalents().getRiptide() != 0) {
            float RTCHTime = RTCast;
            float RTCHRemainingTime = RTCDCast - RTCHTime;
            int RTCHCHCasts = 0;
            if (RTCHRemainingTime > GcdLatency) {
                RTCHRemainingTime -= CHCast_RT;
                RTCHTime += CHCast_RT;
                ++RTCHCHCasts;
                if (RTCHRemainingTime > GcdLatency) {
                    int RTCHCHRemainingCasts = (int) Math.ceil((RTCHRemainingTime - GcdLatency) / CHCast);
                    RTCHTime += RTCHCHRemainingCasts * CHCast;
                    RTCHCHCasts += RTCHCHRemainingCasts;
                }
            }
            float RTCHRTHeal = RTHeal * Critical;
            float RTCHCHHeal = (CHJumpHeal + CHHeal * CHRTConsumption * .25f) * RTCHCHCasts * ChCritical;
            float RTCHAA = RTHeal * CriticalChance * AAScale;
            float RTTargets = TankHeal ? Math.max(RTDuration / RTCHTime - CHRTConsumption, 0) : (RTCast + (RTDuration - RTCast) * (1 - CHRTConsumption)) / RTCHTime;
            float RTCHELWTargets = ELWChance * (CHJumps * RTCHCHCasts + RTTargets) * ELWDuration / RTCHTime;
            calcStats.setRTCHHPS((((RTCHRTHeal * (1 - RTOverheal) + RTCHCHHeal * (1 - CHOverheal) + RTCHAA * (1 - AAOverheal)) / RTCHTime + RTTargets * RTHotHPS * (1 - RTOverheal) + RTCHELWTargets * ELWHPS * (1 - ELWOverheal)) * castingActivity) * (1 + stats.getBonusHealingDoneMultiplier()));
            calcStats.setRTCHMPS(((RTCost + (CHCost * RTCHCHCasts)) / RTCHTime) * castingActivity);
            if (options.getSustStyle().equals("RT+CH")) {
                RTPerSec = 1f / RTCHTime;
                RTCPerSec = RTPerSec * CriticalChance;
                RTTicksPerSec = RTTargets / 3;
                CHPerSec = RTCHCHCasts / RTCHTime;
                CHHitsPerSec = CHPerSec * CHJumps;
                CHCPerSec = RTCHCHCasts * (CriticalChance + (stats.getRestoSham4T9() * .05f)) / RTCHTime;
                CHCHitsPerSec = CHCPerSec * CHJumps;
                AAsPerSec += CriticalChance / RTCHTime;
                ELWTicksPerSec += RTCHELWTargets;
            }
        }
        // endregion
        // region CH Spam (CHHPS / CHMPS)
        float CHELWTargets = ELWChance * CHJumps * ELWDuration / CHCast;
        calcStats.setCHSpamHPS(((CHJumpHeal * ChCritical * (1 - CHOverheal) / CHCast + CHELWTargets * ELWHPS * (1 - ELWOverheal)) * castingActivity) * (1 + stats.getBonusHealingDoneMultiplier()));
        calcStats.setCHSpamMPS((CHCost / CHCast) * castingActivity);
        if (options.getSustStyle().equals("CH Spam")) {
            CHPerSec = 1f / CHCast;
            CHHitsPerSec = CHPerSec * CHJumps;
            CHCPerSec = (CriticalChance + (stats.getRestoSham4T9() * .05f)) / CHCast;
            CHCHitsPerSec = CHCPerSec * CHJumps;
            ELWTicksPerSec += CHELWTargets;
        }
        // endregion
        // region LHW Spam (LHWHPS / LHWMPS)
        float LHWLHWHeal = LHWHeal * Critical;
        float LHWAA = LHWHeal * CriticalChance * AAScale;
        float LHWELWTargets = ELWChance * ELWDuration / LHWCast;
        calcStats.setLHWSpamHPS((((LHWLHWHeal * (1 - LHWOverheal) + LHWAA * (1 - AAOverheal)) / LHWCast + LHWELWTargets * ELWHPS * (1 - ELWOverheal)) * castingActivity) * (1 + stats.getBonusHealingDoneMultiplier()));
        calcStats.setLHWSpamMPS((LHWCost / LHWCast) * castingActivity);
        if (options.getSustStyle().equals("LHW Spam")) {
            LHWPerSec = 1f / LHWCast;
            LHWCPerSec = CriticalChance / LHWCast;
            ELWTicksPerSec += LHWELWTargets;
        }
        // endregion
        // region HW Spam (HWHPS / HWMPS)
        float HWHWHeal = HWHeal * Critical;
        float HWHWSelfHeal = HWHWHeal * HWSelfHealingScale * Critical;
        float HWAA = HWHeal * CriticalChance * AAScale;
        float HWELWTargets = ELWChance * ELWDuration / HWCast;
        calcStats.setHWSpamHPS((((HWHWHeal * (1 - HWOverheal) + HWHWSelfHeal * (1 - HWSelfOverheal) + HWAA * (1 - AAOverheal)) / HWCast + HWELWTargets * ELWHPS * (1 - ELWOverheal)) * castingActivity) * (1 + stats.getBonusHealingDoneMultiplier()));
        calcStats.setHWSpamMPS((HWCost / HWCast) * castingActivity);
        if (options.getSustStyle().equals("HW Spam")) {
            HWPerSec = 1f / HWCast;
            HWCPerSec = CriticalChance / HWCast;
            ELWTicksPerSec += HWELWTargets;
        }
        // endregion
        // region Variables if Riptide not taken in talents
        if (character.getShamanTalents().getRiptide() == 0) {
            calcStats.setRTLHWHPS(calcStats.getLHWSpamHPS());
            calcStats.setRTLHWMPS(calcStats.getLHWSpamMPS());
            calcStats.setRTHWHPS(calcStats.getHWSpamHPS());
            calcStats.setRTHWMPS(calcStats.getHWSpamMPS());
            calcStats.setRTCHHPS(calcStats.getCHSpamHPS());
            calcStats.setRTCHMPS(calcStats.getCHSpamMPS());
        }
        // endregion
        // region Create Final calcs via spell cast (HealPerSec/HealHitPerSec/CritPerSec)
        healPerSec = (RTPerSec + LHWPerSec + HWPerSec + CHPerSec) * castingActivity;
        healHitPerSec = (RTPerSec + RTTicksPerSec + LHWPerSec + HWPerSec + CHHitsPerSec + AAsPerSec + ELWTicksPerSec) * castingActivity;
        critPerSec = (RTCPerSec + LHWCPerSec + HWCPerSec + CHCPerSec) * castingActivity;
        // endregion
        // region Proc Handling for Mana Restore only
        Stats statsProcs2 = new Stats();
        for (SpecialEffect effect : stats.specialEffects()) {
            switch (effect.getTrigger()) {
                case HealingSpellCast:
                    if (healPerSec != 0)
                        effect.accumulateAverageStats(statsProcs2, (1f / healPerSec), 1f, 0f, fightSeconds);
                    break;
                case HealingSpellHit:
                    if (healHitPerSec != 0)
                        effect.accumulateAverageStats(statsProcs2, (1f / healHitPerSec), 1f, 0f, fightSeconds);
                    break;
                case HealingSpellCrit:
                    if (critPerSec != 0)
                        effect.accumulateAverageStats(statsProcs2, (1f / critPerSec), 1f, 0f, fightSeconds);
                    break;
                case SpellCast:
                    if (healPerSec != 0)
                        effect.accumulateAverageStats(statsProcs2, (1f / healPerSec), 1f, 0f, fightSeconds);
                    break;
                case SpellHit:
                    if (healHitPerSec != 0)
                        effect.accumulateAverageStats(statsProcs2, (1f / healHitPerSec), 1f, 0f, fightSeconds);
                    break;
                case SpellCrit:
                    if (critPerSec != 0)
                        effect.accumulateAverageStats(statsProcs2, (1f / critPerSec), 1f, 0f, fightSeconds);
                    break;
                case Use:
                    effect.accumulateAverageStats(statsProcs2, 0f, 1f, 0f, fightSeconds);
                    break;
            }
        }
        // endregion
        // region Calculate Sequence HPS/MPS
        float HSTHPS = (25 + HSTBonusHealing) * HSTHealingScale / 2f * (1 - HSTOverheal);
        calcStats.setHSTHeals(HSTHPS * HSTTargets);

        // Burst
        calcStats.setBurstSequence(options.getBurstStyle());
        switch (options.getBurstStyle()) {
            case "CH Spam":
                calcStats.setBurstHPS(calcStats.getCHSpamHPS());
                break;
            case "HW Spam":
                calcStats.setBurstHPS(calcStats.getHWSpamHPS());
                break;
            case "LHW Spam":
                calcStats.setBurstHPS(calcStats.getLHWSpamHPS());
                break;
            case "RT+HW":
                calcStats.setBurstHPS(calcStats.getRTHWHPS());
                break;
            case "RT+LHW":
                calcStats.setBurstHPS(calcStats.getRTLHWHPS());
                break;
            case "RT+CH":
                calcStats.setBurstHPS(calcStats.getRTCHHPS());
                break;
        }

        // Sustained
        calcStats.setSustainedSequence(options.getSustStyle());
        float SustHPS = 0;
        switch (options.getSustStyle()) {
            case "CH Spam":
                SustHPS = calcStats.getCHSpamHPS();
                calcStats.setMUPS(calcStats.getCHSpamMPS());
                break;
            case "HW Spam":
                SustHPS = calcStats.getHWSpamHPS();
                calcStats.setMUPS(calcStats.getHWSpamMPS());
                break;
            case "LHW Spam":
                SustHPS = calcStats.getLHWSpamHPS();
                calcStats.setMUPS(calcStats.getLHWSpamMPS());
                break;
            case "RT+HW":
                SustHPS = calcStats.getRTHWHPS();
                calcStats.setMUPS(calcStats.getRTHWMPS());
                break;
            case "RT+LHW":
                SustHPS = calcStats.getRTLHWHPS();
                calcStats.setMUPS(calcStats.getRTLHWMPS());
                break;
            case "RT+CH":
                SustHPS = calcStats.getRTCHHPS();
                calcStats.setMUPS(calcStats.getRTCHMPS());
                break;
        }

        calcStats.setBurstHPS(calcStats.getBurstHPS() + calcStats.getHSTHeals());
        SustHPS += calcStats.getHSTHeals();
        calcStats.setMUPS(calcStats.getMUPS() + (DecurseCost * options.getDecurse()) / fightSeconds);
        // endregion
        // region Final Stats
        float ESUsage = UseES ? (float) Math.rint((fightSeconds / ESTimer)) : 0;
        float ESDowntime = (fightSeconds - (RTCast * ESUsage) - 3) / fightSeconds;  // Rip tide cast time is used to simulate ES cast time, as they are exactly the same.  The 3 Simulates the time of two full totem drops.
        calcStats.setMAPS(((stats.getMana()) / (fightSeconds))
                + (stats.getManaRestore() / fightSeconds)
                + ((((((float) Math.floor(options.getFightLength() / 5.025f) + 1) * ((stats.getMana() * (1 + stats.getBonusManaMultiplier())) * (.24f + ((character.getShamanTalents().isGlyphofManaTideTotem() ? 0.04f : 0)))))) * (options.getManaTideEveryCD() && character.getShamanTalents().getManaTideTotem() > 0 ? 1 : 0)) / fightSeconds)
                + ((stats.getManaRestoreFromMaxManaPerSecond() * stats.getMana()) * (options.getReplenishmentPercentage() * .01f))
                + (stats.getMp5() / 5f)
                + (options.getInnervates() * 7866f / fightSeconds)
                + statsProcs2.getManaRestore()
                + ((RTCPerSec * Orb) * castingActivity * ESDowntime)
                + ((LHWCPerSec * Orb * .6f) * castingActivity * ESDowntime)
                + ((HWCPerSec * Orb) * castingActivity * ESDowntime)
                + ((CHCHitsPerSec * Orb * .3f) * castingActivity * ESDowntime)
                - ESMPS);
        if (options.getWSPops() > 0)
            calcStats.setMAPS(calcStats.getMAPS() + ((options.getWSPops() * Orb) / 60));
        calcStats.setManaUsed(calcStats.getMAPS() * fightSeconds);
        float MAPSConvert = (float) Math.min((calcStats.getMAPS() / ((calcStats.getMUPS() * castingActivity) * ESDowntime)), 1);
        //  FIXME: some Healed effects can crit and some are not affected by Purification, this should be taken into account
        float HealedHPS = (stats.getHealed() * PurificationScale) * (1 + stats.getBonusHealingDoneMultiplier());
        calcStats.setBurstHPS((calcStats.getBurstHPS() * ESDowntime) + calcStats.getESHPS() * (1 - ESOverheal) + HealedHPS);
        calcStats.setSustainedHPS((SustHPS * MAPSConvert) + calcStats.getESHPS() * (1 - ESOverheal) + HealedHPS);
        calcStats.setSurvival((calcStats.getBasicStats().getHealth() + calcStats.getBasicStats().getHp5()) * (options.getSurvivalPerc() * .01f));
        calcStats.setOverallPoints(calcStats.getBurstHPS() + calcStats.getSustainedHPS() + calcStats.getSurvival());
        calcStats.getSubPoints()[0] = calcStats.getBurstHPS();
        calcStats.getSubPoints()[1] = calcStats.getSustainedHPS();
        calcStats.getSubPoints()[2] = calcStats.getSurvival();

        return calcStats;
        // endregion
    }
    // endregion

    // region Character Stats and other Final Stats
    public Stats getCharacterStats(Character character, Item additionalItem) {
        return getCharacterStats(character, additionalItem, null);
    }

    public Stats getCharacterStats(Character character, Item additionalItem, Stats statModifier) {
        CalculationOptionsRestoSham calcOpts = character.getCalculationOptions() instanceof CalculationOptionsRestoSham
                ? (CalculationOptionsRestoSham) character.getCalculationOptions()
                : null;
        Stats statsRace = getRaceStats(character.getRace());
        // region Other Final Stats
        Stats statsBaseGear = new Stats();
        accumulateItemStats(statsBaseGear, character, additionalItem);
        Stats statsBuffs = getBuffsStats(character.getActiveBuffs());
        Stats statsTotal = Stats.add(Stats.add(statsBaseGear, statsBuffs), statsRace);
        if (statModifier != null)
            statsTotal = Stats.add(statsTotal, statModifier);
        statsTotal.accumulate(getProcStats(statsTotal.specialEffects()));
        statsTotal.setStamina((float) Math.rint((statsTotal.getStamina()) * (1 + statsTotal.getBonusStaminaMultiplier())));

        float IntMultiplier = (1f + statsTotal.getBonusIntellectMultiplier()) * (1f + character.getShamanTalents().getAncestralKnowledge() * 2f / 100);
        if (IntMultiplier > 1) {
            statsTotal.setIntellect((float) Math.floor((statsRace.getIntellect()) * IntMultiplier) +
                    (float) Math.floor((statsBaseGear.getIntellect() + statsBuffs.getIntellect()) * IntMultiplier));
            if (statModifier != null && statModifier.getIntellect() > 0)
                statsTotal.setIntellect(statsTotal.getIntellect() + (float) Math.floor((statModifier.getIntellect()) * IntMultiplier));
        }

        statsTotal.setSpellPower((float) Math.floor(statsTotal.getSpellPower()) + (float) Math.floor(statsTotal.getIntellect() * 0.05f * character.getShamanTalents().getNaturesBlessing()));
        statsTotal.setMana(statsTotal.getMana() + 20 + ((statsTotal.getIntellect() - 20) * 15));
        statsTotal.setHealth((statsTotal.getHealth() + 20 + ((statsTotal.getStamina() - 20) * 10f)) * (1f + statsTotal.getBonusHealthMultiplier()));
        // endregion
        return statsTotal;
    }

    // region Basic Stats Methods
    /**
     * Filters a Stats object to just the stats relevant to the model.
     */
    public Stats getRelevantStats(Stats stats) {
        Stats relevantStats = new Stats();

        for (String relevantStat : Relevants.RelevantStats) {
            Stats.StatProperty property = Stats.PROPERTY_BY_NAME.get(relevantStat);
            if (property == null) continue;
            float v = property.accessor().get(stats);
            if (v > 0) {
                property.mutator().set(relevantStats, v);
            }
        }

        for (SpecialEffect effect : stats.specialEffects()) {
            if (effect.getTrigger() == Trigger.HealingSpellCast ||
                    effect.getTrigger() == Trigger.HealingSpellCrit ||
                    effect.getTrigger() == Trigger.HealingSpellHit ||
                    effect.getTrigger() == Trigger.SpellCast ||
                    effect.getTrigger() == Trigger.SpellCrit ||
                    effect.getTrigger() == Trigger.SpellHit ||
                    effect.getTrigger() == Trigger.Use) {
                if (hasRelevantStats(effect.getStats()))
                    relevantStats.addSpecialEffect(effect);
            }
        }

        return relevantStats;
    }

    /**
     * Tests whether there are positive relevant stats in the Stats object.
     */
    @Override
    public boolean hasRelevantStats(Stats stats) {
        // Accumulate the "base" stats with the special effect stats.
        Stats comparison = new Stats();
        comparison.accumulate(stats);
        for (SpecialEffect effect : stats.specialEffects()) {
            comparison.accumulate(effect.getStats());
        }

        float statTotal = 0f;

        // Loop over each relevant stat and get its value
        for (String relevantStat : Relevants.RelevantStats) {
            Stats.StatProperty property = Stats.PROPERTY_BY_NAME.get(relevantStat);
            if (property == null) continue;
            float v = property.accessor().get(comparison);
            if (v > 0)
                statTotal += v;
        }

        // if statTotal > 0 then we have relevant stats
        return statTotal > 0;
    }

    /**
     * Gets the stats of procs or special effects
     */
    private Stats getProcStats(Stats.SpecialEffectEnumerator specialEffects) {
        Stats statsProcs = new Stats();
        for (SpecialEffect effect : specialEffects) {
            statsProcs.accumulate(getProcStatsInner(effect));
        }
        return statsProcs;
    }

    private Stats getProcStatsInner(SpecialEffect effect) {
        Stats procStats = new Stats();
        switch (effect.getTrigger()) {
            case HealingSpellCast:
                if (healPerSec != 0)
                    procStats = effect.getAverageStats((1f / healPerSec), 1f, 0f, fightSeconds);
                break;
            case HealingSpellHit:
                if (healHitPerSec != 0)
                    procStats = effect.getAverageStats((1f / healHitPerSec), 1f, 0f, fightSeconds);
                break;
            case HealingSpellCrit:
                if (critPerSec != 0)
                    procStats = effect.getAverageStats((1f / critPerSec), 1f, 0f, fightSeconds);
                break;
            case SpellCast:
                if (healPerSec != 0)
                    procStats = effect.getAverageStats((1f / healPerSec), 1f, 0f, fightSeconds);
                break;
            case SpellHit:
                if (healHitPerSec != 0)
                    procStats = effect.getAverageStats((1f / healHitPerSec), 1f, 0f, fightSeconds);
                break;
            case SpellCrit:
                if (critPerSec != 0)
                    procStats = effect.getAverageStats((1f / critPerSec), 1f, 0f, fightSeconds);
                break;
            case Use:
                if (effect.getStats().getRawSpecialEffectData() != null) {
                    // Handles Recursive Effects
                    Stats SubStats = getProcStatsInner(effect.getStats().getRawSpecialEffectData()[0]);
                    float upTime = effect.getAverageUptime(0f, 1f, 0f, fightSeconds);
                    procStats.accumulate(SubStats, upTime);
                } else {
                    procStats = effect.getAverageStats(0f, 1f, 0f, fightSeconds);
                }
                break;
        }
        return procStats;
    }

    /**
     * Create the base stats for a given character based on their race.
     */
    private static Stats getRaceStats(CharacterRace race) {
        // region Create the statistics for a given character
        Stats statsRace;
        switch (race) {
            case Draenei:
                statsRace = new Stats();
                statsRace.setHealth(6485);
                statsRace.setMana(BaseMana);
                statsRace.setStamina(135);
                statsRace.setIntellect(141);
                statsRace.setSpirit(145);
                break;

            case Tauren:
                statsRace = new Stats();
                statsRace.setHealth(6485);
                statsRace.setMana(BaseMana);
                statsRace.setStamina(138);
                statsRace.setIntellect(135);
                statsRace.setSpirit(145);
                statsRace.setBonusHealthMultiplier(0.05f);
                break;

            case Orc:
                statsRace = new Stats();
                statsRace.setHealth(6485);
                statsRace.setMana(BaseMana);
                statsRace.setStamina(138);
                statsRace.setIntellect(137);
                statsRace.setSpirit(146);
                break;

            case Troll:
                statsRace = new Stats();
                statsRace.setHealth(6485);
                statsRace.setMana(BaseMana);
                statsRace.setStamina(137);
                statsRace.setIntellect(124);
                statsRace.setSpirit(144);
                break;

            default:
                statsRace = new Stats();
                break;
        }
        return statsRace;
        // endregion
    }
    // endregion
    // endregion

    // region Chart data area
    public ComparisonCalculationBase[] getCustomChartData(Character character, String chartName) {
        ChartCalculator chartCalc = CustomCharts.getChartCalculator(chartName);
        if (chartCalc == null)
            return new ComparisonCalculationBase[0];

        java.util.Collection<ComparisonCalculationBase> list = chartCalc.apply(character, this);
        return list.toArray(new ComparisonCalculationBase[list.size()]);
    }
    // endregion

    // region Retrieve our options from XML:
    @Override
    public ICalculationOptionBase deserializeDataObject(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            org.w3c.dom.Document document = factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            CalculationOptionsRestoSham calcOpts = new CalculationOptionsRestoSham();
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
    // endregion
}
