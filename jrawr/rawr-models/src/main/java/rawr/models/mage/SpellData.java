// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/Spell.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class SpellData {
    public float minDamage;
    public float maxDamage;
    public float periodicDamage;
    public int cost;
    public float spellDamageCoefficient;
    public float dotDamageCoefficient;

    public float getMinDamage() {
        return this.minDamage;
    }

    public void setMinDamage(float minDamage) {
        this.minDamage = minDamage;
    }

    public float getMaxDamage() {
        return this.maxDamage;
    }

    public void setMaxDamage(float maxDamage) {
        this.maxDamage = maxDamage;
    }

    public float getPeriodicDamage() {
        return this.periodicDamage;
    }

    public void setPeriodicDamage(float periodicDamage) {
        this.periodicDamage = periodicDamage;
    }

    public int getCost() {
        return this.cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public float getSpellDamageCoefficient() {
        return this.spellDamageCoefficient;
    }

    public void setSpellDamageCoefficient(float spellDamageCoefficient) {
        this.spellDamageCoefficient = spellDamageCoefficient;
    }

    public float getDotDamageCoefficient() {
        return this.dotDamageCoefficient;
    }

    public void setDotDamageCoefficient(float dotDamageCoefficient) {
        this.dotDamageCoefficient = dotDamageCoefficient;
    }
}
