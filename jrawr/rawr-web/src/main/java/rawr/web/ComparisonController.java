package rawr.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Item comparison charts (the original comparison graph). The heavy per-item rating runs in
 * {@link ComparisonService} under the global calculation lock; the charts pane renders the result
 * and lets the user sort/filter it client-side. The availability diamond edits the same
 * {@code Character.AvailableItems} list the optimizer consumes.
 */
@Controller
public class ComparisonController {
    private final ComparisonService comparisonService;
    private final EditorService editorService;
    private final ItemIconService itemIcons;

    public ComparisonController(
            ComparisonService comparisonService,
            EditorService editorService,
            ItemIconService itemIcons) {
        this.comparisonService = comparisonService;
        this.editorService = editorService;
        this.itemIcons = itemIcons;
    }

    @ModelAttribute("icons")
    public ItemIconService icons() {
        return itemIcons;
    }

    /** htmx: the persistent charts pane (initial load and slot selection). */
    @GetMapping("/character/compare")
    public String compare(
            @RequestParam(name = "slot", defaultValue = "Head") String slotName,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "baseline", defaultValue = "overall") String baselineName,
            @RequestParam(name = "chartType", defaultValue = "items") String chartTypeName,
            @RequestParam(name = "graphMode", defaultValue = "stats") String graphModeName,
            @RequestParam(name = "graphStats", required = false) List<String> graphStats,
            @RequestParam(name = "graphBase", required = false) String graphBase,
            @RequestParam(name = "graphValue", defaultValue = "overall") String graphValueName,
            @RequestParam(name = "graphSteps", defaultValue = "10") int graphSteps,
            @RequestParam(name = "enchantSlot", required = false) String enchantSlot,
            @RequestParam(name = "buffFilter", defaultValue = "All") String buffFilter,
            @RequestParam(name = "gemColor", defaultValue = "All") String gemColor,
            HttpSession session,
            Model model) {
        populateCompare(session, model, slotName, limit, baselineName, chartTypeName,
                graphModeName, graphStats, graphBase, graphValueName, graphSteps, enchantSlot,
                buffFilter, gemColor);
        return "fragments/compare :: comparePanel";
    }

    /** htmx: the chart body only, used by the toolbar and the debounced auto-refresh. */
    @GetMapping("/character/compare/body")
    public String compareBody(
            @RequestParam(name = "slot", defaultValue = "Head") String slotName,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "baseline", defaultValue = "overall") String baselineName,
            @RequestParam(name = "chartType", defaultValue = "items") String chartTypeName,
            @RequestParam(name = "graphMode", defaultValue = "stats") String graphModeName,
            @RequestParam(name = "graphStats", required = false) List<String> graphStats,
            @RequestParam(name = "graphBase", required = false) String graphBase,
            @RequestParam(name = "graphValue", defaultValue = "overall") String graphValueName,
            @RequestParam(name = "graphSteps", defaultValue = "10") int graphSteps,
            @RequestParam(name = "enchantSlot", required = false) String enchantSlot,
            @RequestParam(name = "buffFilter", defaultValue = "All") String buffFilter,
            @RequestParam(name = "gemColor", defaultValue = "All") String gemColor,
            HttpSession session,
            Model model) {
        populateCompare(session, model, slotName, limit, baselineName, chartTypeName,
                graphModeName, graphStats, graphBase, graphValueName, graphSteps, enchantSlot,
                buffFilter, gemColor);
        return "fragments/compare :: compareBody";
    }

    private void populateCompare(HttpSession session, Model model, String slotName, Integer limit,
            String baselineName, String chartTypeName, String graphModeName, List<String> graphStats,
            String graphBase, String graphValueName, int graphSteps, String enchantSlot,
            String buffFilter, String gemColor) {
        CharacterSlot slot = parseSlot(slotName);
        ComparisonService.ChartType chartType = "statgraph".equalsIgnoreCase(chartTypeName)
                || "stat-graph".equalsIgnoreCase(chartTypeName)
                ? ComparisonService.ChartType.STAT_GRAPH
                : "upgrades".equalsIgnoreCase(chartTypeName)
                || "directupgrades".equalsIgnoreCase(chartTypeName)
                || "direct-upgrades".equalsIgnoreCase(chartTypeName)
                ? ComparisonService.ChartType.DIRECT_UPGRADES
                : "enchants".equalsIgnoreCase(chartTypeName)
                || "enchant".equalsIgnoreCase(chartTypeName)
                ? ComparisonService.ChartType.ENCHANTS
                : "talents".equalsIgnoreCase(chartTypeName)
                || "talent".equalsIgnoreCase(chartTypeName)
                ? ComparisonService.ChartType.TALENTS
                : "glyphs".equalsIgnoreCase(chartTypeName)
                || "glyph".equalsIgnoreCase(chartTypeName)
                ? ComparisonService.ChartType.GLYPHS
                : "buffs".equalsIgnoreCase(chartTypeName)
                || "buff".equalsIgnoreCase(chartTypeName)
                ? ComparisonService.ChartType.BUFFS
                : "gems".equalsIgnoreCase(chartTypeName)
                || "gem".equalsIgnoreCase(chartTypeName)
                ? ComparisonService.ChartType.GEMS
                : "stats".equalsIgnoreCase(chartTypeName)
                || "relative".equalsIgnoreCase(chartTypeName)
                || "relative-stats".equalsIgnoreCase(chartTypeName)
                ? ComparisonService.ChartType.RELATIVE_STATS
                : ComparisonService.ChartType.ITEMS;
        ComparisonService.StatGraphMode graphMode = "scaling".equalsIgnoreCase(graphModeName)
                ? ComparisonService.StatGraphMode.SCALING : ComparisonService.StatGraphMode.STAT_CHANGE;
        int graphValue = -1;
        if (graphValueName != null && !graphValueName.isBlank() && !"overall".equalsIgnoreCase(graphValueName)) {
            try {
                graphValue = Integer.parseInt(graphValueName);
            } catch (NumberFormatException ignored) {
                graphValue = -1;
            }
        }
        List<String> selectedStats = new java.util.ArrayList<String>();
        if (graphStats != null) {
            for (String value : graphStats) {
                if (value == null) continue;
                for (String part : value.split(",")) {
                    if (!part.isBlank()) selectedStats.add(part.trim());
                }
            }
        }
        Character character = editorService.getOrCreate(session);
        int selectedLimit = limit == null
                ? (chartType == ComparisonService.ChartType.DIRECT_UPGRADES
                    ? ComparisonService.DEFAULT_UPGRADE_LIMIT : ComparisonService.DEFAULT_LIMIT)
                : limit;
        if (!ComparisonService.LIMIT_CHOICES.contains(selectedLimit)) {
            selectedLimit = ComparisonService.DEFAULT_LIMIT;
        }
        ComparisonService.Baseline baseline = "equipped".equalsIgnoreCase(baselineName)
                ? ComparisonService.Baseline.EQUIPPED : ComparisonService.Baseline.EMPTY_SLOT;
        model.addAttribute("compareSlots", OptimizerJobService.OPTIMIZE_SLOTS);
        model.addAttribute("compareSlotLabels", slotLabels());
        model.addAttribute("selectedSlot", slot);
        model.addAttribute("limitChoices", ComparisonService.LIMIT_CHOICES);
        model.addAttribute("selectedLimit", selectedLimit);
        model.addAttribute("selectedBaselineKey",
                baseline == ComparisonService.Baseline.EQUIPPED ? "equipped" : "overall");
        model.addAttribute("selectedChartType",
                chartType == ComparisonService.ChartType.STAT_GRAPH ? "statgraph"
                        : chartType == ComparisonService.ChartType.DIRECT_UPGRADES ? "upgrades"
                        : chartType == ComparisonService.ChartType.ENCHANTS ? "enchants"
                        : chartType == ComparisonService.ChartType.TALENTS ? "talents"
                        : chartType == ComparisonService.ChartType.GLYPHS ? "glyphs"
                        : chartType == ComparisonService.ChartType.BUFFS ? "buffs"
                        : chartType == ComparisonService.ChartType.GEMS ? "gems"
                        : chartType == ComparisonService.ChartType.RELATIVE_STATS ? "stats" : "items");
        model.addAttribute("selectedEnchantSlot", enchantSlot == null ? "" : enchantSlot);
        model.addAttribute("selectedBuffFilter", buffFilter == null ? "All" : buffFilter);
        model.addAttribute("selectedGemColor", gemColor == null ? "All" : gemColor);
        model.addAttribute("selectedGraphMode",
                graphMode == ComparisonService.StatGraphMode.SCALING ? "scaling" : "stats");
        model.addAttribute("selectedGraphBase", graphBase == null ? "" : graphBase);
        model.addAttribute("selectedGraphValue", graphValueName == null ? "overall" : graphValueName);
        model.addAttribute("selectedGraphSteps", graphSteps);
        model.addAttribute("selectedGraphStats", String.join(",", selectedStats));
        model.addAttribute("selectedGraphLineCount", 0);
        model.addAttribute("graphStepsChoices", ComparisonService.STAT_GRAPH_STEPS);
        if (character.getCurrentModel() == null || character.getCurrentModel().isBlank()) {
            model.addAttribute("compareError", chartType == ComparisonService.ChartType.ITEMS
                    ? "Choose a model before comparing items."
                    : chartType == ComparisonService.ChartType.DIRECT_UPGRADES
                    ? "Choose a model before comparing upgrades."
                    : chartType == ComparisonService.ChartType.ENCHANTS
                    ? "Choose a model before comparing enchants."
                    : chartType == ComparisonService.ChartType.TALENTS
                    ? "Choose a model before comparing talents."
                    : chartType == ComparisonService.ChartType.GLYPHS
                    ? "Choose a model before comparing glyphs."
                    : chartType == ComparisonService.ChartType.BUFFS
                    ? "Choose a model before comparing buffs."
                    : chartType == ComparisonService.ChartType.GEMS
                    ? "Choose a model before comparing gems."
                    : "Choose a model before comparing stats.");
            return;
        }
        try {
            if (chartType == ComparisonService.ChartType.DIRECT_UPGRADES) {
                model.addAttribute("upgradeChart",
                        comparisonService.directUpgrades(character, selectedLimit));
            } else if (chartType == ComparisonService.ChartType.ENCHANTS) {
                ComparisonService.EnchantChart enchantChart =
                        comparisonService.enchantChart(character, enchantSlot);
                model.addAttribute("enchantChart", enchantChart);
                if (enchantChart.slot() != rawr.base.character.CharacterSlot.None) {
                    model.addAttribute("selectedEnchantSlot", enchantChart.slot().name());
                }
            } else if (chartType == ComparisonService.ChartType.TALENTS) {
                model.addAttribute("talentChart", comparisonService.talentChart(character));
            } else if (chartType == ComparisonService.ChartType.GLYPHS) {
                model.addAttribute("glyphChart", comparisonService.glyphChart(character));
            } else if (chartType == ComparisonService.ChartType.BUFFS) {
                ComparisonService.BuffChart buffChart =
                        comparisonService.buffChart(character, buffFilter);
                model.addAttribute("buffChart", buffChart);
                model.addAttribute("selectedBuffFilter", buffChart.filter());
            } else if (chartType == ComparisonService.ChartType.GEMS) {
                ComparisonService.GemChart gemChart = comparisonService.gemChart(character, gemColor);
                model.addAttribute("gemChart", gemChart);
                model.addAttribute("selectedGemColor", gemChart.selectedColor());
            } else if (chartType == ComparisonService.ChartType.STAT_GRAPH) {
                ComparisonService.StatGraph graph = comparisonService.statGraph(character, graphMode,
                        selectedStats, graphBase, graphValue, graphSteps);
                model.addAttribute("statGraph", graph);
                model.addAttribute("selectedGraphBase", graph.baseStat() == null ? "" : graph.baseStat());
                java.util.List<String> selectedKeys = new java.util.ArrayList<String>();
                for (ComparisonService.StatOption option : graph.statOptions()) {
                    if (option.selected()) selectedKeys.add(option.key());
                }
                model.addAttribute("selectedGraphStats", String.join(",", selectedKeys));
                model.addAttribute("selectedGraphLineCount", selectedKeys.size());
            } else if (chartType == ComparisonService.ChartType.RELATIVE_STATS) {
                model.addAttribute("statChart", comparisonService.relativeStatsChart(character));
            } else {
                model.addAttribute("chart", comparisonService.gearChart(character, slot, selectedLimit, baseline));
            }
        } catch (RuntimeException e) {
            model.addAttribute("compareError", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    /**
     * htmx: adds or removes this row's exact gemming/enchant entry and returns the clicked diamond.
     * The {@code HX-Trigger} event updates that exact row without recomputing the chart.
     */
    @PostMapping("/character/compare/availability")
    public String availability(
            @RequestParam("slot") String slotName,
            @RequestParam("gemmedId") String gemmedId,
            @RequestParam("available") boolean available,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        CharacterSlot slot = parseSlot(slotName);
        Character character = editorService.getOrCreate(session);
        ItemInstance instance;
        try {
            instance = new ItemInstance(gemmedId);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown item instance: " + gemmedId);
        }
        Item item = instance.getItem();
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown item: " + gemmedId);
        }
        if (!item.fitsInSlot(slot, character)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    item.getName() + " does not fit " + slotName);
        }
        comparisonService.setExactAvailable(character, gemmedId, available);
        editorService.markDirty(session);
        response.setHeader("HX-Trigger", "{\"compareAvailabilityChanged\":{\"itemId\":"
                + item.getId() + ",\"gemmedId\":\"" + gemmedId + "\",\"available\":"
                + available + "}}");
        model.addAttribute("slotParam", slot.name());
        model.addAttribute("gemmedId", gemmedId);
        model.addAttribute("itemId", item.getId());
        model.addAttribute("available", available);
        return "fragments/compare :: availabilityButton";
    }

    private static CharacterSlot parseSlot(String slotName) {
        CharacterSlot slot;
        try {
            slot = CharacterSlot.valueOf(slotName);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown slot: " + slotName);
        }
        if (!OptimizerJobService.OPTIMIZE_SLOTS.contains(slot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slot cannot be compared: " + slotName);
        }
        return slot;
    }

    private static Map<CharacterSlot, String> slotLabels() {
        Map<CharacterSlot, String> labels = new LinkedHashMap<CharacterSlot, String>();
        for (CharacterSlot slot : OptimizerJobService.OPTIMIZE_SLOTS) {
            labels.put(slot, EditorService.slotLabel(slot));
        }
        return labels;
    }
}
