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

    @State private var showSearch = false
    @State private var showManual = false
    @State private var showText = false
    @State private var showPhoto = false

    // Quick-add async processing state
    @State private var isProcessing = false
    @State private var processingTask: Task<Void, Never>? = nil
    @State private var errorMessage: String?

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

                    if isProcessing {
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
        .alert("Erreur", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("OK") { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
        .sheet(isPresented: $showSearch) {
            SearchFoodView(
                draftViewModel: draftViewModel,
                isDraftMode: isDraftMode,
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
        isProcessing = true
        processingTask = Task { @MainActor in
            do {
                _ = try await KoinHelper().getQuickAddFromPhotoUseCase().invoke(
                    imageBase64: imageBase64,
                    hint: hint
                )
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                isProcessing = false
                onDismiss()
            } catch {
                isProcessing = false
                errorMessage = error.localizedDescription
            }
        }
    }

    private func quickAddFromText(description: String) {
        isProcessing = true
        processingTask = Task { @MainActor in
            do {
                _ = try await KoinHelper().getQuickAddFromTextUseCase().invoke(
                    description: description
                )
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                isProcessing = false
                onDismiss()
            } catch {
                isProcessing = false
                errorMessage = error.localizedDescription
            }
        }
    }
}
