package rawr.base.character;

import org.junit.jupiter.api.Test;
import rawr.base.talents.DeathKnightTalents;
import rawr.base.talents.DruidTalents;
import rawr.base.talents.HunterTalents;
import rawr.base.talents.MageTalents;
import rawr.base.talents.PaladinTalents;
import rawr.base.talents.PriestTalents;
import rawr.base.talents.RogueTalents;
import rawr.base.talents.ShamanTalents;
import rawr.base.talents.TalentsBase;
import rawr.base.talents.WarlockTalents;
import rawr.base.talents.WarriorTalents;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The optimizer writes its selected talent gene through {@link Character#setCurrentTalents}.
 * That setter must install every class type, not only the four classes the original partial port
 * implemented.
 */
class CharacterTalentsTest {
    @Test
    void setCurrentTalentsHandlesEveryClass() {
        assertSame(new WarriorTalents(), CharacterClass.Warrior);
        assertSame(new PaladinTalents(), CharacterClass.Paladin);
        assertSame(new HunterTalents(), CharacterClass.Hunter);
        assertSame(new RogueTalents(), CharacterClass.Rogue);
        assertSame(new PriestTalents(), CharacterClass.Priest);
        assertSame(new DeathKnightTalents(), CharacterClass.DeathKnight);
        assertSame(new ShamanTalents(), CharacterClass.Shaman);
        assertSame(new MageTalents(), CharacterClass.Mage);
        assertSame(new WarlockTalents(), CharacterClass.Warlock);
        assertSame(new DruidTalents(), CharacterClass.Druid);
    }

    private static void assertSame(TalentsBase talents, CharacterClass characterClass) {
        Character character = new Character();
        character.setCharacterClass(characterClass);
        character.setCurrentTalents(talents);
        assertThat(character.getCurrentTalents()).as(characterClass + " talents").isSameAs(talents);
    }
}
