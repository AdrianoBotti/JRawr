package rawr.base.stats;

import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterRace;
import rawr.base.effects.SpecialEffect;
import rawr.base.stats.BaseStats.DruidForm;

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
 * Bit-exact replay of the committed {@code BaseStats} dump produced by the legacy
 * {@code Rawr.Base.dll} under Mono ({@code tools/base-stats-probe}). The probe calls
 * {@code BaseStats.GetBaseStats} for levels 1 and 80, every class/race combination and every Druid
 * form, then writes every non-zero float stat (IEEE-754 hex bits) plus every effect and its nested
 * stats. The expected file's {@code == level|class|race|form} headers are the case list.
 */
class BaseStatsGoldenTest {

    @Test
    void allClassRaceFormCombinationsMatchTheLegacyProbe() throws Exception {
        List<String> expected = readLines("base-stats/expected.txt");
        List<String> actual = new ArrayList<String>();

        for (String line : expected) {
            if (!line.startsWith("== ")) {
                continue;
            }
            actual.add(line);
            String[] parts = line.substring(3).split("\\|", -1);
            Stats stats = BaseStats.getBaseStats(
                    Integer.parseInt(parts[0]),
                    CharacterClass.valueOf(parts[1]),
                    CharacterRace.valueOf(parts[2]),
                    DruidForm.valueOf(parts[3]));
            dumpStats(stats, "", actual);
        }

        assertThat(actual).as("BaseStats dump line count").isNotEmpty();
        for (int i = 0; i < Math.min(expected.size(), actual.size()); i++) {
            if (!expected.get(i).equals(actual.get(i))) {
                assertThat(actual.get(i))
                        .as("first difference at line %d (expected): %s", i + 1, expected.get(i))
                        .isEqualTo(expected.get(i));
            }
        }
        assertThat(actual.size()).isEqualTo(expected.size());
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
        try (InputStream in = BaseStatsGoldenTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as("test resource %s", resource).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }
}
