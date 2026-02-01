package org.example.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource

import testkmp.composeapp.generated.resources.Res
import testkmp.composeapp.generated.resources.ic_android
import testkmp.composeapp.generated.resources.ic_apple

@Composable
@Preview
fun App() {
    MaterialTheme {
        var screen by remember { mutableStateOf<Screen>(Screen.Home) }

        when (screen) {
            Screen.Home -> HomeScreen(
                onOpenPlatformIcon = { screen = Screen.PlatformIcon },
            )

            Screen.PlatformIcon -> PlatformIconScreen(
                onBack = { screen = Screen.Home },
            )
        }
    }
}

private sealed interface Screen {
    data object Home : Screen
    data object PlatformIcon : Screen
}

@Composable
private fun HomeScreen(
    onOpenPlatformIcon: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Home")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenPlatformIcon) {
            Text("Open platform screen")
        }
    }
}

@Composable
private fun PlatformIconScreen(
    onBack: () -> Unit,
) {
    val platformKind = remember { currentPlatformKind() }
    val icon = when (platformKind) {
        PlatformKind.Android -> Res.drawable.ic_android
        PlatformKind.Ios -> Res.drawable.ic_apple
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Platform: $platformKind")
        Spacer(Modifier.height(24.dp))
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(140.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}