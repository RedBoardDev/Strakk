import SwiftUI

struct ProBadge: View {
    var body: some View {
        Text("PRO")
            .font(.strakkOverline)
            .foregroundStyle(Color.strakkPrimary)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(Color.strakkPrimary.opacity(0.15), in: RoundedRectangle(cornerRadius: 4))
    }
}
