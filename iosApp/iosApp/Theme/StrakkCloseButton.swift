import SwiftUI

// MARK: - StrakkCloseButton — canonical sheet/modal dismiss control.
//
// DESIGN.md §5 "Buttons / Close":
//   - SF Symbol `xmark` (plain, not filled).
//   - Tint `text-secondary`, switches to `text-primary` on press.
//   - 44pt minimum tap target (iOS HIG).
//   - Localized accessibility label "Close".
//
// Use `.toolbar { StrakkCloseToolbarItem(action:) }` on every sheet header so
// the dismiss control sits in the same place across the app.

struct StrakkCloseButton: View {
    let action: () -> Void
    var placement: Placement = .leading

    enum Placement {
        case leading
        case trailingFloating
    }

    var body: some View {
        Button(action: {
            HapticEngine.light()
            action()
        }, label: {
            switch placement {
            case .leading:
                Image(systemName: "xmark")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.strakkTextSecondary)
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            case .trailingFloating:
                ZStack {
                    Circle()
                        .fill(Color.strakkSurface2)
                        .frame(width: 32, height: 32)
                    Image(systemName: "xmark")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(Color.strakkTextSecondary)
                }
                .frame(width: 44, height: 44)
                .contentShape(Circle())
            }
        })
        .buttonStyle(.plain)
        .accessibilityLabel(Text("Close"))
    }
}

// MARK: - Toolbar helper

/// Drop-in `ToolbarContent` placing a leading `StrakkCloseButton`.
///
/// ```swift
/// .toolbar { StrakkCloseToolbarItem { dismiss() } }
/// ```
struct StrakkCloseToolbarItem: ToolbarContent {
    let action: () -> Void

    var body: some ToolbarContent {
        ToolbarItem(placement: .topBarLeading) {
            StrakkCloseButton(action: action)
                .padding(.leading, -8)
        }
    }
}

#Preview {
    NavigationStack {
        Color.strakkBackground.ignoresSafeArea()
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { StrakkCloseToolbarItem(action: {}) }
    }
}
