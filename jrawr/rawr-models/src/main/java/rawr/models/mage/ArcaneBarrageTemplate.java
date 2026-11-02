// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class ArcaneBarrageTemplate extends SpellTemplate {
    public static final SpellData[] spellData = new SpellData[11];
    public static final SpellData[] spellDataBeta = new SpellData[6];

    static {
        spellData[0] = makeSpellData((int)(0.18 * BaseMana.get(70)), 709, 865, 3.0f / 3.5f, 0, 0);
        spellData[1] = makeSpellData((int)(0.18 * BaseMana.get(71)), 724, 881, 3.0f / 3.5f, 0, 0);
        spellData[2] = makeSpellData((int)(0.18 * BaseMana.get(72)), 740, 896, 3.0f / 3.5f, 0, 0);
        spellData[3] = makeSpellData((int)(0.18 * BaseMana.get(73)), 755, 912, 3.0f / 3.5f, 0, 0);
        spellData[4] = makeSpellData((int)(0.18 * BaseMana.get(74)), 771, 927, 3.0f / 3.5f, 0, 0);
        spellData[5] = makeSpellData((int)(0.18 * BaseMana.get(75)), 786, 943, 3.0f / 3.5f, 0, 0);
        spellData[6] = makeSpellData((int)(0.18 * BaseMana.get(76)), 802, 958, 3.0f / 3.5f, 0, 0);
        spellData[7] = makeSpellData((int)(0.18 * BaseMana.get(77)), 802, 958, 3.0f / 3.5f, 0, 0);
        spellData[8] = makeSpellData((int)(0.18 * BaseMana.get(78)), 802, 958, 3.0f / 3.5f, 0, 0);
        spellData[9] = makeSpellData((int)(0.18 * BaseMana.get(79)), 802, 958, 0.95f * 3.0f / 3.5f, 0, 0);
        spellData[10] = makeSpellData((int)(0.18 * BaseMana.get(80)), 936, 1144, 2.5f / 3.5f, 0, 0);
        spellDataBeta[0] = makeSpellData((int)(0.18 * BaseMana.get(80)), 821, 1003, 2.5f / 3.5f, 0, 0);
        spellDataBeta[1] = makeSpellData((int)(0.18 * BaseMana.get(81)), 840, 1026, 2.5f / 3.5f, 0, 0);
        spellDataBeta[2] = makeSpellData((int)(0.18 * BaseMana.get(82)), 821, 1049, 2.5f / 3.5f, 0, 0);
        spellDataBeta[3] = makeSpellData((int)(0.18 * BaseMana.get(83)), 859, 1072, 2.5f / 3.5f, 0, 0);
        spellDataBeta[4] = makeSpellData((int)(0.18 * BaseMana.get(84)), 878, 1095, 2.5f / 3.5f, 0, 0);
        spellDataBeta[5] = makeSpellData((int)(0.18 * BaseMana.get(85)), 897, 1118, 2.5f / 3.5f, 0, 0);
    }

    private static SpellData getMaxRankSpellData(CalculationOptionsMage options) {
        if (options.getBeta()) {
            return spellDataBeta[options.getPlayerLevel() - 80];
        } else {
            return spellData[options.getPlayerLevel() - 70];
        }
    }

    public Spell getSpell(CastingState castingState, float arcaneBlastDebuff) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        spell.additiveSpellModifier += arcaneBlastDamageMultiplier * arcaneBlastDebuff;
        spell.spellModifier *= (1 + tormentTheWeak * castingState.getSnaredTime());
        spell.calculateDerivedStats(castingState);
        return spell;
    }

    private float arcaneBlastDamageMultiplier;
    private float tormentTheWeak;

    public void initialize(Solver solver) {
        setName("Arcane Barrage");
        initializeCastTime(false, true, 0, 3);
        initializeDamage(solver, false, 30, MagicSchool.Arcane, getMaxRankSpellData(solver.getCalculationOptions()));
        tormentTheWeak = 0.04f * solver.getMageTalents().getTormentTheWeak();
        arcaneBlastDamageMultiplier = solver.getMageTalents().isGlyphOfArcaneBlast() ? 0.18f : 0.15f;
        if (solver.getMageTalents().isGlyphOfArcaneBarrage()) {
            setBaseCostAmplifier(getBaseCostAmplifier() * 0.8f);
        }
        setDirty(false);
    }
}
