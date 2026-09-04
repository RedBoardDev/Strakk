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
    @State private var selectedTab: SearchTabSelection = .myFoods
    @State private var selectedFoodKey: String?
    @State private var selectedCatalogId: Int64?
    @State private var selectedMealKey: String?
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
            .searchable(text: $query, prompt: searchPrompt)
            .onChange(of: query) { _, newQuery in
                searchViewModel.onEvent(SearchFoodEventQueryChanged(query: newQuery))
            }
            .onChange(of: selectedTab) { _, newTab in
                let kmpTab: SearchTab = (newTab == .catalog) ? .catalog : .myfoods
                searchViewModel.onEvent(SearchFoodEventSwitchTab(tab: kmpTab))
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .errorAlert(message: $quickAddViewModel.errorMessage)
        .errorAlert(message: $searchViewModel.errorMessage)
        .onChange(of: quickAddViewModel.didComplete) { _, didComplete in
            if didComplete {
                quickAddViewModel.consumeCompletion()
                onDismiss()
            }
        }
        .onChange(of: searchViewModel.didAddMealTemplate) { _, didAdd in
            if didAdd {
                searchViewModel.consumeMealTemplateAdded()
                UINotificationFeedbackGenerator().notificationOccurred(.success)
                onDismiss()
            }
        }
    }

    private var searchPrompt: Text {
        selectedTab == .catalog
            ? Text("Apple, chicken…")
            : Text("Search my foods")
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
        case .ready(_, let queryText, let myFoods, let catalogItems, let isSearching):
            tabbedList(
                query: queryText,
                myFoods: myFoods,
                catalogItems: catalogItems,
                isSearching: isSearching
            )
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
    func tabbedList(
        query: String,
        myFoods: MyFoodsViewData,
        catalogItems: [FoodCatalogItemData],
        isSearching: Bool
    ) -> some View {
        VStack(spacing: 0) {
            Picker("Tab", selection: $selectedTab) {
                Text("My foods").tag(SearchTabSelection.myFoods)
                Text("Catalog").tag(SearchTabSelection.catalog)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 8)

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 0) {
                    switch selectedTab {
                    case .myFoods:
                        myFoodsContent(query: query, data: myFoods)
                    case .catalog:
                        catalogContent(query: query, items: catalogItems, isSearching: isSearching)
                    }
                    Spacer().frame(height: 32)
                }
                .padding(.top, 4)
            }
        }
    }

    func sectionHeader(_ title: String) -> some View {
        SectionHeader(title: title)
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 4)
    }
}

// MARK: - My foods tab

private extension SearchFoodView {
    @ViewBuilder
    func myFoodsContent(query: String, data: MyFoodsViewData) -> some View {
        let hasAnything = !data.favoriteMeals.isEmpty
            || !data.favoriteFoods.isEmpty
            || !data.recentMeals.isEmpty
            || !data.recentFoods.isEmpty

        if !hasAnything {
            if query.isEmpty { emptyMyFoodsView } else { noResultsView(query: query) }
        } else {
            if !data.favoriteMeals.isEmpty || !data.favoriteFoods.isEmpty {
                sectionHeader("FAVORITES")
            }
            ForEach(data.favoriteMeals) { meal in favoriteMealRow(meal: meal) }
            ForEach(data.favoriteFoods) { food in favoriteFoodRow(food: food) }

            if !data.recentMeals.isEmpty {
                sectionHeader("RECENT MEALS")
                ForEach(data.recentMeals) { meal in recentMealRow(meal: meal) }
            }

            if !data.recentFoods.isEmpty {
                sectionHeader("RECENT FOODS")
                let favSet = Set(data.favoriteFoods.map { $0.normalizedName })
                ForEach(data.recentFoods) { item in
                    frequentRow(item: item, isFavorited: favSet.contains(item.normalizedName))
                }
            }
        }
    }

    func favoriteMealRow(meal: FavoriteMealData) -> some View {
        let mealKey = "fav-\(meal.storedId)"
        return FavoriteMealCard(
            meal: meal,
            isSelected: selectedMealKey == mealKey,
            onTap: {
                withAnimation(.easeInOut(duration: 0.15)) {
                    selectedMealKey = (selectedMealKey == mealKey) ? nil : mealKey
                    selectedFoodKey = nil
                    selectedCatalogId = nil
                }
            },
            onAdd: { addFavoriteMealTemplate(meal: meal) },
            onUnfavorite: {
                searchViewModel.onEvent(SearchFoodEventUnfavoriteMeal(favoriteId: meal.storedId))
            }
        )
    }

    func recentMealRow(meal: RecentMealData) -> some View {
        let mealKey = "rec-\(meal.mealId)"
        return RecentMealCard(
            meal: meal,
            isSelected: selectedMealKey == mealKey,
            onTap: {
                withAnimation(.easeInOut(duration: 0.15)) {
                    selectedMealKey = (selectedMealKey == mealKey) ? nil : mealKey
                    selectedFoodKey = nil
                    selectedCatalogId = nil
                }
            },
            onAdd: { addRecentMealTemplate(meal: meal) }
        )
    }

    func favoriteFoodRow(food: FavoriteFoodData) -> some View {
        let key = "favfood-\(food.normalizedName)"
        let item = FrequentItemData(
            normalizedName: food.normalizedName,
            name: food.name,
            protein: food.protein,
            calories: food.calories,
            fat: food.fat,
            carbs: food.carbs,
            quantity: food.quantity,
            occurrences: 0
        )
        return frequentRow(item: item, key: key, isFavorited: true)
    }

    func frequentRow(item: FrequentItemData, key: String? = nil, isFavorited: Bool) -> some View {
        let rowKey = key ?? "freq-\(item.normalizedName)"
        return FrequentFoodRow(
            item: item,
            isSelected: selectedFoodKey == rowKey,
            isFavorited: isFavorited,
            portionGrams: $portionGrams,
            isProcessing: quickAddViewModel.isProcessing,
            onTap: {
                withAnimation(.easeInOut(duration: 0.15)) {
                    selectedFoodKey = (selectedFoodKey == rowKey) ? nil : rowKey
                    selectedCatalogId = nil
                    selectedMealKey = nil
                    portionGrams = 100
                }
            },
            onToggleFavorite: {
                searchViewModel.onEvent(SearchFoodEventToggleFavoriteFood(
                    name: item.name ?? item.normalizedName,
                    protein: item.protein,
                    calories: item.calories,
                    fat: asKotlinDouble(item.fat),
                    carbs: asKotlinDouble(item.carbs),
                    quantity: item.quantity,
                    foodCatalogId: nil
                ))
            },
            onAdd: { protein, calories, fat, carbs, grams in
                addFoodEntry(
                    name: item.name ?? item.normalizedName,
                    protein: protein,
                    calories: calories,
                    fat: fat,
                    carbs: carbs,
                    grams: grams,
                    source: .frequent
                )
            }
        )
    }
}

// MARK: - Catalog tab

private extension SearchFoodView {
    @ViewBuilder
    func catalogContent(
        query: String,
        items: [FoodCatalogItemData],
        isSearching: Bool
    ) -> some View {
        if query.isEmpty {
            emptyCatalogView
        } else if items.isEmpty {
            if isSearching {
                HStack { Spacer(); ProgressView().tint(Color.strakkPrimary); Spacer() }
                    .padding(.vertical, 40)
            } else {
                noResultsView(query: query)
            }
        } else {
            sectionHeader("CATALOG")
            ForEach(items) { item in catalogRow(item: item) }
            if isSearching {
                HStack { Spacer(); ProgressView().tint(Color.strakkPrimary); Spacer() }
                    .padding(.vertical, 12)
            }
        }
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
                        selectedFoodKey = nil
                    } else {
                        selectedCatalogId = item.id
                        selectedFoodKey = nil
                        selectedMealKey = nil
                        portionGrams = item.defaultPortionGrams
                    }
                }
            },
            onAdd: { protein, calories, fat, carbs, grams in
                addFoodEntry(
                    name: item.name,
                    protein: protein,
                    calories: calories,
                    fat: fat,
                    carbs: carbs,
                    grams: grams,
                    source: .search
                )
            }
        )
    }
}

// MARK: - Actions

private extension SearchFoodView {
    // swiftlint:disable:next function_parameter_count
    func addFoodEntry(
        name: String,
        protein: Double,
        calories: Double,
        fat: Double?,
        carbs: Double?,
        grams: Double,
        source: EntrySource
    ) {
        let qty = String(format: "%.0fg", grams)
        if isDraftMode {
            draftViewModel.onEvent(MealDraftEventAddManualItem(
                name: name,
                protein: protein,
                calories: calories,
                fat: asKotlinDouble(fat),
                carbs: asKotlinDouble(carbs),
                quantity: qty,
                source: source
            ))
            onDismiss()
        } else {
            quickAddViewModel.addKnown(
                name: name,
                protein: protein,
                calories: calories,
                fat: fat,
                carbs: carbs,
                quantity: qty,
                source: source
            )
        }
    }

    func addFavoriteMealTemplate(meal: FavoriteMealData) {
        let items = meal.items.map { templateItemKmp($0) }
        let ref = MealTemplateRefFavorite(id: meal.storedId, name: meal.name, items: items)
        searchViewModel.onEvent(SearchFoodEventAddMealTemplate(ref: ref, logDate: logDate))
    }

    func addRecentMealTemplate(meal: RecentMealData) {
        let items = meal.items.map { templateItemKmp($0) }
        let ref = MealTemplateRefRecent(sourceMealId: meal.mealId, name: meal.name, items: items)
        searchViewModel.onEvent(SearchFoodEventAddMealTemplate(ref: ref, logDate: logDate))
    }

    func templateItemKmp(_ item: MealTemplateItemData) -> MealTemplateItem {
        MealTemplateItem(
            name: item.name,
            protein: item.protein,
            calories: item.calories,
            fat: asKotlinDouble(item.fat),
            carbs: asKotlinDouble(item.carbs),
            quantity: item.quantity
        )
    }
}

// MARK: - Empty states

private extension SearchFoodView {
    var emptyMyFoodsView: some View {
        VStack(spacing: 8) {
            Image(systemName: "heart")
                .font(.system(size: 32))
                .foregroundStyle(Color.strakkTextTertiary)
            Text("No saved foods yet")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
            Text("Foods you add and favorites you save will show up here.")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 48)
    }

    var emptyCatalogView: some View {
        VStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 32))
                .foregroundStyle(Color.strakkTextTertiary)
            Text("Search the food catalog")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
            Text("Type to look up an item in CIQUAL / Open Food Facts.")
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
