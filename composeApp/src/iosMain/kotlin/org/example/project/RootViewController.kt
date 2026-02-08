package org.example.project

import androidx.compose.ui.window.ComposeUIViewController
import org.example.project.root.RootComponent
import platform.UIKit.UIViewController

fun rootViewController(root: RootComponent): UIViewController =
    ComposeUIViewController {
        App(root = root)
    }

