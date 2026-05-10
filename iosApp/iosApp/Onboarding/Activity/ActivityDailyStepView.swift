import SwiftUI
import shared

struct ActivityDailyStepView: View {
    @Bindable var wrapper: OnboardingFlowViewModelWrapper

    private struct IntensityOption {
        let intensity: TrainingIntensity
        let icon: String
        let title: LocalizedStringKey
        let subtitle: LocalizedStringKey
    }

    private struct DailyOption {
        let level: DailyActivityLevel
        let icon: String
        let title: LocalizedStringKey
        let subtitle: LocalizedStringKey
    }

    private let intensityOptions: [IntensityOption] = [
        .init(
            intensity: .light,
            icon: "figure.walk",
            title: "Light",
            subtitle: "Low effort, mild sweat"
        ),
        .init(
            intensity: .moderate,
            icon: "figure.run",
            title: "Moderate",
            subtitle: "Sustained effort, breathing harder"
        ),
        .init(
            intensity: .intense,
            icon: "bolt.fill",
            title: "Intense",
            subtitle: "Maximum effort, exhausting"
        )
    ]

    private let dailyOptions: [DailyOption] = [
        .init(
            level: .sedentary,
            icon: "chair.lounge.fill",
            title: "Sedentary",
            subtitle: "Sitting most of the day"
        ),
        .init(
            level: .moderatelyActive,
            icon: "figure.walk.motion",
            title: "Moderately active",
            subtitle: "On your feet several times a day"
        ),
        .init(
            level: .veryActive,
            icon: "figure.hiking",
            title: "Very active",
            subtitle: "On your feet and moving all day"
        )
    ]

    var body: some View {
        VStack(spacing: 0) {
            stepHeader

            ScrollView {
                VStack(alignment: .leading, spacing: StrakkSpacing.xl) {
                    intensitySection
                    dailySection
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

            Text("And in everyday life?")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)
        }
    }

    private var intensitySection: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            Text("Training intensity")
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextSecondary)

            ForEach(0..<intensityOptions.count, id: \.self) { index in
                let option = intensityOptions[index]
                SelectableCard(
                    icon: option.icon,
                    title: option.title,
                    subtitle: option.subtitle,
                    isSelected: wrapper.state.trainingIntensity == option.intensity
                ) {
                    let same = wrapper.state.trainingIntensity == option.intensity
                    let newValue: TrainingIntensity? = same ? nil : option.intensity
                    wrapper.send(OnboardingFlowEventOnTrainingIntensityChanged(intensity: newValue))
                }
            }
        }
    }

    private var dailySection: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            Text("Daily activity")
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextSecondary)

            ForEach(0..<dailyOptions.count, id: \.self) { index in
                let option = dailyOptions[index]
                SelectableCard(
                    icon: option.icon,
                    title: option.title,
                    subtitle: option.subtitle,
                    isSelected: wrapper.state.dailyActivityLevel == option.level
                ) {
                    let same = wrapper.state.dailyActivityLevel == option.level
                    let newValue: DailyActivityLevel? = same ? nil : option.level
                    wrapper.send(OnboardingFlowEventOnDailyActivityChanged(level: newValue))
                }
            }
        }
    }
}
