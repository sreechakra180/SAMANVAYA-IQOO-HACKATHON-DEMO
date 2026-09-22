package com.sukshma.samanvaya.core

/**
 * NetworkGuard — ensures no accidental internet calls are made during core experience.
 *
 * The INTERNET permission is declared in the manifest ONLY to allow the local WebSocket
 * server that streams session data to the Office Kit laptop dashboard.
 *
 * All AI inference (VLM, STT, LLM, RAG) MUST be local.
 * Any attempt to call a remote AI API will be caught here in debug builds.
 *
 * Note: In a full production build this would use a network security config
 * to allowlist only 192.168.x.x / 10.x.x.x ranges. For the hackathon
 * prototype we track this with counters and surface them in diagnostics.
 */
object NetworkGuard {

    // Count of external (non-local) network calls attempted
    @Volatile private var externalCallCount = 0

    // Allowlist: only local IP ranges permitted
    private val allowedPrefixes = listOf(
        "192.168.", "10.", "172.16.", "172.17.", "172.18.", "172.19.",
        "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.",
        "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31.",
        "127.0.0.1", "localhost"
    )

    fun isLocalAddress(host: String): Boolean =
        allowedPrefixes.any { host.startsWith(it) }

    fun recordExternalAttempt(url: String) {
        externalCallCount++
        // In debug: throw to catch violations early
        if (BuildConfigCompat.isDebug) {
            error("[NetworkGuard] ⛔ External call attempted: $url — violates OFFLINE principle")
        }
    }

    fun getExternalCallCount(): Int = externalCallCount

    fun reset() { externalCallCount = 0 }
}

/**
 * Thin shim around BuildConfig to avoid import issues during scaffold phase.
 * Replaced by actual BuildConfig once the project compiles.
 */
object BuildConfigCompat {
    val isDebug: Boolean = true   // Set to BuildConfig.DEBUG after first compile
}
