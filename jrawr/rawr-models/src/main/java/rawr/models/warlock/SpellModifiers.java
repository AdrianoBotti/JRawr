// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Warlock/StatUtils.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.warlock;

public class SpellModifiers {

    private float additiveMultiplier;
    private float additiveDirectMultiplier;
    private float additiveTickMultiplier;
    private float multiplicativeMultiplier;
    private float multiplicativeDirectMultiplier;
    private float multiplicativeTickMultiplier;
    private float critChance;
    private float critOverallMultiplier;
    private float critBonusMultiplier;

    public SpellModifiers() {
    }

    public float getAdditiveMultiplier() {
        return additiveMultiplier;
    }

    public float getAdditiveDirectMultiplier() {
        return additiveDirectMultiplier;
    }

    public float getAdditiveTickMultiplier() {
        return additiveTickMultiplier;
    }

    public float getMultiplicativeMultiplier() {
        return multiplicativeMultiplier;
    }

    public float getMultiplicativeDirectMultiplier() {
        return multiplicativeDirectMultiplier;
    }

    public float getMultiplicativeTickMultiplier() {
        return multiplicativeTickMultiplier;
    }

    public float getCritChance() {
        return critChance;
    }

    public float getCritOverallMultiplier() {
        return critOverallMultiplier;
    }

    public float getCritBonusMultiplier() {
        return critBonusMultiplier;
    }

    public void accumulate(SpellModifiers other) {
        addAdditiveMultiplier(other.additiveMultiplier);
        addAdditiveDirectMultiplier(other.additiveDirectMultiplier);
        addAdditiveTickMultiplier(other.additiveTickMultiplier);
        addMultiplicativeMultiplier(other.multiplicativeMultiplier);
        addMultiplicativeDirectMultiplier(other.multiplicativeDirectMultiplier);
        addMultiplicativeTickMultiplier(other.multiplicativeTickMultiplier);
        addCritChance(other.critChance);
        addCritOverallMultiplier(other.critOverallMultiplier);
        addCritBonusMultiplier(other.critBonusMultiplier);
    }

    public float getFinalDirectMultiplier() {
        return (1 + additiveMultiplier + additiveDirectMultiplier)
                * (1 + multiplicativeMultiplier)
                * (1 + multiplicativeDirectMultiplier);
    }

    public float getFinalTickMultiplier() {
        return (1 + additiveMultiplier + additiveTickMultiplier)
                * (1 + multiplicativeMultiplier)
                * (1 + multiplicativeTickMultiplier);
    }

    public float getFinalCritMultiplier() {
        float bonus = 1.5f * (1 + critOverallMultiplier) - 1;
        return 1f + bonus * (1 + critBonusMultiplier);
    }

    public void addAdditiveMultiplier(float val) {
        additiveMultiplier += val;
    }

    public void addAdditiveDirectMultiplier(float val) {
        additiveDirectMultiplier += val;
    }

    public void addAdditiveTickMultiplier(float val) {
        additiveTickMultiplier += val;
    }

    public void addMultiplicativeMultiplier(float val) {
        multiplicativeMultiplier = (1 + multiplicativeMultiplier) * (1 + val) - 1;
    }

    public void addMultiplicativeDirectMultiplier(float val) {
        multiplicativeDirectMultiplier = (1 + multiplicativeDirectMultiplier) * (1 + val) - 1;
    }

    public void addMultiplicativeTickMultiplier(float val) {
        multiplicativeTickMultiplier = (1 + multiplicativeTickMultiplier) * (1 + val) - 1;
    }

    public void addCritChance(float val) {
        critChance = Math.min(1f, critChance + val);
    }

    public void addCritOverallMultiplier(float val) {
        critOverallMultiplier = (1 + critOverallMultiplier) * (1 + val) - 1;
    }

    public void addCritBonusMultiplier(float val) {
        critBonusMultiplier = (1 + critBonusMultiplier) * (1 + val) - 1;
    }
}
