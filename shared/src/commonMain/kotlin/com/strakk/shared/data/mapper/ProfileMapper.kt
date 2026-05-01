package com.strakk.shared.data.mapper

import com.strakk.shared.data.dto.ProfileDto
import com.strakk.shared.domain.model.UserProfile

/**
 * Maps a [ProfileDto] from the data layer to a [UserProfile] domain entity.
 */
internal fun ProfileDto.toDomain(): UserProfile = UserProfile(
    id = id,
    proteinGoal = proteinGoal,
    calorieGoal = calorieGoal,
    waterGoal = waterGoal,
)
