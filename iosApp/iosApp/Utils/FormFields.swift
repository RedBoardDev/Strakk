import SwiftUI

// MARK: - StrakkFieldGroup

struct StrakkFieldGroup<Content: View>: View {
    let label: String
    let required: Bool
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 2) {
                Text(label)
                    .font(.strakkCaptionBold)
                    .foregroundStyle(Color.strakkTextSecondary)
                if required {
                    Text("*")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkPrimary)
                }
            }
            content()
        }
    }
}

// MARK: - StrakkInputField

struct StrakkInputField<F: Hashable>: View {
    let placeholder: String
    @Binding var text: String
    let isValid: Bool
    var focusState: FocusState<F?>.Binding
    let focusValue: F

    var body: some View {
        TextField(placeholder, text: $text)
            .font(.strakkBody)
            .foregroundStyle(Color.strakkTextPrimary)
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .strokeBorder(
                        text.isEmpty || isValid
                            ? Color.strakkDivider
                            : Color.strakkError,
                        lineWidth: 1
                    )
            )
            .focused(focusState, equals: focusValue)
            .submitLabel(.next)
            .tint(Color.strakkPrimary)
    }
}

// MARK: - StrakkNumericField

struct StrakkNumericField<F: Hashable>: View {
    let placeholder: String
    @Binding var text: String
    let isValid: Bool
    var focusState: FocusState<F?>.Binding
    let focusValue: F

    var body: some View {
        StrakkInputField(
            placeholder: placeholder,
            text: $text,
            isValid: isValid,
            focusState: focusState,
            focusValue: focusValue
        )
        .keyboardType(.decimalPad)
    }
}

// MARK: - String → Double helper

extension String {
    func toDoubleOrNil() -> Double? {
        Double(self.replacingOccurrences(of: ",", with: "."))
    }
}
