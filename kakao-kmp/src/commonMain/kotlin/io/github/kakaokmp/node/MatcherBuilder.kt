package io.github.kakaokmp.node

import androidx.compose.ui.test.SemanticsMatcher

class MatcherBuilder {
    private val matchers = mutableListOf<SemanticsMatcher>()

    fun hasTestTag(tag: String) {
        matchers += androidx.compose.ui.test.hasTestTag(tag)
    }

    fun hasText(text: String, substring: Boolean = false, ignoreCase: Boolean = false) {
        matchers += androidx.compose.ui.test.hasText(text, substring = substring, ignoreCase = ignoreCase)
    }

    fun hasContentDescription(value: String, substring: Boolean = false, ignoreCase: Boolean = false) {
        matchers += androidx.compose.ui.test.hasContentDescription(value, substring = substring, ignoreCase = ignoreCase)
    }

    fun matcher(custom: SemanticsMatcher) {
        matchers += custom
    }

    internal fun build(): SemanticsMatcher {
        check(matchers.isNotEmpty()) { "MatcherBuilder requires at least one matcher" }
        return matchers.reduce { a, b -> a and b }
    }
}
