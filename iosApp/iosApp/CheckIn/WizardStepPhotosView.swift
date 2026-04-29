import SwiftUI
import PhotosUI

// MARK: - WizardStepPhotosView

struct WizardStepPhotosView: View {
    let photos: [WizardPhotoData]
    let onAddPhoto: (Data) -> Void
    let onRemovePhoto: (String) -> Void

    @State private var showSourcePicker = false
    @State private var showCamera = false
    @State private var showPhotoLibrary = false
    @State private var photoPickerItem: PhotosPickerItem?
    @State private var photoToDelete: WizardPhotoData?
    @State private var showDeleteConfirm = false

    private let maxPhotos = 3
    private let columns = Array(repeating: GridItem(.flexible(), spacing: StrakkSpacing.xs), count: 3)

    var body: some View {
        ZStack {
            ScrollView {
                VStack(alignment: .leading, spacing: StrakkSpacing.xl) {
                    photoGrid

                    Text("\(photos.count)/\(maxPhotos) photos")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextSecondary)
                        .frame(maxWidth: .infinity, alignment: .center)

                    Text("Prends tes photos dans les mêmes conditions chaque semaine pour un meilleur suivi.")
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity, alignment: .center)

                    Spacer().frame(height: StrakkSpacing.xl)
                }
                .padding(.horizontal, StrakkSpacing.lg)
                .padding(.vertical, StrakkSpacing.xl)
            }

            if showSourcePicker {
                sourcePickerOverlay
            }
        }
        .animation(.spring(response: 0.28, dampingFraction: 0.88), value: showSourcePicker)
        .fullScreenCover(isPresented: $showCamera) {
            CameraPicker(sourceType: .camera) { image in
                showCamera = false
                if let image, let data = ImageEncoder.jpegData(from: image, maxDimension: 1280, quality: 0.75) {
                    onAddPhoto(data)
                }
            }
            .ignoresSafeArea()
        }
        .photosPicker(
            isPresented: $showPhotoLibrary,
            selection: $photoPickerItem,
            matching: .images,
            photoLibrary: .shared()
        )
        .onChange(of: photoPickerItem) { _, item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self) {
                    if let image = UIImage(data: data),
                       let compressed = ImageEncoder.jpegData(from: image, maxDimension: 1280, quality: 0.75) {
                        onAddPhoto(compressed)
                    }
                }
                photoPickerItem = nil
            }
        }
        .confirmationDialog("Supprimer cette photo ?", isPresented: $showDeleteConfirm, titleVisibility: .visible) {
            Button("Supprimer", role: .destructive) {
                if let photo = photoToDelete {
                    onRemovePhoto(photo.id)
                }
                photoToDelete = nil
            }
            Button("Annuler", role: .cancel) {
                photoToDelete = nil
            }
        }
    }

    // MARK: - Source picker

    private var sourcePickerOverlay: some View {
        ZStack {
            Color.black.opacity(0.45)
                .ignoresSafeArea()
                .onTapGesture { showSourcePicker = false }

            VStack(alignment: .leading, spacing: StrakkSpacing.md) {
                Text("Ajouter une photo")
                    .font(.strakkHeading3)
                    .foregroundStyle(Color.strakkTextPrimary)

                Text("Choisis une photo récente ou prends-en une maintenant.")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextSecondary)

                VStack(spacing: StrakkSpacing.xs) {
                    sourceButton(
                        title: "Appareil photo",
                        systemImage: "camera.fill",
                        action: {
                            showSourcePicker = false
                            showCamera = true
                        }
                    )

                    sourceButton(
                        title: "Galerie",
                        systemImage: "photo.on.rectangle",
                        action: {
                            showSourcePicker = false
                            showPhotoLibrary = true
                        }
                    )
                }

                Button("Annuler") {
                    showSourcePicker = false
                }
                .font(.strakkBody)
                .foregroundStyle(Color.strakkTextSecondary)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
            }
            .padding(StrakkSpacing.lg)
            .background(Color.strakkSurface1)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .overlay(
                RoundedRectangle(cornerRadius: 18)
                    .strokeBorder(Color.strakkDivider, lineWidth: 1)
            )
            .padding(.horizontal, StrakkSpacing.lg)
            .transition(.scale(scale: 0.96).combined(with: .opacity))
        }
    }

    private func sourceButton(title: String, systemImage: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: StrakkSpacing.sm) {
                Image(systemName: systemImage)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(Color.strakkPrimary)
                    .frame(width: 24)

                Text(title)
                    .font(.strakkBodyBold)
                    .foregroundStyle(Color.strakkTextPrimary)

                Spacer()
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .padding(.horizontal, StrakkSpacing.sm)
            .background(Color.strakkSurface2)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }

    // MARK: - Grid

    private var photoGrid: some View {
        LazyVGrid(columns: columns, spacing: StrakkSpacing.xs) {
            ForEach(photos) { photo in
                photoCell(photo: photo)
            }

            // Add button slot
            if photos.count < maxPhotos {
                addPhotoButton
            }
        }
    }

    // MARK: - Photo cell

    @ViewBuilder
    private func photoCell(photo: WizardPhotoData) -> some View {
        GeometryReader { geo in
            Group {
                switch photo {
                case .local(_, let imageData):
                    if let uiImage = UIImage(data: imageData) {
                        Image(uiImage: uiImage)
                            .resizable()
                            .scaledToFill()
                    } else {
                        placeholderCell
                    }

                case .remote(_, _, let signedUrl):
                    if let url = URL(string: signedUrl), !signedUrl.isEmpty {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let img):
                                img.resizable().scaledToFill()
                            case .failure:
                                placeholderCell
                            case .empty:
                                ProgressView()
                                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                                    .background(Color.strakkSurface2)
                            @unknown default:
                                placeholderCell
                            }
                        }
                    } else {
                        placeholderCell
                    }
                }
            }
            .frame(width: geo.size.width, height: geo.size.width)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .contentShape(RoundedRectangle(cornerRadius: 12))
        }
        .aspectRatio(1, contentMode: .fit)
        .onLongPressGesture {
            photoToDelete = photo
            showDeleteConfirm = true
        }
        .accessibilityLabel("Photo. Appuyer longuement pour supprimer.")
    }

    // MARK: - Placeholder

    private var placeholderCell: some View {
        Color.strakkSurface2
            .overlay(
                Image(systemName: "photo")
                    .font(.system(size: 24))
                    .foregroundStyle(Color.strakkTextTertiary)
            )
    }

    // MARK: - Add button

    private var addPhotoButton: some View {
        Button {
            showSourcePicker = true
        } label: {
            GeometryReader { geo in
                Color.strakkSurface2
                    .overlay(
                        Image(systemName: "plus")
                            .font(.system(size: 28, weight: .medium))
                            .foregroundStyle(Color.strakkTextSecondary)
                    )
                    .frame(width: geo.size.width, height: geo.size.width)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .aspectRatio(1, contentMode: .fit)
        }
        .accessibilityLabel("Ajouter une photo")
    }
}

// MARK: - Preview

#Preview {
    ZStack {
        Color.strakkBackground.ignoresSafeArea()
        WizardStepPhotosView(
            photos: [.local(id: "preview-1", imageData: Data())],
            onAddPhoto: { _ in },
            onRemovePhoto: { _ in }
        )
    }
}
