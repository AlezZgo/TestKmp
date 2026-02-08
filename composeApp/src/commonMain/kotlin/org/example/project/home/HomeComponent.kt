package org.example.project.home

import com.arkivanov.decompose.value.Value
import org.example.project.cards.Card

interface HomeComponent {
    val cards: Value<List<Card>>
    fun onCreateCardClicked()
}

