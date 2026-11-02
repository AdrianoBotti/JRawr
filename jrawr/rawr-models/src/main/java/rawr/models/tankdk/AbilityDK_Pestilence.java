// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/AbilityDK.Pestilence.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

class AbilityDK_Pestilence extends AbilityDK_Base {
    public AbilityDK_Pestilence(CombatState cs) {
        this.cState = cs;
        this.name = "Pestilence";
        this.abilityCost[DKCostTypes.Blood.ordinal()] = 1;
        this.abilityCost[DKCostTypes.RunicPower.ordinal()] = -10;
        this.minDamage = 0;
        this.maxDamage = 0;
        this.weaponRequired = false;
        this.weaponDamageModifier = 0;
        this.triggersGcd = true;
        this.range = 0;
        this.area = 10;
        this.aoe = true;
        // TODO: Glyph - Refreshes disease
        // TODO: Glyph - extends range
    }
}
