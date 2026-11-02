// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/AbilityDK.DeathNDecay.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

import rawr.base.items.ItemDamageType;

class AbilityDK_DeathNDecay extends AbilityDK_Base {
    public AbilityDK_DeathNDecay(CombatState cs) {
        this.cState = cs;
        this.name = "Death N Decay";
        this.abilityCost[DKCostTypes.Frost.ordinal()] = 1;
        this.abilityCost[DKCostTypes.UnHoly.ordinal()] = 1;
        this.abilityCost[DKCostTypes.Blood.ordinal()] = 1;
        this.abilityCost[DKCostTypes.RunicPower.ordinal()] = -15;
        this.minDamage = 62;
        this.maxDamage = 62;
        this.damageType = ItemDamageType.Shadow;
        this.weaponRequired = false;
        this.weaponDamageModifier = 0;
        this.triggersGcd = true;
        this.range = 30;
        this.area = 10;
        this.setTickRate(1 * 1000);
        this.setDuration(10 * 1000);
        this.setCooldown(30 * 1000);
        this.aoe = true;
    }
}
