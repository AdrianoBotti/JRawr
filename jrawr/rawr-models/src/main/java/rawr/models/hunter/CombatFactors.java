// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/CombatFactors.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.hunter;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.character.CharacterRace;
import rawr.base.character.CharacterClass;
import rawr.base.items.Item;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.talents.HunterTalents;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;

public class CombatFactors {
    public CombatFactors(Character character, Stats stats, CalculationOptionsHunter calcOpts, BossOptions bossOpts) {
        charRef = character;
        rw = character == null || character.getRanged() == null ? new Knuckles() : character.getRanged().getItem();
        talents = character == null || character.getHunterTalents() == null ? new HunterTalents() : character.getHunterTalents();
        this.calcOpts = (calcOpts == null ? new CalculationOptionsHunter() : calcOpts);
        this.bossOpts = (bossOpts == null ? new BossOptions() : bossOpts);
        statS = stats;
        invalidateCache();
    }

    private void setCValues() {
        cRwItemType = rw.getType();
        cRwItemSpeed = rw.getSpeed();
        useRW = _useRW();
        cRwRacialExpertise = getRacialExpertiseFromWeaponType(cRwItemType);
        cRwExpertise = statS.getExpertise() + StatConversion.getExpertiseFromRating(statS.getExpertiseRating()) + cRwRacialExpertise;
        cYwMiss = ywMissChance();
        cWhMiss = whMissChance();
        cRwDodge = rwDodgeChance();
        cRwParry = rwParryChance();
        cRwBlock = rwBlockChance();
        cRwWhCrit = rwWhCritChance();
        cRwYwCrit = rwYwCritChance();
        cGlance = glanceChance();
    }

    // Global Variables
    private Stats statS;
    private HunterTalents talents;
    public CalculationOptionsHunter calcOpts;
    public BossOptions bossOpts;
    public Character charRef;

    /** The character's Ranged Weapon */
    public Item rw;
    // Optimizations
    public float cYwMiss;
    public float cWhMiss;

    public ItemType cRwItemType;

    public float cRwItemSpeed;
    public float cRwRacialExpertise;
    public float cRwExpertise;
    public float cRwDodge;
    public float cRwParry;
    public float cRwBlock;
    public float cRwWhCrit;
    public float cRwYwCrit;
    public float cGlance;

    public boolean useRW;

    private boolean _useRW() {
        return rw != null && cRwItemSpeed > 0;
    }

    public void invalidateCache() {
        this.damageBonus = this.damageReduction = this.bonusWhiteCritDmg = this.rwSpeed = this.totalHaste = -1f;
        setCValues();
    }

    // Weapon Damage Calcs
    private float damageBonus = -1f;

    public float getDamageBonus() {
        if (this.damageBonus == -1f) {
            this.damageBonus = (1f + statS.getBonusDamageMultiplier())
                    * (1f + statS.getBonusPhysicalDamageMultiplier());
        }
        return this.damageBonus;
    }

    private float damageReduction = -1f;

    public float getDamageReduction() {
        if (this.damageReduction == -1f) {
            float arpenBuffs = 0.0f;
            this.damageReduction = Math.max(0f, 1f - StatConversion.getArmorDamageReduction(charRef.getLevel(), calcOpts.targetArmor, statS.getArmorPenetration(), arpenBuffs, statS.getArmorPenetrationRating()));
        }
        return this.damageReduction;
    }

    public float getHealthBonus() {
        return 1f + statS.getBonusHealingReceived();
    }

    public float getNormalizedRwWeaponDmg() {
        return useRW ? calcNormalizedWeaponDamage(rw) : 0f;
    }

    private float calcNormalizedWeaponDamage(Item weapon) {
        return weapon.getSpeed() * weapon.getDPS() + statS.getAttackPower() / 14f * 2.8f + statS.getWeaponDamage();
    }

    public float getAvgRwWeaponDmgUnhasted() {
        return (useRW ? (statS.getAttackPower() / 14f + rw.getDPS()) * cRwItemSpeed + statS.getWeaponDamage() : 0f);
    }

    private float bonusWhiteCritDmg = -1f;

    public float getBonusWhiteCritDmg() {
        if (this.bonusWhiteCritDmg == -1f) {
            this.bonusWhiteCritDmg = (2f * (1f + statS.getBonusCritMultiplier()) - 1f);
        }
        return this.bonusWhiteCritDmg;
    }

    public float getBonusYellowCritDmg() {
        return getBonusWhiteCritDmg();
    }

    public float getReducWhBlockedDmg() {
        return 1.00f; // 100% damage, we don't get blocked
    }

    public float getReducYwBlockedDmg() {
        return getReducWhBlockedDmg();
    }

    public float getReducWhGlancedDmg() {
        return 1.00f; // 100% damage, we don't get glanced
    }

    private float totalHaste = -1f;

    public float getTotalHaste() {
        if (this.totalHaste == -1f) {
            this.totalHaste = 1f + statS.getPhysicalHaste(); // All haste is calc'd into PhysicalHaste in GetCharacterStats
        }
        return this.totalHaste;
    }

    private float rwSpeed = -1f;

    public float getRWSpeed() {
        if (this.rwSpeed == -1f) {
            this.rwSpeed = useRW ? cRwItemSpeed / getTotalHaste() : 0f;
        }
        return this.rwSpeed;
    }

    public float getHitPerc() {
        return StatConversion.getHitFromRating(statS.getHitRating(), CharacterClass.Hunter);
    }

    private float getRacialExpertiseFromWeaponType(ItemType weapon) {
        CharacterRace r = charRef.getRace();
        if (weapon != ItemType.None) {
            if (r == CharacterRace.Human) {
                if (weapon == ItemType.OneHandSword || weapon == ItemType.OneHandMace
                        || weapon == ItemType.TwoHandSword || weapon == ItemType.TwoHandMace) {
                    return 3f;
                }
            }
        }
        return 0f;
    }

    private float missPrevBonuses() {
        return statS.getPhysicalHit()    // Hit Perc bonuses like Draenei Racial
                + getHitPerc();          // Bonus from Hit Rating
    }

    private float whMissCap() {
        return StatConversion.WHITE_MISS_CHANCE_CAP[calcOpts.targetLevel - charRef.getLevel()];
    }

    private float ywMissCap() {
        return StatConversion.YELLOW_MISS_CHANCE_CAP[calcOpts.targetLevel - charRef.getLevel()];
    }

    private float whMissChance() {
        return Math.max(0f, whMissCap() - missPrevBonuses());
    }

    private float ywMissChance() {
        return Math.max(0f, ywMissCap() - missPrevBonuses());
    }

    private float dodgeChanceCap() {
        return 0f;
    }

    private float rwDodgeChance() {
        return Math.max(0f, dodgeChanceCap() - StatConversion.getDodgeParryReducFromExpertise(cRwExpertise, CharacterClass.Hunter));
    }

    private float parryChanceCap() {
        return 0f;
    }

    private float rwParryChance() {
        return 0f;
    }

    private float glanceChance() {
        return 0f;
    }

    private float blockChanceCap() {
        return 0f;
    }

    private float rwBlockChance() {
        return 0f;
    }

    private float rwWhCritChance() {
        if (!useRW) {
            return 0f;
        }
        return statS.getPhysicalCrit() + StatConversion.getCritFromRating(statS.getCritRating());
    }

    private float rwYwCritChance() {
        if (!useRW) {
            return 0f;
        }
        return ((statS.getPhysicalCrit() + StatConversion.getCritFromRating(statS.getCritRating())))
                * (1f - cYwMiss - cRwDodge);
    }

    private class Knuckles extends Item {
        public Knuckles() {
            setSpeed(0f);
            setMaxDamage(0);
            setMinDamage(0);
            setSlot(ItemSlot.Ranged);
        }
    }
}
