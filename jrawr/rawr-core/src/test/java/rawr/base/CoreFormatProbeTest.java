package rawr.base;

import rawr.base.character.Buff;
import rawr.base.effects.SpecialEffect;
import rawr.base.effects.Trigger;
import rawr.base.items.Enchant;
import rawr.base.items.ItemSlot;
import rawr.base.stats.Stats;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bit-exact replay of the committed core-format vectors against the dump produced by the legacy
 * {@code Rawr.Base.dll} under Mono ({@code tools/core-format-probe}). The probe calls the original
 * {@code Stats.ToString()}, {@code SpecialEffect.ToString()}, {@code Buff.ToString()} and
 * {@code Enchant.ToString()} and records a throwing line as {@code X throw <Exception>}.
 */
class CoreFormatProbeTest {

    @Test
    void toStringVectorsMatchTheLegacyProbe() throws Exception {
        List<String> vectors = readLines("core-format/vectors.txt");
        List<String> expected = readLines("core-format/expected.txt");
        List<String> actual = new ArrayList<String>();
        replay(vectors, actual);

        for (int i = 0; i < Math.min(expected.size(), actual.size()); i++) {
            if (!expected.get(i).equals(actual.get(i))) {
                assertThat(actual.get(i))
                        .as("first difference at line %d (expected): %s", i + 1, expected.get(i))
                        .isEqualTo(expected.get(i));
            }
        }
        assertThat(actual).hasSize(expected.size());
    }

    private static void replay(List<String> vectors, List<String> out) {
        int index = 0;
        for (String raw : vectors) {
            String text = raw.trim();
            if (text.isEmpty() || text.startsWith("#")) {
                continue;
            }
            String[] parts = text.split("\\|", -1);
            out.add("== " + index + " " + text);
            try {
                switch (parts[0]) {
                    case "S":
                        out.add(parseStats(parts[1]).toString());
                        break;
                    case "E":
                        out.add(makeEffect(parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]).toString());
                        break;
                    case "B":
                        out.add(makeBuff(parts[1], parts.length > 2 ? parts[2] : "").toString());
                        break;
                    case "N":
                        out.add(new Enchant(1234, parts[1], ItemSlot.Head, parseStats(parts.length > 2 ? parts[2] : "")).toString());
                        break;
                    case "SE": {
                        Stats stats = parseStats(parts[1]);
                        stats.addSpecialEffect(makeEffect(parts[2], parts[3], parts[4], parts[5], parts[6], parts[7]));
                        out.add(stats.toString());
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("unknown vector kind: " + parts[0]);
                }
            } catch (Exception e) {
                // The probe records the C# exception type; ArgumentException is the C# counterpart.
                String name = e instanceof IllegalArgumentException ? "ArgumentException" : e.getClass().getSimpleName();
                out.add("X throw " + name);
            }
            index++;
        }
    }

    private static Stats parseStats(String spec) {
        Stats stats = new Stats();
        if (spec == null || spec.isEmpty()) {
            return stats;
        }
        for (String pair : spec.split(",", -1)) {
            String[] kv = pair.split("=", -1);
            Stats.StatProperty property = Stats.PROPERTY_BY_NAME.get(kv[0]);
            if (property == null) {
                throw new IllegalArgumentException("unknown stat " + kv[0]);
            }
            property.mutator().set(stats, parseFloat(kv[1]));
        }
        return stats;
    }

    private static float parseFloat(String text) {
        if (text.equals("INF")) return Float.POSITIVE_INFINITY;
        if (text.equals("-INF")) return Float.NEGATIVE_INFINITY;
        if (text.equals("NaN")) return Float.NaN;
        return Float.parseFloat(text);
    }

    private static SpecialEffect makeEffect(String trigger, String stats, String duration, String cooldown, String chance, String maxStack) {
        return new SpecialEffect(Trigger.valueOf(trigger), parseStats(stats),
                parseFloat(duration), parseFloat(cooldown), parseFloat(chance), Integer.parseInt(maxStack));
    }

    private static Buff makeBuff(String name, String stats) {
        Buff buff = new Buff();
        buff.name = name;
        buff.stats = parseStats(stats);
        return buff;
    }

    private static List<String> readLines(String resource) throws Exception {
        try (InputStream in = CoreFormatProbeTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as("resource %s", resource).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }
}
