import Foundation
import Combine
import AVFoundation

@MainActor
final class SettingsViewModel: ObservableObject {

    // Mirrors PreferencesStore but adds UI-specific state (e.g. alert presentation).
    @Published var showChimePicker = false
    @Published var chimePickerIsMute = true   // true = mute chime, false = restore chime

    // Permission states — refreshed on each appear
    @Published var notificationsGranted = false

    func refreshPermissions() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        notificationsGranted = settings.authorizationStatus == .authorized
    }

    func requestNotifications() async {
        _ = await NotificationScheduler.shared.requestAuthorization()
        await refreshPermissions()
    }
}
