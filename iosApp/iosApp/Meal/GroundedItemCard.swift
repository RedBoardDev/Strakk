import SwiftUI

struct GroundedItemCard: View {
    let item: GroundedItemData
    let onAdjustGrams: (Double) -> Void
    let onSwap: () -> Void
    let onEdit: () -> Void
    let onHide: () -> Void
    let onRestore: () -> Void
    let onRemove: () -> Void

    @State private var isExpanded: Bool = true
    @State private var currentGrams: Double

    init(
        item: GroundedItemData,
        onAdjustGrams: @escaping (Double) -> Void,
        onSwap: @escaping () -> Void,
        onEdit: @escaping () -> Void,
        onHide: @escaping () -> Void,
        onRestore: @escaping () -> Void,
        onRemove: @escaping () -> Void
    ) {
        self.item = item
        self.onAdjustGrams = onAdjustGrams
        self.onSwap = onSwap
        self.onEdit = onEdit
        self.onHide = onHide
        self.onRestore = onRestore
        self.onRemove = onRemove
        self._currentGrams = State(initialValue: item.grams)
    }

    var body: some View {
        VStack(spacing: 0) {
            header
                .padding(16)

            if isExpanded && !item.isHidden {
                Divider()
                    .background(Color.strakkDivider)
                    .padding(.horizontal, 16)

                expandedBody
                    .padding(16)
                    .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .opacity(item.isHidden ? 0.5 : 1.0)
        .swipeActions(edge: .leading, allowsFullSwipe: false) {
            if !item.isHidden && !item.isManuallyAdded {
                Button {
                    onHide()
                } label: {
                    Label("Hide", systemImage: "eye.slash")
                }
                .tint(Color.strakkWarning)
            }
        }
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            if item.isManuallyAdded && !item.isHidden {
                Button(role: .destructive) {
                    onRemove()
                } label: {
                    Label("Remove", systemImage: "trash")
                }
            }
        }
        .accessibilityLabel(item.isHidden ? "\(item.name), hidden" : item.name)
    }

    // MARK: - Header

    private var header: some View {
        HStack(spacing: 12) {
            sourceIcon
            Text(item.name)
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkTextPrimary)
                .lineLimit(1)
                .strikethrough(item.isHidden)
            Spacer()
            if item.isHidden {
                hiddenBadge
                Spacer().frame(width: 8)
                Button {
                    onRestore()
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "eye")
                            .font(.system(size: 12, weight: .semibold))
                        Text("Restore")
                            .font(.strakkCaptionBold)
                    }
                    .foregroundStyle(Color.strakkPrimary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.strakkSurface2)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .accessibilityLabel("Restore \(item.name)")
            } else {
                confidenceBadge
                chevron
            }
        }
        .contentShape(Rectangle())
        .onTapGesture {
            guard !item.isHidden else { return }
            withAnimation(.easeInOut(duration: 0.2)) {
                isExpanded.toggle()
            }
        }
    }

    private var sourceIcon: some View {
        let systemName: String = {
            if item.isManuallyAdded { return "pencil" }
            return item.matchSource != nil ? "camera.fill" : "questionmark.circle"
        }()
        return Image(systemName: systemName)
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(item.isManuallyAdded ? Color.strakkTextSecondary : Color.strakkPrimary)
            .frame(width: 34, height: 34)
            .background(Color.strakkSurface2)
            .clipShape(Circle())
    }

    private var confidenceBadge: some View {
        let (iconName, tint): (String, Color) = {
            if !item.isGrounded {
                return ("xmark.circle.fill", Color.strakkError)
            } else if item.aiConfidence >= 0.8 {
                return ("checkmark.circle.fill", Color.strakkPrimary)
            } else if item.aiConfidence >= 0.5 {
                return ("exclamationmark.triangle.fill", Color.strakkWarning)
            } else {
                return ("questionmark.circle.fill", Color.strakkTextTertiary)
            }
        }()
        return Image(systemName: iconName)
            .font(.system(size: 18))
            .foregroundStyle(tint)
            .accessibilityLabel(confidenceLabel)
    }

    private var hiddenBadge: some View {
        HStack(spacing: 4) {
            Image(systemName: "eye.slash")
                .font(.system(size: 12, weight: .semibold))
            Text("Hidden")
                .font(.strakkCaption)
        }
        .foregroundStyle(Color.strakkTextTertiary)
    }

    private var confidenceLabel: String {
        if !item.isGrounded { return "Not matched" }
        if item.aiConfidence >= 0.8 { return "High confidence" }
        if item.aiConfidence >= 0.5 { return "Medium confidence" }
        return "Low confidence"
    }

    private var chevron: some View {
        Image(systemName: "chevron.down")
            .font(.system(size: 13, weight: .semibold))
            .foregroundStyle(Color.strakkTextTertiary)
            .rotationEffect(.degrees(isExpanded ? 0 : -90))
            .animation(.easeInOut(duration: 0.2), value: isExpanded)
    }

    // MARK: - Expanded body

    private var expandedBody: some View {
        VStack(alignment: .leading, spacing: 14) {
            gramsRow
            macrosLine
            sourceInfo
            actionRow
        }
    }

    private var gramsRow: some View {
        HStack(spacing: 0) {
            Button {
                let newGrams = max(1, currentGrams - 10)
                currentGrams = newGrams
                onAdjustGrams(newGrams)
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
            } label: {
                Image(systemName: "minus.circle.fill")
                    .font(.system(size: 28))
                    .foregroundStyle(Color.strakkSurface2)
            }
            .frame(width: 44, height: 44)
            .accessibilityLabel("Decrease quantity")

            Text(String(format: "%.0fg", currentGrams))
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkTextPrimary)
                .monospacedDigit()
                .frame(minWidth: 64)
                .multilineTextAlignment(.center)

            Button {
                let newGrams = min(currentGrams + 10, 5000)
                currentGrams = newGrams
                onAdjustGrams(newGrams)
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
            } label: {
                Image(systemName: "plus.circle.fill")
                    .font(.system(size: 28))
                    .foregroundStyle(Color.strakkPrimary)
            }
            .frame(width: 44, height: 44)
            .accessibilityLabel("Increase quantity")

            Spacer()
        }
    }

    private var macrosLine: some View {
        HStack(spacing: 6) {
            Text(String(format: "%.0f kcal", item.kcal))
                .foregroundStyle(Color.strakkCalories)
            Text("·").foregroundStyle(Color.strakkTextTertiary)
            Text(String(format: "%.0fg P", item.protein))
                .foregroundStyle(Color.strakkProtein)
            Text("·").foregroundStyle(Color.strakkTextTertiary)
            Text(String(format: "%.0fg F", item.fat))
                .foregroundStyle(Color.strakkAccentYellow)
            Text("·").foregroundStyle(Color.strakkTextTertiary)
            Text(String(format: "%.0fg C", item.carbs))
                .foregroundStyle(Color.strakkAccentIndigo)
        }
        .font(.strakkCaption)
        .monospacedDigit()
        .lineLimit(1)
        .minimumScaleFactor(0.85)
    }

    private var sourceInfo: some View {
        VStack(alignment: .leading, spacing: 4) {
            if let source = item.matchSource {
                Text(source.uppercased())
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextSecondary)
            }
            if let matchName = item.matchName {
                Text(matchName)
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .lineLimit(1)
            }
        }
    }

    private var actionRow: some View {
        HStack(spacing: 10) {
            if !item.isManuallyAdded {
                Button {
                    onSwap()
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.triangle.2.circlepath")
                            .font(.system(size: 12, weight: .semibold))
                        Text("Change food")
                            .font(.strakkCaptionBold)
                    }
                    .foregroundStyle(Color.strakkTextSecondary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Color.strakkSurface2)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .accessibilityLabel("Change food for \(item.name)")
            }

            Button {
                onEdit()
            } label: {
                Image(systemName: "pencil")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Color.strakkTextSecondary)
                    .frame(width: 36, height: 36)
                    .background(Color.strakkSurface2)
                    .clipShape(Circle())
            }
            .accessibilityLabel("Edit \(item.name)")

            Spacer()

            if !item.isManuallyAdded {
                Button {
                    onHide()
                } label: {
                    Image(systemName: "eye.slash")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.strakkWarning)
                        .frame(width: 36, height: 36)
                        .background(Color.strakkSurface2)
                        .clipShape(Circle())
                }
                .accessibilityLabel("Hide \(item.name)")
            } else {
                Button(role: .destructive) {
                    onRemove()
                } label: {
                    Image(systemName: "trash")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Color.strakkError)
                        .frame(width: 36, height: 36)
                        .background(Color.strakkSurface2)
                        .clipShape(Circle())
                }
                .accessibilityLabel("Remove \(item.name)")
            }
        }
    }
}
