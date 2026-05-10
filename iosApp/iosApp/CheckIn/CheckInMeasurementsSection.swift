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
                let rows: [(LocalizedStringKey, Double?, String, Double?)] = [
                    ("Weight", checkIn.weight, "kg", delta?.weight),
                    ("Shoulders", checkIn.shoulders, "cm", delta?.shoulders),
                    ("Chest", checkIn.chest, "cm", delta?.chest),
                    ("Left arm", checkIn.armLeft, "cm", delta?.armLeft),
                    ("Right arm", checkIn.armRight, "cm", delta?.armRight),
                    ("Waist", checkIn.waist, "cm", delta?.waist),
                    ("Hips", checkIn.hips, "cm", delta?.hips),
                    ("Left thigh", checkIn.thighLeft, "cm", delta?.thighLeft),
                    ("Right thigh", checkIn.thighRight, "cm", delta?.thighRight)
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
    private func measurementRow(label: LocalizedStringKey, value: Double?, unit: String, delta: Double?) -> some View {
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
