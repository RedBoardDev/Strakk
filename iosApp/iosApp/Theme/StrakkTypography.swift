import SwiftUI

// MARK: - Strakk type scale — mirror of DESIGN.md §3.

extension Font {
    static let strakkDisplayHero = Font.system(size: 56, weight: .heavy, design: .default)
    static let strakkDisplay = Font.system(size: 32, weight: .bold, design: .default)
    static let strakkHeading1 = Font.system(size: 24, weight: .bold, design: .default)
    static let strakkHeading2 = Font.system(size: 20, weight: .bold, design: .default)
    static let strakkHeading3 = Font.system(size: 17, weight: .semibold, design: .default)
    static let strakkBodyLarge = Font.system(size: 17, weight: .medium, design: .default)
    static let strakkBody = Font.system(size: 15, weight: .regular, design: .default)
    static let strakkBodyBold = Font.system(size: 15, weight: .semibold, design: .default)
    static let strakkCaption = Font.system(size: 13, weight: .regular, design: .default)
    static let strakkCaptionBold = Font.system(size: 13, weight: .semibold, design: .default)
    static let strakkOverline = Font.system(size: 11, weight: .bold, design: .default)
}
