# AMRO: Movie Discovery MVP for Android
Native Android app over the TMDB API.

Product: `docs/superpowers/specs/000-product-brief.md`.

## Git

**Never commit or push.** The user reviews all changes and commits manually. This overrides any skill/workflow step that says to commit (e.g. superpowers). Leave changes in the working tree and report what's ready.

## Tooling

We use **`android` CLI** for project creation, SDK/emulator management, running and inspecting the app and doc lookup. Prefer it over ad-hoc gradle/adb. It's the default for API/version/migration questions too. Treat it as fresher than model training.

Run the test suite with **`./scripts/unittest.sh`**.

## Code style

- **`const val` names use `SCREAMING_SNAKE_CASE`**. Non-const `val`s follow normal `camelCase`.
- **No magic numbers.** A literal whose value carries meaning gets a named `const val`, so the name explains it instead of a comment. Exempt: `0` and `1`, counts and sizes a test asserts on (`assertEquals(2, items.size)`), and numbers that are already the domain's own vocabulary (an HTTP status, a `@Preview` height). A dimension a composable lays out with (`36.dp`, a weight, a fraction) is exempt too, as long as it appears once in the file: it is read where it is applied, and a name states no more than the property already does. The same dimension written twice in a file gets a name, so the two cannot drift apart.
- **No trailing (end-of-line) comments.** Put the comment on its own line *above* the code it describes. The one exception is where a language forces inline syntax.
- **A comment says what the code does, or why it does it that way.** Not what it does not do, and not an answer to a question the code never raises.
- **KDoc where it earns its place.** When a function or class (or any of its arguments) isn't self-explanatory from its name and signature, add a KDoc block: one line on what it does, plus `@param`/`@return` for the non-obvious parts. **Interfaces and contracts always get KDoc**.
- **KDoc opens with what the thing is or does.** One sentence, before any reasoning. A block that starts on the logic inside the body leaves the reader to infer the purpose from an explanation of it. Why it works that way comes after that sentence, or moves into the body above the branch it explains.
- **A comment sits with what it explains.** A class KDoc says what the class is, and stops there. Reasoning about one property, function, or branch goes above that property, function, or branch, never accumulated into the block on top of the class. Constructor `@param` lines are the exception, since the language gives them nowhere else to live.
- **Never name a document in code.** Comments and KDoc say what the code does, in its own terms. A spec number (`004 writes…`), a plan task, or a design frame id (`2e`, `3k`) is document-side: it goes stale, and means nothing to a reader who does not have that file open. Name the thing that behaves the way you are describing (`the repository writes…`) instead.
- **Test function names use backtick spaced form** (`` fun `does the thing`() ``). Device tests in
  `app/src/androidTest` are the exception and use `camelCase`: a space in a method name needs DEX
  040, which the app's `minSdk` of 26 is below, so D8 refuses to build the test APK.
- **Tests never spell out a string the code owns.** Copy the app ships is read from its resource through the module's test helper, and an identifier the domain owns.
- **A fixture value gets a `private const val` when a name earns it.** Two cases earn one: a value the test asserts on, so the assertion and the fixture cannot drift apart, and a value whose meaning the literal does not carry. A value used once inside a fixture builder, read only as the shape of a record, stays inline: a wall of constants naming what the field already names is noise, and it puts the fixture on a different screen from the test that reads it.
- **Public composables ship colocated previews.** Every `public @Composable` gets at least one plain `@Preview` in its own file, one per distinct visual state, each wrapped in `AmroTheme`.

## Writing: specs, docs, comments

- Don't repeat yourself.
- No history / run events. State what the system *does*, not what happened once or what was decided when.
- Don't over-explain. Say it once, short and plain.
- One example only where the rule is genuinely ambiguous without it. Usually zero.
- Never use em dashes. Rewrite the sentence, or use a comma, colon, parentheses, or full stop.
- Never mention the superpowers docs in prose. Refer to the content by what it says, not by its path under `docs/superpowers/`.
