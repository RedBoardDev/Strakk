import SwiftUI

// MARK: - StrakkEmptyState — DESIGN.md §5 "Empty / loading / error states".

struct StrakkEmptyState: View {
    let icon: String
    let title: LocalizedStringKey
    let message: LocalizedStringKey?
    var action: (() -> Void)?
    var actionTitle: LocalizedStringKey?

    init(
        icon: String,
        title: LocalizedStringKey,
        message: LocalizedStringKey? = nil,
        actionTitle: LocalizedStringKey? = nil,
        action: (() -> Void)? = nil
    ) {
        self.icon = icon
        self.title = title
        self.message = message
        self.actionTitle = actionTitle
        self.action = action
    }

    var body: some View {
        VStack(spacing: StrakkSpacing.md) {
            ZStack {
                Circle()
                    .fill(Color.strakkSurface1)
                    .frame(width: 64, height: 64)
                Image(systemName: icon)
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundStyle(Color.strakkTextSecondary)
            }

            Text(title)
                .font(.strakkHeading3)
                .foregroundStyle(Color.strakkTextPrimary)
                .multilineTextAlignment(.center)

            if let message {
                Text(message)
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .multilineTextAlignment(.center)
            }

            if let action, let actionTitle {
                StrakkPrimaryButton(title: actionTitle, action: action)
                    .padding(.top, StrakkSpacing.xs)
                    .padding(.horizontal, StrakkSpacing.xxxl)
            }
        }
        .padding(.vertical, StrakkSpacing.xxxl)
        .padding(.horizontal, StrakkSpacing.lg)
        .frame(maxWidth: .infinity)
    }
}

// MARK: - StrakkLoadingState

struct StrakkLoadingState: View {
    var label: LocalizedStringKey?

    var body: some View {
        VStack(spacing: StrakkSpacing.md) {
            ProgressView()
                .tint(Color.strakkPrimary)
                .controlSize(.large)
            if let label {
                Text(label)
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextSecondary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview("Empty") {
    StrakkEmptyState(
        icon: "fork.knife",
        title: "No meals yet",
        message: "Add your first meal to start tracking your day.",
        actionTitle: "Add a meal",
        action: {}
    )
    .background(Color.strakkBackground)
}

#Preview("Loading") {
    StrakkLoadingState(label: "Loading…")
        .background(Color.strakkBackground)
}
