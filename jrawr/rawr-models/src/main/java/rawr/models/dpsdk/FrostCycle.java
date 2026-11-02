// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/FrostCycle.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.CharacterClass;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class FrostCycle {
    int currentTime;
    int GCDTime;
    int spellGCD;
    int meleeGCD;
    int MHFrequency;
    double KMChance;
    double RimeChance;
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
    int DeathRune1;
    int DeathRune2;
    double PhysicalGCDMultiplier;
    double KMApplicationProbability;
    double RimeApplicationProbability;
    double SpellGCDMultiplier;
    AbilityHandler abilities;

    public FrostCycle(rawr.base.character.Character c, CombatTable t, Stats s, CalculationOptionsDPSDK opts, AbilityHandler a) {
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
        KMApplicationProbability = 0;

        RP = 0;
        FrostRune1 = 0;
        FrostRune2 = 0;
        UnholyRune1 = 0;
        UnholyRune2 = 0;
        BloodRune1 = 0;
        BloodRune2 = 0;
        DeathRune1 = 100 * 60 * 1000 + 1;
        DeathRune2 = 100 * 60 * 1000 + 1;
        horn = 0;
        abilities = a;
        character = c;
        combatTable = t;
        stats = s;
        talents = c.getDeathKnightTalents();
        KMChance = (1 / (60 / t.MH.baseSpeed)) * talents.getKillingMachine() * (1 - combatTable.totalMHMiss);
        calcOpts = opts;
        if (opts.getCurrentPresence() == CalculationOptionsDPSDK.Presence.Unholy) {
            meleeGCD = 1000;
            spellGCD = 1000;
        } else {
            meleeGCD = 1500;
            spellGCD = (int) (1500 / ((1 + (StatConversion.getHasteFromRating(stats.getHasteRating(), CharacterClass.DeathKnight))) * (1d + stats.getSpellHaste())));
            spellGCD = Math.max(spellGCD, 1000);
        }

        PhysicalGCDMultiplier = (1 / (1 - (combatTable.dodgedSpecial + combatTable.missedSpecial)));
        SpellGCDMultiplier = (1 / (1 - (combatTable.spellResist)));

        MHFrequency = (int) (combatTable.MH.hastedSpeed * 1000);
        RimeChance = 0.05 * talents.getRime() + (combatTable.DW ? (1 - talents.getRime() * .05) * (talents.getRime() * .05) : 0);
    }

    public Rotation GetDamage(int fightDuration) {
        boolean Oblit = false;
        boolean BS = false;
        boolean IT = false;
        boolean PS = false;
        boolean pest = false;
        while (currentTime < fightDuration) {
            // Priority Queue
            if (GCDTime <= 0) {
                if (talents.isGlyphofDisease()) {
                    // GoD
                    if (((FF < meleeGCD * PhysicalGCDMultiplier || BP < meleeGCD * PhysicalGCDMultiplier)
                            || (FF < BloodRune1 && FF < BloodRune2 && FF < DeathRune1 && FF > DeathRune2)
                            || (FF < BloodRune1 && FF < BloodRune2 && FF > DeathRune1 && FF < DeathRune2)
                            || (FF < BloodRune1 && FF > BloodRune2 && FF < DeathRune1 && FF < DeathRune2)
                            || (FF > BloodRune1 && FF < BloodRune2 && FF < DeathRune1 && FF < DeathRune2)

                            || (BP < BloodRune1 && BP < BloodRune2 && BP < DeathRune1 && BP > DeathRune2)
                            || (BP < BloodRune1 && BP < BloodRune2 && BP > DeathRune1 && BP < DeathRune2)
                            || (BP < BloodRune1 && BP > BloodRune2 && BP < DeathRune1 && BP < DeathRune2)
                            || (BP > BloodRune1 && BP < BloodRune2 && BP < DeathRune1 && BP < DeathRune2))
                            && (FF > 0 && BP > 0)) {
                        if (BloodRune1 < 0) {
                            BloodRune1 += 10000;
                            if (talents.getBloodOfTheNorth() == 5) {
                                DeathRune1 = BloodRune1;
                                BloodRune1 = fightDuration;
                            }
                            pest = true;
                        } else if (BloodRune2 < 0) {
                            BloodRune2 += 10000;
                            if (talents.getBloodOfTheNorth() == 5) {
                                DeathRune2 = BloodRune2;
                                BloodRune2 = fightDuration;
                            }
                            pest = true;
                        } else if (DeathRune1 < 0) {
                            BloodRune1 += 10000;
                            DeathRune1 = fightDuration + 1;
                            if (talents.getBloodOfTheNorth() == 5) {
                                DeathRune1 = BloodRune1;
                                BloodRune1 = fightDuration;
                            }
                            pest = true;
                        } else if (DeathRune2 < 0) {
                            BloodRune2 += 10000;
                            DeathRune2 = fightDuration + 1;
                            if (talents.getBloodOfTheNorth() == 5) {
                                DeathRune2 = BloodRune2;
                                BloodRune2 = fightDuration;
                            }
                            pest = true;
                        }
                    }
                }

                if (RimeApplicationProbability > 0.5 && KMApplicationProbability > 0.5) {
                    // KMRIME
                    // HB
                    occurence.setKMRime(occurence.getKMRime() + KMApplicationProbability * RimeApplicationProbability);
                    occurence.setHowlingBlast(occurence.getHowlingBlast() + (1 - KMApplicationProbability) * RimeApplicationProbability);
                    KMApplicationProbability = 0;
                    RimeApplicationProbability = 0;
                    GCDTime = (int) (spellGCD * RimeApplicationProbability);
                    RP += (talents.getChillOfTheGrave() * 2.5d) * RimeApplicationProbability;
                } else if (KMApplicationProbability > 0.5 && RP >= (talents.isGlyphofFrostStrike() ? 32 : 40)) {
                    // KM FS
                    // FS
                    occurence.setKMFS(occurence.getKMFS() + KMApplicationProbability);
                    occurence.setFrostStrike(occurence.getFrostStrike() + 1 - KMApplicationProbability);
                    KMApplicationProbability = 0;
                    GCDTime = (int) (meleeGCD);
                    RP -= (talents.isGlyphofFrostStrike() ? 32 : 40);
                } else if ((FF <= 0 && (FrostRune1 < 0 || FrostRune2 < 0 || DeathRune1 < 0 || DeathRune2 < 0))
                        || (FF < FrostRune1 && FF < FrostRune2 && FF < DeathRune1 && FF > DeathRune2)
                        || (FF < FrostRune1 && FF < FrostRune2 && FF > DeathRune1 && FF < DeathRune2)
                        || (FF < FrostRune1 && FF > FrostRune2 && FF < DeathRune1 && FF < DeathRune2)
                        || (FF > FrostRune1 && FF < FrostRune2 && FF < DeathRune1 && FF < DeathRune2)) {
                    // FF
                    if (FrostRune1 < 0) {
                        // IT
                        IT = true;
                        FrostRune1 += 10000;
                    } else if (FrostRune2 < 0) {
                        // IT
                        IT = true;
                        FrostRune2 += 10000;
                    } else if (DeathRune1 < 0) {
                        // IT
                        IT = true;
                        BloodRune1 = DeathRune1 + 10000;
                        DeathRune1 = fightDuration;
                        IT = true;
                    } else if (DeathRune2 < 0) {
                        // IT
                        IT = true;
                        BloodRune2 = DeathRune2 + 10000;
                        DeathRune2 = fightDuration;
                    }
                } else if ((BP <= 0 && (UnholyRune1 < 0 || UnholyRune2 < 0 || DeathRune1 < 0 || DeathRune2 < 0))
                        || (BP < UnholyRune1 && BP < UnholyRune2 && BP < DeathRune1 && BP > DeathRune2)
                        || (BP < UnholyRune1 && BP < UnholyRune2 && BP > DeathRune1 && BP < DeathRune2)
                        || (BP < UnholyRune1 && BP > UnholyRune2 && BP < DeathRune1 && BP < DeathRune2)
                        || (BP > UnholyRune1 && BP < UnholyRune2 && BP < DeathRune1 && BP < DeathRune2)) {
                    // BP
                    if (UnholyRune1 < 0) {
                        // PS
                        PS = true;
                        UnholyRune1 += 10000;
                    } else if (UnholyRune2 < 0) {
                        // PS
                        PS = true;
                        UnholyRune2 += 10000;
                    } else if (DeathRune1 < 0) {
                        // PS
                        PS = true;
                        BloodRune1 = DeathRune1 + 10000;
                        DeathRune1 = fightDuration;
                    } else if (DeathRune2 < 0) {
                        // PS
                        PS = true;
                        BloodRune2 = DeathRune2 + 10000;
                        DeathRune2 = fightDuration;
                    }
                }
                // Obliterate
                else if (FrostRune1 < 0 && UnholyRune1 < 0) {
                    FrostRune1 += 10000;
                    UnholyRune1 += 10000;
                    Oblit = true;
                } else if (FrostRune1 < 0 && UnholyRune2 < 0) {
                    FrostRune1 += 10000;
                    UnholyRune2 += 10000;
                    Oblit = true;
                } else if (FrostRune1 < 0 && DeathRune1 < 0) {
                    FrostRune1 += 10000;
                    BloodRune1 = DeathRune1 + 10000;
                    DeathRune1 = fightDuration + 1;
                    Oblit = true;
                } else if (FrostRune1 < 0 && DeathRune2 < 0) {
                    FrostRune1 += 10000;
                    BloodRune2 = DeathRune2 + 10000;
                    DeathRune2 = fightDuration + 1;
                    Oblit = true;
                } else if (FrostRune2 < 0 && UnholyRune1 < 0) {
                    FrostRune2 += 10000;
                    UnholyRune1 += 10000;
                    Oblit = true;
                } else if (FrostRune2 < 0 && UnholyRune2 < 0) {
                    FrostRune2 += 10000;
                    UnholyRune2 += 10000;
                    Oblit = true;
                } else if (FrostRune2 < 0 && DeathRune1 < 0) {
                    FrostRune2 += 10000;
                    BloodRune1 = DeathRune1 + 10000;
                    DeathRune1 = fightDuration + 1;
                    Oblit = true;
                } else if (FrostRune2 < 0 && DeathRune2 < 0) {
                    FrostRune2 += 10000;
                    BloodRune2 = DeathRune2 + 10000;
                    DeathRune2 = fightDuration + 1;
                    Oblit = true;
                } else if (DeathRune1 < 0 && UnholyRune1 < 0) {
                    BloodRune1 = DeathRune1 + 10000;
                    DeathRune1 = fightDuration + 1;
                    UnholyRune1 += 10000;
                    Oblit = true;
                } else if (DeathRune1 < 0 && UnholyRune2 < 0) {
                    BloodRune1 = DeathRune1 + 10000;
                    DeathRune1 = fightDuration + 1;
                    UnholyRune2 += 10000;
                    Oblit = true;
                } else if (DeathRune1 < 0 && DeathRune2 < 0) {
                    BloodRune1 = DeathRune1 + 10000;
                    DeathRune1 = fightDuration + 1;
                    BloodRune2 = DeathRune2 + 10000;
                    DeathRune2 = fightDuration + 1;
                    Oblit = true;
                }
                // Blood Strike
                else if (BloodRune1 < 0) {
                    if (talents.getBloodOfTheNorth() == 5) {
                        DeathRune1 = BloodRune1 + 10000;
                        BloodRune1 = fightDuration + 1;
                    } else {
                        BloodRune1 += 10000;
                    }
                    BS = true;
                } else if (BloodRune2 < 0) {
                    if (talents.getBloodOfTheNorth() == 5) {
                        DeathRune2 = BloodRune2 + 10000;
                        BloodRune2 = fightDuration + 1;
                    } else {
                        BloodRune2 += 10000;
                    }
                    BS = true;
                } else if (RP > (talents.isGlyphofFrostStrike() ? 32 : 40)) {
                    // FS
                    //occurence.FrostStrike++;
                    occurence.setKMFS(occurence.getKMFS() + KMApplicationProbability);
                    occurence.setFrostStrike(occurence.getFrostStrike() + 1 - KMApplicationProbability);
                    KMApplicationProbability = 0;
                    GCDTime = (int) (meleeGCD);
                    RP -= talents.isGlyphofFrostStrike() ? 32 : 40;
                } else if (RimeApplicationProbability > 0) {
                    // Rime
                    // HB
                    occurence.setKMRime(occurence.getKMRime() + KMApplicationProbability * RimeApplicationProbability);
                    occurence.setHowlingBlast(occurence.getHowlingBlast() + (1 - KMApplicationProbability) * RimeApplicationProbability);
                    KMApplicationProbability = 0;
                    RimeApplicationProbability = 0;
                    RP += (talents.getChillOfTheGrave() * 2.5d) * RimeApplicationProbability;
                    GCDTime = (int) (spellGCD * RimeApplicationProbability);
                } else if (horn <= 0) {
                    horn = 20000;
                    occurence.setHorn(occurence.getHorn() + 1);
                    GCDTime = spellGCD;
                    RP += 10;
                }
            }
            if (Oblit) {
                //Obliterate
                GCDTime = (int) (meleeGCD * PhysicalGCDMultiplier);
                RP += 15 + talents.getChillOfTheGrave() * 2.5;
                RimeApplicationProbability += (1 - RimeApplicationProbability) * RimeChance;
                occurence.setObliterate(occurence.getObliterate() + 1);
                Oblit = false;
                if (talents.getAnnihilation() < 3) {
                    BP = -1;
                    FF = -1;
                }
            }
            if (BS) {
                // Blood Strike
                GCDTime = (int) (meleeGCD * PhysicalGCDMultiplier);
                RP += 10;
                occurence.setBloodStrike(occurence.getBloodStrike() + 1);
                BS = false;
            }
            if (pest) {
                FF = (FF % 3000) + 15000 + talents.getEpidemic() * 3000;
                BP = (BP % 3000) + 15000 + talents.getEpidemic() * 3000;
                GCDTime = (int) (spellGCD * SpellGCDMultiplier);
                occurence.setPestilence(occurence.getPestilence() + 1);
                pest = false;
                RP += 10;
            }
            if (IT) {
                FF = 15000 + talents.getEpidemic() * 3000 - 1;
                IT = false;
                GCDTime = (int) (SpellGCDMultiplier * spellGCD);
                occurence.setIcyTouch(occurence.getIcyTouch() + 1);
                RP += 10 + talents.getChillOfTheGrave() * 2.5;
            }
            if (PS) {
                BP = 15000 + talents.getEpidemic() * 3000 - 1;
                GCDTime = (int) (meleeGCD * PhysicalGCDMultiplier);
                occurence.setPlagueStrike(occurence.getPlagueStrike() + 1);
                PS = false;
                RP += 10 + talents.getDirge() * 2.5;
            }
            if (MHFrequency > 0 && currentTime % MHFrequency == 0) {
                KMApplicationProbability += (1 - KMApplicationProbability) * KMChance;
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
            DeathRune1--;
            DeathRune2--;
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
            DeathRune1 = DeathRune1 < -2000 ? -2000 : DeathRune1;
            DeathRune2 = DeathRune2 < -2000 ? -2000 : DeathRune2;
        }
        return occurence;
    }
}
