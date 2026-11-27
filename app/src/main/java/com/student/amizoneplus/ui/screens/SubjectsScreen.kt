package com.student.amizoneplus.ui.screens

import androidx.compose.foundation.layout.width
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.lerp
import androidx.compose.material.icons.outlined.KeyboardArrowDown
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

    SubjectList(s, courses, onRefresh, banner, updateSettings, open, { code -> if (code == null) haptics.collapse() else haptics.expand(); open = code }, openLms)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SubjectList(
    s: UiState,
    courses: List<Course>,
    onRefresh: () -> Unit,
    banner: (@Composable () -> Unit)?,
    updateSettings: ((Settings) -> Settings) -> Unit,
    openCode: String?,
    onOpen: (String?) -> Unit,
    openLms: (String) -> Unit,
) {
    val t = s.settings.threshold
    var filter by rememberSaveable { mutableStateOf("all") }
    val shown = courses.filter {
        val p = if (it.held > 0) percent(it.attended, it.held) else 100.0
        when (filter) { "at risk" -> p < t; "on track" -> p >= t; else -> true }
    }
    val held = courses.filter { it.held > 0 }
    val atRisk = held.count { percent(it.attended, it.held) < t }

    HeroPage(
        heroColor = P.coral,
        refreshing = s.refreshing,
        onRefresh = onRefresh,
        banner = banner,
        fitSheet = true,
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
        // The sheet over the coral: every subject as a tile. Tap one and it grows to fill the
        // sheet (the coral above stays put), its name and bar scaling into their new places.
        SharedTransitionLayout {
            AnimatedContent(
                courses.firstOrNull { it.code == openCode },
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(140)) using androidx.compose.animation.SizeTransform(clip = false) },
                label = "subjectSheet",
            ) { c ->
                if (c == null) Column {
                    SubjectBars(shown, t, s, Modifier.fillMaxWidth(), this@SharedTransitionLayout, this@AnimatedContent) { onOpen(it) }
                    if (shown.isEmpty()) T("No subjects here.", Type.body, sub = true, modifier = Modifier.padding(top = 8.dp))
                } else ExpandedSubject(s, c, updateSettings, { onOpen(null) }, { openLms(c.code) }, this@SharedTransitionLayout, this@AnimatedContent)
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
 * Every subject as a tile in a two-column grid: short name, a slim bar against your target
 * (the notch) with the percentage beside it, and what it means ("can skip 13"). Tap a tile
 * and it morphs (growing, with a quick blur) into the subject's full page.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SubjectBars(
    courses: List<Course>, threshold: Int, s: UiState, modifier: Modifier,
    shared: SharedTransitionScope, anim: AnimatedVisibilityScope, onOpen: (String) -> Unit,
) {
    val animate = remember { !AttendanceIntro.played }
    LaunchedEffect(Unit) { AttendanceIntro.played = true }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        courses.chunked(2).forEachIndexed { row, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEachIndexed { col, c ->
                    val m = with(shared) {
                        Modifier.weight(1f)
                            .sharedBounds(rememberSharedContentState("card-${c.code}"), anim,
                                boundsTransform = MorphBounds, clipInOverlayDuringTransition = OverlayClip(SquircleShape(24.dp)))
                            .blurMorph(anim)
                    }
                    AttendanceTile(c, threshold, row * 2 + col, animate, false, m, shared, anim) { onOpen(c.code) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Fast, settled growth for the tile <-> page morph. */
@OptIn(ExperimentalSharedTransitionApi::class)
private val MorphBounds = androidx.compose.animation.BoundsTransform { _, _ ->
    androidx.compose.animation.core.spring(dampingRatio = 0.92f, stiffness = 520f)
}

/** Blurs content while it's arriving or leaving, so the two ends of a morph melt into each other. */
@Composable
private fun Modifier.blurMorph(anim: AnimatedVisibilityScope): Modifier {
    val b by anim.transition.animateFloat(transitionSpec = { tween(240) }, label = "blurMorph") {
        if (it == androidx.compose.animation.EnterExitState.Visible) 0f else 1f
    }
    return graphicsLayer {
        val r = 16.dp.toPx() * b
        renderEffect = if (android.os.Build.VERSION.SDK_INT >= 31 && r > 0.5f)
            android.graphics.RenderEffect.createBlurEffect(r, r, android.graphics.Shader.TileMode.DECAL).asComposeRenderEffect()
        else null
    }
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
private fun AttendanceTile(
    c: Course, threshold: Int, index: Int, animate: Boolean, selected: Boolean, modifier: Modifier,
    shared: SharedTransitionScope, anim: AnimatedVisibilityScope, onTap: () -> Unit,
) {
    val none = c.held == 0
    val pct = if (none) 0f else percent(c.attended, c.held).toFloat()
    val low = !none && pct < threshold
    val grow = remember { Animatable(if (animate) 0f else 1f) }
    if (animate) LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(70L * index)
        grow.animateTo(1f, tween(700, easing = Motion.emphasized))
    }
    val sel by androidx.compose.animation.core.animateFloatAsState(if (selected) 1f else 0f, tween(220), label = "sel")
    // Hold the bar, then drag: it thickens (taking the note's row) and follows your finger.
    // Let go and it springs back to the truth, with a cheeky toast.
    val ctx = LocalContext.current
    val haptics = LocalHaptics.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var holding by remember { mutableStateOf(false) }
    val shownPct = remember { Animatable(pct) }
    LaunchedEffect(pct) { if (!holding) shownPct.snapTo(pct) }
    val hold by androidx.compose.animation.core.animateFloatAsState(
        if (holding) 1f else 0f, androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 520f), label = "hold",
    )
    // Cards on the dark sheet; below-target subjects turn coral so they stand out.
    val ink = if (low) P.onHero else P.text
    val bg = if (low) P.coral else lerp(P.card, P.cardRaised, sel)
    val fill = if (low) P.onHero else P.coral
    val name = com.student.amizoneplus.logic.shortCourseName(c.name, 12).replace("OOPS", "OOPs")
    val note = when {
        c.held == 0 -> "no classes yet"
        low -> com.student.amizoneplus.logic.mustAttend(c.attended, c.held, threshold).let { "attend $it more" }
        else -> com.student.amizoneplus.logic.canSkip(c.attended, c.held, threshold).let { if (it == 0) "on the edge" else "can skip $it" }
    }
    Column(
        modifier.perch()
            .graphicsLayer { alpha = (grow.value * 1.6f).coerceIn(0f, 1f); translationY = 10.dp.toPx() * (1f - grow.value) }
            .press(onTap, Feel.Tap, 0.97f)
            .background(bg, SquircleShape(24.dp))
            .padding(start = 14.dp, end = 12.dp, top = 9.dp, bottom = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(name, Type.label, color = ink, maxLines = 1, modifier = Modifier.weight(1f).sharedName(c.code, shared, anim))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = ink.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        }
        Row(Modifier.padding(top = 4.dp).sharedBar(c.code, shared, anim), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.weight(1f).height(8.dp + 17.dp * hold)
                    .pointerInput(none) {
                        var lastStep = -1
                        fun setFrom(x: Float) {
                            val v = (x / size.width * 100f).coerceIn(0f, 100f)
                            val step = (v / 5f).toInt()
                            if (step != lastStep) { lastStep = step; haptics.tick() }
                            scope.launch { shownPct.snapTo(v) }
                        }
                        detectDragGesturesAfterLongPress(
                            onDragStart = { o -> holding = true; haptics.armed(); lastStep = -1; setFrom(o.x) },
                            onDrag = { ch, _ -> ch.consume(); setFrom(ch.position.x) },
                            onDragEnd = {
                                holding = false
                                scope.launch { shownPct.animateTo(pct, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 260f)) }
                                android.widget.Toast.makeText(ctx, (if (none) NO_CLASS_WISHES else WISHES).random(), android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onDragCancel = {
                                holding = false
                                scope.launch { shownPct.animateTo(pct, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 260f)) }
                            },
                        )
                    }
                    .drawBehind {
                        val h = size.height
                        drawPath(com.student.amizoneplus.ui.kit.squirclePillPath(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, h)), ink.copy(alpha = 0.16f))
                        val w = size.width * (shownPct.value / 100f) * grow.value
                        if (w > 0f) drawPath(com.student.amizoneplus.ui.kit.squirclePillPath(androidx.compose.ui.geometry.Rect(0f, 0f, w.coerceAtLeast(h), h)), fill)
                        val tx = size.width * threshold / 100f
                        drawLine(ink.copy(alpha = 0.5f), Offset(tx, -3.dp.toPx()), Offset(tx, h + 3.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                    },
            )
            // Fixed width (fits "100%"), so the bar's end doesn't shift as the digits change.
            Text(if (none && !holding && shownPct.value < 0.5f) "—" else "${(shownPct.value * grow.value).roundToInt()}%",
                style = Type.numSm.copy(fontSize = androidx.compose.ui.unit.TextUnit(22f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = ink, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.padding(start = 6.dp).width(50.dp))
        }
        // The note gives its row to the thickened bar while you're playing with it.
        T(note, Type.caption, color = ink.copy(alpha = 0.72f), maxLines = 1, modifier = Modifier
            .layout { m, cons ->
                val pl = m.measure(cons)
                layout(pl.width, (pl.height * (1f - hold)).roundToInt().coerceAtLeast(0)) { pl.place(0, 0) }
            }
            .graphicsLayer { alpha = (1f - hold * 1.5f).coerceIn(0f, 1f) })
    }
}

/** What the toast says when you let go of a bar you've been "fixing". */
private val WISHES = listOf(
    "Wish it was this easy, mate 😉",
    "Nice try. Amizone didn't see that.",
    "If only attendance worked like this…",
    "Dragging it won't mark you present 😄",
)

/** …and for subjects that haven't even had a class yet. */
private val NO_CLASS_WISHES = listOf(
    "BRUH 💀 there aren't even classes yet",
    "why you wanna be present in that type shit 💀",
    "Attendance for a class that doesn't exist? Bold.",
    "Bro's attending ghost lectures 👻",
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.sharedName(code: String, shared: SharedTransitionScope, anim: AnimatedVisibilityScope): Modifier = with(shared) {
    this@sharedName.sharedBounds(rememberSharedContentState("name-$code"), anim, boundsTransform = MorphBounds,
        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(androidx.compose.ui.layout.ContentScale.FillHeight, Alignment.CenterStart))
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.sharedBar(code: String, shared: SharedTransitionScope, anim: AnimatedVisibilityScope): Modifier = with(shared) {
    this@sharedBar.sharedBounds(rememberSharedContentState("bar-$code"), anim, boundsTransform = MorphBounds,
        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(androidx.compose.ui.layout.ContentScale.FillBounds, Alignment.CenterStart))
}

/**
 * A subject opened in place: the tile grows to fill the sheet (the coral header stays), its
 * name and attendance bar scale up into their spots, and everything the subject page had
 * appears below: the semester plan with its classes-per-week stepper, what you must attend
 * and can miss, extra classes if needed, the weekday chart, and Material / Syllabus.
 * Collapse with the button at the top left (or Back).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExpandedSubject(
    s: UiState,
    c: Course,
    updateSettings: ((Settings) -> Settings) -> Unit,
    onClose: () -> Unit,
    openLms: () -> Unit,
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
    val none = c.held == 0
    val p = if (none) 0.0 else percent(c.attended, c.held)
    val low = !none && p < t
    val plan = credits?.let { semesterPlan(c.attended, c.held, it, t) }
    val name = com.student.amizoneplus.logic.shortCourseName(c.name, 12).replace("OOPS", "OOPs")
    val ink = if (low) P.onHero else P.text
    val fill = if (low) P.onHero else P.coral

    fun setCredits(n: Double) {
        haptics.select()
        updateSettings { st ->
            val next = st.weeklyOverrides.toMutableMap()
            if (fromAmizone != null && n == fromAmizone) next.remove(c.code) else next[c.code] = n
            st.copy(weeklyOverrides = next)
        }
    }

    val screenH = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    Column(
        with(shared) {
            Modifier.fillMaxWidth()
                .sharedBounds(rememberSharedContentState("card-${c.code}"), anim, boundsTransform = MorphBounds,
                    clipInOverlayDuringTransition = OverlayClip(SquircleShape(28.dp)))
        }
            .background(if (low) P.coral else P.card, SquircleShape(28.dp))
            .padding(18.dp),
    ) {
        // Collapse, top left.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).press(onClose, Feel.Select, 0.88f).background(if (low) P.onHero.copy(alpha = 0.12f) else P.cardRaised, SquircleShape(50)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.UnfoldLess, null, tint = ink, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.weight(1f))
            Row(Modifier.blurMorph(anim), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(c.code, Tone.Ink)
                if (c.type.isNotEmpty()) Chip(c.type, Tone.Sun)
            }
        }
        // Name, scaled up from the tile.
        Text(name, style = Type.label.copy(fontSize = androidx.compose.ui.unit.TextUnit(30f, androidx.compose.ui.unit.TextUnitType.Sp),
            lineHeight = androidx.compose.ui.unit.TextUnit(34f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = ink, maxLines = 1, modifier = Modifier.padding(top = 16.dp).sharedName(c.code, shared, anim))
        T(c.name, Type.caption, color = ink.copy(alpha = 0.65f), maxLines = 2, modifier = Modifier.padding(top = 2.dp).blurMorph(anim))
        // Bar + percentage, scaled up from the tile.
        Row(Modifier.padding(top = 16.dp).sharedBar(c.code, shared, anim), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.weight(1f).height(14.dp).drawBehind {
                    val h = size.height
                    drawPath(com.student.amizoneplus.ui.kit.squirclePillPath(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, h)), ink.copy(alpha = 0.16f))
                    val w = size.width * (p.toFloat() / 100f)
                    if (w > 0f) drawPath(com.student.amizoneplus.ui.kit.squirclePillPath(androidx.compose.ui.geometry.Rect(0f, 0f, w.coerceAtLeast(h), h)), fill)
                    val tx = size.width * t / 100f
                    drawLine(ink.copy(alpha = 0.5f), Offset(tx, -4.dp.toPx()), Offset(tx, h + 4.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                },
            )
            Text(if (none) "—" else "${p.roundToInt()}%", style = Type.num.copy(fontSize = androidx.compose.ui.unit.TextUnit(40f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = ink, maxLines = 1, modifier = Modifier.padding(start = 14.dp))
        }

        Column(Modifier.blurMorph(anim)) {
            T(if (none) "No classes held yet" else "${c.attended} of ${c.held} attended · " + statusText(c.attended, c.held, t, credits),
                Type.body, color = ink.copy(alpha = 0.8f), modifier = Modifier.padding(top = 10.dp))
            c.internals?.let { T("Internals: %.0f of %.0f".format(it.have, it.max), Type.caption, color = ink.copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp)) }

            // Semester plan, with the manual classes-per-week stepper.
            Column(Modifier.padding(top = 18.dp).fillMaxWidth().background(P.cardRaised, SquircleShape(22.dp)).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        T("SEMESTER PLAN", Type.title)
                        T(planLine(basis, hours, plan?.basePlanned) + (plan?.extraHeld?.takeIf { it > 0 }?.let { " · $it extra held so far" } ?: ""),
                            Type.caption, sub = true, maxLines = 3, modifier = Modifier.padding(top = 4.dp))
                    }
                    Stepper(credits ?: 0.0, ::setCredits)
                }
                if (plan != null) Row(Modifier.padding(top = 16.dp)) {
                    if (plan.extraHeld > 0) Stat("Extra held", plan.extraHeld.toDouble(), null, Modifier.weight(1f))
                    else Stat("Left", plan.remaining.toDouble(), null, Modifier.weight(1f))
                    Stat(if (plan.reachable) "Must attend" else "Extra needed", (if (plan.reachable) plan.need else plan.extraNeeded).toDouble(),
                        if (plan.reachable) null else P.bad, Modifier.weight(1f))
                    Stat("Can miss", plan.canMiss.toDouble(), if (plan.canMiss > 0) P.good else P.warn, Modifier.weight(1f))
                }
            }
            if (plan != null && !plan.reachable) Box(Modifier.padding(top = 12.dp)) { ExtraClassesCard(c.attended, c.held, credits, t, plan) }
            Column(Modifier.padding(top = 12.dp).fillMaxWidth().background(P.cardRaised, SquircleShape(22.dp)).padding(16.dp)) {
                T("BY WEEKDAY", Type.title)
                val weekly = weekdayRates(s.data?.history.orEmpty(), c.code)
                T(if (weekly.any { it != null }) "Last 4 weeks" else "No marked classes in the last 4 weeks", Type.caption, sub = true, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                WeekBars(weekly, bar = P.coral)
            }
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn("Material", BtnKind.Lav, Icons.Outlined.FolderOpen, openLms)
                Btn("Syllabus", BtnKind.Ghost, Icons.Outlined.Description, c.syllabusUrl?.let { u -> { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) } })
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
                .background(if (disabled) P.cardRaised else P.cream, SquircleShape(50)),
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
