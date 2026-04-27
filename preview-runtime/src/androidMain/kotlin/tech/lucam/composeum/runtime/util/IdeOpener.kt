package tech.lucam.composeum.runtime.util

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

// IntelliJ-based IDEs expose a REST API on their built-in server (default port 63342).
// On Android emulators 10.0.2.2 is the host machine's loopback address.
private const val IDE_HOST = "10.0.2.2"
private const val IDE_PORT = 63342

internal actual suspend fun tryOpenInIde(sourceFile: String, sourceLine: Int): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val apiUrl = Uri.Builder()
                .scheme("http")
                .encodedAuthority("$IDE_HOST:$IDE_PORT")
                .path("/api/file")
                .appendQueryParameter("file", sourceFile)
                .appendQueryParameter("line", sourceLine.toString())
                .build()
                .toString()
            val connection = URL(apiUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            connection.requestMethod = "GET"
            val ok = connection.responseCode in 200..299
            connection.disconnect()
            ok
        } catch (_: Exception) {
            false
        }
    }
