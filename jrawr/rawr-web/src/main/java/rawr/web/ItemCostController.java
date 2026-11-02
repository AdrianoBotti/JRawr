package rawr.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Item cost editor for the optimizer's [Cost] calculation and requirements. */
@Controller
public class ItemCostController {
    private final ItemCostService costs;

    public ItemCostController(ItemCostService costs) {
        this.costs = costs;
    }

    @GetMapping("/character/item-costs")
    public String modal(@RequestParam(name = "q", required = false) String query, Model model) {
        model.addAttribute("costs", costs.view(query));
        model.addAttribute("costError", null);
        return "fragments/item-costs :: costsModal";
    }

    @GetMapping("/character/item-costs/body")
    public String body(@RequestParam(name = "q", required = false) String query, Model model) {
        return render(query, model, null);
    }

    @PostMapping("/character/item-costs")
    public String set(@RequestParam("itemId") int itemId,
            @RequestParam("cost") String rawCost,
            @RequestParam(name = "q", required = false) String query,
            Model model) {
        try {
            costs.setCost(itemId, Float.parseFloat(rawCost.trim()));
        } catch (NumberFormatException e) {
            return render(query, model, "Cost must be a number.");
        } catch (RuntimeException e) {
            return render(query, model, e.getMessage() != null ? e.getMessage()
                    : "Could not save that cost.");
        }
        return render(query, model, null);
    }

    @PostMapping("/character/item-costs/reset")
    public String reset(@RequestParam(name = "q", required = false) String query, Model model) {
        try {
            costs.resetAll();
        } catch (RuntimeException e) {
            return render(query, model, e.getMessage() != null ? e.getMessage()
                    : "Could not reset the item costs.");
        }
        return render(query, model, null);
    }

    private String render(String query, Model model, String error) {
        model.addAttribute("costs", costs.view(query));
        model.addAttribute("costError", error);
        return "fragments/item-costs :: costsPanel";
    }
}
