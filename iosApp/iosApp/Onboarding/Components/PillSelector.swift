import SwiftUI

struct PillSelector: View {
    let values: [Int]
    let selected: Int?
    let onSelect: (Int) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: StrakkSpacing.xs) {
                ForEach(values, id: \.self) { value in
                    pillButton(value: value)
                }
            }
            .padding(.horizontal, 1)
        }
    }

    private func pillButton(value: Int) -> some View {
        let isSelected = selected == value
        return Button {
            onSelect(value)
        } label: {
            Text("\(value)")
                .font(.strakkBodyBold)
                .foregroundStyle(isSelected ? .white : Color.strakkTextSecondary)
                .frame(minWidth: 44, minHeight: 44)
                .padding(.horizontal, StrakkSpacing.sm)
                .background(isSelected ? Color.strakkPrimary : Color.strakkSurface2)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(value) fois")
        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
    }
}
