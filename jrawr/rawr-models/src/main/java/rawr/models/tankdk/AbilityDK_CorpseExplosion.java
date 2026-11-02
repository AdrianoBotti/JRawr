// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/AbilityDK.CorpseExplosion.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

class AbilityDK_CorpseExplosion extends AbilityDK_Base {
    public AbilityDK_CorpseExplosion(CombatState cs) {
        this.cState = cs;
        this.name = "Corpse Explosion";
        this.abilityCost[DKCostTypes.RunicPower.ordinal()] = 40;
        this.setBaseDamage(443);
        this.weaponRequired = false;
        this.triggersGcd = true;
        this.range = 30;
        this.setCooldown(5000);
        this.aoe = true;
    }
}
