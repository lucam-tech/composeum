package tech.lucam.composeum.runtime.util

import android.util.Log

internal actual fun logDebug(tag: String, message: String) {
    Log.d(tag, message)
}
