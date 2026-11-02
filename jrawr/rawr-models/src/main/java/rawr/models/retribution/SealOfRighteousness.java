// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class SealOfRighteousness extends Skill {

    public SealOfRighteousness(CombatStats combats) {
        super(combats, AbilityType.Spell, DamageType.Holy, true, false);
    }

    @Override
    public float abilityDamage() {
        return combats.baseWeaponSpeed * (0.022f * stats.getAttackPower() + 0.044f * stats.getSpellPower()) * (1f
                + .03f * talents.getSealsOfThePure()
                + (talents.isGlyphOfSealOfRighteousness() ? 0.1f : 0f)
                + stats.getBonusSealOfRighteousnessDamageMultiplier())
                + stats.getSealMultiplier();
    }

    @Override
    public float abilityCritChance() {
        return -1f;
    }

    @Override
    public float chanceToLand() {
        return 1f;
    }
}
