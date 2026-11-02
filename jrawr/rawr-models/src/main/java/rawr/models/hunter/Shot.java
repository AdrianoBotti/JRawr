// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/CalculationOptionsHunter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.hunter;

public class Shot {
    public Shot(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public int index = -1;
    public String name = "Invalid";

    @Override
    public String toString() {
        return name;
    }

    public int toInt() {
        return index;
    }
}
