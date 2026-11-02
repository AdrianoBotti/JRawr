// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/ShotRotation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

import rawr.base.character.Character;
import rawr.base.talents.DeathKnightTalents;

public class Rotation {
    public float m_fDodge = 0f;
    public float m_fParry = 0f;
    public float m_fPhysicalHaste = 0f;

    public Type curRotationType = Type.Custom;

    public float curRotationDuration = 0f;  // rotation duration in seconds

    // disease info
    public float avgDiseaseMult = 0f;
    public float numDisease = 0f;
    public float diseaseUptime = 0f;

    // ability number of times per rotation used
    public float DeathCoil = 0f;
    public float IcyTouch = 0f;
    public float PlagueStrike = 0f;
    public float FrostFever = 0f;
    public float BloodPlague = 0f;
    public float ScourgeStrike = 0f;
    public float FrostStrike = 0f;
    public float HowlingBlast = 0f;
    public float Obliterate = 0f;
    public float DeathStrike = 0f;
    public float BloodStrike = 0f;
    public float HeartStrike = 0f;
    public float HornOfWinter = 0f;
    public float Pestilence = 0f;
    public float BloodBoil = 0f;
    public float DeathNDecay = 0f;
    // Defensive Rotations
    public float BoneShield = 0f;
    public float IceBoundFortitude = 0f;
    public float UnbreakableArmor = 0f;
    public float RuneStrike = 0f;

    public boolean fourT7 = false;
    public boolean GlyphofIT = false;
    public boolean GlyphofFS = false;
    public boolean managedRP = true;
    public float GCDTime;
    public float RP;
    public float[] AbilityCost = new float[4];

    public DeathKnightTalents tTalents;

    public Rotation() {
        this.setRotation(this.curRotationType);
    }

    public Rotation(DeathKnightTalents t) {
        if (null != t) {
            tTalents = t.clone();
            this.getRotationByTalents(tTalents);
        } else {
            this.setRotation(this.curRotationType);
        }
    }

    public enum Type {
        Custom, Blood, Frost, Unholy
    }

    public float getMeleeSpecialsPerSecond() {
        float temp;
        temp = PlagueStrike + ScourgeStrike + FrostStrike + Obliterate + DeathStrike + BloodStrike + HeartStrike;
        // As per Bloodysorc, BCB should probably be included in this.
        temp = temp / curRotationDuration;
        return temp;
    }

    public float getSpellSpecialsPerSecond() {
        float temp;
        temp = DeathCoil + IcyTouch + HowlingBlast + DeathNDecay + BloodBoil;
        temp = temp / curRotationDuration;
        return temp;
    }

    public float getRP(DeathKnightTalents talents, Character character) {
        fourT7 = character.activeBuffsContains("Scourgeborne Battlegear 4 Piece Bonus");
        this.GlyphofFS = talents.isGlyphofFrostStrike();

        // Abilities that grant RP:
        RP = ((15 + (fourT7 ? 5 : 0) + 2.5f * talents.getChillOfTheGrave()) * Obliterate) +
                ((15 + (fourT7 ? 5 : 0) + 2.5f * talents.getDirge()) * ScourgeStrike) +
                ((15 + (fourT7 ? 5 : 0) + 2.5f * talents.getDirge()) * DeathStrike) +
                ((10 + 2.5f * talents.getDirge()) * PlagueStrike) +
                ((10 + 2.5f * talents.getChillOfTheGrave()) * IcyTouch) +
                ((15 + 2.5f * talents.getChillOfTheGrave()) * HowlingBlast) +
                (15 * DeathNDecay) +
                (10 * (BloodStrike + HeartStrike + BloodBoil + HornOfWinter)) +
                ((curRotationDuration / 5f) * talents.getButchery());

        if (managedRP) {
            RP = manageRPDumping(talents, RP);
        } else {
            RP -= ((40 * DeathCoil)
                    + ((GlyphofFS ? 32 : 40) * FrostStrike)
                    + (20 * RuneStrike));
        }
        return RP;
    }

    /**
     * Use abilities that have Runic Power to dump the available RP per rotation.
     * Primarily focused on RuneStrike.
     */
    public float manageRPDumping(DeathKnightTalents talents, float RP) {
        // How many RS limited by RP.
        float fMax_ByRP = RP / 20f;

        // So for right now, we just setup how many RS's we get, and
        // Zero out all the rest of the values.
        RuneStrike = fMax_ByRP;
        RP = 0;
        FrostStrike = 0;
        DeathCoil = 0;
        RP = 0;

        return RP;
    }

    public float getRotationDuration() {
        float rotDuration = 20;
        float GCDTime = getGCDTime();
        float RuneTime = getRuneTime();
        float RPTime = getRPTime();

        return Math.max(GCDTime, RuneTime);
    }

    /**
     * Get the total time spent in GCD for the rotation.
     */
    public float getGCDTime() {
        // Melee
        float meleeGCD = 1.5f;

        // Spells / effects
        float spellGCD = getGCDHasted();
        GCDTime = spellGCD * (
                +DeathCoil // RP
                        + IcyTouch // F
                        + HowlingBlast // FU
                        + HornOfWinter // none
                        + DeathNDecay // BFU
                        + BloodBoil // B
                        + Pestilence // B
        ) +
                meleeGCD * (
                        PlagueStrike // U
                                + ScourgeStrike // FU
                                + FrostStrike // RP
                                + Obliterate // FU
                                + DeathStrike // FU
                                + BloodStrike // B
                                + HeartStrike // B
                );
        return GCDTime;
    }

    public float getRuneTime() {
        float TimeSpentRuneCDs = 0;
        AbilityCost[(int) DKCostTypes.Frost.ordinal()] = (
                ScourgeStrike // FU
                        + Obliterate // FU
                        + DeathStrike // FU
                        + IcyTouch // F
                        + HowlingBlast // FU
                        + DeathNDecay // BFU
        );
        AbilityCost[(int) DKCostTypes.UnHoly.ordinal()] = (
                PlagueStrike // U
                        + ScourgeStrike // FU
                        + Obliterate // FU
                        + DeathStrike // FU
                        + HowlingBlast // FU
                        + DeathNDecay // BFU
        );
        AbilityCost[(int) DKCostTypes.Blood.ordinal()] = (
                +BloodStrike // B
                        + HeartStrike // B
                        + DeathNDecay // BFU
                        + BloodBoil // B
                        + Pestilence // B
        );
        // Death Runes
        if (null != tTalents) {
            if (tTalents.getBloodOfTheNorth() > 0) {
                AbilityCost[(int) DKCostTypes.Death.ordinal()] = (BloodStrike
                        + Pestilence) * (tTalents.getBloodOfTheNorth() / 3);
            }
            if (tTalents.getReaping() > 0) {
                AbilityCost[(int) DKCostTypes.Death.ordinal()] = (BloodStrike
                        + Pestilence) * (tTalents.getReaping() / 3);
            }
            if (tTalents.getDeathRuneMastery() > 0) {
                AbilityCost[(int) DKCostTypes.Death.ordinal()] += (DeathStrike
                        + Obliterate) * 2 * (tTalents.getDeathRuneMastery() / 3);
            }

            spendDeathRunes(AbilityCost, 0);
        }

        float MaxRunes = Math.max(AbilityCost[(int) DKCostTypes.Frost.ordinal()], AbilityCost[(int) DKCostTypes.Blood.ordinal()]);
        MaxRunes = Math.max(MaxRunes, AbilityCost[(int) DKCostTypes.UnHoly.ordinal()]);
        MaxRunes = Math.max(MaxRunes, AbilityCost[(int) DKCostTypes.Death.ordinal()]);

        // Each rune requires 10 secs. But there are 2 of them.
        TimeSpentRuneCDs = (MaxRunes * 10 / 2);

        return TimeSpentRuneCDs;
    }

    /**
     * Pass in the AbilityCost[] to process the DeathRunes
     * Stolen from CombatTable2
     */
    public void spendDeathRunes(float[] AbilityCost, int DRSpent) {
        // Let's put some boundary conditions 'round here.
        if (AbilityCost == null)
            return;
        // 10 min fight w/ 2 runes available at Death runes every rune cooldown.
        if (Math.abs(DRSpent) > 120)
            return;
        for (int i = 0; i <= (int) DKCostTypes.Death.ordinal(); i++) {
            if (Math.abs(AbilityCost[i]) > 120)
                return;
        }
        // Need to figure out how to factor in Death Runes
        // Since each death rune replaces any other rune on the rotation,
        // for each death rune, cut the cost of the highest other rune by 1.
        // Do not run this if there are no DeathRunes to spend.
        if (Math.abs(AbilityCost[(int) DKCostTypes.Death.ordinal()]) > DRSpent) {
            int iHighestCostAbilityIndex = 0;
            float fPreviousCostValue = 0;
            for (int t = 0; t < (int) DKCostTypes.Death.ordinal(); t++) {
                // Is the cost higher than our previous checked value.
                if (AbilityCost[t] > fPreviousCostValue) {
                    // If so, save off the index of that ability.
                    iHighestCostAbilityIndex = t;
                }
                fPreviousCostValue = AbilityCost[t];
            }
            // After going through the full list, spend a death rune and
            // then iterate through that list again.
            AbilityCost[iHighestCostAbilityIndex] -= 1;
            // increment the death runes.
            DRSpent++;
            spendDeathRunes(AbilityCost, DRSpent);
        }
    }

    // How long are the GCDs for RP cost abilities?
    public float getRPTime() {
        // Melee
        float meleeGCD = 1.5f;

        // Spells / effects
        float spellGCD = getGCDHasted();

        float GCDTimeforRPAbilties = spellGCD * (
                +DeathCoil // RP
                        + HornOfWinter // none
        ) +
                meleeGCD * (
                        +FrostStrike // RP
                );

        return GCDTimeforRPAbilties;
    }

    /**
     * How fast is the GCD with the current character's stats?
     */
    public float getGCDHasted() {
        float fNormalGCD = 1.5f;
        float fHastedGCD = Math.max(1f, fNormalGCD / (1f + this.m_fPhysicalHaste));
        return fHastedGCD;
    }

    // Rotations set by Skeleton Jack's 3.2 build and rotation notes.
    public void setRotation(Type t) {
        curRotationType = t;
        managedRP = false;
        switch (curRotationType) {
            case Blood:
                numDisease = 2f;
                diseaseUptime = 100f;
                DeathCoil = 2f;
                IcyTouch = 1f;
                PlagueStrike = 1f;
                ScourgeStrike = 0f;
                FrostStrike = 0f;
                HowlingBlast = 0f;
                Obliterate = 0f;
                DeathStrike = 2f;
                BloodStrike = 0f;
                HeartStrike = 6f;
                Pestilence = 0f;
                RuneStrike = 2f;
                curRotationDuration = 20f;
                break;
            case Frost:
                numDisease = 2f;
                diseaseUptime = 100f;
                DeathCoil = 0f;
                IcyTouch = 2f;
                PlagueStrike = 2f;
                ScourgeStrike = 0f;
                FrostStrike = 1f;
                HowlingBlast = 0f;
                Obliterate = 3f;
                BloodStrike = 2f;
                HeartStrike = 0f;
                DeathStrike = 0f;
                RuneStrike = 2f;
                Pestilence = 0f;
                curRotationDuration = 20f;
                break;
            case Unholy:
                numDisease = 3f;
                diseaseUptime = 100f;
                DeathCoil = 0f;
                IcyTouch = 1f;
                PlagueStrike = 1f;
                ScourgeStrike = 4f;
                FrostStrike = 0f;
                HowlingBlast = 0f;
                Obliterate = 0f;
                BloodStrike = 2f;
                HeartStrike = 0f;
                curRotationDuration = 20f;
                DeathStrike = 0f;
                RuneStrike = 2f;
                Pestilence = 0f;
                break;
            case Custom:
                break;
        }
    }

    /**
     * Get the type of rotation we should be using based on talents.
     */
    public void getRotationByTalents(DeathKnightTalents t) {
        if (curRotationType == Type.Custom) {
            managedRP = false;
            // if talents are all 0, then setup a basic rotation:
            curRotationType = Type.Custom;
            // we're going to just spam a very basic rotation.
            // IT-PS-BS-BS-DS-DS-RP
            numDisease = 2f;
            diseaseUptime = 100f;
            IcyTouch = 2f;
            PlagueStrike = 2f;
            BloodStrike = 2f;
            Obliterate = 0f;
            DeathStrike = 2f;
            RuneStrike = 3f;
            Pestilence = 0f;

            DeathCoil = 2f;
            ScourgeStrike = 0f;
            FrostStrike = 0f;
            HowlingBlast = 0f;
            HeartStrike = 0f;
            curRotationDuration = 15f;
        }
        final int indexBlood = 0; // start index of Blood Talents.
        final int indexFrost = 28; // start index of Frost Talents.
        final int indexUnholy = indexFrost + 29; // start index of Unholy Talents.
        int[] TalentCounter = new int[4];
        int index = indexBlood;
        for (int i : t.getData()) {
            if (i > 0) {
                // Blood
                if (index < indexFrost)
                    TalentCounter[Type.Blood.ordinal()]++;
                    // Frost
                else if ((indexFrost <= index) && (index < indexUnholy)) {
                    TalentCounter[Type.Frost.ordinal()]++;
                }
                // Unholy
                else if (index >= indexUnholy) {
                    TalentCounter[Type.Unholy.ordinal()]++;
                }
            }
            index++;
        }
        if ((TalentCounter[Type.Blood.ordinal()] > TalentCounter[Type.Frost.ordinal()]) && (TalentCounter[Type.Blood.ordinal()] > TalentCounter[Type.Unholy.ordinal()])) {
            // Blood
            curRotationType = Type.Blood;
        } else if ((TalentCounter[Type.Frost.ordinal()] > TalentCounter[Type.Blood.ordinal()]) && (TalentCounter[Type.Frost.ordinal()] > TalentCounter[Type.Unholy.ordinal()])) {
            // Frost
            curRotationType = Type.Frost;
        } else if ((TalentCounter[Type.Unholy.ordinal()] > TalentCounter[Type.Frost.ordinal()]) && (TalentCounter[Type.Unholy.ordinal()] > TalentCounter[Type.Blood.ordinal()])) {
            // Unholy
            curRotationType = Type.Unholy;
        }

        managedRP = true;
        // Frost Rotations
        if (curRotationType == Type.Frost) {
            numDisease = 2f;
            diseaseUptime = 100f;
            IcyTouch = 2f;
            PlagueStrike = 2f;
            ScourgeStrike = 0f;
            FrostStrike = 0f;
            if (t.getFrostStrike() > 0) {
                DeathCoil = 0f;
                FrostStrike = 2f;
            } else {
                DeathCoil = 2f;
                FrostStrike = 0f;
            }
            HowlingBlast = 0f;
            Obliterate = 3f;
            BloodStrike = 2f;
            HeartStrike = 0f;
            DeathStrike = 0f;
            RuneStrike = 3f;
            Pestilence = 0f;
            curRotationDuration = 15f;

            if (t.isGlyphofHowlingBlast() && t.getHowlingBlast() > 0) {
                // Single Disease Glyphed HB rotation
                // Means that we start w/ HB for the FF hit, and factor in PS later, while HB is on CD.
                numDisease = 1f;
                IcyTouch = 1f;
                PlagueStrike = 1f;
                HowlingBlast = 1f;
            }
            if (t.getRime() > 0 && t.getHowlingBlast() > 0) {
                // Additional HB proc based on Rime.
                HowlingBlast += .5f;
            }
        }
        // Blood Rotations
        else if (curRotationType == Type.Blood) {
            numDisease = 2f;
            diseaseUptime = 100f;
            DeathCoil = 2f;
            IcyTouch = 1f;
            PlagueStrike = 1f;
            ScourgeStrike = 0f;
            FrostStrike = 0f;
            HowlingBlast = 0f;
            Obliterate = 0f;
            DeathStrike = 2f;
            if (HeartStrike > 0) {
                HeartStrike = 6f;
                BloodStrike = 0f;
            } else {
                HeartStrike = 0f;
                BloodStrike = 6f;
            }
            Pestilence = 0f;
            RuneStrike = 2f;
            curRotationDuration = 15f;
        }
        // UnHoly
        else if (curRotationType == Type.Unholy) {
            numDisease = 3f;
            diseaseUptime = 100f;
            DeathCoil = 2f;
            IcyTouch = 1f;
            PlagueStrike = 1f;
            ScourgeStrike = 0f;
            if (t.getScourgeStrike() > 0)
                ScourgeStrike = 4f;
            else {
                DeathStrike = 4f;
            }
            FrostStrike = 0f;
            HowlingBlast = 0f;
            Obliterate = 0f;
            BloodStrike = 2f;
            HeartStrike = 0f;
            curRotationDuration = 20f;
            RuneStrike = 2f;
            Pestilence = 0f;
        }
    }
}
