# Design System

The app's Material 3 design system: theme, design tokens, and a reusable Compose component catalog. Every feature module renders through it; it depends on nothing else in the project.

## Module

`:core:ui`, package `com.shayan.amro.core.ui`. Android library, pure UI.

## Token layer

Standard tokens flow through `MaterialTheme` (`ColorScheme`, `Typography`, `Shapes`). Tokens M3 has no slot for (semantic/extended colors, motion, named component shapes) live in `:core:ui`, split by whether their value depends on runtime state.

A token set earns a CompositionLocal only if its value depends on the active color scheme or a user/system preference. `ExtendedColors` is the only one: the `AmroTheme` composable installs it, and it is read through the `AmroExtendedTheme.colors` accessor. Everything else is constant and read directly: the `Spacing`, `Elevation`, `ComponentShapes` and `Motion` sets, with no theme lookup, and usable outside composition (draw scopes, previews, test fixtures).

### Color

Color sits in two tiers: `ColorPrimitives`, the palette holding every literal value, named once by hue + tone (`Orange40`, `Navy06`), and two **semantic `ColorScheme`s** (dark default + light) whose roles each reference a primitive. Components read only roles. Both schemes fully specify every role: the light scope darkens hues for legibility on light surfaces rather than reusing the dark tones. Semantic colors M3 has no slot for are an extended set, read via `AmroExtendedTheme.colors`.

### Typography

Full 15-role M3 scale on **Roboto**, each role carrying its own weight. Weight is part of the role, never chosen at a call site.

The M3 Expressive **emphasized** variants are not adopted. Material 3 defines one per role, but the Compose accessors are internal, so no call site can read `MaterialTheme.typography.bodyLargeEmphasized`. Emphasis steps up a role instead (`bodyLarge` → `titleMedium`). Revisit when the API is public.

### Shape

Corner radii follow the primitive→semantic tiering:

- **`RadiusPrimitives`**: the raw radius ramp, a numeric scale where `radiusN` is `4N` dp. **Public and call-site-facing**, which is where the tiering diverges from color: a feature builds its own `RoundedCornerShape` from the matching primitive for any radius no M3 slot or `ComponentShapes` role already names, exactly as it reads the `Spacing` scale directly.
- **`AmroM3Shapes`**: the M3 `Shapes` ramp, built from those primitives and installed into `MaterialTheme(shapes = …)`. It pins the five public M3 slots to our radii, so every built-in M3 component that reads its default corner from the theme draws our values, not the library's.
- **`ComponentShapes`**: named per-component roles (`input`, `chip`, `card`, `dialog`), read directly at call sites, each **resolving to a `RadiusPrimitives` value**. It exists to reach radii the public M3 `Shapes` cannot name: `button` is a pill and has no M3 slot at all. Pinning the rest here is what stops a component drifting to whatever corner the built-in M3 component would otherwise pick.

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

**Icons are glyphs in a font, not drawables.** An icon is text: `DsIcon` draws a ligature name through `material_symbols_rounded.ttf` font, and the font substitutes the matching symbol. So `"arrow_back"` rendered in that family is the back arrow, and the whole icon set is one file with no per-icon asset, no vector XML, and nothing to re-export when a screen wants a symbol the app has not used before. Same applies to Emojis.

**Adding an icon:** Find the symbol in the [Material Symbols catalog](https://fonts.google.com/icons) with the Rounded style selected, take its ligature name, and add it as a constant to `Glyphs` in `:core:ui`.

## Component catalog

All components are stateless and presentational, taking state in via parameters and sending events out via lambdas, so feature modules own state and these stay screenshot-testable. Three groups:

- **core**: `DsButton`, `DsIconButton`, `DsIcon`, `DsCard`, `DsBadge`. `DsButton` carries a `loading` state distinct from disabled: full colour and label kept, trailing slot becomes a spinner, click swallowed.
- **forms**: `DsTextField`, `DsSwitch`, `DsChip`.
- **feedback**: `DsDialog`, `DsSnackbar`.

**Strings:** No component holds a user-visible literal. Text a user reads or TalkBack speaks resolves from `:core:ui`'s `strings.xml`, and the module declares only strings that are generic by nature: a loading state description, a retry label, a dismiss description, the wordmark. Per-screen copy is a component parameter: the feature owns the words and resolves them from its own resources at the call site, which is what keeps `:core:ui` free of product vocabulary. Material Symbols ligature names are glyph identifiers rather than text, so they stay in code, but only in `Glyphs`, never at a call site: a mistyped ligature renders nothing and no compiler catches it.

**What goes here:** A component earns a place in `:core:ui` by having more than one consumer.

**Naming:** Catalog components carry a `Ds` prefix (`DsButton`, `DsIcon`, `DsCard`, …). Feature modules legitimately use both the catalog and `androidx.compose.material3` directly (the latter for M3 components the catalog does not wrap) so an un-prefixed `Button` would leave a reader unsure whether it is ours or M3's. The prefix makes provenance obvious at every call site and lets both coexist in one file with no import aliasing. Inside `:core:ui`, a wrapper still aliases the M3 original it delegates to (e.g. `import androidx.compose.material3.Button as M3Button`).

## Screenshot testing

Compose Preview Screenshot Testing (`com.android.compose.screenshot` plugin, `@PreviewTest` + `@Preview`), wired into the version catalog and `:core:ui`. One preview function per gallery carries `@ThemePreviews`, which covers every component in **both** dark and light from a single declaration. Roborazzi is the documented fallback if the preview-channel tool blocks.

`:core:testing` carries what every module's screenshot tests share: the `FormFactorPreviews`, `FontScalePreviews` and `ThemePreviews` multi-preview annotations.

The states a component is shown in live once, as `internal` composables in `:core:ui`'s `designsystem/preview` package. A component's colocated `@Preview` and the screenshot golden that covers it render the same fixture, so neither can drift from the other or miss a state the other added. A feature module follows the same rule for a screen, with the fixture in `src/main/` beside the screen it feeds.
