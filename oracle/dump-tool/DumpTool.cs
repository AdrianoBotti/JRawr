// Phase 1 oracle: dump tool.
// Loads a Mage fixture (original character XML), runs the original C# engine and
// writes a golden JSON file. Built with mcs against the prebuilt fork DLLs.
using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;
using Rawr;
using Rawr.Mage;
using Rawr.Optimizer;
using Rawr.RestoSham;

public static class DumpTool
{
    static string BaseDir { get { return AppDomain.CurrentDomain.BaseDirectory; } }

    public static int Main(string[] args)
    {
        if (args.Length < 2)
        {
            Console.Error.WriteLine("usage: DumpTool <fixture.xml> <out.json> [api|direct|none|optimize] [model] [optimizerJavaSeed]");
            return 2;
        }
        string fixturePath = args[0];
        string outPath = args[1];
        string comparisonMode = args.Length > 2 ? args[2] : "direct";
        string model = CanonicalModel(args.Length > 3 ? args[3] : "Mage");
        int optimizerJavaSeed = args.Length > 4
            ? int.Parse(args[4], CultureInfo.InvariantCulture)
            : 123456789;

        if (string.Equals(comparisonMode, "optimize", StringComparison.OrdinalIgnoreCase))
            return DumpOptimizer(fixturePath, outPath, model, optimizerJavaSeed);

        if (string.Equals(comparisonMode, "relative", StringComparison.OrdinalIgnoreCase))
            return DumpRelativeStats(fixturePath, outPath, model);

        if (string.Equals(model, "Mage", StringComparison.OrdinalIgnoreCase))
            return DumpMage(fixturePath, outPath, comparisonMode);

        return DumpModel(fixturePath, outPath, comparisonMode, model);
    }

    static string CanonicalModel(string model)
    {
        if (string.Equals(model, "mage", StringComparison.OrdinalIgnoreCase)) return "Mage";
        if (string.Equals(model, "restosham", StringComparison.OrdinalIgnoreCase)) return "RestoSham";
        if (string.Equals(model, "cat", StringComparison.OrdinalIgnoreCase)) return "Cat";
        if (string.Equals(model, "bear", StringComparison.OrdinalIgnoreCase)) return "Bear";
        if (string.Equals(model, "healadin", StringComparison.OrdinalIgnoreCase)) return "Healadin";
        if (string.Equals(model, "protwarr", StringComparison.OrdinalIgnoreCase)) return "ProtWarr";
        if (string.Equals(model, "moonkin", StringComparison.OrdinalIgnoreCase)) return "Moonkin";
        if (string.Equals(model, "elemental", StringComparison.OrdinalIgnoreCase)) return "Elemental";
        if (string.Equals(model, "retribution", StringComparison.OrdinalIgnoreCase)) return "Retribution";
        if (string.Equals(model, "enhance", StringComparison.OrdinalIgnoreCase)) return "Enhance";
        if (string.Equals(model, "protpally", StringComparison.OrdinalIgnoreCase)) return "ProtPaladin";
        if (string.Equals(model, "healpriest", StringComparison.OrdinalIgnoreCase)) return "HealPriest";
        if (string.Equals(model, "shadowpriest", StringComparison.OrdinalIgnoreCase)) return "ShadowPriest";
        if (string.Equals(model, "hunter", StringComparison.OrdinalIgnoreCase)) return "Hunter";
        if (string.Equals(model, "rogue", StringComparison.OrdinalIgnoreCase)) return "Rogue";
        if (string.Equals(model, "tree", StringComparison.OrdinalIgnoreCase)) return "Tree";
        if (string.Equals(model, "tankdk", StringComparison.OrdinalIgnoreCase)) return "TankDK";
        if (string.Equals(model, "warlock", StringComparison.OrdinalIgnoreCase)) return "Warlock";
        if (string.Equals(model, "dpsdk", StringComparison.OrdinalIgnoreCase)) return "DPSDK";
        if (string.Equals(model, "dpswarr", StringComparison.OrdinalIgnoreCase)) return "DPSWarr";
        return model;
    }

    static int DumpMage(string fixturePath, string outPath, string comparisonMode)
    {
        ItemCache.Load();
        Calculations.LoadModel(typeof(CalculationsMage));
        CalculationsMage calc = CalculationsMage.Instance;

        Character c = Character.Load(fixturePath);
        if (c.CurrentModel != "Mage" || c.Class != CharacterClass.Mage)
            throw new InvalidOperationException("fixture is not a Mage character: " + fixturePath);

        CalculationOptionsMage options = c.CalculationOptions as CalculationOptionsMage;
        if (options == null) { options = new CalculationOptionsMage(); c.CalculationOptions = options; }
        options.Character = c;
        c.RecalculateSetBonuses();

        CharacterCalculationsMage ret = (CharacterCalculationsMage)calc.GetCharacterCalculations(c, null, true, true, true);
        Stats stats = calc.GetCharacterStats(c, null);

        Dictionary<string, object> root = new Dictionary<string, object>();
        root["model"] = "Mage";
        root["fixture"] = Path.GetFileNameWithoutExtension(fixturePath);
        root["character"] = DumpCharacter(c);
        root["calculatedStats"] = DumpStats(stats);
        root["ratings"] = DumpRatings(ret);
        root["display"] = DumpDisplay(ret);
        root["solver"] = DumpSolver(ret, options);
        root["comparison"] = comparisonMode == "none" ? new List<object>()
            : (comparisonMode == "api" ? DumpComparison(c, calc) : DumpComparisonDirect(c, calc));

        string json = Json.Serialize(root);
        File.WriteAllText(outPath, json, new UTF8Encoding(false));
        Console.Error.WriteLine("dumped " + Path.GetFileName(fixturePath) + " -> " + Path.GetFileName(outPath) + " (" + json.Length + " chars)");
        return 0;
    }

    // ==================================================================
    // Phase 7 optimizer oracle: fixed deterministic pool, both engines must
    // reach at least the same score (chosen item set is informational only).
    // Model dispatch mirrors DumpModel (Rawr.<model>.Calculations<model>).
    // ==================================================================
    static int DumpOptimizer(string fixturePath, string outPath, string model, int javaSeed)
    {
        ItemCache.Load();
        // The C# optimizer's ThreadPool valuation path is not thread-safe for the model solvers
        // (concurrent GetCharacterCalculations corrupts shared state) and threading would also make
        // tie-breaking nondeterministic. The Java port is sequential; force the same here.
        Rawr.Properties.GeneralSettings.Default.UseMultithreading = false;

        // HealPriest ships as Rawr.HolyPriest.dll (AssemblyName of the Rawr.HealPriest project).
        string assemblyName = model == "HealPriest" ? "Rawr.HolyPriest" : "Rawr." + model;
        Type calcType = Type.GetType("Rawr." + model + ".Calculations" + model + ", " + assemblyName, true);
        Calculations.LoadModel(calcType);
        CalculationsBase calc = Calculations.Instance;

        Character c = Character.Load(fixturePath);
        if (c.CurrentModel != model)
            throw new InvalidOperationException("fixture is not a " + model + " character: " + fixturePath);

        ICalculationOptionBase options = c.CalculationOptions;
        if (options == null)
        {
            Type optType = Type.GetType("Rawr." + model + ".CalculationOptions" + model + ", " + assemblyName, true);
            options = (ICalculationOptionBase)Activator.CreateInstance(optType);
            c.CalculationOptions = options;
        }
        if (model == "Mage")
        {
            CalculationOptionsMage mageOptions = options as CalculationOptionsMage;
            if (mageOptions == null) { mageOptions = new CalculationOptionsMage(); c.CalculationOptions = mageOptions; }
            mageOptions.Character = c;
        }
        c.RecalculateSetBonuses();

        const int itemsPerSlot = 25;
        const int thoroughness = 150;
        CharacterSlot[] targetSlots = new CharacterSlot[] { CharacterSlot.Head, CharacterSlot.Chest, CharacterSlot.MainHand };
        Item[] relevant = ItemCache.GetRelevantItems(calc, c.Race);

        Dictionary<string, object> slotSelection = new Dictionary<string, object>();
        List<int> availableIds = new List<int>();
        foreach (CharacterSlot slot in targetSlots)
        {
            List<int> ids = relevant
                .Where(it => it.FitsInSlot(slot, c))
                .Select(it => it.Id)
                .OrderBy(id => id)
                .Take(itemsPerSlot)
                .ToList();
            slotSelection[slot.ToString()] = ids.Cast<object>().ToList();
            foreach (int id in ids) if (!availableIds.Contains(id)) availableIds.Add(id);
        }
        // Keep the rest of the character's equipped items selectable so the optimizer does not
        // strip the non-target slots.
        for (int i = 0; i < 19; i++)
        {
            ItemInstance inst = c[(CharacterSlot)i];
            if (inst == null || inst.Item == null) continue;
            if (!availableIds.Contains(inst.Item.Id)) availableIds.Add(inst.Item.Id);
        }
        List<string> availableItems = availableIds.Select(id => id.ToString()).ToList();

        ItemInstanceOptimizer optimizer = new ItemInstanceOptimizer();
        optimizer.OptimizationMethod = OptimizationMethod.GeneticAlgorithm;
        optimizer.GreedyOptimizationMethod = GreedyOptimizationMethod.AllCombinations;
        optimizer.InitializeItemCache(c, availableItems, false, false, true, calc, false, false, false, null, false);

        Character optimized = optimizer.OptimizeCharacter(c, "[Overall]", new List<OptimizationRequirement>(), thoroughness, false);
        if (optimized == null) throw new InvalidOperationException("optimizer returned no character");

        CharacterCalculationsBase ret = calc.GetCharacterCalculations(optimized, null, false, false, false);
        float bestValue = ItemInstanceOptimizer.GetOptimizationValue(optimized, ret);

        List<object> sub = new List<object>();
        if (ret.SubPoints != null) foreach (float f in ret.SubPoints) sub.Add(f);

        List<object> chosen = new List<object>();
        for (int i = 0; i < 19; i++)
        {
            ItemInstance inst = optimized[(CharacterSlot)i];
            if (inst == null || inst.Item == null) continue;
            chosen.Add(new Dictionary<string, object> {
                { "slot", ((CharacterSlot)i).ToString() }, { "itemId", inst.Item.Id }, { "gemmedId", inst.GemmedId } });
        }

        Dictionary<string, object> root = new Dictionary<string, object>();
        root["model"] = model;
        root["fixture"] = Path.GetFileNameWithoutExtension(fixturePath);
        root["driving"] = new Dictionary<string, object> {
            { "calculationToOptimize", "[Overall]" },
            { "thoroughness", thoroughness },
            { "optimizationMethod", "GeneticAlgorithm" },
            { "greedyOptimizationMethod", "AllCombinations" },
            { "templateGemsEnabled", true },
            { "overrideRegem", false },
            { "overrideReenchant", false },
            { "optimizeFood", false },
            { "optimizeElixirs", false },
            { "optimizeTalents", false },
            { "multithreading", false },
            { "javaSeed", javaSeed },
            { "tolerance", new Dictionary<string, object> { { "relative", 1e-4 }, { "absoluteFloor", 1.0 } } }
        };
        root["pool"] = new Dictionary<string, object> {
            { "slotSelectionRule", "first " + itemsPerSlot + " model-relevant items fitting the slot, sorted by item id, plus every currently equipped item id" },
            { "slots", slotSelection },
            { "availableItemIds", availableIds.Cast<object>().ToList() }
        };
        root["csharp"] = new Dictionary<string, object> {
            { "bestOverallPoints", bestValue },
            { "bestSubPoints", sub },
            { "chosenItems", chosen }
        };

        string json = Json.Serialize(root);
        File.WriteAllText(outPath, json, new UTF8Encoding(false));
        Console.Error.WriteLine("dumped optimizer " + Path.GetFileName(fixturePath) + " -> " + Path.GetFileName(outPath)
            + " (best " + bestValue.ToString("R", CultureInfo.InvariantCulture) + ", " + availableItems.Count + " pool ids, " + json.Length + " chars)");
        return 0;
    }

    static Dictionary<string, object> DumpCharacter(Character c)
    {
        Dictionary<string, object> d = new Dictionary<string, object>();
        d["name"] = c.Name;
        d["race"] = c.Race.ToString();
        d["class"] = c.Class.ToString();
        d["currentModel"] = c.CurrentModel;
        d["primaryProfession"] = c.PrimaryProfession.ToString();
        d["enforceGemRequirements"] = c.EnforceGemRequirements;

        List<object> buffs = new List<object>();
        if (c.ActiveBuffs != null)
            foreach (Buff b in c.ActiveBuffs) buffs.Add(b.Name);
        d["activeBuffs"] = buffs;

        Dictionary<string, object> setBonuses = new Dictionary<string, object>();
        if (c.SetBonusCount != null)
            foreach (KeyValuePair<string, int> pair in c.SetBonusCount.OrderBy(kv => kv.Key))
                setBonuses[pair.Key] = pair.Value;
        d["setBonusCount"] = setBonuses;

        List<object> items = new List<object>();
        foreach (CharacterSlot slot in Enum.GetValues(typeof(CharacterSlot)))
        {
            int i = (int)slot;
            if (i < 0 || i > 20) continue;
            ItemInstance inst = c[slot];
            if (inst == null || inst.Item == null) continue;
            Dictionary<string, object> item = new Dictionary<string, object>();
            item["slot"] = slot.ToString();
            item["id"] = inst.Item.Id;
            item["name"] = inst.Item.Name;
            item["enchantId"] = inst.EnchantId;
            item["gem1Id"] = inst.Gem1Id;
            item["gem2Id"] = inst.Gem2Id;
            item["gem3Id"] = inst.Gem3Id;
            item["matchesSocketBonus"] = inst.MatchesSocketBonus;
            item["totalStats"] = DumpStats(inst.GetTotalStats(c));
            items.Add(item);
        }
        d["items"] = items;
        return d;
    }

    static Dictionary<string, object> DumpStats(Stats s)
    {
        Dictionary<string, object> d = new Dictionary<string, object>();
        if (s == null) return d;
        Type t = s.GetType();
        List<KeyValuePair<string, object>> values = new List<KeyValuePair<string, object>>();
        foreach (PropertyInfo pi in t.GetProperties(BindingFlags.Public | BindingFlags.Instance))
        {
            if (pi.GetIndexParameters().Length != 0 || !pi.CanRead) continue;
            if (pi.PropertyType == typeof(float)) values.Add(new KeyValuePair<string, object>(pi.Name, pi.GetValue(s, null)));
        }
        foreach (FieldInfo fi in t.GetFields(BindingFlags.Public | BindingFlags.Instance))
        {
            if (fi.FieldType == typeof(float)) values.Add(new KeyValuePair<string, object>(fi.Name, fi.GetValue(s)));
        }
        foreach (KeyValuePair<string, object> kv in values.OrderBy(kv => kv.Key))
            d[kv.Key] = kv.Value;
        return d;
    }

    static object SimpleValue(object o)
    {
        if (o == null) return null;
        Type t = o.GetType();
        if (t == typeof(float) || t == typeof(double) || t == typeof(int) || t == typeof(long)
            || t == typeof(short) || t == typeof(byte) || t == typeof(bool) || t == typeof(string))
            return o;
        return null;
    }

    static Dictionary<string, object> DumpSimpleMembers(object o)
    {
        Dictionary<string, object> d = new Dictionary<string, object>();
        Type t = o.GetType();
        foreach (PropertyInfo pi in t.GetProperties(BindingFlags.Public | BindingFlags.Instance))
        {
            if (pi.GetIndexParameters().Length != 0 || !pi.CanRead) continue;
            object v = SimpleValue(pi.GetValue(o, null));
            if (v != null) d[pi.Name] = v;
        }
        foreach (FieldInfo fi in t.GetFields(BindingFlags.Public | BindingFlags.Instance))
        {
            object v = SimpleValue(fi.GetValue(o));
            if (v != null) d[fi.Name] = v;
        }
        return d;
    }

    static Dictionary<string, object> DumpRatings(CharacterCalculationsMage ret)
    {
        Dictionary<string, object> d = DumpSimpleMembers(ret);
        List<object> sub = new List<object>();
        foreach (float f in ret.SubPoints) sub.Add(f);
        d["SubPoints"] = sub;
        return d;
    }

    static Dictionary<string, object> DumpDisplay(CharacterCalculationsMage ret)
    {
        Dictionary<string, object> d = new Dictionary<string, object>();
        Dictionary<string, string> display = ret.GetCharacterDisplayCalculationValues();
        if (display != null)
            foreach (KeyValuePair<string, string> kv in display.OrderBy(kv => kv.Key))
                d[kv.Key] = kv.Value;
        return d;
    }

    static List<object> DumpComparison(Character c, CalculationsMage calc)
    {
        List<object> list = new List<object>();
        int[] candidates = new int[] { 51281, 50661, 51255, 51231, 51158, 50006, 51837 };
        foreach (int id in candidates)
        {
            Item item = ItemCache.FindItemById(id);
            if (item == null || item.Slot != ItemSlot.Head) continue;
            ComparisonCalculationBase comp = calc.GetItemCalculations(item, c, CharacterSlot.Head);
            if (comp == null) continue;
            Dictionary<string, object> d = new Dictionary<string, object>();
            d["itemId"] = id;
            d["name"] = comp.Name;
            d["overallPoints"] = comp.OverallPoints;
            List<object> sub = new List<object>();
            if (comp.SubPoints != null) foreach (float f in comp.SubPoints) sub.Add(f);
            d["subPoints"] = sub;
            list.Add(d);
        }
        return list;
    }

    static List<object> DumpComparisonDirect(Character c, CalculationsMage calc)
    {
        List<object> list = new List<object>();
        int[] candidates = new int[] { 51281, 50661, 51255, 51231, 51158, 50006, 51837 };
        CharacterCalculationsMage baseline = (CharacterCalculationsMage)calc.GetCharacterCalculations(c, null, true, true, false);
        foreach (int id in candidates)
        {
            Item item = ItemCache.FindItemById(id);
            if (item == null || item.Slot != ItemSlot.Head) continue;
            Character clone = c.Clone();
            clone[CharacterSlot.Head] = new ItemInstance(id, 0, 0, 0, 0);
            clone.RecalculateSetBonuses();
            CharacterCalculationsMage r;
            try { r = (CharacterCalculationsMage)calc.GetCharacterCalculations(clone, null, true, true, false); }
            catch (Exception ex)
            {
                list.Add(new Dictionary<string, object> { { "itemId", id }, { "error", ex.Message } });
                continue;
            }
            Dictionary<string, object> d = new Dictionary<string, object>();
            d["itemId"] = id;
            d["name"] = item.Name;
            d["overallPoints"] = r.OverallPoints - baseline.OverallPoints;
            List<object> sub = new List<object>();
            for (int i = 0; i < r.SubPoints.Length; i++)
                sub.Add(r.SubPoints[i] - baseline.SubPoints[i]);
            d["subPoints"] = sub;
            list.Add(d);
        }
        return list;
    }

    static Dictionary<string, object> DumpSolver(CharacterCalculationsMage ret, CalculationOptionsMage options)
    {
        Dictionary<string, object> d = new Dictionary<string, object>();
        DisplayCalculations dc = ret.DisplayCalculations;
        if (dc == null) return d;
        d["fightDuration"] = options.FightDuration;
        d["targetLevel"] = options.TargetLevel;

        string[] simpleFields = new string[] {
            "MageArmor", "StartingMana", "BaseGlobalCooldown", "Tps", "ManaGemValue", "ManaPotionValue",
            "MaxManaGemValue", "MaxManaPotionValue", "ManaAdeptBonus", "RawArcaneHitRate", "RawFireHitRate",
            "RawFrostHitRate", "EvocationDuration", "EvocationRegen", "EvocationDurationIV", "EvocationRegenIV",
            "EvocationDurationHero", "EvocationRegenHero", "EvocationDurationIVHero", "EvocationRegenIVHero",
            "CombustionCooldown", "PowerInfusionDuration", "PowerInfusionCooldown", "MirrorImageDuration",
            "MirrorImageCooldown", "IcyVeinsCooldown", "ColdsnapCooldown", "ArcanePowerCooldown",
            "ArcanePowerDuration", "WaterElementalCooldown", "WaterElementalDuration", "EvocationCooldown",
            "ManaGemEffectDuration", "UpperBound", "LowerBound", "ChanceToDie", "MeanIncomingDps",
            "DamageTakenReduction" };
        Type dt = dc.GetType();
        foreach (string name in simpleFields)
        {
            FieldInfo fi = dt.GetField(name, BindingFlags.Public | BindingFlags.Instance);
            if (fi != null) { object v = SimpleValue(fi.GetValue(dc)); if (v != null) d[name] = v; continue; }
            PropertyInfo pi = dt.GetProperty(name, BindingFlags.Public | BindingFlags.Instance);
            if (pi != null) { object v = SimpleValue(pi.GetValue(dc, null)); if (v != null) d[name] = v; }
        }

        // segments
        List<object> segments = new List<object>();
        if (dc.SegmentList != null)
            foreach (Segment seg in dc.SegmentList)
                segments.Add(new Dictionary<string, object> {
                    { "index", seg.Index }, { "duration", seg.Duration }, { "timeStart", seg.TimeStart } });
        d["segments"] = segments;

        // cooldowns
        List<object> cooldowns = new List<object>();
        if (dc.CooldownList != null)
            foreach (EffectCooldown cd in dc.CooldownList)
                cooldowns.Add(new Dictionary<string, object> {
                    { "name", cd.Name }, { "mask", cd.Mask }, { "cooldown", cd.Cooldown },
                    { "duration", cd.Duration }, { "maximumDuration", cd.MaximumDuration },
                    { "itemBased", cd.ItemBased } });
        d["cooldowns"] = cooldowns;

        // solution vector
        List<object> solution = new List<object>();
        if (dc.Solution != null) foreach (double v in dc.Solution) solution.Add(v);
        d["solution"] = solution;

        // solution variables (chosen cycles etc.)
        List<object> vars = new List<object>();
        if (dc.SolutionVariable != null)
            foreach (SolutionVariable sv in dc.SolutionVariable)
            {
                Dictionary<string, object> v = new Dictionary<string, object>();
                v["segment"] = sv.Segment;
                v["manaSegment"] = sv.ManaSegment;
                v["type"] = sv.Type.ToString();
                v["dps"] = sv.Dps;
                v["mps"] = sv.Mps;
                v["tps"] = sv.Tps;
                v["cycle"] = sv.Cycle != null ? sv.Cycle.Name : null;
                v["state"] = sv.State != null ? (object)sv.State.Effects : null;
                vars.Add(v);
            }
        d["solutionVariables"] = vars;

        // candidate cycles available in the base casting state
        List<object> cycles = new List<object>();
        if (dc.BaseState != null)
        {
            foreach (CycleId cid in Enum.GetValues(typeof(CycleId)))
            {
                if (cid == CycleId.None) continue;
                Cycle cyc = null;
                try { cyc = dc.BaseState.GetCycle(cid); }
                catch (Exception) { }
                if (cyc == null) continue;
                cycles.Add(new Dictionary<string, object> {
                    { "cycleId", cid.ToString() }, { "name", cyc.Name },
                    { "dps", cyc.DamagePerSecond }, { "mps", cyc.ManaPerSecond }, { "castTime", cyc.CastTime } });
            }
        }
        d["candidateCycles"] = cycles;

        // reconstructed sequence (only when enabled in options)
        string sequence = null;
        try { sequence = dc.ReconstructSequence(); }
        catch (Exception ex) { sequence = "EXCEPTION: " + ex.Message; }
        d["sequence"] = sequence;

        return d;
    }

    // ==================================================================
    // Shared non-Mage dump (Phase 6 oracle: RestoSham, Cat, ...)
    // ==================================================================
    static int DumpModel(string fixturePath, string outPath, string comparisonMode, string model)
    {
        ItemCache.Load();
        // HealPriest ships as Rawr.HolyPriest.dll (AssemblyName of the Rawr.HealPriest project).
        string assemblyName = model == "HealPriest" ? "Rawr.HolyPriest" : "Rawr." + model;
        Type calcType = Type.GetType("Rawr." + model + ".Calculations" + model + ", " + assemblyName, true);
        Calculations.LoadModel(calcType);
        CalculationsBase calc = Calculations.Instance;

        Character c = Character.Load(fixturePath);
        if (c.CurrentModel != model)
            throw new InvalidOperationException("fixture is not a " + model + " character: " + fixturePath);

        ICalculationOptionBase options = c.CalculationOptions;
        if (options == null)
        {
            Type optType = Type.GetType("Rawr." + model + ".CalculationOptions" + model + ", " + assemblyName, true);
            options = (ICalculationOptionBase)Activator.CreateInstance(optType);
            c.CalculationOptions = options;
        }
        c.RecalculateSetBonuses();

        CharacterCalculationsBase ret = calc.GetCharacterCalculations(c, null, true, true, true);
        Stats stats = calc.GetCharacterStats(c, null);

        Dictionary<string, object> root = new Dictionary<string, object>();
        root["model"] = model;
        root["fixture"] = Path.GetFileNameWithoutExtension(fixturePath);
        root["character"] = DumpCharacter(c);
        root["calculatedStats"] = DumpStats(stats);
        root["ratings"] = DumpRatingsSham(ret);
        root["display"] = DumpDisplaySham(ret);
        root["comparison"] = comparisonMode == "none" ? new List<object>()
            : DumpComparisonSham(c, calc, GetComparisonCandidates(model));

        string json = Json.Serialize(root);
        File.WriteAllText(outPath, json, new UTF8Encoding(false));
        Console.Error.WriteLine("dumped " + Path.GetFileName(fixturePath) + " -> " + Path.GetFileName(outPath) + " (" + json.Length + " chars)");
        return 0;
    }

    // Relative Stats chart oracle: dump CalculationsBase.GetRelativeStatValues for one fixture.
    // Kept separate from the model golden so it can cover a few representative fixtures without
    // changing every existing golden file.
    static int DumpRelativeStats(string fixturePath, string outPath, string model)
    {
        ItemCache.Load();
        string assemblyName = model == "HealPriest" ? "Rawr.HolyPriest" : "Rawr." + model;
        Type calcType = Type.GetType("Rawr." + model + ".Calculations" + model + ", " + assemblyName, true);
        Calculations.LoadModel(calcType);
        CalculationsBase calc = Calculations.Instance;

        Character c = Character.Load(fixturePath);
        if (c.CurrentModel != model)
            throw new InvalidOperationException("fixture is not a " + model + " character: " + fixturePath);

        ICalculationOptionBase options = c.CalculationOptions;
        if (options == null)
        {
            Type optType = Type.GetType("Rawr." + model + ".CalculationOptions" + model + ", " + assemblyName, true);
            options = (ICalculationOptionBase)Activator.CreateInstance(optType);
            c.CalculationOptions = options;
        }
        if (model == "Mage")
        {
            CalculationOptionsMage mageOptions = options as CalculationOptionsMage;
            if (mageOptions == null) { mageOptions = new CalculationOptionsMage(); c.CalculationOptions = mageOptions; }
            mageOptions.Character = c;
        }
        c.RecalculateSetBonuses();

        ComparisonCalculationBase[] values = CalculationsBase.GetRelativeStatValues(c);
        List<object> stats = new List<object>();
        foreach (ComparisonCalculationBase value in values)
        {
            Dictionary<string, object> d = new Dictionary<string, object>();
            d["name"] = value.Name;
            d["description"] = value.Description;
            d["baseStat"] = value.BaseStat;
            d["overallPoints"] = value.OverallPoints;
            List<object> sub = new List<object>();
            if (value.SubPoints != null) foreach (float f in value.SubPoints) sub.Add(f);
            d["subPoints"] = sub;
            stats.Add(d);
        }

        Dictionary<string, object> root = new Dictionary<string, object>();
        root["model"] = model;
        root["fixture"] = Path.GetFileNameWithoutExtension(fixturePath);
        root["relativeStats"] = stats;
        string json = Json.Serialize(root);
        File.WriteAllText(outPath, json, new UTF8Encoding(false));
        Console.Error.WriteLine("dumped relative stats " + Path.GetFileName(fixturePath) + " -> " + Path.GetFileName(outPath) + " (" + json.Length + " chars)");
        return 0;
    }

    // Head candidates per model. RestoSham keeps the exact list used when its golden files were
    // first generated; Cat uses fitting leather feral head items (5-7 entries).
    static int[] GetComparisonCandidates(string model)
    {
        if (model == "Cat")
            return new int[] { 51296, 51143, 51252, 50713, 51494, 51427, 51825 };
        if (model == "Bear")
            return new int[] { 51296, 51143, 50713, 51252, 51866, 51494, 51427 };
        if (model == "Healadin")
            return new int[] { 51272, 51167, 50701, 50060, 51470, 50867, 47681 };
        if (model == "ProtWarr")
            return new int[] { 51213, 51214, 51215, 51131, 51470, 50867, 50060 };
        if (model == "Moonkin")
            return new int[] { 51290, 51149, 50679, 50026, 51281, 50661, 50006 };
        if (model == "Elemental")
            return new int[] { 51237, 51242, 51286, 50626, 51906, 51499, 51247 };
        if (model == "Retribution")
            return new int[] { 51277, 51266, 51227, 50712, 50640, 51162, 50072 };
        if (model == "Enhance")
            return new int[] { 51242, 51197, 50832, 51286, 51153, 50605, 49952 };
        if (model == "ProtPaladin")
            return new int[] { 51266, 50640, 50701, 51173, 50060, 51221, 51470 };
        if (model == "HealPriest")
            return new int[] { 51261, 51178, 50661, 50006, 51896, 51255, 51184 };
        if (model == "ShadowPriest")
            return new int[] { 51255, 51184, 51281, 51158, 51231, 51208, 51837 };
        if (model == "Hunter")
            return new int[] { 51286, 51242, 50605, 51153, 51197, 49952, 51877 };
        if (model == "Rogue")
            return new int[] { 51252, 51187, 50713, 51296, 51494, 50073, 51866 };
        if (model == "Tree")
            return new int[] { 51281, 51261, 51178, 50661, 50006, 51255, 51184 };
        if (model == "TankDK")
            return new int[] { 51139, 51143, 50060, 50640, 51266, 51173, 51470 };
        if (model == "Warlock")
            return new int[] { 51208, 51255, 51184, 51281, 51158, 51231, 51837 };
        if (model == "DPSDK")
            return new int[] { 51312, 51127, 51227, 51277, 50712, 50640, 51266 };
        if (model == "DPSWarr")
            return new int[] { 51227, 50856, 50712, 51277, 50640, 51266, 51162 };
        return new int[] { 51281, 50661, 51255, 51231, 51158, 50006, 51837 };
    }

    static Dictionary<string, object> DumpRatingsSham(CharacterCalculationsBase ret)
    {
        Dictionary<string, object> d = DumpSimpleMembers(ret);
        List<object> sub = new List<object>();
        foreach (float f in ret.SubPoints) sub.Add(f);
        d["SubPoints"] = sub;
        return d;
    }

    static Dictionary<string, object> DumpDisplaySham(CharacterCalculationsBase ret)
    {
        Dictionary<string, object> d = new Dictionary<string, object>();
        Dictionary<string, string> display = ret.GetCharacterDisplayCalculationValues();
        if (display != null)
            foreach (KeyValuePair<string, string> kv in display.OrderBy(kv => kv.Key))
                d[kv.Key] = kv.Value;
        return d;
    }

    static List<object> DumpComparisonSham(Character c, CalculationsBase calc, int[] candidates)
    {
        List<object> list = new List<object>();
        foreach (int id in candidates)
        {
            Item item = ItemCache.FindItemById(id);
            if (item == null || item.Slot != ItemSlot.Head) continue;
            ComparisonCalculationBase comp = calc.GetItemCalculations(item, c, CharacterSlot.Head);
            if (comp == null) continue;
            Dictionary<string, object> d = new Dictionary<string, object>();
            d["itemId"] = id;
            d["name"] = comp.Name;
            d["overallPoints"] = comp.OverallPoints;
            List<object> sub = new List<object>();
            if (comp.SubPoints != null) foreach (float f in comp.SubPoints) sub.Add(f);
            d["subPoints"] = sub;
            list.Add(d);
        }
        return list;
    }
}

public static class Json
{
    public static string Serialize(object value)
    {
        StringBuilder sb = new StringBuilder();
        Write(sb, value, 0);
        sb.Append('\n');
        return sb.ToString();
    }

    static void Write(StringBuilder sb, object value, int indent)
    {
        if (value == null) { sb.Append("null"); return; }
        if (value is string) { Escape(sb, (string)value); return; }
        if (value is bool) { sb.Append(((bool)value) ? "true" : "false"); return; }
        if (value is float) { sb.Append(FormatFloat((float)value)); return; }
        if (value is double) { sb.Append(FormatDouble((double)value)); return; }
        if (value is int || value is long || value is short || value is byte)
        { sb.Append(Convert.ToString(value, CultureInfo.InvariantCulture)); return; }

        IDictionary<string, object> map = value as IDictionary<string, object>;
        if (map != null)
        {
            if (map.Count == 0) { sb.Append("{}"); return; }
            sb.Append("{\n");
            bool first = true;
            foreach (KeyValuePair<string, object> kv in map)
            {
                if (!first) sb.Append(",\n");
                first = false;
                Pad(sb, indent + 1);
                Escape(sb, kv.Key);
                sb.Append(": ");
                Write(sb, kv.Value, indent + 1);
            }
            sb.Append('\n'); Pad(sb, indent); sb.Append('}');
            return;
        }

        IEnumerable<object> list = value as IEnumerable<object>;
        if (list != null)
        {
            List<object> items = list.ToList();
            if (items.Count == 0) { sb.Append("[]"); return; }
            sb.Append("[\n");
            for (int i = 0; i < items.Count; i++)
            {
                if (i > 0) sb.Append(",\n");
                Pad(sb, indent + 1);
                Write(sb, items[i], indent + 1);
            }
            sb.Append('\n'); Pad(sb, indent); sb.Append(']');
            return;
        }
        Escape(sb, value.ToString());
    }

    static void Pad(StringBuilder sb, int indent) { sb.Append(' ', indent * 2); }

    static string FormatFloat(float f)
    {
        if (float.IsNaN(f)) return "\"NaN\"";
        if (float.IsPositiveInfinity(f)) return "\"Infinity\"";
        if (float.IsNegativeInfinity(f)) return "\"-Infinity\"";
        return f.ToString("G9", CultureInfo.InvariantCulture);
    }

    static string FormatDouble(double d)
    {
        if (double.IsNaN(d)) return "\"NaN\"";
        if (double.IsPositiveInfinity(d)) return "\"Infinity\"";
        if (double.IsNegativeInfinity(d)) return "\"-Infinity\"";
        return d.ToString("G17", CultureInfo.InvariantCulture);
    }

    static void Escape(StringBuilder sb, string s)
    {
        sb.Append('"');
        foreach (char ch in s)
        {
            switch (ch)
            {
                case '"': sb.Append("\\\""); break;
                case '\\': sb.Append("\\\\"); break;
                case '\b': sb.Append("\\b"); break;
                case '\f': sb.Append("\\f"); break;
                case '\n': sb.Append("\\n"); break;
                case '\r': sb.Append("\\r"); break;
                case '\t': sb.Append("\\t"); break;
                default:
                    if (ch < 0x20) sb.Append("\\u").Append(((int)ch).ToString("x4"));
                    else sb.Append(ch);
                    break;
            }
        }
        sb.Append('"');
    }
}
