// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/Cycle.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.effects.SpecialEffect;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;
import rawr.base.stats.WeightedStat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Cycle {
    public String name;
    public CycleId cycleId;

    @Override
    public String toString() {
        return this.name;
    }

    public CastingState castingState;

    static class SpellData {
        public Spell spell;
        public float weight;
        public float dotUptime;
    }

    List<SpellData> spell;

    public Cycle() {
    }

    public Cycle(boolean needsDisplayCalculations, CastingState castingState) {
        this.castingState = castingState;
        if (needsDisplayCalculations) {
            spell = new ArrayList<SpellData>();
        }
    }

    public static Cycle createNew(boolean needsDisplayCalculations, CastingState castingState) {
        ArraySet arraySet = castingState.getSolver().getArraySet();
        if (needsDisplayCalculations || arraySet == null) {
            return new Cycle(needsDisplayCalculations, castingState);
        } else {
            Cycle cycle = arraySet.newCycle();
            cycle.initialize(castingState);
            return cycle;
        }
    }

    public void addCycle(boolean needsDisplayCalculations, Cycle cycle, float weight) {
        if (needsDisplayCalculations) {
            addSpellsFromCycle(cycle, weight);
        }
        this.castTime += weight * cycle.castTime;
        this.castProcs += weight * cycle.castProcs;
        this.castProcs2 += weight * cycle.castProcs2;
        this.nukeProcs += weight * cycle.nukeProcs;
        this.ticks += weight * cycle.ticks;
        this.hitProcs += weight * cycle.hitProcs;
        this.critProcs += weight * cycle.critProcs;
        this.igniteProcs += weight * cycle.igniteProcs;
        this.dotProcs += weight * cycle.dotProcs;
        this.targetProcs += weight * cycle.targetProcs;
        this.damageProcs += weight * cycle.damageProcs;
        this.absorbed += weight * cycle.absorbed;
        costPerSecond += weight * cycle.castTime * cycle.costPerSecond;
        damagePerSecond += weight * cycle.castTime * cycle.damagePerSecond;
        threatPerSecond += weight * cycle.castTime * cycle.threatPerSecond;
        this.dpsPerSpellPower += weight * cycle.castTime * cycle.dpsPerSpellPower;
    }

    private void addSpellsFromCycle(Cycle cycle, float weight) {
        for (SpellData spell : cycle.spell) {
            SpellData data = new SpellData();
            data.spell = spell.spell;
            data.dotUptime = spell.dotUptime;
            data.weight = weight * spell.weight;
            this.spell.add(data);
        }
    }

    public void addSpell(boolean needsDisplayCalculations, Spell spell, float weight) {
        if (needsDisplayCalculations) {
            SpellData data = new SpellData();
            data.spell = spell;
            data.weight = weight;
            this.spell.add(data);
        }
        this.castTime += weight * spell.castTime;
        this.castProcs += weight * spell.castProcs;
        this.castProcs2 += weight * spell.getCastProcs2();
        this.nukeProcs += weight * spell.getNukeProcs();
        this.ticks += weight * spell.ticks;
        this.hitProcs += weight * spell.hitProcs;
        this.critProcs += weight * spell.critProcs;
        this.igniteProcs += weight * spell.igniteProcs;
        this.dotProcs += weight * spell.dotProcs;
        this.targetProcs += weight * spell.targetProcs;
        this.damageProcs += weight * (spell.hitProcs + spell.dotProcs);
        this.absorbed += weight * spell.totalAbsorb;
        costPerSecond += weight * spell.averageCost;
        damagePerSecond += weight * spell.averageDamage;
        threatPerSecond += weight * spell.averageThreat;
        this.dpsPerSpellPower += weight * spell.damagePerSpellPower;
    }

    public void addSpell(boolean needsDisplayCalculations, Spell spell, float weight, float dotUptime) {
        if (needsDisplayCalculations) {
            SpellData data = new SpellData();
            data.spell = spell;
            data.weight = weight;
            data.dotUptime = dotUptime;
            this.spell.add(data);
        }
        this.castTime += weight * spell.castTime;
        this.castProcs += weight * spell.castProcs;
        this.castProcs2 += weight * spell.getCastProcs2();
        this.nukeProcs += weight * spell.getNukeProcs();
        this.ticks += weight * spell.ticks;
        this.hitProcs += weight * spell.hitProcs;
        this.critProcs += weight * spell.critProcs;
        this.igniteProcs += weight * spell.igniteProcs;
        this.dotProcs += weight * dotUptime * spell.dotProcs;
        this.targetProcs += weight * spell.targetProcs;
        this.damageProcs += weight * (spell.hitProcs + dotUptime * spell.dotProcs);
        this.absorbed += weight * spell.totalAbsorb;
        costPerSecond += weight * spell.averageCost;
        damagePerSecond += weight * (spell.averageDamage + dotUptime * spell.dotAverageDamage);
        threatPerSecond += weight * (spell.averageThreat + dotUptime * spell.dotAverageThreat);
        this.dpsPerSpellPower += weight * (spell.damagePerSpellPower + dotUptime * spell.dotDamagePerSpellPower);
    }

    public void addPause(float duration, float weight) {
        this.castTime += weight * duration;
    }

    public void calculate() {
        costPerSecond /= this.castTime;
        damagePerSecond /= this.castTime;
        threatPerSecond /= this.castTime;
        this.dpsPerSpellPower /= this.castTime;
    }

    public void addSpellContribution(Map<String, SpellContribution> dict, float duration, float effectSpellPower) {
        for (SpellData spell : this.spell) {
            spell.spell.addSpellContribution(dict, spell.weight * spell.spell.castTime / this.castTime * duration, spell.dotUptime, effectSpellPower);
        }
    }

    public void addManaUsageContribution(Map<String, Float> dict, float duration) {
        for (SpellData spell : this.spell) {
            spell.spell.addManaUsageContribution(dict, spell.weight * spell.spell.castTime / this.castTime * duration);
        }
    }

    public void initialize(CastingState castingState) {
        this.castingState = castingState;
        calculated = false;
        damagePerSecond = 0;
        effectDamagePerSecond = 0;
        effectSpellPower = 0;
        threatPerSecond = 0;
        effectThreatPerSecond = 0;
        costPerSecond = 0;
        manaRegenPerSecond = 0;
        this.dpsPerSpellPower = 0;
        this.absorbed = 0;

        this.providesSnare = false;
        this.providesScorch = false;

        this.areaEffect = false;
        this.aoeSpell = null;

        this.hitProcs = 0;
        this.ticks = 0;
        this.castProcs = 0;
        this.castProcs2 = 0;
        this.nukeProcs = 0;
        this.critProcs = 0;
        this.igniteProcs = 0;
        this.dotProcs = 0;
        this.castTime = 0;
        targetProcs = 0;
        damageProcs = 0;
        OO5SR = 0;
    }

    private boolean calculated;

    float damagePerSecond;
    float effectDamagePerSecond;
    float effectSpellPower;

    public float getDamagePerSecond() {
        calculateEffects();
        return damagePerSecond + effectDamagePerSecond;
    }

    public float getDamagePerSecond(float manaAdeptBonus) {
        calculateEffects();
        return damagePerSecond * (1 + manaAdeptBonus) + effectDamagePerSecond;
    }

    public float getSpellDamagePerSecond() {
        calculateEffects();
        return damagePerSecond;
    }

    float threatPerSecond;
    float effectThreatPerSecond;

    public float getThreatPerSecond() {
        calculateEffects();
        return threatPerSecond + effectThreatPerSecond;
    }

    float costPerSecond;

    public float getCostPerSecond() {
        calculateEffects();
        return costPerSecond;
    }

    private float manaRegenPerSecond;

    public float getManaRegenPerSecond() {
        calculateEffects();
        return manaRegenPerSecond;
    }

    public float getManaPerSecond() {
        return getCostPerSecond() - getManaRegenPerSecond();
    }

    public float absorbed;
    public float dpsPerSpellPower;

    public boolean providesSnare;
    public boolean providesScorch;

    public boolean areaEffect;
    public Spell aoeSpell;

    public float hitProcs;
    public float ticks;
    public float castProcs;
    public float castProcs2; // variant with only one proc from AM
    public float nukeProcs;
    public float critProcs;
    public float dotProcs;
    public float igniteProcs;
    public float castTime;
    public float targetProcs;
    public float damageProcs;
    public float OO5SR;

    public void addDamageContribution(Map<String, SpellContribution> dict, float duration) {
        addSpellContribution(dict, duration, effectSpellPower);
        addEffectContribution(dict, duration);
    }

    private void calculateEffects() {
        if (!calculated) {
            calculateIgniteDamageProcs();
            calculateManaRegen();
            calculateEffectDamage();
            calculated = true;
        }
    }

    private void calculateIgniteDamageProcs() {
        if (igniteProcs > 0) {
            double rate = igniteProcs / castTime;
            double k = Math.exp(-2 * rate);
            double ticks = k * (1 + k);
            damageProcs += (float) (igniteProcs * ticks);
            dotProcs += (float) (igniteProcs * ticks);
        }
    }

    private void calculateEffectDamage() {
        Stats baseStats = castingState.baseStats;
        float spellPower = 0;
        if (ticks > 0) {
            for (int i = 0; i < castingState.getSolver().getSpellPowerEffectsCount(); i++) {
                SpecialEffect effect = castingState.getSolver().getSpellPowerEffects()[i];
                switch (effect.getTrigger()) {
                    case DamageSpellCrit:
                    case SpellCrit:
                        spellPower += effect.getStats().getSpellPower() * effect.getAverageUptime(castTime / ticks, critProcs / ticks, 3, castingState.calculationOptions.getFightDuration());
                        break;
                    case DamageSpellHit:
                    case SpellHit:
                        spellPower += effect.getStats().getSpellPower() * effect.getAverageUptime(castTime / ticks, hitProcs / ticks, 3, castingState.calculationOptions.getFightDuration());
                        break;
                    case SpellMiss:
                        spellPower += effect.getStats().getSpellPower() * effect.getAverageUptime(castTime / ticks, 1 - hitProcs / ticks, 3, castingState.calculationOptions.getFightDuration());
                        break;
                    case DamageSpellCast:
                    case SpellCast:
                        if (castProcs > 0) {
                            spellPower += effect.getStats().getSpellPower() * effect.getAverageUptime(castTime / castProcs, 1, 3, castingState.calculationOptions.getFightDuration());
                        }
                        break;
                    case MageNukeCast:
                        if (nukeProcs > 0) {
                            spellPower += effect.getStats().getSpellPower() * effect.getAverageUptime(castTime / nukeProcs, 1, 3, castingState.calculationOptions.getFightDuration());
                        }
                        break;
                    case DamageDone:
                    case DamageOrHealingDone:
                        if (damageProcs > 0) {
                            spellPower += effect.getStats().getSpellPower() * effect.getAverageUptime(castTime / damageProcs, 1, 3, castingState.calculationOptions.getFightDuration());
                        }
                        break;
                    case DoTTick:
                        if (dotProcs > 0) {
                            spellPower += effect.getStats().getSpellPower() * effect.getAverageUptime(castTime / dotProcs, 1, 3, castingState.calculationOptions.getFightDuration());
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        if (castingState.mageTalents.getIncantersAbsorption() > 0) {
            spellPower += absorbed / castTime * 0.05f * castingState.mageTalents.getIncantersAbsorption() * 10;
        }
        effectSpellPower = spellPower;
        effectDamagePerSecond += spellPower * dpsPerSpellPower;
        if (castingState.waterElemental) {
            Spell waterbolt = castingState.getSpell(SpellId.Waterbolt);
            effectDamagePerSecond += (waterbolt.averageDamage + spellPower * waterbolt.damagePerSpellPower) / waterbolt.castTime;
        }
        if (castingState.mirrorImage) {
            Spell mirrorImage = castingState.getSpell(SpellId.MirrorImage);
            effectDamagePerSecond += (mirrorImage.averageDamage + spellPower * mirrorImage.damagePerSpellPower) / mirrorImage.castTime;
        }
        if (ticks > 0) {
            for (int i = 0; i < castingState.getSolver().getDamageProcEffectsCount(); i++) {
                SpecialEffect effect = castingState.getSolver().getDamageProcEffects()[i];
                float chance = 0;
                float interval = 0;
                switch (effect.getTrigger()) {
                    case SpellCrit:
                    case DamageSpellCrit:
                        chance = critProcs / ticks;
                        if (targetProcs > hitProcs) {
                            chance = 1f - (float) Math.pow(1 - chance, targetProcs / hitProcs);
                        }
                        interval = castTime / ticks;
                        break;
                    case SpellHit:
                    case DamageSpellHit:
                        chance = hitProcs / ticks;
                        if (targetProcs > hitProcs) {
                            chance = 1f - (float) Math.pow(1 - chance, targetProcs / hitProcs);
                        }
                        interval = castTime / ticks;
                        break;
                    case DamageDone:
                    case DamageOrHealingDone:
                        chance = 1;
                        interval = castTime / damageProcs;
                        break;
                    case DoTTick:
                        chance = 1;
                        interval = castTime / dotProcs;
                        break;
                    case SpellCast:
                    case DamageSpellCast:
                        chance = 1;
                        if (effect.getStats().getValkyrDamage() > 0) {
                            interval = castTime / castProcs2;
                        } else {
                            interval = castTime / castProcs;
                        }
                        break;
                    default:
                        break;
                }
                float effectsPerSecond = effect.getAverageProcsPerSecond(interval, chance, 3f, castingState.calculationOptions.getFightDuration());
                if (effect.getStats().getArcaneDamage() > 0) {
                    float boltDps = castingState.getArcaneAverageDamage() * effect.getStats().getArcaneDamage() * effectsPerSecond;
                    effectDamagePerSecond += boltDps;
                    effectThreatPerSecond += boltDps * castingState.getArcaneThreatMultiplier();
                }
                if (effect.getStats().getFireDamage() > 0) {
                    float boltDps = castingState.getFireAverageDamage() * effect.getStats().getFireDamage() * effectsPerSecond;
                    effectDamagePerSecond += boltDps;
                    effectThreatPerSecond += boltDps * castingState.getFireThreatMultiplier();
                }
                if (effect.getStats().getFrostDamage() > 0) {
                    float boltDps = castingState.getFrostAverageDamage() * effect.getStats().getFrostDamage() * effectsPerSecond;
                    effectDamagePerSecond += boltDps;
                    effectThreatPerSecond += boltDps * castingState.getFrostThreatMultiplier();
                }
                if (effect.getStats().getShadowDamage() > 0) {
                    float boltDps = castingState.getShadowAverageDamage() * effect.getStats().getShadowDamage() * effectsPerSecond;
                    effectDamagePerSecond += boltDps;
                    effectThreatPerSecond += boltDps * castingState.getShadowThreatMultiplier();
                }
                if (effect.getStats().getNatureDamage() > 0) {
                    float boltDps = castingState.getNatureAverageDamage() * effect.getStats().getNatureDamage() * effectsPerSecond;
                    effectDamagePerSecond += boltDps;
                    effectThreatPerSecond += boltDps * castingState.getNatureThreatMultiplier();
                }
                if (effect.getStats().getHolyDamage() > 0) {
                    float boltDps = castingState.getHolyAverageDamage() * effect.getStats().getHolyDamage() * effectsPerSecond;
                    effectDamagePerSecond += boltDps;
                    effectThreatPerSecond += boltDps * castingState.getHolyThreatMultiplier();
                }
                if (effect.getStats().getValkyrDamage() > 0) {
                    float boltDps = castingState.getValkyrAverageDamage() * effect.getStats().getValkyrDamage() * effectsPerSecond;
                    effectDamagePerSecond += boltDps;
                    effectThreatPerSecond += boltDps * castingState.getHolyThreatMultiplier();
                }
            }
        }
    }

    private void calculateManaRegen() {
        if (castingState.calculationOptions.getEffectDisableManaSources()) return;
        Stats baseStats = castingState.baseStats;
        manaRegenPerSecond = castingState.getManaRegen5SR() + OO5SR * (castingState.getManaRegen() - castingState.getManaRegen5SR());
        float fight = castingState.calculationOptions.getFightDuration();
        for (int i = 0; i < castingState.getSolver().getManaRestoreEffectsCount(); i++) {
            SpecialEffect effect = castingState.getSolver().getManaRestoreEffects()[i];
            switch (effect.getTrigger()) {
                case Use:
                    manaRegenPerSecond += effect.getStats().getManaRestore() * effect.getAverageProcsPerSecond(0, 1, 3, fight);
                    break;
                case DamageSpellCast:
                case SpellCast:
                    if (castProcs > 0) {
                        manaRegenPerSecond += effect.getStats().getManaRestore() * effect.getAverageProcsPerSecond(castTime / castProcs, 1, 3, fight);
                    }
                    break;
                case DamageSpellCrit:
                case SpellCrit:
                    if (ticks > 0) {
                        manaRegenPerSecond += effect.getStats().getManaRestore() * effect.getAverageProcsPerSecond(castTime / ticks, critProcs / ticks, 3, fight);
                    }
                    break;
                case DamageSpellHit:
                case SpellHit:
                    if (ticks > 0) {
                        manaRegenPerSecond += effect.getStats().getManaRestore() * effect.getAverageProcsPerSecond(castTime / ticks, hitProcs / ticks, 3, fight);
                    }
                    break;
                case DamageDone:
                case DamageOrHealingDone:
                    if (damageProcs > 0) {
                        manaRegenPerSecond += effect.getStats().getManaRestore() * effect.getAverageProcsPerSecond(castTime / damageProcs, 1, 3, fight);
                    }
                    break;
                case DoTTick:
                    if (dotProcs > 0) {
                        manaRegenPerSecond += effect.getStats().getManaRestore() * effect.getAverageProcsPerSecond(castTime / dotProcs, 1, 3, fight);
                    }
                    break;
                default:
                    break;
            }
        }
        for (int i = 0; i < castingState.getSolver().getMp5EffectsCount(); i++) {
            SpecialEffect effect = castingState.getSolver().getMp5Effects()[i];
            switch (effect.getTrigger()) {
                case Use:
                    manaRegenPerSecond += effect.getStats().getMp5() / 5f * effect.getAverageUptime(0f, 1f, 3, fight);
                    break;
                case DamageSpellCast:
                case SpellCast:
                    if (castProcs > 0) {
                        manaRegenPerSecond += effect.getStats().getMp5() / 5f * effect.getAverageUptime(castTime / castProcs, 1f, 3, fight);
                    }
                    break;
                case DamageSpellCrit:
                case SpellCrit:
                    if (ticks > 0) {
                        manaRegenPerSecond += effect.getStats().getMp5() / 5f * effect.getAverageUptime(castTime / ticks, critProcs / ticks, 3, fight);
                    }
                    break;
                case DamageSpellHit:
                case SpellHit:
                    if (ticks > 0) {
                        manaRegenPerSecond += effect.getStats().getMp5() / 5f * effect.getAverageUptime(castTime / ticks, hitProcs / ticks, 3, fight);
                    }
                    break;
                case DamageDone:
                case DamageOrHealingDone:
                    if (damageProcs > 0) {
                        manaRegenPerSecond += effect.getStats().getMp5() / 5f * effect.getAverageUptime(castTime / damageProcs, 1f, 3, fight);
                    }
                    break;
                case DoTTick:
                    if (dotProcs > 0) {
                        manaRegenPerSecond += effect.getStats().getMp5() / 5f * effect.getAverageUptime(castTime / dotProcs, 1f, 3, fight);
                    }
                    break;
                default:
                    break;
            }
        }
        if (igniteProcs > 0 && castingState.mageTalents.getEmpoweredFire() > 0) {
            double rate = igniteProcs / castTime;
            double k = Math.exp(-2 * rate);
            double ticks = k * (1 + k);
            double ticksPerSecond = rate * ticks;
            manaRegenPerSecond += 0.02f * 3268f * castingState.mageTalents.getEmpoweredFire() / 3.0f * (float) ticksPerSecond;
        }
    }

    public void addManaSourcesContribution(Map<String, Float> dict, float duration) {
        if (castingState.calculationOptions.getEffectDisableManaSources()) return;
        dict.put("Intellect/Spirit", dict.get("Intellect/Spirit") + duration * (castingState.getSpiritRegen() * castingState.baseStats.getSpellCombatManaRegeneration() + OO5SR * (castingState.getSpiritRegen() - castingState.getSpiritRegen() * castingState.baseStats.getSpellCombatManaRegeneration())));
        dict.put("MP5", dict.get("MP5") + duration * castingState.baseStats.getMp5() / 5f);
        dict.put("Innervate", dict.get("Innervate") + duration * (15732 * castingState.calculationOptions.getInnervate() / castingState.calculationOptions.getFightDuration()));
        dict.put("Mana Tide", dict.get("Mana Tide") + duration * castingState.calculationOptions.getManaTide() * 0.24f * castingState.baseStats.getMana() / castingState.calculationOptions.getFightDuration());
        dict.put("Replenishment", dict.get("Replenishment") + duration * castingState.baseStats.getManaRestoreFromMaxManaPerSecond() * castingState.baseStats.getMana());
        float fight = castingState.calculationOptions.getFightDuration();
        for (int i = 0; i < castingState.getSolver().getManaRestoreEffectsCount(); i++) {
            SpecialEffect effect = castingState.getSolver().getManaRestoreEffects()[i];
            switch (effect.getTrigger()) {
                case Use:
                    dict.put("Other", dict.get("Other") + duration * effect.getStats().getManaRestore() * effect.getAverageProcsPerSecond(0, 1, 3, fight));
                    break;
                case DamageSpellCast:
                case SpellCast:
                    if (castProcs > 0) {
                        dict.put("Other", dict.get("Other") + duration * effect.getStats().getManaRestore() * effect.getAverageProcsPerSecond(castTime / castProcs, 1, 3, fight));
                    }
                    break;
                case DamageSpellCrit:
                case SpellCrit:
                    if (ticks > 0) {
                        dict.put("Other", dict.get("Other") + duration * effect.getStats().getManaRestore() * effect.getAverageProcsPerSecond(castTime / ticks, critProcs / ticks, 3, fight));
                    }
                    break;
                case DamageSpellHit:
                case SpellHit:
                    if (ticks > 0) {
                        dict.put("Other", dict.get("Other") + duration * effect.getStats().getManaRestore() * effect.getAverageProcsPerSecond(castTime / ticks, hitProcs / ticks, 3, fight));
                    }
                    break;
                case DamageDone:
                case DamageOrHealingDone:
                    if (damageProcs > 0) {
                        dict.put("Other", dict.get("Other") + duration * effect.getStats().getManaRestore() * effect.getAverageProcsPerSecond(castTime / damageProcs, 1, 3, fight));
                    }
                    break;
                case DoTTick:
                    if (dotProcs > 0) {
                        dict.put("Other", dict.get("Other") + duration * effect.getStats().getManaRestore() * effect.getAverageProcsPerSecond(castTime / dotProcs, 1, 3, fight));
                    }
                    break;
                default:
                    break;
            }
        }
        for (int i = 0; i < castingState.getSolver().getMp5EffectsCount(); i++) {
            SpecialEffect effect = castingState.getSolver().getMp5Effects()[i];
            switch (effect.getTrigger()) {
                case Use:
                    dict.put("Other", dict.get("Other") + duration * effect.getStats().getMp5() / 5f * effect.getAverageUptime(0f, 1f, 3, castingState.calculationOptions.getFightDuration()));
                    break;
                case DamageSpellCast:
                case SpellCast:
                    if (castProcs > 0) {
                        dict.put("Other", dict.get("Other") + duration * effect.getStats().getMp5() / 5f * effect.getAverageUptime(castTime / castProcs, 1f, 3, castingState.calculationOptions.getFightDuration()));
                    }
                    break;
                case DamageSpellCrit:
                case SpellCrit:
                    if (ticks > 0) {
                        dict.put("Other", dict.get("Other") + duration * effect.getStats().getMp5() / 5f * effect.getAverageUptime(castTime / ticks, critProcs / ticks, 3, castingState.calculationOptions.getFightDuration()));
                    }
                    break;
                case DamageSpellHit:
                case SpellHit:
                    if (ticks > 0) {
                        dict.put("Other", dict.get("Other") + duration * effect.getStats().getMp5() / 5f * effect.getAverageUptime(castTime / ticks, hitProcs / ticks, 3, castingState.calculationOptions.getFightDuration()));
                    }
                    break;
                case DamageDone:
                case DamageOrHealingDone:
                    if (damageProcs > 0) {
                        dict.put("Other", dict.get("Other") + duration * effect.getStats().getMp5() / 5f * effect.getAverageUptime(castTime / damageProcs, 1f, 3, castingState.calculationOptions.getFightDuration()));
                    }
                    break;
                case DoTTick:
                    if (dotProcs > 0) {
                        dict.put("Other", dict.get("Other") + duration * effect.getStats().getMp5() / 5f * effect.getAverageUptime(castTime / dotProcs, 1f, 3, castingState.calculationOptions.getFightDuration()));
                    }
                    break;
                default:
                    break;
            }
        }
        if (igniteProcs > 0 && castingState.mageTalents.getEmpoweredFire() > 0) {
            double rate = igniteProcs / castTime;
            double k = Math.exp(-2 * rate);
            double ticks = k * (1 + k);
            double ticksPerSecond = rate * ticks;
            dict.put("Other", dict.get("Other") + duration * 0.02f * 3268f * castingState.mageTalents.getEmpoweredFire() / 3.0f * (float) ticksPerSecond);
        }
    }

    public void addEffectContribution(Map<String, SpellContribution> dict, float duration) {
        SpellContribution contrib;
        if (castingState.waterElemental) {
            Spell waterbolt = castingState.getSpell(SpellId.Waterbolt);
            contrib = dict.get(waterbolt.getName());
            if (contrib == null) {
                contrib = new SpellContribution();
                contrib.name = waterbolt.getName();
                dict.put(waterbolt.getName(), contrib);
            }
            contrib.hits += duration / waterbolt.castTime;
            contrib.damage += (waterbolt.averageDamage + effectSpellPower * waterbolt.damagePerSpellPower) / waterbolt.castTime * duration;
        }
        if (castingState.mirrorImage) {
            Spell mirrorImage = castingState.getSpell(SpellId.MirrorImage);
            contrib = dict.get("Mirror Image");
            if (contrib == null) {
                contrib = new SpellContribution();
                contrib.name = "Mirror Image";
                dict.put("Mirror Image", contrib);
            }
            contrib.hits += 3 * (castingState.mageTalents.isGlyphOfMirrorImage() ? 4 : 3) * duration / mirrorImage.castTime;
            contrib.damage += (mirrorImage.averageDamage + effectSpellPower * mirrorImage.damagePerSpellPower) / mirrorImage.castTime * duration;
        }
        if (ticks > 0) {
            for (int i = 0; i < castingState.getSolver().getDamageProcEffectsCount(); i++) {
                SpecialEffect effect = castingState.getSolver().getDamageProcEffects()[i];
                String name = null;
                float chance = 0;
                float interval = 0;
                switch (effect.getTrigger()) {
                    case SpellCrit:
                    case DamageSpellCrit:
                        chance = critProcs / ticks;
                        if (targetProcs > hitProcs) {
                            chance = 1f - (float) Math.pow(1 - chance, targetProcs / hitProcs);
                        }
                        interval = castTime / ticks;
                        break;
                    case SpellHit:
                    case DamageSpellHit:
                        chance = hitProcs / ticks;
                        if (targetProcs > hitProcs) {
                            chance = 1f - (float) Math.pow(1 - chance, targetProcs / hitProcs);
                        }
                        interval = castTime / ticks;
                        break;
                    case DamageDone:
                    case DamageOrHealingDone:
                        chance = 1;
                        interval = castTime / damageProcs;
                        break;
                    case DoTTick:
                        chance = 1;
                        interval = castTime / dotProcs;
                        break;
                    case SpellCast:
                    case DamageSpellCast:
                        chance = 1;
                        if (effect.getStats().getValkyrDamage() > 0) {
                            interval = castTime / castProcs2;
                        } else {
                            interval = castTime / castProcs;
                        }
                        break;
                    default:
                        break;
                }
                float effectsPerSecond = effect.getAverageProcsPerSecond(interval, chance, 3f, castingState.calculationOptions.getFightDuration());
                float boltDps = 0f;
                if (effect.getStats().getArcaneDamage() > 0) {
                    boltDps = castingState.getArcaneAverageDamage() * effect.getStats().getArcaneDamage() * effectsPerSecond;
                    name = "Arcane Damage Proc";
                }
                if (effect.getStats().getFireDamage() > 0) {
                    boltDps = castingState.getFireAverageDamage() * effect.getStats().getFireDamage() * effectsPerSecond;
                    name = "Fire Damage Proc";
                }
                if (effect.getStats().getFrostDamage() > 0) {
                    boltDps = castingState.getFrostAverageDamage() * effect.getStats().getFrostDamage() * effectsPerSecond;
                    name = "Frost Damage Proc";
                }
                if (effect.getStats().getShadowDamage() > 0) {
                    boltDps = castingState.getShadowAverageDamage() * effect.getStats().getShadowDamage() * effectsPerSecond;
                    name = "Shadow Damage Proc";
                }
                if (effect.getStats().getNatureDamage() > 0) {
                    boltDps = castingState.getNatureAverageDamage() * effect.getStats().getNatureDamage() * effectsPerSecond;
                    name = "Nature Damage Proc";
                }
                if (effect.getStats().getHolyDamage() > 0) {
                    boltDps = castingState.getHolyAverageDamage() * effect.getStats().getHolyDamage() * effectsPerSecond;
                    name = "Holy Damage Proc";
                }
                if (effect.getStats().getValkyrDamage() > 0) {
                    boltDps = castingState.getValkyrAverageDamage() * effect.getStats().getValkyrDamage() * effectsPerSecond;
                    name = "Val'kyr Damage";
                }
                contrib = dict.get(name);
                if (contrib == null) {
                    contrib = new SpellContribution();
                    contrib.name = name;
                    dict.put(name, contrib);
                }
                contrib.hits += effectsPerSecond * duration;
                contrib.damage += boltDps * duration;
            }
        }
        if (igniteProcs > 0 && dict.get("Ignite") != null) {
            contrib = dict.get("Ignite");
            double rate = igniteProcs / castTime;
            double k = Math.exp(-2 * rate);
            double ticks = k * (1 + k);
            double ticksPerSecond = rate * ticks;
            contrib.hits += duration * (float) ticksPerSecond;
        }
    }
}
