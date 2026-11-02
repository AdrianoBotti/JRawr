// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/Spell.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class AoeSpell extends Spell {
    public AoeSpell(SpellTemplate template) {
        super(template);
    }

    @Override
    public void calculate(CastingState castingState) {
        super.calculate(castingState);
        // do not count debuffs for aoe effects, can't assume it will be up on all
        // do not include molten fury (molten fury relates to boss), instead amplify all by average
        if (castingState.getMoltenFury()) {
            spellModifier /= (1 + 0.06f * castingState.getMageTalents().getMoltenFury());
        }
        if (castingState.getMageTalents().getMoltenFury() > 0) {
            spellModifier *= (1 + 0.06f * castingState.getMageTalents().getMoltenFury() * castingState.getCalculationOptions().getMoltenFuryPercentage());
        }
    }

    @Override
    public void calculateDerivedStats(CastingState castingState, boolean outOfFiveSecondRule, boolean pom, boolean spammedDot, boolean round, boolean forceHit, boolean forceMiss) {
        super.calculateDerivedStats(castingState, outOfFiveSecondRule, pom, spammedDot, round, forceHit, forceMiss);
        targetProcs *= castingState.getCalculationOptions().getAoeTargets();
    }

    @Override
    public float calculateAverageDamage(Solver solver, float spellPower, boolean spammedDot, boolean forceHit, float[] damagePerSpellPower, float[] igniteDamage, float[] igniteDamagePerSpellPower) {
        damagePerSpellPower[0] = 0; // do we really need this for aoe?
        float baseAverage = (getBaseMinDamage() + getBaseMaxDamage()) / 2f + spellPower * getSpellDamageCoefficient();
        float critMultiplier = 1 + (critBonus - 1) * Math.max(0, critRate);
        float resistMultiplier = (forceHit ? 1.0f : getHitRate()) * getPartialResistFactor();
        int targets = solver.getCalculationOptions().getAoeTargets();
        float averageDamage = baseAverage * spellModifier * directDamageModifier * targets * (forceHit ? 1.0f : getHitRate());
        if (targets > 10) {
            averageDamage *= 10.0f / targets;
        }
        if (solver.getNeedsDisplayCalculations() && (getMagicSchool() == MagicSchool.Fire || getMagicSchool() == MagicSchool.FrostFire) && solver.getMageTalents().getIgnite() > 0) {
            igniteDamage[0] = averageDamage * getPartialResistFactor() * critBonus * solver.getIgniteFactor() / (1 + solver.getIgniteFactor()) * Math.max(0, critRate);
            igniteDamagePerSpellPower[0] = 0; // we're not applying effect spell power to aoe
        } else {
            igniteDamage[0] = 0;
            igniteDamagePerSpellPower[0] = 0;
        }
        averageDamage = averageDamage * critMultiplier * getPartialResistFactor();
        if (getBasePeriodicDamage() > 0.0f) {
            if (spammedDot) {
                averageDamage += targets * (getBasePeriodicDamage() + getDotDamageCoefficient() * spellPower) * spellModifier * dotDamageModifier * resistMultiplier * castTime / getDotDuration();
            } else {
                averageDamage += targets * (getBasePeriodicDamage() + getDotDamageCoefficient() * spellPower) * spellModifier * dotDamageModifier * resistMultiplier;
            }
        }
        return averageDamage;
    }
}
