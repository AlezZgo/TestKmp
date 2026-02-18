package io.github.kakaokmp.allure.model

import kotlinx.serialization.Serializable

@Serializable
data class AllureStepResult(
    val name: String,
    val status: AllureStatus,
    val start: Long,
    val stop: Long,
    val steps: List<AllureStepResult> = emptyList(),
)
