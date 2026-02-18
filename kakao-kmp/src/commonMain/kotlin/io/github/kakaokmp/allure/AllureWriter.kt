package io.github.kakaokmp.allure

import io.github.kakaokmp.allure.model.AllureTestResult
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AllureWriter {
    var outputDir: String = "allure-results"

    private val json = Json { prettyPrint = true }

    fun write(result: AllureTestResult) {
        val dir = Path(outputDir)
        SystemFileSystem.createDirectories(dir)
        val file = Path(dir, "${result.uuid}-result.json")
        SystemFileSystem.sink(file).buffered().use { sink ->
            sink.writeString(json.encodeToString(result))
        }
    }
}
