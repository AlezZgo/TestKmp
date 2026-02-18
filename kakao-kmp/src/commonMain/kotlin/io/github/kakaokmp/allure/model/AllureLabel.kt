package io.github.kakaokmp.allure.model

import kotlinx.serialization.Serializable

@Serializable
data class AllureLabel(val name: String, val value: String)
