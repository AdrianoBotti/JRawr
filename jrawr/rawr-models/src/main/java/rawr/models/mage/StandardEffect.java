// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/CastingState.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

// C# [Flags] enum; translated to int constants so mask arithmetic stays literal.
public final class StandardEffect {
    public static final int None = 0;
    public static final int WaterElemental = 0x1;
    public static final int PowerInfusion = 0x2;
    public static final int PotionOfSpeed = 0x4;
    public static final int ArcanePower = 0x8;
    public static final int Combustion = 0x10;
    public static final int PotionOfWildMagic = 0x20;
    public static final int Berserking = 0x40;
    public static final int FlameCap = 0x80;
    public static final int Heroism = 0x100;
    public static final int IcyVeins = 0x200;
    public static final int MoltenFury = 0x400;
    public static final int Evocation = 0x800;
    public static final int ManaGemEffect = 0x1000;
    public static final int MirrorImage = 0x2000;
    public static final int NonItemBasedMask = PowerInfusion | PotionOfSpeed | PotionOfSpeed | ArcanePower | Combustion | PotionOfWildMagic | Berserking | FlameCap | Heroism | IcyVeins | MoltenFury | WaterElemental | MirrorImage;

    private StandardEffect() {
    }
}
