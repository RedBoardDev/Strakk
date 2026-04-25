import SwiftUI
import shared

struct SearchFoodView: View {
    let draftViewModel: MealDraftViewModelWrapper
    let isDraftMode: Bool
    let onDismiss: () -> Void

    @State private var searchViewModel = SearchFoodViewModelWrapper()
    @State private var query: String = ""
    @State private var selectedItemId: String?      // userItem normalizedName
    @State private var selectedCatalogId: Int64?
    @State private var portionGrams: Double = 100

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()
                content
            }
            .navigationTitle("Rechercher un aliment")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { onDismiss() }
                        .foregroundStyle(Color.strakkTextSecondary)
                }
            }
            .searchable(text: $query, prompt: "Pomme, poulet...")
            .onChange(of: query) { _, newQuery in
                searchViewModel.onEvent(SearchFoodEventQueryChanged(query: newQuery))
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }

    // MARK: - Content

    @ViewBuilder
    private var content: some View {
        switch searchViewModel.state {
        case .loading:
            ProgressView().tint(Color.strakkPrimary)

        case .error(let message):
            VStack(spacing: 16) {
                Image(systemName: "exclamationmark.triangle")
                    .font(.system(size: 40))
                    .foregroundStyle(Color.strakkError)
                Text(message)
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
                Button("Réessayer") {
                    searchViewModel.onEvent(SearchFoodEventRetry.shared)
                }
                .foregroundStyle(Color.strakkPrimary)
            }

        case .ready(let q, let results, let isSearching):
            resultsList(query: q, results: results, isSearching: isSearching)
        }
    }

    @ViewBuilder
    private func resultsList(
        query: String,
        results: SearchResultsData,
        isSearching: Bool
    ) -> some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                if query.isEmpty {
                    // Fréquents
                    if results.userItems.isEmpty {
                        emptyFrequentsView
                    } else {
                        sectionHeader("FRÉQUENTS")
                        ForEach(results.userItems) { item in
                            frequentRow(item: item)
                        }
                    }
                } else {
                    // Résultats mixtes
                    if results.userItems.isEmpty && results.catalogItems.isEmpty {
                        noResultsView(query: query)
                    } else {
                        if !results.userItems.isEmpty {
                            sectionHeader("MES ITEMS")
                            ForEach(results.userItems) { item in
                                frequentRow(item: item)
                            }
                        }
                        if !results.catalogItems.isEmpty {
                            sectionHeader("CATALOGUE")
                            ForEach(results.catalogItems) { item in
                                catalogRow(item: item)
                            }
                        }
                    }
                }

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

    // MARK: - Rows

    private func frequentRow(item: FrequentItemData) -> some View {
        let isSelected = selectedItemId == item.normalizedName
        return VStack(spacing: 0) {
            HStack(spacing: 12) {
                Image(systemName: "clock")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.strakkTextTertiary)
                    .frame(width: 18)

                VStack(alignment: .leading, spacing: 2) {
                    Text(item.name ?? item.normalizedName)
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Text(String(format: "%.0f kcal", item.calories))
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextSecondary)
                }
                Spacer()
                if let qty = item.quantity {
                    Text(qty)
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
            .onTapGesture {
                withAnimation(.easeInOut(duration: 0.15)) {
                    if isSelected {
                        selectedItemId = nil
                        selectedCatalogId = nil
                    } else {
                        selectedItemId = item.normalizedName
                        selectedCatalogId = nil
                        portionGrams = 100
                    }
                }
            }

            if isSelected {
                portionRow(
                    name: item.name ?? item.normalizedName,
                    proteinPer100: item.protein,
                    caloriesPer100: item.calories,
                    fatPer100: item.fat,
                    carbsPer100: item.carbs,
                    onAdd: {
                        searchViewModel.onEvent(
                            SearchFoodEventSelectUserItem(normalizedName: item.normalizedName)
                        )
                        onDismiss()
                    }
                )
                .transition(.opacity.combined(with: .move(edge: .top)))
            }

            Divider()
                .background(Color.strakkDivider)
                .padding(.leading, 50)
        }
    }

    private func catalogRow(item: FoodCatalogItemData) -> some View {
        let isSelected = selectedCatalogId == item.id
        return VStack(spacing: 0) {
            HStack(spacing: 12) {
                Image(systemName: "book.closed")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.strakkTextTertiary)
                    .frame(width: 18)

                VStack(alignment: .leading, spacing: 2) {
                    Text(item.name)
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Text(String(format: "%.0f kcal/100g", item.calories))
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextSecondary)
                }
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
            .onTapGesture {
                withAnimation(.easeInOut(duration: 0.15)) {
                    if isSelected {
                        selectedCatalogId = nil
                        selectedItemId = nil
                    } else {
                        selectedCatalogId = item.id
                        selectedItemId = nil
                        portionGrams = item.defaultPortionGrams
                    }
                }
            }

            if isSelected {
                portionRow(
                    name: item.name,
                    proteinPer100: item.protein,
                    caloriesPer100: item.calories,
                    fatPer100: item.fat,
                    carbsPer100: item.carbs,
                    onAdd: {
                        searchViewModel.onEvent(SearchFoodEventSelectCatalogItem(id: item.id))
                        onDismiss()
                    }
                )
                .transition(.opacity.combined(with: .move(edge: .top)))
            }

            Divider()
                .background(Color.strakkDivider)
                .padding(.leading, 50)
        }
    }

    private func portionRow(
        name: String,
        proteinPer100: Double,
        caloriesPer100: Double,
        fatPer100: Double?,
        carbsPer100: Double?,
        onAdd: @escaping () -> Void
    ) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Button {
                        portionGrams = max(10, portionGrams - 10)
                    } label: {
                        Image(systemName: "minus.circle.fill")
                            .font(.system(size: 22))
                            .foregroundStyle(Color.strakkSurface2)
                    }
                    Text(String(format: "%.0fg", portionGrams))
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextPrimary)
                        .monospacedDigit()
                        .frame(minWidth: 48)
                    Button {
                        portionGrams += 10
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 22))
                            .foregroundStyle(Color.strakkPrimary)
                    }
                }

                Text(String(format: "%.0f kcal · %.0fg prot",
                            caloriesPer100 * portionGrams / 100,
                            proteinPer100 * portionGrams / 100))
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .monospacedDigit()
            }

            Spacer()

            Button {
                onAdd()
            } label: {
                Text("Ajouter")
                    .font(.strakkCaptionBold)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.strakkPrimary)
                    .clipShape(Capsule())
            }
            .accessibilityLabel("Ajouter \(name)")
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(Color.strakkSurface1)
    }

    // MARK: - Section header

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.strakkOverline)
            .foregroundStyle(Color.strakkTextTertiary)
            .kerning(1.0)
            .padding(.horizontal, 20)
            .padding(.top, 16)
            .padding(.bottom, 4)
    }

    // MARK: - Empty states

    private var emptyFrequentsView: some View {
        VStack(spacing: 8) {
            Image(systemName: "clock")
                .font(.system(size: 32))
                .foregroundStyle(Color.strakkTextTertiary)
            Text("Aucun aliment récent")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
            Text("Ajoutez-en un pour alimenter votre historique.")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 48)
    }

    private func noResultsView(query: String) -> some View {
        VStack(spacing: 12) {
            Text("Aucun résultat pour \"\(query)\"")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            Button("Ajout manuel") {
                // Dismiss and open manual entry
                onDismiss()
            }
            .foregroundStyle(Color.strakkPrimary)
            .font(.strakkCaptionBold)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 48)
    }
}
