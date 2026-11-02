package rawr.data;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import rawr.base.character.CharacterClass;
import rawr.base.character.SavedTalentSpec;
import rawr.base.talents.TalentsBase;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trips every {@code <*Talents>} string found in the committed oracle fixtures through
 * {@link SavedTalentSpec#talentSpec()} and back, exercising the class dispatch of the ported
 * C# {@code SavedTalentSpec.TalentSpec()}. Glyphs ride inside the same string (the '.' half) and
 * are applied by the talents constructors, exactly like the original.
 */
class SavedTalentSpecRoundTripTest {

    private static final Map<String, CharacterClass> CLASS_BY_ELEMENT = new LinkedHashMap<String, CharacterClass>();

    static {
        CLASS_BY_ELEMENT.put("DeathKnightTalents", CharacterClass.DeathKnight);
        CLASS_BY_ELEMENT.put("WarriorTalents", CharacterClass.Warrior);
        CLASS_BY_ELEMENT.put("PaladinTalents", CharacterClass.Paladin);
        CLASS_BY_ELEMENT.put("ShamanTalents", CharacterClass.Shaman);
        CLASS_BY_ELEMENT.put("HunterTalents", CharacterClass.Hunter);
        CLASS_BY_ELEMENT.put("RogueTalents", CharacterClass.Rogue);
        CLASS_BY_ELEMENT.put("DruidTalents", CharacterClass.Druid);
        CLASS_BY_ELEMENT.put("WarlockTalents", CharacterClass.Warlock);
        CLASS_BY_ELEMENT.put("PriestTalents", CharacterClass.Priest);
        CLASS_BY_ELEMENT.put("MageTalents", CharacterClass.Mage);
    }

    @Test
    void everyFixtureTalentStringRoundTripsThroughTalentSpec() throws Exception {
        Path repoRoot = Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();
        Path fixtures = repoRoot.resolve("oracle/fixtures");
        assertThat(fixtures).as("oracle fixtures directory").isDirectory();

        int checked = 0;
        try (Stream<Path> files = Files.walk(fixtures)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".xml")).toList()) {
                Document doc = XmlLoader.parse(file);
                for (Map.Entry<String, CharacterClass> entry : CLASS_BY_ELEMENT.entrySet()) {
                    NodeList elements = doc.getElementsByTagName(entry.getKey());
                    for (int i = 0; i < elements.getLength(); i++) {
                        Element element = (Element) elements.item(i);
                        String text = element.getTextContent();
                        if (text == null || text.isEmpty()) continue;

                        SavedTalentSpec saved = new SavedTalentSpec();
                        saved.characterClass = entry.getValue();
                        saved.spec = text;
                        TalentsBase talents = saved.talentSpec();

                        assertThat(talents)
                                .as("%s %s", file.getFileName(), entry.getKey())
                                .isNotNull();
                        assertThat(talents.getCharacterClass())
                                .as("%s %s class", file.getFileName(), entry.getKey())
                                .isEqualTo(entry.getValue());
                        assertThat(talents.toString())
                                .as("%s %s", file.getFileName(), entry.getKey())
                                .isEqualTo(text);
                        checked++;
                    }
                }
            }
        }
        assertThat(checked).as("talent strings checked").isGreaterThanOrEqualTo(1140);
    }
}
