// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class PyroblastTemplate extends SpellTemplate {
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
        spellData[0] = makeSpellData((int)(0.22 * BaseMana.get(70)), 939, 1191, 1.15f, 356, 0.2f);
        spellData[1] = makeSpellData((int)(0.22 * BaseMana.get(71)), 939, 1191, 1.15f, 356, 0.2f);
        spellData[2] = makeSpellData((int)(0.22 * BaseMana.get(72)), 939, 1191, 1.15f, 356, 0.2f);
        spellData[3] = makeSpellData((int)(0.22 * BaseMana.get(73)), 1014, 1286, 1.15f, 384, 0.2f);
        spellData[4] = makeSpellData((int)(0.22 * BaseMana.get(74)), 1014, 1286, 1.15f, 384, 0.2f);
        spellData[5] = makeSpellData((int)(0.22 * BaseMana.get(75)), 1014, 1286, 1.15f, 384, 0.2f);
        spellData[6] = makeSpellData((int)(0.22 * BaseMana.get(76)), 1014, 1286, 1.15f, 384, 0.2f);
        spellData[7] = makeSpellData((int)(0.22 * BaseMana.get(77)), 1190, 1510, 1.15f, 452, 0.2f);
        spellData[8] = makeSpellData((int)(0.22 * BaseMana.get(78)), 1190, 1510, 1.15f, 452, 0.2f);
        spellData[9] = makeSpellData((int)(0.22 * BaseMana.get(79)), 1190, 1510, 1.15f, 452, 0.2f);
        spellData[10] = makeSpellData((int)(0.22 * BaseMana.get(80)), 1210, 1531, 1.15f, 452, 0.2f);
        spellDataBeta[0] = makeSpellData((int)(0.22 * BaseMana.get(80)), 1210, 1531, 1.15f, 452, 0.2f);
        spellDataBeta[1] = makeSpellData((int)(0.22 * BaseMana.get(81)), 1210, 1531, 1.15f, 452, 0.2f);
        spellDataBeta[2] = makeSpellData((int)(0.22 * BaseMana.get(82)), 1210, 1531, 1.15f, 452, 0.2f);
        spellDataBeta[3] = makeSpellData((int)(0.22 * BaseMana.get(83)), 1210, 1531, 1.15f, 452, 0.2f);
        spellDataBeta[4] = makeSpellData((int)(0.22 * BaseMana.get(84)), 1210, 1531, 1.15f, 452, 0.2f);
        spellDataBeta[5] = makeSpellData((int)(0.22 * BaseMana.get(85)), 1210, 1531, 1.15f, 452, 0.2f);
    }

    private float tormentTheWeak;

    public Spell getSpell(CastingState castingState, boolean pom, boolean spammedDot) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        spell.spellModifier *= (1 + tormentTheWeak * castingState.getSnaredTime());
        spell.calculateDerivedStats(castingState, false, pom, spammedDot);
        return spell;
    }

    public Spell getSpell(CastingState castingState, boolean pom) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        spell.spellModifier *= (1 + tormentTheWeak * castingState.getSnaredTime());
        spell.calculateDerivedStats(castingState, false, pom, false, false, false, false, true);
        return spell;
    }

    public void initialize(Solver solver) {
        setName("Pyroblast");
        initializeCastTime(false, false, 5f, 0);
        initializeDamage(solver, false, 35, MagicSchool.Fire, getMaxRankSpellData(solver.getCalculationOptions()));
        setDotDuration(12);
        setDotTickInterval(3);
        setBaseCritRate(getBaseCritRate() + 0.02f * solver.getMageTalents().getWorldInFlames());
        tormentTheWeak = 0.04f * solver.getMageTalents().getTormentTheWeak();
        setSpellDamageCoefficient(getSpellDamageCoefficient() + 0.05f * solver.getMageTalents().getEmpoweredFire());
        setDotDamageCoefficient(getDotDamageCoefficient() + 4 * 0.05f * solver.getMageTalents().getEmpoweredFire());
        setDirty(false);
    }
}
