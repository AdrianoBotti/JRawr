// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossHandler.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.character;

import rawr.base.bosses.BossHandler;

/**
 * The role a raid member plays in an encounter, from main tank to healer.
 *
 * <p>The constants keep the C# {@code ROLE_} prefix so they read the same as the encounter
 * definitions and serialized boss data: {@code ROLE_MainTank}, {@code ROLE_OffTank},
 * {@code ROLE_TertiaryTank}, {@code ROLE_MeleeDPS}, {@code ROLE_RangedDPS}, {@code ROLE_Healer}.
 * The role tells the encounter model how an attack is resolved and what "success" means for a
 * character (survival for tanks, output for damage dealers). The ported engine currently defines
 * the enum for parity with {@link BossHandler}; no ported calculation branches on it yet.</p>
 */
public enum PlayerRoles {
    ROLE_MainTank(0),
    ROLE_OffTank(1),
    ROLE_TertiaryTank(2),
    ROLE_MeleeDPS(3),
    ROLE_RangedDPS(4),
    ROLE_Healer(5);

    private final int value;

    PlayerRoles(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
