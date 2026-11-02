// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/AbilityDK.DeathCoil.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

class AbilityDK_DeathCoil extends AbilityDK_Base {
    public AbilityDK_DeathCoil(CombatState cs) {
        this.cState = cs;
        this.name = "Death Coil";
        this.abilityCost[DKCostTypes.RunicPower.ordinal()] = 40;
        this.setBaseDamage(443);
        this.weaponRequired = false;
        this.triggersGcd = true;
        this.range = 30;
    }
}
