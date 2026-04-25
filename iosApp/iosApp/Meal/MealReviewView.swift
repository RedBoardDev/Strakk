import SwiftUI
import shared

struct MealReviewView: View {
    @Bindable var viewModel: MealDraftViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            switch viewModel.state {
            case .loading:
                ProgressView().tint(Color.strakkPrimary)

            case .empty:
                // Should not happen in review — go back
                Color.strakkBackground.onAppear { dismiss() }

            case .editing(let draft):
                reviewBody(draft: draft)
            }
        }
        .navigationTitle("Repas analysé")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(false)
    }

    // MARK: - Review body

    @ViewBuilder
    private func reviewBody(draft: EditingDraftData) -> some View {
        VStack(spacing: 0) {
            // Subtitle
            Text("\(draft.resolvedCount) items détectés. Modifiez si nécessaire avant de valider.")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .padding(.horizontal, 20)
                .padding(.top, 12)
                .frame(maxWidth: .infinity, alignment: .leading)

            // Items list (resolved only in review)
            List {
                ForEach(draft.items.filter(\.isResolved)) { item in
                    if case .resolved(let entry) = item.kind {
                        reviewEntryRow(item: item, entry: entry)
                            .listRowBackground(Color.strakkSurface1)
                            .listRowSeparatorTint(Color.strakkDivider)
                    }
                }
                .onDelete { offsets in
                    let resolved = draft.items.filter(\.isResolved)
                    for idx in offsets {
                        viewModel.onEvent(MealDraftEventRemoveItem(itemId: resolved[idx].id))
                    }
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .background(Color.strakkBackground)

            // Validate CTA
            Button {
                viewModel.onEvent(MealDraftEventCommit.shared)
            } label: {
                Text("Valider le repas")
                    .font(.strakkBodyBold)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(draft.items.isEmpty ? Color.strakkSurface2 : Color.strakkPrimary)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .disabled(draft.items.isEmpty)
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .accessibilityLabel("Valider le repas")
        }
    }

    private func reviewEntryRow(item: DraftItemData, entry: MealEntryData) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text(entry.name ?? "Item")
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)

                HStack(spacing: 4) {
                    Text(String(format: "%.0f kcal", entry.calories))
                        .foregroundStyle(Color.strakkTextSecondary)
                    if entry.protein > 0 {
                        Text("·")
                            .foregroundStyle(Color.strakkTextTertiary)
                        Text(String(format: "%.0fg prot", entry.protein))
                            .foregroundStyle(Color.strakkPrimary)
                    }
                    if let qty = entry.quantity {
                        Text("·")
                            .foregroundStyle(Color.strakkTextTertiary)
                        Text(qty)
                            .foregroundStyle(Color.strakkTextTertiary)
                    }
                }
                .font(.strakkCaption)
            }
            Spacer()
        }
        .padding(.vertical, 4)
        .accessibilityLabel(entry.name ?? "Item")
    }
}
