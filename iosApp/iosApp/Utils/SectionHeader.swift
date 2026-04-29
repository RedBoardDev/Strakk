import SwiftUI

struct SectionHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(.strakkOverline)
            .foregroundStyle(Color.strakkTextTertiary)
            .kerning(1.0)
    }
}
