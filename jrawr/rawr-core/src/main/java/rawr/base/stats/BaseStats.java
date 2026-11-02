// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/BaseStats.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.stats;

import rawr.base.character.Character;
import rawr.base.character.CharacterClass;
import rawr.base.character.CharacterRace;
import rawr.base.character.CharacterSlot;
import rawr.base.effects.SpecialEffect;
import rawr.base.effects.Trigger;
import rawr.base.items.ItemSlot;
import rawr.base.items.ItemType;

import java.util.EnumMap;
import java.util.Map;

/**
 * The stats a character has before wearing any gear: race and class base attributes, inherent
 * health/mana/attack power, base weapon miss chance and racial abilities.
 *
 * <p>The tables below are hardcoded game data transcribed from the original, not computed from a
 * formula: {@link #CLASS_STATS} holds the level-80 base values for every class, race and Druid form
 * (e.g. a level-80 Night Elf Druid starts with 104 agility before any item). The {@code level}
 * argument only affects the two scaling racials (Orc Blood Fury, via {@code level*4} attack power /
 * {@code level*2} spell power) and is part of the cache key; the attribute tables themselves are the
 * level-80 data.</p>
 *
 * <h2>Table layout</h2>
 *
 * <ul>
 *   <li>{@link ClassStats} &mdash; the class-wide values (mana, health, armor, crit, dodge, ...).</li>
 *   <li>{@link PrimaryStats} &mdash; the five primary attributes per race. A {@code null} entry
 *       means the original's {@code switch} had no case for that combination and left the
 *       attributes untouched; Hunter entries also carry the race-specific health value the original
 *       set inside the race case.</li>
 *   <li>{@link FormStats} &mdash; the Druid-form overrides. Only Druids consult the form.</li>
 * </ul>
 *
 * <p>The original expressed these tables as nested {@code switch} statements. Applying the same
 * values field by field is behaviorally identical because the setters are independent; the golden
 * dump produced by {@code tools/base-stats-probe} locks every class/race/form combination.</p>
 *
 * <h2>Racial abilities modeled here</h2>
 *
 * <ul>
 *   <li>Gnome &mdash; +5% intellect multiplier.</li>
 *   <li>Human &mdash; +3% spirit multiplier.</li>
 *   <li>Night Elf &mdash; +2% chance to be missed.</li>
 *   <li>Tauren &mdash; +5% maximum health.</li>
 *   <li>Troll &mdash; Berserking as an on-use haste {@link SpecialEffect} (10 s every 180 s; physical
 *       haste for melee classes, physical + spell haste for casters).</li>
 *   <li>Orc &mdash; Blood Fury as an on-use attack power / spell power effect (15 s every 120 s,
 *       class-dependent).</li>
 * </ul>
 *
 * <p>Racials that only matter with specific weapons are handled separately by
 * {@link #getRacialExpertise(Character, ItemSlot)}: Humans get 3 expertise with swords and maces,
 * Dwarves 5 with maces, Orcs 5 with axes, based on the item type in the main/off hand. Methods
 * return 0 for anything else.</p>
 *
 * <h2>Forms and caching</h2>
 *
 * <p>Druids have form-specific base stats; {@link DruidForm} selects the caster/bear/cat/moonkin
 * table. Results are cached in a single-entry cache keyed by (level, class, race, form); the cached
 * snapshot is copied on return because callers accumulate into the result. The cache is
 * {@code synchronized} because the optimizer can evaluate on worker threads.</p>
 */
public final class BaseStats {
    private BaseStats() {
    }

    public enum DruidForm { Caster, Bear, Cat, Moonkin }

    /** Five primary attributes, plus the race-specific health override the original set for Hunters. */
    private record PrimaryStats(float strength, float agility, float stamina, float intellect, float spirit,
                                Float health) {
        static PrimaryStats of(float strength, float agility, float stamina, float intellect, float spirit) {
            return new PrimaryStats(strength, agility, stamina, intellect, spirit, null);
        }

        static PrimaryStats of(float strength, float agility, float stamina, float intellect, float spirit,
                               float health) {
            return new PrimaryStats(strength, agility, stamina, intellect, spirit, health);
        }
    }

    /** Druid-form overrides; {@code bonusStaminaMultiplier} is 0 for the forms that do not set it. */
    private record FormStats(float attackPower, float physicalCrit, float dodge, float bonusStaminaMultiplier) {
        static FormStats of(float attackPower, float physicalCrit, float dodge) {
            return new FormStats(attackPower, physicalCrit, dodge, 0f);
        }
    }

    /** One row of the original class switch: class-wide values plus the race and form tables. */
    private record ClassStats(float mana, float health, float armor, float attackPower, float rangedAttackPower,
                              float physicalCrit, float dodge, float parry, float block, float spellPower,
                              float spellCrit, float defense,
                              Map<CharacterRace, PrimaryStats> races, Map<DruidForm, FormStats> forms) {
    }

    private static final Map<CharacterClass, ClassStats> CLASS_STATS = new EnumMap<>(CharacterClass.class);

    static {
        Map<CharacterRace, PrimaryStats> deathKnightRaces = races();
        deathKnightRaces.put(CharacterRace.BloodElf, PrimaryStats.of(172f, 114f, 158f, 39f, 58f));
        deathKnightRaces.put(CharacterRace.Draenei, PrimaryStats.of(176f, 109f, 162f, 36f, 61f));
        deathKnightRaces.put(CharacterRace.Dwarf, PrimaryStats.of(177f, 108f, 163f, 34f, 58f));
        deathKnightRaces.put(CharacterRace.Gnome, PrimaryStats.of(170f, 115f, 159f, 37f, 59f));
        deathKnightRaces.put(CharacterRace.Human, PrimaryStats.of(175f, 112f, 160f, 35f, 58f));
        deathKnightRaces.put(CharacterRace.NightElf, PrimaryStats.of(172f, 117f, 159f, 35f, 59f));
        deathKnightRaces.put(CharacterRace.Orc, PrimaryStats.of(178f, 109f, 162f, 32f, 62f));
        deathKnightRaces.put(CharacterRace.Tauren, PrimaryStats.of(180f, 107f, 162f, 30f, 61f));
        deathKnightRaces.put(CharacterRace.Troll, PrimaryStats.of(176f, 114f, 165f, 31f, 60f));
        deathKnightRaces.put(CharacterRace.Undead, PrimaryStats.of(174f, 110f, 161f, 33f, 64f));
        CLASS_STATS.put(CharacterClass.DeathKnight, new ClassStats(
                0f, 8121f, 0f, 220f, 0f, 0.0319f, 0.0362500f, 0.05f, 0f, 0f, 0f, 0f,
                deathKnightRaces, null));

        Map<CharacterRace, PrimaryStats> druidRaces = races();
        druidRaces.put(CharacterRace.NightElf, PrimaryStats.of(85f, 86f, 97f, 143f, 159f));
        druidRaces.put(CharacterRace.Tauren, PrimaryStats.of(94f, 77f, 100f, 140f, 185f));
        FormStats casterForm = FormStats.of(-10f, 0.0743f, 0.0556970f);
        Map<DruidForm, FormStats> druidForms = new EnumMap<>(DruidForm.class);
        druidForms.put(DruidForm.Caster, casterForm);
        druidForms.put(DruidForm.Moonkin, casterForm);
        druidForms.put(DruidForm.Bear, new FormStats(220f, 0.05f, 0.0556970f, 0.25f));
        druidForms.put(DruidForm.Cat, FormStats.of(140f, 0.07476f, 0.0556970f));
        CLASS_STATS.put(CharacterClass.Druid, new ClassStats(
                3496f, 7417f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.0185f, 0f,
                druidRaces, druidForms));

        Map<CharacterRace, PrimaryStats> hunterRaces = races();
        hunterRaces.put(CharacterRace.BloodElf, PrimaryStats.of(71f, 183f, 126f, 94f, 96f, 7324f));
        hunterRaces.put(CharacterRace.Draenei, PrimaryStats.of(75f, 178f, 129f, 91f, 99f, 7400f));
        hunterRaces.put(CharacterRace.Dwarf, PrimaryStats.of(76f, 177f, 131f, 89f, 96f, 7412f));
        hunterRaces.put(CharacterRace.NightElf, PrimaryStats.of(71f, 184f, 127f, 90f, 97f, 7324f));
        hunterRaces.put(CharacterRace.Orc, PrimaryStats.of(77f, 178f, 130f, 87f, 100f, 7324f));
        hunterRaces.put(CharacterRace.Tauren, PrimaryStats.of(79f, 176f, 130f, 85f, 99f, 7400f));
        hunterRaces.put(CharacterRace.Troll, PrimaryStats.of(75f, 183f, 129f, 86f, 98f, 7324f));
        CLASS_STATS.put(CharacterClass.Hunter, new ClassStats(
                5046f, 0f, 0f, 140f, 150f, -0.0153f, -0.0412730f, 0.05f, 0f, 0f, 0.0360f, 0f,
                hunterRaces, null));

        Map<CharacterRace, PrimaryStats> mageRaces = races();
        mageRaces.put(CharacterRace.BloodElf, PrimaryStats.of(33f, 45f, 57f, 185f, 173f));
        mageRaces.put(CharacterRace.Draenei, PrimaryStats.of(37f, 40f, 58f, 182f, 176f));
        mageRaces.put(CharacterRace.Gnome, PrimaryStats.of(31f, 46f, 58f, 184f, 174f));
        mageRaces.put(CharacterRace.Human, PrimaryStats.of(36f, 43f, 59f, 181f, 174f));
        mageRaces.put(CharacterRace.Troll, PrimaryStats.of(37f, 45f, 60f, 177f, 175f));
        mageRaces.put(CharacterRace.Undead, PrimaryStats.of(35f, 41f, 60f, 179f, 179f));
        CLASS_STATS.put(CharacterClass.Mage, new ClassStats(
                3268f, 6963f, 0f, -10f, 0f, 0.03f, 0.0361870f, 0f, 0f, 0f, 0.0091f, 0f,
                mageRaces, null));

        Map<CharacterRace, PrimaryStats> paladinRaces = races();
        paladinRaces.put(CharacterRace.BloodElf, PrimaryStats.of(148f, 92f, 143f, 101f, 103f));
        paladinRaces.put(CharacterRace.Draenei, PrimaryStats.of(152f, 87f, 143f, 98f, 107f));
        paladinRaces.put(CharacterRace.Dwarf, PrimaryStats.of(153f, 86f, 146f, 97f, 104f));
        paladinRaces.put(CharacterRace.Human, PrimaryStats.of(151f, 90f, 143f, 98f, 108f));
        CLASS_STATS.put(CharacterClass.Paladin, new ClassStats(
                4394f, 6934f, 0f, 240f, 0f, 0.0327f, 0.0349430f, 0.05f, 0.05f, 0f, 0.03336f, 400f,
                paladinRaces, null));

        Map<CharacterRace, PrimaryStats> priestRaces = races();
        priestRaces.put(CharacterRace.BloodElf, PrimaryStats.of(40f, 53f, 65f, 178f, 180f));
        priestRaces.put(CharacterRace.Draenei, PrimaryStats.of(44f, 48f, 66f, 175f, 183f));
        priestRaces.put(CharacterRace.Dwarf, PrimaryStats.of(45f, 47f, 70f, 176f, 180f));
        priestRaces.put(CharacterRace.Human, PrimaryStats.of(43f, 51f, 67f, 174f, 181f));
        priestRaces.put(CharacterRace.NightElf, PrimaryStats.of(40f, 56f, 66f, 174f, 181f));
        priestRaces.put(CharacterRace.Troll, PrimaryStats.of(44f, 53f, 68f, 170f, 182f));
        priestRaces.put(CharacterRace.Undead, PrimaryStats.of(42f, 49f, 68f, 172f, 186f));
        CLASS_STATS.put(CharacterClass.Priest, new ClassStats(
                3863f, 6960f, 0f, -10f, 0f, 0.027f, 0.0337780f, 0f, 0f, 0f, 0.0124f, 0f,
                priestRaces, null));

        Map<CharacterRace, PrimaryStats> rogueRaces = races();
        rogueRaces.put(CharacterRace.BloodElf, PrimaryStats.of(110f, 191f, 103f, 47f, 66f));
        rogueRaces.put(CharacterRace.Dwarf, PrimaryStats.of(115f, 185f, 108f, 42f, 66f));
        rogueRaces.put(CharacterRace.Gnome, PrimaryStats.of(108f, 192f, 104f, 46f, 67f));
        rogueRaces.put(CharacterRace.Human, PrimaryStats.of(113f, 189f, 105f, 43f, 67f));
        rogueRaces.put(CharacterRace.NightElf, PrimaryStats.of(110f, 194f, 104f, 43f, 67f));
        rogueRaces.put(CharacterRace.Orc, PrimaryStats.of(116f, 186f, 107f, 40f, 70f));
        rogueRaces.put(CharacterRace.Troll, PrimaryStats.of(114f, 191f, 108f, 39f, 68f));
        rogueRaces.put(CharacterRace.Undead, PrimaryStats.of(112f, 187f, 105f, 41f, 72f));
        CLASS_STATS.put(CharacterClass.Rogue, new ClassStats(
                0f, 7604f, 0f, 140f, 0f, -0.00295f, 0.0205570f, 0.05f, 0f, 0f, 0f, 0f,
                rogueRaces, null));

        Map<CharacterRace, PrimaryStats> shamanRaces = races();
        shamanRaces.put(CharacterRace.Draenei, PrimaryStats.of(121f, 71f, 136f, 128f, 145f));
        shamanRaces.put(CharacterRace.Orc, PrimaryStats.of(123f, 71f, 137f, 125f, 145f));
        shamanRaces.put(CharacterRace.Tauren, PrimaryStats.of(125f, 70f, 137f, 124f, 145f));
        shamanRaces.put(CharacterRace.Troll, PrimaryStats.of(121f, 76f, 137f, 124f, 144f));
        CLASS_STATS.put(CharacterClass.Shaman, new ClassStats(
                4396f, 6960f, 0f, 140f, 0f, 0.0292f, 0.0206800f, 0f, 0.05f, 0f, 0.022f, 0f,
                shamanRaces, null));

        Map<CharacterRace, PrimaryStats> warlockRaces = races();
        warlockRaces.put(CharacterRace.BloodElf, PrimaryStats.of(56f, 69f, 87f, 163f, 165f));
        warlockRaces.put(CharacterRace.Gnome, PrimaryStats.of(54f, 70f, 88f, 162f, 166f));
        warlockRaces.put(CharacterRace.Human, PrimaryStats.of(59f, 67f, 89f, 159f, 165f));
        warlockRaces.put(CharacterRace.Orc, PrimaryStats.of(62f, 64f, 91f, 156f, 169f));
        warlockRaces.put(CharacterRace.Undead, PrimaryStats.of(58f, 65f, 90f, 157f, 171f));
        CLASS_STATS.put(CharacterClass.Warlock, new ClassStats(
                3856f, 7164f, 0f, -10f, 0f, 0.028f, 0.0238110f, 0f, 0f, 0f, 0.01701f, 0f,
                warlockRaces, null));

        Map<CharacterRace, PrimaryStats> warriorRaces = races();
        warriorRaces.put(CharacterRace.Draenei, PrimaryStats.of(182f, 110f, 166f, 37f, 61f));
        warriorRaces.put(CharacterRace.Dwarf, PrimaryStats.of(176f, 109f, 162f, 35f, 58f));
        warriorRaces.put(CharacterRace.Gnome, PrimaryStats.of(169f, 116f, 158f, 38f, 59f));
        warriorRaces.put(CharacterRace.Human, PrimaryStats.of(174f, 113f, 159f, 36f, 58f));
        warriorRaces.put(CharacterRace.NightElf, PrimaryStats.of(171f, 118f, 158f, 36f, 59f));
        warriorRaces.put(CharacterRace.Orc, PrimaryStats.of(177f, 110f, 161f, 33f, 62f));
        warriorRaces.put(CharacterRace.Tauren, PrimaryStats.of(179f, 109f, 161f, 31f, 61f));
        warriorRaces.put(CharacterRace.Troll, PrimaryStats.of(185f, 115f, 162f, 32f, 60f));
        warriorRaces.put(CharacterRace.Undead, PrimaryStats.of(183f, 111f, 169f, 34f, 64f));
        CLASS_STATS.put(CharacterClass.Warrior, new ClassStats(
                0f, 8121f, 0f, 220f, 0f, 0.03192f, 0.03664f, 0.05f, 0.05f, 0f, 0f, 400f,
                warriorRaces, null));
    }

    private static Stats lastStats;
    private static int lastLevel;
    private static CharacterClass lastClass;
    private static CharacterRace lastRace;
    private static DruidForm lastForm;
    private static final Object syncLock = new Object();

    public static Stats getBaseStats(Character character) {
        return getBaseStats(character.getLevel(), character.getCharacterClass(), character.getRace(), DruidForm.Caster);
    }

    public static Stats getBaseStats(int level, CharacterClass characterClass, CharacterRace characterRace) {
        return getBaseStats(level, characterClass, characterRace, DruidForm.Caster);
    }

    public static Stats getBaseStats(int level, CharacterClass characterClass, CharacterRace characterRace, DruidForm characterForm) {
        synchronized (syncLock) {
            if (level == lastLevel
                    && characterClass == lastClass
                    && characterRace == lastRace
                    && characterForm == lastForm)
                return lastStats.copy();
            lastLevel = level;
            lastClass = characterClass;
            lastRace = characterRace;
            lastForm = characterForm;

            ClassStats classStats = CLASS_STATS.get(characterClass);
            Stats s = new Stats();
            s.setMiss(0.05f);
            if (classStats != null) {
                applyClassStats(s, classStats);
                applyPrimaryStats(s, classStats.races().get(characterRace));
                applyFormStats(s, classStats.forms() == null ? null : classStats.forms().get(characterForm));
            }

            applyRacials(s, level, characterClass, characterRace);

            lastStats = s.copy();
            return s;
        }
    }

    private static void applyClassStats(Stats s, ClassStats c) {
        s.setMana(c.mana());
        s.setHealth(c.health());
        s.setArmor(c.armor());
        s.setAttackPower(c.attackPower());
        s.setRangedAttackPower(c.rangedAttackPower());
        s.setPhysicalCrit(c.physicalCrit());
        s.setDodge(c.dodge());
        s.setParry(c.parry());
        s.setBlock(c.block());
        s.setSpellPower(c.spellPower());
        s.setSpellCrit(c.spellCrit());
        s.setDefense(c.defense());
    }

    private static void applyPrimaryStats(Stats s, PrimaryStats p) {
        if (p == null) {
            return;
        }
        s.setStrength(p.strength());
        s.setAgility(p.agility());
        s.setStamina(p.stamina());
        s.setIntellect(p.intellect());
        s.setSpirit(p.spirit());
        if (p.health() != null) {
            s.setHealth(p.health());
        }
    }

    private static void applyFormStats(Stats s, FormStats f) {
        if (f == null) {
            return;
        }
        s.setAttackPower(f.attackPower());
        s.setPhysicalCrit(f.physicalCrit());
        s.setDodge(f.dodge());
        s.setBonusStaminaMultiplier(f.bonusStaminaMultiplier());
    }

    private static void applyRacials(Stats s, int level, CharacterClass characterClass, CharacterRace characterRace) {
        if (characterRace == CharacterRace.Gnome) {
            s.setBonusIntellectMultiplier(0.05f);
        } else if (characterRace == CharacterRace.Human) {
            s.setBonusSpiritMultiplier(0.03f);
        } else if (characterRace == CharacterRace.NightElf) {
            s.setMiss(s.getMiss() + 0.02f);
        } else if (characterRace == CharacterRace.Tauren) {
            s.setHealth(s.getHealth() * 1.05f);
        } else if (characterRace == CharacterRace.Troll) {
            if (characterClass == CharacterClass.DeathKnight || characterClass == CharacterClass.Warrior || characterClass == CharacterClass.Rogue) {
                Stats effectStats = new Stats();
                effectStats.setPhysicalHaste(0.2f);
                s.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 10f, 180f));
            } else {
                Stats effectStats = new Stats();
                effectStats.setSpellHaste(0.2f);
                effectStats.setPhysicalHaste(0.2f);
                s.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 10f, 180f));
            }
        } else if (characterRace == CharacterRace.Orc) {
            if (characterClass == CharacterClass.Shaman) {
                Stats effectStats = new Stats();
                effectStats.setAttackPower(2 + (level * 4));
                effectStats.setSpellPower(3 + (level * 2));
                s.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 15f, 120f));
            } else if (characterClass == CharacterClass.Warlock) {
                Stats effectStats = new Stats();
                effectStats.setSpellPower(3 + (level * 2));
                s.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 15f, 120f));
            } else {
                Stats effectStats = new Stats();
                effectStats.setAttackPower(2 + (level * 4));
                s.addSpecialEffect(new SpecialEffect(Trigger.Use, effectStats, 15f, 120f));
            }
        }
    }

    private static Map<CharacterRace, PrimaryStats> races() {
        return new EnumMap<>(CharacterRace.class);
    }

    public static float getRacialExpertise(Character character, ItemSlot weaponSlot) {
        ItemType weaponType;
        CharacterRace characterRace = character.getRace();

        if (weaponSlot == ItemSlot.MainHand && character.get(CharacterSlot.MainHand) != null)
            weaponType = character.get(CharacterSlot.MainHand).getItem().getType();
        else if (weaponSlot == ItemSlot.OffHand && character.get(CharacterSlot.OffHand) != null)
            weaponType = character.get(CharacterSlot.OffHand).getItem().getType();
        else
            return 0.0f;

        switch (characterRace) {
            case Human:
                if (weaponType == ItemType.OneHandSword || weaponType == ItemType.TwoHandSword
                        || weaponType == ItemType.OneHandMace || weaponType == ItemType.TwoHandMace) {
                    return 3.0f;
                }
                break;
            case Dwarf:
                if (weaponType == ItemType.OneHandMace || weaponType == ItemType.TwoHandMace) {
                    return 5.0f;
                }
                break;
            case Orc:
                if (weaponType == ItemType.OneHandAxe || weaponType == ItemType.TwoHandAxe) {
                    return 5.0f;
                }
                break;
        }

        return 0.0f;
    }
}
