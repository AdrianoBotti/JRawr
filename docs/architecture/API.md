# API

JRawr is an HTML application, not a JSON API. Most endpoints return a Thymeleaf fragment that htmx
swaps into the current page. The table below is the public controller surface; request parameters
are visible in the controller method signatures and the corresponding templates.

## Conventions

| Convention | Meaning |
|---|---|
| `GET /` | Full editor shell |
| `GET /character/...` | Page or fragment read |
| `POST /character/...` | Mutate the session character or a stored side-car |
| `GET /character/.../body` | htmx body fragment for a modal/pane |
| `POST /character/.../cancel` | Cancel a running job |
| `GET /character/.../status` | Job status as JSON/text |
| `GET /character/.../stream` | Server-sent events for job progress |
| Redirect responses | Whole-page actions typically redirect to `/` with a query flag |

The session character is an implicit input for editor endpoints. A character file is exchanged as
Rawr XML, not as a JSON representation.

## Character and files

| Method | Path | Purpose |
|---|---|---|
| GET | `/` | Editor shell |
| GET | `/character` | Redirect to the editor (legacy path) |
| GET | `/character/ratings` | Stats/ratings fragment |
| GET | `/character/import` | Import modal |
| GET | `/import` | Import page |
| POST | `/character` | Import character XML (paste/upload) |
| POST | `/character/fetch-missing` | Fetch missing item ids for a pending import |
| POST | `/character/import-pending` | Import a pending character without fetching |
| POST | `/character/cancel-pending` | Discard a pending import |
| POST | `/character/save` | Save the session character to its file |
| GET | `/character/save-as` | Save-as prompt |
| POST | `/character/save-as` | Save under a new file name |
| GET | `/character/download` | Download the current character as XML |
| GET | `/characters` | Saved-character picker |
| POST | `/character/open` | Open a saved character |
| GET/POST | `/character/new` | New-character prompt/action |
| POST | `/character/delete` | Delete a saved character |
| POST | `/character/meta` | Update name, race, class/model and professions |
| POST | `/character/calculation-options` | Persist model options |

## Gear, items and availability

| Method | Path | Purpose |
|---|---|---|
| GET | `/items` | Item picker fragment |
| GET | `/enchants` | Enchant picker fragment |
| GET | `/gems` | Gem picker fragment |
| POST | `/character/slot` | Equip an item template |
| POST | `/character/slot/instance` | Equip an exact gemmed instance |
| POST | `/character/slot/enchant` | Apply/remove an enchant |
| POST | `/character/slot/gem` | Set/remove a socket gem |
| POST | `/character/slot/remove` | Unequip a slot |
| GET | `/character/availability` | Custom item pool modal |
| GET | `/character/availability/body` | Custom item pool body |
| POST | `/character/availability/item` | Toggle an item/availability entry |
| POST | `/character/availability/exact` | Add/remove an exact gemmed entry |
| POST | `/character/availability/bulk` | Bulk pool actions |
| POST | `/character/availability/import` | Import pool entries from text |
| POST | `/character/availability/other` | Remove legacy/other entries |
| GET | `/character/availability/export` | Export pool entries as text |
| GET | `/item-tooltip` | Item tooltip fragment |

## Talents, glyphs, buffs and specs

| Method | Path | Purpose |
|---|---|---|
| POST | `/character/talents` | Set the talent string |
| POST | `/character/talents/adjust` | Learn/unlearn one talent point |
| POST | `/character/glyphs/toggle` | Toggle a glyph |
| POST | `/character/talents/reset` | Reset talents |
| POST | `/character/glyphs/reset` | Reset glyphs |
| POST | `/character/buff` | Toggle an active buff |
| POST | `/character/buffs/reset` | Reset active buffs |
| GET | `/character/specs` | Talent-spec browser modal |
| POST | `/character/specs/save` | Save the current spec |
| POST | `/character/specs/apply` | Apply a saved spec |
| POST | `/character/specs/rename` | Rename a custom spec |
| POST | `/character/specs/delete` | Delete a custom spec |
| POST | `/character/specs/toggle` | Include/exclude a spec in optimizer searches |

## Boss and model options

| Method | Path | Purpose |
|---|---|---|
| GET | `/character/boss` | Boss/encounter modal |
| POST | `/character/boss` | Save scalar boss settings |
| POST | `/character/boss/preset` | Load a canonical encounter preset |
| POST | `/character/boss/reset` | Reset boss options |
| POST | `/character/boss/targets/add` | Add a target group |
| POST | `/character/boss/targets/update` | Update a target group |
| POST | `/character/boss/targets/delete` | Delete a target group |
| POST | `/character/boss/attacks/add` | Add an attack/DoT row |
| POST | `/character/boss/attacks/update` | Update an attack/DoT row |
| POST | `/character/boss/attacks/delete` | Delete an attack/DoT row |
| GET | `/character/options` | Model-options modal |
| POST | `/character/options` | Save model options |
| POST | `/character/options/collection` | Add/select/delete a collection entry (rotation/profile) |
| POST | `/character/options/reset` | Reset model options |

## Charts and comparisons

| Method | Path | Purpose |
|---|---|---|
| GET | `/character/compare` | Chart pane for a slot |
| GET | `/character/compare/body` | Chart body (items/upgrades/enchants/talents/glyphs/buffs/gems/stat graphs) |
| POST | `/character/compare/availability` | Toggle availability from a chart row |

Chart selection, candidate counts, reference mode, sorting and filters are query/form parameters
on the chart endpoints. The chart fragment contains the rendered SVG/bar data and the row actions.

## Optimizer and upgrades

| Method | Path | Purpose |
|---|---|---|
| GET | `/character/optimize/live` | Optimize screen setup/output fragment |
| GET | `/character/optimize/output` | Restore the result output pane |
| POST | `/character/optimize` | Start an optimizer job |
| GET | `/character/optimize/status` | Job status |
| GET | `/character/optimize/stream` | Server-sent event stream |
| POST | `/character/optimize/cancel` | Cancel the job |
| POST | `/character/optimize/apply` | Apply the best result to the session character |
| POST | `/character/optimize/save-as` | Save the optimized result as a new character |
| GET | `/character/optimize/result` | Result preview |
| POST | `/character/upgrades` | Build the per-slot upgrade list |
| GET | `/character/upgrades/status` | Upgrade job status |
| POST | `/character/upgrades/cancel` | Cancel the upgrade job |
| POST | `/character/upgrades/evaluate` | Evaluate one item |

## Settings and data editors

| Method | Path | Purpose |
|---|---|---|
| GET | `/tools` | Global data-editor hub |
| GET | `/tools/item-editor` | Item editor (Add/Edit tabs) |
| POST | `/tools/item-editor/source` | Choose the item-source preset or custom URL template |
| GET | `/tools/item-editor/custom` | Custom-copy form (new or edit) |
| GET | `/tools/item-editor/edit-body` | Return from the custom form to the Edit tab |
| POST | `/tools/item-editor/custom/preview` | Build a draft preview (not cached) |
| POST | `/tools/item-editor/custom/save` | Validate and save a custom copy |
| POST | `/tools/item-editor/parse` | Parse a batch into the fetch queue |
| POST | `/tools/item-editor/fetch` | Fetch one item (or refresh a fetched item) |
| POST | `/tools/item-editor/undo` | Undo the last fetch of one item |
| GET | `/tools/item-editor/list` | Filtered Edit-tab item list |
| GET | `/tools/item-editor/detail` | One item's detail panel |
| POST | `/tools/item-editor/refresh` | Re-fetch a fetched item |
| POST | `/tools/item-editor/remove` | Remove a user-owned item |
| GET | `/about` | About dialog |
| GET | `/appearance` | Appearance dialog |
| GET | `/settings` | Settings modal |
| POST | `/settings` | Save settings |
| POST | `/settings/optimizer` | Save optimizer multithreading/thread count |
| GET | `/character/item-filters` | Item-filter modal |
| GET | `/character/item-filters/body` | Filter-tree body |
| POST | `/character/item-filters/node` | Edit a filter node |
| POST | `/character/item-filters/add` | Add a filter node |
| POST | `/character/item-filters/delete` | Delete a filter node |
| POST | `/character/item-filters/enabled` | Toggle a rule (optionally the whole subtree) |
| POST | `/character/item-filters/other` | Set the Other flag |
| POST | `/character/item-filters/types` | Edit relevant item types |
| GET | `/character/gemming-templates` | Gemming-template modal |
| GET | `/character/gemming-templates/body` | Template body |
| POST | `/character/gemming-templates/toggle` | Enable/disable a built-in template |
| POST | `/character/gemming-templates/add` | Add a custom template |
| POST | `/character/gemming-templates/create` | Create a custom template |
| POST | `/character/gemming-templates/delete` | Delete a custom template |
| POST | `/character/gemming-templates/update` | Update a template’s gems |
| POST | `/character/gemming-templates/top-gemmings` | Set chart variants per item |
| GET | `/character/item-costs` | Item-cost modal |
| GET | `/character/item-costs/body` | Cost body/search |
| POST | `/character/item-costs` | Set a cost |
| POST | `/character/item-costs/reset` | Reset costs |

## Response model

Most fragment endpoints return `text/html`. The job status endpoints return small JSON/text
payloads for the browser; the stream endpoint uses `text/event-stream`. File download returns the
character XML as an attachment. The exact fragment id and swap target are defined by the
corresponding Thymeleaf template and htmx attributes.
