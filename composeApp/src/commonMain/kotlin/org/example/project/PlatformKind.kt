package org.example.project

enum class PlatformKind {
    Android,
    Ios,
    Jvm,
}

expect fun currentPlatformKind(): PlatformKind

