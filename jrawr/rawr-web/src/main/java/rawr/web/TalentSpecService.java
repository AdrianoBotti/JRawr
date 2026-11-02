package rawr.web;

import org.springframework.stereotype.Service;
import rawr.base.character.Character;
import rawr.base.character.CharacterClass;
import rawr.base.character.SavedTalentSpec;
import rawr.base.character.SavedTalentSpecList;
import rawr.base.talents.GlyphDataAttribute;
import rawr.base.talents.TalentDataAttribute;
import rawr.base.talents.TalentsBase;
import rawr.data.SavedTalentSpecStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Browser and editor for saved talent specs (BACKLOG B-003). Reads the process-wide
 * {@link SavedTalentSpec} registry (shipped + user specs, plus the character's current spec),
 * and mutates the user half through {@link SavedTalentSpecStore}.
 *
 * <p>A spec's identity for apply/toggle is its {@code Class|specString} key; the spec string itself
 * carries the talent ranks and the glyph flags (the {@code '.'} half of
 * {@link TalentsBase#toString()}). Applying a spec always replaces the talent ranks; the glyph half
 * is applied only when the caller asks for it.</p>
 */
@Service
public class TalentSpecService {

    private final SavedTalentSpecStore store;
    private final TalentPanelService talentPanels;

    public TalentSpecService(SavedTalentSpecStore store, TalentPanelService talentPanels) {
        this.store = store;
        this.talentPanels = talentPanels;
    }

    /** One candidate spec for the talent browser. */
    public record TalentSpecView(
            String key,
            String name,
            String source,
            int tree1,
            int tree2,
            int tree3,
            String glyphSummary,
            boolean enabled,
            boolean current,
            int customIndex) {
        /** {@code 57/3/11} style point split. */
        public String split() {
            return tree1 + "/" + tree2 + "/" + tree3;
        }
    }

    /** Lists the current spec first, then the shipped and custom specs for the character's class. */
    public List<TalentSpecView> listSpecs(Character character) {
        CharacterClass characterClass = character.getCharacterClass();
        TalentsBase current = character.getCurrentTalents();
        int[] currentTotals = treeTotals(current);
        List<TalentSpecView> views = new ArrayList<TalentSpecView>();
        views.add(new TalentSpecView(
                SavedTalentSpecStore.key(characterClass, current.toString()),
                "Current",
                "current",
                currentTotals[0], currentTotals[1], currentTotals[2],
                glyphSummary(current),
                true,
                true,
                -1));

        SavedTalentSpecList all = SavedTalentSpec.getAllSpecs();
        int shippedCount = store.shippedCount();
        if (all != null) {
            int index = 0;
            for (SavedTalentSpec spec : all) {
                if (spec.characterClass != characterClass) {
                    index++;
                    continue;
                }
                boolean shipped = index < shippedCount;
                int customIndex = shipped ? -1 : index - shippedCount;
                views.add(new TalentSpecView(
                        SavedTalentSpecStore.key(spec.characterClass, spec.spec),
                        spec.name == null ? "" : spec.name,
                        shipped ? "shipped" : "custom",
                        spec.tree1, spec.tree2, spec.tree3,
                        glyphSummary(talentsFor(spec)),
                        store.isEnabled(spec),
                        isCurrent(spec, current),
                        customIndex));
                index++;
            }
        }
        return List.copyOf(views);
    }

    /** Saves the character's current talents (and glyphs, embedded in the spec string) as a spec. */
    public void saveCurrent(Character character, String name) {
        requireModel(character);
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Enter a name for the spec.");
        }
        TalentsBase current = character.getCurrentTalents();
        int[] totals = treeTotals(current);
        store.saveCurrent(new SavedTalentSpec(trimmed, current, totals[0], totals[1], totals[2]));
    }

    /**
     * Applies a spec to the character. The talent ranks are always replaced; the glyphs are
     * replaced only when {@code includeGlyphs} is set, otherwise the character's current glyphs
     * are kept.
     */
    public void apply(Character character, String key, boolean includeGlyphs) {
        requireModel(character);
        CharacterClass characterClass = character.getCharacterClass();
        String prefix = SavedTalentSpecStore.key(characterClass, "");
        if (key == null || !key.startsWith(prefix)) {
            throw new IllegalArgumentException("That spec belongs to a different class.");
        }
        TalentsBase applied = talentsFor(characterClass, key.substring(prefix.length()));
        if (applied == null) {
            throw new IllegalArgumentException("Invalid talent spec.");
        }
        if (!includeGlyphs) {
            boolean[] currentGlyphs = character.getCurrentTalents().getGlyphData();
            boolean[] appliedGlyphs = applied.getGlyphData();
            if (currentGlyphs != null && appliedGlyphs != null) {
                System.arraycopy(currentGlyphs, 0, appliedGlyphs, 0,
                        Math.min(currentGlyphs.length, appliedGlyphs.length));
            }
        }
        character.setCurrentTalents(applied);
    }

    /** Renames a user spec by its index in {@link SavedTalentSpecStore#userSpecs()}. */
    public void rename(int customIndex, String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Enter a name for the spec.");
        }
        store.rename(customIndex, trimmed);
    }

    /** Deletes a user spec by its index in {@link SavedTalentSpecStore#userSpecs()}. */
    public void delete(int customIndex) {
        store.delete(customIndex);
    }

    /** Includes or excludes a spec from the optimizer search. */
    public void setEnabled(String key, boolean enabled) {
        store.setEnabled(key, enabled);
    }

    /** The disabled {@code Class|specString} keys the optimizer must skip. */
    public java.util.Set<String> disabledKeys() {
        return store.disabledKeys();
    }

    // ------------------------------------------------------------------ helpers

    private static void requireModel(Character character) {
        if (character.getCurrentModel() == null || character.getCurrentModel().isBlank()) {
            throw new IllegalArgumentException("Choose a class/model first.");
        }
    }

    private static boolean isCurrent(SavedTalentSpec spec, TalentsBase current) {
        return spec.spec != null && spec.spec.equals(current.toString())
                && spec.characterClass == current.getCharacterClass();
    }

    private static int[] treeTotals(TalentsBase talents) {
        int[] totals = new int[3];
        int[] data = talents.getData();
        for (TalentDataAttribute meta : TalentPanelService.talentMetas(talents.getClass())) {
            if (meta.tree() >= 0 && meta.tree() < 3 && meta.index() < data.length) {
                totals[meta.tree()] += data[meta.index()];
            }
        }
        return totals;
    }

    private static String glyphSummary(TalentsBase talents) {
        if (talents == null) {
            return "";
        }
        boolean[] flags = talents.getGlyphData();
        if (flags == null) {
            return "";
        }
        List<String> names = new ArrayList<String>();
        for (GlyphDataAttribute glyph : TalentPanelService.glyphMetas(talents.getClass())) {
            if (glyph.index() < flags.length && flags[glyph.index()]) {
                names.add(glyph.name());
            }
        }
        return String.join(", ", names);
    }

    private static TalentsBase talentsFor(SavedTalentSpec spec) {
        return talentsFor(spec.characterClass, spec.spec);
    }

    private static TalentsBase talentsFor(CharacterClass characterClass, String spec) {
        SavedTalentSpec holder = new SavedTalentSpec();
        holder.characterClass = characterClass;
        holder.spec = spec;
        return holder.talentSpec();
    }
}
