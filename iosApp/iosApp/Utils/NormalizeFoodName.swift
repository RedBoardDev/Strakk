import Foundation

/// Mirror of the Kotlin `normalize(text:)` used in `FavoritesRepositoryImpl`,
/// `ToggleFavoriteFoodUseCase`, and the search VM. Lowercased + diacritics
/// stripped for FR/EN. Must produce the same output as the backend so we can
/// resolve favorite-state by name client-side.
func normalizeFoodName(_ raw: String?) -> String {
    guard let raw, !raw.isEmpty else { return "" }
    let lowered = raw.lowercased()
    var result = ""
    result.reserveCapacity(lowered.count)
    for ch in lowered {
        switch ch {
        case "à", "â", "ä": result += "a"
        case "é", "è", "ê", "ë": result += "e"
        case "î", "ï": result += "i"
        case "ô", "ö": result += "o"
        case "ù", "û", "ü": result += "u"
        case "ç": result += "c"
        default: result.append(ch)
        }
    }
    return result
}
