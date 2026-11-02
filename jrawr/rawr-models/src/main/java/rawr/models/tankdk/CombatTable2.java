// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/CombatTable2.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

import rawr.base.character.Character;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.stats.Stats;

import java.util.ArrayList;
import java.util.List;

class CombatTable2 {
    // Basic setup variables
    private CombatState m_CState;
    private CharacterCalculationsTankDK m_Calcs;
    private CalculationOptionsTankDK m_Opts;
    public Rotation m_Rotation;

    public List<AbilityDK_Base> ml_Rot;

    // Output Values
    private float totalDamage;

    public float getTotalDamage() {
        return this.totalDamage;
    }

    private float totalThreat;

    public float getTotalThreat() {
        return this.totalThreat;
    }

    public float getMTps() // readonly
    {
        float tps = 0;
        if (getMRotationDuration() > 0)
            tps = this.totalThreat / (getMRotationDuration() / 1000);
        return tps;
    }

    public float getMDps() // readonly
    {
        float dps = 0;
        if (getMRotationDuration() > 0)
            dps = this.totalDamage / (getMRotationDuration() / 1000);
        return dps;
    }

    private int[] abilityCost;

    public float getMRotationDuration() // readonly
    {
        // At the very least, the rotation duration is how long it takes to cast the
        // Abilities selected in the rotations.
        int iRotDur = this.abilityCost[DKCostTypes.CastTime.ordinal()] + (this.gcds * 1500);
        // However, if the Cooldowns of the rotations take LONGER,
        // then we need to extend the rotation Duration to be that length.
        iRotDur = Math.max(iRotDur, this.abilityCost[DKCostTypes.CooldownTime.ordinal()]);
        return iRotDur;
    }

    public float getMCooldownDuration() // readonly
    {
        return this.abilityCost[DKCostTypes.CooldownTime.ordinal()];
    }

    public float getMCastDuration() // readonly
    {
        return this.abilityCost[DKCostTypes.CastTime.ordinal()];
    }

    public float getMDurationDuration() // readonly
    {
        return this.abilityCost[DKCostTypes.DurationTime.ordinal()];
    }

    public int getMBloodRunes() {
        return this.abilityCost[DKCostTypes.Blood.ordinal()];
    }

    public int getMFrostRunes() {
        return this.abilityCost[DKCostTypes.Frost.ordinal()];
    }

    public int getMUnholyRunes() {
        return this.abilityCost[DKCostTypes.UnHoly.ordinal()];
    }

    public int getMDeathRunes() {
        return this.abilityCost[DKCostTypes.Death.ordinal()];
    }

    public int getMRunicPower() {
        return this.abilityCost[DKCostTypes.RunicPower.ordinal()];
    }

    private int gcds;

    public int getMGCDs() {
        return this.gcds;
    }

    public int m_iRSState = RSState.Good.ordinal();

    public CombatTable2(Character c, Stats stats, CharacterCalculationsTankDK calcs, CalculationOptionsTankDK calcOpts) {
        if (calcOpts.getBExperimental()) {
            this.m_CState = new CombatState();
            if (c != null) {
                if (c.getDeathKnightTalents() == null)
                    c.setDeathKnightTalents(new rawr.base.talents.DeathKnightTalents());
                this.m_CState.m_Talents = c.getDeathKnightTalents().clone();
            }
            this.m_CState.m_Stats = stats.copy();
            m_Calcs = calcs;
            m_Opts = calcOpts;
            this.m_CState.m_NumberOfTargets = (float) m_Opts.getUNumberTargets();
            m_Rotation = calcOpts.getMRotation();

            //TODO: Handle Expertise
            if (c.getMainHand() != null && c.getMainHand().getItem().getType() != ItemType.None) {
                m_CState.MH = new Weapon(c.getMainHand().getItem(), m_CState.m_Stats, m_Opts, 0);
                m_CState.OH = null;
                if (c.getMainHand().getSlot() != ItemSlot.TwoHand) {
                    if (c.getOffHand() != null && c.getOffHand().getItem().getType() != ItemType.None)
                        m_CState.OH = new Weapon(c.getOffHand().getItem(), this.m_CState.m_Stats, m_Opts, 0);
                }
            } else {
                m_CState.MH = null;
                m_CState.OH = null;
            }

            // Checking the rotation:
            if (m_Rotation.IcyTouch == 0
                    && m_Rotation.PlagueStrike == 0
                    && m_Rotation.BloodStrike == 0) {
                // Then this is probably a null rotation, and
                // so let's build one?
                m_Rotation = new Rotation(this.m_CState.m_Talents);
            }

            buildRotation();

            // TODO: move this out of the constructor
            compileRotation(m_Rotation);
        }
    }

    /** Generate a rotation based on available resources. */
    public void buildRotation() {
        if (m_Opts.getBExperimental()) {
            // Setup an instance of each ability.
            // Single Runes:
            AbilityDK_IcyTouch IT = new AbilityDK_IcyTouch(m_CState);
            AbilityDK_FrostFever FF = new AbilityDK_FrostFever(m_CState);
            AbilityDK_PlagueStrike PS = new AbilityDK_PlagueStrike(m_CState);
            AbilityDK_BloodPlague BP = new AbilityDK_BloodPlague(m_CState);
            AbilityDK_BloodStrike BS = new AbilityDK_BloodStrike(m_CState);
            AbilityDK_HeartStrike HS = new AbilityDK_HeartStrike(m_CState);
            AbilityDK_Pestilence Pest = new AbilityDK_Pestilence(m_CState);
            AbilityDK_BloodBoil BB = new AbilityDK_BloodBoil(m_CState);
            // Multi Runes:
            AbilityDK_DeathStrike DS = new AbilityDK_DeathStrike(m_CState);
            AbilityDK_HowlingBlast HB = new AbilityDK_HowlingBlast(m_CState);
            AbilityDK_Obliterate OB = new AbilityDK_Obliterate(m_CState);
            AbilityDK_ScourgeStrike SS = new AbilityDK_ScourgeStrike(m_CState);
            AbilityDK_DeathNDecay DnD = new AbilityDK_DeathNDecay(m_CState);
            // RP:
            AbilityDK_RuneStrike RS = new AbilityDK_RuneStrike(m_CState);
            AbilityDK_DeathCoil DC = new AbilityDK_DeathCoil(m_CState);
            AbilityDK_FrostStrike FS = new AbilityDK_FrostStrike(m_CState);

            // Build the sortable list of abilities
            List<AbilityDK_Base> l_RotRunes = new ArrayList<AbilityDK_Base>();
            List<AbilityDK_Base> l_RotRP = new ArrayList<AbilityDK_Base>();
            l_RotRunes.add(IT);
            l_RotRunes.add(PS);
            l_RotRunes.add(BS);
            l_RotRunes.add(HS);
            l_RotRunes.add(BB);

            l_RotRunes.add(DS);
            l_RotRunes.add(HB);
            l_RotRunes.add(OB);
            l_RotRunes.add(SS);
            l_RotRunes.add(DnD);

            l_RotRP.add(RS);
            l_RotRP.add(DC);
            l_RotRP.add(FS);

            // we now have lists that provide sorted by cost of # of runes
            // and amount of RP needed.
        }
    }

    /** Do the work for populating data based on the Rotation settings. */
    public void compileRotation(Rotation Rot) {
        if (m_Opts.getBExperimental()) {
            // Limitation w/ current setup - can't have partial strikes
            int i = 0;
            this.abilityCost = new int[DKCostTypes.NumCostTypes.ordinal()];
            // Build the rotation into a list of abilities to play with.
            ml_Rot = new ArrayList<AbilityDK_Base>();
            // Runes
            // Frost
            if (Rot.IcyTouch > 0) {
                for (i = (int) Rot.IcyTouch; i > 0; i--) {
                    ml_Rot.add(new AbilityDK_IcyTouch(m_CState));
                    ml_Rot.add(new AbilityDK_FrostFever(m_CState));
                }
            }
            // Unholy
            if (Rot.PlagueStrike > 0) {
                for (i = (int) Rot.PlagueStrike; i > 0; i--) {
                    ml_Rot.add(new AbilityDK_PlagueStrike(m_CState));
                    ml_Rot.add(new AbilityDK_BloodPlague(m_CState));
                }
            }
            // Blood
            if (Rot.BloodStrike > 0) {
                for (i = (int) Rot.BloodStrike; i > 0; i--) {
                    ml_Rot.add(new AbilityDK_BloodStrike(m_CState));
                    if (m_CState.m_Talents.getBloodOfTheNorth() > 0) {
                        this.abilityCost[DKCostTypes.Death.ordinal()] -= (m_CState.m_Talents.getBloodOfTheNorth() / 3);
                    }
                    if (m_CState.m_Talents.getReaping() > 0) {
                        this.abilityCost[DKCostTypes.Death.ordinal()] -= (m_CState.m_Talents.getReaping() / 3);
                    }
                }
            }
            if (Rot.HeartStrike > 0) {
                if (m_CState.m_Talents.getHeartStrike() > 0) {
                    for (i = (int) Rot.HeartStrike; i > 0; i--)
                        ml_Rot.add(new AbilityDK_HeartStrike(m_CState));
                } else {
                    // Error
                    // Shot in rotation but not talented for.
                }
            }
            if (Rot.BloodBoil > 0) {
                for (i = (int) Rot.BloodBoil; i > 0; i--)
                    ml_Rot.add(new AbilityDK_BloodBoil(m_CState));
            }
            if (Rot.Pestilence > 0) {
                for (i = (int) Rot.Pestilence; i > 0; i--) {
                    ml_Rot.add(new AbilityDK_Pestilence(m_CState));
                    if (m_CState.m_Talents.getBloodOfTheNorth() > 0) {
                        this.abilityCost[DKCostTypes.Death.ordinal()] -= (m_CState.m_Talents.getBloodOfTheNorth() / 3);
                    }
                    if (m_CState.m_Talents.getReaping() > 0) {
                        this.abilityCost[DKCostTypes.Death.ordinal()] -= (m_CState.m_Talents.getReaping() / 3);
                    }
                }
            }
            // Frost+Unholy
            if (Rot.DeathStrike > 0) {
                for (i = (int) Rot.DeathStrike; i > 0; i--) {
                    ml_Rot.add(new AbilityDK_DeathStrike(m_CState));
                    if (m_CState.m_Talents.getDeathRuneMastery() > 0) {
                        this.abilityCost[DKCostTypes.Death.ordinal()] -= 2 * (m_CState.m_Talents.getDeathRuneMastery() / 3);
                    }
                }
            }
            if (Rot.HowlingBlast > 0) {
                for (i = (int) Rot.HowlingBlast; i > 0; i--) {
                    ml_Rot.add(new AbilityDK_HowlingBlast(m_CState));
                    if (m_CState.m_Talents.isGlyphofHowlingBlast()) {
                        ml_Rot.add(new AbilityDK_FrostFever(m_CState));
                    }
                }
            }
            if (Rot.Obliterate > 0) {
                for (i = (int) Rot.Obliterate; i > 0; i--) {
                    ml_Rot.add(new AbilityDK_Obliterate(m_CState));
                    if (m_CState.m_Talents.getDeathRuneMastery() > 0) {
                        this.abilityCost[DKCostTypes.Death.ordinal()] -= 2 * (m_CState.m_Talents.getDeathRuneMastery() / 3);
                    }
                    // Oblit will consume the diseases... so we need to cut their duration short.
                    if (m_CState.m_Talents.getAnnihilation() < 3) {
                        float percDuration = m_CState.m_Talents.getAnnihilation() / 3;
                        for (AbilityDK_Base a : ml_Rot) {
                            if (a.name.equals("Blood Plague")
                                    || a.name.equals("Frost Fever")
                                    || a.name.equals("Crypt Fever")
                                    || a.name.equals("Ebon Plague")) {
                                a.abilityCost[DKCostTypes.DurationTime.ordinal()] = (int) (a.abilityCost[DKCostTypes.DurationTime.ordinal()] * percDuration);
                            }
                        }
                    }
                }
            }
            if (Rot.ScourgeStrike > 0) {
                if (m_CState.m_Talents.getScourgeStrike() > 0) {
                    for (i = (int) Rot.ScourgeStrike; i > 0; i--) {
                        ml_Rot.add(new AbilityDK_ScourgeStrike(m_CState));
                    }
                } else {
                    // Error
                    // Shot in rotation but not talented for.
                }
            }
            // BloodFrostUnholy
            if (Rot.DeathNDecay > 0) {
                for (i = (int) Rot.DeathNDecay; i > 0; i--)
                    ml_Rot.add(new AbilityDK_DeathNDecay(m_CState));
            }

            // RunicPower
            if (Rot.RuneStrike > 0) {
                for (i = (int) Rot.RuneStrike; i > 0; i--)
                    ml_Rot.add(new AbilityDK_RuneStrike(m_CState));
                // TODO: need to have these replace melee strikes.
            }
            if (Rot.DeathCoil > 0) {
                for (i = (int) Rot.DeathCoil; i > 0; i--)
                    ml_Rot.add(new AbilityDK_DeathCoil(m_CState));
            }
            if (Rot.FrostStrike > 0) {
                if (m_CState.m_Talents.getFrostStrike() > 0) {
                    for (i = (int) Rot.FrostStrike; i > 0; i--)
                        ml_Rot.add(new AbilityDK_FrostStrike(m_CState));
                } else {
                    // Error
                    // Shot in rotation, but not available via talents.
                }
            }

            // Now that the list is built, setup the costs so that we know what the rotation looks like in runes.
            // Sum of Costs
            this.gcds = 0;
            for (AbilityDK_Base ability : ml_Rot) {
                for (int cost = 0; cost < DKCostTypes.NumCostTypes.ordinal(); cost++) {
                    this.abilityCost[cost] += ability.abilityCost[cost];
                }
                // Setup the total GCD count
                if (ability.triggersGcd)
                    ++this.gcds;
            }
            // TODO: Handle Disease/DoT Clipping

            // Now we have the total costs
            // Need to figure out how to factor in Death Runes
            spendDeathRunes(this.abilityCost, 0);

            // Let's evaluate the rune cooldown cost to the GCD cost:
            // 2 runes every 10 secs
            // in ms
            int BloodCD = this.abilityCost[DKCostTypes.Blood.ordinal()] * 10 * 1000 / 2;
            int FrostCD = this.abilityCost[DKCostTypes.Frost.ordinal()] * 10 * 1000 / 2;
            int UnHolyCD = this.abilityCost[DKCostTypes.UnHoly.ordinal()] * 10 * 1000 / 2;
            int DeathCD = Math.abs(this.abilityCost[DKCostTypes.Death.ordinal()]) * 10 * 1000 / 2; // Assuming all DeathRune possible generating strikes actually generate death runes.

            int maxCD = Math.max(BloodCD, FrostCD);
            maxCD = Math.max(maxCD, UnHolyCD);

            // This sums up all Cooldowntime
            // Not really the best way of handling this since cooldowns can overlap.
            // TODO: Fix cooldown math.
            this.abilityCost[DKCostTypes.CooldownTime.ordinal()] = Math.max(maxCD, this.abilityCost[DKCostTypes.CooldownTime.ordinal()]);

            // White Swings
            float fWhiteSwings = 0;
            if (m_CState.MH != null && m_CState.MH.hastedSpeed != 0) {
                fWhiteSwings = (float) getMRotationDuration() / (int) (m_CState.MH.hastedSpeed * 1000);
                // How many of the shots are parried?
                // TODO: fWhiteSwings here may need to be # of BossSwings from BossHandler.
                float fShotsParried = m_CState.m_Stats.getParry() * fWhiteSwings;
                // What's the average hasted speed of those shots?
                float fParryHastedSpeed = (m_CState.MH.hastedSpeed * 1000) * (1f - 0.24f);
                // How much of the rotation duration has hasted shots?
                float fTimeHasted = fShotsParried * fParryHastedSpeed;
                // The rest of that time, are normal shots
                float fTimeNormal = getMRotationDuration() - fTimeHasted;
                // Get the final total.
                fWhiteSwings = (fTimeNormal / (int) (m_CState.MH.hastedSpeed * 1000)) + fShotsParried;
            }
            // This will provide the opportunity to increase available RP by swings due to Talents.
            // Also get a swing count to balance out the RuneStrikes.
            boolean bAvailableWhiteSwings = false;
            float fWhiteDamage = 0;
            if (0 < fWhiteSwings && fWhiteSwings < Rot.RuneStrike) {
                // There's not enough time to get in all the RS's we have slated for
                bAvailableWhiteSwings = false;
                m_iRSState |= RSState.TimeStarved.ordinal();
            } else if (0 < fWhiteSwings) {
                // Since there are spare white swings, let's figure out how much white damage is done
                bAvailableWhiteSwings = true;
                fWhiteSwings = (fWhiteSwings - Rot.RuneStrike);
                fWhiteDamage = (m_CState.MH.damage * fWhiteSwings);
                if (null != m_CState.OH)
                    fWhiteDamage += (m_CState.OH.damage * fWhiteSwings);
            }

            boolean bSpareGCDs = false;
            boolean bAvailableRP = false;
            if (maxCD > this.abilityCost[DKCostTypes.CooldownTime.ordinal()]) {
                // We have spare GCDs waiting for Runes to come back.
                bSpareGCDs = true;
            }

            // Are we starving the RSs?
            // We may be wanting to save RP for Mind Freeze or something.
            if (this.abilityCost[DKCostTypes.RunicPower.ordinal()] > 0) {
                // starving our RP based abilities in the rotation.
                bAvailableRP = false;
                // We need to adjust the number of RSs due to starvation.
                m_iRSState |= RSState.RPStarved.ordinal();
            } else {
                bAvailableRP = true;
            }

            // Sum of DPS & Threat
            this.totalDamage = 0;
            this.totalThreat = 0;
            this.totalDamage += fWhiteDamage;
            this.totalThreat += fWhiteDamage * AbilityDK_Base.THREAT_FROST_PRESENCE;

            for (AbilityDK_Base ability : ml_Rot) {
                this.totalDamage += ability.getTotalDamage();
                this.totalThreat += ability.getTotalThreat();
            }
        }
    }

    /** Pass in the AbilityCost[] to process the DeathRunes. */
    public void spendDeathRunes(int[] AbilityCost, int DRSpent) {
        // Need to figure out how to factor in Death Runes
        // Since each death rune replaces any other rune on the rotation,
        // for each death rune, cut the cost of the highest other rune by 1.
        // Do not run this if there are no DeathRunes to spend.
        if (Math.abs(AbilityCost[DKCostTypes.Death.ordinal()]) > DRSpent) {
            int iHighestCostAbilityIndex = 0;
            int iPreviousCostValue = 0;
            for (int t = 0; t < DKCostTypes.Death.ordinal(); t++) {
                // Is the cost higher than our previous checked value.
                if (AbilityCost[t] > iPreviousCostValue) {
                    // If so, save off the index of that ability.
                    iHighestCostAbilityIndex = t;
                }
                iPreviousCostValue = AbilityCost[t];
            }
            // After going through the full list, spend a death rune and
            // then iterate through that list again.
            AbilityCost[iHighestCostAbilityIndex] -= 1;
            // increment the death runes.
            DRSpent++;
            spendDeathRunes(AbilityCost, DRSpent);
        }
    }
}
