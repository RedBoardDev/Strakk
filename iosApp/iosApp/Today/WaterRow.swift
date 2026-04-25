import SwiftUI
import UIKit

private let kDefaultAmountML: Int = 250
private let kMinAmountML: Int = 50
private let kMaxAmountML: Int = 2000

// MARK: - WaterRow — ligne unique compacte
//
// 💧  1.5 L / 5 L                              [−]  [+]
//
// Tap court : ajoute / retire la quantité par défaut (250 mL).
// Long press : ouvre une modal pour saisir une quantité custom dans le mode du bouton pressé.

struct WaterRow: View {
    let summary: DailySummaryData
    let onAdd: (Int) -> Void
    let onRemove: (Int) -> Void

    @State private var dialogOpen: Bool = false
    @State private var dialogText: String = String(kDefaultAmountML)

    private var totalL: Double { Double(summary.totalWater) / 1000.0 }
    private var goalL: Double? { summary.waterGoal.map { Double($0) / 1000.0 } }
    private var canRemove: Bool { summary.totalWater > 0 }

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "drop.fill")
                .font(.system(size: 18))
                .foregroundStyle(Color.strakkWater)

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
            .accessibilityLabel("Retirer 250 mL d'eau")

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
            .accessibilityLabel("Ajouter 250 mL d'eau")

            // 3ème bouton — quantité personnalisée
            WaterIconButton(
                systemName: "slider.horizontal.3",
                background: Color.strakkSurface3,
                foreground: Color.strakkTextPrimary,
                enabled: true,
                onTap: {
                    dialogText = String(kDefaultAmountML)
                    dialogOpen = true
                }
            )
            .accessibilityLabel("Quantité personnalisée")
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .alert("Quantité personnalisée", isPresented: $dialogOpen) {
            TextField("mL", text: $dialogText)
                .keyboardType(.numberPad)
            Button("+ Ajouter") {
                guard let amount = Int(dialogText),
                      (kMinAmountML...kMaxAmountML).contains(amount) else {
                    dialogOpen = false
                    return
                }
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                onAdd(amount)
                dialogOpen = false
            }
            Button("− Retirer") {
                guard let amount = Int(dialogText),
                      (kMinAmountML...kMaxAmountML).contains(amount) else {
                    dialogOpen = false
                    return
                }
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                onRemove(amount)
                dialogOpen = false
            }
            .disabled(!canRemove)
            Button("Annuler", role: .cancel) {
                dialogOpen = false
            }
        } message: {
            Text("Quantité en mL (entre \(kMinAmountML) et \(kMaxAmountML))")
        }
    }

    private var headerText: String {
        let total = String(format: "%.1f L", totalL)
        if let g = goalL {
            return "\(total) / \(String(format: "%.1f L", g))"
        }
        return total
    }
}
// MARK: - WaterIconButton (tap + long-press)

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
