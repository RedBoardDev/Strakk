import SwiftUI
import shared

// MARK: - Focus field enum

private enum SettingsField: Hashable {
    case protein, calories, water
}

// MARK: - SettingsView

struct SettingsView: View {
    @State private var viewModel = SettingsViewModelWrapper()

    // Local goal text state (instant input response)
    @State private var proteinText: String = ""
    @State private var caloriesText: String = ""
    @State private var waterText: String = ""

    // Hevy integration state
    @State private var hevyApiKeyText: String = ""

    // Local reminder state
    @State private var dailyEnabled: Bool = false
    @State private var dailyTime: Date = Date()
    @State private var checkinEnabled: Bool = false
    @State private var checkinDay: Int = 0
    @State private var checkinTime: Date = Date()

    @FocusState private var focusedField: SettingsField?

    @State private var didInitialize: Bool = false
    @State private var showSignOutAlert: Bool = false

    var body: some View {
        withAlerts
    }

    // MARK: - Alert layer

    private var withAlerts: some View {
        baseView
            .alert("Error", isPresented: errorAlertBinding) {
                Button("OK", role: .cancel) { viewModel.errorMessage = nil }
            } message: {
                Text(viewModel.errorMessage ?? "")
            }
            .alert("Sign Out", isPresented: $showSignOutAlert) {
                Button("Cancel", role: .cancel) {}
                Button("Sign Out", role: .destructive) {
                    viewModel.onEvent(SettingsEventOnSignOut())
                }
            } message: {
                Text("Are you sure you want to sign out?")
            }
    }

    // MARK: - Base view

    private var baseView: some View {
        ZStack {
            Color.strakkBackground.ignoresSafeArea()

            switch viewModel.state {
            case .loading:
                loadingView
            case .ready(let data):
                readyView(data: data)
            }
        }
        .onChange(of: viewModel.state) { _, newState in
            handleStateChange(newState)
        }
        .toolbar { keyboardToolbar }
    }

    // MARK: - Loading

    private var loadingView: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Settings")
                .font(.strakkHeading1)
                .foregroundStyle(Color.strakkTextPrimary)
                .padding(.horizontal, 20)
                .padding(.top, 16)

            Spacer()
            ProgressView()
                .tint(Color.strakkPrimary)
                .frame(maxWidth: .infinity)
            Spacer()
        }
    }

    // MARK: - Ready

    private func readyView(data: SettingsData) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Header
                Text("Settings")
                    .font(.strakkHeading1)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .padding(.horizontal, 20)
                    .padding(.top, 16)

                Spacer().frame(height: 24)

                accountSection(email: data.email)

                Spacer().frame(height: 24)

                goalsSection

                Spacer().frame(height: 24)

                remindersSection

                Spacer().frame(height: 24)

                hevySection

                Spacer().frame(height: 32)

                signOutButton

                Spacer().frame(height: 32 + 49)
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .task(id: proteinText) {
            guard didInitialize else { return }
            try? await Task.sleep(for: .milliseconds(500))
            viewModel.onEvent(SettingsEventOnProteinGoalChanged(value: proteinText))
        }
        .task(id: caloriesText) {
            guard didInitialize else { return }
            try? await Task.sleep(for: .milliseconds(500))
            viewModel.onEvent(SettingsEventOnCalorieGoalChanged(value: caloriesText))
        }
        .task(id: waterText) {
            guard didInitialize else { return }
            try? await Task.sleep(for: .milliseconds(500))
            viewModel.onEvent(SettingsEventOnWaterGoalChanged(value: waterText))
        }
        .task(id: hevyApiKeyText) {
            guard didInitialize else { return }
            try? await Task.sleep(for: .milliseconds(500))
            viewModel.onEvent(SettingsEventOnHevyApiKeyChanged(value: hevyApiKeyText))
        }
    }

    // MARK: - Account section

    private func accountSection(email: String?) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("ACCOUNT")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .kerning(1.0)
                .padding(.horizontal, 20)

            VStack(spacing: 0) {
                HStack {
                    Image(systemName: "envelope")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.strakkTextTertiary)
                    Spacer().frame(width: 4)
                    Text("Email")
                        .font(.strakkCaptionBold)
                        .foregroundStyle(Color.strakkTextSecondary)
                    Spacer()
                    Text(email ?? "—")
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextPrimary)
                        .lineLimit(1)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
            }
            .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: 12))
            .padding(.horizontal, 20)
        }
    }

    // MARK: - Goals section

    private var goalsSection: some View {
        let anyFocused = focusedField != nil
        return VStack(alignment: .leading, spacing: 8) {
            Text("DAILY GOALS")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .kerning(1.0)
                .padding(.horizontal, 20)

            VStack(spacing: 0) {
                goalRow(
                    color: .strakkProtein,
                    label: "Protein",
                    placeholder: "150",
                    unit: "g",
                    text: $proteinText,
                    field: .protein
                )

                Divider()
                    .background(Color.strakkDivider)
                    .padding(.leading, 16)

                goalRow(
                    color: .strakkCalories,
                    label: "Calories",
                    placeholder: "2200",
                    unit: "kcal",
                    text: $caloriesText,
                    field: .calories
                )

                Divider()
                    .background(Color.strakkDivider)
                    .padding(.leading, 16)

                goalRow(
                    color: .strakkWater,
                    label: "Water",
                    placeholder: "2000",
                    unit: "mL",
                    text: $waterText,
                    field: .water
                )
            }
            .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .strokeBorder(
                        anyFocused ? Color.strakkPrimary.opacity(0.5) : Color.clear,
                        lineWidth: 1
                    )
                    .animation(.easeInOut(duration: 0.15), value: anyFocused)
            )
            .padding(.horizontal, 20)
        }
    }

    @ViewBuilder
    private func goalRow(
        color: Color,
        label: String,
        placeholder: String,
        unit: String,
        text: Binding<String>,
        field: SettingsField
    ) -> some View {
        HStack {
            Circle()
                .fill(color)
                .frame(width: 8, height: 8)
            Spacer().frame(width: 8)
            Text(label)
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkTextPrimary)
            Spacer()
            TextField(placeholder, text: text)
                .font(.strakkHeading3)
                .monospacedDigit()
                .foregroundStyle(Color.strakkTextPrimary)
                .tint(Color.strakkPrimary)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.trailing)
                .focused($focusedField, equals: field)
                .frame(maxWidth: 80)
            Text(unit)
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }

    // MARK: - Reminders section

    private var remindersSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("REMINDERS")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .kerning(1.0)
                .padding(.horizontal, 20)

            VStack(spacing: 0) {
                // Daily tracking reminder toggle
                reminderToggleRow(
                    icon: "bell",
                    label: "Daily reminder",
                    isOn: $dailyEnabled,
                    onChange: {
                        HapticEngine.light()
                        viewModel.onEvent(SettingsEventOnTrackingReminderEnabledChanged(enabled: dailyEnabled))
                    }
                )

                if dailyEnabled {
                    VStack(spacing: 0) {
                        DatePicker(
                            selection: $dailyTime,
                            displayedComponents: .hourAndMinute
                        ) {
                            Text("Time")
                                .font(.strakkCaption)
                                .foregroundStyle(Color.strakkTextSecondary)
                        }
                        .datePickerStyle(.compact)
                        .tint(Color.strakkPrimary)
                        .padding(.horizontal, 16)
                        .padding(.bottom, 14)
                    }
                    .transition(.opacity.combined(with: .move(edge: .top)))
                    .onChange(of: dailyTime) { _, new in
                        guard didInitialize else { return }
                        viewModel.onEvent(SettingsEventOnTrackingReminderTimeChanged(time: formatTime(new)))
                    }
                }

                Divider()
                    .background(Color.strakkDivider)
                    .padding(.leading, 16)

                // Weekly check-in reminder toggle
                reminderToggleRow(
                    icon: "calendar.badge.clock",
                    label: "Weekly check-in",
                    isOn: $checkinEnabled,
                    onChange: {
                        HapticEngine.light()
                        viewModel.onEvent(SettingsEventOnCheckinReminderEnabledChanged(enabled: checkinEnabled))
                    }
                )

                if checkinEnabled {
                    VStack(alignment: .leading, spacing: 12) {
                        dayPickerRow
                        DatePicker(
                            selection: $checkinTime,
                            displayedComponents: .hourAndMinute
                        ) {
                            Text("Time")
                                .font(.strakkCaption)
                                .foregroundStyle(Color.strakkTextSecondary)
                        }
                        .datePickerStyle(.compact)
                        .tint(Color.strakkPrimary)
                        .padding(.horizontal, 16)
                        .padding(.bottom, 14)
                    }
                    .transition(.opacity.combined(with: .move(edge: .top)))
                    .onChange(of: checkinTime) { _, new in
                        guard didInitialize else { return }
                        viewModel.onEvent(SettingsEventOnCheckinReminderTimeChanged(time: formatTime(new)))
                    }
                }
            }
            .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: 12))
            .animation(.easeInOut(duration: 0.2), value: dailyEnabled)
            .animation(.easeInOut(duration: 0.2), value: checkinEnabled)
            .padding(.horizontal, 20)
        }
    }

    @ViewBuilder
    private func reminderToggleRow(
        icon: String,
        label: String,
        isOn: Binding<Bool>,
        onChange: @escaping () -> Void
    ) -> some View {
        HStack {
            Image(systemName: icon)
                .font(.system(size: 15))
                .foregroundStyle(Color.strakkTextTertiary)
            Spacer().frame(width: 8)
            Text(label)
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkTextPrimary)
            Spacer()
            Toggle("", isOn: isOn)
                .tint(Color.strakkPrimary)
                .labelsHidden()
                .onChange(of: isOn.wrappedValue) { _, _ in
                    onChange()
                }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }

    // MARK: - Day picker

    private var dayPickerRow: some View {
        HStack {
            Text("Day")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)
            Spacer()
            HStack(spacing: 6) {
                ForEach(0..<7, id: \.self) { index in
                    let abbrev = dayAbbreviation(index)
                    let isSelected = checkinDay == index
                    Button {
                        withAnimation(.easeInOut(duration: 0.15)) {
                            checkinDay = index
                        }
                        HapticEngine.light()
                        viewModel.onEvent(SettingsEventOnCheckinReminderDayChanged(day: Int32(index)))
                    } label: {
                        Text(abbrev)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(isSelected ? Color.white : Color.strakkTextTertiary)
                            .frame(width: 32, height: 32)
                            .background(
                                Circle()
                                    .fill(isSelected ? Color.strakkPrimary : Color.clear)
                            )
                    }
                    .accessibilityLabel(fullDayName(index))
                }
            }
        }
        .padding(.horizontal, 16)
    }

    // MARK: - Hevy section

    private var hevySection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("HEVY INTEGRATION")
                .font(.strakkOverline)
                .foregroundStyle(Color.strakkTextTertiary)
                .kerning(1.0)
                .padding(.horizontal, 20)

            VStack(spacing: 0) {
                HStack {
                    Image(systemName: "dumbbell.fill")
                        .font(.system(size: 13))
                        .foregroundStyle(Color.strakkTextTertiary)
                    Spacer().frame(width: 8)
                    Text("API Key")
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Spacer()
                    SecureField("Paste Hevy API key", text: $hevyApiKeyText)
                        .font(.strakkBody)
                        .foregroundStyle(Color.strakkTextPrimary)
                        .multilineTextAlignment(.trailing)
                        .frame(maxWidth: 200)
                        .tint(Color.strakkPrimary)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
            }
            .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: 12))
            .padding(.horizontal, 20)
        }
    }

    // MARK: - Sign Out button

    private var signOutButton: some View {
        Button {
            HapticEngine.light()
            showSignOutAlert = true
        } label: {
            Text("Sign Out")
                .font(.strakkBodyBold)
                .foregroundStyle(Color.strakkError)
                .frame(maxWidth: .infinity)
                .frame(height: 52)
                .background(Color.strakkSurface1, in: RoundedRectangle(cornerRadius: 12))
        }
        .padding(.horizontal, 20)
        .accessibilityLabel("Sign out of Strakk")
    }

    // MARK: - Keyboard toolbar

    @ToolbarContentBuilder
    private var keyboardToolbar: some ToolbarContent {
        ToolbarItemGroup(placement: .keyboard) {
            Button {
                navigateFocus(direction: .previous)
            } label: {
                Image(systemName: "chevron.up")
            }
            .disabled(focusedField == .protein)

            Button {
                navigateFocus(direction: .next)
            } label: {
                Image(systemName: "chevron.down")
            }
            .disabled(focusedField == .water)

            Spacer()
        }
    }

    // MARK: - Helpers

    private var errorAlertBinding: Binding<Bool> {
        Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.errorMessage = nil } }
        )
    }

    private func handleStateChange(_ newState: SettingsState) {
        guard !didInitialize, case .ready(let data) = newState else { return }
        proteinText = data.proteinGoal
        caloriesText = data.calorieGoal
        waterText = data.waterGoal
        dailyEnabled = data.trackingReminderEnabled
        dailyTime = parseTime(data.trackingReminderTime)
        checkinEnabled = data.checkinReminderEnabled
        checkinDay = data.checkinReminderDay
        checkinTime = parseTime(data.checkinReminderTime)
        hevyApiKeyText = data.hevyApiKey
        didInitialize = true
    }

    private enum FocusDirection { case previous, next }

    private func navigateFocus(direction: FocusDirection) {
        let order: [SettingsField] = [.protein, .calories, .water]
        guard let current = focusedField,
              let idx = order.firstIndex(of: current) else { return }
        switch direction {
        case .previous:
            if idx > 0 { focusedField = order[idx - 1] }
        case .next:
            if idx < order.count - 1 { focusedField = order[idx + 1] }
        }
    }

    private func parseTime(_ hhmm: String) -> Date {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.date(from: hhmm) ?? Date()
    }

    private func formatTime(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }

    private func dayAbbreviation(_ index: Int) -> String {
        ["M", "T", "W", "T", "F", "S", "S"][index]
    }

    private func fullDayName(_ index: Int) -> String {
        ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"][index]
    }
}
