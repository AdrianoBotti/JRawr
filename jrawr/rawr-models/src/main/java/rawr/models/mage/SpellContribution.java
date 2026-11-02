// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/Spell.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class SpellContribution implements Comparable<SpellContribution> {
    public String name;
    public float hits;
    public float crits;
    public float ticks;
    public float damage;
    public float hitDamage;
    public float critDamage;
    public float tickDamage;
    public float dotUptime;
    public float range;

    @Override
    public int compareTo(SpellContribution other) {
        return Float.compare(other.damage, this.damage);
    }
}
