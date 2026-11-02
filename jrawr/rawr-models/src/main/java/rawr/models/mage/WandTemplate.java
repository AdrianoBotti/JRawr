// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/SpellData.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class WandTemplate extends SpellTemplate {
    private float speed;

    public WandTemplate() {
    }

    public WandTemplate(Solver solver, MagicSchool school, int minDamage, int maxDamage, float speed) {
        initialize(solver, school, minDamage, maxDamage, speed);
    }

    public void initialize(Solver solver, MagicSchool school, int minDamage, int maxDamage, float speed) {
        setName("Wand");
        initializeEffectDamage(solver, school, minDamage, maxDamage);
        setRange(30);
        this.speed = speed;
        setCritBonus((1 + (1.5f * (1 + solver.getBaseStats().getBonusSpellCritMultiplier()) - 1)));
        setBaseSpellModifier((1 + 0.01f * solver.getMageTalents().getArcaneInstability()) * (1 + 0.01f * solver.getMageTalents().getPlayingWithFire()) * (1 + solver.getBaseStats().getBonusDamageMultiplier()));
        switch (school) {
            case Arcane:
                setBaseSpellModifier(getBaseSpellModifier() * (1 + solver.getBaseStats().getBonusArcaneDamageMultiplier()));
                break;
            case Fire:
                setBaseSpellModifier(getBaseSpellModifier() * (1 + solver.getBaseStats().getBonusFireDamageMultiplier()));
                break;
            case Frost:
                setBaseSpellModifier(getBaseSpellModifier() * (1 + solver.getBaseStats().getBonusFrostDamageMultiplier()));
                break;
            case Nature:
                setBaseSpellModifier(getBaseSpellModifier() * (1 + solver.getBaseStats().getBonusNatureDamageMultiplier()));
                break;
            case Shadow:
                setBaseSpellModifier(getBaseSpellModifier() * (1 + solver.getBaseStats().getBonusShadowDamageMultiplier()));
                break;
            default:
                break;
        }
    }

    @Override
    public Spell getSpell(CastingState castingState) {
        Spell spell = Spell.createNew(this, castingState.getSolver());
        spell.calculate(castingState);
        spell.castTime = speed;
        spell.critRate = castingState.getCritRate();

        if (spell.critRate < 0.0f) spell.critRate = 0.0f;
        if (spell.critRate > 1.0f) spell.critRate = 1.0f;

        spell.spellModifier = getBaseSpellModifier();

        spell.hitProcs = getHitRate();
        spell.critProcs = spell.hitProcs * spell.critRate;
        spell.targetProcs = spell.hitProcs;

        float[] damagePerSpellPower = new float[1];
        float[] igniteDamage = new float[1];
        float[] igniteDamagePerSpellPower = new float[1];
        spell.averageDamage = spell.calculateAverageDamage(castingState.getSolver(), 0, false, false, damagePerSpellPower, igniteDamage, igniteDamagePerSpellPower);
        spell.averageThreat = spell.averageDamage * getThreatMultiplier();
        spell.igniteDamage = 0;
        spell.igniteDamagePerSpellPower = 0;
        spell.averageCost = 0;
        spell.OO5SR = 1;
        return spell;
    }
}
