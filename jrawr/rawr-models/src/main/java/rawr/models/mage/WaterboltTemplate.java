// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class WaterboltTemplate extends SpellTemplate {
    Stats waterElementalBuffs;
    private static final String[] validBuffs = new String[]{"Ferocious Inspiration", "Sanctified Retribution", "Improved Moonkin Form", "Swift Retribution", "Elemental Oath", "Moonkin Form", "Wrath of Air Totem", "Demonic Pact", "Flametongue Totem", "Enhancing Totems (Spell Power)", "Totem of Wrath (Spell Power)", "Heart of the Crusader", "Master Poisoner", "Totem of Wrath", "Winter's Chill", "Improved Scorch", "Improved Shadow Bolt", "Curse of the Elements", "Earth and Moon", "Ebon Plaguebringer", "Improved Faerie Fire", "Misery"};
    float baseDamage, baseHaste, dpspBase, multiplier;

    public void initialize(Solver solver) {
        setName("Waterbolt");
        waterElementalBuffs = new Stats();
        for (Buff buff : solver.getActiveBuffs()) {
            if (java.util.Arrays.asList(validBuffs).indexOf(buff.name) >= 0) {
                waterElementalBuffs.accumulate(buff.stats);
            }
        }
        baseDamage = 292.0f + (solver.getCalculationOptions().getPlayerLevel() - 50) * 11.5f;
        CalculationOptionsMage calculationOptions = solver.getCalculationOptions();
        int playerLevel = calculationOptions.getPlayerLevel();
        int targetLevel = calculationOptions.getTargetLevel();
        float spellCrit = 0.05f + waterElementalBuffs.getSpellCrit() + waterElementalBuffs.getSpellCritOnTarget();
        float hitRate = solver.getBaseState().getFrostHitRate();
        multiplier = hitRate;
        baseHaste = (1f + waterElementalBuffs.getSpellHaste());
        multiplier *= (1 + waterElementalBuffs.getBonusDamageMultiplier()) * (1 + waterElementalBuffs.getBonusFrostDamageMultiplier());
        setRealResistance(calculationOptions.getFrostResist());
        setPartialResistFactor((getRealResistance() == -1) ? 0 : (1 - StatConversion.getAverageResistance(playerLevel, targetLevel, getRealResistance(), 0)));
        multiplier *= getPartialResistFactor() * (1 + 0.5f * spellCrit);
        dpspBase = ((1f / 3f) * 5f / 6f) * multiplier;
        setDirty(false);
    }

    @Override
    public Spell getSpell(CastingState castingState) {
        Spell spell = Spell.createNew(this, castingState.getSolver());

        float haste = castingState.getHeroism() ? baseHaste * 1.3f : baseHaste;

        spell.castTime = 2.5f / haste;
        spell.averageCost = 0.0f;
        spell.averageDamage = (baseDamage + (castingState.getFrostSpellPower() / 3f + waterElementalBuffs.getSpellPower() + waterElementalBuffs.getBonusSpellPowerDemonicPactMultiplier() * castingState.getCalculationOptions().getWarlockSpellPower()) * 5f / 6f) * multiplier;
        spell.averageThreat = 0.0f;
        spell.damagePerSpellPower = dpspBase;

        return spell;
    }
}
