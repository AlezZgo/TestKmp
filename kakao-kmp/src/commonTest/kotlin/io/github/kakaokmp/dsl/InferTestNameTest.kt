package io.github.kakaokmp.dsl

import kotlin.test.Test
import kotlin.test.assertTrue

class InferTestNameTest {

    @Test
    fun returnsNonEmptyString() {
        val name = inferTestName()
        assertTrue(name.isNotEmpty(), "inferTestName() returned empty string")
    }

    @Test
    fun fallbackIsUnknownTest() {
        val name = inferTestName()
        assertTrue(
            name.contains("InferTestNameTest") || name == "UnknownTest",
            "Expected test class name or 'UnknownTest', got: $name"
        )
    }
}
