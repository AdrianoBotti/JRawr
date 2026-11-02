// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/AbilityDK.ScourgeStrike.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

class AbilityDK_ScourgeStrike extends AbilityDK_Base {
    public AbilityDK_ScourgeStrike(CombatState cs) {
        this.cState = cs;
        this.mh = cs.MH;
        this.oh = cs.OH;
        this.name = "Scourge Strike";
        this.abilityCost[DKCostTypes.Frost.ordinal()] = 1;
        this.abilityCost[DKCostTypes.UnHoly.ordinal()] = 1;
        this.abilityCost[DKCostTypes.RunicPower.ordinal()] = -15;
        this.setBaseDamage(400);
        this.weaponRequired = true;
        this.weaponDamageModifier = .5f;
        this.triggersGcd = true;
        // TODO: Physical Damage * .25 * # diseases on target as shadow.
    }
}
