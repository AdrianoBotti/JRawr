package rawr.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Preset/validation tests for the Item editor's selectable item source. Offline. */
class ItemSourceTemplatesTest {

    @Test
    void exposesTheKnownPresetsAndAStableDefault() {
        assertThat(ItemSourceTemplates.PRESETS).extracting(ItemSourceTemplates.Preset::key)
                .containsExactly("wowhead", "wotlkdb", "evowow");
        assertThat(ItemSourceTemplates.PRESETS).extracting(ItemSourceTemplates.Preset::label)
                .containsExactly("Wowhead (WotLK)", "wotlkdb (AoWoW)", "EvoWow");
        assertThat(ItemSourceTemplates.DEFAULT_TEMPLATE)
                .isEqualTo("https://wotlkdb.com/?item={id}&xml");
        assertThat(ItemSourceTemplates.preset("WOWHEAD").template())
                .isEqualTo("https://www.wowhead.com/wotlk/?item={id}&xml");
        assertThat(ItemSourceTemplates.preset("missing")).isNull();
    }

    @Test
    void presetForMatchesExactTemplatesOnly() {
        assertThat(ItemSourceTemplates.presetFor("https://wotlkdb.com/?item={id}&xml").key())
                .isEqualTo("wotlkdb");
        assertThat(ItemSourceTemplates.presetFor("https://www.wowhead.com/wotlk/?item={id}&xml").key())
                .isEqualTo("wowhead");
        assertThat(ItemSourceTemplates.presetFor("  https://wotlk.evowow.com/?item={id}&xml  ").key())
                .isEqualTo("evowow");
        assertThat(ItemSourceTemplates.presetFor("https://db.example/item/{id}.xml")).isNull();
        assertThat(ItemSourceTemplates.presetFor(null)).isNull();
    }

    @Test
    void validationRequiresAbsoluteHttpUrlIdAndNoWhitespace() {
        assertThat(ItemSourceTemplates.isValidTemplate("https://x.example/?item={id}&xml")).isTrue();
        assertThat(ItemSourceTemplates.isValidTemplate("http://x.example/{id}")).isTrue();
        assertThat(ItemSourceTemplates.isValidTemplate("{base}/?item={id}&xml")).isFalse();
        assertThat(ItemSourceTemplates.isValidTemplate("ftp://x.example/{id}")).isFalse();
        assertThat(ItemSourceTemplates.isValidTemplate("https://x.example/?item=1&xml")).isFalse();
        assertThat(ItemSourceTemplates.isValidTemplate("https://x.example/{id} two")).isFalse();
        assertThat(ItemSourceTemplates.isValidTemplate("https:///item/{id}")).isFalse();
        assertThat(ItemSourceTemplates.isValidTemplate(null)).isFalse();
    }

    @Test
    void hostOfNormalizesAndRejectsInvalidTemplates() {
        assertThat(ItemSourceTemplates.hostOf("HTTPS://WotLKDB.com/?item={id}&xml"))
                .isEqualTo("wotlkdb.com");
        assertThat(ItemSourceTemplates.hostOf("http://localhost:8080/?item={id}"))
                .isEqualTo("localhost");
        assertThat(ItemSourceTemplates.hostOf("https:///item/{id}")).isNull();
        assertThat(ItemSourceTemplates.hostOf(null)).isNull();
    }
}
