import SwiftUI

// MARK: - Strakk button primitives — mirror of DESIGN.md §5 "Buttons".
//
// All app CTAs MUST go through one of these styles. Never re-implement the
// 52pt height + corner-radius + tap haptic combo per screen.

private enum StrakkButtonMetrics {
    static let height: CGFloat = 52
    static let cornerRadius: CGFloat = StrakkRadius.sm
    static let pressedScale: CGFloat = 0.97
}

// MARK: - Primary

struct StrakkPrimaryButtonStyle: ButtonStyle {
    var isEnabled: Bool = true

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.strakkBodyBold)
            .foregroundStyle(isEnabled ? Color.white : Color.strakkTextTertiary)
            .frame(maxWidth: .infinity)
            .frame(height: StrakkButtonMetrics.height)
            .background(
                isEnabled ? Color.strakkPrimary : Color.strakkSurface2,
                in: RoundedRectangle(cornerRadius: StrakkButtonMetrics.cornerRadius, style: .continuous)
            )
            .scaleEffect(configuration.isPressed ? StrakkButtonMetrics.pressedScale : 1.0)
            .opacity(configuration.isPressed ? 0.92 : 1.0)
            .animation(.easeOut(duration: 0.1), value: configuration.isPressed)
    }
}

struct StrakkPrimaryButton: View {
    let title: LocalizedStringKey
    let action: () -> Void
    var icon: String?
    var isEnabled: Bool = true
    var triggersHaptic: Bool = true

    var body: some View {
        Button {
            if triggersHaptic { HapticEngine.light() }
            action()
        } label: {
            HStack(spacing: StrakkSpacing.xs) {
                if let icon {
                    Image(systemName: icon)
                        .font(.system(size: 16, weight: .semibold))
                }
                Text(title)
            }
        }
        .buttonStyle(StrakkPrimaryButtonStyle(isEnabled: isEnabled))
        .disabled(!isEnabled)
    }
}

// MARK: - Secondary

struct StrakkSecondaryButtonStyle: ButtonStyle {
    var isEnabled: Bool = true

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.strakkBodyBold)
            .foregroundStyle(isEnabled ? Color.strakkTextPrimary : Color.strakkTextTertiary)
            .frame(maxWidth: .infinity)
            .frame(height: StrakkButtonMetrics.height)
            .background(
                configuration.isPressed ? Color.strakkSurface3 : Color.strakkSurface2,
                in: RoundedRectangle(cornerRadius: StrakkButtonMetrics.cornerRadius, style: .continuous)
            )
            .scaleEffect(configuration.isPressed ? StrakkButtonMetrics.pressedScale : 1.0)
            .animation(.easeOut(duration: 0.1), value: configuration.isPressed)
    }
}

struct StrakkSecondaryButton: View {
    let title: LocalizedStringKey
    let action: () -> Void
    var icon: String?
    var isEnabled: Bool = true

    var body: some View {
        Button(action: action) {
            HStack(spacing: StrakkSpacing.xs) {
                if let icon {
                    Image(systemName: icon)
                        .font(.system(size: 16, weight: .semibold))
                }
                Text(title)
            }
        }
        .buttonStyle(StrakkSecondaryButtonStyle(isEnabled: isEnabled))
        .disabled(!isEnabled)
    }
}

// MARK: - Destructive

struct StrakkDestructiveButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.strakkBodyBold)
            .foregroundStyle(Color.white)
            .frame(maxWidth: .infinity)
            .frame(height: StrakkButtonMetrics.height)
            .background(
                Color.strakkError,
                in: RoundedRectangle(cornerRadius: StrakkButtonMetrics.cornerRadius, style: .continuous)
            )
            .scaleEffect(configuration.isPressed ? StrakkButtonMetrics.pressedScale : 1.0)
            .opacity(configuration.isPressed ? 0.92 : 1.0)
            .animation(.easeOut(duration: 0.1), value: configuration.isPressed)
    }
}

struct StrakkDestructiveButton: View {
    let title: LocalizedStringKey
    let action: () -> Void

    var body: some View {
        Button {
            HapticEngine.medium()
            action()
        } label: {
            Text(title)
        }
        .buttonStyle(StrakkDestructiveButtonStyle())
    }
}

// MARK: - Text / link

struct StrakkTextButton: View {
    let title: LocalizedStringKey
    let action: () -> Void
    var emphasized: Bool = true

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(emphasized ? .strakkBodyBold : .strakkBody)
                .foregroundStyle(Color.strakkPrimary)
                .padding(.horizontal, StrakkSpacing.xs)
                .padding(.vertical, StrakkSpacing.xxs)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Convenience

extension View {
    /// Pin a primary CTA to the bottom of a screen with the canonical
    /// horizontal screen margin (`StrakkSpacing.lg`) and bottom padding.
    func strakkBottomCTA() -> some View {
        self
            .padding(.horizontal, StrakkSpacing.lg)
            .padding(.bottom, StrakkSpacing.md)
    }
}

#Preview("Strakk buttons") {
    VStack(spacing: StrakkSpacing.md) {
        StrakkPrimaryButton(title: "Continue", action: {})
        StrakkPrimaryButton(title: "Save changes", action: {}, icon: "checkmark")
        StrakkPrimaryButton(title: "Disabled", action: {}, isEnabled: false)
        StrakkSecondaryButton(title: "Skip for now", action: {})
        StrakkDestructiveButton(title: "Delete account", action: {})
        StrakkTextButton(title: "Forgot password?", action: {})
    }
    .padding(StrakkSpacing.lg)
    .background(Color.strakkBackground)
}
