// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/BloodCycle.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.CharacterClass;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class BloodCycle {
    int currentTime;
    int GCDTime;
    int spellGCD;
    int meleeGCD;
    int MHFrequency;
    Rotation occurence;
    rawr.base.character.Character character;
    CombatTable combatTable;
    DeathKnightTalents talents;
    Stats stats;
    CalculationOptionsDPSDK calcOpts;
    double RP;
    int horn;
    int BP;
    int FF;
    int FrostRune1;
    int FrostRune2;
    int UnholyRune1;
    int UnholyRune2;
    int BloodRune1;
    int BloodRune2;
    int DeathUnholyRune1;
    int DeathUnholyRune2;
    int DeathFrostRune1;
    int DeathFrostRune2;
    double PhysicalGCDMultiplier;
    double SpellGCDMultiplier;

    public BloodCycle(rawr.base.character.Character c, CombatTable t, Stats s, CalculationOptionsDPSDK opts, AbilityHandler a) {
        BP = 0;
        FF = 0;
        currentTime = 0;
        GCDTime = 0;
        occurence = new Rotation();
        occurence.setRotation(Rotation.Type.Custom);
        occurence.setNumDisease(0f);
        occurence.setDiseaseUptime(100f);
        occurence.setDeathCoil(0f);
        occurence.setIcyTouch(0f);
        occurence.setPlagueStrike(0f);
        occurence.setScourgeStrike(0f);
        occurence.setManagedRP(false);
        occurence.setFrostStrike(0f);
        occurence.setHowlingBlast(0f);
        occurence.setObliterate(0f);
        occurence.setBloodStrike(0f);
        occurence.setHeartStrike(0f);
        occurence.setDancingRuneWeapon(0f);
        occurence.setHorn(0f);
        occurence.setGargoyleDuration(30f);
        occurence.setDeathStrike(0f);
        occurence.setGhoulFrenzy(0f);
        occurence.setPestilence(0f);
        occurence.setCurRotationDuration(60f);

        RP = 0;
        FrostRune1 = 0;
        FrostRune2 = 0;
        UnholyRune1 = 0;
        UnholyRune2 = 0;
        BloodRune1 = 0;
        BloodRune2 = 0;
        DeathUnholyRune1 = 100 * 60 * 1000 * 100;
        DeathUnholyRune2 = 100 * 60 * 1000 * 100;
        DeathFrostRune1 = 100 * 60 * 1000 * 100;
        DeathFrostRune2 = 100 * 60 * 1000 * 100;
        horn = 0;
        character = c;
        combatTable = t;
        stats = s;
        talents = c.getDeathKnightTalents();
        calcOpts = opts;
        if (opts.getCurrentPresence() == CalculationOptionsDPSDK.Presence.Unholy) {
            meleeGCD = 1000;
            spellGCD = 1000;
        } else {
            meleeGCD = 1500;
            spellGCD = (int) (1500 / ((1 + (StatConversion.getHasteFromRating(stats.getHasteRating(), CharacterClass.DeathKnight))) * (1d + stats.getSpellHaste())));
            if (spellGCD < 1000) spellGCD = 1000;
        }

        PhysicalGCDMultiplier = (1 / (1 - (combatTable.dodgedSpecial + combatTable.missedSpecial)));
        SpellGCDMultiplier = (1 / (1 - (combatTable.spellResist)));

        MHFrequency = (int) (combatTable.MH.hastedSpeed * 1000);
    }

    public Rotation GetDamage(int fightDuration) {
        boolean DS = false;
        boolean HS = false;
        boolean pest = false;
        boolean IT = false;
        boolean PS = false;
        while (currentTime < fightDuration) {
            // Priority Queue
            if (GCDTime <= 0) {
                if (talents.isGlyphofDisease()) {
                    // GoD
                    if (((FF < meleeGCD * PhysicalGCDMultiplier || BP < meleeGCD * PhysicalGCDMultiplier)
                            || (FF < BloodRune1 && FF < BloodRune2 && FF < DeathFrostRune1 && FF < DeathFrostRune2 && FF < DeathUnholyRune1 && FF > DeathUnholyRune2)
                            || (FF < BloodRune1 && FF < BloodRune2 && FF < DeathFrostRune1 && FF < DeathFrostRune2 && FF > DeathUnholyRune1 && FF < DeathUnholyRune2)
                            || (FF < BloodRune1 && FF < BloodRune2 && FF < DeathFrostRune1 && FF > DeathFrostRune2 && FF < DeathUnholyRune1 && FF < DeathUnholyRune2)
                            || (FF < BloodRune1 && FF < BloodRune2 && FF > DeathFrostRune1 && FF < DeathFrostRune2 && FF < DeathUnholyRune1 && FF < DeathUnholyRune2)
                            || (FF < BloodRune1 && FF > BloodRune2 && FF < DeathFrostRune1 && FF < DeathFrostRune2 && FF < DeathUnholyRune1 && FF < DeathUnholyRune2)
                            || (FF > BloodRune1 && FF < BloodRune2 && FF < DeathFrostRune1 && FF < DeathFrostRune2 && FF < DeathUnholyRune1 && FF < DeathUnholyRune2)

                            || (BP < BloodRune1 && BP < BloodRune2 && BP < DeathFrostRune1 && BP < DeathFrostRune2 && BP < DeathUnholyRune1 && BP > DeathUnholyRune2)
                            || (BP < BloodRune1 && BP < BloodRune2 && BP < DeathFrostRune1 && BP < DeathFrostRune2 && BP > DeathUnholyRune1 && BP < DeathUnholyRune2)
                            || (BP < BloodRune1 && BP < BloodRune2 && BP < DeathFrostRune1 && BP > DeathFrostRune2 && BP < DeathUnholyRune1 && BP < DeathUnholyRune2)
                            || (BP < BloodRune1 && BP < BloodRune2 && BP > DeathFrostRune1 && BP < DeathFrostRune2 && BP < DeathUnholyRune1 && BP < DeathUnholyRune2)
                            || (BP < BloodRune1 && BP > BloodRune2 && BP < DeathFrostRune1 && BP < DeathFrostRune2 && BP < DeathUnholyRune1 && BP < DeathUnholyRune2)
                            || (BP > BloodRune1 && BP < BloodRune2 && BP < DeathFrostRune1 && BP < DeathFrostRune2 && BP < DeathUnholyRune1 && BP < DeathUnholyRune2))
                            && (FF > 0 && BP > 0)) {
                        if (BloodRune1 <= 0) {
                            BloodRune1 += 10000;
                            pest = true;
                        } else if (BloodRune2 <= 0) {
                            BloodRune2 += 10000;
                            pest = true;
                        } else if (DeathFrostRune1 <= 0) {
                            FrostRune1 = DeathFrostRune1 + 10000;
                            DeathFrostRune1 = fightDuration + 1;
                            pest = true;
                        } else if (DeathFrostRune2 <= 0) {
                            FrostRune2 = DeathFrostRune2 + 10000;
                            DeathUnholyRune2 = fightDuration + 1;
                            pest = true;
                        } else if (DeathUnholyRune1 <= 0) {
                            UnholyRune1 = DeathUnholyRune1 + 10000;
                            DeathUnholyRune1 = fightDuration + 1;
                            pest = true;
                        } else if (DeathUnholyRune2 <= 0) {
                            UnholyRune2 = DeathUnholyRune2 + 10000;
                            DeathUnholyRune2 = fightDuration + 1;
                            pest = true;
                        }
                    } else if (FF < spellGCD * SpellGCDMultiplier && (FrostRune1 <= 0 || FrostRune2 <= 0 || DeathFrostRune1 <= 0 || DeathFrostRune2 <= 0 || DeathUnholyRune1 <= 0 || DeathUnholyRune2 <= 0)) {
                        // FF
                        if (FrostRune1 <= 0) {
                            IT = true;
                            FrostRune1 += 10000;
                        } else if (FrostRune2 <= 0) {
                            IT = true;
                            FrostRune2 += 10000;
                        } else if (DeathFrostRune1 <= 0) {
                            IT = true;
                            FrostRune1 = DeathFrostRune1 + 10000;
                            DeathFrostRune1 = fightDuration + 1;
                        } else if (DeathFrostRune2 <= 0) {
                            IT = true;
                            FrostRune2 = DeathFrostRune2 + 10000;
                            DeathFrostRune2 = fightDuration + 1;
                        } else if (DeathUnholyRune1 <= 0) {
                            IT = true;
                            UnholyRune1 = DeathUnholyRune2 + 10000;
                            DeathUnholyRune1 = fightDuration + 1;
                        } else if (DeathUnholyRune2 <= 0) {
                            IT = true;
                            UnholyRune2 = DeathUnholyRune2 + 10000;
                            DeathUnholyRune2 = fightDuration + 1;
                        }
                    } else if (BP < meleeGCD * PhysicalGCDMultiplier && (UnholyRune1 <= 0 || UnholyRune2 <= 0 || DeathFrostRune1 <= 0 || DeathFrostRune2 <= 0 || DeathUnholyRune1 <= 0 || DeathUnholyRune2 <= 0)) {
                        // BP
                        if (UnholyRune1 <= 0) {
                            PS = true;
                            UnholyRune1 += 10000;
                        } else if (UnholyRune2 <= 0) {
                            PS = true;
                            UnholyRune2 += 10000;
                        } else if (DeathUnholyRune1 <= 0) {
                            PS = true;
                            UnholyRune1 = DeathUnholyRune1 + 10000;
                            DeathUnholyRune1 = fightDuration + 1;
                        } else if (DeathUnholyRune2 <= 0) {
                            PS = true;
                            UnholyRune2 = DeathUnholyRune2 + 10000;
                            DeathUnholyRune2 = fightDuration + 1;
                        } else if (DeathFrostRune1 <= 0) {
                            PS = true;
                            FrostRune1 = DeathFrostRune1 + 10000;
                            DeathFrostRune1 = fightDuration + 1;
                        } else if (DeathFrostRune2 <= 0) {
                            PS = true;
                            FrostRune2 = DeathFrostRune2 + 10000;
                            DeathFrostRune2 = fightDuration + 1;
                        }
                    }
                } else {
                    if ((FF <= spellGCD * SpellGCDMultiplier && (FrostRune1 <= 0 || FrostRune2 <= 0 || DeathFrostRune1 <= 0 || DeathFrostRune2 <= 0 || DeathUnholyRune1 <= 0 || DeathUnholyRune2 <= 0))
                            || (FF < FrostRune1 && FF < FrostRune2 && FF < DeathFrostRune1 && FF < DeathFrostRune2 && FF < DeathUnholyRune1 && FF > DeathUnholyRune2)
                            || (FF < FrostRune1 && FF < FrostRune2 && FF < DeathFrostRune1 && FF < DeathFrostRune2 && FF > DeathUnholyRune1 && FF < DeathUnholyRune2)
                            || (FF < FrostRune1 && FF < FrostRune2 && FF < DeathFrostRune1 && FF > DeathFrostRune2 && FF < DeathUnholyRune1 && FF < DeathUnholyRune2)
                            || (FF < FrostRune1 && FF < FrostRune2 && FF > DeathFrostRune1 && FF < DeathFrostRune2 && FF < DeathUnholyRune1 && FF < DeathUnholyRune2)
                            || (FF < FrostRune1 && FF > FrostRune2 && FF < DeathFrostRune1 && FF < DeathFrostRune2 && FF < DeathUnholyRune1 && FF < DeathUnholyRune2)
                            || (FF > FrostRune1 && FF < FrostRune2 && FF < DeathFrostRune1 && FF < DeathFrostRune2 && FF < DeathUnholyRune1 && FF < DeathUnholyRune2)) {
                        // FF
                        if (FrostRune1 <= 0) {
                            IT = true;
                            FrostRune1 += 10000;
                        } else if (FrostRune2 <= 0) {
                            IT = true;
                            FrostRune2 += 10000;
                        } else if (DeathFrostRune1 <= 0) {
                            IT = true;
                            FrostRune1 = DeathFrostRune1 + 10000;
                            DeathFrostRune1 = fightDuration + 1;
                        } else if (DeathFrostRune2 <= 0) {
                            IT = true;
                            FrostRune2 = DeathFrostRune2 + 10000;
                            DeathFrostRune2 = fightDuration + 1;
                        } else if (DeathUnholyRune1 <= 0) {
                            IT = true;
                            UnholyRune1 = DeathUnholyRune2 + 10000;
                            DeathUnholyRune1 = fightDuration + 1;
                        } else if (DeathUnholyRune2 <= 0) {
                            IT = true;
                            UnholyRune2 = DeathUnholyRune2 + 10000;
                            DeathUnholyRune2 = fightDuration + 1;
                        }
                    } else if ((BP <= meleeGCD * PhysicalGCDMultiplier && (UnholyRune1 <= 0 || UnholyRune2 <= 0 || DeathFrostRune1 <= 0 || DeathFrostRune2 <= 0 || DeathUnholyRune1 <= 0 || DeathUnholyRune2 <= 0))
                            || (BP < UnholyRune1 && BP < UnholyRune2 && BP < DeathFrostRune1 && BP < DeathFrostRune2 && BP < DeathUnholyRune1 && BP > DeathUnholyRune2)
                            || (BP < UnholyRune1 && BP < UnholyRune2 && BP < DeathFrostRune1 && BP < DeathFrostRune2 && BP > DeathUnholyRune1 && BP < DeathUnholyRune2)
                            || (BP < UnholyRune1 && BP < UnholyRune2 && BP < DeathFrostRune1 && BP > DeathFrostRune2 && BP < DeathUnholyRune1 && BP < DeathUnholyRune2)
                            || (BP < UnholyRune1 && BP < UnholyRune2 && BP > DeathFrostRune1 && BP < DeathFrostRune2 && BP < DeathUnholyRune1 && BP < DeathUnholyRune2)
                            || (BP < UnholyRune1 && BP > UnholyRune2 && BP < DeathFrostRune1 && BP < DeathFrostRune2 && BP < DeathUnholyRune1 && BP < DeathUnholyRune2)
                            || (BP > UnholyRune1 && BP < UnholyRune2 && BP < DeathFrostRune1 && BP < DeathFrostRune2 && BP < DeathUnholyRune1 && BP < DeathUnholyRune2)) {
                        // BP
                        if (UnholyRune1 <= 0) {
                            PS = true;
                            UnholyRune1 += 10000;
                        } else if (UnholyRune2 <= 0) {
                            PS = true;
                            UnholyRune2 += 10000;
                        } else if (DeathUnholyRune1 <= 0) {
                            PS = true;
                            UnholyRune1 = DeathUnholyRune1 + 10000;
                            DeathUnholyRune1 = fightDuration + 1;
                        } else if (DeathUnholyRune2 <= 0) {
                            PS = true;
                            UnholyRune2 = DeathUnholyRune2 + 10000;
                            DeathUnholyRune2 = fightDuration + 1;
                        } else if (DeathFrostRune1 <= 0) {
                            PS = true;
                            FrostRune1 = DeathFrostRune1 + 10000;
                            DeathFrostRune1 = fightDuration + 1;
                        } else if (DeathFrostRune2 <= 0) {
                            PS = true;
                            FrostRune2 = DeathFrostRune2 + 10000;
                            DeathFrostRune2 = fightDuration + 1;
                        }
                    }
                }
                // HS
                if (BloodRune1 <= 0) {
                    HS = true;
                    BloodRune1 += 10000;
                } else if (BloodRune2 <= 0) {
                    HS = true;
                    BloodRune2 += 10000;
                } else if (DeathFrostRune1 <= 0) {
                    HS = true;
                    FrostRune1 = DeathFrostRune1 + 10000;
                    DeathFrostRune1 = fightDuration + 1;
                } else if (DeathFrostRune2 <= 0) {
                    HS = true;
                    FrostRune2 = DeathFrostRune2 + 10000;
                    DeathFrostRune2 = fightDuration + 1;
                } else if (DeathUnholyRune1 <= 0) {
                    HS = true;
                    UnholyRune1 = DeathUnholyRune1 + 10000;
                    DeathUnholyRune1 = fightDuration + 1;
                } else if (DeathUnholyRune2 <= 0) {
                    HS = true;
                    UnholyRune2 = DeathUnholyRune2 + 10000;
                    DeathUnholyRune2 = fightDuration + 1;
                }
                // Death Strike
                else if (FrostRune1 <= 0 && UnholyRune1 <= 0) {
                    FrostRune1 += 10000;
                    UnholyRune1 += 10000;
                    if (talents.getDeathRuneMastery() == 3) {
                        DeathFrostRune1 = FrostRune1;
                        FrostRune1 = fightDuration + 1;
                        DeathUnholyRune1 = UnholyRune1;
                        UnholyRune1 = fightDuration + 1;
                    }
                    DS = true;
                } else if (FrostRune1 <= 0 && UnholyRune2 <= 0) {
                    FrostRune1 += 10000;
                    UnholyRune2 += 10000;
                    if (talents.getDeathRuneMastery() == 3) {
                        DeathFrostRune1 = FrostRune1;
                        FrostRune1 = fightDuration + 1;
                        DeathUnholyRune2 = UnholyRune2;
                        UnholyRune2 = fightDuration + 1;
                    }
                    DS = true;
                } else if (FrostRune2 <= 0 && UnholyRune1 <= 0) {
                    FrostRune2 += 10000;
                    UnholyRune1 += 10000;
                    if (talents.getDeathRuneMastery() == 3) {
                        DeathFrostRune2 = FrostRune2;
                        FrostRune2 = fightDuration + 1;
                        DeathUnholyRune1 = UnholyRune1;
                        UnholyRune1 = fightDuration + 1;
                    }
                    DS = true;
                } else if (FrostRune2 <= 0 && UnholyRune2 <= 0) {
                    FrostRune2 += 10000;
                    UnholyRune2 += 10000;
                    if (talents.getDeathRuneMastery() == 3) {
                        DeathFrostRune2 = FrostRune2;
                        FrostRune2 = fightDuration + 1;
                        DeathUnholyRune2 = UnholyRune2;
                        UnholyRune2 = fightDuration + 1;
                    }
                    DS = true;
                } else if (RP > 40) {
                    // DC
                    occurence.setDeathCoil(occurence.getDeathCoil() + 1);
                    GCDTime = (int) (spellGCD);
                    RP -= 40;
                } else if (horn <= 0) {
                    horn = 20000;
                    occurence.setHorn(occurence.getHorn() + 1);
                    GCDTime = spellGCD;
                    RP += 10;
                }
            }
            if (pest) {
                FF = (FF % 3000) + 15000 + talents.getEpidemic() * 3000;
                BP = (BP % 3000) + 15000 + talents.getEpidemic() * 3000;
                GCDTime = (int) (spellGCD * SpellGCDMultiplier);
                occurence.setPestilence(occurence.getPestilence() + 1);
                pest = false;
                RP += 10;
            } else if (IT) {
                FF = 15000 + talents.getEpidemic() * 3000 - 1;
                IT = false;
                GCDTime = (int) (SpellGCDMultiplier * spellGCD);
                occurence.setIcyTouch(occurence.getIcyTouch() + 1);
                RP += 10 + talents.getChillOfTheGrave() * 2.5;
            } else if (PS) {
                BP = 15000 + talents.getEpidemic() * 3000 - 1;
                GCDTime = (int) (meleeGCD * PhysicalGCDMultiplier);
                occurence.setPlagueStrike(occurence.getPlagueStrike() + 1);
                RP += 10 + talents.getDirge();
                PS = false;
            } else if (DS) {
                // Death Strike
                GCDTime = (int) (meleeGCD * PhysicalGCDMultiplier);
                RP += 15 + talents.getDirge() * 2.5;
                occurence.setDeathStrike(occurence.getDeathStrike() + 1);
                DS = false;
            } else if (HS) {
                // Blood Strike
                GCDTime = (int) (meleeGCD * PhysicalGCDMultiplier);
                RP += 10;
                occurence.setHeartStrike(occurence.getHeartStrike() + 1);
                occurence.setDeathCoil(occurence.getDeathCoil() + talents.getSuddenDoom() * .05);
                HS = false;
            }

            if (FF % 3000 == 0 && FF >= 0) {
                occurence.setFFTick(occurence.getFFTick() + 1);
            }
            if (BP % 3000 == 0 && BP >= 0) {
                occurence.setBPTick(occurence.getBPTick() + 1);
            }
            if (currentTime % 5000 == 0) {
                RP += talents.getButchery();
            }
            if (RP > 100 + talents.getRunicPowerMastery() * 15) {
                RP = 100 + talents.getRunicPowerMastery() * 15;
            }

            // time keeping
            currentTime++;
            FrostRune1--;
            FrostRune2--;
            UnholyRune1--;
            UnholyRune2--;
            BloodRune1--;
            BloodRune2--;
            DeathFrostRune1--;
            DeathFrostRune2--;
            DeathUnholyRune1--;
            DeathUnholyRune2--;
            horn--;
            GCDTime--;
            FF--;
            BP--;
            BloodRune1 = BloodRune1 < -2000 ? -2000 : BloodRune1;
            BloodRune2 = BloodRune2 < -2000 ? -2000 : BloodRune2;
            FrostRune1 = FrostRune1 < -2000 ? -2000 : FrostRune1;
            FrostRune2 = FrostRune2 < -2000 ? -2000 : FrostRune2;
            UnholyRune1 = UnholyRune1 < -2000 ? -2000 : UnholyRune1;
            UnholyRune2 = UnholyRune2 < -2000 ? -2000 : UnholyRune2;
            DeathFrostRune1 = DeathFrostRune1 < -2000 ? -2000 : DeathFrostRune1;
            DeathFrostRune2 = DeathFrostRune2 < -2000 ? -2000 : DeathFrostRune2;
            DeathUnholyRune1 = DeathUnholyRune1 < -2000 ? -2000 : DeathUnholyRune1;
            DeathUnholyRune2 = DeathUnholyRune2 < -2000 ? -2000 : DeathUnholyRune2;
        }
        return occurence;
    }
}
