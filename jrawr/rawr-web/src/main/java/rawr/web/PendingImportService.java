package rawr.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import rawr.base.character.Character;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Session state for a character import whose equipped items are not all present in the local
 * {@link ItemCache}. The import is held back instead of silently dropping unknown gear; the Import
 * tab lists the missing IDs and can fetch them from the configured item source (DECISIONS #31).
 *
 * <p>The pending XML is the original import text, so the character is rebuilt from scratch after
 * the fetch (fresh {@link ItemInstance} objects resolve the newly cached items). Only one pending
 * import exists per session; storing a new one replaces it.</p>
 */
@Component
public class PendingImportService {

    static final String SESSION_XML = "jrawr.pendingImportXml";
    static final String SESSION_MISSING = "jrawr.pendingImportMissing";
    static final String SESSION_ERROR = "jrawr.pendingImportError";
    static final String SESSION_FETCHING = "jrawr.pendingImportFetching";

    /** Safety cap; an equipped character has at most 20 items with 3 gems each. */
    static final int MAX_FETCH_ITEMS = 100;

    /** Remembers a character import until the missing items are fetched or the user decides. */
    public void store(HttpSession session, String xml, List<Integer> missing) {
        session.setAttribute(SESSION_XML, xml);
        session.setAttribute(SESSION_MISSING, List.copyOf(missing));
        session.removeAttribute(SESSION_ERROR);
    }

    public boolean hasPending(HttpSession session) {
        return session.getAttribute(SESSION_XML) instanceof String;
    }

    public String xml(HttpSession session) {
        return session.getAttribute(SESSION_XML) instanceof String xml ? xml : null;
    }

    @SuppressWarnings("unchecked")
    public List<Integer> missing(HttpSession session) {
        Object value = session.getAttribute(SESSION_MISSING);
        return value instanceof List<?> list ? (List<Integer>) list : List.of();
    }

    public void setError(HttpSession session, String message) {
        if (message == null || message.isBlank()) {
            session.removeAttribute(SESSION_ERROR);
        } else {
            session.setAttribute(SESSION_ERROR, message);
        }
    }

    public String error(HttpSession session) {
        return session.getAttribute(SESSION_ERROR) instanceof String message ? message : null;
    }

    public void clear(HttpSession session) {
        session.removeAttribute(SESSION_XML);
        session.removeAttribute(SESSION_MISSING);
        session.removeAttribute(SESSION_ERROR);
    }

    /** Adds the Import-tab view model for the held-back character (if any). */
    public void populate(Model model, HttpSession session, String itemSourceTemplate) {
        List<Integer> missing = missing(session);
        model.addAttribute("pendingMissingCount", missing.size());
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < missing.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(missing.get(i));
        }
        model.addAttribute("pendingMissingText", text.toString());
        model.addAttribute("pendingImportError", error(session));
        model.addAttribute("itemSourceTemplate", itemSourceTemplate);
    }

    public boolean isFetching(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(SESSION_FETCHING));
    }

    public void setFetching(HttpSession session, boolean fetching) {
        if (fetching) {
            session.setAttribute(SESSION_FETCHING, Boolean.TRUE);
        } else {
            session.removeAttribute(SESSION_FETCHING);
        }
    }

    /**
     * Item IDs referenced by the character that are absent from {@link ItemCache}: every equipped
     * item plus its gems (the item link stores the gem IDs, and gem stats are resolved as items).
     * Enchants are not items and are resolved from the shipped enchant table.
     *
     * <p>IDs are de-duplicated and returned in ascending order so the UI and the fetch order are
     * stable.</p>
     */
    public static List<Integer> findMissingItems(Character character) {
        Set<Integer> missing = new LinkedHashSet<>();
        for (ItemInstance instance : character.getItems()) {
            if (instance == null) {
                continue;
            }
            addIfMissing(missing, instance.getId());
            addIfMissing(missing, instance.getGem1Id());
            addIfMissing(missing, instance.getGem2Id());
            addIfMissing(missing, instance.getGem3Id());
        }
        List<Integer> sorted = new ArrayList<>(missing);
        Collections.sort(sorted);
        return List.copyOf(sorted);
    }

    private static void addIfMissing(Set<Integer> missing, int itemId) {
        if (itemId > 0 && ItemCache.findItemById(itemId) == null) {
            missing.add(itemId);
        }
    }
}
