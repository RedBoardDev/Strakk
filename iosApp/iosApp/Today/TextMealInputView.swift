import SwiftUI

struct TextMealInputView: View {
    let onSubmit: (String) -> Void
    let onCancel: () -> Void

    @State private var text: String = ""
    @FocusState private var isFocused: Bool

    private let maxLength = 2000

    private var canSubmit: Bool {
        !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                VStack(alignment: .leading, spacing: 0) {
                    // Description label
                    Text("Describe what you ate — be as specific as possible.")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextSecondary)
                        .padding(.horizontal, StrakkSpacing.lg)
                        .padding(.top, StrakkSpacing.md)
                        .padding(.bottom, StrakkSpacing.md)

                    // Text editor card
                    VStack(alignment: .trailing, spacing: 6) {
                        ZStack(alignment: .topLeading) {
                            if text.isEmpty {
                                Text("E.g. 200g grilled chicken, basmati rice, olive oil")
                                    .font(.strakkBody)
                                    .foregroundStyle(Color.strakkTextTertiary)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 14)
                                    .allowsHitTesting(false)
                            }

                            TextEditor(text: $text)
                                .font(.strakkBody)
                                .foregroundStyle(Color.strakkTextPrimary)
                                .tint(Color.strakkPrimary)
                                .scrollContentBackground(.hidden)
                                .focused($isFocused)
                                .frame(minHeight: 160, maxHeight: 280)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 8)
                                .onChange(of: text) { _, newValue in
                                    if newValue.count > maxLength {
                                        text = String(newValue.prefix(maxLength))
                                    }
                                }
                        }
                        .background(Color.strakkSurface1)
                        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
                        .overlay(
                            RoundedRectangle(cornerRadius: StrakkRadius.sm)
                                .strokeBorder(
                                    isFocused ? Color.strakkPrimary : Color.strakkDivider,
                                    lineWidth: 1.5
                                )
                                .animation(.easeInOut(duration: 0.15), value: isFocused)
                        )

                        // Character counter
                        Text("\(text.count) / \(maxLength)")
                            .font(.strakkCaption)
                            .foregroundStyle(
                                text.count > Int(Double(maxLength) * 0.9)
                                    ? Color.strakkWarning
                                    : Color.strakkTextTertiary
                            )
                            .monospacedDigit()
                    }
                    .padding(.horizontal, StrakkSpacing.lg)

                    Spacer()

                    StrakkPrimaryButton(
                        title: "Analyze",
                        action: {
                            let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
                            guard !trimmed.isEmpty else { return }
                            onSubmit(trimmed)
                        },
                        isEnabled: canSubmit
                    )
                    .accessibilityLabel(Text("Analyze described meal"))
                    .padding(.horizontal, StrakkSpacing.lg)
                    .padding(.bottom, StrakkSpacing.xxl)
                }
            }
            .navigationTitle("Describe your meal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                StrakkCloseToolbarItem(action: onCancel)
            }
        }
        .onAppear {
            isFocused = true
        }
    }
}
