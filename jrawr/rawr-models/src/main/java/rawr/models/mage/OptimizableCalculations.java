// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/CharacterCalculationsMage.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

public class OptimizableCalculations {
    public float chanceToDie;
    public float health;
    public float natureResistance;
    public float fireResistance;
    public float frostResistance;
    public float shadowResistance;
    public float arcaneResistance;
    public float resilience;
    public float hitRating;
    public float hasteRating;
    public float pVPTrinket;
    public float movementSpeed;

    public float getChanceToDie() {
        return chanceToDie;
    }

    public void setChanceToDie(float value) {
        chanceToDie = value;
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float value) {
        health = value;
    }

    public float getNatureResistance() {
        return natureResistance;
    }

    public void setNatureResistance(float value) {
        natureResistance = value;
    }

    public float getFireResistance() {
        return fireResistance;
    }

    public void setFireResistance(float value) {
        fireResistance = value;
    }

    public float getFrostResistance() {
        return frostResistance;
    }

    public void setFrostResistance(float value) {
        frostResistance = value;
    }

    public float getShadowResistance() {
        return shadowResistance;
    }

    public void setShadowResistance(float value) {
        shadowResistance = value;
    }

    public float getArcaneResistance() {
        return arcaneResistance;
    }

    public void setArcaneResistance(float value) {
        arcaneResistance = value;
    }

    public float getResilience() {
        return resilience;
    }

    public void setResilience(float value) {
        resilience = value;
    }

    public float getHitRating() {
        return hitRating;
    }

    public void setHitRating(float value) {
        hitRating = value;
    }

    public float getHasteRating() {
        return hasteRating;
    }

    public void setHasteRating(float value) {
        hasteRating = value;
    }

    public float getPVPTrinket() {
        return pVPTrinket;
    }

    public void setPVPTrinket(float value) {
        pVPTrinket = value;
    }

    public float getMovementSpeed() {
        return movementSpeed;
    }

    public void setMovementSpeed(float value) {
        movementSpeed = value;
    }
}
