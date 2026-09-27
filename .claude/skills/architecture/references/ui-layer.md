# UI layer

ViewModels and screen composables live in `:feature:*`. Reusable design-system composables
live in `:core:ui`. The navigator, the nav keys and the `NavDisplay` host live in `:app`.

## UiState

One immutable type per screen, named `ScreenUiState`, exposed as a single
`val uiState: StateFlow<ScreenUiState>`.

A screen whose states are mutually exclusive models them as a sealed interface:

```kotlin
sealed interface MovieDetailUiState {
    data object Loading : MovieDetailUiState
    data class Loaded(val movie: MovieDetailUiModel) : MovieDetailUiState
    data class Error(@StringRes val messageRes: Int) : MovieDetailUiState
}
```

A screen whose states combine models them as one data class:

```kotlin
data class TrendingUiState(
    val movies: List<MovieUiModel> = emptyList(),
    val filter: MovieFilter = MovieFilter.None,
    val sort: SortSelection = SortSelection.Default,
    val isRefreshing: Boolean = false,
    val notice: Notice? = null,
)
```

- All fields immutable (`val`, immutable collections).
- Errors are data in state, never exceptions reaching the UI.
- Derive, don't duplicate: `val TrendingUiState.isEmptyFromFilter get() = ...`
- Multiple `StateFlow` properties are acceptable only for genuinely unrelated data updating
  at very different rates. Default is one.
- A cached list rendering behind a failed refresh is one state carrying a notice, not an
  error state. A screen with nothing to show is the error state.

Map model types to UI models in the ViewModel when the screen needs a different shape
(formatted runtime, grouped currency, a localised date). Skip the mapping when the model
already fits. A field with no value is absent from the UI model, so the composable branches
on `null` rather than on a sentinel.

## State production recipe

Flows from the data layer → `combine` → `stateIn`:

```kotlin
private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val trendingRepository: TrendingRepository,
    private val genreRepository: GenreRepository,
) : ViewModel() {

    private val selection = MutableStateFlow(TrendingSelection())

    val uiState: StateFlow<TrendingUiState> = combine(
        trendingRepository.getTrendingMoviesFlow(),
        genreRepository.getGenresFlow(),
        selection,
    ) { movies, genres, selection ->
        TrendingUiState(
            movies = movies.filter(selection.filter).sortedBy(selection.sort).map { it.toUiModel() },
            filter = selection.filter,
            sort = selection.sort,
            isRefreshing = selection.isRefreshing,
            notice = selection.notice,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = TrendingUiState(),
    )
}
```

Rules:

- **Flow inputs:** `combine(...)` + `stateIn(viewModelScope, WhileSubscribed(...), initial)`.
  The timeout stops upstream collection shortly after the UI disappears and survives a
  configuration change, which is what keeps filter, sort and scroll position through a
  rotation.
- **One-shot inputs only** (no upstream `Flow`): a private `MutableStateFlow` holds the whole
  UiState, updated with `_uiState.update { it.copy(...) }` and exposed via `asStateFlow()`.
  No `combine`.
- **Mixed:** UiState comes out of `combine`, so ViewModel-local results go into a private
  `MutableStateFlow` holding only that slice, fed into `combine` as one more input.
- Never launch async work in `init` — the `stateIn` pipeline starts lazily on first
  collection.
- ViewModel work must be main-safe; long-running work belongs in the data layer. Filter and
  sort are pure functions over an already-fetched list, so they run where the state is
  produced.

## Events: no ViewModel → UI channel

`SharedFlow` and `Channel` events from ViewModel to UI are prohibited: an emission while the
UI is not collecting (configuration change, background) is dropped silently. State cannot be
lost, because the UI resubscribes and reads the current value.

The outcome of an event becomes state, and the UI acknowledges consumption:

```kotlin
fun onRefresh() = viewModelScope.launch {
    selection.update { it.copy(isRefreshing = true) }
    val result = trendingRepository.refresh()
    selection.update { it.copy(isRefreshing = false, notice = result.toNoticeOrNull()) }
}

fun onNoticeShown() = selection.update { it.copy(notice = null) }
```

An event function is named `on` + verb + target, the same form the screen's own parameters take,
so a lambda and the function bound to it read the same at the call site.

Navigation triggers follow the same rule: a state field the UI observes and translates into
a navigation lambda. Opening the IMDB row is an `Intent` the composable fires from a state
field holding the URL, never an event emitted by the ViewModel.

## ViewModel rules

- Screen-level only — one ViewModel per navigation destination. Never for reusable
  components (the filter sheet, a rating row): those take hoisted state and lambdas.
- Constructor injection via Hilt (`@HiltViewModel`). Dependencies are repositories or use
  cases — never a DAO or a data source.
- No `Context`, `Resources`, `Activity`, or any lifecycle type inside. No `AndroidViewModel`.
  String resolution happens in the composable, or `@StringRes` ids travel through UiState.
- Nav arguments arrive as the typed Nav 3 key through Hilt assisted injection. Entry-side
  wiring: `navigation-3` skill. `SavedStateHandle` only for process-death restoration.
- Never pass a ViewModel instance down to child composables. Pass state values and lambdas:

```kotlin
@Composable
fun TrendingRoute(viewModel: TrendingViewModel, onMovieClick: (MovieId) -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TrendingScreen(
        uiState = uiState,
        onMovieClick = onMovieClick,
        onRefresh = viewModel::refresh,
        onFilterChange = viewModel::selectGenres,
    )
}
```

- Always collect with `collectAsStateWithLifecycle()` — never `collectAsState()` or a raw
  `collect` in a `LaunchedEffect`.

## State holder choice

| | ViewModel | Plain `@Stable` class |
|---|---|---|
| Scope | Screen / nav destination | Reusable UI component or app shell |
| Logic | Business logic (calls repositories) | UI logic only (scroll, sheet visibility) |
| Survives recreation | Yes | No — `remember`, with `rememberSaveable` for what must persist |
| Android deps | Forbidden | Allowed (same lifecycle as UI) |

```kotlin
@Stable
class FilterSheetState(private val sheetState: SheetState) { /* UI logic */ }

@Composable
fun rememberFilterSheetState(
    sheetState: SheetState = rememberModalBottomSheetState(),
): FilterSheetState = remember(sheetState) { FilterSheetState(sheetState) }
```

Compounding rules:

- Business logic always applies before UI logic in the state pipeline.
- A plain holder never receives a ViewModel instance — pass only the fields and lambdas it
  needs.
- A screen-level holder never depends on another screen-level holder.
- Simple UI logic stays inline in the composable; extract a holder only when it hurts
  readability.

## StateFlow vs SharedFlow

`StateFlow` for all state — always has a value, replays the latest, conflates. `SharedFlow`
in the UI layer: never.

## Testing

- Unit test ViewModels with the fakes from `:core:testing`; assert on `uiState.value`.
- A `stateIn(WhileSubscribed)` flow needs a collector before `value` updates — collect in a
  `backgroundScope` (see the `testing-setup` skill).
- The stateless screen overload is the Compose-test and preview target. Every public
  composable ships colocated previews, one per distinct visual state, each wrapped in
  `AmroTheme`.
