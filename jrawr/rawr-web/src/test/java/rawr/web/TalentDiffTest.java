package rawr.web;

import org.junit.jupiter.api.Test;
import rawr.base.talents.GlyphDataAttribute;
import rawr.base.talents.MageTalents;
import rawr.base.talents.TalentDataAttribute;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TalentDiffTest {
    @Test
    void reportsTalentPointAndGlyphChangesByName() {
        MageTalents before = new MageTalents();
        MageTalents after = new MageTalents();
        List<TalentDataAttribute> talents = TalentPanelService.talentMetas(MageTalents.class);
        TalentDataAttribute talent = talents.stream()
                .filter(meta -> meta.maxPoints() > 0)
                .findFirst()
                .orElseThrow();
        after.getData()[talent.index()] = 1;

        List<GlyphDataAttribute> glyphs = TalentPanelService.glyphMetas(MageTalents.class);
        GlyphDataAttribute glyph = glyphs.get(0);
        after.getGlyphData()[glyph.index()] = true;

        TalentPanelService service = new TalentPanelService(new TalentMetaService());
        TalentPanelService.TalentDiff diff = service.compareTalents(before, after);
        assertThat(diff.talents()).hasSize(1);
        TalentPanelService.TalentChange change = diff.talents().get(0);
        assertThat(change.name()).isEqualTo(talent.name());
        assertThat(change.before()).isZero();
        assertThat(change.after()).isEqualTo(1);
        assertThat(change.maxRank()).isEqualTo(talent.maxPoints());
        assertThat(change.icon()).as("talent icon from talent-meta.json").isNotBlank();
        assertThat(change.description()).as("talent effect text").isNotBlank();
        assertThat(change.descriptionRank()).isEqualTo(1);

        assertThat(diff.glyphs()).hasSize(1);
        TalentPanelService.GlyphChange glyphChange = diff.glyphs().get(0);
        assertThat(glyphChange.name()).isEqualTo(glyph.name());
        assertThat(glyphChange.before()).isFalse();
        assertThat(glyphChange.after()).isTrue();
        assertThat(glyphChange.description()).as("glyph effect text").isNotBlank();
    }
}
