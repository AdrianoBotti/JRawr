package rawr.models.mage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the .NET/Mono-compatible formatting helpers. Expected strings were produced by Mono 6.14
 * (`Single.ToString("F2")`, `Double.ToString("F2")`, `G7`, `G15`, invariant culture) and are the
 * reason the Mage display strings match the oracle (DECISIONS #23).
 */
class NetFormatTest {

    @Test
    void f2MatchesMonoForFloatBoundaries() {
        assertThat(NetFormat.f2(6103.884765625f)).isEqualTo("6103.89");
        assertThat(NetFormat.f2(2.675f)).isEqualTo("2.68");
        assertThat(NetFormat.f2(0.125f)).isEqualTo("0.13");
        assertThat(NetFormat.f2(164.725f)).isEqualTo("164.73");
        assertThat(NetFormat.f2(-1.005f)).isEqualTo("-1.01");
        assertThat(NetFormat.f2(1.005f)).isEqualTo("1.01");
        assertThat(NetFormat.f2(2.345f)).isEqualTo("2.35");
        assertThat(NetFormat.f2(8.835f)).isEqualTo("8.84");
        assertThat(NetFormat.f2(1234.565f)).isEqualTo("1234.57");
        assertThat(NetFormat.f2(0.005f)).isEqualTo("0.01");
        assertThat(NetFormat.f2(31.005f)).isEqualTo("31.01");
        assertThat(NetFormat.f2(215f)).isEqualTo("215.00");
    }

    @Test
    void f2HandlesSpecialValues() {
        assertThat(NetFormat.f2(Float.NaN)).isEqualTo("NaN");
        assertThat(NetFormat.f2(Float.POSITIVE_INFINITY)).isEqualTo("Infinity");
        assertThat(NetFormat.f2(Float.NEGATIVE_INFINITY)).isEqualTo("-Infinity");
    }

    @Test
    void d2MatchesMonoForDoubleBoundaries() {
        assertThat(NetFormat.d2(2.675)).isEqualTo("2.68");
        assertThat(NetFormat.d2(0.125)).isEqualTo("0.13");
        assertThat(NetFormat.d2(1.005)).isEqualTo("1.01");
        assertThat(NetFormat.d2(-1.005)).isEqualTo("-1.01");
        assertThat(NetFormat.d2(6103.884765625)).isEqualTo("6103.88");
        assertThat(NetFormat.d2(1234.565)).isEqualTo("1234.57");
        assertThat(NetFormat.d2(0.005)).isEqualTo("0.01");
        assertThat(NetFormat.d2(8.835)).isEqualTo("8.84");
        assertThat(NetFormat.d2(Double.NaN)).isEqualTo("NaN");
        assertThat(NetFormat.d2(Double.POSITIVE_INFINITY)).isEqualTo("Infinity");
    }

    @Test
    void generalFormattingMatchesMono() {
        assertThat(NetFormat.netFloatToString(6103.884765625f)).isEqualTo("6103.885");
        assertThat(NetFormat.netFloatToString(2.675f)).isEqualTo("2.675");
        assertThat(NetFormat.netFloatToString(31.005f)).isEqualTo("31.005");
        assertThat(NetFormat.netFloatToString(0.005f)).isEqualTo("0.005");
        assertThat(NetFormat.netFloatToString(0f)).isEqualTo("0");
        assertThat(NetFormat.netFloatToString(Float.NaN)).isEqualTo("NaN");

        assertThat(NetFormat.netDoubleToString(0.00001)).isEqualTo("1E-05");
        assertThat(NetFormat.netDoubleToString(6103.884765625)).isEqualTo("6103.884765625");
        assertThat(NetFormat.netDoubleToString(0.0)).isEqualTo("0");
        assertThat(NetFormat.netDoubleToString(Double.POSITIVE_INFINITY)).isEqualTo("Infinity");
    }
}
