import SwiftUI
import VisionKit
import UIKit

// MARK: - DataScanner Representable

@MainActor
private struct DataScannerViewControllerRepresentable: UIViewControllerRepresentable {
    let onScan: (String) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onScan: onScan)
    }

    func makeUIViewController(context: Context) -> DataScannerViewController {
        let scanner = DataScannerViewController(
            recognizedDataTypes: [.barcode()],
            qualityLevel: .balanced,
            recognizesMultipleItems: false,
            isHighFrameRateTrackingEnabled: false,
            isGuidanceEnabled: true,
            isHighlightingEnabled: true
        )
        scanner.delegate = context.coordinator
        try? scanner.startScanning()
        return scanner
    }

    func updateUIViewController(_ uiViewController: DataScannerViewController, context: Context) {}

    // MARK: Coordinator

    final class Coordinator: NSObject, DataScannerViewControllerDelegate, @unchecked Sendable {
        let onScan: (String) -> Void
        private var didScan = false

        init(onScan: @escaping (String) -> Void) {
            self.onScan = onScan
        }

        func dataScanner(
            _ dataScanner: DataScannerViewController,
            didAdd addedItems: [RecognizedItem],
            allItems: [RecognizedItem]
        ) {
            guard !didScan else { return }
            for item in addedItems {
                if case .barcode(let barcode) = item {
                    guard let value = barcode.payloadStringValue, !value.isEmpty else { continue }
                    didScan = true
                    let feedback = UIImpactFeedbackGenerator(style: .medium)
                    feedback.impactOccurred()
                    DispatchQueue.main.async {
                        self.onScan(value)
                    }
                    return
                }
            }
        }
    }
}

// MARK: - BarcodeScannerView

struct BarcodeScannerView: View {
    let onScan: (String) -> Void
    let onCancel: () -> Void
    let onManual: () -> Void

    var body: some View {
        Group {
            if DataScannerViewController.isSupported && DataScannerViewController.isAvailable {
                scannerBody
            } else {
                unavailableBody
            }
        }
    }

    private var scannerBody: some View {
        ZStack {
            DataScannerViewControllerRepresentable(onScan: onScan)
                .ignoresSafeArea()

            // Overlay controls
            VStack {
                // Top bar
                HStack {
                    Button(action: onCancel) {
                        HStack(spacing: 6) {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 14, weight: .semibold))
                            Text("Cancel")
                                .font(.strakkBody)
                        }
                        .foregroundStyle(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 10)
                        .background(.ultraThinMaterial, in: Capsule())
                    }
                    .accessibilityLabel("Cancel scan")

                    Spacer()
                }
                .padding(.horizontal, 20)
                .padding(.top, 60)

                Spacer()

                // Bottom action
                VStack(spacing: 12) {
                    Text("Point the camera at a barcode")
                        .font(.strakkCaption)
                        .foregroundStyle(.white.opacity(0.8))
                        .multilineTextAlignment(.center)

                    Button(action: onManual) {
                        Text("Enter manually")
                            .font(.strakkBodyBold)
                            .foregroundStyle(.white)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 14)
                            .background(.ultraThinMaterial, in: Capsule())
                    }
                    .accessibilityLabel("Enter barcode manually")
                }
                .padding(.bottom, 60)
            }
        }
    }

    private var unavailableBody: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            VStack(spacing: 20) {
                Image(systemName: "barcode.viewfinder")
                    .font(.system(size: 60))
                    .foregroundStyle(Color.strakkTextTertiary)

                Text("Scanner unavailable")
                    .font(.strakkHeading2)
                    .foregroundStyle(Color.strakkTextPrimary)

                Text("The barcode scanner is not available on this device.")
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)

                Button(action: onManual) {
                    Text("Enter manually")
                        .font(.strakkBodyBold)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color.strakkPrimary)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .padding(.horizontal, 40)
                .accessibilityLabel("Enter meal manually")
            }
        }
        .overlay(alignment: .topLeading) {
            Button(action: onCancel) {
                Image(systemName: "xmark")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(Color.strakkTextSecondary)
                    .frame(width: 48, height: 48)
            }
            .accessibilityLabel("Close")
            .padding(.top, 60)
            .padding(.leading, 12)
        }
    }
}
