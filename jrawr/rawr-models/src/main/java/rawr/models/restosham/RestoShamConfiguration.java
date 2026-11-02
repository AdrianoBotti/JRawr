// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.RestoSham/RestoSham.Base.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: SubPointNameColors is a System.Drawing.Color map (UI-only) and is omitted.
package rawr.models.restosham;

/**
 * Contains various static lists for the RestoSham module
 */
public final class RestoShamConfiguration {
    private RestoShamConfiguration() {
    }

    /**
     * The Character Calculations Display configuration
     */
    public static final String[] CharacterDisplayCalculationLabels = new String[] {
            "Totals:HPS - Burst",
            "Totals:HPS - Sustained",
            "Totals:Survival",
            "Basic Stats:Health",
            "Basic Stats:Mana",
            "Basic Stats:Stamina",
            "Basic Stats:Intellect",
            "Basic Stats:Spell Power",
            "Basic Stats:MP5*Mana regeneration while casting",
            "Basic Stats:Mana Available*This is your total mana from all sources throughout the entire fight.",
            "Basic Stats:Heal Spell Crit*This includes all static talents including those that are not shown on the in-game character pane",
            "Basic Stats:Spell Haste",
            "Healing Style Breakdowns:Burst Sequence",
            "Healing Style Breakdowns:Sustained Sequence",
            "Healing Style Breakdowns:Mana Available per Second",
            "Healing Style Breakdowns:Mana Used per Second*This is the mana used per second by your chosen sustained sequence",
            "Healing Style Breakdowns:Healing Stream HPS",
            "Healing Style Breakdowns:Earth Shield HPS",
            "Healing Style Breakdowns:RT+HW HPS",
            "Healing Style Breakdowns:RT+LHW HPS",
            "Healing Style Breakdowns:RT+CH HPS",
            "Healing Style Breakdowns:HW Spam HPS",
            "Healing Style Breakdowns:LHW Spam HPS",
            "Healing Style Breakdowns:CH Spam HPS",
            "Average Cast Times:Global Cooldown",
            "Average Cast Times:Healing Wave*Normal / Tidal Waves",
            "Average Cast Times:Lesser Healing Wave",
            "Average Cast Times:Chain Heal",
            "Average Cast Times:Lightning Bolt*Aren't you busy healing people? ;)",
    };

    public static final String[] OptimizableCalculationLabels = new String[] {
            "Haste %",
            "Crit %",
            "Mana Usable per Second",
    };
}
