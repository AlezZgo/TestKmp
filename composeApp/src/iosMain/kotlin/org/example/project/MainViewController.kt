package org.example.project

import platform.UIKit.UIViewController

// Backward-compatible entrypoint (old Swift template used it).
// The real app uses IosRootHolder + rootViewController(root).
fun MainViewController(): UIViewController =
    rootViewController(root = IosRootHolder().root)