import SwiftUI
import shared

struct ActivityTrainingStepView: View {
    @Bindable var wrapper: OnboardingFlowViewModelWrapper

    private struct TypeOption {
        let type: TrainingType
        let label: LocalizedStringKey
    }

    private let typeOptions: [TypeOption] = [
        .init(type: .strength, label: "Strength"),
        .init(type: .cardio, label: "Cardio"),
        .init(type: .teamSport, label: "Team sport"),
        .init(type: .yogaFlexibility, label: "Yoga / Mobility"),
        .init(type: .other, label: "Other")
    ]

    var body: some View {
        VStack(spacing: 0) {
            stepHeader

            ScrollView {
                VStack(alignment: .leading, spacing: StrakkSpacing.xl) {
                    frequencySection
                    typeSection
                }
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)
            }

            StrakkPrimaryButton(
                title: "Continue",
                action: { wrapper.send(OnboardingFlowEventOnContinue()) }
            )
            .padding(.horizontal, StrakkSpacing.xl)
            .padding(.bottom, StrakkSpacing.xxl)
        }
    }

    private var stepHeader: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            OnboardingProgressBar(progress: wrapper.state.progressFraction)
                .padding(.horizontal, StrakkSpacing.xl)

            Text("Tell me about your training")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)
        }
    }

    private var frequencySection: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            Text("Sessions per week")
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextSecondary)

            PillSelector(
                values: Array(0...7),
                selected: wrapper.state.trainingFrequency?.intValue,
                onSelect: { value in
                    wrapper.send(OnboardingFlowEventOnTrainingFrequencyChanged(frequency: KotlinInt(int: Int32(value))))
                }
            )
        }
    }

    private var typeSection: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            Text("Training types")
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextSecondary)

            ChipGrid(
                chips: typeOptions.map { option in
                    ChipItem(
                        id: option.type.name,
                        label: option.label,
                        isSelected: wrapper.state.trainingTypes.contains(option.type)
                    )
                },
                onToggle: { id in
                    if let option = typeOptions.first(where: { $0.type.name == id }) {
                        wrapper.send(OnboardingFlowEventOnTrainingTypeToggled(type: option.type))
                    }
                }
            )
        }
    }
}
