package com.student.amizoneplus.logic

import com.student.amizoneplus.data.AmizoneData
import com.student.amizoneplus.data.Settings
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

fun percent(attended: Int, held: Int): Double = if (held == 0) 100.0 else attended * 100.0 / held

/** Amity: a course meets for 15 weeks, so planned classes = 15 × (lectures + practicals per week). */
const val WEEKS = 15

/** Targets you can pick. 75% is Amizone's minimum. */
val TARGETS = listOf(75, 80, 85, 90, 95)

data class SemesterPlan(
    val planned: Int, // 15 × (L + P), or more if extra classes were already held
    val held: Int,
    val attended: Int,
    val remaining: Int,
    val required: Int,
    val need: Int, // of the remaining, how many you must attend
    val canMiss: Int, // of the remaining, how many you can still skip
    val reachable: Boolean,
    val bestPct: Double, // final % if you attend every remaining class
    val extraNeeded: Int, // when out of reach: extra classes to get scheduled (and attend) to hit the target
    val basePlanned: Int = planned, // 15 × (L + P), before any extra classes
    val extraHeld: Int = 0, // classes already held beyond the plan
)

/** Extra classes x (all attended) so that (attended + remaining + x) / (planned + x) ≥ target. */
fun extraClassesFor(attended: Int, remaining: Int, planned: Int, threshold: Int): Int {
    if (threshold >= 100) return 0
    val x = (threshold * planned - 100.0 * (attended + remaining)) / (100 - threshold)
    return max(0, ceil(x - 1e-9).toInt())
}

/** The target applies to the whole semester's planned classes: 15 × weekly classes. */
fun semesterPlan(attended: Int, held: Int, weekly: Double, threshold: Int): SemesterPlan {
    val base = floor(weekly * WEEKS + 1e-9).toInt() // 0.5 a week → 7, not 8
    if (held >= base && base > 0) {
        // Every planned class is done and extras are running: judge on what's been held.
        val ok = percent(attended, held) >= threshold
        return SemesterPlan(
            planned = held, held = held, attended = attended, remaining = 0,
            required = ceil(threshold / 100.0 * held - 1e-9).toInt(),
            need = if (ok) 0 else mustAttend(attended, held, threshold),
            canMiss = if (ok) canSkip(attended, held, threshold) else 0,
            reachable = ok, bestPct = percent(attended, held),
            extraNeeded = if (ok) 0 else mustAttend(attended, held, threshold),
            basePlanned = base, extraHeld = held - base,
        )
    }
    val planned = max(base, held)
    val remaining = planned - held
    val required = ceil(threshold / 100.0 * planned - 1e-9).toInt()
    val need = max(0, required - attended)
    val reachable = need <= remaining
    return SemesterPlan(
        planned, held, attended, remaining, required, need,
        if (reachable) remaining - need else 0, reachable,
        if (planned > 0) (attended + remaining) * 100.0 / planned else 100.0,
        if (reachable) 0 else extraClassesFor(attended, remaining, planned, threshold),
    )
}

fun canSkip(attended: Int, held: Int, threshold: Int) = max(0, floor(attended * 100.0 / threshold - held).toInt())

fun mustAttend(attended: Int, held: Int, threshold: Int) =
    if (threshold >= 100) (if (attended < held) Int.MAX_VALUE else 0)
    else max(0, ceil((threshold * held - 100.0 * attended) / (100 - threshold)).toInt())

private fun plural(n: Int, w: String) = "$n $w${if (n == 1) "" else "es"}"

/** One-line status; uses the semester plan when the weekly classes are known. */
fun statusText(attended: Int, held: Int, threshold: Int, weekly: Double? = null): String {
    if (weekly != null && weekly > 0) {
        val p = semesterPlan(attended, held, weekly, threshold)
        if (p.extraHeld > 0 || (p.remaining == 0 && p.basePlanned > 0)) {
            val done = "All ${p.basePlanned} planned held" + if (p.extraHeld > 0) " (+${p.extraHeld} extra)" else ""
            return if (p.reachable) "$done · can still skip ${p.canMiss}" else "$done · attend the next ${p.need} extra"
        }
        if (!p.reachable) return "Out of reach for $threshold% — needs ${plural(p.extraNeeded, "extra class")}"
        if (p.need == 0) return if (p.remaining > 0) "Safe — you can miss ${p.canMiss} of ${p.remaining} left" else "Semester complete"
        return "Attend ${p.need} of ${p.remaining} left · can miss ${p.canMiss}"
    }
    if (held == 0) return "No classes held yet"
    if (percent(attended, held) >= threshold) {
        val k = canSkip(attended, held, threshold)
        return if (k == 0) "On the edge — don't miss the next class" else "You can skip ${plural(k, "class")}"
    }
    return "Attend ${plural(mustAttend(attended, held, threshold), "class")} more to reach $threshold%"
}

/** Where a course's weekly classes came from. */
enum class Basis { Override, Structure, Credits }

/**
 * Classes per week that count for attendance: your override first, then
 * L + P from Amizone's Programme Structure, then (if that's missing) credit units.
 */
fun weeklyFor(code: String, data: AmizoneData?, settings: Settings): Pair<Double, Basis>? {
    val key = code.replace(" ", "").uppercase()
    (settings.weeklyOverrides[key] ?: settings.weeklyOverrides[code])?.let { return it to Basis.Override }
    (data?.hours?.get(key) ?: data?.hours?.get(code))?.weekly?.takeIf { it > 0 }?.let { return it to Basis.Structure }
    (data?.credits?.get(key) ?: data?.credits?.get(code))?.let { return it to Basis.Credits }
    return null
}

/** Old name, still used by a few screens: just the weekly number. */
fun creditsFor(code: String, data: AmizoneData?, settings: Settings): Double? = weeklyFor(code, data, settings)?.first
