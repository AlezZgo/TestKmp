package org.example.project.createcard

import com.arkivanov.decompose.value.Value
import kotlinx.serialization.Serializable

interface CreateCardComponent {
    val model: Value<Model>

    fun onFirstNameChanged(text: String)
    fun onLastNameChanged(text: String)

    fun onCreateClicked()
    fun onBackClicked()

    @Serializable
    data class Model(
        val firstName: String = "",
        val lastName: String = "",
        val isValid: Boolean = false,
    )
}

