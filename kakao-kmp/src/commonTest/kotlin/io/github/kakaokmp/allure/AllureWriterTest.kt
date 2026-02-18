package io.github.kakaokmp.allure

import io.github.kakaokmp.allure.model.*
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AllureWriterTest {

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun writesResultAsJsonAndReadsBack() {
        val testUuid = Uuid.random().toString()
        val result = AllureTestResult(
            uuid = testUuid,
            name = "sampleTest",
            fullName = "sampleTest",
            status = AllureStatus.PASSED,
            start = 1000L,
            stop = 2000L,
            labels = listOf(AllureLabel("feature", "Test")),
            steps = listOf(
                AllureStepResult(
                    name = "Step 1",
                    status = AllureStatus.PASSED,
                    start = 1000L,
                    stop = 1500L,
                )
            ),
        )

        val tempDir = "build/test-allure-results"
        val originalOutputDir = AllureWriter.outputDir
        try {
            AllureWriter.outputDir = tempDir
            AllureWriter.write(result)

            val filePath = Path(tempDir, "$testUuid-result.json")
            val content = SystemFileSystem.source(filePath).buffered().readString()
            val json = Json { ignoreUnknownKeys = true }
            val deserialized = json.decodeFromString<AllureTestResult>(content)

            assertEquals("sampleTest", deserialized.name)
            assertEquals(AllureStatus.PASSED, deserialized.status)
            assertEquals(1000L, deserialized.start)
            assertEquals(2000L, deserialized.stop)
            assertEquals(1, deserialized.labels.size)
            assertEquals("Test", deserialized.labels[0].value)
            assertEquals(1, deserialized.steps.size)
            assertEquals("Step 1", deserialized.steps[0].name)
        } finally {
            AllureWriter.outputDir = originalOutputDir
        }
    }
}
