// New code (not a line-by-line port): side-car store for user-saved talent specs plus the
// optimizer include/exclude selection (BACKLOG B-003). The spec half is written with the exact
// legacy Talents.xml <SavedTalentSpec> element schema (Name/Class/Spec/Tree1..3) so a user list
// could be pasted into the shipped Talents.xml unchanged; the disabled-selection half is a
// non-legacy <DisabledKey> element ignored by the legacy loader.
package rawr.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import rawr.base.character.CharacterClass;
import rawr.base.character.SavedTalentSpec;
import rawr.base.character.SavedTalentSpecList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Persists the user's saved talent specs (and the optimizer include/exclude selection) so they
 * survive restarts, mirroring {@link UserItemStore}. The shipped {@code Talents.xml} list is
 * loaded by {@link #load(Path)} through {@link TalentSpecLoader} and captured as the immutable
 * "shipped" prefix; the user specs read from this side-car are appended after it and the merged
 * list is installed as the process-wide {@link SavedTalentSpec} registry.
 *
 * <p>The include/exclude selection is stored as a set of disabled keys ({@code Class|specString});
 * a spec is enabled unless its key is present, so newly saved specs are included by default. A
 * corrupt side-car is ignored with a warning, leaving the shipped list intact.</p>
 */
public final class SavedTalentSpecStore {
    /** Default file name; the rawr-web layer resolves it next to {@code jrawr.characters-dir}. */
    public static final String DEFAULT_FILE_NAME = "jrawr.talents.xml";

    private static final System.Logger LOG = System.getLogger(SavedTalentSpecStore.class.getName());

    private final Path file;
    private final List<SavedTalentSpec> shipped = new ArrayList<SavedTalentSpec>();
    private final List<SavedTalentSpec> userSpecs = new ArrayList<SavedTalentSpec>();
    private final Set<String> disabledKeys = new LinkedHashSet<String>();

    public SavedTalentSpecStore(Path file) {
        this.file = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
    }

    /** Resolved side-car file (exposed for diagnostics/tests). */
    public Path getFile() {
        return file;
    }

    /**
     * Loads the shipped list from {@code shippedFile} (when present), the user list and the
     * disabled selection from this side-car, and reinstalls the merged registry. A missing
     * side-car is not an error; a corrupt one is ignored with a warning.
     */
    public synchronized void load(Path shippedFile) {
        shipped.clear();
        if (shippedFile != null && Files.isRegularFile(shippedFile)) {
            try {
                TalentSpecLoader.load(shippedFile);
            } catch (Exception e) {
                LOG.log(System.Logger.Level.WARNING,
                        "Ignoring unreadable shipped talent specs " + shippedFile + ": " + e.getMessage());
            }
        }
        SavedTalentSpecList all = SavedTalentSpec.getAllSpecs();
        if (all != null) {
            shipped.addAll(all);
        }
        userSpecs.clear();
        disabledKeys.clear();
        if (Files.isRegularFile(file)) {
            try {
                Document document = XmlLoader.parse(file);
                read(document);
            } catch (Exception e) {
                LOG.log(System.Logger.Level.WARNING,
                        "Ignoring corrupt talent-spec side-car " + file + ": " + e.getMessage());
                userSpecs.clear();
                disabledKeys.clear();
            }
        }
        reinstall();
    }

    /** Number of shipped specs captured at load (the boundary before the user's specs). */
    public synchronized int shippedCount() {
        return shipped.size();
    }

    /** The user's saved specs, in order (read-only view). */
    public synchronized List<SavedTalentSpec> userSpecs() {
        return Collections.unmodifiableList(new ArrayList<SavedTalentSpec>(userSpecs));
    }

    /** The disabled {@code Class|specString} keys (read-only view). */
    public synchronized Set<String> disabledKeys() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(disabledKeys));
    }

    /** Whether a saved spec is included in the optimizer search. */
    public synchronized boolean isEnabled(SavedTalentSpec spec) {
        return !disabledKeys.contains(key(spec));
    }

    /**
     * Adds (or replaces) a user spec. Two specs are the same allocation when their class and spec
     * string match, so re-saving the current build replaces an earlier copy rather than duplicating
     * it. A replaced spec is enabled by default (its disabled key, if any, is dropped).
     */
    public synchronized void saveCurrent(SavedTalentSpec spec) {
        if (spec == null) {
            return;
        }
        userSpecs.removeIf(existing -> sameAllocation(existing, spec));
        userSpecs.add(spec);
        disabledKeys.remove(key(spec));
        persist();
    }

    /** Renames a user spec by index in {@link #userSpecs()}. */
    public synchronized void rename(int index, String name) {
        SavedTalentSpec spec = requireUserSpec(index);
        spec.name = name == null ? "" : name;
        persist();
    }

    /** Deletes a user spec by index in {@link #userSpecs()}, also dropping its disabled key. */
    public synchronized void delete(int index) {
        SavedTalentSpec spec = requireUserSpec(index);
        userSpecs.remove(index);
        disabledKeys.remove(key(spec));
        persist();
    }

    /** Includes or excludes a spec from the optimizer search by {@code Class|specString} key. */
    public synchronized void setEnabled(String key, boolean enabled) {
        if (enabled) {
            disabledKeys.remove(key);
        } else {
            disabledKeys.add(key);
        }
        persist();
    }

    private SavedTalentSpec requireUserSpec(int index) {
        if (index < 0 || index >= userSpecs.size()) {
            throw new IllegalArgumentException("Unknown saved spec.");
        }
        return userSpecs.get(index);
    }

    private void persist() {
        reinstall();
        Path parent = file.getParent();
        Path temp = null;
        try {
            Path directory = parent != null ? parent : Paths.get(".");
            Files.createDirectories(directory);
            temp = Files.createTempFile(directory, "jrawr.talents", ".tmp");
            Files.writeString(temp, toXml(), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING,
                    "Could not persist talent-spec side-car " + file + ": " + e.getMessage());
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // best effort cleanup
                }
            }
        }
    }

    private void reinstall() {
        SavedTalentSpecList merged = new SavedTalentSpecList(shipped.size() + userSpecs.size());
        merged.addAll(shipped);
        merged.addAll(userSpecs);
        SavedTalentSpec.load(merged);
    }

    // ------------------------------------------------------------------ reading

    private void read(Document document) {
        Element root = document.getDocumentElement();
        if (root == null) {
            return;
        }
        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) node;
            if ("SavedTalentSpec".equals(element.getTagName())) {
                SavedTalentSpec spec = parseSpec(element);
                if (spec != null) {
                    userSpecs.add(spec);
                }
            } else if ("DisabledKey".equals(element.getTagName())) {
                String key = element.getTextContent().trim();
                if (!key.isEmpty()) {
                    disabledKeys.add(key);
                }
            }
        }
    }

    private static SavedTalentSpec parseSpec(Element element) {
        String className = XmlLoader.text(element, "Class");
        if (className == null) {
            return null;
        }
        SavedTalentSpec spec = new SavedTalentSpec();
        spec.name = XmlLoader.text(element, "Name");
        try {
            spec.characterClass = CharacterClass.valueOf(className);
        } catch (IllegalArgumentException e) {
            return null;
        }
        spec.spec = XmlLoader.text(element, "Spec");
        spec.tree1 = intValue(element, "Tree1");
        spec.tree2 = intValue(element, "Tree2");
        spec.tree3 = intValue(element, "Tree3");
        return spec;
    }

    private static int intValue(Element element, String name) {
        String text = XmlLoader.text(element, name);
        return text == null || text.isEmpty() ? 0 : Integer.parseInt(text);
    }

    // ------------------------------------------------------------------ writing

    private String toXml() {
        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        xml.append("<ArrayOfSavedTalentSpec xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"")
                .append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");
        for (SavedTalentSpec spec : userSpecs) {
            xml.append("  <SavedTalentSpec>\n");
            element(xml, "    ", "Name", spec.name);
            element(xml, "    ", "Class", spec.characterClass == null ? "" : spec.characterClass.name());
            element(xml, "    ", "Spec", spec.spec);
            element(xml, "    ", "Tree1", spec.tree1);
            element(xml, "    ", "Tree2", spec.tree2);
            element(xml, "    ", "Tree3", spec.tree3);
            xml.append("  </SavedTalentSpec>\n");
        }
        for (String key : disabledKeys) {
            element(xml, "  ", "DisabledKey", key);
        }
        xml.append("</ArrayOfSavedTalentSpec>\n");
        return xml.toString();
    }

    private static void element(StringBuilder xml, String indent, String tag, String value) {
        xml.append(indent).append('<').append(tag).append('>')
                .append(escape(value))
                .append("</").append(tag).append(">\n");
    }

    private static void element(StringBuilder xml, String indent, String tag, int value) {
        xml.append(indent).append('<').append(tag).append('>')
                .append(value)
                .append("</").append(tag).append(">\n");
    }

    private static boolean sameAllocation(SavedTalentSpec left, SavedTalentSpec right) {
        return Objects.equals(left.characterClass, right.characterClass)
                && Objects.equals(left.spec, right.spec);
    }

    static String key(SavedTalentSpec spec) {
        return key(spec.characterClass, spec.spec);
    }

    /** The {@code Class|specString} identity used for the include/exclude selection. */
    public static String key(CharacterClass characterClass, String spec) {
        String className = characterClass == null ? "" : characterClass.name();
        return className + "|" + (spec == null ? "" : spec);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '\r' -> escaped.append("&#xD;");
                default -> {
                    if (c == '\t' || c == '\n' || c >= 0x20) {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
