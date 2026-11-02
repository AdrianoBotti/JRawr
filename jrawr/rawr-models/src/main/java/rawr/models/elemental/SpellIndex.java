// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/SpellBox.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: SpellBox.ApplyEM() is omitted (C# calls Spell.ApplyEM which throws
// NotImplementedException; no caller in the model).
package rawr.models.elemental;

public enum SpellIndex {
    LightningBolt,
    ChainLightning,
    LavaBurst,
    LavaBurstFS,
    FlameShock,
    EarthShock,
    FrostShock,
    Thunderstorm,
    FireNova,
    SearingTotem,
    MagmaTotem,
    FireElemental
}
