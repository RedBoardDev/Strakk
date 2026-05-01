import SwiftUI

// MARK: - WizardStepMeasurementsView

struct WizardStepMeasurementsView: View {
    let weight: String
    let shoulders: String
    let chest: String
    let armLeft: String
    let armRight: String
    let waist: String
    let hips: String
    let thighLeft: String
    let thighRight: String
    let delta: CheckInDeltaData?
    let onWeightChanged: (String) -> Void
    let onShouldersChanged: (String) -> Void
    let onChestChanged: (String) -> Void
    let onArmLeftChanged: (String) -> Void
    let onArmRightChanged: (String) -> Void
    let onWaistChanged: (String) -> Void
    let onHipsChanged: (String) -> Void
    let onThighLeftChanged: (String) -> Void
    let onThighRightChanged: (String) -> Void

    @State private var draftWeight: String
    @State private var draftShoulders: String
    @State private var draftChest: String
    @State private var draftArmLeft: String
    @State private var draftArmRight: String
    @State private var draftWaist: String
    @State private var draftHips: String
    @State private var draftThighLeft: String
    @State private var draftThighRight: String

    init(
        weight: String,
        shoulders: String,
        chest: String,
        armLeft: String,
        armRight: String,
        waist: String,
        hips: String,
        thighLeft: String,
        thighRight: String,
        delta: CheckInDeltaData?,
        onWeightChanged: @escaping (String) -> Void,
        onShouldersChanged: @escaping (String) -> Void,
        onChestChanged: @escaping (String) -> Void,
        onArmLeftChanged: @escaping (String) -> Void,
        onArmRightChanged: @escaping (String) -> Void,
        onWaistChanged: @escaping (String) -> Void,
        onHipsChanged: @escaping (String) -> Void,
        onThighLeftChanged: @escaping (String) -> Void,
        onThighRightChanged: @escaping (String) -> Void
    ) {
        self.weight = weight
        self.shoulders = shoulders
        self.chest = chest
        self.armLeft = armLeft
        self.armRight = armRight
        self.waist = waist
        self.hips = hips
        self.thighLeft = thighLeft
        self.thighRight = thighRight
        self.delta = delta
        self.onWeightChanged = onWeightChanged
        self.onShouldersChanged = onShouldersChanged
        self.onChestChanged = onChestChanged
        self.onArmLeftChanged = onArmLeftChanged
        self.onArmRightChanged = onArmRightChanged
        self.onWaistChanged = onWaistChanged
        self.onHipsChanged = onHipsChanged
        self.onThighLeftChanged = onThighLeftChanged
        self.onThighRightChanged = onThighRightChanged
        _draftWeight = State(initialValue: weight)
        _draftShoulders = State(initialValue: shoulders)
        _draftChest = State(initialValue: chest)
        _draftArmLeft = State(initialValue: armLeft)
        _draftArmRight = State(initialValue: armRight)
        _draftWaist = State(initialValue: waist)
        _draftHips = State(initialValue: hips)
        _draftThighLeft = State(initialValue: thighLeft)
        _draftThighRight = State(initialValue: thighRight)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: StrakkSpacing.xl) {
                // Poids
                measurementSection(title: "WEIGHT") {
                    measurementRow(
                        label: "Weight (kg)",
                        value: $draftWeight,
                        deltaValue: delta?.weight,
                        onChange: onWeightChanged
                    )
                }

                // Haut du corps
                measurementSection(title: "UPPER BODY") {
                    VStack(spacing: StrakkSpacing.sm) {
                        measurementRow(
                            label: "Shoulders (cm)",
                            value: $draftShoulders,
                            deltaValue: delta?.shoulders,
                            onChange: onShouldersChanged
                        )
                        Divider().background(Color.strakkDivider)
                        measurementRow(
                            label: "Chest (cm)",
                            value: $draftChest,
                            deltaValue: delta?.chest,
                            onChange: onChestChanged
                        )
                        Divider().background(Color.strakkDivider)
                        measurementRow(
                            label: "Left arm (cm)",
                            value: $draftArmLeft,
                            deltaValue: delta?.armLeft,
                            onChange: onArmLeftChanged
                        )
                        Divider().background(Color.strakkDivider)
                        measurementRow(
                            label: "Right arm (cm)",
                            value: $draftArmRight,
                            deltaValue: delta?.armRight,
                            onChange: onArmRightChanged
                        )
                    }
                }

                // Bas du corps
                measurementSection(title: "LOWER BODY") {
                    VStack(spacing: StrakkSpacing.sm) {
                        measurementRow(
                            label: "Waist (cm)",
                            value: $draftWaist,
                            deltaValue: delta?.waist,
                            onChange: onWaistChanged
                        )
                        Divider().background(Color.strakkDivider)
                        measurementRow(
                            label: "Hips (cm)",
                            value: $draftHips,
                            deltaValue: delta?.hips,
                            onChange: onHipsChanged
                        )
                        Divider().background(Color.strakkDivider)
                        measurementRow(
                            label: "Left thigh (cm)",
                            value: $draftThighLeft,
                            deltaValue: delta?.thighLeft,
                            onChange: onThighLeftChanged
                        )
                        Divider().background(Color.strakkDivider)
                        measurementRow(
                            label: "Right thigh (cm)",
                            value: $draftThighRight,
                            deltaValue: delta?.thighRight,
                            onChange: onThighRightChanged
                        )
                    }
                }

                Spacer().frame(height: StrakkSpacing.xl)
            }
            .padding(.horizontal, StrakkSpacing.lg)
            .padding(.vertical, StrakkSpacing.xl)
        }
    }

    // MARK: - Section builder

    @ViewBuilder
    private func measurementSection(title: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text(title)
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            VStack(spacing: 0) {
                content()
            }
            .padding(StrakkSpacing.md)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }

    // MARK: - Row

    @ViewBuilder
    private func measurementRow(
        label: String,
        value: Binding<String>,
        deltaValue: Double?,
        onChange: @escaping (String) -> Void
    ) -> some View {
        HStack(spacing: StrakkSpacing.xs) {
            Text(label)
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)

            // Delta indicator
            if let deltaValue {
                deltaView(value: deltaValue)
            }

            // Input field
            TextField("—", text: Binding(
                get: { value.wrappedValue },
                set: {
                    value.wrappedValue = $0
                    onChange($0)
                }
            ))
            .font(.strakkBodyBold)
            .foregroundStyle(Color.strakkTextPrimary)
            .multilineTextAlignment(.trailing)
            .keyboardType(.decimalPad)
            .frame(width: 80, height: 48)
            .padding(.horizontal, StrakkSpacing.xs)
            .background(Color.strakkSurface2)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .accessibilityLabel(label)
        }
    }

    // MARK: - Delta view

    @ViewBuilder
    private func deltaView(value: Double) -> some View {
        if value == 0 {
            Text("=")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
                .monospacedDigit()
        } else if value > 0 {
            Text("↑ +\(String(format: "%.1f", value))")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)
                .monospacedDigit()
        } else {
            Text("↓ \(String(format: "%.1f", value))")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)
                .monospacedDigit()
        }
    }
}

// MARK: - Preview

#Preview {
    ZStack {
        Color.strakkBackground.ignoresSafeArea()
        WizardStepMeasurementsView(
            weight: "78.5",
            shoulders: "120",
            chest: "100",
            armLeft: "35",
            armRight: "35.5",
            waist: "82",
            hips: "98",
            thighLeft: "58",
            thighRight: "57.5",
            delta: CheckInDeltaData(
                weight: -0.5,
                shoulders: 0.0,
                chest: 1.0,
                armLeft: nil,
                armRight: nil,
                waist: -1.0,
                hips: 0.5,
                thighLeft: nil,
                thighRight: nil
            ),
            onWeightChanged: { _ in },
            onShouldersChanged: { _ in },
            onChestChanged: { _ in },
            onArmLeftChanged: { _ in },
            onArmRightChanged: { _ in },
            onWaistChanged: { _ in },
            onHipsChanged: { _ in },
            onThighLeftChanged: { _ in },
            onThighRightChanged: { _ in }
        )
    }
}
