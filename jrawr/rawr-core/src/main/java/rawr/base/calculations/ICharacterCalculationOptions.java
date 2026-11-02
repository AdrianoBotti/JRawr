// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ICharacterCalculationOptions.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.calculations;

import rawr.base.character.Character;

/**
 * Contract for a model's calculation-options object that needs a back-reference to the
 * {@link Character} it belongs to.
 *
 * <p>Options are stored as XML strings inside the character file and rebuilt on demand by
 * {@link Character#getCalculationOptions()}, which calls {@link #setCharacter(Character)} after
 * deserialization so the options can read the character's talents, gear and buffs during a
 * calculation. The reference is deliberately not serialized with the options.</p>
 */
public interface ICharacterCalculationOptions {
    Character getCharacter();

    void setCharacter(Character character);
}
