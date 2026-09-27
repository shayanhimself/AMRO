# 006: Trending screen

`:feature:trending`. The launch screen: a hundred movies off Room, narrowed and ordered by a
selection the user makes in one sheet, in every state the data can put it in.

Behaviour is owned by the product brief, technology by the tech stack spec, the module map and
dependency rules by the architecture doc, the test layers by the testing doc. Where this spec and
those disagree, those win.

---

## 1. What is already settled

- **Two reads and one refresh.** `getTrendingMoviesFlow(): Flow<List<Movie>>` and
  `suspend fun refresh(): DataError?`, where null means the write happened.
- **A read cannot fail and a refresh writes nothing when it fails.** So the cache on screen is
  never destroyed by a failure.
- **The repository returns the cause and never decides whether it is shown.** This screen decides.
- **`applyFilter` and `applySort` already exist** in this module, as `internal` pure extension
  functions over `List<Movie>`. Their meaning is settled. This spec owns where they run.
- **`MovieSort`, `SortKey`, `SortDirection` and `MovieSort.DEFAULT`** are this module's, and `internal`.
- **No layer below this one can produce a partial list**, so the partial page failure state is
  unreachable and is not built.

---

## 2. The state

```kotlin
data class TrendingUiState(
    val content: TrendingContent,
    val filter: FilterUiState,
    val activeSelectionCount: Int,
    val notice: NoticeUiState?,
    val isRefreshing: Boolean,
    val selection: SelectionUiState,
)

data class SelectionUiState(
    val genreNames: ImmutableSet<String>,
    val sort: MovieSort,
)

sealed interface TrendingContent {
    data object Skeleton : TrendingContent

    data class Movies(val rows: ImmutableList<MovieRowUiState>) : TrendingContent

    data object EmptyFromFilter : TrendingContent

    data class Error(val glyph: String, val title: AmroText) : TrendingContent
}

data class MovieRowUiState(
    val movieId: String,
    val title: String,
    val posterUrl: String?,
    val genres: AmroText,
)

data class NoticeUiState(val glyph: String, val message: AmroText)

data class FilterUiState(
    val genres: ImmutableList<ChipUiState<String>>,
    val sortKeys: ImmutableList<ChipUiState<SortKey>>,
    val directions: ImmutableList<ChipUiState<SortDirection>>,
    val shownCount: Int,
    val totalCount: Int,
)

data class ChipUiState<out T>(
    val value: T,
    val label: AmroText,
    val isSelected: Boolean,
    val glyph: String? = null,
)
```

**No type the data layer owns reaches the screen.** The state carries words, addresses and counts,
so a composable resolves what it is handed and never picks it. Every `when` over a `Genre` or a
`DataError` lives behind the state instead.

**Every word travels as an `AmroText`.** It is `:core:ui`'s wrapper over text that is named rather
than resolved: a resource, a plural, a joined list, or a `Raw` string already written out. The
screen calls `resolve()` and nothing else. It resolves at composition, where the configuration is
current, which a resolved string in the state cannot be: a view model outlives the activity, so a
locale change would leave the state holding words in the language the user just left.

**One `ChipUiState` for all three sheet sections.** A genre, a key and a direction differ in what a
click reports and in nothing the sheet draws, so the sheet renders three lists of one type. A
genre's `value` is the enum's own name, which is what the selection is already saved as; a key and
a direction are this module's types, so they travel as themselves.

**One data class carrying a sealed body.** The screen has chrome that outlives every outcome, and
one region that is exactly one thing at a time. A single flat class would leave the exclusion
between a skeleton, a list and an error as a convention across four nullable fields. A single sealed
hierarchy would put the selection, the sort and the counts on every branch, because the app bar and
its badge render in all of them. The split gives the compiler the part that is exclusive and writes
the part that is shared once.

**Six of the brief's seven states, over four content cases.**

| State | `content` | `notice` |
|---|---|---|
| First load | `Skeleton` | null |
| Loaded | `Movies` | null |
| Empty from filter | `EmptyFromFilter` | null |
| Empty from API | `Error`, read from `EmptyResponse` | null |
| Error, no cache | `Error`, read from `NoConnectivity` or `Server` | null |
| Error, cache present | `Movies` | a `NoticeUiState`, read from the cause |

Empty from API is not a case of its own. It is a cause, and each cause is already read as its own
glyph and sentence because the brief asks for three distinguishable messages. A fifth content case
would be a second way to say `EmptyResponse`.

**`activeSelectionCount` is computed in one place.** The badge is the only place the screen
states that anything is narrowed, so the rule that produces it is one expression in the mapper. A
sort away from `MovieSort.DEFAULT` counts one, which is what stops a changed order from being
silent.

**A row is a `MovieRowUiState`.** It carries the title, the poster address, the genres already
joined into the one line the row reads, and the movie's id, which is both what a click reports and
what the lazy list holds the row by. What a row draws and what a click reports are then decided
once, above the composable that does either.

**`selection` states what the list is narrowed and ordered by, beside the rows it produced.**
The rows say what the list holds and the selection says why, which is the pair the screen needs to
tell a reordered list from a longer one. Genres travel as their names, so no type the data layer
owns reaches the screen, and as a set, so selecting a genre and dropping it again lands back on the
selection it started from.

**Sheet visibility is not in this state.** Opening a sheet is UI logic, so it is `rememberSaveable`
in the screen, where it survives both a rotation and process death without a field, an event or an
acknowledgement travelling through the view model.

---

## 3. Producing the state

```kotlin
internal data class RefreshState(
    val isRefreshing: Boolean = false,
    val error: DataError? = null,
)

@HiltViewModel
internal class TrendingViewModel @Inject constructor(
    private val trendingMoviesRepository: TrendingMoviesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val genreNames = savedStateHandle.getMutableStateFlow(KEY_GENRES, ArrayList<String>())
    private val sort = savedStateHandle.getMutableStateFlow(KEY_SORT, MovieSort.DEFAULT)
    private val refreshState = MutableStateFlow(RefreshState())
    private var hasRefreshedOnLaunch = false

    val uiState: StateFlow<TrendingUiState> = ...
}
```

`uiState` combines the trending flow, the two saved keys and `refreshState`, and lands in
`stateIn(viewModelScope, WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS), TrendingUiState())`. The
timeout constant is `:core:ui`'s, so this screen collects the way every other one does.

The combine hands its four values to one mapper, which is the whole policy:

```kotlin
internal fun List<Movie>.toTrendingUiState(
    genreNames: List<String>,
    sort: MovieSort,
    refresh: RefreshState,
): TrendingUiState
```

It decodes the saved genre names, narrows and orders the cache, and reads the result as the state
above. Picking the body is its own step:

```kotlin
val content = when {
    cacheIsEmpty && error != null -> error.toErrorContent()
    cacheIsEmpty -> TrendingContent.Skeleton
    shown.isEmpty() -> TrendingContent.EmptyFromFilter
    else -> TrendingContent.Movies(shown.map { it.toRow() }.toImmutableList())
}
```

**The mapper is where every reading of a domain type happens.** A cause becomes a glyph and a
resource id, a genre becomes a label, a movie becomes a row. One file holds all of it, so the
screen has none of it and a test can read the whole mapping without a composition.

**It runs on the main thread, with no `flowOn`.** Filtering, ordering and mapping are pure over at
most a hundred items, and the state they need is the state the view model holds. A dispatcher hop
would cost a thread switch on every chip tap.

**`EmptyFromFilter` cannot fire on an empty selection.** With a non-empty cache and nothing
selected, `shown` is `cached`. The exclusion is a property of the functions, not a condition anyone
maintains.

**An empty cache with no error is the skeleton, whatever a refresh is doing.** The screen opens on
`TrendingUiState()`, whose content is `Skeleton`, and leaves it when rows arrive or a refresh
fails. Nothing tracks whether a first attempt has been made, because nothing renders differently
before and during one.

**`notice` is read from the cause when there are rows to keep, and null otherwise.** The rule that separates a
bar from a full screen is whether content would otherwise assert something false, which is a fact
about what is rendered, and this is the layer that can see it.

---

## 4. Events

```kotlin
fun onLaunch()

fun onRefresh()

fun onToggleGenre(name: String)

fun onSelectSortKey(key: SortKey)

fun onSelectSortDirection(direction: SortDirection)

fun onClearGenres()

fun onReset()
```

Each one is named for the event that reaches it, in the `on` + verb + target form the screen's own
parameters take, so a lambda and the function it is bound to read the same at the call site.

```kotlin
fun onRefresh() {
    viewModelScope.launch {
        refreshState.value = RefreshState(isRefreshing = true, error = null)
        val error = trendingMoviesRepository.refresh()
        refreshState.value = RefreshState(isRefreshing = false, error = error)
    }
}
```

**A new attempt clears the previous cause.** The error on screen describes the attempt that failed,
so it stops being true the moment another one starts. Clearing it at the start is what makes the bar
and the refresh indicator mutually exclusive without a rule at the render site, and it is why
retrying from the full-screen error returns to the skeleton: that is a first load, and the screen
already draws one.

**The launch refresh is `onLaunch()` from the route**, in a `LaunchedEffect(Unit)`, not from `init`.
The `stateIn` pipeline starts on first collection, and no work starts in a constructor.

**`onLaunch` fetches once per view model, `onRefresh` every time it is called.** The route asks on
every entry into composition, which a configuration change and a return from the detail screen both
are, so the view model holds the launch fetch to the first and lets the rest fall through. The
pull gesture and both retry actions call `onRefresh` instead, because a fetch the user asked for
has to reach the network to be worth the indicator it spins. Returning from the background is not an
entry into composition, so the screen shows what it already holds until the user pulls.

**`onClearGenres` clears genres and leaves the sort.** It is the action on the empty-from-filter
state, and a sort can never empty a list, so clearing genres always resolves that state while
keeping the order the user chose. `onReset` is the sheet's, and clears both.

**`onToggleGenre` takes the genre's name.** The selection is saved as enum names, so the name a
chip reports is written straight to the handle, and nothing converts to a `Genre` and back on the
way through.

**Nothing is emitted to the UI.** Every outcome above is a field the screen reads.

---

## 5. What survives, and how

| Survives | Held by | Survives rotation | Survives process death |
|---|---|---|---|
| Selected genres | `SavedStateHandle` | yes | yes |
| Sort | `SavedStateHandle` | yes | yes |
| Scroll position | `rememberLazyListState` | yes | yes |
| Whether the screen has read a selection yet | the composition | no | no |
| Sheet open | `rememberSaveable` | yes | yes |
| The launch refresh having run | the view model | yes | no |

The four saved values reset on a cold start, because saved instance state does not outlive the task.
The launch refresh resets with them, which is what makes a cold start fetch and a rotation not.

**The handle is the store, not a mirror of one.** `getMutableStateFlow` returns a flow whose writes
land in the handle, so the selection has one home: no field to keep in step, no restore branch in a
constructor, and no window where the two disagree.

**Two keys holding values a `Bundle` takes.** `MovieSort` is `@Parcelize`d, so the ordering is one
savable value rather than two keys rebuilt into a type on every emission. A `Set<Genre>` has no such
option, so genres store as an `ArrayList<String>` of enum names. Names rather than ordinals, because
reordering the enum must not silently change a saved selection.

**Scroll position does not go in the handle.** `rememberLazyListState` is already saved by the
Compose saver, so putting it in the view model would mean watching a UI concern and writing on every
scroll frame to reach the same place.

**A JVM `SavedStateHandle` is a map**, so a unit test proves the wiring and cannot prove the values
are savable. One device test writes a selection, provokes a configuration change and asserts it comes
back, which is the layer that exercises a real `Bundle`. A configuration change is as far as an
in-process instrumented test reaches: killing the app's process would take the test's own process
with it.

---

## 6. The screen

```
feature/trending/src/main/kotlin/com/shayan/amro/feature/trending/
    ui/TrendingRoute.kt
    ui/TrendingScreen.kt
    ui/TrendingPreviewData.kt
    component/MovieRow.kt
    component/MovieRowSkeleton.kt
    component/TrendingAppBar.kt
    component/TrendingSkeleton.kt
    component/FilterSheet.kt
    component/TrendingNoticeBar.kt
    component/TrendingMessage.kt
    viewmodel/TrendingUiState.kt
    viewmodel/TrendingUiStateMapper.kt
    viewmodel/TrendingViewModel.kt
    viewmodel/MovieSort.kt
    viewmodel/MovieFiltering.kt
    viewmodel/MovieSorting.kt
```

`TrendingRoute` collects with `collectAsStateWithLifecycle`, asks for the launch refresh, and passes
values and lambdas down. `TrendingScreen` is stateless and is what previews and Compose tests
target.

**Structure, from the design.** App bar carrying the screen name and a filter action with a badge,
then the body, then the sheet over both. Nothing sits between the app bar and the first row: active
narrowing is stated by the badge, and what is narrowed is stated in the sheet, where it can also be
changed.

| Region | Renders |
|---|---|
| App bar | The screen name, and a filter action badged with `activeSelectionCount` when it is above zero |
| Body, `Skeleton` | Eight placeholder rows shaped like real ones, the pulse frozen in previews |
| Body, `Movies` | The rows, inside `PullToRefreshBox` |
| Body, `EmptyFromFilter` | Icon, message and a Clear filters action |
| Body, `Error` | Icon, a sentence chosen by cause, and Retry |
| Above the body | The notice bar, when `notice` is not null |
| Over everything | The filter sheet, when open |

**`PullToRefreshBox` wraps the `Movies` branch alone.** The gesture and its indicator exist only
where there is a list to pull and a cache worth preserving. The skeleton is already the progress for
an empty cache, and constructing the box only in that one branch is what keeps an indicator off it
without a condition to write.

**A changed selection returns the list to the top.** A scroll position means something about the
list it was reached in, so the screen scrolls to the first row whenever `selection` changes, and
leaves the position alone when only the rows do. Rows appended to the end therefore never move the
list. The first run of that effect is the screen opening rather than a selection changing, so it is
skipped and a restored scroll position stays where it was restored.

**A row** is a poster tile at 2:3, the title, and its genres. An absent poster gets the placeholder
tile from the design, with its own visible label, rather than an empty frame. The list is keyed by
the row's `movieId`, so a refresh moves unchanged rows instead of rebuilding them.

**The sheet** takes one `FilterUiState` and emits the four selection lambdas plus `reset`. It holds
no copy of the selection, so nothing has to be pushed back when it closes and nothing can drift
while it is open. It opens on everything it holds: its own half-height stop would cut the last
section off, and the content scrolls. Every genre is listed, because the genre vocabulary is the
app's own and needs no fetch.

**Copy.** Every string the state carries reaches the screen inside an `AmroText`, so a composable
resolves and never chooses. The mapping from a key, a direction or a cause to its words is in
`TrendingUiStateMapper.kt`. Copy the screen owns outright, its headings and its static labels, is
read where it is drawn.

The empty-from-filter copy names the situation without naming the genres. Listing them would put a
second statement of the selection on the screen the badge decision took it off, and it would need a
plural form of all nineteen names to read correctly in a sentence. The badge says how many, and one
tap says which.

---

## 7. Accessibility

| Requirement | How |
|---|---|
| Result count announced when it changes | `liveRegion = LiveRegionMode.Polite` on the sheet's count. That is the only place the count can change, so it announces where the user is changing it, and politely, so it does not interrupt the chip that caused it |
| Badge legible without colour | The filter action's content description carries the number, so the badge is a duplicate of something spoken rather than the only statement of it |
| Sort direction legible without colour | Each direction chip carries its arrow glyph and its word, and its selection is chip state rather than a tint |
| Every image described or decorative | A poster's description names the film. The placeholder tile keeps its visible label, so its glyph is decorative |
| 48dp targets | The filter action, both sheet chip rows, Retry, Reset and Clear filters |
| Text to 200% | One golden at that scale, on the densest state |
| Rows | One merged clickable node per row, reading the title then the genres |

---

## 8. What the module gains

`:feature:trending` adds the Compose runtime and lifecycle artifacts it did not need while it held
two pure functions, Coil for posters, `kotlinx-collections-immutable` for the lists the state
carries, and the test dependencies for a view model and a screen test. `:core:testing` gains the
dispatcher rule a view model test runs on and the touch-target assertion the screen test reads
with; its fakes and fixtures already carry this screen.

Preview and golden fixtures live in `TrendingPreviewData.kt` in `src/main`, beside the screen they
feed. Each one is a cache and a selection read through the mapper, so a preview and the golden that
captures it render what the app renders rather than a state assembled beside it. Posters in
previews come from Coil's preview handler over a local drawable, so a golden captures layout and
never a download.

`:app` gains the image loader: a `SingletonImageLoader.Factory` on the application class, over the
Ktor engine the API calls already use, so images and requests share one HTTP stack and offline
poster rendering comes from Coil's own disk cache.

---

## 9. Tests

**View model, JVM, against `FakeTrendingMoviesRepository`.**

| Test | Fails when |
|---|---|
| An empty cache with no error is `Skeleton` | the screen opens on something other than a first load |
| A non-empty cache is `Movies`, in popularity descending order | the default ordering stops being the product's |
| A failed refresh over an empty cache is `Error`, naming the cause | a blank screen loses the reason it is blank |
| A failed refresh over a non-empty cache is `Movies` plus a notice | a failure destroys content the user can still use |
| A successful refresh clears a notice | the bar outlives the condition it names |
| A refresh sets `isRefreshing` and clears the previous cause before it runs | the bar and the indicator can be on screen at once |
| The launch refresh fetches once however often the screen is shown | a rotation or a return from detail refetches the set |
| A refresh the user asks for fetches after the launch refresh has | the pull gesture and the retry actions stop reaching the network |
| A selection that matches nothing is `EmptyFromFilter`, not `Error` | a zero-result filter reads as a failure |
| An empty selection is never `EmptyFromFilter` | the two empties stop being distinguishable |
| `activeSelectionCount` is 0, 1, 2 and 3 across no selection, sort only, genres only, and both | the badge stops counting a changed sort |
| `onClearGenres` empties the selection and leaves the sort | the empty state's action discards a choice the user made |
| A selection written to the handle is restored by a view model built on that handle | filter and sort stop surviving process death |

**Screen, Robolectric, against `TrendingScreen`.**

| Test | Fails when |
|---|---|
| Selecting a row reports the movie's id | the wrong movie opens |
| The badge appears, counts and disappears | the badge stops being the only statement of narrowing |
| A first load draws placeholder rows, and announces them as loading | the skeleton is silent to a screen reader |
| Retry fires from the bar and from the error state | an error dead-ends |
| The indicator shows and the bar hides while refreshing | the two stop being exclusive |
| Every interactive target is at least 48dp, and each poster is described | the accessibility bar slips |
| A changed filter and a changed sort each return the list to the top | a new ordering leaves the reader partway down a list they have not seen |
| Rows appended under an unchanged selection leave the list where it is | a later page throws a reader back to the top |

Every one of these feeds a state straight into `TrendingScreen`. No test on the JVM builds the view
model behind it, so what a lambda is wired to is covered by the flow test on a device and nowhere
above it.

**Mapper, JVM, over fixtures.**

| Test | Fails when |
|---|---|
| An empty answer over an empty cache is `Error`, with the glyph and sentence that cause reads as | the one cause with no rows of its own stops being distinguishable |
| An empty answer over a cached set is a notice, and keeps the rows | a successful call with nothing in it discards a cache |
| A movie the source has no poster for is a row with no poster to load | the placeholder tile stops being reachable from real data |

**Screenshot goldens.** Seven states crossed with the two themes, plus one form factor cross and one
font scale cross on the loaded state, which is the densest and the only one where a reflow or a clip
has room to show.

`Skeleton` · `Movies` · `Movies` with a badge of three · `Movies` with the bar · `EmptyFromFilter` ·
`Error` · sheet open.

A refreshing state gets no golden: its indicator animates, and a golden that captures an animation
fails on its own schedule. The screen test above asserts it instead.

**Flow, on a device.** One test, over the app's own graph and a local server answering as TMDB: the
screen opens on the skeleton, and a selection and a scroll position survive the screen being torn
down and built again.

---

## 10. Decisions

| # | Decision | Defence | Rejected alternative |
|---|---|---|---|
| D54 | One `TrendingUiState` data class carrying a sealed `TrendingContent` for the body | The chrome outlives every outcome and the body is exactly one thing at a time, so the compiler holds the part that is exclusive and the shared part is written once | One flat class with four nullable fields, where the exclusion is a convention; one sealed hierarchy, which repeats the selection, the sort and the counts on every branch because the badge renders in all of them |
| D55 | Empty from API is an `Error` read from `EmptyResponse`, not a content case | A glyph and a sentence are already read per cause, because three causes must read differently. A fifth case would be a second way to say one cause | A dedicated `EmptyFromApi` case, which splits one rule across a type and a `when` |
| D56 | `activeSelectionCount` counts selected genres plus one for a sort away from `MovieSort.DEFAULT`, and is read in the mapper | The badge is the screen's only statement that anything is narrowed, so a changed sort that did not count would be silent, and one expression produces it from the selection it counts | A badge counting genres only, which hides a changed order; a count the screen works out, which is a second reading of a selection the screen does not hold |
| D57 | A row is a `MovieRowUiState`, and no state the screen reads carries a type the data layer owns | The screen resolves ids and strings it is handed instead of deciding them, so no reading of a `Genre` or a `DataError` survives in a composable, and a click reports the id the detail screen is opened with | `Movie` straight into the row, which puts genre-to-label and cause-to-message decisions in composables and carries `:core:model` into every preview and screen test |
| D58 | Sheet visibility is `rememberSaveable` in the screen | Opening a sheet is UI logic, and the saver already carries it through a rotation and process death | A `isSheetOpen` field, which puts UI logic in a view model and needs an acknowledgement method to close |
| D59 | Filtering, ordering and mapping run in the state transform, on the main thread | Pure over at most a hundred items, reading state the view model already holds. A dispatcher hop would cost a thread switch per chip tap | `flowOn(Default)`, which buys nothing measurable and adds a qualifier every fake has to satisfy |
| D60 | A refresh clears the previous cause as it starts | The cause describes the attempt that failed, so it stops being true when another begins. The bar and the indicator are then exclusive with no rule at the render site, and Retry from a blank error returns to the skeleton, which is what it is | Clearing the cause on success only, where the bar and the indicator coexist and the render site has to say they do not |
| D61 | `SavedStateHandle.getMutableStateFlow` is the store, over two keys a `Bundle` takes: a `@Parcelize`d `MovieSort` and the genre names | The selection has one home, so there is no field to keep in step and no restore branch in a constructor. `MovieSort` lives in this module, so `Parcelable` is a UI-layer concern where it belongs, and the ordering is saved as the one type everything else passes. Genres store as their names, which survive a reordering of the enum where ordinals do not | A `MutableStateFlow` mirrored into the handle, which is two homes and a window where they disagree; ordinals, which a reordering silently changes; a key per sort field, which rebuilds the type on every emission and lets the two halves be written apart |
| D62 | Scroll position stays in `rememberLazyListState`, not the handle | The Compose saver already carries it through a rotation and process death | Writing it to the handle, which watches a UI concern and writes on every scroll frame to reach the same place |
| D63 | `PullToRefreshBox` wraps the `Movies` branch alone | The gesture and its indicator belong where there is a list to pull and a cache worth preserving. The skeleton is already the progress for an empty cache, and building the box in one branch keeps the indicator off the others with no condition to write | The box around the whole body, which needs `isRefreshing && content is Movies` at the render site and puts an indicator over a skeleton |
| D64 | `onClearGenres` leaves the sort, `onReset` clears both | A sort cannot empty a list, so clearing genres always resolves the empty state, and it keeps the order the user chose | One action for both, which discards a choice unrelated to the state it is resolving |
| D65 | The empty-from-filter copy names the situation, not the genres | Listing them restates the selection the badge decision took off this screen, and reading correctly in a sentence would need a plural form of all nineteen names. The badge says how many and one tap says which | Naming the selected genres, which is nineteen more strings and a second statement of state |
| D66 | The genre-to-label mapping lives in `:feature:trending`, inside the mapper | `:core:ui` cannot hold it: it depends on nothing in the project, so it cannot see `Genre`. The feature that renders genres owns the words for them, and the mapper is where every other reading of a domain type already is | Genre names on the model, which puts user-facing copy below the UI layer; a shared module for one mapping, before a second screen exists to share it |
| D67 | The refreshing state gets a screen test rather than a golden | Its indicator animates, so a golden capturing it fails on its own schedule | A frozen-animation golden, which is machinery for a state two assertions cover |
| D68 | Every word reaches the screen as an `AmroText`, resolved at composition | One type covers every word the screen renders, so a state field never chooses between a resource id, a plural and a list joined at the render site, and `resolve()` is the single seam where configuration is read. A resolved string needs a `Context` above the screen, and a view model outlives the activity, so a locale change would leave the state holding words in the language the user just left | Resolved strings in the state, which buys the same freedom from `when` blocks and pays for it with a stale locale and a `Context` in the view model; a `@StringRes Int` per field, which cannot carry a plural, a join, or a value already written out |
| D69 | Every list in the state is an `ImmutableList`, and every state type is `@Immutable` | The models are rebuilt on each emission, so instance equality cannot skip a composable and a `List` gives Compose nothing to compare | A `List`, which Compose treats as unstable and recomposes every row on every emission |
| D70 | The launch refresh is held to once per view model, behind `onLaunch()` | The route asks on every entry into composition, and both a configuration change and a return from the detail screen are entries, so an unguarded ask refetches a hundred movies the view model is already holding. The paths the user drives call `onRefresh` and always reach the network | `onRefresh()` straight from the route, which refetches on every rotation and every return from detail; the fetch in `init`, which is work in a constructor and starts a fetch in every view model test |
| D71 | The screen returns the list to the top when `selection` changes, skipping the first run | The scroll position the user is at describes the list they are reading, so a new narrowing or ordering invalidates it while rows arriving under the same selection do not, which is what leaves an appended page where it is. The first run is the screen opening, and skipping it is what stops a rotation from scrolling away from the position it just restored | Scrolling on every change of the rows, which throws a reader to the top of an appended page; a scroll event from the view model, which is the one thing this screen does not emit |

---

## 11. What this changes elsewhere

- The brief's partial page failure row in 5.1 and its treatment are deleted. Nothing can produce
  them.
- The brief's 6.3 line on a refresh over existing content is narrowed: a refresh where a list is on
  screen shows the indicator, and one where the skeleton is showing does not, because the skeleton
  is already the progress.
- The brief's summary row in 5.1, and D3, are replaced by the badge. Active narrowing is stated
  once, on the action that reopens the sheet, and a sort away from the default counts toward it.
- The roadmap's 006 row lists seven states. Six are built, over four content cases.
- The cut list's item 2 keeps representative goldens and drops the crosses. One form factor cross
  and one font scale cross come back, on the loaded state.
- The cut list's item 4, pull to refresh, is built. Item 5 already had nothing to cut.
- `:app` gains the image loader, and `:feature:trending` gains the Compose, lifecycle, Coil,
  immutable-collections and test dependencies a screen needs.
- 007 inherits the genre-to-label mapping and the question of whether to hoist it, and the same
  four-case reading of its own three states.
- 008 inherits a stateless `TrendingScreen` that takes a selection lambda, which is what lets the
  expanded width update a pane instead of navigating.
