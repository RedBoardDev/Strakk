import SwiftUI

struct RemindersStepView: View {
    let trackingEnabled: Bool
    let trackingTime: String
    let checkinEnabled: Bool
    let checkinDay: Int
    let checkinTime: String

    let onTrackingToggled: (Bool) -> Void
    let onTrackingTimeChanged: (String) -> Void
    let onCheckinToggled: (Bool) -> Void
    let onCheckinDayChanged: (Int) -> Void
    let onCheckinTimeChanged: (String) -> Void

    // Local date bindings for DatePicker — converted to/from "HH:mm" strings
    @State private var trackingDate: Date = Self.defaultTime(hour: 17, minute: 0)
    @State private var checkinDate: Date = Self.defaultTime(hour: 10, minute: 0)

    private static let weekdayNames = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 32) {
                // Header
                VStack(alignment: .leading, spacing: 12) {
                    Image(systemName: "bell.badge")
                        .font(.system(size: 48))
                        .foregroundStyle(Color.strakkTextSecondary)

                    Text("Stay on track")
                        .font(.strakkHeading1)
                        .foregroundStyle(Color.strakkTextPrimary)

                    Text("We'll gently remind you — you can change these later")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextSecondary)
                }

                // Card 1: Daily tracking reminder
                ReminderCardView {
                    VStack(spacing: 16) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Daily tracking")
                                    .font(.strakkHeading3)
                                    .foregroundStyle(Color.strakkTextPrimary)
                                Text("Log your meals and water")
                                    .font(.strakkCaption)
                                    .foregroundStyle(Color.strakkTextSecondary)
                            }
                            Spacer()
                            Toggle("", isOn: Binding(
                                get: { trackingEnabled },
                                set: { onTrackingToggled($0) }
                            ))
                            .tint(Color.strakkPrimary)
                            .labelsHidden()
                        }

                        if trackingEnabled {
                            Divider()
                                .background(Color.strakkDivider)

                            HStack {
                                Text("Reminder time")
                                    .font(.strakkBody)
                                    .foregroundStyle(Color.strakkTextSecondary)
                                Spacer()
                                DatePicker(
                                    "",
                                    selection: $trackingDate,
                                    displayedComponents: .hourAndMinute
                                )
                                .labelsHidden()
                                .tint(Color.strakkPrimary)
                                .onChange(of: trackingDate) { _, newDate in
                                    onTrackingTimeChanged(Self.formatTime(newDate))
                                }
                            }
                        }
                    }
                }

                // Card 2: Weekly check-in reminder
                ReminderCardView {
                    VStack(spacing: 16) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Weekly check-in")
                                    .font(.strakkHeading3)
                                    .foregroundStyle(Color.strakkTextPrimary)
                                Text("Reflect on your progress")
                                    .font(.strakkCaption)
                                    .foregroundStyle(Color.strakkTextSecondary)
                            }
                            Spacer()
                            Toggle("", isOn: Binding(
                                get: { checkinEnabled },
                                set: { onCheckinToggled($0) }
                            ))
                            .tint(Color.strakkPrimary)
                            .labelsHidden()
                        }

                        if checkinEnabled {
                            Divider()
                                .background(Color.strakkDivider)

                            HStack {
                                Text("Day")
                                    .font(.strakkBody)
                                    .foregroundStyle(Color.strakkTextSecondary)
                                Spacer()
                                Picker("Day", selection: Binding(
                                    get: { checkinDay },
                                    set: { onCheckinDayChanged($0) }
                                )) {
                                    ForEach(0..<Self.weekdayNames.count, id: \.self) { index in
                                        Text(Self.weekdayNames[index]).tag(index)
                                    }
                                }
                                .tint(Color.strakkPrimary)
                            }

                            HStack {
                                Text("Time")
                                    .font(.strakkBody)
                                    .foregroundStyle(Color.strakkTextSecondary)
                                Spacer()
                                DatePicker(
                                    "",
                                    selection: $checkinDate,
                                    displayedComponents: .hourAndMinute
                                )
                                .labelsHidden()
                                .tint(Color.strakkPrimary)
                                .onChange(of: checkinDate) { _, newDate in
                                    onCheckinTimeChanged(Self.formatTime(newDate))
                                }
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 32)
            .padding(.bottom, 24)
        }
    }

    private static func defaultTime(hour: Int, minute: Int) -> Date {
        var components = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        components.hour = hour
        components.minute = minute
        return Calendar.current.date(from: components) ?? Date()
    }

    private static func formatTime(_ date: Date) -> String {
        let components = Calendar.current.dateComponents([.hour, .minute], from: date)
        let hour = components.hour ?? 0
        let minute = components.minute ?? 0
        return String(format: "%02d:%02d", hour, minute)
    }
}

private struct ReminderCardView<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        VStack {
            content
        }
        .padding(16)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
