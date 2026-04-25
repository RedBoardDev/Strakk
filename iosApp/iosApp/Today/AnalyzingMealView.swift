import SwiftUI

struct AnalyzingMealView: View {
    let capturedImage: UIImage?
    let analyzingText: String?
    let onSkip: () -> Void
    let onCancel: () -> Void

    @State private var scanLineOffset: CGFloat = 0
    @State private var phaseIndex: Int = 0
    @State private var iconRotation: Double = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private let phases: [(icon: String, label: String)] = [
        ("eye", "Identifying ingredients..."),
        ("scalemass", "Estimating portions..."),
        ("function", "Calculating macros..."),
    ]

    var body: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            VStack(spacing: 0) {
                // Cancel button row
                HStack {
                    Button(action: onCancel) {
                        Image(systemName: "xmark")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundStyle(Color.strakkTextSecondary)
                            .frame(width: 48, height: 48)
                    }
                    .accessibilityLabel("Cancel scan")
                    Spacer()
                }
                .padding(.horizontal, 8)
                .padding(.top, 8)

                // Main visual area: image or text
                GeometryReader { geo in
                    ZStack(alignment: .top) {
                        if let image = capturedImage {
                            // Photo mode
                            Image(uiImage: image)
                                .resizable()
                                .scaledToFill()
                                .frame(width: geo.size.width, height: geo.size.height)
                                .clipped()
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                        } else if let text = analyzingText {
                            // Text mode — show text in a styled container
                            ZStack {
                                RoundedRectangle(cornerRadius: 16)
                                    .fill(Color.strakkSurface1)

                                ScrollView {
                                    Text(text)
                                        .font(.strakkBody)
                                        .foregroundStyle(Color.strakkTextPrimary)
                                        .multilineTextAlignment(.leading)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .padding(20)
                                }
                            }
                            .frame(width: geo.size.width, height: geo.size.height)
                        } else {
                            // Fallback — empty placeholder
                            RoundedRectangle(cornerRadius: 16)
                                .fill(Color.strakkSurface1)
                                .frame(width: geo.size.width, height: geo.size.height)
                        }

                        // Already-scanned overlay (progressive)
                        if !reduceMotion {
                            let progress = max(0, min(1, scanLineOffset / geo.size.height))
                            Color.strakkPrimary
                                .opacity(0.08 * Double(progress))
                                .frame(height: geo.size.height * CGFloat(progress))
                                .frame(maxHeight: .infinity, alignment: .top)
                                .clipShape(
                                    UnevenRoundedRectangle(
                                        topLeadingRadius: 16,
                                        bottomLeadingRadius: 0,
                                        bottomTrailingRadius: 0,
                                        topTrailingRadius: 16
                                    )
                                )
                        }

                        // Scan line
                        if !reduceMotion {
                            ZStack {
                                Rectangle()
                                    .fill(Color.strakkPrimary.opacity(0.20))
                                    .frame(height: 8)
                                    .blur(radius: 4)

                                Rectangle()
                                    .fill(Color.strakkPrimary)
                                    .frame(height: 2)
                            }
                            .offset(y: scanLineOffset)
                            .frame(maxHeight: .infinity, alignment: .top)
                        }
                    }
                    .frame(width: geo.size.width, height: geo.size.height)
                    .clipped()
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .onAppear {
                        guard !reduceMotion else { return }
                        animateScanLine(in: geo.size.height)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 20)
                .padding(.top, 32)
                .aspectRatio(4/3, contentMode: .fit)

                // Status text
                VStack(spacing: 8) {
                    HStack(spacing: 8) {
                        Image(systemName: phases[phaseIndex].icon)
                            .font(.system(size: 15))
                            .foregroundStyle(Color.strakkTextSecondary)
                            .rotationEffect(.degrees(iconRotation))

                        Text(phases[phaseIndex].label)
                            .font(.strakkBody)
                            .foregroundStyle(Color.strakkTextSecondary)
                    }
                    .transition(.opacity)
                    .animation(.easeInOut(duration: 0.3), value: phaseIndex)
                }
                .padding(.top, 28)

                Spacer()

                // Skip button
                VStack(spacing: 4) {
                    Button(action: onSkip) {
                        Text("Skip & add manually")
                            .font(.strakkBody)
                            .foregroundStyle(Color.strakkPrimary)
                    }
                    .accessibilityLabel("Skip AI analysis and add meal manually")

                    Text("AI not working? No problem.")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }
                .padding(.bottom, 48)
            }
        }
        .task {
            await runPhaseCycling()
        }
    }

    // MARK: - Animations

    private func animateScanLine(in height: CGFloat) {
        withAnimation(
            .easeInOut(duration: 2.5).repeatForever(autoreverses: false)
        ) {
            scanLineOffset = height
        }
    }

    @MainActor
    private func runPhaseCycling() async {
        while !Task.isCancelled {
            try? await Task.sleep(for: .seconds(3))
            guard !Task.isCancelled else { break }
            let next = min(phaseIndex + 1, phases.count - 1)
            if next != phaseIndex {
                withAnimation(.easeInOut(duration: 0.3)) {
                    phaseIndex = next
                }
                if !reduceMotion {
                    withAnimation(.linear(duration: 0.8)) {
                        iconRotation += 360
                    }
                }
            }
        }
    }
}
