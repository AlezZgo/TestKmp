package org.example.project.screens

import androidx.compose.ui.test.ComposeUiTest
import io.github.kakaokmp.screen.KmpScreen
import io.github.kakaokmp.screen.KmpScreenFactory
import io.github.kakaokmp.node.KNode
import org.example.project.ui.UiTags

class HomeScreen(test: ComposeUiTest) : KmpScreen<HomeScreen>(test) {
    val createCardButton = KNode { hasTestTag(UiTags.HomeCreateCardButton) }

    fun cardWithText(text: String) = KNode { hasText(text) }

    companion object : KmpScreenFactory<HomeScreen>(::HomeScreen)
}
