// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.HealPriest/CharacterCalculationsHealPriest.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Display values use the .NET default float ToString (rawr.models.mage.NetFormat.netFloatToString)
// and the custom "0"/"0.00" formats (rawr.base.util.NetFormat.fixed), like the C# string.Format calls.
package rawr.models.healpriest;

import rawr.base.stats.BaseStats;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.character.CharacterRace;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

import java.util.LinkedHashMap;
import java.util.Map;

public class CharacterCalculationsHealPriest extends CharacterCalculationsBase {
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

    private float[] subPoints = new float[] { 0f, 0f, 0f };

    @Override
    public float[] getSubPoints() {
        return subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        subPoints = value;
    }

    public float getHPSBurstPoints() {
        return subPoints[0];
    }

    public void setHPSBurstPoints(float value) {
        subPoints[0] = value;
    }

    public float getHPSSustainPoints() {
        return subPoints[1];
    }

    public void setHPSSustainPoints(float value) {
        subPoints[1] = value;
    }

    public float getSurvivabilityPoints() {
        return subPoints[2];
    }

    public void setSurvivabilityPoints(float value) {
        subPoints[2] = value;
    }

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dictValues = new LinkedHashMap<String, String>();
        Stats baseStats = BaseStats.getBaseStats(character);

        dictValues.put("Health", rawr.models.mage.NetFormat.netFloatToString(basicStats.getHealth()));
        dictValues.put("Stamina", rawr.models.mage.NetFormat.netFloatToString(basicStats.getStamina()));
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
        dictValues.put("Mana", rawr.models.mage.NetFormat.netFloatToString(basicStats.getMana()));
        dictValues.put("Intellect", rawr.models.mage.NetFormat.netFloatToString(basicStats.getIntellect()));
        dictValues.put("Spirit", NetFormat.fixed((float) Math.floor(basicStats.getSpirit()), 0));
        dictValues.put("Spell Power", String.format(java.util.Locale.ROOT, "%s*%s from Inner Fire",
                NetFormat.fixed((float) Math.floor(basicStats.getSpellPower()), 0),
                rawr.models.mage.NetFormat.netFloatToString(basicStats.getPriestInnerFire() * CalculationsHealPriest.getInnerFireSpellPowerBonus(character))));
        dictValues.put("In FSR MP5", String.format(java.util.Locale.ROOT, "%s*%s from MP5\r\n%s from Meditation\r\n%s Outside FSR\r\n%s OFSR w/MP5",
                NetFormat.fixed(basicStats.getMp5() + regenInFSR, 0),
                NetFormat.fixed(basicStats.getMp5(), 0),
                NetFormat.fixed(regenInFSR, 0),
                NetFormat.fixed(regenOutFSR, 0),
                NetFormat.fixed(basicStats.getMp5() + regenOutFSR, 0)));
        dictValues.put("Spell Crit", String.format(java.util.Locale.ROOT, "%s%%*%s%% from Intellect\r\n%s%% from %s Crit rating\r\n%s%% from Focused Will\r\n%s%% Class Base\r\n%s%% from Buffs",
                NetFormat.fixed(basicStats.getSpellCrit() * 100f, 2),
                NetFormat.fixed(StatConversion.getSpellCritFromIntellect(basicStats.getIntellect()) * 100f, 2),
                NetFormat.fixed(StatConversion.getSpellCritFromRating(basicStats.getCritRating()) * 100f, 2),
                rawr.models.mage.NetFormat.netFloatToString(basicStats.getCritRating()),
                netInt(character.getPriestTalents().getFocusedWill()),
                NetFormat.fixed(baseStats.getSpellCrit() * 100f, 2),
                NetFormat.fixed(basicStats.getSpellCrit() * 100f - baseStats.getSpellCrit() * 100f - StatConversion.getSpellCritFromRating(basicStats.getCritRating()) * 100f - StatConversion.getSpellCritFromIntellect(basicStats.getIntellect()) * 100f - character.getPriestTalents().getFocusedWill() * 1f, 2)));
        dictValues.put("Healing Crit", String.format(java.util.Locale.ROOT, "%s%%*%d (%d%%) points in Holy Specialization\r\n%d (%d%%) points in Renewed Hope",
                NetFormat.fixed((basicStats.getSpellCrit() * 100f) + character.getPriestTalents().getHolySpecialization() * 1f + character.getPriestTalents().getRenewedHope() * 2f, 2),
                character.getPriestTalents().getHolySpecialization(),
                character.getPriestTalents().getHolySpecialization(),
                character.getPriestTalents().getRenewedHope(),
                character.getPriestTalents().getRenewedHope() * 2));
        dictValues.put("Spell Haste", String.format(java.util.Locale.ROOT, "%s%%*%s%% from %s Haste rating\r\n%d%% (%d) points in Enlightenment\r\n%s%% from Buffs\r\n%ss Global Cooldown",
                NetFormat.fixed(basicStats.getSpellHaste() * 100f, 2),
                NetFormat.fixed(StatConversion.getSpellHasteFromRating(basicStats.getHasteRating()) * 100f, 2),
                rawr.models.mage.NetFormat.netFloatToString(basicStats.getHasteRating()),
                character.getPriestTalents().getEnlightenment() * 2,
                character.getPriestTalents().getEnlightenment(),
                NetFormat.fixed(((1 + basicStats.getSpellHaste()) / (1 + StatConversion.getSpellHasteFromRating(basicStats.getHasteRating())) / (1 + character.getPriestTalents().getEnlightenment() * 0.02f) - 1) * 100f, 2),
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

        BaseSolver solver;
        if (character.getCalculationOptions() instanceof CalculationOptionsHealPriest && ((CalculationOptionsHealPriest) character.getCalculationOptions()).getRole() == eRole.CUSTOM)
            solver = new AdvancedSolver(basicStats, character);
        else
            solver = new Solver(basicStats, character);
        solver.calculate(this);

        dictValues.put("Role", String.format(java.util.Locale.ROOT, "%s*%s", solver.getRole(), solver.getActionList()));
        dictValues.put("Burst", String.format(java.util.Locale.ROOT, "%s", NetFormat.fixed(getHPSBurstPoints(), 0)));
        dictValues.put("Sustained", String.format(java.util.Locale.ROOT, "%s", NetFormat.fixed(getHPSSustainPoints(), 0)));

        dictValues.put("Renew", new Renew(basicStats, character).toString());
        dictValues.put("Flash Heal", new FlashHeal(basicStats, character).toString());
        dictValues.put("Greater Heal", new Heal(basicStats, character).toString());
        dictValues.put("PoH", new PrayerOfHealing(basicStats, character).toString());
        dictValues.put("Binding Heal", new BindingHeal(basicStats, character).toString());
        dictValues.put("Prayer of Mending", new PrayerOfMending(basicStats, character).toString());
        dictValues.put("Power Word Shield", new PowerWordShield(basicStats, character).toString());
        dictValues.put("Holy Nova", new HolyNova(basicStats, character).toString());

        if (character.getPriestTalents().getCircleOfHealing() > 0)
            dictValues.put("CoH", new CircleOfHealing(basicStats, character).toString());
        else
            dictValues.put("CoH", "- *No required talents");

        if (character.getPriestTalents().getLightwell() > 0)
            dictValues.put("Lightwell", new Lightwell(basicStats, character).toString());
        else
            dictValues.put("Lightwell", "- *No required talents");

        if (character.getPriestTalents().getPenance() > 0)
            dictValues.put("Penance", new Penance(basicStats, character).toString());
        else
            dictValues.put("Penance", "- *No required talents");

        if (race == CharacterRace.Draenei)
            dictValues.put("Gift of the Naaru", new GiftOfTheNaaru(basicStats, character).toString());
        else
            dictValues.put("Gift of the Naaru", "-");
        dictValues.put("Divine Hymn", new DivineHymn(basicStats, character).toString());
        dictValues.put("Resurrection", new Resurrection(basicStats, character).toString());

        return dictValues;
    }

    private static String netInt(int value) {
        return Integer.toString(value);
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Health": return basicStats.getHealth();
            case "Resilience": return basicStats.getResilience();
            case "Mana": return basicStats.getMana();
            case "InFSR Regen": return basicStats.getMp5() + regenInFSR;
            case "OutFSR Regen": return basicStats.getMp5() + regenOutFSR;
            case "Haste Rating": return basicStats.getHasteRating();
            case "Haste %": return basicStats.getSpellHaste() * 100f;
            case "Crit Rating": return basicStats.getCritRating();
            case "Healing Crit %": return (basicStats.getSpellCrit() * 100f) + character.getPriestTalents().getHolySpecialization() * 1f + character.getPriestTalents().getRenewedHope() * 2f;
            case "PW:Shield": return new PowerWordShield(basicStats, character).getAvgHeal();
            case "GHeal Avg": return new Heal(basicStats, character).getAvgHeal();
            case "FHeal Avg": return new FlashHeal(basicStats, character).getAvgHeal();
            case "CoH Avg": return new CircleOfHealing(basicStats, character).getAvgHeal();
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
