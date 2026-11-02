// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/Totem.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.elemental;

public class Totem extends Spell {
    protected float averageUptime;

    public Totem() {
        super();
    }

    @Override
    protected void setBaseValues() {
        super.setBaseValues();
        shortName = "Totem";
        castTime = 0f;
        gcd = 1f;
    }

    @Override
    public float getCCCritChance() {
        return 0f;
    }

    @Override
    public void initialize(ISpellArgs args) {
        super.initialize(args);
    }

    public float getAverageUptime() {
        return averageUptime;
    }

    public void setAverageUptime(float value) {
        averageUptime = value;
    }
}
