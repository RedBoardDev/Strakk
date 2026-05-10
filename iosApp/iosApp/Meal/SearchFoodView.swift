import SwiftUI
import shared

// MARK: - QuickAddParams helper

struct QuickAddParams {
    let name: String
    let protein: Double
    let calories: Double
    let fat: Double?
    let carbs: Double?
    let quantity: String
    let source: EntrySource
}

// MARK: - SearchFoodView

struct SearchFoodView: View {
    let draftViewModel: MealDraftViewModelWrapper
    let isDraftMode: Bool
    let logDate: String?
    let onDismiss: () -> Void

    @State private var searchViewModel = SearchFoodViewModelWrapper()
    @State private var quickAddViewModel: QuickAddViewModelWrapper

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
        self._quickAddViewModel = State(initialValue: QuickAddViewModelWrapper(logDate: logDate))
    }

    @State private var query: String = ""
    @State private var selectedItemId: String?
    @State private var selectedCatalogId: Int64?
    @State private var portionGrams: Double = 100

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()
                content
            }
            .navigationTitle(Text("Search a food"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { StrakkCloseToolbarItem(action: onDismiss) }
            .searchable(text: $query, prompt: Text("Apple, chicken…"))
            .onChange(of: query) { _, newQuery in
                searchViewModel.onEvent(SearchFoodEventQueryChanged(query: newQuery))
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .errorAlert(message: $quickAddViewModel.errorMessage)
        .onChange(of: quickAddViewModel.didComplete) { _, didComplete in
            if didComplete {
                quickAddViewModel.consumeCompletion()
                onDismiss()
            }
        }
    }
}

// MARK: - Content

private extension SearchFoodView {
    @ViewBuilder
    var content: some View {
        switch searchViewModel.state {
        case .loading:
            ProgressView().tint(Color.strakkPrimary)
        case .error(let message):
            errorView(message: message)
        case .ready(let query, let results, let isSearching):
            resultsList(query: query, results: results, isSearching: isSearching)
        }
    }

    @ViewBuilder
    func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 40))
                .foregroundStyle(Color.strakkError)
            Text(message)
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            Button("Retry") {
                searchViewModel.onEvent(SearchFoodEventRetry.shared)
            }
            .foregroundStyle(Color.strakkPrimary)
        }
    }

    @ViewBuilder
    func resultsList(
        query: String,
        results: SearchResultsData,
        isSearching: Bool
    ) -> some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                resultsContent(query: query, results: results)
                if isSearching {
                    HStack {
                        Spacer()
                        ProgressView().tint(Color.strakkPrimary)
                        Spacer()
                    }
                    .padding(.vertical, 12)
                }
                Spacer().frame(height: 32)
            }
            .padding(.top, 8)
        }
    }

    @ViewBuilder
    func resultsContent(query: String, results: SearchResultsData) -> some View {
        if query.isEmpty {
            if results.userItems.isEmpty {
                emptyFrequentsView
            } else {
                sectionHeader("FREQUENT")
                ForEach(results.userItems) { item in frequentRow(item: item) }
            }
        } else {
            if results.userItems.isEmpty && results.catalogItems.isEmpty {
                noResultsView(query: query)
            } else {
                userItemsSection(items: results.userItems)
                catalogItemsSection(items: results.catalogItems)
            }
        }
    }

    @ViewBuilder
    func userItemsSection(items: [FrequentItemData]) -> some View {
        if !items.isEmpty {
            sectionHeader("MY ITEMS")
            ForEach(items) { item in frequentRow(item: item) }
        }
    }

    @ViewBuilder
    func catalogItemsSection(items: [FoodCatalogItemData]) -> some View {
        if !items.isEmpty {
            sectionHeader("CATALOG")
            ForEach(items) { item in catalogRow(item: item) }
        }
    }

    func sectionHeader(_ title: String) -> some View {
        SectionHeader(title: title)
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 4)
    }
}

// MARK: - Rows

private extension SearchFoodView {
    func frequentRow(item: FrequentItemData) -> some View {
        FrequentFoodRow(
            item: item,
            isSelected: selectedItemId == item.normalizedName,
            portionGrams: $portionGrams,
            isProcessing: quickAddViewModel.isProcessing,
            onTap: {
                withAnimation(.easeInOut(duration: 0.15)) {
                    if selectedItemId == item.normalizedName {
                        selectedItemId = nil
                        selectedCatalogId = nil
                    } else {
                        selectedItemId = item.normalizedName
                        selectedCatalogId = nil
                        portionGrams = 100
                    }
                }
            },
            onAdd: { protein, calories, fat, carbs, grams in
                let qty = String(format: "%.0fg", grams)
                if isDraftMode {
                    draftViewModel.onEvent(MealDraftEventAddManualItem(
                        name: item.name ?? item.normalizedName,
                        protein: protein,
                        calories: calories,
                        fat: asKotlinDouble(fat),
                        carbs: asKotlinDouble(carbs),
                        quantity: qty,
                        source: EntrySource.search
                    ))
                    onDismiss()
                } else {
                    quickAddSearchItem(QuickAddParams(
                        name: item.name ?? item.normalizedName,
                        protein: protein,
                        calories: calories,
                        fat: fat,
                        carbs: carbs,
                        quantity: qty,
                        source: EntrySource.frequent
                    ))
                }
            }
        )
    }

    func catalogRow(item: FoodCatalogItemData) -> some View {
        CatalogFoodRow(
            item: item,
            isSelected: selectedCatalogId == item.id,
            portionGrams: $portionGrams,
            isProcessing: quickAddViewModel.isProcessing,
            onTap: {
                withAnimation(.easeInOut(duration: 0.15)) {
                    if selectedCatalogId == item.id {
                        selectedCatalogId = nil
                        selectedItemId = nil
                    } else {
                        selectedCatalogId = item.id
                        selectedItemId = nil
                        portionGrams = item.defaultPortionGrams
                    }
                }
            },
            onAdd: { protein, calories, fat, carbs, grams in
                let qty = String(format: "%.0fg", grams)
                if isDraftMode {
                    draftViewModel.onEvent(MealDraftEventAddManualItem(
                        name: item.name,
                        protein: protein,
                        calories: calories,
                        fat: asKotlinDouble(fat),
                        carbs: asKotlinDouble(carbs),
                        quantity: qty,
                        source: EntrySource.search
                    ))
                    onDismiss()
                } else {
                    quickAddSearchItem(QuickAddParams(
                        name: item.name,
                        protein: protein,
                        calories: calories,
                        fat: fat,
                        carbs: carbs,
                        quantity: qty,
                        source: EntrySource.search
                    ))
                }
            }
        )
    }
}

// MARK: - Quick-add (non-draft mode)

private extension SearchFoodView {
    func quickAddSearchItem(_ params: QuickAddParams) {
        quickAddViewModel.addKnown(
            name: params.name,
            protein: params.protein,
            calories: params.calories,
            fat: params.fat,
            carbs: params.carbs,
            quantity: params.quantity,
            source: params.source
        )
    }
}

// MARK: - Empty states

private extension SearchFoodView {
    var emptyFrequentsView: some View {
        VStack(spacing: 8) {
            Image(systemName: "clock")
                .font(.system(size: 32))
                .foregroundStyle(Color.strakkTextTertiary)
            Text("No recent foods")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
            Text("Add one to build your history.")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 48)
    }

    func noResultsView(query: String) -> some View {
        VStack(spacing: 12) {
            Text("No results for \"\(query)\"")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            Button("Manual entry") { onDismiss() }
                .foregroundStyle(Color.strakkPrimary)
                .font(.strakkCaptionBold)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 48)
    }
}
