package org.example.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.resume
import org.example.project.root.DefaultRootComponent
import org.example.project.ui.UiTags
import kotlin.test.Test

class CardCreationUiTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun createCardFlow_addsCardToHomeList() = runComposeUiTest {
        val lifecycle = LifecycleRegistry().apply {
            create()
            resume()
        }
        val root = DefaultRootComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycle),
        )

        setContent {
            App(root = root)
        }

        onNodeWithTag(UiTags.HomeCreateCardButton).performClick()

        onNodeWithTag(UiTags.CreateFirstNameField).performTextInput("TestName")
        onNodeWithTag(UiTags.CreateLastNameField).performTextInput("TestSurname")
        onNodeWithTag(UiTags.CreateSubmitButton).performClick()

        onNodeWithText("TestName TestSurname").assertIsDisplayed()
    }
}

