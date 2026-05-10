import SwiftUI
import shared

// MARK: - StepData

private struct StepData {
    let weekLabel: String
    let availableWeeks: [WeekOptionData]
    let coveredDates: Set<String>
    let weekDays: [DayOptionData]
    let existingCheckInId: String?
    let isEditMode: Bool
    let weight: String
    let shoulders: String
    let chest: String
    let armLeft: String
    let armRight: String
    let waist: String
    let hips: String
    let thighLeft: String
    let thighRight: String
    let delta: CheckInDeltaData?
    let selectedTags: Set<String>
    let mentalFeeling: String
    let physicalFeeling: String
    let photos: [WizardPhotoData]
    let nutritionSummary: NutritionSummaryData?
    let nutritionLoading: Bool
}

// MARK: - CheckInWizardView

struct CheckInWizardView: View {
    @State private var vm: CheckInWizardViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    init(checkInId: String?) {
        _vm = State(initialValue: CheckInWizardViewModelWrapper(checkInId: checkInId))
    }

    var body: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            switch vm.state {
            case .loading:
                ProgressView()
                    .tint(Color.strakkPrimary)

            case .ready(
                let isEditMode,
                let currentStep,
                let weekLabel,
                let availableWeeks,
                let coveredDates,
                let weekDays,
                let existingCheckInId,
                let weight,
                let shoulders,
                let chest,
                let armLeft,
                let armRight,
                let waist,
                let hips,
                let thighLeft,
                let thighRight,
                let delta,
                let selectedTags,
                let mentalFeeling,
                let physicalFeeling,
                let photos,
                let nutritionSummary,
                let nutritionLoading,
                let saving,
                let canGoNext
            ):
                VStack(spacing: 0) {
                    topBar(isEditMode: isEditMode, currentStep: currentStep)

                    stepContent(
                        currentStep: currentStep,
                        data: StepData(
                            weekLabel: weekLabel,
                            availableWeeks: availableWeeks,
                            coveredDates: coveredDates,
                            weekDays: weekDays,
                            existingCheckInId: existingCheckInId,
                            isEditMode: isEditMode,
                            weight: weight,
                            shoulders: shoulders,
                            chest: chest,
                            armLeft: armLeft,
                            armRight: armRight,
                            waist: waist,
                            hips: hips,
                            thighLeft: thighLeft,
                            thighRight: thighRight,
                            delta: delta,
                            selectedTags: selectedTags,
                            mentalFeeling: mentalFeeling,
                            physicalFeeling: physicalFeeling,
                            photos: photos,
                            nutritionSummary: nutritionSummary,
                            nutritionLoading: nutritionLoading
                        )
                    )

                    bottomBar(
                        currentStep: currentStep,
                        canGoNext: canGoNext,
                        saving: saving
                    )
                }
            }
        }
        .alert("Error", isPresented: Binding(
            get: { vm.errorMessage != nil },
            set: { if !$0 { vm.errorMessage = nil } }
        )) {
            Button("OK", role: .cancel) { vm.errorMessage = nil }
        } message: {
            Text(vm.errorMessage ?? "")
        }
        .onChange(of: vm.navigateBack) { _, navigateBack in
            if navigateBack { dismiss() }
        }
        .onChange(of: vm.navigateToDetailId) { _, detailId in
            if detailId != nil { dismiss() }
        }
    }

    // MARK: - Top bar

    @ViewBuilder
    private func topBar(isEditMode: Bool, currentStep: CheckInWizardStep) -> some View {
        HStack {
            Button {
                if currentStep == .dates {
                    dismiss()
                } else {
                    vm.onEvent(CheckInWizardEventOnBack())
                }
            } label: {
                HStack(spacing: StrakkSpacing.xxs) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 14, weight: .semibold))
                    Text(currentStep == .dates ? "Cancel" : "Back")
                        .font(.strakkBody)
                }
                .foregroundStyle(Color.strakkTextSecondary)
            }
            .accessibilityLabel(currentStep == .dates ? "Cancel" : "Previous step")

            Spacer()

            Text("Step \(stepIndex(currentStep))/5")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
        }
        .padding(.horizontal, StrakkSpacing.lg)
        .padding(.vertical, StrakkSpacing.sm)
        .background(Color.strakkBackground)
        .overlay(alignment: .bottom) {
            Divider()
                .background(Color.strakkDivider)
        }
    }

    // MARK: - Step content

    @ViewBuilder
    private func stepContent(currentStep: CheckInWizardStep, data: StepData) -> some View {
        Group {
            switch currentStep {
            case .dates:
                stepDates(data: data)
            case .measurements:
                stepMeasurements(data: data)
            case .feelings:
                stepFeelings(data: data)
            case .photos:
                stepPhotos(data: data)
            case .summary:
                stepSummary(data: data)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private func stepDates(data: StepData) -> some View {
        WizardStepDatesView(
            weekLabel: data.weekLabel,
            availableWeeks: data.availableWeeks,
            weekDays: data.weekDays,
            coveredDates: data.coveredDates,
            existingCheckInId: data.existingCheckInId,
            isEditMode: data.isEditMode,
            onSelectWeek: { vm.onEvent(CheckInWizardEventOnSelectWeek(weekLabel: $0)) },
            onToggleDate: { vm.onEvent(CheckInWizardEventOnToggleDate(date: $0)) }
        )
    }

    @ViewBuilder
    private func stepMeasurements(data: StepData) -> some View {
        WizardStepMeasurementsView(
            weight: data.weight,
            shoulders: data.shoulders,
            chest: data.chest,
            armLeft: data.armLeft,
            armRight: data.armRight,
            waist: data.waist,
            hips: data.hips,
            thighLeft: data.thighLeft,
            thighRight: data.thighRight,
            delta: data.delta,
            onWeightChanged: { vm.onEvent(CheckInWizardEventOnWeightChanged(value: $0)) },
            onShouldersChanged: { vm.onEvent(CheckInWizardEventOnShouldersChanged(value: $0)) },
            onChestChanged: { vm.onEvent(CheckInWizardEventOnChestChanged(value: $0)) },
            onArmLeftChanged: { vm.onEvent(CheckInWizardEventOnArmLeftChanged(value: $0)) },
            onArmRightChanged: { vm.onEvent(CheckInWizardEventOnArmRightChanged(value: $0)) },
            onWaistChanged: { vm.onEvent(CheckInWizardEventOnWaistChanged(value: $0)) },
            onHipsChanged: { vm.onEvent(CheckInWizardEventOnHipsChanged(value: $0)) },
            onThighLeftChanged: { vm.onEvent(CheckInWizardEventOnThighLeftChanged(value: $0)) },
            onThighRightChanged: { vm.onEvent(CheckInWizardEventOnThighRightChanged(value: $0)) }
        )
    }

    @ViewBuilder
    private func stepFeelings(data: StepData) -> some View {
        WizardStepFeelingsView(
            selectedTags: data.selectedTags,
            mentalFeeling: data.mentalFeeling,
            physicalFeeling: data.physicalFeeling,
            onToggleTag: { vm.onEvent(CheckInWizardEventOnToggleTag(slug: $0)) },
            onMentalFeelingChanged: { vm.onEvent(CheckInWizardEventOnMentalFeelingChanged(text: $0)) },
            onPhysicalFeelingChanged: { vm.onEvent(CheckInWizardEventOnPhysicalFeelingChanged(text: $0)) }
        )
    }

    @ViewBuilder
    private func stepPhotos(data: StepData) -> some View {
        WizardStepPhotosView(
            photos: data.photos,
            onAddPhoto: { photoData in vm.addPhoto(imageData: photoData) },
            onRemovePhoto: { vm.onEvent(CheckInWizardEventOnRemovePhoto(photoId: $0)) }
        )
    }

    @ViewBuilder
    private func stepSummary(data: StepData) -> some View {
        WizardStepSummaryView(
            nutritionSummary: data.nutritionSummary,
            nutritionLoading: data.nutritionLoading,
            weight: data.weight,
            delta: data.delta,
            photoCount: data.photos.count,
            selectedTags: data.selectedTags,
            mentalFeeling: data.mentalFeeling,
            physicalFeeling: data.physicalFeeling
        )
    }

    // MARK: - Bottom bar

    @ViewBuilder
    private func bottomBar(currentStep: CheckInWizardStep, canGoNext: Bool, saving: Bool) -> some View {
        VStack(spacing: 0) {
            Divider().background(Color.strakkDivider)

            HStack {
                if currentStep == .summary {
                    Button {
                        vm.onEvent(CheckInWizardEventOnSave())
                    } label: {
                        HStack(spacing: StrakkSpacing.xs) {
                            if saving {
                                ProgressView()
                                    .tint(.white)
                                    .scaleEffect(0.8)
                            }
                            Text(saving ? "Saving..." : "Save")
                                .font(.strakkBodyBold)
                                .foregroundStyle(.white)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(canGoNext ? Color.strakkPrimary : Color.strakkSurface2)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .disabled(!canGoNext || saving)
                    .accessibilityLabel("Save check-in")
                } else {
                    Button {
                        vm.onEvent(CheckInWizardEventOnNext())
                    } label: {
                        Text("Next")
                            .font(.strakkBodyBold)
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(canGoNext ? Color.strakkPrimary : Color.strakkSurface2)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .disabled(!canGoNext)
                    .accessibilityLabel("Next step")
                }
            }
            .padding(.horizontal, StrakkSpacing.lg)
            .padding(.vertical, StrakkSpacing.md)
        }
        .background(Color.strakkBackground)
    }

    // MARK: - Helpers

    private func stepIndex(_ step: CheckInWizardStep) -> Int {
        switch step {
        case .dates: return 1
        case .measurements: return 2
        case .feelings: return 3
        case .photos: return 4
        case .summary: return 5
        }
    }
}

// MARK: - Preview

#Preview {
    CheckInWizardView(checkInId: nil)
}
