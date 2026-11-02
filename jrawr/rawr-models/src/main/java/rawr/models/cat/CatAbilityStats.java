// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Cat/CatAbilityStats.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Display strings use .NET-compatible formatters: general G7
// (rawr.models.mage.NetFormat.netFloatToString, the same helper as the Mage/RestoSham displays),
// F0/F1 (rawr.base.util.NetFormat.fixed) and P1/P2 (local `percent`, Mono writes "n %" with a space).
package rawr.models.cat;

import rawr.models.mage.NetFormat;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public abstract class CatAbilityStats {
    public float damagePerHit;
    public float damagePerSwing;
    public float energyCost;
    public float durationUptime;
    public float durationAverage;
    public float damagePerHitPerCP;
    public float damagePerSwingPerCP;
    public float durationPerCP;

    public abstract String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration);

    static String f0(float value) {
        return rawr.base.util.NetFormat.fixed(value, 0);
    }

    static String f1(float value) {
        return rawr.base.util.NetFormat.fixed(value, 1);
    }

    static String percent(float value, int decimals) {
        if (Float.isNaN(value)) return "NaN";
        if (Float.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        BigDecimal g7 = new BigDecimal(value).round(new MathContext(7, RoundingMode.HALF_UP));
        return g7.multiply(new BigDecimal(100)).setScale(decimals, RoundingMode.HALF_UP).toPlainString() + " %";
    }
}

class CatMeleeStats extends CatAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (damagePerSwing + cp * damagePerSwingPerCP);
        return f0(useCount) + "x   (" + percent(damageDone / totalDamage, 1) + ")*Use Count:  " + NetFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + NetFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Swing:  " + NetFormat.netFloatToString(damagePerSwing)
                + "\r\nDamage Per Hit:  " + NetFormat.netFloatToString(damagePerHit);
    }
}

class CatMangleStats extends CatAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (damagePerSwing + cp * damagePerSwingPerCP);
        return f1(damagePerSwing / energyCost) + " DPE   (" + percent(damageDone / totalDamage, 1) + ")*Use Count:  " + NetFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + NetFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Landed Swing:  " + NetFormat.netFloatToString(damagePerSwing * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + NetFormat.netFloatToString(damagePerHit)
                + "\r\nDamage Per Energy:  " + NetFormat.netFloatToString(damagePerSwing / energyCost);
    }
}

class CatShredStats extends CatAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (damagePerSwing + cp * damagePerSwingPerCP);
        return f1(damagePerSwing / energyCost) + " DPE   (" + percent(damageDone / totalDamage, 1) + ")*Use Count:  " + NetFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + NetFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Landed Swing:  " + NetFormat.netFloatToString(damagePerSwing * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + NetFormat.netFloatToString(damagePerHit)
                + "\r\nDamage Per Energy:  " + NetFormat.netFloatToString(damagePerSwing / energyCost);
    }
}

class CatRakeStats extends CatAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (damagePerSwing + cp * damagePerSwingPerCP);
        return f1(damagePerSwing / energyCost) + " DPE   (" + percent(damageDone / totalDamage, 1) + ")*Use Count:  " + NetFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + NetFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Landed Swing:  " + NetFormat.netFloatToString(damagePerSwing * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + NetFormat.netFloatToString(damagePerHit)
                + "\r\nDamage Per Energy:  " + NetFormat.netFloatToString(damagePerSwing / energyCost)
                + "\r\nDuration:  " + NetFormat.netFloatToString(durationUptime) + "sec"
                + "\r\nUptime:  " + percent((durationUptime * useCount) / totalDuration, 2);
    }
}

class CatRipStats extends CatAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (damagePerSwing + cp * damagePerSwingPerCP);
        return f1(damagePerSwing / energyCost) + " DPE   (" + percent(damageDone / totalDamage, 1) + ")*Use Count:  " + NetFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + NetFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Landed Swing:  " + NetFormat.netFloatToString(damagePerSwing * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + NetFormat.netFloatToString(damagePerHit)
                + "\r\nDamage Per Energy:  " + NetFormat.netFloatToString(damagePerSwing / energyCost)
                + "\r\nDuration:  " + NetFormat.netFloatToString(durationUptime) + "sec"
                + "\r\nUptime:  " + percent((durationUptime * useCount) / totalDuration, 2);
    }
}

class CatBiteStats extends CatAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damagePerSwing = (this.damagePerSwing + cp * damagePerSwingPerCP);
        float damagePerHit = (this.damagePerHit + cp * damagePerHitPerCP);
        float damageDone = useCount * damagePerSwing;
        float dpe1 = (this.damagePerSwing + 1f * damagePerSwingPerCP) / energyCost;
        float dpe2 = (this.damagePerSwing + 2f * damagePerSwingPerCP) / energyCost;
        float dpe3 = (this.damagePerSwing + 3f * damagePerSwingPerCP) / energyCost;
        float dpe4 = (this.damagePerSwing + 4f * damagePerSwingPerCP) / energyCost;
        float dpe5 = (this.damagePerSwing + 5f * damagePerSwingPerCP) / energyCost;
        return f1(damagePerSwing / energyCost) + " DPE   (" + percent(damageDone / totalDamage, 1) + ")*Use Count:  " + NetFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + NetFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Landed Swing:  " + NetFormat.netFloatToString(damagePerSwing * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + NetFormat.netFloatToString(damagePerHit)
                + "\r\nDamage Per Energy:  " + NetFormat.netFloatToString(damagePerSwing / energyCost)
                + "\r\n1cp DPE:  " + NetFormat.netFloatToString(dpe1)
                + "\r\n2cp DPE:  " + NetFormat.netFloatToString(dpe2)
                + "\r\n3cp DPE:  " + NetFormat.netFloatToString(dpe3)
                + "\r\n4cp DPE:  " + NetFormat.netFloatToString(dpe4)
                + "\r\n5cp DPE:  " + NetFormat.netFloatToString(dpe5)
                + "\r\n";
    }
}

class CatRoarStats extends CatAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        String usage = NetFormat.netFloatToString(useCount) + "x*Use Count: " + NetFormat.netFloatToString(useCount);
        String stats = NetFormat.netFloatToString(durationUptime + (cp * durationPerCP)) + "sec, " + NetFormat.netFloatToString(cp)
                + "cp*Duration: " + NetFormat.netFloatToString(durationUptime + (cp * durationPerCP))
                + "\r\nTarget Combo Points: " + NetFormat.netFloatToString(cp) + "%";
        return usage + stats;
    }
}
