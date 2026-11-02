// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class FireWardTemplate extends SpellTemplate {
    public static final SpellData[] spellData = new SpellData[11];
    public static final SpellData[] spellDataBeta = new SpellData[6];

    static {
        spellData[0] = makeSpellData((int)(0.16 * BaseMana.get(70)), 0, 0, 0, 0, 0);
        spellData[1] = makeSpellData((int)(0.16 * BaseMana.get(71)), 0, 0, 0, 0, 0);
        spellData[2] = makeSpellData((int)(0.16 * BaseMana.get(72)), 0, 0, 0, 0, 0);
        spellData[3] = makeSpellData((int)(0.16 * BaseMana.get(73)), 0, 0, 0, 0, 0);
        spellData[4] = makeSpellData((int)(0.16 * BaseMana.get(74)), 0, 0, 0, 0, 0);
        spellData[5] = makeSpellData((int)(0.16 * BaseMana.get(75)), 0, 0, 0, 0, 0);
        spellData[6] = makeSpellData((int)(0.16 * BaseMana.get(76)), 0, 0, 0, 0, 0);
        spellData[7] = makeSpellData((int)(0.16 * BaseMana.get(77)), 0, 0, 0, 0, 0);
        spellData[8] = makeSpellData((int)(0.16 * BaseMana.get(78)), 0, 0, 0, 0, 0);
        spellData[9] = makeSpellData((int)(0.16 * BaseMana.get(79)), 0, 0, 0, 0, 0);
        spellData[10] = makeSpellData((int)(0.16 * BaseMana.get(80)), 0, 0, 0, 0, 0);
        spellDataBeta[0] = makeSpellData((int)(0.16 * BaseMana.get(80)), 0, 0, 0, 0, 0);
        spellDataBeta[1] = makeSpellData((int)(0.16 * BaseMana.get(81)), 0, 0, 0, 0, 0);
        spellDataBeta[2] = makeSpellData((int)(0.16 * BaseMana.get(82)), 0, 0, 0, 0, 0);
        spellDataBeta[3] = makeSpellData((int)(0.16 * BaseMana.get(83)), 0, 0, 0, 0, 0);
        spellDataBeta[4] = makeSpellData((int)(0.16 * BaseMana.get(84)), 0, 0, 0, 0, 0);
        spellDataBeta[5] = makeSpellData((int)(0.16 * BaseMana.get(85)), 0, 0, 0, 0, 0);
    }

    private static SpellData getMaxRankSpellData(CalculationOptionsMage options) {
        if (options.getBeta()) {
            return spellDataBeta[options.getPlayerLevel() - 80];
        } else {
            return spellData[options.getPlayerLevel() - 70];
        }
    }

    private static final float spellPowerCoefficient = 1.5f / 3.5f * 0.855f / 0.455f;

    @Override
    public Spell getSpell(CastingState castingState) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        spell.calculateDerivedStats(castingState);
        float q = 0.15f * castingState.getMageTalents().getFrostWarding();
        float absorb = 1950f + spellPowerCoefficient * castingState.getFireSpellPower();
        spell.absorb = absorb;
        spell.totalAbsorb = Math.min(absorb, 30f * (float) castingState.getSolver().getIncomingDamageDpsFire());
        spell.averageCost -= Math.min(q / (1 - q) * absorb, q * 30f * (float) castingState.getSolver().getIncomingDamageDpsFire());
        return spell;
    }

    public void initialize(Solver solver) {
        setName("Fire Ward");
        initializeCastTime(false, true, 0, 30);
        initializeDamage(solver, false, 0, MagicSchool.Fire, getMaxRankSpellData(solver.getCalculationOptions()), 0, 1, 0);
        setDirty(false);
    }
}
