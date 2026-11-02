// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Moonkin/Spell.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.moonkin;

public class DotEffect {
    public DotEffect() {
        allDamageModifier = 1.0f;
    }

    // C#: this.AllDamageModifier = copy.AllDamageModifier; this.BaseDamage = copy.BaseDamage;
    // (property setter, executed while this.NumberOfTicks is still 0/0 = NaN, then overwritten by
    // the TickDamage assignment below, exactly like the original) ...
    public DotEffect(DotEffect copy) {
        this.allDamageModifier = copy.allDamageModifier;
        setBaseDamage(copy.getBaseDamage());
        this.duration = copy.duration;
        this.tickDamage = copy.tickDamage;
        this.tickLength = copy.tickLength;
        this.spellDamageModifierPerTick = copy.spellDamageModifierPerTick;
    }

    public float duration;
    public float tickLength;
    public float tickDamage;
    public boolean canCrit;
    public float allDamageModifier;
    public float spellDamageModifierPerTick;

    public float getSpellDamageModifier() {
        return spellDamageModifierPerTick * getNumberOfTicks();
    }

    public void setSpellDamageModifier(float value) {
        spellDamageModifierPerTick += value / getNumberOfTicks();
    }

    public float getNumberOfTicks() {
        return duration / tickLength;
    }

    public float getBaseDamage() {
        return getNumberOfTicks() * tickDamage;
    }

    public void setBaseDamage(float value) {
        tickDamage += value / getNumberOfTicks();
    }

    // Section for variables which get filled in during rotation calcs
    public float damagePerHit;
}
