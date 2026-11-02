// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/Thunderstorm.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the C# `+`/`*` operator overloads are omitted (never used by the model).
package rawr.models.elemental;

import rawr.base.effects.SpecialEffect;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

public class Thunderstorm extends Spell {
    public Thunderstorm() {
        super();
    }

    @Override
    protected void setBaseValues() {
        super.setBaseValues();

        baseMinDamage = 1450;
        baseMaxDamage = 1656;
        castTime = 0f;
        spCoef = 1.5f / 7f * .9f; // NOT CORRECT YET, assuming 1.5f/7f * 0.9f (aoe with additional effect)
        manaCost = 0f;
        cooldown = 45f;
        shortName = "TS";
    }

    @Override
    public void initialize(ISpellArgs args) {
        totalCoef += .01f * args.getTalents().getConcussion();
        crit += .05f * args.getTalents().getCallOfThunder();
        crit += .01f * args.getTalents().getTidalMastery();
        spellPower += args.getStats().getSpellNatureDamageRating();
        totalCoef *= 1 + args.getStats().getBonusNatureDamageMultiplier();

        if (args.getTalents().isGlyphofThunder())
            cooldown -= 10f;

        super.initialize(args);
    }

    public Thunderstorm(ISpellArgs args) {
        this();
        initialize(args);
    }

    public static float getProcsPerSecond(boolean glyphed, int fightDuration) {
        if (glyphed) {
            if (manaRestoreGlyphed == null)
                manaRestoreGlyphed = new SpecialEffect(Trigger.Use, new Stats(), 0f, 35f, 1f);
            return manaRestoreGlyphed.getAverageProcsPerSecond(35f, 1f, 1f, fightDuration);
        } else {
            if (manaRestoreUnglyphed == null)
                manaRestoreUnglyphed = new SpecialEffect(Trigger.Use, new Stats(), 0f, 45f, 1f);
            return manaRestoreUnglyphed.getAverageProcsPerSecond(45f, 1f, 1f, fightDuration);
        }
    }

    private static SpecialEffect manaRestoreGlyphed = null;
    private static SpecialEffect manaRestoreUnglyphed = null;
}
