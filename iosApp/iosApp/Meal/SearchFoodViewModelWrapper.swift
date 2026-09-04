import SwiftUI
import shared

// MARK: - Swift-side data types

struct FrequentItemData: Identifiable, Equatable {
    var id: String { normalizedName }
    let normalizedName: String
    let name: String?
    let protein: Double
    let calories: Double
    let fat: Double?
    let carbs: Double?
    let quantity: String?
    let occurrences: Int
}

struct FoodCatalogItemData: Identifiable, Equatable {
    let id: Int64
    let name: String
    let brand: String?
    let protein: Double
    let calories: Double
    let fat: Double?
    let carbs: Double?
    let defaultPortionGrams: Double
    let servingLabel: String?
    let nutriscore: String?
}

struct FavoriteFoodData: Identifiable, Equatable {
    var id: String { storedId }
    let storedId: String
    let normalizedName: String
    let name: String
    let protein: Double
    let calories: Double
    let fat: Double?
    let carbs: Double?
    let quantity: String?
}

struct MealTemplateItemData: Equatable {
    let name: String
    let protein: Double
    let calories: Double
    let fat: Double?
    let carbs: Double?
    let quantity: String?
}

struct FavoriteMealData: Identifiable, Equatable {
    var id: String { storedId }
    let storedId: String
    let name: String
    let items: [MealTemplateItemData]
    let sourceMealId: String?
    var totalCalories: Double { items.reduce(0) { $0 + $1.calories } }
    var totalProtein: Double { items.reduce(0) { $0 + $1.protein } }
}

struct RecentMealData: Identifiable, Equatable {
    var id: String { mealId }
    let mealId: String
    let name: String
    let items: [MealTemplateItemData]
    var totalCalories: Double { items.reduce(0) { $0 + $1.calories } }
    var totalProtein: Double { items.reduce(0) { $0 + $1.protein } }
}

struct MyFoodsViewData: Equatable {
    let favoriteMeals: [FavoriteMealData]
    let favoriteFoods: [FavoriteFoodData]
    let recentMeals: [RecentMealData]
    let recentFoods: [FrequentItemData]
}

/// Backward-compat shape kept for ReviewSearchSheet / FoodSwapSheet which only
/// consume the catalog half of the search drawer.
struct SearchResultsData: Equatable {
    let userItems: [FrequentItemData]
    let catalogItems: [FoodCatalogItemData]
}

enum SearchTabSelection: Equatable {
    case myFoods
    case catalog
}

enum SearchFoodState: Equatable {
    case loading
    case ready(
        tab: SearchTabSelection,
        query: String,
        myFoods: MyFoodsViewData,
        catalogItems: [FoodCatalogItemData],
        isSearching: Bool
    )
    case error(String)
}

// MARK: - Wrapper

@MainActor
@Observable
final class SearchFoodViewModelWrapper {
    private let sharedVm: SearchFoodViewModel

    var state: SearchFoodState = .loading
    var errorMessage: String?
    var didAddMealTemplate: Bool = false

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinBridge.shared.getSearchFoodViewModel()
        self.state = Self.mapState(sharedVm.uiState.value as? SearchFoodUiState)

        stateTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<SearchFoodUiState> = observeFlow(sharedVm.uiState)
            for await newState in stream {
                self?.state = Self.mapState(newState)
            }
        }

        effectTask = Task { [weak self, sharedVm] in
            let stream: AsyncStream<SearchFoodEffect> = observeFlow(sharedVm.effects)
            for await effect in stream {
                self?.handleEffect(effect)
            }
        }
    }

    deinit {
        stateTask?.cancel()
        effectTask?.cancel()
    }

    func onEvent(_ event: SearchFoodEvent) {
        sharedVm.onEvent(event: event)
    }

    /// Resolves a raw KMP `FoodCatalogItem` by id from the current state.
    /// Used by swap sheets that need to pass the KMP object directly to an event.
    func resolveCatalogItem(id: Int64) -> FoodCatalogItem? {
        guard let ready = sharedVm.uiState.value as? SearchFoodUiStateReady else { return nil }
        return ready.catalog.items.first(where: { $0.id == id })
    }

    func consumeMealTemplateAdded() {
        didAddMealTemplate = false
    }

    // MARK: - Private

    private func handleEffect(_ effect: SearchFoodEffect) {
        if let showError = effect as? SearchFoodEffectShowError {
            errorMessage = showError.message
        } else if effect is SearchFoodEffectMealTemplateAdded {
            didAddMealTemplate = true
        }
    }

    private static func mapState(_ kmpState: SearchFoodUiState?) -> SearchFoodState {
        guard let kmpState else { return .loading }
        if kmpState is SearchFoodUiStateLoading {
            return .loading
        } else if let ready = kmpState as? SearchFoodUiStateReady {
            return mapReady(ready)
        } else if let error = kmpState as? SearchFoodUiStateError {
            return .error(error.message)
        }
        return .loading
    }

    private static func mapReady(_ ready: SearchFoodUiStateReady) -> SearchFoodState {
        let myFoods = MyFoodsViewData(
            favoriteMeals: ready.myFoods.favoriteMeals.map { mapFavoriteMeal($0) },
            favoriteFoods: ready.myFoods.favoriteFoods.map { mapFavoriteFood($0) },
            recentMeals: ready.myFoods.recentMeals.map { mapRecentMeal($0) },
            recentFoods: ready.myFoods.recentFoods.map { mapFrequent($0) }
        )
        let catalog = ready.catalog.items.map { mapCatalog($0) }
        let tab: SearchTabSelection = ready.selectedTab == .catalog ? .catalog : .myFoods
        return .ready(
            tab: tab,
            query: ready.query,
            myFoods: myFoods,
            catalogItems: catalog,
            isSearching: ready.isSearching
        )
    }

    private static func mapFavoriteFood(_ item: FavoriteFood) -> FavoriteFoodData {
        FavoriteFoodData(
            storedId: item.id,
            normalizedName: item.normalizedName,
            name: item.name,
            protein: item.protein,
            calories: item.calories,
            fat: item.fat?.doubleValue,
            carbs: item.carbs?.doubleValue,
            quantity: item.quantity
        )
    }

    private static func mapFavoriteMeal(_ item: FavoriteMeal) -> FavoriteMealData {
        FavoriteMealData(
            storedId: item.id,
            name: item.name,
            items: item.items.map { mapTemplateItem($0) },
            sourceMealId: item.sourceMealId
        )
    }

    private static func mapRecentMeal(_ item: RecentMeal) -> RecentMealData {
        RecentMealData(
            mealId: item.mealId,
            name: item.name,
            items: item.items.map { mapTemplateItem($0) }
        )
    }

    private static func mapTemplateItem(_ item: MealTemplateItem) -> MealTemplateItemData {
        MealTemplateItemData(
            name: item.name,
            protein: item.protein,
            calories: item.calories,
            fat: item.fat?.doubleValue,
            carbs: item.carbs?.doubleValue,
            quantity: item.quantity
        )
    }

    private static func mapFrequent(_ item: FrequentItem) -> FrequentItemData {
        FrequentItemData(
            normalizedName: item.normalizedName,
            name: item.name,
            protein: item.protein,
            calories: item.calories,
            fat: item.fat?.doubleValue,
            carbs: item.carbs?.doubleValue,
            quantity: item.quantity,
            occurrences: Int(item.occurrences)
        )
    }

    private static func mapCatalog(_ item: FoodCatalogItem) -> FoodCatalogItemData {
        FoodCatalogItemData(
            id: item.id,
            name: item.name,
            brand: item.brand,
            protein: item.protein,
            calories: item.calories,
            fat: item.fat?.doubleValue,
            carbs: item.carbs?.doubleValue,
            defaultPortionGrams: item.defaultPortionGrams,
            servingLabel: item.servingLabel,
            nutriscore: item.nutriscore
        )
    }
}
