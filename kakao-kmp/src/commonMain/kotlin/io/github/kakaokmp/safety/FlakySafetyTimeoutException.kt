package io.github.kakaokmp.safety

class FlakySafetyTimeoutException(
    val timeoutMs: Long,
    cause: Throwable,
) : AssertionError(
    "Flaky safety timeout after ${timeoutMs}ms. Last error: ${cause.message}",
    cause,
)
