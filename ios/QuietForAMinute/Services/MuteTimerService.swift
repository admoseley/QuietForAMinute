import Foundation
import Combine
import UIKit

/// Manages the active mute countdown.
/// Equivalent to Android's MuteTimerService foreground service.
///
/// On iOS there is no persistent foreground service; instead a
/// live countdown notification is posted and updated periodically.
@MainActor
final class MuteTimerService: ObservableObject {

    // MARK: - Published state

    @Published var isRunning = false
    @Published var remainingSeconds: Int = 0

    /// Triggers the mute-duration sheet in the root view.
    @Published var showMuteDurationSheet = false

    // MARK: - Dependencies (injected via environment)

    weak var preferences: PreferencesStore?
    weak var volumeMonitor: VolumeMonitor?

    // MARK: - Private

    private var timer: AnyCancellable?
    private var endDate: Date?
    private let notificationID = "com.admoseley.quietforaminute.timer"

    // MARK: - Volume-hit-zero callback (from VolumeMonitor)

    func onVolumeHitZero() {
        guard preferences?.overlayEnabled == true else { return }
        guard !showMuteDurationSheet else { return } // already showing
        if preferences?.chimeOnMute == true {
            ChimePlayer.shared.playChime(named: preferences?.muteChimeName)
        }
        showMuteDurationSheet = true
    }

    // MARK: - Start timer

    /// Called when user confirms a duration in MuteDurationSheet.
    func start(durationMinutes: Int, restoreVolume: Float) {
        guard durationMinutes > 0 else { return }

        stop() // cancel any existing timer

        remainingSeconds = durationMinutes * 60
        endDate = Date().addingTimeInterval(TimeInterval(remainingSeconds))
        isRunning = true

        scheduleTimerNotification(totalMinutes: durationMinutes)

        timer = Timer.publish(every: 1, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self else { return }
                Task { @MainActor in
                    self.tick(restoreVolume: restoreVolume)
                }
            }
    }

    // MARK: - Stop / cancel

    func stop() {
        timer?.cancel()
        timer = nil
        isRunning = false
        remainingSeconds = 0
        endDate = nil
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: [notificationID])
        UNUserNotificationCenter.current()
            .removeDeliveredNotifications(withIdentifiers: [notificationID])
    }

    // MARK: - Private helpers

    private func tick(restoreVolume: Float) {
        guard let end = endDate else { return }
        let remaining = Int(end.timeIntervalSinceNow.rounded(.up))
        if remaining <= 0 {
            finishTimer(restoreVolume: restoreVolume)
        } else {
            remainingSeconds = remaining
            updateTimerNotification(remainingSeconds: remaining)
        }
    }

    private func finishTimer(restoreVolume: Float) {
        stop()

        // Suppress VolumeMonitor so restoring volume doesn't re-open the sheet
        volumeMonitor?.suppressNextChange = true
        VolumeController.shared.setVolume(restoreVolume)

        if preferences?.chimeOnRestore == true {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { [weak self] in
                ChimePlayer.shared.playChime(named: self?.preferences?.restoreChimeName)
            }
        }

        postRestoreNotification()
    }

    // MARK: - Notifications

    private func scheduleTimerNotification(totalMinutes: Int) {
        removeTimerNotification()

        let content = UNMutableNotificationContent()
        content.title = "Phone muted"
        content.body  = "Restoring volume in \(formatDuration(totalMinutes * 60))"
        content.sound = nil

        // We update this notification manually via tick(); this initial post
        // fires immediately so the user sees it in the shade right away.
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let request = UNNotificationRequest(
            identifier: notificationID,
            content: content,
            trigger: trigger
        )
        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }

    private func updateTimerNotification(remainingSeconds: Int) {
        // Only update every 30 s to reduce churn
        guard remainingSeconds % 30 == 0 else { return }
        removeTimerNotification()

        let content = UNMutableNotificationContent()
        content.title = "Phone muted"
        content.body  = "Restoring volume in \(formatDuration(remainingSeconds))"
        content.sound = nil

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let request = UNNotificationRequest(
            identifier: notificationID,
            content: content,
            trigger: trigger
        )
        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }

    private func removeTimerNotification() {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: [notificationID])
        UNUserNotificationCenter.current()
            .removeDeliveredNotifications(withIdentifiers: [notificationID])
    }

    private func postRestoreNotification() {
        let content = UNMutableNotificationContent()
        content.title = "Volume restored"
        content.body  = "Your volume has been automatically restored."
        content.sound = .default

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 0.5, repeats: false)
        let request = UNNotificationRequest(
            identifier: notificationID + ".restore",
            content: content,
            trigger: trigger
        )
        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }

    // MARK: - Formatting

    func formatDuration(_ seconds: Int) -> String {
        let h = seconds / 3600
        let m = (seconds % 3600) / 60
        let s = seconds % 60
        if h > 0 && m > 0 { return "\(h)h \(m)m" }
        if h > 0           { return "\(h)h" }
        if m > 0 && s > 0  { return "\(m)m \(s)s" }
        if m > 0           { return "\(m)m" }
        return "\(s)s"
    }
}
