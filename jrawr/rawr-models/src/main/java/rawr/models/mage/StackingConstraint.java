// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/Solver.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class StackingConstraint {
    public int row;
    public EffectCooldown effect1;
    public EffectCooldown effect2;
    public double maximumStackingDuration;
}
