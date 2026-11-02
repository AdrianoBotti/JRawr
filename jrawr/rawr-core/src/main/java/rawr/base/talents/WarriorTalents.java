// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassTalents.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassGlyphs.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.talents;

/**
 * The warrior's talent and glyph allocation: the Arms, Fury and Protection trees plus 34 glyph
 * slots.
 *
 * <p>{@code data} has 85 rank slots, contiguous per tree: Arms 0-30 (tree 0), Fury 31-57 (tree 1),
 * Protection 58-84 (tree 2); {@link #TreeNames} fixes that order. Both warrior models (dpswarr and
 * protwarr) read this allocation; the dpswarr off-hand logic checks {@link #getTitansGrip()} (Fury,
 * index 57), and the first talent of each tree is {@link #getImprovedHeroicStrike()},
 * {@link #getArmoredToTheTeeth()} and {@link #getImprovedBloodrage()} respectively.</p>
 *
 * <p>{@code glyphData} has 34 flags: 27 major glyphs (0-26) then 7 minor glyphs (27-33). The glyph
 * getters use the {@code isGlyphOfXxx()} spelling, e.g. {@link #isGlyphOfExecution()} or
 * {@link #isGlyphOfSweepingStrikes()}. Construction, cloning and the live-array accessors follow
 * {@link TalentsBase}. See {@link WarriorTalentsCata} for the unused Cataclysm variant of this
 * table.</p>
 */
public class WarriorTalents extends TalentsBase {
    public static final String[] TreeNames = new String[] {
        "Arms",
        "Fury",
        "Protection",
    };

    private final int[] data = new int[85];
    private final boolean[] glyphData = new boolean[34];

    public WarriorTalents() {
    }

    public WarriorTalents(String talents) {
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

    public WarriorTalents clone() {
        WarriorTalents clone = new WarriorTalents();
        System.arraycopy(data, 0, clone.data, 0, data.length);
        System.arraycopy(glyphData, 0, clone.glyphData, 0, glyphData.length);
        return clone;
    }

    @TalentDataAttribute(index = 0, name = "Improved Heroic Strike", maxPoints = 3, tree = 0, column = 1, row = 1, prerequisite = -1)
    public int getImprovedHeroicStrike() {
        return data[0];
    }

    public void setImprovedHeroicStrike(int value) {
        data[0] = value;
    }

    @TalentDataAttribute(index = 1, name = "Deflection", maxPoints = 5, tree = 0, column = 2, row = 1, prerequisite = -1)
    public int getDeflection() {
        return data[1];
    }

    public void setDeflection(int value) {
        data[1] = value;
    }

    @TalentDataAttribute(index = 2, name = "Improved Rend", maxPoints = 2, tree = 0, column = 3, row = 1, prerequisite = -1)
    public int getImprovedRend() {
        return data[2];
    }

    public void setImprovedRend(int value) {
        data[2] = value;
    }

    @TalentDataAttribute(index = 3, name = "Improved Charge", maxPoints = 2, tree = 0, column = 1, row = 2, prerequisite = -1)
    public int getImprovedCharge() {
        return data[3];
    }

    public void setImprovedCharge(int value) {
        data[3] = value;
    }

    @TalentDataAttribute(index = 4, name = "Iron Will", maxPoints = 3, tree = 0, column = 2, row = 2, prerequisite = -1)
    public int getIronWill() {
        return data[4];
    }

    public void setIronWill(int value) {
        data[4] = value;
    }

    @TalentDataAttribute(index = 5, name = "Tactical Mastery", maxPoints = 3, tree = 0, column = 3, row = 2, prerequisite = -1)
    public int getTacticalMastery() {
        return data[5];
    }

    public void setTacticalMastery(int value) {
        data[5] = value;
    }

    @TalentDataAttribute(index = 6, name = "Improved Overpower", maxPoints = 2, tree = 0, column = 1, row = 3, prerequisite = -1)
    public int getImprovedOverpower() {
        return data[6];
    }

    public void setImprovedOverpower(int value) {
        data[6] = value;
    }

    @TalentDataAttribute(index = 7, name = "Anger Management", maxPoints = 1, tree = 0, column = 2, row = 3, prerequisite = -1)
    public int getAngerManagement() {
        return data[7];
    }

    public void setAngerManagement(int value) {
        data[7] = value;
    }

    @TalentDataAttribute(index = 8, name = "Impale", maxPoints = 2, tree = 0, column = 3, row = 3, prerequisite = -1)
    public int getImpale() {
        return data[8];
    }

    public void setImpale(int value) {
        data[8] = value;
    }

    @TalentDataAttribute(index = 9, name = "Deep Wounds", maxPoints = 3, tree = 0, column = 4, row = 3, prerequisite = 8)
    public int getDeepWounds() {
        return data[9];
    }

    public void setDeepWounds(int value) {
        data[9] = value;
    }

    @TalentDataAttribute(index = 10, name = "Two-Handed Weapon Specialization", maxPoints = 3, tree = 0, column = 2, row = 4, prerequisite = -1)
    public int getTwoHandedWeaponSpecialization() {
        return data[10];
    }

    public void setTwoHandedWeaponSpecialization(int value) {
        data[10] = value;
    }

    @TalentDataAttribute(index = 11, name = "Taste for Blood", maxPoints = 3, tree = 0, column = 3, row = 4, prerequisite = -1)
    public int getTasteForBlood() {
        return data[11];
    }

    public void setTasteForBlood(int value) {
        data[11] = value;
    }

    @TalentDataAttribute(index = 12, name = "Poleaxe Specialization", maxPoints = 5, tree = 0, column = 1, row = 5, prerequisite = -1)
    public int getPoleaxeSpecialization() {
        return data[12];
    }

    public void setPoleaxeSpecialization(int value) {
        data[12] = value;
    }

    @TalentDataAttribute(index = 13, name = "Sweeping Strikes", maxPoints = 1, tree = 0, column = 2, row = 5, prerequisite = -1)
    public int getSweepingStrikes() {
        return data[13];
    }

    public void setSweepingStrikes(int value) {
        data[13] = value;
    }

    @TalentDataAttribute(index = 14, name = "Mace Specialization", maxPoints = 5, tree = 0, column = 3, row = 5, prerequisite = -1)
    public int getMaceSpecialization() {
        return data[14];
    }

    public void setMaceSpecialization(int value) {
        data[14] = value;
    }

    @TalentDataAttribute(index = 15, name = "Sword Specialization", maxPoints = 5, tree = 0, column = 4, row = 5, prerequisite = -1)
    public int getSwordSpecialization() {
        return data[15];
    }

    public void setSwordSpecialization(int value) {
        data[15] = value;
    }

    @TalentDataAttribute(index = 16, name = "Weapon Mastery", maxPoints = 2, tree = 0, column = 1, row = 6, prerequisite = -1)
    public int getWeaponMastery() {
        return data[16];
    }

    public void setWeaponMastery(int value) {
        data[16] = value;
    }

    @TalentDataAttribute(index = 17, name = "Improved Hamstring", maxPoints = 3, tree = 0, column = 3, row = 6, prerequisite = -1)
    public int getImprovedHamstring() {
        return data[17];
    }

    public void setImprovedHamstring(int value) {
        data[17] = value;
    }

    @TalentDataAttribute(index = 18, name = "Trauma", maxPoints = 2, tree = 0, column = 4, row = 6, prerequisite = -1)
    public int getTrauma() {
        return data[18];
    }

    public void setTrauma(int value) {
        data[18] = value;
    }

    @TalentDataAttribute(index = 19, name = "Second Wind", maxPoints = 2, tree = 0, column = 1, row = 7, prerequisite = -1)
    public int getSecondWind() {
        return data[19];
    }

    public void setSecondWind(int value) {
        data[19] = value;
    }

    @TalentDataAttribute(index = 20, name = "Mortal Strike", maxPoints = 1, tree = 0, column = 2, row = 7, prerequisite = 13)
    public int getMortalStrike() {
        return data[20];
    }

    public void setMortalStrike(int value) {
        data[20] = value;
    }

    @TalentDataAttribute(index = 21, name = "Strength of Arms", maxPoints = 2, tree = 0, column = 3, row = 7, prerequisite = -1)
    public int getStrengthOfArms() {
        return data[21];
    }

    public void setStrengthOfArms(int value) {
        data[21] = value;
    }

    @TalentDataAttribute(index = 22, name = "Improved Slam", maxPoints = 2, tree = 0, column = 4, row = 7, prerequisite = -1)
    public int getImprovedSlam() {
        return data[22];
    }

    public void setImprovedSlam(int value) {
        data[22] = value;
    }

    @TalentDataAttribute(index = 23, name = "Juggernaut", maxPoints = 1, tree = 0, column = 1, row = 8, prerequisite = -1)
    public int getJuggernaut() {
        return data[23];
    }

    public void setJuggernaut(int value) {
        data[23] = value;
    }

    @TalentDataAttribute(index = 24, name = "Improved Mortal Strike", maxPoints = 3, tree = 0, column = 2, row = 8, prerequisite = 20)
    public int getImprovedMortalStrike() {
        return data[24];
    }

    public void setImprovedMortalStrike(int value) {
        data[24] = value;
    }

    @TalentDataAttribute(index = 25, name = "Unrelenting Assault", maxPoints = 2, tree = 0, column = 3, row = 8, prerequisite = -1)
    public int getUnrelentingAssault() {
        return data[25];
    }

    public void setUnrelentingAssault(int value) {
        data[25] = value;
    }

    @TalentDataAttribute(index = 26, name = "Sudden Death", maxPoints = 3, tree = 0, column = 1, row = 9, prerequisite = -1)
    public int getSuddenDeath() {
        return data[26];
    }

    public void setSuddenDeath(int value) {
        data[26] = value;
    }

    @TalentDataAttribute(index = 27, name = "Endless Rage", maxPoints = 1, tree = 0, column = 2, row = 9, prerequisite = -1)
    public int getEndlessRage() {
        return data[27];
    }

    public void setEndlessRage(int value) {
        data[27] = value;
    }

    @TalentDataAttribute(index = 28, name = "Blood Frenzy", maxPoints = 2, tree = 0, column = 3, row = 9, prerequisite = -1)
    public int getBloodFrenzy() {
        return data[28];
    }

    public void setBloodFrenzy(int value) {
        data[28] = value;
    }

    @TalentDataAttribute(index = 29, name = "Wrecking Crew", maxPoints = 5, tree = 0, column = 2, row = 10, prerequisite = -1)
    public int getWreckingCrew() {
        return data[29];
    }

    public void setWreckingCrew(int value) {
        data[29] = value;
    }

    @TalentDataAttribute(index = 30, name = "Bladestorm", maxPoints = 1, tree = 0, column = 2, row = 11, prerequisite = -1)
    public int getBladestorm() {
        return data[30];
    }

    public void setBladestorm(int value) {
        data[30] = value;
    }

    @TalentDataAttribute(index = 31, name = "Armored to the Teeth", maxPoints = 3, tree = 1, column = 1, row = 1, prerequisite = -1)
    public int getArmoredToTheTeeth() {
        return data[31];
    }

    public void setArmoredToTheTeeth(int value) {
        data[31] = value;
    }

    @TalentDataAttribute(index = 32, name = "Booming Voice", maxPoints = 2, tree = 1, column = 2, row = 1, prerequisite = -1)
    public int getBoomingVoice() {
        return data[32];
    }

    public void setBoomingVoice(int value) {
        data[32] = value;
    }

    @TalentDataAttribute(index = 33, name = "Cruelty", maxPoints = 5, tree = 1, column = 3, row = 1, prerequisite = -1)
    public int getCruelty() {
        return data[33];
    }

    public void setCruelty(int value) {
        data[33] = value;
    }

    @TalentDataAttribute(index = 34, name = "Improved Demoralizing Shout", maxPoints = 5, tree = 1, column = 2, row = 2, prerequisite = -1)
    public int getImprovedDemoralizingShout() {
        return data[34];
    }

    public void setImprovedDemoralizingShout(int value) {
        data[34] = value;
    }

    @TalentDataAttribute(index = 35, name = "Unbridled Wrath", maxPoints = 5, tree = 1, column = 3, row = 2, prerequisite = -1)
    public int getUnbridledWrath() {
        return data[35];
    }

    public void setUnbridledWrath(int value) {
        data[35] = value;
    }

    @TalentDataAttribute(index = 36, name = "Improved Cleave", maxPoints = 3, tree = 1, column = 1, row = 3, prerequisite = -1)
    public int getImprovedCleave() {
        return data[36];
    }

    public void setImprovedCleave(int value) {
        data[36] = value;
    }

    @TalentDataAttribute(index = 37, name = "Piercing Howl", maxPoints = 1, tree = 1, column = 2, row = 3, prerequisite = -1)
    public int getPiercingHowl() {
        return data[37];
    }

    public void setPiercingHowl(int value) {
        data[37] = value;
    }

    @TalentDataAttribute(index = 38, name = "Blood Craze", maxPoints = 3, tree = 1, column = 3, row = 3, prerequisite = -1)
    public int getBloodCraze() {
        return data[38];
    }

    public void setBloodCraze(int value) {
        data[38] = value;
    }

    @TalentDataAttribute(index = 39, name = "Commanding Presence", maxPoints = 5, tree = 1, column = 4, row = 3, prerequisite = -1)
    public int getCommandingPresence() {
        return data[39];
    }

    public void setCommandingPresence(int value) {
        data[39] = value;
    }

    @TalentDataAttribute(index = 40, name = "Dual Wield Specialization", maxPoints = 5, tree = 1, column = 1, row = 4, prerequisite = -1)
    public int getDualWieldSpecialization() {
        return data[40];
    }

    public void setDualWieldSpecialization(int value) {
        data[40] = value;
    }

    @TalentDataAttribute(index = 41, name = "Improved Execute", maxPoints = 2, tree = 1, column = 2, row = 4, prerequisite = -1)
    public int getImprovedExecute() {
        return data[41];
    }

    public void setImprovedExecute(int value) {
        data[41] = value;
    }

    @TalentDataAttribute(index = 42, name = "Enrage", maxPoints = 5, tree = 1, column = 3, row = 4, prerequisite = -1)
    public int getEnrage() {
        return data[42];
    }

    public void setEnrage(int value) {
        data[42] = value;
    }

    @TalentDataAttribute(index = 43, name = "Precision", maxPoints = 3, tree = 1, column = 1, row = 5, prerequisite = -1)
    public int getPrecision() {
        return data[43];
    }

    public void setPrecision(int value) {
        data[43] = value;
    }

    @TalentDataAttribute(index = 44, name = "Death Wish", maxPoints = 1, tree = 1, column = 2, row = 5, prerequisite = -1)
    public int getDeathWish() {
        return data[44];
    }

    public void setDeathWish(int value) {
        data[44] = value;
    }

    @TalentDataAttribute(index = 45, name = "Improved Intercept", maxPoints = 2, tree = 1, column = 3, row = 5, prerequisite = -1)
    public int getImprovedIntercept() {
        return data[45];
    }

    public void setImprovedIntercept(int value) {
        data[45] = value;
    }

    @TalentDataAttribute(index = 46, name = "Improved Berserker Rage", maxPoints = 2, tree = 1, column = 1, row = 6, prerequisite = -1)
    public int getImprovedBerserkerRage() {
        return data[46];
    }

    public void setImprovedBerserkerRage(int value) {
        data[46] = value;
    }

    @TalentDataAttribute(index = 47, name = "Flurry", maxPoints = 5, tree = 1, column = 3, row = 6, prerequisite = -1)
    public int getFlurry() {
        return data[47];
    }

    public void setFlurry(int value) {
        data[47] = value;
    }

    @TalentDataAttribute(index = 48, name = "Intensify Rage", maxPoints = 3, tree = 1, column = 1, row = 7, prerequisite = -1)
    public int getIntensifyRage() {
        return data[48];
    }

    public void setIntensifyRage(int value) {
        data[48] = value;
    }

    @TalentDataAttribute(index = 49, name = "Bloodthirst", maxPoints = 1, tree = 1, column = 2, row = 7, prerequisite = 44)
    public int getBloodthirst() {
        return data[49];
    }

    public void setBloodthirst(int value) {
        data[49] = value;
    }

    @TalentDataAttribute(index = 50, name = "Improved Whirlwind", maxPoints = 2, tree = 1, column = 4, row = 7, prerequisite = -1)
    public int getImprovedWhirlwind() {
        return data[50];
    }

    public void setImprovedWhirlwind(int value) {
        data[50] = value;
    }

    @TalentDataAttribute(index = 51, name = "Furious Attacks", maxPoints = 2, tree = 1, column = 1, row = 8, prerequisite = -1)
    public int getFuriousAttacks() {
        return data[51];
    }

    public void setFuriousAttacks(int value) {
        data[51] = value;
    }

    @TalentDataAttribute(index = 52, name = "Improved Berserker Stance", maxPoints = 5, tree = 1, column = 4, row = 8, prerequisite = -1)
    public int getImprovedBerserkerStance() {
        return data[52];
    }

    public void setImprovedBerserkerStance(int value) {
        data[52] = value;
    }

    @TalentDataAttribute(index = 53, name = "Heroic Fury", maxPoints = 1, tree = 1, column = 1, row = 9, prerequisite = -1)
    public int getHeroicFury() {
        return data[53];
    }

    public void setHeroicFury(int value) {
        data[53] = value;
    }

    @TalentDataAttribute(index = 54, name = "Rampage", maxPoints = 1, tree = 1, column = 2, row = 9, prerequisite = 49)
    public int getRampage() {
        return data[54];
    }

    public void setRampage(int value) {
        data[54] = value;
    }

    @TalentDataAttribute(index = 55, name = "Bloodsurge", maxPoints = 3, tree = 1, column = 3, row = 9, prerequisite = 49)
    public int getBloodsurge() {
        return data[55];
    }

    public void setBloodsurge(int value) {
        data[55] = value;
    }

    @TalentDataAttribute(index = 56, name = "Unending Fury", maxPoints = 5, tree = 1, column = 2, row = 10, prerequisite = -1)
    public int getUnendingFury() {
        return data[56];
    }

    public void setUnendingFury(int value) {
        data[56] = value;
    }

    @TalentDataAttribute(index = 57, name = "Titan's Grip", maxPoints = 1, tree = 1, column = 2, row = 11, prerequisite = -1)
    public int getTitansGrip() {
        return data[57];
    }

    public void setTitansGrip(int value) {
        data[57] = value;
    }

    @TalentDataAttribute(index = 58, name = "Improved Bloodrage", maxPoints = 2, tree = 2, column = 1, row = 1, prerequisite = -1)
    public int getImprovedBloodrage() {
        return data[58];
    }

    public void setImprovedBloodrage(int value) {
        data[58] = value;
    }

    @TalentDataAttribute(index = 59, name = "Shield Specialization", maxPoints = 5, tree = 2, column = 2, row = 1, prerequisite = -1)
    public int getShieldSpecialization() {
        return data[59];
    }

    public void setShieldSpecialization(int value) {
        data[59] = value;
    }

    @TalentDataAttribute(index = 60, name = "Improved Thunder Clap", maxPoints = 3, tree = 2, column = 3, row = 1, prerequisite = -1)
    public int getImprovedThunderClap() {
        return data[60];
    }

    public void setImprovedThunderClap(int value) {
        data[60] = value;
    }

    @TalentDataAttribute(index = 61, name = "Incite", maxPoints = 3, tree = 2, column = 2, row = 2, prerequisite = -1)
    public int getIncite() {
        return data[61];
    }

    public void setIncite(int value) {
        data[61] = value;
    }

    @TalentDataAttribute(index = 62, name = "Anticipation", maxPoints = 5, tree = 2, column = 3, row = 2, prerequisite = -1)
    public int getAnticipation() {
        return data[62];
    }

    public void setAnticipation(int value) {
        data[62] = value;
    }

    @TalentDataAttribute(index = 63, name = "Last Stand", maxPoints = 1, tree = 2, column = 1, row = 3, prerequisite = -1)
    public int getLastStand() {
        return data[63];
    }

    public void setLastStand(int value) {
        data[63] = value;
    }

    @TalentDataAttribute(index = 64, name = "Improved Revenge", maxPoints = 2, tree = 2, column = 2, row = 3, prerequisite = -1)
    public int getImprovedRevenge() {
        return data[64];
    }

    public void setImprovedRevenge(int value) {
        data[64] = value;
    }

    @TalentDataAttribute(index = 65, name = "Shield Mastery", maxPoints = 2, tree = 2, column = 3, row = 3, prerequisite = -1)
    public int getShieldMastery() {
        return data[65];
    }

    public void setShieldMastery(int value) {
        data[65] = value;
    }

    @TalentDataAttribute(index = 66, name = "Toughness", maxPoints = 5, tree = 2, column = 4, row = 3, prerequisite = -1)
    public int getToughness() {
        return data[66];
    }

    public void setToughness(int value) {
        data[66] = value;
    }

    @TalentDataAttribute(index = 67, name = "Improved Spell Reflection", maxPoints = 2, tree = 2, column = 1, row = 4, prerequisite = -1)
    public int getImprovedSpellReflection() {
        return data[67];
    }

    public void setImprovedSpellReflection(int value) {
        data[67] = value;
    }

    @TalentDataAttribute(index = 68, name = "Improved Disarm", maxPoints = 2, tree = 2, column = 2, row = 4, prerequisite = -1)
    public int getImprovedDisarm() {
        return data[68];
    }

    public void setImprovedDisarm(int value) {
        data[68] = value;
    }

    @TalentDataAttribute(index = 69, name = "Puncture", maxPoints = 3, tree = 2, column = 3, row = 4, prerequisite = -1)
    public int getPuncture() {
        return data[69];
    }

    public void setPuncture(int value) {
        data[69] = value;
    }

    @TalentDataAttribute(index = 70, name = "Improved Disciplines", maxPoints = 2, tree = 2, column = 1, row = 5, prerequisite = -1)
    public int getImprovedDisciplines() {
        return data[70];
    }

    public void setImprovedDisciplines(int value) {
        data[70] = value;
    }

    @TalentDataAttribute(index = 71, name = "Concussion Blow", maxPoints = 1, tree = 2, column = 2, row = 5, prerequisite = -1)
    public int getConcussionBlow() {
        return data[71];
    }

    public void setConcussionBlow(int value) {
        data[71] = value;
    }

    @TalentDataAttribute(index = 72, name = "Gag Order", maxPoints = 2, tree = 2, column = 3, row = 5, prerequisite = -1)
    public int getGagOrder() {
        return data[72];
    }

    public void setGagOrder(int value) {
        data[72] = value;
    }

    @TalentDataAttribute(index = 73, name = "One-Handed Weapon Specialization", maxPoints = 5, tree = 2, column = 3, row = 6, prerequisite = -1)
    public int getOneHandedWeaponSpecialization() {
        return data[73];
    }

    public void setOneHandedWeaponSpecialization(int value) {
        data[73] = value;
    }

    @TalentDataAttribute(index = 74, name = "Improved Defensive Stance", maxPoints = 2, tree = 2, column = 1, row = 7, prerequisite = -1)
    public int getImprovedDefensiveStance() {
        return data[74];
    }

    public void setImprovedDefensiveStance(int value) {
        data[74] = value;
    }

    @TalentDataAttribute(index = 75, name = "Vigilance", maxPoints = 1, tree = 2, column = 2, row = 7, prerequisite = 71)
    public int getVigilance() {
        return data[75];
    }

    public void setVigilance(int value) {
        data[75] = value;
    }

    @TalentDataAttribute(index = 76, name = "Focused Rage", maxPoints = 3, tree = 2, column = 3, row = 7, prerequisite = -1)
    public int getFocusedRage() {
        return data[76];
    }

    public void setFocusedRage(int value) {
        data[76] = value;
    }

    @TalentDataAttribute(index = 77, name = "Vitality", maxPoints = 3, tree = 2, column = 2, row = 8, prerequisite = -1)
    public int getVitality() {
        return data[77];
    }

    public void setVitality(int value) {
        data[77] = value;
    }

    @TalentDataAttribute(index = 78, name = "Safeguard", maxPoints = 2, tree = 2, column = 3, row = 8, prerequisite = -1)
    public int getSafeguard() {
        return data[78];
    }

    public void setSafeguard(int value) {
        data[78] = value;
    }

    @TalentDataAttribute(index = 79, name = "Warbringer", maxPoints = 1, tree = 2, column = 1, row = 9, prerequisite = -1)
    public int getWarbringer() {
        return data[79];
    }

    public void setWarbringer(int value) {
        data[79] = value;
    }

    @TalentDataAttribute(index = 80, name = "Devastate", maxPoints = 1, tree = 2, column = 2, row = 9, prerequisite = -1)
    public int getDevastate() {
        return data[80];
    }

    public void setDevastate(int value) {
        data[80] = value;
    }

    @TalentDataAttribute(index = 81, name = "Critical Block", maxPoints = 3, tree = 2, column = 3, row = 9, prerequisite = -1)
    public int getCriticalBlock() {
        return data[81];
    }

    public void setCriticalBlock(int value) {
        data[81] = value;
    }

    @TalentDataAttribute(index = 82, name = "Sword and Board", maxPoints = 3, tree = 2, column = 2, row = 10, prerequisite = 80)
    public int getSwordAndBoard() {
        return data[82];
    }

    public void setSwordAndBoard(int value) {
        data[82] = value;
    }

    @TalentDataAttribute(index = 83, name = "Damage Shield", maxPoints = 2, tree = 2, column = 3, row = 10, prerequisite = -1)
    public int getDamageShield() {
        return data[83];
    }

    public void setDamageShield(int value) {
        data[83] = value;
    }

    @TalentDataAttribute(index = 84, name = "Shockwave", maxPoints = 1, tree = 2, column = 2, row = 11, prerequisite = -1)
    public int getShockwave() {
        return data[84];
    }

    public void setShockwave(int value) {
        data[84] = value;
    }

    @GlyphDataAttribute(index = 0, name = "Glyph of Barbaric Insults", major = true)
    public boolean isGlyphOfBarbaricInsults() {
        return glyphData[0];
    }

    public void setGlyphOfBarbaricInsults(boolean value) {
        glyphData[0] = value;
    }

    @GlyphDataAttribute(index = 1, name = "Glyph of Bladestorm", major = true)
    public boolean isGlyphOfBladestorm() {
        return glyphData[1];
    }

    public void setGlyphOfBladestorm(boolean value) {
        glyphData[1] = value;
    }

    @GlyphDataAttribute(index = 2, name = "Glyph of Blocking", major = true)
    public boolean isGlyphOfBlocking() {
        return glyphData[2];
    }

    public void setGlyphOfBlocking(boolean value) {
        glyphData[2] = value;
    }

    @GlyphDataAttribute(index = 3, name = "Glyph of Bloodthirst", major = true)
    public boolean isGlyphOfBloodthirst() {
        return glyphData[3];
    }

    public void setGlyphOfBloodthirst(boolean value) {
        glyphData[3] = value;
    }

    @GlyphDataAttribute(index = 4, name = "Glyph of Cleaving", major = true)
    public boolean isGlyphOfCleaving() {
        return glyphData[4];
    }

    public void setGlyphOfCleaving(boolean value) {
        glyphData[4] = value;
    }

    @GlyphDataAttribute(index = 5, name = "Glyph of Devastate", major = true)
    public boolean isGlyphOfDevastate() {
        return glyphData[5];
    }

    public void setGlyphOfDevastate(boolean value) {
        glyphData[5] = value;
    }

    @GlyphDataAttribute(index = 6, name = "Glyph of Enraged Regeneration", major = true)
    public boolean isGlyphOfEnragedRegeneration() {
        return glyphData[6];
    }

    public void setGlyphOfEnragedRegeneration(boolean value) {
        glyphData[6] = value;
    }

    @GlyphDataAttribute(index = 7, name = "Glyph of Execution", major = true)
    public boolean isGlyphOfExecution() {
        return glyphData[7];
    }

    public void setGlyphOfExecution(boolean value) {
        glyphData[7] = value;
    }

    @GlyphDataAttribute(index = 8, name = "Glyph of Hamstring", major = true)
    public boolean isGlyphOfHamstring() {
        return glyphData[8];
    }

    public void setGlyphOfHamstring(boolean value) {
        glyphData[8] = value;
    }

    @GlyphDataAttribute(index = 9, name = "Glyph of Heroic Strike", major = true)
    public boolean isGlyphOfHeroicStrike() {
        return glyphData[9];
    }

    public void setGlyphOfHeroicStrike(boolean value) {
        glyphData[9] = value;
    }

    @GlyphDataAttribute(index = 10, name = "Glyph of Intervene", major = true)
    public boolean isGlyphOfIntervene() {
        return glyphData[10];
    }

    public void setGlyphOfIntervene(boolean value) {
        glyphData[10] = value;
    }

    @GlyphDataAttribute(index = 11, name = "Glyph of Last Stand", major = true)
    public boolean isGlyphOfLastStand() {
        return glyphData[11];
    }

    public void setGlyphOfLastStand(boolean value) {
        glyphData[11] = value;
    }

    @GlyphDataAttribute(index = 12, name = "Glyph of Mortal Strike", major = true)
    public boolean isGlyphOfMortalStrike() {
        return glyphData[12];
    }

    public void setGlyphOfMortalStrike(boolean value) {
        glyphData[12] = value;
    }

    @GlyphDataAttribute(index = 13, name = "Glyph of Overpower", major = true)
    public boolean isGlyphOfOverpower() {
        return glyphData[13];
    }

    public void setGlyphOfOverpower(boolean value) {
        glyphData[13] = value;
    }

    @GlyphDataAttribute(index = 14, name = "Glyph of Rapid Charge", major = true)
    public boolean isGlyphOfRapidCharge() {
        return glyphData[14];
    }

    public void setGlyphOfRapidCharge(boolean value) {
        glyphData[14] = value;
    }

    @GlyphDataAttribute(index = 15, name = "Glyph of Rending", major = true)
    public boolean isGlyphOfRending() {
        return glyphData[15];
    }

    public void setGlyphOfRending(boolean value) {
        glyphData[15] = value;
    }

    @GlyphDataAttribute(index = 16, name = "Glyph of Resonating Power", major = true)
    public boolean isGlyphOfResonatingPower() {
        return glyphData[16];
    }

    public void setGlyphOfResonatingPower(boolean value) {
        glyphData[16] = value;
    }

    @GlyphDataAttribute(index = 17, name = "Glyph of Revenge", major = true)
    public boolean isGlyphOfRevenge() {
        return glyphData[17];
    }

    public void setGlyphOfRevenge(boolean value) {
        glyphData[17] = value;
    }

    @GlyphDataAttribute(index = 18, name = "Glyph of Shield Wall", major = true)
    public boolean isGlyphOfShieldWall() {
        return glyphData[18];
    }

    public void setGlyphOfShieldWall(boolean value) {
        glyphData[18] = value;
    }

    @GlyphDataAttribute(index = 19, name = "Glyph of Shockwave", major = true)
    public boolean isGlyphOfShockwave() {
        return glyphData[19];
    }

    public void setGlyphOfShockwave(boolean value) {
        glyphData[19] = value;
    }

    @GlyphDataAttribute(index = 20, name = "Glyph of Spell Reflection", major = true)
    public boolean isGlyphOfSpellReflection() {
        return glyphData[20];
    }

    public void setGlyphOfSpellReflection(boolean value) {
        glyphData[20] = value;
    }

    @GlyphDataAttribute(index = 21, name = "Glyph of Sunder Armor", major = true)
    public boolean isGlyphOfSunderArmor() {
        return glyphData[21];
    }

    public void setGlyphOfSunderArmor(boolean value) {
        glyphData[21] = value;
    }

    @GlyphDataAttribute(index = 22, name = "Glyph of Sweeping Strikes", major = true)
    public boolean isGlyphOfSweepingStrikes() {
        return glyphData[22];
    }

    public void setGlyphOfSweepingStrikes(boolean value) {
        glyphData[22] = value;
    }

    @GlyphDataAttribute(index = 23, name = "Glyph of Taunt", major = true)
    public boolean isGlyphOfTaunt() {
        return glyphData[23];
    }

    public void setGlyphOfTaunt(boolean value) {
        glyphData[23] = value;
    }

    @GlyphDataAttribute(index = 24, name = "Glyph of Victory Rush", major = true)
    public boolean isGlyphOfVictoryRush() {
        return glyphData[24];
    }

    public void setGlyphOfVictoryRush(boolean value) {
        glyphData[24] = value;
    }

    @GlyphDataAttribute(index = 25, name = "Glyph of Vigilance", major = true)
    public boolean isGlyphOfVigilance() {
        return glyphData[25];
    }

    public void setGlyphOfVigilance(boolean value) {
        glyphData[25] = value;
    }

    @GlyphDataAttribute(index = 26, name = "Glyph of Whirlwind", major = true)
    public boolean isGlyphOfWhirlwind() {
        return glyphData[26];
    }

    public void setGlyphOfWhirlwind(boolean value) {
        glyphData[26] = value;
    }

    @GlyphDataAttribute(index = 27, name = "Glyph of Battle", major = false)
    public boolean isGlyphOfBattle() {
        return glyphData[27];
    }

    public void setGlyphOfBattle(boolean value) {
        glyphData[27] = value;
    }

    @GlyphDataAttribute(index = 28, name = "Glyph of Bloodrage", major = false)
    public boolean isGlyphOfBloodrage() {
        return glyphData[28];
    }

    public void setGlyphOfBloodrage(boolean value) {
        glyphData[28] = value;
    }

    @GlyphDataAttribute(index = 29, name = "Glyph of Charge", major = false)
    public boolean isGlyphOfCharge() {
        return glyphData[29];
    }

    public void setGlyphOfCharge(boolean value) {
        glyphData[29] = value;
    }

    @GlyphDataAttribute(index = 30, name = "Glyph of Enduring Victory", major = false)
    public boolean isGlyphOfEnduringVictory() {
        return glyphData[30];
    }

    public void setGlyphOfEnduringVictory(boolean value) {
        glyphData[30] = value;
    }

    @GlyphDataAttribute(index = 31, name = "Glyph of Mocking Blow", major = false)
    public boolean isGlyphOfMockingBlow() {
        return glyphData[31];
    }

    public void setGlyphOfMockingBlow(boolean value) {
        glyphData[31] = value;
    }

    @GlyphDataAttribute(index = 32, name = "Glyph of Thunder Clap", major = false)
    public boolean isGlyphOfThunderClap() {
        return glyphData[32];
    }

    public void setGlyphOfThunderClap(boolean value) {
        glyphData[32] = value;
    }

    @GlyphDataAttribute(index = 33, name = "Glyph of Command", major = false)
    public boolean isGlyphOfCommand() {
        return glyphData[33];
    }

    public void setGlyphOfCommand(boolean value) {
        glyphData[33] = value;
    }

}
