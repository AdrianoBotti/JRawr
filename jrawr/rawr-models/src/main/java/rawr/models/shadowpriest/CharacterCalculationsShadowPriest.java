// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ShadowPriest/CharacterCalculationsShadowPriest.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Display values use the .NET default float/double ToString (rawr.models.mage.NetFormat.netFloatToString/
// netDoubleToString) and the custom "0"/"0.00" formats (rawr.base.util.NetFormat.fixed), like the C# string.Format calls.
package rawr.models.shadowpriest;

import rawr.base.stats.BaseStats;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.CharacterRace;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

import java.util.LinkedHashMap;
import java.util.Map;

public class CharacterCalculationsShadowPriest extends CharacterCalculationsBase {
    private Stats basicStats;
    private Character character;

    private float spiritRegen;

    public float getSpiritRegen() {
        return spiritRegen;
    }

    public void setSpiritRegen(float value) {
        spiritRegen = value;
    }

    private float regenInFSR;

    public float getRegenInFSR() {
        return regenInFSR;
    }

    public void setRegenInFSR(float value) {
        regenInFSR = value;
    }

    private float regenOutFSR;

    public float getRegenOutFSR() {
        return regenOutFSR;
    }

    public void setRegenOutFSR(float value) {
        regenOutFSR = value;
    }

    private CharacterRace race;

    public CharacterRace getRace() {
        return race;
    }

    public void setRace(CharacterRace value) {
        race = value;
    }

    public boolean bHoly;

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character value) {
        character = value;
    }

    public Stats getBasicStats() {
        return basicStats;
    }

    public void setBasicStats(Stats value) {
        basicStats = value;
    }

    @Override
    public float getOverallPoints() {
        float f = 0f;
        for (float f2 : subPoints)
            f += f2;
        return f;
    }

    @Override
    public void setOverallPoints(float value) {
    }

    private float[] subPoints = new float[] { 0f, 0f };

    @Override
    public float[] getSubPoints() {
        return subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        subPoints = value;
    }

    public float getDpsPoints() {
        return subPoints[0];
    }

    public void setDpsPoints(float value) {
        subPoints[0] = value;
    }

    public float getSurvivalPoints() {
        return subPoints[1];
    }

    public void setSurvivalPoints(float value) {
        subPoints[1] = value;
    }

    public SolverBase getSolver(Character character, Stats stats) {
        if (character.getPriestTalents().getMindFlay() > 0)
            return new SolverShadow(stats, character);
        else
            return new SolverHoly(stats, character);
    }

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dictValues = new LinkedHashMap<String, String>();
        CalculationOptionsShadowPriest calcOpts = (CalculationOptionsShadowPriest) character.getCalculationOptions();
        Stats baseStats = BaseStats.getBaseStats(character.getLevel(), character.getCharacterClass(), character.getRace());
        boolean Ptr = calcOpts.getPTR();

        dictValues.put("Health", rawr.models.mage.NetFormat.netFloatToString(basicStats.getHealth()));
        float ResilienceCap = 0.15f, ResilienceFromRating = StatConversion.getCritReductionFromResilience(1);
        float Resilience = StatConversion.getCritReductionFromResilience(basicStats.getResilience());
        dictValues.put("Resilience", String.format(java.util.Locale.ROOT, "%s*-%s%% Damage from DoT and Mana Drains\n\r-%s%% Chance to be crit\r\n-%s%% Damage from Crits.\r\n%s",
                rawr.models.mage.NetFormat.netFloatToString(basicStats.getResilience()),
                NetFormat.fixed(Resilience * 100f, 2),
                NetFormat.fixed(Resilience * 100f, 2),
                NetFormat.fixed(Resilience * 100f * 2.2f, 2),
                (Resilience > ResilienceCap)
                        ? String.format(java.util.Locale.ROOT, "%s rating above cap", NetFormat.fixed((float) Math.floor((Resilience - ResilienceCap) / ResilienceFromRating), 0))
                        : String.format(java.util.Locale.ROOT, "%s rating below cap", NetFormat.fixed((float) Math.ceil((ResilienceCap - Resilience) / ResilienceFromRating), 0))));
        dictValues.put("Stamina", rawr.models.mage.NetFormat.netFloatToString(basicStats.getStamina()));
        dictValues.put("Mana", rawr.models.mage.NetFormat.netFloatToString(basicStats.getMana()));
        dictValues.put("Intellect", rawr.models.mage.NetFormat.netFloatToString(basicStats.getIntellect()));
        dictValues.put("Spirit", NetFormat.fixed((float) Math.floor(basicStats.getSpirit()), 0));
        dictValues.put("Spell Power", String.format(java.util.Locale.ROOT, "%s*%s Bonus Shadow\r\n%s Bonus Holy\r\n%s from Inner Fire",
                rawr.models.mage.NetFormat.netDoubleToString(Math.floor(basicStats.getSpellPower())),
                rawr.models.mage.NetFormat.netDoubleToString(Math.floor(basicStats.getSpellPower() + basicStats.getSpellShadowDamageRating())),
                rawr.models.mage.NetFormat.netDoubleToString(Math.floor(basicStats.getSpellPower())),
                rawr.models.mage.NetFormat.netFloatToString(basicStats.getPriestInnerFire() * CalculationsShadowPriest.getInnerFireSpellPowerBonus(character))));
        dictValues.put("Regen", String.format(java.util.Locale.ROOT, "%s*MP5: %s\r\nOutFSR: %s",
                NetFormat.fixed(regenInFSR, 0),
                rawr.models.mage.NetFormat.netFloatToString(basicStats.getMp5()),
                NetFormat.fixed(regenOutFSR, 0)));

        dictValues.put("Crit", String.format(java.util.Locale.ROOT, "%s%%*%s%% from %s Spell Crit rating\r\n%s%% from Intellect\r\n%s%% from Focused Will\r\n%s%% from Base Crit\r\n%s%% from Buffs\r\n%s%% on Mind Blast, Mind Flay and Mind Sear.\r\n%s%% on VT, SW:P and DP\r\n%s%% on Smite, Holy Fire and Penance.",
                NetFormat.fixed(basicStats.getSpellCrit() * 100f, 2),
                NetFormat.fixed(StatConversion.getSpellCritFromRating(basicStats.getCritRating()) * 100f, 2),
                NetFormat.fixed(basicStats.getCritRating(), 0),
                NetFormat.fixed(StatConversion.getSpellCritFromIntellect(basicStats.getIntellect()) * 100f, 2),
                character.getPriestTalents().getFocusedWill(),
                NetFormat.fixed(baseStats.getSpellCrit() * 100f, 2),
                NetFormat.fixed(basicStats.getSpellCrit() * 100f - StatConversion.getSpellCritFromRating(basicStats.getCritRating()) * 100f - StatConversion.getSpellCritFromIntellect(basicStats.getIntellect()) * 100f - baseStats.getSpellCrit() * 100f - character.getPriestTalents().getFocusedWill(), 2),
                NetFormat.fixed(basicStats.getSpellCrit() * 100f + character.getPriestTalents().getMindMelt() * 2f, 2),
                NetFormat.fixed(basicStats.getSpellCrit() * 100f + character.getPriestTalents().getMindMelt() * 3f, 2),
                NetFormat.fixed(basicStats.getSpellCrit() * 100f + character.getPriestTalents().getHolySpecialization() * 1f, 2)));

        float Hit = 100 - (StatConversion.getSpellMiss(-calcOpts.getTargetLevel(), false) * 100);
        float BonusHit = basicStats.getSpellHit() * 100f;
        float RacialHit = 0;
        String RacialText = "";
        if (character.getRace() == CharacterRace.Draenei) {
            RacialHit = 1;
            RacialText = "1% from Draenei Racial\r\n";
            if (!character.activeBuffsContains("Heroic Presence"))
                BonusHit += 1;
        }
        float DebuffHit = character.getPriestTalents().getMisery() * 1f;
        if (character.activeBuffsConflictingBuffContains("Spell Hit Chance Taken"))
            DebuffHit = 3f;
        else
            BonusHit += DebuffHit;

        float RHitRating = 0.01f / StatConversion.getSpellHitFromRating(1);
        float ShadowFocusHit = character.getPriestTalents().getShadowFocus() * 1f;
        float HitShadow = Hit + BonusHit + ShadowFocusHit;
        float HitHoly = Hit + BonusHit;
        dictValues.put("Hit", String.format(java.util.Locale.ROOT, "%s%%*%s%% from %s Hit Rating\r\n%s%% from Buffs\r\n%s%% from %s points in Misery\r\n%s%% from %s points in Shadow Focus\r\n%s%s%% Hit with Shadow spells, %s\r\n%s%% Hit with Holy spells, %s",
                NetFormat.fixed(BonusHit, 2),
                NetFormat.fixed(StatConversion.getSpellHitFromRating(basicStats.getHitRating()) * 100f, 2),
                rawr.models.mage.NetFormat.netFloatToString(basicStats.getHitRating()),
                NetFormat.fixed(BonusHit - StatConversion.getSpellHitFromRating(basicStats.getHitRating()) * 100f - RacialHit - DebuffHit, 2),
                rawr.models.mage.NetFormat.netFloatToString(DebuffHit),
                character.getPriestTalents().getMisery(),
                rawr.models.mage.NetFormat.netFloatToString(ShadowFocusHit),
                character.getPriestTalents().getShadowFocus(),
                RacialText,
                NetFormat.fixed(HitShadow, 2),
                (HitShadow > 100f)
                        ? String.format(java.util.Locale.ROOT, "%s hit rating above cap", rawr.models.mage.NetFormat.netDoubleToString(Math.floor((HitShadow - 100f) * RHitRating)))
                        : String.format(java.util.Locale.ROOT, "%s hit rating below cap", rawr.models.mage.NetFormat.netDoubleToString(Math.ceil((100f - HitShadow) * RHitRating))),
                NetFormat.fixed(HitHoly, 2),
                (HitHoly > 100f)
                        ? String.format(java.util.Locale.ROOT, "%s hit rating above cap", rawr.models.mage.NetFormat.netDoubleToString(Math.floor((HitHoly - 100f) * RHitRating)))
                        : String.format(java.util.Locale.ROOT, "%s hit rating below cap", rawr.models.mage.NetFormat.netDoubleToString(Math.ceil((100f - HitHoly) * RHitRating)))));

        dictValues.put("Haste", String.format(java.util.Locale.ROOT, "%s%%*%s%% from %s Haste rating\r\n%s%% (%s) points in Enlightenment\r\n%s%% from Buffs\r\n%ss Global Cooldown",
                NetFormat.fixed(basicStats.getSpellHaste() * 100f, 2),
                NetFormat.fixed(StatConversion.getSpellHasteFromRating(basicStats.getHasteRating()) * 100f, 2),
                rawr.models.mage.NetFormat.netFloatToString(basicStats.getHasteRating()),
                NetFormat.fixed(character.getPriestTalents().getEnlightenment() * 2, 0),
                character.getPriestTalents().getEnlightenment(),
                NetFormat.fixed(((1f + basicStats.getSpellHaste()) / (1f + StatConversion.getSpellHasteFromRating(basicStats.getHasteRating())) / (1f + character.getPriestTalents().getEnlightenment() * 0.02f) - 1f) * 100f, 2),
                NetFormat.fixed(Math.max(1.0f, 1.5f / (1 + basicStats.getSpellHaste())), 2),
                character.getPriestTalents().getEnlightenment()));
        dictValues.put("Armor", String.format(java.util.Locale.ROOT, "%s*%s%% Damage Reduction.",
                NetFormat.fixed(basicStats.getArmor() + basicStats.getBonusArmor(), 0),
                NetFormat.fixed(StatConversion.getArmorDamageReduction(80, (basicStats.getArmor() + basicStats.getBonusArmor()), 0f, 0f, 0f) * 100f, 2)));

        float[] Resistances = {
                0,
                basicStats.getArcaneResistance() + basicStats.getArcaneResistanceBuff(),
                basicStats.getFireResistance() + basicStats.getFireResistanceBuff(),
                basicStats.getFrostResistance() + basicStats.getFrostResistanceBuff(),
                basicStats.getNatureResistance() + basicStats.getNatureResistanceBuff(),
                basicStats.getShadowResistance() + basicStats.getShadowResistanceBuff(),
        };

        String[] ResistanceNames = {
                "None",
                "Arcane",
                "Fire",
                "Frost",
                "Nature",
                "Shadow",
        };

        String ResistanceString = "*Resistances:";

        float MaxResist = Resistances[0];
        int MaxResistIndex = 0;
        float AvgResist = 0f;
        for (int x = 1; x < Resistances.length; x++) {
            AvgResist += Resistances[x];
            if (Resistances[x] > MaxResist) {
                MaxResist = Resistances[x];
                MaxResistIndex = x;
            }
            ResistanceString += String.format(java.util.Locale.ROOT, "\r\n%s : %s", ResistanceNames[x], rawr.models.mage.NetFormat.netFloatToString(Resistances[x]));
        }
        AvgResist /= (Resistances.length - 1);

        if (AvgResist == 0)
            ResistanceString = "None" + ResistanceString;
        else {
            String ResistanceName = (MaxResist == AvgResist) ? "All" : ResistanceNames[MaxResistIndex];
            ResistanceString = String.format(java.util.Locale.ROOT, "%s : %s", ResistanceName, NetFormat.fixed(MaxResist, 0)) + ResistanceString;
            ResistanceString += String.format(java.util.Locale.ROOT, "\r\n\r\nResist (%s):", ResistanceName);
            ResistanceString += String.format(java.util.Locale.ROOT, "\r\n%s", StatConversion.getResistanceTableString(character.getLevel() + 3, character.getLevel(), MaxResist, 0));
        }

        dictValues.put("Resistance", ResistanceString);

        SolverBase solver = getSolver(character, basicStats);
        solver.calculate(this);

        dictValues.put("Rotation", String.format(java.util.Locale.ROOT, "%s*%s", solver.getName(), solver.getRotation()));
        if (solver.getSpellSimulation() != null) {
            String s = "Spell Cast List:";
            int i = 0;
            for (Spell spell : solver.getSpellSimulation()) {
                if (i++ % 10 == 0)
                    s += "\r\n";
                s += ", " + spell.getName();
            }
            s += "\r\n---Repeat---";
            dictValues.put("Castlist", String.format(java.util.Locale.ROOT, "%s*%s", solver.getSpellSimulation().size(), s));
        } else
            dictValues.put("Castlist", "Empty");
        dictValues.put("DPS", String.format(java.util.Locale.ROOT, "%s*Damage Pr Second", NetFormat.fixed(solver.getDPS(), 0)));

        float baseMana = BaseStats.getBaseStats(character).getMana();

        dictValues.put("SW Pain", new ShadowWordPain(basicStats, character, baseMana, Ptr).toString());
        DevouringPlague dp = new DevouringPlague(basicStats, character, baseMana, Ptr);
        dictValues.put("Devouring Plague", dp.toString());
        if (dp.ImprovedDP != null)
            dictValues.put("Imp. Devouring Plague", dp.ImprovedDP.toString());
        else
            dictValues.put("Imp. Devouring Plague", "- *No required talents");
        dictValues.put("SW Death", new ShadowWordDeath(basicStats, character, baseMana, Ptr).toString());
        dictValues.put("Mind Blast", new MindBlast(basicStats, character, baseMana, Ptr).toString());
        dictValues.put("PW Shield", new PowerWordShield(basicStats, character, baseMana, Ptr).toString());

        if (character.getPriestTalents().getVampiricTouch() > 0)
            dictValues.put("Vampiric Touch", new VampiricTouch(basicStats, character, baseMana, Ptr).toString());
        else
            dictValues.put("Vampiric Touch", "- *No required talents");

        if (character.getPriestTalents().getMindFlay() > 0)
            dictValues.put("Mind Flay", new MindFlay(basicStats, character, baseMana, Ptr).toString());
        else
            dictValues.put("Mind Flay", "- *No required talents");

        dictValues.put("Shadowfiend", new Shadowfiend(basicStats, character, baseMana, Ptr).toString());

        dictValues.put("Smite", new Smite(basicStats, character, baseMana, Ptr).toString());
        dictValues.put("Holy Fire", new HolyFire(basicStats, character, baseMana, Ptr).toString());
        if (character.getPriestTalents().getPenance() > 0)
            dictValues.put("Penance", new Penance(basicStats, character, baseMana, Ptr).toString());
        else
            dictValues.put("Penance", "- *No required talents");

        return dictValues;
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Health": return basicStats.getHealth();
            case "Resilience": return basicStats.getResilience();
            case "Mana": return basicStats.getMana();
            case "Haste Rating": return basicStats.getHasteRating();
            case "Haste %": return basicStats.getSpellHaste() * 100f;
            case "Crit Rating": return basicStats.getCritRating();
            case "MB Crit %": return new MindBlast(basicStats, character, BaseStats.getBaseStats(character).getMana(), ((CalculationOptionsShadowPriest) character.getCalculationOptions()).getPTR()).getCritChance() * 100f;
            case "Hit Rating": return basicStats.getHitRating();
            case "MF cast time (ms)": return new MindFlay(basicStats, character, BaseStats.getBaseStats(character).getMana(), ((CalculationOptionsShadowPriest) character.getCalculationOptions()).getPTR()).getCastTime() * 1000f;
            case "Armor": return basicStats.getArmor() + basicStats.getBonusArmor();
            case "Arcane Resistance": return basicStats.getArcaneResistance() + basicStats.getArcaneResistanceBuff();
            case "Fire Resistance": return basicStats.getFireResistance() + basicStats.getFireResistanceBuff();
            case "Frost Resistance": return basicStats.getFrostResistance() + basicStats.getFrostResistance();
            case "Nature Resistance": return basicStats.getNatureResistance() + basicStats.getNatureResistanceBuff();
            case "Shadow Resistance": return basicStats.getShadowResistance() + basicStats.getShadowResistanceBuff();
        }
        return 0f;
    }
}
