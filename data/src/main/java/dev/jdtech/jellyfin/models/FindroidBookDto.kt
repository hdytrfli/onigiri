package dev.jdtech.jellyfin.models

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "books")
data class FindroidBookDto(
    @PrimaryKey val id: UUID,
    val serverId: String?,
    val name: String,
    val originalTitle: String?,
    val overview: String,
    val productionYear: Int?,
    val premiereDate: LocalDateTime?,
    val studios: String,
    val isAudiobook: Boolean,
)

fun FindroidBook.toFindroidBookDto(serverId: String? = null): FindroidBookDto =
    FindroidBookDto(
        id = id,
        serverId = serverId,
        name = name,
        originalTitle = originalTitle,
        overview = overview,
        productionYear = productionYear,
        premiereDate = premiereDate,
        studios = studios.joinToString("\u0001"),
        isAudiobook = isAudiobook,
    )
