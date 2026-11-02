package rawr.web;

import org.springframework.stereotype.Service;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.character.Profession;
import rawr.base.items.Item;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemQuality;
import rawr.base.items.ItemSlot;
import rawr.base.util.Extensions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Gem sockets for the paper doll. Socket colors and the Blacksmithing extra socket follow the
 * same rules as {@code GemmingTemplate}/{@code Character.getRelevantItemInstances}. Like the legacy
 * picker, a meta socket only offers meta gems and every other socket offers all non-meta gems,
 * including off-color ones: the user may trade the socket bonus for a gem's stats. Jewelcrafter
 * gems are hidden without the profession.
 */
@Service
public class GemService {

    /** One visible socket of an equipped item, including the optional Blacksmithing socket. */
    public record SocketView(
            int index,
            String colorKey,
            String colorLabel,
            int gemId,
            String gemName,
            String gemIconPath,
            String qualityKey,
            String statsText,
            boolean empty,
            boolean matches) {

        public String title() {
            if (empty) {
                return colorLabel + ": click to socket a gem";
            }
            return gemName + (statsText == null || statsText.isBlank() ? "" : ": " + statsText);
        }
    }

    /** One row of the gem picker. */
    public record GemChoice(
            int id,
            String name,
            String statsText,
            String iconPath,
            String qualityKey,
            boolean socketed,
            boolean matches,
            boolean none) {
    }

    /** The sockets visible on an equipped item, in slot order. */
    public static List<SocketView> sockets(Character character, CharacterSlot slot, ItemInstance instance) {
        if (instance == null || instance.getItem() == null) {
            return List.of();
        }
        Item item = instance.getItem();
        List<SocketView> result = new ArrayList<SocketView>(3);
        for (int index = 1; index <= 3; index++) {
            ItemSlot color = socketColor(character, slot, instance, index);
            Item gem = instance.getGem(index);
            if (color == ItemSlot.None && gem == null) {
                continue;
            }
            ItemSlot displayColor = color == ItemSlot.None ? ItemSlot.Prismatic : color;
            String colorKey = displayColor.name().toLowerCase(Locale.ROOT);
            result.add(new SocketView(
                    index,
                    colorKey,
                    Extensions.spaceCamel(displayColor.name()) + " socket",
                    gem != null ? gem.getId() : 0,
                    gem != null ? gem.getName() : null,
                    gem != null ? gem.getIconPath() : null,
                    gem != null ? qualityKey(gem.getQuality()) : null,
                    gem != null ? gem.getStats().toString() : "",
                    gem == null,
                    gem == null || Item.gemMatchesSlot(gem, displayColor)));
        }
        return List.copyOf(result);
    }

    /**
     * Effective socket color for {@code index}. A {@code None} color becomes a prismatic socket
     * for the first missing socket of a Waist/Hands/Wrist item when the character enabled the
     * Blacksmithing extra socket, matching the optimizer's template expansion. An imported gem
     * in that missing socket also keeps it visible, so old characters with a Blacksmithing socket
     * remain editable.
     */
    public static ItemSlot socketColor(Character character, CharacterSlot slot, ItemInstance instance, int index) {
        if (instance == null || instance.getItem() == null || index < 1 || index > 3) {
            return ItemSlot.None;
        }
        Item item = instance.getItem();
        ItemSlot color = item.getSocketColor(index);
        if (color != ItemSlot.None) {
            return color;
        }
        int firstMissing = 0;
        for (int socket = 1; socket <= 3; socket++) {
            if (item.getSocketColor(socket) == ItemSlot.None) {
                firstMissing = socket;
                break;
            }
        }
        if (index != firstMissing) {
            return ItemSlot.None;
        }
        return hasBlacksmithingSocket(character, slot) || instance.getGem(index) != null
                ? ItemSlot.Prismatic : ItemSlot.None;
    }

    /** Gem color keys of the applied gems, in socket order (red/yellow/blue/meta/orange/...). */
    public static List<String> appliedGemColors(ItemInstance instance) {
        if (instance == null) {
            return List.of();
        }
        List<String> colors = new ArrayList<String>(3);
        for (int index = 1; index <= 3; index++) {
            Item gem = instance.getGem(index);
            if (gem == null) {
                continue;
            }
            ItemSlot color = gem.getSlot();
            colors.add(color == null || color == ItemSlot.None
                    ? "unknown" : color.name().toLowerCase(Locale.ROOT));
        }
        return List.copyOf(colors);
    }

    /** Display label for the picker header ("Red socket", ...). */
    public static String socketLabel(Character character, CharacterSlot slot, ItemInstance instance, int index) {
        ItemSlot color = socketColor(character, slot, instance, index);
        return color == ItemSlot.None ? "Socket" : Extensions.spaceCamel(color.name()) + " socket";
    }

    /** Normalized gem sort key: {@code quality} (default, best first) or {@code name}. */
    public static String normalizeSort(String sort) {
        return "name".equals(sort) ? "name" : "quality";
    }

    /** Candidate gems for one socket, optionally filtered by name and sorted by quality or name. */
    public List<GemChoice> choices(Character character, CharacterSlot slot, int socketIndex, String query,
            String sort) {
        ItemInstance instance = character.get(slot);
        if (instance == null || instance.getItem() == null) {
            return List.of();
        }
        ItemSlot color = socketColor(character, slot, instance, socketIndex);
        if (color == ItemSlot.None) {
            return List.of();
        }
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Item current = instance.getGem(socketIndex);
        List<GemChoice> choices = new ArrayList<GemChoice>();
        if (needle.isEmpty() || "no gem".contains(needle)) {
            choices.add(new GemChoice(0, "No gem", current == null ? "Empty socket" : "Remove the gem",
                    null, null, current == null, true, true));
        }
        boolean metaSocket = color == ItemSlot.Meta;
        List<Item> matches = new ArrayList<Item>();
        for (Item gem : ItemCache.allItems()) {
            boolean fits = metaSocket
                    ? gem.fitsInSlot(CharacterSlot.Metas)
                    : gem.fitsInSlot(CharacterSlot.Gems);
            if (!fits) {
                continue;
            }
            if (gem.isJewelersGem() && CalculationsBase.getHideProfEnchants()
                    && !character.hasProfession(Profession.Jewelcrafting)) {
                continue;
            }
            if (!needle.isEmpty() && !gem.getName().toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            matches.add(gem);
        }
        if ("name".equals(normalizeSort(sort))) {
            matches.sort(Comparator.comparing(Item::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(Item::getId));
        } else {
            matches.sort(Comparator.comparingInt((Item gem) -> gem.getQuality().value()).reversed()
                    .thenComparing(Item::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(Item::getId));
        }
        for (Item gem : matches) {
            choices.add(new GemChoice(gem.getId(), gem.getName(), gem.getStats().toString(),
                    gem.getIconPath(), qualityKey(gem.getQuality()), current != null && current.getId() == gem.getId(),
                    Item.gemMatchesSlot(gem, color), false));
        }
        return List.copyOf(choices);
    }

    /** Puts {@code gemId} into one socket; id 0 clears it. */
    public void apply(Character character, CharacterSlot slot, int socketIndex, int gemId) {
        ItemInstance instance = character.get(slot);
        if (instance == null || instance.getItem() == null) {
            throw new IllegalArgumentException("No item equipped in " + EditorService.slotLabel(slot));
        }
        ItemSlot color = socketColor(character, slot, instance, socketIndex);
        if (color == ItemSlot.None) {
            throw new IllegalArgumentException("There is no socket " + socketIndex + " on "
                    + instance.getItem().getName());
        }
        if (gemId != 0) {
            Item gem = ItemCache.findItemById(gemId);
            boolean fits = gem != null && (color == ItemSlot.Meta
                    ? gem.fitsInSlot(CharacterSlot.Metas)
                    : gem.fitsInSlot(CharacterSlot.Gems));
            if (!fits) {
                throw new IllegalArgumentException("Gem " + gemId + " does not fit a " + color.name() + " socket");
            }
        }
        switch (socketIndex) {
            case 1 -> instance.setGem1Id(gemId);
            case 2 -> instance.setGem2Id(gemId);
            case 3 -> instance.setGem3Id(gemId);
            default -> throw new IllegalArgumentException("Unknown socket: " + socketIndex);
        }
    }

    private static boolean hasBlacksmithingSocket(Character character, CharacterSlot slot) {
        return switch (slot) {
            case Waist -> character.getWaistBlacksmithingSocketEnabled();
            case Hands -> character.getHandsBlacksmithingSocketEnabled();
            case Wrist -> character.getWristBlacksmithingSocketEnabled();
            default -> false;
        };
    }

    private static String qualityKey(ItemQuality quality) {
        return quality == null ? null : quality.name().toLowerCase(Locale.ROOT);
    }
}
