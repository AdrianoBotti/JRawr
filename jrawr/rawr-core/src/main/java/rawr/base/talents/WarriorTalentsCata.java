// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassTalents.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassGlyphs.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.talents;

import rawr.base.character.CharacterClass;
import rawr.base.character.SavedTalentSpec;

/**
 * A Cataclysm-era variant of the warrior talent tree, carried over from the legacy fork but not
 * wired into the Java port.
 *
 * <p>{@code data} has 61 rank slots (Arms 0-19, Fury 20-40, Protection 41-60 in
 * {@link #TreeNames} order) and every talent has {@code maxPoints = 3}, the layout the C# fork kept
 * for its experimental "CataMode" code. That mode is commented out in the original:
 * {@code SavedTalentSpec} always builds a {@link WarriorTalents} for warriors, and nothing in
 * JRawr constructs this class or reads its ranks. The only reference outside this file is a note in
 * the dpswarr model that the C# off-hand branch for it was not ported, so the class exists for
 * source parity and would be needed only if the Cataclysm mode were ever revived.</p>
 *
 * <p>The 34-slot glyph table is identical to {@link WarriorTalents} (27 major + 7 minor), and the
 * compact-string constructor, {@link #clone()} and {@link TalentsBase} contract are unchanged.
 * Because {@link TalentsBase#getCharacterClass()} does not list this type, it reports
 * {@link CharacterClass#Warrior} through the fallback.</p>
 */
public class WarriorTalentsCata extends TalentsBase {
    public static final String[] TreeNames = new String[] {
        "Arms",
        "Fury",
        "Protection",
    };

    private final int[] data = new int[61];
    private final boolean[] glyphData = new boolean[34];

    public WarriorTalentsCata() {
    }

    public WarriorTalentsCata(String talents) {
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

    public WarriorTalentsCata clone() {
        WarriorTalentsCata clone = new WarriorTalentsCata();
        System.arraycopy(data, 0, clone.data, 0, data.length);
        System.arraycopy(glyphData, 0, clone.glyphData, 0, glyphData.length);
        return clone;
    }

    @TalentDataAttribute(index = 0, name = "War Academy", maxPoints = 3, tree = 0, column = 1, row = 1, prerequisite = -1)
    public int getWarAcademy() {
        return data[0];
    }

    public void setWarAcademy(int value) {
        data[0] = value;
    }

    @TalentDataAttribute(index = 1, name = "Field Dressing", maxPoints = 2, tree = 0, column = 2, row = 1, prerequisite = -1)
    public int getFieldDressing() {
        return data[1];
    }

    public void setFieldDressing(int value) {
        data[1] = value;
    }

    @TalentDataAttribute(index = 2, name = "Blitz", maxPoints = 2, tree = 0, column = 3, row = 1, prerequisite = -1)
    public int getBlitz() {
        return data[2];
    }

    public void setBlitz(int value) {
        data[2] = value;
    }

    @TalentDataAttribute(index = 3, name = "Tactical Mastery", maxPoints = 2, tree = 0, column = 1, row = 2, prerequisite = -1)
    public int getTacticalMastery() {
        return data[3];
    }

    public void setTacticalMastery(int value) {
        data[3] = value;
    }

    @TalentDataAttribute(index = 4, name = "Second Wind", maxPoints = 2, tree = 0, column = 2, row = 2, prerequisite = -1)
    public int getSecondWind() {
        return data[4];
    }

    public void setSecondWind(int value) {
        data[4] = value;
    }

    @TalentDataAttribute(index = 5, name = "Deep Wounds", maxPoints = 3, tree = 0, column = 3, row = 2, prerequisite = -1)
    public int getDeepWounds() {
        return data[5];
    }

    public void setDeepWounds(int value) {
        data[5] = value;
    }

    @TalentDataAttribute(index = 6, name = "Drums of War", maxPoints = 2, tree = 0, column = 4, row = 2, prerequisite = -1)
    public int getDrumsofWar() {
        return data[6];
    }

    public void setDrumsofWar(int value) {
        data[6] = value;
    }

    @TalentDataAttribute(index = 7, name = "Taste for Blood", maxPoints = 3, tree = 0, column = 1, row = 3, prerequisite = -1)
    public int getTasteForBlood() {
        return data[7];
    }

    public void setTasteForBlood(int value) {
        data[7] = value;
    }

    @TalentDataAttribute(index = 8, name = "Sweeping Strikes", maxPoints = 1, tree = 0, column = 2, row = 3, prerequisite = -1)
    public int getSweepingStrikes() {
        return data[8];
    }

    public void setSweepingStrikes(int value) {
        data[8] = value;
    }

    @TalentDataAttribute(index = 9, name = "Impale", maxPoints = 2, tree = 0, column = 3, row = 3, prerequisite = 5)
    public int getImpale() {
        return data[9];
    }

    public void setImpale(int value) {
        data[9] = value;
    }

    @TalentDataAttribute(index = 10, name = "Improved Hamstring", maxPoints = 2, tree = 0, column = 4, row = 3, prerequisite = -1)
    public int getImprovedHamstring() {
        return data[10];
    }

    public void setImprovedHamstring(int value) {
        data[10] = value;
    }

    @TalentDataAttribute(index = 11, name = "Improved Slam", maxPoints = 2, tree = 0, column = 1, row = 4, prerequisite = -1)
    public int getImprovedSlam() {
        return data[11];
    }

    public void setImprovedSlam(int value) {
        data[11] = value;
    }

    @TalentDataAttribute(index = 12, name = "Deadly Calm", maxPoints = 1, tree = 0, column = 2, row = 4, prerequisite = -1)
    public int getDeadlyCalm() {
        return data[12];
    }

    public void setDeadlyCalm(int value) {
        data[12] = value;
    }

    @TalentDataAttribute(index = 13, name = "Blood Frenzy", maxPoints = 2, tree = 0, column = 3, row = 4, prerequisite = -1)
    public int getBloodFrenzy() {
        return data[13];
    }

    public void setBloodFrenzy(int value) {
        data[13] = value;
    }

    @TalentDataAttribute(index = 14, name = "Lambs to the Slaughter", maxPoints = 3, tree = 0, column = 1, row = 5, prerequisite = -1)
    public int getLambstotheSlaughter() {
        return data[14];
    }

    public void setLambstotheSlaughter(int value) {
        data[14] = value;
    }

    @TalentDataAttribute(index = 15, name = "Juggernaut", maxPoints = 1, tree = 0, column = 2, row = 5, prerequisite = -1)
    public int getJuggernaut() {
        return data[15];
    }

    public void setJuggernaut(int value) {
        data[15] = value;
    }

    @TalentDataAttribute(index = 16, name = "Sudden Death", maxPoints = 2, tree = 0, column = 4, row = 5, prerequisite = -1)
    public int getSuddenDeath() {
        return data[16];
    }

    public void setSuddenDeath(int value) {
        data[16] = value;
    }

    @TalentDataAttribute(index = 17, name = "Wrecking Crew", maxPoints = 3, tree = 0, column = 1, row = 6, prerequisite = -1)
    public int getWreckingCrew() {
        return data[17];
    }

    public void setWreckingCrew(int value) {
        data[17] = value;
    }

    @TalentDataAttribute(index = 18, name = "Throwdown", maxPoints = 1, tree = 0, column = 3, row = 6, prerequisite = 15)
    public int getThrowdown() {
        return data[18];
    }

    public void setThrowdown(int value) {
        data[18] = value;
    }

    @TalentDataAttribute(index = 19, name = "Bladestorm", maxPoints = 1, tree = 0, column = 2, row = 7, prerequisite = 15)
    public int getBladestorm() {
        return data[19];
    }

    public void setBladestorm(int value) {
        data[19] = value;
    }

    @TalentDataAttribute(index = 20, name = "Blood Craze", maxPoints = 3, tree = 1, column = 1, row = 1, prerequisite = -1)
    public int getBloodCraze() {
        return data[20];
    }

    public void setBloodCraze(int value) {
        data[20] = value;
    }

    @TalentDataAttribute(index = 21, name = "Battle Trance", maxPoints = 3, tree = 1, column = 2, row = 1, prerequisite = -1)
    public int getBattleTrance() {
        return data[21];
    }

    public void setBattleTrance(int value) {
        data[21] = value;
    }

    @TalentDataAttribute(index = 22, name = "Cruelty", maxPoints = 2, tree = 1, column = 3, row = 1, prerequisite = -1)
    public int getCruelty() {
        return data[22];
    }

    public void setCruelty(int value) {
        data[22] = value;
    }

    @TalentDataAttribute(index = 23, name = "Executioner", maxPoints = 2, tree = 1, column = 1, row = 2, prerequisite = -1)
    public int getImprovedExecute() {
        return data[23];
    }

    public void setImprovedExecute(int value) {
        data[23] = value;
    }

    @TalentDataAttribute(index = 24, name = "Booming Voice", maxPoints = 2, tree = 1, column = 2, row = 2, prerequisite = -1)
    public int getBoomingVoice() {
        return data[24];
    }

    public void setBoomingVoice(int value) {
        data[24] = value;
    }

    @TalentDataAttribute(index = 25, name = "Rude Interruption", maxPoints = 2, tree = 1, column = 3, row = 2, prerequisite = -1)
    public int getRudeInterruption() {
        return data[25];
    }

    public void setRudeInterruption(int value) {
        data[25] = value;
    }

    @TalentDataAttribute(index = 26, name = "Piercing Howl", maxPoints = 1, tree = 1, column = 4, row = 2, prerequisite = -1)
    public int getPiercingHowl() {
        return data[26];
    }

    public void setPiercingHowl(int value) {
        data[26] = value;
    }

    @TalentDataAttribute(index = 27, name = "Flurry", maxPoints = 3, tree = 1, column = 1, row = 3, prerequisite = -1)
    public int getFlurry() {
        return data[27];
    }

    public void setFlurry(int value) {
        data[27] = value;
    }

    @TalentDataAttribute(index = 28, name = "Death Wish", maxPoints = 1, tree = 1, column = 2, row = 3, prerequisite = -1)
    public int getDeathWish() {
        return data[28];
    }

    public void setDeathWish(int value) {
        data[28] = value;
    }

    @TalentDataAttribute(index = 29, name = "Enrage", maxPoints = 3, tree = 1, column = 3, row = 3, prerequisite = -1)
    public int getEnrage() {
        return data[29];
    }

    public void setEnrage(int value) {
        data[29] = value;
    }

    @TalentDataAttribute(index = 30, name = "Die by the Sword", maxPoints = 2, tree = 1, column = 1, row = 4, prerequisite = -1)
    public int getDiebytheSword() {
        return data[30];
    }

    public void setDiebytheSword(int value) {
        data[30] = value;
    }

    @TalentDataAttribute(index = 31, name = "Raging Blow", maxPoints = 1, tree = 1, column = 2, row = 4, prerequisite = 28)
    public int getRagingBlow() {
        return data[31];
    }

    public void setRagingBlow(int value) {
        data[31] = value;
    }

    @TalentDataAttribute(index = 32, name = "Rampage", maxPoints = 1, tree = 1, column = 3, row = 4, prerequisite = 31)
    public int getRampage() {
        return data[32];
    }

    public void setRampage(int value) {
        data[32] = value;
    }

    @TalentDataAttribute(index = 33, name = "Heroic Fury", maxPoints = 1, tree = 1, column = 4, row = 4, prerequisite = -1)
    public int getHeroicFury() {
        return data[33];
    }

    public void setHeroicFury(int value) {
        data[33] = value;
    }

    @TalentDataAttribute(index = 34, name = "Furious Attacks", maxPoints = 1, tree = 1, column = 1, row = 5, prerequisite = -1)
    public int getFuriousAttacks() {
        return data[34];
    }

    public void setFuriousAttacks(int value) {
        data[34] = value;
    }

    @TalentDataAttribute(index = 35, name = "Meat Cleaver", maxPoints = 2, tree = 1, column = 3, row = 5, prerequisite = -1)
    public int getMeatCleaver() {
        return data[35];
    }

    public void setMeatCleaver(int value) {
        data[35] = value;
    }

    @TalentDataAttribute(index = 36, name = "Intensify Rage", maxPoints = 2, tree = 1, column = 4, row = 5, prerequisite = -1)
    public int getIntensifyRage() {
        return data[36];
    }

    public void setIntensifyRage(int value) {
        data[36] = value;
    }

    @TalentDataAttribute(index = 37, name = "Bloodsurge", maxPoints = 3, tree = 1, column = 2, row = 6, prerequisite = 31)
    public int getBloodsurge() {
        return data[37];
    }

    public void setBloodsurge(int value) {
        data[37] = value;
    }

    @TalentDataAttribute(index = 38, name = "Skirmisher", maxPoints = 2, tree = 1, column = 3, row = 6, prerequisite = -1)
    public int getSkirmisher() {
        return data[38];
    }

    public void setSkirmisher(int value) {
        data[38] = value;
    }

    @TalentDataAttribute(index = 39, name = "Titan's Grip", maxPoints = 1, tree = 1, column = 2, row = 7, prerequisite = -1)
    public int getTitansGrip() {
        return data[39];
    }

    public void setTitansGrip(int value) {
        data[39] = value;
    }

    @TalentDataAttribute(index = 40, name = "Single-Minded Fury", maxPoints = 1, tree = 1, column = 3, row = 7, prerequisite = -1)
    public int getSingleMindedFury() {
        return data[40];
    }

    public void setSingleMindedFury(int value) {
        data[40] = value;
    }

    @TalentDataAttribute(index = 41, name = "Incite", maxPoints = 3, tree = 2, column = 1, row = 1, prerequisite = -1)
    public int getIncite() {
        return data[41];
    }

    public void setIncite(int value) {
        data[41] = value;
    }

    @TalentDataAttribute(index = 42, name = "Toughness", maxPoints = 3, tree = 2, column = 2, row = 1, prerequisite = -1)
    public int getToughness() {
        return data[42];
    }

    public void setToughness(int value) {
        data[42] = value;
    }

    @TalentDataAttribute(index = 43, name = "Blood and Thunder", maxPoints = 3, tree = 2, column = 3, row = 1, prerequisite = -1)
    public int getBloodandThunder() {
        return data[43];
    }

    public void setBloodandThunder(int value) {
        data[43] = value;
    }

    @TalentDataAttribute(index = 44, name = "Shield Specialization", maxPoints = 3, tree = 2, column = 1, row = 2, prerequisite = -1)
    public int getShieldSpecialization() {
        return data[44];
    }

    public void setShieldSpecialization(int value) {
        data[44] = value;
    }

    @TalentDataAttribute(index = 45, name = "Shield Mastery", maxPoints = 3, tree = 2, column = 2, row = 2, prerequisite = -1)
    public int getShieldMastery() {
        return data[45];
    }

    public void setShieldMastery(int value) {
        data[45] = value;
    }

    @TalentDataAttribute(index = 46, name = "Hold the Line", maxPoints = 2, tree = 2, column = 3, row = 2, prerequisite = -1)
    public int getHoldtheLine() {
        return data[46];
    }

    public void setHoldtheLine(int value) {
        data[46] = value;
    }

    @TalentDataAttribute(index = 47, name = "Gag Order", maxPoints = 2, tree = 2, column = 4, row = 2, prerequisite = -1)
    public int getGagOrder() {
        return data[47];
    }

    public void setGagOrder(int value) {
        data[47] = value;
    }

    @TalentDataAttribute(index = 48, name = "Last Stand", maxPoints = 1, tree = 2, column = 1, row = 3, prerequisite = -1)
    public int getLastStand() {
        return data[48];
    }

    public void setLastStand(int value) {
        data[48] = value;
    }

    @TalentDataAttribute(index = 49, name = "Concussion Blow", maxPoints = 1, tree = 2, column = 2, row = 3, prerequisite = -1)
    public int getConcussionBlow() {
        return data[49];
    }

    public void setConcussionBlow(int value) {
        data[49] = value;
    }

    @TalentDataAttribute(index = 50, name = "Bastion of Defense", maxPoints = 1, tree = 2, column = 3, row = 3, prerequisite = -1)
    public int getBastionofDefense() {
        return data[50];
    }

    public void setBastionofDefense(int value) {
        data[50] = value;
    }

    @TalentDataAttribute(index = 51, name = "Warbringer", maxPoints = 1, tree = 2, column = 4, row = 3, prerequisite = -1)
    public int getWarbringer() {
        return data[51];
    }

    public void setWarbringer(int value) {
        data[51] = value;
    }

    @TalentDataAttribute(index = 52, name = "Improved Revenge", maxPoints = 2, tree = 2, column = 1, row = 4, prerequisite = -1)
    public int getImprovedRevenge() {
        return data[52];
    }

    public void setImprovedRevenge(int value) {
        data[52] = value;
    }

    @TalentDataAttribute(index = 53, name = "Devastate", maxPoints = 1, tree = 2, column = 3, row = 4, prerequisite = -1)
    public int getDevastate() {
        return data[53];
    }

    public void setDevastate(int value) {
        data[53] = value;
    }

    @TalentDataAttribute(index = 54, name = "Impending Victory", maxPoints = 2, tree = 2, column = 4, row = 4, prerequisite = 53)
    public int getImpendingVictory() {
        return data[54];
    }

    public void setImpendingVictory(int value) {
        data[54] = value;
    }

    @TalentDataAttribute(index = 55, name = "Thunderstruck", maxPoints = 2, tree = 2, column = 1, row = 5, prerequisite = -1)
    public int getThunderstruck() {
        return data[55];
    }

    public void setThunderstruck(int value) {
        data[55] = value;
    }

    @TalentDataAttribute(index = 56, name = "Vigilance", maxPoints = 1, tree = 2, column = 2, row = 5, prerequisite = 49)
    public int getVigilance() {
        return data[56];
    }

    public void setVigilance(int value) {
        data[56] = value;
    }

    @TalentDataAttribute(index = 57, name = "Heavy Repercussions", maxPoints = 2, tree = 2, column = 4, row = 5, prerequisite = -1)
    public int getHeavyRepercussions() {
        return data[57];
    }

    public void setHeavyRepercussions(int value) {
        data[57] = value;
    }

    @TalentDataAttribute(index = 58, name = "Safeguard", maxPoints = 2, tree = 2, column = 2, row = 6, prerequisite = -1)
    public int getSafeguard() {
        return data[58];
    }

    public void setSafeguard(int value) {
        data[58] = value;
    }

    @TalentDataAttribute(index = 59, name = "Sword and Board", maxPoints = 3, tree = 2, column = 3, row = 6, prerequisite = 53)
    public int getSwordAndBoard() {
        return data[59];
    }

    public void setSwordAndBoard(int value) {
        data[59] = value;
    }

    @TalentDataAttribute(index = 60, name = "Shockwave", maxPoints = 1, tree = 2, column = 2, row = 7, prerequisite = -1)
    public int getShockwave() {
        return data[60];
    }

    public void setShockwave(int value) {
        data[60] = value;
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
