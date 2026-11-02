// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/AbilityDK.FrostFever.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

import rawr.base.items.ItemDamageType;

/**
 * A disease dealing [0 + AP * 0.055 * 1.15] Frost damage every 3 sec and reducing the target's melee
 * and ranged attack speed by 14% for 15 sec. Caused by Icy Touch and other spells.
 */
class AbilityDK_FrostFever extends AbilityDK_Base {
    public AbilityDK_FrostFever(CombatState cs) {
        this.cState = cs;
        this.name = "Frost Fever";
        this.damageType = ItemDamageType.Frost;
        if (cs.m_Talents.getEpidemic() >= 3)
            // error
            this.setDuration(15000);
        else
            this.setDuration(15000 + (cs.m_Talents.getEpidemic() * 3000));
        this.setTickRate(3 * 1000);
        this.setBaseDamage(0);
        this.triggersGcd = false;
        this.setCooldown(0);
        this.setCastTime(0);
    }

    private int damageAdditiveModifier = 0;

    /** Setup the modifier formula for a given ability. */
    @Override
    public int getDamageAdditiveModifier() {
        //this.DamageAdditiveModifer = //[AP * 0.055 * 1.15]
        return (int) (this.cState.m_Stats.getAttackPower() * .055 * 1.15) + this.damageAdditiveModifier;
    }

    @Override
    public void setDamageAdditiveModifier(int value) {
        this.damageAdditiveModifier = value;
    }
}
