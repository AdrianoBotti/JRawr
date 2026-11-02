package rawr.models.mage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The C# options mark the incremental-set solver fields {@code [XmlIgnore]}. They are runtime
 * state: an empty saved array must not be mistaken for an initialized incremental set.
 */
class CalculationOptionsMageXmlTest {
    @Test
    void runtimeIncrementalSetFieldsAreNotWritten() {
        CalculationOptionsMage options = new CalculationOptionsMage();
        options.setIncrementalSetStateIndexes(new int[0]);
        options.setIncrementalSetSortedStates(new int[0]);
        options.setIncrementalSetSegments(new int[0]);
        options.setIncrementalSetSpells(new CycleId[0]);
        options.setIncrementalSetVariableType(new VariableType[0]);
        options.setIncrementalSetManaSegment(new int[0]);
        options.setIncrementalSetArmor("");
        options.setSmartOptimization(true);

        String xml = options.getXml();
        assertThat(xml).doesNotContain("IncrementalSetStateIndexes");
        assertThat(xml).doesNotContain("IncrementalSetSegments");
        assertThat(xml).doesNotContain("IncrementalSetArmor");
        assertThat(xml).doesNotContain("AdviseAdvancedSolver");
    }

    @Test
    void customSpellMixRoundTripsThroughXml() {
        CalculationOptionsMage options = new CalculationOptionsMage();
        SpellWeight weight = new SpellWeight();
        weight.setSpell(SpellId.Fireball);
        weight.setWeight(0.5f);
        options.getCustomSpellMix().add(weight);
        options.setCustomSpellMixEnabled(true);
        options.setCustomSpellMixOnly(true);

        String xml = options.getXml();
        assertThat(xml).contains(
                "<SpellWeight><Spell>Fireball</Spell><Weight>0.5</Weight></SpellWeight>");
        assertThat(xml).contains("<CustomSpellMixEnabled>true</CustomSpellMixEnabled>");
        assertThat(xml).contains("<CustomSpellMixOnly>true</CustomSpellMixOnly>");
    }
}
