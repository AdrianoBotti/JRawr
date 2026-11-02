package rawr.web;

import org.springframework.stereotype.Service;
import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.BossOptions;
import rawr.base.bosses.DoT;
import rawr.base.character.Character;
import rawr.base.character.TargetGroup;
import rawr.base.items.ItemDamageType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Applies and validates the editable boss/encounter fields of the character card. */
@Service
public class BossOptionsService {
    static final int MAX_TEXT = 500;

    private static final List<String> SUPPORT_KEYS = List.of(
            "Level", "Armor", "Timers", "Health", "TimeSub35", "TimeSub20",
            "InBack_Melee", "InBack_Ranged", "RaidSize", "TargetGroups", "Attacks",
            "Defensive", "Moves", "Stuns", "Fears", "Roots", "Disarms", "Invulnerables");

    public void apply(Character character, Map<String, String> params) {
        BossOptions boss = character.getBossOptions();
        String name = text(params, "name", boss.getName());
        String instance = text(params, "instance", boss.getInstance());
        String comment = text(params, "comment", boss.getComment());
        String content = params.get("content");
        String version = params.get("version");

        int level = integer(params, "level", boss.getLevel(), 1, 83, "Level");
        int armor = integer(params, "armor", boss.getArmor(), 0, 1_000_000, "Armor");
        float health = number(params, "health", boss.getHealth(), 0f, 1_000_000_000f, "Health");
        int berserk = integer(params, "berserkTimer", boss.getBerserkTimer(), 0, 36_000, "Berserk timer");
        int speedKill = integer(params, "speedKillTimer", boss.getSpeedKillTimer(), 0, 36_000,
                "Speed-kill timer");
        int maxPlayers = integer(params, "maxPlayers", boss.getMaxPlayers(), 1, 40, "Max players");
        int healers = integer(params, "minHealers", boss.getMinHealers(), 0, 40, "Min healers");
        int tanks = integer(params, "minTanks", boss.getMinTanks(), 0, 40, "Min tanks");
        float invuln = number(params, "timeBossIsInvuln", boss.getTimeBossIsInvuln(), 0f, 36_000f,
                "Invulnerability time");
        double inBackMelee = decimal(params, "inBackPercMelee", boss.getInBackPercMelee(), 0d, 1d,
                "In-back melee");
        double inBackRanged = decimal(params, "inBackPercRanged", boss.getInBackPercRanged(), 0d, 1d,
                "In-back ranged");
        double under35 = decimal(params, "under35Perc", boss.getUnder35Perc(), 0d, 1d,
                "Time below 35%");
        double under20 = decimal(params, "under20Perc", boss.getUnder20Perc(), 0d, 1d,
                "Time below 20%");

        double resistPhysical = decimal(params, "resistPhysical", boss.getResistPhysical(), 0d, 1d,
                "Physical resistance");
        double resistFrost = decimal(params, "resistFrost", boss.getResistFrost(), 0d, 1d,
                "Frost resistance");
        double resistFire = decimal(params, "resistFire", boss.getResistFire(), 0d, 1d,
                "Fire resistance");
        double resistNature = decimal(params, "resistNature", boss.getResistNature(), 0d, 1d,
                "Nature resistance");
        double resistArcane = decimal(params, "resistArcane", boss.getResistArcane(), 0d, 1d,
                "Arcane resistance");
        double resistShadow = decimal(params, "resistShadow", boss.getResistShadow(), 0d, 1d,
                "Shadow resistance");
        double resistHoly = decimal(params, "resistHoly", boss.getResistHoly(), 0d, 1d,
                "Holy resistance");

        boss.setName(name);
        if (content != null && !content.isBlank()) {
            boss.setContent(BossHandler.TierLevels.valueOf(content));
        }
        boss.setInstance(instance);
        if (version != null && !version.isBlank()) {
            boss.setVersion(BossHandler.Versions.valueOf(version));
        }
        boss.setComment(comment);
        boss.setLevel(level);
        boss.setArmor(armor);
        boss.setHealth(health);
        boss.setBerserkTimer(berserk);
        boss.setSpeedKillTimer(speedKill);
        boss.setMaxPlayers(maxPlayers);
        boss.setMinHealers(healers);
        boss.setMinTanks(tanks);
        boss.setTimeBossIsInvuln(invuln);
        boss.setInBackPercMelee(inBackMelee);
        boss.setInBackPercRanged(inBackRanged);
        boss.setUnder35Perc(under35);
        boss.setUnder20Perc(under20);
        boss.setResistPhysical(resistPhysical);
        boss.setResistFrost(resistFrost);
        boss.setResistFire(resistFire);
        boss.setResistNature(resistNature);
        boss.setResistArcane(resistArcane);
        boss.setResistShadow(resistShadow);
        boss.setResistHoly(resistHoly);
        boss.setInBack(booleanValue(params, "inBack"));
        boss.setMultiTargs(booleanValue(params, "multiTargs"));
        boss.setStunningTargs(booleanValue(params, "stunningTargs"));
        boss.setMovingTargs(booleanValue(params, "movingTargs"));
        boss.setFearingTargs(booleanValue(params, "fearingTargs"));
        boss.setRootingTargs(booleanValue(params, "rootingTargs"));
        boss.setDisarmingTargs(booleanValue(params, "disarmingTargs"));
        boss.setDamagingTargs(booleanValue(params, "damagingTargs"));
    }

    public void reset(Character character) {
        character.setBossOptions(new BossOptions());
    }

    public Map<String, Boolean> supports(String modelName) {
        Map<String, Boolean> supports = new LinkedHashMap<String, Boolean>();
        for (String key : SUPPORT_KEYS) {
            supports.put(key, true);
        }
        if (modelName == null || modelName.isBlank()) {
            return supports;
        }
        Map<String, Boolean> model = BossOptions.getMyModelSupportsThis().get(modelName);
        if (model != null) {
            supports.putAll(model);
        }
        return supports;
    }

    public int addTargetGroup(Character character) {
        List<TargetGroup> targets = character.getBossOptions().targets;
        targets.add(new TargetGroup(60f, 5000f, 1f, 2f, false));
        return targets.size() - 1;
    }

    public void updateTargetGroup(Character character, int index, Map<String, String> params) {
        List<TargetGroup> targets = character.getBossOptions().targets;
        TargetGroup existing = element(targets, index, "target group");
        TargetGroup updated = new TargetGroup(
                number(params, "frequency", existing.frequency, -1f, 36_000f, "Frequency"),
                number(params, "duration", existing.duration, 0f, 3_600_000f, "Duration"),
                number(params, "chance", existing.chance * 100f, 0f, 100f, "Chance") / 100f,
                number(params, "numTargs", existing.numTargs, 0f, 100f, "Number of targets"),
                booleanValue(params, "nearBoss"));
        targets.set(index, updated);
    }

    public void deleteTargetGroup(Character character, int index) {
        List<TargetGroup> targets = character.getBossOptions().targets;
        element(targets, index, "target group");
        targets.remove(index);
    }

    public int addAttack(Character character, boolean dot) {
        List<Attack> attacks = character.getBossOptions().getAttacks();
        if (dot) {
            DoT attack = new DoT();
            attack.name = "New DoT";
            attack.damageType = ItemDamageType.Shadow;
            attack.attackType = AttackTypes.AT_AOE;
            attack.damagePerTick = 1000f;
            attack.numTicks = 1f;
            attack.tickInterval = 1f;
            attack.attackSpeed = 30f;
            attack.maxNumTargets = 1f;
            attacks.add(attack);
        } else {
            Attack attack = new Attack();
            attack.name = "New Attack";
            attack.damageType = ItemDamageType.Physical;
            attack.attackType = AttackTypes.AT_MELEE;
            attack.setDamagePerHit(80000f);
            attack.maxNumTargets = 1f;
            attack.attackSpeed = 2.5f;
            attacks.add(attack);
        }
        return attacks.size() - 1;
    }

    public void updateAttack(Character character, int index, Map<String, String> params) {
        List<Attack> attacks = character.getBossOptions().getAttacks();
        Attack existing = element(attacks, index, "attack");
        Attack updated = existing instanceof DoT ? new DoT() : new Attack();
        applyAttackFields(updated, existing, params);
        if (updated instanceof DoT dot) {
            DoT existingDot = (DoT) existing;
            dot.damagePerTick = number(params, "damagePerTick", existingDot.damagePerTick, 0f,
                    1_000_000_000f, "Damage per tick");
            dot.numTicks = number(params, "numTicks", existingDot.numTicks, 0f, 100_000f,
                    "Number of ticks");
            dot.tickInterval = number(params, "tickInterval", existingDot.tickInterval, 0f, 36_000f,
                    "Tick interval");
        }
        attacks.set(index, updated);
    }

    public void deleteAttack(Character character, int index) {
        List<Attack> attacks = character.getBossOptions().getAttacks();
        element(attacks, index, "attack");
        attacks.remove(index);
    }

    private static void applyAttackFields(Attack target, Attack existing, Map<String, String> params) {
        target.name = text(params, "name", existing.name);
        target.damageType = enumValue(ItemDamageType.class, params.get("damageType"),
                existing.damageType, "Damage type");
        target.setDamagePerHit(number(params, "damagePerHit", existing.getDamagePerHit(), 0f,
                1_000_000_000f, "Damage per hit"));
        target.damageIsPerc = booleanValue(params, "damageIsPerc");
        target.maxNumTargets = number(params, "maxNumTargets", existing.maxNumTargets, 0f, 100f,
                "Max targets");
        target.attackSpeed = number(params, "attackSpeed", existing.attackSpeed, 0f, 36_000f,
                "Attack speed");
        target.attackType = enumValue(AttackTypes.class, params.get("attackType"),
                existing.attackType, "Attack type");
        target.useParryHaste = booleanValue(params, "useParryHaste");
        target.isDualWielding = booleanValue(params, "isDualWielding");
        target.isTheDefaultMelee = booleanValue(params, "isTheDefaultMelee");
        target.interruptable = booleanValue(params, "interruptable");
        target.missable = booleanValue(params, "missable");
        target.dodgable = booleanValue(params, "dodgable");
        target.parryable = booleanValue(params, "parryable");
        target.blockable = booleanValue(params, "blockable");
        target.ignoresMTank = booleanValue(params, "ignoresMTank");
        target.ignoresOTank = booleanValue(params, "ignoresOTank");
        target.ignoresTTank = booleanValue(params, "ignoresTTank");
        target.ignoresHealers = booleanValue(params, "ignoresHealers");
        target.ignoresMeleeDps = booleanValue(params, "ignoresMeleeDps");
        target.ignoresRangedDps = booleanValue(params, "ignoresRangedDps");
    }

    private static <T> T element(List<T> list, int index, String label) {
        if (index < 0 || index >= list.size()) {
            throw new IllegalArgumentException("That " + label
                    + " no longer exists. Reopen the encounter editor.");
        }
        return list.get(index);
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw, E fallback,
            String label) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Enum.valueOf(type, raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(label + " is not recognized.");
        }
    }

    private static String text(Map<String, String> params, String key, String fallback) {
        String value = params.get(key);
        if (value == null) return fallback;
        value = value.trim();
        if (value.length() > MAX_TEXT) {
            throw new IllegalArgumentException("Text fields must be at most " + MAX_TEXT + " characters.");
        }
        return value;
    }

    private static boolean booleanValue(Map<String, String> params, String key) {
        return params.containsKey(key) && "on".equalsIgnoreCase(params.get(key))
                || "true".equalsIgnoreCase(params.get(key));
    }

    private static int integer(Map<String, String> params, String key, int fallback, int min, int max,
            String label) {
        String value = params.get(key);
        if (value == null || value.isBlank()) return fallback;
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException(label + " must be between " + min + " and " + max + ".");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a whole number.");
        }
    }

    private static float number(Map<String, String> params, String key, float fallback, float min,
            float max, String label) {
        String value = params.get(key);
        if (value == null || value.isBlank()) return fallback;
        try {
            float parsed = Float.parseFloat(value.trim());
            if (Float.isNaN(parsed) || parsed < min || parsed > max) {
                throw new IllegalArgumentException(label + " must be between " + min + " and " + max + ".");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a number.");
        }
    }

    private static double decimal(Map<String, String> params, String key, double fallback, double min,
            double max, String label) {
        String value = params.get(key);
        if (value == null || value.isBlank()) return fallback;
        try {
            double parsed = Double.parseDouble(value.trim());
            if (Double.isNaN(parsed) || parsed < min || parsed > max) {
                throw new IllegalArgumentException(label + " must be between " + min + " and " + max + ".");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a number.");
        }
    }
}
