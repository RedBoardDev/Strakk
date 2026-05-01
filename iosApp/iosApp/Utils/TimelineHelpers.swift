import SwiftUI
import shared

func formatTimeLabel(from isoString: String) -> String {
    let formats = ["yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss"]
    let df = DateFormatter()
    df.locale = Locale.current
    for fmt in formats {
        df.dateFormat = fmt
        if let date = df.date(from: isoString) {
            df.dateFormat = "HH:mm"
            return df.string(from: date)
        }
    }
    return ""
}

@ViewBuilder
func entrySourceIcon(for source: EntrySource) -> some View {
    switch source {
    case .photoai:
        Image(systemName: "camera.fill")
            .font(.system(size: 11))
            .foregroundStyle(Color.strakkTextTertiary)
    case .barcode:
        Image(systemName: "barcode.viewfinder")
            .font(.system(size: 11))
            .foregroundStyle(Color.strakkTextTertiary)
    case .manual:
        Image(systemName: "pencil")
            .font(.system(size: 11))
            .foregroundStyle(Color.strakkTextTertiary)
    case .textai:
        Image(systemName: "text.quote")
            .font(.system(size: 11))
            .foregroundStyle(Color.strakkTextTertiary)
    case .search, .frequent:
        Image(systemName: "magnifyingglass")
            .font(.system(size: 11))
            .foregroundStyle(Color.strakkTextTertiary)
    default:
        EmptyView()
    }
}

func entrySourceLabel(for source: EntrySource) -> String {
    switch source {
    case .photoai: return "Photo AI"
    case .barcode: return "Barcode"
    case .manual: return "Manual"
    case .textai: return "Text AI"
    case .search: return "Search"
    case .frequent: return "Frequent"
    default: return ""
    }
}
