# talent-meta-gen

Generates `jrawr/rawr-web/src/main/resources/talent-meta.json` for the WoW-style talent planner.

The Java `*Talents` classes keep the layout (index/name/tree/row/column/prerequisite) and the
`@GlyphDataAttribute` flags, but the port deliberately omitted the C# attributes' UI-only icon and
per-rank description strings. This tool joins the **active Java annotations** with the legacy
`legacy/RAWR_WotLK_3.3.5a/Rawr.Base/ClassTalents.cs` / `ClassGlyphs.cs` attributes by class, index
and name. C# entries that are commented out, and the unused `WarriorTalentsCata` section, are ignored;
a missing match is reported as a warning.

```bash
python3 tools/talent-meta-gen/generate.py
python3 tools/talent-meta-gen/generate.py --check   # fail when the committed JSON is stale
```

The web app loads the artifact once; if it is absent the talent view still renders names, ranks,
layout and glyphs, just without icons/effect text.
