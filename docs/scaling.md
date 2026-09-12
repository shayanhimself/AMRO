# Scaling

What the MVP deliberately does not do, and what it costs when the time comes.

## The list reaches past ten thousand movies

The list is paged, and both the narrowing and the ordering are the provider's: a selection reaches
the remote source, which asks `/discover/movie` for that genre union in that order. The database
holds one list of positions per selection and the screen reads a window of it, so nothing is ordered
in memory and a chip tap re-queries a table rather than re-sorting a list.

What still bounds it is the provider. TMDB refuses page 501, so ten thousand movies is the deepest
any one selection reaches, and `total_pages` counts a catalogue the list cannot walk to. A reader
who reaches the end of a selection has reached the end of what TMDB will serve for it, not the end
of the catalogue.

Going past that means narrowing further rather than paging further: a year, a rating floor or a
language turns one unreachable selection into several reachable ones. Each is another facet on
`MovieSelection`, another segment in the query key, and another parameter in the discover mapper.
Nothing else moves, because the key is built from the selection alone and the database branches on
neither.

## A source that cannot filter and sort

A source backing this screen has to narrow by genre and order by every key in both directions. Four
of the twelve movie APIs surveyed can. One that cannot is still useful for movie details, and it
reaches the app as a second binding behind `MovieRemoteDataSource` rather than as a fallback inside
the list: two filter implementations behind one list would leave no way to tell which produced what
is on screen.

## Offline outside a selection already run

The only thing this design serves with no network is a selection the device has already run. A new
narrowing with no connection is the full-screen error, even while the database holds ten thousand
movies under another key. Filtering those in Kotlin would be a second filter implementation ordered
by a comparator the provider does not use, so the list would differ from what the same selection
shows online. Mitigating it means prefetching selections nobody has asked for, which is a storage
budget the MVP does not have.
