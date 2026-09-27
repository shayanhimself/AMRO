# AMRO — TMDB Sample Data

Real data pulled from the live TMDB API on **2026-09-01**, for use when designing the
trending list and movie detail screens. Nothing here is invented: every title, number,
empty field and image path is what the API actually returned.

Designs must be checked against the awkward rows in this file, not only the tidy ones.
Roughly two in five trending movies are missing budget and revenue, one in five has no
tagline, and one in ten has no rating at all.

Companion to `000-product-brief.md`.

---

## 1. Image URLs

Base: `https://image.tmdb.org/t/p/`

Build a URL as `base + size + file_path`, where `file_path` is the `poster_path` or
`backdrop_path` from the data below (it already begins with `/`).

### Posters — 2:3

| Size | Rendered px | Use |
|---|---|---|
| `w92` | 92 × 138 | tiny thumbnail |
| `w154` | 154 × 231 | small list row |
| `w185` | 185 × 278 | list row |
| `w342` | 342 × 513 | grid cell, large row |
| `w500` | 500 × 750 | detail poster |
| `w780` | 780 × 1170 | tablet detail |
| `original` | source | never use in a list |

### Backdrops — 16:9

| Size | Rendered px | Use |
|---|---|---|
| `w300` | 300 × 169 | thumbnail |
| `w780` | 780 × 439 | phone detail header |
| `w1280` | 1280 × 720 | tablet detail header |
| `original` | source | never use in a list |

### Worked example

**Spider-Man: Brand New Day**

- `poster_path`: `/bjiS5ipwxb9JFy3XRRN4OAilSeX.jpg`
- `backdrop_path`: `/7iwUUcKURMT7aKfCwMy6YnGtchD.jpg`

Full poster set:

```
https://image.tmdb.org/t/p/w92/bjiS5ipwxb9JFy3XRRN4OAilSeX.jpg
https://image.tmdb.org/t/p/w154/bjiS5ipwxb9JFy3XRRN4OAilSeX.jpg
https://image.tmdb.org/t/p/w185/bjiS5ipwxb9JFy3XRRN4OAilSeX.jpg
https://image.tmdb.org/t/p/w342/bjiS5ipwxb9JFy3XRRN4OAilSeX.jpg
https://image.tmdb.org/t/p/w500/bjiS5ipwxb9JFy3XRRN4OAilSeX.jpg
https://image.tmdb.org/t/p/w780/bjiS5ipwxb9JFy3XRRN4OAilSeX.jpg
https://image.tmdb.org/t/p/original/bjiS5ipwxb9JFy3XRRN4OAilSeX.jpg
```

Full backdrop set:

```
https://image.tmdb.org/t/p/w300/7iwUUcKURMT7aKfCwMy6YnGtchD.jpg
https://image.tmdb.org/t/p/w780/7iwUUcKURMT7aKfCwMy6YnGtchD.jpg
https://image.tmdb.org/t/p/w1280/7iwUUcKURMT7aKfCwMy6YnGtchD.jpg
https://image.tmdb.org/t/p/original/7iwUUcKURMT7aKfCwMy6YnGtchD.jpg
```

**Every movie in the live top 100 had both a poster and a backdrop.** A placeholder is still required — `poster_path` and `backdrop_path` are nullable in the API and the app must not render a broken frame.

---

## 2. Trending list — 20 movies

In the app's default order: popularity descending. This is what the first screen shows.

The list payload carries `genre_ids` only; the names below are resolved against the genre table in section 4.

| # | Title | Genres | Popularity | Release date | `poster_path` |
|---|---|---|---|---|---|
| 1 | Spider-Man: Brand New Day | Science Fiction, Action, Adventure | 1028.0 | 2026-07-29 | `/bjiS5ipwxb9JFy3XRRN4OAilSeX.jpg` |
| 2 | The Odyssey | Adventure, Action, Fantasy | 585.9 | 2026-07-15 | `/5rhTDKUhPYvpdQIijFIs5VoWsON.jpg` |
| 3 | Mutiny | Action, Thriller | 413.2 | 2026-08-19 | `/pu2VxGlpGwffOx292w18b1tv96j.jpg` |
| 4 | The Last Sunrise | Romance, Drama | 342.0 | 2026-08-26 | `/3PWJqDfygN0YNNjWsDUOXclCp3h.jpg` |
| 5 | Grand Theft Auto VI: An Extended Look | Animation, Action, Adventure, Crime | 318.6 | 2026-08-27 | `/xTZuh9ziUjIyHBWO9OvqNIPqVWe.jpg` |
| 6 | Facing El Chapo | Crime, Action, Thriller | 306.4 | 2026-08-21 | `/z8eF0ACFFKtIZ4pUeo02PCzxRVO.jpg` |
| 7 | Toxic: A Fairy Tale for Grown-ups | Action, Crime, Drama | 283.5 | 2026-08-26 | `/oiIPU4lvnI0Ag2K9cyAi44eCaoE.jpg` |
| 8 | Obsession | Horror, Thriller | 232.2 | 2026-05-13 | `/bRwnj8WEKBCvmfeUNOukJPwB43K.jpg` |
| 9 | Pinocchio: Unstrung | Horror, Fantasy, Mystery | 231.4 | 2026-07-22 | `/eUJXk3bTvLBi5Zcb0BCedZU7lVL.jpg` |
| 10 | The Whisper Man | Crime, Drama, Thriller | 227.7 | 2026-08-27 | `/6UqflU8Qqkz7Dq4swJPqs0ZJjY4.jpg` |
| 11 | Toy Story 5 | Animation, Family, Comedy, Adventure | 213.4 | 2026-06-17 | `/sfQtVlIHljToOwYjhe21KPGzZWK.jpg` |
| 12 | Moana | Family, Fantasy, Comedy, Adventure | 176.9 | 2026-07-08 | `/zKVgiv5qHCvCLT4A2ymJi5QeXDH.jpg` |
| 13 | Colony | Action, Horror, Science Fiction | 176.5 | 2026-05-21 | `/tN799oUR0f1gUKDYdMNrDaY7I51.jpg` |
| 14 | Minions & Monsters | Adventure, Animation, Comedy, Family, Fantasy | 176.4 | 2026-06-24 | `/4LwvU9SZc8QQzW1X1FAPhNbXnEU.jpg` |
| 15 | Coyote vs. Acme | Comedy, Adventure, Family | 173.5 | 2026-08-20 | `/vhv7lBWYM0DUuNU2a0V7Rhq21dD.jpg` |
| 16 | Batman: Knightfall Part 1: Knightfall | Animation, Action, Adventure, Science Fiction, Crime, Drama | 164.1 | 2026-06-23 | `/360qdtu2hLnqMu8SVHMywn420w1.jpg` |
| 17 | Ghost in the Cell | Horror, Comedy, Thriller | 149.0 | 2026-04-16 | `/zxcMdx0w5Zmg8yZuuiS7CJ8vOea.jpg` |
| 18 | Above & Below | Action, Thriller, Horror, Crime | 143.6 | 2026-07-29 | `/7bOuu1SRALGwsG2fLCTvRkCmQBj.jpg` |
| 19 | Backrooms | Horror, Mystery, Science Fiction | 132.2 | 2026-05-27 | `/rhGx6E3qRNMgj3i5su2oukNHwIQ.jpg` |
| 20 | The Dog Stars | Science Fiction, Adventure, Thriller | 120.5 | 2026-08-26 | `/5O616X9vmRzQdB68PHzBewPittd.jpg` |

### Layout stress cases in this set

- **Longest title** — "Grand Theft Auto VI: An Extended Look" (37 characters). Must not truncate to meaninglessness in a list row.
- **Most genres** — "Batman: Knightfall Part 1: Knightfall" carries 6: Animation, Action, Adventure, Science Fiction, Crime, Drama. Decide whether a row shows all of them, wraps, or truncates.
- **Fewest genres** — "Mutiny" carries 2. The genre slot must not look broken when nearly empty.
- **Popularity range across these 20** — 1028 down to 120. If popularity is ever shown as a number, it needs a format that survives four digits.
- **Single-genre movies** — none in the top 20, but 7 in the full 100, so the row must look right with exactly one chip:
  - "Evil Dead Burn" — Horror
  - "I Want Your Sex" — Comedy
  - "Fall 2: Deadpoint" — Thriller
  - "Scary Movie" — Comedy
  - "Digger" — Comedy
  - "Untold Raygun: Breaking Badly" — Documentary
  - "Hokum" — Horror
- **Worst case in the full 100** — "MOBILE SUIT GUNDAM HATHAWAY The Sorcery of Nymph Circe" (54 characters), which is not in the top 20 but will appear on scroll.

---

## 3. Movie detail — scenario coverage

Eight real records chosen to span the range from complete to nearly empty. Design must hold for all eight.

### 3.1 Complete record — every field populated

**Project Hail Mary** — id `687163`

The easy case. Use it to establish the layout, then break it with the seven below.

| Field | Value |
|---|---|
| Tagline | Believe in the Hail Mary. |
| Genres | Science Fiction, Adventure |
| Status | Released |
| Release date | 2026-03-15 |
| Runtime | 2h 37m |
| Vote average | 8.646 |
| Vote count | 7472 |
| Budget | $200,000,000 |
| Revenue | $684,234,722 |
| IMDB id | tt12042730 |
| `poster_path` | `/yihdXomYb5kTeSivtFndMy5iDmf.jpg` |
| `backdrop_path` | `/8Tfys3mDZVp4tNoH2ktm06a0Tau.jpg` |

**Overview** (366 chars)

> Science teacher Ryland Grace wakes up on a spaceship light years from home with no recollection of who he is or how he got there. As his memory returns, he begins to uncover his mission: solve the riddle of the mysterious substance causing the sun to die out. He must call on his scientific knowledge and unorthodox ideas to save everything on Earth from extinction.

### 3.2 Largest numbers

**Spider-Man: Brand New Day** — id `969681`

Biggest revenue in the sample. Currency formatting must not overflow or wrap awkwardly.

| Field | Value |
|---|---|
| Tagline | A brand new day starts now. |
| Genres | Science Fiction, Action, Adventure |
| Status | Released |
| Release date | 2026-07-29 |
| Runtime | 2h 25m |
| Vote average | 7.875 |
| Vote count | 2330 |
| Budget | $225,000,000 |
| Revenue | $2,332,821,029 |
| IMDB id | tt22084616 |
| `poster_path` | `/bjiS5ipwxb9JFy3XRRN4OAilSeX.jpg` |
| `backdrop_path` | `/7iwUUcKURMT7aKfCwMy6YnGtchD.jpg` |

**Overview** (370 chars)

> Fighting crime full-time as Spider-Man in a world that doesn't remember him—and the pressure of seeing his old friends move on without him—sparks a change in Peter Parker he may not have the power to control. But that transformation might also be the only thing that can stop a shocking new threat to the city and those he loves - a powerful villain no one can even see.

### 3.3 Longest text

**MOBILE SUIT GUNDAM HATHAWAY The Sorcery of Nymph Circe** — id `910850`

Longest title and longest overview in the sample. Title wrapping and overview length are both stressed here.

| Field | Value |
|---|---|
| Tagline | **empty** |
| Genres | Animation, Science Fiction, War, Drama |
| Status | Released |
| Release date | 2026-01-30 |
| Runtime | 1h 55m |
| Vote average | 6.8 |
| Vote count | 16 |
| Budget | —  ← **not disclosed** |
| Revenue | $18,420,625 |
| IMDB id | tt32915652 |
| `poster_path` | `/6YkVpc9Dn1L2FHNlLOybpSKeGc7.jpg` |
| `backdrop_path` | `/4ZTD67RhUOcI8WVP1Kmd1LFFc7d.jpg` |

**Overview** (605 chars)

> Haunted by past trauma, Hathaway Noa is drawn to a mysterious girl named Gigi Andalucia, whose strange powers stir memories within him. While swayed by her cryptic words, he continues preparing for Mafty's mission -- the attack on the Adelaide Conference. Meanwhile, Kenneth Sleg of the Earth Federation Forces prepares a defense operation for the Adelaide Conference and a plan to eliminate Mafty and is approached by Handley Yoxon of the Criminal Police Organization with a secret proposal. As Hathaway and Kenneth pursue their respective goals, Gigi also sets off for Hong Kong to fulfill her own role.

### 3.4 Maximum genres

**Batman: Knightfall Part 1: Knightfall** — id `1560520`

Six genres. The chip row must wrap or scroll without pushing content off screen.

| Field | Value |
|---|---|
| Tagline | **empty** |
| Genres | Animation, Action, Adventure, Science Fiction, Crime, Drama |
| Status | Released |
| Release date | 2026-06-23 |
| Runtime | 1h 19m |
| Vote average | 9.101 |
| Vote count | 153 |
| Budget | —  ← **not disclosed** |
| Revenue | —  ← **not disclosed** |
| IMDB id | tt32333324 |
| `poster_path` | `/360qdtu2hLnqMu8SVHMywn420w1.jpg` |
| `backdrop_path` | `/caBIySpwuFi2i7ynvHIlnxJLOdN.jpg` |

**Overview** (267 chars)

> Arkham Asylum has been destroyed, and all its inmates have been unleashed upon Gotham City. As Batman races to round up some of his greatest enemies, he is pushed to his physical and mental limits and into a final confrontation with a new threat: the man called Bane!

### 3.5 Post Production — unreleased, no financials

**Avengers: Doomsday** — id `1003596`

Released films dominate, but unreleased ones trend hard. No revenue yet.

| Field | Value |
|---|---|
| Tagline | **empty** |
| Genres | Science Fiction, Action, Adventure |
| Status | Post Production |
| Release date | 2026-12-16 |
| Runtime | 2h 45m |
| Vote average | 0.0  ← **no votes** |
| Vote count | 0 |
| Budget | —  ← **not disclosed** |
| Revenue | —  ← **not disclosed** |
| IMDB id | tt21357150 |
| `poster_path` | `/jzPwsojjFStf5lR5Nm07w2hH56G.jpg` |
| `backdrop_path` | `/s4v0UX1anfXm0UvloLsTTJ4v222.jpg` |

**Overview** (154 chars)

> Beloved heroes from three distinct universes are set on a deadly collision course and face an existential threat unlike anything they've ever encountered.

### 3.6 In Production

**Street Fighter** — id `1153576`

Further from release. More fields empty.

| Field | Value |
|---|---|
| Tagline | Ready. Set. Fight. |
| Genres | Action, Fantasy |
| Status | In Production |
| Release date | 2026-10-14 |
| Runtime | 2h 5m |
| Vote average | 0.0  ← **no votes** |
| Vote count | 0 |
| Budget | —  ← **not disclosed** |
| Revenue | —  ← **not disclosed** |
| IMDB id | tt27419420 |
| `poster_path` | `/wOnoOeDcsLSo72ON1UC6c1VHKk1.jpg` |
| `backdrop_path` | `/m45xBFqUT4EYpb8eIbamPGK3qea.jpg` |

**Overview** (372 chars)

> In 1993, estranged Street Fighters Ryu and Ken Masters are thrown back into combat when the mysterious Chun-Li recruits them for the next World Warrior Tournament: a brutal clash of fists, fate, and fury. But behind this battle royale lies a deadly conspiracy that forces them to face off against each other and the demons of their past. And if they don't, it's GAME OVER!

### 3.7 Planned — earliest stage

**Onslaught** — id `1284046`

The least data TMDB holds for a film that is still trending.

| Field | Value |
|---|---|
| Tagline | They unleashed their deadliest weapon on us. |
| Genres | Action, Horror, Thriller |
| Status | Planned |
| Release date | 2026-09-02 |
| Runtime | 1h 32m |
| Vote average | 0.0  ← **no votes** |
| Vote count | 0 |
| Budget | —  ← **not disclosed** |
| Revenue | —  ← **not disclosed** |
| IMDB id | tt32268691 |
| `poster_path` | `/cOGtvhc6Ij9KvzM6jZsfQyg0B0O.jpg` |
| `backdrop_path` | `/pmPXXniQlb4EdYY0gVZO90rf54F.jpg` |

**Overview** (156 chars)

> When a rogue squad of genetically engineered super soldiers break loose in the desert, a badass Army sniper must unleash hell to protect her young daughter.

### 3.8 Worst case — the missing-data fixture

**The Mongoose** — id `1294189`

Named in the brief as the reference fixture. If the design holds here, it holds everywhere.

| Field | Value |
|---|---|
| Tagline | **empty** |
| Genres | Action, Thriller |
| Status | In Production |
| Release date | 2026-10-30 |
| Runtime | 1h 39m |
| Vote average | 0.0  ← **no votes** |
| Vote count | 0 |
| Budget | —  ← **not disclosed** |
| Revenue | —  ← **not disclosed** |
| IMDB id | tt13611778 |
| `poster_path` | `/eSS5mvSG84UUuvtbHel5Yu3Wik4.jpg` |
| `backdrop_path` | `/iRIhPqqoUHiFBxn8oYf3gCQnaKk.jpg` |

**Overview** (243 chars)

> A falsely accused war hero with nothing to lose leads police on an epic televised cross-country car chase, helped by members of his former Special Forces Army battalion and closely monitored by a fascinated public rooting for his safe getaway.

---

## 4. Genres — all 19

Fetched from `/genre/movie/list`. The list payload gives ids only; these are the names.

| id | Name | Movies in the live top 100 |
|---|---|---|
| 53 | Thriller | 33 |
| 28 | Action | 30 |
| 35 | Comedy | 30 |
| 12 | Adventure | 23 |
| 878 | Science Fiction | 21 |
| 18 | Drama | 20 |
| 27 | Horror | 19 |
| 80 | Crime | 17 |
| 14 | Fantasy | 14 |
| 16 | Animation | 12 |
| 10751 | Family | 9 |
| 10749 | Romance | 9 |
| 9648 | Mystery | 8 |
| 36 | History | 4 |
| 10752 | War | 4 |
| 99 | Documentary | 1 |
| 10402 | Music | 1 |
| 37 | Western | 1 |
| 10770 | TV Movie | **0** |

**18 of 19 genres appear** in a live top 100; only TV Movie was absent. Movies carry between 1 and 6 genres, median 3 — which is why multi-select genre filtering uses OR rather than AND.

The filter sheet must handle both ends: a genre matching 34 movies and a genre matching 1, plus a genre matching 0 which still appears in the list.

---

## 5. Status values

Four values appeared across the live top 100. TMDB also defines `Rumored` and `Canceled`, which did not occur here but are valid.

| Status | Count in sample | Typically missing |
|---|---|---|
| Released | 83 | nothing — most fields populated |
| Post Production | 6 | revenue, votes, sometimes runtime |
| In Production | 2 | revenue, budget, votes, tagline |
| Planned | 1 | nearly everything except title, overview, genres, images |

**9 of 92 movies were unreleased** — 11% of the sample. Those are exactly the records with no rating and no financials. Status is the signal that explains the empty fields, so it should not be buried.

---

## 6. Missing data — measured

Across all 92 distinct movies in the live top 100.

| Field | Empty, null or zero | Share |
|---|---|---|
| Budget | 41 of 92 | **45%** |
| Revenue | 38 of 92 | **41%** |
| Tagline | 18 of 92 | **20%** |
| Homepage | 16 of 92 | **17%** |
| Vote count and average | 11 of 92 | **12%** |
| Runtime | 5 of 92 | **5%** |
| Budget *and* revenue together | 35 of 92 | **38%** |

Never empty in the sample: title, overview, poster, backdrop, genres, status, release date, imdb_id. Treat all of them as nullable anyway — the API does not guarantee them.

**Formatting rules these numbers force** (from the brief, Section 5.2):

- Budget or revenue of 0 reads **"Not disclosed"**, never `$0`.
- A `vote_count` of 0 omits the **entire** rating block — no stars, no `0.0`.
- An empty tagline omits the slot entirely; it never renders as blank space.
- Runtime renders as `1h 47m`, never `107`.
- A missing `imdb_id` removes the IMDB row rather than disabling it.

**One state has no real example here.** Every one of the 92 movies had an `imdb_id`, so no sample shows the detail screen without an IMDB row. It still has to be designed — the field is nullable, and the rule is to remove the row entirely rather than disable or grey it. Take *The Mongoose* above and delete its IMDB row to see the shape.
