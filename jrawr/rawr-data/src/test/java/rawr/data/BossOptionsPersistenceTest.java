package rawr.data;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.bosses.BossList;
import rawr.base.bosses.BossOptions;
import rawr.base.bosses.DoT;
import rawr.base.bosses.Impedance;
import rawr.base.character.Character;
import rawr.base.character.TargetGroup;
import rawr.base.items.Enchant;
import rawr.base.items.ItemCache;
import rawr.base.items.ItemDamageType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip check for the &lt;Boss&gt; section of a saved character: every scalar, toggle, target
 * group, impedance, attack and DoT written by {@code Character.save} must come back through
 * {@link CharacterFixtureLoader}.
 */
class BossOptionsPersistenceTest {
    private static final Path REPO_ROOT = Paths.get("../..").toAbsolutePath().normalize();

    @TempDir
    Path tempDir;

    @BeforeAll
    static void loadData() throws Exception {
        Path gameData = REPO_ROOT.resolve("jrawr/data");
        ItemCache.clear();
        Enchant.getAllEnchants().clear();
        ItemCacheLoader.load(gameData.resolve("ItemCache.xml"));
        EnchantLoader.load(gameData.resolve("EnchantCache.xml"));
        BuffLoader.load(gameData.resolve("BuffCache.xml"));
    }

    @Test
    void bossOptionsRoundTripThroughCharacterXml() throws Exception {
        String fixture = Files.readString(REPO_ROOT.resolve(
                "oracle/fixtures/mage/arcane_277_raid.xml"));
        String withBoss = fixture.replaceFirst("(?s)<Boss>.*?</Boss>", """
                <Boss>
                  <Targets><TargetGroup><Frequency>30</Frequency><Duration>15000</Duration><Chance>0.75</Chance><NumTargs>3</NumTargs><NearBoss>true</NearBoss></TargetGroup></Targets>
                  <Moves><Impedance><Frequency>45</Frequency><Duration>2000</Duration><Chance>0.5</Chance><Breakable>false</Breakable></Impedance></Moves>
                  <Stuns /><Fears /><Roots /><Disarms />
                  <Name>Loader Probe</Name>
                  <Content>T8_5</Content>
                  <Instance>Ulduar</Instance>
                  <Version>V_25H</Version>
                  <Comment>round trip</Comment>
                  <Level>83</Level>
                  <Armor>12345</Armor>
                  <BerserkTimer>600</BerserkTimer>
                  <SpeedKillTimer>240</SpeedKillTimer>
                  <Health>1234567</Health>
                  <InBackPerc_Melee>0.25</InBackPerc_Melee>
                  <InBackPerc_Ranged>0.75</InBackPerc_Ranged>
                  <Max_Players>25</Max_Players>
                  <Min_Healers>5</Min_Healers>
                  <Min_Tanks>2</Min_Tanks>
                  <DoTs><DoT><Name>Probe DoT</Name><DamageType>Shadow</DamageType><DamagePerTick>123</DamagePerTick><NumTicks>4</NumTicks><TickInterval>3</TickInterval></DoT></DoTs>
                  <Attacks><Attack><Name>Probe Swing</Name><DamageType>Fire</DamageType><IsTheDefaultMelee>true</IsTheDefaultMelee><MaxNumTargets>1</MaxNumTargets><AttackSpeed>2.5</AttackSpeed><AttackType>AT_RANGED</AttackType><IgnoresHealers>true</IgnoresHealers><DamagePerHit>4321</DamagePerHit></Attack>
                  <Attack xsi:type="DoT"><Name>Probe Attack DoT</Name><DamageType>Shadow</DamageType><AttackSpeed>30</AttackSpeed><AttackType>AT_AOE</AttackType><DamagePerHit>10</DamagePerHit><DamagePerTick>99</DamagePerTick><NumTicks>5</NumTicks><TickInterval>2</TickInterval></Attack></Attacks>
                  <Resist_Physical>0.1</Resist_Physical>
                  <Resist_Frost>0.2</Resist_Frost>
                  <Resist_Fire>0.3</Resist_Fire>
                  <Resist_Nature>0.4</Resist_Nature>
                  <Resist_Arcane>0.5</Resist_Arcane>
                  <Resist_Shadow>0.6</Resist_Shadow>
                  <Resist_Holy>0.7</Resist_Holy>
                  <TimeBossIsInvuln>12.5</TimeBossIsInvuln>
                  <InBack>true</InBack><MultiTargs>true</MultiTargs><StunningTargs>true</StunningTargs>
                  <MovingTargs>true</MovingTargs><FearingTargs>true</FearingTargs>
                  <RootingTargs>true</RootingTargs><DisarmingTargs>true</DisarmingTargs>
                  <DamagingTargs>true</DamagingTargs>
                  <Under35Perc>0.4</Under35Perc><Under20Perc>0.2</Under20Perc>
                  <FilterType>Instance</FilterType><Filter>Ulduar</Filter><BossName>Loader Probe</BossName>
                </Boss>
                """);
        Path saved = tempDir.resolve("boss-round-trip.xml");
        Files.writeString(saved, withBoss);
        BossOptions loaded = CharacterFixtureLoader.load(saved).getBossOptions();

        assertThat(loaded.getName()).isEqualTo("Loader Probe");
        assertThat(loaded.getContent()).isEqualTo(BossHandler.TierLevels.T8_5);
        assertThat(loaded.getInstance()).isEqualTo("Ulduar");
        assertThat(loaded.getVersion()).isEqualTo(BossHandler.Versions.V_25H);
        assertThat(loaded.getComment()).isEqualTo("round trip");
        assertThat(loaded.getLevel()).isEqualTo(83);
        assertThat(loaded.getArmor()).isEqualTo(12345);
        assertThat(loaded.getBerserkTimer()).isEqualTo(600);
        assertThat(loaded.getSpeedKillTimer()).isEqualTo(240);
        assertThat(loaded.getHealth()).isEqualTo(1234567f);
        assertThat(loaded.getInBackPercMelee()).isEqualTo(0.25d);
        assertThat(loaded.getInBackPercRanged()).isEqualTo(0.75d);
        assertThat(loaded.getMaxPlayers()).isEqualTo(25);
        assertThat(loaded.getMinHealers()).isEqualTo(5);
        assertThat(loaded.getMinTanks()).isEqualTo(2);
        assertThat(loaded.getResistPhysical()).isEqualTo(0.1d);
        assertThat(loaded.getResistFrost()).isEqualTo(0.2d);
        assertThat(loaded.getResistFire()).isEqualTo(0.3d);
        assertThat(loaded.getResistNature()).isEqualTo(0.4d);
        assertThat(loaded.getResistArcane()).isEqualTo(0.5d);
        assertThat(loaded.getResistShadow()).isEqualTo(0.6d);
        assertThat(loaded.getResistHoly()).isEqualTo(0.7d);
        assertThat(loaded.getTimeBossIsInvuln()).isEqualTo(12.5f);
        assertThat(loaded.getInBack()).isTrue();
        assertThat(loaded.getMultiTargs()).isTrue();
        assertThat(loaded.getStunningTargs()).isTrue();
        assertThat(loaded.getMovingTargs()).isTrue();
        assertThat(loaded.getFearingTargs()).isTrue();
        assertThat(loaded.getRootingTargs()).isTrue();
        assertThat(loaded.getDisarmingTargs()).isTrue();
        assertThat(loaded.getDamagingTargs()).isTrue();
        assertThat(loaded.getUnder35Perc()).isEqualTo(0.4d);
        assertThat(loaded.getUnder20Perc()).isEqualTo(0.2d);
        assertThat(loaded.getFilterType()).isEqualTo(BossList.FilterType.Instance);
        assertThat(loaded.getFilter()).isEqualTo("Ulduar");
        assertThat(loaded.getBossName()).isEqualTo("Loader Probe");

        assertThat(loaded.targets).hasSize(1);
        TargetGroup target = loaded.targets.get(0);
        assertThat(target.frequency).isEqualTo(30f);
        assertThat(target.duration).isEqualTo(15000f);
        assertThat(target.chance).isEqualTo(0.75f);
        assertThat(target.numTargs).isEqualTo(3f);
        assertThat(target.nearBoss).isTrue();
        assertThat(loaded.moves).hasSize(1);
        assertThat(loaded.moves.get(0).frequency).isEqualTo(45f);
        assertThat(loaded.moves.get(0).duration).isEqualTo(2000f);
        assertThat(loaded.moves.get(0).chance).isEqualTo(0.5f);
        assertThat(loaded.moves.get(0).breakable).isFalse();
        assertThat(loaded.getAttacks()).hasSize(2);
        Attack loadedAttack = loaded.getAttacks().get(0);
        assertThat(loadedAttack.name).isEqualTo("Probe Swing");
        assertThat(loadedAttack.damageType).isEqualTo(ItemDamageType.Fire);
        assertThat(loadedAttack.attackType).isEqualTo(AttackTypes.AT_RANGED);
        assertThat(loadedAttack.attackSpeed).isEqualTo(2.5f);
        assertThat(loadedAttack.getDamagePerHit()).isEqualTo(4321f);
        assertThat(loadedAttack.isTheDefaultMelee).isTrue();
        assertThat(loadedAttack.ignoresHealers).isTrue();
        Attack typedDot = loaded.getAttacks().get(1);
        assertThat(typedDot).isInstanceOf(DoT.class);
        assertThat(typedDot.name).isEqualTo("Probe Attack DoT");
        assertThat(((DoT) typedDot).damagePerTick).isEqualTo(99f);
        assertThat(((DoT) typedDot).numTicks).isEqualTo(5f);
        assertThat(((DoT) typedDot).tickInterval).isEqualTo(2f);
        assertThat(loaded.getDoTs()).hasSize(1);
        DoT loadedDot = loaded.getDoTs().get(0);
        assertThat(loadedDot.name).isEqualTo("Probe DoT");
        assertThat(loadedDot.damagePerTick).isEqualTo(123f);
        assertThat(loadedDot.numTicks).isEqualTo(4f);
        assertThat(loadedDot.tickInterval).isEqualTo(3f);
    }

}
