package rawr.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rawr.base.character.Character;

import java.util.List;
import java.util.Map;

/** Item filter tree, per-model item types and Other flag editor. */
@Controller
public class ItemFilterController {
    private final ItemFilterService filters;
    private final EditorService editorService;

    public ItemFilterController(ItemFilterService filters, EditorService editorService) {
        this.filters = filters;
        this.editorService = editorService;
    }

    @GetMapping("/character/item-filters")
    public String modal(@RequestParam(name = "path", required = false) String path,
            HttpSession session, HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "no-store");
        Character character = editorService.getOrCreate(session);
        model.addAttribute("filters", filters.describe(character, path));
        model.addAttribute("filtersError", null);
        return "fragments/item-filters :: filtersModal";
    }

    @GetMapping("/character/item-filters/body")
    public String body(@RequestParam(name = "path", required = false) String path,
            HttpSession session, HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "no-store");
        Character character = editorService.getOrCreate(session);
        model.addAttribute("filters", filters.describe(character, path));
        model.addAttribute("filtersError", null);
        return "fragments/item-filters :: filtersPanel";
    }

    @PostMapping("/character/item-filters/node")
    public String apply(@RequestParam(name = "path", required = false) String path,
            @RequestParam Map<String, String> params,
            HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            filters.apply(path, params);
            filters.invalidate(character);
        } catch (RuntimeException e) {
            return error(character, model, path, e);
        }
        return render(character, model, path);
    }

    @PostMapping("/character/item-filters/add")
    public String add(@RequestParam(name = "parentPath", required = false) String parentPath,
            HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            String path = filters.add(parentPath);
            filters.invalidate(character);
            return render(character, model, path);
        } catch (RuntimeException e) {
            return error(character, model, parentPath, e);
        }
    }

    @PostMapping("/character/item-filters/delete")
    public String delete(@RequestParam(name = "path", required = false) String path,
            HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            String next = filters.delete(path);
            filters.invalidate(character);
            return render(character, model, next);
        } catch (RuntimeException e) {
            return error(character, model, path, e);
        }
    }

    @PostMapping("/character/item-filters/enabled")
    public String enabled(@RequestParam(name = "path", required = false) String path,
            @RequestParam(name = "selectedPath", required = false) String selectedPath,
            @RequestParam(name = "enabled", defaultValue = "false") boolean enabled,
            @RequestParam(name = "deep", defaultValue = "false") boolean deep,
            HttpSession session, HttpServletResponse response, Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            filters.setEnabled(path, enabled, deep);
            filters.invalidate(character);
        } catch (RuntimeException e) {
            // The checkbox form swaps nothing on success; an error still replaces the panel.
            response.setHeader("HX-Reswap", "outerHTML");
            return error(character, model, selectedPath != null ? selectedPath : path, e);
        }
        return "fragments/file :: empty";
    }

    @PostMapping("/character/item-filters/other")
    public String other(@RequestParam(name = "otherEnabled", defaultValue = "false") boolean enabled,
            HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            filters.setOther(enabled);
            filters.invalidate(character);
        } catch (RuntimeException e) {
            return error(character, model, null, e);
        }
        return render(character, model, null);
    }

    @PostMapping("/character/item-filters/types")
    public String types(@RequestParam(name = "type", required = false) List<String> types,
            HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            filters.setTypes(character, types);
            filters.invalidate(character);
        } catch (RuntimeException e) {
            return error(character, model, null, e);
        }
        return render(character, model, null);
    }

    private String render(Character character, Model model, String path) {
        model.addAttribute("filters", filters.describe(character, path));
        model.addAttribute("filtersError", null);
        return "fragments/item-filters :: filtersPanel";
    }

    private String error(Character character, Model model, String path, RuntimeException e) {
        model.addAttribute("filters", filters.describe(character, path));
        model.addAttribute("filtersError", e.getMessage() != null ? e.getMessage()
                : "Could not update the item filters.");
        return "fragments/item-filters :: filtersPanel";
    }
}
