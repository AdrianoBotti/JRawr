// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Character.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.calculations;

import rawr.base.character.Character;

/**
 * Contract for per-model calculation options: the settings a user chose for one model (fight
 * length, rotation switches, thresholds, ...) that must survive a save/load round trip.
 *
 * <p>{@link #getXml()} serializes the options to the XML string stored in the character file's
 * {@code CalculationOptions} dictionary under the model's name. The matching deserialization is
 * {@code CalculationsBase.deserializeDataObject}; old character files are loaded by calling that
 * method, so implementations must accept the schema they wrote.</p>
 */
public interface ICalculationOptionBase {
    String getXml();
}
