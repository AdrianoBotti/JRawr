// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Rogue/RogueAbilityStats.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.rogue;

import rawr.base.items.Item;

public abstract class RogueAbilityStats {
    private float damagePerHit;
    private float damagePerSwing;
    private float energyCost;
    private float durationUptime;
    private float durationAverage;
    private float damagePerHitPerCP;
    private float damagePerSwingPerCP;
    private float durationPerCP;
    private Item weapon;
    private float critChance;
    private float cpPerSwing;

    public float getDamagePerHit() {
        return damagePerHit;
    }

    public void setDamagePerHit(float value) {
        damagePerHit = value;
    }

    public float getDamagePerSwing() {
        return damagePerSwing;
    }

    public void setDamagePerSwing(float value) {
        damagePerSwing = value;
    }

    public float getEnergyCost() {
        return energyCost;
    }

    public void setEnergyCost(float value) {
        energyCost = value;
    }

    public float getDurationUptime() {
        return durationUptime;
    }

    public void setDurationUptime(float value) {
        durationUptime = value;
    }

    public float getDurationAverage() {
        return durationAverage;
    }

    public void setDurationAverage(float value) {
        durationAverage = value;
    }

    public float getDamagePerHitPerCP() {
        return damagePerHitPerCP;
    }

    public void setDamagePerHitPerCP(float value) {
        damagePerHitPerCP = value;
    }

    public float getDamagePerSwingPerCP() {
        return damagePerSwingPerCP;
    }

    public void setDamagePerSwingPerCP(float value) {
        damagePerSwingPerCP = value;
    }

    public float getDurationPerCP() {
        return durationPerCP;
    }

    public void setDurationPerCP(float value) {
        durationPerCP = value;
    }

    public Item getWeapon() {
        return weapon;
    }

    public void setWeapon(Item value) {
        weapon = value;
    }

    public float getCritChance() {
        return critChance;
    }

    public void setCritChance(float value) {
        critChance = value;
    }

    public float getCPPerSwing() {
        return cpPerSwing;
    }

    public void setCPPerSwing(float value) {
        cpPerSwing = value;
    }

    public abstract String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration);
}

class RogueMHStats extends RogueAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (getDamagePerSwing() + cp * getDamagePerSwingPerCP());
        return RogueFormat.fixed(useCount, 0) + "x   (" + RogueFormat.percent(damageDone / totalDamage, 1)
                + ")*Use Count:  " + RogueFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + RogueFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + RogueFormat.percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Swing:  " + RogueFormat.netFloatToString(getDamagePerSwing())
                + "\r\nDamage Per Hit:  " + RogueFormat.netFloatToString(getDamagePerHit());
    }
}

class RogueOHStats extends RogueAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (getDamagePerSwing() + cp * getDamagePerSwingPerCP());
        return RogueFormat.fixed(useCount, 0) + "x   (" + RogueFormat.percent(damageDone / totalDamage, 1)
                + ")*Use Count:  " + RogueFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + RogueFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + RogueFormat.percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Swing:  " + RogueFormat.netFloatToString(getDamagePerSwing())
                + "\r\nDamage Per Hit:  " + RogueFormat.netFloatToString(getDamagePerHit());
    }
}

class RogueBackstabStats extends RogueAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (getDamagePerSwing() + cp * getDamagePerSwingPerCP());
        return RogueFormat.fixed(getDamagePerSwing() / getEnergyCost(), 1) + " DPE   ("
                + RogueFormat.percent(damageDone / totalDamage, 1)
                + ")*Use Count:  " + RogueFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + RogueFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + RogueFormat.percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Swing:  " + RogueFormat.netFloatToString(getDamagePerSwing() * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + RogueFormat.netFloatToString(getDamagePerHit())
                + "\r\nDamage Per Energy:  " + RogueFormat.netFloatToString(getDamagePerSwing() / getEnergyCost());
    }
}

class RogueHemoStats extends RogueAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (getDamagePerSwing() + cp * getDamagePerSwingPerCP());
        return RogueFormat.fixed(getDamagePerSwing() / getEnergyCost(), 1) + " DPE   ("
                + RogueFormat.percent(damageDone / totalDamage, 1)
                + ")*Use Count:  " + RogueFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + RogueFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + RogueFormat.percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Swing:  " + RogueFormat.netFloatToString(getDamagePerSwing() * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + RogueFormat.netFloatToString(getDamagePerHit())
                + "\r\nDamage Per Energy:  " + RogueFormat.netFloatToString(getDamagePerSwing() / getEnergyCost());
    }
}

class RogueMutiStats extends RogueAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (getDamagePerSwing() + cp * getDamagePerSwingPerCP());
        return RogueFormat.fixed(getDamagePerSwing() / getEnergyCost(), 1) + " DPE   ("
                + RogueFormat.percent(damageDone / totalDamage, 1)
                + ")*Use Count:  " + RogueFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + RogueFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + RogueFormat.percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Swing:  " + RogueFormat.netFloatToString(getDamagePerSwing() * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + RogueFormat.netFloatToString(getDamagePerHit())
                + "\r\nDamage Per Energy:  " + RogueFormat.netFloatToString(getDamagePerSwing() / getEnergyCost());
    }
}

class RogueSStrikeStats extends RogueAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (getDamagePerSwing() + cp * getDamagePerSwingPerCP());
        return RogueFormat.fixed(getDamagePerSwing() / getEnergyCost(), 1) + " DPE   ("
                + RogueFormat.percent(damageDone / totalDamage, 1)
                + ")*Use Count:  " + RogueFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + RogueFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + RogueFormat.percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Swing:  " + RogueFormat.netFloatToString(getDamagePerSwing() * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + RogueFormat.netFloatToString(getDamagePerHit())
                + "\r\nDamage Per Energy:  " + RogueFormat.netFloatToString(getDamagePerSwing() / getEnergyCost());
    }
}

class RogueRuptStats extends RogueAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (getDamagePerSwing() + cp * getDamagePerSwingPerCP());
        return RogueFormat.fixed(getDamagePerSwing() / getEnergyCost(), 1) + " DPE   ("
                + RogueFormat.percent(damageDone / totalDamage, 1)
                + ")*Use Count:  " + RogueFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + RogueFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + RogueFormat.percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Swing:  " + RogueFormat.netFloatToString(getDamagePerSwing() * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + RogueFormat.netFloatToString(getDamagePerHit())
                + "\r\nDamage Per Energy:  " + RogueFormat.netFloatToString(getDamagePerSwing() / getEnergyCost())
                + "\r\nDuration:  " + RogueFormat.netFloatToString(getDurationUptime() + (cp * getDurationPerCP())) + "sec"
                + "\r\nUptime:  " + RogueFormat.percent(((getDurationUptime() + (cp * getDurationPerCP())) * useCount) / totalDuration, 2);
    }
}

class RogueEnvenomStats extends RogueAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damagePerSwing = (getDamagePerSwing() + cp * getDamagePerSwingPerCP());
        float damagePerHit = (getDamagePerHit() + cp * getDamagePerHitPerCP());
        float damageDone = useCount * damagePerSwing;
        float dpe1 = (getDamagePerSwing() + 1f * getDamagePerSwingPerCP()) / getEnergyCost();
        float dpe2 = (getDamagePerSwing() + 2f * getDamagePerSwingPerCP()) / getEnergyCost();
        float dpe3 = (getDamagePerSwing() + 3f * getDamagePerSwingPerCP()) / getEnergyCost();
        float dpe4 = (getDamagePerSwing() + 4f * getDamagePerSwingPerCP()) / getEnergyCost();
        float dpe5 = (getDamagePerSwing() + 5f * getDamagePerSwingPerCP()) / getEnergyCost();
        return RogueFormat.fixed(damagePerSwing / getEnergyCost(), 1) + " DPE   ("
                + RogueFormat.percent(damageDone / totalDamage, 1)
                + ")*Use Count:  " + RogueFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + RogueFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + RogueFormat.percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Swing:  " + RogueFormat.netFloatToString(damagePerSwing * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + RogueFormat.netFloatToString(damagePerHit)
                + "\r\nDamage Per Energy:  " + RogueFormat.netFloatToString(damagePerSwing / getEnergyCost())
                + "\r\n1cp DPE:  " + RogueFormat.netFloatToString(dpe1)
                + "\r\n2cp DPE:  " + RogueFormat.netFloatToString(dpe2)
                + "\r\n3cp DPE:  " + RogueFormat.netFloatToString(dpe3)
                + "\r\n4cp DPE:  " + RogueFormat.netFloatToString(dpe4)
                + "\r\n5cp DPE:  " + RogueFormat.netFloatToString(dpe5)
                + "\r\n";
    }
}

class RogueEvisStats extends RogueAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damagePerSwing = (getDamagePerSwing() + cp * getDamagePerSwingPerCP());
        float damagePerHit = (getDamagePerHit() + cp * getDamagePerHitPerCP());
        float damageDone = useCount * damagePerSwing;
        float dpe1 = (getDamagePerSwing() + 1f * getDamagePerSwingPerCP()) / getEnergyCost();
        float dpe2 = (getDamagePerSwing() + 2f * getDamagePerSwingPerCP()) / getEnergyCost();
        float dpe3 = (getDamagePerSwing() + 3f * getDamagePerSwingPerCP()) / getEnergyCost();
        float dpe4 = (getDamagePerSwing() + 4f * getDamagePerSwingPerCP()) / getEnergyCost();
        float dpe5 = (getDamagePerSwing() + 5f * getDamagePerSwingPerCP()) / getEnergyCost();
        return RogueFormat.fixed(damagePerSwing / getEnergyCost(), 1) + " DPE   ("
                + RogueFormat.percent(damageDone / totalDamage, 1)
                + ")*Use Count:  " + RogueFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + RogueFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + RogueFormat.percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Swing:  " + RogueFormat.netFloatToString(damagePerSwing * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + RogueFormat.netFloatToString(damagePerHit)
                + "\r\nDamage Per Energy:  " + RogueFormat.netFloatToString(damagePerSwing / getEnergyCost())
                + "\r\n1cp DPE:  " + RogueFormat.netFloatToString(dpe1)
                + "\r\n2cp DPE:  " + RogueFormat.netFloatToString(dpe2)
                + "\r\n3cp DPE:  " + RogueFormat.netFloatToString(dpe3)
                + "\r\n4cp DPE:  " + RogueFormat.netFloatToString(dpe4)
                + "\r\n5cp DPE:  " + RogueFormat.netFloatToString(dpe5)
                + "\r\n";
    }
}

class RogueSnDStats extends RogueAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        String usage = RogueFormat.netFloatToString(useCount) + "x*Use Count: " + RogueFormat.netFloatToString(useCount);
        String stats = RogueFormat.netFloatToString(getDurationUptime() + (cp * getDurationPerCP())) + "sec, "
                + RogueFormat.netFloatToString(cp) + "cp*Duration: "
                + RogueFormat.netFloatToString(getDurationUptime() + (cp * getDurationPerCP()))
                + "\r\nTarget Combo Points: " + RogueFormat.netFloatToString(cp) + "%";
        return usage + stats;
    }
}

class RogueIPStats extends RogueAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (getDamagePerSwing() + cp * getDamagePerSwingPerCP());
        return RogueFormat.netFloatToString(useCount) + "x   (" + RogueFormat.percent(damageDone / totalDamage, 1)
                + ")*Use Count:  " + RogueFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + RogueFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + RogueFormat.percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Swing:  " + RogueFormat.netFloatToString(getDamagePerSwing() * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + RogueFormat.netFloatToString(getDamagePerHit());
    }
}

class RogueDPStats extends RogueAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (getDamagePerSwing() + cp * getDamagePerSwingPerCP());
        return RogueFormat.netFloatToString(useCount) + "x   (" + RogueFormat.percent(damageDone / totalDamage, 1)
                + ")*Use Count:  " + RogueFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + RogueFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + RogueFormat.percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Swing:  " + RogueFormat.netFloatToString(getDamagePerSwing() * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + RogueFormat.netFloatToString(getDamagePerHit());
    }
}

class RogueWPStats extends RogueAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (getDamagePerSwing() + cp * getDamagePerSwingPerCP());
        return RogueFormat.netFloatToString(useCount) + "x   (" + RogueFormat.percent(damageDone / totalDamage, 1)
                + ")*Use Count:  " + RogueFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + RogueFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + RogueFormat.percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Swing:  " + RogueFormat.netFloatToString(getDamagePerSwing() * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + RogueFormat.netFloatToString(getDamagePerHit());
    }
}

class RogueAPStats extends RogueAbilityStats {
    @Override
    public String getStatsTexts(float useCount, float cp, float totalDamage, float chanceNonAvoided, float totalDuration) {
        float damageDone = useCount * (getDamagePerSwing() + cp * getDamagePerSwingPerCP());
        return RogueFormat.netFloatToString(useCount) + "x   (" + RogueFormat.percent(damageDone / totalDamage, 1)
                + ")*Use Count:  " + RogueFormat.netFloatToString(useCount)
                + "\r\nDamage Done:  " + RogueFormat.netFloatToString(damageDone)
                + "\r\n% of Total Damage:  " + RogueFormat.percent(damageDone / totalDamage, 2)
                + "\r\nDamage Per Swing:  " + RogueFormat.netFloatToString(getDamagePerSwing() * chanceNonAvoided)
                + "\r\nDamage Per Hit:  " + RogueFormat.netFloatToString(getDamagePerHit());
    }
}
