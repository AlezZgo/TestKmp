package io.github.kakaokmp.allure.model

import kotlinx.serialization.Serializable

@Serializable
data class AllureStatusDetails(
    val message: String? = null,
    val trace: String? = null,
)
