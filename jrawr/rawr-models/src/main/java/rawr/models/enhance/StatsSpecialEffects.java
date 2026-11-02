// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Enhance/StatsSpecialEffects.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.enhance;

import rawr.base.character.Character;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.ItemInstance;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

class StatsSpecialEffects {
    private final Character character;
    private final Stats stats;
    private final CombatStats cs;
    private float trigger = 0f;
    private float chance = 1f;
    private float unhastedAttackSpeed = 3f;
    private final SpecialEffect mainHandEnchant;
    private SpecialEffect offHandEnchant = null;
    private boolean mhProcessed = false;
    private boolean ohProcessed = false;

    public StatsSpecialEffects(Character character, Stats stats, CalculationOptionsEnhance calcOpts) {
        this.character = character;
        this.stats = stats;
        cs = new CombatStats(character, stats, calcOpts);
        ItemInstance mainHand = character.get(CharacterSlot.MainHand);
        Enchant mhEnchant = mainHand == null ? null : mainHand.getEnchant();
        if (mhEnchant != null) {
            Stats.SpecialEffectEnumerator mhEffects = mhEnchant.stats.specialEffects();
            if (mhEffects.hasNext()) {
                mainHandEnchant = mhEffects.next();
            } else {
                mainHandEnchant = null;
            }
        } else {
            mainHandEnchant = null;
        }
        if (character.getShamanTalents().getDualWield() == 1) {
            ItemInstance offHand = character.get(CharacterSlot.OffHand);
            Enchant ohEnchant = offHand == null ? null : offHand.getEnchant();
            if (ohEnchant != null) {
                Stats.SpecialEffectEnumerator ohEffects = ohEnchant.stats.specialEffects();
                if (ohEffects.hasNext()) {
                    offHandEnchant = ohEffects.next();
                }
            }
        }
    }

    public Stats getSpecialEffects() {
        Stats statsAverage = new Stats();
        for (SpecialEffect effect : stats.specialEffects()) {
            statsAverage.accumulate(getSpecialEffects(effect));
        }
        addParagon(statsAverage);
        addHighestStat(statsAverage);
        addDeathBringerProc(statsAverage);
        return statsAverage;
    }

    public Stats getSpecialEffects(SpecialEffect effect) {
        Stats statsAverage = new Stats();
        if (effect == mainHandEnchant || effect == offHandEnchant) {
            if (mainHandEnchant != null && !mhProcessed) {
                statsAverage.accumulate(mainHandEnchant.getStats(), getMHUptime());
                mhProcessed = true;
            } else if (offHandEnchant != null && !ohProcessed) {
                statsAverage.accumulate(offHandEnchant.getStats(), getOHUptime());
                ohProcessed = true;
            }
        } else if (effect.getTrigger() == Trigger.Use) {
            effect.accumulateAverageStats(statsAverage, 0f, 1f, 3f, 0f);
            for (SpecialEffect e : effect.getStats().specialEffects())
                statsAverage.accumulate(Stats.multiply(getSpecialEffects(e), (effect.getDuration() / effect.getCooldown())));
        } else {
            setTriggerChanceAndSpeed(effect);
            for (SpecialEffect e : effect.getStats().specialEffects()) { // deal with secondary effects
                statsAverage.accumulate(getSpecialEffects(e));
            }
            if (effect.getMaxStack() > 1) {
                if (effect.getStats().getMoteOfAnger() > 0) {
                    // When in effect stats, MoteOfAnger is % of melee hits
                    // When in character stats, MoteOfAnger is average procs per second
                    Stats moteStats = new Stats();
                    moteStats.setMoteOfAnger(effect.getStats().getMoteOfAnger() * effect.getAverageProcsPerSecond(trigger, chance, unhastedAttackSpeed, 0f) / effect.getMaxStack());
                    statsAverage.accumulate(moteStats);
                } else {
                    float timeToMax = (float) Math.min(cs.getFightLength(), effect.getChance(unhastedAttackSpeed) * trigger * effect.getMaxStack());
                    float buffDuration = cs.getFightLength();
                    if (effect.getStats().getAttackPower() == 250f || effect.getStats().getAttackPower() == 215f || effect.getStats().getHasteRating() == 57f || effect.getStats().getHasteRating() == 64f) {
                        buffDuration = 20f;
                    }
                    if (timeToMax * .5f > buffDuration) {
                        timeToMax = 2 * buffDuration;
                    }
                    statsAverage.accumulate(Stats.multiply(effect.getStats(), (effect.getMaxStack() * (((buffDuration) - .5f * timeToMax) / (buffDuration)))));
                }
            } else {
                effect.accumulateAverageStats(statsAverage, trigger, chance, unhastedAttackSpeed, 0f);
            }
        }
        return statsAverage;
    }

    private void setTriggerChanceAndSpeed(SpecialEffect effect) {
        trigger = 0f;
        chance = 1f;
        unhastedAttackSpeed = 3f;
        switch (effect.getTrigger()) {
            case DamageDone:
                trigger = (cs.getHastedMHSpeed() + 1f / cs.getSpellAttacksPerSec()) / 2f;
                chance = (float) Math.min(1.0f, cs.getAverageWhiteHitChance() + cs.getChanceSpellHit()); // limit to 100% chance
                unhastedAttackSpeed = cs.getUnhastedMHSpeed();
                break;
            case DamageOrHealingDone:
                // Need to add Self Heals
                trigger = (cs.getHastedMHSpeed() + 1f / cs.getSpellAttacksPerSec()) / 2f;
                chance = (float) Math.min(1.0f, cs.getAverageWhiteHitChance() + cs.getChanceSpellHit()); // limit to 100% chance
                unhastedAttackSpeed = cs.getUnhastedMHSpeed();
                break;
            case MeleeCrit:
            case PhysicalCrit:
                trigger = cs.getHastedMHSpeed();
                chance = cs.getAverageWhiteCritChance();
                unhastedAttackSpeed = cs.getUnhastedMHSpeed();
                break;
            case MeleeHit:
            case PhysicalHit:
                trigger = cs.getHastedMHSpeed();
                chance = cs.getAverageWhiteHitChance();
                unhastedAttackSpeed = cs.getUnhastedMHSpeed();
                break;
            case MeleeAttack:
                if (character.getShamanTalents().getDualWield() == 1) {
                    trigger = (cs.getHastedMHSpeed() + cs.getHastedOHSpeed()) / 2;
                    chance = 1f;
                    unhastedAttackSpeed = (cs.getUnhastedMHSpeed() + cs.getUnhastedOHSpeed()) / 2;
                } else {
                    trigger = cs.getHastedMHSpeed();
                    chance = 1f;
                    unhastedAttackSpeed = cs.getUnhastedMHSpeed();
                }
                break;
            case DamageSpellCast:
            case SpellCast:
                trigger = 1f / cs.getSpellCastsPerSec();
                chance = 1f;
                break;
            case DamageSpellHit:
            case SpellHit:
                trigger = 1f / cs.getSpellAttacksPerSec();
                chance = cs.getChanceSpellHit();
                break;
            case DamageSpellCrit:
            case SpellCrit:
                trigger = 1f / cs.getSpellCritsPerSec();
                chance = cs.getChanceSpellCrit();
                break;
            case SpellMiss:
                trigger = 1f / cs.getSpellMissesPerSec();
                chance = 1 - cs.getChanceSpellHit();
                break;
            case ShamanLightningBolt:
                trigger = 1f / cs.abilityCooldown(EnhanceAbility.LightningBolt);
                chance = cs.getChanceSpellHit();
                break;
            case ShamanStormStrike:
                trigger = 1f / cs.abilityCooldown(EnhanceAbility.StormStrike);
                chance = cs.getChanceYellowHitMH();
                break;
            case ShamanShock:
                trigger = 1f / cs.abilityCooldown(EnhanceAbility.EarthShock);
                chance = cs.getChanceSpellHit();
                break;
            case ShamanLavaLash:
                trigger = 1f / cs.abilityCooldown(EnhanceAbility.LavaLash);
                chance = cs.getChanceYellowHitOH();
                unhastedAttackSpeed = cs.getUnhastedOHSpeed();
                break;
            case ShamanShamanisticRage:
                trigger = 1f / cs.abilityCooldown(EnhanceAbility.ShamanisticRage);
                chance = 1f;
                break;
            case ShamanFlameShockDoTTick:
            case DoTTick:
                trigger = 1f / cs.abilityCooldown(EnhanceAbility.FlameShock);
                chance = 1f;
                break;
            default:
                break;
        }
    }

    public float getUptime(ItemInstance item) {
        float uptime = 0;
        if (item != null)
            for (SpecialEffect effect : item.getTotalStats().specialEffects()) {
                setTriggerChanceAndSpeed(effect);
                uptime = effect.getAverageUptime(trigger, chance, unhastedAttackSpeed);
            }
        return uptime;
    }

    public float getMHUptime() {
        if (mainHandEnchant != null && mainHandEnchant.getTrigger() == Trigger.SpellHit)
            return mainHandEnchant.getAverageUptime(1f / cs.getSpellAttacksPerSec(), cs.getChanceSpellHit(), cs.getUnhastedMHSpeed(), cs.getFightLength());
        return mainHandEnchant == null ? 0f : mainHandEnchant.getAverageUptime(cs.getHastedMHSpeed(), cs.getChanceWhiteHitMH(), cs.getUnhastedMHSpeed(), cs.getFightLength());
    }

    public float getOHUptime() {
        if (offHandEnchant != null && offHandEnchant.getTrigger() == Trigger.SpellHit)
            return offHandEnchant.getAverageUptime(1f / cs.getSpellAttacksPerSec(), cs.getChanceSpellHit(), cs.getUnhastedOHSpeed(), cs.getFightLength());
        return offHandEnchant == null ? 0f : offHandEnchant.getAverageUptime(cs.getHastedOHSpeed(), cs.getChanceWhiteHitOH(), cs.getUnhastedOHSpeed(), cs.getFightLength());
    }

    // Handling for Paragon trinket procs
    private void addParagon(Stats statsAverage) {
        if (statsAverage.getParagon() > 0) {
            float paragon;
            if (stats.getStrength() > stats.getAgility()) {
                paragon = statsAverage.getParagon() * (1 + stats.getBonusStrengthMultiplier());
                statsAverage.setStrength(statsAverage.getStrength() + paragon);
                statsAverage.setAttackPower(statsAverage.getAttackPower() + paragon);
            } else {
                paragon = statsAverage.getParagon() * (1 + stats.getBonusAgilityMultiplier());
                statsAverage.setAgility(statsAverage.getAgility() + paragon);
                statsAverage.setAttackPower(statsAverage.getAttackPower() + paragon);
            }
        }
    }

    private void addHighestStat(Stats statsAverage) {
        //trinket procs
        if (statsAverage.getHighestStat() > 0) {
            float intfromAP = character.getShamanTalents().getMentalDexterity() / 3;
            // Highest stat
            float highestStat;
            if (stats.getAgility() > stats.getStrength()) {
                if (stats.getAgility() > stats.getIntellect()) {
                    highestStat = statsAverage.getHighestStat() * (1 + stats.getBonusAgilityMultiplier());
                    statsAverage.setAgility(statsAverage.getAgility() + highestStat);
                    statsAverage.setAttackPower(statsAverage.getAttackPower() + highestStat);
                } else {
                    highestStat = statsAverage.getHighestStat() * (1 + stats.getBonusIntellectMultiplier());
                    statsAverage.setIntellect(statsAverage.getIntellect() + highestStat);
                    statsAverage.setAttackPower(statsAverage.getAttackPower() + intfromAP * highestStat);
                }
            } else {
                if (stats.getStrength() > stats.getIntellect()) {
                    highestStat = statsAverage.getHighestStat() * (1 + stats.getBonusStrengthMultiplier());
                    statsAverage.setStrength(statsAverage.getStrength() + highestStat);
                    statsAverage.setAttackPower(statsAverage.getAttackPower() + highestStat);
                } else {
                    highestStat = statsAverage.getHighestStat() * (1 + stats.getBonusIntellectMultiplier());
                    statsAverage.setIntellect(statsAverage.getIntellect() + highestStat);
                    statsAverage.setAttackPower(statsAverage.getAttackPower() + intfromAP * highestStat);
                }
            }
        }
    }

    private void addDeathBringerProc(Stats statsAverage) {
        if (statsAverage.getDeathbringerProc() > 0) {
            statsAverage.setAgility(statsAverage.getAgility() + (statsAverage.getDeathbringerProc() * (1 + stats.getBonusAgilityMultiplier())) / 3f);
            statsAverage.setAttackPower(statsAverage.getAttackPower() + 2f * statsAverage.getDeathbringerProc() / 3f); // AP is double budget
            statsAverage.setHasteRating(statsAverage.getHasteRating() + statsAverage.getDeathbringerProc() / 3f);
        }
    }
}
