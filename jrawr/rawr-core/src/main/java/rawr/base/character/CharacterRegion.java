// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Enums.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.character;

/**
 * The five Battle.net regions (US, EU, KR, TW, CN) that the original character import knew about.
 *
 * <p>Stored on the {@link Character} and written to the saved file; the ported engine does not
 * otherwise interpret it. The enum declares no numeric values because the C# enum relied on
 * declaration order.</p>
 */
public enum CharacterRegion {
    US, EU, KR, TW, CN
}
