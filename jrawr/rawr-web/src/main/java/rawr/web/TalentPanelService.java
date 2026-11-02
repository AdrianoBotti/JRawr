package rawr.web;

import org.springframework.stereotype.Service;
import rawr.base.character.Character;
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds the WoW-style talent tree/glyph view model and applies talent/glyph edits with the
 * WotLK rules: 71 total points, 5 points per row of the same tree, prerequisites at max rank, and
 * at most three major and three minor glyphs.
 *
 * <p>The layout and rank data come from the live {@link TalentsBase} annotations/arrays; the
 * icons and effect prose come from {@link TalentMetaService}. Trees are rendered as the game's
 * 4-column, 11-row grid; glyphs are split into the three major and three minor sockets.</p>
 *
 * <p>A row is open only when the character has spent the required points in the rows above it;
 * points in the row itself do not count toward opening that row. Removing a point cascades: if the
 * new previous-row total closes a row or leaves a prerequisite below max rank, every learned talent
 * that is no longer legal is reset to zero, repeating until the build is valid again. This matches
 * the game's respec behavior and keeps the row visibly locked.</p>
 */
@Service
public class TalentPanelService {

    /** WotLK level-80 talent budget. */
    static final int MAX_TALENT_POINTS = 71;

    private static final int TREE_COLUMNS = 4;
    private static final int TREE_ROWS = 11;
    private static final int GLYPH_SLOTS = 3;

    private static final Map<Class<?>, List<TalentDataAttribute>> TALENT_METAS =
            new ConcurrentHashMap<Class<?>, List<TalentDataAttribute>>();
    private static final Map<Class<?>, List<GlyphDataAttribute>> GLYPH_METAS =
            new ConcurrentHashMap<Class<?>, List<GlyphDataAttribute>>();

    private final TalentMetaService meta;

    public TalentPanelService(TalentMetaService meta) {
        this.meta = meta;
    }

    public record TalentChange(String name, int before, int after, int maxRank,
            String icon, String description, int descriptionRank) {
    }

    public record GlyphChange(String name, boolean major, boolean before, boolean after,
            String description) {
    }

    public record TalentDiff(List<TalentChange> talents, List<GlyphChange> glyphs) {
        public boolean hasChanges() {
            return !talents.isEmpty() || !glyphs.isEmpty();
        }
    }

    /**
     * Compares two talent allocations of the same class and lists the point/glyph changes by their
     * annotation names, in slot order. Each change carries the UI icon/effect description from
     * {@link TalentMetaService} when available. Used by the optimizer result view; the full spec
     * strings are displayed next to this list.
     */
    public TalentDiff compareTalents(TalentsBase before, TalentsBase after) {
        if (before == null || after == null || before.getClass() != after.getClass()) {
            return new TalentDiff(List.of(), List.of());
        }
        List<TalentChange> talentChanges = new ArrayList<TalentChange>();
        int[] beforeData = before.getData();
        int[] afterData = after.getData();
        if (beforeData != null && afterData != null) {
            Map<Integer, TalentDataAttribute> byIndex = byIndex(talentMetas(before.getClass()));
            int length = Math.min(beforeData.length, afterData.length);
            for (int i = 0; i < length; i++) {
                if (beforeData[i] != afterData[i]) {
                    TalentDataAttribute annotation = byIndex.get(i);
                    String name = annotation == null ? "Talent " + i : annotation.name();
                    int maxRank = annotation == null
                            ? Math.max(beforeData[i], afterData[i]) : annotation.maxPoints();
                    TalentMetaService.TalentMeta meta = this.meta.talent(
                            before.getCharacterClass(), i);
                    int descriptionRank = Math.max(1,
                            afterData[i] > 0 ? afterData[i] : beforeData[i]);
                    talentChanges.add(new TalentChange(name, beforeData[i], afterData[i], maxRank,
                            meta == null ? null : meta.icon(),
                            description(meta, descriptionRank),
                            Math.min(descriptionRank, maxRank)));
                }
            }
        }
        List<GlyphChange> glyphChanges = new ArrayList<GlyphChange>();
        boolean[] beforeGlyphs = before.getGlyphData();
        boolean[] afterGlyphs = after.getGlyphData();
        if (beforeGlyphs != null && afterGlyphs != null) {
            Map<Integer, GlyphDataAttribute> byIndex = glyphByIndex(glyphMetas(before.getClass()));
            int length = Math.min(beforeGlyphs.length, afterGlyphs.length);
            for (int i = 0; i < length; i++) {
                if (beforeGlyphs[i] != afterGlyphs[i]) {
                    GlyphDataAttribute annotation = byIndex.get(i);
                    String name = annotation == null ? "Glyph " + i : annotation.name();
                    boolean major = annotation != null && annotation.major();
                    TalentMetaService.GlyphMeta meta = this.meta.glyph(
                            before.getCharacterClass(), i);
                    glyphChanges.add(new GlyphChange(name, major, beforeGlyphs[i], afterGlyphs[i],
                            meta == null || meta.description() == null ? "" : meta.description()));
                }
            }
        }
        return new TalentDiff(List.copyOf(talentChanges), List.copyOf(glyphChanges));
    }

    private static String description(TalentMetaService.TalentMeta meta, int rank) {
        if (meta == null || meta.descriptions() == null || meta.descriptions().isEmpty()) {
            return "";
        }
        int index = Math.max(0, Math.min(meta.descriptions().size() - 1, rank - 1));
        String text = meta.descriptions().get(index);
        return text == null ? "" : text;
    }

    private static Map<Integer, GlyphDataAttribute> glyphByIndex(List<GlyphDataAttribute> metas) {
        Map<Integer, GlyphDataAttribute> byIndex = new LinkedHashMap<Integer, GlyphDataAttribute>();
        for (GlyphDataAttribute glyph : metas) {
            byIndex.put(glyph.index(), glyph);
        }
        return byIndex;
    }

    // ------------------------------------------------------------------ view model

    public record TalentPanelView(
            List<TalentTreeView> trees,
            int totalPoints,
            int maxPoints,
            int majorSelected,
            int minorSelected,
            List<GlyphSlotView> majorSlots,
            List<GlyphSlotView> minorSlots,
            List<GlyphView> availableMajor,
            List<GlyphView> availableMinor) {
    }

    public record TalentTreeView(int index, String name, int points, List<TalentRowView> rows) {
        /** CSS class for the tree header colour, derived from the spec name ("Feral Combat" -> "spec-feral-combat"). */
        public String cssClass() {
            return "spec-" + name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        }
    }

    public record TalentRowView(List<TalentCellView> cells) {
    }

    public record TalentCellView(
            int index,
            String name,
            String icon,
            int rank,
            int maxRank,
            boolean canAdd,
            boolean canRemove,
            boolean treeOpen,
            int treeRequirement,
            boolean prerequisiteMet,
            String prerequisiteName,
            String currentDescription,
            String nextDescription,
            String cssClass) {
    }

    public record GlyphView(int index, String name, boolean selected, String description, boolean locked) {
    }

    public record GlyphSlotView(GlyphView glyph) {
    }

    /** One talent tree's name and spent points, for the compact setup summary. */
    public record TreeSetup(String name, int points) {
    }

    /** Compact read-only spec/glyph state shown in the optimizer's "current setup" strip. */
    public record TalentSetupView(
            List<TreeSetup> trees,
            int totalPoints,
            int maxPoints,
            int majorSelected,
            int minorSelected,
            List<String> majorGlyphs,
            List<String> minorGlyphs) {
    }

    public TalentSetupView setupSummary(Character character) {
        TalentsBase talents = character.getCurrentTalents();
        List<TalentDataAttribute> talentMetas = talentMetas(talents.getClass());
        String[] treeNames = treeNames(talents);
        int[] treePoints = treePoints(talentMetas, talents.getData(), treeNames.length);
        int total = 0;
        List<TreeSetup> trees = new ArrayList<TreeSetup>(treeNames.length);
        for (int tree = 0; tree < treeNames.length; tree++) {
            total += treePoints[tree];
            trees.add(new TreeSetup(treeNames[tree], treePoints[tree]));
        }
        boolean[] glyphFlags = talents.getGlyphData();
        int major = 0;
        int minor = 0;
        List<String> majorGlyphs = new ArrayList<String>();
        List<String> minorGlyphs = new ArrayList<String>();
        for (GlyphDataAttribute glyph : glyphMetas(talents.getClass())) {
            if (glyphFlags == null || glyph.index() >= glyphFlags.length || !glyphFlags[glyph.index()]) {
                continue;
            }
            if (glyph.major()) {
                major++;
                majorGlyphs.add(glyph.name());
            } else {
                minor++;
                minorGlyphs.add(glyph.name());
            }
        }
        return new TalentSetupView(List.copyOf(trees), total, MAX_TALENT_POINTS, major, minor,
                List.copyOf(majorGlyphs), List.copyOf(minorGlyphs));
    }

    /** Builds the complete talent/glyph panel for the character's current class. */
    public TalentPanelView build(Character character) {
        TalentsBase talents = character.getCurrentTalents();
        CharacterClass characterClass = talents.getCharacterClass();
        List<TalentDataAttribute> talentMetas = talentMetas(talents.getClass());
        List<GlyphDataAttribute> glyphMetas = glyphMetas(talents.getClass());
        int[] data = talents.getData();
        String[] treeNames = treeNames(talents);
        int[] treePoints = treePoints(talentMetas, data, treeNames.length);
        int totalPoints = 0;
        for (int points : treePoints) {
            totalPoints += points;
        }

        Map<String, TalentDataAttribute> byGrid = new LinkedHashMap<String, TalentDataAttribute>();
        Map<Integer, TalentDataAttribute> byIndex = new LinkedHashMap<Integer, TalentDataAttribute>();
        for (TalentDataAttribute talent : talentMetas) {
            byGrid.put(gridKey(talent.tree(), talent.row(), talent.column()), talent);
            byIndex.put(talent.index(), talent);
        }

        List<TalentTreeView> trees = new ArrayList<TalentTreeView>();
        for (int tree = 0; tree < treeNames.length; tree++) {
            List<TalentRowView> rows = new ArrayList<TalentRowView>();
            for (int row = 1; row <= TREE_ROWS; row++) {
                List<TalentCellView> cells = new ArrayList<TalentCellView>(TREE_COLUMNS);
                for (int column = 1; column <= TREE_COLUMNS; column++) {
                    TalentDataAttribute talent = byGrid.get(gridKey(tree, row, column));
                    cells.add(talent == null ? null
                            : cell(talent, characterClass, data, talentMetas, byIndex, totalPoints));
                }
                rows.add(new TalentRowView(Collections.unmodifiableList(cells)));
            }
            trees.add(new TalentTreeView(tree, treeNames[tree], treePoints[tree], List.copyOf(rows)));
        }

        boolean[] glyphFlags = talents.getGlyphData();
        int majorCount = 0;
        int minorCount = 0;
        for (GlyphDataAttribute glyph : glyphMetas) {
            if (glyphFlags != null && glyph.index() < glyphFlags.length && glyphFlags[glyph.index()]) {
                if (glyph.major()) {
                    majorCount++;
                } else {
                    minorCount++;
                }
            }
        }
        boolean majorFull = majorCount >= GLYPH_SLOTS;
        boolean minorFull = minorCount >= GLYPH_SLOTS;
        List<GlyphView> availableMajor = new ArrayList<GlyphView>();
        List<GlyphView> availableMinor = new ArrayList<GlyphView>();
        List<GlyphView> selectedMajor = new ArrayList<GlyphView>();
        List<GlyphView> selectedMinor = new ArrayList<GlyphView>();
        for (GlyphDataAttribute glyph : glyphMetas) {
            boolean selected = glyphFlags != null && glyph.index() < glyphFlags.length
                    && glyphFlags[glyph.index()];
            boolean locked = !selected && (glyph.major() ? majorFull : minorFull);
            TalentMetaService.GlyphMeta glyphMeta = meta.glyph(talents.getCharacterClass(), glyph.index());
            GlyphView view = new GlyphView(glyph.index(), glyph.name(), selected,
                    glyphMeta == null ? null : glyphMeta.description(), locked);
            if (glyph.major()) {
                availableMajor.add(view);
                if (selected) {
                    selectedMajor.add(view);
                }
            } else {
                availableMinor.add(view);
                if (selected) {
                    selectedMinor.add(view);
                }
            }
        }

        return new TalentPanelView(
                List.copyOf(trees),
                totalPoints,
                MAX_TALENT_POINTS,
                selectedMajor.size(),
                selectedMinor.size(),
                glyphSlots(selectedMajor),
                glyphSlots(selectedMinor),
                List.copyOf(availableMajor),
                List.copyOf(availableMinor));
    }

    private TalentCellView cell(
            TalentDataAttribute talent,
            CharacterClass characterClass,
            int[] data,
            List<TalentDataAttribute> metas,
            Map<Integer, TalentDataAttribute> byIndex,
            int totalPoints) {
        int index = talent.index();
        int rank = data[index];
        int maxRank = talent.maxPoints();
        TalentDataAttribute prerequisite = talent.prerequisite() >= 0
                ? byIndex.get(talent.prerequisite()) : null;
        boolean prerequisiteMet = prerequisite == null
                || data[prerequisite.index()] >= prerequisite.maxPoints();
        int requiredTreePoints = 5 * (talent.row() - 1);
        boolean treeOpen = previousRowPoints(metas, data, talent.tree(), talent.row()) >= requiredTreePoints;
        boolean canAdd = totalPoints < MAX_TALENT_POINTS
                && rank < maxRank
                && treeOpen
                && prerequisiteMet;
        boolean canRemove = rank > 0;
        boolean maxed = rank >= maxRank;

        TalentMetaService.TalentMeta talentMeta = meta.talent(characterClass, index);
        List<String> descriptions = talentMeta == null ? List.of() : talentMeta.descriptions();
        String currentDescription = null;
        String nextDescription = null;
        if (!descriptions.isEmpty()) {
            if (rank > 0) {
                currentDescription = descriptions.get(Math.min(rank - 1, descriptions.size() - 1));
            }
            if (rank < maxRank) {
                nextDescription = descriptions.get(Math.min(rank, descriptions.size() - 1));
            }
        }

        String cssClass;
        if (maxed) {
            cssClass = "is-maxed";
        } else if (rank > 0) {
            cssClass = "is-learned";
        } else if (canAdd) {
            cssClass = "is-available";
        } else {
            cssClass = "is-locked";
        }
        return new TalentCellView(
                index,
                talent.name(),
                talentMeta == null ? null : talentMeta.icon(),
                rank,
                maxRank,
                canAdd,
                canRemove,
                treeOpen,
                requiredTreePoints,
                prerequisiteMet,
                prerequisite == null ? null : prerequisite.name(),
                currentDescription,
                nextDescription,
                cssClass + (treeOpen ? "" : " is-row-locked"));
    }

    private static List<GlyphSlotView> glyphSlots(List<GlyphView> selected) {
        int slots = Math.max(GLYPH_SLOTS, selected.size());
        List<GlyphSlotView> result = new ArrayList<GlyphSlotView>(slots);
        for (int i = 0; i < slots; i++) {
            result.add(new GlyphSlotView(i < selected.size() ? selected.get(i) : null));
        }
        return List.copyOf(result);
    }

    // ------------------------------------------------------------------ edits

    /** Adds or removes one point; {@code delta} is +1 or -1. */
    public void adjustTalent(Character character, int index, int delta) {
        TalentsBase talents = character.getCurrentTalents();
        List<TalentDataAttribute> metas = talentMetas(talents.getClass());
        Map<Integer, TalentDataAttribute> byIndex = byIndex(metas);
        TalentDataAttribute talent = byIndex.get(index);
        if (talent == null) {
            throw new IllegalArgumentException("Unknown talent.");
        }
        if (delta > 0) {
            addTalent(talents, talent, metas, byIndex);
        } else if (delta < 0) {
            removeTalent(talents, talent, metas);
        }
    }

    /** Toggles one modeled glyph, enforcing three major and three minor sockets. */
    public void toggleGlyph(Character character, int index) {
        TalentsBase talents = character.getCurrentTalents();
        GlyphDataAttribute glyph = null;
        for (GlyphDataAttribute candidate : glyphMetas(talents.getClass())) {
            if (candidate.index() == index) {
                glyph = candidate;
                break;
            }
        }
        if (glyph == null) {
            throw new IllegalArgumentException("Unknown glyph.");
        }
        boolean[] flags = talents.getGlyphData();
        if (flags == null || index >= flags.length) {
            throw new IllegalArgumentException("This class has no glyph data.");
        }
        if (flags[index]) {
            flags[index] = false;
            return;
        }
        int selected = 0;
        for (GlyphDataAttribute candidate : glyphMetas(talents.getClass())) {
            if (candidate.major() == glyph.major() && candidate.index() < flags.length
                    && flags[candidate.index()]) {
                selected++;
            }
        }
        if (selected >= GLYPH_SLOTS) {
            throw new IllegalArgumentException("All " + (glyph.major() ? "major" : "minor")
                    + " glyph slots are full. Remove one first.");
        }
        flags[index] = true;
    }

    /** Clears every talent rank, leaving the glyph selection untouched. */
    public void resetTalents(Character character) {
        int[] data = character.getCurrentTalents().getData();
        for (int i = 0; i < data.length; i++) {
            data[i] = 0;
        }
    }

    /** Clears every glyph flag, leaving the talent ranks untouched. */
    public void resetGlyphs(Character character) {
        boolean[] glyphs = character.getCurrentTalents().getGlyphData();
        if (glyphs != null) {
            for (int i = 0; i < glyphs.length; i++) {
                glyphs[i] = false;
            }
        }
    }

    private static void addTalent(
            TalentsBase talents,
            TalentDataAttribute talent,
            List<TalentDataAttribute> metas,
            Map<Integer, TalentDataAttribute> byIndex) {
        int[] data = talents.getData();
        int index = talent.index();
        if (data[index] >= talent.maxPoints()) {
            throw new IllegalArgumentException("That talent is already at maximum rank.");
        }
        int total = totalPoints(data);
        if (total >= MAX_TALENT_POINTS) {
            throw new IllegalArgumentException("All " + MAX_TALENT_POINTS + " talent points are spent.");
        }
        String[] names = treeNames(talents);
        int required = 5 * (talent.row() - 1);
        if (previousRowPoints(metas, data, talent.tree(), talent.row()) < required) {
            String tree = talent.tree() < names.length ? names[talent.tree()] : "that tree";
            throw new IllegalArgumentException("Requires " + required + " points in " + tree + ".");
        }
        if (talent.prerequisite() >= 0) {
            TalentDataAttribute prerequisite = byIndex.get(talent.prerequisite());
            if (prerequisite != null && data[prerequisite.index()] < prerequisite.maxPoints()) {
                throw new IllegalArgumentException("Requires " + prerequisite.name()
                        + " at maximum rank.");
            }
        }
        data[index]++;
    }

    private static void removeTalent(
            TalentsBase talents,
            TalentDataAttribute talent,
            List<TalentDataAttribute> metas) {
        int[] data = talents.getData();
        int index = talent.index();
        if (data[index] <= 0) {
            throw new IllegalArgumentException("That talent has no points to remove.");
        }
        data[index]--;
        // Cascading reset: a point removal can close a row or drop a prerequisite below max.
        // Reset every learned talent that is no longer legal, repeating because each reset lowers
        // the tree total again and may invalidate another talent further down.
        while (true) {
            TalentDataAttribute invalid = invalidTalent(metas, data);
            if (invalid == null) {
                break;
            }
            data[invalid.index()] = 0;
        }
    }

    /** Points spent in the same tree in rows strictly above {@code row}. */
    private static int previousRowPoints(List<TalentDataAttribute> metas, int[] data, int tree, int row) {
        int points = 0;
        for (TalentDataAttribute talent : metas) {
            if (talent.tree() == tree && talent.row() < row
                    && talent.index() < data.length) {
                points += data[talent.index()];
            }
        }
        return points;
    }

    private static TalentDataAttribute invalidTalent(List<TalentDataAttribute> metas, int[] data) {
        Map<Integer, TalentDataAttribute> byIndex = byIndex(metas);
        for (TalentDataAttribute talent : metas) {
            int index = talent.index();
            if (index >= data.length || data[index] <= 0) {
                continue;
            }
            int required = 5 * (talent.row() - 1);
            if (previousRowPoints(metas, data, talent.tree(), talent.row()) < required) {
                return talent;
            }
            if (talent.prerequisite() >= 0) {
                TalentDataAttribute prerequisite = byIndex.get(talent.prerequisite());
                if (prerequisite != null && data[prerequisite.index()] < prerequisite.maxPoints()) {
                    return talent;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ metadata

    static List<TalentDataAttribute> talentMetas(Class<?> talentsClass) {
        return TALENT_METAS.computeIfAbsent(talentsClass, key -> {
            Map<Integer, TalentDataAttribute> byIndex = new LinkedHashMap<Integer, TalentDataAttribute>();
            for (Method method : key.getMethods()) {
                TalentDataAttribute talent = method.getAnnotation(TalentDataAttribute.class);
                if (talent != null) {
                    byIndex.putIfAbsent(talent.index(), talent);
                }
            }
            List<TalentDataAttribute> result = new ArrayList<TalentDataAttribute>(byIndex.values());
            result.sort(Comparator.comparingInt(TalentDataAttribute::index));
            return List.copyOf(result);
        });
    }

    static List<GlyphDataAttribute> glyphMetas(Class<?> talentsClass) {
        return GLYPH_METAS.computeIfAbsent(talentsClass, key -> {
            Map<Integer, GlyphDataAttribute> byIndex = new LinkedHashMap<Integer, GlyphDataAttribute>();
            for (Method method : key.getMethods()) {
                GlyphDataAttribute glyph = method.getAnnotation(GlyphDataAttribute.class);
                if (glyph != null) {
                    byIndex.putIfAbsent(glyph.index(), glyph);
                }
            }
            List<GlyphDataAttribute> result = new ArrayList<GlyphDataAttribute>(byIndex.values());
            result.sort(Comparator.comparingInt(GlyphDataAttribute::index));
            return List.copyOf(result);
        });
    }

    private static Map<Integer, TalentDataAttribute> byIndex(List<TalentDataAttribute> metas) {
        Map<Integer, TalentDataAttribute> byIndex = new LinkedHashMap<Integer, TalentDataAttribute>();
        for (TalentDataAttribute talent : metas) {
            byIndex.put(talent.index(), talent);
        }
        return byIndex;
    }

    private static int[] treePoints(List<TalentDataAttribute> metas, int[] data, int treeCount) {
        int[] points = new int[Math.max(treeCount, 3)];
        for (TalentDataAttribute talent : metas) {
            if (talent.tree() >= 0 && talent.tree() < points.length
                    && talent.index() < data.length) {
                points[talent.tree()] += data[talent.index()];
            }
        }
        return points;
    }

    private static int totalPoints(int[] data) {
        int total = 0;
        for (int rank : data) {
            total += rank;
        }
        return total;
    }

    private static String gridKey(int tree, int row, int column) {
        return tree + ":" + row + ":" + column;
    }

    private static String[] treeNames(TalentsBase talents) {
        if (talents instanceof MageTalents) {
            return MageTalents.TreeNames;
        }
        if (talents instanceof WarriorTalents) {
            return WarriorTalents.TreeNames;
        }
        if (talents instanceof PaladinTalents) {
            return PaladinTalents.TreeNames;
        }
        if (talents instanceof HunterTalents) {
            return HunterTalents.TreeNames;
        }
        if (talents instanceof RogueTalents) {
            return RogueTalents.TreeNames;
        }
        if (talents instanceof PriestTalents) {
            return PriestTalents.TreeNames;
        }
        if (talents instanceof DeathKnightTalents) {
            return DeathKnightTalents.TreeNames;
        }
        if (talents instanceof ShamanTalents) {
            return ShamanTalents.TreeNames;
        }
        if (talents instanceof WarlockTalents) {
            return WarlockTalents.TreeNames;
        }
        if (talents instanceof DruidTalents) {
            return DruidTalents.TreeNames;
        }
        return new String[] { "Tree 1", "Tree 2", "Tree 3" };
    }


}
