// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class DeepFreezeTemplate extends SpellTemplate {
    private static SpellData getMaxRankSpellData(CalculationOptionsMage options) {
        if (options.getBeta()) {
            return spellDataBeta[options.getPlayerLevel() - 80];
        } else {
            return spellData[options.getPlayerLevel() - 70];
        }
    }

    public static final SpellData[] spellData = new SpellData[11];
    public static final SpellData[] spellDataBeta = new SpellData[6];

    static {
        spellData[0] = makeSpellData((int)(0.09 * BaseMana.get(70)), 1919, 2191, 7.5f / 3.5f, 0, 0);
        spellData[1] = makeSpellData((int)(0.09 * BaseMana.get(71)), 1964, 2236, 7.5f / 3.5f, 0, 0);
        spellData[2] = makeSpellData((int)(0.09 * BaseMana.get(72)), 2009, 2281, 7.5f / 3.5f, 0, 0);
        spellData[3] = makeSpellData((int)(0.09 * BaseMana.get(73)), 2054, 2326, 7.5f / 3.5f, 0, 0);
        spellData[4] = makeSpellData((int)(0.09 * BaseMana.get(74)), 2099, 2371, 7.5f / 3.5f, 0, 0);
        spellData[5] = makeSpellData((int)(0.09 * BaseMana.get(75)), 2144, 2416, 7.5f / 3.5f, 0, 0);
        spellData[6] = makeSpellData((int)(0.09 * BaseMana.get(76)), 2189, 2461, 7.5f / 3.5f, 0, 0);
        spellData[7] = makeSpellData((int)(0.09 * BaseMana.get(77)), 2234, 2506, 7.5f / 3.5f, 0, 0);
        spellData[8] = makeSpellData((int)(0.09 * BaseMana.get(78)), 2279, 2551, 7.5f / 3.5f, 0, 0);
        spellData[9] = makeSpellData((int)(0.09 * BaseMana.get(79)), 2324, 2596, 7.5f / 3.5f, 0, 0);
        spellData[10] = makeSpellData((int)(0.09 * BaseMana.get(80)), 2369, 2641, 7.5f / 3.5f, 0, 0);
        spellDataBeta[0] = makeSpellData((int)(0.09 * BaseMana.get(80)), 2369, 2641, 7.5f / 3.5f, 0, 0);
        spellDataBeta[1] = makeSpellData((int)(0.09 * BaseMana.get(81)), 2369, 2641, 7.5f / 3.5f, 0, 0);
        spellDataBeta[2] = makeSpellData((int)(0.09 * BaseMana.get(82)), 2369, 2641, 7.5f / 3.5f, 0, 0);
        spellDataBeta[3] = makeSpellData((int)(0.09 * BaseMana.get(83)), 2369, 2641, 7.5f / 3.5f, 0, 0);
        spellDataBeta[4] = makeSpellData((int)(0.09 * BaseMana.get(84)), 2369, 2641, 7.5f / 3.5f, 0, 0);
        spellDataBeta[5] = makeSpellData((int)(0.09 * BaseMana.get(85)), 2369, 2641, 7.5f / 3.5f, 0, 0);
    }

    public void initialize(Solver solver) {
        setName("Deep Freeze");
        initializeCastTime(false, true, 0, 30);
        initializeDamage(solver, false, 30, MagicSchool.Frost, getMaxRankSpellData(solver.getCalculationOptions()));
        if (solver.getMageTalents().isGlyphOfDeepFreeze()) {
            setRange(getRange() + 10f);
        }
        setDirty(false);
    }

    @Override
    public Spell getSpell(CastingState castingState) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        spell.calculateDerivedStats(castingState);
        return spell;
    }
}
