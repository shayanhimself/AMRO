package com.shayan.amro.core.database.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * One movie's place in the list one query produced.
 *
 * The provider's ordering cannot be recomputed here: its collation is its own, its popularity is
 * unpublished, and its date ordering omits rows this device holds. So the order is stored as data
 * and every read sorts on it.
 *
 * A movie holds one place in a query, which the unique index is what makes true. A provider that
 * re-ranks between two requests serves a movie on both sides of the boundary between them, and a
 * list that held it twice would repeat a row.
 *
 * There is no foreign key onto `movies`. One writer owns both tables and writes them in one
 * transaction, so the join is what a read does rather than what a constraint enforces.
 *
 * @property queryKey the narrowing and ordering this row belongs to.
 * @property position where the provider put the movie in that list, counted from zero.
 * @property movieId the movie at that position.
 */
@Entity(
    tableName = "query_results",
    primaryKeys = ["queryKey", "position"],
    indices = [Index(value = ["queryKey", "movieId"], unique = true)],
)
internal data class QueryResultEntity(
    val queryKey: String,
    val position: Int,
    val movieId: String,
)
