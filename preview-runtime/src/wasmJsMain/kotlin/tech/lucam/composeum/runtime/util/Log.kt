package tech.lucam.composeum.runtime.util

@JsFun("(tag, message) => console.log('[' + tag + '] ' + message)")
private external fun jsConsoleLog(tag: String, message: String)

internal actual fun logDebug(tag: String, message: String) {
    jsConsoleLog(tag, message)
}
