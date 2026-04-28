package tech.lucam.composeum.runtime.ui

/** Navigation route definitions for the preview browser. */
sealed class PreviewRoute(val route: String) {
    /** Root screen showing the group tree. */
    data object GroupList : PreviewRoute("group_list")

    /** Grid screen listing previews that belong to one resolved group key. */
    data class PreviewList(val groupKey: String) :
        PreviewRoute("preview_list/{${PreviewList.ARG}}") {
        companion object {
            /** Navigation argument name used to pass the selected group key. */
            const val ARG = "groupKey"

            /** Builds a concrete navigation route for [groupKey]. */
            fun routeFor(groupKey: String): String = "preview_list/${groupKey.encodePathSegment()}"
        }
    }

    /** Detail screen for one preview entry, addressed by [entryKey]. */
    data class PreviewDetail(val entryKey: String) :
        PreviewRoute("preview_detail/{${PreviewDetail.ARG}}") {
        companion object {
            /** Navigation argument name used to pass the selected preview key. */
            const val ARG = "entryKey"

            /** Builds a concrete navigation route for [entryKey]. */
            fun routeFor(entryKey: String): String = "preview_detail/${entryKey.encodePathSegment()}"
        }
    }
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
