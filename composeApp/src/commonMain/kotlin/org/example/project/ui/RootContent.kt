package org.example.project.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import org.example.project.root.RootComponent

@Composable
fun RootContent(
    component: RootComponent,
    modifier: Modifier = Modifier,
) {
    Children(
        stack = component.stack,
        modifier = modifier,
    ) {
        when (val child = it.instance) {
            is RootComponent.Child.Home -> HomeContent(child.component)
            is RootComponent.Child.CreateCard -> CreateCardContent(child.component)
        }
    }
}

