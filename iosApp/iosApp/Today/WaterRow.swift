import SwiftUI
import UIKit

private let kDefaultAmountML: Int = 250
private let kStepAmountML: Int = 50
private let kMinAmountML: Int = 50
// Matches NutritionDefaults.MAX_WATER_ENTRY_ML in shared/
private let kMaxAmountML: Int = 2000

// MARK: - WaterRow — ligne unique compacte
//
// 💧  1.5 L / 5 L                              [−]  [+]  [⚙]
//
// Tap court : ajoute / retire la quantité par défaut (250 mL).
// Bouton ⚙ : ouvre une sheet compacte pour choisir une quantité custom.

struct WaterRow: View {
    let summary: DailySummaryData
    let onAdd: (Int) -> Void
    let onRemove: (Int) -> Void

    @State private var showCustomSheet: Bool = false
    @State private var dialogAmount: Int = kDefaultAmountML

    private var totalL: Double { Double(summary.totalWater) / 1000.0 }
    private var goalL: Double? { summary.waterGoal.map { Double($0) / 1000.0 } }
    private var canRemove: Bool { summary.totalWater > 0 }

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color.strakkSurface3)
                    .frame(width: 36, height: 36)
                Image(systemName: "drop.fill")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color.strakkWater)
            }

            Text(headerText)
                .font(.system(size: 18, weight: .bold))
                .monospacedDigit()
                .contentTransition(.numericText())
                .foregroundStyle(Color.strakkTextPrimary)

            Spacer()

            // − bouton
            WaterIconButton(
                systemName: "minus",
                background: Color.strakkSurface3,
                foreground: canRemove ? Color.strakkTextPrimary : Color.strakkTextTertiary,
                enabled: canRemove,
                onTap: {
                    if canRemove {
                        UIImpactFeedbackGenerator(style: .light).impactOccurred()
                        onRemove(kDefaultAmountML)
                    }
                }
            )
            .accessibilityLabel("Remove 250 mL of water")

            // + bouton
            WaterIconButton(
                systemName: "plus",
                background: Color.strakkWater.opacity(0.18),
                foreground: Color.strakkWater,
                enabled: true,
                onTap: {
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    onAdd(kDefaultAmountML)
                }
            )
            .accessibilityLabel("Add 250 mL of water")

            // Custom amount
            WaterIconButton(
                systemName: "slider.horizontal.3",
                background: Color.strakkSurface3,
                foreground: Color.strakkTextPrimary,
                enabled: true,
                onTap: {
                    dialogAmount = kDefaultAmountML
                    showCustomSheet = true
                }
            )
            .accessibilityLabel("Custom amount")
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .sheet(isPresented: $showCustomSheet) {
            customAmountSheet
        }
    }

    // MARK: - Custom amount sheet

    private var customAmountSheet: some View {
        VStack(spacing: 28) {
            Text("Custom amount")
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.top, 24)

            // Stepper row
            HStack(spacing: 28) {
                Button {
                    let next = dialogAmount - kStepAmountML
                    dialogAmount = max(kMinAmountML, next)
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                } label: {
                    Image(systemName: "minus.circle.fill")
                        .font(.system(size: 38))
                        .foregroundStyle(dialogAmount <= kMinAmountML ? Color.strakkSurface3 : Color.strakkSurface2)
                }
                .disabled(dialogAmount <= kMinAmountML)
                .accessibilityLabel("Decrease amount")

                Text("\(dialogAmount) mL")
                    .font(.system(size: 28, weight: .bold))
                    .monospacedDigit()
                    .foregroundStyle(Color.strakkTextPrimary)
                    .frame(minWidth: 110)
                    .multilineTextAlignment(.center)
                    .contentTransition(.numericText())

                Button {
                    let next = dialogAmount + kStepAmountML
                    dialogAmount = min(kMaxAmountML, next)
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: 38))
                        .foregroundStyle(dialogAmount >= kMaxAmountML ? Color.strakkSurface3 : Color.strakkWater)
                }
                .disabled(dialogAmount >= kMaxAmountML)
                .accessibilityLabel("Increase amount")
            }

            // Actions row
            HStack(spacing: 12) {
                Button("Cancel") {
                    showCustomSheet = false
                }
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)

                Spacer()

                if canRemove {
                    Button("Remove") {
                        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                        onRemove(dialogAmount)
                        showCustomSheet = false
                    }
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.strakkSurface2)
                    .clipShape(Capsule())
                    .accessibilityLabel("Remove \(dialogAmount) mL of water")
                }

                Button("Add") {
                    UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                    onAdd(dialogAmount)
                    showCustomSheet = false
                }
                .font(.strakkBodyBold)
                .foregroundStyle(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(Color.strakkWater)
                .clipShape(Capsule())
                .accessibilityLabel("Add \(dialogAmount) mL of water")
            }
            .padding(.horizontal, 24)

            Spacer()
        }
        .presentationDetents([.height(200)])
        .presentationDragIndicator(.visible)
        .presentationBackground(Color.strakkSurface1)
    }

    private var headerText: String {
        let total = String(format: "%.1f L", totalL)
        if let g = goalL {
            return "\(total) / \(String(format: "%.1f L", g))"
        }
        return total
    }
}

// MARK: - WaterIconButton

private struct WaterIconButton: View {
    let systemName: String
    let background: Color
    let foreground: Color
    let enabled: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Image(systemName: systemName)
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(foreground)
                .frame(width: 40, height: 40)
                .background(background)
                .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}
