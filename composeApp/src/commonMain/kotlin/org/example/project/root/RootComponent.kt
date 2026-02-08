package org.example.project.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import org.example.project.createcard.CreateCardComponent
import org.example.project.home.HomeComponent

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed class Child {
        data class Home(val component: HomeComponent) : Child()
        data class CreateCard(val component: CreateCardComponent) : Child()
    }
}

