// Phase 1 oracle: fixture generator (discovery + generation).
// Built with mcs against the prebuilt Rawr.Base.dll / Rawr.Mage.dll (legacy fork).
using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Reflection;
using Rawr;
using Rawr.Bear;
using Rawr.Cat;
using Rawr.DPSDK;
using Rawr.DPSWarr;
using Rawr.Elemental;
using Rawr.Enhance;
using Rawr.Healadin;
using Rawr.HealPriest;
using Rawr.Mage;
using Rawr.Moonkin;
using Rawr.ProtPaladin;
using Rawr.ProtWarr;
using Rawr.RestoSham;
using Rawr.Retribution;
using Rawr.Rogue;
using Rawr.ShadowPriest;
using Rawr.Hunter;
using Rawr.TankDK;
using Rawr.Tree;
using Rawr.Warlock;

public static class FixtureGen
{
    static string BaseDir { get { return AppDomain.CurrentDomain.BaseDirectory; } }
    static string DataFile(string name) { return Path.Combine(BaseDir, "Data", name); }

    // gems: red sp, orange (red+yellow), purple (red+blue), meta
    const int GEM_RED = 40113;      // Runed Cardinal Ruby +23 SP
    const int GEM_YELLOW = 40155;   // Reckless Ametrine +12 SP +10 haste
    const int GEM_BLUE = 40133;     // Purified Dreadstone +12 SP +10 spirit
    const int GEM_META = 41285;     // Chaotic Skyflare Diamond
    const int GEM_PRISMATIC = 49110; // Nightmare Tear

    // enchants
    const int ENCH_HEAD = 3820;     // Arcanum of Burning Mysteries
    const int ENCH_SHOULDER = 3810; // Greater Inscription of the Storm
    const int ENCH_BACK = 3728;     // Darkglow Embroidery
    const int ENCH_CHEST = 2661;    // Exceptional Stats
    const int ENCH_WRIST = 2332;    // Superior Spellpower
    const int ENCH_HANDS = 3246;    // Exceptional Spellpower
    const int ENCH_LEGS = 3719;     // Brilliant Spellthread
    const int ENCH_FEET = 3826;     // Icewalker
    const int ENCH_WEAPON = 3834;   // Mighty Spellpower

    // shadow DPS gem variant
    const int GEM_HIT_YELLOW = 40153; // Veiled Ametrine +12 SP +10 hit

    // healer gems (epic)
    const int GEM_HEAL_YELLOW_HASTE = 40155; // Reckless Ametrine +12 SP +10 haste
    const int GEM_HEAL_YELLOW_INT = 40151;   // Luminous Ametrine +12 SP +10 int
    const int GEM_HEAL_BLUE_MP5 = 40175;     // Dazzling Eye of Zul +10 int +5 mp5
    const int GEM_HEAL_META = 41401;         // Insightful Earthsiege Diamond

    // cat gems (from CalculationsCat.DefaultGemmingTemplates)
    const int GEM_CAT_RED = 40112;      // Delicate Cardinal Ruby +20 Agi
    const int GEM_CAT_YELLOW = 40147;   // Deadly Ametrine +10 Agi +10 Crit
    const int GEM_CAT_BLUE = 40130;     // Shifting Dreadstone +10 Agi +15 Sta
    const int GEM_CAT_META = 41398;     // Relentless Earthsiege Diamond
    const int GEM_CAT_PRISMATIC = 49110; // Nightmare Tear

    // cat enchants
    const int ENCH_CAT_HEAD = 3817;     // Arcanum of Torment
    const int ENCH_CAT_SHOULDER = 3808; // Greater Inscription of the Axe
    const int ENCH_CAT_BACK = 1099;     // Major Agility
    const int ENCH_CAT_CHEST = 3832;    // Powerful Stats
    const int ENCH_CAT_WRIST = 3845;    // Greater Assault
    const int ENCH_CAT_HANDS = 1603;    // Crusher
    const int ENCH_CAT_LEGS = 3823;     // Icescale Leg Armor
    const int ENCH_CAT_FEET = 1597;     // Greater Assault
    const int ENCH_CAT_WEAPON = 3827;   // Massacre

    // bear gems (from CalculationsBear.DefaultGemmingTemplates, epic tier)
    const int GEM_BEAR_RED = 40112;       // Delicate Cardinal Ruby +20 Agi
    const int GEM_BEAR_YELLOW = 40148;    // Glinting Ametrine +10 Agi +10 Hit
    const int GEM_BEAR_YELLOW_STA = 40160; // Stalwart Ametrine (Agi/Sta)
    const int GEM_BEAR_BLUE = 40130;      // Shifting Dreadstone +10 Agi +15 Sta
    const int GEM_BEAR_BLUE_STA = 40119;  // Solid Sky Sapphire +30 Sta
    const int GEM_BEAR_META = 41380;      // Austere Earthsiege Diamond
    const int GEM_BEAR_PRISMATIC = 49110; // Nightmare Tear

    // bear tank enchants
    const int ENCH_BEAR_HEAD = 3818;     // Arcanum of the Stalwart Protector
    const int ENCH_BEAR_SHOULDER = 3811; // Greater Inscription of the Pinnacle
    const int ENCH_BEAR_BACK = 1951;     // Titanweave
    const int ENCH_BEAR_CHEST = 3832;    // Powerful Stats
    const int ENCH_BEAR_WRIST = 3850;    // Major Stamina
    const int ENCH_BEAR_HANDS = 3253;    // Armsman (2% threat, 10 parry)
    const int ENCH_BEAR_LEGS = 3822;     // Frosthide Leg Armor
    const int ENCH_BEAR_FEET = 3232;     // Tuskarr's Vitality
    const int ENCH_BEAR_WEAPON = 3827;   // Massacre

    static void Main(string[] args)
    {
        string mode = args.Length > 0 ? args[0] : "list";
        string outDir = args.Length > 1 ? args[1] : Path.Combine(BaseDir, "..", "..", "fixtures", "mage");

        ItemCache.Load();
        Calculations.RegisterModel(typeof(CalculationsMage));
        Calculations.RegisterModel(typeof(CalculationsRestoSham));
        Calculations.RegisterModel(typeof(CalculationsCat));

        if (mode == "list-resto") { ListResto(); return; }
        if (mode == "gen-resto") { RestoMain(outDir); return; }
        if (mode == "list-cat") { ListCat(); return; }
        if (mode == "gen-cat") { CatMain(outDir); return; }
        if (mode == "list-bear") { ListBear(); return; }
        if (mode == "gen-bear") { BearMain(outDir); return; }
        if (mode == "list-healadin") { ListHealadin(); return; }
        if (mode == "gen-healadin") { HealadinMain(outDir); return; }
        if (mode == "list-healpriest") { ListHealpriest(); return; }
        if (mode == "gen-healpriest") { HealpriestMain(outDir); return; }
        if (mode == "list-protwarr") { ListProtWarr(); return; }
        if (mode == "gen-protwarr") { ProtWarrMain(outDir); return; }
        if (mode == "list-moonkin") { ListMoonkin(); return; }
        if (mode == "gen-moonkin") { MoonkinMain(outDir); return; }
        if (mode == "list-elemental") { ListElemental(); return; }
        if (mode == "gen-elemental") { ElementalMain(outDir); return; }
        if (mode == "list-ret") { ListRet(); return; }
        if (mode == "gen-ret") { RetMain(outDir); return; }
        if (mode == "list-enhance") { ListEnhance(); return; }
        if (mode == "gen-enhance") { EnhanceMain(outDir); return; }
        if (mode == "list-protpally") { ListProtPaladin(); return; }
        if (mode == "gen-protpally") { ProtPaladinMain(outDir); return; }
        if (mode == "list-shadowpriest") { ListShadowpriest(); return; }
        if (mode == "gen-shadowpriest") { ShadowpriestMain(outDir); return; }
        if (mode == "list-hunter") { ListHunter(); return; }
        if (mode == "gen-hunter") { HunterMain(outDir); return; }
        if (mode == "list-rogue") { ListRogue(); return; }
        if (mode == "gen-rogue") { RogueMain(outDir); return; }
        if (mode == "list-tree") { ListTree(); return; }
        if (mode == "gen-tree") { TreeMain(outDir); return; }
        if (mode == "list-tankdk") { ListTankDK(); return; }
        if (mode == "gen-tankdk") { TankDKMain(outDir); return; }
        if (mode == "list-warlock") { ListWarlock(); return; }
        if (mode == "gen-warlock") { WarlockMain(outDir); return; }
        if (mode == "list-dpsdk") { ListDPSDK(); return; }
        if (mode == "gen-dpsdk") { DPSDKMain(outDir); return; }
        if (mode == "list-dpswarr") { ListDPSWarr(); return; }
        if (mode == "gen-dpswarr") { DPSWarrMain(outDir); return; }

        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.xml")));
        new CalculationsMage();

        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);

        if (mode == "list") { ListAll(); return; }
        Generate(outDir);
    }

    // ------------------------------------------------------------------
    // RestoSham (Phase 6 oracle)
    // ------------------------------------------------------------------

    static void RestoMain(string outDir)
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Calculations.LoadModel(typeof(CalculationsRestoSham));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        RestoGenerate(outDir);
    }

    static SavedTalentSpec RestoSpec(string name)
    {
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Shaman))
            if (spec.Name == name) return spec;
        throw new InvalidOperationException("shaman spec not found: " + name);
    }

    static void PrintTalents(string label, TalentsBase t)
    {
        Console.WriteLine("-- " + label + " --");
        List<string> parts = new List<string>();
        foreach (PropertyInfo pi in t.GetType().GetProperties(BindingFlags.Public | BindingFlags.Instance))
        {
            if (pi.DeclaringType != t.GetType() || pi.GetIndexParameters().Length != 0) continue;
            object v = pi.GetValue(t, null);
            if (pi.PropertyType == typeof(int) && (int)v != 0) parts.Add(pi.Name + "=" + v);
            else if (pi.PropertyType == typeof(bool) && (bool)v) parts.Add(pi.Name);
        }
        parts.Sort(StringComparer.Ordinal);
        foreach (string p in parts) Console.WriteLine("   " + p);
    }

    static void ListResto()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== SHAMAN TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Shaman))
        {
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));
            TalentsBase t = spec.TalentSpec();
            if (t != null) PrintTalents(spec.Name, t);
        }

        Console.WriteLine();
        Console.WriteLine("== HEALER ENCHANT SPOT CHECK ==");
        int[] ids = new int[] { 3820, 3810, 3728, 2661, 2332, 3246, 3719, 3826, 3834, 3606, 3722, 3854, 3871, 3096, 3731, 3855 };
        foreach (Enchant e in Enchant.AllEnchants.Where(x => ids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | slot={2} | {3}", e.Id, e.Name, e.Slot, e.Stats == null ? "" : StatLine(e.Stats)));

        Console.WriteLine();
        Console.WriteLine("== HEALER GEM SPOT CHECK ==");
        int[] gids = new int[] { 40113, 40151, 40155, 40175, 40133, 41401, 41333, 41376, 49110 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLine(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== MAIL ARMOR (epic, ilvl>=232, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 232 && i.Type == ItemType.Mail)
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(12))
                Console.WriteLine(ItemLine(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger, ItemSlot.Trinket })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 232)
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(14))
                Console.WriteLine(ItemLine(item));
        }
        Console.WriteLine("-- Ranged (Totems) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Quality == ItemQuality.Epic)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- OffHand --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.OffHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 200)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- MainHand (one-hand caster) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.MainHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 200
                && (i.Type == ItemType.OneHandMace || i.Type == ItemType.OneHandAxe || i.Type == ItemType.Dagger || i.Type == ItemType.FistWeapon))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine(item));
    }

    static string ItemLine(Item item)
    {
        int fx = 0;
        foreach (SpecialEffect e in item.Stats.SpecialEffects()) fx++;
        return string.Format(CultureInfo.InvariantCulture, "{0} | {1} | ilvl {2} | {3} | set={4} | fx={5} | {6}",
            item.Id, item.Name, item.ItemLevel, item.Type, item.SetName, fx, StatLine(item.Stats));
    }

    static string StatLine(Stats s)
    {
        return string.Format(CultureInfo.InvariantCulture,
            "sp={0:F0} int={1:F0} crit={2:F0} haste={3:F0} hit={4:F0} spirit={5:F0} mp5={6:F0}",
            s.SpellPower, s.Intellect, s.CritRating, s.HasteRating, s.HitRating, s.Spirit, s.Mp5);
    }

    static void ListAll()
    {
        Console.WriteLine("== MAGE TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Mage))
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));

        Console.WriteLine();
        Console.WriteLine("== ENCHANT ID SPOT CHECK ==");
        int[] ids = new int[] { 3826, 3834, 3854, 3722, 3606, 3820, 3810, 3728, 2661, 2332, 3246, 3719 };
        foreach (Enchant e in Enchant.AllEnchants.Where(x => ids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | slot={2} | {3}", e.Id, e.Name, e.Slot, e.Stats == null ? "" : StatLine(e.Stats)));

        Console.WriteLine();
        Console.WriteLine("== ITEMS BY SLOT (top by ilvl) ==");
        List<string> armorSlots = new List<string>(new string[] { "Head", "Neck", "Shoulders", "Back", "Chest", "Wrist", "Hands", "Waist", "Legs", "Feet", "Finger", "Trinket" });
        foreach (string slotName in armorSlots)
        {
            ItemSlot slot = (ItemSlot)Enum.Parse(typeof(ItemSlot), slotName);
            Console.WriteLine("-- " + slotName + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && (slot == ItemSlot.Finger || slot == ItemSlot.Trinket || slot == ItemSlot.Neck || slot == ItemSlot.Back || i.Type == ItemType.Cloth))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(12))
                Console.WriteLine(string.Format(CultureInfo.InvariantCulture, "{0} | {1} | ilvl {2} | {3} | set={4} | {5}",
                    item.Id, item.Name, item.ItemLevel, item.Type, item.SetName, StatLine(item.Stats)));
        }
    }

    static void E(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = ItemCache.FindItemById(itemId);
            if (item == null) { Console.Error.WriteLine("MISSING ITEM " + itemId); }
            else if (gems)
            {
                g1 = GemFor(item.SocketColor1);
                g2 = GemFor(item.SocketColor2);
                g3 = GemFor(item.SocketColor3);
            }
            if (enchants) ench = EnchForSlot(slot);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    static int GemFor(ItemSlot color)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_RED;
            case ItemSlot.Yellow: return GEM_YELLOW;
            case ItemSlot.Blue: return GEM_BLUE;
            case ItemSlot.Meta: return GEM_META;
            case ItemSlot.Prismatic: return GEM_PRISMATIC;
            default: return 0;
        }
    }

    static int EnchForSlot(CharacterSlot slot)
    {
        switch (slot)
        {
            case CharacterSlot.Head: return ENCH_HEAD;
            case CharacterSlot.Shoulders: return ENCH_SHOULDER;
            case CharacterSlot.Back: return ENCH_BACK;
            case CharacterSlot.Chest: return ENCH_CHEST;
            case CharacterSlot.Wrist: return ENCH_WRIST;
            case CharacterSlot.Hands: return ENCH_HANDS;
            case CharacterSlot.Legs: return ENCH_LEGS;
            case CharacterSlot.Feet: return ENCH_FEET;
            case CharacterSlot.MainHand: return ENCH_WEAPON;
            default: return 0;
        }
    }

    static void Gear277(Character c, bool gems, bool enchants)
    {
        E(c, CharacterSlot.Head, 51281, gems, enchants);
        E(c, CharacterSlot.Neck, 50658, gems, enchants);
        E(c, CharacterSlot.Shoulders, 51284, gems, enchants);
        E(c, CharacterSlot.Back, 50628, gems, enchants);
        E(c, CharacterSlot.Chest, 51283, gems, enchants);
        E(c, CharacterSlot.Wrist, 50686, gems, enchants);
        E(c, CharacterSlot.Hands, 51280, gems, enchants);
        E(c, CharacterSlot.Waist, 50613, gems, enchants);
        E(c, CharacterSlot.Legs, 51282, gems, enchants);
        E(c, CharacterSlot.Feet, 50699, gems, enchants);
        E(c, CharacterSlot.Finger1, 50404, gems, enchants);
        E(c, CharacterSlot.Finger2, 50604, gems, enchants);
        E(c, CharacterSlot.Trinket1, 50348, gems, enchants);
        E(c, CharacterSlot.Trinket2, 50365, gems, enchants);
        E(c, CharacterSlot.MainHand, 50732, gems, enchants);
        E(c, CharacterSlot.OffHand, 50719, gems, enchants);
        E(c, CharacterSlot.Ranged, 50684, gems, enchants);
    }

    static void Gear264(Character c, bool gems, bool enchants)
    {
        E(c, CharacterSlot.Head, 51837, gems, enchants);
        E(c, CharacterSlot.Neck, 50647, gems, enchants);
        E(c, CharacterSlot.Shoulders, 51883, gems, enchants);
        E(c, CharacterSlot.Back, 54583, gems, enchants);
        E(c, CharacterSlot.Chest, 50975, gems, enchants);
        E(c, CharacterSlot.Wrist, 49994, gems, enchants);
        E(c, CharacterSlot.Hands, 51874, gems, enchants);
        E(c, CharacterSlot.Waist, 49978, gems, enchants);
        E(c, CharacterSlot.Legs, 49891, gems, enchants);
        E(c, CharacterSlot.Feet, 50062, gems, enchants);
        E(c, CharacterSlot.Finger1, 50400, gems, enchants);
        E(c, CharacterSlot.Finger2, 50618, gems, enchants);
        E(c, CharacterSlot.Trinket1, 50366, gems, enchants);
        E(c, CharacterSlot.Trinket2, 50348, gems, enchants);
        E(c, CharacterSlot.MainHand, 50608, gems, enchants);
        E(c, CharacterSlot.OffHand, 50719, gems, enchants);
        E(c, CharacterSlot.Ranged, 50631, gems, enchants);
    }

    static MageTalents ArcaneTalents()
    {
        SavedTalentSpecList specs = SavedTalentSpec.SpecsFor(CharacterClass.Mage);
        if (specs.Count > 0 && specs[0].Spec != null)
            return new MageTalents(specs[0].Spec);
        MageTalents t = new MageTalents();
        t.ArcaneSubtlety = 2; t.ArcaneFocus = 3; t.MagicAbsorption = 2; t.ArcaneConcentration = 5;
        t.MagicAttunement = 2; t.SpellImpact = 3; t.FocusMagic = 1; t.ArcaneMeditation = 3;
        t.TormentTheWeak = 3; t.PresenceOfMind = 1; t.ArcaneMind = 5; t.ArcaneInstability = 3;
        t.ArcanePotency = 2; t.ArcaneEmpowerment = 3; t.ArcanePower = 1; t.ArcaneFlows = 2;
        t.MindMastery = 5; t.MissileBarrage = 5; t.NetherwindPresence = 3; t.SpellPower = 2;
        t.ArcaneBarrage = 1; t.ImprovedFireBlast = 2; t.Incineration = 3; t.ImprovedFireball = 4;
        t.Precision = 3; t.IcyVeins = 1; t.IceShards = 3; t.PiercingIce = 3; t.FrostChanneling = 3;
        return t;
    }

    static MageTalents FireTalents()
    {
        MageTalents t = new MageTalents();
        // Arcane 18
        t.ArcaneSubtlety = 2; t.ArcaneFocus = 3; t.MagicAbsorption = 2; t.ArcaneConcentration = 5;
        t.MagicAttunement = 2; t.SpellImpact = 3; t.FocusMagic = 1;
        // Fire 53
        t.ImprovedFireball = 5; t.Ignite = 5; t.WorldInFlames = 3; t.Pyroblast = 1;
        t.BurningSoul = 3; t.ImprovedScorch = 3; t.MasterOfElements = 3; t.PlayingWithFire = 3;
        t.CriticalMass = 3; t.FirePower = 5; t.Pyromaniac = 3; t.Combustion = 1;
        t.MoltenFury = 3; t.EmpoweredFire = 3; t.HotStreak = 3; t.Burnout = 5; t.LivingBomb = 1;
        // glyphs
        t.GlyphOfFireball = true; t.GlyphOfMoltenArmor = true; t.GlyphOfLivingBomb = true;
        return t;
    }

    static MageTalents FrostTalents()
    {
        MageTalents t = new MageTalents();
        // Arcane 18
        t.ArcaneSubtlety = 2; t.ArcaneFocus = 3; t.MagicAbsorption = 2; t.ArcaneConcentration = 5;
        t.MagicAttunement = 2; t.SpellImpact = 3; t.FocusMagic = 1;
        // Frost 53
        t.ImprovedFrostbolt = 5; t.IceShards = 3; t.Precision = 3; t.PiercingIce = 3;
        t.IcyVeins = 1; t.ImprovedBlizzard = 1; t.FrostChanneling = 3; t.Shatter = 3;
        t.ColdSnap = 1; t.ColdAsIce = 2; t.WintersChill = 3; t.IceBarrier = 1;
        t.ArcticWinds = 5; t.EmpoweredFrostbolt = 2; t.FingersOfFrost = 2; t.BrainFreeze = 3;
        t.SummonWaterElemental = 1; t.EnduringWinter = 3; t.ChilledToTheBone = 5; t.DeepFreeze = 1;
        t.IceFloes = 2;
        // glyphs
        t.GlyphOfEternalWater = true; t.GlyphOfFrostbolt = true; t.GlyphOfMoltenArmor = true;
        return t;
    }

    static Character NewMage(string name, MageTalents talents, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = CharacterRace.Troll;
        c.Class = CharacterClass.Mage;
        c.CurrentModel = "Mage";
        c.MageTalents = talents;
        if (raidBuffs)
            CalculationsMage.Instance.SetDefaults(c);
        else
        {
            c.ActiveBuffsAdd("Arcane Intellect");
            c.ActiveBuffsAdd("Molten Armor");
        }
        return c;
    }

    static void Finish(Character c, CalculationOptionsMage options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        options.Character = c;
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    static void Generate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Arcane 57/3/11 full raid buffs, BiS-ish 277, gems+enchants
        {
            Character c = NewMage("ArcaneRaid277", ArcaneTalents(), true);
            Gear277(c, true, true);
            CalculationOptionsMage o = new CalculationOptionsMage();
            o.ReconstructSequence = true;
            Finish(c, o, Path.Combine(outDir, "arcane_277_raid.xml"));
        }

        // 2. Fire 18/53/0 full raid buffs, same gear
        {
            Character c = NewMage("FireRaid277", FireTalents(), true);
            Gear277(c, true, true);
            CalculationOptionsMage o = new CalculationOptionsMage();
            o.ReconstructSequence = true;
            Finish(c, o, Path.Combine(outDir, "fire_277_raid.xml"));
        }

        // 3. Frost 18/0/53, ilvl 264 gear, full raid buffs
        {
            Character c = NewMage("FrostRaid264", FrostTalents(), true);
            Gear264(c, true, true);
            CalculationOptionsMage o = new CalculationOptionsMage();
            Finish(c, o, Path.Combine(outDir, "frost_264_raid.xml"));
        }

        // 4. Arcane, same gear, no raid buffs/debuffs, no gems/enchants
        {
            Character c = NewMage("ArcaneNoBuffs", ArcaneTalents(), false);
            Gear277(c, false, false);
            CalculationOptionsMage o = new CalculationOptionsMage();
            Finish(c, o, Path.Combine(outDir, "arcane_nobuffs.xml"));
        }

        // 5. Arcane mana-starved: long fight, consumables disabled, Evocation available
        {
            Character c = NewMage("ArcaneManaStarved", ArcaneTalents(), true);
            Gear264(c, true, true);
            CalculationOptionsMage o = new CalculationOptionsMage();
            o.FightDuration = 600;
            o.ManaPotionEnabled = false;
            o.ManaGemEnabled = false;
            o.ReconstructSequence = false;
            Finish(c, o, Path.Combine(outDir, "arcane_manastarved_600.xml"));
        }

        // 6. Fire 2pc T10 + proc trinkets (head/legs set, chest/shoulders offpieces)
        {
            Character c = NewMage("FireT10Procs", FireTalents(), true);
            Gear277(c, true, true);
            E(c, CharacterSlot.Shoulders, 50643, true, true); // Shoulders of Mercy Killing
            E(c, CharacterSlot.Chest, 50629, true, true);     // Robe of the Waking Nightmare
            E(c, CharacterSlot.Trinket1, 50348, true, true);  // Dislodged Foreign Object
            E(c, CharacterSlot.Trinket2, 50366, true, true);  // Althor's Abacus
            CalculationOptionsMage o = new CalculationOptionsMage();
            Finish(c, o, Path.Combine(outDir, "fire_t10_2pc_procs.xml"));
        }

        // 7. Arcane cooldown stacking: 120s fight, Heroism + Power Infusion + Drums
        {
            Character c = NewMage("ArcaneCooldowns", ArcaneTalents(), true);
            Gear277(c, true, true);
            CalculationOptionsMage o = new CalculationOptionsMage();
            o.FightDuration = 120;
            o.HeroismAvailable = true;
            o.PowerInfusionAvailable = true;
            o.DrumsOfBattle = true;
            Finish(c, o, Path.Combine(outDir, "arcane_cooldowns_120.xml"));
        }

        // 8. Frost hit/movement/latency options on 264 gear
        {
            Character c = NewMage("FrostHitCap", FrostTalents(), true);
            Gear264(c, true, true);
            CalculationOptionsMage o = new CalculationOptionsMage();
            o.MovementFrequency = 4f;
            o.MovementDuration = 0.5f;
            o.LatencyCast = 0.05f;
            o.LatencyGCD = 0.1f;
            o.LatencyChannel = 0.3f;
            Finish(c, o, Path.Combine(outDir, "frost_hitcap_movement.xml"));
        }

        // 9. Blank baseline: no gear, no buffs, no gems/enchants
        {
            Character c = NewMage("ArcaneBlank", ArcaneTalents(), false);
            c.ActiveBuffs.Clear();
            CalculationOptionsMage o = new CalculationOptionsMage();
            Finish(c, o, Path.Combine(outDir, "arcane_blank.xml"));
        }

        // 10. Arcane advanced solver (SMP): segment cooldowns/mana, integral mana,
        //     constraints level 5, high heap limit so the search finishes headlessly.
        {
            Character c = NewMage("ArcaneAdvanced", ArcaneTalents(), true);
            Gear277(c, true, true);
            CalculationOptionsMage o = new CalculationOptionsMage();
            o.FightDuration = 120;
            o.MaxHeapLimit = 5000;
            o.ComparisonSegmentCooldowns = true; o.DisplaySegmentCooldowns = true;
            o.ComparisonSegmentMana = true; o.DisplaySegmentMana = true;
            o.ComparisonIntegralMana = true; o.DisplayIntegralMana = true;
            o.ComparisonAdvancedConstraintsLevel = 5; o.DisplayAdvancedConstraintsLevel = 5;
            o.ReconstructSequence = true;
            Finish(c, o, Path.Combine(outDir, "arcane_advanced_120.xml"));
        }

        // 11. Arcane beta (WotLK Classic rank data path)
        {
            Character c = NewMage("ArcaneBeta", ArcaneTalents(), true);
            Gear264(c, true, true);
            CalculationOptionsMage o = new CalculationOptionsMage();
            o.Beta = true;
            Finish(c, o, Path.Combine(outDir, "arcane_beta.xml"));
        }

        // 12. Fire with an enabled custom spell mix (mix-only rotation)
        {
            Character c = NewMage("FireCustomMix", FireTalents(), true);
            Gear277(c, true, true);
            CalculationOptionsMage o = new CalculationOptionsMage();
            o.CustomSpellMixEnabled = true;
            o.CustomSpellMixOnly = true;
            o.CustomSpellMix = new List<SpellWeight>();
            o.CustomSpellMix.Add(new SpellWeight { Spell = SpellId.Fireball, Weight = 1.0f });
            o.CustomSpellMix.Add(new SpellWeight { Spell = SpellId.Pyroblast, Weight = 0.35f });
            o.CustomSpellMix.Add(new SpellWeight { Spell = SpellId.LivingBomb, Weight = 0.2f });
            Finish(c, o, Path.Combine(outDir, "fire_custommix.xml"));
        }

        // 13. Frost against a level 80 target (different hit caps) with target damage
        {
            Character c = NewMage("FrostTarget80", FrostTalents(), true);
            Gear264(c, true, true);
            CalculationOptionsMage o = new CalculationOptionsMage();
            o.TargetLevel = 80;
            o.TargetDamage = 5000000;
            Finish(c, o, Path.Combine(outDir, "frost_target80.xml"));
        }
    }

    // ==================================================================
    // RestoSham fixture generation (Phase 6 oracle)
    // ==================================================================

    // Every id used here must already exist in the shipped ItemCache.xml; fail
    // loudly instead of touching the network (AGENTS oracle rule).
    static Item RequireItem(int itemId)
    {
        Item item = ItemCache.FindItemById(itemId);
        if (item == null) throw new InvalidOperationException("item id " + itemId + " is not in the shipped ItemCache.xml");
        return item;
    }

    static int GemForHeal(ItemSlot color, int yellowGem, int blueGem)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_RED;
            case ItemSlot.Yellow: return yellowGem;
            case ItemSlot.Blue: return blueGem;
            case ItemSlot.Meta: return GEM_HEAL_META;
            case ItemSlot.Prismatic: return GEM_PRISMATIC;
            default: return 0;
        }
    }

    static void RE(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants, int yellowGem, int blueGem)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = RequireItem(itemId);
            if (gems)
            {
                g1 = GemForHeal(item.SocketColor1, yellowGem, blueGem);
                g2 = GemForHeal(item.SocketColor2, yellowGem, blueGem);
                g3 = GemForHeal(item.SocketColor3, yellowGem, blueGem);
            }
            if (enchants) ench = EnchForSlot(slot);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    static Character NewResto(string name, ShamanTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Shaman;
        c.CurrentModel = "RestoSham";
        c.ShamanTalents = talents;
        if (raidBuffs)
            ((CalculationsRestoSham)Calculations.Instance).SetDefaults(c);
        else
            c.ActiveBuffsAdd("Earthliving Weapon");
        return c;
    }

    static void FinishResto(Character c, CalculationOptionsRestoSham options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    static ShamanTalents RestoDefaultSpec()
    {
        TalentsBase t = RestoSpec("Default Resto (0/13/55)").TalentSpec();
        if (t == null) throw new InvalidOperationException("Default Resto spec failed to parse");
        return (ShamanTalents)t;
    }

    // 264 mail healer set: T10 264 "Garb" (the set that carries RestoSham2T10/4T10)
    // plus ICC 25 / ToC 25 offpieces and the T10 resto totem.
    static void GearResto264(Character c, bool gems, bool enchants, int yellow, int blue)
    {
        RE(c, CharacterSlot.Head, 51192, gems, enchants, yellow, blue);      // Sanctified Frost Witch's Headpiece (Garb)
        RE(c, CharacterSlot.Neck, 50005, gems, enchants, yellow, blue);      // Amulet of the Silent Eulogy
        RE(c, CharacterSlot.Shoulders, 51194, gems, enchants, yellow, blue); // Sanctified Frost Witch's Spaulders (Garb)
        RE(c, CharacterSlot.Back, 54583, gems, enchants, yellow, blue);      // Cloak of Burning Dusk
        RE(c, CharacterSlot.Chest, 51190, gems, enchants, yellow, blue);     // Sanctified Frost Witch's Tunic (Garb)
        RE(c, CharacterSlot.Wrist, 50030, gems, enchants, yellow, blue);     // Bloodsunder's Bracers
        RE(c, CharacterSlot.Hands, 51191, gems, enchants, yellow, blue);     // Sanctified Frost Witch's Handguards (Garb)
        RE(c, CharacterSlot.Waist, 50015, gems, enchants, yellow, blue);     // Belt of the Blood Nova
        RE(c, CharacterSlot.Legs, 51193, gems, enchants, yellow, blue);      // Sanctified Frost Witch's Legguards (Garb)
        RE(c, CharacterSlot.Feet, 49993, gems, enchants, yellow, blue);      // Necrophotic Greaves
        RE(c, CharacterSlot.Finger1, 50400, gems, enchants, yellow, blue);   // Ashen Band of Endless Wisdom
        RE(c, CharacterSlot.Finger2, 50720, gems, enchants, yellow, blue);   // Incarnadine Band of Mending
        RE(c, CharacterSlot.Trinket1, 48724, gems, enchants, yellow, blue);  // Talisman of Resurgence (Use)
        RE(c, CharacterSlot.Trinket2, 50358, gems, enchants, yellow, blue);  // Purified Lunar Dust (SpellCast proc)
        RE(c, CharacterSlot.MainHand, 50028, gems, enchants, yellow, blue);  // Trauma (264)
        RE(c, CharacterSlot.OffHand, 50616, gems, enchants, yellow, blue);   // Bulwark of Smouldering Steel
        RE(c, CharacterSlot.Ranged, 50464, gems, enchants, yellow, blue);    // Totem of the Surging Sea (RestoShamRelicT10)
    }

    // 277 heroic set: T10 277 "Garb", heroic ICC 25 offpieces, proc trinkets, totem.
    static void GearResto277(Character c, bool gems, bool enchants, int yellow, int blue)
    {
        RE(c, CharacterSlot.Head, 51247, gems, enchants, yellow, blue);      // Sanctified Frost Witch's Headpiece (Garb)
        RE(c, CharacterSlot.Neck, 50609, gems, enchants, yellow, blue);      // Bone Sentinel's Amulet
        RE(c, CharacterSlot.Shoulders, 51245, gems, enchants, yellow, blue); // Sanctified Frost Witch's Spaulders (Garb)
        RE(c, CharacterSlot.Back, 50668, gems, enchants, yellow, blue);      // Greatcloak of the Turned Champion
        RE(c, CharacterSlot.Chest, 51249, gems, enchants, yellow, blue);     // Sanctified Frost Witch's Tunic (Garb)
        RE(c, CharacterSlot.Wrist, 50687, gems, enchants, yellow, blue);     // Bloodsunder's Bracers
        RE(c, CharacterSlot.Hands, 51248, gems, enchants, yellow, blue);     // Sanctified Frost Witch's Handguards (Garb)
        RE(c, CharacterSlot.Waist, 50671, gems, enchants, yellow, blue);     // Belt of the Blood Nova
        RE(c, CharacterSlot.Legs, 51246, gems, enchants, yellow, blue);      // Sanctified Frost Witch's Legguards (Garb)
        RE(c, CharacterSlot.Feet, 50652, gems, enchants, yellow, blue);      // Necrophotic Greaves
        RE(c, CharacterSlot.Finger1, 54585, gems, enchants, yellow, blue);   // Ring of Phased Regeneration
        RE(c, CharacterSlot.Finger2, 50610, gems, enchants, yellow, blue);   // Marrowgar's Frigid Eye
        RE(c, CharacterSlot.Trinket1, 50366, gems, enchants, yellow, blue);  // Althor's Abacus (HealingSpellHit proc)
        RE(c, CharacterSlot.Trinket2, 54589, gems, enchants, yellow, blue);  // Glowing Twilight Scale (Use, recursive)
        RE(c, CharacterSlot.MainHand, 50685, gems, enchants, yellow, blue);  // Trauma (277)
        RE(c, CharacterSlot.OffHand, 50616, gems, enchants, yellow, blue);   // Bulwark of Smouldering Steel
        RE(c, CharacterSlot.Ranged, 50464, gems, enchants, yellow, blue);    // Totem of the Surging Sea
    }

    static void RestoGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Default Resto 0/13/55, full raid buffs, 264 mail, haste gems
        {
            Character c = NewResto("RestoRaid264", RestoDefaultSpec(), CharacterRace.Troll, true);
            GearResto264(c, true, true, GEM_HEAL_YELLOW_HASTE, GEM_HEAL_BLUE_MP5);
            CalculationOptionsRestoSham o = new CalculationOptionsRestoSham();
            FinishResto(c, o, Path.Combine(outDir, "resto_264_raid.xml"));
        }

        // 2. Variant spec (different glyphs) + 277 gear, proc trinkets and a totem, tank healing
        {
            ShamanTalents t = RestoDefaultSpec();
            t.GlyphofLesserHealingWave = true;
            t.GlyphofEarthlivingWeapon = true;
            t.GlyphofChainHeal = true;
            Character c = NewResto("RestoProcs277", t, CharacterRace.Draenei, true);
            GearResto277(c, true, true, GEM_HEAL_YELLOW_INT, GEM_HEAL_BLUE_MP5);
            CalculationOptionsRestoSham o = new CalculationOptionsRestoSham();
            o.Targets = "Tank";
            o.BurstStyle = "RT+LHW";
            o.SustStyle = "RT+HW";
            o.Latency = 100f;
            o.Innervates = 1f;
            o.ReplenishmentPercentage = 50f;
            o.SurvivalPerc = 10f;
            FinishResto(c, o, Path.Combine(outDir, "resto_277_procs.xml"));
        }

        // 3. Edge case: no raid buffs, water/earth shield off, long fight, heavy options
        {
            Character c = NewResto("RestoStarved", RestoDefaultSpec(), CharacterRace.Tauren, false);
            GearResto264(c, true, true, GEM_HEAL_YELLOW_HASTE, GEM_HEAL_BLUE_MP5);
            CalculationOptionsRestoSham o = new CalculationOptionsRestoSham();
            o.FightLength = 15f;
            o.ActivityPerc = 55f;
            o.Latency = 200f;
            o.Decurse = 10f;
            o.WSPops = 15f;
            o.Innervates = 3f;
            o.WaterShield = false;
            o.EarthShield = false;
            o.ManaTideEveryCD = false;
            o.ReplenishmentPercentage = 10f;
            o.Targets = "Heavy Raid";
            o.BurstStyle = "RT+CH";
            o.SustStyle = "CH Spam";
            FinishResto(c, o, Path.Combine(outDir, "resto_starved_options.xml"));
        }

        // 4. No Riptide (exercises the Riptide == 0 fallback branches), HW spam on self
        {
            ShamanTalents t = RestoDefaultSpec();
            t.Riptide = 0;
            Character c = NewResto("RestoNoRiptide", t, CharacterRace.Orc, true);
            GearResto264(c, true, true, GEM_HEAL_YELLOW_HASTE, GEM_HEAL_BLUE_MP5);
            CalculationOptionsRestoSham o = new CalculationOptionsRestoSham();
            o.Targets = "Self";
            o.BurstStyle = "HW Spam";
            o.SustStyle = "HW Spam";
            o.WaterShield = true;
            o.WSPops = 5f;
            FinishResto(c, o, Path.Combine(outDir, "resto_norip_hwspam.xml"));
        }
    }

    // ==================================================================
    // Elemental fixture generation (Phase 6 oracle)
    // ==================================================================

    static void ElementalMain(string outDir)
    {
        Calculations.LoadModel(typeof(CalculationsElemental));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        ElementalGenerate(outDir);
    }

    // The shipped DefaultDataFiles/Talents.xml has no elemental shaman spec (only Untalented and
    // Default Resto), so the build is constructed property by property like the Moonkin balance
    // spec. 57/14/0 style with all talents the model reads, plus every elemental glyph.
    static ShamanTalents ElementalTalents()
    {
        ShamanTalents t = new ShamanTalents();
        // Elemental
        t.Convection = 5; t.Concussion = 5; t.CallOfFlame = 3; t.Reverberation = 3;
        t.ElementalFocus = 1; t.ElementalFury = 5; t.ImprovedFireNova = 2; t.CallOfThunder = 5;
        t.UnrelentingStorm = 3; t.ElementalPrecision = 3; t.LightningMastery = 5;
        t.ElementalMastery = 1; t.StormEarthAndFire = 3; t.BoomingEchoes = 2;
        t.ElementalOath = 2; t.LightningOverload = 5; t.TotemOfWrath = 1; t.LavaFlows = 3;
        t.Shamanism = 5; t.Thunderstorm = 1;
        // Enhancement
        t.AncestralKnowledge = 5; t.ThunderingStrikes = 5; t.ElementalWeapons = 3;
        t.MentalQuickness = 3; t.ShamanisticFocus = 1;
        // Restoration
        t.TidalMastery = 5; t.TotemicFocus = 5;
        // glyphs
        t.GlyphofTotemofWrath = true; t.GlyphofFlametongueWeapon = true; t.GlyphofWaterMastery = true;
        t.GlyphofLightningBolt = true; t.GlyphofLava = true; t.GlyphofFlameShock = true;
        t.GlyphofFireNova = true; t.GlyphofChainLightning = true; t.GlyphofThunder = true;
        t.GlyphofFireElementalTotem = true; t.GlyphofShocking = true; t.GlyphofThunderstorm = true;
        return t;
    }

    static string ItemLineEle(Item item)
    {
        int fx = 0;
        foreach (SpecialEffect e in item.Stats.SpecialEffects()) fx++;
        return string.Format(CultureInfo.InvariantCulture,
            "{0} | {1} | ilvl {2} | {3} | set={4} | fx={5} | lsp={6:F0} lvb={7:F0} | {8}",
            item.Id, item.Name, item.ItemLevel, item.Type, item.SetName, fx,
            item.Stats.LightningSpellPower, item.Stats.LavaBurstBonus, StatLine(item.Stats));
    }

    static void ListElemental()
    {
        Console.WriteLine("== ELEMENTAL TALENTS (constructed, no elemental spec shipped) ==");
        PrintTalents("ElementalTalents", ElementalTalents());

        Console.WriteLine();
        Console.WriteLine("== MAIL ARMOR (epic, ilvl>=251, caster, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 251 && i.Type == ItemType.Mail
                    && (i.Stats.SpellPower > 0 || i.Stats.Mp5 > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(14))
                Console.WriteLine(ItemLineEle(item));
        }
        Console.WriteLine("-- MAIL ARMOR 264 (ilvl 258-269, caster) --");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 258 && i.ItemLevel <= 269 && i.Type == ItemType.Mail
                    && (i.Stats.SpellPower > 0 || i.Stats.Mp5 > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(14))
                Console.WriteLine(ItemLineEle(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger, ItemSlot.Trinket })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 258
                    && (i.Stats.SpellPower > 0 || i.Stats.Mp5 > 0 || i.Stats.ManaRestoreFromMaxManaPerSecond > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(18))
                Console.WriteLine(ItemLineEle(item));
        }
        Console.WriteLine("-- Ranged (Totems with elemental stats) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Type == ItemType.Totem && i.Quality == ItemQuality.Epic
                && (i.Stats.LightningSpellPower > 0 || i.Stats.LavaBurstBonus > 0 || i.Stats.SpellPower > 0))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(24))
            Console.WriteLine(ItemLineEle(item));
        Console.WriteLine("-- OffHand (caster held) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.OffHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 258
                && i.Stats.SpellPower > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(18))
            Console.WriteLine(ItemLineEle(item));
        Console.WriteLine("-- MainHand (caster weapons) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.MainHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 258
                && i.Stats.SpellPower > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(18))
            Console.WriteLine(ItemLineEle(item));

        Console.WriteLine();
        Console.WriteLine("== FROST WITCH'S REGALIA SET BONUSES ==");
        foreach (Buff b in Buff.AllBuffs.Where(x => x.SetName == "Frost Witch's Regalia").OrderBy(x => x.Name))
            Console.WriteLine(b.Name + " | threshold=" + b.SetThreshold + " | " + StatLine(b.Stats));
    }

    static int GemForElemental(ItemSlot color, int yellowGem, int blueGem)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_RED;                 // Runed Cardinal Ruby +23 SP
            case ItemSlot.Yellow: return yellowGem;
            case ItemSlot.Blue: return blueGem;
            case ItemSlot.Meta: return 41285;                  // Chaotic Skyflare Diamond
            case ItemSlot.Prismatic: return GEM_PRISMATIC;
            default: return 0;
        }
    }

    static void EE(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants, int yellowGem, int blueGem)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = RequireItem(itemId);
            if (gems)
            {
                g1 = GemForElemental(item.SocketColor1, yellowGem, blueGem);
                g2 = GemForElemental(item.SocketColor2, yellowGem, blueGem);
                g3 = GemForElemental(item.SocketColor3, yellowGem, blueGem);
            }
            if (enchants) ench = EnchForSlot(slot);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    static Character NewElemental(string name, ShamanTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Shaman;
        c.CurrentModel = "Elemental";
        c.ShamanTalents = talents;
        if (raidBuffs)
            ((CalculationsElemental)Calculations.Instance).SetDefaults(c);
        else
            c.ActiveBuffsAdd("Flametongue Totem");
        return c;
    }

    static void FinishElemental(Character c, CalculationOptionsElemental options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    // 277 elemental: 5pc Sanctified Frost Witch's Regalia (T10 heroic, Elemental2T10/4T10) plus
    // ICC 25 caster offpieces, caster stat-proc trinkets, elemental totem.
    static void GearElemental277(Character c, bool gems, bool enchants, int yellow, int blue)
    {
        EE(c, CharacterSlot.Head, 51237, gems, enchants, yellow, blue);      // Sanctified Frost Witch's Helm (Regalia)
        EE(c, CharacterSlot.Neck, 50658, gems, enchants, yellow, blue);      // Amulet of the Silent Eulogy
        EE(c, CharacterSlot.Shoulders, 51235, gems, enchants, yellow, blue); // Sanctified Frost Witch's Shoulderpads (Regalia)
        EE(c, CharacterSlot.Back, 50668, gems, enchants, yellow, blue);      // Greatcloak of the Turned Champion
        EE(c, CharacterSlot.Chest, 51239, gems, enchants, yellow, blue);     // Sanctified Frost Witch's Hauberk (Regalia)
        EE(c, CharacterSlot.Wrist, 50687, gems, enchants, yellow, blue);     // Bloodsunder's Bracers
        EE(c, CharacterSlot.Hands, 51238, gems, enchants, yellow, blue);     // Sanctified Frost Witch's Gloves (Regalia)
        EE(c, CharacterSlot.Waist, 50671, gems, enchants, yellow, blue);     // Belt of the Blood Nova
        EE(c, CharacterSlot.Legs, 51236, gems, enchants, yellow, blue);      // Sanctified Frost Witch's Kilt (Regalia)
        EE(c, CharacterSlot.Feet, 50652, gems, enchants, yellow, blue);      // Necrophotic Greaves
        EE(c, CharacterSlot.Finger1, 50614, gems, enchants, yellow, blue);   // Loop of the Endless Labyrinth
        EE(c, CharacterSlot.Finger2, 50400, gems, enchants, yellow, blue);   // Ashen Band of Endless Wisdom
        EE(c, CharacterSlot.Trinket1, 50348, gems, enchants, yellow, blue);  // Dislodged Foreign Object (SpellPower proc)
        EE(c, CharacterSlot.Trinket2, 50345, gems, enchants, yellow, blue);  // Muradin's Spyglass (CritRating proc)
        EE(c, CharacterSlot.MainHand, 50734, gems, enchants, yellow, blue);  // Royal Scepter of Terenas II (mace)
        EE(c, CharacterSlot.OffHand, 50719, gems, enchants, yellow, blue);   // Shadow Silk Spindle (held)
        EE(c, CharacterSlot.Ranged, 50458, gems, enchants, yellow, blue);    // Bizuri's Totem of Shattered Ice
    }

    // 264 elemental: 5pc Sanctified Frost Witch's Regalia (T10 normal) plus ICC caster offpieces,
    // a Use trinket and an Mp5 proc trinket, elemental totem.
    static void GearElemental264(Character c, bool gems, bool enchants, int yellow, int blue)
    {
        EE(c, CharacterSlot.Head, 51202, gems, enchants, yellow, blue);      // Sanctified Frost Witch's Helm (Regalia)
        EE(c, CharacterSlot.Neck, 50005, gems, enchants, yellow, blue);      // Amulet of the Silent Eulogy
        EE(c, CharacterSlot.Shoulders, 51204, gems, enchants, yellow, blue); // Sanctified Frost Witch's Shoulderpads (Regalia)
        EE(c, CharacterSlot.Back, 50014, gems, enchants, yellow, blue);      // Greatcloak of the Turned Champion
        EE(c, CharacterSlot.Chest, 51200, gems, enchants, yellow, blue);     // Sanctified Frost Witch's Hauberk (Regalia)
        EE(c, CharacterSlot.Wrist, 50030, gems, enchants, yellow, blue);     // Bloodsunder's Bracers
        EE(c, CharacterSlot.Hands, 51201, gems, enchants, yellow, blue);     // Sanctified Frost Witch's Gloves (Regalia)
        EE(c, CharacterSlot.Waist, 50015, gems, enchants, yellow, blue);     // Belt of the Blood Nova
        EE(c, CharacterSlot.Legs, 51203, gems, enchants, yellow, blue);      // Sanctified Frost Witch's Kilt (Regalia)
        EE(c, CharacterSlot.Feet, 49993, gems, enchants, yellow, blue);      // Necrophotic Greaves
        EE(c, CharacterSlot.Finger1, 50174, gems, enchants, yellow, blue);   // Incarnadine Band of Mending
        EE(c, CharacterSlot.Finger2, 50400, gems, enchants, yellow, blue);   // Ashen Band of Endless Wisdom
        EE(c, CharacterSlot.Trinket1, 48724, gems, enchants, yellow, blue);  // Talisman of Resurgence (Use)
        EE(c, CharacterSlot.Trinket2, 47041, gems, enchants, yellow, blue);  // Solace of the Defeated (Mp5 proc)
        EE(c, CharacterSlot.MainHand, 49968, gems, enchants, yellow, blue);  // Frozen Bonespike (dagger)
        EE(c, CharacterSlot.OffHand, 50173, gems, enchants, yellow, blue);   // Shadow Silk Spindle (held)
        EE(c, CharacterSlot.Ranged, 50458, gems, enchants, yellow, blue);    // Bizuri's Totem of Shattered Ice
    }

    static void ElementalGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Full raid buffs, 264 T10 Regalia 5pc, gems+enchants (sp/haste, sp/mp5), default options
        {
            Character c = NewElemental("ElementalRaid264", ElementalTalents(), CharacterRace.Tauren, true);
            GearElemental264(c, true, true, 40155, 40134);
            CalculationOptionsElemental o = new CalculationOptionsElemental();
            FinishElemental(c, o, Path.Combine(outDir, "elemental_264_raid.xml"));
        }

        // 2. 277 heroic T10 Regalia 5pc + stat-proc trinkets, gems+enchants (sp/hit cap mix)
        {
            Character c = NewElemental("ElementalProcs277", ElementalTalents(), CharacterRace.Orc, true);
            GearElemental277(c, true, true, 40153, 40134);
            CalculationOptionsElemental o = new CalculationOptionsElemental();
            o.BSRatio = 50;
            o.UseFireEle = true;
            FinishElemental(c, o, Path.Combine(outDir, "elemental_277_procs.xml"));
        }

        // 3. No raid buffs, 264 gear, short fight, burst only, no CL/FN, dps fire totem on
        {
            Character c = NewElemental("ElementalNoBuffs60", ElementalTalents(), CharacterRace.Troll, false);
            GearElemental264(c, true, true, 40155, 40133);
            CalculationOptionsElemental o = new CalculationOptionsElemental();
            o.FightDuration = 60;
            o.BSRatio = 100;
            o.UseFireNova = false;
            o.UseChainLightning = false;
            o.UseDpsTotem = true;
            o.UseThunderstorm = false;
            FinishElemental(c, o, Path.Combine(outDir, "elemental_nobuffs_short.xml"));
        }

        // 4. Mana-starved long fight (600s), mana gems (mp5), multi-target CL/FN, latency heavy
        {
            Character c = NewElemental("ElementalStarved600", ElementalTalents(), CharacterRace.Draenei, true);
            GearElemental264(c, true, true, 40155, 40134);
            CalculationOptionsElemental o = new CalculationOptionsElemental();
            o.FightDuration = 600;
            o.BSRatio = 25;
            o.NumberOfTargets = 3;
            o.LatencyGcd = 0.3f;
            o.LatencyCast = 0.15f;
            o.UseDpsTotem = true;
            o.UseFireEle = true;
            FinishElemental(c, o, Path.Combine(outDir, "elemental_starved_long.xml"));
        }

        // 5. Alternate build: no glyphs, no Lava Flows/Shamanism/Tidal Mastery, UF/ToW off,
        //    exercises the no-glyph/zero-talent branches, 277 gear, use-on-CD trinkets
        {
            ShamanTalents t = ElementalTalents();
            t.GlyphofLightningBolt = false; t.GlyphofLava = false; t.GlyphofFlameShock = false;
            t.GlyphofChainLightning = false; t.GlyphofFireNova = false; t.GlyphofShocking = false;
            t.GlyphofFlametongueWeapon = false;
            t.LavaFlows = 0; t.Shamanism = 0; t.TidalMastery = 0; t.ElementalOath = 0;
            t.TotemOfWrath = 0; t.GlyphofTotemofWrath = false; t.BoomingEchoes = 0;
            t.StormEarthAndFire = 0; t.ImprovedFireNova = 0; t.Thunderstorm = 0;
            t.GlyphofThunder = false; t.GlyphofThunderstorm = false;
            Character c = NewElemental("ElementalNoGlyphs277", t, CharacterRace.Orc, true);
            GearElemental277(c, true, true, 40155, 40133);
            EE(c, CharacterSlot.Trinket1, 50358, true, true, 40155, 40133);  // Purified Lunar Dust proc
            EE(c, CharacterSlot.Trinket2, 54589, true, true, 40155, 40133);  // Glowing Twilight Scale (Use)
            CalculationOptionsElemental o = new CalculationOptionsElemental();
            o.UseFireNova = false;
            o.UseChainLightning = false;
            o.UseDpsTotem = false;
            FinishElemental(c, o, Path.Combine(outDir, "elemental_noglyphs_277.xml"));
        }
    }

    // ==================================================================
    // Cat fixture generation (Phase 6 oracle)
    // ==================================================================

    static void CatMain(string outDir)
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Calculations.LoadModel(typeof(CalculationsCat));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        CatGenerate(outDir);
    }

    static SavedTalentSpec CatSpec(string name)
    {
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Druid))
            if (spec.Name == name) return spec;
        throw new InvalidOperationException("druid spec not found: " + name);
    }

    static void ListCat()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== DRUID TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Druid))
        {
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));
            TalentsBase t = spec.TalentSpec();
            if (t != null && spec.Name == "Cat") PrintTalents(spec.Name, t);
        }

        Console.WriteLine();
        Console.WriteLine("== CAT ENCHANT SPOT CHECK ==");
        int[] ids = new int[] { 3817, 3808, 1099, 3832, 3845, 1603, 3823, 1597, 3827 };
        foreach (Enchant e in Enchant.AllEnchants.Where(x => ids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | slot={2} | {3}", e.Id, e.Name, e.Slot, e.Stats == null ? "" : StatLine(e.Stats)));

        Console.WriteLine();
        Console.WriteLine("== CAT GEM SPOT CHECK ==");
        int[] gids = new int[] { 40112, 40147, 40130, 41398, 49110 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLine(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== LEATHER AGI/ARP ITEMS (epic, ilvl>=264, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264 && i.Type == ItemType.Leather
                    && i.Stats.Agility > 0)
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(14))
                Console.WriteLine(ItemLine(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger, ItemSlot.Trinket })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                    && (i.Stats.Agility > 0 || i.Stats.ArmorPenetrationRating > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(14))
                Console.WriteLine(ItemLine(item));
        }
        Console.WriteLine("-- TwoHand (agi weapons) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.TwoHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264 && i.Stats.Agility > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- Ranged (Idols) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Quality == ItemQuality.Epic)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine(item));
    }

    static int GemForCat(ItemSlot color)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_CAT_RED;
            case ItemSlot.Yellow: return GEM_CAT_YELLOW;
            case ItemSlot.Blue: return GEM_CAT_BLUE;
            case ItemSlot.Meta: return GEM_CAT_META;
            case ItemSlot.Prismatic: return GEM_CAT_PRISMATIC;
            default: return 0;
        }
    }

    static int EnchForSlotCat(CharacterSlot slot)
    {
        switch (slot)
        {
            case CharacterSlot.Head: return ENCH_CAT_HEAD;
            case CharacterSlot.Shoulders: return ENCH_CAT_SHOULDER;
            case CharacterSlot.Back: return ENCH_CAT_BACK;
            case CharacterSlot.Chest: return ENCH_CAT_CHEST;
            case CharacterSlot.Wrist: return ENCH_CAT_WRIST;
            case CharacterSlot.Hands: return ENCH_CAT_HANDS;
            case CharacterSlot.Legs: return ENCH_CAT_LEGS;
            case CharacterSlot.Feet: return ENCH_CAT_FEET;
            case CharacterSlot.MainHand: return ENCH_CAT_WEAPON;
            default: return 0;
        }
    }

    static void CE(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = RequireItem(itemId);
            if (gems)
            {
                g1 = GemForCat(item.SocketColor1);
                g2 = GemForCat(item.SocketColor2);
                g3 = GemForCat(item.SocketColor3);
            }
            if (enchants) ench = EnchForSlotCat(slot);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    // 277 feral Cat: 5pc Sanctified Lasherweave Battlegear (T10) + heroic ICC 25 offpieces,
    // proc trinkets (Deathbringer's Will / Sharpened Twilight Scale), Idol of the Crying Moon.
    static void GearCat277(Character c, bool gems, bool enchants)
    {
        CE(c, CharacterSlot.Head, 51296, gems, enchants);        // Sanctified Lasherweave Headguard
        CE(c, CharacterSlot.Neck, 50633, gems, enchants);        // Sindragosa's Cruel Claw
        CE(c, CharacterSlot.Shoulders, 51299, gems, enchants);   // Sanctified Lasherweave Shoulderpads
        CE(c, CharacterSlot.Back, 50653, gems, enchants);        // Shadowvault Slayer's Cloak
        CE(c, CharacterSlot.Chest, 51298, gems, enchants);       // Sanctified Lasherweave Raiment
        CE(c, CharacterSlot.Wrist, 50670, gems, enchants);       // Toskk's Maximized Wristguards
        CE(c, CharacterSlot.Hands, 51295, gems, enchants);       // Sanctified Lasherweave Handgrips
        CE(c, CharacterSlot.Waist, 50707, gems, enchants);       // Astrylian's Sutured Cinch
        CE(c, CharacterSlot.Legs, 51297, gems, enchants);        // Sanctified Lasherweave Legguards
        CE(c, CharacterSlot.Feet, 50607, gems, enchants);        // Frostbitten Fur Boots
        CE(c, CharacterSlot.Finger1, 50618, gems, enchants);     // Frostbrood Sapphire Ring
        CE(c, CharacterSlot.Finger2, 50678, gems, enchants);     // Seal of Many Mouths
        CE(c, CharacterSlot.Trinket1, 50363, gems, enchants);    // Deathbringer's Will (proc)
        CE(c, CharacterSlot.Trinket2, 54590, gems, enchants);    // Sharpened Twilight Scale (proc ArP)
        CE(c, CharacterSlot.MainHand, 50695, gems, enchants);    // Distant Land (277 staff)
        CE(c, CharacterSlot.Ranged, 50456, gems, enchants);      // Idol of the Crying Moon (RakeTick)
    }

    // 264 feral Cat: 5pc Sanctified Lasherweave Battlegear (normal T10) + ICC 25 offpieces.
    static void GearCat264(Character c, bool gems, bool enchants)
    {
        CE(c, CharacterSlot.Head, 51143, gems, enchants);        // Sanctified Lasherweave Headguard
        CE(c, CharacterSlot.Neck, 50421, gems, enchants);        // Sindragosa's Cruel Claw
        CE(c, CharacterSlot.Shoulders, 51140, gems, enchants);   // Sanctified Lasherweave Shoulderpads
        CE(c, CharacterSlot.Back, 49998, gems, enchants);        // Shadowvault Slayer's Cloak
        CE(c, CharacterSlot.Chest, 51141, gems, enchants);       // Sanctified Lasherweave Raiment
        CE(c, CharacterSlot.Wrist, 50333, gems, enchants);       // Toskk's Maximized Wristguards
        CE(c, CharacterSlot.Hands, 51144, gems, enchants);       // Sanctified Lasherweave Handgrips
        CE(c, CharacterSlot.Waist, 50067, gems, enchants);       // Astrylian's Sutured Cinch
        CE(c, CharacterSlot.Legs, 51142, gems, enchants);        // Sanctified Lasherweave Legguards
        CE(c, CharacterSlot.Feet, 49950, gems, enchants);        // Frostbitten Fur Boots
        CE(c, CharacterSlot.Finger1, 50186, gems, enchants);     // Frostbrood Sapphire Ring
        CE(c, CharacterSlot.Finger2, 50025, gems, enchants);     // Seal of Many Mouths
        CE(c, CharacterSlot.Trinket1, 50343, gems, enchants);    // Whispering Fanged Skull (proc)
        CE(c, CharacterSlot.Trinket2, 50362, gems, enchants);    // Deathbringer's Will (proc)
        CE(c, CharacterSlot.MainHand, 50040, gems, enchants);    // Distant Land (264 staff)
        CE(c, CharacterSlot.Ranged, 50456, gems, enchants);      // Idol of the Crying Moon (RakeTick)
    }

    static DruidTalents CatDefaultSpec()
    {
        TalentsBase t = CatSpec("Cat").TalentSpec();
        if (t == null) throw new InvalidOperationException("Cat spec failed to parse");
        return (DruidTalents)t;
    }

    static Character NewCat(string name, DruidTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Druid;
        c.CurrentModel = "Cat";
        c.DruidTalents = talents;
        if (raidBuffs)
            ((CalculationsCat)Calculations.Instance).SetDefaults(c);
        return c;
    }

    static void FinishCat(Character c, CalculationOptionsCat options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    static void CatGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Cat 0/54/17, full raid buffs, 277 T10 5pc + proc trinkets, gems+enchants, 300s
        {
            Character c = NewCat("CatRaid277", CatDefaultSpec(), CharacterRace.NightElf, true);
            GearCat277(c, true, true);
            CalculationOptionsCat o = new CalculationOptionsCat();
            FinishCat(c, o, Path.Combine(outDir, "cat_277_raid.xml"));
        }

        // 2. Same spec, 264 normal T10 5pc, no raid buffs (only the glyphs on the spec)
        {
            Character c = NewCat("CatNoBuffs264", CatDefaultSpec(), CharacterRace.Tauren, false);
            GearCat264(c, true, true);
            CalculationOptionsCat o = new CalculationOptionsCat();
            FinishCat(c, o, Path.Combine(outDir, "cat_264_nobuffs.xml"));
        }

        // 3. Short fight (60s) against a level 80 target with the level-80 armor value
        {
            Character c = NewCat("CatShort80", CatDefaultSpec(), CharacterRace.NightElf, true);
            GearCat264(c, true, true);
            CalculationOptionsCat o = new CalculationOptionsCat();
            o.Duration = 60;
            o.TargetLevel = 80;
            o.TargetArmor = (int)StatConversion.NPC_ARMOR[0];
            FinishCat(c, o, Path.Combine(outDir, "cat_264_short80.xml"));
        }

        // 4. 2pc T10 with offpieces + trinket offset + custom rotation options (long fight)
        {
            DruidTalents t = CatDefaultSpec();
            Character c = NewCat("CatProcsCustom", t, CharacterRace.Troll, true);
            GearCat277(c, true, true);
            CE(c, CharacterSlot.Shoulders, 50646, true, true);   // Cultist's Bloodsoaked Spaulders
            CE(c, CharacterSlot.Hands, 50675, true, true);       // Aldriana's Gloves of Secrecy
            CE(c, CharacterSlot.Legs, 50697, true, true);        // Gangrenous Leggings
            CalculationOptionsCat o = new CalculationOptionsCat();
            o.Duration = 600;
            o.TrinketOffset = 20f;
            o.CustomUseRake = true;
            o.CustomUseShred = true;
            o.CustomUseRip = true;
            o.CustomCPFerociousBite = 0;
            o.CustomCPSavageRoar = 5;
            FinishCat(c, o, Path.Combine(outDir, "cat_277_procs_custom.xml"));
        }
    }

    // ==================================================================
    // Bear fixture generation (Phase 6 oracle)
    // ==================================================================

    static void BearMain(string outDir)
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Calculations.LoadModel(typeof(CalculationsBear));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        BearGenerate(outDir);
    }

    static SavedTalentSpec BearSpec(string name)
    {
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Druid))
            if (spec.Name == name) return spec;
        throw new InvalidOperationException("druid spec not found: " + name);
    }

    static void ListBear()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== DRUID TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Druid))
        {
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));
            TalentsBase t = spec.TalentSpec();
            if (t != null && spec.Name == "Bear") PrintTalents(spec.Name, t);
        }

        Console.WriteLine();
        Console.WriteLine("== BEAR ENCHANT SPOT CHECK ==");
        int[] ids = new int[] { 3818, 3812, 3852, 3832, 3850, 3231, 3822, 3232, 2673, 3817, 3808, 3836, 3837, 3838, 1597, 1603 };
        foreach (Enchant e in Enchant.AllEnchants.Where(x => ids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | slot={2} | {3}", e.Id, e.Name, e.Slot, e.Stats == null ? "" : StatLine(e.Stats)));

        Console.WriteLine();
        Console.WriteLine("== BEAR GEM SPOT CHECK ==");
        int[] gids = new int[] { 40112, 40148, 40160, 40130, 40119, 41380, 49110, 40147, 40155, 40133 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLine(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== LEATHER TANK ITEMS (epic, ilvl>=264, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264 && i.Type == ItemType.Leather
                    && i.Stats.Stamina > 0)
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
                Console.WriteLine(ItemLine2(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger, ItemSlot.Trinket })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                    && (i.Stats.Stamina > 0 || i.Stats.DodgeRating > 0 || i.Stats.Armor > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
                Console.WriteLine(ItemLine2(item));
        }
        Console.WriteLine("-- TwoHand (agi/stamina weapons) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.TwoHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && (i.Stats.Agility > 0 || i.Stats.Stamina > 0))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine2(item));
        Console.WriteLine("-- Ranged (Idols) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Quality == ItemQuality.Epic)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine2(item));
    }

    static string ItemLine2(Item item)
    {
        return string.Format(CultureInfo.InvariantCulture, "{0} | {1} | ilvl {2} | {3} | set={4} | armor={5:F0} sta={6:F0} agi={7:F0} dodge={8:F0} def={9:F0} hit={10:F0} exp={11:F0}",
            item.Id, item.Name, item.ItemLevel, item.Type, item.SetName, item.Stats.Armor, item.Stats.Stamina,
            item.Stats.Agility, item.Stats.DodgeRating, item.Stats.DefenseRating, item.Stats.HitRating, item.Stats.ExpertiseRating);
    }

    static int GemForBear(ItemSlot color, bool survival)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_BEAR_RED;
            case ItemSlot.Yellow: return survival ? GEM_BEAR_YELLOW_STA : GEM_BEAR_YELLOW;
            case ItemSlot.Blue: return survival ? GEM_BEAR_BLUE_STA : GEM_BEAR_BLUE;
            case ItemSlot.Meta: return GEM_BEAR_META;
            case ItemSlot.Prismatic: return GEM_BEAR_PRISMATIC;
            default: return 0;
        }
    }

    static int EnchForSlotBear(CharacterSlot slot)
    {
        switch (slot)
        {
            case CharacterSlot.Head: return ENCH_BEAR_HEAD;
            case CharacterSlot.Shoulders: return ENCH_BEAR_SHOULDER;
            case CharacterSlot.Back: return ENCH_BEAR_BACK;
            case CharacterSlot.Chest: return ENCH_BEAR_CHEST;
            case CharacterSlot.Wrist: return ENCH_BEAR_WRIST;
            case CharacterSlot.Hands: return ENCH_BEAR_HANDS;
            case CharacterSlot.Legs: return ENCH_BEAR_LEGS;
            case CharacterSlot.Feet: return ENCH_BEAR_FEET;
            case CharacterSlot.MainHand: return ENCH_BEAR_WEAPON;
            default: return 0;
        }
    }

    static void BE(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants, bool survival)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = RequireItem(itemId);
            if (gems)
            {
                g1 = GemForBear(item.SocketColor1, survival);
                g2 = GemForBear(item.SocketColor2, survival);
                g3 = GemForBear(item.SocketColor3, survival);
            }
            if (enchants) ench = EnchForSlotBear(slot);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    static DruidTalents BearDefaultSpec()
    {
        TalentsBase t = BearSpec("Bear").TalentSpec();
        if (t == null) throw new InvalidOperationException("Bear spec failed to parse");
        return (DruidTalents)t;
    }

    static Character NewBear(string name, DruidTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Druid;
        c.CurrentModel = "Bear";
        c.DruidTalents = talents;
        if (raidBuffs)
            ((CalculationsBear)Calculations.Instance).SetDefaults(c);
        return c;
    }

    static void FinishBear(Character c, CalculationOptionsBear options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    // 277 feral-tank: 5pc Sanctified Lasherweave Battlegear (T10) + tank offpieces,
    // tank trinkets (Sindragosa's Flawless Fang / Corpse Tongue Coin) and the Mangle idol.
    static void GearBear277(Character c, bool gems, bool enchants, bool survival)
    {
        BE(c, CharacterSlot.Head, 51296, gems, enchants, survival);       // Sanctified Lasherweave Headguard
        BE(c, CharacterSlot.Neck, 50682, gems, enchants, survival);       // Bile-Encrusted Medallion
        BE(c, CharacterSlot.Shoulders, 51299, gems, enchants, survival);  // Sanctified Lasherweave Shoulderpads
        BE(c, CharacterSlot.Back, 50718, gems, enchants, survival);       // Royal Crimson Cloak
        BE(c, CharacterSlot.Chest, 51298, gems, enchants, survival);      // Sanctified Lasherweave Raiment
        BE(c, CharacterSlot.Wrist, 50670, gems, enchants, survival);      // Toskk's Maximized Wristguards
        BE(c, CharacterSlot.Hands, 51295, gems, enchants, survival);      // Sanctified Lasherweave Handgrips
        BE(c, CharacterSlot.Waist, 50707, gems, enchants, survival);      // Astrylian's Sutured Cinch
        BE(c, CharacterSlot.Legs, 51297, gems, enchants, survival);       // Sanctified Lasherweave Legguards
        BE(c, CharacterSlot.Feet, 50607, gems, enchants, survival);       // Frostbitten Fur Boots
        BE(c, CharacterSlot.Finger1, 50642, gems, enchants, survival);    // Juggernaut Band
        BE(c, CharacterSlot.Finger2, 50404, gems, enchants, survival);    // Ashen Band of Endless Courage
        BE(c, CharacterSlot.Trinket1, 50364, gems, enchants, survival);   // Sindragosa's Flawless Fang
        BE(c, CharacterSlot.Trinket2, 50349, gems, enchants, survival);   // Corpse Tongue Coin (DamageTakenPhysical proc)
        BE(c, CharacterSlot.MainHand, 50695, gems, enchants, survival);   // Distant Land (277 staff)
        BE(c, CharacterSlot.Ranged, 45509, gems, enchants, survival);     // Idol of the Corruptor (MangleBearHit proc)
    }

    // 264 feral-tank: 5pc Sanctified Lasherweave Battlegear (normal T10) + ICC 25 offpieces.
    static void GearBear264(Character c, bool gems, bool enchants, bool survival)
    {
        BE(c, CharacterSlot.Head, 51143, gems, enchants, survival);       // Sanctified Lasherweave Headguard
        BE(c, CharacterSlot.Neck, 50023, gems, enchants, survival);       // Bile-Encrusted Medallion
        BE(c, CharacterSlot.Shoulders, 51140, gems, enchants, survival);  // Sanctified Lasherweave Shoulderpads
        BE(c, CharacterSlot.Back, 49998, gems, enchants, survival);       // Shadowvault Slayer's Cloak
        BE(c, CharacterSlot.Chest, 51141, gems, enchants, survival);      // Sanctified Lasherweave Raiment
        BE(c, CharacterSlot.Wrist, 50333, gems, enchants, survival);      // Toskk's Maximized Wristguards
        BE(c, CharacterSlot.Hands, 51144, gems, enchants, survival);      // Sanctified Lasherweave Handgrips
        BE(c, CharacterSlot.Waist, 50067, gems, enchants, survival);      // Astrylian's Sutured Cinch
        BE(c, CharacterSlot.Legs, 51142, gems, enchants, survival);       // Sanctified Lasherweave Legguards
        BE(c, CharacterSlot.Feet, 49950, gems, enchants, survival);       // Frostbitten Fur Boots
        BE(c, CharacterSlot.Finger1, 50186, gems, enchants, survival);    // Frostbrood Sapphire Ring
        BE(c, CharacterSlot.Finger2, 50025, gems, enchants, survival);    // Seal of Many Mouths
        BE(c, CharacterSlot.Trinket1, 50356, gems, enchants, survival);   // Corroded Skeleton Key
        BE(c, CharacterSlot.Trinket2, 50361, gems, enchants, survival);   // Sindragosa's Flawless Fang
        BE(c, CharacterSlot.MainHand, 50040, gems, enchants, survival);   // Distant Land (264 staff)
        BE(c, CharacterSlot.Ranged, 45509, gems, enchants, survival);     // Idol of the Corruptor (MangleBearHit proc)
    }

    static void BearGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);
        // 1. Bear 0/60/11, full raid buffs, 277 T10 5pc + tank trinkets, gems+enchants
        {
            Character c = NewBear("BearRaid277", BearDefaultSpec(), CharacterRace.NightElf, true);
            GearBear277(c, true, true, false);
            CalculationOptionsBear o = new CalculationOptionsBear();
            FinishBear(c, o, Path.Combine(outDir, "bear_277_raid.xml"));
        }

        // 2. Same spec, 264 normal T10 5pc, no raid buffs (no glyphs either), survival gemming
        {
            Character c = NewBear("BearNoBuffs264", BearDefaultSpec(), CharacterRace.Tauren, false);
            GearBear264(c, true, true, true);
            CalculationOptionsBear o = new CalculationOptionsBear();
            FinishBear(c, o, Path.Combine(outDir, "bear_264_nobuffs.xml"));
        }

        // 3. 277 gear with swap-in proc trinkets (Deathbringer's Will) + custom rotation options
        {
            DruidTalents t = BearDefaultSpec();
            t.GlyphOfMaul = true;
            t.GlyphOfGrowl = true;
            t.GlyphOfFrenziedRegeneration = true;
            t.GlyphOfMangle = true;
            Character c = NewBear("BearProcsCustom", t, CharacterRace.Troll, true);
            GearBear277(c, true, true, false);
            BE(c, CharacterSlot.Trinket1, 50363, true, true, false);      // Deathbringer's Will (PhysicalHit proc)
            BE(c, CharacterSlot.Trinket2, 50349, true, true, false);      // Corpse Tongue Coin
            CalculationOptionsBear o = new CalculationOptionsBear();
            o.CustomUseMaul = true;
            o.CustomUseMangle = true;
            o.CustomUseSwipe = true;
            o.CustomUseFaerieFire = true;
            o.CustomUseLacerate = true;
            o.ThreatScale = 20f;
            FinishBear(c, o, Path.Combine(outDir, "bear_277_procs_custom.xml"));
        }

        // 4. Level-80 target: different hit/dodge/parry caps and no parry haste
        {
            Character c = NewBear("BearTarget80", BearDefaultSpec(), CharacterRace.NightElf, true);
            GearBear277(c, true, true, false);
            CalculationOptionsBear o = new CalculationOptionsBear();
            o.TargetLevel = 80;
            o.TargetDamage = 100000;
            o.TargetParryHastes = false;
            o.TargetAttackSpeed = 1.8f;
            FinishBear(c, o, Path.Combine(outDir, "bear_target80.xml"));
        }

        // 5. Survival soft cap + temporary survival scaling edge case
        {
            Character c = NewBear("BearSoftCap", BearDefaultSpec(), CharacterRace.Tauren, true);
            GearBear277(c, true, true, true);
            CalculationOptionsBear o = new CalculationOptionsBear();
            o.SurvivalSoftCap = 100000;
            o.TemporarySurvivalScale = 0.5f;
            FinishBear(c, o, Path.Combine(outDir, "bear_survival_softcap.xml"));
        }
    }

    // ==================================================================
    // Healadin fixture generation (Phase 6 oracle)
    // ==================================================================

    static void HealadinMain(string outDir)
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Calculations.LoadModel(typeof(CalculationsHealadin));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        HealadinGenerate(outDir);
    }

    static SavedTalentSpec HealadinSpec(string name)
    {
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Paladin))
            if (spec.Name == name) return spec;
        throw new InvalidOperationException("paladin spec not found: " + name);
    }

    static void ListHealadin()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== PALADIN TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Paladin))
        {
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));
            TalentsBase t = spec.TalentSpec();
            if (t != null) PrintTalents(spec.Name, t);
        }

        Console.WriteLine();
        Console.WriteLine("== HEALADIN ENCHANT SPOT CHECK ==");
        int[] ids = new int[] { 3820, 3810, 3728, 2661, 2332, 3246, 3719, 3826, 3834, 3854, 3722, 3871, 3096, 3731, 3855 };
        foreach (Enchant e in Enchant.AllEnchants.Where(x => ids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | slot={2} | {3}", e.Id, e.Name, e.Slot, e.Stats == null ? "" : StatLine(e.Stats)));

        Console.WriteLine();
        Console.WriteLine("== HEALADIN GEM SPOT CHECK ==");
        int[] gids = new int[] { 40113, 40151, 40155, 40175, 40133, 41401, 41376, 49110 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLine(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== PLATE HEALER ITEMS (epic, ilvl>=245, sp>0, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245 && i.Type == ItemType.Plate
                    && i.Stats.SpellPower > 0)
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
                Console.WriteLine(ItemLine(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger, ItemSlot.Trinket })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245
                    && (i.Stats.SpellPower > 0 || i.Stats.Mp5 > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
                Console.WriteLine(ItemLine(item));
        }
        Console.WriteLine("-- OffHand (Shields) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.OffHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245
                && i.Type == ItemType.Shield && i.Stats.SpellPower > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- MainHand (one-hand caster) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.MainHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245
                && (i.Type == ItemType.OneHandMace || i.Type == ItemType.OneHandAxe || i.Type == ItemType.OneHandSword)
                && i.Stats.SpellPower > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- Ranged (Librams) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Quality == ItemQuality.Epic)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine(item));
    }

    static Character NewHealadin(string name, PaladinTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Paladin;
        c.CurrentModel = "Healadin";
        c.PaladinTalents = talents;
        if (raidBuffs)
            ((CalculationsHealadin)Calculations.Instance).SetDefaults(c);
        return c;
    }

    static void FinishHealadin(Character c, CalculationOptionsHealadin options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    static void HealadinGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Holy (Sanctity of Battle) spec, raid buffs, 264 Lightsworn Garb, gems+enchants
        {
            Character c = NewHealadin("HealadinRaid264", HealadinDefaultSpec(), CharacterRace.Human, true);
            GearHealadin264(c, true, true, GEM_HEAL_YELLOW_HASTE);
            CalculationOptionsHealadin o = new CalculationOptionsHealadin();
            FinishHealadin(c, o, Path.Combine(outDir, "healadin_264_raid.xml"));
        }

        // 2. Holy (Divine Guardian) spec + extra glyphs, 277 gear, proc trinkets, options
        {
            PaladinTalents t = HealadinDefaultSpec();
            t.GlyphOfHolyShock = true;
            t.GlyphOfFlashOfLight = true;
            t.GlyphOfBeaconOfLight = true;
            Character c = NewHealadin("HealadinProcs277", t, CharacterRace.Draenei, true);
            GearHealadin277(c, true, true, GEM_HEAL_YELLOW_INT);
            CalculationOptionsHealadin o = new CalculationOptionsHealadin();
            o.BurstScale = 0.5f;
            o.SSUptime = 0.8f;
            o.FoLOnTank = 0.5f;
            o.GHL_Targets = 3f;
            o.IoLHolyLight = 0.5f;
            o.HolyShock = 0.25f;
            o.LoHSelf = true;
            FinishHealadin(c, o, Path.Combine(outDir, "healadin_277_procs.xml"));
        }

        // 3. No raid buffs (only the saved glyphs), 264 gear, Intellect gemming
        {
            Character c = NewHealadin("HealadinNoBuffs264", HealadinSpec("Holy: Divinity").TalentSpec() as PaladinTalents, CharacterRace.Tauren, false);
            GearHealadin264(c, true, true, GEM_HEAL_YELLOW_INT);
            CalculationOptionsHealadin o = new CalculationOptionsHealadin();
            FinishHealadin(c, o, Path.Combine(outDir, "healadin_264_nobuffs.xml"));
        }

        // 4. Long mana-starved fight: 20 min, low activity/replenishment, mp5 gemming
        {
            Character c = NewHealadin("HealadinManaStarved", HealadinDefaultSpec(), CharacterRace.BloodElf, false);
            GearHealadin264(c, true, true, GEM_HEAL_YELLOW_HASTE);
            CalculationOptionsHealadin o = new CalculationOptionsHealadin();
            o.Length = 20f;
            o.Activity = 0.75f;
            o.Replenishment = 0.25f;
            o.SSUptime = 0.0f;
            FinishHealadin(c, o, Path.Combine(outDir, "healadin_manastarved.xml"));
        }

        // 5. Burst tank healing: short 3 min fight, FoL-on-tank, high burst scale, no JotP
        {
            Character c = NewHealadin("HealadinBurstTank", HealadinDefaultSpec(), CharacterRace.Human, true);
            GearHealadin277(c, true, true, GEM_HEAL_YELLOW_HASTE);
            CalculationOptionsHealadin o = new CalculationOptionsHealadin();
            o.Length = 3f;
            o.BurstScale = 0.8f;
            o.FoLOnTank = 1.0f;
            o.SSUptime = 1.0f;
            o.GHL_Targets = 1f;
            o.IoLHolyLight = 0.1f;
            o.JotP = false;
            FinishHealadin(c, o, Path.Combine(outDir, "healadin_burst_tank.xml"));
        }
    }

    static PaladinTalents HealadinDefaultSpec()
    {
        TalentsBase t = HealadinSpec("Holy: Sanctity of Battle").TalentSpec();
        if (t == null) throw new InvalidOperationException("Holy: Sanctity of Battle spec failed to parse");
        return (PaladinTalents)t;
    }

    // 264 holy paladin: 5pc Lightsworn Garb (T10 normal) + ICC 25 offpieces, caster libram.
    static void GearHealadin264(Character c, bool gems, bool enchants, int yellowGem)
    {
        RE(c, CharacterSlot.Head, 51167, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);      // Sanctified Lightsworn Headpiece
        RE(c, CharacterSlot.Neck, 50005, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);      // Amulet of the Silent Eulogy
        RE(c, CharacterSlot.Shoulders, 51166, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5); // Sanctified Lightsworn Spaulders
        RE(c, CharacterSlot.Back, 50014, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);      // Greatcloak of the Turned Champion
        RE(c, CharacterSlot.Chest, 51165, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);     // Sanctified Lightsworn Tunic
        RE(c, CharacterSlot.Wrist, 50175, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);     // Crypt Keeper's Bracers
        RE(c, CharacterSlot.Hands, 51169, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);     // Sanctified Lightsworn Gloves
        RE(c, CharacterSlot.Waist, 50010, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);     // Waistband of Righteous Fury
        RE(c, CharacterSlot.Legs, 51168, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);      // Sanctified Lightsworn Greaves
        RE(c, CharacterSlot.Feet, 50416, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);      // Boots of the Funeral March
        RE(c, CharacterSlot.Finger1, 50174, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);   // Incarnadine Band of Mending
        RE(c, CharacterSlot.Finger2, 51849, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);   // Cerise Coiled Ring
        RE(c, CharacterSlot.Trinket1, 50346, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);  // Sliver of Pure Ice (Use)
        RE(c, CharacterSlot.Trinket2, 50358, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);  // Purified Lunar Dust (SpellCast proc)
        RE(c, CharacterSlot.MainHand, 50028, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);  // Trauma (264)
        RE(c, CharacterSlot.OffHand, 49976, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);   // Bulwark of Smouldering Steel
        RE(c, CharacterSlot.Ranged, 50460, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);    // Libram of Blinding Light
    }

    // 277 holy paladin: 5pc Sanctified Lightsworn Garb (T10 heroic) + heroic ICC offpieces,
    // proc trinkets, caster libram.
    static void GearHealadin277(Character c, bool gems, bool enchants, int yellowGem)
    {
        RE(c, CharacterSlot.Head, 51272, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);      // Sanctified Lightsworn Headpiece
        RE(c, CharacterSlot.Neck, 50609, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);      // Bone Sentinel's Amulet
        RE(c, CharacterSlot.Shoulders, 51273, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5); // Sanctified Lightsworn Spaulders
        RE(c, CharacterSlot.Back, 50668, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);      // Greatcloak of the Turned Champion
        RE(c, CharacterSlot.Chest, 51274, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);     // Sanctified Lightsworn Tunic
        RE(c, CharacterSlot.Wrist, 50721, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);     // Crypt Keeper's Bracers
        RE(c, CharacterSlot.Hands, 51270, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);     // Sanctified Lightsworn Gloves
        RE(c, CharacterSlot.Waist, 50667, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);     // Waistband of Righteous Fury
        RE(c, CharacterSlot.Legs, 51271, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);      // Sanctified Lightsworn Greaves
        RE(c, CharacterSlot.Feet, 50632, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);      // Boots of the Funeral March
        RE(c, CharacterSlot.Finger1, 50400, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);   // Ashen Band of Endless Wisdom
        RE(c, CharacterSlot.Finger2, 50720, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);   // Incarnadine Band of Mending
        RE(c, CharacterSlot.Trinket1, 50366, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);  // Althor's Abacus (HealingSpellHit proc)
        RE(c, CharacterSlot.Trinket2, 54589, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);  // Glowing Twilight Scale (Use, recursive)
        RE(c, CharacterSlot.MainHand, 50685, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);  // Trauma (277)
        RE(c, CharacterSlot.OffHand, 50616, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);   // Bulwark of Smouldering Steel
        RE(c, CharacterSlot.Ranged, 50460, gems, enchants, yellowGem, GEM_HEAL_BLUE_MP5);    // Libram of Blinding Light
    }

    // ==================================================================
    // ProtWarr fixture generation (Phase 6 oracle)
    // ==================================================================

    static void ProtWarrMain(string outDir)
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Calculations.LoadModel(typeof(CalculationsProtWarr));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        ProtWarrGenerate(outDir);
    }

    static void ProtWarrGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Tank-Standard 15/5/51, full raid buffs, 277 heroic T10 tank set, gems+enchants
        {
            Character c = NewProtWarr("ProtWarrRaid277", ProtWarrDefaultSpec("Tank-Standard"), CharacterRace.Human, true);
            GearProtWarr277(c, true, true, false);
            CalculationOptionsProtWarr o = new CalculationOptionsProtWarr();
            FinishProtWarr(c, o, Path.Combine(outDir, "protwarr_277_raid.xml"));
        }

        // 2. Tank-DefFocused spec, 264 normal T10 tank set, no raid buffs, survival gemming
        {
            Character c = NewProtWarr("ProtWarrNoBuffs264", ProtWarrDefaultSpec("Tank-DefFocused"), CharacterRace.Tauren, false);
            GearProtWarr264(c, true, true, true);
            CalculationOptionsProtWarr o = new CalculationOptionsProtWarr();
            FinishProtWarr(c, o, Path.Combine(outDir, "protwarr_264_nobuffs.xml"));
        }

        // 3. Proc/on-use trinkets (Eitrigg's Oath DamageTaken child, Corpse Tongue Coin
        //    DamageTakenPhysical), 2pc only (head+shoulders offpieces), parry haste on,
        //    50% Heroic Strike frequency, custom threat scale
        {
            Character c = NewProtWarr("ProtWarrProcsParry", ProtWarrDefaultSpec("Tank-Standard"), CharacterRace.Troll, true);
            GearProtWarr277(c, true, true, false);
            TE(c, CharacterSlot.Head, 50640, true, true, false);      // Broken Ram Skull Helm
            TE(c, CharacterSlot.Shoulders, 50660, true, true, false); // Boneguard Commander's Pauldrons
            TE(c, CharacterSlot.Trinket1, 48021, true, true, false);  // Eitrigg's Oath (Use + DamageTaken stack)
            TE(c, CharacterSlot.Trinket2, 50349, true, true, false);  // Corpse Tongue Coin (DamageTakenPhysical)
            CalculationOptionsProtWarr o = new CalculationOptionsProtWarr();
            o.UseParryHaste = true;
            o.HeroicStrikeFrequency = 0.5f;
            o.ThreatScale = 12f;
            FinishProtWarr(c, o, Path.Combine(outDir, "protwarr_procs_parry.xml"));
        }

        // 4. Level-80 target (different avoidance caps), faster/harder incoming damage,
        //    TankPoints ranking mode
        {
            Character c = NewProtWarr("ProtWarrBoss80", ProtWarrDefaultSpec("Tank-Standard"), CharacterRace.NightElf, true);
            GearProtWarr264(c, true, true, false);
            CalculationOptionsProtWarr o = new CalculationOptionsProtWarr();
            o.TargetLevel = 80;
            o.TargetArmor = (int)StatConversion.NPC_ARMOR[0];
            o.BossAttackValue = 100000;
            o.BossAttackSpeed = 1.8f;
            o.UseParryHaste = true;
            o.RankingMode = 2;
            FinishProtWarr(c, o, Path.Combine(outDir, "protwarr_boss80.xml"));
        }

        // 5. Burst Time ranking mode, Tank-DW spec (Devastate+Revenge rotation),
        //    no Vigilance, no Heroic Strikes, heavy boss attack value
        {
            Character c = NewProtWarr("ProtWarrBurstTime", ProtWarrDefaultSpec("Tank-DW"), CharacterRace.Orc, true);
            GearProtWarr277(c, true, true, false);
            CalculationOptionsProtWarr o = new CalculationOptionsProtWarr();
            o.RankingMode = 3;
            o.BossAttackValue = 150000;
            o.UseVigilance = false;
            o.HeroicStrikeFrequency = 0f;
            FinishProtWarr(c, o, Path.Combine(outDir, "protwarr_bursttime.xml"));
        }

        // 6. Damage Output ranking mode, low threat weighting, 25% Heroic Strikes
        {
            Character c = NewProtWarr("ProtWarrDamage", ProtWarrDefaultSpec("Tank-Standard"), CharacterRace.Dwarf, true);
            GearProtWarr264(c, true, true, false);
            CalculationOptionsProtWarr o = new CalculationOptionsProtWarr();
            o.RankingMode = 4;
            o.HeroicStrikeFrequency = 0.25f;
            o.ThreatScale = 4f;
            o.MitigationScale = 0.25f;
            FinishProtWarr(c, o, Path.Combine(outDir, "protwarr_damage.xml"));
        }
    }

    static WarriorTalents ProtWarrDefaultSpec(string name)
    {
        TalentsBase t = ProtWarrSpec(name).TalentSpec();
        if (t == null) throw new InvalidOperationException(name + " spec failed to parse");
        return (WarriorTalents)t;
    }

    static Character NewProtWarr(string name, WarriorTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Warrior;
        c.CurrentModel = "ProtWarr";
        c.WarriorTalents = talents;
        if (raidBuffs)
            ((CalculationsProtWarr)Calculations.Instance).SetDefaults(c);
        return c;
    }

    static void FinishProtWarr(Character c, CalculationOptionsProtWarr options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    // protwarr tank gems (from CalculationsProtWarr.DefaultGemmingTemplates, epic tier)
    const int GEM_TANK_RED = 40111;        // Bold Cardinal Ruby +20 Str
    const int GEM_TANK_YELLOW_DEF = 40126; // Thick King's Amber +20 Defense
    const int GEM_TANK_YELLOW_DODGE = 40160; // Stalwart Ametrine +10 Dodge +10 Defense
    const int GEM_TANK_BLUE = 40119;       // Solid Sky Sapphire +30 Sta
    const int GEM_TANK_META = 41380;       // Austere Earthsiege Diamond
    const int GEM_TANK_PRISMATIC = 49110;  // Nightmare Tear

    // protwarr tank enchants
    const int ENCH_TANK_HEAD = 3818;     // Arcanum of the Stalwart Protector
    const int ENCH_TANK_SHOULDER = 3811; // Greater Inscription of the Pinnacle
    const int ENCH_TANK_BACK = 1951;     // Titanweave
    const int ENCH_TANK_CHEST = 3832;    // Powerful Stats
    const int ENCH_TANK_WRIST = 3850;    // Major Stamina
    const int ENCH_TANK_HANDS = 3253;    // Armsman
    const int ENCH_TANK_LEGS = 3822;     // Frosthide Leg Armor
    const int ENCH_TANK_FEET = 3232;     // Tuskarr's Vitality
    const int ENCH_TANK_WEAPON = 3827;   // Massacre
    const int ENCH_TANK_SHIELD = 3849;   // Titanium Plating

    static int GemForTank(ItemSlot color, bool survival)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_TANK_RED;
            case ItemSlot.Yellow: return survival ? GEM_TANK_YELLOW_DODGE : GEM_TANK_YELLOW_DEF;
            case ItemSlot.Blue: return GEM_TANK_BLUE;
            case ItemSlot.Meta: return GEM_TANK_META;
            case ItemSlot.Prismatic: return GEM_TANK_PRISMATIC;
            default: return 0;
        }
    }

    static int EnchForSlotTank(CharacterSlot slot)
    {
        switch (slot)
        {
            case CharacterSlot.Head: return ENCH_TANK_HEAD;
            case CharacterSlot.Shoulders: return ENCH_TANK_SHOULDER;
            case CharacterSlot.Back: return ENCH_TANK_BACK;
            case CharacterSlot.Chest: return ENCH_TANK_CHEST;
            case CharacterSlot.Wrist: return ENCH_TANK_WRIST;
            case CharacterSlot.Hands: return ENCH_TANK_HANDS;
            case CharacterSlot.Legs: return ENCH_TANK_LEGS;
            case CharacterSlot.Feet: return ENCH_TANK_FEET;
            case CharacterSlot.MainHand: return ENCH_TANK_WEAPON;
            case CharacterSlot.OffHand: return ENCH_TANK_SHIELD;
            default: return 0;
        }
    }

    static void TE(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants, bool survival)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = RequireItem(itemId);
            if (gems)
            {
                g1 = GemForTank(item.SocketColor1, survival);
                g2 = GemForTank(item.SocketColor2, survival);
                g3 = GemForTank(item.SocketColor3, survival);
            }
            if (enchants) ench = EnchForSlotTank(slot);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    // 277 protection warrior: 5pc Sanctified Ymirjar Lord's Plate (T10 heroic tank) + heroic ICC
    // offpieces, tank trinkets (Sindragosa's Flawless Fang / Corpse Tongue Coin), tank shield.
    static void GearProtWarr277(Character c, bool gems, bool enchants, bool survival)
    {
        TE(c, CharacterSlot.Head, 51221, gems, enchants, survival);       // Sanctified Ymirjar Lord's Greathelm
        TE(c, CharacterSlot.Neck, 50682, gems, enchants, survival);       // Bile-Encrusted Medallion
        TE(c, CharacterSlot.Shoulders, 51224, gems, enchants, survival);  // Sanctified Ymirjar Lord's Pauldrons
        TE(c, CharacterSlot.Back, 50718, gems, enchants, survival);       // Royal Crimson Cloak
        TE(c, CharacterSlot.Chest, 51220, gems, enchants, survival);      // Sanctified Ymirjar Lord's Breastplate
        TE(c, CharacterSlot.Wrist, 50611, gems, enchants, survival);      // Bracers of Dark Reckoning
        TE(c, CharacterSlot.Hands, 51222, gems, enchants, survival);      // Sanctified Ymirjar Lord's Handguards
        TE(c, CharacterSlot.Waist, 50691, gems, enchants, survival);      // Belt of Broken Bones
        TE(c, CharacterSlot.Legs, 51223, gems, enchants, survival);       // Sanctified Ymirjar Lord's Legguards
        TE(c, CharacterSlot.Feet, 50625, gems, enchants, survival);       // Grinning Skull Greatboots
        TE(c, CharacterSlot.Finger1, 50642, gems, enchants, survival);    // Juggernaut Band
        TE(c, CharacterSlot.Finger2, 50404, gems, enchants, survival);    // Ashen Band of Endless Courage
        TE(c, CharacterSlot.Trinket1, 50364, gems, enchants, survival);   // Sindragosa's Flawless Fang (Use)
        TE(c, CharacterSlot.Trinket2, 50349, gems, enchants, survival);   // Corpse Tongue Coin (DamageTakenPhysical)
        TE(c, CharacterSlot.MainHand, 50704, gems, enchants, survival);   // Rigormortis (277)
        TE(c, CharacterSlot.OffHand, 50729, gems, enchants, survival);    // Icecrown Glacial Wall
        TE(c, CharacterSlot.Ranged, 50444, gems, enchants, survival);     // Rowan's Rifle of Silver Bullets
    }

    // 264 protection warrior: 5pc Sanctified Ymirjar Lord's Plate (T10 normal tank) + ICC offpieces.
    static void GearProtWarr264(Character c, bool gems, bool enchants, bool survival)
    {
        TE(c, CharacterSlot.Head, 51218, gems, enchants, survival);       // Sanctified Ymirjar Lord's Greathelm
        TE(c, CharacterSlot.Neck, 50023, gems, enchants, survival);       // Bile-Encrusted Medallion
        TE(c, CharacterSlot.Shoulders, 51215, gems, enchants, survival);  // Sanctified Ymirjar Lord's Pauldrons
        TE(c, CharacterSlot.Back, 50074, gems, enchants, survival);       // Royal Crimson Cloak
        TE(c, CharacterSlot.Chest, 51219, gems, enchants, survival);      // Sanctified Ymirjar Lord's Breastplate
        TE(c, CharacterSlot.Wrist, 49960, gems, enchants, survival);      // Bracers of Dark Reckoning
        TE(c, CharacterSlot.Hands, 51217, gems, enchants, survival);      // Sanctified Ymirjar Lord's Handguards
        TE(c, CharacterSlot.Waist, 50036, gems, enchants, survival);      // Belt of Broken Bones
        TE(c, CharacterSlot.Legs, 51216, gems, enchants, survival);       // Sanctified Ymirjar Lord's Legguards
        TE(c, CharacterSlot.Feet, 49907, gems, enchants, survival);       // Boots of Kingly Upheaval
        TE(c, CharacterSlot.Finger1, 49985, gems, enchants, survival);    // Juggernaut Band
        TE(c, CharacterSlot.Finger2, 50447, gems, enchants, survival);    // Harbinger's Bone Band
        TE(c, CharacterSlot.Trinket1, 50356, gems, enchants, survival);   // Corroded Skeleton Key (Use)
        TE(c, CharacterSlot.Trinket2, 50352, gems, enchants, survival);   // Corpse Tongue Coin (DamageTakenPhysical)
        TE(c, CharacterSlot.MainHand, 50068, gems, enchants, survival);   // Rigormortis (264)
        TE(c, CharacterSlot.OffHand, 50065, gems, enchants, survival);    // Icecrown Glacial Wall
        TE(c, CharacterSlot.Ranged, 50444, gems, enchants, survival);     // Rowan's Rifle of Silver Bullets
    }

    static SavedTalentSpec ProtWarrSpec(string name)
    {
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Warrior))
            if (spec.Name == name) return spec;
        throw new InvalidOperationException("warrior spec not found: " + name);
    }

    static void ListProtWarr()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== WARRIOR TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Warrior))
        {
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));
            TalentsBase t = spec.TalentSpec();
            if (t != null && (spec.Name == "Tank-Standard" || spec.Name == "Tank-DefFocused" || spec.Name == "Tank-DW"))
                PrintTalents(spec.Name, t);
        }

        Console.WriteLine();
        Console.WriteLine("== PROTWARR ENCHANT SPOT CHECK ==");
        int[] ids = new int[] { 3818, 3811, 1951, 3832, 3850, 3253, 3822, 3232, 3827, 3805, 1099, 3845, 1603, 1597, 3823, 3817, 3808 };
        foreach (Enchant e in Enchant.AllEnchants.Where(x => ids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | slot={2} | {3}", e.Id, e.Name, e.Slot, e.Stats == null ? "" : StatLine(e.Stats)));

        Console.WriteLine();
        Console.WriteLine("== PROTWARR GEM SPOT CHECK ==");
        int[] gids = new int[] { 40111, 40112, 40115, 40116, 40118, 40126, 40119, 40167, 40160, 40139, 40138, 40141, 40143, 40144, 40162, 40163, 41380, 49110 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLine(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== PLATE TANK ITEMS (epic, ilvl>=264, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264 && i.Type == ItemType.Plate
                    && i.Stats.Stamina > 0)
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(18))
                Console.WriteLine(ItemLineTank(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger, ItemSlot.Trinket })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                    && (i.Stats.Stamina > 0 || i.Stats.DodgeRating > 0 || i.Stats.Armor > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(18))
                Console.WriteLine(ItemLineTank(item));
        }
        Console.WriteLine("-- OffHand (Shields) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.OffHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245
                && i.Type == ItemType.Shield)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(18))
            Console.WriteLine(ItemLineTank(item));
        Console.WriteLine("-- MainHand (tank one-handers) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.MainHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && i.Stats.Stamina > 0
                && (i.Type == ItemType.OneHandAxe || i.Type == ItemType.OneHandMace || i.Type == ItemType.OneHandSword || i.Type == ItemType.Dagger || i.Type == ItemType.FistWeapon))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(24))
            Console.WriteLine(ItemLineTank(item));
        Console.WriteLine("-- Ranged (thrown/guns/crossbows) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245
                && (i.Type == ItemType.Thrown || i.Type == ItemType.Gun || i.Type == ItemType.Crossbow || i.Type == ItemType.Bow))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(12))
            Console.WriteLine(ItemLineTank(item));
    }

    static string ItemLineTank(Item item)
    {
        return string.Format(CultureInfo.InvariantCulture, "{0} | {1} | ilvl {2} | {3} | set={4} | armor={5:F0} sta={6:F0} str={7:F0} agi={8:F0} dodge={9:F0} parry={10:F0} block={11:F0} def={12:F0} hit={13:F0} exp={14:F0} | {15}",
            item.Id, item.Name, item.ItemLevel, item.Type, item.SetName, item.Stats.Armor, item.Stats.Stamina,
            item.Stats.Strength, item.Stats.Agility, item.Stats.DodgeRating, item.Stats.ParryRating,
            item.Stats.BlockRating, item.Stats.DefenseRating, item.Stats.HitRating, item.Stats.ExpertiseRating,
            StatLine(item.Stats));
    }

    // ==================================================================
    // Moonkin fixture generation (Phase 6 oracle)
    // ==================================================================

    static void MoonkinMain(string outDir)
    {
        Calculations.LoadModel(typeof(CalculationsMoonkin));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        MoonkinGenerate(outDir);
    }

    // The shipped DefaultDataFiles/Talents.xml has no balance druid spec (only Cat/Bear/Tree/Feral
    // Hybrid), so the balance build is constructed property by property like the Mage Fire/Frost
    // specs. 58/0/13 style: 58 balance points including Eclipse/Wrath of Cenarius, 13 in Feral.
    static DruidTalents BalanceTalents()
    {
        DruidTalents t = new DruidTalents();
        // Balance
        t.StarlightWrath = 5; t.Genesis = 3; t.Moonglow = 3; t.NaturesMajesty = 2;
        t.ImprovedMoonfire = 2; t.Brambles = 3; t.NaturesGrace = 3; t.NaturesSplendor = 1;
        t.Vengeance = 5; t.CelestialFocus = 3; t.LunarGuidance = 3; t.InsectSwarm = 1;
        t.ImprovedInsectSwarm = 3; t.Dreamstate = 3; t.Moonfury = 3; t.BalanceOfPower = 2;
        t.MoonkinForm = 1; t.ImprovedMoonkinForm = 3; t.ImprovedFaerieFire = 3;
        t.WrathOfCenarius = 2; t.Eclipse = 3; t.ForceOfNature = 1; t.EarthAndMoon = 3; t.Starfall = 1;
        // Feral 13
        t.Furor = 3; t.ImprovedMarkOfTheWild = 2; t.Intensity = 3; t.OmenOfClarity = 1;
        t.MasterShapeshifter = 2; t.NaturalShapeshifter = 2;
        // glyphs
        t.GlyphOfStarfire = true; t.GlyphOfMoonfire = true; t.GlyphOfInsectSwarm = true;
        return t;
    }

    static void ListMoonkin()
    {
        Console.WriteLine("== MOONKIN BALANCE TALENTS (constructed, no balance spec shipped) ==");
        PrintTalents("BalanceTalents", BalanceTalents());

        Console.WriteLine();
        Console.WriteLine("== MOONKIN ENCHANT SPOT CHECK ==");
        int[] ids = new int[] { 3820, 3810, 3728, 2661, 2332, 3246, 3719, 3826, 3834, 3854, 3722 };
        foreach (Enchant e in Enchant.AllEnchants.Where(x => ids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | slot={2} | {3}", e.Id, e.Name, e.Slot, e.Stats == null ? "" : StatLine(e.Stats)));

        Console.WriteLine();
        Console.WriteLine("== MOONKIN GEM SPOT CHECK ==");
        int[] gids = new int[] { 40113, 40155, 40153, 40152, 40133, 41285, 49110, 41401 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLine(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== CASTER LEATHER (epic, ilvl>=245, sp>0, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245 && i.Type == ItemType.Leather
                    && i.Stats.SpellPower > 0)
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(12))
                Console.WriteLine(ItemLineMk(item));
        }
        Console.WriteLine();
        Console.WriteLine("== CASTER CLOTH (epic, ilvl>=251, sp>0, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 251 && i.Type == ItemType.Cloth
                    && i.Stats.SpellPower > 0)
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(10))
                Console.WriteLine(ItemLineMk(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger, ItemSlot.Trinket })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245
                    && (i.Stats.SpellPower > 0 || i.Stats.Mp5 > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
                Console.WriteLine(ItemLineMk(item));
        }
        Console.WriteLine("-- OffHand (caster held) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.OffHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245
                && i.Stats.SpellPower > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
            Console.WriteLine(ItemLineMk(item));
        Console.WriteLine("-- MainHand (caster weapons) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.MainHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245
                && i.Stats.SpellPower > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLineMk(item));
        Console.WriteLine("-- TwoHand (caster staves) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.TwoHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245
                && i.Stats.SpellPower > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLineMk(item));
        Console.WriteLine("-- Ranged (Idols, all with moonkin stats) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Quality == ItemQuality.Epic
                && (i.Stats.StarfireDmg > 0 || i.Stats.WrathDmg > 0 || i.Stats.MoonfireDmg > 0
                    || i.Stats.InsectSwarmDmg > 0 || i.Stats.BonusMoonkinNukeDamage > 0 || i.Stats.StarfireProc > 0))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(30))
            Console.WriteLine(ItemLineMk(item));
        Console.WriteLine();
        Console.WriteLine("== LASHERWEAVE REGALIA 2PC BUFF ==");
        Buff t10 = Buff.GetBuffByName("Lasherweave Regalia (T10) 2 Piece Bonus");
        Console.WriteLine(t10 == null ? "MISSING BUFF" : "found: " + t10.Name + " | " + StatLine(t10.Stats));
    }

    static string ItemLineMk(Item item)
    {
        int fx = 0;
        foreach (SpecialEffect e in item.Stats.SpecialEffects()) fx++;
        return string.Format(CultureInfo.InvariantCulture, "{0} | {1} | ilvl {2} | {3} | set={4} | fx={5} | sf={6:F0} wr={7:F0} mf={8:F0} is={9:F0} mk={10:F3} sfproc={11:F0} | {12}",
            item.Id, item.Name, item.ItemLevel, item.Type, item.SetName, fx,
            item.Stats.StarfireDmg, item.Stats.WrathDmg, item.Stats.MoonfireDmg, item.Stats.InsectSwarmDmg,
            item.Stats.BonusMoonkinNukeDamage, item.Stats.StarfireProc, StatLine(item.Stats));
    }

    // 277 balance moonkin: 5pc Sanctified Lasherweave Regalia (T10 heroic) + ICC 25 caster
    // leather/cloth offpieces, caster stat-proc trinkets (Dislodged Foreign Object /
    // Muradin's Spyglass), balance idol.
    static void GearMoonkin277(Character c, bool gems, bool enchants)
    {
        ME(c, CharacterSlot.Head, 51290, gems, enchants);        // Sanctified Lasherweave Cover
        ME(c, CharacterSlot.Neck, 50609, gems, enchants);        // Bone Sentinel's Amulet
        ME(c, CharacterSlot.Shoulders, 51292, gems, enchants);   // Sanctified Lasherweave Mantle
        ME(c, CharacterSlot.Back, 50668, gems, enchants);        // Greatcloak of the Turned Champion
        ME(c, CharacterSlot.Chest, 51294, gems, enchants);       // Sanctified Lasherweave Vestment
        ME(c, CharacterSlot.Wrist, 50630, gems, enchants);       // Bracers of Eternal Dreaming (leather)
        ME(c, CharacterSlot.Hands, 51291, gems, enchants);       // Sanctified Lasherweave Gloves
        ME(c, CharacterSlot.Waist, 50705, gems, enchants);       // Professor's Bloodied Smock (leather)
        ME(c, CharacterSlot.Legs, 51293, gems, enchants);        // Sanctified Lasherweave Trousers
        ME(c, CharacterSlot.Feet, 50665, gems, enchants);        // Boots of Unnatural Growth (leather)
        ME(c, CharacterSlot.Finger1, 50614, gems, enchants);     // Loop of the Endless Labyrinth
        ME(c, CharacterSlot.Finger2, 50400, gems, enchants);     // Ashen Band of Endless Wisdom
        ME(c, CharacterSlot.Trinket1, 50348, gems, enchants);    // Dislodged Foreign Object (SpellPower proc)
        ME(c, CharacterSlot.Trinket2, 50345, gems, enchants);    // Muradin's Spyglass (CritRating proc)
        ME(c, CharacterSlot.MainHand, 50734, gems, enchants);    // Royal Scepter of Terenas II (mace)
        ME(c, CharacterSlot.OffHand, 50719, gems, enchants);     // Shadow Silk Spindle
        ME(c, CharacterSlot.Ranged, 45270, gems, enchants);      // Idol of the Crying Wind (InsectSwarmDmg)
    }

    // 264 balance moonkin: 5pc Sanctified Lasherweave Regalia (T10 normal) + ICC offpieces,
    // child-effect trinkets (Fetish of Volatile Power haste stack / Nevermelting Ice Crystal).
    static void GearMoonkin264(Character c, bool gems, bool enchants)
    {
        ME(c, CharacterSlot.Head, 51149, gems, enchants);        // Sanctified Lasherweave Cover
        ME(c, CharacterSlot.Neck, 50005, gems, enchants);        // Amulet of the Silent Eulogy
        ME(c, CharacterSlot.Shoulders, 51147, gems, enchants);   // Sanctified Lasherweave Mantle
        ME(c, CharacterSlot.Back, 50014, gems, enchants);        // Greatcloak of the Turned Champion
        ME(c, CharacterSlot.Chest, 51145, gems, enchants);       // Sanctified Lasherweave Vestment
        ME(c, CharacterSlot.Wrist, 50417, gems, enchants);       // Bracers of Eternal Dreaming (leather)
        ME(c, CharacterSlot.Hands, 51148, gems, enchants);       // Sanctified Lasherweave Gloves
        ME(c, CharacterSlot.Waist, 50069, gems, enchants);       // Professor's Bloodied Smock (leather)
        ME(c, CharacterSlot.Legs, 51146, gems, enchants);        // Sanctified Lasherweave Trousers
        ME(c, CharacterSlot.Feet, 50009, gems, enchants);        // Boots of Unnatural Growth (leather)
        ME(c, CharacterSlot.Finger1, 50174, gems, enchants);     // Incarnadine Band of Mending
        ME(c, CharacterSlot.Finger2, 51849, gems, enchants);     // Cerise Coiled Ring
        ME(c, CharacterSlot.Trinket1, 48018, gems, enchants);    // Fetish of Volatile Power (HasteRating child)
        ME(c, CharacterSlot.Trinket2, 50259, gems, enchants);    // Nevermelting Ice Crystal (CritRating child)
        ME(c, CharacterSlot.MainHand, 49968, gems, enchants);    // Frozen Bonespike (dagger)
        ME(c, CharacterSlot.OffHand, 50173, gems, enchants);     // Shadow Silk Spindle
        ME(c, CharacterSlot.Ranged, 45270, gems, enchants);      // Idol of the Crying Wind
    }

    static Character NewMoonkin(string name, DruidTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Druid;
        c.CurrentModel = "Moonkin";
        c.DruidTalents = talents;
        if (raidBuffs)
            ((CalculationsMoonkin)Calculations.Instance).SetDefaults(c);
        else
            c.ActiveBuffsAdd("Moonkin Form");
        return c;
    }

    static void FinishMoonkin(Character c, CalculationOptionsMoonkin options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    static int GemForMoonkin(ItemSlot color)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_RED;           // Runed Cardinal Ruby +23 SP
            case ItemSlot.Yellow: return 40155;          // Reckless Ametrine +12 SP +10 haste
            case ItemSlot.Blue: return 40133;            // Purified Dreadstone +12 SP +10 spirit
            case ItemSlot.Meta: return 41285;            // Chaotic Skyflare Diamond
            case ItemSlot.Prismatic: return GEM_PRISMATIC;
            default: return 0;
        }
    }

    static void ME(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = RequireItem(itemId);
            if (gems)
            {
                g1 = GemForMoonkin(item.SocketColor1);
                g2 = GemForMoonkin(item.SocketColor2);
                g3 = GemForMoonkin(item.SocketColor3);
            }
            if (enchants) ench = EnchForSlot(slot);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    static void MoonkinGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Balance, full raid buffs, 264 T10 5pc, gems+enchants, target 83
        {
            Character c = NewMoonkin("MoonkinRaid264", BalanceTalents(), CharacterRace.NightElf, true);
            GearMoonkin264(c, true, true);
            CalculationOptionsMoonkin o = new CalculationOptionsMoonkin();
            FinishMoonkin(c, o, Path.Combine(outDir, "moonkin_264_raid.xml"));
        }

        // 2. 277 heroic T10 5pc + proc trinkets, gems+enchants, target 83
        {
            Character c = NewMoonkin("MoonkinProcs277", BalanceTalents(), CharacterRace.Tauren, true);
            GearMoonkin277(c, true, true);
            CalculationOptionsMoonkin o = new CalculationOptionsMoonkin();
            o.ReplenishmentUptime = 0.5f;
            FinishMoonkin(c, o, Path.Combine(outDir, "moonkin_277_procs.xml"));
        }

        // 3. No raid buffs, 264 gear (Moonkin Form still on), long fight with Innervate,
        //    Mp5 proc trinket (Solace of the Defeated) and a shadow-damage proc (Reign of the
        //    Unliving), reduced treant lifespan
        {
            Character c = NewMoonkin("MoonkinNoBuffs", BalanceTalents(), CharacterRace.NightElf, false);
            GearMoonkin264(c, true, true);
            ME(c, CharacterSlot.Trinket1, 47041, true, true);    // Solace of the Defeated (Mp5 proc)
            ME(c, CharacterSlot.Trinket2, 47182, true, true);    // Reign of the Unliving (ShadowDamage proc)
            CalculationOptionsMoonkin o = new CalculationOptionsMoonkin();
            o.FightLength = 10f;
            o.Innervate = true;
            o.InnervateDelay = 2f;
            o.TreantLifespan = 0.5f;
            FinishMoonkin(c, o, Path.Combine(outDir, "moonkin_nobuffs_long.xml"));
        }

        // 4. No Eclipse (exercises the non-Eclipse rotation set), target 80, short fight,
        //    custom rotation options and latency
        {
            DruidTalents t = BalanceTalents();
            t.Eclipse = 0;
            Character c = NewMoonkin("MoonkinNoEclipse80", t, CharacterRace.Troll, true);
            GearMoonkin277(c, true, true);
            CalculationOptionsMoonkin o = new CalculationOptionsMoonkin();
            o.TargetLevel = 80;
            o.FightLength = 3f;
            o.Latency = 0.1f;
            o.UserRotation = "MF/W";
            o.PTRMode = true;
            FinishMoonkin(c, o, Path.Combine(outDir, "moonkin_noeclipse_short80.xml"));
        }
    }

    // ==================================================================
    // Retribution fixture generation (Phase 6 oracle)
    // ==================================================================

    // ret gems (from CalculationsRetribution.DefaultGemmingTemplates, epic tier)
    const int GEM_RET_RED = 40111;         // Bold Cardinal Ruby +20 Str
    const int GEM_RET_YELLOW_CRIT = 40142; // Inscribed Ametrine +10 Str +10 Crit
    const int GEM_RET_YELLOW_HIT = 40143;  // Etched Ametrine +10 Str +10 Hit
    const int GEM_RET_BLUE = 40129;        // Sovereign Dreadstone +10 Str +15 Sta
    const int GEM_RET_META = 41398;        // Relentless Earthsiege Diamond

    // ret enchants
    const int ENCH_RET_HEAD = 3817;     // Arcanum of Torment
    const int ENCH_RET_SHOULDER = 3808; // Greater Inscription of the Axe
    const int ENCH_RET_BACK = 1099;     // Major Agility
    const int ENCH_RET_CHEST = 3832;    // Powerful Stats
    const int ENCH_RET_WRIST = 3845;    // Greater Assault
    const int ENCH_RET_HANDS = 1603;    // Crusher
    const int ENCH_RET_LEGS = 3823;     // Icescale Leg Armor
    const int ENCH_RET_FEET = 1597;     // Greater Assault (or 3232 Tuskarr's Vitality)
    const int ENCH_RET_WEAPON = 3827;   // Massacre

    // ==================================================================
    // Enhance fixture generation (Phase 6 oracle)
    // ==================================================================

    // enhance gems (from CalculationsEnhance -> Gemming.addTemplates, epic tier)
    const int GEM_ENH_RED = 40112;    // Delicate Cardinal Ruby +20 Agi
    const int GEM_ENH_HIT = 40125;    // Rigid King's Amber +20 Hit
    const int GEM_ENH_BLUE = 40130;   // Shifting Dreadstone +10 Agi +15 Sta
    const int GEM_ENH_META = 41398;   // Relentless Earthsiege Diamond

    // enhance enchants
    const int ENCH_ENH_HEAD = 3817;     // Arcanum of Torment
    const int ENCH_ENH_SHOULDER = 3808; // Greater Inscription of the Axe
    const int ENCH_ENH_BACK = 1099;     // Major Agility
    const int ENCH_ENH_CHEST = 3832;    // Powerful Stats
    const int ENCH_ENH_WRIST = 3845;    // Greater Assault
    const int ENCH_ENH_HANDS = 1603;    // Crusher
    const int ENCH_ENH_LEGS = 3823;     // Icescale Leg Armor
    const int ENCH_ENH_FEET = 1597;     // Greater Assault
    const int ENCH_ENH_MH = 3789;       // Berserking
    const int ENCH_ENH_OH = 2673;       // Mongoose

    static void RetMain(string outDir)
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Calculations.LoadModel(typeof(CalculationsRetribution));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        RetGenerate(outDir);
    }

    // The shipped DefaultDataFiles/Talents.xml ships a "Retribution" paladin build (0/17/54).
    static SavedTalentSpec RetSpec()
    {
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Paladin))
            if (spec.Name == "Retribution") return spec;
        throw new InvalidOperationException("paladin Retribution spec not found");
    }

    static string ItemLineRet(Item item)
    {
        int fx = 0;
        foreach (SpecialEffect e in item.Stats.SpecialEffects()) fx++;
        return string.Format(CultureInfo.InvariantCulture,
            "{0} | {1} | ilvl {2} | {3} | set={4} | fx={5} | str={6:F0} agi={7:F0} ap={8:F0} crit={9:F0} hit={10:F0} exp={11:F0} arp={12:F0} haste={13:F0} | sp={14:F0}",
            item.Id, item.Name, item.ItemLevel, item.Type, item.SetName, fx,
            item.Stats.Strength, item.Stats.Agility, item.Stats.AttackPower, item.Stats.CritRating,
            item.Stats.HitRating, item.Stats.ExpertiseRating, item.Stats.ArmorPenetrationRating,
            item.Stats.HasteRating, item.Stats.SpellPower);
    }

    static void ListRet()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== PALADIN TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Paladin))
        {
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));
            if (spec.Name == "Retribution")
                PrintTalents(spec.Name, spec.TalentSpec());
        }

        Console.WriteLine();
        Console.WriteLine("== RET ENCHANT SPOT CHECK ==");
        int[] ids = new int[] { 3817, 3808, 1099, 3832, 3845, 1603, 3823, 1597, 3827, 3789, 3232 };
        foreach (Enchant e in Enchant.AllEnchants.Where(x => ids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | slot={2} | {3}", e.Id, e.Name, e.Slot, e.Stats == null ? "" : StatLine(e.Stats)));

        Console.WriteLine();
        Console.WriteLine("== RET GEM SPOT CHECK ==");
        int[] gids = new int[] { 40111, 40142, 40143, 40129, 41398, 49110 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLine(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== RET SET BONUSES (Lightsworn Battlegear) ==");
        foreach (Buff b in Buff.AllBuffs.Where(x => x.SetName == "Lightsworn Battlegear").OrderBy(x => x.Name))
            Console.WriteLine(b.Name + " | threshold=" + b.SetThreshold + " | " + StatLine(b.Stats));

        Console.WriteLine();
        Console.WriteLine("== PLATE DPS ITEMS (epic, ilvl>=264, str>0, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264 && i.Type == ItemType.Plate
                    && i.Stats.Strength > 0)
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(18))
                Console.WriteLine(ItemLineRet(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                    && i.Stats.Strength > 0)
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(18))
                Console.WriteLine(ItemLineRet(item));
        }
        Console.WriteLine("-- Finger --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Finger && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && i.Stats.Strength > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(18))
            Console.WriteLine(ItemLineRet(item));
        Console.WriteLine("-- Trinket (ilvl>=264, epic) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Trinket && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(30))
            Console.WriteLine(ItemLineRet(item));
        Console.WriteLine("-- TwoHand (ilvl>=264, melee) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.TwoHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && (i.Type == ItemType.TwoHandAxe || i.Type == ItemType.TwoHandMace || i.Type == ItemType.TwoHandSword || i.Type == ItemType.Polearm)
                && (i.Stats.Strength > 0 || i.Stats.AttackPower > 0))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(24))
            Console.WriteLine(ItemLineRet(item));
        Console.WriteLine("-- Ranged (Librams, ilvl>=264) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Quality == ItemQuality.Epic && i.Type == ItemType.Libram)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(30))
            Console.WriteLine(ItemLineRet(item));
    }

    static Character NewRet(string name, PaladinTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Paladin;
        c.CurrentModel = "Retribution";
        c.PaladinTalents = talents;
        if (raidBuffs)
            ((CalculationsRetribution)Calculations.Instance).SetDefaults(c);
        else
            c.ActiveBuffsAdd("Sanctified Retribution");
        return c;
    }

    static void FinishRet(Character c, CalculationOptionsRetribution options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    static int GemForRet(ItemSlot color, int yellowGem)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_RET_RED;
            case ItemSlot.Yellow: return yellowGem;
            case ItemSlot.Blue: return GEM_RET_BLUE;
            case ItemSlot.Meta: return GEM_RET_META;
            case ItemSlot.Prismatic: return GEM_PRISMATIC;
            default: return 0;
        }
    }

    static int EnchForSlotRet(CharacterSlot slot)
    {
        switch (slot)
        {
            case CharacterSlot.Head: return ENCH_RET_HEAD;
            case CharacterSlot.Shoulders: return ENCH_RET_SHOULDER;
            case CharacterSlot.Back: return ENCH_RET_BACK;
            case CharacterSlot.Chest: return ENCH_RET_CHEST;
            case CharacterSlot.Wrist: return ENCH_RET_WRIST;
            case CharacterSlot.Hands: return ENCH_RET_HANDS;
            case CharacterSlot.Legs: return ENCH_RET_LEGS;
            case CharacterSlot.Feet: return ENCH_RET_FEET;
            case CharacterSlot.MainHand: return ENCH_RET_WEAPON;
            default: return 0;
        }
    }

    static void RE(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants, int yellowGem)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = RequireItem(itemId);
            if (gems)
            {
                g1 = GemForRet(item.SocketColor1, yellowGem);
                g2 = GemForRet(item.SocketColor2, yellowGem);
                g3 = GemForRet(item.SocketColor3, yellowGem);
            }
            if (enchants) ench = EnchForSlotRet(slot);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    // 277 ret: 5pc Sanctified Lightsworn Battlegear (T10 heroic) plus ICC 25 heroic offpieces
    // and stat-proc trinkets (Deathbringer's Will / Tiny Abomination in a Jar);
    // Glorenzelg, High-Blade of the Silver Hand (2H sword) and Libram of Valiance.
    static void GearRet277(Character c, bool gems, bool enchants, int yellow)
    {
        RE(c, CharacterSlot.Head, 51277, gems, enchants, yellow);        // Sanctified Lightsworn Helmet (277)
        RE(c, CharacterSlot.Neck, 50728, gems, enchants, yellow);        // Lana'thel's Chain of Flagellation
        RE(c, CharacterSlot.Shoulders, 51279, gems, enchants, yellow);   // Sanctified Lightsworn Shoulderplates (277)
        RE(c, CharacterSlot.Back, 50677, gems, enchants, yellow);        // Winding Sheet
        RE(c, CharacterSlot.Chest, 51275, gems, enchants, yellow);       // Sanctified Lightsworn Battleplate (277)
        RE(c, CharacterSlot.Wrist, 50659, gems, enchants, yellow);       // Polar Bear Claw Bracers
        RE(c, CharacterSlot.Hands, 51276, gems, enchants, yellow);       // Sanctified Lightsworn Gauntlets (277)
        RE(c, CharacterSlot.Waist, 50620, gems, enchants, yellow);       // Coldwraith Links
        RE(c, CharacterSlot.Legs, 51278, gems, enchants, yellow);        // Sanctified Lightsworn Legplates (277)
        RE(c, CharacterSlot.Feet, 50639, gems, enchants, yellow);        // Blood-Soaked Saronite Stompers
        RE(c, CharacterSlot.Finger1, 52572, gems, enchants, yellow);     // Ashen Band of Endless Might (Str proc)
        RE(c, CharacterSlot.Finger2, 50657, gems, enchants, yellow);     // Skeleton Lord's Circle
        RE(c, CharacterSlot.Trinket1, 50363, gems, enchants, yellow);    // Deathbringer's Will (DeathbringerProc)
        RE(c, CharacterSlot.Trinket2, 50706, gems, enchants, yellow);    // Tiny Abomination in a Jar (MoteOfAnger)
        RE(c, CharacterSlot.MainHand, 50730, gems, enchants, yellow);    // Glorenzelg, High-Blade of the Silver Hand
        RE(c, CharacterSlot.Ranged, 47661, gems, enchants, yellow);      // Libram of Valiance (SoV tick Str proc)
    }

    // 264 ret: 5pc Lightsworn Battlegear (T10 normal) plus ICC offpieces, a Use trinket
    // (Mark of Supremacy) and a Paragon proc trinket (Death's Verdict); Shadow's Edge.
    static void GearRet264(Character c, bool gems, bool enchants, int yellow)
    {
        RE(c, CharacterSlot.Head, 51162, gems, enchants, yellow);        // Sanctified Lightsworn Helmet (264)
        RE(c, CharacterSlot.Neck, 49989, gems, enchants, yellow);        // Ahn'kahar Onyx Neckguard
        RE(c, CharacterSlot.Shoulders, 51160, gems, enchants, yellow);   // Sanctified Lightsworn Shoulderplates (264)
        RE(c, CharacterSlot.Back, 50019, gems, enchants, yellow);        // Winding Sheet
        RE(c, CharacterSlot.Chest, 51164, gems, enchants, yellow);       // Sanctified Lightsworn Battleplate (264)
        RE(c, CharacterSlot.Wrist, 50002, gems, enchants, yellow);       // Polar Bear Claw Bracers
        RE(c, CharacterSlot.Hands, 51163, gems, enchants, yellow);       // Sanctified Lightsworn Gauntlets (264)
        RE(c, CharacterSlot.Waist, 50187, gems, enchants, yellow);       // Coldwraith Links
        RE(c, CharacterSlot.Legs, 51161, gems, enchants, yellow);        // Sanctified Lightsworn Legplates (264)
        RE(c, CharacterSlot.Feet, 49983, gems, enchants, yellow);        // Blood-Soaked Saronite Stompers
        RE(c, CharacterSlot.Finger1, 49999, gems, enchants, yellow);     // Skeleton Lord's Circle
        RE(c, CharacterSlot.Finger2, 50453, gems, enchants, yellow);     // Ring of Rotting Sinew
        RE(c, CharacterSlot.Trinket1, 47734, gems, enchants, yellow);    // Mark of Supremacy (Use: +1024 AP)
        RE(c, CharacterSlot.Trinket2, 47115, gems, enchants, yellow);    // Death's Verdict (Paragon)
        RE(c, CharacterSlot.MainHand, 49888, gems, enchants, yellow);    // Shadow's Edge
        RE(c, CharacterSlot.Ranged, 47661, gems, enchants, yellow);      // Libram of Valiance (SoV tick Str proc)
    }

    static void RetGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Full raid buffs, 264 T10 Battlegear 5pc, gems+enchants, default options
        //    (default rotation simulated, Seal of Vengeance, target 83)
        {
            Character c = NewRet("RetRaid264", RetSpec().TalentSpec() as PaladinTalents, CharacterRace.BloodElf, true);
            GearRet264(c, true, true, GEM_RET_YELLOW_CRIT);
            CalculationOptionsRetribution o = new CalculationOptionsRetribution();
            FinishRet(c, o, Path.Combine(outDir, "ret_264_raid.xml"));
        }

        // 2. 277 heroic T10 5pc + proc trinkets (Deathbringer's Will / Tiny Abomination),
        //    gems+enchants (Str/hit), full raid buffs
        {
            Character c = NewRet("RetProcs277", RetSpec().TalentSpec() as PaladinTalents, CharacterRace.Human, true);
            GearRet277(c, true, true, GEM_RET_YELLOW_HIT);
            CalculationOptionsRetribution o = new CalculationOptionsRetribution();
            o.TimeUnder20 = .25f;
            FinishRet(c, o, Path.Combine(outDir, "ret_277_procs.xml"));
        }

        // 3. No raid buffs, 264 gear, short 60s fight, no Bloodlust, custom option set
        {
            Character c = NewRet("RetNoBuffs60", RetSpec().TalentSpec() as PaladinTalents, CharacterRace.Draenei, false);
            GearRet264(c, true, true, GEM_RET_YELLOW_CRIT);
            CalculationOptionsRetribution o = new CalculationOptionsRetribution();
            o.FightLength = 1f;
            o.Bloodlust = false;
            o.TimeUnder20 = .1f;
            o.Targets = 1f;
            FinishRet(c, o, Path.Combine(outDir, "ret_nobuffs_short.xml"));
        }

        // 4. Effective-cooldowns mode (no simulation), long 600s fight, target switches,
        //    HoR effectiveness and Consecration effectiveness on
        {
            Character c = NewRet("RetEcd600", RetSpec().TalentSpec() as PaladinTalents, CharacterRace.Dwarf, true);
            GearRet277(c, true, true, GEM_RET_YELLOW_CRIT);
            CalculationOptionsRetribution o = new CalculationOptionsRetribution();
            o.SimulateRotation = false;
            o.Seal = SealOf.Righteousness;
            o.FightLength = 10f;
            o.TargetSwitches = 3f;
            o.HoREff = 0.5f;
            o.ConsEff = 0.8f;
            o.InFront = 1f;
            FinishRet(c, o, Path.Combine(outDir, "ret_ecd_long.xml"));
        }

        // 5. Seal of Command, undead mob with Glyph of Sense Undead, target level 80,
        //    custom rotation list (subset of abilities, no Exorcism)
        {
            Character c = NewRet("RetCommand80", RetSpec().TalentSpec() as PaladinTalents, CharacterRace.Tauren, true);
            GearRet264(c, true, true, GEM_RET_YELLOW_CRIT);
            CalculationOptionsRetribution o = new CalculationOptionsRetribution();
            o.Seal = SealOf.Command;
            o.Mob = MobType.Undead;
            o.TargetLevel = 80;
            o.Targets = 3f;
            o.Rotations.Add(new Rawr.Retribution.Ability[] { Rawr.Retribution.Ability.CrusaderStrike, Rawr.Retribution.Ability.Judgement, Rawr.Retribution.Ability.Consecration, Rawr.Retribution.Ability.DivineStorm });
            FinishRet(c, o, Path.Combine(outDir, "ret_command_80.xml"));
        }
    }

    // ==================================================================
    // ProtPaladin fixture generation (Phase 6 oracle)
    // ==================================================================

    static void ProtPaladinMain(string outDir)
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Calculations.LoadModel(typeof(CalculationsProtPaladin));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        ProtPaladinGenerate(outDir);
    }

    // The shipped DefaultDataFiles/Talents.xml ships several Protection builds; the tank fixtures
    // use "Protection: Seals of the Pure" (5/59/7) which carries Hammer of the Righteous,
    // Touched by the Light, Ardent Defender and Shield of the Templar.
    static SavedTalentSpec ProtPaladinSpec(string name)
    {
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Paladin))
            if (spec.Name == name) return spec;
        throw new InvalidOperationException("paladin spec not found: " + name);
    }

    static PaladinTalents ProtPaladinDefaultSpec(string name)
    {
        TalentsBase t = ProtPaladinSpec(name).TalentSpec();
        if (t == null) throw new InvalidOperationException(name + " spec failed to parse");
        return (PaladinTalents)t;
    }

    static string ItemLineProtPal(Item item)
    {
        int fx = 0;
        foreach (SpecialEffect e in item.Stats.SpecialEffects()) fx++;
        return string.Format(CultureInfo.InvariantCulture,
            "{0} | {1} | ilvl {2} | {3} | set={4} | fx={5} | armor={6:F0} sta={7:F0} str={8:F0} agi={9:F0} def={10:F0} dodge={11:F0} parry={12:F0} block={13:F0} hp={14:F0} sp={15:F0} hit={16:F0} exp={17:F0}",
            item.Id, item.Name, item.ItemLevel, item.Type, item.SetName, fx, item.Stats.Armor,
            item.Stats.Stamina, item.Stats.Strength, item.Stats.Agility, item.Stats.DefenseRating,
            item.Stats.DodgeRating, item.Stats.ParryRating, item.Stats.BlockRating,
            item.Stats.Health, item.Stats.SpellPower, item.Stats.HitRating, item.Stats.ExpertiseRating);
    }

    static void ListProtPaladin()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== PALADIN TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Paladin))
        {
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));
            if (spec.Name.StartsWith("Protection"))
                PrintTalents(spec.Name, spec.TalentSpec());
        }

        Console.WriteLine();
        Console.WriteLine("== PROTPAL ENCHANT SPOT CHECK ==");
        int[] ids = new int[] { 3818, 3811, 1951, 3832, 3850, 3253, 3822, 3232, 3849, 3834, 2673, 3827, 3817, 3808, 1099, 3845, 1603, 1597, 3823 };
        foreach (Enchant e in Enchant.AllEnchants.Where(x => ids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | slot={2} | {3}", e.Id, e.Name, e.Slot, e.Stats == null ? "" : StatLine(e.Stats)));

        Console.WriteLine();
        Console.WriteLine("== PROTPAL GEM SPOT CHECK ==");
        int[] gids = new int[] { 40111, 40115, 40116, 40118, 40126, 40119, 40167, 40160, 40161, 40163, 41380, 49110, 40142, 40143, 40129, 40138, 40139, 40141 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLine(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== PROTPAL SET BONUSES (Lightsworn) ==");
        foreach (Buff b in Buff.AllBuffs.Where(x => x.SetName != null && x.SetName.Contains("Lightsworn")).OrderBy(x => x.SetName).ThenBy(x => x.Name))
            Console.WriteLine(b.SetName + " | " + b.Name + " | threshold=" + b.SetThreshold + " | " + StatLine(b.Stats));

        Console.WriteLine();
        Console.WriteLine("== PLATE TANK ITEMS (epic, ilvl>=264, sta>0, sp/block/def/dodge, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264 && i.Type == ItemType.Plate
                    && i.Stats.Stamina > 0
                    && (i.Stats.SpellPower > 0 || i.Stats.BlockRating > 0 || i.Stats.BlockValue > 0 || i.Stats.DefenseRating > 0 || i.Stats.DodgeRating > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(24))
                Console.WriteLine(ItemLineProtPal(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                    && i.Stats.Stamina > 0
                    && (i.Stats.SpellPower > 0 || i.Stats.BlockRating > 0 || i.Stats.BlockValue > 0 || i.Stats.DefenseRating > 0 || i.Stats.DodgeRating > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(18))
                Console.WriteLine(ItemLineProtPal(item));
        }
        Console.WriteLine("-- Trinket (ilvl>=245, epic) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Trinket && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245
                && (i.Stats.Stamina > 0 || i.Stats.BlockValue > 0 || i.Stats.DodgeRating > 0))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(30))
            Console.WriteLine(ItemLineProtPal(item));
        Console.WriteLine("-- OffHand (Shields, ilvl>=245) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.OffHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245
                && i.Type == ItemType.Shield)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(18))
            Console.WriteLine(ItemLineProtPal(item));
        Console.WriteLine("-- MainHand (caster one-handers with spell power, ilvl>=264) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.MainHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && i.Stats.SpellPower > 0
                && (i.Type == ItemType.OneHandAxe || i.Type == ItemType.OneHandMace || i.Type == ItemType.OneHandSword || i.Type == ItemType.Dagger))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(24))
            Console.WriteLine(ItemLineProtPal(item));
        Console.WriteLine("-- Ranged (Librams, ilvl>=245) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Quality == ItemQuality.Epic && i.Type == ItemType.Libram)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(30))
            Console.WriteLine(ItemLineProtPal(item));
    }

    static Character NewProtPaladin(string name, PaladinTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Paladin;
        c.CurrentModel = "ProtPaladin";
        c.PaladinTalents = talents;
        if (raidBuffs)
            c.ActiveBuffsAdd("Blessing of Kings");
        return c;
    }

    static void FinishProtPaladin(Character c, CalculationOptionsProtPaladin options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    // protpal tank gems (from CalculationsProtPaladin.DefaultGemmingTemplates, epic tier)
    const int GEM_PP_RED = 40111;          // Bold Cardinal Ruby +20 Str
    const int GEM_PP_YELLOW_DODGE = 40160; // Stalwart Ametrine +10 Dodge +10 Defense
    const int GEM_PP_YELLOW_DEF = 40126;   // Thick King's Amber +20 Defense
    const int GEM_PP_BLUE = 40119;         // Solid Sky Sapphire +30 Sta
    const int GEM_PP_META = 41380;         // Austere Earthsiege Diamond
    const int GEM_PP_PRISMATIC = 49110;    // Nightmare Tear

    // protpal tank enchants
    const int ENCH_PP_HEAD = 3818;     // Arcanum of the Stalwart Protector
    const int ENCH_PP_SHOULDER = 3811; // Greater Inscription of the Pinnacle
    const int ENCH_PP_BACK = 1951;     // Titanweave
    const int ENCH_PP_CHEST = 3832;    // Powerful Stats
    const int ENCH_PP_WRIST = 3850;    // Major Stamina
    const int ENCH_PP_HANDS = 3253;    // Armsman
    const int ENCH_PP_LEGS = 3822;     // Frosthide Leg Armor
    const int ENCH_PP_FEET = 3232;     // Tuskarr's Vitality
    const int ENCH_PP_WEAPON = 3834;   // Mighty Spellpower
    const int ENCH_PP_SHIELD = 3849;   // Titanium Plating

    static int GemForProtPaladin(ItemSlot color, bool survival)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_PP_RED;
            case ItemSlot.Yellow: return survival ? GEM_PP_YELLOW_DODGE : GEM_PP_YELLOW_DEF;
            case ItemSlot.Blue: return GEM_PP_BLUE;
            case ItemSlot.Meta: return GEM_PP_META;
            case ItemSlot.Prismatic: return GEM_PP_PRISMATIC;
            default: return 0;
        }
    }

    static int EnchForSlotProtPaladin(CharacterSlot slot)
    {
        switch (slot)
        {
            case CharacterSlot.Head: return ENCH_PP_HEAD;
            case CharacterSlot.Shoulders: return ENCH_PP_SHOULDER;
            case CharacterSlot.Back: return ENCH_PP_BACK;
            case CharacterSlot.Chest: return ENCH_PP_CHEST;
            case CharacterSlot.Wrist: return ENCH_PP_WRIST;
            case CharacterSlot.Hands: return ENCH_PP_HANDS;
            case CharacterSlot.Legs: return ENCH_PP_LEGS;
            case CharacterSlot.Feet: return ENCH_PP_FEET;
            case CharacterSlot.MainHand: return ENCH_PP_WEAPON;
            case CharacterSlot.OffHand: return ENCH_PP_SHIELD;
            default: return 0;
        }
    }

    static void PE(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants, bool survival)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = RequireItem(itemId);
            if (gems)
            {
                g1 = GemForProtPaladin(item.SocketColor1, survival);
                g2 = GemForProtPaladin(item.SocketColor2, survival);
                g3 = GemForProtPaladin(item.SocketColor3, survival);
            }
            if (enchants) ench = EnchForSlotProtPaladin(slot);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    // 277 protection paladin: 5pc Sanctified Lightsworn Plate (T10 heroic prot) + heroic ICC
    // offpieces, tank trinkets (Sindragosa's Flawless Fang / Corpse Tongue Coin) and a shield.
    static void GearProtPaladin277(Character c, bool gems, bool enchants, bool survival)
    {
        PE(c, CharacterSlot.Head, 51266, gems, enchants, survival);       // Sanctified Lightsworn Faceguard
        PE(c, CharacterSlot.Neck, 50682, gems, enchants, survival);       // Bile-Encrusted Medallion
        PE(c, CharacterSlot.Shoulders, 51269, gems, enchants, survival);  // Sanctified Lightsworn Shoulderguards
        PE(c, CharacterSlot.Back, 50718, gems, enchants, survival);       // Royal Crimson Cloak
        PE(c, CharacterSlot.Chest, 51265, gems, enchants, survival);      // Sanctified Lightsworn Chestguard
        PE(c, CharacterSlot.Wrist, 50611, gems, enchants, survival);      // Bracers of Dark Reckoning
        PE(c, CharacterSlot.Hands, 51267, gems, enchants, survival);      // Sanctified Lightsworn Handguards
        PE(c, CharacterSlot.Waist, 50691, gems, enchants, survival);      // Belt of Broken Bones
        PE(c, CharacterSlot.Legs, 51268, gems, enchants, survival);       // Sanctified Lightsworn Legguards
        PE(c, CharacterSlot.Feet, 50625, gems, enchants, survival);       // Grinning Skull Greatboots
        PE(c, CharacterSlot.Finger1, 50642, gems, enchants, survival);    // Juggernaut Band
        PE(c, CharacterSlot.Finger2, 50404, gems, enchants, survival);    // Ashen Band of Endless Courage
        PE(c, CharacterSlot.Trinket1, 50364, gems, enchants, survival);   // Sindragosa's Flawless Fang (Use)
        PE(c, CharacterSlot.Trinket2, 50349, gems, enchants, survival);   // Corpse Tongue Coin (DamageTakenPhysical)
        PE(c, CharacterSlot.MainHand, 50704, gems, enchants, survival);   // Rigormortis (277)
        PE(c, CharacterSlot.OffHand, 50729, gems, enchants, survival);    // Icecrown Glacial Wall
        PE(c, CharacterSlot.Ranged, 50460, gems, enchants, survival);     // Libram of Blinding Light
    }

    // 264 protection paladin: 5pc Sanctified Lightsworn Plate (T10 normal prot) + ICC offpieces.
    static void GearProtPaladin264(Character c, bool gems, bool enchants, bool survival)
    {
        PE(c, CharacterSlot.Head, 51173, gems, enchants, survival);       // Sanctified Lightsworn Faceguard
        PE(c, CharacterSlot.Neck, 50023, gems, enchants, survival);       // Bile-Encrusted Medallion
        PE(c, CharacterSlot.Shoulders, 51170, gems, enchants, survival);  // Sanctified Lightsworn Shoulderguards
        PE(c, CharacterSlot.Back, 50074, gems, enchants, survival);       // Royal Crimson Cloak
        PE(c, CharacterSlot.Chest, 51174, gems, enchants, survival);      // Sanctified Lightsworn Chestguard
        PE(c, CharacterSlot.Wrist, 49960, gems, enchants, survival);      // Bracers of Dark Reckoning
        PE(c, CharacterSlot.Hands, 51172, gems, enchants, survival);      // Sanctified Lightsworn Handguards
        PE(c, CharacterSlot.Waist, 50036, gems, enchants, survival);      // Belt of Broken Bones
        PE(c, CharacterSlot.Legs, 51171, gems, enchants, survival);       // Sanctified Lightsworn Legguards
        PE(c, CharacterSlot.Feet, 49907, gems, enchants, survival);       // Boots of Kingly Upheaval
        PE(c, CharacterSlot.Finger1, 49985, gems, enchants, survival);    // Juggernaut Band
        PE(c, CharacterSlot.Finger2, 50447, gems, enchants, survival);    // Harbinger's Bone Band
        PE(c, CharacterSlot.Trinket1, 50356, gems, enchants, survival);   // Corroded Skeleton Key (Use)
        PE(c, CharacterSlot.Trinket2, 50352, gems, enchants, survival);   // Corpse Tongue Coin (DamageTakenPhysical)
        PE(c, CharacterSlot.MainHand, 50068, gems, enchants, survival);   // Rigormortis (264)
        PE(c, CharacterSlot.OffHand, 50065, gems, enchants, survival);    // Icecrown Glacial Wall
        PE(c, CharacterSlot.Ranged, 50460, gems, enchants, survival);     // Libram of Blinding Light
    }

    static void ProtPaladinGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Protection: Seals of the Pure (5/59/7), 277 heroic T10 5pc, raid buff, gems+enchants,
        //    default options (RankingMode 1 Mitigation Scale, Seal of Vengeance, target 83,
        //    Holy Shield on)
        {
            Character c = NewProtPaladin("ProtPaladinRaid277", ProtPaladinDefaultSpec("Protection: Seals of the Pure"), CharacterRace.Human, true);
            GearProtPaladin277(c, true, true, false);
            CalculationOptionsProtPaladin o = new CalculationOptionsProtPaladin();
            FinishProtPaladin(c, o, Path.Combine(outDir, "protpally_277_raid.xml"));
        }

        // 2. Protection: Crusade (0/53/18), 264 normal T10 5pc, no raid buffs, survival gemming,
        //    Seal of Righteousness
        {
            Character c = NewProtPaladin("ProtPaladinNoBuffs264", ProtPaladinDefaultSpec("Protection: Crusade"), CharacterRace.Tauren, false);
            GearProtPaladin264(c, true, true, true);
            CalculationOptionsProtPaladin o = new CalculationOptionsProtPaladin();
            o.SealChoice = "Seal of Righteousness";
            FinishProtPaladin(c, o, Path.Combine(outDir, "protpally_264_nobuffs.xml"));
        }

        // 3. TankPoints ranking mode with on-use trinkets handled as active, parry haste on,
        //    undead target (Crusade / Glyph of Sense Undead), 2pc only (head+shoulders offpieces),
        //    survival soft cap lowered
        {
            Character c = NewProtPaladin("ProtPaladinTankPoints", ProtPaladinDefaultSpec("Protection: Seals of the Pure"), CharacterRace.Draenei, true);
            GearProtPaladin277(c, true, true, false);
            PE(c, CharacterSlot.Head, 50640, true, true, false);      // Broken Ram Skull Helm
            PE(c, CharacterSlot.Shoulders, 50660, true, true, false); // Boneguard Commander's Pauldrons
            PE(c, CharacterSlot.Trinket1, 48021, true, true, false);  // Eitrigg's Oath (Use + DamageTaken stack child)
            PE(c, CharacterSlot.Trinket2, 50349, true, true, false);  // Corpse Tongue Coin (DamageTakenPhysical)
            PE(c, CharacterSlot.Ranged, 47664, true, true, false);    // Libram of Defiance (block value)
            CalculationOptionsProtPaladin o = new CalculationOptionsProtPaladin();
            o.RankingMode = 2;
            o.UseParryHaste = true;
            o.TrinketOnUseHandling = "Active";
            o.TargetType = "Undead";
            o.SurvivalSoftCap = 200000;
            FinishProtPaladin(c, o, Path.Combine(outDir, "protpally_tankpoints.xml"));
        }

        // 4. Level-80 target (different avoidance caps), Burst Time ranking mode, faster/harder
        //    incoming damage, parry haste on, Holy Shield off, Glyph of Sense Undead build
        {
            Character c = NewProtPaladin("ProtPaladinBoss80Burst", ProtPaladinDefaultSpec("Protection: Seals of the Pure"), CharacterRace.NightElf, true);
            GearProtPaladin264(c, true, true, false);
            CalculationOptionsProtPaladin o = new CalculationOptionsProtPaladin();
            o.TargetLevel = 80;
            o.TargetArmor = (int)StatConversion.NPC_ARMOR[0];
            o.BossAttackValue = 150000;
            o.BossAttackSpeed = 1.8f;
            o.BossAttackValueMagic = 40000;
            o.BossAttackSpeedMagic = 1.0f;
            o.UseParryHaste = true;
            o.RankingMode = 3;
            o.UseHolyShield = false;
            FinishProtPaladin(c, o, Path.Combine(outDir, "protpally_boss80_burst.xml"));
        }

        // 5. Damage Output ranking mode, high threat scale, parry haste off, 2pc T10, damage
        //    trinkets (Deathbringer's Will / Tiny Abomination in a Jar), PTR mode on
        {
            Character c = NewProtPaladin("ProtPaladinDamage", ProtPaladinDefaultSpec("Protection: Crusade"), CharacterRace.BloodElf, true);
            GearProtPaladin264(c, true, true, false);
            PE(c, CharacterSlot.Head, 51277, true, true, false);      // Sanctified Lightsworn Helmet (DPS)
            PE(c, CharacterSlot.Shoulders, 51279, true, true, false); // Sanctified Lightsworn Shoulderplates (DPS)
            PE(c, CharacterSlot.Trinket1, 50363, true, true, false);  // Deathbringer's Will (DeathbringerProc)
            PE(c, CharacterSlot.Trinket2, 50706, true, true, false);  // Tiny Abomination in a Jar (MoteOfAnger)
            PE(c, CharacterSlot.Ranged, 47662, true, true, false);    // Libram of Veracity (SoV tick)
            CalculationOptionsProtPaladin o = new CalculationOptionsProtPaladin();
            o.RankingMode = 4;
            o.ThreatScale = 15f;
            o.MitigationScale = 20000f;
            o.PTRMode = true;
            FinishProtPaladin(c, o, Path.Combine(outDir, "protpally_damage.xml"));
        }

        // 6. Crittable DPS-plate set (no defense/avoidance rating, no shield),
        //    RankingMode 5 (ProtWarr mode), custom threat/mitigation scales
        {
            Character c = NewProtPaladin("ProtPaladinCrittable", ProtPaladinDefaultSpec("Protection: Crusade"), CharacterRace.Human, true);
            PE(c, CharacterSlot.Head, 51277, true, true, false);       // Sanctified Lightsworn Helmet
            PE(c, CharacterSlot.Neck, 50728, true, true, false);       // Lana'thel's Chain of Flagellation
            PE(c, CharacterSlot.Shoulders, 51279, true, true, false);  // Sanctified Lightsworn Shoulderplates
            PE(c, CharacterSlot.Back, 50677, true, true, false);       // Winding Sheet
            PE(c, CharacterSlot.Chest, 51275, true, true, false);      // Sanctified Lightsworn Battleplate
            PE(c, CharacterSlot.Wrist, 50659, true, true, false);      // Polar Bear Claw Bracers
            PE(c, CharacterSlot.Hands, 51276, true, true, false);      // Sanctified Lightsworn Gauntlets
            PE(c, CharacterSlot.Waist, 50620, true, true, false);      // Coldwraith Links
            PE(c, CharacterSlot.Legs, 51278, true, true, false);       // Sanctified Lightsworn Legplates
            PE(c, CharacterSlot.Feet, 50639, true, true, false);       // Blood-Soaked Saronite Stompers
            PE(c, CharacterSlot.Finger1, 52572, true, true, false);    // Ashen Band of Endless Might
            PE(c, CharacterSlot.Finger2, 50657, true, true, false);    // Skeleton Lord's Circle
            PE(c, CharacterSlot.Trinket1, 50363, true, true, false);   // Deathbringer's Will (DeathbringerProc)
            PE(c, CharacterSlot.Trinket2, 50706, true, true, false);   // Tiny Abomination in a Jar (MoteOfAnger)
            PE(c, CharacterSlot.MainHand, 50704, true, true, false);   // Rigormortis (277)
            PE(c, CharacterSlot.Ranged, 47662, true, true, false);     // Libram of Veracity (SoV tick)
            CalculationOptionsProtPaladin o = new CalculationOptionsProtPaladin();
            o.RankingMode = 5;
            o.ThreatScale = 12f;
            o.MitigationScale = 17000f;
            FinishProtPaladin(c, o, Path.Combine(outDir, "protpally_crittable.xml"));
        }
    }

    // ==================================================================
    // Enhance fixture generation (Phase 6 oracle)
    // ==================================================================

    // ==================================================================
    // TankDK fixture generation (Phase 6 oracle)
    // ==================================================================

    static void TankDKMain(string outDir)
    {
        Calculations.LoadModel(typeof(CalculationsTankDK));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        TankDKGenerate(outDir);
    }

    static string ItemLineTankDK(Item item)
    {
        int fx = 0;
        foreach (SpecialEffect e in item.Stats.SpecialEffects()) fx++;
        return string.Format(CultureInfo.InvariantCulture,
            "{0} | {1} | ilvl {2} | {3} | set={4} | fx={5} | armor={6:F0} sta={7:F0} str={8:F0} agi={9:F0} def={10:F0} dodge={11:F0} parry={12:F0} hit={13:F0} exp={14:F0} arp={15:F0} crit={16:F0} haste={17:F0} ap={18:F0} sp={19:F0}",
            item.Id, item.Name, item.ItemLevel, item.Type, item.SetName, fx, item.Stats.Armor,
            item.Stats.Stamina, item.Stats.Strength, item.Stats.Agility, item.Stats.DefenseRating,
            item.Stats.DodgeRating, item.Stats.ParryRating, item.Stats.HitRating, item.Stats.ExpertiseRating,
            item.Stats.ArmorPenetrationRating, item.Stats.CritRating, item.Stats.HasteRating,
            item.Stats.AttackPower, item.Stats.SpellPower);
    }

    static void ListTankDK()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== DEATHKNIGHT TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.DeathKnight))
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));

        Console.WriteLine();
        Console.WriteLine("== TANKDK ENCHANT SPOT CHECK ==");
        int[] ids = new int[] { 3818, 3811, 1951, 3832, 3850, 3253, 3822, 3232, 3368, 3370, 3845, 3834, 1606, 3847 };
        foreach (Enchant e in Enchant.AllEnchants.Where(x => ids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | slot={2} | {3}", e.Id, e.Name, e.Slot, e.Stats == null ? "" : StatLine(e.Stats)));

        Console.WriteLine();
        Console.WriteLine("== DK SET BONUSES (Scourgelord/Thassarian/Koltira) ==");
        foreach (Buff b in Buff.AllBuffs.Where(x => x.SetName != null && (x.SetName.Contains("Scourgelord") || x.SetName.Contains("Thassarian") || x.SetName.Contains("Koltira"))).OrderBy(x => x.SetName).ThenBy(x => x.Name))
            Console.WriteLine(b.SetName + " | " + b.Name + " | threshold=" + b.SetThreshold + " | " + StatLine(b.Stats));

        Console.WriteLine();
        Console.WriteLine("== PLATE TANK ITEMS (epic, ilvl>=245, sta>0, str/def/dodge/parry) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245 && i.Type == ItemType.Plate
                    && i.Stats.Stamina > 0
                    && (i.Stats.Strength > 0 || i.Stats.DefenseRating > 0 || i.Stats.DodgeRating > 0 || i.Stats.ParryRating > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(30))
                Console.WriteLine(ItemLineTankDK(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245
                    && i.Stats.Stamina > 0
                    && (i.Stats.Strength > 0 || i.Stats.DefenseRating > 0 || i.Stats.DodgeRating > 0 || i.Stats.ParryRating > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(24))
                Console.WriteLine(ItemLineTankDK(item));
        }
        Console.WriteLine("-- Trinket (ilvl>=245, epic) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Trinket && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245
                && (i.Stats.Stamina > 0 || i.Stats.DodgeRating > 0 || i.Stats.ParryRating > 0 || i.Stats.Armor > 0))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(30))
            Console.WriteLine(ItemLineTankDK(item));
        Console.WriteLine("-- TwoHand weapons (epic, ilvl>=245) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.TwoHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 245)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(40))
            Console.WriteLine(ItemLineTankDK(item));
        Console.WriteLine("-- Ranged (Sigils) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Type == ItemType.Sigil)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(40))
            Console.WriteLine(ItemLineTankDK(item));
    }

    static Character NewTankDK(string name, DeathKnightTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.DeathKnight;
        c.CurrentModel = "TankDK";
        c.DeathKnightTalents = talents;
        if (raidBuffs)
            c.ActiveBuffsAdd("Blessing of Kings");
        return c;
    }

    static void FinishTankDK(Character c, CalculationOptionsTankDK options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    // tankdk gems (from CalculationsTankDK.DefaultGemmingTemplates, epic tier)
    const int GEM_DK_RED = 40115;          // Subtle Cardinal Ruby +20 Dodge
    const int GEM_DK_YELLOW_DEF = 40126;   // Thick King's Amber +20 Defense
    const int GEM_DK_YELLOW_DODGE_DEF = 40160; // Stalwart Ametrine +10 Dodge +10 Def
    const int GEM_DK_BLUE = 40119;         // Solid Sky Sapphire +30 Sta
    const int GEM_DK_META = 41380;         // Austere Earthsiege Diamond
    const int GEM_DK_PRISMATIC = 49110;    // Nightmare Tear

    // tankdk enchants
    const int ENCH_DK_HEAD = 3818;     // Arcanum of the Stalwart Protector
    const int ENCH_DK_SHOULDER = 3811; // Greater Inscription of the Pinnacle
    const int ENCH_DK_BACK = 1951;     // Titanweave
    const int ENCH_DK_CHEST = 3832;    // Powerful Stats
    const int ENCH_DK_WRIST = 3850;    // Major Stamina
    const int ENCH_DK_HANDS = 3253;    // Armsman
    const int ENCH_DK_LEGS = 3822;     // Frosthide Leg Armor
    const int ENCH_DK_FEET = 3232;     // Tuskarr's Vitality
    const int ENCH_DK_FC = 3368;       // Rune of the Fallen Crusader
    const int ENCH_DK_STONESKIN = 3847; // Rune of the Stoneskin Gargoyle
    const int ENCH_DK_RAZORICE = 3370; // Rune of Razorice

    static int GemForTankDK(ItemSlot color, bool survival)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_DK_RED;
            case ItemSlot.Yellow: return survival ? GEM_DK_YELLOW_DEF : GEM_DK_YELLOW_DODGE_DEF;
            case ItemSlot.Blue: return GEM_DK_BLUE;
            case ItemSlot.Meta: return GEM_DK_META;
            case ItemSlot.Prismatic: return GEM_DK_PRISMATIC;
            default: return 0;
        }
    }

    static int EnchForSlotTankDK(CharacterSlot slot, int weaponEnchant)
    {
        switch (slot)
        {
            case CharacterSlot.Head: return ENCH_DK_HEAD;
            case CharacterSlot.Shoulders: return ENCH_DK_SHOULDER;
            case CharacterSlot.Back: return ENCH_DK_BACK;
            case CharacterSlot.Chest: return ENCH_DK_CHEST;
            case CharacterSlot.Wrist: return ENCH_DK_WRIST;
            case CharacterSlot.Hands: return ENCH_DK_HANDS;
            case CharacterSlot.Legs: return ENCH_DK_LEGS;
            case CharacterSlot.Feet: return ENCH_DK_FEET;
            case CharacterSlot.MainHand: return weaponEnchant;
            default: return 0;
        }
    }

    static void TE(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants, bool survival, int weaponEnchant)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = RequireItem(itemId);
            if (gems)
            {
                g1 = GemForTankDK(item.SocketColor1, survival);
                g2 = GemForTankDK(item.SocketColor2, survival);
                g3 = GemForTankDK(item.SocketColor3, survival);
            }
            if (enchants) ench = EnchForSlotTankDK(slot, weaponEnchant);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    // 277 blood tank: 5pc Sanctified Scourgelord's Plate (T10 heroic tank) + heroic ICC offpieces,
    // 2H Glorenzelg and the Sindragosa's Flawless Fang / Deathbringer's Will trinkets.
    static void GearTankDK277(Character c, bool gems, bool enchants, bool survival, int weaponEnchant)
    {
        TE(c, CharacterSlot.Head, 51306, gems, enchants, survival, weaponEnchant);      // Sanctified Scourgelord Faceguard
        TE(c, CharacterSlot.Neck, 50682, gems, enchants, survival, weaponEnchant);      // Bile-Encrusted Medallion
        TE(c, CharacterSlot.Shoulders, 51309, gems, enchants, survival, weaponEnchant); // Sanctified Scourgelord Pauldrons
        TE(c, CharacterSlot.Back, 50718, gems, enchants, survival, weaponEnchant);      // Royal Crimson Cloak
        TE(c, CharacterSlot.Chest, 51305, gems, enchants, survival, weaponEnchant);     // Sanctified Scourgelord Chestguard
        TE(c, CharacterSlot.Wrist, 50611, gems, enchants, survival, weaponEnchant);     // Bracers of Dark Reckoning
        TE(c, CharacterSlot.Hands, 51307, gems, enchants, survival, weaponEnchant);     // Sanctified Scourgelord Handguards
        TE(c, CharacterSlot.Waist, 50691, gems, enchants, survival, weaponEnchant);     // Belt of Broken Bones
        TE(c, CharacterSlot.Legs, 51308, gems, enchants, survival, weaponEnchant);      // Sanctified Scourgelord Legguards
        TE(c, CharacterSlot.Feet, 50625, gems, enchants, survival, weaponEnchant);      // Grinning Skull Greatboots
        TE(c, CharacterSlot.Finger1, 50642, gems, enchants, survival, weaponEnchant);   // Juggernaut Band
        TE(c, CharacterSlot.Finger2, 50404, gems, enchants, survival, weaponEnchant);   // Ashen Band of Endless Courage
        TE(c, CharacterSlot.Trinket1, 50364, gems, enchants, survival, weaponEnchant);  // Sindragosa's Flawless Fang (Use)
        TE(c, CharacterSlot.Trinket2, 50363, gems, enchants, survival, weaponEnchant);  // Deathbringer's Will (proc)
        TE(c, CharacterSlot.MainHand, 50730, gems, enchants, survival, weaponEnchant);  // Glorenzelg, High-Blade of the Silver Hand
        TE(c, CharacterSlot.Ranged, 50462, gems, enchants, survival, weaponEnchant);    // Sigil of the Bone Gryphon
    }

    // 264 blood tank: 5pc Sanctified Scourgelord's Plate (T10 normal tank) + ICC offpieces,
    // Shadow's Edge and the Corroded Skeleton Key / Corpse Tongue Coin trinkets.
    static void GearTankDK264(Character c, bool gems, bool enchants, bool survival, int weaponEnchant)
    {
        TE(c, CharacterSlot.Head, 51133, gems, enchants, survival, weaponEnchant);      // Sanctified Scourgelord Faceguard
        TE(c, CharacterSlot.Neck, 50023, gems, enchants, survival, weaponEnchant);      // Bile-Encrusted Medallion
        TE(c, CharacterSlot.Shoulders, 51130, gems, enchants, survival, weaponEnchant); // Sanctified Scourgelord Pauldrons
        TE(c, CharacterSlot.Back, 50074, gems, enchants, survival, weaponEnchant);      // Royal Crimson Cloak
        TE(c, CharacterSlot.Chest, 51134, gems, enchants, survival, weaponEnchant);     // Sanctified Scourgelord Chestguard
        TE(c, CharacterSlot.Wrist, 49960, gems, enchants, survival, weaponEnchant);     // Bracers of Dark Reckoning
        TE(c, CharacterSlot.Hands, 51132, gems, enchants, survival, weaponEnchant);     // Sanctified Scourgelord Handguards
        TE(c, CharacterSlot.Waist, 50036, gems, enchants, survival, weaponEnchant);     // Belt of Broken Bones
        TE(c, CharacterSlot.Legs, 51131, gems, enchants, survival, weaponEnchant);      // Sanctified Scourgelord Legguards
        TE(c, CharacterSlot.Feet, 49907, gems, enchants, survival, weaponEnchant);      // Boots of Kingly Upheaval
        TE(c, CharacterSlot.Finger1, 49985, gems, enchants, survival, weaponEnchant);   // Juggernaut Band
        TE(c, CharacterSlot.Finger2, 50447, gems, enchants, survival, weaponEnchant);   // Harbinger's Bone Band
        TE(c, CharacterSlot.Trinket1, 50356, gems, enchants, survival, weaponEnchant);  // Corroded Skeleton Key (Use)
        TE(c, CharacterSlot.Trinket2, 50352, gems, enchants, survival, weaponEnchant);  // Corpse Tongue Coin (DamageTakenPhysical)
        TE(c, CharacterSlot.MainHand, 49888, gems, enchants, survival, weaponEnchant);  // Shadow's Edge
        TE(c, CharacterSlot.Ranged, 47672, gems, enchants, survival, weaponEnchant);    // Sigil of Insolence
    }

    // Blood tank talents: heavy Blood tree so Rotation.GetRotationByTalents picks Type.Blood.
    static DeathKnightTalents BloodTankTalents(bool glyphs)
    {
        DeathKnightTalents t = new DeathKnightTalents();
        // Blood
        t.Butchery = 2;
        t.Subversion = 3;
        t.BladeBarrier = 5;
        t.BladedArmor = 5;
        t.TwoHandedWeaponSpecialization = 2;
        t.RuneTap = 1;
        t.DarkConviction = 5;
        t.ImprovedRuneTap = 3;
        t.SpellDeflection = 3;
        t.BloodyStrikes = 3;
        t.VeteranOfTheThirdWar = 3;
        t.MarkOfBlood = 1;
        t.BloodyVengeance = 3;
        t.AbominationsMight = 2;
        t.Bloodworms = 3;
        t.ImprovedBloodPresence = 2;
        t.ImprovedDeathStrike = 2;
        t.SuddenDoom = 3;
        t.VampiricBlood = 1;
        t.WillOfTheNecropolis = 3;
        t.HeartStrike = 1;
        t.MightOfMograine = 3;
        t.BloodGorged = 3;
        // Frost splash
        t.ImprovedIcyTouch = 3;
        t.Toughness = 5;
        t.IcyTalons = 5;
        t.BlackIce = 3;
        // Unholy splash
        t.Virulence = 3;
        t.Anticipation = 3;
        t.Epidemic = 2;
        t.Morbidity = 3;
        t.Impurity = 5;
        t.RageOfRivendare = 3;
        t.ScourgeStrike = 1;
        if (glyphs)
        {
            t.GlyphofBoneShield = true;
            t.GlyphofVampiricBlood = true;
            t.GlyphofAntiMagicShell = true;
            t.GlyphofDarkDeath = true;
            t.GlyphofIcyTouch = true;
            t.GlyphofDeathStrike = true;
        }
        return t;
    }

    // Frost tank talents: heavy Frost tree so Rotation.GetRotationByTalents picks Type.Frost.
    static DeathKnightTalents FrostTankTalents()
    {
        DeathKnightTalents t = new DeathKnightTalents();
        // Blood splash
        t.BladeBarrier = 5;
        t.BladedArmor = 5;
        t.DarkConviction = 5;
        t.TwoHandedWeaponSpecialization = 2;
        t.VeteranOfTheThirdWar = 3;
        // Frost
        t.ImprovedIcyTouch = 3;
        t.RunicPowerMastery = 2;
        t.Toughness = 5;
        t.IcyTalons = 5;
        t.BlackIce = 3;
        t.NervesOfColdSteel = 3;
        t.Annihilation = 3;
        t.KillingMachine = 5;
        t.ChillOfTheGrave = 2;
        t.EndlessWinter = 2;
        t.FrigidDreadplate = 3;
        t.GlacierRot = 1;
        t.ImprovedIcyTalons = 1;
        t.MercilessCombat = 2;
        t.Rime = 3;
        t.ImprovedFrostPresence = 2;
        t.ThreatOfThassarian = 3;
        t.BloodOfTheNorth = 3;
        t.UnbreakableArmor = 1;
        t.Acclimation = 3;
        t.FrostStrike = 1;
        t.GuileOfGorefiend = 3;
        t.TundraStalker = 3;
        t.HowlingBlast = 1;
        // Unholy splash
        t.Virulence = 3;
        t.Epidemic = 2;
        t.Impurity = 5;
        t.GlyphofHowlingBlast = true;
        t.GlyphofFrostStrike = true;
        t.GlyphofRuneStrike = true;
        t.GlyphofIceboundFortitude = true;
        t.GlyphofUnbreakableArmor = true;
        return t;
    }

    // Survival-focused blood tank talents: Bone Shield, Vampiric Blood, Rune Tap, Acclimation,
    // Magic Suppression, Will of the Necropolis, Anti-Magic Zone.
    static DeathKnightTalents SurvivalTankTalents()
    {
        DeathKnightTalents t = BloodTankTalents(true);
        t.Acclimation = 3;
        t.UnbreakableArmor = 1;
        t.GuileOfGorefiend = 3;
        t.MagicSuppression = 3;
        t.AntiMagicZone = 1;
        t.BoneShield = 3;
        t.WanderingPlague = 1;
        t.CryptFever = 3;
        t.Desolation = 3;
        t.ViciousStrikes = 3;
        t.Outbreak = 3;
        t.Necrosis = 3;
        t.BloodCakedBlade = 5;
        t.RavenousDead = 3;
        t.EbonPlaguebringer = 3;
        t.RageOfRivendare = 3;
        t.GlyphofDeathandDecay = true;
        t.GlyphofObliterate = true;
        t.GlyphofPlagueStrike = true;
        return t;
    }

    static void TankDKGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Blood tank 277 heroic T10 5pc, raid buff, Fallen Crusader, default SMT options.
        {
            Character c = NewTankDK("TankDKBlood277", BloodTankTalents(true), CharacterRace.Human, true);
            GearTankDK277(c, true, true, false, ENCH_DK_FC);
            CalculationOptionsTankDK o = new CalculationOptionsTankDK();
            FinishTankDK(c, o, Path.Combine(outDir, "tankdk_blood_277.xml"));
        }

        // 2. Blood tank 264 normal T10 5pc, no raid buffs, defense-heavy survival gemming,
        //    Stoneskin Gargoyle, level-80 target.
        {
            Character c = NewTankDK("TankDKBlood264NoBuffs", BloodTankTalents(false), CharacterRace.Tauren, false);
            GearTankDK264(c, true, true, true, ENCH_DK_STONESKIN);
            CalculationOptionsTankDK o = new CalculationOptionsTankDK();
            o.TargetLevel = 80;
            FinishTankDK(c, o, Path.Combine(outDir, "tankdk_blood_264_nobuffs.xml"));
        }

        // 3. Survival weighting: 4pc T9 Thassarian's Plate (m_bT9_4PC), Bone Shield / Vampiric Blood /
        //    Anti-Magic Zone / Acclimation talents, additive mitigation, on-use trinkets inactive,
        //    parry haste off, custom incoming damage and resistance-relevant trinkets.
        {
            Character c = NewTankDK("TankDKSurvivalT9", SurvivalTankTalents(), CharacterRace.Orc, true);
            GearTankDK277(c, true, true, true, ENCH_DK_STONESKIN);
            TE(c, CharacterSlot.Head, 48545, true, true, true, ENCH_DK_STONESKIN);      // Thassarian's Faceguard of Triumph
            TE(c, CharacterSlot.Shoulders, 50660, true, true, true, ENCH_DK_STONESKIN); // Boneguard Commander's Pauldrons
            TE(c, CharacterSlot.Chest, 48547, true, true, true, ENCH_DK_STONESKIN);     // Thassarian's Chestguard of Triumph
            TE(c, CharacterSlot.Hands, 48546, true, true, true, ENCH_DK_STONESKIN);     // Thassarian's Handguards of Triumph
            TE(c, CharacterSlot.Legs, 48544, true, true, true, ENCH_DK_STONESKIN);      // Thassarian's Legguards of Triumph
            TE(c, CharacterSlot.Trinket1, 48021, true, true, true, ENCH_DK_STONESKIN);  // Eitrigg's Oath (Use)
            TE(c, CharacterSlot.Trinket2, 50349, true, true, true, ENCH_DK_STONESKIN);  // Corpse Tongue Coin (DamageTakenPhysical)
            TE(c, CharacterSlot.Ranged, 50459, true, true, true, ENCH_DK_STONESKIN);    // Sigil of the Hanged Man
            CalculationOptionsTankDK o = new CalculationOptionsTankDK();
            o.SurvivalWeight = 2.5f;
            o.MitigationWeight = 6f;
            o.ThreatWeight = 0.5f;
            o.AdditiveMitigation = true;
            o.bUseOnUseAbilities = false;
            o.bParryHaste = false;
            o.IncomingDamage = 90000;
            o.BossAttackSpeed = 1.6f;
            o.IncomingMagicDamage = 30000;
            o.IncomingFromMagicFrequency = 10f;
            o.IncomingBleedDamage = 8000;
            o.BleedTickFrequency = 1f;
            o.FightLength = 8f;
            FinishTankDK(c, o, Path.Combine(outDir, "tankdk_survival_t9.xml"));
        }

        // 4. Burst ranking mode (BurstTime + ReactionTime subpoints), Bryntroll + razorice, damage
        //    proc trinkets (Tiny Abomination in a Jar / Deathbringer's Will), 5 minute fight.
        {
            Character c = NewTankDK("TankDKProcsBurst", BloodTankTalents(true), CharacterRace.Draenei, true);
            GearTankDK277(c, true, true, false, ENCH_DK_RAZORICE);
            TE(c, CharacterSlot.MainHand, 50709, true, true, false, ENCH_DK_RAZORICE);   // Bryntroll, the Bone Arbiter
            TE(c, CharacterSlot.Trinket1, 50706, true, true, false, ENCH_DK_RAZORICE);   // Tiny Abomination in a Jar
            TE(c, CharacterSlot.Trinket2, 50363, true, true, false, ENCH_DK_RAZORICE);   // Deathbringer's Will
            TE(c, CharacterSlot.Ranged, 50462, true, true, false, ENCH_DK_RAZORICE);     // Sigil of the Bone Gryphon
            CalculationOptionsTankDK o = new CalculationOptionsTankDK();
            o.cType = CalculationType.Burst;
            o.FightLength = 5f;
            FinishTankDK(c, o, Path.Combine(outDir, "tankdk_procs_burst.xml"));
        }

        // 5. Frost tank (frost-heavy build -> Frost rotation with Howling Blast / Rime / Frost
        //    Strike), 277 set with Fallen Crusader, raid buff, target 83.
        {
            Character c = NewTankDK("TankDKFrostTank", FrostTankTalents(), CharacterRace.Dwarf, true);
            GearTankDK277(c, true, true, false, ENCH_DK_FC);
            TE(c, CharacterSlot.MainHand, 50603, true, true, false, ENCH_DK_FC);         // Cryptmaker
            TE(c, CharacterSlot.Trinket1, 50363, true, true, false, ENCH_DK_FC);         // Deathbringer's Will
            TE(c, CharacterSlot.Trinket2, 50349, true, true, false, ENCH_DK_FC);         // Corpse Tongue Coin
            TE(c, CharacterSlot.Ranged, 51417, true, true, false, ENCH_DK_FC);           // Wrathful Gladiator's Sigil of Strife
            CalculationOptionsTankDK o = new CalculationOptionsTankDK();
            o.uNumberTargets = 2;
            FinishTankDK(c, o, Path.Combine(outDir, "tankdk_frost_tank.xml"));
        }
    }

    static void EnhanceMain(string outDir) {
        Calculations.LoadModel(typeof(CalculationsEnhance));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        EnhanceGenerate(outDir);
    }

    // The shipped DefaultDataFiles/Talents.xml has no enhancement shaman spec (only Untalented and
    // Default Resto), so the build is constructed property by property like the Elemental balance
    // spec: every talent the Enhance model reads, plus all enhancement glyphs.
    static ShamanTalents EnhanceTalents()
    {
        ShamanTalents t = new ShamanTalents();
        // Elemental
        t.Convection = 5; t.Concussion = 5; t.ElementalWarding = 3; t.ElementalDevastation = 3;
        t.ElementalFocus = 1; t.ElementalFury = 5; t.ImprovedFireNova = 2; t.CallOfThunder = 3;
        t.ElementalPrecision = 1; t.LightningMastery = 5; t.ElementalOath = 2; t.Shamanism = 5;
        // Enhancement
        t.EnhancingTotems = 3; t.AncestralKnowledge = 5; t.GuardianTotems = 2;
        t.ThunderingStrikes = 5; t.ImprovedShields = 3; t.ElementalWeapons = 3;
        t.ShamanisticFocus = 1; t.Anticipation = 1; t.Flurry = 5; t.Toughness = 5;
        t.ImprovedWindfuryTotem = 2; t.SpiritWeapons = 1; t.MentalDexterity = 3;
        t.UnleashedRage = 3; t.WeaponMastery = 3; t.DualWieldSpecialization = 3;
        t.DualWield = 1; t.Stormstrike = 1; t.StaticShock = 3; t.LavaLash = 1;
        t.ImprovedStormstrike = 2; t.MentalQuickness = 3; t.ShamanisticRage = 1;
        t.MaelstromWeapon = 5; t.FeralSpirit = 1;
        // Restoration
        t.TidalMastery = 5; t.TotemicFocus = 5; t.ImprovedWaterShield = 3; t.TidalWaves = 3;
        t.NaturesBlessing = 3;
        // glyphs
        t.GlyphofFeralSpirit = true; t.GlyphofFlametongueWeapon = true; t.GlyphofLavaLash = true;
        t.GlyphofLightningShield = true; t.GlyphofLightningBolt = true; t.GlyphofShocking = true;
        t.GlyphofFlameShock = true; t.GlyphofStormstrike = true; t.GlyphofWindfuryWeapon = true;
        t.GlyphofFireElementalTotem = true;
        return t;
    }

    static string ItemLineEnhance(Item item)
    {
        int fx = 0;
        foreach (SpecialEffect e in item.Stats.SpecialEffects()) fx++;
        return string.Format(CultureInfo.InvariantCulture,
            "{0} | {1} | ilvl {2} | {3} | set={4} | fx={5} | agi={6:F0} str={7:F0} ap={8:F0} crit={9:F0} hit={10:F0} exp={11:F0} arp={12:F0} haste={13:F0} sp={14:F0} sta={15:F0}",
            item.Id, item.Name, item.ItemLevel, item.Type, item.SetName, fx,
            item.Stats.Agility, item.Stats.Strength, item.Stats.AttackPower, item.Stats.CritRating,
            item.Stats.HitRating, item.Stats.ExpertiseRating, item.Stats.ArmorPenetrationRating,
            item.Stats.HasteRating, item.Stats.SpellPower, item.Stats.Stamina);
    }

    static void ListEnhance()
    {
        Console.WriteLine("== ENHANCE TALENTS (constructed, no enhancement spec shipped) ==");
        PrintTalents("EnhanceTalents", EnhanceTalents());

        Console.WriteLine();
        Console.WriteLine("== ENH ENCHANT SPOT CHECK ==");
        int[] ids = new int[] { 3817, 3808, 1099, 3832, 3845, 1603, 3823, 1597, 3789, 2673, 3827, 3368 };
        foreach (Enchant e in Enchant.AllEnchants.Where(x => ids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | slot={2} | {3}", e.Id, e.Name, e.Slot, e.Stats == null ? "" : StatLineEnhance(e.Stats)));

        Console.WriteLine();
        Console.WriteLine("== ENH GEM SPOT CHECK ==");
        int[] gids = new int[] { 40111, 42142, 40112, 42143, 39914, 40124, 40147, 40148, 40130, 40125, 40162, 41398, 41285, 49110 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLineEnhance(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== ENH SET BONUSES (Frost Witch's Battlegear) ==");
        foreach (Buff b in Buff.AllBuffs.Where(x => x.SetName == "Frost Witch's Battlegear").OrderBy(x => x.Name))
            Console.WriteLine(b.Name + " | threshold=" + b.SetThreshold + " | " + StatLineEnhance(b.Stats));

        Console.WriteLine();
        Console.WriteLine("== MAIL ARMOR (epic, ilvl>=251, melee, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 251 && i.Type == ItemType.Mail
                    && (i.Stats.Agility > 0 || i.Stats.Strength > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
                Console.WriteLine(ItemLineEnhance(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 258
                    && (i.Stats.Agility > 0 || i.Stats.Strength > 0 || i.Stats.ArmorPenetrationRating > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
                Console.WriteLine(ItemLineEnhance(item));
        }
        Console.WriteLine("-- Trinket (ilvl>=264, epic) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Trinket && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(40))
            Console.WriteLine(ItemLineEnhance(item));
        Console.WriteLine("-- MainHand (one-hand melee, ilvl>=258) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.MainHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 258
                && (i.Type == ItemType.OneHandAxe || i.Type == ItemType.OneHandMace || i.Type == ItemType.Dagger
                    || i.Type == ItemType.FistWeapon || i.Type == ItemType.OneHandSword)
                && (i.Stats.Agility > 0 || i.Stats.Strength > 0 || i.Stats.AttackPower > 0))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(30))
            Console.WriteLine(ItemLineEnhance(item));
        Console.WriteLine("-- OffHand (one-hand melee, ilvl>=258) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.OffHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 258
                && (i.Type == ItemType.OneHandAxe || i.Type == ItemType.OneHandMace || i.Type == ItemType.Dagger
                    || i.Type == ItemType.FistWeapon || i.Type == ItemType.OneHandSword)
                && (i.Stats.Agility > 0 || i.Stats.Strength > 0 || i.Stats.AttackPower > 0))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLineEnhance(item));
        Console.WriteLine("-- Ranged (Totems with melee stats) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Type == ItemType.Totem && i.Quality == ItemQuality.Epic)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(30))
            Console.WriteLine(ItemLineEnhance(item));
    }

    static string StatLineEnhance(Stats s)
    {
        return string.Format(CultureInfo.InvariantCulture,
            "agi={0:F0} str={1:F0} ap={2:F0} crit={3:F0} hit={4:F0} exp={5:F0} arp={6:F0} haste={7:F0} sp={8:F0} wdmg={9:F0}",
            s.Agility, s.Strength, s.AttackPower, s.CritRating, s.HitRating, s.ExpertiseRating,
            s.ArmorPenetrationRating, s.HasteRating, s.SpellPower, s.WeaponDamage);
    }

    static int GemForEnhance(ItemSlot color, int yellowGem)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_ENH_RED;
            case ItemSlot.Yellow: return yellowGem;
            case ItemSlot.Blue: return GEM_ENH_BLUE;
            case ItemSlot.Meta: return GEM_ENH_META;
            case ItemSlot.Prismatic: return GEM_PRISMATIC;
            default: return 0;
        }
    }

    static void HE(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants, int yellowGem)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = RequireItem(itemId);
            if (gems)
            {
                g1 = GemForEnhance(item.SocketColor1, yellowGem);
                g2 = GemForEnhance(item.SocketColor2, yellowGem);
                g3 = GemForEnhance(item.SocketColor3, yellowGem);
            }
            if (enchants) ench = EnchForSlotEnhance(slot);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    static int EnchForSlotEnhance(CharacterSlot slot)
    {
        switch (slot)
        {
            case CharacterSlot.Head: return ENCH_ENH_HEAD;
            case CharacterSlot.Shoulders: return ENCH_ENH_SHOULDER;
            case CharacterSlot.Back: return ENCH_ENH_BACK;
            case CharacterSlot.Chest: return ENCH_ENH_CHEST;
            case CharacterSlot.Wrist: return ENCH_ENH_WRIST;
            case CharacterSlot.Hands: return ENCH_ENH_HANDS;
            case CharacterSlot.Legs: return ENCH_ENH_LEGS;
            case CharacterSlot.Feet: return ENCH_ENH_FEET;
            case CharacterSlot.MainHand: return ENCH_ENH_MH;
            case CharacterSlot.OffHand: return ENCH_ENH_OH;
            default: return 0;
        }
    }

    static Character NewEnhance(string name, ShamanTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Shaman;
        c.CurrentModel = "Enhance";
        c.ShamanTalents = talents;
        if (raidBuffs)
            ((CalculationsEnhance)Calculations.Instance).SetDefaults(c);
        else
            c.ActiveBuffsAdd("Strength of Earth Totem");
        return c;
    }

    static void FinishEnhance(Character c, CalculationOptionsEnhance options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    // 277 enhance: 5pc Sanctified Frost Witch's Battlegear (T10 heroic) plus ICC 25 heroic melee
    // mail offpieces, proc trinkets (Deathbringer's Will / Tiny Abomination), dual one-handers.
    static void GearEnhance277(Character c, bool gems, bool enchants, int yellow)
    {
        HE(c, CharacterSlot.Head, 51242, gems, enchants, yellow);        // Sanctified Frost Witch's Faceguard
        HE(c, CharacterSlot.Neck, 50633, gems, enchants, yellow);        // Sindragosa's Cruel Claw
        HE(c, CharacterSlot.Shoulders, 51240, gems, enchants, yellow);   // Sanctified Frost Witch's Shoulderguards
        HE(c, CharacterSlot.Back, 50653, gems, enchants, yellow);        // Shadowvault Slayer's Cloak
        HE(c, CharacterSlot.Chest, 51244, gems, enchants, yellow);       // Sanctified Frost Witch's Chestguard
        HE(c, CharacterSlot.Wrist, 50655, gems, enchants, yellow);       // Scourge Hunter's Vambraces
        HE(c, CharacterSlot.Hands, 51243, gems, enchants, yellow);       // Sanctified Frost Witch's Grips
        HE(c, CharacterSlot.Waist, 50688, gems, enchants, yellow);       // Nerub'ar Stalker's Cord
        HE(c, CharacterSlot.Legs, 51241, gems, enchants, yellow);        // Sanctified Frost Witch's War-Kilt
        HE(c, CharacterSlot.Feet, 50711, gems, enchants, yellow);        // Treads of the Wasteland
        HE(c, CharacterSlot.Finger1, 52572, gems, enchants, yellow);     // Ashen Band of Endless Might
        HE(c, CharacterSlot.Finger2, 50657, gems, enchants, yellow);     // Skeleton Lord's Circle
        HE(c, CharacterSlot.Trinket1, 50363, gems, enchants, yellow);    // Deathbringer's Will
        HE(c, CharacterSlot.Trinket2, 50706, gems, enchants, yellow);    // Tiny Abomination in a Jar
        HE(c, CharacterSlot.MainHand, 50692, gems, enchants, yellow);    // Black Bruise
        HE(c, CharacterSlot.OffHand, 50710, gems, enchants, yellow);     // Keleseth's Seducer
        HE(c, CharacterSlot.Ranged, 50463, gems, enchants, yellow);      // Totem of the Avalanche
    }

    // 264 enhance: 5pc Sanctified Frost Witch's Battlegear (T10 normal) plus ICC 25 melee mail
    // offpieces, a Use trinket and a Paragon stat-proc trinket.
    static void GearEnhance264(Character c, bool gems, bool enchants, int yellow)
    {
        HE(c, CharacterSlot.Head, 51197, gems, enchants, yellow);        // Sanctified Frost Witch's Faceguard
        HE(c, CharacterSlot.Neck, 50421, gems, enchants, yellow);        // Sindragosa's Cruel Claw
        HE(c, CharacterSlot.Shoulders, 51199, gems, enchants, yellow);   // Sanctified Frost Witch's Shoulderguards
        HE(c, CharacterSlot.Back, 49998, gems, enchants, yellow);        // Shadowvault Slayer's Cloak
        HE(c, CharacterSlot.Chest, 51195, gems, enchants, yellow);       // Sanctified Frost Witch's Chestguard
        HE(c, CharacterSlot.Wrist, 50000, gems, enchants, yellow);       // Scourge Hunter's Vambraces
        HE(c, CharacterSlot.Hands, 51196, gems, enchants, yellow);       // Sanctified Frost Witch's Grips
        HE(c, CharacterSlot.Waist, 50413, gems, enchants, yellow);       // Nerub'ar Stalker's Cord
        HE(c, CharacterSlot.Legs, 51198, gems, enchants, yellow);        // Sanctified Frost Witch's War-Kilt
        HE(c, CharacterSlot.Feet, 50071, gems, enchants, yellow);        // Treads of the Wasteland
        HE(c, CharacterSlot.Finger1, 50402, gems, enchants, yellow);     // Ashen Band of Endless Vengeance
        HE(c, CharacterSlot.Finger2, 49949, gems, enchants, yellow);     // Band of the Bone Colossus
        HE(c, CharacterSlot.Trinket1, 47734, gems, enchants, yellow);    // Mark of Supremacy (Use)
        HE(c, CharacterSlot.Trinket2, 47115, gems, enchants, yellow);    // Death's Verdict (Paragon)
        HE(c, CharacterSlot.MainHand, 50184, gems, enchants, yellow);    // Keleseth's Seducer
        HE(c, CharacterSlot.OffHand, 51819, gems, enchants, yellow);     // Splintershard
        HE(c, CharacterSlot.Ranged, 50463, gems, enchants, yellow);      // Totem of the Avalanche
    }

    static void EnhanceGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Full raid buffs, 264 T10 Battlegear 5pc, gems+enchants, default options
        {
            Character c = NewEnhance("EnhanceRaid264", EnhanceTalents(), CharacterRace.Orc, true);
            GearEnhance264(c, true, true, GEM_ENH_RED);
            CalculationOptionsEnhance o = new CalculationOptionsEnhance();
            FinishEnhance(c, o, Path.Combine(outDir, "enhance_264_raid.xml"));
        }

        // 2. 277 heroic T10 5pc + proc trinkets, gems+enchants (agi/hit), full raid buffs
        {
            Character c = NewEnhance("EnhanceProcs277", EnhanceTalents(), CharacterRace.Troll, true);
            GearEnhance277(c, true, true, GEM_ENH_HIT);
            CalculationOptionsEnhance o = new CalculationOptionsEnhance();
            o.FightLength = 5f;
            FinishEnhance(c, o, Path.Combine(outDir, "enhance_277_procs.xml"));
        }

        // 3. No raid buffs, 264 gear, short 60s fight, no Fire Elemental/Magma, few priorities
        {
            Character c = NewEnhance("EnhanceNoBuffs60", EnhanceTalents(), CharacterRace.Draenei, false);
            GearEnhance264(c, true, true, GEM_ENH_RED);
            CalculationOptionsEnhance o = new CalculationOptionsEnhance();
            o.FightLength = 1f;
            FinishEnhance(c, o, Path.Combine(outDir, "enhance_nobuffs_short.xml"));
        }

        // 4. Long 300s fight with Fire Elemental on, Searing totem, higher latency/reaction,
        //    multiple targets, 277 gear
        {
            Character c = NewEnhance("EnhanceFireEle300", EnhanceTalents(), CharacterRace.Orc, true);
            GearEnhance277(c, true, true, GEM_ENH_RED);
            CalculationOptionsEnhance o = new CalculationOptionsEnhance();
            o.FightLength = 5f;
            o.AverageLag = 500;
            o.ReactionTime = 400;
            o.MultipleTargets = true;
            o.AdditionalTargets = 2;
            o.AdditionalTargetPercent = 0.25f;
            o.SetAbilityPriority(EnhanceAbility.FireElemental, new Priority("Fire Elemental", EnhanceAbility.FireElemental, "Drop Fire Elemental Totem", true, 4, "FE"));
            FinishEnhance(c, o, Path.Combine(outDir, "enhance_fireele_300.xml"));
        }

        // 5. Target level 80, no glyphs, FT/FT imbues, no Feral Spirits/Static Shock, in front
        //    of the boss (parry-haste on), 264 gear
        {
            ShamanTalents t = EnhanceTalents();
            t.GlyphofFeralSpirit = false; t.GlyphofFlametongueWeapon = false; t.GlyphofLavaLash = false;
            t.GlyphofLightningShield = false; t.GlyphofLightningBolt = false; t.GlyphofShocking = false;
            t.GlyphofFlameShock = false; t.GlyphofStormstrike = false; t.GlyphofWindfuryWeapon = false;
            t.GlyphofFireElementalTotem = false;
            t.StaticShock = 0; t.FeralSpirit = 0; t.ElementalDevastation = 0; t.Flurry = 0;
            Character c = NewEnhance("EnhanceWeird80", t, CharacterRace.Tauren, true);
            GearEnhance264(c, true, true, GEM_ENH_RED);
            CalculationOptionsEnhance o = new CalculationOptionsEnhance();
            o.TargetLevel = 80;
            o.TargetArmor = (int)Rawr.StatConversion.NPC_ARMOR[0];
            o.MainhandImbue = "Flametongue";
            o.OffhandImbue = "Flametongue";
            o.InBack = false;
            o.InBackPerc = 0;
            o.MinManaSR = 0;
            FinishEnhance(c, o, Path.Combine(outDir, "enhance_weird_80.xml"));
        }
    }

    // ==================================================================
    // HealPriest fixture generation (Phase 6 oracle)
    // ==================================================================

    static void HealpriestMain(string outDir)
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Calculations.LoadModel(typeof(CalculationsHealPriest));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        HealpriestGenerate(outDir);
    }

    static SavedTalentSpec HealpriestSpec(string name)
    {
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Priest))
            if (spec.Name == name) return spec;
        throw new InvalidOperationException("priest spec not found: " + name);
    }

    static void ListHealpriest()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== PRIEST TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Priest))
        {
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));
            TalentsBase t = spec.TalentSpec();
            if (t != null) PrintTalents(spec.Name, t);
        }

        Console.WriteLine();
        Console.WriteLine("== HEALER ENCHANT SPOT CHECK ==");
        int[] ids = new int[] { 3820, 3810, 3728, 2661, 2332, 3246, 3719, 3826, 3834 };
        foreach (Enchant e in Enchant.AllEnchants.Where(x => ids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | slot={2} | {3}", e.Id, e.Name, e.Slot, e.Stats == null ? "" : StatLine(e.Stats)));

        Console.WriteLine();
        Console.WriteLine("== HEALER GEM SPOT CHECK ==");
        int[] gids = new int[] { 40113, 40151, 40155, 40175, 40133, 41401, 49110 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLine(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== CLOTH HEALER ITEMS (epic, ilvl>=264, sp/spirit/mp5, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264 && i.Type == ItemType.Cloth
                    && (i.Stats.SpellPower > 0 || i.Stats.Spirit > 0 || i.Stats.Mp5 > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
                Console.WriteLine(ItemLine(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger, ItemSlot.Trinket })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                    && (i.Stats.SpellPower > 0 || i.Stats.Mp5 > 0 || i.Stats.Spirit > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
                Console.WriteLine(ItemLine(item));
        }
        Console.WriteLine("-- OffHand --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.OffHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && i.Stats.SpellPower > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- MainHand (one-hand caster) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.MainHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && (i.Type == ItemType.OneHandMace || i.Type == ItemType.Dagger)
                && i.Stats.SpellPower > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- Ranged (Wands) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Quality == ItemQuality.Epic && i.Type == ItemType.Wand)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine(item));
    }

    static Character NewHealpriest(string name, PriestTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Priest;
        c.CurrentModel = "HealPriest";
        c.PriestTalents = talents;
        if (raidBuffs)
            ((CalculationsHealPriest)Calculations.Instance).SetDefaults(c);
        return c;
    }

    static void FinishHealpriest(Character c, CalculationOptionsHealPriest options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    static void HealpriestGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Holy spec, full raid buffs, 264 healer T10 5pc, sp/haste gemming, default options
        {
            TalentsBase t = HealpriestSpec("Holy").TalentSpec();
            if (t == null) throw new InvalidOperationException("Holy spec failed to parse");
            Character c = NewHealpriest("HealPriestRaid264", (PriestTalents)t, CharacterRace.Human, true);
            GearHealpriest264(c, true, true, GEM_HEAL_YELLOW_HASTE, GEM_HEAL_BLUE_MP5);
            CalculationOptionsHealPriest o = new CalculationOptionsHealPriest();
            FinishHealpriest(c, o, Path.Combine(outDir, "healpriest_264_raid.xml"));
        }

        // 2. Disc spec + Penance/PWS glyphs, 277 heroic T10 5pc, proc/Use trinkets, full raid buffs
        {
            TalentsBase t = HealpriestSpec("Disc").TalentSpec();
            if (t == null) throw new InvalidOperationException("Disc spec failed to parse");
            PriestTalents disc = (PriestTalents)t;
            disc.GlyphofPenance = true;
            disc.GlyphofPowerWordShield = true;
            Character c = NewHealpriest("HealPriestDisc277", disc, CharacterRace.Draenei, true);
            GearHealpriest277(c, true, true, GEM_HEAL_YELLOW_INT, GEM_HEAL_BLUE_MP5);
            CalculationOptionsHealPriest o = new CalculationOptionsHealPriest();
            FinishHealpriest(c, o, Path.Combine(outDir, "healpriest_277_disc.xml"));
        }

        // 3. No raid buffs, 264 gear, custom healing mix (AdvancedSolver), 5 min fight
        {
            TalentsBase t = HealpriestSpec("Disc").TalentSpec();
            if (t == null) throw new InvalidOperationException("Disc spec failed to parse");
            PriestTalents disc = (PriestTalents)t;
            disc.GlyphofPenance = true;
            disc.GlyphofPowerWordShield = true;
            disc.GlyphofFlashHeal = true;
            Character c = NewHealpriest("HealPriestCustom264", disc, CharacterRace.Human, false);
            GearHealpriest264(c, true, true, GEM_HEAL_YELLOW_INT, GEM_HEAL_BLUE_MP5);
            CalculationOptionsHealPriest o = new CalculationOptionsHealPriest();
            o.Role = eRole.CUSTOM;
            o.FightLengthSeconds = 300f;
            o.FSRRatio = 93f;
            o.FlashHealCast = 20;
            o.BindingHealCast = 2;
            o.GreaterHealCast = 10;
            o.PenanceCast = 6;
            o.RenewCast = 6;
            o.RenewTicks = 30;
            o.ProMCast = 4;
            o.ProMTicks = 12;
            o.PoHCast = 8;
            o.PWSCast = 12;
            o.CoHCast = 0;
            o.HolyNovaCast = 2;
            o.DivineHymnCast = 1;
            o.DispelCast = 1;
            o.MDCast = 0;
            o.Shadowfiend = 100f;
            o.Replenishment = 75f;
            o.Rapture = 25f;
            o.TestOfFaith = 25f;
            FinishHealpriest(c, o, Path.Combine(outDir, "healpriest_custom_nobuffs.xml"));
        }

        // 4. Holy spec, Blood Elf Arcane Torrent, long mana-starved 20 min fight, no mana potion,
        //    no replenishment, always in FSR
        {
            TalentsBase t = HealpriestSpec("Holy").TalentSpec();
            if (t == null) throw new InvalidOperationException("Holy spec failed to parse");
            PriestTalents holy = (PriestTalents)t;
            holy.GlyphofCircleOfHealing = true;
            holy.GlyphofRenew = true;
            holy.GlyphofHymnofHope = true;
            Character c = NewHealpriest("HealPriestManaStarved", holy, CharacterRace.BloodElf, true);
            GearHealpriest264(c, true, true, GEM_HEAL_YELLOW_HASTE, GEM_BLUE);
            CalculationOptionsHealPriest o = new CalculationOptionsHealPriest();
            o.Role = eRole.AUTO_Raid;
            o.FightLengthSeconds = 1200f;
            o.FSRRatio = 100f;
            o.Replenishment = 0f;
            o.ManaPot = 0;
            o.Survivability = 2f;
            FinishHealpriest(c, o, Path.Combine(outDir, "healpriest_manastarved.xml"));
        }

        // 5. Holy Raid Renew rotation on a short 90s burst fight, 277 gear, target 83, procs on
        {
            TalentsBase t = HealpriestSpec("Holy AoE").TalentSpec();
            if (t == null) throw new InvalidOperationException("Holy AoE spec failed to parse");
            PriestTalents holy = (PriestTalents)t;
            holy.GlyphofCircleOfHealing = true;
            Character c = NewHealpriest("HealPriestHolyRaidRenew", holy, CharacterRace.NightElf, true);
            GearHealpriest277(c, true, true, GEM_HEAL_YELLOW_HASTE, GEM_BLUE);
            CalculationOptionsHealPriest o = new CalculationOptionsHealPriest();
            o.Role = eRole.Holy_Raid_Renew;
            o.FightLengthSeconds = 90f;
            o.FSRRatio = 80f;
            o.Replenishment = 100f;
            o.TestOfFaith = 0f;
            FinishHealpriest(c, o, Path.Combine(outDir, "healpriest_holy_raid_renew.xml"));
        }
    }

    // 264 priest healer: 5pc Crimson Acolyte's Raiment (T10 normal healer) + ICC 25 offpieces,
    // dagger + held-in-offhand, wand.
    static void GearHealpriest264(Character c, bool gems, bool enchants, int yellowGem, int blueGem)
    {
        RE(c, CharacterSlot.Head, 51178, gems, enchants, yellowGem, blueGem);       // Sanctified Crimson Acolyte Hood
        RE(c, CharacterSlot.Neck, 49975, gems, enchants, yellowGem, blueGem);       // Bone Sentinel's Amulet
        RE(c, CharacterSlot.Shoulders, 51175, gems, enchants, yellowGem, blueGem);  // Sanctified Crimson Acolyte Shoulderpads
        RE(c, CharacterSlot.Back, 50014, gems, enchants, yellowGem, blueGem);       // Greatcloak of the Turned Champion
        RE(c, CharacterSlot.Chest, 51176, gems, enchants, yellowGem, blueGem);      // Sanctified Crimson Acolyte Robe
        RE(c, CharacterSlot.Wrist, 50032, gems, enchants, yellowGem, blueGem);      // Death Surgeon's Sleeves
        RE(c, CharacterSlot.Hands, 51179, gems, enchants, yellowGem, blueGem);      // Sanctified Crimson Acolyte Gloves
        RE(c, CharacterSlot.Waist, 50063, gems, enchants, yellowGem, blueGem);      // Lingering Illness
        RE(c, CharacterSlot.Legs, 51177, gems, enchants, yellowGem, blueGem);       // Sanctified Crimson Acolyte Leggings
        RE(c, CharacterSlot.Feet, 49893, gems, enchants, yellowGem, blueGem);       // Sandals of Consecration
        RE(c, CharacterSlot.Finger1, 50174, gems, enchants, yellowGem, blueGem);    // Incarnadine Band of Mending
        RE(c, CharacterSlot.Finger2, 50424, gems, enchants, yellowGem, blueGem);    // Memory of Malygos
        RE(c, CharacterSlot.Trinket1, 50358, gems, enchants, yellowGem, blueGem);   // Purified Lunar Dust (SpellCast mana proc)
        RE(c, CharacterSlot.Trinket2, 50346, gems, enchants, yellowGem, blueGem);   // Sliver of Pure Ice (Use mana)
        RE(c, CharacterSlot.MainHand, 49968, gems, enchants, yellowGem, blueGem);   // Frozen Bonespike
        RE(c, CharacterSlot.OffHand, 50423, gems, enchants, yellowGem, blueGem);    // Sundial of Eternal Dusk
        RE(c, CharacterSlot.Ranged, 50472, gems, enchants, yellowGem, blueGem);     // Nightmare Ender (wand)
    }

    // 277 heroic priest healer: 5pc Sanctified Crimson Acolyte's Raiment (T10 heroic) + heroic ICC
    // offpieces, proc/Use trinkets, dagger + held-in-offhand, wand.
    static void GearHealpriest277(Character c, bool gems, bool enchants, int yellowGem, int blueGem)
    {
        RE(c, CharacterSlot.Head, 51261, gems, enchants, yellowGem, blueGem);       // Sanctified Crimson Acolyte Hood
        RE(c, CharacterSlot.Neck, 50609, gems, enchants, yellowGem, blueGem);       // Bone Sentinel's Amulet
        RE(c, CharacterSlot.Shoulders, 51264, gems, enchants, yellowGem, blueGem);  // Sanctified Crimson Acolyte Shoulderpads
        RE(c, CharacterSlot.Back, 50668, gems, enchants, yellowGem, blueGem);       // Greatcloak of the Turned Champion
        RE(c, CharacterSlot.Chest, 51263, gems, enchants, yellowGem, blueGem);      // Sanctified Crimson Acolyte Robe
        RE(c, CharacterSlot.Wrist, 50686, gems, enchants, yellowGem, blueGem);      // Death Surgeon's Sleeves
        RE(c, CharacterSlot.Hands, 51260, gems, enchants, yellowGem, blueGem);      // Sanctified Crimson Acolyte Gloves
        RE(c, CharacterSlot.Waist, 50702, gems, enchants, yellowGem, blueGem);      // Lingering Illness
        RE(c, CharacterSlot.Legs, 51262, gems, enchants, yellowGem, blueGem);       // Sanctified Crimson Acolyte Leggings
        RE(c, CharacterSlot.Feet, 50699, gems, enchants, yellowGem, blueGem);       // Plague Scientist's Boots
        RE(c, CharacterSlot.Finger1, 50720, gems, enchants, yellowGem, blueGem);    // Incarnadine Band of Mending
        RE(c, CharacterSlot.Finger2, 50636, gems, enchants, yellowGem, blueGem);    // Memory of Malygos
        RE(c, CharacterSlot.Trinket1, 50366, gems, enchants, yellowGem, blueGem);   // Althor's Abacus (HealingSpellHit proc)
        RE(c, CharacterSlot.Trinket2, 54589, gems, enchants, yellowGem, blueGem);   // Glowing Twilight Scale (Use, recursive)
        RE(c, CharacterSlot.MainHand, 50608, gems, enchants, yellowGem, blueGem);   // Frozen Bonespike
        RE(c, CharacterSlot.OffHand, 50635, gems, enchants, yellowGem, blueGem);    // Sundial of Eternal Dusk
        RE(c, CharacterSlot.Ranged, 50631, gems, enchants, yellowGem, blueGem);     // Nightmare Ender (wand)
    }

    // ==================================================================
    // ShadowPriest fixture generation (Phase 6 oracle)
    // ==================================================================

    static void ShadowpriestMain(string outDir)
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Calculations.LoadModel(typeof(CalculationsShadowPriest));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        ShadowpriestGenerate(outDir);
    }

    static SavedTalentSpec ShadowpriestSpec(string name)
    {
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Priest))
            if (spec.Name == name) return spec;
        throw new InvalidOperationException("priest spec not found: " + name);
    }

    static Character NewShadowpriest(string name, PriestTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Priest;
        c.CurrentModel = "ShadowPriest";
        c.PriestTalents = talents;
        if (raidBuffs)
            ((CalculationsShadowPriest)Calculations.Instance).SetDefaults(c);
        return c;
    }

    static void FinishShadowpriest(Character c, CalculationOptionsShadowPriest options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    static List<string> ShadowDefaultPriority()
    {
        return new List<string>(new string[] {
            "Vampiric Touch", "Devouring Plague", "Mind Blast", "Shadow Word: Pain", "Mind Flay", "Shadow Word: Death" });
    }

    static PriestTalents ShadowTalents()
    {
        TalentsBase t = ShadowpriestSpec("Shadow").TalentSpec();
        if (t == null) throw new InvalidOperationException("Shadow spec failed to parse");
        PriestTalents shadow = (PriestTalents)t;
        shadow.GlyphofMindFlay = true;
        shadow.GlyphofShadowWordPain = true;
        shadow.GlyphofShadow = true;
        return shadow;
    }

    static void ShadowpriestGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Shadow spec + DPS glyphs, full raid buffs, 264 T10 Regalia 5pc (2pc/4pc DPS bonuses),
        //    sp/hit gemming, proc + Use trinkets, default options (6 min).
        {
            PriestTalents shadow = ShadowTalents();
            Character c = NewShadowpriest("ShadowPriestRaid264", shadow, CharacterRace.Human, true);
            GearShadowpriest264(c, true, true, GEM_YELLOW, GEM_BLUE);
            CalculationOptionsShadowPriest o = new CalculationOptionsShadowPriest();
            o.SpellPriority = ShadowDefaultPriority();
            FinishShadowpriest(c, o, Path.Combine(outDir, "shadowpriest_264_raid.xml"));
        }

        // 2. 277 heroic T10 Regalia 5pc, Draenei, haste Use trinket + haste proc trinket, short 3 min
        //    fight, target 83, low latency, dispersion glyph.
        {
            PriestTalents shadow = ShadowTalents();
            shadow.GlyphofDispersion = true;
            shadow.GlyphofInnerFire = true;
            Character c = NewShadowpriest("ShadowPriestProcs277", shadow, CharacterRace.Draenei, true);
            GearShadowpriest277(c, true, true, GEM_YELLOW, GEM_BLUE);
            CalculationOptionsShadowPriest o = new CalculationOptionsShadowPriest();
            o.FightLength = 3f;
            o.Delay = 200f;
            o.Replenishment = 100f;
            o.SpellPriority = ShadowDefaultPriority();
            FinishShadowpriest(c, o, Path.Combine(outDir, "shadowpriest_277_procs.xml"));
        }

        // 3. No raid buffs, 264 gear, hit-capped gemming, 60s burst fight, high latency, no potion.
        {
            PriestTalents shadow = ShadowTalents();
            shadow.GlyphofShadowWordPain = false;
            Character c = NewShadowpriest("ShadowPriestNoBuffs60", shadow, CharacterRace.Gnome, false);
            GearShadowpriest264(c, true, true, GEM_HIT_YELLOW, GEM_BLUE);
            CalculationOptionsShadowPriest o = new CalculationOptionsShadowPriest();
            o.FightLength = 1f;
            o.Delay = 500f;
            o.FSRRatio = 100f;
            o.ManaPot = 0;
            o.SpellPriority = ShadowDefaultPriority();
            FinishShadowpriest(c, o, Path.Combine(outDir, "shadowpriest_nobuffs_short.xml"));
        }

        // 4. Blood Elf 20 min mana-starved fight, 277 gear, no replenishment/JotW/potion, reduced
        //    shadowfiend, movement, always in FSR.
        {
            PriestTalents shadow = ShadowTalents();
            shadow.GlyphofDispersion = true;
            Character c = NewShadowpriest("ShadowPriestManaStarved", shadow, CharacterRace.BloodElf, true);
            GearShadowpriest277(c, true, true, GEM_YELLOW, GEM_BLUE);
            CalculationOptionsShadowPriest o = new CalculationOptionsShadowPriest();
            o.FightLength = 20f;
            o.FSRRatio = 100f;
            o.Replenishment = 0f;
            o.JoW = 0f;
            o.Shadowfiend = 50f;
            o.ManaPot = 0;
            o.Survivability = 1f;
            o.MoveFrequency = 30f;
            o.MoveDuration = 4f;
            o.SpellPriority = ShadowDefaultPriority();
            FinishShadowpriest(c, o, Path.Combine(outDir, "shadowpriest_manastarved.xml"));
        }

        // 5. Undead, no Pain and Suffering (SWP reapplied manually), no spirit tap, Vampiric Embrace
        //    in the priority, non-default order, 8 min fight, 277 gear.
        {
            PriestTalents shadow = ShadowTalents();
            shadow.PainAndSuffering = 0;
            shadow.SpiritTap = 0;
            shadow.ImprovedSpiritTap = 0;
            shadow.GlyphofShadowWordPain = false;
            Character c = NewShadowpriest("ShadowPriestNoPnS480", shadow, CharacterRace.Undead, true);
            GearShadowpriest277(c, true, true, GEM_HIT_YELLOW, GEM_BLUE);
            CalculationOptionsShadowPriest o = new CalculationOptionsShadowPriest();
            o.FightLength = 8f;
            o.Delay = 250f;
            o.FSRRatio = 95f;
            o.SpellPriority = new List<string>(new string[] {
                "Vampiric Touch", "Vampiric Embrace", "Devouring Plague", "Mind Blast",
                "Shadow Word: Pain", "Mind Flay", "Shadow Word: Death" });
            FinishShadowpriest(c, o, Path.Combine(outDir, "shadowpriest_nopns.xml"));
        }

        // 6. Holy Smite DPS build (no Mind Flay talent, so the model takes SolverHoly): the shipped
        //    "Holy" spec, 264 gear, default options. Exercises the holy display path and SolverHoly.
        {
            TalentsBase t = ShadowpriestSpec("Holy").TalentSpec();
            if (t == null) throw new InvalidOperationException("Holy spec failed to parse");
            Character c = NewShadowpriest("ShadowPriestHolySmite", (PriestTalents)t, CharacterRace.Human, true);
            GearShadowpriest264(c, true, true, GEM_YELLOW, GEM_BLUE);
            CalculationOptionsShadowPriest o = new CalculationOptionsShadowPriest();
            o.SpellPriority = ShadowDefaultPriority();
            FinishShadowpriest(c, o, Path.Combine(outDir, "shadowpriest_holy_smite.xml"));
        }
    }

    // 264 shadow DPS: 5pc Crimson Acolyte Regalia (T10 normal) + ICC 25 offpieces, dagger + held
    // offhand, wand, sp/hit trinkets.
    static int GemForShadow(ItemSlot color, int yellowGem, int blueGem)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_RED;
            case ItemSlot.Yellow: return yellowGem;
            case ItemSlot.Blue: return blueGem;
            case ItemSlot.Meta: return GEM_META;
            case ItemSlot.Prismatic: return GEM_PRISMATIC;
            default: return 0;
        }
    }

    static void RES(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants, int yellowGem, int blueGem)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = RequireItem(itemId);
            if (gems)
            {
                g1 = GemForShadow(item.SocketColor1, yellowGem, blueGem);
                g2 = GemForShadow(item.SocketColor2, yellowGem, blueGem);
                g3 = GemForShadow(item.SocketColor3, yellowGem, blueGem);
            }
            if (enchants) ench = EnchForSlot(slot);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    static void GearShadowpriest264(Character c, bool gems, bool enchants, int yellowGem, int blueGem)
    {
        RES(c, CharacterSlot.Head, 51184, gems, enchants, yellowGem, blueGem);       // Sanctified Crimson Acolyte Cowl
        RES(c, CharacterSlot.Neck, 50182, gems, enchants, yellowGem, blueGem);       // Blood Queen's Crimson Choker
        RES(c, CharacterSlot.Shoulders, 51182, gems, enchants, yellowGem, blueGem);  // Sanctified Crimson Acolyte Mantle
        RES(c, CharacterSlot.Back, 50205, gems, enchants, yellowGem, blueGem);       // Frostbinder's Shredded Cape
        RES(c, CharacterSlot.Chest, 51180, gems, enchants, yellowGem, blueGem);      // Sanctified Crimson Acolyte Raiments
        RES(c, CharacterSlot.Wrist, 49994, gems, enchants, yellowGem, blueGem);      // The Lady's Brittle Bracers
        RES(c, CharacterSlot.Hands, 51183, gems, enchants, yellowGem, blueGem);      // Sanctified Crimson Acolyte Handwraps
        RES(c, CharacterSlot.Waist, 50996, gems, enchants, yellowGem, blueGem);      // Belt of Omission
        RES(c, CharacterSlot.Legs, 51181, gems, enchants, yellowGem, blueGem);       // Sanctified Crimson Acolyte Pants
        RES(c, CharacterSlot.Feet, 50062, gems, enchants, yellowGem, blueGem);       // Plague Scientist's Boots
        RES(c, CharacterSlot.Finger1, 50397, gems, enchants, yellowGem, blueGem);    // Ashen Band of Unmatched Destruction
        RES(c, CharacterSlot.Finger2, 50399, gems, enchants, yellowGem, blueGem);    // Ashen Band of Unmatched Wisdom
        RES(c, CharacterSlot.Trinket1, 50353, gems, enchants, yellowGem, blueGem);   // Dislodged Foreign Object (haste proc)
        RES(c, CharacterSlot.Trinket2, 50346, gems, enchants, yellowGem, blueGem);   // Sliver of Pure Ice (Use spell power)
        RES(c, CharacterSlot.MainHand, 51815, gems, enchants, yellowGem, blueGem);   // Bleak Coldarra Carver (hit)
        RES(c, CharacterSlot.OffHand, 50423, gems, enchants, yellowGem, blueGem);    // Sundial of Eternal Dusk
        RES(c, CharacterSlot.Ranged, 50033, gems, enchants, yellowGem, blueGem);     // Corpse-Impaling Spike (wand)
    }

    // 277 shadow DPS: 5pc Sanctified Crimson Acolyte Regalia (T10 heroic) + heroic ICC offpieces,
    // haste Use/proc trinkets, dagger + held offhand, wand.
    static void GearShadowpriest277(Character c, bool gems, bool enchants, int yellowGem, int blueGem)
    {
        RES(c, CharacterSlot.Head, 51255, gems, enchants, yellowGem, blueGem);       // Sanctified Crimson Acolyte Cowl
        RES(c, CharacterSlot.Neck, 50658, gems, enchants, yellowGem, blueGem);       // Amulet of the Silent Eulogy
        RES(c, CharacterSlot.Shoulders, 51257, gems, enchants, yellowGem, blueGem);  // Sanctified Crimson Acolyte Mantle
        RES(c, CharacterSlot.Back, 50628, gems, enchants, yellowGem, blueGem);       // Frostbinder's Shredded Cape
        RES(c, CharacterSlot.Chest, 51259, gems, enchants, yellowGem, blueGem);      // Sanctified Crimson Acolyte Raiments
        RES(c, CharacterSlot.Wrist, 50651, gems, enchants, yellowGem, blueGem);      // The Lady's Brittle Bracers
        RES(c, CharacterSlot.Hands, 51256, gems, enchants, yellowGem, blueGem);      // Sanctified Crimson Acolyte Handwraps
        RES(c, CharacterSlot.Waist, 50613, gems, enchants, yellowGem, blueGem);      // Crushing Coldwraith Belt
        RES(c, CharacterSlot.Legs, 51258, gems, enchants, yellowGem, blueGem);       // Sanctified Crimson Acolyte Pants
        RES(c, CharacterSlot.Feet, 50699, gems, enchants, yellowGem, blueGem);       // Plague Scientist's Boots
        RES(c, CharacterSlot.Finger1, 50398, gems, enchants, yellowGem, blueGem);    // Ashen Band of Endless Destruction
        RES(c, CharacterSlot.Finger2, 50614, gems, enchants, yellowGem, blueGem);    // Loop of the Endless Labyrinth
        RES(c, CharacterSlot.Trinket1, 50348, gems, enchants, yellowGem, blueGem);   // Dislodged Foreign Object (haste proc)
        RES(c, CharacterSlot.Trinket2, 54588, gems, enchants, yellowGem, blueGem);   // Charred Twilight Scale (Use haste)
        RES(c, CharacterSlot.MainHand, 50734, gems, enchants, yellowGem, blueGem);   // Royal Scepter of Terenas II
        RES(c, CharacterSlot.OffHand, 50719, gems, enchants, yellowGem, blueGem);    // Shadow Silk Spindle
        RES(c, CharacterSlot.Ranged, 50684, gems, enchants, yellowGem, blueGem);     // Corpse-Impaling Spike (wand)
    }

    static void ListShadowpriest()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== PRIEST TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Priest))
        {
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));
            TalentsBase t = spec.TalentSpec();
            if (t != null) PrintTalents(spec.Name, t);
        }

        Console.WriteLine();
        Console.WriteLine("== SHADOW GEM SPOT CHECK ==");
        int[] gids = new int[] { 40113, 40153, 40125, 40155, 40133, 41285, 41401, 49110 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLine(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== CRIMSON ACOLYTE SET PIECES ==");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.SetName != null && (i.SetName.Contains("Crimson Acolyte's Regalia") || i.SetName.Contains("Crimson Acolyte's Raiment")))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Id))
            Console.WriteLine(ItemLine(item));

        Console.WriteLine();
        Console.WriteLine("== CLOTH CASTER DPS ITEMS (epic, ilvl>=264, sp + crit/haste/hit, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264 && i.Type == ItemType.Cloth
                    && i.Stats.SpellPower > 0 && (i.Stats.CritRating > 0 || i.Stats.HasteRating > 0 || i.Stats.HitRating > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(14))
                Console.WriteLine(ItemLine(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                    && i.Stats.SpellPower > 0 && (i.Stats.CritRating > 0 || i.Stats.HasteRating > 0 || i.Stats.HitRating > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(14))
                Console.WriteLine(ItemLine(item));
        }
        Console.WriteLine("-- Trinket --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Trinket && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && (i.Stats.SpellPower > 0 || i.Stats.HasteRating > 0 || i.Stats.CritRating > 0 || i.Stats.HitRating > 0 || i.Stats.SpellPower > 0))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(40))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- OffHand --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.OffHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && i.Stats.SpellPower > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- MainHand (one-hand caster) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.MainHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && (i.Type == ItemType.OneHandMace || i.Type == ItemType.Dagger)
                && i.Stats.SpellPower > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- Ranged (Wands) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Quality == ItemQuality.Epic && i.Type == ItemType.Wand && i.ItemLevel >= 264)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine(item));
    }

    // ==================================================================
    // Hunter fixture generation (Phase 6 oracle)
    // ==================================================================

    // hunter gems: agi, agi/hit, agi/stam, meta, prismatic
    const int GEM_HUNT_RED = 40112;      // Delicate Cardinal Ruby +20 Agi
    const int GEM_HUNT_YELLOW_HIT = 40148; // Glinting Ametrine +10 Agi +10 Hit
    const int GEM_HUNT_YELLOW_CRIT = 40147; // Deadly Ametrine +10 Agi +10 Crit
    const int GEM_HUNT_BLUE = 40130;     // Shifting Dreadstone +10 Agi +15 Sta
    const int GEM_HUNT_META = 41398;     // Relentless Earthsiege Diamond
    const int GEM_HUNT_PRISMATIC = 49110; // Nightmare Tear

    static void HunterMain(string outDir)
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Calculations.LoadModel(typeof(CalculationsHunter));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        HunterGenerate(outDir);
    }

    static SavedTalentSpec HunterSpec(string name)
    {
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Hunter))
            if (spec.Name == name) return spec;
        throw new InvalidOperationException("hunter spec not found: " + name);
    }

    static void ListHunter()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== HUNTER TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Hunter))
        {
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));
            TalentsBase t = spec.TalentSpec();
            if (t != null) PrintTalents(spec.Name, t);
        }

        Console.WriteLine();
        Console.WriteLine("== HUNTER GEM SPOT CHECK ==");
        int[] gids = new int[] { 40112, 40148, 40147, 40130, 41398, 49110, 40155, 40113, 40133 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLine(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== RANGED SLOT ENCHANTS ==");
        foreach (Enchant e in Enchant.AllEnchants.Where(x => x.Slot == ItemSlot.Ranged || x.Slot == ItemSlot.Projectile).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | slot={2} | {3}", e.Id, e.Name, e.Slot, e.Stats == null ? "" : StatLine(e.Stats)));

        Console.WriteLine();
        Console.WriteLine("== MAIL AGI ITEMS (epic, ilvl>=264, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264 && i.Type == ItemType.Mail
                    && i.Stats.Agility > 0)
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
                Console.WriteLine(ItemLine(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger, ItemSlot.Trinket, ItemSlot.Projectile })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                    && (i.Stats.Agility > 0 || i.Stats.ArmorPenetrationRating > 0 || i.Stats.AttackPower > 0 || i.Stats.RangedAttackPower > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
                Console.WriteLine(ItemLine(item));
        }
        Console.WriteLine("-- Ranged (epic ranged weapons) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && (i.Type == ItemType.Bow || i.Type == ItemType.Gun || i.Type == ItemType.Crossbow))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- MainHand/TwoHand (agi stat weapons) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => (i.Slot == ItemSlot.TwoHand || i.Slot == ItemSlot.MainHand || i.Slot == ItemSlot.OneHand)
                && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264 && i.Stats.Agility > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine(item));
    }

    static int GemForHunter(ItemSlot color, int yellowGem, int blueGem)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_HUNT_RED;
            case ItemSlot.Yellow: return yellowGem;
            case ItemSlot.Blue: return blueGem;
            case ItemSlot.Meta: return GEM_HUNT_META;
            case ItemSlot.Prismatic: return GEM_HUNT_PRISMATIC;
            default: return 0;
        }
    }

    static void HE(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants, int yellowGem, int blueGem)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = RequireItem(itemId);
            if (gems)
            {
                g1 = GemForHunter(item.SocketColor1, yellowGem, blueGem);
                g2 = GemForHunter(item.SocketColor2, yellowGem, blueGem);
                g3 = GemForHunter(item.SocketColor3, yellowGem, blueGem);
            }
            if (enchants) ench = EnchForSlotHunter(slot);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    static int EnchForSlotHunter(CharacterSlot slot)
    {
        switch (slot)
        {
            case CharacterSlot.Head: return ENCH_CAT_HEAD;
            case CharacterSlot.Shoulders: return ENCH_CAT_SHOULDER;
            case CharacterSlot.Back: return ENCH_CAT_BACK;
            case CharacterSlot.Chest: return ENCH_CAT_CHEST;
            case CharacterSlot.Wrist: return ENCH_CAT_WRIST;
            case CharacterSlot.Hands: return ENCH_CAT_HANDS;
            case CharacterSlot.Legs: return ENCH_CAT_LEGS;
            case CharacterSlot.Feet: return ENCH_CAT_FEET;
            case CharacterSlot.MainHand: return ENCH_CAT_WEAPON;
            case CharacterSlot.Ranged: return 3843; // Diamond-cut Refractor Scope
            default: return 0;
        }
    }

    // 264 mail hunter DPS: 5pc Ahn'Kahar Blood Hunter's Battlegear (T10 normal) + ICC offpieces.
    static void GearHunter264(Character c, bool gems, bool enchants, int yellowGem, int blueGem, int rangedId)
    {
        HE(c, CharacterSlot.Head, 51153, gems, enchants, yellowGem, blueGem);       // Sanctified Ahn'Kahar Blood Hunter's Headpiece
        HE(c, CharacterSlot.Neck, 51822, gems, enchants, yellowGem, blueGem);       // Rimetooth Pendant
        HE(c, CharacterSlot.Shoulders, 51151, gems, enchants, yellowGem, blueGem);  // Sanctified Ahn'Kahar Blood Hunter's Spaulders
        HE(c, CharacterSlot.Back, 49998, gems, enchants, yellowGem, blueGem);       // Shadowvault Slayer's Cloak
        HE(c, CharacterSlot.Chest, 51150, gems, enchants, yellowGem, blueGem);      // Sanctified Ahn'Kahar Blood Hunter's Tunic
        HE(c, CharacterSlot.Wrist, 50000, gems, enchants, yellowGem, blueGem);      // Scourge Hunter's Vambraces
        HE(c, CharacterSlot.Hands, 51154, gems, enchants, yellowGem, blueGem);      // Sanctified Ahn'Kahar Blood Hunter's Handguards
        HE(c, CharacterSlot.Waist, 50993, gems, enchants, yellowGem, blueGem);      // Band of the Night Raven
        HE(c, CharacterSlot.Legs, 51152, gems, enchants, yellowGem, blueGem);       // Sanctified Ahn'Kahar Blood Hunter's Legguards
        HE(c, CharacterSlot.Feet, 50071, gems, enchants, yellowGem, blueGem);       // Treads of the Wasteland
        HE(c, CharacterSlot.Finger1, 50402, gems, enchants, yellowGem, blueGem);    // Ashen Band of Endless Vengeance
        HE(c, CharacterSlot.Finger2, 50414, gems, enchants, yellowGem, blueGem);    // Might of Blight
        HE(c, CharacterSlot.Trinket1, 50343, gems, enchants, yellowGem, blueGem);   // Whispering Fanged Skull (proc)
        HE(c, CharacterSlot.Trinket2, 47734, gems, enchants, yellowGem, blueGem);   // Mark of Supremacy (Use AP)
        HE(c, CharacterSlot.MainHand, 50178, gems, enchants, yellowGem, blueGem);   // Bloodfall (polearm)
        HE(c, CharacterSlot.Ranged, rangedId, gems, enchants, yellowGem, blueGem);
    }

    // 277 heroic mail hunter DPS: 5pc Sanctified Ahn'Kahar Blood Hunter's Battlegear + heroic offpieces.
    static void GearHunter277(Character c, bool gems, bool enchants, int yellowGem, int blueGem, int rangedId)
    {
        HE(c, CharacterSlot.Head, 51286, gems, enchants, yellowGem, blueGem);       // Sanctified Ahn'Kahar Blood Hunter's Headpiece
        HE(c, CharacterSlot.Neck, 50633, gems, enchants, yellowGem, blueGem);       // Sindragosa's Cruel Claw
        HE(c, CharacterSlot.Shoulders, 51288, gems, enchants, yellowGem, blueGem);  // Sanctified Ahn'Kahar Blood Hunter's Spaulders
        HE(c, CharacterSlot.Back, 50653, gems, enchants, yellowGem, blueGem);       // Shadowvault Slayer's Cloak
        HE(c, CharacterSlot.Chest, 51289, gems, enchants, yellowGem, blueGem);      // Sanctified Ahn'Kahar Blood Hunter's Tunic
        HE(c, CharacterSlot.Wrist, 50655, gems, enchants, yellowGem, blueGem);      // Scourge Hunter's Vambraces
        HE(c, CharacterSlot.Hands, 51285, gems, enchants, yellowGem, blueGem);      // Sanctified Ahn'Kahar Blood Hunter's Handguards
        HE(c, CharacterSlot.Waist, 50688, gems, enchants, yellowGem, blueGem);      // Nerub'ar Stalker's Cord
        HE(c, CharacterSlot.Legs, 51287, gems, enchants, yellowGem, blueGem);       // Sanctified Ahn'Kahar Blood Hunter's Legguards
        HE(c, CharacterSlot.Feet, 50711, gems, enchants, yellowGem, blueGem);       // Treads of the Wasteland
        HE(c, CharacterSlot.Finger1, 50402, gems, enchants, yellowGem, blueGem);    // Ashen Band of Endless Vengeance
        HE(c, CharacterSlot.Finger2, 50604, gems, enchants, yellowGem, blueGem);    // Band of the Bone Colossus
        HE(c, CharacterSlot.Trinket1, 50363, gems, enchants, yellowGem, blueGem);   // Deathbringer's Will (proc)
        HE(c, CharacterSlot.Trinket2, 54590, gems, enchants, yellowGem, blueGem);   // Sharpened Twilight Scale (proc ArP)
        HE(c, CharacterSlot.MainHand, 50735, gems, enchants, yellowGem, blueGem);   // Oathbinder, Charge of the Ranger-General
        HE(c, CharacterSlot.Ranged, rangedId, gems, enchants, yellowGem, blueGem);
    }

    static HunterTalents HunterDefaultSpec(string name)
    {
        TalentsBase t = HunterSpec(name).TalentSpec();
        if (t == null) throw new InvalidOperationException(name + " spec failed to parse");
        return (HunterTalents)t;
    }

    static Character NewHunter(string name, HunterTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Hunter;
        c.CurrentModel = "Hunter";
        c.HunterTalents = talents;
        if (raidBuffs)
        {
            string[] buffs = new string[] {
                "Arcane Intellect", "Mark of the Wild", "Improved Mark of the Wild", "Blessing of Kings",
                "Blessing of Might", "Battle Shout", "Strength of Earth Totem", "Grace of Air Totem",
                "Leader of the Pack", "Unleashed Rage", "Horn of Winter", "Judgement of Wisdom",
                "Heroism/Bloodlust", "Flask of Endless Rage", "Fish Feast",
            };
            foreach (string b in buffs) { c.ActiveBuffsAdd(b); }
        }
        return c;
    }

    // Ferocity pet talent build used by the Cat/Wolf pets (values are set directly; the model does
    // not validate the tree prerequisites).
    static void PetTalentsCat(CalculationOptionsHunter o)
    {
        o.PetTalents.CobraReflexes.Value = 2;
        o.PetTalents.DiveDash.Value = 1;
        o.PetTalents.GreatStamina.Value = 3;
        o.PetTalents.NaturalArmor.Value = 2;
        o.PetTalents.SpidersBite.Value = 3;
        o.PetTalents.SpikedCollar.Value = 3;
        o.PetTalents.CullingTheHerd.Value = 3;
        o.PetTalents.Rabid.Value = 1;
        o.PetTalents.CallOfTheWild.Value = 1;
        o.PetTalents.WildHunt.Value = 2;
        o.PetTalents.SharkAttack.Value = 2;
        o.PetTalents.GreatResistance.Value = 3;
        o.PetTalents.GraceOfTheMantis.Value = 2;
        o.PetTalents.FeedingFrenzy.Value = 2;
    }

    // Touch the lazy list so GetXml has something to serialize (the app always initializes it via
    // GetPetBuffsStats before saving; fixtures without pet buffs must do the same).
    static void PetBuffsNone(CalculationOptionsHunter o)
    {
        if (o.petActiveBuffs == null) throw new InvalidOperationException();
    }

    static void PetBuffsRaid(CalculationOptionsHunter o)
    {
        string[] names = new string[] {
            "Blessing of Kings", "Blessing of Might", "Battle Shout", "Strength of Earth Totem",
            "Leader of the Pack", "Horn of Winter",
        };
        o.petActiveBuffs = new List<Buff>();
        foreach (string n in names)
        {
            Buff b = Buff.GetBuffByName(n);
            if (b == null) throw new InvalidOperationException("pet buff not found: " + n);
            o.petActiveBuffs.Add(b);
        }
    }

    static void FinishHunter(Character c, CalculationOptionsHunter options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    static void HunterGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Marksmanship 7/57/7, raid buffs, 264 T10 5pc, Zod's Repeating Longbow (proc), rotation test on.
        {
            Character c = NewHunter("HunterRaid264", HunterDefaultSpec("Marksmanship"), CharacterRace.NightElf, true);
            GearHunter264(c, true, true, GEM_HUNT_YELLOW_HIT, GEM_HUNT_BLUE, 50034); // Zod's Repeating Longbow
            CalculationOptionsHunter o = new CalculationOptionsHunter();
            o.PriorityIndexes = new int[] { 18, 19, 4, 17, 9, 1, 7, 8, 0, 0 };
            o.PetFamily = PetFamily.Cat;
            o.PetPriority1 = PetAttacks.Growl;
            o.PetPriority2 = PetAttacks.Claw;
            o.PetPriority3 = PetAttacks.None;
            PetTalentsCat(o);
            PetBuffsRaid(o);
            FinishHunter(c, o, Path.Combine(outDir, "hunter_264_raid.xml"));
        }

        // 2. Marksmanship 277 heroic 5pc, Draenei, Deathbringer's Will + Sharpened Twilight Scale,
        //    short 180s fight, target 83, proc trinkets and Zod's proc.
        {
            Character c = NewHunter("HunterProcs277", HunterDefaultSpec("Marksmanship"), CharacterRace.Draenei, true);
            GearHunter277(c, true, true, GEM_HUNT_YELLOW_CRIT, GEM_HUNT_BLUE, 50638); // Zod's Repeating Longbow (277)
            CalculationOptionsHunter o = new CalculationOptionsHunter();
            o.PriorityIndexes = new int[] { 18, 19, 4, 17, 9, 1, 7, 8, 0, 0 };
            o.Duration = 180;
            o.Lag = 50;
            o.PetFamily = PetFamily.Cat;
            o.PetPriority1 = PetAttacks.Growl;
            o.PetPriority2 = PetAttacks.Claw;
            PetTalentsCat(o);
            PetBuffsRaid(o);
            FinishHunter(c, o, Path.Combine(outDir, "hunter_277_procs.xml"));
        }

        // 3. Survival 0/15/56, raid buffs, 264 gear, Black Arrow + Explosive Shot (Lock and Load),
        //    Njorndar Bone Bow, 300s, T10 2pc/4pc set bonuses.
        {
            Character c = NewHunter("HunterSurvival264", HunterDefaultSpec("Survival"), CharacterRace.Orc, true);
            GearHunter264(c, true, true, GEM_HUNT_YELLOW_HIT, GEM_HUNT_BLUE, 51927); // Njorndar Bone Bow
            CalculationOptionsHunter o = new CalculationOptionsHunter();
            o.PriorityIndexes = new int[] { 18, 9, 10, 4, 11, 1, 8, 0, 0, 0 };
            o.Duration = 300;
            o.PetFamily = PetFamily.Cat;
            o.PetPriority1 = PetAttacks.Growl;
            o.PetPriority2 = PetAttacks.Claw;
            PetTalentsCat(o);
            PetBuffsRaid(o);
            FinishHunter(c, o, Path.Combine(outDir, "hunter_survival_264.xml"));
        }

        // 4. Beast Mastery 54/12/5, no raid buffs, 264 gear, Wolf pet (Furious Howl), 60s fight,
        //    target level 80, high latency, Bestial Wrath in the priority.
        {
            Character c = NewHunter("HunterBMNoBuffs60", HunterDefaultSpec("Beast Mastery"), CharacterRace.Tauren, false);
            GearHunter264(c, true, true, GEM_HUNT_YELLOW_CRIT, GEM_HUNT_BLUE, 50444); // Rowan's Rifle of Silver Bullets
            CalculationOptionsHunter o = new CalculationOptionsHunter();
            o.PriorityIndexes = new int[] { 18, 20, 9, 1, 2, 4, 8, 0, 0, 0 };
            o.Duration = 60;
            o.TargetLevel = 80;
            o.Lag = 300;
            o.React = 400;
            o.PetFamily = PetFamily.Wolf;
            o.PetPriority1 = PetAttacks.Growl;
            o.PetPriority2 = PetAttacks.Bite;
            o.PetPriority3 = PetAttacks.FuriousHowl;
            PetTalentsCat(o);
            PetBuffsNone(o);
            FinishHunter(c, o, Path.Combine(outDir, "hunter_bm_nobuffs_short.xml"));
        }

        // 5. Marksmanship 264, rotation test OFF (the frequency/exponential path), fixed 600s fight,
        //    Volley/multi-target enabled at 25%, Use trinket, Cat pet.
        {
            Character c = NewHunter("HunterFreq264", HunterDefaultSpec("Marksmanship"), CharacterRace.Human, true);
            GearHunter264(c, true, true, GEM_HUNT_YELLOW_CRIT, GEM_HUNT_BLUE, 50034);
            CalculationOptionsHunter o = new CalculationOptionsHunter();
            o.PriorityIndexes = new int[] { 18, 19, 4, 17, 9, 16, 1, 7, 8, 0 };
            o.UseRotationTest = false;
            o.Duration = 600;
            o.MultipleTargets = true;
            o.MultipleTargetsPerc = 25f;
            o.TimeSpentSub20 = 90;
            o.TimeSpent35To20 = 60;
            o.PetFamily = PetFamily.Cat;
            o.PetPriority1 = PetAttacks.Growl;
            o.PetPriority2 = PetAttacks.Claw;
            PetTalentsCat(o);
            PetBuffsRaid(o);
            FinishHunter(c, o, Path.Combine(outDir, "hunter_freq_264.xml"));
        }

        // 6. Survival 277, mana-starved 20 min fight: no Judgement of Wisdom/Replenishment buffs,
        //    constant Aspect of the Viper, boss below 20% (Kill Shot), Use trinket, no potion.
        {
            Character c = NewHunter("HunterStarved", HunterDefaultSpec("Survival"), CharacterRace.Undead, false);
            GearHunter277(c, true, true, GEM_HUNT_YELLOW_CRIT, GEM_HUNT_BLUE, 50638);
            CalculationOptionsHunter o = new CalculationOptionsHunter();
            o.PriorityIndexes = new int[] { 18, 9, 10, 4, 11, 1, 8, 0, 0, 0 };
            o.Duration = 1200;
            o.SelectedAspect = Aspect.Viper;
            o.AspectUsage = AspectUsage.ViperRegen;
            o.BossHPPerc = 0.15f;
            o.PetFamily = PetFamily.Cat;
            o.PetPriority1 = PetAttacks.Growl;
            o.PetPriority2 = PetAttacks.Claw;
            PetTalentsCat(o);
            PetBuffsNone(o);
            FinishHunter(c, o, Path.Combine(outDir, "hunter_starved_1200.xml"));
        }
    }

    // ==================================================================
    // Rogue fixture generation (Phase 6 oracle)
    // ==================================================================

    // rogue gems: agi, agi/hit, agi/crit, agi/stam, meta, prismatic
    const int GEM_ROGUE_RED = 40112;      // Delicate Cardinal Ruby +20 Agi
    const int GEM_ROGUE_YELLOW_HIT = 40148; // Glinting Ametrine +10 Agi +10 Hit
    const int GEM_ROGUE_YELLOW_CRIT = 40147; // Deadly Ametrine +10 Agi +10 Crit
    const int GEM_ROGUE_BLUE = 40130;     // Shifting Dreadstone +10 Agi +15 Sta
    const int GEM_ROGUE_META = 41398;     // Relentless Earthsiege Diamond
    const int GEM_ROGUE_PRISMATIC = 49110; // Nightmare Tear

    static void RogueMain(string outDir)
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Calculations.LoadModel(typeof(CalculationsRogue));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        RogueGenerate(outDir);
    }

    static SavedTalentSpec RogueSpec(string name)
    {
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Rogue))
            if (spec.Name == name) return spec;
        throw new InvalidOperationException("rogue spec not found: " + name);
    }

    // The shipped DefaultDataFiles/Talents.xml only has Untalented, Mutilate (51/18/2) and Combat
    // (20/51/0); the subtlety PvE build is constructed property by property (all talents the model
    // reads, plus the eviscerate/rupture/slice-and-dice glyphs).
    static RogueTalents RogueSubtletyTalents()
    {
        RogueTalents t = new RogueTalents();
        // Assassination (20)
        t.ImprovedEviscerate = 3; t.Malice = 5; t.Ruthlessness = 2; t.BloodSpatter = 2;
        t.PuncturingWounds = 3; t.Vigor = 1; t.Lethality = 4;
        // Subtlety (51)
        t.Opportunity = 2; t.SleightOfHand = 2; t.DirtyTricks = 2; t.Camouflage = 2;
        t.Elusiveness = 2; t.GhostlyStrike = 1; t.SerratedBlades = 3; t.Setup = 3;
        t.Initiative = 3; t.HeightenedSenses = 2; t.Preparation = 1; t.DirtyDeeds = 2;
        t.Hemorrhage = 1; t.MasterOfSubtlety = 3; t.Deadliness = 5; t.SinisterCalling = 5;
        t.HonorAmongThieves = 3; t.Shadowstep = 2; t.FilthyTricks = 2; t.Waylay = 2;
        t.SlaughterFromTheShadows = 3;
        // glyphs
        t.GlyphOfEviscerate = true; t.GlyphOfRupture = true; t.GlyphOfSliceandDice = true;
        t.GlyphOfBackstab = true;
        return t;
    }

    static void ListRogue()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== ROGUE TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Rogue))
        {
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));
            TalentsBase t = spec.TalentSpec();
            if (t != null) PrintTalents(spec.Name, t);
        }
        Console.WriteLine();
        PrintTalents("Subtlety (constructed)", RogueSubtletyTalents());

        Console.WriteLine();
        Console.WriteLine("== ROGUE GEM SPOT CHECK ==");
        int[] gids = new int[] { 40112, 40148, 40147, 40130, 41398, 49110, 40155, 40113, 40133 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLine(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== LEATHER AGI ITEMS (epic, ilvl>=264, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264 && i.Type == ItemType.Leather
                    && (i.Stats.Agility > 0 || i.Stats.ArmorPenetrationRating > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
                Console.WriteLine(ItemLine(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger, ItemSlot.Trinket })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                    && (i.Stats.Agility > 0 || i.Stats.ArmorPenetrationRating > 0 || i.Stats.AttackPower > 0
                        || i.Stats.MoteOfAnger > 0 || i.Stats.DeathbringerProc > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
                Console.WriteLine(ItemLine(item));
        }
        Console.WriteLine("-- MainHand/OffHand (agi daggers/swords/axes, ilvl>=264) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => (i.Slot == ItemSlot.MainHand || i.Slot == ItemSlot.OneHand)
                && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && (i.Type == ItemType.Dagger || i.Type == ItemType.OneHandSword || i.Type == ItemType.OneHandAxe)
                && (i.Stats.Agility > 0 || i.Stats.ArmorPenetrationRating > 0 || i.Name == "Heartpierce"))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(28))
            Console.WriteLine(ItemLine(item));

        Console.WriteLine();
        Console.WriteLine("== SHADOWBLADE'S BATTLEGEAR SET BONUSES ==");
        foreach (Buff b in Buff.AllBuffs.Where(x => x.SetName == "Shadowblade's Battlegear").OrderBy(x => x.Name))
            Console.WriteLine(b.Name + " | threshold=" + b.SetThreshold + " | " + StatLine(b.Stats));
    }

    static int GemForRogue(ItemSlot color, int yellowGem, int blueGem)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_ROGUE_RED;
            case ItemSlot.Yellow: return yellowGem;
            case ItemSlot.Blue: return blueGem;
            case ItemSlot.Meta: return GEM_ROGUE_META;
            case ItemSlot.Prismatic: return GEM_ROGUE_PRISMATIC;
            default: return 0;
        }
    }

    static void RG(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants, int yellowGem, int blueGem)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = RequireItem(itemId);
            if (gems)
            {
                g1 = GemForRogue(item.SocketColor1, yellowGem, blueGem);
                g2 = GemForRogue(item.SocketColor2, yellowGem, blueGem);
                g3 = GemForRogue(item.SocketColor3, yellowGem, blueGem);
            }
            if (enchants) ench = EnchForSlotRogue(slot);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    static int EnchForSlotRogue(CharacterSlot slot)
    {
        switch (slot)
        {
            case CharacterSlot.Head: return ENCH_CAT_HEAD;
            case CharacterSlot.Shoulders: return ENCH_CAT_SHOULDER;
            case CharacterSlot.Back: return ENCH_CAT_BACK;
            case CharacterSlot.Chest: return ENCH_CAT_CHEST;
            case CharacterSlot.Wrist: return ENCH_CAT_WRIST;
            case CharacterSlot.Hands: return ENCH_CAT_HANDS;
            case CharacterSlot.Legs: return ENCH_CAT_LEGS;
            case CharacterSlot.Feet: return ENCH_CAT_FEET;
            case CharacterSlot.MainHand: return ENCH_CAT_WEAPON;
            case CharacterSlot.OffHand: return ENCH_CAT_WEAPON;
            default: return 0;
        }
    }

    // 264 leather rogue DPS: 5pc Sanctified Shadowblade's Battlegear (T10 normal) + ICC offpieces.
    static void GearRogue264(Character c, bool gems, bool enchants, int yellowGem, int blueGem, int mhId, int ohId)
    {
        RG(c, CharacterSlot.Head, 51187, gems, enchants, yellowGem, blueGem);       // Sanctified Shadowblade Helmet
        RG(c, CharacterSlot.Neck, 51822, gems, enchants, yellowGem, blueGem);       // Rimetooth Pendant
        RG(c, CharacterSlot.Shoulders, 51185, gems, enchants, yellowGem, blueGem);  // Sanctified Shadowblade Pauldrons
        RG(c, CharacterSlot.Back, 49998, gems, enchants, yellowGem, blueGem);       // Shadowvault Slayer's Cloak
        RG(c, CharacterSlot.Chest, 51189, gems, enchants, yellowGem, blueGem);      // Sanctified Shadowblade Breastplate
        RG(c, CharacterSlot.Wrist, 50333, gems, enchants, yellowGem, blueGem);      // Toskk's Maximized Wristguards
        RG(c, CharacterSlot.Hands, 51188, gems, enchants, yellowGem, blueGem);      // Sanctified Shadowblade Gauntlets
        RG(c, CharacterSlot.Waist, 50067, gems, enchants, yellowGem, blueGem);      // Astrylian's Sutured Cinch
        RG(c, CharacterSlot.Legs, 51186, gems, enchants, yellowGem, blueGem);       // Sanctified Shadowblade Legplates
        RG(c, CharacterSlot.Feet, 49950, gems, enchants, yellowGem, blueGem);       // Frostbitten Fur Boots
        RG(c, CharacterSlot.Finger1, 50402, gems, enchants, yellowGem, blueGem);    // Ashen Band of Endless Vengeance
        RG(c, CharacterSlot.Finger2, 50186, gems, enchants, yellowGem, blueGem);    // Frostbrood Sapphire Ring
        RG(c, CharacterSlot.Trinket1, 50343, gems, enchants, yellowGem, blueGem);   // Whispering Fanged Skull (proc)
        RG(c, CharacterSlot.Trinket2, 47734, gems, enchants, yellowGem, blueGem);   // Mark of Supremacy (Use AP)
        RG(c, CharacterSlot.MainHand, mhId, gems, enchants, yellowGem, blueGem);
        RG(c, CharacterSlot.OffHand, ohId, gems, enchants, yellowGem, blueGem);
    }

    // 277 heroic leather rogue DPS: 5pc Sanctified Shadowblade's Battlegear + heroic offpieces.
    static void GearRogue277(Character c, bool gems, bool enchants, int yellowGem, int blueGem, int mhId, int ohId)
    {
        RG(c, CharacterSlot.Head, 51252, gems, enchants, yellowGem, blueGem);       // Sanctified Shadowblade Helmet
        RG(c, CharacterSlot.Neck, 50633, gems, enchants, yellowGem, blueGem);       // Sindragosa's Cruel Claw
        RG(c, CharacterSlot.Shoulders, 51254, gems, enchants, yellowGem, blueGem);  // Sanctified Shadowblade Pauldrons
        RG(c, CharacterSlot.Back, 50653, gems, enchants, yellowGem, blueGem);       // Shadowvault Slayer's Cloak
        RG(c, CharacterSlot.Chest, 51250, gems, enchants, yellowGem, blueGem);      // Sanctified Shadowblade Breastplate
        RG(c, CharacterSlot.Wrist, 50670, gems, enchants, yellowGem, blueGem);      // Toskk's Maximized Wristguards
        RG(c, CharacterSlot.Hands, 51251, gems, enchants, yellowGem, blueGem);      // Sanctified Shadowblade Gauntlets
        RG(c, CharacterSlot.Waist, 50707, gems, enchants, yellowGem, blueGem);      // Astrylian's Sutured Cinch
        RG(c, CharacterSlot.Legs, 51253, gems, enchants, yellowGem, blueGem);       // Sanctified Shadowblade Legplates
        RG(c, CharacterSlot.Feet, 50607, gems, enchants, yellowGem, blueGem);       // Frostbitten Fur Boots
        RG(c, CharacterSlot.Finger1, 50618, gems, enchants, yellowGem, blueGem);    // Frostbrood Sapphire Ring
        RG(c, CharacterSlot.Finger2, 50604, gems, enchants, yellowGem, blueGem);    // Band of the Bone Colossus
        RG(c, CharacterSlot.Trinket1, 50362, gems, enchants, yellowGem, blueGem);   // Deathbringer's Will (proc)
        RG(c, CharacterSlot.Trinket2, 50351, gems, enchants, yellowGem, blueGem);   // Tiny Abomination in a Jar (proc)
        RG(c, CharacterSlot.MainHand, mhId, gems, enchants, yellowGem, blueGem);
        RG(c, CharacterSlot.OffHand, ohId, gems, enchants, yellowGem, blueGem);
    }

    static RogueTalents RogueDefaultSpec(string name)
    {
        TalentsBase t = RogueSpec(name).TalentSpec();
        if (t == null) throw new InvalidOperationException(name + " spec failed to parse");
        return (RogueTalents)t;
    }

    static Character NewRogue(string name, RogueTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Rogue;
        c.CurrentModel = "Rogue";
        c.RogueTalents = talents;
        if (raidBuffs)
        {
            string[] buffs = new string[] {
                "Horn of Winter", "Battle Shout", "Unleashed Rage", "Improved Moonkin Form",
                "Leader of the Pack", "Improved Icy Talons", "Power Word: Fortitude", "Mark of the Wild",
                "Blessing of Kings", "Sunder Armor", "Faerie Fire", "Totem of Wrath",
                "Flask of Endless Rage", "Agility Food", "Heroism/Bloodlust",
            };
            foreach (string b in buffs) { c.ActiveBuffsAdd(b); }
        }
        return c;
    }

    static void FinishRogue(Character c, CalculationOptionsRogue options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    static void RogueGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Assassination Mutilate 51/18/2, NightElf, raid buffs, 264 T10 5pc, Heartpierce +
        //    Lungbreaker (Heartpierce 264 proc path), 300s, target 83.
        {
            Character c = NewRogue("RogueMutilate264", RogueDefaultSpec("Mutilate"), CharacterRace.NightElf, true);
            GearRogue264(c, true, true, GEM_ROGUE_YELLOW_HIT, GEM_ROGUE_BLUE, 49982, 50183);
            CalculationOptionsRogue o = new CalculationOptionsRogue();
            o.CustomMHPoison = 1; o.CustomOHPoison = 2;
            FinishRogue(c, o, Path.Combine(outDir, "rogue_mutilate_264.xml"));
        }

        // 2. Combat 20/51/0, Human, raid buffs, 277 heroic 5pc, Scourgeborne Waraxe + Bloodvenom
        //    Blade (Hack and Slash axe/sword path), Deathbringer's Will + Tiny Abomination, 300s.
        {
            Character c = NewRogue("RogueCombat277", RogueDefaultSpec("Combat"), CharacterRace.Human, true);
            GearRogue277(c, true, true, GEM_ROGUE_YELLOW_CRIT, GEM_ROGUE_BLUE, 50654, 50672);
            CalculationOptionsRogue o = new CalculationOptionsRogue();
            o.CustomCPG = 1; o.CustomFinisher = 1; o.CustomCPFinisher = 5; o.CustomCPSnD = 5;
            FinishRogue(c, o, Path.Combine(outDir, "rogue_combat_277.xml"));
        }

        // 3. Subtlety (constructed 20/0/51 Hemo build with eviscerate/rupture/SnD glyphs, Serrated
        //    Blades), Undead, raid buffs, 264 gear, Heartpierce + Rib Spreader, 300s, target 83.
        {
            Character c = NewRogue("RogueSubtlety264", RogueSubtletyTalents(), CharacterRace.Undead, true);
            GearRogue264(c, true, true, GEM_ROGUE_YELLOW_CRIT, GEM_ROGUE_BLUE, 49982, 50016);
            CalculationOptionsRogue o = new CalculationOptionsRogue();
            o.EnableMuti = false; o.EnableSS = true; o.EnableBS = false; o.EnableHemo = true;
            o.CustomCPG = 3; o.CustomFinisher = 1; o.CustomCPFinisher = 5; o.CustomCPSnD = 5;
            FinishRogue(c, o, Path.Combine(outDir, "rogue_subtlety_264.xml"));
        }

        // 4. Mutilate, Tauren, NO raid buffs, 264 gear, 60s fight, target 80, low latency variance.
        {
            Character c = NewRogue("RogueNoBuffs60", RogueDefaultSpec("Mutilate"), CharacterRace.Tauren, false);
            GearRogue264(c, true, true, GEM_ROGUE_YELLOW_CRIT, GEM_ROGUE_BLUE, 50016, 50183);
            CalculationOptionsRogue o = new CalculationOptionsRogue();
            o.Duration = 60;
            o.TargetLevel = 80;
            o.TargetArmor = (int)StatConversion.NPC_ARMOR[80 - 80];
            o.LagVariance = 50;
            FinishRogue(c, o, Path.Combine(outDir, "rogue_nobuffs_60.xml"));
        }

        // 5. Mutilate, BloodElf, 277 T10 + Deathbringer's Will and Tiny Abomination in a Jar
        //    (DeathbringerProc / MoteOfAnger paths), Heartpierce 277 + Lungbreaker 277, 600s.
        {
            Character c = NewRogue("RogueProcs277", RogueDefaultSpec("Mutilate"), CharacterRace.BloodElf, true);
            GearRogue277(c, true, true, GEM_ROGUE_YELLOW_HIT, GEM_ROGUE_BLUE, 50641, 50621);
            CalculationOptionsRogue o = new CalculationOptionsRogue();
            o.Duration = 600;
            o.TrinketOffset = 20f;
            FinishRogue(c, o, Path.Combine(outDir, "rogue_procs_277.xml"));
        }

        // 6. CUSTOM forced rotation, Combat spec, no buffs, target level 80 with level-80 armor,
        //    non-poisonable target, no bleed up, 264 gear, 300s (custom CPG/CP/poison options).
        {
            Character c = NewRogue("RogueCustom80", RogueDefaultSpec("Combat"), CharacterRace.Human, false);
            GearRogue264(c, true, true, GEM_ROGUE_YELLOW_HIT, GEM_ROGUE_BLUE, 50412, 50411);
            CalculationOptionsRogue o = new CalculationOptionsRogue();
            o.ForceCustom = true;
            o.CustomCPG = 1; o.CustomUseRupt = true; o.CustomFinisher = 1; o.CustomCPFinisher = 3;
            o.CustomCPSnD = 4; o.CustomMHPoison = 3; o.CustomOHPoison = 3; o.CustomUseTotT = false;
            o.TargetLevel = 80;
            o.TargetArmor = (int)StatConversion.NPC_ARMOR[80 - 80];
            o.TargetPoisonable = false;
            o.BleedIsUp = false;
            FinishRogue(c, o, Path.Combine(outDir, "rogue_custom_80.xml"));
        }
    }

    // ==================================================================
    // Tree (restoration druid) fixture generation (Phase 6 oracle)
    // ==================================================================

    static void TreeMain(string outDir)
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Calculations.LoadModel(typeof(CalculationsTree));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        TreeGenerate(outDir);
    }

    static SavedTalentSpec TreeSpec(string name)
    {
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Druid))
            if (spec.Name == name) return spec;
        throw new InvalidOperationException("druid spec not found: " + name);
    }

    static void ListTree()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== DRUID TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Druid))
        {
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));
            TalentsBase t = spec.TalentSpec();
            if (t != null) PrintTalents(spec.Name, t);
        }

        Console.WriteLine();
        Console.WriteLine("== LASHERWEAVE / DRUID HEALER SET ITEMS ==");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => (i.Name.Contains("Lasherweave") || i.Name.Contains("Malfurion"))
                && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264)
            .OrderBy(i => i.SetName).ThenBy(i => i.Slot).ThenBy(i => i.ItemLevel))
            Console.WriteLine(ItemLine(item));

        Console.WriteLine();
        Console.WriteLine("== LEATHER/CLOTH HEALER ITEMS (epic, ilvl>=264, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                    && (i.Type == ItemType.Leather || i.Type == ItemType.Cloth)
                    && (i.Stats.SpellPower > 0 || i.Stats.Spirit > 0 || i.Stats.Mp5 > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(14))
                Console.WriteLine(ItemLine(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger, ItemSlot.Trinket })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                    && (i.Stats.SpellPower > 0 || i.Stats.Mp5 > 0 || i.Stats.Spirit > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(14))
                Console.WriteLine(ItemLine(item));
        }
        Console.WriteLine("-- OffHand --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.OffHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && i.Stats.SpellPower > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(12))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- MainHand (one-hand caster) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.MainHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && (i.Type == ItemType.OneHandMace || i.Type == ItemType.Dagger)
                && i.Stats.SpellPower > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(12))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- Ranged (Idols with spell power / heal procs) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Type == ItemType.Idol && i.Quality == ItemQuality.Epic
                && i.ItemLevel >= 200)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(24))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- MainHand (staff, caster) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.MainHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && i.Type == ItemType.Staff)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(12))
            Console.WriteLine(ItemLine(item));
    }

    static Character NewTree(string name, DruidTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Druid;
        c.CurrentModel = "Tree";
        c.DruidTalents = talents;
        if (raidBuffs) TreeRaidBuffs(c);
        return c;
    }

    // The Tree model has no model-specific SetDefaults (base is a no-op), so the raid buffs that a
    // restoration druid would raid with are added explicitly (same list the healer priest uses).
    static void TreeRaidBuffs(Character c)
    {
        c.ActiveBuffsAdd("Tree of Life Aura");
        c.ActiveBuffsAdd("Arcane Intellect");
        c.ActiveBuffsAdd("Divine Spirit");
        c.ActiveBuffsAdd("Mana Spring Totem");
        c.ActiveBuffsAdd("Restorative Totems");
        c.ActiveBuffsAdd("Wrath of Air Totem");
        c.ActiveBuffsAdd("Totem of Wrath (Spell Power)");
        c.ActiveBuffsAdd("Improved Moonkin Form");
        c.ActiveBuffsAdd("Moonkin Form");
        c.ActiveBuffsAdd("Power Word: Fortitude");
        c.ActiveBuffsAdd("Improved Power Word: Fortitude");
        c.ActiveBuffsAdd("Mark of the Wild");
        c.ActiveBuffsAdd("Improved Mark of the Wild");
        c.ActiveBuffsAdd("Blessing of Kings");
        c.ActiveBuffsAdd("Shadow Protection");
        c.ActiveBuffsAdd("Flask of the Frost Wyrm");
        c.ActiveBuffsAdd("Spell Power Food");
    }

    static void FinishTree(Character c, CalculationOptionsTree options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    // 264 resto druid: 5pc Lasherweave Garb (T10 normal) + ICC 25 offpieces, idol, dagger + offhand.
    static void GearTree264(Character c, bool gems, bool enchants, int yellowGem, int blueGem)
    {
        RE(c, CharacterSlot.Head, 51137, gems, enchants, yellowGem, blueGem);       // Sanctified Lasherweave Helmet (Garb)
        RE(c, CharacterSlot.Neck, 49975, gems, enchants, yellowGem, blueGem);       // Bone Sentinel's Amulet
        RE(c, CharacterSlot.Shoulders, 51135, gems, enchants, yellowGem, blueGem);  // Sanctified Lasherweave Pauldrons (Garb)
        RE(c, CharacterSlot.Back, 50014, gems, enchants, yellowGem, blueGem);       // Greatcloak of the Turned Champion
        RE(c, CharacterSlot.Chest, 51139, gems, enchants, yellowGem, blueGem);      // Sanctified Lasherweave Robes (Garb)
        RE(c, CharacterSlot.Wrist, 50032, gems, enchants, yellowGem, blueGem);      // Death Surgeon's Sleeves
        RE(c, CharacterSlot.Hands, 51138, gems, enchants, yellowGem, blueGem);      // Sanctified Lasherweave Gauntlets (Garb)
        RE(c, CharacterSlot.Waist, 50063, gems, enchants, yellowGem, blueGem);      // Lingering Illness
        RE(c, CharacterSlot.Legs, 51136, gems, enchants, yellowGem, blueGem);       // Sanctified Lasherweave Legplates (Garb)
        RE(c, CharacterSlot.Feet, 49893, gems, enchants, yellowGem, blueGem);       // Sandals of Consecration
        RE(c, CharacterSlot.Finger1, 50400, gems, enchants, yellowGem, blueGem);    // Ashen Band of Endless Wisdom
        RE(c, CharacterSlot.Finger2, 50720, gems, enchants, yellowGem, blueGem);    // Incarnadine Band of Mending
        RE(c, CharacterSlot.Trinket1, 50346, gems, enchants, yellowGem, blueGem);   // Sliver of Pure Ice (Use mana)
        RE(c, CharacterSlot.Trinket2, 50358, gems, enchants, yellowGem, blueGem);   // Purified Lunar Dust (SpellCast proc)
        RE(c, CharacterSlot.MainHand, 49968, gems, enchants, yellowGem, blueGem);   // Frozen Bonespike (264 dagger)
        RE(c, CharacterSlot.OffHand, 51922, gems, enchants, yellowGem, blueGem);    // Scourgelord's Baton (264 held)
        RE(c, CharacterSlot.Ranged, 47671, gems, enchants, yellowGem, blueGem);     // Idol of Flaring Growth
    }

    // 277 heroic resto druid: 5pc Sanctified Lasherweave Garb + heroic ICC offpieces, proc/Use
    // trinkets, idol, staff.
    static void GearTree277(Character c, bool gems, bool enchants, int yellowGem, int blueGem)
    {
        RE(c, CharacterSlot.Head, 51302, gems, enchants, yellowGem, blueGem);       // Sanctified Lasherweave Helmet (Garb)
        RE(c, CharacterSlot.Neck, 50609, gems, enchants, yellowGem, blueGem);       // Bone Sentinel's Amulet
        RE(c, CharacterSlot.Shoulders, 51304, gems, enchants, yellowGem, blueGem);  // Sanctified Lasherweave Pauldrons (Garb)
        RE(c, CharacterSlot.Back, 50668, gems, enchants, yellowGem, blueGem);       // Greatcloak of the Turned Champion
        RE(c, CharacterSlot.Chest, 51300, gems, enchants, yellowGem, blueGem);      // Sanctified Lasherweave Robes (Garb)
        RE(c, CharacterSlot.Wrist, 50686, gems, enchants, yellowGem, blueGem);      // Death Surgeon's Sleeves
        RE(c, CharacterSlot.Hands, 51301, gems, enchants, yellowGem, blueGem);      // Sanctified Lasherweave Gauntlets (Garb)
        RE(c, CharacterSlot.Waist, 50702, gems, enchants, yellowGem, blueGem);      // Lingering Illness
        RE(c, CharacterSlot.Legs, 51303, gems, enchants, yellowGem, blueGem);       // Sanctified Lasherweave Legplates (Garb)
        RE(c, CharacterSlot.Feet, 50699, gems, enchants, yellowGem, blueGem);       // Plague Scientist's Boots
        RE(c, CharacterSlot.Finger1, 54585, gems, enchants, yellowGem, blueGem);    // Ring of Phased Regeneration
        RE(c, CharacterSlot.Finger2, 50610, gems, enchants, yellowGem, blueGem);    // Marrowgar's Frigid Eye
        RE(c, CharacterSlot.Trinket1, 50366, gems, enchants, yellowGem, blueGem);   // Althor's Abacus (HealingSpellHit proc)
        RE(c, CharacterSlot.Trinket2, 54589, gems, enchants, yellowGem, blueGem);   // Glowing Twilight Scale (Use, recursive)
        RE(c, CharacterSlot.MainHand, 50608, gems, enchants, yellowGem, blueGem);   // Frozen Bonespike (277 dagger)
        RE(c, CharacterSlot.OffHand, 50635, gems, enchants, yellowGem, blueGem);    // Sundial of Eternal Dusk
        RE(c, CharacterSlot.Ranged, 46138, gems, enchants, yellowGem, blueGem);     // Idol of the Flourishing Life
    }

    static void TreeGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Tree 14/0/57, full raid buffs, 264 Lasherweave Garb (T10 2pc+4pc), sp/haste gems,
        //    default options (300s, 4 WG/min, 2 Swiftmend/min), target 83.
        {
            TalentsBase t = TreeSpec("Tree").TalentSpec();
            if (t == null) throw new InvalidOperationException("Tree spec failed to parse");
            DruidTalents tree = (DruidTalents)t;
            Character c = NewTree("TreeRaid264", tree, CharacterRace.NightElf, true);
            GearTree264(c, true, true, GEM_HEAL_YELLOW_HASTE, GEM_HEAL_BLUE_MP5);
            CalculationOptionsTree o = new CalculationOptionsTree();
            FinishTree(c, o, Path.Combine(outDir, "tree_264_raid.xml"));
        }

        // 2. 277 heroic set + proc/Use trinkets, Nature's Grace modelled (IgnoreNaturesGrace=false),
        //    average proc mode, long 600s fight, rapid-rejuv / nourish / wild growth glyphs.
        {
            TalentsBase t = TreeSpec("Tree").TalentSpec();
            if (t == null) throw new InvalidOperationException("Tree spec failed to parse");
            DruidTalents tree = (DruidTalents)t;
            tree.GlyphOfRapidRejuvenation = true;
            tree.GlyphOfNourish = true;
            tree.GlyphOfWildGrowth = true;
            Character c = NewTree("TreeProcs277", tree, CharacterRace.Troll, true);
            GearTree277(c, true, true, GEM_HEAL_YELLOW_INT, GEM_HEAL_BLUE_MP5);
            CalculationOptionsTree o = new CalculationOptionsTree();
            o.IgnoreNaturesGrace = false;
            o.ProcType = 1;
            o.Current.FightDuration = 600;
            o.Current.ReplenishmentUptime = 50;
            FinishTree(c, o, Path.Combine(outDir, "tree_277_procs.xml"));
        }

        // 3. NO raid buffs, 264 gear, short 90s burst fight, forced RjRgLbN burst rotation,
        //    fast lifebloom stacking, 60% idle cast time, target 83.
        {
            TalentsBase t = TreeSpec("Tree").TalentSpec();
            if (t == null) throw new InvalidOperationException("Tree spec failed to parse");
            DruidTalents tree = (DruidTalents)t;
            Character c = NewTree("TreeNoBuffs90", tree, CharacterRace.Tauren, false);
            GearTree264(c, true, true, GEM_HEAL_YELLOW_HASTE, GEM_BLUE);
            CalculationOptionsTree o = new CalculationOptionsTree();
            o.Current.FightDuration = 90;
            o.Current.LifebloomStackType = 1;   // Fast
            o.Current.IdleCastTimePercent = 60;
            o.SingleTargetRotation = 5;          // RjRgLbN
            FinishTree(c, o, Path.Combine(outDir, "tree_nobuffs_90.xml"));
        }

        // 4. Mana-starved 20 min fight, no replenishment, no innervates, rolling lifebloom stack,
        //    custom time adjustments and OOM reductions (exercises both correction loops).
        {
            TalentsBase t = TreeSpec("Tree").TalentSpec();
            if (t == null) throw new InvalidOperationException("Tree spec failed to parse");
            DruidTalents tree = (DruidTalents)t;
            tree.GlyphOfInnervate = false;
            Character c = NewTree("TreeStarved1200", tree, CharacterRace.BloodElf, true);
            GearTree264(c, true, true, GEM_HEAL_YELLOW_INT, GEM_HEAL_BLUE_MP5);
            RE(c, CharacterSlot.Ranged, 40342, true, true, GEM_HEAL_YELLOW_INT, GEM_HEAL_BLUE_MP5); // Idol of Awakening
            CalculationOptionsTree o = new CalculationOptionsTree();
            o.Current.FightDuration = 1200;
            o.Current.ReplenishmentUptime = 0;
            o.Current.Innervates = 0;
            o.Current.LifebloomStackType = 2;    // Rolling
            o.Current.IdleCastTimePercent = 5;
            o.Current.AdjustTimeRejuv = 60;
            o.Current.AdjustTimeRegrowth = 40;
            o.Current.AdjustTimeNourish = 30;
            o.Current.AdjustTimeLifebloom = 70;
            o.Current.AdjustTimeWildGrowth = 50;
            o.Current.ReduceOOMRejuv = 60;
            o.Current.ReduceOOMNourish = 70;
            o.Current.ReduceOOMRegrowth = 80;
            o.Current.ReduceOOMLifebloom = 90;
            o.Current.ReduceOOMWildGrowth = 20;
            FinishTree(c, o, Path.Combine(outDir, "tree_starved_1200.xml"));
        }

        // 5. Sustained rolling-HoT loadout: 277 gear, slow lifebloom stack, custom nourish HoT mix,
        //    Living Seed at 100%, 100% rejuvenation/regrowth uptime, revitalize PPM, 300s.
        {
            TalentsBase t = TreeSpec("Tree").TalentSpec();
            if (t == null) throw new InvalidOperationException("Tree spec failed to parse");
            DruidTalents tree = (DruidTalents)t;
            tree.GlyphOfSwiftmend = true;
            tree.GlyphOfLifebloom = true;
            Character c = NewTree("TreeRollingHots", tree, CharacterRace.NightElf, true);
            GearTree277(c, true, true, GEM_HEAL_YELLOW_HASTE, GEM_HEAL_BLUE_MP5);
            RE(c, CharacterSlot.Ranged, 40711, true, true, GEM_HEAL_YELLOW_HASTE, GEM_HEAL_BLUE_MP5); // Idol of Lush Moss
            CalculationOptionsTree o = new CalculationOptionsTree();
            o.Current.FightDuration = 300;
            o.Current.LifebloomStackType = 3;    // Slow2
            o.Current.LifebloomStackAmount = 2;
            o.Current.LifebloomFrac = 0;
            o.Current.RejuvAmount = 1;
            o.Current.RegrowthAmount = 1;
            o.Current.NourishFrac = 20;
            o.Current.Nourish1 = 40;
            o.Current.Nourish2 = 30;
            o.Current.Nourish3 = 20;
            o.Current.Nourish4 = 10;
            o.Current.LivingSeedEfficiency = 100;
            o.Current.RevitalizePPM = 10;
            o.Current.WildGrowthPerMinute = 6;
            o.Current.SwiftmendPerMinute = 4;
            FinishTree(c, o, Path.Combine(outDir, "tree_rolling_hots.xml"));
        }
    }

    // ==================================================================
    // Warlock fixture generation (Phase 6 oracle)
    // ==================================================================

    static void WarlockMain(string outDir)
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Calculations.LoadModel(typeof(CalculationsWarlock));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        WarlockGenerate(outDir);
    }

    static SavedTalentSpec WarlockSpec(string name)
    {
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Warlock))
            if (spec.Name == name) return spec;
        throw new InvalidOperationException("warlock spec not found: " + name);
    }

    static void ListWarlock()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== WARLOCK TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Warlock))
        {
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));
            TalentsBase t = spec.TalentSpec();
            if (t != null) PrintTalents(spec.Name, t);
        }

        Console.WriteLine();
        Console.WriteLine("== WARLOCK GEM SPOT CHECK ==");
        int[] gids = new int[] { 39911, 39998, 40113, 42144, 39941, 40026, 40133, 39959, 40051, 40155, 39957, 40049, 40153, 41333, 41285, 49110 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLine(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== DARK COVEN SET ITEMS (>=264) ==");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.SetName != null && i.SetName.Contains("Dark Coven") && i.ItemLevel >= 264)
            .OrderBy(i => i.Slot).ThenBy(i => i.ItemLevel).ThenBy(i => i.Id))
            Console.WriteLine(ItemLine(item));

        Console.WriteLine();
        Console.WriteLine("== CLOTH CASTER DPS ITEMS (epic, ilvl>=264, sp + crit/haste/hit, by slot) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264 && i.Type == ItemType.Cloth
                    && i.Stats.SpellPower > 0 && (i.Stats.CritRating > 0 || i.Stats.HasteRating > 0 || i.Stats.HitRating > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(14))
                Console.WriteLine(ItemLine(item));
        }
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                    && i.Stats.SpellPower > 0 && (i.Stats.CritRating > 0 || i.Stats.HasteRating > 0 || i.Stats.HitRating > 0))
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(14))
                Console.WriteLine(ItemLine(item));
        }
        Console.WriteLine("-- Trinket --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Trinket && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && (i.Stats.SpellPower > 0 || i.Stats.HasteRating > 0 || i.Stats.CritRating > 0 || i.Stats.HitRating > 0))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(40))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- OffHand --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.OffHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && i.Stats.SpellPower > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- MainHand (one-hand caster) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.MainHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && (i.Type == ItemType.OneHandMace || i.Type == ItemType.Dagger || i.Type == ItemType.OneHandSword)
                && i.Stats.SpellPower > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- Ranged (Wands) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Quality == ItemQuality.Epic && i.Type == ItemType.Wand && i.ItemLevel >= 264)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine(item));

        Console.WriteLine();
        Console.WriteLine("== WARLOCK SET BONUS BUFFS ==");
        foreach (Buff b in Buff.AllBuffs.Where(x => x.SetName != null
                && (x.Stats.Warlock2T7 > 0 || x.Stats.Warlock4T7 > 0 || x.Stats.Warlock2T8 > 0 || x.Stats.Warlock4T8 > 0
                    || x.Stats.Warlock2T9 > 0 || x.Stats.Warlock4T9 > 0 || x.Stats.Warlock2T10 > 0 || x.Stats.Warlock4T10 > 0))
            .OrderBy(x => x.Name))
            Console.WriteLine(b.Name + " | set=" + b.SetName + " | threshold=" + b.SetThreshold + " | " + StatLine(b.Stats));
    }

    static void WarlockGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Affliction-ImprovedFelhunter 55/0/16, raid buffs, 264 Dark Coven 5pc (2pc/4pc T10),
        //    sp/haste gemming, Felhunter, 300s, target 83, default Affliction rotation.
        {
            Character c = NewWarlock("WarlockAffliction264", WarlockDefaultSpec("Affliction-ImprovedFelhunter"), CharacterRace.Human, true);
            GearWarlock264(c, true, true, GEM_YELLOW, GEM_BLUE);
            CalculationOptionsWarlock o = CalculationOptionsWarlock.MakeDefaultOptions();
            o.Pet = "Felhunter";
            FinishWarlock(c, o, Path.Combine(outDir, "warlock_affliction_264.xml"));
        }

        // 2. Demonology-Meta/Ruin 0/56/15, raid buffs, 277 Dark Coven 5pc, Felguard + Demonic
        //    Pact/Demonic Empowerment, Metamorphosis uptime, Molten Core, 600s, target 83.
        {
            Character c = NewWarlock("WarlockDemonology277", WarlockDefaultSpec("Demonology-Meta/Ruin"), CharacterRace.Orc, true);
            GearWarlock277(c, true, true, GEM_YELLOW, GEM_BLUE);
            CalculationOptionsWarlock o = CalculationOptionsWarlock.MakeDefaultOptions();
            o.Pet = "Felguard";
            o.ActiveRotationIndex = 1;
            o.Duration = 600;
            FinishWarlock(c, o, Path.Combine(outDir, "warlock_demonology_277.xml"));
        }

        // 3. Destruction-Standard 3/13/55, raid buffs, 264 Dark Coven 5pc, Imp + Empowered Imp,
        //    Backdraft/Molten Core? (Backdraft), Chaos Bolt, Pyroclasm, Conflagrate clipping
        //    (Glyph of Conflagrate deliberately unset), 300s, target 83.
        {
            WarlockTalents dest = WarlockDefaultSpec("Destruction-Standard");
            dest.GlyphConflag = false;
            Character c = NewWarlock("WarlockDestruction264", dest, CharacterRace.Gnome, true);
            GearWarlock264(c, true, true, GEM_YELLOW, GEM_BLUE);
            CalculationOptionsWarlock o = CalculationOptionsWarlock.MakeDefaultOptions();
            o.Pet = "Imp";
            o.ActiveRotationIndex = 2;
            FinishWarlock(c, o, Path.Combine(outDir, "warlock_destruction_264.xml"));
        }

        // 4. Affliction-DarkPact 56/0/15, NO raid buffs, 264 gear, sp/haste gemming (under the hit
        //    cap: RecordCollisionDelays recurses without bound once HitChance hits exactly 1, see
        //    docs/dev/SUSPECTED_BUGS.md), no pet, custom rotation that self-casts Curse Of The Elements,
        //    Grand Firestone imbue, 60s fight, target 83, 500ms latency. Note Latency is in seconds.
        {
            Character c = NewWarlock("WarlockNoBuffs60", WarlockDefaultSpec("Affliction-DarkPact"), CharacterRace.BloodElf, false);
            GearWarlock264(c, true, true, GEM_YELLOW, GEM_BLUE);
            // stay below the spell-hit cap (hit rating only from the T10 gloves/legs): the original
            // RecordCollisionDelays recurses without bound when HitChance is exactly 1 (SUSPECTED_BUGS)
            WG(c, CharacterSlot.Wrist, 51918, true, true, GEM_YELLOW, GEM_BLUE);    // Bracers of Dark Blessings
            WG(c, CharacterSlot.Waist, 50997, true, true, GEM_YELLOW, GEM_BLUE);    // Circle of Ossus
            WG(c, CharacterSlot.MainHand, 51910, true, true, GEM_YELLOW, GEM_BLUE); // Midnight Sun
            CalculationOptionsWarlock o = new CalculationOptionsWarlock();
            o.Pet = "None";
            o.Imbue = "Grand Firestone";
            o.Duration = 60;
            o.TargetLevel = 83;
            o.Latency = .5f;
            o.Rotations = new List<Rawr.Warlock.Rotation>();
            o.Rotations.Add(new Rawr.Warlock.Rotation(
                "Self Curse",
                "Shadow Bolt",
                "Drain Soul",
                "Curse Of The Elements",
                "Haunt",
                "Unstable Affliction",
                "Corruption"));
            o.ActiveRotationIndex = 0;
            FinishWarlock(c, o, Path.Combine(outDir, "warlock_nobuffs_60.xml"));
        }

        // 5. Destruction-Replenishment 0/13/58, raid buffs, 277 gear, Imp, Dislodged Foreign Object
        //    (haste proc) + Charred Twilight Scale (Use haste), 600s, target 83.
        {
            Character c = NewWarlock("WarlockProcs277", WarlockDefaultSpec("Destruction-Replenishment"), CharacterRace.Undead, true);
            GearWarlock277(c, true, true, GEM_YELLOW, GEM_BLUE);
            CalculationOptionsWarlock o = CalculationOptionsWarlock.MakeDefaultOptions();
            o.Pet = "Imp";
            o.ActiveRotationIndex = 2;
            o.Duration = 600;
            o.UseInfernal = true;
            FinishWarlock(c, o, Path.Combine(outDir, "warlock_procs_277.xml"));
        }

        // 6. Demonology-FG/Ember 0/41/30, raid buffs, 277 gear, Felguard, Soul Fire execute phase
        //    (Decimation, custom 35%/25% windows), 300s, target 83.
        {
            Character c = NewWarlock("WarlockExecute277", WarlockDefaultSpec("Demonology-FG/Ember"), CharacterRace.Draenei, true);
            GearWarlock277(c, true, true, GEM_YELLOW, GEM_BLUE);
            CalculationOptionsWarlock o = CalculationOptionsWarlock.MakeDefaultOptions();
            o.Pet = "Felguard";
            o.ActiveRotationIndex = 1;
            o.ThirtyFive = .35f;
            o.TwentyFive = .2f;
            FinishWarlock(c, o, Path.Combine(outDir, "warlock_execute_277.xml"));
        }

        // 7. Pre-rotation options (plain constructor: empty Rotations list, Pet "None", leftover
        //    defaults), Affliction-Imp 53/0/18, raid buffs, 264 gear, 300s, target 83. Exercises
        //    the default Rotation() fallback path for old character files.
        {
            Character c = NewWarlock("WarlockDefaults264", WarlockDefaultSpec("Affliction-Imp"), CharacterRace.Human, true);
            GearWarlock264(c, true, true, GEM_YELLOW, GEM_BLUE);
            FinishWarlock(c, new CalculationOptionsWarlock(), Path.Combine(outDir, "warlock_defaults_264.xml"));
        }
    }

    static WarlockTalents WarlockDefaultSpec(string name)
    {
        TalentsBase t = WarlockSpec(name).TalentSpec();
        if (t == null) throw new InvalidOperationException(name + " spec failed to parse");
        return (WarlockTalents)t;
    }

    static Character NewWarlock(string name, WarlockTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Warlock;
        c.CurrentModel = "Warlock";
        c.WarlockTalents = talents;
        if (raidBuffs) WarlockRaidBuffs(c);
        return c;
    }

    // Spell-damage raid buffs/debuffs a warlock would raid with (the ShadowPriest list, which
    // already matches what the model's GetPetBuffStats filters into pet stats).
    static void WarlockRaidBuffs(Character c)
    {
        string[] buffs = new string[] {
            "Arcane Intellect", "Improved Moonkin Form", "Vampiric Touch", "Mana Spring Totem",
            "Restorative Totems", "Moonkin Form", "Wrath of Air Totem", "Totem of Wrath (Spell Power)",
            "Divine Spirit", "Power Word: Fortitude", "Improved Power Word: Fortitude",
            "Mark of the Wild", "Improved Mark of the Wild", "Blessing of Kings", "Shadow Protection",
            "Heart of the Crusader", "Sanctified Retribution", "Improved Scorch", "Judgement of Wisdom",
            "Earth and Moon", "Misery", "Crypt Fever", "Flask of the Frost Wyrm", "Spell Power Food",
            "Heroism/Bloodlust",
        };
        foreach (string b in buffs) c.ActiveBuffsAdd(b);
    }

    static void FinishWarlock(Character c, CalculationOptionsWarlock options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    static void WG(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants, int yellowGem, int blueGem)
    {
        RES(c, slot, itemId, gems, enchants, yellowGem, blueGem);
    }

    // 264 warlock: 5pc Sanctified Dark Coven (T10 normal) + ICC 25 offpieces, dagger + held
    // offhand, wand, haste proc/Use trinkets.
    static void GearWarlock264(Character c, bool gems, bool enchants, int yellowGem, int blueGem)
    {
        WG(c, CharacterSlot.Head, 51208, gems, enchants, yellowGem, blueGem);       // Sanctified Dark Coven Hood
        WG(c, CharacterSlot.Neck, 50182, gems, enchants, yellowGem, blueGem);       // Blood Queen's Crimson Choker
        WG(c, CharacterSlot.Shoulders, 51205, gems, enchants, yellowGem, blueGem);  // Sanctified Dark Coven Shoulderpads
        WG(c, CharacterSlot.Back, 50205, gems, enchants, yellowGem, blueGem);       // Frostbinder's Shredded Cape
        WG(c, CharacterSlot.Chest, 51206, gems, enchants, yellowGem, blueGem);      // Sanctified Dark Coven Robe
        WG(c, CharacterSlot.Wrist, 49994, gems, enchants, yellowGem, blueGem);      // The Lady's Brittle Bracers
        WG(c, CharacterSlot.Hands, 51209, gems, enchants, yellowGem, blueGem);      // Sanctified Dark Coven Gloves
        WG(c, CharacterSlot.Waist, 50996, gems, enchants, yellowGem, blueGem);      // Belt of Omission
        WG(c, CharacterSlot.Legs, 51207, gems, enchants, yellowGem, blueGem);       // Sanctified Dark Coven Leggings
        WG(c, CharacterSlot.Feet, 50062, gems, enchants, yellowGem, blueGem);       // Plague Scientist's Boots
        WG(c, CharacterSlot.Finger1, 50397, gems, enchants, yellowGem, blueGem);    // Ashen Band of Unmatched Destruction
        WG(c, CharacterSlot.Finger2, 50399, gems, enchants, yellowGem, blueGem);    // Ashen Band of Unmatched Wisdom
        WG(c, CharacterSlot.Trinket1, 50353, gems, enchants, yellowGem, blueGem);   // Dislodged Foreign Object (haste proc)
        WG(c, CharacterSlot.Trinket2, 50346, gems, enchants, yellowGem, blueGem);   // Sliver of Pure Ice (Use spell power)
        WG(c, CharacterSlot.MainHand, 51815, gems, enchants, yellowGem, blueGem);   // Bleak Coldarra Carver (hit)
        WG(c, CharacterSlot.OffHand, 50423, gems, enchants, yellowGem, blueGem);    // Sundial of Eternal Dusk
        WG(c, CharacterSlot.Ranged, 50033, gems, enchants, yellowGem, blueGem);     // Corpse-Impaling Spike (wand)
    }

    // 277 heroic warlock: 5pc Sanctified Dark Coven (T10 heroic) + heroic ICC offpieces, haste
    // proc/Use trinkets, dagger/mace + held offhand, wand.
    static void GearWarlock277(Character c, bool gems, bool enchants, int yellowGem, int blueGem)
    {
        WG(c, CharacterSlot.Head, 51231, gems, enchants, yellowGem, blueGem);       // Sanctified Dark Coven Hood
        WG(c, CharacterSlot.Neck, 50658, gems, enchants, yellowGem, blueGem);       // Amulet of the Silent Eulogy
        WG(c, CharacterSlot.Shoulders, 51234, gems, enchants, yellowGem, blueGem);  // Sanctified Dark Coven Shoulderpads
        WG(c, CharacterSlot.Back, 50628, gems, enchants, yellowGem, blueGem);       // Frostbinder's Shredded Cape
        WG(c, CharacterSlot.Chest, 51233, gems, enchants, yellowGem, blueGem);      // Sanctified Dark Coven Robe
        WG(c, CharacterSlot.Wrist, 50651, gems, enchants, yellowGem, blueGem);      // The Lady's Brittle Bracers
        WG(c, CharacterSlot.Hands, 51230, gems, enchants, yellowGem, blueGem);      // Sanctified Dark Coven Gloves
        WG(c, CharacterSlot.Waist, 50613, gems, enchants, yellowGem, blueGem);      // Crushing Coldwraith Belt
        WG(c, CharacterSlot.Legs, 51232, gems, enchants, yellowGem, blueGem);       // Sanctified Dark Coven Leggings
        WG(c, CharacterSlot.Feet, 50699, gems, enchants, yellowGem, blueGem);       // Plague Scientist's Boots
        WG(c, CharacterSlot.Finger1, 50398, gems, enchants, yellowGem, blueGem);    // Ashen Band of Endless Destruction
        WG(c, CharacterSlot.Finger2, 50614, gems, enchants, yellowGem, blueGem);    // Loop of the Endless Labyrinth
        WG(c, CharacterSlot.Trinket1, 50348, gems, enchants, yellowGem, blueGem);   // Dislodged Foreign Object (haste proc)
        WG(c, CharacterSlot.Trinket2, 54588, gems, enchants, yellowGem, blueGem);   // Charred Twilight Scale (Use haste)
        WG(c, CharacterSlot.MainHand, 50734, gems, enchants, yellowGem, blueGem);   // Royal Scepter of Terenas II
        WG(c, CharacterSlot.OffHand, 50719, gems, enchants, yellowGem, blueGem);    // Shadow Silk Spindle
        WG(c, CharacterSlot.Ranged, 50684, gems, enchants, yellowGem, blueGem);     // Corpse-Impaling Spike (wand)
    }

    // ==================================================================
    // DPSDK fixture generation (Phase 6 oracle)
    // ==================================================================

    // dpsdk gems (from CalculationsDPSDK.DefaultGemmingTemplates, epic tier)
    const int GEM_DPSDK_RED = 40111;       // Bold Cardinal Ruby +20 Str
    const int GEM_DPSDK_YELLOW = 40142;    // Inscribed Ametrine +10 Str +10 Crit
    const int GEM_DPSDK_BLUE = 40129;      // Sovereign Dreadstone +10 Str +15 Sta
    const int GEM_DPSDK_META = 41285;      // Chaotic Skyflare Diamond
    const int GEM_DPSDK_PRISMATIC = 49110; // Nightmare Tear

    // dpsdk runes
    const int ENCH_DPSDK_FC = 3368;        // Rune of the Fallen Crusader
    const int ENCH_DPSDK_RAZORICE = 3370;  // Rune of Razorice

    static void DPSDKMain(string outDir)
    {
        Calculations.LoadModel(typeof(CalculationsDPSDK));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        DPSDKGenerate(outDir);
    }

    static void ListDPSDK()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== DEATH KNIGHT TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.DeathKnight))
        {
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));
            TalentsBase t = spec.TalentSpec();
            if (t != null) PrintTalents(spec.Name, t);
        }
        Console.WriteLine();
        PrintTalents("Unholy DPS (constructed)", UnholyDPSTalents(true));
        PrintTalents("Frost DPS (constructed)", FrostDPSTalents(true, false));
        PrintTalents("Blood DPS (constructed)", BloodDPSTalents(true));

        Console.WriteLine();
        Console.WriteLine("== DPSDK GEM SPOT CHECK ==");
        int[] gids = new int[] { 39900, 39996, 40111, 42142, 39909, 40002, 40117, 42153, 40129, 40142, 41285, 49110 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLine(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== PLATE DPS ITEMS (epic, ilvl>=264, str + arp/crit/hit) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264 && i.Type == ItemType.Plate
                    && i.Stats.Strength > 0 && i.Stats.DodgeRating == 0 && i.Stats.ParryRating == 0 && i.Stats.DefenseRating == 0)
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(10))
                Console.WriteLine(ItemLine(item));
        }
        Console.WriteLine("-- TwoHand weapons (epic, ilvl>=264) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.TwoHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && i.Stats.Strength > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(24))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- OneHand weapons (epic, ilvl>=264, str/arp) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => (i.Slot == ItemSlot.MainHand || i.Slot == ItemSlot.OneHand) && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && i.Stats.Strength > 0)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(16))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- Trinket (epic, ilvl>=264, str/arp/ap) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Trinket && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                && (i.Stats.Strength > 0 || i.Stats.ArmorPenetrationRating > 0 || i.Stats.AttackPower > 0))
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine(item));
        Console.WriteLine("-- Ranged (Sigils) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Type == ItemType.Sigil)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name).Take(20))
            Console.WriteLine(ItemLine(item));

        Console.WriteLine();
        Console.WriteLine("== SCOURGELORD SET BONUSES ==");
        foreach (Buff b in Buff.AllBuffs.Where(x => x.SetName != null && x.SetName.Contains("Scourgelord")).OrderBy(x => x.SetName).ThenBy(x => x.Name))
            Console.WriteLine(b.SetName + " | " + b.Name + " | threshold=" + b.SetThreshold + " | " + StatLine(b.Stats));
    }

    static Character NewDPSDK(string name, DeathKnightTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.DeathKnight;
        c.CurrentModel = "DPSDK";
        c.DeathKnightTalents = talents;
        if (raidBuffs) DPSDKRaidBuffs(c);
        return c;
    }

    // Physical DPS buffs/debuffs a death knight would raid with.
    static void DPSDKRaidBuffs(Character c)
    {
        string[] buffs = new string[] {
            "Blessing of Kings", "Blessing of Might", "Strength of Earth Totem", "Windfury Totem",
            "Horn of Winter", "Abomination's Might", "Trueshot Aura", "Leader of the Pack",
            "Rampage", "Battle Shout", "Sanctified Retribution", "Sunder Armor", "Faerie Fire",
            "Mangle", "Blood Frenzy", "Trauma", "Savage Combat", "Improved Faerie Fire",
            "Heart of the Crusader", "Heroism/Bloodlust", "Flask of Endless Rage",
            "Attack Power Food", "Mark of the Wild", "Power Word: Fortitude", "Shadow Protection",
        };
        foreach (string b in buffs) c.ActiveBuffsAdd(b);
    }

    static void FinishDPSDK(Character c, CalculationOptionsDPSDK options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    static int GemForDPSDK(ItemSlot color)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_DPSDK_RED;
            case ItemSlot.Yellow: return GEM_DPSDK_YELLOW;
            case ItemSlot.Blue: return GEM_DPSDK_BLUE;
            case ItemSlot.Meta: return GEM_DPSDK_META;
            case ItemSlot.Prismatic: return GEM_DPSDK_PRISMATIC;
            default: return 0;
        }
    }

    static int EnchForSlotDPSDK(CharacterSlot slot, int weaponEnchant)
    {
        switch (slot)
        {
            case CharacterSlot.Head: return ENCH_CAT_HEAD;
            case CharacterSlot.Shoulders: return ENCH_CAT_SHOULDER;
            case CharacterSlot.Back: return ENCH_CAT_BACK;
            case CharacterSlot.Chest: return ENCH_CAT_CHEST;
            case CharacterSlot.Wrist: return ENCH_CAT_WRIST;
            case CharacterSlot.Hands: return ENCH_CAT_HANDS;
            case CharacterSlot.Legs: return ENCH_CAT_LEGS;
            case CharacterSlot.Feet: return ENCH_CAT_FEET;
            case CharacterSlot.MainHand: return weaponEnchant;
            case CharacterSlot.OffHand: return weaponEnchant;
            default: return 0;
        }
    }

    static void DG(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants, int weaponEnchant)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = RequireItem(itemId);
            if (gems)
            {
                g1 = GemForDPSDK(item.SocketColor1);
                g2 = GemForDPSDK(item.SocketColor2);
                g3 = GemForDPSDK(item.SocketColor3);
            }
            if (enchants) ench = EnchForSlotDPSDK(slot, weaponEnchant);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    // 264 DPS: 5pc Sanctified Scourgelord's Battlegear (T10 normal DPS) + ICC 10/25 offpieces.
    static void GearDPSDK264(Character c, bool gems, bool enchants, int trinket1, int trinket2, int mhId, int rune)
    {
        DG(c, CharacterSlot.Head, 51127, gems, enchants, rune);      // Sanctified Scourgelord Helmet
        DG(c, CharacterSlot.Neck, 50180, gems, enchants, rune);      // Lana'thel's Chain of Flagellation
        DG(c, CharacterSlot.Shoulders, 51125, gems, enchants, rune); // Sanctified Scourgelord Shoulderplates
        DG(c, CharacterSlot.Back, 50467, gems, enchants, rune);      // Might of the Ocean Serpent
        DG(c, CharacterSlot.Chest, 51129, gems, enchants, rune);     // Sanctified Scourgelord Battleplate
        DG(c, CharacterSlot.Wrist, 50002, gems, enchants, rune);     // Polar Bear Claw Bracers
        DG(c, CharacterSlot.Hands, 51128, gems, enchants, rune);     // Sanctified Scourgelord Gauntlets
        DG(c, CharacterSlot.Waist, 50987, gems, enchants, rune);     // Malevolent Girdle
        DG(c, CharacterSlot.Legs, 51126, gems, enchants, rune);      // Sanctified Scourgelord Legplates
        DG(c, CharacterSlot.Feet, 49983, gems, enchants, rune);      // Blood-Soaked Saronite Stompers
        DG(c, CharacterSlot.Finger1, 50414, gems, enchants, rune);   // Might of Blight
        DG(c, CharacterSlot.Finger2, 50453, gems, enchants, rune);   // Ring of Rotting Sinew
        DG(c, CharacterSlot.Trinket1, trinket1, gems, enchants, rune);
        DG(c, CharacterSlot.Trinket2, trinket2, gems, enchants, rune);
        DG(c, CharacterSlot.MainHand, mhId, gems, enchants, rune);
        DG(c, CharacterSlot.Ranged, 50462, gems, enchants, rune);    // Sigil of the Bone Gryphon
    }

    // 277 DPS: 5pc Sanctified Scourgelord's Battlegear (T10 heroic DPS) + heroic ICC offpieces.
    static void GearDPSDK277(Character c, bool gems, bool enchants, int trinket1, int trinket2, int mhId, int rune)
    {
        DG(c, CharacterSlot.Head, 51312, gems, enchants, rune);      // Sanctified Scourgelord Helmet
        DG(c, CharacterSlot.Neck, 50647, gems, enchants, rune);      // Ahn'kahar Onyx Neckguard
        DG(c, CharacterSlot.Shoulders, 51314, gems, enchants, rune); // Sanctified Scourgelord Shoulderplates
        DG(c, CharacterSlot.Back, 50677, gems, enchants, rune);      // Winding Sheet
        DG(c, CharacterSlot.Chest, 51310, gems, enchants, rune);     // Sanctified Scourgelord Battleplate
        DG(c, CharacterSlot.Wrist, 50659, gems, enchants, rune);     // Polar Bear Claw Bracers
        DG(c, CharacterSlot.Hands, 51311, gems, enchants, rune);     // Sanctified Scourgelord Gauntlets
        DG(c, CharacterSlot.Waist, 50620, gems, enchants, rune);     // Coldwraith Links
        DG(c, CharacterSlot.Legs, 51313, gems, enchants, rune);      // Sanctified Scourgelord Legplates
        DG(c, CharacterSlot.Feet, 50639, gems, enchants, rune);      // Blood-Soaked Saronite Stompers
        DG(c, CharacterSlot.Finger1, 50657, gems, enchants, rune);   // Skeleton Lord's Circle
        DG(c, CharacterSlot.Finger2, 50404, gems, enchants, rune);   // Ashen Band of Endless Courage
        DG(c, CharacterSlot.Trinket1, trinket1, gems, enchants, rune);
        DG(c, CharacterSlot.Trinket2, trinket2, gems, enchants, rune);
        DG(c, CharacterSlot.MainHand, mhId, gems, enchants, rune);
        DG(c, CharacterSlot.Ranged, 50462, gems, enchants, rune);    // Sigil of the Bone Gryphon
    }

    // Unholy DPS 0/17/54: heavy Unholy tree so Rotation.GetRotationType picks Type.Unholy.
    static DeathKnightTalents UnholyDPSTalents(bool glyphs)
    {
        DeathKnightTalents t = new DeathKnightTalents();
        // Frost (17)
        t.ImprovedIcyTouch = 3;
        t.RunicPowerMastery = 2;
        t.BlackIce = 5;
        t.IcyTalons = 5;
        t.IcyReach = 2;
        // Unholy (54)
        t.Virulence = 3;
        t.Epidemic = 2;
        t.RavenousDead = 3;
        t.Necrosis = 5;
        t.BloodCakedBlade = 3;
        t.NightOfTheDead = 2;
        t.UnholyBlight = 1;
        t.Impurity = 5;
        t.Dirge = 2;
        t.MasterOfGhouls = 1;
        t.Desecration = 3;
        t.ImprovedUnholyPresence = 2;
        t.GhoulFrenzy = 1;
        t.CryptFever = 3;
        t.EbonPlaguebringer = 3;
        t.WanderingPlague = 3;
        t.RageOfRivendare = 3;
        t.ScourgeStrike = 1;
        t.SummonGargoyle = 1;
        if (glyphs)
        {
            t.GlyphofScourgeStrike = true;
            t.GlyphofDarkDeath = true;
            t.GlyphofIcyTouch = true;
            t.GlyphoftheGhoul = true;
        }
        return t;
    }

    // Frost DPS 0/53/18: heavy Frost tree so Rotation.GetRotationType picks Type.Frost.
    // dw adds Nerves of Cold Steel / Threat of Thassarian for the dual-wield variant.
    static DeathKnightTalents FrostDPSTalents(bool glyphs, bool dw)
    {
        DeathKnightTalents t = new DeathKnightTalents();
        // Frost (53)
        t.ImprovedIcyTouch = 3;
        t.RunicPowerMastery = 2;
        t.BlackIce = 5;
        t.IcyTalons = 5;
        t.NervesOfColdSteel = 3;
        t.Annihilation = 3;
        t.KillingMachine = 5;
        t.ChillOfTheGrave = 2;
        t.EndlessWinter = 2;
        t.IcyReach = 2;
        t.GlacierRot = 3;
        t.ImprovedIcyTalons = 5;
        t.MercilessCombat = 2;
        t.Rime = 3;
        t.BloodOfTheNorth = 3;
        t.FrostStrike = 1;
        t.GuileOfGorefiend = 3;
        t.TundraStalker = 3;
        t.HowlingBlast = 1;
        if (dw) t.ThreatOfThassarian = 3;
        // Unholy (18)
        t.Virulence = 3;
        t.Epidemic = 2;
        t.RavenousDead = 3;
        t.Necrosis = 5;
        t.BloodCakedBlade = 3;
        t.NightOfTheDead = 2;
        if (glyphs)
        {
            t.GlyphofFrostStrike = true;
            t.GlyphofHowlingBlast = true;
            t.GlyphofIcyTouch = true;
            t.GlyphofDisease = true;
        }
        return t;
    }

    // Blood DPS 51/0/20: heavy Blood tree so Rotation.GetRotationType picks Type.Blood.
    static DeathKnightTalents BloodDPSTalents(bool glyphs)
    {
        DeathKnightTalents t = new DeathKnightTalents();
        // Blood (51)
        t.Butchery = 2;
        t.Subversion = 3;
        t.BladedArmor = 5;
        t.TwoHandedWeaponSpecialization = 2;
        t.DarkConviction = 5;
        t.DeathRuneMastery = 3;
        t.BloodyStrikes = 3;
        t.VeteranOfTheThirdWar = 3;
        t.BloodyVengeance = 3;
        t.AbominationsMight = 2;
        t.Hysteria = 1;
        t.ImprovedBloodPresence = 3;
        t.ImprovedDeathStrike = 2;
        t.SuddenDoom = 3;
        t.VampiricBlood = 1;
        t.HeartStrike = 1;
        t.MightOfMograine = 3;
        t.BloodGorged = 5;
        t.DancingRuneWeapon = 1;
        // Unholy (20)
        t.Virulence = 3;
        t.Epidemic = 2;
        t.RavenousDead = 3;
        t.Necrosis = 5;
        t.BloodCakedBlade = 3;
        t.NightOfTheDead = 2;
        t.UnholyBlight = 1;
        t.Dirge = 1;
        if (glyphs)
        {
            t.GlyphofHeartStrike = true;
            t.GlyphofDarkDeath = true;
            t.GlyphofDancingRuneWeapon = true;
            t.GlyphofUnholyBlight = true;
        }
        return t;
    }

    static void DPSDKGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Unholy DPS 0/17/54, raid buffs, 277 T10 DPS 5pc, Glorenzelg + Fallen Crusader,
        //    Deathbringer's Will / Tiny Abomination in a Jar proc trinkets, 300s, target 83.
        {
            Character c = NewDPSDK("DPSDKUnholy277", UnholyDPSTalents(true), CharacterRace.Human, true);
            GearDPSDK277(c, true, true, 50363, 50706, 50730, ENCH_DPSDK_FC); // DBW + Tiny Abomination, Glorenzelg
            CalculationOptionsDPSDK o = new CalculationOptionsDPSDK();
            o.rotation.setRotation(Rawr.DPSDK.Rotation.Type.Unholy);
            FinishDPSDK(c, o, Path.Combine(outDir, "dpsdk_unholy_277.xml"));
        }

        // 2. Frost DPS 0/53/18, raid buffs, 264 T10 DPS 5pc, Shadow's Edge + Razorice,
        //    Whispering Fanged Skull (crit proc) / Mark of Supremacy (Use AP), 300s, target 83.
        {
            Character c = NewDPSDK("DPSDKFrost264", FrostDPSTalents(true, false), CharacterRace.Orc, true);
            GearDPSDK264(c, true, true, 50343, 47734, 49888, ENCH_DPSDK_RAZORICE); // WFS + MoS, Shadow's Edge
            CalculationOptionsDPSDK o = new CalculationOptionsDPSDK();
            o.rotation.setRotation(Rawr.DPSDK.Rotation.Type.Frost);
            FinishDPSDK(c, o, Path.Combine(outDir, "dpsdk_frost_264.xml"));
        }

        // 3. Blood DPS 51/0/20, raid buffs, 277 T10 DPS 5pc, Oxheart + Fallen Crusader,
        //    Tiny Abomination in a Jar / Mark of Supremacy, 300s, target 83.
        {
            Character c = NewDPSDK("DPSDKBlood277", BloodDPSTalents(true), CharacterRace.Dwarf, true);
            GearDPSDK277(c, true, true, 50706, 47734, 51833, ENCH_DPSDK_FC); // Tiny Abom + MoS, Oxheart
            CalculationOptionsDPSDK o = new CalculationOptionsDPSDK();
            o.rotation.setRotation(Rawr.DPSDK.Rotation.Type.Blood);
            FinishDPSDK(c, o, Path.Combine(outDir, "dpsdk_blood_277.xml"));
        }

        // 4. Unholy DPS, NO raid buffs, 264 T10 DPS 5pc, Bryntroll + Fallen Crusader,
        //    60s fight (short), Mark of Supremacy Use trinket + Whispering Fanged Skull, target 83.
        {
            Character c = NewDPSDK("DPSDKUnholyNoBuffs60", UnholyDPSTalents(true), CharacterRace.Tauren, false);
            GearDPSDK264(c, true, true, 47734, 50343, 50415, ENCH_DPSDK_FC); // MoS + WFS, Bryntroll
            CalculationOptionsDPSDK o = new CalculationOptionsDPSDK();
            o.rotation.setRotation(Rawr.DPSDK.Rotation.Type.Unholy);
            o.FightLength = 1;
            FinishDPSDK(c, o, Path.Combine(outDir, "dpsdk_unholy_nobuffs_60.xml"));
        }

        // 5. Frost DPS dual-wield 0/53/18 with Nerves of Cold Steel / Threat of Thassarian,
        //    raid buffs, 277 T10 DPS 5pc, Scourgeborne Waraxe + Bloodvenom Blade with Fallen
        //    Crusader on BOTH hands (exercises the duplicate-rune removal), 600s, target 83.
        {
            Character c = NewDPSDK("DPSDKFrostDW277", FrostDPSTalents(true, true), CharacterRace.NightElf, true);
            GearDPSDK277(c, true, true, 50363, 50706, 50654, ENCH_DPSDK_FC); // DBW + Tiny Abom, MH Waraxe
            DG(c, CharacterSlot.OffHand, 50672, true, true, ENCH_DPSDK_FC);   // OH Bloodvenom Blade, also FC
            CalculationOptionsDPSDK o = new CalculationOptionsDPSDK();
            o.rotation.setRotation(Rawr.DPSDK.Rotation.Type.Frost);
            o.FightLength = 10;
            FinishDPSDK(c, o, Path.Combine(outDir, "dpsdk_frost_dw_277.xml"));
        }

        // 6. Blood DPS, raid buffs, 264 T10 DPS 5pc, Ramaladni's Blade + Fallen Crusader,
        //    Deathbringer's Will (normal) + Eitrigg's Oath Use trinket, 600s fight, target 80.
        {
            Character c = NewDPSDK("DPSDKBlood264Target80", BloodDPSTalents(true), CharacterRace.Orc, true);
            GearDPSDK264(c, true, true, 50362, 48021, 51905, ENCH_DPSDK_FC); // DBW + Eitrigg's Oath, Ramaladni
            CalculationOptionsDPSDK o = new CalculationOptionsDPSDK();
            o.rotation.setRotation(Rawr.DPSDK.Rotation.Type.Blood);
            o.FightLength = 10;
            o.TargetLevel = 80;
            FinishDPSDK(c, o, Path.Combine(outDir, "dpsdk_blood_264_80.xml"));
        }
    }

    // ==================================================================
    // DPSWarr fixture generation (Phase 6 oracle)
    // ==================================================================

    // dpswarr gems (from CalculationsDPSWarr.DefaultGemmingTemplates, epic tier)
    const int GEM_DPSWARR_RED = 40111;         // Bold Cardinal Ruby +20 Str
    const int GEM_DPSWARR_YELLOW = 40142;      // Inscribed Ametrine +10 Str +10 Crit
    const int GEM_DPSWARR_YELLOW_HIT = 40146;  // Etched Ametrine +10 Str +10 Hit
    const int GEM_DPSWARR_BLUE = 40129;        // Sovereign Dreadstone +10 Str +15 Sta
    const int GEM_DPSWARR_META = 41285;        // Chaotic Skyflare Diamond
    const int GEM_DPSWARR_PRISMATIC = 49110;   // Nightmare Tear

    // dpswarr weapon enchants
    const int ENCH_DPSWARR_MASSACRE = 3827;    // Massacre (+110 AP)
    const int ENCH_DPSWARR_SAVAGERY = 3828;    // Greater Savagery

    static void DPSWarrMain(string outDir)
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Calculations.LoadModel(typeof(CalculationsDPSWarr));
        Console.Error.WriteLine("ItemCache=" + ItemCache.Items.Count
            + " Buffs=" + Buff.AllBuffs.Count
            + " Enchants=" + Enchant.AllEnchants.Count);
        DPSWarrGenerate(outDir);
    }

    static void ListDPSWarr()
    {
        SavedTalentSpec.Load(new StreamReader(DataFile("Talents.Default.xml")));
        Console.WriteLine("== WARRIOR TALENT SPECS (DefaultDataFiles/Talents.xml) ==");
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Warrior))
            Console.WriteLine(string.Format("{0} ({1}/{2}/{3}) specLen={4}", spec.Name, spec.Tree1, spec.Tree2, spec.Tree3,
                spec.Spec == null ? -1 : spec.Spec.Length));
        Console.WriteLine();
        PrintTalents("Arms-Sword-WM (default)", DPSWarrSpec("Arms-Sword-WM").TalentSpec());
        PrintTalents("Arms-Mace-WM (default)", DPSWarrSpec("Arms-Mace-WM").TalentSpec());
        PrintTalents("Arms-Axe-Exe (default)", DPSWarrSpec("Arms-Axe-Exe").TalentSpec());
        PrintTalents("Fury (default)", DPSWarrSpec("Fury").TalentSpec());

        Console.WriteLine();
        Console.WriteLine("== DPSWARR ENCHANT SPOT CHECK ==");
        int[] enchIds = new int[] { 3827, 3828, 3830, 3820, 3810, 3728, 3832, 3845, 1603, 3823, 1597, 3817, 3808, 1099, 1951, 3253, 3232, 3822, 3850 };
        foreach (Enchant e in Enchant.AllEnchants.Where(x => enchIds.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | slot={2} | {3}", e.Id, e.Name, e.Slot, e.Stats == null ? "" : StatLine(e.Stats)));

        Console.WriteLine();
        Console.WriteLine("== DPSWARR GEM SPOT CHECK ==");
        int[] gids = new int[] { 40111, 40112, 40115, 40116, 40118, 40119, 40126, 40129, 40142, 40146, 40147, 41285, 41398, 49110 };
        foreach (Item g in ItemCache.Items.Values.Where(x => gids.Contains(x.Id)).OrderBy(x => x.Id))
            Console.WriteLine(string.Format("{0} | {1} | {2} | {3}", g.Id, g.Name, g.SocketColor1, StatLine(g.Stats)));

        Console.WriteLine();
        Console.WriteLine("== ALL YMIRJAR LORD'S ITEMS ==");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Name.Contains("Ymirjar"))
            .OrderBy(i => i.ItemLevel).ThenBy(i => i.Name))
            Console.WriteLine(string.Format(CultureInfo.InvariantCulture, "{0} | {1} | ilvl {2} | {3} | set={4} | str={5:F0} arp={6:F0}",
                item.Id, item.Name, item.ItemLevel, item.Type, item.SetName, item.Stats.Strength, item.Stats.ArmorPenetrationRating));

        Console.WriteLine();
        Console.WriteLine("== PLATE DPS ITEMS (epic, ilvl>=264, str, no tank ratings) ==");
        foreach (ItemSlot slot in new ItemSlot[] { ItemSlot.Head, ItemSlot.Shoulders, ItemSlot.Chest, ItemSlot.Wrist,
            ItemSlot.Hands, ItemSlot.Waist, ItemSlot.Legs, ItemSlot.Feet, ItemSlot.Neck, ItemSlot.Back, ItemSlot.Finger })
        {
            Console.WriteLine("-- " + slot + " --");
            foreach (Item item in ItemCache.Items.Values
                .Where(i => i.Slot == slot && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264
                    && (i.Type == ItemType.Plate || slot == ItemSlot.Neck || slot == ItemSlot.Back || slot == ItemSlot.Finger)
                    && i.Stats.Strength > 0 && i.Stats.DodgeRating == 0 && i.Stats.ParryRating == 0 && i.Stats.DefenseRating == 0)
                .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name))
                Console.WriteLine(string.Format(CultureInfo.InvariantCulture, "{0} | {1} | ilvl {2} | {3} | set={4} | str={5:F0} arp={6:F0} crit={7:F0} hit={8:F0} exp={9:F0} haste={10:F0}",
                    item.Id, item.Name, item.ItemLevel, item.Type, item.SetName,
                    item.Stats.Strength, item.Stats.ArmorPenetrationRating, item.Stats.CritRating,
                    item.Stats.HitRating, item.Stats.ExpertiseRating, item.Stats.HasteRating));
        }
        Console.WriteLine("-- TwoHand weapons (epic, ilvl>=264) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.TwoHand && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name))
            Console.WriteLine(string.Format(CultureInfo.InvariantCulture, "{0} | {1} | ilvl {2} | {3} | str={4:F0} arp={5:F0} crit={6:F0} hit={7:F0}",
                item.Id, item.Name, item.ItemLevel, item.Type, item.Stats.Strength, item.Stats.ArmorPenetrationRating,
                item.Stats.CritRating, item.Stats.HitRating));
        Console.WriteLine("-- OneHand weapons (epic, ilvl>=264) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => (i.Slot == ItemSlot.MainHand || i.Slot == ItemSlot.OneHand) && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name))
            Console.WriteLine(string.Format(CultureInfo.InvariantCulture, "{0} | {1} | ilvl {2} | {3} | str={4:F0} arp={5:F0} crit={6:F0} hit={7:F0}",
                item.Id, item.Name, item.ItemLevel, item.Type, item.Stats.Strength, item.Stats.ArmorPenetrationRating,
                item.Stats.CritRating, item.Stats.HitRating));
        Console.WriteLine("-- Trinket (epic, ilvl>=264) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Trinket && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name))
            Console.WriteLine(string.Format(CultureInfo.InvariantCulture, "{0} | {1} | ilvl {2} | fx={3} | str={4:F0} arp={5:F0} crit={6:F0} hit={7:F0} ap={8:F0}",
                item.Id, item.Name, item.ItemLevel, item.Stats.SpecialEffects().Count(), item.Stats.Strength,
                item.Stats.ArmorPenetrationRating, item.Stats.CritRating, item.Stats.HitRating, item.Stats.AttackPower));
        Console.WriteLine("-- Ranged (epic, ilvl>=264) --");
        foreach (Item item in ItemCache.Items.Values
            .Where(i => i.Slot == ItemSlot.Ranged && i.Quality == ItemQuality.Epic && i.ItemLevel >= 264)
            .OrderByDescending(i => i.ItemLevel).ThenBy(i => i.Name))
            Console.WriteLine(ItemLine(item));

        Console.WriteLine();
        Console.WriteLine("== WARRIOR DPS SET BONUSES ==");
        foreach (Buff b in Buff.AllBuffs.Where(x => x.SetName != null && (x.SetName.Contains("Ymirjar") || x.SetName.Contains("Wrynn") || x.SetName.Contains("Dread")))
            .OrderBy(x => x.SetName).ThenBy(x => x.Name))
            Console.WriteLine(b.SetName + " | " + b.Name + " | threshold=" + b.SetThreshold + " | " + StatLine(b.Stats));
    }

    static Character NewDPSWarr(string name, WarriorTalents talents, CharacterRace race, bool raidBuffs)
    {
        Character c = new Character();
        c.Name = name;
        c.Region = CharacterRegion.US;
        c.Realm = "Test";
        c.Race = race;
        c.Class = CharacterClass.Warrior;
        c.CurrentModel = "DPSWarr";
        c.WarriorTalents = talents;
        if (raidBuffs) DPSWarrRaidBuffs(c);
        return c;
    }

    // Physical DPS buffs/debuffs a warrior would raid with.
    static void DPSWarrRaidBuffs(Character c)
    {
        string[] buffs = new string[] {
            "Blessing of Kings", "Blessing of Might", "Strength of Earth Totem", "Windfury Totem",
            "Horn of Winter", "Abomination's Might", "Trueshot Aura", "Leader of the Pack",
            "Rampage", "Battle Shout", "Sanctified Retribution", "Sunder Armor", "Faerie Fire",
            "Mangle", "Blood Frenzy", "Trauma", "Savage Combat", "Improved Faerie Fire",
            "Heart of the Crusader", "Heroism/Bloodlust", "Flask of Endless Rage",
            "Attack Power Food", "Mark of the Wild", "Power Word: Fortitude", "Shadow Protection",
        };
        foreach (string b in buffs) c.ActiveBuffsAdd(b);
    }

    static void FinishDPSWarr(Character c, CalculationOptionsDPSWarr options, string path)
    {
        c.BossOptions = new BossOptions();
        c.RecalculateSetBonuses();
        c.CalculationOptions = options;
        c.Save(path);
        Console.Error.WriteLine("saved " + Path.GetFileName(path) + " (" + new FileInfo(path).Length + " bytes)");
    }

    static int GemForDPSWarr(ItemSlot color)
    {
        switch (color)
        {
            case ItemSlot.Red: return GEM_DPSWARR_RED;
            case ItemSlot.Yellow: return GEM_DPSWARR_YELLOW;
            case ItemSlot.Blue: return GEM_DPSWARR_BLUE;
            case ItemSlot.Meta: return GEM_DPSWARR_META;
            case ItemSlot.Prismatic: return GEM_DPSWARR_PRISMATIC;
            default: return 0;
        }
    }

    static int EnchForSlotDPSWarr(CharacterSlot slot, int weaponEnchant)
    {
        switch (slot)
        {
            case CharacterSlot.Head: return ENCH_CAT_HEAD;
            case CharacterSlot.Shoulders: return ENCH_CAT_SHOULDER;
            case CharacterSlot.Back: return ENCH_CAT_BACK;
            case CharacterSlot.Chest: return ENCH_CAT_CHEST;
            case CharacterSlot.Wrist: return ENCH_CAT_WRIST;
            case CharacterSlot.Hands: return ENCH_CAT_HANDS;
            case CharacterSlot.Legs: return ENCH_CAT_LEGS;
            case CharacterSlot.Feet: return ENCH_CAT_FEET;
            case CharacterSlot.MainHand: return weaponEnchant;
            case CharacterSlot.OffHand: return weaponEnchant;
            default: return 0;
        }
    }

    static void DW(Character c, CharacterSlot slot, int itemId, bool gems, bool enchants, int weaponEnchant)
    {
        int g1 = 0, g2 = 0, g3 = 0, ench = 0;
        if (itemId != 0)
        {
            Item item = RequireItem(itemId);
            if (gems)
            {
                g1 = GemForDPSWarr(item.SocketColor1);
                g2 = GemForDPSWarr(item.SocketColor2);
                g3 = GemForDPSWarr(item.SocketColor3);
            }
            if (enchants) ench = EnchForSlotDPSWarr(slot, weaponEnchant);
        }
        c[slot] = itemId == 0 ? null : new ItemInstance(itemId, g1, g2, g3, ench);
    }

    // 264 DPS: 5pc Ymirjar Lord's Battlegear (T10 normal DPS) + ICC 10/25 offpieces.
    static void GearDPSWarr264(Character c, bool gems, bool enchants, int trinket1, int trinket2, int mhId, int weaponEnchant)
    {
        DW(c, CharacterSlot.Head, 51212, gems, enchants, weaponEnchant);      // Sanctified Ymirjar Lord's Helmet
        DW(c, CharacterSlot.Neck, 50180, gems, enchants, weaponEnchant);      // Lana'thel's Chain of Flagellation
        DW(c, CharacterSlot.Shoulders, 51210, gems, enchants, weaponEnchant); // Sanctified Ymirjar Lord's Shoulderplates
        DW(c, CharacterSlot.Back, 50467, gems, enchants, weaponEnchant);      // Might of the Ocean Serpent
        DW(c, CharacterSlot.Chest, 51214, gems, enchants, weaponEnchant);     // Sanctified Ymirjar Lord's Battleplate
        DW(c, CharacterSlot.Wrist, 50002, gems, enchants, weaponEnchant);     // Polar Bear Claw Bracers
        DW(c, CharacterSlot.Hands, 51213, gems, enchants, weaponEnchant);     // Sanctified Ymirjar Lord's Gauntlets
        DW(c, CharacterSlot.Waist, 50987, gems, enchants, weaponEnchant);     // Malevolent Girdle
        DW(c, CharacterSlot.Legs, 51211, gems, enchants, weaponEnchant);      // Sanctified Ymirjar Lord's Legplates
        DW(c, CharacterSlot.Feet, 49983, gems, enchants, weaponEnchant);      // Blood-Soaked Saronite Stompers
        DW(c, CharacterSlot.Finger1, 50414, gems, enchants, weaponEnchant);   // Might of Blight
        DW(c, CharacterSlot.Finger2, 50453, gems, enchants, weaponEnchant);   // Ring of Rotting Sinew
        DW(c, CharacterSlot.Trinket1, trinket1, gems, enchants, weaponEnchant);
        DW(c, CharacterSlot.Trinket2, trinket2, gems, enchants, weaponEnchant);
        DW(c, CharacterSlot.MainHand, mhId, gems, enchants, weaponEnchant);
        DW(c, CharacterSlot.Ranged, 50462, gems, enchants, weaponEnchant);    // Sigil of the Bone Gryphon
    }

    // 277 DPS: 5pc Sanctified Ymirjar Lord's Battlegear (T10 heroic DPS) + heroic ICC offpieces.
    static void GearDPSWarr277(Character c, bool gems, bool enchants, int trinket1, int trinket2, int mhId, int weaponEnchant)
    {
        DW(c, CharacterSlot.Head, 51227, gems, enchants, weaponEnchant);      // Sanctified Ymirjar Lord's Helmet
        DW(c, CharacterSlot.Neck, 50647, gems, enchants, weaponEnchant);      // Ahn'kahar Onyx Neckguard
        DW(c, CharacterSlot.Shoulders, 51229, gems, enchants, weaponEnchant); // Sanctified Ymirjar Lord's Shoulderplates
        DW(c, CharacterSlot.Back, 50677, gems, enchants, weaponEnchant);      // Winding Sheet
        DW(c, CharacterSlot.Chest, 51225, gems, enchants, weaponEnchant);     // Sanctified Ymirjar Lord's Battleplate
        DW(c, CharacterSlot.Wrist, 50659, gems, enchants, weaponEnchant);     // Polar Bear Claw Bracers
        DW(c, CharacterSlot.Hands, 51226, gems, enchants, weaponEnchant);     // Sanctified Ymirjar Lord's Gauntlets
        DW(c, CharacterSlot.Waist, 50620, gems, enchants, weaponEnchant);     // Coldwraith Links
        DW(c, CharacterSlot.Legs, 51228, gems, enchants, weaponEnchant);      // Sanctified Ymirjar Lord's Legplates
        DW(c, CharacterSlot.Feet, 50639, gems, enchants, weaponEnchant);      // Blood-Soaked Saronite Stompers
        DW(c, CharacterSlot.Finger1, 50657, gems, enchants, weaponEnchant);   // Skeleton Lord's Circle
        DW(c, CharacterSlot.Finger2, 50404, gems, enchants, weaponEnchant);   // Ashen Band of Endless Courage
        DW(c, CharacterSlot.Trinket1, trinket1, gems, enchants, weaponEnchant);
        DW(c, CharacterSlot.Trinket2, trinket2, gems, enchants, weaponEnchant);
        DW(c, CharacterSlot.MainHand, mhId, gems, enchants, weaponEnchant);
        DW(c, CharacterSlot.Ranged, 50462, gems, enchants, weaponEnchant);    // Sigil of the Bone Gryphon
    }

    static SavedTalentSpec DPSWarrSpec(string name)
    {
        foreach (SavedTalentSpec spec in SavedTalentSpec.SpecsFor(CharacterClass.Warrior))
            if (spec.Name == name) return spec;
        throw new InvalidOperationException("warrior spec not found: " + name);
    }

    static WarriorTalents DPSWarrDefaultSpec(string name)
    {
        TalentsBase t = DPSWarrSpec(name).TalentSpec();
        if (t == null) throw new InvalidOperationException(name + " spec failed to parse");
        return (WarriorTalents)t;
    }

    static void DPSWarrGenerate(string outDir)
    {
        Directory.CreateDirectory(outDir);

        // 1. Arms 56/15/0 sword spec, Human, raid buffs, 277 T10 DPS 5pc, Glorenzelg (284 2H
        //    sword) + Berserking, Deathbringer's Will / Tiny Abomination in a Jar, 300s, target 83.
        {
            Character c = NewDPSWarr("DPSWarrArms277", DPSWarrDefaultSpec("Arms-Sword-WM"), CharacterRace.Human, true);
            GearDPSWarr277(c, true, true, 50363, 50706, 50730, ENCH_DPSWARR_SAVAGERY);
            CalculationOptionsDPSWarr o = new CalculationOptionsDPSWarr();
            FinishDPSWarr(c, o, Path.Combine(outDir, "dpswarr_arms_277.xml"));
        }

        // 2. Fury 18/53/0 Titan's Grip, Orc, raid buffs, 277 T10 DPS 5pc, Bryntroll + Glorenzelg
        //    (two two-handers, both Massacre), Deathbringer's Will / Sharpened Twilight Scale,
        //    300s, target 83.
        {
            Character c = NewDPSWarr("DPSWarrFury277", DPSWarrDefaultSpec("Fury"), CharacterRace.Orc, true);
            GearDPSWarr277(c, true, true, 50363, 54590, 50709, ENCH_DPSWARR_MASSACRE);
            DW(c, CharacterSlot.OffHand, 50730, true, true, ENCH_DPSWARR_MASSACRE);
            CalculationOptionsDPSWarr o = new CalculationOptionsDPSWarr();
            FinishDPSWarr(c, o, Path.Combine(outDir, "dpswarr_fury_277.xml"));
        }

        // 3. Fury 18/52/0 one-hand dual wield (no Titan's Grip), Night Elf, NO raid buffs,
        //    264 T10 DPS 5pc, Troggbane + Mithrios both Massacre, Mark of Supremacy (Use) /
        //    Eitrigg's Oath (Use), 60s short fight, target 83.
        {
            Character c = NewDPSWarr("DPSWarrFuryDW264NoBuffs", DPSWarrDefaultSpec("Fury"), CharacterRace.NightElf, false);
            c.WarriorTalents.TitansGrip = 0;
            GearDPSWarr264(c, true, true, 47734, 48021, 51947, ENCH_DPSWARR_MASSACRE);
            DW(c, CharacterSlot.OffHand, 50738, true, true, ENCH_DPSWARR_MASSACRE);
            CalculationOptionsDPSWarr o = new CalculationOptionsDPSWarr();
            o.Duration = 60f;
            FinishDPSWarr(c, o, Path.Combine(outDir, "dpswarr_fury_dw_264_nobuffs_60.xml"));
        }

        // 4. Arms 56/15/0 with mace specialization, Dwarf, raid buffs, 264 T10 DPS 5pc,
        //    Oxheart + Massacre, Deathbringer's Will (normal) / Whispering Fanged Skull,
        //    300s, Rend/Overpower/Shattering Throw maintenance disabled.
        {
            Character c = NewDPSWarr("DPSWarrArms264", DPSWarrDefaultSpec("Arms-Mace-WM"), CharacterRace.Dwarf, true);
            GearDPSWarr264(c, true, true, 50362, 50343, 51833, ENCH_DPSWARR_MASSACRE);
            CalculationOptionsDPSWarr o = new CalculationOptionsDPSWarr();
            o.M_Rend = false;
            o.M_Overpower = false;
            o.M_ShatteringThrow = false;
            FinishDPSWarr(c, o, Path.Combine(outDir, "dpswarr_arms_264.xml"));
        }

        // 5. Arms 56/15/0 sword spec, Troll, raid buffs, 277 T10 DPS 5pc, Glorenzelg (284) +
        //    Executioner, Sharpened Twilight Scale (284 proc) / Deathbringer's Will (277 proc),
        //    600s long fight, Bladestorm maintained, target 83.
        {
            Character c = NewDPSWarr("DPSWarrArmsProcs600", DPSWarrDefaultSpec("Arms-Axe-Exe"), CharacterRace.Troll, true);
            GearDPSWarr277(c, true, true, 54590, 50363, 50709, ENCH_DPSWARR_MASSACRE);
            CalculationOptionsDPSWarr o = new CalculationOptionsDPSWarr();
            o.Duration = 600f;
            o.M_Bladestorm = true;
            FinishDPSWarr(c, o, Path.Combine(outDir, "dpswarr_arms_procs_600.xml"));
        }

        // 6. Fury 18/53/0 Titan's Grip, Tauren, NO raid buffs, 264 T10 DPS 5pc, Bryntroll +
        //    Ramaladni's Blade of Culling (two two-handers, Massacre), Mark of Supremacy (Use) /
        //    Corpse Tongue Coin, 60s short fight, target 80, no Heroic Strike rage dump.
        {
            Character c = NewDPSWarr("DPSWarrFuryShort80", DPSWarrDefaultSpec("Fury"), CharacterRace.Tauren, false);
            GearDPSWarr264(c, true, true, 47734, 50352, 50709, ENCH_DPSWARR_MASSACRE);
            DW(c, CharacterSlot.OffHand, 51905, true, true, ENCH_DPSWARR_MASSACRE);
            CalculationOptionsDPSWarr o = new CalculationOptionsDPSWarr();
            o.Duration = 60f;
            o.TargetLevel = 80;
            o.TargetArmor = StatConversion.NPC_ARMOR[0];
            o.M_HeroicStrike = false;
            FinishDPSWarr(c, o, Path.Combine(outDir, "dpswarr_fury_short_264_80.xml"));
        }

        // 7. Same character as fixture 2 with UseMarkov=true: exercises the state-space diagnostic
        //    branch (FuryGenerator/BloodSurgeMatrix/MarkovProcess). The branch discards its result,
        //    so the golden ratings equal fixture 2's; the dump proves the branch runs without
        //    changing any returned value (DECISIONS #58).
        {
            Character c = NewDPSWarr("DPSWarrFuryMarkov277", DPSWarrDefaultSpec("Fury"), CharacterRace.Orc, true);
            GearDPSWarr277(c, true, true, 50363, 54590, 50709, ENCH_DPSWARR_MASSACRE);
            DW(c, CharacterSlot.OffHand, 50730, true, true, ENCH_DPSWARR_MASSACRE);
            CalculationOptionsDPSWarr o = new CalculationOptionsDPSWarr();
            o.UseMarkov = true;
            FinishDPSWarr(c, o, Path.Combine(outDir, "dpswarr_fury_markov_277.xml"));
        }
    }
}
