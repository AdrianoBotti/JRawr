package rawr.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rawr.base.character.Character;

import java.util.Map;

/**
 * "Model options" modal of the editor, opened from the header. {@code GET /character/options}
 * renders the current model's allowlisted options; {@code POST} validates and applies the posted
 * fields, marks the character dirty, recalculates and swaps the inner panel plus the paper doll and
 * stats (htmx). {@code POST /character/options/reset} restores the model defaults. Invalid input
 * re-renders the inner panel with an inline error and leaves the current options untouched.
 */
@Controller
public class ModelOptionsController {
    private final ModelOptionsService modelOptionsService;
    private final EditorService editorService;
    private final EditorViews editorViews;
    private final ItemIconService itemIcons;

    public ModelOptionsController(
            ModelOptionsService modelOptionsService,
            EditorService editorService,
            EditorViews editorViews,
            ItemIconService itemIcons) {
        this.modelOptionsService = modelOptionsService;
        this.editorService = editorService;
        this.editorViews = editorViews;
        this.itemIcons = itemIcons;
    }

    /** Exposes the icon URL/initials helper to the swapped paper-doll fragment as {@code ${icons}}. */
    @ModelAttribute("icons")
    public ItemIconService icons() {
        return itemIcons;
    }

    /** htmx: the current model's options card (also the full-page sidebar fragment). */
    @GetMapping("/character/options")
    public String options(HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        model.addAttribute("modelOptions", modelOptionsService.describe(character));
        model.addAttribute("optionsError", null);
        return "fragments/options :: optionsModal";
    }

    /** htmx: apply the posted options, then swap paper doll + stats + options card. */
    @PostMapping("/character/options")
    public String apply(
            @RequestParam(name = "model", required = false) String modelName,
            @RequestParam Map<String, String> params,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            modelOptionsService.apply(character, modelName, params);
            editorService.markDirty(session);
        } catch (RuntimeException e) {
            model.addAttribute("modelOptions", modelOptionsService.describe(character));
            model.addAttribute("optionsError", e.getMessage() != null
                    ? e.getMessage()
                    : "Could not apply the options.");
            return "fragments/options :: optionsPanel";
        }
        editorViews.populateEditor(model, character);
        model.addAttribute("modelOptions", modelOptionsService.describe(character));
        model.addAttribute("optionsError", null);
        return "fragments/options :: optionsSwap";
    }

    /** htmx: select, add or delete a model-specific collection entry (rotations, profiles). */
    @PostMapping("/character/options/collection")
    public String collection(
            @RequestParam(name = "kind") String kind,
            @RequestParam(name = "action", defaultValue = "SELECT") String action,
            @RequestParam(name = "index", defaultValue = "0") String index,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            int selected = Integer.parseInt(index);
            modelOptionsService.applyCollection(character, kind, action, selected);
            editorService.markDirty(session);
        } catch (RuntimeException e) {
            model.addAttribute("modelOptions", modelOptionsService.describe(character));
            model.addAttribute("optionsError", e.getMessage() != null
                    ? e.getMessage()
                    : "Could not change the selected entry.");
            return "fragments/options :: optionsPanel";
        }
        editorViews.populateEditor(model, character);
        model.addAttribute("modelOptions", modelOptionsService.describe(character));
        model.addAttribute("optionsError", null);
        return "fragments/options :: optionsSwap";
    }

    /** htmx: restore the current model's default options. */
    @PostMapping("/character/options/reset")
    public String reset(HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            modelOptionsService.reset(character);
            editorService.markDirty(session);
        } catch (RuntimeException e) {
            model.addAttribute("modelOptions", modelOptionsService.describe(character));
            model.addAttribute("optionsError", "Could not reset the options: " + e.getMessage());
            return "fragments/options :: optionsPanel";
        }
        editorViews.populateEditor(model, character);
        model.addAttribute("modelOptions", modelOptionsService.describe(character));
        model.addAttribute("optionsError", null);
        return "fragments/options :: optionsSwap";
    }
}
