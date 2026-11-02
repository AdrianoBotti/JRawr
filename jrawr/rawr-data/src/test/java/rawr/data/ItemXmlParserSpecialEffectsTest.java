package rawr.data;

import org.junit.jupiter.api.Test;
import rawr.base.items.Item;
import rawr.base.items.ItemSlot;
import rawr.base.effects.SpecialEffect;
import rawr.base.effects.Trigger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline parser tests for the tooltip SpecialEffects wiring of {@link ItemXmlParser}: a recorded
 * {@code <aowow>} proc/use trinket and a meta gem. No network access.
 */
class ItemXmlParserSpecialEffectsTest {

    @Test
    void aowowProcTrinketCarriesUseAndEquipEffects() throws Exception {
        Item item = parse("payloads/aowow_trinket_99010.xml");

        assertThat(item.getSlot()).isEqualTo(ItemSlot.Trinket);
        assertThat(item.getStats().getIntellect()).isEqualTo(150f);

        List<SpecialEffect> effects = effects(item);
        assertThat(effects).hasSize(2);
        // Like Wowhead.GetItem: every use line runs before every equip line.
        SpecialEffect use = effects.get(0);
        assertThat(use.getTrigger()).isEqualTo(Trigger.Use);
        assertThat(use.getDuration()).isEqualTo(20f);
        assertThat(use.getCooldown()).isEqualTo(120f);
        assertThat(use.getChance()).isEqualTo(1f);
        assertThat(use.getMaxStack()).isEqualTo(1);
        assertThat(use.getStats().getAttackPower()).isEqualTo(1215f);

        SpecialEffect equip = effects.get(1);
        assertThat(equip.getTrigger()).isEqualTo(Trigger.DamageSpellHit);
        assertThat(equip.getDuration()).isEqualTo(10f);
        assertThat(equip.getCooldown()).isEqualTo(0f);
        assertThat(equip.getChance()).isEqualTo(1f);
        assertThat(equip.getMaxStack()).isEqualTo(10);
        assertThat(equip.getStats().getSpellPower()).isEqualTo(20f);
    }

    @Test
    void aowowMetaGemCarriesTheRestoreManaEffect() throws Exception {
        Item item = parse("payloads/aowow_gem_41398.xml");

        assertThat(item.getSlot()).isEqualTo(ItemSlot.Meta);
        assertThat(item.getStats().getIntellect()).isEqualTo(21f);

        List<SpecialEffect> effects = effects(item);
        assertThat(effects).hasSize(1);
        SpecialEffect effect = effects.get(0);
        assertThat(effect.getTrigger()).isEqualTo(Trigger.SpellCast);
        assertThat(effect.getDuration()).isEqualTo(0f);
        assertThat(effect.getCooldown()).isEqualTo(15f);
        assertThat(effect.getChance()).isEqualTo(0.05f);
        assertThat(effect.getStats().getManaRestore()).isEqualTo(600f);
        assertThat(item.getStats().getManaRestoreOnCast_5_15()).isEqualTo(600f);
    }

    private static List<SpecialEffect> effects(Item item) {
        List<SpecialEffect> effects = new ArrayList<SpecialEffect>();
        for (SpecialEffect effect : item.getStats().specialEffects()) {
            effects.add(effect);
        }
        return effects;
    }

    private static Item parse(String resource) throws Exception {
        try (InputStream in = ItemXmlParserSpecialEffectsTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as("test payload %s", resource).isNotNull();
            return ItemXmlParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
