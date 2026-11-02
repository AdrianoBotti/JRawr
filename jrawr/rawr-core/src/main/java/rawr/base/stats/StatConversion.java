// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/StatConversion.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.stats;

import rawr.base.character.Character;
import rawr.base.character.CharacterClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * All level-80 conversion formulas of the game: rating to percentage, attribute to derived stat,
 * armor and resistance math, and the melee combat-outcome table. Pure static functions with no
 * state.
 *
 * <h2>Ratings vs. percentages</h2>
 *
 * <p>In-game gear grants <i>ratings</i> ("32 hit rating"), but combat resolves <i>percentages</i>
 * ("1% hit"). At level 80 each rating type has a fixed divisor; the constants at the top of this
 * class are exactly those divisors, e.g. {@link #RATING_PER_SPELLHIT} = 2623.199... means
 * {@code spellHitPercent = spellHitRating / 2623.199}. The rest of the rating-bonus stats (crit,
 * haste, hit, defense, dodge, parry, block, armor penetration, expertise, resilience) follow the
 * same pattern.</p>
 *
 * <p>Most methods exist in two forms: {@code getXFromRating(float rating, CharacterClass)} and
 * {@code getXFromRating(float rating)}. Check whether the class actually uses the class parameter
 * before assuming the overloads differ; several delegate. Truly class-dependent conversions index
 * per-class tables such as {@link #AGI_PER_PHYSICALCRIT} and {@link #AGI_PER_DODGE}.</p>
 *
 * <h2>Caps</h2>
 *
 * <p>{@code *_CHANCE_CAP} arrays are indexed by {@code targetLevel - attackerLevel} (0..3, i.e.
 * targets level 80..83). White attacks, dual-wield white attacks and yellow (special) attacks have
 * separate caps, and some caps differ for glancing blows. These values are the encounter's baseline
 * miss/dodge/parry/glance/block chances before the attacker's own hit and expertise are applied.</p>
 *
 * <h2>The combat table</h2>
 *
 * <p>Every melee attack resolves as a single roll against a table of mutually exclusive outcomes.
 * A {@link CombatTable} holds the probability of each outcome (miss, dodge, parry, glancing blow,
 * block, crit, normal hit); {@link #getCombatTablePlayerVsNpc(int, int, boolean, boolean, float, float, float, float)}
 * builds that table from the caps plus the attacker's hit, expertise, crit and position.
 * {@link CombatTable#evasion()} returns the total avoid chance (used by tank models). Damage models
 * multiply each outcome by its damage factor.</p>
 *
 * <h2>Diminishing returns</h2>
 *
 * <p>Dodge, parry and miss do not grow linearly: piling on rating yields less and less percentage
 * (the game applies a class-specific cap curve). {@link #getDRAvoidanceChance} implements this:
 * base avoidance (attribute-derived) is kept separate from "modified" avoidance (rating-derived),
 * which is passed through {@link #DRMath} with per-class {@link #CAP_DODGE}/{@link #CAP_PARRY}/
 * {@link #CAP_MISSED} and {@link #DR_COEFFIENT} constants, then clamped. Tank models call this to
 * answer "how often will I be hit?" and to find the defense rating needed for crit immunity
 * ({@link #getDefenseRatingNeeded}).</p>
 *
 * <h2>Armor, resistance and regen</h2>
 *
 * <ul>
 *   <li>{@link #getArmorDamageReduction} &mdash; physical damage reduction from armor, including
 *       armor-ignore debuffs, armor-ignore buffs and the armor-penetration rating cap (the game
 *       limits how much armor can be removed at once).</li>
 *   <li>{@link #getResistanceTable} / {@link #getAverageResistance} &mdash; magic resistance
 *       resolves as partial resists; this builds the 0..90% bucket distribution a spell model
 *       averages over. {@link #getResistanceTableString} renders the tooltip text.</li>
 *   <li>{@link #getSpiritRegenSec} &mdash; out-of-combat mana regeneration as a function of spirit
 *       and intellect.</li>
 * </ul>
 *
 * <p><b>Porting notes:</b> {@link #roundHalfEven} replicates the .NET {@code Math.Round(x, digits)}
 * behavior (half-to-even on a decimal representation) and {@link #netRoundToString} the .NET display
 * rounding; keep them. The {@code long targetLevel} overloads exist because the C# code cast an
 * {@code int} to {@code uint}; the unsigned conversion is intentional.</p>
 */
public final class StatConversion {
    public static final float LEVEL_80_COMBATRATING_MODIFIER = 3.2789987789987789987789987789988f;
    public static final float RATING_PER_ARMORPENETRATION = 1399.572614f;
    public static final float RATING_PER_BLOCK = 1639.499474f;
    public static final float RATING_PER_DEFENSE = 4.918498039f;
    public static final float RATING_PER_DODGE = 4525.018692f;
    public static final float RATING_PER_EXPERTISE = 8.1974969f;
    public static final float RATING_PER_PARRY = 4525.018692f;
    public static final float RATING_PER_PHYSICALCRIT = 4590.598679f;
    public static final float RATING_PER_PHYSICALHASTE = 3278.998947f;
    public static final float RATING_PER_PHYSICALHIT = 3278.998947f;
    public static final float RATING_PER_RESILIENCE = 9427.122498f;
    public static final float RATING_PER_SPELLCRIT = 4590.598679f;
    public static final float RATING_PER_SPELLHASTE = 3278.998947f;
    public static final float RATING_PER_SPELLHIT = 2623.199272f;
    public static final float RATING_PER_ARMOR = 2.00f;
    public static final float RATING_PER_HEALTH = 10.00f;
    public static final float RATING_PER_MANA = 15.00f;
    public static final float RATING_PER_DODGEPARRYREDUC = 0.0025f;
    public static final float DEFENSE_RATING_AVOIDANCE_MULTIPLIER = 0.04f;
    public static final float BLOCKVALUE_PER_STR = 2.0f;

    public static final float[] WHITE_MISS_CHANCE_CAP = new float[]{0.0500f, 0.0520f, 0.0540f, 0.0800f};
    public static final float[] WHITE_MISS_CHANCE_CAP_DW = new float[]{0.2400f, 0.2420f, 0.2440f, 0.2700f};
    public static final float[] YELLOW_MISS_CHANCE_CAP = WHITE_MISS_CHANCE_CAP;

    public static final float[] WHITE_DODGE_CHANCE_CAP = new float[]{0.0500f, 0.0550f, 0.0600f, 0.0650f};
    public static final float[] YELLOW_DODGE_CHANCE_CAP = WHITE_DODGE_CHANCE_CAP;

    public static final float[] WHITE_PARRY_CHANCE_CAP = new float[]{0.0500f, 0.0550f, 0.0600f, 0.14f};
    public static final float[] YELLOW_PARRY_CHANCE_CAP = WHITE_PARRY_CHANCE_CAP;

    public static final float[] WHITE_GLANCE_CHANCE_CAP = new float[]{0.1000f, 0.1500f, 0.2000f, 0.2400f};
    public static final float[] YELLOW_GLANCE_CHANCE_CAP = new float[]{0.0000f, 0.0000f, 0.0000f, 0.0000f};

    public static final float[] WHITE_BLOCK_CHANCE_CAP = new float[]{0.0500f, 0.0520f, 0.0540f, 0.0650f};
    public static final float[] YELLOW_BLOCK_CHANCE_CAP = WHITE_BLOCK_CHANCE_CAP;

    public static final float[] NPC_LEVEL_CRIT_MOD = new float[]{-0.0000f, -0.0060f, -0.0120f, -0.0480f};

    public static final float[] NPC_ARMOR = new float[]{9729f, 10034f, 10338f, 10643f};

    public static final float INT_PER_SPELLCRIT = 166.66667f;
    public static final float REGEN_CONSTANT = 0.003345f;

    public static final float[] AGI_PER_PHYSICALCRIT = {0.0f,
            62.50f,
            52.083f,
            83.333f,
            83.333f,
            52.083f,
            62.50f,
            83.333f,
            51.0204f,
            50.505f,
            0.0f,
            83.333f,
    };

    public static final float[] AGI_PER_DODGE = {0.0f,
            84.74576271f,
            59.88023952f,
            86.20689655f,
            47.84688995f,
            59.88023952f,
            84.74576271f,
            59.88023952f,
            58.82352941f,
            59.88023952f,
            0.0f,
            47.84688995f,
    };

    public static final float[] DR_COEFFIENT = {0.0f,
            0.9560f,
            0.9560f,
            0.9880f,
            0.9880f,
            0.9530f,
            0.9560f,
            0.9880f,
            0.9530f,
            0.9530f,
            0.0f,
            0.9720f,
    };

    public static final float[] CAP_DODGE = {0.0f,
            88.129021f,
            88.129021f,
            145.560408f,
            145.560408f,
            150.375940f,
            88.129021f,
            145.560408f,
            150.375940f,
            150.375940f,
            0.0f,
            116.890707f,
    };

    public static final float[] CAP_DODGE_INV = {0.0f,
            0.011347f,
            0.011347f,
            0.006870f,
            0.006870f,
            0.006650f,
            0.011347f,
            0.006870f,
            0.006650f,
            0.006650f,
            0.0f,
            0.008555f,
    };

    public static final float[] CAP_PARRY = {0.0f,
            47.003525f,
            47.003525f,
            145.560408f,
            145.560408f,
            0f,
            47.003525f,
            145.560408f,
            0f,
            0f,
            0.0f,
            0f,
    };

    public static final float[] CAP_PARRY_INV = {0.0f,
            0.021275f,
            0.021275f,
            0.006870f,
            0.006870f,
            0f,
            0.021275f,
            0.006870f,
            0f,
            0f,
            0.0f,
            0f,
    };

    public static final float[] CAP_MISSED = {0.0f,
            16f,
            16f,
            0f,
            0f,
            0f,
            16f,
            0f,
            0f,
            0f,
            0.0f,
            0f,
    };

    private StatConversion() {
    }

    private static float attackerResistancePenalty(int levelDelta) {
        return levelDelta == 3 ? 95f : 0f;
    }

    public static float getAverageResistance(int attackerLevel, int targetLevel,
                                             float targetResistance, float attackerSpellPenetration) {
        float actualResistance = Math.max(0f, targetResistance - attackerSpellPenetration);
        return actualResistance / (attackerLevel * 5f + attackerResistancePenalty(attackerLevel - targetLevel) + actualResistance) + 0.02f * (float) Math.max(0, targetLevel - attackerLevel);
    }

    public static float getArmorFromAgility(float rating, CharacterClass characterClass) {
        return getArmorFromAgility(rating);
    }

    public static float getArmorFromAgility(float rating) {
        return rating * RATING_PER_ARMOR;
    }

    public static float getHealthFromStamina(float rating, CharacterClass characterClass) {
        return getHealthFromStamina(rating);
    }

    public static float getHealthFromStamina(float rating) {
        return rating <= 20 ? rating : (rating - 20) * RATING_PER_HEALTH + 20;
    }

    public static float getManaFromIntellect(float rating, CharacterClass characterClass) {
        return getManaFromIntellect(rating);
    }

    public static float getManaFromIntellect(float rating) {
        return rating <= 20 ? rating : (rating - 20) * RATING_PER_MANA + 20;
    }

    public static float getArmorPenetrationFromRating(float rating, CharacterClass characterClass) {
        return getArmorPenetrationFromRating(rating);
    }

    public static float getArmorPenetrationFromRating(float rating) {
        return rating / RATING_PER_ARMORPENETRATION;
    }

    public static float getRatingFromArmorPenetration(float rating, CharacterClass characterClass) {
        return getRatingFromArmorPenetration(rating);
    }

    public static float getRatingFromArmorPenetration(float rating) {
        return rating * RATING_PER_ARMORPENETRATION;
    }

    public static float getBlockFromRating(float rating, CharacterClass characterClass) {
        return getBlockFromRating(rating);
    }

    public static float getBlockFromRating(float rating) {
        return rating / RATING_PER_BLOCK;
    }

    public static float getBlockValueFromStrength(float str, CharacterClass characterClass) {
        return getBlockValueFromStrength(str);
    }

    public static float getBlockValueFromStrength(float str) {
        return str / BLOCKVALUE_PER_STR;
    }

    public static float getDefenseFromRating(float rating, CharacterClass characterClass) {
        return getDefenseFromRating(rating);
    }

    public static float getDefenseFromRating(float rating) {
        return (float) Math.floor(rating / RATING_PER_DEFENSE);
    }

    public static float getDodgeFromRating(float rating, CharacterClass characterClass) {
        return getDodgeFromRating(rating);
    }

    public static float getDodgeFromRating(float rating) {
        return rating / RATING_PER_DODGE;
    }

    public static float getExpertiseFromRating(float rating, CharacterClass characterClass) {
        return getExpertiseFromRating(rating);
    }

    public static float getExpertiseFromRating(float rating) {
        return rating / RATING_PER_EXPERTISE;
    }

    public static float getRatingFromExpertise(float value, CharacterClass characterClass) {
        return getRatingFromExpertise(value);
    }

    public static float getRatingFromExpertise(float value) {
        return value * RATING_PER_EXPERTISE;
    }

    public static float getDodgeParryReducFromExpertise(float rating, CharacterClass characterClass) {
        return getDodgeParryReducFromExpertise(rating);
    }

    public static float getDodgeParryReducFromExpertise(float rating) {
        return rating * RATING_PER_DODGEPARRYREDUC;
    }

    public static float getExpertiseFromDodgeParryReduc(float value, CharacterClass characterClass) {
        return getExpertiseFromDodgeParryReduc(value);
    }

    public static float getExpertiseFromDodgeParryReduc(float value) {
        return value / RATING_PER_DODGEPARRYREDUC;
    }

    public static float getParryFromRating(float rating, CharacterClass characterClass) {
        return getParryFromRating(rating);
    }

    public static float getParryFromRating(float rating) {
        return rating / RATING_PER_PARRY;
    }

    public static float getCritFromRating(float rating, CharacterClass characterClass) {
        return getPhysicalCritFromRating(rating);
    }

    public static float getCritFromRating(float rating) {
        return getPhysicalCritFromRating(rating);
    }

    public static float getPhysicalCritFromRating(float rating, CharacterClass characterClass) {
        return getPhysicalCritFromRating(rating);
    }

    public static float getPhysicalCritFromRating(float rating) {
        return rating / RATING_PER_PHYSICALCRIT;
    }

    public static float getRatingFromCrit(float rating, CharacterClass characterClass) {
        return getRatingFromPhysicalCrit(rating);
    }

    public static float getRatingFromCrit(float rating) {
        return getRatingFromPhysicalCrit(rating);
    }

    public static float getRatingFromPhysicalCrit(float rating, CharacterClass characterClass) {
        return getRatingFromPhysicalCrit(rating);
    }

    public static float getRatingFromPhysicalCrit(float rating) {
        return rating * RATING_PER_PHYSICALCRIT;
    }

    public static float getHasteFromRating(float rating, CharacterClass characterClass) {
        return getPhysicalHasteFromRating(rating, characterClass);
    }

    public static float getPhysicalHasteFromRating(float rating, CharacterClass characterClass) {
        if (characterClass == CharacterClass.DeathKnight
                || characterClass == CharacterClass.Druid
                || characterClass == CharacterClass.Paladin
                || characterClass == CharacterClass.Shaman)
            return rating / RATING_PER_PHYSICALHASTE * 1.3f;
        return rating / RATING_PER_PHYSICALHASTE;
    }

    public static float getHitFromRating(float rating, CharacterClass characterClass) {
        return getPhysicalHitFromRating(rating);
    }

    public static float getHitFromRating(float rating) {
        return getPhysicalHitFromRating(rating);
    }

    public static float getPhysicalHitFromRating(float rating, CharacterClass characterClass) {
        return getPhysicalHitFromRating(rating);
    }

    public static float getPhysicalHitFromRating(float rating) {
        return rating / RATING_PER_PHYSICALHIT;
    }

    public static float getRatingFromHit(float value, CharacterClass characterClass) {
        return getRatingFromHit(value);
    }

    public static float getRatingFromHit(float value) {
        return value * RATING_PER_PHYSICALHIT;
    }

    public static float getCritReductionFromResilience(float rating, CharacterClass characterClass) {
        return getCritReductionFromResilience(rating);
    }

    public static float getCritReductionFromResilience(float rating) {
        return rating / RATING_PER_RESILIENCE;
    }

    public static float getSpellCritFromRating(float rating, CharacterClass characterClass) {
        return getSpellCritFromRating(rating);
    }

    public static float getSpellCritFromRating(float rating) {
        return rating / RATING_PER_SPELLCRIT;
    }

    public static float getSpellHasteFromRating(float rating, CharacterClass characterClass) {
        return getSpellHasteFromRating(rating);
    }

    public static float getSpellHasteFromRating(float rating) {
        return rating / RATING_PER_SPELLHASTE;
    }

    public static float getSpellHitFromRating(float rating, CharacterClass characterClass) {
        return getSpellHitFromRating(rating);
    }

    public static float getSpellHitFromRating(float rating) {
        return rating / RATING_PER_SPELLHIT;
    }

    public static float getSpellCritFromIntellect(float intellect, CharacterClass characterClass) {
        return getSpellCritFromIntellect(intellect);
    }

    public static float getSpellCritFromIntellect(float intellect) {
        return intellect / INT_PER_SPELLCRIT * 0.01f;
    }

    public static float getCritFromAgility(float agility, CharacterClass characterClass) {
        return getPhysicalCritFromAgility(agility, characterClass);
    }

    public static float getPhysicalCritFromAgility(float agility, CharacterClass characterClass) {
        return agility / AGI_PER_PHYSICALCRIT[characterClass.value()] * 0.01f;
    }

    public static float getDodgeFromAgility(float agility, CharacterClass characterClass) {
        return agility / AGI_PER_DODGE[characterClass.value()] * 0.01f;
    }

    public static float getSpiritRegenSec(float spirit, float intellect, CharacterClass characterClass) {
        return getSpiritRegenSec(spirit, intellect);
    }

    public static float getSpiritRegenSec(float spirit, float intellect) {
        return 0.001f + spirit * REGEN_CONSTANT * (float) Math.sqrt(intellect);
    }

    public static float applyMultiplier(float baseValue, float multiplier) {
        return (baseValue * (1f + multiplier));
    }

    public static float getArmorDamageReduction(int attackerLevel, float targetArmor,
            float armorIgnoreDebuffs, float armorIgnoreBuffs, float armorPenetrationRating) {
        return getArmorDamageReduction(attackerLevel, 83, targetArmor, armorIgnoreDebuffs, armorIgnoreBuffs, armorPenetrationRating);
    }

    public static float getArmorDamageReduction(int attackerLevel, int targetLevel, float targetArmor,
            float armorIgnoreDebuffs, float armorIgnoreBuffs, float armorPenetrationRating) {
        float armorConstant = 400 + 85 * targetLevel + 4.5f * 85 * (targetLevel - 59);
        targetArmor *= (1f - armorIgnoreDebuffs);
        float arpCap = Math.min((targetArmor + armorConstant) / 3f, targetArmor);
        targetArmor -= arpCap * Math.min(1f, getArmorPenetrationFromRating(armorPenetrationRating) + armorIgnoreBuffs);

        return 1f - armorConstant / (armorConstant + targetArmor);
    }

    public static float getRatingFromArmorReduction(int attackerLevel, float targetArmor,
            float armorIgnoreDebuffs, float armorIgnoreBuffs, float targetArmorReduction) {
        float armorConstant = 400 + 85 * attackerLevel + 4.5f * 85 * (attackerLevel - 59);
        targetArmor *= (1f - armorIgnoreDebuffs);
        float arpCap = Math.min((targetArmor + armorConstant) / 3f, targetArmor);

        float a = targetArmorReduction, b = armorConstant, c = targetArmor, d = arpCap, e = armorIgnoreBuffs;

        float arpFromRating = (c * (1 - a) + d * e * (a - 1) - a * b) / (d * (1 - a));
        return (float) roundHalfEven((double) (arpFromRating * RATING_PER_ARMORPENETRATION), 3);
    }

    public static float getSpellMiss(int levelDelta, boolean bPvP) {
        if (-levelDelta <= 2)
            return Math.min(0.0f, -(levelDelta + 4) * 0.01f);

        if (-levelDelta > 2)
            if (bPvP)
                return Math.min(0.62f, (-levelDelta * 7 - 8) * 0.01f);
        return Math.min(0.94f, (-levelDelta * 11 - 16) * 0.01f);
    }

    public static float[] getResistanceTable(int attackerLevel, int targetLevel,
            float targetResistance, float attackerSpellPenetration) {
        float[] resistTable = {0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};
        float averageResistance = getAverageResistance(attackerLevel, targetLevel, targetResistance, attackerSpellPenetration);

        for (int x = -1; x < 11; x++) {
            float resistSlice = Math.max(0f, 0.5f - 2.5f * (float) Math.abs(0.1f * x - averageResistance));
            if (x == -1) {
                resistTable[0] += 2f * resistSlice;
                resistTable[1] -= 1f * resistSlice;
            } else
                resistTable[x] += resistSlice;
        }
        return resistTable;
    }

    public static String getResistanceTableString(int attackerLevel, int targetLevel,
            float targetResistance, float attackerSpellPenetration) {
        String tipResist = "";
        tipResist = netRoundToString(getAverageResistance(attackerLevel, targetLevel, targetResistance, attackerSpellPenetration) * 100.0f, 2) + "% average resistance \n";
        tipResist += "% Resisted     Occurance";

        float[] resistTable = getResistanceTable(attackerLevel, targetLevel, targetResistance, attackerSpellPenetration);
        for (int count = 0; count < 10; count++) {
            if (resistTable[count] > 0) {
                tipResist += "\n" + netRoundToString(count * 10.0f, 1) + " % resisted   " + netRoundToString(resistTable[count] * 100.0f, 2) + "%";
            }
        }

        return tipResist;
    }

    public static float getMinimumResistance(int attackerLevel, int targetLevel,
            float targetResistance, float attackerSpellPenetration) {
        float[] resistTable = getResistanceTable(attackerLevel, targetLevel, targetResistance, attackerSpellPenetration);

        for (int x = 0; x < 11; x++)
            if (resistTable[x] > 0f)
                return 0.1f * x;
        return 0f;
    }

    public static float getDRAvoidanceChance(Character character, Stats stats, HitResult avoidanceType, int targetLevel) {
        return getDRAvoidanceChance(character, stats, avoidanceType, Integer.toUnsignedLong(targetLevel));
    }

    public static float getDRAvoidanceChance(Character character, Stats stats, HitResult avoidanceType, long targetLevel) {
        float defSkill = stats.getDefense();
        stats.setDefenseRating(Math.max(stats.getDefenseRating(), 0f));
        float defSkillMod = (getDefenseFromRating(stats.getDefenseRating(), character.getCharacterClass()) * DEFENSE_RATING_AVOIDANCE_MULTIPLIER);
        float baseAvoid = (defSkill - ((targetLevel * 5) & 0xFFFFFFFFL)) * DEFENSE_RATING_AVOIDANCE_MULTIPLIER;
        float modifiedAvoid = defSkillMod;
        float finalAvoid = 0f;
        int iClass = character.getCharacterClass().value();

        switch (avoidanceType) {
            case Dodge:
                baseAvoid += ((stats.getDodge() + getDodgeFromAgility(stats.getBaseAgility(), character.getCharacterClass())) * 100f);
                stats.setDodgeRating(Math.max(stats.getDodgeRating(), 0f));
                modifiedAvoid += ((getDodgeFromAgility((stats.getAgility() - stats.getBaseAgility()), character.getCharacterClass()) +
                                getDodgeFromRating(stats.getDodgeRating())) * 100f);
                modifiedAvoid = DRMath(CAP_DODGE_INV[iClass], DR_COEFFIENT[iClass], modifiedAvoid);
                finalAvoid = Math.max(baseAvoid + modifiedAvoid, 0);
                finalAvoid = Math.min(finalAvoid, CAP_DODGE[iClass]);
                break;
            case Parry:
                baseAvoid += stats.getParry() * 100f;
                stats.setParryRating(Math.max(stats.getParryRating(), 0f));
                modifiedAvoid += (getParryFromRating(stats.getParryRating()) * 100f);
                modifiedAvoid = DRMath(CAP_PARRY_INV[iClass], DR_COEFFIENT[iClass], modifiedAvoid);
                finalAvoid = (baseAvoid + modifiedAvoid);
                finalAvoid = Math.min(finalAvoid, CAP_PARRY[iClass]);
                break;
            case Miss:
                baseAvoid += stats.getMiss() * 100f;
                modifiedAvoid = DRMath((1f / CAP_MISSED[iClass]), DR_COEFFIENT[iClass], modifiedAvoid);
                modifiedAvoid = Math.min(CAP_MISSED[iClass], modifiedAvoid);
                finalAvoid = (baseAvoid + modifiedAvoid);
                finalAvoid = Math.min(finalAvoid, CAP_MISSED[iClass]);
                break;
            case Block:
                baseAvoid += stats.getBlock() * 100f;
                stats.setBlockRating(Math.max(stats.getBlockRating(), 0f));

                modifiedAvoid += (getBlockFromRating(stats.getBlockRating()) * 100f);
                finalAvoid = (baseAvoid + modifiedAvoid);
                break;
            case Crit:
                modifiedAvoid += (getCritReductionFromResilience(stats.getResilience()) * 100f);
                finalAvoid = (baseAvoid + modifiedAvoid);
                break;
        }

        finalAvoid = finalAvoid / 100.0f;
        return finalAvoid;
    }

    private static float DRMath(float invCap, float coefficient, float valuePreDR) {
        float drValue = 0f;
        drValue = 1f / (invCap + coefficient / valuePreDR);
        return drValue;
    }

    public static float getDefenseRatingNeeded(Character character, Stats stats, int targetLevel) {
        return getDefenseRatingNeeded(character, stats, Integer.toUnsignedLong(targetLevel));
    }

    public static float getDefenseRatingNeeded(Character character, Stats stats, long targetLevel) {
        float fAvoidance = getDRAvoidanceChance(character, stats, HitResult.Crit, targetLevel);
        float critChance = 5.0f - (fAvoidance * 100);
        return (critChance / DEFENSE_RATING_AVOIDANCE_MULTIPLIER) * RATING_PER_DEFENSE;
    }

    private static double roundHalfEven(double value, int digits) {
        if (!Double.isFinite(value))
            return value;
        return new BigDecimal(value).setScale(digits, RoundingMode.HALF_EVEN).doubleValue();
    }

    private static String netRoundToString(double value, int digits) {
        if (Double.isNaN(value))
            return "NaN";
        if (Double.isInfinite(value))
            return value > 0 ? "Infinity" : "-Infinity";
        return new BigDecimal(value).setScale(digits, RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString();
    }

    public static class CombatTable {
        public float miss;
        public float dodge;
        public float parry;
        public float glancing;
        public float block;
        public float crit;
        public float hit;

        public boolean bPlayerTarget;
        public boolean bBehind;

        public float evasion() {
            return miss + (bBehind ? (bPlayerTarget ? 0f : dodge) : (dodge + parry));
        }

        public CombatTable() {
            miss = dodge = parry = glancing = block = crit = hit = 0f;
            bPlayerTarget = bBehind = false;
        }
    }

    public static CombatTable getCombatTablePlayer_vs_NPC(int attackerLevel, int targetLevel,
            boolean bDualWield, boolean bWhiteorYellow, float percBehind, float hitBonus, float expertiseBonus, float critChance) {
        CombatTable ct = new CombatTable();

        int deltaLevel = targetLevel - attackerLevel;

        ct.miss = (!bDualWield || bWhiteorYellow)
                ? YELLOW_MISS_CHANCE_CAP[deltaLevel]
                : WHITE_MISS_CHANCE_CAP_DW[deltaLevel];

        ct.glancing = bWhiteorYellow ? YELLOW_GLANCE_CHANCE_CAP[deltaLevel] : WHITE_GLANCE_CHANCE_CAP[deltaLevel];
        ct.dodge    = bWhiteorYellow ? YELLOW_DODGE_CHANCE_CAP[deltaLevel] : WHITE_DODGE_CHANCE_CAP[deltaLevel];
        ct.parry    = bWhiteorYellow ? YELLOW_PARRY_CHANCE_CAP[deltaLevel] : WHITE_PARRY_CHANCE_CAP[deltaLevel];
        ct.block    = bWhiteorYellow ? YELLOW_BLOCK_CHANCE_CAP[deltaLevel] : WHITE_BLOCK_CHANCE_CAP[deltaLevel];
        ct.crit     = NPC_LEVEL_CRIT_MOD[deltaLevel];

        ct.miss     -= hitBonus;
        ct.dodge    -= expertiseBonus;
        ct.parry    -= expertiseBonus;
        ct.crit     += critChance;

        ct.parry    *= percBehind;
        ct.block    *= percBehind;

        return ct;
    }

    public static CombatTable getCombatTablePlayerVsNpc(int attackerLevel, int targetLevel,
            boolean bDualWield, boolean bWhiteorYellow, float percBehind, float hitBonus, float expertiseBonus, float critChance) {
        return getCombatTablePlayer_vs_NPC(attackerLevel, targetLevel, bDualWield, bWhiteorYellow, percBehind, hitBonus, expertiseBonus, critChance);
    }
}
