package rawr.base.effects;

import rawr.base.effects.SpecialEffect;
import rawr.base.effects.SpecialEffects;
import rawr.base.stats.Stats;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bit-exact replay of the committed SpecialEffects vectors against the dump produced by the legacy
 * {@code Rawr.Base.dll} under Mono ({@code tools/special-effects-probe}). The probe calls
 * {@code ProcessMetaGem}/{@code ProcessUseLine}/{@code ProcessEquipLine} and writes every non-zero
 * float stat (IEEE-754 hex bits) plus every effect (trigger/duration/cooldown/chance/maxStack and
 * nested stats). A throwing line is recorded as {@code X throw} and its partial stats are still
 * dumped, so exception behavior is locked too.
 */
class SpecialEffectsTest {

    @Test
    void vectorsMatchTheLegacyProbe() throws Exception {
        List<String> vectors = readLines("special-effects/vectors.txt");
        List<String> expected = readLines("special-effects/expected.txt");
        List<String> actual = new ArrayList<String>();
        replay(vectors, actual);

        for (int i = 0; i < Math.min(expected.size(), actual.size()); i++) {
            if (!expected.get(i).equals(actual.get(i))) {
                assertThat(actual.get(i))
                        .as("first difference at line %d (expected): %s", i + 1, expected.get(i))
                        .isEqualTo(expected.get(i));
            }
        }
        assertThat(actual.size()).as("SpecialEffects dump line count").isEqualTo(expected.size());
    }

    private static void replay(List<String> vectors, List<String> out) {
        int index = 0;
        for (String raw : vectors) {
            String text = raw.trim();
            if (text.isEmpty() || text.startsWith("#")) {
                continue;
            }
            String[] parts = text.split("\\|", -1);
            String kind = parts[0];
            Stats stats = new Stats();
            out.add("== " + index + " " + text);
            try {
                switch (kind) {
                    case "M" -> SpecialEffects.processMetaGem(join(parts, 1), stats, false);
                    case "U" -> SpecialEffects.processUseLine(join(parts, 3), stats,
                            "1".equals(parts[1]), Integer.parseInt(parts[2]));
                    case "E" -> SpecialEffects.processEquipLine(join(parts, 4), stats,
                            "1".equals(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                    default -> throw new IllegalArgumentException("unknown vector kind: " + kind);
                }
            } catch (RuntimeException e) {
                out.add("X throw");
            }
            dumpStats(stats, "", out);
            index++;
        }
    }

    private static String join(String[] parts, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < parts.length; i++) {
            if (i > start) {
                sb.append('|');
            }
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    private static List<Method> floatGetters;

    private static synchronized List<Method> floatGetters() {
        if (floatGetters == null) {
            List<Method> getters = new ArrayList<Method>();
            for (Method method : Stats.class.getMethods()) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                if (method.getReturnType() != float.class) {
                    continue;
                }
                if (!method.getName().startsWith("get") || method.getParameterCount() != 0) {
                    continue;
                }
                getters.add(method);
            }
            getters.sort(Comparator.comparing(method -> method.getName().substring(3)));
            floatGetters = getters;
        }
        return floatGetters;
    }

    private static void dumpStats(Stats stats, String prefix, List<String> out) {
        try {
            for (Method getter : floatGetters()) {
                float value = (Float) getter.invoke(stats);
                if (Float.floatToIntBits(value) != 0) {
                    out.add("S " + prefix + getter.getName().substring(3) + " " + hex(value));
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        if (stats.getRawSpecialEffectData() == null) {
            return;
        }
        for (int i = 0; i < stats.getRawSpecialEffectDataSize(); i++) {
            SpecialEffect effect = stats.getRawSpecialEffectData()[i];
            if (effect == null) {
                continue;
            }
            String path = prefix + i + ".";
            out.add("E " + path
                    + "trigger=" + effect.getTrigger()
                    + " duration=" + hex(effect.getDuration())
                    + " cooldown=" + hex(effect.getCooldown())
                    + " chance=" + hex(effect.getChance())
                    + " maxStack=" + effect.getMaxStack());
            dumpStats(effect.getStats(), path, out);
        }
    }

    private static String hex(float value) {
        String hex = Integer.toHexString(Float.floatToIntBits(value)).toUpperCase(Locale.ROOT);
        return "00000000".substring(hex.length()) + hex;
    }

    private static List<String> readLines(String resource) throws Exception {
        try (InputStream in = SpecialEffectsTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as("test resource %s", resource).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }
}
