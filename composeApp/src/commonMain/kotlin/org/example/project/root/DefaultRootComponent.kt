package org.example.project.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import kotlinx.serialization.Serializable
import org.example.project.cards.Card
import org.example.project.createcard.CreateCardComponent
import org.example.project.home.HomeComponent

internal class DefaultRootComponent(
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    private var nextId: Long = 1L

    private val cardsValue = MutableValue(
        stateKeeper.consume(key = STATE_KEY, strategy = PersistedState.serializer())?.cards ?: emptyList(),
    )

    init {
        stateKeeper.register(key = STATE_KEY, strategy = PersistedState.serializer()) {
            PersistedState(cards = cardsValue.value)
        }
    }

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Home,
            handleBackButton = true,
            childFactory = ::child,
        )

    private fun child(
        config: Config,
        componentContext: ComponentContext,
    ): RootComponent.Child =
        when (config) {
            Config.Home -> RootComponent.Child.Home(
                component = DefaultHomeComponent(
                    componentContext = componentContext,
                    cards = cardsValue,
                    onCreate = { navigation.push(Config.CreateCard) },
                ),
            )

            Config.CreateCard -> RootComponent.Child.CreateCard(
                component = DefaultCreateCardComponent(
                    componentContext = componentContext,
                    onCreated = { firstName, lastName ->
                        cardsValue.update { it + Card(id = nextId++, firstName = firstName, lastName = lastName) }
                        navigation.pop()
                    },
                    onCancel = navigation::pop,
                ),
            )
        }

    @Serializable
    private sealed interface Config {
        @Serializable
        data object Home : Config

        @Serializable
        data object CreateCard : Config
    }

    @Serializable
    private data class PersistedState(
        val cards: List<Card> = emptyList(),
    )

    private companion object {
        private const val STATE_KEY: String = "RootPersistedState"
    }
}

private class DefaultHomeComponent(
    componentContext: ComponentContext,
    private val cards: Value<List<Card>>,
    private val onCreate: () -> Unit,
) : HomeComponent, ComponentContext by componentContext {
    override val cards: Value<List<Card>> = this.cards
    override fun onCreateCardClicked() = onCreate()
}

private class DefaultCreateCardComponent(
    componentContext: ComponentContext,
    private val onCreated: (firstName: String, lastName: String) -> Unit,
    private val onCancel: () -> Unit,
) : CreateCardComponent, ComponentContext by componentContext {

    private var state: CreateCardComponent.Model =
        stateKeeper.consume(key = STATE_KEY, strategy = CreateCardComponent.Model.serializer())
            ?: CreateCardComponent.Model()

    private val modelValue = MutableValue(state)
    override val model: Value<CreateCardComponent.Model> = modelValue

    init {
        stateKeeper.register(key = STATE_KEY, strategy = CreateCardComponent.Model.serializer()) { model.value }
    }

    override fun onFirstNameChanged(text: String) {
        update { it.copy(firstName = text) }
    }

    override fun onLastNameChanged(text: String) {
        update { it.copy(lastName = text) }
    }

    override fun onCreateClicked() {
        val current = model.value
        if (!current.isValid) return
        onCreated(current.firstName.trim(), current.lastName.trim())
    }

    override fun onBackClicked() = onCancel()

    private fun update(block: (CreateCardComponent.Model) -> CreateCardComponent.Model) {
        val next = block(model.value).withValidation()
        state = next
        modelValue.value = next
    }

    private fun CreateCardComponent.Model.withValidation(): CreateCardComponent.Model =
        copy(isValid = firstName.trim().isNotEmpty() && lastName.trim().isNotEmpty())

    private companion object {
        private const val STATE_KEY: String = "CreateCardState"
    }
}

