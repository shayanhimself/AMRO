# Domain layer (optional)

Use cases live in `:core:data` when used at all, and are the only thing in it a ViewModel
may call besides a repository.

## Default: skip it

ViewModels call repositories directly. Add a use case only when one of these is true:

- The same business logic is needed by **two or more ViewModels**.
- A ViewModel's state production has grown genuinely complex and extracting a named
  operation makes it readable.

Never add pass-through use cases that just forward one repository call — that is ceremony,
not architecture. There is no blanket "UI must go through domain" rule in this codebase.

## Shape

```kotlin
class SortMoviesUseCase @Inject constructor() {
    operator fun invoke(movies: List<Movie>, key: SortKey, direction: SortDirection): List<Movie>
}
```

- Name: *VerbNoun*UseCase (`SortMoviesUseCase`, `FilterMoviesUseCase`).
- Single responsibility; callable via `operator fun invoke`.
- **Stateless.** No mutable fields, no lifecycle. A new instance per injection site is fine.
- Depends on repositories and other use cases; never on DAOs, a data source, or the UI.
- Main-safe. If it does CPU-heavy aggregation, `withContext(defaultDispatcher)` inside —
  but first ask whether that work belongs in the data layer.
- If Room can answer it with a relation query, write the query in the DAO instead of
  combining repositories in a use case.

## Testing

Plain JVM unit tests with fake repositories, or with a fixture where the use case is pure.
No Android, no mocks.
