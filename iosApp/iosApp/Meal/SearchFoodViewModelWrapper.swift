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
    let protein: Double
    let calories: Double
    let fat: Double?
    let carbs: Double?
    let defaultPortionGrams: Double
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
    var selectedItem: (name: String, sourceId: String)?
    var errorMessage: String?

    nonisolated(unsafe) private var stateTask: Task<Void, Never>?
    nonisolated(unsafe) private var effectTask: Task<Void, Never>?

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
        if let selected = effect as? SearchFoodEffectItemSelected {
            selectedItem = (name: selected.name, sourceId: selected.sourceId)
        } else if let showError = effect as? SearchFoodEffectShowError {
            errorMessage = showError.message
        }
    }

    private static func mapState(_ kmpState: SearchFoodUiState?) -> SearchFoodState {
        guard let kmpState else { return .loading }

        if kmpState is SearchFoodUiStateLoading {
            return .loading
        } else if let ready = kmpState as? SearchFoodUiStateReady {
            let results = SearchResultsData(
                userItems: ready.results.userItems.compactMap { $0 as? FrequentItem }.map { item in
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
                catalogItems: ready.results.catalogItems.compactMap { $0 as? FoodCatalogItem }.map { item in
                    FoodCatalogItemData(
                        id: item.id,
                        name: item.name,
                        protein: item.protein,
                        calories: item.calories,
                        fat: item.fat?.doubleValue,
                        carbs: item.carbs?.doubleValue,
                        defaultPortionGrams: item.defaultPortionGrams
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
