import SwiftUI

// MARK: - Feeling tag definitions

private struct FeelingTag: Identifiable {
    let id: String
    let label: String
}

private enum FeelingCategory {
    static let positive: [FeelingTag] = [
        FeelingTag(id: "energy_stable", label: "Énergie stable"),
        FeelingTag(id: "good_energy", label: "Bonne énergie"),
        FeelingTag(id: "motivated", label: "Motivation"),
        FeelingTag(id: "disciplined", label: "Régularité"),
        FeelingTag(id: "good_sleep", label: "Bien dormi"),
        FeelingTag(id: "good_recovery", label: "Bonne récup"),
        FeelingTag(id: "strong_training", label: "Séances solides"),
        FeelingTag(id: "good_mood", label: "Bonne humeur"),
        FeelingTag(id: "focused", label: "Mental clair"),
        FeelingTag(id: "light_body", label: "Corps léger"),
        FeelingTag(id: "good_digestion", label: "Bonne digestion")
    ]

    static let negative: [FeelingTag] = [
        FeelingTag(id: "low_energy", label: "Peu d'énergie"),
        FeelingTag(id: "tired", label: "Fatigue"),
        FeelingTag(id: "poor_sleep", label: "Mal dormi"),
        FeelingTag(id: "stress", label: "Stress"),
        FeelingTag(id: "low_motivation", label: "Motivation basse"),
        FeelingTag(id: "heavy_body", label: "Corps lourd"),
        FeelingTag(id: "sore", label: "Courbatures"),
        FeelingTag(id: "joint_discomfort", label: "Gêne articulaire"),
        FeelingTag(id: "digestion_discomfort", label: "Digestion difficile"),
        FeelingTag(id: "bloating", label: "Ballonnements"),
        FeelingTag(id: "hungry", label: "Faim marquée"),
        FeelingTag(id: "irritability", label: "Irritabilité"),
        FeelingTag(id: "low_mood", label: "Moral bas")
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

                Text("Pick the feelings that summarize your week, then describe your mental and physical state separately.")
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
    private func tagSection(title: String, tags: [FeelingTag], selectedColor: Color) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text(title.uppercased())
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

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
        title: String,
        placeholder: String,
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
            mentalFeeling: "Bonne motivation, semaine plus stable.",
            physicalFeeling: "Sommeil correct mais digestion moyenne.",
            onToggleTag: { _ in },
            onMentalFeelingChanged: { _ in },
            onPhysicalFeelingChanged: { _ in }
        )
    }
}
