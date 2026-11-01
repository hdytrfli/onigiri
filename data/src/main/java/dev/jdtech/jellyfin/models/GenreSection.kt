package dev.jdtech.jellyfin.models

import java.util.UUID
import kotlinx.serialization.Serializable

/** One genre row on the Genres tab; also the persisted payload of that SWR query. */
@Serializable
data class GenreSection(
    @Serializable(with = UuidSerializer::class) val id: UUID,
    val name: String,
    val items: List<FindroidItem>,
)
