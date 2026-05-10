import SwiftUI
import PhotosUI
import UIKit

// MARK: - PhotoHintView
//
// Headless content for the "capturing" state of `PhotoMealView`. Hosting is
// the parent's responsibility (NavigationStack, toolbar, sheet detents).

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
        ScrollView {
            VStack(spacing: StrakkSpacing.lg) {
                photoArea
                    .frame(height: 240)

                HStack(spacing: StrakkSpacing.sm) {
                    Button {
                        showCamera = true
                    } label: {
                        Label("Camera", systemImage: "camera.fill")
                            .font(.strakkBodyBold)
                            .foregroundStyle(Color.strakkTextPrimary)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(Color.strakkSurface1)
                            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
                    }
                    .accessibilityLabel(Text("Take a photo"))

                    PhotosPicker(
                        selection: $photoPickerItem,
                        matching: .images
                    ) {
                        Label("Library", systemImage: "photo.on.rectangle")
                            .font(.strakkBodyBold)
                            .foregroundStyle(Color.strakkTextPrimary)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .background(Color.strakkSurface1)
                            .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
                    }
                    .accessibilityLabel(Text("Pick from library"))
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text("Hint (optional)")
                        .font(.strakkCaptionBold)
                        .foregroundStyle(Color.strakkTextSecondary)

                    TextField(
                        "Portion for two, chicken with rice…",
                        text: $hintText
                    )
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .padding(.horizontal, StrakkSpacing.sm)
                    .padding(.vertical, StrakkSpacing.sm)
                    .background(Color.strakkSurface1)
                    .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
                    .overlay(
                        RoundedRectangle(cornerRadius: StrakkRadius.sm)
                            .strokeBorder(Color.strakkBorderFaint, lineWidth: 1)
                    )
                    .onChange(of: hintText) { _, value in
                        if value.count > 150 {
                            hintText = String(value.prefix(150))
                        }
                    }

                    Text("\(hintText.count)/150")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }

                StrakkPrimaryButton(
                    title: isCompressing ? "Adding…" : "Add to meal",
                    action: handleSubmit,
                    isEnabled: canAdd
                )
                .accessibilityLabel(Text("Add the photo to the meal"))
            }
            .padding(.horizontal, StrakkSpacing.lg)
            .padding(.top, StrakkSpacing.md)
            .padding(.bottom, StrakkSpacing.xxl)
        }
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

    private func handleSubmit() {
        guard let image = selectedImage else { return }
        isCompressing = true
        Task.detached(priority: .userInitiated) {
            let base64 = compressImage(image)
            await MainActor.run {
                isCompressing = false
                if let value = base64 {
                    let trimmed = hintText.trimmingCharacters(in: .whitespaces)
                    onAdd(value, trimmed.isEmpty ? nil : trimmed)
                }
            }
        }
    }

    @ViewBuilder
    private var photoArea: some View {
        if let img = selectedImage {
            Image(uiImage: img)
                .resizable()
                .scaledToFill()
                .frame(maxWidth: .infinity, maxHeight: 240)
                .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
                .overlay(alignment: .topTrailing) {
                    Button {
                        selectedImage = nil
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 24))
                            .foregroundStyle(.white)
                            .background(Color.black.opacity(0.4), in: Circle())
                    }
                    .padding(StrakkSpacing.xs)
                    .accessibilityLabel(Text("Remove photo"))
                }
        } else {
            ZStack {
                RoundedRectangle(cornerRadius: StrakkRadius.sm)
                    .fill(Color.strakkSurface1)
                    .overlay(
                        RoundedRectangle(cornerRadius: StrakkRadius.sm)
                            .strokeBorder(
                                style: StrokeStyle(lineWidth: 1.5, dash: [6])
                            )
                            .foregroundStyle(Color.strakkBorderSubtle)
                    )
                VStack(spacing: 8) {
                    Image(systemName: "camera.fill")
                        .font(.system(size: 36))
                        .foregroundStyle(Color.strakkTextTertiary)
                    Text("Select a photo")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextTertiary)
                }
            }
        }
    }
}

// MARK: - Image compression
//
// Compresses to JPEG ≤ 300KB, max 1024px on the longest side.

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

    var quality: CGFloat = 0.8
    var data = resized.jpegData(compressionQuality: quality)

    while let payload = data, payload.count > 300_000, quality > 0.1 {
        quality -= 0.1
        data = resized.jpegData(compressionQuality: quality)
    }

    guard let finalData = data else { return nil }
    return finalData.base64EncodedString()
}
