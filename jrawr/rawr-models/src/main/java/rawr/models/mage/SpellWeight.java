// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/CalculationOptionsMage.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class SpellWeight {
    public SpellId spell;
    public float weight;

    public SpellId getSpell() {
        return spell;
    }

    public void setSpell(SpellId spell) {
        this.spell = spell;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
}
