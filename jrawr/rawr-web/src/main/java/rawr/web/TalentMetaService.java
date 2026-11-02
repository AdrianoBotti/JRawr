package rawr.web;

import org.springframework.stereotype.Service;
import rawr.base.character.CharacterClass;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * UI-only talent/glyph metadata generated from the legacy C# attributes by
 * {@code tools/talent-meta-gen/generate.py}: the icon name and the per-rank effect descriptions
 * that the Java model annotations deliberately omit. The committed {@code /talent-meta.json}
 * artifact is optional at runtime: without it the talent view still renders names, ranks and
 * layout, just without icons and descriptions.
 */
@Service
public class TalentMetaService {

    static final String RESOURCE = "/talent-meta.json";

    /** One talent: icon name (no extension) plus the effect text per rank. */
    public record TalentMeta(String icon, List<String> descriptions) {
    }

    /** One glyph: the effect text (glyphs have no per-rank data). */
    public record GlyphMeta(String description) {
    }

    /** JSON shape of one class section. Public fields keep Jackson binding independent of records. */
    static final class ClassMeta {
        public Map<String, TalentMeta> talents;
        public Map<String, GlyphMeta> glyphs;
    }

    private final Map<String, ClassMeta> classes;

    public TalentMetaService() {
        this(load());
    }

    TalentMetaService(Map<String, ClassMeta> classes) {
        this.classes = classes;
    }

    /** Metadata for one talent slot, or null when the artifact has no entry. */
    public TalentMeta talent(CharacterClass characterClass, int index) {
        ClassMeta meta = classes.get(characterClass.name());
        return meta == null || meta.talents == null ? null : meta.talents.get(Integer.toString(index));
    }

    /** Metadata for one glyph slot, or null when the artifact has no entry. */
    public GlyphMeta glyph(CharacterClass characterClass, int index) {
        ClassMeta meta = classes.get(characterClass.name());
        return meta == null || meta.glyphs == null ? null : meta.glyphs.get(Integer.toString(index));
    }

    static Map<String, ClassMeta> load() {
        try (InputStream in = TalentMetaService.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return Map.of();
            }
            Map<String, ClassMeta> loaded = JsonMapper.builder().build()
                    .readValue(in, new TypeReference<Map<String, ClassMeta>>() {
                    });
            for (ClassMeta meta : loaded.values()) {
                if (meta.talents != null) {
                    for (Map.Entry<String, TalentMeta> entry : meta.talents.entrySet()) {
                        TalentMeta talent = entry.getValue();
                        if (talent.descriptions() == null) {
                            entry.setValue(new TalentMeta(talent.icon(), List.of()));
                        }
                    }
                }
            }
            return Map.copyOf(loaded);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load " + RESOURCE, e);
        }
    }
}
