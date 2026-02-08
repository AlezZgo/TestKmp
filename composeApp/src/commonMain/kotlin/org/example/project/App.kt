package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import org.example.project.root.RootComponent
import org.example.project.ui.RootContent

@Composable
fun App(
    root: RootComponent,
    modifier: Modifier = Modifier,
) {
    MaterialTheme {
        RootContent(
            component = root,
            modifier = modifier
                .fillMaxSize()
                .safeContentPadding(),
        )
    }
}