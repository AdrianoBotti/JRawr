// Oracle probe for Rawr.Base.BaseStats (prebuilt legacy assembly, run under Mono).
//
// Calls BaseStats.GetBaseStats for every class/race combination at levels 1 and 80 (all four
// Druid forms, Caster only for the other classes) and writes a canonical dump of the resulting
// Stats: every non-zero public float property (IEEE-754 hex bits) plus every special effect
// (trigger/duration/cooldown/chance/maxStack and nested stats), recursively.
//
// The Java BaseStatsGoldenTest replays the same combinations and compares the dump line by line.
// The legacy tree is only read, never built or modified.
//
// Build/run: tools/base-stats-probe/run.sh
using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Reflection;
using System.Text;
using Rawr;

public static class BaseStatsProbe
{
    private static PropertyInfo[] floatProps;

    public static int Main(string[] args)
    {
        if (args.Length != 1)
        {
            Console.Error.WriteLine("usage: BaseStatsProbe <expected.txt>");
            return 2;
        }
        CultureInfo.CurrentCulture = CultureInfo.InvariantCulture;
        CultureInfo.CurrentUICulture = CultureInfo.InvariantCulture;
        floatProps = FloatProperties();

        StringBuilder sb = new StringBuilder();
        int[] levels = { 1, 80 };
        foreach (int level in levels)
        {
            foreach (CharacterClass characterClass in Enum.GetValues(typeof(CharacterClass)))
            {
                foreach (CharacterRace characterRace in Enum.GetValues(typeof(CharacterRace)))
                {
                    BaseStats.DruidForm[] forms = characterClass == CharacterClass.Druid
                        ? new[]
                          {
                              BaseStats.DruidForm.Caster,
                              BaseStats.DruidForm.Bear,
                              BaseStats.DruidForm.Cat,
                              BaseStats.DruidForm.Moonkin,
                          }
                        : new[] { BaseStats.DruidForm.Caster };
                    foreach (BaseStats.DruidForm form in forms)
                    {
                        Stats stats = BaseStats.GetBaseStats(level, characterClass, characterRace, form);
                        sb.Append("== ").Append(level).Append('|').Append(characterClass).Append('|')
                          .Append(characterRace).Append('|').Append(form).Append('\n');
                        DumpStats(stats, "", sb);
                    }
                }
            }
        }
        File.WriteAllText(args[0], sb.ToString());
        Console.WriteLine("wrote base stats to " + args[0]);
        return 0;
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
