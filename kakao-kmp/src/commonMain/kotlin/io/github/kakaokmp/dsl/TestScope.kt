package io.github.kakaokmp.dsl

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import io.github.kakaokmp.allure.AllureLifecycle
import io.github.kakaokmp.allure.model.AllureStatus
import io.github.kakaokmp.screen.KmpScreen
import io.github.kakaokmp.screen.KmpScreenFactory

@OptIn(ExperimentalTestApi::class)
class TestScope(
    val composeUiTest: ComposeUiTest,
    @PublishedApi internal val allureLifecycle: AllureLifecycle = AllureLifecycle(),
) {

    fun setContent(composable: @Composable () -> Unit) {
        composeUiTest.setContent(composable)
    }

    operator fun <T : KmpScreen<T>> KmpScreenFactory<T>.invoke(block: T.() -> Unit) {
        create(composeUiTest).block()
    }

    fun allureId(id: String) = allureLifecycle.addLabel("AS_ID", id)
    fun feature(name: String) = allureLifecycle.addLabel("feature", name)
    fun epic(name: String) = allureLifecycle.addLabel("epic", name)
    fun story(name: String) = allureLifecycle.addLabel("story", name)

    inline fun <R> step(title: String, block: TestScope.() -> R): R {
        println("STEP ▶ $title")
        allureLifecycle.startStep(title)
        return try {
            val result = block()
            allureLifecycle.stopStep(AllureStatus.PASSED)
            result
        } catch (e: Throwable) {
            println("STEP ✗ $title")
            println("ERROR: ${e::class.simpleName}: ${e.message}")
            runCatching {
                composeUiTest.onRoot(useUnmergedTree = true).printToLog(tag = "SemanticsTree")
            }
            allureLifecycle.stopStep(AllureStatus.FAILED, e)
            throw e
        }
    }
}
