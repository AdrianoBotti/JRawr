package rawr.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * About dialog: project provenance, credits, license and attribution. The text is drawn from the
 * repository {@code NOTICE} file so it stays in one place.
 */
@Controller
public class AboutController {

    /** htmx: the About modal. */
    @GetMapping("/about")
    public String about() {
        return "fragments/about :: aboutModal";
    }
}
