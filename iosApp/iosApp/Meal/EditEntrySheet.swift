import SwiftUI
import shared

struct EditEntrySheet: View {
    let entry: MealEntryData
    let onSave: (_ name: String, _ protein: Double, _ calories: Double, _ fat: Double?, _ carbs: Double?, _ quantity: String?) -> Void
    let onCancel: () -> Void

    @State private var name: String
    @State private var protein: String
    @State private var calories: String
    @State private var fat: String
    @State private var carbs: String
    @State private var quantity: String

    @FocusState private var focusedField: Field?

    private enum Field: Hashable {
        case name, protein, calories, fat, carbs, quantity
    }

    init(
        entry: MealEntryData,
        onSave: @escaping (String, Double, Double, Double?, Double?, String?) -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.entry = entry
        self.onSave = onSave
        self.onCancel = onCancel
        _name = State(initialValue: entry.name ?? "")
        _protein = State(initialValue: entry.protein > 0 ? String(format: "%.0f", entry.protein) : "")
        _calories = State(initialValue: entry.calories > 0 ? String(format: "%.0f", entry.calories) : "")
        _fat = State(initialValue: entry.fat.map { String(format: "%.0f", $0) } ?? "")
        _carbs = State(initialValue: entry.carbs.map { String(format: "%.0f", $0) } ?? "")
        _quantity = State(initialValue: entry.quantity ?? "")
    }

    private var isSubmittable: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty
            && protein.toDoubleOrNil() != nil
            && calories.toDoubleOrNil() != nil
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        StrakkFieldGroup(label: "Nom *", required: true) {
                            StrakkInputField(
                                placeholder: "ex : Poulet grillé",
                                text: $name,
                                isValid: name.count <= 100,
                                focusState: $focusedField,
                                focusValue: .name
                            )
                        }

                        HStack(spacing: 12) {
                            StrakkFieldGroup(label: "Protéines (g) *", required: true) {
                                StrakkNumericField(
                                    placeholder: "35",
                                    text: $protein,
                                    isValid: protein.isEmpty || protein.toDoubleOrNil().map { $0 >= 0 && $0 <= 500 } == true,
                                    focusState: $focusedField,
                                    focusValue: .protein
                                )
                            }

                            StrakkFieldGroup(label: "Calories *", required: true) {
                                StrakkNumericField(
                                    placeholder: "400",
                                    text: $calories,
                                    isValid: calories.isEmpty || calories.toDoubleOrNil().map { $0 >= 0 && $0 <= 5000 } == true,
                                    focusState: $focusedField,
                                    focusValue: .calories
                                )
                            }
                        }

                        HStack(spacing: 12) {
                            StrakkFieldGroup(label: "Lipides (g)", required: false) {
                                StrakkNumericField(
                                    placeholder: "15",
                                    text: $fat,
                                    isValid: fat.isEmpty || fat.toDoubleOrNil().map { $0 >= 0 && $0 <= 500 } == true,
                                    focusState: $focusedField,
                                    focusValue: .fat
                                )
                            }

                            StrakkFieldGroup(label: "Glucides (g)", required: false) {
                                StrakkNumericField(
                                    placeholder: "40",
                                    text: $carbs,
                                    isValid: carbs.isEmpty || carbs.toDoubleOrNil().map { $0 >= 0 && $0 <= 500 } == true,
                                    focusState: $focusedField,
                                    focusValue: .carbs
                                )
                            }
                        }

                        StrakkFieldGroup(label: "Quantité", required: false) {
                            StrakkInputField(
                                placeholder: "ex : 150g, 1 bol",
                                text: $quantity,
                                isValid: quantity.count <= 50,
                                focusState: $focusedField,
                                focusValue: .quantity
                            )
                        }

                        Button {
                            guard let proteinVal = protein.toDoubleOrNil(),
                                  let caloriesVal = calories.toDoubleOrNil() else { return }
                            let fatVal = fat.toDoubleOrNil()
                            let carbsVal = carbs.toDoubleOrNil()
                            let quantityVal = quantity.trimmingCharacters(in: .whitespaces).isEmpty ? nil : quantity
                            onSave(name, proteinVal, caloriesVal, fatVal, carbsVal, quantityVal)
                        } label: {
                            Text("Enregistrer")
                                .font(.strakkBodyBold)
                                .foregroundStyle(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 52)
                                .background(isSubmittable ? Color.strakkPrimary : Color.strakkSurface2)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .disabled(!isSubmittable)
                        .accessibilityLabel("Enregistrer les modifications")
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .padding(.bottom, 32)
                }
            }
            .navigationTitle("Modifier l'entrée")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        onCancel()
                    } label: {
                        Image(systemName: "xmark")
                            .foregroundStyle(Color.strakkTextSecondary)
                    }
                    .accessibilityLabel("Annuler")
                }
            }
            .onAppear {
                focusedField = .name
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }
}
