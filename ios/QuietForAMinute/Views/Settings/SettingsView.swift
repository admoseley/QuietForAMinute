import SwiftUI
import UserNotifications

struct SettingsView: View {

    @EnvironmentObject private var preferences: PreferencesStore
    @EnvironmentObject private var timerService: MuteTimerService
    @StateObject private var viewModel = SettingsViewModel()

    var body: some View {
        NavigationStack {
            List {

                // MARK: - Hero card
                Section {
                    heroCard
                }
                .listRowInsets(.init())
                .listRowBackground(Color.clear)

                // MARK: - Restore Volume
                Section {
                    restoreVolumeRow
                } header: {
                    Text("Restore Volume")
                } footer: {
                    Text("Volume restored to this level when a mute timer expires.")
                }

                // MARK: - App Settings
                Section("App Settings") {
                    themePicker
                    overlayToggle
                    chimeOnMuteRow
                    chimeOnRestoreRow
                }

                // MARK: - Permissions
                Section("Permissions") {
                    notificationPermissionRow
                }
            }
            .navigationTitle("Quiet For A Minute")
            .navigationBarTitleDisplayMode(.large)
            .task { await viewModel.refreshPermissions() }
            .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
                Task { await viewModel.refreshPermissions() }
            }
        }
    }

    // MARK: - Hero card

    private var heroCard: some View {
        HStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 4) {
                Text("Mute when you need it.")
                    .font(.headline.weight(.bold))
                Text("Restore when you forget.")
                    .font(.headline.weight(.bold))
            }
            Spacer()
            Image(systemName: "speaker.slash.circle.fill")
                .font(.system(size: 52))
                .foregroundStyle(.tint)
        }
        .padding(20)
        .background(Color(.systemGray6))
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    // MARK: - Restore Volume row

    private var restoreVolumeRow: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: volumeIcon(for: preferences.restoreVolume))
                    .foregroundStyle(.tint)
                    .frame(width: 24)
                Text("Default Volume")
                Spacer()
                Text("\(Int(preferences.restoreVolume * 100))%")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(.tint)
                    .monospacedDigit()
            }
            Slider(value: $preferences.restoreVolume, in: 0...1)
        }
        .padding(.vertical, 4)
    }

    private func volumeIcon(for v: Float) -> String {
        if v == 0   { return "speaker.slash" }
        if v < 0.4  { return "speaker.wave.1" }
        if v < 0.75 { return "speaker.wave.2" }
        return "speaker.wave.3"
    }

    // MARK: - Theme picker

    private var themePicker: some View {
        HStack {
            Label("App Theme", systemImage: "paintpalette")
            Spacer()
            Picker("Theme", selection: $preferences.themeMode) {
                ForEach(PreferencesStore.ThemeMode.allCases) { mode in
                    Text(mode.displayName).tag(mode)
                }
            }
            .pickerStyle(.menu)
        }
    }

    // MARK: - Overlay toggle

    private var overlayToggle: some View {
        Toggle(isOn: $preferences.overlayEnabled) {
            Label("Show popup on mute", systemImage: "timer")
        }
    }

    // MARK: - Chime rows

    private var chimeOnMuteRow: some View {
        HStack {
            Label("Chime on mute", systemImage: "speaker.wave.3")
            Spacer()
            Toggle("", isOn: $preferences.chimeOnMute)
                .labelsHidden()
        }
    }

    private var chimeOnRestoreRow: some View {
        HStack {
            Label("Chime on restore", systemImage: "bell.and.waves.left.and.right")
            Spacer()
            Toggle("", isOn: $preferences.chimeOnRestore)
                .labelsHidden()
        }
    }

    // MARK: - Permissions

    private var notificationPermissionRow: some View {
        HStack {
            Label {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Notifications")
                    Text("Required for timer and schedule alerts")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            } icon: {
                Image(systemName: viewModel.notificationsGranted
                      ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                    .foregroundStyle(viewModel.notificationsGranted ? .green : .orange)
            }

            Spacer()

            if !viewModel.notificationsGranted {
                Button("Grant") {
                    if let url = URL(string: UIApplication.openSettingsURLString) {
                        UIApplication.shared.open(url)
                    }
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.small)
            } else {
                Text("Granted")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.green)
            }
        }
        .padding(.vertical, 4)
    }
}
