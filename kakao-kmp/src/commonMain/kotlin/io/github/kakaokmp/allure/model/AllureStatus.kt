package io.github.kakaokmp.allure.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AllureStatus {
    @SerialName("passed") PASSED,
    @SerialName("failed") FAILED,
    @SerialName("broken") BROKEN,
    @SerialName("skipped") SKIPPED,
}
