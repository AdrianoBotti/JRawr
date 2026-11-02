// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/Markov/FuryGenerator.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the `System.Diagnostics.Stopwatch` diagnostics ("GenStateSpace"/"MarkovProcess._ctor"
// elapsed ticks) and the WinForms ErrorBox in the catch are not reproducible/portable; the catch
// swallows the exception like the original dialog did (DECISIONS #47). Console output otherwise
// matches the original with .NET invariant formatting.
package rawr.models.dpswarr;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.stats.Stats;
import rawr.base.algorithms.MarkovProcess;
import rawr.base.algorithms.State;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StateSpaceGeneratorFuryTest {
    public void stateSpaceGeneratorFuryTest1(Character c, Stats s, CombatFactors cf, WhiteAttacks wa, CalculationOptionsDPSWarr co, BossOptions bo, boolean showOutput) {
        FuryGenerator gen = new FuryGenerator(c, s, cf, wa, co, bo);
        List<State<Ability>> stateSpace = gen.generateStateSpace();
        if (showOutput) {
            try {
                int numIterations = 0;
                gen.hsPerc = 1;
                double oldDps = 0;
                while (numIterations < 2) {
                    double averageDamage = 0;
                    double rageNeededNoHs = 0f;

                    MarkovProcess<Ability> mp2 = new MarkovProcess<Ability>(stateSpace);

                    for (Map.Entry<Ability, Double> kvp : mp2.abilityWeight.entrySet()) {
                        rageNeededNoHs += kvp.getKey().rageCost * (kvp.getValue() * co.getDuration() / mp2.averageTransitionDuration);
                        averageDamage += kvp.getKey().damageOnUse() * kvp.getValue();
                    }

                    double hsdps = gen.rot.getWrapper(HeroicStrike.class).ability.damageOnUse() / wa.mhEffectiveSpeed() * gen.hsPerc;
                    double mhdps = wa.mhDamageOnUse() / wa.mhEffectiveSpeed() * (1 - gen.hsPerc);
                    double ohdps = gen.rot.whiteAtks.ohDPS();

                    double dps = averageDamage / mp2.averageTransitionDuration;
                    dps += hsdps + mhdps + ohdps;
                    System.out.println(String.format(Locale.ROOT, "DPS: %s || HSPerc: %s",
                            rawr.models.mage.NetFormat.netDoubleToString(dps),
                            rawr.models.mage.NetFormat.netDoubleToString(gen.hsPerc)));

                    double rageNeeded = rageNeededNoHs;
                    HeroicStrike hs = (HeroicStrike) gen.rot.getWrapper(HeroicStrike.class).ability;
                    rageNeeded += hs.fullRageCost() * (co.getDuration() / wa.mhEffectiveSpeed() * gen.hsPerc);
                    double rageGenerated = wa.mhSwingRage() * (co.getDuration() / wa.mhEffectiveSpeed()) +
                            wa.ohSwingRage() * (co.getDuration() / wa.ohEffectiveSpeed());

                    double hsRage = rageNeeded - rageNeededNoHs;
                    double hsRageNeeded = rageGenerated - rageNeededNoHs;
                    gen.hsPerc = Math.min((hsRageNeeded / hs.fullRageCost()) /
                            (co.getDuration() / wa.mhEffectiveSpeed()), 1); // Needed HS Activates / White activates

                    oldDps = dps;
                    numIterations++;
                }
            } catch (RuntimeException ex) {
                // ErrorBox (WinForms) omitted; the original swallowed the error and continued.
            }
        }
    }
}
