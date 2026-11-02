package rawr.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rawr.base.character.Character;

import java.util.Map;

/** Gemming template editor (current model defaults + custom templates). */
@Controller
public class GemmingTemplateController {
    private final GemmingTemplateService templates;
    private final EditorService editorService;
    private final GlobalSettingsService globalSettings;

    public GemmingTemplateController(GemmingTemplateService templates, EditorService editorService,
            GlobalSettingsService globalSettings) {
        this.templates = templates;
        this.editorService = editorService;
        this.globalSettings = globalSettings;
    }

    @GetMapping("/character/gemming-templates")
    public String modal(HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        model.addAttribute("gemming", templates.view(character));
        model.addAttribute("gemmingError", null);
        return "fragments/gemming-templates :: gemmingModal";
    }

    @GetMapping("/character/gemming-templates/body")
    public String body(HttpSession session, Model model) {
        return render(session, model, null);
    }

    @PostMapping("/character/gemming-templates/toggle")
    public String toggle(@RequestParam(name = "index", defaultValue = "0") int index,
            @RequestParam(name = "custom", defaultValue = "false") boolean custom,
            @RequestParam(name = "enabled", defaultValue = "false") boolean enabled,
            HttpSession session, Model model) {
        try {
            templates.toggle(editorService.getOrCreate(session), custom, index, enabled);
            editorService.markDirty(session);
        } catch (RuntimeException e) {
            return render(session, model, message(e));
        }
        return render(session, model, null);
    }

    @PostMapping("/character/gemming-templates/add")
    public String add(HttpSession session, Model model) {
        try {
            templates.addCustom(editorService.getOrCreate(session));
            editorService.markDirty(session);
        } catch (RuntimeException e) {
            return render(session, model, message(e));
        }
        return render(session, model, null);
    }

    @PostMapping("/character/gemming-templates/delete")
    public String delete(@RequestParam(name = "index", defaultValue = "0") int index,
            HttpSession session, Model model) {
        try {
            templates.deleteCustom(editorService.getOrCreate(session), index);
            editorService.markDirty(session);
        } catch (RuntimeException e) {
            return render(session, model, message(e));
        }
        return render(session, model, null);
    }

    @PostMapping("/character/gemming-templates/create")
    public String create(@RequestParam Map<String, String> params,
            HttpSession session, Model model) {
        try {
            templates.createCustom(editorService.getOrCreate(session), params);
            editorService.markDirty(session);
        } catch (RuntimeException e) {
            return render(session, model, message(e));
        }
        return render(session, model, null);
    }

    @PostMapping("/character/gemming-templates/update")
    public String update(@RequestParam(name = "index", defaultValue = "0") int index,
            @RequestParam Map<String, String> params,
            HttpSession session, Model model) {
        try {
            templates.updateCustom(editorService.getOrCreate(session), index, params);
            editorService.markDirty(session);
        } catch (RuntimeException e) {
            return render(session, model, message(e));
        }
        return render(session, model, null);
    }

    /** htmx: the global chart variant count (top gemmings) lives with the gemming templates. */
    @PostMapping("/character/gemming-templates/top-gemmings")
    public String topGemmings(
            @RequestParam(name = "topGemmings", required = false) String value,
            HttpServletResponse response,
            Model model) {
        int parsed;
        try {
            parsed = Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            parsed = -1;
        }
        if (parsed < 1 || parsed > GlobalSettings.MAX_TOP_GEMMINGS) {
            model.addAttribute("topGemmingsError",
                    "Enter a number between 1 and " + GlobalSettings.MAX_TOP_GEMMINGS + ".");
            return "fragments/gemming-templates :: topGemmingsStatus";
        }
        GlobalSettings current = globalSettings.current();
        GlobalSettings updated = new GlobalSettings(current.hideProfEnchants(),
                current.procEffectMode(), current.combinationEffectMode(),
                current.useMultithreading(), current.maxThreads(), parsed);
        globalSettings.apply(updated);
        GlobalSettingsService.storeCookie(response, GlobalSettingsService.TOP_GEMMINGS_COOKIE,
                Integer.toString(parsed));
        model.addAttribute("topGemmingsSaved", true);
        return "fragments/gemming-templates :: topGemmingsStatus";
    }

    private String render(HttpSession session, Model model, String error) {
        Character character = editorService.getOrCreate(session);
        model.addAttribute("gemming", templates.view(character));
        model.addAttribute("gemmingError", error);
        return "fragments/gemming-templates :: gemmingPanel";
    }

    private static String message(RuntimeException e) {
        return e.getMessage() != null ? e.getMessage() : "Could not update the gemming templates.";
    }
}
