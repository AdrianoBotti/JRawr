// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class ValkyrDamageTemplate extends SpellTemplate {
    public float multiplier;

    public void initialize(Solver solver) {
        setName("Valkyr Damage");
        float spellCrit = 0.05f + solver.getTargetDebuffs().spellCritOnTarget;
        setRealResistance(solver.getCalculationOptions().getHolyResist());
        setPartialResistFactor((getRealResistance() == -1) ? 0 : (1 - StatConversion.getAverageResistance(solver.getCalculationOptions().getPlayerLevel(), solver.getCalculationOptions().getTargetLevel(), getRealResistance(), 0)));
        this.multiplier = getPartialResistFactor() * (1 + solver.getTargetDebuffs().bonusDamageMultiplier) * (1 + solver.getTargetDebuffs().bonusHolyDamageMultiplier) * (1 + 0.5f * spellCrit);
        setDirty(false);
    }
}
