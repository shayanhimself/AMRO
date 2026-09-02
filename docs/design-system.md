# Design System

The app's Material 3 design system: theme, design tokens, and a reusable Compose component catalog. Every feature module renders through it; it depends on nothing else in the project.

## Module

`:core:ui`, package `com.shayan.amro.core.ui`. Android library, pure UI.

## Token layer

Standard tokens flow through `MaterialTheme` (`ColorScheme`, `Typography`, `Shapes`). Tokens M3 has no slot for (semantic/extended colors, motion, named component shapes) live in `:core:ui`, split by whether their value depends on runtime state.

A token set earns a CompositionLocal only if its value depends on the active color scheme or a user/system preference. `ExtendedColors` is the only one: the `AmroTheme` composable installs it, and it is read through the `AmroExtendedTheme.colors` accessor. Everything else is constant and read directly: the `Spacing`, `Elevation`, `ComponentShapes` and `Motion` sets, with no theme lookup, and usable outside composition (draw scopes, previews, test fixtures).

### Color

The source organizes color in two tiers, and the Android layer mirrors it: a **primitive palette** (every literal value, named once by hue + tone) and two **semantic `ColorScheme`s** (dark default + light) whose roles each reference a primitive. Components read only roles. Both schemes fully specify every role: the light scope darkens hues for legibility on light surfaces rather than reusing the dark tones. Semantic colors M3 has no slot for are an extended set, read via `AmroExtendedTheme.colors`.

### Typography

Full 15-role M3 scale on **Roboto**, with per-role weights from the source. Weight is part of the role, never chosen at a call site.

The M3 Expressive **emphasized** variants are not adopted. Material 3 defines one per role, but the Compose accessors are internal, so no call site can read `MaterialTheme.typography.bodyLargeEmphasized`. Emphasis steps up a role instead (`bodyLarge` → `titleMedium`), which is what the upstream design system provides. Revisit when the API is public.

### Shape

Corner radii follow the primitive→semantic tiering:

- **`RadiusPrimitives`**: the raw radius ramp, mirrored 1:1 from the upstream `--radius-*` tokens (numeric scale, `N = px/4`). **Public and call-site-facing**: this is where the tiering diverges from color: the mockups use the numeric radius scale directly on plain surfaces, exactly as they use the `Spacing` scale, so a feature builds its own `RoundedCornerShape` from the matching primitive for any radius no M3 slot or `ComponentShapes` role already names.
- **`AmroM3Shapes`**: the M3 `Shapes` ramp, built from those primitives and installed into `MaterialTheme(shapes = …)`. It pins the five public M3 slots to our radii, so every built-in M3 component that reads its default corner from the theme draws our values, not the library's.
- **`ComponentShapes`**: named per-component roles, read directly at call sites. Each maps to its upstream component-scoped token, and so **resolves to a `RadiusPrimitives` value**. Two reasons: it mirrors the upstream structure exactly (a component role points at a primitive, `--radius-card: var(--radius-3)`), and it can reach off-slot radii the public M3 `Shapes` cannot name. `button` is the pill `--radius-full`.

### Elevation & motion

Dark elevation is conveyed by tonal surface color; shadow (`Elevation`) is reserved for FAB, menus, dialogs, and transient notifications. Cards default to flat filled surfaces. The shadow ramp is identical in both schemes: depth in dark comes from the tonal `surfaceContainer*` roles, not from different shadow values.

Motion (`Motion`): a set of easings and durations. Press scales the target down and, on filled surfaces, shifts to the pressed color; hover applies a state layer. No infinite decorative loops. No glass/backdrop blur; `scrim` dims behind dialogs.

Honouring the system reduce-motion setting is out of scope. Compose does not apply the platform animator scale to these durations automatically, so it would be deliberate work; if it is ever specced, `Motion` becomes a CompositionLocal again so durations can be zeroed in one place rather than checked in every animated component.

## Theme entry

```
AmroTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)
```

Selects the dark or light `ColorScheme`, installs `MaterialTheme(colorScheme, typography, shapes)`, and provides the extended-token CompositionLocals. No `dynamicColor` parameter. Edge-to-edge is applied at the app level, not here.

## Icon system

`DsIcon` wraps **Material Symbols Rounded**, shipped as a **variable-font asset in `:core:ui`** (the source loads it from the Google Fonts CDN; the app bundles it in-APK so chrome renders offline). Glyphs are referenced by ligature name; variable axes `FILL` (rest → active), `wght`, `GRAD`, and optical size are settable per use. No PNG/SVG icon assets; emoji are never UI icons. The entire icon set is that one bundled font file, with no per-icon drawables; subsetting the font to only used glyphs is a deferred APK-size optimization.

Every ligature the app renders is a constant on `Glyphs` so no call site spells one out. Feature modules extend it as their screens need glyphs.

## Component catalog

All components are stateless and presentational, taking state in via parameters and sending events out via lambdas, so feature modules own state and these stay screenshot-testable. Grouped as in the source project:

- **core**: `DsButton`, `DsIconButton`, `DsIcon`, `DsCard`, `DsBadge`. `DsButton` carries a `loading` state distinct from disabled: full colour and label kept, trailing slot becomes a spinner, click swallowed.
- **forms**: `DsTextField`, `DsSwitch`, `DsChip`.
- **feedback**: `DsDialog`, `DsSnackbar`.

**Strings.** No component holds a user-visible literal. Text a user reads or TalkBack speaks resolves from `:core:ui`'s `strings.xml`, and the module declares only strings that are generic by nature: a loading state description, a retry label, a dismiss description, the wordmark. Per-screen copy is a component parameter: the feature owns the words and resolves them from its own resources at the call site, which is what keeps `:core:ui` free of product vocabulary. Material Symbols ligature names are glyph identifiers rather than text, so they stay in code, but only in `Glyphs`, never at a call site: a mistyped ligature renders nothing and no compiler catches it.

**What is deliberately not here.** A component earns a place in `:core:ui` by having more than one consumer *and* a shape that does not vary per screen. Two families fail that test:

- **Token-styled screen content**: a poster tile, a detail fact row. Single consumer and domain-shaped props, which `:core:ui` cannot see. Built in the feature module that needs them, never forced into `:core:ui`.
- **Empty states**: structurally different per screen, not merely different in copy: a list screen wants icon + line + optional CTA, a detail screen wants something else. A shared component would freeze a guessed layout before any screen exists to validate it. Features build their own; hoist only once two screens demonstrably share a structure.

The design system remains the source of truth for their *appearance* regardless: the token vocabulary is the appearance SSOT, not every composition built from it.

**Naming.** Catalog components carry a `Ds` prefix (`DsButton`, `DsIcon`, `DsCard`, …). Feature modules legitimately use both the catalog and `androidx.compose.material3` directly (the latter for M3 components the catalog does not wrap) so an un-prefixed `Button` would leave a reader unsure whether it is ours or M3's. The prefix makes provenance obvious at every call site and lets both coexist in one file with no import aliasing. Inside `:core:ui`, a wrapper still aliases the M3 original it delegates to (e.g. `import androidx.compose.material3.Button as M3Button`).

## Screenshot testing

Compose Preview Screenshot Testing (`com.android.compose.screenshot` plugin, `@PreviewTest` + `@Preview`), wired into the version catalog and `:core:ui`. One preview function per gallery carries `@ThemePreviews`, which covers every component in **both** dark and light from a single declaration. Roborazzi is the documented fallback if the preview-channel tool blocks.

`:core:testing` carries what every module's screenshot tests share: the `FormFactorPreviews`, `FontScalePreviews` and `ThemePreviews` multi-preview annotations.

The states a component is shown in live once, as `internal` composables in `:core:ui`'s `designsystem/preview` package. A component's colocated `@Preview` and the screenshot golden that covers it render the same fixture, so neither can drift from the other or miss a state the other added. A feature module follows the same rule for a screen, with the fixture in `src/main/` beside the screen it feeds.
