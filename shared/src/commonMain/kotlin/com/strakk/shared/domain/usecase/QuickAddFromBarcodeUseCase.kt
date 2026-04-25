package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.MealEntry
import com.strakk.shared.domain.repository.BarcodeLookupRepository
import com.strakk.shared.domain.repository.NutritionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Quick-add via barcode: lookup Open Food Facts, persist as orphan entry.
 *
 * Returns [Result.failure] with a [DomainError.DataError] when the barcode is
 * unknown — the UI should offer to open the manual-entry form pre-filled.
 */
class QuickAddFromBarcodeUseCase(
    private val barcodeLookup: BarcodeLookupRepository,
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(barcode: String): Result<MealEntry> = runSuspendCatching {
        val lookup = barcodeLookup.lookup(barcode)
            ?: throw DomainError.DataError("Produit inconnu (code-barres $barcode)")
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
        nutritionRepository.addMeal(lookup.copy(logDate = today))
    }
}
