package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.NutritionGoals
import com.strakk.shared.domain.model.UserProfile
import com.strakk.shared.domain.repository.ProfileRepository

class CompleteOnboardingUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(goals: NutritionGoals): Result<UserProfile> =
        runSuspendCatching {
            profileRepository.completeOnboarding(goals)
        }
}
