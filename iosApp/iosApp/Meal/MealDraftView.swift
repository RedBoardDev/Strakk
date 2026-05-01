import SwiftUI
import shared

struct MealDraftView: View {
    @Bindable var viewModel: MealDraftViewModelWrapper
    let onAdd: () -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var showRenameAlert = false
    @State private var renameText = ""
    @State private var showDiscardConfirm = false
    @State private var showReview = false
    @State private var showProcessConfirm = false
    @State private var editingItem: DraftItemData?

    var body: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            switch viewModel.state {
            case .loading:
                ProgressView().tint(Color.strakkPrimary)

            case .empty:
                emptyDraftView

            case .editing(let draft):
                editingBody(draft: draft)
            }
        }
        .navigationTitle(draftName)
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(false)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Menu {
                    Button("Rename") {
                        if case .editing(let d) = viewModel.state {
                            renameText = d.name
                        }
                        showRenameAlert = true
                    }
                    Button("Cancel meal", role: .destructive) {
                        showDiscardConfirm = true
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .foregroundStyle(Color.strakkPrimary)
                }
            }
        }
        .alert("Rename meal", isPresented: $showRenameAlert) {
            TextField("Meal name", text: $renameText)
                .autocorrectionDisabled(false)
            Button("OK") {
                let trimmed = renameText.trimmingCharacters(in: .whitespaces)
                if !trimmed.isEmpty {
                    viewModel.onEvent(MealDraftEventRename(name: trimmed))
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("1 to 60 characters")
        }
        .confirmationDialog(
            "Cancel meal?",
            isPresented: $showDiscardConfirm,
            titleVisibility: .visible
        ) {
            Button("Cancel meal", role: .destructive) {
                viewModel.onEvent(MealDraftEventDiscard.shared)
                dismiss()
            }
            Button("Keep", role: .cancel) {}
        } message: {
            Text("Your current meal will be deleted.")
        }
        .confirmationDialog(
            "Analyze meal?",
            isPresented: $showProcessConfirm,
            titleVisibility: .visible
        ) {
            Button("Start analysis") {
                viewModel.onEvent(MealDraftEventProcess.shared)
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Pending items will be analyzed by AI. This takes a few seconds.")
        }
        .errorAlert(message: $viewModel.errorMessage)
        .onChange(of: viewModel.navigateToReview) { _, navigate in
            if navigate {
                showReview = true
                viewModel.navigateToReview = false
            }
        }
        .navigationDestination(isPresented: $showReview) {
            MealReviewView(viewModel: viewModel)
        }
        .sheet(item: $editingItem) { item in
            if case .resolved(let entry) = item.kind {
                EditEntrySheet(
                    entry: entry,
                    onSave: { name, protein, calories, fat, carbs, quantity in
                        viewModel.onEvent(MealDraftEventUpdateResolvedItem(
                            itemId: item.id,
                            name: name,
                            protein: protein,
                            calories: calories,
                            fat: asKotlinDouble(fat),
                            carbs: asKotlinDouble(carbs),
                            quantity: quantity,
                            source: entry.source,
                            createdAt: entry.createdAt
                        ))
                        editingItem = nil
                    },
                    onCancel: { editingItem = nil }
                )
            }
        }
    }

    // MARK: - Draft name from state

    private var draftName: String {
        if case .editing(let d) = viewModel.state { return d.name }
        return "Meal in progress"
    }

    // MARK: - Editing body

    @ViewBuilder
    private func editingBody(draft: EditingDraftData) -> some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    mealHeader(draft: draft)
                    totalsCard(draft: draft)

                    VStack(alignment: .leading, spacing: 12) {
                        SectionHeader(title: "ITEMS")

                        if draft.items.isEmpty {
                            emptyItemsCard
                        } else {
                            VStack(spacing: 10) {
                                ForEach(draft.items) { item in
                                    draftItemCard(item: item)
                                }
                            }
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 24)
                .padding(.bottom, 20)
            }

            bottomBar(draft: draft)
        }

        // Processing overlay
        if draft.isProcessing {
            Color.black.opacity(0.5).ignoresSafeArea()
            VStack(spacing: 16) {
                ProgressView()
                    .tint(Color.strakkPrimary)
                    .scaleEffect(1.5)
                Text("Analyzing your meal...")
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextPrimary)
            }
            .padding(32)
            .background(Color.strakkSurface2)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    // MARK: - Totals card

    private func mealHeader(draft: EditingDraftData) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline) {
                Text("\(draft.items.count) item\(draft.items.count > 1 ? "s" : "")")
                    .font(.strakkOverline)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .kerning(1.0)
                Spacer()
                if draft.pendingCount > 0 {
                    Text("\(draft.pendingCount) en attente")
                        .font(.strakkCaptionBold)
                        .foregroundStyle(Color.strakkWarning)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Color.strakkSurface2)
                        .clipShape(Capsule())
                }
            }

            Text("Compose your meal, adjust portions, then analyze if needed.")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private func totalsCard(draft: EditingDraftData) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .lastTextBaseline) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("MEAL TOTAL")
                        .font(.strakkOverline)
                        .foregroundStyle(Color.strakkTextTertiary)
                        .kerning(1.0)
                    Text(String(format: "%.0f kcal", draft.totals.calories))
                        .font(.strakkDisplay)
                        .foregroundStyle(Color.strakkTextPrimary)
                        .monospacedDigit()
                }
                Spacer()
                Text(String(format: "%.0fg prot", draft.totals.protein))
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkPrimary)
                    .monospacedDigit()
            }

            HStack(spacing: 8) {
                macroPill(label: "Fat", value: draft.totals.fat, unit: "g")
                macroPill(label: "Carbs", value: draft.totals.carbs, unit: "g")
                macroPill(label: "Items", value: Double(draft.resolvedCount), unit: "")
            }
        }
        .padding(16)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func macroPill(label: String, value: Double, unit: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
            Text(String(format: "%.0f\(unit)", value))
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkTextPrimary)
                .monospacedDigit()
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color.strakkSurface2)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Draft item cards

    private func draftItemCard(item: DraftItemData) -> some View {
        HStack(spacing: 12) {
            itemIcon(item: item)

            switch item.kind {
            case .resolved(let entry):
                VStack(alignment: .leading, spacing: 6) {
                    Text(entry.name ?? "Item")
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextPrimary)
                        .lineLimit(1)
                    HStack(spacing: 4) {
                        Text(String(format: "%.0f kcal", entry.calories))
                            .foregroundStyle(Color.strakkCalories)
                        if entry.protein > 0 {
                            Text("·")
                                .foregroundStyle(Color.strakkTextTertiary)
                            Text(String(format: "%.0fg prot", entry.protein))
                                .foregroundStyle(Color.strakkPrimary)
                        }
                        if let qty = entry.quantity {
                            Text("·")
                                .foregroundStyle(Color.strakkTextTertiary)
                            Text(qty)
                                .foregroundStyle(Color.strakkTextTertiary)
                        }
                    }
                    .font(.strakkCaption)
                }
                Spacer()
                Button {
                    editingItem = item
                } label: {
                    Image(systemName: "pencil")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.strakkTextSecondary)
                        .frame(width: 34, height: 34)
                        .background(Color.strakkSurface2)
                        .clipShape(Circle())
                }
                .accessibilityLabel("Edit \(entry.name ?? "item")")

            case .pendingPhoto(let hint):
                VStack(alignment: .leading, spacing: 6) {
                    Text("Pending analysis")
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextSecondary)
                    if let h = hint, !h.isEmpty {
                        Text(h)
                            .font(.strakkCaption)
                            .foregroundStyle(Color.strakkTextTertiary)
                            .lineLimit(1)
                    }
                }
                Spacer()
                Image(systemName: "hourglass")
                    .foregroundStyle(Color.strakkTextTertiary)

            case .pendingText(let description):
                VStack(alignment: .leading, spacing: 6) {
                    Text("Pending text")
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextSecondary)
                    Text(description)
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                        .lineLimit(1)
                }
                Spacer()
                Image(systemName: "hourglass")
                    .foregroundStyle(Color.strakkTextTertiary)
            }

            Button(role: .destructive) {
                viewModel.onEvent(MealDraftEventRemoveItem(itemId: item.id))
            } label: {
                Image(systemName: "trash")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.strakkError)
                    .frame(width: 34, height: 34)
                    .background(Color.strakkSurface2)
                    .clipShape(Circle())
            }
            .accessibilityLabel("Delete")
        }
        .padding(16)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .accessibilityLabel(draftItemLabel(item: item))
    }

    private var emptyItemsCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Image(systemName: "fork.knife.circle")
                .font(.system(size: 28, weight: .medium))
                .foregroundStyle(Color.strakkPrimary)
            Text("Your meal is empty")
                .font(.strakkHeading3)
                .foregroundStyle(Color.strakkTextPrimary)
            Text("Add a manual item, a CIQUAL search, a barcode, a photo or text.")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    // MARK: - Bottom bar

    private func bottomBar(draft: EditingDraftData) -> some View {
        HStack(spacing: 12) {
            Button {
                onAdd()
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "plus")
                    Text("Add")
                }
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkPrimary)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(Color.strakkSurface1)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .accessibilityLabel("Add an item")

            Button {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                if draft.pendingCount > 0 {
                    showProcessConfirm = true
                } else {
                    viewModel.onEvent(MealDraftEventProcess.shared)
                }
            } label: {
                Text("Finish meal")
                    .font(.strakkBodyBold)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(draft.items.isEmpty ? Color.strakkSurface2 : Color.strakkPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .disabled(draft.items.isEmpty)
            .accessibilityLabel("Finish meal")
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
        .background(Color.strakkBackground)
    }

    // MARK: - Empty draft view

    private var emptyDraftView: some View {
        VStack(spacing: 16) {
            Image(systemName: "tray")
                .font(.system(size: 48))
                .foregroundStyle(Color.strakkTextTertiary)
            Text("No meal in progress")
                .font(.strakkHeading3)
                .foregroundStyle(Color.strakkTextSecondary)
            Button {
                viewModel.onEvent(MealDraftEventStartDraft(initialName: nil, date: nil))
            } label: {
                Text("Create a meal")
                    .font(.strakkBodyBold)
                    .foregroundStyle(.white)
                    .frame(height: 52)
                    .padding(.horizontal, 32)
                    .background(Color.strakkPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    // MARK: - Helpers

    private func draftItemLabel(item: DraftItemData) -> String {
        switch item.kind {
        case .resolved(let entry): return entry.name ?? "Item"
        case .pendingPhoto: return "Photo pending analysis"
        case .pendingText(let d): return "Pending text: \(d)"
        }
    }

    private func itemIcon(item: DraftItemData) -> some View {
        let systemName: String
        let tint: Color

        switch item.kind {
        case .resolved(let entry):
            switch entry.source {
            case .manual:
                systemName = "pencil"
            case .barcode:
                systemName = "barcode.viewfinder"
            case .photoai:
                systemName = "camera.fill"
            case .textai:
                systemName = "text.quote"
            default:
                systemName = "magnifyingglass"
            }
            tint = Color.strakkPrimary
        case .pendingPhoto:
            systemName = "camera.fill"
            tint = Color.strakkWarning
        case .pendingText:
            systemName = "text.quote"
            tint = Color.strakkWarning
        }

        return Image(systemName: systemName)
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(tint)
            .frame(width: 38, height: 38)
            .background(Color.strakkSurface2)
            .clipShape(Circle())
    }

}
