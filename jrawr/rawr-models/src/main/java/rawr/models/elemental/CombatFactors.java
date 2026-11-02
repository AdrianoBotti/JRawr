// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/CombatFactors.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.elemental;

import rawr.base.talents.ShamanTalents;
import rawr.base.stats.Stats;

/**
 * This class holds all combat relevant information. It is used as argument to a Spell or Spellbox
 * in order to Initialize its values.
 */
public class CombatFactors implements ISpellArgs, IRotationOptions {
    public CombatFactors(ShamanTalents talents, Stats stats, int additionalTargets, float latencyCast, float latencyGcd) {
        this(talents, stats, additionalTargets, latencyCast, latencyGcd, true, true, false);
    }

    public CombatFactors(ShamanTalents talents, Stats stats, int additionalTargets, float latencyCast, float latencyGcd,
                         boolean useFireNova, boolean useChainLightning, boolean useDpsFireTotem) {
        this.talents = talents;
        this.stats = stats;
        this.additionalTargets = additionalTargets;
        this.latencyCast = latencyCast;
        this.latencyGcd = latencyGcd;
        this.useFireNova = useFireNova;
        this.useChainLightning = useChainLightning;
        this.useDpsFireTotem = useDpsFireTotem;
    }

    private ShamanTalents talents;

    @Override
    public ShamanTalents getTalents() {
        return talents;
    }

    public void setTalents(ShamanTalents value) {
        talents = value;
    }

    private Stats stats;

    @Override
    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats value) {
        stats = value;
    }

    private float latencyCast;

    @Override
    public float getLatencyCast() {
        return latencyCast;
    }

    public void setLatencyCast(float value) {
        latencyCast = value;
    }

    private float latencyGcd;

    @Override
    public float getLatencyGcd() {
        return latencyGcd;
    }

    public void setLatencyGcd(float value) {
        latencyGcd = value;
    }

    private int additionalTargets;

    @Override
    public int getAdditionalTargets() {
        return additionalTargets;
    }

    public void setAdditionalTargets(int value) {
        additionalTargets = value;
    }

    private boolean useFireNova;

    @Override
    public boolean getUseFireNova() {
        return useFireNova;
    }

    public void setUseFireNova(boolean value) {
        useFireNova = value;
    }

    private boolean useChainLightning;

    @Override
    public boolean getUseChainLightning() {
        return useChainLightning;
    }

    public void setUseChainLightning(boolean value) {
        useChainLightning = value;
    }

    private boolean useDpsFireTotem;

    @Override
    public boolean getUseDpsFireTotem() {
        return useDpsFireTotem;
    }

    public void setUseDpsFireTotem(boolean value) {
        useDpsFireTotem = value;
    }

    private boolean useFireEle;

    @Override
    public boolean getUseFireEle() {
        return useFireEle;
    }

    public void setUseFireEle(boolean value) {
        useFireEle = value;
    }

    private float fightDuration;

    public float getFightDuration() {
        return fightDuration;
    }

    public void setFightDuration(float value) {
        fightDuration = value;
    }
}
