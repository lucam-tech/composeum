package tech.lucam.composeum.runtime.util

/** Emits a debug log message. No-ops in release builds on Android (controlled by BuildConfig). */
internal expect fun logDebug(tag: String, message: String)
