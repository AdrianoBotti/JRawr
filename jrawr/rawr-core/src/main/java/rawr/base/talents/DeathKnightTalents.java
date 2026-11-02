// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassTalents.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassGlyphs.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.talents;

/**
 * The death knight's talent and glyph allocation: the Blood, Frost and Unholy trees plus the 33
 * glyph slots of this fork's table.
 *
 * <p>{@code data} has 88 rank slots, contiguous per tree: Blood 0-27 (tree 0), Frost 28-56
 * (tree 1), Unholy 57-87 (tree 2); {@link #TreeNames} fixes that order and each getter's
 * {@link TalentDataAttribute} declares its row, column and prerequisite. The death knight models
 * ({@code rawr.models.dpsdk} and {@code rawr.models.tankdk}) read the tree matching the chosen
 * rotation, for example {@link #getButchery()}, {@link #getImprovedIcyTouch()} and
 * {@link #getViciousStrikes()}, the first talent of each tree, and test glyph flags directly.</p>
 *
 * <p>{@code glyphData} has 33 flags: 27 major glyphs (indices 0-26) followed by 6 minor glyphs
 * (27-32). The glyph getters keep the C# spelling {@code isGlyphofXxx()} ("of" lowercase), e.g.
 * {@link #isGlyphofDeathStrike()} and {@link #isGlyphofIcyTouch()}. The {@code String} constructor
 * and {@link #clone()} follow the {@link TalentsBase} contract, so the compact string matches the
 * C# saved format.</p>
 */
public class DeathKnightTalents extends TalentsBase {
    public static final String[] TreeNames = new String[] {
        "Blood",
        "Frost",
        "Unholy",
    };

    private final int[] data = new int[88];
    private final boolean[] glyphData = new boolean[33];

    public DeathKnightTalents() {
    }

    public DeathKnightTalents(String talents) {
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

    public DeathKnightTalents clone() {
        DeathKnightTalents clone = new DeathKnightTalents();
        System.arraycopy(data, 0, clone.data, 0, data.length);
        System.arraycopy(glyphData, 0, clone.glyphData, 0, glyphData.length);
        return clone;
    }

    @TalentDataAttribute(index = 0, name = "Butchery", maxPoints = 2, tree = 0, column = 1, row = 1, prerequisite = -1)
    public int getButchery() {
        return data[0];
    }

    public void setButchery(int value) {
        data[0] = value;
    }

    @TalentDataAttribute(index = 1, name = "Subversion", maxPoints = 3, tree = 0, column = 2, row = 1, prerequisite = -1)
    public int getSubversion() {
        return data[1];
    }

    public void setSubversion(int value) {
        data[1] = value;
    }

    @TalentDataAttribute(index = 2, name = "Blade Barrier", maxPoints = 5, tree = 0, column = 3, row = 1, prerequisite = -1)
    public int getBladeBarrier() {
        return data[2];
    }

    public void setBladeBarrier(int value) {
        data[2] = value;
    }

    @TalentDataAttribute(index = 3, name = "Bladed Armor", maxPoints = 5, tree = 0, column = 1, row = 2, prerequisite = -1)
    public int getBladedArmor() {
        return data[3];
    }

    public void setBladedArmor(int value) {
        data[3] = value;
    }

    @TalentDataAttribute(index = 4, name = "Scent of Blood", maxPoints = 3, tree = 0, column = 2, row = 2, prerequisite = -1)
    public int getScentOfBlood() {
        return data[4];
    }

    public void setScentOfBlood(int value) {
        data[4] = value;
    }

    @TalentDataAttribute(index = 5, name = "Two-Handed Weapon Specialization", maxPoints = 2, tree = 0, column = 3, row = 2, prerequisite = -1)
    public int getTwoHandedWeaponSpecialization() {
        return data[5];
    }

    public void setTwoHandedWeaponSpecialization(int value) {
        data[5] = value;
    }

    @TalentDataAttribute(index = 6, name = "Rune Tap", maxPoints = 1, tree = 0, column = 1, row = 3, prerequisite = -1)
    public int getRuneTap() {
        return data[6];
    }

    public void setRuneTap(int value) {
        data[6] = value;
    }

    @TalentDataAttribute(index = 7, name = "Dark Conviction", maxPoints = 5, tree = 0, column = 2, row = 3, prerequisite = -1)
    public int getDarkConviction() {
        return data[7];
    }

    public void setDarkConviction(int value) {
        data[7] = value;
    }

    @TalentDataAttribute(index = 8, name = "Death Rune Mastery", maxPoints = 3, tree = 0, column = 3, row = 3, prerequisite = -1)
    public int getDeathRuneMastery() {
        return data[8];
    }

    public void setDeathRuneMastery(int value) {
        data[8] = value;
    }

    @TalentDataAttribute(index = 9, name = "Improved Rune Tap", maxPoints = 3, tree = 0, column = 1, row = 4, prerequisite = 6)
    public int getImprovedRuneTap() {
        return data[9];
    }

    public void setImprovedRuneTap(int value) {
        data[9] = value;
    }

    @TalentDataAttribute(index = 10, name = "Spell Deflection", maxPoints = 3, tree = 0, column = 3, row = 4, prerequisite = -1)
    public int getSpellDeflection() {
        return data[10];
    }

    public void setSpellDeflection(int value) {
        data[10] = value;
    }

    @TalentDataAttribute(index = 11, name = "Vendetta", maxPoints = 3, tree = 0, column = 4, row = 4, prerequisite = -1)
    public int getVendetta() {
        return data[11];
    }

    public void setVendetta(int value) {
        data[11] = value;
    }

    @TalentDataAttribute(index = 12, name = "Bloody Strikes", maxPoints = 3, tree = 0, column = 1, row = 5, prerequisite = -1)
    public int getBloodyStrikes() {
        return data[12];
    }

    public void setBloodyStrikes(int value) {
        data[12] = value;
    }

    @TalentDataAttribute(index = 13, name = "Veteran of the Third War", maxPoints = 3, tree = 0, column = 3, row = 5, prerequisite = -1)
    public int getVeteranOfTheThirdWar() {
        return data[13];
    }

    public void setVeteranOfTheThirdWar(int value) {
        data[13] = value;
    }

    @TalentDataAttribute(index = 14, name = "Mark of Blood", maxPoints = 1, tree = 0, column = 4, row = 5, prerequisite = -1)
    public int getMarkOfBlood() {
        return data[14];
    }

    public void setMarkOfBlood(int value) {
        data[14] = value;
    }

    @TalentDataAttribute(index = 15, name = "Bloody Vengeance", maxPoints = 3, tree = 0, column = 2, row = 6, prerequisite = 7)
    public int getBloodyVengeance() {
        return data[15];
    }

    public void setBloodyVengeance(int value) {
        data[15] = value;
    }

    @TalentDataAttribute(index = 16, name = "Abomination's Might", maxPoints = 2, tree = 0, column = 3, row = 6, prerequisite = -1)
    public int getAbominationsMight() {
        return data[16];
    }

    public void setAbominationsMight(int value) {
        data[16] = value;
    }

    @TalentDataAttribute(index = 17, name = "Bloodworms", maxPoints = 3, tree = 0, column = 1, row = 7, prerequisite = -1)
    public int getBloodworms() {
        return data[17];
    }

    public void setBloodworms(int value) {
        data[17] = value;
    }

    @TalentDataAttribute(index = 18, name = "Hysteria", maxPoints = 1, tree = 0, column = 2, row = 7, prerequisite = -1)
    public int getHysteria() {
        return data[18];
    }

    public void setHysteria(int value) {
        data[18] = value;
    }

    @TalentDataAttribute(index = 19, name = "Improved Blood Presence", maxPoints = 2, tree = 0, column = 3, row = 7, prerequisite = -1)
    public int getImprovedBloodPresence() {
        return data[19];
    }

    public void setImprovedBloodPresence(int value) {
        data[19] = value;
    }

    @TalentDataAttribute(index = 20, name = "Improved Death Strike", maxPoints = 2, tree = 0, column = 1, row = 8, prerequisite = -1)
    public int getImprovedDeathStrike() {
        return data[20];
    }

    public void setImprovedDeathStrike(int value) {
        data[20] = value;
    }

    @TalentDataAttribute(index = 21, name = "Sudden Doom", maxPoints = 3, tree = 0, column = 2, row = 8, prerequisite = -1)
    public int getSuddenDoom() {
        return data[21];
    }

    public void setSuddenDoom(int value) {
        data[21] = value;
    }

    @TalentDataAttribute(index = 22, name = "Vampiric Blood", maxPoints = 1, tree = 0, column = 3, row = 8, prerequisite = -1)
    public int getVampiricBlood() {
        return data[22];
    }

    public void setVampiricBlood(int value) {
        data[22] = value;
    }

    @TalentDataAttribute(index = 23, name = "Will of the Necropolis", maxPoints = 3, tree = 0, column = 1, row = 9, prerequisite = -1)
    public int getWillOfTheNecropolis() {
        return data[23];
    }

    public void setWillOfTheNecropolis(int value) {
        data[23] = value;
    }

    @TalentDataAttribute(index = 24, name = "Heart Strike", maxPoints = 1, tree = 0, column = 2, row = 9, prerequisite = -1)
    public int getHeartStrike() {
        return data[24];
    }

    public void setHeartStrike(int value) {
        data[24] = value;
    }

    @TalentDataAttribute(index = 25, name = "Might of Mograine", maxPoints = 3, tree = 0, column = 3, row = 9, prerequisite = -1)
    public int getMightOfMograine() {
        return data[25];
    }

    public void setMightOfMograine(int value) {
        data[25] = value;
    }

    @TalentDataAttribute(index = 26, name = "Blood Gorged", maxPoints = 5, tree = 0, column = 2, row = 10, prerequisite = -1)
    public int getBloodGorged() {
        return data[26];
    }

    public void setBloodGorged(int value) {
        data[26] = value;
    }

    @TalentDataAttribute(index = 27, name = "Dancing Rune Weapon", maxPoints = 1, tree = 0, column = 2, row = 11, prerequisite = -1)
    public int getDancingRuneWeapon() {
        return data[27];
    }

    public void setDancingRuneWeapon(int value) {
        data[27] = value;
    }

    @TalentDataAttribute(index = 28, name = "Improved Icy Touch", maxPoints = 3, tree = 1, column = 1, row = 1, prerequisite = -1)
    public int getImprovedIcyTouch() {
        return data[28];
    }

    public void setImprovedIcyTouch(int value) {
        data[28] = value;
    }

    @TalentDataAttribute(index = 29, name = "Runic Power Mastery", maxPoints = 2, tree = 1, column = 2, row = 1, prerequisite = -1)
    public int getRunicPowerMastery() {
        return data[29];
    }

    public void setRunicPowerMastery(int value) {
        data[29] = value;
    }

    @TalentDataAttribute(index = 30, name = "Toughness", maxPoints = 5, tree = 1, column = 3, row = 1, prerequisite = -1)
    public int getToughness() {
        return data[30];
    }

    public void setToughness(int value) {
        data[30] = value;
    }

    @TalentDataAttribute(index = 31, name = "Icy Reach", maxPoints = 2, tree = 1, column = 2, row = 2, prerequisite = -1)
    public int getIcyReach() {
        return data[31];
    }

    public void setIcyReach(int value) {
        data[31] = value;
    }

    @TalentDataAttribute(index = 32, name = "Black Ice", maxPoints = 5, tree = 1, column = 3, row = 2, prerequisite = -1)
    public int getBlackIce() {
        return data[32];
    }

    public void setBlackIce(int value) {
        data[32] = value;
    }

    @TalentDataAttribute(index = 33, name = "Nerves of Cold Steel", maxPoints = 3, tree = 1, column = 4, row = 2, prerequisite = -1)
    public int getNervesOfColdSteel() {
        return data[33];
    }

    public void setNervesOfColdSteel(int value) {
        data[33] = value;
    }

    @TalentDataAttribute(index = 34, name = "Icy Talons", maxPoints = 5, tree = 1, column = 1, row = 3, prerequisite = 28)
    public int getIcyTalons() {
        return data[34];
    }

    public void setIcyTalons(int value) {
        data[34] = value;
    }

    @TalentDataAttribute(index = 35, name = "Lichborne", maxPoints = 1, tree = 1, column = 2, row = 3, prerequisite = -1)
    public int getLichborne() {
        return data[35];
    }

    public void setLichborne(int value) {
        data[35] = value;
    }

    @TalentDataAttribute(index = 36, name = "Annihilation", maxPoints = 3, tree = 1, column = 3, row = 3, prerequisite = -1)
    public int getAnnihilation() {
        return data[36];
    }

    public void setAnnihilation(int value) {
        data[36] = value;
    }

    @TalentDataAttribute(index = 37, name = "Killing Machine", maxPoints = 5, tree = 1, column = 2, row = 4, prerequisite = -1)
    public int getKillingMachine() {
        return data[37];
    }

    public void setKillingMachine(int value) {
        data[37] = value;
    }

    @TalentDataAttribute(index = 38, name = "Chill of the Grave", maxPoints = 2, tree = 1, column = 3, row = 4, prerequisite = -1)
    public int getChillOfTheGrave() {
        return data[38];
    }

    public void setChillOfTheGrave(int value) {
        data[38] = value;
    }

    @TalentDataAttribute(index = 39, name = "Endless Winter", maxPoints = 2, tree = 1, column = 4, row = 4, prerequisite = -1)
    public int getEndlessWinter() {
        return data[39];
    }

    public void setEndlessWinter(int value) {
        data[39] = value;
    }

    @TalentDataAttribute(index = 40, name = "Frigid Dreadplate", maxPoints = 3, tree = 1, column = 2, row = 5, prerequisite = -1)
    public int getFrigidDreadplate() {
        return data[40];
    }

    public void setFrigidDreadplate(int value) {
        data[40] = value;
    }

    @TalentDataAttribute(index = 41, name = "Glacier Rot", maxPoints = 3, tree = 1, column = 3, row = 5, prerequisite = -1)
    public int getGlacierRot() {
        return data[41];
    }

    public void setGlacierRot(int value) {
        data[41] = value;
    }

    @TalentDataAttribute(index = 42, name = "Deathchill", maxPoints = 1, tree = 1, column = 4, row = 5, prerequisite = -1)
    public int getDeathchill() {
        return data[42];
    }

    public void setDeathchill(int value) {
        data[42] = value;
    }

    @TalentDataAttribute(index = 43, name = "Improved Icy Talons", maxPoints = 1, tree = 1, column = 1, row = 6, prerequisite = 34)
    public int getImprovedIcyTalons() {
        return data[43];
    }

    public void setImprovedIcyTalons(int value) {
        data[43] = value;
    }

    @TalentDataAttribute(index = 44, name = "Merciless Combat", maxPoints = 2, tree = 1, column = 2, row = 6, prerequisite = -1)
    public int getMercilessCombat() {
        return data[44];
    }

    public void setMercilessCombat(int value) {
        data[44] = value;
    }

    @TalentDataAttribute(index = 45, name = "Rime", maxPoints = 3, tree = 1, column = 3, row = 6, prerequisite = -1)
    public int getRime() {
        return data[45];
    }

    public void setRime(int value) {
        data[45] = value;
    }

    @TalentDataAttribute(index = 46, name = "Chilblains", maxPoints = 3, tree = 1, column = 1, row = 7, prerequisite = -1)
    public int getChilblains() {
        return data[46];
    }

    public void setChilblains(int value) {
        data[46] = value;
    }

    @TalentDataAttribute(index = 47, name = "Hungering Cold", maxPoints = 1, tree = 1, column = 2, row = 7, prerequisite = -1)
    public int getHungeringCold() {
        return data[47];
    }

    public void setHungeringCold(int value) {
        data[47] = value;
    }

    @TalentDataAttribute(index = 48, name = "Improved Frost Presence", maxPoints = 2, tree = 1, column = 3, row = 7, prerequisite = -1)
    public int getImprovedFrostPresence() {
        return data[48];
    }

    public void setImprovedFrostPresence(int value) {
        data[48] = value;
    }

    @TalentDataAttribute(index = 49, name = "Threat of Thassarian", maxPoints = 3, tree = 1, column = 1, row = 8, prerequisite = -1)
    public int getThreatOfThassarian() {
        return data[49];
    }

    public void setThreatOfThassarian(int value) {
        data[49] = value;
    }

    @TalentDataAttribute(index = 50, name = "Blood of the North", maxPoints = 3, tree = 1, column = 2, row = 8, prerequisite = -1)
    public int getBloodOfTheNorth() {
        return data[50];
    }

    public void setBloodOfTheNorth(int value) {
        data[50] = value;
    }

    @TalentDataAttribute(index = 51, name = "Unbreakable Armor", maxPoints = 1, tree = 1, column = 3, row = 8, prerequisite = -1)
    public int getUnbreakableArmor() {
        return data[51];
    }

    public void setUnbreakableArmor(int value) {
        data[51] = value;
    }

    @TalentDataAttribute(index = 52, name = "Acclimation", maxPoints = 3, tree = 1, column = 1, row = 9, prerequisite = -1)
    public int getAcclimation() {
        return data[52];
    }

    public void setAcclimation(int value) {
        data[52] = value;
    }

    @TalentDataAttribute(index = 53, name = "Frost Strike", maxPoints = 1, tree = 1, column = 2, row = 9, prerequisite = -1)
    public int getFrostStrike() {
        return data[53];
    }

    public void setFrostStrike(int value) {
        data[53] = value;
    }

    @TalentDataAttribute(index = 54, name = "Guile of Gorefiend", maxPoints = 3, tree = 1, column = 3, row = 9, prerequisite = -1)
    public int getGuileOfGorefiend() {
        return data[54];
    }

    public void setGuileOfGorefiend(int value) {
        data[54] = value;
    }

    @TalentDataAttribute(index = 55, name = "Tundra Stalker", maxPoints = 5, tree = 1, column = 2, row = 10, prerequisite = -1)
    public int getTundraStalker() {
        return data[55];
    }

    public void setTundraStalker(int value) {
        data[55] = value;
    }

    @TalentDataAttribute(index = 56, name = "Howling Blast", maxPoints = 1, tree = 1, column = 2, row = 11, prerequisite = -1)
    public int getHowlingBlast() {
        return data[56];
    }

    public void setHowlingBlast(int value) {
        data[56] = value;
    }

    @TalentDataAttribute(index = 57, name = "Vicious Strikes", maxPoints = 2, tree = 2, column = 1, row = 1, prerequisite = -1)
    public int getViciousStrikes() {
        return data[57];
    }

    public void setViciousStrikes(int value) {
        data[57] = value;
    }

    @TalentDataAttribute(index = 58, name = "Virulence", maxPoints = 3, tree = 2, column = 2, row = 1, prerequisite = -1)
    public int getVirulence() {
        return data[58];
    }

    public void setVirulence(int value) {
        data[58] = value;
    }

    @TalentDataAttribute(index = 59, name = "Anticipation", maxPoints = 5, tree = 2, column = 3, row = 1, prerequisite = -1)
    public int getAnticipation() {
        return data[59];
    }

    public void setAnticipation(int value) {
        data[59] = value;
    }

    @TalentDataAttribute(index = 60, name = "Epidemic", maxPoints = 2, tree = 2, column = 1, row = 2, prerequisite = -1)
    public int getEpidemic() {
        return data[60];
    }

    public void setEpidemic(int value) {
        data[60] = value;
    }

    @TalentDataAttribute(index = 61, name = "Morbidity", maxPoints = 3, tree = 2, column = 2, row = 2, prerequisite = -1)
    public int getMorbidity() {
        return data[61];
    }

    public void setMorbidity(int value) {
        data[61] = value;
    }

    @TalentDataAttribute(index = 62, name = "Unholy Command", maxPoints = 2, tree = 2, column = 3, row = 2, prerequisite = -1)
    public int getUnholyCommand() {
        return data[62];
    }

    public void setUnholyCommand(int value) {
        data[62] = value;
    }

    @TalentDataAttribute(index = 63, name = "Ravenous Dead", maxPoints = 3, tree = 2, column = 4, row = 2, prerequisite = -1)
    public int getRavenousDead() {
        return data[63];
    }

    public void setRavenousDead(int value) {
        data[63] = value;
    }

    @TalentDataAttribute(index = 64, name = "Outbreak", maxPoints = 3, tree = 2, column = 1, row = 3, prerequisite = -1)
    public int getOutbreak() {
        return data[64];
    }

    public void setOutbreak(int value) {
        data[64] = value;
    }

    @TalentDataAttribute(index = 65, name = "Necrosis", maxPoints = 5, tree = 2, column = 2, row = 3, prerequisite = -1)
    public int getNecrosis() {
        return data[65];
    }

    public void setNecrosis(int value) {
        data[65] = value;
    }

    @TalentDataAttribute(index = 66, name = "Corpse Explosion", maxPoints = 1, tree = 2, column = 3, row = 3, prerequisite = -1)
    public int getCorpseExplosion() {
        return data[66];
    }

    public void setCorpseExplosion(int value) {
        data[66] = value;
    }

    @TalentDataAttribute(index = 67, name = "On a Pale Horse", maxPoints = 2, tree = 2, column = 2, row = 4, prerequisite = -1)
    public int getOnAPaleHorse() {
        return data[67];
    }

    public void setOnAPaleHorse(int value) {
        data[67] = value;
    }

    @TalentDataAttribute(index = 68, name = "Blood-Caked Blade", maxPoints = 3, tree = 2, column = 3, row = 4, prerequisite = -1)
    public int getBloodCakedBlade() {
        return data[68];
    }

    public void setBloodCakedBlade(int value) {
        data[68] = value;
    }

    @TalentDataAttribute(index = 69, name = "Night of the Dead", maxPoints = 2, tree = 2, column = 4, row = 4, prerequisite = -1)
    public int getNightOfTheDead() {
        return data[69];
    }

    public void setNightOfTheDead(int value) {
        data[69] = value;
    }

    @TalentDataAttribute(index = 70, name = "Unholy Blight", maxPoints = 1, tree = 2, column = 1, row = 5, prerequisite = -1)
    public int getUnholyBlight() {
        return data[70];
    }

    public void setUnholyBlight(int value) {
        data[70] = value;
    }

    @TalentDataAttribute(index = 71, name = "Impurity", maxPoints = 5, tree = 2, column = 2, row = 5, prerequisite = -1)
    public int getImpurity() {
        return data[71];
    }

    public void setImpurity(int value) {
        data[71] = value;
    }

    @TalentDataAttribute(index = 72, name = "Dirge", maxPoints = 2, tree = 2, column = 3, row = 5, prerequisite = -1)
    public int getDirge() {
        return data[72];
    }

    public void setDirge(int value) {
        data[72] = value;
    }

    @TalentDataAttribute(index = 73, name = "Desecration", maxPoints = 2, tree = 2, column = 1, row = 6, prerequisite = -1)
    public int getDesecration() {
        return data[73];
    }

    public void setDesecration(int value) {
        data[73] = value;
    }

    @TalentDataAttribute(index = 74, name = "Magic Suppression", maxPoints = 3, tree = 2, column = 2, row = 6, prerequisite = -1)
    public int getMagicSuppression() {
        return data[74];
    }

    public void setMagicSuppression(int value) {
        data[74] = value;
    }

    @TalentDataAttribute(index = 75, name = "Reaping", maxPoints = 3, tree = 2, column = 3, row = 6, prerequisite = -1)
    public int getReaping() {
        return data[75];
    }

    public void setReaping(int value) {
        data[75] = value;
    }

    @TalentDataAttribute(index = 76, name = "Master of Ghouls", maxPoints = 1, tree = 2, column = 4, row = 6, prerequisite = 69)
    public int getMasterOfGhouls() {
        return data[76];
    }

    public void setMasterOfGhouls(int value) {
        data[76] = value;
    }

    @TalentDataAttribute(index = 77, name = "Desolation", maxPoints = 5, tree = 2, column = 1, row = 7, prerequisite = -1)
    public int getDesolation() {
        return data[77];
    }

    public void setDesolation(int value) {
        data[77] = value;
    }

    @TalentDataAttribute(index = 78, name = "Anti-Magic Zone", maxPoints = 1, tree = 2, column = 2, row = 7, prerequisite = 74)
    public int getAntiMagicZone() {
        return data[78];
    }

    public void setAntiMagicZone(int value) {
        data[78] = value;
    }

    @TalentDataAttribute(index = 79, name = "Improved Unholy Presence", maxPoints = 2, tree = 2, column = 3, row = 7, prerequisite = -1)
    public int getImprovedUnholyPresence() {
        return data[79];
    }

    public void setImprovedUnholyPresence(int value) {
        data[79] = value;
    }

    @TalentDataAttribute(index = 80, name = "Ghoul Frenzy", maxPoints = 1, tree = 2, column = 4, row = 7, prerequisite = 76)
    public int getGhoulFrenzy() {
        return data[80];
    }

    public void setGhoulFrenzy(int value) {
        data[80] = value;
    }

    @TalentDataAttribute(index = 81, name = "Crypt Fever", maxPoints = 3, tree = 2, column = 2, row = 8, prerequisite = -1)
    public int getCryptFever() {
        return data[81];
    }

    public void setCryptFever(int value) {
        data[81] = value;
    }

    @TalentDataAttribute(index = 82, name = "Bone Shield", maxPoints = 1, tree = 2, column = 3, row = 8, prerequisite = -1)
    public int getBoneShield() {
        return data[82];
    }

    public void setBoneShield(int value) {
        data[82] = value;
    }

    @TalentDataAttribute(index = 83, name = "Wandering Plague", maxPoints = 3, tree = 2, column = 1, row = 9, prerequisite = -1)
    public int getWanderingPlague() {
        return data[83];
    }

    public void setWanderingPlague(int value) {
        data[83] = value;
    }

    @TalentDataAttribute(index = 84, name = "Ebon Plaguebringer", maxPoints = 3, tree = 2, column = 2, row = 9, prerequisite = 81)
    public int getEbonPlaguebringer() {
        return data[84];
    }

    public void setEbonPlaguebringer(int value) {
        data[84] = value;
    }

    @TalentDataAttribute(index = 85, name = "Scourge Strike", maxPoints = 1, tree = 2, column = 3, row = 9, prerequisite = -1)
    public int getScourgeStrike() {
        return data[85];
    }

    public void setScourgeStrike(int value) {
        data[85] = value;
    }

    @TalentDataAttribute(index = 86, name = "Rage of Rivendare", maxPoints = 5, tree = 2, column = 2, row = 10, prerequisite = -1)
    public int getRageOfRivendare() {
        return data[86];
    }

    public void setRageOfRivendare(int value) {
        data[86] = value;
    }

    @TalentDataAttribute(index = 87, name = "Summon Gargoyle", maxPoints = 1, tree = 2, column = 2, row = 11, prerequisite = -1)
    public int getSummonGargoyle() {
        return data[87];
    }

    public void setSummonGargoyle(int value) {
        data[87] = value;
    }

    @GlyphDataAttribute(index = 0, name = "Glyph of Anti-Magic Shell", major = true)
    public boolean isGlyphofAntiMagicShell() {
        return glyphData[0];
    }

    public void setGlyphofAntiMagicShell(boolean value) {
        glyphData[0] = value;
    }

    @GlyphDataAttribute(index = 1, name = "Glyph of Blood Strike", major = true)
    public boolean isGlyphofBloodStrike() {
        return glyphData[1];
    }

    public void setGlyphofBloodStrike(boolean value) {
        glyphData[1] = value;
    }

    @GlyphDataAttribute(index = 2, name = "Glyph of Bone Shield", major = true)
    public boolean isGlyphofBoneShield() {
        return glyphData[2];
    }

    public void setGlyphofBoneShield(boolean value) {
        glyphData[2] = value;
    }

    @GlyphDataAttribute(index = 3, name = "Glyph of Chains of Ice", major = true)
    public boolean isGlyphofChainsofIce() {
        return glyphData[3];
    }

    public void setGlyphofChainsofIce(boolean value) {
        glyphData[3] = value;
    }

    @GlyphDataAttribute(index = 4, name = "Glyph of Dancing Rune Weapon", major = true)
    public boolean isGlyphofDancingRuneWeapon() {
        return glyphData[4];
    }

    public void setGlyphofDancingRuneWeapon(boolean value) {
        glyphData[4] = value;
    }

    @GlyphDataAttribute(index = 5, name = "Glyph of Dark Command", major = true)
    public boolean isGlyphofDarkCommand() {
        return glyphData[5];
    }

    public void setGlyphofDarkCommand(boolean value) {
        glyphData[5] = value;
    }

    @GlyphDataAttribute(index = 6, name = "Glyph of Dark Death", major = true)
    public boolean isGlyphofDarkDeath() {
        return glyphData[6];
    }

    public void setGlyphofDarkDeath(boolean value) {
        glyphData[6] = value;
    }

    @GlyphDataAttribute(index = 7, name = "Glyph of Death and Decay", major = true)
    public boolean isGlyphofDeathandDecay() {
        return glyphData[7];
    }

    public void setGlyphofDeathandDecay(boolean value) {
        glyphData[7] = value;
    }

    @GlyphDataAttribute(index = 8, name = "Glyph of Death Grip", major = true)
    public boolean isGlyphofDeathGrip() {
        return glyphData[8];
    }

    public void setGlyphofDeathGrip(boolean value) {
        glyphData[8] = value;
    }

    @GlyphDataAttribute(index = 9, name = "Glyph of Death Strike", major = true)
    public boolean isGlyphofDeathStrike() {
        return glyphData[9];
    }

    public void setGlyphofDeathStrike(boolean value) {
        glyphData[9] = value;
    }

    @GlyphDataAttribute(index = 10, name = "Glyph of Disease", major = true)
    public boolean isGlyphofDisease() {
        return glyphData[10];
    }

    public void setGlyphofDisease(boolean value) {
        glyphData[10] = value;
    }

    @GlyphDataAttribute(index = 11, name = "Glyph of Frost Strike", major = true)
    public boolean isGlyphofFrostStrike() {
        return glyphData[11];
    }

    public void setGlyphofFrostStrike(boolean value) {
        glyphData[11] = value;
    }

    @GlyphDataAttribute(index = 12, name = "Glyph of Heart Strike", major = true)
    public boolean isGlyphofHeartStrike() {
        return glyphData[12];
    }

    public void setGlyphofHeartStrike(boolean value) {
        glyphData[12] = value;
    }

    @GlyphDataAttribute(index = 13, name = "Glyph of Howling Blast", major = true)
    public boolean isGlyphofHowlingBlast() {
        return glyphData[13];
    }

    public void setGlyphofHowlingBlast(boolean value) {
        glyphData[13] = value;
    }

    @GlyphDataAttribute(index = 14, name = "Glyph of Hungering Cold", major = true)
    public boolean isGlyphofHungeringCold() {
        return glyphData[14];
    }

    public void setGlyphofHungeringCold(boolean value) {
        glyphData[14] = value;
    }

    @GlyphDataAttribute(index = 15, name = "Glyph of Icebound Fortitude", major = true)
    public boolean isGlyphofIceboundFortitude() {
        return glyphData[15];
    }

    public void setGlyphofIceboundFortitude(boolean value) {
        glyphData[15] = value;
    }

    @GlyphDataAttribute(index = 16, name = "Glyph of Icy Touch", major = true)
    public boolean isGlyphofIcyTouch() {
        return glyphData[16];
    }

    public void setGlyphofIcyTouch(boolean value) {
        glyphData[16] = value;
    }

    @GlyphDataAttribute(index = 17, name = "Glyph of Obliterate", major = true)
    public boolean isGlyphofObliterate() {
        return glyphData[17];
    }

    public void setGlyphofObliterate(boolean value) {
        glyphData[17] = value;
    }

    @GlyphDataAttribute(index = 18, name = "Glyph of Plague Strike", major = true)
    public boolean isGlyphofPlagueStrike() {
        return glyphData[18];
    }

    public void setGlyphofPlagueStrike(boolean value) {
        glyphData[18] = value;
    }

    @GlyphDataAttribute(index = 19, name = "Glyph of Rune Strike", major = true)
    public boolean isGlyphofRuneStrike() {
        return glyphData[19];
    }

    public void setGlyphofRuneStrike(boolean value) {
        glyphData[19] = value;
    }

    @GlyphDataAttribute(index = 20, name = "Glyph of Rune Tap", major = true)
    public boolean isGlyphofRuneTap() {
        return glyphData[20];
    }

    public void setGlyphofRuneTap(boolean value) {
        glyphData[20] = value;
    }

    @GlyphDataAttribute(index = 21, name = "Glyph of Scourge Strike", major = true)
    public boolean isGlyphofScourgeStrike() {
        return glyphData[21];
    }

    public void setGlyphofScourgeStrike(boolean value) {
        glyphData[21] = value;
    }

    @GlyphDataAttribute(index = 22, name = "Glyph of Strangulate", major = true)
    public boolean isGlyphofStrangulate() {
        return glyphData[22];
    }

    public void setGlyphofStrangulate(boolean value) {
        glyphData[22] = value;
    }

    @GlyphDataAttribute(index = 23, name = "Glyph of the Ghoul", major = true)
    public boolean isGlyphoftheGhoul() {
        return glyphData[23];
    }

    public void setGlyphoftheGhoul(boolean value) {
        glyphData[23] = value;
    }

    @GlyphDataAttribute(index = 24, name = "Glyph of Unbreakable Armor", major = true)
    public boolean isGlyphofUnbreakableArmor() {
        return glyphData[24];
    }

    public void setGlyphofUnbreakableArmor(boolean value) {
        glyphData[24] = value;
    }

    @GlyphDataAttribute(index = 25, name = "Glyph of Unholy Blight", major = true)
    public boolean isGlyphofUnholyBlight() {
        return glyphData[25];
    }

    public void setGlyphofUnholyBlight(boolean value) {
        glyphData[25] = value;
    }

    @GlyphDataAttribute(index = 26, name = "Glyph of Vampiric Blood", major = true)
    public boolean isGlyphofVampiricBlood() {
        return glyphData[26];
    }

    public void setGlyphofVampiricBlood(boolean value) {
        glyphData[26] = value;
    }

    @GlyphDataAttribute(index = 27, name = "Glyph of Blood Tap", major = false)
    public boolean isGlyphofBloodTap() {
        return glyphData[27];
    }

    public void setGlyphofBloodTap(boolean value) {
        glyphData[27] = value;
    }

    @GlyphDataAttribute(index = 28, name = "Glyph of Corpse Explosion", major = false)
    public boolean isGlyphofCorpseExplosion() {
        return glyphData[28];
    }

    public void setGlyphofCorpseExplosion(boolean value) {
        glyphData[28] = value;
    }

    @GlyphDataAttribute(index = 29, name = "Glyph of Death's Embrace", major = false)
    public boolean isGlyphofDeathsEmbrace() {
        return glyphData[29];
    }

    public void setGlyphofDeathsEmbrace(boolean value) {
        glyphData[29] = value;
    }

    @GlyphDataAttribute(index = 30, name = "Glyph of Horn of Winter", major = false)
    public boolean isGlyphofHornofWinter() {
        return glyphData[30];
    }

    public void setGlyphofHornofWinter(boolean value) {
        glyphData[30] = value;
    }

    @GlyphDataAttribute(index = 31, name = "Glyph of Pestilence", major = false)
    public boolean isGlyphofPestilence() {
        return glyphData[31];
    }

    public void setGlyphofPestilence(boolean value) {
        glyphData[31] = value;
    }

    @GlyphDataAttribute(index = 32, name = "Glyph of Raise Dead", major = false)
    public boolean isGlyphofRaiseDead() {
        return glyphData[32];
    }

    public void setGlyphofRaiseDead(boolean value) {
        glyphData[32] = value;
    }

}
