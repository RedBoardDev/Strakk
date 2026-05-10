import SwiftUI

// MARK: - Strakk sheet primitives — mirror of DESIGN.md §5 "Sheets".
//
// Every sheet in the app should be wrapped with `.strakkSheet(title:)` and
// dismissed via `StrakkCloseToolbarItem`. This guarantees a consistent
// header (plain `xmark` leading, centered title), background, and detents.

enum StrakkSheetSize {
    case medium
    case large
    case mediumOrLarge

    var detents: Set<PresentationDetent> {
        switch self {
        case .medium: return [.medium]
        case .large: return [.large]
        case .mediumOrLarge: return [.medium, .large]
        }
    }
}

private struct StrakkSheetChromeModifier: ViewModifier {
    let title: LocalizedStringKey?
    let size: StrakkSheetSize
    let onClose: (() -> Void)?

    func body(content: Content) -> some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()
                content
            }
            .applyTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if let onClose {
                    StrakkCloseToolbarItem(action: onClose)
                }
            }
        }
        .presentationDetents(size.detents)
        .presentationDragIndicator(.visible)
        .presentationBackground(Color.strakkBackground)
    }
}

private extension View {
    @ViewBuilder
    func applyTitle(_ title: LocalizedStringKey?) -> some View {
        if let title {
            self.navigationTitle(Text(title))
        } else {
            self
        }
    }
}

extension View {
    /// Wrap the receiver in the canonical Strakk sheet chrome:
    /// - `NavigationStack` host
    /// - `Color.strakkBackground` behind the content
    /// - centered inline `navigationTitle` (when `title != nil`)
    /// - leading plain `xmark` close (when `onClose != nil`)
    /// - configured detents and drag indicator.
    func strakkSheet(
        title: LocalizedStringKey? = nil,
        size: StrakkSheetSize = .large,
        onClose: (() -> Void)? = nil
    ) -> some View {
        self.modifier(StrakkSheetChromeModifier(title: title, size: size, onClose: onClose))
    }
}

#Preview {
    Color.strakkBackground
        .overlay {
            Text("Sheet body")
                .foregroundStyle(Color.strakkTextPrimary)
        }
        .strakkSheet(title: "Add a meal", size: .medium, onClose: {})
}
