package org.example.project

enum class PlatformKind {
    Android,
    Ios,
}

expect fun currentPlatformKind(): PlatformKind

