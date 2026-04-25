import UIKit

enum ImageEncoder {
    /// Resizes `image` so that its longest side does not exceed `maxDimension`,
    /// then encodes it as a JPEG base64 string.
    static func base64JPEG(from image: UIImage, maxDimension: CGFloat = 1024, quality: CGFloat = 0.85) -> String? {
        let resized = resized(image, maxDimension: maxDimension)
        guard let data = resized.jpegData(compressionQuality: quality) else { return nil }
        return data.base64EncodedString()
    }

    /// Resizes `image` so that its longest side does not exceed `maxDimension`,
    /// then encodes it as JPEG `Data`.
    static func jpegData(from image: UIImage, maxDimension: CGFloat = 1024, quality: CGFloat = 0.85) -> Data? {
        let resized = resized(image, maxDimension: maxDimension)
        return resized.jpegData(compressionQuality: quality)
    }

    private static func resized(_ image: UIImage, maxDimension: CGFloat) -> UIImage {
        let size = image.size
        let longest = max(size.width, size.height)
        guard longest > maxDimension else { return image }

        let scale = maxDimension / longest
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)

        let renderer = UIGraphicsImageRenderer(size: newSize)
        return renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: newSize))
        }
    }
}
