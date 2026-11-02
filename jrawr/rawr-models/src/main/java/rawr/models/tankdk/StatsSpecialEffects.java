// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/StatsSpecialEffects.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

import rawr.base.character.Character;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

class StatsSpecialEffects {
    public Character character;
    public Stats stats;
    public CombatTable combatTable;

    public StatsSpecialEffects(Character c, Stats s, CombatTable t) {
        // It doesn't actually use the character or stats object being passed in.
        character = c;
        stats = s;
        combatTable = t;
    }

    public Stats getSpecialEffects(CalculationOptionsTankDK calcOpts, SpecialEffect effect) {
        Stats statsAverage = new Stats();
        Rotation rRotation = calcOpts.getMRotation();
        if (effect.getTrigger() == Trigger.Use) {
            if (calcOpts.getBUseOnUseAbilities() == true)
                statsAverage.accumulate(effect.getAverageStats());
        } else {
            float trigger = 0f;
            float chance = effect.getChance();
            float duration = effect.getDuration();
            float unhastedAttackSpeed = 2f;
            switch (effect.getTrigger()) {
                case MeleeCrit:
                case PhysicalCrit:
                    trigger = (1f / rRotation.getMeleeSpecialsPerSecond()) + (combatTable.combinedSwingTime != 0 ? 1f / combatTable.combinedSwingTime : 0.5f);
                    chance = combatTable.physCrits * effect.getChance();
                    unhastedAttackSpeed = (combatTable.MH != null ? combatTable.MH.baseSpeed : 2.0f);
                    break;
                case MeleeHit:
                case PhysicalHit:
                    trigger = (1f / (rRotation.getMeleeSpecialsPerSecond() * (combatTable.dw ? 2 : 1))) + (combatTable.combinedSwingTime != 0 ? 1f / combatTable.combinedSwingTime : 0.5f);
                    chance = effect.getChance() * (1f - (combatTable.missedSpecial + combatTable.dodgedSpecial));
                    unhastedAttackSpeed = (combatTable.MH != null ? combatTable.MH.baseSpeed : 2.0f);
                    break;
                case CurrentHandHit:
                case MainHandHit:
                    trigger = (1f / rRotation.getMeleeSpecialsPerSecond()) + (combatTable.MH.hastedSpeed != 0 ? 1f / combatTable.MH.hastedSpeed : 0.5f);
                    chance = effect.getChance() * (1f - (combatTable.missedSpecial + combatTable.dodgedSpecial));
                    unhastedAttackSpeed = (combatTable.MH != null ? combatTable.MH.baseSpeed : 2.0f);
                    break;
                case OffHandHit:
                    trigger = (1f / rRotation.getMeleeSpecialsPerSecond()) + (combatTable.OH.hastedSpeed != 0 ? 1f / combatTable.OH.hastedSpeed : 0.5f);
                    chance = effect.getChance() * (1f - (combatTable.missedSpecial + combatTable.dodgedSpecial));
                    unhastedAttackSpeed = (combatTable.MH != null ? combatTable.MH.baseSpeed : 2.0f);
                    break;
                case DamageDone:
                case DamageOrHealingDone:
                    trigger = (1f / rRotation.getMeleeSpecialsPerSecond()) + (combatTable.combinedSwingTime != 0 ? 1f / combatTable.combinedSwingTime : 0.5f);
                    unhastedAttackSpeed = (combatTable.MH != null ? combatTable.MH.baseSpeed : 2.0f);
                    chance = effect.getChance() * (1f - (combatTable.missedSpecial + combatTable.dodgedSpecial));
                    break;
                case DamageSpellCast:
                case SpellCast:
                case DamageSpellHit:
                case SpellHit:
                    trigger = 1f / rRotation.getSpellSpecialsPerSecond();
                    chance = 1f - combatTable.spellResist;
                    break;
                case DamageSpellCrit:
                case SpellCrit:
                    trigger = 1f / rRotation.getSpellSpecialsPerSecond();
                    chance = combatTable.spellCrits * effect.getChance();
                    break;
                case DoTTick:
                    trigger = (rRotation.BloodPlague + rRotation.FrostFever) / 3;
                    break;
                case DamageTaken:
                case DamageTakenPhysical:
                    trigger = calcOpts.getBossAttackSpeed();
                    chance *= 1f - (stats.getDodge() + stats.getParry() + stats.getMiss());
                    unhastedAttackSpeed = calcOpts.getBossAttackSpeed();
                    break;
                case DamageTakenMagical:
                    trigger = calcOpts.getIncomingFromMagicFrequency();
                    break;
                //////////////////////////////////
                // DK specific triggers:
                case BloodStrikeHit:
                case HeartStrikeHit:
                    trigger = rRotation.curRotationDuration / (rRotation.BloodStrike + rRotation.HeartStrike);
                    break;
                case PlagueStrikeHit:
                    trigger = rRotation.curRotationDuration / rRotation.PlagueStrike;
                    break;
                case RuneStrikeHit:
                    trigger = rRotation.curRotationDuration / rRotation.RuneStrike;
                    break;
                case IcyTouchHit:
                    trigger = rRotation.curRotationDuration / rRotation.IcyTouch;
                    break;
                case DeathStrikeHit:
                    trigger = rRotation.curRotationDuration / rRotation.DeathStrike;
                    break;
                case ObliterateHit:
                    trigger = rRotation.curRotationDuration / rRotation.Obliterate;
                    break;
                case ScourgeStrikeHit:
                    trigger = rRotation.curRotationDuration / rRotation.ScourgeStrike;
                    break;
                case FrostFeverHit:
                    // Icy Talons triggers off this.
                    trigger = rRotation.curRotationDuration / rRotation.IcyTouch;
                    if (character.getDeathKnightTalents().isGlyphofHowlingBlast())
                        trigger += rRotation.curRotationDuration / rRotation.HowlingBlast;
                    break;
                default:
                    break;
            }
            if (!Float.isInfinite(trigger) && !Float.isNaN(trigger)) {
                if (effect.usesPpm()) {
                    // If effect.chance < 0 , then it's using PPM.
                    // Let's get the duration * how many times it procs per min:
                    float UptimePerMin = 0;
                    float fWeight = 0;
                    if (duration == 0) // Duration of 0 means that it's a 1 time effect that procs every time the proc comes up.
                    {
                        fWeight = Math.abs(effect.getChance()) / 60;
                    } else {
                        UptimePerMin = duration * Math.abs(effect.getChance());
                        fWeight = UptimePerMin / 60;
                    }
                    statsAverage.accumulate(effect.getStats(), fWeight);
                } else {
                    effect.accumulateAverageStats(statsAverage, trigger, chance, unhastedAttackSpeed, calcOpts.getFightLength() * 60);
                }
            }
        }
        return statsAverage;
    }
}
