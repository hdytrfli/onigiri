package dev.hdytrfli.onigiri.models

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = UiTextSerializer::class)
sealed class UiText {
    data class DynamicString(val value: String) : UiText()

    class StringResource(@param:StringRes val resId: Int, vararg val args: Any) : UiText()

    fun asString(resources: Resources): String {
        return when (this) {
            is DynamicString -> return value
            is StringResource -> resources.getString(resId, *args)
        }
    }

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId, *args)
        }
    }
}

/**
 * Persists [UiText] without resolving it, so cached section titles still follow the app language.
 * Numeric arguments are kept numeric because several strings use `%d`.
 */
object UiTextSerializer : KSerializer<UiText> {
    @Serializable
    private data class Argument(val intValue: Int? = null, val stringValue: String? = null)

    @Serializable
    private data class Surrogate(
        val kind: String,
        val value: String? = null,
        val resourceId: Int? = null,
        val arguments: List<Argument> = emptyList(),
    )

    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: UiText) {
        val surrogate =
            when (value) {
                is UiText.DynamicString -> Surrogate(kind = "string", value = value.value)
                is UiText.StringResource ->
                    Surrogate(
                        kind = "resource",
                        resourceId = value.resId,
                        arguments =
                            value.args.map { argument ->
                                when (argument) {
                                    is Int -> Argument(intValue = argument)
                                    else -> Argument(stringValue = argument.toString())
                                }
                            },
                    )
            }
        encoder.encodeSerializableValue(Surrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): UiText {
        val surrogate = decoder.decodeSerializableValue(Surrogate.serializer())
        return when (surrogate.kind) {
            "string" -> UiText.DynamicString(surrogate.value.orEmpty())
            "resource" -> {
                val args: Array<Any> =
                    surrogate.arguments
                        .map { argument ->
                            (argument.intValue ?: argument.stringValue.orEmpty()) as Any
                        }
                        .toTypedArray()
                UiText.StringResource(surrogate.resourceId ?: 0, *args)
            }
            else -> UiText.DynamicString(surrogate.value.orEmpty())
        }
    }
}
