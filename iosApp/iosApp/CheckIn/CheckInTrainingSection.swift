import SwiftUI

// MARK: - CheckInTrainingSection

struct CheckInTrainingSection: View {
    let stats: WeeklyTrainingStatsData?
    let isLoading: Bool
    let onRefresh: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: StrakkSpacing.xs) {
            HStack {
                Text("TRAINING")
                    .font(.strakkOverline)
                    .foregroundStyle(Color.strakkTextTertiary)
                Spacer()
                refreshButton
            }

            if let stats {
                statsGrid(stats)
                muscleVolumeBars(stats)
                workoutsList(stats.workouts)
            } else if isLoading {
                skeletonGrid
            } else {
                loadPrompt
            }
        }
    }

    // MARK: - Refresh / Load button

    @ViewBuilder
    private var refreshButton: some View {
        Button(action: onRefresh) {
            if isLoading {
                ProgressView()
                    .tint(Color.strakkPrimary)
                    .scaleEffect(0.7)
            } else {
                Image(systemName: stats != nil ? "arrow.clockwise" : "arrow.down.circle")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(Color.strakkPrimary)
            }
        }
        .disabled(isLoading)
        .accessibilityLabel(Text(stats != nil ? "Refresh training data" : "Load training data"))
    }

    // MARK: - Empty state

    private var loadPrompt: some View {
        VStack(spacing: StrakkSpacing.sm) {
            Image(systemName: "dumbbell")
                .font(.system(size: 24))
                .foregroundStyle(Color.strakkTextTertiary)
            Text("Tap to load training data from Hevy")
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(StrakkSpacing.lg)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
        .onTapGesture { onRefresh() }
    }

    // MARK: - Stats grid (2×2)

    private func statsGrid(_ stats: WeeklyTrainingStatsData) -> some View {
        LazyVGrid(
            columns: [GridItem(.flexible()), GridItem(.flexible())],
            spacing: StrakkSpacing.sm
        ) {
            statCard(
                icon: "flame.fill",
                value: "\(stats.totalSessions)",
                label: "Sessions"
            )
            statCard(
                icon: "clock.fill",
                value: formattedDuration(stats.totalDurationMinutes),
                label: "Duration"
            )
            statCard(
                icon: "scalemass.fill",
                value: formatVolume(stats.totalVolumeKg),
                label: "Volume"
            )
            if let rpe = stats.avgRpe {
                statCard(
                    icon: "heart.fill",
                    value: String(format: "%.1f", rpe),
                    label: "Avg RPE"
                )
            } else {
                let totalExercises = stats.workouts.reduce(0) { $0 + $1.exercises.count }
                statCard(
                    icon: "figure.strengthtraining.traditional",
                    value: "\(totalExercises)",
                    label: "Exercises"
                )
            }
        }
    }

    private func statCard(icon: String, value: String, label: LocalizedStringKey) -> some View {
        VStack(spacing: StrakkSpacing.xxs) {
            Image(systemName: icon)
                .font(.system(size: 14))
                .foregroundStyle(Color.strakkPrimary)

            Text(value)
                .font(.strakkHeading3)
                .foregroundStyle(Color.strakkTextPrimary)

            Text(label)
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextTertiary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, StrakkSpacing.md)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
    }

    // MARK: - Muscle volume bars

    @ViewBuilder
    private func muscleVolumeBars(_ stats: WeeklyTrainingStatsData) -> some View {
        let sorted = stats.muscleGroupVolume
            .sorted { $0.value > $1.value }
        if !sorted.isEmpty, let maxVol = sorted.first?.value, maxVol > 0 {
            VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
                Text("MUSCLE GROUPS")
                    .font(.strakkOverline)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .padding(.top, StrakkSpacing.xs)

                VStack(spacing: 4) {
                    ForEach(sorted, id: \.key) { muscle, vol in
                        HStack(spacing: StrakkSpacing.xs) {
                            Text(muscle.capitalized)
                                .font(.strakkCaption)
                                .foregroundStyle(Color.strakkTextSecondary)
                                .frame(width: 72, alignment: .trailing)

                            GeometryReader { geo in
                                RoundedRectangle(cornerRadius: 3)
                                    .fill(Color.strakkPrimary.opacity(0.35))
                                    .frame(width: geo.size.width * CGFloat(vol / maxVol))
                            }
                            .frame(height: 8)

                            Text(formatVolume(vol))
                                .font(.strakkCaption)
                                .foregroundStyle(Color.strakkTextTertiary)
                                .frame(width: 48, alignment: .trailing)
                                .monospacedDigit()
                        }
                        .frame(height: 20)
                    }
                }
                .padding(StrakkSpacing.md)
                .background(Color.strakkSurface1)
                .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
            }
        }
    }

    // MARK: - Workouts list (collapsible)

    @ViewBuilder
    private func workoutsList(_ workouts: [HevyWorkoutData]) -> some View {
        if !workouts.isEmpty {
            VStack(alignment: .leading, spacing: StrakkSpacing.xxs) {
                Text("SESSIONS")
                    .font(.strakkOverline)
                    .foregroundStyle(Color.strakkTextTertiary)
                    .padding(.top, StrakkSpacing.xs)

                VStack(spacing: StrakkSpacing.sm) {
                    ForEach(workouts) { workout in
                        workoutCard(workout)
                    }
                }
            }
        }
    }

    private func workoutCard(_ workout: HevyWorkoutData) -> some View {
        DisclosureGroup {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(workout.exercises.enumerated()), id: \.offset) { index, exercise in
                    if index > 0 {
                        Divider()
                            .background(Color.strakkDivider)
                    }
                    exerciseRow(exercise)
                        .padding(.vertical, StrakkSpacing.xs)
                }
            }
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(workout.title)
                        .font(.strakkBodyBold)
                        .foregroundStyle(Color.strakkTextPrimary)
                    Text(workout.date)
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextTertiary)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(formattedDuration(workout.durationMinutes))
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkTextSecondary)
                    Text(formatVolume(workout.totalVolumeKg))
                        .font(.strakkCaption)
                        .foregroundStyle(Color.strakkPrimary)
                }
            }
        }
        .tint(Color.strakkTextTertiary)
        .padding(StrakkSpacing.md)
        .background(Color.strakkSurface1)
        .clipShape(RoundedRectangle(cornerRadius: StrakkRadius.sm))
    }

    private func exerciseRow(_ exercise: HevyWorkoutExerciseData) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 1) {
                Text(exercise.name)
                    .font(.strakkBody)
                    .foregroundStyle(Color.strakkTextPrimary)
                    .lineLimit(1)
                Text(exercise.muscleGroup.capitalized)
                    .font(.strakkCaption)
                    .foregroundStyle(Color.strakkTextTertiary)
            }
            Spacer()
            Text(setsDescription(exercise.sets))
                .font(.strakkCaption)
                .foregroundStyle(Color.strakkTextSecondary)
                .monospacedDigit()
        }
    }

    // MARK: - Skeleton

    private var skeletonGrid: some View {
        LazyVGrid(
            columns: [GridItem(.flexible()), GridItem(.flexible())],
            spacing: StrakkSpacing.sm
        ) {
            ForEach(0..<4, id: \.self) { _ in
                RoundedRectangle(cornerRadius: StrakkRadius.sm)
                    .fill(Color.strakkSurface1)
                    .frame(height: 80)
                    .redacted(reason: .placeholder)
            }
        }
    }

    // MARK: - Formatting

    private func formattedDuration(_ minutes: Int) -> String {
        let hours = minutes / 60
        let mins = minutes % 60
        if hours > 0 { return "\(hours)h\(String(format: "%02d", mins))" }
        return "\(mins) min"
    }

    private func formatVolume(_ kg: Double) -> String {
        if kg >= 1000 {
            return String(format: "%.1ft", kg / 1000)
        }
        return String(format: "%.0f kg", kg)
    }

    private func setsDescription(_ sets: [HevyWorkoutSetData]) -> String {
        let normalSets = sets.filter { $0.type == "normal" }
        if normalSets.isEmpty { return "\(sets.count) sets" }
        let reps = normalSets.compactMap(\.reps)
        if reps.isEmpty { return "\(normalSets.count) sets" }
        let weight = normalSets.compactMap(\.weightKg).max()
        let repsStr = reps.map(String.init).joined(separator: "·")
        if let w = weight, w > 0 {
            return "\(repsStr) × \(String(format: "%.0f", w))kg"
        }
        return "\(repsStr) reps"
    }
}
