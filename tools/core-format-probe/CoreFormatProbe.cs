// Oracle probe for the ported core ToString/XmlSerializer behavior (prebuilt legacy
// Rawr.Base.dll, run under Mono). It emits:
//   - expected.txt: the exact Stats/SpecialEffect/Buff/Enchant ToString() output for
//     jrawr/rawr-core/src/test/resources/core-format/vectors.txt
//   - buff-cache.xml: XmlSerializer(typeof(BuffList)) of a constructed BuffList
//   - item-filter.xml: XmlSerializer(typeof(ItemFilterData)) of a constructed ItemFilterData
//   - item-source.xml: XmlSerializer(typeof(ItemLocationDictionary)) of a constructed dictionary
//   - boss-info.txt: BossHandler.GenInfoString() and GenInfoString(Character) for a constructed
//     boss and a stub current model returning fixed stats (escaped \r\n)
// The Java tests replay the same vectors and rebuild the same object graphs with the ported
// writers; the legacy tree is only read, never built or modified.
//
// Build/run: tools/core-format-probe/run.sh
using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Reflection;
using System.Text;
using System.Xml;
using System.Xml.Serialization;
using Rawr;

public static class CoreFormatProbe
{
    public static int Main(string[] args)
    {
        if (args.Length != 6)
        {
            Console.Error.WriteLine("usage: CoreFormatProbe <vectors.txt> <expected.txt> <buff.xml> <filter.xml> <source.xml> <boss-info.txt>");
            return 2;
        }
        CultureInfo.CurrentCulture = CultureInfo.InvariantCulture;
        CultureInfo.CurrentUICulture = CultureInfo.InvariantCulture;

        StringBuilder sb = new StringBuilder();
        string[] lines = File.ReadAllLines(args[0]);
        int index = 0;
        foreach (string raw in lines)
        {
            string text = raw.Trim();
            if (text.Length == 0 || text.StartsWith("#")) continue;
            string[] parts = text.Split('|');
            sb.Append("== ").Append(index).Append(' ').Append(text).Append('\n');
            try
            {
                switch (parts[0])
                {
                    case "S":
                        sb.Append(MakeStats(parts[1]).ToString()).Append('\n');
                        break;
                    case "E":
                        sb.Append(MakeEffect(parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]).ToString()).Append('\n');
                        break;
                    case "B":
                        sb.Append(MakeBuff(parts[1], parts.Length > 2 ? parts[2] : "").ToString()).Append('\n');
                        break;
                    case "N":
                        sb.Append(new Enchant(1234, parts[1], ItemSlot.Head, MakeStats(parts.Length > 2 ? parts[2] : "")).ToString()).Append('\n');
                        break;
                    case "SE":
                        {
                            Stats stats = MakeStats(parts[1]);
                            stats.AddSpecialEffect(MakeEffect(parts[2], parts[3], parts[4], parts[5], parts[6], parts[7]));
                            sb.Append(stats.ToString()).Append('\n');
                        }
                        break;
                    default:
                        Console.Error.WriteLine("unknown vector kind: " + parts[0]);
                        return 2;
                }
            }
            catch (Exception e)
            {
                sb.Append("X throw ").Append(e.GetType().Name).Append('\n');
            }
            index++;
        }
        File.WriteAllText(args[1], sb.ToString());

        WriteXml(args[2], typeof(BuffList), BuildBuffList());
        WriteXml(args[3], typeof(ItemFilterData), BuildItemFilterData());
        WriteXml(args[4], typeof(ItemLocationDictionary), BuildLocationDictionary());
        File.WriteAllText(args[5], BuildBossInfo());

        Console.WriteLine("wrote " + index + " vectors to " + args[1]);
        return 0;
    }

    // Same construction order as BossHandlerGenInfoTest on the Java side. Escapes \r/\n so the dump
    // is line-oriented.
    private static string BuildBossInfo()
    {
        BossHandler boss = new BossHandler();
        boss.Name = "Probe Boss";
        boss.Content = BossHandler.TierLevels.T10_9;
        boss.Instance = "Probe Instance";
        boss.Version = BossHandler.Versions.V_25H;
        boss.Comment = "Probe Comment";
        boss.Level = 83;
        boss.Armor = 10643;
        boss.Health = 10000000f;
        boss.BerserkTimer = 600;
        boss.SpeedKillTimer = 300;
        boss.TimeBossIsInvuln = 10f;
        boss.Max_Players = 25;
        boss.Min_Tanks = 2;
        boss.Min_Healers = 5;

        Attack melee = new Attack();
        melee.Name = "Probe Melee";
        melee.IsTheDefaultMelee = true;
        melee.DamagePerHit = 20000f;
        melee.AttackSpeed = 2f;
        melee.MaxNumTargets = 1f;
        melee.AttackType = ATTACK_TYPES.AT_MELEE;
        boss.Attacks.Add(melee);

        // Calculations.Models normally scans <AppDomain.BaseDirectory>/Data for model DLLs; the probe
        // has no Data directory and only needs its own stub, so seed the three private statics
        // (names verified against the prebuilt Rawr.Base.dll with monodis) and register normally.
        typeof(Calculations).GetField("_models", BindingFlags.NonPublic | BindingFlags.Static)
            .SetValue(null, new Dictionary<string, Type>());
        typeof(Calculations).GetField("_modelIcons", BindingFlags.NonPublic | BindingFlags.Static)
            .SetValue(null, new Dictionary<string, string>());
        typeof(Calculations).GetField("_modelClasses", BindingFlags.NonPublic | BindingFlags.Static)
            .SetValue(null, new Dictionary<string, CharacterClass>());
        Calculations.RegisterModel(typeof(ProbeStubCalculations));

        Character character = new Character();
        character.CurrentModel = "ProbeModel";

        StringBuilder sb = new StringBuilder();
        sb.Append("info=").Append(Escape(boss.GenInfoString())).Append('\n');
        sb.Append("charinfo=").Append(Escape(boss.GenInfoString(character))).Append('\n');
        return sb.ToString();
    }

    private static string Escape(string text)
    {
        return text.Replace("\\", "\\\\").Replace("\r", "\\r").Replace("\n", "\\n");
    }

    private static float Number(string text)
    {
        if (text == "INF") return float.PositiveInfinity;
        if (text == "-INF") return float.NegativeInfinity;
        if (text == "NaN") return float.NaN;
        return float.Parse(text, CultureInfo.InvariantCulture);
    }

    private static Stats MakeStats(string spec)
    {
        Stats stats = new Stats();
        if (string.IsNullOrEmpty(spec)) return stats;
        foreach (string pair in spec.Split(','))
        {
            string[] kv = pair.Split('=');
            PropertyInfo info = typeof(Stats).GetProperty(kv[0]);
            if (info == null) throw new ArgumentException("unknown stat " + kv[0]);
            info.SetValue(stats, Number(kv[1]), null);
        }
        return stats;
    }

    private static SpecialEffect MakeEffect(string trigger, string stats, string duration, string cooldown, string chance, string maxStack)
    {
        return new SpecialEffect(
            (Trigger)Enum.Parse(typeof(Trigger), trigger),
            MakeStats(stats),
            Number(duration), Number(cooldown), Number(chance), int.Parse(maxStack));
    }

    private static Buff MakeBuff(string name, string stats)
    {
        Buff buff = new Buff();
        buff.Name = name;
        buff.Stats = MakeStats(stats);
        return buff;
    }

    private static BuffList BuildBuffList()
    {
        BuffList list = new BuffList();
        Buff buff = new Buff();
        buff.Name = "Probe Buff";
        buff.Group = "Probe Group";
        buff.Source = "Probe Source";
        buff.SetName = "Probe Set";
        buff.SetThreshold = 2;
        buff.Stats = MakeStats("Agility=10,SpellPower=50,BonusDamageMultiplier=0.03");
        buff.Stats.AddSpecialEffect(MakeEffect("DamageSpellHit", "HasteRating=250", "10", "35", "0.35", "1"));
        Buff improvement = MakeBuff("Probe Improvement", "Intellect=5");
        improvement.AllowedClasses = new List<CharacterClass> { CharacterClass.Mage };
        improvement.Professions = new List<Profession> { Profession.Alchemy };
        improvement.ConflictingBuffs = new List<string> { "Probe Group", "Other" };
        improvement.IsTargetDebuff = true;
        improvement.IsCustom = true;
        buff.Improvements.Add(improvement);
        list.Add(buff);

        Buff sparse = new Buff();
        sparse.Name = "Sparse";
        list.Add(sparse);
        return list;
    }

    private static ItemFilterData BuildItemFilterData()
    {
        ItemFilterData data = new ItemFilterData();
        data.RelevantItemTypes["ProbeModel"] = new ItemTypeList(new ItemType[] { ItemType.None, ItemType.Cloth, ItemType.Staff });
        ItemFilterRegex regex = new ItemFilterRegex();
        regex.Name = "Probe Filter";
        regex.Pattern = "Ulduar";
        regex.MinItemLevel = 200;
        regex.MaxItemLevel = 300;
        regex.MinItemQuality = ItemQuality.Temp;
        regex.MaxItemQuality = ItemQuality.Heirloom;
        regex.BoP = false;
        regex.AppliesToGems = false;
        ItemFilterRegex sub = new ItemFilterRegex();
        sub.Name = "Probe Sub";
        sub.Pattern = "Hard Mode";
        sub.Enabled = false;
        sub.AdditiveFilter = false;
        sub.OtherRegexEnabled = false;
        regex.RegexList.Add(sub);
        data.RegexList.Add(regex);
        ItemFilterRegex second = new ItemFilterRegex();
        second.Name = "Second";
        data.RegexList.Add(second);
        data.OtherRegexEnabled = false;
        return data;
    }

    private static ItemLocationDictionary BuildLocationDictionary()
    {
        ItemLocationDictionary dict = new ItemLocationDictionary();
        VendorItem vendor = new VendorItem();
        vendor.TokenMap["Probe Token"] = 10;
        vendor.Cost = 12345;
        vendor.VendorName = "Probe Vendor";
        vendor.VendorArea = "Dalaran";
        vendor.Note = "Probe Note";
        dict["1"] = new ItemLocation[] { vendor, null };

        StaticDrop drop = new StaticDrop();
        drop.Area = "Ulduar (25)";
        drop.Heroic = true;
        drop.Boss = "Auriaya";
        WorldDrop world = new WorldDrop();
        world.Location = "Wintergrasp";
        dict["2"] = new ItemLocation[] { drop, world };

        CraftedItem crafted = (CraftedItem)CraftedItem.Construct();
        crafted.Skill = "Blacksmithing";
        crafted.Level = 450;
        crafted.Bind = BindsOn.BoP;
        crafted.SpellName = "Probe Spell";
        crafted.BopMats["Probe Mat"] = 4;
        dict["3"] = new ItemLocation[] { crafted, null };

        QuestItem quest = (QuestItem)QuestItem.Construct();
        quest.Area = "Icecrown";
        quest.Quest = "Probe Quest";
        quest.MinLevel = 80;
        quest.Party = 3;
        quest.Type = "g";
        dict["4"] = new ItemLocation[] { quest, null };

        PvpItem pvp = new PvpItem();
        pvp.PointType = "Honor";
        pvp.Points = 100;
        pvp.TokenType = "Mark";
        pvp.TokenCount = 2;
        pvp.PointTypeA = "Arena";
        pvp.PointsA = 50;
        pvp.RequiredArenaRating = 1500;
        pvp.VendorName = "Probe PvP";
        pvp.VendorArea = "Dalaran";
        dict["5"] = new ItemLocation[] { pvp, null };

        FactionItem faction = new FactionItem();
        faction.FactionName = "Probe Faction";
        faction.Level = ReputationLevel.Exalted;
        faction.Cost = 55000;
        faction.TokenMap["Probe Faction Token"] = 1;
        dict["6"] = new ItemLocation[] { faction, null };

        ContainerItem container = new ContainerItem();
        container.Heroic = true;
        container.Area = "Ulduar (10)";
        container.Container = "Cache of Living Stone";
        container.MinLevel = 75;
        container.Party = 2;
        dict["7"] = new ItemLocation[] { container, null };

        dict["8"] = new ItemLocation[] { new NoSource(), new UnknownItem() };
        ItemLocation plain = new ItemLocation();
        plain.Note = "Plain Note";
        dict["9"] = new ItemLocation[] { plain, null };
        return dict;
    }

    private static void WriteXml(string path, Type type, object value)
    {
        XmlWriterSettings settings = new XmlWriterSettings();
        settings.Indent = true;
        settings.Encoding = new UTF8Encoding(false);
        using (XmlWriter writer = XmlWriter.Create(path, settings))
        {
            new XmlSerializer(type).Serialize(writer, value);
        }
    }
}

// Instantiated by Calculations.GetModel(Type) through Activator, so it must be public with a
// public parameterless constructor. Only GetCharacterStats matters for GenInfoString(Character).
[Rawr.Calculations.RawrModelInfo("ProbeModel", "", Rawr.CharacterClass.Mage)]
public class ProbeStubCalculations : Rawr.CalculationsBase
{
    public override Dictionary<string, System.Drawing.Color> SubPointNameColors { get { return new Dictionary<string, System.Drawing.Color>(); } }
    public override string[] CharacterDisplayCalculationLabels { get { return new string[0]; } }
    public override string[] CustomChartNames { get { return new string[0]; } }
    public override CalculationOptionsPanelBase CalculationOptionsPanel { get { return null; } }
    public override List<ItemType> RelevantItemTypes { get { return new List<ItemType>(); } }
    public override CharacterClass TargetClass { get { return CharacterClass.Mage; } }
    public override ComparisonCalculationBase CreateNewComparisonCalculation() { return null; }
    public override CharacterCalculationsBase CreateNewCharacterCalculations() { return null; }
    public override CharacterCalculationsBase GetCharacterCalculations(Character character, Item additionalItem, bool referenceCalculation, bool significantChange, bool needsDisplayCalculations) { return null; }
    public override Stats GetCharacterStats(Character character, Item additionalItem)
    {
        Stats stats = new Stats();
        stats.DamageTakenMultiplier = 0.1f;
        stats.Defense = 275f;
        stats.Dodge = 0.05f;
        stats.Parry = 0.06f;
        stats.Block = 0.07f;
        stats.BlockValue = 800f;
        return stats;
    }
    public override ComparisonCalculationBase[] GetCustomChartData(Character character, string chartName) { return null; }
    public override List<GemmingTemplate> DefaultGemmingTemplates { get { return new List<GemmingTemplate>(); } }
    public override Stats GetRelevantStats(Stats stats) { return stats; }
    public override bool HasRelevantStats(Stats stats) { return true; }
    public override ICalculationOptionBase DeserializeDataObject(string xml) { return null; }
}
