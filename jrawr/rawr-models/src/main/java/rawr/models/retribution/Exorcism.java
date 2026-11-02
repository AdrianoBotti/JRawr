// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class Exorcism extends Skill {

    public Exorcism(CombatStats combats) {
        super(combats, AbilityType.Spell, DamageType.Holy, false, false);
    }

    @Override
    public Ability getRotationAbility() {
        return Ability.Exorcism;
    }

    @Override
    public float abilityDamage() {
        return (1087f + .42f * stats.getSpellPower())
                * (1f + .05f * talents.getSanctityOfBattle() + stats.getExorcismMultiplier() + (talents.isGlyphOfExorcism() ? 0.2f : 0f));
    }

    @Override
    public float abilityCritChance() {
        return (calcOpts.getMob() == MobType.Demon || calcOpts.getMob() == MobType.Undead) ? 1f : 0;
    }
}
