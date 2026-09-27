---
name: architecture
description: This project's app architecture and conventions. Use when building or
  modifying any feature — adding a screen, ViewModel, UiState, repository, data source,
  use case, Room entity/DAO, or a new module; when deciding which module or layer code
  belongs in; or when wiring Hilt.
metadata:
  keywords:
  - architecture
  - feature
  - module
  - layer
  - ViewModel
  - UiState
  - repository
  - data source
  - UseCase
  - StateFlow
  - Hilt
  - Room
  - Ktor
  - architecture doc
---

Conventions here are binding. They implement Google's architecture guidance.

`docs/architecture.md` is the overview: the module map, the dependency rules, and how the
trending fetch, the freshness rule and the genre mapping work. Read it for the shape of the
app. This skill is how to write code that fits it.

## Layering

Two mandatory layers plus one optional, dependencies point one way only:

```
UI layer (:feature:*, :app)  →  [domain layer (optional)]  →  data layer (:core:data)
```

- **Repositories are the sole entry to the data layer.** ViewModels and use cases never
  touch a DAO or a data source directly.
- **Room is the single source of truth.** A repository exposes a `Flow` off Room; the
  network path only writes. Nothing above the data layer renders a network result.
- **Unidirectional data flow:** state flows down (immutable), events flow up (method
  calls). Applies inside every layer and between them.
- **Data exposed by any layer is immutable.** Mutation happens only inside the owner.

## Which module

| Putting in | Goes to |
|---|---|
| A type the whole app speaks in, no Android import | `:core:model` |
| An endpoint, a wire DTO, a DTO-to-model mapper, a provider's own facts | `:core:network` |
| An entity, a DAO, a migration, a relation query | `:core:database` |
| A repository, a fetch policy, a freshness rule | `:core:data` |
| A theme token, a `Ds*` component, a generic string | `:core:ui` |
| A screen, its ViewModel, its UiState, its copy, the rules it selects and orders with | `:feature:trending`, `:feature:detail` |
| A nav key, the navigator, the Hilt root, `MainActivity` | `:app` |
| A fake, a fixture, a test helper | `:core:testing` |

The dependency rules in `docs/architecture.md` are hard. A build file declaring a
dependency they forbid is wrong even when it compiles.

Two of them decide most placement questions:

- **Ktor types never leave `:core:network`, Room types never leave `:core:database`.** A
  repository returns model types or a `DataError`.
- **No user-facing copy below the UI layer.** The data layer returns typed errors and a
  feature maps each to a string resource it owns. The test is whether a translator would
  ever touch the string. Untranslatable identifiers stay on the model.

## Non-negotiables

- No ViewModel → UI events (no SharedFlow/Channel event bus). Fold every event outcome
  into `UiState`; UI acknowledges consumption via a ViewModel method.
- No async work in ViewModel `init` blocks or constructors.
- Navigation 2 (`navigation-compose`) is prohibited — Nav 3 only.
- No MockK or any mocking library. Fakes and real objects only (Room in-memory, Ktor
  `MockEngine`, a fake remote source for device tests).
- Every type is main-safe; the data layer moves work off the main thread.
- Anything decided against the current time takes an injected `Clock`.
- TDD (red → green → refactor) for every step.

## Naming

| Thing | Pattern | Example |
|---|---|---|
| Repository | `XRepository` (interface) | `TrendingRepository` |
| Repository impl | `DefaultXRepository` | `DefaultTrendingRepository` |
| Test double | `FakeX` | `FakeTrendingRepository` |
| Data source | `XRemoteDataSource` / `XLocalDataSource` | `MovieRemoteDataSource` |
| Provider implementation | `<Provider>XRemoteDataSource` | `TmdbRemoteDataSource` |
| Use case | `VerbNounUseCase`, `operator fun invoke` | `SortMoviesUseCase` |
| Screen state | `ScreenUiState` | `TrendingUiState` |
| ViewModel event | `on` + verb + target | `onToggleGenre`, `onRefresh` |
| Flow accessor | `getXFlow()` | `getTrendingMoviesFlow()` |
| Mapper | extension `X.toY()`, in `YMapper.kt` | `MovieDto.toMovie()` |

**Domain vocabulary.** `Movie` is the app's own type; `MovieDetail` is the fuller record
behind one. `Genre` is the app's genre, never a provider's, and a provider's genre reaches
it through that provider's alias table. `DataError` is the sealed failure every repository
returns.

**No provider's name above `:core:network`.** `Tmdb` appears in that module and nowhere
above it. Which source answers `MovieRemoteDataSource` is decided by that module's own
`RemoteSourceModule`, the one public Hilt module it has, so a device test uninstalls that
binding and puts a fake behind the contract. A repository depends on `MovieRemoteDataSource`.

## Building a feature end to end

Order matters: data layer first, UI last. TDD at every step.

1. **Read the spec** (`docs/superpowers/specs/NNN-*.md`) — data model, acceptance criteria,
   journeys.
2. **Model** (`:core:model`): the types the feature speaks in.
3. **Data layer**: Room entities + DAO in `:core:database`, the endpoint and its mapper in
   `:core:network`, then the repository interface and `Default` impl in `:core:data`. Write
   `FakeXRepository` in `:core:testing` alongside.
   → [references/data-layer.md](references/data-layer.md)
4. **Domain layer** — only if logic is reused across ViewModels or a ViewModel is getting
   complex. Usually skip. → [references/domain-layer.md](references/domain-layer.md)
5. **ViewModel + UiState** (`:feature:*`): define `XUiState`, produce it with the `stateIn`
   recipe, handle events as methods. → [references/ui-layer.md](references/ui-layer.md)
6. **Screen composable**: stateless, takes `uiState` + event lambdas. Compose from
   `:core:ui` tokens and catalog components, translate the mockup → `design-system` skill.
   Adaptive from day one → `adaptive` skill; insets → `edge-to-edge` skill.
7. **Navigation**: typed key plus an `entryProvider` entry in the `:app` graph →
   `navigation-3` skill.
8. **Hilt wiring**: a module in the owning feature, or `:app` when cross-feature.
9. **Tests**: mapper and repository tests against fixtures, DAO tests on in-memory Room,
   ViewModel tests on fakes, Compose UI tests → `testing-setup` skill and `docs/testing.md`.
   Every public composable also ships colocated previews, screenshot-tested through
   `:core:ui`'s infra.
10. **Journeys**: one per acceptance criterion → `journeys` skill.
11. **Architecture doc**: sync `docs/architecture.md` if the shape changed. Keep it an
    overview; detail belongs in the spec or in this skill.

## References

- [references/ui-layer.md](references/ui-layer.md) — UiState modeling, state production
  recipe, ViewModel rules, state holder choice
- [references/data-layer.md](references/data-layer.md) — repositories, data sources, Room,
  Ktor, threading
- [references/domain-layer.md](references/domain-layer.md) — use cases
- Google source docs these distill — fetch latest via `android docs fetch <uri>`:
  - `kb://android/topic/architecture/index`
  - `kb://android/topic/architecture/recommendations`
  - `kb://android/topic/architecture/ui-layer/index`
  - `kb://android/topic/architecture/data-layer/index`
  - `kb://android/topic/architecture/domain-layer`
  - `kb://android/topic/libraries/architecture/viewmodel/index`
  - `kb://android/develop/ui/compose/architecture`
  - `kb://android/kotlin/flow/stateflow-and-sharedflow`
