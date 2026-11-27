package com.student.amizoneplus.ui.screens

import androidx.compose.material3.Text
import com.student.amizoneplus.ui.pets.perch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import kotlin.math.roundToInt
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.graphicsLayer
import com.student.amizoneplus.ui.kit.SquircleShape
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.student.amizoneplus.data.Course
import com.student.amizoneplus.data.Settings
import com.student.amizoneplus.logic.creditsFor
import com.student.amizoneplus.logic.percent
import com.student.amizoneplus.logic.semesterPlan
import com.student.amizoneplus.logic.statusText
import com.student.amizoneplus.logic.weekdayRates
import com.student.amizoneplus.ui.UiState
import com.student.amizoneplus.ui.kit.ArrowButton
import com.student.amizoneplus.ui.kit.Avatar
import com.student.amizoneplus.ui.kit.Btn
import com.student.amizoneplus.ui.kit.BtnKind
import com.student.amizoneplus.ui.kit.Card
import com.student.amizoneplus.ui.kit.Chip
import com.student.amizoneplus.ui.kit.CircleButton
import com.student.amizoneplus.ui.kit.CountUp
import com.student.amizoneplus.ui.kit.DropdownPill
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.Rise
import com.student.amizoneplus.ui.kit.SearchPill
import com.student.amizoneplus.ui.kit.StatTile
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.Tone
import com.student.amizoneplus.ui.kit.WeekBars
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.shell.HeroPage
import com.student.amizoneplus.ui.shell.MenuButton
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import com.student.amizoneplus.ui.theme.attendanceTone

private val AVATAR_COLORS = listOf(P.sun, P.sky, P.sage, P.lilac, P.coralSoft)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SubjectsScreen(
    s: UiState,
    onRefresh: () -> Unit,
    openLms: (String) -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    banner: (@Composable () -> Unit)?,
) {
    val haptics = LocalHaptics.current
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = open != null) { haptics.collapse(); open = null }
    val data = s.data
    val courses: List<Course> = data?.courses?.ifEmpty { null }
        ?: data?.attendance.orEmpty().map { Course(it.code, it.name, "", it.attended, it.held, null, null) }

    SharedTransitionLayout {
        AnimatedContent(
            open,
            transitionSpec = { fadeIn(tween(Motion.MORPH_MS, easing = Motion.emphasized)) togetherWith fadeOut(tween(Motion.MORPH_MS / 2)) },
            label = "subjects",
        ) { code ->
            val course = courses.firstOrNull { it.code == code }
            if (course == null) {
                SubjectList(s, courses, onRefresh, banner, updateSettings, this@SharedTransitionLayout, this) { haptics.expand(); open = it }
            } else {
                SubjectDetail(s, course, courses.indexOf(course), onRefresh, { haptics.collapse(); open = null }, { openLms(course.code) },
                    updateSettings, this@SharedTransitionLayout, this)
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SubjectList(
    s: UiState,
    courses: List<Course>,
    onRefresh: () -> Unit,
    banner: (@Composable () -> Unit)?,
    updateSettings: ((Settings) -> Settings) -> Unit,
    shared: SharedTransitionScope,
    anim: AnimatedVisibilityScope,
    onOpen: (String) -> Unit,
) {
    val t = s.settings.threshold
    var filter by rememberSaveable { mutableStateOf("all") }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = courses.filter {
        val p = if (it.held > 0) percent(it.attended, it.held) else 100.0
        when (filter) { "at risk" -> p < t; "on track" -> p >= t; else -> true } &&
            (query.isBlank() || it.name.contains(query, true) || it.code.contains(query, true))
    }
    val held = courses.filter { it.held > 0 }
    val atRisk = held.count { percent(it.attended, it.held) < t }

    HeroPage(
        heroColor = P.coral,
        refreshing = s.refreshing,
        onRefresh = onRefresh,
        banner = banner,
        hero = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The title shrinks a touch rather than wrapping when the filter label is long.
                androidx.compose.material3.Text(
                    "SUBJECTS", Modifier.weight(1f), color = P.onHero, maxLines = 1, softWrap = false,
                    style = Type.display, autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(
                        minFontSize = androidx.compose.ui.unit.TextUnit(28f, androidx.compose.ui.unit.TextUnitType.Sp),
                        maxFontSize = Type.display.fontSize,
                    ),
                )
                Spacer(Modifier.size(8.dp))
                FilterMenu(filter) { filter = it }
                Spacer(Modifier.size(8.dp))
                MenuButton()
            }
            // Data where the reference has mascots: every subject's attendance, with the target line.
            SubjectBars(held, t, Modifier.fillMaxWidth().padding(top = 20.dp))
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${courses.size}", "subjects", P.coralSoft)
                StatTile("$atRisk", "below $t%", P.coralSoft, onClick = { filter = "at risk" })
            }
            // What you're aiming for: every "can miss" and "must attend" follows it.
            com.student.amizoneplus.ui.kit.Caps("YOUR TARGET", P.onHero.copy(alpha = 0.7f), Modifier.padding(top = 18.dp, bottom = 8.dp))
            com.student.amizoneplus.ui.kit.TargetPicker(t, { v -> updateSettings { it.copy(threshold = v) } },
                bg = P.coralSoft, fg = P.onHero, activeBg = P.onHero, activeFg = Color.White)
        },
    ) {
        SearchPill(query, { query = it }, "Search by name or code")
        T("${shown.size} subject${if (shown.size == 1) "" else "s"} found", Type.label, sub = true, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
        shown.forEachIndexed { i, c ->
            val credits = creditsFor(c.code, s.data, s.settings)
            Rise(i) {
                with(shared) {
                    Card(
                        Modifier.sharedBounds(rememberSharedContentState("card-${c.code}"), anim, clipInOverlayDuringTransition = OverlayClip(SquircleShape(28.dp))),
                        onClick = { onOpen(c.code) },
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Avatar(c.code, AVATAR_COLORS[courses.indexOf(c) % AVATAR_COLORS.size], 56.dp)
                            Column(Modifier.weight(1f)) {
                                T(c.name.uppercase(), Type.title, maxLines = 2,
                                    modifier = Modifier.sharedElement(rememberSharedContentState("name-${c.code}"), anim))
                                if (c.type.isNotEmpty()) Box(Modifier.padding(top = 8.dp)) { Chip(c.type, Tone.Sun) }
                            }
                            ArrowButton({ onOpen(c.code) })
                        }
                        T(if (c.held > 0) "%.1f%% · %s".format(percent(c.attended, c.held), statusText(c.attended, c.held, t, credits)) else "No classes held yet",
                            Type.body, modifier = Modifier.padding(top = 14.dp), maxLines = 2)
                        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            T("CODE: ", Type.caption, faint = true); T(c.code, Type.caption, sub = true)
                            Box(Modifier.weight(1f))
                            if (c.held > 0) Box(Modifier.size(10.dp).background(attendanceTone(percent(c.attended, c.held), t), CircleShape))
                        }
                    }
                }
            }
        }
    }
}

/** The "all / at risk / on track" filter: a pill that opens a small menu. */
@Composable
private fun FilterMenu(value: String, onPick: (String) -> Unit) {
    val haptics = LocalHaptics.current
    var open by remember { mutableStateOf(false) }
    Box {
        DropdownPill(value, { haptics.select(); open = true }, P.coralSoft, P.onHero)
        androidx.compose.material3.DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = SquircleShape(22.dp),
            containerColor = P.cardRaised,
            offset = androidx.compose.ui.unit.DpOffset(0.dp, 8.dp),
        ) {
            listOf("all" to "All subjects", "at risk" to "At risk", "on track" to "On track").forEach { (key, label) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { T(label, Type.label, color = if (key == value) P.text else P.sub) },
                    trailingIcon = if (key == value) ({ Icon(Icons.Outlined.Check, null, tint = P.coral, modifier = Modifier.size(18.dp)) }) else null,
                    onClick = { open = false; if (key != value) { haptics.select(); onPick(key) } },
                )
            }
        }
    }
}

/** The tiles fill in once per app launch (the first time you open Subjects), not on every visit. */
private object AttendanceIntro { var played = false }

/**
 * Attendance at a glance, as in student attendance trackers: a two-column grid of tiles,
 * one per subject. Each shows the short name, the percentage big, a slim bar against your
 * target (the notch), and what it means in plain words: how many classes you can skip, or
 * how many you need to attend. Subjects under target turn dark so they stand out.
 */
@Composable
private fun SubjectBars(courses: List<Course>, threshold: Int, modifier: Modifier) {
    val animate = remember { !AttendanceIntro.played }
    LaunchedEffect(Unit) { AttendanceIntro.played = true }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        courses.chunked(2).forEachIndexed { row, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEachIndexed { col, c -> AttendanceTile(c, threshold, row * 2 + col, animate, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AttendanceTile(c: Course, threshold: Int, index: Int, animate: Boolean, modifier: Modifier) {
    val pct = percent(c.attended, c.held).toFloat()
    val low = pct < threshold
    val grow = remember { Animatable(if (animate) 0f else 1f) }
    if (animate) LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(70L * index)
        grow.animateTo(1f, tween(700, easing = Motion.emphasized))
    }
    val ink = if (low) Color.White else P.onHero
    val bg = if (low) P.onHero else Color.White.copy(alpha = 0.22f)
    val name = com.student.amizoneplus.logic.shortCourseName(c.name, 12).replace("OOPS", "OOPs")
    val note = when {
        c.held == 0 -> "no classes yet"
        low -> com.student.amizoneplus.logic.mustAttend(c.attended, c.held, threshold).let { "attend $it more" }
        else -> com.student.amizoneplus.logic.canSkip(c.attended, c.held, threshold).let { if (it == 0) "on the edge" else "can skip $it" }
    }
    Column(
        modifier.perch()
            .graphicsLayer { alpha = (grow.value * 1.6f).coerceIn(0f, 1f); translationY = 10.dp.toPx() * (1f - grow.value) }
            .background(bg, SquircleShape(24.dp))
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(name, Type.label, color = ink, maxLines = 1, modifier = Modifier.weight(1f))
            Text("${(pct * grow.value).roundToInt()}%", style = Type.numSm.copy(fontSize = androidx.compose.ui.unit.TextUnit(26f, androidx.compose.ui.unit.TextUnitType.Sp)), color = ink, maxLines = 1)
        }
        Box(
            Modifier.padding(top = 8.dp).fillMaxWidth().height(8.dp).drawBehind {
                val h = size.height
                // Squircle-pill track and fill (softer ends than a semicircle).
                drawPath(com.student.amizoneplus.ui.kit.squirclePillPath(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, h)), ink.copy(alpha = 0.16f))
                val w = size.width * (pct / 100f) * grow.value
                if (w > 0f) drawPath(com.student.amizoneplus.ui.kit.squirclePillPath(androidx.compose.ui.geometry.Rect(0f, 0f, w.coerceAtLeast(h), h)), ink)
                // Target notch, sticking out a little above and below the bar.
                val tx = size.width * threshold / 100f
                drawLine(if (low) Color.White.copy(alpha = 0.6f) else P.onHero.copy(alpha = 0.5f),
                    Offset(tx, -3.dp.toPx()), Offset(tx, h + 3.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
            },
        )
        T(note, Type.caption, color = ink.copy(alpha = 0.72f), maxLines = 1, modifier = Modifier.padding(top = 6.dp))
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SubjectDetail(
    s: UiState,
    c: Course,
    index: Int,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
    openLms: () -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    shared: SharedTransitionScope,
    anim: AnimatedVisibilityScope,
) {
    val context = LocalContext.current
    val haptics = LocalHaptics.current
    val t = s.settings.threshold
    val basis = com.student.amizoneplus.logic.weeklyFor(c.code, s.data, s.settings)
    val credits = basis?.first
    val hours = s.data?.hours?.get(c.code.replace(" ", "").uppercase())
    val fromAmizone = hours?.weekly?.takeIf { it > 0 } ?: s.data?.credits?.get(c.code)
    val p = if (c.held > 0) percent(c.attended, c.held) else 0.0
    val plan = credits?.let { semesterPlan(c.attended, c.held, it, t) }

    fun setCredits(n: Double) {
        haptics.select()
        updateSettings { st ->
            val next = st.weeklyOverrides.toMutableMap()
            if (fromAmizone != null && n == fromAmizone) next.remove(c.code) else next[c.code] = n
            st.copy(weeklyOverrides = next)
        }
    }

    with(shared) {
        Box(Modifier.fillMaxSize().sharedBounds(rememberSharedContentState("card-${c.code}"), anim, clipInOverlayDuringTransition = OverlayClip(SquircleShape(28.dp)))) {
            HeroPage(
                heroColor = P.coral,
                refreshing = s.refreshing,
                onRefresh = onRefresh,
                hero = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircleButton(Icons.AutoMirrored.Outlined.ArrowBack, onClose, 48.dp)
                        Box(Modifier.weight(1f))
                        Avatar(c.code, AVATAR_COLORS[index.coerceAtLeast(0) % AVATAR_COLORS.size], 48.dp)
                    }
                    T(c.name.uppercase(), Type.display, color = P.onHero, maxLines = 3,
                        modifier = Modifier.padding(top = 18.dp).sharedElement(rememberSharedContentState("name-${c.code}"), anim))
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(c.code, Tone.Cream)
                        if (c.type.isNotEmpty()) Chip(c.type, Tone.Cream)
                    }
                    Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("%.0f%%".format(p), "${c.attended}/${c.held} held", P.coralSoft)
                        StatTile(c.internals?.let { "%.0f".format(it.have) } ?: "—", c.internals?.let { "of %.0f internals".format(it.max) } ?: "internals", P.coralSoft)
                    }
                },
            ) {
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            T("SEMESTER PLAN", Type.title)
                            T(planLine(basis, hours, plan?.basePlanned) + (plan?.extraHeld?.takeIf { it > 0 }?.let { " · $it extra held so far" } ?: ""),
                                Type.caption, sub = true, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
                        }
                        Stepper(credits ?: 0.0, ::setCredits)
                    }
                    com.student.amizoneplus.ui.kit.Caps("TARGET", P.faint, Modifier.padding(top = 18.dp, bottom = 8.dp))
                    com.student.amizoneplus.ui.kit.TargetPicker(t, { v -> haptics.select(); updateSettings { it.copy(threshold = v) } })
                    if (plan != null) Row(Modifier.padding(top = 18.dp)) {
                        if (plan.extraHeld > 0) Stat("Extra held", plan.extraHeld.toDouble(), null, Modifier.weight(1f))
                        else Stat("Left", plan.remaining.toDouble(), null, Modifier.weight(1f))
                        Stat(if (plan.reachable) "Must attend" else "Extra needed", (if (plan.reachable) plan.need else plan.extraNeeded).toDouble(),
                            if (plan.reachable) null else P.bad, Modifier.weight(1f))
                        Stat("Can miss", plan.canMiss.toDouble(), if (plan.canMiss > 0) P.good else P.warn, Modifier.weight(1f))
                    }
                    Box(Modifier.padding(top = 16.dp)) { Chip(statusText(c.attended, c.held, t, credits), Tone.Ink) }
                }
                if (plan != null && !plan.reachable) ExtraClassesCard(c.attended, c.held, credits, t, plan)
                Card {
                    T("BY WEEKDAY", Type.title)
                    val weekly = weekdayRates(s.data?.history.orEmpty(), c.code)
                    T(if (weekly.any { it != null }) "Last 4 weeks" else "No marked classes in the last 4 weeks", Type.caption, sub = true, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                    WeekBars(weekly, bar = P.coral)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Btn("Material", BtnKind.Lav, Icons.Outlined.FolderOpen, openLms)
                    Btn("Syllabus", BtnKind.Ghost, Icons.Outlined.Description, c.syllabusUrl?.let { u -> { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) } })
                }
            }
        }
    }
}

private fun fmt(d: Double) = if (d % 1 == 0.0) d.toInt().toString() else d.toString()

/** How the planned classes were worked out, in one line. */
private fun planLine(basis: Pair<Double, com.student.amizoneplus.logic.Basis>?, hours: com.student.amizoneplus.data.CourseHours?, planned: Int?): String {
    if (basis == null || planned == null) return "Set classes per week to plan the semester"
    val (w, how) = basis
    return when (how) {
        com.student.amizoneplus.logic.Basis.Structure -> {
            val h = hours ?: com.student.amizoneplus.data.CourseHours()
            val parts = if (h.l == 0.0 && h.t > 0) listOfNotNull("T %s ÷ 2".format(fmt(h.t)), if (h.p > 0) "P " + fmt(h.p) else null)
                else listOfNotNull("L " + fmt(h.l), if (h.t > 0) "T " + fmt(h.t) else null, "P " + fmt(h.p))
            "%s = %s a week × 15 weeks = %d classes · from Programme Structure".format(parts.joinToString(" + "), fmt(w), planned)
        }
        com.student.amizoneplus.logic.Basis.Credits ->
            "%s a week × 15 weeks = %d classes · L/P not found, using credits".format(fmt(w), planned)
        com.student.amizoneplus.logic.Basis.Override ->
            "%s a week × 15 weeks = %d classes · set by you".format(fmt(w), planned)
    }
}

/**
 * Shown when even perfect attendance from here can't reach the target: how many
 * extra classes to ask for (and attend), plus how things stand for the 75% minimum.
 */
@Composable
private fun ExtraClassesCard(attended: Int, held: Int, weekly: Double, t: Int, plan: com.student.amizoneplus.logic.SemesterPlan) {
    val min = semesterPlan(attended, held, weekly, 75)
    Card(color = P.badSoft) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T("NEED ${plan.extraNeeded} EXTRA CLASS${if (plan.extraNeeded == 1) "" else "ES"}", Type.title, color = P.bad, modifier = Modifier.weight(1f))
            CountUp(plan.extraNeeded.toDouble(), style = Type.numSm, color = P.bad)
        }
        val n = "${plan.extraNeeded} extra class${if (plan.extraNeeded == 1) "" else "es"}"
        T(
            if (plan.remaining == 0) "All ${plan.basePlanned} planned classes are done and you're at %.1f%%. You need $n — and to attend every one — to reach $t%%.".format(plan.bestPct)
            else "Even if you attend all ${plan.remaining} classes left, you'd finish at %.1f%%. Get $n scheduled — and attend every one — to reach $t%%.".format(plan.bestPct),
            Type.body, modifier = Modifier.padding(top = 8.dp),
        )
        if (t > 75) T(
            if (min.reachable) "For the 75% minimum you don't need extras: attend ${min.need} of ${min.remaining} left (you can miss ${min.canMiss})."
            else "For the 75% minimum alone: ${min.extraNeeded} extra class${if (min.extraNeeded == 1) "" else "es"}.",
            Type.caption, sub = true, modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun Stat(label: String, value: Double, tone: Color?, modifier: Modifier) {
    Column(modifier) {
        T(label, Type.caption, sub = true)
        CountUp(value, style = Type.numSm, color = tone ?: P.text, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun Stepper(value: Double, onChange: (Double) -> Unit) {
    @Composable
    fun btn(icon: androidx.compose.ui.graphics.vector.ImageVector, next: Double, disabled: Boolean) {
        Box(
            Modifier.size(40.dp).press(if (disabled) null else ({ onChange(next) }), Feel.None, 0.88f)
                .background(if (disabled) P.cardRaised else P.cream, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (disabled) P.faint else P.onCream, modifier = Modifier.size(18.dp)) }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val step = if (value <= 1.0) 0.5 else 1.0
        btn(Icons.Outlined.Remove, (value - step).coerceAtLeast(0.5), value <= 0.5)
        T(if (value > 0) fmt(value) else "–", Type.numSm, modifier = Modifier.widthIn(min = 26.dp))
        btn(Icons.Outlined.Add, (value + (if (value < 1.0) 0.5 else 1.0)).coerceAtMost(16.0), value >= 16)
    }
}
