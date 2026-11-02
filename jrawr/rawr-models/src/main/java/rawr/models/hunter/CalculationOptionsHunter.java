// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/CalculationOptionsHunter.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// FRAMEWORK ADAPTATION: INotifyPropertyChanged is not ported (UI-only); GetXml writes the
// XmlSerializer element order by hand and deserializeDataObject is implemented in CalculationsHunter.
package rawr.models.hunter;

import rawr.base.character.Buff;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.util.NetFormat;
import rawr.base.stats.StatConversion;
import rawr.models.hunter.HunterEnums.Aspect;
import rawr.models.hunter.HunterEnums.AspectUsage;
import rawr.models.hunter.HunterEnums.PetAttacks;
import rawr.models.hunter.HunterEnums.PetFamily;
import rawr.models.hunter.HunterEnums.PetFamilyTree;
import rawr.models.hunter.HunterEnums.PetHappiness;
import rawr.models.hunter.HunterEnums.Shots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CalculationOptionsHunter implements ICalculationOptionBase {

    // ==== Boss Settings ====
    public int targetLevel = 83;
    public float targetArmor = StatConversion.NPC_ARMOR[3];
    public int duration = 300;
    public int timeSpentSub20 = 72;
    public int timeSpent35To20 = 54;
    public boolean multipleTargets = false;
    public float multipleTargetsPerc = 0;

    // ==== Fight Settings ====
    public float lag = 150;
    public float react = 200;
    public float bossHPPerc = 1.00f;

    // ==== Hunter Settings ====
    public int cdCutoff = 0;
    public Aspect selectedAspect = Aspect.Dragonhawk;
    public AspectUsage aspectUsage = AspectUsage.ViperToOOM;
    public boolean useBeastDuringBestialWrath = true;
    public boolean useRotationTest = true;
    public boolean randomizeProcs = false;

    // ==== Rotations Tab ====
    public int priorityIndex1 = 0;
    public int priorityIndex2 = 0;
    public int priorityIndex3 = 0;
    public int priorityIndex4 = 0;
    public int priorityIndex5 = 0;
    public int priorityIndex6 = 0;
    public int priorityIndex7 = 0;
    public int priorityIndex8 = 0;
    public int priorityIndex9 = 0;
    public int priorityIndex10 = 0;

    // ==== Pet Tab ====
    public int petLevel = 80; // Not Editable
    public int selectedArmoryPet = 0;
    public PetHappiness petHappinessLevel = PetHappiness.Happy; // Not Editable
    private PetTalentTreeData petTalentTree;
    public String petTalents; // serialized form of PetTalents (C# property `petTalents`)
    public PetFamily petFamily = PetFamily.Cat;
    public PetAttacks petPriority1 = PetAttacks.Growl;
    public PetAttacks petPriority2 = PetAttacks.Bite;
    public PetAttacks petPriority3 = PetAttacks.None;
    public PetAttacks petPriority4 = PetAttacks.None;
    public PetAttacks petPriority5 = PetAttacks.None;
    public PetAttacks petPriority6 = PetAttacks.None;
    public PetAttacks petPriority7 = PetAttacks.None;

    public List<String> petActiveBuffsXml = new ArrayList<String>();
    private List<Buff> petActiveBuffs;

    public List<Buff> getPetActiveBuffs() {
        if (this.petActiveBuffs == null) {
            this.petActiveBuffs = new ArrayList<Buff>();
        }
        return this.petActiveBuffs;
    }

    public void setPetActiveBuffs(List<Buff> value) {
        this.petActiveBuffs = value;
    }

    // ==== Misc Tab ====
    public boolean hideBadItemsSpl = true;
    public boolean hideBadItemsPvP = true;
    public boolean ptrMode = false;
    public float survScale = 1.0f;
    public boolean seUseDur = true;
    public boolean[] statsList = new boolean[] { true, true, true, true, true, true };
    public int statsIncrement = 100;
    public String calculationToGraph = "Overall Rating";

    // ==== Other Options not on the pane ====
    public float waitForCooldown = 0.8f; // not editable
    public boolean interleaveLAL = false; // not editable
    public boolean prioritiseArcAimedOverSteady = true; // not editable
    public boolean debugShotRotation = false; // not editable

    public boolean useKillCommand = true;
    public float gcdsToLayImmoTrap = 2.0f; // not editable
    public float gcdsToLayExploTrap = 2.0f; // not editable
    public float gcdsToVolley = 4.0f; // not editable, 6 seconds
    public Shots lalShotToUse = Shots.ExplosiveShot; // not editable
    public int lalShotsReplaced = 2; // not editable
    public float lalProcChance = 2; // not editable

    public float getLatency() {
        return lag / 1000f;
    }

    public float getAllowedReact() {
        return Math.max(0f, (react - 250f) / 1000f);
    }

    public float getFullLatency() {
        return getAllowedReact() + getLatency();
    }

    public PetTalentTreeData getPetTalents() {
        if (this.petTalentTree == null) {
            this.petTalentTree = new PetTalentTreeData();
        }
        return this.petTalentTree;
    }

    public void setPetTalents(PetTalentTreeData value) {
        this.petTalentTree = value;
    }

    public int[] getPriorityIndexes() {
        return new int[] { priorityIndex1, priorityIndex2, priorityIndex3, priorityIndex4, priorityIndex5,
                priorityIndex6, priorityIndex7, priorityIndex8, priorityIndex9, priorityIndex10 };
    }

    public void setPriorityIndexes(int[] value) {
        priorityIndex1 = value[0];
        priorityIndex2 = value[1];
        priorityIndex3 = value[2];
        priorityIndex4 = value[3];
        priorityIndex5 = value[4];
        priorityIndex6 = value[5];
        priorityIndex7 = value[6];
        priorityIndex8 = value[7];
        priorityIndex9 = value[8];
        priorityIndex10 = value[9];
    }

    // ==== Shots ====
    public static final Shot None = new Shot(0, "None");
    public static final Shot AimedShot = new Shot(1, "Aimed Shot");
    public static final Shot ArcaneShot = new Shot(2, "Arcane Shot");
    public static final Shot MultiShot = new Shot(3, "Multi-Shot");
    public static final Shot SerpentSting = new Shot(4, "Serpent Sting");
    public static final Shot ScorpidSting = new Shot(5, "Scorpid Sting");
    public static final Shot ViperSting = new Shot(6, "Viper Sting");
    public static final Shot SilencingShot = new Shot(7, "Silencing Shot");
    public static final Shot SteadyShot = new Shot(8, "Steady Shot");
    public static final Shot KillShot = new Shot(9, "Kill Shot");
    public static final Shot ExplosiveShot = new Shot(10, "Explosive Shot");
    public static final Shot BlackArrow = new Shot(11, "Black Arrow");
    public static final Shot ImmolationTrap = new Shot(12, "Immolation Trap");
    public static final Shot ExplosiveTrap = new Shot(13, "Explosive Trap");
    public static final Shot FreezingTrap = new Shot(14, "Freezing Trap");
    public static final Shot FrostTrap = new Shot(15, "Frost Trap");
    public static final Shot Volley = new Shot(16, "Volley");
    public static final Shot ChimeraShot = new Shot(17, "Chimera Shot");
    public static final Shot RapidFire = new Shot(18, "Rapid Fire");
    public static final Shot Readiness = new Shot(19, "Readiness");
    public static final Shot BeastialWrath = new Shot(20, "Beastial Wrath");

    private static List<Shot> shotList = null;

    public static List<Shot> getShotList() {
        if (shotList == null) {
            shotList = new ArrayList<Shot>(Arrays.asList(
                    None, AimedShot, ArcaneShot, MultiShot, SerpentSting, ScorpidSting, ViperSting, SilencingShot,
                    SteadyShot, KillShot, ExplosiveShot, BlackArrow, ImmolationTrap, ExplosiveTrap, FreezingTrap,
                    FrostTrap, Volley, ChimeraShot, RapidFire, Readiness, BeastialWrath));
        }
        return shotList;
    }

    public static final ShotGroup Marksman = new ShotGroup("Marksman", Arrays.asList(
            RapidFire, Readiness, SerpentSting, ChimeraShot, KillShot, AimedShot, SilencingShot, SteadyShot,
            None, None));

    public static final ShotGroup BeastMaster = new ShotGroup("Beast Master", Arrays.asList(
            RapidFire, BeastialWrath, KillShot, AimedShot, ArcaneShot, SerpentSting, SteadyShot,
            None, None, None));

    public static final ShotGroup Survival = new ShotGroup("Survival", Arrays.asList(
            RapidFire, KillShot, ExplosiveShot, SerpentSting, BlackArrow, AimedShot, SteadyShot,
            None, None, None));

    // ==== Pet Functions ====
    public static PetFamilyTree petFamilyToPetFamilyTree(PetFamily family) {
        switch (family) {
            case Bat:
            case BirdOfPrey:
            case Chimaera:
            case Dragonhawk:
            case NetherRay:
            case Ravager:
            case Serpent:
            case Silithid:
            case Spider:
            case SporeBat:
            case WindSerpent:
                return PetFamilyTree.Cunning;

            case Bear:
            case Boar:
            case Crab:
            case Crocolisk:
            case Gorilla:
            case Rhino:
            case Scorpid:
            case Turtle:
            case WarpStalker:
            case Worm:
                return PetFamilyTree.Tenacity;

            case CarrionBird:
            case Cat:
            case CoreHound:
            case Devilsaur:
            case Hyena:
            case Moth:
            case Raptor:
            case SpiritBeast:
            case Tallstrider:
            case Wasp:
            case Wolf:
                return PetFamilyTree.Ferocity;
            default:
                return PetFamilyTree.None;
        }
    }

    // C# GetXml: writes the XmlSerializer element order for CalculationOptionsHunter.
    public String getXml() {
        petActiveBuffsXml = new ArrayList<String>();
        for (Buff buff : getPetActiveBuffs()) {
            petActiveBuffsXml.add(buff.name);
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>\r\n");
        xml.append("<CalculationOptionsHunter xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n");
        element(xml, "_TimeSpentSub20", Integer.toString(timeSpentSub20));
        element(xml, "_TimeSpent35To20", Integer.toString(timeSpent35To20));
        element(xml, "_BossHPPerc", NetFormat.floatR(bossHPPerc));
        element(xml, "_CDCutoff", Integer.toString(cdCutoff));
        element(xml, "_AspectUsage", aspectUsage.name());
        element(xml, "_UseBeastDuringBestialWrath", Boolean.toString(useBeastDuringBestialWrath));
        element(xml, "_UseRotationTest", Boolean.toString(useRotationTest));
        element(xml, "_RandomizeProcs", Boolean.toString(randomizeProcs));
        int[] indexes = getPriorityIndexes();
        for (int i = 0; i < 10; i++) {
            element(xml, "_PriorityIndex" + (i + 1), Integer.toString(indexes[i]));
        }
        PetAttacks[] petPriorities = new PetAttacks[] { petPriority1, petPriority2, petPriority3, petPriority4,
                petPriority5, petPriority6, petPriority7 };
        for (int i = 0; i < petPriorities.length; i++) {
            element(xml, "_PetPriority" + (i + 1), petPriorities[i].name());
        }
        for (String buffName : petActiveBuffsXml) {
            element(xml, "petActiveBuffs", buffName);
        }
        element(xml, "waitForCooldown", NetFormat.floatR(waitForCooldown));
        element(xml, "interleaveLAL", Boolean.toString(interleaveLAL));
        element(xml, "prioritiseArcAimedOverSteady", Boolean.toString(prioritiseArcAimedOverSteady));
        element(xml, "debugShotRotation", Boolean.toString(debugShotRotation));
        element(xml, "TargetLevel", Integer.toString(targetLevel));
        element(xml, "TargetArmor", NetFormat.floatR(targetArmor));
        element(xml, "Duration", Integer.toString(duration));
        element(xml, "TimeSpentSub20", Integer.toString(timeSpentSub20));
        element(xml, "TimeSpent35To20", Integer.toString(timeSpent35To20));
        element(xml, "MultipleTargets", Boolean.toString(multipleTargets));
        element(xml, "MultipleTargetsPerc", NetFormat.floatR(multipleTargetsPerc));
        element(xml, "Lag", NetFormat.floatR(lag));
        element(xml, "React", NetFormat.floatR(react));
        element(xml, "BossHPPerc", NetFormat.floatR(bossHPPerc));
        element(xml, "CDCutoff", Integer.toString(cdCutoff));
        element(xml, "SelectedAspect", selectedAspect.name());
        element(xml, "AspectUsage", aspectUsage.name());
        element(xml, "UseBeastDuringBestialWrath", Boolean.toString(useBeastDuringBestialWrath));
        element(xml, "UseRotationTest", Boolean.toString(useRotationTest));
        element(xml, "RandomizeProcs", Boolean.toString(randomizeProcs));
        for (int i = 0; i < 10; i++) {
            element(xml, "PriorityIndex" + (i + 1), Integer.toString(indexes[i]));
        }
        element(xml, "PetLevel", Integer.toString(petLevel));
        element(xml, "SelectedArmoryPet", Integer.toString(selectedArmoryPet));
        element(xml, "PetHappinessLevel", petHappinessLevel.name());
        element(xml, "petTalents", petTalents != null ? petTalents : getPetTalents().toString());
        element(xml, "PetFamily", petFamily.name());
        for (int i = 0; i < petPriorities.length; i++) {
            element(xml, "PetPriority" + (i + 1), petPriorities[i].name());
        }
        element(xml, "HideBadItems_Spl", Boolean.toString(hideBadItemsSpl));
        element(xml, "HideBadItems_PvP", Boolean.toString(hideBadItemsPvP));
        element(xml, "PTRMode", Boolean.toString(ptrMode));
        element(xml, "SurvScale", NetFormat.floatR(survScale));
        element(xml, "SE_UseDur", Boolean.toString(seUseDur));
        if (statsList != null) {
            xml.append("  <StatsList>\r\n");
            for (boolean b : statsList) {
                xml.append("    <boolean>").append(b).append("</boolean>\r\n");
            }
            xml.append("  </StatsList>\r\n");
        }
        element(xml, "StatsIncrement", Integer.toString(statsIncrement));
        element(xml, "CalculationToGraph", calculationToGraph);
        element(xml, "useKillCommand", Boolean.toString(useKillCommand));
        element(xml, "gcdsToLayImmoTrap", NetFormat.floatR(gcdsToLayImmoTrap));
        element(xml, "gcdsToLayExploTrap", NetFormat.floatR(gcdsToLayExploTrap));
        element(xml, "gcdsToVolley", NetFormat.floatR(gcdsToVolley));
        element(xml, "LALShotToUse", lalShotToUse.name());
        element(xml, "LALShotsReplaced", Integer.toString(lalShotsReplaced));
        element(xml, "LALProcChance", NetFormat.floatR(lalProcChance));
        xml.append("</CalculationOptionsHunter>");
        return xml.toString();
    }

    private static void element(StringBuilder xml, String name, String value) {
        xml.append("  <").append(name).append('>').append(value).append("</").append(name).append(">\r\n");
    }
}
