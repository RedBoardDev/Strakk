import SwiftUI
import shared

struct WeightStepView: View {
    @Bindable var wrapper: OnboardingFlowViewModelWrapper

    // Local picker state; synced to KMP on change
    @State private var pickerKg: Int = 75

    var body: some View {
        VStack(spacing: 0) {
            stepHeader

            Spacer()

            weightPicker

            Spacer()

            continueButton
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.bottom, StrakkSpacing.xxl)
        }
        .onAppear {
            pickerKg = Int(wrapper.state.weightKg)
        }
        .onChange(of: pickerKg) { _, newValue in
            wrapper.send(OnboardingFlowEventOnWeightChanged(kg: Double(newValue)))
        }
    }

    private var stepHeader: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            OnboardingProgressBar(progress: wrapper.state.progressFraction)
                .padding(.horizontal, StrakkSpacing.xl)

            Text("Quel est ton poids actuel ?")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)
        }
    }

    private var weightPicker: some View {
        VStack(spacing: StrakkSpacing.xs) {
            Picker("Poids", selection: $pickerKg) {
                ForEach(30...250, id: \.self) { kg in
                    Text("\(kg) kg")
                        .font(.strakkBodyLarge)
                        .foregroundStyle(Color.strakkTextPrimary)
                        .tag(kg)
                }
            }
            .pickerStyle(.wheel)
            .frame(height: 200)
            .tint(Color.strakkPrimary)
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
        .accessibilityLabel("Continuer avec \(pickerKg) kg")
    }
}
