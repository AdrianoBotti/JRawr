package rawr.web;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import rawr.base.character.Character;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterRace;
import rawr.base.character.Profession;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the model attributes shared by every endpoint that renders the character editor
 * (character header, paper doll, buffs, talents, stats). Extracted from
 * {@link CharacterController} so the optimizer result endpoint can re-render the same fragments.
 */
@Component
public class EditorViews {
    /** Select option (enum name + human label); avoids leaking enum names such as NightElf into the UI. */
    public record OptionView(String value, String label) {
    }

    /** One {@code <optgroup>} of the class/model picker. */
    public record ModelGroupView(String classLabel, List<OptionView> models) {
    }

    private static final List<ModelGroupView> MODEL_GROUPS = buildModelGroups();

    private final EditorService editorService;
    private final CalculationService calculationService;
    private final TalentPanelService talentPanelService;

    public EditorViews(
            EditorService editorService,
            CalculationService calculationService,
            TalentPanelService talentPanelService) {
        this.editorService = editorService;
        this.calculationService = calculationService;
        this.talentPanelService = talentPanelService;
    }

    public void populateEditor(Model model, Character character) {
        calculationService.ensureDataLoaded();
        model.addAttribute("character", character);
        model.addAttribute("modelGroups", MODEL_GROUPS);
        model.addAttribute("raceOptions", raceOptions());
        model.addAttribute("professionOptions", professionOptions());
        model.addAttribute("classLabel", classLabel(character.getCharacterClass()));
        model.addAttribute("raceLabel", raceLabel(character.getRace()));
        model.addAttribute("gearSummary", editorService.gearSummary(character));
        model.addAttribute("slotsLeft", editorService.slotViews(character, EditorService.LEFT_SLOTS));
        model.addAttribute("slotsRight", editorService.slotViews(character, EditorService.RIGHT_SLOTS));
        model.addAttribute("slotsBottom", editorService.slotViews(character, EditorService.BOTTOM_SLOTS));
        model.addAttribute("buffGroupColumns", buffColumns(editorService.buffGroups(character), 3));
        model.addAttribute("talentPanel", talentPanelService.build(character));
        populateCalculation(model, character);
    }

    public void populateCalculation(Model model, Character character) {
        if (character.getCurrentModel() == null || character.getCurrentModel().isBlank()) {
            model.addAttribute("incomplete", true);
            model.addAttribute("incompleteReason", "Choose a model to calculate ratings.");
            model.addAttribute("result", null);
            model.addAttribute("calculationError", null);
            return;
        }
        if (!editorService.hasAnyItem(character)) {
            model.addAttribute("incomplete", true);
            model.addAttribute("incompleteReason", "Equip at least one item to calculate ratings.");
            model.addAttribute("result", null);
            model.addAttribute("calculationError", null);
            return;
        }
        model.addAttribute("incomplete", false);
        model.addAttribute("incompleteReason", null);
        try {
            model.addAttribute("result", calculationService.calculate(character));
            model.addAttribute("calculationError", null);
        } catch (RuntimeException e) {
            model.addAttribute("result", null);
            model.addAttribute("calculationError", e.getMessage() != null
                    ? e.getMessage()
                    : e.getClass().getSimpleName());
        }
    }

    /** Splits the buff groups into independent column lists so expanding one group cannot shift another column. */
    private static List<List<EditorService.BuffGroupView>> buffColumns(
            List<EditorService.BuffGroupView> groups, int columnCount) {
        List<List<EditorService.BuffGroupView>> columns = new ArrayList<>(columnCount);
        int perColumn = (groups.size() + columnCount - 1) / columnCount;
        for (int column = 0; column < columnCount; column++) {
            int start = Math.min(column * perColumn, groups.size());
            int end = Math.min(start + perColumn, groups.size());
            columns.add(List.copyOf(groups.subList(start, end)));
        }
        return List.copyOf(columns);
    }

    private static List<OptionView> raceOptions() {
        List<OptionView> options = new ArrayList<>(CharacterRace.values().length);
        for (CharacterRace race : CharacterRace.values()) {
            options.add(new OptionView(race.name(), raceLabel(race)));
        }
        return List.copyOf(options);
    }

    private static List<OptionView> professionOptions() {
        List<OptionView> options = new ArrayList<>(Profession.values().length);
        for (Profession profession : Profession.values()) {
            options.add(new OptionView(profession.name(), profession == Profession.None
                    ? "None"
                    : profession.name()));
        }
        return List.copyOf(options);
    }

    /**
     * The model picker is grouped by class so all twenty ported models are visible without a long,
     * flat list. Keeping this table here (instead of in the core) keeps game-data names out of the
     * calculation layer; a test checks that every value of {@link CalculationService#MODELS} appears
     * in the rendered picker.
     */
    private static List<ModelGroupView> buildModelGroups() {
        return List.of(
                group("Death Knight",
                        option("TankDK", "Blood (tank)"),
                        option("DPSDK", "Frost / Unholy DPS")),
                group("Druid",
                        option("Bear", "Feral (tank)"),
                        option("Cat", "Feral (DPS)"),
                        option("Moonkin", "Balance"),
                        option("Tree", "Restoration")),
                group("Hunter",
                        option("Hunter", "Beast Mastery / Marksmanship / Survival")),
                group("Mage",
                        option("Mage", "Arcane / Fire / Frost")),
                group("Paladin",
                        option("Healadin", "Holy"),
                        option("ProtPaladin", "Protection"),
                        option("Retribution", "Retribution")),
                group("Priest",
                        option("HealPriest", "Holy"),
                        option("ShadowPriest", "Shadow")),
                group("Rogue",
                        option("Rogue", "Assassination / Combat / Subtlety")),
                group("Shaman",
                        option("Elemental", "Elemental"),
                        option("Enhance", "Enhancement"),
                        option("RestoSham", "Restoration")),
                group("Warlock",
                        option("Warlock", "Affliction / Demonology / Destruction")),
                group("Warrior",
                        option("ProtWarr", "Protection"),
                        option("DPSWarr", "Arms / Fury DPS")));
    }

    private static ModelGroupView group(String classLabel, OptionView... models) {
        return new ModelGroupView(classLabel, List.of(models));
    }

    private static OptionView option(String value, String label) {
        return new OptionView(value, label);
    }

    private static String raceLabel(CharacterRace race) {
        return switch (race) {
            case NightElf -> "Night Elf";
            case BloodElf -> "Blood Elf";
            default -> race.name();
        };
    }

    private static String classLabel(CharacterClass characterClass) {
        return characterClass == CharacterClass.DeathKnight ? "Death Knight" : characterClass.name();
    }
}
