// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/AbilityDK.HeartStrike.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

class AbilityDK_HeartStrike extends AbilityDK_Base {
    public AbilityDK_HeartStrike(CombatState cs) {
        this.cState = cs;
        this.mh = cs.MH;
        this.oh = cs.OH;
        this.name = "Heart Strike";
        this.abilityCost[DKCostTypes.Blood.ordinal()] = 1;
        this.abilityCost[DKCostTypes.RunicPower.ordinal()] = -10;
        this.setBaseDamage(736); // May need to adjust this.
        this.weaponRequired = true;
        this.weaponDamageModifier = .5f;
        this.triggersGcd = true;
        this.aoe = true;
        // TODO: Multi Target spell - need to add in the damage for the 2nd target.
    }
}
