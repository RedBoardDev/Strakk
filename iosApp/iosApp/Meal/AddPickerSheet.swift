import SwiftUI
import shared

// MARK: - AddPickerSheet

struct AddPickerSheet: View {
    let isDraftMode: Bool
    let draftViewModel: MealDraftViewModelWrapper
    let onDismiss: () -> Void
    let onFeatureGated: (Feature) -> Void
    let logDate: String?

    @State private var quickAddViewModel: QuickAddViewModelWrapper
    @State private var photoMealWrapper: PhotoMealViewModelWrapper
    @State private var showSearch = false
    @State private var showManual = false
    @State private var showText = false
    @State private var showPhotoMeal = false

    init(
        isDraftMode: Bool,
        draftViewModel: MealDraftViewModelWrapper,
        onDismiss: @escaping () -> Void,
        onFeatureGated: @escaping (Feature) -> Void = { _ in },
        logDate: String? = nil
    ) {
        self.isDraftMode = isDraftMode
        self.draftViewModel = draftViewModel
        self.onDismiss = onDismiss
        self.onFeatureGated = onFeatureGated
        self.logDate = logDate
        self._quickAddViewModel = State(initialValue: QuickAddViewModelWrapper(logDate: logDate))
        self._photoMealWrapper = State(initialValue: PhotoMealViewModelWrapper())
    }

    private var isProUser: Bool { KoinBridge.shared.isProUser() }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                VStack(spacing: StrakkSpacing.xs) {
                    optionRow(icon: "magnifyingglass", label: "Search") {
                        showSearch = true
                    }
                    optionRow(icon: "pencil", label: "Manual") {
                        showManual = true
                    }
                    optionRow(icon: "text.quote", label: "Free text", isPro: true) {
                        guardFeature(.aiTextAnalysis) { showText = true }
                    }
                    if !isDraftMode {
                        optionRow(icon: "camera.fill", label: "Photo", isPro: true) {
                            guardFeature(.aiPhotoAnalysis) { showPhotoMeal = true }
                        }
                    }

                    if quickAddViewModel.isProcessing {
                        HStack(spacing: 10) {
                            ProgressView().tint(Color.strakkPrimary)
                            Text("Analyzing…")
                                .font(.strakkBody)
                                .foregroundStyle(Color.strakkTextSecondary)
                        }
                        .padding(.top, StrakkSpacing.sm)
                    }

                    Spacer()
                }
                .padding(.horizontal, StrakkSpacing.lg)
                .padding(.top, StrakkSpacing.sm)
            }
            .navigationTitle(Text(isDraftMode ? "Add to meal" : "Quick add"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { StrakkCloseToolbarItem(action: onDismiss) }
        }
        .errorAlert(message: $quickAddViewModel.errorMessage)
        .presentationDetents([.fraction(0.45)])
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
                onDismiss: { showSearch = false; onDismiss() }
            )
        }
        .sheet(isPresented: $showManual) {
            ManualEntryView(
                draftViewModel: draftViewModel,
                isDraftMode: isDraftMode,
                logDate: logDate,
                onDismiss: { showManual = false; onDismiss() }
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
                        quickAddViewModel.addFromText(description: description)
                    }
                },
                onCancel: { showText = false }
            )
        }
        .fullScreenCover(isPresented: $showPhotoMeal) {
            PhotoMealView(
                viewModel: photoMealWrapper,
                date: logDate ?? todayDateString(),
                mealName: String(localized: "Photo meal"),
                onDismiss: { showPhotoMeal = false }
            )
        }
        .onChange(of: photoMealWrapper.savedMeal) { _, saved in
            if saved != nil {
                showPhotoMeal = false
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                onDismiss()
            }
        }
        .onChange(of: photoMealWrapper.didCancel) { _, cancelled in
            if cancelled { showPhotoMeal = false }
        }
    }

    // MARK: - Row

    private func optionRow(
        icon: String,
        label: LocalizedStringKey,
        isPro: Bool = false,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: { HapticEngine.light(); action() }, label: {
            HStack(spacing: StrakkSpacing.md) {
                ZStack {
                    Circle()
                        .fill(Color.strakkSurface2)
                        .frame(width: 40, height: 40)
                    Image(systemName: icon)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(Color.strakkPrimary)
                }

                Text(label)
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextPrimary)

                Spacer()

                if isPro && !isProUser {
                    ProBadge()
                }

                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(Color.strakkTextTertiary)
            }
            .padding(.horizontal, StrakkSpacing.md)
            .frame(height: 60)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        })
        .buttonStyle(.plain)
        .accessibilityLabel(Text(label) + Text((isPro && !isProUser) ? ", Pro" : ""))
    }

    // MARK: - Helpers

    private func guardFeature(_ feature: Feature, onGranted: () -> Void) {
        if isProUser { onGranted() } else { onFeatureGated(feature); onDismiss() }
    }

    private func todayDateString() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: Date())
    }
}
