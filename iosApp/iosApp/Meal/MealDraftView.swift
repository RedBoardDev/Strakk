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
                    Button("Renommer") {
                        if case .editing(let d) = viewModel.state {
                            renameText = d.name
                        }
                        showRenameAlert = true
                    }
                    Button("Annuler le repas", role: .destructive) {
                        showDiscardConfirm = true
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .foregroundStyle(Color.strakkPrimary)
                }
            }
        }
        .alert("Renommer le repas", isPresented: $showRenameAlert) {
            TextField("Nom du repas", text: $renameText)
                .autocorrectionDisabled(false)
            Button("OK") {
                let trimmed = renameText.trimmingCharacters(in: .whitespaces)
                if !trimmed.isEmpty {
                    viewModel.onEvent(MealDraftEventRename(name: trimmed))
                }
            }
            Button("Annuler", role: .cancel) {}
        } message: {
            Text("1 à 60 caractères")
        }
        .confirmationDialog(
            "Annuler le repas ?",
            isPresented: $showDiscardConfirm,
            titleVisibility: .visible
        ) {
            Button("Annuler le repas", role: .destructive) {
                viewModel.onEvent(MealDraftEventDiscard.shared)
                dismiss()
            }
            Button("Garder", role: .cancel) {}
        } message: {
            Text("Votre repas en cours sera supprimé.")
        }
        .confirmationDialog(
            "Analyser le repas ?",
            isPresented: $showProcessConfirm,
            titleVisibility: .visible
        ) {
            Button("Lancer l'analyse") {
                viewModel.onEvent(MealDraftEventProcess.shared)
            }
            Button("Annuler", role: .cancel) {}
        } message: {
            Text("Des items en attente seront analysés par IA. L'opération prend quelques secondes.")
        }
        .alert("Erreur", isPresented: Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.errorMessage = nil } }
        )) {
            Button("OK") { viewModel.errorMessage = nil }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
        .onChange(of: viewModel.navigateToReview) { _, navigate in
            if navigate {
                showReview = true
                viewModel.navigateToReview = false
            }
        }
        .navigationDestination(isPresented: $showReview) {
            MealReviewView(viewModel: viewModel)
        }
    }

    // MARK: - Draft name from state

    private var draftName: String {
        if case .editing(let d) = viewModel.state { return d.name }
        return "Repas en cours"
    }

    // MARK: - Editing body

    @ViewBuilder
    private func editingBody(draft: EditingDraftData) -> some View {
        VStack(spacing: 0) {
            // Totals card
            totalsCard(draft: draft)
                .padding(.horizontal, 20)
                .padding(.top, 12)

            // Items list
            if draft.items.isEmpty {
                Spacer()
                Text("Votre repas est vide. Tapez + Ajouter pour commencer.")
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
                Spacer()
            } else {
                List {
                    ForEach(draft.items) { item in
                        draftItemRow(item: item)
                            .listRowBackground(Color.strakkSurface1)
                            .listRowSeparatorTint(Color.strakkDivider)
                    }
                    .onDelete { offsets in
                        let items = draft.items
                        for idx in offsets {
                            viewModel.onEvent(MealDraftEventRemoveItem(itemId: items[idx].id))
                        }
                    }
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .background(Color.strakkBackground)
            }

            // Bottom buttons
            bottomBar(draft: draft)
        }

        // Processing overlay
        if draft.isProcessing {
            Color.black.opacity(0.5).ignoresSafeArea()
            VStack(spacing: 16) {
                ProgressView()
                    .tint(Color.strakkPrimary)
                    .scaleEffect(1.5)
                Text("Analyse de votre repas...")
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextPrimary)
            }
            .padding(32)
            .background(Color.strakkSurface2)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }

    // MARK: - Totals card

    private func totalsCard(draft: EditingDraftData) -> some View {
        HStack(spacing: 0) {
            macroCell(label: "PROT", value: draft.totals.protein, unit: "g", color: .strakkPrimary)
            Divider().frame(height: 32).background(Color.strakkDivider)
            macroCell(label: "KCAL", value: draft.totals.calories, unit: "", color: .strakkPrimaryLight)
            Divider().frame(height: 32).background(Color.strakkDivider)
            macroCell(label: "FAT", value: draft.totals.fat, unit: "g", color: .strakkTextSecondary)
            Divider().frame(height: 32).background(Color.strakkDivider)
            macroCell(label: "CARBS", value: draft.totals.carbs, unit: "g", color: .strakkTextSecondary)
        }
        .padding(.vertical, 12)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            Group {
                if draft.pendingCount > 0 {
                    HStack {
                        Spacer()
                        Text("\(draft.pendingCount) en attente")
                            .font(.strakkCaption)
                            .foregroundStyle(Color.strakkTextTertiary)
                            .padding(.trailing, 12)
                    }
                    .frame(maxHeight: .infinity, alignment: .bottom)
                    .padding(.bottom, 4)
                }
            }
        )
    }

    private func macroCell(label: String, value: Double, unit: String, color: Color) -> some View {
        VStack(spacing: 2) {
            Text(label)
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
            Text(String(format: "%.0f\(unit)", value))
                .font(.strakkBodyBold)
                .foregroundStyle(color)
                .monospacedDigit()
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Draft item row

    private func draftItemRow(item: DraftItemData) -> some View {
        HStack(spacing: 12) {
            switch item.kind {
            case .resolved(let entry):
                VStack(alignment: .leading, spacing: 2) {
                    Text(entry.name ?? "Item")
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextPrimary)
                    HStack(spacing: 4) {
                        Text(String(format: "%.0f kcal", entry.calories))
                            .foregroundStyle(Color.strakkTextSecondary)
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
                sourceIconSmall(for: entry.source)

            case .pendingPhoto(let hint):
                VStack(alignment: .leading, spacing: 2) {
                    Text("Analyse en attente")
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
                VStack(alignment: .leading, spacing: 2) {
                    Text("Texte en attente")
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
        }
        .padding(.vertical, 4)
        .accessibilityLabel(draftItemLabel(item: item))
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
                    Text("Ajouter")
                }
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkPrimary)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(Color.strakkSurface1)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .accessibilityLabel("Ajouter un item")

            Button {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                if draft.pendingCount > 0 {
                    showProcessConfirm = true
                } else {
                    viewModel.onEvent(MealDraftEventProcess.shared)
                }
            } label: {
                Text("Terminer le repas")
                    .font(.strakkBodyBold)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(draft.items.isEmpty ? Color.strakkSurface2 : Color.strakkPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .disabled(draft.items.isEmpty)
            .accessibilityLabel("Terminer le repas")
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
            Text("Aucun repas en cours")
                .font(.strakkHeading3)
                .foregroundStyle(Color.strakkTextSecondary)
            Button {
                viewModel.onEvent(MealDraftEventStartDraft(initialName: nil, date: nil))
            } label: {
                Text("Créer un repas")
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
        case .pendingPhoto: return "Photo en attente d'analyse"
        case .pendingText(let d): return "Texte en attente: \(d)"
        }
    }

    @ViewBuilder
    private func sourceIconSmall(for source: EntrySource) -> some View {
        switch source {
        case .photoai:
            Image(systemName: "camera.fill")
                .font(.system(size: 13))
                .foregroundStyle(Color.strakkTextTertiary)
        case .barcode:
            Image(systemName: "barcode.viewfinder")
                .font(.system(size: 13))
                .foregroundStyle(Color.strakkTextTertiary)
        case .manual:
            Image(systemName: "pencil")
                .font(.system(size: 13))
                .foregroundStyle(Color.strakkTextTertiary)
        case .textai:
            Image(systemName: "text.quote")
                .font(.system(size: 13))
                .foregroundStyle(Color.strakkTextTertiary)
        default:
            Image(systemName: "magnifyingglass")
                .font(.system(size: 13))
                .foregroundStyle(Color.strakkTextTertiary)
        }
    }
}
