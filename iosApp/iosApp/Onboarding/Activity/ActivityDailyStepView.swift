import SwiftUI
import shared

struct ActivityDailyStepView: View {
    @Bindable var wrapper: OnboardingFlowViewModelWrapper

    private struct IntensityOption {
        let intensity: TrainingIntensity
        let icon: String
        let title: String
        let subtitle: String
    }

    private struct DailyOption {
        let level: DailyActivityLevel
        let icon: String
        let title: String
        let subtitle: String
    }

    private let intensityOptions: [IntensityOption] = [
        .init(
            intensity: .light,
            icon: "figure.walk",
            title: "Légère",
            subtitle: "Effort faible, transpiration modérée"
        ),
        .init(
            intensity: .moderate,
            icon: "figure.run",
            title: "Modérée",
            subtitle: "Effort soutenu, essoufflement"
        ),
        .init(
            intensity: .intense,
            icon: "bolt.fill",
            title: "Intense",
            subtitle: "Effort maximal, épuisant"
        )
    ]

    private let dailyOptions: [DailyOption] = [
        .init(
            level: .sedentary,
            icon: "chair.lounge.fill",
            title: "Sédentaire",
            subtitle: "Assis la plupart du temps"
        ),
        .init(
            level: .moderatelyActive,
            icon: "figure.walk.motion",
            title: "Modérément actif",
            subtitle: "Quelques déplacements par jour"
        ),
        .init(
            level: .veryActive,
            icon: "figure.hiking",
            title: "Très actif",
            subtitle: "Debout et en mouvement toute la journée"
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

            continueButton
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.bottom, StrakkSpacing.xxl)
        }
    }

    private var stepHeader: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            OnboardingProgressBar(progress: wrapper.state.progressFraction)
                .padding(.horizontal, StrakkSpacing.xl)

            Text("Et au quotidien ?")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)
        }
    }

    private var intensitySection: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            Text("Intensité de tes séances")
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextSecondary)

            ForEach(intensityOptions, id: \.title) { option in
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
            Text("Activité hors entraînement")
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextSecondary)

            ForEach(dailyOptions, id: \.title) { option in
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

    private var continueButton: some View {
        Button {
            wrapper.send(OnboardingFlowEventOnContinue())
        } label: {
            Text("Continuer")
                .font(.strakkBodyBold)
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(Color.strakkPrimary)
                .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        }
    }
}
