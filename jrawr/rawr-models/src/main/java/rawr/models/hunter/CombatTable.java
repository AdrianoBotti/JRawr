// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/CombatTable.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the Skills.Ability parameter of CombatTable.Initialize/AttackTable is omitted. The only
// AttackTable instantiated by the ported code is the white ranged-weapon table (WhiteAttacks), so the
// ability-dependent branches (CanBeDodged/CanBeParried/CanBeBlocked/CanCrit/ContainCritValue_RW) are
// unreachable; they are documented in the omitted-members list.
package rawr.models.hunter;

import rawr.base.character.Character;
import rawr.base.stats.Stats;
import rawr.models.hunter.HunterEnums.PetAttacks;

public abstract class CombatTable {
    public static CombatTable NULL = new NullCombatTable();
    protected Character character;
    protected CalculationOptionsHunter calcOpts;
    protected CombatFactors combatFactors;
    protected Stats statS;
    protected PetAttacks petAbil;
    protected boolean useSpellHit = false;

    public boolean isWhite;

    public float miss;
    public float dodge;
    public float parry;
    public float block;
    public float glance;
    public float crit;
    public float hit;

    private float anyLand = 0f;
    private float anyNotLand = 1f;

    public float getAnyLand() {
        return this.anyLand;
    }

    public float getAnyNotLand() {
        return this.anyNotLand;
    }

    private boolean alwaysHit = false;

    protected void calculate() {
        this.anyNotLand = dodge + parry + miss;
        this.anyLand = 1f - this.anyNotLand;
    }

    protected void calculate(float[] avoidChances) {
        this.anyNotLand = dodge + parry + miss;
        this.anyLand = 1f - this.anyNotLand;
    }

    protected void calculateAlwaysHit() {
        miss = dodge = parry = block = glance = crit = 0f;
        hit = 1f;
        this.anyLand = 1f;
        this.anyNotLand = 0f;
    }

    protected void initialize(Character character, Stats stats, CombatFactors cf, CalculationOptionsHunter co,
            boolean useSpellHit, boolean alwaysHit) {
        this.character = character;
        this.statS = stats;
        this.calcOpts = co;
        this.combatFactors = cf;
        this.petAbil = PetAttacks.None;
        this.isWhite = true;
        this.useSpellHit = useSpellHit;
        // Start a calc
        if (alwaysHit) calculateAlwaysHit();
        else calculate();
    }

    protected void reset(boolean alwaysHit) {
        if (alwaysHit) calculateAlwaysHit();
        else calculate();
    }

    public void reset() {
        if (this.alwaysHit) return;
        reset(false);
    }

    @Override
    public String toString() {
        return String.format("Land: %s Not Land: %s Miss: %s Dodge: %s Parry: %s Glance: %s Crit: %s Hit: %s",
                getAnyLand(), getAnyNotLand(), miss, dodge, parry, glance, crit, hit);
    }
}
