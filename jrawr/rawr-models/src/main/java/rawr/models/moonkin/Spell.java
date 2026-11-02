// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Moonkin/Spell.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.moonkin;

public class Spell {
    public Spell() {
        allDamageModifier = 1.0f;
        criticalDamageModifier = 1.0f;
    }

    // C# copy constructor. Note it deliberately does not copy the rotation-scoped members
    // (DamagePerHit/CastTime/NGCastTime/ManaCost) nor DotEffect.CanCrit, exactly like the original.
    public Spell(Spell copy) {
        this.name = copy.name;
        this.school = copy.school == SpellSchool.Arcane ? SpellSchool.Arcane : SpellSchool.Nature;
        this.baseCastTime = copy.baseCastTime;
        this.baseDamage = copy.baseDamage;
        this.allDamageModifier = copy.allDamageModifier;
        this.baseManaCost = copy.baseManaCost;
        this.criticalChanceModifier = copy.criticalChanceModifier;
        this.criticalDamageModifier = copy.criticalDamageModifier;
        this.dotEffect = copy.dotEffect == null ? null : new DotEffect(copy.dotEffect);
        this.idolExtraSpellPower = copy.idolExtraSpellPower;
        this.spellDamageModifier = copy.spellDamageModifier;
    }

    public String name;
    public float baseDamage;
    public SpellSchool school;
    public float spellDamageModifier;
    public float allDamageModifier;
    public float idolExtraSpellPower;
    public float criticalDamageModifier;
    public float criticalChanceModifier;
    public float baseCastTime;
    public float baseManaCost;
    public DotEffect dotEffect;
    // Section for variables which get filled in during rotation calcs
    public float damagePerHit;
    public float castTime;
    public float ngCastTime;
    public float manaCost;
}
