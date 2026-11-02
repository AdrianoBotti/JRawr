// Oracle probe for Rawr.Base.SpecialEffects (prebuilt legacy assembly, run under Mono).
//
// Reads the committed vectors (jrawr/rawr-core/src/test/resources/special-effects/vectors.txt),
// calls ProcessMetaGem / ProcessUseLine / ProcessEquipLine on a fresh Rawr.Stats for each vector
// and writes a canonical dump of the resulting float fields (IEEE-754 hex bits) and special
// effects (trigger/duration/cooldown/chance/maxStack + nested stats) to expected.txt.
//
// The Java SpecialEffectsTest replays the same vectors and compares the dump text line by line.
// The legacy tree is only read, never built or modified.
//
// Build/run: tools/special-effects-probe/run.sh
using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Text;
using Rawr;

public static class SpecialEffectsProbe
{
    private static PropertyInfo[] floatProps;

    public static int Main(string[] args)
    {
        if (args.Length != 2)
        {
            Console.Error.WriteLine("usage: SpecialEffectsProbe <vectors.txt> <expected.txt>");
            return 2;
        }
        floatProps = FloatProperties();
        StringBuilder sb = new StringBuilder();
        string[] lines = File.ReadAllLines(args[0]);
        int index = 0;
        foreach (string raw in lines)
        {
            string text = raw.Trim();
            if (text.Length == 0 || text.StartsWith("#")) continue;
            string[] parts = text.Split('|');
            string kind = parts[0];
            Stats stats = new Stats();
            sb.Append("== ").Append(index).Append(' ').Append(text).Append('\n');
            // A throwing line is observable behavior: record it and still dump the partial stats.
            try
            {
                if (kind == "M")
                {
                    SpecialEffects.ProcessMetaGem(Join(parts, 1), stats, false);
                }
                else if (kind == "U")
                {
                    bool isArmory = parts[1] == "1";
                    int id = int.Parse(parts[2]);
                    SpecialEffects.ProcessUseLine(Join(parts, 3), stats, isArmory, id);
                }
                else if (kind == "E")
                {
                    bool isArmory = parts[1] == "1";
                    int ilvl = int.Parse(parts[2]);
                    int id = int.Parse(parts[3]);
                    SpecialEffects.ProcessEquipLine(Join(parts, 4), stats, isArmory, ilvl, id);
                }
                else
                {
                    Console.Error.WriteLine("unknown vector kind: " + kind);
                    return 2;
                }
            }
            catch (Exception)
            {
                sb.Append("X throw\n");
            }
            DumpStats(stats, "", sb);
            index++;
        }
        File.WriteAllText(args[1], sb.ToString());
        Console.WriteLine("wrote " + index + " vectors to " + args[1]);
        return 0;
    }

    private static string Join(string[] parts, int start)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < parts.Length; i++)
        {
            if (i > start) sb.Append('|');
            sb.Append(parts[i]);
        }
        return sb.ToString();
    }

    private static PropertyInfo[] FloatProperties()
    {
        List<PropertyInfo> list = new List<PropertyInfo>();
        foreach (PropertyInfo p in typeof(Stats).GetProperties(BindingFlags.Public | BindingFlags.Instance))
        {
            if (p.PropertyType != typeof(float)) continue;
            if (!p.CanRead) continue;
            if (p.GetIndexParameters().Length != 0) continue;
            list.Add(p);
        }
        list.Sort(delegate(PropertyInfo a, PropertyInfo b) { return string.CompareOrdinal(a.Name, b.Name); });
        return list.ToArray();
    }

    private static void DumpStats(Stats stats, string prefix, StringBuilder sb)
    {
        foreach (PropertyInfo p in floatProps)
        {
            float v = (float)p.GetValue(stats, null);
            int bits = BitConverter.ToInt32(BitConverter.GetBytes(v), 0);
            if (bits != 0)
            {
                sb.Append("S ").Append(prefix).Append(p.Name).Append(' ').Append(bits.ToString("X8")).Append('\n');
            }
        }
        if (stats._rawSpecialEffectData == null) return;
        for (int i = 0; i < stats._rawSpecialEffectDataSize; i++)
        {
            SpecialEffect e = stats._rawSpecialEffectData[i];
            if (e == null) continue;
            string path = prefix + i + ".";
            sb.Append("E ").Append(path)
              .Append("trigger=").Append(e.Trigger)
              .Append(" duration=").Append(Hex(e.Duration))
              .Append(" cooldown=").Append(Hex(e.Cooldown))
              .Append(" chance=").Append(Hex(e.Chance))
              .Append(" maxStack=").Append(e.MaxStack).Append('\n');
            DumpStats(e.Stats, path, sb);
        }
    }

    private static string Hex(float v)
    {
        return BitConverter.ToInt32(BitConverter.GetBytes(v), 0).ToString("X8");
    }
}
