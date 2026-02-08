import SwiftUI

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate: AppDelegate
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView(root: appDelegate.rootHolder.root)
                .onChange(of: scenePhase) { newPhase in
                    switch newPhase {
                    case .active:
                        appDelegate.rootHolder.onActive()
                    case .inactive:
                        appDelegate.rootHolder.onInactive()
                    case .background:
                        appDelegate.rootHolder.onBackground()
                    @unknown default:
                        break
                    }
                }
        }
    }
}