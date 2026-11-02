// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class ScorchTemplate extends SpellTemplate {
    public static final SpellData[] spellData = new SpellData[11];
    public static final SpellData[] spellDataBeta = new SpellData[6];

    static {
        spellData[0] = makeSpellData((int)(0.08 * BaseMana.get(70)), 305, 361, 1.5f / 3.5f, 0, 0);
        spellData[1] = makeSpellData((int)(0.08 * BaseMana.get(71)), 307, 364, 1.5f / 3.5f, 0, 0);
        spellData[2] = makeSpellData((int)(0.08 * BaseMana.get(72)), 310, 366, 1.5f / 3.5f, 0, 0);
        spellData[3] = makeSpellData((int)(0.08 * BaseMana.get(73)), 321, 379, 1.5f / 3.5f, 0, 0);
        spellData[4] = makeSpellData((int)(0.08 * BaseMana.get(74)), 323, 382, 1.5f / 3.5f, 0, 0);
        spellData[5] = makeSpellData((int)(0.08 * BaseMana.get(75)), 326, 385, 1.5f / 3.5f, 0, 0);
        spellData[6] = makeSpellData((int)(0.08 * BaseMana.get(76)), 328, 387, 1.5f / 3.5f, 0, 0);
        spellData[7] = makeSpellData((int)(0.08 * BaseMana.get(77)), 331, 390, 1.5f / 3.5f, 0, 0);
        spellData[8] = makeSpellData((int)(0.08 * BaseMana.get(78)), 376, 444, 1.5f / 3.5f, 0, 0);
        spellData[9] = makeSpellData((int)(0.08 * BaseMana.get(79)), 379, 448, 1.5f / 3.5f, 0, 0);
        spellData[10] = makeSpellData((int)(0.08 * BaseMana.get(80)), 382, 451, 1.5f / 3.5f, 0, 0);
        spellDataBeta[0] = makeSpellData((int)(0.08 * BaseMana.get(80)), 312, 368, 0.429f, 0, 0);
        spellDataBeta[1] = makeSpellData((int)(0.08 * BaseMana.get(81)), 319, 377, 0.429f, 0, 0);
        spellDataBeta[2] = makeSpellData((int)(0.08 * BaseMana.get(82)), 325, 385, 0.429f, 0, 0);
        spellDataBeta[3] = makeSpellData((int)(0.08 * BaseMana.get(83)), 333, 395, 0.429f, 0, 0);
        spellDataBeta[4] = makeSpellData((int)(0.08 * BaseMana.get(84)), 340, 404, 0.429f, 0, 0);
        spellDataBeta[5] = makeSpellData((int)(0.08 * BaseMana.get(85)), 347, 413, 0.429f, 0, 0);
    }

    private static SpellData getMaxRankSpellData(CalculationOptionsMage options) {
        if (options.getBeta()) {
            return spellDataBeta[options.getPlayerLevel() - 80];
        } else {
            return spellData[options.getPlayerLevel() - 70];
        }
    }


    public Spell getSpell(CastingState castingState, boolean clearcastingActive) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        spell.calculateManualClearcasting(true, false, clearcastingActive);
        spell.calculateDerivedStats(castingState);
        spell.calculateManualClearcastingCost(castingState.getSolver(), false, true, false, clearcastingActive);
        return spell;
    }

    public void initialize(Solver solver) {
        setName("Scorch");
        initializeCastTime(false, false, 1.5f, 0);
        initializeDamage(solver, false, 30, MagicSchool.Fire, getMaxRankSpellData(solver.getCalculationOptions()));
        setBaseCritRate(getBaseCritRate() + 0.02f * solver.getMageTalents().getIncineration());
        setBaseCritRate(getBaseCritRate() + 0.01f * solver.getMageTalents().getImprovedScorch());
        setBaseAdditiveSpellModifier(getBaseAdditiveSpellModifier() + 0.02f * solver.getMageTalents().getSpellImpact());
        if (solver.getMageTalents().isGlyphOfImprovedScorch()) {
            setBaseSpellModifier(getBaseSpellModifier() * 1.2f);
        }
        setDirty(false);
    }
}
