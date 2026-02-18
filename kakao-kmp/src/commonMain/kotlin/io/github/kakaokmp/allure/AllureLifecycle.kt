package io.github.kakaokmp.allure

import io.github.kakaokmp.allure.model.*
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AllureLifecycle {
    private var testName: String = ""
    private var testStart: Long = 0L
    private var testStop: Long = 0L
    private var testStatus: AllureStatus = AllureStatus.PASSED
    private var testStatusDetails: AllureStatusDetails? = null
    private val labels = mutableListOf<AllureLabel>()
    private val steps = mutableListOf<AllureStepResult>()

    private data class StepEntry(
        val name: String,
        val start: Long,
        val children: MutableList<AllureStepResult> = mutableListOf(),
    )

    private val stepStack = ArrayDeque<StepEntry>()

    fun startTest(name: String) {
        testName = name
        testStart = now()
    }

    fun addLabel(name: String, value: String) {
        labels += AllureLabel(name, value)
    }

    fun startStep(name: String) {
        stepStack.addLast(StepEntry(name = name, start = now()))
    }

    fun stopStep(status: AllureStatus, error: Throwable? = null) {
        val entry = stepStack.removeLast()
        val result = AllureStepResult(
            name = entry.name,
            status = status,
            start = entry.start,
            stop = now(),
            steps = entry.children.toList(),
        )
        val parent = stepStack.lastOrNull()
        if (parent != null) {
            parent.children += result
        } else {
            steps += result
        }
    }

    fun finishTest(status: AllureStatus, error: Throwable? = null) {
        testStatus = status
        testStop = now()
        if (error != null) {
            testStatusDetails = AllureStatusDetails(
                message = error.message,
                trace = error.stackTraceToString(),
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun buildResult(): AllureTestResult = AllureTestResult(
        uuid = Uuid.random().toString(),
        name = testName,
        fullName = testName,
        status = testStatus,
        start = testStart,
        stop = testStop,
        labels = labels.toList(),
        steps = steps.toList(),
        statusDetails = testStatusDetails,
    )

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
