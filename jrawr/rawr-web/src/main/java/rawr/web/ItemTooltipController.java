package rawr.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;

/**
 * htmx fragment endpoint for item tooltips. {@code GET /item-tooltip?id=<itemId>} renders the
 * base item; with an extra {@code &slot=<slot>} it renders the equipped instance's total stats
 * (gems, socket bonus, enchant) for the session character. With {@code &picker=true} (item
 * picker rows) the requested item is a candidate for the slot and set bonuses are marked for the
 * simulated equip instead.
 */
@Controller
public class ItemTooltipController {
    private final ItemTooltipService tooltipService;
    private final EditorService editorService;
    private final CalculationService calculationService;

    public ItemTooltipController(
            ItemTooltipService tooltipService,
            EditorService editorService,
            CalculationService calculationService) {
        this.tooltipService = tooltipService;
        this.editorService = editorService;
        this.calculationService = calculationService;
    }

    @GetMapping("/item-tooltip")
    public String itemTooltip(
            @RequestParam(name = "id", required = false) Integer itemId,
            @RequestParam(name = "slot", required = false) String slotName,
            @RequestParam(name = "instance", required = false) String gemmedId,
            @RequestParam(name = "picker", defaultValue = "false") boolean picker,
            HttpSession session,
            Model model) {
        ItemTooltip tooltip = null;
        if (slotName != null) {
            CharacterSlot slot;
            try {
                slot = CharacterSlot.valueOf(slotName);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown slot: " + slotName);
            }
            Character character = editorService.getOrCreate(session);
            calculationService.ensureModelLoaded(character);
            if (gemmedId != null && !gemmedId.isBlank()) {
                tooltip = tooltipService.forInstance(character, slot, gemmedId);
            } else {
                tooltip = picker && itemId != null
                        ? tooltipService.forPicker(character, slot, itemId)
                        : tooltipService.forSlot(character, slot);
            }
        }
        if (tooltip == null && itemId != null) {
            tooltip = tooltipService.forItem(itemId);
        }
        if (tooltip == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No item for id=" + itemId + " slot=" + slotName);
        }
        model.addAttribute("tooltip", tooltip);
        return "fragments/item-tooltip :: tooltip";
    }
}
