# 007: Movie detail screen

`:feature:detail`. One movie's record, in the three states the data can put it in, over a layout
that has to stay composed when six of its thirteen fields are absent.

Behaviour is owned by the product brief, technology by the tech stack spec, the module map and
dependency rules by the architecture doc, the test layers by the testing doc. Where this spec and
those disagree, those win.

---

## 1. What is already settled

- **Two reads and one refresh.** `getMovieFlow(id): Flow<Movie?>`,
  `getMovieDetailFlow(id): Flow<MovieDetail?>` and `suspend fun refresh(id): DataError?`, where null
  means the write happened.
- **A read cannot fail and a failed refresh writes nothing.** The cached record on screen is never
  destroyed by a failure.
- **The repository returns the cause and never decides whether it is shown.** This screen decides.
- **Exactly three states: loading, loaded, error.** No offline state and no stale state. A
  cache-served render is an ordinary loaded screen, and so is one whose background refetch failed.
- **The screen is reached only by selecting a row**, so the summary read has a movie whenever the
  screen opens, and the record it does not have yet is the detail.
- **The missing-data rules.** The rating block goes when the source reports no rating, and shows the
  rating alone when it reports no vote count. Budget and revenue read "Not disclosed", never "$0".
  Runtime reads "2h 37m". An empty tagline and an absent IMDB id remove their rows. The IMDB row
  always launches the browser.
- **The shape 006 settled.** One state class carrying a sealed body, `ImmutableList` in every state
  type, no event channel, `stateIn` on `:core:ui`'s shared timeout, and every word travelling as an
  `AmroText`.

---

## 2. The state

```kotlin
data class MovieDetailUiState(
    val title: String?,
    val content: MovieDetailContent,
)

sealed interface MovieDetailContent {
    data class Skeleton(
        val posterUrl: String?,
        val genreLabels: ImmutableList<AmroText>,
        val genreDescription: AmroText,
    ) : MovieDetailContent

    data class Loaded(
        val posterUrl: String?,
        val backdropUrl: String?,
        val badge: HeaderBadgeUiState?,
        val tagline: String?,
        val overview: String?,
        val genreLabels: ImmutableList<AmroText>,
        val genreDescription: AmroText,
        val facts: ImmutableList<MovieFactUiState>,
        val imdbUrl: String?,
    ) : MovieDetailContent

    data class Error(val glyph: String, val title: AmroText) : MovieDetailContent
}

sealed interface HeaderBadgeUiState {
    data class Rating(
        val rating: AmroText,
        val voteCount: AmroText?,
        val contentDescription: AmroText,
    ) : HeaderBadgeUiState

    data class Status(val label: AmroText) : HeaderBadgeUiState
}

data class MovieFactUiState(
    val label: AmroText,
    val value: AmroText,
    val disclosed: Boolean,
)
```

**The title is chrome, and sits outside the body.** The app bar reads it in all three states, which
is the test for what sits at the top level. It is read from the detail where there is one and from
the cached summary otherwise, so one field is right in every state and the bar and the header render
the same value rather than two copies of it.

**A first load carries what the trending row already handed over.** The poster, the title and the
genres are on screen before the request returns, and only what the request is fetching is a
placeholder. The poster is the tile the user tapped, so the continuity the header is built on holds
from the first frame rather than from the answer. Nothing outside the body reads the poster or the
genres, so they sit on `Skeleton` rather than beside the title. Its three fields default to nothing,
which is a movie the trending set holds no row for.

**No type the data layer owns reaches the screen, and no value reaches it unread.** A `Genre`, a
`ReleaseStatus` and a `DataError` each become an `AmroText` in the mapper, and so does every amount,
count, date and running time. The screen is handed words to resolve and never a value to interpret.

**The rating slot holds a rating or a status, never both and sometimes neither.** A movie with no
votes has no rating to show, and what explains both the missing votes and the missing box office
below is that it has not been released. So the slot the rating would occupy carries the status
instead, and the status leaves the fact grid it would otherwise close. One nullable field
carrying a sealed body is what makes that exclusive: null is the one case with nothing to say, a
source that reports neither a rating nor a status the app knows.

**The fact grid arrives built, in the order it is read.** Runtime, release date, budget and revenue
are always there, in that order, and a value the source does not have reads "Not disclosed" rather
than taking its row away. A reader looks for a fact where they last saw it, so a row that
disappears moves every row after it, and the reader has to find them again. The mapper fixes that
order and gives every cell its own label, so the grid is a list the screen lays out two to a row
and nothing about which fact is where is decided at composition.

The status closes the list, and only where the header slot is not already stating it. Last is where
it cannot move anything: the four the reader scans for lead in every record, and the status either
follows them or does not. It is also the lowest-priority fact for a released film, so it takes the
fifth cell, on a row of its own with the right column empty.

**Every word travels as an `AmroText`.** It is `:core:ui`'s wrapper over text that is named rather
than resolved: a resource, a plural, a joined list, or a `Raw` string already written out. The
screen calls `resolve()` and nothing else, so no composable in this feature names a `NumberFormat`,
a `DateTimeFormatter`, a `Currency` or a locale.

**A cell states whether it is disclosed, and the screen reads that as a colour.** An undisclosed
cell carries the words that say so and takes the muted role, which is what keeps "Not disclosed"
from reading as a value the film has. The words and the flag are set together in one place, so the
two cannot disagree.

**Three of the brief's states, over three content cases.**

| State | `content` |
|---|---|
| Loading | `Skeleton` |
| Loaded, from cache or from an answer | `Loaded` |
| Loaded, behind a background refetch that failed | `Loaded` |
| Error | `Error`, read from the cause |

**There is no notice slot and no refreshing flag.** A failure over a cached record has nowhere to
be rendered, so the rule that it reaches nobody is a property of the state rather than a condition
at a render site. Nothing renders differently while a refetch runs either, so nothing tracks that it
is running.

---

## 3. Producing the state

```kotlin
@HiltViewModel(assistedFactory = MovieDetailViewModel.Factory::class)
internal class MovieDetailViewModel @AssistedInject constructor(
    @Assisted private val movieId: String,
    private val movieDetailRepository: MovieDetailRepository,
) : ViewModel() {

    private val error = MutableStateFlow<DataError?>(null)
    private var hasFetchedOnLaunch = false

    val uiState: StateFlow<MovieDetailUiState> = ...

    @AssistedFactory
    interface Factory {
        fun create(movieId: String): MovieDetailViewModel
    }
}
```

`uiState` combines the two reads and the cause of the last fetch, and lands in
`stateIn(viewModelScope, WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS), MovieDetailUiState())`.

**The movie id is a constructor parameter.** The back stack holds a typed `MovieDetailKey` and the
entry decorator already scopes a view model to it, so the id reaches the route as a value rather
than through a handle. Assisted injection puts it in the constructor, where the flows can be built
from it in a field initialiser instead of waiting for a method call to say which movie this is.

The combine hands its three values to one mapper:

```kotlin
internal fun toMovieDetailUiState(
    movie: Movie?,
    detail: MovieDetail?,
    error: DataError?,
): MovieDetailUiState
```

```kotlin
val summary = detail?.movie ?: movie
val content = when {
    detail != null -> detail.toLoaded()
    error != null -> error.toErrorContent()
    else -> summary.toSkeleton()
}
```

**The cause is read only where there is no record.** A cached record wins over a failure, so a
background refetch that failed leaves a loaded screen without anything deciding that it should.
This is the same rule the trending screen applies, reaching the opposite answer because this screen
has no content that a stale record makes false.

**The mapper is where every reading of a domain type happens.** A cause becomes a glyph and a
sentence, a status becomes a pill or a fact, a genre becomes a label, an amount the source does not
have becomes the words that state so. One file holds all of it, so the screen has none of it and a test reads the whole
mapping without a composition.

**It runs on the main thread, with no `flowOn`.** Mapping one record is a handful of null checks.

**Formatting runs with it, in `viewmodel/DetailFormatting.kt`.** A date, an amount and a rating are
written out there against `Locale.getDefault(Locale.Category.FORMAT)`, which the platform rewrites
when the system or per-app language changes, so the mapper needs no `Context` to know which language
it is in. Each one arrives at the screen as an `AmroText.Raw`.

**A running time stays a resource.** Hours and minutes are two numbers a translator arranges rather
than a format the JDK owns, so it travels as an `AmroText.Resource` carrying both, and the
arithmetic that splits them is the mapper's.

**What this costs.** A combine re-runs only when the record, the cause or the id changes, and a
locale change is none of those, so the written-out values hold the previous language until the next
emission while the resources beside them resolve to the new one. The exchange is that no screen file
holds a formatter.

---

## 4. Events

```kotlin
fun onLaunch()

fun onRetry()
```

```kotlin
fun onRetry() {
    viewModelScope.launch {
        error.value = null
        error.value = movieDetailRepository.refresh(movieId)
    }
}
```

**A new attempt clears the previous cause.** The cause describes the attempt that failed, so it
stops being true the moment another one starts. Clearing it first is why Retry from the error state
returns to the skeleton: that is a first load, and the screen already draws one.

**`onLaunch` fetches once per view model, `onRetry` every time it is called.** The route asks on
every entry into composition, which a configuration change is, and the record is refetched on every
open rather than on every recomposition. A different movie is a different key, a different entry and
a different view model, so selecting another film in the two-pane layout fetches that film.

**Nothing is emitted to the UI.** Every outcome above is a field the screen reads.

---

## 5. What survives, and how

| Survives | Held by | Survives rotation | Survives process death |
|---|---|---|---|
| Which movie is shown | the back stack | yes | yes |
| Scroll position | `rememberScrollState` | yes | yes |
| The launch fetch having run | the view model | yes | no |

The screen holds no selection of its own, so nothing goes in a `SavedStateHandle`. Scroll position
is already carried by the Compose saver, and putting it in the view model would mean watching a UI
concern and writing on every scroll frame to reach the same place.

A process death drops the launch fetch along with the view model, so the screen fetches again when
it is rebuilt, which is what a cold open of a movie is.

---

## 6. The screen

```
feature/detail/src/main/kotlin/com/shayan/amro/feature/detail/
    ui/MovieDetailRoute.kt
    ui/MovieDetailScreen.kt
    ui/MovieDetailPreviewData.kt
    component/DetailAppBar.kt
    component/DetailHeader.kt
    component/DetailFacts.kt
    component/DetailSkeleton.kt
    component/GenreChips.kt
    viewmodel/MovieDetailUiState.kt
    viewmodel/MovieDetailUiStateMapper.kt
    viewmodel/DetailFormatting.kt
    viewmodel/MovieDetailViewModel.kt
```

`MovieDetailRoute` builds the view model from the id, collects with `collectAsStateWithLifecycle`,
asks for the launch fetch, and passes values and lambdas down. `MovieDetailScreen` is stateless and
is what previews and Compose tests target.

**Structure, from the design.** A 16:9 backdrop opens the screen and fades into the background. The
2:3 poster overlaps it, so the tile the user tapped is still on screen, with the rating or the
status beside it. Then the title and the tagline as one unit, the genre chips, the overview, the
fact grid behind a rule, and the IMDB action.

| Region | Renders |
|---|---|
| App bar | The back action where there is one, and the title, at an opacity the scroll decides |
| Body, `Skeleton` | The summary's poster, title and genres, and placeholder blocks for what only the record has |
| Body, `Loaded` | The backdrop, the header, the text, the fact grid and the IMDB action |
| Body, `Error` | Icon, a sentence chosen by cause, and Retry |

**One title, in one place at a time.** At rest the bar is transparent over the art and holds only
the back action, on a scrim disc, because the header below already carries the title in headline
type. As the header scrolls under it the bar fills in and the title fades up, and past the backdrop
it settles opaque with a divider. Both the fill and the title opacity come from one fraction of the
scroll offset over the backdrop's height, so there is one number to be right about and the two can
never disagree.

A first load has that header too, so the same fraction runs it and the bar is at rest there as well.
The error state is the one with no header to name the screen, and it starts the bar filled.

**The body scrolls as a `Column`.** The content is a fixed handful of sections rather than a list of
rows, so there is nothing to key, recycle or measure lazily.

**The header is the shape that has to survive an absent field.** A missing backdrop is the panel
with its own glyph rather than a gap the poster hangs off, and a missing poster is the tile the
trending row already uses. Neither is reachable from the recorded data, and both are what stops the
overlap from collapsing when one day they are.

**The poster is the one the row loaded.** The header draws the small size, which is the size the
trending row already put in the cache, so the tile the user tapped is on screen with no request at
all. Both bodies read it, so nothing reloads as the record lands. The backdrop is still the large
one: nothing has it, and it runs the width of the screen.

**A first load lays out over the same box.** The header's height, its poster and the overlap are one
set of values both draw from, so the poster is in the same place before and after the record lands
and the swap moves nothing above the tagline.

**A placeholder is a block in a shared fill, breathing on a shared clock.** The block, the fill and
the pulse all come from `:core:ui`, so this screen and the trending list wait at the same rate in
the same colour. The muted fill is for a block the eye should not read first: the backdrop behind
the poster, and a fact's label above its value.

**The fact grid is two columns, filled row by row.** Each cell is a label above a value, and the
list arrives in its final order, so the four fixed facts read in the same two positions on every
record and the status closes the grid when the header is not carrying it.

**Copy.** Every string is a resource in this module, except the genre names and the two shared
components' own copy, which are `:core:ui`'s. Every one of them reaches the screen inside an
`AmroText`, so a composable resolves and never chooses. The IMDB address is built in the mapper from
the id and the one host constant.

---

## 7. Accessibility

| Requirement | How |
|---|---|
| Every image described or decorative | The poster's description names the film. The backdrop is decorative: it carries nothing the poster beside it does not already say, and two images announcing the same film is one announcement too many |
| The rating reads as one statement | The rating, its scale and the sample size are one merged node with its own description, because a star, a number and a count read as three fragments otherwise |
| The status is legible without colour | It is a word in a pill, not a tint |
| Facts read as label then value | Each cell is one merged node, so "Budget, Not disclosed" is one thing a reader hears |
| An action that leaves the app says so | The IMDB action's description names the browser, so the app bar disappearing afterwards is not a surprise |
| 48dp targets | The back action and the IMDB action |
| Text to 200% | One golden at that scale, on the record with the longest text |
| Genres | One merged node reading the list once |
| A first load announces the wait once | The placeholder region is one node saying so. The title, the poster and the genres above it are real, and stay their own nodes |

---

## 8. What the module gains

`:feature:detail` adds the lifecycle and Hilt view model artifacts, Coil for the poster and the
backdrop, `kotlinx-collections-immutable` for the lists the state carries, and the test dependencies
for a view model and a screen test.

`:core:ui` gains seven things. Three this screen and the trending screen both need: the poster tile,
the message panel behind an empty or failed screen, and the genre labels. The first two take
primitives and sit beside the design system. The labels need `Genre`, so `:core:ui` gains
`:core:model` as an `api` dependency.

The fourth is `AmroText`, the wrapper every state type above it carries its words in. It sits in
`:core:ui` because it is the layer that owns how a word reaches a screen, and it holds its format
arguments in an `ImmutableList`, so that becomes an `api` dependency too. A `data class` compares an
`Array` by identity, which would cost the type both its equality and its Compose stability.

The last three are what a placeholder is made of. `PlaceholderBlock` is the rectangle itself, cut
to a line's corner unless a caller asks for a poster's. `placeholderPulseAlpha()` is the breathing
opacity every skeleton draws at, taking the offset that staggers a list of rows and returning the
full value under `LocalInspectionMode`, so a golden captures a still frame. `placeholder` and
`placeholderMuted` are the fills, on `ExtendedColors` beside the other roles M3 has no slot for.
Each scheme carries its own pair, because a fill that reads against a dark surface is lighter than
it and one that reads against a light surface is darker.

`:core:testing` gains nothing. `MovieFixture.detail()` already defaults every field and
`FakeMovieDetailRepository` already seeds and scripts per id.

Preview and golden fixtures live in `MovieDetailPreviewData.kt` in `src/main`, beside the screen
they feed. Each one is a movie and a detail read through the mapper, so a preview and the golden
that captures it render what the app renders. Posters come from Coil's preview handler over a local
drawable.

---

## 9. Tests

**View model, JVM, against `FakeMovieDetailRepository`.**

| Test | Fails when |
|---|---|
| A cached detail is `Loaded` before any fetch returns | the cache stops being what the screen opens on |
| Nothing cached and nothing failed is `Skeleton` | the screen opens on something other than a first load |
| A failed fetch with nothing cached is `Error`, naming the cause | a blank screen loses the reason it is blank |
| A failed fetch over a cached detail stays `Loaded` | a refresh the user never asked for reports itself |
| A successful fetch replaces the record on screen | a film cached while unreleased keeps its empty fields |
| The title is the cached movie's before any detail arrives | the loading bar cannot name where the user is |
| The launch fetch runs once however often the screen is shown | a rotation refetches |
| Retry fetches again and clears the previous cause first | Retry dead-ends, or the error outlives the attempt it describes |

**Mapper, JVM, over fixtures.**

| Test | Fails when |
|---|---|
| A movie with no rating states its status in the slot, and leaves the grid | an unreleased film reads as one scoring zero, or states its status twice |
| A movie with a rating closes the grid with its status, after the four fixed facts | a fact a reader scans for is pushed down a row by one that outranks nothing |
| A rating with no vote count is the rating alone | a source that publishes no count loses a rating it has |
| Budget, revenue and runtime with no value read as undisclosed | a two-in-five case reads "$0", or a row leaves and moves the rows after it |
| The grid reads runtime, release date, budget, revenue, status | the order a reader scans by becomes a property of whatever built the list |
| An absent tagline and an absent IMDB id leave no tagline and no action | a blank slot survives |
| The missing-data fixture maps to a screen with a status pill and the four fixed facts | the shape two in five records take stops being the one that is checked |
| An IMDB id becomes the title address | the row opens nothing |
| A first load carries the summary's poster and genres | a first load stops drawing the row the user tapped |
| A first load with no summary carries nothing to draw | the fallback for a movie no row was read for stops holding |

**Screen, Robolectric, against `MovieDetailScreen`.**

| Test | Fails when |
|---|---|
| The bar has no title at rest and has it once the header is scrolled past | the screen stops naming where the user is |
| A first load names the movie in the header, shows the poster it was opened from, and announces the wait once | a first load stops standing in for the screen it becomes |
| The IMDB action reports its address | the browser opens the wrong film |
| Back fires where there is a back action, and there is none where there is not | a pane grows an affordance with nowhere to go |
| Retry fires from the error state | an error dead-ends |
| Every interactive target is at least 48dp, the poster is described and the backdrop is not | the accessibility bar slips |

**Formatting, plain JVM.** Each function against the `AmroText` it produces, with the locale pinned,
because an assertion on grouped digits is otherwise an assertion about the machine it ran on: a
runtime of 157 minutes, one under an hour, an amount at ten digits, a rating to one decimal, a long
release date, and a vote count at both of its plural forms. Nothing here needs a composition, an
Android framework or a resource.

**`AmroText` itself, Robolectric, in `:core:ui`.** Every case resolved against real `Resources`,
including an argument that is itself an `AmroText`.

**Screenshot goldens.** Five states crossed with the two themes, plus one form factor cross and one
font scale cross on the record with the longest text, which is where a clip has room to show.

`Skeleton` · `Skeleton` with no summary · `Loaded`, complete · `Loaded`, the missing-data fixture ·
`Error`.

The second skeleton is the fallback with nothing to draw for real, which is the one shape the first
does not cover.

One more loaded golden takes the crowded record: six genres, so the chips wrap, and a ten-digit
revenue. Those are the two ways this layout breaks and neither shows on the other records.

The collapsed bar gets no golden. It is a position in a scroll rather than a state the mapper can
produce, and the screen test above asserts it.

**Flow, on a device.** Covered by 008's navigation test, which opens a movie and returns to the
list. Nothing here needs a device of its own.

---

## 10. Decisions

| # | Decision | Defence | Rejected alternative |
|---|---|---|---|
| D71 | One `MovieDetailUiState` carrying the title and a sealed `MovieDetailContent` | The bar names the screen in all three states and the body is exactly one thing at a time, so the compiler holds the part that is exclusive and the title is written once for the bar and the header both | A sealed hierarchy alone, which repeats the title on all three branches; a flat class with three nullable bodies, where the exclusion is a convention |
| D72 | The content is chosen by whether a record is cached, and the cause is read only when there is none | A background failure then has nowhere to be rendered, so "it reaches nobody" is a property of the state rather than a rule at a render site. The same repository call serves a bar on the trending screen and silence here, and the difference is where it is folded in | A notice field the screen is told not to use, which is one edit away from being used; a repository that decides, which would have to be told what is on screen |
| D73 | No refreshing flag and no indicator | The user did not ask for the refetch, and nothing on screen changes while it runs, so there is nothing to report and nothing to track | A progress affordance over cached content, which reports work the user did not request and offers no action |
| D74 | The rating slot is one nullable field carrying either a rating or the status | The two are exclusive by construction: a status pill appears because there is no rating, and it explains the empty fields around it. Null is the one real third case, a source with neither | Two nullable fields, which is the exclusion as a convention; the status always in the fact grid, which leaves the slot empty on exactly the records that most need explaining |
| D75 | The mapper builds the fact grid: a list of cells in a fixed order, each carrying its own label, its value or the words that state there is none, and whether it is disclosed; the conditional status closes it | A reader looks for a fact where they last saw it, so a row that leaves moves every row after it and the reader has to find them again. Building the list in the mapper makes the order and the labelling one decision, taken where the record is read, and leaves the screen laying out cells it does not have to identify. Runtime and release date are what a reader scans for, so they lead in every record; the status is the lowest-priority fact for a released film, and last is the one position from which appearing moves nothing | A named field per fact, which reads well until the screen has to pair each field with a label and put the five back in order, which is the same decision taken twice; a list of the facts that exist, where an absent runtime shifts the three rows below it; the status leading the grid, which pushes the two facts a reader came for down a row on exactly the records that have a rating; a fixed template with blank cells, which is the empty slot the brief rules out |
| D76 | Every word travels as an `AmroText`, and formatting runs in the mapper | A screen that resolves and never interprets is the point: no composable in this feature names a `NumberFormat`, a `DateTimeFormatter`, a `Currency` or a locale, and the arithmetic that splits a running time sits with the rest of the reading rather than in a composable. The formatters read the platform's own formatting default, so the mapper needs no `Context`. The case still picks the muted role that keeps "Not disclosed" from reading as a value | Formatting at composition, which leaves five loose composables in the UI layer naming four JDK formatters between them, and puts non-locale arithmetic there with them; a formatted `String` in the state with no wrapper, which gives copy nowhere to stay a resource |
| D77 | `onLaunch` holds the fetch to once per view model, `onRetry` always fetches and clears the cause first | The route asks on every entry into composition, and a rotation is one, so an unguarded ask refetches a record the view model is already holding. Clearing the cause as an attempt starts is what returns Retry to the skeleton, which is what a first load is | The fetch in `init`, which is work in a constructor and starts one in every view model test; clearing on success only, where a stale error is on screen behind the attempt that replaces it |
| D78 | The movie id is an assisted constructor parameter | The entry decorator already scopes a view model to its key, so the id is a value the route has rather than something to look up. In the constructor, the flows are built in a field initialiser instead of waiting to be told which movie this is | The id through `SavedStateHandle`, which is Navigation Compose's argument model and not this back stack's; an `onLaunch(id)` that also seeds the flows, which leaves a window where the state is for no movie |
| D79 | The IMDB action reports an address, and the route hands it to `LocalUriHandler` | A screen test can then assert what a tap reports without a browser, and the launch stays inside the feature, because leaving the app is not a move in the app's own back stack | The launch inside the screen, which puts a platform call where a golden and a screen test both run; a lambda into `:app`, which makes navigation own something that is not navigation |
| D80 | The bar's fill and its title both follow one fraction of the scroll over the backdrop's height, in every state that has a header | One number decides both, so the fill and the title cannot disagree about where the header ended. The title is on screen at all times, in exactly one place. A first load has a header naming the movie, so it runs the same fraction and the chrome does not change as the record lands; the error state, which has no header, is the one that starts filled | Two independent thresholds, which is two numbers to keep in step; a title always in the bar, which states it twice while the header is showing; a first load held at full collapse, which flips the whole bar at the moment the record arrives |
| D81 | The poster tile, the message panel and the genre labels move to `:core:ui`, which gains `:core:model` as an `api` dependency | A second screen renders all three, and the app's nineteen genre names are one translator's work, so a second copy of a nineteen-arm reading is a drift that shows on whichever screen is looked at less. `:core:model` is types only and depends on nothing, so the edge cannot cycle and adds no weight, and the rule it relaxes, that design system components take primitives, still holds: a mapping is not a component | A copy per feature, which drifts silently on a rename; a shared feature-support module for three files, which is a feature depending on a feature under another name; genre names on the model, which puts user-facing copy below the UI layer |
| D82 | The backdrop is decorative and the poster is described | The poster beside it already names the film, and a second image announcing the same film is an announcement a reader has to skip | A description on both, which is the design's own alt text and reads as a duplicate; a description on neither, which loses the only naming of the poster |
| D83 | `AmroText` is `:core:ui`'s, is sealed over `Raw`, `Resource`, `Plural`, `Joined` and `Empty`, and resolves against `Resources` | One type covers every word any screen renders, so a feature's state never chooses between a resource id and a string, and `resolve()` is the single seam where configuration is read. `Raw` is what lets a value already written out travel beside copy that is still a resource. It resolves from `Resources` rather than only at composition, so a test reads it without one | A `@StringRes Int` per field, which cannot carry a plural, a join, or a value the mapper wrote out; a resolved `String` everywhere, which is D68's stale locale for copy as well as for values; the type in a feature, which the second screen would copy |
| D84 | A first load draws the summary's poster, title and genres for real, and greys only what the request is fetching. The three fields sit on `Skeleton` rather than beside the title | The trending row already handed all three over, so a placeholder in their place is a worse drawing of something known. The poster is the tile the user tapped, which is what the header's overlap is for, and a grey box there breaks the continuity for exactly as long as the wait lasts. A header naming the movie is also what lets the bar stay at rest, so the chrome is the same either side of the swap. They sit on the branch because the test for the top level is whether chrome reads it, and only the title is read there | An all-grey skeleton, which is a slower way to show what is already known and forces the bar to flip when the record lands; the three fields beside the title, which puts them on the error branch, where nothing draws them; the release date drawn for real too, which the grid can only state through the undisclosed path, and that path asserts the source has no value |
| D85 | Both bodies read the poster size the trending row reads, and the backdrop reads the large one | The two sizes are two addresses and so two cache entries, and the record's own request is already in flight when the header wants the poster. Reading the row's address makes the poster free on the state that has nothing else to show, and reading it on both is what stops the tile reloading as the record lands. The backdrop has no other reader and runs the width of the screen | The large size on both, which downloads the poster again while the screen is already waiting; the row's size on a first load alone, which blanks the tile at the swap, at the moment the change exists to smooth |
| D87 | The pulse and the two placeholder fills are `:core:ui`'s, and each scheme carries its own pair of fills | Two screens wait, and a wait that breathes at one rate here and another there reads as two apps. The fills were arithmetic over the surface at both call sites, which is a colour decided in a composable and a scheme sniffed from a luminance rather than read. As tokens they sit where every other role sits and a designer can change one. Two pairs rather than one, because a fill that reads against a dark surface is lighter than it and one that reads against a light surface is darker, so there is no single colour that reads in both | One neutral fill for both schemes, which is washed out on one of them; the arithmetic left at each call site, which is the same colour derived twice and a theme the code infers rather than reads |

---

## 11. What this changes elsewhere

- A first load holds one line for the tagline and eight for the overview. A record whose own run to
  a different count is the one place a row still moves, and nothing above the tagline does.
- The brief's 5.2 app bar line is narrowed: the bar carries the title once the header holding it has
  scrolled away, so the title is on screen throughout and never twice.
- The brief's missing-data rule is bounded to what a label does not already promise. A tagline, a
  rating and the IMDB action leave no trace when they are absent, because nothing on screen said
  they were coming. A labelled fact states its absence in place, because the label said it was.
- The brief's 6.3 line on an inline indicator over existing content is the trending screen's alone.
  This screen's refetch shows nothing.
- The roadmap's 007 question about where the formatters live is answered in one part: the mapper owns
  what is said and how it reads, and the screen resolves. What the screen still owns is the role a
  value renders in, which is the fact's case rather than its words.
- The roadmap's 007 question about the fact grid reflowing is answered by no row ever leaving and
  the one conditional fact sitting last, where appearing moves nothing.
- D66 is superseded by D81. The genre labels leave `:feature:trending`, along with its poster tile
  and its message panel, and `:core:ui` gains a dependency on `:core:model`. The architecture doc's
  rule becomes: `:core:ui` depends on `:core:model` and on nothing else in the project.
- `:feature:trending` renders the two moved components, reads genre labels from `:core:ui`, and
  draws its rows out of the shared block, in the shared fills, on the shared pulse. Its goldens are
  re-recorded only if the move changes a pixel, which it should not.
- The cut list's item 2 keeps the complete and missing-data records on this screen, and one more for
  the crowded one. One form factor cross and one font scale cross come back, on the longest record.
- 008 inherits a stateless `MovieDetailScreen` whose back action is absent where there is nowhere to
  go, and a view model built from the key rather than from a handle, which is what lets the expanded
  width swap the pane's record by swapping its key.
