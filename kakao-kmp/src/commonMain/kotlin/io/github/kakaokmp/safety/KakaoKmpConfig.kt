package io.github.kakaokmp.safety

import kotlin.reflect.KClass

object KakaoKmpConfig {
    var defaultTimeoutMs: Long = 5_000L
    var defaultIntervalMs: Long = 500L
    var allowedExceptions: Set<KClass<out Throwable>> = setOf(
        AssertionError::class,
        IllegalStateException::class,
    )
}
