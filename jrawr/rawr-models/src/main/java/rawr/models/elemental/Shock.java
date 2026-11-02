// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/Shock.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.elemental;

public class Shock extends Spell {
    public Shock() {
        super();
    }

    @Override
    protected void setBaseValues() {
        super.setBaseValues();
        shortName = "Shock";
    }
}
