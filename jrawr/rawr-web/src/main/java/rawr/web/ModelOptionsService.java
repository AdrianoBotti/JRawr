package rawr.web;

import org.springframework.stereotype.Service;
import rawr.base.character.Character;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.calculations.ICharacterCalculationOptions;
import rawr.models.dpswarr.CalculationOptionsDPSWarr;
import rawr.models.enhance.CalculationOptionsEnhance;
import rawr.models.hunter.CalculationOptionsHunter;
import rawr.models.hunter.Shot;
import rawr.models.mage.CalculationOptionsMage;
import rawr.models.mage.SpellId;
import rawr.models.mage.SpellWeight;
import rawr.models.restosham.CalculationOptionsRestoSham;
import rawr.models.retribution.Ability;
import rawr.models.retribution.CalculationOptionsRetribution;
import rawr.models.retribution.SimulatorParameters;
import rawr.models.shadowpriest.CalculationOptionsShadowPriest;
import rawr.models.shadowpriest.Spell;
import rawr.models.tree.CalculationOptionsTree;
import rawr.models.tree.CalculationOptionsTree.SpellProfile;
import rawr.models.warlock.CalculationOptionsWarlock;
import rawr.models.warlock.Rotation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Per-model options allowlist for the editor's "Model options" panel. Mage and RestoSham keep
 * hand-curated field lists; every other ported model gets a safe reflective fallback that exposes
 * the options object's public getter/setter pairs (and public fields for classes such as Hunter
 * that were ported with public fields) whose type is primitive/boxed/boolean/String/enum.
 * Excluded by name: Character/Calculations/Sequence/CooldownRestrictionList, caches and Xml
 * members; excluded by type: arrays, collections and other large objects, except curated
 * boolean-array fields (the DPSWarr/Enhance stat-graph weight lists). Collected options that need
 * more than a flat field (priorities, rotations, profiles, custom spell mixes and the DPSWarr
 * maintenance array) get purpose-built extra sections, with duplicate suppression and optional
 * pre-write validation / post-write normalization hooks. Discovered fields are grouped into
 * Fight / Movement &amp; Latency / Consumables &amp; Cooldowns / Advanced, with obvious numeric
 * bounds (level 1-83, percent 0-100 or 0-1 by name and default). Values are read through the
 * ported getters/fields and written through the ported setters, so types keep their exact widths;
 * a field whose current value renders identically to the fresh-defaults instance is reported as
 * "default". Validation parses every posted field before any setter runs, so invalid input never
 * corrupts the current options.
 */
@Service
public class ModelOptionsService {
    static final int TEXT_MAX_LENGTH = 500;
    private static final String UNUSED = "(unused)";

    public enum FieldType {
        NUMBER("number"),
        INTEGER("number"),
        CHECKBOX("checkbox"),
        TEXT("text"),
        SELECT("select");

        private final String inputType;

        FieldType(String inputType) {
            this.inputType = inputType;
        }
    }

    public record OptionFieldView(
            String key,
            String label,
            String inputType,
            String hint,
            String value,
            boolean isDefault,
            boolean checked,
            List<String> choices) {
    }

    public record OptionSectionView(String title, boolean advanced, List<OptionFieldView> fields) {
        /** Fields paired into rows so the two-column layout cannot offset one column against the other. */
        public List<List<OptionFieldView>> rows() {
            List<List<OptionFieldView>> rows = new ArrayList<>();
            for (int i = 0; i < fields.size(); i += 2) {
                rows.add(List.copyOf(fields.subList(i, Math.min(i + 2, fields.size()))));
            }
            return List.copyOf(rows);
        }
    }

    public record OptionsPanelView(String modelName, List<OptionSectionView> sections,
            CollectionView collection) {
    }

    public record CollectionView(String kind, String title, String hint, List<String> names,
            int selectedIndex, boolean canAdd, boolean canDelete) {
    }

    private record FieldSpec(
            String key,
            String label,
            FieldType type,
            String hint,
            List<String> choices,
            Double min,
            Double max,
            Function<Object, String> display,
            BiConsumer<Object, String> write,
            String defaultDisplay) {

        boolean isDefault(Object options) {
            return defaultDisplay.equals(display.apply(options));
        }
    }

    private record SectionSpec(String title, boolean advanced, List<FieldSpec> fields) {
    }

    private record ModelSpec<T extends ICalculationOptionBase>(
            String name,
            Class<T> type,
            Supplier<T> supplier,
            T defaults,
            List<SectionSpec> sections,
            BiConsumer<Object, Map<String, String>> validate,
            Consumer<Object> afterWrite) {

        ModelSpec(String name, Class<T> type, Supplier<T> supplier, T defaults,
                List<SectionSpec> sections) {
            this(name, type, supplier, defaults, sections, null, null);
        }
    }

    private record ExtraSections(List<SectionSpec> sections, Predicate<String> suppressFallbackKey,
            BiConsumer<Object, Map<String, String>> validate, Consumer<Object> afterWrite) {
    }

    private record FieldWrite(FieldSpec field, String raw) {
    }

    private static final Map<String, ModelSpec<?>> SPECS = buildSpecs();
    private static final Map<String, ModelSpec<?>> FALLBACK_SPECS = new ConcurrentHashMap<>();
    private static final Map<String, ExtraSections> EXTRA_SECTIONS = Map.of(
            "DPSWarr", dpsWarrExtra(),
            "Enhance", simpleExtra(statWeightsSection("Enhance")),
            "Hunter", hunterExtra(),
            "ShadowPriest", shadowPriestExtra(),
            "Warlock", warlockExtra(),
            "Retribution", retributionExtra(),
            "Tree", treeExtra());

    private final CalculationService calculationService;

    public ModelOptionsService(CalculationService calculationService) {
        this.calculationService = calculationService;
    }

    // ------------------------------------------------------------------ read

    /**
     * Panel view for the character's current model. Returns {@code null} when no model is chosen
     * and an empty-section view when the chosen model has no allowlist yet.
     */
    public OptionsPanelView describe(Character character) {
        String modelName = character.getCurrentModel();
        if (modelName == null || modelName.isBlank()) {
            return null;
        }
        ModelSpec<?> spec = specFor(modelName);
        if (spec == null) {
            return new OptionsPanelView(modelName, List.of(), null);
        }
        calculationService.ensureModelLoaded(character);
        ICalculationOptionBase current = character.getCalculationOptions();
        Object source = current != null && spec.type().isInstance(current) ? current : spec.defaults();
        prepareOptions(source);
        List<OptionSectionView> sections = new ArrayList<>(spec.sections().size());
        for (SectionSpec section : spec.sections()) {
            List<OptionFieldView> fields = new ArrayList<>(section.fields().size());
            for (FieldSpec field : section.fields()) {
                String value = field.display().apply(source);
                fields.add(new OptionFieldView(
                        field.key(),
                        field.label(),
                        field.type().inputType,
                        field.hint(),
                        value,
                        field.isDefault(source),
                        "true".equals(value),
                        field.choices()));
            }
            sections.add(new OptionSectionView(section.title(), section.advanced(), List.copyOf(fields)));
        }
        return new OptionsPanelView(spec.name(), List.copyOf(sections),
                collection(modelName, source));
    }

    // ------------------------------------------------------------------ write

    /**
     * Applies the posted values to the session character's options. All fields are validated before
     * any setter runs, so an invalid submission leaves the current options untouched. When the
     * character has no options object yet, a fresh defaults instance is created, filled and only
     * then attached.
     */
    public void apply(Character character, String postedModel, Map<String, String> params) {
        ModelSpec<?> spec = requireSpec(character);
        if (postedModel != null && !postedModel.isBlank() && !spec.name().equals(postedModel)) {
            throw new IllegalArgumentException("Model changed, reload the page.");
        }
        calculationService.ensureModelLoaded(character);
        ICalculationOptionBase existing = character.getCalculationOptions();
        boolean replace = existing == null || !spec.type().isInstance(existing);
        ICalculationOptionBase options = replace ? spec.supplier().get() : existing;
        prepareOptions(options);

        if (spec.validate() != null) {
            spec.validate().accept(options, params);
        }
        List<FieldWrite> writes = new ArrayList<>();
        for (SectionSpec section : spec.sections()) {
            for (FieldSpec field : section.fields()) {
                String raw = params.get(field.key());
                if (raw == null) {
                    if (field.type() != FieldType.CHECKBOX) {
                        continue;
                    }
                    raw = "false";
                }
                if (field.type() != FieldType.TEXT) {
                    raw = raw.trim();
                }
                validate(field, raw);
                writes.add(new FieldWrite(field, raw));
            }
        }
        for (FieldWrite write : writes) {
            write.field().write().accept(options, write.raw());
        }
        if (spec.afterWrite() != null) {
            spec.afterWrite().accept(options);
        }
        if (replace) {
            if (options instanceof ICharacterCalculationOptions withCharacter) {
                withCharacter.setCharacter(character);
            }
            character.setCalculationOptions(options);
        }
    }

    /** Replaces the current model's options with a fresh defaults instance. */
    public void reset(Character character) {
        ModelSpec<?> spec = requireSpec(character);
        calculationService.ensureModelLoaded(character);
        ICalculationOptionBase options = spec.supplier().get();
        if (options instanceof ICharacterCalculationOptions withCharacter) {
            withCharacter.setCharacter(character);
        }
        character.setCalculationOptions(options);
    }

    // ------------------------------------------------------------------ validation

    private static void validate(FieldSpec field, String raw) {
        switch (field.type()) {
            case NUMBER -> {
                float value = parseFloat(field, raw);
                if (!Float.isFinite(value)) {
                    throw error(field, "enter a number.");
                }
                checkRange(field, value);
            }
            case INTEGER -> checkRange(field, parseInt(field, raw));
            case CHECKBOX -> {
                if (!isBooleanText(raw)) {
                    throw error(field, "choose yes or no.");
                }
            }
            case TEXT -> {
                if (raw.length() > TEXT_MAX_LENGTH) {
                    throw error(field, "must be at most " + TEXT_MAX_LENGTH + " characters.");
                }
            }
            case SELECT -> {
                if (!field.choices().contains(raw)) {
                    throw error(field, "choose one of: " + String.join(", ", field.choices()) + ".");
                }
            }
        }
    }

    private static float parseFloat(FieldSpec field, String raw) {
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException e) {
            throw error(field, "enter a number.");
        }
    }

    private static int parseInt(FieldSpec field, String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw error(field, "enter a whole number.");
        }
    }

    private static void checkRange(FieldSpec field, double value) {
        boolean belowMin = field.min() != null && value < field.min();
        boolean aboveMax = field.max() != null && value > field.max();
        if (belowMin || aboveMax) {
            throw error(field, "enter a number between " + formatBound(field.min()) + " and "
                    + formatBound(field.max()) + ".");
        }
    }

    private static boolean isBooleanText(String raw) {
        return switch (raw) {
            case "true", "on", "1", "false", "off", "0", "" -> true;
            default -> false;
        };
    }

    private static boolean parseBoolean(String raw) {
        return switch (raw) {
            case "true", "on", "1" -> true;
            default -> false;
        };
    }

    private static IllegalArgumentException error(FieldSpec field, String message) {
        return new IllegalArgumentException(field.label() + ": " + message);
    }

    // ------------------------------------------------------------------ registry

    private static Map<String, ModelSpec<?>> buildSpecs() {
        Map<String, ModelSpec<?>> specs = new LinkedHashMap<>();
        addSpec(specs, buildMage());
        addSpec(specs, buildRestoSham());
        return Map.copyOf(specs);
    }

    private static void addSpec(Map<String, ModelSpec<?>> specs, ModelSpec<?> spec) {
        specs.put(spec.name(), spec);
    }

    private static ModelSpec<CalculationOptionsMage> buildMage() {
        ModelBuilder<CalculationOptionsMage> b = new ModelBuilder<>(
                "Mage", CalculationOptionsMage.class, CalculationOptionsMage::new);

        b.section("Fight", false)
                .number("fightDuration", "Fight duration", "seconds", 1, 36000,
                        CalculationOptionsMage::getFightDuration, CalculationOptionsMage::setFightDuration)
                .select("targetLevel", "Target level", null, choices(70, 83),
                        o -> Integer.toString(o.getTargetLevel()),
                        (o, raw) -> o.setTargetLevel(Integer.parseInt(raw)))
                .integer("playerLevel", "Player level", null, 1, 100,
                        CalculationOptionsMage::getPlayerLevel, CalculationOptionsMage::setPlayerLevel)
                .number("tpsLimit", "TPS limit", "threat per second; 0 = off", 0, 1_000_000_000,
                        CalculationOptionsMage::getTpsLimit, CalculationOptionsMage::setTpsLimit)
                .number("targetDamage", "Target damage", "health to burn; 0 = unlimited", 0, 1_000_000_000,
                        CalculationOptionsMage::getTargetDamage, CalculationOptionsMage::setTargetDamage);

        b.section("Movement & Latency", false)
                .number("latencyCast", "Latency (cast)", "seconds", 0, 60,
                        CalculationOptionsMage::getLatencyCast, CalculationOptionsMage::setLatencyCast)
                .number("latencyGCD", "Latency (GCD)", "seconds", 0, 60,
                        CalculationOptionsMage::getLatencyGCD, CalculationOptionsMage::setLatencyGCD)
                .number("latencyChannel", "Latency (channel)", "seconds", 0, 60,
                        CalculationOptionsMage::getLatencyChannel, CalculationOptionsMage::setLatencyChannel)
                .number("movementFrequency", "Movement frequency", "times per minute", 0, 100,
                        CalculationOptionsMage::getMovementFrequency, CalculationOptionsMage::setMovementFrequency)
                .number("movementDuration", "Movement duration", "seconds", 0, 3600,
                        CalculationOptionsMage::getMovementDuration, CalculationOptionsMage::setMovementDuration)
                .number("interruptFrequency", "Interrupt frequency", "times per minute", 0, 100,
                        CalculationOptionsMage::getInterruptFrequency, CalculationOptionsMage::setInterruptFrequency)
                .integer("aoeTargets", "AoE targets", null, 0, 100,
                        CalculationOptionsMage::getAoeTargets, CalculationOptionsMage::setAoeTargets)
                .select("aoeTargetLevel", "AoE target level", null, choices(70, 83),
                        o -> Integer.toString(o.getAoeTargetLevel()),
                        (o, raw) -> o.setAoeTargetLevel(Integer.parseInt(raw)))
                .number("aoeDuration", "AoE duration", "seconds", 0, 3600,
                        CalculationOptionsMage::getAoeDuration, CalculationOptionsMage::setAoeDuration);

        b.section("Cooldowns & Consumables", false)
                .checkbox("heroismAvailable", "Heroism available", null,
                        CalculationOptionsMage::getHeroismAvailable, CalculationOptionsMage::setHeroismAvailable)
                .checkbox("powerInfusionAvailable", "Power Infusion available", null,
                        CalculationOptionsMage::getPowerInfusionAvailable, CalculationOptionsMage::setPowerInfusionAvailable)
                .checkbox("potionOfWildMagic", "Potion of Wild Magic", null,
                        CalculationOptionsMage::getPotionOfWildMagic, CalculationOptionsMage::setPotionOfWildMagic)
                .checkbox("potionOfSpeed", "Potion of Speed", null,
                        CalculationOptionsMage::getPotionOfSpeed, CalculationOptionsMage::setPotionOfSpeed)
                .checkbox("flameCap", "Flame Cap", null,
                        CalculationOptionsMage::getFlameCap, CalculationOptionsMage::setFlameCap)
                .checkbox("drumsOfBattle", "Drums of Battle", null,
                        CalculationOptionsMage::getDrumsOfBattle, CalculationOptionsMage::setDrumsOfBattle)
                .checkbox("evocationEnabled", "Evocation", null,
                        CalculationOptionsMage::getEvocationEnabled, CalculationOptionsMage::setEvocationEnabled)
                .checkbox("manaPotionEnabled", "Mana potion", null,
                        CalculationOptionsMage::getManaPotionEnabled, CalculationOptionsMage::setManaPotionEnabled)
                .checkbox("manaGemEnabled", "Mana gem", null,
                        CalculationOptionsMage::getManaGemEnabled, CalculationOptionsMage::setManaGemEnabled)
                .checkbox("mirrorImageEnabled", "Mirror Image", null,
                        CalculationOptionsMage::getMirrorImageEnabled, CalculationOptionsMage::setMirrorImageEnabled)
                .checkbox("enableHastedEvocation", "Hasted Evocation", null,
                        CalculationOptionsMage::getEnableHastedEvocation, CalculationOptionsMage::setEnableHastedEvocation)
                .checkbox("enable2T10Evocation", "2T10 Evocation", null,
                        CalculationOptionsMage::getEnable2T10Evocation, CalculationOptionsMage::setEnable2T10Evocation)
                .number("innervate", "Innervates", "count", 0, 100,
                        CalculationOptionsMage::getInnervate, CalculationOptionsMage::setInnervate)
                .number("manaTide", "Mana Tides", "count", 0, 100,
                        CalculationOptionsMage::getManaTide, CalculationOptionsMage::setManaTide);

        b.section("Advanced", true)
                .number("drinkingTime", "Drinking time", "seconds before the fight", 0, 3600,
                        CalculationOptionsMage::getDrinkingTime, CalculationOptionsMage::setDrinkingTime)
                .number("snaredTime", "Snared uptime", "fraction of the fight (0-1)", 0, 1,
                        CalculationOptionsMage::getSnaredTime, CalculationOptionsMage::setSnaredTime)
                .number("focusMagicTargetCritRate", "Focus Magic target crit", "fraction (0-1)", 0, 1,
                        CalculationOptionsMage::getFocusMagicTargetCritRate, CalculationOptionsMage::setFocusMagicTargetCritRate)
                .number("warlockSpellPower", "Warlock spell power", null, 0, 100000,
                        CalculationOptionsMage::getWarlockSpellPower, CalculationOptionsMage::setWarlockSpellPower)
                .checkbox("unlimitedMana", "Unlimited mana", null,
                        CalculationOptionsMage::getUnlimitedMana, CalculationOptionsMage::setUnlimitedMana)
                .checkbox("maxUseAssumption", "Max-use assumption", null,
                        CalculationOptionsMage::getMaxUseAssumption, CalculationOptionsMage::setMaxUseAssumption)
                .checkbox("reconstructSequence", "Reconstruct sequence", "may be slow",
                        CalculationOptionsMage::getReconstructSequence, CalculationOptionsMage::setReconstructSequence)
                .integer("fixedSegmentDuration", "Fixed segment duration", "seconds", 0, 3600,
                        CalculationOptionsMage::getFixedSegmentDuration, CalculationOptionsMage::setFixedSegmentDuration)
                .checkbox("variableSegmentDuration", "Variable segment duration", null,
                        CalculationOptionsMage::getVariableSegmentDuration, CalculationOptionsMage::setVariableSegmentDuration)
                .text("additionalSegmentSplits", "Additional segment splits", "comma-separated times in seconds",
                        CalculationOptionsMage::getAdditionalSegmentSplits, CalculationOptionsMage::setAdditionalSegmentSplits)
                .text("cooldownRestrictions", "Cooldown restrictions", "original CooldownRestrictions syntax",
                        CalculationOptionsMage::getCooldownRestrictions, CalculationOptionsMage::setCooldownRestrictions);

        List<String> spellIds = new ArrayList<>();
        for (SpellId id : SpellId.values()) {
            spellIds.add(id.name());
        }
        b.section("Custom spell mix", true)
                .checkbox("customSpellMixEnabled", "Enable custom spell mix", null,
                        CalculationOptionsMage::getCustomSpellMixEnabled,
                        CalculationOptionsMage::setCustomSpellMixEnabled)
                .checkbox("customSpellMixOnly", "Custom spell mix only", null,
                        CalculationOptionsMage::getCustomSpellMixOnly,
                        CalculationOptionsMage::setCustomSpellMixOnly);
        for (int i = 0; i < 10; i++) {
            final int slot = i;
            b.select("customSpellMixSpell" + (i + 1), "Spell " + (i + 1), null, spellIds,
                    o -> mageMixSpell(o, slot),
                    (o, raw) -> setMageMixSpell(o, slot, raw));
            b.number("customSpellMixWeight" + (i + 1), "Weight " + (i + 1), null, 0, 1_000_000,
                    o -> mageMixWeight(o, slot),
                    (o, value) -> setMageMixWeight(o, slot, value));
        }
        b.afterWrite(o -> trimMageMix((CalculationOptionsMage) o));

        return b.build();
    }

    private static ModelSpec<CalculationOptionsRestoSham> buildRestoSham() {
        ModelBuilder<CalculationOptionsRestoSham> b = new ModelBuilder<>(
                "RestoSham", CalculationOptionsRestoSham.class, CalculationOptionsRestoSham::new);

        b.section("Fight", false)
                .number("fightLength", "Fight length", "minutes", 1, 20,
                        CalculationOptionsRestoSham::getFightLength, CalculationOptionsRestoSham::setFightLength)
                .select("targets", "Healing targets", null, List.of("Raid", "Tank", "Self", "Heavy Raid"),
                        CalculationOptionsRestoSham::getTargets, CalculationOptionsRestoSham::setTargets)
                .select("heroism", "Heroism", "who uses it", List.of("No", "Yes", "Me"),
                        CalculationOptionsRestoSham::getHeroism, CalculationOptionsRestoSham::setHeroism);

        b.section("Movement & Latency", false)
                .number("latency", "Latency", "milliseconds", 0, 500,
                        CalculationOptionsRestoSham::getLatency, CalculationOptionsRestoSham::setLatency);

        b.section("Cooldowns & Consumables", false)
                .checkbox("manaTideEveryCD", "Mana Tide every cooldown", null,
                        CalculationOptionsRestoSham::getManaTideEveryCD, CalculationOptionsRestoSham::setManaTideEveryCD)
                .checkbox("waterShield", "Water Shield", null,
                        CalculationOptionsRestoSham::getWaterShield, CalculationOptionsRestoSham::setWaterShield)
                .checkbox("earthShield", "Earth Shield", null,
                        CalculationOptionsRestoSham::getEarthShield, CalculationOptionsRestoSham::setEarthShield)
                .number("innervates", "Innervates", "count", 0, 100,
                        CalculationOptionsRestoSham::getInnervates, CalculationOptionsRestoSham::setInnervates);

        b.section("Advanced", true)
                .select("burstStyle", "Burst style", null,
                        List.of("RT+HW", "RT+LHW", "RT+CH", "HW Spam", "LHW Spam", "CH Spam"),
                        CalculationOptionsRestoSham::getBurstStyle, CalculationOptionsRestoSham::setBurstStyle)
                .select("sustStyle", "Sustained style", null,
                        List.of("RT+HW", "RT+LHW", "RT+CH", "HW Spam", "LHW Spam", "CH Spam"),
                        CalculationOptionsRestoSham::getSustStyle, CalculationOptionsRestoSham::setSustStyle)
                .number("replenishmentPercentage", "Replenishment uptime", "percent (0-100)", 0, 100,
                        CalculationOptionsRestoSham::getReplenishmentPercentage, CalculationOptionsRestoSham::setReplenishmentPercentage)
                .number("decurse", "Decurses", "count", 0, 300,
                        CalculationOptionsRestoSham::getDecurse, CalculationOptionsRestoSham::setDecurse)
                .number("survivalPerc", "Survival weight", "percent (0-100)", 0, 100,
                        CalculationOptionsRestoSham::getSurvivalPerc, CalculationOptionsRestoSham::setSurvivalPerc)
                .number("activityPerc", "Activity", "percent (1-100)", 1, 100,
                        CalculationOptionsRestoSham::getActivityPerc, CalculationOptionsRestoSham::setActivityPerc)
                .number("wsPops", "Water Shield pops", "per minute (0-20)", 0, 20,
                        CalculationOptionsRestoSham::getWSPops, CalculationOptionsRestoSham::setWSPops);

        return b.build();
    }

    private ModelSpec<?> requireSpec(Character character) {
        String modelName = character.getCurrentModel();
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("Choose a model first.");
        }
        ModelSpec<?> spec = specFor(modelName);
        if (spec == null) {
            throw new IllegalArgumentException("No editable options for model " + modelName + ".");
        }
        return spec;
    }

    /** Curated allowlist when one exists, otherwise the cached reflective fallback. */
    private static ModelSpec<?> specFor(String modelName) {
        ModelSpec<?> curated = SPECS.get(modelName);
        if (curated != null) {
            return curated;
        }
        if (!CalculationService.MODELS.contains(modelName)) {
            return null;
        }
        ModelSpec<?> fallback = FALLBACK_SPECS.computeIfAbsent(
                modelName, ModelOptionsService::buildFallbackSpec);
        ExtraSections extras = EXTRA_SECTIONS.get(modelName);
        if (extras == null) {
            return fallback;
        }
        Set<String> extraKeys = new HashSet<>();
        for (SectionSpec section : extras.sections()) {
            for (FieldSpec field : section.fields()) {
                extraKeys.add(field.key());
            }
        }
        List<SectionSpec> sections = new ArrayList<>();
        for (SectionSpec section : fallback.sections()) {
            List<FieldSpec> fields = new ArrayList<>();
            for (FieldSpec field : section.fields()) {
                if (!extraKeys.contains(field.key())
                        && (extras.suppressFallbackKey() == null
                                || !extras.suppressFallbackKey().test(field.key()))) {
                    fields.add(field);
                }
            }
            if (!fields.isEmpty()) {
                sections.add(new SectionSpec(section.title(), section.advanced(), List.copyOf(fields)));
            }
        }
        sections.addAll(extras.sections());
        @SuppressWarnings({ "rawtypes", "unchecked" })
        ModelSpec<?> merged = new ModelSpec(fallback.name(), fallback.type(), fallback.supplier(),
                fallback.defaults(), List.copyOf(sections), extras.validate(), extras.afterWrite());
        return merged;
    }

    private static ExtraSections simpleExtra(SectionSpec... sections) {
        return new ExtraSections(List.of(sections), null, null, null);
    }

    private static CollectionView collection(String modelName, Object source) {
        if (source instanceof CalculationOptionsWarlock warlock) {
            List<String> names = new ArrayList<>();
            for (Rotation rotation : warlock.getRotations()) {
                names.add(rotation.name);
            }
            int selected = names.isEmpty() ? -1
                    : Math.max(0, Math.min(warlock.getActiveRotationIndex(), names.size() - 1));
            return new CollectionView("warlockRotation", "Rotation",
                    "The selected rotation is edited below.", List.copyOf(names), selected,
                    true, !names.isEmpty());
        }
        if (source instanceof CalculationOptionsTree tree) {
            List<SpellProfile> profiles = tree.getProfiles();
            List<String> names = new ArrayList<>();
            int selected = -1;
            if (profiles != null) {
                for (int i = 0; i < profiles.size(); i++) {
                    SpellProfile profile = profiles.get(i);
                    names.add(profile.getName());
                    if (profile == tree.getCurrent()) {
                        selected = i;
                    }
                }
            }
            return new CollectionView("treeProfile", "Profile",
                    "Select a saved profile, or add the current one to the library.",
                    List.copyOf(names), selected, true, !names.isEmpty());
        }
        return null;
    }

    public void applyCollection(Character character, String kind, String action, int index) {
        String modelName = character.getCurrentModel();
        String verb = action == null || action.isBlank() ? "SELECT" : action.toUpperCase(Locale.ROOT);
        if ("warlockRotation".equals(kind)) {
            if (!"Warlock".equals(modelName)) {
                throw new IllegalArgumentException("The selected rotation belongs to another model.");
            }
            calculationService.ensureModelLoaded(character);
            CalculationOptionsWarlock warlock = warlockOptions(character);
            List<Rotation> rotations = warlock.getRotations();
            if (rotations == null) {
                rotations = new ArrayList<>();
                warlock.setRotations(rotations);
            }
            switch (verb) {
                case "SELECT" -> {
                    checkCollectionIndex(index, rotations.size(), "rotation");
                    warlock.setActiveRotationIndex(index);
                }
                case "ADD" -> {
                    rotations.add(new Rotation(uniqueRotationName(rotations), "Shadow Bolt", null));
                    warlock.setActiveRotationIndex(rotations.size() - 1);
                }
                case "DELETE" -> {
                    checkCollectionIndex(warlock.getActiveRotationIndex(), rotations.size(), "rotation");
                    rotations.remove(warlock.getActiveRotationIndex());
                    if (rotations.isEmpty()) {
                        rotations.add(new Rotation("Rotation 1", "Shadow Bolt", null));
                    }
                    warlock.setActiveRotationIndex(
                            Math.min(warlock.getActiveRotationIndex(), rotations.size() - 1));
                }
                default -> throw new IllegalArgumentException("Unknown rotation action.");
            }
            return;
        }
        if ("treeProfile".equals(kind)) {
            if (!"Tree".equals(modelName)) {
                throw new IllegalArgumentException("The selected profile belongs to another model.");
            }
            calculationService.ensureModelLoaded(character);
            CalculationOptionsTree tree = treeOptions(character);
            List<SpellProfile> profiles = tree.getProfiles();
            if (profiles == null) {
                profiles = new ArrayList<>();
                tree.setProfiles(profiles);
            }
            switch (verb) {
                case "SELECT" -> {
                    checkCollectionIndex(index, profiles.size(), "profile");
                    tree.setCurrent(profiles.get(index));
                }
                case "ADD" -> {
                    SpellProfile copy = copyProfile(currentProfile(tree));
                    copy.setName(uniqueProfileName(profiles));
                    profiles.add(copy);
                    tree.setCurrent(copy);
                }
                case "DELETE" -> {
                    int current = indexOfIdentity(profiles, tree.getCurrent());
                    if (current < 0) {
                        throw new IllegalArgumentException(
                                "The current profile is not saved in the library.");
                    }
                    profiles.remove(current);
                    tree.setCurrent(profiles.isEmpty()
                            ? new SpellProfile()
                            : profiles.get(Math.min(current, profiles.size() - 1)));
                }
                default -> throw new IllegalArgumentException("Unknown profile action.");
            }
            return;
        }
        throw new IllegalArgumentException("Unknown options collection.");
    }

    private CalculationOptionsWarlock warlockOptions(Character character) {
        ICalculationOptionBase options = character.getCalculationOptions();
        if (options instanceof CalculationOptionsWarlock warlock) {
            return warlock;
        }
        CalculationOptionsWarlock warlock =
                (CalculationOptionsWarlock) CalculationService.createDefaultOptions("Warlock");
        character.setCalculationOptions(warlock);
        return warlock;
    }

    private CalculationOptionsTree treeOptions(Character character) {
        ICalculationOptionBase options = character.getCalculationOptions();
        if (options instanceof CalculationOptionsTree tree) {
            return tree;
        }
        CalculationOptionsTree tree =
                (CalculationOptionsTree) CalculationService.createDefaultOptions("Tree");
        character.setCalculationOptions(tree);
        return tree;
    }

    private static void checkCollectionIndex(int index, int size, String label) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("That " + label + " no longer exists.");
        }
    }

    private static String uniqueRotationName(List<Rotation> rotations) {
        String base = "New Rotation";
        String name = base;
        int suffix = 2;
        while (containsRotationName(rotations, name)) {
            name = base + " " + suffix++;
        }
        return name;
    }

    private static boolean containsRotationName(List<Rotation> rotations, String name) {
        for (Rotation rotation : rotations) {
            if (Objects.equals(rotation.name, name)) {
                return true;
            }
        }
        return false;
    }

    private static <T extends ICalculationOptionBase> Supplier<T> modelDefaults(String modelName) {
        return () -> {
            @SuppressWarnings("unchecked")
            T options = (T) CalculationService.createDefaultOptions(modelName);
            return options;
        };
    }

    private static ExtraSections dpsWarrExtra() {
        Object defaults = CalculationService.createDefaultOptions("DPSWarr");
        List<SectionSpec> sections = new ArrayList<>();
        sections.add(statWeightsSection("DPSWarr"));
        sections.add(maintenanceSection("Maintenance: rage and shouts", defaults,
                new int[] { 1, 2, 5, 6, 7, 8, 9, 10 },
                new String[] { "Berserker Rage", "Bloodrage", "Battle Shout", "Commanding Shout",
                        "Demoralizing Shout", "Sunder Armor", "Thunder Clap", "Hamstring" }));
        sections.add(maintenanceSection("Maintenance: periodics", defaults,
                new int[] { 12, 13, 14, 15, 16 },
                new String[] { "Shattering Throw", "Sweeping Strikes", "Death Wish",
                        "Recklessness", "Enraged Regeneration" }));
        sections.add(maintenanceSection("Maintenance: damage dealers", defaults,
                new int[] { 19, 20, 21, 23, 24, 25, 26, 27, 28, 29, 30 },
                new String[] { "Whirlwind", "Bloodthirst", "Bloodsurge", "Bladestorm",
                        "Mortal Strike", "Rend", "Overpower", "Taste for Blood",
                        "Sudden Death", "Slam", "Execute (<20%)" }));
        sections.add(maintenanceSection("Maintenance: rage dumps", defaults,
                new int[] { 32, 33 }, new String[] { "Cleave", "Heroic Strike" }));
        return new ExtraSections(List.copyOf(sections), key -> key.startsWith("m_"), null, null);
    }

    private static SectionSpec maintenanceSection(String title, Object defaults, int[] indexes,
            String[] labels) {
        List<FieldSpec> fields = new ArrayList<>();
        for (int i = 0; i < indexes.length; i++) {
            String label = labels[i];
            String key = "maintenance" + label.replaceAll("[^A-Za-z0-9]", "");
            fields.add(booleanArrayField(key, label, indexes[i],
                    o -> ((CalculationOptionsDPSWarr) o).getMaintenance(), defaults));
        }
        return new SectionSpec(title, true, List.copyOf(fields));
    }

    private static ExtraSections hunterExtra() {
        ModelBuilder<CalculationOptionsHunter> b = new ModelBuilder<>("Hunter",
                CalculationOptionsHunter.class, modelDefaults("Hunter"));
        List<String> shots = new ArrayList<>();
        for (Shot shot : CalculationOptionsHunter.getShotList()) {
            shots.add(shot.name);
        }
        b.section("Shot priority", false);
        for (int i = 0; i < 10; i++) {
            final int slot = i;
            b.select("priority" + (i + 1), "Priority " + (i + 1),
                    i == 0 ? "top of the priority first" : null, shots,
                    o -> hunterShotName(o.getPriorityIndexes()[slot]),
                    (o, raw) -> {
                        int[] indexes = o.getPriorityIndexes();
                        indexes[slot] = hunterShotIndex(raw);
                        o.setPriorityIndexes(indexes);
                    });
        }
        ModelSpec<CalculationOptionsHunter> spec = b.build();
        return new ExtraSections(spec.sections(), key -> key.matches("priorityIndex\\d+"),
                null, null);
    }

    private static String hunterShotName(int index) {
        List<Shot> shots = CalculationOptionsHunter.getShotList();
        return index >= 0 && index < shots.size() ? shots.get(index).name : "None";
    }

    private static int hunterShotIndex(String name) {
        for (Shot shot : CalculationOptionsHunter.getShotList()) {
            if (shot.name.equals(name)) {
                return shot.index;
            }
        }
        return 0;
    }

    private static ExtraSections shadowPriestExtra() {
        ModelBuilder<CalculationOptionsShadowPriest> b = new ModelBuilder<>("ShadowPriest",
                CalculationOptionsShadowPriest.class, modelDefaults("ShadowPriest"));
        List<String> choices = new ArrayList<>();
        choices.add(UNUSED);
        choices.addAll(Spell.ShadowSpellList);
        b.section("Spell priority", false);
        for (int i = 0; i < 6; i++) {
            final int slot = i;
            b.select("spellPriority" + (i + 1), "Priority " + (i + 1),
                    i == 0 ? "first spell to cast" : null, choices,
                    o -> priorityAt(o.getSpellPriority(), slot),
                    (o, raw) -> o.setSpellPriority(priorityAtSet(o.getSpellPriority(), slot, raw)));
        }
        b.validate((o, params) -> validatePrioritySlots(params, "spellPriority", 6, "spell"))
                .afterWrite(o -> {
                    CalculationOptionsShadowPriest options =
                            (CalculationOptionsShadowPriest) o;
                    options.setSpellPriority(trimPriority(options.getSpellPriority()));
                });
        ModelSpec<CalculationOptionsShadowPriest> spec = b.build();
        return new ExtraSections(spec.sections(), null, spec.validate(), spec.afterWrite());
    }

    private static ExtraSections warlockExtra() {
        ModelBuilder<CalculationOptionsWarlock> b = new ModelBuilder<>("Warlock",
                CalculationOptionsWarlock.class, modelDefaults("Warlock"));
        List<String> fillers = List.of("Shadow Bolt", "Incinerate");
        List<String> executes = List.of(UNUSED, "Drain Soul", "Soul Fire");
        List<String> spells = new ArrayList<>();
        spells.add(UNUSED);
        spells.addAll(rawr.models.warlock.Spell.ALL_SPELLS);
        b.section("Rotation", false)
                .text("rotationName", "Rotation name", null,
                        o -> activeWarlockRotation(o).name,
                        (o, raw) -> activeWarlockRotation(o).name = raw)
                .select("rotationFiller", "Filler", null, fillers,
                        o -> activeWarlockRotation(o).filler,
                        (o, raw) -> activeWarlockRotation(o).filler = raw)
                .select("rotationExecute", "Execute", null, executes,
                        o -> {
                            String execute = activeWarlockRotation(o).execute;
                            return execute == null ? UNUSED : execute;
                        },
                        (o, raw) -> activeWarlockRotation(o).execute =
                                UNUSED.equals(raw) ? null : raw);
        for (int i = 0; i < 12; i++) {
            final int slot = i;
            b.select("rotationPriority" + (i + 1), "Priority " + (i + 1),
                    i == 0 ? "spells are cast top to bottom" : null, spells,
                    o -> priorityAt(activeWarlockRotation(o).spellPriority, slot),
                    (o, raw) -> activeWarlockRotation(o).spellPriority =
                            priorityAtSet(activeWarlockRotation(o).spellPriority, slot, raw));
        }
        b.validate((o, params) -> validateWarlockRotation((CalculationOptionsWarlock) o, params))
                .afterWrite(o -> {
                    Rotation rotation = activeWarlockRotation((CalculationOptionsWarlock) o);
                    rotation.spellPriority = trimPriority(rotation.spellPriority);
                });
        ModelSpec<CalculationOptionsWarlock> spec = b.build();
        return new ExtraSections(spec.sections(), key -> key.equals("activeRotationIndex"),
                spec.validate(), spec.afterWrite());
    }

    private static Rotation activeWarlockRotation(CalculationOptionsWarlock options) {
        List<Rotation> rotations = options.getRotations();
        if (rotations == null) {
            rotations = new ArrayList<>();
            options.setRotations(rotations);
        }
        if (rotations.isEmpty()) {
            rotations.add(new Rotation("Rotation 1", "Shadow Bolt", null));
        }
        int index = Math.max(0, Math.min(options.getActiveRotationIndex(), rotations.size() - 1));
        options.setActiveRotationIndex(index);
        return rotations.get(index);
    }

    private static void validateWarlockRotation(CalculationOptionsWarlock options,
            Map<String, String> params) {
        String name = params.get("rotationName");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Rotation name cannot be blank.");
        }
        List<Rotation> rotations = options.getRotations();
        int active = rotations == null ? -1
                : Math.max(0, Math.min(options.getActiveRotationIndex(), rotations.size() - 1));
        if (rotations != null) {
            for (int i = 0; i < rotations.size(); i++) {
                if (i != active && name.equals(rotations.get(i).name)) {
                    throw new IllegalArgumentException(
                            "There is already a rotation named " + name + ".");
                }
            }
        }
        validatePrioritySlots(params, "rotationPriority", 12, "spell");
        String filler = params.getOrDefault("rotationFiller", "Shadow Bolt");
        String execute = params.get("rotationExecute");
        Rotation probe = new Rotation(name, filler,
                execute == null || UNUSED.equals(execute) ? null : execute);
        for (int i = 1; i <= 12; i++) {
            String raw = params.get("rotationPriority" + i);
            if (raw != null && !raw.isBlank() && !UNUSED.equals(raw)) {
                probe.spellPriority.add(raw);
            }
        }
        String error = probe.getError();
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
    }

    private static ExtraSections retributionExtra() {
        ModelBuilder<CalculationOptionsRetribution> b = new ModelBuilder<>("Retribution",
                CalculationOptionsRetribution.class, modelDefaults("Retribution"));
        List<String> abilities = List.of(UNUSED, "Crusader Strike", "Hammer of Wrath",
                "Judgement", "Consecration", "Divine Storm", "Exorcism");
        b.section("Rotation", false);
        for (int i = 0; i < 6; i++) {
            final int slot = i;
            b.select("rotationAbility" + (i + 1), "Priority " + (i + 1),
                    i == 0 ? "abilities are used top to bottom" : null, abilities,
                    o -> retributionAbilityAt(o, slot),
                    (o, raw) -> setRetributionAbility(o, slot, raw));
        }
        b.validate((o, params) -> validatePrioritySlots(params, "rotationAbility", 6, "ability"))
                .afterWrite(o -> compactRetributionRotation((CalculationOptionsRetribution) o));
        ModelSpec<CalculationOptionsRetribution> spec = b.build();
        return new ExtraSections(spec.sections(), key -> key.equals("effectiveCD"),
                spec.validate(), spec.afterWrite());
    }

    private static Ability[] retributionRotation(CalculationOptionsRetribution options) {
        List<Ability[]> rotations = options.getRotations();
        if (rotations == null || rotations.isEmpty() || rotations.get(0) == null) {
            return SimulatorParameters.defaultRotation();
        }
        return rotations.get(0);
    }

    private static String retributionAbilityAt(CalculationOptionsRetribution options, int slot) {
        Ability[] rotation = retributionRotation(options);
        if (slot >= rotation.length || rotation[slot] == null) {
            return UNUSED;
        }
        return SimulatorParameters.abilityString(rotation[slot]);
    }

    private static void setRetributionAbility(CalculationOptionsRetribution options, int slot,
            String raw) {
        Ability[] current = retributionRotation(options);
        Ability[] updated = new Ability[Math.max(6, current.length)];
        System.arraycopy(current, 0, updated, 0, current.length);
        updated[slot] = UNUSED.equals(raw) ? null : retributionAbility(raw);
        List<Ability[]> rotations = options.getRotations();
        if (rotations == null) {
            rotations = new ArrayList<>();
            options.setRotations(rotations);
        }
        if (rotations.isEmpty()) {
            rotations.add(updated);
        } else {
            rotations.set(0, updated);
        }
    }

    private static Ability retributionAbility(String label) {
        return switch (label) {
            case "Crusader Strike" -> Ability.CrusaderStrike;
            case "Hammer of Wrath" -> Ability.HammerOfWrath;
            case "Judgement" -> Ability.Judgement;
            case "Consecration" -> Ability.Consecration;
            case "Divine Storm" -> Ability.DivineStorm;
            case "Exorcism" -> Ability.Exorcism;
            default -> null;
        };
    }

    private static void compactRetributionRotation(CalculationOptionsRetribution options) {
        List<Ability[]> rotations = options.getRotations();
        if (rotations == null || rotations.isEmpty() || rotations.get(0) == null) {
            return;
        }
        Ability[] compact = Arrays.stream(rotations.get(0))
                .filter(Objects::nonNull)
                .toArray(Ability[]::new);
        if (compact.length == 0) {
            rotations.remove(0);
        } else {
            rotations.set(0, compact);
        }
    }

    private static ExtraSections treeExtra() {
        ModelBuilder<CalculationOptionsTree> b = new ModelBuilder<>("Tree",
                CalculationOptionsTree.class, modelDefaults("Tree"));
        b.section("Profile", false)
                .text("profileName", "Profile name", null,
                        o -> currentProfile(o).getName(),
                        (o, raw) -> currentProfile(o).setName(raw));
        profileInteger(b, "profileFightDuration", "Fight duration", "seconds", 1, 36000,
                SpellProfile::getFightDuration, SpellProfile::setFightDuration);
        profileInteger(b, "profileReplenishmentUptime", "Replenishment uptime", "percent", 0, 100,
                SpellProfile::getReplenishmentUptime, SpellProfile::setReplenishmentUptime);
        profileInteger(b, "profileWildGrowthPerMinute", "Wild Growths per minute", null, 0, 100,
                SpellProfile::getWildGrowthPerMinute, SpellProfile::setWildGrowthPerMinute);
        profileInteger(b, "profileInnervates", "Innervates", null, 0, 100,
                SpellProfile::getInnervates, SpellProfile::setInnervates);
        profileInteger(b, "profileSwiftmendPerMinute", "Swiftmends per minute", null, 0, 100,
                SpellProfile::getSwiftmendPerMinute, SpellProfile::setSwiftmendPerMinute);
        profileInteger(b, "profileIdleCastTimePercent", "Idle cast time", "percent", 0, 100,
                SpellProfile::getIdleCastTimePercent, SpellProfile::setIdleCastTimePercent);
        profileInteger(b, "profileLivingSeedEfficiency", "Living Seed efficiency", "percent", 0, 100,
                SpellProfile::getLivingSeedEfficiency, SpellProfile::setLivingSeedEfficiency);
        profileInteger(b, "profileRevitalizePPM", "Revitalize PPM", null, 0, 100,
                SpellProfile::getRevitalizePPM, SpellProfile::setRevitalizePPM);
        profileInteger(b, "profileRejuvFrac", "Rejuvenation fraction", "percent", 0, 100,
                SpellProfile::getRejuvFrac, SpellProfile::setRejuvFrac);
        profileInteger(b, "profileRegrowthFrac", "Regrowth fraction", "percent", 0, 100,
                SpellProfile::getRegrowthFrac, SpellProfile::setRegrowthFrac);
        profileInteger(b, "profileLifebloomFrac", "Lifebloom fraction", "percent", 0, 100,
                SpellProfile::getLifebloomFrac, SpellProfile::setLifebloomFrac);
        profileInteger(b, "profileNourishFrac", "Nourish fraction", "percent", 0, 100,
                SpellProfile::getNourishFrac, SpellProfile::setNourishFrac);
        profileInteger(b, "profileLifebloomStackAmount", "Lifebloom stack amount", null, 0, 100,
                SpellProfile::getLifebloomStackAmount, SpellProfile::setLifebloomStackAmount);
        profileInteger(b, "profileRejuvAmount", "Rejuvenation amount", null, 0, 1_000_000,
                SpellProfile::getRejuvAmount, SpellProfile::setRejuvAmount);
        profileInteger(b, "profileRegrowthAmount", "Regrowth amount", null, 0, 1_000_000,
                SpellProfile::getRegrowthAmount, SpellProfile::setRegrowthAmount);
        profileInteger(b, "profileLifebloomStackType", "Lifebloom stack type", null, 0, 10,
                SpellProfile::getLifebloomStackType, SpellProfile::setLifebloomStackType);
        profileInteger(b, "profileNourish1", "Nourish 1", null, 0, 100,
                SpellProfile::getNourish1, SpellProfile::setNourish1);
        profileInteger(b, "profileNourish2", "Nourish 2", null, 0, 100,
                SpellProfile::getNourish2, SpellProfile::setNourish2);
        profileInteger(b, "profileNourish3", "Nourish 3", null, 0, 100,
                SpellProfile::getNourish3, SpellProfile::setNourish3);
        profileInteger(b, "profileNourish4", "Nourish 4", null, 0, 100,
                SpellProfile::getNourish4, SpellProfile::setNourish4);

        b.section("Time adjustments", true);
        profileInteger(b, "profileAdjustTimeRejuv", "Rejuvenation time", "percent", 0, 10000,
                SpellProfile::getAdjustTimeRejuv, SpellProfile::setAdjustTimeRejuv);
        profileInteger(b, "profileAdjustTimeRegrowth", "Regrowth time", "percent", 0, 10000,
                SpellProfile::getAdjustTimeRegrowth, SpellProfile::setAdjustTimeRegrowth);
        profileInteger(b, "profileAdjustTimeNourish", "Nourish time", "percent", 0, 10000,
                SpellProfile::getAdjustTimeNourish, SpellProfile::setAdjustTimeNourish);
        profileInteger(b, "profileAdjustTimeLifebloom", "Lifebloom time", "percent", 0, 10000,
                SpellProfile::getAdjustTimeLifebloom, SpellProfile::setAdjustTimeLifebloom);
        profileInteger(b, "profileAdjustTimeSwiftmend", "Swiftmend time", "percent", 0, 10000,
                SpellProfile::getAdjustTimeSwiftmend, SpellProfile::setAdjustTimeSwiftmend);
        profileInteger(b, "profileAdjustTimeWildGrowth", "Wild Growth time", "percent", 0, 10000,
                SpellProfile::getAdjustTimeWildGrowth, SpellProfile::setAdjustTimeWildGrowth);
        profileInteger(b, "profileAdjustTimeIdle", "Idle time", "percent", 0, 10000,
                SpellProfile::getAdjustTimeIdle, SpellProfile::setAdjustTimeIdle);
        profileInteger(b, "profileAdjustTimeManagedRejuv", "Managed Rejuvenation time", "percent", 0, 10000,
                SpellProfile::getAdjustTimeManagedRejuv, SpellProfile::setAdjustTimeManagedRejuv);
        profileInteger(b, "profileAdjustTimeManagedRegrowth", "Managed Regrowth time", "percent", 0, 10000,
                SpellProfile::getAdjustTimeManagedRegrowth, SpellProfile::setAdjustTimeManagedRegrowth);
        profileInteger(b, "profileAdjustTimeManagedLifebloomStack", "Managed Lifebloom time", "percent", 0, 10000,
                SpellProfile::getAdjustTimeManagedLifebloomStack, SpellProfile::setAdjustTimeManagedLifebloomStack);
        profileInteger(b, "profileAdjustTimeRejuvOrder", "Rejuvenation order", null, 0, 100,
                SpellProfile::getAdjustTimeRejuvOrder, SpellProfile::setAdjustTimeRejuvOrder);
        profileInteger(b, "profileAdjustTimeRegrowthOrder", "Regrowth order", null, 0, 100,
                SpellProfile::getAdjustTimeRegrowthOrder, SpellProfile::setAdjustTimeRegrowthOrder);
        profileInteger(b, "profileAdjustTimeNourishOrder", "Nourish order", null, 0, 100,
                SpellProfile::getAdjustTimeNourishOrder, SpellProfile::setAdjustTimeNourishOrder);
        profileInteger(b, "profileAdjustTimeLifebloomOrder", "Lifebloom order", null, 0, 100,
                SpellProfile::getAdjustTimeLifebloomOrder, SpellProfile::setAdjustTimeLifebloomOrder);
        profileInteger(b, "profileAdjustTimeSwiftmendOrder", "Swiftmend order", null, 0, 100,
                SpellProfile::getAdjustTimeSwiftmendOrder, SpellProfile::setAdjustTimeSwiftmendOrder);
        profileInteger(b, "profileAdjustTimeWildGrowthOrder", "Wild Growth order", null, 0, 100,
                SpellProfile::getAdjustTimeWildGrowthOrder, SpellProfile::setAdjustTimeWildGrowthOrder);
        profileInteger(b, "profileAdjustTimeIdleOrder", "Idle order", null, 0, 100,
                SpellProfile::getAdjustTimeIdleOrder, SpellProfile::setAdjustTimeIdleOrder);
        profileInteger(b, "profileAdjustTimeManagedRejuvOrder", "Managed Rejuvenation order", null, 0, 100,
                SpellProfile::getAdjustTimeManagedRejuvOrder, SpellProfile::setAdjustTimeManagedRejuvOrder);
        profileInteger(b, "profileAdjustTimeManagedRegrowthOrder", "Managed Regrowth order", null, 0, 100,
                SpellProfile::getAdjustTimeManagedRegrowthOrder, SpellProfile::setAdjustTimeManagedRegrowthOrder);
        profileInteger(b, "profileAdjustTimeManagedLifebloomStackOrder", "Managed Lifebloom order", null, 0, 100,
                SpellProfile::getAdjustTimeManagedLifebloomStackOrder, SpellProfile::setAdjustTimeManagedLifebloomStackOrder);

        b.section("Mana adjustments", true);
        profileInteger(b, "profileReduceOOMRejuv", "Rejuvenation reduction", "percent", 0, 100,
                SpellProfile::getReduceOOMRejuv, SpellProfile::setReduceOOMRejuv);
        profileInteger(b, "profileReduceOOMRegrowth", "Regrowth reduction", "percent", 0, 100,
                SpellProfile::getReduceOOMRegrowth, SpellProfile::setReduceOOMRegrowth);
        profileInteger(b, "profileReduceOOMLifebloom", "Lifebloom reduction", "percent", 0, 100,
                SpellProfile::getReduceOOMLifebloom, SpellProfile::setReduceOOMLifebloom);
        profileInteger(b, "profileReduceOOMNourish", "Nourish reduction", "percent", 0, 100,
                SpellProfile::getReduceOOMNourish, SpellProfile::setReduceOOMNourish);
        profileInteger(b, "profileReduceOOMWildGrowth", "Wild Growth reduction", "percent", 0, 100,
                SpellProfile::getReduceOOMWildGrowth, SpellProfile::setReduceOOMWildGrowth);
        profileInteger(b, "profileReduceOOMRejuvOrder", "Rejuvenation reduction order", null, 0, 100,
                SpellProfile::getReduceOOMRejuvOrder, SpellProfile::setReduceOOMRejuvOrder);
        profileInteger(b, "profileReduceOOMRegrowthOrder", "Regrowth reduction order", null, 0, 100,
                SpellProfile::getReduceOOMRegrowthOrder, SpellProfile::setReduceOOMRegrowthOrder);
        profileInteger(b, "profileReduceOOMLifebloomOrder", "Lifebloom reduction order", null, 0, 100,
                SpellProfile::getReduceOOMLifebloomOrder, SpellProfile::setReduceOOMLifebloomOrder);
        profileInteger(b, "profileReduceOOMNourishOrder", "Nourish reduction order", null, 0, 100,
                SpellProfile::getReduceOOMNourishOrder, SpellProfile::setReduceOOMNourishOrder);
        profileInteger(b, "profileReduceOOMWildGrowthOrder", "Wild Growth reduction order", null, 0, 100,
                SpellProfile::getReduceOOMWildGrowthOrder, SpellProfile::setReduceOOMWildGrowthOrder);

        ModelSpec<CalculationOptionsTree> spec = b.build();
        return new ExtraSections(spec.sections(), null, null, null);
    }

    private static SpellProfile currentProfile(CalculationOptionsTree options) {
        if (options.getCurrent() == null) {
            options.setCurrent(new SpellProfile());
        }
        return options.getCurrent();
    }

    private static void profileInteger(ModelBuilder<CalculationOptionsTree> b, String key,
            String label, String hint, int min, int max, Function<SpellProfile, Integer> getter,
            BiConsumer<SpellProfile, Integer> setter) {
        b.integer(key, label, hint, min, max,
                o -> getter.apply(currentProfile(o)),
                (o, value) -> setter.accept(currentProfile(o), value));
    }

    private static String priorityAt(List<String> list, int slot) {
        if (list == null || slot >= list.size()) {
            return UNUSED;
        }
        String value = list.get(slot);
        return value == null || value.isBlank() ? UNUSED : value;
    }

    private static List<String> priorityAtSet(List<String> list, int slot, String raw) {
        List<String> updated = list == null ? new ArrayList<>() : new ArrayList<>(list);
        while (updated.size() <= slot) {
            updated.add("");
        }
        updated.set(slot, UNUSED.equals(raw) ? "" : raw);
        return updated;
    }

    private static List<String> trimPriority(List<String> list) {
        List<String> trimmed = new ArrayList<>();
        if (list != null) {
            for (String value : list) {
                if (value != null && !value.isBlank() && !UNUSED.equals(value)) {
                    trimmed.add(value);
                }
            }
        }
        return trimmed;
    }

    private static void validatePrioritySlots(Map<String, String> params, String prefix, int count,
            String label) {
        Set<String> seen = new HashSet<>();
        for (int i = 1; i <= count; i++) {
            String raw = params.get(prefix + i);
            if (raw == null || raw.isBlank() || UNUSED.equals(raw)) {
                continue;
            }
            if (!seen.add(raw)) {
                throw new IllegalArgumentException(
                        "Each " + label + " can only appear once in the priority.");
            }
        }
    }



    private static void prepareOptions(Object options) {
        if (options instanceof CalculationOptionsShadowPriest shadowPriest
                && shadowPriest.getSpellPriority() == null) {
            shadowPriest.setSpellPriority(new ArrayList<>(Spell.ShadowSpellList));
        } else if (options instanceof CalculationOptionsWarlock warlock) {
            activeWarlockRotation(warlock);
        } else if (options instanceof CalculationOptionsTree tree) {
            currentProfile(tree);
        } else if (options instanceof CalculationOptionsMage mage
                && mage.getCustomSpellMix() == null) {
            mage.setCustomSpellMix(new ArrayList<>());
        }
    }

    private static String mageMixSpell(CalculationOptionsMage options, int slot) {
        List<SpellWeight> mix = options.getCustomSpellMix();
        if (mix == null || slot >= mix.size() || mix.get(slot) == null
                || mix.get(slot).getSpell() == null) {
            return SpellId.None.name();
        }
        return mix.get(slot).getSpell().name();
    }

    private static void setMageMixSpell(CalculationOptionsMage options, int slot, String raw) {
        ensureMageMix(options, slot).get(slot).setSpell(SpellId.valueOf(raw));
    }

    private static Float mageMixWeight(CalculationOptionsMage options, int slot) {
        List<SpellWeight> mix = options.getCustomSpellMix();
        if (mix == null || slot >= mix.size() || mix.get(slot) == null) {
            return 0f;
        }
        return mix.get(slot).getWeight();
    }

    private static void setMageMixWeight(CalculationOptionsMage options, int slot, Float value) {
        ensureMageMix(options, slot).get(slot).setWeight(value == null ? 0f : value);
    }

    private static List<SpellWeight> ensureMageMix(CalculationOptionsMage options, int slot) {
        List<SpellWeight> mix = options.getCustomSpellMix();
        if (mix == null) {
            mix = new ArrayList<>();
            options.setCustomSpellMix(mix);
        }
        while (mix.size() <= slot) {
            SpellWeight weight = new SpellWeight();
            weight.setSpell(SpellId.None);
            weight.setWeight(0f);
            mix.add(weight);
        }
        if (mix.get(slot) == null) {
            SpellWeight weight = new SpellWeight();
            weight.setSpell(SpellId.None);
            weight.setWeight(0f);
            mix.set(slot, weight);
        }
        return mix;
    }

    private static void trimMageMix(CalculationOptionsMage options) {
        List<SpellWeight> mix = options.getCustomSpellMix();
        if (mix == null) {
            options.setCustomSpellMix(new ArrayList<>());
            return;
        }
        List<SpellWeight> trimmed = new ArrayList<>();
        for (SpellWeight weight : mix) {
            if (weight != null && weight.getSpell() != null
                    && weight.getSpell() != SpellId.None) {
                trimmed.add(weight);
            }
        }
        options.setCustomSpellMix(trimmed);
    }


    private static SpellProfile copyProfile(SpellProfile source) {
        SpellProfile copy = new SpellProfile();
        for (Method getter : SpellProfile.class.getDeclaredMethods()) {
            String property = getterProperty(getter);
            if (property == null || getter.getParameterCount() != 0) {
                continue;
            }
            Method setter = null;
            for (Method candidate : SpellProfile.class.getDeclaredMethods()) {
                if (property.equals(setterProperty(candidate))
                        && candidate.getParameterCount() == 1
                        && candidate.getParameterTypes()[0].equals(getter.getReturnType())) {
                    setter = candidate;
                    break;
                }
            }
            if (setter == null) {
                continue;
            }
            try {
                setter.invoke(copy, getter.invoke(source));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Could not copy the spell profile.", e);
            }
        }
        return copy;
    }

    private static String uniqueProfileName(List<SpellProfile> profiles) {
        String base = "New Profile";
        String name = base;
        int suffix = 2;
        while (containsProfileName(profiles, name)) {
            name = base + " " + suffix++;
        }
        return name;
    }

    private static boolean containsProfileName(List<SpellProfile> profiles, String name) {
        for (SpellProfile profile : profiles) {
            if (Objects.equals(profile.getName(), name)) {
                return true;
            }
        }
        return false;
    }

    private static int indexOfIdentity(List<SpellProfile> profiles, SpellProfile profile) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i) == profile) {
                return i;
            }
        }
        return -1;
    }

    private static SectionSpec statWeightsSection(String modelName) {
        Object defaults = CalculationService.createDefaultOptions(modelName);
        List<FieldSpec> fields = new ArrayList<>();
        if ("DPSWarr".equals(modelName)) {
            fields.add(booleanArrayField("statsStrength", "Weight: Strength", 0,
                    o -> ((CalculationOptionsDPSWarr) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsAgility", "Weight: Agility", 1,
                    o -> ((CalculationOptionsDPSWarr) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsAttackPower", "Weight: Attack power", 2,
                    o -> ((CalculationOptionsDPSWarr) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsCrit", "Weight: Crit rating", 3,
                    o -> ((CalculationOptionsDPSWarr) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsHit", "Weight: Hit rating", 4,
                    o -> ((CalculationOptionsDPSWarr) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsExpertise", "Weight: Expertise rating", 5,
                    o -> ((CalculationOptionsDPSWarr) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsHaste", "Weight: Haste rating", 6,
                    o -> ((CalculationOptionsDPSWarr) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsArmorPenetration", "Weight: Armor penetration", 7,
                    o -> ((CalculationOptionsDPSWarr) o).getStatsList(), defaults));
        } else {
            fields.add(booleanArrayField("statsStrength", "Weight: Strength", 0,
                    o -> ((CalculationOptionsEnhance) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsAgility", "Weight: Agility", 1,
                    o -> ((CalculationOptionsEnhance) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsAttackPower", "Weight: Attack power", 2,
                    o -> ((CalculationOptionsEnhance) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsCrit", "Weight: Crit rating", 3,
                    o -> ((CalculationOptionsEnhance) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsHit", "Weight: Hit rating", 4,
                    o -> ((CalculationOptionsEnhance) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsExpertise", "Weight: Expertise rating", 5,
                    o -> ((CalculationOptionsEnhance) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsHaste", "Weight: Haste rating", 6,
                    o -> ((CalculationOptionsEnhance) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsArmorPenetration", "Weight: Armor penetration", 7,
                    o -> ((CalculationOptionsEnhance) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsSpellPower", "Weight: Spell power", 8,
                    o -> ((CalculationOptionsEnhance) o).getStatsList(), defaults));
            fields.add(booleanArrayField("statsIntellect", "Weight: Intellect", 9,
                    o -> ((CalculationOptionsEnhance) o).getStatsList(), defaults));
        }
        return new SectionSpec("Stat graph weights", false, List.copyOf(fields));
    }

    private static FieldSpec booleanArrayField(String key, String label, int index,
            Function<Object, boolean[]> getter, Object defaults) {
        return new FieldSpec(key, label, FieldType.CHECKBOX, null, List.of(), null, null,
                o -> {
                    boolean[] values = getter.apply(o);
                    return Boolean.toString(values != null && index < values.length && values[index]);
                },
                (o, raw) -> {
                    boolean[] values = getter.apply(o);
                    if (values != null && index < values.length) {
                        values[index] = parseBoolean(raw);
                    }
                },
                Boolean.toString(getter.apply(defaults)[index]));
    }

    // ------------------------------------------------------------------ reflective fallback

    private static final List<String> SECTION_TITLES =
            List.of("Fight", "Movement & Latency", "Consumables & Cooldowns", "Advanced");

    /**
     * Discovers a model's options object reflectively. The defaults instance comes from
     * {@link CalculationService#createDefaultOptions(String)}, the same object the editor
     * calculation uses, so models with non-trivial defaults (Hunter priority, Warlock rotations)
     * still reset to a runnable state.
     */
    private static ModelSpec<ICalculationOptionBase> buildFallbackSpec(String modelName) {
        ICalculationOptionBase defaults = CalculationService.createDefaultOptions(modelName);
        Supplier<ICalculationOptionBase> supplier =
                () -> CalculationService.createDefaultOptions(modelName);
        return new ModelSpec<>(modelName, optionsClass(defaults.getClass()), supplier, defaults,
                discoverSections(defaults.getClass(), defaults));
    }

    @SuppressWarnings("unchecked")
    private static Class<ICalculationOptionBase> optionsClass(Class<?> type) {
        return (Class<ICalculationOptionBase>) type;
    }

    private static List<SectionSpec> discoverSections(Class<?> type, ICalculationOptionBase defaults) {
        Map<String, Method> setters = new HashMap<>();
        Map<String, FieldSpec> discovered = new TreeMap<>();
        Set<String> lowercaseKeys = new HashSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            String property = setterProperty(method);
            if (property != null) {
                setters.put(property, method);
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || !Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            String property = getterProperty(method);
            if (property == null || !isEligibleType(method.getReturnType())
                    || isExcludedName(property)) {
                continue;
            }
            Method setter = setters.get(property);
            if (setter == null
                    || !setter.getParameterTypes()[0].equals(method.getReturnType())) {
                continue;
            }
            String key = decapitalize(property);
            if (!lowercaseKeys.add(key.toLowerCase(Locale.ROOT))) {
                continue;
            }
            discovered.putIfAbsent(key, methodBackedField(key, method, setter, defaults));
        }
        for (Field field : type.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())
                    || field.isSynthetic() || !isEligibleType(field.getType())) {
                continue;
            }
            String key = field.getName();
            if (isExcludedName(key) || !lowercaseKeys.add(key.toLowerCase(Locale.ROOT))) {
                continue;
            }
            discovered.putIfAbsent(key, fieldBackedField(key, field, defaults));
        }
        Map<String, List<FieldSpec>> grouped = new LinkedHashMap<>();
        for (String title : SECTION_TITLES) {
            grouped.put(title, new ArrayList<>());
        }
        for (FieldSpec field : discovered.values()) {
            grouped.get(sectionFor(field.key(), field.type())).add(field);
        }
        List<SectionSpec> sections = new ArrayList<>(SECTION_TITLES.size());
        for (Map.Entry<String, List<FieldSpec>> entry : grouped.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                sections.add(new SectionSpec(entry.getKey(), "Advanced".equals(entry.getKey()),
                        List.copyOf(entry.getValue())));
            }
        }
        return List.copyOf(sections);
    }

    private static FieldSpec methodBackedField(
            String key, Method getter, Method setter, ICalculationOptionBase defaults) {
        Class<?> valueType = getter.getReturnType();
        FieldType type = fieldType(valueType);
        Object defaultValue = readGetter(getter, defaults);
        Double[] bounds = numericBounds(key, type, defaultValue);
        return new FieldSpec(key, labelFromKey(key), type, hintFor(bounds), enumChoices(valueType),
                bounds[0], bounds[1],
                options -> formatOptionValue(readGetter(getter, options), valueType),
                (options, raw) -> writeSetter(setter, options, parseOptionValue(valueType, raw)),
                formatOptionValue(defaultValue, valueType));
    }

    private static FieldSpec fieldBackedField(String key, Field field, ICalculationOptionBase defaults) {
        Class<?> valueType = field.getType();
        FieldType type = fieldType(valueType);
        Object defaultValue = readField(field, defaults);
        Double[] bounds = numericBounds(key, type, defaultValue);
        return new FieldSpec(key, labelFromKey(key), type, hintFor(bounds), enumChoices(valueType),
                bounds[0], bounds[1],
                options -> formatOptionValue(readField(field, options), valueType),
                (options, raw) -> writeField(field, options, parseOptionValue(valueType, raw)),
                formatOptionValue(defaultValue, valueType));
    }

    private static String getterProperty(Method method) {
        if (method.getParameterCount() != 0 || method.getReturnType() == void.class) {
            return null;
        }
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return name.substring(3);
        }
        if (name.startsWith("is") && name.length() > 2
                && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
            return name.substring(2);
        }
        return null;
    }

    private static String setterProperty(Method method) {
        if (method.getParameterCount() != 1) {
            return null;
        }
        String name = method.getName();
        if (name.startsWith("set") && name.length() > 3) {
            return name.substring(3);
        }
        return null;
    }

    private static boolean isEligibleType(Class<?> type) {
        if (type == boolean.class || type == byte.class || type == short.class || type == int.class
                || type == long.class || type == float.class || type == double.class) {
            return true;
        }
        if (type == Boolean.class || type == Byte.class || type == Short.class || type == Integer.class
                || type == Long.class || type == Float.class || type == Double.class
                || type == String.class) {
            return true;
        }
        return type.isEnum();
    }

    private static boolean isExcludedName(String property) {
        String name = property.toLowerCase(Locale.ROOT);
        return name.contains("character")
                || name.contains("calculation")
                || name.contains("sequence")
                || name.contains("cooldownrestriction")
                || name.contains("xml")
                || name.contains("cache");
    }

    private static FieldType fieldType(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            return FieldType.CHECKBOX;
        }
        if (type == float.class || type == Float.class || type == double.class || type == Double.class) {
            return FieldType.NUMBER;
        }
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class
                || type == short.class || type == Short.class || type == byte.class || type == Byte.class) {
            return FieldType.INTEGER;
        }
        if (type == String.class) {
            return FieldType.TEXT;
        }
        if (type.isEnum()) {
            return FieldType.SELECT;
        }
        throw new IllegalArgumentException("Unsupported option type: " + type);
    }

    private static List<String> enumChoices(Class<?> type) {
        if (!type.isEnum()) {
            return List.of();
        }
        Object[] constants = type.getEnumConstants();
        List<String> names = new ArrayList<>(constants.length);
        for (Object constant : constants) {
            names.add(((Enum<?>) constant).name());
        }
        return List.copyOf(names);
    }

    private static Object readGetter(Method getter, Object target) {
        try {
            return getter.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    private static Object readField(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static void writeSetter(Method setter, Object target, Object value) {
        try {
            setter.invoke(target, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Could not apply " + setter.getName() + ".", e);
        }
    }

    private static void writeField(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not apply " + field.getName() + ".", e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object parseOptionValue(Class<?> type, String raw) {
        if (type == boolean.class || type == Boolean.class) {
            return parseBoolean(raw);
        }
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(raw);
        }
        if (type == long.class || type == Long.class) {
            return Long.parseLong(raw);
        }
        if (type == short.class || type == Short.class) {
            return Short.parseShort(raw);
        }
        if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(raw);
        }
        if (type == float.class || type == Float.class) {
            return Float.parseFloat(raw);
        }
        if (type == double.class || type == Double.class) {
            return Double.parseDouble(raw);
        }
        if (type == String.class) {
            return raw;
        }
        if (type.isEnum()) {
            return Enum.valueOf((Class) type, raw);
        }
        throw new IllegalArgumentException("Unsupported option type: " + type);
    }

    private static String formatOptionValue(Object value, Class<?> type) {
        if (value == null) {
            return "";
        }
        if (value instanceof Boolean bool) {
            return Boolean.toString(bool);
        }
        if (value instanceof Float number) {
            return formatFloat(number);
        }
        if (value instanceof Double number) {
            return formatDouble(number);
        }
        if (value instanceof Enum<?> constant) {
            return constant.name();
        }
        return value.toString();
    }

    private static String formatDouble(Double value) {
        if (value == null) {
            return "";
        }
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return Long.toString(value.longValue());
        }
        return Double.toString(value);
    }

    /**
     * Best-effort numeric bounds for the obvious cases only. A bound that would reject the
     * model's own default is dropped, so validation can never block the defaults.
     */
    private static Double[] numericBounds(String key, FieldType type, Object defaultValue) {
        if (type != FieldType.NUMBER && type != FieldType.INTEGER) {
            return new Double[] { null, null };
        }
        double value = defaultValue instanceof Number number ? number.doubleValue() : 0d;
        String name = key.toLowerCase(Locale.ROOT);
        Double min = null;
        Double max = null;
        if (name.contains("level")) {
            min = 1d;
            max = 83d;
        } else if (name.endsWith("targets") || name.equals("numberoftargets")
                || name.endsWith("targetcount")) {
            min = 0d;
            max = 100d;
        } else if (name.contains("duration") || name.contains("length")) {
            min = 0d;
            max = 36000d;
        } else if (name.contains("percent") || name.contains("perc") || name.contains("pct")) {
            min = 0d;
            max = 100d;
        } else if (name.contains("fraction")) {
            min = 0d;
            max = 1d;
        } else if (name.contains("ratio")) {
            min = 0d;
            max = value > 1d ? 100d : 1d;
        } else if (name.contains("uptime")) {
            min = 0d;
            max = value > 1d ? 100d : 1d;
        }
        if (min != null && value < min) {
            return new Double[] { null, null };
        }
        if (max != null && value > max) {
            return new Double[] { null, null };
        }
        return new Double[] { min, max };
    }

    private static String sectionFor(String key, FieldType type) {
        String name = key.toLowerCase(Locale.ROOT);
        if (name.startsWith("movement") || name.startsWith("move") || name.startsWith("latency")
                || name.startsWith("lag") || name.startsWith("react") || name.startsWith("delay")
                || name.startsWith("wait")) {
            return "Movement & Latency";
        }
        if (name.contains("fightduration") || name.contains("fightlength")
                || name.contains("duration") || name.contains("length")
                || name.equals("targetlevel") || name.equals("playerlevel")
                || name.endsWith("targets") || name.equals("numberoftargets")
                || name.endsWith("targetcount")) {
            return "Fight";
        }
        boolean consumable = name.contains("potion") || name.contains("gem") || name.contains("heroism")
                || name.contains("drums") || name.contains("flamecap") || name.contains("innervate");
        boolean availability = type == FieldType.CHECKBOX
                && (name.endsWith("available") || name.endsWith("enabled"));
        if (consumable || availability) {
            return "Consumables & Cooldowns";
        }
        return "Advanced";
    }

    private static String hintFor(Double[] bounds) {
        Double min = bounds[0];
        Double max = bounds[1];
        if (min == null && max == null) {
            return null;
        }
        if (min != null && max != null) {
            return "range " + formatBound(min) + "-" + formatBound(max);
        }
        return (min != null ? "min " + formatBound(min) : "max " + formatBound(max));
    }

    private static String labelFromKey(String key) {
        String spaced = key
                .replace('_', ' ')
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .trim();
        if (spaced.isEmpty()) {
            return key;
        }
        return java.lang.Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    /** JavaBeans-style decapitalization, but only when the first two characters are not acronyms. */
    private static String decapitalize(String name) {
        if (name.isEmpty()) {
            return name;
        }
        if (name.length() > 1 && java.lang.Character.isUpperCase(name.charAt(0))
                && java.lang.Character.isUpperCase(name.charAt(1))) {
            return name;
        }
        return java.lang.Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    // ------------------------------------------------------------------ helpers

    private static List<String> choices(int from, int to) {
        List<String> values = new ArrayList<>(to - from + 1);
        for (int value = from; value <= to; value++) {
            values.add(Integer.toString(value));
        }
        return List.copyOf(values);
    }

    private static String formatFloat(Float value) {
        if (value == null) {
            return "";
        }
        if (value == value.longValue()) {
            return Long.toString(value.longValue());
        }
        return Float.toString(value);
    }

    private static String formatBound(Double value) {
        return value == null ? "" : formatFloat(value.floatValue());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class ModelBuilder<T extends ICalculationOptionBase> {
        private final String name;
        private final Class<T> type;
        private final Supplier<T> supplier;
        private final T defaults;
        private final List<SectionSpec> sections = new ArrayList<>();
        private BiConsumer<T, Map<String, String>> validate;
        private Consumer<T> afterWrite;
        private List<FieldSpec> current;

        ModelBuilder(String name, Class<T> type, Supplier<T> supplier) {
            this.name = name;
            this.type = type;
            this.supplier = supplier;
            this.defaults = supplier.get();
        }

        ModelBuilder<T> section(String title, boolean advanced) {
            current = new ArrayList<>();
            sections.add(new SectionSpec(title, advanced, current));
            return this;
        }

        ModelBuilder<T> number(String key, String label, String hint, double min, double max,
                Function<T, Float> getter, BiConsumer<T, Float> setter) {
            current.add(new FieldSpec(key, label, FieldType.NUMBER, hint, List.of(), min, max,
                    o -> formatFloat(getter.apply(type.cast(o))),
                    (o, raw) -> setter.accept(type.cast(o), Float.parseFloat(raw)),
                    formatFloat(getter.apply(defaults))));
            return this;
        }

        ModelBuilder<T> integer(String key, String label, String hint, int min, int max,
                Function<T, Integer> getter, BiConsumer<T, Integer> setter) {
            current.add(new FieldSpec(key, label, FieldType.INTEGER, hint, List.of(),
                    (double) min, (double) max,
                    o -> Integer.toString(getter.apply(type.cast(o))),
                    (o, raw) -> setter.accept(type.cast(o), Integer.parseInt(raw)),
                    Integer.toString(getter.apply(defaults))));
            return this;
        }

        ModelBuilder<T> checkbox(String key, String label, String hint,
                Function<T, Boolean> getter, BiConsumer<T, Boolean> setter) {
            current.add(new FieldSpec(key, label, FieldType.CHECKBOX, hint, List.of(), null, null,
                    o -> Boolean.toString(getter.apply(type.cast(o))),
                    (o, raw) -> setter.accept(type.cast(o), parseBoolean(raw)),
                    Boolean.toString(getter.apply(defaults))));
            return this;
        }

        ModelBuilder<T> text(String key, String label, String hint,
                Function<T, String> getter, BiConsumer<T, String> setter) {
            current.add(new FieldSpec(key, label, FieldType.TEXT, hint, List.of(), null, null,
                    o -> nullToEmpty(getter.apply(type.cast(o))),
                    (o, raw) -> setter.accept(type.cast(o), raw),
                    nullToEmpty(getter.apply(defaults))));
            return this;
        }

        ModelBuilder<T> select(String key, String label, String hint, List<String> choices,
                Function<T, String> getter, BiConsumer<T, String> setter) {
            current.add(new FieldSpec(key, label, FieldType.SELECT, hint, List.copyOf(choices), null, null,
                    o -> nullToEmpty(getter.apply(type.cast(o))),
                    (o, raw) -> setter.accept(type.cast(o), raw),
                    nullToEmpty(getter.apply(defaults))));
            return this;
        }

        ModelBuilder<T> validate(BiConsumer<T, Map<String, String>> validator) {
            this.validate = validator;
            return this;
        }

        ModelBuilder<T> afterWrite(Consumer<T> normalizer) {
            this.afterWrite = normalizer;
            return this;
        }

        ModelSpec<T> build() {
            BiConsumer<Object, Map<String, String>> validator = validate == null ? null
                    : (o, params) -> validate.accept(type.cast(o), params);
            Consumer<Object> normalizer = afterWrite == null ? null
                    : o -> afterWrite.accept(type.cast(o));
            return new ModelSpec<>(name, type, supplier, defaults, List.copyOf(sections),
                    validator, normalizer);
        }
    }
}
