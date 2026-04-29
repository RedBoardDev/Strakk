import SwiftUI

struct MacroBreakdownItem: Identifiable {
    let id = UUID()
    let label: String
    let value: String
    let color: Color
}

struct MacroBreakdown: View {
    let items: [MacroBreakdownItem]

    var body: some View {
        VStack(spacing: 0) {
            ForEach(items) { item in
                if item.id != items.first?.id {
                    Divider().background(Color.strakkDivider)
                }
                MacroRow(label: item.label, value: item.value, color: item.color)
            }
        }
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
    }
}
