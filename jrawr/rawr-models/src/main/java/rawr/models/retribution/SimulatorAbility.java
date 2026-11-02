// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Rotation.FCFS Simulator/SimulatorAbility.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class SimulatorAbility {

    public SimulatorAbility(int cooldown, int globalCooldown) {
        this.cooldown = cooldown;
        this.globalCooldown = globalCooldown;
        firstUse = -1;
        lastUse = -1;
        nextUse = 0;
    }

    public static int wait;
    public static int delay;

    public int globalCooldown;
    public int cooldown;
    public int firstUse;
    public int lastUse;
    public int nextUse;
    public int uses;

    public void resetCooldown(int currentTime) {
        // CD reset will be visible only after delay
        nextUse = Math.min(nextUse, currentTime + delay);
    }

    /**
     * Uses the ability next time it's available.
     *
     * @param currentTime Current time
     * @return GCD finish time
     */
    public int useAbility(int currentTime) {
        int usageTime = currentTime + delay;

        if (firstUse < 0)
            firstUse = usageTime;
        lastUse = usageTime;
        nextUse = usageTime + cooldown;
        uses++;
        return usageTime + globalCooldown;
    }

    public boolean shouldAbilityBeUsedNext(int currentTime) {
        return nextUse <= currentTime + wait;
    }

    public boolean canAbilityBeUsedNow(int currentTime) {
        return currentTime >= nextUse;
    }

    public int getNextUseTime(int currentTime) {
        return Math.max(currentTime, nextUse);
    }

    public float effectiveCooldown() {
        return ((float) (lastUse - firstUse)) / (uses - 1);
    }
}
