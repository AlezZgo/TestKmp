package io.github.kakaokmp.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith

class MatcherBuilderTest {

    @Test
    fun buildsSingleMatcher() {
        val matcher = MatcherBuilder().apply {
            hasTestTag("my_tag")
        }.build()
        assertNotNull(matcher)
    }

    @Test
    fun combinesMultipleMatchers() {
        val matcher = MatcherBuilder().apply {
            hasTestTag("my_tag")
            hasText("Hello")
        }.build()
        assertNotNull(matcher)
    }

    @Test
    fun failsOnEmptyBuilder() {
        assertFailsWith<IllegalStateException> {
            MatcherBuilder().build()
        }
    }
}
