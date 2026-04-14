import AVFoundation
import AudioToolbox

/// Plays a one-shot chime sound.
/// Supports system sound names or falls back to a built-in AudioServices sound.
final class ChimePlayer {

    static let shared = ChimePlayer()
    private var player: AVAudioPlayer?

    private init() {}

    /// Play a chime identified by system sound name.
    /// Pass nil to use the default (system notification sound).
    func playChime(named name: String?) {
        if let name {
            playSystemSound(named: name)
        } else {
            // Default: UIKit "notification" sound (kSystemSoundID_Vibrate alternative)
            AudioServicesPlaySystemSound(1007) // "new-mail.caf" — pleasant chime
        }
    }

    private func playSystemSound(named name: String) {
        // Search common system sound directories
        let directories = [
            "/System/Library/Audio/UISounds/",
            "/System/Library/Audio/UISounds/Modern/",
            "/Library/Audio/UISounds/"
        ]
        for dir in directories {
            let url = URL(fileURLWithPath: dir + name)
            if FileManager.default.fileExists(atPath: url.path) {
                var soundID: SystemSoundID = 0
                AudioServicesCreateSystemSoundID(url as CFURL, &soundID)
                AudioServicesPlaySystemSound(soundID)
                return
            }
        }
        // Fallback
        AudioServicesPlaySystemSound(1007)
    }
}
