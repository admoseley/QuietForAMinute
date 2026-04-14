import SwiftUI
import UserNotifications

@main
struct QuietForAMinuteApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    @StateObject private var preferences       = PreferencesStore()
    @StateObject private var scheduleStore     = ScheduleStore()
    @StateObject private var muteTimerService  = MuteTimerService()
    @StateObject private var volumeMonitor     = VolumeMonitor()

    var body: some Scene {
        WindowGroup {
            MainTabView()
                .environmentObject(preferences)
                .environmentObject(scheduleStore)
                .environmentObject(muteTimerService)
                .environmentObject(volumeMonitor)
                .onAppear {
                    // Wire services together
                    muteTimerService.preferences   = preferences
                    muteTimerService.volumeMonitor = volumeMonitor
                    appDelegate.muteTimerService   = muteTimerService

                    // Begin volume monitoring
                    volumeMonitor.startMonitoring {
                        Task { @MainActor in
                            muteTimerService.onVolumeHitZero()
                        }
                    }

                    // Request notification permission on first launch
                    Task {
                        await NotificationScheduler.shared.requestAuthorization()
                    }
                }
                .preferredColorScheme(colorScheme(for: preferences.themeMode))
        }
    }

    private func colorScheme(for mode: PreferencesStore.ThemeMode) -> ColorScheme? {
        switch mode {
        case .light:  return .light
        case .dark:   return .dark
        case .system: return nil
        }
    }
}
