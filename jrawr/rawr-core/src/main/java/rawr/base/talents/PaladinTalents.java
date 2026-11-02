// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassTalents.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassGlyphs.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.talents;

/**
 * The paladin's talent and glyph allocation: the Holy, Protection and Retribution trees plus 34
 * glyph slots.
 *
 * <p>{@code data} has 78 rank slots, contiguous per tree: Holy 0-25 (tree 0), Protection 26-51
 * (tree 1), Retribution 52-77 (tree 2); {@link #TreeNames} fixes that order. The three paladin
 * models (healadin, protpally and retribution) share this allocation and read the ranks relevant to
 * them, e.g. {@link #getSpiritualFocus()}, {@link #getDivinity()} and {@link #getDeflection()},
 * the first talent of each tree, plus glyph checks such as {@link #isGlyphOfHolyShock()} or
 * {@link #isGlyphOfCrusaderStrike()}.</p>
 *
 * <p>{@code glyphData} has 34 flags: 28 major and 6 minor, but unlike most classes the flags are
 * not grouped major-first; the ported table interleaves them in the original order, for example
 * {@code "Glyph of the Wise"} at index 16 and {@code "Glyph of Blessing of Kings"} at 23 are minor.
 * The glyph getters mix spellings, including {@link #isGlyphOftheWise()}. Construction, cloning and
 * the live-array accessors follow {@link TalentsBase}.</p>
 */
public class PaladinTalents extends TalentsBase {
    public static final String[] TreeNames = new String[] {
        "Holy",
        "Protection",
        "Retribution",
    };

    private final int[] data = new int[78];
    private final boolean[] glyphData = new boolean[34];

    public PaladinTalents() {
    }

    public PaladinTalents(String talents) {
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

    public PaladinTalents clone() {
        PaladinTalents clone = new PaladinTalents();
        System.arraycopy(data, 0, clone.data, 0, data.length);
        System.arraycopy(glyphData, 0, clone.glyphData, 0, glyphData.length);
        return clone;
    }

    @TalentDataAttribute(index = 0, name = "Spiritual Focus", maxPoints = 5, tree = 0, column = 2, row = 1, prerequisite = -1)
    public int getSpiritualFocus() {
        return data[0];
    }

    public void setSpiritualFocus(int value) {
        data[0] = value;
    }

    @TalentDataAttribute(index = 1, name = "Seals of the Pure", maxPoints = 5, tree = 0, column = 3, row = 1, prerequisite = -1)
    public int getSealsOfThePure() {
        return data[1];
    }

    public void setSealsOfThePure(int value) {
        data[1] = value;
    }

    @TalentDataAttribute(index = 2, name = "Healing Light", maxPoints = 3, tree = 0, column = 1, row = 2, prerequisite = -1)
    public int getHealingLight() {
        return data[2];
    }

    public void setHealingLight(int value) {
        data[2] = value;
    }

    @TalentDataAttribute(index = 3, name = "Divine Intellect", maxPoints = 5, tree = 0, column = 2, row = 2, prerequisite = -1)
    public int getDivineIntellect() {
        return data[3];
    }

    public void setDivineIntellect(int value) {
        data[3] = value;
    }

    @TalentDataAttribute(index = 4, name = "Unyielding Faith", maxPoints = 2, tree = 0, column = 3, row = 2, prerequisite = -1)
    public int getUnyieldingFaith() {
        return data[4];
    }

    public void setUnyieldingFaith(int value) {
        data[4] = value;
    }

    @TalentDataAttribute(index = 5, name = "Aura Mastery", maxPoints = 1, tree = 0, column = 1, row = 3, prerequisite = -1)
    public int getAuraMastery() {
        return data[5];
    }

    public void setAuraMastery(int value) {
        data[5] = value;
    }

    @TalentDataAttribute(index = 6, name = "Illumination", maxPoints = 5, tree = 0, column = 2, row = 3, prerequisite = -1)
    public int getIllumination() {
        return data[6];
    }

    public void setIllumination(int value) {
        data[6] = value;
    }

    @TalentDataAttribute(index = 7, name = "Improved Lay on Hands", maxPoints = 2, tree = 0, column = 3, row = 3, prerequisite = -1)
    public int getImprovedLayOnHands() {
        return data[7];
    }

    public void setImprovedLayOnHands(int value) {
        data[7] = value;
    }

    @TalentDataAttribute(index = 8, name = "Improved Concentration Aura", maxPoints = 3, tree = 0, column = 1, row = 4, prerequisite = -1)
    public int getImprovedConcentrationAura() {
        return data[8];
    }

    public void setImprovedConcentrationAura(int value) {
        data[8] = value;
    }

    @TalentDataAttribute(index = 9, name = "Improved Blessing of Wisdom", maxPoints = 2, tree = 0, column = 3, row = 4, prerequisite = -1)
    public int getImprovedBlessingOfWisdom() {
        return data[9];
    }

    public void setImprovedBlessingOfWisdom(int value) {
        data[9] = value;
    }

    @TalentDataAttribute(index = 10, name = "Blessed Hands", maxPoints = 2, tree = 0, column = 4, row = 4, prerequisite = -1)
    public int getBlessedHands() {
        return data[10];
    }

    public void setBlessedHands(int value) {
        data[10] = value;
    }

    @TalentDataAttribute(index = 11, name = "Pure of Heart", maxPoints = 2, tree = 0, column = 1, row = 5, prerequisite = -1)
    public int getPureOfHeart() {
        return data[11];
    }

    public void setPureOfHeart(int value) {
        data[11] = value;
    }

    @TalentDataAttribute(index = 12, name = "Divine Favor", maxPoints = 1, tree = 0, column = 2, row = 5, prerequisite = 6)
    public int getDivineFavor() {
        return data[12];
    }

    public void setDivineFavor(int value) {
        data[12] = value;
    }

    @TalentDataAttribute(index = 13, name = "Sanctified Light", maxPoints = 3, tree = 0, column = 3, row = 5, prerequisite = -1)
    public int getSanctifiedLight() {
        return data[13];
    }

    public void setSanctifiedLight(int value) {
        data[13] = value;
    }

    @TalentDataAttribute(index = 14, name = "Purifying Power", maxPoints = 2, tree = 0, column = 1, row = 6, prerequisite = -1)
    public int getPurifyingPower() {
        return data[14];
    }

    public void setPurifyingPower(int value) {
        data[14] = value;
    }

    @TalentDataAttribute(index = 15, name = "Holy Power", maxPoints = 5, tree = 0, column = 3, row = 6, prerequisite = -1)
    public int getHolyPower() {
        return data[15];
    }

    public void setHolyPower(int value) {
        data[15] = value;
    }

    @TalentDataAttribute(index = 16, name = "Light's Grace", maxPoints = 3, tree = 0, column = 1, row = 7, prerequisite = -1)
    public int getLightsGrace() {
        return data[16];
    }

    public void setLightsGrace(int value) {
        data[16] = value;
    }

    @TalentDataAttribute(index = 17, name = "Holy Shock", maxPoints = 1, tree = 0, column = 2, row = 7, prerequisite = 12)
    public int getHolyShock() {
        return data[17];
    }

    public void setHolyShock(int value) {
        data[17] = value;
    }

    @TalentDataAttribute(index = 18, name = "Blessed Life", maxPoints = 3, tree = 0, column = 3, row = 7, prerequisite = -1)
    public int getBlessedLife() {
        return data[18];
    }

    public void setBlessedLife(int value) {
        data[18] = value;
    }

    @TalentDataAttribute(index = 19, name = "Sacred Cleansing", maxPoints = 3, tree = 0, column = 1, row = 8, prerequisite = -1)
    public int getSacredCleansing() {
        return data[19];
    }

    public void setSacredCleansing(int value) {
        data[19] = value;
    }

    @TalentDataAttribute(index = 20, name = "Holy Guidance", maxPoints = 5, tree = 0, column = 3, row = 8, prerequisite = -1)
    public int getHolyGuidance() {
        return data[20];
    }

    public void setHolyGuidance(int value) {
        data[20] = value;
    }

    @TalentDataAttribute(index = 21, name = "Divine Illumination", maxPoints = 1, tree = 0, column = 1, row = 9, prerequisite = -1)
    public int getDivineIllumination() {
        return data[21];
    }

    public void setDivineIllumination(int value) {
        data[21] = value;
    }

    @TalentDataAttribute(index = 22, name = "Judgements of the Pure", maxPoints = 5, tree = 0, column = 3, row = 9, prerequisite = -1)
    public int getJudgementsOfThePure() {
        return data[22];
    }

    public void setJudgementsOfThePure(int value) {
        data[22] = value;
    }

    @TalentDataAttribute(index = 23, name = "Infusion of Light", maxPoints = 2, tree = 0, column = 2, row = 10, prerequisite = 17)
    public int getInfusionOfLight() {
        return data[23];
    }

    public void setInfusionOfLight(int value) {
        data[23] = value;
    }

    @TalentDataAttribute(index = 24, name = "Enlightened Judgements", maxPoints = 2, tree = 0, column = 3, row = 10, prerequisite = -1)
    public int getEnlightenedJudgements() {
        return data[24];
    }

    public void setEnlightenedJudgements(int value) {
        data[24] = value;
    }

    @TalentDataAttribute(index = 25, name = "Beacon of Light", maxPoints = 1, tree = 0, column = 2, row = 11, prerequisite = -1)
    public int getBeaconOfLight() {
        return data[25];
    }

    public void setBeaconOfLight(int value) {
        data[25] = value;
    }

    @TalentDataAttribute(index = 26, name = "Divinity", maxPoints = 5, tree = 1, column = 2, row = 1, prerequisite = -1)
    public int getDivinity() {
        return data[26];
    }

    public void setDivinity(int value) {
        data[26] = value;
    }

    @TalentDataAttribute(index = 27, name = "Divine Strength", maxPoints = 5, tree = 1, column = 3, row = 1, prerequisite = -1)
    public int getDivineStrength() {
        return data[27];
    }

    public void setDivineStrength(int value) {
        data[27] = value;
    }

    @TalentDataAttribute(index = 28, name = "Stoicism", maxPoints = 3, tree = 1, column = 1, row = 2, prerequisite = -1)
    public int getStoicism() {
        return data[28];
    }

    public void setStoicism(int value) {
        data[28] = value;
    }

    @TalentDataAttribute(index = 29, name = "Guardian's Favor", maxPoints = 2, tree = 1, column = 2, row = 2, prerequisite = -1)
    public int getGuardiansFavor() {
        return data[29];
    }

    public void setGuardiansFavor(int value) {
        data[29] = value;
    }

    @TalentDataAttribute(index = 30, name = "Anticipation", maxPoints = 5, tree = 1, column = 3, row = 2, prerequisite = -1)
    public int getAnticipation() {
        return data[30];
    }

    public void setAnticipation(int value) {
        data[30] = value;
    }

    @TalentDataAttribute(index = 31, name = "Divine Sacrifice", maxPoints = 1, tree = 1, column = 1, row = 3, prerequisite = -1)
    public int getDivineSacrifice() {
        return data[31];
    }

    public void setDivineSacrifice(int value) {
        data[31] = value;
    }

    @TalentDataAttribute(index = 32, name = "Improved Righteous Fury", maxPoints = 3, tree = 1, column = 2, row = 3, prerequisite = -1)
    public int getImprovedRighteousFury() {
        return data[32];
    }

    public void setImprovedRighteousFury(int value) {
        data[32] = value;
    }

    @TalentDataAttribute(index = 33, name = "Toughness", maxPoints = 5, tree = 1, column = 3, row = 3, prerequisite = -1)
    public int getToughness() {
        return data[33];
    }

    public void setToughness(int value) {
        data[33] = value;
    }

    @TalentDataAttribute(index = 34, name = "Divine Guardian", maxPoints = 2, tree = 1, column = 1, row = 4, prerequisite = 31)
    public int getDivineGuardian() {
        return data[34];
    }

    public void setDivineGuardian(int value) {
        data[34] = value;
    }

    @TalentDataAttribute(index = 35, name = "Improved Hammer of Justice", maxPoints = 2, tree = 1, column = 2, row = 4, prerequisite = -1)
    public int getImprovedHammerOfJustice() {
        return data[35];
    }

    public void setImprovedHammerOfJustice(int value) {
        data[35] = value;
    }

    @TalentDataAttribute(index = 36, name = "Improved Devotion Aura", maxPoints = 3, tree = 1, column = 3, row = 4, prerequisite = -1)
    public int getImprovedDevotionAura() {
        return data[36];
    }

    public void setImprovedDevotionAura(int value) {
        data[36] = value;
    }

    @TalentDataAttribute(index = 37, name = "Blessing of Sanctuary", maxPoints = 1, tree = 1, column = 2, row = 5, prerequisite = -1)
    public int getBlessingOfSanctuary() {
        return data[37];
    }

    public void setBlessingOfSanctuary(int value) {
        data[37] = value;
    }

    @TalentDataAttribute(index = 38, name = "Reckoning", maxPoints = 5, tree = 1, column = 3, row = 5, prerequisite = -1)
    public int getReckoning() {
        return data[38];
    }

    public void setReckoning(int value) {
        data[38] = value;
    }

    @TalentDataAttribute(index = 39, name = "Sacred Duty", maxPoints = 2, tree = 1, column = 1, row = 6, prerequisite = -1)
    public int getSacredDuty() {
        return data[39];
    }

    public void setSacredDuty(int value) {
        data[39] = value;
    }

    @TalentDataAttribute(index = 40, name = "One-Handed Weapon Specialization", maxPoints = 3, tree = 1, column = 3, row = 6, prerequisite = -1)
    public int getOneHandedWeaponSpecialization() {
        return data[40];
    }

    public void setOneHandedWeaponSpecialization(int value) {
        data[40] = value;
    }

    @TalentDataAttribute(index = 41, name = "Spiritual Attunement", maxPoints = 2, tree = 1, column = 1, row = 7, prerequisite = -1)
    public int getSpiritualAttunement() {
        return data[41];
    }

    public void setSpiritualAttunement(int value) {
        data[41] = value;
    }

    @TalentDataAttribute(index = 42, name = "Holy Shield", maxPoints = 1, tree = 1, column = 2, row = 7, prerequisite = 37)
    public int getHolyShield() {
        return data[42];
    }

    public void setHolyShield(int value) {
        data[42] = value;
    }

    @TalentDataAttribute(index = 43, name = "Ardent Defender", maxPoints = 3, tree = 1, column = 3, row = 7, prerequisite = -1)
    public int getArdentDefender() {
        return data[43];
    }

    public void setArdentDefender(int value) {
        data[43] = value;
    }

    @TalentDataAttribute(index = 44, name = "Redoubt", maxPoints = 3, tree = 1, column = 1, row = 8, prerequisite = -1)
    public int getRedoubt() {
        return data[44];
    }

    public void setRedoubt(int value) {
        data[44] = value;
    }

    @TalentDataAttribute(index = 45, name = "Combat Expertise", maxPoints = 3, tree = 1, column = 3, row = 8, prerequisite = -1)
    public int getCombatExpertise() {
        return data[45];
    }

    public void setCombatExpertise(int value) {
        data[45] = value;
    }

    @TalentDataAttribute(index = 46, name = "Touched by the Light", maxPoints = 3, tree = 1, column = 1, row = 9, prerequisite = -1)
    public int getTouchedByTheLight() {
        return data[46];
    }

    public void setTouchedByTheLight(int value) {
        data[46] = value;
    }

    @TalentDataAttribute(index = 47, name = "Avenger's Shield", maxPoints = 1, tree = 1, column = 2, row = 9, prerequisite = 42)
    public int getAvengersShield() {
        return data[47];
    }

    public void setAvengersShield(int value) {
        data[47] = value;
    }

    @TalentDataAttribute(index = 48, name = "Guarded by the Light", maxPoints = 2, tree = 1, column = 3, row = 9, prerequisite = -1)
    public int getGuardedByTheLight() {
        return data[48];
    }

    public void setGuardedByTheLight(int value) {
        data[48] = value;
    }

    @TalentDataAttribute(index = 49, name = "Shield of the Templar", maxPoints = 3, tree = 1, column = 2, row = 10, prerequisite = 47)
    public int getShieldOfTheTemplar() {
        return data[49];
    }

    public void setShieldOfTheTemplar(int value) {
        data[49] = value;
    }

    @TalentDataAttribute(index = 50, name = "Judgements of the Just", maxPoints = 2, tree = 1, column = 3, row = 10, prerequisite = -1)
    public int getJudgementsOfTheJust() {
        return data[50];
    }

    public void setJudgementsOfTheJust(int value) {
        data[50] = value;
    }

    @TalentDataAttribute(index = 51, name = "Hammer of the Righteous", maxPoints = 1, tree = 1, column = 2, row = 11, prerequisite = -1)
    public int getHammerOfTheRighteous() {
        return data[51];
    }

    public void setHammerOfTheRighteous(int value) {
        data[51] = value;
    }

    @TalentDataAttribute(index = 52, name = "Deflection", maxPoints = 5, tree = 2, column = 2, row = 1, prerequisite = -1)
    public int getDeflection() {
        return data[52];
    }

    public void setDeflection(int value) {
        data[52] = value;
    }

    @TalentDataAttribute(index = 53, name = "Benediction", maxPoints = 5, tree = 2, column = 3, row = 1, prerequisite = -1)
    public int getBenediction() {
        return data[53];
    }

    public void setBenediction(int value) {
        data[53] = value;
    }

    @TalentDataAttribute(index = 54, name = "Improved Judgements", maxPoints = 2, tree = 2, column = 1, row = 2, prerequisite = -1)
    public int getImprovedJudgements() {
        return data[54];
    }

    public void setImprovedJudgements(int value) {
        data[54] = value;
    }

    @TalentDataAttribute(index = 55, name = "Heart of the Crusader", maxPoints = 3, tree = 2, column = 2, row = 2, prerequisite = -1)
    public int getHeartOfTheCrusader() {
        return data[55];
    }

    public void setHeartOfTheCrusader(int value) {
        data[55] = value;
    }

    @TalentDataAttribute(index = 56, name = "Improved Blessing of Might", maxPoints = 2, tree = 2, column = 3, row = 2, prerequisite = -1)
    public int getImprovedBlessingOfMight() {
        return data[56];
    }

    public void setImprovedBlessingOfMight(int value) {
        data[56] = value;
    }

    @TalentDataAttribute(index = 57, name = "Vindication", maxPoints = 2, tree = 2, column = 1, row = 3, prerequisite = -1)
    public int getVindication() {
        return data[57];
    }

    public void setVindication(int value) {
        data[57] = value;
    }

    @TalentDataAttribute(index = 58, name = "Conviction", maxPoints = 5, tree = 2, column = 2, row = 3, prerequisite = -1)
    public int getConviction() {
        return data[58];
    }

    public void setConviction(int value) {
        data[58] = value;
    }

    @TalentDataAttribute(index = 59, name = "Seal of Command", maxPoints = 1, tree = 2, column = 3, row = 3, prerequisite = -1)
    public int getSealOfCommand() {
        return data[59];
    }

    public void setSealOfCommand(int value) {
        data[59] = value;
    }

    @TalentDataAttribute(index = 60, name = "Pursuit of Justice", maxPoints = 2, tree = 2, column = 4, row = 3, prerequisite = -1)
    public int getPursuitOfJustice() {
        return data[60];
    }

    public void setPursuitOfJustice(int value) {
        data[60] = value;
    }

    @TalentDataAttribute(index = 61, name = "Eye for an Eye", maxPoints = 2, tree = 2, column = 1, row = 4, prerequisite = -1)
    public int getEyeForAnEye() {
        return data[61];
    }

    public void setEyeForAnEye(int value) {
        data[61] = value;
    }

    @TalentDataAttribute(index = 62, name = "Sanctity of Battle", maxPoints = 3, tree = 2, column = 3, row = 4, prerequisite = -1)
    public int getSanctityOfBattle() {
        return data[62];
    }

    public void setSanctityOfBattle(int value) {
        data[62] = value;
    }

    @TalentDataAttribute(index = 63, name = "Crusade", maxPoints = 3, tree = 2, column = 4, row = 4, prerequisite = -1)
    public int getCrusade() {
        return data[63];
    }

    public void setCrusade(int value) {
        data[63] = value;
    }

    @TalentDataAttribute(index = 64, name = "Two-Handed Weapon Specialization", maxPoints = 3, tree = 2, column = 1, row = 5, prerequisite = -1)
    public int getTwoHandedWeaponSpecialization() {
        return data[64];
    }

    public void setTwoHandedWeaponSpecialization(int value) {
        data[64] = value;
    }

    @TalentDataAttribute(index = 65, name = "Sanctified Retribution", maxPoints = 1, tree = 2, column = 3, row = 5, prerequisite = -1)
    public int getSanctifiedRetribution() {
        return data[65];
    }

    public void setSanctifiedRetribution(int value) {
        data[65] = value;
    }

    @TalentDataAttribute(index = 66, name = "Vengeance", maxPoints = 3, tree = 2, column = 2, row = 6, prerequisite = 58)
    public int getVengeance() {
        return data[66];
    }

    public void setVengeance(int value) {
        data[66] = value;
    }

    @TalentDataAttribute(index = 67, name = "Divine Purpose", maxPoints = 2, tree = 2, column = 3, row = 6, prerequisite = -1)
    public int getDivinePurpose() {
        return data[67];
    }

    public void setDivinePurpose(int value) {
        data[67] = value;
    }

    @TalentDataAttribute(index = 68, name = "The Art of War", maxPoints = 2, tree = 2, column = 1, row = 7, prerequisite = -1)
    public int getTheArtOfWar() {
        return data[68];
    }

    public void setTheArtOfWar(int value) {
        data[68] = value;
    }

    @TalentDataAttribute(index = 69, name = "Repentance", maxPoints = 1, tree = 2, column = 2, row = 7, prerequisite = -1)
    public int getRepentance() {
        return data[69];
    }

    public void setRepentance(int value) {
        data[69] = value;
    }

    @TalentDataAttribute(index = 70, name = "Judgements of the Wise", maxPoints = 3, tree = 2, column = 3, row = 7, prerequisite = -1)
    public int getJudgementsOfTheWise() {
        return data[70];
    }

    public void setJudgementsOfTheWise(int value) {
        data[70] = value;
    }

    @TalentDataAttribute(index = 71, name = "Fanaticism", maxPoints = 3, tree = 2, column = 2, row = 8, prerequisite = 69)
    public int getFanaticism() {
        return data[71];
    }

    public void setFanaticism(int value) {
        data[71] = value;
    }

    @TalentDataAttribute(index = 72, name = "Sanctified Wrath", maxPoints = 2, tree = 2, column = 3, row = 8, prerequisite = -1)
    public int getSanctifiedWrath() {
        return data[72];
    }

    public void setSanctifiedWrath(int value) {
        data[72] = value;
    }

    @TalentDataAttribute(index = 73, name = "Swift Retribution", maxPoints = 3, tree = 2, column = 1, row = 9, prerequisite = -1)
    public int getSwiftRetribution() {
        return data[73];
    }

    public void setSwiftRetribution(int value) {
        data[73] = value;
    }

    @TalentDataAttribute(index = 74, name = "Crusader Strike", maxPoints = 1, tree = 2, column = 2, row = 9, prerequisite = -1)
    public int getCrusaderStrike() {
        return data[74];
    }

    public void setCrusaderStrike(int value) {
        data[74] = value;
    }

    @TalentDataAttribute(index = 75, name = "Sheath of Light", maxPoints = 3, tree = 2, column = 3, row = 9, prerequisite = -1)
    public int getSheathOfLight() {
        return data[75];
    }

    public void setSheathOfLight(int value) {
        data[75] = value;
    }

    @TalentDataAttribute(index = 76, name = "Righteous Vengeance", maxPoints = 3, tree = 2, column = 2, row = 10, prerequisite = -1)
    public int getRighteousVengeance() {
        return data[76];
    }

    public void setRighteousVengeance(int value) {
        data[76] = value;
    }

    @TalentDataAttribute(index = 77, name = "Divine Storm", maxPoints = 1, tree = 2, column = 2, row = 11, prerequisite = -1)
    public int getDivineStorm() {
        return data[77];
    }

    public void setDivineStorm(int value) {
        data[77] = value;
    }

    @GlyphDataAttribute(index = 0, name = "Glyph of Avenging Wrath", major = true)
    public boolean isGlyphOfAvengingWrath() {
        return glyphData[0];
    }

    public void setGlyphOfAvengingWrath(boolean value) {
        glyphData[0] = value;
    }

    @GlyphDataAttribute(index = 1, name = "Glyph of Avenger's Shield", major = true)
    public boolean isGlyphOfAvengersShield() {
        return glyphData[1];
    }

    public void setGlyphOfAvengersShield(boolean value) {
        glyphData[1] = value;
    }

    @GlyphDataAttribute(index = 2, name = "Glyph of Seal of Blood", major = true)
    public boolean isGlyphOfSealOfBlood() {
        return glyphData[2];
    }

    public void setGlyphOfSealOfBlood(boolean value) {
        glyphData[2] = value;
    }

    @GlyphDataAttribute(index = 3, name = "Glyph of Seal of Righteousness", major = true)
    public boolean isGlyphOfSealOfRighteousness() {
        return glyphData[3];
    }

    public void setGlyphOfSealOfRighteousness(boolean value) {
        glyphData[3] = value;
    }

    @GlyphDataAttribute(index = 4, name = "Glyph of Seal of Vengeance", major = true)
    public boolean isGlyphOfSealOfVengeance() {
        return glyphData[4];
    }

    public void setGlyphOfSealOfVengeance(boolean value) {
        glyphData[4] = value;
    }

    @GlyphDataAttribute(index = 5, name = "Glyph of Hammer of Wrath", major = true)
    public boolean isGlyphOfHammerOfWrath() {
        return glyphData[5];
    }

    public void setGlyphOfHammerOfWrath(boolean value) {
        glyphData[5] = value;
    }

    @GlyphDataAttribute(index = 6, name = "Glyph of Beacon of Light", major = true)
    public boolean isGlyphOfBeaconOfLight() {
        return glyphData[6];
    }

    public void setGlyphOfBeaconOfLight(boolean value) {
        glyphData[6] = value;
    }

    @GlyphDataAttribute(index = 7, name = "Glyph of Divine Plea", major = true)
    public boolean isGlyphOfDivinePlea() {
        return glyphData[7];
    }

    public void setGlyphOfDivinePlea(boolean value) {
        glyphData[7] = value;
    }

    @GlyphDataAttribute(index = 8, name = "Glyph of Divine Storm", major = true)
    public boolean isGlyphOfDivineStorm() {
        return glyphData[8];
    }

    public void setGlyphOfDivineStorm(boolean value) {
        glyphData[8] = value;
    }

    @GlyphDataAttribute(index = 9, name = "Glyph of Hammer of the Righteous", major = true)
    public boolean isGlyphOfHammerOfTheRighteous() {
        return glyphData[9];
    }

    public void setGlyphOfHammerOfTheRighteous(boolean value) {
        glyphData[9] = value;
    }

    @GlyphDataAttribute(index = 10, name = "Glyph of Holy Shock", major = true)
    public boolean isGlyphOfHolyShock() {
        return glyphData[10];
    }

    public void setGlyphOfHolyShock(boolean value) {
        glyphData[10] = value;
    }

    @GlyphDataAttribute(index = 11, name = "Glyph of Salvation", major = true)
    public boolean isGlyphOfSalvation() {
        return glyphData[11];
    }

    public void setGlyphOfSalvation(boolean value) {
        glyphData[11] = value;
    }

    @GlyphDataAttribute(index = 12, name = "Glyph of Shield of Righteousness", major = true)
    public boolean isGlyphOfShieldOfRighteousness() {
        return glyphData[12];
    }

    public void setGlyphOfShieldOfRighteousness(boolean value) {
        glyphData[12] = value;
    }

    @GlyphDataAttribute(index = 13, name = "Glyph of Seal of Wisdom", major = true)
    public boolean isGlyphOfSealOfWisdom() {
        return glyphData[13];
    }

    public void setGlyphOfSealOfWisdom(boolean value) {
        glyphData[13] = value;
    }

    @GlyphDataAttribute(index = 14, name = "Glyph of Cleansing", major = true)
    public boolean isGlyphOfCleansing() {
        return glyphData[14];
    }

    public void setGlyphOfCleansing(boolean value) {
        glyphData[14] = value;
    }

    @GlyphDataAttribute(index = 15, name = "Glyph of Seal of Light", major = true)
    public boolean isGlyphOfSealOfLight() {
        return glyphData[15];
    }

    public void setGlyphOfSealOfLight(boolean value) {
        glyphData[15] = value;
    }

    @GlyphDataAttribute(index = 16, name = "Glyph of the Wise", major = false)
    public boolean isGlyphOftheWise() {
        return glyphData[16];
    }

    public void setGlyphOftheWise(boolean value) {
        glyphData[16] = value;
    }

    @GlyphDataAttribute(index = 17, name = "Glyph of Turn Evil", major = true)
    public boolean isGlyphOfTurnEvil() {
        return glyphData[17];
    }

    public void setGlyphOfTurnEvil(boolean value) {
        glyphData[17] = value;
    }

    @GlyphDataAttribute(index = 18, name = "Glyph of Consecration", major = true)
    public boolean isGlyphOfConsecration() {
        return glyphData[18];
    }

    public void setGlyphOfConsecration(boolean value) {
        glyphData[18] = value;
    }

    @GlyphDataAttribute(index = 19, name = "Glyph of Crusader Strike", major = true)
    public boolean isGlyphOfCrusaderStrike() {
        return glyphData[19];
    }

    public void setGlyphOfCrusaderStrike(boolean value) {
        glyphData[19] = value;
    }

    @GlyphDataAttribute(index = 20, name = "Glyph of Exorcism", major = true)
    public boolean isGlyphOfExorcism() {
        return glyphData[20];
    }

    public void setGlyphOfExorcism(boolean value) {
        glyphData[20] = value;
    }

    @GlyphDataAttribute(index = 21, name = "Glyph of Flash of Light", major = true)
    public boolean isGlyphOfFlashOfLight() {
        return glyphData[21];
    }

    public void setGlyphOfFlashOfLight(boolean value) {
        glyphData[21] = value;
    }

    @GlyphDataAttribute(index = 22, name = "Glyph of Seal of Command", major = true)
    public boolean isGlyphOfSealOfCommand() {
        return glyphData[22];
    }

    public void setGlyphOfSealOfCommand(boolean value) {
        glyphData[22] = value;
    }

    @GlyphDataAttribute(index = 23, name = "Glyph of Blessing of Kings", major = false)
    public boolean isGlyphOfBlessingOfKings() {
        return glyphData[23];
    }

    public void setGlyphOfBlessingOfKings(boolean value) {
        glyphData[23] = value;
    }

    @GlyphDataAttribute(index = 24, name = "Glyph of Sense Undead", major = false)
    public boolean isGlyphOfSenseUndead() {
        return glyphData[24];
    }

    public void setGlyphOfSenseUndead(boolean value) {
        glyphData[24] = value;
    }

    @GlyphDataAttribute(index = 25, name = "Glyph of Righteous Defense", major = true)
    public boolean isGlyphOfRighteousDefense() {
        return glyphData[25];
    }

    public void setGlyphOfRighteousDefense(boolean value) {
        glyphData[25] = value;
    }

    @GlyphDataAttribute(index = 26, name = "Glyph of Spiritual Attunement", major = true)
    public boolean isGlyphOfSpiritualAttunement() {
        return glyphData[26];
    }

    public void setGlyphOfSpiritualAttunement(boolean value) {
        glyphData[26] = value;
    }

    @GlyphDataAttribute(index = 27, name = "Glyph of Divinity", major = true)
    public boolean isGlyphOfDivinity() {
        return glyphData[27];
    }

    public void setGlyphOfDivinity(boolean value) {
        glyphData[27] = value;
    }

    @GlyphDataAttribute(index = 28, name = "Glyph of Blessing of Wisdom", major = false)
    public boolean isGlyphOfBlessingOfWisdom() {
        return glyphData[28];
    }

    public void setGlyphOfBlessingOfWisdom(boolean value) {
        glyphData[28] = value;
    }

    @GlyphDataAttribute(index = 29, name = "Glyph of Hammer of Justice", major = true)
    public boolean isGlyphOfHammerOfJustice() {
        return glyphData[29];
    }

    public void setGlyphOfHammerOfJustice(boolean value) {
        glyphData[29] = value;
    }

    @GlyphDataAttribute(index = 30, name = "Glyph of Lay on Hands", major = false)
    public boolean isGlyphOfLayonHands() {
        return glyphData[30];
    }

    public void setGlyphOfLayonHands(boolean value) {
        glyphData[30] = value;
    }

    @GlyphDataAttribute(index = 31, name = "Glyph of Judgement", major = true)
    public boolean isGlyphOfJudgement() {
        return glyphData[31];
    }

    public void setGlyphOfJudgement(boolean value) {
        glyphData[31] = value;
    }

    @GlyphDataAttribute(index = 32, name = "Glyph of Holy Light", major = true)
    public boolean isGlyphOfHolyLight() {
        return glyphData[32];
    }

    public void setGlyphOfHolyLight(boolean value) {
        glyphData[32] = value;
    }

    @GlyphDataAttribute(index = 33, name = "Glyph of Blessing of Might", major = false)
    public boolean isGlyphOfBlessingOfMight() {
        return glyphData[33];
    }

    public void setGlyphOfBlessingOfMight(boolean value) {
        glyphData[33] = value;
    }

}
