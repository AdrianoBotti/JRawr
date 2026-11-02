// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Warlock/StatUtils.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.warlock;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

import java.util.List;

public final class StatUtils {

    private StatUtils() {
    }

    public static float getBuffEffect(
            List<Buff> activeBuffs,
            float candidateBuff,
            String group,
            StatExtractor extractor) {

        float active = getActiveBuff(activeBuffs, group, extractor);
        return Math.max(0f, candidateBuff - active);
    }

    public static float getActiveBuff(
            List<Buff> activeBuffs,
            String group,
            StatExtractor extractor) {

        float active = 0f;
        for (Buff buff : activeBuffs) {
            if (buff.getConflictingBuffs().contains(group)) {
                active += extractor.extract(buff.stats);
                for (Buff improvement : buff.improvements) {
                    if (activeBuffs.contains(improvement)) {
                        active += extractor.extract(improvement.stats);
                    }
                }
            }
        }
        return active;
    }

    public static float calcStamina(Stats stats) {
        return stats.getStamina() * (1f + stats.getBonusStaminaMultiplier());
    }

    public static float calcIntellect(Stats stats) {
        return stats.getIntellect() * (1f + stats.getBonusIntellectMultiplier());
    }

    public static float calcSpirit(Stats stats) {
        return stats.getSpirit() * (1f + stats.getBonusSpiritMultiplier());
    }

    public static float calcHealth(Stats stats) {
        return (stats.getHealth()
                + StatConversion.getHealthFromStamina(calcStamina(stats)))
                * (1f + stats.getBonusHealthMultiplier());
    }

    public static float calcMana(Stats stats) {
        return (1f + stats.getBonusManaMultiplier())
                * (stats.getMana()
                    + StatConversion.getManaFromIntellect(calcIntellect(stats)));
    }

    public static float calcUsableMana(Stats stats, float fightLen) {
        float mps = stats.getMp5() / 5f
                + stats.getMana() * stats.getManaRestoreFromMaxManaPerSecond();
        return calcMana(stats) + stats.getManaRestore() + mps * fightLen;
    }

    public static float calcSpellCrit(Stats stats) {
        return stats.getSpellCrit()
                + StatConversion.getSpellCritFromIntellect(calcIntellect(stats))
                + StatConversion.getSpellCritFromRating(stats.getCritRating())
                + stats.getBonusCritChance()
                + stats.getSpellCritOnTarget();
    }

    public static float calcSpellHit(Stats stats) {
        return stats.getSpellHit()
                + StatConversion.getSpellHitFromRating(stats.getHitRating());
    }

    public static float calcSpellPower(Stats stats) {
        return (stats.getSpellPower()
                    + stats.getSpellDamageFromSpiritPercentage() * calcSpirit(stats))
                * (1f + stats.getBonusSpellPowerMultiplier());
    }

    public static float calcSpellHaste(Stats stats) {
        return (1f + stats.getSpellHaste())
                * (1f + StatConversion.getSpellHasteFromRating(stats.getHasteRating()));
    }

    public static float calcStrength(Stats stats) {
        return stats.getStrength() * (1f + stats.getBonusStrengthMultiplier());
    }

    public static float calcAgility(Stats stats) {
        return stats.getAgility() * (1f + stats.getBonusAgilityMultiplier());
    }

    public static float calcAttackPower(Stats stats, float apPerStrength, float apPerAgility) {
        return (1f + stats.getBonusAttackPowerMultiplier())
                * (stats.getAttackPower()
                    + calcStrength(stats) * apPerStrength
                    + calcAgility(stats) * apPerAgility);
    }

    public static float calcPhysicalCrit(Stats stats, float critPerAgility, int levelDelta) {
        return stats.getPhysicalCrit()
                + calcAgility(stats) * critPerAgility
                + StatConversion.NPC_LEVEL_CRIT_MOD[levelDelta];
    }
}
