// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassTalents.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassGlyphs.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.talents;

/**
 * The rogue's talent and glyph allocation: the Assassination, Combat and Subtlety trees plus 34
 * glyph slots.
 *
 * <p>{@code data} has 83 rank slots, contiguous per tree: Assassination 0-26 (tree 0), Combat 27-54
 * (tree 1), Subtlety 55-82 (tree 2); {@link #TreeNames} fixes that order. The rogue model reads
 * ranks such as {@link #getImprovedEviscerate()} (Assassination first talent) and
 * {@link #getRelentlessStrikes()} (Subtlety first talent) and tests flags like
 * {@link #isGlyphOfMutilate()} and {@link #isGlyphOfHungerforBlood()}.</p>
 *
 * <p>{@code glyphData} has 34 flags: 28 major glyphs (0-27) then 6 minor glyphs (28-33). One
 * faithfully ported quirk: the getter {@link #isGlyphOfDistrict()} is the method kept from the C#
 * original for the entry named {@code "Glyph of Distract"} (index 31), so method name and display
 * name differ. Construction, cloning and the live-array accessors follow {@link TalentsBase}.</p>
 */
public class RogueTalents extends TalentsBase {
    public static final String[] TreeNames = new String[] {
        "Assassination",
        "Combat",
        "Subtlety",
    };

    private final int[] data = new int[83];
    private final boolean[] glyphData = new boolean[34];

    public RogueTalents() {
    }

    public RogueTalents(String talents) {
        loadString(talents);
    }

    @Override
    public int[] getData() {
        return data;
    }

    @Override
    public boolean[] getGlyphData() {
        return glyphData;
    }

    public RogueTalents clone() {
        RogueTalents clone = new RogueTalents();
        System.arraycopy(data, 0, clone.data, 0, data.length);
        System.arraycopy(glyphData, 0, clone.glyphData, 0, glyphData.length);
        return clone;
    }

    @TalentDataAttribute(index = 0, name = "Improved Eviscerate", maxPoints = 3, tree = 0, column = 1, row = 1, prerequisite = -1)
    public int getImprovedEviscerate() {
        return data[0];
    }

    public void setImprovedEviscerate(int value) {
        data[0] = value;
    }

    @TalentDataAttribute(index = 1, name = "Remorseless Attacks", maxPoints = 2, tree = 0, column = 2, row = 1, prerequisite = -1)
    public int getRemorselessAttacks() {
        return data[1];
    }

    public void setRemorselessAttacks(int value) {
        data[1] = value;
    }

    @TalentDataAttribute(index = 2, name = "Malice", maxPoints = 5, tree = 0, column = 3, row = 1, prerequisite = -1)
    public int getMalice() {
        return data[2];
    }

    public void setMalice(int value) {
        data[2] = value;
    }

    @TalentDataAttribute(index = 3, name = "Ruthlessness", maxPoints = 3, tree = 0, column = 1, row = 2, prerequisite = -1)
    public int getRuthlessness() {
        return data[3];
    }

    public void setRuthlessness(int value) {
        data[3] = value;
    }

    @TalentDataAttribute(index = 4, name = "Blood Spatter", maxPoints = 2, tree = 0, column = 2, row = 2, prerequisite = -1)
    public int getBloodSpatter() {
        return data[4];
    }

    public void setBloodSpatter(int value) {
        data[4] = value;
    }

    @TalentDataAttribute(index = 5, name = "Puncturing Wounds", maxPoints = 3, tree = 0, column = 4, row = 2, prerequisite = -1)
    public int getPuncturingWounds() {
        return data[5];
    }

    public void setPuncturingWounds(int value) {
        data[5] = value;
    }

    @TalentDataAttribute(index = 6, name = "Vigor", maxPoints = 1, tree = 0, column = 1, row = 3, prerequisite = -1)
    public int getVigor() {
        return data[6];
    }

    public void setVigor(int value) {
        data[6] = value;
    }

    @TalentDataAttribute(index = 7, name = "Improved Expose Armor", maxPoints = 2, tree = 0, column = 2, row = 3, prerequisite = -1)
    public int getImprovedExposeArmor() {
        return data[7];
    }

    public void setImprovedExposeArmor(int value) {
        data[7] = value;
    }

    @TalentDataAttribute(index = 8, name = "Lethality", maxPoints = 5, tree = 0, column = 3, row = 3, prerequisite = 2)
    public int getLethality() {
        return data[8];
    }

    public void setLethality(int value) {
        data[8] = value;
    }

    @TalentDataAttribute(index = 9, name = "Vile Poisons", maxPoints = 3, tree = 0, column = 2, row = 4, prerequisite = -1)
    public int getVilePoisons() {
        return data[9];
    }

    public void setVilePoisons(int value) {
        data[9] = value;
    }

    @TalentDataAttribute(index = 10, name = "Improved Poisons", maxPoints = 5, tree = 0, column = 3, row = 4, prerequisite = -1)
    public int getImprovedPoisons() {
        return data[10];
    }

    public void setImprovedPoisons(int value) {
        data[10] = value;
    }

    @TalentDataAttribute(index = 11, name = "Fleet Footed", maxPoints = 2, tree = 0, column = 1, row = 5, prerequisite = -1)
    public int getFleetFooted() {
        return data[11];
    }

    public void setFleetFooted(int value) {
        data[11] = value;
    }

    @TalentDataAttribute(index = 12, name = "Cold Blood", maxPoints = 1, tree = 0, column = 2, row = 5, prerequisite = -1)
    public int getColdBlood() {
        return data[12];
    }

    public void setColdBlood(int value) {
        data[12] = value;
    }

    @TalentDataAttribute(index = 13, name = "Improved Kidney Shot", maxPoints = 3, tree = 0, column = 3, row = 5, prerequisite = -1)
    public int getImprovedKidneyShot() {
        return data[13];
    }

    public void setImprovedKidneyShot(int value) {
        data[13] = value;
    }

    @TalentDataAttribute(index = 14, name = "Quick Recovery", maxPoints = 2, tree = 0, column = 4, row = 5, prerequisite = -1)
    public int getQuickRecovery() {
        return data[14];
    }

    public void setQuickRecovery(int value) {
        data[14] = value;
    }

    @TalentDataAttribute(index = 15, name = "Seal Fate", maxPoints = 5, tree = 0, column = 2, row = 6, prerequisite = 12)
    public int getSealFate() {
        return data[15];
    }

    public void setSealFate(int value) {
        data[15] = value;
    }

    @TalentDataAttribute(index = 16, name = "Murder", maxPoints = 2, tree = 0, column = 3, row = 6, prerequisite = -1)
    public int getMurder() {
        return data[16];
    }

    public void setMurder(int value) {
        data[16] = value;
    }

    @TalentDataAttribute(index = 17, name = "Deadly Brew", maxPoints = 2, tree = 0, column = 1, row = 7, prerequisite = -1)
    public int getDeadlyBrew() {
        return data[17];
    }

    public void setDeadlyBrew(int value) {
        data[17] = value;
    }

    @TalentDataAttribute(index = 18, name = "Overkill", maxPoints = 1, tree = 0, column = 2, row = 7, prerequisite = -1)
    public int getOverkill() {
        return data[18];
    }

    public void setOverkill(int value) {
        data[18] = value;
    }

    @TalentDataAttribute(index = 19, name = "Deadened Nerves", maxPoints = 3, tree = 0, column = 3, row = 7, prerequisite = -1)
    public int getDeadenedNerves() {
        return data[19];
    }

    public void setDeadenedNerves(int value) {
        data[19] = value;
    }

    @TalentDataAttribute(index = 20, name = "Focused Attacks", maxPoints = 3, tree = 0, column = 1, row = 8, prerequisite = -1)
    public int getFocusedAttacks() {
        return data[20];
    }

    public void setFocusedAttacks(int value) {
        data[20] = value;
    }

    @TalentDataAttribute(index = 21, name = "Find Weakness", maxPoints = 3, tree = 0, column = 3, row = 8, prerequisite = -1)
    public int getFindWeakness() {
        return data[21];
    }

    public void setFindWeakness(int value) {
        data[21] = value;
    }

    @TalentDataAttribute(index = 22, name = "Master Poisoner", maxPoints = 3, tree = 0, column = 1, row = 9, prerequisite = -1)
    public int getMasterPoisoner() {
        return data[22];
    }

    public void setMasterPoisoner(int value) {
        data[22] = value;
    }

    @TalentDataAttribute(index = 23, name = "Mutilate", maxPoints = 1, tree = 0, column = 2, row = 9, prerequisite = 18)
    public int getMutilate() {
        return data[23];
    }

    public void setMutilate(int value) {
        data[23] = value;
    }

    @TalentDataAttribute(index = 24, name = "Turn the Tables", maxPoints = 3, tree = 0, column = 3, row = 9, prerequisite = -1)
    public int getTurnTheTables() {
        return data[24];
    }

    public void setTurnTheTables(int value) {
        data[24] = value;
    }

    @TalentDataAttribute(index = 25, name = "Cut to the Chase", maxPoints = 5, tree = 0, column = 2, row = 10, prerequisite = -1)
    public int getCutToTheChase() {
        return data[25];
    }

    public void setCutToTheChase(int value) {
        data[25] = value;
    }

    @TalentDataAttribute(index = 26, name = "Hunger For Blood", maxPoints = 1, tree = 0, column = 2, row = 11, prerequisite = -1)
    public int getHungerForBlood() {
        return data[26];
    }

    public void setHungerForBlood(int value) {
        data[26] = value;
    }

    @TalentDataAttribute(index = 27, name = "Improved Gouge", maxPoints = 3, tree = 1, column = 1, row = 1, prerequisite = -1)
    public int getImprovedGouge() {
        return data[27];
    }

    public void setImprovedGouge(int value) {
        data[27] = value;
    }

    @TalentDataAttribute(index = 28, name = "Improved Sinister Strike", maxPoints = 2, tree = 1, column = 2, row = 1, prerequisite = -1)
    public int getImprovedSinisterStrike() {
        return data[28];
    }

    public void setImprovedSinisterStrike(int value) {
        data[28] = value;
    }

    @TalentDataAttribute(index = 29, name = "Dual Wield Specialization", maxPoints = 5, tree = 1, column = 3, row = 1, prerequisite = -1)
    public int getDualWieldSpecialization() {
        return data[29];
    }

    public void setDualWieldSpecialization(int value) {
        data[29] = value;
    }

    @TalentDataAttribute(index = 30, name = "Improved Slice and Dice", maxPoints = 2, tree = 1, column = 1, row = 2, prerequisite = -1)
    public int getImprovedSliceAndDice() {
        return data[30];
    }

    public void setImprovedSliceAndDice(int value) {
        data[30] = value;
    }

    @TalentDataAttribute(index = 31, name = "Deflection", maxPoints = 3, tree = 1, column = 2, row = 2, prerequisite = -1)
    public int getDeflection() {
        return data[31];
    }

    public void setDeflection(int value) {
        data[31] = value;
    }

    @TalentDataAttribute(index = 32, name = "Precision", maxPoints = 5, tree = 1, column = 4, row = 2, prerequisite = -1)
    public int getPrecision() {
        return data[32];
    }

    public void setPrecision(int value) {
        data[32] = value;
    }

    @TalentDataAttribute(index = 33, name = "Endurance", maxPoints = 2, tree = 1, column = 1, row = 3, prerequisite = -1)
    public int getEndurance() {
        return data[33];
    }

    public void setEndurance(int value) {
        data[33] = value;
    }

    @TalentDataAttribute(index = 34, name = "Riposte", maxPoints = 1, tree = 1, column = 2, row = 3, prerequisite = 31)
    public int getRiposte() {
        return data[34];
    }

    public void setRiposte(int value) {
        data[34] = value;
    }

    @TalentDataAttribute(index = 35, name = "Close Quarters Combat", maxPoints = 5, tree = 1, column = 3, row = 3, prerequisite = 29)
    public int getCloseQuartersCombat() {
        return data[35];
    }

    public void setCloseQuartersCombat(int value) {
        data[35] = value;
    }

    @TalentDataAttribute(index = 36, name = "Improved Kick", maxPoints = 2, tree = 1, column = 1, row = 4, prerequisite = -1)
    public int getImprovedKick() {
        return data[36];
    }

    public void setImprovedKick(int value) {
        data[36] = value;
    }

    @TalentDataAttribute(index = 37, name = "Improved Sprint", maxPoints = 2, tree = 1, column = 2, row = 4, prerequisite = -1)
    public int getImprovedSprint() {
        return data[37];
    }

    public void setImprovedSprint(int value) {
        data[37] = value;
    }

    @TalentDataAttribute(index = 38, name = "Lightning Reflexes", maxPoints = 3, tree = 1, column = 3, row = 4, prerequisite = -1)
    public int getLightningReflexes() {
        return data[38];
    }

    public void setLightningReflexes(int value) {
        data[38] = value;
    }

    @TalentDataAttribute(index = 39, name = "Aggression", maxPoints = 5, tree = 1, column = 4, row = 4, prerequisite = -1)
    public int getAggression() {
        return data[39];
    }

    public void setAggression(int value) {
        data[39] = value;
    }

    @TalentDataAttribute(index = 40, name = "Mace Specialization", maxPoints = 5, tree = 1, column = 1, row = 5, prerequisite = -1)
    public int getMaceSpecialization() {
        return data[40];
    }

    public void setMaceSpecialization(int value) {
        data[40] = value;
    }

    @TalentDataAttribute(index = 41, name = "Blade Flurry", maxPoints = 1, tree = 1, column = 2, row = 5, prerequisite = -1)
    public int getBladeFlurry() {
        return data[41];
    }

    public void setBladeFlurry(int value) {
        data[41] = value;
    }

    @TalentDataAttribute(index = 42, name = "Hack and Slash", maxPoints = 5, tree = 1, column = 3, row = 5, prerequisite = -1)
    public int getHackAndSlash() {
        return data[42];
    }

    public void setHackAndSlash(int value) {
        data[42] = value;
    }

    @TalentDataAttribute(index = 43, name = "Weapon Expertise", maxPoints = 2, tree = 1, column = 2, row = 6, prerequisite = 41)
    public int getWeaponExpertise() {
        return data[43];
    }

    public void setWeaponExpertise(int value) {
        data[43] = value;
    }

    @TalentDataAttribute(index = 44, name = "Blade Twisting", maxPoints = 2, tree = 1, column = 3, row = 6, prerequisite = -1)
    public int getBladeTwisting() {
        return data[44];
    }

    public void setBladeTwisting(int value) {
        data[44] = value;
    }

    @TalentDataAttribute(index = 45, name = "Vitality", maxPoints = 3, tree = 1, column = 1, row = 7, prerequisite = -1)
    public int getVitality() {
        return data[45];
    }

    public void setVitality(int value) {
        data[45] = value;
    }

    @TalentDataAttribute(index = 46, name = "Adrenaline Rush", maxPoints = 1, tree = 1, column = 2, row = 7, prerequisite = -1)
    public int getAdrenalineRush() {
        return data[46];
    }

    public void setAdrenalineRush(int value) {
        data[46] = value;
    }

    @TalentDataAttribute(index = 47, name = "Nerves of Steel", maxPoints = 2, tree = 1, column = 3, row = 7, prerequisite = -1)
    public int getNervesOfSteel() {
        return data[47];
    }

    public void setNervesOfSteel(int value) {
        data[47] = value;
    }

    @TalentDataAttribute(index = 48, name = "Throwing Specialization", maxPoints = 2, tree = 1, column = 1, row = 8, prerequisite = -1)
    public int getThrowingSpecialization() {
        return data[48];
    }

    public void setThrowingSpecialization(int value) {
        data[48] = value;
    }

    @TalentDataAttribute(index = 49, name = "Combat Potency", maxPoints = 5, tree = 1, column = 3, row = 8, prerequisite = -1)
    public int getCombatPotency() {
        return data[49];
    }

    public void setCombatPotency(int value) {
        data[49] = value;
    }

    @TalentDataAttribute(index = 50, name = "Unfair Advantage", maxPoints = 2, tree = 1, column = 1, row = 9, prerequisite = -1)
    public int getUnfairAdvantage() {
        return data[50];
    }

    public void setUnfairAdvantage(int value) {
        data[50] = value;
    }

    @TalentDataAttribute(index = 51, name = "Surprise Attacks", maxPoints = 1, tree = 1, column = 2, row = 9, prerequisite = 46)
    public int getSurpriseAttacks() {
        return data[51];
    }

    public void setSurpriseAttacks(int value) {
        data[51] = value;
    }

    @TalentDataAttribute(index = 52, name = "Savage Combat", maxPoints = 2, tree = 1, column = 3, row = 9, prerequisite = -1)
    public int getSavageCombat() {
        return data[52];
    }

    public void setSavageCombat(int value) {
        data[52] = value;
    }

    @TalentDataAttribute(index = 53, name = "Prey on the Weak", maxPoints = 5, tree = 1, column = 2, row = 10, prerequisite = -1)
    public int getPreyOnTheWeak() {
        return data[53];
    }

    public void setPreyOnTheWeak(int value) {
        data[53] = value;
    }

    @TalentDataAttribute(index = 54, name = "Killing Spree", maxPoints = 1, tree = 1, column = 2, row = 11, prerequisite = -1)
    public int getKillingSpree() {
        return data[54];
    }

    public void setKillingSpree(int value) {
        data[54] = value;
    }

    @TalentDataAttribute(index = 55, name = "Relentless Strikes", maxPoints = 5, tree = 2, column = 1, row = 1, prerequisite = -1)
    public int getRelentlessStrikes() {
        return data[55];
    }

    public void setRelentlessStrikes(int value) {
        data[55] = value;
    }

    @TalentDataAttribute(index = 56, name = "Master of Deception", maxPoints = 3, tree = 2, column = 2, row = 1, prerequisite = -1)
    public int getMasterOfDeception() {
        return data[56];
    }

    public void setMasterOfDeception(int value) {
        data[56] = value;
    }

    @TalentDataAttribute(index = 57, name = "Opportunity", maxPoints = 2, tree = 2, column = 3, row = 1, prerequisite = -1)
    public int getOpportunity() {
        return data[57];
    }

    public void setOpportunity(int value) {
        data[57] = value;
    }

    @TalentDataAttribute(index = 58, name = "Sleight of Hand", maxPoints = 2, tree = 2, column = 1, row = 2, prerequisite = -1)
    public int getSleightOfHand() {
        return data[58];
    }

    public void setSleightOfHand(int value) {
        data[58] = value;
    }

    @TalentDataAttribute(index = 59, name = "Dirty Tricks", maxPoints = 2, tree = 2, column = 2, row = 2, prerequisite = -1)
    public int getDirtyTricks() {
        return data[59];
    }

    public void setDirtyTricks(int value) {
        data[59] = value;
    }

    @TalentDataAttribute(index = 60, name = "Camouflage", maxPoints = 3, tree = 2, column = 3, row = 2, prerequisite = -1)
    public int getCamouflage() {
        return data[60];
    }

    public void setCamouflage(int value) {
        data[60] = value;
    }

    @TalentDataAttribute(index = 61, name = "Elusiveness", maxPoints = 2, tree = 2, column = 1, row = 3, prerequisite = -1)
    public int getElusiveness() {
        return data[61];
    }

    public void setElusiveness(int value) {
        data[61] = value;
    }

    @TalentDataAttribute(index = 62, name = "Ghostly Strike", maxPoints = 1, tree = 2, column = 2, row = 3, prerequisite = -1)
    public int getGhostlyStrike() {
        return data[62];
    }

    public void setGhostlyStrike(int value) {
        data[62] = value;
    }

    @TalentDataAttribute(index = 63, name = "Serrated Blades", maxPoints = 3, tree = 2, column = 3, row = 3, prerequisite = -1)
    public int getSerratedBlades() {
        return data[63];
    }

    public void setSerratedBlades(int value) {
        data[63] = value;
    }

    @TalentDataAttribute(index = 64, name = "Setup", maxPoints = 3, tree = 2, column = 1, row = 4, prerequisite = -1)
    public int getSetup() {
        return data[64];
    }

    public void setSetup(int value) {
        data[64] = value;
    }

    @TalentDataAttribute(index = 65, name = "Initiative", maxPoints = 3, tree = 2, column = 2, row = 4, prerequisite = -1)
    public int getInitiative() {
        return data[65];
    }

    public void setInitiative(int value) {
        data[65] = value;
    }

    @TalentDataAttribute(index = 66, name = "Improved Ambush", maxPoints = 2, tree = 2, column = 3, row = 4, prerequisite = -1)
    public int getImprovedAmbush() {
        return data[66];
    }

    public void setImprovedAmbush(int value) {
        data[66] = value;
    }

    @TalentDataAttribute(index = 67, name = "Heightened Senses", maxPoints = 2, tree = 2, column = 1, row = 5, prerequisite = -1)
    public int getHeightenedSenses() {
        return data[67];
    }

    public void setHeightenedSenses(int value) {
        data[67] = value;
    }

    @TalentDataAttribute(index = 68, name = "Preparation", maxPoints = 1, tree = 2, column = 2, row = 5, prerequisite = -1)
    public int getPreparation() {
        return data[68];
    }

    public void setPreparation(int value) {
        data[68] = value;
    }

    @TalentDataAttribute(index = 69, name = "Dirty Deeds", maxPoints = 2, tree = 2, column = 3, row = 5, prerequisite = -1)
    public int getDirtyDeeds() {
        return data[69];
    }

    public void setDirtyDeeds(int value) {
        data[69] = value;
    }

    @TalentDataAttribute(index = 70, name = "Hemorrhage", maxPoints = 1, tree = 2, column = 4, row = 5, prerequisite = 63)
    public int getHemorrhage() {
        return data[70];
    }

    public void setHemorrhage(int value) {
        data[70] = value;
    }

    @TalentDataAttribute(index = 71, name = "Master of Subtlety", maxPoints = 3, tree = 2, column = 1, row = 6, prerequisite = -1)
    public int getMasterOfSubtlety() {
        return data[71];
    }

    public void setMasterOfSubtlety(int value) {
        data[71] = value;
    }

    @TalentDataAttribute(index = 72, name = "Deadliness", maxPoints = 5, tree = 2, column = 3, row = 6, prerequisite = -1)
    public int getDeadliness() {
        return data[72];
    }

    public void setDeadliness(int value) {
        data[72] = value;
    }

    @TalentDataAttribute(index = 73, name = "Enveloping Shadows", maxPoints = 3, tree = 2, column = 1, row = 7, prerequisite = -1)
    public int getEnvelopingShadows() {
        return data[73];
    }

    public void setEnvelopingShadows(int value) {
        data[73] = value;
    }

    @TalentDataAttribute(index = 74, name = "Premeditation", maxPoints = 1, tree = 2, column = 2, row = 7, prerequisite = 68)
    public int getPremeditation() {
        return data[74];
    }

    public void setPremeditation(int value) {
        data[74] = value;
    }

    @TalentDataAttribute(index = 75, name = "Cheat Death", maxPoints = 3, tree = 2, column = 3, row = 7, prerequisite = -1)
    public int getCheatDeath() {
        return data[75];
    }

    public void setCheatDeath(int value) {
        data[75] = value;
    }

    @TalentDataAttribute(index = 76, name = "Sinister Calling", maxPoints = 5, tree = 2, column = 2, row = 8, prerequisite = 74)
    public int getSinisterCalling() {
        return data[76];
    }

    public void setSinisterCalling(int value) {
        data[76] = value;
    }

    @TalentDataAttribute(index = 77, name = "Waylay", maxPoints = 2, tree = 2, column = 3, row = 8, prerequisite = -1)
    public int getWaylay() {
        return data[77];
    }

    public void setWaylay(int value) {
        data[77] = value;
    }

    @TalentDataAttribute(index = 78, name = "Honor Among Thieves", maxPoints = 3, tree = 2, column = 1, row = 9, prerequisite = -1)
    public int getHonorAmongThieves() {
        return data[78];
    }

    public void setHonorAmongThieves(int value) {
        data[78] = value;
    }

    @TalentDataAttribute(index = 79, name = "Shadowstep", maxPoints = 1, tree = 2, column = 2, row = 9, prerequisite = -1)
    public int getShadowstep() {
        return data[79];
    }

    public void setShadowstep(int value) {
        data[79] = value;
    }

    @TalentDataAttribute(index = 80, name = "Filthy Tricks", maxPoints = 2, tree = 2, column = 3, row = 9, prerequisite = -1)
    public int getFilthyTricks() {
        return data[80];
    }

    public void setFilthyTricks(int value) {
        data[80] = value;
    }

    @TalentDataAttribute(index = 81, name = "Slaughter from the Shadows", maxPoints = 5, tree = 2, column = 2, row = 10, prerequisite = -1)
    public int getSlaughterFromTheShadows() {
        return data[81];
    }

    public void setSlaughterFromTheShadows(int value) {
        data[81] = value;
    }

    @TalentDataAttribute(index = 82, name = "Shadow Dance", maxPoints = 1, tree = 2, column = 2, row = 11, prerequisite = -1)
    public int getShadowDance() {
        return data[82];
    }

    public void setShadowDance(int value) {
        data[82] = value;
    }

    @GlyphDataAttribute(index = 0, name = "Glyph of Backstab", major = true)
    public boolean isGlyphOfBackstab() {
        return glyphData[0];
    }

    public void setGlyphOfBackstab(boolean value) {
        glyphData[0] = value;
    }

    @GlyphDataAttribute(index = 1, name = "Glyph of Eviscerate", major = true)
    public boolean isGlyphOfEviscerate() {
        return glyphData[1];
    }

    public void setGlyphOfEviscerate(boolean value) {
        glyphData[1] = value;
    }

    @GlyphDataAttribute(index = 2, name = "Glyph of Mutilate", major = true)
    public boolean isGlyphOfMutilate() {
        return glyphData[2];
    }

    public void setGlyphOfMutilate(boolean value) {
        glyphData[2] = value;
    }

    @GlyphDataAttribute(index = 3, name = "Glyph of Hunger For Blood", major = true)
    public boolean isGlyphOfHungerforBlood() {
        return glyphData[3];
    }

    public void setGlyphOfHungerforBlood(boolean value) {
        glyphData[3] = value;
    }

    @GlyphDataAttribute(index = 4, name = "Glyph of Killing Spree", major = true)
    public boolean isGlyphOfKillingSpree() {
        return glyphData[4];
    }

    public void setGlyphOfKillingSpree(boolean value) {
        glyphData[4] = value;
    }

    @GlyphDataAttribute(index = 5, name = "Glyph of Vigor", major = true)
    public boolean isGlyphOfVigor() {
        return glyphData[5];
    }

    public void setGlyphOfVigor(boolean value) {
        glyphData[5] = value;
    }

    @GlyphDataAttribute(index = 6, name = "Glyph of Fan of Knives", major = true)
    public boolean isGlyphOfFanOfKnives() {
        return glyphData[6];
    }

    public void setGlyphOfFanOfKnives(boolean value) {
        glyphData[6] = value;
    }

    @GlyphDataAttribute(index = 7, name = "Glyph of Expose Armor", major = true)
    public boolean isGlyphOfExposeArmor() {
        return glyphData[7];
    }

    public void setGlyphOfExposeArmor(boolean value) {
        glyphData[7] = value;
    }

    @GlyphDataAttribute(index = 8, name = "Glyph of Sinister Strike", major = true)
    public boolean isGlyphOfSinisterStrike() {
        return glyphData[8];
    }

    public void setGlyphOfSinisterStrike(boolean value) {
        glyphData[8] = value;
    }

    @GlyphDataAttribute(index = 9, name = "Glyph of Slice and Dice", major = true)
    public boolean isGlyphOfSliceandDice() {
        return glyphData[9];
    }

    public void setGlyphOfSliceandDice(boolean value) {
        glyphData[9] = value;
    }

    @GlyphDataAttribute(index = 10, name = "Glyph of Feint", major = true)
    public boolean isGlyphOfFeint() {
        return glyphData[10];
    }

    public void setGlyphOfFeint(boolean value) {
        glyphData[10] = value;
    }

    @GlyphDataAttribute(index = 11, name = "Glyph of Ghostly Strike", major = true)
    public boolean isGlyphOfGhostlyStrike() {
        return glyphData[11];
    }

    public void setGlyphOfGhostlyStrike(boolean value) {
        glyphData[11] = value;
    }

    @GlyphDataAttribute(index = 12, name = "Glyph of Rupture", major = true)
    public boolean isGlyphOfRupture() {
        return glyphData[12];
    }

    public void setGlyphOfRupture(boolean value) {
        glyphData[12] = value;
    }

    @GlyphDataAttribute(index = 13, name = "Glyph of Blade Flurry", major = true)
    public boolean isGlyphOfBladeFlurry() {
        return glyphData[13];
    }

    public void setGlyphOfBladeFlurry(boolean value) {
        glyphData[13] = value;
    }

    @GlyphDataAttribute(index = 14, name = "Glyph of Adrenaline Rush", major = true)
    public boolean isGlyphOfAdrenalineRush() {
        return glyphData[14];
    }

    public void setGlyphOfAdrenalineRush(boolean value) {
        glyphData[14] = value;
    }

    @GlyphDataAttribute(index = 15, name = "Glyph of Evasion", major = true)
    public boolean isGlyphOfEvasion() {
        return glyphData[15];
    }

    public void setGlyphOfEvasion(boolean value) {
        glyphData[15] = value;
    }

    @GlyphDataAttribute(index = 16, name = "Glyph of Garrote", major = true)
    public boolean isGlyphOfGarrote() {
        return glyphData[16];
    }

    public void setGlyphOfGarrote(boolean value) {
        glyphData[16] = value;
    }

    @GlyphDataAttribute(index = 17, name = "Glyph of Gouge", major = true)
    public boolean isGlyphOfGouge() {
        return glyphData[17];
    }

    public void setGlyphOfGouge(boolean value) {
        glyphData[17] = value;
    }

    @GlyphDataAttribute(index = 18, name = "Glyph of Sap", major = true)
    public boolean isGlyphOfSap() {
        return glyphData[18];
    }

    public void setGlyphOfSap(boolean value) {
        glyphData[18] = value;
    }

    @GlyphDataAttribute(index = 19, name = "Glyph of Sprint", major = true)
    public boolean isGlyphOfSprint() {
        return glyphData[19];
    }

    public void setGlyphOfSprint(boolean value) {
        glyphData[19] = value;
    }

    @GlyphDataAttribute(index = 20, name = "Glyph of Ambush", major = true)
    public boolean isGlyphOfAmbush() {
        return glyphData[20];
    }

    public void setGlyphOfAmbush(boolean value) {
        glyphData[20] = value;
    }

    @GlyphDataAttribute(index = 21, name = "Glyph of Crippling Poison", major = true)
    public boolean isGlyphOfCripplingPoison() {
        return glyphData[21];
    }

    public void setGlyphOfCripplingPoison(boolean value) {
        glyphData[21] = value;
    }

    @GlyphDataAttribute(index = 22, name = "Glyph of Hemorrhage", major = true)
    public boolean isGlyphOfHemorrhage() {
        return glyphData[22];
    }

    public void setGlyphOfHemorrhage(boolean value) {
        glyphData[22] = value;
    }

    @GlyphDataAttribute(index = 23, name = "Glyph of Preparation", major = true)
    public boolean isGlyphOfPreparation() {
        return glyphData[23];
    }

    public void setGlyphOfPreparation(boolean value) {
        glyphData[23] = value;
    }

    @GlyphDataAttribute(index = 24, name = "Glyph of Shadow Dance", major = true)
    public boolean isGlyphOfShadowDance() {
        return glyphData[24];
    }

    public void setGlyphOfShadowDance(boolean value) {
        glyphData[24] = value;
    }

    @GlyphDataAttribute(index = 25, name = "Glyph of Deadly Throw", major = true)
    public boolean isGlyphOfDeadlyThrow() {
        return glyphData[25];
    }

    public void setGlyphOfDeadlyThrow(boolean value) {
        glyphData[25] = value;
    }

    @GlyphDataAttribute(index = 26, name = "Glyph of Cloak of Shadows", major = true)
    public boolean isGlyphOfCloakOfShadows() {
        return glyphData[26];
    }

    public void setGlyphOfCloakOfShadows(boolean value) {
        glyphData[26] = value;
    }

    @GlyphDataAttribute(index = 27, name = "Glyph of Tricks of the Trade", major = true)
    public boolean isGlyphOfTricksOfTheTrade() {
        return glyphData[27];
    }

    public void setGlyphOfTricksOfTheTrade(boolean value) {
        glyphData[27] = value;
    }

    @GlyphDataAttribute(index = 28, name = "Glyph of Blurred Speed", major = false)
    public boolean isGlyphOfBlurredSpeed() {
        return glyphData[28];
    }

    public void setGlyphOfBlurredSpeed(boolean value) {
        glyphData[28] = value;
    }

    @GlyphDataAttribute(index = 29, name = "Glyph of Pick Pocket", major = false)
    public boolean isGlyphOfPickPocket() {
        return glyphData[29];
    }

    public void setGlyphOfPickPocket(boolean value) {
        glyphData[29] = value;
    }

    @GlyphDataAttribute(index = 30, name = "Glyph of Pick Lock", major = false)
    public boolean isGlyphOfPickLock() {
        return glyphData[30];
    }

    public void setGlyphOfPickLock(boolean value) {
        glyphData[30] = value;
    }

    @GlyphDataAttribute(index = 31, name = "Glyph of Distract", major = false)
    public boolean isGlyphOfDistrict() {
        return glyphData[31];
    }

    public void setGlyphOfDistrict(boolean value) {
        glyphData[31] = value;
    }

    @GlyphDataAttribute(index = 32, name = "Glyph of Vanish", major = false)
    public boolean isGlyphOfVanish() {
        return glyphData[32];
    }

    public void setGlyphOfVanish(boolean value) {
        glyphData[32] = value;
    }

    @GlyphDataAttribute(index = 33, name = "Glyph of Safe Fall", major = false)
    public boolean isGlyphOfSafeFall() {
        return glyphData[33];
    }

    public void setGlyphOfSafeFall(boolean value) {
        glyphData[33] = value;
    }

}
