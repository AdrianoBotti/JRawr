// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/AbilityDK.IcyTouch.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

import rawr.base.items.ItemDamageType;

/**
 * Chills the target for 227 to 245 Frost damage and infects them with Frost Fever, a disease that
 * deals periodic damage and reduces melee and ranged attack speed by 14% for 15 sec.
 */
class AbilityDK_IcyTouch extends AbilityDK_Base {
    public AbilityDK_IcyTouch(CombatState cs) {
        this.cState = cs;
        this.name = "Icy Touch";
        this.abilityCost[DKCostTypes.Frost.ordinal()] = 1;
        this.abilityCost[DKCostTypes.RunicPower.ordinal()] = -10;
        this.maxDamage = 245;
        this.minDamage = 227;
        this.range = 20;
        this.damageType = ItemDamageType.Frost;
        this.triggersGcd = true;
        // 3.3.3 IT now hits alot harder for threat.
        this.setThreatMultiplier(7);
        this.triggeredAbility = new AbilityDK_FrostFever(cs);
    }
}
