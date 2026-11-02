// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossHandler.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.items.ItemDamageType;

import rawr.base.bosses.BossHandler.TierLevels;
import rawr.base.bosses.BossHandler.Versions;

import java.util.ArrayList;

/**
 * Four parallel {@link BossHandler} entries for one boss: 10-player normal, 25-player normal,
 * 10-player heroic and 25-player heroic, in that order.
 *
 * <p>Wrath of the Lich King bosses share their mechanics across raid sizes and difficulties but
 * differ in numbers (health, timers, damage). A boss class therefore derives from this list and
 * its constructor calls the array setters, which write the same logical field into all four
 * entries at once &mdash; for example
 * {@code setHealth(new float[] { h10n, h25n, h10h, h25h })}. The getters return the four values
 * as a new array. Entries for variants the original game does not have (earlier tiers have no
 * heroic modes) are left at their zero/empty defaults and are not included in
 * {@link BossList#list}.</p>
 *
 * <p>{@link #bossByVersion(BossHandler.Versions)} selects one entry: {@code V_10N} maps to index
 * 0, {@code V_25N} to 1, {@code V_10H} to 2 and {@code V_25H} to 3. The constructor seeds the
 * defaults (tier 10 heroic/normal content, two tanks, two or five healers) that each boss class
 * then overrides.</p>
 *
 * <p>{@link #genAStandardMelee(BossHandler.TierLevels)} builds the generic melee swing used by
 * most bosses: {@link BossHandler#StandardMeleePerHit} damage for the tier, a 2-second swing,
 * single target, flagged as the default melee and hitting only tanks.</p>
 */
public class MultiDiffBoss extends ArrayList<BossHandler> {
    private static final int VERSIONS = 4;

    public MultiDiffBoss() {
        add(new BossHandler());
        add(new BossHandler());
        add(new BossHandler());
        add(new BossHandler());
        setContent(new TierLevels[] { TierLevels.T10_0, TierLevels.T10_5, TierLevels.T10_5, TierLevels.T10_9 });
        setVersion(new Versions[] { Versions.V_10N, Versions.V_25N, Versions.V_10H, Versions.V_25H });
        setMinTanks(new int[] { 2, 2, 2, 2 });
        setMinHealers(new int[] { 2, 5, 2, 5 });
    }

    public void setName(String value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setName(value);
    }

    public String getName() {
        return get(0).getName();
    }

    public void setInstance(String value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setInstance(value);
    }

    public String getInstance() {
        return get(0).getInstance();
    }

    public void setContent(TierLevels[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setContent(value[i]);
    }

    public TierLevels[] getContent() {
        TierLevels[] result = new TierLevels[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getContent();
        return result;
    }

    public void setVersion(Versions[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setVersion(value[i]);
    }

    public Versions[] getVersion() {
        Versions[] result = new Versions[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getVersion();
        return result;
    }

    public void setComment(String value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setComment(value);
    }

    public String getComment() {
        return get(0).getComment();
    }

    public void setHealth(float[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setHealth(value[i]);
    }

    public float[] getHealth() {
        float[] result = new float[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getHealth();
        return result;
    }

    public void setBerserkTimer(int[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setBerserkTimer(value[i]);
    }

    public int[] getBerserkTimer() {
        int[] result = new int[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getBerserkTimer();
        return result;
    }

    public void setSpeedKillTimer(int[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setSpeedKillTimer(value[i]);
    }

    public int[] getSpeedKillTimer() {
        int[] result = new int[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getSpeedKillTimer();
        return result;
    }

    public void setInBackPercMelee(double[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setInBackPercMelee(value[i]);
    }

    public double[] getInBackPercMelee() {
        double[] result = new double[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getInBackPercMelee();
        return result;
    }

    public void setInBackPercRanged(double[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setInBackPercRanged(value[i]);
    }

    public double[] getInBackPercRanged() {
        double[] result = new double[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getInBackPercRanged();
        return result;
    }

    public void setMaxPlayers(int[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setMaxPlayers(value[i]);
    }

    public int[] getMaxPlayers() {
        int[] result = new int[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getMaxPlayers();
        return result;
    }

    public void setMinTanks(int[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setMinTanks(value[i]);
    }

    public int[] getMinTanks() {
        int[] result = new int[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getMinTanks();
        return result;
    }

    public void setMinHealers(int[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setMinHealers(value[i]);
    }

    public int[] getMinHealers() {
        int[] result = new int[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getMinHealers();
        return result;
    }

    public void setResistPhysical(double[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setResistPhysical(value[i]);
    }

    public double[] getResistPhysical() {
        double[] result = new double[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getResistPhysical();
        return result;
    }

    public void setResistFrost(double[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setResistFrost(value[i]);
    }

    public double[] getResistFrost() {
        double[] result = new double[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getResistFrost();
        return result;
    }

    public void setResistFire(double[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setResistFire(value[i]);
    }

    public double[] getResistFire() {
        double[] result = new double[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getResistFire();
        return result;
    }

    public void setResistNature(double[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setResistNature(value[i]);
    }

    public double[] getResistNature() {
        double[] result = new double[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getResistNature();
        return result;
    }

    public void setResistArcane(double[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setResistArcane(value[i]);
    }

    public double[] getResistArcane() {
        double[] result = new double[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getResistArcane();
        return result;
    }

    public void setResistShadow(double[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setResistShadow(value[i]);
    }

    public double[] getResistShadow() {
        double[] result = new double[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getResistShadow();
        return result;
    }

    public void setResistHoly(double[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setResistHoly(value[i]);
    }

    public double[] getResistHoly() {
        double[] result = new double[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getResistHoly();
        return result;
    }

    public void setTimeBossIsInvuln(float[] value) {
        for (int i = 0; i < VERSIONS; i++) get(i).setTimeBossIsInvuln(value[i]);
    }

    public float[] getTimeBossIsInvuln() {
        float[] result = new float[VERSIONS];
        for (int i = 0; i < VERSIONS; i++) result[i] = get(i).getTimeBossIsInvuln();
        return result;
    }

    /**
     * The entry for a raid-size/difficulty variant: 10N, 25N, 10H or 25H, indexed by
     * {@link BossHandler.Versions#value()}.
     */
    public BossHandler bossByVersion(Versions v) {
        return get(v.value());
    }

    /**
     * The standard melee swing for a tier: physical, single target, 2-second swing speed,
     * {@link BossHandler#StandardMeleePerHit} damage, avoidable, flagged as the default melee and
     * ignoring damage dealers and healers (it hits tanks only).
     */
    public Attack genAStandardMelee(TierLevels content) {
        Attack a = new Attack();
        a.name = "Melee";
        a.damageType = ItemDamageType.Physical;
        a.setDamagePerHit(BossHandler.StandardMeleePerHit[content.value()]);
        a.maxNumTargets = 1f;
        a.attackSpeed = 2.0f;
        a.attackType = AttackTypes.AT_MELEE;
        a.ignoresMeleeDps = true;
        a.ignoresRangedDps = true;
        a.ignoresHealers = true;
        a.isTheDefaultMelee = true;
        return a;
    }
}
