// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/UnholyCycle.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.CharacterClass;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class UnholyCycle {
    int currentTime;
    int GCDTime;
    int spellGCD;
    int meleeGCD;
    int MHFrequency;
    int RuneCD;
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
    double SpellGCDMultiplier;
    AbilityHandler abilities;

    public UnholyCycle(rawr.base.character.Character c, CombatTable t, Stats s, CalculationOptionsDPSDK opts, AbilityHandler a) {
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
        DeathRune1 = 100 * 60 * 1000 + 1;
        DeathRune2 = 100 * 60 * 1000 + 1;
        horn = 0;
        abilities = a;
        character = c;
        combatTable = t;
        stats = s;
        talents = c.getDeathKnightTalents();
        calcOpts = opts;
        if (opts.getCurrentPresence() == CalculationOptionsDPSDK.Presence.Unholy) {
            meleeGCD = 1000;
            spellGCD = 1000;
            RuneCD = (int) (10000 * (1 - talents.getImprovedUnholyPresence() * .05));
        } else {
            meleeGCD = 1500;
            spellGCD = (int) (1500 / ((1 + (StatConversion.getHasteFromRating(stats.getHasteRating(), CharacterClass.DeathKnight))) * (1d + stats.getSpellHaste())));
            if (spellGCD < 1000) spellGCD = 1000;
            RuneCD = 10000;
        }

        PhysicalGCDMultiplier = (1 / (1 - (combatTable.dodgedSpecial + combatTable.missedSpecial)));
        SpellGCDMultiplier = (1 / (1 - (combatTable.spellResist)));

        MHFrequency = (int) (combatTable.MH.hastedSpeed * 1000);
    }

    public Rotation GetDamage(int fightDuration) {
        boolean SS = false;
        boolean BS = false;
        boolean IT = false;
        boolean PS = false;
        boolean pest = false;
        int BPGoSS = 0;
        int FFGoSS = 0;
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
                            BloodRune1 = RuneCD + BloodRune1;
                            if (talents.getReaping() == 3) {
                                DeathRune1 = BloodRune1;
                                BloodRune1 = fightDuration;
                            }
                            pest = true;
                        } else if (BloodRune2 < 0) {
                            BloodRune2 += RuneCD;
                            if (talents.getReaping() == 3) {
                                DeathRune2 = BloodRune2;
                                BloodRune2 = fightDuration;
                            }
                            pest = true;
                        } else if (DeathRune1 < 0) {
                            BloodRune1 = RuneCD + DeathRune1;
                            DeathRune1 = fightDuration + 1;
                            if (talents.getReaping() == 3) {
                                DeathRune1 = BloodRune1;
                                BloodRune1 = fightDuration;
                            }
                            pest = true;
                        } else if (DeathRune2 < 0) {
                            BloodRune2 = DeathRune2 + RuneCD;
                            DeathRune2 = fightDuration + 1;
                            if (talents.getReaping() == 3) {
                                DeathRune2 = BloodRune2;
                                BloodRune2 = fightDuration;
                            }
                            pest = true;
                        }
                    }
                }

                if ((FF <= 0 && (FrostRune1 < 0 || FrostRune2 < 0 || DeathRune1 < 0 || DeathRune2 < 0))
                        || (FF < FrostRune1 && FF < FrostRune2 && FF < DeathRune1 && FF > DeathRune2)
                        || (FF < FrostRune1 && FF < FrostRune2 && FF > DeathRune1 && FF < DeathRune2)
                        || (FF < FrostRune1 && FF > FrostRune2 && FF < DeathRune1 && FF < DeathRune2)
                        || (FF > FrostRune1 && FF < FrostRune2 && FF < DeathRune1 && FF < DeathRune2)) {
                    // FF
                    if (FrostRune1 < 0) {
                        // IT
                        IT = true;
                        FrostRune1 += RuneCD;
                    } else if (FrostRune2 < 0) {
                        // IT
                        IT = true;
                        FrostRune2 += RuneCD;
                    } else if (DeathRune1 < 0) {
                        // IT
                        IT = true;
                        BloodRune1 = DeathRune1 + RuneCD;
                        DeathRune1 = fightDuration;
                        IT = true;
                    } else if (DeathRune2 < 0) {
                        // IT
                        IT = true;
                        BloodRune2 = DeathRune2 + RuneCD;
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
                        UnholyRune1 += RuneCD;
                    } else if (UnholyRune2 < 0) {
                        // PS
                        PS = true;
                        UnholyRune2 += RuneCD;
                    } else if (DeathRune1 < 0) {
                        // PS
                        PS = true;
                        BloodRune1 = DeathRune1 + RuneCD;
                        DeathRune1 = fightDuration;
                    } else if (DeathRune2 < 0) {
                        // PS
                        PS = true;
                        BloodRune2 = DeathRune2 + RuneCD;
                        DeathRune2 = fightDuration;
                    }
                }
                // Scourge Strike
                else if (FrostRune1 < 0 && UnholyRune1 < 0) {
                    FrostRune1 += RuneCD;
                    UnholyRune1 += RuneCD;
                    SS = true;
                } else if (FrostRune2 < 0 && UnholyRune2 < 0) {
                    FrostRune2 += RuneCD;
                    UnholyRune2 += RuneCD;
                    SS = true;
                } else if (FrostRune1 < 0 && UnholyRune2 < 0) {
                    FrostRune1 += RuneCD;
                    UnholyRune2 += RuneCD;
                    SS = true;
                } else if (FrostRune1 < 0 && DeathRune1 < 0) {
                    FrostRune1 += RuneCD;
                    BloodRune1 = DeathRune1 + RuneCD;
                    DeathRune1 = fightDuration + 1;
                    SS = true;
                } else if (FrostRune1 < 0 && DeathRune2 < 0) {
                    FrostRune1 += RuneCD;
                    BloodRune2 = DeathRune2 + RuneCD;
                    DeathRune2 = fightDuration + 1;
                    SS = true;
                } else if (FrostRune2 < 0 && UnholyRune1 < 0) {
                    FrostRune2 += RuneCD;
                    UnholyRune1 += RuneCD;
                    SS = true;
                } else if (FrostRune2 < 0 && DeathRune1 < 0) {
                    FrostRune2 += RuneCD;
                    BloodRune1 = DeathRune1 + RuneCD;
                    DeathRune1 = fightDuration + 1;
                    SS = true;
                } else if (FrostRune2 < 0 && DeathRune2 < 0) {
                    FrostRune2 = RuneCD;
                    BloodRune2 = DeathRune2 + RuneCD;
                    DeathRune2 = fightDuration + 1;
                    SS = true;
                } else if (DeathRune1 < 0 && UnholyRune1 < 0) {
                    BloodRune1 = RuneCD + DeathRune1;
                    DeathRune1 = fightDuration + 1;
                    UnholyRune1 += RuneCD;
                    SS = true;
                } else if (DeathRune1 < 0 && UnholyRune2 < 0) {
                    BloodRune1 = DeathRune1 + RuneCD;
                    DeathRune1 = fightDuration + 1;
                    UnholyRune2 += RuneCD;
                    SS = true;
                } else if (DeathRune1 < 0 && DeathRune2 < 0) {
                    BloodRune1 = DeathRune1 + RuneCD;
                    DeathRune1 = fightDuration + 1;
                    BloodRune2 = DeathRune2 + RuneCD;
                    DeathRune2 = fightDuration + 1;
                    SS = true;
                }
                // Blood Strike
                else if (BloodRune1 < 0) {
                    if (talents.getBloodOfTheNorth() == 5 || talents.getReaping() == 3) {
                        DeathRune1 = BloodRune1 + RuneCD;
                        BloodRune1 = fightDuration + 1;
                    } else {
                        BloodRune1 += RuneCD;
                    }
                    BS = true;
                } else if (BloodRune2 < 0) {
                    if (talents.getBloodOfTheNorth() == 5 || talents.getReaping() == 3) {
                        DeathRune2 = BloodRune2 + RuneCD;
                        BloodRune2 = fightDuration + 1;
                    } else {
                        BloodRune2 += RuneCD;
                    }
                    BS = true;
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
            if (SS) {
                //SS
                GCDTime = (int) (meleeGCD * PhysicalGCDMultiplier);
                RP += 15 + talents.getDirge() * 2.5;
                occurence.setScourgeStrike(occurence.getScourgeStrike() + 1);
                SS = false;
                if (talents.isGlyphofScourgeStrike()) {
                    if (BPGoSS < 3 && BP > 0) {
                        BP += 3000;
                        BPGoSS++;
                    }
                    if (FFGoSS < 3 && FF > 0) {
                        FF += 3000;
                        FFGoSS++;
                    }
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
                RP += 10;
                occurence.setPestilence(occurence.getPestilence() + 1);
                pest = false;
            }
            if (IT) {
                FF = 15000 + talents.getEpidemic() * 3000 - 1;
                FFGoSS = 0;
                IT = false;
                RP += 10 + talents.getChillOfTheGrave() * 2.5;
                GCDTime = (int) (SpellGCDMultiplier * spellGCD);
                occurence.setIcyTouch(occurence.getIcyTouch() + 1);
            }
            if (PS) {
                BP = 15000 + talents.getEpidemic() * 3000 - 1;
                BPGoSS = 0;
                GCDTime = (int) (meleeGCD * PhysicalGCDMultiplier);
                occurence.setPlagueStrike(occurence.getPlagueStrike() + 1);
                RP += 10 + talents.getDirge() * 2.5;
                PS = false;
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
