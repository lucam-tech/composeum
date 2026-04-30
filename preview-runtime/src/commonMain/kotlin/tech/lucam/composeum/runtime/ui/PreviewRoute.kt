package tech.lucam.composeum.runtime.ui

import tech.lucam.composeum.runtime.AccessibilityPreviewState
import tech.lucam.composeum.runtime.store.ThemeOverride
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Navigation route definitions for the preview browser. */
sealed class PreviewRoute(val route: String) {
    /** Root screen showing the group tree. */
    data object GroupList : PreviewRoute("group_list?${STATE_ARG}={${STATE_ARG}}") {
        fun routeFor(state: ShareablePreviewState? = null): String =
            buildRoute("group_list", state)
    }

    /** Grid screen listing previews that belong to one resolved group key. */
    data class PreviewList(val groupKey: String) :
        PreviewRoute("preview_list/{${ARG}}?${STATE_ARG}={${STATE_ARG}}") {
        companion object {
            /** Navigation argument name used to pass the selected group key. */
            const val ARG = "groupKey"

            /** Builds a concrete navigation route for [groupKey]. */
            fun routeFor(groupKey: String, state: ShareablePreviewState? = null): String =
                buildRoute("preview_list/${groupKey.encodePathSegment()}", state)
        }
    }

    /** Detail screen for one preview family, addressed by [familyKey]. */
    data class PreviewDetail(val familyKey: String) :
        PreviewRoute("preview_detail/{${ARG}}?${STATE_ARG}={${STATE_ARG}}") {
        companion object {
            /** Navigation argument name used to pass the selected preview family key. */
            const val ARG = "familyKey"

            /** Builds a concrete navigation route for [familyKey]. */
            fun routeFor(familyKey: String, state: ShareablePreviewState? = null): String =
                buildRoute("preview_detail/${familyKey.encodePathSegment()}", state)
        }
    }

    companion object {
        const val STATE_ARG = "state"
    }
}

/**
 * Shareable browser snapshot embedded in preview routes.
 *
 * Custom param values that cannot be serialized into the supported scalar set are omitted.
 */
@Serializable
data class ShareablePreviewState(
    val selectedEntryKey: String? = null,
    val paramState: Map<String, String> = emptyMap(),
    val themeId: String? = null,
    val themeOverride: ThemeOverride? = null,
    val fontScale: Float? = null,
    val uiScale: Float? = null,
    val locale: String? = null,
    val accessibilityState: AccessibilityPreviewState = AccessibilityPreviewState(),
)

private fun buildRoute(basePath: String, state: ShareablePreviewState?): String =
    state?.encodedOrNull()?.let { encoded -> "$basePath?${PreviewRoute.STATE_ARG}=$encoded" } ?: basePath

@OptIn(ExperimentalEncodingApi::class)
fun ShareablePreviewState.encodedOrNull(): String? = runCatching {
    Base64.UrlSafe.encode(Json.encodeToString(this).encodeToByteArray())
}.getOrNull()

@OptIn(ExperimentalEncodingApi::class)
fun decodeShareablePreviewState(encoded: String?): ShareablePreviewState? {
    if (encoded.isNullOrBlank()) return null
    return runCatching {
        Json.decodeFromString<ShareablePreviewState>(
            Base64.UrlSafe.decode(encoded).decodeToString(),
        )
    }.getOrNull()
}

/**
 * Percent-encodes characters that would break URL path segment matching in Compose Navigation.
 * Navigation's NavType.StringType automatically decodes the value when reading from arguments,
 * so callers receive the original unencoded string.
 */
private fun String.encodePathSegment(): String = replace("%", "%25")
    .replace("/", "%2F")
    .replace(" ", "%20")
    .replace("?", "%3F")
    .replace("#", "%23")
