// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/AbilityDK.BloodStrike.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

class AbilityDK_BloodStrike extends AbilityDK_Base {
    public AbilityDK_BloodStrike(CombatState cs) {
        this.cState = cs;
        this.mh = cs.MH;
        this.oh = cs.OH;
        this.name = "Blood Strike";
        this.abilityCost[DKCostTypes.Blood.ordinal()] = 1;
        this.abilityCost[DKCostTypes.RunicPower.ordinal()] = -10;
        this.setBaseDamage(306);
        this.weaponDamageModifier = 0.4f;
        this.weaponRequired = true;
        this.triggersGcd = true;
        m_iToT = cState.m_Talents.getThreatOfThassarian();
    }

    // TODO: Damage increased by 12.5% per disease on target.
    private float damageMultiplierModifier;

    /** Setup the modifier formula for a given ability. */
    @Override
    public float getDamageMultiplierModifier() {
        // return .125 * uNumDiseases;
        return this.damageMultiplierModifier;
    }

    @Override
    public void setDamageMultiplierModifier(float value) {
        this.damageMultiplierModifier = value;
    }

    private int m_iToT = 0;

    /**
     * Get the average value between Max and Min damage
     * For DOTs damage is on a per-tick basis.
     */
    @Override
    public int getBaseDamage() {
        int wDam = super.getBaseDamage();
        // Off-hand damage is only effective if we have Threat of Thassaurian
        // And only for specific strikes as defined by the talent.
        if (m_iToT > 0 && null != this.oh) // DW
        {
            float iToTMultiplier = 0;
            if (m_iToT == 1)
                iToTMultiplier = .30f;
            if (m_iToT == 2)
                iToTMultiplier = .60f;
            if (m_iToT == 3)
                iToTMultiplier = 1f;
            wDam += (int) (this.oh.damage * iToTMultiplier * this.weaponDamageModifier);
        }
        return wDam;
    }

    @Override
    public void setBaseDamage(int value) {
        // Setup so that we can just set a base damage w/o having to
        // manually set Min & Max all the time.
        maxDamage = minDamage = value;
    }
}
