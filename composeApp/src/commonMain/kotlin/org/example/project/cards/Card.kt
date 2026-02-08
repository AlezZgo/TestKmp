package org.example.project.cards

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val id: Long,
    val firstName: String,
    val lastName: String,
)

