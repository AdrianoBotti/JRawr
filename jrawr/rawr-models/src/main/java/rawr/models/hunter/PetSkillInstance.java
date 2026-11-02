// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/Pet/PetSkill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.hunter;

import rawr.models.hunter.HunterEnums.PetAttacks;

// This class is used for building the priority list.
// It contains a pointer to the skill it represents.
// This object will hold all of the rotation calculations.

public class PetSkillInstance {
    public PetAttacks skillType;
    public PetSkill skillData;
    private PetSkillPriorityRotation priorityRotation;

    public PetSkillInstance(PetSkillPriorityRotation priorityRotation, PetAttacks skillType, PetSkill skillData) {
        this.priorityRotation = priorityRotation;
        this.skillType = skillType;
        this.skillData = skillData;
    }

    private static final float PetGCD = 1.25f;

    public float focus = 0; // D
    public float cooldown = 0; // E
    public float damage = 0; // scattered all over 'Pet Calculations'
    public boolean canCrit = false;

    public float dps = 0; // I
    public float possibleFreq = 0; // K
    public float gcdLeft = 0; // L
    public float gcdNeeded = 0; // M
    public float gcdUsed = 0; // N
    public float tempFrequency = 0; // O
    public float crittableFreq = 0; // P
    public float fpsNeeded = 0; // Q
    public float fpsAvailable = 0; // R
    public float frequency = 0; // S (and F)
    public float kcDps = 0; // T

    public void calculateTimings(PetSkillInstance prevSkill) {
        possibleFreq = cooldown - (cooldown % PetGCD) + ((cooldown % PetGCD) > 0 ? PetGCD : 0);

        gcdLeft = 1;
        if (prevSkill != null) {
            gcdLeft = prevSkill.gcdLeft - prevSkill.gcdUsed;
        }

        gcdNeeded = cooldown >= 30 ? 0 : possibleFreq > 0 ? PetGCD / possibleFreq : 0;

        gcdUsed = 1f - (gcdLeft - gcdNeeded);
        if (prevSkill != null) {
            gcdUsed = gcdLeft > gcdNeeded ? gcdNeeded : gcdLeft;
        }

        tempFrequency = gcdUsed > 0 ? PetGCD / gcdUsed : cooldown >= 30 ? cooldown : 0;

        fpsNeeded = (1f - priorityRotation.owlsFocus) * (tempFrequency > 0 ? focus / tempFrequency : 0);

        fpsAvailable = priorityRotation.fpsGained;
        if (prevSkill != null) {
            fpsAvailable = prevSkill.fpsAvailable - prevSkill.fpsNeeded;
        }

        frequency = 0;
        if (fpsAvailable > 0) frequency = tempFrequency * (fpsNeeded / fpsAvailable);
        if (fpsAvailable > fpsNeeded) frequency = tempFrequency;

        crittableFreq = canCrit ? frequency : 0;
    }

    public void calculateDPS() {
        dps = frequency > 0 ? damage / frequency : 0;
        kcDps = canCrit ? dps : 0;
    }
}
