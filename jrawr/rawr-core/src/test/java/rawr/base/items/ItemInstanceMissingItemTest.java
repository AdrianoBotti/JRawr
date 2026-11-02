package rawr.base.items;

import rawr.base.character.CharacterSlot;
import rawr.base.stats.Stats;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An item id missing from the cache (for example a server-custom item imported without fetching)
 * must not break calculation or UI paths that ask the instance for stats, sockets or slot fit.
 */
class ItemInstanceMissingItemTest {

    private static final String MISSING_ITEM = "1209270.0.0.0.0";

    @Test
    void unknownItemHasNoStatsAndNoSockets() {
        ItemInstance instance = new ItemInstance(MISSING_ITEM);

        assertThat(instance.getItem()).isNull();
        assertThat(instance.matchesSocketBonus()).isTrue();
        assertThat(instance.fitsInSlot(CharacterSlot.MainHand)).isFalse();
        assertThat(instance.getTotalStats()).isEqualTo(new Stats());

        Stats accumulated = new Stats();
        assertThat(instance.accumulateTotalStats(null, accumulated)).isEqualTo(new Stats());
        assertThat(accumulated).isEqualTo(new Stats());
    }
}
