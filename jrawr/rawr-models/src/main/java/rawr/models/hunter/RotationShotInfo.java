// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/RotationTest.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.hunter;

import rawr.models.hunter.HunterEnums.Shots;

class RotationShotInfo {
    public boolean noGcd = false;           // noGCD()
    public boolean checkGcd = true;         // CheckGCD()

    public Shots type;                      // shotTable 1
    public float timeUntilOffCd = -1;       // shotTable 2
    public float cooldown = 0;              // shotTable 3
    public float unknownStat4 = -1;         // shotTable 4
    public float unknownStat5 = -1;         // shotTable 5
    public float castTime = 0;              // shotTable 6

    public float damage = 0;                // shotData 3

    public float countUsed = 0;
    public float frequency = 0;

    public RotationShotInfo(ShotData shot) {
        this.type = shot.type;
        this.cooldown = shot.cd;
        this.castTime = shot.cd;
        this.checkGcd = shot.triggersGCD;
        this.noGcd = !shot.triggersGCD && shot.type != Shots.Readiness;
        this.damage = shot.damage;
        if (shot.type == Shots.SerpentSting) this.cooldown = shot.duration;
    }
}
