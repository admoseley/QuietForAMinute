import SwiftUI

struct ScheduleListView: View {

    @EnvironmentObject private var store: ScheduleStore
    @StateObject private var viewModel = SchedulesViewModel()

    var body: some View {
        NavigationStack {
            Group {
                if store.schedules.isEmpty {
                    emptyState
                } else {
                    scheduleList
                }
            }
            .navigationTitle("Schedules")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button { viewModel.addNew() } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $viewModel.showEditSheet) {
                if let schedule = viewModel.editingSchedule {
                    ScheduleEditView(
                        schedule: schedule,
                        onSave: { updated in
                            if store.schedules.contains(where: { $0.id == updated.id }) {
                                store.update(updated)
                            } else {
                                store.add(updated)
                            }
                            viewModel.showEditSheet = false
                        },
                        onDelete: { deleted in
                            store.delete(deleted)
                            viewModel.showEditSheet = false
                        },
                        onCancel: {
                            viewModel.showEditSheet = false
                        }
                    )
                }
            }
            .confirmationDialog(
                "Delete \"\(viewModel.scheduleToDelete?.label ?? "")\"?",
                isPresented: $viewModel.showDeleteConfirm,
                titleVisibility: .visible
            ) {
                Button("Delete", role: .destructive) {
                    if let s = viewModel.scheduleToDelete { store.delete(s) }
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("All future alarms for this schedule will be removed.")
            }
        }
    }

    // MARK: - List

    private var scheduleList: some View {
        List {
            ForEach(store.schedules) { schedule in
                ScheduleRow(
                    schedule: schedule,
                    onToggle: { store.setEnabled(schedule, enabled: $0) },
                    onEdit:   { viewModel.edit(schedule) }
                )
                .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                    Button(role: .destructive) {
                        viewModel.confirmDelete(schedule)
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
    }

    // MARK: - Empty state

    private var emptyState: some View {
        VStack(spacing: 20) {
            Image(systemName: "calendar.badge.clock")
                .font(.system(size: 64))
                .foregroundStyle(.quaternary)

            Text("No schedules yet")
                .font(.title3.weight(.semibold))
                .foregroundStyle(.secondary)

            Text("Create a schedule to automatically mute your phone at specific times — like every weekday morning or Friday afternoons.")
                .font(.subheadline)
                .foregroundStyle(.tertiary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Button {
                viewModel.addNew()
            } label: {
                Label("Create First Schedule", systemImage: "plus")
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}

// MARK: - ScheduleRow

private struct ScheduleRow: View {
    let schedule: Schedule
    let onToggle: (Bool) -> Void
    let onEdit:   () -> Void

    var body: some View {
        Button(action: onEdit) {
            HStack(alignment: .center, spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    Text(schedule.label.isEmpty ? "Unnamed schedule" : schedule.label)
                        .font(.headline)
                        .foregroundStyle(schedule.isEnabled ? .primary : .secondary)

                    // Day chips
                    DayChipsDisplay(activeDays: schedule.days)

                    // Time & duration
                    HStack(spacing: 14) {
                        Label(schedule.formattedTime, systemImage: "clock")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Label(schedule.formattedDuration, systemImage: "timer")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }

                Spacer()

                Toggle("", isOn: Binding(
                    get: { schedule.isEnabled },
                    set: { onToggle($0) }
                ))
                .labelsHidden()
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .padding(.vertical, 4)
    }
}

// MARK: - DayChipsDisplay

struct DayChipsDisplay: View {
    let activeDays: Set<Schedule.Weekday>

    private let ordered: [Schedule.Weekday] = [
        .monday, .tuesday, .wednesday, .thursday, .friday, .saturday, .sunday
    ]

    var body: some View {
        HStack(spacing: 4) {
            ForEach(ordered) { day in
                let active = activeDays.contains(day)
                Text(day.shortName)
                    .font(.system(size: 10, weight: .semibold))
                    .padding(.horizontal, 5)
                    .padding(.vertical, 3)
                    .background(active ? Color.accentColor : Color(.systemGray5))
                    .foregroundStyle(active ? .white : Color(.systemGray2))
                    .clipShape(Capsule())
            }
        }
    }
}
