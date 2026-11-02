// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtPaladin/AbilityModel.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protpally;

import rawr.base.character.Character;
import rawr.base.stats.Stats;

import java.util.LinkedHashMap;

public class AbilityModelList extends LinkedHashMap<Ability, AbilityModel> {
    public void add(Ability ability, Character character, Stats stats, CalculationOptionsProtPaladin calcOpts) {
        this.put(ability, new AbilityModel(character, stats, ability, calcOpts));
    }
}
