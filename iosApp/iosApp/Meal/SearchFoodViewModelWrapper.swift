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

struct SearchResultsData: Equatable {
    let userItems: [FrequentItemData]
    let catalogItems: [FoodCatalogItemData]
}

enum SearchFoodState: Equatable {
    case loading
    case ready(query: String, results: SearchResultsData, isSearching: Bool)
    case error(String)
}

// MARK: - Wrapper

@MainActor
@Observable
final class SearchFoodViewModelWrapper {
    private let sharedVm: SearchFoodViewModel

    var state: SearchFoodState = .loading
    var errorMessage: String?

    @ObservationIgnored private var stateTask: Task<Void, Never>?
    @ObservationIgnored private var effectTask: Task<Void, Never>?

    init() {
        self.sharedVm = KoinHelper().getSearchFoodViewModel()
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

    // MARK: - Private

    private func handleEffect(_ effect: SearchFoodEffect) {
        if let showError = effect as? SearchFoodEffectShowError {
            errorMessage = showError.message
        }
    }

    private static func mapState(_ kmpState: SearchFoodUiState?) -> SearchFoodState {
        guard let kmpState else { return .loading }

        if kmpState is SearchFoodUiStateLoading {
            return .loading
        } else if let ready = kmpState as? SearchFoodUiStateReady {
            let results = SearchResultsData(
                userItems: ready.results.userItems.map { item in
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
                },
                catalogItems: ready.results.catalogItems.map { item in
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
            )
            return .ready(query: ready.query, results: results, isSearching: ready.isSearching)
        } else if let error = kmpState as? SearchFoodUiStateError {
            return .error(error.message)
        }
        return .loading
    }
}
