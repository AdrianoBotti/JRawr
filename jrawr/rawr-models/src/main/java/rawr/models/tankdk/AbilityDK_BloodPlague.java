// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/AbilityDK.BloodPlague.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

import rawr.base.items.ItemDamageType;

/**
 * A disease dealing [0 + AP * 0.055 * 1.15] Shadow damage every 3 sec for 15 sec.
 * Caused by Plague Strike and other abilities.
 */
class AbilityDK_BloodPlague extends AbilityDK_Base {
    public AbilityDK_BloodPlague(CombatState cs) {
        this.cState = cs;
        this.name = "Blood Plague";
        this.damageType = ItemDamageType.Shadow;
        if (cState.m_Talents.getEpidemic() >= 3)
            // error
            this.setDuration(15000);
        else
            this.setDuration(15000 + (cState.m_Talents.getEpidemic() * 3000));
        this.setTickRate(3 * 1000);
        this.setBaseDamage(0);
        this.triggersGcd = false;
        this.setCastTime(0);
        this.setCooldown(0);
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
