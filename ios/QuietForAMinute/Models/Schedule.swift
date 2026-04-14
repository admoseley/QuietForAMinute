import Foundation

struct Schedule: Identifiable, Codable, Equatable {
    var id: UUID = UUID()
    var label: String
    var days: Set<Weekday>
    var triggerHour: Int
    var triggerMinute: Int
    var durationMinutes: Int
    var isEnabled: Bool = true

    // MARK: - Weekday

    enum Weekday: Int, Codable, CaseIterable, Identifiable, Comparable {
        // Values match Calendar's weekday component (1=Sun … 7=Sat)
        case sunday = 1, monday = 2, tuesday = 3, wednesday = 4,
             thursday = 5, friday = 6, saturday = 7

        var id: Int { rawValue }

        var shortName: String {
            switch self {
            case .sunday:    return "Sun"
            case .monday:    return "Mon"
            case .tuesday:   return "Tue"
            case .wednesday: return "Wed"
            case .thursday:  return "Thu"
            case .friday:    return "Fri"
            case .saturday:  return "Sat"
            }
        }

        var fullName: String {
            switch self {
            case .sunday:    return "Sunday"
            case .monday:    return "Monday"
            case .tuesday:   return "Tuesday"
            case .wednesday: return "Wednesday"
            case .thursday:  return "Thursday"
            case .friday:    return "Friday"
            case .saturday:  return "Saturday"
            }
        }

        // Calendar weekday number (same as rawValue)
        var calendarWeekday: Int { rawValue }

        static func < (lhs: Weekday, rhs: Weekday) -> Bool {
            // Display order: Mon … Sun
            let order: [Weekday] = [.monday, .tuesday, .wednesday, .thursday, .friday, .saturday, .sunday]
            return (order.firstIndex(of: lhs) ?? 0) < (order.firstIndex(of: rhs) ?? 0)
        }

        static var weekdays: Set<Weekday> { [.monday, .tuesday, .wednesday, .thursday, .friday] }
        static var weekend:  Set<Weekday> { [.saturday, .sunday] }
        static var allDays:  Set<Weekday> { Set(Weekday.allCases) }
    }

    // MARK: - Formatted helpers

    var formattedTime: String {
        let amPm  = triggerHour < 12 ? "AM" : "PM"
        let hour12: Int
        if      triggerHour == 0  { hour12 = 12 }
        else if triggerHour > 12  { hour12 = triggerHour - 12 }
        else                      { hour12 = triggerHour }
        return String(format: "%d:%02d %@", hour12, triggerMinute, amPm)
    }

    var formattedDuration: String {
        let h = durationMinutes / 60
        let m = durationMinutes % 60
        if h > 0 && m > 0 { return "\(h)h \(m)m" }
        if h > 0           { return "\(h)h" }
        return "\(m)m"
    }

    /// Notification identifier for a given day — one per (schedule × day) pair.
    func notificationId(for day: Weekday) -> String {
        "\(id.uuidString)-\(day.rawValue)"
    }
}
