// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/Spells/ISpellArgs.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.elemental;

import rawr.base.talents.ShamanTalents;
import rawr.base.stats.Stats;

public interface ISpellArgs {
    ShamanTalents getTalents();

    Stats getStats();

    float getLatencyCast();

    float getLatencyGcd();

    int getAdditionalTargets();
}
