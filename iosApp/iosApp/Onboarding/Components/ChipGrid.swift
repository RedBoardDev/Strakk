import SwiftUI

struct ChipItem: Identifiable {
    let id: String
    let label: String
    let isSelected: Bool
}

struct ChipGrid: View {
    let chips: [ChipItem]
    let onToggle: (String) -> Void

    var body: some View {
        FlowLayout(spacing: StrakkSpacing.xs) {
            ForEach(chips) { chip in
                Button {
                    onToggle(chip.id)
                } label: {
                    Text(chip.label)
                        .font(.strakkBody)
                        .foregroundStyle(chip.isSelected ? .white : Color.strakkTextSecondary)
                        .padding(.horizontal, StrakkSpacing.sm)
                        .frame(height: 44)
                        .background(chip.isSelected ? Color.strakkPrimary : Color.strakkSurface2)
                        .clipShape(Capsule())
                        .overlay(
                            Capsule()
                                .strokeBorder(
                                    chip.isSelected ? Color.strakkPrimary : Color.strakkDivider,
                                    lineWidth: 1
                                )
                        )
                }
                .buttonStyle(.plain)
                .accessibilityLabel(chip.label)
                .accessibilityAddTraits(chip.isSelected ? [.isSelected, .isButton] : .isButton)
            }
        }
    }
}
