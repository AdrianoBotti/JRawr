// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossHandler.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.items.ItemDamageType;
import rawr.base.util.NetFormat;

/**
 * One damage event the boss can deal to the raid: a melee swing, a breath, a bolt volley, an area
 * effect, or a damage-over-time aura (see {@link DoT}).
 *
 * <p>Models use attacks in both directions: tank-survival models evaluate the incoming damage to
 * judge whether a tank can live through it, and damage/healing models read the list to know the
 * fight's shape. Fields:</p>
 *
 * <ul>
 *   <li>{@link #name} &mdash; display name of the ability.</li>
 *   <li>{@link #damageType} &mdash; the damage school ({@link ItemDamageType}: Physical, Fire,
 *       Frost, ...); the victim's resistance to that school reduces the damage.</li>
 *   <li>{@link #getDamagePerHit()} &mdash; raw damage of one hit before the victim's mitigation.
 *       When {@link #damageIsPerc} is true this is instead a fraction of the victim's maximum
 *       health (1.04 = 104% of maximum health).</li>
 *   <li>{@link #maxNumTargets} &mdash; how many raid members the attack hits: 1 is a single
 *       target, the raid size is raid-wide (area damage).</li>
 *   <li>{@link #attackSpeed} &mdash; seconds between occurrences of the attack.</li>
 *   <li>{@link #attackType} &mdash; {@link AttackTypes#AT_MELEE}, {@link AttackTypes#AT_RANGED} or
 *       {@link AttackTypes#AT_AOE}; used to group attacks for summaries and DPS totals (see
 *       {@link BossHandler#getDpsByType}).</li>
 *   <li>{@link #missable}, {@link #dodgable}, {@link #parryable}, {@link #blockable} &mdash;
 *       whether the victim's avoidance statistics can avoid the attack. An attack that cannot be
 *       avoided deals full damage. {@link #isAvoidable()} and the {@code isIgnores*} helpers
 *       summarize these.</li>
 *   <li>{@link #ignoresMTank}, {@link #ignoresOTank}, {@link #ignoresTTank} &mdash; whether the
 *       main tank, an off-tank or a third tank is excluded from the target set, so the damage
 *       falls on other raid roles instead; {@link #ignoresHealers}, {@link #ignoresMeleeDps} and
 *       {@link #ignoresRangedDps} do the same for the healer and damage-dealer groups.</li>
 *   <li>{@link #interruptable} &mdash; whether an interrupted cast stops the attack (used to model
 *       lost casting time when the player interrupts).</li>
 *   <li>{@link #useParryHaste}, {@link #isDualWielding} &mdash; combat-table details for the
 *       boss's own swings: a parried swing can reset the next swing timer, and an attack that is
 *       part of a dual wield carries an additional 20% chance to miss.</li>
 *   <li>{@link #isTheDefaultMelee} &mdash; marks the one swing that represents the boss's standard
 *       auto-attack (see {@link BossHandler#getDefaultMeleeAttack()}).</li>
 * </ul>
 *
 * <p>All fields are public and mutable, matching the original properties; {@link #toString()} is
 * the compact display form ("Spd: ... sec D: ... #T: ... PH name").</p>
 */
public class Attack {
    public String name;
    public ItemDamageType damageType = ItemDamageType.Physical;
    public boolean isTheDefaultMelee = false;
    private float damagePerHit;
    public boolean damageIsPerc = false;
    public float maxNumTargets;
    public float attackSpeed;
    public AttackTypes attackType = AttackTypes.AT_MELEE;
    public boolean useParryHaste = false;
    public boolean isDualWielding = false;
    public boolean missable = true;
    public boolean dodgable = true;
    public boolean parryable = true;
    public boolean blockable = true;
    public boolean ignoresMTank = false;
    public boolean ignoresOTank = false;
    public boolean ignoresTTank = false;
    public boolean ignoresHealers = false;
    public boolean ignoresMeleeDps = false;
    public boolean ignoresRangedDps = false;
    public boolean interruptable = false;

    public float getDamagePerHit() {
        return damagePerHit;
    }

    public void setDamagePerHit(float value) {
        damagePerHit = value;
    }

    /** True when the victim's avoidance table (miss, dodge, parry or block) can reduce this attack. */
    public boolean isAvoidable() {
        return missable || dodgable || parryable || blockable;
    }

    public boolean isIgnoresSomeone() {
        return isIgnoresSomeTanks() || isIgnoresSomeDps() || ignoresHealers;
    }

    public boolean isIgnoresAllTanks() {
        return ignoresMTank && ignoresOTank && ignoresTTank;
    }

    public boolean isIgnoresSomeTanks() {
        return ignoresMTank || ignoresOTank || ignoresTTank;
    }

    public boolean isIgnoresAllDps() {
        return ignoresMeleeDps && ignoresRangedDps;
    }

    public boolean isIgnoresSomeDps() {
        return ignoresMeleeDps || ignoresRangedDps;
    }

    @Override
    public String toString() {
        if (attackSpeed <= 0) {
            return "None";
        }
        return "Spd: " + NetFormat.optionalDecimal(attackSpeed, 1)
                + "sec D: " + NetFormat.fixed(damagePerHit, 0) + (damageIsPerc ? "%" : "")
                + " #T: " + NetFormat.fixed(maxNumTargets, 0)
                + " " + (useParryHaste ? ": PH" : "")
                + (!"Dynamic".equals(name) ? ": " + name : "");
    }
}
