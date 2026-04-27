package tech.lucam.composeum.sample

// ── Enum ─────────────────────────────────────────────────────────────────────

enum class ButtonVariant { Primary, Secondary, Destructive }

// ── Data class ───────────────────────────────────────────────────────────────

data class UserProfile(
    val name: String,
    val role: String,
    val isVerified: Boolean,
)

// ── Sealed interface ──────────────────────────────────────────────────────────

sealed interface ContentState {
    data object Idle : ContentState
    data object Loading : ContentState
    data class Success(val message: String) : ContentState
    data class Failure(val reason: String, val code: Int) : ContentState
}

// ── Custom type (simulates an external-library class KSP cannot introspect) ──

/**
 * Simulates a type from an external library — not a data class, enum, or sealed class,
 * so KSP falls through to the custom-type handler and relies on a registered
 * [tech.lucam.composeum.runtime.config.CustomParamField] for its param widget.
 */
class AlertSeverity private constructor(val level: Int, val label: String) {
    companion object {
        val LOW      = AlertSeverity(1, "Low")
        val MEDIUM   = AlertSeverity(2, "Medium")
        val HIGH     = AlertSeverity(3, "High")
        val CRITICAL = AlertSeverity(4, "Critical")
        val ALL      = listOf(LOW, MEDIUM, HIGH, CRITICAL)
    }
    override fun toString() = label
}
