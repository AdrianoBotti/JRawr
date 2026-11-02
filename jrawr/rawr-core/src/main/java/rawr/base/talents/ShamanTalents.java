// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassTalents.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassGlyphs.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.talents;

/**
 * The shaman's talent and glyph allocation: the Elemental, Enhancement and Restoration trees plus
 * 29 glyph slots.
 *
 * <p>{@code data} has 80 rank slots, contiguous per tree: Elemental 0-24 (tree 0), Enhancement
 * 25-53 (tree 1), Restoration 54-79 (tree 2); {@link #TreeNames} fixes that order. The elemental,
 * enhance and restosham models share this allocation; representative members are
 * {@link #getConvection()}, {@link #getEnhancingTotems()} and {@link #getImprovedHealingWave()},
 * the first talent of each tree, plus glyph flags such as {@link #isGlyphofLava()} or
 * {@link #isGlyphofRiptide()}.</p>
 *
 * <p>{@code glyphData} has 29 flags: 28 major glyphs (0-27) then a single minor glyph,
 * {@code "Glyph of Thunderstorm"} (28). The glyph getters keep the {@code isGlyphofXxx()} spelling.
 * Construction, cloning and the live-array accessors follow {@link TalentsBase}.</p>
 */
public class ShamanTalents extends TalentsBase {
    public static final String[] TreeNames = new String[] {
        "Elemental",
        "Enhancement",
        "Restoration",
    };

    private final int[] data = new int[80];
    private final boolean[] glyphData = new boolean[29];

    public ShamanTalents() {
    }

    public ShamanTalents(String talents) {
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

    public ShamanTalents clone() {
        ShamanTalents clone = new ShamanTalents();
        System.arraycopy(data, 0, clone.data, 0, data.length);
        System.arraycopy(glyphData, 0, clone.glyphData, 0, glyphData.length);
        return clone;
    }

    @TalentDataAttribute(index = 0, name = "Convection", maxPoints = 5, tree = 0, column = 2, row = 1, prerequisite = -1)
    public int getConvection() {
        return data[0];
    }

    public void setConvection(int value) {
        data[0] = value;
    }

    @TalentDataAttribute(index = 1, name = "Concussion", maxPoints = 5, tree = 0, column = 3, row = 1, prerequisite = -1)
    public int getConcussion() {
        return data[1];
    }

    public void setConcussion(int value) {
        data[1] = value;
    }

    @TalentDataAttribute(index = 2, name = "Call of Flame", maxPoints = 3, tree = 0, column = 1, row = 2, prerequisite = -1)
    public int getCallOfFlame() {
        return data[2];
    }

    public void setCallOfFlame(int value) {
        data[2] = value;
    }

    @TalentDataAttribute(index = 3, name = "Elemental Warding", maxPoints = 3, tree = 0, column = 2, row = 2, prerequisite = -1)
    public int getElementalWarding() {
        return data[3];
    }

    public void setElementalWarding(int value) {
        data[3] = value;
    }

    @TalentDataAttribute(index = 4, name = "Elemental Devastation", maxPoints = 3, tree = 0, column = 3, row = 2, prerequisite = -1)
    public int getElementalDevastation() {
        return data[4];
    }

    public void setElementalDevastation(int value) {
        data[4] = value;
    }

    @TalentDataAttribute(index = 5, name = "Reverberation", maxPoints = 5, tree = 0, column = 1, row = 3, prerequisite = -1)
    public int getReverberation() {
        return data[5];
    }

    public void setReverberation(int value) {
        data[5] = value;
    }

    @TalentDataAttribute(index = 6, name = "Elemental Focus", maxPoints = 1, tree = 0, column = 2, row = 3, prerequisite = -1)
    public int getElementalFocus() {
        return data[6];
    }

    public void setElementalFocus(int value) {
        data[6] = value;
    }

    @TalentDataAttribute(index = 7, name = "Elemental Fury", maxPoints = 5, tree = 0, column = 3, row = 3, prerequisite = -1)
    public int getElementalFury() {
        return data[7];
    }

    public void setElementalFury(int value) {
        data[7] = value;
    }

    @TalentDataAttribute(index = 8, name = "Improved Fire Nova", maxPoints = 2, tree = 0, column = 1, row = 4, prerequisite = -1)
    public int getImprovedFireNova() {
        return data[8];
    }

    public void setImprovedFireNova(int value) {
        data[8] = value;
    }

    @TalentDataAttribute(index = 9, name = "Eye of the Storm", maxPoints = 3, tree = 0, column = 4, row = 4, prerequisite = -1)
    public int getEyeOfTheStorm() {
        return data[9];
    }

    public void setEyeOfTheStorm(int value) {
        data[9] = value;
    }

    @TalentDataAttribute(index = 10, name = "Elemental Reach", maxPoints = 2, tree = 0, column = 1, row = 5, prerequisite = -1)
    public int getElementalReach() {
        return data[10];
    }

    public void setElementalReach(int value) {
        data[10] = value;
    }

    @TalentDataAttribute(index = 11, name = "Call of Thunder", maxPoints = 1, tree = 0, column = 2, row = 5, prerequisite = 6)
    public int getCallOfThunder() {
        return data[11];
    }

    public void setCallOfThunder(int value) {
        data[11] = value;
    }

    @TalentDataAttribute(index = 12, name = "Unrelenting Storm", maxPoints = 3, tree = 0, column = 4, row = 5, prerequisite = -1)
    public int getUnrelentingStorm() {
        return data[12];
    }

    public void setUnrelentingStorm(int value) {
        data[12] = value;
    }

    @TalentDataAttribute(index = 13, name = "Elemental Precision", maxPoints = 3, tree = 0, column = 1, row = 6, prerequisite = -1)
    public int getElementalPrecision() {
        return data[13];
    }

    public void setElementalPrecision(int value) {
        data[13] = value;
    }

    @TalentDataAttribute(index = 14, name = "Lightning Mastery", maxPoints = 5, tree = 0, column = 3, row = 6, prerequisite = 7)
    public int getLightningMastery() {
        return data[14];
    }

    public void setLightningMastery(int value) {
        data[14] = value;
    }

    @TalentDataAttribute(index = 15, name = "Elemental Mastery", maxPoints = 1, tree = 0, column = 2, row = 7, prerequisite = 11)
    public int getElementalMastery() {
        return data[15];
    }

    public void setElementalMastery(int value) {
        data[15] = value;
    }

    @TalentDataAttribute(index = 16, name = "Storm, Earth and Fire", maxPoints = 3, tree = 0, column = 3, row = 7, prerequisite = -1)
    public int getStormEarthAndFire() {
        return data[16];
    }

    public void setStormEarthAndFire(int value) {
        data[16] = value;
    }

    @TalentDataAttribute(index = 17, name = "Booming Echoes", maxPoints = 2, tree = 0, column = 1, row = 8, prerequisite = -1)
    public int getBoomingEchoes() {
        return data[17];
    }

    public void setBoomingEchoes(int value) {
        data[17] = value;
    }

    @TalentDataAttribute(index = 18, name = "Elemental Oath", maxPoints = 2, tree = 0, column = 2, row = 8, prerequisite = 15)
    public int getElementalOath() {
        return data[18];
    }

    public void setElementalOath(int value) {
        data[18] = value;
    }

    @TalentDataAttribute(index = 19, name = "Lightning Overload", maxPoints = 3, tree = 0, column = 3, row = 8, prerequisite = -1)
    public int getLightningOverload() {
        return data[19];
    }

    public void setLightningOverload(int value) {
        data[19] = value;
    }

    @TalentDataAttribute(index = 20, name = "Astral Shift", maxPoints = 3, tree = 0, column = 1, row = 9, prerequisite = -1)
    public int getAstralShift() {
        return data[20];
    }

    public void setAstralShift(int value) {
        data[20] = value;
    }

    @TalentDataAttribute(index = 21, name = "Totem of Wrath", maxPoints = 1, tree = 0, column = 2, row = 9, prerequisite = -1)
    public int getTotemOfWrath() {
        return data[21];
    }

    public void setTotemOfWrath(int value) {
        data[21] = value;
    }

    @TalentDataAttribute(index = 22, name = "Lava Flows", maxPoints = 3, tree = 0, column = 3, row = 9, prerequisite = -1)
    public int getLavaFlows() {
        return data[22];
    }

    public void setLavaFlows(int value) {
        data[22] = value;
    }

    @TalentDataAttribute(index = 23, name = "Shamanism", maxPoints = 5, tree = 0, column = 2, row = 10, prerequisite = -1)
    public int getShamanism() {
        return data[23];
    }

    public void setShamanism(int value) {
        data[23] = value;
    }

    @TalentDataAttribute(index = 24, name = "Thunderstorm", maxPoints = 1, tree = 0, column = 2, row = 11, prerequisite = -1)
    public int getThunderstorm() {
        return data[24];
    }

    public void setThunderstorm(int value) {
        data[24] = value;
    }

    @TalentDataAttribute(index = 25, name = "Enhancing Totems", maxPoints = 3, tree = 1, column = 1, row = 1, prerequisite = -1)
    public int getEnhancingTotems() {
        return data[25];
    }

    public void setEnhancingTotems(int value) {
        data[25] = value;
    }

    @TalentDataAttribute(index = 26, name = "Earth's Grasp", maxPoints = 2, tree = 1, column = 2, row = 1, prerequisite = -1)
    public int getEarthsGrasp() {
        return data[26];
    }

    public void setEarthsGrasp(int value) {
        data[26] = value;
    }

    @TalentDataAttribute(index = 27, name = "Ancestral Knowledge", maxPoints = 5, tree = 1, column = 3, row = 1, prerequisite = -1)
    public int getAncestralKnowledge() {
        return data[27];
    }

    public void setAncestralKnowledge(int value) {
        data[27] = value;
    }

    @TalentDataAttribute(index = 28, name = "Guardian Totems", maxPoints = 2, tree = 1, column = 1, row = 2, prerequisite = -1)
    public int getGuardianTotems() {
        return data[28];
    }

    public void setGuardianTotems(int value) {
        data[28] = value;
    }

    @TalentDataAttribute(index = 29, name = "Thundering Strikes", maxPoints = 5, tree = 1, column = 2, row = 2, prerequisite = -1)
    public int getThunderingStrikes() {
        return data[29];
    }

    public void setThunderingStrikes(int value) {
        data[29] = value;
    }

    @TalentDataAttribute(index = 30, name = "Improved Ghost Wolf", maxPoints = 2, tree = 1, column = 3, row = 2, prerequisite = -1)
    public int getImprovedGhostWolf() {
        return data[30];
    }

    public void setImprovedGhostWolf(int value) {
        data[30] = value;
    }

    @TalentDataAttribute(index = 31, name = "Improved Shields", maxPoints = 3, tree = 1, column = 4, row = 2, prerequisite = -1)
    public int getImprovedShields() {
        return data[31];
    }

    public void setImprovedShields(int value) {
        data[31] = value;
    }

    @TalentDataAttribute(index = 32, name = "Elemental Weapons", maxPoints = 3, tree = 1, column = 1, row = 3, prerequisite = -1)
    public int getElementalWeapons() {
        return data[32];
    }

    public void setElementalWeapons(int value) {
        data[32] = value;
    }

    @TalentDataAttribute(index = 33, name = "Shamanistic Focus", maxPoints = 1, tree = 1, column = 3, row = 3, prerequisite = -1)
    public int getShamanisticFocus() {
        return data[33];
    }

    public void setShamanisticFocus(int value) {
        data[33] = value;
    }

    @TalentDataAttribute(index = 34, name = "Anticipation", maxPoints = 3, tree = 1, column = 4, row = 3, prerequisite = -1)
    public int getAnticipation() {
        return data[34];
    }

    public void setAnticipation(int value) {
        data[34] = value;
    }

    @TalentDataAttribute(index = 35, name = "Flurry", maxPoints = 5, tree = 1, column = 2, row = 4, prerequisite = 29)
    public int getFlurry() {
        return data[35];
    }

    public void setFlurry(int value) {
        data[35] = value;
    }

    @TalentDataAttribute(index = 36, name = "Toughness", maxPoints = 5, tree = 1, column = 3, row = 4, prerequisite = -1)
    public int getToughness() {
        return data[36];
    }

    public void setToughness(int value) {
        data[36] = value;
    }

    @TalentDataAttribute(index = 37, name = "Improved Windfury Totem", maxPoints = 2, tree = 1, column = 1, row = 5, prerequisite = -1)
    public int getImprovedWindfuryTotem() {
        return data[37];
    }

    public void setImprovedWindfuryTotem(int value) {
        data[37] = value;
    }

    @TalentDataAttribute(index = 38, name = "Spirit Weapons", maxPoints = 1, tree = 1, column = 2, row = 5, prerequisite = -1)
    public int getSpiritWeapons() {
        return data[38];
    }

    public void setSpiritWeapons(int value) {
        data[38] = value;
    }

    @TalentDataAttribute(index = 39, name = "Mental Dexterity", maxPoints = 3, tree = 1, column = 3, row = 5, prerequisite = -1)
    public int getMentalDexterity() {
        return data[39];
    }

    public void setMentalDexterity(int value) {
        data[39] = value;
    }

    @TalentDataAttribute(index = 40, name = "Unleashed Rage", maxPoints = 3, tree = 1, column = 1, row = 6, prerequisite = -1)
    public int getUnleashedRage() {
        return data[40];
    }

    public void setUnleashedRage(int value) {
        data[40] = value;
    }

    @TalentDataAttribute(index = 41, name = "Weapon Mastery", maxPoints = 3, tree = 1, column = 3, row = 6, prerequisite = -1)
    public int getWeaponMastery() {
        return data[41];
    }

    public void setWeaponMastery(int value) {
        data[41] = value;
    }

    @TalentDataAttribute(index = 42, name = "Frozen Power", maxPoints = 2, tree = 1, column = 4, row = 6, prerequisite = -1)
    public int getFrozenPower() {
        return data[42];
    }

    public void setFrozenPower(int value) {
        data[42] = value;
    }

    @TalentDataAttribute(index = 43, name = "Dual Wield Specialization", maxPoints = 3, tree = 1, column = 1, row = 7, prerequisite = 44)
    public int getDualWieldSpecialization() {
        return data[43];
    }

    public void setDualWieldSpecialization(int value) {
        data[43] = value;
    }

    @TalentDataAttribute(index = 44, name = "Dual Wield", maxPoints = 1, tree = 1, column = 2, row = 7, prerequisite = 38)
    public int getDualWield() {
        return data[44];
    }

    public void setDualWield(int value) {
        data[44] = value;
    }

    @TalentDataAttribute(index = 45, name = "Stormstrike", maxPoints = 1, tree = 1, column = 3, row = 7, prerequisite = -1)
    public int getStormstrike() {
        return data[45];
    }

    public void setStormstrike(int value) {
        data[45] = value;
    }

    @TalentDataAttribute(index = 46, name = "Static Shock", maxPoints = 3, tree = 1, column = 1, row = 8, prerequisite = -1)
    public int getStaticShock() {
        return data[46];
    }

    public void setStaticShock(int value) {
        data[46] = value;
    }

    @TalentDataAttribute(index = 47, name = "Lava Lash", maxPoints = 1, tree = 1, column = 2, row = 8, prerequisite = 44)
    public int getLavaLash() {
        return data[47];
    }

    public void setLavaLash(int value) {
        data[47] = value;
    }

    @TalentDataAttribute(index = 48, name = "Improved Stormstrike", maxPoints = 2, tree = 1, column = 3, row = 8, prerequisite = 45)
    public int getImprovedStormstrike() {
        return data[48];
    }

    public void setImprovedStormstrike(int value) {
        data[48] = value;
    }

    @TalentDataAttribute(index = 49, name = "Mental Quickness", maxPoints = 3, tree = 1, column = 1, row = 9, prerequisite = -1)
    public int getMentalQuickness() {
        return data[49];
    }

    public void setMentalQuickness(int value) {
        data[49] = value;
    }

    @TalentDataAttribute(index = 50, name = "Shamanistic Rage", maxPoints = 1, tree = 1, column = 2, row = 9, prerequisite = -1)
    public int getShamanisticRage() {
        return data[50];
    }

    public void setShamanisticRage(int value) {
        data[50] = value;
    }

    @TalentDataAttribute(index = 51, name = "Earthen Power", maxPoints = 2, tree = 1, column = 3, row = 9, prerequisite = -1)
    public int getEarthenPower() {
        return data[51];
    }

    public void setEarthenPower(int value) {
        data[51] = value;
    }

    @TalentDataAttribute(index = 52, name = "Maelstrom Weapon", maxPoints = 5, tree = 1, column = 2, row = 10, prerequisite = -1)
    public int getMaelstromWeapon() {
        return data[52];
    }

    public void setMaelstromWeapon(int value) {
        data[52] = value;
    }

    @TalentDataAttribute(index = 53, name = "Feral Spirit", maxPoints = 1, tree = 1, column = 2, row = 11, prerequisite = -1)
    public int getFeralSpirit() {
        return data[53];
    }

    public void setFeralSpirit(int value) {
        data[53] = value;
    }

    @TalentDataAttribute(index = 54, name = "Improved Healing Wave", maxPoints = 5, tree = 2, column = 2, row = 1, prerequisite = -1)
    public int getImprovedHealingWave() {
        return data[54];
    }

    public void setImprovedHealingWave(int value) {
        data[54] = value;
    }

    @TalentDataAttribute(index = 55, name = "Totemic Focus", maxPoints = 5, tree = 2, column = 3, row = 1, prerequisite = -1)
    public int getTotemicFocus() {
        return data[55];
    }

    public void setTotemicFocus(int value) {
        data[55] = value;
    }

    @TalentDataAttribute(index = 56, name = "Improved Reincarnation", maxPoints = 2, tree = 2, column = 1, row = 2, prerequisite = -1)
    public int getImprovedReincarnation() {
        return data[56];
    }

    public void setImprovedReincarnation(int value) {
        data[56] = value;
    }

    @TalentDataAttribute(index = 57, name = "Healing Grace", maxPoints = 3, tree = 2, column = 2, row = 2, prerequisite = -1)
    public int getHealingGrace() {
        return data[57];
    }

    public void setHealingGrace(int value) {
        data[57] = value;
    }

    @TalentDataAttribute(index = 58, name = "Tidal Focus", maxPoints = 5, tree = 2, column = 3, row = 2, prerequisite = -1)
    public int getTidalFocus() {
        return data[58];
    }

    public void setTidalFocus(int value) {
        data[58] = value;
    }

    @TalentDataAttribute(index = 59, name = "Improved Water Shield", maxPoints = 3, tree = 2, column = 1, row = 3, prerequisite = -1)
    public int getImprovedWaterShield() {
        return data[59];
    }

    public void setImprovedWaterShield(int value) {
        data[59] = value;
    }

    @TalentDataAttribute(index = 60, name = "Healing Focus", maxPoints = 3, tree = 2, column = 2, row = 3, prerequisite = -1)
    public int getHealingFocus() {
        return data[60];
    }

    public void setHealingFocus(int value) {
        data[60] = value;
    }

    @TalentDataAttribute(index = 61, name = "Tidal Force", maxPoints = 1, tree = 2, column = 3, row = 3, prerequisite = -1)
    public int getTidalForce() {
        return data[61];
    }

    public void setTidalForce(int value) {
        data[61] = value;
    }

    @TalentDataAttribute(index = 62, name = "Ancestral Healing", maxPoints = 3, tree = 2, column = 4, row = 3, prerequisite = -1)
    public int getAncestralHealing() {
        return data[62];
    }

    public void setAncestralHealing(int value) {
        data[62] = value;
    }

    @TalentDataAttribute(index = 63, name = "Restorative Totems", maxPoints = 3, tree = 2, column = 2, row = 4, prerequisite = -1)
    public int getRestorativeTotems() {
        return data[63];
    }

    public void setRestorativeTotems(int value) {
        data[63] = value;
    }

    @TalentDataAttribute(index = 64, name = "Tidal Mastery", maxPoints = 5, tree = 2, column = 3, row = 4, prerequisite = -1)
    public int getTidalMastery() {
        return data[64];
    }

    public void setTidalMastery(int value) {
        data[64] = value;
    }

    @TalentDataAttribute(index = 65, name = "Healing Way", maxPoints = 3, tree = 2, column = 1, row = 5, prerequisite = -1)
    public int getHealingWay() {
        return data[65];
    }

    public void setHealingWay(int value) {
        data[65] = value;
    }

    @TalentDataAttribute(index = 66, name = "Nature's Swiftness", maxPoints = 1, tree = 2, column = 3, row = 5, prerequisite = -1)
    public int getNaturesSwiftness() {
        return data[66];
    }

    public void setNaturesSwiftness(int value) {
        data[66] = value;
    }

    @TalentDataAttribute(index = 67, name = "Focused Mind", maxPoints = 3, tree = 2, column = 4, row = 5, prerequisite = -1)
    public int getFocusedMind() {
        return data[67];
    }

    public void setFocusedMind(int value) {
        data[67] = value;
    }

    @TalentDataAttribute(index = 68, name = "Purification", maxPoints = 5, tree = 2, column = 3, row = 6, prerequisite = -1)
    public int getPurification() {
        return data[68];
    }

    public void setPurification(int value) {
        data[68] = value;
    }

    @TalentDataAttribute(index = 69, name = "Nature's Guardian", maxPoints = 5, tree = 2, column = 1, row = 7, prerequisite = -1)
    public int getNaturesGuardian() {
        return data[69];
    }

    public void setNaturesGuardian(int value) {
        data[69] = value;
    }

    @TalentDataAttribute(index = 70, name = "Mana Tide Totem", maxPoints = 1, tree = 2, column = 2, row = 7, prerequisite = 63)
    public int getManaTideTotem() {
        return data[70];
    }

    public void setManaTideTotem(int value) {
        data[70] = value;
    }

    @TalentDataAttribute(index = 71, name = "Cleanse Spirit", maxPoints = 1, tree = 2, column = 3, row = 7, prerequisite = 68)
    public int getCleanseSpirit() {
        return data[71];
    }

    public void setCleanseSpirit(int value) {
        data[71] = value;
    }

    @TalentDataAttribute(index = 72, name = "Blessing of the Eternals", maxPoints = 2, tree = 2, column = 1, row = 8, prerequisite = -1)
    public int getBlessingOfTheEternals() {
        return data[72];
    }

    public void setBlessingOfTheEternals(int value) {
        data[72] = value;
    }

    @TalentDataAttribute(index = 73, name = "Improved Chain Heal", maxPoints = 2, tree = 2, column = 2, row = 8, prerequisite = -1)
    public int getImprovedChainHeal() {
        return data[73];
    }

    public void setImprovedChainHeal(int value) {
        data[73] = value;
    }

    @TalentDataAttribute(index = 74, name = "Nature's Blessing", maxPoints = 3, tree = 2, column = 3, row = 8, prerequisite = -1)
    public int getNaturesBlessing() {
        return data[74];
    }

    public void setNaturesBlessing(int value) {
        data[74] = value;
    }

    @TalentDataAttribute(index = 75, name = "Ancestral Awakening", maxPoints = 3, tree = 2, column = 1, row = 9, prerequisite = -1)
    public int getAncestralAwakening() {
        return data[75];
    }

    public void setAncestralAwakening(int value) {
        data[75] = value;
    }

    @TalentDataAttribute(index = 76, name = "Earth Shield", maxPoints = 1, tree = 2, column = 2, row = 9, prerequisite = -1)
    public int getEarthShield() {
        return data[76];
    }

    public void setEarthShield(int value) {
        data[76] = value;
    }

    @TalentDataAttribute(index = 77, name = "Improved Earth Shield", maxPoints = 2, tree = 2, column = 3, row = 9, prerequisite = 76)
    public int getImprovedEarthShield() {
        return data[77];
    }

    public void setImprovedEarthShield(int value) {
        data[77] = value;
    }

    @TalentDataAttribute(index = 78, name = "Tidal Waves", maxPoints = 5, tree = 2, column = 2, row = 10, prerequisite = -1)
    public int getTidalWaves() {
        return data[78];
    }

    public void setTidalWaves(int value) {
        data[78] = value;
    }

    @TalentDataAttribute(index = 79, name = "Riptide", maxPoints = 1, tree = 2, column = 2, row = 11, prerequisite = -1)
    public int getRiptide() {
        return data[79];
    }

    public void setRiptide(int value) {
        data[79] = value;
    }

    @GlyphDataAttribute(index = 0, name = "Glyph of Healing Wave", major = true)
    public boolean isGlyphofHealingWave() {
        return glyphData[0];
    }

    public void setGlyphofHealingWave(boolean value) {
        glyphData[0] = value;
    }

    @GlyphDataAttribute(index = 1, name = "Glyph of Lightning Bolt", major = true)
    public boolean isGlyphofLightningBolt() {
        return glyphData[1];
    }

    public void setGlyphofLightningBolt(boolean value) {
        glyphData[1] = value;
    }

    @GlyphDataAttribute(index = 2, name = "Glyph of Shocking", major = true)
    public boolean isGlyphofShocking() {
        return glyphData[2];
    }

    public void setGlyphofShocking(boolean value) {
        glyphData[2] = value;
    }

    @GlyphDataAttribute(index = 3, name = "Glyph of Lightning Shield", major = true)
    public boolean isGlyphofLightningShield() {
        return glyphData[3];
    }

    public void setGlyphofLightningShield(boolean value) {
        glyphData[3] = value;
    }

    @GlyphDataAttribute(index = 4, name = "Glyph of Flame Shock", major = true)
    public boolean isGlyphofFlameShock() {
        return glyphData[4];
    }

    public void setGlyphofFlameShock(boolean value) {
        glyphData[4] = value;
    }

    @GlyphDataAttribute(index = 5, name = "Glyph of Flametongue Weapon", major = true)
    public boolean isGlyphofFlametongueWeapon() {
        return glyphData[5];
    }

    public void setGlyphofFlametongueWeapon(boolean value) {
        glyphData[5] = value;
    }

    @GlyphDataAttribute(index = 6, name = "Glyph of Lava Lash", major = true)
    public boolean isGlyphofLavaLash() {
        return glyphData[6];
    }

    public void setGlyphofLavaLash(boolean value) {
        glyphData[6] = value;
    }

    @GlyphDataAttribute(index = 7, name = "Glyph of Fire Nova", major = true)
    public boolean isGlyphofFireNova() {
        return glyphData[7];
    }

    public void setGlyphofFireNova(boolean value) {
        glyphData[7] = value;
    }

    @GlyphDataAttribute(index = 8, name = "Glyph of Frost Shock", major = true)
    public boolean isGlyphofFrostShock() {
        return glyphData[8];
    }

    public void setGlyphofFrostShock(boolean value) {
        glyphData[8] = value;
    }

    @GlyphDataAttribute(index = 9, name = "Glyph of Healing Stream Totem", major = true)
    public boolean isGlyphofHealingStreamTotem() {
        return glyphData[9];
    }

    public void setGlyphofHealingStreamTotem(boolean value) {
        glyphData[9] = value;
    }

    @GlyphDataAttribute(index = 10, name = "Glyph of Lesser Healing Wave", major = true)
    public boolean isGlyphofLesserHealingWave() {
        return glyphData[10];
    }

    public void setGlyphofLesserHealingWave(boolean value) {
        glyphData[10] = value;
    }

    @GlyphDataAttribute(index = 11, name = "Glyph of Water Mastery", major = true)
    public boolean isGlyphofWaterMastery() {
        return glyphData[11];
    }

    public void setGlyphofWaterMastery(boolean value) {
        glyphData[11] = value;
    }

    @GlyphDataAttribute(index = 12, name = "Glyph of Earthliving Weapon", major = true)
    public boolean isGlyphofEarthlivingWeapon() {
        return glyphData[12];
    }

    public void setGlyphofEarthlivingWeapon(boolean value) {
        glyphData[12] = value;
    }

    @GlyphDataAttribute(index = 13, name = "Glyph of Windfury Weapon", major = true)
    public boolean isGlyphofWindfuryWeapon() {
        return glyphData[13];
    }

    public void setGlyphofWindfuryWeapon(boolean value) {
        glyphData[13] = value;
    }

    @GlyphDataAttribute(index = 14, name = "Glyph of Chain Lightning", major = true)
    public boolean isGlyphofChainLightning() {
        return glyphData[14];
    }

    public void setGlyphofChainLightning(boolean value) {
        glyphData[14] = value;
    }

    @GlyphDataAttribute(index = 15, name = "Glyph of Chain Heal", major = true)
    public boolean isGlyphofChainHeal() {
        return glyphData[15];
    }

    public void setGlyphofChainHeal(boolean value) {
        glyphData[15] = value;
    }

    @GlyphDataAttribute(index = 16, name = "Glyph of Earth Shield", major = true)
    public boolean isGlyphofEarthShield() {
        return glyphData[16];
    }

    public void setGlyphofEarthShield(boolean value) {
        glyphData[16] = value;
    }

    @GlyphDataAttribute(index = 17, name = "Glyph of Feral Spirit", major = true)
    public boolean isGlyphofFeralSpirit() {
        return glyphData[17];
    }

    public void setGlyphofFeralSpirit(boolean value) {
        glyphData[17] = value;
    }

    @GlyphDataAttribute(index = 18, name = "Glyph of Hex", major = true)
    public boolean isGlyphofHex() {
        return glyphData[18];
    }

    public void setGlyphofHex(boolean value) {
        glyphData[18] = value;
    }

    @GlyphDataAttribute(index = 19, name = "Glyph of Mana Tide Totem", major = true)
    public boolean isGlyphofManaTideTotem() {
        return glyphData[19];
    }

    public void setGlyphofManaTideTotem(boolean value) {
        glyphData[19] = value;
    }

    @GlyphDataAttribute(index = 20, name = "Glyph of Riptide", major = true)
    public boolean isGlyphofRiptide() {
        return glyphData[20];
    }

    public void setGlyphofRiptide(boolean value) {
        glyphData[20] = value;
    }

    @GlyphDataAttribute(index = 21, name = "Glyph of Stoneclaw Totem", major = true)
    public boolean isGlyphofStoneclawTotem() {
        return glyphData[21];
    }

    public void setGlyphofStoneclawTotem(boolean value) {
        glyphData[21] = value;
    }

    @GlyphDataAttribute(index = 22, name = "Glyph of Stormstrike", major = true)
    public boolean isGlyphofStormstrike() {
        return glyphData[22];
    }

    public void setGlyphofStormstrike(boolean value) {
        glyphData[22] = value;
    }

    @GlyphDataAttribute(index = 23, name = "Glyph of Thunder", major = true)
    public boolean isGlyphofThunder() {
        return glyphData[23];
    }

    public void setGlyphofThunder(boolean value) {
        glyphData[23] = value;
    }

    @GlyphDataAttribute(index = 24, name = "Glyph of Totem of Wrath", major = true)
    public boolean isGlyphofTotemofWrath() {
        return glyphData[24];
    }

    public void setGlyphofTotemofWrath(boolean value) {
        glyphData[24] = value;
    }

    @GlyphDataAttribute(index = 25, name = "Glyph of Elemental Mastery", major = true)
    public boolean isGlyphofElementalMastery() {
        return glyphData[25];
    }

    public void setGlyphofElementalMastery(boolean value) {
        glyphData[25] = value;
    }

    @GlyphDataAttribute(index = 26, name = "Glyph of Lava", major = true)
    public boolean isGlyphofLava() {
        return glyphData[26];
    }

    public void setGlyphofLava(boolean value) {
        glyphData[26] = value;
    }

    @GlyphDataAttribute(index = 27, name = "Glyph of Fire Elemental Totem", major = true)
    public boolean isGlyphofFireElementalTotem() {
        return glyphData[27];
    }

    public void setGlyphofFireElementalTotem(boolean value) {
        glyphData[27] = value;
    }

    @GlyphDataAttribute(index = 28, name = "Glyph of Thunderstorm", major = false)
    public boolean isGlyphofThunderstorm() {
        return glyphData[28];
    }

    public void setGlyphofThunderstorm(boolean value) {
        glyphData[28] = value;
    }

}
