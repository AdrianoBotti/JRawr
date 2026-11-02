package rawr.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.calculations.GreedyOptimizationMethod;
import rawr.base.calculations.OptimizationMethod;
import rawr.base.calculations.OptimizationRequirement;
import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.character.Profession;
import rawr.base.items.Enchant;
import rawr.base.items.Item;
import rawr.base.items.ItemInstance;
import rawr.base.talents.TalentsBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Optimizer job endpoints (Phase 8): start/cancel a background run, poll its status, stream
 * progress over SSE and fetch the editor swap when it finishes. The heavy lifting and the
 * per-session job cap live in {@link OptimizerJobService}.
 */
@Controller
public class OptimizerController {
    /** One labelled stop of the search-effort slider. */
    record ThoroughnessLevel(int value, String label) {
    }

    static final List<ThoroughnessLevel> THOROUGHNESS_LEVELS = List.of(
            new ThoroughnessLevel(1, "Minimal"),
            new ThoroughnessLevel(50, "Quick"),
            new ThoroughnessLevel(150, "Balanced"),
            new ThoroughnessLevel(275, "Thorough"),
            new ThoroughnessLevel(400, "Maximum"));
    static final int DEFAULT_THOROUGHNESS = OptimizerJobService.DEFAULT_THOROUGHNESS;

    private final OptimizerJobService optimizerJobs;
    private final EditorService editorService;
    private final EditorViews editorViews;
    private final ItemIconService itemIcons;
    private final CalculationService calculationService;
    private final CharacterStore characterStore;
    private final TalentPanelService talentPanels;
    private final UpgradeListService upgradeListService;
    private final UpgradeJobService upgradeJobs;
    private final GlobalSettingsService globalSettings;

    public OptimizerController(
            OptimizerJobService optimizerJobs,
            EditorService editorService,
            EditorViews editorViews,
            ItemIconService itemIcons,
            CalculationService calculationService,
            CharacterStore characterStore,
            TalentPanelService talentPanels,
            UpgradeListService upgradeListService,
            UpgradeJobService upgradeJobs,
            GlobalSettingsService globalSettings) {
        this.optimizerJobs = optimizerJobs;
        this.editorService = editorService;
        this.editorViews = editorViews;
        this.itemIcons = itemIcons;
        this.calculationService = calculationService;
        this.characterStore = characterStore;
        this.talentPanels = talentPanels;
        this.upgradeListService = upgradeListService;
        this.upgradeJobs = upgradeJobs;
        this.globalSettings = globalSettings;
    }

    @ModelAttribute("icons")
    public ItemIconService icons() {
        return itemIcons;
    }

    /** htmx: returns only the live optimizer form (used to leave the upgrade result view). */
    @GetMapping("/character/optimize/live")
    public String live(
            @RequestParam(name = "new", defaultValue = "false") boolean newRun,
            HttpSession session,
            Model model) {
        if (newRun) {
            optimizerJobs.dismissFinished(session);
        }
        Character character = editorService.getOrCreate(session);
        populatePanel(model, session, character);
        return "fragments/optimize :: optimizeLive";
    }

    /** htmx: restores the optimizer output panel after a gear-only upgrade view. */
    @GetMapping("/character/optimize/output")
    public String output(HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        populatePanel(model, session, character);
        return "fragments/optimize :: optimizeOutput";
    }

    /** htmx: start a job (at most one per session) and return the live status area. */
    @PostMapping("/character/optimize")
    public String start(
            @RequestParam(name = "thoroughness", defaultValue = "150") int thoroughness,
            @RequestParam(name = "itemsPerSlot", defaultValue = "0") int itemsPerSlot,
            @RequestParam(name = "slot", required = false) List<String> slotNames,
            @RequestParam(name = "calculation", required = false) String calculation,
            @RequestParam(name = "overrideRegem", defaultValue = "false") boolean overrideRegem,
            @RequestParam(name = "overrideReenchant", defaultValue = "false") boolean overrideReenchant,
            @RequestParam(name = "templateGemsEnabled", defaultValue = "false") boolean templateGemsEnabled,
            @RequestParam(name = "optimizeFood", defaultValue = "false") boolean optimizeFood,
            @RequestParam(name = "optimizeElixirs", defaultValue = "false") boolean optimizeElixirs,
            @RequestParam(name = "talentMode", required = false) String talentMode,
            @RequestParam(name = "optimizeTalents", defaultValue = "false") boolean optimizeTalents,
            @RequestParam(name = "mutateTalents", defaultValue = "false") boolean mutateTalents,
            @RequestParam(name = "talentsOnly", defaultValue = "false") boolean talentsOnly,
            @RequestParam(name = "poolMode", defaultValue = "ALL_RELEVANT") String poolMode,
            @RequestParam(name = "method", defaultValue = "GeneticAlgorithm") String method,
            @RequestParam(name = "greedy", defaultValue = "AllCombinations") String greedy,
            @RequestParam(name = "requirementCalculation", required = false) List<String> requirementCalculations,
            @RequestParam(name = "requirementDirection", required = false) List<String> requirementDirections,
            @RequestParam(name = "requirementValue", required = false) List<String> requirementValues,
            HttpServletRequest request,
            HttpSession session,
            HttpServletResponse response,
            Model model) {
        Character character = editorService.getOrCreate(session);
        rememberSettings(session, request);
        boolean mixology = character.hasProfession(Profession.Alchemy);
        if (talentMode != null) {
            switch (talentMode) {
                case "spec" -> {
                    optimizeTalents = true;
                    mutateTalents = false;
                }
                case "mutate" -> {
                    optimizeTalents = true;
                    mutateTalents = true;
                }
                default -> {
                    optimizeTalents = false;
                    mutateTalents = false;
                }
            }
        }
        try {
            List<CharacterSlot> slots = parseSlots(slotNames);
            String target = calculation == null || calculation.isBlank() ? "[Overall]" : calculation;
            OptimizationMethod optimizationMethod = OptimizationMethod.valueOf(method);
            GreedyOptimizationMethod greedyMethod = GreedyOptimizationMethod.valueOf(greedy);
            List<OptimizationRequirement> requirements = parseRequirements(
                    requirementCalculations, requirementDirections, requirementValues);
            GlobalSettings settings = globalSettings.current();
            int maxThreads = settings.useMultithreading() ? settings.maxThreads() : 1;
            OptimizerJobService.OptimizerParams params = new OptimizerJobService.OptimizerParams(
                    thoroughness, slots, itemsPerSlot,
                    maxThreads, target, requirements,
                    overrideRegem, overrideReenchant, templateGemsEnabled,
                    optimizeFood, optimizeElixirs, mixology,
                    "ALL_RELEVANT".equals(poolMode), optimizationMethod, greedyMethod,
                    optimizeTalents, mutateTalents, talentsOnly,
                    positiveCostItems(character, target, requirements));
            optimizerJobs.start(session, character, params);
            String previousTarget = character.getCalculationToOptimize();
            String effectivePreviousTarget = previousTarget == null || previousTarget.isBlank()
                    ? "[Overall]" : previousTarget;
            boolean targetChanged = !target.equals(effectivePreviousTarget);
            boolean requirementsChanged = !requirementsEqual(
                    character.getOptimizationRequirements(), requirements);
            if (targetChanged) {
                character.setCalculationToOptimize(target);
            }
            if (requirementsChanged) {
                character.setOptimizationRequirements(requirements);
            }
            if (targetChanged || requirementsChanged) {
                editorService.markDirty(session);
                response.setHeader("HX-Trigger", "{\"jrawrDirty\":true}");
            }
        } catch (RuntimeException e) {
            model.addAttribute("optimizeError", e.getMessage());
        }
        populatePanel(model, session, character);
        return "fragments/optimize :: optimizeLive";
    }

    /**
     * htmx: runs the optimizer's per-item gemming search and returns the per-slot upgrade list.
     * This is a synchronous, bounded-pool operation (the full asynchronous job is B-013).
     */
    @PostMapping("/character/upgrades")
    public String buildUpgrades(
            @RequestParam(name = "thoroughness", defaultValue = "150") int thoroughness,
            @RequestParam(name = "slot", required = false) List<String> slotNames,
            @RequestParam(name = "calculation", required = false) String calculation,
            @RequestParam(name = "overrideRegem", defaultValue = "false") boolean overrideRegem,
            @RequestParam(name = "overrideReenchant", defaultValue = "false") boolean overrideReenchant,
            @RequestParam(name = "templateGemsEnabled", defaultValue = "false") boolean templateGemsEnabled,
            @RequestParam(name = "poolMode", defaultValue = "ALL_RELEVANT") String poolMode,
            @RequestParam(name = "method", defaultValue = "GeneticAlgorithm") String method,
            @RequestParam(name = "greedy", defaultValue = "AllCombinations") String greedy,
            @RequestParam(name = "requirementCalculation", required = false) List<String> requirementCalculations,
            @RequestParam(name = "requirementDirection", required = false) List<String> requirementDirections,
            @RequestParam(name = "requirementValue", required = false) List<String> requirementValues,
            @RequestParam(name = "upgradeItemsPerSlot", defaultValue = "5") int upgradeItemsPerSlot,
            @RequestParam(name = "upgradeThoroughness", defaultValue = "1") int upgradeThoroughness,
            HttpServletRequest request,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        rememberSettings(session, request);
        try {
            UpgradeListService.Options options = upgradeOptions(character, slotNames, calculation,
                    overrideRegem, overrideReenchant, templateGemsEnabled, poolMode, method, greedy,
                    requirementCalculations, requirementDirections, requirementValues,
                    upgradeItemsPerSlot, upgradeThoroughness);
            UpgradeJobService.Job job = upgradeJobs.start(session, character, options);
            model.addAttribute("upgradeStatus", job.status());
        } catch (RuntimeException e) {
            model.addAttribute("upgradeError", message(e));
        }
        return "fragments/upgrades :: upgradeProgress";
    }

    /** Polled by the upgrade progress fragment until the background job finishes. */
    @GetMapping("/character/upgrades/status")
    public String upgradeStatus(HttpSession session, Model model) {
        UpgradeJobService.Job job = upgradeJobs.currentJob(session).orElse(null);
        if (job == null) {
            model.addAttribute("upgradeError", "No upgrade list is running.");
            return "fragments/upgrades :: upgradeResultView";
        }
        if (job.active()) {
            model.addAttribute("upgradeStatus", job.status());
            return "fragments/upgrades :: upgradeProgress";
        }
        if ("done".equals(job.state()) && job.result() != null) {
            model.addAttribute("upgradeResult", job.result());
        } else if ("cancelled".equals(job.state())) {
            model.addAttribute("upgradeError", "Upgrade list cancelled.");
        } else {
            model.addAttribute("upgradeError", job.error() != null
                    ? job.error() : "The upgrade list failed.");
        }
        return "fragments/upgrades :: upgradeResultView";
    }

    /** Cooperative cancel for the background upgrade list. */
    @PostMapping("/character/upgrades/cancel")
    public String cancelUpgrades(HttpSession session, Model model) {
        upgradeJobs.cancel(session);
        UpgradeJobService.Job job = upgradeJobs.currentJob(session).orElse(null);
        if (job != null && job.active()) {
            model.addAttribute("upgradeStatus", job.status());
        } else if (job != null && "done".equals(job.state()) && job.result() != null) {
            model.addAttribute("upgradeResult", job.result());
            return "fragments/upgrades :: upgradeResultView";
        } else {
            model.addAttribute("upgradeError", "Upgrade list cancelled.");
            return "fragments/upgrades :: upgradeResultView";
        }
        return "fragments/upgrades :: upgradeProgress";
    }

    /** htmx: evaluates one item id or exact gemmed id. */
    @PostMapping("/character/upgrades/evaluate")
    public String evaluateUpgrade(
            @RequestParam(name = "thoroughness", defaultValue = "150") int thoroughness,
            @RequestParam(name = "slot", required = false) List<String> slotNames,
            @RequestParam(name = "calculation", required = false) String calculation,
            @RequestParam(name = "overrideRegem", defaultValue = "false") boolean overrideRegem,
            @RequestParam(name = "overrideReenchant", defaultValue = "false") boolean overrideReenchant,
            @RequestParam(name = "templateGemsEnabled", defaultValue = "false") boolean templateGemsEnabled,
            @RequestParam(name = "poolMode", defaultValue = "ALL_RELEVANT") String poolMode,
            @RequestParam(name = "method", defaultValue = "GeneticAlgorithm") String method,
            @RequestParam(name = "greedy", defaultValue = "AllCombinations") String greedy,
            @RequestParam(name = "requirementCalculation", required = false) List<String> requirementCalculations,
            @RequestParam(name = "requirementDirection", required = false) List<String> requirementDirections,
            @RequestParam(name = "requirementValue", required = false) List<String> requirementValues,
            @RequestParam(name = "upgradeItemsPerSlot", defaultValue = "5") int upgradeItemsPerSlot,
            @RequestParam(name = "upgradeThoroughness", defaultValue = "1") int upgradeThoroughness,
            @RequestParam(name = "upgradeItem", required = false) String upgradeItem,
            HttpServletRequest request,
            HttpSession session,
            Model model) {
        Character character = editorService.getOrCreate(session);
        rememberSettings(session, request);
        try {
            UpgradeListService.Options options = upgradeOptions(character, slotNames, calculation,
                    overrideRegem, overrideReenchant, templateGemsEnabled, poolMode, method, greedy,
                    requirementCalculations, requirementDirections, requirementValues,
                    upgradeItemsPerSlot, upgradeThoroughness);
            model.addAttribute("upgradeEvaluation",
                    upgradeListService.evaluate(character, options, upgradeItem));
        } catch (RuntimeException e) {
            model.addAttribute("upgradeError", message(e));
        }
        return "fragments/upgrades :: upgradeEvaluationView";
    }

    private UpgradeListService.Options upgradeOptions(Character character,
            List<String> slotNames, String calculation, boolean overrideRegem,
            boolean overrideReenchant, boolean templateGemsEnabled, String poolMode,
            String method, String greedy, List<String> requirementCalculations,
            List<String> requirementDirections, List<String> requirementValues,
            int upgradeItemsPerSlot, int upgradeThoroughness) {
        List<CharacterSlot> slots = parseSlots(slotNames);
        if (slots.isEmpty()) {
            throw new IllegalStateException("Select at least one slot to evaluate.");
        }
        String target = calculation == null || calculation.isBlank() ? "[Overall]" : calculation;
        List<OptimizationRequirement> requirements = parseRequirements(
                requirementCalculations, requirementDirections, requirementValues);
        OptimizationMethod optimizationMethod = OptimizationMethod.valueOf(method);
        GreedyOptimizationMethod greedyMethod = GreedyOptimizationMethod.valueOf(greedy);
        return new UpgradeListService.Options(
                Math.max(1, Math.min(400, upgradeThoroughness)), slots,
                Math.max(0, upgradeItemsPerSlot), target, requirements, overrideRegem,
                overrideReenchant, templateGemsEnabled, "ALL_RELEVANT".equals(poolMode),
                optimizationMethod, greedyMethod);
    }

    private static String message(RuntimeException e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    /** Polling fallback for browsers without EventSource support. */
    @GetMapping("/character/optimize/status")
    @ResponseBody
    public OptimizerJobService.JobStatus status(HttpSession session) {
        return optimizerJobs.currentJob(session)
                .map(OptimizerJobService.Job::status)
                .orElseGet(OptimizerJobService.JobStatus::idle);
    }

    /** SSE stream of {@code status} events and the terminal {@code done} event. */
    @GetMapping(path = "/character/optimize/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpSession session) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        optimizerJobs.attach(session, emitter);
        return emitter;
    }

    /** htmx: cooperative cancel; returns the live status area. */
    @PostMapping("/character/optimize/cancel")
    public String cancel(HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        optimizerJobs.cancel(session);
        populatePanel(model, session, character);
        return "fragments/optimize :: optimizeLive";
    }

    /** htmx: applies the last finished result after the user confirms it. */
    @PostMapping("/character/optimize/apply")
    public String apply(HttpSession session, HttpServletResponse response, Model model) {
        Character character = editorService.getOrCreate(session);
        if (!optimizerJobs.apply(session)) {
            model.addAttribute("optimizeError", "No finished optimizer result is available to apply.");
            populatePanel(model, session, character);
            return "fragments/optimize :: optimizeLive";
        }
        response.setHeader("HX-Redirect", "/");
        return "fragments/optimize :: applied";
    }

    /**
     * Saves the finished optimizer setup as a separate character file. The current session
     * character is only used as the identity/options base and is not modified.
     */
    @PostMapping("/character/optimize/save-as")
    public String saveOptimizedAs(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "confirm", defaultValue = "false") boolean confirm,
            HttpSession session,
            Model model) {
        Character current = editorService.getOrCreate(session);
        Character optimized = optimizerJobs.optimizedSaveCopy(session, current);
        if (optimized == null) {
            model.addAttribute("saveResultError",
                    "No finished optimizer result is available to save.");
            populatePanel(model, session, current);
            return "fragments/optimize :: optimizeLive";
        }
        String fileName = CharacterStore.sanitizeSaveAsName(name);
        if (fileName == null) {
            model.addAttribute("saveResultName", CharacterStore.saveAsInputBase(name));
            model.addAttribute("saveResultError", "Enter a valid file name.");
            populatePanel(model, session, current);
            return "fragments/optimize :: optimizeLive";
        }
        if (!confirm && characterStore.exists(fileName)) {
            model.addAttribute("saveResultName", CharacterStore.saveAsInputBase(name));
            model.addAttribute("saveResultFile", fileName);
            model.addAttribute("saveResultConfirm", true);
            populatePanel(model, session, current);
            return "fragments/optimize :: optimizeLive";
        }
        try {
            characterStore.write(fileName, optimized, optimized.getCurrentModel());
            model.addAttribute("saveResultName", characterStore.defaultName(current) + "-optimized");
            model.addAttribute("saveResultMessage", "Saved optimized setup as " + fileName + ".");
        } catch (IOException | RuntimeException e) {
            model.addAttribute("saveResultName", CharacterStore.saveAsInputBase(name));
            model.addAttribute("saveResultError",
                    "Could not save the optimized setup: " + e.getMessage());
        }
        populatePanel(model, session, current);
        return "fragments/optimize :: optimizeLive";
    }

    /**
     * Compatibility endpoint retained for the old result flow. It re-renders the paper doll (and,
     * out of band, the stats panel) from the session character; use /character/optimize/apply to
     * load a finished result.
     */
    @GetMapping("/character/optimize/result")
    public String result(HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        editorViews.populateEditor(model, character);
        return "fragments/optimize :: resultSwap";
    }

    // ------------------------------------------------------------------ helpers

    private void populatePanel(Model model, HttpSession session, Character character) {
        OptimizerJobService.JobStatus status = optimizerJobs.currentJob(session)
                .map(OptimizerJobService.Job::status)
                .orElse(null);
        model.addAttribute("jobStatus", status);
        model.addAttribute("optimizeSummary", summaryOf(status));
        model.addAttribute("optimizeSlots", OptimizerJobService.OPTIMIZE_SLOTS);
        model.addAttribute("slotLabels", slotLabels());
        OptimizerSettings settings = optimizerSettings(session, character);
        int defaultLevel = thoroughnessLevel(settings.thoroughness());
        model.addAttribute("optimizerSettings", settings);
        model.addAttribute("selectedSlots", settings.slots());
        model.addAttribute("selectedTalentMode", settings.talentMode());
        model.addAttribute("thoroughnessLevels", THOROUGHNESS_LEVELS);
        model.addAttribute("thoroughnessValues", thoroughnessValues());
        model.addAttribute("defaultThoroughnessLevel", defaultLevel);
        model.addAttribute("defaultThoroughnessLabel", THOROUGHNESS_LEVELS.get(defaultLevel - 1).label());
        model.addAttribute("defaultThoroughness", settings.thoroughness());
        List<EditorViews.OptionView> calculationChoices = calculationChoices(character);
        model.addAttribute("calculationChoices", calculationChoices);
        model.addAttribute("selectedCalculation", settings.calculation());
        model.addAttribute("optimizationMethods", OptimizationMethod.values());
        model.addAttribute("greedyMethods", GreedyOptimizationMethod.values());
        model.addAttribute("optimizerRequirements", settings.requirements());
        List<String> availability = character.getAvailableItems();
        model.addAttribute("availabilityCount", availability == null ? 0 : availability.size());
        model.addAttribute("talentSetup", talentPanels.setupSummary(character));
        model.addAttribute("alchemist", character.hasProfession(Profession.Alchemy));

        OptimizerJobService.FinishedResult finished = optimizerJobs.finishedResult(session).orElse(null);
        if (finished != null) {
            calculationService.ensureModelLoaded(character);
            model.addAttribute("optimizerResult",
                    optimizerResultView(finished, character, calculationChoices));
            if (!model.containsAttribute("saveResultName")) {
                model.addAttribute("saveResultName", characterStore.defaultName(character) + "-optimized");
            }
        }
    }

    /** View data for one side (before or after) of a changed slot in the result preview. */
    public record ItemSideView(
            boolean empty,
            String name,
            String qualityKey,
            String iconUrl,
            String initials,
            String metaText,
            List<String> gemColors) {
    }

    public record ItemChangeView(
            String slotLabel,
            ItemSideView before,
            ItemSideView after,
            String valueText,
            String valueState) {
    }

    public record ConsumableItemView(String name, String statsText) {
    }

    public record RunSettingView(String label, String value) {
    }

    public record ConsumableGroupView(
            String label,
            List<ConsumableItemView> added,
            List<ConsumableItemView> removed) {
        public boolean hasChanges() {
            return !added.isEmpty() || !removed.isEmpty();
        }
    }

    public record OptimizerResultView(
            String targetLabel,
            String beforeText,
            String afterText,
            String deltaText,
            int changedCount,
            List<RunSettingView> runSettings,
            List<ItemChangeView> changes,
            List<ConsumableGroupView> consumableGroups,
            boolean talentsChanged,
            String beforeTalents,
            String afterTalents,
            String beforeTalentSpec,
            String afterTalentSpec,
            List<TalentPanelService.TalentChange> talentChanges,
            List<TalentPanelService.GlyphChange> glyphChanges) {

        public boolean hasBuffChanges() {
            return consumableGroups.stream().anyMatch(ConsumableGroupView::hasChanges);
        }
    }

    private OptimizerResultView optimizerResultView(
            OptimizerJobService.FinishedResult finished,
            Character character,
            List<EditorViews.OptionView> calculationChoices) {
        List<ItemChangeView> changes = new ArrayList<ItemChangeView>();
        for (int i = 0; i < Character.OptimizableSlotCount; i++) {
            CharacterSlot slot = CharacterSlot.fromValue(i);
            ItemInstance before = finished.before().get(slot);
            ItemInstance after = finished.optimized().get(slot);
            if (sameGemmed(before, after)) {
                continue;
            }
            Float slotDelta = finished.slotDeltas().get(slot);
            changes.add(new ItemChangeView(EditorService.slotLabel(slot),
                    itemSide(before, character), itemSide(after, character),
                    slotValueText(slotDelta), slotValueState(slotDelta)));
        }
        List<ConsumableGroupView> consumableGroups =
                consumableGroups(finished.before(), finished.optimized());
        float delta = finished.afterPoints() - finished.beforePoints();
        String beforeTalentSpec = talentSpec(finished.before().getCurrentTalents());
        String afterTalentSpec = talentSpec(finished.optimized().getCurrentTalents());
        TalentPanelService.TalentDiff talentDiff = talentPanels.compareTalents(
                finished.before().getCurrentTalents(), finished.optimized().getCurrentTalents());
        return new OptimizerResultView(
                calculationLabel(finished.calculation(), calculationChoices),
                RatingFormat.format(finished.beforePoints()),
                RatingFormat.format(finished.afterPoints()),
                signedDelta(delta),
                changes.size(),
                runSettings(finished, calculationChoices),
                List.copyOf(changes),
                consumableGroups,
                !Objects.equals(beforeTalentSpec, afterTalentSpec),
                talentSummary(finished.before().getCurrentTalents()),
                talentSummary(finished.optimized().getCurrentTalents()),
                beforeTalentSpec,
                afterTalentSpec,
                talentDiff.talents(),
                talentDiff.glyphs());
    }

    /** A compact record of the settings that produced this result. */
    private static List<RunSettingView> runSettings(
            OptimizerJobService.FinishedResult finished,
            List<EditorViews.OptionView> calculationChoices) {
        OptimizerJobService.OptimizerParams params = finished.params();
        if (params == null) {
            return List.of();
        }
        List<RunSettingView> settings = new ArrayList<RunSettingView>();
        settings.add(new RunSettingView("Target",
                calculationLabel(finished.calculation(), calculationChoices)));

        int slotCount = params.slots() == null ? 0 : params.slots().size();
        int allSlots = OptimizerJobService.OPTIMIZE_SLOTS.size();
        settings.add(new RunSettingView("Slots", slotCount >= allSlots
                ? "All " + allSlots + " slots"
                : slotCount + " of " + allSlots + " slots"));

        StringBuilder pool = new StringBuilder(params.poolAllRelevant()
                ? "All relevant items" : "Custom item pool");
        if (params.itemsPerSlot() > 0) {
            pool.append(" \u00b7 ").append(params.itemsPerSlot()).append(" per slot");
        }
        settings.add(new RunSettingView("Pool", pool.toString()));

        List<String> overrides = new ArrayList<String>();
        if (params.overrideRegem()) {
            overrides.add("all gems");
        }
        if (params.overrideReenchant()) {
            overrides.add("all enchants");
        }
        if (params.templateGemsEnabled()) {
            overrides.add("template gems");
        }
        settings.add(new RunSettingView("Gems & enchants",
                overrides.isEmpty() ? "Exact pool" : String.join(", ", overrides)));

        String talents;
        if (params.talentsOnly()) {
            talents = "Talent & glyph only";
        } else if (params.mutateTalents()) {
            talents = "Best spec plus point tweaks";
        } else if (params.optimizeTalents()) {
            talents = "Best existing spec";
        } else {
            talents = "No talent search";
        }
        settings.add(new RunSettingView("Talents & glyphs", talents));

        List<String> consumables = new ArrayList<String>();
        if (params.optimizeFood()) {
            consumables.add("food");
        }
        if (params.optimizeElixirs()) {
            consumables.add("elixirs/flasks");
            if (params.mixology()) {
                consumables.add("mixology");
            }
        }
        settings.add(new RunSettingView("Consumables",
                consumables.isEmpty() ? "None" : String.join(", ", consumables)));

        String method = params.optimizationMethod() == OptimizationMethod.SimulatedAnnealing
                ? "Simulated annealing" : "Genetic algorithm";
        settings.add(new RunSettingView("Effort",
                effortLabel(params.thoroughness()) + " \u00b7 " + method));

        if (params.requirements() != null && !params.requirements().isEmpty()) {
            int count = params.requirements().size();
            settings.add(new RunSettingView("Constraints",
                    count + (count == 1 ? " rule" : " rules")));
        }
        return List.copyOf(settings);
    }

    private static String effortLabel(int thoroughness) {
        for (ThoroughnessLevel level : THOROUGHNESS_LEVELS) {
            if (level.value() == thoroughness) {
                return level.label() + " (" + thoroughness + ")";
            }
        }
        return "Custom (" + thoroughness + ")";
    }

    private ItemSideView itemSide(ItemInstance instance, Character character) {
        Item item = instance == null ? null : instance.getItem();
        if (item == null) {
            return new ItemSideView(true, "Empty", null, null, "", "", List.of());
        }
        return new ItemSideView(false,
                item.getName(),
                item.getQuality().name().toLowerCase(Locale.ROOT),
                itemIcons.url(item.getIconPath()),
                itemIcons.initials(item.getName()),
                itemMeta(instance, character),
                GemService.appliedGemColors(instance));
    }

    private static String itemMeta(ItemInstance instance, Character character) {
        Item item = instance.getItem();
        int gems = 0;
        for (int index = 1; index <= 3; index++) {
            if (instance.getGem(index) != null) {
                gems++;
            }
        }
        StringBuilder text = new StringBuilder("ilvl ").append(item.getItemLevel());
        if (gems > 0) {
            text.append(" \u00b7 ").append(gems).append(gems == 1 ? " gem" : " gems");
        }
        if (instance.getEnchantId() != 0) {
            Enchant enchant = Enchant.findEnchant(instance.getEnchantId(), item.getSlot(), character);
            if (enchant != null && enchant.id == instance.getEnchantId()) {
                text.append(" \u00b7 ").append(enchant.name);
            }
        }
        return text.toString();
    }

    private static boolean sameGemmed(ItemInstance before, ItemInstance after) {
        if (before == null || after == null) {
            return before == after;
        }
        return before.getGemmedId().equals(after.getGemmedId());
    }

    private static List<ConsumableGroupView> consumableGroups(Character before, Character after) {
        Map<String, Buff> beforeBuffs = buffMap(before);
        Map<String, Buff> afterBuffs = buffMap(after);
        List<ConsumableItemView> foodAdded = new ArrayList<ConsumableItemView>();
        List<ConsumableItemView> foodRemoved = new ArrayList<ConsumableItemView>();
        List<ConsumableItemView> elixirAdded = new ArrayList<ConsumableItemView>();
        List<ConsumableItemView> elixirRemoved = new ArrayList<ConsumableItemView>();
        List<ConsumableItemView> otherAdded = new ArrayList<ConsumableItemView>();
        List<ConsumableItemView> otherRemoved = new ArrayList<ConsumableItemView>();
        for (Map.Entry<String, Buff> entry : afterBuffs.entrySet()) {
            if (!beforeBuffs.containsKey(entry.getKey())) {
                addConsumable(entry.getValue(), foodAdded, elixirAdded, otherAdded);
            }
        }
        for (Map.Entry<String, Buff> entry : beforeBuffs.entrySet()) {
            if (!afterBuffs.containsKey(entry.getKey())) {
                addConsumable(entry.getValue(), foodRemoved, elixirRemoved, otherRemoved);
            }
        }
        sortConsumables(foodAdded);
        sortConsumables(foodRemoved);
        sortConsumables(elixirAdded);
        sortConsumables(elixirRemoved);
        sortConsumables(otherAdded);
        sortConsumables(otherRemoved);
        List<ConsumableGroupView> groups = new ArrayList<ConsumableGroupView>();
        groups.add(new ConsumableGroupView("Food", List.copyOf(foodAdded), List.copyOf(foodRemoved)));
        groups.add(new ConsumableGroupView("Elixirs & Flasks", List.copyOf(elixirAdded),
                List.copyOf(elixirRemoved)));
        if (!otherAdded.isEmpty() || !otherRemoved.isEmpty()) {
            groups.add(new ConsumableGroupView("Other buffs", List.copyOf(otherAdded),
                    List.copyOf(otherRemoved)));
        }
        return List.copyOf(groups);
    }

    private static Map<String, Buff> buffMap(Character character) {
        Map<String, Buff> buffs = new LinkedHashMap<String, Buff>();
        for (Buff buff : character.getActiveBuffs()) {
            if (buff == null || buff.name == null || buff.name.isBlank()) {
                continue;
            }
            if (buff.setName != null && !buff.setName.isEmpty() || "Set Bonuses".equals(buff.group)) {
                continue;
            }
            buffs.putIfAbsent(buff.name, buff);
        }
        return buffs;
    }

    private static void addConsumable(Buff buff, List<ConsumableItemView> food,
            List<ConsumableItemView> elixir, List<ConsumableItemView> other) {
        ConsumableItemView view = new ConsumableItemView(buff.name, buffStats(buff));
        String group = buff.group == null ? "" : buff.group.toLowerCase(Locale.ROOT);
        if (group.contains("food")) {
            food.add(view);
        } else if (group.contains("elixir") || group.contains("flask")
                || (buff.name != null && buff.name.toLowerCase(Locale.ROOT).contains("(mixology)"))) {
            elixir.add(view);
        } else {
            other.add(view);
        }
    }

    private static String buffStats(Buff buff) {
        String stats = buff.stats == null ? "" : buff.stats.toString();
        return stats.isBlank() ? "Special effect" : stats;
    }

    private static void sortConsumables(List<ConsumableItemView> items) {
        items.sort(Comparator.comparing(ConsumableItemView::name, String.CASE_INSENSITIVE_ORDER));
    }

    private static String talentSpec(TalentsBase talents) {
        return talents == null ? "" : talents.toString();
    }

    /** Tree point totals (before the glyph section of the canonical talent string). */
    private static String talentSummary(TalentsBase talents) {
        if (talents == null) {
            return "";
        }
        String spec = talents.toString();
        int dot = spec.indexOf('.');
        String data = dot < 0 ? spec : spec.substring(0, dot);
        int[] trees = new int[3];
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            if (c >= '0' && c <= '9') {
                trees[Math.min(2, i / 29)] += c - '0';
            }
        }
        return trees[0] + "/" + trees[1] + "/" + trees[2];
    }

    private static String slotValueText(Float delta) {
        return delta == null || Float.isNaN(delta) ? "n/a" : signedDelta(delta);
    }

    private static String slotValueState(Float delta) {
        if (delta == null || Float.isNaN(delta) || Math.abs(delta) < 0.005f) {
            return "neutral";
        }
        return delta > 0 ? "positive" : "negative";
    }

    /** Formats a delta without the misleading "-0.00" produced by sub-cent float noise. */
    private static String signedDelta(float delta) {
        if (Math.abs(delta) < 0.005f) {
            delta = 0f;
        }
        return (delta >= 0 ? "+" : "") + RatingFormat.format(delta);
    }

    private static String calculationLabel(String calculation, List<EditorViews.OptionView> choices) {
        if (calculation == null || calculation.isBlank()) {
            return "Overall rating";
        }
        for (EditorViews.OptionView choice : choices) {
            if (calculation.equals(choice.value())) {
                return choice.label();
            }
        }
        return calculation;
    }

    /** Last submitted optimizer form, kept per session so a run or view does not reset the form. */
    record OptimizerSettings(
            int thoroughness,
            int itemsPerSlot,
            List<String> slots,
            String calculation,
            boolean overrideRegem,
            boolean overrideReenchant,
            boolean templateGems,
            boolean optimizeFood,
            boolean optimizeElixirs,
            boolean optimizeTalents,
            boolean mutateTalents,
            String poolMode,
            String method,
            String greedy,
            int upgradeItemsPerSlot,
            int upgradeThoroughness,
            List<OptimizationRequirement> requirements) {

        String talentMode() {
            if (mutateTalents) {
                return "mutate";
            }
            if (optimizeTalents) {
                return "spec";
            }
            return "none";
        }
    }

    private static OptimizerSettings defaultOptimizerSettings(Character character) {
        List<String> slots = new ArrayList<String>();
        for (CharacterSlot slot : OptimizerJobService.OPTIMIZE_SLOTS) {
            slots.add(slot.name());
        }
        String calculation = character.getCalculationToOptimize();
        if (calculation == null || calculation.isBlank()) {
            calculation = "[Overall]";
        }
        List<OptimizationRequirement> requirements = character.getOptimizationRequirements();
        return new OptimizerSettings(DEFAULT_THOROUGHNESS, 0, List.copyOf(slots), calculation,
                false, true, true, false, false, false, false,
                "ALL_RELEVANT", "GeneticAlgorithm", "AllCombinations", 5, 1,
                requirements == null ? List.of() : List.copyOf(requirements));
    }

    private static OptimizerSettings optimizerSettings(HttpSession session, Character character) {
        Object stored = session.getAttribute(EditorService.SESSION_OPTIMIZER_SETTINGS);
        if (stored instanceof OptimizerSettings storedSettings) {
            return storedSettings;
        }
        return defaultOptimizerSettings(character);
    }

    private static void rememberSettings(HttpSession session, HttpServletRequest request) {
        String mode = request.getParameter("talentMode");
        boolean mutateTalents;
        boolean optimizeTalents;
        if (mode == null) {
            mutateTalents = flag(request, "mutateTalents");
            optimizeTalents = flag(request, "optimizeTalents") || mutateTalents;
        } else {
            mutateTalents = "mutate".equals(mode);
            optimizeTalents = mutateTalents || "spec".equals(mode);
        }
        List<OptimizationRequirement> requirements = parseRequirements(
                paramList(request, "requirementCalculation"),
                paramList(request, "requirementDirection"),
                paramList(request, "requirementValue"));
        session.setAttribute(EditorService.SESSION_OPTIMIZER_SETTINGS, new OptimizerSettings(
                intParam(request, "thoroughness", DEFAULT_THOROUGHNESS),
                intParam(request, "itemsPerSlot", 0),
                paramList(request, "slot"),
                stringParam(request, "calculation", "[Overall]"),
                flag(request, "overrideRegem"),
                flag(request, "overrideReenchant"),
                flag(request, "templateGemsEnabled"),
                flag(request, "optimizeFood"),
                flag(request, "optimizeElixirs"),
                optimizeTalents,
                mutateTalents,
                stringParam(request, "poolMode", "ALL_RELEVANT"),
                stringParam(request, "method", "GeneticAlgorithm"),
                stringParam(request, "greedy", "AllCombinations"),
                intParam(request, "upgradeItemsPerSlot", 5),
                intParam(request, "upgradeThoroughness", 1),
                List.copyOf(requirements)));
    }

    private static String stringParam(HttpServletRequest request, String name, String fallback) {
        String value = request.getParameter(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int intParam(HttpServletRequest request, String name, int fallback) {
        String value = request.getParameter(name);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean flag(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value != null && !"false".equalsIgnoreCase(value)
                && !"off".equalsIgnoreCase(value) && !"0".equals(value);
    }

    private static List<String> paramList(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        return values == null ? List.of() : List.of(values);
    }

    private static int thoroughnessLevel(int value) {
        for (int i = 0; i < THOROUGHNESS_LEVELS.size(); i++) {
            if (THOROUGHNESS_LEVELS.get(i).value() == value) {
                return i + 1;
            }
        }
        return defaultThoroughnessLevel();
    }

    private static int defaultThoroughnessLevel() {
        for (int i = 0; i < THOROUGHNESS_LEVELS.size(); i++) {
            if (THOROUGHNESS_LEVELS.get(i).value() == DEFAULT_THOROUGHNESS) {
                return i + 1;
            }
        }
        return 1;
    }

    private static String thoroughnessValues() {
        StringBuilder values = new StringBuilder();
        for (ThoroughnessLevel level : THOROUGHNESS_LEVELS) {
            if (values.length() > 0) {
                values.append(',');
            }
            values.append(level.value());
        }
        return values.toString();
    }

    private static List<OptimizationRequirement> parseRequirements(List<String> calculations,
            List<String> directions, List<String> values) {
        if (calculations == null || calculations.isEmpty()) {
            return List.of();
        }
        if (directions == null || values == null
                || directions.size() != calculations.size() || values.size() != calculations.size()) {
            throw new IllegalStateException("Fill in every optimizer requirement row.");
        }
        List<OptimizationRequirement> requirements = new ArrayList<OptimizationRequirement>();
        for (int i = 0; i < calculations.size(); i++) {
            String calculation = calculations.get(i);
            String valueText = values.get(i);
            if (calculation == null || calculation.isBlank() || valueText == null || valueText.isBlank()) {
                throw new IllegalStateException("Fill in every optimizer requirement row.");
            }
            float value;
            try {
                value = Float.parseFloat(valueText.trim());
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Optimizer requirement values must be numbers.");
            }
            OptimizationRequirement requirement = new OptimizationRequirement();
            requirement.calculation = calculation;
            requirement.lessThan = "lte".equals(directions.get(i));
            requirement.value = value;
            requirements.add(requirement);
        }
        return requirements;
    }

    private List<EditorViews.OptionView> calculationChoices(Character character) {
        List<EditorViews.OptionView> choices = new ArrayList<EditorViews.OptionView>();
        choices.add(new EditorViews.OptionView("[Overall]", "Overall rating"));
        choices.add(new EditorViews.OptionView("[Cost]", "Item cost"));
        String modelName = character.getCurrentModel();
        if (modelName != null && CalculationService.MODELS.contains(modelName)) {
            calculationService.ensureModelLoaded(character);
            CalculationsBase model = Calculations.getModel(modelName);
            int index = 0;
            for (String label : model.getSubPointNameColors().keySet()) {
                choices.add(new EditorViews.OptionView("[SubPoint " + index + "]", label + " rating"));
                index++;
            }
            for (String label : model.getOptimizableCalculationLabels()) {
                if (label != null && !label.isBlank() && !containsValue(choices, label)) {
                    choices.add(new EditorViews.OptionView(label, label));
                }
            }
        }
        String selected = selectedCalculation(character);
        if (!containsValue(choices, selected)) {
            choices.add(new EditorViews.OptionView(selected, selected));
        }
        return List.copyOf(choices);
    }

    private static boolean containsValue(List<EditorViews.OptionView> choices, String value) {
        for (EditorViews.OptionView choice : choices) {
            if (choice.value().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean positiveCostItems(Character character, String target,
            List<OptimizationRequirement> requirements) {
        if ("[Cost]".equals(target)) {
            return true;
        }
        for (OptimizationRequirement requirement : requirements) {
            if (requirement != null && "[Cost]".equals(requirement.calculation)) {
                return true;
            }
        }
        return false;
    }

    private static boolean requirementsEqual(
            List<OptimizationRequirement> left, List<OptimizationRequirement> right) {
        List<OptimizationRequirement> a = left == null ? List.of() : left;
        List<OptimizationRequirement> b = right == null ? List.of() : right;
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            OptimizationRequirement x = a.get(i);
            OptimizationRequirement y = b.get(i);
            if (x == null || y == null) {
                if (x != y) {
                    return false;
                }
                continue;
            }
            if (!Objects.equals(x.calculation, y.calculation)
                    || x.lessThan != y.lessThan
                    || Float.compare(x.value, y.value) != 0) {
                return false;
            }
        }
        return true;
    }

    private static String selectedCalculation(Character character) {
        String selected = character.getCalculationToOptimize();
        return selected == null || selected.isBlank() ? "[Overall]" : selected;
    }

    private static List<CharacterSlot> parseSlots(List<String> names) {
        if (names == null || names.isEmpty()) {
            throw new IllegalStateException("Select at least one slot to optimize.");
        }
        List<CharacterSlot> slots = new ArrayList<CharacterSlot>();
        for (String name : names) {
            CharacterSlot slot;
            try {
                slot = CharacterSlot.valueOf(name);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Unknown slot: " + name);
            }
            if (!slots.contains(slot)) {
                slots.add(slot);
            }
        }
        return slots;
    }

    static String summaryOf(OptimizerJobService.JobStatus status) {
        if (status == null || !"done".equals(status.state())) {
            return null;
        }
        float delta = status.afterPoints() - status.beforePoints();
        int changed = status.changedSlots() == null ? 0 : status.changedSlots().size();
        StringBuilder text = new StringBuilder("Optimized: ");
        text.append(RatingFormat.format(status.beforePoints())).append(" \u2192 ")
                .append(RatingFormat.format(status.afterPoints())).append(" (")
                .append(signedDelta(delta)).append(")");
        if (changed == 0) {
            text.append("; no gear slots changed");
        } else {
            List<String> labels = status.changedSlots();
            String listed = String.join(", ", labels.subList(0, Math.min(4, labels.size())));
            if (labels.size() > 4) {
                listed += ", \u2026";
            }
            text.append("; ").append(changed)
                    .append(changed == 1 ? " gear slot changed: " : " gear slots changed: ")
                    .append(listed);
        }
        return text.toString();
    }

    private static Map<CharacterSlot, String> slotLabels() {
        Map<CharacterSlot, String> labels = new LinkedHashMap<CharacterSlot, String>();
        for (CharacterSlot slot : OptimizerJobService.OPTIMIZE_SLOTS) {
            labels.put(slot, EditorService.slotLabel(slot));
        }
        return labels;
    }
}
