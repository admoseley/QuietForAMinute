import Foundation
import Combine

/// Persists schedules as JSON in UserDefaults.
/// Observable via @Published so SwiftUI views update automatically.
final class ScheduleStore: ObservableObject {

    @Published private(set) var schedules: [Schedule] = []

    private let key = "com.admoseley.quietforaminute.schedules"
    private let notificationScheduler: NotificationScheduler

    init(notificationScheduler: NotificationScheduler = .shared) {
        self.notificationScheduler = notificationScheduler
        load()
    }

    // MARK: - CRUD

    func add(_ schedule: Schedule) {
        schedules.append(schedule)
        save()
        if schedule.isEnabled {
            notificationScheduler.schedule(schedule)
        }
    }

    func update(_ schedule: Schedule) {
        guard let index = schedules.firstIndex(where: { $0.id == schedule.id }) else { return }
        notificationScheduler.cancel(schedule)
        schedules[index] = schedule
        save()
        if schedule.isEnabled {
            notificationScheduler.schedule(schedule)
        }
    }

    func delete(_ schedule: Schedule) {
        notificationScheduler.cancel(schedule)
        schedules.removeAll { $0.id == schedule.id }
        save()
    }

    func setEnabled(_ schedule: Schedule, enabled: Bool) {
        var updated = schedule
        updated.isEnabled = enabled
        update(updated)
    }

    // MARK: - Persistence

    private func load() {
        guard let data = UserDefaults.standard.data(forKey: key),
              let decoded = try? JSONDecoder().decode([Schedule].self, from: data)
        else { return }
        schedules = decoded
    }

    private func save() {
        guard let data = try? JSONEncoder().encode(schedules) else { return }
        UserDefaults.standard.set(data, forKey: key)
    }
}
