import SwiftUI
import UniformTypeIdentifiers
import shared

struct HevyExportFlow: View {
    let onDismiss: () -> Void

    @State private var viewModel = HevyExportViewModelWrapper()
    @State private var showPdfPicker = true
    @State private var pulseScale: CGFloat = 1.0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                Group {
                    switch viewModel.state {
                    case .idle:
                        idleBody
                    case .parsing:
                        parsingBody
                    case .sessionList(let programName, let sessions):
                        sessionListBody(programName: programName, sessions: sessions)
                    case .exporting(let sessionName):
                        exportingBody(sessionName: sessionName)
                    case .done(let result):
                        doneBody(result: result)
                    }
                }
            }
            .navigationTitle("Export to Hevy")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onDismiss() }
                        .foregroundStyle(Color.strakkTextSecondary)
                }
            }
            .toolbarBackground(Color.strakkBackground, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
        }
        .fileImporter(
            isPresented: $showPdfPicker,
            allowedContentTypes: [UTType.pdf]
        ) { result in
            handlePdfResult(result)
        }
        .onChange(of: viewModel.shouldDismiss) { _, should in
            if should { onDismiss() }
        }
        .alert("Error", isPresented: errorBinding) {
            Button("OK") { viewModel.errorMessage = nil }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
        .alert("Hevy API Key Required", isPresented: $viewModel.requiresApiKey) {
            Button("Go to Settings") {
                onDismiss()
                AppNavigator.shared.selectedTab = 3
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Add your Hevy API key in Settings to export workouts.")
        }
    }

    // MARK: - Idle

    private var idleBody: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: "doc.fill")
                .font(.system(size: 48))
                .foregroundStyle(Color.strakkTextTertiary)
            Text("Select a PDF to get started")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
            Spacer()
        }
        .frame(maxWidth: .infinity)
        .onAppear {
            showPdfPicker = true
        }
    }

    // MARK: - Parsing

    private var parsingBody: some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: "dumbbell.fill")
                .font(.system(size: 40))
                .foregroundStyle(Color.strakkPrimary)
                .scaleEffect(pulseScale)
                .animation(
                    reduceMotion ? nil : .easeInOut(duration: 1.0).repeatForever(autoreverses: true),
                    value: pulseScale
                )
                .onAppear {
                    guard !reduceMotion else { return }
                    pulseScale = 1.15
                }

            ProgressView()
                .tint(Color.strakkPrimary)

            Text("Analyzing workout program...")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Session List

    private func sessionListBody(programName: String, sessions: [WorkoutSessionData]) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 8) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(programName)
                        .font(.strakkHeading2)
                        .foregroundStyle(Color.strakkTextPrimary)

                    Text("Select a session to export")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextSecondary)
                }
                .padding(.bottom, 8)

                ForEach(sessions) { session in
                    Button {
                        viewModel.onEvent(HevyExportEventOnSessionSelected(sessionIndex: Int32(session.id)))
                    } label: {
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(session.name)
                                    .font(.strakkBodyBold)
                                    .foregroundStyle(Color.strakkTextPrimary)

                                if !session.sectionNames.isEmpty {
                                    Text(session.sectionNames.joined(separator: " · "))
                                        .font(.strakkCaption)
                                        .foregroundStyle(Color.strakkTextSecondary)
                                        .lineLimit(1)
                                }
                            }

                            Spacer()

                            VStack(alignment: .trailing, spacing: 2) {
                                Text("\(session.exerciseCount)")
                                    .font(.strakkCaptionBold)
                                    .foregroundStyle(Color.strakkTextPrimary)
                                Text("exercises")
                                    .font(.strakkCaption)
                                    .foregroundStyle(Color.strakkTextTertiary)
                            }

                            Image(systemName: "chevron.right")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(Color.strakkTextTertiary)
                        }
                        .padding(16)
                        .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: 12))
                    }
                    .accessibilityLabel("Export session \(session.name)")
                }
            }
            .padding(20)
        }
    }

    // MARK: - Exporting

    private func exportingBody(sessionName: String) -> some View {
        VStack(spacing: 24) {
            Spacer()
            Image(systemName: "dumbbell.fill")
                .font(.system(size: 40))
                .foregroundStyle(Color.strakkPrimary)
                .scaleEffect(pulseScale)
                .animation(
                    reduceMotion ? nil : .easeInOut(duration: 1.0).repeatForever(autoreverses: true),
                    value: pulseScale
                )
                .onAppear {
                    guard !reduceMotion else { return }
                    pulseScale = 1.15
                }

            ProgressView()
                .tint(Color.strakkPrimary)

            VStack(spacing: 4) {
                Text("Exporting to Hevy...")
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextSecondary)
                Text(sessionName)
                    .font(.strakkCaptionBold)
                    .foregroundStyle(Color.strakkTextTertiary)
            }
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Done

    private func doneBody(result: ExportResultData) -> some View {
        VStack(spacing: 0) {
            Spacer()

            VStack(spacing: 16) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 64))
                    .foregroundStyle(Color.strakkSuccess)

                Text("Routine Created!")
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextPrimary)

                Text(result.routineTitle)
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .multilineTextAlignment(.center)

                HStack(spacing: 12) {
                    statPill(value: result.exercisesMatched, label: "matched")
                    statPill(value: result.exercisesCreated, label: "created")
                }
            }

            Spacer()

            VStack(spacing: 12) {
                Button {
                    viewModel.onEvent(HevyExportEventOnExportAnother.shared)
                } label: {
                    Text("Export Another")
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkPrimary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: 12))
                }
                .accessibilityLabel("Export another workout program")

                Button {
                    onDismiss()
                } label: {
                    Text("Done")
                        .font(.strakkBodyBold)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color.strakkPrimary, in: RoundedRectangle(cornerRadius: 12))
                }
                .accessibilityLabel("Done, close export")
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 32)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Helpers

    private func statPill(value: Int, label: String) -> some View {
        HStack(spacing: 4) {
            Text("\(value)")
                .font(.strakkCaptionBold)
                .foregroundStyle(Color.strakkTextPrimary)
            Text(label)
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Color.strakkSurface1, in: Capsule())
    }

    private func handlePdfResult(_ result: Result<URL, Error>) {
        switch result {
        case .success(let url):
            guard url.startAccessingSecurityScopedResource() else { return }
            defer { url.stopAccessingSecurityScopedResource() }
            guard let data = try? Data(contentsOf: url) else { return }
            let base64 = data.base64EncodedString()
            viewModel.onEvent(HevyExportEventOnPdfSelected(pdfBase64: base64))
        case .failure:
            onDismiss()
        }
    }

    private var errorBinding: Binding<Bool> {
        Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.errorMessage = nil } }
        )
    }
}
