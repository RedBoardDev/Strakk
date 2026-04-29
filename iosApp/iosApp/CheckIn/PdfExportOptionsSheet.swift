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
                        optionSection("GÉNÉRAL") {
                            toggleRow("Photos", isOn: $options.includePhotos)
                            Divider().background(Color.strakkDivider).padding(.leading, 16)
                            toggleRow("Mesures corporelles", isOn: $options.includeMeasurements)
                            Divider().background(Color.strakkDivider).padding(.leading, 16)
                            toggleRow("Ressentis", isOn: $options.includeFeelings)
                        }

                        optionSection("NUTRITION") {
                            toggleRow("Calories", isOn: $options.includeCalories)
                            Divider().background(Color.strakkDivider).padding(.leading, 16)
                            toggleRow("Protéines", isOn: $options.includeProtein)
                            Divider().background(Color.strakkDivider).padding(.leading, 16)
                            toggleRow("Glucides", isOn: $options.includeCarbs)
                            Divider().background(Color.strakkDivider).padding(.leading, 16)
                            toggleRow("Lipides", isOn: $options.includeFat)
                            Divider().background(Color.strakkDivider).padding(.leading, 16)
                            toggleRow("Eau", isOn: $options.includeWater)
                            Divider().background(Color.strakkDivider).padding(.leading, 16)
                            toggleRow("Moyennes", isOn: $options.includeAverages)
                            Divider().background(Color.strakkDivider).padding(.leading, 16)
                            toggleRow("Par jour", isOn: $options.includeDailyData)
                            Divider().background(Color.strakkDivider).padding(.leading, 16)
                            toggleRow("Résumé IA", isOn: $options.includeAiSummary)
                        }

                        Button(action: onExport) {
                            HStack(spacing: StrakkSpacing.xs) {
                                if isGenerating {
                                    ProgressView()
                                        .tint(.white)
                                        .scaleEffect(0.8)
                                } else {
                                    Image(systemName: "doc.fill")
                                        .font(.system(size: 14, weight: .semibold))
                                }
                                Text(isGenerating ? "Génération…" : "Générer le PDF")
                                    .font(.strakkBodyBold)
                            }
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(isGenerating ? Color.strakkPrimary.opacity(0.6) : Color.strakkPrimary)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .disabled(isGenerating)
                        .padding(.horizontal, 20)
                        .padding(.top, 24)
                        .padding(.bottom, 32)
                        .accessibilityLabel(isGenerating ? "Génération du PDF en cours" : "Générer le PDF")
                    }
                }
            }
            .navigationTitle("Contenu du PDF")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler", action: onCancel)
                        .foregroundStyle(Color.strakkTextSecondary)
                        .disabled(isGenerating)
                }
            }
        }
    }

    // MARK: - Subviews

    private func optionSection<Content: View>(
        _ title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title)
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .kerning(1.0)
                .padding(.horizontal, 20)
                .padding(.top, 24)
                .padding(.bottom, 8)

            VStack(spacing: 0) {
                content()
            }
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .padding(.horizontal, 20)
        }
    }

    private func toggleRow(_ label: String, isOn: Binding<Bool>) -> some View {
        Toggle(isOn: isOn) {
            Text(label)
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextPrimary)
        }
        .tint(Color.strakkPrimary)
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }
}
