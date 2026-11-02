# Cross-cutting Concerns

This document collects the behaviours that cut across modules: state, concurrency, configuration,
persistence, errors, testing and deployment.

## State ownership

| State | Owner | Scope |
|---|---|---|
| Current editor character | HTTP session via `EditorService` | Per user session |
| Saved character files | `CharacterStore` under `jrawr.characters-dir` | Shared filesystem |
| Item/buff/enchant caches | `rawr-core` static catalogs | Whole JVM |
| Model registry and model instances | `Calculations` | Whole JVM |
| Pending import | HTTP session via `PendingImportService` | Per user session |
| Optimizer/upgrade jobs | `OptimizerJobService` / `UpgradeJobService` executors | Whole JVM, bounded |
| Global calculation settings | Cookies + `GlobalSettingsInterceptor` | Per browser/user |
| User side-cars | `UserItemStore`, `SavedTalentSpecStore`, `SavedItemFilterStore`, `ItemCostStore` | Shared filesystem |

The HTTP session is the only place a live unsaved character exists. The engine has no per-request
state container; it operates on the character object it is given.

## Concurrency

The engine is not thread-safe:

- `Calculations` has a process-wide registry and current-model pointer; the registry caches one
  instance per model class.
- Model instances may keep caches/carry-over state. The web layer loads a fresh instance for each
  top-level calculation, but model-internal static state is still process-wide.
- Item, buff and enchant catalogs are static.
- Special-effect interpolation uses a shared cache.

`CalculationService` serializes calculations with a fair `ReentrantLock`. The lock is also used by
optimizer jobs, so a long optimization cannot race an editor calculation. The web application is
designed for a single JVM; do not run multiple application instances against the same in-memory
caches and expect coordinated calculations.

Parallel candidate evaluation is opt-in and restricted to models verified as deterministic under
it. When in doubt, use a single thread. Talent search is single-threaded.

## Global settings

The Settings page sets calculation-wide options such as profession-gated enchants, proc/effect
modes, combined-effect mode and threading. `GlobalSettingsInterceptor` reads the persisted values
and applies them to the engine before a request. These are process-wide statics/volatile fields,
not per-character options; character-saved options remain on the character.

## Persistence

All persistence is file-based:

- shipped game data XML is read-only at runtime;
- saved characters are original Rawr XML files;
- runtime user items, talent specs, filters and costs are separate side-car XML files;
- templates/static assets are served by Spring Boot.

Writes are synchronous and scoped to the operation that triggers them. There is no database
transaction or migration layer. A character save is a full XML serialization, not a diff.

## Errors and validation

- Controller/service validation failures generally re-render the active fragment or modal with a
  message; they do not return a raw exception page in normal use.
- Import validates the document, the model name and (where applicable) missing item ids before the
  character enters the session.
- Calculation failures are shown as a calculation error in the UI while the editor remains usable.
- Unresolved item templates are a valid state: they contribute empty stats and no sockets rather
  than breaking a calculation.
- Upload size is capped by the servlet multipart settings and by the importer's own checks.

## Testing strategy

- `rawr-core`, `rawr-data`, `rawr-models` and `rawr-web` each have JUnit 5 suites.
- Golden-master tasks compare calculation and comparison output against frozen reference data;
  they run one fixture per fresh JVM because models keep state.
- Optimizer verification checks that the Java best score meets the reference bound; parallel
  determinism is checked for the verified models.
- Web tests use Spring Boot test support and a headless browser for selected UI behaviour.
- The full testing model is described in [Testing](testing.md).

The public documentation is not generated from tests; it is checked by review.

## Build and deployment

- Java 25 toolchain; Gradle multi-module build.
- Spring Boot 4.1.1 with embedded web server.
- Production configuration is supplied through `application.properties`/environment properties:
  data directory, characters directory, side-car files, upload limits, server port.
- The application is a single-process, single-user deployment unless the surrounding environment
  provides more. Multi-user hosting would require externalizing session state and serializing or
  partitioning engine use; those are not part of the current design.

## Public/private documentation boundary

This architecture handbook is intended for publication with the source. Development decision
logs, progress notes and backlog files are internal working documents and are not part of the
public documentation set.
