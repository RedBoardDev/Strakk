package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.ScanMealResult
import com.strakk.shared.domain.repository.MealRepository

/**
 * Calls the `scan-meal` edge function, which combines identify + ground into
 * a single VPS call and returns both predictions and grounded items.
 */
class ScanMealUseCase(private val repository: MealRepository) {
    suspend operator fun invoke(
        photoStoragePaths: List<String>,
        hint: String? = null,
        isTextOnly: Boolean = false,
    ): Result<ScanMealResult> = runSuspendCatching {
        repository.scanMeal(photoStoragePaths, hint, isTextOnly)
    }
}
