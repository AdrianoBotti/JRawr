// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/CharacterCalculationsMage.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.calculations.CharacterCalculationsBase;

import java.util.Locale;
import java.util.Map;

public class CharacterCalculationsMage extends CharacterCalculationsBase {
    private float overallPoints = 0f;

    @Override
    public float getOverallPoints() {
        return this.overallPoints;
    }

    @Override
    public void setOverallPoints(float overallPoints) {
        this.overallPoints = overallPoints;
    }

    private float[] subPoints = new float[2];

    @Override
    public float[] getSubPoints() {
        return this.subPoints;
    }

    @Override
    public void setSubPoints(float[] subPoints) {
        this.subPoints = subPoints;
    }

    public float getDpsRating() {
        return this.subPoints[0];
    }

    public float getSurvivabilityRating() {
        return this.subPoints[1];
    }

    public DisplayCalculations displayCalculations;

    public DisplayCalculations getDisplayCalculations() {
        return displayCalculations;
    }

    public void setDisplayCalculations(DisplayCalculations value) {
        displayCalculations = value;
    }

    public OptimizableCalculations optimizableCalculations;

    public OptimizableCalculations getOptimizableCalculations() {
        return optimizableCalculations;
    }

    public void setOptimizableCalculations(OptimizableCalculations value) {
        optimizableCalculations = value;
    }

    @Override
    public Map<String, String> getCharacterDisplayCalculationValues() {
        if (getRequiresAsynchronousDisplayCalculation()) {
            Map<String, String> ret = displayCalculations.getCharacterDisplayCalculationValues(false);
            ret.put("Dps", "...");
            ret.put("Total Damage", "...");
            ret.put("Score", "...");
            ret.put("Tps", "...");
            ret.put("Spell Cycles", "...");
            ret.put("By Spell", "...");
            ret.put("Status", "Score: ..., Dps: ..., Survivability: ...");
            displayCalculations.setDisplaySolver(new Solver(displayCalculations.getCharacter(), displayCalculations.getCalculationOptions(), displayCalculations.getCalculationOptions().getDisplaySegmentCooldowns(), displayCalculations.getCalculationOptions().getDisplaySegmentMana(), displayCalculations.getCalculationOptions().getDisplayIntegralMana(), displayCalculations.getCalculationOptions().getDisplayAdvancedConstraintsLevel(), displayCalculations.getMageArmor(), false, displayCalculations.getCalculationOptions().getSmartOptimization(), true, true));
            CalculationsMage.enableSolver(displayCalculations.getDisplaySolver());
            displayCalculations.getCalculationOptions().setSequenceReconstruction(null);
            return ret;
        } else {
            return displayCalculations.getCharacterDisplayCalculationValues(true);
        }
    }

    @Override
    public void cancelAsynchronousCharacterDisplayCalculation() {
        displayCalculations.getDisplaySolver().cancelAsync();
    }

    @Override
    public boolean getRequiresAsynchronousDisplayCalculation() {
        return displayCalculations.getCalculationOptions().getDisplaySegmentCooldowns() != displayCalculations.getCalculationOptions().getComparisonSegmentCooldowns() || displayCalculations.getCalculationOptions().getDisplaySegmentMana() != displayCalculations.getCalculationOptions().getComparisonSegmentMana() || displayCalculations.getCalculationOptions().getDisplayIntegralMana() != displayCalculations.getCalculationOptions().getComparisonIntegralMana() || (displayCalculations.getCalculationOptions().getDisplaySegmentCooldowns() == true && displayCalculations.getCalculationOptions().getDisplayAdvancedConstraintsLevel() != displayCalculations.getCalculationOptions().getComparisonAdvancedConstraintsLevel());
    }

    @Override
    public Map<String, String> getAsynchronousCharacterDisplayCalculationValues() {
        CharacterCalculationsMage smp = displayCalculations.getDisplaySolver().getCharacterCalculations(null);
        smp.displayCalculations.setDisplaySolver(displayCalculations.getDisplaySolver());
        Map<String, String> ret = smp.displayCalculations.getCharacterDisplayCalculationValues(true);
        CalculationsMage.disableSolver(displayCalculations.getDisplaySolver());
        ret.put("Dps", String.format(Locale.ROOT, "%.2f*%.2f%% Error margin", smp.getDpsRating(), Math.abs(getDpsRating() - smp.getDpsRating()) / getDpsRating() * 100));
        return ret;
    }

    public static boolean debugCooldownSegmentation;

    @Override
    public float getOptimizableCalculationValue(String calculation) {
        switch (calculation) {
            case "Health":
                return optimizableCalculations.health;
            case "Nature Resistance":
                return optimizableCalculations.natureResistance;
            case "Fire Resistance":
                return optimizableCalculations.fireResistance;
            case "Frost Resistance":
                return optimizableCalculations.frostResistance;
            case "Shadow Resistance":
                return optimizableCalculations.shadowResistance;
            case "Arcane Resistance":
                return optimizableCalculations.arcaneResistance;
            case "Resilience":
                return optimizableCalculations.resilience;
            case "Chance to Live":
                return 100 * (1 - optimizableCalculations.chanceToDie);
            case "Hit Rating":
                return optimizableCalculations.hitRating;
            case "Haste Rating":
                return optimizableCalculations.hasteRating;
            case "PVP Trinket":
                return optimizableCalculations.pVPTrinket;
            case "Movement Speed":
                return optimizableCalculations.movementSpeed * 100f;
        }
        return 0;
    }
}
