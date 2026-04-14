import UserNotifications
import Foundation

/// Manages local notifications for scheduled mutes.
///
/// Each (schedule × day-of-week) pair gets its own repeating
/// UNCalendarNotificationTrigger — the same one-per-combination
/// approach used by AlarmManager on Android.
final class NotificationScheduler {

    static let shared = NotificationScheduler()
    private init() { registerCategories() }

    // MARK: - Notification category / actions

    static let categoryID     = "MUTE_SCHEDULE"
    static let startActionID  = "START_MUTE"
    static let skipActionID   = "SKIP_MUTE"

    /// Payload key carrying the schedule duration in minutes.
    static let durationKey    = "durationMinutes"
    /// Payload key carrying the schedule label.
    static let labelKey       = "scheduleLabel"

    private func registerCategories() {
        let start = UNNotificationAction(
            identifier: Self.startActionID,
            title: "Start Timer",
            options: [.foreground]
        )
        let skip = UNNotificationAction(
            identifier: Self.skipActionID,
            title: "Skip",
            options: []
        )
        let category = UNNotificationCategory(
            identifier: Self.categoryID,
            actions: [start, skip],
            intentIdentifiers: [],
            options: []
        )
        UNUserNotificationCenter.current().setNotificationCategories([category])
    }

    // MARK: - Request permission

    func requestAuthorization() async -> Bool {
        do {
            return try await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .sound, .badge])
        } catch {
            print("[NotificationScheduler] Auth error: \(error)")
            return false
        }
    }

    // MARK: - Schedule / cancel

    func schedule(_ schedule: Schedule) {
        for day in schedule.days {
            let content = UNMutableNotificationContent()
            content.title = "Quiet For A Minute"
            content.body  = "\(schedule.label) — tap to start your \(schedule.formattedDuration) mute"
            content.sound = .default
            content.categoryIdentifier = Self.categoryID
            content.userInfo = [
                Self.durationKey: schedule.durationMinutes,
                Self.labelKey:    schedule.label
            ]

            var components = DateComponents()
            components.weekday = day.calendarWeekday
            components.hour    = schedule.triggerHour
            components.minute  = schedule.triggerMinute

            let trigger = UNCalendarNotificationTrigger(
                dateMatching: components,
                repeats: true
            )

            let request = UNNotificationRequest(
                identifier: schedule.notificationId(for: day),
                content: content,
                trigger: trigger
            )

            UNUserNotificationCenter.current().add(request) { error in
                if let error {
                    print("[NotificationScheduler] Failed to add \(request.identifier): \(error)")
                }
            }
        }
    }

    func cancel(_ schedule: Schedule) {
        let ids = Schedule.Weekday.allCases.map { schedule.notificationId(for: $0) }
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ids)
    }

    func cancelAll() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
    }
}
