// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassTalents.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.talents;

/**
 * The mage's talent and glyph allocation: the Arcane, Fire and Frost trees plus 19 glyph slots, all
 * flagged major in this table.
 *
 * <p>The mage is the pilot model of the port, and this class is its character sheet. {@code data}
 * has 86 rank slots, contiguous per tree: Arcane 0-29 (tree 0), Fire 30-57 (tree 1), Frost 58-85
 * (tree 2); {@link #TreeNames} fixes that order and each getter's {@link TalentDataAttribute}
 * declares its row, column and prerequisite. Models read ranks through {@code getXxx()} (e.g.
 * {@link #getArcaneConcentration()}, {@link #getMissileBarrage()}, {@link #getHotStreak()}) and
 * glyphs through {@code isGlyphOfXxx()} (e.g. {@link #isGlyphOfArcaneBlast()},
 * {@link #isGlyphOfLivingBomb()}); the mage {@code Solver} additionally sums the rank ranges of the
 * three trees to find the one with the most points invested.</p>
 *
 * <p>{@code glyphData} has 19 flags, all flagged major (no minor glyphs are modelled). The
 * {@code String} constructor rebuilds the allocation from the compact form stored in saved
 * character XML, and {@link #clone()} returns an independent deep copy; both follow
 * {@link TalentsBase}.</p>
 */
public class MageTalents extends TalentsBase {
    public static final String[] TreeNames = new String[] {
        "Arcane",
        "Fire",
        "Frost",
    };

    private final int[] data = new int[86];
    private final boolean[] glyphData = new boolean[19];

    public MageTalents() {
    }

    public MageTalents(String talents) {
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

    public MageTalents clone() {
        MageTalents clone = new MageTalents();
        System.arraycopy(data, 0, clone.data, 0, data.length);
        System.arraycopy(glyphData, 0, clone.glyphData, 0, glyphData.length);
        return clone;
    }

    @TalentDataAttribute(index = 0, name = "Arcane Subtlety", maxPoints = 2, tree = 0, column = 1, row = 1, prerequisite = -1)
    public int getArcaneSubtlety() {
        return data[0];
    }

    public void setArcaneSubtlety(int value) {
        data[0] = value;
    }

    @TalentDataAttribute(index = 1, name = "Arcane Focus", maxPoints = 3, tree = 0, column = 2, row = 1, prerequisite = -1)
    public int getArcaneFocus() {
        return data[1];
    }

    public void setArcaneFocus(int value) {
        data[1] = value;
    }

    @TalentDataAttribute(index = 2, name = "Arcane Stability", maxPoints = 5, tree = 0, column = 3, row = 1, prerequisite = -1)
    public int getArcaneStability() {
        return data[2];
    }

    public void setArcaneStability(int value) {
        data[2] = value;
    }

    @TalentDataAttribute(index = 3, name = "Arcane Fortitude", maxPoints = 3, tree = 0, column = 1, row = 2, prerequisite = -1)
    public int getArcaneFortitude() {
        return data[3];
    }

    public void setArcaneFortitude(int value) {
        data[3] = value;
    }

    @TalentDataAttribute(index = 4, name = "Magic Absorption", maxPoints = 2, tree = 0, column = 2, row = 2, prerequisite = -1)
    public int getMagicAbsorption() {
        return data[4];
    }

    public void setMagicAbsorption(int value) {
        data[4] = value;
    }

    @TalentDataAttribute(index = 5, name = "Arcane Concentration", maxPoints = 5, tree = 0, column = 3, row = 2, prerequisite = -1)
    public int getArcaneConcentration() {
        return data[5];
    }

    public void setArcaneConcentration(int value) {
        data[5] = value;
    }

    @TalentDataAttribute(index = 6, name = "Magic Attunement", maxPoints = 2, tree = 0, column = 1, row = 3, prerequisite = -1)
    public int getMagicAttunement() {
        return data[6];
    }

    public void setMagicAttunement(int value) {
        data[6] = value;
    }

    @TalentDataAttribute(index = 7, name = "Spell Impact", maxPoints = 3, tree = 0, column = 2, row = 3, prerequisite = -1)
    public int getSpellImpact() {
        return data[7];
    }

    public void setSpellImpact(int value) {
        data[7] = value;
    }

    @TalentDataAttribute(index = 8, name = "Student of the Mind", maxPoints = 3, tree = 0, column = 3, row = 3, prerequisite = -1)
    public int getStudentOfTheMind() {
        return data[8];
    }

    public void setStudentOfTheMind(int value) {
        data[8] = value;
    }

    @TalentDataAttribute(index = 9, name = "Focus Magic", maxPoints = 1, tree = 0, column = 4, row = 3, prerequisite = -1)
    public int getFocusMagic() {
        return data[9];
    }

    public void setFocusMagic(int value) {
        data[9] = value;
    }

    @TalentDataAttribute(index = 10, name = "Arcane Shielding", maxPoints = 2, tree = 0, column = 1, row = 4, prerequisite = -1)
    public int getArcaneShielding() {
        return data[10];
    }

    public void setArcaneShielding(int value) {
        data[10] = value;
    }

    @TalentDataAttribute(index = 11, name = "Improved Counterspell", maxPoints = 2, tree = 0, column = 2, row = 4, prerequisite = -1)
    public int getImprovedCounterspell() {
        return data[11];
    }

    public void setImprovedCounterspell(int value) {
        data[11] = value;
    }

    @TalentDataAttribute(index = 12, name = "Arcane Meditation", maxPoints = 3, tree = 0, column = 3, row = 4, prerequisite = -1)
    public int getArcaneMeditation() {
        return data[12];
    }

    public void setArcaneMeditation(int value) {
        data[12] = value;
    }

    @TalentDataAttribute(index = 13, name = "Torment the Weak", maxPoints = 3, tree = 0, column = 4, row = 4, prerequisite = -1)
    public int getTormentTheWeak() {
        return data[13];
    }

    public void setTormentTheWeak(int value) {
        data[13] = value;
    }

    @TalentDataAttribute(index = 14, name = "Improved Blink", maxPoints = 2, tree = 0, column = 1, row = 5, prerequisite = -1)
    public int getImprovedBlink() {
        return data[14];
    }

    public void setImprovedBlink(int value) {
        data[14] = value;
    }

    @TalentDataAttribute(index = 15, name = "Presence of Mind", maxPoints = 1, tree = 0, column = 2, row = 5, prerequisite = -1)
    public int getPresenceOfMind() {
        return data[15];
    }

    public void setPresenceOfMind(int value) {
        data[15] = value;
    }

    @TalentDataAttribute(index = 16, name = "Arcane Mind", maxPoints = 5, tree = 0, column = 4, row = 5, prerequisite = -1)
    public int getArcaneMind() {
        return data[16];
    }

    public void setArcaneMind(int value) {
        data[16] = value;
    }

    @TalentDataAttribute(index = 17, name = "Prismatic Cloak", maxPoints = 3, tree = 0, column = 1, row = 6, prerequisite = -1)
    public int getPrismaticCloak() {
        return data[17];
    }

    public void setPrismaticCloak(int value) {
        data[17] = value;
    }

    @TalentDataAttribute(index = 18, name = "Arcane Instability", maxPoints = 3, tree = 0, column = 2, row = 6, prerequisite = 15)
    public int getArcaneInstability() {
        return data[18];
    }

    public void setArcaneInstability(int value) {
        data[18] = value;
    }

    @TalentDataAttribute(index = 19, name = "Arcane Potency", maxPoints = 2, tree = 0, column = 3, row = 6, prerequisite = 15)
    public int getArcanePotency() {
        return data[19];
    }

    public void setArcanePotency(int value) {
        data[19] = value;
    }

    @TalentDataAttribute(index = 20, name = "Arcane Empowerment", maxPoints = 3, tree = 0, column = 1, row = 7, prerequisite = -1)
    public int getArcaneEmpowerment() {
        return data[20];
    }

    public void setArcaneEmpowerment(int value) {
        data[20] = value;
    }

    @TalentDataAttribute(index = 21, name = "Arcane Power", maxPoints = 1, tree = 0, column = 2, row = 7, prerequisite = 18)
    public int getArcanePower() {
        return data[21];
    }

    public void setArcanePower(int value) {
        data[21] = value;
    }

    @TalentDataAttribute(index = 22, name = "Incanter's Absorption", maxPoints = 3, tree = 0, column = 3, row = 7, prerequisite = -1)
    public int getIncantersAbsorption() {
        return data[22];
    }

    public void setIncantersAbsorption(int value) {
        data[22] = value;
    }

    @TalentDataAttribute(index = 23, name = "Arcane Flows", maxPoints = 2, tree = 0, column = 2, row = 8, prerequisite = 21)
    public int getArcaneFlows() {
        return data[23];
    }

    public void setArcaneFlows(int value) {
        data[23] = value;
    }

    @TalentDataAttribute(index = 24, name = "Mind Mastery", maxPoints = 5, tree = 0, column = 3, row = 8, prerequisite = -1)
    public int getMindMastery() {
        return data[24];
    }

    public void setMindMastery(int value) {
        data[24] = value;
    }

    @TalentDataAttribute(index = 25, name = "Slow", maxPoints = 1, tree = 0, column = 2, row = 9, prerequisite = -1)
    public int getSlow() {
        return data[25];
    }

    public void setSlow(int value) {
        data[25] = value;
    }

    @TalentDataAttribute(index = 26, name = "Missile Barrage", maxPoints = 5, tree = 0, column = 3, row = 9, prerequisite = -1)
    public int getMissileBarrage() {
        return data[26];
    }

    public void setMissileBarrage(int value) {
        data[26] = value;
    }

    @TalentDataAttribute(index = 27, name = "Netherwind Presence", maxPoints = 3, tree = 0, column = 2, row = 10, prerequisite = -1)
    public int getNetherwindPresence() {
        return data[27];
    }

    public void setNetherwindPresence(int value) {
        data[27] = value;
    }

    @TalentDataAttribute(index = 28, name = "Spell Power", maxPoints = 2, tree = 0, column = 3, row = 10, prerequisite = -1)
    public int getSpellPower() {
        return data[28];
    }

    public void setSpellPower(int value) {
        data[28] = value;
    }

    @TalentDataAttribute(index = 29, name = "Arcane Barrage", maxPoints = 1, tree = 0, column = 2, row = 11, prerequisite = -1)
    public int getArcaneBarrage() {
        return data[29];
    }

    public void setArcaneBarrage(int value) {
        data[29] = value;
    }

    @TalentDataAttribute(index = 30, name = "Improved Fire Blast", maxPoints = 2, tree = 1, column = 1, row = 1, prerequisite = -1)
    public int getImprovedFireBlast() {
        return data[30];
    }

    public void setImprovedFireBlast(int value) {
        data[30] = value;
    }

    @TalentDataAttribute(index = 31, name = "Incineration", maxPoints = 3, tree = 1, column = 2, row = 1, prerequisite = -1)
    public int getIncineration() {
        return data[31];
    }

    public void setIncineration(int value) {
        data[31] = value;
    }

    @TalentDataAttribute(index = 32, name = "Improved Fireball", maxPoints = 5, tree = 1, column = 3, row = 1, prerequisite = -1)
    public int getImprovedFireball() {
        return data[32];
    }

    public void setImprovedFireball(int value) {
        data[32] = value;
    }

    @TalentDataAttribute(index = 33, name = "Ignite", maxPoints = 5, tree = 1, column = 1, row = 2, prerequisite = -1)
    public int getIgnite() {
        return data[33];
    }

    public void setIgnite(int value) {
        data[33] = value;
    }

    @TalentDataAttribute(index = 34, name = "Burning Determination", maxPoints = 2, tree = 1, column = 2, row = 2, prerequisite = -1)
    public int getBurningDetermination() {
        return data[34];
    }

    public void setBurningDetermination(int value) {
        data[34] = value;
    }

    @TalentDataAttribute(index = 35, name = "World in Flames", maxPoints = 3, tree = 1, column = 3, row = 2, prerequisite = -1)
    public int getWorldInFlames() {
        return data[35];
    }

    public void setWorldInFlames(int value) {
        data[35] = value;
    }

    @TalentDataAttribute(index = 36, name = "Flame Throwing", maxPoints = 2, tree = 1, column = 1, row = 3, prerequisite = -1)
    public int getFlameThrowing() {
        return data[36];
    }

    public void setFlameThrowing(int value) {
        data[36] = value;
    }

    @TalentDataAttribute(index = 37, name = "Impact", maxPoints = 3, tree = 1, column = 2, row = 3, prerequisite = -1)
    public int getImpact() {
        return data[37];
    }

    public void setImpact(int value) {
        data[37] = value;
    }

    @TalentDataAttribute(index = 38, name = "Pyroblast", maxPoints = 1, tree = 1, column = 3, row = 3, prerequisite = -1)
    public int getPyroblast() {
        return data[38];
    }

    public void setPyroblast(int value) {
        data[38] = value;
    }

    @TalentDataAttribute(index = 39, name = "Burning Soul", maxPoints = 2, tree = 1, column = 4, row = 3, prerequisite = -1)
    public int getBurningSoul() {
        return data[39];
    }

    public void setBurningSoul(int value) {
        data[39] = value;
    }

    @TalentDataAttribute(index = 40, name = "Improved Scorch", maxPoints = 3, tree = 1, column = 1, row = 4, prerequisite = -1)
    public int getImprovedScorch() {
        return data[40];
    }

    public void setImprovedScorch(int value) {
        data[40] = value;
    }

    @TalentDataAttribute(index = 41, name = "Molten Shields", maxPoints = 2, tree = 1, column = 2, row = 4, prerequisite = -1)
    public int getMoltenShields() {
        return data[41];
    }

    public void setMoltenShields(int value) {
        data[41] = value;
    }

    @TalentDataAttribute(index = 42, name = "Master of Elements", maxPoints = 3, tree = 1, column = 4, row = 4, prerequisite = -1)
    public int getMasterOfElements() {
        return data[42];
    }

    public void setMasterOfElements(int value) {
        data[42] = value;
    }

    @TalentDataAttribute(index = 43, name = "Playing with Fire", maxPoints = 3, tree = 1, column = 1, row = 5, prerequisite = -1)
    public int getPlayingWithFire() {
        return data[43];
    }

    public void setPlayingWithFire(int value) {
        data[43] = value;
    }

    @TalentDataAttribute(index = 44, name = "Critical Mass", maxPoints = 3, tree = 1, column = 2, row = 5, prerequisite = -1)
    public int getCriticalMass() {
        return data[44];
    }

    public void setCriticalMass(int value) {
        data[44] = value;
    }

    @TalentDataAttribute(index = 45, name = "Blast Wave", maxPoints = 1, tree = 1, column = 3, row = 5, prerequisite = 38)
    public int getBlastWave() {
        return data[45];
    }

    public void setBlastWave(int value) {
        data[45] = value;
    }

    @TalentDataAttribute(index = 46, name = "Blazing Speed", maxPoints = 2, tree = 1, column = 1, row = 6, prerequisite = -1)
    public int getBlazingSpeed() {
        return data[46];
    }

    public void setBlazingSpeed(int value) {
        data[46] = value;
    }

    @TalentDataAttribute(index = 47, name = "Fire Power", maxPoints = 5, tree = 1, column = 3, row = 6, prerequisite = -1)
    public int getFirePower() {
        return data[47];
    }

    public void setFirePower(int value) {
        data[47] = value;
    }

    @TalentDataAttribute(index = 48, name = "Pyromaniac", maxPoints = 3, tree = 1, column = 1, row = 7, prerequisite = -1)
    public int getPyromaniac() {
        return data[48];
    }

    public void setPyromaniac(int value) {
        data[48] = value;
    }

    @TalentDataAttribute(index = 49, name = "Combustion", maxPoints = 1, tree = 1, column = 2, row = 7, prerequisite = 44)
    public int getCombustion() {
        return data[49];
    }

    public void setCombustion(int value) {
        data[49] = value;
    }

    @TalentDataAttribute(index = 50, name = "Molten Fury", maxPoints = 2, tree = 1, column = 3, row = 7, prerequisite = -1)
    public int getMoltenFury() {
        return data[50];
    }

    public void setMoltenFury(int value) {
        data[50] = value;
    }

    @TalentDataAttribute(index = 51, name = "Fiery Payback", maxPoints = 2, tree = 1, column = 1, row = 8, prerequisite = -1)
    public int getFieryPayback() {
        return data[51];
    }

    public void setFieryPayback(int value) {
        data[51] = value;
    }

    @TalentDataAttribute(index = 52, name = "Empowered Fire", maxPoints = 3, tree = 1, column = 3, row = 8, prerequisite = -1)
    public int getEmpoweredFire() {
        return data[52];
    }

    public void setEmpoweredFire(int value) {
        data[52] = value;
    }

    @TalentDataAttribute(index = 53, name = "Firestarter", maxPoints = 2, tree = 1, column = 1, row = 9, prerequisite = 54)
    public int getFirestarter() {
        return data[53];
    }

    public void setFirestarter(int value) {
        data[53] = value;
    }

    @TalentDataAttribute(index = 54, name = "Dragon's Breath", maxPoints = 1, tree = 1, column = 2, row = 9, prerequisite = 49)
    public int getDragonsBreath() {
        return data[54];
    }

    public void setDragonsBreath(int value) {
        data[54] = value;
    }

    @TalentDataAttribute(index = 55, name = "Hot Streak", maxPoints = 3, tree = 1, column = 3, row = 9, prerequisite = -1)
    public int getHotStreak() {
        return data[55];
    }

    public void setHotStreak(int value) {
        data[55] = value;
    }

    @TalentDataAttribute(index = 56, name = "Burnout", maxPoints = 5, tree = 1, column = 2, row = 10, prerequisite = -1)
    public int getBurnout() {
        return data[56];
    }

    public void setBurnout(int value) {
        data[56] = value;
    }

    @TalentDataAttribute(index = 57, name = "Living Bomb", maxPoints = 1, tree = 1, column = 2, row = 11, prerequisite = -1)
    public int getLivingBomb() {
        return data[57];
    }

    public void setLivingBomb(int value) {
        data[57] = value;
    }

    @TalentDataAttribute(index = 58, name = "Frostbite", maxPoints = 3, tree = 2, column = 1, row = 1, prerequisite = -1)
    public int getFrostbite() {
        return data[58];
    }

    public void setFrostbite(int value) {
        data[58] = value;
    }

    @TalentDataAttribute(index = 59, name = "Improved Frostbolt", maxPoints = 5, tree = 2, column = 2, row = 1, prerequisite = -1)
    public int getImprovedFrostbolt() {
        return data[59];
    }

    public void setImprovedFrostbolt(int value) {
        data[59] = value;
    }

    @TalentDataAttribute(index = 60, name = "Ice Floes", maxPoints = 3, tree = 2, column = 3, row = 1, prerequisite = -1)
    public int getIceFloes() {
        return data[60];
    }

    public void setIceFloes(int value) {
        data[60] = value;
    }

    @TalentDataAttribute(index = 61, name = "Ice Shards", maxPoints = 3, tree = 2, column = 1, row = 2, prerequisite = -1)
    public int getIceShards() {
        return data[61];
    }

    public void setIceShards(int value) {
        data[61] = value;
    }

    @TalentDataAttribute(index = 62, name = "Frost Warding", maxPoints = 2, tree = 2, column = 2, row = 2, prerequisite = -1)
    public int getFrostWarding() {
        return data[62];
    }

    public void setFrostWarding(int value) {
        data[62] = value;
    }

    @TalentDataAttribute(index = 63, name = "Precision", maxPoints = 3, tree = 2, column = 3, row = 2, prerequisite = -1)
    public int getPrecision() {
        return data[63];
    }

    public void setPrecision(int value) {
        data[63] = value;
    }

    @TalentDataAttribute(index = 64, name = "Permafrost", maxPoints = 3, tree = 2, column = 4, row = 2, prerequisite = -1)
    public int getPermafrost() {
        return data[64];
    }

    public void setPermafrost(int value) {
        data[64] = value;
    }

    @TalentDataAttribute(index = 65, name = "Piercing Ice", maxPoints = 3, tree = 2, column = 1, row = 3, prerequisite = -1)
    public int getPiercingIce() {
        return data[65];
    }

    public void setPiercingIce(int value) {
        data[65] = value;
    }

    @TalentDataAttribute(index = 66, name = "Icy Veins", maxPoints = 1, tree = 2, column = 2, row = 3, prerequisite = -1)
    public int getIcyVeins() {
        return data[66];
    }

    public void setIcyVeins(int value) {
        data[66] = value;
    }

    @TalentDataAttribute(index = 67, name = "Improved Blizzard", maxPoints = 3, tree = 2, column = 3, row = 3, prerequisite = -1)
    public int getImprovedBlizzard() {
        return data[67];
    }

    public void setImprovedBlizzard(int value) {
        data[67] = value;
    }

    @TalentDataAttribute(index = 68, name = "Arctic Reach", maxPoints = 2, tree = 2, column = 1, row = 4, prerequisite = -1)
    public int getArcticReach() {
        return data[68];
    }

    public void setArcticReach(int value) {
        data[68] = value;
    }

    @TalentDataAttribute(index = 69, name = "Frost Channeling", maxPoints = 3, tree = 2, column = 2, row = 4, prerequisite = -1)
    public int getFrostChanneling() {
        return data[69];
    }

    public void setFrostChanneling(int value) {
        data[69] = value;
    }

    @TalentDataAttribute(index = 70, name = "Shatter", maxPoints = 3, tree = 2, column = 3, row = 4, prerequisite = -1)
    public int getShatter() {
        return data[70];
    }

    public void setShatter(int value) {
        data[70] = value;
    }

    @TalentDataAttribute(index = 71, name = "Cold Snap", maxPoints = 1, tree = 2, column = 2, row = 5, prerequisite = -1)
    public int getColdSnap() {
        return data[71];
    }

    public void setColdSnap(int value) {
        data[71] = value;
    }

    @TalentDataAttribute(index = 72, name = "Improved Cone of Cold", maxPoints = 3, tree = 2, column = 3, row = 5, prerequisite = -1)
    public int getImprovedConeOfCold() {
        return data[72];
    }

    public void setImprovedConeOfCold(int value) {
        data[72] = value;
    }

    @TalentDataAttribute(index = 73, name = "Frozen Core", maxPoints = 3, tree = 2, column = 4, row = 5, prerequisite = -1)
    public int getFrozenCore() {
        return data[73];
    }

    public void setFrozenCore(int value) {
        data[73] = value;
    }

    @TalentDataAttribute(index = 74, name = "Cold as Ice", maxPoints = 2, tree = 2, column = 1, row = 6, prerequisite = 71)
    public int getColdAsIce() {
        return data[74];
    }

    public void setColdAsIce(int value) {
        data[74] = value;
    }

    @TalentDataAttribute(index = 75, name = "Winter's Chill", maxPoints = 3, tree = 2, column = 3, row = 6, prerequisite = -1)
    public int getWintersChill() {
        return data[75];
    }

    public void setWintersChill(int value) {
        data[75] = value;
    }

    @TalentDataAttribute(index = 76, name = "Shattered Barrier", maxPoints = 2, tree = 2, column = 1, row = 7, prerequisite = 77)
    public int getShatteredBarrier() {
        return data[76];
    }

    public void setShatteredBarrier(int value) {
        data[76] = value;
    }

    @TalentDataAttribute(index = 77, name = "Ice Barrier", maxPoints = 1, tree = 2, column = 2, row = 7, prerequisite = 71)
    public int getIceBarrier() {
        return data[77];
    }

    public void setIceBarrier(int value) {
        data[77] = value;
    }

    @TalentDataAttribute(index = 78, name = "Arctic Winds", maxPoints = 5, tree = 2, column = 3, row = 7, prerequisite = -1)
    public int getArcticWinds() {
        return data[78];
    }

    public void setArcticWinds(int value) {
        data[78] = value;
    }

    @TalentDataAttribute(index = 79, name = "Empowered Frostbolt", maxPoints = 2, tree = 2, column = 2, row = 8, prerequisite = -1)
    public int getEmpoweredFrostbolt() {
        return data[79];
    }

    public void setEmpoweredFrostbolt(int value) {
        data[79] = value;
    }

    @TalentDataAttribute(index = 80, name = "Fingers of Frost", maxPoints = 2, tree = 2, column = 3, row = 8, prerequisite = -1)
    public int getFingersOfFrost() {
        return data[80];
    }

    public void setFingersOfFrost(int value) {
        data[80] = value;
    }

    @TalentDataAttribute(index = 81, name = "Brain Freeze", maxPoints = 3, tree = 2, column = 1, row = 9, prerequisite = -1)
    public int getBrainFreeze() {
        return data[81];
    }

    public void setBrainFreeze(int value) {
        data[81] = value;
    }

    @TalentDataAttribute(index = 82, name = "Summon Water Elemental", maxPoints = 1, tree = 2, column = 2, row = 9, prerequisite = -1)
    public int getSummonWaterElemental() {
        return data[82];
    }

    public void setSummonWaterElemental(int value) {
        data[82] = value;
    }

    @TalentDataAttribute(index = 83, name = "Enduring Winter", maxPoints = 3, tree = 2, column = 3, row = 9, prerequisite = 82)
    public int getEnduringWinter() {
        return data[83];
    }

    public void setEnduringWinter(int value) {
        data[83] = value;
    }

    @TalentDataAttribute(index = 84, name = "Chilled to the Bone", maxPoints = 5, tree = 2, column = 2, row = 10, prerequisite = -1)
    public int getChilledToTheBone() {
        return data[84];
    }

    public void setChilledToTheBone(int value) {
        data[84] = value;
    }

    @TalentDataAttribute(index = 85, name = "Deep Freeze", maxPoints = 1, tree = 2, column = 2, row = 11, prerequisite = -1)
    public int getDeepFreeze() {
        return data[85];
    }

    public void setDeepFreeze(int value) {
        data[85] = value;
    }

    @GlyphDataAttribute(index = 0, name = "Glyph of Fireball", major = true)
    public boolean isGlyphOfFireball() {
        return glyphData[0];
    }

    public void setGlyphOfFireball(boolean value) {
        glyphData[0] = value;
    }

    @GlyphDataAttribute(index = 1, name = "Glyph of Frostfire", major = true)
    public boolean isGlyphOfFrostfire() {
        return glyphData[1];
    }

    public void setGlyphOfFrostfire(boolean value) {
        glyphData[1] = value;
    }

    @GlyphDataAttribute(index = 2, name = "Glyph of Frostbolt", major = true)
    public boolean isGlyphOfFrostbolt() {
        return glyphData[2];
    }

    public void setGlyphOfFrostbolt(boolean value) {
        glyphData[2] = value;
    }

    @GlyphDataAttribute(index = 3, name = "Glyph of Ice Armor", major = true)
    public boolean isGlyphOfIceArmor() {
        return glyphData[3];
    }

    public void setGlyphOfIceArmor(boolean value) {
        glyphData[3] = value;
    }

    @GlyphDataAttribute(index = 4, name = "Glyph of Improved Scorch", major = true)
    public boolean isGlyphOfImprovedScorch() {
        return glyphData[4];
    }

    public void setGlyphOfImprovedScorch(boolean value) {
        glyphData[4] = value;
    }

    @GlyphDataAttribute(index = 5, name = "Glyph of Mage Armor", major = true)
    public boolean isGlyphOfMageArmor() {
        return glyphData[5];
    }

    public void setGlyphOfMageArmor(boolean value) {
        glyphData[5] = value;
    }

    @GlyphDataAttribute(index = 6, name = "Glyph of Mana Gem", major = true)
    public boolean isGlyphOfManaGem() {
        return glyphData[6];
    }

    public void setGlyphOfManaGem(boolean value) {
        glyphData[6] = value;
    }

    @GlyphDataAttribute(index = 7, name = "Glyph of Molten Armor", major = true)
    public boolean isGlyphOfMoltenArmor() {
        return glyphData[7];
    }

    public void setGlyphOfMoltenArmor(boolean value) {
        glyphData[7] = value;
    }

    @GlyphDataAttribute(index = 8, name = "Glyph of Water Elemental", major = true)
    public boolean isGlyphOfWaterElemental() {
        return glyphData[8];
    }

    public void setGlyphOfWaterElemental(boolean value) {
        glyphData[8] = value;
    }

    @GlyphDataAttribute(index = 9, name = "Glyph of Arcane Explosion", major = true)
    public boolean isGlyphOfArcaneExplosion() {
        return glyphData[9];
    }

    public void setGlyphOfArcaneExplosion(boolean value) {
        glyphData[9] = value;
    }

    @GlyphDataAttribute(index = 10, name = "Glyph of Arcane Power", major = true)
    public boolean isGlyphOfArcanePower() {
        return glyphData[10];
    }

    public void setGlyphOfArcanePower(boolean value) {
        glyphData[10] = value;
    }

    @GlyphDataAttribute(index = 11, name = "Glyph of Arcane Blast", major = true)
    public boolean isGlyphOfArcaneBlast() {
        return glyphData[11];
    }

    public void setGlyphOfArcaneBlast(boolean value) {
        glyphData[11] = value;
    }

    @GlyphDataAttribute(index = 12, name = "Glyph of Arcane Missiles", major = true)
    public boolean isGlyphOfArcaneMissiles() {
        return glyphData[12];
    }

    public void setGlyphOfArcaneMissiles(boolean value) {
        glyphData[12] = value;
    }

    @GlyphDataAttribute(index = 13, name = "Glyph of Arcane Barrage", major = true)
    public boolean isGlyphOfArcaneBarrage() {
        return glyphData[13];
    }

    public void setGlyphOfArcaneBarrage(boolean value) {
        glyphData[13] = value;
    }

    @GlyphDataAttribute(index = 14, name = "Glyph of Living Bomb", major = true)
    public boolean isGlyphOfLivingBomb() {
        return glyphData[14];
    }

    public void setGlyphOfLivingBomb(boolean value) {
        glyphData[14] = value;
    }

    @GlyphDataAttribute(index = 15, name = "Glyph of Ice Lance", major = true)
    public boolean isGlyphOfIceLance() {
        return glyphData[15];
    }

    public void setGlyphOfIceLance(boolean value) {
        glyphData[15] = value;
    }

    @GlyphDataAttribute(index = 16, name = "Glyph of Mirror Image", major = true)
    public boolean isGlyphOfMirrorImage() {
        return glyphData[16];
    }

    public void setGlyphOfMirrorImage(boolean value) {
        glyphData[16] = value;
    }

    @GlyphDataAttribute(index = 17, name = "Glyph of Deep Freeze", major = true)
    public boolean isGlyphOfDeepFreeze() {
        return glyphData[17];
    }

    public void setGlyphOfDeepFreeze(boolean value) {
        glyphData[17] = value;
    }

    @GlyphDataAttribute(index = 18, name = "Glyph of Eternal Water", major = true)
    public boolean isGlyphOfEternalWater() {
        return glyphData[18];
    }

    public void setGlyphOfEternalWater(boolean value) {
        glyphData[18] = value;
    }

}
