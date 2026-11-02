package rawr.web;

import org.springframework.stereotype.Service;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.calculations.ICharacterCalculationOptions;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Model-aware rows for the item picker. It keeps one row per item (the first relevant gemming
 * template is the representative instance), rates the visible rows against the equipped item and
 * exposes the variant count; the persistent comparison chart remains the place to inspect every
 * gemming/enchant variant.
 */
@Service
public class ItemPickerService {
    private static final int MAX_ROWS = 100;

    public record Row(int itemId, String gemmedId, String name, String qualityKey, int itemLevel,
            String iconPath, String iconUrl, String initials, String meta, boolean equipped,
            boolean available, boolean relevant, boolean added, int variants,
            float overall, String overallText, String state) {
    }

    public record View(String slotParam, String slotLabel, String query, boolean relevantOnly,
            String sort, String modelName, String ratingLabel, List<Row> rows, int totalCandidates,
            String error) {
        public boolean hasRatings() {
            return modelName != null && !modelName.isBlank();
        }
    }

    private final CalculationService calculationService;
    private final ItemIconService icons;

    public ItemPickerService(CalculationService calculationService, ItemIconService icons) {
        this.calculationService = calculationService;
        this.icons = icons;
    }

    public View search(Character character, CharacterSlot slot, String query, boolean relevantOnly,
            String sort, Integer addedItemId) {
        String modelName = character.getCurrentModel();
        String selectedSort = normalizeSort(sort);
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return calculationService.withCalculationLock(() -> {
            CalculationsBase model = null;
            String error = null;
            if (modelName != null && !modelName.isBlank()) {
                try {
                    calculationService.ensureModelLoaded(character);
                    model = Calculations.loadFreshModel(modelName);
                    if (character.getCalculationOptions() == null) {
                        character.setCalculationOptions(
                                CalculationService.createDefaultOptions(modelName));
                    }
                    if (character.getCalculationOptions()
                            instanceof ICharacterCalculationOptions bound) {
                        bound.setCharacter(character);
                    }
                    character.recalculateSetBonuses();
                    model.clearCache();
                } catch (RuntimeException e) {
                    model = null;
                    error = "Ratings unavailable: " + shortMessage(e);
                }
            }

            Map<Integer, Candidate> candidates = candidates(character, slot, relevantOnly, model,
                    addedItemId);
            List<Candidate> filtered = new ArrayList<>();
            for (Candidate candidate : candidates.values()) {
                if (needle.isEmpty()
                        || candidate.item.getName().toLowerCase(Locale.ROOT).contains(needle)) {
                    filtered.add(candidate);
                }
            }
            int total = filtered.size();
            if ("name".equals(selectedSort)) {
                filtered.sort(Comparator.comparing((Candidate c) -> c.item.getName(),
                        String.CASE_INSENSITIVE_ORDER).thenComparingInt(c -> c.item.getId()));
            } else {
                filtered.sort(Comparator.comparingInt((Candidate c) -> c.item.getItemLevel())
                        .reversed().thenComparingInt(c -> c.item.getId()));
            }
            List<Candidate> limited = filtered.size() > MAX_ROWS
                    ? new ArrayList<>(filtered.subList(0, MAX_ROWS)) : new ArrayList<>(filtered);
            if (addedItemId != null) {
                Candidate added = candidates.get(addedItemId);
                if (added != null && !limited.contains(added)) {
                    if (limited.size() >= MAX_ROWS) {
                        limited.remove(limited.size() - 1);
                    }
                    limited.add(added);
                }
            }

            Float equippedOverall = null;
            if (model != null && !limited.isEmpty()) {
                try {
                    ItemInstance equipped = character.get(slot);
                    if (equipped != null && equipped.getItem() != null) {
                        ComparisonCalculationBase baseline = model.getItemCalculations(
                                equipped, character, slot);
                        if (baseline != null) {
                            equippedOverall = baseline.getOverallPoints();
                        }
                    }
                    for (Candidate candidate : limited) {
                        ComparisonCalculationBase comparison = model.getItemCalculations(
                                candidate.instance, character, slot);
                        if (comparison != null) {
                            candidate.overall = comparison.getOverallPoints();
                            candidate.delta = candidate.overall
                                    - (equippedOverall == null ? 0f : equippedOverall);
                            candidate.rated = true;
                        }
                    }
                } catch (RuntimeException e) {
                    error = "Ratings unavailable: " + shortMessage(e);
                }
            }
            if ("overall".equals(selectedSort) && model != null) {
                limited.sort(Comparator.comparingDouble((Candidate c) -> c.rated ? c.delta : -Float.MAX_VALUE)
                        .reversed().thenComparingInt(c -> c.item.getId()));
            }

            String equippedId = character.get(slot) == null ? null
                    : character.get(slot).getGemmedId();
            List<Row> rows = new ArrayList<>(limited.size());
            for (Candidate candidate : limited) {
                Item item = candidate.item;
                boolean equipped = equippedId != null && equippedId.equals(candidate.instance.getGemmedId());
                boolean available = isAvailable(character, item, candidate.instance);
                boolean relevant = model != null && model.isItemRelevant(item);
                String meta = "ilvl " + item.getItemLevel();
                if (candidate.variants > 1) {
                    meta += " · " + candidate.variants + " gemmings";
                }
                if (equipped) {
                    meta += " · equipped";
                }
                if (available) {
                    meta += " · in pool";
                }
                if (model != null && !relevant) {
                    meta += " · not model-relevant";
                }
                rows.add(new Row(
                        item.getId(),
                        candidate.instance.getGemmedId(),
                        item.getName(),
                        item.getQuality().name().toLowerCase(Locale.ROOT),
                        item.getItemLevel(),
                        item.getIconPath(),
                        icons.url(item.getIconPath()),
                        icons.initials(item.getName()),
                        meta,
                        equipped,
                        available,
                        relevant,
                        addedItemId != null && addedItemId == item.getId(),
                        candidate.variants,
                        candidate.delta,
                        signed(candidate.rated ? candidate.delta : 0f),
                        state(candidate.rated ? candidate.delta : 0f)));
            }
            String ratingLabel = model == null ? null : model.getOverallRatingLabel();
            return new View(slot.name(), EditorService.slotLabel(slot), query, relevantOnly,
                    selectedSort, modelName, ratingLabel, List.copyOf(rows), total, error);
        });
    }

    private static Map<Integer, Candidate> candidates(Character character, CharacterSlot slot,
            boolean relevantOnly, CalculationsBase model, Integer addedItemId) {
        Map<Integer, Candidate> candidates = new LinkedHashMap<>();
        if (relevantOnly && model != null) {
            for (ItemInstance instance : character.getRelevantItemInstances(slot)) {
                if (instance == null || instance.getItem() == null) {
                    continue;
                }
                Item item = instance.getItem();
                Candidate candidate = candidates.get(item.getId());
                if (candidate == null) {
                    candidates.put(item.getId(), new Candidate(item, instance));
                } else {
                    candidate.variants++;
                }
            }
        } else {
            for (Item item : ItemCache.allItems()) {
                if (item.getName() == null) {
                    continue;
                }
                String requiredClasses = item.getRequiredClasses();
                if (character.getCharacterClass() != null && requiredClasses != null
                        && !requiredClasses.isEmpty()
                        && !requiredClasses.replace(" ", "")
                                .contains(character.getCharacterClass().name())) {
                    continue;
                }
                boolean fits = model != null ? item.fitsInSlot(slot, character) : item.fitsInSlot(slot);
                if (fits) {
                    candidates.put(item.getId(),
                            new Candidate(item, new ItemInstance(item, null, null, null, null)));
                }
            }
        }
        if (addedItemId != null && addedItemId > 0 && !candidates.containsKey(addedItemId)) {
            Item item = ItemCache.findItemById(addedItemId);
            if (item != null && item.fitsInSlot(slot)) {
                candidates.put(item.getId(),
                        new Candidate(item, new ItemInstance(item, null, null, null, null)));
            }
        }
        return candidates;
    }

    private static boolean isAvailable(Character character, Item item, ItemInstance instance) {
        List<String> entries = character.getAvailableItems();
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        String gemmedId = instance.getGemmedId();
        for (String entry : entries) {
            if (entry == null || entry.isEmpty() || entry.charAt(0) == '-') {
                continue;
            }
            if (entry.indexOf('.') < 0) {
                if (entry.equals(Integer.toString(item.getId()))) {
                    return true;
                }
            } else if (entry.equals(gemmedId)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeSort(String sort) {
        if ("name".equals(sort) || "itemLevel".equals(sort)) {
            return sort;
        }
        return "overall";
    }

    private static String signed(float value) {
        return value > 0f ? "+" + RatingFormat.format(value) : RatingFormat.format(value);
    }

    private static String state(float value) {
        if (value > 0f) return "positive";
        if (value < 0f) return "negative";
        return "neutral";
    }

    private static String shortMessage(Throwable e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        message = message.replaceAll("\\s+", " ").trim();
        return message.length() > 160 ? message.substring(0, 160) + "..." : message;
    }

    private static final class Candidate {
        private final Item item;
        private final ItemInstance instance;
        private int variants = 1;
        private float overall;
        private float delta;
        private boolean rated;

        private Candidate(Item item, ItemInstance instance) {
            this.item = item;
            this.instance = instance;
        }
    }
}
