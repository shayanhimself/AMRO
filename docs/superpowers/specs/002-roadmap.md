# MVP roadmap

The foundation is in place: build logic, catalog, module skeleton, design system, app shell, test
harness and scripts. What remains is the product.

`:core:model`, `:core:database` and `:core:data` hold no sources. `:core:network` holds
`TmdbConfig` alone. Both feature routes are empty surfaces the graph resolves.

This document sequences the remaining work into six specs, numbered from 003. It does not decide
what those specs decide. Each one is brainstormed on its own, written, and then built. What appears
below per spec is the decision surface it has to close and the constraints it inherits, so the
brainstorm starts from the real question rather than from a blank page.

The budget is **16 hours**, and the spec sessions are inside it.

Behaviour is owned by the product brief, technology by the tech stack spec, the module map and
dependency rules by `docs/architecture.md`, the test layers by `docs/testing.md`. Where a spec and
those disagree, those win.

---

## Sequence

| Spec | Owns | Budget | Running |
|---|---|---|---|
| 003 | Models and network | 2:30 | 2:30 |
| 004 | Persistence and the genre join | 1:15 | 3:45 |
| 005 | Repositories, fetch policy, filter and sort | 2:30 | 6:15 |
| 006 | Trending screen | 3:30 | 9:45 |
| 007 | Movie detail screen | 2:30 | 12:15 |
| 008 | Navigation, adaptive layout, and release | 2:30 | 14:45 |

**1:15 of slack.** That is the overrun allowance, not spare capacity. Each budget includes its own
brainstorm and spec write, which is roughly 20 minutes of it.

Demoable checkpoints: a live trending list at 6:15, filter and sort at 9:45, the detail screen at
12:15, the whole app wired at 13:15.

Order is not negotiable below 006. Each spec is tested against the one below it, and the fixtures
recorded in 003 are what every layer above asserts against.

---

## 003: Models and network

`:core:model` and `:core:network`. The riskiest layer, so it goes first.

**Fixed already.** The three `DataError` causes. The v4 token in a Bearer header. Ktor on OkHttp
with `MockEngine` for tests. Ktor types never leave `:core:network`. A movie is identified by one
id, which carries the source that issued it. TMDB's page size is 20 and is not configurable.

**Settled.** The spec holds the decisions. Four of them change what the specs below inherit:

- `Genre` is an app-owned enum and each source maps its own ids onto it, so the genre endpoint is
  never called and there is no genre table.
- The source returns fully formed `Movie` and `MovieDetail` values, and a failure is a returned
  `NetworkResult` rather than a thrown exception.
- `ImageRef` holds a resolved URL for each of two app-owned sizes, so it stores as two columns and
  needs no converter.
- A `Rating` carries an average on one app-owned scale and an optional vote count. No rating is a
  null `Rating`, and each source resolves its own way of saying so.

**Cheapest test that can fail for the right reason:** `MockEngine` over recorded fixtures. Every
failure branch is a value the engine returns rather than a condition anyone arranges.

---

## 004: Persistence and the genre join

`:core:database`. Room is the single source of truth, so this is what the screens observe.

**Fixed already.** A movie table and a detail table, keyed on source and the source's id. Room types
never leave the module. Genres arrive already resolved to the app's
own enum, so nothing here translates them and the filter sheet reads the enum rather than a table.
An `ImageRef` is two columns per image.

**To settle.**

- Whether movie and detail are one table or two, and what a write does to the other.
- How a movie's genres are stored, given nothing ever queries by genre in SQL.
- What a refresh does to rows the new response no longer contains.

**Cheapest test:** in-memory Room under Robolectric, exercising the real query compiler.

---

## 005: Repositories, fetch policy, filter and sort

`:core:data`. The heaviest test cluster in the project.

**Fixed already.** Sequential paged requests, because ranking shifts between requests and
concurrent requests would multiply duplicates rather than avoid them. Deduplication is a pure
function in the data layer, driven by a fixture reproducing the observed boundary duplicates. The
10-request cap. A read that fails part way through fails whole and writes nothing. There is no
freshness window: both screens serve the cache and fetch regardless. Filter and sort are pure Kotlin
over the fetched set, not SQL: the rules live here, the selection lives in the view model.

**To settle.**

- The three repository surfaces, and how a `Flow` off Room composes with a suspending refresh.
- How a partial failure is expressed, so the UI can render a short list and name it incomplete
  without unpacking an exception.
- Whether the detail refetch is a separate call or a policy inside the read, given a background
  refetch failure must reach nobody while a cache-miss failure must reach the screen.
- Where the trim to exactly 100 sits relative to dedupe and sort.
- What lands in `:core:testing` as a fake, and what the fake's own tests assert.

**Non-negotiable tests:** dedupe against the boundary fixture, the request cap, a partial failure
keeping earlier pages, each sort key in each direction against a fixed fixture, nulls-last dates,
OR semantics with no repeated row, and filter and sort composing.

---

## 006: Trending screen

`:feature:trending`. Pull `Screen-TrendingList`, `Component-TrendingFrame` and
`Component-TrendingTwoPane` from the designs before the brainstorm, not after.

**Fixed already.** Seven states: first load, loaded, empty from filter, empty from API, partial page
failure, error with cache, error without cache. One immutable `UiState` with every outcome folded
in, so nothing reaches the UI through an event channel. Filter and sort apply live, with no Apply
button. Genre multi-select is OR. A summary row repays the cost of hiding state in a sheet. Filter,
sort and scroll survive rotation and process death, and reset on cold start. A zero-result filter is
an empty state, never an error. Skeletons, never a spinner on a blank screen.

**To settle.**

- The `UiState` shape: one sealed hierarchy, or a single class with nullable content plus a
  banner slot.
- What `SavedStateHandle` actually holds, and how scroll position gets there.
- How the sheet, the summary row and the badge stay in agreement about one selection.
- Which visual states earn a golden and which earn a screen test.
- The accessibility contract in concrete terms: what TalkBack announces when the result count
  changes, and how sort direction reads without colour.

**The bar, which is part of releasable rather than polish:** 48dp targets, a content description or
an explicit decorative mark on every image, text to 200% without clipping, AA contrast over posters.

Posters in goldens come from a fake image loader over a local drawable, never the network.

---

## 007: Movie detail screen

`:feature:detail`. Pull `Screen-MovieDetail` and `Component-DetailFrame` first.

**Fixed already.** Exactly three states: loading, loaded, error. No offline state, no stale state. A
cache-served render is an ordinary loaded screen, and so is one whose background refetch failed. The
rating block is omitted when there is no rating, and shows the average alone when the source
reports no vote count. Budget and revenue read "Not disclosed",
never "$0". Runtime reads "2h 37m". An empty tagline and an absent `imdb_id` remove their rows
rather than blanking them. The IMDB row always launches the browser.

**To settle.**

- Where the formatters live, given they are presentation and the data layer holds no user-facing
  copy.
- How the layout stays composed when the rating block, the tagline and both money rows are all
  absent at once, which is a real shape and not a hypothetical.
- Which of the three states the screen can be in while a background refetch is running, and how the
  answer stays "loaded".
- How the fact grid reflows when a row disappears.

Every layout is checked against *The Mongoose*, not only against a movie with every field present.

---

## 008: Navigation, adaptive layout, and release

`:app`, plus the handover README.

**Fixed already.** `AmroNavigator` owns the back stack and is the only thing that mutates it.
Features receive lambdas and know nothing about navigation. Two panes at expanded width only, keyed
to window size class. Nothing reads device type or orientation, so a phone in landscape
legitimately shows two panes. Back returns to the list with its state intact.

**To settle.**

- What the detail pane shows at expanded width before anything is selected.
- Whether selecting at expanded width touches the back stack at all.
- Which flow tests earn a device, given every layer below is already covered on the JVM.

**The README is a deliverable, not documentation.** Written as a handover to the developer
inheriting the code: project overview, the module map so a developer finds any feature in about
thirty seconds, how to run it with a token in `local.properties`, the test strategy in two minutes,
and every significant library with a one-sentence defence **and the alternative rejected for a
reason**. Ktor over Retrofit, Navigation 3 over Navigation Compose, Room over an HTTP disk cache,
fakes over a mocking library, reactive offline over `ConnectivityManager`.

It also states what the three post-MVP ambitions would cost, without having built any of them.
Being able to say the cost is the deliverable.

Then screenshots in light and dark, `./scripts/format.sh`, and clean runs of
`./scripts/unittest.sh` and `./scripts/instrumented.sh`.

---

## Where the open questions land

| Question | Spec |
|---|---|
| Where the genre join happens, and when the 19-entry list is fetched | Answered in 003: there is no join, and the list is never fetched |
| What is cached and in what form | 004 |
| How a developer runs the project without a token | Already settled: a `buildConfigField` defaulting to empty. Documented in 008 |
| Test strategy | Owned by `docs/testing.md` |
| How the paged requests are issued, and how a mid-sequence failure surfaces | 005 |
| Where deduplication lives | 005 |
| Where the freshness window lives, and how it is tested without waiting a day | Answered in 004: there is none |

---

## The cut list

Sixteen hours is tight for this scope. "Cut" means dropped from the MVP, not deferred: decided now,
so hour 14 is a lookup rather than a judgement call. Cut in this order, and stop the moment the
budget is back.

1. **Journeys.** The E2E layer never gates `check` and needs a token and a network. Costs nothing a
   flow test does not already cover.
2. **Feature screenshot goldens, down to representative states.** Keep loading, loaded, empty from
   filter and error on the list, and the complete and missing-data movies on the detail screen. Drop
   the form factor and font scale crosses.
3. **Flow tests, down to one navigation test and one offline test.**
4. **Pull to refresh.** The launch refresh exercises the same path.

**Never cut, because these are what is being assessed:** deduplication by id, the missing-data
omission rule, cached read with a full-screen error only when there is nothing to show, filter and
sort correctness against a fixture, the accessibility bar, and the README.

Anything from the out-of-scope list appearing in the build is a scope failure, not a bonus. Search,
watchlists, personalisation and prefetching stay out even if there is time.
