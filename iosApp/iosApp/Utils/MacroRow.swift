import SwiftUI

struct MacroRow: View {
    let label: String
    let value: String
    let color: Color

    var body: some View {
        HStack {
            Text(label)
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextPrimary)
            Spacer()
            Text(value)
                .font(.strakkBodyBold)
                .foregroundStyle(color)
                .monospacedDigit()
        }
        .padding(.horizontal, StrakkSpacing.md)
        .padding(.vertical, StrakkSpacing.sm)
    }
}
