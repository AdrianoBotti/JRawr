// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class IceLanceTemplate extends SpellTemplate {
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
        spellData[0] = makeSpellData((int)(0.06 * BaseMana.get(70)), 161, 187, 0.1429f, 0, 0);
        spellData[1] = makeSpellData((int)(0.06 * BaseMana.get(71)), 161, 187, 0.1429f, 0, 0);
        spellData[2] = makeSpellData((int)(0.06 * BaseMana.get(72)), 182, 210, 0.1429f, 0, 0);
        spellData[3] = makeSpellData((int)(0.06 * BaseMana.get(73)), 182, 210, 0.1429f, 0, 0);
        spellData[4] = makeSpellData((int)(0.06 * BaseMana.get(74)), 182, 210, 0.1429f, 0, 0);
        spellData[5] = makeSpellData((int)(0.06 * BaseMana.get(75)), 182, 210, 0.1429f, 0, 0);
        spellData[6] = makeSpellData((int)(0.06 * BaseMana.get(76)), 182, 210, 0.1429f, 0, 0);
        spellData[7] = makeSpellData((int)(0.06 * BaseMana.get(77)), 182, 210, 0.1429f, 0, 0);
        spellData[8] = makeSpellData((int)(0.06 * BaseMana.get(78)), 221, 255, 0.1429f, 0, 0);
        spellData[9] = makeSpellData((int)(0.06 * BaseMana.get(79)), 221, 255, 0.1429f, 0, 0);
        spellData[10] = makeSpellData((int)(0.06 * BaseMana.get(80)), 223, 258, 0.1429f, 0, 0);
        spellDataBeta[0] = makeSpellData((int)(0.06 * BaseMana.get(80)), 215, 215, 0.1429f, 0, 0);
        spellDataBeta[1] = makeSpellData((int)(0.06 * BaseMana.get(81)), 220, 220, 0.1429f, 0, 0);
        spellDataBeta[2] = makeSpellData((int)(0.06 * BaseMana.get(82)), 225, 225, 0.1429f, 0, 0);
        spellDataBeta[3] = makeSpellData((int)(0.06 * BaseMana.get(83)), 230, 230, 0.1429f, 0, 0);
        spellDataBeta[4] = makeSpellData((int)(0.06 * BaseMana.get(84)), 235, 235, 0.1429f, 0, 0);
        spellDataBeta[5] = makeSpellData((int)(0.06 * BaseMana.get(85)), 240, 240, 0.1429f, 0, 0);
    }

    @Override
    public Spell getSpell(CastingState castingState) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        if (castingState.getFrozen()) {
            if (castingState.getMageTalents().isGlyphOfIceLance() && castingState.getCalculationOptions().getTargetLevel() > castingState.getCalculationOptions().getPlayerLevel()) {
                spell.spellModifier *= 4;
            } else {
                spell.spellModifier *= 3;
            }
        }
        spell.calculateDerivedStats(castingState);
        return spell;
    }

    public void initialize(Solver solver) {
        setName("Ice Lance");
        initializeCastTime(false, true, 0, 0);
        initializeDamage(solver, false, 30, MagicSchool.Frost, getMaxRankSpellData(solver.getCalculationOptions()));
        setBaseAdditiveSpellModifier(getBaseAdditiveSpellModifier() + 0.02f * solver.getMageTalents().getSpellImpact());
        setBaseSpellModifier(getBaseSpellModifier() * (1 + 0.01f * solver.getMageTalents().getChilledToTheBone()));
        setDirty(false);
    }
}
