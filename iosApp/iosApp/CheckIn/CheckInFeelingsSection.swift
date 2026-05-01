import SwiftUI
import shared

// MARK: - CheckInFeelingsSection

struct CheckInFeelingsSection: View {
    let tags: [String]
    let mentalFeeling: String?
    let physicalFeeling: String?

    private static let positiveFeelingIds: Set<String> = FeelingTagLabels.positiveIds
    private static let negativeFeelingIds: Set<String> = FeelingTagLabels.negativeIds

    var body: some View {
        let positiveTags = tags.filter { Self.positiveFeelingIds.contains($0) }
        let negativeTags = tags.filter { Self.negativeFeelingIds.contains($0) }
        let hasMental = mentalFeeling.map { !$0.isEmpty } ?? false
        let hasPhysical = physicalFeeling.map { !$0.isEmpty } ?? false
        let isEmpty = tags.isEmpty && !hasMental && !hasPhysical

        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("FEELINGS")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(alignment: .leading, spacing: StrakkSpacing.sm) {
                if isEmpty {
                    Text("Aucun ressenti renseigné")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextTertiary)
                } else {
                    if !positiveTags.isEmpty {
                        VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
                            Text("Positive feelings")
                                .font(.strakkOverline)
                                .foregroundStyle(Color.strakkTextTertiary)
                            FlowLayout(spacing: StrakkSpacing.xs) {
                                ForEach(positiveTags, id: \.self) { tag in
                                    Text(FeelingTagLabels.label(for: tag))
                                        .font(.strakkCaptionBold)
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, StrakkSpacing.sm)
                                        .padding(.vertical, StrakkSpacing.xxs)
                                        .background(Color.strakkSuccess)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }

                    if !negativeTags.isEmpty {
                        VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
                            Text("Negative feelings")
                                .font(.strakkOverline)
                                .foregroundStyle(Color.strakkTextTertiary)
                            FlowLayout(spacing: StrakkSpacing.xs) {
                                ForEach(negativeTags, id: \.self) { tag in
                                    Text(FeelingTagLabels.label(for: tag))
                                        .font(.strakkCaptionBold)
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, StrakkSpacing.sm)
                                        .padding(.vertical, StrakkSpacing.xxs)
                                        .background(Color.strakkError)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }

                    if hasMental, let mentalFeeling {
                        feelingCard(header: "RESSENTI MENTAL", text: mentalFeeling)
                    }

                    if hasPhysical, let physicalFeeling {
                        feelingCard(header: "RESSENTI PHYSIQUE", text: physicalFeeling)
                    }
                }
            }
            .padding(StrakkSpacing.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    private func feelingCard(header: String, text: String) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
            Text(header)
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
            Text(text)
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
        }
        .padding(StrakkSpacing.sm)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.strakkSurface2)
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }
}
