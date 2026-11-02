// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassTalents.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassGlyphs.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.talents;

/**
 * The druid's talent and glyph allocation: the Balance, Feral Combat and Restoration trees plus 25
 * glyph slots, all flagged major in this table.
 *
 * <p>{@code data} has 85 rank slots, contiguous per tree: Balance 0-27 (tree 0), Feral Combat
 * 28-57 (tree 1), Restoration 58-84 (tree 2); {@link #TreeNames} fixes that order. The four druid
 * models (bear, cat, moonkin and tree in rawr-models) share this allocation and each reads the part
 * it needs; {@link #getStarlightWrath()}, {@link #getFerocity()} and
 * {@link #getImprovedMarkOfTheWild()} are the first talent of the Balance, Feral Combat and
 * Restoration trees respectively. The glyph getters use the {@code isGlyphOfXxx()} spelling, for
 * example {@link #isGlyphOfMangle()} and {@link #isGlyphOfStarfire()}.</p>
 *
 * <p>Construction from the compact string, {@link #clone()} and the live-array accessors all
 * follow the {@link TalentsBase} contract.</p>
 */
public class DruidTalents extends TalentsBase {
    public static final String[] TreeNames = new String[] {
        "Balance",
        "Feral Combat",
        "Restoration",
    };

    private final int[] data = new int[85];
    private final boolean[] glyphData = new boolean[25];

    public DruidTalents() {
    }

    public DruidTalents(String talents) {
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

    public DruidTalents clone() {
        DruidTalents clone = new DruidTalents();
        System.arraycopy(data, 0, clone.data, 0, data.length);
        System.arraycopy(glyphData, 0, clone.glyphData, 0, glyphData.length);
        return clone;
    }

    @TalentDataAttribute(index = 0, name = "Starlight Wrath", maxPoints = 5, tree = 0, column = 2, row = 1, prerequisite = -1)
    public int getStarlightWrath() {
        return data[0];
    }

    public void setStarlightWrath(int value) {
        data[0] = value;
    }

    @TalentDataAttribute(index = 1, name = "Genesis", maxPoints = 5, tree = 0, column = 3, row = 1, prerequisite = -1)
    public int getGenesis() {
        return data[1];
    }

    public void setGenesis(int value) {
        data[1] = value;
    }

    @TalentDataAttribute(index = 2, name = "Moonglow", maxPoints = 3, tree = 0, column = 1, row = 2, prerequisite = -1)
    public int getMoonglow() {
        return data[2];
    }

    public void setMoonglow(int value) {
        data[2] = value;
    }

    @TalentDataAttribute(index = 3, name = "Nature's Majesty", maxPoints = 2, tree = 0, column = 2, row = 2, prerequisite = -1)
    public int getNaturesMajesty() {
        return data[3];
    }

    public void setNaturesMajesty(int value) {
        data[3] = value;
    }

    @TalentDataAttribute(index = 4, name = "Improved Moonfire", maxPoints = 2, tree = 0, column = 4, row = 2, prerequisite = -1)
    public int getImprovedMoonfire() {
        return data[4];
    }

    public void setImprovedMoonfire(int value) {
        data[4] = value;
    }

    @TalentDataAttribute(index = 5, name = "Brambles", maxPoints = 3, tree = 0, column = 1, row = 3, prerequisite = -1)
    public int getBrambles() {
        return data[5];
    }

    public void setBrambles(int value) {
        data[5] = value;
    }

    @TalentDataAttribute(index = 6, name = "Nature's Grace", maxPoints = 3, tree = 0, column = 2, row = 3, prerequisite = 3)
    public int getNaturesGrace() {
        return data[6];
    }

    public void setNaturesGrace(int value) {
        data[6] = value;
    }

    @TalentDataAttribute(index = 7, name = "Nature's Splendor", maxPoints = 1, tree = 0, column = 3, row = 3, prerequisite = 3)
    public int getNaturesSplendor() {
        return data[7];
    }

    public void setNaturesSplendor(int value) {
        data[7] = value;
    }

    @TalentDataAttribute(index = 8, name = "Nature's Reach", maxPoints = 2, tree = 0, column = 4, row = 3, prerequisite = -1)
    public int getNaturesReach() {
        return data[8];
    }

    public void setNaturesReach(int value) {
        data[8] = value;
    }

    @TalentDataAttribute(index = 9, name = "Vengeance", maxPoints = 5, tree = 0, column = 2, row = 4, prerequisite = -1)
    public int getVengeance() {
        return data[9];
    }

    public void setVengeance(int value) {
        data[9] = value;
    }

    @TalentDataAttribute(index = 10, name = "Celestial Focus", maxPoints = 3, tree = 0, column = 3, row = 4, prerequisite = -1)
    public int getCelestialFocus() {
        return data[10];
    }

    public void setCelestialFocus(int value) {
        data[10] = value;
    }

    @TalentDataAttribute(index = 11, name = "Lunar Guidance", maxPoints = 3, tree = 0, column = 1, row = 5, prerequisite = -1)
    public int getLunarGuidance() {
        return data[11];
    }

    public void setLunarGuidance(int value) {
        data[11] = value;
    }

    @TalentDataAttribute(index = 12, name = "Insect Swarm", maxPoints = 1, tree = 0, column = 2, row = 5, prerequisite = -1)
    public int getInsectSwarm() {
        return data[12];
    }

    public void setInsectSwarm(int value) {
        data[12] = value;
    }

    @TalentDataAttribute(index = 13, name = "Improved Insect Swarm", maxPoints = 3, tree = 0, column = 3, row = 5, prerequisite = 12)
    public int getImprovedInsectSwarm() {
        return data[13];
    }

    public void setImprovedInsectSwarm(int value) {
        data[13] = value;
    }

    @TalentDataAttribute(index = 14, name = "Dreamstate", maxPoints = 3, tree = 0, column = 1, row = 6, prerequisite = -1)
    public int getDreamstate() {
        return data[14];
    }

    public void setDreamstate(int value) {
        data[14] = value;
    }

    @TalentDataAttribute(index = 15, name = "Moonfury", maxPoints = 3, tree = 0, column = 2, row = 6, prerequisite = -1)
    public int getMoonfury() {
        return data[15];
    }

    public void setMoonfury(int value) {
        data[15] = value;
    }

    @TalentDataAttribute(index = 16, name = "Balance of Power", maxPoints = 2, tree = 0, column = 3, row = 6, prerequisite = -1)
    public int getBalanceOfPower() {
        return data[16];
    }

    public void setBalanceOfPower(int value) {
        data[16] = value;
    }

    @TalentDataAttribute(index = 17, name = "Moonkin Form", maxPoints = 1, tree = 0, column = 2, row = 7, prerequisite = -1)
    public int getMoonkinForm() {
        return data[17];
    }

    public void setMoonkinForm(int value) {
        data[17] = value;
    }

    @TalentDataAttribute(index = 18, name = "Improved Moonkin Form", maxPoints = 3, tree = 0, column = 3, row = 7, prerequisite = 17)
    public int getImprovedMoonkinForm() {
        return data[18];
    }

    public void setImprovedMoonkinForm(int value) {
        data[18] = value;
    }

    @TalentDataAttribute(index = 19, name = "Improved Faerie Fire", maxPoints = 3, tree = 0, column = 4, row = 7, prerequisite = -1)
    public int getImprovedFaerieFire() {
        return data[19];
    }

    public void setImprovedFaerieFire(int value) {
        data[19] = value;
    }

    @TalentDataAttribute(index = 20, name = "Owlkin Frenzy", maxPoints = 3, tree = 0, column = 1, row = 8, prerequisite = 17)
    public int getOwlkinFrenzy() {
        return data[20];
    }

    public void setOwlkinFrenzy(int value) {
        data[20] = value;
    }

    @TalentDataAttribute(index = 21, name = "Wrath of Cenarius", maxPoints = 5, tree = 0, column = 3, row = 8, prerequisite = -1)
    public int getWrathOfCenarius() {
        return data[21];
    }

    public void setWrathOfCenarius(int value) {
        data[21] = value;
    }

    @TalentDataAttribute(index = 22, name = "Eclipse", maxPoints = 3, tree = 0, column = 1, row = 9, prerequisite = -1)
    public int getEclipse() {
        return data[22];
    }

    public void setEclipse(int value) {
        data[22] = value;
    }

    @TalentDataAttribute(index = 23, name = "Typhoon", maxPoints = 1, tree = 0, column = 2, row = 9, prerequisite = 17)
    public int getTyphoon() {
        return data[23];
    }

    public void setTyphoon(int value) {
        data[23] = value;
    }

    @TalentDataAttribute(index = 24, name = "Force of Nature", maxPoints = 1, tree = 0, column = 3, row = 9, prerequisite = -1)
    public int getForceOfNature() {
        return data[24];
    }

    public void setForceOfNature(int value) {
        data[24] = value;
    }

    @TalentDataAttribute(index = 25, name = "Gale Winds", maxPoints = 2, tree = 0, column = 4, row = 9, prerequisite = -1)
    public int getGaleWinds() {
        return data[25];
    }

    public void setGaleWinds(int value) {
        data[25] = value;
    }

    @TalentDataAttribute(index = 26, name = "Earth and Moon", maxPoints = 3, tree = 0, column = 2, row = 10, prerequisite = -1)
    public int getEarthAndMoon() {
        return data[26];
    }

    public void setEarthAndMoon(int value) {
        data[26] = value;
    }

    @TalentDataAttribute(index = 27, name = "Starfall", maxPoints = 1, tree = 0, column = 2, row = 11, prerequisite = -1)
    public int getStarfall() {
        return data[27];
    }

    public void setStarfall(int value) {
        data[27] = value;
    }

    @TalentDataAttribute(index = 28, name = "Ferocity", maxPoints = 5, tree = 1, column = 2, row = 1, prerequisite = -1)
    public int getFerocity() {
        return data[28];
    }

    public void setFerocity(int value) {
        data[28] = value;
    }

    @TalentDataAttribute(index = 29, name = "Feral Aggression", maxPoints = 5, tree = 1, column = 3, row = 1, prerequisite = -1)
    public int getFeralAggression() {
        return data[29];
    }

    public void setFeralAggression(int value) {
        data[29] = value;
    }

    @TalentDataAttribute(index = 30, name = "Feral Instinct", maxPoints = 3, tree = 1, column = 1, row = 2, prerequisite = -1)
    public int getFeralInstinct() {
        return data[30];
    }

    public void setFeralInstinct(int value) {
        data[30] = value;
    }

    @TalentDataAttribute(index = 31, name = "Savage Fury", maxPoints = 2, tree = 1, column = 2, row = 2, prerequisite = -1)
    public int getSavageFury() {
        return data[31];
    }

    public void setSavageFury(int value) {
        data[31] = value;
    }

    @TalentDataAttribute(index = 32, name = "Thick Hide", maxPoints = 3, tree = 1, column = 3, row = 2, prerequisite = -1)
    public int getThickHide() {
        return data[32];
    }

    public void setThickHide(int value) {
        data[32] = value;
    }

    @TalentDataAttribute(index = 33, name = "Feral Swiftness", maxPoints = 2, tree = 1, column = 1, row = 3, prerequisite = -1)
    public int getFeralSwiftness() {
        return data[33];
    }

    public void setFeralSwiftness(int value) {
        data[33] = value;
    }

    @TalentDataAttribute(index = 34, name = "Survival Instincts", maxPoints = 1, tree = 1, column = 2, row = 3, prerequisite = -1)
    public int getSurvivalInstincts() {
        return data[34];
    }

    public void setSurvivalInstincts(int value) {
        data[34] = value;
    }

    @TalentDataAttribute(index = 35, name = "Sharpened Claws", maxPoints = 3, tree = 1, column = 3, row = 3, prerequisite = -1)
    public int getSharpenedClaws() {
        return data[35];
    }

    public void setSharpenedClaws(int value) {
        data[35] = value;
    }

    @TalentDataAttribute(index = 36, name = "Shredding Attacks", maxPoints = 2, tree = 1, column = 1, row = 4, prerequisite = -1)
    public int getShreddingAttacks() {
        return data[36];
    }

    public void setShreddingAttacks(int value) {
        data[36] = value;
    }

    @TalentDataAttribute(index = 37, name = "Predatory Strikes", maxPoints = 3, tree = 1, column = 2, row = 4, prerequisite = -1)
    public int getPredatoryStrikes() {
        return data[37];
    }

    public void setPredatoryStrikes(int value) {
        data[37] = value;
    }

    @TalentDataAttribute(index = 38, name = "Primal Fury", maxPoints = 2, tree = 1, column = 3, row = 4, prerequisite = 35)
    public int getPrimalFury() {
        return data[38];
    }

    public void setPrimalFury(int value) {
        data[38] = value;
    }

    @TalentDataAttribute(index = 39, name = "Primal Precision", maxPoints = 2, tree = 1, column = 4, row = 4, prerequisite = 35)
    public int getPrimalPrecision() {
        return data[39];
    }

    public void setPrimalPrecision(int value) {
        data[39] = value;
    }

    @TalentDataAttribute(index = 40, name = "Brutal Impact", maxPoints = 2, tree = 1, column = 1, row = 5, prerequisite = -1)
    public int getBrutalImpact() {
        return data[40];
    }

    public void setBrutalImpact(int value) {
        data[40] = value;
    }

    @TalentDataAttribute(index = 41, name = "Feral Charge", maxPoints = 1, tree = 1, column = 3, row = 5, prerequisite = -1)
    public int getFeralCharge() {
        return data[41];
    }

    public void setFeralCharge(int value) {
        data[41] = value;
    }

    @TalentDataAttribute(index = 42, name = "Nurturing Instinct", maxPoints = 2, tree = 1, column = 4, row = 5, prerequisite = -1)
    public int getNurturingInstinct() {
        return data[42];
    }

    public void setNurturingInstinct(int value) {
        data[42] = value;
    }

    @TalentDataAttribute(index = 43, name = "Natural Reaction", maxPoints = 3, tree = 1, column = 1, row = 6, prerequisite = -1)
    public int getNaturalReaction() {
        return data[43];
    }

    public void setNaturalReaction(int value) {
        data[43] = value;
    }

    @TalentDataAttribute(index = 44, name = "Heart of the Wild", maxPoints = 5, tree = 1, column = 2, row = 6, prerequisite = 37)
    public int getHeartOfTheWild() {
        return data[44];
    }

    public void setHeartOfTheWild(int value) {
        data[44] = value;
    }

    @TalentDataAttribute(index = 45, name = "Survival of the Fittest", maxPoints = 3, tree = 1, column = 3, row = 6, prerequisite = -1)
    public int getSurvivalOfTheFittest() {
        return data[45];
    }

    public void setSurvivalOfTheFittest(int value) {
        data[45] = value;
    }

    @TalentDataAttribute(index = 46, name = "Leader of the Pack", maxPoints = 1, tree = 1, column = 2, row = 7, prerequisite = -1)
    public int getLeaderOfThePack() {
        return data[46];
    }

    public void setLeaderOfThePack(int value) {
        data[46] = value;
    }

    @TalentDataAttribute(index = 47, name = "Improved Leader of the Pack", maxPoints = 2, tree = 1, column = 3, row = 7, prerequisite = 46)
    public int getImprovedLeaderOfThePack() {
        return data[47];
    }

    public void setImprovedLeaderOfThePack(int value) {
        data[47] = value;
    }

    @TalentDataAttribute(index = 48, name = "Primal Tenacity", maxPoints = 3, tree = 1, column = 4, row = 7, prerequisite = -1)
    public int getPrimalTenacity() {
        return data[48];
    }

    public void setPrimalTenacity(int value) {
        data[48] = value;
    }

    @TalentDataAttribute(index = 49, name = "Protector of the Pack", maxPoints = 3, tree = 1, column = 1, row = 8, prerequisite = 46)
    public int getProtectorOfThePack() {
        return data[49];
    }

    public void setProtectorOfThePack(int value) {
        data[49] = value;
    }

    @TalentDataAttribute(index = 50, name = "Predatory Instincts", maxPoints = 3, tree = 1, column = 3, row = 8, prerequisite = -1)
    public int getPredatoryInstincts() {
        return data[50];
    }

    public void setPredatoryInstincts(int value) {
        data[50] = value;
    }

    @TalentDataAttribute(index = 51, name = "Infected Wounds", maxPoints = 3, tree = 1, column = 4, row = 8, prerequisite = -1)
    public int getInfectedWounds() {
        return data[51];
    }

    public void setInfectedWounds(int value) {
        data[51] = value;
    }

    @TalentDataAttribute(index = 52, name = "King of the Jungle", maxPoints = 3, tree = 1, column = 1, row = 9, prerequisite = -1)
    public int getKingOfTheJungle() {
        return data[52];
    }

    public void setKingOfTheJungle(int value) {
        data[52] = value;
    }

    @TalentDataAttribute(index = 53, name = "Mangle", maxPoints = 1, tree = 1, column = 2, row = 9, prerequisite = 46)
    public int getMangle() {
        return data[53];
    }

    public void setMangle(int value) {
        data[53] = value;
    }

    @TalentDataAttribute(index = 54, name = "Improved Mangle", maxPoints = 3, tree = 1, column = 3, row = 9, prerequisite = 53)
    public int getImprovedMangle() {
        return data[54];
    }

    public void setImprovedMangle(int value) {
        data[54] = value;
    }

    @TalentDataAttribute(index = 55, name = "Rend and Tear", maxPoints = 5, tree = 1, column = 2, row = 10, prerequisite = -1)
    public int getRendAndTear() {
        return data[55];
    }

    public void setRendAndTear(int value) {
        data[55] = value;
    }

    @TalentDataAttribute(index = 56, name = "Primal Gore", maxPoints = 1, tree = 1, column = 3, row = 10, prerequisite = 55)
    public int getPrimalGore() {
        return data[56];
    }

    public void setPrimalGore(int value) {
        data[56] = value;
    }

    @TalentDataAttribute(index = 57, name = "Berserk", maxPoints = 1, tree = 1, column = 2, row = 11, prerequisite = -1)
    public int getBerserk() {
        return data[57];
    }

    public void setBerserk(int value) {
        data[57] = value;
    }

    @TalentDataAttribute(index = 58, name = "Improved Mark of the Wild", maxPoints = 2, tree = 2, column = 1, row = 1, prerequisite = -1)
    public int getImprovedMarkOfTheWild() {
        return data[58];
    }

    public void setImprovedMarkOfTheWild(int value) {
        data[58] = value;
    }

    @TalentDataAttribute(index = 59, name = "Nature's Focus", maxPoints = 3, tree = 2, column = 2, row = 1, prerequisite = -1)
    public int getNaturesFocus() {
        return data[59];
    }

    public void setNaturesFocus(int value) {
        data[59] = value;
    }

    @TalentDataAttribute(index = 60, name = "Furor", maxPoints = 5, tree = 2, column = 3, row = 1, prerequisite = -1)
    public int getFuror() {
        return data[60];
    }

    public void setFuror(int value) {
        data[60] = value;
    }

    @TalentDataAttribute(index = 61, name = "Naturalist", maxPoints = 5, tree = 2, column = 1, row = 2, prerequisite = -1)
    public int getNaturalist() {
        return data[61];
    }

    public void setNaturalist(int value) {
        data[61] = value;
    }

    @TalentDataAttribute(index = 62, name = "Subtlety", maxPoints = 3, tree = 2, column = 2, row = 2, prerequisite = -1)
    public int getSubtlety() {
        return data[62];
    }

    public void setSubtlety(int value) {
        data[62] = value;
    }

    @TalentDataAttribute(index = 63, name = "Natural Shapeshifter", maxPoints = 3, tree = 2, column = 3, row = 2, prerequisite = -1)
    public int getNaturalShapeshifter() {
        return data[63];
    }

    public void setNaturalShapeshifter(int value) {
        data[63] = value;
    }

    @TalentDataAttribute(index = 64, name = "Intensity", maxPoints = 3, tree = 2, column = 1, row = 3, prerequisite = -1)
    public int getIntensity() {
        return data[64];
    }

    public void setIntensity(int value) {
        data[64] = value;
    }

    @TalentDataAttribute(index = 65, name = "Omen of Clarity", maxPoints = 1, tree = 2, column = 2, row = 3, prerequisite = -1)
    public int getOmenOfClarity() {
        return data[65];
    }

    public void setOmenOfClarity(int value) {
        data[65] = value;
    }

    @TalentDataAttribute(index = 66, name = "Master Shapeshifter", maxPoints = 2, tree = 2, column = 3, row = 3, prerequisite = 63)
    public int getMasterShapeshifter() {
        return data[66];
    }

    public void setMasterShapeshifter(int value) {
        data[66] = value;
    }

    @TalentDataAttribute(index = 67, name = "Tranquil Spirit", maxPoints = 5, tree = 2, column = 2, row = 4, prerequisite = -1)
    public int getTranquilSpirit() {
        return data[67];
    }

    public void setTranquilSpirit(int value) {
        data[67] = value;
    }

    @TalentDataAttribute(index = 68, name = "Improved Rejuvenation", maxPoints = 3, tree = 2, column = 3, row = 4, prerequisite = -1)
    public int getImprovedRejuvenation() {
        return data[68];
    }

    public void setImprovedRejuvenation(int value) {
        data[68] = value;
    }

    @TalentDataAttribute(index = 69, name = "Nature's Swiftness", maxPoints = 1, tree = 2, column = 1, row = 5, prerequisite = 64)
    public int getNaturesSwiftness() {
        return data[69];
    }

    public void setNaturesSwiftness(int value) {
        data[69] = value;
    }

    @TalentDataAttribute(index = 70, name = "Gift of Nature", maxPoints = 5, tree = 2, column = 2, row = 5, prerequisite = -1)
    public int getGiftOfNature() {
        return data[70];
    }

    public void setGiftOfNature(int value) {
        data[70] = value;
    }

    @TalentDataAttribute(index = 71, name = "Improved Tranquility", maxPoints = 2, tree = 2, column = 4, row = 5, prerequisite = -1)
    public int getImprovedTranquility() {
        return data[71];
    }

    public void setImprovedTranquility(int value) {
        data[71] = value;
    }

    @TalentDataAttribute(index = 72, name = "Empowered Touch", maxPoints = 2, tree = 2, column = 1, row = 6, prerequisite = -1)
    public int getEmpoweredTouch() {
        return data[72];
    }

    public void setEmpoweredTouch(int value) {
        data[72] = value;
    }

    @TalentDataAttribute(index = 73, name = "Nature's Bounty", maxPoints = 5, tree = 2, column = 3, row = 6, prerequisite = 68)
    public int getNaturesBounty() {
        return data[73];
    }

    public void setNaturesBounty(int value) {
        data[73] = value;
    }

    @TalentDataAttribute(index = 74, name = "Living Spirit", maxPoints = 3, tree = 2, column = 1, row = 7, prerequisite = -1)
    public int getLivingSpirit() {
        return data[74];
    }

    public void setLivingSpirit(int value) {
        data[74] = value;
    }

    @TalentDataAttribute(index = 75, name = "Swiftmend", maxPoints = 1, tree = 2, column = 2, row = 7, prerequisite = 70)
    public int getSwiftmend() {
        return data[75];
    }

    public void setSwiftmend(int value) {
        data[75] = value;
    }

    @TalentDataAttribute(index = 76, name = "Natural Perfection", maxPoints = 3, tree = 2, column = 3, row = 7, prerequisite = -1)
    public int getNaturalPerfection() {
        return data[76];
    }

    public void setNaturalPerfection(int value) {
        data[76] = value;
    }

    @TalentDataAttribute(index = 77, name = "Empowered Rejuvenation", maxPoints = 5, tree = 2, column = 2, row = 8, prerequisite = -1)
    public int getEmpoweredRejuvenation() {
        return data[77];
    }

    public void setEmpoweredRejuvenation(int value) {
        data[77] = value;
    }

    @TalentDataAttribute(index = 78, name = "Living Seed", maxPoints = 3, tree = 2, column = 3, row = 8, prerequisite = -1)
    public int getLivingSeed() {
        return data[78];
    }

    public void setLivingSeed(int value) {
        data[78] = value;
    }

    @TalentDataAttribute(index = 79, name = "Revitalize", maxPoints = 3, tree = 2, column = 1, row = 9, prerequisite = -1)
    public int getRevitalize() {
        return data[79];
    }

    public void setRevitalize(int value) {
        data[79] = value;
    }

    @TalentDataAttribute(index = 80, name = "Tree of Life", maxPoints = 1, tree = 2, column = 2, row = 9, prerequisite = 77)
    public int getTreeOfLife() {
        return data[80];
    }

    public void setTreeOfLife(int value) {
        data[80] = value;
    }

    @TalentDataAttribute(index = 81, name = "Improved Tree of Life", maxPoints = 3, tree = 2, column = 3, row = 9, prerequisite = 80)
    public int getImprovedTreeOfLife() {
        return data[81];
    }

    public void setImprovedTreeOfLife(int value) {
        data[81] = value;
    }

    @TalentDataAttribute(index = 82, name = "Improved Barkskin", maxPoints = 2, tree = 2, column = 1, row = 10, prerequisite = -1)
    public int getImprovedBarkskin() {
        return data[82];
    }

    public void setImprovedBarkskin(int value) {
        data[82] = value;
    }

    @TalentDataAttribute(index = 83, name = "Gift of the Earthmother", maxPoints = 5, tree = 2, column = 3, row = 10, prerequisite = -1)
    public int getGiftOfTheEarthmother() {
        return data[83];
    }

    public void setGiftOfTheEarthmother(int value) {
        data[83] = value;
    }

    @TalentDataAttribute(index = 84, name = "Wild Growth", maxPoints = 1, tree = 2, column = 2, row = 11, prerequisite = 80)
    public int getWildGrowth() {
        return data[84];
    }

    public void setWildGrowth(int value) {
        data[84] = value;
    }

    @GlyphDataAttribute(index = 0, name = "Glyph of Mangle", major = true)
    public boolean isGlyphOfMangle() {
        return glyphData[0];
    }

    public void setGlyphOfMangle(boolean value) {
        glyphData[0] = value;
    }

    @GlyphDataAttribute(index = 1, name = "Glyph of Shred", major = true)
    public boolean isGlyphOfShred() {
        return glyphData[1];
    }

    public void setGlyphOfShred(boolean value) {
        glyphData[1] = value;
    }

    @GlyphDataAttribute(index = 2, name = "Glyph of Rip", major = true)
    public boolean isGlyphOfRip() {
        return glyphData[2];
    }

    public void setGlyphOfRip(boolean value) {
        glyphData[2] = value;
    }

    @GlyphDataAttribute(index = 3, name = "Glyph of Berserk", major = true)
    public boolean isGlyphOfBerserk() {
        return glyphData[3];
    }

    public void setGlyphOfBerserk(boolean value) {
        glyphData[3] = value;
    }

    @GlyphDataAttribute(index = 4, name = "Glyph of Savage Roar", major = true)
    public boolean isGlyphOfSavageRoar() {
        return glyphData[4];
    }

    public void setGlyphOfSavageRoar(boolean value) {
        glyphData[4] = value;
    }

    @GlyphDataAttribute(index = 5, name = "Glyph of Growl", major = true)
    public boolean isGlyphOfGrowl() {
        return glyphData[5];
    }

    public void setGlyphOfGrowl(boolean value) {
        glyphData[5] = value;
    }

    @GlyphDataAttribute(index = 6, name = "Glyph of Maul", major = true)
    public boolean isGlyphOfMaul() {
        return glyphData[6];
    }

    public void setGlyphOfMaul(boolean value) {
        glyphData[6] = value;
    }

    @GlyphDataAttribute(index = 7, name = "Glyph of Frenzied Regeneration", major = true)
    public boolean isGlyphOfFrenziedRegeneration() {
        return glyphData[7];
    }

    public void setGlyphOfFrenziedRegeneration(boolean value) {
        glyphData[7] = value;
    }

    @GlyphDataAttribute(index = 8, name = "Glyph of Focus", major = true)
    public boolean isGlyphOfFocus() {
        return glyphData[8];
    }

    public void setGlyphOfFocus(boolean value) {
        glyphData[8] = value;
    }

    @GlyphDataAttribute(index = 9, name = "Glyph of Insect Swarm", major = true)
    public boolean isGlyphOfInsectSwarm() {
        return glyphData[9];
    }

    public void setGlyphOfInsectSwarm(boolean value) {
        glyphData[9] = value;
    }

    @GlyphDataAttribute(index = 10, name = "Glyph of Monsoon", major = true)
    public boolean isGlyphOfMonsoon() {
        return glyphData[10];
    }

    public void setGlyphOfMonsoon(boolean value) {
        glyphData[10] = value;
    }

    @GlyphDataAttribute(index = 11, name = "Glyph of Moonfire", major = true)
    public boolean isGlyphOfMoonfire() {
        return glyphData[11];
    }

    public void setGlyphOfMoonfire(boolean value) {
        glyphData[11] = value;
    }

    @GlyphDataAttribute(index = 12, name = "Glyph of Starfall", major = true)
    public boolean isGlyphOfStarfall() {
        return glyphData[12];
    }

    public void setGlyphOfStarfall(boolean value) {
        glyphData[12] = value;
    }

    @GlyphDataAttribute(index = 13, name = "Glyph of Starfire", major = true)
    public boolean isGlyphOfStarfire() {
        return glyphData[13];
    }

    public void setGlyphOfStarfire(boolean value) {
        glyphData[13] = value;
    }

    @GlyphDataAttribute(index = 14, name = "Glyph of Wrath", major = true)
    public boolean isGlyphOfWrath() {
        return glyphData[14];
    }

    public void setGlyphOfWrath(boolean value) {
        glyphData[14] = value;
    }

    @GlyphDataAttribute(index = 15, name = "Glyph of Healing Touch", major = true)
    public boolean isGlyphOfHealingTouch() {
        return glyphData[15];
    }

    public void setGlyphOfHealingTouch(boolean value) {
        glyphData[15] = value;
    }

    @GlyphDataAttribute(index = 16, name = "Glyph of Innervate", major = true)
    public boolean isGlyphOfInnervate() {
        return glyphData[16];
    }

    public void setGlyphOfInnervate(boolean value) {
        glyphData[16] = value;
    }

    @GlyphDataAttribute(index = 17, name = "Glyph of Lifebloom", major = true)
    public boolean isGlyphOfLifebloom() {
        return glyphData[17];
    }

    public void setGlyphOfLifebloom(boolean value) {
        glyphData[17] = value;
    }

    @GlyphDataAttribute(index = 18, name = "Glyph of Nourish", major = true)
    public boolean isGlyphOfNourish() {
        return glyphData[18];
    }

    public void setGlyphOfNourish(boolean value) {
        glyphData[18] = value;
    }

    @GlyphDataAttribute(index = 19, name = "Glyph of Rebirth", major = true)
    public boolean isGlyphOfRebirth() {
        return glyphData[19];
    }

    public void setGlyphOfRebirth(boolean value) {
        glyphData[19] = value;
    }

    @GlyphDataAttribute(index = 20, name = "Glyph of Regrowth", major = true)
    public boolean isGlyphOfRegrowth() {
        return glyphData[20];
    }

    public void setGlyphOfRegrowth(boolean value) {
        glyphData[20] = value;
    }

    @GlyphDataAttribute(index = 21, name = "Glyph of Rejuvenation", major = true)
    public boolean isGlyphOfRejuvenation() {
        return glyphData[21];
    }

    public void setGlyphOfRejuvenation(boolean value) {
        glyphData[21] = value;
    }

    @GlyphDataAttribute(index = 22, name = "Glyph of Swiftmend", major = true)
    public boolean isGlyphOfSwiftmend() {
        return glyphData[22];
    }

    public void setGlyphOfSwiftmend(boolean value) {
        glyphData[22] = value;
    }

    @GlyphDataAttribute(index = 23, name = "Glyph of Wild Growth", major = true)
    public boolean isGlyphOfWildGrowth() {
        return glyphData[23];
    }

    public void setGlyphOfWildGrowth(boolean value) {
        glyphData[23] = value;
    }

    @GlyphDataAttribute(index = 24, name = "Glyph of Rapid Rejuvenation", major = true)
    public boolean isGlyphOfRapidRejuvenation() {
        return glyphData[24];
    }

    public void setGlyphOfRapidRejuvenation(boolean value) {
        glyphData[24] = value;
    }

}
