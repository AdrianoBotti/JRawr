package rawr.data;

import org.junit.jupiter.api.Test;
import rawr.base.character.Character;
import rawr.base.items.GemmingTemplate;
import rawr.base.items.ItemFilter;
import rawr.base.items.ItemFilterEnabledOverride;
import rawr.base.items.ItemFilterRegex;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Character-file fidelity for the gemming-template and item-filter-enabled overrides. The expected
 * XML shape comes from a sample saved by the original engine
 * ({@code character-gemming-filter-overrides.xml}, produced offline with the prebuilt Rawr.Base.dll
 * plus Mono: a Character with one custom template, one gemming override and a two-level filter tree
 * with non-default flags).
 */
class CharacterGemmingFilterOverrideTest {
    private static final Path SAMPLE = resource("character-gemming-filter-overrides.xml");

    @Test
    void csharpSavedOverridesParseButDoNotRewriteGlobalFiltersAndRoundTripShape() throws Exception {
        ItemFilterRegex probe = new ItemFilterRegex();
        probe.setName("ProbeFilter");
        probe.setEnabled(true); // sample has false
        ItemFilterRegex sub = new ItemFilterRegex();
        sub.setName("ProbeSub");
        sub.setEnabled(false); // sample has true
        probe.regexList.add(sub);
        probe.otherRegexEnabled = true; // sample has false
        ItemFilter.getFilterList().add(probe);
        ItemFilter.setOtherEnabled(true); // sample has false
        try {
            Character character = CharacterFixtureLoader.load(SAMPLE);

            assertThat(character.getCustomGemmingTemplates()).hasSize(1);
            GemmingTemplate custom = character.getCustomGemmingTemplates().get(0);
            assertThat(custom.getId()).isEqualTo("tpl1");
            assertThat(custom.getModel()).isEqualTo("Mage");
            assertThat(custom.getEnabled()).isFalse();
            assertThat(custom.getGroup()).isEqualTo("Meta");
            assertThat(custom.getRedId()).isEqualTo(40113);
            assertThat(custom.getYellowId()).isEqualTo(40155);
            assertThat(custom.getBlueId()).isEqualTo(40133);
            assertThat(custom.getMetaId()).isEqualTo(41285);
            assertThat(custom.getPrismaticId()).isEqualTo(40155);

            assertThat(character.getGemmingTemplateOverrides()).hasSize(1);
            GemmingTemplate override = character.getGemmingTemplateOverrides().get(0);
            assertThat(override.getId()).isNull();
            assertThat(override.getModel()).isEqualTo("Mage");
            assertThat(override.getEnabled()).isTrue();
            assertThat(override.getGroup()).isEqualTo("Meta");
            assertThat(override.getMetaId()).isEqualTo(41285);

            // Parse shape: wrapper and items share the name, SubFilterOverride nests the same type.
            List<ItemFilterEnabledOverride> filters = character.getItemFilterEnabledOverride();
            assertThat(filters).hasSize(2);
            ItemFilterEnabledOverride probeOverride = filters.get(0);
            assertThat(probeOverride.getName()).isEqualTo("ProbeFilter");
            assertThat(probeOverride.isEnabled()).isFalse();
            assertThat(probeOverride.getSubFilterOverride()).hasSize(2);
            assertThat(probeOverride.getSubFilterOverride().get(0).getName()).isEqualTo("ProbeSub");
            assertThat(probeOverride.getSubFilterOverride().get(0).isEnabled()).isTrue();
            assertThat(probeOverride.getSubFilterOverride().get(1).getName()).isEqualTo("Other");
            assertThat(probeOverride.getSubFilterOverride().get(1).isEnabled()).isFalse();
            assertThat(filters.get(1).getName()).isEqualTo("Other");
            assertThat(filters.get(1).isEnabled()).isFalse();

            // JRawr keeps filters global: the legacy per-character snapshot is parsed and kept
            // for round-tripping, but opening the character must not rewrite the shared tree.
            assertThat(probe.isEnabled()).isTrue();
            assertThat(sub.isEnabled()).isFalse();
            assertThat(probe.otherRegexEnabled).isTrue();
            assertThat(ItemFilter.isOtherEnabled()).isTrue();

            // Round trip: the gemming fields survive; the saved filter block serializes the
            // current global state in the C# shape.
            Path saved = Files.createTempFile("jrawr-overrides", ".xml");
            try {
                probe.setEnabled(false);
                sub.setEnabled(true);
                probe.otherRegexEnabled = false;
                ItemFilter.setOtherEnabled(false);
                character.save(saved);
                Character reloaded = CharacterFixtureLoader.load(saved);
                assertThat(reloaded.getCustomGemmingTemplates()).hasSize(1);
                assertThat(reloaded.getGemmingTemplateOverrides()).hasSize(1);
                assertThat(reloaded.getGemmingTemplateOverrides().get(0).getModel()).isEqualTo("Mage");
                assertThat(reloaded.getItemFilterEnabledOverride()).hasSize(2);
                assertThat(reloaded.getItemFilterEnabledOverride().get(0).getSubFilterOverride()).hasSize(2);

                String sampleXml = Files.readString(SAMPLE, StandardCharsets.UTF_8);
                String savedXml = Files.readString(saved, StandardCharsets.UTF_8);
                assertThat(extractOverrideBlock(savedXml)).isEqualTo(extractOverrideBlock(sampleXml));
            } finally {
                Files.deleteIfExists(saved);
            }
        } finally {
            ItemFilter.getFilterList().clear();
            ItemFilter.setOtherEnabled(true);
        }
    }

    @Test
    void absentOverridesStayAtDefaults() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<Character><Name>No Overrides</Name><CurrentModel>Mage</CurrentModel>"
                + "<CustomGemmingTemplates /><GemmingTemplateOverrides /></Character>";
        Path file = Files.createTempFile("jrawr-no-overrides", ".xml");
        try {
            Files.writeString(file, xml, StandardCharsets.UTF_8);
            Character character = CharacterFixtureLoader.load(file);
            assertThat(character.getCustomGemmingTemplates()).isEmpty();
            assertThat(character.getGemmingTemplateOverrides()).isEmpty();
            assertThat(character.getItemFilterEnabledOverride()).isNull();
            assertThat(character.loadItemFilterEnabledOverride()).isFalse();
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /** The C# engine block: CustomGemmingTemplates, GemmingTemplateOverrides, ItemFilterEnabledOverride. */
    private static String extractOverrideBlock(String xml) {
        int start = xml.indexOf("<CustomGemmingTemplates");
        int end = xml.indexOf("</ItemFilterEnabledOverride>");
        assertThat(start).as("CustomGemmingTemplates in %s", xml).isGreaterThanOrEqualTo(0);
        assertThat(end).as("ItemFilterEnabledOverride in %s", xml).isGreaterThan(start);
        return xml.substring(start, end + "</ItemFilterEnabledOverride>".length());
    }

    private static Path resource(String name) {
        try {
            return Paths.get(CharacterGemmingFilterOverrideTest.class.getClassLoader().getResource(name).toURI());
        } catch (Exception e) {
            throw new IllegalStateException("Missing test resource " + name, e);
        }
    }
}
