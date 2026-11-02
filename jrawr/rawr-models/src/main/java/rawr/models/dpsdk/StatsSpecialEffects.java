// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/StatsSpecialEffects.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

public class StatsSpecialEffects {
    public Character character;
    public Stats stats;
    public CombatTable combatTable;

    public StatsSpecialEffects(Character c, Stats s, CombatTable t) {
        character = c;
        stats = s;
        combatTable = t;
    }

    public Stats getSpecialEffects(CalculationOptionsDPSDK calcOpts, SpecialEffect effect) {
        Stats statsAverage = new Stats();
        Rotation rotation = calcOpts.getRotation();
        if (effect.getTrigger() == Trigger.Use) {
            effect.accumulateAverageStats(statsAverage, 0f, 1f, 3f, 0f);
            for (SpecialEffect e : effect.getStats().specialEffects()) {
                statsAverage.accumulate(this.getSpecialEffects(calcOpts, e), (effect.getDuration() / effect.getCooldown()));
            }
        } else {
            double trigger = 0f;
            float chance = 0f;
            float unhastedAttackSpeed = 2f;
            switch (effect.getTrigger()) {
                case MeleeCrit:
                case PhysicalCrit:
                    trigger = (1f / ((rotation.getMeleeSpecialsPerSecond() * (combatTable.DW ? 2f : 1f)) + (combatTable.combinedSwingTime != 0 ? 1f / combatTable.combinedSwingTime : 0.5f)));
                    chance = combatTable.physCrits;
                    unhastedAttackSpeed = (combatTable.MH != null ? combatTable.MH.baseSpeed : 2.0f);
                    break;
                case MeleeHit:
                case PhysicalHit:
                    trigger = (1f / ((rotation.getMeleeSpecialsPerSecond() * (combatTable.DW ? 2f : 1f)) + (combatTable.combinedSwingTime != 0 ? 1f / combatTable.combinedSwingTime : 0.5f)));
                    chance = 1f - (combatTable.missedSpecial + combatTable.dodgedSpecial) * (1f - combatTable.totalMHMiss);
                    unhastedAttackSpeed = (combatTable.MH != null ? combatTable.MH.baseSpeed : 2.0f);
                    break;
                case CurrentHandHit:
                    trigger = (1f / ((rotation.getMeleeSpecialsPerSecond()) + (combatTable.combinedSwingTime != 0 ? 1f / combatTable.combinedSwingTime : 0.5f)));
                    chance = 1f - (combatTable.missedSpecial + combatTable.dodgedSpecial) * (1f - combatTable.totalMHMiss);
                    // TODO: need to know if this is MH or OH.
                    unhastedAttackSpeed = (combatTable.MH != null ? combatTable.MH.baseSpeed : 2.0f);
                    break;
                case MainHandHit:
                    trigger = (1f / ((rotation.getMeleeSpecialsPerSecond()) + (combatTable.combinedSwingTime != 0 ? 1f / combatTable.combinedSwingTime : 0.5f)));
                    chance = 1f - (combatTable.missedSpecial + combatTable.dodgedSpecial) * (1f - combatTable.totalMHMiss);
                    unhastedAttackSpeed = (combatTable.MH != null ? combatTable.MH.baseSpeed : 2.0f);
                    break;
                case OffHandHit:
                    trigger = (1f / ((rotation.getMeleeSpecialsPerSecond()) + (combatTable.combinedSwingTime != 0 ? 1f / combatTable.combinedSwingTime : 0.5f)));
                    chance = 1f - (combatTable.missedSpecial + combatTable.dodgedSpecial) * (1f - combatTable.totalMHMiss);
                    unhastedAttackSpeed = (combatTable.OH != null ? combatTable.OH.baseSpeed : 2.0f);
                    break;
                case DamageDone:
                    trigger = 1f / (((rotation.getMeleeSpecialsPerSecond() * (combatTable.DW ? 2f : 1f)) + rotation.getSpellSpecialsPerSecond()) + (combatTable.combinedSwingTime != 0 ? 1f / combatTable.combinedSwingTime : 0.5f));
                    chance = (1f - (combatTable.missedSpecial + combatTable.dodgedSpecial)) * (1f - combatTable.totalMHMiss);
                    unhastedAttackSpeed = (combatTable.MH != null ? combatTable.MH.baseSpeed : 2.0f);
                    break;
                case DamageOrHealingDone:
                    // Need to add Self Healing
                    trigger = 1f / (((rotation.getMeleeSpecialsPerSecond() * (combatTable.DW ? 2f : 1f)) + rotation.getSpellSpecialsPerSecond()) + (combatTable.combinedSwingTime != 0 ? 1f / combatTable.combinedSwingTime : 0.5f));
                    chance = (1f - (combatTable.missedSpecial + combatTable.dodgedSpecial)) * (1f - combatTable.totalMHMiss);
                    unhastedAttackSpeed = (combatTable.MH != null ? combatTable.MH.baseSpeed : 2.0f);
                    break;
                case DamageSpellCast:
                case SpellCast:
                case DamageSpellHit:
                case SpellHit:
                    trigger = 1f / rotation.getSpellSpecialsPerSecond();
                    chance = 1f - combatTable.spellResist;
                    break;
                case DamageSpellCrit:
                case SpellCrit:
                    trigger = 1f / rotation.getSpellSpecialsPerSecond();
                    chance = combatTable.spellCrits;
                    break;
                case BloodStrikeHit:
                    trigger = rotation.getCurRotationDuration() / (rotation.getBloodStrike() * (combatTable.DW ? 2f : 1f));
                    chance = 1f;
                    break;
                case HeartStrikeHit:
                    trigger = rotation.getCurRotationDuration() / rotation.getHeartStrike();
                    chance = 1f;
                    break;
                case BloodStrikeOrHeartStrikeHit:
                    trigger = rotation.getCurRotationDuration() / ((rotation.getBloodStrike() + rotation.getHeartStrike()) * (combatTable.DW ? 2f : 1f));
                    chance = 1f;
                    break;
                case ObliterateHit:
                    trigger = rotation.getCurRotationDuration() / (rotation.getObliterate() * (combatTable.DW ? 2f : 1f));
                    chance = 1f;
                    break;
                case ScourgeStrikeHit:
                    trigger = rotation.getCurRotationDuration() / rotation.getScourgeStrike();
                    chance = 1f;
                    break;
                case DeathStrikeHit:
                    trigger = rotation.getCurRotationDuration() / rotation.getDeathStrike();
                    chance = 1f;
                    break;
                case PlagueStrikeHit:
                    trigger = rotation.getCurRotationDuration() / (rotation.getPlagueStrike() * (combatTable.DW ? 2f : 1f));
                    chance = 1f;
                    break;
                case DoTTick:
                    trigger = (rotation.getBloodPlague() + rotation.getFrostFever()) / 3;
                    chance = 1f;
                    break;
                default:
                    break;
            }

            if (effect.getMaxStack() > 1) {
                float timeToMax = (float) Math.min(calcOpts.getFightLength() * 60, effect.getChance(unhastedAttackSpeed) * trigger * effect.getMaxStack());
                float buffDuration = calcOpts.getFightLength() * 60f;
                if (effect.getStats().getAttackPower() == 250f || effect.getStats().getAttackPower() == 215f || effect.getStats().getHasteRating() == 57f || effect.getStats().getHasteRating() == 64f) {
                    buffDuration = 20f;
                }
                if (timeToMax * .5f > buffDuration) {
                    timeToMax = 2 * buffDuration;
                }
                statsAverage.accumulate(effect.getStats(), effect.getAverageStackSize((float) trigger, chance, unhastedAttackSpeed, buffDuration));
            } else {
                effect.accumulateAverageStats(statsAverage, (float) trigger, chance, unhastedAttackSpeed, calcOpts.getFightLength() * 60);
            }
        }

        return statsAverage;
    }
}
