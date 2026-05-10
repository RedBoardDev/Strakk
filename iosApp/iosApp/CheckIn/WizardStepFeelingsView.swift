import SwiftUI

// MARK: - Feeling tag definitions

private struct FeelingTag: Identifiable {
    let id: String
    var label: String { FeelingTagLabels.label(for: id) }
}

private enum FeelingCategory {
    static let positive: [FeelingTag] = [
        FeelingTag(id: "energy_stable"),
        FeelingTag(id: "good_energy"),
        FeelingTag(id: "motivated"),
        FeelingTag(id: "disciplined"),
        FeelingTag(id: "good_sleep"),
        FeelingTag(id: "good_recovery"),
        FeelingTag(id: "strong_training"),
        FeelingTag(id: "good_mood"),
        FeelingTag(id: "focused"),
        FeelingTag(id: "light_body"),
        FeelingTag(id: "good_digestion")
    ]

    static let negative: [FeelingTag] = [
        FeelingTag(id: "low_energy"),
        FeelingTag(id: "tired"),
        FeelingTag(id: "poor_sleep"),
        FeelingTag(id: "stress"),
        FeelingTag(id: "low_motivation"),
        FeelingTag(id: "heavy_body"),
        FeelingTag(id: "sore"),
        FeelingTag(id: "joint_discomfort"),
        FeelingTag(id: "digestion_discomfort"),
        FeelingTag(id: "bloating"),
        FeelingTag(id: "hungry"),
        FeelingTag(id: "irritability"),
        FeelingTag(id: "low_mood")
    ]
}

// MARK: - WizardStepFeelingsView

struct WizardStepFeelingsView: View {
    let selectedTags: Set<String>
    let mentalFeeling: String
    let physicalFeeling: String
    let onToggleTag: (String) -> Void
    let onMentalFeelingChanged: (String) -> Void
    let onPhysicalFeelingChanged: (String) -> Void

    @State private var draftMentalFeeling: String
    @State private var draftPhysicalFeeling: String
    @FocusState private var focusedField: Field?

    private enum Field {
        case mental
        case physical
    }

    init(
        selectedTags: Set<String>,
        mentalFeeling: String,
        physicalFeeling: String,
        onToggleTag: @escaping (String) -> Void,
        onMentalFeelingChanged: @escaping (String) -> Void,
        onPhysicalFeelingChanged: @escaping (String) -> Void
    ) {
        self.selectedTags = selectedTags
        self.mentalFeeling = mentalFeeling
        self.physicalFeeling = physicalFeeling
        self.onToggleTag = onToggleTag
        self.onMentalFeelingChanged = onMentalFeelingChanged
        self.onPhysicalFeelingChanged = onPhysicalFeelingChanged
        _draftMentalFeeling = State(initialValue: mentalFeeling)
        _draftPhysicalFeeling = State(initialValue: physicalFeeling)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: StrakkSpacing.xl) {
                // Title
                Text("How are you feeling?")
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextPrimary)

                Text(
                    "Pick the feelings that summarize your week, " +
                    "then describe your mental and physical state separately."
                )
                .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextSecondary)

                // Positive tags
                tagSection(
                    title: "Positive feelings",
                    tags: FeelingCategory.positive,
                    selectedColor: Color.strakkSuccess
                )

                // Negative tags
                tagSection(
                    title: "Negative feelings",
                    tags: FeelingCategory.negative,
                    selectedColor: Color.strakkError
                )

                feelingTextSection(
                    title: "MENTAL FEELING",
                    placeholder: "E.g. motivation, stress, mood...",
                    text: $draftMentalFeeling,
                    focusedField: .mental,
                    onChanged: onMentalFeelingChanged
                )

                feelingTextSection(
                    title: "PHYSICAL FEELING",
                    placeholder: "E.g. energy, sleep, digestion...",
                    text: $draftPhysicalFeeling,
                    focusedField: .physical,
                    onChanged: onPhysicalFeelingChanged
                )

                Spacer().frame(height: StrakkSpacing.xl)
            }
            .padding(.horizontal, StrakkSpacing.lg)
            .padding(.vertical, StrakkSpacing.xl)
        }
        .onTapGesture {
            focusedField = nil
        }
    }

    // MARK: - Tag section

    @ViewBuilder
    private func tagSection(title: LocalizedStringKey, tags: [FeelingTag], selectedColor: Color) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text(title)
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .textCase(.uppercase)

            FlowLayout(spacing: StrakkSpacing.xs) {
                ForEach(tags) { tag in
                    tagChip(tag: tag, selectedColor: selectedColor)
                }
            }
        }
    }

    // MARK: - Tag chip

    private func tagChip(tag: FeelingTag, selectedColor: Color) -> some View {
        let isSelected = selectedTags.contains(tag.id)
        return Button {
            onToggleTag(tag.id)
        } label: {
            Text(tag.label)
                .font(.strakkCaptionBold)
                .foregroundStyle(isSelected ? .white : Color.strakkTextSecondary)
                .padding(.horizontal, StrakkSpacing.sm)
                .padding(.vertical, StrakkSpacing.xxs + 2)
                .background(isSelected ? selectedColor : Color.strakkSurface2)
                .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .accessibilityLabel("\(tag.label), \(isSelected ? "selected" : "not selected")")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    // MARK: - Free text sections

    private func feelingTextSection(
        title: LocalizedStringKey,
        placeholder: LocalizedStringKey,
        text: Binding<String>,
        focusedField field: Field,
        onChanged: @escaping (String) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text(title)
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            ZStack(alignment: .topLeading) {
                if text.wrappedValue.isEmpty {
                    Text(placeholder)
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextTertiary)
                        .padding(.horizontal, StrakkSpacing.sm)
                        .padding(.vertical, StrakkSpacing.sm)
                        .allowsHitTesting(false)
                }

                TextEditor(text: Binding(
                    get: { text.wrappedValue },
                    set: {
                        text.wrappedValue = $0
                        onChanged($0)
                    }
                ))
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextPrimary)
                .scrollContentBackground(.hidden)
                .focused($focusedField, equals: field)
                .frame(minHeight: 104)
                .padding(StrakkSpacing.xs)
            }
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))

            if !text.wrappedValue.isEmpty {
                Text("\(text.wrappedValue.count)/1000")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
        }
    }
}

// MARK: - Preview

#Preview {
    ZStack {
        Color.strakkBackground.ignoresSafeArea()
        WizardStepFeelingsView(
            selectedTags: ["good_energy", "good_sleep", "tired"],
            mentalFeeling: "Strong motivation, more stable week overall.",
            physicalFeeling: "Sleep was OK, digestion average.",
            onToggleTag: { _ in },
            onMentalFeelingChanged: { _ in },
            onPhysicalFeelingChanged: { _ in }
        )
    }
}
