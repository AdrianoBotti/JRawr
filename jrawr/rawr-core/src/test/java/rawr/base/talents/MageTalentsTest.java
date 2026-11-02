package rawr.base.talents;

import rawr.base.talents.MageTalents;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Talent-string parsing. The input is the `frost_hitcap_movement` fixture's string; the expected
 * values were produced by the original `Rawr.Base.dll` through a Mono probe (C# `MageTalents`).
 */
class MageTalentsTest {
    private static final String FIXTURE_TALENTS =
            "23002523010000000000000000000000000000000000000000000000000523030311033100230152231351.0010000100000000001";

    @Test
    void parsesKnownTalentsFromFixtureString() {
        MageTalents talents = new MageTalents(FIXTURE_TALENTS);
        assertThat(talents.getIncantersAbsorption()).isEqualTo(0);
        assertThat(talents.getFingersOfFrost()).isEqualTo(2);
        assertThat(talents.getImprovedFrostbolt()).isEqualTo(5);
        assertThat(talents.getIceShards()).isEqualTo(3);
    }

    @Test
    void toStringRoundTrips() {
        MageTalents talents = new MageTalents(FIXTURE_TALENTS);
        assertThat(talents.toString()).isEqualTo(FIXTURE_TALENTS);
    }

    @Test
    void emptyTalentsAreAllZero() {
        MageTalents talents = new MageTalents();
        int[] data = talents.getData();
        for (int value : data) {
            assertThat(value).isZero();
        }
    }
}
