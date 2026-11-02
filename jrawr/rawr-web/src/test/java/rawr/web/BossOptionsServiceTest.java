package rawr.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossOptions;
import rawr.base.bosses.DoT;
import rawr.base.character.Character;
import rawr.base.character.TargetGroup;
import rawr.base.items.ItemDamageType;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Validation and list-mutation coverage for the boss encounter editor backend. */
class BossOptionsServiceTest {
    private final BossOptionsService service = new BossOptionsService();
    private Character character;

    @BeforeEach
    void setUp() {
        character = new Character();
    }

    @Test
    void addAndUpdateTargetGroupRoundTrip() {
        int index = service.addTargetGroup(character);
        TargetGroup added = character.getBossOptions().targets.get(index);
        assertThat(added.frequency).isEqualTo(60f);
        assertThat(added.duration).isEqualTo(5000f);
        assertThat(added.chance).isEqualTo(1f);
        assertThat(added.numTargs).isEqualTo(2f);
        assertThat(added.nearBoss).isFalse();

        service.updateTargetGroup(character, index, Map.of(
                "frequency", "30",
                "duration", "15000",
                "chance", "75",
                "numTargs", "3",
                "nearBoss", "on"));

        TargetGroup updated = character.getBossOptions().targets.get(index);
        assertThat(updated.frequency).isEqualTo(30f);
        assertThat(updated.duration).isEqualTo(15000f);
        assertThat(updated.chance).isEqualTo(0.75f);
        assertThat(updated.numTargs).isEqualTo(3f);
        assertThat(updated.nearBoss).isTrue();
    }

    @Test
    void addAndUpdateDotRoundTrip() {
        int index = service.addAttack(character, true);
        assertThat(character.getBossOptions().getAttacks().get(index)).isInstanceOf(DoT.class);

        service.updateAttack(character, index, attackParams("Probe DoT", "Shadow", "AT_AOE"));

        Attack updated = character.getBossOptions().getAttacks().get(index);
        assertThat(updated).isInstanceOf(DoT.class);
        assertThat(updated.name).isEqualTo("Probe DoT");
        assertThat(updated.damageType).isEqualTo(ItemDamageType.Shadow);
        assertThat(updated.attackType).isEqualTo(AttackTypes.AT_AOE);
        assertThat(updated.getDamagePerHit()).isEqualTo(1234f);
        assertThat(updated.damageIsPerc).isTrue();
        assertThat(updated.maxNumTargets).isEqualTo(3f);
        assertThat(updated.attackSpeed).isEqualTo(2.5f);
        assertThat(updated.useParryHaste).isTrue();
        assertThat(updated.isDualWielding).isTrue();
        assertThat(updated.isTheDefaultMelee).isTrue();
        assertThat(updated.interruptable).isTrue();
        assertThat(updated.missable).isTrue();
        assertThat(updated.dodgable).isTrue();
        assertThat(updated.parryable).isTrue();
        assertThat(updated.blockable).isTrue();
        assertThat(updated.ignoresMTank).isTrue();
        assertThat(updated.ignoresOTank).isTrue();
        assertThat(updated.ignoresTTank).isTrue();
        assertThat(updated.ignoresHealers).isTrue();
        assertThat(updated.ignoresMeleeDps).isTrue();
        assertThat(updated.ignoresRangedDps).isTrue();

        DoT dot = (DoT) updated;
        assertThat(dot.damagePerTick).isEqualTo(500f);
        assertThat(dot.numTicks).isEqualTo(4f);
        assertThat(dot.tickInterval).isEqualTo(3f);
        assertThat(dot.getDuration()).isEqualTo(12f);
    }

    @Test
    void updateAttackReplacesTheElementInsteadOfMutatingTheSharedPresetAttack() {
        BossOptions canonical = new BossOptions();
        Attack canonicalAttack = new Attack();
        canonicalAttack.name = "Canonical";
        canonicalAttack.setDamagePerHit(100f);
        canonical.getAttacks().add(canonicalAttack);

        BossOptions copied = new BossOptions();
        copied.cloneThis(canonical);
        character.setBossOptions(copied);
        assertThat(copied.getAttacks().get(0)).isSameAs(canonicalAttack);

        service.updateAttack(character, 0, attackParams("Edited", "Fire", "AT_RANGED"));

        assertThat(copied.getAttacks().get(0)).isNotSameAs(canonicalAttack);
        assertThat(copied.getAttacks().get(0).name).isEqualTo("Edited");
        assertThat(canonicalAttack.name).isEqualTo("Canonical");
        assertThat(canonicalAttack.getDamagePerHit()).isEqualTo(100f);
    }

    @Test
    void deleteAndIndexChecks() {
        service.addTargetGroup(character);
        service.addAttack(character, false);
        service.deleteTargetGroup(character, 0);
        service.deleteAttack(character, 0);

        assertThat(character.getBossOptions().targets).isEmpty();
        assertThat(character.getBossOptions().getAttacks()).isEmpty();
        assertThatThrownBy(() -> service.deleteAttack(character, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no longer exists");
    }

    @Test
    void validationRejectsUnknownEnumsAndOutOfRangeValues() {
        service.addAttack(character, false);
        service.addTargetGroup(character);

        assertThatThrownBy(() -> service.updateAttack(character, 0,
                Map.of("damageType", "Bogus")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Damage type");
        assertThatThrownBy(() -> service.updateAttack(character, 0,
                Map.of("attackSpeed", "-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Attack speed");
        assertThatThrownBy(() -> service.updateTargetGroup(character, 0,
                Map.of("chance", "101")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chance");
        assertThat(character.getBossOptions().getAttacks().get(0).name).isEqualTo("New Attack");
    }

    @Test
    void supportsFallsBackToAllSectionsForUnknownModels() {
        assertThat(service.supports("Healadin"))
                .containsEntry("Timers", true)
                .containsEntry("Level", false)
                .containsEntry("TargetGroups", false)
                .containsEntry("Attacks", false);
        assertThat(service.supports("Mage"))
                .containsEntry("Level", true)
                .containsEntry("TargetGroups", true)
                .containsEntry("Attacks", true);
        assertThat(service.supports("NotAModel").values()).allMatch(Boolean::booleanValue);
        assertThat(service.supports(null).values()).allMatch(Boolean::booleanValue);
    }

    private static Map<String, String> attackParams(String name, String damageType,
            String attackType) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("name", name);
        params.put("damageType", damageType);
        params.put("attackType", attackType);
        params.put("damagePerHit", "1234");
        params.put("damageIsPerc", "on");
        params.put("maxNumTargets", "3");
        params.put("attackSpeed", "2.5");
        params.put("useParryHaste", "on");
        params.put("isDualWielding", "on");
        params.put("isTheDefaultMelee", "on");
        params.put("interruptable", "on");
        params.put("missable", "on");
        params.put("dodgable", "on");
        params.put("parryable", "on");
        params.put("blockable", "on");
        params.put("ignoresMTank", "on");
        params.put("ignoresOTank", "on");
        params.put("ignoresTTank", "on");
        params.put("ignoresHealers", "on");
        params.put("ignoresMeleeDps", "on");
        params.put("ignoresRangedDps", "on");
        params.put("damagePerTick", "500");
        params.put("numTicks", "4");
        params.put("tickInterval", "3");
        return params;
    }
}
