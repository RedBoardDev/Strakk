package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.repository.NutritionRepository
import kotlinx.coroutines.flow.first

/**
 * Removes the most recent water entry matching [amount] for the given [date].
 *
 * Used by the Today screen when the user taps the `− 250ml` / `− 500ml`
 * buttons: we don't expose a generic "remove water" — we delete the latest
 * entry of the same size, which mirrors the typical "I tapped + by mistake"
 * intent.
 *
 * If no entry of the exact [amount] exists, falls back to deleting the most
 * recent water entry of the day (whatever its size). Returns success with no
 * effect when the day has no water entries.
 */
class RemoveLastWaterEntryUseCase(
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(date: String, amount: Int): Result<Unit> =
        runSuspendCatching {
            val entries = nutritionRepository
                .observeWaterEntriesForDate(date)
                .first()
            if (entries.isEmpty()) return@runSuspendCatching

            val target = entries.lastOrNull { it.amount == amount }
                ?: entries.last()

            nutritionRepository.deleteWater(target.id)
        }
}
