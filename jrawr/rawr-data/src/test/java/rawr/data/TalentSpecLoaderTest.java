package rawr.data;

import org.junit.jupiter.api.Test;
import rawr.base.character.CharacterClass;
import rawr.base.character.SavedTalentSpec;
import rawr.base.character.SavedTalentSpecList;
import rawr.base.talents.TalentsBase;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class TalentSpecLoaderTest {
    private static final Path REPO_ROOT =
            Paths.get(System.getProperty("jrawr.repoRoot", "../..")).toAbsolutePath().normalize();

    @Test
    void loadsDefaultSavedSpecsAndRebuildsTalentStrings() throws Exception {
        TalentSpecLoader.load(REPO_ROOT.resolve("jrawr/data/Talents.xml"));
        SavedTalentSpecList all = SavedTalentSpec.getAllSpecs();
        assertThat(all).isNotNull().isNotEmpty();
        SavedTalentSpecList mages = SavedTalentSpec.specsFor(CharacterClass.Mage);
        assertThat(mages).isNotEmpty();
        SavedTalentSpec arcane = mages.stream()
                .filter(spec -> "Arcane".equals(spec.name))
                .findFirst()
                .orElseThrow();
        assertThat(arcane.tree1).isEqualTo(57);
        assertThat(arcane.tree2).isEqualTo(3);
        assertThat(arcane.tree3).isEqualTo(11);
        TalentsBase talents = arcane.talentSpec();
        assertThat(talents).isNotNull();
        assertThat(talents.getCharacterClass()).isEqualTo(CharacterClass.Mage);
        int points = 0;
        for (int value : talents.getData()) {
            points += value;
        }
        assertThat(points).isEqualTo(71);
    }
}
