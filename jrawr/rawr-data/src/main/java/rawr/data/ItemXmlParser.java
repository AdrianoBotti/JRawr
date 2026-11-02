// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Wowhead.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
//
// This is the payload-parsing half of Wowhead.GetItem(site, query, filter): root-agnostic
// (<wowhead> and <aowow>), name/quality/icon/display/slot/class, the <json>/<jsonEquip> stat
// mapping, sockets and socket bonus from the tooltip HTML, bind/unique, required classes, the
// meta-gem and use/equip tooltip-line SpecialEffects scan, the armor-vs-bonus-armor fix, the set
// name (with faction normalization) and the json source/sourcemore -> ItemLocation mapping.
// Deliberately omitted (out of scope, see DECISIONS #52):
//   - the vendor/token HTML scrape and the quest page scrape (network); the offline subset uses
//     the data already present in source/sourcemore;
//   - the random-suffix green quality filter (an import policy, not payload parsing);
//   - locale-specific zone/faction/profession strings (English only).
package rawr.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import rawr.base.items.sources.BindsOn;
import rawr.base.items.sources.ContainerItem;
import rawr.base.items.sources.CraftedItem;
import rawr.base.items.sources.FactionItem;
import rawr.base.items.Item;
import rawr.base.items.ItemDamageType;
import rawr.base.items.sources.ItemLocation;
import rawr.base.items.ItemQuality;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.items.sources.LocationFactory;
import rawr.base.items.sources.PvpItem;
import rawr.base.items.sources.QuestItem;
import rawr.base.character.ReputationLevel;
import rawr.base.effects.SpecialEffects;
import rawr.base.items.sources.StaticDrop;
import rawr.base.stats.Stats;
import rawr.base.items.sources.VendorItem;
import rawr.base.items.sources.WorldDrop;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Wowhead/AoWoW item payload ({@code <wowhead>/<item>} or {@code <aowow>/<item>}) into a
 * {@link Item}. Both databases emit the same payload shape; the differences (socket markup,
 * socket-bonus rendering, set prose) are handled explicitly.
 *
 * <p>{@link #parse(String)} registers the item's {@code ItemLocation} in {@link LocationFactory},
 * exactly like the original {@code Wowhead.GetItem} did. Callers that need to know whether the
 * payload was "Item not found" get {@code null}; malformed XML or payloads without an item throw
 * {@link ItemXmlParseException}.
 */
public final class ItemXmlParser {

    private static final Pattern SOCKET_BONUS_TEXT =
            Pattern.compile("Socket Bonus:\\s*\\+?(\\d+)\\s+([^<]+)");

    private static final String SOCKET_MARKUP = "class=\"socket-";

    private ItemXmlParser() {
    }

    /**
     * Parses one item payload.
     *
     * @return the item, or null when the payload explicitly reports "Item not found!"
     * @throws ItemXmlParseException when the XML is malformed or has no item element
     */
    public static Item parse(String xml) throws ItemXmlParseException {
        if (xml == null || xml.isBlank()) {
            throw new ItemXmlParseException("Empty item payload");
        }
        Document document = parseDocument(xml);
        Element root = document.getDocumentElement();
        if (root == null) {
            throw new ItemXmlParseException("Item payload has no document element");
        }
        if (root.getTextContent().contains("Item not found!")) {
            return null;
        }
        Element itemNode = firstChild(root, "item");
        if (itemNode == null) {
            throw new ItemXmlParseException("Unexpected item payload: no <item> element");
        }

        int id;
        try {
            id = Integer.parseInt(itemNode.getAttribute("id").trim());
        } catch (NumberFormatException e) {
            throw new ItemXmlParseException("Item payload has no numeric id attribute", e);
        }

        Item item = new Item();
        item.setId(id);
        item.setStats(new Stats());
        String htmlTooltip = "";
        String json1s = "";
        String json2s = "";
        String repSource = "";
        String repLevel = "";

        // Set initial data (name, quality, icon, ...) and record the tooltip/json sections.
        for (Element node : children(itemNode, "name")) {
            item.setName(node.getTextContent().trim());
        }
        for (Element node : children(itemNode, "quality")) {
            item.setQuality(qualityFromValue(parseIntAttribute(node, "id")));
        }
        for (Element node : children(itemNode, "icon")) {
            item.setIconPath(node.getTextContent());
        }
        for (Element node : children(itemNode, "htmlTooltip")) {
            htmlTooltip = node.getTextContent();
        }
        for (Element node : children(itemNode, "json")) {
            json1s = node.getTextContent();
        }
        for (Element node : children(itemNode, "jsonEquip")) {
            json2s = node.getTextContent();
        }
        if (htmlTooltip.contains("Unique")) {
            item.setUnique(true);
        }

        // Bind status (Wowhead.cs:269-279).
        if (htmlTooltip.contains("Binds when picked up")) {
            item.setBind(BindsOn.BoP);
        } else if (htmlTooltip.contains("Binds when equipped")) {
            item.setBind(BindsOn.BoE);
        } else if (htmlTooltip.contains("Binds to account")) {
            item.setBind(BindsOn.BoA);
        } else if (htmlTooltip.contains("Binds when used")) {
            item.setBind(BindsOn.BoU);
        }

        Map<String, Object> json;
        try {
            json = JsonParser.merge(JsonParser.parse(json1s), JsonParser.parse(json2s));
        } catch (RuntimeException e) {
            throw new ItemXmlParseException("Could not parse item json: " + e.getMessage(), e);
        }

        // Process json & jsonEquip (Wowhead.cs:296-629).
        Object value;
        if ((value = json.get("reqfaction")) != null) {
            repSource = stringValue(value);
        }
        if ((value = json.get("reqrep")) != null) {
            repLevel = stringValue(value);
        }
        if ((value = json.get("displayid")) != null) {
            item.setDisplayId(toInt(value));
        }
        if ((value = json.get("slotbak")) != null) {
            item.setDisplaySlot(toInt(value));
        }
        if ((value = json.get("level")) != null) {
            item.setItemLevel(toInt(value));
        }
        if ((value = json.get("slot")) != null) {
            int slot = toInt(value);
            if (slot != 0) {
                item.setSlot(getItemSlot(slot));
            }
        }
        if ((value = json.get("classs")) != null) {
            String classSubclass = stringValue(value);
            Object subclass = json.get("subclass");
            if (subclass != null) {
                classSubclass = classSubclass + "." + stringValue(subclass);
            }
            if (classSubclass.startsWith("1.") || classSubclass.startsWith("12.")) {
                // Container and quest item classes: not equippable, slot stays None.
            } else if (classSubclass.startsWith("3.")) {
                item.setType(ItemType.None);
                switch (classSubclass) {
                    case "3.0" -> item.setSlot(ItemSlot.Red);
                    case "3.1" -> item.setSlot(ItemSlot.Blue);
                    case "3.2" -> item.setSlot(ItemSlot.Yellow);
                    case "3.3" -> item.setSlot(ItemSlot.Purple);
                    case "3.4" -> item.setSlot(ItemSlot.Green);
                    case "3.5" -> item.setSlot(ItemSlot.Orange);
                    case "3.6" -> item.setSlot(ItemSlot.Meta);
                    case "3.8" -> item.setSlot(ItemSlot.Prismatic);
                    default -> {
                        // Unknown gem class variant; Type stays None and Slot stays None.
                    }
                }
            } else {
                item.setType(getItemType(classSubclass));
            }
        }
        if ((value = json.get("speed")) != null) {
            item.setSpeed(toFloat(value));
        }
        if ((value = json.get("dmgmin1")) != null) {
            item.setMinDamage((int) Math.floor(toFloat(value)));
        }
        if ((value = json.get("dmgmax1")) != null) {
            item.setMaxDamage((int) Math.ceil(toFloat(value)));
        }
        if ((value = json.get("dmgtype1")) != null) {
            item.setDamageType(damageTypeFromValue(toInt(value)));
        }
        if ((value = json.get("armor")) != null) {
            item.getStats().setArmor(toFloat(value));
        }
        if ((value = json.get("armorbonus")) != null) {
            item.getStats().setArmor(item.getStats().getArmor() - toFloat(value));
            item.getStats().setBonusArmor(toFloat(value));
        }
        if ((value = json.get("healthrgn")) != null) {
            item.getStats().setHp5(item.getStats().getHp5() + toFloat(value));
        }
        if ((value = json.get("manargn")) != null) {
            item.getStats().setMp5(item.getStats().getMp5() + toFloat(value));
        }
        if ((value = json.get("health")) != null) {
            item.getStats().setHealth(item.getStats().getHealth() + toFloat(value));
        }
        if ((value = json.get("agi")) != null) {
            item.getStats().setAgility(item.getStats().getAgility() + toFloat(value));
        }
        if ((value = json.get("int")) != null) {
            item.getStats().setIntellect(item.getStats().getIntellect() + toFloat(value));
        }
        if ((value = json.get("spi")) != null) {
            item.getStats().setSpirit(item.getStats().getSpirit() + toFloat(value));
        }
        if ((value = json.get("sta")) != null) {
            item.getStats().setStamina(item.getStats().getStamina() + toFloat(value));
        }
        if ((value = json.get("str")) != null) {
            item.getStats().setStrength(item.getStats().getStrength() + toFloat(value));
        }
        String hastertng = firstKey(json, "hastertng", "mlehastertng", "rgdhastertng", "splhastertng");
        if (hastertng != null) {
            item.getStats().setHasteRating(item.getStats().getHasteRating() + toFloat(json.get(hastertng)));
        }
        String spellPower = firstKey(json, "splpwr", "splheal", "spldmg");
        if (spellPower != null) {
            item.getStats().setSpellPower(item.getStats().getSpellPower() + toFloat(json.get(spellPower)));
        }
        String critRating = firstKey(json, "critstrkrtng", "mlecritstrkrtng", "rgdcritstrkrtng", "splcritstrkrtng");
        if (critRating != null) {
            item.getStats().setCritRating(item.getStats().getCritRating() + toFloat(json.get(critRating)));
        }
        if ((value = json.get("firres")) != null) {
            item.getStats().setFireResistance(item.getStats().getFireResistance() + toFloat(value));
        }
        if ((value = json.get("natres")) != null) {
            item.getStats().setNatureResistance(item.getStats().getNatureResistance() + toFloat(value));
        }
        if ((value = json.get("frores")) != null) {
            item.getStats().setFrostResistance(item.getStats().getFrostResistance() + toFloat(value));
        }
        if ((value = json.get("shares")) != null) {
            item.getStats().setShadowResistance(item.getStats().getShadowResistance() + toFloat(value));
        }
        if ((value = json.get("arcres")) != null) {
            item.getStats().setArcaneResistance(item.getStats().getArcaneResistance() + toFloat(value));
        }
        String hitRating = firstKey(json, "hitrtng", "mlehitrtng", "rgdhitrtng", "splhitrtng");
        if (hitRating != null) {
            item.getStats().setHitRating(item.getStats().getHitRating() + toFloat(json.get(hitRating)));
        }
        String attackPower = firstKey(json, "atkpwr", "mleatkpwr");
        if (attackPower != null) {
            item.getStats().setAttackPower(item.getStats().getAttackPower() + toFloat(json.get(attackPower)));
        }
        if ((value = json.get("rgdatkpwr")) != null) {
            if (item.getStats().getAttackPower() != toFloat(value)) {
                item.getStats().setRangedAttackPower(toFloat(value));
            }
        }
        if ((value = json.get("block")) != null) {
            item.getStats().setBlockValue(item.getStats().getBlockValue() + toFloat(value));
        }
        if ((value = json.get("exprtng")) != null) {
            item.getStats().setExpertiseRating(item.getStats().getExpertiseRating() + toFloat(value));
        }
        if ((value = json.get("defrtng")) != null) {
            item.getStats().setDefenseRating(item.getStats().getDefenseRating() + toFloat(value));
        }
        if ((value = json.get("dodgertng")) != null) {
            item.getStats().setDodgeRating(item.getStats().getDodgeRating() + toFloat(value));
        }
        if ((value = json.get("blockrtng")) != null) {
            item.getStats().setBlockRating(item.getStats().getBlockRating() + toFloat(value));
        }

        // Sockets and socket bonus (Wowhead.cs:501-560): socket colors come from the tooltip HTML
        // (both the AoWoW <span class="socket-X"> and the Wowhead <a ... class="socket-X"> forms).
        {
            int numSockets = json.containsKey("nsockets")
                    ? convertToInt32(json.get("nsockets"))
                    : countOccurrences(htmlTooltip, SOCKET_MARKUP);
            String htmlCopy = htmlTooltip;
            for (int i = 0; i < numSockets; i++) {
                if (!htmlCopy.contains(SOCKET_MARKUP)) {
                    continue;
                }
                String[] socket = findSockets(htmlCopy);
                htmlCopy = socket[1];
                ItemSlot color = getSocketTypeNew(socket[0]);
                switch (i) {
                    case 0 -> item.setSocketColor1(color);
                    case 1 -> item.setSocketColor2(color);
                    case 2 -> item.setSocketColor3(color);
                    default -> {
                        // More than three sockets are not representable; the original ignored them too.
                    }
                }
            }
            if (htmlCopy.contains("Socket Bonus: <a href=\"?enchantment=")) {
                int startPos = htmlCopy.indexOf("Socket Bonus: <a href=\"?enchantment=");
                int pos = startPos + 36;
                StringBuilder enchantId = new StringBuilder();
                while (htmlCopy.charAt(pos) != '"') {
                    enchantId.append(htmlCopy.charAt(pos));
                    pos++;
                }
                htmlCopy = htmlCopy.substring(0, startPos) + htmlCopy.substring(pos);
                item.setSocketBonus(getSocketBonus(enchantId.toString()));
            } else {
                Matcher socketBonus = SOCKET_BONUS_TEXT.matcher(htmlCopy);
                if (socketBonus.find()) {
                    item.setSocketBonus(getSocketBonusFromText(
                            Integer.parseInt(socketBonus.group(1)),
                            socketBonus.group(2).trim()));
                }
            }
        }
        if ((value = json.get("parryrtng")) != null) {
            item.getStats().setParryRating(item.getStats().getParryRating() + toFloat(value));
        }
        if ((value = json.get("classes")) != null) {
            List<String> requiredClasses = new ArrayList<String>();
            int classBitfield = toInt(value);
            if ((classBitfield & 1) > 0) {
                requiredClasses.add("Warrior");
            }
            if ((classBitfield & 2) > 0) {
                requiredClasses.add("Paladin");
            }
            if ((classBitfield & 4) > 0) {
                requiredClasses.add("Hunter");
            }
            if ((classBitfield & 8) > 0) {
                requiredClasses.add("Rogue");
            }
            if ((classBitfield & 16) > 0) {
                requiredClasses.add("Priest");
            }
            if ((classBitfield & 32) > 0) {
                requiredClasses.add("Death Knight");
            }
            if ((classBitfield & 64) > 0) {
                requiredClasses.add("Shaman");
            }
            if ((classBitfield & 128) > 0) {
                requiredClasses.add("Mage");
            }
            if ((classBitfield & 256) > 0) {
                requiredClasses.add("Warlock");
            }
            // (classBitfield & 512) only occurs on PvP gear; the original ignored it too.
            if ((classBitfield & 1024) > 0) {
                requiredClasses.add("Druid");
            }
            item.setRequiredClasses(String.join("|", requiredClasses));
        }
        if ((value = json.get("resirtng")) != null) {
            item.getStats().setResilience(item.getStats().getResilience() + toFloat(value));
        }
        String armorPen = firstKey(json, "armorpen", "armorpenrtng");
        if (armorPen != null) {
            item.getStats().setArmorPenetrationRating(toFloat(json.get(armorPen)));
        }
        if ((value = json.get("splpen")) != null) {
            item.getStats().setSpellPenetration(toFloat(value));
        }
        if ((value = json.get("mana")) != null) {
            item.getStats().setMana(toFloat(value));
        }
        if ((value = json.get("dmg")) != null) {
            item.getStats().setWeaponDamage(toFloat(value));
        }
        if ((value = json.get("frospldmg")) != null) {
            item.getStats().setSpellFrostDamageRating(toFloat(value));
        }
        if ((value = json.get("shaspldmg")) != null) {
            item.getStats().setSpellShadowDamageRating(toFloat(value));
        }
        if ((value = json.get("firspldmg")) != null) {
            item.getStats().setSpellFireDamageRating(toFloat(value));
        }
        if ((value = json.get("arcspldmg")) != null) {
            item.getStats().setSpellArcaneDamageRating(toFloat(value));
        }

        // Not slottable (e.g. a non-gem item with classs 1.x/12.x): nothing more to do.
        if (item.getSlot() == ItemSlot.None) {
            return null;
        }

        // Item source (Wowhead.cs:634-1290), offline subset using only source/sourcemore data.
        parseLocation(item, json, repSource, repLevel);

        // Meta gem effects (Wowhead.cs:1298-1321): the gem bonus line is the first q1 span of the
        // tooltip with its <a>/</a> link markup stripped.
        if (item.getSlot() == ItemSlot.Meta) {
            if (htmlTooltip.contains("<span class=\"q1\">") && htmlTooltip.contains("</span>")) {
                String line = htmlTooltip.substring(htmlTooltip.indexOf("<span class=\"q1\">") + "<span class=\"q1\">".length());
                line = line.substring(0, line.indexOf("</span>"));
                if (line.contains("<a")) {
                    {
                        int start = line.indexOf("<a");
                        int end = line.indexOf(">", start + 1);
                        line = line.substring(0, start) + line.substring(end + 1); // Remove(start, end - start + 1)
                    }
                    {
                        int start = line.indexOf("</a>");
                        line = line.substring(0, start) + line.substring(start + 4);
                    }
                }
                SpecialEffects.processMetaGem(line, item.getStats(), false);
            } else {
                throw new ItemXmlParseException("Unhandled Metagem:\r\n" + item.getName());
            }
        }

        // Special effects (Wowhead.cs:1329-1391): collect Equip/Chance on hit/Use q2 spans, then run
        // the use/equip parsers. The extraction quirks (line mutation before advancing the cursor,
        // "sec." -> "sec", comment/link/small removal) are ported as written.
        List<String> useLines = new ArrayList<String>();
        List<String> equipLines = new ArrayList<String>();
        while (htmlTooltip.contains("<span class=\"q2\">") && htmlTooltip.contains("</span>")) {
            htmlTooltip = htmlTooltip.substring(htmlTooltip.indexOf("<span class=\"q2\">") + "<span class=\"q2\">".length());
            String line = htmlTooltip.substring(0, htmlTooltip.indexOf("</span>"));

            // Remove Comments
            while (line.contains("<!--")) {
                int start = line.indexOf("<!--");
                int end = line.indexOf("-->");
                String toRemove = line.substring(start, end + 3); // Substring(start, end - start + 3)
                line = line.replace(toRemove, "");
            }
            // Swap out to real spaces
            while (line.contains("&nbsp;")) {
                line = line.replace("&nbsp;", " ");
            }
            // Remove the Spell Links
            // Later we will instead USE the spell links but we aren't set up for that right now
            while (line.contains("<a")) {
                int start = line.indexOf("<a");
                int end = line.indexOf(">");
                String toRemove = line.substring(start, end + 1); // Substring(start, end - start + 1)
                line = line.replace(toRemove, "");
            }
            while (line.contains("</a>")) {
                line = line.replace("</a>", "");
            }
            // Remove the Small tags, we don't use those
            while (line.contains("<small")) {
                int start = line.indexOf("<small>");
                int end = line.indexOf("</small>");
                String toRemove = line.substring(start, end + "</small>".length()); // Substring(start, end - start + len)
                line = line.replace(toRemove, "");
            }
            // Remove double spaces
            while (line.contains("  ")) {
                line = line.replace("  ", " ");
            }
            // Swap out "sec." with "sec" as sometimes they
            // do and sometimes they don't, regex for both is annoying
            while (line.contains("sec.")) {
                line = line.replace("sec.", "sec");
            }

            // Now Process it
            if (line.startsWith("Equip: ")) {
                String equipLine = line.substring("Equip: ".length());
                equipLines.add(equipLine);
            } else if (line.startsWith("Chance on hit: ")) {
                String chanceLine = line.substring("Chance on hit: ".length());
                equipLines.add(chanceLine);
            } else if (line.startsWith("Use: ")) {
                String useLine = line.substring("Use: ".length());
                useLines.add(useLine);
            }
            htmlTooltip = htmlTooltip.substring(line.length() + "</span>".length());
        }
        for (String useLine : useLines) {
            SpecialEffects.processUseLine(useLine, item.getStats(), false, item.getId());
        }
        for (String equipLine : equipLines) {
            SpecialEffects.processEquipLine(equipLine, item.getStats(), false, item.getItemLevel(), item.getId());
        }

        // Armor vs bonus armor fixes (Wowhead.cs:1417-1437).
        if (item.getSlot() == ItemSlot.Finger
                || item.getSlot() == ItemSlot.MainHand
                || item.getSlot() == ItemSlot.Neck
                || (item.getSlot() == ItemSlot.OffHand && item.getType() != ItemType.Shield)
                || item.getSlot() == ItemSlot.OneHand
                || item.getSlot() == ItemSlot.Trinket
                || item.getSlot() == ItemSlot.TwoHand) {
            item.getStats().setBonusArmor(item.getStats().getBonusArmor() + item.getStats().getArmor());
            item.getStats().setArmor(0f);
        } else if (item.getStats().getArmor() + item.getStats().getBonusArmor() == 0f) {
            item.getStats().setArmor(0f);
            item.getStats().setBonusArmor(0f);
        }

        // Belongs to a set (Wowhead.cs:1440-1452).
        if (htmlTooltip.contains(" (0/")) {
            htmlTooltip = htmlTooltip.substring(0, htmlTooltip.indexOf("</a> (0/"));
            htmlTooltip = htmlTooltip.substring(htmlTooltip.lastIndexOf(">") + 1);
            htmlTooltip = htmlTooltip
                    .replace("Wrathful ", "").replace("Relentless ", "").replace("Furious ", "")
                    .replace("Deadly ", "").replace("Hateful ", "").replace("Savage ", "")
                    .replace("Brutal ", "").replace("Vengeful ", "").replace("Merciless ", "")
                    .replace("Valorous ", "")
                    .replace("Heroes' ", "").replace("Conqueror's ", "").replace("Totally ", "")
                    .replace("Triumphant ", "").replace("Kirin'dor", "Kirin Tor")
                    .replace("Regaila", "Regalia").replace("Sanctified ", "");
            if (htmlTooltip.contains("Sunstrider's") || htmlTooltip.contains("Zabra's")
                    || htmlTooltip.contains("Gul'dan's") || htmlTooltip.contains("Garona's")
                    || htmlTooltip.contains("Runetotem's") || htmlTooltip.contains("Windrunner's Pursuit")
                    || htmlTooltip.contains("Thrall's") || htmlTooltip.contains("Liadrin's")
                    || htmlTooltip.contains("Hellscream's") || htmlTooltip.contains("Kolitra's")
                    || htmlTooltip.contains("Koltira's")) {
                item.setFaction(rawr.base.items.ItemFaction.Horde);
            } else if (htmlTooltip.contains("Khadgar's") || htmlTooltip.contains("Velen's")
                    || htmlTooltip.contains("Kel'Thuzad's") || htmlTooltip.contains("VanCleef's")
                    || htmlTooltip.contains("Malfurion's") || htmlTooltip.contains("Windrunner's Battlegear")
                    || htmlTooltip.contains("Nobundo's") || htmlTooltip.contains("Turalyon's")
                    || htmlTooltip.contains("Wrynn's") || htmlTooltip.contains("Thassarian's")) {
                item.setFaction(rawr.base.items.ItemFaction.Alliance);
            }
            htmlTooltip = htmlTooltip.replace("Sunstrider's", "Khadgar's")
                    .replace("Zabra's", "Velen's")
                    .replace("Gul'dan's", "Kel'Thuzad's")
                    .replace("Garona's", "VanCleef's")
                    .replace("Runetotem's", "Malfurion's")
                    .replace("Windrunner's Pursuit", "Windrunner's Battlegear")
                    .replace("Thrall's", "Nobundo's")
                    .replace("Liadrin's", "Turalyon's")
                    .replace("Hellscream's", "Wrynn's")
                    .replace("Koltira's", "Thassarian's")
                    .replace("Kolitra's", "Thassarian's");
            item.setSetName(htmlTooltip.trim());
        }

        return item;
    }

    // ------------------------------------------------------------------ document helpers

    private static Document parseDocument(String xml) throws ItemXmlParseException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            factory.setXIncludeAware(false);
            trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
            trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new ItemXmlParseException("Invalid item XML: " + e.getMessage(), e);
        }
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception e) {
            // Hardening only; the payloads are simple and never rely on DTDs.
        }
    }

    private static Element firstChild(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getTagName().equals(name)) {
                return (Element) node;
            }
        }
        return null;
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> matches = new ArrayList<Element>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getTagName().equals(name)) {
                matches.add((Element) node);
            }
        }
        return matches;
    }

    // ------------------------------------------------------------------ number helpers

    private static int parseIntAttribute(Element element, String name) {
        return Integer.parseInt(element.getAttribute(name).trim());
    }

    /** C# {@code (int)tmp} unboxing: truncation, NaN becomes 0, infinities saturate. */
    static int toInt(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Double d) {
            return (int) d.doubleValue();
        }
        if (value instanceof String s) {
            return Integer.parseInt(s.trim());
        }
        if (value instanceof Boolean b) {
            return b ? 1 : 0;
        }
        throw new IllegalArgumentException("Not an integer value: " + value);
    }

    /** C# {@code Convert.ToInt32(tmp)}: rounds half to even (never truncates). */
    static int convertToInt32(Object value) {
        if (value instanceof Double d) {
            return (int) Math.rint(d.doubleValue());
        }
        return toInt(value);
    }

    /**
     * C# {@code Convert.ToSingle(tmp)}: accepts numbers, numeric strings (including NaN and
     * Infinity, which the databases do emit) and booleans.
     */
    static float toFloat(Object value) {
        if (value instanceof Double d) {
            return d.floatValue();
        }
        if (value instanceof Integer integer) {
            return integer.floatValue();
        }
        if (value instanceof String s) {
            return Float.parseFloat(s.trim());
        }
        if (value instanceof Boolean b) {
            return b ? 1f : 0f;
        }
        throw new IllegalArgumentException("Not a numeric value: " + value);
    }

    /** C# {@code tmp.ToString()} for the integer/double/boolean/string values in the payloads. */
    static String stringValue(Object value) {
        if (value instanceof Double d) {
            double raw = d.doubleValue();
            if (!Double.isNaN(raw) && !Double.isInfinite(raw) && raw == Math.floor(raw)) {
                return Long.toString((long) raw);
            }
            return Double.toString(raw);
        }
        if (value instanceof Boolean b) {
            return b ? "True" : "False";
        }
        return String.valueOf(value);
    }

    private static String firstKey(Map<String, Object> json, String... keys) {
        for (String key : keys) {
            if (json.containsKey(key)) {
                return key;
            }
        }
        return null;
    }

    private static ItemQuality qualityFromValue(int value) {
        for (ItemQuality quality : ItemQuality.values()) {
            if (quality.value() == value) {
                return quality;
            }
        }
        return ItemQuality.Poor;
    }

    private static ItemDamageType damageTypeFromValue(int value) {
        for (ItemDamageType type : ItemDamageType.values()) {
            if (type.value() == value) {
                return type;
            }
        }
        return ItemDamageType.Physical;
    }

    // ------------------------------------------------------------------ sockets (Wowhead.cs:1463-1478, 2166-2341)

    /** Finds the next socket color in the tooltip and returns {color, remainingHtml}. */
    static String[] findSockets(String html) {
        int startPos = html.indexOf(SOCKET_MARKUP);
        int pos = startPos + SOCKET_MARKUP.length();
        StringBuilder color = new StringBuilder();
        while (html.charAt(pos) != ' ' && html.charAt(pos) != '"' && html.charAt(pos) != '>') {
            color.append(html.charAt(pos));
            pos++;
        }
        html = html.substring(0, startPos) + html.substring(pos);
        return new String[] { color.toString(), html };
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            count++;
            index = text.indexOf(needle, index + needle.length());
        }
        return count;
    }

    static ItemSlot getSocketTypeNew(String socketColor) {
        return switch (socketColor) {
            case "meta" -> ItemSlot.Meta;
            case "red" -> ItemSlot.Red;
            case "yellow" -> ItemSlot.Yellow;
            case "orange" -> ItemSlot.Orange;
            case "blue" -> ItemSlot.Blue;
            case "purple" -> ItemSlot.Purple;
            case "green" -> ItemSlot.Green;
            case "prismatic" -> ItemSlot.Prismatic;
            default -> throw new IllegalArgumentException("Unknown Slot Type :" + socketColor);
        };
    }

    static Stats getSocketBonus(String socketBonus) {
        Stats stats = new Stats();
        switch (socketBonus) {
            // Spell Power
            case "2900" -> stats.setSpellPower(stats.getSpellPower() + 4);
            case "2872", "2889", "3198", "3596", "3752" -> stats.setSpellPower(stats.getSpellPower() + 5);
            case "428", "2770", "3602" -> stats.setSpellPower(stats.getSpellPower() + 7);
            case "430", "440", "2314", "3753" -> stats.setSpellPower(stats.getSpellPower() + 9);
            // Attack Power
            case "3114" -> stats.setAttackPower(stats.getAttackPower() + 4);
            case "2936" -> stats.setAttackPower(stats.getAttackPower() + 8);
            case "1587", "3356", "3764" -> stats.setAttackPower(stats.getAttackPower() + 12);
            case "3877", "1589" -> stats.setAttackPower(stats.getAttackPower() + 16);
            case "1597" -> stats.setAttackPower(stats.getAttackPower() + 32);
            // Stamina
            case "2895" -> stats.setStamina(stats.getStamina() + 4);
            case "2868", "2882" -> stats.setStamina(stats.getStamina() + 6);
            case "1886", "3307" -> stats.setStamina(stats.getStamina() + 9);
            case "3354", "3305", "3766" -> stats.setStamina(stats.getStamina() + 12);
            // Mp5
            case "2367", "2865", "3306" -> stats.setMp5(stats.getMp5() + 2);
            case "2370", "2854" -> stats.setMp5(stats.getMp5() + 3);
            case "2371" -> stats.setMp5(stats.getMp5() + 4);
            // Hit Rating
            case "2873", "2908" -> stats.setHitRating(stats.getHitRating() + 4);
            case "3351" -> stats.setHitRating(stats.getHitRating() + 6);
            case "2767", "2844" -> stats.setHitRating(stats.getHitRating() + 8);
            // Crit Rating
            case "2887", "3204" -> stats.setCritRating(stats.getCritRating() + 3);
            case "2864", "2874", "2951", "2952", "3263" -> stats.setCritRating(stats.getCritRating() + 4);
            case "3301", "3316" -> stats.setCritRating(stats.getCritRating() + 6);
            case "2771", "2787", "2843", "3314" -> stats.setCritRating(stats.getCritRating() + 8);
            // Spirit
            case "2890" -> stats.setSpirit(stats.getSpirit() + 4);
            case "3311" -> stats.setSpirit(stats.getSpirit() + 6);
            case "2842", "3352" -> stats.setSpirit(stats.getSpirit() + 8);
            // Intellect
            case "2869" -> stats.setIntellect(stats.getIntellect() + 4);
            case "3310" -> stats.setIntellect(stats.getIntellect() + 6);
            case "3353" -> stats.setIntellect(stats.getIntellect() + 8);
            // Dodge Rating
            case "2871" -> stats.setDodgeRating(stats.getDodgeRating() + 4);
            case "3358" -> stats.setDodgeRating(stats.getDodgeRating() + 6);
            case "3304" -> stats.setDodgeRating(stats.getDodgeRating() + 8);
            // Agility
            case "3149" -> stats.setAgility(stats.getAgility() + 2);
            case "2877" -> stats.setAgility(stats.getAgility() + 4);
            case "3355" -> stats.setAgility(stats.getAgility() + 6);
            case "3313" -> stats.setAgility(stats.getAgility() + 8);
            // Resilience
            case "2878" -> stats.setResilience(stats.getResilience() + 4);
            case "3600" -> stats.setResilience(stats.getResilience() + 6);
            case "3821" -> stats.setResilience(stats.getResilience() + 8);
            // Strength
            case "2892" -> stats.setStrength(stats.getStrength() + 4);
            case "2927" -> stats.setStrength(stats.getStrength() + 4);
            case "3312" -> stats.setStrength(stats.getStrength() + 8);
            case "3357" -> stats.setStrength(stats.getStrength() + 6);
            // Block Rating
            case "2972" -> stats.setBlockRating(stats.getBlockRating() + 4);
            case "3361" -> stats.setBlockRating(stats.getBlockRating() + 6);
            // Block Value
            case "2888" -> stats.setBlockValue(stats.getBlockValue() + 6);
            case "3363" -> stats.setBlockValue(stats.getBlockValue() + 9);
            // Defense Rating
            case "2932" -> stats.setDefenseRating(stats.getDefenseRating() + 4);
            case "3751", "3857" -> stats.setDefenseRating(stats.getDefenseRating() + 6);
            case "3302" -> stats.setDefenseRating(stats.getDefenseRating() + 8);
            // Haste Rating
            case "3267", "3308" -> stats.setHasteRating(stats.getHasteRating() + 4);
            case "3309" -> stats.setHasteRating(stats.getHasteRating() + 6);
            case "2963", "3303" -> stats.setHasteRating(stats.getHasteRating() + 8);
            // Expertise Rating
            case "3094" -> stats.setExpertiseRating(stats.getExpertiseRating() + 4);
            case "3362" -> stats.setExpertiseRating(stats.getExpertiseRating() + 6);
            case "3778" -> stats.setExpertiseRating(stats.getExpertiseRating() + 8);
            // Parry Rating
            case "3359" -> stats.setParryRating(stats.getParryRating() + 4);
            case "3871" -> stats.setParryRating(stats.getParryRating() + 6);
            case "3360" -> stats.setParryRating(stats.getParryRating() + 8);
            // Armor Penetration Rating
            case "3765", "3880" -> stats.setArmorPenetrationRating(stats.getArmorPenetrationRating() + 4);
            case "3882" -> stats.setArmorPenetrationRating(stats.getArmorPenetrationRating() + 8);
            default -> {
                // Unhandled socket bonus ids are ignored, like the original's _unhandledSocketBonus list.
            }
        }
        return stats;
    }

    static Stats getSocketBonusFromText(int value, String statName) {
        Stats stats = new Stats();
        switch (statName.toLowerCase(java.util.Locale.ROOT)) {
            case "strength" -> stats.setStrength(stats.getStrength() + value);
            case "agility" -> stats.setAgility(stats.getAgility() + value);
            case "stamina" -> stats.setStamina(stats.getStamina() + value);
            case "intellect" -> stats.setIntellect(stats.getIntellect() + value);
            case "spirit" -> stats.setSpirit(stats.getSpirit() + value);
            case "health" -> stats.setHealth(stats.getHealth() + value);
            case "mana" -> stats.setMana(stats.getMana() + value);
            case "health per 5 sec", "health per 5 sec." -> stats.setHp5(stats.getHp5() + value);
            case "mana per 5 sec", "mana per 5 sec." -> stats.setMp5(stats.getMp5() + value);
            case "armor" -> stats.setArmor(stats.getArmor() + value);
            case "bonus armor" -> stats.setBonusArmor(stats.getBonusArmor() + value);
            case "defense rating" -> stats.setDefenseRating(stats.getDefenseRating() + value);
            case "block value" -> stats.setBlockValue(stats.getBlockValue() + value);
            case "block rating" -> stats.setBlockRating(stats.getBlockRating() + value);
            case "dodge rating" -> stats.setDodgeRating(stats.getDodgeRating() + value);
            case "parry rating" -> stats.setParryRating(stats.getParryRating() + value);
            case "resilience rating" -> stats.setResilience(stats.getResilience() + value);
            case "attack power" -> stats.setAttackPower(stats.getAttackPower() + value);
            case "spell power" -> stats.setSpellPower(stats.getSpellPower() + value);
            case "expertise rating" -> stats.setExpertiseRating(stats.getExpertiseRating() + value);
            case "hit rating" -> stats.setHitRating(stats.getHitRating() + value);
            case "critical strike rating" -> stats.setCritRating(stats.getCritRating() + value);
            case "haste rating" -> stats.setHasteRating(stats.getHasteRating() + value);
            case "armor penetration rating" -> stats.setArmorPenetrationRating(stats.getArmorPenetrationRating() + value);
            case "spell penetration" -> stats.setSpellPenetration(stats.getSpellPenetration() + value);
            default -> {
                // Unknown text bonuses are ignored, like the original.
            }
        }
        return stats;
    }

    // ------------------------------------------------------------------ item type/slot (Wowhead.cs:2377-2549)

    static ItemType getItemType(String classSubclass) {
        return switch (classSubclass) {
            case "4.1" -> ItemType.Cloth;
            case "4.2" -> ItemType.Leather;
            case "4.3" -> ItemType.Mail;
            case "4.4" -> ItemType.Plate;
            case "2.15" -> ItemType.Dagger;
            case "2.13" -> ItemType.FistWeapon;
            case "2.1" -> ItemType.TwoHandAxe;
            case "2.0" -> ItemType.OneHandAxe;
            case "2.5" -> ItemType.TwoHandMace;
            case "2.4" -> ItemType.OneHandMace;
            case "2.8" -> ItemType.TwoHandSword;
            case "2.7" -> ItemType.OneHandSword;
            case "2.6" -> ItemType.Polearm;
            case "2.10" -> ItemType.Staff;
            case "4.6" -> ItemType.Shield;
            case "2.2" -> ItemType.Bow;
            case "2.18" -> ItemType.Crossbow;
            case "2.3" -> ItemType.Gun;
            case "2.19" -> ItemType.Wand;
            case "2.16" -> ItemType.Thrown;
            case "4.8" -> ItemType.Idol;
            case "4.7" -> ItemType.Libram;
            case "4.9" -> ItemType.Totem;
            case "6.2" -> ItemType.Arrow;
            case "6.3" -> ItemType.Bullet;
            case "11.2" -> ItemType.Quiver;
            case "11.3" -> ItemType.AmmoPouch;
            case "4.10" -> ItemType.Sigil;
            default -> ItemType.None;
        };
    }

    static ItemSlot getItemSlot(int slotId) {
        return switch (slotId) {
            case 1 -> ItemSlot.Head;
            case 2 -> ItemSlot.Neck;
            case 3 -> ItemSlot.Shoulders;
            case 16 -> ItemSlot.Back;
            case 5, 20 -> ItemSlot.Chest;
            case 4 -> ItemSlot.Shirt;
            case 19 -> ItemSlot.Tabard;
            case 9 -> ItemSlot.Wrist;
            case 10 -> ItemSlot.Hands;
            case 6 -> ItemSlot.Waist;
            case 7 -> ItemSlot.Legs;
            case 8 -> ItemSlot.Feet;
            case 11 -> ItemSlot.Finger;
            case 12 -> ItemSlot.Trinket;
            case 13 -> ItemSlot.OneHand;
            case 17 -> ItemSlot.TwoHand;
            case 21 -> ItemSlot.MainHand;
            case 14, 22, 23 -> ItemSlot.OffHand;
            case 15, 25, 26, 28 -> ItemSlot.Ranged;
            case 24 -> ItemSlot.Projectile;
            case 18, 27 -> ItemSlot.ProjectileBag;
            default -> ItemSlot.None;
        };
    }

    // ------------------------------------------------------------------ item source (Wowhead.cs:634-1290)

    /**
     * Offline subset of the original source processing. Vendor token costs and quest metadata come
     * from HTML pages the original scraped (not ported); everything derivable from source/
     * sourcemore is mapped to the same {@link ItemLocation} subclass the original chose.
     */
    private static void parseLocation(Item item, Map<String, Object> json, String repSource, String repLevel) {
        String itemId = Integer.toString(item.getId());
        if (json.containsKey("source")) {
            Object[] sourceArr = (Object[]) json.get("source");
            Object[] sourcemoreArr = json.containsKey("sourcemore") ? (Object[]) json.get("sourcemore") : null;
            if (sourceArr == null || sourceArr.length == 0) {
                return;
            }
            int source = toInt(sourceArr[0]);
            Map<String, Object> sourcemore = sourcemoreArr != null && sourcemoreArr.length > 0
                    ? asMap(sourcemoreArr[0])
                    : null;
            int vendorIndex = indexOfInt(sourceArr, 5);
            if (vendorIndex >= 0) {
                source = toInt(sourceArr[vendorIndex]);
                if (sourcemoreArr != null && sourcemoreArr.length > vendorIndex) {
                    sourcemore = asMap(sourcemoreArr[vendorIndex]);
                }
            }
            String n = sourcemore != null && sourcemore.get("n") != null
                    ? stringValue(sourcemore.get("n"))
                    : "";

            if (source == 2 && sourcemore == null) {
                LocationFactory.add(itemId, new WorldDrop());
            } else if (source == 5) {
                // Vendor: without the token/page scrape this is either a reputation item (when the
                // json names a faction and level) or a plain vendor item with sourcemore name/zone.
                if (!repSource.isEmpty() && !repLevel.isEmpty()) {
                    String[] repInfo = getItemFactionVendorInfo(repSource, repLevel);
                    FactionItem loc = new FactionItem();
                    loc.setFactionName(repInfo[0]);
                    loc.setLevel(reputationLevel(repLevel));
                    loc.setCost(0);
                    LocationFactory.add(itemId, loc);
                } else {
                    VendorItem loc = new VendorItem();
                    loc.setCost(0);
                    if (!n.isEmpty()) {
                        loc.setVendorName(n);
                    }
                    if (sourcemore != null && sourcemore.get("z") != null) {
                        loc.setVendorArea(getZoneName(stringValue(sourcemore.get("z"))));
                    }
                    LocationFactory.add(itemId, loc);
                }
            } else if (sourcemore != null) {
                Object type = sourcemore.get("t");
                if (type != null) {
                    switch (toInt(type)) {
                        case 1 -> LocationFactory.add(itemId, staticDrop(sourcemore));
                        case 2 -> LocationFactory.add(itemId, containerItem(sourcemore));
                        case 5 -> LocationFactory.add(itemId, questItem(sourcemore));
                        case 6 -> LocationFactory.add(itemId, craftedItem(sourcemore));
                        default -> {
                            // Other content types (item, itemset, zone, faction, pet, ...) have
                            // no matching ItemLocation in the original either.
                        }
                    }
                }
                if (sourcemore.containsKey("p")) {
                    PvpItem pvp = new PvpItem();
                    pvp.setPoints(0);
                    pvp.setPointType("PvP");
                    LocationFactory.add(itemId, pvp);
                }
            }
        } else if (item.getStats().getResilience() > 0) {
            LocationFactory.add(itemId, new PvpItem());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private static int indexOfInt(Object[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] instanceof Number number && toInt(number) == value) {
                return i;
            }
        }
        return -1;
    }

    private static StaticDrop staticDrop(Map<String, Object> sourcemore) {
        StaticDrop drop = new StaticDrop();
        if (sourcemore.get("n") != null) {
            drop.setBoss(stringValue(sourcemore.get("n")));
        }
        Object zone = sourcemore.get("z") != null ? sourcemore.get("z") : sourcemore.get("c");
        if (zone != null) {
            drop.setArea(getZoneName(stringValue(zone)));
        }
        applyDifficulty(sourcemore, (heroic, suffix) -> {
            drop.setHeroic(heroic);
            drop.setArea((drop.getArea() == null ? "" : drop.getArea()) + suffix);
        });
        return drop;
    }

    private static ContainerItem containerItem(Map<String, Object> sourcemore) {
        ContainerItem container = new ContainerItem();
        if (sourcemore.get("n") != null) {
            container.setContainer(stringValue(sourcemore.get("n")));
        }
        Object zone = sourcemore.get("z") != null ? sourcemore.get("z") : sourcemore.get("c");
        if (zone != null) {
            container.setArea(getZoneName(stringValue(zone)));
        }
        applyDifficulty(sourcemore, (heroic, suffix) -> {
            container.setHeroic(heroic);
            container.setArea((container.getArea() == null ? "" : container.getArea()) + suffix);
        });
        return container;
    }

    private static QuestItem questItem(Map<String, Object> sourcemore) {
        QuestItem quest = (QuestItem) QuestItem.construct();
        if (sourcemore.get("n") != null) {
            quest.setQuest(stringValue(sourcemore.get("n")));
        }
        Object zone = sourcemore.get("z") != null ? sourcemore.get("z") : sourcemore.get("c");
        if (zone != null) {
            quest.setArea(getZoneName(stringValue(zone)));
        }
        return quest;
    }

    private static CraftedItem craftedItem(Map<String, Object> sourcemore) {
        CraftedItem crafted = (CraftedItem) CraftedItem.construct();
        if (sourcemore.get("n") != null) {
            crafted.setSpellName(stringValue(sourcemore.get("n")));
        }
        if (sourcemore.get("z") != null) {
            crafted.setSkill(getZoneName(stringValue(sourcemore.get("z"))));
        }
        if (sourcemore.get("s") != null) {
            String profession = professionName(stringValue(sourcemore.get("s")));
            if (!profession.isEmpty()) {
                crafted.setSkill(profession);
            }
        }
        return crafted;
    }

    private static void applyDifficulty(Map<String, Object> sourcemore, DifficultyConsumer consumer) {
        if (sourcemore.get("dd") == null) {
            return;
        }
        String value = stringValue(sourcemore.get("dd"));
        boolean heroic = "-2".equals(value) || "3".equals(value) || "4".equals(value);
        String suffix = ("1".equals(value) || "3".equals(value)) ? " (10)"
                : ("2".equals(value) || "4".equals(value)) ? " (25)" : "";
        consumer.accept(heroic, suffix);
    }

    private interface DifficultyConsumer {
        void accept(boolean heroic, String suffix);
    }

    private static ReputationLevel reputationLevel(String level) {
        int index = Integer.parseInt(level);
        ReputationLevel[] levels = ReputationLevel.values();
        return index >= 0 && index < levels.length ? levels[index] : ReputationLevel.Hated;
    }

    /** English profession names for the {@code s} skill ids, as in the original default locale. */
    static String professionName(String skillId) {
        return switch (skillId) {
            case "171" -> "Alchemy";
            case "164" -> "Blacksmithing";
            case "333" -> "Enchanting";
            case "202" -> "Engineering";
            case "182" -> "Herbalism";
            case "773" -> "Inscription";
            case "755" -> "Jewelcrafting";
            case "165" -> "Leatherworking";
            case "186" -> "Mining";
            case "393" -> "Skinning";
            case "197" -> "Tailoring";
            default -> "";
        };
    }

    /** Zone id -> name (English), ported from Wowhead.GetZoneName (Wowhead.cs:1979-2136). */
    static String getZoneName(String zoneId) {
        return switch (zoneId) {
            // Old World
            case "-364" -> "Darkmoon Faire";
            case "12" -> "Elwynn Forest";
            case "38" -> "Loch Modan";
            case "40" -> "Westfall";
            case "133" -> "Gnomeregan";
            case "209" -> "Shadowfang Keep";
            case "491" -> "Razorfen Kraul";
            case "717" -> "The Stockade";
            case "718" -> "Wailing Caverns";
            case "719" -> "Blackfathom Deeps";
            case "722" -> "Razorfen Downs";
            case "796" -> "Scarlet Monastery";
            case "978" -> "Zul'Farrak";
            case "1337" -> "Uldaman";
            case "1417" -> "Sunken Temple";
            case "1519" -> "Stormwind City";
            case "1581" -> "The Deadmines";
            case "1583" -> "Blackrock Spire";
            case "1584" -> "Blackrock Depths";
            case "2017" -> "Stratholme";
            case "2057" -> "Scholomance";
            case "2100" -> "Maraudon";
            case "2159" -> "Onyxia's Lair";
            case "2437" -> "Ragefire Chasm";
            case "2557" -> "Dire Maul";
            case "2597" -> "Alterac Valley";
            case "2677" -> "Blackwing Lair";
            case "2918" -> "Champion's Hall";
            // TBC
            case "2366" -> "The Black Morass";
            case "2367" -> "Old Hillsbrad Foothills";
            case "2562" -> "Karazhan";
            case "3520" -> "Shadowmoon Valley";
            case "3523" -> "Netherstorm";
            case "3562" -> "Hellfire Ramparts";
            case "3606" -> "Hyjal Summit";
            case "3607" -> "Serpentshrine Cavern";
            case "3618" -> "Gruul's Lair";
            case "3703" -> "Shattrath City";
            case "3713" -> "The Blood Furnace";
            case "3714" -> "The Shattered Halls";
            case "3715" -> "The Steamvault";
            case "3716" -> "The Underbog";
            case "3717" -> "The Slave Pens";
            case "3789" -> "Shadow Labyrinth";
            case "3790" -> "Auchenai Crypts";
            case "3791" -> "Sethekk Halls";
            case "3792" -> "Mana-Tombs";
            case "3805" -> "Zul'Aman";
            case "3836" -> "Magtheridon's Lair";
            case "3842" -> "The Eye";
            case "3846" -> "The Arcatraz";
            case "3847" -> "The Botanica";
            case "3849" -> "The Mechanar";
            case "3959" -> "Black Temple";
            case "4075" -> "Sunwell Plateau";
            case "4080" -> "Isle of Quel'Danas";
            case "4095" -> "Magisters' Terrace";
            // WotLK
            case "65" -> "Dragonblight";
            case "66" -> "Zul'Drak";
            case "67" -> "The Storm Peaks";
            case "206" -> "Utgarde Keep";
            case "210" -> "Icecrown";
            case "394" -> "Grizzly Hills";
            case "495" -> "Howling Fjord";
            case "1196" -> "Utgarde Pinnacle";
            case "2817" -> "Crystalsong Forest";
            case "2917" -> "Hall of Legends";
            case "3456" -> "Naxxramas";
            case "3477" -> "Azjol-Nerub";
            case "3537" -> "Borean Tundra";
            case "3711" -> "Sholazar Basin";
            case "4100" -> "The Culling of Stratholme";
            case "4120" -> "The Nexus";
            case "4196" -> "Drak'Tharon Keep";
            case "4197" -> "Wintergrasp";
            case "4228" -> "The Oculus";
            case "4264" -> "Halls of Stone";
            case "4272" -> "Halls of Lightning";
            case "4273" -> "Ulduar";
            case "4298" -> "The Scarlet Enclave";
            case "4375" -> "Gundrak";
            case "4395" -> "Dalaran";
            case "4415" -> "The Violet Hold";
            case "4493" -> "The Obsidian Sanctum";
            case "4494" -> "Ahn'kahet: The Old Kingdom";
            case "4500" -> "The Eye of Eternity";
            case "4603" -> "Vault of Archavon";
            case "4722" -> "Trial of the Crusader";
            case "4723" -> "Trial of the Champion";
            case "4809" -> "Forge of Souls";
            case "4812" -> "Icecrown Citadel";
            case "4813" -> "Pit of Saron";
            case "4820" -> "Halls of Reflection";
            case "4987" -> "Ruby Sanctum";
            default -> "Unknown - " + zoneId;
        };
    }

    /** Faction name table, ported from Wowhead.GetItemFactionVendorInfo (Wowhead.cs:2549-2645). */
    static String[] getItemFactionVendorInfo(String repReqId, String repLevel) {
        String[] result = { "Unknown Faction", "Unknown Vendor", "Unknown Zone", "Unknown Level" };
        switch (repReqId) {
            case "1037" -> result[0] = "Alliance Vanguard";
            case "1106" -> result[0] = "Argent Crusade";
            case "529" -> result[0] = "Argent Dawn";
            case "1012" -> result[0] = "Ashtongue Deathsworn";
            case "87" -> result[0] = "Bloodsail Buccaneers";
            case "21" -> result[0] = "Booty Bay";
            case "910" -> result[0] = "Brood of Nozdormu";
            case "609" -> result[0] = "Cenarion Circle";
            case "942" -> result[0] = "Cenarion Expedition";
            case "909" -> result[0] = "Darkmoon Faire";
            case "530" -> result[0] = "Darkspear Trolls";
            case "69" -> result[0] = "Darnassus";
            case "577" -> result[0] = "Everlook";
            case "930" -> result[0] = "Exodar";
            case "1068" -> result[0] = "Explorers' League";
            case "1104" -> result[0] = "Frenzyheart Tribe";
            case "729" -> result[0] = "Frostwolf Clan";
            case "369" -> result[0] = "Gadgetzan";
            case "92" -> result[0] = "Gelkis Clan Centaur";
            case "54" -> result[0] = "Gnomeregan Exiles";
            case "946" -> result[0] = "Honor Hold";
            case "1052" -> result[0] = "Horde Expedition";
            case "749" -> result[0] = "Hydraxian Waterlords";
            case "47" -> result[0] = "Ironforge";
            case "989" -> result[0] = "Keepers of Time";
            case "1090" -> result[0] = "Kirin Tor";
            case "1098" -> result[0] = "Knights of the Ebon Blade";
            case "978" -> result[0] = "Kurenai";
            case "1011" -> result[0] = "Lower City";
            case "93" -> result[0] = "Magram Clan Centaur";
            case "1015" -> result[0] = "Netherwing";
            case "1038" -> result[0] = "Ogri'la";
            case "76" -> result[0] = "Orgrimmar";
            case "470" -> result[0] = "Ratchet";
            case "349" -> result[0] = "Ravenholdt";
            case "1031" -> result[0] = "Sha'tari Skyguard";
            case "1077" -> result[0] = "Shattered Sun Offensive";
            case "809" -> result[0] = "Shen'dralar";
            case "911" -> result[0] = "Silvermoon City";
            case "890" -> result[0] = "Silverwing Sentinels";
            case "970" -> result[0] = "Sporeggar";
            case "730" -> result[0] = "Stormpike Guard";
            case "72" -> result[0] = "Stormwind";
            case "70" -> result[0] = "Syndicate";
            case "932" -> result[0] = "The Aldor";
            case "1156" -> result[0] = "The Ashen Verdict";
            case "933" -> result[0] = "The Consortium";
            case "510" -> result[0] = "The Defilers";
            case "1126" -> result[0] = "The Frostborn";
            case "1067" -> result[0] = "The Hand of Vengeance";
            case "1073" -> result[0] = "The Kalu'ak";
            case "509" -> result[0] = "The League of Arathor";
            case "941" -> result[0] = "The Mag'har";
            case "1105" -> result[0] = "The Oracles";
            case "990" -> result[0] = "The Scale of the Sands";
            case "934" -> result[0] = "The Scryers";
            case "935" -> result[0] = "The Sha'tar";
            case "1094" -> result[0] = "The Silver Covenant";
            case "1119" -> result[0] = "The Sons of Hodir";
            case "1124" -> result[0] = "The Sunreavers";
            case "1064" -> result[0] = "The Taunka";
            case "967" -> result[0] = "The Violet Eye";
            case "1091" -> result[0] = "The Wyrmrest Accord";
            case "59" -> result[0] = "Thorium Brotherhood";
            case "947" -> result[0] = "Thrallmar";
            case "81" -> result[0] = "Thunder Bluff";
            case "576" -> result[0] = "Timbermaw Hold";
            case "922" -> result[0] = "Tranquillien";
            case "68" -> result[0] = "Undercity";
            case "1050" -> result[0] = "Valiance Expedition";
            case "1085" -> result[0] = "Warsong Offensive";
            case "889" -> result[0] = "Warsong Outriders";
            case "589" -> result[0] = "Wintersaber Trainers";
            case "270" -> result[0] = "Zandalar Tribe";
            default -> {
                // Unknown faction id; the original kept "Unknown Faction".
            }
        }
        switch (repLevel) {
            case "4" -> result[3] = "Friendly";
            case "5" -> result[3] = "Honored";
            case "6" -> result[3] = "Revered";
            case "7" -> result[3] = "Exalted";
            default -> {
                // Unknown level; the original kept "0".
            }
        }
        if ("".equals(result[0])) {
            result[0] = "Unknown Faction";
        }
        if ("".equals(result[1])) {
            result[1] = "Unknown Vendor";
        }
        if ("".equals(result[2])) {
            result[2] = "Unknown Zone";
        }
        if ("".equals(result[3])) {
            result[3] = "0";
        }
        return result;
    }
}
