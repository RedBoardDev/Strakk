import SwiftUI
import shared

// MARK: - CheckInMeasurementsSection

struct CheckInMeasurementsSection: View {
    let checkIn: CheckInData
    let delta: CheckInDeltaData?

    var body: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("MEASUREMENTS")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(spacing: 0) {
                let rows: [(String, Double?, String, Double?)] = [
                    ("Poids", checkIn.weight, "kg", delta?.weight),
                    ("Épaules", checkIn.shoulders, "cm", delta?.shoulders),
                    ("Poitrine", checkIn.chest, "cm", delta?.chest),
                    ("Bras gauche", checkIn.armLeft, "cm", delta?.armLeft),
                    ("Bras droit", checkIn.armRight, "cm", delta?.armRight),
                    ("Taille", checkIn.waist, "cm", delta?.waist),
                    ("Hanches", checkIn.hips, "cm", delta?.hips),
                    ("Cuisse gauche", checkIn.thighLeft, "cm", delta?.thighLeft),
                    ("Cuisse droite", checkIn.thighRight, "cm", delta?.thighRight)
                ]

                ForEach(Array(rows.enumerated()), id: \.offset) { index, row in
                    measurementRow(label: row.0, value: row.1, unit: row.2, delta: row.3)
                    if index < rows.count - 1 {
                        Divider().background(Color.strakkDivider)
                    }
                }
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    @ViewBuilder
    private func measurementRow(label: String, value: Double?, unit: String, delta: Double?) -> some View {
        HStack(spacing: StrakkSpacing.xs) {
            Text(label)
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)

            if let delta {
                deltaView(delta)
            }

            if let value {
                Text("\(String(format: "%.1f", value)) \(unit)")
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .monospacedDigit()
            } else {
                Text("—")
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextTertiary)
            }
        }
        .frame(minHeight: 44)
    }

    @ViewBuilder
    private func deltaView(_ delta: Double) -> some View {
        if delta == 0 {
            Text("=")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
        } else if delta > 0 {
            Text("↑ +\(String(format: "%.1f", delta))")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)
        } else {
            Text("↓ \(String(format: "%.1f", delta))")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)
        }
    }
}
