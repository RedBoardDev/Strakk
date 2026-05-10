import SwiftUI
import shared

struct PhotoMealView: View {
    @Bindable var viewModel: PhotoMealViewModelWrapper
    let date: String
    let mealName: String
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()
                stateContent
            }
            .navigationTitle(navigationTitleKey)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { StrakkCloseToolbarItem(action: handleDismiss) }
        }
        .errorAlert(message: $viewModel.errorMessage)
        .featureGate($viewModel.gatedFeature)
        .onChange(of: viewModel.didCancel) { _, cancelled in
            if cancelled { onDismiss() }
        }
        .onChange(of: viewModel.savedMeal) { _, meal in
            if meal != nil { onDismiss() }
        }
    }

    private var navigationTitleKey: LocalizedStringKey {
        switch viewModel.state {
        case .capturing: return "Add a photo"
        case .identifying: return "Analyzing photo"
        case .grounding: return "Matching foods"
        case .reviewing: return "Review meal"
        }
    }

    private func handleDismiss() {
        viewModel.onEvent(PhotoMealEventCancel.shared)
        onDismiss()
    }

    // MARK: - State routing

    @ViewBuilder
    private var stateContent: some View {
        switch viewModel.state {
        case .capturing:
            capturingView

        case .identifying(let hint):
            identifyingView(hint: hint)

        case .grounding(let names):
            groundingView(names: names)

        case .reviewing(let data):
            MealReviewV3View(data: data, viewModel: viewModel)
        }
    }

    // MARK: - Capturing state

    private var capturingView: some View {
        PhotoHintView(
            onAdd: { base64, hint in
                viewModel.onEvent(PhotoMealEventStartAnalysis(
                    imageBase64: base64,
                    hint: hint,
                    date: date,
                    mealName: mealName
                ))
            },
            onCancel: handleDismiss
        )
    }

    // MARK: - Identifying state

    private func identifyingView(hint: String?) -> some View {
        VStack(spacing: 20) {
            ProgressView()
                .tint(Color.strakkPrimary)
                .scaleEffect(1.5)

            Text("Identifying foods...")
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkTextPrimary)

            if let hint, !hint.isEmpty {
                Text(hint)
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Grounding state

    private func groundingView(names: [String]) -> some View {
        VStack(alignment: .leading, spacing: 20) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Matching with database...")
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)

                Text("Looking up nutritional data for detected items.")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextSecondary)
            }
            .padding(.horizontal, 20)
            .padding(.top, 24)

            ScrollView {
                VStack(spacing: 10) {
                    ForEach(Array(names.enumerated()), id: \.offset) { _, name in
                        skeletonItemCard(name: name)
                    }
                }
                .padding(.horizontal, 20)
            }
        }
    }

    private func skeletonItemCard(name: String) -> some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.strakkSurface2)
                .frame(width: 34, height: 34)

            VStack(alignment: .leading, spacing: 6) {
                Text(name)
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .lineLimit(1)

                RoundedRectangle(cornerRadius: 4)
                    .fill(Color.strakkSurface2)
                    .frame(width: 120, height: 10)
            }

            Spacer()

            ProgressView()
                .tint(Color.strakkPrimary)
                .scaleEffect(0.8)
        }
        .padding(16)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}
