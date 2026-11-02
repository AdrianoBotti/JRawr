// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/StateDescriptionParseTree.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage.statedescription;

@FunctionalInterface
public interface StateDescriptionDelegate {
    boolean test(int effects);
}
