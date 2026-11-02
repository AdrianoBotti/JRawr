// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Enhance/CombatStats.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the C# [XmlIgnore] export getters used only by the EnhSim clipboard export are omitted.
package rawr.models.enhance;

import rawr.base.stats.BaseStats;
import rawr.base.character.Character;
import rawr.base.character.CharacterRace;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemType;
import rawr.base.talents.ShamanTalents;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class CombatStats {

    private final CalculationOptionsEnhance calcOpts;
    private final Character character;
    private final Stats stats;
    private final ShamanTalents talents;
    private Priorities rotation;

    private int levelDifference = 3;
    private float whiteCritDepression = 0.048f;
    private float yellowCritDepression = 0.018f;
    private float fightLength = 0f;
    private float chanceDodgeMH = 0f;
    private float chanceDodgeOH = 0f;
    private float chanceParryMH = 0f;

    private float chanceParryOH = 0f;
    private float expertiseBonusMH = 0f;
    private float expertiseBonusOH = 0f;

    private float critMultiplierMelee = 0f;
    private float critMultiplierSpell = 0f;

    private float chanceSpellMiss = 0f;
    private float chanceWhiteMissMH = 0f;
    private float chanceYellowMissMH = 0f;
    private float chanceYellowCritMH = 0f;
    private float chanceWhiteMissOH = 0f;
    private float chanceYellowMissOH = 0f;
    private float chanceYellowCritOH = 0f;
    private float chanceWhiteCritMH = 0f;
    private float chanceWhiteCritOH = 0f;
    private float chanceSpellCrit = 0f;
    private float chanceMeleeHit = 0f;
    private float overSpellHitCap = 0f;
    private float overMeleeCritCap = 0f;

    private float unhastedMHSpeed = 0f;
    private float hastedMHSpeed = 0f;
    private float unhastedOHSpeed = 0f;
    private float hastedOHSpeed = 0f;
    private float averageFSDotTime = 18f;

    private float secondsToFiveStack = 0f;
    private float hitsPerSOH = 0f;
    private float hitsPerSMH = 0f;
    private float hitsPerSOHSS = 0f;
    private float hitsPerSMHSS = 0f;
    private float hitsPerSWF = 0f;
    private float hitsPerSLL = 0f;

    private float flurryUptime = 1f;
    private float edUptime = 0f;
    private float edBonusCrit = 0f;
    private float ftBonusCrit = 0f;
    private float fireTotemUptime = 0f;

    private float meleeAttacksPerSec = 0f;
    private float meleeCritsPerSec = 0f;
    private float spellAttacksPerSec = 0f;
    private float spellCritsPerSec = 0f;
    private float spellCastsPerSec = 0f;
    private float spellMissesPerSec = 0f;

    private float callOfThunder = 0f;
    private float staticShocksPerSecond = 0f;
    private float baseMana = 0f;
    private float maxMana = 0f;
    private float manaRegen = 0f;
    private float exportMeleeCritMH = 0f;
    private float exportMeleeCritOH = 0f;

    public float getFightLength() {
        return fightLength;
    }

    public float getChanceDodgeMH() {
        return chanceDodgeMH;
    }

    public float getChanceDodgeOH() {
        return chanceDodgeOH;
    }

    public float getChanceParryMH() {
        return chanceParryMH;
    }

    public float getChanceParryOH() {
        return chanceParryOH;
    }

    public float getExpertiseBonusMH() {
        return expertiseBonusMH;
    }

    public float getExpertiseBonusOH() {
        return expertiseBonusOH;
    }

    public float getNormalHitModifierMH() {
        return getChanceWhiteHitMH() - chanceWhiteCritMH - getGlancingRate();
    }

    public float getCritHitModifierMH() {
        return chanceWhiteCritMH * (2f * (1f + stats.getBonusCritMultiplier()));
    }

    public float getNormalHitModifierOH() {
        return getChanceWhiteHitOH() - chanceWhiteCritOH - getGlancingRate();
    }

    public float getCritHitModifierOH() {
        return chanceWhiteCritOH * (2f * (1f + stats.getBonusCritMultiplier()));
    }

    public float getGlancingHitModifier() {
        return getGlancingRate() * 0.7f;
    }

    public float getYellowHitModifierMH() {
        return getChanceYellowHitMH() * (1 - chanceYellowCritMH);
    }

    public float getYellowHitModifierOH() {
        return getChanceYellowHitOH() * (1 - chanceYellowCritOH);
    }

    public float getYellowCritModifierMH() {
        return getChanceYellowHitMH() * chanceYellowCritMH;
    }

    public float getYellowCritModifierOH() {
        return getChanceYellowHitOH() * chanceYellowCritOH;
    }

    public float getSpellHitModifier() {
        return getChanceSpellHit() * (1 - chanceSpellCrit);
    }

    public float getSpellCritModifier() {
        return getChanceSpellHit() * chanceSpellCrit;
    }

    public float getLBHitModifier() {
        return getChanceSpellHit() * (1 - chanceSpellCrit - callOfThunder);
    }

    public float getLBCritModifier() {
        return getChanceSpellHit() * (chanceSpellCrit + callOfThunder);
    }

    public float getCritMultiplierMelee() {
        return critMultiplierMelee;
    }

    public float getCritMultiplierSpell() {
        return critMultiplierSpell;
    }

    public float getChanceSpellHit() {
        return 1 - chanceSpellMiss;
    }

    public float getChanceWhiteHitMH() {
        return 1 - chanceWhiteMissMH - chanceDodgeMH;
    }

    public float getChanceWhiteHitOH() {
        return 1 - chanceWhiteMissOH - chanceDodgeOH;
    }

    public float getChanceYellowHitMH() {
        return 1 - chanceYellowMissMH;
    }

    public float getChanceYellowHitOH() {
        return 1 - chanceYellowMissOH;
    }

    public float getChanceSpellCrit() {
        return chanceSpellCrit;
    }

    public float getChanceWhiteCritMH() {
        return chanceWhiteCritMH;
    }

    public float getChanceWhiteCritOH() {
        return chanceWhiteCritOH;
    }

    public float getChanceYellowCritMH() {
        return chanceYellowCritMH;
    }

    public float getChanceYellowCritOH() {
        return chanceYellowCritOH;
    }

    public float getOverSpellHitCap() {
        return overSpellHitCap;
    }

    public float getOverMeleeCritCap() {
        return overMeleeCritCap;
    }

    public float getAverageDodge() {
        return (getChanceDodgeMH() + getChanceDodgeOH()) / 2;
    }

    public float getAverageParry() {
        return (getChanceParryMH() + getChanceParryOH()) / 2;
    }

    public float getAverageExpertise() {
        return (expertiseBonusMH + expertiseBonusOH) / 2;
    }

    public float getAverageWhiteHitChance() {
        return (getChanceWhiteHitMH() + getChanceWhiteHitOH()) / 2;
    }

    public float getAverageWhiteCritChance() {
        return (getChanceWhiteCritMH() + getChanceWhiteCritOH()) / 2;
    }

    public float getAverageYellowHitChance() {
        return (getChanceYellowHitMH() + getChanceYellowHitOH()) / 2;
    }

    public float getAverageYellowCritChance() {
        return (getChanceYellowCritMH() + getChanceYellowCritOH()) / 2;
    }

    public float getUnhastedMHSpeed() {
        return unhastedMHSpeed;
    }

    public float getHastedMHSpeed() {
        return hastedMHSpeed;
    }

    public float getUnhastedOHSpeed() {
        return unhastedOHSpeed;
    }

    public float getHastedOHSpeed() {
        return hastedOHSpeed;
    }

    public float getSecondsToFiveStack() {
        return secondsToFiveStack;
    }

    public float getAverageFSDotTime() {
        return averageFSDotTime;
    }

    public float getBaseShockSpeed() {
        return 6f - .2f * talents.getReverberation();
    }

    public float getBaseFireNovaSpeed() {
        return 10f - 2f * talents.getImprovedFireNova();
    }

    public float getStaticShockProcsPerS() {
        return staticShocksPerSecond;
    }

    public float getStaticShockAvDuration() {
        return getStaticShockProcsPerS() == 0 ? 600f : ((3f + 2f * character.getShamanTalents().getStaticShock()) / getStaticShockProcsPerS());
    }

    public float getMultiTargetMultiplier() {
        return calcOpts.getMultipleTargets() ? calcOpts.getAdditionalTargets() * calcOpts.getAdditionalTargetPercent() : 1f;
    }

    public float getHitsPerSOH() {
        return hitsPerSOH;
    }

    public float getHitsPerSMH() {
        return hitsPerSMH;
    }

    public float getHitsPerSOHSS() {
        return hitsPerSOHSS;
    }

    public float getHitsPerSMHSS() {
        return hitsPerSMHSS;
    }

    public float getHitsPerSWF() {
        return hitsPerSWF;
    }

    public float getHitsPerSLL() {
        return hitsPerSLL;
    }

    public float getWFPPM() {
        return hitsPerSWF == 0 ? 0f : 60f * hitsPerSWF / 2f; // we doubled the hits to get extra attacks for PPM we need number of WFs not number of hits
    }

    public float getFTPPM() {
        return hitsPerSOH == 0 ? 0f : 60f * hitsPerSOH;
    }

    public float getMeleePPM() {
        return (hastedMHSpeed == 0 ? 0f : 60f / hastedMHSpeed) + (hastedOHSpeed == 0 ? 0f : 60f / hastedOHSpeed);
    }

    public float getPecentageBehindBoss() {
        return calcOpts.getInBackPerc() / 100f;
    }

    public float getPecentageInfrontBoss() {
        return 1f - calcOpts.getInBackPerc() / 100f;
    }

    public float getEDUptime() {
        return edUptime;
    }

    public float getEDBonusCrit() {
        return edBonusCrit;
    }

    public float getFlurryUptime() {
        return flurryUptime;
    }

    public float getFireTotemUptime() {
        return fireTotemUptime;
    }

    public float getFireElementalUptime() {
        return getFireElementalUptimeImpl();
    }

    public float abilityCooldown(EnhanceAbility abilityType) {
        return rotation.abilityCooldown(abilityType);
    }

    public float getExportMeleeCritMH() {
        return exportMeleeCritMH; // doesn't include ED
    }

    public float getExportMeleeCritOH() {
        return exportMeleeCritOH; // doesn't include ED
    }

    public float getDisplayMeleeCrit() {
        return getAverageWhiteCritChance() + whiteCritDepression;
    }

    public float getDisplayYellowCrit() {
        return getAverageYellowCritChance() + yellowCritDepression;
    }

    public float getDisplaySpellCrit() {
        return chanceSpellCrit - ftBonusCrit;
    }

    public float getMaxMana() {
        return maxMana;
    }

    public float getManaRegen() {
        return manaRegen;
    }

    public float getImpStormStrikeMana() {
        return talents.getImprovedStormstrike() * .1f * baseMana;
    }

    public float getDamageReduction() {
        return 1f - StatConversion.getArmorDamageReduction(character.getLevel(), calcOpts.getTargetArmor(), stats.getArmorPenetration(), 0f, stats.getArmorPenetrationRating());
    }

    public float getGlancingRate() {
        return StatConversion.WHITE_GLANCE_CHANCE_CAP[levelDifference];
    }

    private float getDPRfromExp(float Expertise) {
        return StatConversion.getDodgeParryReducFromExpertise(Expertise, rawr.base.character.CharacterClass.Shaman);
    }

    private float getDodgeChanceCap() {
        return StatConversion.WHITE_DODGE_CHANCE_CAP[levelDifference];
    }

    private float getParryChanceCap() {
        return StatConversion.WHITE_PARRY_CHANCE_CAP[levelDifference];
    }

    private float getWhiteHitCap() {
        return StatConversion.WHITE_MISS_CHANCE_CAP_DW[levelDifference];
    }

    private float getYellowHitCap() {
        return StatConversion.YELLOW_MISS_CHANCE_CAP[levelDifference];
    }

    private float getSpellMissRate() {
        return StatConversion.getSpellMiss(-levelDifference, false);
    }

    public CombatStats(Character character, Stats stats, CalculationOptionsEnhance calcOpts) {
        this.stats = stats;
        this.character = character;
        this.calcOpts = calcOpts;
        this.talents = character.getShamanTalents();
        fightLength = calcOpts.getFightLength() * 60f;
        levelDifference = calcOpts.getTargetLevel() - character.getLevel();
        if (levelDifference > 3)
            levelDifference = 3;
        if (levelDifference < 0)
            levelDifference = 0;
        whiteCritDepression = 0.03f + 0.006f * levelDifference;
        yellowCritDepression = 0.006f * levelDifference;
        setManaRegen();
        updateCalcs(true);
        rotation = new Priorities(this, calcOpts, character, stats, talents);
        rotation.calculateAbilities();
        updateCalcs(false); // second pass to revise calcs based on new ability cooldowns
    }

    public void setManaRegen() {
        baseMana = BaseStats.getBaseStats(character).getMana();
        float spiRegen = StatConversion.getSpiritRegenSec(stats.getSpirit(), stats.getIntellect());
        float replenishRegen = stats.getMana() * stats.getManaRestoreFromMaxManaPerSecond();
        float judgementRegen = stats.getManaRestoreFromBaseManaPPM() / 60f * baseMana;
        manaRegen = stats.getMp5() / 5 + spiRegen + replenishRegen + judgementRegen;
    }

    public void updateCalcs(boolean firstPass) {
        // talents
        callOfThunder = .05f * talents.getCallOfThunder();
        critMultiplierMelee = 2f * (1 + stats.getBonusCritMultiplier());
        critMultiplierSpell = (1.5f + .1f * character.getShamanTalents().getElementalFury()) * (1 + stats.getBonusSpellCritMultiplier());

        // Melee
        float hitBonus = stats.getPhysicalHit() + StatConversion.getHitFromRating(stats.getHitRating()) + 0.02f * talents.getDualWieldSpecialization();
        expertiseBonusMH = getDPRfromExp(stats.getExpertise() + StatConversion.getExpertiseFromRating(stats.getExpertiseRating()));
        expertiseBonusOH = getDPRfromExp(stats.getExpertise() + StatConversion.getExpertiseFromRating(stats.getExpertiseRating()));

        // Need to modify expertiseBonusMH & OH if Orc and have racial bonus weapons
        if (character.getRace() == CharacterRace.Orc) {
            ItemType mhType = character.get(rawr.base.character.CharacterSlot.MainHand) == null ? ItemType.None : character.get(rawr.base.character.CharacterSlot.MainHand).getType();
            ItemType ohType = character.get(rawr.base.character.CharacterSlot.OffHand) == null ? ItemType.None : character.get(rawr.base.character.CharacterSlot.OffHand).getType();
            if (mhType == ItemType.OneHandAxe || mhType == ItemType.FistWeapon) // patch 3.2 includes fists
                expertiseBonusMH += 0.0125f;
            if (ohType == ItemType.OneHandAxe || ohType == ItemType.FistWeapon) // patch 3.2 includes fists
                expertiseBonusOH += 0.0125f;
        }

        float meleeCritModifier = stats.getPhysicalCrit();
        float baseMeleeCrit = StatConversion.getCritFromRating(stats.getCritRating()) +
                StatConversion.getCritFromAgility(stats.getAgility(), character.getCharacterClass()) + .01f * talents.getThunderingStrikes();
        chanceDodgeMH = Math.max(0f, getDodgeChanceCap() - expertiseBonusMH);
        chanceDodgeOH = Math.max(0f, getDodgeChanceCap() - expertiseBonusOH);
        float ParryChance = getParryChanceCap() - expertiseBonusMH;
        chanceParryMH = (float) Math.max(0f, calcOpts.getInBack() ? ParryChance * (1f - calcOpts.getInBackPerc() / 100f) : ParryChance);
        ParryChance = getParryChanceCap() - expertiseBonusOH;
        chanceParryOH = (float) Math.max(0f, calcOpts.getInBack() ? ParryChance * (1f - calcOpts.getInBackPerc() / 100f) : ParryChance);
        chanceWhiteMissMH = Math.max(0f, getWhiteHitCap() - hitBonus) + chanceDodgeMH + chanceParryMH;
        chanceWhiteMissOH = Math.max(0f, getWhiteHitCap() - hitBonus) + chanceDodgeOH + chanceParryOH;
        chanceYellowMissMH = Math.max(0f, getYellowHitCap() - hitBonus) + chanceDodgeMH + chanceParryMH; // base miss 8% now
        chanceYellowMissOH = Math.max(0f, getYellowHitCap() - hitBonus) + chanceDodgeOH + chanceParryOH; // base miss 8% now

        // SetCritValues((1 + _stats.BonusCritChance) * (baseMeleeCrit + meleeCritModifier) + .00005f); //fudge factor for rounding
        setCritValues(baseMeleeCrit + meleeCritModifier + .00005f); //fudge factor for rounding
        // set two export values so that ED crit isn't included
        exportMeleeCritMH = chanceWhiteCritMH + whiteCritDepression;
        exportMeleeCritOH = chanceWhiteCritOH + whiteCritDepression;

        // Spells
        ftBonusCrit = 0f;
        if (calcOpts.getMainhandImbue().equals("Flametongue"))
            ftBonusCrit += talents.isGlyphofFlametongueWeapon() ? .02f : 0f;
        if (calcOpts.getOffhandImbue().equals("Flametongue") && talents.getDualWield() == 1)
            ftBonusCrit += talents.isGlyphofFlametongueWeapon() ? .02f : 0f;

        float spellCritModifier = stats.getSpellCrit() + stats.getSpellCritOnTarget() + ftBonusCrit;
        float hitBonusSpell = stats.getSpellHit() + StatConversion.getSpellHitFromRating(stats.getHitRating());
        chanceSpellMiss = Math.max(0f, getSpellMissRate() - hitBonusSpell);
        overSpellHitCap = Math.max(0f, hitBonusSpell - getSpellMissRate());
        float baseSpellCrit = StatConversion.getSpellCritFromRating(stats.getCritRating()) +
                StatConversion.getSpellCritFromIntellect(stats.getIntellect()) + .01f * talents.getThunderingStrikes();
        //chanceSpellCrit = Math.Min(0.75f, (1 + _stats.BonusCritChance) * (baseSpellCrit + spellCritModifier) + .00005f); //fudge factor for rounding
        chanceSpellCrit = Math.min(0.75f, baseSpellCrit + spellCritModifier + .00005f); //fudge factor for rounding

        float hasteBonus = StatConversion.getHasteFromRating(stats.getHasteRating(), character.getCharacterClass());
        unhastedMHSpeed = character.get(rawr.base.character.CharacterSlot.MainHand) == null ? 3.0f : character.get(rawr.base.character.CharacterSlot.MainHand).getItem().getSpeed();
        unhastedOHSpeed = character.get(rawr.base.character.CharacterSlot.OffHand) == null ? 3.0f : character.get(rawr.base.character.CharacterSlot.OffHand).getItem().getSpeed();
        float baseHastedMHSpeed = unhastedMHSpeed / (1f + hasteBonus) / (1f + stats.getPhysicalHaste());
        float baseHastedOHSpeed = unhastedOHSpeed / (1f + hasteBonus) / (1f + stats.getPhysicalHaste());
        float chanceToProcWFPerHit = .2f + (character.getShamanTalents().isGlyphofWindfuryWeapon() ? .02f : 0f);

        //The Swing Loop
        //This is where we figure out feedback systems -- WF, MW, ED, Flurry, etc.
        //--------------
        flurryUptime = 1f;
        edUptime = 0f;
        float stormstrikeSpeed = firstPass ? (talents.getStormstrike() == 1 ? 8f : 0f) : abilityCooldown(EnhanceAbility.StormStrike);
        float shockSpeed = firstPass ? getBaseShockSpeed() : abilityCooldown(EnhanceAbility.EarthShock);
        float lavaLashSpeed = firstPass ? (talents.getLavaLash() == 1 ? 6f : 0f) : abilityCooldown(EnhanceAbility.LavaLash);
        float fireNovaSpeed = firstPass ? getBaseFireNovaSpeed() : abilityCooldown(EnhanceAbility.FireNova);
        if (calcOpts.priorityInUse(EnhanceAbility.MagmaTotem))
            fireTotemUptime = firstPass ? 1.0f : 20f / abilityCooldown(EnhanceAbility.MagmaTotem);
        else if (calcOpts.priorityInUse(EnhanceAbility.SearingTotem))
            fireTotemUptime = firstPass ? 1.0f : 60f / abilityCooldown(EnhanceAbility.SearingTotem);
        else if (calcOpts.priorityInUse(EnhanceAbility.RefreshTotems)) // if no Searing or Magma totem use refresh of Flametongue totem.
            fireTotemUptime = firstPass ? 1.0f : 300f / abilityCooldown(EnhanceAbility.RefreshTotems);

        float mwPPM = 2 * talents.getMaelstromWeapon() * (1 + stats.getEnhance4T8() * 0.2f);
        float flurryHasteBonus = .06f * talents.getFlurry() + stats.getEnhance4T7();
        float edCritBonus = .03f * talents.getElementalDevastation();
        float staticShockChance = (.02f * character.getShamanTalents().getStaticShock() + (stats.getEnhance2T9() == 1f ? 0.03f : 0f));
        hitsPerSMHSS = 0f;
        hitsPerSOHSS = 0f;
        hitsPerSOH = 0f;
        hitsPerSMH = 0f;
        hitsPerSWF = 0f;
        if (talents.getStormstrike() == 1) {
            hitsPerSMHSS = (1f - chanceYellowMissMH) / stormstrikeSpeed;
            hitsPerSOHSS = character.getShamanTalents().getDualWield() == 1 ? ((1f - 2 * chanceYellowMissOH) / stormstrikeSpeed) : 0f; //OH only swings if MH connects
        }
        hitsPerSLL = lavaLashSpeed == 0 ? 0f : (1f - chanceYellowMissOH) / lavaLashSpeed;
        float swingsPerSMHMelee = 0f;
        float swingsPerSOHMelee = 0f;
        float wfProcsPerSecond = 0f;
        float mwProcsPerSecond = 0f;
        secondsToFiveStack = 10f;
        float averageMeleeCritChance = (chanceWhiteCritMH + chanceWhiteCritOH + chanceYellowCritMH + chanceYellowCritOH) / 4f;
        float averageMeleeHitChance = ((1f - chanceWhiteMissMH - chanceDodgeMH - chanceParryMH) + (1f - chanceWhiteMissOH - chanceDodgeOH - chanceParryOH)) / 2f;
        float averageMeleeMissChance = (chanceWhiteMissMH + chanceWhiteMissOH) / 2f;
        float whiteHitsPerSMH = 0f;
        float whiteHitsPerSOH = 0f;
        float moteHitsPerS = 0f;
        float yellowHitsPerSMH = 0f;
        float yellowHitsPerSOH = 0f;
        float flameTongueHitsPerSecond = 0f;
        for (int i = 0; i < 5; i++) {
            // float bonusHaste = (1f + (flurryUptime * flurryHasteBonus));
            float bonusHaste = 1 / (1 - flurryUptime + flurryUptime / (1 + flurryHasteBonus)); // use time based not proc based flurryUptime
            hastedMHSpeed = baseHastedMHSpeed / bonusHaste;
            hastedOHSpeed = baseHastedOHSpeed / bonusHaste;
            swingsPerSMHMelee = 1f / hastedMHSpeed;
            swingsPerSOHMelee = (hastedOHSpeed == 0f) ? 0f : 1f / hastedOHSpeed;
            whiteHitsPerSMH = getChanceWhiteHitMH() * swingsPerSMHMelee;
            whiteHitsPerSOH = getChanceWhiteHitOH() * swingsPerSOHMelee;
            moteHitsPerS = stats.getMoteOfAnger() * 2 * getAverageWhiteHitChance();
            // Windfury model
            if (calcOpts.getMainhandImbue().equals("Windfury")) {
                float hitsThatProcWFPerS = whiteHitsPerSMH + hitsPerSMHSS;
                if (character.getShamanTalents().getDualWield() == 1)
                    hitsThatProcWFPerS += moteHitsPerS / 2; // half the hits will be OH and thus won't proc WF
                else
                    hitsThatProcWFPerS += moteHitsPerS; // if no offhand then all motes will be MH weapon by definition
                float maxExpectedWFPerFight = hitsThatProcWFPerS * chanceToProcWFPerHit * fightLength;
                float ineligibleSeconds = maxExpectedWFPerFight * (3.25f - hastedMHSpeed);
                float expectedWFPerFight = hitsThatProcWFPerS * chanceToProcWFPerHit * (fightLength - ineligibleSeconds);
                wfProcsPerSecond = expectedWFPerFight / fightLength;
                hitsPerSWF = 2f * wfProcsPerSecond * (1f - chanceYellowMissMH);
            }
            yellowHitsPerSMH = hitsPerSWF + hitsPerSMHSS;
            yellowHitsPerSOH = hitsPerSOHSS + hitsPerSLL;

            //Due to attack table, a white swing has the same chance to crit as a yellow hit
            // Old Flurry calc changed 10 Nov 2009
            flurryUptime = calculateFlurryUptime(averageMeleeCritChance, averageMeleeHitChance, averageMeleeMissChance);

            // Maelstrom Weapon time to 5 stacks calc
            if (character.getShamanTalents().getDualWield() == 1 && unhastedOHSpeed != 0f) {
                hitsPerSMH = whiteHitsPerSMH + yellowHitsPerSMH + moteHitsPerS / 2;
                hitsPerSOH = whiteHitsPerSOH + yellowHitsPerSOH + moteHitsPerS / 2;
                mwProcsPerSecond = (mwPPM / (60f / unhastedMHSpeed)) * hitsPerSMH + (mwPPM / (60f / unhastedOHSpeed)) * hitsPerSOH;
            } else {
                hitsPerSMH = whiteHitsPerSMH + yellowHitsPerSMH + moteHitsPerS;
                hitsPerSOH = 0f;
                mwProcsPerSecond = (mwPPM / (60f / unhastedMHSpeed)) * hitsPerSMH;
            }
            secondsToFiveStack = 5f / mwProcsPerSecond;

            // Elemental Devastation Uptime calc
            staticShocksPerSecond = (hitsPerSMH + hitsPerSOH) * staticShockChance;
            flameTongueHitsPerSecond = (calcOpts.getMainhandImbue().equals("Flametongue") ? getHitsPerSMH() : 0f) +
                    ((calcOpts.getOffhandImbue().equals("Flametongue") && talents.getDualWield() == 1) ? getHitsPerSOH() : 0f);
            spellAttacksPerSec = (1f / secondsToFiveStack + 1f / shockSpeed + 1f / fireNovaSpeed + staticShocksPerSecond) // + flameTongueHitsPerSecond)
                    * (1f - chanceSpellMiss);
            float couldCritSpellsPerS = spellAttacksPerSec - staticShocksPerSecond; // LS procs from Static Shock cannot crit
            edUptime = 1f - (float) Math.pow(1 - chanceSpellCrit, 10 * couldCritSpellsPerS);
            averageMeleeCritChance = (chanceWhiteCritMH + chanceWhiteCritOH + chanceYellowCritMH + chanceYellowCritOH) / 4f + edUptime * edCritBonus;
        }
        float yellowAttacksPerSecond = hitsPerSWF + hitsPerSMHSS;
        if (character.getShamanTalents().getDualWield() == 1 && unhastedMHSpeed != 0)
            yellowAttacksPerSecond += hitsPerSOHSS;

        // set output variables
        edBonusCrit = edUptime * edCritBonus;
        //SetCritValues((1 + _stats.BonusCritChance) * (baseMeleeCrit + meleeCritModifier) + edBonusCrit + .00005f); //fudge factor for rounding
        setCritValues(baseMeleeCrit + meleeCritModifier + edBonusCrit + .00005f); //fudge factor for rounding
        meleeAttacksPerSec = hitsPerSMH + hitsPerSOH;
        meleeCritsPerSec = (whiteHitsPerSMH * chanceWhiteCritMH) + (whiteHitsPerSOH * chanceWhiteCritOH) +
                (yellowHitsPerSMH * chanceYellowCritMH) + (yellowHitsPerSOH * chanceYellowCritOH) +
                (stats.getMoteOfAnger() * 2 * getAverageWhiteCritChance());
        spellCritsPerSec = spellAttacksPerSec * getChanceSpellCrit();
        spellCastsPerSec = spellAttacksPerSec;
        spellMissesPerSec = spellAttacksPerSec * chanceSpellMiss;
        chanceMeleeHit = meleeAttacksPerSec / (swingsPerSMHMelee + swingsPerSOHMelee + 2f * wfProcsPerSecond + .25f + 1f / 6f);
        maxMana = stats.getMana();
        float spellhaste = stats.getSpellHaste() + StatConversion.getSpellHasteFromRating(stats.getHasteRating());
        averageFSDotTime = 18f / (1f + spellhaste);
    }

    private void setCritValues(float chanceCrit) {
        // first set max crit chance after crit depression is 76% - miss chance (ie 100% - 24% glancing - miss chance)
        // see http://elitistjerks.com/f31/t76785-crit_depression_combat_table/
        // miss chance includes dodge & parry
        chanceWhiteCritMH = Math.min(chanceCrit - whiteCritDepression, 1f - getGlancingRate() - chanceWhiteMissMH);
        chanceWhiteCritOH = Math.min(chanceCrit - whiteCritDepression, 1f - getGlancingRate() - chanceWhiteMissOH);
        chanceYellowCritMH = Math.min(chanceCrit - yellowCritDepression, 1f - chanceYellowMissMH);
        chanceYellowCritOH = Math.min(chanceCrit - yellowCritDepression, 1f - chanceYellowMissOH);
        if (chanceCrit - whiteCritDepression > 1f - getGlancingRate() - chanceWhiteMissMH)
            overMeleeCritCap = chanceCrit - whiteCritDepression - (1f - getGlancingRate() - chanceWhiteMissMH);
        else
            overMeleeCritCap = 0f;
        // now apply min 1% crit chance
        chanceWhiteCritMH = Math.max(0.01f, chanceWhiteCritMH);
        chanceWhiteCritOH = Math.max(0.01f, chanceWhiteCritOH);
        chanceYellowCritMH = Math.max(0.01f, chanceYellowCritMH);
        chanceYellowCritOH = Math.max(0.01f, chanceYellowCritOH);
    }

    private float calculateFlurryUptime(float c, float h, float m) { // c = crit rate, h = hit rate, m = miss rate, assuming hits as noncrits only
        h = h - c; // remove crits from hit figure
        float result = 1 - (float) Math.pow(1 - c, 3) + 3 * c * m * h * h + 6 * c * m * m * h * h + 3 * c * m * m * h +
                c * m * m * m * (-6 * h * h * m * m - m * m - 3 * h * m * m + 15 * h * h * m + 2 * m + 7 * h * m - 10 * h * h - 1 - 4 * h) / (float) Math.pow(m - 1, 3);
        return result;
    }

    private float getFireElementalUptimeImpl() {
        if (!calcOpts.priorityInUse(EnhanceAbility.FireElemental))
            return 0f;
        float cooldown = talents.isGlyphofFireElementalTotem() ? 300f : 600f;
        float duration = 120f;
        float totalDuration = 0f;
        float uses = 0f;
        while (totalDuration < fightLength) {
            if (totalDuration + duration >= fightLength) {   // if next totem drop will exceed remaining fight length then only process part available
                uses = (fightLength - totalDuration) / duration;
                totalDuration += duration;
            } else {
                uses++;
                totalDuration += cooldown;
            }
        }
        return uses * duration / fightLength;
    }

    public float getMeleeCritsPerSec() {
        return meleeCritsPerSec;
    }

    public float getMeleeAttacksPerSec() {
        return meleeAttacksPerSec;
    }

    public float getSpellCritsPerSec() {
        return spellCritsPerSec;
    }

    public float getSpellAttacksPerSec() {
        return spellAttacksPerSec;
    }

    public float getSpellCastsPerSec() {
        return spellCastsPerSec;
    }

    public float getSpellMissesPerSec() {
        return spellMissesPerSec;
    }
}
