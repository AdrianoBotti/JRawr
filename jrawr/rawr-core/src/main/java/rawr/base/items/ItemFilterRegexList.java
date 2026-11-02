// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ItemFilter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: BindingList (non-Silverlight C#) is replaced by ArrayList; its change events have no
// consumers in the port.
package rawr.base.items;

import java.util.ArrayList;

/**
 * The ordered child list of an {@link ItemFilterRegex} node (and of the root filter tree).
 *
 * <p>A named {@code ArrayList} subclass replacing the C# {@code BindingList}, whose change
 * events had no consumers in the port; the XML schema serializes it as the nested
 * {@code RegexList}/{@code ItemFilterRegex} elements. Element order is the evaluation order;
 * {@link ItemFilterRegex} iterates it directly.</p>
 */
public class ItemFilterRegexList extends ArrayList<ItemFilterRegex> {
}
