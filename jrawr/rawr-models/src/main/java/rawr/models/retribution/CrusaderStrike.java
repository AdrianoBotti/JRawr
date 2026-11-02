// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Retribution/Skill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.retribution;

public class CrusaderStrike extends Skill {

    public CrusaderStrike(CombatStats combats) {
        super(combats, AbilityType.Melee, DamageType.Physical, true, true);
    }

    @Override
    public Ability getRotationAbility() {
        return Ability.CrusaderStrike;
    }

    @Override
    public float abilityDamage() {
        return (combats.normalWeaponDamage * .75f + stats.getCrusaderStrikeDamage())
                * (1f + .05f * talents.getSanctityOfBattle() + .05f * talents.getTheArtOfWar() + stats.getCrusaderStrikeMultiplier());
    }

    @Override
    public float abilityCritChance() {
        return stats.getCrusaderStrikeCrit();
    }
}
