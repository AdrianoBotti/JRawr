// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/Pet/PetTalents.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: the Silverlight/RAWR3 PetTalentsBase/PetTalents/PetTalentDataAttribute path and the
// PetTalent icon loading (ItemIconsPet, UI) are omitted; only the desktop PetTalentTreeData path is
// ported.
package rawr.models.hunter;

import java.util.ArrayList;
import java.util.List;

public class PetTalentTreeData {
    public PetTalentTreeData() {
        initialize();
        makeTree();
    }

    public PetTalentTreeData(String source) {
        if (talentTree != null) {
            talentTree.clear();
        }
        initialize();
        makeTree();
        if (source == null || source.isEmpty()) {
            return;
        }
        int index = 0;
        try {
            for (char pt : source.toCharArray()) {
                talentTree.get(index).value = Math.min(talentTree.get(index).max, Integer.parseInt(Character.toString(pt)));
                index++;
            }
        } catch (Exception e) {
            // We don't care that it's wrong
        }
    }

    private List<PetTalent> talentTree;

    public List<PetTalent> getTalentTree() {
        return talentTree;
    }

    public void setTalentTree(List<PetTalent> value) {
        talentTree = value;
    }

    public void makeTree() {
        if (talentTree == null || talentTree.isEmpty()) {
            talentTree = new ArrayList<PetTalent>(java.util.Arrays.asList(
                    bloodOfTheRhino,
                    bloodthirsty,
                    boarsSpeed,
                    bullheaded,
                    callOfTheWild,
                    carrionFeeder,
                    chargeSwoop,
                    cobraReflexes,
                    cornered,
                    cullingTheHerd,
                    diveDash,
                    feedingFrenzy,
                    graceOfTheMantis,
                    greatResistance,
                    greatStamina,
                    guardDog,
                    heartOfThePhoenix,
                    improvedCower,
                    intervene,
                    lastStand,
                    lickYourWounds,
                    lionhearted,
                    mobility,
                    naturalArmor,
                    owlsFocus,
                    petBarding,
                    rabid,
                    roarOfRecovery,
                    roarOfSacrifice,
                    sharkAttack,
                    silverback,
                    spidersBite,
                    spikedCollar,
                    taunt,
                    thunderstomp,
                    wildHunt,
                    wolverineBite));
        }
    }

    // Talents
    public PetTalent bloodOfTheRhino;
    public PetTalent bloodthirsty;
    public PetTalent boarsSpeed;
    public PetTalent bullheaded;
    public PetTalent callOfTheWild;
    public PetTalent carrionFeeder;
    public PetTalent chargeSwoop;
    public PetTalent cobraReflexes;
    public PetTalent cornered;
    public PetTalent diveDash;
    public PetTalent feedingFrenzy;
    public PetTalent graceOfTheMantis;
    public PetTalent greatResistance;
    public PetTalent greatStamina;
    public PetTalent guardDog;
    public PetTalent heartOfThePhoenix;
    public PetTalent improvedCower;
    public PetTalent intervene;
    public PetTalent lastStand;
    public PetTalent lickYourWounds;
    public PetTalent lionhearted;
    public PetTalent mobility;
    public PetTalent naturalArmor;
    public PetTalent owlsFocus;
    public PetTalent petBarding;
    public PetTalent rabid;
    public PetTalent roarOfRecovery;
    public PetTalent roarOfSacrifice;
    public PetTalent sharkAttack;
    public PetTalent silverback;
    public PetTalent spidersBite;
    public PetTalent spikedCollar;
    public PetTalent taunt;
    public PetTalent thunderstomp;
    public PetTalent wildHunt;
    public PetTalent wolverineBite;
    public PetTalent cullingTheHerd;

    /** Initializes the Abilities */
    public void initialize() {
        bloodOfTheRhino = new PetTalent(PetTalentsBaseStatic.BloodOfTheRhino, 0);
        bloodthirsty = new PetTalent(PetTalentsBaseStatic.Bloodthirsty, 0);
        boarsSpeed = new PetTalent(PetTalentsBaseStatic.BoarsSpeed, 0);
        bullheaded = new PetTalent(PetTalentsBaseStatic.Bullheaded, 0);
        callOfTheWild = new PetTalent(PetTalentsBaseStatic.CallOfTheWild, 0);
        carrionFeeder = new PetTalent(PetTalentsBaseStatic.CarrionFeeder, 0);
        chargeSwoop = new PetTalent(PetTalentsBaseStatic.ChargeSwoop, 0);
        cobraReflexes = new PetTalent(PetTalentsBaseStatic.CobraReflexes, 0);
        cornered = new PetTalent(PetTalentsBaseStatic.Cornered, 0);
        diveDash = new PetTalent(PetTalentsBaseStatic.DiveDash, 0);
        feedingFrenzy = new PetTalent(PetTalentsBaseStatic.FeedingFrenzy, 0);
        graceOfTheMantis = new PetTalent(PetTalentsBaseStatic.GraceOfTheMantis, 0);
        greatResistance = new PetTalent(PetTalentsBaseStatic.GreatResistance, 0);
        greatStamina = new PetTalent(PetTalentsBaseStatic.GreatStamina, 0);
        guardDog = new PetTalent(PetTalentsBaseStatic.GuardDog, 0);
        heartOfThePhoenix = new PetTalent(PetTalentsBaseStatic.HeartOfThePhoenix, 0);
        improvedCower = new PetTalent(PetTalentsBaseStatic.ImprovedCower, 0);
        intervene = new PetTalent(PetTalentsBaseStatic.Intervene, 0);
        lastStand = new PetTalent(PetTalentsBaseStatic.LastStand, 0);
        lickYourWounds = new PetTalent(PetTalentsBaseStatic.LickYourWounds, 0);
        lionhearted = new PetTalent(PetTalentsBaseStatic.Lionhearted, 0);
        mobility = new PetTalent(PetTalentsBaseStatic.Mobility, 0);
        naturalArmor = new PetTalent(PetTalentsBaseStatic.NaturalArmor, 0);
        owlsFocus = new PetTalent(PetTalentsBaseStatic.OwlsFocus, 0);
        petBarding = new PetTalent(PetTalentsBaseStatic.PetBarding, 0);
        rabid = new PetTalent(PetTalentsBaseStatic.Rabid, 0);
        roarOfRecovery = new PetTalent(PetTalentsBaseStatic.RoarOfRecovery, 0);
        roarOfSacrifice = new PetTalent(PetTalentsBaseStatic.RoarOfSacrifice, 0);
        sharkAttack = new PetTalent(PetTalentsBaseStatic.SharkAttack, 0);
        silverback = new PetTalent(PetTalentsBaseStatic.Silverback, 0);
        spidersBite = new PetTalent(PetTalentsBaseStatic.SpidersBite, 0);
        spikedCollar = new PetTalent(PetTalentsBaseStatic.SpikedCollar, 0);
        taunt = new PetTalent(PetTalentsBaseStatic.Taunt, 0);
        thunderstomp = new PetTalent(PetTalentsBaseStatic.Thunderstomp, 0);
        wildHunt = new PetTalent(PetTalentsBaseStatic.WildHunt, 0);
        wolverineBite = new PetTalent(PetTalentsBaseStatic.WolverineBite, 0);
        cullingTheHerd = new PetTalent(PetTalentsBaseStatic.CullingTheHerd, 0);
    }

    /** Resets all Talents to 0 */
    public void reset() {
        if (cobraReflexes.id == -1) {
            initialize();
            return;
        }
        cobraReflexes.value = 0;
        diveDash.value = 0;
        chargeSwoop.value = 0;
        greatStamina.value = 0;
        naturalArmor.value = 0;
        boarsSpeed.value = 0;
        mobility.value = 0;
        spikedCollar.value = 0;
        improvedCower.value = 0;
        bloodthirsty.value = 0;
        bloodOfTheRhino.value = 0;
        petBarding.value = 0;
        cullingTheHerd.value = 0;
        lionhearted.value = 0;
        carrionFeeder.value = 0;
        guardDog.value = 0;
        thunderstomp.value = 0;
        greatResistance.value = 0;
        owlsFocus.value = 0;
        cornered.value = 0;
        feedingFrenzy.value = 0;
        heartOfThePhoenix.value = 0;
        spidersBite.value = 0;
        wolverineBite.value = 0;
        roarOfRecovery.value = 0;
        bullheaded.value = 0;
        graceOfTheMantis.value = 0;
        rabid.value = 0;
        lickYourWounds.value = 0;
        callOfTheWild.value = 0;
        lastStand.value = 0;
        taunt.value = 0;
        intervene.value = 0;
        wildHunt.value = 0;
        roarOfSacrifice.value = 0;
        sharkAttack.value = 0;
        silverback.value = 0;
    }

    public int totalPoints() {
        int total = 0;
        for (PetTalent pt : getTalentTree()) {
            total += pt.value;
        }
        return total;
    }

    public HunterEnums.PetFamilyTree getPetFamilyTree() {
        if (roarOfRecovery.value + cornered.value + bullheaded.value + feedingFrenzy.value > 0)
            return HunterEnums.PetFamilyTree.Cunning;
        else if (sharkAttack.value + rabid.value + lickYourWounds.value > 0)
            return HunterEnums.PetFamilyTree.Ferocity;
        else if (silverback.value + lastStand.value + intervene.value + thunderstomp.value + bloodOfTheRhino.value > 0)
            return HunterEnums.PetFamilyTree.Tenacity;
        else
            return HunterEnums.PetFamilyTree.None;
    }

    @Override
    public String toString() {
        String ret = "";
        if (getTalentTree() == null
                || getTalentTree().isEmpty()
                || getTalentTree().size() > 38) {
            if (talentTree != null) {
                talentTree.clear();
            }
            initialize();
        }
        for (PetTalent pt : getTalentTree()) {
            ret += Integer.toString(pt.value);
        }
        return ret;
    }
}
