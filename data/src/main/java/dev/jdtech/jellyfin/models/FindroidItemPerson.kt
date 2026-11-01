package dev.jdtech.jellyfin.models

import android.net.Uri
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.BaseItemPerson
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.PersonKind

@Serializable
data class FindroidItemPersonImage(
    @Serializable(with = UriSerializer::class) val uri: Uri?,
    val blurHash: String?,
)

fun BaseItemPerson.toFindroidImage(repository: JellyfinRepository): FindroidItemPersonImage {
    val baseUrl = Uri.parse(repository.getBaseUrl())
    val builder =
        baseUrl
            .buildUpon()
            .appendEncodedPath("items/$id/Images/${ImageType.PRIMARY}")
            .appendQueryParameter("maxWidth", "400")
    // Some item payloads (notably books/authors) omit the image tag even though the
    // person has a picture. The image endpoint still serves it without the tag.
    primaryImageTag?.let { builder.appendQueryParameter("tag", it) }
    return FindroidItemPersonImage(
        uri = builder.build(),
        blurHash = imageBlurHashes?.get(ImageType.PRIMARY)?.get(primaryImageTag),
    )
}

@Serializable
data class FindroidItemPerson(
    @Serializable(with = UuidSerializer::class) val id: UUID,
    val name: String,
    val type: PersonKind,
    val role: String,
    val image: FindroidItemPersonImage,
)

fun BaseItemPerson.toFindroidPerson(repository: JellyfinRepository): FindroidItemPerson {
    return FindroidItemPerson(
        id = id,
        name = name.orEmpty(),
        type = type,
        role = role.orEmpty(),
        image = toFindroidImage(repository),
    )
}
