// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Healadin/CharacterCalculationsHealadin.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Display values use the .NET N0/N1/N2 formats (rawr.base.util.NetFormat.grouped) and the plain float
// ToString of the spell objects.
package rawr.models.healadin;

import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.util.NetFormat;
import rawr.base.stats.Stats;

import java.util.LinkedHashMap;
import java.util.Map;

public class CharacterCalculationsHealadin extends CharacterCalculationsBase {
    private float overallPoints = 0f;

    @Override
    public float getOverallPoints() {
        return this.overallPoints;
    }

    @Override
    public void setOverallPoints(float value) {
        this.overallPoints = value;
    }

    private float[] subPoints = new float[] { 0f, 0f };

    @Override
    public float[] getSubPoints() {
        return this.subPoints;
    }

    @Override
    public void setSubPoints(float[] value) {
        this.subPoints = value;
    }

    public float getFightPoints() {
        return this.subPoints[0];
    }

    public void setFightPoints(float value) {
        this.subPoints[0] = value;
    }

    public float getBurstPoints() {
        return this.subPoints[1];
    }

    public void setBurstPoints(float value) {
        this.subPoints[1] = value;
    }

    public float avgHPS;
    public float avgHPM;
    public float totalHealed;
    public float totalMana;

    public float fightLength;

    public FlashOfLight foL;
    public HolyLight hl;
    public HolyShock hS;
    public SacredShield sS;
    public BeaconOfLight boL;
    public JudgementsOfThePure jotP;

    public float rotationFoL;
    public float rotationHL;
    public float rotationHS;
    public float rotationJotP;
    public float rotationBoL;
    public float rotationSS;

    public float healedFoL;
    public float healedHL;
    public float healedHS;
    public float healedGHL;
    public float healedBoL;
    public float healedSS;
    public float healedOther;

    public float usageFoL;
    public float usageHL;
    public float usageHS;
    public float usageBoL;
    public float usageSS;
    public float usageJotP;

    public float manaBase;
    public float manaMp5;
    public float manaReplenishment;
    public float manaArcaneTorrent;
    public float manaDivinePlea;
    public float manaLayOnHands;
    public float manaOther;

    public Stats basicStats;

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        Map<String, String> dictValues = new LinkedHashMap<String, String>();
        dictValues.put("Status", String.format(java.util.Locale.ROOT, "Overall: %-10s Fight: %-10s Burst: %-10s",
                NetFormat.grouped(this.overallPoints),
                NetFormat.grouped(getFightPoints()),
                NetFormat.grouped(getBurstPoints())));

        // Basic Stats
        dictValues.put("Health", NetFormat.grouped(basicStats.getHealth()));
        dictValues.put("Stamina", NetFormat.grouped(basicStats.getStamina()));
        dictValues.put("Mana", NetFormat.grouped(basicStats.getMana()));
        dictValues.put("Intellect", NetFormat.grouped(basicStats.getIntellect()));
        dictValues.put("Spell Power", NetFormat.grouped(basicStats.getSpellPower()));
        dictValues.put("Mp5", NetFormat.grouped(basicStats.getMp5()));
        dictValues.put("Spell Crit", NetFormat.grouped(basicStats.getSpellCrit() * 100, 2) + "%*" + rawr.models.mage.NetFormat.netFloatToString(basicStats.getCritRating()) + " Crit Rating");
        dictValues.put("Spell Haste", NetFormat.grouped(basicStats.getSpellHaste() * 100, 2) + "%*" + rawr.models.mage.NetFormat.netFloatToString(basicStats.getHasteRating()) + " Haste Rating");

        // Cycle Stats
        dictValues.put("Total Healed", NetFormat.grouped(totalHealed) + " healing");
        dictValues.put("Total Mana", NetFormat.grouped(totalMana) + " mana");
        dictValues.put("Average Healing per sec", NetFormat.grouped(avgHPS) + " hps");
        dictValues.put("Average Healing per mana", NetFormat.grouped(avgHPM, 2) + " hpm");

        // Rotation Info
        dictValues.put("Holy Light Time", NetFormat.grouped(rotationHL, 1) + " sec");
        dictValues.put("Flash of Light Time", NetFormat.grouped(rotationFoL, 1) + " sec");
        dictValues.put("Holy Shock Time", NetFormat.grouped(rotationHS, 1) + " sec");
        dictValues.put("Sacred Shield Time", NetFormat.grouped(rotationSS, 1) + " sec");
        dictValues.put("Beacon of Light Time", NetFormat.grouped(rotationBoL, 1) + " sec");
        dictValues.put("Judgement Time", NetFormat.grouped(rotationJotP, 1) + " sec");

        // Healing Breakdown
        dictValues.put("Holy Light Healed", NetFormat.grouped(healedHL) + " healed");
        dictValues.put("Flash of Light Healed", NetFormat.grouped(healedFoL) + " healed");
        dictValues.put("Holy Shock Healed", NetFormat.grouped(healedHS) + " healed");
        dictValues.put("Sacred Shield Healed", NetFormat.grouped(healedSS) + " healed");
        dictValues.put("Beacon of Light Healed", NetFormat.grouped(healedBoL) + " healed");
        dictValues.put("Glyph of HL Healed", NetFormat.grouped(healedGHL) + " healed");
        dictValues.put("Other Healed", NetFormat.grouped(healedOther) + " healed");

        dictValues.put("Holy Light", "*" + hl.toString());
        dictValues.put("Flash of Light", "*" + foL.toString());
        dictValues.put("Holy Shock", "*" + hS.toString());
        dictValues.put("Sacred Shield", "*" + sS.toString());

        return dictValues;
    }

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Health": return basicStats.getHealth();
            case "Holy Light Cast Time": return hl.castTime();
            case "Holy Light HPS": return hl.hps();
            case "Holy Light Time": return rotationHL;
            case "Flash of Light Cast Time": return foL.castTime();
            case "Flash of Light HPS": return foL.hps();
            case "Flash of Light Time": return rotationFoL;
        }
        return 0f;
    }
}
