package rawr.web;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItemIdParserTest {

    @Test
    void parsesBareIdsSeparatedByWhitespaceCommasAndSemicolons() {
        ItemIdParser.Parsed parsed = ItemIdParser.parse("50731, 51281\n51310; 50000");

        assertThat(parsed.ids()).containsExactly(50731, 51281, 51310, 50000);
        assertThat(parsed.invalidTokens()).isEmpty();
        assertThat(parsed.truncated()).isFalse();
    }

    @Test
    void parsesClassicItemLinksIncludingNamesWithSpacesAndColourPrefixes() {
        String input = "|Hitem:51281:0:0:0:0:0:0:0:0|h[Bloodmage Hood]|h\n"
                + "|cffa335ee|Hitem:50731:0:0:0:0:0:0:0:0|h[Archus, Greatstaff of Antonidas]|h|r";

        ItemIdParser.Parsed parsed = ItemIdParser.parse(input);

        assertThat(parsed.ids()).containsExactly(51281, 50731);
        assertThat(parsed.invalidTokens()).isEmpty();
    }

    @Test
    void parsesWowheadStyleUrls() {
        String input = "https://www.wowhead.com/wotlk/item=51310\n"
                + "https://wotlkdb.com/?item=50731&xml\n"
                + "https://example.com/item/12345/bloodmage\n"
                + "item:999";

        ItemIdParser.Parsed parsed = ItemIdParser.parse(input);

        assertThat(parsed.ids()).containsExactly(51310, 50731, 12345, 999);
        assertThat(parsed.invalidTokens()).isEmpty();
    }

    @Test
    void deduplicatesInInputOrder() {
        ItemIdParser.Parsed parsed = ItemIdParser.parse("51281 50731 51281 51310 50731");

        assertThat(parsed.ids()).containsExactly(51281, 50731, 51310);
    }

    @Test
    void reportsTokensThatAreNotIds() {
        ItemIdParser.Parsed parsed = ItemIdParser.parse("51281 not-an-id [nope] 50731");

        assertThat(parsed.ids()).containsExactly(51281, 50731);
        assertThat(parsed.invalidTokens()).containsExactly("not-an-id", "nope");
    }

    @Test
    void trimsCommonPunctuationAroundBareIds() {
        ItemIdParser.Parsed parsed = ItemIdParser.parse("#51281, (50731), 51310.");

        assertThat(parsed.ids()).containsExactly(51281, 50731, 51310);
        assertThat(parsed.invalidTokens()).isEmpty();
    }

    @Test
    void rejectsZeroAndNegativeTokens() {
        ItemIdParser.Parsed parsed = ItemIdParser.parse("0 -5 51281");

        assertThat(parsed.ids()).containsExactly(51281);
        assertThat(parsed.invalidTokens()).containsExactly("0", "-5");
    }

    @Test
    void capsTheBatchAndReportsTruncation() {
        List<String> values = new ArrayList<String>();
        for (int i = 1; i <= ItemIdParser.MAX_IDS + 5; i++) {
            values.add(Integer.toString(50000 + i));
        }

        ItemIdParser.Parsed parsed = ItemIdParser.parse(String.join(" ", values));

        assertThat(parsed.ids()).hasSize(ItemIdParser.MAX_IDS);
        assertThat(parsed.ids().get(0)).isEqualTo(50001);
        assertThat(parsed.ids().get(ItemIdParser.MAX_IDS - 1)).isEqualTo(50000 + ItemIdParser.MAX_IDS);
        assertThat(parsed.truncated()).isTrue();
    }

    @Test
    void emptyAndNullInputProduceNothing() {
        assertThat(ItemIdParser.parse(null).ids()).isEmpty();
        assertThat(ItemIdParser.parse("   ").ids()).isEmpty();
        assertThat(ItemIdParser.parse(null).invalidTokens()).isEmpty();
    }

    @Test
    void mixedInputKeepsRecognisedIdsAndReportsTheRest() {
        String input = "|Hitem:51281:0:0:0:0:0:0:0:0|h[Bloodmage Hood]|h\n"
                + "https://www.wowhead.com/wotlk/item=50731\n"
                + "garbage 51310";

        ItemIdParser.Parsed parsed = ItemIdParser.parse(input);

        assertThat(parsed.ids()).containsExactly(51281, 50731, 51310);
        assertThat(parsed.invalidTokens()).containsExactly("garbage");
    }
}
