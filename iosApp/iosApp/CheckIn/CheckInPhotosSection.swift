import SwiftUI
import shared

// MARK: - CheckInPhotosSection

struct CheckInPhotosSection: View {
    let photos: [CheckInPhotoData]
    let photoUrls: [String: String]

    var body: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("PHOTOS")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            if photos.isEmpty {
                emptyValue("Aucune photo")
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: StrakkSpacing.sm) {
                        ForEach(photos.sorted(by: { $0.position < $1.position })) { photo in
                            photoThumbnail(photo: photo, signedUrl: photoUrls[photo.id])
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func photoThumbnail(photo: CheckInPhotoData, signedUrl: String?) -> some View {
        ZStack {
            Color.strakkSurface2

            if let urlString = signedUrl, let url = URL(string: urlString) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    case .failure:
                        Image(systemName: "camera.fill")
                            .font(.system(size: 24))
                            .foregroundStyle(Color.strakkTextTertiary)
                    case .empty:
                        ProgressView()
                            .tint(Color.strakkPrimary)
                    @unknown default:
                        EmptyView()
                    }
                }
            } else {
                Image(systemName: "camera.fill")
                    .font(.system(size: 24))
                    .foregroundStyle(Color.strakkTextTertiary)
            }
        }
        .frame(width: 120, height: 160)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .accessibilityLabel("Photo \(photo.position + 1)")
    }

    private func emptyValue(_ text: String) -> some View {
        Text(text)
            .font(.strakkBody)
            .foregroundStyle(Color.strakkTextTertiary)
    }
}
