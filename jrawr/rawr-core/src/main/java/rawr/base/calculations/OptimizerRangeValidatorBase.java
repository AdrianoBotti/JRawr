// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/OptimizerBase.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.calculations;

import rawr.base.optimizer.OptimizerBase;

/**
 * Abstract class that is used by the optimizer to validate a certain range of slots in an
 * individual. When optimizer is constructing an individual it will call the validator after
 * it constructs the individual up to endSlot, if validation fails optimizer restarts construction
 * at startSlot.
 */
public abstract class OptimizerRangeValidatorBase<TItem> {
    /** First slot of validation range. */
    public int startSlot;

    /** Last slot of validation range. */
    public int endSlot;

    /**
     * Checks whether the range of items is valid.
     * Validation should only depend on items between startSlot and endSlot. Items after endSlot
     * may not be initialized.
     */
    public abstract boolean isValid(TItem[] items);
}
