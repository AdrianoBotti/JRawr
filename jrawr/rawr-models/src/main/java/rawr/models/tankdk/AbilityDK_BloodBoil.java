// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/AbilityDK.BloodBoil.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

import rawr.base.items.ItemDamageType;

/**
 * This class is the implmentation of the BloodBoil Ability based on the AbilityDK_Base class.
 */
class AbilityDK_BloodBoil extends AbilityDK_Base {
    public AbilityDK_BloodBoil(CombatState cs) {
        this.cState = cs;
        this.name = "Blood Boil";
        this.abilityCost[DKCostTypes.Blood.ordinal()] = 1;
        this.abilityCost[DKCostTypes.RunicPower.ordinal()] = -10;
        this.minDamage = 180;
        this.maxDamage = 220;
        this.damageType = ItemDamageType.Shadow;
        this.weaponRequired = false;
        this.weaponDamageModifier = 0;
        this.triggersGcd = true;
        this.range = 0;
        this.area = 10;
        this.aoe = true;
    }

    private int damageAdditiveModifier = 95;

    /** Setup the modifier formula for a given ability. */
    @Override
    public int getDamageAdditiveModifier() {
        // AP Scaling
        int AdditionalDamage = (int) (this.cState.m_Stats.getAttackPower() * 0.04);
        // TODO: Additional Disease Damage:
        // if (uNumDiseases > 0): 95 + (0.035 × Attack Power).
        return (int) (this.cState.m_Stats.getAttackPower() * 0.035) + this.damageAdditiveModifier + AdditionalDamage;
    }

    @Override
    public void setDamageAdditiveModifier(int value) {
        this.damageAdditiveModifier = value;
    }
}
