package org.example.project

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.resume
import io.github.kakaokmp.dsl.kakaoTest
import org.example.project.root.DefaultRootComponent
import org.example.project.screens.CreateCardScreen
import org.example.project.screens.HomeScreen
import kotlin.test.Test

class CardCreationKakaoTest {

    @Test
    fun createCardFlow_addsCardToHomeList() = kakaoTest(testName = "createCardFlow_addsCardToHomeList") {
        allureId("TC-001")
        feature("Cards")

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
            HomeScreen {
                createCardButton.click()
            }
        }
        step("[2] Ввести имя и фамилию") {
            CreateCardScreen {
                firstNameField.typeText("TestName")
                lastNameField.typeText("TestSurname")
            }
        }
        step("[3] Нажать \"Создать\"") {
            CreateCardScreen {
                submitButton.click()
            }
        }
        step("[4] Проверить, что карточка появилась на главном") {
            HomeScreen {
                cardWithText("TestName TestSurname").assertIsDisplayed()
            }
        }
    }
}
