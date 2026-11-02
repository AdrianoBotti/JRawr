// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class LivingBombTemplate extends SpellTemplate {
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
        spellData[0] = makeSpellData((int)(0.22 * BaseMana.get(70)), 306, 306, 0.4f, 1024, 0.8f);
        spellData[1] = makeSpellData((int)(0.22 * BaseMana.get(71)), 306, 306, 0.4f, 1024, 0.8f);
        spellData[2] = makeSpellData((int)(0.22 * BaseMana.get(72)), 306, 306, 0.4f, 1024, 0.8f);
        spellData[3] = makeSpellData((int)(0.22 * BaseMana.get(73)), 306, 306, 0.4f, 1024, 0.8f);
        spellData[4] = makeSpellData((int)(0.22 * BaseMana.get(74)), 306, 306, 0.4f, 1024, 0.8f);
        spellData[5] = makeSpellData((int)(0.22 * BaseMana.get(75)), 306, 306, 0.4f, 1024, 0.8f);
        spellData[6] = makeSpellData((int)(0.22 * BaseMana.get(76)), 306, 306, 0.4f, 1024, 0.8f);
        spellData[7] = makeSpellData((int)(0.22 * BaseMana.get(77)), 306, 306, 0.4f, 1024, 0.8f);
        spellData[8] = makeSpellData((int)(0.22 * BaseMana.get(78)), 306, 306, 0.4f, 1024, 0.8f);
        spellData[9] = makeSpellData((int)(0.22 * BaseMana.get(79)), 306, 306, 0.4f, 1024, 0.8f);
        spellData[10] = makeSpellData((int)(0.22 * BaseMana.get(80)), 690, 690, 0.4f, 1380, 0.8f);
        spellDataBeta[0] = makeSpellData((int)(0.22 * BaseMana.get(80)), 690, 690, 0.4f, 1380, 0.8f);
        spellDataBeta[1] = makeSpellData((int)(0.22 * BaseMana.get(81)), 690, 690, 0.4f, 1380, 0.8f);
        spellDataBeta[2] = makeSpellData((int)(0.22 * BaseMana.get(82)), 690, 690, 0.4f, 1380, 0.8f);
        spellDataBeta[3] = makeSpellData((int)(0.22 * BaseMana.get(83)), 690, 690, 0.4f, 1380, 0.8f);
        spellDataBeta[4] = makeSpellData((int)(0.22 * BaseMana.get(84)), 690, 690, 0.4f, 1380, 0.8f);
        spellDataBeta[5] = makeSpellData((int)(0.22 * BaseMana.get(85)), 690, 690, 0.4f, 1380, 0.8f);
    }

    @Override
    public Spell getSpell(CastingState castingState) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        if (castingState.getMageTalents().isGlyphOfLivingBomb()) {
            spell.dotDamageModifier = (1 + Math.max(0.0f, Math.min(1.0f, castingState.getFireCritRate())) * (castingState.getFireCritBonus() - 1));
        }
        spell.calculateDerivedStats(castingState, false, false, false);
        if (castingState.getMageTalents().isGlyphOfLivingBomb()) {
            spell.igniteProcs *= 5;
            if (castingState.getSolver().getNeedsDisplayCalculations()) {
                float igniteFactor = spell.spellModifier * spell.getHitRate() * spell.getPartialResistFactor() * Math.max(0.0f, Math.min(1.0f, castingState.getFireCritRate())) * castingState.getFireCritBonus() * castingState.getSolver().getIgniteFactor() / (1 + castingState.getSolver().getIgniteFactor());
                spell.igniteDamage += spell.getBasePeriodicDamage() * igniteFactor;
                spell.igniteDamagePerSpellPower += spell.getDotDamageCoefficient() * igniteFactor;
            }
        }
        return spell;
    }

    public void initialize(Solver solver) {
        setName("Living Bomb");
        initializeCastTime(false, true, 0f, 0f);
        initializeDamage(solver, false, 35, MagicSchool.Fire, getMaxRankSpellData(solver.getCalculationOptions()));
        setDotDuration(12);
        setDotTickInterval(3);
        setBaseCritRate(getBaseCritRate() + 0.02f * solver.getMageTalents().getWorldInFlames());
        setDirty(false);
    }
}
