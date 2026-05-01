import Foundation

// MARK: - FeelingTagLabels
//
// Single source of truth for feeling tag ID → French label mapping.
// Used in: CheckInDetailView, WizardStepFeelingsView, WizardStepSummaryView

enum FeelingTagLabels {
    static func label(for tag: String) -> String {
        labels[tag] ?? tag
    }

    static let labels: [String: String] = [
        "energy_stable": String(localized: "Stable energy"),
        "good_energy": String(localized: "Good energy"),
        "motivated": String(localized: "Motivated"),
        "disciplined": String(localized: "Disciplined"),
        "good_sleep": String(localized: "Slept well"),
        "good_recovery": String(localized: "Good recovery"),
        "strong_training": String(localized: "Strong training"),
        "good_mood": String(localized: "Good mood"),
        "focused": String(localized: "Focused"),
        "light_body": String(localized: "Light body"),
        "good_digestion": String(localized: "Good digestion"),
        "low_energy": String(localized: "Low energy"),
        "tired": String(localized: "Tired"),
        "poor_sleep": String(localized: "Slept poorly"),
        "stress": String(localized: "Stress"),
        "low_motivation": String(localized: "Low motivation"),
        "heavy_body": String(localized: "Heavy body"),
        "sore": String(localized: "Sore"),
        "joint_discomfort": String(localized: "Joint discomfort"),
        "digestion_discomfort": String(localized: "Poor digestion"),
        "bloating": String(localized: "Bloating"),
        "hungry": String(localized: "Hungry"),
        "irritability": String(localized: "Irritable"),
        "low_mood": String(localized: "Low mood"),
    ]

    /// Positive tag IDs
    static let positiveIds: Set<String> = [
        "energy_stable", "good_energy", "motivated", "disciplined", "good_sleep",
        "good_recovery", "strong_training", "good_mood", "focused", "light_body", "good_digestion"
    ]

    /// Negative tag IDs
    static let negativeIds: Set<String> = [
        "low_energy", "tired", "poor_sleep", "stress", "low_motivation", "heavy_body",
        "sore", "joint_discomfort", "digestion_discomfort", "bloating", "hungry",
        "irritability", "low_mood"
    ]
}
