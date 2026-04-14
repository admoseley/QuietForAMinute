import SwiftUI

/// Shown when volume hits zero — equivalent to Android's MuteDurationDialog overlay.
/// On iOS this is a bottom sheet (no system overlay permitted).
struct MuteDurationSheet: View {

    let initialRestoreVolume: Float
    let onConfirm: (Int, Int, Float) -> Void
    let onDismiss: () -> Void

    @State private var hours   = 0
    @State private var minutes = 30
    @State private var restoreVolume: Float

    init(
        initialRestoreVolume: Float,
        onConfirm: @escaping (Int, Int, Float) -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.initialRestoreVolume = initialRestoreVolume
        self.onConfirm = onConfirm
        self.onDismiss = onDismiss
        _restoreVolume = State(initialValue: initialRestoreVolume)
    }

    private var durationIsZero: Bool { hours == 0 && minutes == 0 }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {

                    // Header
                    VStack(spacing: 8) {
                        Image(systemName: "speaker.slash.fill")
                            .font(.system(size: 40))
                            .foregroundStyle(.tint)
                        Text("Mute for how long?")
                            .font(.title2.weight(.bold))
                    }
                    .padding(.top, 8)

                    // Duration picker
                    GroupBox {
                        HStack(spacing: 0) {
                            // Hours
                            Picker("Hours", selection: $hours) {
                                ForEach(0..<24, id: \.self) { h in
                                    Text("\(h)h").tag(h)
                                }
                            }
                            .pickerStyle(.wheel)
                            .frame(maxWidth: .infinity)

                            // Minutes
                            Picker("Minutes", selection: $minutes) {
                                ForEach(0..<60, id: \.self) { m in
                                    Text("\(m)m").tag(m)
                                }
                            }
                            .pickerStyle(.wheel)
                            .frame(maxWidth: .infinity)
                        }
                        .frame(height: 150)
                    } label: {
                        Label("Duration", systemImage: "timer")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.secondary)
                    }

                    // Restore volume
                    GroupBox {
                        VStack(spacing: 8) {
                            HStack {
                                Label("Restore Volume", systemImage: volumeIcon)
                                    .font(.subheadline)
                                Spacer()
                                Text("\(Int(restoreVolume * 100))%")
                                    .font(.subheadline.weight(.bold))
                                    .foregroundStyle(.tint)
                                    .monospacedDigit()
                            }
                            Slider(value: $restoreVolume, in: 0...1)
                        }
                        .padding(.vertical, 4)
                    } label: {
                        Label("Volume After Mute", systemImage: "speaker.wave.2")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.secondary)
                    }

                    // Action buttons
                    VStack(spacing: 12) {
                        Button {
                            onConfirm(hours, minutes, restoreVolume)
                        } label: {
                            Label("Start Mute", systemImage: "play.fill")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .controlSize(.large)
                        .disabled(durationIsZero)

                        Button("Skip — keep muted", role: .cancel) {
                            onDismiss()
                        }
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    }

                    Spacer(minLength: 8)
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 20)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Dismiss") { onDismiss() }
                }
            }
        }
    }

    private var volumeIcon: String {
        if restoreVolume == 0        { return "speaker.slash" }
        if restoreVolume < 0.4       { return "speaker.wave.1" }
        if restoreVolume < 0.75      { return "speaker.wave.2" }
        return "speaker.wave.3"
    }
}
