// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/CharacterCalculationsMage.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class SolutionVariable {
    public int segment;
    public int manaSegment;
    public CastingState state;
    public Cycle cycle;
    public VariableType type;
    public double dps;
    public double mps;
    public double tps;

    public boolean isZeroTime() {
        return this.type == VariableType.ManaPotion || this.type == VariableType.ManaGem || this.type == VariableType.ManaOverflow;
    }

    public boolean isMatch(int effects, VariableType cooldownType) {
        return ((effects != 0 && this.state != null && this.state.effectsActive(effects) && (cooldownType == VariableType.None || this.type == cooldownType)) || (effects == 0 && this.type == cooldownType));
    }
}
