// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/Markov/BloodSurgeMatrix.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpswarr;

class BloodSurgeMatrix {
    private double[] procChances;
    private double proc1;
    private double proc2;

    public double procChances(int index) {
        if (index < 0 || index > 2) return 0;
        return procChances[index];
    }

    public BloodSurgeMatrix(double proc1, double proc2) {
        this.proc1 = proc1;
        this.proc2 = proc2;

        procChances = new double[] {1, 0, 0};
    }

    public void setBaseChance(double procChance) {
        procChances[0] = 1 - procChance;
        procChances[1] = procChance - procChance * proc2;
        procChances[2] = procChance * proc2;
    }

    public void addAbility(double chanceLand, double numActivates) {
        double oldProcChance = procChances[1] + procChances[2];
        double newProcChance = proc1 * chanceLand;
        double finalProcAny = 1 - (1 - oldProcChance) * Math.pow(1 - newProcChance, numActivates);

        procChances[0] = 1 - finalProcAny;
        procChances[1] = finalProcAny - finalProcAny * proc2;
        procChances[2] = finalProcAny * proc2;
    }

    public void addAbility(double chance) {
        addAbility(chance, 1);
    }

    private void resetMatrix(double[] chances) {
        double[] ret = new double[3];

        ret[0] = procChances[0] * chances[0];

        ret[1] = procChances[1] * (chances[0] + chances[1]) + procChances[0] * chances[1];

        ret[2] = 1 - ret[1] - ret[0];

        procChances = ret;
    }
}
