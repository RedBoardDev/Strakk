import SwiftUI

struct TextEntryView: View {
    let onAdd: (String) -> Void
    let onCancel: () -> Void

    @State private var text: String = ""
    @FocusState private var isFocused: Bool

    private var isValid: Bool { text.count >= 3 && text.count <= 300 }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                VStack(alignment: .leading, spacing: 12) {
                    Text("Describe what you ate")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextSecondary)
                        .padding(.top, 16)

                    ZStack(alignment: .topLeading) {
                        if text.isEmpty {
                            Text("E.g. 200g grilled chicken, basmati rice...")
                                .font(.strakkBody)
                                .foregroundStyle(Color.strakkTextTertiary)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 14)
                                .allowsHitTesting(false)
                        }
                        TextEditor(text: $text)
                            .font(.strakkBody)
                            .foregroundStyle(Color.strakkTextPrimary)
                            .scrollContentBackground(.hidden)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 10)
                            .frame(minHeight: 140)
                            .focused($isFocused)
                            .onChange(of: text) { _, v in
                                if v.count > 300 {
                                    text = String(v.prefix(300))
                                }
                            }
                            .accessibilityLabel("Meal description")
                    }
                    .background(Color.strakkSurface1)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .strokeBorder(
                                isFocused ? Color.strakkPrimary : (text.count > 300 ? Color.strakkError : Color.strakkDivider),
                                lineWidth: 1.5
                            )
                            .animation(.easeInOut(duration: 0.15), value: isFocused)
                    )

                    HStack {
                        if text.count > 300 {
                            Text("Maximum 300 characters")
                                .font(.strakkCaption)
                                .foregroundStyle(Color.strakkError)
                        } else if text.count < 3 && !text.isEmpty {
                            Text("Minimum 3 characters")
                                .font(.strakkCaption)
                                .foregroundStyle(Color.strakkError)
                        }
                        Spacer()
                        Text("\(text.count)/300")
                            .font(.strakkCaption)
                            .foregroundStyle(
                                text.count > 280 ? Color.strakkError : Color.strakkTextTertiary
                            )
                    }

                    Button {
                        onAdd(text.trimmingCharacters(in: .whitespacesAndNewlines))
                    } label: {
                        Text("Add to meal")
                            .font(.strakkBodyBold)
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(isValid ? Color.strakkPrimary : Color.strakkSurface2)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .disabled(!isValid)
                    .accessibilityLabel("Add text to meal")

                    Spacer()
                }
                .padding(.horizontal, 20)
            }
            .navigationTitle("Add text entry")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onCancel() }
                        .foregroundStyle(Color.strakkTextSecondary)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
        .onAppear { isFocused = true }
    }
}
