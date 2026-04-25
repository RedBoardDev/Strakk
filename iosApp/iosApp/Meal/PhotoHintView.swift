import SwiftUI
import PhotosUI
import UIKit

struct PhotoHintView: View {
    let onAdd: (String, String?) -> Void
    let onCancel: () -> Void

    @State private var selectedImage: UIImage?
    @State private var hintText: String = ""
    @State private var showCamera: Bool = false
    @State private var photoPickerItem: PhotosPickerItem?
    @State private var isCompressing: Bool = false

    private var canAdd: Bool { selectedImage != nil && !isCompressing }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        // Photo preview / placeholder
                        photoArea
                            .frame(height: 240)

                        // Camera / Gallery buttons
                        HStack(spacing: 12) {
                            Button {
                                showCamera = true
                            } label: {
                                Label("Caméra", systemImage: "camera.fill")
                                    .font(.strakkBodyBold)
                                    .foregroundStyle(Color.strakkTextPrimary)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 48)
                                    .background(Color.strakkSurface1)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                            .accessibilityLabel("Prendre une photo")

                            PhotosPicker(
                                selection: $photoPickerItem,
                                matching: .images
                            ) {
                                Label("Galerie", systemImage: "photo.on.rectangle")
                                    .font(.strakkBodyBold)
                                    .foregroundStyle(Color.strakkTextPrimary)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 48)
                                    .background(Color.strakkSurface1)
                                    .clipShape(RoundedRectangle(cornerRadius: 12))
                            }
                            .accessibilityLabel("Choisir depuis la galerie")
                        }

                        // Hint field
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Hint (optionnel)")
                                .font(.strakkCaptionBold)
                                .foregroundStyle(Color.strakkTextSecondary)

                            TextField(
                                "ex : portion pour 2, poulet riz...",
                                text: $hintText
                            )
                            .font(.strakkBody)
                            .foregroundStyle(Color.strakkTextPrimary)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 12)
                            .background(Color.strakkSurface1)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .strokeBorder(Color.strakkDivider, lineWidth: 1)
                            )
                            .onChange(of: hintText) { _, v in
                                if v.count > 150 {
                                    hintText = String(v.prefix(150))
                                }
                            }

                            Text("\(hintText.count)/150")
                                .font(.strakkCaption)
                                .foregroundStyle(Color.strakkTextTertiary)
                                .frame(maxWidth: .infinity, alignment: .trailing)
                        }

                        // CTA
                        Button {
                            guard let img = selectedImage else { return }
                            isCompressing = true
                            Task.detached(priority: .userInitiated) {
                                let base64 = compressImage(img)
                                await MainActor.run {
                                    isCompressing = false
                                    if let b64 = base64 {
                                        onAdd(b64, hintText.trimmingCharacters(in: .whitespaces).isEmpty ? nil : hintText)
                                    }
                                }
                            }
                        } label: {
                            HStack {
                                if isCompressing {
                                    ProgressView().tint(.white).scaleEffect(0.8)
                                } else {
                                    Text("Ajouter au repas")
                                        .font(.strakkBodyBold)
                                        .foregroundStyle(.white)
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(canAdd ? Color.strakkPrimary : Color.strakkSurface2)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .disabled(!canAdd)
                        .accessibilityLabel("Ajouter la photo au repas")
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                    .padding(.bottom, 32)
                }
            }
            .navigationTitle("Ajouter une photo")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { onCancel() }
                        .foregroundStyle(Color.strakkTextSecondary)
                }
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .fullScreenCover(isPresented: $showCamera) {
            CameraPicker(sourceType: .camera) { image in
                showCamera = false
                selectedImage = image
            }
            .ignoresSafeArea()
        }
        .onChange(of: photoPickerItem) { _, item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self),
                   let img = UIImage(data: data) {
                    selectedImage = img
                }
                photoPickerItem = nil
            }
        }
    }

    // MARK: - Photo area

    @ViewBuilder
    private var photoArea: some View {
        if let img = selectedImage {
            Image(uiImage: img)
                .resizable()
                .scaledToFill()
                .frame(maxWidth: .infinity, maxHeight: 240)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(alignment: .topTrailing) {
                    Button {
                        selectedImage = nil
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 24))
                            .foregroundStyle(.white)
                            .background(Color.black.opacity(0.4), in: Circle())
                    }
                    .padding(10)
                    .accessibilityLabel("Supprimer la photo")
                }
        } else {
            ZStack {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.strakkSurface1)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .strokeBorder(
                                style: StrokeStyle(lineWidth: 1.5, dash: [6])
                            )
                            .foregroundStyle(Color.strakkDivider)
                    )
                VStack(spacing: 8) {
                    Image(systemName: "camera.fill")
                        .font(.system(size: 36))
                        .foregroundStyle(Color.strakkTextTertiary)
                    Text("Sélectionnez une photo")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextTertiary)
                }
            }
        }
    }
}

// MARK: - Image compression

/// Compresses to JPEG ≤ 300KB, max 1024px on longest side.
private func compressImage(_ image: UIImage) -> String? {
    let maxDimension: CGFloat = 1024
    let size = image.size
    var newSize = size

    if size.width > maxDimension || size.height > maxDimension {
        let ratio = min(maxDimension / size.width, maxDimension / size.height)
        newSize = CGSize(width: size.width * ratio, height: size.height * ratio)
    }

    let renderer = UIGraphicsImageRenderer(size: newSize)
    let resized = renderer.image { _ in
        image.draw(in: CGRect(origin: .zero, size: newSize))
    }

    // Try quality 0.8 first, then lower if needed
    var quality: CGFloat = 0.8
    var data = resized.jpegData(compressionQuality: quality)

    while let d = data, d.count > 300_000, quality > 0.1 {
        quality -= 0.1
        data = resized.jpegData(compressionQuality: quality)
    }

    guard let finalData = data else { return nil }
    return finalData.base64EncodedString()
}
