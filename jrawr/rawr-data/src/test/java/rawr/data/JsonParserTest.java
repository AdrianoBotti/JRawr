package rawr.data;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the ported brace-less JSON payload parser. Offline. */
class JsonParserTest {

    @Test
    void parsesBraceLessWowheadBody() {
        Map<String, Object> json = JsonParser.parse(
                "\"classs\":3,\"displayid\":54467,\"id\":41380,\"level\":80,\"name\":\"Gem\",\"slot\":0");

        assertThat(json)
                .containsEntry("classs", 3)
                .containsEntry("displayid", 54467)
                .containsEntry("name", "Gem")
                .containsEntry("slot", 0);
    }

    @Test
    void integersStayIntegersAndDecimalsBecomeDoubles() {
        Map<String, Object> json = JsonParser.parse("\"level\":80,\"dps\":9.5,\"neg\":-3");

        assertThat(json.get("level")).isInstanceOf(Integer.class);
        assertThat(json.get("dps")).isEqualTo(9.5d);
        assertThat(json.get("neg")).isEqualTo(-3);
    }

    @Test
    void parsesArraysNestedObjectsAndEscapedStrings() {
        Map<String, Object> json = JsonParser.parse(
                "\"source\":[1,2,5],\"sourcemore\":[{\"n\":\"Boss's Loot\",\"t\":1}],\"name\":\"A \\\"quote\\\"\"");

        assertThat((Object[]) json.get("source")).containsExactly(1, 2, 5);
        Map<?, ?> sourcemore = (Map<?, ?>) ((Object[]) json.get("sourcemore"))[0];
        assertThat(sourcemore.get("n")).isEqualTo("Boss's Loot");
        assertThat(sourcemore.get("t")).isEqualTo(1);
        assertThat(json.get("name")).isEqualTo("A \"quote\"");
    }

    @Test
    void acceptsEmptyAndWhitespaceOnlyBodies() {
        assertThat(JsonParser.parse("")).isEmpty();
        assertThat(JsonParser.parse("   ")).isEmpty();
    }

    @Test
    void mergeLetsTheSecondObjectOverrideTheFirst() {
        Map<String, Object> merged = JsonParser.merge(
                JsonParser.parse("\"int\":10,\"sta\":5"),
                JsonParser.parse("\"int\":25"));

        assertThat(merged).containsEntry("int", 25).containsEntry("sta", 5);
    }

    @Test
    void unicodeEscapesThrowLikeTheOriginal() {
        // Faithful port of the legacy parser (SUSPECTED_BUGS #34): the original reads the escape
        // payload starting at the 'u', so every unicode escape fails instead of decoding.
        assertThatThrownBy(() -> JsonParser.parse("\"name\":\"\\u0041\""))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void malformedInputThrows() {
        assertThatThrownBy(() -> JsonParser.parse("\"a\":}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
