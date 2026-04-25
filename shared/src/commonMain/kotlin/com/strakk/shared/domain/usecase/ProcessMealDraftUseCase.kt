package com.strakk.shared.domain.usecase

import com.strakk.shared.domain.common.DomainError
import com.strakk.shared.domain.common.runSuspendCatching
import com.strakk.shared.domain.model.ActiveMealDraft
import com.strakk.shared.domain.model.DraftItem
import com.strakk.shared.domain.repository.MealDraftRepository
import com.strakk.shared.domain.repository.MealPhotoRepository
import com.strakk.shared.domain.repository.MealRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * Processes the active draft:
 *   1. Uploads any un-uploaded [DraftItem.PendingPhoto] to Supabase Storage
 *      (skipping already-uploaded items via [ActiveMealDraft.uploadedPaths]).
 *   2. Calls the `extract-meal-draft` edge function in batches (max 2 images/call, D16).
 *   3. Marks each pending item as [DraftItem.Resolved] in the local draft.
 *
 * Returns the updated [ActiveMealDraft] with all items resolved (best-effort:
 * items that failed AI extraction have a null entry; the caller must handle
 * these in the Review screen).
 *
 * Does NOT commit the draft to Supabase — that is [CommitMealDraftUseCase].
 *
 * @throws [DomainError.DataError] if a photo upload fails after all retries
 *   (draft is left intact for retry).
 */
class ProcessMealDraftUseCase(
    private val draftRepository: MealDraftRepository,
    private val photoRepository: MealPhotoRepository,
    private val mealRepository: MealRepository,
) {
    /**
     * Extraction result for a single item returned after processing.
     * On extraction failure, [error] is set and [resolvedItem] is null.
     */
    data class ItemResult(
        val itemId: String,
        val resolvedItem: DraftItem.Resolved?,
        val error: String?,
    )

    suspend operator fun invoke(): Result<ActiveMealDraft> =
        runSuspendCatching {
            val draft = draftRepository.observeActiveDraft().first()
                ?: throw DomainError.DataError("No active draft to process.")

            // --- Phase 1: upload pending photos ---
            val pendingPhotos = draft.items.filterIsInstance<DraftItem.PendingPhoto>()
            val uploadedPaths = draft.uploadedPaths.toMutableMap()

            for (photo in pendingPhotos) {
                if (uploadedPaths.containsKey(photo.id)) continue
                val path = photoRepository.uploadPhoto(
                    draftId = draft.id,
                    itemId = photo.id,
                    base64 = photo.imageBase64,
                )
                uploadedPaths[photo.id] = path
                draftRepository.recordUploadedPath(photo.id, path)
            }

            // --- Phase 2: extract pending items (split: max 2 images per batch) ---
            val pendingTexts = draft.items.filterIsInstance<DraftItem.PendingText>()
            val allPending: List<DraftItem> = pendingPhotos + pendingTexts

            if (allPending.isEmpty()) {
                return@runSuspendCatching draftRepository.observeActiveDraft().first()
                    ?: throw DomainError.DataError("Draft disappeared after processing.")
            }

            val results = mutableMapOf<String, ItemResult>()

            val photoBatches = pendingPhotos.chunked(2)

            coroutineScope {
                if (photoBatches.isEmpty()) {
                    // Only text items — single batch
                    val batchResults = extractBatch(
                        photos = emptyList(),
                        texts = pendingTexts,
                        uploadedPaths = uploadedPaths,
                        draft = draft,
                    )
                    results.putAll(batchResults)
                } else {
                    val jobs = photoBatches.mapIndexed { batchIndex, photoBatch ->
                        val textsForBatch = if (batchIndex == 0) pendingTexts else emptyList()
                        async {
                            runCatching {
                                extractBatch(
                                    photos = photoBatch,
                                    texts = textsForBatch,
                                    uploadedPaths = uploadedPaths,
                                    draft = draft,
                                )
                            }.getOrElse { _ ->
                                // Batch failed → per-item fallback via analyze-meal-single
                                val fallback = mutableMapOf<String, ItemResult>()
                                for (photo in photoBatch) {
                                    fallback[photo.id] = runCatching {
                                        val resolved = mealRepository.analyzePhotoSingle(
                                            imageBase64 = photo.imageBase64,
                                            hint = photo.hint,
                                            draftItemId = photo.id,
                                        )
                                        ItemResult(photo.id, resolved, null)
                                    }.getOrElse { e ->
                                        ItemResult(
                                            photo.id,
                                            null,
                                            e.message ?: "Analysis failed",
                                        )
                                    }
                                }
                                if (batchIndex == 0) {
                                    for (text in textsForBatch) {
                                        fallback[text.id] = runCatching {
                                            val resolved = mealRepository.analyzeTextSingle(
                                                description = text.description,
                                                draftItemId = text.id,
                                            )
                                            ItemResult(text.id, resolved, null)
                                        }.getOrElse { e ->
                                            ItemResult(
                                                text.id,
                                                null,
                                                e.message ?: "Analysis failed",
                                            )
                                        }
                                    }
                                }
                                fallback
                            }
                        }
                    }
                    jobs.forEach { job -> results.putAll(job.await()) }
                }
            }

            // --- Phase 3: apply resolved items to local draft ---
            for ((_, result) in results) {
                val resolved = result.resolvedItem ?: continue
                draftRepository.markItemResolved(result.itemId, resolved.entry)
            }

            draftRepository.observeActiveDraft().first()
                ?: throw DomainError.DataError("Draft disappeared after processing.")
        }

    private suspend fun extractBatch(
        photos: List<DraftItem.PendingPhoto>,
        texts: List<DraftItem.PendingText>,
        uploadedPaths: Map<String, String>,
        draft: ActiveMealDraft,
    ): Map<String, ItemResult> {
        val batchResults = mealRepository.extractMealDraftBatch(
            draftId = draft.id,
            photoItems = photos.map { photo ->
                MealRepository.ExtractPhotoItem(
                    id = photo.id,
                    photoPath = uploadedPaths[photo.id]
                        ?: throw DomainError.DataError(
                            "Missing upload path for item ${photo.id}",
                        ),
                    hint = photo.hint,
                )
            },
            textItems = texts.map { text ->
                MealRepository.ExtractTextItem(
                    id = text.id,
                    description = text.description,
                )
            },
        )
        return batchResults.associate { result ->
            result.id to ItemResult(result.id, result.resolvedItem, result.error)
        }
    }
}
