// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/SpellBox.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: ApplyEM() and EMapplied are omitted (see Spell).
package rawr.models.elemental;

/**
 * A container class to hold an instance of every spell that can be used in a rotation. It also
 * supplies methods to update these spells to new stats and talents. The idea is to save memory by
 * reusing the spells.
 */
public class SpellBox {
    private final Spell[] spells;

    public SpellBox(ISpellArgs args) {
        spells = new Spell[12];
        spells[SpellIndex.LightningBolt.ordinal()] = new LightningBolt(args);
        spells[SpellIndex.ChainLightning.ordinal()] = new ChainLightning(args);
        spells[SpellIndex.LavaBurst.ordinal()] = new LavaBurst(args, 0);
        spells[SpellIndex.LavaBurstFS.ordinal()] = new LavaBurst(args, 1);
        spells[SpellIndex.FlameShock.ordinal()] = new FlameShock(args);
        spells[SpellIndex.EarthShock.ordinal()] = new EarthShock(args);
        spells[SpellIndex.FrostShock.ordinal()] = new FrostShock(args);
        spells[SpellIndex.Thunderstorm.ordinal()] = new Thunderstorm(args);
        spells[SpellIndex.FireNova.ordinal()] = new FireNova(args);
        spells[SpellIndex.SearingTotem.ordinal()] = new SearingTotem(args);
        spells[SpellIndex.MagmaTotem.ordinal()] = new MagmaTotem(args);
        spells[SpellIndex.FireElemental.ordinal()] = new FireElemental(args);
    }

    public void update(ISpellArgs args) {
        for (Spell s : spells) {
            if (s != null)
                s.update(args);
        }
    }

    public Spell get(SpellIndex spellIndex) {
        return spells[spellIndex.ordinal()];
    }

    /**
     * The underlying spell array. This is a reference, not a copy.
     */
    public Spell[] getArray() {
        return spells;
    }

    public LightningBolt getLB() {
        return (LightningBolt) spells[SpellIndex.LightningBolt.ordinal()];
    }

    public ChainLightning getCL() {
        return (ChainLightning) spells[SpellIndex.ChainLightning.ordinal()];
    }

    public LavaBurst getLvB() {
        return (LavaBurst) spells[SpellIndex.LavaBurst.ordinal()];
    }

    public LavaBurst getLvBFS() {
        return (LavaBurst) spells[SpellIndex.LavaBurstFS.ordinal()];
    }

    public FlameShock getFS() {
        return (FlameShock) spells[SpellIndex.FlameShock.ordinal()];
    }

    public EarthShock getES() {
        return (EarthShock) spells[SpellIndex.EarthShock.ordinal()];
    }

    public FrostShock getFrS() {
        return (FrostShock) spells[SpellIndex.FrostShock.ordinal()];
    }

    public Thunderstorm getTS() {
        return (Thunderstorm) spells[SpellIndex.Thunderstorm.ordinal()];
    }

    public FireNova getFN() {
        return (FireNova) spells[SpellIndex.FireNova.ordinal()];
    }

    public SearingTotem getST() {
        return (SearingTotem) spells[SpellIndex.SearingTotem.ordinal()];
    }

    public MagmaTotem getMT() {
        return (MagmaTotem) spells[SpellIndex.MagmaTotem.ordinal()];
    }

    public FireElemental getFE() {
        return (FireElemental) spells[SpellIndex.FireElemental.ordinal()];
    }
}
