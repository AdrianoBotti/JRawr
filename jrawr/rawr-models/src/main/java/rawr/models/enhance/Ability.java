// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Enhance/Ability.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.enhance;

class Ability implements Comparable<Ability> {
    private final EnhanceAbility abilityType;
    private final String name;
    private final float duration;
    private int priority;
    private float cooldownOver;
    private float uses;
    private final float manacost;
    private final float gcd;
    private final float timedrift = 0.1f;
    private final boolean manaRegen;

    public Ability(EnhanceAbility abilityType, float duration, float gcd, float manacost, int priority, boolean useBeforeCombat, boolean manaRegen) {
        this.abilityType = abilityType;
        this.name = abilityType.name();
        this.duration = duration;
        this.priority = priority;
        this.manacost = manacost;
        this.gcd = gcd;
        this.manaRegen = manaRegen;
        // TODO initial cooldown on SR is when you are almost out of mana for now use its duration ie: 60 seconds
        if (useBeforeCombat)  // if ability is to be used before start of combat refresh is after first duration over (eg: totems)
            cooldownOver = duration;
        else
            cooldownOver = 0f;
        uses = 0;
    }

    public EnhanceAbility getAbilityType() {
        return abilityType;
    }

    public String getName() {
        return name;
    }

    public float getDuration() {
        return duration;
    }

    public float getGcd() {
        return gcd;
    }

    public float getCooldownOver() {
        return cooldownOver;
    }

    public float getUses() {
        return uses;
    }

    public float getManaCost() {
        return manacost;
    }

    public boolean getManaRegen() {
        return manaRegen;
    }

    public int getPriority() {
        return priority;
    }

    public void use(float useTime) {
        uses++;
        cooldownOver = useTime + duration;
    }

    public void addUses(float uses) {
        this.uses += uses > 0f ? uses : 0f;
    }

    public void removePossibleClashingUses(float clashChance, float fightLength) {
        if (abilityType != EnhanceAbility.FireElemental) {
            float possibleClashes = clashChance * (float) Math.sqrt(priority - 1) * fightLength / duration;
            uses -= possibleClashes;
        }
        if (uses < 0) uses = 0;
    }

    public void averageUses(float iterations) {
        uses /= iterations;
    }

    public void deferAbility() {
        cooldownOver += gcd;
    }

    public void updateCooldown(float time) {
        cooldownOver = time;
    }

    public boolean missedCooldown(float starttime) {
        return cooldownOver < starttime;
    }

    public boolean offCooldown(float starttime) {
        return starttime >= cooldownOver;
    }

    @Override
    public int compareTo(Ability other) {
        float diff = cooldownOver - other.cooldownOver;
        if (diff < -timedrift)  // current ability is off cooldown earlier than other ability
            return -1;
        else if (diff > timedrift) // current ability is off cooldown later than other ability
            return 1;
        else  // off cooldown at same time so use highest priority ability
            return Integer.compare(priority, other.priority);
    }
}
