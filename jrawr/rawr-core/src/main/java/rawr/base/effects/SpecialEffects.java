// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/SpecialEffects.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
//
// Tooltip-line parsers for the network item import (use/equip/meta-gem lines). All patterns,
// branch order, string tests and float widths are kept exactly as written. Translation notes:
//   - C# `Regex` -> `java.util.regex.Pattern` with the same pattern text and default options.
//     The only semantic difference in play is that .NET `\d`/`\w` are Unicode classes while Java's
//     are ASCII by default; the tooltip lines are English ASCII, so this is not observable here.
//   - C# `Regex.Split(text, pattern)` inserts captured groups into the result; Java's
//     `Pattern.split` does not. `netSplit` below reproduces the .NET behavior.
//   - C# `int.Parse`/`float.Parse` allow surrounding whitespace; `parseIntNet` trims first (needed
//     by `...Substring(Length - 3, 2)` sites that parse " 2").
//   - `line.Trim('+')`/`Trim('%')`/`TrimStart(' ')` are char-trims, not Java `trim()`.
//   - The C# prototype-sigil dedupe compares `SpecialEffect.ToString()`; at every call site the two
//     effects differ only in their trigger and `SpecialEffect.TriggerString` is injective over the
//     Trigger enum, so `effectTextDiffers` compares the triggers (documented in DECISIONS).
//   - `StringComparison.InvariantCultureIgnoreCase` on ASCII stat names -> `equalsIgnoreCase`.
package rawr.base.effects;

import rawr.base.bosses.Attack;
import rawr.base.character.Buff;
import rawr.base.character.CharacterClass;
import rawr.base.stats.Stats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static parsers that turn item tooltip text into stats and {@link SpecialEffect}s.
 *
 * <p>Rawr's item data arrives as human-readable tooltip/description text (Wowhead descriptions,
 * Armory pages or the locally cached item database), not as structured fields. This class is the
 * translation layer: the item loader hands it one line at a time together with the {@link Stats}
 * object being built; the parser recognizes the text and adds the numbers, either as a flat stat or
 * as a proc/on-use {@link SpecialEffect} that a model averages later. Lines that match nothing are
 * silently ignored.</p>
 *
 * <h2>Entry points</h2>
 *
 * <ul>
 *   <li>{@link #processMetaGem(String, Stats, boolean)} &mdash; meta-gem bonus text. A meta gem can
 *       carry two bonuses joined by "and"/"&amp;"/comma, so the line is first split and each piece is
 *       matched against known bonuses (flat stats, "+2% Intellect", mana-on-cast procs, run speed,
 *       reduced stun/snare/fear durations, ...). Numerical pieces fall through to a generic
 *       "value + stat name" switch, so most gem bonuses work without a dedicated branch.</li>
 *   <li>{@link #processEquipLine(String, Stats, boolean, int, int)} &mdash; "Equip:" effect lines.
 *       This is a long ordered chain of exact pattern tests: the first matching pattern wins and the
 *       rest are skipped. Some branches change a stat directly, others register a proc whose
 *       duration, cooldown and chance come from the text, and a few recognize a specific item id or
 *       item level to hard-code values the tooltip does not state (the original's data quirks).</li>
 *   <li>{@link #processUseLine(String, Stats, boolean, int)} &mdash; "Use:" effect lines; the trigger
 *       is always {@link Trigger#Use}. A generic pattern reads
 *       "Increases ... by N for D sec (C Min Cooldown)" and maps the stat name, while the remaining
 *       branches cover tooltips whose cooldown is missing and therefore has to be guessed.</li>
 * </ul>
 *
 * <p>Both equip and use parsing end with a fallback section of patterns for the recurring grammar
 * "Your &lt;ability&gt; ... grants ... &lt;stat&gt; for &lt;duration&gt; sec", optionally with
 * stacks. Those patterns build effects through {@link #evalRegex(String, float, float, String, float)}
 * and its stack-aware overloads, which map a stat name to a {@code Stats} field and an ability name
 * to a {@link Trigger}; damage bonuses on abilities go through
 * {@link #evalAbility(String, Stats, float)}.</p>
 *
 * <h2>Parsing model and fidelity</h2>
 *
 * <ul>
 *   <li>Patterns keep the exact pattern text, named capture groups and test order of the C#
 *       original. The file header records the translation details: Java's regex classes are ASCII
 *       where .NET's were Unicode (irrelevant for these English tooltips), {@code netSplit}
 *       reproduces C# {@code Regex.Split} including captured groups, and {@code parseIntNet}
 *       tolerates surrounding whitespace like C# {@code int.Parse}.</li>
 *   <li>Values and constants are ported as written, including the original's guessed cooldowns for
 *       silent tooltips and the item-id special cases (for example the Ashen Band rings).</li>
 *   <li>When a fallback pattern captures two or three abilities that would create textually identical
 *       effects, the duplicate is suppressed (see {@code effectTextDiffers}).</li>
 *   <li>{@link #getDeathBringerEffects(CharacterClass, float)} is not a tooltip parser: it builds
 *       the class-specific proc candidates of one trinket for a model to pick from at calculation
 *       time.</li>
 * </ul>
 *
 * <p>The class is stateless: every method is a pure string-to-stats transformation over its
 * arguments. The parsers are locked by {@code SpecialEffectsTest} fixture lines.</p>
 *
 * @see SpecialEffect
 * @see Trigger
 */
public final class SpecialEffects {
    private static final ConcurrentHashMap<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    private static Pattern pattern(String regex) {
        return PATTERN_CACHE.computeIfAbsent(regex, Pattern::compile);
    }


    private SpecialEffects() {
    }

    /**
     * Parses one meta-gem bonus phrase and applies it to {@code stats}. Combined bonuses such as
     * "12 Spell Power and 2% Mana" are split first; each piece is then either a known named bonus or
     * a numeric "value + stat" pair. Unknown pieces are ignored, and a piece that cannot be parsed
     * numerically is swallowed silently, exactly as in the original.
     *
     * @param bisArmory retained from the C# signature; the parser does not branch on it
     */
    public static void processMetaGem(String line, Stats stats, boolean bisArmory) {
        List<String> gemBonuses = new ArrayList<String>();
        String[] gemBonusStrings = splitMulti(line, " and ", " & ", ", ");
        for (String gemBonusString : gemBonusStrings) {
            if (gemBonusString.indexOf('+') != gemBonusString.lastIndexOf('+')) {
                gemBonuses.add(gemBonusString.substring(0, gemBonusString.indexOf(" +")));
                gemBonuses.add(gemBonusString.substring(gemBonusString.indexOf(" +") + 1));
            } else {
                gemBonuses.add(gemBonusString);
            }
        }
        for (String gemBonus : gemBonuses) {
            if (gemBonus.equals("Spell Damage +6")) {
                stats.setSpellPower(6.0f);
            } else if (gemBonus.equals("Stamina +6")) {
                stats.setStamina(6.0f);
            } else if (gemBonus.equals("Chance to restore mana on spellcast")) {
                Stats effectStats = new Stats();
                effectStats.setManaRestore(600);
                stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCast, effectStats, 0f, 15f, .05f));
                stats.setManaRestoreOnCast_5_15(600); // IED
            } else if (gemBonus.equals("2% Increased Armor Value from Items")) {
                stats.setBaseArmorMultiplier(0.02f); // IED
            } else if (gemBonus.equals("Chance on spellcast - next spell cast in half time") || gemBonus.equals("Chance to Increase Spell Cast Speed")) {
                Stats effectStats = new Stats();
                effectStats.setHasteRating(320);
                stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCast, effectStats, 6, 45, 0.15f));
            } else if (gemBonus.equals("+5% Shield Block Value")) {
                stats.setBonusBlockValueMultiplier(0.05f);
            } else if (gemBonus.equals("+2% Intellect")) {
                stats.setBonusIntellectMultiplier(0.02f);
            } else if (gemBonus.equals("+2% Mana")) {
                stats.setBonusManaMultiplier(0.02f);
            } else if (gemBonus.contains("Reduce Spell Damage Taken by ")) {
                int bonus = parseIntNet(gemBonus.substring(gemBonus.length() - 3, gemBonus.length() - 1));
                stats.setSpellDamageTakenMultiplier((float) bonus / -100f);
            } else if (gemBonus.equals("3% Increased Critical Damage")) {
                stats.setBonusCritMultiplier(0.03f);
                stats.setBonusSpellCritMultiplier(0.03f);
            } else if (gemBonus.equals("3% Increased Critical Healing Effect")) {
                stats.setBonusCritHealMultiplier(0.03f);
            } else if (gemBonus.equals("Minor Run Speed Increase")) {
                stats.setMovementSpeed(0.08f);
            } else if (gemBonus.contains("Stun Duration Reduced by ")) {
                int bonus = parseIntNet(gemBonus.substring(gemBonus.length() - 3, gemBonus.length() - 1));
                stats.setStunDurReduc((float) bonus / 100f);
            } else if (gemBonus.contains("Reduces Snare/Root Duration by ")) {
                int bonus = parseIntNet(gemBonus.substring(gemBonus.length() - 3, gemBonus.length() - 1));
                stats.setSnareRootDurReduc((float) bonus / 100f);
            } else if (gemBonus.contains("Fear Duration Reduced by ")) {
                int bonus = parseIntNet(gemBonus.substring(gemBonus.length() - 3, gemBonus.length() - 1));
                stats.setFearDurReduc((float) bonus / 100f);
            } else if (gemBonus.contains("Chance to Increase Melee/Ranged Attack Speed")) {
                // 480 Haste Rating on 100% Chance to proc every 60 seconds for 6 seconds
                Stats effectStats = new Stats();
                effectStats.setHasteRating(480f);
                stats.addSpecialEffect(new SpecialEffect(Trigger.DamageDone, effectStats, 6f, 60f));
            } else if (gemBonus.contains("% Spell Reflect")) {
                int bonus = parseIntNet(trimChar(gemBonus.substring(0, 2), '%'));
                stats.setSpellReflectChance((float) bonus / 100f);
            } else if (gemBonus.equals("Sometimes Heal on Your Crits")) {
                // this is supposed to be 2% of your total health healed when it procs, 50% chance to proc on crit
                Stats physicalStats = new Stats();
                physicalStats.setHealthRestoreFromMaxHealth(0.02f);
                stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalCrit, physicalStats, 0f, 0f, 0.50f));
                Stats spellStats = new Stats();
                spellStats.setHealthRestoreFromMaxHealth(0.02f);
                stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCrit, spellStats, 0f, 0f, 0.50f));
            } else if (gemBonus.equals("2% Reduced Threat")) {
                stats.setThreatReductionMultiplier(0.02f);
            } else {
                try {
                    int gemBonusValue = parseIntNet(trimChar(trimChar(gemBonus.substring(0, gemBonus.indexOf(' ')), '+'), '%'));
                    switch (gemBonus.substring(gemBonus.indexOf(' ') + 1).trim()) {
                        case "Resist All":
                            stats.setArcaneResistance(gemBonusValue);
                            stats.setFireResistance(gemBonusValue);
                            stats.setFrostResistance(gemBonusValue);
                            stats.setNatureResistance(gemBonusValue);
                            stats.setShadowResistance(gemBonusValue);
                            break;
                        case "Increased Critical Damage":
                            stats.setBonusCritMultiplier((float) gemBonusValue / 100f);
                            stats.setBonusSpellCritMultiplier((float) gemBonusValue / 100f); // both melee and spell crit use the same text, would have to disambiguate based on id
                            break;
                        case "Agility":
                            stats.setAgility(gemBonusValue);
                            break;
                        case "Stamina":
                            stats.setStamina(gemBonusValue);
                            break;
                        case "Dodge Rating":
                            stats.setDodgeRating(gemBonusValue);
                            break;
                        case "Parry Rating":
                            stats.setParryRating(gemBonusValue);
                            break;
                        case "Block Rating":
                            stats.setBlockRating(gemBonusValue);
                            break;
                        case "Defense Rating":
                            stats.setDefenseRating(gemBonusValue);
                            break;
                        case "Hit Rating":
                            stats.setHitRating(gemBonusValue);
                            break;
                        case "Haste Rating":
                            stats.setHasteRating(gemBonusValue);
                            break;
                        case "Expertise Rating":
                            stats.setExpertiseRating(gemBonusValue);
                            break;
                        case "Armor Penetration Rating":
                            stats.setArmorPenetrationRating(gemBonusValue);
                            break;
                        case "Strength":
                            stats.setStrength(gemBonusValue);
                            break;
                        case "Crit Rating":
                        case "Crit Strike Rating":
                        case "Critical Rating":
                        case "Critical Strike Rating":
                            stats.setCritRating(gemBonusValue);
                            break;
                        case "Attack Power":
                            stats.setAttackPower(gemBonusValue);
                            break;
                        case "Weapon Damage":
                            stats.setWeaponDamage(gemBonusValue);
                            break;
                        case "Resilience":
                        case "Resilience Rating":
                            stats.setResilience(gemBonusValue);
                            break;
                        case "Spell Hit Rating":
                            stats.setHitRating(gemBonusValue);
                            break;
                        case "Spell Haste Rating":
                            stats.setHasteRating(gemBonusValue);
                            break;
                        case "Spell Damage":
                            // Ignore spell damage from gem if Healing has already been applied, as it might be a "9 Healing 3 Spell" gem.
                            if (stats.getSpellPower() == 0) {
                                stats.setSpellPower(gemBonusValue);
                            }
                            break;
                        case "Spell Damage and Healing":
                            stats.setSpellPower(gemBonusValue);
                            break;
                        case "Healing":
                            stats.setSpellPower((float) Math.rint(gemBonusValue / 1.88f));
                            break;
                        case "Spell Power":
                            stats.setSpellPower(gemBonusValue);
                            break;
                        case "Spell Crit":
                        case "Spell Crit Rating":
                        case "Spell Critical":
                        case "Spell Critical Rating":
                            stats.setCritRating(gemBonusValue);
                            break;
                        case "Mana every 5 seconds":
                        case "Mana ever 5 Sec":
                        case "mana per 5 sec":
                        case "mana per 5 sec.":
                        case "Mana per 5 Seconds":
                            stats.setMp5(gemBonusValue);
                            break;
                        case "Intellect":
                            stats.setIntellect(gemBonusValue);
                            break;
                        case "Spirit":
                            stats.setSpirit(gemBonusValue);
                            break;
                        default:
                            break;
                    }
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    /**
     * Parses one "Equip:" effect line and applies it to {@code stats}. The line is first normalized
     * (plural "secs", trailing periods, collapsed spaces) and then matched against the ordered
     * pattern chain; only the first matching branch contributes. Proc branches register a
     * {@link SpecialEffect} with the trigger implied by the ability named in the text.
     *
     * @param isArmory enables the trailing block of Armory-format lines ("Increases spell power by",
     *        "Improves haste rating by", ...) that the local item cache does not contain
     * @param ilvl item level, used by a few branches to choose a heroic/normal value
     * @param id item id, used by a few branches with tooltips the original could not trust
     */
    public static void processEquipLine(String line, Stats stats, boolean isArmory, int ilvl, int id) {
        Matcher match;
        // Prep the line, if it needs it (SpecialEffects.cs:219-224)
        while (line.contains("secs")) {
            line = line.replace("secs", "sec");
        }
        while (line.contains("sec.")) {
            line = line.replace("sec.", "sec");
        }
        while (line.contains("  ")) {
            line = line.replace("  ", " ");
        }
        while (line.endsWith(".")) {
            line = line.substring(0, line.length() - 1);
        }
        if (false) { /* Never run, this is just to make all the stuff below uniform with an 'else if' line start */ }
        // Class Specific
        // Added by Druid: Bear/Cat
        else if ((match = match(line, "Your Mangle ability also grants you (?<amount>\\d\\d*) attack power for 10 sec")).find()) {
            // Gladiator's Idol of Resolve
            Stats effectStats = new Stats();
            effectStats.setAttackPower((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.MangleCatHit, effectStats, 10, 0));
        } else if ((match = match(line, "While in Bear Form, your Lacerate and Swipe abilities have a chance to grant (?<amount1>\\d\\d*) dodge rating for (?<dur1>\\d\\d*) sec, and your Cat Form's Mangle and Shred abilities have a chance to grant (?<amount2>\\d\\d*) Agility for (?<dur2>\\d\\d*) sec")).find()) {
            // Idol of Mutilation
            Stats bearStats = new Stats();
            bearStats.setDodgeRating((float) parseIntNet(group(match, "amount1")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.SwipeBearOrLacerateHit, bearStats, (float) parseIntNet(group(match, "dur1")), 8f, .65f));
            Stats catStats = new Stats();
            catStats.setAgility((float) parseIntNet(group(match, "amount2")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.MangleCatOrShredHit, catStats, (float) parseIntNet(group(match, "dur2")), 8f, .85f));
        } else if ((match = match(line, "Increases periodic damage done by Rip by (?<amount>\\d\\d*) per combo point")).find()) {
            // Idol of Worship
            stats.setBonusRipDamagePerCPPerTick(stats.getBonusRipDamagePerCPPerTick() + (float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Your Mangle ability has a chance to grant (?<amount>\\d\\d*) agility for (?<dur>\\d\\d*) sec")).find()) {
            // Idol of the Corruptor | Idol of Terror
            float Dur = (float) parseIntNet(group(match, "dur"));
            if (Dur == 10) { // Terror
                Stats catStats = new Stats();
                catStats.setAgility((float) parseIntNet(group(match, "amount")));
                stats.addSpecialEffect(new SpecialEffect(Trigger.MangleCatHit, catStats, Dur, 0f, 0.65f));
                Stats bearStats = new Stats();
                bearStats.setAgility((float) parseIntNet(group(match, "amount")));
                stats.addSpecialEffect(new SpecialEffect(Trigger.MangleBearHit, bearStats, Dur, 0f, 0.45f));
            } else { // Corruptor
                Stats catStats = new Stats();
                catStats.setAgility((float) parseIntNet(group(match, "amount")));
                stats.addSpecialEffect(new SpecialEffect(Trigger.MangleCatHit, catStats, Dur, 0f, 1f));
                Stats bearStats = new Stats();
                bearStats.setAgility((float) parseIntNet(group(match, "amount")));
                stats.addSpecialEffect(new SpecialEffect(Trigger.MangleBearHit, bearStats, Dur, 0f, 0.7f));
            }
        } else if ((match = match(line, "The periodic damage from your Lacerate and Rake abilities grants (?<amount>\\d\\d*) Agility for (?<dur>\\d\\d*) sec Stacks up to (?<stack>\\d\\d*) times")).find()) {
            // Idol of the Crying Moon
            Stats lacerateStats = new Stats();
            lacerateStats.setAgility((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.LacerateTick, lacerateStats, (float) parseIntNet(group(match, "dur")), 0f, 1f, parseIntNet(group(match, "stack"))));
            Stats rakeStats = new Stats();
            rakeStats.setAgility((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.RakeTick, rakeStats, (float) parseIntNet(group(match, "dur")), 0f, 1f, parseIntNet(group(match, "stack"))));
        } else if ((match = match(line, "Increases the damage dealt by Shred by (?<amount>\\d\\d*)")).find()) {
            // Idol of the Ravenous Beast
            stats.setBonusShredDamage(stats.getBonusShredDamage() + parseIntNet(group(match, "amount")));
        }
        // Added by Druid: Tree
        else if ((match = match(line, "Increases the spell power of the final healing value of your Lifebloom by (?<amount>\\d\\d*)")).find()) {
            // Gladiator's Idol of Tenacity
            // Note: This may need to go to another stat or drop this stat's value
            stats.setLifebloomTickHealBonus(stats.getLifebloomTickHealBonus() + (float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Reduces the mana cost of Rejuvenation by (?<amount>\\d\\d*)")).find()) {
            // Idol of Awakening
            stats.setReduceRejuvenationCost(stats.getReduceRejuvenationCost() + (float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Increases the spell power on the periodic portion of your Lifebloom by (?<amount>\\d\\d*)")).find()) {
            // Idol of Lush Mosh
            stats.setLifebloomTickHealBonus(stats.getLifebloomTickHealBonus() + (float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Each time your Rejuvenation spell deals periodic healing, you have a chance to gain (?<amount>\\d+) spell power for (?<dur>\\d+) sec")).find()) {
            // Idol of Flaring Growth
            // Not yet sure about cooldown and proc chance
            Stats effectStats = new Stats();
            effectStats.setSpellPower(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.RejuvenationTick, effectStats, parseIntNet(group(match, "dur")), 0, 0.7f));
        } else if ((match = match(line, "The periodic healing from your Rejuvenation spell grants (?<amount>\\d+) spell power for (?<dur>\\d+) sec Stacks up to (?<stacks>\\d+) times")).find()) {
            // Idol of the Black Willow
            Stats effectStats = new Stats();
            effectStats.setSpellPower(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.RejuvenationTick, effectStats, parseIntNet(group(match, "dur")), 0, 1f, parseIntNet(group(match, "stacks"))));
        } else if ((match = match(line, "Increases the spell power of your Nourish by (?<amount>\\d+)")).find()) {
            // Idol of the Flourishing Life
            stats.setNourishSpellpower(stats.getNourishSpellpower() + parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Increases the periodic healing of Rejuvenation by (?<amount>\\d+)")).find()) {
            // Idol of Pure Thoughts
            stats.setRejuvenationHealBonus(stats.getRejuvenationHealBonus() + parseIntNet(group(match, "amount")));
        }
        // Added by Druid: Moonkin
        else if ((match = match(line, "The periodic damage from your Insect Swarm and Moonfire spells grants (?<amount>\\d\\d*) critical strike rating for (?<dur>\\d\\d*) sec Stacks up to (?<stacks>\\d\\d*) times")).find()) {
            // Idol of the Lunar Eclipse
            Stats effectStats = new Stats();
            effectStats.setCritRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.InsectSwarmOrMoonfireTick, effectStats, (float) parseIntNet(group(match, "dur")), 0, 1f, parseIntNet(group(match, "stacks"))));
        } else if ((match = match(line, "Each time your Moonfire spell deals periodic damage, you have a chance to gain (?<amount>\\d\\d*) critical strike rating for (?<dur>\\d\\d*) sec")).find()) {
            // Idol of Lunar Fury
            Stats effectStats = new Stats();
            effectStats.setCritRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.MoonfireTick, effectStats, (float) parseIntNet(group(match, "dur")), 0f, 0.7f));
        } else if ((match = match(line, "Increases the damage dealt by Wrath by (?<amount>\\d\\d*)")).find()) {
            // Idol of Steadfast Renewal
            stats.setWrathDmg(stats.getWrathDmg() + (float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Increases the spell power of your Insect Swarm by (?<amount>\\d\\d*)")).find()) {
            // Idol of the Crying Wind
            stats.setInsectSwarmDmg(stats.getInsectSwarmDmg() + (float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Increases the spell power of your Starfire spell by (?<amount>\\d\\d*)")).find()) {
            // Idol of the Shooting Star
            stats.setStarfireDmg(stats.getStarfireDmg() + (float) parseIntNet(group(match, "amount")));
        }
        // Other procs to process (Unknown sources)
        else if ((match = match(line, "Increases the spell power of your Moonfire spell by (?<amount>\\d\\d*)")).find()) {
            stats.setMoonfireDmg(stats.getMoonfireDmg() + (float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Your Moonfire spell grants (?<amount>\\d\\d*) spell power for 10 sec")).find()) {
            Stats effectStats = new Stats();
            effectStats.setSpellPower((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.MoonfireCast, effectStats, 10f, 0f));
        } else if (line.startsWith("Your Moonfire ability has a chance to grant ")) {
            line = line.substring("Your Moonfire ability has a chance to grant ".length());
            String spellPowerLine = line.substring(0, line.indexOf(" spell power for"));
            String durationLine = line.substring(line.indexOf("for") + 3, line.indexOf(" sec") - line.indexOf("for") - 3);
            float spellPower = Float.parseFloat(spellPowerLine);
            float duration = Float.parseFloat(durationLine);
            Stats effectStats = new Stats();
            effectStats.setSpellPower(spellPower);
            stats.addSpecialEffect(new SpecialEffect(Trigger.MoonfireCast, effectStats, duration, 0f, 0.5f));
        }
        // Added by DK: Tank/DPS
        // Note: Sigils code can be found at the bottom of this else statement
        // Added by Shaman: Enhance/Elemental/Resto
        else if ((match = match(line, "Your Lava Lash ability also grants you (?<amount>\\d\\d*) attack power for (?<dur>\\d\\d*) sec")).find()) {
            // Gladiator's Totem of Indomitability
            Stats effectStats = new Stats();
            effectStats.setAttackPower((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.ShamanLavaLash, effectStats, (float) parseIntNet(group(match, "dur")), 0f));
        } else if ((match = match(line, "Your Shock spells grant (?<amount>\\d\\d*) spellpower for (?<dur>\\d\\d*) sec")).find()) {
            // Gladiator's Totem of Survival
            Stats effectStats = new Stats();
            effectStats.setSpellPower((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.ShamanShock, effectStats, (float) parseIntNet(group(match, "dur")), 0f));
        } else if ((match = match(line, "Increases spell power of Lesser Healing Wave by (?<amount>\\d\\d*)")).find()) {
            // Gladiator's Totem of the Third Wind | Totem of the Plains
            stats.setTotemLHWSpellpower((float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "The periodic damage from your Flame Shock spell grants (?<amount>\\d\\d*) haste rating for (?<dur>\\d\\d*) sec Stacks up to (?<stacks>\\d\\d*) times")).find()) {
            // Bizuri's Totem of Shattered Ice
            Stats effectStats = new Stats();
            effectStats.setHasteRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.ShamanFlameShockDoTTick, effectStats, (float) parseIntNet(group(match, "dur")), 0f, 1f, parseIntNet(group(match, "stacks"))));
        } else if ((match = match(line, "Increases the base amount healed by your chain heal by (?<amount>\\d\\d*)")).find()) {
            // Steamcaller's Totem | Totem of the Bay
            stats.setTotemCHBaseHeal((float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Increases the base damage of your Lava Burst by (?<amount>\\d\\d*)")).find()) {
            // Thunderfall Totem
            stats.setLavaBurstBonus((float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Each time you cast Chain Heal, you have a chance to gain (?<amount>\\d\\d*) spell power for (?<dur>\\d\\d*) sec")).find()) {
            // Totem of Calming Tides
            stats.setRestoShamRelicT9((float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Your Storm Strike ability also grants you (?<amount>\\d\\d*) haste rating for (?<dur>\\d\\d*) sec")).find()) {
            // Totem of Dueling
            Stats effectStats = new Stats();
            effectStats.setHasteRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.ShamanStormStrike, effectStats, (float) parseIntNet(group(match, "dur")), 0f));
        } else if ((match = match(line, "Each time you cast Lightning Bolt, you have a chance to gain (?<amount>\\d\\d*) haste rating for (?<dur>\\d\\d*) sec")).find()) {
            // Totem of Electrifying Wind
            Stats effectStats = new Stats();
            effectStats.setHasteRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.ShamanLightningBolt, effectStats, (float) parseIntNet(group(match, "dur")), 0f, 0.70f));
        } else if ((match = match(line, "Reduces the base mana cost of Chain Heal by (?<amount>\\d\\d*)")).find()) {
            // Totem of Forest Growth
            stats.setTotemCHBaseCost((float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Increases spell power of Chain Lightning and Lightning Bolt by (?<amount>\\d\\d*)")).find()) {
            // Totem of Hex
            stats.setLightningSpellPower((float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Reduces the mana cost of Healing Wave by (?<amount>\\d\\d*)")).find()) {
            // Totem of Misery
            stats.setTotemHWBaseCost((float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Each time you use your Lava Lash ability, you have a chance to gain (?<amount>\\d\\d*) attack power for (?<dur>\\d\\d*) sec")).find()) {
            // Totem of Quaking Earth
            Stats effectStats = new Stats();
            effectStats.setAttackPower((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.ShamanLavaLash, effectStats, (float) parseIntNet(group(match, "dur")), 9f, 0.80f));
        } else if ((match = match(line, "Increases the attack power bonus on Windfury Weapon attacks by (?<amount>\\d\\d*)")).find()) {
            // Totem of Astral Winds & Totem of Splintering
            stats.setBonusWFAttackPower((float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Your Stormstrike ability grants (?<amount>\\d\\d*) attack power for (?<dur>\\d\\d*) sec Stacks up to (?<stacks>\\d\\d*) times")).find()) {
            // Totem of the Avalanche
            Stats effectStats = new Stats();
            effectStats.setAttackPower((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.ShamanStormStrike, effectStats, (float) parseIntNet(group(match, "dur")), 0f, 1f, parseIntNet(group(match, "stacks"))));
        } else if ((match = match(line, "Increases weapon damage when you use Stormstrike by (?<amount>\\d\\d*)")).find()) {
            // Totem of the Dancing Flame
            stats.setBonusSSDamage(stats.getBonusSSDamage() + (float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Your Lightning Bolt spell has a chance to grant (?<amount>\\d\\d*) haste rating for (?<dur>\\d\\d*) sec")).find()) {
            // Totem of the Elemental Plane
            //stats.LightningBoltHasteProc_15_45 += (float)int.Parse(match.Groups["amount"].Value);
            Stats effectStats = new Stats();
            effectStats.setHasteRating(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.ShamanLightningBolt, effectStats, (float) parseIntNet(group(match, "dur")), 45f, 0.15f));
        } else if ((match = match(line, "Your Riptide spell grants (?<amount>\\d\\d*) spell power for (?<dur>\\d\\d*) sec Stacks up to (?<stacks>\\d\\d*) times")).find()) {
            // Totem of the Surging Sea
            // This needs to be remodeled as a SpecialEffect
            stats.setRestoShamRelicT10(parseIntNet(group(match, "amount")) * parseIntNet(group(match, "stacks")));
        }
        // Other
        else if (line.startsWith("Increases spell power of Healing Wave by ")) {
            // Totem of Spontaneous Regrowth
            line = line.replace(".", "");
            line = line.substring("Increases spell power of Healing Wave by ".length());
            stats.setTotemHWSpellpower(Float.parseFloat(line));
        } else if (line.startsWith("Your Water Shield ability grants an additional ")) {
            // Totem of the Thunderhead, Possible Future totems
            stats.setTotemThunderhead(1f);
        } else if (line.startsWith("Increases the damage dealt by your Lava Burst by ")) {
            line = line.replace(".", "");
            line = line.substring("Increases the damage dealt by your Lava Burst by ".length());
            stats.setLavaBurstBonus(Float.parseFloat(line));
        } else if (line.equals("Your Shock spells have a chance to grant 110 attack power for 10 sec")) {
            Stats effectStats = new Stats();
            effectStats.setAttackPower(110);
            stats.addSpecialEffect(new SpecialEffect(Trigger.ShamanShock, effectStats, 10f, 45f));
        }
        // Added by Priest: HealPriest
        // Added by Priest: Shadow
        // Added by Warrior: ProtWarr
        // Added by Warrior: DPSWarr
        // Added by Paladin: Healadin
        else if ((match = match(line, "Each time you cast Holy Light, you have a chance to gain (?<amount>\\d\\d*) spell power for (?<dur>\\d\\d*) sec")).find()) {
            // Libram of Veracity
            Stats effectStats = new Stats();
            effectStats.setSpellPower((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.HolyLightCast, effectStats, (float) parseIntNet(group(match, "dur")), 10f, 0.70f));
        } else if ((match = match(line, "Your Holy Shock heals grant (?<amount>\\d\\d*) spell power for (?<dur>\\d\\d*) sec Stacks up to (?<stacks>\\d\\d*) times")).find()) {
            // Libram of Blinding Light
            Stats effectStats = new Stats();
            effectStats.setSpellPower((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.HolyShockCast, effectStats, (float) parseIntNet(group(match, "dur")), 0f, 1f, parseIntNet(group(match, "stacks"))));
        } else if ((match = match(line, "Reduces the mana cost of Holy Light by (?<amount>\\d\\d*)")).find()) {
            // Libram of Renewal
            stats.setHolyLightManaCostReduction(stats.getHolyLightManaCostReduction() + (float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Increases spell power of Holy Light by (?<amount>\\d\\d*)")).find()) {
            // Libram of Tolerance | the Resolute
            stats.setHolyLightSpellPower(stats.getHolyLightSpellPower() + (float) parseIntNet(group(match, "amount")));
        }
        // Other
        else if ((match = match(line, "Increases spell power of Flash of Light by (?<amount>\\d\\d*)")).find()) {
            stats.setFlashOfLightSpellPower(stats.getFlashOfLightSpellPower() + (float) parseIntNet(group(match, "amount")));
        }
        // Added by Paladin: Tankadin
        else if ((match = match(line, "Your Judgement ability also increases your shield block value by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            // Libram of Obstruction
            Stats effectStats = new Stats();
            effectStats.setJudgementBlockValue((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.JudgementHit, effectStats, (float) parseIntNet(group(match, "dur")), 0f, 1f));
        } else if ((match = match(line, "Increases your block value by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec each time you use Holy Shield")).find()) {
            // Libram of the Sacred Shield
            Stats effectStats = new Stats();
            effectStats.setHolyShieldBlockValue((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.HolyShield, effectStats, (float) parseIntNet(group(match, "dur")), 0f, 1f));
        } else if ((match = match(line, "Each time you use your Hammer of The Righteous ability, you have a chance to gain (?<amount>\\d\\d*) dodge rating for (?<dur>\\d\\d*) sec")).find()) {
            // Libram of Defiance
            Stats effectStats = new Stats();
            effectStats.setDodgeRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.HammeroftheRighteous, effectStats, (float) parseIntNet(group(match, "dur")), 10f, 0.8f));
        } else if (line.equals("Increases the spell power of your Consecration spell by 141")) {
            // Libram of Resurgence
            stats.setConsecrationSpellPower(141f);
        } else if ((match = match(line, "Your Shield of Righteousness ability grants (?<amount>\\d\\d*) dodge rating for (?<dur>\\d\\d*) sec Stacks up to (?<stacks>\\d\\d*) times")).find()) {
            // Libram of the Eternal Tower
            Stats effectStats = new Stats();
            effectStats.setDodgeRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.ShieldofRighteousness, effectStats, (float) parseIntNet(group(match, "dur")), 0f, 1f, parseIntNet(group(match, "stacks"))));
        }
        // Other
        else if ((match = match(line, "Your Shield of Righteousness deals an additional (?<amount>\\d\\d*) damage")).find()) {
            stats.setBonusShieldOfRighteousnessDamage((float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Your Holy Shield ability also grants you (?<amount>\\d\\d*) resilience rating for (?<dur>\\d\\d*) sec")).find()) {
            Stats effectStats = new Stats();
            effectStats.setResilience((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.HolyShield, effectStats, (float) parseIntNet(group(match, "dur")), 0f, 1f));
        } else if ((match = match(line, "Your Judgement ability also grants you (?<amount>\\d\\d*) resilience rating for (?<dur>\\d\\d*) sec")).find()) {
            Stats effectStats = new Stats();
            effectStats.setResilience((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.JudgementHit, effectStats, (float) parseIntNet(group(match, "dur")), 0f, 1f));
        }
        // Added by Paladin: Retadin
        else if ((match = match(line, "Your Crusader Strike ability also grants you (?<amount>\\d\\d*) attack power for (?<dur>\\d\\d*) sec")).find()) {
            // Gladiator's Libram of Fortitude
            Stats effectStats = new Stats();
            effectStats.setAttackPower(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.CrusaderStrikeHit, effectStats, parseIntNet(group(match, "dur")), 0f));
        } else if ((match = match(line, "Increases the damage dealt by Crusader Strike by (?<amount>\\d\\d*)")).find()) {
            // Libram of Radiance
            stats.setCrusaderStrikeDamage(78.75f); //int.Parse(match.Groups["amount"].Value);
        } else if ((match = match(line, "Your Crusader Strike ability grants (?<amount>\\d\\d*) Strength for (?<dur>\\d\\d*) sec Stacks up to (?<stacks>\\d\\d*) times")).find()) {
            // Libram of Three Truths
            Stats effectStats = new Stats();
            effectStats.setStrength((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.CrusaderStrikeHit, effectStats, (float) parseIntNet(group(match, "dur")), 0f, 1f, parseIntNet(group(match, "stacks"))));
        } else if ((match = match(line, "Increases the damage done by Divine Storm by (?<amount>\\d\\d*)")).find()) {
            // Libram of Discord
            stats.setDivineStormDamage((float) parseIntNet(group(match, "amount")));
        } else if ((match = match(line, "Each time your Seal of Vengeance or Seal of Corruption ability deals periodic damage, you have a chance to gain (?<amount>\\d\\d*) Strength for (?<dur>\\d\\d*) sec")).find()) {
            // Libram of Valiance
            Stats effectStats = new Stats();
            effectStats.setStrength((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.SealOfVengeanceTick, effectStats, (float) parseIntNet(group(match, "dur")), 6f, 0.70f));
        }
        // Other
        else if (line.equals("Increases the damage dealt by your Crusader Strike ability by 5%")) {
            stats.setCrusaderStrikeMultiplier(0.05f);
        } else if (line.startsWith("Causes your Divine Storm to increase your Critical Strike rating by 73 for 8 sec")) {
            stats.setDivineStormDamage(81);
        } else if (line.startsWith("Causes your Judgements to increase your Critical Strike rating by 53 for 5 sec")) {
            Stats effectStats = new Stats();
            effectStats.setCritRating(53);
            stats.addSpecialEffect(new SpecialEffect(Trigger.JudgementHit, effectStats, 5f, 0f, 1f));
        } else if (line.startsWith("Causes your Judgements to increase your Critical Strike Rating by 61 for 5 sec")) {
            Stats effectStats = new Stats();
            effectStats.setCritRating(61);
            stats.addSpecialEffect(new SpecialEffect(Trigger.JudgementHit, effectStats, 5f, 0f, 1f));
        }
        // Added by Hunter
        // Added by Warlock
        // Added by Mage
        // Added by Rogue
        // Professions
        else if (line.startsWith("Increases the effect that healing and mana potions have on the wearer by ")) {
            // Alchemist's Stone
            line = line.substring("Increases the effect that healing and mana potions have on the wearer by ".length());
            line = line.substring(0, line.indexOf('%'));
            stats.setBonusManaPotion(stats.getBonusManaPotion() + parseIntNet(line) / 100f);
            stats.setHealthRestore(stats.getHealthRestore() + parseIntNet(line) / 100f);
        }
        // General
        // Armor Penetration Rating
        else if ((match = match(line, "Chance on melee (and|or) ranged critical strike to increase your armor penetration rating by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            // Needle-Encrusted Scorpion
            Stats effectStats = new Stats();
            effectStats.setArmorPenetrationRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalCrit, effectStats, (float) parseIntNet(group(match, "dur")), 45, 0.10f));
        } else if ((match = match(line, "Your melee (and|or) ranged attacks have a chance to increase your armor penetration rating by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            // Mjolnir Runestone | Grim Toll
            Stats effectStats = new Stats();
            effectStats.setArmorPenetrationRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalHit, effectStats, (float) parseIntNet(group(match, "dur")), 45, 0.15f));
        }
        // Crit Rating
        else if ((match = match(line, "Chance on hit to increase your critical strike rating by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            Stats effectStats = new Stats();
            effectStats.setCritRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalHit, effectStats, (float) parseIntNet(group(match, "dur")), 45, 0.10f));
        } else if ((match = match(line, "Your melee and ranged attacks have a chance to increase your critical strike rating by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            Stats effectStats = new Stats();
            effectStats.setCritRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalHit, effectStats, (float) parseIntNet(group(match, "dur")), 45f, 0.15f));
        }
        // Haste Rating
        else if ((match = match(line, "Your harmful spells have a chance to increase your haste rating by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            // Elemental Focus Stone
            Stats effectStats = new Stats();
            effectStats.setHasteRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageSpellCast, effectStats, (float) parseIntNet(group(match, "dur")), 45f, 0.10f));
        } else if ((match = match(line, "Your melee and ranged attacks have a chance to increase your haste rating by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            Stats effectStats = new Stats();
            effectStats.setHasteRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalHit, effectStats, (float) parseIntNet(group(match, "dur")), 45, 0.15f));
        } else if ((match = match(line, "Chance on melee and ranged critical strike to increase your haste rating by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            Stats effectStats = new Stats();
            effectStats.setHasteRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalCrit, effectStats, (float) parseIntNet(group(match, "dur")), 45, 0.10f));
        } else if ((match = match(line, "Your spell critical strikes have a 50% chance to grant you (?<amount>\\d\\d*) spell haste rating for (?<dur>\\d\\d*) sec")).find()) {
            Stats effectStats = new Stats();
            effectStats.setHasteRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCrit, effectStats, (float) parseIntNet(group(match, "dur")), 0, 0.50f));
        } else if ((match = match(line, "Your spells have a chance to increase your haste rating by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            // Embrace of the Spider
            Stats effectStats = new Stats();
            effectStats.setHasteRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCast, effectStats, (float) parseIntNet(group(match, "dur")), 45, 0.10f));
        } else if ((match = match(line, "Your direct healing and heal over time spells have a chance to increase your haste rating by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            // The Egg of Mortal Essence
            Stats effectStats = new Stats();
            effectStats.setHasteRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.HealingSpellCast, effectStats, (float) parseIntNet(group(match, "dur")), 45f, 0.10f));
        }
        // Attack Power
        else if ((match = match(line, "Each time you deal melee or ranged damage to an opponent, you gain (?<amount>\\d\\d*) attack power for the next (?<dur>\\d\\d*) sec, stacking up to (?<stacks>\\d\\d*) times")).find()) {
            // Fury of the Five Flights
            Stats effectStats = new Stats();
            effectStats.setAttackPower((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalHit, effectStats, (float) parseIntNet(group(match, "dur")), 0f, 1f, parseIntNet(group(match, "stacks"))));
        } else if ((match = match(line, "Chance on critical hit to increase your attack power by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            int ap = parseIntNet(group(match, "amount"));
            int duration = parseIntNet(group(match, "dur"));
            Stats physicalStats = new Stats();
            physicalStats.setAttackPower(ap);
            stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalCrit, physicalStats, duration, 45f, 0.10f));
            Stats spellStats = new Stats();
            spellStats.setAttackPower(ap);
            stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCrit, spellStats, duration, 45f, 0.10f));
        } else if ((match = match(line, "Chance on melee and ranged critical strike to increase your attack power by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            Stats effectStats = new Stats();
            effectStats.setAttackPower((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalCrit, effectStats, parseIntNet(group(match, "dur")), 50f, 0.10f));
        } else if ((match = match(line, "Chance on melee or ranged hit to increase your attack power by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            Stats effectStats = new Stats();
            effectStats.setAttackPower((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalHit, effectStats, (float) parseIntNet(group(match, "dur")), 45f, 0.10f));
        } else if ((match = match(line, "Each time you hit with a melee or ranged attack, you have a chance to gain (?<amount>\\d\\d*) attack power for (?<dur>\\d\\d*) sec")).find()) {
            Stats effectStats = new Stats();
            effectStats.setAttackPower(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalHit, effectStats, (float) parseIntNet(group(match, "dur")), 45f, 0.20f));
        } else if ((match = match(line, "Each time you deal melee or ranged damage to an opponent, you gain (?<amount>\\d\\d*) attack power for the next (?<dur>\\d\\d*) sec(|,) stacking up to (?<stacks>\\d\\d*) times")).find()) {
            // Herkuml War Token
            Stats effectStats = new Stats();
            effectStats.setAttackPower((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalHit, effectStats, (float) parseIntNet(group(match, "dur")), 0f, 1f, parseIntNet(group(match, "stacks"))));
        } else if ((match = match(line, "When you deal damage you have a chance to gain (?<amount>\\d\\d*) attack power for (?<dur>\\d\\d*) sec")).find()) {
            // Whispering Fanged Skull & Sharpened Twilight Scale
            Stats effectStats = new Stats();
            effectStats.setAttackPower(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageDone, effectStats, parseIntNet(group(match, "dur")), 45f, 0.35f));
        } else if ((match = match(line, "Chance on hit to increase your attack power by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            // Ashen Band of Vengeance - seems to be 60 sec iCD (wowhead)
            Stats effectStats = new Stats();
            effectStats.setAttackPower((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalHit, effectStats, (float) parseIntNet(group(match, "dur")), 60f, 0.10f));
        }
        // Spell Power
        else if ((match = match(line, "Each time one of your spells deals periodic damage, you have a chance to gain (?<amount>\\d\\d*) spell power for (?<dur>\\d\\d*) sec")).find()) {
            // Phylactery of the Nameless Lich
            Stats effectStats = new Stats();
            effectStats.setSpellPower(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.DoTTick, effectStats, parseIntNet(group(match, "dur")), 100f, 0.30f));
        } else if ((match = match(line, "Each time you deal spell damage to an opponent, you gain (?<amount>\\d\\d*) spell power for the next (?<dur>\\d\\d*) sec, stacking up to (?<stacks>\\d\\d*) times")).find()) {
            // Muradin's Spyglass
            Stats effectStats = new Stats();
            effectStats.setSpellPower(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageSpellHit, effectStats, parseIntNet(group(match, "dur")), 0f, 1f, parseIntNet(group(match, "stacks"))));
        } else if ((match = match(line, "Each time you cast a damaging or healing spell you gain (?<amount>\\d\\d*) spell power for the next (?<dur>\\d\\d*) sec, stacking up to (?<stacks>\\d\\d*) times")).find()) {
            // Illustration of the Dragon Soul | Eye of the Broodmother
            Stats effectStats = new Stats();
            effectStats.setSpellPower(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCast, effectStats, parseIntNet(group(match, "dur")), 0f, 1f, parseIntNet(group(match, "stacks"))));
        } else if ((match = match(line, "Your spells have a chance to increase your spell power by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            // Pandora's Plea
            Stats effectStats = new Stats();
            effectStats.setSpellPower(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCast, effectStats, parseIntNet(group(match, "dur")), 45f, 0.10f));
        } else if ((match = match(line, "Each time you cast a harmful spell, you have a chance to gain (?<amount>\\d\\d*) spell power for (?<dur>\\d\\d*) sec")).find()) {
            // Abyssal Rune
            Stats effectStats = new Stats();
            effectStats.setSpellPower(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageSpellCast, effectStats, parseIntNet(group(match, "dur")), 45f, 0.25f));
        } else if ((match = match(line, "Your spell casts have a chance to increase your spell power by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            // Flow of Knowledge
            Stats effectStats = new Stats();
            effectStats.setSpellPower(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCast, effectStats, parseIntNet(group(match, "dur")), 45, 0.10f));
        } else if ((match = match(line, "Your spell critical strikes have a chance to increase your spell power by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            Stats effectStats = new Stats();
            effectStats.setSpellPower(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCrit, effectStats, parseIntNet(group(match, "dur")), 45, 0.20f));
        } else if ((match = match(line, "Gives a chance when your harmful spells land to increase the damage of your spells and effects by up to (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            Stats effectStats = new Stats();
            effectStats.setSpellPower(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageSpellHit, effectStats, parseIntNet(group(match, "dur")), 45, 0.10f));
        } else if ((match = match(line, "Your offensive spells have a chance on hit to increase your spell power by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            // Ashen Band of Destruction - seems to be 60 sec iCD (wowhead)
            float internalCooldown = 45.0f;
            if (id == 50397 || id == 50398) {
                internalCooldown = 60.0f;
            }
            Stats effectStats = new Stats();
            effectStats.setSpellPower((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageSpellHit, effectStats, (float) parseIntNet(group(match, "dur")), internalCooldown, 0.10f));
        } else if ((match = match(line, "Your damaging and healing spells have a chance to increase your spell power by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            // Forge Ember
            // This is a nasty trick for compatibility = when designing a healer, please use this version:
            Stats effectStats = new Stats();
            effectStats.setSpellPower(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCast, effectStats, parseIntNet(group(match, "dur")), 45, 0.10f));
        } else if ((match = match(line, "Your harmful spells have a chance to increase your spell power by (?<amount>\\d+) for (?<dur>\\d+) sec")).find()) {
            // Sundial of the Exiled (NOT FOR HEALERS) | Flare of the Heavens
            Stats effectStats = new Stats();
            effectStats.setSpellPower(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageSpellCast, effectStats, parseIntNet(group(match, "dur")), 45, 0.10f));
        }
        // Mp5
        else if ((match = match(line, "Each time you cast a spell, you gain (?<amount>\\d\\d*) mana per 5 sec for (?<dur>\\d\\d*) sec Stacks up to (?<stacks>\\d\\d*) times")).find()) {
            // Solace of the Defeated
            Stats effectStats = new Stats();
            effectStats.setMp5((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCast, effectStats, (float) parseIntNet(group(match, "dur")), 0f, 1f, parseIntNet(group(match, "stacks"))));
        } else if ((match = match(line, "Each time you cast a (damaging or healing |)spell, there is a chance you will gain up to (?<amount>\\d\\d*) mana per 5 for (?<dur>\\d\\d*) sec")).find()) {
            Stats effectStats = new Stats();
            effectStats.setMp5((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCast, effectStats, (float) parseIntNet(group(match, "dur")), 45f, 0.10f));
        } else if ((match = match(line, "Your spell casts have a chance to grant (?<amount>\\d\\d*) mana per 5 sec for (?<dur>\\d\\d*) sec")).find()) {
            // Purified Lunar Dust
            Stats effectStats = new Stats();
            effectStats.setMp5((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCast, effectStats, (float) parseIntNet(group(match, "dur")), 45f, 0.10f));
        }
        // Damaging Procs
        else if ((match = match(line, "Sends a shadowy bolt at the enemy causing (?<min>\\d\\d*) to (?<max>\\d\\d*) Shadow damage.*")).find()) {
            // Raging Deathbringer / Empowered Deathbringer
            int min = parseIntNet(group(match, "min"));
            int max = parseIntNet(group(match, "max"));
            Stats effectStats = new Stats();
            effectStats.setShadowDamage((float) (min + max) / 2f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.CurrentHandHit, effectStats, 0f, 0f, 0.05f)); // proc chance is ~5% according to wowhead.
        } else if ((match = match(line, "Your harmful spells have a chance to strike your enemy, dealing (?<min>\\d\\d*) to (?<max>\\d\\d*) shadow damage.*")).find()) {
            // Pendulum of Telluric Currents
            int min = parseIntNet(group(match, "min"));
            int max = parseIntNet(group(match, "max"));
            Stats effectStats = new Stats();
            effectStats.setShadowDamage((float) (min + max) / 2f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.SpellHit, effectStats, 0f, 45f, 0.10f));
        } else if ((match = match(line, "Each time one of your spells deals periodic damage, there is a chance (?<min>\\d\\d*) to (?<max>\\d\\d*) additional damage will be dealt.*")).find()) {
            // Extract of Necromantic Power
            stats.setExtractOfNecromanticPowerProc(stats.getExtractOfNecromanticPowerProc() + 1);
            int min = parseIntNet(group(match, "min"));
            int max = parseIntNet(group(match, "max"));
            Stats effectStats = new Stats();
            effectStats.setShadowDamage((float) (min + max) / 2f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.DoTTick, effectStats, 0f, 45f, 0.10f));
        } else if ((match = match(line, "Each time you deal damage, you have a chance to do an additional (?<min>\\d\\d*) to (?<max>\\d\\d*) Shadow damage.*")).find()) {
            // Darkmoon Card: Death
            stats.setDarkmoonCardDeathProc(stats.getDarkmoonCardDeathProc() + 1);
            int min = parseIntNet(group(match, "min"));
            int max = parseIntNet(group(match, "max"));
            Stats effectStats = new Stats();
            effectStats.setShadowDamage((float) (min + max) / 2f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageDone, effectStats, 0f, 45f, 0.35f));
        } else if ((match = match(line, "Your melee and ranged attacks have a chance to strike your enemy, dealing (?<min>\\d\\d*) to (?<max>\\d\\d*) arcane damage.*")).find()) {
            // Bandit's Insignia
            int min = parseIntNet(group(match, "min"));
            int max = parseIntNet(group(match, "max"));
            Stats effectStats = new Stats();
            effectStats.setArcaneDamage((float) (min + max) / 2f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalHit, effectStats, 0f, 45f, 0.15f));
        } else if ((match = match(line, "Delivers a fatal wound for (?<min>\\d\\d*) damage")).find()) {
            // Tempered Vis'kag the bloodletter
            int min = parseIntNet(group(match, "min"));
            Stats effectStats = new Stats();
            effectStats.setPhysicalDamage((float) min);
            stats.addSpecialEffect(new SpecialEffect(Trigger.CurrentHandHit, effectStats, 0f, 0f, 0.05f));
        }
        // Spirit
        else if ((match = match(line, "Each time you cast a spell you gain (?<amount>\\d\\d*) Spirit for the next (?<dur>\\d\\d*) sec, stacking up to (?<stacks>\\d\\d*) times")).find()) {
            // Majestic Dragon Figurine
            Stats effectStats = new Stats();
            effectStats.setSpirit(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCast, effectStats, parseIntNet(group(match, "dur")), 0f, 1f, parseIntNet(group(match, "stacks"))));
        }
        // Armor
        else if ((match = match(line, "Each time a melee attack strikes you, you have a chance to gain (?<amount>\\d\\d*) armor for (?<dur>\\d\\d*) sec")).find()) {
            // The Black Heart
            Stats effectStats = new Stats();
            effectStats.setBonusArmor(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageTakenPhysical, effectStats, parseIntNet(group(match, "dur")), 45f, 0.25f));
        }
        // Specialty Stat Procs
        else if ((match = match(line, "When you heal or deal damage you have a chance to gain Greatness")).find()) {
            // Darkmoon Card: Greatness
            Stats effectStats = new Stats();
            effectStats.setHighestStat(300f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageOrHealingDone, effectStats, 15f, 45f, .33f));
        } else if ((match = match(line, "Your harmful spells have a chance to cause you to summon a Val'kyr to fight by your side for 30 sec")).find()) {
            // Nibelung
            // source http://elitistjerks.com/f75/t37825-rawr_mage/p42/#post1517923
            // 5% crit rate, 1.5 crit multiplier, not affected by talents, affected only by target debuffs
            float damage = (id == 50648) ? 30000 : 27008; // enter value for heroic when it becomes known
            Stats effectStats = new Stats();
            effectStats.setValkyrDamage(damage);
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageSpellCast, effectStats, 0f, 0f, 0.02f));
        }
        // Misc
        else if (line.startsWith("When struck in combat has a chance of shielding you in a protective barrier which will reduce damage from each attack by 140")) {
            // Essence of Gossamer
            // 140 damage reduced from EACH attack.
            // Lasts 10 secs.
            // So this isn't a normal X amount absorbed period over 10 sec. like a Corroded Skeleton Key or priest's bubble.
            Stats effectStats = new Stats();
            effectStats.setDamageAbsorbed(140f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageTaken, effectStats, 10, 1, 0.05f));
        } else if (line.startsWith("Protected from the cold.  Your Frost resistance is increased by 20")) {
            // Mechanized Snow Goggles of the...
            stats.setFrostResistance(20);
        } else if ((match = match(line, "You gain (?:a|an) (?<buffName>[\\w\\s]+) each time you cause a (?<trigger>non-periodic|damaging)+ spell critical strike\\.(?:\\s|nbsp;)+When you reach (?<stackSize>\\d+) [\\w\\s]+, they will release, firing (?<projectile>[\\w\\s]+) for (?<mindmg>\\d+) to (?<maxdmg>\\d+) damage\\.(?:\\s|nbsp;)+[\\w\\s]+ cannot be gained more often than once every (?<icd>\\d+(?:\\.\\d+)?) sec")).find()) {
            // Capacitor like procs
            String buffName = trimStartChar(group(match, "buffName"), ' ');
            int stackSize = parseIntNet(group(match, "stackSize"));
            String projectile = trimStartChar(group(match, "projectile"), ' ');
            int mindmg = parseIntNet(group(match, "mindmg"));
            int maxdmg = parseIntNet(group(match, "maxdmg"));
            float avgdmgperstack = (mindmg + maxdmg) / 2f / stackSize;
            float icd = Float.parseFloat(group(match, "icd"));
            Trigger trigger = Trigger.SpellCrit;
            if (group(match, "trigger").startsWith("damaging")) {
                trigger = Trigger.DamageSpellCrit;
            }
            Stats projectileStats = new Stats();

            if (projectile.startsWith("a Lightning Bolt")) {
                projectileStats.setNatureDamage(avgdmgperstack);
            } else if (projectile.startsWith("a Pillar of Flame")) {
                projectileStats.setFireDamage(avgdmgperstack);
            }
            stats.addSpecialEffect(new SpecialEffect(trigger, projectileStats, 0f, icd));
        }
        // Shadowmourne
        // Your melee attacks have a chance to drain a Soul Fragment granting you 30 Strength.  When you have acquired 10 Soul Fragments you will unleash Chaos Bane, dealing 1900 to 2100 Shadow damage split between all enemies within 15 yards and granting you 270 Strength for 10 sec.
        else if ((match = match(line, "Your melee attacks have a chance to drain a Soul Fragment granting you 30 Strength")).find()) {
            // Capacitor like procs
            Stats BuffStats = new Stats();
            Stats BuffStats2 = new Stats();
            // How much strength per stack.
            BuffStats.setStrength(30);
            // Stack size
            int stackSize = 10;
            // Damage of the final proc.
            int mindmg = 1900;
            int maxdmg = 2100;
            float avgdmg = (mindmg + maxdmg) / 2f;
            // 2ndary Strength Buff
            BuffStats2.setStrength(270);
            BuffStats2.setShadowDamage(avgdmg);
            float icd = 10;
            Trigger trigger = Trigger.MeleeHit;
            // At 10 stacks, the stacks proc the 2nd effect, so you would never have 10 stacks.
            stats.addSpecialEffect(new SpecialEffect(trigger, BuffStats, 60f, 0f, .2f, (stackSize - 1)));
            stats.addSpecialEffect(new SpecialEffect(trigger, BuffStats2, icd, 0f, .02f));
        } else if (line.startsWith("Increases your pet's critical strike chance by ")) {
            String critChance = line.substring("Increases your pet's critical strike chance by ".length()).trim();
            if (critChance.endsWith("%")) {
                stats.setBonusPetCritChance(Float.parseFloat(critChance.substring(0, critChance.length() - 2)) / 100f);
            }
        } else if (line.startsWith("Increases damage dealt by your pet by ")) {
            String critChance = line.substring("Increases damage dealt by your pet by ".length()).trim();
            if (critChance.endsWith("%")) {
                stats.setBonusPetDamageMultiplier(Float.parseFloat(critChance.substring(0, critChance.length() - 2)) / 100f);
            }
        } else if (line.startsWith("Your healing spells have a chance to make your next heal cast within 15 sec cost 800 less mana")) {
            // Soul Preserver
            // This is how RestoSham does it:
            stats.setManacostReduceWithin15OnHealingCast(stats.getManacostReduceWithin15OnHealingCast() + 800);
            // And that is how Tree does it:
            Stats effectStats = new Stats();
            effectStats.setHealingOmenProc(800);
            stats.addSpecialEffect(new SpecialEffect(Trigger.HealingSpellCast, effectStats, 0, 0, 0.02f));
        } else if ((match = match(line, "Reduces the base mana cost of your spells by (?<amount>\\d+)")).find()) {
            stats.setSpellsManaReduction(parseIntNet(group(match, "amount")));
        } else if (line.startsWith("Your spell critical strikes have a chance to restore 900 mana")) {
            // Soul of the Dead
            Stats effectStats = new Stats();
            effectStats.setManaRestore(900f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCrit, effectStats, 0f, 45f, .25f));
        } else if (line.startsWith("Your direct healing spells have a chance to place a heal over time on your target")) {
            Pattern r = pattern("Your direct healing spells have a chance to place a heal over time on your target, healing (?<hot>\\d*) over (?<dur>\\d*) sec");
            Matcher m = r.matcher(line);
            if (m.find()) {
                float hot = parseIntNet(group(m, "hot"));
                float dur = parseIntNet(group(m, "dur"));
                // internal cooldown: 45 seconds
                // 20% chance, so on average procs after 5 casts
                Stats effectStats = new Stats();
                effectStats.setHealed(hot);
                stats.addSpecialEffect(new SpecialEffect(Trigger.HealingSpellCast, effectStats, 0f, 45f, .2f));
            }
        } else if ((match = match(line, "Grants the wielder (?<amount1>\\d\\d*) defense rating and (?<amount2>\\d\\d*) armor for (?<dur>\\d\\d*) sec")).find()) {
            // Quel'Serrar Sanctuary proc (all versions)
            Stats effectStats = new Stats();
            effectStats.setDefenseRating(parseIntNet(group(match, "amount1")));
            effectStats.setBonusArmor(parseIntNet(group(match, "amount2")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.MeleeHit, effectStats, parseIntNet(group(match, "dur")), 0f, -2f));
        } else if ((match = match(line, "Steals (?<amount1>\\d\\d*) to (?<amount2>\\d\\d*) life from target enemy.*")).find()) {
            Stats effectStats = new Stats();
            effectStats.setShadowDamage((parseIntNet(group(match, "amount1")) + parseIntNet(group(match, "amount2"))) / 2f);
            //ProcdShadowDamageMin = int.Parse(match.Groups["amount1"].Value),
            //ProcdShadowDamageMax = int.Parse(match.Groups["amount2"].Value),
            // Stealing health means that some is restored to the user.
            effectStats.setHealthRestore((parseIntNet(group(match, "amount1")) + parseIntNet(group(match, "amount2"))) / 2f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.MeleeHit, effectStats, 0f, 0f, -2f));
        } else if (line.equals("Your healing spells have a chance to cause Blessing of Ancient Kings for 15 sec allowing your heals to shield the target absorbing damage equal to 15% of the amount healed")) {
            // Val'anyr, Hammer of Ancient Kings effect
            Stats effectStats = new Stats();
            effectStats.setShieldFromHealed(.15f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.HealingSpellHit, effectStats, 15f, 45f, .1f));
        } else if (line.endsWith("Weapon Damage")) {
            line = trimChar(line, '+').substring(0, line.indexOf(" "));
            stats.setWeaponDamage(stats.getWeaponDamage() + parseIntNet(line));
        }
        // Special Armory Tags
        else if (isArmory && line.startsWith("Increases spell power by")) {
            line = line.substring("Increases spell power by".length());
            line = line.replace(".", "").replace(" ", "");
            stats.setSpellPower(stats.getSpellPower() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases damage done by Shadow spells and effects by up to")) {
            line = line.substring("Increases damage done by Shadow spells and effects by up to".length());
            line = line.replace(".", "").replace(" ", "");
            stats.setSpellShadowDamageRating(stats.getSpellShadowDamageRating() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases damage done by Fire spells and effects by up to")) {
            line = line.substring("Increases damage done by Fire spells and effects by up to".length());
            line = line.replace(".", "").replace(" ", "");
            stats.setSpellFireDamageRating(stats.getSpellFireDamageRating() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases damage done by Frost spells and effects by up to")) {
            line = line.substring("Increases damage done by Frost spells and effects by up to".length());
            line = line.replace(".", "").replace(" ", "");
            stats.setSpellFrostDamageRating(stats.getSpellFrostDamageRating() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases damage done by Arcane spells and effects by up to")) {
            line = line.substring("Increases damage done by Arcane spells and effects by up to".length());
            line = line.replace(".", "").replace(" ", "");
            stats.setSpellArcaneDamageRating(stats.getSpellArcaneDamageRating() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases damage done by Nature spells and effects by up to")) {
            line = line.substring("Increases damage done by Nature spells and effects by up to".length());
            line = line.replace(".", "").replace(" ", "");
            stats.setSpellNatureDamageRating(stats.getSpellNatureDamageRating() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Improves haste rating by")) {
            line = line.substring("Improves haste rating by".length());
            line = line.replace(".", "").replace(" ", "");
            stats.setHasteRating(stats.getHasteRating() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Improves critical strike rating by")) {
            line = line.substring("Improves critical strike rating by".length());
            line = line.replace(".", "").replace(" ", "");
            stats.setCritRating(stats.getCritRating() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases your spell penetration by")) {
            line = line.substring("Increases your spell penetration by".length());
            line = line.replace(".", "").replace(" ", "");
            stats.setSpellPenetration(stats.getSpellPenetration() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases hit rating by ")) {
            line = line.substring("Increases hit rating by ".length());
            line = line.replace(".", "").replace(" ", "");
            stats.setHitRating(stats.getHitRating() + parseIntNet(line));
        }
        // Restores 7 mana per 5 sec
        // Check to see if the desc contains the token 'mana'.  Items like Frostwolf Insignia
        // and Essense Infused Shroom Restore health.
        else if (isArmory && line.startsWith("Restores ") && line.contains("mana") && !line.contains("kill a target")) {
            line = line.substring("Restores ".length());
            line = line.substring(0, line.indexOf(" mana"));
            stats.setMp5(stats.getMp5() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases attack power by ") && !line.contains("forms only")) {
            line = line.substring("Increases attack power by ".length());
            if (line.contains(".")) {
                line = line.substring(0, line.indexOf("."));
            }
            if (line.contains(" ")) {
                line = line.substring(0, line.indexOf(" "));
            }
            stats.setAttackPower(stats.getAttackPower() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases defense rating by ")) {
            line = line.substring("Increases defense rating by ".length());
            if (line.contains(".")) {
                line = line.substring(0, line.indexOf("."));
            }
            if (line.contains(" ")) {
                line = line.substring(0, line.indexOf(" "));
            }
            stats.setDefenseRating(stats.getDefenseRating() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases your dodge rating by ")) {
            line = line.substring("Increases your dodge rating by ".length());
            if (line.contains(".")) {
                line = line.substring(0, line.indexOf("."));
            }
            if (line.contains(" ")) {
                line = line.substring(0, line.indexOf(" "));
            }
            stats.setDodgeRating(stats.getDodgeRating() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases your parry rating by ")) {
            line = line.substring("Increases your parry rating by ".length());
            if (line.contains(".")) {
                line = line.substring(0, line.indexOf("."));
            }
            if (line.contains(" ")) {
                line = line.substring(0, line.indexOf(" "));
            }
            stats.setParryRating(stats.getParryRating() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases the block value of your shield by ")) {
            line = line.substring("Increases the block value of your shield by ".length());
            if (line.contains(".")) {
                line = line.substring(0, line.indexOf("."));
            }
            if (line.contains(" ")) {
                line = line.substring(0, line.indexOf(" "));
            }
            stats.setBlockValue(stats.getBlockValue() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases your shield block rating by ")) {
            line = line.substring("Increases your shield block rating by ".length());
            if (line.contains(".")) {
                line = line.substring(0, line.indexOf("."));
            }
            if (line.contains(" ")) {
                line = line.substring(0, line.indexOf(" "));
            }
            stats.setBlockRating(stats.getBlockRating() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases your block rating by ")) {
            line = line.substring("Increases your block rating by ".length());
            if (line.contains(".")) {
                line = line.substring(0, line.indexOf("."));
            }
            if (line.contains(" ")) {
                line = line.substring(0, line.indexOf(" "));
            }
            stats.setBlockRating(stats.getBlockRating() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases your hit rating by ")) {
            line = line.substring("Increases your hit rating by ".length());
            if (line.contains(".")) {
                line = line.substring(0, line.indexOf("."));
            }
            if (line.contains(" ")) {
                line = line.substring(0, line.indexOf(" "));
            }
            stats.setHitRating(stats.getHitRating() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases your armor penetration rating by ")) {
            line = line.substring("Increases your armor penetration rating by ".length());
            if (line.contains(".")) {
                line = line.substring(0, line.indexOf("."));
            }
            if (line.contains(" ")) {
                line = line.substring(0, line.indexOf(" "));
            }
            stats.setArmorPenetrationRating(stats.getArmorPenetrationRating() + parseIntNet(line));
        } else if (isArmory && line.startsWith("Increases armor penetration rating by ")) {
            line = line.substring("Increases armor penetration rating by ".length());
            if (line.contains(".")) {
                line = line.substring(0, line.indexOf("."));
            }
            if (line.contains(" ")) {
                line = line.substring(0, line.indexOf(" "));
            }
            stats.setArmorPenetrationRating(stats.getArmorPenetrationRating() + parseIntNet(line));
        }
        // 3.0.1 Trinkets
        else if ((match = match(line, "Chance on melee or ranged attack to enter Wracking Pains, during which your attacks will each grant 15 crit rating, stacking up to 10 times. Expires after 20 sec")).find()) {
            // Death Knight's Anguish
            SpecialEffect primary = new SpecialEffect(Trigger.PhysicalHit, new Stats(), 20f, 45f, 0.10f);
            Stats secondaryStats = new Stats();
            secondaryStats.setCritRating(15);
            SpecialEffect secondary = new SpecialEffect(Trigger.PhysicalHit, secondaryStats, 20f, 0f, 1f, 10);
            primary.getStats().addSpecialEffect(secondary);
            stats.addSpecialEffect(primary);
        }
        // 3.2 Trinkets
        else if ((match = match(line.replace("nbsp;", " "), "When you deal damage you have a chance to gain Paragon, increasing your Strength or Agility by (?<amount>\\d+) for 15 sec Your highest stat is always chosen")).find()) {
            Stats effectStats = new Stats();
            effectStats.setParagon(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageDone, effectStats, 15f, 45f, 0.35f));
        } else if ((match = match(line, "Each time you cast a helpful spell, you have a chance to gain (?<amount>\\d+) mana")).find()) {
            // Ephemeral Snowflake
            Stats effectStats = new Stats();
            effectStats.setManaRestore(parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.SpellCast, effectStats, 0f, 45f, 0.4f));
        }
        // Icecrown Weapon Procs
        else if ((match = match(line, "Your melee attacks have a chance to grant you Necrotic Touch for 10 sec, causing all your melee attacks to deal an additional (?<amount>\\d+)% damage as shadow damage")).find()) {
            // Black Bruise
            Stats effectStats = new Stats();
            effectStats.setBonusPhysicalDamageMultiplier(parseIntNet(group(match, "amount")) / 100f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.MeleeHit, effectStats, 10f, 0f, 0.033f));
        } else if ((match = match(line, "Your ranged attacks have a (?<amount1>\\d\\d*)% chance to cause you to instantly attack with this weapon for 50% weapon damage")).find()) {
            // Zod, kneel before him
            stats.setZodProc((ilvl == 277 ? 0.05f : 0.04f));
        } else if ((match = match(line, "Each time your spells heal a target you have a chance to cause the target of your heal to heal themselves and friends within 10 yards for (?<amount>\\d+) each sec for 6 sec")).find()) {
            // Trauma; Procs on Healing Hit and HoTs; Hits up to 7 people
            Stats effectStats = new Stats();
            effectStats.setHealed(parseIntNet(group(match, "amount")) * 7f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.HealingSpellHit, effectStats, 6f, 0f, 0.01f, 0));
        } else if ((match = match(line, "Your melee attacks have a chance to grant you Blessing of Light, increasing your strength by (?<stramount>\\d+) and your healing received by up to (?<healamount>\\d+) for 10 sec")).find()) {
            // Last Word
            Stats effectStats = new Stats();
            effectStats.setStrength(parseIntNet(group(match, "stramount")));
            effectStats.setBonusHealingReceived(parseIntNet(group(match, "healamount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.HealingSpellHit, effectStats, 10f, 0f, 0.37f, 1));
        }
        // 3.3 Trinkets
        else if ((match = match(line, "Each time you are struck by a melee attack, you have a 60% chance to gain (?<stamina>\\d+) stamina for the next 10 sec, stacking up to 10 times")).find()) {
            // Unidentifiable Organ
            Stats effectStats = new Stats();
            effectStats.setStamina(parseIntNet(group(match, "stamina")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageTakenPhysical, effectStats, 10.0f, 0.0f, 0.6f, 10));
        } else if (line.startsWith("Your attacks have a chance to awaken the powers of the races of Northrend, temporarily transforming you and increasing your combat capabilities for 30 sec")) {
            // Deathbringer's Will
            Stats effectStats = new Stats();
            effectStats.setDeathbringerProc(ilvl == 277 ? 700 : 600);
            stats.addSpecialEffect(new SpecialEffect(Trigger.PhysicalHit, effectStats, 30f, 105f, 0.15f));
        } else if ((match = match(line, "Your melee attacks have a chance to grant you a Mote of Anger\\. (nbsp;| )?When you reach (?<amount>\\d+) Motes of Anger, they will release, causing you to instantly attack for 50% weapon damage with one of your melee weapons")).find()) {
            // Tiny Abomination Jar
            Stats effectStats = new Stats();
            effectStats.setMoteOfAnger(0.5f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.MeleeAttack, effectStats, 0f, 0f, 0.5f, parseIntNet(group(match, "amount"))));
        } else if ((match = match(line, "You gain (?<mana>\\d+) mana each time you heal a target with one of your spells")).find()) {
            // Epheremal Snowflake - assume iCD of 0.33 sec (wowhead)
            Stats effectStats = new Stats();
            effectStats.setManaRestore(parseIntNet(group(match, "mana")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.HealingSpellHit, effectStats, 0f, 0.33f, 1f));
        } else if ((match = match(line, "Each time your spells heal a target you have a chance to cause another nearby friendly target to be instantly healed for (?<min>\\d\\d*) to (?<max>\\d\\d*)")).find()) {
            // Althor's Abacus
            float min = (float) parseIntNet(group(match, "min"));
            float max = (float) parseIntNet(group(match, "max"));
            Stats effectStats = new Stats();
            effectStats.setHealed(min + (max - min) / 2f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.HealingSpellHit, effectStats, 0f, 45f, 0.3f));
        } else if ((match = match(line, "Your harmful spells have a chance to increase your spell power by (?<initialSP>\\d+) and an additional (?<additionalSP>\\d+) every (?<frequency>\\d+) sec for (?<time>\\d+) sec")).find()) {
            // Dislodged Foreign Object - seems to be 45 sec iCD (wowhead)
            float initialSP = (float) parseIntNet(group(match, "initialSP"));
            float additionalSP = (float) parseIntNet(group(match, "additionalSP"));
            float frequency = (float) parseIntNet(group(match, "frequency"));
            float time = (float) parseIntNet(group(match, "time"));
            float min = initialSP;
            float max = initialSP + additionalSP * (time / frequency - 1f);
            float averageSP = (min + max) / 2f;
            Stats effectStats = new Stats();
            effectStats.setSpellPower(averageSP);
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageSpellHit, effectStats, time, 45f, 0.1f));
        } else if ((match = match(line, "Melee attacks which reduce you below 35% health cause you to gain (?<armor>\\d+) armor for 10 sec")).find()) {
            // Corpse Tongue Coin
            // Melee attacks which reduce you below 35% health cause you to gain 5712 armor for 10 sec.  Cannot occur more than once every 30 sec.
            float fArmor = (float) parseIntNet(group(match, "armor"));
            float fDuration = 10;
            float fICD = 30;
            // Assuming the target will be under 35% health for that amount of time.
            float fChance = .35f / 2f;
            Stats effectStats = new Stats();
            effectStats.setBonusArmor(fArmor);
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageTakenPhysical, effectStats, fDuration, fICD, fChance));
        }
        // 3.3 rings
        else if ((match = match(line, "Your helpful spells have a chance to increase your spell power by (?<power>\\d+) for (?<time>\\d+) sec")).find()) {
            // Ashen Band of Wisdom - seems to be 60 sec iCD (wowhead)
            float spellpower = (float) parseIntNet(group(match, "power"));
            float time = (float) parseIntNet(group(match, "time"));
            Stats effectStats = new Stats();
            effectStats.setSpellPower(spellpower);
            stats.addSpecialEffect(new SpecialEffect(Trigger.HealingSpellCast, effectStats, time, 60f, 0.1f));
        } else if ((match = match(line, "When struck in combat has a chance of increasing your armor by (?<armor>\\d+) for (?<time>\\d+) sec")).find()) {
            // Ashen Band of Courage - seems to be 60 sec iCD (wowhead)
            // Apparently confirmed as 3% chance to proc -> http://maintankadin.failsafedesign.com/forum/index.php?f=21&t=27115&rb_v=viewtopic
            float armor = (float) parseIntNet(group(match, "armor"));
            float time = (float) parseIntNet(group(match, "time"));
            Stats effectStats = new Stats();
            effectStats.setBonusArmor(armor);
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageTaken, effectStats, time, 60f, 0.03f));
        }
        // 3.3.5 Trinkets
        // Note that Sharpened Twilight Scale already is modeled via Whispering Fanged Skull
        else if ((match = match(line, "Melee attacks which reduce you below 35% health cause you to gain (?<amount>\\d+) dodge rating for 10 sec")).find()) {
            // Petrified Twilight Scale
            // Melee attacks which reduce you below 35% health cause you to gain 733 dodge rating for 10 sec.  Cannot occur more than once every 45 sec.
            float fDodge = parseIntNet(group(match, "amount"));
            float fDuration = 10;
            float fICD = 45;
            // Assuming the target will be under 35% health for that amount of time.
            float fChance = .35f / 2f;
            Stats effectStats = new Stats();
            effectStats.setDodgeRating(fDodge);
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageTakenPhysical, effectStats, fDuration, fICD, fChance));
        } else if ((match = match(line, "Your damaging spells have a chance to grant (?<spellPower>\\d+) spell power for 15 sec")).find()) {
            // Charred Twilight Scale
            float spellPower = parseIntNet(group(match, "spellPower"));
            Stats effectStats = new Stats();
            effectStats.setSpellPower(spellPower);
            stats.addSpecialEffect(new SpecialEffect(Trigger.DamageSpellHit, effectStats, 15.0f, 45.0f, 0.1f));
        } else {
            // Prototype Sigil Code:
            // (the original keeps a long comment block listing sigil tooltips here)
            // Single Ability.
            Pattern regex = pattern("Your (?<ability>\\w+\\s*\\w*) (ability )?(also )?grants (you )?(?<amount>\\d*) (?<stat>\\w+\\s*\\w*) for (?<duration>\\d*) sec(\\s*Stacks up to (?<stack>\\d*) times)?");
            match = regex.matcher(line);
            if (match.find()) {
                String statName = group(match, "stat");
                float amount = parseIntNet(group(match, "amount"));
                float duration = parseIntNet(group(match, "duration"));
                String ability = group(match, "ability");
                int stack = 0;
                if (!group(match, "stack").isEmpty()) {
                    stack = parseIntNet(group(match, "stack"));
                }

                stats.addSpecialEffect(evalRegex(statName, amount, duration, ability, 0f, 1f, stack));
            }
            // ok... at this point with the way the code is written, this is really just for Sigil of the Unfaltering Knight, which grants a
            // 30 sec buff of 53 Def. Since it's a triggered event, and all, it's better to go off the trigger rather than a straight
            // 53 def all the time.  So adjusting this to be a special effect and assuming 30 secs.
            regex = pattern("Your (?<ability>\\w+\\s*\\w*) (ability )?(will )?(also )?increase (your )?(?<stat>\\w+\\s*\\w*) by (?<amount>\\d*)");
            match = regex.matcher(line);
            if (match.find()) {
                String statName = group(match, "stat");
                float amount = parseIntNet(group(match, "amount"));
                String ability = group(match, "ability");

                stats.addSpecialEffect(evalRegex(statName, amount, 30, ability, 0f));
            }

            regex = pattern("Your (?<ability>\\w+\\s*\\w*) and (?<ability2>\\w+\\s*\\w*) (abilities )?have a chance to grant (?<amount>\\d*) (?<stat>\\w+[\\s\\w]*) for (?<duration>\\d*) sec");
            match = regex.matcher(line);
            if (match.find()) {
                String statName = group(match, "stat");
                float amount = parseIntNet(group(match, "amount"));
                float duration = parseIntNet(group(match, "duration"));
                String ability = group(match, "ability");
                String ability2 = group(match, "ability2");

                SpecialEffect SE1 = evalRegex(statName, amount, duration, ability, 0f, 0.15f);
                stats.addSpecialEffect(SE1);
                SpecialEffect SE2 = evalRegex(statName, amount, duration, ability2, 0f, 0.15f);
                if (effectTextDiffers(SE1, SE2)) {
                    stats.addSpecialEffect(SE2);
                }
            }
            /*
             * Sigil of the Hanged Man
             * Equip: Your Obliterate, Scourge Strike, and Death Strike abilities grants 73 Strength for 15 sec.  Stacks up to 3 times.
             */
            regex = pattern("Your (?<ability>\\w+\\s*\\w*), (?<ability2>\\w+\\s*\\w*), and (?<ability3>\\w+\\s*\\w*) (abilities )?grants (?<amount>\\d*) (?<stat>\\w+[\\s\\w]*) for (?<duration>\\d*) sec Stacks up to (?<stacks>\\d*) times");
            match = regex.matcher(line);
            if (match.find()) {
                String statName = group(match, "stat");
                float amount = parseIntNet(group(match, "amount"));
                float duration = parseIntNet(group(match, "duration"));
                int stacks = parseIntNet(group(match, "stacks"));
                String ability = group(match, "ability");
                String ability2 = group(match, "ability2");
                String ability3 = group(match, "ability3");

                SpecialEffect SE1 = evalRegex(statName, amount, duration, ability, 0f, 1f, stacks);
                stats.addSpecialEffect(SE1);
                SpecialEffect SE2 = evalRegex(statName, amount, duration, ability2, 0f, 1f, stacks);
                SpecialEffect SE3 = evalRegex(statName, amount, duration, ability3, 0f, 1f, stacks);
                if (effectTextDiffers(SE1, SE2)) {
                    stats.addSpecialEffect(SE2);
                }
                if (effectTextDiffers(SE3, SE2) && effectTextDiffers(SE3, SE1)) {
                    stats.addSpecialEffect(SE3);
                }
            }

            regex = pattern("Each time you use your (?<ability3>\\w+\\s*\\w*), (?<ability>\\w+\\s*\\w*), or (?<ability2>\\w+\\s*\\w*) ability, you have a chance to gain (?<amount>\\d*) (?<stat>\\w+[\\s\\w]*) for (?<duration>\\d*) sec");
            match = regex.matcher(line);
            if (match.find()) {
                String statName = group(match, "stat");
                float amount = parseIntNet(group(match, "amount"));
                float duration = parseIntNet(group(match, "duration"));
                String ability = group(match, "ability");
                String ability2 = group(match, "ability2");
                String ability3 = group(match, "ability3");

                SpecialEffect SE1 = evalRegex(statName, amount, duration, ability, 10f, 0.8f);
                stats.addSpecialEffect(SE1);
                SpecialEffect SE2 = evalRegex(statName, amount, duration, ability2, 10f, 0.8f);
                SpecialEffect SE3 = evalRegex(statName, amount, duration, ability3, 10f, 0.8f);
                if (effectTextDiffers(SE1, SE2)) {
                    stats.addSpecialEffect(SE2);
                }
                if (effectTextDiffers(SE3, SE2)) {
                    stats.addSpecialEffect(SE3);
                }
            }

            // Each time you use your [ability] ability, you have a chance to gain [amount] [stat] for [duration] sec
            regex = pattern("Each time you use your (?<ability>\\w+\\s*\\w*) ability, you have a chance to gain (?<amount>\\d*) (?<stat>\\w+[\\s\\w]*) for (?<duration>\\d*) sec");
            match = regex.matcher(line);
            if (match.find()) {
                String statName = group(match, "stat");
                float amount = parseIntNet(group(match, "amount"));
                float duration = parseIntNet(group(match, "duration"));
                String ability = group(match, "ability");

                SpecialEffect SE1 = evalRegex(statName, amount, duration, ability, 0f, 0.8f);
                stats.addSpecialEffect(SE1);
            }

            // Single Ability damage increase.
            regex = pattern("Increases the damage dealt by your (?<ability>\\w+\\s*\\w*) (ability )?by (?<amount>\\d+[.]*\\d*)");
            match = regex.matcher(line);
            if (match.find()) {
                String statName = group(match, "stat");
                float amount = Float.parseFloat(group(match, "amount"));
                String ability = group(match, "ability");
                //string ability2 = match.Groups["ability2"].Value;

                evalAbility(ability, stats, amount);
            }
            // 2 Abilities damage increase.
            regex = pattern("Increases the damage (dealt |done )?by your (?<ability>\\w+\\s*\\w*) and (?<ability2>\\w+\\s*\\w*) (abilities )?by (?<amount>\\d+)");
            match = regex.matcher(line);
            if (match.find()) {
                String statName = group(match, "stat");
                float amount = parseIntNet(group(match, "amount"));
                String ability = group(match, "ability");
                String ability2 = group(match, "ability2");

                evalAbility(ability, stats, amount);
                evalAbility(ability2, stats, amount);
            }
            // 2 Abilities with separate damage increases.
            regex = pattern("Increases the (base )?damage (dealt |done )?by your (?<ability>\\w+\\s*\\w*) ability by (?<amount>\\d+) and by your (?<ability2>\\w+\\s*\\w*) ability by (?<amount2>\\d+)");
            match = regex.matcher(line);
            if (match.find()) {
                String statName = group(match, "stat");
                float amount = parseIntNet(group(match, "amount"));
                float amount2 = parseIntNet(group(match, "amount2"));
                String ability = group(match, "ability");
                String ability2 = group(match, "ability2");

                evalAbility(ability, stats, amount);
                evalAbility(ability2, stats, amount2);
            }
            // 3 Abilities damage increase.
            regex = pattern("Increases the (base )?damage (dealt |done )?by your (?<ability>\\w+\\s*\\w*) by (?<amount>\\d+), your (?<ability2>\\w+\\s*\\w*) by (?<amount2>\\d+), and your (?<ability3>\\w+\\s*\\w*) by (?<amount3>\\d+)");
            match = regex.matcher(line);
            if (match.find()) {
                String statName = group(match, "stat");
                float amount = parseIntNet(group(match, "amount"));
                float amount2 = parseIntNet(group(match, "amount2"));
                float amount3 = parseIntNet(group(match, "amount3"));
                String ability = group(match, "ability");
                String ability2 = group(match, "ability2");
                String ability3 = group(match, "ability3");

                evalAbility(ability, stats, amount);
                evalAbility(ability2, stats, amount2);
                evalAbility(ability3, stats, amount3);
            }
        }
    }

    /**
     * Parses one "Use:" effect line: an on-use effect becomes a {@link SpecialEffect} with
     * {@link Trigger#Use}, the duration from the tooltip and the cooldown in seconds (minute values
     * are multiplied by 60). The generic first pattern requires an "(N Min Cooldown)" suffix; later
     * branches cover tooltips without one and fall back to the original's guessed cooldowns, and a
     * few recognize an item id to correct a wrong cooldown or amount.
     */
    public static void processUseLine(String line, Stats stats, boolean isArmory, int id) {
        Matcher match = match(line, "Increases (your )?(?<stat>\\w\\w*( \\w\\w*)*) by (?<amount>\\+?\\d\\d*)(nbsp;\\<small\\>.*\\<\\/small\\>)?(<a.*q2.*>) for (?<duration>\\d\\d*) sec \\((?<cooldown>\\d\\d*) Min Cooldown\\)");
        // The commented-out "prep the line" block of the original is intentionally not ported (dead code).
        if (match.find()) {
            Stats s = new Stats();
            String statName = group(match, "stat");
            float amount = parseIntNet(group(match, "amount"));
            float duration = parseIntNet(group(match, "duration"));
            float cooldown = parseIntNet(group(match, "cooldown")) * 60;

            if (statName.equalsIgnoreCase("attack power")) {
                s.setAttackPower(amount);
            } else if (statName.equalsIgnoreCase("melee and ranged attack power")) {
                s.setAttackPower(amount);
            } else if (statName.equalsIgnoreCase("haste rating")) {
                s.setHasteRating(amount);
            } else if (statName.equalsIgnoreCase("spell power")) {
                s.setSpellPower(amount);
            } else if (statName.equalsIgnoreCase("agility")) {
                s.setAgility(amount);
            } else if (statName.equalsIgnoreCase("critical strike rating")) {
                s.setCritRating(amount);
            } else if (statName.equalsIgnoreCase("dodge")) {
                s.setDodgeRating(amount);
            } else if (statName.equalsIgnoreCase("dodge rating")) {
                s.setDodgeRating(amount);
            } else if (statName.equalsIgnoreCase("parry")) {
                s.setParryRating(amount);
            } else if (statName.equalsIgnoreCase("parry rating")) {
                s.setParryRating(amount);
            } else if (statName.equalsIgnoreCase("spirit")) {
                s.setSpirit(amount);
            } else if (statName.equalsIgnoreCase("armor penetration rating")) {
                s.setArmorPenetrationRating(amount);
            } else if (statName.equalsIgnoreCase("block value")) {
                s.setBlockValue(amount);
            } else if (statName.equalsIgnoreCase("maximum health")) {
                s.setHealth(amount);
            } else if (statName.equalsIgnoreCase("armor")) {
                s.setBonusArmor(amount);
            } else if (statName.equalsIgnoreCase("the block value of your shield")) {
                s.setBlockValue(amount);
            } else if (statName.equalsIgnoreCase("master rating")) {
                s.setMasteryRating(amount);
            }

            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, s, duration, cooldown));
        }
        // Victor's Call and Vengeance of the Forsaken (232 & 245)
        else if ((match = match(line.replace("  ", " "), "Each time you strike an enemy.*, you gain (?<amount>\\d+) attack power. Stacks up to 5 times. Entire effect lasts 20 sec.*")).find()) {
            SpecialEffect primary = new SpecialEffect(Trigger.Use, new Stats(), 20f, 2f * 60f);
            Stats secondaryStats = new Stats();
            secondaryStats.setAttackPower((float) parseIntNet(group(match, "amount")));
            SpecialEffect secondary = new SpecialEffect(Trigger.MeleeHit, secondaryStats, 20f, 0f, 1f, 5);
            primary.getStats().addSpecialEffect(secondary);
            stats.addSpecialEffect(primary);
        } else if ((match = match(line, "Increases attack power by (?<amount>\\d\\d*) for (?<dur>\\d\\d*) sec")).find()) {
            // Wrath Stone
            Stats effectStats = new Stats();
            effectStats.setAttackPower((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, (float) parseIntNet(group(match, "dur")), 120f));
        }
        // Wrath: Eye of the Night
        else if (isMatch(line, "Increases spell power by (\\d{2}) for all nearby party members.")) {
            String[] inputs = netSplit(line, "Increases spell power by (\\d{2}) for all nearby party members.");
            // the full text reads: "Use: Increases spell power by 34 for all nearby party members.  Lasts 30 min. (1 Hour Cooldown)"
            // So in general you get the 34 spell power for 100% of a given boss fight.  Like most trinkets it is up to the player to
            // use it correctly, so we are going to give them the full power instead of half.
            float spell_power = Float.parseFloat(inputs[1]);
            stats.setSpellPower(stats.getSpellPower() + spell_power);
        }
        // Medallion of Heroism
        else if ((match = match(line, "Heal self for (?<amount1>\\d\\d\\d*) to (?<amount2>\\d\\d\\d*) damage.*")).find()) {
            int val1 = parseIntNet(group(match, "amount1"));
            int val2 = parseIntNet(group(match, "amount2"));
            Stats effectStats = new Stats();
            effectStats.setHealthRestore((float) ((val1 + val2) / 2f));
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 0f, 2 * 60));
        }
        // Increases spell power by 183 for 20 sec.
        else if (isMatch(line, "Increases spell power by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases spell power by (\\d{3}) for (\\d{2}) sec");
            float spell_power = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown is on the next line
            float cooldown_sec = 2.0f * 60.0f;
            // Vengeance of the Illidari (tooltip lies!)
            if (id == 28040) {
                cooldown_sec = 90.0f;
            }
            // we don't have a stat for this case so do the average
            Stats effectStats = new Stats();
            effectStats.setSpellPower(spell_power);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, cooldown_sec));
        }
        // Increases spell power by 183 for 20 sec.
        else if (isMatch(line, "Increases your spell power by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases your spell power by (\\d{3}) for (\\d{2}) sec");
            float spell_power = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown is on the next line
            float cooldown_sec = 2.0f * 60.0f;
            // Twilight Serpent
            if (id == 42395) {
                cooldown_sec = 5.0f * 60.0f;
            }
            // Vengeance of the Illidari (tooltip lies!)
            if (id == 28040) {
                cooldown_sec = 90.0f;
            }
            // we don't have a stat for this case so do the average
            Stats effectStats = new Stats();
            effectStats.setSpellPower(spell_power);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, cooldown_sec));
        }
        // Increases attack power by 183 for 20 sec.
        else if (isMatch(line, "Increases armor penetration rating by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases armor penetration rating by (\\d{3}) for (\\d{2}) sec");
            float armor_penetration = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setArmorPenetrationRating(armor_penetration);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 120f));
        }
        // Increases attack power by 183 for 20 sec.
        else if (isMatch(line, "Increases your armor penetration rating by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases your armor penetration rating by (\\d{3}) for (\\d{2}) sec");
            float armor_penetration = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setArmorPenetrationRating(armor_penetration);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 120f));
        }
        // Increases attack power by 183 for 20 sec.
        else if (isMatch(line, "Increases attack power by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases attack power by (\\d{3}) for (\\d{2}) sec");
            float attack_power = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setAttackPower(attack_power);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 120f));
        }
        // Increases attack power by 183 for 20 sec.
        else if (isMatch(line, "Increases your attack power by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases your attack power by (\\d{3}) for (\\d{2}) sec");
            float attack_power = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setAttackPower(attack_power);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 120f));
        }
        // Increases attack power by 183 for 20 sec.
        else if (isMatch(line, "Increases attack power by (\\d{4}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases attack power by (\\d{4}) for (\\d{2}) sec");
            float attack_power = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setAttackPower(attack_power);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 120f));
        }
        // Increases attack power by 183 for 20 sec.
        else if (isMatch(line, "Increases your attack power by (\\d{4}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases your attack power by (\\d{4}) for (\\d{2}) sec");
            float attack_power = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setAttackPower(attack_power);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 120f));
        }
        // Increases armor by 183 for 20 sec.
        else if (isMatch(line, "Increases armor by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases armor by (\\d{3}) for (\\d{2}) sec");
            float armor = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setBonusArmor(armor);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 120f));
        }
        // Increases armor by 183 for 20 sec.
        else if (isMatch(line, "Increases your armor by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases your armor by (\\d{3}) for (\\d{2}) sec");
            float armor = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setBonusArmor(armor);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 120f));
        }
        // Increases armor by 183 for 20 sec.
        else if (isMatch(line, "Increases armor by (\\d{4}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases armor by (\\d{4}) for (\\d{2}) sec");
            float armor = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setBonusArmor(armor);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 120f));
        }
        // Increases armor by 183 for 20 sec.
        else if (isMatch(line, "Increases your armor by (\\d{4}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases your armor by (\\d{4}) for (\\d{2}) sec");
            float armor = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setBonusArmor(armor);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 120f));
        }
        // Increases maximum health, shares cooldown with other Battlemaster's trinkets
        else if (line.contains("Battlemaster's trinkets.")) {
            Pattern r = pattern("Increases maximum health by (?<health>\\d*) for (?<duration>\\d*) sec");
            Matcher m = r.matcher(line);
            if (m.find()) {
                float health = Float.parseFloat(group(m, "health"));
                float duration = Float.parseFloat(group(m, "duration"));
                float cooldown = 3.0f * 60.0f; // Default to 3 minute cooldown, if not otherwise known

                if (!isArmory) {
                    Pattern rCD = pattern("\\((?<cooldown>\\d*) Min Cooldown\\)");
                    Matcher mCD = rCD.matcher(line);
                    if (mCD.find()) {
                        cooldown = parseIntNet(group(mCD, "cooldown")) * 60.0f;
                    }
                }

                Stats effectStats = new Stats();
                effectStats.setBattlemasterHealth(health);
                stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, duration, cooldown));
            }
        }
        // Increases maximum health by 183 for 20 sec.
        else if (isMatch(line, "Increases maximum health by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases maximum health by (\\d{3}) for (\\d{2}) sec");
            float health = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 3min.
            Stats effectStats = new Stats();
            effectStats.setHealth(health);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 180f));
        }
        // Increases maximum health by 183 for 20 sec.
        else if (isMatch(line, "Increases your maximum health by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases your maximum health by (\\d{3}) for (\\d{2}) sec");
            float health = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 3min.
            Stats effectStats = new Stats();
            effectStats.setHealth(health);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 180f));
        }
        // Increases maximum health by 183 for 20 sec.
        else if (isMatch(line, "Increases maximum health by (\\d{4}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases maximum health by (\\d{4}) for (\\d{2}) sec");
            float health = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 3min.
            Stats effectStats = new Stats();
            effectStats.setHealth(health);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 180f));
        }
        // Increases maximum health by 183 for 20 sec.
        else if (isMatch(line, "Increases your maximum health by (\\d{4}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases your maximum health by (\\d{4}) for (\\d{2}) sec");
            float health = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 3min.
            Stats effectStats = new Stats();
            effectStats.setHealth(health);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 180f));
        }
        // Increases dodge rating by 183 for 20 sec.
        else if (isMatch(line, "Increases dodge by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases dodge by (\\d{3}) for (\\d{2}) sec");
            float dodge_rating = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            float cooldown_sec = 120f;
            if (id == 44063) {
                cooldown_sec = 60.0f; //Figurine - Monarch Crab
            }
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setDodgeRating(dodge_rating);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, cooldown_sec));
        }
        // Increases dodge rating by 183 for 20 sec.
        else if (isMatch(line, "Increases dodge rating by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases dodge rating by (\\d{3}) for (\\d{2}) sec");
            float dodge_rating = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            float cooldown_sec = 120f;
            if (id == 44063) {
                cooldown_sec = 60.0f; //Figurine - Monarch Crab
            }
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setDodgeRating(dodge_rating);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, cooldown_sec));
        }
        // Increases dodge rating by 183 for 20 sec.
        else if (isMatch(line, "Increases your dodge rating by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases your dodge rating by (\\d{3}) for (\\d{2}) sec");
            float dodge_rating = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            float cooldown_sec = 120f;
            if (id == 44063) {
                cooldown_sec = 60.0f; //Figurine - Monarch Crab
            }
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setDodgeRating(dodge_rating);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, cooldown_sec));
        }
        // Increases parry rating by 183 for 20 sec.
        else if (isMatch(line, "Increases parry rating by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases parry rating by (\\d{3}) for (\\d{2}) sec");
            float parry_rating = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setParryRating(parry_rating);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 120f));
        }
        // Increases parry rating by 183 for 20 sec.
        else if (isMatch(line, "Increases your parry rating by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases your parry rating by (\\d{3}) for (\\d{2}) sec");
            float parry_rating = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setParryRating(parry_rating);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 120f));
        }
        // Increases haste rating by 183 for 20 sec.
        else if (isMatch(line, "Increases haste rating by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases haste rating by (\\d{3}) for (\\d{2}) sec");
            float haste_rating = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setHasteRating(haste_rating);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 120f));
        }
        // Increases haste rating by 183 for 20 sec.
        else if (isMatch(line, "Increases your haste rating by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases your haste rating by (\\d{3}) for (\\d{2}) sec");
            float haste_rating = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setHasteRating(haste_rating);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 120f));
        }
        // Increases haste rating by 183 for 20 sec.
        else if (isMatch(line, "Increases the block value of your shield by (\\d{3}) for (\\d{2}) sec")) {
            String[] inputs = netSplit(line, "Increases the block value of your shield by (\\d{3}) for (\\d{2}) sec");
            float block = Float.parseFloat(inputs[1]);
            float uptime = Float.parseFloat(inputs[2]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setBlockValue(block);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, uptime, 120f));
        }
        // Increases haste rating by 183 for 20 sec.
        else if (isMatch(line, "Instantly heal your current friendly target for (\\d{4}).")) {
            String[] inputs = netSplit(line, "Instantly heal your current friendly target for (\\d{4}).");
            float heal = Float.parseFloat(inputs[1]);
            // unfortunately for us the cooldown isn't listed, so guess 2min.
            Stats effectStats = new Stats();
            effectStats.setHealed(heal);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 0, 60f));
        } else if (line.startsWith("Increases your Spirit by ")) {
            // Bangle of Endless Blessings, Earring of Soulful Meditation
            Pattern r = pattern("Increases your Spirit by \\+?(?<spi>\\d*) for (?<dur>\\d*) sec"); // \\(2 Min Cooldown\\)");
            Matcher m = r.matcher(line);
            if (m.find()) {
                int spi = parseIntNet(group(m, "spi"));
                int dur = parseIntNet(group(m, "dur"));
                Stats effectStats = new Stats();
                effectStats.setSpirit(spi);
                stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, dur, 120f));
            }
        } else if (line.startsWith("Increases speed by ")) {
            // Figurine - Ruby Hare : "Increases speed by 30% for 6 sec. (3 Min Cooldown)"
            Pattern r = pattern("Increases speed by (?<speed>\\d*)% for (?<dur>\\d*) sec"); // \\((?<cd>\\d*) Min Cooldown\\)");
            Matcher m = r.matcher(line);
            if (m.find()) {
                int speed = parseIntNet(group(m, "speed"));
                int dur = parseIntNet(group(m, "dur"));
                // Test again with the cd... Available only on wowhead.
                float cd;
                Pattern rCD = pattern("Increases speed by (?<speed>\\d*)% for (?<dur>\\d*) sec \\((?<cd>\\d*) Min Cooldown\\)");
                Matcher mCD = rCD.matcher(line);
                if (mCD.find()) {
                    cd = parseIntNet(group(mCD, "cd")) * 60.0f;
                } else {
                    cd = 3.0f * 60.0f;
                }
                Stats effectStats = new Stats();
                effectStats.setMovementSpeed(speed / 100f);
                stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, dur, cd));
            }
        } else if (isMatch(line, "When the shield is removed by any means, you regain (\\d{4}) mana.")) {
            String[] inputs = netSplit(line, "When the shield is removed by any means, you regain (\\d{4}) mana.");
            float restore = Float.parseFloat(inputs[1]);
            Stats effectStats = new Stats();
            effectStats.setManaRestore(restore);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 0f, 5 * 60f));
        } else if (line.startsWith("For the next 20 sec, your direct heals increase healing received by their target by up to 58.")) {
            // Talisman of Troll Divinity
            // For 20 seconds, direct healing adds a stack of 58 +healing for 10 seconds
            // Stacks 5 times, 2 minute cd
            // Direct heals: Nourish (1.5) HT (3) Regrowth (2)
            // Assumption: every 2 seconds, a direct healing spell is cast, so after 5 casts, full effect
            // That would mean: 10 seconds ramping up, then 20 seconds having the effect (assuming the stack is refreshed)
            // Average stack of 4 (24/30 * 5)
            // But remember that the spellpower will increase for others in the raid too!
            // stats.BonusHealingReceived = 58 * 4;
            Stats effectStats = new Stats();
            effectStats.setBonusHealingReceived(58 * 4f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 30, 120));
        }
        /*else if (line.StartsWith("Your heals each cost "))
        {
            // Lower City Prayerbook
            Regex r = new Regex("Your heals each cost (?<mana>\\d*) less mana for the next 15 sec");
            Match m = r.Match(line);
            if (m.Success)
            {
                stats.ManacostReduceWithin15OnUse1Min += (float)int.Parse(m.Groups["mana"].Value);
            }
        }*/
        else if (line.startsWith("Gain 250 mana each sec for ")) {
            //stats.ManaregenFor8SecOnUse5Min += 250;
            Stats effectStats = new Stats();
            effectStats.setMp5(250 * 5);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 8.0f, 300.0f));
        } else if (line.startsWith("Conjures a Power Circle lasting for 15 sec.  While standing in this circle, the caster gains 320 spell power.")) {
            // Shifting Naaru Sliver
            Stats effectStats = new Stats();
            effectStats.setSpellPower(320);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 15f, 90.0f));
        } else if ((match = match(line, "Each spell cast within 20 seconds will grant a stacking bonus of (?<mp5>\\d+) mana regen per 5 sec.? Expires after (?<duration>\\d+) seconds.*")).find()) {
            int mp5 = parseIntNet(group(match, "mp5"));
            // Meteorite Crystal and Pendant of the Violet Eye
            // Estimate cast as every 2.0f seconds, average stack height is 0.5f of final value
            Stats effectStats = new Stats();
            effectStats.setMp5(mp5 * parseIntNet(group(match, "duration")) * 0.5f / 2.0f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, parseIntNet(group(match, "duration")), 120));
        } else if ((match = match(line, "Each time you cast a helpful spell, you gain (?<amount>\\d+) spell power. *Stacks up to (?<stacks>\\d+) times. *Entire effect lasts (?<duration>\\d+) sec.*")).find()) {
            // Binding Light / Stone
            SpecialEffect primary = new SpecialEffect(Trigger.Use, new Stats(),
                    (float) parseIntNet(group(match, "duration")), 2f * 60f);
            Stats secondaryStats = new Stats();
            secondaryStats.setSpellPower((float) parseIntNet(group(match, "amount")));
            SpecialEffect secondary = new SpecialEffect(Trigger.HealingSpellCast,
                    secondaryStats,
                    (float) parseIntNet(group(match, "duration")),
                    0f, 1f, parseIntNet(group(match, "stacks")));
            primary.getStats().addSpecialEffect(secondary);
            stats.addSpecialEffect(primary);
        } else if ((match = match(line,
                "Each time you cast a harmful spell, you gain (?<hasteRating>\\d+) haste rating.? .*Stacks up to (?<stacks>\\d+) times.? .*Entire effect lasts (?<duration>\\d+) sec.?( \\((?<cooldown>\\d+) Min Cooldown\\))?"))
                .find()) {
            // Fetish of Volatile Power
            // armory doesn't give cooldown info
            int cooldown = 120;
            if (!group(match, "cooldown").isEmpty()) {
                cooldown = parseIntNet(group(match, "cooldown")) * 60;
            }
            Stats childEffect = new Stats();
            Stats childStats = new Stats();
            childStats.setHasteRating(parseIntNet(group(match, "hasteRating")));
            childEffect.addSpecialEffect(new SpecialEffect(Trigger.SpellCast, childStats, Float.POSITIVE_INFINITY, 0f, 1f, parseIntNet(group(match, "stacks"))));
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, childEffect, parseIntNet(group(match, "duration")), cooldown, 1f));
        }
        // Wowhead: "Each time you are struck by an attack, you gain 1265 armor. Stacks up to 5 times. Entire effect lasts 20 sec (2 Min Cooldown)"
        // Armory:  "Each time you are struck by an attack, you gain 1265 armor. Stacks up to 5 times. Entire effect lasts 20 sec."
        else if ((match = match(line, "Each time you are struck by an attack, you gain (?<bonusArmor>\\d+) armor\\.\\s+Stacks up to (?<stacks>\\d+) times\\.\\s+Entire effect lasts (?<duration>\\d+) sec(\\.\\s+\\((?<cooldown>\\d+) Min Cooldown\\))?")).find()) {
            // Fervor of the Frostborn / Eitrigg's Oath
            // armory doesn't give cooldown info
            int cooldown = 120;
            if (!group(match, "cooldown").isEmpty()) {
                cooldown = parseIntNet(group(match, "cooldown")) * 60;
            }
            Stats childEffect = new Stats();
            Stats childStats = new Stats();
            childStats.setBonusArmor(parseIntNet(group(match, "bonusArmor")));
            childEffect.addSpecialEffect(new SpecialEffect(Trigger.DamageTaken, childStats, Float.POSITIVE_INFINITY, 0f, 1f, parseIntNet(group(match, "stacks"))));
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, childEffect, parseIntNet(group(match, "duration")), cooldown, 1f));
        }
        // Figurine - Talasite Owl, 5 min cooldown
        else if (line.startsWith("Restores 900 mana over 12 sec.")) {
            //if (stats.Mp5 == 18) // Figurine - Seaspray Albatross, 3 min cooldown
            //    stats.ManaregenOver12SecOnUse3Min += 900;
            //else if (stats.Mp5 == 14) // Figurine - Talasite Owl, 5 min cooldown
            //    stats.ManaregenOver12SecOnUse5Min += 900;
        } else if ((match = match(line, "Increases the block value of your shield by (?<amount>\\d\\d*) for 20 sec.")).find()) {
            Stats effectStats = new Stats();
            effectStats.setBlockValue((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 20.0f, 120.0f));
        }
        /*else if (line.StartsWith("Your heals each cost "))
        {
            // Lower City Prayerbook
            Regex r = new Regex("Your heals each cost (?<mana>\\d*) less mana for the next 15 sec");
            Match m = r.Match(line);
            if (m.Success)
            {
                stats.ManacostReduceWithin15OnUse1Min += (float)int.Parse(m.Groups["mana"].Value);
            }
        }*/
        else if (line.startsWith("Tap into the power of the skull, increasing haste rating by 175 for 20 sec")) {
            // The Skull of Gul'dan
            Stats effectStats = new Stats();
            effectStats.setHasteRating(175);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 20.0f, 120.0f));
        } else if (line.startsWith("Each spell cast within 20 seconds will grant a stacking bonus of 21 mana regen per 5 sec. Expires after 20 seconds.  Abilities with no mana cost will not trigger this trinket.")
                || line.startsWith("Each spell cast within 20 seconds will grant a stacking bonus of 21 mana regen per 5 sec Expires after 20 seconds.  Abilities with no mana cost will not trigger this trinket.")) {
            //stats.Mp5OnCastFor20SecOnUse2Min += 21;
        }
        // Mind Quickening Gem
        else if (line.startsWith("Quickens the mind, increasing the Mage's haste rating by 330 for 20 sec")) {
            Stats effectStats = new Stats();
            effectStats.setHasteRating(330);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 20, 300));
        }
        //			else if (line.StartsWith("Increases the block value of your shield by 200 for 20 sec"))
        //			{
        //				stats.BlockValue += (float)Math.Round(200f * (20f / 120f));
        //			}
        else if (line.startsWith("Removes all movement impairing effects and all effects which cause loss of control of your character.")) {
            stats.setPVPTrinket(stats.getPVPTrinket() + 1f);
        } else if (line.startsWith("Increases the damage dealt by your Scourge Strike and Obliterate abilities by 420.")) {
            stats.setBonusObliterateDamage(stats.getBonusObliterateDamage() + 420);
            stats.setBonusScourgeStrikeDamage(stats.getBonusScourgeStrikeDamage() + 420);
        } else if (line.startsWith("Restores 2340 mana over 12 sec")) {
            // Figurine - Sapphire Owl
            //stats.ManaRestore5min = 2340;
            Stats effectStats = new Stats();
            effectStats.setManaRestore(2340);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 0f, 300f));
        } else if (line.equals("Instantly heal your current friendly target for 2710. (1 Min Cooldown)")) {
            // Living Ice Crystals
            Stats effectStats = new Stats();
            effectStats.setHealed(2710);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 0f, 60f));
        } else if ((match = match(line, "Grants (?<amount>\\d\\d*) haste rating for 20 sec")).find()) {
            // Ephemeral Snowflake
            Stats effectStats = new Stats();
            effectStats.setHasteRating((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 20f, 120f));
        } else if ((match = match(line, "Restores (?<amount>\\d+) mana.")).find()) {
            // Sliver of Pure Ice
            Stats effectStats = new Stats();
            effectStats.setManaRestore((float) parseIntNet(group(match, "amount")));
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 0f, 120f));
        } else if ((match = match(line, "Every time one of your non-periodic spells deals a critical strike, the bonus is reduced by 184 critical strike rating.")).find()) {
            // Nevermelting Ice Crystal - Yes when armory is fixed this needs update
            Stats childEffect = new Stats();
            childEffect.setCritRating(920f);
            Stats childStats = new Stats();
            childStats.setCritRating(-184f);
            childEffect.addSpecialEffect(new SpecialEffect(Trigger.SpellCrit, childStats, Float.POSITIVE_INFINITY, 0f, 1f, 5));
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, childEffect, 20f, 180f, 1f));
        } else if ((match = match(line, "Generates a bolt of lightning to strike an enemy for (?<min>\\d\\d*) to (?<max>\\d\\d*) Nature damage*")).find()) {
            // Gnomish Lightning Generator
            int min = parseIntNet(group(match, "min"));
            int max = parseIntNet(group(match, "max"));
            Stats effectStats = new Stats();
            effectStats.setNatureDamage((float) (min + max) / 2f);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 0f, 60f, 1f));
        }
        // TODO: Update this so it can have a wider use.
        else if ((match = match(line, "Absorbs 6400 damage")).find()) {
            // Corroded Skeleton Key
            // Absorbs 6400 damage.  Lasts 10 sec. (2 Min Cooldown)
            float DamageAbsorb = 6400;
            float fDuration = 10;
            float fCD = 120; // 2 min * 60 sec
            Stats effectStats = new Stats();
            effectStats.setDamageAbsorbed(DamageAbsorb);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, fDuration, fCD));
        } else if ((match = match(line, "Increases resistance to Arcane, Fire, Frost, Nature, and Shadow spells by (?<amount>\\d\\d*) for (?<duration>\\d\\d*) sec.")).find()) {
            // Sindragosa's Flawless Fang
            // Increases resistance to Arcane, Fire, Frost, Nature, and Shadow spells by 239 for 10 sec. (1 Min Cooldown)
            int amount = parseIntNet(group(match, "amount"));
            float fDuration = Float.parseFloat(group(match, "duration"));
            float fCD = 60; // 1 min * 60 sec
            Stats effectStats = new Stats();
            effectStats.setArcaneResistance(amount);
            effectStats.setFireResistance(amount);
            effectStats.setFrostResistance(amount);
            effectStats.setNatureResistance(amount);
            effectStats.setShadowResistance(amount);
            stats.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, fDuration, fCD));
        } else if ((match = match(line, "For the next 15 sec, each time your direct healing spells heal a target you cause the target of your heal to heal themselves and friends within 10 yards for (?<amount>\\d+) each sec for (?<seconds>\\d+) sec")).find()) {
            // Glowing Twilight Scale proc (Eyes of Twilight); Procs on Healing Hit, NOT HoTs; Hits average of 5 people;
            int amount = parseIntNet(group(match, "amount"));
            int seconds = parseIntNet(group(match, "seconds"));
            SpecialEffect primary = new SpecialEffect(Trigger.Use, new Stats(), 15f, 2f * 60f);
            Stats secondaryStats = new Stats();
            secondaryStats.setHealed(amount * 5);
            SpecialEffect secondary = new SpecialEffect(Trigger.HealingSpellCast, secondaryStats, seconds, 0f);
            primary.getStats().addSpecialEffect(secondary);
            stats.addSpecialEffect(primary);
        }
    }

    /**
     * Builds the proc candidates of the Deathbringer's Will trinket for one class. All candidates
     * share the same trigger, duration, cooldown and chance but grant different stats; the returned
     * list holds the three the class can receive (attack power is doubled relative to
     * {@code value}). Classes outside the switch yield an empty list. This is called from inside a
     * model, not from the tooltip parser.
     */
    /// <summary>
    /// Special Handler for Deathbringer's Will to be call from INSIDE the model.
    /// </summary>
    public static List<SpecialEffect> getDeathBringerEffects(CharacterClass clazz, float value) {
        List<SpecialEffect> retVal = new ArrayList<SpecialEffect>();
        float dur = 30f, cd = 105 * 3, ch = 0.15f;

        Stats strStats = new Stats();
        strStats.setStrength(value);
        SpecialEffect procSTR = new SpecialEffect(Trigger.PhysicalHit, strStats, dur, cd, ch);
        Stats critStats = new Stats();
        critStats.setCritRating(value);
        SpecialEffect procCrit = new SpecialEffect(Trigger.PhysicalHit, critStats, dur, cd, ch);
        Stats arpStats = new Stats();
        arpStats.setArmorPenetrationRating(value);
        SpecialEffect procArP = new SpecialEffect(Trigger.PhysicalHit, arpStats, dur, cd, ch);
        Stats hasteStats = new Stats();
        hasteStats.setHasteRating(value);
        SpecialEffect procHaste = new SpecialEffect(Trigger.PhysicalHit, hasteStats, dur, cd, ch);
        Stats agiStats = new Stats();
        agiStats.setAgility(value);
        SpecialEffect procAGI = new SpecialEffect(Trigger.PhysicalHit, agiStats, dur, cd, ch);
        Stats apStats = new Stats();
        apStats.setAttackPower(value * 2);
        SpecialEffect procAP = new SpecialEffect(Trigger.PhysicalHit, apStats, dur, cd, ch);

        switch (clazz) {
            case Warrior:
                retVal.add(procSTR);
                retVal.add(procArP);
                retVal.add(procCrit);
                break;
            case Rogue:
                retVal.add(procAGI);
                retVal.add(procArP);
                retVal.add(procAP);
                break;
            case Paladin:
                retVal.add(procSTR);
                retVal.add(procHaste);
                retVal.add(procCrit);
                break;
            case Hunter:
                retVal.add(procAP);
                retVal.add(procAGI);
                retVal.add(procCrit);
                break;
            case DeathKnight:
                retVal.add(procSTR);
                retVal.add(procCrit);
                retVal.add(procHaste);
                break;
            case Druid:
                retVal.add(procArP);
                retVal.add(procSTR);
                retVal.add(procAGI);
                break;
            case Shaman:
                retVal.add(procAGI);
                retVal.add(procAP);
                retVal.add(procArP);
                break;
            default:
                break; // None
        }

        return retVal;
    }

    /** Assigns one parsed tooltip stat name to {@code s}; unknown names leave it unchanged. */
    private static void applyStatName(Stats s, String statName, float amount) {
        if (statName.equalsIgnoreCase("attack power")) {
            s.setAttackPower(amount);
        } else if (statName.equalsIgnoreCase("agility")) {
            s.setAgility(amount);
        } else if (statName.equalsIgnoreCase("armor")) {
            s.setBonusArmor(amount);
        } else if (statName.equalsIgnoreCase("armor penetration rating")) {
            s.setArmorPenetrationRating(amount);
        } else if (statName.equalsIgnoreCase("block value")) {
            s.setBlockValue(amount);
        } else if (statName.equalsIgnoreCase("critical strike rating")) {
            s.setCritRating(amount);
        } else if (statName.equalsIgnoreCase("defense rating")) {
            s.setDefenseRating(amount);
        } else if (statName.equalsIgnoreCase("dodge")) {
            s.setDodgeRating(amount);
        } else if (statName.equalsIgnoreCase("dodge rating")) {
            s.setDodgeRating(amount);
        } else if (statName.equalsIgnoreCase("melee and ranged attack power")) {
            s.setAttackPower(amount);
        } else if (statName.equalsIgnoreCase("haste rating")) {
            s.setHasteRating(amount);
        } else if (statName.equalsIgnoreCase("maximum health")) {
            s.setHealth(amount);
        } else if (statName.equalsIgnoreCase("parry")) {
            s.setParryRating(amount);
        } else if (statName.equalsIgnoreCase("parry rating")) {
            s.setParryRating(amount);
        } else if (statName.equalsIgnoreCase("spell power")) {
            s.setSpellPower(amount);
        } else if (statName.equalsIgnoreCase("spirit")) {
            s.setSpirit(amount);
        }
    }

    /** Maps a parsed ability name to its trigger; unknown abilities use {@link Trigger#SpellHit}. */
    private static Trigger triggerFor(String ability) {
        Trigger trigger;
        switch (ability) {
            case "Icy Touch":
                trigger = Trigger.IcyTouchHit;
                break;
            case "Plague Strike":
                trigger = Trigger.PlagueStrikeHit;
                break;
            case "Rune Strike":
                trigger = Trigger.RuneStrikeHit;
                break;
            case "Blood Strike":
                trigger = Trigger.BloodStrikeHit;
                break;
            case "Heart Strike":
            case "Heart Strikes":
                trigger = Trigger.HeartStrikeHit;
                break;
            case "Obliterate":
                trigger = Trigger.ObliterateHit;
                break;
            case "Scourge Strike":
                trigger = Trigger.ScourgeStrikeHit;
                break;
            case "Death Strike":
                trigger = Trigger.DeathStrikeHit;
                break;
            default:
                trigger = Trigger.SpellHit;
                break;
        }

        return trigger;
    }

    /**
     * Creates a 100%-chance effect from a fallback-pattern match; delegates to the six-argument
     * {@link #evalRegex(String, float, float, String, float, float)}.
     */
    public static SpecialEffect evalRegex(String statName, float amount, float duration, String ability, float cooldown) {
        return evalRegex(statName, amount, duration, ability, cooldown, 1f);
    }

    /// <summary>
    /// For those objects that have a special effect trigger (As opposed to just straight stat upgrades)
    /// This allows you to pass in a statName, amount, duration, trigger type, and cooldown and get a SpecialEffect back.
    /// </summary>
    /**
     * Builds one effect from parsed tooltip text: maps {@code statName} to a stat field (unknown
     * names leave the stats empty), maps {@code ability} to a trigger (unknown abilities fall back to
     * {@link Trigger#SpellHit}) and returns a {@link SpecialEffect} with the given duration,
     * cooldown and chance.
     */
    public static SpecialEffect evalRegex(String statName, float amount, float duration, String ability, float cooldown, float chance) {
        Stats s = new Stats();

        applyStatName(s, statName, amount);
        if (statName.equalsIgnoreCase("strength")) {
            s.setStrength(amount);
        }
        Trigger trigger = triggerFor(ability);

        return new SpecialEffect(trigger, s, duration, cooldown, chance);
    }

    /// <summary>
    /// For those objects that have a special effect trigger (As opposed to just straight stat upgrades)
    /// This allows you to pass in a statName, amount, duration, trigger type, and cooldown and get a SpecialEffect back.
    /// </summary>
    /**
     * Stack-aware variant of the six-argument {@code evalRegex}: the resulting effect may stack up
     * to {@code stacks} times, matching tooltips that end in "Stacks up to N times".
     */
    public static SpecialEffect evalRegex(String statName, float amount, float duration, String ability, float cooldown, float chance, int stacks) {
        Stats s = new Stats();

        applyStatName(s, statName, amount);
        if (statName.equalsIgnoreCase("strength")) {
            s.setStrength(amount);
        }
        Trigger trigger = triggerFor(ability);

        return new SpecialEffect(trigger, s, duration, cooldown, chance, stacks);
    }

    /// <summary>
    /// Using the Regex functions above, update the passed in stat value of a given statName by the amount appropriate.
    /// </summary>
    /**
     * Assigns {@code amount} to the matching field of {@code s} (assignment, not accumulation);
     * unknown stat names change nothing. Used by fallback patterns that describe a flat bonus rather
     * than a proc.
     */
    public static void evalRegex(Stats s, String statName, float amount) {
        applyStatName(s, statName, amount);
    }

    /// <summary>
    /// Eval the Bonus damage values of given sigils and assign the bonus damage.
    /// This is specifically for DKs, but could be expanded as necessary.
    /// </summary>
    /**
     * Adds {@code amount} to the ability-specific bonus damage field of {@code s} (Death Knight
     * sigil patterns); abilities outside the switch are ignored. Note this accumulates, unlike
     * {@link #evalRegex(Stats, String, float)} which assigns.
     */
    public static void evalAbility(String ability, Stats s, float amount) {
        switch (ability) {
            case "Blood Strike":
                s.setBonusBloodStrikeDamage(s.getBonusBloodStrikeDamage() + amount);
                break;
            case "Heart Strike":
                s.setBonusHeartStrikeDamage(s.getBonusHeartStrikeDamage() + amount);
                break;
            case "Death Coil":
                s.setBonusDeathCoilDamage(s.getBonusDeathCoilDamage() + amount);
                break;
            case "Frost Strike":
                s.setBonusFrostStrikeDamage(s.getBonusFrostStrikeDamage() + amount);
                break;
            case "Obliterate":
                s.setBonusObliterateDamage(s.getBonusObliterateDamage() + amount);
                break;
            case "Scourge Strike":
                s.setBonusScourgeStrikeDamage(s.getBonusScourgeStrikeDamage() + amount);
                break;
            case "Death Strike":
                s.setBonusDeathStrikeDamage(s.getBonusDeathStrikeDamage() + amount);
                break;
            case "Icy Touch":
                s.setBonusIcyTouchDamage(s.getBonusIcyTouchDamage() + amount);
                break;
            default:
                break; // Error.
        }
    }

    // ------------------------------------------------------------------ translation helpers

    /** C# {@code String.Split(string[], StringSplitOptions.None)}: no empty-entry removal. */
    private static String[] splitMulti(String input, String... separators) {
        List<String> result = new ArrayList<String>();
        int pos = 0;
        while (true) {
            int bestIndex = -1;
            String bestSeparator = null;
            for (String separator : separators) {
                int index = input.indexOf(separator, pos);
                if (index >= 0 && (bestIndex == -1 || index < bestIndex)) {
                    bestIndex = index;
                    bestSeparator = separator;
                }
            }
            if (bestIndex == -1) {
                break;
            }
            result.add(input.substring(pos, bestIndex));
            pos = bestIndex + bestSeparator.length();
        }
        result.add(input.substring(pos));
        return result.toArray(new String[0]);
    }

    /** C# {@code Regex.Split(text, pattern)}: captured groups are inserted in the result. */
    private static String[] netSplit(String input, String pattern) {
        Matcher m = pattern(pattern).matcher(input);
        List<String> result = new ArrayList<String>();
        int last = 0;
        while (m.find()) {
            result.add(input.substring(last, m.start()));
            for (int g = 1; g <= m.groupCount(); g++) {
                String value = m.group(g);
                result.add(value == null ? "" : value);
            }
            last = m.end();
        }
        result.add(input.substring(last));
        return result.toArray(new String[0]);
    }

    /**
     * C# {@code match.Groups["name"].Value}: returns "" for an unknown group name or a group that
     * did not participate, where Java's {@link Matcher#group(String)} throws or returns null.
     */
    private static String group(Matcher matcher, String name) {
        try {
            String value = matcher.group(name);
            return value == null ? "" : value;
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    /** C# {@code Regex.IsMatch(input, pattern)}: a match anywhere in the input. */
    private static boolean isMatch(String input, String pattern) {
        return pattern(pattern).matcher(input).find();
    }

    private static Matcher match(String input, String pattern) {
        return pattern(pattern).matcher(input);
    }

    /** C# {@code int.Parse}: trims surrounding whitespace first (Java's parseInt rejects it). */
    private static int parseIntNet(String value) {
        return Integer.parseInt(value.trim());
    }

    /** C# {@code String.Trim(char)}: removes that character from both ends. */
    private static String trimChar(String value, char ch) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == ch) {
            start++;
        }
        while (end > start && value.charAt(end - 1) == ch) {
            end--;
        }
        return value.substring(start, end);
    }

    /** C# {@code String.TrimStart(' ')}. */
    private static String trimStartChar(String value, char ch) {
        int start = 0;
        while (start < value.length() && value.charAt(start) == ch) {
            start++;
        }
        return value.substring(start);
    }

    /**
     * C# compares {@code SpecialEffect.ToString()} for the prototype-sigil de-duplication. At every
     * call site the two effects are built from the same amount/duration/cooldown/chance/stacks, so
     * their {@code Stats.ToString()}, duration, chance, cooldown and stack strings are equal and only
     * the `TriggerString` can differ; that string is injective over the Trigger enum, so comparing
     * the triggers is exactly equivalent (see DECISIONS #54).
     */
    private static boolean effectTextDiffers(SpecialEffect a, SpecialEffect b) {
        return a.getTrigger() != b.getTrigger();
    }
}
