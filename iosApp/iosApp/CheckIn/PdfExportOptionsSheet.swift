import SwiftUI

// MARK: - Swift-side options model

struct PdfExportConfig {
    var includePhotos: Bool = true
    var includeMeasurements: Bool = true
    var includeFeelings: Bool = true
    var includeProtein: Bool = true
    var includeCalories: Bool = true
    var includeCarbs: Bool = true
    var includeFat: Bool = true
    var includeWater: Bool = true
    var includeAverages: Bool = true
    var includeDailyData: Bool = true
    var includeAiSummary: Bool = true
}

// MARK: - Sheet

struct PdfExportOptionsSheet: View {
    @Binding var options: PdfExportConfig
    let isGenerating: Bool
    let onExport: () -> Void
    let onCancel: () -> Void

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        optionSection("GENERAL") {
                            toggleRow("Photos", isOn: $options.includePhotos)
                            sectionDivider
                            toggleRow("Body measurements", isOn: $options.includeMeasurements)
                            sectionDivider
                            toggleRow("Feelings", isOn: $options.includeFeelings)
                        }

                        optionSection("NUTRITION") {
                            toggleRow("Calories", isOn: $options.includeCalories)
                            sectionDivider
                            toggleRow("Protein", isOn: $options.includeProtein)
                            sectionDivider
                            toggleRow("Carbs", isOn: $options.includeCarbs)
                            sectionDivider
                            toggleRow("Fat", isOn: $options.includeFat)
                            sectionDivider
                            toggleRow("Water", isOn: $options.includeWater)
                            sectionDivider
                            toggleRow("Averages", isOn: $options.includeAverages)
                            sectionDivider
                            toggleRow("Per day", isOn: $options.includeDailyData)
                            sectionDivider
                            toggleRow("AI Summary", isOn: $options.includeAiSummary)
                        }

                        StrakkPrimaryButton(
                            title: isGenerating ? "Generating…" : "Generate PDF",
                            action: onExport,
                            icon: isGenerating ? nil : "doc.fill",
                            isEnabled: !isGenerating
                        )
                        .padding(.horizontal, StrakkSpacing.lg)
                        .padding(.top, StrakkSpacing.xl)
                        .padding(.bottom, StrakkSpacing.xxl)
                        .accessibilityLabel(Text(isGenerating ? "Generating PDF" : "Generate PDF"))
                    }
                }
            }
            .navigationTitle("PDF content")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                StrakkCloseToolbarItem(action: onCancel)
            }
        }
    }

    // MARK: - Subviews

    private func optionSection<Content: View>(
        _ title: LocalizedStringKey,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .kerning(1.0)
                .padding(.horizontal, StrakkSpacing.lg)
                .padding(.top, StrakkSpacing.xl)
                .padding(.bottom, StrakkSpacing.xs)

            VStack(spacing: 0) {
                content()
            }
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
            .padding(.horizontal, StrakkSpacing.lg)
        }
    }

    private func toggleRow(_ label: LocalizedStringKey, isOn: Binding<Bool>) -> some View {
        Toggle(isOn: isOn) {
            Text(label)
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextPrimary)
        }
        .tint(Color.strakkPrimary)
        .padding(.horizontal, StrakkSpacing.md)
        .padding(.vertical, StrakkSpacing.sm)
    }

    private var sectionDivider: some View {
        Divider()
            .background(Color.strakkDivider)
            .padding(.leading, StrakkSpacing.md)
    }
}
