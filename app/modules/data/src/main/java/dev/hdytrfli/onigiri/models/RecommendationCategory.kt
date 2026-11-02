package dev.hdytrfli.onigiri.models

import java.util.UUID
import kotlinx.serialization.Serializable

/** The server's recommendation categories, mapped away from the SDK enum for the UI. */
@Serializable
enum class RecommendationKind {
    BecauseYouWatched,
    BecauseYouLike,
    DirectedBy,
    Starring,
    Unknown,
}

/**
 * One suggestion row from `GET /Movies/Recommendations`. [baselineName] is the movie, director or
 * actor the row is about; [categoryId] can repeat, so rows are keyed by position as well.
 */
@Serializable
data class RecommendationCategory(
    val kind: RecommendationKind,
    val baselineName: String?,
    @Serializable(with = UuidSerializer::class) val categoryId: UUID?,
    val items: List<FindroidItem>,
)
