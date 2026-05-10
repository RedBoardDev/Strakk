import SwiftUI

/// Minimal picker presented when the user taps "+" in the review items section.
/// Offers Search and Manual entry options only (no photo/text scan).
struct ReviewAddPickerSheet: View {
    let onSearch: () -> Void
    let onManual: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                VStack(spacing: 0) {
                    pickerRow(
                        icon: "magnifyingglass",
                        iconColor: Color.strakkPrimary,
                        title: "Search food",
                        subtitle: "Find from the catalog",
                        action: onSearch
                    )

                    Divider()
                        .background(Color.strakkDivider)
                        .padding(.leading, 68)

                    pickerRow(
                        icon: "pencil",
                        iconColor: Color.strakkTextSecondary,
                        title: "Enter manually",
                        subtitle: "Type name and macros",
                        action: onManual
                    )
                }
                .background(Color.strakkSurface1)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .padding(.horizontal, StrakkSpacing.lg)
                .padding(.top, StrakkSpacing.md)

                Spacer()
            }
            .navigationTitle("Add item")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                StrakkCloseToolbarItem(action: onDismiss)
            }
        }
        .presentationDetents([.height(260)])
        .presentationDragIndicator(.visible)
    }

    private func pickerRow(
        icon: String,
        iconColor: Color,
        title: LocalizedStringKey,
        subtitle: LocalizedStringKey,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(iconColor)
                    .frame(width: 36, height: 36)
                    .background(Color.strakkSurface2)
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Text(subtitle)
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(Color.strakkTextTertiary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .contentShape(Rectangle())
        }
        .accessibilityLabel(title)
    }
}
