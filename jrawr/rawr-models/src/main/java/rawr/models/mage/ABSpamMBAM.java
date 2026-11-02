// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SpellInfo/CycleDataDeprecated.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class ABSpamMBAM extends Cycle {
    public ABSpamMBAM(boolean needsDisplayCalculations, CastingState castingState) {
        super(needsDisplayCalculations, castingState);
        Spell AB3;
        float MB, MB3, MB4, MB5, hit, miss;
        name = "ABSpamMBAM";

        Spell AB0 = castingState.getSpell(SpellId.ArcaneBlast0);
        Spell AB1 = castingState.getSpell(SpellId.ArcaneBlast1);
        Spell AB2 = castingState.getSpell(SpellId.ArcaneBlast2);
        AB3 = castingState.getSpell(SpellId.ArcaneBlast3);
        Spell MBAM = castingState.getSpell(SpellId.ArcaneMissilesMB);
        Spell MBAM2 = castingState.getSpell(SpellId.ArcaneMissilesMB2);
        Spell MBAM3 = castingState.getSpell(SpellId.ArcaneMissilesMB3);

        MB = 0.04f * castingState.mageTalents.getMissileBarrage();
        hit = AB3.getHitRate();
        miss = 1 - hit;

        if (MB == 0.0) {
            addSpell(needsDisplayCalculations, AB3, 1);
            calculate();
        } else {
            MB3 = MB / (1 - MB);
            MB4 = MB / (1 - MB) / (1 - MB);
            MB5 = MB / (1 - MB) / (1 - MB) / (1 - MB);

            addSpell(needsDisplayCalculations, AB3, MB);
            addSpell(needsDisplayCalculations, AB3, MB); // account for latency
            addSpell(needsDisplayCalculations, MBAM3, MB);
            addSpell(needsDisplayCalculations, AB0, MB);
            addSpell(needsDisplayCalculations, AB1, MB);
            addSpell(needsDisplayCalculations, AB2, MB);

            addSpell(needsDisplayCalculations, AB0, MB * MB3);
            addSpell(needsDisplayCalculations, AB1, MB * MB3);
            addSpell(needsDisplayCalculations, AB2, MB * MB3);
            addSpell(needsDisplayCalculations, AB3, MB * MB3); // account for latency
            addSpell(needsDisplayCalculations, MBAM3, MB * MB3);

            addSpell(needsDisplayCalculations, AB0, MB * MB4);
            addSpell(needsDisplayCalculations, AB1, MB * MB4);
            addSpell(needsDisplayCalculations, AB2, MB * MB4); // account for latency
            addSpell(needsDisplayCalculations, MBAM3, MB * MB4);

            addSpell(needsDisplayCalculations, AB0, MB * MB5);
            addSpell(needsDisplayCalculations, AB1, MB * MB5); // account for latency
            addSpell(needsDisplayCalculations, MBAM2, MB * MB5);

            addSpell(needsDisplayCalculations, AB3, 1 - MB);

            calculate();
        }
    }
}
