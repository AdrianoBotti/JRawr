// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/Pet/PetSkill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// FRAMEWORK ADAPTATION: the C# Debug.WriteLine debug block in calculateDPS is omitted (debug output
// only).
package rawr.models.hunter;

import rawr.base.character.Character;
import rawr.models.hunter.HunterEnums.PetAttacks;
import rawr.models.hunter.HunterEnums.PetSkillType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PetSkillPriorityRotation {
    public List<PetSkillInstance> skills = new ArrayList<PetSkillInstance>();
    private CalculationOptionsHunter options;
    private Character character;

    private static final Map<PetAttacks, PetSkill> skillLibrary = new LinkedHashMap<PetAttacks, PetSkill>();

    static {
        // common skills
        skillLibrary.put(PetAttacks.Growl, new PetSkill(false, 5f, 15, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.Cower, new PetSkill(false, 20f, 25, PetSkillType.NonDamaging));

        // focus dumps
        skillLibrary.put(PetAttacks.Claw, new PetSkill(true, 1.25f, 25, PetSkillType.FocusDump));
        skillLibrary.put(PetAttacks.Bite, new PetSkill(true, 1.25f, 25, PetSkillType.FocusDump));
        skillLibrary.put(PetAttacks.Smack, new PetSkill(true, 1.25f, 25, PetSkillType.FocusDump));

        // movement skills
        skillLibrary.put(PetAttacks.Dive, new PetSkill(false, 32, 20, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.Dash, new PetSkill(false, 32, 20, PetSkillType.NonDamaging));

        // family skills - non damaging
        skillLibrary.put(PetAttacks.Snatch, new PetSkill(false, 60, 20, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.Prowl, new PetSkill(false, 10, 0, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.Pin, new PetSkill(false, 60, 0, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.BadAttitude, new PetSkill(false, 120, 0, PetSkillType.NonDamaging)); // technically a damaging ability, but only if you get hit
        skillLibrary.put(PetAttacks.Pummel, new PetSkill(false, 30, 20, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.SerenityDust, new PetSkill(false, 60, 0, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.Stampede, new PetSkill(false, 60, 0, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.Web, new PetSkill(false, 120, 0, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.DustCloud, new PetSkill(false, 40, 20, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.ShellShield, new PetSkill(false, 180, 0, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.Warp, new PetSkill(false, 15, 0, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.FuriousHowl, new PetSkill(false, 40, 20, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.TendonRip, new PetSkill(false, 20, 20, PetSkillType.NonDamaging));

        // family skills - damaging
        skillLibrary.put(PetAttacks.SonicBlast, new PetSkill(true, 80, 0, PetSkillType.SpecialSpell, 62, 88));
        skillLibrary.put(PetAttacks.Swipe, new PetSkill(true, 5, 20, PetSkillType.SpecialMelee, 90, 126));
        skillLibrary.put(PetAttacks.Gore, new PetSkill(true, 10, 20, PetSkillType.SpecialMelee, 122, 164));
        skillLibrary.put(PetAttacks.DemoralizingScreech, new PetSkill(true, 10, 20, PetSkillType.SpecialMelee, 85, 129));
        skillLibrary.put(PetAttacks.Rake, new PetSkill(true, 10, 20, PetSkillType.SpecialUnique));
        skillLibrary.put(PetAttacks.FroststormBreath, new PetSkill(true, 10, 20, PetSkillType.SpecialSpell, 128, 172));
        skillLibrary.put(PetAttacks.LavaBreath, new PetSkill(true, 10, 20, PetSkillType.SpecialSpell, 128, 172));
        skillLibrary.put(PetAttacks.MonstrousBite, new PetSkill(true, 10, 20, PetSkillType.SpecialMelee, 91, 123));
        skillLibrary.put(PetAttacks.FireBreath, new PetSkill(false, 10, 20, PetSkillType.SpecialUnique));
        skillLibrary.put(PetAttacks.NetherShock, new PetSkill(true, 40, 0, PetSkillType.SpecialSpell, 64, 86));
        skillLibrary.put(PetAttacks.SavageRend, new PetSkill(true, 60, 20, PetSkillType.SpecialUnique));
        skillLibrary.put(PetAttacks.Ravage, new PetSkill(true, 40, 0, PetSkillType.SpecialMelee, 85, 129));
        skillLibrary.put(PetAttacks.ScorpidPoison, new PetSkill(false, 10, 20, PetSkillType.SpecialUnique));
        skillLibrary.put(PetAttacks.PoisonSpit, new PetSkill(false, 10, 20, PetSkillType.SpecialUnique));
        skillLibrary.put(PetAttacks.VenomWebSpray, new PetSkill(false, 120, 0, PetSkillType.SpecialUnique));
        skillLibrary.put(PetAttacks.SpiritStrike, new PetSkill(true, 10, 20, PetSkillType.SpecialUnique));
        skillLibrary.put(PetAttacks.SporeCloud, new PetSkill(false, 10, 20, PetSkillType.SpecialUnique, 25, 25));
        skillLibrary.put(PetAttacks.Sting, new PetSkill(true, 6, 20, PetSkillType.SpecialSpell, 64, 86));
        skillLibrary.put(PetAttacks.LightningBreath, new PetSkill(true, 10, 20, PetSkillType.SpecialSpell, 80, 120));
        skillLibrary.put(PetAttacks.AcidSpit, new PetSkill(true, 10, 20, PetSkillType.SpecialUnique, 124, 176));

        // talented skills
        skillLibrary.put(PetAttacks.Charge, new PetSkill(false, 25, 35, PetSkillType.Unimplemented)); // maybe this will cause damage :)
        skillLibrary.put(PetAttacks.WolverineBite, new PetSkill(true, 0, 0, PetSkillType.Unimplemented));
        skillLibrary.put(PetAttacks.LastStand, new PetSkill(false, 360, 0, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.RoarOfRecovery, new PetSkill(false, 180, 0, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.RoarOfSacrifice, new PetSkill(false, 30, 0, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.LickYourWounds, new PetSkill(false, 180, 0, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.Taunt, new PetSkill(false, 180, 0, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.Thunderstomp, new PetSkill(true, 10, 20, PetSkillType.Unimplemented));

        skillLibrary.put(PetAttacks.Rabid, new PetSkill(false, 45, 0, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.Bullheaded, new PetSkill(false, 180, 0, PetSkillType.NonDamaging));
        skillLibrary.put(PetAttacks.CallOfTheWild, new PetSkill(false, 300, 0, PetSkillType.NonDamaging));
    }

    public PetSkillPriorityRotation(Character character, CalculationOptionsHunter options) {
        this.character = character;
        this.options = options;
    }

    public void addSkill(PetAttacks skillType) {
        // don't add non-skills to the rotation
        if (skillType == PetAttacks.None) return;

        // check that this skill is not already in the rotation
        for (PetSkillInstance s : skills) {
            if (s.skillType == skillType) return;
        }

        // don't add skills that require a talent we are missing
        if (skillType == PetAttacks.Bullheaded && options.getPetTalents().bullheaded.value == 0) return;
        if (skillType == PetAttacks.CallOfTheWild && options.getPetTalents().callOfTheWild.value == 0) return;
        if (skillType == PetAttacks.Dash && options.getPetTalents().diveDash.value == 0) return;
        if (skillType == PetAttacks.Dive && options.getPetTalents().diveDash.value == 0) return;
        if (skillType == PetAttacks.LastStand && options.getPetTalents().lastStand.value == 0) return;
        if (skillType == PetAttacks.LickYourWounds && options.getPetTalents().lickYourWounds.value == 0) return;
        if (skillType == PetAttacks.Rabid && options.getPetTalents().rabid.value == 0) return;
        if (skillType == PetAttacks.RoarOfRecovery && options.getPetTalents().roarOfRecovery.value == 0) return;
        if (skillType == PetAttacks.RoarOfSacrifice && options.getPetTalents().roarOfSacrifice.value == 0) return;
        if (skillType == PetAttacks.Taunt && options.getPetTalents().taunt.value == 0) return;
        if (skillType == PetAttacks.WolverineBite && options.getPetTalents().wolverineBite.value == 0) return;
        if (skillType == PetAttacks.Thunderstomp && options.getPetTalents().thunderstomp.value == 0) return;
        if (skillType == PetAttacks.Charge && options.getPetTalents().chargeSwoop.value == 0) return;
        if (skillType == PetAttacks.Swoop && options.getPetTalents().chargeSwoop.value == 0) return;

        // it looks good - create an instance wrapper
        skills.add(new PetSkillInstance(this, skillType, skillLibrary.get(skillType)));
    }

    public float owlsFocus = 0;
    public float fpsGained = 0;

    public void calculateTimings() {
        calculateSkillData();

        float crittableInverseSum = 0;

        PetSkillInstance prevSkill = null;
        for (PetSkillInstance s : skills) {
            s.calculateTimings(prevSkill);
            prevSkill = s;

            crittableInverseSum += s.crittableFreq > 0 ? 1f / s.crittableFreq : 0;
        }

        petSpecialFrequency = crittableInverseSum > 0 ? 1f / crittableInverseSum : 0;
    }

    private void calculateSkillData() {
        // This function is called before we calculate timings, to allow
        // us to calculate cooldown and focus cost for the skill

        float longevityCooldownAdjust = 1f - character.getHunterTalents().getLongevity() * 0.1f;

        for (PetSkillInstance s : skills) {
            s.cooldown = s.skillData.cooldown;
            s.focus = s.skillData.focus;
            s.canCrit = s.skillData.canCrit;

            if (s.skillType == PetAttacks.Dive) s.cooldown -= options.getPetTalents().diveDash.value * 8f;
            if (s.skillType == PetAttacks.Dash) s.cooldown -= options.getPetTalents().diveDash.value * 8f;

            if (s.skillData.type != PetSkillType.FocusDump) s.cooldown *= longevityCooldownAdjust;
        }
    }

    public float dps;
    public float petSpecialFrequency = 0;
    public float kcDps = 0;

    public void calculateDPS() {
        dps = 0;
        kcDps = 0;
        for (PetSkillInstance s : skills) {
            dps += s.dps;
            kcDps += s.kcDps;
        }
    }

    public float getSkillFrequency(PetAttacks skill) {
        // if we're asking for the frequency of one of the 'always on' skills,
        // and we have the talent for it, just return the cooldown.
        if (skill == PetAttacks.Rabid) return (options.getPetTalents().rabid.value > 0) ? skillLibrary.get(PetAttacks.Rabid).cooldown : 0;
        if (skill == PetAttacks.CallOfTheWild) return (options.getPetTalents().callOfTheWild.value > 0) ? skillLibrary.get(PetAttacks.CallOfTheWild).cooldown : 0;
        if (skill == PetAttacks.Bullheaded) return (options.getPetTalents().bullheaded.value > 0) ? skillLibrary.get(PetAttacks.Bullheaded).cooldown : 0;
        if (skill == PetAttacks.RoarOfRecovery) return (options.getPetTalents().roarOfRecovery.value > 0) ? skillLibrary.get(PetAttacks.RoarOfRecovery).cooldown : 0;

        for (PetSkillInstance s : skills) {
            if (s.skillType == skill) return s.frequency;
        }
        return 0;
    }

    public float getSkillCooldown(PetAttacks skill) {
        for (PetSkillInstance s : skills) {
            if (s.skillType == skill) return s.cooldown;
        }
        return 0;
    }
}
