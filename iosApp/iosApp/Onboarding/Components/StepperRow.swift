import SwiftUI

struct StepperRow: View {
    let label: String
    let value: Int
    let unit: String
    let step: Int
    let range: ClosedRange<Int>
    let onDecrement: () -> Void
    let onIncrement: () -> Void

    // Long-press acceleration
    @State private var longPressTimer: Timer?
    @State private var accelerationCount: Int = 0

    var body: some View {
        HStack {
            Text(label)
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextPrimary)

            Spacer()

            HStack(spacing: StrakkSpacing.sm) {
                decrementButton
                    .frame(width: 48, height: 48)

                Text("\(value)")
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .monospacedDigit()
                    .frame(minWidth: 48, alignment: .center)

                Text(unit)
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextSecondary)

                incrementButton
                    .frame(width: 48, height: 48)
            }
        }
        .padding(.vertical, StrakkSpacing.xs)
    }

    private var decrementButton: some View {
        Button {
            if value - step >= range.lowerBound {
                onDecrement()
            }
        } label: {
            Image(systemName: "minus")
                .font(.body.weight(.semibold))
                .foregroundStyle(value <= range.lowerBound ? Color.strakkTextDisabled : Color.strakkTextPrimary)
                .frame(width: 48, height: 48)
                .background(Color.strakkSurface2)
                .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
        .disabled(value <= range.lowerBound)
        .simultaneousGesture(
            LongPressGesture(minimumDuration: 0.4)
                .onEnded { _ in
                    startLongPress(increment: false)
                }
        )
        .onLongPressGesture(
            minimumDuration: 0.4,
            pressing: { isPressing in
                if !isPressing { stopLongPress() }
            },
            perform: {}
        )
        .accessibilityLabel("Diminuer \(label)")
    }

    private var incrementButton: some View {
        Button {
            if value + step <= range.upperBound {
                onIncrement()
            }
        } label: {
            Image(systemName: "plus")
                .font(.body.weight(.semibold))
                .foregroundStyle(value >= range.upperBound ? Color.strakkTextDisabled : Color.strakkPrimary)
                .frame(width: 48, height: 48)
                .background(Color.strakkAccentOrangeFaint)
                .clipShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
        .disabled(value >= range.upperBound)
        .simultaneousGesture(
            LongPressGesture(minimumDuration: 0.4)
                .onEnded { _ in
                    startLongPress(increment: true)
                }
        )
        .onLongPressGesture(
            minimumDuration: 0.4,
            pressing: { isPressing in
                if !isPressing { stopLongPress() }
            },
            perform: {}
        )
        .accessibilityLabel("Augmenter \(label)")
    }

    private func startLongPress(increment: Bool) {
        accelerationCount = 0
        longPressTimer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { _ in
            // Timer fires on the main RunLoop — safe to assume MainActor isolation
            MainActor.assumeIsolated {
                accelerationCount += 1
                let effectiveStep = accelerationCount > 20 ? step * 5 : step
                if increment {
                    if value + effectiveStep <= range.upperBound { onIncrement() }
                } else {
                    if value - effectiveStep >= range.lowerBound { onDecrement() }
                }
            }
        }
    }

    private func stopLongPress() {
        longPressTimer?.invalidate()
        longPressTimer = nil
        accelerationCount = 0
    }
}
