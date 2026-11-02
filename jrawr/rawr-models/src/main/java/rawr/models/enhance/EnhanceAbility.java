// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Enhance/Ability.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.enhance;

// C# enum with explicit values 0..13. Kept as enum; getValue() mirrors the C# underlying value.
public enum EnhanceAbility {
    None(0),
    FeralSpirits(1),
    LightningBolt(2),
    StormStrike(3),
    EarthShock(4),
    FlameShock(5),
    LavaLash(6),
    LightningShield(7),
    MagmaTotem(8),
    SearingTotem(9),
    RefreshTotems(10),
    ShamanisticRage(11),
    FireNova(12),
    FireElemental(13);

    private final int value;

    EnhanceAbility(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
