package dev.jdtech.jellyfin.models

import java.util.UUID

/** The server's recommendation categories, mapped away from the SDK enum for the UI. */
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
data class RecommendationCategory(
    val kind: RecommendationKind,
    val baselineName: String?,
    val categoryId: UUID?,
    val items: List<FindroidItem>,
)
