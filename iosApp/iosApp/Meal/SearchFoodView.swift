import SwiftUI
import shared

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
        .errorAlert(message: $quickAddViewModel.errorMessage)
        .onChange(of: quickAddViewModel.didComplete) { _, didComplete in
            if didComplete {
                quickAddViewModel.consumeCompletion()
                onDismiss()
            }
        }
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
                        SectionHeader(title: "FRÉQUENTS")
                            .padding(.horizontal, 20)
                            .padding(.top, 16)
                            .padding(.bottom, 4)
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
                            SectionHeader(title: "MES ITEMS")
                                .padding(.horizontal, 20)
                                .padding(.top, 16)
                                .padding(.bottom, 4)
                            ForEach(results.userItems) { item in
                                frequentRow(item: item)
                            }
                        }
                        if !results.catalogItems.isEmpty {
                            SectionHeader(title: "CATALOGUE")
                                .padding(.horizontal, 20)
                                .padding(.top, 16)
                                .padding(.bottom, 4)
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
                            quickAddSearchItem(
                                name: item.name ?? item.normalizedName,
                                protein: protein, calories: calories,
                                fat: fat, carbs: carbs, quantity: qty,
                                source: EntrySource.frequent
                            )
                        }
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
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "book.closed")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.strakkTextTertiary)
                    .frame(width: 18)
                    .padding(.top, 2)

                VStack(alignment: .leading, spacing: 6) {
                    HStack(alignment: .top, spacing: 8) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.name)
                                .font(.strakkBody)
                                .foregroundStyle(Color.strakkTextPrimary)
                                .lineLimit(2)
                            if let brand = item.brand, !brand.isEmpty {
                                Text(brand)
                                    .font(.strakkCaption)
                                    .foregroundStyle(Color.strakkTextSecondary)
                            }
                        }
                        Spacer(minLength: 0)
                        if let grade = item.nutriscore?.first {
                            NutriscoreBadgeView(grade: grade)
                        }
                    }
                    macroLine(item: item)
                    HStack(spacing: 4) {
                        Image(systemName: "scalemass")
                            .font(.system(size: 10, weight: .medium))
                        Text("valeurs pour 100 g")
                            .font(.strakkCaption)
                    }
                    .foregroundStyle(Color.strakkTextTertiary)
                }
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
                            quickAddSearchItem(
                                name: item.name,
                                protein: protein, calories: calories,
                                fat: fat, carbs: carbs, quantity: qty,
                                source: EntrySource.search
                            )
                        }
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
        onAdd: @escaping (_ protein: Double, _ calories: Double, _ fat: Double?, _ carbs: Double?, _ grams: Double) -> Void
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
                let p = portionGrams
                onAdd(
                    proteinPer100 * p / 100,
                    caloriesPer100 * p / 100,
                    fatPer100.map { $0 * p / 100 },
                    carbsPer100.map { $0 * p / 100 },
                    p
                )
            } label: {
                HStack(spacing: 6) {
                    if quickAddViewModel.isProcessing {
                        ProgressView()
                            .tint(.white)
                            .scaleEffect(0.7)
                    }
                    Text(quickAddViewModel.isProcessing ? "Ajout..." : "Ajouter")
                        .font(.strakkCaptionBold)
                }
                .foregroundStyle(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(quickAddViewModel.isProcessing ? Color.strakkSurface2 : Color.strakkPrimary)
                .clipShape(Capsule())
            }
            .disabled(quickAddViewModel.isProcessing)
            .accessibilityLabel("Ajouter \(name)")
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(Color.strakkSurface1)
    }

    // MARK: - Quick-add (non-draft mode)

    private func quickAddSearchItem(
        name: String,
        protein: Double,
        calories: Double,
        fat: Double?,
        carbs: Double?,
        quantity: String,
        source: EntrySource
    ) {
        quickAddViewModel.addKnown(
            name: name,
            protein: protein,
            calories: calories,
            fat: fat,
            carbs: carbs,
            quantity: quantity,
            source: source
        )
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

private extension SearchFoodView {
    /// Dense macro line: kcal · prot · lip · gluc, each in its semantic colour.
    /// Mirrors the palette used in EntryDetailSheet / MealDraftView so the same
    /// number stays the same colour everywhere in the app.
    func macroLine(item: FoodCatalogItemData) -> some View {
        HStack(spacing: 6) {
            Text(String(format: "%.0f kcal", item.calories))
                .foregroundStyle(Color.strakkCalories)
            Text("·").foregroundStyle(Color.strakkTextTertiary)
            Text(String(format: "%.0f g prot", item.protein))
                .foregroundStyle(Color.strakkProtein)
            if let fat = item.fat {
                Text("·").foregroundStyle(Color.strakkTextTertiary)
                Text(String(format: "%.0f g lip", fat))
                    .foregroundStyle(Color.strakkAccentYellow)
            }
            if let carbs = item.carbs {
                Text("·").foregroundStyle(Color.strakkTextTertiary)
                Text(String(format: "%.0f g gluc", carbs))
                    .foregroundStyle(Color.strakkAccentIndigo)
            }
        }
        .font(.strakkCaption)
        .monospacedDigit()
        .lineLimit(1)
        .minimumScaleFactor(0.85)
    }
}

private struct NutriscoreBadgeView: View {
    let grade: Character

    var body: some View {
        let color: Color = {
            switch String(grade).lowercased() {
            case "a": return Color(red: 0.12, green: 0.56, blue: 0.24)
            case "b": return Color(red: 0.52, green: 0.73, blue: 0.18)
            case "c": return Color(red: 0.95, green: 0.76, blue: 0.20)
            case "d": return Color(red: 0.90, green: 0.49, blue: 0.13)
            case "e": return Color(red: 0.75, green: 0.22, blue: 0.17)
            default:  return Color.strakkTextTertiary
            }
        }()
        return Text(String(grade).uppercased())
            .font(.system(size: 11, weight: .bold))
            .foregroundStyle(.white)
            .frame(width: 22, height: 22)
            .background(color, in: RoundedRectangle(cornerRadius: 6))
    }
}
