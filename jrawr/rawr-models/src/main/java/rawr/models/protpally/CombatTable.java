// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtPaladin/CombatTable.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the fully commented-out ResistTable class in the original is dead code and omitted.
package rawr.models.protpally;

import rawr.base.character.Character;
import rawr.base.items.ItemType;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public abstract class CombatTable {
    protected Character character;
    protected CalculationOptionsProtPaladin calcOpts;
    protected Stats stats;
    protected Ability ability;

    private float miss;
    private float dodge;
    private float parry;
    private float block;
    private float glance;
    private float resist;
    private float critical;
    private float hit;

    public float getMiss() { return miss; }
    protected void setMiss(float value) { miss = value; }
    public float getDodge() { return dodge; }
    protected void setDodge(float value) { dodge = value; }
    public float getParry() { return parry; }
    protected void setParry(float value) { parry = value; }
    public float getBlock() { return block; }
    protected void setBlock(float value) { block = value; }
    public float getGlance() { return glance; }
    protected void setGlance(float value) { glance = value; }
    public float getResist() { return resist; }
    protected void setResist(float value) { resist = value; }
    public float getCritical() { return critical; }
    protected void setCritical(float value) { critical = value; }
    public float getHit() { return hit; }
    protected void setHit(float value) { hit = value; }

    public float anyHit() {
        return (1.0f - (miss + dodge + parry));
    }

    public float anyMiss() {
        return (miss + dodge + parry);
    }

    protected void calculate() {
    }

    protected void initialize(Character character, Stats stats, Ability ability, CalculationOptionsProtPaladin calcOpts) {
        this.character = character;
        this.calcOpts = calcOpts;
        this.stats = stats;
        this.ability = ability;
        calculate();
    }
}
