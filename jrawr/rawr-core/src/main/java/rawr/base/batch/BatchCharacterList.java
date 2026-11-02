// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/BatchCharacter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// BindingList<T> becomes ArrayList<BatchCharacter> and the XmlSerializer Load/Save pair is replaced by
// JDK DOM read/write of the same element names/order the XmlSerializer produced
// (ArrayOfBatchCharacter/BatchCharacter/RelativePath/Weight/Locked; XmlIgnore members are skipped).
// Omitted (reported): IBindingList notifications and PropertyChanged (UI).
package rawr.base.batch;

import rawr.base.util.KeyValuePair;

import rawr.base.character.Character;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static rawr.base.util.XmlDom.appendElementIfNotNull;
import static rawr.base.util.XmlDom.rawText;

/**
 * The ordered roster of a batch run: a list of {@link BatchCharacter} entries with XML
 * persistence, score aggregation and conversion helpers for the batch tools.
 *
 * <p>Extends {@code ArrayList}, so it is a plain mutable list; the added methods serve the batch
 * workflow. {@link #load(String)} and {@link #save(String)} round-trip the same XML shape the C#
 * {@code XmlSerializer} produced (a root {@code ArrayOfBatchCharacter} holding one
 * {@code BatchCharacter} element per entry). {@link #getBatchOptimizerList()} converts the roster
 * to the (character, weight) pairs the joint {@link BatchOptimizer} takes. {@link #getScore()}
 * and {@link #getNewScore()} aggregate per-entry scores into the weighted roster totals shown by
 * {@link BatchTools#updateStatusLabel()}; an entry's weight defaults to 1 and scales its
 * contribution.</p>
 *
 * <p>The C# binding-list change notifications and per-entry {@code PropertyChanged} forwarding
 * were UI plumbing and are omitted; {@link BatchTools#setBatchCharacterList} is where the port
 * raises the roster-level property-change notification.</p>
 */
public class BatchCharacterList extends ArrayList<BatchCharacter> {
    /**
     * Reads a roster file and rebuilds the list. The accepted layout is the one
     * {@link #save(String)} writes: root {@code ArrayOfBatchCharacter} with {@code BatchCharacter}
     * children carrying optional {@code RelativePath}, {@code Weight} and {@code Locked} elements
     * in that order. A missing element keeps the entry default (no path, weight 1, unlocked);
     * unknown elements and non-element nodes are ignored, so both original serializer output and
     * hand-written files load. Parse failures are wrapped in a {@link RuntimeException}.
     */
    public static BatchCharacterList load(String path) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(path));
            BatchCharacterList list = new BatchCharacterList();
            Element root = doc.getDocumentElement();
            for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n.getNodeType() != Node.ELEMENT_NODE) continue;
                Element element = (Element) n;
                if (!element.getTagName().equals("BatchCharacter")) continue;
                BatchCharacter batchCharacter = new BatchCharacter();
                String relativePath = rawText(element, "RelativePath");
                if (relativePath != null) batchCharacter.setRelativePath(relativePath);
                String weight = rawText(element, "Weight");
                if (weight != null) batchCharacter.setWeight(Float.parseFloat(weight));
                String locked = rawText(element, "Locked");
                if (locked != null) batchCharacter.setLocked(Boolean.parseBoolean(locked));
                list.add(batchCharacter);
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load batch character list " + path, e);
        }
    }

    /**
     * Writes the roster to a file in the layout described in {@link #load(String)}: UTF-8,
     * indented with two spaces, with the .NET root namespace attributes the original serializer
     * emitted. Weight and locked are always written; a {@code null} relative path omits its
     * element. I/O failures are wrapped in a {@link RuntimeException}.
     */
    public void save(String path) {
        try (OutputStream out = Files.newOutputStream(Path.of(path))) {
            save(out);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save batch character list " + path, e);
        }
    }

    /**
     * Same XML output as {@link #save(String)}, written to a caller-owned stream that is not
     * closed. Failures are wrapped in a {@link RuntimeException}.
     */
    public void save(OutputStream stream) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = doc.createElement("ArrayOfBatchCharacter");
            root.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
            root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            doc.appendChild(root);
            for (BatchCharacter batchCharacter : this) {
                Element element = doc.createElement("BatchCharacter");
                root.appendChild(element);
                appendElementIfNotNull(doc, element, "RelativePath", batchCharacter.getRelativePath());
                appendElementIfNotNull(doc, element, "Weight", Float.toString(batchCharacter.getWeight()));
                appendElementIfNotNull(doc, element, "Locked", Boolean.toString(batchCharacter.isLocked()));
            }
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(doc), new StreamResult(stream));
        } catch (Exception e) {
            throw new RuntimeException("Failed to save batch character list", e);
        }
    }



    /**
     * Snapshot of the roster as (character, weight) pairs for the {@link BatchOptimizer}
     * constructor. Resolves every entry's character, which may trigger lazy file loads; an entry
     * without a character contributes a pair with a {@code null} key.
     */
    public List<KeyValuePair<Character, Float>> getBatchOptimizerList() {
        List<KeyValuePair<Character, Float>> list = new ArrayList<KeyValuePair<Character, Float>>();
        for (BatchCharacter batchCharacter : this) {
            list.add(new KeyValuePair<Character, Float>(batchCharacter.getCharacter(), batchCharacter.getWeight()));
        }
        return list;
    }

    /**
     * Weighted roster total of the projected scores: each entry contributes
     * {@code weight * (newScore != null ? newScore : score)}. This is the "after changes" side of
     * the status string shown by {@link BatchTools#updateStatusLabel()}.
     */
    public float getNewScore() {
        float score = 0.0f;
        for (BatchCharacter character : this) {
            score += character.getWeight() * (character.getNewScore() != null ? character.getNewScore() : character.getScore());
        }
        return score;
    }

    /**
     * Weighted roster total of the saved scores ({@code weight * score} per entry), the baseline
     * side of the status string shown by {@link BatchTools#updateStatusLabel()}.
     */
    public float getScore() {
        float score = 0.0f;
        for (BatchCharacter character : this) {
            score += character.getWeight() * character.getScore();
        }
        return score;
    }
}
