import SwiftUI

struct MainTabView: View {

    @EnvironmentObject private var muteTimerService: MuteTimerService
    @EnvironmentObject private var preferences: PreferencesStore

    var body: some View {
        TabView {
            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape.fill")
                }

            ScheduleListView()
                .tabItem {
                    Label("Schedules", systemImage: "calendar")
                }
        }
        // Mute duration sheet — shown from anywhere in the app
        .sheet(isPresented: $muteTimerService.showMuteDurationSheet) {
            MuteDurationSheet(
                initialRestoreVolume: preferences.restoreVolume,
                onConfirm: { hours, minutes, restoreVolume in
                    let totalMinutes = hours * 60 + minutes
                    muteTimerService.start(durationMinutes: totalMinutes, restoreVolume: restoreVolume)
                    muteTimerService.showMuteDurationSheet = false
                },
                onDismiss: {
                    muteTimerService.showMuteDurationSheet = false
                }
            )
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
        // Active timer banner at top of each tab
        .safeAreaInset(edge: .top) {
            if muteTimerService.isRunning {
                TimerBanner()
            }
        }
    }
}

// MARK: - TimerBanner

private struct TimerBanner: View {
    @EnvironmentObject private var timerService: MuteTimerService

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "speaker.slash.fill")
                .foregroundStyle(.white)
            Text("Muted · \(timerService.formatDuration(timerService.remainingSeconds))")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.white)
                .monospacedDigit()
            Spacer()
            Button("Cancel") {
                // Restore volume before cancelling
                VolumeController.shared.setVolume(
                    timerService.preferences?.restoreVolume ?? 0.5
                )
                timerService.stop()
            }
            .font(.subheadline.weight(.medium))
            .foregroundStyle(.white)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Color.accentColor.gradient)
        .transition(.move(edge: .top).combined(with: .opacity))
        .animation(.spring(response: 0.35), value: timerService.isRunning)
    }
}
