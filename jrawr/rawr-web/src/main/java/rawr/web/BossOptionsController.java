package rawr.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.BossOptions;
import rawr.base.bosses.DoT;
import rawr.base.character.Character;
import rawr.base.character.TargetGroup;
import rawr.base.items.ItemDamageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The boss/encounter options modal. Core encounter fields are edited by the main form; target
 * groups and attacks/DoTs have immediate row editors so unsaved scalar edits are not discarded.
 */
@Controller
public class BossOptionsController {
    private final BossOptionsService bossOptionsService;
    private final BossPresetService bossPresetService;
    private final EditorService editorService;
    private final EditorViews editorViews;
    private final ItemIconService itemIcons;

    public BossOptionsController(BossOptionsService bossOptionsService,
            BossPresetService bossPresetService, EditorService editorService,
            EditorViews editorViews, ItemIconService itemIcons) {
        this.bossOptionsService = bossOptionsService;
        this.bossPresetService = bossPresetService;
        this.editorService = editorService;
        this.editorViews = editorViews;
        this.itemIcons = itemIcons;
    }

    public record TargetRow(int index, TargetGroup group) {
    }

    public record AttackRow(int index, boolean dot, Attack attack) {
    }

    @ModelAttribute("icons")
    public ItemIconService icons() {
        return itemIcons;
    }

    @GetMapping("/character/boss")
    public String boss(HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        return render(character, model, null, "bossModal");
    }

    @PostMapping("/character/boss")
    public String apply(@RequestParam Map<String, String> params, HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            bossOptionsService.apply(character, params);
            editorService.markDirty(session);
        } catch (RuntimeException e) {
            return render(character, model, message(e, "Could not apply the encounter options."),
                    "bossPanel");
        }
        editorViews.populateEditor(model, character);
        return render(character, model, null, "bossSwap");
    }

    @PostMapping("/character/boss/preset")
    public String preset(@RequestParam(name = "preset", required = false) String preset,
            HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            if (preset == null || preset.isBlank()) {
                throw new IllegalArgumentException("Choose a boss preset first.");
            }
            bossPresetService.apply(character, Integer.parseInt(preset));
            editorService.markDirty(session);
        } catch (RuntimeException e) {
            return render(character, model, message(e, "Could not load the boss preset."),
                    "bossPanel");
        }
        editorViews.populateEditor(model, character);
        return render(character, model, null, "bossSwap");
    }

    @PostMapping("/character/boss/reset")
    public String reset(HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        bossOptionsService.reset(character);
        editorService.markDirty(session);
        editorViews.populateEditor(model, character);
        return render(character, model, null, "bossSwap");
    }

    @PostMapping("/character/boss/targets/add")
    public String addTargetGroup(HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        int index = bossOptionsService.addTargetGroup(character);
        editorService.markDirty(session);
        editorViews.populateEditor(model, character);
        return renderLists(character, model, null, index, -1);
    }

    @PostMapping("/character/boss/targets/update")
    public String updateTargetGroup(@RequestParam Map<String, String> params,
            @RequestParam(name = "index", required = false) String index,
            HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        int row;
        try {
            row = index(index);
            bossOptionsService.updateTargetGroup(character, row, params);
            editorService.markDirty(session);
        } catch (RuntimeException e) {
            editorViews.populateEditor(model, character);
            return renderLists(character, model,
                    message(e, "Could not update the target group."), openIndex(index), -1);
        }
        editorViews.populateEditor(model, character);
        return renderLists(character, model, null, row, -1);
    }

    @PostMapping("/character/boss/targets/delete")
    public String deleteTargetGroup(@RequestParam(name = "index", required = false) String index,
            HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            bossOptionsService.deleteTargetGroup(character, index(index));
            editorService.markDirty(session);
        } catch (RuntimeException e) {
            editorViews.populateEditor(model, character);
            return renderLists(character, model,
                    message(e, "Could not delete the target group."), -1, -1);
        }
        editorViews.populateEditor(model, character);
        return renderLists(character, model, null, -1, -1);
    }

    @PostMapping("/character/boss/attacks/add")
    public String addAttack(@RequestParam(name = "kind", defaultValue = "ATTACK") String kind,
            HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        int index = bossOptionsService.addAttack(character, "DOT".equalsIgnoreCase(kind));
        editorService.markDirty(session);
        editorViews.populateEditor(model, character);
        return renderLists(character, model, null, -1, index);
    }

    @PostMapping("/character/boss/attacks/update")
    public String updateAttack(@RequestParam Map<String, String> params,
            @RequestParam(name = "index", required = false) String index,
            HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        int row;
        try {
            row = index(index);
            bossOptionsService.updateAttack(character, row, params);
            editorService.markDirty(session);
        } catch (RuntimeException e) {
            editorViews.populateEditor(model, character);
            return renderLists(character, model, message(e, "Could not update the attack."), -1,
                    openIndex(index));
        }
        editorViews.populateEditor(model, character);
        return renderLists(character, model, null, -1, row);
    }

    @PostMapping("/character/boss/attacks/delete")
    public String deleteAttack(@RequestParam(name = "index", required = false) String index,
            HttpSession session, Model model) {
        Character character = editorService.getOrCreate(session);
        try {
            bossOptionsService.deleteAttack(character, index(index));
            editorService.markDirty(session);
        } catch (RuntimeException e) {
            editorViews.populateEditor(model, character);
            return renderLists(character, model, message(e, "Could not delete the attack."), -1, -1);
        }
        editorViews.populateEditor(model, character);
        return renderLists(character, model, null, -1, -1);
    }

    private String render(Character character, Model model, String error, String fragment) {
        populate(character, model, error, null, -1, -1);
        return "fragments/boss :: " + fragment;
    }

    private String renderLists(Character character, Model model, String error, int openTarget,
            int openAttack) {
        populate(character, model, null, error, openTarget, openAttack);
        return "fragments/boss :: bossListsSwap";
    }

    private void populate(Character character, Model model, String error, String listError,
            int openTarget, int openAttack) {
        BossOptions boss = character.getBossOptions();
        model.addAttribute("boss", boss);
        model.addAttribute("bossContents", BossHandler.TierLevels.values());
        model.addAttribute("bossVersions", BossHandler.Versions.values());
        model.addAttribute("bossPresets", bossPresetService.presets());
        model.addAttribute("bossError", error);
        model.addAttribute("bossListError", listError);
        model.addAttribute("bossSupports", bossOptionsService.supports(character.getCurrentModel()));
        model.addAttribute("bossModel", character.getCurrentModel());
        model.addAttribute("bossTargets", targetRows(boss));
        model.addAttribute("bossAttacks", attackRows(boss));
        model.addAttribute("bossLegacyDotCount",
                boss.getDoTs() == null ? 0 : boss.getDoTs().size());
        model.addAttribute("bossOpenTarget", openTarget);
        model.addAttribute("bossOpenAttack", openAttack);
        model.addAttribute("damageTypes", ItemDamageType.values());
        model.addAttribute("attackTypes", AttackTypes.values());
    }

    private static List<TargetRow> targetRows(BossOptions boss) {
        List<TargetRow> rows = new ArrayList<TargetRow>();
        for (int i = 0; i < boss.targets.size(); i++) {
            rows.add(new TargetRow(i, boss.targets.get(i)));
        }
        return rows;
    }

    private static List<AttackRow> attackRows(BossOptions boss) {
        List<AttackRow> rows = new ArrayList<AttackRow>();
        for (int i = 0; i < boss.getAttacks().size(); i++) {
            Attack attack = boss.getAttacks().get(i);
            rows.add(new AttackRow(i, attack instanceof DoT, attack));
        }
        return rows;
    }

    private static int index(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Choose a row to edit.");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Choose a valid row.");
        }
    }

    private static int openIndex(String raw) {
        try {
            return index(raw);
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    private static String message(RuntimeException e, String fallback) {
        return e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : fallback;
    }
}
