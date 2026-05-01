import SwiftUI

// MARK: - WizardStepDatesView

struct WizardStepDatesView: View {
    let weekLabel: String
    let availableWeeks: [WeekOptionData]
    let weekDays: [DayOptionData]
    let coveredDates: Set<String>
    let existingCheckInId: String?
    let isEditMode: Bool
    let onSelectWeek: (String) -> Void
    let onToggleDate: (String) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: StrakkSpacing.xl) {
                // Week picker
                weekPickerSection

                // Existing check-in warning
                if existingCheckInId != nil {
                    existingWarningCard
                }

                // Day chips
                dayChipsSection

                // Counter
                Text("\(coveredDates.count) day\(coveredDates.count > 1 ? "s" : "") selected")
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextSecondary)
                    .frame(maxWidth: .infinity, alignment: .center)
            }
            .padding(.horizontal, StrakkSpacing.lg)
            .padding(.vertical, StrakkSpacing.xl)
        }
    }

    // MARK: - Sections

    private var weekPickerSection: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("WEEK")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            if isEditMode {
                // Locked — display only
                HStack {
                    Text(currentWeekDisplayLabel)
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextSecondary)
                    Spacer()
                    Image(systemName: "lock.fill")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.strakkTextTertiary)
                }
                .padding(.horizontal, StrakkSpacing.md)
                .frame(height: 48)
                .background(Color.strakkSurface2)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .accessibilityLabel("Locked week : \(currentWeekDisplayLabel)")
            } else {
                Menu {
                    ForEach(availableWeeks) { week in
                        Button(week.displayLabel) {
                            onSelectWeek(week.weekLabel)
                        }
                    }
                } label: {
                    HStack {
                        Text(currentWeekDisplayLabel)
                            .font(.strakkBody)
                            .foregroundStyle(Color.strakkTextPrimary)
                        Spacer()
                        Image(systemName: "chevron.up.chevron.down")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Color.strakkTextSecondary)
                    }
                    .padding(.horizontal, StrakkSpacing.md)
                    .frame(height: 48)
                    .background(Color.strakkSurface2)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .accessibilityLabel("Select week")
            }
        }
    }

    private var existingWarningCard: some View {
        HStack(spacing: StrakkSpacing.sm) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 16))
                .foregroundStyle(Color.strakkError)

            Text("A check-in already exists for this week.")
                .font(.strakkBody)
                .foregroundStyle(Color.strakkError)

            Spacer()
        }
        .padding(StrakkSpacing.md)
        .background(Color.strakkSurface2)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .strokeBorder(Color.strakkError.opacity(0.3), lineWidth: 1)
        )
        .accessibilityLabel("Warning: a check-in already exists for this week.")
    }

    private var dayChipsSection: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            Text("COVERED DAYS")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)

            LazyVGrid(
                columns: Array(repeating: GridItem(.flexible(), spacing: StrakkSpacing.xs), count: 4),
                spacing: StrakkSpacing.xs
            ) {
                ForEach(weekDays) { day in
                    dayChip(day: day)
                }
            }
        }
    }

    private func dayChip(day: DayOptionData) -> some View {
        let isSelected = coveredDates.contains(day.date)
        return Button {
            onToggleDate(day.date)
        } label: {
            Text(day.displayLabel)
                .font(.strakkCaptionBold)
                .foregroundStyle(isSelected ? .white : Color.strakkTextSecondary)
                .frame(maxWidth: .infinity)
                .frame(height: 40)
                .background(isSelected ? Color.strakkPrimary : Color.strakkSurface2)
                .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .accessibilityLabel("\(day.displayLabel), \(isSelected ? "selected" : "not selected")")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    // MARK: - Helpers

    private var currentWeekDisplayLabel: String {
        availableWeeks.first(where: { $0.weekLabel == weekLabel })?.displayLabel ?? weekLabel
    }
}

// MARK: - Preview

#Preview {
    ZStack {
        Color.strakkBackground.ignoresSafeArea()
        WizardStepDatesView(
            weekLabel: "2024-W04",
            availableWeeks: [
                WeekOptionData(weekLabel: "2024-W04", displayLabel: "Sem. 4 (22–28 jan)", startDate: "2024-01-22", endDate: "2024-01-28"),
                WeekOptionData(weekLabel: "2024-W03", displayLabel: "Sem. 3 (15–21 jan)", startDate: "2024-01-15", endDate: "2024-01-21")
            ],
            weekDays: [
                DayOptionData(date: "2024-01-22", displayLabel: "Lun", selected: true),
                DayOptionData(date: "2024-01-23", displayLabel: "Mar", selected: true),
                DayOptionData(date: "2024-01-24", displayLabel: "Mer", selected: false),
                DayOptionData(date: "2024-01-25", displayLabel: "Jeu", selected: false),
                DayOptionData(date: "2024-01-26", displayLabel: "Ven", selected: false),
                DayOptionData(date: "2024-01-27", displayLabel: "Sam", selected: false),
                DayOptionData(date: "2024-01-28", displayLabel: "Dim", selected: false)
            ],
            coveredDates: ["2024-01-22", "2024-01-23"],
            existingCheckInId: nil,
            isEditMode: false,
            onSelectWeek: { _ in },
            onToggleDate: { _ in }
        )
    }
}
