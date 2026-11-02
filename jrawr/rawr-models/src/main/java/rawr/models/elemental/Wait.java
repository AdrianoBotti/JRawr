// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/Wait.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.elemental;

import rawr.models.mage.NetFormat;

public class Wait extends Spell {
    public Wait() {
        super();
    }

    @Override
    protected void setBaseValues() {
        super.setBaseValues();

        gcd = 0f;
        missChance = 0f;
        totalCoef = 0f;
        spCoef = 0f;
    }

    public void initialize(float duration) {
        castTime = duration;
        baseCastTime = duration;
        latencyCast = 0;
        latencyGcd = 0;
    }

    public Wait(float duration) {
        this();
        initialize(duration);
    }

    @Override
    public String toString() {
        return "W(" + NetFormat.netDoubleToString(netRound(castTime, 2)) + ")";
    }
}
