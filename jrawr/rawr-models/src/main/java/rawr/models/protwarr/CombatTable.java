// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtWarr/CombatTable.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protwarr;

import rawr.base.character.Character;
import rawr.base.stats.Stats;

public abstract class CombatTable {
    protected Character character;
    protected CalculationOptionsProtWarr options;
    protected Stats stats;
    protected Ability ability;

    private float miss;
    private float dodge;
    private float parry;
    private float block;
    private float glance;
    private float critical;
    private float hit;

    public float getMiss() {
        return miss;
    }

    protected void setMiss(float value) {
        miss = value;
    }

    public float getDodge() {
        return dodge;
    }

    protected void setDodge(float value) {
        dodge = value;
    }

    public float getParry() {
        return parry;
    }

    protected void setParry(float value) {
        parry = value;
    }

    public float getBlock() {
        return block;
    }

    protected void setBlock(float value) {
        block = value;
    }

    public float getGlance() {
        return glance;
    }

    protected void setGlance(float value) {
        glance = value;
    }

    public float getCritical() {
        return critical;
    }

    protected void setCritical(float value) {
        critical = value;
    }

    public float getHit() {
        return hit;
    }

    protected void setHit(float value) {
        hit = value;
    }

    public float getAnyHit() {
        return (1.0f - (miss + dodge + parry));
    }

    public float getAnyMiss() {
        return (miss + dodge + parry);
    }

    public float getDodgeParryBlock() {
        return (dodge + parry + block);
    }

    protected void calculate() {
    }

    protected void initialize(Character character, Stats stats, CalculationOptionsProtWarr options, Ability ability) {
        this.character = character;
        this.options = options;
        this.stats = stats;
        this.ability = ability;
        calculate();
    }
}
