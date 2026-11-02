// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.DPSWarr/CombatFactors.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// C# auto-properties become public fields/methods; the lazy caches keep their -1 sentinels. The
// non-Silverlight branches are used (CalcOpts.TargetLevel / CalcOpts.TargetArmor, CalcOpts.InBack).
package rawr.models.dpswarr;

import rawr.base.bosses.BossOptions;
import rawr.base.character.Character;
import rawr.base.character.CharacterRace;
import rawr.base.stats.HitResult;
import rawr.base.items.Item;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.stats.WeightedStat;

public class CombatFactors {
    public CombatFactors(Character character, Stats stats, CalculationOptionsDPSWarr calcOpts, BossOptions bossOpts) {
        char_ = character;
        MH = char_ == null || char_.getMainHand() == null ? new Knuckles() : char_.getMainHand().getItem();
        OH = char_ == null || char_.getOffHand() == null || char_.getWarriorTalents().getTitansGrip() == 0 ? null : char_.getOffHand().getItem();
        talents = char_ == null || char_.getWarriorTalents() == null ? new rawr.base.talents.WarriorTalents() : char_.getWarriorTalents();
        CalcOpts = (calcOpts == null ? new CalculationOptionsDPSWarr() : calcOpts);
        BossOpts = (bossOpts == null ? new BossOptions() : bossOpts);
        StatS = stats;
        critProcs = new WeightedStat[] { new WeightedStat(1f, 0f) };
        invalidateCache();
    }

    private Boolean furyStance = null;

    public boolean furyStance() {
        if (this.furyStance == null) {
            if (talents == null) {
                return false; // wait till there is one
            }
            int armsCounter = 0, furyCounter = 0;
            String t = talents.toString();
            for (int i = 0; i <= 30; i++) {
                armsCounter += Integer.parseInt(t.substring(i, i + 1));
            }
            for (int i = 31; i <= 57; i++) {
                furyCounter += Integer.parseInt(t.substring(i, i + 1));
            }
            if (armsCounter >= furyCounter) this.furyStance = false;
            else this.furyStance = true;
        }
        return this.furyStance;
    }

    public WeightedStat[] critProcs;

    private void setCValues() {
        this.cMhItemType = MH.getType();
        this.cMhItemSpeed = MH.getSpeed();
        if (OH != null) {
            this.cOhItemType = OH.getType();
            this.cOhItemSpeed = OH.getSpeed();
        }
        useMH = _useMH();
        useOH = _useOH();

        this.cMhRacialExpertise = getRacialExpertiseFromWeaponType(this.cMhItemType);
        this.cMhExpertise = StatS.getExpertise() + StatConversion.getExpertiseFromRating(Math.max(0, StatS.getExpertiseRating())) + this.cMhRacialExpertise;
        this.cYMiss = ywMissChance();
        this.cWMiss = whMissChance();
        this.cMhDodge = mhDodgeChance();
        this.cMhParry = mhParryChance();
        this.cMhBlock = mhBlockChance();
        this.cMhWCrit = mhWhCritChance();
        this.cMhYCrit = mhYwCritChance();
        this.cGlance = glanceChance();
        if (useOH) {
            this.cOhRacialExpertise = getRacialExpertiseFromWeaponType(this.cOhItemType);
            this.cOhExpertise = StatS.getExpertise() + StatConversion.getExpertiseFromRating(Math.max(0, StatS.getExpertiseRating())) + this.cOhRacialExpertise;
            this.cOhDodge = ohDodgeChance();
            this.cOhParry = ohParryChance();
            this.cOhBlock = ohBlockChance();
            this.cOhWCrit = ohWhCritChance();
            this.cOhYCrit = ohYwCritChance();
        } else {
            this.cOhItemType = ItemType.None;
            this.cOhItemSpeed = 0f;
            this.cOhRacialExpertise = 0f;
            this.cOhExpertise = 0f;
            this.cOhDodge = StatConversion.WHITE_DODGE_CHANCE_CAP[levelDif()];
            this.cOhParry = StatConversion.WHITE_PARRY_CHANCE_CAP[levelDif()];
            this.cOhBlock = 0.0f;
            this.cOhWCrit = 0.0f;
            this.cOhYCrit = 0.0f;
        }
    }

    public Stats StatS;

    private final rawr.base.talents.WarriorTalents talents;
    public final CalculationOptionsDPSWarr CalcOpts;
    public final BossOptions BossOpts;
    public final Character char_;
    public final Item MH;
    public final Item OH;

    public float cYMiss;
    public float cWMiss;

    public ItemType cMhItemType;
    public ItemType cOhItemType;

    public float cMhItemSpeed;
    public float cOhItemSpeed;
    public float cMhRacialExpertise;
    public float cOhRacialExpertise;
    public float cMhExpertise;
    public float cOhExpertise;
    public float cMhDodge;
    public float cOhDodge;
    public float cMhParry;
    public float cOhParry;
    public float cMhBlock;
    public float cOhBlock;
    public float cMhWCrit;
    public float cOhWCrit;
    public float cMhYCrit;
    public float cOhYCrit;
    public float cGlance;

    public int levelDif() {
        return CalcOpts.getTargetLevel() - char_.getLevel();
    }

    public boolean useMH;
    public boolean useOH;

    private boolean _useMH() {
        return MH != null && this.cMhItemSpeed > 0;
    }

    private boolean _useOH() {
        return talents.getTitansGrip() > 0 && OH != null && this.cOhItemSpeed > 0;
    }

    public void invalidateCache() {
        this.damageBonus = this.damageReduction = this.bonusWhiteCritDmg = this.mhSpeed = this.ohSpeed = this.totalHaste = -1f;
        this.attackTableBasicMh = this.attackTableBasicOh = null;
        setCValues();
    }

    // Weapon Damage Calcs
    private float damageBonus = -1f;

    public float damageBonus() {
        if (this.damageBonus == -1f) {
            this.damageBonus = (1f + StatS.getBonusDamageMultiplier())
                    * (1f + StatS.getBonusPhysicalDamageMultiplier());
        }
        return this.damageBonus;
    }

    private float damageReduction = -1f;

    public float damageReduction() {
        if (this.damageReduction == -1f) {
            float arpenBuffs =
                    ((this.cMhItemType == ItemType.TwoHandMace) ? talents.getMaceSpecialization() * 0.03f : 0.00f)
                    + (!furyStance() ? (0.10f + StatS.getBonusWarrior_T9_2P_ArP()) : 0.0f);

            this.damageReduction = Math.max(0f, 1f - StatConversion.getArmorDamageReduction(char_.getLevel(),
                    CalcOpts.getTargetArmor(),
                    StatS.getArmorPenetration(), arpenBuffs, Math.max(0, StatS.getArmorPenetrationRating())));
        }
        return this.damageReduction;
    }

    public float healthBonus() {
        return 1f + StatS.getBonusHealingReceived();
    }

    public float ohDamageReduc() {
        return 0.5f * (1f + talents.getDualWieldSpecialization() * 0.05f);
    }

    public float normalizedMhWeaponDmg() {
        return useMH ? calcNormalizedWeaponDamage(MH) : 0f;
    }

    public float normalizedOhWeaponDmg() {
        return useOH ? calcNormalizedWeaponDamage(OH) * ohDamageReduc() : 0f;
    }

    private float calcNormalizedWeaponDamage(Item weapon) {
        return weapon.getSpeed() * weapon.getDPS() + StatS.getAttackPower() / 14f * 3.3f + StatS.getWeaponDamage();
    }

    public float avgMhWeaponDmgUnhasted() {
        return (useMH ? (StatS.getAttackPower() / 14f + MH.getDPS()) * this.cMhItemSpeed + StatS.getWeaponDamage() : 0f);
    }

    public float avgOhWeaponDmgUnhasted() {
        return (useOH ? (StatS.getAttackPower() / 14f + OH.getDPS()) * this.cOhItemSpeed * ohDamageReduc() + StatS.getWeaponDamage() : 0f);
    }

    private float bonusWhiteCritDmg = -1f;

    public float bonusWhiteCritDmg() {
        if (this.bonusWhiteCritDmg == -1f) {
            this.bonusWhiteCritDmg = (2f * (1f + StatS.getBonusCritMultiplier()) - 1f)
                    * (1f + ((this.cMhItemType == ItemType.TwoHandAxe || this.cMhItemType == ItemType.Polearm) ? 0.01f * talents.getPoleaxeSpecialization() : 0f));
        }
        return this.bonusWhiteCritDmg;
    }

    public float bonusYellowCritDmg() {
        return bonusWhiteCritDmg() * (1f + talents.getImpale() * 0.1f);
    }

    public float reducWhBlockedDmg() {
        return 0.70f; // 70% damage
    }

    public float reducYwBlockedDmg() {
        return reducWhBlockedDmg();
    }

    public float reducWhGlancedDmg() {
        return 0.70f;
    }

    private float totalHaste = -1f;

    public float totalHaste() {
        if (this.totalHaste == -1f) {
            this.totalHaste = 1f + StatS.getPhysicalHaste();
        }
        return this.totalHaste;
    }

    private float mhSpeed = -1f;
    private float ohSpeed = -1f;

    public float mhSpeed() {
        if (this.mhSpeed == -1f) {
            this.mhSpeed = useMH ? this.cMhItemSpeed / totalHaste() : 0f;
        }
        return this.mhSpeed;
    }

    public float ohSpeed() {
        if (this.ohSpeed == -1f) {
            this.ohSpeed = useOH ? this.cOhItemSpeed / totalHaste() : 0f;
        }
        return this.ohSpeed;
    }

    private AttackTable attackTableBasicMh, attackTableBasicOh;

    public AttackTable attackTableBasicMH() {
        if (this.attackTableBasicMh == null) {
            this.attackTableBasicMh = new AttackTable(char_, StatS, this, CalcOpts, BossOpts, Ability.NULL, true, false, false);
        }
        return this.attackTableBasicMh;
    }

    public AttackTable attackTableBasicOH() {
        if (this.attackTableBasicOh == null) {
            this.attackTableBasicOh = new AttackTable(char_, StatS, this, CalcOpts, BossOpts, Ability.NULL, false, false, false);
        }
        return this.attackTableBasicOh;
    }

    public float hitPerc() {
        return StatConversion.getHitFromRating(Math.max(0, StatS.getHitRating()), rawr.base.character.CharacterClass.Warrior);
    }

    private float getRacialExpertiseFromWeaponType(ItemType weapon) {
        CharacterRace r = char_.getRace();
        if (weapon != ItemType.None) {
            if (r == CharacterRace.Human) {
                if (weapon == ItemType.OneHandSword || weapon == ItemType.OneHandMace
                        || weapon == ItemType.TwoHandSword || weapon == ItemType.TwoHandMace) {
                    return 3f;
                }
            } else if (r == CharacterRace.Dwarf) {
                if (weapon == ItemType.OneHandMace || weapon == ItemType.TwoHandMace) {
                    return 5f;
                }
            } else if (r == CharacterRace.Orc) {
                if (weapon == ItemType.OneHandAxe || weapon == ItemType.TwoHandAxe) {
                    return 5f;
                }
            }
        }
        return 0f;
    }

    private float missPrevBonuses() {
        return StatS.getPhysicalHit()
                + hitPerc();
    }

    private float whMissCap() {
        return (useOH
                && MH.getSlot() == ItemSlot.TwoHand
                && OH.getSlot() == ItemSlot.TwoHand
                ? StatConversion.WHITE_MISS_CHANCE_CAP_DW[levelDif()]
                : StatConversion.WHITE_MISS_CHANCE_CAP[levelDif()]);
    }

    private float whMissChance() {
        return Math.max(0f, whMissCap() - missPrevBonuses());
    }

    private float ywMissCap() {
        return StatConversion.YELLOW_MISS_CHANCE_CAP[levelDif()];
    }

    private float ywMissChance() {
        return Math.max(0f, ywMissCap() - missPrevBonuses());
    }

    private float dodgeChanceCap() {
        return StatConversion.WHITE_DODGE_CHANCE_CAP[levelDif()];
    }

    private float mhDodgeChance() {
        return Math.max(0f, dodgeChanceCap() - StatConversion.getDodgeParryReducFromExpertise(this.cMhExpertise, rawr.base.character.CharacterClass.Warrior) - talents.getWeaponMastery() * 0.01f);
    }

    private float ohDodgeChance() {
        return Math.max(0f, dodgeChanceCap() - StatConversion.getDodgeParryReducFromExpertise(this.cOhExpertise, rawr.base.character.CharacterClass.Warrior) - talents.getWeaponMastery() * 0.01f);
    }

    private float parryChanceCap() {
        return StatConversion.WHITE_PARRY_CHANCE_CAP[levelDif()];
    }

    private float mhParryChance() {
        float parryChance = parryChanceCap() - StatConversion.getDodgeParryReducFromExpertise(this.cMhExpertise, rawr.base.character.CharacterClass.Warrior);
        return Math.max(0f, CalcOpts.getInBack() ? parryChance * (1f - CalcOpts.getInBackPerc() / 100f) : parryChance);
    }

    private float ohParryChance() {
        float parryChance = parryChanceCap() - StatConversion.getDodgeParryReducFromExpertise(this.cOhExpertise, rawr.base.character.CharacterClass.Warrior);
        return Math.max(0f, CalcOpts.getInBack() ? parryChance * (1f - CalcOpts.getInBackPerc() / 100f) : parryChance);
    }

    private float glanceChance() {
        return StatConversion.WHITE_GLANCE_CHANCE_CAP[levelDif()];
    }

    private float blockChanceCap() {
        return 0f;
    }

    private float mhBlockChance() {
        return Math.max(0f, CalcOpts.getInBack() ? blockChanceCap() * (1f - CalcOpts.getInBackPerc() / 100f) : blockChanceCap());
    }

    private float ohBlockChance() {
        return Math.max(0f, CalcOpts.getInBack() ? blockChanceCap() * (1f - CalcOpts.getInBackPerc() / 100f) : blockChanceCap());
    }

    private float mhWhCritChance() {
        if (!useMH) {
            return 0f;
        }
        return StatS.getPhysicalCrit()
                + ((this.cMhItemType == ItemType.TwoHandAxe || this.cMhItemType == ItemType.Polearm) ? 0.01f * talents.getPoleaxeSpecialization() : 0f);
    }

    private float mhYwCritChance() {
        if (!useMH) {
            return 0f;
        }
        return StatS.getPhysicalCrit()
                + ((this.cMhItemType == ItemType.TwoHandAxe || this.cMhItemType == ItemType.Polearm) ? 0.01f * talents.getPoleaxeSpecialization() : 0f);
    }

    private float ohWhCritChance() {
        if (!useOH) {
            return 0f;
        }
        return StatS.getPhysicalCrit()
                + ((this.cOhItemType == ItemType.TwoHandAxe || this.cOhItemType == ItemType.Polearm) ? 0.01f * talents.getPoleaxeSpecialization() : 0f);
    }

    private float ohYwCritChance() {
        if (!useOH) {
            return 0f;
        }
        return StatS.getPhysicalCrit()
                + ((this.cOhItemType == ItemType.TwoHandAxe || this.cOhItemType == ItemType.Polearm) ? 0.01f * talents.getPoleaxeSpecialization() : 0f);
    }

    private float levelModifier() {
        return (levelDif()) * 0.002f;
    }

    public float npcCritChance() {
        return Math.max(0f, 0.05f + levelModifier() - StatConversion.getDRAvoidanceChance(char_, StatS, HitResult.Crit,
                CalcOpts.getTargetLevel()));
    }
}

class Knuckles extends Item {
    public Knuckles() {
        setSpeed(0f);
        setMaxDamage(0);
        setMinDamage(0);
    }
}
