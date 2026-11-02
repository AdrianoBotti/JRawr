package rawr.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Custom Item Pool manager: a per-slot item list with item-level and exact gemming/enchant
 * availability editing, bulk actions and text import/export. Edits write the same
 * {@code Character.AvailableItems} list the optimizer and comparison charts consume.
 *
 * <p>It lives in its own modal root ({@code #availability-modal}) so opening it from the character
 * header leaves the optimizer form untouched.</p>
 */
@Controller
public class AvailabilityController {
    private final AvailabilityService availabilityService;
    private final EditorService editorService;
    private final ItemIconService itemIcons;

    public AvailabilityController(
            AvailabilityService availabilityService,
            EditorService editorService,
            ItemIconService itemIcons) {
        this.availabilityService = availabilityService;
        this.editorService = editorService;
        this.itemIcons = itemIcons;
    }

    @ModelAttribute("icons")
    public ItemIconService icons() {
        return itemIcons;
    }

    /** htmx: the availability modal (loaded into {@code #availability-modal}). */
    @GetMapping("/character/availability")
    public String modal(
            @RequestParam(name = "slot", required = false) String slotName,
            @RequestParam(name = "q", required = false) String query,
            HttpSession session,
            Model model) {
        populate(model, session, slotName, query);
        return "fragments/availability :: availabilityModal";
    }

    /** htmx: only the modal body, used by slot/search changes and every edit response. */
    @GetMapping("/character/availability/body")
    public String body(
            @RequestParam(name = "slot", required = false) String slotName,
            @RequestParam(name = "q", required = false) String query,
            HttpSession session,
            Model model) {
        populate(model, session, slotName, query);
        return "fragments/availability :: availabilityBody";
    }

    /** htmx: toggles the item-level entry (removing it also drops exact entries for the item). */
    @PostMapping("/character/availability/item")
    public String toggleItem(
            @RequestParam(name = "slot", required = false) String slotName,
            @RequestParam("itemId") int itemId,
            @RequestParam("available") boolean available,
            @RequestParam(name = "q", required = false) String query,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        Character character = editorService.getOrCreate(session);
        Item item = ItemCache.findItemById(itemId);
        if (available && item == null) {
            model.addAttribute("availabilityError", "Item " + itemId
                    + " is not in the item cache. Use Tools → Item editor to fetch it first.");
        } else if (available && !availabilityService.isUsableItem(character, item)) {
            model.addAttribute("availabilityError", "Item " + itemId
                    + " is not usable by this character.");
        } else {
            availabilityService.setItemLevelAvailable(character, itemId, available);
            markChanged(session, response);
        }
        populate(model, session, slotName, query);
        return "fragments/availability :: availabilityBody";
    }

    /** htmx: adds/removes one exact gemming/enchant entry. */
    @PostMapping("/character/availability/exact")
    public String exact(
            @RequestParam(name = "slot", required = false) String slotName,
            @RequestParam(name = "entry", required = false) String entry,
            @RequestParam(name = "action") String action,
            @RequestParam(name = "q", required = false) String query,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            if ("REMOVE".equals(action)) {
                availabilityService.removeExact(character, entry);
                model.addAttribute("availabilityMessage", "Removed the exact availability entry.");
                markChanged(session, response);
            } else if ("ADD_EQUIPPED".equals(action)) {
                boolean added = availabilityService.addEquippedExact(character, parseSlot(slotName));
                model.addAttribute("availabilityMessage", added
                        ? "Added the equipped instance as an exact entry."
                        : "The equipped instance is already covered by the custom item pool.");
                if (added) {
                    markChanged(session, response);
                }
            } else if ("ADD".equals(action)) {
                CharacterSlot slot = parseSlot(slotName);
                if (entry == null || entry.isBlank()) {
                    model.addAttribute("availabilityError", "Enter an exact availability entry.");
                } else if (!availabilityService.entryFitsSlot(character, entry, slot)) {
                    model.addAttribute("availabilityError", "Enter an exact entry for an item that "
                            + "fits " + EditorService.slotLabel(slot) + " and this character.");
                } else {
                    boolean added = availabilityService.addExact(character, entry);
                    model.addAttribute("availabilityMessage", added
                            ? "Added the exact entry."
                            : "That exact entry is already in the custom item pool.");
                    if (added) {
                        markChanged(session, response);
                    }
                }
            } else {
                throw new IllegalArgumentException("Unknown exact-entry action: " + action);
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("availabilityError", e.getMessage());
        }
        populate(model, session, slotName, query);
        return "fragments/availability :: availabilityBody";
    }

    /** htmx: bulk replacement/clear actions. */
    @PostMapping("/character/availability/bulk")
    public String bulk(
            @RequestParam(name = "slot", required = false) String slotName,
            @RequestParam("action") String action,
            @RequestParam(name = "q", required = false) String query,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            model.addAttribute("availabilityMessage",
                    availabilityService.bulk(character, parseSlot(slotName), action));
            markChanged(session, response);
        } catch (RuntimeException e) {
            model.addAttribute("availabilityError", message(e));
        }
        populate(model, session, slotName, query);
        return "fragments/availability :: availabilityBody";
    }

    /** htmx: import the textarea list (one entry per line); duplicates are removed. */
    @PostMapping("/character/availability/import")
    public String importEntries(
            @RequestParam(name = "slot", required = false) String slotName,
            @RequestParam(name = "entries", required = false) String entries,
            @RequestParam(name = "mode", defaultValue = "replace") String mode,
            @RequestParam(name = "q", required = false) String query,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            AvailabilityService.ImportResult result = availabilityService.importEntries(
                    character, entries, "append".equals(mode));
            model.addAttribute("availabilityMessage", "Imported " + result.added()
                    + " entries (" + result.total() + " total).");
            markChanged(session, response);
        } catch (RuntimeException e) {
            model.addAttribute("availabilityError", message(e));
            model.addAttribute("availabilityImportText", entries);
        }
        populate(model, session, slotName, query);
        return "fragments/availability :: availabilityBody";
    }

    /** htmx: removes a raw pool entry shown in the "Other pool entries" section. */
    @PostMapping("/character/availability/other")
    public String other(
            @RequestParam(name = "slot", required = false) String slotName,
            @RequestParam("entry") String entry,
            @RequestParam(name = "action", defaultValue = "REMOVE") String action,
            @RequestParam(name = "q", required = false) String query,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        Character character = editorService.getOrCreate(session);
        if (!"REMOVE".equals(action)) {
            model.addAttribute("availabilityError", "Unknown other-entry action: " + action);
        } else {
            boolean removed = availabilityService.removeEntry(character, entry);
            model.addAttribute("availabilityMessage", removed
                    ? "Removed the pool entry." : "The pool entry was already gone.");
            if (removed) {
                markChanged(session, response);
            }
        }
        populate(model, session, slotName, query);
        return "fragments/availability :: availabilityBody";
    }

    /** Downloads the whole custom item pool as one entry per line. */
    @GetMapping("/character/availability/export")
    @ResponseBody
    public ResponseEntity<String> export(HttpSession session) {
        Character character = editorService.getOrCreate(session);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"availability.txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(availabilityService.exportText(character) + "\n");
    }

    // ------------------------------------------------------------------ helpers

    private void populate(Model model, HttpSession session, String slotName, String query) {
        Character character = editorService.getOrCreate(session);
        CharacterSlot slot = parseSlot(slotName);
        List<CharacterSlot> slots = OptimizerJobService.OPTIMIZE_SLOTS;
        Map<CharacterSlot, String> labels = new LinkedHashMap<CharacterSlot, String>();
        for (CharacterSlot choice : slots) {
            labels.put(choice, EditorService.slotLabel(choice));
        }
        model.addAttribute("availabilitySlots", slots);
        model.addAttribute("availabilitySlotLabels", labels);
        model.addAttribute("availabilitySlot", slot);
        model.addAttribute("availabilitySlotParam", slot.name());
        model.addAttribute("availabilityQuery", query == null ? "" : query.trim());
        model.addAttribute("availabilityExportText", availabilityService.exportText(character));
        try {
            model.addAttribute("availabilityView",
                    availabilityService.view(character, slot, query));
        } catch (RuntimeException e) {
            model.addAttribute("availabilityView", availabilityService.emptyView(slot, query));
            model.addAttribute("availabilityError", message(e));
        }
    }

    private void markChanged(HttpSession session, HttpServletResponse response) {
        editorService.markDirty(session);
        response.setHeader("HX-Trigger", "{\"jrawrDirty\":true}");
    }

    private static CharacterSlot parseSlot(String slotName) {
        if (slotName == null || slotName.isBlank()) {
            return CharacterSlot.Head;
        }
        CharacterSlot slot;
        try {
            slot = CharacterSlot.valueOf(slotName);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown slot: " + slotName);
        }
        if (!OptimizerJobService.OPTIMIZE_SLOTS.contains(slot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Slot cannot be managed: " + slotName);
        }
        return slot;
    }

    private static String message(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
