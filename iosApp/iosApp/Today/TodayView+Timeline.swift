import SwiftUI
import shared

extension TodayView {

    // MARK: - Timeline row dispatch

    @ViewBuilder
    func timelineRow(item: TimelineItemData) -> some View {
        switch item {
        case .mealContainer(let meal):
            mealContainerRow(meal: meal)
        case .orphanEntry(let entry):
            orphanEntryRow(entry: entry)
        }
    }

    // MARK: - Meal container row

    func mealContainerRow(meal: MealData) -> some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            selectedMeal = meal
        } label: {
            HStack(spacing: 10) {
                Text(timeLabel(from: meal.createdAt))
                    .font(.strakkCaptionBold)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .frame(width: 44, alignment: .leading)

                Image(systemName: "fork.knife")
                    .font(.system(size: 13))
                    .foregroundStyle(Color.strakkTextSecondary)
                    .frame(width: 16)

                VStack(alignment: .leading, spacing: 2) {
                    Text(meal.name)
                        .font(.strakkHeading3)
                        .foregroundStyle(Color.strakkTextPrimary)
                        .lineLimit(1)
                    Text("\(meal.entries.count) item\(meal.entries.count > 1 ? "s" : "")")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextSecondary)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(Color.strakkTextTertiary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
        }
        .buttonStyle(.plain)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .contextMenu {
            Button(role: .destructive) {
                viewModel.onEvent(TodayEventOnDeleteMeal(mealId: meal.id))
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
            Button(role: .destructive) {
                viewModel.onEvent(TodayEventOnDeleteMeal(mealId: meal.id))
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
        .accessibilityLabel("\(meal.name), \(meal.entries.count) items")
    }

    // MARK: - Orphan entry row

    func orphanEntryRow(entry: MealEntryData) -> some View {
        Button {
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            selectedEntry = entry
        } label: {
            HStack(spacing: 10) {
                Text(timeLabel(from: entry.createdAt))
                    .font(.strakkCaptionBold)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .frame(width: 44, alignment: .leading)

                sourceIcon(for: entry.source)
                    .frame(width: 16)

                Text(entry.name ?? "Item")
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .lineLimit(1)

                if let qty = entry.quantity {
                    Text(qty)
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }

                Spacer()
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
        }
        .buttonStyle(.plain)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) {
                viewModel.onEvent(TodayEventOnDeleteOrphanEntry(id: entry.id))
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
        .contextMenu {
            Button { editingEntry = entry } label: {
                Label("Edit", systemImage: "pencil")
            }
            Button(role: .destructive) {
                viewModel.onEvent(TodayEventOnDeleteOrphanEntry(id: entry.id))
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
        .accessibilityLabel(entry.name ?? "Item")
    }

    // MARK: - Empty state

    var emptyTimelineView: some View {
        VStack(spacing: 20) {
            HStack(spacing: 14) {
                Rectangle()
                    .fill(LinearGradient(
                        colors: [.clear, Color.strakkDivider],
                        startPoint: .leading,
                        endPoint: .trailing))
                    .frame(height: 1)

                ZStack {
                    Circle()
                        .fill(Color.strakkSurface2)
                        .overlay(Circle().strokeBorder(Color.strakkDivider, lineWidth: 1))
                        .frame(width: 64, height: 64)
                    Image(systemName: "fork.knife")
                        .font(.system(size: 26, weight: .medium))
                        .foregroundStyle(Color.strakkTextSecondary)
                }

                Rectangle()
                    .fill(LinearGradient(
                        colors: [Color.strakkDivider, .clear],
                        startPoint: .leading,
                        endPoint: .trailing))
                    .frame(height: 1)
            }

            VStack(spacing: 5) {
                Text("No items today")
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)
                Text("Use the buttons below to get started")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
    }

    // MARK: - Helpers

    func timeLabel(from isoString: String) -> String {
        formatTimeLabel(from: isoString)
    }

    @ViewBuilder
    func sourceIcon(for source: EntrySource) -> some View {
        entrySourceIcon(for: source)
    }
}
