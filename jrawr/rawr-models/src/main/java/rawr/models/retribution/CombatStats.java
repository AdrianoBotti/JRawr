// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/CombatStats.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

import rawr.base.character.Character;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterSlot;
import rawr.base.items.ItemInstance;
import rawr.base.talents.PaladinTalents;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class CombatStats {

    public CombatStats(Character character, Stats stats) {
        this.character = character;
        this.stats = stats;
        this.calcOpts = character.getCalculationOptions() instanceof CalculationOptionsRetribution
                ? (CalculationOptionsRetribution) character.getCalculationOptions()
                : null;
        this.talents = character.getPaladinTalents();
        updateCalcs();
    }

    private final Character character;
    private final Stats stats;
    private final CalculationOptionsRetribution calcOpts;
    private final PaladinTalents talents;

    public Character getCharacter() {
        return character;
    }

    public Stats getStats() {
        return stats;
    }

    public CalculationOptionsRetribution getCalcOpts() {
        return calcOpts;
    }

    public PaladinTalents getTalents() {
        return talents;
    }

    public float weaponDamage;
    public float baseWeaponSpeed;
    public float attackSpeed;
    public float normalWeaponDamage;

    public float avengingWrathMulti = 1f;
    public float armorReduction = 1f;
    public final float partialResist = 0.94f;

    public float getMeleeMissChance() {    // Chance to miss a white/yellow
        return Math.max(StatConversion.WHITE_MISS_CHANCE_CAP[calcOpts.getTargetLevel() - 80] - stats.getPhysicalHit(), 0f);
    }

    public float getRangedMissChance() {    // Chance to miss a ranged attack (HoW)
        // Should be 'RangedHit' instead of PhysicalHit, pala's won't be gearing for specific ranged hit, and there's no RangedHit stat in Stats (only RangedHitRating).
        return Math.max(StatConversion.WHITE_MISS_CHANCE_CAP[calcOpts.getTargetLevel() - 80] - stats.getPhysicalHit(), 0f);
    }

    public float getToBeParriedChance() {
        return Math.max(StatConversion.WHITE_PARRY_CHANCE_CAP[calcOpts.getTargetLevel() - 80] - StatConversion.getDodgeParryReducFromExpertise(stats.getExpertise(), CharacterClass.Paladin), 0f);
    }

    public float getToBeDodgedChance() {
        return Math.max(StatConversion.WHITE_DODGE_CHANCE_CAP[calcOpts.getTargetLevel() - 80] - StatConversion.getDodgeParryReducFromExpertise(stats.getExpertise(), CharacterClass.Paladin), 0f);
    }

    public float getSpellMissChance() {
        return StatConversion.getSpellMiss(calcOpts.getTargetLevel() - 80, false);
    }

    public void updateCalcs() {
        float fightLength = calcOpts.getFightLength() * 60f;

        float bloodlustUptime = ((float) Math.floor(fightLength / 600f) * 40f + (float) Math.min(fightLength % 600f, 40f)) / fightLength;
        float bloodlustHaste = 1f + (calcOpts.getBloodlust() ? (bloodlustUptime * .3f) : 0f);

        float awUptime = (float) Math.ceil((fightLength - 20f) / (180f - talents.getSanctifiedWrath() * 30f)) * 20f / fightLength;
        avengingWrathMulti = 1f + awUptime * .2f;

        float targetArmor = StatConversion.NPC_ARMOR[calcOpts.getTargetLevel() - 80];

        float dr = StatConversion.getArmorDamageReduction(character.getLevel(), targetArmor, stats.getArmorPenetration(), 0f, stats.getArmorPenetrationRating());
        float drAW = dr * ((1 - awUptime) + (1 - .25f * talents.getSanctifiedWrath()) * awUptime);
        armorReduction = 1f - drAW;

        ItemInstance mainHand = character.get(CharacterSlot.MainHand);
        baseWeaponSpeed = (mainHand == null || mainHand.getSpeed() == 0.0f) ? 3.5f : mainHand.getSpeed(); // NOTE by Kavan: added a check against speed == 0, it can happen when item data is still being downloaded
        float baseWeaponDamage = mainHand == null ? 371.5f : (mainHand.getMinDamage() + mainHand.getMaxDamage()) / 2f;
        attackSpeed = baseWeaponSpeed / ((1f + stats.getPhysicalHaste()) * bloodlustHaste);
        weaponDamage = baseWeaponDamage + stats.getAttackPower() * baseWeaponSpeed / 14f;
        normalWeaponDamage = baseWeaponDamage + stats.getAttackPower() * 3.3f / 14f;
    }
}
