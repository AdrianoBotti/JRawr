// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Enhance/CombatStats.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.enhance;

public class DPSAnalysis {
    private float dps = 0f;
    private float miss = -1f;
    private float dodge = -1f;
    private float glancing = -1f;
    private float hit = -1f;
    private float crit = -1f;
    private float ppm = 0f;

    public DPSAnalysis(float dps, float miss, float dodge, float glancing, float crit, float PPM) {
        this.dps = dps;
        this.miss = miss;
        this.dodge = dodge;
        this.glancing = glancing;
        this.crit = crit;
        this.ppm = PPM;
        this.hit = 1f;
        if (miss > 0)
            this.hit -= miss;  // only need to subtract miss from hit as at this point miss includes dodge
        if (dodge > 0)
            this.miss -= dodge;  // so if we have dodge then the true miss is less dodge
        if (glancing > 0)
            this.hit -= glancing;
        if (crit > 0)
            this.hit -= crit;
    }

    public float getDps() {
        return dps;
    }

    public float getPPM() {
        return ppm;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(rawr.models.mage.NetFormat.f2(dps)).append("*");
        if (miss >= 0)
            sb.append("Miss            ").append(rawr.models.mage.NetFormat.f2(100f * miss)).append("%\n");
        if (dodge >= 0)
            sb.append("Dodge        ").append(rawr.models.mage.NetFormat.f2(100f * dodge)).append("%\n");
        if (glancing >= 0)
            sb.append("Glancing     ").append(rawr.models.mage.NetFormat.f2(100f * glancing)).append("%\n");
        if (hit >= 0)
            sb.append("Normal Hit  ").append(rawr.models.mage.NetFormat.f2(100f * hit)).append("%\n");
        if (crit >= 0)
            sb.append("Crit Hit       ").append(rawr.models.mage.NetFormat.f2(100f * crit)).append("%\n");
        return sb.toString();
    }
}
