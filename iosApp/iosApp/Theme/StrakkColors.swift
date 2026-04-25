import SwiftUI

// MARK: - Strakk color tokens — single source of truth, mirror of DESIGN.md §2.
// Never use a hex literal in a View. All colors come from this file.

extension Color {
    // ---- Background & surfaces ----
    static let strakkBackground = Color(hex: "#050918")
    static let strakkBackgroundElevated = Color(hex: "#080D1F")
    static let strakkBackgroundEdge = Color(hex: "#0B1028")
    static let strakkSurface1 = Color(hex: "#10162F")
    static let strakkSurface1GradientTop = Color(hex: "#121833")
    static let strakkSurface1GradientBottom = Color(hex: "#0C1127")
    static let strakkSurface2 = Color(hex: "#151B38")
    static let strakkSurface3 = Color(hex: "#1A2142")

    // ---- Borders & dividers ----
    static let strakkBorderSubtle = Color(rgba: 0x7D89BE, alpha: 0.25)
    static let strakkBorderFaint = Color(rgba: 0x858FBE, alpha: 0.18)
    static let strakkDividerStrong = Color(rgba: 0x969DC8, alpha: 0.22)
    static let strakkDividerWeak = Color(white: 1.0, opacity: 0.12)
    /// Legacy alias kept for transitional callers.
    static let strakkDivider = Color(white: 1.0, opacity: 0.12)

    // ---- Text ----
    static let strakkTextPrimary = Color(hex: "#F4F6FF")
    static let strakkTextSecondary = Color(hex: "#9CA1B8")
    static let strakkTextTertiary = Color(hex: "#6F748C")
    static let strakkTextDisabled = Color(hex: "#50566F")

    // ---- Accent — Orange (primary, protein, Rapide) ----
    static let strakkPrimary = Color(hex: "#FF7A3D")
    static let strakkPrimaryLight = Color(hex: "#FF9A55")
    static let strakkAccentOrange = Color(hex: "#FF7A3D")
    static let strakkAccentOrangeLight = Color(hex: "#FF9A55")
    static let strakkAccentOrangeGlow = Color(rgba: 0xFF7A3D, alpha: 0.35)
    static let strakkAccentOrangeFaint = Color(rgba: 0xFF7A3D, alpha: 0.08)
    static let strakkAccentOrangeBorder = Color(rgba: 0xFF7A3D, alpha: 0.18)

    // ---- Accent — Blue (water) ----
    static let strakkWater = Color(hex: "#4B8DFF")
    static let strakkAccentBlue = Color(hex: "#4B8DFF")
    static let strakkAccentBlueLight = Color(hex: "#67B7FF")
    static let strakkAccentBlueGlow = Color(rgba: 0x4B8DFF, alpha: 0.35)
    static let strakkAccentBlueFaint = Color(rgba: 0x4B8DFF, alpha: 0.12)
    static let strakkAccentBlueBorder = Color(rgba: 0x4B8DFF, alpha: 0.28)

    // ---- Accent — Yellow (lipids) ----
    static let strakkAccentYellow = Color(hex: "#FFC84D")
    static let strakkAccentYellowFaint = Color(rgba: 0xFFC84D, alpha: 0.10)
    static let strakkAccentYellowBorder = Color(rgba: 0xFFC84D, alpha: 0.22)

    // ---- Accent — Indigo (carbs) ----
    static let strakkAccentIndigo = Color(hex: "#637CFF")
    static let strakkAccentIndigoFaint = Color(rgba: 0x637CFF, alpha: 0.10)
    static let strakkAccentIndigoBorder = Color(rgba: 0x637CFF, alpha: 0.22)

    // ---- Semantic ----
    static let strakkSuccess = Color(hex: "#4DAE6A")
    static let strakkError = Color(hex: "#E05252")
    static let strakkWarning = Color(hex: "#E0A84D")

    // ---- Legacy aliases (kept for transitional callers; prefer the canonical names above) ----
    static let strakkBackgroundMuted = Color(hex: "#080D1F")
    static let strakkProtein = Color(hex: "#FF7A3D")
    static let strakkCalories = Color(hex: "#FF9A55")
}

// MARK: - Color initializers

private extension Color {
    init(hex: String) {
        let cleaned = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: cleaned).scanHexInt64(&int)
        let r = Double((int >> 16) & 0xFF) / 255.0
        let g = Double((int >> 8) & 0xFF) / 255.0
        let b = Double(int & 0xFF) / 255.0
        self.init(red: r, green: g, blue: b)
    }

    init(rgba: UInt32, alpha: Double) {
        let r = Double((rgba >> 16) & 0xFF) / 255.0
        let g = Double((rgba >> 8) & 0xFF) / 255.0
        let b = Double(rgba & 0xFF) / 255.0
        self.init(red: r, green: g, blue: b, opacity: alpha)
    }
}
