package rawr.web;

import org.springframework.stereotype.Service;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.BossList;
import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;

import java.util.ArrayList;
import java.util.List;

/**
 * Canonical encounter definitions offered as presets. Loading one copies the full BossHandler
 * (including target groups, attacks and impedances) into the character's BossOptions.
 */
@Service
public class BossPresetService {
    private final BossList bossList = new BossList();

    public List<Preset> presets() {
        List<Preset> presets = new ArrayList<Preset>();
        if (bossList.list == null) return presets;
        for (int i = 0; i < bossList.list.length; i++) {
            BossHandler boss = bossList.list[i];
            String label = boss.getContentString() + " : " + boss.getInstance()
                    + " (" + boss.getVersionString() + ") : " + boss.getName();
            presets.add(new Preset(i, label, boss.getName()));
        }
        return presets;
    }

    public void apply(Character character, int index) {
        if (bossList.list == null || index < 0 || index >= bossList.list.length) {
            throw new IllegalArgumentException("Choose a valid boss preset.");
        }
        BossHandler boss = bossList.list[index];
        BossOptions options = character.getBossOptions();
        options.cloneThis(boss);
        options.setBossName(boss.getName());
    }

    public record Preset(int index, String label, String name) {
    }
}
