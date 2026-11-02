// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/SearingTotem.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the C# `+`/`*` operator overloads are omitted (never used by the model). The constructor
// deliberately does not initialize (C# `SearingTotem(ISpellArgs) : base()` only calls the base
// constructor); SpellBox.Update performs the initialization.
package rawr.models.elemental;

public class SearingTotem extends Totem {
    public SearingTotem(ISpellArgs args) {
        super();
    }

    @Override
    protected void setBaseValues() {
        super.setBaseValues();

        periodicTick = 105f; // 105 is the average base damage it deals (90-120 being the full range)
        dotSpCoef = 1f / 6f;
        periodicTicks = 27f;
        periodicTickTime = 2.2f;
        manaCost = .07f * Constants.BaseMana;
        shortName = "ST";
        dotCanCrit = 1;
    }

    @Override
    public void initialize(ISpellArgs args) {
        manaCost *= 1 - (.05f * args.getTalents().getTotemicFocus());
        totalCoef *= 1 + (.05f * args.getTalents().getCallOfFlame());

        super.initialize(args);
    }
}
