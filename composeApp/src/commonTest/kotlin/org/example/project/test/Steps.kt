package org.example.project.test

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog

@OptIn(ExperimentalTestApi::class)
inline fun <T> ComposeUiTest.step(
    title: String,
    block: ComposeUiTest.() -> T,
): T {
    println("STEP ▶ $title")
    return try {
        block()
    } catch (t: Throwable) {
        println("STEP ✗ $title")
        println("ERROR: ${t::class.simpleName}: ${t.message}")
        // Best-effort dump: helps to understand what's actually on screen.
        runCatching {
            onRoot(useUnmergedTree = true).printToLog(tag = "SemanticsTree")
        }
        throw t
    }
}

