package io.github.kakaokmp.safety

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FlakySafetyTest {

    @Test
    fun succeedsOnFirstAttempt() {
        var callCount = 0
        val result = flakySafelyPlain(timeoutMs = 1000) {
            callCount++
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(1, callCount)
    }

    @Test
    fun retriesUntilSuccess() {
        var callCount = 0
        val result = flakySafelyPlain(timeoutMs = 5000) {
            callCount++
            if (callCount < 3) throw AssertionError("not yet")
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(3, callCount)
    }

    @Test
    fun throwsNonRetryableImmediately() {
        var callCount = 0
        assertFailsWith<IllegalArgumentException> {
            flakySafelyPlain(timeoutMs = 5000) {
                callCount++
                throw IllegalArgumentException("bad arg")
            }
        }
        assertEquals(1, callCount)
    }

    @Test
    fun throwsTimeoutWithLastError() {
        val ex = assertFailsWith<FlakySafetyTimeoutException> {
            flakySafelyPlain(timeoutMs = 100) {
                throw AssertionError("always fails")
            }
        }
        assertEquals(100, ex.timeoutMs)
        assertEquals("always fails", ex.cause?.message)
    }

    @Test
    fun rejectsNonPositiveTimeout() {
        assertFailsWith<IllegalArgumentException> {
            flakySafelyPlain(timeoutMs = 0) { "never" }
        }
        assertFailsWith<IllegalArgumentException> {
            flakySafelyPlain(timeoutMs = -1) { "never" }
        }
    }

    @Test
    fun retriesSubclassOfAllowedException() {
        open class ParentException : AssertionError("parent")
        class ChildException : ParentException()

        var callCount = 0
        val result = flakySafelyPlain(
            timeoutMs = 5000,
            allowedExceptions = setOf(ParentException::class),
        ) {
            callCount++
            if (callCount < 2) throw ChildException()
            "ok"
        }
        assertEquals("ok", result)
        assertEquals(2, callCount)
    }
}
