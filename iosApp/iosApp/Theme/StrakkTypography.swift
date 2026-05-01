import SwiftUI

// MARK: - Strakk type scale — mirror of DESIGN.md §3.
// Uses @ScaledMetric-compatible base sizes with semantic text style mapping
// so fonts scale with the user's Dynamic Type setting.

extension Font {
    static let strakkDisplayHero = Font.system(.largeTitle, design: .default, weight: .heavy)
    static let strakkDisplay = Font.system(.largeTitle, design: .default, weight: .bold)
    static let strakkHeading1 = Font.system(.title, design: .default, weight: .bold)
    static let strakkHeading2 = Font.system(.title2, design: .default, weight: .bold)
    static let strakkHeading3 = Font.system(.headline, design: .default, weight: .semibold)
    static let strakkBodyLarge = Font.system(.body, design: .default, weight: .medium)
    static let strakkBody = Font.system(.subheadline, design: .default, weight: .regular)
    static let strakkBodyBold = Font.system(.subheadline, design: .default, weight: .semibold)
    static let strakkCaption = Font.system(.caption, design: .default, weight: .regular)
    static let strakkCaptionBold = Font.system(.caption, design: .default, weight: .semibold)
    static let strakkOverline = Font.system(.caption2, design: .default, weight: .bold)
}
