// New loader code (not a line-by-line port): reads the original character's <Boss> element into
// the ported BossOptions/BossHandler model. Element names/order follow BossHandler.writeXml and
// BossOptions.writeXml.
package rawr.data;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.BossList;
import rawr.base.bosses.BossOptions;
import rawr.base.bosses.DoT;
import rawr.base.bosses.Impedance;
import rawr.base.character.TargetGroup;
import rawr.base.items.ItemDamageType;

import java.util.ArrayList;
import java.util.List;

/** Reads the boss options embedded in a saved character file. */
public final class BossOptionsLoader {
    private BossOptionsLoader() {
    }

    public static BossOptions load(Element root) {
        BossOptions options = new BossOptions();
        options.targets = readTargets(XmlLoader.firstChild(root, "Targets"));
        options.moves = readImpedances(XmlLoader.firstChild(root, "Moves"));
        options.stuns = readImpedances(XmlLoader.firstChild(root, "Stuns"));
        options.fears = readImpedances(XmlLoader.firstChild(root, "Fears"));
        options.roots = readImpedances(XmlLoader.firstChild(root, "Roots"));
        options.disarms = readImpedances(XmlLoader.firstChild(root, "Disarms"));

        options.setName(string(root, "Name", options.getName()));
        String content = XmlLoader.text(root, "Content");
        if (content != null) options.setContent(BossHandler.TierLevels.valueOf(content));
        options.setInstance(string(root, "Instance", options.getInstance()));
        String version = XmlLoader.text(root, "Version");
        if (version != null) options.setVersion(BossHandler.Versions.valueOf(version));
        options.setComment(string(root, "Comment", options.getComment()));
        options.setLevel(integer(root, "Level", options.getLevel()));
        options.setArmor(integer(root, "Armor", options.getArmor()));
        options.setBerserkTimer(integer(root, "BerserkTimer", options.getBerserkTimer()));
        options.setSpeedKillTimer(integer(root, "SpeedKillTimer", options.getSpeedKillTimer()));
        options.setHealth(number(root, "Health", options.getHealth()));
        options.setInBackPercMelee(decimal(root, "InBackPerc_Melee", options.getInBackPercMelee()));
        options.setInBackPercRanged(decimal(root, "InBackPerc_Ranged", options.getInBackPercRanged()));
        options.setMaxPlayers(integer(root, "Max_Players", options.getMaxPlayers()));
        options.setMinHealers(integer(root, "Min_Healers", options.getMinHealers()));
        options.setMinTanks(integer(root, "Min_Tanks", options.getMinTanks()));

        List<DoT> dots = new ArrayList<DoT>();
        List<Attack> attacks = new ArrayList<Attack>();
        readAttacks(XmlLoader.firstChild(root, "DoTs"), dots, attacks, false);
        readAttacks(XmlLoader.firstChild(root, "Attacks"), dots, attacks, true);
        options.setDoTs(dots);
        options.setAttacks(attacks);

        options.setResistPhysical(decimal(root, "Resist_Physical", options.getResistPhysical()));
        options.setResistFrost(decimal(root, "Resist_Frost", options.getResistFrost()));
        options.setResistFire(decimal(root, "Resist_Fire", options.getResistFire()));
        options.setResistNature(decimal(root, "Resist_Nature", options.getResistNature()));
        options.setResistArcane(decimal(root, "Resist_Arcane", options.getResistArcane()));
        options.setResistShadow(decimal(root, "Resist_Shadow", options.getResistShadow()));
        options.setResistHoly(decimal(root, "Resist_Holy", options.getResistHoly()));
        options.setTimeBossIsInvuln(number(root, "TimeBossIsInvuln", options.getTimeBossIsInvuln()));

        options.setInBack(bool(root, "InBack", options.getInBack()));
        options.setMultiTargs(bool(root, "MultiTargs", options.getMultiTargs()));
        options.setStunningTargs(bool(root, "StunningTargs", options.getStunningTargs()));
        options.setMovingTargs(bool(root, "MovingTargs", options.getMovingTargs()));
        options.setFearingTargs(bool(root, "FearingTargs", options.getFearingTargs()));
        options.setRootingTargs(bool(root, "RootingTargs", options.getRootingTargs()));
        options.setDisarmingTargs(bool(root, "DisarmingTargs", options.getDisarmingTargs()));
        options.setDamagingTargs(bool(root, "DamagingTargs", options.getDamagingTargs()));
        options.setUnder35Perc(decimal(root, "Under35Perc", options.getUnder35Perc()));
        options.setUnder20Perc(decimal(root, "Under20Perc", options.getUnder20Perc()));
        String filterType = XmlLoader.text(root, "FilterType");
        if (filterType != null) options.setFilterType(BossList.FilterType.valueOf(filterType));
        options.setFilter(string(root, "Filter", options.getFilter()));
        options.setBossName(string(root, "BossName", options.getBossName()));
        return options;
    }

    private static List<TargetGroup> readTargets(Element parent) {
        List<TargetGroup> targets = new ArrayList<TargetGroup>();
        if (parent == null) return targets;
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE || !((Element) n).getTagName().equals("TargetGroup")) {
                continue;
            }
            Element e = (Element) n;
            targets.add(new TargetGroup(
                    number(e, "Frequency", 0f),
                    number(e, "Duration", 0f),
                    number(e, "Chance", 0f),
                    number(e, "NumTargs", 0f),
                    bool(e, "NearBoss", false)));
        }
        return targets;
    }

    private static List<Impedance> readImpedances(Element parent) {
        List<Impedance> impedances = new ArrayList<Impedance>();
        if (parent == null) return impedances;
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE || !((Element) n).getTagName().equals("Impedance")) {
                continue;
            }
            Element e = (Element) n;
            impedances.add(new Impedance(
                    number(e, "Frequency", 0f),
                    number(e, "Duration", 0f),
                    number(e, "Chance", 0f),
                    bool(e, "Breakable", false)));
        }
        return impedances;
    }

    private static void readAttacks(Element parent, List<DoT> dots, List<Attack> attacks,
            boolean fromAttacks) {
        if (parent == null) return;
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) n;
            if (!e.getTagName().equals("Attack") && !e.getTagName().equals("DoT")) continue;
            boolean isDot = e.getTagName().equals("DoT")
                    || "DoT".equalsIgnoreCase(e.getAttribute("xsi:type"))
                    || "DoT".equalsIgnoreCase(e.getAttributeNS(
                            "http://www.w3.org/2001/XMLSchema-instance", "type"));
            Attack attack = isDot ? new DoT() : new Attack();
            attack.name = string(e, "Name", attack.name);
            String damageType = XmlLoader.text(e, "DamageType");
            if (damageType != null) attack.damageType = ItemDamageType.valueOf(damageType);
            attack.isTheDefaultMelee = bool(e, "IsTheDefaultMelee", attack.isTheDefaultMelee);
            attack.damageIsPerc = bool(e, "DamageIsPerc", attack.damageIsPerc);
            attack.maxNumTargets = number(e, "MaxNumTargets", attack.maxNumTargets);
            attack.attackSpeed = number(e, "AttackSpeed", attack.attackSpeed);
            String attackType = XmlLoader.text(e, "AttackType");
            if (attackType != null) attack.attackType = AttackTypes.valueOf(attackType);
            attack.useParryHaste = bool(e, "UseParryHaste", attack.useParryHaste);
            attack.isDualWielding = bool(e, "IsDualWielding", attack.isDualWielding);
            attack.missable = bool(e, "Missable", attack.missable);
            attack.dodgable = bool(e, "Dodgable", attack.dodgable);
            attack.parryable = bool(e, "Parryable", attack.parryable);
            attack.blockable = bool(e, "Blockable", attack.blockable);
            attack.ignoresMTank = bool(e, "IgnoresMTank", attack.ignoresMTank);
            attack.ignoresOTank = bool(e, "IgnoresOTank", attack.ignoresOTank);
            attack.ignoresTTank = bool(e, "IgnoresTTank", attack.ignoresTTank);
            attack.ignoresHealers = bool(e, "IgnoresHealers", attack.ignoresHealers);
            attack.ignoresMeleeDps = bool(e, "IgnoresMeleeDPS", attack.ignoresMeleeDps);
            attack.ignoresRangedDps = bool(e, "IgnoresRangedDPS", attack.ignoresRangedDps);
            attack.interruptable = bool(e, "Interruptable", attack.interruptable);
            attack.setDamagePerHit(number(e, "DamagePerHit", attack.getDamagePerHit()));
            if (attack instanceof DoT dot) {
                dot.damagePerTick = number(e, "DamagePerTick", dot.damagePerTick);
                dot.numTicks = number(e, "NumTicks", dot.numTicks);
                dot.tickInterval = number(e, "TickInterval", dot.tickInterval);
                if (!fromAttacks) {
                    dots.add(dot);
                }
            }
            if (fromAttacks) {
                attacks.add(attack);
            }
        }
    }

    private static String string(Element parent, String name, String fallback) {
        String value = XmlLoader.text(parent, name);
        return value == null ? fallback : value;
    }

    private static boolean bool(Element parent, String name, boolean fallback) {
        String value = XmlLoader.text(parent, name);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static int integer(Element parent, String name, int fallback) {
        String value = XmlLoader.text(parent, name);
        return value == null || value.isEmpty() ? fallback : Integer.parseInt(value);
    }

    private static float number(Element parent, String name, float fallback) {
        String value = XmlLoader.text(parent, name);
        return value == null || value.isEmpty() ? fallback : XmlLoader.parseFloat(value);
    }

    private static double decimal(Element parent, String name, double fallback) {
        String value = XmlLoader.text(parent, name);
        return value == null || value.isEmpty() ? fallback : Double.parseDouble(value);
    }
}
