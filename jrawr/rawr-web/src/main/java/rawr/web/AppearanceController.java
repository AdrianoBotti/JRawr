package rawr.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Appearance modal: a browser-local palette editor for every themed colour and font stack in the
 * web UI. Nothing is stored server-side; the theme is applied by {@code theme.js} and persisted in
 * {@code localStorage} so it can be restored before the first paint.
 */
@Controller
public class AppearanceController {

    /** htmx: the appearance modal. */
    @GetMapping("/appearance")
    public String appearance(Model model) {
        model.addAttribute("palette", AppearancePalette.groups());
        return "fragments/appearance :: appearanceModal";
    }
}
