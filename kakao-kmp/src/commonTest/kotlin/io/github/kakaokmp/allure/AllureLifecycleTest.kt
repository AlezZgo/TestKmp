package io.github.kakaokmp.allure

import io.github.kakaokmp.allure.model.AllureStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class AllureLifecycleTest {

    @Test
    fun collectsLabels() {
        val lifecycle = AllureLifecycle()
        lifecycle.startTest("myTest")
        lifecycle.addLabel("AS_ID", "TC-123")
        lifecycle.addLabel("feature", "Cards")
        lifecycle.finishTest(AllureStatus.PASSED)

        val result = lifecycle.buildResult()
        assertEquals("myTest", result.name)
        assertEquals(AllureStatus.PASSED, result.status)
        assertEquals(2, result.labels.size)
        assertEquals("TC-123", result.labels.first { it.name == "AS_ID" }.value)
    }

    @Test
    fun collectsSteps() {
        val lifecycle = AllureLifecycle()
        lifecycle.startTest("myTest")
        lifecycle.startStep("Step 1")
        lifecycle.stopStep(AllureStatus.PASSED)
        lifecycle.startStep("Step 2")
        lifecycle.stopStep(AllureStatus.FAILED)
        lifecycle.finishTest(AllureStatus.FAILED)

        val result = lifecycle.buildResult()
        assertEquals(2, result.steps.size)
        assertEquals("Step 1", result.steps[0].name)
        assertEquals(AllureStatus.PASSED, result.steps[0].status)
        assertEquals("Step 2", result.steps[1].name)
        assertEquals(AllureStatus.FAILED, result.steps[1].status)
    }

    @Test
    fun collectsNestedSteps() {
        val lifecycle = AllureLifecycle()
        lifecycle.startTest("myTest")
        lifecycle.startStep("Outer step")
        lifecycle.startStep("Inner step")
        lifecycle.stopStep(AllureStatus.PASSED)
        lifecycle.stopStep(AllureStatus.PASSED)
        lifecycle.finishTest(AllureStatus.PASSED)

        val result = lifecycle.buildResult()
        assertEquals(1, result.steps.size)
        assertEquals("Outer step", result.steps[0].name)
        assertEquals(1, result.steps[0].steps.size)
        assertEquals("Inner step", result.steps[0].steps[0].name)
        assertEquals(AllureStatus.PASSED, result.steps[0].steps[0].status)
    }

    @Test
    fun recordsTimestamps() {
        val lifecycle = AllureLifecycle()
        lifecycle.startTest("myTest")
        lifecycle.finishTest(AllureStatus.PASSED)

        val result = lifecycle.buildResult()
        assert(result.start > 0)
        assert(result.stop >= result.start)
    }
}
