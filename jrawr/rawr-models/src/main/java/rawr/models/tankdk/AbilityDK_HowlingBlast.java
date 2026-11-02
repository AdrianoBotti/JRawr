// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/AbilityDK.HowlingBlast.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

import rawr.base.items.ItemDamageType;

class AbilityDK_HowlingBlast extends AbilityDK_Base {
    public AbilityDK_HowlingBlast(CombatState cs) {
        this.cState = cs;
        this.name = "Howling Blast";
        this.abilityCost[DKCostTypes.Frost.ordinal()] = 1;
        this.abilityCost[DKCostTypes.UnHoly.ordinal()] = 1;
        this.abilityCost[DKCostTypes.RunicPower.ordinal()] = -15;
        this.minDamage = 518;
        this.maxDamage = 562;
        this.damageType = ItemDamageType.Frost;
        this.weaponRequired = false;
        this.weaponDamageModifier = 0;
        this.triggersGcd = true;
        this.range = 20;
        this.area = 10;
        this.aoe = true;
    }
}
