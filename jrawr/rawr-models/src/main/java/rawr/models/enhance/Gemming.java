// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Enhance/Gemming.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.enhance;

import rawr.base.items.GemmingTemplate;

import java.util.ArrayList;
import java.util.List;

class Gemming {
    ////Relevant Gem IDs for Enhancement Shamans
    //Red
    private final int[] delicate = { 39905, 39997, 40112, 42143 };  // Agility
    private final int[] bright = { 39906, 39999, 40114, 36766 };    // Attack Power
    private final int[] precise = { 39910, 40003, 40118, 42154 };   // Expertise
    private final int[] fractured = { 39909, 40002, 40117, 42153 }; // Armour Penetration

    //Yellow
    private final int[] rigid = { 39915, 40014, 40125, 42156 };  // Hit
    private final int[] smooth = { 39914, 40013, 40124, 42149 }; // Crit
    private final int[] quick = { 39918, 40017, 40128, 42150 };  // Haste

    //Orange
    private final int[] deadly = { 39952, 40043, 40147, 40147 };   // Agi/Crit
    private final int[] glinting = { 39953, 40044, 40148, 40148 }; // Agi/Hit
    private final int[] deft = { 39955, 40046, 40150, 40150 };     // Agi/Haste
    private final int[] wicked = { 39960, 40052, 40156, 40156 };   // AP/Crit
    private final int[] pristine = { 39961, 40053, 40157, 40157 }; // AP/Hit
    private final int[] stark = { 39963, 40055, 40159, 40159 };    // AP/Haste
    private final int[] accurate = { 39966, 40058, 40162, 40162 }; // Exp/Hit

    //Purple
    private final int[] guardian = { 39940, 40034, 40141, 40141 }; // Exp/Sta
    private final int[] balanced = { 39937, 40029, 40136, 40136 }; // Ap/Sta
    private final int[] shifting = { 39935, 40023, 40130, 40130 }; // Agi/Sta
    private final int[] puissant = { 39933, 40033, 40140, 40140 }; // ArP/Sta

    //Green
    private final int[] vivid = { 39975, 40088, 40166, 40166 };    // Hit/Sta
    private final int[] jagged = { 39974, 40086, 40165, 40165 };   // Crit/Sta
    private final int[] forceful = { 39978, 40091, 40169, 40169 }; // Haste/Sta

    //Prismatic
    private final int[] tear = { 34143, 42702, 49110, 49110 }; // +X to all stats

    public List<GemmingTemplate> addTemplates(String group, int rarity, int metagem, boolean enabled) {
        List<GemmingTemplate> templates = new ArrayList<GemmingTemplate>();

        templates.add(template("Enhance", group, enabled, tear[rarity], tear[rarity], tear[rarity], tear[rarity], metagem)); // Stats

        templates.add(template("Enhance", group, enabled, precise[rarity], accurate[rarity], guardian[rarity], precise[rarity], metagem)); //Max Expertise - colour match
        templates.add(template("Enhance", group, enabled, precise[rarity], precise[rarity], precise[rarity], precise[rarity], metagem)); //Max Expertise - no colour match

        templates.add(template("Enhance", group, enabled, accurate[rarity], rigid[rarity], vivid[rarity], rigid[rarity], metagem)); //Max Hit - red exp
        templates.add(template("Enhance", group, enabled, glinting[rarity], rigid[rarity], vivid[rarity], rigid[rarity], metagem)); //Max Hit - red agi
        templates.add(template("Enhance", group, enabled, pristine[rarity], rigid[rarity], vivid[rarity], rigid[rarity], metagem)); //Max Hit - red ap
        templates.add(template("Enhance", group, enabled, rigid[rarity], rigid[rarity], rigid[rarity], rigid[rarity], metagem)); //Max Hit - no colour match

        templates.add(template("Enhance", group, enabled, bright[rarity], pristine[rarity], tear[rarity], bright[rarity], metagem)); //Max Attack Power - yellow hit
        templates.add(template("Enhance", group, enabled, bright[rarity], stark[rarity], tear[rarity], bright[rarity], metagem)); //Max Attack Power - yellow haste
        templates.add(template("Enhance", group, enabled, bright[rarity], wicked[rarity], tear[rarity], bright[rarity], metagem)); //Max Attack Power - yellow crit
        templates.add(template("Enhance", group, enabled, bright[rarity], bright[rarity], bright[rarity], bright[rarity], metagem)); //Max Attack Power - no colour match

        templates.add(template("Enhance", group, enabled, delicate[rarity], glinting[rarity], shifting[rarity], delicate[rarity], metagem)); //Max Agility - yellow hit
        templates.add(template("Enhance", group, enabled, delicate[rarity], deft[rarity], shifting[rarity], delicate[rarity], metagem)); //Max Agility - yellow haste
        templates.add(template("Enhance", group, enabled, delicate[rarity], deadly[rarity], shifting[rarity], delicate[rarity], metagem)); //Max Agility - yellow crit
        templates.add(template("Enhance", group, enabled, delicate[rarity], delicate[rarity], delicate[rarity], delicate[rarity], metagem)); //Max Agility - no colour match

        templates.add(template("Enhance", group, enabled, deadly[rarity], smooth[rarity], jagged[rarity], smooth[rarity], metagem)); //Max Crit - red agi
        templates.add(template("Enhance", group, enabled, wicked[rarity], smooth[rarity], jagged[rarity], smooth[rarity], metagem)); //Max Crit - red ap
        templates.add(template("Enhance", group, enabled, smooth[rarity], smooth[rarity], smooth[rarity], smooth[rarity], metagem)); //Max Crit - no colour match

        templates.add(template("Enhance", group, enabled, deft[rarity], quick[rarity], forceful[rarity], quick[rarity], metagem)); //Max Haste - red agi
        templates.add(template("Enhance", group, enabled, stark[rarity], quick[rarity], forceful[rarity], quick[rarity], metagem)); //Max Haste - red ap
        templates.add(template("Enhance", group, enabled, quick[rarity], quick[rarity], quick[rarity], quick[rarity], metagem)); //Max Haste - no colour match

        templates.add(template("Enhance", group, enabled, fractured[rarity], fractured[rarity], fractured[rarity], fractured[rarity], metagem)); //Max Armour Penetration - no colour match

        return templates;
    }

    private static GemmingTemplate template(String model, String group, boolean enabled, int redId, int yellowId, int blueId, int prismaticId, int metaId) {
        GemmingTemplate result = new GemmingTemplate();
        result.setModel(model);
        result.setGroup(group);
        result.setEnabled(enabled);
        result.setRedId(redId);
        result.setYellowId(yellowId);
        result.setBlueId(blueId);
        result.setPrismaticId(prismaticId);
        result.setMetaId(metaId);
        return result;
    }
}
