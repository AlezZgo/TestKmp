package io.github.kakaokmp.safety

import androidx.compose.ui.test.ComposeUiTest
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

// Retry loop without ComposeUiTest dependency — suitable for plain unit tests
inline fun <T> flakySafelyPlain(
    timeoutMs: Long = KakaoKmpConfig.defaultTimeoutMs,
    allowedExceptions: Set<KClass<out Throwable>> = KakaoKmpConfig.allowedExceptions,
    block: () -> T,
): T {
    require(timeoutMs > 0) { "timeoutMs must be positive, was $timeoutMs" }

    val mark = TimeSource.Monotonic.markNow()
    var lastError: Throwable? = null
    while (mark.elapsedNow() < timeoutMs.milliseconds) {
        try {
            return block()
        } catch (e: Throwable) {
            if (allowedExceptions.none { it.isInstance(e) }) throw e
            lastError = e
        }
    }
    throw FlakySafetyTimeoutException(timeoutMs, lastError!!)
}

// Retry loop with ComposeUiTest.waitForIdle() between retries
inline fun <T> ComposeUiTest.flakySafely(
    timeoutMs: Long = KakaoKmpConfig.defaultTimeoutMs,
    allowedExceptions: Set<KClass<out Throwable>> = KakaoKmpConfig.allowedExceptions,
    block: () -> T,
): T {
    require(timeoutMs > 0) { "timeoutMs must be positive, was $timeoutMs" }

    val mark = TimeSource.Monotonic.markNow()
    var lastError: Throwable? = null
    while (mark.elapsedNow() < timeoutMs.milliseconds) {
        try {
            return block()
        } catch (e: Throwable) {
            if (allowedExceptions.none { it.isInstance(e) }) throw e
            lastError = e
            waitForIdle()
        }
    }
    throw FlakySafetyTimeoutException(timeoutMs, lastError!!)
}
