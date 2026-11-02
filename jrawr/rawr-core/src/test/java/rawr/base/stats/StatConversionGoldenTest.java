package rawr.base.stats;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bit-exact replay of the committed combat-table dump produced by the legacy {@code Rawr.Base.dll}
 * under Mono ({@code tools/stat-conversion-probe}). Covers attacker level 80, target levels 80-83,
 * dual-wield/white-or-yellow, behind-time 0/0.5/1 and the hit/expertise/crit modifiers.
 */
class StatConversionGoldenTest {

    @Test
    void combatTableMatchesTheLegacyProbe() throws Exception {
        List<String> expected = readLines("stat-conversion/expected.txt");
        List<String> actual = new ArrayList<String>();

        for (String line : expected) {
            if (!line.startsWith("== ")) {
                continue;
            }
            actual.add(line);
            String[] parts = line.substring(3).split("\\|", -1);
            StatConversion.CombatTable table = StatConversion.getCombatTablePlayer_vs_NPC(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    "1".equals(parts[2]),
                    "1".equals(parts[3]),
                    Float.parseFloat(parts[4]),
                    Float.parseFloat(parts[5]),
                    Float.parseFloat(parts[6]),
                    Float.parseFloat(parts[7]));
            append(actual, "Miss", table.miss);
            append(actual, "Dodge", table.dodge);
            append(actual, "Parry", table.parry);
            append(actual, "Glancing", table.glancing);
            append(actual, "Block", table.block);
            append(actual, "Crit", table.crit);
            append(actual, "Hit", table.hit);
            actual.add("B bPlayerTarget " + (table.bPlayerTarget ? 1 : 0));
            actual.add("B bBehind " + (table.bBehind ? 1 : 0));
        }

        assertThat(actual).as("combat-table dump line count").isNotEmpty();
        for (int i = 0; i < Math.min(expected.size(), actual.size()); i++) {
            if (!expected.get(i).equals(actual.get(i))) {
                assertThat(actual.get(i))
                        .as("first difference at line %d (expected): %s", i + 1, expected.get(i))
                        .isEqualTo(expected.get(i));
            }
        }
        assertThat(actual.size()).isEqualTo(expected.size());
    }

    private static void append(List<String> out, String name, float value) {
        out.add("F " + name + " " + hex(value));
    }

    private static String hex(float value) {
        String hex = Integer.toHexString(Float.floatToIntBits(value)).toUpperCase(Locale.ROOT);
        return "00000000".substring(hex.length()) + hex;
    }

    private static List<String> readLines(String resource) throws Exception {
        try (InputStream in = StatConversionGoldenTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as("test resource %s", resource).isNotNull();
            List<String> lines = new ArrayList<String>(
                    Arrays.asList(new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n", -1)));
            if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
                lines.remove(lines.size() - 1);
            }
            return lines;
        }
    }
}
