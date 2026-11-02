// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/GemmingTemplate.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port).
package rawr.base.items;

import rawr.base.calculations.Calculations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A named rule for gemming a whole item consistently: which gem to use in each socket color.
 *
 * <p>A template stores one gem id per color ({@link #getRedId() red}, {@link #getYellowId()
 * yellow}, {@link #getBlueId() blue}, {@link #getMetaId() meta}, {@link #getPrismaticId()
 * prismatic}); each id resolves lazily to the {@link Item} through the item cache. The optimizer
 * and the item picker use templates as a cheap way to produce candidate {@link ItemInstance}s
 * without searching the whole gem space: {@link #getItemInstance(Item, Enchant, boolean)} walks
 * the item's three sockets, picks the template gem that matches each socket's color, optionally
 * fills one extra socket with the prismatic gem when the Blacksmithing profession grants an
 * extra socket ({@code blacksmithingSocket}), and returns the resulting instance with the
 * passed enchant. Whether the socket bonus actually applies is decided later by the instance's
 * total-stats resolution, from the gem colors.</p>
 *
 * <p>Templates are grouped per model name in a static {@code LinkedHashMap}; the first request
 * for a model seeds its bucket from
 * {@code Calculations.getInstance().getDefaultGemmingTemplates()} and caches it, so later UI
 * edits apply to that cached list. The fields are private and exposed through bean accessors:
 * {@code id} names the template, {@code model} scopes it to a specialization, {@code enabled}
 * lets the user switch it off, and {@code group} is the display bucket. This class only describes
 * and expands templates; it does not run the optimizer.</p>
 *
 * @see ItemInstance
 * @see Enchant
 */
public class GemmingTemplate {
    private String id;
    private String model;
    private boolean enabled;
    private String group;
    private int redId;
    private int yellowId;
    private int blueId;
    private int metaId;
    private int prismaticId;

    private Item redGem;
    private Item yellowGem;
    private Item blueGem;
    private Item metaGem;
    private Item prismaticGem;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return this.model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getGroup() {
        return this.group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public int getRedId() {
        return this.redId;
    }

    public void setRedId(int redId) {
        this.redId = redId;
    }

    public Item getRedGem() {
        if (this.redId == 0) return null;
        if (redGem == null || redGem.getId() != this.redId) {
            redGem = Item.loadFromId(this.redId);
        }
        return redGem;
    }

    public void setRedGem(Item value) {
        if (value == null)
            setRedId(0);
        else
            setRedId(value.getId());
        redGem = value;
    }

    public int getYellowId() {
        return this.yellowId;
    }

    public void setYellowId(int yellowId) {
        this.yellowId = yellowId;
    }

    public Item getYellowGem() {
        if (this.yellowId == 0) return null;
        if (yellowGem == null || yellowGem.getId() != this.yellowId) {
            yellowGem = Item.loadFromId(this.yellowId);
        }
        return yellowGem;
    }

    public void setYellowGem(Item value) {
        if (value == null)
            setYellowId(0);
        else
            setYellowId(value.getId());
        yellowGem = value;
    }

    public int getBlueId() {
        return this.blueId;
    }

    public void setBlueId(int blueId) {
        this.blueId = blueId;
    }

    public Item getBlueGem() {
        if (this.blueId == 0) return null;
        if (blueGem == null || blueGem.getId() != this.blueId) {
            blueGem = Item.loadFromId(this.blueId);
        }
        return blueGem;
    }

    public void setBlueGem(Item value) {
        if (value == null)
            setBlueId(0);
        else
            setBlueId(value.getId());
        blueGem = value;
    }

    public int getMetaId() {
        return this.metaId;
    }

    public void setMetaId(int metaId) {
        this.metaId = metaId;
    }

    public Item getMetaGem() {
        if (this.metaId == 0) return null;
        if (metaGem == null || metaGem.getId() != this.metaId) {
            metaGem = Item.loadFromId(this.metaId);
        }
        return metaGem;
    }

    public void setMetaGem(Item value) {
        if (value == null)
            setMetaId(0);
        else
            setMetaId(value.getId());
        metaGem = value;
    }

    public int getPrismaticId() {
        return this.prismaticId;
    }

    public void setPrismaticId(int prismaticId) {
        this.prismaticId = prismaticId;
    }

    public Item getPrismaticGem() {
        if (this.prismaticId == 0) return null;
        if (prismaticGem == null || prismaticGem.getId() != this.prismaticId) {
            prismaticGem = Item.loadFromId(this.prismaticId);
        }
        return prismaticGem;
    }

    public void setPrismaticGem(Item value) {
        if (value == null)
            setPrismaticId(0);
        else
            setPrismaticId(value.getId());
        prismaticGem = value;
    }

    /**
     * Produces the gemmed instance for one item: socket 1..3 each receive the template's gem for
     * its color ({@link ItemSlot#Meta} &rarr; meta gem, {@code Red}/{@code Yellow}/{@code Blue}
     * &rarr; that color's gem, {@code Prismatic} &rarr; prismatic gem). A socket whose color is
     * {@link ItemSlot#None} consumes one blacksmithing-granted prismatic gem when
     * {@code blacksmithingSocket} is true, and only the first such socket does. Returns
     * {@code null} for a {@code null} item; the enchant may be {@code null}.
     */
    public ItemInstance getItemInstance(Item item, Enchant enchant, boolean blacksmithingSocket) {
        if (item == null) return null;
        Item gem1 = null;
        Item gem2 = null;
        Item gem3 = null;
        switch (item.getSocketColor1()) {
            case Meta:
                gem1 = getMetaGem();
                break;
            case Red:
                gem1 = getRedGem();
                break;
            case Yellow:
                gem1 = getYellowGem();
                break;
            case Blue:
                gem1 = getBlueGem();
                break;
            case Prismatic:
                gem1 = getPrismaticGem();
                break;
            case None:
                if (blacksmithingSocket) {
                    gem1 = getPrismaticGem();
                    blacksmithingSocket = false;
                }
                break;
        }
        switch (item.getSocketColor2()) {
            case Meta:
                gem2 = getMetaGem();
                break;
            case Red:
                gem2 = getRedGem();
                break;
            case Yellow:
                gem2 = getYellowGem();
                break;
            case Blue:
                gem2 = getBlueGem();
                break;
            case Prismatic:
                gem2 = getPrismaticGem();
                break;
            case None:
                if (blacksmithingSocket) {
                    gem2 = getPrismaticGem();
                    blacksmithingSocket = false;
                }
                break;
        }
        switch (item.getSocketColor3()) {
            case Meta:
                gem3 = getMetaGem();
                break;
            case Red:
                gem3 = getRedGem();
                break;
            case Yellow:
                gem3 = getYellowGem();
                break;
            case Blue:
                gem3 = getBlueGem();
                break;
            case Prismatic:
                gem3 = getPrismaticGem();
                break;
            case None:
                if (blacksmithingSocket) {
                    gem3 = getPrismaticGem();
                    blacksmithingSocket = false;
                }
                break;
        }
        return new ItemInstance(item, gem1, gem2, gem3, enchant);
    }

    private static final Map<String, List<GemmingTemplate>> allTemplates = new LinkedHashMap<String, List<GemmingTemplate>>();

    public static Map<String, List<GemmingTemplate>> getAllTemplates() {
        return allTemplates;
    }

    /**
     * The template list for the current model ({@code Calculations.getInstance()}). On first
     * use the bucket is filled with a copy of the model's default templates and cached in
     * {@link #getAllTemplates()}, so subsequent calls return the same mutable list; a different
     * current model has its own bucket.
     */
    public static List<GemmingTemplate> getCurrentTemplates() {
        List<GemmingTemplate> list = allTemplates.get(Calculations.getInstance().getName());
        if (list == null) {
            list = new ArrayList<GemmingTemplate>(Calculations.getInstance().getDefaultGemmingTemplates());
            allTemplates.put(Calculations.getInstance().getName(), list);
        }
        return list;
    }
}
