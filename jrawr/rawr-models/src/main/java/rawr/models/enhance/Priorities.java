// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Enhance/Priority.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.enhance;

import rawr.base.stats.BaseStats;
import rawr.base.character.Character;
import rawr.base.talents.ShamanTalents;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

import java.util.ArrayList;
import java.util.List;

class Priorities {
    private final CombatStats cs;
    private final CalculationOptionsEnhance calcOpts;
    private final Character character;
    private final Stats stats;
    private final ShamanTalents talents;
    private final List<Ability> abilities;
    private final float fightLength;

    public Priorities(CombatStats cs, CalculationOptionsEnhance calcOpts, Character character, Stats stats, ShamanTalents talents) {
        this.cs = cs;
        this.calcOpts = calcOpts;
        this.character = character;
        this.stats = stats;
        this.talents = talents;
        fightLength = calcOpts.getFightLength() * 60f;
        abilities = setupAbilities();
    }

    private List<Ability> setupAbilities() {
        List<Ability> abilities = new ArrayList<Ability>();
        float convection = 1f - talents.getConvection() * 0.02f;
        float baseMana = BaseStats.getBaseStats(character).getMana();
        float elementalFocus = (talents.getElementalFocus() == 1) ? .6f * cs.getChanceSpellCrit() : 1f;
        float ESMana = talents.getShamanisticFocus() == 1 ? baseMana * 0.55f * 0.18f : baseMana * 0.18f; // 45% reduction if Shamanistic Focus
        float FSMana = talents.getShamanisticFocus() == 1 ? baseMana * 0.55f * 0.17f : baseMana * 0.17f; // 45% reduction if Shamanistic Focus
        float fireElementalCD = talents.isGlyphofFireElementalTotem() ? 300f : 600f;
        float gcd = Math.max(1.0f, 1.5f / (1f + stats.getSpellHaste() + StatConversion.getSpellHasteFromRating(stats.getHasteRating())));

        int priority = calcOpts.getAbilityPriorityValue(EnhanceAbility.ShamanisticRage);
        if (priority > 0 && talents.getShamanisticRage() == 1)
            abilities.add(new Ability(EnhanceAbility.ShamanisticRage, 60f, 1.5f, 0f, priority, false, true));

        priority = calcOpts.getAbilityPriorityValue(EnhanceAbility.FeralSpirits);
        if (priority > 0 && talents.getFeralSpirit() == 1)
            abilities.add(new Ability(EnhanceAbility.FeralSpirits, 180f, gcd, 0.12f * baseMana, priority, false, false));

        priority = calcOpts.getAbilityPriorityValue(EnhanceAbility.LightningBolt);
        if (priority > 0 && talents.getMaelstromWeapon() > 0)
            abilities.add(new Ability(EnhanceAbility.LightningBolt, cs.getSecondsToFiveStack(), gcd, 0.1f * baseMana * convection * elementalFocus, priority, false, false));

        priority = calcOpts.getAbilityPriorityValue(EnhanceAbility.FlameShock);
        if (priority > 0)
            if (talents.isGlyphofShocking())
                abilities.add(new Ability(EnhanceAbility.FlameShock, cs.getAverageFSDotTime(), 1.0f, FSMana * convection * elementalFocus, priority, false, false));
            else
                abilities.add(new Ability(EnhanceAbility.FlameShock, cs.getAverageFSDotTime(), gcd, FSMana * convection * elementalFocus, priority, false, false));

        priority = calcOpts.getAbilityPriorityValue(EnhanceAbility.StormStrike);
        if (priority > 0 && talents.getStormstrike() == 1)
            abilities.add(new Ability(EnhanceAbility.StormStrike, 8f, 1.5f, 0.08f * baseMana, priority, false, true));

        priority = calcOpts.getAbilityPriorityValue(EnhanceAbility.EarthShock);
        if (priority > 0)
            if (talents.isGlyphofShocking())
                abilities.add(new Ability(EnhanceAbility.EarthShock, cs.getBaseShockSpeed(), 1.0f, ESMana * convection * elementalFocus, priority, false, false));
            else
                abilities.add(new Ability(EnhanceAbility.EarthShock, cs.getBaseShockSpeed(), gcd, ESMana * convection * elementalFocus, priority, false, false));

        priority = calcOpts.getAbilityPriorityValue(EnhanceAbility.LavaLash);
        if (priority > 0 && talents.getLavaLash() == 1)
            abilities.add(new Ability(EnhanceAbility.LavaLash, 6f, 1.5f, 0.04f * baseMana, priority, false, false));

        priority = calcOpts.getAbilityPriorityValue(EnhanceAbility.FireNova);
        if (priority > 0)
            abilities.add(new Ability(EnhanceAbility.FireNova, cs.getBaseFireNovaSpeed(), gcd, 0.22f * baseMana, priority, false, false));

        priority = calcOpts.getAbilityPriorityValue(EnhanceAbility.FireElemental);
        if (priority > 0 && calcOpts.getFireElemental())
            abilities.add(new Ability(EnhanceAbility.FireElemental, fireElementalCD, 1.0f, 0.23f * baseMana, priority, false, false));

        priority = calcOpts.getAbilityPriorityValue(EnhanceAbility.MagmaTotem);
        if (priority > 0 && calcOpts.getMagma())
            abilities.add(new Ability(EnhanceAbility.MagmaTotem, 20f, 1.0f, 0.27f * baseMana * elementalFocus, priority, false, false));

        priority = calcOpts.getAbilityPriorityValue(EnhanceAbility.SearingTotem);
        if (priority > 0 && !calcOpts.getMagma())
            abilities.add(new Ability(EnhanceAbility.SearingTotem, 60f, 1.0f, 0.07f * baseMana * elementalFocus, priority, false, false));

        priority = calcOpts.getAbilityPriorityValue(EnhanceAbility.LightningShield);
        if (priority > 0 && talents.getStaticShock() > 0)
            abilities.add(new Ability(EnhanceAbility.LightningShield, cs.getStaticShockAvDuration(), gcd, 0f, priority, true, false));

        priority = calcOpts.getAbilityPriorityValue(EnhanceAbility.RefreshTotems);
        if (priority > 0)
            abilities.add(new Ability(EnhanceAbility.RefreshTotems, 300f, 1.0f, 0.24f * baseMana, calcOpts.getAbilityPriorityValue(EnhanceAbility.RefreshTotems), true, false)); // patch 3.2 takes just 1 second GCD to refresh totems.

        abilities.sort(null);
        return abilities;
    }

    public void calculateAbilities() {
        float averageLag = calcOpts.getAverageLag() / 1000f;
        float reactionTime = (calcOpts.getReactionTime() / 1000f) - 0.25f; // the 0.25 seconds before spell comes off gcd allows spells added to queue.
        if (reactionTime < 0f)
            reactionTime = 0f;
        // first calculate base abilities uses
        float totalTimeUsed = 0f;
        float totalManaUsed = 0f;
        float uses = 0f;
        float shockTime = 0f;
        for (Ability ability : abilities) {
            if (ability.getAbilityType() == EnhanceAbility.EarthShock || ability.getAbilityType() == EnhanceAbility.FlameShock) {
                uses = (fightLength - shockTime) / (ability.getDuration() + averageLag);
                shockTime = uses * cs.getBaseShockSpeed();
            } else
                uses = fightLength / (ability.getDuration() + averageLag);
            ability.addUses(uses);
            totalTimeUsed += uses * (ability.getGcd() + reactionTime);
            totalManaUsed += uses * (ability.getManaCost() - ability.getGcd() * cs.getManaRegen());
            if (ability.getAbilityType() == EnhanceAbility.ShamanisticRage)
                totalManaUsed -= uses * cs.getMaxMana();
            if (ability.getAbilityType() == EnhanceAbility.StormStrike)
                totalManaUsed -= uses * cs.getImpStormStrikeMana();
        }
        // fight length & mana calcs causing huge issues - disabling for now
        // at this stage have the upper bounds of number of uses - ie: no GCD interference
        for (Ability ability : abilities) {
            ability.removePossibleClashingUses(0.075f, fightLength);
        }
    }

    public float abilityCooldown(EnhanceAbility abilityType) {
        for (Ability ability : abilities) {
            if (ability.getAbilityType() == abilityType)
                return ability.getUses() == 0 ? ability.getDuration() : fightLength / ability.getUses();
        }
        return fightLength;
    }

    public float getFireElementalUses() {
        for (Ability ability : abilities)
            if (ability.getAbilityType() == EnhanceAbility.FireElemental)
                return ability.getUses();
        return 0f;
    }
}
