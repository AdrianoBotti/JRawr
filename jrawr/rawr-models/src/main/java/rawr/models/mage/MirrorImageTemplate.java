// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class MirrorImageTemplate extends SpellTemplate {
    float baseDamageBlast, baseDamageBolt, boltMultiplier, blastMultiplier, castTime, multiplier, dpsp;

    public void initialize(Solver solver) {
        setName("Mirror Image");
        baseDamageBlast = 97.5f;
        baseDamageBolt = 166.0f;
        CalculationOptionsMage calculationOptions = solver.getCalculationOptions();
        int playerLevel = calculationOptions.getPlayerLevel();
        int targetLevel = calculationOptions.getTargetLevel();
        float spellCrit = 0.05f + solver.getTargetDebuffs().spellCritOnTarget;
        float blastHitRate = solver.getBaseState().getFireHitRate();
        float boltHitRate = solver.getBaseState().getFrostHitRate();
        float haste = (1f + solver.getTargetDebuffs().spellHaste);
        boltMultiplier = boltHitRate * (1 + solver.getTargetDebuffs().bonusDamageMultiplier) * (1 + solver.getTargetDebuffs().bonusFrostDamageMultiplier) * ((calculationOptions.getFrostResist() == -1) ? 0 : (1 - StatConversion.getAverageResistance(playerLevel, targetLevel, calculationOptions.getFrostResist(), 0)));
        blastMultiplier = blastHitRate * (1 + solver.getTargetDebuffs().bonusDamageMultiplier) * (1 + solver.getTargetDebuffs().bonusFireDamageMultiplier) * ((calculationOptions.getFireResist() == -1) ? 0 : (1 - StatConversion.getAverageResistance(playerLevel, targetLevel, calculationOptions.getFireResist(), 0)));
        castTime = (2 * 3.0f + 1.5f) / haste;
        multiplier = (solver.getMageTalents().isGlyphOfMirrorImage() ? 4 : 3) * (1 + 0.5f * spellCrit);
        dpsp = multiplier * (2 * (1f / 3f * 0.3f) * boltMultiplier + (1f / 3f * 0.15f) * blastMultiplier);
        setDirty(false);
    }

    @Override
    public Spell getSpell(CastingState castingState) {
        Spell spell = Spell.createNew(this, castingState.getSolver());

        spell.castTime = castTime;
        spell.averageCost = 0.0f;
        spell.averageDamage = multiplier * (2 * (baseDamageBolt + castingState.getFrostSpellPower() / 3f * 0.3f) * boltMultiplier + (baseDamageBlast + castingState.getFireSpellPower() / 3f * 0.15f) * blastMultiplier);
        spell.averageThreat = 0.0f;
        spell.damagePerSpellPower = dpsp;

        return spell;
    }
}
