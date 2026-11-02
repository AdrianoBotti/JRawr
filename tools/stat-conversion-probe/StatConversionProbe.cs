// Oracle probe for Rawr.Base.StatConversion.GetCombatTablePlayer_vs_NPC (prebuilt legacy
// assembly, run under Mono). Dumps the seven float fields and two flags as IEEE-754 hex bits for
// a matrix of levels, dual-wield/white-or-yellow, behind-time, hit, expertise and crit values.
//
// The Java StatConversionGoldenTest replays the same matrix and compares the dump line by line.
// The legacy tree is only read, never built or modified.
//
// Build/run: tools/stat-conversion-probe/run.sh
using System;
using System.Globalization;
using System.IO;
using System.Text;
using Rawr;

public static class StatConversionProbe
{
    public static int Main(string[] args)
    {
        if (args.Length != 1)
        {
            Console.Error.WriteLine("usage: StatConversionProbe <expected.txt>");
            return 2;
        }
        CultureInfo.CurrentCulture = CultureInfo.InvariantCulture;
        CultureInfo.CurrentUICulture = CultureInfo.InvariantCulture;

        int[] attackerLevels = { 80 };
        int[] targetLevels = { 80, 81, 82, 83 };
        bool[] bools = { false, true };
        float[] behinds = { 0f, 0.5f, 1f };
        float[] hits = { 0f, 0.03f };
        float[] expertises = { 0f, 0.02f };
        float[] crits = { 0f, 0.1f };

        StringBuilder sb = new StringBuilder();
        foreach (int attackerLevel in attackerLevels)
        {
            foreach (int targetLevel in targetLevels)
            {
                foreach (bool dualWield in bools)
                {
                    foreach (bool whiteOrYellow in bools)
                    {
                        foreach (float behind in behinds)
                        {
                            foreach (float hit in hits)
                            {
                                foreach (float expertise in expertises)
                                {
                                    foreach (float crit in crits)
                                    {
                                        StatConversion.CombatTable ct = StatConversion.GetCombatTablePlayer_vs_NPC(
                                                attackerLevel, targetLevel, dualWield, whiteOrYellow, behind, hit, expertise, crit);
                                        sb.Append("== ").Append(attackerLevel).Append('|').Append(targetLevel)
                                          .Append('|').Append(dualWield ? 1 : 0)
                                          .Append('|').Append(whiteOrYellow ? 1 : 0)
                                          .Append('|').Append(Float(behind))
                                          .Append('|').Append(Float(hit))
                                          .Append('|').Append(Float(expertise))
                                          .Append('|').Append(Float(crit)).Append('\n');
                                        Append(sb, "Miss", ct.Miss);
                                        Append(sb, "Dodge", ct.Dodge);
                                        Append(sb, "Parry", ct.Parry);
                                        Append(sb, "Glancing", ct.Glancing);
                                        Append(sb, "Block", ct.Block);
                                        Append(sb, "Crit", ct.Crit);
                                        Append(sb, "Hit", ct.Hit);
                                        sb.Append("B bPlayerTarget ").Append(ct.bPlayerTarget ? 1 : 0).Append('\n');
                                        sb.Append("B bBehind ").Append(ct.bBehind ? 1 : 0).Append('\n');
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        File.WriteAllText(args[0], sb.ToString());
        Console.WriteLine("wrote StatConversion combat table to " + args[0]);
        return 0;
    }

    private static string Float(float value)
    {
        return value.ToString("R", CultureInfo.InvariantCulture);
    }

    private static void Append(StringBuilder sb, string name, float value)
    {
        int bits = BitConverter.ToInt32(BitConverter.GetBytes(value), 0);
        sb.Append("F ").Append(name).Append(' ').Append(bits.ToString("X8")).Append('\n');
    }
}
