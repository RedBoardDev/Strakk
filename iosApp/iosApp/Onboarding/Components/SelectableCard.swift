import SwiftUI

struct SelectableCard: View {
    let icon: String
    let title: String
    var subtitle: String?
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: StrakkSpacing.sm) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundStyle(isSelected ? Color.strakkPrimary : Color.strakkTextSecondary)
                    .frame(width: 32, height: 32)

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextPrimary)

                    if let subtitle {
                        Text(subtitle)
                            .font(.strakkCaption)
                            .foregroundStyle(Color.strakkTextSecondary)
                    }
                }

                Spacer()

                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.body)
                        .foregroundStyle(Color.strakkPrimary)
                }
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
            .overlay(
                RoundedRectangle(cornerRadius: StrakkRadius.sm)
                    .strokeBorder(
                        isSelected ? Color.strakkPrimary : Color.strakkDivider,
                        lineWidth: isSelected ? 2 : 1
                    )
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title)
        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
    }
}
