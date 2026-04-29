import SwiftUI
import shared

// MARK: - Tile model

private struct AddTile: Identifiable {
    let id: String
    let icon: String
    let label: String
}

// MARK: - AddPickerSheet

struct AddPickerSheet: View {
    let isDraftMode: Bool
    let draftViewModel: MealDraftViewModelWrapper
    let onDismiss: () -> Void
    let logDate: String?

    @State private var quickAddViewModel: QuickAddViewModelWrapper
    @State private var showSearch = false
    @State private var showManual = false
    @State private var showText = false
    @State private var showPhoto = false

    init(
        isDraftMode: Bool,
        draftViewModel: MealDraftViewModelWrapper,
        onDismiss: @escaping () -> Void,
        logDate: String? = nil
    ) {
        self.isDraftMode = isDraftMode
        self.draftViewModel = draftViewModel
        self.onDismiss = onDismiss
        self.logDate = logDate
        self._quickAddViewModel = State(initialValue: QuickAddViewModelWrapper(logDate: logDate))
    }

    private var title: String {
        isDraftMode ? "Ajouter au repas" : "Ajout rapide"
    }

    private let tiles: [AddTile] = [
        AddTile(id: "search",  icon: "magnifyingglass",     label: "Rechercher"),
        AddTile(id: "manual",  icon: "pencil",              label: "Manuel"),
        AddTile(id: "text",    icon: "text.quote",          label: "Texte libre"),
        AddTile(id: "photo",   icon: "camera.fill",         label: "Photo"),
    ]

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                VStack(spacing: 24) {
                    // Grid 3 columns (5 tiles → 3+2)
                    let columns = [
                        GridItem(.flexible(), spacing: 12),
                        GridItem(.flexible(), spacing: 12),
                        GridItem(.flexible(), spacing: 12),
                    ]

                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(tiles) { tile in
                            tileButton(tile: tile)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 8)

                    if quickAddViewModel.isProcessing {
                        HStack(spacing: 10) {
                            ProgressView()
                                .tint(Color.strakkPrimary)
                            Text("Analyse en cours…")
                                .font(.strakkBody)
                                .foregroundStyle(Color.strakkTextSecondary)
                        }
                        .padding(.top, 8)
                    }

                    Spacer()
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { onDismiss() }
                        .foregroundStyle(Color.strakkTextSecondary)
                }
            }
        }
        .errorAlert(message: $quickAddViewModel.errorMessage)
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
        .onChange(of: quickAddViewModel.didComplete) { _, didComplete in
            if didComplete {
                quickAddViewModel.consumeCompletion()
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                onDismiss()
            }
        }
        .sheet(isPresented: $showSearch) {
            SearchFoodView(
                draftViewModel: draftViewModel,
                isDraftMode: isDraftMode,
                logDate: logDate,
                onDismiss: {
                    showSearch = false
                    onDismiss()
                }
            )
        }
        .sheet(isPresented: $showManual) {
            ManualEntryView(
                draftViewModel: draftViewModel,
                isDraftMode: isDraftMode,
                logDate: logDate,
                onDismiss: {
                    showManual = false
                    onDismiss()
                }
            )
        }
        .sheet(isPresented: $showText) {
            TextEntryView(
                onAdd: { description in
                    if isDraftMode {
                        draftViewModel.onEvent(MealDraftEventAddPendingText(description: description))
                        showText = false
                        onDismiss()
                    } else {
                        showText = false
                        quickAddFromText(description: description)
                    }
                },
                onCancel: { showText = false }
            )
        }
        .sheet(isPresented: $showPhoto) {
            PhotoHintView(
                onAdd: { imageBase64, hint in
                    if isDraftMode {
                        draftViewModel.onEvent(
                            MealDraftEventAddPendingPhoto(imageBase64: imageBase64, hint: hint)
                        )
                        showPhoto = false
                        onDismiss()
                    } else {
                        showPhoto = false
                        quickAddFromPhoto(imageBase64: imageBase64, hint: hint)
                    }
                },
                onCancel: { showPhoto = false }
            )
        }
    }

    private func tileButton(tile: AddTile) -> some View {
        Button {
            switch tile.id {
            case "search":  showSearch = true
            case "manual":  showManual = true
            case "text":    showText = true
            case "photo":   showPhoto = true
            default: break
            }
        } label: {
            VStack(spacing: 10) {
                Image(systemName: tile.icon)
                    .font(.system(size: 24, weight: .medium))
                    .foregroundStyle(Color.strakkPrimary)
                Text(tile.label)
                    .font(.strakkCaptionBold)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 88)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(tile.label)
    }

    // MARK: - Quick-add dispatchers

    private func quickAddFromPhoto(imageBase64: String, hint: String?) {
        quickAddViewModel.addFromPhoto(imageBase64: imageBase64, hint: hint)
    }

    private func quickAddFromText(description: String) {
        quickAddViewModel.addFromText(description: description)
    }
}
