// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSDK/Rotation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.dpsdk;

import rawr.base.character.Character;
import rawr.base.talents.DeathKnightTalents;

public class Rotation {
    public Type curRotationType = Type.Custom;

    private CalculationOptionsDPSDK.Presence presence = CalculationOptionsDPSDK.Presence.Blood;

    public CalculationOptionsDPSDK.Presence getPresence() {
        return presence;
    }

    public void setPresence(CalculationOptionsDPSDK.Presence value) {
        presence = value;
    }

    private int presenceByIndex = 0;

    public int getPresenceByIndex() {
        return presenceByIndex;
    }

    public void setPresenceByIndex(int value) {
        presenceByIndex = value;
        if (presenceByIndex == 0) presence = CalculationOptionsDPSDK.Presence.Blood;
        if (presenceByIndex == 1) presence = CalculationOptionsDPSDK.Presence.Unholy;
        if (presenceByIndex == 2) presence = CalculationOptionsDPSDK.Presence.Frost;
    }

    private double curRotationDuration = 0f; // rotation duration in seconds

    public double getCurRotationDuration() {
        return curRotationDuration;
    }

    public void setCurRotationDuration(double value) {
        curRotationDuration = value;
    }

    // disease info
    private double avgDiseaseMult = 0f;

    public double getAvgDiseaseMult() {
        return avgDiseaseMult;
    }

    public void setAvgDiseaseMult(double value) {
        avgDiseaseMult = value;
    }

    private double numDisease = 0f;

    public double getNumDisease() {
        return numDisease;
    }

    public void setNumDisease(double value) {
        numDisease = value;
    }

    private double diseaseUptime = 0f;

    public double getDiseaseUptime() {
        return diseaseUptime;
    }

    public void setDiseaseUptime(double value) {
        diseaseUptime = value;
    }

    private double gargoyleDuration = 0f;

    public double getGargoyleDuration() {
        return gargoyleDuration;
    }

    public void setGargoyleDuration(double value) {
        gargoyleDuration = value;
    }

    private double deathCoil = 0f;

    public double getDeathCoil() {
        return deathCoil;
    }

    public void setDeathCoil(double value) {
        deathCoil = value;
    }

    private double icyTouch = 0f;

    public double getIcyTouch() {
        return icyTouch;
    }

    public void setIcyTouch(double value) {
        icyTouch = value;
    }

    private double plagueStrike = 0f;

    public double getPlagueStrike() {
        return plagueStrike;
    }

    public void setPlagueStrike(double value) {
        plagueStrike = value;
    }

    private double frostFever = 0f;

    public double getFrostFever() {
        return frostFever;
    }

    public void setFrostFever(double value) {
        frostFever = value;
    }

    private double bloodPlague = 0f;

    public double getBloodPlague() {
        return bloodPlague;
    }

    public void setBloodPlague(double value) {
        bloodPlague = value;
    }

    private double scourgeStrike = 0f;

    public double getScourgeStrike() {
        return scourgeStrike;
    }

    public void setScourgeStrike(double value) {
        scourgeStrike = value;
    }

    private double frostStrike = 0f;

    public double getFrostStrike() {
        return frostStrike;
    }

    public void setFrostStrike(double value) {
        frostStrike = value;
    }

    private double howlingBlast = 0f;

    public double getHowlingBlast() {
        return howlingBlast;
    }

    public void setHowlingBlast(double value) {
        howlingBlast = value;
    }

    private double obliterate = 0f;

    public double getObliterate() {
        return obliterate;
    }

    public void setObliterate(double value) {
        obliterate = value;
    }

    private double deathStrike = 0f;

    public double getDeathStrike() {
        return deathStrike;
    }

    public void setDeathStrike(double value) {
        deathStrike = value;
    }

    private double bloodStrike = 0f;

    public double getBloodStrike() {
        return bloodStrike;
    }

    public void setBloodStrike(double value) {
        bloodStrike = value;
    }

    private double bloodBoil = 0f;

    private double kmfs;

    public double getKMFS() {
        return kmfs;
    }

    public void setKMFS(double value) {
        kmfs = value;
    }

    private double kmRime;

    public double getKMRime() {
        return kmRime;
    }

    public void setKMRime(double value) {
        kmRime = value;
    }

    private double ffTick;

    public double getFFTick() {
        return ffTick;
    }

    public void setFFTick(double value) {
        ffTick = value;
    }

    private double bpTick;

    public double getBPTick() {
        return bpTick;
    }

    public void setBPTick(double value) {
        bpTick = value;
    }

    public double getBloodBoil() {
        return bloodBoil;
    }

    public void setBloodBoil(double value) {
        bloodBoil = value;
    }

    private double heartStrike = 0f;

    public double getHeartStrike() {
        return heartStrike;
    }

    public void setHeartStrike(double value) {
        heartStrike = value;
    }

    private double dancingRuneWeapon = 0f;

    public double getDancingRuneWeapon() {
        return dancingRuneWeapon;
    }

    public void setDancingRuneWeapon(double value) {
        dancingRuneWeapon = value;
    }

    private double horn = 0f;

    public double getHorn() {
        return horn;
    }

    public void setHorn(double value) {
        horn = value;
    }

    private double ghoulFrenzy = 0f;

    public double getGhoulFrenzy() {
        return ghoulFrenzy;
    }

    public void setGhoulFrenzy(double value) {
        ghoulFrenzy = value;
    }

    private double pestilence = 0f;

    public double getPestilence() {
        return pestilence;
    }

    public void setPestilence(double value) {
        pestilence = value;
    }

    private boolean managedRP = false;

    public boolean getManagedRP() {
        return managedRP;
    }

    public void setManagedRP(boolean value) {
        managedRP = value;
    }

    private boolean ptrCalcs = false;

    public boolean getPTRCalcs() {
        return ptrCalcs;
    }

    public void setPTRCalcs(boolean value) {
        ptrCalcs = value;
    }

    private double gcdTime;

    public double getGcdTime() {
        return gcdTime;
    }

    public void setGcdTime(double value) {
        gcdTime = value;
    }

    private double rp;

    public double getRP() {
        return rp;
    }

    public void setRP(double value) {
        rp = value;
    }

    private double weight;

    public double getWeight() {
        return weight;
    }

    public void setWeight(double value) {
        weight = value;
    }

    public void applyWeight(double weight) {
        this.setWeight(weight);
        this.setAvgDiseaseMult(avgDiseaseMult * weight);
        this.setBloodBoil(bloodBoil * weight);
        this.setBloodPlague(bloodPlague * weight);
        this.setBloodStrike(bloodStrike * weight);
        this.setBPTick(bpTick * weight);
        this.setDeathCoil(deathCoil * weight);
        this.setDeathStrike(deathStrike * weight);
        this.setFFTick(ffTick * weight);
        this.setFrostFever(frostFever * weight);
        this.setFrostStrike(frostStrike * weight);
        this.setGhoulFrenzy(ghoulFrenzy * weight);
        this.setHeartStrike(heartStrike * weight);
        this.setHorn(horn * weight);
        this.setHowlingBlast(howlingBlast * weight);
        this.setIcyTouch(icyTouch * weight);
        this.setKMFS(kmfs * weight);
        this.setKMRime(kmRime * weight);
        this.setObliterate(obliterate * weight);
        this.setPestilence(pestilence * weight);
        this.setPlagueStrike(plagueStrike * weight);
        this.setScourgeStrike(scourgeStrike * weight);
        this.setNumDisease(numDisease * weight);
    }

    public void addRotation(Rotation other) {
        this.setWeight(weight + other.getWeight());
        this.setAvgDiseaseMult(avgDiseaseMult + other.getAvgDiseaseMult());
        this.setBloodBoil(bloodBoil + other.getBloodBoil());
        this.setBloodPlague(bloodPlague + other.getBloodPlague());
        this.setBloodStrike(bloodStrike + other.getBloodStrike());
        this.setBPTick(bpTick + other.getBPTick());
        this.setDancingRuneWeapon(dancingRuneWeapon + other.getDancingRuneWeapon());
        this.setDeathCoil(deathCoil + other.getDeathCoil());
        this.setDeathStrike(deathStrike + other.getDeathStrike());
        this.setFFTick(ffTick + other.getFFTick());
        this.setFrostFever(frostFever + other.getFrostFever());
        this.setFrostStrike(frostStrike + other.getFrostStrike());
        this.setGhoulFrenzy(ghoulFrenzy + other.getGhoulFrenzy());
        this.setHeartStrike(heartStrike + other.getHeartStrike());
        this.setHorn(horn + other.getHorn());
        this.setHowlingBlast(howlingBlast + other.getHowlingBlast());
        this.setIcyTouch(icyTouch + other.getIcyTouch());
        this.setKMFS(kmfs + other.getKMFS());
        this.setKMRime(kmRime + other.getKMRime());
        this.setNumDisease(numDisease + other.getNumDisease());
        this.setObliterate(obliterate + other.getObliterate());
        this.setPestilence(pestilence + other.getPestilence());
        this.setPlagueStrike(plagueStrike + other.getPlagueStrike());
        this.setScourgeStrike(scourgeStrike + other.getScourgeStrike());
    }

    public void copyRotation(Rotation other) {
        this.setWeight(other.getWeight());
        this.setCurRotationDuration(other.getCurRotationDuration());
        this.setAvgDiseaseMult(other.getAvgDiseaseMult());
        this.setBloodBoil(other.getBloodBoil());
        this.setBloodPlague(other.getBloodPlague());
        this.setBloodStrike(other.getBloodStrike());
        this.setBPTick(other.getBPTick());
        this.setDancingRuneWeapon(other.getDancingRuneWeapon());
        this.setDeathCoil(other.getDeathCoil());
        this.setDeathStrike(other.getDeathStrike());
        this.setFFTick(other.getFFTick());
        this.setFrostFever(other.getFrostFever());
        this.setFrostStrike(other.getFrostStrike());
        this.setGhoulFrenzy(other.getGhoulFrenzy());
        this.setHeartStrike(other.getHeartStrike());
        this.setHorn(other.getHorn());
        this.setHowlingBlast(other.getHowlingBlast());
        this.setIcyTouch(other.getIcyTouch());
        this.setKMFS(other.getKMFS());
        this.setKMRime(other.getKMRime());
        this.setNumDisease(other.getNumDisease());
        this.setObliterate(other.getObliterate());
        this.setPestilence(other.getPestilence());
        this.setPlagueStrike(other.getPlagueStrike());
        this.setScourgeStrike(other.getScourgeStrike());
        this.setPresence(other.getPresence());
        this.setNumDisease(other.getNumDisease());
    }

    public enum Type {
        Custom, Blood, Frost, Unholy, Unknown,
    }

    public Rotation() {
        setRotation(Type.Unholy);
    }

    public Type getRotationType(DeathKnightTalents t) {
        curRotationType = Type.Custom;
        final int indexBlood = 0; // start index of Blood Talents.
        final int indexFrost = 28; // start index of Frost Talents.
        final int indexUnholy = indexFrost + 29; // start index of Unholy Talents.
        int[] talentCounter = new int[4];
        int index = indexBlood;
        for (int i : t.getData()) {
            if (i > 0) {
                // Blood
                if (index < indexFrost) talentCounter[Type.Blood.ordinal()]++;
                // Frost
                else if ((indexFrost <= index) && (index < indexUnholy)) talentCounter[Type.Frost.ordinal()]++;
                // Unholy
                else if (index >= indexUnholy) talentCounter[Type.Unholy.ordinal()]++;
            }
            index++;
        }
        if ((talentCounter[Type.Blood.ordinal()] > talentCounter[Type.Frost.ordinal()]) && (talentCounter[Type.Blood.ordinal()] > talentCounter[Type.Unholy.ordinal()])) {
            // Blood
            curRotationType = Type.Blood;
        } else if ((talentCounter[Type.Frost.ordinal()] > talentCounter[Type.Blood.ordinal()]) && (talentCounter[Type.Frost.ordinal()] > talentCounter[Type.Unholy.ordinal()])) {
            // Frost
            curRotationType = Type.Frost;
        } else if ((talentCounter[Type.Unholy.ordinal()] > talentCounter[Type.Frost.ordinal()]) && (talentCounter[Type.Unholy.ordinal()] > talentCounter[Type.Blood.ordinal()])) {
            // Unholy
            curRotationType = Type.Unholy;
        }
        return curRotationType;
    }

    public float getMeleeSpecialsPerSecond() {
        double temp;
        temp = plagueStrike + scourgeStrike + frostStrike + obliterate + deathStrike + bloodStrike + heartStrike;
        temp = temp / curRotationDuration;
        return (float) temp;
    }

    public float getSpellSpecialsPerSecond() {
        float temp;
        temp = (float) (deathCoil + icyTouch + howlingBlast);
        temp = temp / (float) curRotationDuration;
        return temp;
    }

    public float getRP(DeathKnightTalents talents, Character character) {
        boolean fourT7 = character.activeBuffsContains("Scourgeborne Battlegear 4 Piece Bonus");
        boolean glyphofIT = talents.isGlyphofIcyTouch();
        boolean glyphofFS = talents.isGlyphofFrostStrike();

        setRP(((15 + (fourT7 ? 5 : 0) + 2.5f * talents.getChillOfTheGrave()) * (obliterate))
                + ((15 + (fourT7 ? 5 : 0) + 2.5f * talents.getDirge()) * (scourgeStrike))
                + ((15 + (fourT7 ? 5 : 0) + 2.5f * talents.getDirge()) * (deathStrike))
                + ((10 + 2.5f * talents.getDirge()) * (plagueStrike))
                + (10 * (bloodStrike + heartStrike + pestilence + ghoulFrenzy))
                + ((10 + 2.5f * talents.getChillOfTheGrave()) * (icyTouch))
                + ((15 + 2.5f * talents.getChillOfTheGrave()) * howlingBlast)
                + (10 * horn)
                + ((curRotationDuration / 5f) * talents.getButchery()));

        if (managedRP) {
            setRP(manageRPDumping(talents, (float) rp));
        } else {
            setRP(rp - ((40 * deathCoil) + ((glyphofFS ? 32 : 40) * frostStrike)));
        }
        return (float) rp;
    }

    public float manageRPDumping(DeathKnightTalents talents, float RP) {
        if (talents.getDancingRuneWeapon() > 0f) {
            RP -= (float) curRotationDuration * (60f / 90f);
        }
        if (talents.getSummonGargoyle() > 0f) {
            RP -= (float) (curRotationDuration * (60f / 180f));
        }
        if (talents.getFrostStrike() > 0f) {
            setFrostStrike(RP / (talents.isGlyphofFrostStrike() ? 32f : 40f));
            setDeathCoil(0f);
            RP = 0f;
        } else {
            setDeathCoil(RP / 40f);
            setFrostStrike(0f);
            RP = 0f;
        }
        return RP;
    }

    public float getGCDTime() {
        if (presence == CalculationOptionsDPSDK.Presence.Unholy) {
            gcdTime = deathCoil + icyTouch + plagueStrike + scourgeStrike
                    + frostStrike + howlingBlast + obliterate + deathStrike + bloodStrike
                    + heartStrike + horn + pestilence + ghoulFrenzy;
        } else if (presence == CalculationOptionsDPSDK.Presence.Blood) {
            gcdTime = 1.5f * (plagueStrike + scourgeStrike + frostStrike + obliterate + deathStrike
                    + bloodStrike + heartStrike);
            gcdTime += 1.5f * (deathCoil + icyTouch + howlingBlast + horn + pestilence + ghoulFrenzy);
            // this does not currently account for haste, and I don't think it is possible in the current design.
        }
        return (float) gcdTime;
    }

    public void setRotation(Type t) {
        curRotationType = t;
        switch (curRotationType) {
            case Blood:
                setNumDisease(2f);
                setDiseaseUptime(100f);
                setDeathCoil(3f);
                setIcyTouch(1f);
                setPlagueStrike(1f);
                setScourgeStrike(0f);
                setManagedRP(true);
                setHorn(0f);
                setFrostStrike(0f);
                setHowlingBlast(0f);
                setObliterate(0f);
                setDeathStrike(2f);
                setBloodStrike(0f);
                setHeartStrike(6f);
                setCurRotationDuration(360f);
                setDancingRuneWeapon(90f);
                setGargoyleDuration(0f);
                setDeathStrike(2f);
                setGhoulFrenzy(0f);
                setPestilence(0f);
                setPresence(CalculationOptionsDPSDK.Presence.Blood);
                break;
            case Frost:
                setNumDisease(2f);
                setDiseaseUptime(100f);
                setDeathCoil(0f);
                setIcyTouch(1f);
                setPlagueStrike(1f);
                setScourgeStrike(0f);
                setManagedRP(true);
                setHorn(0f);
                setFrostStrike(3f);
                setHowlingBlast(0f);
                setObliterate(4f);
                setBloodStrike(2f);
                setHeartStrike(0f);
                setDancingRuneWeapon(0f);
                setCurRotationDuration(360f);
                setGargoyleDuration(0f);
                setDeathStrike(0f);
                setPestilence(0f);
                setGhoulFrenzy(0f);
                setPresence(CalculationOptionsDPSDK.Presence.Blood);
                break;
            case Unholy:
                setNumDisease(3f);
                setDiseaseUptime(100f);
                setDeathCoil(3.5f);
                setIcyTouch(1f);
                setPlagueStrike(1f);
                setScourgeStrike(4f);
                setManagedRP(true);
                setFrostStrike(0f);
                setHowlingBlast(0f);
                setObliterate(0f);
                setBloodStrike(2f);
                setHeartStrike(0f);
                setDancingRuneWeapon(0f);
                setCurRotationDuration(360f);
                setHorn(1f);
                setGargoyleDuration(30f);
                setDeathStrike(0f);
                setGhoulFrenzy(0f);
                setPestilence(0f);
                setPresence(CalculationOptionsDPSDK.Presence.Blood);
                break;
            case Custom:
                break;
        }
    }
}
