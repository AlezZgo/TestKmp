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
import org.example.project.test.step
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

        step("[1] Нажать \"Создать карточку\"") {
            onNodeWithTag(UiTags.HomeCreateCardButton).performClick()
        }
        step("[2] Ввести имя и фамилию") {
            onNodeWithTag(UiTags.CreateFirstNameField).performTextInput("TestName")
            onNodeWithTag(UiTags.CreateLastNameField).performTextInput("TestSurname")
        }
        step("[3] Нажать \"Создать\"") {
            onNodeWithTag(UiTags.CreateSubmitButton).performClick()
        }
        step("[4] Проверить, что карточка появилась на главном") {
            onNodeWithText("TestName TestSurname").assertIsDisplayed()
        }
    }
}

