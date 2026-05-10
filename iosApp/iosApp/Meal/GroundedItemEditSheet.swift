import SwiftUI

/// Sheet to edit macros of a grounded item (scan or manually added).
/// Includes a grams field in addition to the standard macro fields.
struct GroundedItemEditSheet: View {
    let item: GroundedItemData?
    let onSave: (_ name: String, _ kcal: Double, _ protein: Double, _ fat: Double, _ carbs: Double, _ grams: Double)
        -> Void
    let onCancel: () -> Void

    @State private var name: String
    @State private var kcal: String
    @State private var protein: String
    @State private var fat: String
    @State private var carbs: String
    @State private var grams: String

    @FocusState private var focusedField: Field?

    private enum Field: Hashable {
        case name, kcal, protein, fat, carbs, grams
    }

    init(
        item: GroundedItemData?,
        onSave: @escaping (String, Double, Double, Double, Double, Double) -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.item = item
        self.onSave = onSave
        self.onCancel = onCancel
        _name = State(initialValue: item?.name ?? "")
        _kcal = State(initialValue: item.map { String(format: "%.0f", $0.kcal) } ?? "")
        _protein = State(initialValue: item.map { String(format: "%.0f", $0.protein) } ?? "")
        _fat = State(initialValue: item.map { String(format: "%.0f", $0.fat) } ?? "")
        _carbs = State(initialValue: item.map { String(format: "%.0f", $0.carbs) } ?? "")
        _grams = State(initialValue: item.map { String(format: "%.0f", $0.grams) } ?? "100")
    }

    private var isSubmittable: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty
            && kcal.toDoubleOrNil() != nil
            && protein.toDoubleOrNil() != nil
            && grams.toDoubleOrNil().map { $0 > 0 } == true
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        StrakkFieldGroup(label: "Name *", required: true) {
                            StrakkInputField(
                                placeholder: "e.g. Grilled chicken",
                                text: $name,
                                isValid: name.count <= 100,
                                focusState: $focusedField,
                                focusValue: .name
                            )
                        }

                        HStack(spacing: 12) {
                            StrakkFieldGroup(label: "Calories *", required: true) {
                                StrakkNumericField(
                                    placeholder: "400",
                                    text: $kcal,
                                    isValid: kcal.isEmpty || kcal.toDoubleOrNil().map { $0 >= 0 && $0 <= 5000 } == true,
                                    focusState: $focusedField,
                                    focusValue: .kcal
                                )
                            }

                            StrakkFieldGroup(label: "Protein (g) *", required: true) {
                                StrakkNumericField(
                                    placeholder: "35",
                                    text: $protein,
                                    isValid: protein.isEmpty ||
                                        protein.toDoubleOrNil().map { $0 >= 0 && $0 <= 500 } == true,
                                    focusState: $focusedField,
                                    focusValue: .protein
                                )
                            }
                        }

                        HStack(spacing: 12) {
                            StrakkFieldGroup(label: "Fat (g)", required: false) {
                                StrakkNumericField(
                                    placeholder: "15",
                                    text: $fat,
                                    isValid: fat.isEmpty || fat.toDoubleOrNil().map { $0 >= 0 && $0 <= 500 } == true,
                                    focusState: $focusedField,
                                    focusValue: .fat
                                )
                            }

                            StrakkFieldGroup(label: "Carbs (g)", required: false) {
                                StrakkNumericField(
                                    placeholder: "40",
                                    text: $carbs,
                                    isValid: carbs.isEmpty ||
                                        carbs.toDoubleOrNil().map { $0 >= 0 && $0 <= 500 } == true,
                                    focusState: $focusedField,
                                    focusValue: .carbs
                                )
                            }
                        }

                        StrakkFieldGroup(label: "Grams *", required: true) {
                            StrakkNumericField(
                                placeholder: "150",
                                text: $grams,
                                isValid: grams.isEmpty || grams.toDoubleOrNil().map { $0 > 0 && $0 <= 5000 } == true,
                                focusState: $focusedField,
                                focusValue: .grams
                            )
                        }

                        StrakkPrimaryButton(
                            title: "Save",
                            action: {
                                guard let kcalVal = kcal.toDoubleOrNil(),
                                      let proteinVal = protein.toDoubleOrNil(),
                                      let gramsVal = grams.toDoubleOrNil() else { return }
                                let fatVal = fat.toDoubleOrNil() ?? 0.0
                                let carbsVal = carbs.toDoubleOrNil() ?? 0.0
                                onSave(name, kcalVal, proteinVal, fatVal, carbsVal, gramsVal)
                            },
                            isEnabled: isSubmittable
                        )
                        .accessibilityLabel(Text("Save changes"))
                    }
                    .padding(.horizontal, StrakkSpacing.lg)
                    .padding(.top, StrakkSpacing.md)
                    .padding(.bottom, StrakkSpacing.xxl)
                }
            }
            .navigationTitle(item == nil ? "Add item" : "Edit item")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                StrakkCloseToolbarItem(action: onCancel)
            }
            .onAppear {
                focusedField = .name
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }
}
