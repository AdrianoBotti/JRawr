package rawr.base;

import rawr.base.bosses.Attack;
import rawr.base.bosses.AttackTypes;
import rawr.base.bosses.BossHandler;
import rawr.base.calculations.Calculations;
import rawr.base.calculations.CalculationsBase;
import rawr.base.calculations.CharacterCalculationsBase;
import rawr.base.calculations.ComparisonCalculationBase;
import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.character.Character;
import rawr.base.items.GemmingTemplate;
import rawr.base.items.Item;
import rawr.base.stats.Stats;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Replays the {@code boss-info.txt} dump produced by the legacy {@code Rawr.Base.dll} under Mono
 * ({@code tools/core-format-probe}): {@code BossHandler.GenInfoString()} and
 * {@code GenInfoString(Character)} for a constructed boss and a stub current model returning fixed
 * stats. The character overload passes {@code Defense * DEFENSE_RATING_AVOIDANCE_MULTIPLIER} (275 *
 * 0.04 = 11) as {@code p_missPerc} although the parameter expects a fraction; the negative melee DPS
 * in the oracle is the original behavior and is reproduced verbatim.
 */
class BossHandlerGenInfoTest {

    @Test
    void genInfoStringsMatchTheLegacyProbe() throws Exception {
        List<String> expected = readLines("core-format/boss-info.txt");

        BossHandler boss = new BossHandler();
        boss.setName("Probe Boss");
        boss.setContent(BossHandler.TierLevels.T10_9);
        boss.setInstance("Probe Instance");
        boss.setVersion(BossHandler.Versions.V_25H);
        boss.setComment("Probe Comment");
        boss.setLevel(83);
        boss.setArmor(10643);
        boss.setHealth(10000000f);
        boss.setBerserkTimer(600);
        boss.setSpeedKillTimer(300);
        boss.setTimeBossIsInvuln(10f);
        boss.setMaxPlayers(25);
        boss.setMinTanks(2);
        boss.setMinHealers(5);

        Attack melee = new Attack();
        melee.name = "Probe Melee";
        melee.isTheDefaultMelee = true;
        melee.setDamagePerHit(20000f);
        melee.attackSpeed = 2f;
        melee.maxNumTargets = 1f;
        melee.attackType = AttackTypes.AT_MELEE;
        boss.getAttacks().add(melee);

        Calculations.registerModel("ProbeModel", ProbeModel.class);
        Character character = new Character();
        character.setCurrentModel("ProbeModel");

        String info = "info=" + escape(boss.genInfoString());
        String charInfo = "charinfo=" + escape(boss.genInfoString(character));

        assertThat(expected).hasSize(2);
        assertThat(info).as("GenInfoString()").isEqualTo(expected.get(0));
        assertThat(charInfo).as("GenInfoString(Character)").isEqualTo(expected.get(1));
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");
    }

    public static class ProbeModel extends CalculationsBase {
        @Override
        public ComparisonCalculationBase createNewComparisonCalculation() {
            return null;
        }

        @Override
        public List<GemmingTemplate> getDefaultGemmingTemplates() {
            return List.of();
        }

        @Override
        public ICalculationOptionBase deserializeDataObject(String xml) {
            return null;
        }

        @Override
        public CharacterCalculationsBase getCharacterCalculations(Character character, Item additionalItem, boolean referenceCalculation, boolean significantChange, boolean needsDisplayCalculations) {
            return null;
        }

        @Override
        public Stats getCharacterStats(Character character, Item additionalItem) {
            Stats stats = new Stats();
            stats.setDamageTakenMultiplier(0.1f);
            stats.setDefense(275f);
            stats.setDodge(0.05f);
            stats.setParry(0.06f);
            stats.setBlock(0.07f);
            stats.setBlockValue(800f);
            return stats;
        }
    }

    private static List<String> readLines(String resource) throws Exception {
        try (InputStream in = BossHandlerGenInfoTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as("resource %s", resource).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }
}
