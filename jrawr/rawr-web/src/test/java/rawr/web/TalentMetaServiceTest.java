package rawr.web;

import org.junit.jupiter.api.Test;
import rawr.base.character.CharacterClass;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.talents.DruidTalents;
import rawr.base.talents.GlyphDataAttribute;
import rawr.base.talents.HunterTalents;
import rawr.base.talents.MageTalents;
import rawr.base.talents.PaladinTalents;
import rawr.base.talents.PriestTalents;
import rawr.base.talents.RogueTalents;
import rawr.base.talents.ShamanTalents;
import rawr.base.talents.TalentDataAttribute;
import rawr.base.talents.TalentsBase;
import rawr.base.talents.WarlockTalents;
import rawr.base.talents.WarriorTalents;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks the generated {@code talent-meta.json} to the active Java annotations: every modeled
 * talent must have an icon and per-rank text, and every modeled glyph a description. The artifact
 * is generated from the legacy C# attributes by {@code tools/talent-meta-gen/generate.py}.
 */
class TalentMetaServiceTest {

    private static final List<TalentsBase> CLASSES = List.of(
            new WarriorTalents(), new PaladinTalents(), new HunterTalents(), new RogueTalents(),
            new PriestTalents(), new DeathKnightTalents(), new ShamanTalents(), new MageTalents(),
            new WarlockTalents(), new DruidTalents());

    @Test
    void everyModeledTalentAndGlyphHasMetadata() {
        TalentMetaService meta = new TalentMetaService();
        for (TalentsBase talents : CLASSES) {
            CharacterClass characterClass = talents.getCharacterClass();
            List<TalentDataAttribute> talentMetas =
                    TalentPanelService.talentMetas(talents.getClass());
            assertThat(talentMetas).as("%s talents", characterClass).isNotEmpty();
            for (TalentDataAttribute talent : talentMetas) {
                TalentMetaService.TalentMeta talentMeta = meta.talent(characterClass, talent.index());
                assertThat(talentMeta).as("%s talent %d %s metadata",
                        characterClass, talent.index(), talent.name()).isNotNull();
                assertThat(talentMeta.icon()).as("%s talent %d icon",
                        characterClass, talent.index()).isNotBlank();
                assertThat(talentMeta.descriptions()).as("%s talent %d descriptions",
                        characterClass, talent.index()).isNotEmpty();
            }

            List<GlyphDataAttribute> glyphMetas =
                    TalentPanelService.glyphMetas(talents.getClass());
            assertThat(glyphMetas).as("%s glyphs", characterClass).isNotEmpty();
            for (GlyphDataAttribute glyph : glyphMetas) {
                TalentMetaService.GlyphMeta glyphMeta = meta.glyph(characterClass, glyph.index());
                assertThat(glyphMeta).as("%s glyph %d %s metadata",
                        characterClass, glyph.index(), glyph.name()).isNotNull();
                assertThat(glyphMeta.description()).as("%s glyph %d description",
                        characterClass, glyph.index()).isNotBlank();
            }
        }
    }
}
