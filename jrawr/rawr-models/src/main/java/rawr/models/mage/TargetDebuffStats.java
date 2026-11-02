// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/CharacterCalculationsMage.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.stats.Stats;

/**
 * Lightweight storage for target debuffs, just store the relevant stats
 * instead of creating a full blown Stats object
 */
public class TargetDebuffStats {
    public float spellCritOnTarget;
    public float spellHaste;
    public float bonusDamageMultiplier;
    public float bonusFrostDamageMultiplier;
    public float bonusFireDamageMultiplier;
    public float bonusHolyDamageMultiplier;

    public float getSpellCritOnTarget() {
        return this.spellCritOnTarget;
    }

    public void setSpellCritOnTarget(float value) {
        this.spellCritOnTarget = value;
    }

    public float getSpellHaste() {
        return this.spellHaste;
    }

    public void setSpellHaste(float value) {
        this.spellHaste = value;
    }

    public float getBonusDamageMultiplier() {
        return this.bonusDamageMultiplier;
    }

    public void setBonusDamageMultiplier(float value) {
        this.bonusDamageMultiplier = value;
    }

    public float getBonusFrostDamageMultiplier() {
        return this.bonusFrostDamageMultiplier;
    }

    public void setBonusFrostDamageMultiplier(float value) {
        this.bonusFrostDamageMultiplier = value;
    }

    public float getBonusFireDamageMultiplier() {
        return this.bonusFireDamageMultiplier;
    }

    public void setBonusFireDamageMultiplier(float value) {
        this.bonusFireDamageMultiplier = value;
    }

    public float getBonusHolyDamageMultiplier() {
        return this.bonusHolyDamageMultiplier;
    }

    public void setBonusHolyDamageMultiplier(float value) {
        this.bonusHolyDamageMultiplier = value;
    }

    public void accumulate(Stats stats) {
        this.spellCritOnTarget += stats.getSpellCritOnTarget();
        this.spellHaste = (1 + this.spellHaste) * (1 + stats.getSpellHaste()) - 1;
        this.bonusDamageMultiplier = (1 + this.bonusDamageMultiplier) * (1 + stats.getBonusDamageMultiplier()) - 1;
        this.bonusFrostDamageMultiplier = (1 + this.bonusFrostDamageMultiplier) * (1 + stats.getBonusFrostDamageMultiplier()) - 1;
        this.bonusFireDamageMultiplier = (1 + this.bonusFireDamageMultiplier) * (1 + stats.getBonusFireDamageMultiplier()) - 1;
        this.bonusHolyDamageMultiplier = (1 + this.bonusHolyDamageMultiplier) * (1 + stats.getBonusHolyDamageMultiplier()) - 1;
    }
}
