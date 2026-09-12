package com.shayan.amro.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Where one query resumes.
 *
 * It is in the database rather than in the mediator that reads it, because a mediator dies with
 * the process and an append after a restore would otherwise refetch from the head of the list.
 *
 * @property queryKey the narrowing and ordering this row belongs to.
 * @property nextCursor what the source needs to serve the next movies, or null once it has served
 * everything it will.
 */
@Entity(tableName = "query_keys")
internal data class QueryKeyEntity(
    @PrimaryKey val queryKey: String,
    val nextCursor: String?,
)
