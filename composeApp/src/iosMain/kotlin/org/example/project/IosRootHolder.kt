package org.example.project

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import org.example.project.root.DefaultRootComponent
import org.example.project.root.RootComponent

class IosRootHolder {
    private val lifecycle: LifecycleRegistry = LifecycleRegistry()

    val root: RootComponent = DefaultRootComponent(
        componentContext = DefaultComponentContext(lifecycle = lifecycle),
    )

    init {
        lifecycle.create()
    }

    fun onActive() {
        lifecycle.resume()
    }

    fun onInactive() {
        lifecycle.pause()
    }

    fun onBackground() {
        lifecycle.stop()
    }

    fun onDestroy() {
        lifecycle.destroy()
    }
}

