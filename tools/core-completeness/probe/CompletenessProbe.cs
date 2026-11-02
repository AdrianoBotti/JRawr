// CompletenessProbe — emits the expected output for the members closed by the Rawr.Base
// completeness audit (tools/core-completeness). Runs against the prebuilt legacy Rawr.Base.dll.
//
// Build (run.sh does it): mcs -r:<Data>/Rawr.Base.dll -out:bin/CompletenessProbe.exe CompletenessProbe.cs
// Run: MONO_PATH=<Data> mono bin/CompletenessProbe.exe > <out>
using System;
using System.Collections.Generic;
using System.Globalization;
using Rawr;

class CompletenessProbe
{
    static void Main()
    {
        var ic = CultureInfo.InvariantCulture;

        Console.WriteLine("# SpecialFunction.Normal");
        double[] xs = { -8.0, -3.0, -1.5, -1.0, -0.75, -0.5, -0.25, 0.0, 0.25, 0.5, 0.75, 1.0, 1.5, 3.0, 8.0 };
        foreach (double x in xs)
            Console.WriteLine("normal " + x.ToString("R", ic) + " " + SpecialFunction.Normal(x).ToString("R", ic));

        Console.WriteLine("# Stats.StatNames");
        foreach (string name in Stats.StatNames)
            Console.WriteLine("statname " + name.Replace("\r", "\\r").Replace("\n", "\\n"));

        Console.WriteLine("# Item.GetItemSlotByCharacterSlot");
        foreach (CharacterSlot slot in Enum.GetValues(typeof(CharacterSlot)))
            Console.WriteLine("itemslot " + slot + " " + Item.GetItemSlotByCharacterSlot(slot));

        Console.WriteLine("# Item.DefaultSlotMap");
        foreach (KeyValuePair<ItemSlot, CharacterSlot> kv in Item.DefaultSlotMap)
            Console.WriteLine("slotmap " + kv.Key + " " + kv.Value);

        Console.WriteLine("# Character.GetCharacterSlotFromId");
        int[] ids = { -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 101, 102, 103 };
        foreach (int id in ids)
            Console.WriteLine("slotid " + id + " " + Character.GetCharacterSlotFromId(id));
    }
}
