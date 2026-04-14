import Foundation
import Combine

@MainActor
final class SchedulesViewModel: ObservableObject {

    // Edit sheet state
    @Published var editingSchedule: Schedule? = nil
    @Published var showEditSheet = false

    // Confirmation for delete
    @Published var scheduleToDelete: Schedule? = nil
    @Published var showDeleteConfirm = false

    func addNew() {
        editingSchedule = Schedule(
            label: "",
            days: [],
            triggerHour: 9,
            triggerMinute: 0,
            durationMinutes: 60
        )
        showEditSheet = true
    }

    func edit(_ schedule: Schedule) {
        editingSchedule = schedule
        showEditSheet = true
    }

    func confirmDelete(_ schedule: Schedule) {
        scheduleToDelete = schedule
        showDeleteConfirm = true
    }
}
