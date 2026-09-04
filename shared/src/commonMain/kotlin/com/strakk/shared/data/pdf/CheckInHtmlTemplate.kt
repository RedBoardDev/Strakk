@file:Suppress(
    "MagicNumber", "MaxLineLength", "TooManyFunctions", "LongMethod",
    "CyclomaticComplexMethod", "ArgumentListWrapping", "SpacingAroundComma",
    "SpacingAroundOperators", "SpacingAroundKeyword", "FunctionSignature",
    "Wrapping", "SpacingAroundColon", "SpacingAroundCurly",
    "FunctionStartOfBodySpacing", "ImportOrdering", "LongParameterList",
    "FunctionReturnTypeSpacing", "PropertyWrapping", "NoSemicolons",
    "StringTemplate", "Indentation", "ParameterListWrapping",
    "MultiLineIfElse", "VariableNaming", "ReturnCount",
)

package com.strakk.shared.data.pdf

import com.strakk.shared.domain.model.CheckIn
import com.strakk.shared.domain.model.CheckInDelta
import com.strakk.shared.domain.model.HevyWorkout
import com.strakk.shared.domain.model.NutritionGoals
import com.strakk.shared.domain.model.NutritionSummary
import com.strakk.shared.domain.model.PdfExportOptions
import com.strakk.shared.domain.model.WeeklyTrainingStats
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Pure-Kotlin HTML template builder for check-in PDF reports.
 * Matches the validated prototype design: warm orange accent (#E07C4F),
 * body measurement SVG, training stats, and links to Hevy workouts.
 */
internal class CheckInHtmlTemplate {

    @Suppress("LongParameterList")
    fun build(
        checkIn: CheckIn,
        delta: CheckInDelta?,
        trainingStats: WeeklyTrainingStats?,
        photoDataBase64: Map<String, String>,
        options: PdfExportOptions,
        weightHistory: List<Pair<String, Double>> = emptyList(),
        nutritionGoals: NutritionGoals? = null,
        measurementHistory: List<MeasurementSeries> = emptyList(),
    ): String = buildString {
        append("<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\"><style>")
        append(CSS)
        append("</style></head><body>")
        append("<div class=\"page\">")
        append(headerHtml(checkIn))
        if (options.includePhotos && photoDataBase64.isNotEmpty()) append(photosHtml(photoDataBase64))
        if (options.includeMeasurements) append(measurementsHtml(checkIn, delta, weightHistory))

        // Page 2 — feelings + nutrition together
        val hasPage2 = options.includeFeelings || options.includeNutrition
        if (hasPage2) append("<div class=\"pb\"></div>")
        if (options.includeFeelings) append(feelingsHtml(checkIn))
        if (options.includeNutrition) {
            append(nutritionHtml(checkIn, options, trainingStats, nutritionGoals))
        }

        // Measurement evolution + training stats flow after page 2 content.
        // The renderer adds another page only when the remaining space is insufficient.
        val hasMeasurementEvolution = options.includeMeasurements &&
            measurementHistory.any { series -> series.values.count { it != null } >= 2 }
        val hasTrainingContent = trainingStats != null && options.includeTraining
        val renderStatsPage = hasMeasurementEvolution || hasTrainingContent
        if (renderStatsPage) {
            append(statisticsPageHtml(trainingStats, options, measurementHistory))
        }
        append("</div>")
        append("</body></html>")
    }

    private fun headerHtml(c: CheckIn): String {
        val weekNum = c.weekLabel.substringAfter("-W", "")
        val year = c.weekLabel.substringBefore("-W", "")
        val title = if (weekNum.isNotEmpty()) "Semaine $weekNum — $year" else c.weekLabel
        val sub = if (c.coveredDates.isNotEmpty()) {
            val dateRange = formatDateRange(c.coveredDates)
            "$dateRange · ${c.coveredDates.size} jours couverts"
        } else ""
        return "<div class=\"bar\"></div><div class=\"over\">BILAN SEMAINE</div><div class=\"title\">$title</div><div class=\"sub\">$sub</div>"
    }

    private fun formatDateRange(dates: List<String>): String {
        if (dates.isEmpty()) return ""
        val months = arrayOf("", "janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre")
        val first = dates.min()
        val last = dates.max()
        val fDay = first.substringAfterLast("-").toIntOrNull() ?: return "$first — $last"
        val fMonth = first.substring(5, 7).toIntOrNull() ?: return "$first — $last"
        val lDay = last.substringAfterLast("-").toIntOrNull() ?: return "$first — $last"
        val lMonth = last.substring(5, 7).toIntOrNull() ?: return "$first — $last"
        val lYear = last.substring(0, 4)
        return if (fMonth == lMonth) {
            "$fDay au $lDay ${months[lMonth]} $lYear"
        } else {
            "$fDay ${months[fMonth]} au $lDay ${months[lMonth]} $lYear"
        }
    }

    private fun photosHtml(photos: Map<String, String>): String = buildString {
        append("<div class=\"sec\">Photos</div><div style=\"display:flex;gap:8px;justify-content:center;flex-wrap:wrap;\">")
        for ((_, b64) in photos) append("<img src=\"data:image/jpeg;base64,$b64\" class=\"photo\"/>")
        append("</div>")
    }

    private fun measurementsHtml(c: CheckIn, delta: CheckInDelta?, weightHistory: List<Pair<String, Double>>): String = buildString {
        data class R(val n: String, val v: Double?, val d: Double?, val u: String)
        val rows = listOf(R("Poids",c.weight,delta?.weight,"kg"),R("Épaules",c.shoulders,delta?.shoulders,"cm"),R("Poitrine",c.chest,delta?.chest,"cm"),R("Tour de taille",c.waist,delta?.waist,"cm"),R("Hanches",c.hips,delta?.hips,"cm"),R("Bras G",c.armLeft,delta?.armLeft,"cm"),R("Bras D",c.armRight,delta?.armRight,"cm"),R("Cuisse G",c.thighLeft,delta?.thighLeft,"cm"),R("Cuisse D",c.thighRight,delta?.thighRight,"cm")).filter { it.v != null }
        if (rows.isEmpty()) return@buildString
        append("<div class=\"sec\">Mesures</div><div style=\"display:flex;gap:6px;align-items:stretch;\">")

        // Left column: table + weight sparkline
        append("<div style=\"flex:1;display:flex;flex-direction:column;gap:8px;\"><div class=\"card\"><table><tbody>")
        for (r in rows) {
            append("<tr><td>${r.n}</td><td class=\"r b\">${fmt(r.v!!)} ${r.u}</td></tr>")
        }
        append("</tbody></table></div>")

        // Weight sparkline chart
        if (weightHistory.size >= 2) {
            append("<div class=\"sub-label\">Évolution du poids</div>")
            append("<div class=\"card\" style=\"padding:6px 8px;flex:1;display:flex;align-items:center;\">")
            append(weightSparklineSvg(weightHistory))
            append("</div>")
        }

        append("</div>")

        // Right column: body silhouette
        append("<div style=\"flex:0.85;display:flex;\">")
        append(bodyMeasureSvg(c))
        append("</div></div>")
    }

    private fun bodyMeasureSvg(c: CheckIn): String = buildString {
        append("<svg viewBox=\"-46 -4 124 102\" xmlns=\"http://www.w3.org/2000/svg\" style=\"display:block;width:100%;height:100%;\" preserveAspectRatio=\"xMidYMid meet\">")
        append(BODY_SVG)
        c.shoulders?.let { tape(this,1.0,14.0,30.0,2.5,-14.0,13.5,17.0,"Épaules",fmt(it),true) }
        c.chest?.let { tape(this,4.5,19.5,23.0,2.5,45.0,19.0,22.5,"Poitrine",fmt(it),false) }
        c.armLeft?.let { tape(this,0.5,27.0,7.0,2.0,-14.0,26.5,30.0,"Bras G",fmt(it),true) }
        c.armRight?.let { tape(this,24.5,27.0,7.0,2.0,45.0,26.5,30.0,"Bras D",fmt(it),false) }
        c.waist?.let { tape(this,8.5,31.5,15.0,2.5,-14.0,36.5,40.0,"Taille",fmt(it),true) }
        c.hips?.let { tape(this,7.5,44.5,17.0,2.5,45.0,44.0,47.5,"Hanches",fmt(it),false) }
        c.thighLeft?.let { tape(this,6.0,52.5,9.0,2.0,-14.0,52.0,55.5,"Cuisse G",fmt(it),true) }
        c.thighRight?.let { tape(this,17.0,52.5,9.0,2.0,45.0,52.0,55.5,"Cuisse D",fmt(it),false) }
        append("</svg>")
    }

    private fun tape(sb: StringBuilder,x:Double,y:Double,w:Double,h:Double,lx:Double,vy:Double,ny:Double,name:String,value:String,left:Boolean) {
        val a=if(left)"end"else"start"; val ex=if(left)x else x+w; val tx=lx+(if(left)1 else -1)
        sb.append("<rect x=\"$x\" y=\"$y\" rx=\"0.5\" width=\"$w\" height=\"$h\" fill=\"#FDD835\" opacity=\"0.7\"/>")
        sb.append("<path d=\"M$x,$y Q${x-2},${y+h/2} $x,${y+h}\" fill=\"none\" stroke=\"#F9A825\" stroke-width=\"0.2\"/>")
        sb.append("<path d=\"M${x+w},$y Q${x+w+2},${y+h/2} ${x+w},${y+h}\" fill=\"none\" stroke=\"#F9A825\" stroke-width=\"0.2\"/>")
        sb.append("<line x1=\"$ex\" y1=\"${y+h/2}\" x2=\"$tx\" y2=\"${y+h/2}\" stroke=\"#C68A2E\" stroke-width=\"0.2\" opacity=\"0.5\"/>")
        sb.append("<text x=\"$lx\" y=\"$vy\" font-size=\"3.2\" fill=\"#222\" font-weight=\"700\" text-anchor=\"$a\">$value</text>")
        sb.append("<text x=\"$lx\" y=\"$ny\" font-size=\"2.2\" fill=\"#999\" text-anchor=\"$a\">$name</text>")
    }

    private fun weightSparklineSvg(history: List<Pair<String, Double>>): String = buildString {
        if (history.size < 2) return@buildString
        val minW = history.minOf { it.second } - 0.5
        val maxW = history.maxOf { it.second } + 0.5
        val range = (maxW - minW).coerceAtLeast(1.0)
        val w = 200.0; val h = 50.0; val padL = 10.0; val padR = 5.0; val padT = 8.0; val padB = 12.0
        val chartW = w - padL - padR; val chartH = h - padT - padB
        fun xFor(i: Int) = padL + (i.toDouble() / (history.size - 1)) * chartW
        fun yFor(v: Double) = padT + (1.0 - (v - minW) / range) * chartH

        append("<svg viewBox=\"0 0 200 50\" width=\"100%\" height=\"100%\" preserveAspectRatio=\"xMidYMid meet\">")
        // Grid lines
        val steps = 4
        for (s in 0..steps) {
            val y = padT + s.toDouble() / steps * chartH
            val v = maxW - s.toDouble() / steps * range
            append("<line x1=\"$padL\" y1=\"$y\" x2=\"${w - padR}\" y2=\"$y\" stroke=\"#F5F5F5\" stroke-width=\"0.3\"/>")
            append("<text x=\"${padL - 2}\" y=\"${y + 1.5}\" font-size=\"3.5\" fill=\"#CCC\" text-anchor=\"end\">${fmt(v)}</text>")
        }
        // Area fill
        val pts = history.mapIndexed { i, (_, v) -> "${xFor(i)},${yFor(v)}" }.joinToString(" ")
        val lastX = xFor(history.size - 1)
        append("<polyline fill=\"rgba(224,124,79,0.05)\" stroke=\"none\" points=\"$pts $lastX,${padT + chartH} ${xFor(0)},${padT + chartH}\"/>")
        // Line
        append("<polyline fill=\"none\" stroke=\"#E07C4F\" stroke-width=\"1.2\" stroke-linejoin=\"round\" points=\"$pts\"/>")
        // Points + labels
        for ((i, pair) in history.withIndex()) {
            val (label, v) = pair
            val x = xFor(i); val y = yFor(v)
            val isLast = i == history.size - 1
            val wkLabel = label.substringAfter("-W", label).let { "S$it" }
            if (isLast) {
                append("<circle cx=\"$x\" cy=\"$y\" r=\"2.5\" fill=\"#E07C4F\"/>")
                append("<text x=\"$x\" y=\"${y - 3}\" text-anchor=\"middle\" font-size=\"4.5\" fill=\"#E07C4F\" font-weight=\"700\">${fmt(v)}</text>")
                append("<text x=\"$x\" y=\"${padT + chartH + 6}\" text-anchor=\"middle\" font-size=\"3.5\" fill=\"#E07C4F\" font-weight=\"600\">$wkLabel</text>")
            } else if (i % 2 == 0 || history.size <= 5) {
                append("<circle cx=\"$x\" cy=\"$y\" r=\"1.2\" fill=\"#E07C4F\" opacity=\"0.3\"/>")
                append("<text x=\"$x\" y=\"${y - 2}\" text-anchor=\"middle\" font-size=\"4\" fill=\"#999\">${fmt(v)}</text>")
                append("<text x=\"$x\" y=\"${padT + chartH + 6}\" text-anchor=\"middle\" font-size=\"3.5\" fill=\"#CCC\">$wkLabel</text>")
            }
        }
        append("</svg>")
    }

    private fun feelingsHtml(c: CheckIn): String = buildString {
        if (c.feelingTags.isEmpty() && c.mentalFeeling.isNullOrBlank() && c.physicalFeeling.isNullOrBlank()) return@buildString
        append("<div class=\"sec\">Ressentis</div>")
        if (c.feelingTags.isNotEmpty()) {
            val pos=c.feelingTags.filter{it in POS}; val neg=c.feelingTags.filter{it !in POS}
            if(pos.isNotEmpty()) append("<div class=\"feel-row\"><div class=\"fl fl-p\">Sensations positives</div><div class=\"ftags\">${pos.joinToString(" · "){tag(it)}}</div></div>")
            if(neg.isNotEmpty()) append("<div class=\"feel-row\"><div class=\"fl fl-n\">Sensations négatives</div><div class=\"ftags\">${neg.joinToString(" · "){tag(it)}}</div></div>")
        }
        if(!c.mentalFeeling.isNullOrBlank()||!c.physicalFeeling.isNullOrBlank()){
            append("<div class=\"fq-wrap\">")
            c.mentalFeeling?.takeIf{it.isNotBlank()}?.let{append("<div class=\"fq\"><div class=\"fq-t\">Ressenti mental</div><div class=\"fq-b\">${esc(it)}</div></div>")}
            c.physicalFeeling?.takeIf{it.isNotBlank()}?.let{append("<div class=\"fq\"><div class=\"fq-t\">Ressenti physique</div><div class=\"fq-b\">${esc(it)}</div></div>")}
            append("</div>")
        }
    }

    private fun nutritionHtml(
        c: CheckIn,
        o: PdfExportOptions,
        training: WeeklyTrainingStats?,
        goals: NutritionGoals?,
    ): String = buildString {
        val s=c.nutritionSummary ?: return@buildString
        append("<div class=\"sec\">Nutrition</div>")
        val adherenceHtml = goals?.let { macroAdherenceHtml(s, it, o) }.orEmpty()
        if(o.includeAverages || adherenceHtml.isNotEmpty()){
            append("<div class=\"nutrition-overview\">")
        }
        if(o.includeAverages){
            append("<div class=\"nutrition-panel\"><div class=\"sub-label\">Moyennes sur ${s.nutritionDays} jours</div><div class=\"card\"><table><tbody>")
            if(o.includeCalories)append("<tr><td>Calories</td><td class=\"r b\">${s.avgCalories.toInt()} kcal</td></tr>")
            if(o.includeProtein)append("<tr><td>Protéines</td><td class=\"r b\">${s.avgProtein.toInt()} g</td></tr>")
            if(o.includeCarbs)append("<tr><td>Glucides</td><td class=\"r b\">${s.avgCarbs.toInt()} g</td></tr>")
            if(o.includeFat)append("<tr><td>Lipides</td><td class=\"r b\">${s.avgFat.toInt()} g</td></tr>")
            if(o.includeWater)append("<tr><td>Eau</td><td class=\"r b\">${fmtW(s.avgWater.toDouble())}</td></tr>")
            append("</tbody></table></div></div>")
        }
        if (adherenceHtml.isNotEmpty()) {
            append("<div class=\"nutrition-panel\"><div class=\"sub-label\">Adhérence macros</div>")
            append(adherenceHtml)
            append("</div>")
        }
        if(o.includeAverages || adherenceHtml.isNotEmpty()){
            append("</div>")
        }
        if(o.includeDailyData && s.dailyData.isNotEmpty()){
            append("<div style=\"break-inside:avoid;\"><div class=\"sub-label\" style=\"margin-top:10px;\">Détail par jour</div><div class=\"card\"><table><thead><tr><th>Jour</th>")
            if(o.includeCalories)append("<th class=\"r\">Cal.</th>")
            if(o.includeProtein)append("<th class=\"r\">Prot.</th>")
            if(o.includeCarbs)append("<th class=\"r\">Gluc.</th>")
            if(o.includeFat)append("<th class=\"r\">Lip.</th>")
            if(o.includeWater)append("<th class=\"r\">Eau</th>")
            append("</tr></thead><tbody>")
            val trainingDates = training?.workouts?.map { it.date }?.toSet() ?: emptySet()
            for(d in s.dailyData){
                val isTrainDay = d.date in trainingDates
                val cls = if (isTrainDay) " class=\"train-day\"" else ""
                val icon = if (isTrainDay) DUMBBELL_SVG else EMPTY_ICON
                append("<tr$cls><td>$icon${d.date}</td>")
                if(o.includeCalories)append("<td class=\"r\">${d.calories.toInt()}</td>")
                if(o.includeProtein)append("<td class=\"r\">${d.protein.toInt()} g</td>")
                if(o.includeCarbs)append("<td class=\"r\">${d.carbs.toInt()} g</td>")
                if(o.includeFat)append("<td class=\"r\">${d.fat.toInt()} g</td>")
                if(o.includeWater)append("<td class=\"r\">${fmtW(d.waterMl.toDouble())}</td>")
                append("</tr>")
            }
            append("</tbody></table>")
            if (trainingDates.isNotEmpty()) {
                append("<div class=\"legend\">$DUMBBELL_SVG Jour d'entraînement</div>")
            }
            append("</div></div>")
        }
    }

    private fun macroAdherenceHtml(
        summary: NutritionSummary,
        goals: NutritionGoals,
        options: PdfExportOptions,
    ): String = buildString {
        data class MacroBar(
            val name: String,
            val average: Double,
            val goal: Int?,
            val color: String,
            val included: Boolean,
        )

        val macros = listOf(
            MacroBar("Calories", summary.avgCalories, goals.calorieGoal, "#E07C4F", options.includeCalories),
            MacroBar("Protéines", summary.avgProtein, goals.proteinGoal, "#637CFF", options.includeProtein),
            MacroBar("Glucides", summary.avgCarbs, goals.carbGoal, "#FFC84D", options.includeCarbs),
            MacroBar("Lipides", summary.avgFat, goals.fatGoal, "#4DAE6A", options.includeFat),
            MacroBar("Eau", summary.avgWater.toDouble(), goals.waterGoal, "#4B8DFF", options.includeWater),
        ).filter { it.included && it.goal != null && it.goal > 0 }

        if (macros.isEmpty()) return@buildString

        append("<div class=\"card macro-card\">")
        val percentages = macros.map { macro -> adherencePercent(macro.average, macro.goal!!) }
        macros.zip(percentages).forEach { (macro, percentage) ->
            append("<div class=\"sel-mb-row\"><span class=\"sel-mb-name\">${macro.name}</span>")
            append("<div class=\"sel-mb-track\"><div class=\"sel-mb-fill\" ")
            append("style=\"width:${percentage}%;background:${macro.color};\"></div></div>")
            append("<span class=\"sel-mb-pct\">$percentage%</span></div>")
        }
        append("<div class=\"macro-average\">Moyenne : <b>${percentages.average().toInt()}%</b></div>")
        append("</div>")
    }

    private fun adherencePercent(average: Double, goal: Int): Int {
        val distanceRatio = abs(average - goal.toDouble()) / goal
        return ((1.0 - distanceRatio) * 100).toInt().coerceIn(0, 100)
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun statisticsPageHtml(
        st: WeeklyTrainingStats?,
        options: PdfExportOptions,
        measurementHistory: List<MeasurementSeries>,
    ): String = buildString {
        append("<div class=\"sec\">Statistiques</div>")
        if (measurementHistory.isNotEmpty()) {
            append(measurementSparklines(measurementHistory))
        }
        val hasMuscleVolume = st != null &&
            options.includeMuscleVolume &&
            st.muscleGroupVolume.isNotEmpty()
        val hasTrainingContent = options.includeTrainingSummary ||
            hasMuscleVolume ||
            options.includeWorkoutDetails
        if (st != null && hasTrainingContent) {
            append("<div class=\"fin-row\">")
            if (hasMuscleVolume || options.includeWorkoutDetails) {
                append("<div class=\"fin-col\" style=\"flex:1;\">")
                if (hasMuscleVolume) {
                    append(pushPullLegsDonut(st.muscleGroupVolume))
                }
                if (options.includeWorkoutDetails) {
                    append(workoutLinksHtml(st.workouts))
                }
                append("</div>")
            }
            if (options.includeTrainingSummary || hasMuscleVolume) {
                append("<div class=\"fin-col\" style=\"flex:1;display:flex;flex-direction:column;gap:0;\">")
                if (options.includeTrainingSummary) {
                    val h = st.totalDurationMinutes / 60; val m = st.totalDurationMinutes % 60
                    val dur = if (h > 0) "$h <span class=\"fin-ts-unit\">h</span> $m" else "$m <span class=\"fin-ts-unit\">min</span>"
                    val (volNum, volUnit) = fmtVolume(st.totalVolumeKg)
                    val vol = "$volNum <span class=\"fin-ts-unit\">$volUnit</span>"
                    val totalExercises = st.workouts.sumOf { it.exercises.size }
                    append("<div class=\"sub-label\">Entraînement</div>")
                    append("<div class=\"fin-train-grid two\">")
                    // icon + value layout for each stat block
                    append("<div class=\"fin-train-stat\"><div class=\"fin-ts-val accent\">${st.totalSessions}</div><div class=\"fin-ts-label\">Séances</div></div>")
                    append("<div class=\"fin-train-stat\"><div class=\"fin-ts-val accent\">$dur</div><div class=\"fin-ts-label\">Durée</div></div>")
                    append("<div class=\"fin-train-stat\"><div class=\"fin-ts-val accent\">$vol</div><div class=\"fin-ts-label\">Volume</div></div>")
                    append("<div class=\"fin-train-stat\"><div class=\"fin-ts-val accent\">$totalExercises</div><div class=\"fin-ts-label\">Exercices</div></div>")
                    append("</div>")
                }
                if (hasMuscleVolume) {
                    val sorted = st.muscleGroupVolume.entries.sortedByDescending { it.value }
                    val mx = sorted.first().value
                    append("<div class=\"sub-label\">Volume par groupe musculaire</div>")
                    append("<div class=\"card\" style=\"padding:8px 10px;\">")
                    for ((mu, v) in sorted) {
                        val p = if (mx > 0) ((v / mx) * 100).toInt() else 0
                        append("<div class=\"mb\"><span class=\"mb-l\">${cap(mu)}</span><span class=\"mb-t\"><span class=\"mb-f\" style=\"width:${p}%\"></span></span><span class=\"mb-p\">${fmtV(v)}</span></div>")
                    }
                    append("</div>")
                }
                append("</div>")
            }
            append("</div>")
        }
    }

    private fun workoutLinksHtml(workouts: List<HevyWorkout>): String = buildString {
        val linkedWorkouts = workouts.mapNotNull { workout ->
            hevyWorkoutUrl(workout.id)?.let { url -> workout to url }
        }
        if (linkedWorkouts.isEmpty()) return@buildString

        append("<div class=\"sub-label\">Séances Hevy</div><div class=\"card workout-links\">")
        for ((workout, url) in linkedWorkouts) {
            append("<a class=\"workout-link\" href=\"$url\" data-pdf-link-text=\"Ouvrir dans Hevy\">")
            append("<span><b>${esc(workout.title)}</b><small>${workout.date} · ${workout.durationMinutes} min</small></span>")
            append("<span class=\"workout-link-action\">Ouvrir dans Hevy ↗</span></a>")
        }
        append("</div>")
    }

    private fun hevyWorkoutUrl(workoutId: String): String? = workoutId
        .takeIf { HEVY_WORKOUT_ID.matches(it) }
        ?.let { "https://hevy.com/workout/$it" }

    /** Small multiples with an absolute scale for each measurement. */
    @Suppress("MagicNumber")
    private fun measurementSparklines(measurements: List<MeasurementSeries>): String = buildString {
        val series = measurements.filter { ms -> ms.values.count { it != null } >= 2 }
        if (series.isEmpty()) return@buildString

        val colors = listOf("#E07C4F", "#637CFF", "#4DAE6A", "#FFC84D", "#E05252", "#B07AAF", "#27B0C4")
        append("<div class=\"sub-label\">Évolution des mesures</div>")
        append("<div class=\"measurement-grid\">")
        series.forEachIndexed { idx, ms ->
            val color = colors[idx % colors.size]
            val cur = ms.values.lastOrNull { it != null }
            append("<div class=\"measure-chart\"><div class=\"measure-chart-head\">")
            append("<b>${ms.label}</b><span>${cur?.let { "${fmt(it)} ${ms.unit}" }.orEmpty()}")
            ms.delta?.let { delta ->
                val sign = if (delta > 0.0) "+" else ""
                append(" <small>$sign${fmt(delta)} ${ms.unit}</small>")
            }
            append("</span></div>")
            append(measurementSparklineSvg(ms, color))
            append("</div>")
        }
        append("</div>")
    }

    @Suppress("MagicNumber")
    private fun measurementSparklineSvg(series: MeasurementSeries, color: String): String = buildString {
        val points = series.values.mapIndexedNotNull { index, value -> value?.let { index to it } }
        if (points.size < 2) return@buildString

        val width = 160.0
        val height = 58.0
        val plotLeft = 27.0
        val plotRight = 157.0
        val plotTop = 5.0
        val plotBottom = 43.0
        val rawMin = points.minOf { it.second }
        val rawMax = points.maxOf { it.second }
        val padding = max((rawMax - rawMin) * 0.15, 0.5)
        val scaleMin = rawMin - padding
        val scaleMax = rawMax + padding
        val range = scaleMax - scaleMin
        val lastIndex = (series.values.size - 1).coerceAtLeast(1)
        fun xOf(index: Int) = plotLeft + index.toDouble() / lastIndex * (plotRight - plotLeft)
        fun yOf(value: Double) = plotBottom - (value - scaleMin) / range * (plotBottom - plotTop)

        append("<svg viewBox=\"0 0 $width $height\" class=\"measure-chart-svg\">")
        for (ratio in listOf(0.0, 0.5, 1.0)) {
            val y = plotTop + ratio * (plotBottom - plotTop)
            append("<line x1=\"$plotLeft\" y1=\"${fmtD(y)}\" x2=\"$plotRight\" y2=\"${fmtD(y)}\" ")
            append("stroke=\"#EBEBEB\" stroke-width=\"0.6\"/>")
        }
        append("<text x=\"${plotLeft - 3}\" y=\"${plotTop + 2}\" text-anchor=\"end\">${fmt(scaleMax)}</text>")
        append("<text x=\"${plotLeft - 3}\" y=\"${plotBottom + 2}\" text-anchor=\"end\">${fmt(scaleMin)}</text>")
        val polyline = points.joinToString(" ") { (index, value) ->
            "${fmtD(xOf(index))},${fmtD(yOf(value))}"
        }
        append("<polyline points=\"$polyline\" fill=\"none\" stroke=\"$color\" stroke-width=\"1.6\" ")
        append("stroke-linejoin=\"round\" stroke-linecap=\"round\"/>")
        points.forEach { (index, value) ->
            append("<circle cx=\"${fmtD(xOf(index))}\" cy=\"${fmtD(yOf(value))}\" r=\"1.5\" fill=\"$color\"/>")
        }
        val firstWeek = series.weekLabels.firstOrNull().orEmpty()
        val lastWeek = series.weekLabels.lastOrNull().orEmpty()
        append("<text x=\"$plotLeft\" y=\"54\" text-anchor=\"start\">$firstWeek</text>")
        append("<text x=\"$plotRight\" y=\"54\" text-anchor=\"end\">$lastWeek</text>")
        append("</svg>")
    }

    // ── Graph B: Push / Pull / Legs donut ─────────────────────────────────

    private fun classifyMuscle(group: String): String {
        val g = group.lowercase()
        return when {
            g.contains("chest") || g.contains("shoulder") || g.contains("tricep") ||
                g.contains("pec") || g.contains("delt") -> "Poussé"
            g.contains("back") || g.contains("bicep") || g.contains("lat") ||
                g.contains("row") || g.contains("pull") || g.contains("trap") -> "Tiré"
            g.contains("leg") || g.contains("quad") || g.contains("hamstring") ||
                g.contains("glute") || g.contains("calf") || g.contains("calve") ||
                g.contains("hip") -> "Jambes"
            else -> "Autre"
        }
    }

    private fun pushPullLegsDonut(muscleGroupVolume: Map<String, Double>): String = buildString {
        data class Slice(val label: String, val value: Double, val color: String)
        val buckets = mutableMapOf("Poussé" to 0.0, "Tiré" to 0.0, "Jambes" to 0.0, "Autre" to 0.0)
        for ((g, v) in muscleGroupVolume) buckets[classifyMuscle(g)] = buckets.getValue(classifyMuscle(g)) + v
        val slices = listOf(
            Slice("Poussé", buckets["Poussé"]!!, "#E07C4F"),
            Slice("Tiré", buckets["Tiré"]!!, "#637CFF"),
            Slice("Jambes", buckets["Jambes"]!!, "#4DAE6A"),
            Slice("Autre", buckets["Autre"]!!, "#CCCCCC"),
        ).filter { it.value > 0 }
        val total = slices.sumOf { it.value }
        if (total == 0.0) return@buildString

        val cx = 50.0; val cy = 50.0; val outerR = 42.0; val innerR = 24.0
        fun pt(r: Double, a: Double) = fmtD(cx + r * cos(a)) + " " + fmtD(cy + r * sin(a))
        fun arcPath(r: Double, a1: Double, a2: Double, sweep: Int): String {
            val large = if (a2 - a1 > PI) 1 else 0
            return "A $r $r 0 $large $sweep ${pt(r, a2)}"
        }

        append("<div class=\"sub-label\">Répartition Push / Pull / Legs</div>")
        append("<div class=\"card\" style=\"padding:8px 10px;\">")
        append("<div style=\"display:flex;align-items:center;gap:10px;\">")

        // Donut SVG
        append("<svg viewBox=\"0 0 100 100\" width=\"80\" height=\"80\" style=\"flex-shrink:0;\">")
        var angle = -PI / 2
        for (sl in slices) {
            val sweep = sl.value / total * 2 * PI
            val end = angle + sweep * 0.998 // tiny gap between sectors
            append("<path d=\"M ${pt(outerR, angle)} ${arcPath(outerR, angle, end, 1)} ")
            append("L ${pt(innerR, end)} ${arcPath(innerR, end, angle, 0)} Z\" fill=\"${sl.color}\"/>")
            angle += sweep
        }
        append("</svg>")

        // Legend
        append("<div style=\"display:flex;flex-direction:column;gap:5px;\">")
        for (sl in slices) {
            val pct = (sl.value / total * 100).toInt()
            val (vn, vu) = fmtVolume(sl.value)
            append("<div style=\"display:flex;align-items:center;gap:5px;font-size:8px;color:#444;\">")
            append("<span style=\"display:inline-block;width:10px;height:10px;border-radius:2px;background:${sl.color};\"></span>")
            append("<span>${sl.label}</span>")
            append("<span style=\"margin-left:auto;font-weight:700;color:#222;\">$pct%</span>")
            append("<span style=\"color:#AAA;font-size:7px;\"> $vn $vu</span>")
            append("</div>")
        }
        append("</div>") // legend
        append("</div>") // flex row
        append("</div>") // card
    }

    @Suppress("UnusedPrivateMember", "FunctionOnlyReturningConstant")
    private fun sparklineSvg(values: List<Double?>): String = ""

    @Suppress("MaxLineLength")
    private val DUMBBELL_SVG = """<svg class="train-icon" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path fill="#E07C4F" stroke="#E07C4F" stroke-width="0.8" stroke-linejoin="round" d="M20.57 14.86L22 13.43 20.57 12 17 15.57 8.43 7 12 3.43 10.57 2 9.14 3.43 7.71 2 5.57 4.14 4.14 2.71 2.71 4.14l1.43 1.43L2 7.71l1.43 1.43L2 10.57 3.43 12 7 8.43 15.57 17 12 20.57 13.43 22l1.43-1.43L16.29 22l2.14-2.14 1.43 1.43 1.43-1.43-1.43-1.43L22 16.29z"/></svg>"""

    private val EMPTY_ICON = """<span class="train-icon-empty"></span>"""

    private fun fmt(v:Double):String=if(v==v.toLong().toDouble())v.toLong().toString()else fmtD(v)
    private fun fmtD(v:Double):String=((v*10).toLong()/10.0).toString()
    private fun fmtV(kg:Double):String=if(kg>=1000)"${fmtD(kg/1000)}k"else fmt(kg)

    /** Pretty volume: < 1 t → "X kg" ; ≥ 1 t → "X.x t". Returned as (number, unit). */
    private fun fmtVolume(kg:Double):Pair<String,String> =
        if (kg >= 1000.0) fmtD(kg / 1000.0) to "t" else fmt(kg) to "kg"
    private fun fmtW(ml:Double):String{val l=ml/1000.0;return if(l>=1.0)"${fmtD(l)} L"else"${ml.toInt()} ml"}
    private fun cap(s:String):String=s.replaceFirstChar{if(it.isLowerCase())it.titlecase()else it.toString()}
    private fun esc(t:String):String=t.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
    private fun tag(s:String):String=TAGS[s]?:s.replace("_"," ")
    private val HEVY_WORKOUT_ID = Regex("^[A-Za-z0-9-]{8,64}$")
    private val POS=setOf("energy_stable","good_energy","motivated","disciplined","good_sleep","good_recovery","strong_training","good_mood","focused","light_body","good_digestion")
    private val TAGS=mapOf(
        "energy_stable" to "Énergie stable","good_energy" to "Bonne énergie","motivated" to "Motivé","disciplined" to "Discipliné",
        "good_sleep" to "Bien dormi","good_recovery" to "Bonne récupération","strong_training" to "Séances solides",
        "good_mood" to "Bonne humeur","focused" to "Concentré","light_body" to "Corps léger","good_digestion" to "Bonne digestion",
        "low_energy" to "Manque d'énergie","tired" to "Fatigue","poor_sleep" to "Mal dormi","stress" to "Stress",
        "low_motivation" to "Manque de motivation","heavy_body" to "Corps lourd","sore" to "Courbatures",
        "joint_discomfort" to "Gêne articulaire","digestion_discomfort" to "Troubles digestifs",
        "bloating" to "Ballonnements","hungry" to "Faim","irritability" to "Irritabilité","low_mood" to "Moral bas",
    )

    @Suppress("MaxLineLength")
    private val BODY_SVG="""
<path fill="#1A1A1A" d="m 11.671635,6.3585449 -0.0482,-2.59085 4.20648,-2.46806 4.42769,2.95361 -0.0405,1.94408 0.24197,-3.34467 -2.03129,-2.31103004 -2.84508,-0.51629 -2.20423,0.52915 -1.9363,2.63077004 z"/><path fill="#1A1A1A" d="m 19.748825,6.7034949 0.0203,-2.20747 -3.96689,-2.7637 -3.74099,2.23559 -0.006,2.63528 -0.60741,0.0403 0.27408,1.82447 0.97635,0.33932 0.44244,2.1802901 1.82222,2.06556 2.03518,-0.0607 1.79223,-1.94408 0.35957,-2.2406601 0.97616,-0.33932 0.25159,-1.78416 z"/><path fill="#1A1A1A" d="m 13.304665,11.910505 1.64975,2.35202 0.74426,2.62159 -1.73486,-1.38354 -0.86649,-2.97104 z"/><path fill="#1A1A1A" d="m 18.385135,11.910505 -1.64975,2.35202 -0.74538,2.62234 1.73486,-1.38354 0.86649,-2.97104 z"/><path fill="#1A1A1A" d="m 19.047795,13.248365 3.55748,1.97916 0.72653,-0.35074 z m -0.107,0.43288 -0.37119,1.73073 2.1846,0.53561 1.40116,-0.49436 z"/><path fill="#1A1A1A" d="m 22.922305,15.657195 0.75814,-0.41 2.40806,1.66799 1.17364,1.50707 0.62662,1.5626 -0.0464,3.70194 -1.3284,-1.72153 0.0407,-2.59376 -0.48842,-0.50049 c 0,0 -3.09778,-3.19058 -3.14371,-3.21401 z m -0.2409,0.10873 c -0.001,0.0525 3.32987,3.54733 3.32987,3.54733 l 0.10067,3.10396 -1.15426,-1.97782 -2.22547,-0.94804 -1.56576,-2.88481 z"/><path fill="#1A1A1A" d="m 12.624785,13.248365 -3.5574599,1.97916 -0.72653,-0.35074 z m 0.107,0.43288 0.37119,1.73073 -2.18459,0.53561 -1.4011499,-0.49436 z"/><path fill="#1A1A1A" d="m 8.7502951,15.657195 -0.75814,-0.41 -2.40806,1.66799 -1.17364,1.50707 -0.62662,1.56259 0.0464,3.70195 1.3284,-1.72153 -0.0407,-2.59376 0.48843,-0.5005 c 0,0 3.09777,-3.19057 3.1437,-3.214 z m 0.2409,0.10873 c 0.002,0.0525 -3.32987,3.54733 -3.32987,3.54733 l -0.10067,3.10396 1.15426,-1.97782 2.22547,-0.94804 1.5657499,-2.88481 z"/><path fill="#1A1A1A" d="m 27.621665,30.814715 -0.33838,1.70499 -1.81932,-2.54418 -0.6629,-1.26895 z m -2.85271,-2.6096 c -0.0259,-0.0144 -0.0536,-0.0254 -0.0824,-0.0324 l -1.48333,-4.95503 1.00456,-2.08428 1.65511,1.74532 2.23034,6.67667 0.0415,0.93739 c -1.06528,-0.84215 -2.18962,-1.60679 -3.36434,-2.28803 z m 1.6945,-5.75654 1.64893,6.43421 -0.36469,-4.92266 z"/><path fill="#1A1A1A" d="m 26.955425,32.969125 1.30083,10.28927 -1.10778,0.01 -1.89387,-7.99609 0.19174,-4.53719 z m 1.21978,-1.94971 -0.58729,2.58635 1.11876,9.15614 0.55849,-0.21663 0.2304,-6.77018 z"/><path fill="#1A1A1A" d="m 4.0746451,30.814715 0.33838,1.70499 1.81931,-2.54418 0.66289,-1.26895 z m 2.8527,-2.6096 c 0.0259,-0.0144 0.0536,-0.0254 0.0824,-0.0324 l 1.48332,-4.95503 -1.00455,-2.08428 -1.65509,1.74532 -2.23034,6.67667 -0.0415,0.93739 c 1.06528,-0.84215 2.18961,-1.60679 3.36433,-2.28803 z m -1.6945,-5.75654 -1.64891,6.43421 0.36468,-4.92266 z"/><path fill="#1A1A1A" d="m 4.5752651,32.969125 -1.30083,10.28927 1.10778,0.01 1.89387,-7.99609 -0.19174,-4.53719 z m -1.21978,-1.94971 0.58728,2.58635 -1.11875,9.15614 -0.55849,-0.21663 -0.2304,-6.77018 z"/><path fill="#1A1A1A" d="m 20.337455,17.085495 1.72942,3.09103 1.890,0.94 -0.5,0.3 -6.8, -2.1 z"/><path fill="#1A1A1A" d="m 16.66,19.72 6.8,2.1 -0.65,0.5 -0.90604,2.63773 -2.09968,0.86537 -3.34524,-1.655 0.2,-3.8 z"/><path fill="#1A1A1A" d="m 11.351215,17.085495 -1.7294199,3.09103 -1.890,0.94 0.5,0.3 6.8,-2.1 z"/><path fill="#1A1A1A" d="m 15.03,19.72 -6.8,2.1 0.65,0.5 0.90586,2.63773 2.0996699,0.86537 3.34636,-1.655 -0.2,-3.8 z"/><path fill="#1A1A1A" d="m 19.641935,34.707615 1.81341,-1.36479 0.15748,1.83347 1.28642,2.37338 -1.98044,2.73652 -1.03109,0.16554 -0.37026,-3.88816 z"/><path fill="#1A1A1A" d="M 19.289,26.152 l -3.11202 -1.40604 0.0937 2.27965 2.80119 1.43603 z M 21.224,27.820 l -1.29355 0.7212 0.14997 -1.70898 z M 20.171,26.183 l 2.47968 -1.03241 -0.9336 2.52093 z M 21.702,27.921 l -1.69005 1.03372 -0.28871 2.0678 1.64975 -1.07533 z"/><path fill="#1A1A1A" d="M 18.791,29.025 l -0.0622 1.62387 -2.30308 -0.49961 -0.12448 -2.21722 z M 18.635,31.429 l 0.0311 1.99844 -2.20953 0.59391 -0.0311 -3.1227 z M 21.290,30.444 l -1.48383 1.03372 -0.20622 2.10905 1.64862 -1.32355 z"/><path fill="#1A1A1A" d="m 12.045985,34.707615 -1.81341,-1.36479 -0.15748,1.83347 -1.2856799,2.37432 1.9804499,2.73595 1.03109,0.16554 0.37119,-3.88721 z"/><path fill="#1A1A1A" d="m 15.636055,44.919735 -0.60647,-5.91209 -0.015,-3.84879 -2.18479,-1.07533 -0.24746,7.03017 z"/><path fill="#1A1A1A" d="m 16.051865,44.919165 0.60628,-5.91209 0.0154,-3.84915 2.18404,-1.07515 0.24746,7.03017 z"/><path fill="#1A1A1A" d="m 12.399365,26.152365 3.11202,-1.40603 -0.0937,2.27965 -2.80138,1.4364 z m -1.93508,1.6685 1.29355,0.72139 -0.14997,-1.70899 z m 1.05303,-1.637 -2.4793099,-1.03259 0.93361,2.52148 z m -1.5316399,1.73729 1.6900499,1.03372 0.28871,2.06743 -1.64881,-1.07515 z"/><path fill="#1A1A1A" d="M 12.897,29.025 l 0.0623 1.62387 2.30327 -0.49961 0.12448 -2.21703 z M 13.053,31.430 l -0.0309 1.99844 2.20973 0.59353 0.0311 -3.1227 z M 10.398,30.445 l 1.48384 1.0339 0.20622 2.10905 -1.64975 -1.32355 z"/><path fill="#1A1A1A" d="m 14.404465,45.040075 0.0221,-0.0277 -0.14866,-0.37945 -3.10172,-3.40449 -0.23283,-0.0825 2.05918,5.32009 z m -1.17263,2.01833 1.27705,3.29948 0.42631,-4.04862 -0.25196,-0.64303 z"/><path fill="#1A1A1A" d="m 17.284025,45.040455 -0.0221,-0.0281 0.14867,-0.37926 3.10171,-3.40449 0.23246,-0.0825 -2.05843,5.3199 z m 1.17263,2.01795 -1.27706,3.29948 -0.42631,-4.04843 0.25197,-0.64303 z"/><path fill="#1A1A1A" d="m 23.419015,50.399125 -0.15504,4.75091 -2.40263,6.60949 0.7362,1.90021 2.36401,-8.34435 z m -0.58154,-11.60825 -0.15485,4.00722 1.31793,7.93154 0.61977,-6.40308 z m -0.38731,5.12268 -2.75152,6.07258 -0.62015,4.87425 1.16232,6.85771 2.51886,-6.98144 0.15504,-7.18764 z"/><path fill="#1A1A1A" d="m 22.063225,39.369605 v 4.21363 l -2.94574,5.82511 -1.86027,5.78349 0.19365,-4.0072 z m -3.24944,13.42596 -0.0649,0.15467 -1.21294,2.90207 0.78325,7.18803 1.23619,-0.66122 -1.0714,-6.69272 z"/><path fill="#1A1A1A" d="m 17.255895,87.868445 0.1243,3.45228 0.28983,1.20638 h 0.87136 l 0.24897,-0.83181 0.29058,-0.0416 -0.0624,0.83181 1.09914,-0.33332 0.29058,-0.16629 1.24444,-0.27033 0.0416,-0.97748 -1.20319,-2.03743 -0.82974,-1.0399 -2.03294,-0.83181 z"/><path fill="#1A1A1A" d="m 18.251375,70.441125 0.29058,0.91486 0.6224,3.8681 0.0829,5.15733 -0.87136,5.03304 0.0412,-6.44714 -0.91242,-2.57848 -0.12561,-2.82837 z m 1.9915,2.32915 -0.20753,7.73637 -1.65949,6.23904 1.80478,-0.853 3.00816,-10.83583 -1.03727,-6.82095 z"/><path fill="#1A1A1A" d="m 21.404635,64.784375 0.1243,1.12295 -0.87118,1.08171 -0.29058,1.70599 -0.58116,0.24933 -0.49774,-2.57866 -0.33182,-0.91486 0.29058,-0.58247 z m -3.85853,0.0832 0.6224,1.74685 1.3273,2.57867 -0.33182,2.37095 -0.95423,-2.66209 -0.78738,-1.49734 z m 4.97811,-2.37039 -0.95423,5.11609 0.62241,-0.33295 0.49773,1.66381 z"/><path fill="#1A1A1A" d="m 8.2694651,50.399125 0.15504,4.75053 2.4026299,6.60968 -0.73638,1.90021 -2.3640099,-8.34435 z m 0.58117,-11.60768 0.15503,4.00684 -1.31754,7.93154 -0.61978,-6.40308 z m 0.38769,5.1223 2.7515099,6.07239 0.61997,4.87425 -1.16232,6.85771 -2.5190499,-6.98163 -0.15504,-7.18801 z"/><path fill="#1A1A1A" d="m 9.6258251,39.369415 v 4.21363 l 2.9451699,5.8253 1.86028,5.78349 -0.19366,-4.0072 z m 3.2488699,13.42559 0.0647,0.15485 1.21294,2.90207 -0.78307,7.18803 -1.23618,-0.66102 1.0714,-6.69273 z"/><path fill="#1A1A1A" d="m 14.433335,87.868265 -0.12448,3.45228 -0.29058,1.20637 h -0.87118 l -0.24877,-0.83181 -0.29059,-0.0416 0.0623,0.83181 -1.09934,-0.33333 -0.29058,-0.16629 -1.2448,-0.27033 -0.0412,-0.97747 1.2031899,-2.03781 0.82975,-1.04009 2.03294,-0.83181 z"/><path fill="#1A1A1A" d="m 13.437675,70.440945 -0.29058,0.91486 -0.62241,3.86828 -0.0829,5.15733 0.87174,5.03304 -0.0418,-6.44714 0.91298,-2.57848 0.1243,-2.82837 z m -1.99151,2.32914 0.20735,7.73637 1.65968,6.23904 -1.80497,-0.85299 -3.0079799,-10.83584 1.03728,-6.82095 z"/><path fill="#1A1A1A" d="m 10.284405,64.784375 -0.12448,1.12295 0.87118,1.08171 0.29058,1.70599 0.58116,0.24933 0.49774,-2.57866 0.33182,-0.91486 -0.29058,-0.58247 z m 3.85854,0.0832 -0.62241,1.74685 -1.32767,2.57867 0.33182,2.37095 0.95423,-2.66209 0.78832,-1.4964 z m -4.9786799,-2.37058 0.9542299,5.11609 -0.6223999,-0.33313 -0.49793,1.6638 z"/><path fill="#1A1A1A" d="m 3.2054751,27.370125 0.005,3.09419 -0.57959,1.91184 -0.54539,-2.41185 z"/><path fill="#1A1A1A" d="m 4.3904451,43.563145 -1.5198,0.0506 -0.76631,-0.67112 -1.21261996,2.15767 -0.86245,3.32873 0.49386,0.22113 0.59814996,-2.20238 0.50016,0.25356 -0.35639,2.49422 0.62382,0.24345 0.41402,-2.49194 0.55839,0.17851 -0.2262,2.76603 0.76938,0.32268 0.25788,-2.86764 0.4578,-0.0181 0.16611,2.65239 0.65997,0.2633 0.0712,-4.56643 0.34158,-0.19428 1.35316,1.68367 0.32832,-0.34354 -0.72644,-2.0551 z"/><path fill="#1A1A1A" d="m 28.325215,27.370125 -0.005,3.09419 0.57959,1.91184 0.54538,-2.41185 z"/><path fill="#1A1A1A" d="m 27.140245,43.563145 1.5198,0.0506 0.76631,-0.67111 1.21262,2.15766 0.86245,3.32873 -0.49386,0.22113 -0.59815,-2.20238 -0.50016,0.25356 0.35639,2.49422 -0.62382,0.24345 -0.41402,-2.49194 -0.55839,0.17851 0.2262,2.76603 -0.76938,0.32268 -0.25788,-2.86764 -0.4578,-0.0181 -0.16611,2.6524 -0.65997,0.26329 -0.0712,-4.56643 -0.34158,-0.19428 -1.35316,1.68368 -0.32832,-0.34355 0.72644,-2.0551 z"/>
""".trimIndent()

    @Suppress("MaxLineLength")
    companion object {
        private const val CSS="""
@page{size:A4;margin:0;}
*{box-sizing:border-box;margin:0;padding:0;}
body{font-family:"Helvetica Neue",Helvetica,Arial,sans-serif;color:#222;line-height:1.5;-webkit-print-color-adjust:exact;print-color-adjust:exact;background:#FFF;}
.page{width:210mm;min-height:297mm;padding:0 16mm;margin:0 auto;}

.pb{break-before:page;page-break-before:always;}
.bar{height:3px;background:#E07C4F;margin-bottom:10px;}
.over{font-size:8.5px;font-weight:700;letter-spacing:1.2px;color:#737373;}
.title{font-size:21px;font-weight:800;color:#1A1A1A;margin:3px 0 2px;}
.sub{font-size:9.5px;color:#737373;margin-bottom:6px;}
.sec{font-size:8.5px;font-weight:700;letter-spacing:1px;color:#737373;text-transform:uppercase;padding-bottom:4px;border-bottom:1px solid #E0E0E0;margin:14px 0 6px;break-after:avoid;page-break-after:avoid;}
.sec:first-child{margin-top:0;}
.card{background:#FAFAFA;border:1px solid #EBEBEB;border-radius:5px;padding:10px 12px;margin-top:4px;break-inside:avoid;}
.fq-wrap,.feel-row,table,.fin-row,.fin-train-grid,.nutrition-overview,.measure-chart,.workout-link{break-inside:avoid;}
.sub-label{font-size:7.5px;font-weight:700;letter-spacing:0.5px;color:#AAA;text-transform:uppercase;margin:10px 0 3px;break-after:avoid;page-break-after:avoid;}
table{width:100%;border-collapse:collapse;font-size:9.5px;}
th{text-align:left;font-size:7.5px;font-weight:700;color:#AAA;letter-spacing:0.4px;text-transform:uppercase;padding:4px 6px;background:#F0F0F0;}
th.r{text-align:right;}
td{padding:5px 6px;border-bottom:1px solid #F0F0F0;}
td.r{text-align:right;font-variant-numeric:tabular-nums;}
tr:nth-child(even) td{background:#F7F7F7;}
.b{font-weight:700;}
.pos{color:#4DAE6A;}
.neg{color:#E05252;}
.photo{max-width:200px;max-height:260px;object-fit:contain;border-radius:5px;border:1px solid #EBEBEB;}
.feel-row{margin-bottom:8px;}
.fl{font-size:8.5px;font-weight:700;margin-bottom:2px;}
.fl-p{color:#4DAE6A;}
.fl-n{color:#E05252;}
.ftags{font-size:9.5px;color:#333;line-height:1.6;}
.fq-wrap{margin-top:12px;padding-top:10px;border-top:1px solid #EBEBEB;}
.fq{border-left:3px solid #E07C4F;padding:5px 0 5px 12px;margin-bottom:10px;}
.fq:last-child{margin-bottom:0;}
.fq-t{font-size:9.5px;font-weight:700;color:#333;}
.fq-b{font-size:9.5px;color:#666;margin-top:1px;}
.nutrition-overview{display:flex;gap:10px;align-items:stretch;}
.nutrition-panel{flex:1;min-width:0;display:flex;flex-direction:column;}
.nutrition-panel .card{flex:1;}
.macro-card{padding:10px 12px;}
.macro-average{font-size:7.5px;color:#AAA;margin-top:5px;}
.macro-average b{color:#555;}
.mbs{margin-top:10px;}
.mbs-label{font-size:7.5px;font-weight:700;letter-spacing:0.5px;color:#AAA;text-transform:uppercase;margin-bottom:5px;}
.mb{display:flex;align-items:center;margin-bottom:3px;}
.mb-l{font-size:8px;color:#888;width:56px;text-align:right;padding-right:8px;}
.mb-t{flex:1;height:7px;background:#EBEBEB;border-radius:3px;overflow:hidden;}
.mb-f{display:block;height:100%;background:#E07C4F;border-radius:3px;opacity:0.4;}
.mb-p{font-size:8px;color:#999;width:36px;padding-left:6px;font-variant-numeric:tabular-nums;}
.fin-train-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:6px;}
.fin-train-grid.two{grid-template-columns:repeat(2,auto);gap:0;border:1px solid #EBEBEB;border-radius:6px;overflow:hidden;background:#FAFAFA;}
.fin-train-grid.two .fin-train-stat{background:transparent;border:0;border-radius:0;padding:10px 20px;min-width:84px;}
.fin-train-grid.two .fin-train-stat:nth-child(odd){border-right:1px solid #EBEBEB;}
.fin-train-grid.two .fin-train-stat:nth-child(-n+2){border-bottom:1px solid #EBEBEB;}
.fin-train-stat{text-align:center;padding:8px 4px;background:#FAFAFA;border:1px solid #EBEBEB;border-radius:5px;}
.fin-ts-val{font-size:14px;font-weight:800;color:#222;font-variant-numeric:tabular-nums;}
.fin-ts-val.accent{color:#E07C4F;}
.fin-ts-unit{font-size:8px;color:#AAA;font-weight:600;}
.fin-ts-label{font-size:7px;color:#AAA;margin-top:2px;text-transform:uppercase;letter-spacing:0.3px;}
.measurement-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:7px;margin-top:4px;}
.measure-chart{background:#FAFAFA;border:1px solid #EBEBEB;border-radius:5px;padding:7px 8px 4px;}
.measure-chart-head{display:flex;justify-content:space-between;align-items:baseline;font-size:8px;color:#333;gap:6px;}
.measure-chart-head>b{font-size:8.5px;}
.measure-chart-head>span{font-weight:700;white-space:nowrap;}
.measure-chart-head small{font-size:7px;color:#888;font-weight:500;margin-left:3px;}
.measure-chart-svg{display:block;width:100%;height:58px;margin-top:2px;}
.measure-chart-svg text{font-size:6px;fill:#AAA;font-family:"Helvetica Neue",Helvetica,Arial,sans-serif;}
.workout-links{padding:0;overflow:hidden;}
.workout-link{display:flex;justify-content:space-between;align-items:center;gap:12px;padding:8px 10px;color:#222;text-decoration:none;border-bottom:1px solid #EBEBEB;}
.workout-link:last-child{border-bottom:0;}
.workout-link b{display:block;font-size:9px;}
.workout-link small{display:block;font-size:7.5px;color:#999;margin-top:1px;}
.workout-link-action{font-size:7.5px;color:#E07C4F;font-weight:700;white-space:nowrap;}
.train-day td{background:rgba(224,124,79,0.04) !important;}
.train-icon{display:inline-block;width:10px;height:10px;margin-right:4px;vertical-align:middle;position:relative;top:-1px;}
.train-icon-empty{display:inline-block;width:10px;height:10px;margin-right:4px;}
.legend{display:flex;align-items:center;gap:0;font-size:7.5px;color:#AAA;margin-top:4px;}
.fin-row{display:flex;gap:10px;align-items:stretch;}
.fin-col{min-width:0;}
.sel-mb-row{display:flex;align-items:center;gap:0;margin-bottom:5px;}
.sel-mb-name{font-size:8px;color:#888;width:50px;}
.sel-mb-track{flex:1;height:8px;background:#F0F0F0;border-radius:4px;overflow:hidden;margin:0 6px;}
.sel-mb-fill{height:100%;border-radius:4px;opacity:0.55;}
.sel-mb-pct{font-size:9px;font-weight:700;color:#222;width:28px;text-align:right;}
"""
    }
}
