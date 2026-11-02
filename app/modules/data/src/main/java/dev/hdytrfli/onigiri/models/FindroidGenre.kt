package dev.hdytrfli.onigiri.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class FindroidGenre(
    @Serializable(with = UuidSerializer::class) val id: UUID,
    val name: String,
)
