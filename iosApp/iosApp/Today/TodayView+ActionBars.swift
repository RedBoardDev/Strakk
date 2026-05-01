import SwiftUI
import shared

extension TodayView {

    // MARK: - Sticky action buttons

    @ViewBuilder
    func stickyActionButtons() -> some View {
        HStack(spacing: 12) {
            Button {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                shouldOpenDraftAfterStart = true
                draftViewModel.didStartDraft = false
                draftViewModel.onEvent(MealDraftEventStartDraft(initialName: nil, date: nil))
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "fork.knife")
                        .font(.system(size: 16, weight: .semibold))
                    Text("Meal")
                        .font(.strakkBodyBold)
                }
                .foregroundStyle(Color.strakkTextPrimary)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color.strakkSurface2)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .accessibilityLabel("New meal")

            Button {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                addPickerMode = .quickAdd
            } label: {
                HStack(spacing: 8) {
                    Image(systemName: "bolt.fill")
                        .font(.system(size: 16, weight: .semibold))
                    Text("Quick")
                        .font(.strakkBodyBold)
                }
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color.strakkPrimary)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .accessibilityLabel("Quick add")
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 8)
        .padding(.top, 4)
        .background(Color.strakkBackground.opacity(0.95))
    }

    // MARK: - Floating draft bar

    func floatingDraftBar(draft: ActiveDraftData) -> some View {
        let isEmpty = draft.resolvedCount == 0 && draft.pendingCount == 0

        return HStack(spacing: 12) {
            Button {
                navigationPath.append(TodayDestination.mealDraft)
            } label: {
                VStack(alignment: .leading, spacing: 2) {
                    Text(draft.name)
                        .font(.strakkBodyBold)
                        .foregroundStyle(.white)
                        .lineLimit(1)
                    if !isEmpty {
                        Text(draftSubtitle(draft: draft))
                            .font(.strakkCaption)
                            .foregroundStyle(.white.opacity(0.75))
                    }
                }
            }
            .buttonStyle(.plain)

            Spacer()

            Button {
                addPickerMode = .draft
            } label: {
                Text("+ Add")
                    .font(.strakkCaptionBold)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(.white.opacity(0.2))
                    .clipShape(Capsule())
            }
            .accessibilityLabel("Add item to current meal")

            Button {
                if isEmpty {
                    draftViewModel.onEvent(MealDraftEventDiscard.shared)
                } else {
                    draftViewModel.onEvent(MealDraftEventProcess.shared)
                }
            } label: {
                Text(isEmpty ? "Cancel" : "Finish")
                    .font(.strakkCaptionBold)
                    .foregroundStyle(isEmpty ? .white : Color.strakkPrimary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(isEmpty ? .white.opacity(0.2) : .white)
                    .clipShape(Capsule())
            }
            .accessibilityLabel(isEmpty ? "Cancel empty meal" : "Finish meal")
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.strakkPrimary)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal, 16)
        .padding(.bottom, 8)
        .shadow(color: .black.opacity(0.15), radius: 8, x: 0, y: -2)
    }

    // MARK: - Helpers

    func draftSubtitle(draft: ActiveDraftData) -> String {
        var parts: [String] = []
        if draft.resolvedCount > 0 {
            parts.append("\(draft.resolvedCount) item\(draft.resolvedCount > 1 ? "s" : "")")
        }
        if draft.pendingCount > 0 {
            parts.append("\(draft.pendingCount) pending")
        }
        parts.append(String(format: "%.0f kcal", draft.totalCalories))
        return parts.joined(separator: " · ")
    }
}
