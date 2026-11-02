package rawr.web;

import org.springframework.stereotype.Service;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.data.ItemCostStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** User-entered item purchase costs used by the optimizer's [Cost] calculation/requirements. */
@Service
public class ItemCostService {
    private static final int MAX_ROWS = 200;

    public record CostRow(int itemId, String name, int itemLevel, float cost, String costText) {
    }

    public record View(String query, List<CostRow> rows, int total, String error) {
    }

    private final ItemCostStore store;
    private final CalculationService calculationService;

    public ItemCostService(ItemCostStore store, CalculationService calculationService) {
        this.store = store;
        this.calculationService = calculationService;
    }

    public View view(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Item> matches = new ArrayList<>();
        for (Item item : ItemCache.allItems()) {
            if (item.getName() == null) continue;
            if (needle.isEmpty() ? item.getCost() > 0f
                    : item.getName().toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(item);
            }
        }
        matches.sort(needle.isEmpty()
                ? Comparator.comparingDouble(Item::getCost).reversed()
                        .thenComparing(Item::getName, String.CASE_INSENSITIVE_ORDER)
                : Comparator.comparingInt(Item::getItemLevel).reversed()
                        .thenComparing(Item::getName, String.CASE_INSENSITIVE_ORDER));
        int total = matches.size();
        if (matches.size() > MAX_ROWS) {
            matches = matches.subList(0, MAX_ROWS);
        }
        List<CostRow> rows = new ArrayList<>(matches.size());
        for (Item item : matches) {
            rows.add(new CostRow(item.getId(), item.getName(), item.getItemLevel(), item.getCost(),
                    item.getCost() > 0f ? Float.toString(item.getCost()) : ""));
        }
        return new View(query, List.copyOf(rows), total, null);
    }

    public void setCost(int itemId, float cost) {
        calculationService.withCalculationLock(() -> {
            Item item = ItemCache.findItemById(itemId);
            if (item == null) {
                throw new IllegalArgumentException("Item " + itemId + " is not in the item cache.");
            }
            if (!Float.isFinite(cost) || cost < 0f) {
                throw new IllegalArgumentException("Cost must be a non-negative number.");
            }
            store.setCost(item, cost);
            store.save();
            ItemCache.onItemsChanged();
            return null;
        });
    }

    public void resetAll() {
        calculationService.withCalculationLock(() -> {
            store.reset();
            ItemCache.onItemsChanged();
            return null;
        });
    }
}
