// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Moonkin/ProcEffect.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.moonkin;

import rawr.base.character.Character;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

// The proc effect public class itself.
// NOTE: constructor with special effect allows efficient construction of the proc list in a loop.
public class ProcEffect {
    // Mutable float holder replacing the C# `ref float` parameters of Activate/Deactivate.
    public static final class StatRefs {
        public float spellPower;
        public float spellHit;
        public float spellCrit;
        public float spellHaste;
    }

    @FunctionalInterface
    public interface Activate {
        void apply(Character theChar, CharacterCalculationsMoonkin calcs, StatRefs stats);
    }

    @FunctionalInterface
    public interface Deactivate {
        void apply(Character theChar, CharacterCalculationsMoonkin calcs, StatRefs stats);
    }

    @FunctionalInterface
    public interface UpTime {
        float apply(SpellRotation rotation, CharacterCalculationsMoonkin calcs);
    }

    @FunctionalInterface
    public interface CalculateDPS {
        float apply(SpellRotation rotation, CharacterCalculationsMoonkin calcs, float spellPower, float spellHit, float spellCrit, float spellHaste);
    }

    @FunctionalInterface
    public interface CalculateMP5 {
        float apply(SpellRotation rotation, CharacterCalculationsMoonkin calcs, float spellPower, float spellHit, float spellCrit, float spellHaste);
    }

    public ProcEffect() {
    }

    public ProcEffect(SpecialEffect effect) {
        this.effect = effect;
        // Shadow damage procs - most widely varied at the current moment
        if (effect.getStats().getShadowDamage() > 0) {
            calculateDPS = (r, c, sp, sHi, sc, sHa) -> {
                SpecialEffect e = this.effect;
                float specialDamageModifier = (1 + c.basicStats.getBonusSpellPowerMultiplier()) * (1 + c.basicStats.getBonusShadowDamageMultiplier()) * (1 + c.basicStats.getBonusDamageMultiplier());
                float triggerInterval = 0.0f;
                switch (e.getTrigger()) {
                    case DoTTick:       // Extract
                        triggerInterval = r.duration / r.dotTicks;
                        break;
                    case SpellHit:      // Pendulum
                        triggerInterval = r.duration / r.castCount;
                        break;
                    case DamageDone:    // DMC: Death
                        triggerInterval = r.duration / (r.castCount + r.dotTicks);
                        break;
                    case DamageOrHealingDone:    // DMC: Greatness
                        // Need to add Self-Heals
                        triggerInterval = r.duration / (r.castCount + r.dotTicks);
                        break;
                    default:
                        return 0.0f;
                }
                float procsPerSecond = e.getAverageProcsPerSecond(triggerInterval, 1.0f, 3.0f, c.fightLength * 60.0f);
                return e.getStats().getShadowDamage() * specialDamageModifier * procsPerSecond;
            };
        }
        // Lightning Capacitor, Thunder Capacitor, Reign of the Unliving/Undead, Nibelung
        else if (effect.getStats().getNatureDamage() > 0 || effect.getStats().getFireDamage() > 0 || effect.getStats().getValkyrDamage() > 0) {
            if (effect.getStats().getNatureDamage() > 0) {
                calculateDPS = (r, c, sp, sHi, sc, sHa) -> {
                    float specialDamageModifier = (1 + c.basicStats.getBonusSpellPowerMultiplier()) * (1 + c.basicStats.getBonusNatureDamageMultiplier()) * (1 + c.basicStats.getBonusDamageMultiplier());
                    float procsPerSecond = effect.getAverageProcsPerSecond(r.duration / (r.castCount * sc), 1.0f, 3.0f, c.fightLength * 60.0f);
                    return effect.getStats().getNatureDamage() * specialDamageModifier * procsPerSecond;
                };
            } else if (effect.getStats().getFireDamage() > 0) {
                calculateDPS = (r, c, sp, sHi, sc, sHa) -> {
                    float specialDamageModifier = (1 + c.basicStats.getBonusSpellPowerMultiplier()) * (1 + c.basicStats.getBonusFireDamageMultiplier()) * (1 + c.basicStats.getBonusDamageMultiplier());
                    float procsPerSecond = effect.getAverageProcsPerSecond(r.duration / (r.castCount * sc), 1.0f, 3.0f, c.fightLength * 60.0f);
                    return effect.getStats().getFireDamage() * specialDamageModifier * procsPerSecond;
                };
            } else {
                calculateDPS = (r, c, sp, sHi, sc, sHa) -> {
                    float specialDamageModifier = (1 + c.basicStats.getBonusSpellPowerMultiplier()) * (1 + c.basicStats.getBonusDamageMultiplier());
                    float procsPerSecond = effect.getAverageProcsPerSecond(r.duration / (r.castCount * sc), 1.0f, 3.0f, c.fightLength * 60.0f);
                    return effect.getStats().getValkyrDamage() * specialDamageModifier * procsPerSecond;
                };
            }
        } else if (effect.getStats().getMp5() > 0) {
            calculateMP5 = (r, c, sp, sHi, sc, sHa) -> {
                SpecialEffect e = this.effect;
                float procsPerSecond = e.getAverageProcsPerSecond(r.duration / r.castCount, 1.0f, 3.0f, c.fightLength * 60f);
                return (e.getStats().getMp5() / 5.0f * e.getDuration()) * procsPerSecond * 5.0f;
            };
        }
        // Moonkin 4T8 set bonus (15% chance on IS tick to proc an instant-cast Starfire)
        else if (effect.getStats().getStarfireProc() == 1) {
            calculateDPS = (r, c, sp, sHi, sc, sHa) -> {
                if (r.insectSwarmTicks == 0) return 0.0f;
                Spell newSF = new Spell();
                newSF.allDamageModifier = r.solver.getStarfire().allDamageModifier;
                newSF.baseCastTime = 1.5f;
                newSF.baseDamage = r.solver.getStarfire().baseDamage;
                newSF.baseManaCost = r.solver.getStarfire().baseManaCost;
                newSF.criticalChanceModifier = r.solver.getStarfire().criticalChanceModifier;
                newSF.criticalDamageModifier = r.solver.getStarfire().criticalDamageModifier;
                newSF.dotEffect = null;
                newSF.idolExtraSpellPower = r.solver.getStarfire().idolExtraSpellPower;
                newSF.name = r.solver.getStarfire().name;
                newSF.school = r.solver.getStarfire().school;
                newSF.spellDamageModifier = r.solver.getStarfire().spellDamageModifier;
                r.doSpecialStarfire(c, newSF, sp, sHi, sc, sHa);
                float timeBetweenProcs = r.solver.getInsectSwarm().dotEffect.tickLength / effect.getChance();
                float replaceWrathWithSFDPS = (newSF.damagePerHit / newSF.castTime) - (r.solver.getWrath().damagePerHit / r.solver.getWrath().castTime);
                float replaceSFWithSFDPS = (newSF.damagePerHit / newSF.castTime) - (r.solver.getStarfire().damagePerHit / r.solver.getStarfire().castTime);
                return (replaceWrathWithSFDPS * (r.wrathCount / (r.wrathCount + r.starfireCount)) +
                        replaceSFWithSFDPS * (r.starfireCount / (r.wrathCount + r.starfireCount)))
                        / timeBetweenProcs;
            };
        } else if (effect.getStats().getRawSpecialEffectDataSize() == 0
                && (effect.getTrigger() == Trigger.DamageDone
                || effect.getTrigger() == Trigger.DamageOrHealingDone
                || effect.getTrigger() == Trigger.DamageSpellCast
                || effect.getTrigger() == Trigger.DamageSpellCrit
                || effect.getTrigger() == Trigger.DamageSpellHit
                || effect.getTrigger() == Trigger.SpellCast
                || effect.getTrigger() == Trigger.SpellCrit
                || effect.getTrigger() == Trigger.SpellHit
                || effect.getTrigger() == Trigger.SpellMiss
                || effect.getTrigger() == Trigger.Use
                || effect.getTrigger() == Trigger.MoonfireCast
                || effect.getTrigger() == Trigger.InsectSwarmOrMoonfireTick
                || effect.getTrigger() == Trigger.MoonfireTick
                || effect.getTrigger() == Trigger.InsectSwarmTick
                || effect.getTrigger() == Trigger.DoTTick)
                && (effect.getStats().getHasteRating() > 0
                || effect.getStats().getSpellHaste() > 0
                || effect.getStats().getCritRating() > 0
                || effect.getStats().getHighestStat() > 0)) {
            activate = (ch, c, stats) -> {
                int maxStack = effect.getMaxStack();
                Stats st = effect.getStats();
                float critRating = st.getCritRating();
                float spellCrit = StatConversion.getSpellCritFromRating(critRating * maxStack);
                float hasteRating = st.getHasteRating();
                float spellHaste = StatConversion.getSpellHasteFromRating(hasteRating * maxStack);
                spellHaste += st.getSpellHaste();
                float highestStat = st.getHighestStat();

                if (critRating > 0)
                    stats.spellCrit += spellCrit;
                if (spellHaste > 0)
                    stats.spellHaste += spellHaste;
                if (highestStat > 0) {
                    if (c.basicStats.getSpirit() > c.basicStats.getIntellect()) {
                        Stats s = c.basicStats.copy();
                        s.setSpirit(s.getSpirit() + highestStat);
                        CharacterCalculationsMoonkin cNew = CalculationsMoonkin.getInnerCharacterCalculations(ch, s, null);
                        storedStats.setSpellPower(cNew.spellPower - c.spellPower);
                        stats.spellPower += storedStats.getSpellPower();
                    } else {
                        Stats s = c.basicStats.copy();
                        s.setIntellect(s.getIntellect() + highestStat);
                        CharacterCalculationsMoonkin cNew = CalculationsMoonkin.getInnerCharacterCalculations(ch, s, null);
                        storedStats.setSpellPower(cNew.spellPower - c.spellPower);
                        storedStats.setSpellCrit(cNew.spellCrit - c.spellCrit);
                        stats.spellPower += storedStats.getSpellPower();
                        stats.spellCrit += storedStats.getSpellCrit();
                    }
                }
            };
            deactivate = (ch, c, stats) -> {
                int maxStack = effect.getMaxStack();
                Stats st = effect.getStats();
                float critRating = st.getCritRating();
                float spellCrit = StatConversion.getSpellCritFromRating(critRating * maxStack);
                float hasteRating = st.getHasteRating();
                float spellHaste = StatConversion.getSpellHasteFromRating(hasteRating * maxStack);
                spellHaste += st.getSpellHaste();
                float highestStat = st.getHighestStat();

                if (critRating > 0)
                    stats.spellCrit -= spellCrit;
                if (spellHaste > 0)
                    stats.spellHaste -= spellHaste;
                if (highestStat > 0) {
                    stats.spellPower -= storedStats.getSpellPower();
                    if (c.basicStats.getIntellect() >= c.basicStats.getSpirit())
                        stats.spellCrit -= storedStats.getSpellCrit();
                }
            };
            upTime = (r, c) -> {
                float upTime = 0.0f;
                switch (effect.getTrigger()) {
                    case Use:
                        upTime = effect.getAverageUptime(0f, 1f);
                        break;
                    case SpellHit:
                    case DamageSpellHit:
                        upTime = effect.getAverageUptime(r.duration / r.castCount, r.solver.getSpellHit(c));
                        break;
                    case DamageSpellCrit:
                    case SpellCrit:
                        upTime = effect.getAverageUptime(r.duration / (r.castCount - (r.insectSwarmTicks / r.solver.getInsectSwarm().dotEffect.getNumberOfTicks())), c.spellCrit);
                        break;
                    case SpellCast:
                    case DamageSpellCast:
                        upTime = effect.getAverageUptime(r.duration / r.castCount, 1f);
                        break;
                    case MoonfireCast:
                        upTime = effect.getAverageUptime(r.duration / r.moonfireCasts, 1f);
                        break;
                    case InsectSwarmOrMoonfireTick:
                        upTime = effect.getAverageUptime(r.duration / (r.insectSwarmTicks + r.moonfireTicks), 1f);
                        break;
                    case MoonfireTick:
                        upTime = effect.getAverageUptime(r.duration / r.moonfireTicks, 1f);
                        break;
                    case InsectSwarmTick:
                        upTime = effect.getAverageUptime(r.duration / r.insectSwarmTicks, 1f);
                        break;
                    case DoTTick:
                        upTime = effect.getAverageUptime(r.duration / (r.moonfireTicks + r.insectSwarmTicks), 1f);
                        break;
                    case DamageDone:
                    case DamageOrHealingDone:
                        upTime = effect.getAverageUptime(((r.duration / r.castCount) + (r.duration / (r.moonfireTicks + r.insectSwarmTicks))) / 2.0f, 1f);
                        break;
                    default:
                        break;
                }
                return upTime;
            };
        }
    }

    private Stats storedStats = new Stats();
    public SpecialEffect effect;
    public Activate activate;
    public Deactivate deactivate;
    public UpTime upTime;
    public CalculateDPS calculateDPS;
    public CalculateMP5 calculateMP5;
}
