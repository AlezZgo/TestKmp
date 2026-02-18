package io.github.kakaokmp.dsl

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import io.github.kakaokmp.allure.AllureLifecycle
import io.github.kakaokmp.allure.AllureWriter
import io.github.kakaokmp.allure.model.AllureStatus

@OptIn(ExperimentalTestApi::class)
fun kakaoTest(
    testName: String? = null,
    block: TestScope.() -> Unit,
) = runComposeUiTest {
    val lifecycle = AllureLifecycle()
    val scope = TestScope(this, lifecycle)
    val name = testName ?: inferTestName()

    lifecycle.startTest(name)
    try {
        scope.block()
        lifecycle.finishTest(AllureStatus.PASSED)
    } catch (e: Throwable) {
        lifecycle.finishTest(AllureStatus.FAILED, e)
        throw e
    } finally {
        AllureWriter.write(lifecycle.buildResult())
    }
}

internal fun inferTestName(): String {
    val trace = Throwable().stackTraceToString()
    val testLine = trace.lineSequence()
        .firstOrNull { line ->
            line.contains("Test.") && !line.contains("kakaoTest") && !line.contains("inferTestName")
        }
    return testLine
        ?.trim()
        ?.substringAfter("at ")
        ?.substringBefore("(")
        ?: "UnknownTest"
}
