import AVFoundation
import Combine
import UIKit

/// Monitors system output volume via AVAudioSession KVO.
///
/// Foreground: works immediately.
/// Background: requires the audio UIBackgroundMode and an active audio session.
/// A silent 1-second looping audio file keeps the session alive in background
/// (see SilentAudioPlayer). Apple permits this when the app has a legitimate
/// audio use-case; review your use-case before App Store submission.
final class VolumeMonitor: ObservableObject {

    @Published private(set) var currentVolume: Float = AVAudioSession.sharedInstance().outputVolume

    /// Set to true by MuteTimerService while it is programmatically restoring
    /// volume, so the restore action does not re-trigger the mute popup.
    var suppressNextChange = false

    private var observation: NSKeyValueObservation?
    private var onMuted: (() -> Void)?
    private var silentPlayer: SilentAudioPlayer?
    private let session = AVAudioSession.sharedInstance()

    // MARK: - Public API

    func startMonitoring(onMuted: @escaping () -> Void) {
        self.onMuted = onMuted

        do {
            // .playback keeps the session alive in background
            try session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try session.setActive(true)
        } catch {
            print("[VolumeMonitor] Audio session setup failed: \(error)")
        }

        observation = session.observe(\.outputVolume, options: [.new]) { [weak self] _, change in
            guard let self, let newVolume = change.newValue else { return }
            DispatchQueue.main.async {
                self.currentVolume = newVolume
                if newVolume == 0 {
                    if self.suppressNextChange {
                        self.suppressNextChange = false
                        return
                    }
                    self.onMuted?()
                }
            }
        }

        // Keep audio session alive in background with a silent loop
        silentPlayer = SilentAudioPlayer()
        silentPlayer?.play()
    }

    func stopMonitoring() {
        observation = nil
        silentPlayer?.stop()
        silentPlayer = nil
    }
}

// MARK: - SilentAudioPlayer

/// Plays a synthesised silent buffer in a loop, keeping AVAudioSession active
/// so that outputVolume KVO continues to fire while the app is backgrounded.
private final class SilentAudioPlayer {

    private var player: AVAudioPlayer?

    func play() {
        // Build a minimal valid WAV: 44-byte header + 1 sample of silence
        var wav = Data()

        func append<T: FixedWidthInteger>(_ value: T) {
            var v = value.littleEndian
            wav.append(contentsOf: withUnsafeBytes(of: &v, Array.init))
        }

        let dataSize: UInt32 = 2          // 1 sample × 2 bytes (16-bit mono)
        let byteRate: UInt32 = 44100 * 2  // sampleRate × blockAlign

        // RIFF header
        wav.append(contentsOf: "RIFF".utf8)
        append(UInt32(36 + dataSize))     // chunk size
        wav.append(contentsOf: "WAVE".utf8)

        // fmt sub-chunk
        wav.append(contentsOf: "fmt ".utf8)
        append(UInt32(16))                // sub-chunk size
        append(UInt16(1))                 // PCM
        append(UInt16(1))                 // mono
        append(UInt32(44100))             // sample rate
        append(byteRate)
        append(UInt16(2))                 // block align
        append(UInt16(16))                // bits per sample

        // data sub-chunk
        wav.append(contentsOf: "data".utf8)
        append(dataSize)
        append(UInt16(0))                 // one silent sample

        do {
            player = try AVAudioPlayer(data: wav, fileTypeHint: "wav")
            player?.numberOfLoops = -1   // loop forever
            player?.volume = 0
            player?.play()
        } catch {
            print("[SilentAudioPlayer] Failed: \(error)")
        }
    }

    func stop() {
        player?.stop()
        player = nil
    }
}
