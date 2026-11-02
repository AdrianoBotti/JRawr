package rawr.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * XML scalar quirks: the original XmlSerializer writes non-finite floats as INF/-INF/NaN, so the
 * loader must read them back (rules used by every cache loader).
 */
class XmlLoaderTest {

    @Test
    void parsesSpecialFloats() {
        assertThat(XmlLoader.parseFloat("INF")).isEqualTo(Float.POSITIVE_INFINITY);
        assertThat(XmlLoader.parseFloat("-INF")).isEqualTo(Float.NEGATIVE_INFINITY);
        assertThat(XmlLoader.parseFloat("NaN")).isNaN();
    }

    @Test
    void parsesPlainFloats() {
        assertThat(XmlLoader.parseFloat("1.5")).isEqualTo(1.5f);
        assertThat(XmlLoader.parseFloat("-2.25")).isEqualTo(-2.25f);
        assertThat(XmlLoader.parseFloat("0")).isEqualTo(0f);
    }
}
