// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/CharacterCalculationsMage.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public enum VariableType {
    None,
    IdleRegen,
    Wand,
    Evocation,
    EvocationIV,
    EvocationHero,
    EvocationIVHero,
    ManaPotion,
    ManaGem,
    Drinking,
    TimeExtension,
    AfterFightRegen,
    ManaOverflow,
    Spell,
    SummonWaterElemental,
    SummonMirrorImage,
    ConjureManaGem;

    public static final int COUNT = values().length;
}
