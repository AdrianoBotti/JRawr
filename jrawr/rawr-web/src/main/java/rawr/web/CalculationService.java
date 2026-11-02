package rawr.web;

import org.springframework.stereotype.Service;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.calculations.ICharacterCalculationOptions;
import rawr.data.CharacterFixtureLoader;
import rawr.models.bear.CalculationOptionsBear;
import rawr.models.cat.CalculationOptionsCat;
import rawr.models.elemental.CalculationOptionsElemental;
import rawr.models.enhance.CalculationOptionsEnhance;
import rawr.models.retribution.CalculationOptionsRetribution;
import rawr.models.healadin.CalculationOptionsHealadin;
import rawr.models.healpriest.CalculationOptionsHealPriest;
import rawr.models.hunter.CalculationOptionsHunter;
import rawr.models.shadowpriest.CalculationOptionsShadowPriest;
import rawr.models.shadowpriest.Spell;
import rawr.models.mage.CalculationOptionsMage;
import rawr.models.moonkin.CalculationOptionsMoonkin;
import rawr.models.protpally.CalculationOptionsProtPaladin;
import rawr.models.protwarr.CalculationOptionsProtWarr;
import rawr.models.restosham.CalculationOptionsRestoSham;
import rawr.models.rogue.CalculationOptionsRogue;
import rawr.models.tankdk.CalculationOptionsTankDK;
import rawr.models.tree.CalculationOptionsTree;
import rawr.models.dpsdk.CalculationOptionsDPSDK;
import rawr.models.dpswarr.CalculationOptionsDPSWarr;
import rawr.models.warlock.CalculationOptionsWarlock;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Runs a character calculation for one of the ported models. All calculations are serialized
 * behind a single global lock: the engine keeps process-wide mutable state (item/buff/enchant
 * caches, SpecialEffect interpolation caches, the model registry and {@code Calculations.Instance})
 * that is not thread-safe. See docs/dev/DECISIONS.md #33 and docs/dev/STATICS_AUDIT.md.
 */
@Service
public class CalculationService {
    public static final List<String> MODELS = List.of("Mage", "RestoSham", "Cat", "Bear", "Moonkin", "Elemental", "Enhance", "Healadin", "HealPriest", "ShadowPriest", "ProtWarr", "Retribution", "ProtPaladin", "Hunter", "Rogue", "Tree", "TankDK", "Warlock", "DPSDK", "DPSWarr");

    private final DataBootstrap dataBootstrap;
    private final ReentrantLock calculationLock = new ReentrantLock(true);

    public CalculationService(DataBootstrap dataBootstrap) {
        this.dataBootstrap = dataBootstrap;
    }

    /**
     * Runs an arbitrary engine operation under the same global lock as {@link #calculate(Character)}.
     * The optimizer job service uses this to keep long optimizer runs from racing concurrent
     * calculations on any character (the caches and the model registry are process-wide).
     */
    public <T> T withCalculationLock(java.util.function.Supplier<T> action) {
        dataBootstrap.load();
        calculationLock.lock();
        try {
            return action.get();
        } finally {
            calculationLock.unlock();
        }
    }

    /**
     * Blocks the first editor request until the game-data caches are loaded. The cache loader runs
     * as an ApplicationRunner, after Tomcat has already opened its port, so without this gate an
     * early request can reach {@code EditorService.buffGroups} with an empty Buff list.
     */
    public void ensureDataLoaded() {
        dataBootstrap.load();
    }

    /** Calculates an in-memory editor character (the HTTP-session character). */
    public WebCalculationResult calculate(Character character) {
        String modelName = character.getCurrentModel();
        if (modelName == null || !MODELS.contains(modelName)) {
            throw new IllegalArgumentException("Unknown model: " + modelName);
        }
        // Tomcat starts accepting requests before ApplicationRunner executes; guarantee the caches
        // are loaded before touching the engine (idempotent).
        dataBootstrap.load();
        calculationLock.lock();
        try {
            return calculateLocked(character, modelName);
        } finally {
            calculationLock.unlock();
        }
    }

    /**
     * Ensures the character's model is the globally loaded one. Needed before touching
     * {@code ItemInstance.getTotalStats}, whose enchant resolution calls the model-sensitive
     * {@code Enchant.findEnchant(..., null)} ({@code Calculations.instance} is global).
     */
    public void ensureModelLoaded(Character character) {
        String modelName = character.getCurrentModel();
        if (modelName == null || !MODELS.contains(modelName)) {
            return;
        }
        dataBootstrap.load();
        calculationLock.lock();
        try {
            Calculations.loadModel(Calculations.getModel(modelName).getClass());
        } finally {
            calculationLock.unlock();
        }
    }

    /** Compatibility entry point: load a saved character XML and calculate it. */
    public WebCalculationResult calculate(String characterXml, String modelName) {
        if (!MODELS.contains(modelName)) {
            throw new IllegalArgumentException("Unknown model: " + modelName);
        }
        dataBootstrap.load();
        calculationLock.lock();
        try {
            Path tempFile = Files.createTempFile("jrawr-character-", ".xml");
            try {
                Files.writeString(tempFile, characterXml, StandardCharsets.UTF_8);
                Character character = CharacterFixtureLoader.load(tempFile);
                character.setCurrentModel(modelName);
                return calculateLocked(character, modelName);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Calculation failed for model " + modelName, e);
        } finally {
            calculationLock.unlock();
        }
    }

    private WebCalculationResult calculateLocked(Character character, String modelName) {
        // Use a fresh model instance for every top-level calculation. Models keep per-run
        // carry-over state on the shared instance, and a long-lived web process must not let a
        // previous character/job change the next result.
        CalculationsBase model = Calculations.loadFreshModel(modelName);
        ICalculationOptionBase options = character.getCalculationOptions();
        if (options == null) {
            options = createDefaultOptions(modelName);
            character.setCalculationOptions(options);
        }
        character.recalculateSetBonuses();
        // Calculate on an isolated copy with a fresh options object. Models may mutate the
        // character/options as scratch state; the session character must stay repeatable.
        Character calculationCharacter = character.cloneForCalculation();
        ICalculationOptionBase calculationOptions;
        try {
            calculationOptions = model.deserializeDataObject(options.getXml());
        } catch (RuntimeException e) {
            calculationOptions = options;
        }
        if (calculationOptions == null) {
            calculationOptions = createDefaultOptions(modelName);
        }
        if (calculationOptions instanceof ICharacterCalculationOptions characterOptions) {
            characterOptions.setCharacter(calculationCharacter);
        }
        calculationCharacter.setCalculationOptions(calculationOptions);
        CharacterCalculationsBase calculations =
                model.getCharacterCalculations(calculationCharacter, null, true, true, true);
        return new WebCalculationResult(
                model.getOverallRatingLabel(),
                calculations.getOverallPoints(),
                calculations.getSubPoints(),
                model.getSubPointNameColors(),
                calculations.getCharacterDisplayCalculationValues(),
                model.getCharacterDisplayCalculationLabels());
    }

    // WoW-style average item level: the 17 gear slots count, empty slots count as 0 (shirt, tabard,
    // projectile and projectile bag are excluded), so unequipping gear cannot inflate the average.
    public static final rawr.base.character.CharacterSlot[] AVERAGE_ITEM_LEVEL_SLOTS = new rawr.base.character.CharacterSlot[] {
            rawr.base.character.CharacterSlot.Head, rawr.base.character.CharacterSlot.Neck, rawr.base.character.CharacterSlot.Shoulders,
            rawr.base.character.CharacterSlot.Chest, rawr.base.character.CharacterSlot.Waist, rawr.base.character.CharacterSlot.Legs,
            rawr.base.character.CharacterSlot.Feet, rawr.base.character.CharacterSlot.Wrist, rawr.base.character.CharacterSlot.Hands,
            rawr.base.character.CharacterSlot.Finger1, rawr.base.character.CharacterSlot.Finger2,
            rawr.base.character.CharacterSlot.Trinket1, rawr.base.character.CharacterSlot.Trinket2,
            rawr.base.character.CharacterSlot.Back, rawr.base.character.CharacterSlot.MainHand,
            rawr.base.character.CharacterSlot.OffHand, rawr.base.character.CharacterSlot.Ranged };

    public static String averageItemLevel(Character character) {
        int sum = 0;
        for (rawr.base.character.CharacterSlot slot : AVERAGE_ITEM_LEVEL_SLOTS) {
            rawr.base.items.ItemInstance instance = character.get(slot);
            if (instance != null && instance.getItem() != null) {
                sum += instance.getItem().getItemLevel();
            }
        }
        return Integer.toString(sum / AVERAGE_ITEM_LEVEL_SLOTS.length);
    }

    static ICalculationOptionBase createDefaultOptions(String modelName) {
        return switch (modelName) {
            case "Mage" -> new CalculationOptionsMage();
            case "RestoSham" -> new CalculationOptionsRestoSham();
            case "Cat" -> new CalculationOptionsCat();
            case "Bear" -> new CalculationOptionsBear();
            case "Moonkin" -> new CalculationOptionsMoonkin();
            case "Elemental" -> new CalculationOptionsElemental();
            case "Enhance" -> new CalculationOptionsEnhance();
            case "Healadin" -> new CalculationOptionsHealadin();
            case "HealPriest" -> new CalculationOptionsHealPriest();
            case "ShadowPriest" -> defaultShadowPriestOptions();
            case "ProtWarr" -> new CalculationOptionsProtWarr();
            case "Retribution" -> new CalculationOptionsRetribution();
            case "ProtPaladin" -> new CalculationOptionsProtPaladin();
            case "Hunter" -> defaultHunterOptions();
            case "Rogue" -> new CalculationOptionsRogue();
            case "TankDK" -> new CalculationOptionsTankDK();
            case "Tree" -> new CalculationOptionsTree();
            case "Warlock" -> defaultWarlockOptions();
            case "DPSDK" -> new CalculationOptionsDPSDK();
            case "DPSWarr" -> new CalculationOptionsDPSWarr();
            default -> throw new IllegalArgumentException("Unknown model: " + modelName);
        };
    }

    // C# CalculationOptionsPanelWarlock.LoadCalculationOptions replaces null options with
    // CalculationOptionsWarlock.MakeDefaultOptions(); the web editor needs the same rotations.
    private static ICalculationOptionBase defaultWarlockOptions() {
        return CalculationOptionsWarlock.makeDefaultOptions();
    }

    // C# CalculationOptionsPanelHunter.LoadCalculationOptions defaults the shot priority to the
    // Marksman group; the web editor needs a valid rotation to run the calculation.
    private static ICalculationOptionBase defaultHunterOptions() {
        CalculationOptionsHunter options = new CalculationOptionsHunter();
        options.setPriorityIndexes(new int[] { 18, 19, 4, 17, 9, 1, 7, 8, 0, 0 });
        return options;
    }

    // C# CalculationOptionsPanelShadowPriest.LoadCalculationOptions seeds the spell priority with
    // Spell.ShadowSpellList when it is null; the web editor needs that default to run the solver.
    private static ICalculationOptionBase defaultShadowPriestOptions() {
        CalculationOptionsShadowPriest options = new CalculationOptionsShadowPriest();
        options.setSpellPriority(new java.util.ArrayList<String>(Spell.ShadowSpellList));
        return options;
    }
}
