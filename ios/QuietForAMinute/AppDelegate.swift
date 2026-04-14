import UIKit
import UserNotifications

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    /// Injected after the app state objects are created in QuietForAMinuteApp.
    weak var muteTimerService: MuteTimerService?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self

        // Install the hidden MPVolumeView used to set system volume
        if let window = application.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first?.windows.first {
            VolumeController.shared.install(in: window)
        }

        return true
    }

    // MARK: - UNUserNotificationCenterDelegate

    /// Called when a notification action is tapped (e.g. "Start Timer" on a scheduled mute).
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo  = response.notification.request.content.userInfo
        let duration  = userInfo[NotificationScheduler.durationKey] as? Int ?? 0
        let restore   = muteTimerService?.preferences?.restoreVolume ?? 0.5

        switch response.actionIdentifier {
        case NotificationScheduler.startActionID:
            // User tapped "Start Timer" — mute immediately
            Task { @MainActor [weak self] in
                self?.muteTimerService?.start(durationMinutes: duration, restoreVolume: restore)
                VolumeController.shared.setVolume(0)
            }

        case NotificationScheduler.skipActionID, UNNotificationDefaultActionIdentifier:
            // "Skip" or notification body tap — just open the app
            break

        default:
            break
        }

        completionHandler()
    }

    /// Allow notifications to be shown while app is in the foreground.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }
}
