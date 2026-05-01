import SwiftUI
import shared

struct BioStepView: View {
    @Bindable var wrapper: OnboardingFlowViewModelWrapper

    @State private var heightCm: Int = 175
    @State private var birthDate: Date = Date()
    @State private var showDatePicker: Bool = false

    private static let calendar = Calendar.current
    private static let minDate: Date = {
        var comps = DateComponents(); comps.year = 1920
        return calendar.date(from: comps) ?? Date()
    }()
    private static let maxDate: Date = {
        var comps = DateComponents()
        comps.year = Calendar.current.component(.year, from: Date()) - 5
        return calendar.date(from: comps) ?? Date()
    }()

    var body: some View {
        VStack(spacing: 0) {
            stepHeader

            ScrollView {
                VStack(spacing: StrakkSpacing.lg) {
                    heightSection
                    birthDateSection
                    sexSection
                }
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)
            }

            continueButton
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.bottom, StrakkSpacing.xxl)
        }
        .onAppear {
            heightCm = Int(wrapper.state.heightCm)
        }
        .onChange(of: heightCm) { _, newValue in
            wrapper.send(OnboardingFlowEventOnHeightChanged(cm: Int32(newValue)))
        }
    }

    private var stepHeader: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            OnboardingProgressBar(progress: wrapper.state.progressFraction)
                .padding(.horizontal, StrakkSpacing.xl)

            Text("Quelques infos sur toi")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.horizontal, StrakkSpacing.xl)
                .padding(.top, StrakkSpacing.xl)

            Text("Facultatif — pour personnaliser tes objectifs.")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .padding(.horizontal, StrakkSpacing.xl)
        }
    }

    private var heightSection: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("Taille")
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextSecondary)

            Picker("Taille", selection: $heightCm) {
                ForEach(100...230, id: \.self) { cm in
                    Text("\(cm) cm").tag(cm)
                }
            }
            .pickerStyle(.wheel)
            .frame(height: 140)
            .tint(Color.strakkPrimary)
        }
    }

    private var birthDateSection: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("Date de naissance")
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextSecondary)

            Button {
                showDatePicker.toggle()
            } label: {
                HStack {
                    Text(formattedDate)
                        .font(.strakkBody)
                        .foregroundStyle(
                            wrapper.state.birthDate != nil
                                ? Color.strakkTextPrimary
                                : Color.strakkTextTertiary
                        )
                    Spacer()
                    Image(systemName: "calendar")
                        .foregroundStyle(Color.strakkTextSecondary)
                }
                .padding(.horizontal, StrakkSpacing.sm)
                .padding(.vertical, StrakkSpacing.sm)
                .background(Color.strakkSurface1)
                .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
                .overlay(
                    RoundedRectangle(cornerRadius: StrakkRadius.sm)
                        .strokeBorder(Color.strakkDivider, lineWidth: 1)
                )
            }
            .accessibilityLabel("Choisir la date de naissance")

            if showDatePicker {
                DatePicker(
                    "",
                    selection: $birthDate,
                    in: Self.minDate...Self.maxDate,
                    displayedComponents: .date
                )
                .datePickerStyle(.wheel)
                .labelsHidden()
                .onChange(of: birthDate) { _, newDate in
                    sendBirthDate(newDate)
                }
            }
        }
    }

    private var sexSection: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("Sexe biologique")
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextSecondary)

            HStack(spacing: StrakkSpacing.xs) {
                sexPill(label: "Homme", value: BiologicalSex.male)
                sexPill(label: "Femme", value: BiologicalSex.female)
                sexPill(label: "Préfère ne pas dire", value: BiologicalSex.unspecified)
            }
        }
    }

    private func sexPill(label: String, value: BiologicalSex) -> some View {
        let isSelected = wrapper.state.biologicalSex == value
        return Button {
            let newValue: BiologicalSex? = isSelected ? nil : value
            wrapper.send(OnboardingFlowEventOnBiologicalSexChanged(sex: newValue))
        } label: {
            Text(label)
                .font(.strakkCaption)
                .foregroundStyle(isSelected ? .white : Color.strakkTextSecondary)
                .padding(.horizontal, StrakkSpacing.xs)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .background(isSelected ? Color.strakkPrimary : Color.strakkSurface2)
                .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : .isButton)
    }

    private var formattedDate: String {
        guard wrapper.state.birthDate != nil else { return "Sélectionner" }
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: birthDate)
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

    private func sendBirthDate(_ date: Date) {
        let comps = Self.calendar.dateComponents([.year, .month, .day], from: date)
        guard let year = comps.year, let month = comps.month, let day = comps.day else { return }
        let localDate = Kotlinx_datetimeLocalDate(year: Int32(year), monthNumber: Int32(month), dayOfMonth: Int32(day))
        wrapper.send(OnboardingFlowEventOnBirthDateChanged(date: localDate))
    }
}
