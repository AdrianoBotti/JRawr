// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtWarr/AbilityModel.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protwarr;

import rawr.base.character.Character;
import rawr.base.stats.Stats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AbilityModelList {
    private final List<AbilityModel> items = new ArrayList<AbilityModel>();
    private final Map<Ability, AbilityModel> byKey = new LinkedHashMap<Ability, AbilityModel>();

    public AbilityModel get(Ability ability) {
        return byKey.get(ability);
    }

    public int getCount() {
        return items.size();
    }

    public List<AbilityModel> values() {
        return items;
    }

    public void add(AbilityModel abilityModel) {
        items.add(abilityModel);
        byKey.put(abilityModel.getAbility(), abilityModel);
    }

    public void add(Ability ability, Character character, Stats stats, CalculationOptionsProtWarr options) {
        add(new AbilityModel(character, stats, options, ability));
    }
}
