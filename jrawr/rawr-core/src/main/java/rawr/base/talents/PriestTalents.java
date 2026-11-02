// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassTalents.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassGlyphs.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.talents;

/**
 * The priest's talent and glyph allocation: the Discipline, Holy and Shadow trees plus 33 glyph
 * slots.
 *
 * <p>{@code data} has 82 rank slots, contiguous per tree: Discipline 0-27 (tree 0), Holy 28-54
 * (tree 1), Shadow 55-81 (tree 2); {@link #TreeNames} fixes that order. The healpriest and
 * shadowpriest models read the ranks they need, e.g. {@link #getUnbreakableWill()},
 * {@link #getHealingFocus()} and {@link #getSpiritTap()}, the first talent of each tree, and test
 * glyph flags such as {@link #isGlyphofShadowWordPain()} or {@link #isGlyphofPowerWordShield()}.</p>
 *
 * <p>{@code glyphData} has 33 flags: 27 major glyphs (0-26) then 6 minor glyphs (27-32). The glyph
 * getters keep the C# spelling {@code isGlyphofXxx()} ("of" lowercase), as in
 * {@link #isGlyphofShadowfiend()}. Construction, cloning and the live-array accessors follow
 * {@link TalentsBase}.</p>
 */
public class PriestTalents extends TalentsBase {
    public static final String[] TreeNames = new String[] {
        "Discipline",
        "Holy",
        "Shadow",
    };

    private final int[] data = new int[82];
    private final boolean[] glyphData = new boolean[33];

    public PriestTalents() {
    }

    public PriestTalents(String talents) {
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

    public PriestTalents clone() {
        PriestTalents clone = new PriestTalents();
        System.arraycopy(data, 0, clone.data, 0, data.length);
        System.arraycopy(glyphData, 0, clone.glyphData, 0, glyphData.length);
        return clone;
    }

    @TalentDataAttribute(index = 0, name = "Unbreakable Will", maxPoints = 5, tree = 0, column = 2, row = 1, prerequisite = -1)
    public int getUnbreakableWill() {
        return data[0];
    }

    public void setUnbreakableWill(int value) {
        data[0] = value;
    }

    @TalentDataAttribute(index = 1, name = "Twin Disciplines", maxPoints = 5, tree = 0, column = 3, row = 1, prerequisite = -1)
    public int getTwinDisciplines() {
        return data[1];
    }

    public void setTwinDisciplines(int value) {
        data[1] = value;
    }

    @TalentDataAttribute(index = 2, name = "Silent Resolve", maxPoints = 3, tree = 0, column = 1, row = 2, prerequisite = -1)
    public int getSilentResolve() {
        return data[2];
    }

    public void setSilentResolve(int value) {
        data[2] = value;
    }

    @TalentDataAttribute(index = 3, name = "Improved Inner Fire", maxPoints = 3, tree = 0, column = 2, row = 2, prerequisite = -1)
    public int getImprovedInnerFire() {
        return data[3];
    }

    public void setImprovedInnerFire(int value) {
        data[3] = value;
    }

    @TalentDataAttribute(index = 4, name = "Improved Power Word: Fortitude", maxPoints = 2, tree = 0, column = 3, row = 2, prerequisite = -1)
    public int getImprovedPowerWordFortitude() {
        return data[4];
    }

    public void setImprovedPowerWordFortitude(int value) {
        data[4] = value;
    }

    @TalentDataAttribute(index = 5, name = "Martyrdom", maxPoints = 2, tree = 0, column = 4, row = 2, prerequisite = -1)
    public int getMartyrdom() {
        return data[5];
    }

    public void setMartyrdom(int value) {
        data[5] = value;
    }

    @TalentDataAttribute(index = 6, name = "Meditation", maxPoints = 3, tree = 0, column = 1, row = 3, prerequisite = -1)
    public int getMeditation() {
        return data[6];
    }

    public void setMeditation(int value) {
        data[6] = value;
    }

    @TalentDataAttribute(index = 7, name = "Inner Focus", maxPoints = 1, tree = 0, column = 2, row = 3, prerequisite = -1)
    public int getInnerFocus() {
        return data[7];
    }

    public void setInnerFocus(int value) {
        data[7] = value;
    }

    @TalentDataAttribute(index = 8, name = "Improved Power Word: Shield", maxPoints = 3, tree = 0, column = 3, row = 3, prerequisite = -1)
    public int getImprovedPowerWordShield() {
        return data[8];
    }

    public void setImprovedPowerWordShield(int value) {
        data[8] = value;
    }

    @TalentDataAttribute(index = 9, name = "Absolution", maxPoints = 3, tree = 0, column = 1, row = 4, prerequisite = -1)
    public int getAbsolution() {
        return data[9];
    }

    public void setAbsolution(int value) {
        data[9] = value;
    }

    @TalentDataAttribute(index = 10, name = "Mental Agility", maxPoints = 3, tree = 0, column = 2, row = 4, prerequisite = -1)
    public int getMentalAgility() {
        return data[10];
    }

    public void setMentalAgility(int value) {
        data[10] = value;
    }

    @TalentDataAttribute(index = 11, name = "Improved Mana Burn", maxPoints = 2, tree = 0, column = 4, row = 4, prerequisite = -1)
    public int getImprovedManaBurn() {
        return data[11];
    }

    public void setImprovedManaBurn(int value) {
        data[11] = value;
    }

    @TalentDataAttribute(index = 12, name = "Reflective Shield", maxPoints = 2, tree = 0, column = 1, row = 5, prerequisite = -1)
    public int getReflectiveShield() {
        return data[12];
    }

    public void setReflectiveShield(int value) {
        data[12] = value;
    }

    @TalentDataAttribute(index = 13, name = "Mental Strength", maxPoints = 5, tree = 0, column = 2, row = 5, prerequisite = -1)
    public int getMentalStrength() {
        return data[13];
    }

    public void setMentalStrength(int value) {
        data[13] = value;
    }

    @TalentDataAttribute(index = 14, name = "Soul Warding", maxPoints = 1, tree = 0, column = 3, row = 5, prerequisite = 8)
    public int getSoulWarding() {
        return data[14];
    }

    public void setSoulWarding(int value) {
        data[14] = value;
    }

    @TalentDataAttribute(index = 15, name = "Focused Power", maxPoints = 2, tree = 0, column = 1, row = 6, prerequisite = -1)
    public int getFocusedPower() {
        return data[15];
    }

    public void setFocusedPower(int value) {
        data[15] = value;
    }

    @TalentDataAttribute(index = 16, name = "Enlightenment", maxPoints = 3, tree = 0, column = 3, row = 6, prerequisite = -1)
    public int getEnlightenment() {
        return data[16];
    }

    public void setEnlightenment(int value) {
        data[16] = value;
    }

    @TalentDataAttribute(index = 17, name = "Focused Will", maxPoints = 3, tree = 0, column = 1, row = 7, prerequisite = -1)
    public int getFocusedWill() {
        return data[17];
    }

    public void setFocusedWill(int value) {
        data[17] = value;
    }

    @TalentDataAttribute(index = 18, name = "Power Infusion", maxPoints = 1, tree = 0, column = 2, row = 7, prerequisite = 13)
    public int getPowerInfusion() {
        return data[18];
    }

    public void setPowerInfusion(int value) {
        data[18] = value;
    }

    @TalentDataAttribute(index = 19, name = "Improved Flash Heal", maxPoints = 3, tree = 0, column = 3, row = 7, prerequisite = -1)
    public int getImprovedFlashHeal() {
        return data[19];
    }

    public void setImprovedFlashHeal(int value) {
        data[19] = value;
    }

    @TalentDataAttribute(index = 20, name = "Renewed Hope", maxPoints = 2, tree = 0, column = 1, row = 8, prerequisite = -1)
    public int getRenewedHope() {
        return data[20];
    }

    public void setRenewedHope(int value) {
        data[20] = value;
    }

    @TalentDataAttribute(index = 21, name = "Rapture", maxPoints = 3, tree = 0, column = 2, row = 8, prerequisite = -1)
    public int getRapture() {
        return data[21];
    }

    public void setRapture(int value) {
        data[21] = value;
    }

    @TalentDataAttribute(index = 22, name = "Aspiration", maxPoints = 2, tree = 0, column = 3, row = 8, prerequisite = -1)
    public int getAspiration() {
        return data[22];
    }

    public void setAspiration(int value) {
        data[22] = value;
    }

    @TalentDataAttribute(index = 23, name = "Divine Aegis", maxPoints = 3, tree = 0, column = 1, row = 9, prerequisite = -1)
    public int getDivineAegis() {
        return data[23];
    }

    public void setDivineAegis(int value) {
        data[23] = value;
    }

    @TalentDataAttribute(index = 24, name = "Pain Suppression", maxPoints = 1, tree = 0, column = 2, row = 9, prerequisite = -1)
    public int getPainSuppression() {
        return data[24];
    }

    public void setPainSuppression(int value) {
        data[24] = value;
    }

    @TalentDataAttribute(index = 25, name = "Grace", maxPoints = 2, tree = 0, column = 3, row = 9, prerequisite = -1)
    public int getGrace() {
        return data[25];
    }

    public void setGrace(int value) {
        data[25] = value;
    }

    @TalentDataAttribute(index = 26, name = "Borrowed Time", maxPoints = 5, tree = 0, column = 2, row = 10, prerequisite = -1)
    public int getBorrowedTime() {
        return data[26];
    }

    public void setBorrowedTime(int value) {
        data[26] = value;
    }

    @TalentDataAttribute(index = 27, name = "Penance", maxPoints = 1, tree = 0, column = 2, row = 11, prerequisite = -1)
    public int getPenance() {
        return data[27];
    }

    public void setPenance(int value) {
        data[27] = value;
    }

    @TalentDataAttribute(index = 28, name = "Healing Focus", maxPoints = 2, tree = 1, column = 1, row = 1, prerequisite = -1)
    public int getHealingFocus() {
        return data[28];
    }

    public void setHealingFocus(int value) {
        data[28] = value;
    }

    @TalentDataAttribute(index = 29, name = "Improved Renew", maxPoints = 3, tree = 1, column = 2, row = 1, prerequisite = -1)
    public int getImprovedRenew() {
        return data[29];
    }

    public void setImprovedRenew(int value) {
        data[29] = value;
    }

    @TalentDataAttribute(index = 30, name = "Holy Specialization", maxPoints = 5, tree = 1, column = 3, row = 1, prerequisite = -1)
    public int getHolySpecialization() {
        return data[30];
    }

    public void setHolySpecialization(int value) {
        data[30] = value;
    }

    @TalentDataAttribute(index = 31, name = "Spell Warding", maxPoints = 5, tree = 1, column = 2, row = 2, prerequisite = -1)
    public int getSpellWarding() {
        return data[31];
    }

    public void setSpellWarding(int value) {
        data[31] = value;
    }

    @TalentDataAttribute(index = 32, name = "Divine Fury", maxPoints = 5, tree = 1, column = 3, row = 2, prerequisite = -1)
    public int getDivineFury() {
        return data[32];
    }

    public void setDivineFury(int value) {
        data[32] = value;
    }

    @TalentDataAttribute(index = 33, name = "Desperate Prayer", maxPoints = 1, tree = 1, column = 1, row = 3, prerequisite = -1)
    public int getDesperatePrayer() {
        return data[33];
    }

    public void setDesperatePrayer(int value) {
        data[33] = value;
    }

    @TalentDataAttribute(index = 34, name = "Blessed Recovery", maxPoints = 3, tree = 1, column = 2, row = 3, prerequisite = -1)
    public int getBlessedRecovery() {
        return data[34];
    }

    public void setBlessedRecovery(int value) {
        data[34] = value;
    }

    @TalentDataAttribute(index = 35, name = "Inspiration", maxPoints = 3, tree = 1, column = 4, row = 3, prerequisite = -1)
    public int getInspiration() {
        return data[35];
    }

    public void setInspiration(int value) {
        data[35] = value;
    }

    @TalentDataAttribute(index = 36, name = "Holy Reach", maxPoints = 2, tree = 1, column = 1, row = 4, prerequisite = -1)
    public int getHolyReach() {
        return data[36];
    }

    public void setHolyReach(int value) {
        data[36] = value;
    }

    @TalentDataAttribute(index = 37, name = "Improved Healing", maxPoints = 3, tree = 1, column = 2, row = 4, prerequisite = -1)
    public int getImprovedHealing() {
        return data[37];
    }

    public void setImprovedHealing(int value) {
        data[37] = value;
    }

    @TalentDataAttribute(index = 38, name = "Searing Light", maxPoints = 2, tree = 1, column = 3, row = 4, prerequisite = 32)
    public int getSearingLight() {
        return data[38];
    }

    public void setSearingLight(int value) {
        data[38] = value;
    }

    @TalentDataAttribute(index = 39, name = "Healing Prayers", maxPoints = 2, tree = 1, column = 1, row = 5, prerequisite = -1)
    public int getHealingPrayers() {
        return data[39];
    }

    public void setHealingPrayers(int value) {
        data[39] = value;
    }

    @TalentDataAttribute(index = 40, name = "Spirit of Redemption", maxPoints = 1, tree = 1, column = 2, row = 5, prerequisite = -1)
    public int getSpiritOfRedemption() {
        return data[40];
    }

    public void setSpiritOfRedemption(int value) {
        data[40] = value;
    }

    @TalentDataAttribute(index = 41, name = "Spiritual Guidance", maxPoints = 5, tree = 1, column = 3, row = 5, prerequisite = -1)
    public int getSpiritualGuidance() {
        return data[41];
    }

    public void setSpiritualGuidance(int value) {
        data[41] = value;
    }

    @TalentDataAttribute(index = 42, name = "Surge of Light", maxPoints = 2, tree = 1, column = 1, row = 6, prerequisite = -1)
    public int getSurgeOfLight() {
        return data[42];
    }

    public void setSurgeOfLight(int value) {
        data[42] = value;
    }

    @TalentDataAttribute(index = 43, name = "Spiritual Healing", maxPoints = 5, tree = 1, column = 3, row = 6, prerequisite = -1)
    public int getSpiritualHealing() {
        return data[43];
    }

    public void setSpiritualHealing(int value) {
        data[43] = value;
    }

    @TalentDataAttribute(index = 44, name = "Holy Concentration", maxPoints = 3, tree = 1, column = 1, row = 7, prerequisite = -1)
    public int getHolyConcentration() {
        return data[44];
    }

    public void setHolyConcentration(int value) {
        data[44] = value;
    }

    @TalentDataAttribute(index = 45, name = "Lightwell", maxPoints = 1, tree = 1, column = 2, row = 7, prerequisite = 40)
    public int getLightwell() {
        return data[45];
    }

    public void setLightwell(int value) {
        data[45] = value;
    }

    @TalentDataAttribute(index = 46, name = "Blessed Resilience", maxPoints = 3, tree = 1, column = 3, row = 7, prerequisite = -1)
    public int getBlessedResilience() {
        return data[46];
    }

    public void setBlessedResilience(int value) {
        data[46] = value;
    }

    @TalentDataAttribute(index = 47, name = "Body and Soul", maxPoints = 2, tree = 1, column = 1, row = 8, prerequisite = -1)
    public int getBodyAndSoul() {
        return data[47];
    }

    public void setBodyAndSoul(int value) {
        data[47] = value;
    }

    @TalentDataAttribute(index = 48, name = "Empowered Healing", maxPoints = 5, tree = 1, column = 2, row = 8, prerequisite = -1)
    public int getEmpoweredHealing() {
        return data[48];
    }

    public void setEmpoweredHealing(int value) {
        data[48] = value;
    }

    @TalentDataAttribute(index = 49, name = "Serendipity", maxPoints = 3, tree = 1, column = 3, row = 8, prerequisite = -1)
    public int getSerendipity() {
        return data[49];
    }

    public void setSerendipity(int value) {
        data[49] = value;
    }

    @TalentDataAttribute(index = 50, name = "Empowered Renew", maxPoints = 3, tree = 1, column = 1, row = 9, prerequisite = -1)
    public int getEmpoweredRenew() {
        return data[50];
    }

    public void setEmpoweredRenew(int value) {
        data[50] = value;
    }

    @TalentDataAttribute(index = 51, name = "Circle of Healing", maxPoints = 1, tree = 1, column = 2, row = 9, prerequisite = -1)
    public int getCircleOfHealing() {
        return data[51];
    }

    public void setCircleOfHealing(int value) {
        data[51] = value;
    }

    @TalentDataAttribute(index = 52, name = "Test of Faith", maxPoints = 3, tree = 1, column = 3, row = 9, prerequisite = -1)
    public int getTestOfFaith() {
        return data[52];
    }

    public void setTestOfFaith(int value) {
        data[52] = value;
    }

    @TalentDataAttribute(index = 53, name = "Divine Providence", maxPoints = 5, tree = 1, column = 2, row = 10, prerequisite = -1)
    public int getDivineProvidence() {
        return data[53];
    }

    public void setDivineProvidence(int value) {
        data[53] = value;
    }

    @TalentDataAttribute(index = 54, name = "Guardian Spirit", maxPoints = 1, tree = 1, column = 2, row = 11, prerequisite = -1)
    public int getGuardianSpirit() {
        return data[54];
    }

    public void setGuardianSpirit(int value) {
        data[54] = value;
    }

    @TalentDataAttribute(index = 55, name = "Spirit Tap", maxPoints = 3, tree = 2, column = 1, row = 1, prerequisite = -1)
    public int getSpiritTap() {
        return data[55];
    }

    public void setSpiritTap(int value) {
        data[55] = value;
    }

    @TalentDataAttribute(index = 56, name = "Improved Spirit Tap", maxPoints = 2, tree = 2, column = 2, row = 1, prerequisite = 55)
    public int getImprovedSpiritTap() {
        return data[56];
    }

    public void setImprovedSpiritTap(int value) {
        data[56] = value;
    }

    @TalentDataAttribute(index = 57, name = "Darkness", maxPoints = 5, tree = 2, column = 3, row = 1, prerequisite = -1)
    public int getDarkness() {
        return data[57];
    }

    public void setDarkness(int value) {
        data[57] = value;
    }

    @TalentDataAttribute(index = 58, name = "Shadow Affinity", maxPoints = 3, tree = 2, column = 1, row = 2, prerequisite = -1)
    public int getShadowAffinity() {
        return data[58];
    }

    public void setShadowAffinity(int value) {
        data[58] = value;
    }

    @TalentDataAttribute(index = 59, name = "Improved Shadow Word: Pain", maxPoints = 2, tree = 2, column = 2, row = 2, prerequisite = -1)
    public int getImprovedShadowWordPain() {
        return data[59];
    }

    public void setImprovedShadowWordPain(int value) {
        data[59] = value;
    }

    @TalentDataAttribute(index = 60, name = "Shadow Focus", maxPoints = 3, tree = 2, column = 3, row = 2, prerequisite = -1)
    public int getShadowFocus() {
        return data[60];
    }

    public void setShadowFocus(int value) {
        data[60] = value;
    }

    @TalentDataAttribute(index = 61, name = "Improved Psychic Scream", maxPoints = 2, tree = 2, column = 1, row = 3, prerequisite = -1)
    public int getImprovedPsychicScream() {
        return data[61];
    }

    public void setImprovedPsychicScream(int value) {
        data[61] = value;
    }

    @TalentDataAttribute(index = 62, name = "Improved Mind Blast", maxPoints = 5, tree = 2, column = 2, row = 3, prerequisite = -1)
    public int getImprovedMindBlast() {
        return data[62];
    }

    public void setImprovedMindBlast(int value) {
        data[62] = value;
    }

    @TalentDataAttribute(index = 63, name = "Mind Flay", maxPoints = 1, tree = 2, column = 3, row = 3, prerequisite = -1)
    public int getMindFlay() {
        return data[63];
    }

    public void setMindFlay(int value) {
        data[63] = value;
    }

    @TalentDataAttribute(index = 64, name = "Veiled Shadows", maxPoints = 2, tree = 2, column = 2, row = 4, prerequisite = -1)
    public int getVeiledShadows() {
        return data[64];
    }

    public void setVeiledShadows(int value) {
        data[64] = value;
    }

    @TalentDataAttribute(index = 65, name = "Shadow Reach", maxPoints = 2, tree = 2, column = 3, row = 4, prerequisite = -1)
    public int getShadowReach() {
        return data[65];
    }

    public void setShadowReach(int value) {
        data[65] = value;
    }

    @TalentDataAttribute(index = 66, name = "Shadow Weaving", maxPoints = 3, tree = 2, column = 4, row = 4, prerequisite = -1)
    public int getShadowWeaving() {
        return data[66];
    }

    public void setShadowWeaving(int value) {
        data[66] = value;
    }

    @TalentDataAttribute(index = 67, name = "Silence", maxPoints = 1, tree = 2, column = 1, row = 5, prerequisite = 61)
    public int getSilence() {
        return data[67];
    }

    public void setSilence(int value) {
        data[67] = value;
    }

    @TalentDataAttribute(index = 68, name = "Vampiric Embrace", maxPoints = 1, tree = 2, column = 2, row = 5, prerequisite = -1)
    public int getVampiricEmbrace() {
        return data[68];
    }

    public void setVampiricEmbrace(int value) {
        data[68] = value;
    }

    @TalentDataAttribute(index = 69, name = "Improved Vampiric Embrace", maxPoints = 2, tree = 2, column = 3, row = 5, prerequisite = 68)
    public int getImprovedVampiricEmbrace() {
        return data[69];
    }

    public void setImprovedVampiricEmbrace(int value) {
        data[69] = value;
    }

    @TalentDataAttribute(index = 70, name = "Focused Mind", maxPoints = 3, tree = 2, column = 4, row = 5, prerequisite = -1)
    public int getFocusedMind() {
        return data[70];
    }

    public void setFocusedMind(int value) {
        data[70] = value;
    }

    @TalentDataAttribute(index = 71, name = "Mind Melt", maxPoints = 2, tree = 2, column = 1, row = 6, prerequisite = -1)
    public int getMindMelt() {
        return data[71];
    }

    public void setMindMelt(int value) {
        data[71] = value;
    }

    @TalentDataAttribute(index = 72, name = "Improved Devouring Plague", maxPoints = 3, tree = 2, column = 3, row = 6, prerequisite = -1)
    public int getImprovedDevouringPlague() {
        return data[72];
    }

    public void setImprovedDevouringPlague(int value) {
        data[72] = value;
    }

    @TalentDataAttribute(index = 73, name = "Shadowform", maxPoints = 1, tree = 2, column = 2, row = 7, prerequisite = 68)
    public int getShadowform() {
        return data[73];
    }

    public void setShadowform(int value) {
        data[73] = value;
    }

    @TalentDataAttribute(index = 74, name = "Shadow Power", maxPoints = 5, tree = 2, column = 3, row = 7, prerequisite = -1)
    public int getShadowPower() {
        return data[74];
    }

    public void setShadowPower(int value) {
        data[74] = value;
    }

    @TalentDataAttribute(index = 75, name = "Improved Shadowform", maxPoints = 2, tree = 2, column = 1, row = 8, prerequisite = 73)
    public int getImprovedShadowform() {
        return data[75];
    }

    public void setImprovedShadowform(int value) {
        data[75] = value;
    }

    @TalentDataAttribute(index = 76, name = "Misery", maxPoints = 3, tree = 2, column = 3, row = 8, prerequisite = -1)
    public int getMisery() {
        return data[76];
    }

    public void setMisery(int value) {
        data[76] = value;
    }

    @TalentDataAttribute(index = 77, name = "Psychic Horror", maxPoints = 1, tree = 2, column = 1, row = 9, prerequisite = -1)
    public int getPsychicHorror() {
        return data[77];
    }

    public void setPsychicHorror(int value) {
        data[77] = value;
    }

    @TalentDataAttribute(index = 78, name = "Vampiric Touch", maxPoints = 1, tree = 2, column = 2, row = 9, prerequisite = 73)
    public int getVampiricTouch() {
        return data[78];
    }

    public void setVampiricTouch(int value) {
        data[78] = value;
    }

    @TalentDataAttribute(index = 79, name = "Pain and Suffering", maxPoints = 3, tree = 2, column = 3, row = 9, prerequisite = -1)
    public int getPainAndSuffering() {
        return data[79];
    }

    public void setPainAndSuffering(int value) {
        data[79] = value;
    }

    @TalentDataAttribute(index = 80, name = "Twisted Faith", maxPoints = 5, tree = 2, column = 3, row = 10, prerequisite = -1)
    public int getTwistedFaith() {
        return data[80];
    }

    public void setTwistedFaith(int value) {
        data[80] = value;
    }

    @TalentDataAttribute(index = 81, name = "Dispersion", maxPoints = 1, tree = 2, column = 2, row = 11, prerequisite = 78)
    public int getDispersion() {
        return data[81];
    }

    public void setDispersion(int value) {
        data[81] = value;
    }

    @GlyphDataAttribute(index = 0, name = "Glyph of Circle of Healing", major = true)
    public boolean isGlyphofCircleOfHealing() {
        return glyphData[0];
    }

    public void setGlyphofCircleOfHealing(boolean value) {
        glyphData[0] = value;
    }

    @GlyphDataAttribute(index = 1, name = "Glyph of Dispel Magic", major = true)
    public boolean isGlyphofDispelMagic() {
        return glyphData[1];
    }

    public void setGlyphofDispelMagic(boolean value) {
        glyphData[1] = value;
    }

    @GlyphDataAttribute(index = 2, name = "Glyph of Dispersion", major = true)
    public boolean isGlyphofDispersion() {
        return glyphData[2];
    }

    public void setGlyphofDispersion(boolean value) {
        glyphData[2] = value;
    }

    @GlyphDataAttribute(index = 3, name = "Glyph of Fade", major = true)
    public boolean isGlyphofFade() {
        return glyphData[3];
    }

    public void setGlyphofFade(boolean value) {
        glyphData[3] = value;
    }

    @GlyphDataAttribute(index = 4, name = "Glyph of Fear Ward", major = true)
    public boolean isGlyphofFearWard() {
        return glyphData[4];
    }

    public void setGlyphofFearWard(boolean value) {
        glyphData[4] = value;
    }

    @GlyphDataAttribute(index = 5, name = "Glyph of Flash Heal", major = true)
    public boolean isGlyphofFlashHeal() {
        return glyphData[5];
    }

    public void setGlyphofFlashHeal(boolean value) {
        glyphData[5] = value;
    }

    @GlyphDataAttribute(index = 6, name = "Glyph of Guardian Spirit", major = true)
    public boolean isGlyphofGuardianSpirit() {
        return glyphData[6];
    }

    public void setGlyphofGuardianSpirit(boolean value) {
        glyphData[6] = value;
    }

    @GlyphDataAttribute(index = 7, name = "Glyph of Holy Nova", major = true)
    public boolean isGlyphofHolyNova() {
        return glyphData[7];
    }

    public void setGlyphofHolyNova(boolean value) {
        glyphData[7] = value;
    }

    @GlyphDataAttribute(index = 8, name = "Glyph of Hymn of Hope", major = true)
    public boolean isGlyphofHymnofHope() {
        return glyphData[8];
    }

    public void setGlyphofHymnofHope(boolean value) {
        glyphData[8] = value;
    }

    @GlyphDataAttribute(index = 9, name = "Glyph of Inner Fire", major = true)
    public boolean isGlyphofInnerFire() {
        return glyphData[9];
    }

    public void setGlyphofInnerFire(boolean value) {
        glyphData[9] = value;
    }

    @GlyphDataAttribute(index = 10, name = "Glyph of Lightwell", major = true)
    public boolean isGlyphofLightwell() {
        return glyphData[10];
    }

    public void setGlyphofLightwell(boolean value) {
        glyphData[10] = value;
    }

    @GlyphDataAttribute(index = 11, name = "Glyph of Mass Dispel", major = true)
    public boolean isGlyphofMassDispel() {
        return glyphData[11];
    }

    public void setGlyphofMassDispel(boolean value) {
        glyphData[11] = value;
    }

    @GlyphDataAttribute(index = 12, name = "Glyph of Mind Control", major = true)
    public boolean isGlyphofMindControl() {
        return glyphData[12];
    }

    public void setGlyphofMindControl(boolean value) {
        glyphData[12] = value;
    }

    @GlyphDataAttribute(index = 13, name = "Glyph of Mind Flay", major = true)
    public boolean isGlyphofMindFlay() {
        return glyphData[13];
    }

    public void setGlyphofMindFlay(boolean value) {
        glyphData[13] = value;
    }

    @GlyphDataAttribute(index = 14, name = "Glyph of Mind Sear", major = true)
    public boolean isGlyphofMindSear() {
        return glyphData[14];
    }

    public void setGlyphofMindSear(boolean value) {
        glyphData[14] = value;
    }

    @GlyphDataAttribute(index = 15, name = "Glyph of Pain Suppression", major = true)
    public boolean isGlyphofPainSuppression() {
        return glyphData[15];
    }

    public void setGlyphofPainSuppression(boolean value) {
        glyphData[15] = value;
    }

    @GlyphDataAttribute(index = 16, name = "Glyph of Penance", major = true)
    public boolean isGlyphofPenance() {
        return glyphData[16];
    }

    public void setGlyphofPenance(boolean value) {
        glyphData[16] = value;
    }

    @GlyphDataAttribute(index = 17, name = "Glyph of Power Word: Shield", major = true)
    public boolean isGlyphofPowerWordShield() {
        return glyphData[17];
    }

    public void setGlyphofPowerWordShield(boolean value) {
        glyphData[17] = value;
    }

    @GlyphDataAttribute(index = 18, name = "Glyph of Prayer of Healing", major = true)
    public boolean isGlyphofPrayerofHealing() {
        return glyphData[18];
    }

    public void setGlyphofPrayerofHealing(boolean value) {
        glyphData[18] = value;
    }

    @GlyphDataAttribute(index = 19, name = "Glyph of Psychic Scream", major = true)
    public boolean isGlyphofPsychicScream() {
        return glyphData[19];
    }

    public void setGlyphofPsychicScream(boolean value) {
        glyphData[19] = value;
    }

    @GlyphDataAttribute(index = 20, name = "Glyph of Renew", major = true)
    public boolean isGlyphofRenew() {
        return glyphData[20];
    }

    public void setGlyphofRenew(boolean value) {
        glyphData[20] = value;
    }

    @GlyphDataAttribute(index = 21, name = "Glyph of Scourge Imprisonment", major = true)
    public boolean isGlyphofScourgeImprisonment() {
        return glyphData[21];
    }

    public void setGlyphofScourgeImprisonment(boolean value) {
        glyphData[21] = value;
    }

    @GlyphDataAttribute(index = 22, name = "Glyph of Shadow", major = true)
    public boolean isGlyphofShadow() {
        return glyphData[22];
    }

    public void setGlyphofShadow(boolean value) {
        glyphData[22] = value;
    }

    @GlyphDataAttribute(index = 23, name = "Glyph of Shadow Word: Death", major = true)
    public boolean isGlyphofShadowWordDeath() {
        return glyphData[23];
    }

    public void setGlyphofShadowWordDeath(boolean value) {
        glyphData[23] = value;
    }

    @GlyphDataAttribute(index = 24, name = "Glyph of Shadow Word: Pain", major = true)
    public boolean isGlyphofShadowWordPain() {
        return glyphData[24];
    }

    public void setGlyphofShadowWordPain(boolean value) {
        glyphData[24] = value;
    }

    @GlyphDataAttribute(index = 25, name = "Glyph of Smite", major = true)
    public boolean isGlyphofSmite() {
        return glyphData[25];
    }

    public void setGlyphofSmite(boolean value) {
        glyphData[25] = value;
    }

    @GlyphDataAttribute(index = 26, name = "Glyph of Spirit of Redemption", major = true)
    public boolean isGlyphofSpiritofRedemption() {
        return glyphData[26];
    }

    public void setGlyphofSpiritofRedemption(boolean value) {
        glyphData[26] = value;
    }

    @GlyphDataAttribute(index = 27, name = "Glyph of Fading", major = false)
    public boolean isGlyphofFading() {
        return glyphData[27];
    }

    public void setGlyphofFading(boolean value) {
        glyphData[27] = value;
    }

    @GlyphDataAttribute(index = 28, name = "Glyph of Fortitude", major = false)
    public boolean isGlyphofFortitude() {
        return glyphData[28];
    }

    public void setGlyphofFortitude(boolean value) {
        glyphData[28] = value;
    }

    @GlyphDataAttribute(index = 29, name = "Glyph of Levitate", major = false)
    public boolean isGlyphofLevitate() {
        return glyphData[29];
    }

    public void setGlyphofLevitate(boolean value) {
        glyphData[29] = value;
    }

    @GlyphDataAttribute(index = 30, name = "Glyph of Shackle Undead", major = false)
    public boolean isGlyphofShackleUndead() {
        return glyphData[30];
    }

    public void setGlyphofShackleUndead(boolean value) {
        glyphData[30] = value;
    }

    @GlyphDataAttribute(index = 31, name = "Glyph of Shadow Protection", major = false)
    public boolean isGlyphofShadowProtection() {
        return glyphData[31];
    }

    public void setGlyphofShadowProtection(boolean value) {
        glyphData[31] = value;
    }

    @GlyphDataAttribute(index = 32, name = "Glyph of Shadowfiend", major = false)
    public boolean isGlyphofShadowfiend() {
        return glyphData[32];
    }

    public void setGlyphofShadowfiend(boolean value) {
        glyphData[32] = value;
    }

}
