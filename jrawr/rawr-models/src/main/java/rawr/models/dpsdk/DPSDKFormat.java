// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/SpecialEffect.cs (ToString helpers) and
// legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Stats.cs (ToString + Extensions.DisplayName) @
// 46f190bee32964cc8df5fe853d95b8a343f5aa23. The DPSDK model is the first consumer of
// SpecialEffect.ToString()/Stats.ToString() (DRWStats), so the formatting lives with the model.
package rawr.models.dpsdk;

import rawr.base.util.NetFormat;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;

public final class DPSDKFormat {
    private DPSDKFormat() {
    }

    /** .NET `P`-style percent on a Single (Mono invariant writes a space before `%`). */
    public static String percent(float value, int decimals) {
        String s = NetFormat.percent(value, decimals);
        if (s.endsWith("%")) {
            return s.substring(0, s.length() - 1) + " %";
        }
        return s;
    }

    /** C# Stats.ToString(): all nonzero float properties in declaration order, then special effects. */
    public static String statsToString(Stats stats) {
        StringBuilder sb = new StringBuilder();
        for (Stats.StatProperty property : Stats.PROPERTY_INFO) {
            float value = property.accessor().get(stats);
            if (value != 0) {
                if ( property.percentage() ) {
                    value *= 100;
                }
                value = (float) Math.rint((double) (value * 100f)) / 100f;
                sb.append(rawr.models.mage.NetFormat.netFloatToString(value)).append(displayName(property)).append(", ");
            }
        }
        for (SpecialEffect effect : stats.specialEffects()) {
            sb.append(specialEffectToString(effect)).append(", ");
        }
        return trimEnd(sb.toString());
    }

    /** Ported from Rawr.Base/Stats.cs Extensions.DisplayName + SpaceCamel. */
    private static String displayName(Stats.StatProperty property) {
        String prettyName = property.displayName();
        if (prettyName == null) {
            prettyName = spaceCamel(property.name());
        }
        if (!prettyName.startsWith("%")) {
            prettyName = " " + prettyName;
        }
        return prettyName;
    }

    private static String spaceCamel(String name) {
        return name.replaceAll("([A-Z])", " $1").trim();
    }

    /** C# SpecialEffect.ToString(). */
    public static String specialEffectToString(SpecialEffect effect) {
        StringBuilder s = new StringBuilder();
        if (effect.getMaxStack() > 1) {
            s.append(effect.getMaxStack()).append("x").append(" ");
        }

        s.append(statsToString(effect.getStats()));
        s.append(" (");

        boolean needsSpace = false;
        if (effect.getDuration() > 0 && !Float.isInfinite(effect.getDuration())) {
            s.append(durationString(effect));
            needsSpace = true;
        }
        if (effect.getChance() < 1) {
            if (needsSpace) s.append(" ");
            s.append(chanceString(effect));
            needsSpace = true;
        }

        if (effect.getTrigger() != Trigger.Use) {
            if (needsSpace) s.append(" ");
            s.append(triggerString(effect.getTrigger()));
            needsSpace = true;
        }
        if (effect.getCooldown() > 0) {
            if (needsSpace) s.append("/");
            s.append(cooldownString(effect));
        }
        s.append(")");
        return s.toString();
    }

    private static String durationString(SpecialEffect effect) {
        return ((int) effect.getDuration()) + " Sec";
    }

    private static String cooldownString(SpecialEffect effect) {
        if (Float.isInfinite(effect.getCooldown()) && effect.getCooldown() > 0) {
            return "Once per Fight";
        }
        int cooldown = (int) effect.getCooldown();
        if (cooldown % 60 == 0) {
            return (cooldown / 60) + " Min";
        }
        return cooldown + " Sec";
    }

    private static String chanceString(SpecialEffect effect) {
        if (effect.getChance() < 0) {
            return NetFormat.grouped(-effect.getChance(), 2) + " PPM";
        }
        return NetFormat.grouped(effect.getChance() * 100, 0) + "%";
    }

    private static String triggerString(Trigger trigger) {
        switch (trigger) {
            case DamageSpellCast:
                return "on Damaging Spell Cast";
            case DamageDone:
                return "on Damage Dealt";
            case DamageSpellCrit:
                return "on Damaging Spell Crit";
            case DamageSpellHit:
                return "on Damaging Spell Hit";
            case HealingSpellCast:
                return "on Healing Cast";
            case HealingSpellCrit:
                return "on Healing Crit";
            case HealingSpellHit:
                return "on Healing Hit";
            case MeleeCrit:
                return "on Melee Crit";
            case MeleeHit:
                return "on Melee Hit";
            case CurrentHandHit:
                return "on Current Hand Melee Hit";
            case MainHandHit:
                return "on Main Hand Melee Hit";
            case OffHandHit:
                return "on Off Hand Melee Hit";
            case MeleeAttack:
                return "on Melee Attack";
            case SpellCast:
                return "on Spell Cast";
            case SpellCrit:
                return "on Spell Crit";
            case SpellHit:
                return "on Spell Hit";
            case SpellMiss:
                return "on Spell Miss";
            case Use:
                return "";
            case PhysicalHit:
                return "on Physical Hit";
            case PhysicalCrit:
                return "on Physical Crit";
            case ManaGem:
                return "on Mana Gem";
            case MageNukeCast:
                return "on Mage Nuke Cast";
            case JudgementHit:
                return "on Judgement";
            case HolyShield:
                return "on Holy Shield cast";
            case HammeroftheRighteous:
                return "on HotR cast";
            case ShieldofRighteousness:
                return "on ShoR cast";
            case CrusaderStrikeHit:
                return "on Crusader Strike";
            case InsectSwarmTick:
                return "on Insect Swarm Tick";
            case ShamanLightningBolt:
                return "on Lightning Bolt Cast";
            case ShamanLavaLash:
                return "on Lava Lash Hit";
            case ShamanShock:
                return "on Shock Hit";
            case ShamanStormStrike:
                return "on Stormstrike Hit";
            case BloodStrikeHit:
                return "on Blood Strike";
            case HeartStrikeHit:
                return "on Heart Strike";
            case BloodStrikeOrHeartStrikeHit:
                return "on Blood Strike or Heart Strike";
            case IcyTouchHit:
                return "on Icy Touch";
            case PlagueStrikeHit:
                return "on Plague Strike";
            case RuneStrikeHit:
                return "on Rune Strike";
            default:
                return trigger.toString();
        }
    }

    private static String trimEnd(String value) {
        int end = value.length();
        while (end > 0 && (value.charAt(end - 1) == ' ' || value.charAt(end - 1) == ',')) {
            end--;
        }
        return value.substring(0, end);
    }
}
