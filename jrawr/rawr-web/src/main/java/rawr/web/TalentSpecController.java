package rawr.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rawr.base.character.Character;

/**
 * The talent-spec browser and saved-spec library (BACKLOG B-003). Lists the character's current
 * spec plus the shipped and user-saved specs for its class, lets the user save/apply/rename/delete
 * specs and include/exclude them from the optimizer's talent search.
 *
 * <p>It lives in its own modal root ({@code #specs-modal}) opened from the character header, so the
 * optimizer form underneath stays untouched. Applying a spec edits the live character (dirty state
 * plus an out-of-band refresh of the talent/glyph panels and the rating/stats columns); the
 * library mutations (save/rename/delete/include) only touch the persisted spec side-car.</p>
 */
@Controller
public class TalentSpecController {

    private final TalentSpecService talentSpecs;
    private final EditorService editorService;
    private final EditorViews editorViews;
    private final ItemIconService itemIcons;

    public TalentSpecController(
            TalentSpecService talentSpecs,
            EditorService editorService,
            EditorViews editorViews,
            ItemIconService itemIcons) {
        this.talentSpecs = talentSpecs;
        this.editorService = editorService;
        this.editorViews = editorViews;
        this.itemIcons = itemIcons;
    }

    @ModelAttribute("icons")
    public ItemIconService icons() {
        return itemIcons;
    }

    /** htmx: the spec browser modal (loaded into {@code #specs-modal}). */
    @GetMapping("/character/specs")
    public String modal(HttpSession session, Model model) {
        populate(model, session);
        return "fragments/specs :: specsModal";
    }

    /** htmx: saves the character's current talents (and glyphs) as a named spec. */
    @PostMapping("/character/specs/save")
    public String save(
            @RequestParam(name = "name", required = false) String name,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            talentSpecs.saveCurrent(character, name);
            model.addAttribute("specMessage", "Saved the current spec.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("specError", e.getMessage());
        }
        populate(model, session);
        return "fragments/specs :: specsBody";
    }

    /** htmx: applies a spec; glyphs are overwritten only when the checkbox is set. */
    @PostMapping("/character/specs/apply")
    public String apply(
            @RequestParam("key") String key,
            @RequestParam(name = "includeGlyphs", defaultValue = "false") boolean includeGlyphs,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            talentSpecs.apply(character, key, includeGlyphs);
            editorService.markDirty(session);
            response.setHeader("HX-Trigger", "{\"jrawrDirty\":true}");
        } catch (IllegalArgumentException e) {
            model.addAttribute("specError", e.getMessage());
        }
        editorViews.populateEditor(model, character);
        populateSpecs(model, session);
        return "fragments/specs :: specsApplySwap";
    }

    /** htmx: renames a user-saved spec. */
    @PostMapping("/character/specs/rename")
    public String rename(
            @RequestParam("index") int index,
            @RequestParam(name = "name", required = false) String name,
            HttpSession session,
            Model model) {
        try {
            talentSpecs.rename(index, name);
            model.addAttribute("specMessage", "Renamed the spec.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("specError", e.getMessage());
        }
        populate(model, session);
        return "fragments/specs :: specsBody";
    }

    /** htmx: deletes a user-saved spec. */
    @PostMapping("/character/specs/delete")
    public String delete(
            @RequestParam("index") int index,
            HttpSession session,
            Model model) {
        try {
            talentSpecs.delete(index);
            model.addAttribute("specMessage", "Deleted the spec.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("specError", e.getMessage());
        }
        populate(model, session);
        return "fragments/specs :: specsBody";
    }

    /** htmx: includes or excludes a spec from the optimizer's talent search. */
    @PostMapping("/character/specs/toggle")
    public String toggle(
            @RequestParam("key") String key,
            @RequestParam("enabled") boolean enabled,
            HttpSession session,
            Model model) {
        talentSpecs.setEnabled(key, enabled);
        populate(model, session);
        return "fragments/specs :: specsBody";
    }

    // ------------------------------------------------------------------ helpers

    private void populate(Model model, HttpSession session) {
        Character character = editorService.getOrCreate(session);
        populateSpecs(model, session);
        model.addAttribute("character", character);
    }

    private void populateSpecs(Model model, HttpSession session) {
        Character character = editorService.getOrCreate(session);
        if (character.getCurrentModel() == null || character.getCurrentModel().isBlank()) {
            model.addAttribute("specViews", java.util.List.of());
            model.addAttribute("specError", "Choose a class/model first.");
            return;
        }
        model.addAttribute("specViews", talentSpecs.listSpecs(character));
    }
}
