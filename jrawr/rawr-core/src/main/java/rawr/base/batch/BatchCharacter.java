// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/BatchCharacter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The C# PropertyChanged (INotifyPropertyChanged) event is ported as a small listener interface
// (AGENTS §8); the Silverlight-only OriginalCharacter member and the direct
// `Character.Load(absolutePath)` call are omitted. The character file
// loader lives in rawr-data (CharacterFixtureLoader), which depends on rawr-core and therefore cannot
// be called from here; instead a process-wide loader hook may be installed with setCharacterLoader,
// or the character can be supplied with setCharacter. When neither is used the lazy getter returns
// null (the C# code would load the file).
package rawr.base.batch;

import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.character.Character;
import rawr.base.optimizer.ItemInstanceOptimizer;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * One character entry of a batch (roster) run: a saved-character file reference plus the
 * per-character state the batch tools need.
 *
 * <p>A batch is a group of characters that are optimized together or ranked against the same
 * candidate items. Each entry keeps the character file path, the lazily loaded {@link Character},
 * a weight that scales the character's contribution when the roster combines scores, and a
 * locked flag that lets the progressive optimizer skip the entry. This is the unit the batch UI
 * lists; {@link BatchCharacterList} holds the entries and
 * {@link BatchTools}/{@link BatchOptimizer} consume them.</p>
 *
 * <h2>Scores and dirty tracking</h2>
 *
 * <p>{@link #getScore()} is the model-defined optimization value of the character as last loaded
 * (or as of the last cleared dirty flag), not a percentage; it starts at zero until a relative
 * path is assigned or a pending {@code NewScore} is promoted. When the character is modified (its
 * {@link Character#onCalculationsInvalidated()} fires, e.g. after a gear change) the entry marks
 * itself dirty and recomputes {@link #getNewScore()}; clearing the dirty flag through
 * {@link #setUnsavedChanges(boolean)} promotes the new score to the saved one.</p>
 *
 * <h2>Loading</h2>
 *
 * <p>The path is stored relative and resolved to an absolute path when it is set, against the
 * process working directory (the C# original resolved against its installation directory). The
 * C# original called {@code Character.Load(absolutePath)} directly; because the file loader lives
 * in the {@code rawr-data} module, which depends on this module, the port offers a process-wide
 * hook ({@link #setCharacterLoader(CharacterLoader)}) that {@code rawr-data} installs, plus
 * {@link #setCharacter(Character)} for an already loaded character. Every character obtained is
 * subscribed to calculation invalidation so the score cache stays honest; load failures surface
 * as a {@link RuntimeException}. With no loader installed the lazy getter returns {@code null}.</p>
 *
 * <h2>Change notifications</h2>
 *
 * <p>The C# {@code INotifyPropertyChanged} event became {@link PropertyChangedListener}, fired on
 * the thread performing the change (there is no dispatcher here). The property names are the C#
 * ones: {@code "Score"}, {@code "Character"}, {@code "Name"} and {@code "NewScore"}. The weight
 * and locked flag do not notify, exactly as in the original. Listeners are stored in a
 * {@link java.util.concurrent.CopyOnWriteArrayList}, so registering or removing a listener during
 * a notification is safe.</p>
 */
public class BatchCharacter {
    /** Replaces `Character.Load(string)`; rawr-data installs this from outside rawr-core. */
    @FunctionalInterface
    public interface CharacterLoader {
        Character load(String absolutePath) throws Exception;
    }

    /** Replaces the C# `PropertyChanged` event (INotifyPropertyChanged). */
    @FunctionalInterface
    public interface PropertyChangedListener {
        void propertyChanged(BatchCharacter sender, String propertyName);
    }

    private final List<PropertyChangedListener> propertyChangedListeners = new CopyOnWriteArrayList<PropertyChangedListener>();

    public void addPropertyChangedListener(PropertyChangedListener listener) {
        propertyChangedListeners.add(listener);
    }

    public void removePropertyChangedListener(PropertyChangedListener listener) {
        propertyChangedListeners.remove(listener);
    }

    /** Raises PropertyChanged on the calling thread (no-op when there are no listeners). */
    protected void onPropertyChanged(String propertyName) {
        for (PropertyChangedListener listener : propertyChangedListeners) {
            listener.propertyChanged(this, propertyName);
        }
    }

    private static CharacterLoader characterLoader;

    /**
     * Installs the process-wide character-file loader used by {@link #getCharacter()} (the Java
     * replacement for the C# {@code Character.Load} call). Normally called once at start-up by
     * {@code rawr-data}; passing {@code null} disables lazy loading.
     */
    public static void setCharacterLoader(CharacterLoader loader) {
        characterLoader = loader;
    }

    private String relativePath;
    private String absolutePath = "";

    public String getRelativePath() {
        return relativePath;
    }

    /**
     * Points the entry at another character file: drops the cached character, resolves the new
     * absolute path against the process working directory (the C# original resolved against its
     * installation directory), loads the character and computes the initial {@link #getScore()},
     * then fires the {@code Score}, {@code Character} and {@code Name} notifications in that
     * order. A load failure aborts with a {@link RuntimeException} after the path fields have
     * been updated.
     */
    public void setRelativePath(String value) {
        if (!Objects.equals(relativePath, value)) {
            relativePath = value;
            character = null;

            // C# resolved against AppDomain.CurrentDomain.BaseDirectory; the headless port resolves
            // against the process working directory.
            absolutePath = Path.of(relativePath).toAbsolutePath().normalize().toString();

            Character c = getCharacter();
            if (c != null) {
                score = ItemInstanceOptimizer.getOptimizationValue(c, getModel(), true);
            }
            onPropertyChanged("Score");
            onPropertyChanged("Character");
            onPropertyChanged("Name");
        }
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    /**
     * The file name without extension, with a trailing {@code " *"} while the entry has unsaved
     * changes; {@code null} when no relative path has been assigned.
     */
    public String getName() {
        if (relativePath == null) return null;
        Path fileName = Path.of(relativePath).getFileName();
        String name = fileName == null ? null : fileName.toString();
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0) name = name.substring(0, dot);
            if (unsavedChanges) name += " *";
        }
        return name;
    }

    private Character character;

    /**
     * Returns the character, loading it through the installed {@link CharacterLoader} on first
     * use and subscribing it to calculation invalidation. Returns {@code null} when there is no
     * loader (or the path is {@code null}); a failing load is wrapped in a
     * {@link RuntimeException}. A character supplied with {@link #setCharacter(Character)} is
     * returned as is.
     */
    public Character getCharacter() {
        if (character == null && absolutePath != null && characterLoader != null) {
            try {
                character = characterLoader.load(absolutePath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load character " + absolutePath, e);
            }
            character.addCalculationsInvalidatedListener(this::characterCalculationsInvalidated);
        }
        return character;
    }

    /**
     * Supplies an already loaded character instead of loading the file (the C# property setter
     * existed only in the Silverlight build; this port exposes it unconditionally), subscribing
     * it to calculation invalidation and firing the {@code Score} and {@code Character}
     * notifications. Unlike {@link #setRelativePath(String)} it does not compute the score, so the
     * reported score stays at its previous value until a later dirty-flag transition promotes a
     * recomputed one.
     */
    public void setCharacter(Character value) {
        character = value;
        if (character != null) {
            character.addCalculationsInvalidatedListener(this::characterCalculationsInvalidated);
        }
        onPropertyChanged("Score");
        onPropertyChanged("Character");
    }

    private void characterCalculationsInvalidated() {
        unsavedChanges = true;
        newScore = ItemInstanceOptimizer.getOptimizationValue(getCharacter(), getModel(), true);
        onPropertyChanged("Name");
        onPropertyChanged("NewScore");
    }

    private CalculationsBase model;

    /**
     * Returns the simulation model registered for the character's current model name, caching it
     * and re-resolving whenever the character switches models. Requires a loaded character.
     */
    public CalculationsBase getModel() {
        if (model == null || !Objects.equals(model.getName(), getCharacter().getCurrentModel())) {
            model = Calculations.getModel(getCharacter().getCurrentModel());
        }
        return model;
    }

    private boolean unsavedChanges;

    public boolean isUnsavedChanges() {
        return unsavedChanges;
    }

    /**
     * Marks the entry as changed or clears the mark. When clearing while a recomputed
     * {@code NewScore} is pending, that value is promoted to the saved {@link #getScore()} and
     * reset to {@code null}; any transition fires the {@code Name}, {@code Score} and
     * {@code NewScore} notifications. Setting the same value is a no-op.
     */
    public void setUnsavedChanges(boolean value) {
        if (unsavedChanges != value) {
            if (!value) {
                if (newScore != null) {
                    score = newScore;
                    newScore = null;
                }
            }
            unsavedChanges = value;
            onPropertyChanged("Name");
            onPropertyChanged("Score");
            onPropertyChanged("NewScore");
        }
    }

    private float weight = 1f;

    public float getWeight() {
        return weight;
    }

    public void setWeight(float value) {
        if (weight != value) {
            weight = value;
        }
    }

    private boolean locked;

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    private float score;

    public float getScore() {
        return score;
    }

    private Float newScore;

    public Float getNewScore() {
        return newScore;
    }
}
