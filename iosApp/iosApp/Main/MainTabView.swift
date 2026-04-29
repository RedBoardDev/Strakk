import SwiftUI
import shared

struct MainTabView: View {
    private var navigator = AppNavigator.shared

    var body: some View {
        TabView(selection: Binding(
            get: { navigator.selectedTab },
            set: { navigator.selectedTab = $0 }
        )) {
            TodayView()
                .tabItem { Label("Today", systemImage: "house.fill") }
                .tag(0)

            CalendarView()
                .tabItem { Label("Calendar", systemImage: "calendar") }
                .tag(1)

            CheckInListView()
                .tabItem { Label("Bilans", systemImage: "chart.bar.doc.horizontal") }
                .tag(2)

            SettingsView()
                .tabItem { Label("Settings", systemImage: "gearshape.fill") }
                .tag(3)
        }
        .tint(Color.strakkPrimary)
    }
}
