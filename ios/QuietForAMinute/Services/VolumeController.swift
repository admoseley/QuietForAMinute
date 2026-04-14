import MediaPlayer
import UIKit

/// Sets the system output volume programmatically.
///
/// iOS does not expose a public API for this. The widely-used workaround is
/// to embed a hidden MPVolumeView in the window hierarchy and manipulate its
/// internal UISlider. This is a grey-area technique — test thoroughly and
/// review Apple's guidelines before App Store submission.
final class VolumeController {

    static let shared = VolumeController()

    private var volumeView: MPVolumeView?

    private init() {}

    /// Call once after the main window is available (e.g. in AppDelegate didFinishLaunching).
    func install(in window: UIWindow) {
        let view = MPVolumeView(frame: CGRect(x: -3000, y: -3000, width: 1, height: 1))
        view.isHidden = false   // must NOT be hidden or the slider won't respond
        view.alpha = 0.001      // visually invisible
        window.addSubview(view)
        volumeView = view
    }

    /// Set volume in the range 0.0 – 1.0.
    func setVolume(_ volume: Float) {
        let clamped = max(0, min(1, volume))
        // Slight delay lets the audio session settle before the change registers
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { [weak self] in
            self?.applyVolume(clamped)
        }
    }

    private func applyVolume(_ volume: Float) {
        guard let slider = volumeView?
            .subviews
            .compactMap({ $0 as? UISlider })
            .first
        else {
            print("[VolumeController] MPVolumeView slider not found — call install(in:) first")
            return
        }
        slider.value = volume
    }

    /// Read the current system volume (0.0 – 1.0).
    var currentVolume: Float {
        AVAudioSession.sharedInstance().outputVolume
    }
}
