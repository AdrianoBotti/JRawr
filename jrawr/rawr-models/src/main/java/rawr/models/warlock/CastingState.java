// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Warlock/CastingState.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.warlock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CastingState {

    public CharacterCalculationsWarlock mommy;
    public float elapsed;
    public float probability;
    public Map<Spell, Float> cooldowns;
    public List<Spell> series;
    public List<Integer> seriesPriorities;
    public List<Float> seriesTimes;
    public List<Boolean> seriesHits;

    public int backdraftCharges;
    public int moltenCoreCharges;

    public CastingState(
            CharacterCalculationsWarlock mommy,
            Spell precedingSpell,
            float probability) {

        this.mommy = mommy;
        this.probability = probability;
        cooldowns = new LinkedHashMap<Spell, Float>();
        seriesPriorities = new ArrayList<Integer>();
        series = new ArrayList<Spell>();
        seriesTimes = new ArrayList<Float>();
        seriesHits = new ArrayList<Boolean>();

        if (precedingSpell != null) {
            elapsed = precedingSpell.getAvgTimeUsed();
        }
    }

    public CastingState(CastingState toCopy) {
        mommy = toCopy.mommy;
        probability = toCopy.probability;
        cooldowns = new LinkedHashMap<Spell, Float>(toCopy.cooldowns);
        seriesPriorities = new ArrayList<Integer>(toCopy.seriesPriorities);
        series = new ArrayList<Spell>(toCopy.series);
        seriesTimes = new ArrayList<Float>(toCopy.seriesTimes);
        seriesHits = new ArrayList<Boolean>(toCopy.seriesHits);

        backdraftCharges = toCopy.backdraftCharges;
        moltenCoreCharges = toCopy.moltenCoreCharges;
    }

    public float getMaxTimeQueued(Spell spell) {
        if (cooldowns.containsKey(spell)) {
            return -cooldowns.get(spell);
        }

        // This method answers the question, "If my spell is being cast now,
        // how long could it have been since it queued up to be cast?"
        //
        // We know if there are lower priority spells that have already been
        // cast, it was not queued before that.  So we backtrack until we
        // find a lower priority spell.
        //
        // Lower index == higher priority.

        float t = 0;
        int newPriority = mommy.priorities.indexOf(spell);
        for (int i = seriesTimes.size(); --i >= 0; ) {
            t += seriesTimes.get(i);
            if (seriesPriorities.get(i) > newPriority) {
                return t;
            }
        }
        return elapsed;
    }

    public void addSpell(Spell spell, float timeAdvance, boolean hit) {
        elapsed += timeAdvance;
        series.add(spell);
        seriesTimes.add(timeAdvance);
        seriesPriorities.add(mommy.priorities.indexOf(spell));
        seriesHits.add(hit);
        for (Spell key : new ArrayList<Spell>(cooldowns.keySet())) {
            cooldowns.put(key, cooldowns.get(key) - timeAdvance);
        }
    }

    @Override
    public String toString() {
        String str = String.format(java.util.Locale.ROOT, "%s:", rawr.base.util.NetFormat.fixed(probability, 5));
        for (Spell spell : series) {
            str += " " + spell.getClass().getSimpleName();
        }
        return str;
    }

    public boolean lastCastHit(Spell spell) {
        return seriesHits.get(series.lastIndexOf(spell));
    }
}
