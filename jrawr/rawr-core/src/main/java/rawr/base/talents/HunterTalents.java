// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassTalents.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassGlyphs.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.talents;

/**
 * The hunter's talent and glyph allocation: the Beast Mastery, Marksmanship and Survival trees plus
 * the 33 glyph slots of this fork's table.
 *
 * <p>{@code data} has 81 rank slots, contiguous per tree: Beast Mastery 0-25 (tree 0),
 * Marksmanship 26-52 (tree 1), Survival 53-80 (tree 2); {@link #TreeNames} fixes that order. The
 * hunter model derives its shot priority and pet behaviour from these ranks, e.g.
 * {@link #getImprovedAspectOfTheHawk()} (Beast Mastery first talent),
 * {@link #getImprovedConcussiveShot()} (Marksmanship) and {@link #getBlackArrow()} (Survival), and
 * tests glyph flags such as {@link #isGlyphOfSteadyShot()} and {@link #isGlyphOfKillShot()}.</p>
 *
 * <p>{@code glyphData} has 33 flags: 27 major glyphs (0-26) then 6 minor glyphs (27-32). The
 * {@code String} constructor, {@link #clone()} and the live-array accessors follow
 * {@link TalentsBase}.</p>
 */
public class HunterTalents extends TalentsBase {
    public static final String[] TreeNames = new String[] {
        "Beast Mastery",
        "Marksmanship",
        "Survival",
    };

    private final int[] data = new int[81];
    private final boolean[] glyphData = new boolean[33];

    public HunterTalents() {
    }

    public HunterTalents(String talents) {
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

    public HunterTalents clone() {
        HunterTalents clone = new HunterTalents();
        System.arraycopy(data, 0, clone.data, 0, data.length);
        System.arraycopy(glyphData, 0, clone.glyphData, 0, glyphData.length);
        return clone;
    }

    @TalentDataAttribute(index = 0, name = "Improved Aspect of the Hawk", maxPoints = 5, tree = 0, column = 2, row = 1, prerequisite = -1)
    public int getImprovedAspectOfTheHawk() {
        return data[0];
    }

    public void setImprovedAspectOfTheHawk(int value) {
        data[0] = value;
    }

    @TalentDataAttribute(index = 1, name = "Endurance Training", maxPoints = 5, tree = 0, column = 3, row = 1, prerequisite = -1)
    public int getEnduranceTraining() {
        return data[1];
    }

    public void setEnduranceTraining(int value) {
        data[1] = value;
    }

    @TalentDataAttribute(index = 2, name = "Focused Fire", maxPoints = 2, tree = 0, column = 1, row = 2, prerequisite = -1)
    public int getFocusedFire() {
        return data[2];
    }

    public void setFocusedFire(int value) {
        data[2] = value;
    }

    @TalentDataAttribute(index = 3, name = "Improved Aspect of the Monkey", maxPoints = 3, tree = 0, column = 2, row = 2, prerequisite = -1)
    public int getImprovedAspectOfTheMonkey() {
        return data[3];
    }

    public void setImprovedAspectOfTheMonkey(int value) {
        data[3] = value;
    }

    @TalentDataAttribute(index = 4, name = "Thick Hide", maxPoints = 3, tree = 0, column = 3, row = 2, prerequisite = -1)
    public int getThickHide() {
        return data[4];
    }

    public void setThickHide(int value) {
        data[4] = value;
    }

    @TalentDataAttribute(index = 5, name = "Improved Revive Pet", maxPoints = 2, tree = 0, column = 4, row = 2, prerequisite = -1)
    public int getImprovedRevivePet() {
        return data[5];
    }

    public void setImprovedRevivePet(int value) {
        data[5] = value;
    }

    @TalentDataAttribute(index = 6, name = "Pathfinding", maxPoints = 2, tree = 0, column = 1, row = 3, prerequisite = -1)
    public int getPathfinding() {
        return data[6];
    }

    public void setPathfinding(int value) {
        data[6] = value;
    }

    @TalentDataAttribute(index = 7, name = "Aspect Mastery", maxPoints = 1, tree = 0, column = 2, row = 3, prerequisite = -1)
    public int getAspectMastery() {
        return data[7];
    }

    public void setAspectMastery(int value) {
        data[7] = value;
    }

    @TalentDataAttribute(index = 8, name = "Unleashed Fury", maxPoints = 5, tree = 0, column = 3, row = 3, prerequisite = -1)
    public int getUnleashedFury() {
        return data[8];
    }

    public void setUnleashedFury(int value) {
        data[8] = value;
    }

    @TalentDataAttribute(index = 9, name = "Improved Mend Pet", maxPoints = 2, tree = 0, column = 2, row = 4, prerequisite = -1)
    public int getImprovedMendPet() {
        return data[9];
    }

    public void setImprovedMendPet(int value) {
        data[9] = value;
    }

    @TalentDataAttribute(index = 10, name = "Ferocity", maxPoints = 5, tree = 0, column = 3, row = 4, prerequisite = -1)
    public int getFerocity() {
        return data[10];
    }

    public void setFerocity(int value) {
        data[10] = value;
    }

    @TalentDataAttribute(index = 11, name = "Spirit Bond", maxPoints = 2, tree = 0, column = 1, row = 5, prerequisite = -1)
    public int getSpiritBond() {
        return data[11];
    }

    public void setSpiritBond(int value) {
        data[11] = value;
    }

    @TalentDataAttribute(index = 12, name = "Intimidation", maxPoints = 1, tree = 0, column = 2, row = 5, prerequisite = -1)
    public int getIntimidation() {
        return data[12];
    }

    public void setIntimidation(int value) {
        data[12] = value;
    }

    @TalentDataAttribute(index = 13, name = "Bestial Discipline", maxPoints = 2, tree = 0, column = 4, row = 5, prerequisite = -1)
    public int getBestialDiscipline() {
        return data[13];
    }

    public void setBestialDiscipline(int value) {
        data[13] = value;
    }

    @TalentDataAttribute(index = 14, name = "Animal Handler", maxPoints = 2, tree = 0, column = 1, row = 6, prerequisite = -1)
    public int getAnimalHandler() {
        return data[14];
    }

    public void setAnimalHandler(int value) {
        data[14] = value;
    }

    @TalentDataAttribute(index = 15, name = "Frenzy", maxPoints = 5, tree = 0, column = 3, row = 6, prerequisite = 10)
    public int getFrenzy() {
        return data[15];
    }

    public void setFrenzy(int value) {
        data[15] = value;
    }

    @TalentDataAttribute(index = 16, name = "Ferocious Inspiration", maxPoints = 3, tree = 0, column = 1, row = 7, prerequisite = -1)
    public int getFerociousInspiration() {
        return data[16];
    }

    public void setFerociousInspiration(int value) {
        data[16] = value;
    }

    @TalentDataAttribute(index = 17, name = "Bestial Wrath", maxPoints = 1, tree = 0, column = 2, row = 7, prerequisite = 12)
    public int getBestialWrath() {
        return data[17];
    }

    public void setBestialWrath(int value) {
        data[17] = value;
    }

    @TalentDataAttribute(index = 18, name = "Catlike Reflexes", maxPoints = 3, tree = 0, column = 3, row = 7, prerequisite = -1)
    public int getCatlikeReflexes() {
        return data[18];
    }

    public void setCatlikeReflexes(int value) {
        data[18] = value;
    }

    @TalentDataAttribute(index = 19, name = "Invigoration", maxPoints = 2, tree = 0, column = 1, row = 8, prerequisite = 16)
    public int getInvigoration() {
        return data[19];
    }

    public void setInvigoration(int value) {
        data[19] = value;
    }

    @TalentDataAttribute(index = 20, name = "Serpent's Swiftness", maxPoints = 5, tree = 0, column = 3, row = 8, prerequisite = -1)
    public int getSerpentsSwiftness() {
        return data[20];
    }

    public void setSerpentsSwiftness(int value) {
        data[20] = value;
    }

    @TalentDataAttribute(index = 21, name = "Longevity", maxPoints = 3, tree = 0, column = 1, row = 9, prerequisite = -1)
    public int getLongevity() {
        return data[21];
    }

    public void setLongevity(int value) {
        data[21] = value;
    }

    @TalentDataAttribute(index = 22, name = "The Beast Within", maxPoints = 1, tree = 0, column = 2, row = 9, prerequisite = 17)
    public int getTheBeastWithin() {
        return data[22];
    }

    public void setTheBeastWithin(int value) {
        data[22] = value;
    }

    @TalentDataAttribute(index = 23, name = "Cobra Strikes", maxPoints = 3, tree = 0, column = 3, row = 9, prerequisite = 20)
    public int getCobraStrikes() {
        return data[23];
    }

    public void setCobraStrikes(int value) {
        data[23] = value;
    }

    @TalentDataAttribute(index = 24, name = "Kindred Spirits", maxPoints = 5, tree = 0, column = 2, row = 10, prerequisite = -1)
    public int getKindredSpirits() {
        return data[24];
    }

    public void setKindredSpirits(int value) {
        data[24] = value;
    }

    @TalentDataAttribute(index = 25, name = "Beast Mastery", maxPoints = 1, tree = 0, column = 2, row = 11, prerequisite = -1)
    public int getBeastMastery() {
        return data[25];
    }

    public void setBeastMastery(int value) {
        data[25] = value;
    }

    @TalentDataAttribute(index = 26, name = "Improved Concussive Shot", maxPoints = 2, tree = 1, column = 1, row = 1, prerequisite = -1)
    public int getImprovedConcussiveShot() {
        return data[26];
    }

    public void setImprovedConcussiveShot(int value) {
        data[26] = value;
    }

    @TalentDataAttribute(index = 27, name = "Focused Aim", maxPoints = 3, tree = 1, column = 2, row = 1, prerequisite = -1)
    public int getFocusedAim() {
        return data[27];
    }

    public void setFocusedAim(int value) {
        data[27] = value;
    }

    @TalentDataAttribute(index = 28, name = "Lethal Shots", maxPoints = 5, tree = 1, column = 3, row = 1, prerequisite = -1)
    public int getLethalShots() {
        return data[28];
    }

    public void setLethalShots(int value) {
        data[28] = value;
    }

    @TalentDataAttribute(index = 29, name = "Careful Aim", maxPoints = 3, tree = 1, column = 1, row = 2, prerequisite = -1)
    public int getCarefulAim() {
        return data[29];
    }

    public void setCarefulAim(int value) {
        data[29] = value;
    }

    @TalentDataAttribute(index = 30, name = "Improved Hunter's Mark", maxPoints = 3, tree = 1, column = 2, row = 2, prerequisite = -1)
    public int getImprovedHuntersMark() {
        return data[30];
    }

    public void setImprovedHuntersMark(int value) {
        data[30] = value;
    }

    @TalentDataAttribute(index = 31, name = "Mortal Shots", maxPoints = 5, tree = 1, column = 3, row = 2, prerequisite = -1)
    public int getMortalShots() {
        return data[31];
    }

    public void setMortalShots(int value) {
        data[31] = value;
    }

    @TalentDataAttribute(index = 32, name = "Go for the Throat", maxPoints = 2, tree = 1, column = 1, row = 3, prerequisite = -1)
    public int getGoForTheThroat() {
        return data[32];
    }

    public void setGoForTheThroat(int value) {
        data[32] = value;
    }

    @TalentDataAttribute(index = 33, name = "Improved Arcane Shot", maxPoints = 3, tree = 1, column = 2, row = 3, prerequisite = -1)
    public int getImprovedArcaneShot() {
        return data[33];
    }

    public void setImprovedArcaneShot(int value) {
        data[33] = value;
    }

    @TalentDataAttribute(index = 34, name = "Aimed Shot", maxPoints = 1, tree = 1, column = 3, row = 3, prerequisite = 31)
    public int getAimedShot() {
        return data[34];
    }

    public void setAimedShot(int value) {
        data[34] = value;
    }

    @TalentDataAttribute(index = 35, name = "Rapid Killing", maxPoints = 2, tree = 1, column = 4, row = 3, prerequisite = -1)
    public int getRapidKilling() {
        return data[35];
    }

    public void setRapidKilling(int value) {
        data[35] = value;
    }

    @TalentDataAttribute(index = 36, name = "Improved Stings", maxPoints = 3, tree = 1, column = 2, row = 4, prerequisite = -1)
    public int getImprovedStings() {
        return data[36];
    }

    public void setImprovedStings(int value) {
        data[36] = value;
    }

    @TalentDataAttribute(index = 37, name = "Efficiency", maxPoints = 5, tree = 1, column = 3, row = 4, prerequisite = -1)
    public int getEfficiency() {
        return data[37];
    }

    public void setEfficiency(int value) {
        data[37] = value;
    }

    @TalentDataAttribute(index = 38, name = "Concussive Barrage", maxPoints = 2, tree = 1, column = 1, row = 5, prerequisite = -1)
    public int getConcussiveBarrage() {
        return data[38];
    }

    public void setConcussiveBarrage(int value) {
        data[38] = value;
    }

    @TalentDataAttribute(index = 39, name = "Readiness", maxPoints = 1, tree = 1, column = 2, row = 5, prerequisite = -1)
    public int getReadiness() {
        return data[39];
    }

    public void setReadiness(int value) {
        data[39] = value;
    }

    @TalentDataAttribute(index = 40, name = "Barrage", maxPoints = 3, tree = 1, column = 3, row = 5, prerequisite = -1)
    public int getBarrage() {
        return data[40];
    }

    public void setBarrage(int value) {
        data[40] = value;
    }

    @TalentDataAttribute(index = 41, name = "Combat Experience", maxPoints = 2, tree = 1, column = 1, row = 6, prerequisite = -1)
    public int getCombatExperience() {
        return data[41];
    }

    public void setCombatExperience(int value) {
        data[41] = value;
    }

    @TalentDataAttribute(index = 42, name = "Ranged Weapon Specialization", maxPoints = 3, tree = 1, column = 4, row = 6, prerequisite = -1)
    public int getRangedWeaponSpecialization() {
        return data[42];
    }

    public void setRangedWeaponSpecialization(int value) {
        data[42] = value;
    }

    @TalentDataAttribute(index = 43, name = "Piercing Shots", maxPoints = 3, tree = 1, column = 1, row = 7, prerequisite = -1)
    public int getPiercingShots() {
        return data[43];
    }

    public void setPiercingShots(int value) {
        data[43] = value;
    }

    @TalentDataAttribute(index = 44, name = "Trueshot Aura", maxPoints = 1, tree = 1, column = 2, row = 7, prerequisite = 39)
    public int getTrueshotAura() {
        return data[44];
    }

    public void setTrueshotAura(int value) {
        data[44] = value;
    }

    @TalentDataAttribute(index = 45, name = "Improved Barrage", maxPoints = 3, tree = 1, column = 3, row = 7, prerequisite = 40)
    public int getImprovedBarrage() {
        return data[45];
    }

    public void setImprovedBarrage(int value) {
        data[45] = value;
    }

    @TalentDataAttribute(index = 46, name = "Master Marksman", maxPoints = 5, tree = 1, column = 2, row = 8, prerequisite = -1)
    public int getMasterMarksman() {
        return data[46];
    }

    public void setMasterMarksman(int value) {
        data[46] = value;
    }

    @TalentDataAttribute(index = 47, name = "Rapid Recuperation", maxPoints = 2, tree = 1, column = 3, row = 8, prerequisite = -1)
    public int getRapidRecuperation() {
        return data[47];
    }

    public void setRapidRecuperation(int value) {
        data[47] = value;
    }

    @TalentDataAttribute(index = 48, name = "Wild Quiver", maxPoints = 3, tree = 1, column = 1, row = 9, prerequisite = -1)
    public int getWildQuiver() {
        return data[48];
    }

    public void setWildQuiver(int value) {
        data[48] = value;
    }

    @TalentDataAttribute(index = 49, name = "Silencing Shot", maxPoints = 1, tree = 1, column = 2, row = 9, prerequisite = 46)
    public int getSilencingShot() {
        return data[49];
    }

    public void setSilencingShot(int value) {
        data[49] = value;
    }

    @TalentDataAttribute(index = 50, name = "Improved Steady Shot", maxPoints = 3, tree = 1, column = 3, row = 9, prerequisite = -1)
    public int getImprovedSteadyShot() {
        return data[50];
    }

    public void setImprovedSteadyShot(int value) {
        data[50] = value;
    }

    @TalentDataAttribute(index = 51, name = "Marked for Death", maxPoints = 5, tree = 1, column = 2, row = 10, prerequisite = -1)
    public int getMarkedForDeath() {
        return data[51];
    }

    public void setMarkedForDeath(int value) {
        data[51] = value;
    }

    @TalentDataAttribute(index = 52, name = "Chimera Shot", maxPoints = 1, tree = 1, column = 2, row = 11, prerequisite = -1)
    public int getChimeraShot() {
        return data[52];
    }

    public void setChimeraShot(int value) {
        data[52] = value;
    }

    @TalentDataAttribute(index = 53, name = "Improved Tracking", maxPoints = 5, tree = 2, column = 1, row = 1, prerequisite = -1)
    public int getImprovedTracking() {
        return data[53];
    }

    public void setImprovedTracking(int value) {
        data[53] = value;
    }

    @TalentDataAttribute(index = 54, name = "Hawk Eye", maxPoints = 3, tree = 2, column = 2, row = 1, prerequisite = -1)
    public int getHawkEye() {
        return data[54];
    }

    public void setHawkEye(int value) {
        data[54] = value;
    }

    @TalentDataAttribute(index = 55, name = "Savage Strikes", maxPoints = 2, tree = 2, column = 3, row = 1, prerequisite = -1)
    public int getSavageStrikes() {
        return data[55];
    }

    public void setSavageStrikes(int value) {
        data[55] = value;
    }

    @TalentDataAttribute(index = 56, name = "Surefooted", maxPoints = 3, tree = 2, column = 1, row = 2, prerequisite = -1)
    public int getSurefooted() {
        return data[56];
    }

    public void setSurefooted(int value) {
        data[56] = value;
    }

    @TalentDataAttribute(index = 57, name = "Entrapment", maxPoints = 3, tree = 2, column = 2, row = 2, prerequisite = -1)
    public int getEntrapment() {
        return data[57];
    }

    public void setEntrapment(int value) {
        data[57] = value;
    }

    @TalentDataAttribute(index = 58, name = "Trap Mastery", maxPoints = 3, tree = 2, column = 3, row = 2, prerequisite = -1)
    public int getTrapMastery() {
        return data[58];
    }

    public void setTrapMastery(int value) {
        data[58] = value;
    }

    @TalentDataAttribute(index = 59, name = "Survival Instincts", maxPoints = 2, tree = 2, column = 4, row = 2, prerequisite = -1)
    public int getSurvivalInstincts() {
        return data[59];
    }

    public void setSurvivalInstincts(int value) {
        data[59] = value;
    }

    @TalentDataAttribute(index = 60, name = "Survivalist", maxPoints = 5, tree = 2, column = 1, row = 3, prerequisite = -1)
    public int getSurvivalist() {
        return data[60];
    }

    public void setSurvivalist(int value) {
        data[60] = value;
    }

    @TalentDataAttribute(index = 61, name = "Scatter Shot", maxPoints = 1, tree = 2, column = 2, row = 3, prerequisite = -1)
    public int getScatterShot() {
        return data[61];
    }

    public void setScatterShot(int value) {
        data[61] = value;
    }

    @TalentDataAttribute(index = 62, name = "Deflection", maxPoints = 3, tree = 2, column = 3, row = 3, prerequisite = -1)
    public int getDeflection() {
        return data[62];
    }

    public void setDeflection(int value) {
        data[62] = value;
    }

    @TalentDataAttribute(index = 63, name = "Survival Tactics", maxPoints = 2, tree = 2, column = 4, row = 3, prerequisite = -1)
    public int getSurvivalTactics() {
        return data[63];
    }

    public void setSurvivalTactics(int value) {
        data[63] = value;
    }

    @TalentDataAttribute(index = 64, name = "T.N.T.", maxPoints = 3, tree = 2, column = 2, row = 4, prerequisite = -1)
    public int getTNT() {
        return data[64];
    }

    public void setTNT(int value) {
        data[64] = value;
    }

    @TalentDataAttribute(index = 65, name = "Lock and Load", maxPoints = 3, tree = 2, column = 4, row = 4, prerequisite = -1)
    public int getLockAndLoad() {
        return data[65];
    }

    public void setLockAndLoad(int value) {
        data[65] = value;
    }

    @TalentDataAttribute(index = 66, name = "Hunter vs. Wild", maxPoints = 3, tree = 2, column = 1, row = 5, prerequisite = 60)
    public int getHunterVsWild() {
        return data[66];
    }

    public void setHunterVsWild(int value) {
        data[66] = value;
    }

    @TalentDataAttribute(index = 67, name = "Killer Instinct", maxPoints = 3, tree = 2, column = 2, row = 5, prerequisite = -1)
    public int getKillerInstinct() {
        return data[67];
    }

    public void setKillerInstinct(int value) {
        data[67] = value;
    }

    @TalentDataAttribute(index = 68, name = "Counterattack", maxPoints = 1, tree = 2, column = 3, row = 5, prerequisite = 62)
    public int getCounterattack() {
        return data[68];
    }

    public void setCounterattack(int value) {
        data[68] = value;
    }

    @TalentDataAttribute(index = 69, name = "Lightning Reflexes", maxPoints = 5, tree = 2, column = 1, row = 6, prerequisite = -1)
    public int getLightningReflexes() {
        return data[69];
    }

    public void setLightningReflexes(int value) {
        data[69] = value;
    }

    @TalentDataAttribute(index = 70, name = "Resourcefulness", maxPoints = 3, tree = 2, column = 3, row = 6, prerequisite = -1)
    public int getResourcefulness() {
        return data[70];
    }

    public void setResourcefulness(int value) {
        data[70] = value;
    }

    @TalentDataAttribute(index = 71, name = "Expose Weakness", maxPoints = 3, tree = 2, column = 1, row = 7, prerequisite = 69)
    public int getExposeWeakness() {
        return data[71];
    }

    public void setExposeWeakness(int value) {
        data[71] = value;
    }

    @TalentDataAttribute(index = 72, name = "Wyvern Sting", maxPoints = 1, tree = 2, column = 2, row = 7, prerequisite = 67)
    public int getWyvernSting() {
        return data[72];
    }

    public void setWyvernSting(int value) {
        data[72] = value;
    }

    @TalentDataAttribute(index = 73, name = "Thrill of the Hunt", maxPoints = 3, tree = 2, column = 3, row = 7, prerequisite = -1)
    public int getThrillOfTheHunt() {
        return data[73];
    }

    public void setThrillOfTheHunt(int value) {
        data[73] = value;
    }

    @TalentDataAttribute(index = 74, name = "Master Tactician", maxPoints = 5, tree = 2, column = 1, row = 8, prerequisite = -1)
    public int getMasterTactician() {
        return data[74];
    }

    public void setMasterTactician(int value) {
        data[74] = value;
    }

    @TalentDataAttribute(index = 75, name = "Noxious Stings", maxPoints = 3, tree = 2, column = 2, row = 8, prerequisite = 72)
    public int getNoxiousStings() {
        return data[75];
    }

    public void setNoxiousStings(int value) {
        data[75] = value;
    }

    @TalentDataAttribute(index = 76, name = "Point of No Escape", maxPoints = 2, tree = 2, column = 1, row = 9, prerequisite = -1)
    public int getPointOfNoEscape() {
        return data[76];
    }

    public void setPointOfNoEscape(int value) {
        data[76] = value;
    }

    @TalentDataAttribute(index = 77, name = "Black Arrow", maxPoints = 1, tree = 2, column = 2, row = 9, prerequisite = -1)
    public int getBlackArrow() {
        return data[77];
    }

    public void setBlackArrow(int value) {
        data[77] = value;
    }

    @TalentDataAttribute(index = 78, name = "Sniper Training", maxPoints = 3, tree = 2, column = 4, row = 9, prerequisite = -1)
    public int getSniperTraining() {
        return data[78];
    }

    public void setSniperTraining(int value) {
        data[78] = value;
    }

    @TalentDataAttribute(index = 79, name = "Hunting Party", maxPoints = 3, tree = 2, column = 3, row = 10, prerequisite = 73)
    public int getHuntingParty() {
        return data[79];
    }

    public void setHuntingParty(int value) {
        data[79] = value;
    }

    @TalentDataAttribute(index = 80, name = "Explosive Shot", maxPoints = 1, tree = 2, column = 2, row = 11, prerequisite = 77)
    public int getExplosiveShot() {
        return data[80];
    }

    public void setExplosiveShot(int value) {
        data[80] = value;
    }

    @GlyphDataAttribute(index = 0, name = "Glyph of Aimed Shot", major = true)
    public boolean isGlyphOfAimedShot() {
        return glyphData[0];
    }

    public void setGlyphOfAimedShot(boolean value) {
        glyphData[0] = value;
    }

    @GlyphDataAttribute(index = 1, name = "Glyph of Arcane Shot", major = true)
    public boolean isGlyphOfArcaneShot() {
        return glyphData[1];
    }

    public void setGlyphOfArcaneShot(boolean value) {
        glyphData[1] = value;
    }

    @GlyphDataAttribute(index = 2, name = "Glyph of Aspect of the Viper", major = true)
    public boolean isGlyphOfAspectOfTheViper() {
        return glyphData[2];
    }

    public void setGlyphOfAspectOfTheViper(boolean value) {
        glyphData[2] = value;
    }

    @GlyphDataAttribute(index = 3, name = "Glyph of Bestial Wrath", major = true)
    public boolean isGlyphOfBestialWrath() {
        return glyphData[3];
    }

    public void setGlyphOfBestialWrath(boolean value) {
        glyphData[3] = value;
    }

    @GlyphDataAttribute(index = 4, name = "Glyph of Chimera Shot", major = true)
    public boolean isGlyphOfChimeraShot() {
        return glyphData[4];
    }

    public void setGlyphOfChimeraShot(boolean value) {
        glyphData[4] = value;
    }

    @GlyphDataAttribute(index = 5, name = "Glyph of Deterrence", major = true)
    public boolean isGlyphOfDeterrence() {
        return glyphData[5];
    }

    public void setGlyphOfDeterrence(boolean value) {
        glyphData[5] = value;
    }

    @GlyphDataAttribute(index = 6, name = "Glyph of Disengage", major = true)
    public boolean isGlyphOfDisengage() {
        return glyphData[6];
    }

    public void setGlyphOfDisengage(boolean value) {
        glyphData[6] = value;
    }

    @GlyphDataAttribute(index = 7, name = "Glyph of Explosive Shot", major = true)
    public boolean isGlyphOfExplosiveShot() {
        return glyphData[7];
    }

    public void setGlyphOfExplosiveShot(boolean value) {
        glyphData[7] = value;
    }

    @GlyphDataAttribute(index = 8, name = "Glyph of Explosive Trap", major = true)
    public boolean isGlyphOfExplosiveTrap() {
        return glyphData[8];
    }

    public void setGlyphOfExplosiveTrap(boolean value) {
        glyphData[8] = value;
    }

    @GlyphDataAttribute(index = 9, name = "Glyph of Freezing Trap", major = true)
    public boolean isGlyphOfFreezingTrap() {
        return glyphData[9];
    }

    public void setGlyphOfFreezingTrap(boolean value) {
        glyphData[9] = value;
    }

    @GlyphDataAttribute(index = 10, name = "Glyph of Frost Trap", major = true)
    public boolean isGlyphOfFrostTrap() {
        return glyphData[10];
    }

    public void setGlyphOfFrostTrap(boolean value) {
        glyphData[10] = value;
    }

    @GlyphDataAttribute(index = 11, name = "Glyph of Hunter's Mark", major = true)
    public boolean isGlyphOfHuntersMark() {
        return glyphData[11];
    }

    public void setGlyphOfHuntersMark(boolean value) {
        glyphData[11] = value;
    }

    @GlyphDataAttribute(index = 12, name = "Glyph of Immolation Trap", major = true)
    public boolean isGlyphOfImmolationTrap() {
        return glyphData[12];
    }

    public void setGlyphOfImmolationTrap(boolean value) {
        glyphData[12] = value;
    }

    @GlyphDataAttribute(index = 13, name = "Glyph of Kill Shot", major = true)
    public boolean isGlyphOfKillShot() {
        return glyphData[13];
    }

    public void setGlyphOfKillShot(boolean value) {
        glyphData[13] = value;
    }

    @GlyphDataAttribute(index = 14, name = "Glyph of Mending", major = true)
    public boolean isGlyphOfMending() {
        return glyphData[14];
    }

    public void setGlyphOfMending(boolean value) {
        glyphData[14] = value;
    }

    @GlyphDataAttribute(index = 15, name = "Glyph of Multi-Shot", major = true)
    public boolean isGlyphOfMultiShot() {
        return glyphData[15];
    }

    public void setGlyphOfMultiShot(boolean value) {
        glyphData[15] = value;
    }

    @GlyphDataAttribute(index = 16, name = "Glyph of Rapid Fire", major = true)
    public boolean isGlyphOfRapidFire() {
        return glyphData[16];
    }

    public void setGlyphOfRapidFire(boolean value) {
        glyphData[16] = value;
    }

    @GlyphDataAttribute(index = 17, name = "Glyph of Raptor Strike", major = true)
    public boolean isGlyphOfRaptorStrike() {
        return glyphData[17];
    }

    public void setGlyphOfRaptorStrike(boolean value) {
        glyphData[17] = value;
    }

    @GlyphDataAttribute(index = 18, name = "Glyph of Scatter Shot", major = true)
    public boolean isGlyphOfScatterShot() {
        return glyphData[18];
    }

    public void setGlyphOfScatterShot(boolean value) {
        glyphData[18] = value;
    }

    @GlyphDataAttribute(index = 19, name = "Glyph of Serpent Sting", major = true)
    public boolean isGlyphOfSerpentSting() {
        return glyphData[19];
    }

    public void setGlyphOfSerpentSting(boolean value) {
        glyphData[19] = value;
    }

    @GlyphDataAttribute(index = 20, name = "Glyph of Snake Trap", major = true)
    public boolean isGlyphOfSnakeTrap() {
        return glyphData[20];
    }

    public void setGlyphOfSnakeTrap(boolean value) {
        glyphData[20] = value;
    }

    @GlyphDataAttribute(index = 21, name = "Glyph of Steady Shot", major = true)
    public boolean isGlyphOfSteadyShot() {
        return glyphData[21];
    }

    public void setGlyphOfSteadyShot(boolean value) {
        glyphData[21] = value;
    }

    @GlyphDataAttribute(index = 22, name = "Glyph of the Beast", major = true)
    public boolean isGlyphOfTheBeast() {
        return glyphData[22];
    }

    public void setGlyphOfTheBeast(boolean value) {
        glyphData[22] = value;
    }

    @GlyphDataAttribute(index = 23, name = "Glyph of the Hawk", major = true)
    public boolean isGlyphOfTheHawk() {
        return glyphData[23];
    }

    public void setGlyphOfTheHawk(boolean value) {
        glyphData[23] = value;
    }

    @GlyphDataAttribute(index = 24, name = "Glyph of Trueshot Aura", major = true)
    public boolean isGlyphOfTrueshotAura() {
        return glyphData[24];
    }

    public void setGlyphOfTrueshotAura(boolean value) {
        glyphData[24] = value;
    }

    @GlyphDataAttribute(index = 25, name = "Glyph of Volley", major = true)
    public boolean isGlyphOfVolley() {
        return glyphData[25];
    }

    public void setGlyphOfVolley(boolean value) {
        glyphData[25] = value;
    }

    @GlyphDataAttribute(index = 26, name = "Glyph of Wyvern Sting", major = true)
    public boolean isGlyphOfWyvernSting() {
        return glyphData[26];
    }

    public void setGlyphOfWyvernSting(boolean value) {
        glyphData[26] = value;
    }

    @GlyphDataAttribute(index = 27, name = "Glyph of Feign Death", major = false)
    public boolean isGlyphOfFeignDeath() {
        return glyphData[27];
    }

    public void setGlyphOfFeignDeath(boolean value) {
        glyphData[27] = value;
    }

    @GlyphDataAttribute(index = 28, name = "Glyph of Mend Pet", major = false)
    public boolean isGlyphOfMendPet() {
        return glyphData[28];
    }

    public void setGlyphOfMendPet(boolean value) {
        glyphData[28] = value;
    }

    @GlyphDataAttribute(index = 29, name = "Glyph of Possessed Strength", major = false)
    public boolean isGlyphOfPossessedStrength() {
        return glyphData[29];
    }

    public void setGlyphOfPossessedStrength(boolean value) {
        glyphData[29] = value;
    }

    @GlyphDataAttribute(index = 30, name = "Glyph of Revive Pet", major = false)
    public boolean isGlyphOfRevivePet() {
        return glyphData[30];
    }

    public void setGlyphOfRevivePet(boolean value) {
        glyphData[30] = value;
    }

    @GlyphDataAttribute(index = 31, name = "Glyph of Scare Beast", major = false)
    public boolean isGlyphOfScareBeast() {
        return glyphData[31];
    }

    public void setGlyphOfScareBeast(boolean value) {
        glyphData[31] = value;
    }

    @GlyphDataAttribute(index = 32, name = "Glyph of the Pack", major = false)
    public boolean isGlyphOfThePack() {
        return glyphData[32];
    }

    public void setGlyphOfThePack(boolean value) {
        glyphData[32] = value;
    }

}
