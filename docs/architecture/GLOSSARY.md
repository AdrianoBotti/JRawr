# Glossary

| Term | Meaning in JRawr |
|---|---|
| **Character** | The aggregate root of a build: identity, equipped slots, buffs, talents, professions, boss and model options. |
| **Model** | A simulation of one class/spec, such as `Mage`, `Cat` or `Warlock`. It implements `CalculationsBase` and lives in `rawr-models`. |
| **Calculation** | One run of a model for a character. It returns a `CharacterCalculationsBase` with an overall score, sub-points and display values. |
| **Overall points / rating** | The single score the optimizer maximizes. Its unit is model-defined (DPS, HPS, threat, weighted tank score). |
| **Sub-points** | The model's breakdown of the overall score into named dimensions (for example damage schools or threat/survival components). |
| **Display values** | The label-to-text map a model returns for the stats panel; values may include tooltip suffixes. |
| **Item** | An item template from `ItemCache`: base stats, slot, sockets, set, class/faction restrictions and special effects. |
| **ItemInstance** | An item template plus gem and enchant ids, and the equipment slot context. It can be unresolved when an item id is missing from the cache. |
| **Gemmed id** | The compact slot representation `itemId.gem1Id.gem2Id.gem3Id.enchantId`. |
| **AvailableItems** | The character's optimizer pool: bare ids, exact gemmed ids, wildcard entries and legacy enchant tokens. |
| **Stats** | The engine's numeric container for additive, multiplicative and inverse-multiplicative stats plus special effects. |
| **Special effect** | A proc/trigger/use effect attached to an item, buff, enchant or set; uptime is computed from the character context. |
| **Calculation options** | Per-model settings stored on the character (fight length, rotations, priorities, custom spell mix, etc.). |
| **Boss options** | Encounter definition: target level, resistances, attacks, DoTs, target groups, timers and toggles. |
| **Buff** | A raid/self buff or debuff object with stats, stacking rules, groups and conflicts. |
| **Talent string** | The compact per-class talent/glyph array serialization used by Rawr character XML. |
| **Comparison** | A model result for one candidate item (or buff/talent/gem) relative to the equipped build; used by charts and the optimizer UI. |
| **Optimizer** | Search over item/gem/enchant/buff/talent candidates to maximize a model objective under constraints. |
| **Job** | A bounded background task for optimizer or upgrade work, with status, progress, cancellation and a result. |
| **Fragment** | A Thymeleaf HTML partial returned to an htmx request and swapped into the page. |
| **OOB** | htmx “out-of-band” swap: a response fragment updates another part of the page (for example the stats panel) in the same request. |
| **Side-car** | A user-editable XML file next to saved characters, holding runtime items, talent specs, filters or costs. |
| **Data directory** | The directory containing the shipped XML caches; configured by `jrawr.data-dir`. |
| **Characters directory** | The directory containing saved Rawr character XML; configured by `jrawr.characters-dir`. |
| **Golden master** | A frozen reference result from the original implementation, used to verify the Java port. |
| **Model relevance** | Whether an item, buff or enchant can affect a model; used to keep candidate pools and charts focused. |
