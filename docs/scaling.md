# Scaling

What the MVP deliberately does not do, and what it costs when the time comes.

## The trending list grows past a hundred

Today the set is exactly a hundred movies. The remote source fetches all of them in one call, the
local source reads the whole table as one `Flow`, and `:feature:trending` filters and sorts that list
in Kotlin (`MovieFiltering.kt`, `MovieSorting.kt`). At a hundred items that is
cheap, and it puts the ordering rules where a plain JVM test can assert them against a fixture.
SQL complication is not worth it for the MVP.

At thousands of items it becomes expensive, it's in memory and on every chip tap it's recalculated.

### Two changes:

### **Paginate, on both sides.**
The remote source gains a second argument, the page number, so a caller
asks for page N of `count` distinct movies. Remote source keeps abstracting how many api call it takes
or whether the api accepts page size argument at all. A repository says how many movies per page it
wants.

The local source reads a window rather than the whole table, so the screen holds the rows it renders
instead of every row it fetched.

The deduplication of the movies can be done either in the remote source or in
the local source.

### **Move filter and sort into SQL.**
A `WHERE` and an `ORDER BY` over the table, not a pass over a list
in Kotlin, because Kotlin can only order what it already holds and a paginated screen holds a page.
Two things move with it: genres need a real column or a cross-reference table to be queryable, and
the ordering rules become the database's.

Paging 3 is the tool to use. PagingData, `PagingSource`, `Pager`, and `RemoteMediator` are the
pieces, which will enter both data layer and UI layer.
