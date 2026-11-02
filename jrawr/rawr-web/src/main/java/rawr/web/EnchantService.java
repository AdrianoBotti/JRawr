package rawr.web;

import org.springframework.stereotype.Service;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.ItemInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Enchant selection for the paper doll. The candidate list mirrors the legacy item picker: every
 * non-zero enchant must fit the equipped item and, when a model is selected, be relevant for the
 * character's class and professions. The applied enchant id is stored on the {@link ItemInstance},
 * exactly like a saved character file.
 */
@Service
public class EnchantService {
    private final CalculationService calculationService;

    public EnchantService(CalculationService calculationService) {
        this.calculationService = calculationService;
    }

    /** Candidate enchants for the item in {@code slot}, optionally filtered by name. */
    public List<EnchantChoice> choices(Character character, CharacterSlot slot, String query) {
        ItemInstance instance = character.get(slot);
        if (instance == null || instance.getItem() == null) {
            return List.of();
        }
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return calculationService.withCalculationLock(() -> {
            CalculationsBase model = modelFor(character);
            List<EnchantChoice> choices = new ArrayList<EnchantChoice>();
            boolean enchanted = instance.getEnchantId() != 0;
            if (needle.isEmpty() || "no enchant".contains(needle)) {
                choices.add(new EnchantChoice(0, "No enchant", enchanted ? "Remove the enchant" : "No bonus",
                        !enchanted, true));
            }
            List<Enchant> matches = new ArrayList<Enchant>();
            for (Enchant enchant : Enchant.getAllEnchants()) {
                if (enchant.id == 0 || !instance.isEnchantEligible(enchant)) {
                    continue;
                }
                if (model != null && enchant.id != instance.getEnchantId()
                        && !model.isEnchantRelevant(enchant, character)) {
                    continue;
                }
                if (!needle.isEmpty() && !enchant.name.toLowerCase(Locale.ROOT).contains(needle)) {
                    continue;
                }
                matches.add(enchant);
            }
            matches.sort(Comparator.comparing((Enchant enchant) -> enchant.name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(enchant -> enchant.id));
            for (Enchant enchant : matches) {
                choices.add(new EnchantChoice(enchant.id, enchant.name, statsText(enchant),
                        enchant.id == instance.getEnchantId(), false));
            }
            return List.copyOf(choices);
        });
    }

    /** Applies {@code enchantId} to the item in {@code slot}; id 0 removes the enchant. */
    public void apply(Character character, CharacterSlot slot, int enchantId) {
        ItemInstance instance = character.get(slot);
        if (instance == null || instance.getItem() == null) {
            throw new IllegalArgumentException("No item equipped in " + EditorService.slotLabel(slot));
        }
        if (enchantId != 0) {
            Enchant enchant = find(enchantId);
            if (enchant == null) {
                throw new IllegalArgumentException("Unknown enchant: " + enchantId);
            }
            if (!instance.isEnchantEligible(enchant)) {
                throw new IllegalArgumentException("Enchant " + enchant.name + " does not fit "
                        + instance.getItem().getName());
            }
        }
        instance.setEnchantId(enchantId);
    }

    /** Catalog lookup that does not fall back to "No Enchant" for an unknown id. */
    public static Enchant find(int enchantId) {
        for (Enchant enchant : Enchant.getAllEnchants()) {
            if (enchant.id == enchantId) {
                return enchant;
            }
        }
        return null;
    }

    /** True when the item can take at least one catalog enchant (used to show the slot button). */
    public static boolean hasOptions(ItemInstance instance) {
        if (instance == null || instance.getItem() == null) {
            return false;
        }
        for (Enchant enchant : Enchant.getAllEnchants()) {
            if (enchant.id != 0 && instance.isEnchantEligible(enchant)) {
                return true;
            }
        }
        return false;
    }

    private static CalculationsBase modelFor(Character character) {
        String modelName = character.getCurrentModel();
        if (modelName == null || !Calculations.isModelRegistered(modelName)) {
            return null;
        }
        return Calculations.getModel(modelName);
    }

    private static String statsText(Enchant enchant) {
        String text = enchant.stats.toString();
        return text == null || text.isBlank() ? "No bonus" : text;
    }

    /** One row of the enchant picker. */
    public record EnchantChoice(int id, String name, String statsText, boolean equipped, boolean none) {
    }
}
