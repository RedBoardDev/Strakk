import SwiftUI
import shared

private struct EditTarget: Identifiable {
    let id: Int // index in items array
    let item: GroundedItemData
}

struct MealReviewV3View: View {
    let data: ReviewingData
    @Bindable var viewModel: PhotoMealViewModelWrapper

    @State private var showSwapSheet: Bool = false
    @State private var swapTargetIndex: Int?
    @State private var editTarget: EditTarget?
    @State private var showAddPicker: Bool = false
    @State private var showAddSearch: Bool = false
    @State private var showAddManual: Bool = false
    @State private var searchWrapper = SearchFoodViewModelWrapper()

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.strakkBackground.ignoresSafeArea()

            VStack(spacing: 0) {
                totalsHeader
                itemsList
            }

            saveFooter
        }
        .navigationTitle(data.mealName)
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showSwapSheet) {
            FoodSwapSheet(
                searchVm: searchWrapper,
                onSelect: { kmpItem in
                    if let idx = swapTargetIndex {
                        viewModel.onEvent(PhotoMealEventSwapFood(
                            itemIndex: Int32(idx),
                            newMatch: kmpItem
                        ))
                    }
                    showSwapSheet = false
                },
                onCancel: { showSwapSheet = false }
            )
        }
        .sheet(item: $editTarget) { target in
            GroundedItemEditSheet(
                item: target.item,
                onSave: { name, kcal, protein, fat, carbs, grams in
                    viewModel.onEvent(PhotoMealEventEditItemMacros(
                        itemIndex: Int32(target.id),
                        name: name,
                        kcal: kcal,
                        protein: protein,
                        fat: fat,
                        carbs: carbs,
                        grams: grams
                    ))
                    editTarget = nil
                },
                onCancel: { editTarget = nil }
            )
        }
        .sheet(isPresented: $showAddPicker) {
            ReviewAddPickerSheet(
                onSearch: { showAddPicker = false; showAddSearch = true },
                onManual: { showAddPicker = false; showAddManual = true },
                onDismiss: { showAddPicker = false }
            )
        }
        .sheet(isPresented: $showAddSearch) {
            ReviewSearchSheet(
                searchVm: searchWrapper,
                onSelect: { kmpItem in
                    viewModel.onEvent(PhotoMealEventAddSearchItem(
                        match: kmpItem,
                        grams: kmpItem.defaultPortionGrams
                    ))
                    showAddSearch = false
                },
                onCancel: { showAddSearch = false }
            )
        }
        .sheet(isPresented: $showAddManual) {
            GroundedItemEditSheet(
                item: nil,
                onSave: { name, kcal, protein, fat, carbs, grams in
                    viewModel.onEvent(PhotoMealEventAddManualItem(
                        name: name,
                        kcal: kcal,
                        protein: protein,
                        fat: fat,
                        carbs: carbs,
                        grams: grams
                    ))
                    showAddManual = false
                },
                onCancel: { showAddManual = false }
            )
        }
    }

    // MARK: - Totals header

    private var totalsHeader: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
            HStack(alignment: .firstTextBaseline) {
                Text("MEAL TOTAL")
                    .font(.strakkOverline)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .kerning(1.0)
                Spacer()
                let visibleCount = data.items.count - data.hiddenCount
                Text("\(data.groundedCount)/\(visibleCount) matched")
                    .font(.strakkCaption)
                    .foregroundStyle(
                        data.groundedCount == visibleCount && visibleCount > 0
                            ? Color.strakkPrimary
                            : Color.strakkTextTertiary
                    )
            }

            Text(String(format: "%.0f kcal", data.totalKcal))
                .font(.strakkDisplay)
                .foregroundStyle(Color.strakkTextPrimary)
                .monospacedDigit()

            HStack(spacing: 20) {
                compactMacro(label: "Protein", value: data.totalProtein, unit: "g", color: Color.strakkProtein)
                compactMacro(label: "Carbs", value: data.totalCarbs, unit: "g", color: Color.strakkAccentIndigo)
            }
            HStack(spacing: 20) {
                compactMacro(label: "Fat", value: data.totalFat, unit: "g", color: Color.strakkAccentYellow)
                if data.hiddenCount > 0 {
                    Text("\(data.hiddenCount) hidden")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }
            }
        }
        .padding(.horizontal, StrakkSpacing.lg)
        .padding(.top, StrakkSpacing.md)
        .padding(.bottom, StrakkSpacing.sm)
        .background(Color.strakkSurface1)
    }

    private func compactMacro(label: String, value: Double, unit: String, color: Color) -> some View {
        HStack(spacing: 4) {
            Text(label)
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
            Text(String(format: "%.0f\(unit)", value))
                .font(.strakkCaptionBold)
                .foregroundStyle(color)
                .monospacedDigit()
        }
    }

    // MARK: - Items list

    private var itemsList: some View {
        ScrollView {
            VStack(spacing: 0) {
                itemsSectionHeader

                if data.items.isEmpty {
                    StrakkEmptyState(
                        icon: "tray",
                        title: "No items detected",
                        message: "All items are hidden or removed. Tap + to add one."
                    )
                    .padding(.top, 40)
                } else {
                    VStack(spacing: 10) {
                        ForEach(
                            Array(data.items.enumerated()),
                            id: \.element.stableKey
                        ) { index, item in
                            GroundedItemCard(
                                item: item,
                                onAdjustGrams: { newGrams in
                                    viewModel.onEvent(PhotoMealEventAdjustQuantity(
                                        itemIndex: Int32(index),
                                        newGrams: newGrams
                                    ))
                                },
                                onSwap: {
                                    swapTargetIndex = index
                                    showSwapSheet = true
                                },
                                onEdit: {
                                    editTarget = EditTarget(id: index, item: item)
                                },
                                onHide: {
                                    viewModel.onEvent(PhotoMealEventHideItem(itemIndex: Int32(index)))
                                },
                                onRestore: {
                                    viewModel.onEvent(PhotoMealEventRestoreItem(itemIndex: Int32(index)))
                                },
                                onRemove: {
                                    viewModel.onEvent(PhotoMealEventRemoveItem(itemIndex: Int32(index)))
                                }
                            )
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 8)
                }
            }
            .padding(.bottom, 120)
        }
    }

    private var itemsSectionHeader: some View {
        HStack {
            Text("ITEMS")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .kerning(1.0)
            Spacer()
            Button {
                showAddPicker = true
            } label: {
                Image(systemName: "plus")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.strakkPrimary)
                    .frame(width: 28, height: 28)
                    .background(Color.strakkSurface2)
                    .clipShape(Circle())
            }
            .accessibilityLabel("Add item")
        }
        .padding(.horizontal, 20)
        .padding(.top, 16)
        .padding(.bottom, 8)
    }

    // MARK: - Save footer

    private var saveFooter: some View {
        VStack(spacing: 8) {
            if data.hiddenCount > 0 {
                Text("\(data.hiddenCount) item(s) hidden won't be saved")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkWarning)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.top, 8)
            }
            Divider().background(Color.strakkDivider)
            StrakkPrimaryButton(
                title: data.isSaving ? "Saving…" : "Save meal",
                action: { viewModel.onEvent(PhotoMealEventSave.shared) },
                isEnabled: !data.isSaving && data.hasVisibleItems
            )
            .accessibilityLabel(Text("Save meal"))
            .padding(.horizontal, StrakkSpacing.lg)
            .padding(.vertical, StrakkSpacing.md)
            .background(Color.strakkBackground)
        }
        .background(Color.strakkBackground)
    }
}
