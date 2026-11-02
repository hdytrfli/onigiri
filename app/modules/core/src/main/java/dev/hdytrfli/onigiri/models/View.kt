package dev.hdytrfli.onigiri.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class View(
    @Serializable(with = UuidSerializer::class) val id: UUID,
    val name: String,
    val items: List<FindroidItem>,
    val type: CollectionType,
)
