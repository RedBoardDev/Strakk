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

            VStack {
                HStack {
                    Button(action: {
                        HapticEngine.light()
                        onCancel()
                    }, label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(.white)
                            .frame(width: 44, height: 44)
                            .background(.ultraThinMaterial, in: Circle())
                    })
                    .accessibilityLabel(Text("Close scanner"))

                    Spacer()
                }
                .padding(.horizontal, StrakkSpacing.lg)
                .padding(.top, StrakkSpacing.xxxl + StrakkSpacing.lg)

                Spacer()

                VStack(spacing: StrakkSpacing.sm) {
                    Text("Point the camera at a barcode")
                        .font(.strakkCaption)
                        .foregroundStyle(.white.opacity(0.8))
                        .multilineTextAlignment(.center)

                    Button(action: onManual) {
                        Text("Enter manually")
                            .font(.strakkBodyBold)
                            .foregroundStyle(.white)
                            .padding(.horizontal, StrakkSpacing.xl)
                            .padding(.vertical, StrakkSpacing.md)
                            .background(.ultraThinMaterial, in: Capsule())
                    }
                    .accessibilityLabel(Text("Enter barcode manually"))
                }
                .padding(.bottom, StrakkSpacing.xxxl + StrakkSpacing.lg)
            }
        }
    }

    private var unavailableBody: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            VStack(spacing: StrakkSpacing.lg) {
                StrakkEmptyState(
                    icon: "barcode.viewfinder",
                    title: "Scanner unavailable",
                    message: "The barcode scanner is not available on this device.",
                    actionTitle: "Enter manually",
                    action: onManual
                )
            }
        }
        .overlay(alignment: .topLeading) {
            StrakkCloseButton(action: onCancel)
                .padding(.top, StrakkSpacing.xxxl + StrakkSpacing.lg)
                .padding(.leading, StrakkSpacing.sm)
        }
    }
}
