// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Enums.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.items;

import rawr.base.optimizer.AvailableItemGenerator;

/**
 * Optimizer bookkeeping: what the optimizer may do with one item in one run, from "cannot be
 * used at all" to "use it and re-gem it freely".
 *
 * <p>Values, weakest to strongest: {@code NotAvailable}; {@code Available} (usable as it is);
 * {@code AvailableWithEnchantRestrictions} (usable, but the enchant must stay as saved);
 * {@code RegemmingAllowed} (gems may be changed); {@code RegemmingAllowedWithEnchantRestrictions}
 * (both). The value is carried by {@link Item#getAvailabilityInformation()} while the optimizer's
 * {@code AvailableItemGenerator} builds candidates; it is not persisted and not used by the
 * calculators.</p>
 */
public enum ItemAvailability {
    NotAvailable,
    Available,
    AvailableWithEnchantRestrictions,
    RegemmingAllowed,
    RegemmingAllowedWithEnchantRestrictions
}
