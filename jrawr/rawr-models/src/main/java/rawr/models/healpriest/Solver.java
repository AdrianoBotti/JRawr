// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.HealPriest/Solver.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.healpriest;

import rawr.base.character.Character;
import rawr.base.character.CharacterRace;
import rawr.base.util.NetFormat;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

import java.util.ArrayList;
import java.util.List;

class BaseSolver {
    public Character character;
    public Stats stats;
    public CalculationOptionsHealPriest calculationOptions;

    private String role;

    public String getRole() {
        return role;
    }

    protected void setRole(String value) {
        role = value;
    }

    private String actionList;

    public String getActionList() {
        return actionList;
    }

    protected void setActionList(String value) {
        actionList = value;
    }

    public List<ManaSource> ManaSources = new ArrayList<ManaSource>();

    public static class ManaSource {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String value) {
            name = value;
        }

        private float value;

        public float getValue() {
            return value;
        }

        public void setValue(float value) {
            this.value = value;
        }

        public ManaSource(String name, float value) {
            this.name = name;
            this.value = value;
        }
    }

    public float procInterval(float procChance, float procDelay, float procCooldown) {
        float ProcActual = 1f - (float) Math.pow(1f - procChance, 1f / procChance);
        float EffCooldown = procCooldown + procDelay * 0.5f + (float) Math.log(procChance) / (float) Math.log(ProcActual) * procDelay / ProcActual;
        return EffCooldown;
    }

    public BaseSolver(Stats stats, Character character) {
        this.stats = stats;
        this.character = character;
        calculationOptions = character.getCalculationOptions() instanceof CalculationOptionsHealPriest
                ? (CalculationOptionsHealPriest) character.getCalculationOptions()
                : new CalculationOptionsHealPriest();

        role = "";
        actionList = "Cast List:";
    }

    public void calculate(CharacterCalculationsHealPriest calculatedStats) {
        actionList += "- Virtual.";
        calculatedStats.setHPSBurstPoints(0);
        calculatedStats.setHPSSustainPoints(0);
        calculatedStats.setSurvivabilityPoints(0);
    }
}

class Solver extends BaseSolver {
    private eRole role;

    public Solver(Stats stats, Character character) {
        super(stats, character);
        role = calculationOptions.getRole();

        if (role == eRole.AUTO_Tank) { // OOOH MAGIC TANK ROTATION!!!
            setRole("Auto ");
            if (character.getPriestTalents().getPenance() > 0) {
                if (character.getPriestTalents().getDivineFury() < 5)
                    role = eRole.Disc_Tank_FH; // Disc-MT, Using Flash Heal instead of GH
                else
                    role = eRole.Disc_Tank_GH; // Disc-MT
            } else
                role = eRole.Holy_Tank; // Holy-MT
        } else if (role == eRole.AUTO_Raid) { // Raid rotation
            setRole("Auto ");
            if (character.getPriestTalents().getPenance() > 0)
                role = eRole.Disc_Raid; // Disc-Raid (PW:S/Penance/Flash)
            else if (character.getPriestTalents().getCircleOfHealing() > 0)
                role = eRole.Holy_Raid; // Holy-Raid (CoH/FH)
            else
                role = eRole.Flash_Heal; // Fallback to Flash Heal raid.
        }
    }

    @Override
    public void calculate(CharacterCalculationsHealPriest calculatedStats) {
        Stats simstats = calculatedStats.getBasicStats().copy();

        float valanyrProc = 0f;

        // Pre calc Procs (Power boosting Procs)
        Stats UseProcs = new Stats();
        if (calculationOptions.getModelProcs()) {
            for (SpecialEffect se : simstats.specialEffects()) {
                if (se.getStats().getManaRestore() == 0 && se.getStats().getMp5() == 0) {   // We handle mana restoration stats later.
                    if (se.getTrigger() == Trigger.Use) {
                        float Factor = se.getAverageFactor(3f, 1f);
                        UseProcs = Stats.add(UseProcs, Stats.multiply(se.getStats(), Factor));
                        for (SpecialEffect s : se.getStats().specialEffects())
                            UseProcs = Stats.add(UseProcs, Stats.multiply(Stats.multiply(s.getStats(), Factor), s.getMaxStack()));
                    } else if (se.getTrigger() == Trigger.SpellCast
                            || se.getTrigger() == Trigger.HealingSpellCast
                            || se.getTrigger() == Trigger.HealingSpellHit) {
                        if (se.getStats().getShieldFromHealed() > 0) {
                            valanyrProc = se.getAverageUptime(2f, 1f) * se.getStats().getShieldFromHealed();
                        }
                        if (se.getStats().getHighestStat() > 0) {
                            float greatnessProc = se.getAverageStats(2f, 1f).getHighestStat();
                            if (simstats.getSpirit() > simstats.getIntellect())
                                UseProcs.setSpirit(UseProcs.getSpirit() + greatnessProc);
                            else
                                UseProcs.setIntellect(UseProcs.getIntellect() + greatnessProc);
                        } else
                            UseProcs = Stats.add(UseProcs, se.getAverageStats(2f, 1f));
                    }
                }
            }

            // Juggle out the original spell haste and put in new.
            if (UseProcs.getHasteRating() > 0) {
                simstats.setSpellHaste((1 + simstats.getSpellHaste())
                        / (1 + StatConversion.getSpellHasteFromRating(simstats.getHasteRating()))
                        * (1 + StatConversion.getSpellHasteFromRating(UseProcs.getHasteRating() + simstats.getHasteRating()))
                        - 1);
            }
            UseProcs.setSpirit((float) Math.rint(UseProcs.getSpirit() * (1 + simstats.getBonusSpiritMultiplier())));
            UseProcs.setIntellect((float) Math.rint(UseProcs.getIntellect() * (1 + simstats.getBonusIntellectMultiplier())));
            UseProcs.setSpellPower(UseProcs.getSpellPower() + (float) Math.rint(UseProcs.getSpirit() * simstats.getSpellDamageFromSpiritPercentage()));
            simstats = Stats.add(simstats, UseProcs);
        }

        float solchance = (character.getPriestTalents().getHolySpecialization() * 0.01f + simstats.getSpellCrit()) * character.getPriestTalents().getSurgeOfLight() * 0.25f;
        float sol5chance = 1f - (float) Math.pow(1f - solchance, 5);
        float healmultiplier = (1 + character.getPriestTalents().getTestOfFaith() * 0.04f * calculationOptions.getTestOfFaith() / 100f) * (1 + simstats.getHealingReceivedMultiplier()) * (1 + simstats.getBonusHealingDoneMultiplier());

        // Add on Renewed Hope crit & Grace for Disc Maintank Rotation.
        if (role == eRole.Disc_Tank_FH
                || role == eRole.Disc_Tank_GH) {
            simstats.setSpellCrit(simstats.getSpellCrit() + character.getPriestTalents().getRenewedHope() * 0.02f);
            healmultiplier *= (1 + character.getPriestTalents().getGrace() * 0.045f);
        }

        Heal gh = new Heal(simstats, character);
        FlashHeal fh = new FlashHeal(simstats, character);
        CircleOfHealing coh = new CircleOfHealing(simstats, character);
        Penance penance = new Penance(simstats, character);
        PowerWordShield pws = new PowerWordShield(simstats, character);
        PrayerOfMending prom_1 = new PrayerOfMending(simstats, character, 1);
        PrayerOfMending prom_4 = new PrayerOfMending(simstats, character, 4);
        Renew renew = new Renew(simstats, character);
        PrayerOfHealing proh_max = new PrayerOfHealing(simstats, character);

        // Surge of Light Flash Heal (cannot crit, is free)
        FlashHeal fh_sol = new FlashHeal(simstats, character);
        fh_sol.SurgeOfLight();

        // Serendipity haste
        PrayerOfHealing proh_serendipity_2 = new PrayerOfHealing(simstats, character, 5, character.getPriestTalents().getSerendipity() * 0.04f * 2);

        // Borrowed Time Haste
        float oldSpellHaste = simstats.getSpellHaste(), oldSpellPower = simstats.getSpellPower();
        simstats.setSpellHaste((1 + simstats.getSpellHaste()) * (1 + character.getPriestTalents().getBorrowedTime() * 0.05f) - 1);
        if (simstats.getPWSBonusSpellPowerProc() > 0)
            simstats.setSpellPower(simstats.getSpellPower() + simstats.getPWSBonusSpellPowerProc());
        Heal gh_bt = new Heal(simstats, character);
        FlashHeal fh_bt = new FlashHeal(simstats, character);
        Penance penance_bt = new Penance(simstats, character);
        PowerWordShield pws_bt = new PowerWordShield(simstats, character);
        PrayerOfMending prom_1_bt = new PrayerOfMending(simstats, character, 1);
        PrayerOfMending prom_4_bt = new PrayerOfMending(simstats, character, 4);
        PrayerOfHealing proh_max_bt = new PrayerOfHealing(simstats, character);
        simstats.setSpellHaste(oldSpellHaste);
        simstats.setSpellPower(oldSpellPower);

        List<Spell> sr = new ArrayList<Spell>();
        switch (role) {
            case Greater_Heal:     // Greater Heal Spam
                setRole(getRole() + "Greater Heal");
                sr.add(gh);
                break;
            case Flash_Heal:     // Flash Heal Spam
                setRole(getRole() + "Flash Heal");
                sr.add(fh);
                break;
            case CoH_PoH:     // Circle of Healing/Prayer of Healing spam
                if (character.getPriestTalents().getCircleOfHealing() > 0) {
                    setRole(getRole() + "CoH + PoH");
                    sr.add(coh);
                    sr.add(proh_max);
                    sr.add(proh_max);
                } else if (character.getPriestTalents().getBorrowedTime() > 0) {
                    setRole(getRole() + "PWS + PoH");
                    sr.add(pws);
                    sr.add(proh_max_bt);
                } else {
                    setRole(getRole() + "PoH");
                    sr.add(proh_max);
                }
                break;
            case Holy_Tank:     // Holy MT Healing, renew + prom + ghx5 repeat
                setRole(getRole() + "Holy Tank");
                sr.add(renew);      // 1.5s 1.5  -13.5 -??.?
                sr.add(prom_1);     // 1.5s 3.0  -12.0 -8.5
                sr.add(gh);         // 2.5s 5.5  -9.5  -6
                sr.add(gh);         // 2.5s 8.0  -7.0  -3.5
                sr.add(gh);         // 2.5s 10.5 -4.5  -1.0
                sr.add(gh);         // 2.5s 13.0 -2    -??
                sr.add(gh);         // 2.5s 15.5 -??   -??   Although, adjusted for haste and improved holy conc, this gets better and better.
                break;
            case Holy_Raid:     // Holy Raid Healing, prom, coh, fhx2, proh, coh, fhx2, proh (2 haste stacks if serendipity)
                setRole(getRole() + "Holy Raid");
                sr.add(prom_4);   // 1.5s 1.5 -8.5
                sr.add(coh);        // 1.5s 3.0 -7.0
                sr.add(fh);        // 1.5s 4.5 -5.5
                sr.add(fh);         // 1.5s 6.0 -4.0
                sr.add(proh_serendipity_2);
                sr.add(coh);
                sr.add(fh);
                sr.add(fh);
                sr.add(proh_serendipity_2);
                // Repeat
                break;
            case Disc_Tank_GH:     // Disc MT Healing, pws, penance, prom, gh, penance
                setRole(getRole() + "Disc Tank w/Gheal");
                sr.add(pws);
                sr.add(penance_bt);
                sr.add(prom_1_bt);
                sr.add(gh_bt);
                sr.add(gh);
                sr.add(gh);
                sr.add(gh);
                // repeat
                break;
            case Disc_Tank_FH:     // Disc MT Healing, pws, penance, prom, fh - Does not have Divine Fury.
                setRole(getRole() + "Disc Tank w/Fheal");
                sr.add(pws);
                sr.add(penance_bt);
                sr.add(prom_1_bt);
                sr.add(fh_bt);
                sr.add(fh);
                sr.add(fh);
                sr.add(fh);
                sr.add(fh);
                sr.add(fh);
                // repeat
                break;
            case Disc_Raid:     // Disc Raid Healing, pws, penance, prom, pw:s, fh, fh
                setRole(getRole() + "Disc Raid");
                sr.add(pws);        // 1.5  1.5  -2.5  -??   -??
                sr.add(penance_bt); // 1.5  3.0  -1.0  -8.0  -??
                sr.add(prom_4_bt);  // 1.5  4.5  -??   -6.5  -8.5
                sr.add(pws_bt);     // 1.5  6.0  -2.5  -5.0  -7.0
                sr.add(fh_bt);      // 1.5  7.5  -1.0  -3.5  -5.5
                // repeat
                break;
            case Holy_Raid_Renew:    // Holy Raid healing with Renew. Almost no FH.
                setRole(getRole() + "Holy Raid Renew");
                sr.add(prom_4);
                sr.add(coh);
                sr.add(renew);
                sr.add(renew);
                sr.add(renew);
                sr.add(renew);
                sr.add(renew);
                sr.add(prom_4);
                sr.add(coh);
                sr.add(renew);
                sr.add(renew);
                sr.add(renew);
                sr.add(renew);
                sr.add(fh);
                break;
            default:
                break;
        }

        for (Spell s : sr)
            setActionList(getActionList() + "\r\n- " + s.getName());

        float manacost = 0, cyclelen = 0, healamount = 0, solctr = 0, castctr = 0, castlandctr = 0, crittable = 0, holyconccast = 0, holyconccrit = 0, pwscasts = 0;
        float divineaegis = character.getPriestTalents().getDivineAegis() * 0.1f * (1f + stats.getPriestHeal_T9_4pc());
        for (int x = 0; x < sr.size(); x++) {
            float mcost = 0, absorb = 0, heal = 0, rheal = 0, clen = 0;
            if (sr.get(x) == gh || sr.get(x) == gh_bt) {   // Greater Heal (A Borrowed Time GHeal cannot also be improved Holy conc hasted, so this works)
                clen = sr.get(x).getCastTime();
                rheal = sr.get(x).getAvgTotHeal() * healmultiplier;
                absorb = sr.get(x).getAvgCrit() * healmultiplier * sr.get(x).getCritChance() * divineaegis;
                solctr = 1f - (1f - solctr) * (1f - solchance);
                mcost = sr.get(x).getManaCost();
                mcost -= simstats.getManaGainOnGreaterHealOverheal() * calculationOptions.getSerendipity() / 100f;
                castctr++;
                castlandctr++;
                crittable += sr.get(x).getCritChance();
                holyconccast++;
                holyconccrit += sr.get(x).getCritChance();
            } else if (sr.get(x) == fh || sr.get(x) == fh_bt) {   // Flash Heal (Same applies to FH as GHeal with regards to borrowed time)
                clen = sr.get(x).getCastTime();
                rheal = sr.get(x).getAvgTotHeal() * healmultiplier;
                if (simstats.getPriestHeal_T10_2pc() > 0)
                    heal = rheal * 1 / 9;   // 33% chance of getting 33% of healed amount over 6s. 1/9 (11.11...%)
                absorb = sr.get(x).getAvgCrit() * healmultiplier * sr.get(x).getCritChance() * divineaegis;
                solctr = 1f - (1f - solctr) * (1f - solchance);
                mcost = sr.get(x).getManaCost();
                mcost -= mcost * solctr;
                solctr = 0;
                castctr++;
                castlandctr++;
                crittable += sr.get(x).getCritChance();
                holyconccast++;
                holyconccrit += sr.get(x).getCritChance();
            } else if (sr.get(x) == penance || sr.get(x) == penance_bt) {
                clen = sr.get(x).getCastTime();
                rheal = sr.get(x).getAvgTotHeal() * healmultiplier;
                absorb = sr.get(x).getAvgCrit() * healmultiplier * sr.get(x).getCritChance() * divineaegis;
                mcost = sr.get(x).getManaCost();
                castctr++;
                castlandctr += 3f; // Penance counts as 3 casts for some purposes.
                crittable += 1f - (float) Math.pow(1f - sr.get(x).getCritChance(), 3f);
            } else if (sr.get(x) == coh) {   // Circle of Healing
                clen = coh.getGlobalCooldown();
                heal = coh.getAvgTotHeal() * healmultiplier;
                solctr = 1f - (1f - solctr) * (1f - sol5chance);
                mcost = coh.getManaCost();
                castctr++;
                castlandctr += sr.get(x).getTargets();
                crittable += 1f - (float) Math.pow(1f - sr.get(x).getCritChance(), sr.get(x).getTargets());
            } else if (sr.get(x) == proh_max || sr.get(x) == proh_serendipity_2 || sr.get(x) == proh_max_bt) {
                clen = sr.get(x).getCastTime();
                heal = sr.get(x).getAvgTotHeal() * healmultiplier;
                solctr = 1f - (1f - solctr) * (1f - sol5chance);
                absorb = sr.get(x).getAvgCrit() * sr.get(x).getTargets() * healmultiplier * sr.get(x).getCritChance() * divineaegis;
                mcost = sr.get(x).getManaCost();
                castctr++;
                castlandctr += sr.get(x).getTargets();
                crittable += 1f - (float) Math.pow(1f - sr.get(x).getCritChance(), sr.get(x).getTargets());
            } else if (sr.get(x) == renew) {   // Renew
                clen = renew.getGlobalCooldown();
                heal = renew.getAvgTotHeal() * healmultiplier;
                mcost = renew.getManaCost();
                castctr++;
                castlandctr++;
                if (character.getPriestTalents().getImprovedRenew() > 0) {
                    holyconccast++;
                    holyconccrit += renew.getCritChance();
                }
            } else if (sr.get(x) == pws || sr.get(x) == pws_bt) {
                clen = sr.get(x).getGlobalCooldown();
                absorb = sr.get(x).getAvgTotHeal();
                mcost = sr.get(x).getManaCost();
                if (character.getPriestTalents().isGlyphofPowerWordShield()) {
                    heal = absorb * 0.2f * healmultiplier * (1 + 0.5f * (stats.getSpellCrit() + character.getPriestTalents().getHolySpecialization()));   // Not entirely right, but close enough.
                    // Divine Aegis isn't yet entirely fixed for PWS
                    // absorb += heal * sr[x].CritChance * divineaegis;
                }
                castctr++;
                castlandctr++;
                pwscasts++;
            } else if (sr.get(x) == prom_1 || sr.get(x) == prom_4 || sr.get(x) == prom_1_bt || sr.get(x) == prom_4_bt) {
                clen = sr.get(x).getGlobalCooldown();
                heal = sr.get(x).getAvgTotHeal() * healmultiplier;
                absorb = sr.get(x).getAvgCrit() * sr.get(x).getTargets() * healmultiplier * sr.get(x).getCritChance() * divineaegis;
                mcost = sr.get(x).getManaCost();
                castctr++;
                castlandctr += sr.get(x).getTargets();
                crittable += 1f - (float) Math.pow(1f - sr.get(x).getCritChance(), sr.get(x).getTargets());
            }
            absorb += valanyrProc * (heal + rheal);
            cyclelen += clen;
            healamount += heal + rheal + absorb;
            manacost += mcost;
        }

        // Real Cyclelen also has time for FSR. To get 80% FSR, a cycle of 20 seconds needs to include:
        // (20 + 5) / 0.8 = 31.25 seconds. (31.25 - 5 - 20 = 6.25 / 31.25 = 0.2 seconds of FSR regen).
        float realcyclelen = cyclelen * ((25f + 5f) / (calculationOptions.getFSRRatio() / 100f)) / 25f;
        float avgcastlen = realcyclelen / castctr;
        float avgcastlandlen = realcyclelen / castlandctr;
        float avgcritcast = crittable / sr.size();

        float periodicRegenOutFSR = StatConversion.getSpiritRegenSec(simstats.getSpirit(), simstats.getIntellect());
        // Add up all mana gains.
        float regen = 0, tmpregen = 0;

        // Spirit/Intellect based Regeneration and MP5
        tmpregen = periodicRegenOutFSR * (1f - calculationOptions.getFSRRatio() / 100f);
        if (tmpregen > 0f) {
            ManaSources.add(new ManaSource("OutFSR", tmpregen));
            regen += tmpregen;
        }
        tmpregen = periodicRegenOutFSR * simstats.getSpellCombatManaRegeneration() * calculationOptions.getFSRRatio() / 100f;
        if (tmpregen > 0f) {
            ManaSources.add(new ManaSource("Meditation", tmpregen));
            regen += tmpregen;
        }
        if (character.getPriestTalents().getHolyConcentration() > 0) {
            float hceffect = character.getPriestTalents().getHolyConcentration() * 0.5f / 3f;
            float hccastinterval = cyclelen / holyconccast;
            float hccritchance = holyconccrit / holyconccast;
            // Calculate chance that you crit within 8 seconds.
            float hcuptime = 1f - (float) Math.pow(1f - hccritchance, 8f / hccastinterval);
            tmpregen = (periodicRegenOutFSR * (1f - calculationOptions.getFSRRatio() / 100f)
                    + periodicRegenOutFSR * simstats.getSpellCombatManaRegeneration() * calculationOptions.getFSRRatio() / 100f)
                    * hcuptime * hceffect;
            if (tmpregen > 0) {
                ManaSources.add(new ManaSource("Holy Concentration", tmpregen));
                regen += tmpregen;
            }
        }
        if (character.getPriestTalents().getRapture() > 0 && pwscasts > 0) {   // New Rapture restores 1.5% - 2% - 2.5% of max mana every 12 seconds at best.
            float rapturereturn = 0.015f + (character.getPriestTalents().getRapture() - 1) * 0.005f;
            float maxrapture = simstats.getMana() * rapturereturn / 12f;
            tmpregen = maxrapture * calculationOptions.getRapture() / 100f;
            if (tmpregen > 0) {
                ManaSources.add(new ManaSource("Rapture", tmpregen));
                regen += tmpregen;
            }
        }
        tmpregen = simstats.getMp5() / 5;
        ManaSources.add(new ManaSource("MP5", tmpregen));
        regen += tmpregen;
        tmpregen = simstats.getMana() / (calculationOptions.getFightLengthSeconds());
        ManaSources.add(new ManaSource("Intellect", tmpregen));
        regen += tmpregen;
        if (calculationOptions.getModelProcs()) {
            float heal = 0f;
            for (SpecialEffect se : simstats.specialEffects()) {
                tmpregen = 0f;
                if (se.getStats().getManaRestore() > 0 || se.getStats().getMp5() > 0) {
                    if (se.getTrigger() == Trigger.SpellCast
                            || se.getTrigger() == Trigger.HealingSpellCast) {
                        tmpregen = se.getAverageStats(avgcastlen, 1f, 0f, calculationOptions.getFightLengthSeconds()).getManaRestore()
                                + se.getAverageStats(avgcastlen, 1f, 0f, calculationOptions.getFightLengthSeconds()).getMp5() / 5;
                    } else if (se.getTrigger() == Trigger.HealingSpellHit) {
                        tmpregen = se.getAverageStats(avgcastlandlen, 1f, 0f, calculationOptions.getFightLengthSeconds()).getManaRestore()
                                + se.getAverageStats(avgcastlandlen, 1f, 0f, calculationOptions.getFightLengthSeconds()).getMp5() / 5;
                    } else if (se.getTrigger() == Trigger.SpellCrit
                            || se.getTrigger() == Trigger.HealingSpellCrit) {
                        tmpregen = se.getAverageStats(avgcastlen, avgcritcast / avgcastlen, 0f, calculationOptions.getFightLengthSeconds()).getManaRestore()
                                + se.getAverageStats(avgcastlen, avgcritcast / avgcastlen, 0f, calculationOptions.getFightLengthSeconds()).getMp5() / 5;
                    } else if (se.getTrigger() == Trigger.Use) {
                        tmpregen = se.getAverageStats().getManaRestore()
                                + se.getAverageStats().getMp5() / 5;
                    }
                }
                if (tmpregen > 0) {
                    ManaSources.add(new ManaSource(se.toString(), tmpregen));
                    regen += tmpregen;
                }
                if (se.getStats().getHealed() > 0f) {
                    if (se.getTrigger() == Trigger.HealingSpellCast
                            || se.getTrigger() == Trigger.SpellCast)
                        heal += se.getAverageStats(avgcastlen, 1f, 0f, calculationOptions.getFightLengthSeconds()).getHealed();
                    else if (se.getTrigger() == Trigger.HealingSpellHit)
                        heal += se.getAverageStats(avgcastlandlen, 1f, 0f, calculationOptions.getFightLengthSeconds()).getHealed();
                    else if (se.getTrigger() == Trigger.SpellCrit
                            || se.getTrigger() == Trigger.HealingSpellCrit)
                        heal += se.getAverageStats(avgcastlen, avgcritcast / avgcastlen, 0f, calculationOptions.getFightLengthSeconds()).getHealed();
                    else if (se.getTrigger() == Trigger.Use)
                        heal += se.getAverageStats().getHealed();
                }
            }
            healamount += heal * (1f + stats.getSpellCrit() * 0.5f) * healmultiplier * (1f + valanyrProc);
        }

        // External and Other mana sources.
        tmpregen = simstats.getMana() * simstats.getManaRestoreFromMaxManaPerSecond() * calculationOptions.getReplenishment() / 100f;
        if (tmpregen > 0f) {
            ManaSources.add(new ManaSource("Replenishment", tmpregen));
            regen += tmpregen;
        }

        setActionList(getActionList() + "\r\n\r\nMana Options:");

        float mp1use = manacost / realcyclelen;

        if (mp1use > regen && character.getRace() == CharacterRace.BloodElf) {   // Arcane Torrent is 6% max mana every 2 minutes.
            tmpregen = simstats.getMana() * 0.06f / 120f;
            ManaSources.add(new ManaSource("Arcane Torrent", tmpregen));
            regen += tmpregen;
            setActionList(getActionList() + "\r\n- Used Arcane Torrent");
        }

        if (mp1use > regen && calculationOptions.getManaAmt() > 0f) {
            float ManaPot = calculationOptions.getManaAmt() * (1f + simstats.getBonusManaPotion());
            tmpregen = ManaPot / (calculationOptions.getFightLengthSeconds());
            ManaSources.add(new ManaSource("Mana Potion", tmpregen));
            setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- Used Mana Potion (%s)", NetFormat.fixed(ManaPot, 0)));
            regen += tmpregen;
        }
        if (mp1use > regen) {
            tmpregen = (simstats.getMana() * 0.4f * calculationOptions.getShadowfiend() / 100f)
                    / ((5f - character.getPriestTalents().getVeiledShadows() * 1f) * 60f);
            ManaSources.add(new ManaSource("Shadowfiend", tmpregen));
            setActionList(getActionList() + "\r\n- Used Shadowfiend");
            regen += tmpregen;
        }
        if (mp1use > regen) {   // Hymn of Hope increases max mana by 20% and restores 3% of total mana for 4 ticks, 5 if glyphed.
            float ticks = character.getPriestTalents().isGlyphofHymnofHope() ? 5 : 4;
            tmpregen = (simstats.getMana() * 1.2f * 0.03f * ticks)
                    / (6f * 60f);
            ManaSources.add(new ManaSource("Hymn of Hope", tmpregen));
            setActionList(getActionList() + "\r\n- Used Hymn of Hope");
            regen += tmpregen;
        }

        if (mp1use > regen) {
            setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %s mp5 deficit. Could use more mana!", NetFormat.fixed((mp1use - regen) * 5, 0)));
        }

        calculatedStats.setHPSBurstPoints(healamount / cyclelen);
        // Sustained is limited by how much mana you regenerate over the time it would take to cast the spells, divided by the cost.
        if (regen > mp1use) // Regenerating more mana than we can use. Dont make user believe this is an upgrade.
            calculatedStats.setHPSSustainPoints(calculatedStats.getHPSBurstPoints());
        else
            calculatedStats.setHPSSustainPoints(calculatedStats.getHPSBurstPoints() * regen / mp1use);

        // Lets just say that 15% of resilience scales all health by 150%.
        float Resilience = Math.min(15f, StatConversion.getCritReductionFromResilience(simstats.getResilience()) * 100f) / 15f;
        calculatedStats.setSurvivabilityPoints(calculatedStats.getBasicStats().getHealth() * (Resilience * 1.5f + 1f) * calculationOptions.getSurvivability() / 100f);
    }
}

class AdvancedSolver extends BaseSolver {
    public AdvancedSolver(Stats stats, Character character) {
        super(stats, character);
        setRole("Custom");
    }

    @Override
    public void calculate(CharacterCalculationsHealPriest calculatedStats) {
        Stats simstats = calculatedStats.getBasicStats().copy();
        float valanyrProc = 0f;

        Stats UseProcs = new Stats();
        if (calculationOptions.getModelProcs()) {
            for (SpecialEffect se : simstats.specialEffects()) {
                if (se.getStats().getManaRestore() == 0 && se.getStats().getMp5() == 0) {   // We handle mana restoration stats later.
                    if (se.getTrigger() == Trigger.Use)
                        UseProcs = Stats.add(UseProcs, se.getAverageStats(2f, 1f));
                    else if (se.getTrigger() == Trigger.SpellCast
                            || se.getTrigger() == Trigger.HealingSpellCast
                            || se.getTrigger() == Trigger.HealingSpellHit) {
                        if (se.getStats().getShieldFromHealed() > 0) {
                            valanyrProc = se.getAverageUptime(2f, 1f) * se.getStats().getShieldFromHealed();
                        }
                        if (se.getStats().getHighestStat() > 0) {
                            float greatnessProc = se.getAverageStats(2f, 1f).getHighestStat();
                            if (simstats.getSpirit() > simstats.getIntellect())
                                UseProcs.setSpirit(UseProcs.getSpirit() + greatnessProc);
                            else
                                UseProcs.setIntellect(UseProcs.getIntellect() + greatnessProc);
                        } else
                            UseProcs = Stats.add(UseProcs, se.getAverageStats(2f, 1f));
                    }
                }
            }

            // Juggle out the original spell haste and put in new.
            if (UseProcs.getHasteRating() > 0) {
                simstats.setSpellHaste((1 + simstats.getSpellHaste())
                        / (1 + StatConversion.getSpellHasteFromRating(simstats.getHasteRating()))
                        * (1 + StatConversion.getSpellHasteFromRating(UseProcs.getHasteRating() + simstats.getHasteRating()))
                        - 1);
            }
            UseProcs.setSpirit((float) Math.rint(UseProcs.getSpirit() * (1 + simstats.getBonusSpiritMultiplier())));
            UseProcs.setIntellect((float) Math.rint(UseProcs.getIntellect() * (1 + simstats.getBonusIntellectMultiplier())));
            UseProcs.setSpellPower(UseProcs.getSpellPower() + (float) Math.rint(UseProcs.getSpirit() * simstats.getSpellDamageFromSpiritPercentage()));
            simstats = Stats.add(simstats, UseProcs);
        }

        // Insightful Earthstorm Diamond.
        float healmultiplier = (1 + character.getPriestTalents().getTestOfFaith() * 0.04f * calculationOptions.getTestOfFaith() / 100f) * (1 + character.getPriestTalents().getGrace() * 0.045f) * (1 + simstats.getHealingReceivedMultiplier()) * (1 + simstats.getBonusHealingDoneMultiplier());
        float divineaegis = character.getPriestTalents().getDivineAegis() * 0.1f
                * (1f + stats.getPriestHeal_T9_4pc());

        float solchance = (character.getPriestTalents().getHolySpecialization() * 0.01f + simstats.getSpellCrit()) * character.getPriestTalents().getSurgeOfLight() * 0.25f;
        float solbhchance = 1f - (float) Math.pow(1f - solchance, 2);
        float solcohchance = 1f - (float) Math.pow(1f - solchance, character.getPriestTalents().isGlyphofCircleOfHealing() ? 6 : 5);
        float solpohhnchance = 1f - (float) Math.pow(1f - solchance, 5);
        float solpromchance = 1f - (float) Math.pow(1f - solchance, (calculationOptions.getProMCast() == 0) ? 0 : (calculationOptions.getProMTicks() / calculationOptions.getProMCast()));

        // Add on Renewed Hope crit for Disc Maintank Rotation, adjusted by uptime of Weakened Soul
        float WeakenedSoulUptime = Math.min(1f, calculationOptions.getPWSCast() * 15f / calculationOptions.getFightLengthSeconds());
        simstats.setSpellCrit(simstats.getSpellCrit() + character.getPriestTalents().getRenewedHope() * 0.02f * WeakenedSoulUptime);
        int instantCasts = calculationOptions.getCoHCast() + calculationOptions.getDispelCast() + calculationOptions.getHolyNovaCast()
                + calculationOptions.getPenanceCast() + calculationOptions.getProMCast() + calculationOptions.getPWSCast()
                + calculationOptions.getRenewCast();
        int nonInstantCasts = calculationOptions.getBindingHealCast() + calculationOptions.getDivineHymnCast()
                + calculationOptions.getFlashHealCast() + calculationOptions.getGreaterHealCast() + calculationOptions.getMDCast()
                + calculationOptions.getPoHCast();

        float BorrowedTimeHaste = character.getPriestTalents().getBorrowedTime() * 0.05f * (nonInstantCasts > 0 ? (float) Math.min((float) (calculationOptions.getPWSCast() / nonInstantCasts), 1f) : 1f);
        if (BorrowedTimeHaste > 0)
            simstats.setSpellHaste((1 + simstats.getSpellHaste()) * (1 + BorrowedTimeHaste) - 1);
        if (simstats.getPWSBonusSpellPowerProc() > 0 && calculationOptions.getPWSCast() > 0) {
            float timeBetweenShields = calculationOptions.getFightLengthSeconds() / calculationOptions.getPWSCast();
            float bonusSpellPower = simstats.getPWSBonusSpellPowerProc() * Math.min(1f, 5 / timeBetweenShields);
            simstats.setSpellPower(simstats.getSpellPower() + bonusSpellPower);
        }

        int TotalCasts = 0;
        float ManaUsed = 0f;
        float TimeUsed = 0f;
        float BaseTimeUsed = 0f;
        float DirectHeal = 0f;
        float OtherHeal = 0f;
        float AbsorbHeal = 0f;
        float CritCounter = 0f;
        float HCCritCounter = 0f;

        FlashHeal fh = new FlashHeal(simstats, character);
        BindingHeal bh = new BindingHeal(simstats, character);
        Heal gh = new Heal(simstats, character);
        Penance pen = new Penance(simstats, character);
        Renew renew = new Renew(simstats, character);
        PrayerOfMending prom = new PrayerOfMending(simstats, character, 1);
        PrayerOfMending prom_max = new PrayerOfMending(simstats, character);
        PrayerOfHealing proh = new PrayerOfHealing(simstats, character);
        PowerWordShield pws = new PowerWordShield(simstats, character);
        CircleOfHealing coh_max = new CircleOfHealing(simstats, character);
        HolyNova hn = new HolyNova(simstats, character, 1);
        DivineHymn dh = new DivineHymn(simstats, character);
        Dispel dispel = new Dispel(simstats, character);
        MassDispel md = new MassDispel(simstats, character);

        // Calculate how many of the flash heals are actually paid for.
        float FreeFlashes = calculationOptions.getFlashHealCast() * solchance
                + calculationOptions.getBindingHealCast() * solbhchance
                + calculationOptions.getGreaterHealCast() * solchance
                + calculationOptions.getCoHCast() * solcohchance
                + calculationOptions.getProMCast() * solpromchance
                + calculationOptions.getPoHCast() * solpohhnchance
                + calculationOptions.getHolyNovaCast() * solpohhnchance;
        FreeFlashes = Math.min(calculationOptions.getFlashHealCast(), FreeFlashes);

        // Flash Heal
        if (calculationOptions.getFlashHealCast() > 0) {
            TotalCasts += calculationOptions.getFlashHealCast();
            setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %d Flash Heal%s",
                    calculationOptions.getFlashHealCast(),
                    (FreeFlashes > 0f) ? String.format(java.util.Locale.ROOT, ", %s Surge of Lights", NetFormat.fixed(FreeFlashes, 0)) : ""));
            float Cost = fh.getManaCost() * (calculationOptions.getFlashHealCast() - FreeFlashes);
            ManaUsed += Cost;
            TimeUsed += fh.getCastTime() * calculationOptions.getFlashHealCast();
            BaseTimeUsed += fh.getBaseCastTime() * calculationOptions.getFlashHealCast();
            DirectHeal += fh.getAvgTotHeal() * healmultiplier * calculationOptions.getFlashHealCast();
            if (simstats.getPriestHeal_T10_2pc() > 0)
                OtherHeal += fh.getAvgTotHeal() * healmultiplier * calculationOptions.getFlashHealCast() * (1 / 9); // 33% chance to get 33% of healed over time. (~11.11...%)
            AbsorbHeal += fh.getAvgCrit() * fh.getCritChance() * healmultiplier * calculationOptions.getFlashHealCast() * divineaegis;
            CritCounter += fh.getCritChance() * calculationOptions.getFlashHealCast();
            HCCritCounter += fh.getCritChance() * calculationOptions.getFlashHealCast();
        }

        // Binding Heal
        if (calculationOptions.getBindingHealCast() > 0) {
            TotalCasts += calculationOptions.getBindingHealCast();
            setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %d Binding Heal", calculationOptions.getBindingHealCast()));
            ManaUsed += bh.getManaCost() * calculationOptions.getBindingHealCast();
            TimeUsed += bh.getCastTime() * calculationOptions.getBindingHealCast();
            BaseTimeUsed += bh.getBaseCastTime() * calculationOptions.getBindingHealCast();
            OtherHeal += bh.getAvgTotHeal() * healmultiplier * calculationOptions.getBindingHealCast();
            AbsorbHeal += bh.getAvgCrit() * 2 * bh.getCritChance() * healmultiplier * calculationOptions.getBindingHealCast() * divineaegis;
            CritCounter += bh.getCritChance() * 2 * calculationOptions.getBindingHealCast();
            HCCritCounter += bh.getCritChance() * 2 * calculationOptions.getBindingHealCast();
        }

        // Greater Heal
        if (calculationOptions.getGreaterHealCast() > 0) {
            TotalCasts += calculationOptions.getGreaterHealCast();
            setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %d Greater Heal", calculationOptions.getGreaterHealCast()));
            float Cost = gh.getManaCost() * calculationOptions.getGreaterHealCast();
            ManaUsed += Cost;
            TimeUsed += gh.getCastTime() * calculationOptions.getGreaterHealCast();
            BaseTimeUsed += gh.getBaseCastTime() * calculationOptions.getGreaterHealCast();
            DirectHeal += gh.getAvgTotHeal() * healmultiplier * calculationOptions.getGreaterHealCast();
            AbsorbHeal += gh.getAvgCrit() * gh.getCritChance() * healmultiplier * calculationOptions.getGreaterHealCast() * divineaegis;
            CritCounter += gh.getCritChance() * calculationOptions.getGreaterHealCast();
            HCCritCounter += gh.getCritChance() * calculationOptions.getGreaterHealCast();
        }

        // Penance
        if (calculationOptions.getPenanceCast() > 0 && character.getPriestTalents().getPenance() > 0) {
            TotalCasts += calculationOptions.getPenanceCast();
            setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %d Penance", calculationOptions.getPenanceCast()));
            ManaUsed += pen.getManaCost() * calculationOptions.getPenanceCast();
            TimeUsed += pen.getCastTime() * calculationOptions.getPenanceCast();
            BaseTimeUsed += pen.getBaseCastTime() * calculationOptions.getPenanceCast();
            DirectHeal += pen.getAvgTotHeal() * healmultiplier * calculationOptions.getPenanceCast();
            AbsorbHeal += pen.getAvgCrit() * pen.getCritChance() * healmultiplier * calculationOptions.getPenanceCast() * divineaegis;
            CritCounter += pen.getCritChance() * 3f * calculationOptions.getPenanceCast();
        }

        // Renew
        if (calculationOptions.getRenewCast() > 0) {
            TotalCasts += calculationOptions.getRenewCast();
            setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %d Renew, %d Ticks", calculationOptions.getRenewCast(), calculationOptions.getRenewTicks()));
            ManaUsed += renew.getManaCost() * calculationOptions.getRenewCast();
            TimeUsed += renew.getGlobalCooldown() * calculationOptions.getRenewCast();
            BaseTimeUsed += 1.5f * calculationOptions.getRenewCast();
            OtherHeal += renew.getAvgHeal() / (renew.getHotDuration() * 3) * healmultiplier * calculationOptions.getRenewTicks();
            if (character.getPriestTalents().getEmpoweredRenew() > 0) {
                DirectHeal += (renew.getAvgTotHeal() - renew.getAvgHeal()) * healmultiplier;
                CritCounter += renew.getCritChance() * calculationOptions.getRenewCast();
                HCCritCounter += renew.getCritChance() * calculationOptions.getRenewCast();
            }
        }

        // Prayer of Mending
        if (calculationOptions.getProMCast() > 0) {
            TotalCasts += calculationOptions.getProMCast();
            setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %d Prayer of Mending, %d Procs", calculationOptions.getProMCast(), calculationOptions.getProMTicks()));
            ManaUsed += prom.getManaCost() * calculationOptions.getProMCast();
            TimeUsed += prom.getGlobalCooldown() * calculationOptions.getProMCast();
            BaseTimeUsed += 1.5f * calculationOptions.getProMCast();
            DirectHeal += prom.getAvgTotHeal() * healmultiplier * calculationOptions.getProMTicks();
            AbsorbHeal += prom.getAvgCrit() * prom.getCritChance() * healmultiplier * calculationOptions.getProMTicks() * divineaegis;
            CritCounter += prom.getCritChance() * calculationOptions.getProMTicks();
        }

        // Prayer of Healing
        if (calculationOptions.getPoHCast() > 0) {
            TotalCasts += calculationOptions.getPoHCast();
            setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %d Prayer of Healing", calculationOptions.getPoHCast()));
            ManaUsed += proh.getManaCost() * calculationOptions.getPoHCast();
            TimeUsed += proh.getCastTime() * calculationOptions.getPoHCast();
            BaseTimeUsed += proh.getBaseCastTime() * calculationOptions.getPoHCast();
            DirectHeal += proh.getAvgTotHeal() * healmultiplier * calculationOptions.getPoHCast();
            AbsorbHeal += proh.getAvgCrit() * proh.getCritChance() * healmultiplier * calculationOptions.getPoHCast() * divineaegis;
            CritCounter += proh.getCritChance() * 5 * calculationOptions.getPoHCast();
        }

        // PW:Shield
        if (calculationOptions.getPWSCast() > 0) {
            TotalCasts += calculationOptions.getPWSCast();
            setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %d Power Word: Shield", calculationOptions.getPWSCast()));
            ManaUsed += pws.getManaCost() * calculationOptions.getPWSCast();
            TimeUsed += pws.getGlobalCooldown() * calculationOptions.getPWSCast();
            BaseTimeUsed += 1.5f * calculationOptions.getPWSCast();
            AbsorbHeal += pws.getAvgTotHeal() * calculationOptions.getPWSCast();
            if (character.getPriestTalents().isGlyphofPowerWordShield()) {
                float pwsglyphheal = pws.getAvgTotHeal() * 0.2f * healmultiplier * 0.2f;
                OtherHeal += pwsglyphheal * (1f - simstats.getSpellCrit()) + pwsglyphheal * 1.5f * simstats.getSpellCrit();
                CritCounter += simstats.getSpellCrit() * calculationOptions.getPWSCast();
            }
        }

        // Circle of Healing
        if (calculationOptions.getCoHCast() > 0) {
            TotalCasts += calculationOptions.getCoHCast();
            setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %d Circle of Healing", calculationOptions.getCoHCast()));
            ManaUsed += coh_max.getManaCost() * calculationOptions.getCoHCast();
            TimeUsed += coh_max.getGlobalCooldown() * calculationOptions.getCoHCast();
            BaseTimeUsed += 1.5f * calculationOptions.getCoHCast();
            OtherHeal += coh_max.getAvgTotHeal() * calculationOptions.getCoHCast();
            CritCounter += coh_max.getCritChance() * coh_max.getTargets() * calculationOptions.getCoHCast();
        }

        // Holy Nova
        if (calculationOptions.getHolyNovaCast() > 0) {
            TotalCasts += calculationOptions.getHolyNovaCast();
            setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %d Holy Nova", calculationOptions.getHolyNovaCast()));
            ManaUsed += hn.getManaCost() * calculationOptions.getHolyNovaCast();
            TimeUsed += hn.getGlobalCooldown() * calculationOptions.getHolyNovaCast();
            BaseTimeUsed += 1.5f * calculationOptions.getHolyNovaCast();
            DirectHeal += hn.getAvgTotHeal() * healmultiplier * calculationOptions.getHolyNovaCast();
            AbsorbHeal += hn.getAvgCrit() * hn.getCritChance() * healmultiplier * calculationOptions.getHolyNovaCast() * divineaegis;
            CritCounter += hn.getCritChance() * 5 * calculationOptions.getHolyNovaCast();
        }

        // Divine Hymn
        if (calculationOptions.getDivineHymnCast() > 0) {
            TotalCasts += calculationOptions.getDivineHymnCast();
            setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %d Divine Hymn", calculationOptions.getDivineHymnCast()));
            ManaUsed += dh.getManaCost() * calculationOptions.getDivineHymnCast();
            TimeUsed += dh.getCastTime() * calculationOptions.getDivineHymnCast();
            BaseTimeUsed += dh.getBaseCastTime() * calculationOptions.getDivineHymnCast();
            DirectHeal += dh.getAvgTotHeal() * healmultiplier * calculationOptions.getDivineHymnCast();
            AbsorbHeal += dh.getAvgCrit() * 3 * dh.getCritChance() * healmultiplier * calculationOptions.getDivineHymnCast() * divineaegis;
            CritCounter += dh.getCritChance() * 8 / 2 * 3 * calculationOptions.getDivineHymnCast(); // 12 total heals from Divine Hymn
        }

        // Dispel
        if (calculationOptions.getDispelCast() > 0) {
            TotalCasts += calculationOptions.getDispelCast();
            setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %d Dispel", calculationOptions.getDispelCast()));
            ManaUsed += dispel.getManaCost() * calculationOptions.getDispelCast();
            TimeUsed += dispel.getGlobalCooldown() * calculationOptions.getDispelCast();
            BaseTimeUsed += 1.5f * calculationOptions.getDispelCast();
        }

        // Mass Dispel
        if (calculationOptions.getMDCast() > 0) {
            TotalCasts += calculationOptions.getMDCast();
            setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %d Mass Dispel", calculationOptions.getMDCast()));
            ManaUsed += md.getManaCost() * calculationOptions.getMDCast();
            TimeUsed += md.getCastTime() * calculationOptions.getMDCast();
            BaseTimeUsed += md.getBaseCastTime() * calculationOptions.getMDCast();
        }

        setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %d Spells Cast", TotalCasts));

        if (TimeUsed > calculationOptions.getFightLengthSeconds()) {
            setActionList(getActionList() + "\r\n\r\nWARNING:\r\nFight Length is less than time needed to perform the actions listed!");
            calculatedStats.setHPSBurstPoints(-1);
            calculatedStats.setHPSSustainPoints(-1);
        } else {
            float mp1use = ManaUsed / calculationOptions.getFightLengthSeconds();

            float periodicRegenOutFSR = StatConversion.getSpiritRegenSec(simstats.getSpirit(), simstats.getIntellect());

            // Add up all mana gains.
            float regen = 0, tmpregen = 0;

            // Spirit/Intellect based Regeneration and MP5
            tmpregen = periodicRegenOutFSR * (1f - calculationOptions.getFSRRatio() / 100f);
            if (tmpregen > 0f) {
                ManaSources.add(new ManaSource("OutFSR", tmpregen));
                regen += tmpregen;
            }
            tmpregen = periodicRegenOutFSR * simstats.getSpellCombatManaRegeneration() * calculationOptions.getFSRRatio() / 100f;
            if (tmpregen > 0f) {
                ManaSources.add(new ManaSource("Meditation", tmpregen));
                regen += tmpregen;
            }
            float holyconccast = calculationOptions.getRenewCast() + calculationOptions.getFlashHealCast() + calculationOptions.getGreaterHealCast() + calculationOptions.getBindingHealCast() * 2;
            if (character.getPriestTalents().getHolyConcentration() > 0 && holyconccast > 0) {
                float hceffect = character.getPriestTalents().getHolyConcentration() * 0.5f / 3f;
                float hccastinterval = calculationOptions.getFightLengthSeconds() / holyconccast;
                float hccritchance = HCCritCounter / holyconccast;
                // Calculate chance that you crit within 8 seconds.
                float hcuptime = 1f - (float) Math.pow(1f - hccritchance, 8f / hccastinterval);
                tmpregen = (periodicRegenOutFSR * (1f - calculationOptions.getFSRRatio() / 100f)
                        + periodicRegenOutFSR * simstats.getSpellCombatManaRegeneration() * calculationOptions.getFSRRatio() / 100f)
                        * hcuptime * hceffect;
                if (tmpregen > 0) {
                    ManaSources.add(new ManaSource("Holy Concentration", tmpregen));
                    regen += tmpregen;
                }
            }
            if (character.getPriestTalents().getRapture() > 0 && calculationOptions.getPWSCast() > 0) {   // New Rapture restores 1.5% - 2% - 2.5% of max mana.
                float rapturereturn = 0.015f + (character.getPriestTalents().getRapture() - 1) * 0.005f;
                float timebetweenshields = calculationOptions.getFightLengthSeconds() / calculationOptions.getPWSCast();
                float maxrapture = simstats.getMana() * rapturereturn / timebetweenshields;
                tmpregen = maxrapture * calculationOptions.getRapture() / 100f;
                if (tmpregen > 0) {
                    ManaSources.add(new ManaSource("Rapture", tmpregen));
                    regen += tmpregen;
                }
            }

            tmpregen = simstats.getMp5() / 5;
            ManaSources.add(new ManaSource("MP5", tmpregen));
            regen += tmpregen;
            tmpregen = simstats.getMana() / (calculationOptions.getFightLengthSeconds());
            ManaSources.add(new ManaSource("Intellect", tmpregen));
            regen += tmpregen;

            if (calculationOptions.getModelProcs()) {
                float heal = 0f;
                for (SpecialEffect se : simstats.specialEffects()) {
                    tmpregen = 0f;
                    if (se.getStats().getManaRestore() > 0 || se.getStats().getMp5() > 0) {
                        if (se.getTrigger() == Trigger.SpellCast
                                || se.getTrigger() == Trigger.HealingSpellCast
                                || se.getTrigger() == Trigger.HealingSpellHit) {
                            tmpregen = se.getAverageStats(calculationOptions.getFightLengthSeconds() / TotalCasts, 1f, 0f, calculationOptions.getFightLengthSeconds()).getManaRestore()
                                    + se.getAverageStats(calculationOptions.getFightLengthSeconds() / TotalCasts, 1f, 0f, calculationOptions.getFightLengthSeconds()).getMp5() / 5;
                        } else if (se.getTrigger() == Trigger.SpellCrit
                                || se.getTrigger() == Trigger.HealingSpellCrit) {
                            tmpregen = se.getAverageStats(calculationOptions.getFightLengthSeconds() / TotalCasts, CritCounter / TotalCasts, 0f, calculationOptions.getFightLengthSeconds()).getManaRestore()
                                    + se.getAverageStats(calculationOptions.getFightLengthSeconds() / TotalCasts, CritCounter / TotalCasts, 0f, calculationOptions.getFightLengthSeconds()).getMp5() / 5;
                        } else if (se.getTrigger() == Trigger.Use) {
                            tmpregen = se.getAverageStats().getManaRestore()
                                    + se.getAverageStats().getMp5() / 5;
                        }
                    }
                    if (tmpregen > 0) {
                        ManaSources.add(new ManaSource(se.toString(), tmpregen));
                        regen += tmpregen;
                    }
                    if (se.getStats().getHealed() > 0f) {
                        if (se.getTrigger() == Trigger.HealingSpellCast
                                || se.getTrigger() == Trigger.SpellCast
                                || se.getTrigger() == Trigger.HealingSpellHit)
                            heal += se.getAverageStats(calculationOptions.getFightLengthSeconds() / TotalCasts, 1f, 0f, calculationOptions.getFightLengthSeconds()).getHealed();
                        else if (se.getTrigger() == Trigger.SpellCrit
                                || se.getTrigger() == Trigger.HealingSpellCrit)
                            heal += se.getAverageStats(calculationOptions.getFightLengthSeconds() / TotalCasts, CritCounter / TotalCasts, 0f, calculationOptions.getFightLengthSeconds()).getHealed();
                        else if (se.getTrigger() == Trigger.Use)
                            heal += se.getAverageStats().getHealed();
                    }
                }
                heal *= (1f + stats.getSpellCrit() * 0.5f) * healmultiplier;
                DirectHeal += heal;
                AbsorbHeal += heal * valanyrProc;
            }

            // External and Other mana sources.
            tmpregen = simstats.getMana() * simstats.getManaRestoreFromMaxManaPerSecond() * calculationOptions.getReplenishment() / 100f;
            if (tmpregen > 0f) {
                ManaSources.add(new ManaSource("Replenishment", tmpregen));
                regen += tmpregen;
            }

            setActionList(getActionList() + "\r\n\r\nMana Options:");

            if (mp1use > regen && character.getRace() == CharacterRace.BloodElf) {   // Arcane Torrent is 6% max mana every 2 minutes.
                tmpregen = simstats.getMana() * 0.06f / 120f;
                ManaSources.add(new ManaSource("Arcane Torrent", tmpregen));
                regen += tmpregen;
                setActionList(getActionList() + "\r\n- Used Arcane Torrent");
            }

            if (mp1use > regen && calculationOptions.getManaAmt() > 0f) {
                float ManaPot = calculationOptions.getManaAmt() * (1f + simstats.getBonusManaPotion());
                tmpregen = ManaPot / (calculationOptions.getFightLengthSeconds());
                ManaSources.add(new ManaSource("Mana Potion", tmpregen));
                setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- Used Mana Potion (%s)", NetFormat.fixed(ManaPot, 0)));
                regen += tmpregen;
            }
            if (mp1use > regen) {
                tmpregen = (simstats.getMana() * 0.4f * calculationOptions.getShadowfiend() / 100f)
                        / ((5f - character.getPriestTalents().getVeiledShadows() * 1f) * 60f);
                ManaSources.add(new ManaSource("Shadowfiend", tmpregen));
                setActionList(getActionList() + "\r\n- Used Shadowfiend");
                regen += tmpregen;
            }
            if (mp1use > regen) {   // 20% increased mana, 3% restored for 4 ticks unless Glyphed. Then its 5.
                float ticks = character.getPriestTalents().isGlyphofHymnofHope() ? 5 : 4;
                tmpregen = (simstats.getMana() * 1.2f * 0.03f * ticks)
                        / (6f * 60f);
                ManaSources.add(new ManaSource("Hymn of Hope", tmpregen));
                setActionList(getActionList() + "\r\n- Used Hymn of Hope");
                regen += tmpregen;
            }
            if (mp1use > regen) {
                setActionList(getActionList() + String.format(java.util.Locale.ROOT, "\r\n- %s mp5 deficit. Could use more mana!", NetFormat.fixed((mp1use - regen) * 5, 0)));
            }

            calculatedStats.setHPSBurstPoints((DirectHeal + AbsorbHeal + OtherHeal) / TimeUsed);
            calculatedStats.setHPSSustainPoints((DirectHeal + AbsorbHeal + OtherHeal) / calculationOptions.getFightLengthSeconds());
            if (regen < mp1use)
                calculatedStats.setHPSSustainPoints(calculatedStats.getHPSSustainPoints() * regen / mp1use);
        }

        // Lets just say that 15% of resilience scales all health by 150%.
        float Resilience = Math.min(15f, StatConversion.getCritReductionFromResilience(simstats.getResilience()) * 100f) / 15f;
        calculatedStats.setSurvivabilityPoints(calculatedStats.getBasicStats().getHealth() * (Resilience * 1.5f + 1f) * calculationOptions.getSurvivability() / 100f);
    }
}
