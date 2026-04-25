import SwiftUI
import shared

struct CalendarView: View {
    @State private var viewModel = CalendarViewModelWrapper()

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 4), count: 7)
    private let weekdaySymbols = ["L", "M", "M", "J", "V", "S", "D"]

    var body: some View {
        NavigationStack {
            ZStack {
                Color.strakkBackground.ignoresSafeArea()
                calendarContent
            }
            .navigationTitle("Calendrier")
            .navigationBarTitleDisplayMode(.large)
        }
        .alert("Erreur", isPresented: Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.errorMessage = nil } }
        )) {
            Button("OK") { viewModel.errorMessage = nil }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
    }

    @ViewBuilder
    private var calendarContent: some View {
        switch viewModel.state {
        case .loading:
            ProgressView()
                .tint(Color.strakkPrimary)

        case .ready(let year, let month, let activeDays, let selectedDay, let dayDetail):
            ScrollView {
                VStack(spacing: 0) {
                    monthNavigator(year: year, month: month)
                        .padding(.horizontal, 20)
                        .padding(.top, 16)

                    weekdayHeader
                        .padding(.horizontal, 20)
                        .padding(.top, 16)
                        .padding(.bottom, 8)

                    calendarGrid(
                        year: year,
                        month: month,
                        activeDays: activeDays,
                        selectedDay: selectedDay
                    )
                    .padding(.horizontal, 20)

                    Spacer().frame(height: 32 + 49)
                }
            }
            .sheet(
                isPresented: Binding(
                    get: { dayDetail != nil },
                    set: { if !$0 { viewModel.onEvent(CalendarEventDismissDay.shared) } }
                )
            ) {
                if let detail = dayDetail {
                    DayDetailSheet(
                        detail: detail,
                        onDismiss: {
                            viewModel.onEvent(CalendarEventDismissDay.shared)
                        },
                        onAddMeal: {
                            viewModel.onEvent(CalendarEventDismissDay.shared)
                            // Meal entry from calendar is not implemented in v2 — user goes to Today to add.
                        }
                    )
                    .presentationDetents([.large])
                    .presentationDragIndicator(.visible)
                }
            }
        }
    }

    // MARK: - Month navigator

    private func monthNavigator(year: Int, month: Int) -> some View {
        HStack {
            Button {
                viewModel.onEvent(CalendarEventSelectMonth(year: Int32(previousYear(year: year, month: month)), month: Int32(previousMonth(month: month))))
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(Color.strakkTextPrimary)
                    .frame(width: 44, height: 44)
            }
            .accessibilityLabel("Mois précédent")

            Spacer()

            Text(monthLabel(year: year, month: month))
                .font(.strakkHeading2)
                .foregroundStyle(Color.strakkTextPrimary)

            Spacer()

            Button {
                viewModel.onEvent(CalendarEventSelectMonth(year: Int32(nextYear(year: year, month: month)), month: Int32(nextMonth(month: month))))
            } label: {
                Image(systemName: "chevron.right")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(Color.strakkTextPrimary)
                    .frame(width: 44, height: 44)
            }
            .accessibilityLabel("Mois suivant")
        }
    }

    // MARK: - Weekday header

    private var weekdayHeader: some View {
        LazyVGrid(columns: columns, spacing: 4) {
            ForEach(weekdaySymbols, id: \.self) { symbol in
                Text(symbol)
                    .font(.strakkOverline)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    // MARK: - Calendar grid

    private func calendarGrid(
        year: Int,
        month: Int,
        activeDays: Set<String>,
        selectedDay: String?
    ) -> some View {
        let days = generateDays(year: year, month: month)
        return LazyVGrid(columns: columns, spacing: 8) {
            ForEach(days, id: \.self) { day in
                if let day {
                    dayCell(
                        day: day,
                        year: year,
                        month: month,
                        activeDays: activeDays,
                        selectedDay: selectedDay
                    )
                } else {
                    Color.clear
                        .frame(height: 44)
                }
            }
        }
    }

    @ViewBuilder
    private func dayCell(
        day: Int,
        year: Int,
        month: Int,
        activeDays: Set<String>,
        selectedDay: String?
    ) -> some View {
        let dateStr = dateString(year: year, month: month, day: day)
        let isActive = activeDays.contains(dateStr)
        let isSelected = selectedDay == dateStr
        let isToday = dateStr == todayString()

        Button {
            viewModel.onEvent(CalendarEventSelectDay(date: dateStr))
        } label: {
            VStack(spacing: 3) {
                Text("\(day)")
                    .font(.strakkBody)
                    .foregroundStyle(
                        isSelected
                            ? Color.strakkPrimary
                            : isToday
                                ? Color.strakkPrimary
                                : Color.strakkTextPrimary
                    )
                    .fontWeight(isToday || isSelected ? .semibold : .regular)

                if isActive {
                    Circle()
                        .fill(Color.strakkPrimary)
                        .frame(width: 5, height: 5)
                } else {
                    Circle()
                        .fill(Color.clear)
                        .frame(width: 5, height: 5)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 44)
            .background(
                isSelected
                    ? Color.strakkPrimary.opacity(0.15)
                    : Color.clear,
                in: RoundedRectangle(cornerRadius: 8)
            )
            .overlay(
                isToday && !isSelected
                    ? RoundedRectangle(cornerRadius: 8)
                        .strokeBorder(Color.strakkPrimary.opacity(0.4), lineWidth: 1)
                    : nil
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Jour \(day)\(isActive ? ", repas enregistrés" : "")")
    }

    // MARK: - Helpers

    private func generateDays(year: Int, month: Int) -> [Int?] {
        var components = DateComponents()
        components.year = year
        components.month = month
        components.day = 1

        guard let firstDate = Calendar.current.date(from: components) else { return [] }

        let firstWeekday = Calendar.current.component(.weekday, from: firstDate)
        // Convert Sunday=1 to Monday=1 offset
        let offset = (firstWeekday + 5) % 7

        let range = Calendar.current.range(of: .day, in: .month, for: firstDate)!
        let daysInMonth = range.count

        var cells: [Int?] = Array(repeating: nil, count: offset)
        cells += (1...daysInMonth).map { Optional($0) }
        // Pad to complete the last row
        while cells.count % 7 != 0 { cells.append(nil) }
        return cells
    }

    private func dateString(year: Int, month: Int, day: Int) -> String {
        String(format: "%04d-%02d-%02d", year, month, day)
    }

    private func todayString() -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: Date())
    }

    private func monthLabel(year: Int, month: Int) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        formatter.locale = Locale(identifier: "fr_FR")
        var components = DateComponents()
        components.year = year
        components.month = month
        components.day = 1
        guard let date = Calendar.current.date(from: components) else { return "" }
        return formatter.string(from: date).capitalized
    }

    private func previousMonth(month: Int) -> Int {
        month == 1 ? 12 : month - 1
    }

    private func previousYear(year: Int, month: Int) -> Int {
        month == 1 ? year - 1 : year
    }

    private func nextMonth(month: Int) -> Int {
        month == 12 ? 1 : month + 1
    }

    private func nextYear(year: Int, month: Int) -> Int {
        month == 12 ? year + 1 : year
    }
}
