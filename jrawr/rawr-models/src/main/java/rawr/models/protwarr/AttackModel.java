// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtWarr/AttackModel.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protwarr;

import rawr.base.character.Character;
import rawr.base.stats.Stats;
import rawr.base.talents.WarriorTalents;

import java.util.LinkedHashMap;
import java.util.Map;

public class AttackModel {
    private Character character;
    private CalculationOptionsProtWarr options;
    private Stats stats;
    private WarriorTalents talents;
    private DefendTable defendTable;
    private ParryModel parryModel;

    public AbilityModelList abilities = new AbilityModelList();

    private AttackModelMode attackModelMode;

    public AttackModelMode getAttackModelMode() {
        return attackModelMode;
    }

    public void setAttackModelMode(AttackModelMode value) {
        attackModelMode = value;
        calculate();
    }

    private RageModelMode rageModelMode;

    public RageModelMode getRageModelMode() {
        return rageModelMode;
    }

    public void setRageModelMode(RageModelMode value) {
        rageModelMode = value;
        calculate();
    }

    private String name;

    public String getName() {
        return name;
    }

    public String getShortName() {
        return name.replace("Sword and Board", "SnB").replace("Revenge + Shockwave", "Rev + Shockwave");
    }

    private String description;

    public String getDescription() {
        return description;
    }

    private float threatPerSecond;

    public float getThreatPerSecond() {
        return threatPerSecond;
    }

    private float damagePerSecond;

    public float getDamagePerSecond() {
        return damagePerSecond;
    }

    private float weaponAttacksPerSecond;

    public float getWeaponAttacksPerSecond() {
        return weaponAttacksPerSecond;
    }

    private float hitsPerSecond;

    public float getHitsPerSecond() {
        return hitsPerSecond;
    }

    private float critsPerSecond;

    public float getCritsPerSecond() {
        return critsPerSecond;
    }

    private float attackerSwingsPerSecond;

    public float getAttackerSwingsPerSecond() {
        return attackerSwingsPerSecond;
    }

    private float attackerHitsPerSecond;

    public float getAttackerHitsPerSecond() {
        return attackerHitsPerSecond;
    }

    private void calculate() {
        Map<Ability, Float> modelAbilities = new LinkedHashMap<Ability, Float>();
        float modelLength = 0.0f;
        float modelThreat = 0.0f;
        float modelDamage = 0.0f;
        float modelCrits = 0.0f;
        float modelHits = 0.0f;
        float modelWeaponAttacks = 0.0f;

        // Rotation Auto-Detection
        if (attackModelMode == AttackModelMode.Optimal) {
            if (talents.getShockwave() == 1 && talents.getSwordAndBoard() == 3)
                if (talents.getImprovedRevenge() > 0)
                    setAttackModelMode(AttackModelMode.FullProtectionRevenge);
                else
                    setAttackModelMode(AttackModelMode.FullProtection);
            else if (talents.getSwordAndBoard() == 3)
                if (talents.getImprovedRevenge() > 0)
                    setAttackModelMode(AttackModelMode.SwordAndBoardRevenge);
                else
                    setAttackModelMode(AttackModelMode.SwordAndBoard);
            else if (talents.getDevastate() == 1)
                if (talents.getImprovedRevenge() > 0)
                    setAttackModelMode(AttackModelMode.DevastateRevenge);
                else
                    setAttackModelMode(AttackModelMode.Devastate);
            else if (talents.getUnrelentingAssault() == 2)
                setAttackModelMode(AttackModelMode.UnrelentingAssault);
            else
                setAttackModelMode(AttackModelMode.Basic);
        }

        switch (attackModelMode) {
            case Basic: {
                // Basic Rotation
                // Shield Slam -> Revenge -> Sunder Armor -> Sunder Armor
                name = "Basic Cycle";
                description = "Shield Slam -> Revenge -> Sunder Armor -> Sunder Armor";
                modelLength = 6.0f;
                modelAbilities.put(Ability.ShieldSlam, 1.0f);
                modelAbilities.put(Ability.Revenge, 1.0f);
                modelAbilities.put(Ability.SunderArmor, 2.0f);
                break;
            }
            case Devastate: {
                // Devastate Rotation
                // Shield Slam -> Devastate -> Devastate -> Devastate
                name = "Devastate";
                description = "Shield Slam -> Devastate -> Devastate -> Devastate";
                modelLength = 6.0f;
                modelAbilities.put(Ability.ShieldSlam, 1.0f);
                modelAbilities.put(Ability.Devastate, 3.0f);
                break;
            }
            case DevastateRevenge: {
                // Devastate + Revenge Rotation
                // Shield Slam -> Revenge -> Devastate -> Devastate
                name = "Devastate + Revenge";
                description = "Shield Slam -> Revenge -> Devastate -> Devastate";
                modelLength = 6.0f;
                modelAbilities.put(Ability.ShieldSlam, 1.0f);
                modelAbilities.put(Ability.Revenge, 1.0f);
                modelAbilities.put(Ability.Devastate, 2.0f);
                break;
            }
            case SwordAndBoard:
            case SwordAndBoardRevenge:
            case FullProtection:
            case FullProtectionRevenge: {
                // Sword And Board Rotation
                // Shield Slam > Revenge > Devastate
                // The distribution of abilities in the model is as follows:
                // 1.0 * Shield Slam + 0.73 * Revenge + 1.4596 * Devastate
                // -or-
                // Shield Slam > Revenge > Devastate @ 3s Shield Slam Cooldown > Shockwave > Devastate
                // The distribution of abilities in the model is as follows:
                // 1.0 * Shield Slam + 0.73 * Revenge + 1.133 * Devastate + 0.3266 * (Concussion Blow/Shockwave/Devastate)
                // The cycle length is 4.7844s, abilities per cycle is 3.1896
                name = "Sword and Board";
                description = "Shield Slam > ";
                if (attackModelMode == AttackModelMode.SwordAndBoardRevenge || attackModelMode == AttackModelMode.FullProtectionRevenge) {
                    name += " + Revenge";
                    description += "Revenge > Devastate";
                } else
                    description += "Devastate";
                if (attackModelMode == AttackModelMode.FullProtection || attackModelMode == AttackModelMode.FullProtectionRevenge) {
                    name += " + Shockwave";
                    description += "\n@ 1.5s Shield Slam Cooldown: Shockwave > Devastate";
                }

                modelLength = 4.7844f;
                modelAbilities.put(Ability.ShieldSlam, 1.0f);
                modelAbilities.put(Ability.Devastate, 1.133f);

                // Add Revenge, if applicable
                if (attackModelMode == AttackModelMode.SwordAndBoardRevenge || attackModelMode == AttackModelMode.FullProtectionRevenge)
                    modelAbilities.put(Ability.Revenge, 0.73f);
                else
                    modelAbilities.merge(Ability.Devastate, 0.73f, Float::sum);

                // Add Shockwave, if applicable
                if (attackModelMode == AttackModelMode.FullProtection || attackModelMode == AttackModelMode.FullProtectionRevenge) {
                    modelAbilities.put(Ability.Shockwave, (0.3266f / 3.0f));
                    modelAbilities.merge(Ability.Devastate, (0.3266f / 3.0f) * 2.0f, Float::sum);
                } else
                    modelAbilities.merge(Ability.Devastate, 0.3266f, Float::sum);

                break;
            }
            case UnrelentingAssault: {
                name = "Unrelenting Assault";
                description = "Revenge Only";
                modelLength = 1.0f;
                modelAbilities.put(Ability.Revenge, 1.0f);
                break;
            }
        }

        // Accumulate base model statistics
        for (Map.Entry<Ability, Float> modelAbility : modelAbilities.entrySet()) {
            AbilityModel ability = abilities.get(modelAbility.getKey());
            modelThreat += ability.getThreat() * modelAbility.getValue();
            modelDamage += ability.getDamage() * modelAbility.getValue();
            if (ability.getDamage() > 0.0f) {
                modelHits += ability.getHitPercentage() * modelAbility.getValue();
                modelCrits += ability.getCritPercentage() * modelAbility.getValue();
            }
            if (ability.getIsWeaponAttack())
                modelWeaponAttacks += modelAbility.getValue();
        }

        // Simple GCD-Based Latency Adjustment
        modelLength *= Lookup.globalCooldownSpeed(character, true) / Lookup.globalCooldownSpeed(character, false);

        // Weapon Swings/Heroic Strike
        float attackerSwings = (modelLength / parryModel.getBossAttackSpeed());
        float attackerHits = defendTable.getAnyHit() * attackerSwings;
        float weaponSwings = modelLength / parryModel.getWeaponSpeed();
        float heroicStrikePercentage = Math.max(0.0f, Math.min(1.0f, options.getHeroicStrikeFrequency()));

        AbilityModel heroicStrike = abilities.get(Ability.HeroicStrike);
        modelThreat += heroicStrike.getThreat() * weaponSwings * heroicStrikePercentage;
        modelDamage += heroicStrike.getDamage() * weaponSwings * heroicStrikePercentage;
        modelHits += heroicStrike.getHitPercentage() * weaponSwings * heroicStrikePercentage;
        modelCrits += heroicStrike.getCritPercentage() * weaponSwings * heroicStrikePercentage;

        AbilityModel whiteSwing = abilities.get(Ability.None);
        modelThreat += whiteSwing.getThreat() * weaponSwings * (1.0f - heroicStrikePercentage);
        modelDamage += whiteSwing.getDamage() * weaponSwings * (1.0f - heroicStrikePercentage);
        modelCrits += whiteSwing.getCritPercentage() * weaponSwings * (1.0f - heroicStrikePercentage);
        modelHits += whiteSwing.getHitPercentage() * weaponSwings * (1.0f - heroicStrikePercentage);

        modelWeaponAttacks += weaponSwings;

        // Damage Shield
        AbilityModel damageShield = abilities.get(Ability.DamageShield);
        modelThreat += damageShield.getThreat() * attackerHits;
        modelDamage += damageShield.getDamage() * attackerHits;

        // Deep Wounds
        AbilityModel deepWounds = abilities.get(Ability.DeepWounds);
        modelThreat += deepWounds.getThreat() * modelCrits;
        modelDamage += deepWounds.getDamage() * modelCrits;

        // Misc. Power Gains
        modelThreat += defendTable.getDodgeParryBlock() * (modelLength / parryModel.getBossAttackSpeed()) * 25.0f *
                (talents.getShieldSpecialization() * 0.2f);
        modelThreat += defendTable.getDodgeParryBlock() * (modelLength / parryModel.getBossAttackSpeed()) * 1.0f *
                Lookup.stanceThreatMultipler(character, stats) * (talents.getImprovedDefensiveStance() * 0.5f);

        // Vigilance, is already calculated as TPS
        if (options.getUseVigilance())
            modelThreat += abilities.get(Ability.Vigilance).getThreat() * modelLength;

        // Final Per-Second Calculations
        threatPerSecond = modelThreat / modelLength;
        damagePerSecond = modelDamage / modelLength;
        weaponAttacksPerSecond = modelWeaponAttacks / modelLength;
        hitsPerSecond = modelHits / modelLength;
        critsPerSecond = modelCrits / modelLength;
        attackerSwingsPerSecond = attackerSwings / modelLength;
        attackerHitsPerSecond = attackerHits / modelLength;
    }

    public AttackModel(Character character, Stats stats, CalculationOptionsProtWarr options, AttackModelMode attackModelMode) {
        this(character, stats, options, attackModelMode, RageModelMode.Infinite);
    }

    public AttackModel(Character character, Stats stats, CalculationOptionsProtWarr options, AttackModelMode attackModelMode, RageModelMode rageModelMode) {
        this.character = character;
        this.stats = stats;
        this.options = options;
        this.talents = character.getWarriorTalents();
        this.defendTable = new DefendTable(character, stats, options);
        this.parryModel = new ParryModel(character, stats, options);
        this.attackModelMode = attackModelMode;
        this.rageModelMode = rageModelMode;

        abilities.add(Ability.None, character, stats, options);
        abilities.add(Ability.Cleave, character, stats, options);
        abilities.add(Ability.ConcussionBlow, character, stats, options);
        abilities.add(Ability.DamageShield, character, stats, options);
        abilities.add(Ability.DeepWounds, character, stats, options);
        abilities.add(Ability.Devastate, character, stats, options);
        abilities.add(Ability.HeroicStrike, character, stats, options);
        abilities.add(Ability.HeroicThrow, character, stats, options);
        abilities.add(Ability.MockingBlow, character, stats, options);
        abilities.add(Ability.Rend, character, stats, options);
        abilities.add(Ability.Revenge, character, stats, options);
        abilities.add(Ability.ShieldSlam, character, stats, options);
        abilities.add(Ability.Shockwave, character, stats, options);
        abilities.add(Ability.Slam, character, stats, options);
        abilities.add(Ability.SunderArmor, character, stats, options);
        abilities.add(Ability.ThunderClap, character, stats, options);
        abilities.add(Ability.Vigilance, character, stats, options);

        calculate();
    }
}
