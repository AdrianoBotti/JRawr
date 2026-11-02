package rawr.web;

import org.springframework.stereotype.Service;
import rawr.base.character.Character;
import rawr.base.items.GemmingTemplate;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemSlot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Edits the current model's gemming templates and the per-character custom templates. */
@Service
public class GemmingTemplateService {
    public record GemOption(int id, String name) {
    }

    public record TemplateRow(int index, boolean custom, String group, boolean enabled,
            String red, String yellow, String blue, String meta, String prismatic,
            int redId, int yellowId, int blueId, int metaId, int prismaticId) {
    }

    public record View(String modelName, String error, List<TemplateRow> rows,
            List<GemOption> redGems, List<GemOption> yellowGems, List<GemOption> blueGems,
            List<GemOption> metaGems, List<GemOption> prismaticGems, int topGemmings) {
        public boolean hasModel() {
            return modelName != null && !modelName.isBlank();
        }
    }

    private final CalculationService calculationService;
    private final GlobalSettingsService globalSettings;

    public GemmingTemplateService(CalculationService calculationService,
            GlobalSettingsService globalSettings) {
        this.calculationService = calculationService;
        this.globalSettings = globalSettings;
    }

    public View view(Character character) {
        String modelName = character.getCurrentModel();
        int top = globalSettings.current().topGemmings();
        if (modelName == null || modelName.isBlank()) {
            return new View(modelName, "Choose a model before editing gemming templates.",
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), top);
        }
        calculationService.ensureModelLoaded(character);
        List<TemplateRow> rows = new ArrayList<>();
        List<GemmingTemplate> defaults = character.getCurrentGemmingTemplates();
        for (int i = 0; i < defaults.size(); i++) {
            GemmingTemplate template = defaults.get(i);
            rows.add(new TemplateRow(i, false, template.getGroup(), template.getEnabled(),
                    name(template.getRedGem()), name(template.getYellowGem()),
                    name(template.getBlueGem()), name(template.getMetaGem()),
                    name(template.getPrismaticGem()), template.getRedId(), template.getYellowId(),
                    template.getBlueId(), template.getMetaId(), template.getPrismaticId()));
        }
        List<GemmingTemplate> custom = character.getCustomGemmingTemplates();
        for (int i = 0; i < custom.size(); i++) {
            GemmingTemplate template = custom.get(i);
            if (!java.util.Objects.equals(template.getModel(), modelName)) {
                continue;
            }
            rows.add(new TemplateRow(i, true, "Custom", template.getEnabled(),
                    name(template.getRedGem()), name(template.getYellowGem()),
                    name(template.getBlueGem()), name(template.getMetaGem()),
                    name(template.getPrismaticGem()), template.getRedId(), template.getYellowId(),
                    template.getBlueId(), template.getMetaId(), template.getPrismaticId()));
        }
        return new View(modelName, null, List.copyOf(rows), gemOptions("red"), gemOptions("yellow"),
                gemOptions("blue"), gemOptions("meta"), gemOptions("prismatic"), top);
    }

    public void toggle(Character character, boolean custom, int index, boolean enabled) {
        GemmingTemplate template = template(character, custom, index);
        template.setEnabled(enabled);
        changed(character);
    }

    public int addCustom(Character character) {
        String model = character.getCurrentModel();
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Choose a model first.");
        }
        GemmingTemplate template = new GemmingTemplate();
        template.setGroup("Custom");
        template.setModel(model);
        template.setEnabled(true);
        character.getCustomGemmingTemplates().add(template);
        changed(character);
        return character.getCustomGemmingTemplates().size() - 1;
    }

    public void deleteCustom(Character character, int index) {
        List<GemmingTemplate> custom = character.getCustomGemmingTemplates();
        if (index < 0 || index >= custom.size()) {
            throw new IllegalArgumentException("That template no longer exists.");
        }
        if (!java.util.Objects.equals(custom.get(index).getModel(), character.getCurrentModel())) {
            throw new IllegalArgumentException("That template belongs to another model.");
        }
        custom.remove(index);
        changed(character);
    }

    /** Creates a custom template from the footer creation form after validating every gem. */
    public void createCustom(Character character, Map<String, String> params) {
        String model = character.getCurrentModel();
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Choose a model first.");
        }
        int red = gemId(params, "redId", "red");
        int yellow = gemId(params, "yellowId", "yellow");
        int blue = gemId(params, "blueId", "blue");
        int meta = gemId(params, "metaId", "meta");
        int prismatic = gemId(params, "prismaticId", "prismatic");
        GemmingTemplate template = new GemmingTemplate();
        template.setGroup("Custom");
        template.setModel(model);
        template.setEnabled(params.containsKey("enabled")
                && ("on".equalsIgnoreCase(params.get("enabled"))
                        || "true".equalsIgnoreCase(params.get("enabled"))));
        template.setRedId(red);
        template.setYellowId(yellow);
        template.setBlueId(blue);
        template.setMetaId(meta);
        template.setPrismaticId(prismatic);
        character.getCustomGemmingTemplates().add(template);
        changed(character);
    }

    public void updateCustom(Character character, int index, Map<String, String> params) {
        GemmingTemplate template = template(character, true, index);
        if (!java.util.Objects.equals(template.getModel(), character.getCurrentModel())) {
            throw new IllegalArgumentException("That template belongs to another model.");
        }
        template.setRedId(gemId(params, "redId", "red"));
        template.setYellowId(gemId(params, "yellowId", "yellow"));
        template.setBlueId(gemId(params, "blueId", "blue"));
        template.setMetaId(gemId(params, "metaId", "meta"));
        template.setPrismaticId(gemId(params, "prismaticId", "prismatic"));
        template.setEnabled(params.containsKey("enabled")
                && ("on".equalsIgnoreCase(params.get("enabled"))
                        || "true".equalsIgnoreCase(params.get("enabled"))));
        changed(character);
    }

    private GemmingTemplate template(Character character, boolean custom, int index) {
        List<GemmingTemplate> list = custom ? character.getCustomGemmingTemplates()
                : character.getCurrentGemmingTemplates();
        if (index < 0 || index >= list.size()) {
            throw new IllegalArgumentException("That template no longer exists.");
        }
        return list.get(index);
    }

    private static int gemId(Map<String, String> params, String key, String color) {
        String raw = params.get(key);
        int id = 0;
        if (raw != null && !raw.isBlank()) {
            try {
                id = Integer.parseInt(raw.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Choose a valid gem.");
            }
        }
        if (id == 0) {
            return 0;
        }
        Item item = ItemCache.findItemById(id);
        if (item == null || !isColor(item, color)) {
            throw new IllegalArgumentException("Item " + id + " is not a " + color + " gem.");
        }
        return id;
    }

    private static boolean isColor(Item item, String color) {
        return switch (color) {
            case "red" -> item.isRedGem();
            case "yellow" -> item.isYellowGem();
            case "blue" -> item.isBlueGem();
            case "meta" -> item.getSlot() == ItemSlot.Meta;
            default -> item.isGem();
        };
    }

    private static List<GemOption> gemOptions(String color) {
        List<GemOption> options = new ArrayList<>();
        options.add(new GemOption(0, "None"));
        for (Item item : ItemCache.allItems()) {
            if (item.getName() != null && isColor(item, color)) {
                options.add(new GemOption(item.getId(), item.getName()));
            }
        }
        options.subList(1, options.size()).sort(Comparator.comparing(GemOption::name,
                String.CASE_INSENSITIVE_ORDER).thenComparingInt(GemOption::id));
        return List.copyOf(options);
    }

    private static String name(Item item) {
        return item == null ? "None" : item.getName();
    }

    private static void changed(Character character) {
        character.onCalculationsInvalidated();
    }
}
