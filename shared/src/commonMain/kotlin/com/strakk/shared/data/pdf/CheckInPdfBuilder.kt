package com.strakk.shared.data.pdf

import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.NutritionSummary
import com.strakk.shared.domain.model.PdfExportOptions
import com.strakk.shared.domain.repository.CheckInRepository
import com.strakk.shared.domain.service.CheckInPdfGenerator
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull

private const val BUCKET = "checkin-photos"

internal class CheckInPdfBuilderImpl(
    private val checkInRepository: CheckInRepository,
    private val supabaseClient: SupabaseClient,
) : CheckInPdfGenerator {

    private val pageH = PdfWriter.PAGE_HEIGHT
    private val pageW = PdfWriter.PAGE_WIDTH
    private val marginL = 56f
    private val marginR = 56f
    private val marginT = 56f
    private val marginB = 72f
    private val contentW get() = pageW - marginL - marginR

    private val colDark = Triple(0.13f, 0.13f, 0.13f)
    private val colMid = Triple(0.45f, 0.45f, 0.45f)
    private val colLight = Triple(0.65f, 0.65f, 0.65f)
    private val colSep = Triple(0.82f, 0.82f, 0.82f)
    private val colRowAlt = Triple(0.96f, 0.96f, 0.96f)
    private val colPhotoBg = Triple(0.93f, 0.93f, 0.93f)
    private val colAccent   = Triple(0.878f, 0.486f, 0.310f) // #E07C4F — Strakk orange
    private val colPositive = Triple(0.302f, 0.682f, 0.416f) // #4DAE6A — success green
    private val colNeg      = Triple(0.878f, 0.322f, 0.322f) // #E05252 — error red

    private val tagLabels = mapOf(
        "energy_stable" to "Énergie stable",
        "good_energy" to "Bonne énergie",
        "motivated" to "Motivation",
        "disciplined" to "Régularité",
        "good_sleep" to "Bien dormi",
        "good_recovery" to "Bonne récup",
        "strong_training" to "Séances solides",
        "good_mood" to "Bonne humeur",
        "focused" to "Mental clair",
        "light_body" to "Corps léger",
        "good_digestion" to "Bonne digestion",
        "low_energy" to "Peu d'énergie",
        "tired" to "Fatigue",
        "poor_sleep" to "Mal dormi",
        "stress" to "Stress",
        "low_motivation" to "Motivation basse",
        "heavy_body" to "Corps lourd",
        "sore" to "Courbatures",
        "joint_discomfort" to "Gêne articulaire",
        "digestion_discomfort" to "Digestion difficile",
        "bloating" to "Ballonnements",
        "hungry" to "Faim marquée",
        "irritability" to "Irritabilité",
        "low_mood" to "Moral bas",
    )

    private val positiveSlugs = setOf(
        "energy_stable", "good_energy", "motivated", "disciplined", "good_sleep",
        "good_recovery", "strong_training", "good_mood", "focused", "light_body", "good_digestion",
    )

    private val months = listOf(
        "janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre",
    )

    private var fontSize = 12f

    // ── Entry point ─────────────────────────────────────────────────────────

    override suspend fun generate(checkInId: String, options: PdfExportOptions): ByteArray {
        val checkIn = checkInRepository.observeCheckIn(checkInId).firstOrNull()
            ?: error("Check-in $checkInId not found")

        val photoData = if (options.includePhotos) downloadPhotos(checkIn) else emptyMap()

        val enrichedCheckIn = if (options.includeDailyData && checkIn.nutritionSummary != null && checkIn.coveredDates.isNotEmpty()) {
            val averages = checkInRepository.computeNutritionAverages(checkIn.coveredDates)
            checkIn.copy(
                nutritionSummary = checkIn.nutritionSummary.copy(dailyData = averages.dailyData)
            )
        } else {
            checkIn
        }

        fontSize = 12f
        return buildPdf(enrichedCheckIn, photoData, options)
    }

    private suspend fun downloadPhotos(checkIn: CheckIn): Map<String, ByteArray> =
        coroutineScope {
            checkIn.photos.map { photo ->
                async {
                    try {
                        photo.id to supabaseClient.storage
                            .from(BUCKET)
                            .downloadAuthenticated(photo.storagePath)
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull().toMap()
        }

    // ── PDF construction ────────────────────────────────────────────────────

    private fun buildPdf(checkIn: CheckIn, photoData: Map<String, ByteArray>, options: PdfExportOptions): ByteArray {
        val pdf = PdfWriter()
        pdf.addPage()

        val imageRefs = mutableMapOf<String, ImageRef>()
        for (photo in checkIn.photos.sortedBy { it.position }) {
            val jpeg = photoData[photo.id] ?: continue
            val ref = pdf.addImage(jpeg) ?: continue
            imageRefs[photo.id] = ref
        }

        var y = pageH - marginT

        pdf.setColor(colAccent)
        pdf.fillRect(marginL, y, contentW, 3f)
        y -= 20f

        y = drawHeader(pdf, checkIn, y)
        y -= 24f

        if (options.includePhotos && imageRefs.isNotEmpty()) {
            y = drawPhotos(pdf, checkIn, imageRefs, y)
            y -= 20f
        }

        if (options.includeMeasurements) {
            y = drawMeasurements(pdf, checkIn, y)
            y -= 20f
        }

        if (options.includeFeelings && (
            checkIn.feelingTags.isNotEmpty() ||
            !checkIn.mentalFeeling.isNullOrBlank() ||
            !checkIn.physicalFeeling.isNullOrBlank()
        )) {
            y = drawFeelings(pdf, checkIn, y)
            y -= 20f
        }

        if (options.includeNutrition) {
            checkIn.nutritionSummary?.let { ns ->
                drawNutrition(pdf, ns, y, options)
            }
        }

        drawFooter(pdf)
        return pdf.toByteArray()
    }

    // ── Header ──────────────────────────────────────────────────────────────

    private fun drawHeader(pdf: PdfWriter, checkIn: CheckIn, startY: Float): Float {
        var y = startY

        font(pdf, PdfWriter.FONT_HELVETICA_BOLD, 10f)
        pdf.setColor(colMid)
        pdf.drawText(marginL, y, "BILAN SEMAINE")
        y -= 26f

        font(pdf, PdfWriter.FONT_HELVETICA_BOLD, 24f)
        pdf.setColor(colDark)
        pdf.drawText(marginL, y, formatWeekLabel(checkIn.weekLabel))
        y -= 20f

        val subtitle = buildSubtitle(checkIn)
        if (subtitle.isNotEmpty()) {
            font(pdf, PdfWriter.FONT_HELVETICA, 11f)
            pdf.setColor(colMid)
            pdf.drawText(marginL, y, subtitle)
            y -= 16f
        }

        return y
    }

    // ── Photos ──────────────────────────────────────────────────────────────

    private fun drawPhotos(
        pdf: PdfWriter,
        checkIn: CheckIn,
        imageRefs: Map<String, ImageRef>,
        startY: Float,
    ): Float {
        val sorted = checkIn.photos.sortedBy { it.position }.filter { imageRefs.containsKey(it.id) }
        if (sorted.isEmpty()) return startY

        var y = startY
        y = drawSectionHeader(pdf, "PHOTOS", y)
        y -= 10f

        val maxPerRow = 3
        val gap = 10f
        val maxPhotoH = 180f

        for (row in sorted.chunked(maxPerRow)) {
            val photoW = (contentW - (row.size - 1) * gap) / row.size
            val photoH = minOf(photoW * 4f / 3f, maxPhotoH)

            y = breakIfNeeded(pdf, y, photoH + 10f)

            var x = marginL
            for (photo in row) {
                val ref = imageRefs[photo.id] ?: continue
                pdf.setColor(colPhotoBg)
                pdf.fillRect(x, y - photoH, photoW, photoH)

                val imgAspect = ref.width.toFloat() / ref.height.toFloat()
                val boxAspect = photoW / photoH
                val (drawW, drawH) = if (imgAspect > boxAspect) {
                    photoW to (photoW / imgAspect)
                } else {
                    (photoH * imgAspect) to photoH
                }
                pdf.drawImage(
                    x + (photoW - drawW) / 2f,
                    (y - photoH) + (photoH - drawH) / 2f,
                    drawW, drawH, ref.name,
                )
                x += photoW + gap
            }
            y -= photoH + gap
        }

        return y
    }

    // ── Measurements ────────────────────────────────────────────────────────

    private fun drawMeasurements(pdf: PdfWriter, checkIn: CheckIn, startY: Float): Float {
        val rows = buildMeasurementRows(checkIn)

        var y = startY
        val sectionH = 20f + rows.size.coerceAtLeast(1) * 22f
        y = breakIfNeeded(pdf, y, sectionH)
        y = drawSectionHeader(pdf, "MESURES", y)
        y -= 8f

        if (rows.isEmpty()) {
            font(pdf, PdfWriter.FONT_HELVETICA, 10f)
            pdf.setColor(colLight)
            pdf.drawText(marginL, y, "Aucune mesure renseignée")
            return y - 14f
        }

        y = drawTableRows(pdf, rows, y)
        return y
    }

    // ── Feelings ────────────────────────────────────────────────────────────

    private fun drawFeelings(pdf: PdfWriter, checkIn: CheckIn, startY: Float): Float {
        var y = startY
        y = breakIfNeeded(pdf, y, 60f)
        y = drawSectionHeader(pdf, "RESSENTIS", y)
        y -= 10f

        if (checkIn.feelingTags.isNotEmpty()) {
            val positive = checkIn.feelingTags.filter { it in positiveSlugs }
            val negative = checkIn.feelingTags.filter { it !in positiveSlugs }

            if (positive.isNotEmpty()) {
                val tagStr = positive.joinToString("  •  ") { tagLabels[it] ?: it }
                font(pdf, PdfWriter.FONT_HELVETICA_BOLD, 9f)
                pdf.setColor(colPositive)
                pdf.drawText(marginL, y, "Sensations positives")
                y -= 14f
                font(pdf, PdfWriter.FONT_HELVETICA, 10f)
                pdf.setColor(colDark)
                y = drawWrapped(pdf, tagStr, y, 13f)
                y -= 8f
            }

            if (negative.isNotEmpty()) {
                val tagStr = negative.joinToString("  •  ") { tagLabels[it] ?: it }
                y = breakIfNeeded(pdf, y, 30f)
                font(pdf, PdfWriter.FONT_HELVETICA_BOLD, 9f)
                pdf.setColor(colNeg)
                pdf.drawText(marginL, y, "Sensations négatives")
                y -= 14f
                font(pdf, PdfWriter.FONT_HELVETICA, 10f)
                pdf.setColor(colDark)
                y = drawWrapped(pdf, tagStr, y, 13f)
                y -= 8f
            }
        }

        checkIn.mentalFeeling?.takeIf { it.isNotBlank() }?.let { text ->
            y = drawFeelingBlock(pdf, "Ressenti mental", text, y)
            y -= 8f
        }

        checkIn.physicalFeeling?.takeIf { it.isNotBlank() }?.let { text ->
            y = drawFeelingBlock(pdf, "Ressenti physique", text, y)
        }

        return y
    }

    private fun drawFeelingBlock(pdf: PdfWriter, title: String, text: String, startY: Float): Float {
        val indentX = marginL + 14f
        val indentW = contentW - 14f

        val textH = measureWrappedH(pdf, text, indentW, 10f, 14f)
        val blockH = 18f + textH

        var y = breakIfNeeded(pdf, startY, blockH)

        pdf.setColor(colAccent)
        pdf.fillRect(marginL, y - blockH, 3f, blockH)

        font(pdf, PdfWriter.FONT_HELVETICA_BOLD, 10f)
        pdf.setColor(colDark)
        pdf.drawText(indentX, y, title)
        y -= 16f

        font(pdf, PdfWriter.FONT_HELVETICA, 10f)
        pdf.setColor(colMid)
        y = drawWrappedAt(pdf, text, indentX, y, indentW, 14f)

        return y
    }

    // ── Nutrition ────────────────────────────────────────────────────────────

    private fun drawNutrition(pdf: PdfWriter, ns: NutritionSummary, startY: Float, options: PdfExportOptions): Float {
        val rows = buildNutritionRows(ns, options)
        val showDailyData = options.includeDailyData && ns.dailyData.isNotEmpty()

        val header = when {
            options.includeAverages && showDailyData ->
                "NUTRITION (moy. + détail sur ${ns.nutritionDays} jour${if (ns.nutritionDays > 1) "s" else ""})"
            options.includeAverages ->
                "NUTRITION (moy. sur ${ns.nutritionDays} jour${if (ns.nutritionDays > 1) "s" else ""})"
            showDailyData -> "NUTRITION — détail par jour"
            else -> "NUTRITION"
        }

        val sectionH = 20f + rows.size * 22f + if (options.includeAiSummary && !ns.aiSummary.isNullOrBlank()) 60f else 0f

        var y = breakIfNeeded(pdf, startY, sectionH.coerceAtMost(300f))
        y = drawSectionHeader(pdf, header, y)
        y -= 8f

        if (rows.isNotEmpty()) {
            y = drawTableRows(pdf, rows, y)
        }

        if (showDailyData) {
            y -= 10f
            y = drawDailyNutritionTable(pdf, ns, options, y)
        }

        if (options.includeAiSummary) {
            ns.aiSummary?.takeIf { it.isNotBlank() }?.let { summary ->
                y -= 10f
                y = breakIfNeeded(pdf, y, 40f)
                font(pdf, PdfWriter.FONT_HELVETICA_BOLD, 9f)
                pdf.setColor(colMid)
                pdf.drawText(marginL, y, "Résumé IA")
                y -= 14f
                font(pdf, PdfWriter.FONT_HELVETICA, 10f)
                pdf.setColor(colMid)
                y = drawWrapped(pdf, summary, y, 13f)
            }
        }

        return y
    }

    private fun drawDailyNutritionTable(
        pdf: PdfWriter,
        ns: NutritionSummary,
        options: PdfExportOptions,
        startY: Float,
    ): Float {
        if (ns.dailyData.isEmpty()) return startY

        val cols = buildList {
            add("Date" to 60f)
            if (options.includeCalories) add("Cal." to null)
            if (options.includeProtein) add("Prot." to null)
            if (options.includeCarbs) add("Gluc." to null)
            if (options.includeFat) add("Lip." to null)
            if (options.includeWater) add("Eau" to null)
        }

        val fixedW = cols.sumOf { it.second?.toDouble() ?: 0.0 }.toFloat()
        val nullCount = cols.count { it.second == null }
        val varW = if (nullCount > 0) (contentW - fixedW) / nullCount else 0f
        val colWidths = cols.map { it.second ?: varW }
        val colLabels = cols.map { it.first }

        val rowH = 18f

        var y = startY
        y = breakIfNeeded(pdf, y, rowH * 2)

        pdf.setColor(colRowAlt)
        pdf.fillRect(marginL, y - rowH + 4f, contentW, rowH)
        font(pdf, PdfWriter.FONT_HELVETICA_BOLD, 9f)
        pdf.setColor(colMid)

        var x = marginL + 4f
        for (i in colLabels.indices) {
            if (i == 0) {
                pdf.drawText(x, y - 4f, colLabels[i])
            } else {
                pdf.drawTextRightAligned(x + colWidths[i] - 8f, y - 4f, colLabels[i])
            }
            x += colWidths[i]
        }
        y -= rowH

        font(pdf, PdfWriter.FONT_HELVETICA, 9f)
        for ((idx, day) in ns.dailyData.withIndex()) {
            y = breakIfNeeded(pdf, y, rowH)
            if (idx % 2 == 1) {
                pdf.setColor(colRowAlt)
                pdf.fillRect(marginL, y - rowH + 4f, contentW, rowH)
            }

            val values = buildList {
                add(formatShortDate(day.date))
                if (options.includeCalories) add("${fmtVal(day.calories)} kcal")
                if (options.includeProtein) add("${fmtVal(day.protein)}g")
                if (options.includeCarbs) add("${fmtVal(day.carbs)}g")
                if (options.includeFat) add("${fmtVal(day.fat)}g")
                if (options.includeWater) add("${fmtWater(day.waterMl)}L")
            }

            x = marginL + 4f
            pdf.setColor(colDark)
            for (i in values.indices) {
                if (i == 0) {
                    pdf.drawText(x, y - 4f, values[i])
                } else {
                    pdf.drawTextRightAligned(x + colWidths[i] - 8f, y - 4f, values[i])
                }
                x += colWidths[i]
            }
            y -= rowH
        }

        return y
    }

    private fun formatShortDate(iso: String): String {
        val (day, month, _) = parseDate(iso)
        val monthShort = months.getOrElse((month - 1).coerceIn(0, 11)) { "" }.take(4)
        return "$day $monthShort"
    }

    // ── Shared table renderer ───────────────────────────────────────────────

    private fun drawTableRows(pdf: PdfWriter, rows: List<Pair<String, String>>, startY: Float): Float {
        val rowH = 22f
        var y = startY

        for ((index, row) in rows.withIndex()) {
            y = breakIfNeeded(pdf, y, rowH)

            if (index % 2 == 0) {
                pdf.setColor(colRowAlt)
                pdf.fillRect(marginL, y - rowH + 6f, contentW, rowH)
            }

            font(pdf, PdfWriter.FONT_HELVETICA, 11f)
            pdf.setColor(colDark)
            pdf.drawText(marginL + 8f, y - 4f, row.first)

            font(pdf, PdfWriter.FONT_HELVETICA_BOLD, 11f)
            pdf.drawTextRightAligned(pageW - marginR - 8f, y - 4f, row.second)

            y -= rowH
        }

        return y
    }

    // ── Footer ──────────────────────────────────────────────────────────────

    private fun drawFooter(pdf: PdfWriter) {
        val footerY = 38f
        pdf.setColor(colSep)
        pdf.drawLine(marginL, footerY + 14f, pageW - marginR, footerY + 14f)
        font(pdf, PdfWriter.FONT_HELVETICA, 8f)
        pdf.setColor(colLight)
        pdf.drawTextCentered(pageW / 2f, footerY, "Généré par Strakk")
    }

    // ── Section header ──────────────────────────────────────────────────────

    private fun drawSectionHeader(pdf: PdfWriter, title: String, startY: Float): Float {
        var y = startY
        font(pdf, PdfWriter.FONT_HELVETICA_BOLD, 10f)
        pdf.setColor(colMid)
        pdf.drawText(marginL, y, title)
        y -= 6f
        pdf.setColor(colSep)
        pdf.drawLine(marginL, y, pageW - marginR, y)
        return y
    }

    // ── Text helpers ────────────────────────────────────────────────────────

    private fun drawWrapped(pdf: PdfWriter, text: String, startY: Float, lineH: Float): Float =
        drawWrappedAt(pdf, text, marginL, startY, contentW, lineH)

    private fun drawWrappedAt(
        pdf: PdfWriter, text: String, x: Float, startY: Float, maxW: Float, lineH: Float,
    ): Float {
        val words = text.split(" ")
        var line = StringBuilder()
        var y = startY
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (pdf.measureText(candidate, fontSize) <= maxW) {
                line = StringBuilder(candidate)
            } else {
                if (line.isNotEmpty()) {
                    y = breakIfNeeded(pdf, y, lineH)
                    pdf.drawText(x, y, line.toString())
                    y -= lineH
                }
                line = StringBuilder(word)
            }
        }
        if (line.isNotEmpty()) {
            y = breakIfNeeded(pdf, y, lineH)
            pdf.drawText(x, y, line.toString())
            y -= lineH
        }
        return y
    }

    private fun measureWrappedH(
        pdf: PdfWriter, text: String, maxW: Float, textSize: Float, lineH: Float,
    ): Float {
        val words = text.split(" ")
        var line = StringBuilder()
        var lines = 0
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (pdf.measureText(candidate, textSize) <= maxW) {
                line = StringBuilder(candidate)
            } else {
                if (line.isNotEmpty()) lines++
                line = StringBuilder(word)
            }
        }
        if (line.isNotEmpty()) lines++
        return lines * lineH
    }

    // ── Page break ──────────────────────────────────────────────────────────

    private fun breakIfNeeded(pdf: PdfWriter, y: Float, needed: Float): Float =
        if (y - needed < marginB) {
            pdf.addPage()
            pageH - marginT
        } else {
            y
        }

    // ── Font / color helpers ────────────────────────────────────────────────

    private fun font(pdf: PdfWriter, name: String, size: Float) {
        fontSize = size
        pdf.setFont(name, size)
    }

    private fun PdfWriter.setColor(c: Triple<Float, Float, Float>) =
        setColor(c.first, c.second, c.third)

    // ── Data builders ───────────────────────────────────────────────────────

    private fun buildMeasurementRows(checkIn: CheckIn): List<Pair<String, String>> =
        listOfNotNull(
            checkIn.weight?.let { "Poids" to "${fmtVal(it)} kg" },
            checkIn.shoulders?.let { "Épaules" to "${fmtVal(it)} cm" },
            checkIn.chest?.let { "Poitrine" to "${fmtVal(it)} cm" },
            checkIn.armLeft?.let { "Bras gauche" to "${fmtVal(it)} cm" },
            checkIn.armRight?.let { "Bras droit" to "${fmtVal(it)} cm" },
            checkIn.waist?.let { "Tour de taille" to "${fmtVal(it)} cm" },
            checkIn.hips?.let { "Hanches" to "${fmtVal(it)} cm" },
            checkIn.thighLeft?.let { "Cuisse gauche" to "${fmtVal(it)} cm" },
            checkIn.thighRight?.let { "Cuisse droite" to "${fmtVal(it)} cm" },
        )

    private fun buildNutritionRows(ns: NutritionSummary, options: PdfExportOptions): List<Pair<String, String>> {
        if (!options.includeAverages) return emptyList()
        return listOfNotNull(
            if (options.includeCalories) "Calories" to "${fmtVal(ns.avgCalories)} kcal" else null,
            if (options.includeProtein) "Protéines" to "${fmtVal(ns.avgProtein)} g" else null,
            if (options.includeCarbs) "Glucides" to "${fmtVal(ns.avgCarbs)} g" else null,
            if (options.includeFat) "Lipides" to "${fmtVal(ns.avgFat)} g" else null,
            if (options.includeWater) "Eau" to "${fmtWater(ns.avgWater)} L" else null,
        )
    }

    // ── Formatters ──────────────────────────────────────────────────────────

    private fun fmtVal(value: Double): String {
        val r = (value * 10).toLong().toDouble() / 10.0
        return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
    }

    private fun fmtWater(ml: Int): String {
        val l = ml / 1000.0
        val r = (l * 10).toLong().toDouble() / 10.0
        return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
    }

    private fun formatWeekLabel(weekLabel: String): String {
        val parts = weekLabel.split("-W")
        return if (parts.size == 2) "Semaine ${parts[1]} — ${parts[0]}" else weekLabel
    }

    private fun buildSubtitle(checkIn: CheckIn): String {
        val dates = checkIn.coveredDates.sorted()
        if (dates.isEmpty()) return ""
        val count = dates.size
        val first = parseDate(dates.first())
        val last = parseDate(dates.last())
        val lastMonth = months.getOrElse((last.second - 1).coerceIn(0, 11)) { "" }
        val dayWord = if (count > 1) "jours" else "jour"
        return if (first.second == last.second && first.third == last.third) {
            "${first.first} au ${last.first} $lastMonth ${last.third} · $count $dayWord couverts"
        } else {
            val firstMonth = months.getOrElse((first.second - 1).coerceIn(0, 11)) { "" }
            "${first.first} $firstMonth au ${last.first} $lastMonth ${last.third} · $count $dayWord couverts"
        }
    }

    private fun parseDate(iso: String): Triple<Int, Int, Int> = try {
        val p = iso.split("-")
        Triple(p[2].toInt(), p[1].toInt(), p[0].toInt())
    } catch (_: Exception) {
        Triple(1, 1, 2000)
    }
}
