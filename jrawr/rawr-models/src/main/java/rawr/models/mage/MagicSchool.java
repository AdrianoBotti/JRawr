// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/Spell.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public enum MagicSchool {
    Physical(0), // C# value 0 (undefined); produced by casting ItemDamageType.Physical
    Holy(1),
    Fire(2),
    Nature(3),
    Frost(4),
    Shadow(5),
    Arcane(6),
    FrostFire(7);

    private final int value;

    MagicSchool(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
