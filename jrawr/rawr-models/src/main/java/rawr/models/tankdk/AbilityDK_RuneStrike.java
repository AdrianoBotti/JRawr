// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/AbilityDK.RuneStrike.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

/**
 * Strike the target for 150% weapon damage plus [150 * AP * 10 / 10000]. Only usable after the
 * Death Knight dodges or parries. Can't be dodged, blocked, or parried.
 */
class AbilityDK_RuneStrike extends AbilityDK_Base {
    public AbilityDK_RuneStrike(CombatState cs) {
        this.cState = cs;
        this.mh = cs.MH;
        this.oh = cs.OH;
        this.name = "Rune Strike";
        this.abilityCost[DKCostTypes.RunicPower.ordinal()] = 20;
        this.setBaseDamage(736); // May need to adjust this.
        this.weaponRequired = true;
        this.weaponDamageModifier = 1.5f;
        this.setDamageAdditiveModifier(150 * (int) cs.m_Stats.getAttackPower() * 10 / 10000);
        this.triggersGcd = false;
        this.setCooldown(0);
        m_iToT = cs.m_Talents.getThreatOfThassarian();
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
