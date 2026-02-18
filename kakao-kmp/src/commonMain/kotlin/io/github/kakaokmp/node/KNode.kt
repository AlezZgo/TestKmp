package io.github.kakaokmp.node

import androidx.compose.ui.test.*
import io.github.kakaokmp.safety.flakySafely

class KNode(
    private val composeUiTest: ComposeUiTest,
    private val matcher: SemanticsMatcher,
) {
    private fun interaction(): SemanticsNodeInteraction =
        composeUiTest.onNode(matcher)

    // --- Actions ---

    fun click() = composeUiTest.flakySafely {
        interaction().performClick()
    }

    fun typeText(text: String) = composeUiTest.flakySafely {
        interaction().performTextInput(text)
    }

    fun clearText() = composeUiTest.flakySafely {
        interaction().performTextClearance()
    }

    fun scrollTo() = composeUiTest.flakySafely {
        interaction().performScrollTo()
    }

    // --- Assertions ---

    fun assertIsDisplayed() = composeUiTest.flakySafely {
        interaction().assertIsDisplayed()
    }

    fun assertIsNotDisplayed() = composeUiTest.flakySafely {
        interaction().assertIsNotDisplayed()
    }

    fun assertTextEquals(vararg expected: String, includeEditableText: Boolean = true) =
        composeUiTest.flakySafely {
            interaction().assertTextEquals(*expected, includeEditableText = includeEditableText)
        }

    fun assertTextContains(value: String, substring: Boolean = true, ignoreCase: Boolean = false) =
        composeUiTest.flakySafely {
            interaction().assertTextContains(value, substring = substring, ignoreCase = ignoreCase)
        }

    fun assertIsEnabled() = composeUiTest.flakySafely {
        interaction().assertIsEnabled()
    }

    fun assertIsNotEnabled() = composeUiTest.flakySafely {
        interaction().assertIsNotEnabled()
    }

    fun assertExists() = composeUiTest.flakySafely {
        interaction().assertExists()
    }

    fun assertDoesNotExist() = composeUiTest.flakySafely {
        interaction().assertDoesNotExist()
    }
}
