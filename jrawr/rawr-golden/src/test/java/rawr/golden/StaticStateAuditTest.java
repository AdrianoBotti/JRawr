package rawr.golden;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import rawr.base.calculations.Calculations;
import rawr.base.character.Character;
import rawr.base.items.Enchant;
import rawr.base.items.ItemCache;
import rawr.data.BuffLoader;
import rawr.data.CharacterFixtureLoader;
import rawr.data.EnchantLoader;
import rawr.data.ItemCacheLoader;
import rawr.models.mage.CalculationOptionsMage;
import rawr.models.mage.CalculationsMage;
import rawr.models.mage.CharacterCalculationsMage;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * TD-011 audit: finds mutable static state that a calculation run changes, which is the likely source
 * of the 1-2 ULP cross-fixture interference in a shared JVM. Run on demand; prints a report.
 */
class StaticStateAuditTest {
    private static Path fixturesDir;

    @BeforeAll
    static void setUp() throws Exception {
        Path repoRoot = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        Path gameData = repoRoot.resolve("jrawr/data");
        fixturesDir = repoRoot.resolve("oracle/fixtures/mage");
        ItemCache.clear();
        Enchant.getAllEnchants().clear();
        ItemCacheLoader.load(gameData.resolve("ItemCache.xml"));
        EnchantLoader.load(gameData.resolve("EnchantCache.xml"));
        BuffLoader.load(gameData.resolve("BuffCache.xml"));
        CalculationsMage.register();
        Calculations.loadModel(CalculationsMage.class);
    }

    @Test
    void reportStaticStateChangesBetweenFixtures() throws Exception {
        Path repoRoot = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (String module : new String[] { "rawr-core", "rawr-models", "rawr-data" }) {
            Path dir = repoRoot.resolve("jrawr/" + module + "/build/classes/java/main");
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> files = Files.walk(dir)) {
                files.filter(p -> p.toString().endsWith(".class") && !p.toString().contains("$"))
                        .forEach(p -> {
                            String name = dir.relativize(p).toString().replace('/', '.').replace(".class", "");
                            try {
                                classes.add(Class.forName(name, false, StaticStateAuditTest.class.getClassLoader()));
                            } catch (Throwable ignored) {
                            }
                        });
            }
        }
        System.out.println("[audit] classes=" + classes.size());

        Map<String, Object> s0 = snapshot(classes);
        calculate("arcane_277_raid");
        Map<String, Object> s1 = snapshot(classes);
        calculate("fire_277_raid");
        Map<String, Object> s2 = snapshot(classes);
        report("after arcane_277_raid (S0->S1)", s0, s1);
        report("after fire_277_raid (S1->S2)", s1, s2);
        org.assertj.core.api.Assertions.assertThat(true).isTrue();
    }

    private static void report(String label, Map<String, Object> before, Map<String, Object> after) {
        int changed = 0;
        for (String key : new TreeMap<String, Object>(before).keySet()) {
            Object a = before.get(key);
            Object b = after.get(key);
            if (!String.valueOf(a).equals(String.valueOf(b))) {
                if (changed < 40) {
                    System.out.println("[audit] CHANGED " + label + " :: " + key + "\n    before=" + a + "\n    after =" + b);
                }
                changed++;
            }
        }
        System.out.println("[audit] changed statics " + label + " = " + changed);
    }

    private static void calculate(String fixture) throws Exception {
        Character character = CharacterFixtureLoader.load(fixturesDir.resolve(fixture + ".xml"));
        CalculationOptionsMage options = (CalculationOptionsMage) character.getCalculationOptions();
        if (options == null) {
            options = new CalculationOptionsMage();
            character.setCalculationOptions(options);
        }
        character.recalculateSetBonuses();
        CharacterCalculationsMage ret = (CharacterCalculationsMage) CalculationsMage.getInstance()
                .getCharacterCalculations(character, null, true, true, false);
        if (ret.getOverallPoints() == 0) throw new IllegalStateException("calculation failed for " + fixture);
    }

    private static Map<String, Object> snapshot(List<Class<?>> classes) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (Class<?> type : classes) {
            for (Field field : type.getDeclaredFields()) {
                int mod = field.getModifiers();
                if (!Modifier.isStatic(mod)) continue;
                if (Modifier.isFinal(mod) && !isMutableContainer(field)) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    out.put(type.getName() + "." + field.getName(), summarize(value, 0));
                } catch (Throwable ignored) {
                }
            }
        }
        return out;
    }

    private static boolean isMutableContainer(Field field) {
        Class<?> t = field.getType();
        return Map.class.isAssignableFrom(t) || Collection.class.isAssignableFrom(t) || t.isArray();
    }

    private static Object summarize(Object value, int depth) {
        if (value == null) return null;
        if (value instanceof Map) {
            if (depth > 0) return "Map(size=" + ((Map<?, ?>) value).size() + ")";
            Map<String, Object> map = new TreeMap<String, Object>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                map.put(String.valueOf(e.getKey()), summarize(e.getValue(), depth + 1));
            }
            return map;
        }
        if (value instanceof Collection) {
            if (depth > 1) return "Collection(size=" + ((Collection<?>) value).size() + ")";
            List<Object> list = new ArrayList<Object>();
            for (Object o : (Collection<?>) value) list.add(summarize(o, depth + 1));
            return list;
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            if (depth > 1) return "Array(size=" + length + ")";
            List<Object> list = new ArrayList<Object>();
            for (int i = 0; i < Math.min(length, 8); i++) list.add(summarize(java.lang.reflect.Array.get(value, i), depth + 1));
            if (length > 8) list.add("...");
            return list;
        }
        return String.valueOf(value);
    }
}
