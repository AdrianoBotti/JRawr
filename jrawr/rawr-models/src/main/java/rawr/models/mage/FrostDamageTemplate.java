// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class FrostDamageTemplate extends SpellTemplate {
    public void initialize(Solver solver) {
        setName("Frost Damage");
        initializeEffectDamage(solver, MagicSchool.Frost, 1, 1);
        setCritBonus((1 + (1.5f * (1 + solver.getBaseStats().getBonusSpellCritMultiplier()) - 1)));
        setBaseSpellModifier(solver.getBaseSpellModifier() * (1 + solver.getBaseStats().getBonusFrostDamageMultiplier()));
        setBaseCritRate(solver.getBaseCritRate());
        setDirty(false);
    }
}
