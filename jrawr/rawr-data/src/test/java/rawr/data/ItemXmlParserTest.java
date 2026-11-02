package rawr.data;

import org.junit.jupiter.api.Test;
import rawr.base.items.sources.BindsOn;
import rawr.base.items.sources.CraftedItem;
import rawr.base.items.Item;
import rawr.base.items.sources.ItemLocation;
import rawr.base.items.ItemQuality;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.items.sources.VendorItem;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Offline parser tests against committed Wowhead/AoWoW payloads (one {@code <aowow>} recorded from
 * wotlkdb.com, one recorded from evowow, and their {@code <wowhead>} shapes). No network access.
 */
class ItemXmlParserTest {

    @Test
    void parsesAowowGemPayload() throws Exception {
        Item item = parse("payloads/aowow_gem_41380.xml");

        assertThat(item.getId()).isEqualTo(41380);
        assertThat(item.getName()).isEqualTo("Austere Earthsiege Diamond");
        assertThat(item.getItemLevel()).isEqualTo(80);
        assertThat(item.getQuality()).isEqualTo(ItemQuality.Rare);
        assertThat(item.getIconPath()).isEqualTo("inv_jewelcrafting_shadowspirit_02");
        assertThat(item.getDisplayId()).isEqualTo(54467);
        assertThat(item.getSlot()).isEqualTo(ItemSlot.Meta);
        assertThat(item.getType()).isEqualTo(ItemType.None);
        assertThat(item.getStats().getStamina()).isEqualTo(32f);
        assertThat(item.getStats().getSpellPower()).isEqualTo(0f);
        assertThat(item.getBind()).isEqualTo(BindsOn.None);
        assertThat(item.isUnique()).isFalse();
        assertThat(item.getSetName()).isNull();

        ItemLocation location = item.getLocationInfo()[0];
        assertThat(location).isInstanceOf(CraftedItem.class);
        CraftedItem crafted = (CraftedItem) location;
        assertThat(crafted.getSpellName()).isEqualTo("Austere Earthsiege Diamond");
        assertThat(crafted.getSkill()).isEqualTo("Jewelcrafting");
    }

    @Test
    void parsesWowheadGemPayload() throws Exception {
        Item item = parse("payloads/wowhead_gem_41380.xml");

        assertThat(item.getId()).isEqualTo(41380);
        assertThat(item.getName()).isEqualTo("Austere Earthsiege Diamond");
        assertThat(item.getItemLevel()).isEqualTo(80);
        assertThat(item.getQuality()).isEqualTo(ItemQuality.Rare);
        assertThat(item.getSlot()).isEqualTo(ItemSlot.Meta);
        assertThat(item.getStats().getStamina()).isEqualTo(32f);

        ItemLocation location = item.getLocationInfo()[0];
        assertThat(location).isInstanceOf(CraftedItem.class);
        assertThat(((CraftedItem) location).getSkill()).isEqualTo("Jewelcrafting");
    }

    @Test
    void parsesAowowHoodWithSocketsSetNameAndEnchantmentSocketBonus() throws Exception {
        Item item = parse("payloads/aowow_hood_51281.xml");

        assertThat(item.getId()).isEqualTo(51281);
        assertThat(item.getName()).isEqualTo("Sanctified Bloodmage Hood");
        assertThat(item.getItemLevel()).isEqualTo(277);
        assertThat(item.getQuality()).isEqualTo(ItemQuality.Epic);
        assertThat(item.getIconPath()).isEqualTo("inv_helmet_150black");
        assertThat(item.getSlot()).isEqualTo(ItemSlot.Head);
        assertThat(item.getType()).isEqualTo(ItemType.Cloth);
        assertThat(item.getBind()).isEqualTo(BindsOn.BoP);
        assertThat(item.getStats().getArmor()).isEqualTo(300f);
        assertThat(item.getStats().getStamina()).isEqualTo(139f);
        assertThat(item.getStats().getIntellect()).isEqualTo(139f);
        assertThat(item.getStats().getCritRating()).isEqualTo(122f);
        assertThat(item.getStats().getHasteRating()).isEqualTo(98f);
        assertThat(item.getStats().getSpellPower()).isEqualTo(186f);

        assertThat(item.getSocketColor1()).isEqualTo(ItemSlot.Meta);
        assertThat(item.getSocketColor2()).isEqualTo(ItemSlot.Blue);
        assertThat(item.getSocketColor3()).isEqualTo(ItemSlot.None);
        assertThat(item.getSocketBonus().getSpellPower()).isEqualTo(9f);

        assertThat(item.getSetName()).isEqualTo("Bloodmage's Regalia");
        assertThat(item.getRequiredClasses()).isNull();

        // source:[5] without token page scraping -> plain vendor item, cost 0.
        ItemLocation location = item.getLocationInfo()[0];
        assertThat(location).isInstanceOf(VendorItem.class);
    }

    @Test
    void parsesWowheadHoodWithTextSocketBonusAndClassRestriction() throws Exception {
        Item item = parse("payloads/wowhead_hood_51281.xml");

        assertThat(item.getName()).isEqualTo("Sanctified Bloodmage Hood");
        assertThat(item.getItemLevel()).isEqualTo(277);
        assertThat(item.getQuality()).isEqualTo(ItemQuality.Epic);
        assertThat(item.getSlot()).isEqualTo(ItemSlot.Head);
        assertThat(item.getType()).isEqualTo(ItemType.Cloth);
        assertThat(item.getBind()).isEqualTo(BindsOn.BoP);
        assertThat(item.getStats().getSpellPower()).isEqualTo(186f);

        assertThat(item.getSocketColor1()).isEqualTo(ItemSlot.Meta);
        assertThat(item.getSocketColor2()).isEqualTo(ItemSlot.Blue);
        assertThat(item.getSocketBonus().getSpellPower()).isEqualTo(9f);

        assertThat(item.getSetName()).isEqualTo("Bloodmage's Regalia");
        assertThat(item.getRequiredClasses()).isEqualTo("Mage");
    }

    @Test
    void nanAndInfinityStringsAreKeptLikeConvertToSingle() throws Exception {
        String xml = "<wowhead><item id=\"123\"><name><![CDATA[Numeric Test]]></name>"
                + "<quality id=\"4\">Epic</quality><icon>inv_test</icon>"
                + "<htmlTooltip><![CDATA[<table><tr><td>Binds when picked up</td></tr></table>]]></htmlTooltip>"
                + "<json><![CDATA[\"classs\":4,\"id\":123,\"level\":80,\"name\":\"Numeric Test\",\"slot\":1,\"subclass\":1]]></json>"
                + "<jsonEquip><![CDATA[\"sta\":\"NaN\",\"int\":\"Infinity\",\"armor\":300]]></jsonEquip>"
                + "</item></wowhead>";

        Item item = ItemXmlParser.parse(xml);

        assertThat(item.getStats().getStamina()).isNaN();
        assertThat(item.getStats().getIntellect()).isInfinite();
        assertThat(item.getStats().getArmor()).isEqualTo(300f);
    }

    @Test
    void jsonEquipOverridesJsonValues() throws Exception {
        String xml = "<aowow><item id=\"321\"><name><![CDATA[Override Test]]></name>"
                + "<quality id=\"4\">Epic</quality><icon>inv_test</icon>"
                + "<htmlTooltip><![CDATA[<table><tr><td>Head</td></tr></table>]]></htmlTooltip>"
                + "<json><![CDATA[\"classs\":4,\"int\":10,\"level\":80,\"slot\":1,\"subclass\":1]]></json>"
                + "<jsonEquip><![CDATA[\"int\":25]]></jsonEquip>"
                + "</item></aowow>";

        Item item = ItemXmlParser.parse(xml);

        assertThat(item.getStats().getIntellect()).isEqualTo(25f);
    }

    @Test
    void itemNotFoundPayloadReturnsNull() throws Exception {
        String xml = "<wowhead><error>Item not found! <a href=\"/\">search</a></error></wowhead>";
        assertThat(ItemXmlParser.parse(xml)).isNull();
    }

    @Test
    void malformedOrItemlessPayloadsThrow() {
        assertThatThrownBy(() -> ItemXmlParser.parse("<wowhead><item id=\"1\">"))
                .isInstanceOf(ItemXmlParseException.class);
        assertThatThrownBy(() -> ItemXmlParser.parse("<wowhead><foo/></wowhead>"))
                .isInstanceOf(ItemXmlParseException.class)
                .hasMessageContaining("item");
        assertThatThrownBy(() -> ItemXmlParser.parse(""))
                .isInstanceOf(ItemXmlParseException.class);
        assertThatThrownBy(() -> ItemXmlParser.parse(null))
                .isInstanceOf(ItemXmlParseException.class);
    }

    private static Item parse(String resource) throws Exception {
        try (InputStream in = ItemXmlParserTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as("test payload %s", resource).isNotNull();
            return ItemXmlParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
