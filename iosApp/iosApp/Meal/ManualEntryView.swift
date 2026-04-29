import SwiftUI
import shared

struct ManualEntryView: View {
    let draftViewModel: MealDraftViewModelWrapper
    let isDraftMode: Bool
    let logDate: String?
    let onDismiss: () -> Void

    init(
        draftViewModel: MealDraftViewModelWrapper,
        isDraftMode: Bool,
        logDate: String? = nil,
        onDismiss: @escaping () -> Void
    ) {
        self.draftViewModel = draftViewModel
        self.isDraftMode = isDraftMode
        self.logDate = logDate
        self.onDismiss = onDismiss
    }

    @State private var formViewModel = ManualEntryViewModelWrapper()

    // Local bindings that dispatch events on change
    @State private var name = ""
    @State private var protein = ""
    @State private var calories = ""
    @State private var fat = ""
    @State private var carbs = ""
    @State private var quantity = ""

    @FocusState private var focusedField: Field?

    private enum Field: Hashable {
        case name, protein, calories, fat, carbs, quantity
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        fieldGroup(label: "Nom *", required: true) {
                            inputField(
                                placeholder: "ex : Poulet grillé",
                                text: $name,
                                field: .name,
                                isValid: formViewModel.formData.name.count <= 100
                            )
                            .onChange(of: name) { _, v in
                                formViewModel.onEvent(ManualEntryEventNameChanged(value: v))
                            }
                        }

                        HStack(spacing: 12) {
                            fieldGroup(label: "Protéines (g) *", required: true) {
                                numericField(
                                    placeholder: "35",
                                    text: $protein,
                                    field: .protein,
                                    isValid: protein.isEmpty || protein.toDoubleOrNil().map { $0 >= 0 && $0 <= 500 } == true
                                )
                                .onChange(of: protein) { _, v in
                                    formViewModel.onEvent(ManualEntryEventProteinChanged(value: v))
                                }
                            }

                            fieldGroup(label: "Calories *", required: true) {
                                numericField(
                                    placeholder: "400",
                                    text: $calories,
                                    field: .calories,
                                    isValid: calories.isEmpty || calories.toDoubleOrNil().map { $0 >= 0 && $0 <= 5000 } == true
                                )
                                .onChange(of: calories) { _, v in
                                    formViewModel.onEvent(ManualEntryEventCaloriesChanged(value: v))
                                }
                            }
                        }

                        HStack(spacing: 12) {
                            fieldGroup(label: "Lipides (g)", required: false) {
                                numericField(
                                    placeholder: "15",
                                    text: $fat,
                                    field: .fat,
                                    isValid: fat.isEmpty || fat.toDoubleOrNil().map { $0 >= 0 && $0 <= 500 } == true
                                )
                                .onChange(of: fat) { _, v in
                                    formViewModel.onEvent(ManualEntryEventFatChanged(value: v))
                                }
                            }

                            fieldGroup(label: "Glucides (g)", required: false) {
                                numericField(
                                    placeholder: "40",
                                    text: $carbs,
                                    field: .carbs,
                                    isValid: carbs.isEmpty || carbs.toDoubleOrNil().map { $0 >= 0 && $0 <= 500 } == true
                                )
                                .onChange(of: carbs) { _, v in
                                    formViewModel.onEvent(ManualEntryEventCarbsChanged(value: v))
                                }
                            }
                        }

                        fieldGroup(label: "Quantité", required: false) {
                            inputField(
                                placeholder: "ex : 150g, 1 bol",
                                text: $quantity,
                                field: .quantity,
                                isValid: quantity.count <= 50
                            )
                            .onChange(of: quantity) { _, v in
                                formViewModel.onEvent(ManualEntryEventQuantityChanged(value: v))
                            }
                        }

                        if let errorMessage = formViewModel.formData.errorMessage {
                            Text(errorMessage)
                                .font(.strakkCaption)
                                .foregroundStyle(Color.strakkError)
                        }

                        // Submit button
                        Button {
                            if isDraftMode {
                                submitToDraft()
                            } else {
                                formViewModel.onEvent(ManualEntryEventSubmit(logDate: logDate))
                            }
                        } label: {
                            HStack {
                                if formViewModel.formData.isSubmitting {
                                    ProgressView()
                                        .tint(.white)
                                        .scaleEffect(0.8)
                                } else {
                                    Text("Ajouter")
                                        .font(.strakkBodyBold)
                                        .foregroundStyle(.white)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(
                                formViewModel.formData.isSubmittable
                                    ? Color.strakkPrimary
                                    : Color.strakkSurface2
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .disabled(!formViewModel.formData.isSubmittable || formViewModel.formData.isSubmitting)
                        .accessibilityLabel("Ajouter l'aliment")
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .padding(.bottom, 32)
                }
            }
            .navigationTitle("Ajout manuel")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        formViewModel.onEvent(ManualEntryEventCancel.shared)
                    } label: {
                        Image(systemName: "xmark")
                            .foregroundStyle(Color.strakkTextSecondary)
                    }
                    .accessibilityLabel("Annuler")
                }
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .onChange(of: formViewModel.shouldDismiss) { _, should in
            if should { onDismiss() }
        }
    }

    // MARK: - Draft mode submit (bypasses QuickAddManualUseCase)

    private func submitToDraft() {
        let d = formViewModel.formData
        let protein = Double(d.protein.replacingOccurrences(of: ",", with: ".")) ?? 0
        let calories = Double(d.calories.replacingOccurrences(of: ",", with: ".")) ?? 0
        let fat = Double(d.fat.replacingOccurrences(of: ",", with: "."))
        let carbs = Double(d.carbs.replacingOccurrences(of: ",", with: "."))
        let qty: String? = d.quantity.isEmpty ? nil : d.quantity

        draftViewModel.onEvent(MealDraftEventAddManualItem(
            name: d.name,
            protein: protein,
            calories: calories,
            fat: asKotlinDouble(fat),
            carbs: asKotlinDouble(carbs),
            quantity: qty,
            source: EntrySource.manual
        ))
        onDismiss()
    }

    // MARK: - Field builders

    private func fieldGroup<Content: View>(
        label: String,
        required: Bool,
        @ViewBuilder content: () -> Content
    ) -> some View {
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

    private func inputField(
        placeholder: String,
        text: Binding<String>,
        field: Field,
        isValid: Bool
    ) -> some View {
        TextField(placeholder, text: text)
            .font(.strakkBody)
            .foregroundStyle(Color.strakkTextPrimary)
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .strokeBorder(
                        text.wrappedValue.isEmpty || isValid
                            ? Color.strakkDivider
                            : Color.strakkError,
                        lineWidth: 1
                    )
            )
            .focused($focusedField, equals: field)
            .submitLabel(.next)
    }

    private func numericField(
        placeholder: String,
        text: Binding<String>,
        field: Field,
        isValid: Bool
    ) -> some View {
        inputField(placeholder: placeholder, text: text, field: field, isValid: isValid)
            .keyboardType(.decimalPad)
    }
}
