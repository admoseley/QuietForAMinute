import SwiftUI

struct ScheduleEditView: View {

    let onSave:   (Schedule) -> Void
    let onDelete: (Schedule) -> Void
    let onCancel: () -> Void

    // Editable local copy
    @State private var schedule: Schedule

    // Validation
    @State private var labelError:    String? = nil
    @State private var daysError:     String? = nil
    @State private var durationError: String? = nil

    // Pickers
    @State private var showTimePicker     = false
    @State private var showDurationPicker = false
    @State private var showDeleteConfirm  = false

    // Time picker state
    @State private var triggerHour:    Int
    @State private var triggerMinute:  Int
    @State private var durationHours:  Int
    @State private var durationMins:   Int

    private var isNew: Bool { !schedule.label.isEmpty ? false : true }

    init(schedule: Schedule, onSave: @escaping (Schedule) -> Void,
         onDelete: @escaping (Schedule) -> Void, onCancel: @escaping () -> Void) {
        _schedule        = State(initialValue: schedule)
        _triggerHour     = State(initialValue: schedule.triggerHour)
        _triggerMinute   = State(initialValue: schedule.triggerMinute)
        _durationHours   = State(initialValue: schedule.durationMinutes / 60)
        _durationMins    = State(initialValue: schedule.durationMinutes % 60)
        self.onSave      = onSave
        self.onDelete    = onDelete
        self.onCancel    = onCancel
    }

    private var isCreating: Bool { schedule.label.isEmpty && schedule.days.isEmpty }

    var body: some View {
        NavigationStack {
            Form {

                // MARK: - Label
                Section {
                    TextField("e.g. Morning class, Friday meetings", text: $schedule.label)
                } header: {
                    Text("Schedule Name")
                } footer: {
                    if let err = labelError {
                        Text(err).foregroundStyle(.red)
                    }
                }

                // MARK: - Days
                Section {
                    DayChipSelector(
                        selectedDays: schedule.days,
                        onToggle: { day in
                            if schedule.days.contains(day) { schedule.days.remove(day) }
                            else                           { schedule.days.insert(day) }
                        }
                    )

                    // Quick presets
                    HStack(spacing: 8) {
                        presetChip("Weekdays")  { setDays(.weekdays) }
                        presetChip("Weekends")  { setDays(.weekend) }
                        presetChip("Every Day") { setDays(.allDays) }
                    }
                    .padding(.vertical, 4)

                } header: {
                    Text("Repeat On")
                } footer: {
                    if let err = daysError {
                        Text(err).foregroundStyle(.red)
                    }
                }

                // MARK: - Start Time
                Section("Start Time") {
                    HStack {
                        Text(schedule.formattedTime)
                            .font(.title2.weight(.semibold))
                            .foregroundStyle(.tint)
                        Spacer()
                        Button("Change") { showTimePicker = true }
                            .buttonStyle(.bordered)
                    }
                }

                // MARK: - Duration
                Section {
                    HStack {
                        Text(schedule.formattedDuration)
                            .font(.title2.weight(.semibold))
                            .foregroundStyle(.tint)
                        Spacer()
                        Button("Change") { showDurationPicker = true }
                            .buttonStyle(.bordered)
                    }
                } header: {
                    Text("Mute Duration")
                } footer: {
                    if let err = durationError {
                        Text(err).foregroundStyle(.red)
                    }
                }

                // MARK: - Delete (existing only)
                if !isCreating {
                    Section {
                        Button(role: .destructive) {
                            showDeleteConfirm = true
                        } label: {
                            Label("Delete Schedule", systemImage: "trash")
                                .frame(maxWidth: .infinity, alignment: .center)
                        }
                    }
                }
            }
            .navigationTitle(isCreating ? "New Schedule" : "Edit Schedule")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onCancel)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isCreating ? "Create" : "Save") {
                        attemptSave()
                    }
                    .fontWeight(.semibold)
                }
            }
            // Time picker sheet
            .sheet(isPresented: $showTimePicker) {
                TimePickerSheet(
                    title: "Start Time",
                    hour: $triggerHour,
                    minute: $triggerMinute,
                    is24Hour: false,
                    onDone: {
                        schedule.triggerHour   = triggerHour
                        schedule.triggerMinute = triggerMinute
                        showTimePicker = false
                    }
                )
                .presentationDetents([.medium])
            }
            // Duration picker sheet
            .sheet(isPresented: $showDurationPicker) {
                TimePickerSheet(
                    title: "Duration (Hours : Minutes)",
                    hour: $durationHours,
                    minute: $durationMins,
                    is24Hour: true,
                    onDone: {
                        schedule.durationMinutes = durationHours * 60 + durationMins
                        showDurationPicker = false
                    }
                )
                .presentationDetents([.medium])
            }
            // Delete confirmation
            .confirmationDialog(
                "Delete \"\(schedule.label)\"?",
                isPresented: $showDeleteConfirm,
                titleVisibility: .visible
            ) {
                Button("Delete", role: .destructive) { onDelete(schedule) }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("All future alarms for this schedule will be removed.")
            }
        }
    }

    // MARK: - Helpers

    private func attemptSave() {
        var valid = true
        labelError    = nil
        daysError     = nil
        durationError = nil

        if schedule.label.trimmingCharacters(in: .whitespaces).isEmpty {
            labelError = "Please enter a schedule name."
            valid = false
        }
        if schedule.days.isEmpty {
            daysError = "Please select at least one day."
            valid = false
        }
        if schedule.durationMinutes <= 0 {
            durationError = "Please select a duration greater than zero."
            valid = false
        }
        if valid { onSave(schedule) }
    }

    private func setDays(_ set: Set<Schedule.Weekday>) {
        schedule.days = set
    }

    @ViewBuilder
    private func presetChip(_ label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.caption.weight(.medium))
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(Color(.systemGray5))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - DayChipSelector

private struct DayChipSelector: View {
    let selectedDays: Set<Schedule.Weekday>
    let onToggle: (Schedule.Weekday) -> Void

    private let ordered: [Schedule.Weekday] = [
        .monday, .tuesday, .wednesday, .thursday, .friday, .saturday, .sunday
    ]

    var body: some View {
        HStack(spacing: 6) {
            ForEach(ordered) { day in
                let selected = selectedDays.contains(day)
                Button {
                    onToggle(day)
                } label: {
                    Text(day.shortName)
                        .font(.caption.weight(.semibold))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 6)
                        .background(selected ? Color.accentColor : Color(.systemGray5))
                        .foregroundStyle(selected ? .white : .primary)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
            }
        }
    }
}

// MARK: - TimePickerSheet

private struct TimePickerSheet: View {
    let title:    String
    @Binding var hour:   Int
    @Binding var minute: Int
    let is24Hour: Bool
    let onDone:   () -> Void

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Text(title)
                    .font(.headline)
                    .padding(.top, 16)

                HStack(spacing: 0) {
                    Picker("Hour", selection: $hour) {
                        ForEach(0..<(is24Hour ? 24 : 12), id: \.self) { h in
                            Text(is24Hour ? "\(h)h" : "\(h == 0 ? 12 : h)")
                                .tag(is24Hour ? h : (h == 0 ? 12 : h))
                        }
                    }
                    .pickerStyle(.wheel)
                    .frame(maxWidth: .infinity)

                    Text(":")
                        .font(.title.weight(.medium))

                    Picker("Minute", selection: $minute) {
                        ForEach(0..<60, id: \.self) { m in
                            Text(String(format: "%02d", m)).tag(m)
                        }
                    }
                    .pickerStyle(.wheel)
                    .frame(maxWidth: .infinity)

                    if !is24Hour {
                        Picker("AM/PM", selection: Binding(
                            get: { hour < 12 ? 0 : 1 },
                            set: { hour = $0 == 0 ? (hour % 12) : (hour % 12 + 12) }
                        )) {
                            Text("AM").tag(0)
                            Text("PM").tag(1)
                        }
                        .pickerStyle(.wheel)
                        .frame(maxWidth: .infinity)
                    }
                }
                .padding(.horizontal)
            }
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done", action: onDone).fontWeight(.semibold)
                }
            }
        }
    }
}
