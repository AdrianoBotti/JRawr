// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Warlock/Pets.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// FRAMEWORK ADAPTATION: Pet.ALL_PETS (C# reflection over assembly types) is a fixed list; the
// pet factory in CharacterCalculationsWarlock replaces Type.GetType + Activator.CreateInstance.
package rawr.models.warlock;

import rawr.base.character.CharacterRace;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;
import rawr.base.util.Utilities;
import rawr.base.stats.WeightedStat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Pet {

    public static final List<String> ALL_PETS = new ArrayList<String>(Arrays.asList(
            "Felguard", "Felhunter", "Imp", "Succubus", "Voidwalker"));

    public CharacterCalculationsWarlock mommy;
    public Stats stats;

    public float baseStamina;
    public float staminaCoef;
    public float healthPerStamina;
    public float baseHealth;

    public float baseIntellect;
    public float intellectCoef;
    public float manaPerIntellect;
    public float baseMana;
    public float spellCritPerIntellect;
    public float baseSpellCrit;

    public float baseSpellPower;
    public float spellPowerCoef;

    public float baseAttackPower;
    public float attackPowerCoef;

    public float baseMeleeDamage;
    public float damagePerAttackPower;

    public float specialCooldown;
    public float specialCastTime;
    public float specialBaseDamage;
    public float specialDamagePerSpellPower;

    public SpellModifiers totalModifiers;
    public SpellModifiers meleeModifiers;
    public SpellModifiers specialModifiers;

    public Pet(
            CharacterCalculationsWarlock mommy,
            float baseHealth,
            float healthPerStamina) {

        this.mommy = mommy;
        totalModifiers = new SpellModifiers();
        meleeModifiers = new SpellModifiers();
        specialModifiers = new SpellModifiers();

        baseStamina = 328f;
        staminaCoef = .75f;
        this.healthPerStamina = healthPerStamina;
        this.baseHealth = baseHealth;

        baseIntellect = 150f;
        intellectCoef = .3f;
        manaPerIntellect = 11.55f;
        baseMana = 1343f;
        baseSpellCrit = .03333f;

        baseSpellPower = 0f;
        spellPowerCoef = .15f;

        baseAttackPower = 294f;
        attackPowerCoef = .57f;
    }

    public void calcStats1() {
        // Stam & Int have to happen in this stage, so that Demonic
        // Knowledge is in effect when calculating the benefit of a
        // Demonic Pact proc.

        // Crit has to happen in this stage, so that Empowered Imp gives
        // the warlocks the right amount of crit.

        float vitality = mommy.talents.getFelVitality() * .05f;
        float tacticsCrit = .02f * mommy.talents.getDemonicTactics()
                + .1f * mommy.talents.getImprovedDemonicTactics()
                        * (mommy.calcSpellCrit() - mommy.stats.getSpellCritOnTarget());
        stats = new Stats();
        stats.setStamina(baseStamina + staminaCoef * mommy.calcStamina());
        stats.setIntellect(baseIntellect + intellectCoef * mommy.calcIntellect());
        stats.setStrength(297f);
        stats.setAgility(90f);
        stats.setBonusStaminaMultiplier(vitality);
        stats.setBonusIntellectMultiplier(vitality);
        stats.setSpellCrit(baseSpellCrit + tacticsCrit + mommy.stats.getWarlock2T9());
        stats.setSpellPower(baseSpellPower);
        stats.setAttackPower(baseAttackPower);
        stats.setPhysicalCrit(.0329f + tacticsCrit + mommy.stats.getWarlock2T9());
        stats.accumulate(mommy.petBuffs);

        mommy.add4pT10(totalModifiers);
        finalizeModifiers();
        specialModifiers.accumulate(totalModifiers);
        meleeModifiers.accumulate(totalModifiers);

        stats.setSpellHaste(getHasteWithProcs(s -> s.getSpellHaste()));
        stats.setPhysicalHaste(getHasteWithProcs(s -> s.getPhysicalHaste()));
    }

    private float getHasteWithProcs(StatExtractor statExtractor) {
        Map<Integer, Float> periods = new LinkedHashMap<Integer, Float>();
        Map<Integer, Float> chances = new LinkedHashMap<Integer, Float>();
        periods.put(Trigger.Use.ordinal(), 0f);
        chances.put(Trigger.Use.ordinal(), 1f);
        WeightedStat[] hasteProcs = mommy.getUptimes(
                stats,
                periods,
                chances,
                statExtractor,
                (a, b, c, d, e, f, g, h) -> SpecialEffect
                        .getAverageCombinedUptimeCombinationsMultiplicative(
                                a, b, c, d, e, f, g, h));
        float avgProcHaste = 0f;
        for (WeightedStat proc : hasteProcs) {
            avgProcHaste += proc.chance() * proc.value();
        }
        return (1f + statExtractor.extract(stats)) * (1f + avgProcHaste) - 1f;
    }

    public void calcStats2() {
        stats.setSpellPower(stats.getSpellPower() + spellPowerCoef * mommy.calcSpellPower());
        stats.setAttackPower(stats.getAttackPower() + attackPowerCoef * mommy.calcSpellPower());
    }

    protected void finalizeModifiers() {
        totalModifiers.addMultiplicativeMultiplier(stats.getBonusDamageMultiplier());
        meleeModifiers.addAdditiveMultiplier(.04f * mommy.talents.getUnholyPower());
        meleeModifiers.addMultiplicativeMultiplier(stats.getBonusPhysicalDamageMultiplier());
        if (mommy.character.getRace() == CharacterRace.Orc) {
            totalModifiers.addAdditiveMultiplier(.05f);
        }
    }

    public float calcStamina() {
        return StatUtils.calcStamina(stats);
    }

    public float calcIntellect() {
        return StatUtils.calcIntellect(stats);
    }

    public float calcHealth() {
        // sadly, pets do not have the same stamina->health conversion as
        // players, so have to write our own CalcHealth method.
        return (stats.getHealth() + healthPerStamina * calcStamina())
                * (1 + stats.getBonusHealthMultiplier());
    }

    public float calcSpellPower() {
        return StatUtils.calcSpellPower(stats);
    }

    public float calcSpellCrit() {
        return StatUtils.calcSpellCrit(stats);
    }

    public float calcAttackPower() {
        return StatUtils.calcAttackPower(stats, 1f, 0f);
    }

    public float calcMeleeCrit() {
        return StatUtils.calcPhysicalCrit(
                stats, .00019f, mommy.options.getTargetLevel() - 80);
    }

    public float calcMeleeHaste() {
        return 1f + stats.getPhysicalHaste();
    }

    public float calcMeleeSpeed() {
        return 2f / calcMeleeHaste();
    }

    public float calcMeleeHitChance() {
        // the warlock's miss rate, not including buff/debuff
        float miss = .17f
                - mommy.calcSpellHit()
                + stats.getSpellHit()
                - mommy.talents.getSuppression() * .01f;

        // adjust to melee miss rate
        miss *= 8f / 13f;

        // add in physical hit buff/debuffs
        miss -= stats.getPhysicalHit();

        return Math.min(1f, 1f - miss);
    }

    public float calcMeleeDamage() {
        return calcMeleeDamage(true, 0f);
    }

    protected float calcMeleeDamage(boolean canGlance, float bonusDamage) {
        int level = mommy.options.getTargetLevel();
        int levelDelta = level - 80;

        float characterSheetDamage = baseMeleeDamage
                + damagePerAttackPower * calcAttackPower()
                + bonusDamage;
        float combatTableModifier = calcMeleeHitChance()
                + calcMeleeCrit()
                - StatConversion.WHITE_DODGE_CHANCE_CAP[levelDelta];
        if (canGlance) {
            combatTableModifier -= .3f * StatConversion.WHITE_GLANCE_CHANCE_CAP[levelDelta];
        }
        float armorModifier = 1f
                - StatConversion.getArmorDamageReduction(
                        level,
                        StatConversion.NPC_ARMOR[levelDelta],
                        stats.getArmorPenetration(), // arpen debuffs
                        0f, // arpen buffs
                        0f); // arpen rating
        return characterSheetDamage
                * combatTableModifier
                * armorModifier
                * meleeModifiers.getFinalDirectMultiplier();
    }

    public float calcMeleeDps() {
        return calcMeleeDamage() / calcMeleeSpeed();
    }

    public float getSpecialSpeed() {
        return specialCooldown
                + specialCastTime / StatUtils.calcSpellHaste(stats);
    }

    public float getSpecialDamage() {
        float resist = StatConversion.getAverageResistance(
                80, mommy.options.getTargetLevel(), 0f, 0f);
        float nonCrit = (specialBaseDamage
                    + specialDamagePerSpellPower * calcSpellPower())
                * specialModifiers.getFinalDirectMultiplier()
                * (1 - resist);
        float crit = nonCrit * specialModifiers.getFinalCritMultiplier();
        float critChance = calcSpellCrit();
        return mommy.hitChance
                * Utilities.getWeightedSum(
                        crit, critChance, nonCrit, 1 - critChance);
    }

    public float calcSpecialDps() {
        float speed = getSpecialSpeed();
        if (speed == 0) {
            return 0f;
        } else {
            return getSpecialDamage() / getSpecialSpeed();
        }
    }

    public float applyPactProcBenefit() {
        float benefit = getPactProcBenefit();
        stats.setSpellPower(stats.getSpellPower() + benefit);
        return benefit;
    }

    public float getPactProcBenefit() {
        float pact = .02f * mommy.talents.getDemonicPact();
        if (pact == 0) {
            return 0f;
        }

        float buff = StatUtils.getBuffEffect(
                mommy.character.getActiveBuffs(),
                mommy.calcSpellPower() * pact,
                "Spell Power",
                s -> s.getSpellPower());
        if (buff == 0) {
            return 0f;
        }

        SpecialEffect pactEffect = new SpecialEffect(Trigger.Use, null, 45f, 20f);
        float meleeRate;
        if (baseMeleeDamage == 0) {
            meleeRate = 0f;
        } else {
            meleeRate = 1 / calcMeleeSpeed();
        }
        float spellRate;
        if (specialDamagePerSpellPower == 0) {
            spellRate = 0f;
            float specialSpeed = getSpecialSpeed();
            if (specialSpeed > 0) {
                meleeRate += 1 / getSpecialSpeed();
            }
        } else {
            spellRate = 1 / getSpecialSpeed();
        }
        float triggerRate = 1 / (meleeRate + spellRate);
        float uprate = pactEffect.getAverageUptime(
                triggerRate,
                Utilities.getWeightedSum(
                        mommy.hitChance * calcSpellCrit(),
                        spellRate,
                        calcMeleeCrit(),
                        meleeRate),
                triggerRate,
                mommy.options.getDuration());

        return uprate * buff;
    }

    protected float getEmpowermentCooldown() {
        return 60f * (1f - .1f * mommy.talents.getNemesis());
    }
}

class Felguard extends Pet {

    public Felguard(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                1820f, // baseHealth,
                11f); // healthPerStamina

        baseMeleeDamage = (410f + 627f) / 2f;
        damagePerAttackPower = .187f;

        specialCooldown = 6f;
    }

    @Override
    protected void finalizeModifiers() {
        super.finalizeModifiers();

        float apBonus = 1.5f + mommy.talents.getDemonicBrutality() * .1f;
        if (mommy.talents.isGlyphFelguard()) {
            apBonus *= 1.2f;
        }
        stats.setBonusAttackPowerMultiplier(
                (1f + stats.getBonusAttackPowerMultiplier()) * apBonus - 1f);
        totalModifiers.addMultiplicativeMultiplier(
                mommy.talents.getMasterDemonologist() * .01f);

        if (mommy.talents.getDemonicEmpowerment() > 0) {
            Stats effectStats = new Stats();
            effectStats.setPhysicalHaste(.2f);
            stats.addSpecialEffect(
                    new SpecialEffect(
                            Trigger.Use,
                            effectStats,
                            15f,
                            getEmpowermentCooldown()));
        }
    }

    @Override
    public float getSpecialDamage() {
        return calcMeleeDamage(false, 72f);
    }
}

class Felhunter extends Pet {

    public Felhunter(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                1671f, // baseHealth,
                9.5f); // healthPerStamina,

        baseMeleeDamage = (277f + 416f) / 2f;
        damagePerAttackPower = .12f;

        specialBaseDamage = (112f + 159f) / 2f;
        specialDamagePerSpellPower = .5f;
        specialCooldown = 6f - mommy.talents.getImprovedFelhunter() * 2f;
    }

    @Override
    protected void finalizeModifiers() {
        super.finalizeModifiers();

        // multipliers go into SpecialModifiers
        specialModifiers.addMultiplicativeMultiplier(
                stats.getBonusShadowDamageMultiplier());
        if (mommy.castSpells.containsKey("Corruption")) {
            specialModifiers.addAdditiveMultiplier(.15f);
        }
        if (mommy.castSpells.containsKey("Curse Of Agony")) {
            specialModifiers.addAdditiveMultiplier(.15f);
        }
        if (mommy.castSpells.containsKey("Immolate")) {
            specialModifiers.addAdditiveMultiplier(.15f);
        }
        if (mommy.castSpells.containsKey("Unstable Affliction")) {
            specialModifiers.addAdditiveMultiplier(.15f);
        }
    }
}

class Imp extends Pet {

    public Imp(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                2877f, // baseHealth,
                8.4f); // healthPerStamina,

        baseStamina = 118f;
        baseIntellect = 368f;
        manaPerIntellect = 4.95f;
        baseMana = 1052f;
        baseSpellCrit = .01f;

        specialBaseDamage = (213f + 239f) / 2f;
        specialDamagePerSpellPower = .79f;
        specialCastTime = 2.5f - mommy.talents.getDemonicPower() * .25f;
    }

    public float getCritsPerSec() {
        return mommy.hitChance * calcSpellCrit() / getSpecialSpeed();
    }

    @Override
    protected void finalizeModifiers() {
        super.finalizeModifiers();

        float demonologist = mommy.talents.getMasterDemonologist() * .01f;

        // crit goes into the stats object
        stats.setSpellCrit(stats.getSpellCrit() + demonologist);
        if (mommy.talents.getDemonicEmpowerment() > 0) {
            stats.setSpellCrit(stats.getSpellCrit() + .2f * 30f / getEmpowermentCooldown());
        }

        // multipliers go into SpecialModifiers
        specialModifiers.addMultiplicativeMultiplier(
                stats.getBonusFireDamageMultiplier());
        specialModifiers.addAdditiveMultiplier(.1f * mommy.talents.getImprovedImp());
        specialModifiers.addAdditiveMultiplier(.04f * mommy.talents.getUnholyPower());
        specialModifiers.addMultiplicativeMultiplier(demonologist);
        specialModifiers.addAdditiveMultiplier(.1f * mommy.talents.getEmpoweredImp());
        specialModifiers.addCritBonusMultiplier(.2f * mommy.talents.getRuin());
        if (mommy.talents.isGlyphImp()) {
            specialModifiers.addAdditiveMultiplier(.2f);
        }
    }
}

class Succubus extends Pet {

    public Succubus(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                1671f, // baseHealth,
                9f); // healthPerStamina,

        baseMeleeDamage = (363f + 546f) / 2f;
        damagePerAttackPower = .157f;

        specialBaseDamage = 248f;
        specialDamagePerSpellPower = .5f;
        specialCooldown = 12f - mommy.talents.getDemonicPower() * 3f;
    }

    @Override
    protected void finalizeModifiers() {
        super.finalizeModifiers();

        float demonologist = mommy.talents.getMasterDemonologist() * .01f;

        // crit goes into the stats object
        stats.setSpellCrit(stats.getSpellCrit() + demonologist);

        // multipliers go into SpecialModifiers
        specialModifiers.addMultiplicativeMultiplier(
                stats.getBonusShadowDamageMultiplier());
        specialModifiers.addAdditiveMultiplier(demonologist);
    }
}

class Voidwalker extends Pet {

    public Voidwalker(CharacterCalculationsWarlock mommy) {
        super(
                mommy,
                1671f, // baseHealth,
                11f); // healthPerStamina,

        baseMeleeDamage = (297f + 448f) / 2f;
        damagePerAttackPower = .13f;
    }
}
