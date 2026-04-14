import Foundation
import Combine
import AVFoundation

/// Wraps all user preferences stored in UserDefaults.
/// Use @EnvironmentObject to share across the view hierarchy.
final class PreferencesStore: ObservableObject {

    // MARK: - Restore Volume (0.0 – 1.0, mirrors system scale)
    @Published var restoreVolume: Float {
        didSet { UserDefaults.standard.set(restoreVolume, forKey: Keys.restoreVolume) }
    }

    // MARK: - Overlay / popup
    @Published var overlayEnabled: Bool {
        didSet { UserDefaults.standard.set(overlayEnabled, forKey: Keys.overlayEnabled) }
    }

    // MARK: - Chimes
    @Published var chimeOnMute: Bool {
        didSet { UserDefaults.standard.set(chimeOnMute, forKey: Keys.chimeOnMute) }
    }
    @Published var chimeOnRestore: Bool {
        didSet { UserDefaults.standard.set(chimeOnRestore, forKey: Keys.chimeOnRestore) }
    }

    /// System sound name for mute chime (nil = default)
    @Published var muteChimeName: String? {
        didSet { UserDefaults.standard.set(muteChimeName, forKey: Keys.muteChimeName) }
    }
    /// System sound name for restore chime (nil = default)
    @Published var restoreChimeName: String? {
        didSet { UserDefaults.standard.set(restoreChimeName, forKey: Keys.restoreChimeName) }
    }

    // MARK: - Theme
    @Published var themeMode: ThemeMode {
        didSet { UserDefaults.standard.set(themeMode.rawValue, forKey: Keys.themeMode) }
    }

    enum ThemeMode: String, CaseIterable, Identifiable {
        case light, dark, system
        var id: String { rawValue }
        var displayName: String {
            switch self {
            case .light:  return "Light"
            case .dark:   return "Dark"
            case .system: return "System"
            }
        }
    }

    // MARK: - Init

    init() {
        let defaults = UserDefaults.standard
        restoreVolume   = defaults.object(forKey: Keys.restoreVolume)   as? Float  ?? 0.5
        overlayEnabled  = defaults.object(forKey: Keys.overlayEnabled)  as? Bool   ?? true
        chimeOnMute     = defaults.object(forKey: Keys.chimeOnMute)     as? Bool   ?? true
        chimeOnRestore  = defaults.object(forKey: Keys.chimeOnRestore)  as? Bool   ?? true
        muteChimeName   = defaults.string(forKey: Keys.muteChimeName)
        restoreChimeName = defaults.string(forKey: Keys.restoreChimeName)
        themeMode       = ThemeMode(rawValue: defaults.string(forKey: Keys.themeMode) ?? "") ?? .system
    }

    // MARK: - Keys

    private enum Keys {
        static let restoreVolume    = "pref.restoreVolume"
        static let overlayEnabled   = "pref.overlayEnabled"
        static let chimeOnMute      = "pref.chimeOnMute"
        static let chimeOnRestore   = "pref.chimeOnRestore"
        static let muteChimeName    = "pref.muteChimeName"
        static let restoreChimeName = "pref.restoreChimeName"
        static let themeMode        = "pref.themeMode"
    }
}
