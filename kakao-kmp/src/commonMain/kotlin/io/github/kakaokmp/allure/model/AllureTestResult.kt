package io.github.kakaokmp.allure.model

import kotlinx.serialization.Serializable

@Serializable
data class AllureTestResult(
    val uuid: String,
    val name: String,
    val fullName: String = name,
    val status: AllureStatus,
    val start: Long,
    val stop: Long,
    val labels: List<AllureLabel> = emptyList(),
    val steps: List<AllureStepResult> = emptyList(),
    val statusDetails: AllureStatusDetails? = null,
)
