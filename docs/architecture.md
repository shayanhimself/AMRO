# Architecture

An overview of how the app is put together: what the modules are, what each one holds, and how data
and events move between them.

The app has no backend of its own. TMDB is the only remote, reached with a read access token, and
everything the app shows is served from a local database that the network layer fills. Offline is
not a mode the app enters: it is what a failed request means.

## Modules

> Mermaid diagrams do not render in Android Studio. Read this on GitHub or in VS Code.

```mermaid
graph TD
    app[":app"]

    discover[":feature:discover"]
    detail[":feature:detail"]

    ui[":core:ui"]
    data[":core:data"]
    network[":core:network"]
    database[":core:database"]
    model[":core:model"]

    subgraph TESTONLY["test-only"]
        coreTest[":core:testing"]
    end

    app --> discover
    app --> detail
    app --> ui
    app --> data
    app --> network

    discover --> ui
    discover --> data
    detail --> ui
    detail --> data

    ui --> model

    data --> network
    data --> database
    data --> model
    network --> model
    database --> model

    discover -.-> coreTest
    detail -.-> coreTest
    data -.-> coreTest
```

| Module | Holds                                                                                                                                                                   |
|---|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `:core:model` | The types every other module speaks in. Types only, pure Kotlin, no Android dependency                                                                                  |
| `:core:network` | `MovieRemoteDataSource`, the api-neutral contract the data layer depends on, and its TMDB implementation: Ktor client configuration, endpoints, wire DTOs and mappings. A source narrows and orders its own catalogue |
| `:core:database` | The Room database, entities, and DAOs. The movies, the lists one selection produced, and where each of those resumes                                                    |
| `:core:data` | Repository interfaces and their implementations, the `Pager` and its `RemoteMediator`, and the fetch policy. Names no source                                            |
| `:core:ui` | Theme, design system tokens and components, generic strings, reusable UI components and helpers                                                                          |
| `:feature:discover` | The movie list, the filter sheet, and their view model                                                                                                                  |
| `:feature:detail` | The movie detail screen and its view model                                                                                                                              |
| `:app` | `MainActivity`, the navigator and the `NavDisplay` host, the application class, and the Hilt root                                                                       |
| `:core:testing` | Fakes, fixtures, and test helpers other modules' tests reuse. Never a production dependency                                                                             |

Dependency rules (hard):

- A feature never depends on another feature: navigation between them goes through the `:app` graph.
- The components of `:core:ui` take primitives, so a feature unpacks a model type at the call site.
- `:core:model` depends on nothing and never imports `android.*`.
- `:app` depends on everything. It is the only module that sees every feature.
- Modules speak the domain model language. DTO types never leave `:core:network`, and Room entity types never leave `:core:database`.
- A repository depends on a `MovieRemoteDataSource` interface, never on a named API (ex: TMBD). No API name
  ever leaves `:core:network`.
- `:core:testing` is only a test dependency, never a production one.

## Layers

Two layers, plus a domain layer the project does not currently need. Dependencies point one way.

> Mermaid diagrams do not render in Android Studio. Read this on GitHub or in VS Code.

```mermaid
graph TD
    subgraph UILAYER["UI layer (:feature:*, :app)"]
        Screen["Screen composable, stateless"]
        Route["Route, collects state and passes lambdas"]
        VM["ViewModel, owns UiState and the selection"]
    end

    subgraph DATALAYER["Data layer (:core:data, :core:database)"]
        MoviesRepo["MoviesRepository<br/>Pager + RemoteMediator"]
        DetailRepo["MovieDetailRepository"]
        Room[("MovieLocalDataSource<br/>Room: movies, movie_details,<br/>query_results, query_keys")]
    end

    subgraph SOURCE["Remote source (:core:network)"]
        Contract["MovieRemoteDataSource, interface"]
        Tmdb["TmdbRemoteDataSource, Ktor"]
    end

    API(["TMDB API"])

    Screen -->|events| Route
    Route -->|events| VM
    Route -->|UiState| Screen
    VM -->|StateFlow| Route

    VM -->|MovieSelection| MoviesRepo
    MoviesRepo -->|"Flow&lt;PagingData&gt;"| VM
    VM --> DetailRepo

    MoviesRepo --> Room
    MoviesRepo --> Contract
    DetailRepo --> Room
    DetailRepo --> Contract
    Tmdb -.->|implements| Contract
    Tmdb -->|"GET, Bearer token"| API
```

- **Unidirectional data flow.** State travels one way, from the database through the repository and
  the view model to the screen, and events travel the other as method calls. A screen receives one
  immutable `UiState` and emits lambdas. Every outcome, errors included, is folded into that state,
  so nothing is pushed to the UI through an event channel.
- **Repositories are the sole entry to the data layer.** No view model touches a DAO or a data
  source.
- **A repository speaks to a data source interface.**
- **Room is the single source of truth.** A repository exposes its data as a `Flow` off Room and
  never returns network results directly. The network path only writes, and for the paged list that
  writer is the `RemoteMediator`. A cached list therefore renders before any request completes.
- **Data crossing a layer boundary is immutable**, and mutation happens only inside the owner.

## Navigation and the adaptive layout

`AmroNavigator` owns the back stack, and is the only thing that
mutates it. Features receive lambdas and know nothing about navigation.

> Mermaid diagrams do not render in Android Studio. Read this on GitHub or in VS Code.

```mermaid
graph LR
    Nav["AmroNavigator, owns the back stack"] --> Display["NavDisplay"]
    Display --> Scene["List-detail scene strategy"]
    Scene --> Routes["DiscoverRoute<br/>DetailRoute"]
```

One graph plus the scene strategy gives a compact window a stack of screens and an expanded window
two panes, from the same keys: at expanded width both keys resolve into panes that are shown at
once, so selecting a movie replaces the detail pane instead of navigating. Nothing in the graph or
the screens reads the device type or the orientation.
