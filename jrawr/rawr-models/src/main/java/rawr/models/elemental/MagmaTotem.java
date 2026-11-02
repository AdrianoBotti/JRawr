// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/MagmaTotem.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the C# `+`/`*` operator overloads are omitted (never used by the model). The constructor
// deliberately does not initialize (C# `MagmaTotem(ISpellArgs) : base()` only calls the base
// constructor); SpellBox.Update performs the initialization.
package rawr.models.elemental;

public class MagmaTotem extends Totem {
    private int additionalTargets = 0;

    public MagmaTotem(ISpellArgs args) {
        super();
    }

    @Override
    protected void setBaseValues() {
        super.setBaseValues();

        dotSpCoef = 1 / 10f;

        periodicTick = 371.0f;
        periodicTicks = 10f;
        periodicTickTime = 2.0f;
        manaCost = .27f * Constants.BaseMana;
        shortName = "MT";
        dotCanCrit = 1;
    }

    @Override
    public void initialize(ISpellArgs args) {
        additionalTargets = args.getAdditionalTargets();
        if (additionalTargets < 0)
            additionalTargets = 0;
        else if (additionalTargets > 9)
            additionalTargets = 9;

        totalCoef *= 1 + (.05f * args.getTalents().getCallOfFlame());
        manaCost *= 1 - (.05f * args.getTalents().getTotemicFocus());
        totalCoef *= (1 + additionalTargets);
        shortName = "MT" + (1 + additionalTargets);

        super.initialize(args);
    }

    public int getAdditionalTargets() {
        return additionalTargets;
    }
}
