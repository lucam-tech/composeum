package tech.lucam.composeum.runtime.util

/**
 * Attempts to navigate the host IDE to [sourceFile] at [sourceLine] via the JetBrains
 * built-in HTTP server (port 63342).
 *
 * On Android emulators 10.0.2.2 routes to the host machine, so this works when Android
 * Studio is running on the same machine. Returns false on physical devices and wasmJs.
 */
internal expect suspend fun tryOpenInIde(sourceFile: String, sourceLine: Int): Boolean
