import SwiftUI

// MARK: - StrakkSheetHeader — DESIGN.md §5 "Sheets".
//
// Some sheets need a custom header *inside* the body (e.g. when there is a
// big illustration or hero block above the title). For those cases use this
// view; otherwise rely on `.strakkSheet(title:onClose:)`.

struct StrakkSheetHeader: View {
    let title: LocalizedStringKey
    var subtitle: LocalizedStringKey?

    var body: some View {
        VStack(spacing: StrakkSpacing.xxs) {
            Text(title)
                .font(.strakkHeading2)
                .foregroundStyle(Color.strakkTextPrimary)
                .multilineTextAlignment(.center)
            if let subtitle {
                Text(subtitle)
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, StrakkSpacing.lg)
    }
}

#Preview {
    StrakkSheetHeader(title: "Add a meal", subtitle: "Pick how you want to log it.")
        .padding()
        .background(Color.strakkBackground)
}
