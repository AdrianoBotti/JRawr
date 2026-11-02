// RelevantItemsProbe — emits the expected relevant item(-instance) lists produced by the shipped
// Rawr.Base.dll for the character fixtures (part of the member-level completeness audit,
// tools/core-completeness). Runs against the prebuilt legacy assemblies under Mono; no network.
//
// Build/run: tools/core-completeness/probe/run.sh (writes
// jrawr/rawr-golden/src/test/resources/core-completeness/relevant-items.txt).
// The Java side (RelevantItemInstancesGoldenTest) reproduces the same character state and compares
// the sorted gemmed ids / item ids.
using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using Rawr;
using Rawr.Mage;

class RelevantItemsProbe
{
    static int Main(string[] args)
    {
        if (args.Length < 1)
        {
            Console.Error.WriteLine("usage: RelevantItemsProbe <fixture.xml> [<fixture.xml> ...]");
            return 2;
        }
        ItemCache.Load();
        Calculations.LoadModel(typeof(CalculationsMage));

        foreach (string fixturePath in args)
        {
            string fixture = System.IO.Path.GetFileNameWithoutExtension(fixturePath);
            Console.WriteLine("fixture " + fixture);
            Character c = Character.Load(fixturePath);
            // The C# UI applies the saved filter state right after opening a character
            // (FormMain.LoadCharacterIntoForm) and refreshes the item cache when a flag changed;
            // rawr-data/CharacterFixtureLoader does the same.
            if (c.LoadItemFilterEnabledOverride()) ItemCache.OnItemsChanged();
            c.RecalculateSetBonuses();

            // The global pool the per-slot lists are built from.
            Console.WriteLine("relevant " + ItemCache.RelevantItems.Length);
            foreach (string ri in ItemCache.RelevantItems
                .Select(x => x.Id.ToString(CultureInfo.InvariantCulture))
                .OrderBy(x => x, StringComparer.Ordinal))
                Console.WriteLine("relevantid " + ri);

            // Deterministic extra state the Java side reproduces: blacksmithing socket enabled and
            // a custom item instance plus available-item entries derived from the head item.
            c.WaistBlacksmithingSocketEnabled = true;
            string headId = c.Head != null ? c.Head.Id.ToString(CultureInfo.InvariantCulture) : "0";
            ItemInstance custom = new ItemInstance(int.Parse(headId), 0, 0, 0, 0);
            custom.ForceDisplay = true;
            c.CustomItemInstances.Add(custom);
            c.AvailableItems.Add(headId);
            c.AvailableItems.Add(headId + ".*.*.*");
            c.AvailableItems.Add(headId + ".0.0.0.0");
            c.AvailableItems.Add(headId + ".0.0.0.*");

            CharacterSlot[] slots = { CharacterSlot.Head, CharacterSlot.Waist, CharacterSlot.Finger1,
                CharacterSlot.MainHand, CharacterSlot.OffHand, CharacterSlot.Gems };
            foreach (CharacterSlot slot in slots)
            {
                List<ItemInstance> instances = c.GetRelevantItemInstances(slot);
                List<string> ids = instances.Select(x => x.GemmedId)
                    .OrderBy(x => x, StringComparer.Ordinal).ToList();
                Console.WriteLine("instances " + slot + " " + ids.Count);
                foreach (string id in ids) Console.WriteLine("id " + id);
            }

            ItemSlot[] colours = { ItemSlot.None, ItemSlot.Red, ItemSlot.Yellow, ItemSlot.Blue };
            foreach (ItemSlot colour in colours)
            {
                c.ClearRelevantGems();
                List<Item> items = c.GetRelevantItems(CharacterSlot.Gems, colour);
                List<string> ids = items.Select(x => x.Id.ToString(CultureInfo.InvariantCulture))
                    .OrderBy(x => x, StringComparer.Ordinal).ToList();
                Console.WriteLine("gems " + colour + " " + ids.Count);
                foreach (string id in ids) Console.WriteLine("id " + id);
            }

            List<Item> headItems = c.GetRelevantItems(CharacterSlot.Head);
            List<string> headIds = headItems.Select(x => x.Id.ToString(CultureInfo.InvariantCulture))
                .OrderBy(x => x, StringComparer.Ordinal).ToList();
            Console.WriteLine("items Head " + headIds.Count);
            foreach (string id in headIds) Console.WriteLine("id " + id);
        }
        return 0;
    }
}
