// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.TankDK/DKAbilities/CombatState.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.tankdk;

import rawr.base.talents.DeathKnightTalents;
import rawr.base.stats.Stats;

class CombatState {
    public Stats m_Stats;
    public DeathKnightTalents m_Talents;
    public Weapon MH;
    public Weapon OH;
    public float m_NumberOfTargets;
}
