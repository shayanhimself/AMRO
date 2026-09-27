# AMRO Android MVP — Product Brief

**Date:** 2026-09-01
**Status:** Approved. Feeds design work and the technical spec session.
**API status:** TMDB verified live on 2026-09-01. Section 9.2 holds measured findings, not assumptions — one original assumption was wrong and changed the list spec.

---

## 0. How to read this

Three readers:

- **Owner (Shayan)** — everything.
- **Design agent** — Sections 3, 5, 6 and 7 are the working material. Section 7 states what is yours to decide.
- **Spec agent** — Sections 2, 4, 5, 6, 8 and 9. Use Section 8 as the constraint set when brainstorming architecture.

This brief defines **what** and **why**. It does not define **how**. No architecture, module layout, or library choices appear here — those belong to the spec session. Where the product puts pressure on architecture, Section 8 names the pressure and stops.

---

## 1. Context

AMRO — Advanced Movie Recommendation Organisation — is a start-up entering the movie data space. Its pitch: personalised movie recommendations plus detailed movie information, in an app that feels good to use. It needs a Minimum Viable Product quickly, to convince early adopters and investors that the platform can deliver an engaging movie experience.

AMRO's stated plans after the MVP:

- Hire **multiple feature teams**, each owning a feature set added on top of the existing base — actor info, user profiles, streaming, social.
- Offer features **offline**.
- Collect data from **multiple API sources**.

---

## 2. Success criteria

- **P1** — A user opens the app and sees what is trending this week on the first screen, with no setup, account, or onboarding.
- **P2** — A user narrows 100 movies to the ones matching their mood in two taps or fewer.
- **P3** — A user can decide whether to watch a film without leaving the app.
- **P4** — The app stays useful on a bad or absent network.
- **P5** — The app feels crafted enough that a user would show it to someone.

**Non-criteria.** Feature count, invented functionality, and visual maximalism do not improve the product. Restraint that can be explained beats surface area that cannot.

---

## 3. User and jobs

One persona, no segmentation: **someone deciding what to watch tonight.** No account, no history, no stated preferences. Opens the app cold. Has minutes, not hours.

Jobs to be done:

- **J1** — "Show me what people are watching this week." → trending list
- **J2** — "Only the kinds of film I'm in the mood for." → genre filter
- **J3** — "Order it the way I think about films." → sort
- **J4** — "Tell me enough to commit two hours." → detail screen
- **J5** — "Let me look it up elsewhere." → IMDB link
- **J6** — "Don't die on the train." → offline read

**Explicit anti-job:** the MVP does not know who the user is. It shows what is globally trending. See Section 4.2.

---

## 4. Scope

### 4.1 In scope

- Two screens: **trending list** and **movie detail**
- Genre filter over the fetched top 100
- Sort by popularity, title, release date — ascending or descending
- Offline read of previously fetched data, detected reactively (see Section 6.1)
- Full state coverage: loading, loaded, empty-from-filter, empty-from-API, error with cache, error without cache
- Adaptive layout driven by window size class, not device or orientation (see Section 6.5); light and dark themes
- Automated tests plus a stated, defensible test strategy
- README **written as a handover document**. Covers project overview, libraries used, and choices made. Written for the developer inheriting the code, not as a description of it.

### 4.2 Out of scope (deliberate non-goals)

- **Personalised recommendations.** AMRO's vision names them; the requirements do not. The MVP shows *global* trending. Naming this gap honestly is worth more than a faked "For you" row.
- Search, actor info, user profiles, streaming links, social features, watchlists, user ratings
- Pagination beyond the top 100 — the set is exactly 100 items
- Prefetching details for movies the user never opened (see Section 6.1)
- Authentication, push notifications, analytics, localisation beyond English
- Tablet-exclusive features beyond the two-pane layout in Section 6.5

Anything from this list appearing in the build is a scope failure, not a bonus.

---

## 5. Screens

### 5.1 Trending list

**Purpose:** J1, J2, J3. This is the launch screen. Nothing precedes it — no splash content, no onboarding.

**Data set:** this week's top 100 trending movies — 100 *distinct* movies.

The API does not hand this over in one call, and the naive reading of it is wrong. Verified against the live endpoint on 2026-09-01 (see Section 9.2):

- `/trending/movie/week` returns **20 items per page**. One hundred movies means paging.
- **Paging is unstable.** TMDB re-ranks between requests, so a title sitting on a page boundary is served twice. Five pages returned 100 rows but only **92 distinct movies** — eight duplicates, every one of them on two adjacent pages.
- Therefore: fetch pages in order, **deduplicate by movie id**, and keep fetching until 100 distinct movies are held. Six pages were needed on the day of writing (109 distinct), then trimmed to exactly 100.
- The page budget is **capped at 10 requests**. If the cap is hit before 100 distinct movies, the app shows what it has rather than looping — the count is honest, not padded.
- Duplicates must be removed before anything else touches the list. Duplicate ids in a lazy list are a visible defect, not a cosmetic one.

**Content per item** (minimum): title, image, genre(s).
**Hierarchy:** the poster carries recognition and gets the visual weight; title second; genres tertiary.

**Controls**

- App bar: the **screen name** ("Trending"), and a **Filter** action carrying a count badge when any filter is active. The app bar names where the user is, not what the app is called.
- **Filter bottom sheet**, holding both narrowing controls in one place:
  - *Genre* — multi-select, **OR** semantics. Nothing selected means everything.
  - *Sort key* — Popularity (default), Title, Release date. Single-select.
  - *Direction* — Descending (default), Ascending.
  - Changes apply **live** as they are made; there is no Apply button. A **Reset** action clears everything.
- **Summary row**, shown below the app bar whenever a filter is active: the selected genres as removable chips, plus a result count ("32 of 100"). This exists to repay the cost of hiding state inside a sheet.

**Behaviours**

- Filtering and sorting happen **client-side over the already-fetched 100**. No refetch, no server re-query. Filtering for comedy shows the comedies that are in the top 100 — it does not go looking for a hundred comedies.
- **The default view is sorted by popularity, descending, applied client-side.** This is deliberate and worth knowing: TMDB's trending rank is *not* popularity order. Concatenating five pages produced 48 monotonicity violations against the `popularity` field. Popularity descending is the product's default sort, so the app applies it — which means the first screen deliberately re-orders TMDB's own trending ranking. The alternative is in Section 9.1 D12.
- Multi-select genre is OR: a movie appears if it carries any selected genre, and appears once.
- Sort by title is locale-aware and case-insensitive.
- Sort by release date places missing dates last in both directions.
- Sort by popularity uses the value TMDB supplies.
- Filter and sort compose: filter first, then sort.
- Selecting an item opens the detail screen.
- Pull-to-refresh re-fetches the 100 and preserves the active filter and sort.
- Filter, sort, and scroll position survive rotation and process death. They **reset on cold start**.
- A refresh keeps the current selection. A selected genre absent from the new set simply matches nothing; if the result is empty, the empty-from-filter state applies.

**States**

| State | Treatment |
|---|---|
| First load | Skeleton placeholders shaped like the final rows. Not a spinner on a blank screen. |
| Loaded | The list. |
| Empty from filter | Message naming the situation ("No comedies in this week's top 100") plus a **Clear filters** action. This is **not** an error. |
| Empty from API | The API returned zero items. Distinct copy, Retry action. |
| Partial page failure | One of the paged requests fails after others succeeded. The movies already retrieved render, with the inline bar naming the incomplete result and offering Retry. A partial list beats an empty screen. |
| Error, no cache | Full-screen error with cause-appropriate copy and a single Retry. |
| Error, cache present | The cached list renders and stays fully interactive, with a non-blocking inline bar naming the reason and offering Retry. Never a blank screen when a cache exists. |

**Acceptance criteria**

- **AC-L1** — 100 distinct movies render, each with title, image, and genre name(s) resolved from `genre_ids`.
- **AC-L2** — Selecting genre G shows exactly the subset of the 100 carrying G; the count in the summary row matches.
- **AC-L3** — Selecting G1 and G2 shows movies carrying G1 or G2, each exactly once.
- **AC-L4** — Each sort key in each direction produces the correct ordering, verified against a fixed fixture.
- **AC-L5** — Filter and sort combine correctly in either order of application.
- **AC-L6** — Rotation preserves filter, sort, and scroll position.
- **AC-L7** — A zero-result filter shows the empty state with a clear action, never an error.
- **AC-L8** — With a cache present and no network, the list renders and stays interactive, with the inline bar and a working Retry. Filter and sort still operate on the cached data.
- **AC-L9** — With no cache and no network, the error state appears with Retry, and Retry succeeds once the network returns.
- **AC-L10** — Given paged responses containing repeated movie ids, the rendered list contains no duplicate id. Verified with a fixture reproducing the observed boundary duplicates.
- **AC-L11** — The list holds exactly 100 distinct movies when the API supplies them within the 10-request cap; when it does not, it holds every distinct movie retrieved and the count reported to the user matches what is shown.
- **AC-L12** — The default ordering on first render is popularity descending, not the order the API returned.

### 5.2 Movie detail

**Purpose:** J4, J5. Reached only by selecting an item in the list.

**Required fields:** title, tagline, image, genre, description, vote average, vote count, budget, revenue, status, IMDB link, runtime, release date.

**App bar:** back navigation plus the **movie title**. Same principle as the list — the bar names the screen the user is on.

**Presentation rules**

- The image leads the screen.
- Title and tagline read as one unit. If the tagline is empty — TMDB often leaves it blank — it is omitted entirely, not rendered as an empty slot.
- The description is the primary reading content.
- Vote average and vote count present as a single unit. The count is shown whenever the source reports one, because a rating with its sample size says more than a rating without. A source that publishes a score and no count shows the score alone.
- **A rating is shown when there is one, and the block is omitted entirely when there is not** — no stars, no "0.0", no "0 votes". TMDB reports an unrated film as a `vote_average` of 0.0 with a `vote_count` of 0 rather than as an absent field, so an unrated film reads as unrated rather than as a film scoring zero. This is not rare: 11% of the sampled top 100 had no votes at all, because unreleased films trend hard. Showing "0.0 ★" on an unreleased film states something false about it.
- Budget and revenue are formatted as grouped currency. A zero or missing value reads "Not disclosed", never "$0". This carries real weight: **38% of the sampled top 100 had both budget and revenue at zero.**
- Runtime reads "1h 47m", not "107".
- Release date uses a localised long form.
- Status is shown as TMDB supplies it (Released, Post Production, and so on).
- Genres render as non-interactive chips. Tapping a genre to filter is out of scope.
- The IMDB row opens the external browser. If `imdb_id` is absent, the row is omitted.

**Missing-data rule (applies to every field):** a field with no value is omitted, never rendered blank, as `null`, or as a zero. A movie with missing data must still look like a designed screen.

This rule is load-bearing, not defensive polish. Measured across all 92 distinct movies in a live top-100 sample (Section 9.2): budget empty **44%**, revenue **41%**, both together **38%**, tagline **19%**, votes **11%**, runtime **5%**. A detail screen that renders raw values is wrong for roughly two in five trending movies.

**Missing-data fixture for design and tests:** *The Mongoose* (id `1294189`) — status In Production, empty tagline, budget 0, revenue 0, zero votes. Every design must be checked against this shape, not only against a movie with every field populated.

**States: exactly three — loading (skeleton), loaded, error.** There is no offline state and no stale state on this screen.

- **Loading** — no cache, request in flight.
- **Loaded** — content renders. A cache-served render is an ordinary loaded screen; so is one whose background refetch failed.
- **Error** — only when there is nothing to show: an uncached movie that failed to load, or a tapped Retry that failed again with no cache behind it. Never while valid cached content is on screen.

**A failed background refetch is silent.** The cached facts on screen are still correct, so nothing is asserting anything false — and the user did not ask for the refresh in the first place. It is retried the next time the movie is opened. See Section 6.1 and D18.

Back navigation returns to the list with its state intact.

**Acceptance criteria**

- **AC-D1** — All thirteen required fields render when present.
- **AC-D2** — A movie with missing data (no tagline, no `imdb_id`, budget 0, no runtime) renders with no blank rows and no zero values.
- **AC-D3** — The IMDB link opens the external browser; an absent id produces no row.
- **AC-D4** — A previously visited movie opens from cache with no network and renders as a normal loaded screen: no bar, no error, and the failed fetch behind it reaches nobody.
- **AC-D5** — A never-visited movie with no network shows the error state with Retry, because a request was attempted and failed.
- **AC-D6** — Back returns to the list with filter, sort, and scroll position intact.
- **AC-D7** — A movie TMDB reports as unrated (`vote_count` 0) renders with no rating block at all, one whose source gives a score but no count renders the score without a vote line, and one with budget and revenue 0 renders "Not disclosed" rather than a zero currency value.
- **AC-D8** — A cached detail renders immediately and triggers a background refetch; a record cached while unreleased shows its rating and revenue once refreshed.
- **AC-D9** — When that background refetch fails, the screen stays in the loaded state: no bar, no error, cached content intact. Opening the movie again re-attempts it.

### 5.3 Navigation

List → detail → back. That is the entire graph. Deep links are out of scope. On expanded widths the two panes coexist; see Section 6.5.

---

## 6. Cross-cutting behaviour

### 6.1 Offline

**Offline is detected reactively, and only reactively.** The app never asks the operating system whether a network exists. It knows a request failed because it made the request and the request failed. There is no proactive connectivity check anywhere in the MVP.

This is a deliberate constraint, and it decides the behaviour of every screen below. Two consequences follow directly:

- A screen that makes no request cannot know it is offline, and therefore must not claim to be.
- The app **never refuses to make a request** on the grounds that it believes it is offline. It always attempts, and lets the attempt decide. A successful response clears any offline state immediately.

**Trending list — always refresh, never block.**

- A request is issued on every launch.
- If a cache exists it renders immediately, without waiting. A cold, slow network must not produce a blank screen when usable content is on hand.
- Refresh succeeds → the list updates. Only genuinely changed items change on screen; the list is keyed by movie id so unchanged rows are untouched.
- Refresh fails **with** a cache → the cached list stays fully interactive, and a **non-blocking inline bar** names the reason and offers Retry. Filter and sort keep working on the cached data.
- Refresh fails **without** a cache → the full error state, with reason and Retry.

The rule in one line: **the full-screen error is only for when there is nothing to show.** When content exists, failure degrades to a bar and never destroys it.

**Movie detail. Always fetch, and render the cache meanwhile.**

- Cache present → render it immediately and fetch anyway. An answer replaces what is on screen, and no offline affordance is shown.
- Cache absent → fetch. On failure, the screen's error state with Retry.
- That background fetch **fails silently**. No bar, no error, cache keeps rendering, retried on next open.

Fetching on every open rather than after a freshness window is what keeps an unreleased film correct. 11% of the live top 100 were unreleased (Post Production, In Production, Planned), and those records genuinely change: a film is released, gains votes, gains revenue. A cache that is never refreshed shows "no rating" and "Not disclosed" for them permanently. The cost is one request on a screen the user explicitly asked for, and it needs no stored timestamp, no clock and no window to explain.

**Why the two screens differ.** The governing rule is *feedback follows the claim, not the request*: **a failure is surfaced when the content on screen would otherwise assert something false; it is silent when the content stays true.** A trending list is a claim about *now* — "this week's top 100" — so a stale one misleads and earns the bar. A movie's runtime, budget and release date are facts, not claims about now, so a detail that failed to refresh is still correct and has nothing to report. The same rule produces a bar on one screen and silence on the other.

**Both screens**

- Details for movies the user has not opened are **never** prefetched.
- Images are cached by the image layer. A missing image gets a deliberate placeholder, never a broken frame.
- The IMDB link always opens the external browser. **Offline handling there is the browser's job, not the app's** — the browser already has an offline page, and intercepting it would require exactly the proactive connectivity check this section rules out.

**Not in the MVP:** prefetching, storage budgets, background sync scheduling, and expiry of any kind. Section 8.3 says where they would go.

### 6.2 Errors

Three causes must be distinguishable to the user, with three different messages: no connectivity, server or API error, unexpected empty response. No raw exception text, HTTP status code, or stack trace ever reaches the screen. Every error offers exactly one primary action (Retry) and never dead-ends. An error that occurs while a cache exists degrades to the inline bar and never replaces content. The detail screen shows an error only when it actually made a request and that request failed — never when it is serving cache.

### 6.3 Loading

Skeletons shaped like the real content. No blank screens, and no full-screen spinner covering content that already exists. A refresh over existing content shows an inline indicator while the content stays visible and interactive.

### 6.4 Accessibility bar

Non-negotiable, and part of "releasable":

- Every interactive target is at least 48dp.
- Every image carries a content description or is explicitly marked decorative.
- Text scales to 200% without clipping or overlap.
- Text over posters and backdrops meets WCAG AA contrast; scrims are applied where needed.
- Full TalkBack traversal works. The filter sheet announces selection state, and the result count is announced when it changes.
- No meaning is carried by colour alone — sort direction and active filters must also be legible without colour.

### 6.5 Adaptive layout

Layout is driven by **window size class**, never by device type or orientation. The app asks how much window it has, not what it is running on.

- **Compact width** — single pane. The list navigates to the detail screen.
- **Medium width** — single pane. This is the Material 3 standard pane directive, which permits two horizontal partitions only at expanded width. We accept the platform default rather than overriding it with the dense directive; a list of posters plus a detail pane is cramped at medium.
- **Expanded width** — two panes. List on the left, detail on the right. Selecting an item updates the right pane instead of navigating.

**A phone in landscape is not a special case.** It reports roughly 800–930dp of width, so depending on the device it lands in medium or expanded and may legitimately show two panes. That is correct behaviour, not a defect. Nothing in the design may key off orientation.

**Deliverable:** a couple of designs showing the two-pane tablet layout. Phone designs cover the rest.

---

## 7. Design constraints


**Hard constraints**

- Material 3 is the baseline system. Deviations are allowed but must be deliberate and applied consistently.
- Poster art is the dominant source of colour on screen. The chrome must not compete with it.
- Every state listed in Sections 5 and 6 needs a designed treatment. A design covering only the happy path is incomplete.
- Light and dark are both first-class and both designed. Auto-inversion is not a design.
- Density matters: the list must show enough items at once to feel like a top 100, not two hero cards.
- Image geometry is fixed by the source: **posters are 2:3, backdrops 16:9**, served from `https://image.tmdb.org/t/p/` at poster widths `w92/w154/w185/w342/w500/w780/original` and backdrop widths `w300/w780/w1280/original`. Designs must compose against those ratios.
- Every layout must be checked against the missing-data fixture in Section 5.2, not only against a movie with all fields present.

**Yours to decide:** palette, type scale, spacing system, card composition, motion and transitions, and whether empty states are illustrative or typographic.

---

## 8. Post-MVP horizon — pressure, not scope

**Build none of this.** The base must simply not make it expensive. This section exists so the spec session designs against real forces instead of imagined ones.

### 8.1 Many feature teams

AMRO intends parallel teams owning actor info, user profiles, streaming, and social, each added on top of the existing base.

*Pressure:* a new feature should be addable without editing existing features; teams should not serialise on shared files; a feature's whole surface should be findable in one place. How — modularisation strategy, navigation ownership, dependency boundaries — is the spec session's decision.

### 8.2 Multiple API sources

TMDB is the first source, not the only one.

*Pressure:* the app's idea of a movie must not be TMDB's JSON shape. Adding or swapping a source should not require touching UI or presentation logic. How — domain model, mapping boundary, source abstraction — is the spec session's decision.

*Requirement:* **a source that feeds the trending list must be able to order its own catalogue.** It does not have to publish a popularity number, a scale, or a unit. A ranked list is enough, and each source maps whatever it does publish, a score, a watcher count, or a position, onto the app's own ordering. This is deliberately the weakest requirement that still delivers the default sort: across thirteen movie APIs surveyed, eleven can order a list while only six publish a number, and no two of those six are on the same scale. A source that cannot order its catalogue can still serve movie details.

### 8.3 Deeper offline

The MVP does cached read (Section 6.1). Later comes prefetching, expiry, background sync, and eventually offline-first writes.

*Pressure:* today's caching should live in a deliberate layer that owns the question "where does this data come from", not as logic bolted inside a screen.

---

## 9. Decisions and assumptions

### 9.1 Decisions taken

| # | Decision | Defence | Rejected alternative |
|---|---|---|---|
| D1 | Multi-select genre, OR semantics | People pick by mood, not by a single genre; the sheet scales to future facets | Single-select — simpler, but it forces one mood at a time and does not scale to further facets |
| D2 | Filter and sort together in one bottom sheet | One place for all narrowing; scales to year/rating later; keeps the app bar clean | Chip row plus a sort menu — more discoverable, but clutters the bar and does not scale |
| D3 | Summary row with active chips and result count | Repays the cost of hiding state inside a sheet | Badge only |
| D4 | Live apply, no Apply button | Fewer taps, and the result count updates as choices are made | Apply button — clearer commit point, one more tap |
| D5 | Filter/sort reset on cold start | Trending is a weekly fresh-look feed; a stale filter misleads | Persist indefinitely |
| D6 | Client-side filter and sort over the fetched 100 | The hundred are already in memory, so narrowing them costs a pass over a list rather than a round trip | Server-side re-query |
| D7 | Cached read, with a full-screen error only when there is nothing to show | Failure must never destroy content the user can still use; a bar over a live list is honest without being destructive | Full-screen error on every failure (throws away usable content); silent failure (user believes stale data is current) |
| D8 | Omit missing fields entirely | A movie with missing data must still look designed | Placeholders or "N/A" rows |
| D9 | No personalisation in the MVP | The requirements do not ask for it; naming the gap beats faking it | A fabricated "For you" row |
| D10 | Two-pane layout at expanded width only, following the Material 3 standard pane directive | A real adaptive answer keyed to window size class rather than device or orientation, and it takes the platform default rather than inventing a breakpoint | The dense directive, which also splits at medium width — more panes sooner, but a poster list beside a detail pane is cramped there; a stretched single pane at all widths |
| D11 | Page until 100 **distinct** movies, deduplicating by id, capped at 10 requests | The list is specified as 100 movies, and unstable paging means five pages deliver only ~92. The cap keeps a pathological response from looping | Fetch a fixed five pages and show the ~92 that survive dedupe — simpler and fully deterministic, but silently delivers fewer than the hundred that were asked for |
| D12 | Default order is popularity descending, applied client-side | Popularity descending is the specified default sort, and applying it makes the first render deterministic and testable against a fixture | Preserve TMDB's trending rank until the user sorts — truer to the word "trending", but then the default view matches no available sort option and the stated default goes unmet on first paint |
| D13 | Authenticate with the v4 Read Access Token in an `Authorization: Bearer` header | Same access as the v3 key, but the credential never enters a URL, so it cannot leak through logs, crash reports, or proxy history | v3 `api_key` query parameter — marginally simpler, worse hygiene |
| D14 | Show a rating whenever the source reports one, with its vote count where the source reports that too, and omit the block entirely otherwise. TMDB's 0.0 with zero votes means unrated | 11% of trending films are unreleased and unrated, and "0.0 ★" asserts something false about them. Meanwhile four of twelve movie APIs surveyed publish a score with no vote count at all, so requiring a count would discard a rating those sources genuinely have | Requiring the count, which throws away real ratings from a third of the sources surveyed; rendering 0.0 with a zero count, which is the false claim above |
| D15 | Offline is detected **reactively only** — a request failed | The OS reporting "connected" does not mean the internet works (captive portals, DNS blackholes), and an app that refuses to try because it believes it is offline turns a false reading into a permanent outage. The last real request outranks any system flag | Observing `ConnectivityManager` — enables proactive affordances such as pre-disabling the IMDB row, at the cost of a signal that lies in both directions |
| D16 | The IMDB link always launches the browser; offline handling is delegated there | The browser already has an offline page, and intercepting would require the proactive check ruled out in D15 | Disable or annotate the row when offline |
| D17 | A cached detail renders immediately and is refetched on every open, and that refetch fails silently | 11% of trending films are unreleased and their votes, revenue and status genuinely change, so a cache that is never refreshed stays wrong for them. Refetching every time costs one request on a screen the user asked for, needs no stored timestamp, no clock and no window to explain, and is the shape the trending list already has | A 24-hour freshness window, which saves that one request and pays for it with a column, an injected clock and a rule; never refetching, which is permanently wrong for unreleased films; refetching only records with status != Released or zero votes, which is a second rule to explain; surfacing the failed refetch in a bar, which reports a refresh the user never asked for and offers a Retry whose success they cannot perceive |
| D18 | A failure is surfaced when the content left on screen would otherwise assert something false, and is silent when that content stays true. Where nothing is on screen, the failure is always surfaced | Feedback follows the claim the screen makes, not who initiated the request. "This week's top 100" is a claim about *now*, so a stale list misleads and earns the bar even though the app asked for that refresh itself; a movie's runtime and budget are facts, so a detail that failed to refresh is still correct and has nothing to report | Intent-based — surface only what the user asked for. Agrees on the detail screen but contradicts the list, whose launch refresh is app-initiated yet must still report staleness. One affordance everywhere — consistent, but noisy where nothing is wrong |

### 9.2 Verified API findings

All of the below were confirmed against the live TMDB API on **2026-09-01** using the v4 read access token. The original assumptions are kept with their verdicts, because one of them was wrong in a way that changes the spec.

| # | Original assumption | Verdict | What is actually true |
|---|---|---|---|
| A1 | Trending supplies at least 100 items for the weekly window | **WRONG** | `/trending/movie/week` returns **20 per page**; `total_pages` 500, `total_results` 10000. One hundred movies is a paged operation, and paging is unstable — see below. |
| A2 | Trending carries genre ids requiring a join | **CONFIRMED** | List items carry `genre_ids` only. `/genre/movie/list` returns **19 genres**. **18 of the 19** appeared in the live top 100; only "TV Movie" was absent. |
| A3 | `imdb_id` is details-only | **CONFIRMED** | Absent from list items, present on `/movie/{id}`. Populated for 100% of the sample, but still treated as optional. |
| A4 | The key must not sit in plain source | **RESOLVED** | The v4 read access token lives in `local.properties`, which is gitignored. See D13 and Q3. |
| A5 | Adult filtering uses the TMDB default | **CONFIRMED** | The trending endpoint does not accept `include_adult` at all, so it is filtered server-side before the app sees it. Every one of the 100 list items and 92 details came back `adult: false` and `softcore: false` (the latter an undocumented flag TMDB now sends). Neither field enters the domain model — nothing reads them, and a flag that is never checked is not a safeguard. No toggle, no client-side filtering. |

**Page size is not configurable.** The reference for `/trending/movie/{time_window}` documents only `time_window` (path) and `language`. Seven undocumented variants were probed against the live endpoint — `page_size`, `per_page`, `limit`, `count`, `results_per_page`, `size`, and `page_size` combined with `page` — and **every one was silently ignored**: 20 results, `total_pages` 500, in all cases. Twenty per page is a platform constraint, not an overridable default, so paging is unavoidable. (The reference does not document `page` for this endpoint either, though it works.)

**Unstable paging — the finding that changes the design.** Five sequential page requests returned 100 rows containing only **92 distinct movies**. Eight titles were duplicated, and every duplicate pair sat on two *adjacent* pages — the signature of a ranking that shifts between requests. Reaching 100 distinct movies took **six pages** (109 distinct, trimmed to 100). Deduplication by id is mandatory. See D11 and AC-L10.

**Trending rank is not popularity rank.** Across the concatenated 100 rows, the `popularity` field violated descending order **48 times**. Sorting by popularity therefore visibly re-orders the API's own trending list. See D12.

**Genre distribution in a live top 100** — filtering has real material to work with:

Thriller 34 · Comedy 33 · Action 32 · Adventure 24 · Drama 23 · Science Fiction 22 · Horror 21 · Crime 18 · Fantasy 16 · Animation 13 · Romance 11 · Family 10 · Mystery 10 · War 5 · History 4 · Western 1 · Documentary 1 · Music 1

Movies carry between 1 and 6 genres (median 3), and **no movie had zero genres**. Multi-select with OR semantics (D1) is the right call: with a median of three genres per film, AND semantics would return almost nothing.

**Missing data across all 92 distinct movies:**

| Field | Empty, null, or zero |
|---|---|
| `budget` | 44% |
| `revenue` | 41% |
| both budget and revenue | 38% |
| `tagline` | 19% |
| `homepage` | 17% |
| `vote_count` and `vote_average` (always together) | 11% |
| `runtime` | 5% |
| title, overview, poster, backdrop, genres, status, release_date, imdb_id | 0% |

**Why so much is missing:** `status` across the sample was Released 83, Post Production 6, In Production 2, Planned 1. Unreleased films trend hard, and they have no box office and no votes. Missing data is the normal case here, not an edge case.

**Images:** base `https://image.tmdb.org/t/p/`. Posters `w92, w154, w185, w342, w500, w780, original` (2:3). Backdrops `w300, w780, w1280, original` (16:9).

### 9.3 Questions for the spec session

Left open here, and settled by the specs. The answer sits beside each, with the spec that owns it.

- **Q1** — Where does the genre-id-to-name join happen, when is the 19-entry genre list fetched, and is it cached separately from the trending list? It is required for both list rendering and the filter sheet.
  **003. There is no join, and the list is never fetched.** `Genre` is an enum the app owns, and
  each source maps its own ids onto it at its own mapper, so a movie carries app genres by the time
  it leaves the network layer. The genre endpoint is not called in production and no genre list is
  stored. The filter sheet lists every genre with no fetch, no cache dependency and no empty-state
  race, and an id outside the table is dropped, so every chip on a screen is one the sheet offers.
- **Q2** — What exactly is cached, and in what form?
  **004. Two Room tables**, movie summary and movie detail, each keyed on the movie's id, which
  carries the source that issued it. The detail table carries its own copy of the summary columns,
  so a movie the user opened keeps its detail after the trending set has moved on. Genres store as a delimited list of
  the app's own constants: there is no genre table and no cross reference, because nothing queries
  by genre in SQL. Neither table stores a timestamp.
- **Q3** — `local.properties` holds the token locally and is gitignored, so a clean clone will not build without it. How does a developer run the project — a documented placeholder, a build-time default, or README instructions? This must be settled before delivery, or a fresh clone cannot run the app.
  **001, through the plugin in 003. A `buildConfigField` defaulting to empty.** The `amro.api.tokens`
  convention plugin reads `local.properties` first and each source's environment variable second,
  and either absent yields an empty string, so a clean clone builds and runs. The app then fails its
  requests and shows the error state rather than failing at the Gradle stage, and the README names
  the two-line fix.
- **Q4** — Test strategy: what earns a unit test, what earns a UI test, what earns a screenshot test, and what earns nothing?
  **Owned by `docs/testing.md`.** Five layers, and every check goes to the cheapest layer that can
  still fail for the right reason, and to exactly one layer.
- **Q5** — How are the up-to-ten paged requests issued — sequentially, or in bounded parallel — and how does a mid-sequence failure surface as the partial-failure state in Section 5.1?
  **003. Sequentially, inside the remote source, and a mid-sequence failure fails the whole call.**
  The walk continues on distinct movies held rather than on pages fetched, and ends on the count, on
  an empty page, or on the ten-request cap. Ranking shifts between requests, so pages have to resolve
  in order and concurrent ones would multiply the duplicates rather than avoid them. A caller cannot
  tell a list cut short by a failure from one that is simply short, so no partial list is returned,
  and nothing can now produce the partial page failure state in Section 5.1.
- **Q6** — Where does deduplication live, so that it is unit-testable against a fixture reproducing the observed boundary duplicates rather than buried in a UI layer?
  **003. In the remote source**, as a pure function over pages keyed on the movie's id. It is driven
  by a fixture assembled from recorded rows that repeats three across a page boundary, so it fails
  for the right reason on every run rather than only on a day the provider happened to re-rank.
- **Q7** — Where does the 24-hour detail freshness window live, and what makes it testable without waiting a day?
  **004. There is no window.** Both screens render whatever is cached and issue their request
  regardless, so nothing stores a moment, the app holds no clock, and no test has to move time.
