package rawr.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rawr.base.character.Character;
import rawr.base.items.ItemSlot;
import rawr.data.ItemSourceTemplates;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global data/tools hub and the Item editor (BACKLOG B-022). The editor owns the only item-fetch
 * path: the Add tab parses a batch, fetches each missing item one by one, and the Edit tab lists
 * the local cache with refresh/remove actions for user-owned items.
 */
@Controller
public class ToolsController {
    private static final int SOURCE_COOKIE_MAX_AGE_SECONDS = 30 * 24 * 60 * 60;

    private final ItemEditorService itemEditor;
    private final ItemSourceService itemSources;
    private final EditorService editorService;

    public ToolsController(ItemEditorService itemEditor, ItemSourceService itemSources,
            EditorService editorService) {
        this.itemEditor = itemEditor;
        this.itemSources = itemSources;
        this.editorService = editorService;
    }

    /** Known item-source presets exposed to the editor's source selector. */
    @ModelAttribute("sourcePresets")
    public List<ItemSourceTemplates.Preset> sourcePresets() {
        return ItemSourceTemplates.PRESETS;
    }

    /** htmx: the tools modal. The individual editors replace it in the same modal root. */
    @GetMapping("/tools")
    public String tools() {
        return "fragments/tools :: toolsModal";
    }

    /** htmx: the Item editor dialog with the Add or Edit tab active. */
    @GetMapping("/tools/item-editor")
    public String editor(
            @RequestParam(name = "tab", defaultValue = "add") String tab,
            @RequestParam(name = "selected", required = false) Integer selected,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        String activeTab = "edit".equals(tab) ? "edit" : "add";
        model.addAttribute("editorTab", activeTab);
        model.addAttribute("source", itemSources.sourceInfo());
        model.addAttribute("list", itemEditor.list("", "all", "all"));
        model.addAttribute("detail", selected == null ? null
                : itemEditor.detail(selected, character));
        model.addAttribute("selectedId", selected);
        addFilters(model, "", "all", "all");
        return "fragments/item-editor :: itemEditorModal";
    }

    /**
     * htmx: apply the selected item source. Presets replace the template cookie with their known
     * URL; the Custom option accepts an absolute http(s) template containing {@code {id}}. The
     * response re-renders the source strip (or shows a validation error).
     */
    @PostMapping("/tools/item-editor/source")
    public String source(
            @RequestParam(name = "preset", defaultValue = "") String preset,
            @RequestParam(name = "template", required = false) String template,
            HttpServletResponse response,
            Model model) {
        String selected = null;
        String draft = null;
        String error = null;
        if ("custom".equalsIgnoreCase(preset)) {
            String candidate = template == null ? "" : template.trim();
            draft = candidate;
            if (ItemSourceTemplates.isValidTemplate(candidate)) {
                selected = candidate;
            } else {
                error = "Custom source must be an absolute http(s) URL template containing {id}, "
                        + "for example https://example.com/item/{id}.xml.";
            }
        } else {
            ItemSourceTemplates.Preset known = ItemSourceTemplates.preset(preset);
            if (known == null) {
                error = "Choose an item source.";
            } else {
                selected = known.template();
            }
        }
        if (selected != null) {
            Cookie cookie = new Cookie(ItemSourceService.TEMPLATE_COOKIE, selected);
            cookie.setPath("/");
            cookie.setMaxAge(SOURCE_COOKIE_MAX_AGE_SECONDS);
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        }
        model.addAttribute("source", selected == null
                ? itemSources.sourceInfo() : itemSources.sourceInfo(selected));
        model.addAttribute("sourceDraft", draft);
        model.addAttribute("sourceError", error);
        return "fragments/item-editor :: sourceStrip";
    }

    /** htmx: the custom-copy editor for a new copy or an existing custom item. */
    @GetMapping("/tools/item-editor/custom")
    public String customForm(
            @RequestParam(name = "source", required = false) Integer source,
            @RequestParam(name = "id", required = false) Integer id,
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "origin", defaultValue = "all") String origin,
            @RequestParam(name = "slot", defaultValue = "all") String slot,
            Model model) {
        model.addAttribute("form", itemEditor.customForm(source, id));
        addFilters(model, query, origin, slot);
        return "fragments/item-editor :: customItemForm";
    }

    /** htmx: return from the custom editor to the Edit tab (list plus the selected item). */
    @GetMapping("/tools/item-editor/edit-body")
    public String editBody(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "origin", defaultValue = "all") String origin,
            @RequestParam(name = "slot", defaultValue = "all") String slot,
            @RequestParam(name = "selected", required = false) Integer selected,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        model.addAttribute("list", itemEditor.list(query, origin, slot));
        model.addAttribute("detail", selected == null ? null
                : itemEditor.detail(selected, character));
        model.addAttribute("selectedId", selected);
        addFilters(model, query, origin, slot);
        return "fragments/item-editor :: editBody";
    }

    /** htmx: live preview of the custom form; nothing is cached. */
    @PostMapping("/tools/item-editor/custom/preview")
    public String customPreview(
            @RequestParam MultiValueMap<String, String> params,
            Model model) {
        ItemEditorService.CustomPreview preview = itemEditor.previewCustom(params);
        model.addAttribute("preview", preview.tooltip());
        model.addAttribute("previewError", preview.error());
        return "fragments/item-editor :: customPreview";
    }

    /** htmx: validate and save a custom copy, then show it in the Edit tab. */
    @PostMapping("/tools/item-editor/custom/save")
    public String customSave(
            @RequestParam MultiValueMap<String, String> params,
            @RequestParam(name = "filterQuery", defaultValue = "") String query,
            @RequestParam(name = "filterOrigin", defaultValue = "all") String origin,
            @RequestParam(name = "filterSlot", defaultValue = "all") String slot,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        Character character = editorService.getOrCreate(session);
        ItemEditorService.CustomSaveResult result = itemEditor.saveCustom(params, character);
        if (result.error() != null) {
            model.addAttribute("form", result.form());
            addFilters(model, query, origin, slot);
            return "fragments/item-editor :: customItemForm";
        }
        ItemEditorService.ListView list = itemEditor.list(query, origin, slot);
        ItemEditorService.DetailView detail = itemEditor.detail(result.itemId(), character)
                .withMessage("Saved custom item.");
        model.addAttribute("list", list);
        model.addAttribute("detail", detail);
        model.addAttribute("selectedId", result.itemId());
        addFilters(model, query, origin, slot);
        response.setHeader("HX-Trigger", cacheChanged(result.itemId()));
        return "fragments/item-editor :: editBody";
    }

    /** htmx: the Edit tab's filtered item list. */
    @GetMapping("/tools/item-editor/list")
    public String itemList(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "origin", defaultValue = "all") String origin,
            @RequestParam(name = "slot", defaultValue = "all") String slot,
            Model model) {
        ItemEditorService.ListView list = itemEditor.list(query, origin, slot);
        model.addAttribute("list", list);
        addFilters(model, query, list.origin(), list.slot());
        return "fragments/item-editor :: itemList";
    }

    /** htmx: one item's detail panel. */
    @GetMapping("/tools/item-editor/detail")
    public String itemDetail(
            @RequestParam("id") int id,
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "origin", defaultValue = "all") String origin,
            @RequestParam(name = "slot", defaultValue = "all") String slot,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        model.addAttribute("detail", itemEditor.detail(id, character));
        addFilters(model, query, origin, slot);
        return "fragments/item-editor :: itemDetail";
    }

    /** htmx: parse a batch into the fetch queue rendered in the Add tab. */
    @PostMapping("/tools/item-editor/parse")
    public String parse(
            @RequestParam(name = "items", required = false) String items,
            @RequestParam(name = "skipCached", required = false) String skipCached,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        model.addAttribute("queue", itemEditor.parse(items, "true".equals(skipCached), character));
        return "fragments/item-editor :: addQueue";
    }

    /** htmx: fetch one item; the browser drives the queue one request at a time. */
    @PostMapping("/tools/item-editor/fetch")
    public String fetch(
            @RequestParam("id") int id,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        Character character = editorService.getOrCreate(session);
        ItemEditorService.AddRow row = itemEditor.fetch(id, refresh, session, character);
        if (row.status() == ItemEditorService.AddStatus.ADDED
                || row.status() == ItemEditorService.AddStatus.REFRESHED) {
            response.setHeader("HX-Trigger", cacheChanged(id));
        }
        model.addAttribute("row", row);
        return "fragments/item-editor :: addResult";
    }

    /** htmx: undo the last fetch of one item. */
    @PostMapping("/tools/item-editor/undo")
    public String undo(
            @RequestParam("id") int id,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        Character character = editorService.getOrCreate(session);
        ItemEditorService.AddRow row = itemEditor.undo(id, session, character);
        if (row.status() == ItemEditorService.AddStatus.CACHED
                || row.status() == ItemEditorService.AddStatus.UNDONE) {
            response.setHeader("HX-Trigger", cacheChanged(0));
        }
        model.addAttribute("row", row);
        return "fragments/item-editor :: addResult";
    }

    /** htmx: re-fetch one user-owned item from the source and re-render its detail. */
    @PostMapping("/tools/item-editor/refresh")
    public String refresh(
            @RequestParam("id") int id,
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "origin", defaultValue = "all") String origin,
            @RequestParam(name = "slot", defaultValue = "all") String slot,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        Character character = editorService.getOrCreate(session);
        ItemEditorService.DetailView detail = itemEditor.refresh(id, session, character);
        if (!detail.hasError()) {
            response.setHeader("HX-Trigger", cacheChanged(id));
        }
        model.addAttribute("detail", detail);
        addFilters(model, query, origin, slot);
        return "fragments/item-editor :: itemDetail";
    }

    /** htmx: remove one user-owned item from the local cache and re-render the Edit tab. */
    @PostMapping("/tools/item-editor/remove")
    public String remove(
            @RequestParam("id") int id,
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "origin", defaultValue = "all") String origin,
            @RequestParam(name = "slot", defaultValue = "all") String slot,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        Character character = editorService.getOrCreate(session);
        ItemEditorService.RemoveResult result = itemEditor.remove(id, session, character);
        ItemEditorService.ListView list = itemEditor.list(query, origin, slot);
        if (result.removed()) {
            response.setHeader("HX-Trigger", cacheChanged(0));
            list = list.withMessage(result.message());
            model.addAttribute("detail", null);
        } else {
            list = list.withError(result.message());
            model.addAttribute("detail", itemEditor.detail(id, character)
                    .withError(result.message()));
        }
        model.addAttribute("list", list);
        addFilters(model, query, list.origin(), list.slot());
        return "fragments/item-editor :: editBody";
    }

    private static void addFilters(Model model, String query, String origin, String slot) {
        model.addAttribute("filterQuery", query == null ? "" : query);
        model.addAttribute("filterOrigin", origin == null ? "all" : origin);
        model.addAttribute("filterSlot", slot == null ? "all" : slot);
        model.addAttribute("itemSlots", itemSlotOptions());
    }

    /** Item slot filter options, in the enum's declaration order. */
    private static Map<String, String> itemSlotOptions() {
        Map<String, String> options = new LinkedHashMap<String, String>();
        for (ItemSlot slot : ItemSlot.values()) {
            if (slot == ItemSlot.None) {
                continue;
            }
            options.put(slot.name(), ItemEditorService.slotLabel(slot));
        }
        return options;
    }

    private static String cacheChanged(int id) {
        return id > 0
                ? "{\"jrawr:item-cache-changed\":{\"id\":" + id + "}}"
                : "{\"jrawr:item-cache-changed\":{}}";
    }
}
