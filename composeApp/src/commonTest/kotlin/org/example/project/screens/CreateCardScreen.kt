package org.example.project.screens

import androidx.compose.ui.test.ComposeUiTest
import io.github.kakaokmp.screen.KmpScreen
import io.github.kakaokmp.screen.KmpScreenFactory
import io.github.kakaokmp.node.KNode
import org.example.project.ui.UiTags

class CreateCardScreen(test: ComposeUiTest) : KmpScreen<CreateCardScreen>(test) {
    val firstNameField = KNode { hasTestTag(UiTags.CreateFirstNameField) }
    val lastNameField = KNode { hasTestTag(UiTags.CreateLastNameField) }
    val submitButton = KNode { hasTestTag(UiTags.CreateSubmitButton) }
    val backButton = KNode { hasTestTag(UiTags.CreateBackButton) }

    companion object : KmpScreenFactory<CreateCardScreen>(::CreateCardScreen)
}
