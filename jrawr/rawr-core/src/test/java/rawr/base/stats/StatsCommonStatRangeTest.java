package rawr.base.stats;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the {@code Stats.PROPERTY_INFO.minRange} values against the C# {@code [CommonStat]}
 * attributes in {@code Rawr.Base/Stats.cs}. The finite-difference relative-stat probe treats a
 * negative range as "not a common stat"; a missing default ({@code 0f}) silently removes the stat
 * from the Relative Stats chart (the bug fixed in DECISIONS #108).
 */
class StatsCommonStatRangeTest {
    /** The 8 properties whose attribute has an explicit MinRange. */
    private static final List<String[]> EXPLICIT = List.of(
            new String[]{"Armor", "80"},
            new String[]{"BonusArmor", "80"},
            new String[]{"Agility", "10"},
            new String[]{"AttackPower", "20"},
            new String[]{"Strength", "20"},
            new String[]{"Intellect", "10"},
            new String[]{"Spirit", "10"},
            new String[]{"HasteRating", "10"});

    /** The 29 properties with a bare {@code [CommonStat]} attribute (default MinRange = 0). */
    private static final List<String> DEFAULT_ZERO = List.of(
            "Health", "Stamina", "ScopeDamage", "ArmorPenetrationRating", "SpellPower",
            "CritRating", "HitRating", "MasteryRating", "DodgeRating", "ParryRating",
            "BlockRating", "BlockValue", "DefenseRating", "Resilience", "ExpertiseRating",
            "Mp5", "Hp5", "SpellShadowDamageRating", "SpellFireDamageRating",
            "SpellFrostDamageRating", "SpellArcaneDamageRating", "SpellNatureDamageRating",
            "DamageAbsorbed", "RangedCritRating", "RangedHitRating", "RangedHasteRating",
            "RangedAttackPower", "BonusRangedAttackPowerMultiplier",
            "BonusPetAttackPowerMultiplier");

    @Test
    void commonStatRangesMatchTheOriginalAttributes() {
        for (String[] entry : EXPLICIT) {
            Stats.StatProperty property = Stats.PROPERTY_BY_NAME.get(entry[0]);
            assertThat(property).as(entry[0]).isNotNull();
            assertThat(property.minRange()).as(entry[0]).isEqualTo(Float.parseFloat(entry[1]));
        }
        for (String name : DEFAULT_ZERO) {
            Stats.StatProperty property = Stats.PROPERTY_BY_NAME.get(name);
            assertThat(property).as(name).isNotNull();
            assertThat(property.minRange()).as(name).isZero();
        }
    }

    @Test
    void nonCommonStatsStayUnconstrained() {
        for (String name : List.of("Mana", "SpellPenetration", "RangedHaste", "SpellCrit")) {
            assertThat(Stats.PROPERTY_BY_NAME.get(name).minRange()).as(name).isEqualTo(-1f);
        }
    }
}
