package dev.hdytrfli.onigiri.models

import dev.hdytrfli.onigiri.repository.JellyfinRepository
import java.util.UUID
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemDto

@Serializable
data class FindroidPerson(
    @Serializable(with = UuidSerializer::class) val id: UUID,
    val name: String,
    val overview: String,
    val images: FindroidImages,
)

fun BaseItemDto.toFindroidPerson(repository: JellyfinRepository): FindroidPerson {
    return FindroidPerson(
        id = id,
        name = name.orEmpty(),
        overview = overview.orEmpty(),
        images = toFindroidImages(repository),
    )
}
