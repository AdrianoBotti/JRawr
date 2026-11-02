// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassTalents.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassGlyphs.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.talents;

/**
 * The warlock's talent and glyph allocation: the Affliction, Demonology and Destruction trees plus
 * 17 glyph slots, all flagged major.
 *
 * <p>{@code data} has 81 rank slots, contiguous per tree: Affliction 0-27 (tree 0), Demonology
 * 28-54 (tree 1), Destruction 55-80 (tree 2); {@link #TreeNames} fixes that order. The warlock
 * model reads ranks such as {@link #getImprovedCurseOfAgony()}, {@link #getImprovedHealthstone()}
 * and {@link #getImprovedShadowBolt()}, the first talent of each tree, and tests glyph flags in its
 * rotation solver.</p>
 *
 * <p>{@code glyphData} has 17 flags, all flagged major (no minor glyphs are modelled), and the
 * getters abbreviate the C# names: {@link #isGlyphSB()} stands for the entry named
 * {@code "Glyph of Shadowbolt"}, {@link #isGlyphCoA()} for {@code "Glyph of Curse of Agony"} and
 * {@link #isGlyphUA()} for {@code "Glyph of Unstable Affliction"}. Construction, cloning and the
 * live-array accessors follow {@link TalentsBase}.</p>
 */
public class WarlockTalents extends TalentsBase {
    public static final String[] TreeNames = new String[] {
        "Affliction",
        "Demonology",
        "Destruction",
    };

    private final int[] data = new int[81];
    private final boolean[] glyphData = new boolean[17];

    public WarlockTalents() {
    }

    public WarlockTalents(String talents) {
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

    public WarlockTalents clone() {
        WarlockTalents clone = new WarlockTalents();
        System.arraycopy(data, 0, clone.data, 0, data.length);
        System.arraycopy(glyphData, 0, clone.glyphData, 0, glyphData.length);
        return clone;
    }

    @TalentDataAttribute(index = 0, name = "Improved Curse of Agony", maxPoints = 2, tree = 0, column = 1, row = 1, prerequisite = -1)
    public int getImprovedCurseOfAgony() {
        return data[0];
    }

    public void setImprovedCurseOfAgony(int value) {
        data[0] = value;
    }

    @TalentDataAttribute(index = 1, name = "Suppression", maxPoints = 3, tree = 0, column = 2, row = 1, prerequisite = -1)
    public int getSuppression() {
        return data[1];
    }

    public void setSuppression(int value) {
        data[1] = value;
    }

    @TalentDataAttribute(index = 2, name = "Improved Corruption", maxPoints = 5, tree = 0, column = 3, row = 1, prerequisite = -1)
    public int getImprovedCorruption() {
        return data[2];
    }

    public void setImprovedCorruption(int value) {
        data[2] = value;
    }

    @TalentDataAttribute(index = 3, name = "Improved Curse of Weakness", maxPoints = 2, tree = 0, column = 1, row = 2, prerequisite = -1)
    public int getImprovedCurseOfWeakness() {
        return data[3];
    }

    public void setImprovedCurseOfWeakness(int value) {
        data[3] = value;
    }

    @TalentDataAttribute(index = 4, name = "Improved Drain Soul", maxPoints = 2, tree = 0, column = 2, row = 2, prerequisite = -1)
    public int getImprovedDrainSoul() {
        return data[4];
    }

    public void setImprovedDrainSoul(int value) {
        data[4] = value;
    }

    @TalentDataAttribute(index = 5, name = "Improved Life Tap", maxPoints = 2, tree = 0, column = 3, row = 2, prerequisite = -1)
    public int getImprovedLifeTap() {
        return data[5];
    }

    public void setImprovedLifeTap(int value) {
        data[5] = value;
    }

    @TalentDataAttribute(index = 6, name = "Soul Siphon", maxPoints = 2, tree = 0, column = 4, row = 2, prerequisite = -1)
    public int getSoulSiphon() {
        return data[6];
    }

    public void setSoulSiphon(int value) {
        data[6] = value;
    }

    @TalentDataAttribute(index = 7, name = "Improved Fear", maxPoints = 2, tree = 0, column = 1, row = 3, prerequisite = -1)
    public int getImprovedFear() {
        return data[7];
    }

    public void setImprovedFear(int value) {
        data[7] = value;
    }

    @TalentDataAttribute(index = 8, name = "Fel Concentration", maxPoints = 3, tree = 0, column = 2, row = 3, prerequisite = -1)
    public int getFelConcentration() {
        return data[8];
    }

    public void setFelConcentration(int value) {
        data[8] = value;
    }

    @TalentDataAttribute(index = 9, name = "Amplify Curse", maxPoints = 1, tree = 0, column = 3, row = 3, prerequisite = -1)
    public int getAmplifyCurse() {
        return data[9];
    }

    public void setAmplifyCurse(int value) {
        data[9] = value;
    }

    @TalentDataAttribute(index = 10, name = "Grim Reach", maxPoints = 2, tree = 0, column = 1, row = 4, prerequisite = -1)
    public int getGrimReach() {
        return data[10];
    }

    public void setGrimReach(int value) {
        data[10] = value;
    }

    @TalentDataAttribute(index = 11, name = "Nightfall", maxPoints = 2, tree = 0, column = 2, row = 4, prerequisite = -1)
    public int getNightfall() {
        return data[11];
    }

    public void setNightfall(int value) {
        data[11] = value;
    }

    @TalentDataAttribute(index = 12, name = "Empowered Corruption", maxPoints = 3, tree = 0, column = 4, row = 4, prerequisite = -1)
    public int getEmpoweredCorruption() {
        return data[12];
    }

    public void setEmpoweredCorruption(int value) {
        data[12] = value;
    }

    @TalentDataAttribute(index = 13, name = "Shadow Embrace", maxPoints = 5, tree = 0, column = 1, row = 5, prerequisite = -1)
    public int getShadowEmbrace() {
        return data[13];
    }

    public void setShadowEmbrace(int value) {
        data[13] = value;
    }

    @TalentDataAttribute(index = 14, name = "Siphon Life", maxPoints = 1, tree = 0, column = 2, row = 5, prerequisite = -1)
    public int getSiphonLife() {
        return data[14];
    }

    public void setSiphonLife(int value) {
        data[14] = value;
    }

    @TalentDataAttribute(index = 15, name = "Curse of Exhaustion", maxPoints = 1, tree = 0, column = 3, row = 5, prerequisite = 9)
    public int getCurseOfExhaustion() {
        return data[15];
    }

    public void setCurseOfExhaustion(int value) {
        data[15] = value;
    }

    @TalentDataAttribute(index = 16, name = "Improved Felhunter", maxPoints = 2, tree = 0, column = 1, row = 6, prerequisite = -1)
    public int getImprovedFelhunter() {
        return data[16];
    }

    public void setImprovedFelhunter(int value) {
        data[16] = value;
    }

    @TalentDataAttribute(index = 17, name = "Shadow Mastery", maxPoints = 5, tree = 0, column = 2, row = 6, prerequisite = 14)
    public int getShadowMastery() {
        return data[17];
    }

    public void setShadowMastery(int value) {
        data[17] = value;
    }

    @TalentDataAttribute(index = 18, name = "Eradication", maxPoints = 3, tree = 0, column = 1, row = 7, prerequisite = -1)
    public int getEradication() {
        return data[18];
    }

    public void setEradication(int value) {
        data[18] = value;
    }

    @TalentDataAttribute(index = 19, name = "Contagion", maxPoints = 5, tree = 0, column = 2, row = 7, prerequisite = -1)
    public int getContagion() {
        return data[19];
    }

    public void setContagion(int value) {
        data[19] = value;
    }

    @TalentDataAttribute(index = 20, name = "Dark Pact", maxPoints = 1, tree = 0, column = 3, row = 7, prerequisite = -1)
    public int getDarkPact() {
        return data[20];
    }

    public void setDarkPact(int value) {
        data[20] = value;
    }

    @TalentDataAttribute(index = 21, name = "Improved Howl of Terror", maxPoints = 2, tree = 0, column = 1, row = 8, prerequisite = -1)
    public int getImprovedHowlOfTerror() {
        return data[21];
    }

    public void setImprovedHowlOfTerror(int value) {
        data[21] = value;
    }

    @TalentDataAttribute(index = 22, name = "Malediction", maxPoints = 3, tree = 0, column = 3, row = 8, prerequisite = -1)
    public int getMalediction() {
        return data[22];
    }

    public void setMalediction(int value) {
        data[22] = value;
    }

    @TalentDataAttribute(index = 23, name = "Death's Embrace", maxPoints = 3, tree = 0, column = 1, row = 9, prerequisite = -1)
    public int getDeathsEmbrace() {
        return data[23];
    }

    public void setDeathsEmbrace(int value) {
        data[23] = value;
    }

    @TalentDataAttribute(index = 24, name = "Unstable Affliction", maxPoints = 1, tree = 0, column = 2, row = 9, prerequisite = 19)
    public int getUnstableAffliction() {
        return data[24];
    }

    public void setUnstableAffliction(int value) {
        data[24] = value;
    }

    @TalentDataAttribute(index = 25, name = "Pandemic", maxPoints = 1, tree = 0, column = 3, row = 9, prerequisite = 24)
    public int getPandemic() {
        return data[25];
    }

    public void setPandemic(int value) {
        data[25] = value;
    }

    @TalentDataAttribute(index = 26, name = "Everlasting Affliction", maxPoints = 5, tree = 0, column = 2, row = 10, prerequisite = -1)
    public int getEverlastingAffliction() {
        return data[26];
    }

    public void setEverlastingAffliction(int value) {
        data[26] = value;
    }

    @TalentDataAttribute(index = 27, name = "Haunt", maxPoints = 1, tree = 0, column = 2, row = 11, prerequisite = -1)
    public int getHaunt() {
        return data[27];
    }

    public void setHaunt(int value) {
        data[27] = value;
    }

    @TalentDataAttribute(index = 28, name = "Improved Healthstone", maxPoints = 2, tree = 1, column = 1, row = 1, prerequisite = -1)
    public int getImprovedHealthstone() {
        return data[28];
    }

    public void setImprovedHealthstone(int value) {
        data[28] = value;
    }

    @TalentDataAttribute(index = 29, name = "Improved Imp", maxPoints = 3, tree = 1, column = 2, row = 1, prerequisite = -1)
    public int getImprovedImp() {
        return data[29];
    }

    public void setImprovedImp(int value) {
        data[29] = value;
    }

    @TalentDataAttribute(index = 30, name = "Demonic Embrace", maxPoints = 3, tree = 1, column = 3, row = 1, prerequisite = -1)
    public int getDemonicEmbrace() {
        return data[30];
    }

    public void setDemonicEmbrace(int value) {
        data[30] = value;
    }

    @TalentDataAttribute(index = 31, name = "Fel Synergy", maxPoints = 2, tree = 1, column = 4, row = 1, prerequisite = -1)
    public int getFelSynergy() {
        return data[31];
    }

    public void setFelSynergy(int value) {
        data[31] = value;
    }

    @TalentDataAttribute(index = 32, name = "Improved Health Funnel", maxPoints = 2, tree = 1, column = 1, row = 2, prerequisite = -1)
    public int getImprovedHealthFunnel() {
        return data[32];
    }

    public void setImprovedHealthFunnel(int value) {
        data[32] = value;
    }

    @TalentDataAttribute(index = 33, name = "Demonic Brutality", maxPoints = 3, tree = 1, column = 2, row = 2, prerequisite = -1)
    public int getDemonicBrutality() {
        return data[33];
    }

    public void setDemonicBrutality(int value) {
        data[33] = value;
    }

    @TalentDataAttribute(index = 34, name = "Fel Vitality", maxPoints = 3, tree = 1, column = 3, row = 2, prerequisite = -1)
    public int getFelVitality() {
        return data[34];
    }

    public void setFelVitality(int value) {
        data[34] = value;
    }

    @TalentDataAttribute(index = 35, name = "Improved Succubus", maxPoints = 3, tree = 1, column = 1, row = 3, prerequisite = -1)
    public int getImprovedSuccubus() {
        return data[35];
    }

    public void setImprovedSuccubus(int value) {
        data[35] = value;
    }

    @TalentDataAttribute(index = 36, name = "Soul Link", maxPoints = 1, tree = 1, column = 2, row = 3, prerequisite = -1)
    public int getSoulLink() {
        return data[36];
    }

    public void setSoulLink(int value) {
        data[36] = value;
    }

    @TalentDataAttribute(index = 37, name = "Fel Domination", maxPoints = 1, tree = 1, column = 3, row = 3, prerequisite = -1)
    public int getFelDomination() {
        return data[37];
    }

    public void setFelDomination(int value) {
        data[37] = value;
    }

    @TalentDataAttribute(index = 38, name = "Demonic Aegis", maxPoints = 3, tree = 1, column = 4, row = 3, prerequisite = -1)
    public int getDemonicAegis() {
        return data[38];
    }

    public void setDemonicAegis(int value) {
        data[38] = value;
    }

    @TalentDataAttribute(index = 39, name = "Unholy Power", maxPoints = 5, tree = 1, column = 2, row = 4, prerequisite = 36)
    public int getUnholyPower() {
        return data[39];
    }

    public void setUnholyPower(int value) {
        data[39] = value;
    }

    @TalentDataAttribute(index = 40, name = "Master Summoner", maxPoints = 2, tree = 1, column = 3, row = 4, prerequisite = 37)
    public int getMasterSummoner() {
        return data[40];
    }

    public void setMasterSummoner(int value) {
        data[40] = value;
    }

    @TalentDataAttribute(index = 41, name = "Mana Feed", maxPoints = 1, tree = 1, column = 1, row = 5, prerequisite = 39)
    public int getManaFeed() {
        return data[41];
    }

    public void setManaFeed(int value) {
        data[41] = value;
    }

    @TalentDataAttribute(index = 42, name = "Master Conjuror", maxPoints = 2, tree = 1, column = 3, row = 5, prerequisite = -1)
    public int getMasterConjuror() {
        return data[42];
    }

    public void setMasterConjuror(int value) {
        data[42] = value;
    }

    @TalentDataAttribute(index = 43, name = "Master Demonologist", maxPoints = 5, tree = 1, column = 2, row = 6, prerequisite = 39)
    public int getMasterDemonologist() {
        return data[43];
    }

    public void setMasterDemonologist(int value) {
        data[43] = value;
    }

    @TalentDataAttribute(index = 44, name = "Molten Core", maxPoints = 3, tree = 1, column = 3, row = 6, prerequisite = -1)
    public int getMoltenCore() {
        return data[44];
    }

    public void setMoltenCore(int value) {
        data[44] = value;
    }

    @TalentDataAttribute(index = 45, name = "Demonic Resilience", maxPoints = 3, tree = 1, column = 1, row = 7, prerequisite = -1)
    public int getDemonicResilience() {
        return data[45];
    }

    public void setDemonicResilience(int value) {
        data[45] = value;
    }

    @TalentDataAttribute(index = 46, name = "Demonic Empowerment", maxPoints = 1, tree = 1, column = 2, row = 7, prerequisite = 43)
    public int getDemonicEmpowerment() {
        return data[46];
    }

    public void setDemonicEmpowerment(int value) {
        data[46] = value;
    }

    @TalentDataAttribute(index = 47, name = "Demonic Knowledge", maxPoints = 3, tree = 1, column = 3, row = 7, prerequisite = -1)
    public int getDemonicKnowledge() {
        return data[47];
    }

    public void setDemonicKnowledge(int value) {
        data[47] = value;
    }

    @TalentDataAttribute(index = 48, name = "Demonic Tactics", maxPoints = 5, tree = 1, column = 2, row = 8, prerequisite = -1)
    public int getDemonicTactics() {
        return data[48];
    }

    public void setDemonicTactics(int value) {
        data[48] = value;
    }

    @TalentDataAttribute(index = 49, name = "Decimation", maxPoints = 2, tree = 1, column = 3, row = 8, prerequisite = -1)
    public int getDecimation() {
        return data[49];
    }

    public void setDecimation(int value) {
        data[49] = value;
    }

    @TalentDataAttribute(index = 50, name = "Improved Demonic Tactics", maxPoints = 3, tree = 1, column = 1, row = 9, prerequisite = 48)
    public int getImprovedDemonicTactics() {
        return data[50];
    }

    public void setImprovedDemonicTactics(int value) {
        data[50] = value;
    }

    @TalentDataAttribute(index = 51, name = "Summon Felguard", maxPoints = 1, tree = 1, column = 2, row = 9, prerequisite = -1)
    public int getSummonFelguard() {
        return data[51];
    }

    public void setSummonFelguard(int value) {
        data[51] = value;
    }

    @TalentDataAttribute(index = 52, name = "Nemesis", maxPoints = 3, tree = 1, column = 3, row = 9, prerequisite = -1)
    public int getNemesis() {
        return data[52];
    }

    public void setNemesis(int value) {
        data[52] = value;
    }

    @TalentDataAttribute(index = 53, name = "Demonic Pact", maxPoints = 5, tree = 1, column = 2, row = 10, prerequisite = -1)
    public int getDemonicPact() {
        return data[53];
    }

    public void setDemonicPact(int value) {
        data[53] = value;
    }

    @TalentDataAttribute(index = 54, name = "Metamorphosis", maxPoints = 1, tree = 1, column = 2, row = 11, prerequisite = -1)
    public int getMetamorphosis() {
        return data[54];
    }

    public void setMetamorphosis(int value) {
        data[54] = value;
    }

    @TalentDataAttribute(index = 55, name = "Improved Shadow Bolt", maxPoints = 5, tree = 2, column = 2, row = 1, prerequisite = -1)
    public int getImprovedShadowBolt() {
        return data[55];
    }

    public void setImprovedShadowBolt(int value) {
        data[55] = value;
    }

    @TalentDataAttribute(index = 56, name = "Bane", maxPoints = 5, tree = 2, column = 3, row = 1, prerequisite = -1)
    public int getBane() {
        return data[56];
    }

    public void setBane(int value) {
        data[56] = value;
    }

    @TalentDataAttribute(index = 57, name = "Aftermath", maxPoints = 2, tree = 2, column = 1, row = 2, prerequisite = -1)
    public int getAftermath() {
        return data[57];
    }

    public void setAftermath(int value) {
        data[57] = value;
    }

    @TalentDataAttribute(index = 58, name = "Molten Skin", maxPoints = 3, tree = 2, column = 2, row = 2, prerequisite = -1)
    public int getMoltenSkin() {
        return data[58];
    }

    public void setMoltenSkin(int value) {
        data[58] = value;
    }

    @TalentDataAttribute(index = 59, name = "Cataclysm", maxPoints = 3, tree = 2, column = 3, row = 2, prerequisite = -1)
    public int getCataclysm() {
        return data[59];
    }

    public void setCataclysm(int value) {
        data[59] = value;
    }

    @TalentDataAttribute(index = 60, name = "Demonic Power", maxPoints = 2, tree = 2, column = 1, row = 3, prerequisite = -1)
    public int getDemonicPower() {
        return data[60];
    }

    public void setDemonicPower(int value) {
        data[60] = value;
    }

    @TalentDataAttribute(index = 61, name = "Shadowburn", maxPoints = 1, tree = 2, column = 2, row = 3, prerequisite = -1)
    public int getShadowburn() {
        return data[61];
    }

    public void setShadowburn(int value) {
        data[61] = value;
    }

    @TalentDataAttribute(index = 62, name = "Ruin", maxPoints = 5, tree = 2, column = 3, row = 3, prerequisite = -1)
    public int getRuin() {
        return data[62];
    }

    public void setRuin(int value) {
        data[62] = value;
    }

    @TalentDataAttribute(index = 63, name = "Intensity", maxPoints = 2, tree = 2, column = 1, row = 4, prerequisite = -1)
    public int getIntensity() {
        return data[63];
    }

    public void setIntensity(int value) {
        data[63] = value;
    }

    @TalentDataAttribute(index = 64, name = "Destructive Reach", maxPoints = 2, tree = 2, column = 2, row = 4, prerequisite = -1)
    public int getDestructiveReach() {
        return data[64];
    }

    public void setDestructiveReach(int value) {
        data[64] = value;
    }

    @TalentDataAttribute(index = 65, name = "Improved Searing Pain", maxPoints = 3, tree = 2, column = 4, row = 4, prerequisite = -1)
    public int getImprovedSearingPain() {
        return data[65];
    }

    public void setImprovedSearingPain(int value) {
        data[65] = value;
    }

    @TalentDataAttribute(index = 66, name = "Backlash", maxPoints = 3, tree = 2, column = 1, row = 5, prerequisite = 63)
    public int getBacklash() {
        return data[66];
    }

    public void setBacklash(int value) {
        data[66] = value;
    }

    @TalentDataAttribute(index = 67, name = "Improved Immolate", maxPoints = 3, tree = 2, column = 2, row = 5, prerequisite = -1)
    public int getImprovedImmolate() {
        return data[67];
    }

    public void setImprovedImmolate(int value) {
        data[67] = value;
    }

    @TalentDataAttribute(index = 68, name = "Devastation", maxPoints = 1, tree = 2, column = 3, row = 5, prerequisite = 62)
    public int getDevastation() {
        return data[68];
    }

    public void setDevastation(int value) {
        data[68] = value;
    }

    @TalentDataAttribute(index = 69, name = "Nether Protection", maxPoints = 3, tree = 2, column = 1, row = 6, prerequisite = -1)
    public int getNetherProtection() {
        return data[69];
    }

    public void setNetherProtection(int value) {
        data[69] = value;
    }

    @TalentDataAttribute(index = 70, name = "Emberstorm", maxPoints = 5, tree = 2, column = 3, row = 6, prerequisite = -1)
    public int getEmberstorm() {
        return data[70];
    }

    public void setEmberstorm(int value) {
        data[70] = value;
    }

    @TalentDataAttribute(index = 71, name = "Conflagrate", maxPoints = 1, tree = 2, column = 2, row = 7, prerequisite = 67)
    public int getConflagrate() {
        return data[71];
    }

    public void setConflagrate(int value) {
        data[71] = value;
    }

    @TalentDataAttribute(index = 72, name = "Soul Leech", maxPoints = 3, tree = 2, column = 3, row = 7, prerequisite = -1)
    public int getSoulLeech() {
        return data[72];
    }

    public void setSoulLeech(int value) {
        data[72] = value;
    }

    @TalentDataAttribute(index = 73, name = "Pyroclasm", maxPoints = 3, tree = 2, column = 4, row = 7, prerequisite = -1)
    public int getPyroclasm() {
        return data[73];
    }

    public void setPyroclasm(int value) {
        data[73] = value;
    }

    @TalentDataAttribute(index = 74, name = "Shadow and Flame", maxPoints = 5, tree = 2, column = 2, row = 8, prerequisite = -1)
    public int getShadowAndFlame() {
        return data[74];
    }

    public void setShadowAndFlame(int value) {
        data[74] = value;
    }

    @TalentDataAttribute(index = 75, name = "Improved Soul Leech", maxPoints = 2, tree = 2, column = 3, row = 8, prerequisite = 72)
    public int getImprovedSoulLeech() {
        return data[75];
    }

    public void setImprovedSoulLeech(int value) {
        data[75] = value;
    }

    @TalentDataAttribute(index = 76, name = "Backdraft", maxPoints = 3, tree = 2, column = 1, row = 9, prerequisite = 71)
    public int getBackdraft() {
        return data[76];
    }

    public void setBackdraft(int value) {
        data[76] = value;
    }

    @TalentDataAttribute(index = 77, name = "Shadowfury", maxPoints = 1, tree = 2, column = 2, row = 9, prerequisite = -1)
    public int getShadowfury() {
        return data[77];
    }

    public void setShadowfury(int value) {
        data[77] = value;
    }

    @TalentDataAttribute(index = 78, name = "Empowered Imp", maxPoints = 3, tree = 2, column = 3, row = 9, prerequisite = -1)
    public int getEmpoweredImp() {
        return data[78];
    }

    public void setEmpoweredImp(int value) {
        data[78] = value;
    }

    @TalentDataAttribute(index = 79, name = "Fire and Brimstone", maxPoints = 5, tree = 2, column = 2, row = 10, prerequisite = -1)
    public int getFireAndBrimstone() {
        return data[79];
    }

    public void setFireAndBrimstone(int value) {
        data[79] = value;
    }

    @TalentDataAttribute(index = 80, name = "Chaos Bolt", maxPoints = 1, tree = 2, column = 2, row = 11, prerequisite = -1)
    public int getChaosBolt() {
        return data[80];
    }

    public void setChaosBolt(int value) {
        data[80] = value;
    }

    @GlyphDataAttribute(index = 0, name = "Glyph of Chaos Bolt", major = true)
    public boolean isGlyphChaosBolt() {
        return glyphData[0];
    }

    public void setGlyphChaosBolt(boolean value) {
        glyphData[0] = value;
    }

    @GlyphDataAttribute(index = 1, name = "Glyph of Conflagrate", major = true)
    public boolean isGlyphConflag() {
        return glyphData[1];
    }

    public void setGlyphConflag(boolean value) {
        glyphData[1] = value;
    }

    @GlyphDataAttribute(index = 2, name = "Glyph of Corruption", major = true)
    public boolean isGlyphCorruption() {
        return glyphData[2];
    }

    public void setGlyphCorruption(boolean value) {
        glyphData[2] = value;
    }

    @GlyphDataAttribute(index = 3, name = "Glyph of Curse of Agony", major = true)
    public boolean isGlyphCoA() {
        return glyphData[3];
    }

    public void setGlyphCoA(boolean value) {
        glyphData[3] = value;
    }

    @GlyphDataAttribute(index = 4, name = "Glyph of Death Coil", major = true)
    public boolean isGlyphDeathCoil() {
        return glyphData[4];
    }

    public void setGlyphDeathCoil(boolean value) {
        glyphData[4] = value;
    }

    @GlyphDataAttribute(index = 5, name = "Glyph of Felguard", major = true)
    public boolean isGlyphFelguard() {
        return glyphData[5];
    }

    public void setGlyphFelguard(boolean value) {
        glyphData[5] = value;
    }

    @GlyphDataAttribute(index = 6, name = "Glyph of Haunt", major = true)
    public boolean isGlyphHaunt() {
        return glyphData[6];
    }

    public void setGlyphHaunt(boolean value) {
        glyphData[6] = value;
    }

    @GlyphDataAttribute(index = 7, name = "Glyph of Immolate", major = true)
    public boolean isGlyphImmolate() {
        return glyphData[7];
    }

    public void setGlyphImmolate(boolean value) {
        glyphData[7] = value;
    }

    @GlyphDataAttribute(index = 8, name = "Glyph of Imp", major = true)
    public boolean isGlyphImp() {
        return glyphData[8];
    }

    public void setGlyphImp(boolean value) {
        glyphData[8] = value;
    }

    @GlyphDataAttribute(index = 9, name = "Glyph of Incinerate", major = true)
    public boolean isGlyphIncinerate() {
        return glyphData[9];
    }

    public void setGlyphIncinerate(boolean value) {
        glyphData[9] = value;
    }

    @GlyphDataAttribute(index = 10, name = "Glyph of Life Tap", major = true)
    public boolean isGlyphLifeTap() {
        return glyphData[10];
    }

    public void setGlyphLifeTap(boolean value) {
        glyphData[10] = value;
    }

    @GlyphDataAttribute(index = 11, name = "Glyph of Metamorphosis", major = true)
    public boolean isGlyphMetamorphosis() {
        return glyphData[11];
    }

    public void setGlyphMetamorphosis(boolean value) {
        glyphData[11] = value;
    }

    @GlyphDataAttribute(index = 12, name = "Glyph of Quick Decay", major = true)
    public boolean isGlyphQuickDecay() {
        return glyphData[12];
    }

    public void setGlyphQuickDecay(boolean value) {
        glyphData[12] = value;
    }

    @GlyphDataAttribute(index = 13, name = "Glyph of Searing Pain", major = true)
    public boolean isGlyphSearingPain() {
        return glyphData[13];
    }

    public void setGlyphSearingPain(boolean value) {
        glyphData[13] = value;
    }

    @GlyphDataAttribute(index = 14, name = "Glyph of Shadowbolt", major = true)
    public boolean isGlyphSB() {
        return glyphData[14];
    }

    public void setGlyphSB(boolean value) {
        glyphData[14] = value;
    }

    @GlyphDataAttribute(index = 15, name = "Glyph of Shadowburn", major = true)
    public boolean isGlyphShadowburn() {
        return glyphData[15];
    }

    public void setGlyphShadowburn(boolean value) {
        glyphData[15] = value;
    }

    @GlyphDataAttribute(index = 16, name = "Glyph of Unstable Affliction", major = true)
    public boolean isGlyphUA() {
        return glyphData[16];
    }

    public void setGlyphUA(boolean value) {
        glyphData[16] = value;
    }

}
