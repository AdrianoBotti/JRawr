// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/Solver.cs and Rawr.Mage/Solver.MIP.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// C# partial classes merged into one Java class. The C# `unsafe`/`fixed` pointer paths are ported as
// index-based double[]/int[] calls with the same arithmetic and loop order (DECISIONS #4).
package rawr.models.mage;

import rawr.base.character.Buff;
import rawr.base.character.Character;
import rawr.base.character.CharacterRace;
import rawr.base.character.CharacterSlot;
import rawr.base.items.Enchant;
import rawr.base.items.Item;
import rawr.base.items.ItemDamageType;
import rawr.base.items.ItemInstance;
import rawr.base.items.ItemType;
import rawr.base.talents.MageTalents;
import rawr.base.effects.SpecialEffect;
import rawr.base.effects.SpecialFunction;
import rawr.base.stats.StatConversion;
import rawr.base.stats.Stats;
import rawr.base.effects.Trigger;
import rawr.models.mage.statedescription.ParseTree;
import rawr.models.mage.statedescription.Parser;
import rawr.models.mage.statedescription.Scanner;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntPredicate;

public final class Solver {

    private static class StackingMemoizationEntry {
        public int order;
        public int effect2ActiveDuration;
        public int effect2ActiveCooldown;
        public int maximizeStackingDuration;
    }

    private static class ResolutionMapping {
        public int minSegment;
        public int maxSegment;
    }

    private static class ActivationConstraints {
        public int segment;
        public boolean loose;
        public double minTime;
        public double maxTime;
        public int jointEffect;
        public int leftLink;
        public int rightLink;
        public int fullLeftLink;
        public int fullRightLink;
        public double leftPaddding;
        public double rightPadding;
    }

    private static class BranchNode implements Comparable<BranchNode> {
        public SolverLP lp;
        public BranchNode parent;
        public List<BranchNode> children = new ArrayList<BranchNode>();
        public int index;
        public int depth;
        public double value;
        public double probeValue;

        public double getLpValue() {
            return this.lp != null ? this.lp.getValue() : this.value;
        }

        @Override
        public int compareTo(BranchNode other) {
            return -Double.compare(getLpValue(), other.getLpValue());
        }

        public void releaseConstraints() {
            for (BranchNode node : this.children) {
                if (node != null) node.releaseConstraints();
            }
            if (this.lp != null) this.lp.releaseConstraints();
        }
    }

    // initialized in constructor
    public Character character;
    public MageTalents mageTalents;
    public CalculationOptionsMage calculationOptions;
    private boolean segmentCooldowns;
    private boolean segmentMana;
    private int advancedConstraintsLevel;
    private boolean integralMana;
    private String armor;
    private boolean useIncrementalOptimizations;
    private boolean useGlobalOptimizations;
    public boolean needsDisplayCalculations;
    private boolean requiresMIP;
    private boolean needsSolutionVariables;
    private boolean cancellationPending;
    private boolean needsQuadratic;

    public ArraySet arraySet;

    public ArraySet getArraySet() {
        return this.arraySet;
    }

    public void setArraySet(ArraySet value) {
        this.arraySet = value;
    }

    // initialized in Initialize
    public Stats baseStats;
    public List<Buff> activeBuffs;
    private List<Buff> autoActivatedBuffs;
    private TargetDebuffStats targetDebuffs;
    private boolean restrictThreat;

    public float manaGemValue;
    public float manaPotionValue;
    public float maxManaGemValue;
    public float maxManaPotionValue;

    private boolean heroismAvailable;
    private boolean arcanePowerAvailable;
    private boolean icyVeinsAvailable;
    private boolean combustionAvailable;
    private boolean moltenFuryAvailable;
    private boolean coldsnapAvailable;
    private boolean potionOfWildMagicAvailable;
    private boolean potionOfSpeedAvailable;
    private boolean effectPotionAvailable;
    private boolean berserkingAvailable;
    private boolean flameCapAvailable;
    private boolean waterElementalAvailable;
    private boolean mirrorImageAvailable;
    private boolean manaGemEffectAvailable;
    private boolean powerInfusionAvailable;
    private boolean evocationAvailable;
    private boolean manaPotionAvailable;

    // initialized in InitializeEffectCooldowns
    private static final int standardEffectCount = 14; // can't just compute from enum, because that counts the combined masks also
    public List<EffectCooldown> cooldownList;
    public Map<Integer, EffectCooldown> effectCooldown;
    private int[] effectExclusionList;
    private int cooldownCount;
    public float manaGemEffectDuration;
    private int availableCooldownMask;

    public static final float CombustionCooldown = 120.0f;
    public static final float PowerInfusionDuration = 15.0f;
    public static final float PowerInfusionCooldown = 120.0f;
    public static final float MirrorImageDuration = 30.0f;
    public static final float MirrorImageCooldown = 180.0f;
    public float icyVeinsCooldown;
    public float coldsnapCooldown;
    public float arcanePowerCooldown;
    public float arcanePowerDuration;
    public float waterElementalCooldown;
    public float waterElementalDuration;
    public float evocationCooldown;

    public EffectCooldown[] itemBasedEffectCooldowns;
    public int itemBasedEffectCooldownsCount;
    public EffectCooldown[] stackingHasteEffectCooldowns;
    public int stackingHasteEffectCooldownsCount;
    public EffectCooldown[] stackingNonHasteEffectCooldowns;
    public int stackingNonHasteEffectCooldownsCount;

    // initialized in InitializeProcEffects
    public SpecialEffect[] spellPowerEffects;
    public int spellPowerEffectsCount;
    public SpecialEffect[] hasteRatingEffects;
    public int hasteRatingEffectsCount;
    public SpecialEffect[] damageProcEffects;
    public int damageProcEffectsCount;
    public SpecialEffect[] manaRestoreEffects;
    public int manaRestoreEffectsCount;
    public SpecialEffect[] mp5Effects;
    public int mp5EffectsCount;

    // initialized in CalculateBaseStateStats
    public float baseSpellHit;
    public float rawArcaneHitRate;
    public float rawFireHitRate;
    public float rawFrostHitRate;
    public float baseArcaneHitRate;
    public float baseFireHitRate;
    public float baseFrostHitRate;
    public float baseNatureHitRate;
    public float baseShadowHitRate;
    public float baseFrostFireHitRate;
    public float baseHolyHitRate;

    public float arcaneThreatMultiplier;
    public float fireThreatMultiplier;
    public float frostThreatMultiplier;
    public float natureThreatMultiplier;
    public float shadowThreatMultiplier;
    public float frostFireThreatMultiplier;
    public float holyThreatMultiplier;

    public float baseSpellModifier;
    public float baseArcaneSpellModifier;
    public float baseFireSpellModifier;
    public float baseFrostSpellModifier;
    public float baseNatureSpellModifier;
    public float baseShadowSpellModifier;
    public float baseFrostFireSpellModifier;
    public float baseHolySpellModifier;

    public float baseAdditiveSpellModifier;
    public float baseArcaneAdditiveSpellModifier;
    public float baseFireAdditiveSpellModifier;
    public float baseFrostAdditiveSpellModifier;
    public float baseNatureAdditiveSpellModifier;
    public float baseShadowAdditiveSpellModifier;
    public float baseFrostFireAdditiveSpellModifier;
    public float baseHolyAdditiveSpellModifier;

    public float baseCritRate;
    public float baseArcaneCritRate;
    public float baseFireCritRate;
    public float baseFrostCritRate;
    public float baseNatureCritRate;
    public float baseShadowCritRate;
    public float baseFrostFireCritRate;
    public float baseHolyCritRate;

    public float igniteFactor;

    public float baseArcaneCritBonus;
    public float baseFireCritBonus;
    public float baseFrostCritBonus;
    public float baseNatureCritBonus;
    public float baseShadowCritBonus;
    public float baseFrostFireCritBonus;
    public float baseHolyCritBonus;

    public float combustionFireCritBonus;
    public float combustionFrostFireCritBonus;

    public float baseArcaneSpellPower;
    public float baseFireSpellPower;
    public float baseFrostSpellPower;
    public float baseNatureSpellPower;
    public float baseShadowSpellPower;
    public float baseHolySpellPower;

    public float spiritRegen;
    public float manaRegen;
    public float manaRegen5SR;
    public float manaRegenDrinking;
    public float healthRegen;
    public float healthRegenCombat;
    public float healthRegenEating;
    public float meleeMitigation;
    public float defense;
    public float physicalCritReduction;
    public float spellCritReduction;
    public float critDamageReduction;
    public float damageTakenReduction;
    public float dodge;

    public float baseCastingSpeed;
    public float castingSpeedMultiplier;
    public float baseGlobalCooldown;

    public float incomingDamageAmpMelee;
    public float incomingDamageAmpPhysical;
    public float incomingDamageAmpArcane;
    public float incomingDamageAmpFire;
    public float incomingDamageAmpFrost;
    public float incomingDamageAmpNature;
    public float incomingDamageAmpShadow;
    public float incomingDamageAmpHoly;

    public float incomingDamageDpsMelee;
    public float incomingDamageDpsPhysical;
    public float incomingDamageDpsArcane;
    public float incomingDamageDpsFire;
    public float incomingDamageDpsFrost;
    public float incomingDamageDpsNature;
    public float incomingDamageDpsShadow;
    public float incomingDamageDpsHoly;

    public float incomingDamageDps;

    public int maxTalents;
    public float mastery;
    public float manaAdeptBonus;

    public float getBaseSpellHit() { return this.baseSpellHit; }
    public float getRawArcaneHitRate() { return this.rawArcaneHitRate; }
    public float getRawFireHitRate() { return this.rawFireHitRate; }
    public float getRawFrostHitRate() { return this.rawFrostHitRate; }
    public float getBaseArcaneHitRate() { return this.baseArcaneHitRate; }
    public float getBaseFireHitRate() { return this.baseFireHitRate; }
    public float getBaseFrostHitRate() { return this.baseFrostHitRate; }
    public float getBaseNatureHitRate() { return this.baseNatureHitRate; }
    public float getBaseShadowHitRate() { return this.baseShadowHitRate; }
    public float getBaseFrostFireHitRate() { return this.baseFrostFireHitRate; }
    public float getBaseHolyHitRate() { return this.baseHolyHitRate; }
    public float getArcaneThreatMultiplier() { return this.arcaneThreatMultiplier; }
    public float getFireThreatMultiplier() { return this.fireThreatMultiplier; }
    public float getFrostThreatMultiplier() { return this.frostThreatMultiplier; }
    public float getNatureThreatMultiplier() { return this.natureThreatMultiplier; }
    public float getShadowThreatMultiplier() { return this.shadowThreatMultiplier; }
    public float getFrostFireThreatMultiplier() { return this.frostFireThreatMultiplier; }
    public float getHolyThreatMultiplier() { return this.holyThreatMultiplier; }
    public float getBaseSpellModifier() { return this.baseSpellModifier; }
    public float getBaseArcaneSpellModifier() { return this.baseArcaneSpellModifier; }
    public float getBaseFireSpellModifier() { return this.baseFireSpellModifier; }
    public float getBaseFrostSpellModifier() { return this.baseFrostSpellModifier; }
    public float getBaseNatureSpellModifier() { return this.baseNatureSpellModifier; }
    public float getBaseShadowSpellModifier() { return this.baseShadowSpellModifier; }
    public float getBaseFrostFireSpellModifier() { return this.baseFrostFireSpellModifier; }
    public float getBaseHolySpellModifier() { return this.baseHolySpellModifier; }
    public float getBaseAdditiveSpellModifier() { return this.baseAdditiveSpellModifier; }
    public float getBaseArcaneAdditiveSpellModifier() { return this.baseArcaneAdditiveSpellModifier; }
    public float getBaseFireAdditiveSpellModifier() { return this.baseFireAdditiveSpellModifier; }
    public float getBaseFrostAdditiveSpellModifier() { return this.baseFrostAdditiveSpellModifier; }
    public float getBaseNatureAdditiveSpellModifier() { return this.baseNatureAdditiveSpellModifier; }
    public float getBaseShadowAdditiveSpellModifier() { return this.baseShadowAdditiveSpellModifier; }
    public float getBaseFrostFireAdditiveSpellModifier() { return this.baseFrostFireAdditiveSpellModifier; }
    public float getBaseHolyAdditiveSpellModifier() { return this.baseHolyAdditiveSpellModifier; }
    public float getBaseCritRate() { return this.baseCritRate; }
    public float getBaseArcaneCritRate() { return this.baseArcaneCritRate; }
    public float getBaseFireCritRate() { return this.baseFireCritRate; }
    public float getBaseFrostCritRate() { return this.baseFrostCritRate; }
    public float getBaseNatureCritRate() { return this.baseNatureCritRate; }
    public float getBaseShadowCritRate() { return this.baseShadowCritRate; }
    public float getBaseFrostFireCritRate() { return this.baseFrostFireCritRate; }
    public float getBaseHolyCritRate() { return this.baseHolyCritRate; }
    public float getIgniteFactor() { return this.igniteFactor; }
    public float getBaseArcaneCritBonus() { return this.baseArcaneCritBonus; }
    public float getBaseFireCritBonus() { return this.baseFireCritBonus; }
    public float getBaseFrostCritBonus() { return this.baseFrostCritBonus; }
    public float getBaseNatureCritBonus() { return this.baseNatureCritBonus; }
    public float getBaseShadowCritBonus() { return this.baseShadowCritBonus; }
    public float getBaseFrostFireCritBonus() { return this.baseFrostFireCritBonus; }
    public float getBaseHolyCritBonus() { return this.baseHolyCritBonus; }
    public float getCombustionFireCritBonus() { return this.combustionFireCritBonus; }
    public float getCombustionFrostFireCritBonus() { return this.combustionFrostFireCritBonus; }
    public float getBaseArcaneSpellPower() { return this.baseArcaneSpellPower; }
    public float getBaseFireSpellPower() { return this.baseFireSpellPower; }
    public float getBaseFrostSpellPower() { return this.baseFrostSpellPower; }
    public float getBaseNatureSpellPower() { return this.baseNatureSpellPower; }
    public float getBaseShadowSpellPower() { return this.baseShadowSpellPower; }
    public float getBaseHolySpellPower() { return this.baseHolySpellPower; }
    public float getSpiritRegen() { return this.spiritRegen; }
    public float getManaRegen() { return this.manaRegen; }
    public float getManaRegen5SR() { return this.manaRegen5SR; }
    public float getManaRegenDrinking() { return this.manaRegenDrinking; }
    public float getHealthRegen() { return this.healthRegen; }
    public float getHealthRegenCombat() { return this.healthRegenCombat; }
    public float getHealthRegenEating() { return this.healthRegenEating; }
    public float getMeleeMitigation() { return this.meleeMitigation; }
    public float getDefense() { return this.defense; }
    public float getPhysicalCritReduction() { return this.physicalCritReduction; }
    public float getSpellCritReduction() { return this.spellCritReduction; }
    public float getCritDamageReduction() { return this.critDamageReduction; }
    public float getDamageTakenReduction() { return this.damageTakenReduction; }
    public float getDodge() { return this.dodge; }
    public float getBaseCastingSpeed() { return this.baseCastingSpeed; }
    public float getCastingSpeedMultiplier() { return this.castingSpeedMultiplier; }
    public float getBaseGlobalCooldown() { return this.baseGlobalCooldown; }
    public float getIncomingDamageAmpMelee() { return this.incomingDamageAmpMelee; }
    public float getIncomingDamageAmpPhysical() { return this.incomingDamageAmpPhysical; }
    public float getIncomingDamageAmpArcane() { return this.incomingDamageAmpArcane; }
    public float getIncomingDamageAmpFire() { return this.incomingDamageAmpFire; }
    public float getIncomingDamageAmpFrost() { return this.incomingDamageAmpFrost; }
    public float getIncomingDamageAmpNature() { return this.incomingDamageAmpNature; }
    public float getIncomingDamageAmpShadow() { return this.incomingDamageAmpShadow; }
    public float getIncomingDamageAmpHoly() { return this.incomingDamageAmpHoly; }
    public float getIncomingDamageDpsMelee() { return this.incomingDamageDpsMelee; }
    public float getIncomingDamageDpsPhysical() { return this.incomingDamageDpsPhysical; }
    public float getIncomingDamageDpsArcane() { return this.incomingDamageDpsArcane; }
    public float getIncomingDamageDpsFire() { return this.incomingDamageDpsFire; }
    public float getIncomingDamageDpsFrost() { return this.incomingDamageDpsFrost; }
    public float getIncomingDamageDpsNature() { return this.incomingDamageDpsNature; }
    public float getIncomingDamageDpsShadow() { return this.incomingDamageDpsShadow; }
    public float getIncomingDamageDpsHoly() { return this.incomingDamageDpsHoly; }
    public float getIncomingDamageDps() { return this.incomingDamageDps; }
    public int getMaxTalents() { return this.maxTalents; }
    public float getMastery() { return this.mastery; }
    public float getManaAdeptBonus() { return this.manaAdeptBonus; }

    // initialized in InitializeSpellTemplates
    private WaterboltTemplate waterboltTemplate;

    public WaterboltTemplate getWaterboltTemplate() {
        if (this.waterboltTemplate == null) this.waterboltTemplate = new WaterboltTemplate();
        if (this.waterboltTemplate.getDirty()) this.waterboltTemplate.initialize(this);
        return this.waterboltTemplate;
    }

    private MirrorImageTemplate mirrorImageTemplate;

    public MirrorImageTemplate getMirrorImageTemplate() {
        if (this.mirrorImageTemplate == null) this.mirrorImageTemplate = new MirrorImageTemplate();
        if (this.mirrorImageTemplate.getDirty()) this.mirrorImageTemplate.initialize(this);
        return this.mirrorImageTemplate;
    }

    private FireBlastTemplate fireBlastTemplate;

    public FireBlastTemplate getFireBlastTemplate() {
        if (this.fireBlastTemplate == null) this.fireBlastTemplate = new FireBlastTemplate();
        if (this.fireBlastTemplate.getDirty()) this.fireBlastTemplate.initialize(this);
        return this.fireBlastTemplate;
    }

    private LightningBoltTemplate lightningBoltTemplate;

    public LightningBoltTemplate getLightningBoltTemplate() {
        if (this.lightningBoltTemplate == null) this.lightningBoltTemplate = new LightningBoltTemplate();
        if (this.lightningBoltTemplate.getDirty()) this.lightningBoltTemplate.initialize(this);
        return this.lightningBoltTemplate;
    }

    private ThunderBoltTemplate thunderBoltTemplate;

    public ThunderBoltTemplate getThunderBoltTemplate() {
        if (this.thunderBoltTemplate == null) this.thunderBoltTemplate = new ThunderBoltTemplate();
        if (this.thunderBoltTemplate.getDirty()) this.thunderBoltTemplate.initialize(this);
        return this.thunderBoltTemplate;
    }

    private LightweaveBoltTemplate lightweaveBoltTemplate;

    public LightweaveBoltTemplate getLightweaveBoltTemplate() {
        if (this.lightweaveBoltTemplate == null) this.lightweaveBoltTemplate = new LightweaveBoltTemplate();
        if (this.lightweaveBoltTemplate.getDirty()) this.lightweaveBoltTemplate.initialize(this);
        return this.lightweaveBoltTemplate;
    }

    private ArcaneBoltTemplate arcaneBoltTemplate;

    public ArcaneBoltTemplate getArcaneBoltTemplate() {
        if (this.arcaneBoltTemplate == null) this.arcaneBoltTemplate = new ArcaneBoltTemplate();
        if (this.arcaneBoltTemplate.getDirty()) this.arcaneBoltTemplate.initialize(this);
        return this.arcaneBoltTemplate;
    }

    private PendulumOfTelluricCurrentsTemplate pendulumOfTelluricCurrentsTemplate;

    public PendulumOfTelluricCurrentsTemplate getPendulumOfTelluricCurrentsTemplate() {
        if (this.pendulumOfTelluricCurrentsTemplate == null) this.pendulumOfTelluricCurrentsTemplate = new PendulumOfTelluricCurrentsTemplate();
        if (this.pendulumOfTelluricCurrentsTemplate.getDirty()) this.pendulumOfTelluricCurrentsTemplate.initialize(this);
        return this.pendulumOfTelluricCurrentsTemplate;
    }

    private FrostboltTemplate frostboltTemplate;

    public FrostboltTemplate getFrostboltTemplate() {
        if (this.frostboltTemplate == null) this.frostboltTemplate = new FrostboltTemplate();
        if (this.frostboltTemplate.getDirty()) this.frostboltTemplate.initialize(this);
        return this.frostboltTemplate;
    }

    private FrostfireBoltTemplate frostfireBoltTemplate;

    public FrostfireBoltTemplate getFrostfireBoltTemplate() {
        if (this.frostfireBoltTemplate == null) this.frostfireBoltTemplate = new FrostfireBoltTemplate();
        if (this.frostfireBoltTemplate.getDirty()) this.frostfireBoltTemplate.initialize(this);
        return this.frostfireBoltTemplate;
    }

    private ArcaneMissilesTemplate arcaneMissilesTemplate;

    public ArcaneMissilesTemplate getArcaneMissilesTemplate() {
        if (this.arcaneMissilesTemplate == null) this.arcaneMissilesTemplate = new ArcaneMissilesTemplate();
        if (this.arcaneMissilesTemplate.getDirty()) this.arcaneMissilesTemplate.initialize(this);
        return this.arcaneMissilesTemplate;
    }

    private FireballTemplate fireballTemplate;

    public FireballTemplate getFireballTemplate() {
        if (this.fireballTemplate == null) this.fireballTemplate = new FireballTemplate();
        if (this.fireballTemplate.getDirty()) this.fireballTemplate.initialize(this);
        return this.fireballTemplate;
    }

    private PyroblastTemplate pyroblastTemplate;

    public PyroblastTemplate getPyroblastTemplate() {
        if (this.pyroblastTemplate == null) this.pyroblastTemplate = new PyroblastTemplate();
        if (this.pyroblastTemplate.getDirty()) this.pyroblastTemplate.initialize(this);
        return this.pyroblastTemplate;
    }

    private ScorchTemplate scorchTemplate;

    public ScorchTemplate getScorchTemplate() {
        if (this.scorchTemplate == null) this.scorchTemplate = new ScorchTemplate();
        if (this.scorchTemplate.getDirty()) this.scorchTemplate.initialize(this);
        return this.scorchTemplate;
    }

    private ArcaneBarrageTemplate arcaneBarrageTemplate;

    public ArcaneBarrageTemplate getArcaneBarrageTemplate() {
        if (this.arcaneBarrageTemplate == null) this.arcaneBarrageTemplate = new ArcaneBarrageTemplate();
        if (this.arcaneBarrageTemplate.getDirty()) this.arcaneBarrageTemplate.initialize(this);
        return this.arcaneBarrageTemplate;
    }

    private DeepFreezeTemplate deepFreezeTemplate;

    public DeepFreezeTemplate getDeepFreezeTemplate() {
        if (this.deepFreezeTemplate == null) this.deepFreezeTemplate = new DeepFreezeTemplate();
        if (this.deepFreezeTemplate.getDirty()) this.deepFreezeTemplate.initialize(this);
        return this.deepFreezeTemplate;
    }

    private ArcaneBlastTemplate arcaneBlastTemplate;

    public ArcaneBlastTemplate getArcaneBlastTemplate() {
        if (this.arcaneBlastTemplate == null) this.arcaneBlastTemplate = new ArcaneBlastTemplate();
        if (this.arcaneBlastTemplate.getDirty()) this.arcaneBlastTemplate.initialize(this);
        return this.arcaneBlastTemplate;
    }

    private IceLanceTemplate iceLanceTemplate;

    public IceLanceTemplate getIceLanceTemplate() {
        if (this.iceLanceTemplate == null) this.iceLanceTemplate = new IceLanceTemplate();
        if (this.iceLanceTemplate.getDirty()) this.iceLanceTemplate.initialize(this);
        return this.iceLanceTemplate;
    }

    private ArcaneExplosionTemplate arcaneExplosionTemplate;

    public ArcaneExplosionTemplate getArcaneExplosionTemplate() {
        if (this.arcaneExplosionTemplate == null) this.arcaneExplosionTemplate = new ArcaneExplosionTemplate();
        if (this.arcaneExplosionTemplate.getDirty()) this.arcaneExplosionTemplate.initialize(this);
        return this.arcaneExplosionTemplate;
    }

    private FlamestrikeTemplate flamestrikeTemplate;

    public FlamestrikeTemplate getFlamestrikeTemplate() {
        if (this.flamestrikeTemplate == null) this.flamestrikeTemplate = new FlamestrikeTemplate();
        if (this.flamestrikeTemplate.getDirty()) this.flamestrikeTemplate.initialize(this);
        return this.flamestrikeTemplate;
    }

    private BlizzardTemplate blizzardTemplate;

    public BlizzardTemplate getBlizzardTemplate() {
        if (this.blizzardTemplate == null) this.blizzardTemplate = new BlizzardTemplate();
        if (this.blizzardTemplate.getDirty()) this.blizzardTemplate.initialize(this);
        return this.blizzardTemplate;
    }

    private BlastWaveTemplate blastWaveTemplate;

    public BlastWaveTemplate getBlastWaveTemplate() {
        if (this.blastWaveTemplate == null) this.blastWaveTemplate = new BlastWaveTemplate();
        if (this.blastWaveTemplate.getDirty()) this.blastWaveTemplate.initialize(this);
        return this.blastWaveTemplate;
    }

    private DragonsBreathTemplate dragonsBreathTemplate;

    public DragonsBreathTemplate getDragonsBreathTemplate() {
        if (this.dragonsBreathTemplate == null) this.dragonsBreathTemplate = new DragonsBreathTemplate();
        if (this.dragonsBreathTemplate.getDirty()) this.dragonsBreathTemplate.initialize(this);
        return this.dragonsBreathTemplate;
    }

    private ConeOfColdTemplate coneOfColdTemplate;

    public ConeOfColdTemplate getConeOfColdTemplate() {
        if (this.coneOfColdTemplate == null) this.coneOfColdTemplate = new ConeOfColdTemplate();
        if (this.coneOfColdTemplate.getDirty()) this.coneOfColdTemplate.initialize(this);
        return this.coneOfColdTemplate;
    }

    private SlowTemplate slowTemplate;

    public SlowTemplate getSlowTemplate() {
        if (this.slowTemplate == null) this.slowTemplate = new SlowTemplate();
        if (this.slowTemplate.getDirty()) this.slowTemplate.initialize(this);
        return this.slowTemplate;
    }

    private LivingBombTemplate livingBombTemplate;

    public LivingBombTemplate getLivingBombTemplate() {
        if (this.livingBombTemplate == null) this.livingBombTemplate = new LivingBombTemplate();
        if (this.livingBombTemplate.getDirty()) this.livingBombTemplate.initialize(this);
        return this.livingBombTemplate;
    }

    private FireWardTemplate fireWardTemplate;

    public FireWardTemplate getFireWardTemplate() {
        if (this.fireWardTemplate == null) this.fireWardTemplate = new FireWardTemplate();
        if (this.fireWardTemplate.getDirty()) this.fireWardTemplate.initialize(this);
        return this.fireWardTemplate;
    }

    private FrostWardTemplate frostWardTemplate;

    public FrostWardTemplate getFrostWardTemplate() {
        if (this.frostWardTemplate == null) this.frostWardTemplate = new FrostWardTemplate();
        if (this.frostWardTemplate.getDirty()) this.frostWardTemplate.initialize(this);
        return this.frostWardTemplate;
    }

    private ConjureManaGemTemplate conjureManaGemTemplate;

    public ConjureManaGemTemplate getConjureManaGemTemplate() {
        if (this.conjureManaGemTemplate == null) this.conjureManaGemTemplate = new ConjureManaGemTemplate();
        if (this.conjureManaGemTemplate.getDirty()) this.conjureManaGemTemplate.initialize(this);
        return this.conjureManaGemTemplate;
    }

    private ArcaneDamageTemplate arcaneDamageTemplate;

    public ArcaneDamageTemplate getArcaneDamageTemplate() {
        if (this.arcaneDamageTemplate == null) this.arcaneDamageTemplate = new ArcaneDamageTemplate();
        if (this.arcaneDamageTemplate.getDirty()) this.arcaneDamageTemplate.initialize(this);
        return this.arcaneDamageTemplate;
    }

    private FireDamageTemplate fireDamageTemplate;

    public FireDamageTemplate getFireDamageTemplate() {
        if (this.fireDamageTemplate == null) this.fireDamageTemplate = new FireDamageTemplate();
        if (this.fireDamageTemplate.getDirty()) this.fireDamageTemplate.initialize(this);
        return this.fireDamageTemplate;
    }

    private FrostDamageTemplate frostDamageTemplate;

    public FrostDamageTemplate getFrostDamageTemplate() {
        if (this.frostDamageTemplate == null) this.frostDamageTemplate = new FrostDamageTemplate();
        if (this.frostDamageTemplate.getDirty()) this.frostDamageTemplate.initialize(this);
        return this.frostDamageTemplate;
    }

    private ShadowDamageTemplate shadowDamageTemplate;

    public ShadowDamageTemplate getShadowDamageTemplate() {
        if (this.shadowDamageTemplate == null) this.shadowDamageTemplate = new ShadowDamageTemplate();
        if (this.shadowDamageTemplate.getDirty()) this.shadowDamageTemplate.initialize(this);
        return this.shadowDamageTemplate;
    }

    private NatureDamageTemplate natureDamageTemplate;

    public NatureDamageTemplate getNatureDamageTemplate() {
        if (this.natureDamageTemplate == null) this.natureDamageTemplate = new NatureDamageTemplate();
        if (this.natureDamageTemplate.getDirty()) this.natureDamageTemplate.initialize(this);
        return this.natureDamageTemplate;
    }

    private HolyDamageTemplate holyDamageTemplate;

    public HolyDamageTemplate getHolyDamageTemplate() {
        if (this.holyDamageTemplate == null) this.holyDamageTemplate = new HolyDamageTemplate();
        if (this.holyDamageTemplate.getDirty()) this.holyDamageTemplate.initialize(this);
        return this.holyDamageTemplate;
    }

    private ValkyrDamageTemplate valkyrDamageTemplate;

    public ValkyrDamageTemplate getValkyrDamageTemplate() {
        if (this.valkyrDamageTemplate == null) this.valkyrDamageTemplate = new ValkyrDamageTemplate();
        if (this.valkyrDamageTemplate.getDirty()) this.valkyrDamageTemplate.initialize(this);
        return this.valkyrDamageTemplate;
    }

    private WandTemplate wandTemplate;

    public WandTemplate getWandTemplate() {
        if (this.wandTemplate == null) this.wandTemplate = new WandTemplate();
        return this.wandTemplate;
    }

    // initialized in GenerateStateList
    private List<CastingState> stateList;
    private List<CastingState> scratchStateList = new ArrayList<CastingState>();
    public CastingState baseState;

    public CastingState getBaseState() {
        return this.baseState;
    }

    // initialized in GenerateSpellList
    private List<CycleId> spellList;

    // initialized in ConstructProblem
    private boolean segmentNonCooldowns;
    private boolean minimizeTime;
    private boolean restrictManaUse;
    private boolean needsTimeExtension;
    private boolean conjureManaGem;
    private float dpsTime;
    public int manaSegments;

    private SolverLP lp;
    private int[] segmentColumn;
    public List<SolutionVariable> solutionVariable;

    private static final double ManaRegenLPScaling = 0.01;
    public float startingMana;

    public Cycle conjureManaGemCycle;
    public int maxConjureManaGem;

    public Spell wand;

    public float maxEvocation;
    public float evocationDuration;
    public float evocationRegen;
    public float evocationDurationIV;
    public float evocationRegenIV;
    public float evocationDurationHero;
    public float evocationRegenHero;
    public float evocationDurationIVHero;
    public float evocationRegenIVHero;

    public int maxManaGem;
    public float manaGemTps;
    public float manaPotionTps;

    // initialized in ConstructSegments
    public List<Segment> segmentList;

    // initialized in ConstructRows
    private StackingConstraint[] rowStackingConstraint;
    private int rowStackingConstraintCount;

    // LP rows
    private int rowManaRegen;
    private int rowFightDuration;
    private int rowEvocation;
    private int rowEvocationIV;
    private int rowEvocationHero;
    private int rowEvocationIVHero;
    private int rowPotion;
    private int rowManaPotion;
    private int rowConjureManaGem;
    private int rowManaGem;
    private int rowManaGemMax;
    private int rowHeroism;
    private int rowArcanePower;
    private int rowIcyVeins;
    private int rowWaterElemental;
    private int rowMirrorImage;
    private int rowMoltenFury;
    private int rowMoltenFuryIcyVeins;
    private int rowFlameCap;
    private int rowManaGemEffect;
    private int rowManaGemEffectActivation;
    private int rowDpsTime;
    private int rowAoe;
    private int rowFlamestrike;
    private int rowConeOfCold;
    private int rowBlastWave;
    private int rowDragonsBreath;
    private int rowCombustion;
    private int rowPowerInfusion;
    private int rowMoltenFuryCombustion;
    private int rowHeroismCombustion;
    private int rowHeroismIcyVeins;
    private int rowSummonWaterElemental;
    private int rowSummonWaterElementalCount;
    private int rowSummonMirrorImage;
    private int rowSummonMirrorImageCount;
    private int rowThreat;
    private int rowBerserking;
    private int rowTimeExtension;
    private int rowAfterFightRegenMana;
    private int rowTargetDamage;
    private int rowSegment;
    private int rowSegmentManaOverflow;
    private int rowSegmentManaUnderflow;
    private int rowSegmentThreat;
    private List<SegmentConstraint> rowSegmentArcanePower;
    private List<SegmentConstraint> rowSegmentPowerInfusion;
    private List<SegmentConstraint> rowSegmentIcyVeins;
    private List<SegmentConstraint> rowSegmentWaterElemental;
    private List<SegmentConstraint> rowSegmentSummonWaterElemental;
    private List<SegmentConstraint> rowSegmentMirrorImage;
    private List<SegmentConstraint> rowSegmentSummonMirrorImage;
    private List<SegmentConstraint> rowSegmentCombustion;
    private List<SegmentConstraint> rowSegmentBerserking;
    private List<SegmentConstraint> rowSegmentFlameCap;
    private List<SegmentConstraint> rowSegmentManaGem;
    private List<SegmentConstraint> rowSegmentManaGemEffect;
    private List<SegmentConstraint> rowSegmentEvocation;

    // initialized in RestrictSolution
    private double[] solution;
    private double lowerBound;
    private double upperBound;

    // initialized in EvaluateSurvivability
    public float chanceToDie;
    public float meanIncomingDps;

    public float getChanceToDie() { return this.chanceToDie; }

    public float getMeanIncomingDps() { return this.meanIncomingDps; }

    public float getStartingMana() { return this.startingMana; }

    public int getMaxConjureManaGem() { return maxConjureManaGem; }

    public Cycle getConjureManaGem() { return conjureManaGemCycle; }

    public Spell getWand() { return this.wand; }

    public int getMaxManaGem() { return this.maxManaGem; }

    public float getManaGemTps() { return this.manaGemTps; }

    public float getManaPotionTps() { return this.manaPotionTps; }

    public float getManaGemEffectDuration() { return this.manaGemEffectDuration; }

    public float getEvocationDuration() { return this.evocationDuration; }
    public float getEvocationRegen() { return this.evocationRegen; }
    public float getEvocationDurationIV() { return this.evocationDurationIV; }
    public float getEvocationRegenIV() { return this.evocationRegenIV; }
    public float getEvocationDurationHero() { return this.evocationDurationHero; }
    public float getEvocationRegenHero() { return this.evocationRegenHero; }
    public float getEvocationDurationIVHero() { return this.evocationDurationIVHero; }
    public float getEvocationRegenIVHero() { return this.evocationRegenIVHero; }
    public float getMaxEvocation() { return this.maxEvocation; }

    public List<EffectCooldown> getCooldownList() { return this.cooldownList; }

    public SpecialEffect[] getSpellPowerEffects() { return this.spellPowerEffects; }

    public int getSpellPowerEffectsCount() { return this.spellPowerEffectsCount; }

    public SpecialEffect[] getHasteRatingEffects() { return this.hasteRatingEffects; }

    public int getHasteRatingEffectsCount() { return this.hasteRatingEffectsCount; }

    public SpecialEffect[] getDamageProcEffects() { return this.damageProcEffects; }

    public int getDamageProcEffectsCount() { return this.damageProcEffectsCount; }

    public SpecialEffect[] getManaRestoreEffects() { return this.manaRestoreEffects; }

    public int getManaRestoreEffectsCount() { return this.manaRestoreEffectsCount; }

    public SpecialEffect[] getMp5Effects() { return this.mp5Effects; }

    public int getMp5EffectsCount() { return this.mp5EffectsCount; }

    public EffectCooldown[] getStackingHasteEffectCooldowns() { return this.stackingHasteEffectCooldowns; }

    public int getStackingHasteEffectCooldownsCount() { return this.stackingHasteEffectCooldownsCount; }

    public EffectCooldown[] getStackingNonHasteEffectCooldowns() { return this.stackingNonHasteEffectCooldowns; }

    public int getStackingNonHasteEffectCooldownsCount() { return this.stackingNonHasteEffectCooldownsCount; }

    public List<Buff> getActiveBuffs() { return this.activeBuffs; }

    public Map<Integer, EffectCooldown> getEffectCooldown() { return this.effectCooldown; }

    public List<Segment> getSegmentList() { return this.segmentList; }

    public List<SolutionVariable> getSolutionVariable() { return this.solutionVariable; }

    public boolean getNeedsDisplayCalculations() { return this.needsDisplayCalculations; }

    public boolean getCancellationPending() { return cancellationPending; }

    public Character getCharacter() { return this.character; }

    public MageTalents getMageTalents() { return this.mageTalents; }

    public CalculationOptionsMage getCalculationOptions() { return this.calculationOptions; }

    public Stats getBaseStats() { return this.baseStats; }

    public TargetDebuffStats getTargetDebuffs() {
        if (targetDebuffs == null) {
            targetDebuffs = new TargetDebuffStats();
            for (Buff buff : this.activeBuffs) {
                if (buff.isTargetDebuff) {
                    targetDebuffs.accumulate(buff.stats);
                }
            }
        }
        return targetDebuffs;
    }

    public void cancelAsync() {
        cancellationPending = true;
    }

    public List<EffectCooldown> getEffectList(int effects) {
        List<EffectCooldown> ret = new ArrayList<EffectCooldown>();
        for (EffectCooldown effect : this.cooldownList) {
            if ((effects & effect.mask) == effect.mask) ret.add(effect);
        }
        return ret;
    }

    public String effectsDescription(int effects) {
        List<String> buffList = new ArrayList<String>();
        List<EffectCooldown> cooldownList = this.cooldownList;
        for (int i = 0; i < cooldownList.size(); i++) {
            EffectCooldown effect = cooldownList.get(i);
            if ((effects & effect.mask) == effect.mask) {
                buffList.add(effect.name);
            }
        }
        return String.join("+", buffList);
    }

    private static boolean isItemActivatable(ItemInstance item) {
        if (item == null || item.getItem() == null) return false;
        return item.getItem().getStats().containsSpecialEffect(effect -> effect.getTrigger() == Trigger.Use);
    }

    public Solver(Character character, CalculationOptionsMage calculationOptions, boolean segmentCooldowns, boolean segmentMana, boolean integralMana, int advancedConstraintsLevel, String armor, boolean useIncrementalOptimizations, boolean useGlobalOptimizations, boolean needsDisplayCalculations, boolean needsSolutionVariables) {
        construct(character, calculationOptions, segmentCooldowns, segmentMana, integralMana, advancedConstraintsLevel, armor, useIncrementalOptimizations, useGlobalOptimizations, needsDisplayCalculations, needsSolutionVariables);
    }

    private void construct(Character character, CalculationOptionsMage calculationOptions, boolean segmentCooldowns, boolean segmentMana, boolean integralMana, int advancedConstraintsLevel, String armor, boolean useIncrementalOptimizations, boolean useGlobalOptimizations, boolean needsDisplayCalculations, boolean needsSolutionVariables) {
        this.character = character;
        this.mageTalents = character.getMageTalents();
        this.calculationOptions = calculationOptions;
        this.segmentCooldowns = segmentCooldowns;
        this.segmentMana = segmentMana;
        this.advancedConstraintsLevel = advancedConstraintsLevel;
        this.integralMana = integralMana;
        this.armor = armor;
        this.useIncrementalOptimizations = useIncrementalOptimizations;
        this.useGlobalOptimizations = useGlobalOptimizations;
        this.needsDisplayCalculations = needsDisplayCalculations;
        this.requiresMIP = segmentCooldowns || integralMana;
        if (needsDisplayCalculations || requiresMIP) needsSolutionVariables = true;
        this.needsSolutionVariables = needsSolutionVariables;
        this.needsQuadratic = false;
        cancellationPending = false;
    }

    private static final ThreadLocal<Solver> threadSolver = new ThreadLocal<Solver>();

    public static CharacterCalculationsMage getCharacterCalculations(Character character, Item additionalItem, CalculationOptionsMage calculationOptions, CalculationsMage calculations, String armor, boolean segmentCooldowns, boolean segmentMana, boolean integralMana, int advancedConstraintsLevel, boolean useIncrementalOptimizations, boolean useGlobalOptimizations, boolean needsDisplayCalculations, boolean needsSolutionVariables) {
        Solver s = threadSolver.get();
        if (s == null) {
            s = new Solver(character, calculationOptions, segmentCooldowns, segmentMana, integralMana, advancedConstraintsLevel, armor, useIncrementalOptimizations, useGlobalOptimizations, needsDisplayCalculations, needsSolutionVariables);
            threadSolver.set(s);
        } else {
            s.construct(character, calculationOptions, segmentCooldowns, segmentMana, integralMana, advancedConstraintsLevel, armor, useIncrementalOptimizations, useGlobalOptimizations, needsDisplayCalculations, needsSolutionVariables);
        }
        return s.getCharacterCalculations(additionalItem);
    }

    public CharacterCalculationsMage getCharacterCalculations(Item additionalItem) {
        this.arraySet = ArrayPool.requestArraySet(!useIncrementalOptimizations && this.needsDisplayCalculations && segmentCooldowns);

        initialize(additionalItem);

        generateSpellList();
        generateStateList();

        constructProblem();

        if (needsQuadratic) {
            solveQuadratic();
        }

        if (requiresMIP) {
            restrictSolution();
        }

        solution = lp.solve();

        CharacterCalculationsMage ret = getCalculationsResult();

        ArrayPool.releaseArraySet(this.arraySet);
        this.arraySet = null;

        return ret;
    }

    // Effect Maximization
    private double maximizeColdsnapDuration(double fightDuration, double coldsnapCooldown, double effectDuration, double effectCooldown, int[] coldsnapCount) {
        int bestColdsnap = 0;
        double bestEffect = 0.0;
        List<Integer> coldsnap = new ArrayList<Integer>();
        List<Double> startTime = new ArrayList<Double>();
        List<Double> coldsnapTime = new ArrayList<Double>();
        int index = 0;
        coldsnap.add(2);
        startTime.add(0.0);
        coldsnapTime.add(0.0);
        do {
            if (index > 0 && startTime.get(index - 1) + effectDuration >= fightDuration) {
                double effect = (index - 1) * effectDuration + Math.max(fightDuration - startTime.get(index - 1), 0.0);
                if (effect > bestEffect) {
                    bestEffect = effect;
                    bestColdsnap = 0;
                    for (int i = 0; i < index; i++) {
                        if (startTime.get(i) < fightDuration - 20.0) bestColdsnap += coldsnap.get(i);
                    }
                }
                index--;
            }
            coldsnap.set(index, coldsnap.get(index) - 1);
            if (coldsnap.get(index) < 0) {
                index--;
            } else {
                double time = 0.0;
                if (index > 0) {
                    time = startTime.get(index - 1) + effectDuration;
                    int lastColdsnap = -1;
                    for (int j = 0; j < index; j++) {
                        if (coldsnap.get(j) == 1) lastColdsnap = j;
                    }
                    if (coldsnap.get(index) == 1) {
                        // use coldsnap
                        double normalTime = Math.max(time, startTime.get(index - 1) + effectCooldown);
                        double coldsnapReady = 0.0;
                        if (lastColdsnap >= 0) coldsnapReady = coldsnapTime.get(lastColdsnap) + coldsnapCooldown;
                        if (coldsnapReady >= normalTime) {
                            // coldsnap won't be ready until effect will be back anyway, so we don't actually need it
                            coldsnap.set(index, 0);
                            time = normalTime;
                        } else {
                            // go now or when coldsnap is ready
                            time = Math.max(coldsnapReady, time);
                            coldsnapTime.set(index, Math.max(coldsnapReady, startTime.get(index - 1)));
                        }
                    } else {
                        // we are not allowed to use coldsnap even if we could
                        // make sure to adjust by coldsnap constraints
                        time = Math.max(time, startTime.get(index - 1) + effectCooldown);
                    }
                } else {
                    coldsnap.set(index, 0);
                }
                startTime.set(index, time);
                index++;
                if (index >= coldsnap.size()) {
                    coldsnap.add(0);
                    coldsnapTime.add(0.0);
                    startTime.add(0.0);
                }
                coldsnap.set(index, 2);
            }
        } while (index >= 0);
        coldsnapCount[0] = bestColdsnap;
        return bestEffect;
    }

    private double maximizeEffectDuration(double fightDuration, double effectDuration, double effectCooldown) {
        if (fightDuration < effectDuration) return fightDuration;
        double total = effectDuration;
        fightDuration -= effectDuration;
        int count = (int) (fightDuration / effectCooldown);
        total += effectDuration * count;
        fightDuration -= effectCooldown * count;
        fightDuration -= effectCooldown - effectDuration;
        if (fightDuration > 0) total += fightDuration;
        return total;
    }

    private double maximizeStackingDuration(double fightDuration, double effect1Duration, double effect1Cooldown, double effect2Duration, double effect2Cooldown) {
        List<CooldownStackingCacheEntry> cache = this.calculationOptions.getCooldownStackingCache();
        synchronized (cache) {
            // do binary search
            int num = 0;
            int num2 = cache.size() - 1;
            while (num <= num2) {
                int num3 = num + ((num2 - num) >> 1);
                CooldownStackingCacheEntry entry = cache.get(num3);
                double key = entry.effect1Duration;
                if (key < effect1Duration) {
                    num = num3 + 1;
                } else if (key > effect1Duration) {
                    num2 = num3 - 1;
                } else {
                    key = entry.effect1Cooldown;
                    if (key < effect1Cooldown) {
                        num = num3 + 1;
                    } else if (key > effect1Cooldown) {
                        num2 = num3 - 1;
                    } else {
                        key = entry.effect2Duration;
                        if (key < effect2Duration) {
                            num = num3 + 1;
                        } else if (key > effect2Duration) {
                            num2 = num3 - 1;
                        } else {
                            key = entry.effect2Cooldown;
                            if (key < effect2Cooldown) {
                                num = num3 + 1;
                            } else if (key > effect2Cooldown) {
                                num2 = num3 - 1;
                            } else {
                                return entry.maximumStackingDuration;
                            }
                        }
                    }
                }
            }

            double value;
            if (effect1Cooldown == Double.POSITIVE_INFINITY || effect2Cooldown == Double.POSITIVE_INFINITY) {
                value = maximizeStackingDuration(fightDuration, effect1Duration, effect1Cooldown, effect2Duration, effect2Cooldown, 0, 0);
            } else {
                memoizationCache = newMemoizationCache((int) fightDuration + 1);
                value = maximizeStackingDuration2((int) fightDuration, (int) effect1Duration, (int) effect1Cooldown, (int) effect2Duration, (int) effect2Cooldown, 0, 0, 0);
                memoizationCache = null;
            }
            CooldownStackingCacheEntry newEntry = new CooldownStackingCacheEntry();
            newEntry.effect1Duration = effect1Duration;
            newEntry.effect1Cooldown = effect1Cooldown;
            newEntry.effect2Duration = effect2Duration;
            newEntry.effect2Cooldown = effect2Cooldown;
            newEntry.maximumStackingDuration = value;
            cache.add(num, newEntry);
            return value;
        }
    }

    private double maximizeStackingDuration(double fightDuration, double effect1Duration, double effect1Cooldown, double effect2Duration, double effect2Cooldown, double effect2ActiveDuration, double effect2ActiveCooldown) {
        if (fightDuration <= 0) return 0;
        if (effect2ActiveCooldown == Double.POSITIVE_INFINITY && effect2ActiveDuration == 0) return 0;
        effect2ActiveDuration = Math.min(effect2ActiveDuration, fightDuration);

        double slack = 0;
        double f = fightDuration;

        if (f < effect1Duration) {
            slack = 0;
        } else {
            f -= effect1Duration;
            int count = (int) (f / effect1Cooldown);
            if (count > 0) {
                f -= effect1Cooldown * count;
            }
            if (f - effect1Cooldown + effect1Duration > 0) {
                slack = 0;
            } else {
                slack = f;
            }
        }
        if (!this.calculationOptions.getMaxUseAssumption()) {
            slack = effect2ActiveCooldown;
        }

        // ####........|
        double best = 0;
        double value = 0;
        double min = 0;

        if (effect2ActiveCooldown > 0) {
            // if optimal placement of effect1 is stacked with effect2 activation
            // and it doesn't overlap two different activations
            // or if optimal placement has effect1 activated and finished before effect2 gets off cooldown
            // then we'll get as good or better stacking if we move effect1 all the way to the start
            if (effect1Cooldown < effect2ActiveCooldown) {
                // effect1 will be off cooldown first
                value = Math.min(effect1Duration, effect2ActiveDuration) + maximizeStackingDuration(fightDuration - effect1Cooldown, effect1Duration, effect1Cooldown, effect2Duration, effect2Cooldown, Math.max(0, effect2ActiveDuration - effect1Cooldown), Math.max(0, effect2ActiveCooldown - effect1Cooldown));
            } else {
                // effect2 will be off cooldown first
                value = Math.min(effect1Duration, effect2ActiveDuration) + maximizeStackingDuration(fightDuration - effect2ActiveCooldown, effect2Duration, effect2Cooldown, effect1Duration, effect1Cooldown, Math.max(0, effect1Duration - effect2ActiveCooldown), Math.max(0, effect1Cooldown - effect2ActiveCooldown));
            }
            if (value > best) {
                best = value;
            }
        }
        // the next case is if effect1 activation crosses over effect2 cooldown
        // in this case it's just as good if effect2 starts right on cooldown
        // now in this case moving effect1 earlier has negative effect so we can't do that
        // we want however to push it as late as possible without affecting the optimum
        // but just as long as it ends before effect2 ends
        // ####........|#####
        // ..........#######
        // 0 <= offset <= effect2ActiveCooldown
        // offset <= effect1Duration
        if (effect2ActiveCooldown != Double.POSITIVE_INFINITY) {
            int minOffset = Math.max(0, (int) (effect2ActiveCooldown - slack));
            int maxOffset = (int) Math.min(effect1Duration, effect2ActiveCooldown);
            if (effect2Cooldown >= effect1Duration) {
                int endAlignedOffset = (int) (effect1Duration - effect2Duration);
                if (endAlignedOffset > minOffset) {
                    minOffset = Math.min(endAlignedOffset, maxOffset);
                }
            }
            for (int offset = minOffset; offset <= maxOffset; offset++) {
                // is there any stacking left from current effect2 activation?
                double leftover = Math.max(0, effect2ActiveDuration - (effect2ActiveCooldown - offset));
                min = Math.min(effect1Duration - offset, effect2Duration);
                if (effect1Cooldown - offset < effect2Cooldown) {
                    // effect1 will be off cooldown first
                    value = leftover + Math.min(min, fightDuration - effect2ActiveCooldown) + maximizeStackingDuration(fightDuration - effect2ActiveCooldown - effect1Cooldown + offset, effect1Duration, effect1Cooldown, effect2Duration, effect2Cooldown, Math.max(0, effect2Duration - effect1Cooldown + offset), Math.max(0, effect2Cooldown - effect1Cooldown + offset));
                } else {
                    // effect2 will be off cooldown first
                    value = leftover + Math.min(min, fightDuration - effect2ActiveCooldown) + maximizeStackingDuration(fightDuration - effect2ActiveCooldown - effect2Cooldown, effect2Duration, effect2Cooldown, effect1Duration, effect1Cooldown, Math.max(0, effect1Duration - offset - effect2Cooldown), Math.max(0, effect1Cooldown - offset - effect2Cooldown));
                }
                if (value > best) {
                    best = value;
                }
            }
        }
        return best;
    }

    private List<StackingMemoizationEntry>[] memoizationCache;

    @SuppressWarnings("unchecked")
    private static List<StackingMemoizationEntry>[] newMemoizationCache(int size) {
        return new List[size];
    }

    private int maximizeStackingDuration2(int fightDuration, int effect1Duration, int effect1Cooldown, int effect2Duration, int effect2Cooldown, int effect2ActiveDuration, int effect2ActiveCooldown, int order) {
        if (fightDuration <= 0) return 0;
        effect2ActiveDuration = Math.min(effect2ActiveDuration, fightDuration);

        List<StackingMemoizationEntry> list = memoizationCache[fightDuration];
        if (list == null) {
            list = new ArrayList<StackingMemoizationEntry>();
            memoizationCache[fightDuration] = list;
        }

        for (int i = 0; i < list.size(); i++) {
            StackingMemoizationEntry entry = list.get(i);
            if (entry.effect2ActiveCooldown == effect2ActiveCooldown && entry.order == order && entry.effect2ActiveDuration == effect2ActiveDuration) {
                return entry.maximizeStackingDuration;
            }
        }

        int slack = 0;
        int f = fightDuration;

        if (f < effect1Duration) {
            slack = 0;
        } else {
            f -= effect1Duration;
            int count = f / effect1Cooldown;
            if (count > 0) {
                f -= effect1Cooldown * count;
            }
            if (f - effect1Cooldown + effect1Duration > 0) {
                slack = 0;
            } else {
                slack = f;
            }
        }
        if (!this.calculationOptions.getMaxUseAssumption()) {
            slack = effect2ActiveCooldown;
        }

        // ####........|
        int best = 0;
        int value = 0;
        int min = 0;

        if (effect2ActiveCooldown > 0) {
            if (effect1Cooldown < effect2ActiveCooldown) {
                value = Math.min(effect1Duration, effect2ActiveDuration) + maximizeStackingDuration2(fightDuration - effect1Cooldown, effect1Duration, effect1Cooldown, effect2Duration, effect2Cooldown, Math.max(0, effect2ActiveDuration - effect1Cooldown), Math.max(0, effect2ActiveCooldown - effect1Cooldown), order);
            } else {
                value = Math.min(effect1Duration, effect2ActiveDuration) + maximizeStackingDuration2(fightDuration - effect2ActiveCooldown, effect2Duration, effect2Cooldown, effect1Duration, effect1Cooldown, Math.max(0, effect1Duration - effect2ActiveCooldown), Math.max(0, effect1Cooldown - effect2ActiveCooldown), 1 - order);
            }
            if (value > best) {
                best = value;
            }
        }
        int minOffset = Math.max(0, effect2ActiveCooldown - slack);
        int maxOffset = Math.min(effect1Duration, effect2ActiveCooldown);
        if (effect2Cooldown >= effect1Duration) {
            int endAlignedOffset = effect1Duration - effect2Duration;
            if (endAlignedOffset > minOffset) {
                minOffset = Math.min(endAlignedOffset, maxOffset);
            }
        }
        for (int offset = minOffset; offset <= maxOffset; offset++) {
            int leftover = Math.max(0, effect2ActiveDuration - (effect2ActiveCooldown - offset));
            min = Math.min(effect1Duration - offset, effect2Duration);
            if (effect1Cooldown - offset < effect2Cooldown) {
                value = leftover + Math.min(min, fightDuration - effect2ActiveCooldown) + maximizeStackingDuration2(fightDuration - effect2ActiveCooldown - effect1Cooldown + offset, effect1Duration, effect1Cooldown, effect2Duration, effect2Cooldown, Math.max(0, effect2Duration - effect1Cooldown + offset), Math.max(0, effect2Cooldown - effect1Cooldown + offset), order);
            } else {
                value = leftover + Math.min(min, fightDuration - effect2ActiveCooldown) + maximizeStackingDuration2(fightDuration - effect2ActiveCooldown - effect2Cooldown, effect2Duration, effect2Cooldown, effect1Duration, effect1Cooldown, Math.max(0, effect1Duration - offset - effect2Cooldown), Math.max(0, effect1Cooldown - offset - effect2Cooldown), 1 - order);
            }
            if (value > best) {
                best = value;
            }
        }

        StackingMemoizationEntry newEntry = new StackingMemoizationEntry();
        newEntry.order = order;
        newEntry.effect2ActiveDuration = effect2ActiveDuration;
        newEntry.effect2ActiveCooldown = effect2ActiveCooldown;
        newEntry.maximizeStackingDuration = best;
        list.add(newEntry);

        return best;
    }

    // Initialize
    public void initialize(Item additionalItem) {
        Stats rawStats;
        if (this.needsDisplayCalculations || this.arraySet == null) {
            rawStats = new Stats();
        } else {
            rawStats = this.arraySet.accumulator;
            if (rawStats == null) {
                rawStats = new Stats();
                this.arraySet.accumulator = rawStats;
            } else {
                rawStats.clear();
            }
        }

        CalculationsMage calculations = CalculationsMage.getInstance();
        targetDebuffs = null;
        @SuppressWarnings("unchecked")
        List<Buff>[] autoActivatedOut = new List[1];
        @SuppressWarnings("unchecked")
        List<Buff>[] activeOut = new List[1];
        calculations.accumulateRawStats(rawStats, this.character, additionalItem, this.calculationOptions, autoActivatedOut, armor, activeOut);
        autoActivatedBuffs = autoActivatedOut[0];
        this.activeBuffs = activeOut[0];
        this.baseStats = calculations.getCharacterStats(this.character, additionalItem, rawStats, this.calculationOptions);

        evocationAvailable = this.calculationOptions.getEvocationEnabled() && !this.calculationOptions.getEffectDisableManaSources();
        manaPotionAvailable = this.calculationOptions.getManaPotionEnabled() && !this.calculationOptions.getEffectDisableManaSources();
        restrictThreat = segmentCooldowns && this.calculationOptions.getTpsLimit() > 0f;
        powerInfusionAvailable = !this.calculationOptions.getDisableCooldowns() && this.calculationOptions.getPowerInfusionAvailable();
        heroismAvailable = !this.calculationOptions.getDisableCooldowns() && this.calculationOptions.getHeroismAvailable();
        arcanePowerAvailable = !this.calculationOptions.getDisableCooldowns() && (this.mageTalents.getArcanePower() == 1);
        icyVeinsAvailable = !this.calculationOptions.getDisableCooldowns() && (this.mageTalents.getIcyVeins() == 1);
        combustionAvailable = !this.calculationOptions.getDisableCooldowns() && (this.mageTalents.getCombustion() == 1);
        moltenFuryAvailable = this.mageTalents.getMoltenFury() > 0;
        coldsnapAvailable = !this.calculationOptions.getDisableCooldowns() && (this.mageTalents.getColdSnap() == 1);
        potionOfWildMagicAvailable = !this.calculationOptions.getDisableCooldowns() && this.calculationOptions.getPotionOfWildMagic();
        potionOfSpeedAvailable = !this.calculationOptions.getDisableCooldowns() && this.calculationOptions.getPotionOfSpeed();
        effectPotionAvailable = potionOfWildMagicAvailable || potionOfSpeedAvailable;
        flameCapAvailable = !this.calculationOptions.getDisableCooldowns() && this.calculationOptions.getFlameCap();
        berserkingAvailable = !this.calculationOptions.getDisableCooldowns() && this.character.getRace() == CharacterRace.Troll;
        waterElementalAvailable = !this.calculationOptions.getDisableCooldowns() && (this.mageTalents.getSummonWaterElemental() == 1);
        mirrorImageAvailable = !this.calculationOptions.getDisableCooldowns() && this.calculationOptions.getMirrorImageEnabled();
        manaGemEffectAvailable = this.calculationOptions.getManaGemEnabled() && this.baseStats.containsSpecialEffect(effect -> effect.getTrigger() == Trigger.ManaGem);

        if (!this.calculationOptions.getEffectDisableManaSources()) {
            if (this.calculationOptions.getPlayerLevel() < 77) {
                this.manaGemValue = 2400.0f;
                this.maxManaGemValue = 2460.0f;
            } else {
                this.manaGemValue = 3415.0f;
                this.maxManaGemValue = 3500.0f;
            }
            if (this.calculationOptions.getPlayerLevel() <= 70) {
                this.manaPotionValue = 2400.0f;
                this.maxManaPotionValue = 3000.0f;
            } else {
                this.manaPotionValue = 4300.0f;
                this.maxManaPotionValue = 4400.0f;
            }
        }

        initializeEffectCooldowns();
        initializeProcEffects();

        if (armor == null) {
            if (this.character.getActiveBuffs().contains(CalculationsMage.MoltenArmorBuff)) armor = "Molten Armor";
            else if (this.character.getActiveBuffs().contains(CalculationsMage.MageArmorBuff)) armor = "Mage Armor";
            else if (this.character.getActiveBuffs().contains(CalculationsMage.IceArmorBuff)) armor = "Ice Armor";
        }

        calculateBaseStateStats();

        initializeSpellTemplates();
    }

    private void initializeProcEffects() {
        Stats baseStats = this.baseStats;
        int N = baseStats.getRawSpecialEffectDataSize();
        this.hasteRatingEffectsCount = 0;
        if (this.hasteRatingEffects == null || this.hasteRatingEffects.length < N) {
            this.hasteRatingEffects = new SpecialEffect[N];
        }
        this.spellPowerEffectsCount = 0;
        if (this.spellPowerEffects == null || this.spellPowerEffects.length < N) {
            this.spellPowerEffects = new SpecialEffect[N];
        }
        this.damageProcEffectsCount = 0;
        if (this.damageProcEffects == null || this.damageProcEffects.length < N) {
            this.damageProcEffects = new SpecialEffect[N];
        }
        this.manaRestoreEffectsCount = 0;
        if (this.manaRestoreEffects == null || this.manaRestoreEffects.length < N) {
            this.manaRestoreEffects = new SpecialEffect[N];
        }
        this.mp5EffectsCount = 0;
        if (this.mp5Effects == null || this.mp5Effects.length < N) {
            this.mp5Effects = new SpecialEffect[N];
        }
        for (int i = 0; i < baseStats.getRawSpecialEffectDataSize(); i++) {
            SpecialEffect effect = baseStats.getRawSpecialEffectData()[i];
            if (effect.getStats().getHasteRating() > 0 && effect.getMaxStack() == 1) {
                if (effect.getCooldown() >= effect.getDuration() && (effect.getTrigger() == Trigger.DamageSpellCrit || effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellHit || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.DamageSpellCast)) {
                    this.hasteRatingEffects[this.hasteRatingEffectsCount++] = effect;
                } else if (effect.getCooldown() == 0 && (effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.DamageSpellCrit)) {
                    this.hasteRatingEffects[this.hasteRatingEffectsCount++] = effect;
                }
            }
            if (effect.getStats().getSpellPower() > 0 && effect.getMaxStack() == 1) {
                if (effect.getTrigger() == Trigger.DamageSpellCrit || effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellHit || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.DamageSpellCast || effect.getTrigger() == Trigger.SpellMiss || effect.getTrigger() == Trigger.MageNukeCast || effect.getTrigger() == Trigger.DamageDone || effect.getTrigger() == Trigger.DoTTick || effect.getTrigger() == Trigger.DamageOrHealingDone) {
                    this.spellPowerEffects[this.spellPowerEffectsCount++] = effect;
                }
            }
            if (effect.getStats().getArcaneDamage() + effect.getStats().getFireDamage() + effect.getStats().getFrostDamage() + effect.getStats().getNatureDamage() + effect.getStats().getShadowDamage() + effect.getStats().getHolyDamage() + effect.getStats().getValkyrDamage() > 0 && effect.getMaxStack() == 1) {
                if (effect.getTrigger() == Trigger.DamageSpellCrit || effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellHit || effect.getTrigger() == Trigger.DamageDone || effect.getTrigger() == Trigger.DoTTick || effect.getTrigger() == Trigger.DamageOrHealingDone || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.DamageSpellCast) {
                    this.damageProcEffects[this.damageProcEffectsCount++] = effect;
                }
            }
            if (effect.getStats().getManaRestore() > 0 && effect.getMaxStack() == 1) {
                if (effect.getTrigger() == Trigger.Use || effect.getTrigger() == Trigger.DamageSpellCast || effect.getTrigger() == Trigger.DamageSpellCrit || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.SpellHit || effect.getTrigger() == Trigger.DamageDone || effect.getTrigger() == Trigger.DoTTick || effect.getTrigger() == Trigger.DamageOrHealingDone) {
                    this.manaRestoreEffects[this.manaRestoreEffectsCount++] = effect;
                }
            }
            if (effect.getStats().getMp5() > 0 && effect.getMaxStack() == 1) {
                if (effect.getTrigger() == Trigger.Use || effect.getTrigger() == Trigger.DamageSpellCast || effect.getTrigger() == Trigger.DamageSpellCrit || effect.getTrigger() == Trigger.DamageSpellHit || effect.getTrigger() == Trigger.SpellCast || effect.getTrigger() == Trigger.SpellCrit || effect.getTrigger() == Trigger.SpellHit || effect.getTrigger() == Trigger.DamageDone || effect.getTrigger() == Trigger.DoTTick || effect.getTrigger() == Trigger.DamageOrHealingDone) {
                    this.mp5Effects[this.mp5EffectsCount++] = effect;
                }
            }
        }
    }

    private boolean isRelevantOnUseEffect(SpecialEffect effect, boolean[] hasteEffect, boolean[] stackingEffect) {
        // check if it is a stacking use effect
        stackingEffect[0] = false;
        hasteEffect[0] = false;
        Stats effectStats = effect.getStats();
        for (int i = 0; i < effectStats.getRawSpecialEffectDataSize(); i++) {
            SpecialEffect e = effectStats.getRawSpecialEffectData()[i];
            if (e.getChance() == 1f && e.getCooldown() == 0f && (e.getTrigger() == Trigger.DamageSpellCast || e.getTrigger() == Trigger.DamageSpellHit || e.getTrigger() == Trigger.SpellCast || e.getTrigger() == Trigger.SpellHit)) {
                if (e.getStats().getHasteRating() > 0) {
                    hasteEffect[0] = true;
                    stackingEffect[0] = true;
                    break;
                }
            }
            if (e.getChance() == 1f && e.getCooldown() == 0f && (e.getTrigger() == Trigger.DamageSpellCrit || e.getTrigger() == Trigger.SpellCrit)) {
                if (e.getStats().getCritRating() < 0 && effect.getStats().getCritRating() > 0) {
                    stackingEffect[0] = true;
                    break;
                }
            }
        }
        if (stackingEffect[0]) {
            return true;
        }
        if (effect.getStats().getHasteRating() > 0) {
            hasteEffect[0] = true;
        }
        return effect.getStats().getSpellPower() + effect.getStats().getHasteRating() > 0;
    }

    private static final Color[] itemColors = new Color[]{
            new Color(0x00, 0xFF, 0xFF, 0xFF), //Aqua
            new Color(0, 0, 255, 255),
            new Color(0xFF, 0x7F, 0x50, 0xFF), //Coral
            new Color(0xBD, 0xB7, 0x6B, 0xFF), //DarkKhaki
            new Color(0x2F, 0x4F, 0x4F, 0xFF), //DarkSlateGray
            new Color(0xB2, 0x22, 0x22, 0xFF), //Firebrick
            new Color(0xFF, 0xD7, 0x00, 0xFF), //Gold
            new Color(0xFF, 0xFF, 0xF0, 0xFF), //Ivory
    };

    private EffectCooldown newEffectCooldown() {
        if (this.needsDisplayCalculations || this.arraySet == null) {
            return new EffectCooldown();
        } else {
            EffectCooldown effect = this.arraySet.newEffectCooldown();
            effect.clear();
            return effect;
        }
    }

    private EffectCooldown newStandardEffectCooldown(EffectCooldown cachedEffect) {
        if (this.needsDisplayCalculations || this.arraySet == null) {
            return cachedEffect.clone();
        } else {
            cachedEffect.clear();
            return cachedEffect;
        }
    }

    EffectCooldown cachedEffectEvocation = new EffectCooldown() {{
        this.mask = rawr.models.mage.StandardEffect.Evocation;
        this.name = "Evocation";
        this.standardEffect = rawr.models.mage.StandardEffect.Evocation;
        this.color = new Color(0x7F, 0xFF, 0xD4, 0xFF); //Aquamarine
    }};
    EffectCooldown cachedEffectPowerInfusion = new EffectCooldown() {{
        this.cooldown = PowerInfusionCooldown;
        this.duration = PowerInfusionDuration;
        this.automaticConstraints = true;
        this.automaticStackingConstraints = true;
        this.mask = rawr.models.mage.StandardEffect.PowerInfusion;
        this.name = "Power Infusion";
        this.standardEffect = rawr.models.mage.StandardEffect.PowerInfusion;
        this.color = new Color(255, 255, 0, 255);
    }};
    EffectCooldown cachedEffectPotionOfSpeed = new EffectCooldown() {{
        this.cooldown = Float.POSITIVE_INFINITY;
        this.duration = 15.0f;
        this.maximumDuration = 15.0f;
        this.automaticStackingConstraints = true;
        this.mask = rawr.models.mage.StandardEffect.PotionOfSpeed;
        this.name = "Potion of Speed";
        this.standardEffect = rawr.models.mage.StandardEffect.PotionOfSpeed;
        this.color = new Color(0xFF, 0xFA, 0xCD, 0xFF); //LemonChiffon
    }};
    EffectCooldown cachedEffectArcanePower = new EffectCooldown() {{
        this.automaticConstraints = true;
        this.automaticStackingConstraints = true;
        this.mask = rawr.models.mage.StandardEffect.ArcanePower;
        this.name = "Arcane Power";
        this.standardEffect = rawr.models.mage.StandardEffect.ArcanePower;
        this.color = new Color(0xF0, 0xFF, 0xFF, 0xFF); //Azure
    }};
    EffectCooldown cachedEffectCombustion = new EffectCooldown() {{
        this.cooldown = CombustionCooldown;
        this.mask = rawr.models.mage.StandardEffect.Combustion;
        this.name = "Combustion";
        this.standardEffect = rawr.models.mage.StandardEffect.Combustion;
        this.color = new Color(255, 69, 0, 255);
    }};
    EffectCooldown cachedEffectPotionOfWildMagic = new EffectCooldown() {{
        this.cooldown = Float.POSITIVE_INFINITY;
        this.duration = 15.0f;
        this.maximumDuration = 15.0f;
        this.automaticStackingConstraints = true;
        this.mask = rawr.models.mage.StandardEffect.PotionOfWildMagic;
        this.name = "Potion of Wild Magic";
        this.standardEffect = rawr.models.mage.StandardEffect.PotionOfWildMagic;
        this.color = new Color(128, 0, 128, 255);
    }};
    EffectCooldown cachedEffectBerserking = new EffectCooldown() {{
        this.cooldown = 180.0f;
        this.duration = 10.0f;
        this.automaticConstraints = true;
        this.automaticStackingConstraints = true;
        this.mask = rawr.models.mage.StandardEffect.Berserking;
        this.name = "Berserking";
        this.standardEffect = rawr.models.mage.StandardEffect.Berserking;
        this.color = new Color(0xA5, 0x2A, 0x2A, 0xFF); //Brown
    }};
    EffectCooldown cachedEffectFlameCap = new EffectCooldown() {{
        this.cooldown = 180.0f;
        this.duration = 60.0f;
        this.automaticConstraints = true;
        this.automaticStackingConstraints = true;
        this.mask = rawr.models.mage.StandardEffect.FlameCap;
        this.name = "Flame Cap";
        this.standardEffect = rawr.models.mage.StandardEffect.FlameCap;
        this.color = new Color(255, 165, 0, 255);
    }};
    EffectCooldown cachedEffectHeroism = new EffectCooldown() {{
        this.cooldown = Float.POSITIVE_INFINITY;
        this.duration = 40.0f;
        this.automaticConstraints = true;
        this.automaticStackingConstraints = true;
        this.mask = rawr.models.mage.StandardEffect.Heroism;
        this.name = "Heroism";
        this.standardEffect = rawr.models.mage.StandardEffect.Heroism;
        this.color = new Color(0x80, 0x80, 0x00, 0xFF); //Olive
    }};
    EffectCooldown cachedEffectIcyVeins = new EffectCooldown() {{
        this.duration = 20.0f;
        this.mask = rawr.models.mage.StandardEffect.IcyVeins;
        this.name = "Icy Veins";
        this.standardEffect = rawr.models.mage.StandardEffect.IcyVeins;
        this.color = new Color(0x00, 0x00, 0x8B, 0xFF); //DarkBlue
    }};
    EffectCooldown cachedEffectMoltenFury = new EffectCooldown() {{
        this.cooldown = Float.POSITIVE_INFINITY;
        this.automaticStackingConstraints = true;
        this.mask = rawr.models.mage.StandardEffect.MoltenFury;
        this.name = "Molten Fury";
        this.standardEffect = rawr.models.mage.StandardEffect.MoltenFury;
        this.color = new Color(0xDC, 0x14, 0x3C, 0xFF); //Crimson
    }};
    EffectCooldown cachedEffectWaterElemental = new EffectCooldown() {{
        this.mask = rawr.models.mage.StandardEffect.WaterElemental;
        this.name = "Water Elemental";
        this.standardEffect = rawr.models.mage.StandardEffect.WaterElemental;
        this.color = new Color(0x00, 0x8B, 0x8B, 0xFF); //DarkCyan
    }};
    EffectCooldown cachedEffectMirrorImage = new EffectCooldown() {{
        this.cooldown = MirrorImageCooldown;
        this.duration = MirrorImageDuration;
        this.mask = rawr.models.mage.StandardEffect.MirrorImage;
        this.name = "Mirror Image";
        this.standardEffect = rawr.models.mage.StandardEffect.MirrorImage;
        this.automaticConstraints = true;
        this.automaticStackingConstraints = true;
        this.color = new Color(0xFF, 0xA0, 0x7A, 0xFF); //LightSalmon
    }};

    private static final CharacterSlot[] OptimizableSlots = new CharacterSlot[rawr.base.character.Character.OptimizableSlotCount];

    static {
        for (CharacterSlot s : CharacterSlot.values()) {
            if (s.value() >= 0 && s.value() < OptimizableSlots.length) OptimizableSlots[s.value()] = s;
        }
    }

    private void initializeEffectCooldowns() {
        if (this.cooldownList == null) {
            this.cooldownList = new ArrayList<EffectCooldown>();
        } else {
            this.cooldownList.clear();
        }

        this.evocationCooldown = (240.0f - 60.0f * this.mageTalents.getArcaneFlows());
        this.coldsnapCooldown = (8 * 60) * (1 - 0.1f * this.mageTalents.getColdAsIce());
        this.arcanePowerCooldown = 120.0f * (1 - 0.15f * this.mageTalents.getArcaneFlows());
        this.arcanePowerDuration = 15.0f + (this.mageTalents.isGlyphOfArcanePower() ? 3.0f : 0.0f);
        this.icyVeinsCooldown = 180.0f * (1 - 0.07f * this.mageTalents.getIceFloes() + (this.mageTalents.getIceFloes() == 3 ? 0.01f : 0.00f));
        this.waterElementalCooldown = (180.0f - (this.mageTalents.isGlyphOfWaterElemental() ? 30.0f : 0.0f)) * (1 - 0.1f * this.mageTalents.getColdAsIce());
        if (this.mageTalents.isGlyphOfEternalWater()) {
            this.waterElementalDuration = Float.POSITIVE_INFINITY;
        } else {
            this.waterElementalDuration = 45.0f + 5.0f * this.mageTalents.getEnduringWinter();
        }

        if (evocationAvailable) {
            EffectCooldown cooldown = newStandardEffectCooldown(cachedEffectEvocation);
            cooldown.cooldown = this.evocationDuration;
            this.cooldownList.add(cooldown);
        }
        if (powerInfusionAvailable) {
            EffectCooldown cooldown = newStandardEffectCooldown(cachedEffectPowerInfusion);
            this.cooldownList.add(cooldown);
        }
        if (potionOfSpeedAvailable) {
            EffectCooldown cooldown = newStandardEffectCooldown(cachedEffectPotionOfSpeed);
            this.cooldownList.add(cooldown);
        }
        if (arcanePowerAvailable) {
            EffectCooldown cooldown = newStandardEffectCooldown(cachedEffectArcanePower);
            cooldown.cooldown = this.arcanePowerCooldown;
            cooldown.duration = this.arcanePowerDuration;
            this.cooldownList.add(cooldown);
        }
        if (combustionAvailable) {
            EffectCooldown cooldown = newStandardEffectCooldown(cachedEffectCombustion);
            this.cooldownList.add(cooldown);
        }
        if (potionOfWildMagicAvailable) {
            EffectCooldown cooldown = newStandardEffectCooldown(cachedEffectPotionOfWildMagic);
            this.cooldownList.add(cooldown);
        }
        if (berserkingAvailable) {
            EffectCooldown cooldown = newStandardEffectCooldown(cachedEffectBerserking);
            this.cooldownList.add(cooldown);
        }
        if (flameCapAvailable) {
            EffectCooldown cooldown = newStandardEffectCooldown(cachedEffectFlameCap);
            this.cooldownList.add(cooldown);
        }
        if (heroismAvailable) {
            EffectCooldown cooldown = newStandardEffectCooldown(cachedEffectHeroism);
            this.cooldownList.add(cooldown);
        }
        if (icyVeinsAvailable) {
            EffectCooldown cooldown = newStandardEffectCooldown(cachedEffectIcyVeins);
            cooldown.cooldown = this.icyVeinsCooldown;
            cooldown.automaticStackingConstraints = cooldown.automaticConstraints = (this.mageTalents.getColdSnap() == 0);
            this.cooldownList.add(cooldown);
        }
        if (moltenFuryAvailable) {
            EffectCooldown cooldown = newStandardEffectCooldown(cachedEffectMoltenFury);
            cooldown.duration = cooldown.maximumDuration = this.calculationOptions.getMoltenFuryPercentage() * this.calculationOptions.getFightDuration();
            this.cooldownList.add(cooldown);
        }
        if (waterElementalAvailable) {
            EffectCooldown cooldown = newStandardEffectCooldown(cachedEffectWaterElemental);
            cooldown.cooldown = this.waterElementalCooldown;
            cooldown.duration = this.waterElementalDuration;
            this.cooldownList.add(cooldown);
        }
        if (mirrorImageAvailable) {
            EffectCooldown cooldown = newStandardEffectCooldown(cachedEffectMirrorImage);
            this.cooldownList.add(cooldown);
        }

        cooldownCount = standardEffectCount;
        int mask = 1 << standardEffectCount;

        int N = this.baseStats.getRawSpecialEffectDataSize();
        this.itemBasedEffectCooldownsCount = 0;
        if (this.itemBasedEffectCooldowns == null || this.itemBasedEffectCooldowns.length < N) {
            this.itemBasedEffectCooldowns = new EffectCooldown[N];
        }
        this.stackingHasteEffectCooldownsCount = 0;
        if (this.stackingHasteEffectCooldowns == null || this.stackingHasteEffectCooldowns.length < N) {
            this.stackingHasteEffectCooldowns = new EffectCooldown[N];
        }
        this.stackingNonHasteEffectCooldownsCount = 0;
        if (this.stackingNonHasteEffectCooldowns == null || this.stackingNonHasteEffectCooldowns.length < N) {
            this.stackingNonHasteEffectCooldowns = new EffectCooldown[N];
        }
        int itemBasedMask = 0;
        boolean[] hasteEffect = new boolean[1];
        boolean[] stackingEffect = new boolean[1];

        int colorIndex = 0;

        if (!this.calculationOptions.getDisableCooldowns()) {
            for (int slot = 0; slot < Character.OptimizableSlotCount; slot++) {
                ItemInstance itemInstance = this.character.get(OptimizableSlots[slot]);
                if (itemInstance != null) {
                    Item item = itemInstance.getItem();
                    if (item != null) {
                        Stats stats = item.getStats();
                        for (int j = 0; j < stats.getRawSpecialEffectDataSize(); j++) {
                            SpecialEffect effect = stats.getRawSpecialEffectData()[j];
                            if (effect.getTrigger() == Trigger.Use && isRelevantOnUseEffect(effect, hasteEffect, stackingEffect)) {
                                EffectCooldown cooldown = newEffectCooldown();
                                cooldown.standardEffect = 0;
                                cooldown.specialEffect = effect;
                                cooldown.hasteEffect = hasteEffect[0];
                                cooldown.mask = mask;
                                itemBasedMask |= mask;
                                mask <<= 1;
                                cooldownCount++;
                                cooldown.itemBased = true;
                                cooldown.name = item.getName();
                                cooldown.cooldown = effect.getCooldown();
                                cooldown.duration = effect.getDuration();
                                cooldown.automaticConstraints = true;
                                cooldown.automaticStackingConstraints = true;
                                cooldown.color = itemColors[Math.min(itemColors.length - 1, colorIndex++)];
                                this.cooldownList.add(cooldown);
                                this.itemBasedEffectCooldowns[this.itemBasedEffectCooldownsCount++] = cooldown;
                                if (stackingEffect[0]) {
                                    if (hasteEffect[0]) {
                                        this.stackingHasteEffectCooldowns[this.stackingHasteEffectCooldownsCount++] = cooldown;
                                    } else {
                                        this.stackingNonHasteEffectCooldowns[this.stackingNonHasteEffectCooldownsCount++] = cooldown;
                                    }
                                }
                            }
                        }
                    }
                    Enchant enchant = itemInstance.getEnchant();
                    if (enchant != null) {
                        Stats stats = enchant.stats;
                        for (int j = 0; j < stats.getRawSpecialEffectDataSize(); j++) {
                            SpecialEffect effect = stats.getRawSpecialEffectData()[j];
                            if (effect.getTrigger() == Trigger.Use && isRelevantOnUseEffect(effect, hasteEffect, stackingEffect)) {
                                EffectCooldown cooldown = newEffectCooldown();
                                cooldown.standardEffect = 0;
                                cooldown.specialEffect = effect;
                                cooldown.mask = mask;
                                cooldown.hasteEffect = hasteEffect[0];
                                itemBasedMask |= mask;
                                mask <<= 1;
                                cooldownCount++;
                                cooldown.itemBased = true;
                                cooldown.name = enchant.name;
                                cooldown.cooldown = effect.getCooldown();
                                cooldown.duration = effect.getDuration();
                                cooldown.automaticConstraints = true;
                                cooldown.automaticStackingConstraints = true;
                                cooldown.color = itemColors[Math.min(itemColors.length - 1, colorIndex++)];
                                this.cooldownList.add(cooldown);
                                this.itemBasedEffectCooldowns[this.itemBasedEffectCooldownsCount++] = cooldown;
                                if (stackingEffect[0]) {
                                    if (hasteEffect[0]) {
                                        this.stackingHasteEffectCooldowns[this.stackingHasteEffectCooldownsCount++] = cooldown;
                                    } else {
                                        this.stackingNonHasteEffectCooldowns[this.stackingNonHasteEffectCooldownsCount++] = cooldown;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (manaGemEffectAvailable) {
            for (SpecialEffect effect : this.baseStats.specialEffects(e -> e.getTrigger() == Trigger.ManaGem)) {
                EffectCooldown cooldown = newEffectCooldown();
                cooldown.standardEffect = 0;
                cooldown.specialEffect = effect;
                cooldown.mask = rawr.models.mage.StandardEffect.ManaGemEffect;
                cooldown.hasteEffect = false;
                cooldown.itemBased = true;
                cooldown.name = "Mana Gem Effect";
                cooldown.cooldown = 120f;
                cooldown.duration = effect.getDuration();
                cooldown.maximumDuration = (float) maximizeEffectDuration(this.calculationOptions.getFightDuration(), effect.getDuration(), 120);
                cooldown.automaticConstraints = false;
                cooldown.automaticStackingConstraints = true;
                cooldown.color = new Color(0x00, 0x64, 0x00, 0xFF); //DarkGreen
                this.cooldownList.add(cooldown);
                this.manaGemEffectDuration = effect.getDuration();
            }
        }
        {
            this.manaGemEffectDuration = 0;
        }

        if (this.effectCooldown == null) {
            this.effectCooldown = new LinkedHashMap<Integer, EffectCooldown>(this.cooldownList.size());
        } else {
            this.effectCooldown.clear();
        }
        availableCooldownMask = 0;
        for (EffectCooldown cooldown : this.cooldownList) {
            this.effectCooldown.put(cooldown.mask, cooldown);
            if (cooldown.standardEffect != rawr.models.mage.StandardEffect.Evocation) {
                availableCooldownMask |= cooldown.mask;
            }
        }

        if (effectExclusionList == null) {
            effectExclusionList = new int[]{
                    (StandardEffect.ArcanePower | StandardEffect.PowerInfusion),
                    (StandardEffect.PotionOfSpeed | StandardEffect.PotionOfWildMagic),
                    itemBasedMask
            };
        } else {
            effectExclusionList[2] = itemBasedMask;
        }
    }

    private void calculateBaseStateStats() {
        int[] talentData = this.mageTalents.getData();
        int arcane = 0;
        for (int i = 0; i <= 29; i++) {
            arcane += talentData[i];
        }
        int fire = 0;
        for (int i = 30; i <= 57; i++) {
            fire += talentData[i];
        }
        int frost = 0;
        for (int i = 58; i <= 85; i++) {
            frost += talentData[i];
        }
        this.maxTalents = Math.max(arcane, Math.max(fire, frost));

        Stats baseStats = this.baseStats;
        this.baseSpellHit = baseStats.getHitRating() * this.calculationOptions.getLevelScalingFactor() / 800f + baseStats.getSpellHit() + 0.01f * this.mageTalents.getPrecision();

        int targetLevel = this.calculationOptions.getTargetLevel();
        int playerLevel = this.calculationOptions.getPlayerLevel();

        float hitRate = ((targetLevel <= playerLevel + 2) ? (0.96f - (targetLevel - playerLevel) * 0.01f) : (0.94f - (targetLevel - playerLevel - 2) * 0.11f)) + this.baseSpellHit;
        this.rawArcaneHitRate = hitRate + 0.01f * this.mageTalents.getArcaneFocus();
        this.rawFireHitRate = hitRate;
        this.rawFrostHitRate = hitRate;
        hitRate = Math.min(Spell.MaxHitRate, hitRate);

        this.baseArcaneHitRate = Math.min(Spell.MaxHitRate, this.rawArcaneHitRate);
        this.baseFireHitRate = hitRate;
        this.baseFrostHitRate = hitRate;
        this.baseNatureHitRate = hitRate;
        this.baseShadowHitRate = hitRate;
        this.baseFrostFireHitRate = hitRate;
        this.baseHolyHitRate = hitRate;

        float threatFactor = (1 + baseStats.getThreatIncreaseMultiplier()) * (1 - baseStats.getThreatReductionMultiplier());

        this.arcaneThreatMultiplier = threatFactor * (1 - this.mageTalents.getArcaneSubtlety() * 0.2f);
        this.fireThreatMultiplier = threatFactor * (1 - this.mageTalents.getBurningSoul() * 0.1f);
        this.frostThreatMultiplier = threatFactor * (1 - ((this.mageTalents.getFrostChanneling() > 0) ? (0.01f + 0.03f * this.mageTalents.getFrostChanneling()) : 0f));
        this.frostFireThreatMultiplier = threatFactor * Math.min(1 - this.mageTalents.getBurningSoul() * 0.05f, 1 - ((this.mageTalents.getFrostChanneling() > 0) ? (0.01f + 0.03f * this.mageTalents.getFrostChanneling()) : 0f));
        this.natureThreatMultiplier = threatFactor;
        this.shadowThreatMultiplier = threatFactor;
        this.holyThreatMultiplier = threatFactor;

        float baseSpellModifier = (1 + 0.01f * this.mageTalents.getArcaneInstability()) * (1 + 0.01f * this.mageTalents.getPlayingWithFire()) * (1 + baseStats.getBonusDamageMultiplier()) * this.calculationOptions.getEffectDamageMultiplier();
        if (this.calculationOptions.getBeta()) {
            baseSpellModifier *= (1 + (Math.min(51, this.maxTalents) * 0.15700000524521f) * 0.01f);
        }
        float baseAdditiveSpellModifier = 1.0f;
        this.baseSpellModifier = baseSpellModifier;
        this.baseAdditiveSpellModifier = baseAdditiveSpellModifier;
        this.baseArcaneSpellModifier = baseSpellModifier * (1 + baseStats.getBonusArcaneDamageMultiplier());
        this.baseArcaneAdditiveSpellModifier = baseAdditiveSpellModifier;
        this.baseFireSpellModifier = baseSpellModifier * (1 + baseStats.getBonusFireDamageMultiplier());
        this.baseFireAdditiveSpellModifier = baseAdditiveSpellModifier + 0.02f * this.mageTalents.getFirePower();
        this.baseFrostSpellModifier = baseSpellModifier * (1 + 0.02f * this.mageTalents.getPiercingIce()) * (1 + 0.01f * this.mageTalents.getArcticWinds()) * (1 + baseStats.getBonusFrostDamageMultiplier());
        this.baseFrostAdditiveSpellModifier = baseAdditiveSpellModifier;
        this.baseNatureSpellModifier = baseSpellModifier * (1 + baseStats.getBonusNatureDamageMultiplier());
        this.baseNatureAdditiveSpellModifier = baseAdditiveSpellModifier;
        this.baseShadowSpellModifier = baseSpellModifier * (1 + baseStats.getBonusShadowDamageMultiplier());
        this.baseShadowAdditiveSpellModifier = baseAdditiveSpellModifier;
        this.baseHolySpellModifier = baseSpellModifier * (1 + baseStats.getBonusHolyDamageMultiplier());
        this.baseHolyAdditiveSpellModifier = baseAdditiveSpellModifier;
        this.baseFrostFireSpellModifier = baseSpellModifier * (1 + 0.02f * this.mageTalents.getPiercingIce()) * (1 + 0.01f * this.mageTalents.getArcticWinds()) * Math.max(1 + baseStats.getBonusFireDamageMultiplier(), 1 + baseStats.getBonusFrostDamageMultiplier());
        this.baseFrostFireAdditiveSpellModifier = baseAdditiveSpellModifier + 0.02f * this.mageTalents.getFirePower();

        float spellCritPerInt = 0f;
        float spellCritBase = 0.9075f;
        float baseRegen = 0f;
        switch (playerLevel) {
            case 70:
                spellCritPerInt = 0.0125f;
                baseRegen = 0.005596f;
                break;
            case 71:
                spellCritPerInt = 0.0116f;
                baseRegen = 0.005316f;
                break;
            case 72:
                spellCritPerInt = 0.0108f;
                baseRegen = 0.005049f;
                break;
            case 73:
                spellCritPerInt = 0.0101f;
                baseRegen = 0.004796f;
                break;
            case 74:
                spellCritPerInt = 0.0093f;
                baseRegen = 0.004555f;
                break;
            case 75:
                spellCritPerInt = 0.0087f;
                baseRegen = 0.004327f;
                break;
            case 76:
                spellCritPerInt = 0.0081f;
                baseRegen = 0.004110f;
                break;
            case 77:
                spellCritPerInt = 0.0075f;
                baseRegen = 0.003903f;
                break;
            case 78:
                spellCritPerInt = 0.007f;
                baseRegen = 0.003708f;
                break;
            case 79:
                spellCritPerInt = 0.0065f;
                baseRegen = 0.003522f;
                break;
            case 80:
                spellCritPerInt = 0.006f;
                baseRegen = 0.003345f;
                break;
            case 81:
                spellCritPerInt = 0.0042f;
                baseRegen = 0.003345f;
                break;
            case 82:
                spellCritPerInt = 0.0029676f;  // 5/27/10
                baseRegen = 0.003345f;
                break;
            case 83:
                spellCritPerInt = 0.003f;
                baseRegen = 0.003345f;
                break;
            case 84:
                spellCritPerInt = 0.003f;
                baseRegen = 0.003345f;
                break;
            case 85:
                spellCritPerInt = 0.003f;
                baseRegen = 0.003345f;
                break;
        }
        float levelScalingFactor = this.calculationOptions.getLevelScalingFactor();
        float spellCrit = 0.01f * (baseStats.getIntellect() * spellCritPerInt + spellCritBase) + 0.01f * this.mageTalents.getArcaneInstability() + 0.15f * 0.02f * this.mageTalents.getArcaneConcentration() * this.mageTalents.getArcanePotency() + baseStats.getCritRating() / 1400f * levelScalingFactor + baseStats.getSpellCrit() + baseStats.getSpellCritOnTarget() + this.mageTalents.getFocusMagic() * 0.03f * (1 - (float) Math.pow(1 - this.calculationOptions.getFocusMagicTargetCritRate(), 10.0)) + 0.01f * this.mageTalents.getPyromaniac();
        if (this.calculationOptions.getBeta()) {
            if (fire > arcane && fire >= frost) {
                spellCrit += (1 + (Math.min(51, this.maxTalents) * 0.15700000524521f) * 0.01f);
            }
        }

        this.baseCritRate = spellCrit;
        this.baseArcaneCritRate = spellCrit;
        this.baseFireCritRate = spellCrit + 0.02f * this.mageTalents.getCriticalMass();
        this.baseFrostFireCritRate = spellCrit + 0.02f * this.mageTalents.getCriticalMass();
        this.baseFrostCritRate = spellCrit;
        this.baseNatureCritRate = spellCrit;
        this.baseShadowCritRate = spellCrit;
        this.baseHolyCritRate = spellCrit;

        if (!this.calculationOptions.getEffectDisableManaSources()) {
            this.spiritRegen = (0.001f + baseStats.getSpirit() * baseRegen * (float) Math.sqrt(baseStats.getIntellect())) * this.calculationOptions.getEffectRegenMultiplier();
            this.manaRegen = this.spiritRegen + baseStats.getMp5() / 5f + 15732 * this.calculationOptions.getInnervate() / this.calculationOptions.getFightDuration() + this.calculationOptions.getManaTide() * 0.24f * baseStats.getMana() / this.calculationOptions.getFightDuration() + baseStats.getManaRestoreFromMaxManaPerSecond() * baseStats.getMana();
            this.manaRegen5SR = this.spiritRegen * baseStats.getSpellCombatManaRegeneration() + baseStats.getMp5() / 5f + 15732 * this.calculationOptions.getInnervate() / this.calculationOptions.getFightDuration() + this.calculationOptions.getManaTide() * 0.24f * baseStats.getMana() / this.calculationOptions.getFightDuration() + baseStats.getManaRestoreFromMaxManaPerSecond() * baseStats.getMana();
        } else {
            this.spiritRegen = 0;
            this.manaRegen = 0;
            this.manaRegen5SR = 0;
        }
        this.healthRegen = 0.0312f * baseStats.getSpirit() + baseStats.getHp5() / 5f;
        this.healthRegenCombat = baseStats.getHp5() / 5f;
        if (playerLevel < 75) {
            this.manaRegenDrinking = this.manaRegen + 240f;
            this.healthRegenEating = this.healthRegen + 250f;
        } else if (playerLevel < 80) {
            this.manaRegenDrinking = this.manaRegen + 306f;
            this.healthRegenEating = this.healthRegen + 440f;
        } else {
            this.manaRegenDrinking = this.manaRegen + 640f;
            this.healthRegenEating = this.healthRegen + 750f;
        }
        this.meleeMitigation = (1 - 1 / (1 + 0.1f * baseStats.getArmor() / (8.5f * (targetLevel + 4.5f * (targetLevel - 59)) + 40)));
        this.defense = 5 * playerLevel + baseStats.getDefenseRating() / 4.918498039f; // this is for level 80 only
        int molten = (armor == "Molten Armor") ? 1 : 0;
        float resilienceFactor = 2875f;
        this.physicalCritReduction = (0.04f * (this.defense - 5 * this.calculationOptions.getPlayerLevel()) / 100 + baseStats.getResilience() / resilienceFactor * levelScalingFactor + molten * 0.05f);
        this.spellCritReduction = (baseStats.getResilience() / resilienceFactor * levelScalingFactor + molten * 0.05f);
        this.critDamageReduction = (baseStats.getResilience() / resilienceFactor * 2.2f * levelScalingFactor);
        this.damageTakenReduction = baseStats.getResilience() / resilienceFactor * levelScalingFactor;
        if (this.calculationOptions.getPVP()) {
            this.damageTakenReduction *= 2f;
        }
        this.dodge = 0.043545f + 0.01f / (0.006650f + 0.953f / ((0.04f * (this.defense - 5 * playerLevel)) / 100f + baseStats.getDodgeRating() / 1200 * levelScalingFactor + (baseStats.getAgility() - 46f) * 0.0195f));

        this.igniteFactor = (1f - 0.02f * (float) Math.max(0, targetLevel - playerLevel)) /* partial resist */ * 0.08f * this.mageTalents.getIgnite();

        float mult = (1.5f * (1 + baseStats.getBonusSpellCritMultiplier()) - 1);
        float baseAddMult = (1 + 0.25f * this.mageTalents.getSpellPower() + 0.1f * this.mageTalents.getBurnout() + baseStats.getCritBonusDamage());
        if (this.calculationOptions.getBeta()) {
            if (frost > arcane && frost > fire) {
                spellCrit += (1 + (Math.min(51, this.maxTalents) * 0.35299998521805f) * 0.01f);
            }
        }
        this.baseArcaneCritBonus = (1 + mult * baseAddMult);
        this.baseFireCritBonus = (1 + mult * baseAddMult) * (1 + this.igniteFactor);
        this.baseFrostCritBonus = (1 + mult * (baseAddMult + this.mageTalents.getIceShards() / 3.0f));
        this.baseFrostFireCritBonus = (1 + mult * (baseAddMult + this.mageTalents.getIceShards() / 3.0f)) * (1 + this.igniteFactor);
        this.baseNatureCritBonus =
                this.baseShadowCritBonus =
                        this.baseHolyCritBonus = (1 + mult * baseAddMult); // unknown if affected by burnout

        float combustionCritBonus = 0.5f;

        this.combustionFireCritBonus = (1 + (1.5f * (1 + baseStats.getBonusSpellCritMultiplier()) - 1) * (1 + combustionCritBonus + 0.25f * this.mageTalents.getSpellPower() + 0.1f * this.mageTalents.getBurnout() + baseStats.getCritBonusDamage())) * (1 + this.igniteFactor);
        this.combustionFrostFireCritBonus = (1 + (1.5f * (1 + baseStats.getBonusSpellCritMultiplier()) - 1) * (1 + combustionCritBonus + this.mageTalents.getIceShards() / 3.0f + 0.25f * this.mageTalents.getSpellPower() + 0.1f * this.mageTalents.getBurnout() + baseStats.getCritBonusDamage())) * (1 + this.igniteFactor);

        this.castingSpeedMultiplier = (1f + baseStats.getSpellHaste()) * (1f + 0.02f * this.mageTalents.getNetherwindPresence()) * this.calculationOptions.getEffectHasteMultiplier();
        if (this.calculationOptions.getBeta()) {
            if (arcane >= fire && arcane >= frost) {
                this.castingSpeedMultiplier *= (1 + (Math.min(51, this.maxTalents) * 0.15700000524521f) * 0.01f);
            }
        }
        this.baseCastingSpeed = (1 + baseStats.getHasteRating() / 1000f * levelScalingFactor) * this.castingSpeedMultiplier;
        this.baseGlobalCooldown = Math.max(Spell.GlobalCooldownLimit, 1.5f / this.baseCastingSpeed);

        this.incomingDamageAmpMelee = (1 - 0.02f * this.mageTalents.getPrismaticCloak()) * (1 - 0.01f * this.mageTalents.getArcticWinds()) * (1 - this.meleeMitigation) * (1 - this.dodge) * (1 - this.damageTakenReduction);
        this.incomingDamageAmpPhysical = (1 - 0.02f * this.mageTalents.getPrismaticCloak()) * (1 - 0.01f * this.mageTalents.getArcticWinds()) * (1 - this.meleeMitigation) * (1 - this.damageTakenReduction);
        this.incomingDamageAmpArcane = (1 - 0.02f * this.mageTalents.getPrismaticCloak()) * (1 + 0.01f * this.mageTalents.getPlayingWithFire()) * (1 - 0.02f * this.mageTalents.getFrozenCore()) * (1 - StatConversion.getAverageResistance(targetLevel, playerLevel, baseStats.getArcaneResistance(), 0)) * (1 - this.damageTakenReduction);
        this.incomingDamageAmpFire = (1 - 0.02f * this.mageTalents.getPrismaticCloak()) * (1 + 0.01f * this.mageTalents.getPlayingWithFire()) * (1 - 0.02f * this.mageTalents.getFrozenCore()) * (1 - StatConversion.getAverageResistance(targetLevel, playerLevel, baseStats.getFireResistance(), 0)) * (1 - this.damageTakenReduction);
        this.incomingDamageAmpFrost = (1 - 0.02f * this.mageTalents.getPrismaticCloak()) * (1 + 0.01f * this.mageTalents.getPlayingWithFire()) * (1 - 0.02f * this.mageTalents.getFrozenCore()) * (1 - StatConversion.getAverageResistance(targetLevel, playerLevel, baseStats.getFrostResistance(), 0)) * (1 - this.damageTakenReduction);
        this.incomingDamageAmpNature = (1 - 0.02f * this.mageTalents.getPrismaticCloak()) * (1 + 0.01f * this.mageTalents.getPlayingWithFire()) * (1 - 0.02f * this.mageTalents.getFrozenCore()) * (1 - StatConversion.getAverageResistance(targetLevel, playerLevel, baseStats.getNatureResistance(), 0)) * (1 - this.damageTakenReduction);
        this.incomingDamageAmpShadow = (1 - 0.02f * this.mageTalents.getPrismaticCloak()) * (1 + 0.01f * this.mageTalents.getPlayingWithFire()) * (1 - 0.02f * this.mageTalents.getFrozenCore()) * (1 - StatConversion.getAverageResistance(targetLevel, playerLevel, baseStats.getShadowResistance(), 0)) * (1 - this.damageTakenReduction);
        this.incomingDamageAmpHoly = (1 - 0.02f * this.mageTalents.getPrismaticCloak()) * (1 + 0.01f * this.mageTalents.getPlayingWithFire()) * (1 - 0.02f * this.mageTalents.getFrozenCore()) * (1 - this.damageTakenReduction);

        this.incomingDamageDpsMelee = this.incomingDamageAmpMelee * (this.calculationOptions.getMeleeDps() * (1 + Math.max(0, this.calculationOptions.getMeleeCrit() / 100.0f - this.physicalCritReduction) * (2 * (1 - this.critDamageReduction) - 1)) + this.calculationOptions.getMeleeDot());
        this.incomingDamageDpsPhysical = this.incomingDamageAmpPhysical * (this.calculationOptions.getPhysicalDps() * (1 + Math.max(0, this.calculationOptions.getPhysicalCrit() / 100.0f - this.physicalCritReduction) * (2 * (1 - this.critDamageReduction) - 1)) + this.calculationOptions.getPhysicalDot());
        this.incomingDamageDpsArcane = this.incomingDamageAmpArcane * (this.calculationOptions.getArcaneDps() * (1 + Math.max(0, this.calculationOptions.getArcaneCrit() / 100.0f - this.spellCritReduction) * (2 * (1 - this.critDamageReduction) - 1)) + this.calculationOptions.getArcaneDot());
        this.incomingDamageDpsFire = this.incomingDamageAmpFire * (this.calculationOptions.getFireDps() * (1 + Math.max(0, this.calculationOptions.getFireCrit() / 100.0f - this.spellCritReduction) * (2 * (1 - this.critDamageReduction) - 1)) + this.calculationOptions.getFireDot());
        this.incomingDamageDpsFrost = this.incomingDamageAmpFrost * (this.calculationOptions.getFrostDps() * (1 + Math.max(0, this.calculationOptions.getFrostCrit() / 100.0f - this.spellCritReduction) * (2 * (1 - this.critDamageReduction) - 1)) + this.calculationOptions.getFrostDot());
        this.incomingDamageDpsNature = this.incomingDamageAmpNature * (this.calculationOptions.getNatureDps() * (1 + Math.max(0, this.calculationOptions.getNatureCrit() / 100.0f - this.spellCritReduction) * (2 * (1 - this.critDamageReduction) - 1)) + this.calculationOptions.getNatureDot());
        this.incomingDamageDpsShadow = this.incomingDamageAmpShadow * (this.calculationOptions.getShadowDps() * (1 + Math.max(0, this.calculationOptions.getShadowCrit() / 100.0f - this.spellCritReduction) * (2 * (1 - this.critDamageReduction) - 1)) + this.calculationOptions.getShadowDot());
        this.incomingDamageDpsHoly = this.incomingDamageAmpHoly * (this.calculationOptions.getHolyDps() * (1 + Math.max(0, this.calculationOptions.getHolyCrit() / 100.0f - this.spellCritReduction) * (2 * (1 - this.critDamageReduction) - 1)) + this.calculationOptions.getHolyDot());

        this.incomingDamageDps = this.incomingDamageDpsMelee + this.incomingDamageDpsPhysical + this.incomingDamageDpsArcane + this.incomingDamageDpsFire + this.incomingDamageDpsFrost + this.incomingDamageDpsShadow + this.incomingDamageDpsNature + this.incomingDamageDpsHoly;
        if (this.calculationOptions.getAbsorptionPerSecond() > this.incomingDamageDps) {
            this.incomingDamageDps = 0.0f;
        } else {
            this.incomingDamageDps -= this.calculationOptions.getAbsorptionPerSecond();
        }

        this.baseArcaneSpellPower = baseStats.getSpellArcaneDamageRating() + baseStats.getSpellPower();
        this.baseFireSpellPower = baseStats.getSpellFireDamageRating() + baseStats.getSpellPower();
        this.baseFrostSpellPower = baseStats.getSpellFrostDamageRating() + baseStats.getSpellPower();
        this.baseNatureSpellPower = baseStats.getSpellNatureDamageRating() + baseStats.getSpellPower();
        this.baseShadowSpellPower = baseStats.getSpellShadowDamageRating() + baseStats.getSpellPower();
        this.baseHolySpellPower = baseStats.getSpellPower();

        this.mastery = baseStats.getMasteryRating() / 14 * levelScalingFactor;
        this.manaAdeptBonus = 0.0f;
        if (this.calculationOptions.getBeta()) {
            if (arcane >= fire && arcane >= frost) {
                this.manaAdeptBonus = (0.23600000143051f * Math.min(51, arcane) + 1.5f * this.mastery) * 0.01f; // 0.12036
                needsQuadratic = true;
                needsSolutionVariables = true;
            }
        }
    }

    private void initializeSpellTemplates() {
        if (this.waterboltTemplate != null) this.waterboltTemplate.setDirty(true);
        if (this.mirrorImageTemplate != null) this.mirrorImageTemplate.setDirty(true);
        if (this.fireBlastTemplate != null) this.fireBlastTemplate.setDirty(true);
        if (this.lightningBoltTemplate != null) this.lightningBoltTemplate.setDirty(true);
        if (this.thunderBoltTemplate != null) this.thunderBoltTemplate.setDirty(true);
        if (this.lightweaveBoltTemplate != null) this.lightweaveBoltTemplate.setDirty(true);
        if (this.arcaneBoltTemplate != null) this.arcaneBoltTemplate.setDirty(true);
        if (this.pendulumOfTelluricCurrentsTemplate != null) this.pendulumOfTelluricCurrentsTemplate.setDirty(true);
        if (this.frostboltTemplate != null) this.frostboltTemplate.setDirty(true);
        if (this.frostfireBoltTemplate != null) this.frostfireBoltTemplate.setDirty(true);
        if (this.arcaneMissilesTemplate != null) this.arcaneMissilesTemplate.setDirty(true);
        if (this.fireballTemplate != null) this.fireballTemplate.setDirty(true);
        if (this.pyroblastTemplate != null) this.pyroblastTemplate.setDirty(true);
        if (this.scorchTemplate != null) this.scorchTemplate.setDirty(true);
        if (this.arcaneBarrageTemplate != null) this.arcaneBarrageTemplate.setDirty(true);
        if (this.deepFreezeTemplate != null) this.deepFreezeTemplate.setDirty(true);
        if (this.arcaneBlastTemplate != null) this.arcaneBlastTemplate.setDirty(true);
        if (this.iceLanceTemplate != null) this.iceLanceTemplate.setDirty(true);
        if (this.arcaneExplosionTemplate != null) this.arcaneExplosionTemplate.setDirty(true);
        if (this.flamestrikeTemplate != null) this.flamestrikeTemplate.setDirty(true);
        if (this.blizzardTemplate != null) this.blizzardTemplate.setDirty(true);
        if (this.blastWaveTemplate != null) this.blastWaveTemplate.setDirty(true);
        if (this.dragonsBreathTemplate != null) this.dragonsBreathTemplate.setDirty(true);
        if (this.coneOfColdTemplate != null) this.coneOfColdTemplate.setDirty(true);
        if (this.slowTemplate != null) this.slowTemplate.setDirty(true);
        if (this.livingBombTemplate != null) this.livingBombTemplate.setDirty(true);
        if (this.fireWardTemplate != null) this.fireWardTemplate.setDirty(true);
        if (this.frostWardTemplate != null) this.frostWardTemplate.setDirty(true);
        if (this.conjureManaGemTemplate != null) this.conjureManaGemTemplate.setDirty(true);
        if (this.arcaneDamageTemplate != null) this.arcaneDamageTemplate.setDirty(true);
        if (this.fireDamageTemplate != null) this.fireDamageTemplate.setDirty(true);
        if (this.frostDamageTemplate != null) this.frostDamageTemplate.setDirty(true);
        if (this.shadowDamageTemplate != null) this.shadowDamageTemplate.setDirty(true);
        if (this.natureDamageTemplate != null) this.natureDamageTemplate.setDirty(true);
        if (this.holyDamageTemplate != null) this.holyDamageTemplate.setDirty(true);
        if (this.valkyrDamageTemplate != null) this.valkyrDamageTemplate.setDirty(true);
    }

    // Construct Problem
    private void addSegmentTicks(List<Double> ticks, double cooldownDuration) {
        for (int i = 0; i * 0.5 * cooldownDuration < this.calculationOptions.getFightDuration(); i++) {
            ticks.add(i * 0.5 * cooldownDuration);
        }
    }

    private void addEffectTicks(List<Double> ticks, double cooldownDuration, double effectDuration) {
        for (int i = 0; i * cooldownDuration + effectDuration < this.calculationOptions.getFightDuration(); i++) {
            ticks.add(i * cooldownDuration + effectDuration);
            if (i * cooldownDuration + effectDuration > this.calculationOptions.getFightDuration() - effectDuration) {
                ticks.add((double) this.calculationOptions.getFightDuration() - effectDuration);
            }
        }
    }

    private void constructProblem() {
        Stats baseStats = this.baseStats;

        constructSegments();

        if (requiresMIP) {
            segmentColumn = new int[this.segmentList.size() + 1];
        }

        this.startingMana = Math.min(baseStats.getMana(), this.baseState.getManaRegenDrinking() * this.calculationOptions.getDrinkingTime());
        double maxDrinkingTime = Math.min(30, (baseStats.getMana() - this.startingMana) / this.baseState.getManaRegenDrinking());
        boolean drinkingEnabled = (maxDrinkingTime > 0.000001);

        needsTimeExtension = false;
        boolean afterFightRegen = this.calculationOptions.getFarmingMode();
        conjureManaGem = this.calculationOptions.getManaGemEnabled() && this.calculationOptions.getFightDuration() > 500.0f;

        minimizeTime = false;
        if (this.calculationOptions.getTargetDamage() > 0) {
            if (!needsQuadratic) {
                minimizeTime = true;
            }
            needsTimeExtension = true;
        }

        restrictManaUse = false;
        if (segmentCooldowns || segmentMana) restrictManaUse = true;
        if (this.calculationOptions.getUnlimitedMana()) {
            restrictManaUse = false;
            integralMana = false;
            segmentMana = false;
        }
        segmentNonCooldowns = false;
        if (segmentCooldowns) {
            if (restrictManaUse) segmentNonCooldowns = true;
            if (restrictThreat) segmentNonCooldowns = true;
        }

        dpsTime = this.calculationOptions.getDpsTime();
        float silenceTime = this.calculationOptions.getEffectShadowSilenceFrequency() * this.calculationOptions.getEffectShadowSilenceDuration() * Math.max(1 - baseStats.getShadowResistance() / this.calculationOptions.getTargetLevel() * 0.15f, 0.25f);
        if (1 - silenceTime < dpsTime) dpsTime = 1 - silenceTime;
        if (this.calculationOptions.getMovementFrequency() > 0) {
            float movementShare = this.calculationOptions.getMovementDuration() / this.calculationOptions.getMovementFrequency() / (1 + baseStats.getMovementSpeed());
            dpsTime -= movementShare;
        }

        if (segmentMana) {
            if (segmentCooldowns) {
                manaSegments = 2;
            } else {
                manaSegments = (int) Math.ceil(this.calculationOptions.getFightDuration() / this.evocationCooldown) + 1;
            }
        } else {
            manaSegments = 1;
        }

        int rowCount = constructRows(minimizeTime, drinkingEnabled, needsTimeExtension, afterFightRegen);

        if (lp == null) {
            lp = new SolverLP();
        }
        lp.initialize(this.arraySet, rowCount, 9 + (12 + (this.calculationOptions.getEnableHastedEvocation() ? 6 : 0) + spellList.size() * stateList.size() * (1 + (this.calculationOptions.getUseFireWard() ? 1 : 0) + (this.calculationOptions.getUseFrostWard() ? 1 : 0))) * manaSegments * this.segmentList.size(), this, this.segmentList.size());

        setCalculationReuseReferences();
        addWardStates();

        if (needsSolutionVariables) {
            this.solutionVariable = new ArrayList<SolutionVariable>();
        }

        lp.beginUnsafe(lp.getArraySet().rowScale, lp.getArraySet().columnScale, lp.getArraySet().cost, lp.getArraySet().sparseMatrixData, lp.getArraySet().sparseMatrixValue, lp.getArraySet().sparseMatrixRow, lp.getArraySet().sparseMatrixCol);

        // Set LP Scaling
        lp.setRowScaleUnsafe(rowManaRegen, ManaRegenLPScaling);
        lp.setRowScaleUnsafe(rowManaGem, 40.0);
        lp.setRowScaleUnsafe(rowPotion, 40.0);
        lp.setRowScaleUnsafe(rowManaGemMax, 40.0);
        lp.setRowScaleUnsafe(rowManaPotion, 40.0);
        lp.setRowScaleUnsafe(rowCombustion, 10.0);
        lp.setRowScaleUnsafe(rowHeroismCombustion, 10.0);
        lp.setRowScaleUnsafe(rowMoltenFuryCombustion, 10.0);
        lp.setRowScaleUnsafe(rowThreat, 0.001);
        lp.setRowScaleUnsafe(rowCount, 0.05);
        if (restrictManaUse) {
            for (int ss = 0; ss < manaSegments * this.segmentList.size() - 1; ss++) {
                lp.setRowScaleUnsafe(rowSegmentManaOverflow + ss, ManaRegenLPScaling);
                lp.setRowScaleUnsafe(rowSegmentManaUnderflow + ss, ManaRegenLPScaling);
            }
        }
        if (restrictThreat) {
            for (int ss = 0; ss < this.segmentList.size() - 1; ss++) {
                lp.setRowScaleUnsafe(rowSegmentThreat + ss, 0.001);
            }
        }

        float threatFactor = (1 + baseStats.getThreatIncreaseMultiplier()) * (1 - baseStats.getThreatReductionMultiplier());

        constructIdleRegen();
        constructWand();
        constructEvocation(baseStats, threatFactor);
        constructManaPotion(baseStats, threatFactor);
        constructManaGem(baseStats, threatFactor);
        constructSummonWaterElemental();
        constructSummonMirrorImage();
        constructDrinking(maxDrinkingTime, drinkingEnabled);
        constructTimeExtension();
        constructAfterFightRegen(afterFightRegen);
        constructManaOverflow();
        constructConjureManaGem();
        constructSpells();

        lp.endColumnConstruction();
        setProblemRHS();

        lp.endUnsafe();
    }

    private void constructSpells() {
        int column = 0;
        if (useIncrementalOptimizations) {
            int lastSegment = -1;
            for (int index = 0; index < this.calculationOptions.getIncrementalSetStateIndexes().length; index++) {
                if (this.calculationOptions.getIncrementalSetVariableType()[index] == VariableType.Spell) {
                    for (int buffset = 0; buffset < stateList.size(); buffset++) {
                        CastingState state = stateList.get(buffset);
                        if ((state.getEffects() & StandardEffect.NonItemBasedMask) == this.calculationOptions.getIncrementalSetStateIndexes()[index]) {
                            if (this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(this.calculationOptions.getIncrementalSetSegments()[index]), state)) {
                                float mult = segmentCooldowns ? this.calculationOptions.getDamageMultiplier(this.segmentList.get(this.calculationOptions.getIncrementalSetSegments()[index])) : 1.0f;
                                if (mult > 0) {
                                    Cycle c = state.getCycle(this.calculationOptions.getIncrementalSetSpells()[index]);
                                    int seg = this.calculationOptions.getIncrementalSetSegments()[index];
                                    int manaSegment = this.calculationOptions.getIncrementalSetManaSegment()[index];
                                    column = lp.addColumnUnsafe();
                                    if (requiresMIP) {
                                        if (seg != lastSegment) {
                                            for (; lastSegment < seg; ) {
                                                segmentColumn[++lastSegment] = column;
                                            }
                                        }
                                    }
                                    if (needsSolutionVariables) {
                                        SolutionVariable sv = new SolutionVariable();
                                        sv.state = state;
                                        sv.cycle = c;
                                        sv.segment = seg;
                                        sv.manaSegment = manaSegment;
                                        sv.type = VariableType.Spell;
                                        sv.dps = c.getDamagePerSecond();
                                        sv.mps = c.getManaPerSecond();
                                        sv.tps = c.getThreatPerSecond();
                                        this.solutionVariable.add(sv);
                                    }
                                    setSpellColumn(minimizeTime, seg, manaSegment, state, column, c, mult);
                                }
                            }
                        }
                    }
                }
            }
            if (requiresMIP) {
                for (; lastSegment < this.segmentList.size(); ) {
                    segmentColumn[++lastSegment] = column + 1;
                }
            }
        } else {
            float mfMin = this.calculationOptions.getFightDuration() * (1.0f - this.calculationOptions.getMoltenFuryPercentage()) + 0.00001f;
            float heroMin = Math.min(mfMin, this.calculationOptions.getFightDuration() - 40.0f + 0.00001f);
            int firstMoltenFurySegment = findIndexTimeEndAfter(mfMin);
            int firstHeroismSegment = findIndexTimeEndAfter(heroMin);

            List<Cycle> placed = new ArrayList<Cycle>();
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                if (requiresMIP) {
                    segmentColumn[seg] = lp.getColumns();
                }
                for (int buffset = 0; buffset < stateList.size(); buffset++) {
                    CastingState state = stateList.get(buffset);
                    if (this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(seg), state)) {
                        float mult = segmentCooldowns ? this.calculationOptions.getDamageMultiplier(this.segmentList.get(seg)) : 1.0f;
                        if (mult > 0) {
                            placed.clear();
                            for (int spell = 0; spell < spellList.size(); spell++) {
                                if (segmentCooldowns && moltenFuryAvailable && state.moltenFury && seg < firstMoltenFurySegment) continue;
                                if (segmentCooldowns && moltenFuryAvailable && !state.moltenFury && seg >= firstMoltenFurySegment) continue;
                                if (!segmentNonCooldowns && state == this.baseState && seg != 0) continue;
                                if (segmentCooldowns && this.calculationOptions.getHeroismControl() == 3 && state.heroism && seg < firstHeroismSegment) continue;
                                Cycle c = state.getCycle(spellList.get(spell));
                                boolean skip = false;
                                for (Cycle s2 : placed) {
                                    // it assumes that spells placed under same casting state are independent except for aoe spells
                                    if (!c.areaEffect && s2.getDamagePerSecond() >= c.getDamagePerSecond() - 0.00001 && s2.getManaPerSecond() <= c.getManaPerSecond() + 0.00001) {
                                        skip = true;
                                        break;
                                    }
                                }
                                if (!skip) {
                                    placed.add(c);
                                    for (int manaSegment = manaSegments - 1; manaSegment >= 0; manaSegment--) {
                                        column = lp.addColumnUnsafe();
                                        if (needsSolutionVariables) {
                                            SolutionVariable sv = new SolutionVariable();
                                            sv.state = state;
                                            sv.cycle = c;
                                            sv.segment = seg;
                                            sv.manaSegment = manaSegment;
                                            sv.type = VariableType.Spell;
                                            sv.dps = c.getDamagePerSecond();
                                            sv.mps = c.getManaPerSecond();
                                            sv.tps = c.getThreatPerSecond();
                                            this.solutionVariable.add(sv);
                                        }
                                        setSpellColumn(minimizeTime, seg, manaSegment, state, column, c, mult);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (requiresMIP) {
                segmentColumn[this.segmentList.size()] = column + 1;
            }
        }
    }

    private void constructConjureManaGem() {
        if (conjureManaGem) {
            int conjureSegments = (restrictManaUse) ? this.segmentList.size() : 1;
            Cycle spell = getConjureManaGemTemplate().getSpell(this.baseState).getCycle();
            this.conjureManaGemCycle = spell;
            this.maxConjureManaGem = (int) ((this.calculationOptions.getFightDuration() - 300.0f) / 360.0f) + 1;
            double mps = spell.getManaPerSecond();
            double dps = 0.0;
            double tps = spell.getThreatPerSecond();
            for (int segment = 0; segment < conjureSegments; segment++) {
                for (int manaSegment = 0; manaSegment < manaSegments; manaSegment++) {
                    int column = lp.addColumnUnsafe();
                    lp.setColumnUpperBound(column, spell.castTime * ((conjureSegments > 1) ? 1 : this.maxConjureManaGem));
                    if (needsSolutionVariables) {
                        SolutionVariable sv = new SolutionVariable();
                        sv.type = VariableType.ConjureManaGem;
                        sv.cycle = spell;
                        sv.segment = segment;
                        sv.manaSegment = manaSegment;
                        sv.state = this.baseState;
                        sv.dps = dps;
                        sv.tps = tps;
                        sv.mps = mps;
                        this.solutionVariable.add(sv);
                    }
                    lp.setElementUnsafe(rowAfterFightRegenMana, column, mps);
                    lp.setElementUnsafe(rowManaRegen, column, mps);
                    lp.setElementUnsafe(rowConjureManaGem, column, 1.0);
                    lp.setElementUnsafe(rowFightDuration, column, 1.0);
                    lp.setElementUnsafe(rowTimeExtension, column, -1.0);
                    tps = spell.getThreatPerSecond();
                    lp.setElementUnsafe(rowThreat, column, tps);
                    lp.setElementUnsafe(rowTargetDamage, column, -spell.getDamagePerSecond());
                    lp.setCostUnsafe(column, minimizeTime ? -1 : spell.getDamagePerSecond());
                    lp.setElementUnsafe(rowManaGem, column, -3.0 / spell.castTime); // one cast time gives 3 new gem uses
                    if (segmentNonCooldowns) lp.setElementUnsafe(rowSegment + segment, column, 1.0);
                    if (restrictManaUse) {
                        for (int ss = segment * manaSegments + manaSegment; ss < this.segmentList.size() * manaSegments - 1; ss++) {
                            lp.setElementUnsafe(rowSegmentManaUnderflow + ss, column, mps);
                            lp.setElementUnsafe(rowSegmentManaOverflow + ss, column, -mps);
                        }
                    }
                    if (restrictThreat) {
                        for (int ss = segment; ss < this.segmentList.size() - 1; ss++) {
                            lp.setElementUnsafe(rowSegmentThreat + ss, column, tps);
                        }
                    }
                }
            }
        } else {
            this.conjureManaGemCycle = null;
            this.maxConjureManaGem = 0;
        }
    }

    private void constructManaOverflow() {
        if (restrictManaUse) {
            if (useIncrementalOptimizations) {
                for (int index = 0; index < this.calculationOptions.getIncrementalSetStateIndexes().length; index++) {
                    if (this.calculationOptions.getIncrementalSetVariableType()[index] == VariableType.ManaOverflow) {
                        int segment = this.calculationOptions.getIncrementalSetSegments()[index];
                        int manaSegment = this.calculationOptions.getIncrementalSetManaSegment()[index];
                        setManaOverflowColumn(segment, manaSegment);
                    }
                }
            } else {
                for (int segment = 0; segment < this.segmentList.size(); segment++) {
                    for (int manaSegment = 0; manaSegment < manaSegments; manaSegment++) {
                        setManaOverflowColumn(segment, manaSegment);
                    }
                }
            }
        }
    }

    private void setManaOverflowColumn(int segment, int manaSegment) {
        if (needsSolutionVariables) {
            SolutionVariable sv = new SolutionVariable();
            sv.type = VariableType.ManaOverflow;
            sv.segment = segment;
            sv.manaSegment = manaSegment;
            this.solutionVariable.add(sv);
        }
        int column = lp.addColumnUnsafe();
        lp.setElementUnsafe(rowAfterFightRegenMana, column, 1.0);
        lp.setElementUnsafe(rowManaRegen, column, 1.0);
        for (int ss = segment * manaSegments + manaSegment; ss < this.segmentList.size() * manaSegments - 1; ss++) {
            lp.setElementUnsafe(rowSegmentManaUnderflow + ss, column, 1.0);
            lp.setElementUnsafe(rowSegmentManaOverflow + ss, column, -1.0);
        }
    }

    private void constructAfterFightRegen(boolean afterFightRegen) {
        if (afterFightRegen) {
            if (needsSolutionVariables) {
                SolutionVariable sv = new SolutionVariable();
                sv.type = VariableType.AfterFightRegen;
                this.solutionVariable.add(sv);
            }
            int column = lp.addColumnUnsafe();
            lp.setColumnUpperBound(column, this.calculationOptions.getFightDuration());
            lp.setElementUnsafe(rowFightDuration, column, 1.0);
            lp.setElementUnsafe(rowTimeExtension, column, -1.0);
            lp.setElementUnsafe(rowAfterFightRegenMana, column, -this.baseState.getManaRegenDrinking());
            lp.setCostUnsafe(column, minimizeTime ? -1 : 0);
        }
    }

    private void constructTimeExtension() {
        if (needsTimeExtension) {
            if (needsSolutionVariables) {
                SolutionVariable sv = new SolutionVariable();
                sv.type = VariableType.TimeExtension;
                this.solutionVariable.add(sv);
            }
            int column = lp.addColumnUnsafe();
            lp.setColumnUpperBound(column, this.calculationOptions.getFightDuration());
            if (!needsQuadratic) {
                lp.setElementUnsafe(rowFightDuration, column, 1.0);
            }
            lp.setElementUnsafe(rowTimeExtension, column, -1.0);
            lp.setElementUnsafe(rowEvocation, column, this.evocationDuration / this.evocationCooldown);
            lp.setElementUnsafe(rowManaGem, column, 1.0 / 120.0);
            lp.setElementUnsafe(rowPowerInfusion, column, PowerInfusionDuration / PowerInfusionCooldown);
            lp.setElementUnsafe(rowArcanePower, column, this.arcanePowerDuration / this.arcanePowerCooldown);
            lp.setElementUnsafe(rowIcyVeins, column, 20.0 / this.icyVeinsCooldown + (coldsnapAvailable ? 20.0 / this.coldsnapCooldown : 0.0));
            lp.setElementUnsafe(rowMoltenFury, column, this.calculationOptions.getMoltenFuryPercentage());
            lp.setElementUnsafe(rowFlameCap, column, 60f / 180f);
            for (int i = 0; i < this.itemBasedEffectCooldownsCount; i++) {
                EffectCooldown cooldown = this.itemBasedEffectCooldowns[i];
                lp.setElementUnsafe(cooldown.row, column, cooldown.duration / cooldown.cooldown);
            }
            lp.setElementUnsafe(rowManaGemEffect, column, this.manaGemEffectDuration / 120f);
            lp.setElementUnsafe(rowDpsTime, column, -(1 - dpsTime));
            lp.setElementUnsafe(rowAoe, column, this.calculationOptions.getAoeDuration());
            lp.setElementUnsafe(rowCombustion, column, 1.0 / CombustionCooldown);
            lp.setElementUnsafe(rowBerserking, column, 10.0 / 180.0);
        }
    }

    private void constructDrinking(double maxDrinkingTime, boolean drinkingEnabled) {
        if (drinkingEnabled) {
            double mps = -this.baseState.getManaRegenDrinking();
            double dps = 0.0f;
            double tps = 0.0f;
            if (needsSolutionVariables) {
                SolutionVariable sv = new SolutionVariable();
                sv.type = VariableType.Drinking;
                sv.dps = dps;
                sv.mps = mps;
                sv.tps = tps;
                this.solutionVariable.add(sv);
            }
            int column = lp.addColumnUnsafe();
            lp.setColumnUpperBound(column, maxDrinkingTime);
            lp.setElementUnsafe(rowAfterFightRegenMana, column, mps);
            lp.setElementUnsafe(rowManaRegen, column, mps);
            lp.setElementUnsafe(rowFightDuration, column, 1.0);
            lp.setElementUnsafe(rowTimeExtension, column, -1.0);
            lp.setCostUnsafe(column, minimizeTime ? -1 : 0);
            if (segmentNonCooldowns) lp.setElementUnsafe(rowSegment + 0, column, 1.0);
            if (restrictManaUse) {
                for (int ss = 0; ss < this.segmentList.size() * manaSegments - 1; ss++) {
                    lp.setElementUnsafe(rowSegmentManaUnderflow + ss, column, mps);
                    lp.setElementUnsafe(rowSegmentManaOverflow + ss, column, -mps);
                }
            }
        }
    }

    private void constructSummonMirrorImage() {
        if (mirrorImageAvailable) {
            int mirrorImageSegments = this.segmentList.size(); // always segment, we need it to guarantee each block has activation
            double mps = (int) (0.10 * SpellTemplate.BaseMana.get(this.calculationOptions.getPlayerLevel())) / this.baseGlobalCooldown - this.baseState.getManaRegen5SR();
            scratchStateList.clear();
            boolean found = false;
            for (int i = 0; i < stateList.size(); i++) {
                if (stateList.get(i).getEffects() == rawr.models.mage.StandardEffect.MirrorImage) {
                    scratchStateList.add(stateList.get(i));
                    found = true;
                    break;
                }
            }
            if (!found) {
                scratchStateList.add(CastingState.createNew(this, StandardEffect.MirrorImage, false, 0));
            }
            if (useIncrementalOptimizations) {
                for (int index = 0; index < this.calculationOptions.getIncrementalSetStateIndexes().length; index++) {
                    if (this.calculationOptions.getIncrementalSetVariableType()[index] == VariableType.SummonMirrorImage) {
                        int segment = this.calculationOptions.getIncrementalSetSegments()[index];
                        int manaSegment = this.calculationOptions.getIncrementalSetManaSegment()[index];
                        for (CastingState state : scratchStateList) {
                            setSummonMirrorImageColumn(mirrorImageSegments, mps, segment, manaSegment, state);
                        }
                    }
                }
            } else {
                for (int segment = 0; segment < mirrorImageSegments; segment++) {
                    for (int manaSegment = 0; manaSegment < manaSegments; manaSegment++) {
                        for (CastingState state : scratchStateList) {
                            setSummonMirrorImageColumn(mirrorImageSegments, mps, segment, manaSegment, state);
                        }
                    }
                }
            }
        }
    }

    private void setSummonMirrorImageColumn(int mirrorImageSegments, double mps, int segment, int manaSegment, CastingState state) {
        Spell mirrorImage = state.getSpell(SpellId.MirrorImage);
        double dps = mirrorImage.getDamagePerSecond();
        double tps = 0.0;
        if (needsSolutionVariables) {
            SolutionVariable sv = new SolutionVariable();
            sv.type = VariableType.SummonMirrorImage;
            sv.segment = segment;
            sv.manaSegment = manaSegment;
            sv.state = state;
            sv.dps = dps;
            sv.mps = mps;
            sv.tps = tps;
            this.solutionVariable.add(sv);
        }
        int column = lp.addColumnUnsafe();
        if (mirrorImageSegments > 1) lp.setColumnUpperBound(column, this.baseGlobalCooldown);
        lp.setElementUnsafe(rowAfterFightRegenMana, column, mps);
        lp.setElementUnsafe(rowManaRegen, column, mps);
        lp.setElementUnsafe(rowFightDuration, column, 1.0);
        lp.setElementUnsafe(rowTimeExtension, column, -1.0);
        lp.setElementUnsafe(rowSummonMirrorImage, column, -1 / this.baseGlobalCooldown);
        lp.setElementUnsafe(rowSummonMirrorImageCount, column, 1.0);
        lp.setElementUnsafe(rowMirrorImage, column, 1.0);
        lp.setCostUnsafe(column, minimizeTime ? -1 : mirrorImage.getDamagePerSecond());
        lp.setElementUnsafe(rowSegment + segment, column, 1.0);
        if (restrictManaUse) {
            for (int ss = segment * manaSegments + manaSegment; ss < this.segmentList.size() * manaSegments - 1; ss++) {
                lp.setElementUnsafe(rowSegmentManaUnderflow + ss, column, mps);
                lp.setElementUnsafe(rowSegmentManaOverflow + ss, column, -mps);
            }
        }
        if (segmentCooldowns) {
            for (SegmentConstraint constraint : rowSegmentMirrorImage) {
                if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
            }
            for (SegmentConstraint constraint : rowSegmentSummonMirrorImage) {
                if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
            }
        }
    }

    private void constructSummonWaterElemental() {
        if (waterElementalAvailable && !this.mageTalents.isGlyphOfEternalWater()) {
            int waterElementalSegments = this.segmentList.size(); // always segment, we need it to guarantee each block has activation
            double mps = (int) (0.16 * SpellTemplate.BaseMana.get(this.calculationOptions.getPlayerLevel())) / this.baseGlobalCooldown - this.baseState.getManaRegen5SR();
            scratchStateList.clear();
            boolean found = false;
            // WE = 0x100
            for (int i = 0; i < stateList.size(); i++) {
                if (stateList.get(i).getEffects() == rawr.models.mage.StandardEffect.WaterElemental) {
                    scratchStateList.add(stateList.get(i));
                    found = true;
                    break;
                }
            }
            if (!found) {
                scratchStateList.add(CastingState.createNew(this, StandardEffect.WaterElemental, false, 0));
            }
            if (useIncrementalOptimizations) {
                for (int index = 0; index < this.calculationOptions.getIncrementalSetStateIndexes().length; index++) {
                    if (this.calculationOptions.getIncrementalSetVariableType()[index] == VariableType.SummonWaterElemental) {
                        int segment = this.calculationOptions.getIncrementalSetSegments()[index];
                        int manaSegment = this.calculationOptions.getIncrementalSetManaSegment()[index];
                        for (CastingState state : scratchStateList) {
                            setSummonWaterElementalColumn(waterElementalSegments, mps, segment, manaSegment, state);
                        }
                    }
                }
            } else {
                for (int segment = 0; segment < waterElementalSegments; segment++) {
                    for (int manaSegment = 0; manaSegment < manaSegments; manaSegment++) {
                        for (CastingState state : scratchStateList) {
                            setSummonWaterElementalColumn(waterElementalSegments, mps, segment, manaSegment, state);
                        }
                    }
                }
            }
        }
    }

    private void setSummonWaterElementalColumn(int waterElementalSegments, double mps, int segment, int manaSegment, CastingState state) {
        Spell waterbolt = state.getSpell(SpellId.Waterbolt);
        double dps = waterbolt.getDamagePerSecond();
        double tps = 0.0;
        if (needsSolutionVariables) {
            SolutionVariable sv = new SolutionVariable();
            sv.type = VariableType.SummonWaterElemental;
            sv.segment = segment;
            sv.manaSegment = manaSegment;
            sv.state = state;
            sv.dps = dps;
            sv.mps = mps;
            sv.tps = tps;
            this.solutionVariable.add(sv);
        }
        int column = lp.addColumnUnsafe();
        if (waterElementalSegments > 1) lp.setColumnUpperBound(column, this.baseGlobalCooldown);
        lp.setElementUnsafe(rowAfterFightRegenMana, column, mps);
        lp.setElementUnsafe(rowManaRegen, column, mps);
        lp.setElementUnsafe(rowFightDuration, column, 1.0);
        lp.setElementUnsafe(rowTimeExtension, column, -1.0);
        if (!this.mageTalents.isGlyphOfEternalWater()) {
            lp.setElementUnsafe(rowSummonWaterElemental, column, -1 / this.baseGlobalCooldown);
            lp.setElementUnsafe(rowSummonWaterElementalCount, column, 1.0);
            lp.setElementUnsafe(rowWaterElemental, column, 1.0);
        }
        lp.setCostUnsafe(column, minimizeTime ? -1 : waterbolt.getDamagePerSecond());
        lp.setElementUnsafe(rowSegment + segment, column, 1.0);
        if (restrictManaUse) {
            for (int ss = segment; ss < this.segmentList.size() - 1; ss++) {
                lp.setElementUnsafe(rowSegmentManaUnderflow + ss, column, mps);
                lp.setElementUnsafe(rowSegmentManaOverflow + ss, column, -mps);
            }
        }
        if (segmentCooldowns) {
            for (SegmentConstraint constraint : rowSegmentWaterElemental) {
                if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
            }
            for (SegmentConstraint constraint : rowSegmentSummonWaterElemental) {
                if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
            }
        }
    }

    private void constructManaGem(Stats baseStats, float threatFactor) {
        if (this.calculationOptions.getManaGemEnabled()) {
            int manaGemSegments = (segmentCooldowns && restrictManaUse) ? this.segmentList.size() : 1;
            if (segmentCooldowns && advancedConstraintsLevel >= 3) {
                this.maxManaGem = 1 + (int) ((this.calculationOptions.getFightDuration() - 1f) / 120f);
            } else {
                this.maxManaGem = 1 + (int) ((this.calculationOptions.getFightDuration() - 30f) / 120f);
            }
            double mps = -(1 + baseStats.getBonusManaGem()) * this.manaGemValue;
            double tps = -mps * 0.5f * threatFactor;
            double dps = 0.0f;
            double upperBound;
            if (manaGemSegments > 1) {
                upperBound = 1.0;
            } else {
                if (needsTimeExtension || conjureManaGem) {
                    upperBound = this.maxManaGem;
                } else {
                    upperBound = Math.min(3, this.calculationOptions.getAverageCooldowns() ? this.calculationOptions.getFightDuration() / 120.0 : this.maxManaGem);
                }
            }
            if (useIncrementalOptimizations) {
                for (int index = 0; index < this.calculationOptions.getIncrementalSetStateIndexes().length; index++) {
                    if (this.calculationOptions.getIncrementalSetVariableType()[index] == VariableType.ManaGem) {
                        int segment = this.calculationOptions.getIncrementalSetSegments()[index];
                        int manaSegment = this.calculationOptions.getIncrementalSetManaSegment()[index];
                        setManaGemColumn(mps, tps, dps, upperBound, segment, manaSegment);
                    }
                }
            } else {
                for (int segment = 0; segment < manaGemSegments; segment++) {
                    for (int manaSegment = 0; manaSegment < manaSegments; manaSegment++) {
                        setManaGemColumn(mps, tps, dps, upperBound, segment, manaSegment);
                    }
                }
            }
        } else {
            this.maxManaGem = 0;
            this.manaGemTps = 0;
        }
    }

    private void setManaGemColumn(double mps, double tps, double dps, double upperBound, int segment, int manaSegment) {
        if (needsSolutionVariables) {
            SolutionVariable sv = new SolutionVariable();
            sv.type = VariableType.ManaGem;
            sv.segment = segment;
            sv.manaSegment = manaSegment;
            sv.dps = dps;
            sv.mps = mps;
            sv.tps = tps;
            this.solutionVariable.add(sv);
        }
        int column = lp.addColumnUnsafe();
        lp.setColumnScaleUnsafe(column, 1.0 / 40.0);
        lp.setColumnUpperBound(column, upperBound);
        lp.setElementUnsafe(rowAfterFightRegenMana, column, mps);
        lp.setElementUnsafe(rowManaRegen, column, mps);
        lp.setElementUnsafe(rowManaGem, column, 1.0);
        lp.setElementUnsafe(rowManaGemMax, column, 1.0);
        lp.setElementUnsafe(rowManaGemEffectActivation, column, -1.0);
        lp.setElementUnsafe(rowThreat, column, tps);
        this.manaGemTps = (float) tps;
        lp.setCostUnsafe(column, 0.0);
        if (segmentCooldowns) {
            for (SegmentConstraint constraint : rowSegmentManaGem) {
                if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
            }
        }
        if (restrictManaUse) {
            for (int ss = segment * manaSegments + manaSegment; ss < this.segmentList.size() * manaSegments - 1; ss++) {
                lp.setElementUnsafe(rowSegmentManaUnderflow + ss, column, mps);
                lp.setElementUnsafe(rowSegmentManaOverflow + ss, column, -mps);
            }
        }
        if (restrictThreat) {
            for (int ss = segment; ss < this.segmentList.size() - 1; ss++) {
                lp.setElementUnsafe(rowSegmentThreat + ss, column, tps);
            }
        }
    }

    private void constructManaPotion(Stats baseStats, float threatFactor) {
        if (manaPotionAvailable) {
            int manaPotionSegments = (segmentCooldowns && (potionOfWildMagicAvailable || restrictManaUse)) ? this.segmentList.size() : 1;
            double mps = -(1 + baseStats.getBonusManaPotion()) * this.manaPotionValue;
            double dps = 0;
            double tps = (1 + baseStats.getBonusManaPotion()) * this.manaPotionValue * 0.5f * threatFactor;
            if (useIncrementalOptimizations) {
                for (int index = 0; index < this.calculationOptions.getIncrementalSetStateIndexes().length; index++) {
                    if (this.calculationOptions.getIncrementalSetVariableType()[index] == VariableType.ManaPotion) {
                        int segment = this.calculationOptions.getIncrementalSetSegments()[index];
                        int manaSegment = this.calculationOptions.getIncrementalSetManaSegment()[index];
                        setManaPotionColumn(mps, dps, tps, segment, manaSegment);
                    }
                }
            } else {
                for (int segment = 0; segment < manaPotionSegments; segment++) {
                    for (int manaSegment = 0; manaSegment < manaSegments; manaSegment++) {
                        setManaPotionColumn(mps, dps, tps, segment, manaSegment);
                    }
                }
            }
        } else {
            this.manaPotionTps = 0;
        }
    }

    private void setManaPotionColumn(double mps, double dps, double tps, int segment, int manaSegment) {
        if (needsSolutionVariables) {
            SolutionVariable sv = new SolutionVariable();
            sv.type = VariableType.ManaPotion;
            sv.segment = segment;
            sv.manaSegment = manaSegment;
            sv.dps = dps;
            sv.mps = mps;
            sv.tps = tps;
            this.solutionVariable.add(sv);
        }
        int column = lp.addColumnUnsafe();
        lp.setColumnScaleUnsafe(column, 1.0 / 40.0);
        lp.setColumnUpperBound(column, 1.0);
        lp.setElementUnsafe(rowAfterFightRegenMana, column, mps);
        lp.setElementUnsafe(rowManaRegen, column, mps);
        lp.setElementUnsafe(rowPotion, column, 1.0);
        lp.setElementUnsafe(rowManaPotion, column, 1.0);
        lp.setElementUnsafe(rowThreat, column, tps);
        this.manaPotionTps = (float) tps;
        lp.setCostUnsafe(column, 0.0);
        if (restrictManaUse) {
            for (int ss = segment * manaSegments + manaSegment; ss < this.segmentList.size() * manaSegments - 1; ss++) {
                lp.setElementUnsafe(rowSegmentManaUnderflow + ss, column, mps);
                lp.setElementUnsafe(rowSegmentManaOverflow + ss, column, -mps);
            }
        }
        if (restrictThreat) {
            for (int ss = segment; ss < this.segmentList.size() - 1; ss++) {
                lp.setElementUnsafe(rowSegmentThreat + ss, column, tps);
            }
        }
    }

    private void constructEvocation(Stats baseStats, float threatFactor) {
        if (evocationAvailable) {
            CastingState evoBaseState = this.baseState;
            if (this.calculationOptions.getEnable2T10Evocation() && baseStats.getMage2T10() > 0) {
                evoBaseState = this.baseState.getTier10TwoPieceState();
            }
            int evocationSegments = (restrictManaUse) ? this.segmentList.size() : 1;
            float evocationDuration = (8f/* + baseStats.EvocationExtension*/) / evoBaseState.castingSpeed;
            this.evocationDuration = evocationDuration;
            this.evocationDurationIV = evocationDuration / 1.2f;
            this.evocationDurationHero = evocationDuration / 1.3f;
            this.evocationDurationIVHero = evocationDuration / 1.2f / 1.3f;
            if (this.calculationOptions.getBeta()) {
                this.evocationDuration = (float) Math.floor(8 / (this.evocationDuration / 4)) * this.evocationDuration / 4;
                this.evocationDurationIV = (float) Math.floor(8 / (this.evocationDurationIV / 4)) * this.evocationDurationIV / 4;
                this.evocationDurationHero = (float) Math.floor(8 / (this.evocationDurationHero / 4)) * this.evocationDurationHero / 4;
                this.evocationDurationIVHero = (float) Math.floor(8 / (this.evocationDurationIVHero / 4)) * this.evocationDurationIVHero / 4;
            }
            float evocationMana = baseStats.getMana();
            this.evocationRegen = this.baseState.getManaRegen5SR() + 0.15f * evocationMana / 2f * evoBaseState.castingSpeed;
            this.evocationRegenIV = this.baseState.getManaRegen5SR() + 0.15f * evocationMana / 2f * evoBaseState.castingSpeed * 1.2f;
            this.evocationRegenHero = this.baseState.getManaRegen5SR() + 0.15f * evocationMana / 2f * evoBaseState.castingSpeed * 1.3f;
            this.evocationRegenIVHero = this.baseState.getManaRegen5SR() + 0.15f * evocationMana / 2f * evoBaseState.castingSpeed * 1.2f * 1.3f;
            if (this.evocationRegen * evocationDuration > baseStats.getMana()) {
                evocationDuration = baseStats.getMana() / this.evocationRegen;
                this.evocationDuration = evocationDuration;
                this.evocationDurationIV = baseStats.getMana() / this.evocationRegenIV;
                this.evocationDurationHero = baseStats.getMana() / this.evocationRegenHero;
                this.evocationDurationIVHero = baseStats.getMana() / this.evocationRegenIVHero;
            }
            if (this.calculationOptions.getAverageCooldowns()) {
                this.maxEvocation = this.calculationOptions.getFightDuration() / this.evocationCooldown;
            } else if (segmentCooldowns && advancedConstraintsLevel >= 3) {
                this.maxEvocation = Math.max(1, 1 + (float) Math.floor((this.calculationOptions.getFightDuration() - evocationDuration) / this.evocationCooldown));
            } else {
                this.maxEvocation = Math.max(1, 1 + (float) Math.floor((this.calculationOptions.getFightDuration() - 90f) / this.evocationCooldown));
            }
            int mask = 0;
            if (waterElementalAvailable && this.mageTalents.isGlyphOfEternalWater()) {
                mask |= rawr.models.mage.StandardEffect.WaterElemental;
            }
            CastingState evoState = null;
            CastingState evoStateIV = null;
            CastingState evoStateHero = null;
            CastingState evoStateIVHero = null;
            if (waterElementalAvailable && this.mageTalents.isGlyphOfEternalWater()) {
                evoState = CastingState.createNew(this, StandardEffect.Evocation | mask, false, 0);
                if (this.calculationOptions.getEnableHastedEvocation()) {
                    evoStateIV = CastingState.createNew(this, StandardEffect.Evocation | StandardEffect.IcyVeins | mask, false, 0);
                    evoStateHero = CastingState.createNew(this, StandardEffect.Evocation | StandardEffect.Heroism | mask, false, 0);
                    evoStateIVHero = CastingState.createNew(this, StandardEffect.Evocation | StandardEffect.IcyVeins | StandardEffect.Heroism | mask, false, 0);
                }
            } else {
                evoState = CastingState.newRaw(this, StandardEffect.Evocation | mask);
                if (this.calculationOptions.getEnableHastedEvocation()) {
                    evoStateIV = CastingState.newRaw(this, StandardEffect.Evocation | StandardEffect.IcyVeins | mask);
                    evoStateHero = CastingState.newRaw(this, StandardEffect.Evocation | StandardEffect.Heroism | mask);
                    evoStateIVHero = CastingState.newRaw(this, StandardEffect.Evocation | StandardEffect.IcyVeins | StandardEffect.Heroism | mask);
                }
            }
            if (useIncrementalOptimizations && segmentMana) {
                for (int index = 0; index < this.calculationOptions.getIncrementalSetStateIndexes().length; index++) {
                    int segment = this.calculationOptions.getIncrementalSetSegments()[index];
                    int manaSegment = this.calculationOptions.getIncrementalSetManaSegment()[index];
                    int state = this.calculationOptions.getIncrementalSetStateIndexes()[index];
                    switch (this.calculationOptions.getIncrementalSetVariableType()[index]) {
                        case Evocation:
                            if (this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(segment), evoState)) {
                                setEvocationColumn(threatFactor, evocationSegments, evocationMana, evoState, segment, manaSegment, VariableType.Evocation, false);
                            }
                            break;
                        case EvocationHero:
                            if (state == evoStateHero.getEffects() && this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(segment), evoStateHero)) {
                                // last tick of heroism
                                setEvocationColumn(threatFactor, evocationSegments, evocationMana, evoStateHero, segment, manaSegment, VariableType.EvocationHero, true);
                            }
                            if (state == evoState.getEffects() && this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(segment), evoState)) {
                                // remainder
                                setEvocationColumn(threatFactor, evocationSegments, evocationMana, evoState, segment, manaSegment, VariableType.EvocationHero, false);
                            }
                            break;
                        case EvocationIV:
                            if (state == evoStateIV.getEffects() && this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(segment), evoStateIV)) {
                                // last tick of icy veins
                                setEvocationColumn(threatFactor, evocationSegments, evocationMana, evoStateIV, segment, manaSegment, VariableType.EvocationIV, true);
                            } else if (state == evoState.getEffects() && this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(segment), evoState)) {
                                // remainder
                                setEvocationColumn(threatFactor, evocationSegments, evocationMana, evoState, segment, manaSegment, VariableType.EvocationIV, false);
                            }
                            break;
                        case EvocationIVHero:
                            if (state == evoStateIVHero.getEffects() && this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(segment), evoStateIVHero)) {
                                // last tick of icy veins+heroism
                                setEvocationColumn(threatFactor, evocationSegments, evocationMana, evoStateIVHero, segment, manaSegment, VariableType.EvocationIVHero, true);
                            }
                            if (state == evoState.getEffects() && this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(segment), evoState)) {
                                // remainder
                                setEvocationColumn(threatFactor, evocationSegments, evocationMana, evoState, segment, manaSegment, VariableType.EvocationIVHero, false);
                            }
                            break;
                    }
                }
            } else {
                int minManaSegment = segmentMana ? 1 : 0;
                for (int segment = 0; segment < evocationSegments; segment++) {
                    for (int manaSegment = minManaSegment; manaSegment < manaSegments; manaSegment++) {
                        // base evocation
                        if (this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(segment), evoState)) {
                            setEvocationColumn(threatFactor, evocationSegments, evocationMana, evoState, segment, manaSegment, VariableType.Evocation, false);
                        }
                        if (this.calculationOptions.getEnableHastedEvocation()) {
                            if (icyVeinsAvailable) {
                                if (this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(segment), evoStateIV)) {
                                    // last tick of icy veins
                                    setEvocationColumn(threatFactor, evocationSegments, evocationMana, evoStateIV, segment, manaSegment, VariableType.EvocationIV, true);
                                }
                                if (this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(segment), evoState)) {
                                    // remainder
                                    setEvocationColumn(threatFactor, evocationSegments, evocationMana, evoState, segment, manaSegment, VariableType.EvocationIV, false);
                                }
                            }
                            if (heroismAvailable) {
                                if (this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(segment), evoStateHero)) {
                                    // last tick of heroism
                                    setEvocationColumn(threatFactor, evocationSegments, evocationMana, evoStateHero, segment, manaSegment, VariableType.EvocationHero, true);
                                }
                                if (this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(segment), evoState)) {
                                    // remainder
                                    setEvocationColumn(threatFactor, evocationSegments, evocationMana, evoState, segment, manaSegment, VariableType.EvocationHero, false);
                                }
                            }
                            if (icyVeinsAvailable && heroismAvailable) {
                                if (this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(segment), evoStateIVHero)) {
                                    // last tick of icy veins+heroism
                                    setEvocationColumn(threatFactor, evocationSegments, evocationMana, evoStateIVHero, segment, manaSegment, VariableType.EvocationIVHero, true);
                                }
                                if (this.calculationOptions.cooldownRestrictionsValid(this.segmentList.get(segment), evoState)) {
                                    // remainder
                                    setEvocationColumn(threatFactor, evocationSegments, evocationMana, evoState, segment, manaSegment, VariableType.EvocationIVHero, false);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            this.maxEvocation = 0;
            this.evocationDuration = 0;
            this.evocationDurationIV = 0;
            this.evocationDurationHero = 0;
            this.evocationDurationIVHero = 0;
            this.evocationRegen = 0;
            this.evocationRegenIV = 0;
            this.evocationRegenHero = 0;
            this.evocationRegenIVHero = 0;
        }
    }

    private void setEvocationColumn(float threatFactor, int evocationSegments, float evocationMana, CastingState evoState, int segment, int manaSegment, VariableType evocationType, boolean lastTick) {
        double dps = 0.0f;
        if (waterElementalAvailable && this.mageTalents.isGlyphOfEternalWater()) {
            dps = evoState.getSpell(SpellId.Waterbolt).getDamagePerSecond();
        }
        double tps;
        double mps;
        float evocationDuration;
        int column = lp.addColumnUnsafe();
        double evoFactor;
        switch (evocationType) {
            case Evocation:
                mps = -this.evocationRegen;
                tps = 0.15f * evocationMana / 2f * this.baseState.castingSpeed * 0.5f * threatFactor;
                evocationDuration = this.evocationDuration;
                evoFactor = 1.0;

                lp.setElementUnsafe(rowEvocation, column, 1.0);

                break;
            case EvocationHero:
                mps = -this.evocationRegenHero;
                tps = 0.15f * evocationMana / 2f * this.baseState.castingSpeed * 1.3 * 0.5f * threatFactor;
                evocationDuration = this.evocationDurationHero;
                evoFactor = 1.3;

                if (lastTick) {
                    lp.setElementUnsafe(rowHeroism, column, 1.0);
                }
                lp.setElementUnsafe(rowEvocation, column, 1.3);
                lp.setElementUnsafe(rowEvocationHero, column, 1.0);

                break;
            case EvocationIV:
                mps = -this.evocationRegenIV;
                tps = 0.15f * evocationMana / 2f * this.baseState.castingSpeed * 1.2 * 0.5f * threatFactor;
                evocationDuration = this.evocationDurationIV;
                evoFactor = 1.2;

                if (lastTick) {
                    lp.setElementUnsafe(rowIcyVeins, column, 1.0);
                    if (segmentCooldowns) {
                        for (SegmentConstraint constraint : rowSegmentIcyVeins) {
                            if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
                        }
                    }
                }
                lp.setElementUnsafe(rowEvocation, column, 1.2);
                lp.setElementUnsafe(rowEvocationIV, column, 1.0);

                break;
            case EvocationIVHero:
            default:
                mps = -this.evocationRegenIVHero;
                tps = 0.15f * evocationMana / 2f * this.baseState.castingSpeed * 1.2 * 1.3 * 0.5f * threatFactor;
                evocationDuration = this.evocationDurationIVHero;
                evoFactor = 1.2 * 1.3;

                if (lastTick) {
                    lp.setElementUnsafe(rowHeroism, column, 1.0);
                    lp.setElementUnsafe(rowIcyVeins, column, 1.0);
                    lp.setElementUnsafe(rowHeroismIcyVeins, column, 1.0);
                    if (segmentCooldowns) {
                        for (SegmentConstraint constraint : rowSegmentIcyVeins) {
                            if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
                        }
                    }
                }
                lp.setElementUnsafe(rowEvocation, column, 1.2 * 1.3);
                lp.setElementUnsafe(rowEvocationHero, column, 1.2);
                lp.setElementUnsafe(rowEvocationIVHero, column, 1.0);

                break;
        }
        if (needsSolutionVariables) {
            SolutionVariable sv = new SolutionVariable();
            sv.type = evocationType;
            sv.segment = segment;
            sv.manaSegment = manaSegment;
            sv.state = evoState;
            sv.dps = dps;
            sv.mps = mps;
            sv.tps = tps;
            this.solutionVariable.add(sv);
        }
        lp.setColumnUpperBound(column, (evocationSegments > 1 || manaSegments > 1) ? evocationDuration : evocationDuration * this.maxEvocation);
        lp.setElementUnsafe(rowAfterFightRegenMana, column, mps);
        lp.setElementUnsafe(rowManaRegen, column, mps);
        lp.setElementUnsafe(rowFightDuration, column, 1.0);
        lp.setElementUnsafe(rowTimeExtension, column, -1.0);
        lp.setElementUnsafe(rowThreat, column, tps); // should split among all targets if more than one, assume one only
        lp.setCostUnsafe(column, minimizeTime ? -1 : dps);
        if (segmentNonCooldowns) lp.setElementUnsafe(rowSegment + segment, column, 1.0);
        if (restrictManaUse) {
            if (segmentMana) {
                for (int ss = segment * manaSegments + manaSegment - 1; ss < this.segmentList.size() * manaSegments - 1; ss++) {
                    lp.setElementUnsafe(rowSegmentManaOverflow + ss, column, -mps);
                }
                for (int ss = segment * manaSegments + manaSegment; ss < this.segmentList.size() * manaSegments - 1; ss++) {
                    lp.setElementUnsafe(rowSegmentManaUnderflow + ss, column, mps);
                }
            } else {
                for (int ss = segment; ss < this.segmentList.size() - 1; ss++) {
                    lp.setElementUnsafe(rowSegmentManaUnderflow + ss, column, mps);
                    lp.setElementUnsafe(rowSegmentManaOverflow + ss, column, -mps);
                }
            }
            if (segmentCooldowns) {
                for (SegmentConstraint constraint : rowSegmentEvocation) {
                    if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, evoFactor);
                }
            }
        }
        if (restrictThreat) {
            for (int ss = segment; ss < this.segmentList.size() - 1; ss++) {
                lp.setElementUnsafe(rowSegmentThreat + ss, column, tps);
            }
        }
    }

    private static MagicSchool magicSchool(ItemDamageType damageType) {
        switch (damageType) {
            case Holy: return MagicSchool.Holy;
            case Fire: return MagicSchool.Fire;
            case Nature: return MagicSchool.Nature;
            case Frost: return MagicSchool.Frost;
            case Shadow: return MagicSchool.Shadow;
            case Arcane: return MagicSchool.Arcane;
            default: return MagicSchool.Physical; // C# casts ItemDamageType.Physical to undefined MagicSchool 0
        }
    }

    private void constructWand() {
        if (this.character.getRanged() != null && this.character.getRanged().getItem().getType() == ItemType.Wand) {
            if (useIncrementalOptimizations) {
                boolean first = true;
                Cycle wand = null;
                for (int index = 0; index < this.calculationOptions.getIncrementalSetStateIndexes().length; index++) {
                    if (this.calculationOptions.getIncrementalSetVariableType()[index] == VariableType.Wand) {
                        if (first) {
                            getWandTemplate().initialize(this, magicSchool(this.character.getRanged().getItem().getDamageType()), this.character.getRanged().getItem().getMinDamage(), this.character.getRanged().getItem().getMaxDamage(), this.character.getRanged().getItem().getSpeed());
                            Spell w = getWandTemplate().getSpell(this.baseState);
                            this.wand = w;
                            wand = w.getCycle();
                            first = false;
                        }
                        int segment = this.calculationOptions.getIncrementalSetSegments()[index];
                        int manaSegment = this.calculationOptions.getIncrementalSetManaSegment()[index];
                        float mult = segmentCooldowns ? this.calculationOptions.getDamageMultiplier(this.segmentList.get(segment)) : 1.0f;
                        double mps = wand.getManaPerSecond();
                        double dps = wand.getDamagePerSecond() * mult;
                        double tps = wand.getThreatPerSecond();
                        if (mult > 0) {
                            setWandColumn(wand, mps, segment, manaSegment, dps, tps);
                        }
                    }
                }
            } else {
                int wandSegments = (segmentNonCooldowns) ? this.segmentList.size() : 1;
                getWandTemplate().initialize(this, magicSchool(this.character.getRanged().getItem().getDamageType()), this.character.getRanged().getItem().getMinDamage(), this.character.getRanged().getItem().getMaxDamage(), this.character.getRanged().getItem().getSpeed());
                Spell w = getWandTemplate().getSpell(this.baseState);
                this.wand = w;
                Cycle wand = w.getCycle();
                double mps = wand.getManaPerSecond();
                for (int segment = 0; segment < wandSegments; segment++) {
                    for (int manaSegment = 0; manaSegment < manaSegments; manaSegment++) {
                        float mult = segmentCooldowns ? this.calculationOptions.getDamageMultiplier(this.segmentList.get(segment)) : 1.0f;
                        double dps = wand.getDamagePerSecond() * mult;
                        double tps = wand.getThreatPerSecond();
                        if (mult > 0) {
                            setWandColumn(wand, mps, segment, manaSegment, dps, tps);
                        }
                    }
                }
            }
        } else {
            this.wand = null;
        }
    }

    private void setWandColumn(Cycle wand, double mps, int segment, int manaSegment, double dps, double tps) {
        int column = lp.addColumnUnsafe();
        lp.setColumnUpperBound(column, segmentNonCooldowns ? this.segmentList.get(segment).duration : this.calculationOptions.getFightDuration());
        if (needsSolutionVariables) {
            SolutionVariable sv = new SolutionVariable();
            sv.type = VariableType.Wand;
            sv.cycle = wand;
            sv.segment = segment;
            sv.manaSegment = manaSegment;
            sv.state = this.baseState;
            sv.dps = dps;
            sv.mps = mps;
            sv.tps = tps;
            this.solutionVariable.add(sv);
        }
        lp.setElementUnsafe(rowAfterFightRegenMana, column, mps);
        lp.setElementUnsafe(rowManaRegen, column, mps);
        lp.setElementUnsafe(rowFightDuration, column, 1.0);
        lp.setElementUnsafe(rowTimeExtension, column, -1.0);
        lp.setElementUnsafe(rowThreat, column, tps);
        lp.setElementUnsafe(rowTargetDamage, column, -wand.getDamagePerSecond());
        lp.setCostUnsafe(column, minimizeTime ? -1 : dps);
        if (segmentNonCooldowns) lp.setElementUnsafe(rowSegment + segment, column, 1.0);
        if (restrictManaUse) {
            for (int ss = segment * manaSegments + manaSegment; ss < this.segmentList.size() * manaSegments - 1; ss++) {
                lp.setElementUnsafe(rowSegmentManaUnderflow + ss, column, mps);
                lp.setElementUnsafe(rowSegmentManaOverflow + ss, column, -mps);
            }
        }
        if (restrictThreat) {
            for (int ss = segment; ss < this.segmentList.size() - 1; ss++) {
                lp.setElementUnsafe(rowSegmentThreat + ss, column, tps);
            }
        }
    }

    private void constructIdleRegen() {
        int idleRegenSegments = (restrictManaUse) ? this.segmentList.size() : 1;
        double dps = 0.0f;
        double tps = 0.0f;
        double mps = -(this.baseState.getManaRegen() * (1 - this.calculationOptions.getFragmentation()) + this.baseState.getManaRegen5SR() * this.calculationOptions.getFragmentation());
        if (useIncrementalOptimizations) {
            for (int index = 0; index < this.calculationOptions.getIncrementalSetStateIndexes().length; index++) {
                if (this.calculationOptions.getIncrementalSetVariableType()[index] == VariableType.IdleRegen) {
                    int segment = this.calculationOptions.getIncrementalSetSegments()[index];
                    int manaSegment = this.calculationOptions.getIncrementalSetManaSegment()[index];
                    setIdleRegenColumn(idleRegenSegments, dps, tps, mps, segment, manaSegment);
                }
            }
        } else {
            for (int segment = 0; segment < idleRegenSegments; segment++) {
                for (int manaSegment = 0; manaSegment < manaSegments; manaSegment++) {
                    setIdleRegenColumn(idleRegenSegments, dps, tps, mps, segment, manaSegment);
                }
            }
        }
    }

    private void setIdleRegenColumn(int idleRegenSegments, double dps, double tps, double mps, int segment, int manaSegment) {
        int column = lp.addColumnUnsafe();
        lp.setColumnUpperBound(column, (idleRegenSegments > 1) ? this.segmentList.get(segment).duration : this.calculationOptions.getFightDuration());
        if (idleRegenSegments == 1 && manaSegments == 1 && !needsTimeExtension) {
            lp.setColumnLowerBound(column, (1 - dpsTime) * this.calculationOptions.getFightDuration());
        }
        if (needsSolutionVariables) {
            SolutionVariable sv = new SolutionVariable();
            sv.type = VariableType.IdleRegen;
            sv.segment = segment;
            sv.manaSegment = manaSegment;
            sv.state = this.baseState;
            sv.dps = dps;
            sv.mps = mps;
            sv.tps = tps;
            this.solutionVariable.add(sv);
        }
        lp.setElementUnsafe(rowAfterFightRegenMana, column, mps);
        lp.setElementUnsafe(rowManaRegen, column, mps);
        lp.setElementUnsafe(rowFightDuration, column, 1.0);
        lp.setElementUnsafe(rowTimeExtension, column, -1.0);
        lp.setElementUnsafe(rowDpsTime, column, -1.0);
        lp.setCostUnsafe(column, minimizeTime ? -1 : 0);
        if (segmentNonCooldowns) lp.setElementUnsafe(rowSegment + segment, column, 1.0);
        if (restrictManaUse) {
            for (int ss = segment * manaSegments + manaSegment; ss < idleRegenSegments * manaSegments - 1; ss++) {
                lp.setElementUnsafe(rowSegmentManaUnderflow + ss, column, mps);
                lp.setElementUnsafe(rowSegmentManaOverflow + ss, column, -mps);
            }
        }
    }

    private void setCalculationReuseReferences() {
        // determine which effects only cause a change in haste, thus allowing calculation reuse (only recalculating cast time)
        int recalcCastTime = rawr.models.mage.StandardEffect.IcyVeins | StandardEffect.Heroism | StandardEffect.PotionOfSpeed | StandardEffect.Berserking | StandardEffect.PowerInfusion;
        for (int i = 0; i < this.itemBasedEffectCooldownsCount; i++) {
            EffectCooldown effect = this.itemBasedEffectCooldowns[i];
            if (effect.hasteEffect) {
                recalcCastTime |= effect.mask;
            }
        }
        if (this.baseStats.getMage4T10() == 0) {
            recalcCastTime |= rawr.models.mage.StandardEffect.MirrorImage; // for this it's actually identical, potential for further optimization
        }
        // states will be calculated in forward manner, see if some can reuse previous states
        for (int i = 0; i < stateList.size(); i++) {
            CastingState si = stateList.get(i);
            for (int j = 0; j < i; j++) {
                CastingState sj = stateList.get(j);
                // check the difference
                int diff = si.getEffects() ^ sj.getEffects();
                if ((diff & ~recalcCastTime) == 0) {
                    // the only difference is in haste effects
                    si.referenceCastingState = sj;
                    break;
                }
            }
        }
    }

    private void addWardStates() {
        if (this.calculationOptions.getUseFireWard() || this.calculationOptions.getUseFrostWard()) {
            List<CastingState> newStates = new ArrayList<CastingState>();
            for (CastingState state : stateList) {
                newStates.add(state);
                if (this.calculationOptions.getUseFireWard()) {
                    CastingState s = new CastingState(this, state.getEffects(), false, 0);
                    s.useFireWard = true;
                    s.referenceCastingState = state;
                    newStates.add(s);
                }
                if (this.calculationOptions.getUseFrostWard()) {
                    CastingState s = new CastingState(this, state.getEffects(), false, 0);
                    s.useFrostWard = true;
                    s.referenceCastingState = state;
                    newStates.add(s);
                }
            }
            stateList = newStates;
        }
    }

    private static boolean tryParseDouble(String s, double[] result) {
        try {
            result[0] = Double.parseDouble(s.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String[] splitLimitRemoveEmpty(String s, int limit) {
        List<String> tokens = new ArrayList<String>();
        int i = 0;
        while (i < s.length()) {
            while (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == ':' || s.charAt(i) == ' ')) i++;
            if (i >= s.length()) break;
            if (tokens.size() == limit - 1) {
                tokens.add(s.substring(i));
                break;
            }
            int start = i;
            while (i < s.length() && s.charAt(i) != '-' && s.charAt(i) != ':' && s.charAt(i) != ' ') i++;
            tokens.add(s.substring(start, i));
        }
        return tokens.toArray(new String[0]);
    }

    private void constructSegments() {
        if (this.segmentList == null) {
            this.segmentList = new ArrayList<Segment>();
        } else {
            this.segmentList.clear();
        }
        if (segmentCooldowns) {
            List<Double> ticks = new ArrayList<Double>();
            if (this.calculationOptions.getVariableSegmentDuration()) {
                // variable segment durations to get a better grasp on varied cooldown durations
                // create ticks in intervals of half cooldown duration
                if (potionOfSpeedAvailable || potionOfWildMagicAvailable || manaPotionAvailable) {
                    addSegmentTicks(ticks, 120.0);
                }
                if (arcanePowerAvailable) {
                    addSegmentTicks(ticks, this.arcanePowerCooldown);
                }
                if (combustionAvailable) addSegmentTicks(ticks, 300.0);
                if (berserkingAvailable) addSegmentTicks(ticks, 180.0);
                if (this.calculationOptions.getManaGemEnabled() || manaGemEffectAvailable) {
                    ticks.add(15.0); // get a better grasp on mana overflow
                    addSegmentTicks(ticks, 60.0);
                }
                if (icyVeinsAvailable) {
                    addSegmentTicks(ticks, this.icyVeinsCooldown);
                }
                if (waterElementalAvailable && !this.mageTalents.isGlyphOfEternalWater()) addSegmentTicks(ticks, this.waterElementalCooldown);
                for (int i = 0; i < this.itemBasedEffectCooldownsCount; i++) {
                    EffectCooldown cooldown = this.itemBasedEffectCooldowns[i];
                    addSegmentTicks(ticks, cooldown.cooldown);
                }
            } else {
                for (int i = 0; this.calculationOptions.getFixedSegmentDuration() * i < this.calculationOptions.getFightDuration() - 0.00001; i++) {
                    ticks.add((double) (this.calculationOptions.getFixedSegmentDuration() * i));
                }
            }
            if (this.calculationOptions.getAdditionalSegmentSplits() != null && !this.calculationOptions.getAdditionalSegmentSplits().isEmpty()) {
                String[] splits = this.calculationOptions.getAdditionalSegmentSplits().split(",", -1);
                for (String split : splits) {
                    double[] tick = new double[1];
                    if (tryParseDouble(split.trim(), tick)) {
                        ticks.add(tick[0]);
                    }
                }
            }
            if (moltenFuryAvailable) {
                ticks.add((double) ((1 - this.calculationOptions.getMoltenFuryPercentage()) * this.calculationOptions.getFightDuration()));
            }
            if (this.calculationOptions.getHeroismControl() == 3) {
                ticks.add(Math.max(0.0, Math.min(this.calculationOptions.getFightDuration() - this.calculationOptions.getMoltenFuryPercentage() * this.calculationOptions.getFightDuration(), this.calculationOptions.getFightDuration() - 40.0)));
            }
            if (this.calculationOptions.getCooldownRestrictions() != null && !this.calculationOptions.getCooldownRestrictions().isEmpty() && this.calculationOptions.getCooldownRestrictionList() == null) {
                Scanner scanner = new Scanner();
                Parser parser = new Parser(scanner);
                this.calculationOptions.setCooldownRestrictionList(new ArrayList<CooldownRestriction>());
                String[] lines = splitLinesRemoveEmpty(this.calculationOptions.getCooldownRestrictions());
                for (String line : lines) {
                    String[] tokens = splitLimitRemoveEmpty(line, 3);
                    if (tokens.length == 3) {
                        CooldownRestriction restriction = new CooldownRestriction();
                        double[] value = new double[1];
                        if (!tryParseDouble(tokens[0], value)) continue;
                        restriction.timeStart = value[0];
                        if (!tryParseDouble(tokens[1], value)) continue;
                        restriction.timeEnd = value[0];
                        ParseTree parseTree = parser.parse(tokens[2]);
                        if (parseTree != null && parseTree.errors.size() == 0) {
                            try {
                                restriction.isMatch = parseTree.compile();
                                this.calculationOptions.getCooldownRestrictionList().add(restriction);
                            } catch (Exception ex) {
                            }
                        }
                    }
                }
            }
            if (this.calculationOptions.getCooldownRestrictionList() != null) {
                for (CooldownRestriction restriction : this.calculationOptions.getCooldownRestrictionList()) {
                    ticks.add(restriction.timeStart);
                    ticks.add(restriction.timeEnd);
                }
            }
            if (this.calculationOptions.getEncounterEnabled() && this.calculationOptions.getEncounter() != null) {
                for (DamageMultiplier multiplier : this.calculationOptions.getEncounter().getGlobalMultipliers()) {
                    if (multiplier.relativeTime) {
                        ticks.add((double) (multiplier.startTime * this.calculationOptions.getFightDuration()));
                        ticks.add((double) (multiplier.endTime * this.calculationOptions.getFightDuration()));
                    } else {
                        ticks.add((double) multiplier.startTime);
                        ticks.add((double) multiplier.endTime);
                    }
                }
                for (TargetGroup group : this.calculationOptions.getEncounter().getTargetGroups()) {
                    float startTime, endTime;
                    if (group.relativeTime) {
                        startTime = group.entranceTime * this.calculationOptions.getFightDuration();
                        endTime = group.exitTime * this.calculationOptions.getFightDuration();
                    } else {
                        startTime = group.entranceTime;
                        endTime = group.exitTime;
                    }
                    ticks.add((double) startTime);
                    ticks.add((double) endTime);
                    for (DamageMultiplier multiplier : group.multipliers) {
                        if (multiplier.relativeTime) {
                            ticks.add((double) (startTime + multiplier.startTime * (endTime - startTime)));
                            ticks.add((double) (startTime + multiplier.endTime * (endTime - startTime)));
                        } else {
                            ticks.add((double) (startTime + multiplier.startTime));
                            ticks.add((double) (startTime + multiplier.endTime));
                        }
                    }
                }
            }
            Collections.sort(ticks);
            for (int i = 0; i < ticks.size(); i++) {
                if ((i == 0 || ticks.get(i) > ticks.get(i - 1) + 0.00001) && ticks.get(i) < this.calculationOptions.getFightDuration() - 0.00001) {
                    if (this.segmentList.size() > 0) {
                        this.segmentList.get(this.segmentList.size() - 1).duration = ticks.get(i) - ticks.get(i - 1);
                    }
                    Segment segment = new Segment();
                    segment.timeStart = ticks.get(i);
                    this.segmentList.add(segment);
                }
            }
            this.segmentList.get(this.segmentList.size() - 1).duration = this.calculationOptions.getFightDuration() - this.segmentList.get(this.segmentList.size() - 1).timeStart;
        } else {
            Segment segment = new Segment();
            segment.timeStart = 0;
            segment.duration = this.calculationOptions.getFightDuration();
            this.segmentList.add(segment);
        }
        for (int i = 0; i < this.segmentList.size(); i++) {
            this.segmentList.get(i).index = i;
        }
    }

    private static String[] splitLinesRemoveEmpty(String s) {
        List<String> lines = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == '\r' || s.charAt(i) == '\n') {
                if (i > start) lines.add(s.substring(start, i));
                start = i + 1;
            }
        }
        return lines.toArray(new String[0]);
    }

    private int findIndexTimeEndAfter(double value) {
        for (int i = 0; i < this.segmentList.size(); i++) {
            if (this.segmentList.get(i).getTimeEnd() > value) return i;
        }
        return -1;
    }

    private int findIndexTimeStartAtLeast(double value) {
        for (int i = 0; i < this.segmentList.size(); i++) {
            if (this.segmentList.get(i).timeStart >= value) return i;
        }
        return -1;
    }

    private void setProblemRHS() {
        int[] coldsnapCount = new int[1];
        coldsnapCount[0] = coldsnapAvailable ? (1 + (int) ((this.calculationOptions.getFightDuration() - this.waterElementalDuration) / this.coldsnapCooldown)) : 0;

        // water elemental
        double weDuration = 0.0;
        if (waterElementalAvailable) {
            if (this.mageTalents.isGlyphOfEternalWater()) {
                weDuration = this.calculationOptions.getFightDuration();
            } else {
                weDuration = maximizeEffectDuration(this.calculationOptions.getFightDuration(), this.waterElementalDuration, this.waterElementalCooldown);
                if (coldsnapAvailable) weDuration = maximizeColdsnapDuration(this.calculationOptions.getFightDuration(), this.coldsnapCooldown, this.waterElementalDuration, this.waterElementalCooldown, coldsnapCount);
            }
        }

        double combustionCount = combustionAvailable ? (1 + (int) ((this.calculationOptions.getFightDuration() - 15f) / 195f)) : 0;

        double ivlength = 0.0;
        if ((!waterElementalAvailable || this.mageTalents.isGlyphOfEternalWater()) && coldsnapAvailable) {
            ivlength = Math.floor(maximizeColdsnapDuration(this.calculationOptions.getFightDuration(), this.coldsnapCooldown, 20.0, this.icyVeinsCooldown, coldsnapCount));
        } else if (waterElementalAvailable && coldsnapAvailable) {
            double wecount = (weDuration / this.waterElementalDuration);
            if (wecount >= Math.floor(wecount) + 20.0 / this.waterElementalDuration)
                ivlength = Math.ceil(wecount) * 20.0;
            else
                ivlength = Math.floor(wecount) * 20.0;
        } else {
            double effectiveDuration = this.calculationOptions.getFightDuration();
            if (waterElementalAvailable) effectiveDuration -= this.baseGlobalCooldown; // EXPERIMENTAL
            ivlength = maximizeEffectDuration(effectiveDuration, 20.0, this.icyVeinsCooldown);
        }

        double mflength = this.calculationOptions.getMoltenFuryPercentage() * this.calculationOptions.getFightDuration();

        lp.setRHSUnsafe(rowManaRegen, this.startingMana);
        lp.setRHSUnsafe(rowFightDuration, this.calculationOptions.getFightDuration());
        lp.setRHSUnsafe(rowTimeExtension, -this.calculationOptions.getFightDuration());
        lp.setRHSUnsafe(rowEvocation, this.evocationDuration * this.maxEvocation);
        if (icyVeinsAvailable) lp.setRHSUnsafe(rowEvocationIV, this.evocationDurationIV * this.maxEvocation);
        if (heroismAvailable) lp.setRHSUnsafe(rowEvocationHero, this.evocationDurationHero);
        if (icyVeinsAvailable && heroismAvailable) lp.setRHSUnsafe(rowEvocationIVHero, this.evocationDurationIVHero);
        lp.setRHSUnsafe(rowPotion, 1.0);
        lp.setRHSUnsafe(rowManaPotion, 1.0);
        lp.setRHSUnsafe(rowManaGem, Math.min(3, this.calculationOptions.getAverageCooldowns() ? this.calculationOptions.getFightDuration() / 120.0 : this.maxManaGem));
        lp.setRHSUnsafe(rowManaGemMax, this.calculationOptions.getAverageCooldowns() ? this.calculationOptions.getFightDuration() / 120.0 : this.maxManaGem);
        if (conjureManaGem) lp.setRHSUnsafe(rowConjureManaGem, this.maxConjureManaGem * this.conjureManaGemCycle.castTime);

        for (EffectCooldown cooldown : this.cooldownList) {
            if (cooldown.automaticConstraints) {
                lp.setRHSUnsafe(cooldown.row, (this.calculationOptions.getAverageCooldowns() && cooldown.cooldown != Float.POSITIVE_INFINITY) ? this.calculationOptions.getFightDuration() * cooldown.duration / cooldown.cooldown : cooldown.maximumDuration);
            }
        }

        if (heroismAvailable) {
            double minDuration = Math.min(0.99 * this.calculationOptions.getFightDuration() * dpsTime, 40.0);
            if (moltenFuryAvailable && this.calculationOptions.getHeroismControl() == 3 && mflength < minDuration) {
                minDuration = 0.99 * mflength;
            }
            lp.setLHSUnsafe(rowHeroism, minDuration); // if heroism is marked as available then this implies that it has to be used, not only that it can be used
        }
        if (icyVeinsAvailable && coldsnapAvailable) {
            lp.setRHSUnsafe(rowIcyVeins, this.calculationOptions.getAverageCooldowns() ? (20.0 / this.icyVeinsCooldown + 20.0 / this.coldsnapCooldown) * this.calculationOptions.getFightDuration() : ivlength);
            if (moltenFuryAvailable) lp.setRHSUnsafe(rowMoltenFuryIcyVeins, 40);
            if (heroismAvailable) lp.setRHSUnsafe(rowHeroismIcyVeins, 40);
        }
        if (moltenFuryAvailable) lp.setRHSUnsafe(rowMoltenFury, mflength);
        if (manaGemEffectAvailable) lp.setRHSUnsafe(rowManaGemEffect, this.calculationOptions.getAverageCooldowns() ? this.calculationOptions.getFightDuration() * this.manaGemEffectDuration / 120f : maximizeEffectDuration(this.calculationOptions.getFightDuration(), this.manaGemEffectDuration, 120.0));
        lp.setRHSUnsafe(rowDpsTime, -(1 - dpsTime) * this.calculationOptions.getFightDuration());
        lp.setRHSUnsafe(rowAoe, this.calculationOptions.getAoeDuration() * this.calculationOptions.getFightDuration());
        lp.setRHSUnsafe(rowCombustion, this.calculationOptions.getAverageCooldowns() ? this.calculationOptions.getFightDuration() / CombustionCooldown : combustionCount);
        lp.setRHSUnsafe(rowMoltenFuryCombustion, 1);
        lp.setRHSUnsafe(rowHeroismCombustion, 1);
        lp.setRHSUnsafe(rowThreat, this.calculationOptions.getTpsLimit() * this.calculationOptions.getFightDuration());
        double manaConsum;
        manaConsum = this.maxManaGem;
        lp.setRHSUnsafe(rowBerserking, this.calculationOptions.getAverageCooldowns() ? this.calculationOptions.getFightDuration() * 10.0 / 180.0 : 10.0 * (1 + (int) ((this.calculationOptions.getFightDuration() - 10) / 180)));
        if (waterElementalAvailable && !this.mageTalents.isGlyphOfEternalWater()) {
            double duration = this.calculationOptions.getAverageCooldowns() ? (this.waterElementalDuration / this.waterElementalCooldown + (coldsnapAvailable ? this.waterElementalDuration / this.coldsnapCooldown : 0.0)) * this.calculationOptions.getFightDuration() : weDuration;
            lp.setRHSUnsafe(rowWaterElemental, duration);
            lp.setRHSUnsafe(rowSummonWaterElementalCount, this.baseGlobalCooldown * Math.ceil(duration / this.waterElementalDuration));
        }
        if (mirrorImageAvailable) {
            double duration = this.effectCooldown.get(StandardEffect.MirrorImage).maximumDuration;
            lp.setRHSUnsafe(rowSummonMirrorImageCount, this.baseGlobalCooldown * Math.ceil(duration / MirrorImageDuration));
        }
        lp.setRHSUnsafe(rowTargetDamage, -this.calculationOptions.getTargetDamage());

        for (int i = 0; i < rowStackingConstraintCount; i++) {
            lp.setRHSUnsafe(rowStackingConstraint[i].row, rowStackingConstraint[i].maximumStackingDuration);
        }

        if (segmentCooldowns) {
            // heroism
            // ap
            if (arcanePowerAvailable) {
                for (SegmentConstraint constraint : rowSegmentArcanePower) {
                    lp.setRHSUnsafe(constraint.row, this.arcanePowerDuration);
                }
            }
            // pi
            if (powerInfusionAvailable) {
                for (SegmentConstraint constraint : rowSegmentPowerInfusion) {
                    lp.setRHSUnsafe(constraint.row, PowerInfusionDuration);
                }
            }
            // iv
            if (icyVeinsAvailable) {
                for (SegmentConstraint constraint : rowSegmentIcyVeins) {
                    lp.setRHSUnsafe(constraint.row, 20 + (coldsnapAvailable ? 20 : 0));
                }
            }
            // combustion
            if (combustionAvailable) {
                for (SegmentConstraint constraint : rowSegmentCombustion) {
                    lp.setRHSUnsafe(constraint.row, 1.0);
                }
            }
            // berserking
            if (berserkingAvailable) {
                for (SegmentConstraint constraint : rowSegmentBerserking) {
                    lp.setRHSUnsafe(constraint.row, 10.0);
                }
            }
            // water elemental
            if (waterElementalAvailable && !this.mageTalents.isGlyphOfEternalWater()) {
                for (SegmentConstraint constraint : rowSegmentWaterElemental) {
                    lp.setRHSUnsafe(constraint.row, this.waterElementalDuration + (coldsnapAvailable ? this.waterElementalDuration : 0.0));
                }
                for (SegmentConstraint constraint : rowSegmentSummonWaterElemental) {
                    lp.setRHSUnsafe(constraint.row, this.baseGlobalCooldown + (coldsnapAvailable ? this.baseGlobalCooldown : 0.0));
                }
            }
            // mirror image
            if (mirrorImageAvailable) {
                for (SegmentConstraint constraint : rowSegmentMirrorImage) {
                    lp.setRHSUnsafe(constraint.row, MirrorImageDuration);
                }
                for (SegmentConstraint constraint : rowSegmentSummonMirrorImage) {
                    lp.setRHSUnsafe(constraint.row, this.baseGlobalCooldown);
                }
            }
            // flamecap
            if (flameCapAvailable) {
                for (SegmentConstraint constraint : rowSegmentFlameCap) {
                    lp.setRHSUnsafe(constraint.row, 60.0);
                }
            }
            if (this.calculationOptions.getManaGemEnabled()) {
                for (SegmentConstraint constraint : rowSegmentManaGem) {
                    lp.setRHSUnsafe(constraint.row, 1.0);
                }
            }
            for (int i = 0; i < this.itemBasedEffectCooldownsCount; i++) {
                EffectCooldown cooldown = this.itemBasedEffectCooldowns[i];
                for (SegmentConstraint constraint : cooldown.segmentConstraints) {
                    lp.setRHSUnsafe(constraint.row, cooldown.duration);
                }
            }
            if (manaGemEffectAvailable) {
                for (SegmentConstraint constraint : rowSegmentManaGemEffect) {
                    lp.setRHSUnsafe(constraint.row, this.manaGemEffectDuration);
                }
            }
            if (evocationAvailable) {
                for (SegmentConstraint constraint : rowSegmentEvocation) {
                    lp.setRHSUnsafe(constraint.row, this.evocationDuration);
                }
            }
            // timing
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                lp.setRHSUnsafe(rowSegment + seg, this.segmentList.get(seg).duration);
            }
        }
        if (restrictManaUse) {
            for (int ss = 0; ss < this.segmentList.size() * manaSegments - 1; ss++) {
                lp.setRHSUnsafe(rowSegmentManaUnderflow + ss, this.startingMana);
                lp.setRHSUnsafe(rowSegmentManaOverflow + ss, this.baseStats.getMana() - this.startingMana);
            }
        }
        if (restrictThreat) {
            for (int ss = 0; ss < this.segmentList.size() - 1; ss++) {
                lp.setRHSUnsafe(rowSegmentThreat + ss, this.calculationOptions.getTpsLimit() * this.segmentList.get(ss).getTimeEnd());
            }
        }
    }

    private int constructRows(boolean minimizeTime, boolean drinkingEnabled, boolean needsTimeExtension, boolean afterFightRegen) {
        // Reset Rows
        rowManaRegen = -1;
        rowFightDuration = -1;
        rowEvocation = -1;
        rowEvocationIV = -1;
        rowEvocationHero = -1;
        rowEvocationIVHero = -1;
        rowPotion = -1;
        rowManaPotion = -1;
        rowConjureManaGem = -1;
        rowManaGem = -1;
        rowManaGemMax = -1;
        rowHeroism = -1;
        rowArcanePower = -1;
        rowIcyVeins = -1;
        rowWaterElemental = -1;
        rowMirrorImage = -1;
        rowMoltenFury = -1;
        rowMoltenFuryIcyVeins = -1;
        rowFlameCap = -1;
        rowManaGemEffect = -1;
        rowManaGemEffectActivation = -1;
        rowDpsTime = -1;
        rowAoe = -1;
        rowFlamestrike = -1;
        rowConeOfCold = -1;
        rowBlastWave = -1;
        rowDragonsBreath = -1;
        rowCombustion = -1;
        rowPowerInfusion = -1;
        rowMoltenFuryCombustion = -1;
        rowHeroismCombustion = -1;
        rowHeroismIcyVeins = -1;
        rowSummonWaterElemental = -1;
        rowSummonWaterElementalCount = -1;
        rowSummonMirrorImage = -1;
        rowSummonMirrorImageCount = -1;
        rowThreat = -1;
        rowBerserking = -1;
        rowTimeExtension = -1;
        rowAfterFightRegenMana = -1;
        rowTargetDamage = -1;
        rowSegment = -1;
        rowSegmentManaOverflow = -1;
        rowSegmentManaUnderflow = -1;
        rowSegmentThreat = -1;
        rowSegmentArcanePower = null;
        rowSegmentPowerInfusion = null;
        rowSegmentIcyVeins = null;
        rowSegmentWaterElemental = null;
        rowSegmentSummonWaterElemental = null;
        rowSegmentMirrorImage = null;
        rowSegmentSummonMirrorImage = null;
        rowSegmentCombustion = null;
        rowSegmentBerserking = null;
        rowSegmentFlameCap = null;
        rowSegmentManaGem = null;
        rowSegmentManaGemEffect = null;
        rowSegmentEvocation = null;

        int rowCount = 0;

        if (!this.calculationOptions.getUnlimitedMana()) rowManaRegen = rowCount++;
        rowFightDuration = rowCount++;
        if (evocationAvailable && (needsTimeExtension || restrictManaUse || integralMana || this.calculationOptions.getEnableHastedEvocation())) rowEvocation = rowCount++;
        if (this.calculationOptions.getEnableHastedEvocation()) {
            if (evocationAvailable && icyVeinsAvailable) {
                if (needsTimeExtension || restrictManaUse || integralMana) rowEvocationIV = rowCount++;
            }
            if (evocationAvailable && heroismAvailable) {
                if (needsTimeExtension || restrictManaUse || integralMana) rowEvocationHero = rowCount++;
            }
            if (evocationAvailable && icyVeinsAvailable && heroismAvailable) {
                if (needsTimeExtension || restrictManaUse || integralMana) rowEvocationIVHero = rowCount++;
            }
        }
        if (manaPotionAvailable || effectPotionAvailable) rowPotion = rowCount++;
        if (manaPotionAvailable && integralMana) rowManaPotion = rowCount++;
        if (this.calculationOptions.getManaGemEnabled()) {
            if (segmentCooldowns || conjureManaGem || needsTimeExtension || segmentMana) {
                rowManaGem = rowCount++;
            }
            if (requiresMIP || conjureManaGem) {
                rowManaGemMax = rowCount++;
            }
            if (conjureManaGem) {
                rowConjureManaGem = rowCount++;
            }
        }
        if (moltenFuryAvailable) rowMoltenFury = rowCount++;
        for (EffectCooldown cooldown : this.cooldownList) {
            if (cooldown.automaticConstraints) {
                cooldown.row = rowCount++;
                cooldown.maximumDuration = (float) maximizeEffectDuration(this.calculationOptions.getFightDuration(), cooldown.duration, cooldown.cooldown);
            }
        }
        if (manaGemEffectAvailable) rowManaGemEffectActivation = rowCount++;
        if (this.calculationOptions.getAoeDuration() > 0) {
            rowAoe = rowCount++;
            rowFlamestrike = rowCount++;
            rowConeOfCold = rowCount++;
            if (this.mageTalents.getBlastWave() == 1) rowBlastWave = rowCount++;
            if (this.mageTalents.getDragonsBreath() == 1) rowDragonsBreath = rowCount++;
        }
        if (combustionAvailable) rowCombustion = rowCount++;
        if (combustionAvailable && moltenFuryAvailable) rowMoltenFuryCombustion = rowCount++;
        if (combustionAvailable && heroismAvailable) rowHeroismCombustion = rowCount++;
        if (this.calculationOptions.getTpsLimit() > 0f) rowThreat = rowCount++;
        if (needsTimeExtension) rowTimeExtension = rowCount++;
        if (afterFightRegen) rowAfterFightRegenMana = rowCount++;
        if (minimizeTime) rowTargetDamage = rowCount++;
        if (waterElementalAvailable && !this.mageTalents.isGlyphOfEternalWater()) {
            rowWaterElemental = rowCount++;
            rowSummonWaterElemental = rowCount++;
            rowSummonWaterElementalCount = rowCount++;
        }
        if (mirrorImageAvailable) {
            rowSummonMirrorImage = rowCount++;
            if (requiresMIP) {
                rowSummonMirrorImageCount = rowCount++;
            }
        }
        if (dpsTime < 1 && (needsTimeExtension || segmentCooldowns || segmentMana)) {
            rowDpsTime = rowCount++;
        }

        rowStackingConstraintCount = 0;
        if (rowStackingConstraint == null) {
            rowStackingConstraint = new StackingConstraint[8];
        }
        for (int i = 0; i < this.cooldownList.size(); i++) {
            EffectCooldown cooli = this.cooldownList.get(i);
            if (cooli.automaticStackingConstraints) {
                for (int j = i + 1; j < this.cooldownList.size(); j++) {
                    EffectCooldown coolj = this.cooldownList.get(j);
                    if (coolj.automaticStackingConstraints) {
                        boolean valid = true;
                        for (int exclusionMask : effectExclusionList) {
                            if (bitCount2((cooli.mask | coolj.mask) & exclusionMask)) {
                                valid = false;
                                break;
                            }
                        }
                        if (valid) {
                            double maxDuration = maximizeStackingDuration(this.calculationOptions.getFightDuration(), cooli.duration, cooli.cooldown, coolj.duration, coolj.cooldown);
                            if (maxDuration < cooli.maximumDuration && maxDuration < coolj.maximumDuration) {
                                int scCount = rowStackingConstraintCount;
                                if (scCount >= rowStackingConstraint.length) {
                                    StackingConstraint[] newArr = new StackingConstraint[rowStackingConstraint.length * 2];
                                    System.arraycopy(rowStackingConstraint, 0, newArr, 0, scCount);
                                    rowStackingConstraint = newArr;
                                }
                                // Heroism is before IcyVeins in cooldown list in cooldown list (initialized in InitializeEffectCooldowns)
                                if (cooli.standardEffect == rawr.models.mage.StandardEffect.Heroism && coolj.standardEffect == rawr.models.mage.StandardEffect.IcyVeins) {
                                    rowHeroismIcyVeins = rowCount;
                                }
                                StackingConstraint[] arr = rowStackingConstraint;
                                if (arr[scCount] == null) arr[scCount] = new StackingConstraint();
                                arr[scCount].row = rowCount++;
                                arr[scCount].maximumStackingDuration = maxDuration;
                                arr[scCount].effect1 = cooli;
                                arr[scCount].effect2 = coolj;
                                rowStackingConstraintCount = scCount + 1;
                            }
                        }
                    }
                }
            }
        }

        if (coldsnapAvailable) {
            if (icyVeinsAvailable) rowIcyVeins = rowCount++;
            if (icyVeinsAvailable && heroismAvailable) rowHeroismIcyVeins = rowCount++;
            if (moltenFuryAvailable && icyVeinsAvailable) rowMoltenFuryIcyVeins = rowCount++;
        } else if (icyVeinsAvailable) {
            rowIcyVeins = this.effectCooldown.get(StandardEffect.IcyVeins).row;
        }
        if (heroismAvailable) rowHeroism = this.effectCooldown.get(StandardEffect.Heroism).row;
        if (arcanePowerAvailable) rowArcanePower = this.effectCooldown.get(StandardEffect.ArcanePower).row;
        if (powerInfusionAvailable) rowPowerInfusion = this.effectCooldown.get(StandardEffect.PowerInfusion).row;
        if (flameCapAvailable) rowFlameCap = this.effectCooldown.get(StandardEffect.FlameCap).row;
        if (berserkingAvailable) rowBerserking = this.effectCooldown.get(StandardEffect.Berserking).row;
        if (mirrorImageAvailable) rowMirrorImage = this.effectCooldown.get(StandardEffect.MirrorImage).row;

        if (segmentCooldowns) {
            rowCount = constructSegmentationRows(rowCount);
        }
        if (segmentCooldowns || segmentMana) {
            if (restrictManaUse) {
                int segments = segmentCooldowns ? this.segmentList.size() : 1;
                rowSegmentManaOverflow = rowCount;
                rowCount += segments * manaSegments - 1;
                rowSegmentManaUnderflow = rowCount;
                rowCount += segments * manaSegments - 1;
            }
        }
        return rowCount;
    }

    private int findIndexTimeEndAfterFrom(int seg, double cool) {
        double value = this.segmentList.get(seg).timeStart + cool + 0.00001;
        for (int i = 0; i < this.segmentList.size(); i++) {
            if (this.segmentList.get(i).getTimeEnd() > value) return i;
        }
        return -1;
    }

    private int constructSegmentationRows(int rowCount) {
        if (arcanePowerAvailable) {
            List<SegmentConstraint> list = rowSegmentArcanePower = new ArrayList<SegmentConstraint>();
            double cool = this.arcanePowerCooldown;
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                int maxs = findIndexTimeEndAfterFrom(seg, cool) - 1;
                if (maxs == -2) maxs = this.segmentList.size() - 1;
                if (list.size() == 0 || maxs > list.get(list.size() - 1).maxSegment) {
                    SegmentConstraint c = new SegmentConstraint();
                    c.row = rowCount++;
                    c.minSegment = seg;
                    c.maxSegment = maxs;
                    list.add(c);
                }
            }
        }
        if (powerInfusionAvailable) {
            List<SegmentConstraint> list = rowSegmentPowerInfusion = new ArrayList<SegmentConstraint>();
            double cool = PowerInfusionCooldown;
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                int maxs = findIndexTimeEndAfterFrom(seg, cool) - 1;
                if (maxs == -2) maxs = this.segmentList.size() - 1;
                if (list.size() == 0 || maxs > list.get(list.size() - 1).maxSegment) {
                    SegmentConstraint c = new SegmentConstraint();
                    c.row = rowCount++;
                    c.minSegment = seg;
                    c.maxSegment = maxs;
                    list.add(c);
                }
            }
        }
        if (icyVeinsAvailable) {
            List<SegmentConstraint> list = rowSegmentIcyVeins = new ArrayList<SegmentConstraint>();
            double cool = this.icyVeinsCooldown + (coldsnapAvailable ? 20 : 0);
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                int maxs = findIndexTimeEndAfterFrom(seg, cool) - 1;
                if (maxs == -2) maxs = this.segmentList.size() - 1;
                if (list.size() == 0 || maxs > list.get(list.size() - 1).maxSegment) {
                    SegmentConstraint c = new SegmentConstraint();
                    c.row = rowCount++;
                    c.minSegment = seg;
                    c.maxSegment = maxs;
                    list.add(c);
                }
            }
        }
        if (combustionAvailable) {
            List<SegmentConstraint> list = rowSegmentCombustion = new ArrayList<SegmentConstraint>();
            double cool = CombustionCooldown + 15;
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                int maxs = findIndexTimeEndAfterFrom(seg, cool) - 1;
                if (maxs == -2) maxs = this.segmentList.size() - 1;
                if (list.size() == 0 || maxs > list.get(list.size() - 1).maxSegment) {
                    SegmentConstraint c = new SegmentConstraint();
                    c.row = rowCount++;
                    c.minSegment = seg;
                    c.maxSegment = maxs;
                    list.add(c);
                }
            }
        }
        if (berserkingAvailable) {
            List<SegmentConstraint> list = rowSegmentBerserking = new ArrayList<SegmentConstraint>();
            double cool = 120;
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                int maxs = findIndexTimeEndAfterFrom(seg, cool) - 1;
                if (maxs == -2) maxs = this.segmentList.size() - 1;
                if (list.size() == 0 || maxs > list.get(list.size() - 1).maxSegment) {
                    SegmentConstraint c = new SegmentConstraint();
                    c.row = rowCount++;
                    c.minSegment = seg;
                    c.maxSegment = maxs;
                    list.add(c);
                }
            }
        }
        if (waterElementalAvailable && !this.mageTalents.isGlyphOfEternalWater()) {
            List<SegmentConstraint> list = rowSegmentWaterElemental = new ArrayList<SegmentConstraint>();
            double cool = this.waterElementalCooldown + (coldsnapAvailable ? this.waterElementalDuration : 0.0);
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                int maxs = findIndexTimeEndAfterFrom(seg, cool) - 1;
                if (maxs == -2) maxs = this.segmentList.size() - 1;
                if (list.size() == 0 || maxs > list.get(list.size() - 1).maxSegment) {
                    SegmentConstraint c = new SegmentConstraint();
                    c.row = rowCount++;
                    c.minSegment = seg;
                    c.maxSegment = maxs;
                    list.add(c);
                }
            }
            list = rowSegmentSummonWaterElemental = new ArrayList<SegmentConstraint>();
            cool = this.waterElementalCooldown + (coldsnapAvailable ? this.waterElementalDuration : 0.0);
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                int maxs = findIndexTimeEndAfterFrom(seg, cool) - 1;
                if (maxs == -2) maxs = this.segmentList.size() - 1;
                if (list.size() == 0 || maxs > list.get(list.size() - 1).maxSegment) {
                    SegmentConstraint c = new SegmentConstraint();
                    c.row = rowCount++;
                    c.minSegment = seg;
                    c.maxSegment = maxs;
                    list.add(c);
                }
            }
        }
        if (mirrorImageAvailable) {
            List<SegmentConstraint> list = rowSegmentMirrorImage = new ArrayList<SegmentConstraint>();
            double cool = MirrorImageCooldown;
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                int maxs = findIndexTimeEndAfterFrom(seg, cool) - 1;
                if (maxs == -2) maxs = this.segmentList.size() - 1;
                if (list.size() == 0 || maxs > list.get(list.size() - 1).maxSegment) {
                    SegmentConstraint c = new SegmentConstraint();
                    c.row = rowCount++;
                    c.minSegment = seg;
                    c.maxSegment = maxs;
                    list.add(c);
                }
            }
            list = rowSegmentSummonMirrorImage = new ArrayList<SegmentConstraint>();
            cool = MirrorImageCooldown;
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                int maxs = findIndexTimeEndAfterFrom(seg, cool) - 1;
                if (maxs == -2) maxs = this.segmentList.size() - 1;
                if (list.size() == 0 || maxs > list.get(list.size() - 1).maxSegment) {
                    SegmentConstraint c = new SegmentConstraint();
                    c.row = rowCount++;
                    c.minSegment = seg;
                    c.maxSegment = maxs;
                    list.add(c);
                }
            }
        }
        // flamecap & mana gem
        if (flameCapAvailable) {
            List<SegmentConstraint> list = rowSegmentFlameCap = new ArrayList<SegmentConstraint>();
            double cool = 180;
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                int maxs = findIndexTimeEndAfterFrom(seg, cool) - 1;
                if (maxs == -2) maxs = this.segmentList.size() - 1;
                if (list.size() == 0 || maxs > list.get(list.size() - 1).maxSegment) {
                    SegmentConstraint c = new SegmentConstraint();
                    c.row = rowCount++;
                    c.minSegment = seg;
                    c.maxSegment = maxs;
                    list.add(c);
                }
            }
        }
        if (this.calculationOptions.getManaGemEnabled()) {
            List<SegmentConstraint> list = rowSegmentManaGem = new ArrayList<SegmentConstraint>();
            double cool = 120;
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                int maxs = findIndexTimeEndAfterFrom(seg, cool) - 1;
                if (maxs == -2) maxs = this.segmentList.size() - 1;
                if (list.size() == 0 || maxs > list.get(list.size() - 1).maxSegment) {
                    SegmentConstraint c = new SegmentConstraint();
                    c.row = rowCount++;
                    c.minSegment = seg;
                    c.maxSegment = maxs;
                    list.add(c);
                }
            }
        }
        for (int i = 0; i < this.itemBasedEffectCooldownsCount; i++) {
            EffectCooldown cooldown = this.itemBasedEffectCooldowns[i];
            if (cooldown.segmentConstraints == null) {
                cooldown.segmentConstraints = new ArrayList<SegmentConstraint>();
            }
            List<SegmentConstraint> list = cooldown.segmentConstraints;
            double cool = cooldown.cooldown;
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                int maxs = findIndexTimeEndAfterFrom(seg, cool) - 1;
                if (maxs == -2) maxs = this.segmentList.size() - 1;
                if (list.size() == 0 || maxs > list.get(list.size() - 1).maxSegment) {
                    SegmentConstraint c = new SegmentConstraint();
                    c.row = rowCount++;
                    c.minSegment = seg;
                    c.maxSegment = maxs;
                    list.add(c);
                }
            }
        }
        // mana gem effect
        if (manaGemEffectAvailable) {
            List<SegmentConstraint> list = rowSegmentManaGemEffect = new ArrayList<SegmentConstraint>();
            double cool = 120.0;
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                int maxs = findIndexTimeEndAfterFrom(seg, cool) - 1;
                if (maxs == -2) maxs = this.segmentList.size() - 1;
                if (list.size() == 0 || maxs > list.get(list.size() - 1).maxSegment) {
                    SegmentConstraint c = new SegmentConstraint();
                    c.row = rowCount++;
                    c.minSegment = seg;
                    c.maxSegment = maxs;
                    list.add(c);
                }
            }
        }
        if (evocationAvailable) {
            List<SegmentConstraint> list = rowSegmentEvocation = new ArrayList<SegmentConstraint>();
            double cool = this.evocationCooldown;
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                int maxs = findIndexTimeEndAfterFrom(seg, cool) - 1;
                if (maxs == -2) maxs = this.segmentList.size() - 1;
                if (list.size() == 0 || maxs > list.get(list.size() - 1).maxSegment) {
                    SegmentConstraint c = new SegmentConstraint();
                    c.row = rowCount++;
                    c.minSegment = seg;
                    c.maxSegment = maxs;
                    list.add(c);
                }
            }
        }
        // max segment time
        rowSegment = rowCount;
        rowCount += this.segmentList.size();
        // mana overflow & underflow (don't need over all segments, that is already verified)
        if (restrictThreat) {
            rowSegmentThreat = rowCount;
            rowCount += this.segmentList.size() - 1;
        }
        return rowCount;
    }

    private void setSpellColumn(boolean minimizeTime, int segment, int manaSegment, CastingState state, int column, Cycle cycle, float multiplier) {
        double bound = this.calculationOptions.getFightDuration();
        double manaRegen = cycle.getManaPerSecond();
        lp.setElementUnsafe(rowAfterFightRegenMana, column, manaRegen);
        lp.setElementUnsafe(rowManaRegen, column, manaRegen);
        lp.setElementUnsafe(rowFightDuration, column, 1.0);
        lp.setElementUnsafe(rowTimeExtension, column, -1.0);
        if (state.potionOfWildMagic || state.potionOfSpeed) {
            lp.setElementUnsafe(rowPotion, column, 1.0 / 15.0);
        }
        if (state.waterElemental && !this.mageTalents.isGlyphOfEternalWater()) {
            lp.setElementUnsafe(rowWaterElemental, column, 1.0);
            lp.setElementUnsafe(rowSummonWaterElemental, column, 1 / (this.waterElementalDuration - this.baseGlobalCooldown));
        }
        if (state.mirrorImage) {
            lp.setElementUnsafe(rowMirrorImage, column, 1.0);
            lp.setElementUnsafe(rowSummonMirrorImage, column, 1 / (MirrorImageDuration - this.baseGlobalCooldown));
        }
        if (state.heroism) lp.setElementUnsafe(rowHeroism, column, 1.0);
        if (state.arcanePower) lp.setElementUnsafe(rowArcanePower, column, 1.0);
        if (state.powerInfusion) lp.setElementUnsafe(rowPowerInfusion, column, 1.0);
        if (state.moltenFury) lp.setElementUnsafe(rowMoltenFury, column, 1.0);
        if (state.icyVeins) {
            lp.setElementUnsafe(rowIcyVeins, column, 1.0);
            if (state.moltenFury) lp.setElementUnsafe(rowMoltenFuryIcyVeins, column, 1.0);
            if (coldsnapAvailable && state.heroism) {
                // VERY IMPORTANT!!!
                // you're only allowed to set this for coldsnap case, because otherwise it's already handled
                // by automatic constraints and those already include it
                // otherwise you end with two identical entries in sparse matrix which creates problems
                lp.setElementUnsafe(rowHeroismIcyVeins, column, 1.0);
            }
        }
        if (state.flameCap) lp.setElementUnsafe(rowFlameCap, column, 1.0);
        for (int i = 0; i < this.itemBasedEffectCooldownsCount; i++) {
            EffectCooldown cooldown = this.itemBasedEffectCooldowns[i];
            if (state.effectsActive(cooldown.mask)) {
                lp.setElementUnsafe(cooldown.row, column, 1.0);
            }
        }
        if (state.manaGemEffect) lp.setElementUnsafe(rowManaGemEffectActivation, column, 1 / this.manaGemEffectDuration);
        if (cycle.areaEffect) {
            lp.setElementUnsafe(rowAoe, column, 1.0);
            Spell fs = cycle.aoeSpell;
            if (fs.getSpellTemplate() instanceof FlamestrikeTemplate) {
                if (!fs.spammedDot) lp.setElementUnsafe(rowFlamestrike, column, fs.getDotDuration() / fs.castTime);
            } else {
                lp.setElementUnsafe(rowFlamestrike, column, -1.0);
            }
            Spell coc = cycle.aoeSpell;
            if (coc.getSpellTemplate() instanceof ConeOfColdTemplate) {
                lp.setElementUnsafe(rowConeOfCold, column, (coc.getCooldown() / coc.castTime - 1.0));
            } else {
                lp.setElementUnsafe(rowConeOfCold, column, -1.0);
            }
            Spell bw = cycle.aoeSpell;
            if (bw.getSpellTemplate() instanceof BlastWaveTemplate) {
                lp.setElementUnsafe(rowBlastWave, column, (bw.getCooldown() / bw.castTime - 1.0));
            } else {
                lp.setElementUnsafe(rowBlastWave, column, -1);
            }
            Spell db = cycle.aoeSpell;
            if (db.getSpellTemplate() instanceof DragonsBreathTemplate) {
                lp.setElementUnsafe(rowDragonsBreath, column, (db.getCooldown() / db.castTime - 1.0));
            } else {
                lp.setElementUnsafe(rowDragonsBreath, column, -1.0);
            }
        }
        if (state.combustion) {
            lp.setElementUnsafe(rowCombustion, column, (1 / (state.getCombustionDuration() * cycle.castTime / cycle.castProcs)));
            if (state.moltenFury) lp.setElementUnsafe(rowMoltenFuryCombustion, column, (1 / (state.getCombustionDuration() * cycle.castTime / cycle.castProcs)));
            if (state.heroism) lp.setElementUnsafe(rowHeroismCombustion, column, (1 / (state.getCombustionDuration() * cycle.castTime / cycle.castProcs)));
        }
        if (state.berserking) lp.setElementUnsafe(rowBerserking, column, 1.0);
        lp.setElementUnsafe(rowThreat, column, cycle.getThreatPerSecond());
        if (needsQuadratic) {
            float dps = cycle.getDamagePerSecond(this.manaAdeptBonus);
            lp.setElementUnsafe(rowTargetDamage, column, -dps * multiplier);
            lp.setCostUnsafe(column, minimizeTime ? -1 : dps * multiplier);
            lp.setSpellDpsUnsafe(column, cycle.getSpellDamagePerSecond() * multiplier);
        } else {
            lp.setElementUnsafe(rowTargetDamage, column, -cycle.getDamagePerSecond() * multiplier);
            lp.setCostUnsafe(column, minimizeTime ? -1 : cycle.getDamagePerSecond() * multiplier);
        }

        for (int i = 0; i < rowStackingConstraintCount; i++) {
            int effects = rowStackingConstraint[i].effect1.mask | rowStackingConstraint[i].effect2.mask;
            if ((effects & state.getEffects()) == effects) {
                lp.setElementUnsafe(rowStackingConstraint[i].row, column, 1.0);
            }
        }

        if (segmentCooldowns) {
            bound = setSpellColumnSegment(segment, manaSegment, state, column, cycle, bound, manaRegen);
        }
        if (restrictManaUse) {
            for (int ss = segment * manaSegments + manaSegment; ss < this.segmentList.size() * manaSegments - 1; ss++) {
                lp.setElementUnsafe(rowSegmentManaUnderflow + ss, column, manaRegen);
                lp.setElementUnsafe(rowSegmentManaOverflow + ss, column, -manaRegen);
            }
        }
        lp.setColumnUpperBound(column, bound);
    }

    private double setSpellColumnSegment(int segment, int manaSegment, CastingState state, int column, Cycle cycle, double bound, double manaRegen) {
        if (state.arcanePower) {
            bound = Math.min(bound, this.arcanePowerDuration);
            for (SegmentConstraint constraint : rowSegmentArcanePower) {
                if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
            }
        }
        if (state.powerInfusion) {
            bound = Math.min(bound, PowerInfusionDuration);
            for (SegmentConstraint constraint : rowSegmentPowerInfusion) {
                if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
            }
        }
        if (state.icyVeins) {
            bound = Math.min(bound, (coldsnapAvailable) ? 40.0 : 20.0);
            for (SegmentConstraint constraint : rowSegmentIcyVeins) {
                if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
            }
        }
        if (state.waterElemental) {
            for (SegmentConstraint constraint : rowSegmentWaterElemental) {
                if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
            }
        }
        if (state.mirrorImage) {
            for (SegmentConstraint constraint : rowSegmentMirrorImage) {
                if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
            }
        }
        if (state.combustion) {
            for (SegmentConstraint constraint : rowSegmentCombustion) {
                if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
            }
        }
        if (state.berserking) {
            for (SegmentConstraint constraint : rowSegmentBerserking) {
                if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
            }
        }
        if (state.flameCap) {
            for (SegmentConstraint constraint : rowSegmentFlameCap) {
                if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
            }
        }
        if (state.potionOfWildMagic || state.potionOfSpeed) {
            bound = Math.min(bound, 15.0);
        }
        for (int i = 0; i < this.itemBasedEffectCooldownsCount; i++) {
            EffectCooldown cooldown = this.itemBasedEffectCooldowns[i];
            if (state.effectsActive(cooldown.mask)) {
                bound = Math.min(bound, cooldown.duration);
                for (SegmentConstraint constraint : cooldown.segmentConstraints) {
                    if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
                }
            }
        }
        if (state.manaGemEffect) {
            bound = Math.min(bound, this.manaGemEffectDuration);
            for (SegmentConstraint constraint : rowSegmentManaGemEffect) {
                if (segment >= constraint.minSegment && segment <= constraint.maxSegment) lp.setElementUnsafe(constraint.row, column, 1.0);
            }
        }
        if (segmentNonCooldowns || state != this.baseState) {
            bound = Math.min(bound, this.segmentList.get(segment).duration);
            lp.setElementUnsafe(rowSegment + segment, column, 1.0);
        }
        if (restrictThreat) {
            for (int ss = segment; ss < this.segmentList.size() - 1; ss++) {
                lp.setElementUnsafe(rowSegmentThreat + ss, column, cycle.getThreatPerSecond());
            }
        }
        return bound;
    }

    private void generateSpellList() {
        if (spellList == null) {
            spellList = new ArrayList<CycleId>();
        } else {
            spellList.clear();
        }

        if (this.calculationOptions.getCustomSpellMixEnabled() || this.calculationOptions.getCustomSpellMixOnly()) {
            spellList.add(CycleId.CustomSpellMix);
        }
        if (!this.calculationOptions.getCustomSpellMixOnly()) {
            if (this.calculationOptions.getMaintainScorch() && this.calculationOptions.getMaintainSnare() && this.mageTalents.getImprovedScorch() > 0 && this.mageTalents.getSlow() > 0) {
                // no cycles right now that provide scorch and snare
            }
            if (this.calculationOptions.getMaintainScorch() && this.mageTalents.getImprovedScorch() > 0) {
                if (useGlobalOptimizations) {
                    if (this.mageTalents.getPiercingIce() == 3 && this.mageTalents.getIceShards() == 3 && this.calculationOptions.getPlayerLevel() >= 75) {
                        if (this.mageTalents.getLivingBomb() > 0) {
                            spellList.add(CycleId.FFBScLBPyro);
                        }
                        spellList.add(CycleId.FFBScPyro);
                    } else {
                        if (this.mageTalents.getLivingBomb() > 0) {
                            spellList.add(CycleId.FBScLBPyro);
                        }
                        spellList.add(CycleId.FBScPyro);
                    }
                } else {
                    if (this.mageTalents.getLivingBomb() > 0) {
                        spellList.add(CycleId.FBScLBPyro);
                        spellList.add(CycleId.ScLBPyro);
                    }
                    spellList.add(CycleId.FBScPyro);
                    if (this.calculationOptions.getPlayerLevel() >= 75) {
                        spellList.add(CycleId.FFBScPyro);
                        if (this.mageTalents.getLivingBomb() > 0) {
                            spellList.add(CycleId.FFBScLBPyro);
                        }
                    }
                }
            } else {
                if (useGlobalOptimizations) {
                    if (this.mageTalents.getEmpoweredFire() > 0) {
                        if (this.mageTalents.getPiercingIce() == 3 && this.mageTalents.getIceShards() == 3 && this.calculationOptions.getPlayerLevel() >= 75) {
                            spellList.add(CycleId.FFBPyro);
                            if (this.mageTalents.getLivingBomb() > 0) spellList.add(CycleId.FFBLBPyro);
                        } else {
                            if (this.mageTalents.getHotStreak() > 0 && this.mageTalents.getPyroblast() > 0) {
                                spellList.add(CycleId.FBPyro);
                            } else {
                                spellList.add(CycleId.Fireball);
                            }
                            if (this.mageTalents.getLivingBomb() > 0) spellList.add(CycleId.FBLBPyro);
                        }
                    } else if (this.mageTalents.getEmpoweredFrostbolt() > 0) {
                        if (this.mageTalents.getBrainFreeze() > 0) {
                            spellList.add(CycleId.FrBFB);
                            spellList.add(CycleId.FrBFBIL);
                            spellList.add(CycleId.FrBILFB);
                        }
                        if (this.mageTalents.getDeepFreeze() > 0) {
                            spellList.add(CycleId.FrBDFFBIL);
                            spellList.add(CycleId.FrBDFFFB);
                        }
                        spellList.add(CycleId.FrBIL);
                        spellList.add(CycleId.FrostboltFOF);
                    } else if (this.mageTalents.getArcaneEmpowerment() > 0) {
                        spellList.add(CycleId.AB2AM);
                        spellList.add(CycleId.AB3AM023MBAM);
                        spellList.add(CycleId.AB4AM0234MBAM);
                        if (this.mageTalents.getMissileBarrage() > 0) {
                            spellList.add(CycleId.ABSpam0234MBAM);
                            spellList.add(CycleId.ABSpam024MBAM);
                            spellList.add(CycleId.ABSpam034MBAM);
                            spellList.add(CycleId.ABSpam04MBAM);
                        }
                        spellList.add(CycleId.ArcaneBlastSpam);
                    } else {
                        spellList.add(CycleId.ArcaneMissiles);
                        spellList.add(CycleId.Fireball);
                        spellList.add(CycleId.FrostboltFOF);
                        if (this.calculationOptions.getPlayerLevel() >= 75) spellList.add(CycleId.FrostfireBoltFOF);
                    }
                } else {
                    spellList.add(CycleId.ArcaneMissiles);
                    spellList.add(CycleId.Scorch);
                    if (this.mageTalents.getLivingBomb() > 0) spellList.add(CycleId.ScLBPyro);
                    if (this.mageTalents.getHotStreak() > 0 && this.mageTalents.getPyroblast() > 0) {
                        spellList.add(CycleId.FBPyro);
                    } else {
                        spellList.add(CycleId.Fireball);
                    }
                    if (this.calculationOptions.getPlayerLevel() >= 75) {
                        spellList.add(CycleId.FrostfireBoltFOF);
                        spellList.add(CycleId.FFBPyro);
                        if (this.mageTalents.getLivingBomb() > 0) spellList.add(CycleId.FFBLBPyro);
                    }
                    if (this.mageTalents.getLivingBomb() > 0) spellList.add(CycleId.FBLBPyro);
                    spellList.add(CycleId.FrostboltFOF);
                    if (this.mageTalents.getBrainFreeze() > 0) spellList.add(CycleId.FrBFB);
                    if (this.mageTalents.getFingersOfFrost() > 0) {
                        if (this.mageTalents.getBrainFreeze() > 0) {
                            spellList.add(CycleId.FrBFBIL);
                            spellList.add(CycleId.FrBILFB);
                        }
                        spellList.add(CycleId.FrBIL);
                        if (this.mageTalents.getDeepFreeze() > 0) {
                            spellList.add(CycleId.FrBDFFBIL);
                            spellList.add(CycleId.FrBDFFFB);
                        }
                    }
                    spellList.add(CycleId.AB2AM);
                    spellList.add(CycleId.AB3AM023MBAM);
                    spellList.add(CycleId.AB4AM0234MBAM);
                    if (this.mageTalents.getMissileBarrage() > 0) {
                        spellList.add(CycleId.ABSpam0234MBAM);
                        spellList.add(CycleId.ABSpam024MBAM);
                        spellList.add(CycleId.ABSpam034MBAM);
                        spellList.add(CycleId.ABSpam04MBAM);
                    }
                    spellList.add(CycleId.ArcaneBlastSpam);
                }
            }
            if (this.calculationOptions.getAoeDuration() > 0) {
                spellList.add(CycleId.ArcaneExplosion);
                spellList.add(CycleId.FlamestrikeSpammed);
                spellList.add(CycleId.FlamestrikeSingle);
                spellList.add(CycleId.Blizzard);
                spellList.add(CycleId.ConeOfCold);
                if (this.mageTalents.getBlastWave() == 1) spellList.add(CycleId.BlastWave);
                if (this.mageTalents.getDragonsBreath() == 1) spellList.add(CycleId.DragonsBreath);
            }
        }
    }

    // http://tekpool.wordpress.com/category/bit-count/
    int bitCount(int i) {
        int u = i;
        int uCount = 0;

        while (u != 0) {
            uCount++;
            u &= (u - 1);
        }

        return uCount;
    }

    boolean bitCount2(int i) {
        int u = i;

        if (u == 0) return false;
        u &= (u - 1);
        return u != 0;
    }

    private void generateStateList() {
        this.baseState = CastingState.createNew(this, 0, false, 0);

        if (stateList == null) {
            stateList = new ArrayList<CastingState>(64);
        } else {
            stateList.clear();
        }
        if (useIncrementalOptimizations) {
            int[] sortedStates = this.calculationOptions.getIncrementalSetSortedStates();
            for (int incrementalSortedIndex = 0; incrementalSortedIndex < sortedStates.length; incrementalSortedIndex++) {
                // incremental index is filtered by non-item based cooldowns
                int incrementalSetIndex = sortedStates[incrementalSortedIndex];
                boolean mf = (incrementalSetIndex & StandardEffect.MoltenFury) != 0;
                boolean heroism = (incrementalSetIndex & StandardEffect.Heroism) != 0;
                int itemBasedMax = 1 << this.itemBasedEffectCooldownsCount;
                for (int index = 0; index < itemBasedMax; index++) {
                    int combinedIndex = incrementalSetIndex | (index << standardEffectCount);
                    if ((combinedIndex & availableCooldownMask) == combinedIndex) { // make sure all are available
                        boolean valid = true;
                        for (int exclusionMask : effectExclusionList) {
                            if (bitCount2(combinedIndex & exclusionMask)) {
                                valid = false;
                                break;
                            }
                        }
                        if (valid) {
                            if ((this.calculationOptions.getHeroismControl() != 1 || !heroism || !mf) && (this.calculationOptions.getHeroismControl() != 2 || !heroism || (combinedIndex == rawr.models.mage.StandardEffect.Heroism && index == 0)) && (this.calculationOptions.getHeroismControl() != 3 || !moltenFuryAvailable || !heroism || mf)) {
                                if (combinedIndex == 0) {
                                    stateList.add(this.baseState);
                                } else {
                                    stateList.add(CastingState.createNew(this, combinedIndex, false, 0));
                                }
                            }
                        }
                    }
                }
            }
        } else {
            for (int incrementalSetIndex = 0; incrementalSetIndex <= availableCooldownMask; incrementalSetIndex++) {
                if (((incrementalSetIndex) & availableCooldownMask) == (incrementalSetIndex)) { // make sure all are available
                    boolean valid = true;
                    for (int exclusionMask : effectExclusionList) {
                        if (bitCount2(incrementalSetIndex & exclusionMask)) {
                            valid = false;
                            break;
                        }
                    }
                    if (valid) {
                        boolean mf = (incrementalSetIndex & StandardEffect.MoltenFury) != 0;
                        boolean heroism = (incrementalSetIndex & StandardEffect.Heroism) != 0;
                        if ((this.calculationOptions.getHeroismControl() != 1 || !heroism || !mf) && (this.calculationOptions.getHeroismControl() != 2 || !heroism || (incrementalSetIndex == rawr.models.mage.StandardEffect.Heroism)) && (this.calculationOptions.getHeroismControl() != 3 || !moltenFuryAvailable || !heroism || mf)) {
                            if (incrementalSetIndex == 0) {
                                stateList.add(this.baseState);
                            } else {
                                stateList.add(CastingState.createNew(this, incrementalSetIndex, false, 0));
                            }
                        }
                    }
                }
            }
        }
    }

    // Quadratic Solver
    private void solveQuadratic() {
        // for now requires solution variables to work
        Integer[] sort = new Integer[lp.getColumns()];
        for (int j = 0; j < lp.getColumns(); j++) {
            sort[j] = j;
        }
        Arrays.sort(sort, new Comparator<Integer>() {
            @Override
            public int compare(Integer x, Integer y) {
                SolutionVariable vx = solutionVariable.get(x);
                SolutionVariable vy = solutionVariable.get(y);
                if (vx.state != null && vx.state.evocation && vy.type == VariableType.ManaOverflow && vx.segment == vy.segment && vx.manaSegment == vy.manaSegment + 1) {
                    return -1;
                }
                if (vy.state != null && vy.state.evocation && vx.type == VariableType.ManaOverflow && vx.segment == vy.segment && vy.manaSegment == vx.manaSegment + 1) {
                    return 1;
                }
                int comp = Integer.compare(vx.segment, vy.segment);
                if (comp != 0) return comp;
                comp = Integer.compare(vx.manaSegment, vy.manaSegment);
                if (comp != 0) return comp;
                if (vx.dps == 0.0) {
                    if (vy.dps == 0.0) {
                        return Double.compare(vx.mps, vy.mps);
                    } else {
                        if (vx.mps <= 0) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                } else if (vy.dps == 0.0) {
                    if (vy.mps <= 0) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
                return Double.compare(vx.mps, vy.mps);
            }
        });

        int[] sortArr = new int[sort.length];
        for (int i = 0; i < sort.length; i++) sortArr[i] = sort[i];
        lp.solvePrimalQuadratic(rowManaRegen, sortArr, this.manaAdeptBonus / (this.baseStats.getMana() * ManaRegenLPScaling), useIncrementalOptimizations, this.calculationOptions.getTargetDamage() > 0 ? lp.getColumns() + rowFightDuration : -1, this.calculationOptions.getTargetDamage());
    }

    // Calculation Result
    private DisplayCalculations getDisplayCalculations(CharacterCalculationsMage baseCalculations) {
        DisplayCalculations displayCalculations = new DisplayCalculations();

        displayCalculations.setBaseStats(this.baseStats);
        displayCalculations.setBaseState(this.baseState);
        displayCalculations.setCharacter(this.character);
        displayCalculations.setCalculationOptions(this.calculationOptions);
        displayCalculations.solutionVariable = this.solutionVariable;
        displayCalculations.setMageTalents(this.mageTalents);
        displayCalculations.solution = solution;
        displayCalculations.setBaseCalculations(baseCalculations);

        displayCalculations.setRawArcaneHitRate(this.rawArcaneHitRate);
        displayCalculations.setRawFireHitRate(this.rawFireHitRate);
        displayCalculations.setRawFrostHitRate(this.rawFrostHitRate);

        displayCalculations.setCooldownList(new ArrayList<EffectCooldown>(this.cooldownList));
        displayCalculations.setEffectCooldown(new LinkedHashMap<Integer, EffectCooldown>(this.effectCooldown));
        displayCalculations.setItemBasedEffectCooldowns(new EffectCooldown[this.itemBasedEffectCooldownsCount]);
        System.arraycopy(this.itemBasedEffectCooldowns, 0, displayCalculations.getItemBasedEffectCooldowns(), 0, this.itemBasedEffectCooldownsCount);

        displayCalculations.setSegmentList(new ArrayList<Segment>(this.segmentList));

        displayCalculations.setSpellPowerEffects(new SpecialEffect[this.spellPowerEffectsCount]);
        System.arraycopy(this.spellPowerEffects, 0, displayCalculations.getSpellPowerEffects(), 0, this.spellPowerEffectsCount);
        displayCalculations.setHasteRatingEffects(new SpecialEffect[this.hasteRatingEffectsCount]);
        System.arraycopy(this.hasteRatingEffects, 0, displayCalculations.getHasteRatingEffects(), 0, this.hasteRatingEffectsCount);

        displayCalculations.setBaseGlobalCooldown(this.baseGlobalCooldown);
        displayCalculations.setManaAdeptBonus(this.manaAdeptBonus);

        displayCalculations.evocationDuration = this.evocationDuration;
        displayCalculations.evocationRegen = this.evocationRegen;
        displayCalculations.evocationDurationIV = this.evocationDurationIV;
        displayCalculations.evocationRegenIV = this.evocationRegenIV;
        displayCalculations.evocationDurationHero = this.evocationDurationHero;
        displayCalculations.evocationRegenHero = this.evocationRegenHero;
        displayCalculations.evocationDurationIVHero = this.evocationDurationIVHero;
        displayCalculations.evocationRegenIVHero = this.evocationRegenIVHero;

        displayCalculations.maxManaGem = this.maxManaGem;
        displayCalculations.maxEvocation = this.maxEvocation;
        displayCalculations.manaGemTps = this.manaGemTps;
        displayCalculations.manaPotionTps = this.manaPotionTps;
        displayCalculations.manaGemValue = this.manaGemValue;
        displayCalculations.maxManaGemValue = this.maxManaGemValue;
        displayCalculations.manaPotionValue = this.manaPotionValue;
        displayCalculations.maxManaPotionValue = this.maxManaPotionValue;

        displayCalculations.combustionCooldown = CombustionCooldown;
        displayCalculations.powerInfusionDuration = PowerInfusionDuration;
        displayCalculations.powerInfusionCooldown = PowerInfusionCooldown;
        displayCalculations.mirrorImageDuration = MirrorImageDuration;
        displayCalculations.mirrorImageCooldown = MirrorImageCooldown;
        displayCalculations.icyVeinsCooldown = this.icyVeinsCooldown;
        displayCalculations.coldsnapCooldown = this.coldsnapCooldown;
        displayCalculations.arcanePowerCooldown = this.arcanePowerCooldown;
        displayCalculations.arcanePowerDuration = this.arcanePowerDuration;
        displayCalculations.waterElementalCooldown = this.waterElementalCooldown;
        displayCalculations.waterElementalDuration = this.waterElementalDuration;
        displayCalculations.evocationCooldown = this.evocationCooldown;
        displayCalculations.manaGemEffectDuration = this.manaGemEffectDuration;

        displayCalculations.setStartingMana(this.startingMana);
        displayCalculations.setConjureManaGem(this.conjureManaGemCycle);
        displayCalculations.setMaxConjureManaGem(this.maxConjureManaGem);
        displayCalculations.setManaGemEffect(manaGemEffectAvailable);
        displayCalculations.setChanceToDie(this.chanceToDie);
        displayCalculations.setMeanIncomingDps(this.meanIncomingDps);
        displayCalculations.setMageArmor(armor);
        displayCalculations.setDamageTakenReduction(this.damageTakenReduction);
        displayCalculations.setWand(this.wand);

        if (!requiresMIP) {
            displayCalculations.upperBound = lp.getValue();
            displayCalculations.lowerBound = 0.0;
        } else {
            displayCalculations.upperBound = upperBound;
            if (integralMana && segmentCooldowns && advancedConstraintsLevel >= 5) displayCalculations.lowerBound = lowerBound;
        }

        float threat = 0;
        for (int i = 0; i < this.solutionVariable.size(); i++) {
            threat += (float) (this.solutionVariable.get(i).tps * solution[i]);
        }
        displayCalculations.tps = threat / this.calculationOptions.getFightDuration();

        return displayCalculations;
    }

    private OptimizableCalculations getOptimizableCalculations() {
        OptimizableCalculations optimizableCalculations = new OptimizableCalculations();

        optimizableCalculations.setArcaneResistance(this.baseStats.getArcaneResistance());
        optimizableCalculations.setChanceToDie(this.chanceToDie);
        optimizableCalculations.setFireResistance(this.baseStats.getFireResistance());
        optimizableCalculations.setFrostResistance(this.baseStats.getFrostResistance());
        optimizableCalculations.setHasteRating(this.baseStats.getHasteRating());
        optimizableCalculations.setHealth(this.baseStats.getHealth());
        optimizableCalculations.setHitRating(this.baseStats.getHitRating());
        optimizableCalculations.setMovementSpeed(this.baseStats.getMovementSpeed());
        optimizableCalculations.setNatureResistance(this.baseStats.getNatureResistance());
        optimizableCalculations.setPVPTrinket(this.baseStats.getPVPTrinket());
        optimizableCalculations.setResilience(this.baseStats.getResilience());
        optimizableCalculations.setShadowResistance(this.baseStats.getShadowResistance());

        return optimizableCalculations;
    }

    private CharacterCalculationsMage getCalculationsResult() {
        CharacterCalculationsMage calculationResult = new CharacterCalculationsMage();

        if (this.calculationOptions.getTargetDamage() > 0) {
            calculationResult.getSubPoints()[0] = -(float) (this.calculationOptions.getTargetDamage() / solution[solution.length - 1]);
        } else {
            calculationResult.getSubPoints()[0] = ((float) solution[solution.length - 1] /*+ calculationResult.WaterElementalDamage*/) / this.calculationOptions.getFightDuration();
        }
        calculationResult.getSubPoints()[1] = evaluateSurvivability();
        calculationResult.setOverallPoints(calculationResult.getSubPoints()[0] + calculationResult.getSubPoints()[1]);

        if (this.needsDisplayCalculations) {
            calculationResult.setDisplayCalculations(getDisplayCalculations(calculationResult));
        }
        calculationResult.setOptimizableCalculations(getOptimizableCalculations());

        if (autoActivatedBuffs != null) {
            calculationResult.getAutoActivatedBuffs().addAll(autoActivatedBuffs);
        }

        this.solutionVariable = null;

        return calculationResult;
    }

    // Survivability
    public void calculateChanceToDie() {
        double ampMelee = this.incomingDamageAmpMelee;
        double ampPhysical = this.incomingDamageAmpPhysical;
        double ampArcane = this.incomingDamageAmpArcane;
        double ampFire = this.incomingDamageAmpFire;
        double ampFrost = this.incomingDamageAmpFrost;
        double ampNature = this.incomingDamageAmpNature;
        double ampShadow = this.incomingDamageAmpShadow;
        double ampHoly = this.incomingDamageAmpHoly;

        double melee = this.incomingDamageDpsMelee;
        double physical = this.incomingDamageDpsPhysical;
        double arcane = this.incomingDamageDpsArcane;
        double fire = this.incomingDamageDpsFire;
        double frost = this.incomingDamageDpsFrost;
        double nature = this.incomingDamageDpsNature;
        double shadow = this.incomingDamageDpsShadow;
        double holy = this.incomingDamageDpsHoly;

        double burstWindow = this.calculationOptions.getBurstWindow();
        double burstImpacts = this.calculationOptions.getBurstImpacts();

        // B(n, p) ~ N(np, np(1-p))
        // n = burstImpacts
        // Xi ~ ampi * (dpsi * (1 + B(n, criti) / n * critMulti) + doti)
        //    ~ ampi * (dpsi * (1 + N(n * criti, n * criti * (1 - criti)) / n * critMulti) + doti)
        //    ~ N(ampi * (doti + dpsi * (1 + critMulti * criti)), ampi^2 * dpsi^2 * critMulti^2 / n * criti * (1 - criti))
        // X = sum Xi ~ N(sum ampi * (doti + dpsi * (1 + critMulti * criti)), sum ampi^2 * dpsi^2 * critMulti^2 / n * criti * (1 - criti))
        // H = Health + hp5 / 5 * burstWindow
        // P(burstWindow * sum Xi >= H) = 1 - P(burstWindow * sum Xi <= H) = 1 / 2 * (1 - Erf((H - mu) / (sigma * sqrt(2)))) =
        //                = 1 / 2 * (1 - Erf((H / burstWindow - [sum ampi * (doti + dpsi * (1 + critMulti * criti))]) / sqrt(2 * [sum ampi^2 * dpsi^2 * critMulti^2 / n * criti * (1 - criti)])))

        double meleeVar = Math.pow(ampMelee * this.calculationOptions.getMeleeDps() * (2 * (1 - this.baseState.getCritDamageReduction()) - 1), 2) / burstImpacts * Math.max(0, this.calculationOptions.getMeleeCrit() / 100.0 - this.baseState.getPhysicalCritReduction()) * (1 - Math.max(0, this.calculationOptions.getMeleeCrit() / 100.0 - this.baseState.getPhysicalCritReduction()));
        double physicalVar = Math.pow(ampPhysical * this.calculationOptions.getPhysicalDps() * (2 * (1 - this.baseState.getCritDamageReduction()) - 1), 2) / burstImpacts * Math.max(0, this.calculationOptions.getPhysicalCrit() / 100.0 - this.baseState.getPhysicalCritReduction()) * (1 - Math.max(0, this.calculationOptions.getPhysicalCrit() / 100.0 - this.baseState.getPhysicalCritReduction()));
        double arcaneVar = Math.pow(ampArcane * this.calculationOptions.getArcaneDps() * (1.75 * (1 - this.baseState.getCritDamageReduction()) - 1), 2) / burstImpacts * Math.max(0, this.calculationOptions.getArcaneCrit() / 100.0 - this.baseState.getSpellCritReduction()) * (1 - Math.max(0, this.calculationOptions.getArcaneCrit() / 100.0 - this.baseState.getSpellCritReduction()));
        double fireVar = Math.pow(ampFire * this.calculationOptions.getFireDps() * (2.1 * (1 - this.baseState.getCritDamageReduction()) - 1), 2) / burstImpacts * Math.max(0, this.calculationOptions.getFireCrit() / 100.0 - this.baseState.getSpellCritReduction()) * (1 - Math.max(0, this.calculationOptions.getFireCrit() / 100.0 - this.baseState.getSpellCritReduction()));
        double frostVar = Math.pow(ampFrost * this.calculationOptions.getFrostDps() * (2 * (1 - this.baseState.getCritDamageReduction()) - 1), 2) / burstImpacts * Math.max(0, this.calculationOptions.getFrostCrit() / 100.0 - this.baseState.getSpellCritReduction()) * (1 - Math.max(0, this.calculationOptions.getFrostCrit() / 100.0 - this.baseState.getSpellCritReduction()));
        double holyVar = Math.pow(ampHoly * this.calculationOptions.getHolyDps() * (1.5 * (1 - this.baseState.getCritDamageReduction()) - 1), 2) / burstImpacts * Math.max(0, this.calculationOptions.getHolyCrit() / 100.0 - this.baseState.getSpellCritReduction()) * (1 - Math.max(0, this.calculationOptions.getHolyCrit() / 100.0 - this.baseState.getSpellCritReduction()));
        double natureVar = Math.pow(ampNature * this.calculationOptions.getNatureDps() * (2 * (1 - this.baseState.getCritDamageReduction()) - 1), 2) / burstImpacts * Math.max(0, this.calculationOptions.getNatureCrit() / 100.0 - this.baseState.getSpellCritReduction()) * (1 - Math.max(0, this.calculationOptions.getNatureCrit() / 100.0 - this.baseState.getSpellCritReduction()));
        double shadowVar = Math.pow(ampShadow * this.calculationOptions.getShadowDps() * (2 * (1 - this.baseState.getCritDamageReduction()) - 1), 2) / burstImpacts * Math.max(0, this.calculationOptions.getShadowCrit() / 100.0 - this.baseState.getSpellCritReduction()) * (1 - Math.max(0, this.calculationOptions.getShadowCrit() / 100.0 - this.baseState.getSpellCritReduction()));

        double Xmean = melee + physical + arcane + fire + frost + holy + nature + shadow;
        double Xvar = meleeVar + physicalVar + arcaneVar + fireVar + frostVar + holyVar + natureVar + shadowVar;

        // T = healing response time ~ N(Tmean, Tvar)
        // T * X ~ N(Tmean * Xmean, Tvar * Xvar + Tmean^2 * Xvar + Xmean^2 * Tvar)   // approximation reasonable for high Tmean / sqrt(Tvar)
        // P(T * X >= H) = 1 / 2 * (1 - Erf((H - mean) / (sigma * sqrt(2)))) =
        //               = 1 / 2 * (1 - Erf((H - mean) / sqrt(2 * var)))
        //               = 1 / 2 * (1 - Erf((H - Tmean * Xmean) / sqrt(2 * (Tvar * Xvar + Tmean^2 * Xvar + Xmean^2 * Tvar))))

        // Tvar := Tk * Tmean^2,   Tk <<< 1

        // P(T * X >= H) = 1 / 2 * (1 - Erf((H / Tmean - Xmean) / sqrt(2 * (Xvar * (Tk + 1) + Xmean^2 * Tk))))

        double Tk = 0.01;

        this.chanceToDie = (float) (0.5f * (1f - SpecialFunction.erf((this.baseStats.getHealth() / burstWindow + this.baseStats.getHp5() / 5 - Xmean) / Math.sqrt(2 * (Xvar * (1 + Tk) + Xmean * Xmean * Tk)))));
        this.meanIncomingDps = (float) Xmean;
    }

    private float evaluateSurvivability() {
        float ret = this.baseStats.getHealth() * this.calculationOptions.getSurvivabilityRating();
        if (this.calculationOptions.getChanceToLiveScore() > 0 || this.needsDisplayCalculations) {
            calculateChanceToDie();

            ret += (float) (this.calculationOptions.getChanceToLiveScore() * Math.pow(1 - this.chanceToDie, this.calculationOptions.getChanceToLiveAttenuation()));
            if (Float.isNaN(ret)) ret = 0f;
        } else {
            this.chanceToDie = 0f;
            this.meanIncomingDps = 0f;
        }
        return ret;
    }

    // MIP
    private Heap<SolverLP> heap;
    private BranchNode currentNode;

    private List<Integer>[] hexList;
    private double[][] segmentCooldownCount;
    private double[] manaList;
    private double[] segmentFilled;
    private int[] hexMask;

    private ResolutionMapping[] lowResolution;
    private ResolutionMapping[] highResolution;
    private ResolutionMapping[] resolution;

    @SuppressWarnings("unchecked")
    private static List<Integer>[] newIntListArray(int size) {
        return new List[size];
    }

    @SuppressWarnings("unchecked")
    private static ResolutionMapping[] newResolutionArray(int size) {
        ResolutionMapping[] arr = new ResolutionMapping[size];
        for (int i = 0; i < size; i++) arr[i] = new ResolutionMapping();
        return arr;
    }

    private double[][] newDoubleArrayArray(int size) {
        return new double[size][];
    }

    private void computeResolutionMaps() {
        double res = 30.0;
        lowResolution = newResolutionArray(this.segmentList.size());
        for (int i = 0; i < this.segmentList.size(); i++) {
            int max = findLastIndexTimeEndWithin(this.segmentList.get(i).timeStart, res);
            if (max < i) max = i;
            for (int j = i; j <= max; j++) {
                lowResolution[j].minSegment = i;
                lowResolution[j].maxSegment = max;
            }
            i = max;
        }

        highResolution = newResolutionArray(this.segmentList.size());
        for (int i = 0; i < this.segmentList.size(); i++) {
            highResolution[i].minSegment = i;
            highResolution[i].maxSegment = i;
        }

        resolution = highResolution;
    }

    private int findLastIndexTimeEndWithin(double start, double res) {
        for (int i = this.segmentList.size() - 1; i >= 0; i--) {
            if (this.segmentList.get(i).getTimeEnd() - start <= res + 0.00001) return i;
        }
        return -1;
    }

    private void restrictSolution() {
        lp.log = new StringBuilder();

        computeResolutionMaps();

        switch (this.calculationOptions.getMIPMethod()) {
            case BestBound:
                bestBoundSearch();
                break;
            case DepthFirst:
                depthFirstSearch();
                break;
            case HybridUnguided:
                hybridSearch(false);
                break;
            case HybridGuided:
                hybridSearch(true);
                break;
        }
    }

    private void hybridSearch(boolean guided) {
        int sizeLimit = this.calculationOptions.getMaxHeapLimit();
        if (!needsQuadratic) {
            lp.solvePrimalDual(); // solve primal and recalculate to get a stable starting point
        }

        int[] round = new int[1];

        upperBound = lp.getValue();
        lowerBound = this.calculationOptions.getLowerBoundHint();

        SolverLP[] incumbent = new SolverLP[1];

        LinkedList<BranchNode> leafNodes = new LinkedList<BranchNode>();

        SolverLP rootCopy = lp.clone();
        BranchNode root = new BranchNode();
        root.lp = lp;
        probeDive(root, round, incumbent, sizeLimit, guided);

        leafNodes.addFirst(root);
        root = null;

        do {
            double weight = (double) round[0] / sizeLimit;
            double maxValue = Double.NEGATIVE_INFINITY;
            double maxWeightedValue = Double.NEGATIVE_INFINITY;
            BranchNode bestNode = null;

            ListIterator<BranchNode> it = leafNodes.listIterator();
            while (it.hasNext()) {
                BranchNode node = it.next();
                if (node.getLpValue() < lowerBound + 0.00001) {
                    node.releaseConstraints();
                    it.remove();
                } else {
                    if (node.probeValue != 0.0 && node.probeValue < lowerBound + 0.00001) {
                        BranchNode drop = node;
                        while (drop != null) {
                            while (drop.children.size() > 0 && drop.children.get(0).getLpValue() < lowerBound + 0.00001) {
                                drop.children.get(0).releaseConstraints();
                                drop.children.remove(0);
                            }
                            if (drop.children.size() > 0) {
                                drop = drop.children.get(0);
                            } else {
                                drop = null;
                            }
                        }
                    }
                    if (node.getLpValue() > maxValue) maxValue = node.getLpValue();
                    double weightedValue = weight * node.getLpValue() + (1 - weight) * node.probeValue;
                    if (weightedValue > maxWeightedValue) {
                        maxWeightedValue = weightedValue;
                        bestNode = node;
                    }
                }
            }

            if (bestNode != null) {
                if (maxValue < upperBound - 0.00001) {
                    upperBound = maxValue;
                    CalculationsMage.log(this, "Upper bound lowered to " + upperBound + " at round " + round[0]);
                }
                // the node was already probed, so at the very least we have the child nodes
                // probe each child and add them to leaf nodes
                leafNodes.remove(bestNode);
                for (BranchNode child : bestNode.children) {
                    child.parent = null;
                    if (child.getLpValue() > lowerBound + 0.00001) {
                        probeDive(child, round, incumbent, sizeLimit, guided);
                        leafNodes.addLast(child);
                    } else {
                        child.releaseConstraints();
                    }
                }
            }
        } while (round[0] < sizeLimit && leafNodes.size() > 0 && !cancellationPending);

        if (round[0] < sizeLimit && !cancellationPending) CalculationsMage.log(this, "Full search complete at round " + round[0]);

        if (leafNodes.size() == 0 && incumbent[0] != null) {
            upperBound = lowerBound;
        }

        lp = incumbent[0];
        if (lp == null) {
            lp = rootCopy;
            lowerBound = 0.0;
        }
        solution = lp.solve();
        currentNode = null;
    }

    private void depthFirstSearch() {
        int sizeLimit = this.calculationOptions.getMaxHeapLimit();
        if (!needsQuadratic) {
            lp.solvePrimalDual(); // solve primal and recalculate to get a stable starting point
        }

        upperBound = lp.getValue();
        lowerBound = this.calculationOptions.getLowerBoundHint();

        SolverLP rootCopy = lp.clone();
        BranchNode root = new BranchNode();
        root.lp = lp;

        currentNode = root;

        SolverLP[] incumbent = new SolverLP[1];

        int highestBacktrack = Integer.MAX_VALUE;

        int[] round = new int[1];
        do {
            double value = (currentNode.lp != null) ? currentNode.lp.getValue() : currentNode.value; // force evaluation and get value
            currentNode.value = value;
            if (value < lowerBound + 0.00001) {
                // prune this, free space by removing from parent, backtrack
                if (currentNode.parent == null) break;
                do {
                    currentNode = currentNode.parent;
                    currentNode.children.get(currentNode.index).releaseConstraints();
                    currentNode.children.set(currentNode.index, null);
                    currentNode.index++;
                    if (currentNode.depth < highestBacktrack) {
                        highestBacktrack = currentNode.depth;
                        CalculationsMage.log(this, "Backtrack at " + highestBacktrack + ", value = " + lowerBound + ", root = " + currentNode.value + ", round = " + round[0]);
                    }
                } while (currentNode.index >= currentNode.children.size() && currentNode.parent != null);
                if (currentNode.index >= currentNode.children.size()) {
                    CalculationsMage.log(this, "Full search complete at round = " + round[0]);
                    upperBound = lowerBound;
                    break; // we explored the whole search space
                }
                currentNode = currentNode.children.get(currentNode.index);
            } else {
                lp = currentNode.lp;
                if (lp != null) {
                    solution = lp.solve();
                    round[0]++;
                    boolean valid = isLpValid();
                    if (valid) {
                        // we found a new lower bound
                        lowerBound = value;
                        incumbent[0] = lp;
                        // now backtrack
                        if (currentNode.parent != null) {
                            do {
                                currentNode = currentNode.parent;
                                currentNode.children.get(currentNode.index).releaseConstraints();
                                currentNode.children.set(currentNode.index, null);
                                currentNode.index++;
                                if (currentNode.depth < highestBacktrack) {
                                    highestBacktrack = currentNode.depth;
                                    CalculationsMage.log(this, "Backtrack at " + highestBacktrack + ", value = " + lowerBound + ", root = " + currentNode.value + ", round = " + round[0]);
                                }
                            } while (currentNode.index >= currentNode.children.size() && currentNode.parent != null);
                        } else {
                            break;
                        }
                        if (currentNode.index >= currentNode.children.size()) break; // we explored the whole search space
                        currentNode = currentNode.children.get(currentNode.index);
                    } else {
                        BranchNode first = null;
                        for (BranchNode node : currentNode.children) {
                            probeDive(node, round, incumbent, sizeLimit, true);
                            if (node.probeValue > lowerBound - 0.00001) first = node;
                        }
                        if (first != null) {
                            currentNode.children.remove(first);
                            currentNode.children.add(0, first);
                        }
                        currentNode.lp = null; // current lp may be reused by one of its children
                        // evaluate child nodes
                        currentNode = currentNode.children.get(0);
                    }
                } else {
                    if (currentNode.children.size() > 0) {
                        BranchNode first = null;
                        for (BranchNode node : currentNode.children) {
                            probeDive(node, round, incumbent, sizeLimit, true);
                            if (node.probeValue > lowerBound - 0.00001) first = node;
                        }
                        if (first != null) {
                            currentNode.children.remove(first);
                            currentNode.children.add(0, first);
                        }
                        currentNode.lp = null; // current lp may be reused by one of its children
                        // evaluate child nodes
                        if (currentNode.children.size() == 0) {
                            // can happen, so just handle it (no it can't?)
                            if (currentNode.parent != null) {
                                do {
                                    currentNode = currentNode.parent;
                                    currentNode.children.get(currentNode.index).releaseConstraints();
                                    currentNode.children.set(currentNode.index, null);
                                    currentNode.index++;
                                    if (currentNode.depth < highestBacktrack) {
                                        highestBacktrack = currentNode.depth;
                                        CalculationsMage.log(this, "Backtrack at " + highestBacktrack + ", value = " + lowerBound + ", root = " + currentNode.value + ", round = " + round[0]);
                                    }
                                } while (currentNode.index >= currentNode.children.size() && currentNode.parent != null);
                            } else {
                                break;
                            }
                            if (currentNode.index >= currentNode.children.size()) break; // we explored the whole search space
                            currentNode = currentNode.children.get(currentNode.index);
                        } else {
                            currentNode = currentNode.children.get(0);
                        }
                    } else {
                        if (currentNode.parent != null) {
                            do {
                                currentNode = currentNode.parent;
                                currentNode.children.get(currentNode.index).releaseConstraints();
                                currentNode.children.set(currentNode.index, null);
                                currentNode.index++;
                                if (currentNode.depth < highestBacktrack) {
                                    highestBacktrack = currentNode.depth;
                                    CalculationsMage.log(this, "Backtrack at " + highestBacktrack + ", value = " + lowerBound + ", root = " + currentNode.value + ", round = " + round[0]);
                                }
                            } while (currentNode.index >= currentNode.children.size() && currentNode.parent != null);
                        } else {
                            break;
                        }
                        if (currentNode.index >= currentNode.children.size()) break; // we explored the whole search space
                        currentNode = currentNode.children.get(currentNode.index);
                    }
                }
            }
        } while (round[0] < sizeLimit && !cancellationPending);

        lp = incumbent[0];
        if (lp == null) {
            lp = rootCopy;
            lowerBound = 0.0;
        }
        solution = lp.solve();
        currentNode = null;
    }

    private void probeDive(BranchNode node, int[] round, SolverLP[] incumbent, int sizeLimit, boolean sortedDive) {
        if (node.probeValue != 0) return;
        BranchNode store = currentNode;
        currentNode = node;
        int probeRound = 0;
        do {
            double value = currentNode.lp != null ? currentNode.lp.getValue() : currentNode.value; // force evaluation and get value
            currentNode.value = value;
            if (value < lowerBound + 0.00001) {
                if (value == 0.0) {
                    value = -1.0; // if we have a value of 0 then all siblings have a value of 0, so this whole subtree is unfeasible, reprobe (but only in sorted dive)
                    currentNode.probeValue = value;
                    if (currentNode == node) {
                        currentNode = store;
                        return;
                    }
                    // go to the next unprobed child
                    currentNode = currentNode.parent;
                    boolean allProbed = true;
                    if (!sortedDive) {
                        for (BranchNode child : currentNode.children) {
                            if (child.probeValue == 0) {
                                allProbed = false;
                                currentNode = child;
                                break;
                            }
                        }
                    }
                    if (allProbed) {
                        // this node has all children with value of 0
                        currentNode.value = 0.0;
                        currentNode.probeValue = value;
                        currentNode.releaseConstraints();
                        currentNode.children.clear(); // clean out the trash, make it available for garbage collection
                        if (currentNode == node) {
                            currentNode = store;
                            return;
                        }
                        currentNode = currentNode.parent; // this node has to be reevaluated
                        // reprobe
                        if (sortedDive) Collections.sort(currentNode.children);
                        // evaluate child nodes
                        currentNode = currentNode.children.get(0);
                    }
                } else {
                    currentNode.probeValue = value;
                    while (currentNode != node) {
                        currentNode = currentNode.parent;
                        currentNode.probeValue = value;
                    }
                    currentNode = store;
                    return;
                }
            } else {
                lp = currentNode.lp;
                boolean valid = false;
                if (lp != null) {
                    solution = lp.solve();
                    round[0]++;
                    probeRound++;
                    valid = isLpValid();
                }
                if (valid) {
                    // we found a new lower bound
                    lowerBound = value;
                    incumbent[0] = lp;
                    CalculationsMage.log(this, "Probe value = " + lowerBound + ", root = " + node.value + ", round = " + round[0]);
                    currentNode.probeValue = value;
                    while (currentNode != node) {
                        currentNode = currentNode.parent;
                        currentNode.probeValue = value;
                    }
                    currentNode = store;
                    return;
                } else {
                    if (sortedDive) Collections.sort(currentNode.children);
                    currentNode.lp = null; // current lp may be reused by one of its children
                    // evaluate child nodes
                    currentNode = currentNode.children.get(0);
                }
            }
        } while (round[0] < sizeLimit && probeRound < 100 && !cancellationPending);
        // don't spend too much time on a single probe, hope another branch will give something
        currentNode = store;
    }

    private void bestBoundSearch() {
        int maxHeap = this.calculationOptions.getMaxHeapLimit();
        if (!needsQuadratic) {
            lp.solvePrimalDual(); // solve primal and recalculate to get a stable starting point
        }
        heap = new Heap<SolverLP>(HeapType.MaximumHeap);
        heapPush(lp);

        upperBound = lp.getValue();
        lowerBound = 0.0;

        boolean valid = true;
        do {
            if (heap.getHead().getValue() > upperBound + 0.001) // lowered instability threshold, in case it is still an issue just recompute the solution which "should" give a stable result hopefully
            {
                // recovery measures first
                double current = heap.getHead().getValue();
                lp = heap.pop();
                lp.forceRecalculation();
                if (!needsQuadratic) {
                    lp.solvePrimalDual();
                }
                // some testing indicates that the recalculated solution gives the correct result, so the previous solution is most likely to be the problematic one, since we just discarded it not a big deal
                upperBound = lp.getValue();
                heapPush(lp);
                continue;
            }
            lp = heap.pop();
            if (lp.getValue() < upperBound - 0.00001) {
                upperBound = lp.getValue();
                CalculationsMage.log(this, "Upper bound lowered to " + upperBound + " at round " + heap.getCount());
            }
            // this is the best non-evaluated option (highest partially-constrained LP, the optimum has to be lower)
            // if this one is valid than all others are sub-optimal
            // validate all segments for each cooldown
            solution = lp.solve();
            if (heap.getCount() > maxHeap) {
                // WinForms MessageBox omitted; headless stop (DECISIONS #10)
                break;
            }
            valid = isLpValid();
        } while (heap.getCount() > 0 && !valid && !cancellationPending);
        if (valid) {
            CalculationsMage.log(this, "Full search complete at round = " + heap.getCount());
            lowerBound = upperBound;
        }
        heap = null;
    }

    private boolean isLpValid() {
        analyzeSolution();

        if (segmentCooldowns && advancedConstraintsLevel >= 1) {
            resolution = lowResolution;
            if (needsQuadratic) {
                // evocation
                if (evocationAvailable && !validateCooldown(StandardEffect.Evocation, this.evocationDuration, this.evocationCooldown, false, 0.0, rowSegmentEvocation, VariableType.None)) return false;
                if (evocationAvailable && this.calculationOptions.getEnableHastedEvocation()) {
                    if (!validateHastedEvocation()) return false;
                    if (icyVeinsAvailable && !validateActivation(StandardEffect.Evocation, VariableType.EvocationIV, this.evocationDurationIV, this.evocationCooldown, VariableType.EvocationIV, StandardEffect.Evocation | StandardEffect.IcyVeins)) return false;
                }
            }
        }

        if (integralMana) {
            if (manaPotionAvailable && !validateIntegralConsumableOverall(VariableType.ManaPotion, 1.0)) return false;
            if (this.calculationOptions.getManaGemEnabled() && !validateIntegralConsumableOverall(VariableType.ManaGem, 1.0)) return false;

            CastingState evoBaseState = this.baseState;
            if (this.calculationOptions.getEnable2T10Evocation() && this.baseStats.getMage2T10() > 0) {
                evoBaseState = this.baseState.getTier10TwoPieceState();
            }

            if (evocationAvailable && !validateIntegralConsumableOverall(VariableType.Evocation, 2.0 / evoBaseState.castingSpeed)) return false;
            if (this.calculationOptions.getEnableHastedEvocation()) {
                if (evocationAvailable && icyVeinsAvailable && !validateIntegralConsumableOverall(VariableType.EvocationIV, 2.0 / evoBaseState.castingSpeed / 1.2)) return false;
                if (evocationAvailable && heroismAvailable && !validateIntegralConsumableOverall(VariableType.EvocationHero, 2.0 / evoBaseState.castingSpeed / 1.3)) return false;
                if (evocationAvailable && icyVeinsAvailable && heroismAvailable && !validateIntegralConsumableOverall(VariableType.EvocationIVHero, 2.0 / evoBaseState.castingSpeed / 1.2 / 1.3)) return false;
            }
            if (conjureManaGem && !validateIntegralConsumableOverall(VariableType.ConjureManaGem, this.conjureManaGemCycle.castTime)) return false;
        }

        // low resolution
        if (segmentCooldowns && advancedConstraintsLevel >= 1) {
            resolution = lowResolution;
            // berserking
            if (berserkingAvailable && !validateCooldown(StandardEffect.Berserking, 10.0, 180.0, true, 10.0, rowSegmentBerserking, VariableType.None)) return false;
            // make sure all cooldowns are tightly packed and not fragmented
            // mf is trivially satisfied
            // ap
            if (arcanePowerAvailable && !validateCooldown(StandardEffect.ArcanePower, this.arcanePowerDuration, this.arcanePowerCooldown, true, this.arcanePowerDuration, rowSegmentArcanePower, VariableType.None)) return false;
            // iv
            if (icyVeinsAvailable && !validateCooldown(StandardEffect.IcyVeins, 20.0 + (coldsnapAvailable ? 20.0 : 0.0), this.icyVeinsCooldown + (coldsnapAvailable ? 20.0 : 0.0), true, 20.0, rowSegmentIcyVeins, VariableType.None)) return false;
            // pi
            if (powerInfusionAvailable && !validateCooldown(StandardEffect.PowerInfusion, PowerInfusionDuration, PowerInfusionCooldown, true, PowerInfusionDuration, rowSegmentPowerInfusion, VariableType.None)) return false;
            // water elemental
            if (waterElementalAvailable && !this.mageTalents.isGlyphOfEternalWater()) {
                if (!validateIntegralConsumableOverall(VariableType.SummonWaterElemental, this.baseGlobalCooldown)) return false;
                if (!validateCooldown(StandardEffect.WaterElemental, this.waterElementalDuration + (coldsnapAvailable ? this.waterElementalDuration : 0.0), this.waterElementalCooldown + (coldsnapAvailable ? this.waterElementalDuration : 0.0), true, this.waterElementalDuration, rowSegmentWaterElemental, VariableType.None)) return false;
            }
            // mirror image
            if (mirrorImageAvailable && !validateIntegralConsumableOverall(VariableType.SummonMirrorImage, this.baseGlobalCooldown)) return false;
            if (mirrorImageAvailable && !validateCooldown(StandardEffect.MirrorImage, MirrorImageDuration, MirrorImageCooldown, true, MirrorImageDuration, rowSegmentMirrorImage, VariableType.None)) return false;
            // combustion
            if (combustionAvailable && !validateCooldown(StandardEffect.Combustion, 15.0, CombustionCooldown + 15.0)) return false; // the durations are only used to compute segment distances, for 30 sec segments this should work pretty well
            // flamecap
            if (flameCapAvailable && !validateCooldown(StandardEffect.FlameCap, 60.0, 180.0, integralMana, 60.0, rowSegmentFlameCap, VariableType.None)) return false;
            for (int i = 0; i < this.itemBasedEffectCooldownsCount; i++) {
                EffectCooldown cooldown = this.itemBasedEffectCooldowns[i];
                if (!validateCooldown(cooldown.mask, cooldown.duration, cooldown.cooldown, true, cooldown.duration, cooldown.segmentConstraints, VariableType.None)) return false;
            }
            // mana gem effect
            if (manaGemEffectAvailable && !validateCooldown(StandardEffect.ManaGemEffect, this.manaGemEffectDuration, 120f, true, this.manaGemEffectDuration, rowSegmentManaGemEffect, VariableType.None)) return false;
            if (!needsQuadratic) {
                // evocation
                if (evocationAvailable && !validateCooldown(StandardEffect.Evocation, this.evocationDuration, this.evocationCooldown, false, 0.0, rowSegmentEvocation, VariableType.None)) return false;
                if (evocationAvailable && this.calculationOptions.getEnableHastedEvocation()) {
                    if (!validateHastedEvocation()) return false;
                    if (icyVeinsAvailable && !validateActivation(StandardEffect.Evocation, VariableType.EvocationIV, this.evocationDurationIV, this.evocationCooldown, VariableType.EvocationIV, StandardEffect.Evocation | StandardEffect.IcyVeins)) return false;
                }
            }
            // heroism
            if (heroismAvailable && !validateCooldown(StandardEffect.Heroism, 40, -1)) return false;
            if (evocationAvailable && this.calculationOptions.getEnableHastedEvocation()) {
                if (heroismAvailable && !validateActivation(StandardEffect.Evocation, VariableType.EvocationHero, this.evocationDurationHero, this.evocationCooldown, VariableType.EvocationHero, StandardEffect.Evocation | StandardEffect.Heroism)) return false;
                if (icyVeinsAvailable && heroismAvailable && !validateActivation(StandardEffect.Evocation, VariableType.EvocationIVHero, this.evocationDurationIVHero, this.evocationCooldown, VariableType.EvocationIVHero, StandardEffect.Evocation | StandardEffect.IcyVeins | StandardEffect.Heroism)) return false;
            }
            if (effectPotionAvailable && !validateEffectPotion()) return false;
            // potion of wild magic
            if (potionOfWildMagicAvailable && !validateCooldown(StandardEffect.PotionOfWildMagic, 15, -1)) return false;
            // potion of speed
            if (potionOfSpeedAvailable && !validateCooldown(StandardEffect.PotionOfSpeed, 15, -1)) return false;
        }

        // high resolution
        if (segmentCooldowns && advancedConstraintsLevel >= 2) {
            resolution = highResolution;
            if (needsQuadratic) {
                // evocation
                if (evocationAvailable && !validateCooldown(StandardEffect.Evocation, this.evocationDuration, this.evocationCooldown, false, 0.0, rowSegmentEvocation, VariableType.None)) return false;
                if (evocationAvailable && this.calculationOptions.getEnableHastedEvocation()) {
                    if (!validateHastedEvocation()) return false;
                    if (icyVeinsAvailable && !validateActivation(StandardEffect.Evocation, VariableType.EvocationIV, this.evocationDurationIV, this.evocationCooldown, VariableType.EvocationIV, StandardEffect.Evocation | StandardEffect.IcyVeins)) return false;
                }
                // mana
                if (evocationAvailable && !validateEvocation()) return false;
            }
            // drums
            if (berserkingAvailable && !validateCooldown(StandardEffect.Berserking, 10.0, 180.0, true, 10.0, rowSegmentBerserking, VariableType.None)) return false;
            // ap
            if (arcanePowerAvailable && !validateCooldown(StandardEffect.ArcanePower, this.arcanePowerDuration, this.arcanePowerCooldown, true, this.arcanePowerDuration, rowSegmentArcanePower, VariableType.None)) return false;
            // iv
            if (icyVeinsAvailable && !validateCooldown(StandardEffect.IcyVeins, 20.0 + (coldsnapAvailable ? 20.0 : 0.0), this.icyVeinsCooldown + (coldsnapAvailable ? 20.0 : 0.0), true, 20.0, rowSegmentIcyVeins, VariableType.None)) return false;
            // ap
            if (powerInfusionAvailable && !validateCooldown(StandardEffect.PowerInfusion, PowerInfusionDuration, PowerInfusionCooldown, true, PowerInfusionDuration, rowSegmentPowerInfusion, VariableType.None)) return false;
            // water elemental
            if (waterElementalAvailable && !this.mageTalents.isGlyphOfEternalWater() && !validateCooldown(StandardEffect.WaterElemental, this.waterElementalDuration + (coldsnapAvailable ? this.waterElementalDuration : 0.0), this.waterElementalCooldown + (coldsnapAvailable ? this.waterElementalDuration : 0.0), true, this.waterElementalDuration, rowSegmentWaterElemental, VariableType.None)) return false;
            // mirror image
            if (mirrorImageAvailable) {
                if (!validateCooldown(StandardEffect.MirrorImage, MirrorImageDuration, MirrorImageCooldown, true, MirrorImageDuration, rowSegmentMirrorImage, VariableType.None)) return false;
                if (!validateActivation(StandardEffect.MirrorImage, VariableType.None, MirrorImageDuration, MirrorImageCooldown, VariableType.SummonMirrorImage, 0)) return false;
            }

            // coldsnap
            if (icyVeinsAvailable && coldsnapAvailable && !validateColdsnap()) return false;
            // combustion
            if (combustionAvailable && !validateCooldown(StandardEffect.Combustion, 15.0, CombustionCooldown + 15.0)) return false; // the durations are only used to compute segment distances, for 30 sec segments this should work pretty well
            // flamecap
            if (flameCapAvailable && !validateCooldown(StandardEffect.FlameCap, 60.0, 180.0, integralMana, 60.0, rowSegmentFlameCap, VariableType.None)) return false;
            for (int i = 0; i < this.itemBasedEffectCooldownsCount; i++) {
                EffectCooldown cooldown = this.itemBasedEffectCooldowns[i];
                if (!validateCooldown(cooldown.mask, cooldown.duration, cooldown.cooldown, true, cooldown.duration, cooldown.segmentConstraints, VariableType.None)) return false;
            }
            // mana gem effect
            if (manaGemEffectAvailable && !validateCooldown(StandardEffect.ManaGemEffect, this.manaGemEffectDuration, 120f, true, this.manaGemEffectDuration, rowSegmentManaGemEffect, VariableType.None)) return false;
            if (!needsQuadratic) {
                // evocation
                if (evocationAvailable && !validateCooldown(StandardEffect.Evocation, this.evocationDuration, this.evocationCooldown, false, 0.0, rowSegmentEvocation, VariableType.None)) return false;
                if (evocationAvailable && this.calculationOptions.getEnableHastedEvocation()) {
                    if (!validateHastedEvocation()) return false;
                    if (icyVeinsAvailable && !validateActivation(StandardEffect.Evocation, VariableType.EvocationIV, this.evocationDurationIV, this.evocationCooldown, VariableType.EvocationIV, StandardEffect.Evocation | StandardEffect.IcyVeins)) return false;
                }
                // mana
                if (evocationAvailable && !validateEvocation()) return false;
            }
            if (manaGemEffectAvailable && !validateManaGemEffect()) return false;
            // heroism
            if (heroismAvailable && !validateCooldown(StandardEffect.Heroism, 40, -1)) return false;
            if (evocationAvailable && this.calculationOptions.getEnableHastedEvocation()) {
                if (heroismAvailable && !validateActivation(StandardEffect.Evocation, VariableType.EvocationHero, this.evocationDurationHero, this.evocationCooldown, VariableType.EvocationHero, StandardEffect.Evocation | StandardEffect.Heroism)) return false;
                if (icyVeinsAvailable && heroismAvailable && !validateActivation(StandardEffect.Evocation, VariableType.EvocationIVHero, this.evocationDurationIVHero, this.evocationCooldown, VariableType.EvocationIVHero, StandardEffect.Evocation | StandardEffect.IcyVeins | StandardEffect.Heroism)) return false;
            }
            // potion of wild magic
            if (potionOfWildMagicAvailable && !validateCooldown(StandardEffect.PotionOfWildMagic, 15, -1)) return false;
            // potion of speed
            if (potionOfSpeedAvailable && !validateCooldown(StandardEffect.PotionOfSpeed, 15, -1)) return false;
        }

        if (integralMana && advancedConstraintsLevel >= 2) {
            if (manaPotionAvailable && !validateIntegralConsumable(VariableType.ManaPotion)) return false;
            if (this.calculationOptions.getManaGemEnabled() && !validateIntegralConsumable(VariableType.ManaGem)) return false;
        }

        if (segmentCooldowns && advancedConstraintsLevel >= 3) {
            if (!validateCycling()) return false;
            if (!validateSupergroupCycles()) return false;
            if (!validateSupergroupFragmentation()) return false;

            if (evocationAvailable && this.calculationOptions.getEnableHastedEvocation()) {
                if (icyVeinsAvailable && !validateActivationAdvanced(StandardEffect.Evocation, VariableType.EvocationIV, this.evocationDurationIV, this.evocationCooldown, VariableType.EvocationIV, StandardEffect.Evocation | StandardEffect.IcyVeins)) return false;
                if (heroismAvailable && !validateActivationAdvanced(StandardEffect.Evocation, VariableType.EvocationHero, this.evocationDurationHero, this.evocationCooldown, VariableType.EvocationHero, StandardEffect.Evocation | StandardEffect.Heroism)) return false;
                if (icyVeinsAvailable && heroismAvailable && !validateActivationAdvanced(StandardEffect.Evocation, VariableType.EvocationIVHero, this.evocationDurationIVHero, this.evocationCooldown, VariableType.EvocationIVHero, StandardEffect.Evocation | StandardEffect.IcyVeins | StandardEffect.Heroism)) return false;
            }
            if (mirrorImageAvailable) {
                if (!validateActivationAdvanced(StandardEffect.MirrorImage, VariableType.None, MirrorImageDuration, MirrorImageCooldown, VariableType.SummonMirrorImage, 0)) return false;
            }
        }

        if (segmentCooldowns && advancedConstraintsLevel >= 4) {
            // advanced cooldown validation
            if (arcanePowerAvailable && !validateCooldownAdvanced(StandardEffect.ArcanePower, this.arcanePowerDuration, this.arcanePowerCooldown, VariableType.None)) return false;
            if (icyVeinsAvailable && !coldsnapAvailable && !validateCooldownAdvanced(StandardEffect.IcyVeins, 20.0, this.icyVeinsCooldown, VariableType.None)) return false;
            if (powerInfusionAvailable && !validateCooldownAdvanced(StandardEffect.PowerInfusion, PowerInfusionDuration, PowerInfusionCooldown, VariableType.None)) return false;
            for (int i = 0; i < this.itemBasedEffectCooldownsCount; i++) {
                EffectCooldown cooldown = this.itemBasedEffectCooldowns[i];
                if (!validateCooldownAdvanced(cooldown.mask, cooldown.duration, cooldown.cooldown, VariableType.None)) return false;
            }
            if (manaGemEffectAvailable && !validateCooldownAdvanced(StandardEffect.ManaGemEffect, this.manaGemEffectDuration, 120.0, VariableType.None)) return false;
            if (berserkingAvailable && !validateCooldownAdvanced(StandardEffect.Berserking, 10.0, 180.0, VariableType.None)) return false;
            if (mirrorImageAvailable && !validateCooldownAdvanced(StandardEffect.MirrorImage, MirrorImageDuration, MirrorImageCooldown, VariableType.None)) return false;
        }

        if (segmentCooldowns && advancedConstraintsLevel >= 5) {
            if (waterElementalAvailable && !this.mageTalents.isGlyphOfEternalWater()) {
                if (coldsnapAvailable) {
                    if (!validateWaterElementalSummon()) return false;
                } else {
                    if (!validateActivation(StandardEffect.WaterElemental, VariableType.None, this.waterElementalDuration, this.waterElementalCooldown, VariableType.SummonWaterElemental, 0)) return false;
                }
            }

            // advanced cooldown validation 2
            if (arcanePowerAvailable && !validateCooldownAdvanced2(StandardEffect.ArcanePower, this.arcanePowerDuration, this.arcanePowerCooldown, VariableType.None)) return false;
            if (icyVeinsAvailable && !coldsnapAvailable && !validateCooldownAdvanced2(StandardEffect.IcyVeins, 20.0, this.icyVeinsCooldown, VariableType.None)) return false;
            if (powerInfusionAvailable && !validateCooldownAdvanced2(StandardEffect.PowerInfusion, PowerInfusionDuration, PowerInfusionCooldown, VariableType.None)) return false;
            for (int i = 0; i < this.itemBasedEffectCooldownsCount; i++) {
                EffectCooldown cooldown = this.itemBasedEffectCooldowns[i];
                if (!validateCooldownAdvanced2(cooldown.mask, cooldown.duration, cooldown.cooldown, VariableType.None)) return false;
            }
            if (manaGemEffectAvailable && !validateCooldownAdvanced2(StandardEffect.ManaGemEffect, this.manaGemEffectDuration, 120.0, VariableType.None)) return false;
            if (berserkingAvailable && !validateCooldownAdvanced2(StandardEffect.Berserking, 10.0, 180.0, VariableType.None)) return false;
            if (mirrorImageAvailable && !validateCooldownAdvanced2(StandardEffect.MirrorImage, MirrorImageDuration, MirrorImageCooldown, VariableType.None)) return false;
        }

        return true;
    }

    private void heapPush(SolverLP childLP) {
        switch (this.calculationOptions.getMIPMethod()) {
            case BestBound:
                if (childLP.getValue() > 0) heap.push(childLP);
                break;
            case DepthFirst:
            case HybridUnguided:
            case HybridGuided:
                BranchNode child = new BranchNode();
                child.lp = childLP;
                child.parent = currentNode;
                child.depth = currentNode.depth + 1;
                currentNode.children.add(child);
                break;
        }
    }

    private double[] getSegmentCooldownCount(int effectsMask, VariableType cooldownType) {
        int ind = -1;
        switch (effectsMask) {
            case StandardEffect.ArcanePower:
                ind = 0;
                break;
            case StandardEffect.Combustion:
                ind = 1;
                break;
            case StandardEffect.Berserking:
                ind = 2;
                break;
            case StandardEffect.FlameCap:
                ind = 3;
                break;
            case StandardEffect.Heroism:
                ind = 4;
                break;
            case StandardEffect.IcyVeins:
                ind = 5;
                break;
            case StandardEffect.ManaGemEffect:
                ind = 6;
                break;
            case StandardEffect.PotionOfSpeed:
                ind = 7;
                break;
            case StandardEffect.PotionOfWildMagic:
                ind = 8;
                break;
            case StandardEffect.MirrorImage:
                ind = 9;
                break;
            case StandardEffect.WaterElemental:
                ind = 11;
                break;
            case StandardEffect.Evocation:
                switch (cooldownType) {
                    case None:
                        ind = 12;
                        break;
                    case Evocation:
                        ind = 21;
                        break;
                    case EvocationIV:
                        ind = 18;
                        break;
                    case EvocationHero:
                        ind = 19;
                        break;
                    case EvocationIVHero:
                        ind = 20;
                        break;
                    default:
                        break;
                }
                break;
            case StandardEffect.Evocation | StandardEffect.IcyVeins:
                ind = 15;
                break;
            case StandardEffect.Evocation | StandardEffect.Heroism:
                ind = 16;
                break;
            case StandardEffect.Evocation | StandardEffect.IcyVeins | StandardEffect.Heroism:
                ind = 17;
                break;
            case StandardEffect.PowerInfusion:
                ind = 22;
                break;
            default:
                for (int i = 0; i < this.itemBasedEffectCooldownsCount; i++) {
                    EffectCooldown cooldown = this.itemBasedEffectCooldowns[i];
                    if (effectsMask == cooldown.mask) {
                        ind = 23 + i;
                        break;
                    }
                }
                if (ind != -1) {
                    break;
                }
                switch (cooldownType) {
                    case ManaGem:
                        ind = 13;
                        break;
                    case SummonWaterElemental:
                        ind = 14;
                        break;
                    case SummonMirrorImage:
                        ind = 10;
                        break;
                    default:
                        break;
                }
                break;
        }
        double[] segCount = segmentCooldownCount[ind];
        if (segCount == null) {
            segCount = new double[this.segmentList.size()];
            segmentCooldownCount[ind] = segCount;
            if (effectsMask != 0) {
                for (int outseg = 0; outseg < this.segmentList.size(); outseg++) {
                    double s = 0.0;
                    for (int index = segmentColumn[outseg]; index < segmentColumn[outseg + 1]; index++) {
                        if (this.solutionVariable.get(index).isMatch(effectsMask, cooldownType)) {
                            s += solution[index];
                        }
                    }
                    segCount[outseg] = s;
                }
            }
            for (int index = 0; index < segmentColumn[0]; index++) { // fix if variable ordering changes
                if (this.solutionVariable.get(index).isMatch(effectsMask, cooldownType)) segCount[this.solutionVariable.get(index).segment] += solution[index];
            }
        }
        return segCount;
    }

    private void analyzeSolution() {
        manaList = new double[this.segmentList.size()];
        segmentCooldownCount = newDoubleArrayArray(23 + this.itemBasedEffectCooldownsCount);
        hexList = newIntListArray(this.segmentList.size());
        segmentFilled = new double[this.segmentList.size()];
        hexMask = new int[this.segmentList.size()];
        for (int seg = 0; seg < this.segmentList.size(); seg++) {
            manaList[seg] = this.startingMana;
            hexList[seg] = new ArrayList<Integer>();
        }
        for (int index = 0; index < this.solutionVariable.size(); index++) {
            CastingState state = this.solutionVariable.get(index).state;
            int iseg = this.solutionVariable.get(index).segment;
            if (!this.calculationOptions.getUnlimitedMana()) {
                for (int seg = iseg + 1; seg < this.segmentList.size(); seg++) {
                    manaList[seg] -= solution[index] * lp.get(rowManaRegen, index);
                }
            }
            if (solution[index] > 0.00001 && state != null) {
                int h = state.getEffects();
                if (h != 0) {
                    hexMask[iseg] |= h;
                    if (!hexList[iseg].contains(h)) hexList[iseg].add(h);
                    segmentFilled[iseg] += solution[index];
                }
            }
        }
    }

    private int addConstraint(SolverLP branchlp, int effects, int segment) {
        return addConstraint(branchlp, effects, VariableType.None, segment, segment);
    }

    private int addConstraint(SolverLP branchlp, int effects, int minSegment, int maxSegment) {
        return addConstraint(branchlp, effects, VariableType.None, minSegment, maxSegment);
    }

    private int addConstraint(SolverLP branchlp, int effects, VariableType cooldownType, int minSegment, int maxSegment) {
        boolean[] newConstraint = new boolean[1];
        int index = branchlp.addConstraint(String.format(Locale.ROOT, "%d-%s-%d-%d", effects, cooldownType, minSegment, maxSegment), newConstraint);
        if (newConstraint[0]) {
            setCooldownElements(branchlp, index, effects, cooldownType, minSegment, maxSegment, 1.0);
        }
        return index;
    }

    private boolean validateSupergroupCycles() {
        for (int seg = 1; seg < this.segmentList.size() - 1; seg++) {
            {
                int N = hexList[seg].size();
                if (N > 0) {
                    for (int i = 0; i < N; i++) {
                        int current = hexList[seg].get(i);
                        int leftmask = 0;
                        for (int j = 0; j < hexList[seg - 1].size(); j++) {
                            leftmask |= (current & hexList[seg - 1].get(j));
                        }
                        int rightmask = 0;
                        for (int j = 0; j < hexList[seg + 1].size(); j++) {
                            rightmask |= (current & hexList[seg + 1].get(j));
                        }
                        if (leftmask != 0 && rightmask != 0) {
                            // single node connects to both left and right
                            // this means that every element in this segment has to contain either full left mask
                            // or full right mask
                            // first detect if we have a problem
                            for (int j = 0; j < N; j++) {
                                int h = hexList[seg].get(j);
                                if ((h & leftmask) != leftmask && (h & rightmask) != rightmask) {
                                    // this indicates a problem
                                    // basically find an entry from leftmask that is not in this one and an entry
                                    // from right mask that is not in this one
                                    int ind1 = 1;
                                    while ((ind1 & leftmask & ~h) == 0) ind1 <<= 1;
                                    int ind2 = 1;
                                    while ((ind2 & rightmask & ~h) == 0) ind2 <<= 1;

                                    // do elimination
                                    // eliminate left
                                    SolverLP hexRemovedLP = lp.clone();
                                    if (hexRemovedLP.log != null) hexRemovedLP.log.append("Breaking supergroup cycle " + ind1 + "-" + ind2 + " at " + seg + ", removing left " + ind1).append('\n');
                                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                                        CastingState state = this.solutionVariable.get(index).state;
                                        int iseg = this.solutionVariable.get(index).segment;
                                        if (state != null && iseg == seg - 1 && (state.getEffects() & ind1) != 0) hexRemovedLP.eraseColumn(index);
                                    }
                                    heapPush(hexRemovedLP);
                                    // eliminate right
                                    hexRemovedLP = lp.clone();
                                    if (hexRemovedLP.log != null) hexRemovedLP.log.append("Breaking supergroup cycle " + ind1 + "-" + ind2 + " at " + seg + ", removing right " + ind2).append('\n');
                                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                                        CastingState state = this.solutionVariable.get(index).state;
                                        int iseg = this.solutionVariable.get(index).segment;
                                        if (state != null && iseg == seg + 1 && (state.getEffects() & ind2) != 0) hexRemovedLP.eraseColumn(index);
                                    }
                                    heapPush(hexRemovedLP);
                                    // eliminate middle
                                    hexRemovedLP = lp.clone();
                                    int ind = ind1 | ind2;
                                    if (hexRemovedLP.log != null) hexRemovedLP.log.append("Breaking supergroup cycle " + ind1 + "-" + ind2 + " at " + seg + ", removing center " + ind).append('\n');
                                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                                        CastingState state = this.solutionVariable.get(index).state;
                                        int iseg = this.solutionVariable.get(index).segment;
                                        if (state != null && iseg == seg && (state.getEffects() & ind) == ind) hexRemovedLP.eraseColumn(index);
                                    }
                                    heapPush(hexRemovedLP);
                                    // force to full
                                    // this is equivalent to disabling all that are not good (at least I think it is)
                                    if (lp.log != null) lp.log.append("Breaking supergroup cycle " + ind1 + "-" + ind2 + " at " + seg + ", force to max").append('\n');
                                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                                        CastingState state = this.solutionVariable.get(index).state;
                                        int iseg = this.solutionVariable.get(index).segment;
                                        if (state != null && iseg == seg && !this.solutionVariable.get(index).isZeroTime()) {
                                            int hex = state.getEffects();
                                            if ((state.getEffects() & (ind1 | ind2)) == 0) lp.eraseColumn(index);
                                        }
                                    }
                                    lp.setLHS(rowSegment + seg, this.segmentList.get(seg).duration);
                                    heapPush(lp);
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
            for (int seg2 = seg - 1; seg2 <= seg + 1; seg2 += 2) {
                if (seg2 >= 0 && seg2 < this.segmentList.size()) {
                    int mask = hexMask[seg2];
                    for (int i = 0; i < hexList[seg].size(); i++) {
                        int core = hexList[seg].get(i);

                        int coremask = core & mask;
                        for (int j = 0; j < hexList[seg].size(); j++) {
                            int h = hexList[seg].get(j);
                            if ((h & core) != 0 && (~h & coremask) != 0) {
                                for (int k = j + 1; k < hexList[seg].size(); k++) {
                                    int g = hexList[seg].get(k);
                                    if ((g & core) != 0 && (~g & coremask) != 0) {
                                        int hcore = core & h & ~g;
                                        int gcore = core & g & ~h;
                                        if (hcore != 0 && gcore != 0) {
                                            // key elements are:
                                            // - element from coremask that is missing in h
                                            // - element from coremask that is missing in g
                                            // - connecting element between core and h not in g
                                            // - connecting element between core and g not in h
                                            int c = 1;
                                            while ((c & ~h & coremask) == 0) c <<= 1;
                                            int d = 1;
                                            while ((d & ~g & coremask) == 0) d <<= 1;
                                            int v = 1;
                                            while ((v & hcore) == 0) v <<= 1;
                                            int z = 1;
                                            while ((z & gcore) == 0) z <<= 1;

                                            // eliminate c from mask
                                            SolverLP hexRemovedLP = lp.clone();
                                            if (hexRemovedLP.log != null) hexRemovedLP.log.append("Breaking advanced supergroup cycle at " + seg + ", removing from neighbor mask " + c).append('\n');
                                            for (int index = 0; index < this.solutionVariable.size(); index++) {
                                                CastingState state = this.solutionVariable.get(index).state;
                                                int iseg = this.solutionVariable.get(index).segment;
                                                if (state != null && iseg == seg2 && (state.getEffects() & c) != 0) hexRemovedLP.eraseColumn(index);
                                            }
                                            heapPush(hexRemovedLP);

                                            // eliminate d from mask
                                            if (d != c) {
                                                hexRemovedLP = lp.clone();
                                                if (hexRemovedLP.log != null) hexRemovedLP.log.append("Breaking advanced supergroup cycle at " + seg + ", removing from neighbor mask " + d).append('\n');
                                                for (int index = 0; index < this.solutionVariable.size(); index++) {
                                                    CastingState state = this.solutionVariable.get(index).state;
                                                    int iseg = this.solutionVariable.get(index).segment;
                                                    if (state != null && iseg == seg2 && (state.getEffects() & d) != 0) hexRemovedLP.eraseColumn(index);
                                                }
                                                heapPush(hexRemovedLP);
                                            }

                                            // eliminate bridge
                                            int bridge = c | d | v | z;
                                            hexRemovedLP = lp.clone();
                                            if (hexRemovedLP.log != null) hexRemovedLP.log.append("Breaking advanced supergroup cycle at " + seg + ", removing bridge " + bridge).append('\n');
                                            for (int index = 0; index < this.solutionVariable.size(); index++) {
                                                CastingState state = this.solutionVariable.get(index).state;
                                                int iseg = this.solutionVariable.get(index).segment;
                                                if (state != null && iseg == seg && (state.getEffects() & bridge) == bridge) hexRemovedLP.eraseColumn(index);
                                            }
                                            heapPush(hexRemovedLP);

                                            // eliminate hcore
                                            hexRemovedLP = lp.clone();
                                            if (hexRemovedLP.log != null) hexRemovedLP.log.append("Breaking advanced supergroup cycle at " + seg + ", removing doublecross " + v + "-" + z).append('\n');
                                            for (int index = 0; index < this.solutionVariable.size(); index++) {
                                                CastingState state = this.solutionVariable.get(index).state;
                                                int iseg = this.solutionVariable.get(index).segment;
                                                if (state != null && iseg == seg && (state.getEffects() & (v | z)) == v) hexRemovedLP.eraseColumn(index);
                                            }
                                            heapPush(hexRemovedLP);

                                            // eliminate gcore
                                            hexRemovedLP = lp.clone();
                                            if (hexRemovedLP.log != null) hexRemovedLP.log.append("Breaking advanced supergroup cycle at " + seg + ", removing doublecross " + z + "-" + v).append('\n');
                                            for (int index = 0; index < this.solutionVariable.size(); index++) {
                                                CastingState state = this.solutionVariable.get(index).state;
                                                int iseg = this.solutionVariable.get(index).segment;
                                                if (state != null && iseg == seg && (state.getEffects() & (v | z)) == z) hexRemovedLP.eraseColumn(index);
                                            }
                                            heapPush(hexRemovedLP);

                                            // lp unused
                                            lp.releaseConstraints();
                                            return false;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean validateSupergroupFragmentation() {
        for (int seg = 1; seg < this.segmentList.size() - 1; seg++) {
            int N = hexList[seg].size();
            if (N > 0 && segmentFilled[seg] < this.segmentList.get(seg).duration - 0.00001) {
                // if any hex links to left and right segment we have a problem
                List<Integer> minHexChain = null;
                for (int i = 0; i < N; i++) {
                    // compute distance
                    int[] hexDistance = new int[N];
                    for (int j = 0; j < N; j++) {
                        hexDistance[j] = -1;
                    }
                    hexDistance[i] = 0;
                    int current = hexList[seg].get(i);
                    int distance = 0;
                    int next;
                    boolean unlinked = true;
                    while (unlinked) {
                        unlinked = false;
                        distance++;
                        next = current;
                        for (int j = 0; j < N; j++) {
                            if (hexDistance[j] == -1 && (hexList[seg].get(j) & current) != 0) {
                                hexDistance[j] = distance;
                                next |= hexList[seg].get(j);
                                unlinked = true;
                            }
                        }
                        current = next;
                    }
                    // find closest link on left and right
                    int leftMin = Integer.MAX_VALUE;
                    int leftMinHex = 0;
                    for (int j = 0; j < hexList[seg - 1].size(); j++) {
                        int h = hexList[seg - 1].get(j);
                        if ((h & current) != 0) {
                            for (int k = 0; k < N; k++) {
                                if ((h & hexList[seg].get(k)) != 0) {
                                    if (hexDistance[k] != -1 && hexDistance[k] < leftMin) {
                                        leftMin = hexDistance[k];
                                        leftMinHex = h;
                                    }
                                }
                            }
                        }
                    }
                    int rightMin = Integer.MAX_VALUE;
                    int rightMinHex = 0;
                    for (int j = 0; j < hexList[seg + 1].size(); j++) {
                        int h = hexList[seg + 1].get(j);
                        if ((h & current) != 0) {
                            for (int k = 0; k < N; k++) {
                                if ((h & hexList[seg].get(k)) != 0) {
                                    if (hexDistance[k] != -1 && hexDistance[k] < rightMin) {
                                        rightMin = hexDistance[k];
                                        rightMinHex = h;
                                    }
                                }
                            }
                        }
                    }
                    if (leftMinHex != 0 && rightMinHex != 0) {
                        // we found an offensive hex chain
                        if (minHexChain == null || (leftMin + rightMin + 2) < minHexChain.size()) {
                            // reconstruct chain
                            minHexChain = new ArrayList<Integer>();
                            int currentHex = leftMinHex;
                            int currentDist = leftMin;
                            if (!minHexChain.contains(currentHex)) minHexChain.add(currentHex);
                            while (currentDist > 0) {
                                for (int j = 0; j < N; j++) {
                                    if (hexDistance[j] == currentDist && (hexList[seg].get(j) & currentHex) != 0) {
                                        currentHex = hexList[seg].get(j);
                                        currentDist--;
                                        if (!minHexChain.contains(currentHex)) minHexChain.add(currentHex);
                                        break;
                                    }
                                }
                            }
                            currentHex = rightMinHex;
                            currentDist = rightMin;
                            if (!minHexChain.contains(currentHex)) minHexChain.add(currentHex);
                            while (currentDist > 0) {
                                for (int j = 0; j < N; j++) {
                                    if (hexDistance[j] == currentDist && (hexList[seg].get(j) & currentHex) != 0) {
                                        currentHex = hexList[seg].get(j);
                                        currentDist--;
                                        if (!minHexChain.contains(currentHex)) minHexChain.add(currentHex);
                                        break;
                                    }
                                }
                            }
                            currentHex = hexList[seg].get(i);
                            if (!minHexChain.contains(currentHex)) minHexChain.add(currentHex);
                        }
                    }
                }
                if (minHexChain != null) {
                    // we have a problem and the shortest hex chain that identifies it
                    // solve by branching on eliminating one of the elements in the chain
                    // or forcing the segment to max
                    for (int i = 0; i < minHexChain.size(); i++) {
                        SolverLP hexRemovedLP = lp.clone();
                        if (hexRemovedLP.log != null) hexRemovedLP.log.append("Breaking supergroup fragmentation at " + seg + ", removing " + minHexChain.get(i)).append('\n');
                        for (int index = 0; index < this.solutionVariable.size(); index++) {
                            CastingState state = this.solutionVariable.get(index).state;
                            int iseg = this.solutionVariable.get(index).segment;
                            if (state != null && iseg >= seg - 1 && iseg <= seg + 1 && state.getEffects() == minHexChain.get(i)) hexRemovedLP.eraseColumn(index);
                        }
                        heapPush(hexRemovedLP);
                    }
                    if (lp.log != null) lp.log.append("Breaking supergroup fragmentation at " + seg + ", force to max").append('\n');
                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                        CastingState state = this.solutionVariable.get(index).state;
                        int iseg = this.solutionVariable.get(index).segment;
                        if (state != null && iseg == seg && state.getEffects() == 0 && !this.solutionVariable.get(index).isZeroTime()) lp.eraseColumn(index);
                    }
                    lp.setLHS(rowSegment + seg, this.segmentList.get(seg).duration);
                    lp.forceRecalculation(true);
                    heapPush(lp);
                    return false;
                }
            }
        }
        return true;
    }

    private List<Integer> findShortestTailPath(int core, int t, int node, int[] used, int N, List<Integer> segmentHexList, int distinctFrom) {
        int[] dist = new int[N]; // distance from CTn indicator
        int[] parent = new int[N];
        for (int k = 0; k < N; k++) {
            if (used[k] == t + 1) {
                if ((segmentHexList.get(k) & core & ~distinctFrom) != 0) {
                    dist[k] = 1;
                    parent[k] = -1;
                }
            }
        }
        boolean updated;
        do {
            updated = false;
            for (int k = 0; k < N; k++) {
                if (used[k] == t + 1) {
                    for (int l = 0; l < N; l++) {
                        if (used[l] == t + 1 && dist[l] > 0) {
                            if ((segmentHexList.get(k) & segmentHexList.get(l) & ~core & ~distinctFrom) != 0 && (dist[l] + 1 < dist[k] || dist[k] == 0)) {
                                dist[k] = dist[l] + 1;
                                parent[k] = l;
                                updated = true;
                            }
                        }
                    }
                }
            }
        } while (updated);
        int min = Integer.MAX_VALUE;
        int closest = -1;
        for (int k = 0; k < N; k++) {
            if (used[k] == t + 1) {
                if (dist[k] > 0 && (segmentHexList.get(k) & node & ~core) != 0 && dist[k] + 1 < min) {
                    min = dist[k] + 1;
                    closest = k;
                }
            }
        }
        List<Integer> ret = new ArrayList<Integer>();
        while (closest != -1) {
            ret.add(segmentHexList.get(closest));
            closest = parent[closest];
        }
        return ret;
    }

    private boolean validateCycling() {
        // eliminate packing cycles
        boolean valid = true;
        for (int seg = 0; seg < this.segmentList.size() - 1; seg++) {
            List<Integer> hexList1 = hexList[seg];
            List<Integer> hexList2 = hexList[seg + 1];
            List<Integer> hexList12 = new ArrayList<Integer>();
            hexList12.addAll(hexList1);
            hexList12.addAll(hexList2);

            int N = hexList12.size();
            List<Integer> shortestCycle = null;
            List<Integer> shortestBridge = null;
            List<Integer> threeTail = null;
            List<Integer> connectingTails = new ArrayList<Integer>();
            breakCycle:
            for (int i = 0; i < N; i++) {
                int core = hexList12.get(i);
                int[] tail = new int[3];
                int[] used = new int[N]; // index+1 of the tail to which it is attached, -1 for core
                int[] firsttail = new int[3];
                used[i] = -1;
                boolean cont = true;
                while (cont) {
                    cont = false;
                    for (int j = 0; j < N; j++) {
                        if (used[j] == 0) {
                            int node = hexList12.get(j);
                            int t = 0;
                            boolean alldifferent = true;
                            if (t == 0 && tail[t] == 0) {
                                alldifferent = (node & ~core) != 0 && (core & ~node) != 0 && (node & core) != 0;
                            }
                            connectingTails.clear();
                            for (; t < 3 && tail[t] != 0; t++) {
                                int cTN = tail[t] & node & ~core;
                                int CtN = ~tail[t] & node & core;
                                int CTn = tail[t] & ~node & core;
                                boolean connects = cTN != 0;
                                boolean different = CtN != 0 && CTn != 0;
                                if (connects && different) {
                                    // this is a cycle
                                    // to break the cycle we have to find the connecting link in tail
                                    List<Integer> path = findShortestTailPath(core, t, node, used, N, hexList12, node);
                                    if (shortestCycle == null || path.size() + 2 < shortestCycle.size()) {
                                        shortestCycle = new ArrayList<Integer>();
                                        shortestCycle.add(node);
                                        shortestCycle.addAll(path);
                                        shortestCycle.add(core);
                                        if (shortestCycle.size() == 3) break breakCycle;
                                    }
                                    cont = true;
                                    used[j] = -1;
                                    break;
                                } else if (connects && !different) {
                                    cont = true;
                                    connectingTails.add(t);
                                } else if (!different) {
                                    alldifferent = false;
                                }
                            }
                            if (used[j] != -1) {
                                if (connectingTails.size() == 0 && t < 3 && tail[t] == 0) {
                                    if (alldifferent) {
                                        // it doesn't connect to any existing tails and we can prove it's not in any of them
                                        // this means it has to be in a different tail
                                        cont = true;
                                        tail[t] = hexList12.get(j);
                                        firsttail[t] = hexList12.get(j);
                                        used[j] = t + 1;
                                        if (t == 2) {
                                            // we have a three-tail
                                            threeTail = new ArrayList<Integer>();
                                            threeTail.add(core);
                                            for (t = 0; t < 3; t++) {
                                                threeTail.add(firsttail[t]);
                                            }
                                        }
                                    }
                                } else if (connectingTails.size() >= 2) {
                                    // node connected to one of the tails
                                    // in this case we also have a cycle, find the shortest path through
                                    // each tail and loop it into cycle
                                    int t0 = connectingTails.get(0);
                                    int t1 = connectingTails.get(1);
                                    used[j] = -1;
                                    List<Integer> path1 = findShortestTailPath(core, t0, node, used, N, hexList12, firsttail[t1]);
                                    List<Integer> path2 = findShortestTailPath(core, t1, node, used, N, hexList12, firsttail[t0]);
                                    if (shortestBridge == null || path1.size() + path2.size() + 2 < shortestBridge.size()) {
                                        shortestBridge = new ArrayList<Integer>();
                                        shortestBridge.add(node);
                                        shortestBridge.addAll(path1);
                                        shortestBridge.add(core);
                                        Collections.reverse(path2);
                                        shortestBridge.addAll(path2);
                                    }
                                } else if (connectingTails.size() == 1) {
                                    used[j] = connectingTails.get(0) + 1;
                                    tail[connectingTails.get(0)] |= hexList12.get(j);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (shortestCycle != null && (threeTail == null || shortestCycle.size() < threeTail.size()) && (shortestBridge == null || shortestCycle.size() < shortestBridge.size())) {
                // break the cycle
                // first compute indicators and for each indicator select a single representative
                int M = shortestCycle.size();
                List<Integer> indicator = new ArrayList<Integer>();
                for (int i = 0; i < M; i++) {
                    int j = (i + 1) % M;
                    int ind = shortestCycle.get(i) & shortestCycle.get(j);
                    for (int k = 0; k < M; k++) {
                        if (k != i && k != j) ind &= ~shortestCycle.get(k);
                    }
                    int s = 1;
                    while ((s & ind) == 0) s <<= 1;
                    indicator.add(s);
                }
                int cycle = 0;
                for (int i = 0; i < M; i++) {
                    cycle |= indicator.get(i);
                }
                // break cycle by eliminating one pair of indicators
                for (int i = 0; i < M; i++) {
                    int j = (i + 1) % M;
                    SolverLP hexRemovedLP = lp.clone();
                    if (hexRemovedLP.log != null) hexRemovedLP.log.append("Breaking cycle " + cycle + " at boundary " + seg + ", removing " + indicator.get(i) + "+" + indicator.get(j)).append('\n');
                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                        CastingState state = this.solutionVariable.get(index).state;
                        int iseg = this.solutionVariable.get(index).segment;
                        if (state != null && (iseg == seg || iseg == seg + 1)) {
                            int h = state.getEffects();
                            boolean isindicated = (h & indicator.get(i)) != 0 && (h & indicator.get(j)) != 0;
                            if (isindicated) {
                                for (int k = 0; k < M; k++) {
                                    if (k != i && k != j && (h & indicator.get(k)) != 0) {
                                        isindicated = false;
                                        break;
                                    }
                                }
                            }
                            if (isindicated) hexRemovedLP.eraseColumn(index);
                        }
                    }
                    heapPush(hexRemovedLP);
                }
                // lp unused
                lp.releaseConstraints();
                return false;
            } else if (shortestBridge != null && (threeTail == null || shortestBridge.size() < threeTail.size())) {
                // break the cycle
                // first compute indicators and for each indicator select a single representative
                int M = shortestBridge.size();
                List<Integer> indicator = new ArrayList<Integer>();
                for (int i = 0; i < M; i++) {
                    int j = (i + 1) % M;
                    int ind = shortestBridge.get(i) & shortestBridge.get(j);
                    for (int k = 0; k < M; k++) {
                        if (i == 0 || i == M - 1) {
                            // bridge connection
                            if (k != i && k != j) ind &= ~shortestBridge.get(k);
                        } else {
                            // path
                            if (k != i && k != j && k != 0) ind &= ~shortestBridge.get(k);
                        }
                    }
                    int s = 1;
                    while ((s & ind) == 0) s <<= 1;
                    indicator.add(s);
                }
                // break cycle by eliminating one pair of indicators
                for (int i = 0; i < M; i++) {
                    int j = (i + 1) % M;
                    SolverLP hexRemovedLP = lp.clone();
                    if (hexRemovedLP.log != null) hexRemovedLP.log.append("Breaking bridge at boundary " + seg + ", removing " + indicator.get(i) + "+" + indicator.get(j)).append('\n');
                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                        CastingState state = this.solutionVariable.get(index).state;
                        int iseg = this.solutionVariable.get(index).segment;
                        if (state != null && (iseg == seg || iseg == seg + 1)) {
                            int h = state.getEffects();
                            boolean isindicated = (h & indicator.get(i)) != 0 && (h & indicator.get(j)) != 0;
                            if (isindicated && i < M - 1) // for bridge it is indicated as long as it contains bridge connections
                            {
                                for (int k = 0; k < M; k++) {
                                    if (k != i && k != j && (h & indicator.get(k)) != 0) {
                                        isindicated = false;
                                        break;
                                    }
                                }
                            }
                            if (isindicated) hexRemovedLP.eraseColumn(index);
                        }
                    }
                    heapPush(hexRemovedLP);
                }
                // lp unused
                lp.releaseConstraints();
                return false;
            } else if (threeTail != null) {
                // find the indicators
                List<Integer> indicator = new ArrayList<Integer>();
                int core = threeTail.get(0);
                int mask = (threeTail.get(1) | threeTail.get(2) | threeTail.get(3)) & ~(threeTail.get(1) & threeTail.get(2) & threeTail.get(3));
                SolverLP hexRemovedLP = lp.clone();
                if (hexRemovedLP.log != null) hexRemovedLP.log.append("Breaking threetail at boundary " + seg + ", removing core").append('\n');
                for (int index = 0; index < this.solutionVariable.size(); index++) {
                    CastingState state = this.solutionVariable.get(index).state;
                    int iseg = this.solutionVariable.get(index).segment;
                    if (state != null && (iseg == seg || iseg == seg + 1)) {
                        int h = state.getEffects();
                        boolean isindicated = (h & mask) == (core & mask);
                        if (isindicated) hexRemovedLP.eraseColumn(index);
                    }
                }
                heapPush(hexRemovedLP);
                for (int i = 0; i < 3; i++) {
                    hexRemovedLP = lp.clone();
                    if (hexRemovedLP.log != null) hexRemovedLP.log.append("Breaking threetail at boundary " + seg + ", removing " + (threeTail.get(i + 1) & mask)).append('\n');
                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                        CastingState state = this.solutionVariable.get(index).state;
                        int iseg = this.solutionVariable.get(index).segment;
                        if (state != null && (iseg == seg || iseg == seg + 1)) {
                            int h = state.getEffects();
                            boolean isindicated = (h & mask) == (threeTail.get(i + 1) & mask);
                            if (isindicated) hexRemovedLP.eraseColumn(index);
                        }
                    }
                    heapPush(hexRemovedLP);
                }
                // lp unused
                lp.releaseConstraints();
                return false;
            }

            int cool1 = 0, cool2 = 0, seg1 = 0, seg2 = 0;
            eliminateCrossing:
            {
                // detect and eliminate double crossings
                int hex1 = 0;
                for (int i = 0; i < hexList1.size(); i++) {
                    hex1 |= hexList1.get(i);
                }
                int hex2 = 0;
                for (int i = 0; i < hexList2.size(); i++) {
                    hex2 |= hexList2.get(i);
                }
                int hex = hex1 & hex2; // crossings
                for (int i = 0; i < hexList1.size(); i++) {
                    for (int j = i + 1; j < hexList1.size(); j++) {
                        int placed = hex & hexList1.get(i);
                        int h = hex & hexList1.get(j);
                        int hp = h & placed;
                        if (placed != hp && h != hp) {
                            // XXX XX   placed
                            //  XXXX    h
                            //  .. .    hp
                            // #  - #   placed - hp
                            // -  # -   h - hp
                            valid = false;
                            for (cool1 = 0; cool1 < cooldownCount; cool1++) {
                                if (((1 << cool1) & placed & ~hp) != 0) break;
                            }
                            for (cool2 = 0; cool2 < cooldownCount; cool2++) {
                                if (((1 << cool2) & h & ~hp) != 0) break;
                            }
                            seg1 = seg;
                            seg2 = seg + 1;
                            break eliminateCrossing;
                        }
                    }
                }
                for (int i = 0; i < hexList2.size(); i++) {
                    for (int j = i + 1; j < hexList2.size(); j++) {
                        int placed = hex & hexList2.get(i);
                        int h = hex & hexList2.get(j);
                        int hp = h & placed;
                        if (placed != hp && h != hp) {
                            valid = false;
                            for (cool1 = 0; cool1 < cooldownCount; cool1++) {
                                if (((1 << cool1) & placed & ~hp) != 0) break;
                            }
                            for (cool2 = 0; cool2 < cooldownCount; cool2++) {
                                if (((1 << cool2) & h & ~hp) != 0) break;
                            }
                            seg1 = seg + 1;
                            seg2 = seg;
                            break eliminateCrossing;
                        }
                    }
                }
            }
            if (!valid) {
                int c1 = 1 << cool1;
                int c2 = 1 << cool2;
                // eliminate cool1 - cool2 in seg1
                SolverLP elimLP = lp.clone();
                for (int index = 0; index < this.solutionVariable.size(); index++) {
                    CastingState state = this.solutionVariable.get(index).state;
                    int iseg = this.solutionVariable.get(index).segment;
                    if (state != null && iseg == seg1) {
                        int h = state.getEffects();
                        if ((h & c1) != 0 && (h & c2) == 0) elimLP.eraseColumn(index);
                    }
                }
                if (elimLP.log != null) elimLP.log.append("Doublecrossing at " + seg1 + ", " + cool1 + " - " + cool2).append('\n');
                heapPush(elimLP);
                // eliminate cool2 - cool1 in seg1
                elimLP = lp.clone();
                for (int index = 0; index < this.solutionVariable.size(); index++) {
                    CastingState state = this.solutionVariable.get(index).state;
                    int iseg = this.solutionVariable.get(index).segment;
                    if (state != null && iseg == seg1) {
                        int h = state.getEffects();
                        if ((h & c2) != 0 && (h & c1) == 0) elimLP.eraseColumn(index);
                    }
                }
                if (elimLP.log != null) elimLP.log.append("Doublecrossing at " + seg1 + ", " + cool2 + " - " + cool1).append('\n');
                heapPush(elimLP);
                // eliminate cool1 in seg2
                elimLP = lp.clone();
                for (int index = 0; index < this.solutionVariable.size(); index++) {
                    CastingState state = this.solutionVariable.get(index).state;
                    int iseg = this.solutionVariable.get(index).segment;
                    if (state != null && iseg == seg2) {
                        int h = state.getEffects();
                        if ((h & c1) != 0) elimLP.eraseColumn(index);
                    }
                }
                if (elimLP.log != null) elimLP.log.append("Doublecrossing at " + seg1 + "+1, delete " + cool1).append('\n');
                heapPush(elimLP);
                // eliminate cool2 in seg2
                elimLP = lp;
                for (int index = 0; index < this.solutionVariable.size(); index++) {
                    CastingState state = this.solutionVariable.get(index).state;
                    int iseg = this.solutionVariable.get(index).segment;
                    if (state != null && iseg == seg2) {
                        int h = state.getEffects();
                        if ((h & c2) != 0) elimLP.eraseColumn(index);
                    }
                }
                if (elimLP.log != null) elimLP.log.append("Doublecrossing at " + seg1 + "+1, delete " + cool2).append('\n');
                heapPush(elimLP);
                break;
            }
        }
        return valid;
    }

    private boolean validateIntegralConsumable(VariableType integralConsumable) {
        for (int index = 0; index < this.solutionVariable.size(); index++) {
            if (this.solutionVariable.get(index).type == integralConsumable) {
                double value = solution[index];
                int count = (int) Math.rint(value);
                boolean valid = (Math.abs(value - count) < 0.000001);

                if (!valid) {
                    SolverLP maxCount = lp.clone();
                    // count >= ceiling(value)
                    lp.setColumnLowerBound(index, Math.ceil(value));
                    lp.forceRecalculation(true);
                    if (lp.log != null) lp.log.append("Integral consumable " + integralConsumable + " at " + this.solutionVariable.get(index).segment + ", min " + Math.ceil(value)).append('\n');
                    heapPush(lp);
                    // count <= floor(value)
                    maxCount.setColumnUpperBound(index, Math.floor(value));
                    maxCount.forceRecalculation(true);
                    if (maxCount.log != null) maxCount.log.append("Integral consumable " + integralConsumable + " at " + this.solutionVariable.get(index).segment + ", max " + Math.floor(value)).append('\n');
                    heapPush(maxCount);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean validateIntegralConsumableOverall(VariableType integralConsumable, double unit) {
        double value = 0.0;
        for (int index = 0; index < this.solutionVariable.size(); index++) {
            if (this.solutionVariable.get(index).type == integralConsumable) {
                value += solution[index];
            } else if (integralConsumable == VariableType.Evocation && this.solutionVariable.get(index).type == VariableType.EvocationIV) {
                value += solution[index] * 1.2;
            } else if (integralConsumable == VariableType.Evocation && this.solutionVariable.get(index).type == VariableType.EvocationHero) {
                value += solution[index] * 1.3;
            } else if (integralConsumable == VariableType.Evocation && this.solutionVariable.get(index).type == VariableType.EvocationIVHero) {
                value += solution[index] * 1.2 * 1.3;
            } else if (integralConsumable == VariableType.EvocationHero && this.solutionVariable.get(index).type == VariableType.EvocationIVHero) {
                value += solution[index] * 1.2;
            }
        }
        double count = Math.rint(value / unit) * unit;
        boolean valid = (Math.abs(value - count) < 0.0001); // had to loosen the verification, seem to have problems with hasted evocation, maybe because of the activation constraints?
        int row = -1;
        switch (integralConsumable) {
            case ManaGem:
                row = rowManaGemMax;
                break;
            case ManaPotion:
                row = rowManaPotion;
                break;
            case Evocation:
                row = rowEvocation;
                break;
            case EvocationIV:
                row = rowEvocationIV;
                break;
            case EvocationHero:
                row = rowEvocationHero;
                break;
            case EvocationIVHero:
                row = rowEvocationIVHero;
                break;
            case SummonWaterElemental:
                row = rowSummonWaterElementalCount;
                break;
            case SummonMirrorImage:
                row = rowSummonMirrorImageCount;
                break;
            case ConjureManaGem:
                row = rowConjureManaGem;
                break;
            default:
                break;
        }
        if (!valid) {
            SolverLP maxCount = lp.clone();
            // count <= floor(value)
            maxCount.setRHS(row, Math.floor(value / unit) * unit);
            maxCount.forceRecalculation(true);
            if (maxCount.log != null) maxCount.log.append("Integral consumable " + integralConsumable + " overall, max " + Math.floor(value / unit)).append('\n');
            heapPush(maxCount);
            // count >= ceiling(value)
            lp.setLHS(row, Math.ceil(value / unit) * unit);
            lp.forceRecalculation(true);
            if (lp.log != null) lp.log.append("Integral consumable " + integralConsumable + " overall, min " + Math.ceil(value / unit)).append('\n');
            heapPush(lp);
            return false;
        }
        return true;
    }

    private boolean validateColdsnap() {
        final double eps = 0.000001;
        double[] ivCount = getSegmentCooldownCount(StandardEffect.IcyVeins, VariableType.None);
        double[] weCount = getSegmentCooldownCount(StandardEffect.WaterElemental, VariableType.None);

        // everything is valid, except possibly coldsnap so we can assume the effects are nicely packed
        // check where coldsnaps are needed, similar to evaluation in sequence reconstruction
        boolean ivActive = false;
        boolean weActive = false;
        double lastIVstart = Double.NEGATIVE_INFINITY;
        double lastWEstart = Double.NEGATIVE_INFINITY;
        double ivCooldown = 0.0;
        double weCooldown = 0.0;
        boolean valid = true;
        double coldsnapTimeMin = Double.NEGATIVE_INFINITY;
        double coldsnapTimeMax = Double.NEGATIVE_INFINITY;
        List<Double> ivStart = new ArrayList<Double>();
        List<Double> weStart = new ArrayList<Double>();
        double e1t1 = Double.NEGATIVE_INFINITY;
        double e1t2 = Double.NEGATIVE_INFINITY;
        int e1 = rawr.models.mage.StandardEffect.IcyVeins;
        double e2t1 = Double.NEGATIVE_INFINITY;
        double e2t2 = Double.NEGATIVE_INFINITY;
        int e2 = rawr.models.mage.StandardEffect.IcyVeins;
        for (int seg = 0; seg < this.segmentList.size(); seg++) {
            if (!ivActive && ivCount[seg] > eps) {
                if (ivCooldown + ivCount[seg] > this.segmentList.get(seg).duration + eps) {
                    boolean reuse = false;
                    // we need to coldsnap somewhere from [lastIVstart] and [(seg + 1) * segmentDuration - ivCount[seg]]
                    double restrictedColdsnapMin = Math.max(coldsnapTimeMin, lastIVstart);
                    double restrictedColdsnapMax = Math.min(coldsnapTimeMax, this.segmentList.get(seg).getTimeEnd() - ivCount[seg]);
                    if (restrictedColdsnapMax > restrictedColdsnapMin + eps) {
                        // we can reuse last coldsnap
                        coldsnapTimeMin = restrictedColdsnapMin;
                        coldsnapTimeMax = restrictedColdsnapMax;
                        ivCooldown = 0.0;
                        reuse = true;
                    } else if (this.coldsnapCooldown - (this.segmentList.get(seg).getTimeEnd() - ivCount[seg] - coldsnapTimeMin) <= eps) {
                        // coldsnap is ready
                        coldsnapTimeMin = Math.max(coldsnapTimeMin + this.coldsnapCooldown, lastIVstart);
                        coldsnapTimeMax = this.segmentList.get(seg).getTimeEnd() - ivCount[seg];
                        ivCooldown = 0.0;
                    } else {
                        // coldsnap is not ready
                        ivStart.add(this.segmentList.get(seg).getTimeEnd() - ivCount[seg]);
                        e1 = e2;
                        e1t1 = e2t1;
                        e1t2 = e2t2;
                        e2 = rawr.models.mage.StandardEffect.IcyVeins;
                        e2t1 = ivStart.get(ivStart.size() - 2);
                        e2t2 = ivStart.get(ivStart.size() - 1);
                        valid = false;
                        break;
                    }
                    double ivActivation = Math.max(coldsnapTimeMin, this.segmentList.get(seg).timeStart);
                    if (seg + 1 < this.segmentList.size() && ivCount[seg + 1] > 0) ivActivation = Math.max(coldsnapTimeMin, this.segmentList.get(seg).getTimeEnd() - ivCount[seg]);
                    ivStart.add(ivActivation);
                    if (!reuse) {
                        e1 = e2;
                        e1t1 = e2t1;
                        e1t2 = e2t2;
                        e2 = rawr.models.mage.StandardEffect.IcyVeins;
                        e2t1 = ivStart.get(ivStart.size() - 2);
                        e2t2 = ivStart.get(ivStart.size() - 1);
                    }
                    ivCooldown = this.icyVeinsCooldown + ivActivation - this.segmentList.get(seg).timeStart;
                    ivActive = true;
                    lastIVstart = ivActivation;
                } else {
                    // start as soon as possible
                    double ivActivation = Math.max(this.segmentList.get(seg).timeStart + ivCooldown, this.segmentList.get(seg).timeStart);
                    if (seg + 1 < this.segmentList.size() && ivCount[seg + 1] > 0) ivActivation = this.segmentList.get(seg).getTimeEnd() - ivCount[seg];
                    ivStart.add(ivActivation);
                    ivCooldown = this.icyVeinsCooldown + ivActivation - this.segmentList.get(seg).timeStart;
                    ivActive = true;
                    lastIVstart = ivActivation;
                }
            }
            if (ivActive) {
                if (ivCount[seg] > 0.0) {
                    if (this.segmentList.get(seg).timeStart + ivCount[seg] > lastIVstart + 20.0 + eps) {
                        // we need to coldsnap somewhere from [ivTime] and [ivTime + 20]
                        double restrictedColdsnapMin = Math.max(coldsnapTimeMin, lastIVstart);
                        double restrictedColdsnapMax = Math.min(coldsnapTimeMax, lastIVstart + 20.0);
                        if (restrictedColdsnapMax > restrictedColdsnapMin + eps) {
                            // we can reuse last coldsnap
                            coldsnapTimeMin = restrictedColdsnapMin;
                            coldsnapTimeMax = restrictedColdsnapMax;
                            lastIVstart += 20.0;
                            ivStart.add(lastIVstart);
                            ivCooldown += 20.0;
                        } else if (this.coldsnapCooldown + coldsnapTimeMin <= lastIVstart + 20.0 + eps) {
                            coldsnapTimeMin = Math.max(coldsnapTimeMin + this.coldsnapCooldown, lastIVstart);
                            coldsnapTimeMax = lastIVstart + 20.0;
                            lastIVstart += 20.0;
                            if (ivCount[seg] >= 20.0 - eps && ivCount[seg] <= 20.0 + eps) {
                                // VERY VERY special case, experimental
                                // this IV has room to move around a bit and placing it at the very start
                                // in many cases causes bad coldsnap interaction with WE, so nudge it a bit
                                coldsnapTimeMax += this.baseGlobalCooldown;
                                lastIVstart += this.baseGlobalCooldown;
                            }
                            ivStart.add(lastIVstart);
                            e1 = e2;
                            e1t1 = e2t1;
                            e1t2 = e2t2;
                            e2 = rawr.models.mage.StandardEffect.IcyVeins;
                            e2t1 = ivStart.get(ivStart.size() - 2);
                            e2t2 = ivStart.get(ivStart.size() - 1);
                            ivCooldown += 20.0;
                        }
                        if (this.segmentList.get(seg).timeStart + ivCount[seg] > lastIVstart + 20.0 + eps) {
                            // we need to coldsnap iv, but it is on cooldown
                            ivStart.add(lastIVstart + 20.0);
                            e1 = e2;
                            e1t1 = e2t1;
                            e1t2 = e2t2;
                            e2 = rawr.models.mage.StandardEffect.IcyVeins;
                            e2t1 = ivStart.get(ivStart.size() - 2);
                            e2t2 = ivStart.get(ivStart.size() - 1);
                            valid = false;
                            break;
                        }
                    }
                } else {
                    ivActive = false;
                }
            }
            if (!weActive && weCount[seg] > eps) {
                if (weCooldown + weCount[seg] > this.segmentList.get(seg).duration + eps) {
                    boolean reuse = false;
                    // we need to coldsnap somewhere from [lastWEstart] and [(seg + 1) * segmentDuration - weCount[seg]]
                    double restrictedColdsnapMin = Math.max(coldsnapTimeMin, lastWEstart);
                    double restrictedColdsnapMax = Math.min(coldsnapTimeMax, this.segmentList.get(seg).getTimeEnd() - weCount[seg]);
                    if (restrictedColdsnapMax > restrictedColdsnapMin + eps) {
                        // we can reuse last coldsnap
                        coldsnapTimeMin = restrictedColdsnapMin;
                        coldsnapTimeMax = restrictedColdsnapMax;
                        weCooldown = 0.0;
                        reuse = true;
                    } else if (this.coldsnapCooldown - (this.segmentList.get(seg).getTimeEnd() - weCount[seg] - coldsnapTimeMin) <= eps) {
                        // coldsnap is ready
                        coldsnapTimeMin = Math.max(coldsnapTimeMin + this.coldsnapCooldown, lastWEstart);
                        coldsnapTimeMax = this.segmentList.get(seg).getTimeEnd() - weCount[seg];
                        weCooldown = 0.0;
                    } else {
                        // coldsnap is not ready
                        weStart.add(this.segmentList.get(seg).getTimeEnd() - weCount[seg]);
                        e1 = e2;
                        e1t1 = e2t1;
                        e1t2 = e2t2;
                        e2 = rawr.models.mage.StandardEffect.WaterElemental;
                        e2t1 = weStart.get(weStart.size() - 2);
                        e2t2 = weStart.get(weStart.size() - 1);
                        valid = false;
                        break;
                    }
                    double weActivation = Math.max(coldsnapTimeMin, this.segmentList.get(seg).timeStart);
                    if (seg + 1 < this.segmentList.size() && weCount[seg + 1] > 0) weActivation = Math.max(coldsnapTimeMin, this.segmentList.get(seg).getTimeEnd() - weCount[seg]);
                    weStart.add(weActivation);
                    if (!reuse) {
                        e1 = e2;
                        e1t1 = e2t1;
                        e1t2 = e2t2;
                        e2 = rawr.models.mage.StandardEffect.WaterElemental;
                        e2t1 = weStart.get(weStart.size() - 2);
                        e2t2 = weStart.get(weStart.size() - 1);
                    }
                    weCooldown = this.waterElementalCooldown + weActivation - this.segmentList.get(seg).timeStart;
                    weActive = true;
                    lastWEstart = weActivation;
                } else {
                    // start as soon as possible
                    double weActivation = Math.max(this.segmentList.get(seg).timeStart + weCooldown, this.segmentList.get(seg).timeStart);
                    if (seg + 1 < this.segmentList.size() && weCount[seg + 1] > 0) weActivation = this.segmentList.get(seg).getTimeEnd() - weCount[seg];
                    weStart.add(weActivation);
                    weCooldown = this.waterElementalCooldown + weActivation - this.segmentList.get(seg).timeStart;
                    weActive = true;
                    lastWEstart = weActivation;
                }
            }
            if (weActive) {
                if (weCount[seg] > 0.0) {
                    if (this.segmentList.get(seg).timeStart + weCount[seg] > lastWEstart + this.waterElementalDuration + eps) {
                        // we need to coldsnap somewhere from [weTime] and [weTime + 45]
                        double restrictedColdsnapMin = Math.max(coldsnapTimeMin, lastWEstart);
                        double restrictedColdsnapMax = Math.min(coldsnapTimeMax, lastWEstart + this.waterElementalDuration);
                        if (restrictedColdsnapMax > restrictedColdsnapMin + eps) {
                            // we can reuse last coldsnap
                            coldsnapTimeMin = restrictedColdsnapMin;
                            coldsnapTimeMax = restrictedColdsnapMax;
                            lastWEstart += this.waterElementalDuration;
                            weStart.add(lastWEstart);
                            weCooldown += this.waterElementalDuration;
                        } else if (this.coldsnapCooldown + coldsnapTimeMin <= lastWEstart + this.waterElementalDuration + eps) {
                            coldsnapTimeMin = Math.max(coldsnapTimeMin + this.coldsnapCooldown, lastWEstart);
                            coldsnapTimeMax = lastWEstart + this.waterElementalDuration;
                            lastWEstart += this.waterElementalDuration;
                            weStart.add(lastWEstart);
                            e1 = e2;
                            e1t1 = e2t1;
                            e1t2 = e2t2;
                            e2 = rawr.models.mage.StandardEffect.WaterElemental;
                            e2t1 = weStart.get(weStart.size() - 2);
                            e2t2 = weStart.get(weStart.size() - 1);
                            weCooldown += this.waterElementalDuration;
                        }
                        if (this.segmentList.get(seg).timeStart + weCount[seg] > lastWEstart + this.waterElementalDuration + eps) {
                            // we need to coldsnap iv, but it is on cooldown
                            weStart.add(lastWEstart + this.waterElementalDuration);
                            e1 = e2;
                            e1t1 = e2t1;
                            e1t2 = e2t2;
                            e2 = rawr.models.mage.StandardEffect.WaterElemental;
                            e2t1 = weStart.get(weStart.size() - 2);
                            e2t2 = weStart.get(weStart.size() - 1);
                            valid = false;
                            break;
                        }
                    }
                } else {
                    weActive = false;
                }
            }
            ivCooldown -= this.segmentList.get(seg).duration;
            weCooldown -= this.segmentList.get(seg).duration;
        }
        if (!valid) {
            // there are three possible constructions that are not valid
            // 3 consecutive activations of same cooldown
            // 2 pairs of activations of same cooldown
            // 2 pairs of activations of different cooldowns
            // each configuration has a different way to resolve infeasibility

            // in some cases reusing of coldsnap for two effects will miss the fact that we have coldsnap problem within the same cooldown
            // those should have priority, because if they are not resolved they cause problem with estimating cross cooldown behavior
            if (e1 != e2) {
                int firstColdsnap = -1;
                for (int i = 0; i < ivStart.size() - 1; i++) {
                    if (ivStart.get(i + 1) - ivStart.get(i) < this.icyVeinsCooldown - eps) {
                        if (firstColdsnap == -1) {
                            firstColdsnap = i;
                        } else {
                            if (ivStart.get(i + 1) - ivStart.get(firstColdsnap) < this.coldsnapCooldown - eps) {
                                // we have intra cooldown problem
                                e1t1 = ivStart.get(firstColdsnap);
                                e1t2 = ivStart.get(firstColdsnap + 1);
                                e2t1 = ivStart.get(i);
                                e2t2 = ivStart.get(i + 1);
                                e1 = rawr.models.mage.StandardEffect.IcyVeins;
                                e2 = rawr.models.mage.StandardEffect.IcyVeins;
                                break;
                            } else {
                                firstColdsnap = i;
                            }
                        }
                    }
                }
                if (e1 != e2) {
                    firstColdsnap = -1;
                    for (int i = 0; i < weStart.size() - 1; i++) {
                        if (weStart.get(i + 1) - weStart.get(i) < this.waterElementalCooldown - eps) {
                            if (firstColdsnap == -1) {
                                firstColdsnap = i;
                            } else {
                                if (weStart.get(i + 1) - weStart.get(firstColdsnap) < this.coldsnapCooldown - eps) {
                                    // we have intra cooldown problem
                                    e1t1 = weStart.get(firstColdsnap);
                                    e1t2 = weStart.get(firstColdsnap + 1);
                                    e2t1 = weStart.get(i);
                                    e2t2 = weStart.get(i + 1);
                                    e1 = rawr.models.mage.StandardEffect.WaterElemental;
                                    e2 = rawr.models.mage.StandardEffect.WaterElemental;
                                    break;
                                } else {
                                    firstColdsnap = i;
                                }
                            }
                        }
                    }
                }
            }

            if (e1 == e2) {
                // 2 pairs of activations of same cooldown, t3 can be equal to t2
                double t1 = e1t1;
                double t2 = e1t2;
                double t3 = e2t1;
                double t4 = e2t2;
                int e = e1;
                double c = (e == rawr.models.mage.StandardEffect.IcyVeins) ? this.icyVeinsCooldown : this.waterElementalCooldown;
                double d = (e == rawr.models.mage.StandardEffect.IcyVeins) ? 20.0 : this.waterElementalDuration;

                // branch on one of the conditions responsible for infeasibility
                // t2 - t1 < c1
                enforceEffectCooldown(t1, e, c, d, true);
                // t4 - t3 < c2
                enforceEffectCooldown(t3, e, c, d, true);
                // t4 - t1 < cs
                // cs is at least 384 sec
                // notice that t3 >= t2, then t4 - t1 <= (t4 - t3) + (t2 - t1)
                // to break this condition at least one of pairs has to be at min 192 sec apart
                // since IV and WE cooldown is 180 sec at most this means that breaking this
                // constraint automatically breaks one of the other constraints also
            } else {
                // 2 pairs of activations of different cooldowns
                double t1 = e1t1;
                double t2 = e1t2;
                double t3 = e2t1;
                double t4 = e2t2;
                double c1 = (e1 == rawr.models.mage.StandardEffect.IcyVeins) ? this.icyVeinsCooldown : this.waterElementalCooldown;
                double d1 = (e1 == rawr.models.mage.StandardEffect.IcyVeins) ? 20.0 : this.waterElementalDuration;
                double c2 = (e2 == rawr.models.mage.StandardEffect.IcyVeins) ? this.icyVeinsCooldown : this.waterElementalCooldown;
                double d2 = (e2 == rawr.models.mage.StandardEffect.IcyVeins) ? 20.0 : this.waterElementalDuration;

                // branch on one of the conditions responsible for infeasibility
                // t2 - t1 < c1
                enforceEffectCooldown(t1, e1, c1, d1, true);
                // t4 - t3 < c2
                enforceEffectCooldown(t3, e2, c2, d2, true);
                // t3 >= t2
                // [----]             [-----]
                //                      [------------][-------------]
                int seg2 = findIndexTimeEndAfter(t2 + eps);
                int seg3 = findIndexTimeEndAfter(t3 + eps);
                // assume e2 will start in seg3, if it does not that is already examined in t4 - t3 < c2 case
                // so in this case we have to force e1 to not start before seg3
                SolverLP branchlp = lp.clone();
                if (branchlp.log != null) branchlp.log.append("Force shared coldsnap activation after " + seg3).append('\n');
                if (seg2 < seg3) disableCooldown(branchlp, e1, seg2, seg3 - 1);
                // make sure t2 is after t3
                // but it has to be at least a gcd later, or we won't have enough room to coldsnap
                // t2 = (seg3 + 1) * segmentDuration - count1[seg3]
                // t3 = (seg3 + 1) * segmentDuration - count2[seg3]
                // count1[seg3] - count2[seg3] <= - gcd
                // if e1 is WE then forcing t2 after t3 will cause WE summon to be during IV active
                // this is currently not supported, so make sure in this case the shift is big enough that summon comes after IV is over
                int row = branchlp.addConstraint();
                setCooldownElements(branchlp, row, e1, seg3, 1.0);
                setCooldownElements(branchlp, row, e2, seg3, -1.0);
                if (e1 == rawr.models.mage.StandardEffect.WaterElemental) {
                    branchlp.setConstraintRHS(row, -20.0);
                } else {
                    branchlp.setConstraintRHS(row, -this.baseGlobalCooldown);
                }
                branchlp.forceRecalculation(true);
                heapPush(branchlp);
                if (e2 == rawr.models.mage.StandardEffect.IcyVeins) {
                    // in this case we have an additional option to nudge IV not to cross segments
                    branchlp = lp.clone();
                    if (branchlp.log != null) branchlp.log.append("Force shared coldsnap activation after " + seg3 + ", nudge IV").append('\n');
                    if (seg2 < seg3) disableCooldown(branchlp, e1, seg2, seg3 - 1);
                    disableCooldown(branchlp, e2, seg3 + 1);
                    heapPush(branchlp);
                }
                // t4 - t1 < cs
                // cs is at least 384 sec
                // if t3 >= t2, then similar to above at least one pair has to be 192 sec apart
                // if not then the t3 >= t2 condition is broken, so we can ignore this constraint in this case also
            }
            // lp unused
            lp.releaseConstraints();
            return false;
        }
        return valid;
    }

    private void enforceRemoteEffectCooldown(int seg1, int seg3, int cooldowns, int effect, double effectCooldown, double effectDuration) {
        final double eps = 0.000001;
        SolverLP branchlp = lp.clone();
        if (branchlp.log != null) branchlp.log.append("Restrict remote activation of " + effect + " at " + seg1).append('\n');
        // make sure there is enough distance between the activations
        int row = branchlp.addConstraint();
        setCooldownElements(branchlp, row, effect, seg1, -1.0);
        setCooldownElements(branchlp, row, effect, seg3, 1.0);
        branchlp.setConstraintRHS(row, this.segmentList.get(seg3).getTimeEnd() - this.segmentList.get(seg1).getTimeEnd() - cooldowns * effectCooldown);
        branchlp.forceRecalculation(true);
        heapPush(branchlp);
        if (effectDuration < this.segmentList.get(seg1).duration - eps) {
            double buffer = 0.0;
            branchlp = lp.clone();
            if (branchlp.log != null) branchlp.log.append("Restrict remote activation of " + effect + " at " + seg1 + ", version for short effects").append('\n');
            // make sure seg1 is actually a free activation
            disableCooldown(branchlp, effect, seg1 + 1);
            row = addConstraint(branchlp, effect, seg3);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg3).getTimeEnd() - this.segmentList.get(seg1).timeStart - cooldowns * effectCooldown - buffer);
            branchlp.forceRecalculation(true);
            heapPush(branchlp);
        }
        // if first activation moves to seg1 - 1
        if (seg1 >= 1) restrictConsecutiveActivation(effect, effectCooldown, effectDuration, seg1 - 1, false);
        // if seg1 is disabled
        branchlp = lp.clone();
        if (branchlp.log != null) branchlp.log.append("Enforce remote cooldown after " + effect + " at " + seg1 + ", remove effect at " + seg1).append('\n');
        disableCooldown(branchlp, effect, seg1);
        heapPush(branchlp);
        // if seg3 is disabled
        branchlp = lp.clone();
        if (branchlp.log != null) branchlp.log.append("Enforce remote cooldown after " + effect + " at " + seg1 + ", remove effect at " + seg3).append('\n');
        disableCooldown(branchlp, effect, seg3);
        heapPush(branchlp);
        // restrict intermediate activations
        branchlp = lp.clone();
        if (branchlp.log != null) branchlp.log.append("Enforce remote cooldown after " + effect + " at " + seg1 + ", remove intermediate effects").append('\n');
        row = addConstraint(branchlp, effect, seg1, seg3 - 1);
        branchlp.setConstraintRHS(row, (cooldowns - 1) * effectDuration);
        branchlp.forceRecalculation(true);
        heapPush(branchlp);
    }

    private void branchOnLeftPadding(ActivationConstraints act, int effect) {
        SolverLP branchlp;
        int seg = act.segment;
        // dropping right link: remove joint items with effect in seg1 or remove items in seg1+1 for each effect from right link
        for (int cool = 0; cool < cooldownCount; cool++) {
            int cooldown = (1 << cool);
            if (cooldown != effect && (cooldown & act.rightLink) != 0) {
                branchlp = lp.clone();
                if (branchlp.log != null) branchlp.log.append("Remove left padding for " + effect + " at " + seg + ", drop right link here " + cooldown).append('\n');
                disableCooldown(branchlp, effect | cooldown, seg);
                branchlp.forceRecalculation(true);
                heapPush(branchlp);
                branchlp = lp.clone();
                if (branchlp.log != null) branchlp.log.append("Remove left padding for " + effect + " at " + seg + ", drop right link there " + cooldown).append('\n');
                disableCooldown(branchlp, cooldown, seg + 1);
                branchlp.forceRecalculation(true);
                heapPush(branchlp);
            }
        }
        // dropping left full link: remove items in seg1 or in seg1-1 for all effects from full left link that are not in left link
        for (int cool = 0; cool < cooldownCount; cool++) {
            int cooldown = (1 << cool);
            if (cooldown != effect && (cooldown & act.fullLeftLink & ~act.leftLink) != 0) {
                branchlp = lp.clone();
                if (branchlp.log != null) branchlp.log.append("Remove left padding for " + effect + " at " + seg + ", drop left full link here " + cooldown).append('\n');
                disableCooldown(branchlp, cooldown, seg);
                branchlp.forceRecalculation(true);
                heapPush(branchlp);
                branchlp = lp.clone();
                if (branchlp.log != null) branchlp.log.append("Remove left padding for " + effect + " at " + seg + ", drop left full link there " + cooldown).append('\n');
                disableCooldown(branchlp, cooldown, seg - 1);
                branchlp.forceRecalculation(true);
                heapPush(branchlp);
            }
        }
        // increasing left link: for each effect not in left link force there to be some left link+effect items on both sides
        if (seg > 0) {
            for (int cool = 0; cool < cooldownCount; cool++) {
                int cooldown = (1 << cool);
                if (cooldown != effect && (cooldown & availableCooldownMask & ~act.leftLink) != 0) {
                    // these are expensive branches, hopefully they die out quick
                    branchlp = lp.clone();
                    if (branchlp.log != null) branchlp.log.append("Remove left padding for " + effect + " at " + seg + ", increase left link " + cooldown).append('\n');
                    int row = addConstraint(branchlp, cooldown, seg - 1);
                    branchlp.setConstraintRHS(row, this.segmentList.get(seg - 1).duration);
                    branchlp.setConstraintLHS(row, 0.1);
                    row = addConstraint(branchlp, effect | cooldown, seg);
                    branchlp.setConstraintRHS(row, this.segmentList.get(seg).duration);
                    branchlp.setConstraintLHS(row, 0.1);
                    branchlp.forceRecalculation(true);
                    heapPush(branchlp);
                }
            }
        }
    }

    private void branchOnRightPadding(ActivationConstraints act, int effect) {
        SolverLP branchlp;
        int seg = act.segment;
        // dropping right link: remove joint items with effect in seg1 or remove items in seg1+1 for each effect from right link
        for (int cool = 0; cool < cooldownCount; cool++) {
            int cooldown = (1 << cool);
            if (cooldown != effect && (cooldown & act.leftLink) != 0) {
                branchlp = lp.clone();
                if (branchlp.log != null) branchlp.log.append("Remove right padding for " + effect + " at " + seg + ", drop left link here " + cooldown).append('\n');
                disableCooldown(branchlp, effect | cooldown, seg);
                branchlp.forceRecalculation(true);
                heapPush(branchlp);
                branchlp = lp.clone();
                if (branchlp.log != null) branchlp.log.append("Remove right padding for " + effect + " at " + seg + ", drop left link there " + cooldown).append('\n');
                disableCooldown(branchlp, cooldown, seg - 1);
                branchlp.forceRecalculation(true);
                heapPush(branchlp);
            }
        }
        // dropping left full link: remove items in seg1 or in seg1-1 for all effects from full left link that are not in left link
        for (int cool = 0; cool < cooldownCount; cool++) {
            int cooldown = (1 << cool);
            if (cooldown != effect && (cooldown & act.fullRightLink & ~act.rightLink) != 0) {
                branchlp = lp.clone();
                if (branchlp.log != null) branchlp.log.append("Remove right padding for " + effect + " at " + seg + ", drop right full link here " + cooldown).append('\n');
                disableCooldown(branchlp, cooldown, seg);
                branchlp.forceRecalculation(true);
                heapPush(branchlp);
                branchlp = lp.clone();
                if (branchlp.log != null) branchlp.log.append("Remove right padding for " + effect + " at " + seg + ", drop right full link there " + cooldown).append('\n');
                disableCooldown(branchlp, cooldown, seg + 1);
                branchlp.forceRecalculation(true);
                heapPush(branchlp);
            }
        }
        // increasing left link: for each effect not in left link force there to be some left link+effect items on both sides
        if (seg + 1 < this.segmentList.size()) {
            for (int cool = 0; cool < cooldownCount; cool++) {
                int cooldown = (1 << cool);
                if (cooldown != effect && (cooldown & availableCooldownMask & ~act.rightLink) != 0) {
                    // these are expensive branches, hopefully they die out quick
                    branchlp = lp.clone();
                    if (branchlp.log != null) branchlp.log.append("Remove right padding for " + effect + " at " + seg + ", increase right link " + cooldown).append('\n');
                    int row = addConstraint(branchlp, cooldown, seg + 1);
                    branchlp.setConstraintRHS(row, this.segmentList.get(seg + 1).duration);
                    branchlp.setConstraintLHS(row, 0.1);
                    row = addConstraint(branchlp, effect | cooldown, seg);
                    branchlp.setConstraintRHS(row, this.segmentList.get(seg).duration);
                    branchlp.setConstraintLHS(row, 0.1);
                    branchlp.forceRecalculation(true);
                    heapPush(branchlp);
                }
            }
        }
    }

    private void enforceEffectCooldown(ActivationConstraints act1, ActivationConstraints act2, int effect, double effectCooldown, double effectDuration) {
        final double eps = 0.000001;

        // if we have no padding or if both are fixed use the simpler version
        if ((act1.leftPaddding < eps || !act1.loose) && (act2.rightPadding < eps || !act2.loose)) {
            enforceEffectCooldown(act1.minTime, effect, effectCooldown, effectDuration, false);
            return;
        }

        int seg1 = act1.segment;
        int seg2 = act2.segment;
        SolverLP branchlp;
        if (act1.leftPaddding < eps || !act1.loose) {
            // only right padding
            branchOnRightPadding(act2, effect);
            // either act1 is loose with no padding
            branchlp = lp.clone();
            if (branchlp.log != null) branchlp.log.append("Restrict consecutive activation of " + effect + " at " + seg1 + " and " + seg2 + ", act1 loose with no padding").append('\n');
            disableCooldown(branchlp, effect, seg1 + 1, seg2 - 1);
            int row = branchlp.addConstraint();
            setCooldownElements(branchlp, row, effect, seg2, 1.0);
            setRightPaddingElements(branchlp, row, act2, effect, 1.0);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg2).getTimeEnd() - this.segmentList.get(seg1).timeStart - effectCooldown);
            row = addConstraint(branchlp, effect, seg1);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg1).duration);
            branchlp.setConstraintLHS(row, 0.1);
            row = addConstraint(branchlp, effect, seg2);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg2).duration);
            branchlp.setConstraintLHS(row, 0.1);
            branchlp.forceRecalculation(true);
            heapPush(branchlp);
            // or act1 is fixed
            branchlp = lp.clone();
            if (branchlp.log != null) branchlp.log.append("Restrict consecutive activation of " + effect + " at " + seg1 + " and " + seg2 + ", act1 fixed").append('\n');
            disableCooldown(branchlp, effect, findIndexTimeStartAtLeast(this.segmentList.get(seg1).getTimeEnd() + effectDuration - eps), seg2 - 1);
            row = branchlp.addConstraint();
            setCooldownElements(branchlp, row, effect, seg2, 1.0);
            setCooldownElements(branchlp, row, effect, seg1, -1.0);
            setRightPaddingElements(branchlp, row, act2, effect, 1.0);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg2).getTimeEnd() - this.segmentList.get(seg1).getTimeEnd() - effectCooldown);
            row = addConstraint(branchlp, effect, seg1);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg1).duration);
            branchlp.setConstraintLHS(row, 0.1);
            row = addConstraint(branchlp, effect, seg2);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg2).duration);
            branchlp.setConstraintLHS(row, 0.1);
            branchlp.forceRecalculation(true);
            heapPush(branchlp);
        } else if (act2.rightPadding < eps || !act2.loose) {
            // only left padding
            branchOnLeftPadding(act1, effect);
            branchlp = lp.clone();
            if (branchlp.log != null) branchlp.log.append("Restrict consecutive activation of " + effect + " at " + seg1 + " and " + seg2 + ", only left padding").append('\n');
            disableCooldown(branchlp, effect, findIndexTimeStartAtLeast(this.segmentList.get(seg1).getTimeEnd() + effectDuration - eps), seg2 - 1);
            int row = branchlp.addConstraint();
            setCooldownElements(branchlp, row, effect, seg2, 1.0);
            setLeftPaddingElements(branchlp, row, act1, effect, 1.0);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg2).getTimeEnd() - this.segmentList.get(seg1).timeStart - effectCooldown);
            row = addConstraint(branchlp, effect, seg1);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg1).duration);
            branchlp.setConstraintLHS(row, 0.1);
            row = addConstraint(branchlp, effect, seg2);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg2).duration);
            branchlp.setConstraintLHS(row, 0.1);
            branchlp.forceRecalculation(true);
            heapPush(branchlp);
        } else {
            // both paddings
            branchOnLeftPadding(act1, effect);
            branchOnRightPadding(act2, effect);
            branchlp = lp.clone();
            if (branchlp.log != null) branchlp.log.append("Restrict consecutive activation of " + effect + " at " + seg1 + " and " + seg2 + ", both paddings").append('\n');
            disableCooldown(branchlp, effect, findIndexTimeStartAtLeast(this.segmentList.get(seg1).getTimeEnd() + effectDuration - eps), seg2 - 1);
            int row = branchlp.addConstraint();
            setCooldownElements(branchlp, row, effect, seg2, 1.0);
            setRightPaddingElements(branchlp, row, act2, effect, 1.0);
            setLeftPaddingElements(branchlp, row, act1, effect, 1.0);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg2).getTimeEnd() - this.segmentList.get(seg1).timeStart - effectCooldown);
            row = addConstraint(branchlp, effect, seg1);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg1).duration);
            branchlp.setConstraintLHS(row, 0.1);
            row = addConstraint(branchlp, effect, seg2);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg2).duration);
            branchlp.setConstraintLHS(row, 0.1);
            branchlp.forceRecalculation(true);
            heapPush(branchlp);
        }
        // there is effect before seg1
        if (seg1 >= 1) {
            branchlp = lp.clone();
            if (branchlp.log != null) branchlp.log.append("Enforce cooldown after " + effect + " at " + seg1 + " and " + seg2 + ", activation before").append('\n');
            int row = addConstraint(branchlp, effect, seg1 - 1);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg1 - 1).duration);
            branchlp.setConstraintLHS(row, 0.1);
            branchlp.forceRecalculation(true);
            heapPush(branchlp);
        }
        // or there is no effect in seg1
        branchlp = lp.clone();
        if (branchlp.log != null) branchlp.log.append("Enforce cooldown after " + effect + " at " + seg1 + " and " + seg2 + ", remove effect").append('\n');
        disableCooldown(branchlp, effect, seg1);
        heapPush(branchlp);
        // there is effect before seg2
        if (seg2 >= 1) {
            branchlp = lp.clone();
            if (branchlp.log != null) branchlp.log.append("Enforce cooldown after " + effect + " at " + seg1 + " and " + seg2 + ", activation2 before").append('\n');
            int row = addConstraint(branchlp, effect, seg2 - 1);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg2 - 1).duration);
            branchlp.setConstraintLHS(row, 0.1);
            branchlp.forceRecalculation(true);
            heapPush(branchlp);
        }
        // or there is no effect in seg2
        branchlp = lp.clone();
        if (branchlp.log != null) branchlp.log.append("Enforce cooldown after " + effect + " at " + seg1 + " and " + seg2 + ", remove effect2").append('\n');
        disableCooldown(branchlp, effect, seg2);
        heapPush(branchlp);
    }

    private void enforceEffectCooldown(double firstEffectActivation, int effect, double effectCooldown, double effectDuration, boolean needsToCleanCloseActivations) {
        final double eps = 0.000001;
        int seg1 = findIndexTimeEndAfter(firstEffectActivation + eps);
        SolverLP branchlp;
        // if we have effect in seg1 then either
        // there is activation in seg1
        restrictConsecutiveActivation(effect, effectCooldown, effectDuration, seg1, needsToCleanCloseActivations);
        // there is effect before seg1
        if (seg1 >= 1) {
            branchlp = lp.clone();
            if (branchlp.log != null) branchlp.log.append("Enforce cooldown after " + effect + " at " + seg1 + ", activation before").append('\n');
            int row = addConstraint(branchlp, effect, seg1 - 1);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg1 - 1).duration);
            branchlp.setConstraintLHS(row, 0.1);
            branchlp.forceRecalculation(true);
            heapPush(branchlp);
        }
        // or there is no effect in seg1
        branchlp = lp.clone();
        if (branchlp.log != null) branchlp.log.append("Enforce cooldown after " + effect + " at " + seg1 + ", remove effect").append('\n');
        disableCooldown(branchlp, effect, seg1);
        heapPush(branchlp);
    }

    private boolean segmentContainsEffect(int segment, int effect) {
        int hex = effect;
        for (int h : hexList[segment]) {
            if ((hex & h) == hex) {
                return true;
            }
        }
        return false;
    }

    private boolean segmentContainsVariable(int segment, VariableType variable) {
        final double eps = 0.000001;
        for (int index = 0; index < this.solutionVariable.size(); index++) {
            if (this.solutionVariable.get(index).segment == segment && this.solutionVariable.get(index).type == variable) {
                if (solution[index] > eps) return true;
            }
        }
        return false;
    }

    private boolean segmentContainsCooldown(int cooldown, VariableType cooldownType, int minSegment, int maxSegment) {
        final double eps = 0.000001;
        double[] segCount = getSegmentCooldownCount(cooldown, cooldownType);
        for (int seg = 0; seg < this.segmentList.size(); seg++) {
            if (segCount[seg] > eps && seg >= minSegment && seg <= maxSegment) return true;
        }
        return false;
    }

    private void restrictConsecutiveActivation(int effect, double effectCooldown, double effectDuration, int firstActivationSegment, boolean needsToCleanCloseActivations) {
        final double eps = 0.000001;
        int seg2 = findIndexTimeEndAfter(this.segmentList.get(firstActivationSegment).timeStart + effectCooldown + eps);
        SolverLP branchlp;
        int row;
        if (seg2 == -1) {
            // in this case the only thing we can enforce is that there are no subsequent activations
            branchlp = lp.clone();
            if (branchlp.log != null) branchlp.log.append("Restrict all activation of " + effect + " after " + firstActivationSegment).append('\n');
            disableCooldown(branchlp, effect, findIndexTimeStartAtLeast(this.segmentList.get(firstActivationSegment).getTimeEnd() + effectDuration - eps), this.segmentList.size() - 1);
            heapPush(branchlp);
            return;
        }
        if (-effectDuration <= this.segmentList.get(seg2).getTimeEnd() - this.segmentList.get(firstActivationSegment).getTimeEnd() - effectCooldown) {
            branchlp = lp.clone();
            if (branchlp.log != null) branchlp.log.append("Restrict consecutive activation of " + effect + " at " + firstActivationSegment).append('\n');
            // first make sure that second activation is not too close
            if (needsToCleanCloseActivations) {
                row = addConstraint(branchlp, effect, firstActivationSegment, seg2 - 1);
                branchlp.setConstraintRHS(row, effectDuration);
            }
            // make sure there is enough distance between the activations
            row = branchlp.addConstraint();
            setCooldownElements(branchlp, row, effect, firstActivationSegment, -1.0);
            setCooldownElements(branchlp, row, effect, seg2, 1.0);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg2).getTimeEnd() - this.segmentList.get(firstActivationSegment).getTimeEnd() - effectCooldown);
            // make sure it actually starts in this segment
            row = addConstraint(branchlp, effect, firstActivationSegment);
            branchlp.setConstraintRHS(row, this.segmentList.get(firstActivationSegment).duration);
            branchlp.setConstraintLHS(row, 0.1);
            branchlp.forceRecalculation(true);
            heapPush(branchlp);
        }
        // there is another case if cooldown duration is funny (i.e. IV)
        if (seg2 + 1 < this.segmentList.size()) {
            branchlp = lp.clone();
            if (branchlp.log != null) branchlp.log.append("Restrict consecutive activation of " + effect + " at " + firstActivationSegment + ", long distance").append('\n');
            if (needsToCleanCloseActivations) {
                row = addConstraint(branchlp, effect, firstActivationSegment, seg2);
                branchlp.setConstraintRHS(row, effectDuration);
            }
            disableCooldown(branchlp, effect, seg2); // have to disable it in seg2, normal constraints don't enforce this
            row = branchlp.addConstraint();
            setCooldownElements(branchlp, row, effect, firstActivationSegment, -1.0);
            setCooldownElements(branchlp, row, effect, seg2 + 1, 1.0);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg2 + 1).getTimeEnd() - this.segmentList.get(firstActivationSegment).getTimeEnd() - effectCooldown);
            row = addConstraint(branchlp, effect, firstActivationSegment);
            branchlp.setConstraintRHS(row, this.segmentList.get(firstActivationSegment).duration);
            branchlp.setConstraintLHS(row, 0.1);
            branchlp.forceRecalculation(true);
            heapPush(branchlp);
        }
        if (effectDuration < this.segmentList.get(firstActivationSegment).duration - eps) {
            // the effect has some room for movement, but we have to be careful because it can't be moved
            // to the very beginning of segment in all cases
            double buffer = 0.0;
            if (effect == rawr.models.mage.StandardEffect.IcyVeins) {
                if (segmentContainsEffect(firstActivationSegment, StandardEffect.IcyVeins | StandardEffect.WaterElemental)) {
                    if (segmentContainsVariable(firstActivationSegment, VariableType.SummonWaterElemental)) {
                        buffer = this.baseGlobalCooldown;
                        // it is possible to force effect to the start of segment if we eliminate summoning
                        branchlp = lp.clone();
                        if (branchlp.log != null) branchlp.log.append("Restrict consecutive activation of " + effect + " at " + firstActivationSegment + ", force to start of segment by eliminating summoning").append('\n');
                        // first make sure that second activation is not too close
                        disableCooldown(branchlp, effect, firstActivationSegment + 1, seg2 - 1);
                        disableVariable(branchlp, VariableType.SummonWaterElemental, firstActivationSegment);
                        row = addConstraint(branchlp, effect, seg2);
                        branchlp.setConstraintRHS(row, this.segmentList.get(seg2).getTimeEnd() - this.segmentList.get(firstActivationSegment).timeStart - effectCooldown);
                        row = addConstraint(branchlp, effect, firstActivationSegment);
                        branchlp.setConstraintRHS(row, this.segmentList.get(firstActivationSegment).duration);
                        branchlp.setConstraintLHS(row, 0.1);
                        branchlp.forceRecalculation(true);
                        heapPush(branchlp);
                    }
                }
            }
            branchlp = lp.clone();
            if (branchlp.log != null) branchlp.log.append("Restrict consecutive activation of " + effect + " at " + firstActivationSegment + ", version for short effects").append('\n');
            // first make sure that second activation is not too close
            disableCooldown(branchlp, effect, firstActivationSegment + 1, seg2 - 1);
            row = addConstraint(branchlp, effect, seg2);
            branchlp.setConstraintRHS(row, this.segmentList.get(seg2).getTimeEnd() - this.segmentList.get(firstActivationSegment).timeStart - effectCooldown - buffer);
            row = addConstraint(branchlp, effect, firstActivationSegment);
            branchlp.setConstraintRHS(row, this.segmentList.get(firstActivationSegment).duration);
            branchlp.setConstraintLHS(row, 0.1);
            branchlp.forceRecalculation(true);
            heapPush(branchlp);
        }
    }

    private void setCooldownElements(SolverLP branchlp, int row, int effects, int segment, double value) {
        setCooldownElements(branchlp, row, effects, segment, segment, value);
    }

    private void setCooldownElements(SolverLP branchlp, int row, int effects, int minSegment, int maxSegment, double value) {
        for (int index = 0; index < this.solutionVariable.size(); index++) {
            CastingState state = this.solutionVariable.get(index).state;
            if (state != null && state.effectsActive(effects)) {
                int seg = this.solutionVariable.get(index).segment;
                if (seg >= minSegment && seg <= maxSegment) branchlp.setConstraintElement(row, index, value);
            }
        }
    }

    private void setCooldownElements(SolverLP branchlp, int row, int effects, VariableType cooldownType, int minSegment, int maxSegment, double value) {
        for (int index = 0; index < this.solutionVariable.size(); index++) {
            int seg = this.solutionVariable.get(index).segment;
            if (seg >= minSegment && seg <= maxSegment && this.solutionVariable.get(index).isMatch(effects, cooldownType)) branchlp.setConstraintElement(row, index, value);
        }
    }

    private void setVariableElements(SolverLP branchlp, int row, VariableType variable, int segment, double value) {
        setVariableElements(branchlp, row, variable, segment, segment, value);
    }

    private void setVariableElements(SolverLP branchlp, int row, VariableType variable, int minSegment, int maxSegment, double value) {
        for (int index = 0; index < this.solutionVariable.size(); index++) {
            if (this.solutionVariable.get(index).type == variable) {
                int seg = this.solutionVariable.get(index).segment;
                if (seg >= minSegment && seg <= maxSegment) branchlp.setConstraintElement(row, index, value);
            }
        }
    }

    private void setLeftPaddingElements(SolverLP branchlp, int row, ActivationConstraints activation, int effect, double value) {
        for (int index = 0; index < this.solutionVariable.size(); index++) {
            CastingState state = this.solutionVariable.get(index).state;
            if (state != null && this.solutionVariable.get(index).segment == activation.segment && !this.solutionVariable.get(index).isZeroTime()) {
                int c = state.getEffects();
                // item is included for left padding if it doesn't include all right link
                // or if it includes more than left link from full left link
                if ((c & effect) == 0 && ((c & activation.rightLink) != activation.rightLink || ((c & activation.leftLink) == activation.leftLink && (c & activation.fullLeftLink) != activation.leftLink))) {
                    branchlp.setConstraintElement(row, index, value);
                }
            }
        }
    }

    private void setRightPaddingElements(SolverLP branchlp, int row, ActivationConstraints activation, int effect, double value) {
        for (int index = 0; index < this.solutionVariable.size(); index++) {
            CastingState state = this.solutionVariable.get(index).state;
            if (state != null && this.solutionVariable.get(index).segment == activation.segment && !this.solutionVariable.get(index).isZeroTime()) {
                int c = state.getEffects();
                // item is included for left padding if it doesn't include all right link
                // or if it includes more than left link from full left link
                if ((c & effect) == 0 && ((c & activation.leftLink) != activation.leftLink || ((c & activation.rightLink) == activation.rightLink && (c & activation.fullRightLink) != activation.rightLink))) {
                    branchlp.setConstraintElement(row, index, value);
                }
            }
        }
    }

    private void disableVariable(SolverLP branchlp, VariableType variable, int segment) {
        disableVariable(branchlp, variable, segment, segment);
    }

    private void disableCooldown(SolverLP branchlp, int effect, int segment) {
        disableCooldown(branchlp, effect, segment, segment);
    }

    private void disableVariable(SolverLP branchlp, VariableType variable, int minSegment, int maxSegment) {
        for (int index = 0; index < this.solutionVariable.size(); index++) {
            if (this.solutionVariable.get(index).type == variable) {
                int seg = this.solutionVariable.get(index).segment;
                if (seg >= minSegment && seg <= maxSegment) branchlp.eraseColumn(index);
            }
        }
    }

    private void disableCooldown(SolverLP branchlp, int effect, int minSegment, int maxSegment) {
        for (int index = 0; index < this.solutionVariable.size(); index++) {
            CastingState state = this.solutionVariable.get(index).state;
            if (state != null && state.effectsActive(effect)) {
                int seg = this.solutionVariable.get(index).segment;
                if (seg >= minSegment && seg <= maxSegment) branchlp.eraseColumn(index);
            }
        }
    }

    private void disableCooldown(SolverLP branchlp, int effect, VariableType cooldownType, int minSegment, int maxSegment) {
        for (int index = 0; index < this.solutionVariable.size(); index++) {
            if (this.solutionVariable.get(index).isMatch(effect, cooldownType)) {
                int seg = this.solutionVariable.get(index).segment;
                if (seg >= minSegment && seg <= maxSegment) branchlp.eraseColumn(index);
            }
        }
    }

    private void disableCooldown(SolverLP branchlp, int effect, VariableType cooldownType, IntPredicate includeSegment) {
        for (int index = 0; index < this.solutionVariable.size(); index++) {
            if (this.solutionVariable.get(index).isMatch(effect, cooldownType)) {
                int seg = this.solutionVariable.get(index).segment;
                if (includeSegment.test(seg)) branchlp.eraseColumn(index);
            }
        }
    }

    private boolean validateCooldown(int effect, double effectDuration, double cooldownDuration) {
        return validateCooldown(effect, effectDuration, cooldownDuration, false, effectDuration, null, VariableType.None);
    }

    private boolean validateWaterElementalSummon() {
        int cooldown = rawr.models.mage.StandardEffect.WaterElemental;
        double effectDuration = this.waterElementalDuration;
        double cooldownDuration = this.waterElementalCooldown;
        VariableType activation = VariableType.SummonWaterElemental;

        double[] segCount = getSegmentCooldownCount(cooldown, VariableType.None);
        double[] segActivation = getSegmentCooldownCount(0, activation);

        boolean valid = true;

        // special summon validation with coldsnap, without coldsnap use ValidateActivation

        return valid;
    }

    // Determines if effect in two segments could potentially belong to the same effect activation.
    private boolean inActivationDistance(int seg1, int seg2, double effectDuration) {
        if (seg1 > seg2) {
            int tmp = seg1;
            seg1 = seg2;
            seg2 = tmp;
        }
        return this.segmentList.get(seg2).timeStart - this.segmentList.get(seg1).getTimeEnd() < effectDuration - 0.00001;
    }

    private boolean inActivationDistance(int seg1min, int seg1max, int seg2, double effectDuration) {
        if (seg2 > seg1max) {
            return this.segmentList.get(seg2).timeStart - this.segmentList.get(seg1max).getTimeEnd() < effectDuration - 0.00001;
        } else if (seg2 < seg1min) {
            return this.segmentList.get(seg1min).timeStart - this.segmentList.get(seg2).getTimeEnd() < effectDuration - 0.00001;
        }
        return true;
    }

    // Determines if effect in two segments must necessarily belong to the same cooldown
    private boolean inCooldownDistance(int seg1, int seg2, double effectDuration, double cooldownDuration) {
        if (seg1 > seg2) {
            int tmp = seg1;
            seg1 = seg2;
            seg2 = tmp;
        }
        return this.segmentList.get(seg2).getTimeEnd() - Math.max(0.0, this.segmentList.get(seg1).timeStart - effectDuration) <= cooldownDuration + 0.00001;
    }

    private boolean inCooldownDistance(int seg1min, int seg1max, int seg2, double effectDuration, double cooldownDuration) {
        if (seg2 > seg1max) {
            return this.segmentList.get(seg2).getTimeEnd() - Math.max(0.0, this.segmentList.get(seg1min).timeStart - effectDuration) <= cooldownDuration + 0.00001;
        } else if (seg2 < seg1min) {
            return this.segmentList.get(seg1max).getTimeEnd() - Math.max(0.0, this.segmentList.get(seg2).timeStart - effectDuration) <= cooldownDuration + 0.00001;
        }
        return true;
    }

    private boolean validateActivation(int effect, VariableType cooldownType, double effectDuration, double cooldownDuration, VariableType activation, int activationEffect) {
        final double eps = 0.00001;
        double[] segCount = getSegmentCooldownCount(effect, cooldownType);
        double[] segActivation = getSegmentCooldownCount(activationEffect, activation);

        boolean valid = true;

        for (int seg = 0; seg < this.segmentList.size(); seg++) {
            int segmin = resolution[seg].minSegment;
            int segmax = resolution[seg].maxSegment;
            if (segActivation[seg] > eps) {
                for (int s = 0; s < this.segmentList.size(); s++) {
                    if (inCooldownDistance(segmin, segmax, s, effectDuration, cooldownDuration) && s < segmin && segCount[s] > eps) valid = false;
                }
                if (!valid) {
                    // can't have effect before activation
                    // either activation is not here or effect is not before it
                    SolverLP cooldownUsed = lp.clone();
                    // activation not used
                    if (lp.log != null) lp.log.append("Disable activation of " + activation.toString() + " at " + segmin + "-" + segmax).append('\n');
                    disableCooldown(lp, activationEffect, activation, segmin, segmax);
                    heapPush(lp);
                    if (cooldownUsed.log != null) cooldownUsed.log.append("No " + effectString(effect) + " before activation at " + segmin).append('\n');
                    disableCooldown(cooldownUsed, effect, cooldownType, findIndexTimeStartAtLeastForCooldown(segmin, segmax, effectDuration, cooldownDuration), segmin - 1);
                    heapPush(cooldownUsed);
                    return false;
                }
            }
        }

        // each effect needs activation
        for (int seg = 0; seg < this.segmentList.size(); seg++) {
            int segmin = resolution[seg].minSegment;
            int segmax = resolution[seg].maxSegment;
            int prevsegmin = 0;
            int prevsegmax = 0;
            if (segmin > 0) {
                prevsegmin = resolution[segmin - 1].minSegment;
                prevsegmax = resolution[segmin - 1].maxSegment;
            }
            if (segCount[seg] > eps && (segmin == 0 || !segmentContainsCooldown(effect, cooldownType, prevsegmin, prevsegmax))) {
                if (!segmentContainsCooldown(activationEffect, activation, prevsegmin, segmax)) {
                    // either there is no effect or there is activation
                    SolverLP cooldownUsed = lp.clone();
                    // force activation
                    if (lp.log != null) lp.log.append("Force activation of " + activation + " at " + prevsegmin + "-" + segmax).append('\n');
                    int row = addConstraint(lp, activationEffect, activation, prevsegmin, segmax);
                    lp.setConstraintRHS(row, this.segmentList.get(segmax).getTimeEnd() - this.segmentList.get(prevsegmin).timeStart);
                    lp.setConstraintLHS(row, 0.1);
                    lp.forceRecalculation(true);
                    heapPush(lp);
                    // no effect
                    if (cooldownUsed.log != null) cooldownUsed.log.append("No " + effect + "-" + cooldownType + " without activation at " + segmin + "-" + segmax).append('\n');
                    disableCooldown(cooldownUsed, effect, cooldownType, segmin, segmax);
                    heapPush(cooldownUsed);
                    return false;
                }
            }
        }

        return true;
    }

    private String effectString(int effect) {
        return ((Integer) effect).toString();
    }

    private int findIndexTimeStartAtLeastForCooldown(int segmin, int segmax, double effectDuration, double cooldownDuration) {
        for (int i = 0; i < this.segmentList.size(); i++) {
            if (inCooldownDistance(segmin, segmax, this.segmentList.get(i).index, effectDuration, cooldownDuration)) return i;
        }
        return -1;
    }

    private boolean validateActivationAdvanced(int effect, VariableType cooldownType, double effectDuration, double cooldownDuration, VariableType activation, int activationEffect) {
        final double eps = 0.00001;
        double[] segCount = getSegmentCooldownCount(effect, cooldownType);
        double[] segActivation = getSegmentCooldownCount(activationEffect, activation);

        for (int i = 0; i < segmentColumn[0]; i++) { // fix if variable ordering changes
            if (this.solutionVariable.get(i).isMatch(activationEffect, activation) && solution[i] > eps) {
                int seg = this.solutionVariable.get(i).segment;
                int seghex = this.solutionVariable.get(i).state.getEffects();
                // check all cooldowns that link to this drums activation
                // if any of them is also before this segment we have a cycle if it's not present at activation
                int linkedHex = 0;
                for (int s = 0; s < this.segmentList.size(); s++) {
                    if (inActivationDistance(s, seg, effectDuration)) {
                        for (int index = segmentColumn[s]; index < segmentColumn[s + 1]; index++) {
                            CastingState state = this.solutionVariable.get(index).state;
                            if (this.solutionVariable.get(index).isMatch(effect, cooldownType) && solution[index] > eps) {
                                linkedHex |= state.getEffects();
                            }
                        }
                    }
                }
                for (int index = 0; index < segmentColumn[0]; index++) { // fix if variable ordering changes
                    CastingState state = this.solutionVariable.get(index).state;
                    if (inActivationDistance(seg, this.solutionVariable.get(index).segment, effectDuration) && this.solutionVariable.get(index).isMatch(effect, cooldownType) && solution[index] > eps) {
                        linkedHex |= state.getEffects();
                    }
                }

                int brokenHex = 0;
                for (int s = 0; s < this.segmentList.size(); s++) {
                    if (s == seg - 1) {
                        for (int index = segmentColumn[s]; index < segmentColumn[s + 1]; index++) {
                            CastingState state = this.solutionVariable.get(index).state;
                            if (state != null && solution[index] > eps) {
                                int h = state.getEffects() & linkedHex & ~seghex;
                                if (h != 0) brokenHex = h;
                            }
                        }
                    }
                }
                for (int index = 0; index < segmentColumn[0]; index++) { // fix if variable ordering changes
                    CastingState state = this.solutionVariable.get(index).state;
                    if (state != null && solution[index] > eps) {
                        int outseg = this.solutionVariable.get(index).segment;
                        if (outseg == seg - 1) {
                            int h = state.getEffects() & linkedHex & ~seghex;
                            if (h != 0) brokenHex = h;
                        }
                    }
                }
                if (brokenHex != 0) {
                    // either we don't have activation that is without broken hex or drums casting with hex or hex before activation
                    SolverLP drumsnohex = lp.clone();
                    if (drumsnohex.log != null) drumsnohex.log.append(effect + " without hex").append('\n');
                    for (int s = 0; s < this.segmentList.size(); s++) {
                        if (inActivationDistance(seg, s, effectDuration)) {
                            for (int index = segmentColumn[s]; index < segmentColumn[s + 1]; index++) {
                                CastingState state = this.solutionVariable.get(index).state;
                                if (state != null && this.solutionVariable.get(index).isMatch(effect, cooldownType) && (state.getEffects() & brokenHex) != 0) drumsnohex.eraseColumn(index);
                            }
                        }
                    }
                    heapPush(drumsnohex);

                    SolverLP nohex = lp.clone();
                    if (nohex.log != null) nohex.log.append("No hex").append('\n');
                    for (int s = 0; s < this.segmentList.size(); s++) {
                        if (s == seg - 1) {
                            for (int index = segmentColumn[s]; index < segmentColumn[s + 1]; index++) {
                                CastingState state = this.solutionVariable.get(index).state;
                                if (state != null && (state.getEffects() & brokenHex) != 0) nohex.eraseColumn(index);
                            }
                        }
                    }
                    heapPush(nohex);

                    if (lp.log != null) lp.log.append("Disable activation of " + activation.toString() + " at " + seg + " without hex").append('\n');
                    for (int index = 0; index < segmentColumn[0]; index++) { // fix if variable ordering changes
                        if (this.solutionVariable.get(index).isMatch(activationEffect, activation) && this.solutionVariable.get(index).segment == seg && (this.solutionVariable.get(index).state.getEffects() & brokenHex) != brokenHex) lp.eraseColumn(index);
                    }
                    heapPush(lp);
                    return false;
                }

                // another class of activation cycles is if drums link into next segment, this
                // causes drums to be full from activation to the end of segment so anything in this or previous
                // segment that links to some of the drums has to cross the activation and so has to be present
                // in the activation

                // first check if drums are present in next segment
                if (seg < this.segmentList.size() - 1) {
                    boolean cooldownInNext = false;
                    int cooldownHex = activationEffect;
                    for (int j = 0; j < hexList[seg + 1].size(); j++) {
                        if ((hexList[seg + 1].get(j) & cooldownHex) != 0) {
                            cooldownInNext = true;
                            break;
                        }
                    }
                    if (cooldownInNext) {
                        // check that effect has activation cooldown if it extends into next segment
                        int extendedCooldown = (cooldownHex & hexMask[seg + 1]);
                        for (int j = 0; j < hexList[seg].size(); j++) {
                            if ((hexList[seg].get(j) & effect) == effect && (hexList[seg].get(j) & extendedCooldown) != extendedCooldown) {
                                // we have a problem, extended cooldown is not present during effect even though it's present at activation and in next segment
                                // identify which part of extended cooldown is responsible
                                int h = hexList[seg].get(j);
                                int ind = 1;
                                while ((ind & ~h & extendedCooldown) == 0) ind <<= 1;
                                // either all effect has this cooldown or this effect is not present at activation or it is not present in next segment
                                SolverLP branchlp = lp.clone();
                                if (branchlp.log != null) branchlp.log.append("Disable " + effect + " without " + ind + " at " + seg).append('\n');
                                for (int index = 0; index < this.solutionVariable.size(); index++) {
                                    CastingState state = this.solutionVariable.get(index).state;
                                    if (this.solutionVariable.get(index).segment == seg) {
                                        if (state != null && state.effectsActive(effect) && !state.effectsActive(ind)) {
                                            branchlp.eraseColumn(index);
                                        }
                                    }
                                }
                                heapPush(branchlp);
                                branchlp = lp.clone();
                                if (branchlp.log != null) branchlp.log.append("Disable " + ind + " at activation of " + effect + " at " + seg).append('\n');
                                for (int index = 0; index < this.solutionVariable.size(); index++) {
                                    CastingState state = this.solutionVariable.get(index).state;
                                    if (this.solutionVariable.get(index).segment == seg) {
                                        if (state != null && this.solutionVariable.get(index).isMatch(activationEffect, activation) && state.effectsActive(ind)) {
                                            branchlp.eraseColumn(index);
                                        }
                                    }
                                }
                                heapPush(branchlp);
                                branchlp = lp;
                                disableCooldown(branchlp, ind, seg + 1);
                                heapPush(branchlp);
                                return false;
                            }
                        }

                        // check what links to drums, we already have all that is active during drums in linkedHex
                        for (int ss = Math.max(0, seg - 1); ss <= seg; ss++) {
                            for (int j = 0; j < hexList[ss].size(); j++) {
                                int h = hexList[ss].get(j);
                                if ((h & linkedHex) != 0 && (h & cooldownHex) == 0 && (h & linkedHex & ~seghex) != 0) {
                                    // we found an item that is before activation that has cooldown not present at activation
                                    // identify one such cooldown
                                    int ind = 1;
                                    while ((ind & h & linkedHex & ~seghex) == 0) ind <<= 1;
                                    // we branch
                                    // either activation has this cooldown, this cooldown without drums is not present in this or previous segment
                                    // or drums don't link to this cooldown, or drums don't extend to next segment

                                    // no cooldown without drums
                                    SolverLP drumsnohex = lp.clone();
                                    if (drumsnohex.log != null) drumsnohex.log.append("Disable " + ind + " without drums before activation at " + seg).append('\n');
                                    for (int s = 0; s < this.segmentList.size(); s++) {
                                        if (s >= seg - 1 && s <= seg) {
                                            for (int index = segmentColumn[s]; index < segmentColumn[s + 1]; index++) {
                                                CastingState state = this.solutionVariable.get(index).state;
                                                if (state != null && !this.solutionVariable.get(index).isMatch(effect, cooldownType) && (state.getEffects() & ind) != 0) drumsnohex.eraseColumn(index);
                                            }
                                        }
                                    }
                                    heapPush(drumsnohex);

                                    // no cooldown with drums
                                    drumsnohex = lp.clone();
                                    if (drumsnohex.log != null) drumsnohex.log.append("Disable " + ind + " with drums in " + seg).append('\n');
                                    for (int s = 0; s < this.segmentList.size(); s++) {
                                        if (inActivationDistance(seg, s, effectDuration)) {
                                            for (int index = segmentColumn[s]; index < segmentColumn[s + 1]; index++) {
                                                CastingState state = this.solutionVariable.get(index).state;
                                                if (state != null && this.solutionVariable.get(index).isMatch(effect, cooldownType) && (state.getEffects() & ind) != 0) drumsnohex.eraseColumn(index);
                                            }
                                        }
                                    }
                                    heapPush(drumsnohex);

                                    // drums don't extend to next segment
                                    drumsnohex = lp.clone();
                                    if (drumsnohex.log != null) drumsnohex.log.append("Disable drums in next segment " + (seg + 1)).append('\n');
                                    for (int s = 0; s < this.segmentList.size(); s++) {
                                        if (s == seg + 1) {
                                            for (int index = segmentColumn[s]; index < segmentColumn[s + 1]; index++) {
                                                CastingState state = this.solutionVariable.get(index).state;
                                                if (state != null && this.solutionVariable.get(index).isMatch(effect, cooldownType)) drumsnohex.eraseColumn(index);
                                            }
                                        }
                                    }
                                    heapPush(drumsnohex);

                                    // activation has this cooldown
                                    if (lp.log != null) lp.log.append("Disable activation of " + activation.toString() + " at " + seg + " without hex").append('\n');
                                    for (int index = 0; index < segmentColumn[0]; index++) { // fix if variable ordering changes
                                        if (this.solutionVariable.get(index).type == activation && this.solutionVariable.get(index).state.effectsActive(activationEffect) && this.solutionVariable.get(index).segment == seg && (this.solutionVariable.get(index).state.getEffects() & ind) == 0) lp.eraseColumn(index);
                                    }
                                    heapPush(lp);
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    private boolean validateCooldown(int effect, double effectDuration, double cooldownDuration, boolean needsFullEffect, double fullEffectDuration, List<SegmentConstraint> segmentConstraints, VariableType cooldownType) {
        final double eps = 0.00002;
        double[] segCount = getSegmentCooldownCount(effect, cooldownType);

        if (cooldownDuration < 0) cooldownDuration = 3 * this.calculationOptions.getFightDuration();

        boolean valid = true;

        if (needsFullEffect) {
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                if (segCount[seg] > 0.0 && this.segmentList.get(seg).getTimeEnd() + effectDuration < this.calculationOptions.getFightDuration()) {
                    // extend to resolution
                    int segmin = resolution[seg].minSegment;
                    int segmax = resolution[seg].maxSegment;
                    // check
                    double total = 0.0;
                    double localtotal = 0.0;
                    for (int s = 0; s < this.segmentList.size(); s++) {
                        if (inActivationDistance(segmin, segmax, s, effectDuration)) total += segCount[s];
                        if (Math.abs(seg - s) <= 1) localtotal += segCount[s];
                    }
                    if (localtotal > fullEffectDuration + eps && total < effectDuration - eps) {
                        // there might be other similar cases, but I'm afraid to overrestrict
                        // two branches, either force to full full total or restrict to just one
                        SolverLP cooldownUsed = lp.clone();
                        if (cooldownUsed.log != null) cooldownUsed.log.append("Force total full " + effectString(effect) + " at " + seg).append('\n');
                        int row = cooldownUsed.addConstraint();
                        for (int s = 0; s < this.segmentList.size(); s++) {
                            if (inActivationDistance(seg, s, effectDuration)) {
                                for (int index = segmentColumn[s]; index < segmentColumn[s + 1]; index++) {
                                    if (this.solutionVariable.get(index).isMatch(effect, cooldownType)) cooldownUsed.setConstraintElement(row, index, -1.0);
                                }
                            }
                        }
                        for (int index = 0; index < segmentColumn[0]; index++) { // fix if variable ordering changes
                            if (this.solutionVariable.get(index).isMatch(effect, cooldownType)) {
                                int outseg = this.solutionVariable.get(index).segment;
                                if (inActivationDistance(seg, outseg, effectDuration)) cooldownUsed.setConstraintElement(row, index, -1.0);
                            }
                        }
                        cooldownUsed.setConstraintRHS(row, -effectDuration);
                        cooldownUsed.forceRecalculation(true);
                        heapPush(cooldownUsed);
                        cooldownUsed = lp;
                        if (cooldownUsed.log != null) cooldownUsed.log.append("Limit total full " + effectString(effect) + " at " + seg).append('\n');
                        row = cooldownUsed.addConstraint();
                        for (int s = 0; s < this.segmentList.size(); s++) {
                            if (inActivationDistance(seg, s, effectDuration)) {
                                for (int index = segmentColumn[s]; index < segmentColumn[s + 1]; index++) {
                                    if (this.solutionVariable.get(index).isMatch(effect, cooldownType)) cooldownUsed.setConstraintElement(row, index, 1.0);
                                }
                            }
                        }
                        for (int index = 0; index < segmentColumn[0]; index++) { // fix if variable ordering changes
                            if (this.solutionVariable.get(index).isMatch(effect, cooldownType)) {
                                int outseg = this.solutionVariable.get(index).segment;
                                if (inActivationDistance(seg, outseg, effectDuration)) cooldownUsed.setConstraintElement(row, index, 1.0);
                            }
                        }
                        cooldownUsed.setConstraintRHS(row, fullEffectDuration);
                        cooldownUsed.forceRecalculation(true);
                        heapPush(cooldownUsed);
                        return false;
                    }
                    if (total < fullEffectDuration - 0.0001) // loosened tolerance, sometimes it can't enforce
                    {
                        // check if we can reuse an existing constraint
                        int minseg = -1;
                        int maxseg = -1;
                        boolean foundConstraint = false;
                        SegmentConstraint constraint = new SegmentConstraint();
                        minseg = -1;
                        maxseg = -1;
                        for (int s = 0; s < this.segmentList.size(); s++) {
                            if (inActivationDistance(segmin, segmax, s, effectDuration)) {
                                if (minseg == -1) minseg = s;
                                maxseg = s;
                            }
                        }
                        foundConstraint = false;
                        if (segmentConstraints != null) {
                            for (SegmentConstraint c : segmentConstraints) {
                                if (c.minSegment <= minseg && c.maxSegment >= maxseg) {
                                    foundConstraint = true;
                                    constraint = c;
                                    break;
                                }
                            }
                        }
                        if (foundConstraint && Math.abs(lp.getRHS(constraint.row) - fullEffectDuration) < eps) {
                            SolverLP cooldownUsed = lp.clone();
                            if (cooldownUsed.log != null) cooldownUsed.log.append("Force full " + effectString(effect) + " at " + segmin + "-" + segmax).append('\n');
                            cooldownUsed.setLHS(constraint.row, fullEffectDuration);
                            if (constraint.minSegment < minseg) disableCooldown(cooldownUsed, effect, cooldownType, constraint.minSegment, minseg - 1);
                            if (constraint.maxSegment > maxseg) disableCooldown(cooldownUsed, effect, cooldownType, maxseg + 1, constraint.maxSegment);
                            // make sure something is in segmin-segmax or we're duplicating work
                            int row = addConstraint(cooldownUsed, effect, cooldownType, segmin, segmax);
                            cooldownUsed.setConstraintRHS(row, this.segmentList.get(segmax).getTimeEnd() - this.segmentList.get(segmin).timeStart);
                            cooldownUsed.setConstraintLHS(row, 0.1);
                            cooldownUsed.forceRecalculation(true);
                            heapPush(cooldownUsed);
                        } else {
                            SolverLP cooldownUsed = lp.clone();
                            if (cooldownUsed.log != null) cooldownUsed.log.append("Force full " + effectString(effect) + " at " + segmin + "-" + segmax).append('\n');
                            int row = cooldownUsed.addConstraint();
                            for (int s = 0; s < this.segmentList.size(); s++) {
                                if (s >= minseg && s <= maxseg) {
                                    for (int index = segmentColumn[s]; index < segmentColumn[s + 1]; index++) {
                                        if (this.solutionVariable.get(index).isMatch(effect, cooldownType)) cooldownUsed.setConstraintElement(row, index, -1.0);
                                    }
                                }
                            }
                            for (int index = 0; index < segmentColumn[0]; index++) { // fix if variable ordering changes
                                if (this.solutionVariable.get(index).isMatch(effect, cooldownType)) {
                                    int outseg = this.solutionVariable.get(index).segment;
                                    if (outseg >= minseg && outseg <= maxseg) cooldownUsed.setConstraintElement(row, index, -1.0);
                                }
                                if (effect == rawr.models.mage.StandardEffect.FlameCap && integralMana) {
                                    // if we push flame cap to full in this segment then we can further restrict mana gems to eliminate unnecessary MIP branches
                                    if (this.solutionVariable.get(index).type == VariableType.ManaGem) {
                                        int outseg = this.solutionVariable.get(index).segment;
                                        if (inCooldownDistance(seg, outseg, 0.0, 120.0)) cooldownUsed.eraseColumn(index);
                                    }
                                }
                            }
                            cooldownUsed.setConstraintRHS(row, -fullEffectDuration);
                            // make sure something is in segmin-segmax or we're duplicating work
                            row = addConstraint(cooldownUsed, effect, cooldownType, segmin, segmax);
                            cooldownUsed.setConstraintRHS(row, this.segmentList.get(segmax).getTimeEnd() - this.segmentList.get(segmin).timeStart);
                            cooldownUsed.setConstraintLHS(row, 0.1);
                            cooldownUsed.forceRecalculation(true);
                            heapPush(cooldownUsed);
                        }
                        // cooldown not used
                        if (lp.log != null) lp.log.append("Disable " + effectString(effect) + " at " + segmin + "-" + segmax).append('\n');
                        disableCooldown(lp, effect, cooldownType, segmin, segmax);
                        heapPush(lp);
                        return false;
                    }
                }
            }
        }

        for (int seg = 0; seg < this.segmentList.size(); seg++) {
            double inseg = segCount[seg];
            if (inseg > eps) {
                // extend to resolution
                int segmin = resolution[seg].minSegment;
                int segmax = resolution[seg].maxSegment;
                // verify that outside duration segments are 0
                for (int outseg = 0; outseg < this.segmentList.size(); outseg++) {
                    if (!inActivationDistance(segmin, segmax, outseg, effectDuration) && inCooldownDistance(segmin, segmax, outseg, effectDuration, cooldownDuration)) {
                        if (segCount[outseg] > eps) {
                            valid = false;
                            break;
                        }
                    }
                }
                if (!valid) {
                    // branch on whether cooldown is used in this segment
                    SolverLP cooldownUsed = lp.clone();
                    // cooldown used
                    if (cooldownUsed.log != null) cooldownUsed.log.append("Use " + effectString(effect) + " at " + segmin + "-" + segmax + ", disable around").append('\n');
                    final int fsegmin = segmin;
                    final int fsegmax = segmax;
                    final double feffectDuration = effectDuration;
                    final double fcooldownDuration = cooldownDuration;
                    disableCooldown(cooldownUsed, effect, cooldownType, outseg -> !inActivationDistance(fsegmin, fsegmax, outseg, feffectDuration) && inCooldownDistance(fsegmin, fsegmax, outseg, feffectDuration, fcooldownDuration));
                    // make sure something is in segmin-segmax or we're duplicating work
                    int row = addConstraint(cooldownUsed, effect, cooldownType, segmin, segmax);
                    cooldownUsed.setConstraintRHS(row, this.segmentList.get(segmax).getTimeEnd() - this.segmentList.get(segmin).timeStart);
                    cooldownUsed.setConstraintLHS(row, 0.1);
                    cooldownUsed.forceRecalculation(true);
                    heapPush(cooldownUsed);
                    // cooldown not used
                    if (lp.log != null) lp.log.append("Disable " + effectString(effect) + " at " + segmin + "-" + segmax).append('\n');
                    disableCooldown(lp, effect, cooldownType, segmin, segmax);
                    heapPush(lp);
                    return false;
                }
            }
        }

        // detect holes
        for (int seg = 0; seg < this.segmentList.size(); seg++) {
            double inseg = segCount[seg];
            if (inseg > eps) {
                int lastseg = -1;
                for (int outseg = seg + 1; outseg < this.segmentList.size() && inActivationDistance(seg, outseg, effectDuration); outseg++) {
                    if (segCount[outseg] > eps) lastseg = outseg;
                }
                if (lastseg != -1) {
                    // extend to resolution
                    int segmin = resolution[seg].minSegment;
                    int segmax = resolution[seg].maxSegment;
                    int lastsegmin = resolution[lastseg].minSegment;
                    int lastsegmax = resolution[lastseg].maxSegment;

                    for (int outseg = segmax + 1; outseg < lastsegmin; outseg++) {
                        if (segCount[outseg] < this.segmentList.get(outseg).duration - eps) valid = false;
                    }
                    if (!valid) // coldsnapped icy veins doesn't have to be contiguous, but getting better results assuming it is
                    {
                        // either seg must be disabled, lastseg disabled, or middle to max
                        SolverLP leftDisabled = lp.clone();
                        SolverLP rightDisabled = lp.clone();
                        // do force to full first if all are equally good
                        if (lp.log != null) lp.log.append("Force " + effectString(effect) + " to max from " + segmax + " to " + lastsegmin).append('\n');
                        for (int outseg = 0; outseg < this.segmentList.size(); outseg++) {
                            if (outseg > segmax && outseg < lastsegmin) {
                                for (int index = segmentColumn[outseg]; index < segmentColumn[outseg + 1]; index++) {
                                    CastingState state = this.solutionVariable.get(index).state;
                                    if (!this.solutionVariable.get(index).isMatch(effect, cooldownType)) lp.eraseColumn(index);
                                }
                            }
                        }
                        for (int index = 0; index < segmentColumn[0]; index++) { // fix if variable ordering changes
                            CastingState state = this.solutionVariable.get(index).state;
                            int outseg = this.solutionVariable.get(index).segment;
                            if (outseg > segmax && outseg < lastsegmin) {
                                if (!this.solutionVariable.get(index).isMatch(effect, cooldownType) && !this.solutionVariable.get(index).isZeroTime()) lp.eraseColumn(index); // don't remove zero-length variables
                            }
                        }
                        for (int outseg = segmax + 1; outseg < lastsegmin; outseg++) {
                            lp.setLHS(rowSegment + outseg, this.segmentList.get(outseg).duration);
                        }
                        lp.forceRecalculation(true);
                        heapPush(lp);
                        if (leftDisabled.log != null) leftDisabled.log.append("Disable left " + effectString(effect) + " at " + segmin + "-" + segmax).append('\n');
                        disableCooldown(leftDisabled, effect, cooldownType, segmin, segmax);
                        heapPush(leftDisabled);
                        if (rightDisabled.log != null) rightDisabled.log.append("Disable right " + effectString(effect) + " at " + lastsegmin + "-" + lastsegmax).append('\n');
                        disableCooldown(rightDisabled, effect, cooldownType, lastsegmin, lastsegmax);
                        heapPush(rightDisabled);
                        return false;
                    }
                }
            }
        }

        return valid;
    }

    private boolean validateCooldownAdvanced(int effect, double effectDuration, double cooldownDuration, VariableType cooldownType) {
        final double eps = 0.00001;
        double[] segCount = getSegmentCooldownCount(effect, cooldownType);

        // for irregular cooldown durations have to make a special pass verifying everything is in order
        // if cooldowns are broken need to add special constraints that ensure cooldowns are respected
        // do this only for effects that can't be coldsnapped as those don't have to respect cooldown always and are handled separately

        if (effect != 0 && (!coldsnapAvailable || (effect != rawr.models.mage.StandardEffect.WaterElemental && effect != rawr.models.mage.StandardEffect.IcyVeins))) {
            List<Integer> activations = new ArrayList<Integer>();
            // validate consecutive activations
            double lastStart = Double.NEGATIVE_INFINITY;
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                if (segCount[seg] > eps && (seg == 0 || segCount[seg - 1] < eps)) {
                    activations.add(seg);
                    // activation at the latest (seg + 1) * segmentDuration - segCount[seg]
                    // make sure (seg + 1) * segmentDuration - segCount[seg] - lastStart >= cooldownDuration
                    if (this.segmentList.get(seg).getTimeEnd() - segCount[seg] - lastStart < cooldownDuration - eps) {
                        enforceEffectCooldown(lastStart, effect, cooldownDuration, effectDuration, false);
                        // lp unused
                        lp.releaseConstraints();
                        return false;
                    }
                    lastStart = this.segmentList.get(seg).timeStart;
                    if (seg + 1 < this.segmentList.size() && segCount[seg + 1] > eps) lastStart = this.segmentList.get(seg).getTimeEnd() - segCount[seg];
                }
            }

            for (int cooldowns = 2; cooldowns < activations.size(); cooldowns++) {
                for (int act = 0; act < activations.size() - cooldowns; act++) {
                    int seg1 = activations.get(act);
                    int seg3 = activations.get(act + cooldowns);
                    // first activation at seg1 * segmentDuration or (seg1 + 1) * segmentDuration - segCount[seg1] if activation is right bound
                    double firstActivation = this.segmentList.get(seg1).timeStart;
                    if (seg1 + 1 < this.segmentList.size() && segCount[seg1 + 1] > eps) firstActivation = this.segmentList.get(seg1).getTimeEnd() - segCount[seg1];
                    // remote activation at (seg3 + 1) * segmentDuration - segCount[seg3]
                    // make sure (seg3 + 1) * segmentDuration - segCount[seg3] - firstActivation >= cooldowns * cooldownDuration
                    if (this.segmentList.get(seg3).getTimeEnd() - segCount[seg3] - firstActivation < cooldowns * cooldownDuration - eps) {
                        enforceRemoteEffectCooldown(seg1, seg3, cooldowns, effect, cooldownDuration, effectDuration);
                        // lp unused
                        lp.releaseConstraints();
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean validateCooldownAdvanced2(int effect, double effectDuration, double cooldownDuration, VariableType cooldownType) {
        final double eps = 0.00001;
        double[] segCount = getSegmentCooldownCount(effect, cooldownType);

        // for irregular cooldown durations have to make a special pass verifying everything is in order
        // if cooldowns are broken need to add special constraints that ensure cooldowns are respected
        // do this only for effects that can't be coldsnapped as those don't have to respect cooldown always and are handled separately

        if (effect != 0 && (!coldsnapAvailable || (effect != rawr.models.mage.StandardEffect.WaterElemental && effect != rawr.models.mage.StandardEffect.IcyVeins))) {
            List<ActivationConstraints> activations = new ArrayList<ActivationConstraints>();
            // validate consecutive activations
            double lastStart = Double.NEGATIVE_INFINITY;
            ActivationConstraints lastActivation = null;
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                if (segCount[seg] > eps && (seg == 0 || segCount[seg - 1] < eps)) {
                    ActivationConstraints activation = getActivationConstraints(effect, seg, segCount[seg]);
                    activations.add(activation);
                    // activation at the latest (seg + 1) * segmentDuration - segCount[seg]
                    // make sure (seg + 1) * segmentDuration - segCount[seg] - lastStart >= cooldownDuration
                    if (activation.maxTime - lastStart < cooldownDuration - eps) {
                        enforceEffectCooldown(lastActivation, activation, effect, cooldownDuration, effectDuration);
                        // lp unused
                        lp.releaseConstraints();
                        return false;
                    }
                    lastStart = activation.minTime;
                    lastActivation = activation;
                }
            }
        }

        return true;
    }

    private ActivationConstraints getActivationConstraints(int effect, int segment, double effectInSegment) {
        ActivationConstraints result = new ActivationConstraints();
        result.segment = segment;
        result.loose = true;
        int hex = effect;
        int N = hexList[segment].size();
        for (int i = 0; i < N; i++) {
            if ((effect & hexList[segment].get(i)) != 0) {
                hex |= hexList[segment].get(i);
            }
        }
        result.jointEffect = hex;
        if (segment < this.segmentList.size() - 1) {
            result.rightLink = (hex & hexMask[segment + 1]);
            result.fullRightLink = (hexMask[segment] & hexMask[segment + 1]);
        }
        if (segment > 0) {
            result.leftLink = (hex & hexMask[segment - 1]);
            result.fullLeftLink = (hexMask[segment] & hexMask[segment - 1]);
        }
        result.maxTime = this.segmentList.get(segment).getTimeEnd() - effectInSegment;
        result.minTime = this.segmentList.get(segment).timeStart;
        // things that are linked, but don't include the full link have to be on the other side of the link
        // things that are linked and include the full link and link some more have to be on the link
        // things that are linked and include the full link and don't link anything else can be on either side
        // things that are not linked have to be on the other side if there is a link
        // things that are not linked can be on either side if there is no link
        final double eps = 0.00001;
        for (int index = segmentColumn[segment]; index < segmentColumn[segment + 1]; index++) {
            if (solution[index] > eps) {
                CastingState state = this.solutionVariable.get(index).state;
                if (state != null && !state.effectsActive(effect)) {
                    boolean hasToBeLeft = (state.getEffects() & result.rightLink) != result.rightLink || ((state.getEffects() & result.leftLink) == result.leftLink && (state.getEffects() & result.fullLeftLink) != result.leftLink);
                    boolean hasToBeRight = (state.getEffects() & result.leftLink) != result.leftLink || ((state.getEffects() & result.rightLink) == result.rightLink && (state.getEffects() & result.fullRightLink) != result.rightLink);
                    if (hasToBeLeft) {
                        result.minTime += solution[index];
                        result.leftPaddding += solution[index];
                    }
                    if (hasToBeRight) {
                        result.maxTime -= solution[index];
                        result.rightPadding += solution[index];
                    }
                }
            }
        }
        for (int index = 0; index < segmentColumn[0]; index++) { // fix if variable ordering changes
            if (solution[index] > eps && this.solutionVariable.get(index).segment == segment && !this.solutionVariable.get(index).isZeroTime()) {
                CastingState state = this.solutionVariable.get(index).state;
                if (state != null && !state.effectsActive(effect)) {
                    boolean hasToBeLeft = (state.getEffects() & result.rightLink) != result.rightLink || ((state.getEffects() & result.leftLink) == result.leftLink && (state.getEffects() & result.fullLeftLink) != result.leftLink);
                    boolean hasToBeRight = (state.getEffects() & result.leftLink) != result.leftLink || ((state.getEffects() & result.rightLink) == result.rightLink && (state.getEffects() & result.fullRightLink) != result.rightLink);
                    if (hasToBeLeft) {
                        result.minTime += solution[index];
                        result.leftPaddding += solution[index];
                    }
                    if (hasToBeRight) {
                        result.maxTime -= solution[index];
                        result.rightPadding += solution[index];
                    }
                }
            }
        }
        if (segment < this.segmentList.size() - 1 && (hexMask[segment + 1] & effect) == effect) {
            result.loose = false;
            result.minTime = result.maxTime = this.segmentList.get(segment).getTimeEnd() - effectInSegment;
        }
        return result;
    }

    private boolean validateManaGemEffect() {
        final double eps = 0.00001;
        double[] trinketCount = getSegmentCooldownCount(StandardEffect.ManaGemEffect, VariableType.None);
        double[] manaGem = getSegmentCooldownCount(0, VariableType.ManaGem);

        // make sure gem is activated together with SCB
        for (int i = 0; i < this.segmentList.size(); i++) {
            if (trinketCount[i] > 0.0 && (i == 0 || trinketCount[i - 1] == 0.0)) {
                if (manaGem[i] < 1.0 - eps) {
                    // either pop gem
                    SolverLP gem = lp.clone();
                    if (gem.log != null) gem.log.append("Pop gem with gem effect at " + i).append('\n');
                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                        if (this.solutionVariable.get(index).type == VariableType.ManaGem && this.solutionVariable.get(index).segment == i) {
                            gem.setColumnLowerBound(index, 1.0);
                        }
                    }
                    gem.forceRecalculation(true);
                    heapPush(gem);
                    // or force activation before this segment
                    if (i > 0) {
                        gem = lp.clone();
                        if (gem.log != null) gem.log.append("Force gem effect before " + i).append('\n');
                        int row = gem.addConstraint();
                        gem.setConstraintRHS(row, this.segmentList.get(i - 1).duration);
                        gem.setConstraintLHS(row, 0.1);
                        setCooldownElements(gem, row, StandardEffect.ManaGemEffect, i - 1, 1.0);
                        gem.forceRecalculation(true);
                        heapPush(gem);
                    }
                    // or remove effect
                    if (lp.log != null) lp.log.append("Remove gem effect at " + i).append('\n');
                    for (int index = segmentColumn[i]; index < segmentColumn[i + 1]; index++) {
                        CastingState state = this.solutionVariable.get(index).state;
                        if (state != null && state.effectsActive(StandardEffect.ManaGemEffect) && this.solutionVariable.get(index).segment == i) {
                            lp.eraseColumn(index);
                        }
                    }
                    heapPush(lp);
                    return false;
                }
            }
        }

        for (int seg = 0; seg < this.segmentList.size(); seg++) {
            if (manaGem[seg] > 0 && trinketCount[seg] > 0) {
                // make accurate prediction about mana burn if you can, assume all non-effect comes in front (that consumes mana)
                double manaSpentBeforeGem = 0;
                double manaChangeBeforeGem = 0;
                for (int index = 0; index < this.solutionVariable.size(); index++) {
                    if (this.solutionVariable.get(index).segment == seg) {
                        CastingState state = this.solutionVariable.get(index).state;
                        if (this.solutionVariable.get(index).type != VariableType.ManaGem && (state == null || !state.effectsActive(StandardEffect.ManaGemEffect))) {
                            double mps = lp.get(rowManaRegen, index);
                            manaChangeBeforeGem += mps * solution[index];
                            if (mps > 0) {
                                manaSpentBeforeGem += mps * solution[index];
                            }
                        }
                    }
                }

                // mana overflow guards
                if (this.baseStats.getMana() - manaList[seg] + manaSpentBeforeGem < this.manaGemValue * (1 + this.baseStats.getBonusManaGem()) - 0.001) {
                    SolverLP nogem = lp.clone();
                    // restrict SCB/overflow
                    if (lp.log != null) lp.log.append("Restrict overflow gem effect at " + seg).append('\n');
                    int row = lp.addConstraint();
                    lp.setConstraintRHS(row, Double.POSITIVE_INFINITY);
                    lp.setConstraintLHS(row, 0.1 * (this.manaGemValue * (1 + this.baseStats.getBonusManaGem()) - this.baseStats.getMana() + this.startingMana));
                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                        CastingState state = this.solutionVariable.get(index).state;
                        int s = this.solutionVariable.get(index).segment;
                        if (s < seg) {
                            double mps = lp.get(rowManaRegen, index);
                            lp.setConstraintElement(row, index, 0.1 * mps);
                        } else if (s == seg && this.solutionVariable.get(index).type != VariableType.ManaGem && (state == null || !state.effectsActive(StandardEffect.ManaGemEffect))) {
                            double mps = lp.get(rowManaRegen, index);
                            if (mps > 0) lp.setConstraintElement(row, index, 0.1 * mps);
                        }
                    }
                    lp.forceRecalculation(true);
                    heapPush(lp);
                    // no gem/trinket at 0
                    if (nogem.log != null) nogem.log.append("No gem at " + seg).append('\n');
                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                        if (this.solutionVariable.get(index).type == VariableType.ManaGem && this.solutionVariable.get(index).segment == seg) {
                            nogem.eraseColumn(index);
                        }
                    }
                    heapPush(nogem);
                    return false;
                }

                // mana underflow guards, can only use if mana gem effect activation is fixed
                ActivationConstraints activation = getActivationConstraints(StandardEffect.ManaGemEffect, seg, trinketCount[seg]);
                if (manaList[seg] - manaChangeBeforeGem < -0.001 && !activation.loose) {
                    // either make sure gem is popped soon enough
                    SolverLP branchlp = lp.clone();
                    if (branchlp.log != null) branchlp.log.append("Restrict underflow gem effect at " + seg).append('\n');
                    int row = branchlp.addConstraint();
                    // mana spending <= start mana
                    branchlp.setConstraintRHS(row, 0.1 * this.startingMana);
                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                        CastingState state = this.solutionVariable.get(index).state;
                        int s = this.solutionVariable.get(index).segment;
                        if (s < seg) {
                            double mps = branchlp.get(rowManaRegen, index);
                            branchlp.setConstraintElement(row, index, 0.1 * mps);
                        } else if (s == seg && this.solutionVariable.get(index).type != VariableType.ManaGem && (state == null || !state.effectsActive(StandardEffect.ManaGemEffect))) {
                            double mps = branchlp.get(rowManaRegen, index);
                            branchlp.setConstraintElement(row, index, 0.1 * mps);
                        }
                    }
                    branchlp.forceRecalculation(true);
                    heapPush(branchlp);
                    // or make it loose
                    branchlp = lp.clone();
                    if (branchlp.log != null) branchlp.log.append("Remove gem effect at " + (seg + 1)).append('\n');
                    disableCooldown(branchlp, StandardEffect.ManaGemEffect, seg + 1);
                    branchlp.forceRecalculation(true);
                    heapPush(branchlp);
                    // or there is no activation here
                    if (lp.log != null) lp.log.append("No gem at " + seg).append('\n');
                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                        if (this.solutionVariable.get(index).type == VariableType.ManaGem && this.solutionVariable.get(index).segment == seg) {
                            lp.eraseColumn(index);
                        }
                    }
                    heapPush(lp);
                    return false;
                }
            }
        }

        return true;
    }

    private boolean validateEffectPotion() {
        final double eps = 0.00001;
        double[] potionOfSpeedCount = getSegmentCooldownCount(StandardEffect.PotionOfSpeed, VariableType.None);
        double[] potionOfWildMagicCount = getSegmentCooldownCount(StandardEffect.PotionOfWildMagic, VariableType.None);
        double[][] segCount = new double[][]{potionOfSpeedCount, potionOfWildMagicCount};
        int[] effect = new int[]{(int) StandardEffect.PotionOfSpeed, (int) StandardEffect.PotionOfWildMagic};
        double[] total = new double[2];

        for (int i = 0; i < total.length; i++) {
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                total[i] += segCount[i][seg];
            }
        }

        for (int i = 0; i < total.length; i++) {
            if (total[i] > eps) {
                for (int j = i + 1; j < total.length; j++) {
                    if (total[j] > eps) {
                        // can't have both
                        SolverLP branchlp = lp.clone();
                        if (branchlp.log != null) branchlp.log.append("Disable " + effect[i]).append('\n');
                        disableCooldown(branchlp, effect[i], 0, this.segmentList.size() - 1);
                        heapPush(branchlp);
                        if (lp.log != null) lp.log.append("Disable " + effect[j]).append('\n');
                        disableCooldown(lp, effect[j], 0, this.segmentList.size() - 1);
                        heapPush(lp);
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean validateHastedEvocation() {
        final double eps = 0.00001;
        double[] evoCount = getSegmentCooldownCount(StandardEffect.Evocation, VariableType.Evocation);
        double[] evoIVCount = getSegmentCooldownCount(StandardEffect.Evocation, VariableType.EvocationIV);
        double[] evoHeroCount = getSegmentCooldownCount(StandardEffect.Evocation, VariableType.EvocationHero);
        double[] evoIVHeroCount = getSegmentCooldownCount(StandardEffect.Evocation, VariableType.EvocationIVHero);
        double[][] segCount = new double[][]{evoCount, evoIVCount, evoHeroCount, evoIVHeroCount};
        VariableType[] speedType = new VariableType[]{VariableType.Evocation, VariableType.EvocationIV, VariableType.EvocationHero, VariableType.EvocationIVHero};

        // can't have different speeds of evocation in same activation
        for (int seg = 0; seg < this.segmentList.size(); seg++) {
            int segmin = resolution[seg].minSegment;
            int segmax = resolution[seg].maxSegment;
            for (int speed = 0; speed < 3; speed++) {
                double inseg = segCount[speed][seg];
                if (inseg > eps) {
                    boolean valid = true;
                    // verify that in cooldown distance there is no different speed
                    for (int outseg = 0; outseg < this.segmentList.size(); outseg++) {
                        if (inCooldownDistance(segmin, segmax, outseg, this.evocationDuration, this.evocationCooldown)) {
                            for (int speed2 = speed + 1; speed2 < 4; speed2++) {
                                if (segCount[speed2][outseg] > eps) {
                                    valid = false;
                                    break;
                                }
                            }
                        }
                    }
                    if (!valid) {
                        // branch on whether cooldown is used in this segment
                        SolverLP cooldownUsed = lp.clone();
                        // cooldown used
                        if (cooldownUsed.log != null) cooldownUsed.log.append(speedType[speed] + " at " + segmin + "-" + segmax).append('\n');
                        for (int index = 0; index < segmentColumn[0]; index++) { // fix if variable ordering changes
                            int outseg = this.solutionVariable.get(index).segment;
                            CastingState state = this.solutionVariable.get(index).state;
                            if (inCooldownDistance(segmin, segmax, outseg, this.evocationDuration, this.evocationCooldown)) {
                                VariableType okSpeed = speedType[speed];
                                if (state != null && state.evocation && this.solutionVariable.get(index).type != okSpeed) {
                                    cooldownUsed.eraseColumn(index);
                                }
                            }
                        }
                        int row = addConstraint(cooldownUsed, StandardEffect.Evocation, speedType[speed], segmin, segmax);
                        cooldownUsed.setConstraintRHS(row, this.evocationDuration);
                        cooldownUsed.setConstraintLHS(row, 0.1);
                        cooldownUsed.forceRecalculation(true);
                        heapPush(cooldownUsed);
                        // cooldown not used
                        if (lp.log != null) lp.log.append("Disable " + speedType[speed] + " at " + segmin + "-" + segmax).append('\n');
                        disableCooldown(lp, StandardEffect.Evocation, speedType[speed], segmin, segmax);
                        heapPush(lp);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean validateEvocation() {
        final double eps = 0.00001;
        double[] evoCount = getSegmentCooldownCount(StandardEffect.Evocation, VariableType.None);
        double[][] evoHastedCount = new double[3][];
        double[][] evoHastedActivation = new double[3][];
        boolean[][] cooldownPresent = new boolean[3][];
        int[] hastedCooldown = new int[]{(int) StandardEffect.IcyVeins, (int) StandardEffect.Heroism, (int) (StandardEffect.IcyVeins | StandardEffect.Heroism)};
        VariableType[] hastedCooldownType = new VariableType[]{VariableType.EvocationIV, VariableType.EvocationHero, VariableType.EvocationIVHero};
        if (this.calculationOptions.getEnableHastedEvocation()) {
            for (int i = 0; i < hastedCooldown.length; i++) {
                evoHastedCount[i] = getSegmentCooldownCount(StandardEffect.Evocation, hastedCooldownType[i]);
                evoHastedActivation[i] = getSegmentCooldownCount(StandardEffect.Evocation | hastedCooldown[i], hastedCooldownType[i]);
            }
            double[] ivCount = getSegmentCooldownCount(StandardEffect.IcyVeins, VariableType.None);
            double[] heroCount = getSegmentCooldownCount(StandardEffect.Heroism, VariableType.None);
            cooldownPresent[0] = new boolean[this.segmentList.size()];
            cooldownPresent[1] = new boolean[this.segmentList.size()];
            cooldownPresent[2] = new boolean[this.segmentList.size()];
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                cooldownPresent[0][seg] = ivCount[seg] > eps;
                cooldownPresent[1][seg] = heroCount[seg] > eps;
                cooldownPresent[2][seg] = ivCount[seg] + heroCount[seg] > eps;
            }
        }

        if (segmentMana) {
            for (int seg = 0; seg < this.segmentList.size(); seg++) {
                if (evoCount[seg] > eps && (seg + 1 < this.segmentList.size() && evoCount[seg + 1] > eps)) {
                    // if we're using mana segmentation then there can be nothing else in seg.1 or seg+1.0

                    boolean validCrossing = true;
                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                        if (this.solutionVariable.get(index).segment == seg && this.solutionVariable.get(index).manaSegment == 1) {
                            CastingState state = this.solutionVariable.get(index).state;
                            if (state == null || !state.evocation) {
                                if (solution[index] > eps) {
                                    validCrossing = false;
                                    break;
                                }
                            }
                        } else if (this.solutionVariable.get(index).segment == seg + 1 && this.solutionVariable.get(index).manaSegment == 0) {
                            if (solution[index] > eps) {
                                validCrossing = false;
                                break;
                            }
                        }
                    }
                    if (!validCrossing) {
                        // either there is evo crossing segment and there is nothing else in between
                        // or evo is not crossing here, which means either no evo at seg or no evo at seg+1
                        SolverLP branchlp = lp.clone();
                        if (branchlp.log != null) branchlp.log.append("Remove evocation at " + seg).append('\n');
                        disableCooldown(branchlp, StandardEffect.Evocation, seg);
                        branchlp.forceRecalculation(true);
                        heapPush(branchlp);

                        branchlp = lp.clone();
                        if (branchlp.log != null) branchlp.log.append("Remove evocation at " + (seg + 1)).append('\n');
                        disableCooldown(branchlp, StandardEffect.Evocation, seg + 1);
                        branchlp.forceRecalculation(true);
                        heapPush(branchlp);

                        if (lp.log != null) lp.log.append("Fix evocation crossing at " + seg + "-" + (seg + 1)).append('\n');
                        for (int index = 0; index < this.solutionVariable.size(); index++) {
                            if (this.solutionVariable.get(index).segment == seg && this.solutionVariable.get(index).manaSegment == 1) {
                                CastingState state = this.solutionVariable.get(index).state;
                                if (state == null || !state.evocation) {
                                    lp.eraseColumn(index);
                                }
                            } else if (this.solutionVariable.get(index).segment == seg + 1 && this.solutionVariable.get(index).manaSegment == 0) {
                                lp.eraseColumn(index);
                            }
                        }
                        lp.forceRecalculation(true);
                        heapPush(lp);
                    }
                }
            }
        }

        for (int seg = 0; seg < this.segmentList.size(); seg++) {
            if (evoCount[seg] > eps && (seg + 1 < this.segmentList.size() && evoCount[seg + 1] > eps) && (seg == 0 || evoCount[seg - 1] < eps)) {
                // this is the activation of evocation and it is fixed, so it has to come at the end
                // validate against underflow

                // make accurate prediction about mana burn if you can, assume all non-effect comes in front (that consumes mana)
                double manaChangeBefore = 0;
                for (int index = 0; index < this.solutionVariable.size(); index++) {
                    if (this.solutionVariable.get(index).segment == seg) {
                        CastingState state = this.solutionVariable.get(index).state;
                        if (state == null || !state.evocation) {
                            double mps = lp.get(rowManaRegen, index);
                            manaChangeBefore += mps * solution[index];
                        }
                    }
                }

                if (manaList[seg] - manaChangeBefore < -0.001) {
                    // either make sure evo is popped soon enough
                    SolverLP branchlp = lp.clone();
                    if (branchlp.log != null) branchlp.log.append("Restrict underflow evocation at " + seg).append('\n');
                    int row = branchlp.addConstraint();
                    // mana spending <= start mana
                    branchlp.setConstraintRHS(row, 0.1 * this.startingMana);
                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                        int s = this.solutionVariable.get(index).segment;
                        CastingState state = this.solutionVariable.get(index).state;
                        if (s < seg) {
                            double mps = branchlp.get(rowManaRegen, index);
                            branchlp.setConstraintElement(row, index, 0.1 * mps);
                        } else if (s == seg && (state == null || !state.evocation)) {
                            double mps = branchlp.get(rowManaRegen, index);
                            branchlp.setConstraintElement(row, index, 0.1 * mps);
                        }
                    }
                    branchlp.forceRecalculation(true);
                    heapPush(branchlp);
                    // or make it loose
                    branchlp = lp.clone();
                    if (branchlp.log != null) branchlp.log.append("Remove evocation at " + (seg + 1)).append('\n');
                    disableCooldown(branchlp, StandardEffect.Evocation, seg + 1);
                    branchlp.forceRecalculation(true);
                    heapPush(branchlp);
                    // or there is no activation here
                    // this means that either there is evo before or it isn't here
                    branchlp = lp.clone();
                    if (branchlp.log != null) branchlp.log.append("Evocation starts before " + seg).append('\n');
                    row = branchlp.addConstraint();
                    setCooldownElements(branchlp, row, StandardEffect.Evocation, seg - 1, 1.0);
                    branchlp.setConstraintRHS(row, this.segmentList.get(seg - 1).duration);
                    branchlp.setConstraintLHS(row, 0.1);
                    branchlp.forceRecalculation(true);
                    heapPush(branchlp);
                    if (lp.log != null) lp.log.append("No evocation at " + seg).append('\n');
                    disableCooldown(lp, StandardEffect.Evocation, seg);
                    heapPush(lp);
                    return false;
                }
            }

            if (evoCount[seg] > eps && (seg + 1 >= this.segmentList.size() || evoCount[seg + 1] < eps) && (seg > 0 && evoCount[seg - 1] > eps)) {
                double overflow = 0;
                double evoRegen = 0;
                for (int index = 0; index < this.solutionVariable.size(); index++) {
                    if (this.solutionVariable.get(index).segment == seg) {
                        CastingState state = this.solutionVariable.get(index).state;
                        if (this.solutionVariable.get(index).type == VariableType.ManaOverflow) {
                            overflow += solution[index];
                        } else if (state != null && state.evocation) {
                            evoRegen -= solution[index] * lp.get(rowManaRegen, index);
                        }
                    }
                }

                // this is the last segment of evocation activation that is fixed, has to be activated in previous segment
                // guard against overflow
                if (this.baseStats.getMana() - manaList[seg] + overflow < evoRegen - 0.001) {
                    SolverLP branchlp = lp.clone();
                    // restrict overflow
                    if (branchlp.log != null) branchlp.log.append("Restrict overflow evocation at " + seg).append('\n');
                    int row = branchlp.addConstraint();
                    branchlp.setConstraintRHS(row, Double.POSITIVE_INFINITY);
                    branchlp.setConstraintLHS(row, 0.1 * (this.startingMana - this.baseStats.getMana()));
                    for (int index = 0; index < this.solutionVariable.size(); index++) {
                        int s = this.solutionVariable.get(index).segment;
                        CastingState state = this.solutionVariable.get(index).state;
                        if (s < seg || (s == seg && (this.solutionVariable.get(index).type == VariableType.ManaOverflow || (state != null && state.evocation)))) {
                            double mps = branchlp.get(rowManaRegen, index);
                            branchlp.setConstraintElement(row, index, 0.1 * mps);
                        }
                    }
                    branchlp.forceRecalculation(true);
                    heapPush(branchlp);
                    // otherwise it has to be loose
                    branchlp = lp.clone();
                    if (branchlp.log != null) branchlp.log.append("No evocation before " + seg).append('\n');
                    disableCooldown(branchlp, StandardEffect.Evocation, seg - 1);
                    heapPush(branchlp);
                    // or no evocation here
                    if (lp.log != null) lp.log.append("No evocation at " + seg).append('\n');
                    disableCooldown(lp, StandardEffect.Evocation, seg);
                    heapPush(lp);
                    return false;
                }
            }

            if (this.calculationOptions.getEnableHastedEvocation()) {
                for (int i = 0; i < hastedCooldown.length; i++) {
                    if (evoHastedCount[i][seg] > eps && evoHastedCount[i][seg] > evoHastedActivation[i][seg] + eps) {
                        // has to happen after IV, so make sure we don't run oom
                        // this only has to hold if there is some evocation left when IV is out
                        // if evocation happens during IV it can be placed anywhere within IV
                        double manaChangeBefore = 0;
                        for (int index = 0; index < this.solutionVariable.size(); index++) {
                            if (this.solutionVariable.get(index).segment == seg) {
                                CastingState state = this.solutionVariable.get(index).state;
                                if (state == null || !state.evocation) {
                                    double mps = lp.get(rowManaRegen, index);
                                    if ((state != null && state.effectsActive(hastedCooldown[i])) || mps < 0) // count what has to be before and what is benefitial to be before
                                    {
                                        manaChangeBefore += mps * solution[index];
                                    }
                                }
                            }
                        }

                        if (manaList[seg] - manaChangeBefore < -0.001) {
                            // either make sure evo is popped soon enough
                            SolverLP branchlp = lp.clone();
                            if (branchlp.log != null) branchlp.log.append("Restrict underflow " + hastedCooldownType[i] + " at " + seg).append('\n');
                            int row = branchlp.addConstraint();
                            // mana spending <= start mana
                            branchlp.setConstraintRHS(row, 0.1 * this.startingMana);
                            for (int index = 0; index < this.solutionVariable.size(); index++) {
                                int s = this.solutionVariable.get(index).segment;
                                CastingState state = this.solutionVariable.get(index).state;
                                if (s < seg) {
                                    double mps = branchlp.get(rowManaRegen, index);
                                    branchlp.setConstraintElement(row, index, 0.1 * mps);
                                } else if (s == seg && (state == null || !state.evocation)) {
                                    double mps = branchlp.get(rowManaRegen, index);
                                    if ((state != null && state.effectsActive(hastedCooldown[i])) || mps < 0) {
                                        branchlp.setConstraintElement(row, index, 0.1 * mps);
                                    }
                                }
                            }
                            branchlp.forceRecalculation(true);
                            heapPush(branchlp);
                            // or there is no evo here without IV
                            if (lp.log != null) lp.log.append("No " + hastedCooldownType[i] + " without cooldown at " + seg).append('\n');
                            for (int index = 0; index < this.solutionVariable.size(); index++) {
                                if (this.solutionVariable.get(index).type == hastedCooldownType[i] && !this.solutionVariable.get(index).state.effectsActive(hastedCooldown[i])) {
                                    if (this.solutionVariable.get(index).segment == seg) lp.eraseColumn(index);
                                }
                            }
                            heapPush(lp);
                            return false;
                        }
                    }

                    if (evoHastedCount[i][seg] > eps && seg + 1 < this.segmentList.size() && cooldownPresent[i][seg + 1]) {
                        // if cooldown happens at next segment then we're fixed in the constraints of
                        // the cooldown, meaning that anything that is not cooldown must happen
                        // in front of evocation
                        double manaChangeBefore = 0;
                        for (int index = 0; index < this.solutionVariable.size(); index++) {
                            if (this.solutionVariable.get(index).segment == seg) {
                                CastingState state = this.solutionVariable.get(index).state;
                                if (state == null || !state.evocation) {
                                    double mps = lp.get(rowManaRegen, index);
                                    if ((state != null && (state.getEffects() & hastedCooldown[i]) == 0) || mps < 0) // count what has to be before and what is benefitial to be before
                                    {
                                        manaChangeBefore += mps * solution[index];
                                    }
                                }
                            }
                        }

                        if (manaList[seg] - manaChangeBefore < -0.001) {
                            // either make sure evo is popped soon enough
                            SolverLP branchlp = lp.clone();
                            if (branchlp.log != null) branchlp.log.append("Restrict underflow " + hastedCooldownType[i] + " with cooldown at " + seg).append('\n');
                            int row = branchlp.addConstraint();
                            // mana spending <= start mana
                            branchlp.setConstraintRHS(row, 0.1 * this.startingMana);
                            for (int index = 0; index < this.solutionVariable.size(); index++) {
                                int s = this.solutionVariable.get(index).segment;
                                CastingState state = this.solutionVariable.get(index).state;
                                if (s < seg) {
                                    double mps = branchlp.get(rowManaRegen, index);
                                    branchlp.setConstraintElement(row, index, 0.1 * mps);
                                } else if (s == seg && (state == null || !state.evocation)) {
                                    double mps = branchlp.get(rowManaRegen, index);
                                    if ((state != null && (state.getEffects() & hastedCooldown[i]) == 0) || mps < 0) {
                                        branchlp.setConstraintElement(row, index, 0.1 * mps);
                                    }
                                }
                            }
                            branchlp.forceRecalculation(true);
                            heapPush(branchlp);
                            // or there is no evo here without IV
                            if (lp.log != null) lp.log.append("Nothing of " + hastedCooldown[i] + " at " + (seg + 1)).append('\n');
                            for (int index = 0; index < this.solutionVariable.size(); index++) {
                                CastingState state = this.solutionVariable.get(index).state;
                                if (state != null && (state.getEffects() & hastedCooldown[i]) != 0) {
                                    if (this.solutionVariable.get(index).segment == seg + 1) lp.eraseColumn(index);
                                }
                            }
                            heapPush(lp);
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }
}
