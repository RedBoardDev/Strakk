import SwiftUI

struct PlaceholderTabView: View {
    let title: String

    var body: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            VStack(spacing: 8) {
                Text(title)
                    .font(.strakkHeading1)
                    .foregroundStyle(Color.strakkTextPrimary)

                Text("Coming soon")
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextSecondary)
            }
        }
    }
}
