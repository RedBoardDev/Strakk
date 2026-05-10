import SwiftUI
import shared

struct FoodSwapSheet: View {
    @Bindable var searchVm: SearchFoodViewModelWrapper
    let onSelect: (FoodCatalogItem) -> Void
    let onCancel: () -> Void

    @State private var query: String = ""

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()
                content
            }
            .navigationTitle("Change food")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                StrakkCloseToolbarItem(action: onCancel)
            }
            .searchable(text: $query, prompt: Text("Search catalog…"))
            .onChange(of: query) { _, newQuery in
                searchVm.onEvent(SearchFoodEventQueryChanged(query: newQuery))
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }

    // MARK: - Content

    @ViewBuilder
    private var content: some View {
        switch searchVm.state {
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
                Button("Retry") {
                    searchVm.onEvent(SearchFoodEventRetry.shared)
                }
                .foregroundStyle(Color.strakkPrimary)
            }

        case .ready(let queryText, let results, let isSearching):
            resultsList(query: queryText, results: results, isSearching: isSearching)
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
                    emptyQueryPrompt
                } else if results.catalogItems.isEmpty {
                    noResultsView(query: query)
                } else {
                    SectionHeader(title: "CATALOG")
                        .padding(.horizontal, 20)
                        .padding(.top, 16)
                        .padding(.bottom, 4)
                    ForEach(results.catalogItems) { item in
                        catalogRow(item: item)
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

    private func catalogRow(item: FoodCatalogItemData) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "book.closed")
                .font(.system(size: 13))
                .foregroundStyle(Color.strakkTextTertiary)
                .frame(width: 18)

            VStack(alignment: .leading, spacing: 3) {
                Text(item.name)
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .lineLimit(1)
                HStack(spacing: 4) {
                    Text(String(format: "%.0f kcal", item.calories))
                        .foregroundStyle(Color.strakkTextSecondary)
                    if let brand = item.brand, !brand.isEmpty {
                        Text("·").foregroundStyle(Color.strakkTextTertiary)
                        Text(brand)
                            .foregroundStyle(Color.strakkTextTertiary)
                    }
                }
                .font(.strakkCaption)
                .lineLimit(1)
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(Color.strakkTextTertiary)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
        .contentShape(Rectangle())
        .onTapGesture {
            if let kmpItem = searchVm.resolveCatalogItem(id: item.id) {
                onSelect(kmpItem)
            }
        }
        .accessibilityLabel("Select \(item.name)")
    }

    // MARK: - Empty states

    private var emptyQueryPrompt: some View {
        VStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 36))
                .foregroundStyle(Color.strakkTextTertiary)
            Text("Search the catalog")
                .font(.strakkHeading3)
                .foregroundStyle(Color.strakkTextSecondary)
            Text("Type a food name to find a replacement.")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextTertiary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 60)
    }

    private func noResultsView(query: String) -> some View {
        VStack(spacing: 8) {
            Text("No results for \"\(query)\"")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 48)
    }
}
