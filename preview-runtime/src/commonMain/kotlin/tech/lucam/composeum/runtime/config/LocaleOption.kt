package tech.lucam.composeum.runtime.config

/**
 * A locale option shown in the settings sheet locale picker.
 *
 * @param tag         BCP 47 locale tag (e.g. "en", "de-AT"), or `"system"` to represent the
 *                    device default. This value is stored in [tech.lucam.composeum.runtime.store.RuntimeSettings.locale]
 *                    (with `"system"` serialized as `null`).
 * @param displayName Human-readable name shown in the picker (e.g. "English", "Deutsch").
 */
data class LocaleOption(
    val tag: String,
    val displayName: String,
)
