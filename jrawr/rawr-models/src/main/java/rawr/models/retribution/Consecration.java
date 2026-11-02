// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class Consecration extends Skill {

    public Consecration(CombatStats combats) {
        super(combats, AbilityType.Spell, DamageType.Holy, false, false);
    }

    @Override
    public Ability getRotationAbility() {
        return Ability.Consecration;
    }

    @Override
    public float abilityDamage() {
        return (113f + .04f * (stats.getSpellPower() + stats.getConsecrationSpellPower()) + .04f * stats.getAttackPower())
                * tickCount() * (calcOpts.getConsEff());
    }

    @Override
    public float abilityCritChance() {
        return -1f; // -1 = can't crit.
    }

    @Override
    public float targets() {
        return calcOpts.getTargets();
    }

    @Override
    public float tickCount() {
        // Every second for 8 seconds (10 seconds with glyph)
        return talents.isGlyphOfConsecration() ? 10f : 8f;
    }
}
