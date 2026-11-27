package com.student.amizoneplus.ui.kit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.student.amizoneplus.ui.pets.hangPoints
import com.student.amizoneplus.ui.pets.perch
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** True inside a fused shape: children draw no background and dim instead of shrinking. */
val LocalInFused = compositionLocalOf { false }

/* ───────────── Text ───────────── */

@Composable
fun T(
    text: String,
    style: TextStyle = Type.body,
    modifier: Modifier = Modifier,
    ink: Boolean = false,
    sub: Boolean = false,
    faint: Boolean = false,
    color: Color? = null,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
) {
    val c = color ?: if (faint) P.faint else if (sub) P.sub else P.text
    // Big titles are things pets can stand on.
    val m = if (style.fontSize.isSp && style.fontSize.value >= 30f) modifier.perch() else modifier
    FitText(text, m, c, style, maxLines, align)
}

@Composable
fun Caps(text: String, color: Color, modifier: Modifier = Modifier) = FitText(text, modifier, color, Type.caps, 1, null)

/**
 * Text that ends in "…" when it doesn't fit — and then opens up to show everything
 * when tapped (tap again to fold it back). Text that fits behaves like plain text.
 */
@Composable
fun FitText(text: String, modifier: Modifier, color: Color, style: TextStyle, maxLines: Int, align: TextAlign?) {
    var open by remember(text) { mutableStateOf(false) }
    var cut by remember(text) { mutableStateOf(false) }
    val haptics = LocalHaptics.current
    val canOpen = maxLines != Int.MAX_VALUE && (cut || open)
    Text(
        text,
        modifier
            .then(
                if (canOpen) Modifier.clickable(remember { MutableInteractionSource() }, indication = null) { haptics.select(); open = !open }
                else Modifier,
            )
            .animateContentSize(tween(Motion.MORPH_MS, easing = Motion.emphasized)),
        color = color, style = style,
        maxLines = if (open) Int.MAX_VALUE else maxLines,
        overflow = TextOverflow.Ellipsis, textAlign = align,
        onTextLayout = { if (!open) cut = it.hasVisualOverflow },
    )
}

/** Attendance target: 75 (Amizone's minimum) to 95, in steps of 5. */
@Composable
fun TargetPicker(value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, bg: Color = P.cardRaised, fg: Color = P.text, activeBg: Color = P.cream, activeFg: Color = P.onCream) {
    val haptics = LocalHaptics.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        com.student.amizoneplus.logic.TARGETS.forEach { v ->
            val on = v == value
            val bgc by animateColorAsState(if (on) activeBg else bg, tween(260), label = "tgt")
            Box(
                Modifier.weight(1f).height(40.dp).press({ if (!on) { haptics.select(); onChange(v) } }, Feel.None, 0.92f)
                    .background(bgc, SquircleShape(50)),
                contentAlignment = Alignment.Center,
            ) { Text("$v%", style = Type.label, color = if (on) activeFg else fg, maxLines = 1) }
        }
    }
}

/* ───────────── Press: scale, or dim inside fused shapes; with haptics ───────────── */

enum class Feel { Tap, Press, Select, None }

fun Modifier.press(onClick: (() -> Unit)?, feel: Feel = Feel.Tap, scaleTo: Float = 0.97f): Modifier = composed {
    if (onClick == null) return@composed this
    val haptics = LocalHaptics.current
    val inFused = LocalInFused.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val k by animateFloatAsState(if (pressed) 1f else 0f, if (pressed) Motion.press() else Motion.spring(), label = "press")
    this
        .graphicsLayer {
            if (inFused) alpha = 1f - 0.25f * k
            else { val s = 1f - (1f - scaleTo) * k; scaleX = s; scaleY = s }
        }
        .clickable(interactionSource = source, indication = null) {
            when (feel) {
                Feel.Tap -> haptics.tap()
                Feel.Press -> haptics.press()
                Feel.Select -> haptics.select()
                Feel.None -> Unit
            }
            onClick()
        }
}

/* ───────────── Rise: smooth glide-in, whole block as one layer ───────────── */

@Composable
fun Rise(index: Int = 0, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(30 + index * Motion.STAGGER_MS)
        p.animateTo(1f, tween(Motion.ENTER_MS, easing = Motion.emphasized))
    }
    Box(
        modifier.graphicsLayer {
            alpha = (p.value * 1.4f).coerceAtMost(1f)
            translationY = 12f * density * (1f - p.value)
            compositingStrategy = CompositingStrategy.ModulateAlpha
        },
    ) { content() }
}

/* ───────────── Surfaces ───────────── */

/** Rounded card. Dark grey in dark mode, white in light mode. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    color: Color = P.card,
    onClick: (() -> Unit)? = null,
    feel: Feel = Feel.Tap,
    padding: Dp = L.pad,
    radius: Dp = L.radius,
    content: @Composable ColumnScope.() -> Unit,
) {
    val inFused = LocalInFused.current
    Column(
        modifier
            .perch()
            .press(onClick, feel)
            .then(if (inFused) Modifier else Modifier.background(color, SquircleShape(radius)))
            .padding(padding),
        content = content,
    )
}

/** Old name for [Card]; kept for screens not yet restyled. */
@Composable
fun Ink(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, feel: Feel = Feel.Tap, padding: Dp = L.pad, content: @Composable ColumnScope.() -> Unit) =
    Card(modifier, P.card, onClick, feel, padding, content = content)

/** Round button: cream disc with a dark icon (or any colours). */
@Composable
fun CircleButton(
    icon: ImageVector,
    onClick: (() -> Unit)?,
    size: Dp = 48.dp,
    bg: Color = P.cream,
    fg: Color = P.onCream,
    feel: Feel = Feel.Tap,
    badge: Boolean = false,
) {
    Box(Modifier.perch().size(size).press(onClick, feel, 0.9f).background(bg, SquircleShape(50)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(size * 0.44f))
        if (badge) Box(Modifier.align(Alignment.TopEnd).offset(x = (-8).dp, y = 9.dp).size(8.dp).background(P.red, CircleShape))
    }
}

/** The reference's ↗ button. */
@Composable
fun ArrowButton(onClick: (() -> Unit)?, size: Dp = 44.dp) = CircleButton(Icons.Outlined.NorthEast, onClick, size)

/** Red floating + button. */
@Composable
fun Fab(onClick: () -> Unit, icon: ImageVector = Icons.Outlined.Add) =
    CircleButton(icon, onClick, 56.dp, P.red, Color.White, Feel.Press)

@Composable
fun Avatar(text: String, color: Color = P.sun, size: Dp = 48.dp) {
    val initials = text.split(' ', '/', '-').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase().ifEmpty { "?" }
    Box(Modifier.perch().size(size).background(color, SquircleShape(size * 0.32f)), contentAlignment = Alignment.Center) {
        Text(initials, style = Type.title.copy(fontSize = (size.value * 0.42f).sp), color = P.onHero)
    }
}

/* ───────────── Pills & chips ───────────── */

enum class Tone { Lav, Ink, Good, Warn, Bad, Ghost, Sage, Sky, Sun, Cream }

@Composable
fun Chip(text: String, tone: Tone = Tone.Lav, dot: Color? = null) {
    val (bg, fg) = when (tone) {
        Tone.Lav -> P.lilac to P.onHero
        Tone.Sage -> P.sage to P.onHero
        Tone.Sky -> P.sky to P.onHero
        Tone.Sun -> P.sun.copy(alpha = 0.22f) to P.sun
        Tone.Cream -> P.cream to P.onCream
        Tone.Ink -> P.cardRaised to P.text
        Tone.Good -> P.goodSoft to P.good
        Tone.Warn -> P.warnSoft to P.warn
        Tone.Bad -> P.badSoft to P.bad
        Tone.Ghost -> Color.Transparent to P.sub
    }
    Row(
        Modifier
            .background(bg, SquircleShape(50))
            .then(if (tone == Tone.Ghost) Modifier.border(1.dp, P.line, SquircleShape(50)) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) { Box(Modifier.size(9.dp).background(dot, CircleShape)); Spacer(Modifier.width(7.dp)) }
        Caps(text, fg)
    }
}

/** Pill with a ⌄ — filters and selectors ("all ⌄", "this week ⌄"). */
@Composable
fun DropdownPill(text: String, onClick: () -> Unit, bg: Color = P.cream, fg: Color = P.onCream) {
    Row(
        Modifier.perch().press(onClick, Feel.Select).background(bg, SquircleShape(50)).padding(start = 16.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        T(text, Type.label, color = fg, maxLines = 1)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Outlined.KeyboardArrowDown, null, tint = fg, modifier = Modifier.size(18.dp))
    }
}

/** Big white selector pill (the reference's "The Daily Grind, NY ⌄"). */
@Composable
fun SelectorPill(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier.perch().fillMaxWidth().press(onClick).background(P.cream, SquircleShape(50)).padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = P.onCream, modifier = Modifier.size(20.dp))
        T(text, Type.label, color = P.onCream, maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
        Box(Modifier.size(42.dp).background(P.onCream, SquircleShape(50)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.KeyboardArrowDown, null, tint = P.cream, modifier = Modifier.size(20.dp))
        }
    }
}

/** "64 employees" — a big number with a small label, on a translucent tile. */
@Composable
fun RowScope.StatTile(value: String, label: String, bg: Color, fg: Color = P.onHero, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.weight(1f).perch().press(onClick).background(bg, SquircleShape(22.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(value, style = Type.num, color = fg, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        T(label, Type.caption, color = fg.copy(alpha = 0.8f), modifier = Modifier.padding(top = 6.dp), maxLines = 2)
    }
}

@Composable
fun SearchPill(value: String, onChange: (String) -> Unit, placeholder: String) {
    Row(
        Modifier.perch().fillMaxWidth().background(P.cardRaised, SquircleShape(50)).padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, null, tint = P.sub, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f).padding(start = 12.dp)) {
            if (value.isEmpty()) T(placeholder, Type.body, sub = true, maxLines = 1)
            BasicTextField(value, onChange, singleLine = true, textStyle = Type.body.copy(color = P.text), modifier = Modifier.fillMaxWidth(),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(P.text))
        }
    }
}

/** Live "2:59:12"-style countdown chip. */
@Composable
fun CountdownChip(targetMs: Long, color: Color = P.good) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val ms = (targetMs - now).coerceAtLeast(0)
    Box(Modifier.background(color.copy(alpha = 0.2f), SquircleShape(8.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
        T("%d:%02d:%02d".format(ms / 3_600_000, (ms % 3_600_000) / 60_000, (ms % 60_000) / 1000), Type.label, color = color)
    }
}

/* ───────────── Fused geometry (kept for the menu's unfolding rows) ───────────── */

private fun rr(l: Float, t: Float, r: Float, b: Float, rad: Float) = squirclePath(Rect(l, t, r, b), rad)

private fun Path.unionWith(other: Path): Path = Path().also { it.op(this, other, PathOperation.Union) }
private fun Path.minus(other: Path): Path = Path().also { it.op(this, other, PathOperation.Difference) }

fun stackPath(w: Float, blocks: List<Pair<Float, Float>>, radius: Float, notch: Float): Path {
    var p = Path()
    blocks.forEach { (t, h) -> p = p.unionWith(rr(0f, t, w, t + h, min(radius, h / 2))) }
    for (i in 0 until blocks.size - 1) {
        val bot = blocks[i].first + blocks[i].second
        val top = blocks[i + 1].first
        val g = (top - bot) / 2
        if (g <= 0f) continue
        p = p.unionWith(Path().apply { addRect(Rect(0f, bot, w, top)) })
        val n = min(notch, w / 2 - g)
        p = p.minus(rr(-4 * g, bot, n, top, g)).minus(rr(w - n, bot, w + 4 * g, top, g))
    }
    return p
}

fun pairPath(w: Float, h: Float, gap: Float, radius: Float, notch: Float): Path {
    val w1 = (w - gap) / 2
    val r = min(radius, h / 2)
    val g = gap / 2
    val n = min(notch, h / 2 - g)
    return rr(0f, 0f, w1, h, r).unionWith(rr(w1 + gap, 0f, w, h, r))
        .unionWith(Path().apply { addRect(Rect(w1, 0f, w1 + gap, h)) })
        .minus(rr(w1, -4 * g, w1 + gap, n, g)).minus(rr(w1, h - n, w1 + gap, h + 4 * g, g))
}

@Composable
fun Fused(modifier: Modifier = Modifier, color: Color = P.card, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    val joint = with(d) { L.joint.roundToPx() }
    val radius = with(d) { L.radius.toPx() }
    val notch = with(d) { L.notch.toPx() }
    val blocks = remember { mutableStateOf(listOf<Pair<Float, Float>>()) }
    Layout(
        content = { CompositionLocalProvider(LocalInFused provides true) { content() } },
        modifier = modifier.drawBehind { if (blocks.value.isNotEmpty()) drawPath(stackPath(size.width, blocks.value, radius, notch), color) },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        var y = 0
        val list = ArrayList<Pair<Float, Float>>()
        placeables.forEachIndexed { i, p -> if (i > 0) y += joint; list += y.toFloat() to p.height.toFloat(); y += p.height }
        if (list != blocks.value) blocks.value = list
        layout(constraints.maxWidth, y) {
            var yy = 0
            placeables.forEachIndexed { i, p -> if (i > 0) yy += joint; p.place(0, yy); yy += p.height }
        }
    }
}

@Composable
fun PairCards(modifier: Modifier = Modifier, left: @Composable () -> Unit, right: @Composable () -> Unit) {
    Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(L.block)) {
        Box(Modifier.weight(1f).fillMaxSize().background(P.card, SquircleShape(L.radius))) { left() }
        Box(Modifier.weight(1f).fillMaxSize().background(P.card, SquircleShape(L.radius))) { right() }
    }
}

/* ───────────── Buttons & rows ───────────── */

enum class BtnKind { Lav, Ghost, Ink, Outline }

@Composable
fun RowScope.Btn(label: String, kind: BtnKind = BtnKind.Lav, icon: ImageVector? = null, onClick: (() -> Unit)?) {
    val bg = when (kind) { BtnKind.Lav -> P.cream; BtnKind.Ink -> P.text; else -> Color.Transparent }
    val fg = when (kind) { BtnKind.Lav -> P.onCream; BtnKind.Ink -> P.bg; BtnKind.Ghost, BtnKind.Outline -> P.text }
    val border = when (kind) { BtnKind.Ghost, BtnKind.Outline -> P.line; else -> bg }
    Row(
        Modifier.weight(1f).press(onClick, Feel.Press).height(54.dp).background(bg, SquircleShape(50))
            .border(1.dp, border, SquircleShape(50)).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Caps(label, fg)
    }
}

/** A list row card: title, subtitle, and a trailing ↗ (or custom). */
@Composable
fun OutlineRow(title: String, sub: String? = null, onClick: (() -> Unit)? = null, right: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.perch().fillMaxWidth().press(onClick).heightIn(min = 72.dp).background(P.card, SquircleShape(24.dp)).padding(start = L.pad, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            T(title, Type.title.copy(fontSize = 21.sp, lineHeight = 23.sp), maxLines = 2)
            if (sub != null) T(sub, Type.caption, sub = true, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
        }
        if (right != null) right() else if (onClick != null) ArrowButton(onClick, 40.dp) else Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = P.sub)
    }
}

@Composable
fun SectionLabel(text: String, color: Color = P.sub) = Caps(text, color, Modifier.padding(start = 6.dp, top = 6.dp))

/* ───────────── Numbers ───────────── */

@Composable
fun CountUp(value: Double, decimals: Int = 0, suffix: String = "", style: TextStyle = Type.num, color: Color = P.text, modifier: Modifier = Modifier) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(value) { anim.animateTo(value.toFloat(), tween(900, easing = Motion.emphasized)) }
    Text("%.${decimals}f".format(anim.value) + suffix, modifier, color = color, style = style, maxLines = 1)
}

/** Segmented bar, filling smoothly. */
@Composable
fun SegBar(pct: Double, marker: Int?, tone: Color, modifier: Modifier = Modifier, segments: Int = 20, height: Dp = 32.dp, track: Color = P.cardRaised) {
    val haptics = LocalHaptics.current
    val target = ((pct.coerceIn(0.0, 100.0) / 100.0) * segments).roundToInt()
    var filled by remember { mutableIntStateOf(0) }
    LaunchedEffect(target) {
        filled = 0; delay(200)
        for (n in 1..target) { filled = n; if (n % 4 == 0) haptics.tick(); delay((14 + 30 * (n.toFloat() / target).pow(3)).toLong()) }
    }
    val markIdx = marker?.let { ((it / 100.0) * segments).roundToInt() - 1 } ?: -1
    Row(modifier.height(height), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(segments) { i ->
            val c by animateColorAsState(if (i < filled) tone else track, tween(220), label = "seg")
            Box(Modifier.weight(1f).fillMaxSize().background(c, SquircleShape(4.dp))
                .then(if (i == markIdx && i >= filled) Modifier.border(1.dp, P.sub, SquircleShape(4.dp)) else Modifier))
        }
    }
}

/** Mon–Sun bars, growing smoothly. */
@Composable
fun WeekBars(values: List<Double?>, height: Dp = 72.dp, bar: Color = P.sky, track: Color = P.cardRaised) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(height).background(track, SquircleShape(10.dp)), contentAlignment = Alignment.BottomCenter) {
                    if (v != null) {
                        val f = remember { Animatable(0f) }
                        LaunchedEffect(v) { delay(100L + i * 40L); f.animateTo(v.coerceAtLeast(0.06).toFloat(), tween(650, easing = Motion.emphasized)) }
                        Box(Modifier.fillMaxWidth().height(height * f.value).background(bar, SquircleShape(10.dp)))
                    }
                }
                T(days[i], Type.caption.copy(fontSize = 11.sp), sub = true, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

/** Weekday % circles (reference "Venue Capacity" row). Null days are hatched. */
private val WeekDayFree = com.student.amizoneplus.logic.WeekDay.Kind.Free
private val WeekDayFuture = com.student.amizoneplus.logic.WeekDay.Kind.Future

/** No classes on this day (past or ahead): drawn with an emoji instead of a number. */
val com.student.amizoneplus.logic.WeekDay.restDay: Boolean
    get() = total == 0 && (kind == WeekDayFree || kind == WeekDayFuture)

@Composable
fun DayCircles(days: List<com.student.amizoneplus.logic.WeekDay>, selected: Int, onSelect: (Int) -> Unit, onColor: Color = P.onHero, todayIdx: Int = -1) {
    // The "no classes" bubble opens only when a free day is tapped (tap again to close).
    var bubble by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEachIndexed { i, d ->
                val active = i == selected
                val scale by animateFloatAsState(if (active) (if (d.restDay) 1.16f else 1.08f) else 1f, Motion.spring(), label = "dc")
                val emojiScale by animateFloatAsState(if (active && d.restDay) 1.22f else 1f, Motion.spring(), label = "de")
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.press({ bubble = if (i == selected && bubble) false else d.restDay; onSelect(i) }, Feel.Select, 0.9f)) {
                    T(d.label, Type.caption, color = onColor.copy(alpha = 0.75f))
                    Box(
                        Modifier.padding(top = 8.dp).size(40.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                            .drawBehind {
                                val r = size.minDimension / 2
                                when {
                                    active -> drawCircle(P.onHero)
                                    d.kind == WeekDayFuture -> {
                                        drawCircle(Color.White.copy(alpha = 0.18f))
                                        clipRect { // hatched: still ahead
                                            var x = -size.width
                                            while (x < size.width) {
                                                drawLine(Color.White.copy(alpha = 0.30f), Offset(x, size.height), Offset(x + size.width, 0f), 1.5f)
                                                x += 6.dp.toPx()
                                            }
                                        }
                                    }
                                    d.kind == WeekDayFree -> drawCircle(Color.White.copy(alpha = 0.30f))
                                    d.kind == com.student.amizoneplus.logic.WeekDay.Kind.Pending -> drawCircle(Color.White.copy(alpha = 0.55f), r - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                                    else -> { drawCircle(Color.White.copy(alpha = 0.55f)); drawCircle(Color.White, r - 3.dp.toPx()) }
                                }
                            }
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        val ink = if (active) Color.White else P.onHero
                        when {
                            d.restDay -> Text(Quips.freeDayFor(d.epochDay), fontSize = 18.sp,
                                modifier = Modifier.graphicsLayer { scaleX = emojiScale; scaleY = emojiScale; alpha = if (d.kind == WeekDayFuture && !active) 0.75f else 1f })
                            d.kind == com.student.amizoneplus.logic.WeekDay.Kind.Rate ->
                                T("${(d.rate * 100).roundToInt()}", Type.caption.copy(fontSize = 11.sp), color = ink)
                            else -> T("${d.total}", Type.caption.copy(fontSize = 11.sp), color = ink) // classes, not marked yet / ahead
                        }
                    }
                }
            }
        }

        // The speech bubble that unfolds from a tapped emoji.
        val sel = days.getOrNull(selected)
        val caretX by animateFloatAsState((selected + 0.5f) / days.size.coerceAtLeast(1), Motion.spring(), label = "caret")
        androidx.compose.animation.AnimatedVisibility(
            visible = bubble && sel?.restDay == true,
            enter = androidx.compose.animation.expandVertically(tween(Motion.MORPH_MS, easing = Motion.emphasized), expandFrom = Alignment.Top) +
                androidx.compose.animation.scaleIn(tween(Motion.MORPH_MS, easing = Motion.emphasized), initialScale = 0.4f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(caretX, 0f)) +
                androidx.compose.animation.fadeIn(tween(220)),
            exit = androidx.compose.animation.shrinkVertically(tween(300, easing = Motion.emphasized), shrinkTowards = Alignment.Top) +
                androidx.compose.animation.scaleOut(tween(300, easing = Motion.emphasized), targetScale = 0.5f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(caretX, 0f)) +
                androidx.compose.animation.fadeOut(tween(160)),
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                // caret pointing at the tapped day
                Box(Modifier.fillMaxWidth().height(9.dp).drawBehind {
                    val cx = size.width * caretX
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx - 9.dp.toPx(), size.height); lineTo(cx, 0f); lineTo(cx + 9.dp.toPx(), size.height); close()
                    }
                    drawPath(path, P.onHero)
                })
                Row(
                    Modifier.fillMaxWidth().background(P.onHero, SquircleShape(22.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    androidx.compose.animation.AnimatedContent(sel?.epochDay ?: 0L, transitionSpec = {
                        (androidx.compose.animation.fadeIn(tween(220)) + androidx.compose.animation.scaleIn(tween(320, easing = Motion.emphasized), initialScale = 0.6f)) togetherWith
                            androidx.compose.animation.fadeOut(tween(120))
                    }, label = "bubbleEmoji") { e -> Text(Quips.freeDayFor(e), fontSize = 30.sp) }
                    Column(Modifier.weight(1f)) {
                        T("No classes scheduled", Type.label, color = Color.White)
                        val name = if (selected == todayIdx) "Today" else sel?.let { fullDay(it.label) }.orEmpty()
                        T(if (sel?.kind == WeekDayFuture) "$name is free. Plan something fun." else "$name was a free day. Nothing to count.",
                            Type.caption, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}



private fun fullDay(short: String) = when (short) {
    "Mon" -> "Monday"; "Tue" -> "Tuesday"; "Wed" -> "Wednesday"; "Thu" -> "Thursday"
    "Fri" -> "Friday"; "Sat" -> "Saturday"; else -> "Sunday"
}

/** One line about the selected day under the circles (rest days get the bubble instead). */
fun dayLine(d: com.student.amizoneplus.logic.WeekDay, isToday: Boolean): String? {
    if (d.restDay) return null
    val name = if (isToday) "Today" else d.label
    return when (d.kind) {
        com.student.amizoneplus.logic.WeekDay.Kind.Rate -> "$name · ${d.present} of ${d.marked} present" + if (d.total > d.marked) " · ${d.total - d.marked} not marked yet" else ""
        com.student.amizoneplus.logic.WeekDay.Kind.Pending -> "$name · ${d.total} class${if (d.total == 1) "" else "es"}, attendance not marked yet"
        com.student.amizoneplus.logic.WeekDay.Kind.Future -> "$name · ${d.total} class${if (d.total == 1) "" else "es"} ahead"
        else -> null
    }
}

/**
 * Area chart (0..1 values) that draws itself in and morphs between data sets.
 * A dotted guide marks [highlight].
 */
@Composable
fun AreaChart(values: List<Float>, modifier: Modifier = Modifier, fill: Color = P.coral, highlight: Int? = null, guide: Color = P.onHero) {
    val reveal = remember { Animatable(0f) }
    val morph = remember { Animatable(1f) }
    var from by remember { mutableStateOf(values) }
    var to by remember { mutableStateOf(values) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(900, easing = Motion.emphasized)) }
    LaunchedEffect(values) {
        if (values != to) {
            from = interp(from, to, morph.value); to = values
            morph.snapTo(0f); morph.animateTo(1f, tween(Motion.MORPH_MS, easing = Motion.emphasized))
        }
    }
    Canvas(modifier) {
        val pts = interp(from, to, morph.value)
        if (pts.size < 2) return@Canvas
        val w = size.width; val h = size.height
        val step = w / (pts.size - 1)
        fun y(v: Float) = h - (v.coerceIn(0f, 1f) * h * 0.86f) - h * 0.04f
        val line = Path().apply {
            moveTo(0f, y(pts[0]))
            for (i in 1 until pts.size) {
                val x0 = (i - 1) * step; val x1 = i * step
                cubicTo(x0 + step / 2, y(pts[i - 1]), x1 - step / 2, y(pts[i]), x1, y(pts[i]))
            }
        }
        val area = Path().apply { addPath(line); lineTo(w, h); lineTo(0f, h); close() }
        clipRect(right = w * reveal.value) {
            drawPath(area, Brush.verticalGradient(listOf(fill, fill.copy(alpha = 0.55f))))
            drawPath(line, guide, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            pts.forEachIndexed { i, v -> if (i % 3 == 0 || i == pts.lastIndex) drawCircle(guide, 3.5.dp.toPx(), Offset(i * step, y(v))) }
        }
        highlight?.takeIf { it in pts.indices }?.let { i ->
            val x = i * step
            drawLine(guide, Offset(x, 0f), Offset(x, h), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 6.dp.toPx())), cap = StrokeCap.Round)
            drawCircle(guide, 5.dp.toPx(), Offset(x, y(pts[i])))
        }
    }
}

private fun interp(a: List<Float>, b: List<Float>, t: Float): List<Float> {
    if (a.isEmpty()) return b
    val n = b.size
    return List(n) { i ->
        val av = a.getOrElse((i.toFloat() / (n - 1).coerceAtLeast(1) * (a.size - 1)).roundToInt()) { 0f }
        av + (b[i] - av) * t
    }
}

/**
 * Clock dial (reference "Operational Timing"): 12-hour face with tick marks,
 * today's classes as arcs (current/next highlighted), and live hands.
 */
@Composable
fun ClockDial(arcs: List<Triple<Long, Long, Boolean>>, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(arcs) { sweep.snapTo(0f); sweep.animateTo(1f, tween(1100, easing = Motion.emphasized)) }
    // A cat can hang from the hour and minute hands (a point ~60% along each).
    val hands = Modifier.hangPoints { b ->
        val cal = Calendar.getInstance()
        val r = min(b.width, b.height) / 2
        val c = b.center
        val hA = Math.toRadians(((cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f) / 12f * 360f - 90f).toDouble())
        val mA = Math.toRadians(((cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f) / 60f * 360f - 90f).toDouble())
        listOf(
            Offset((c.x + cos(mA) * r * 0.55f).toFloat(), (c.y + sin(mA) * r * 0.55f).toFloat()),
            Offset((c.x + cos(hA) * r * 0.36f).toFloat(), (c.y + sin(hA) * r * 0.36f).toFloat()),
        )
    }
    Canvas(modifier.then(hands)) {
        val c = center
        val r = size.minDimension / 2
        drawCircle(Color.White, r)
        // Fine ticks
        for (i in 0 until 120) {
            val a = Math.toRadians(i * 3.0 - 90)
            val len = if (i % 10 == 0) r * 0.1f else r * 0.04f
            val o = Offset((c.x + cos(a) * r * 0.97f).toFloat(), (c.y + sin(a) * r * 0.97f).toFloat())
            val inn = Offset((c.x + cos(a) * (r * 0.97f - len)).toFloat(), (c.y + sin(a) * (r * 0.97f - len)).toFloat())
            drawLine(if (i % 10 == 0) P.onHero else P.onHero.copy(alpha = 0.18f), inn, o, if (i % 10 == 0) 2.dp.toPx() else 1.dp.toPx())
        }
        fun angleOf(ms: Long): Float {
            val cal = Calendar.getInstance().apply { timeInMillis = ms }
            val h = cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f + cal.get(Calendar.SECOND) / 3600f
            return h / 12f * 360f - 90f
        }
        // Class arcs on two rings: highlighted (current/next) on the inner ring in coral.
        val ringW = r * 0.13f
        arcs.forEach { (start, end, hot) ->
            val a0 = angleOf(start)
            val span = ((end - start) / 3_600_000f) * 30f * sweep.value
            val rr2 = if (hot) r * 0.42f else r * 0.56f
            drawArc(if (hot) P.coral else P.sage.copy(alpha = 0.9f), a0, span.coerceAtMost(359f), false,
                topLeft = Offset(c.x - rr2, c.y - rr2), size = Size(rr2 * 2, rr2 * 2), style = Stroke(ringW, cap = StrokeCap.Butt))
        }
        // Hands
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val hA = Math.toRadians(((cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f) / 12f * 360f - 90f).toDouble())
        val mA = Math.toRadians(((cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f) / 60f * 360f - 90f).toDouble())
        val sA = Math.toRadians((cal.get(Calendar.SECOND) / 60f * 360f - 90f).toDouble())
        drawLine(P.onHero, c, Offset((c.x + cos(hA) * r * 0.45f).toFloat(), (c.y + sin(hA) * r * 0.45f).toFloat()), 3.dp.toPx(), cap = StrokeCap.Round)
        drawLine(P.onHero, c, Offset((c.x + cos(mA) * r * 0.7f).toFloat(), (c.y + sin(mA) * r * 0.7f).toFloat()), 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(P.red, Offset((c.x - cos(sA) * r * 0.12f).toFloat(), (c.y - sin(sA) * r * 0.12f).toFloat()),
            Offset((c.x + cos(sA) * r * 0.78f).toFloat(), (c.y + sin(sA) * r * 0.78f).toFloat()), 1.5.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(P.onHero, 4.dp.toPx(), c)
    }
}

/** Segmented toggle with a sliding indicator. */
@Composable
fun <V> Segmented(options: List<Pair<V, String>>, value: V, onChange: (V) -> Unit, modifier: Modifier = Modifier, bg: Color = P.cardRaised, active: Color = P.cream) {
    val haptics = LocalHaptics.current
    BoxWithConstraints(modifier.fillMaxWidth().height(52.dp).background(bg, SquircleShape(50)).padding(4.dp)) {
        val segW = maxWidth / options.size
        val idx = options.indexOfFirst { it.first == value }.coerceAtLeast(0)
        val segPx = with(androidx.compose.ui.platform.LocalDensity.current) { segW.toPx() }
        val scope = rememberCoroutineScope()
        val pos = remember { Animatable(segPx * idx) }
        var dragging by remember { mutableStateOf(false) }
        var dragX by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(idx, segPx) { if (!dragging) pos.animateTo(segPx * idx, androidx.compose.animation.core.spring(0.78f, 420f)) }
        val idxNow by rememberUpdatedState(idx)
        val valueNow by rememberUpdatedState(onChange)
        val grow by animateFloatAsState(if (dragging) 1.04f else 1f,
            if (dragging) androidx.compose.animation.core.spring(0.7f, 500f) else androidx.compose.animation.core.spring(0.45f, 380f), label = "seggrow")
        val shown = if (dragging) (dragX / segPx).roundToInt().coerceIn(0, options.lastIndex) else idx
        Box(
            Modifier.offset { IntOffset((if (dragging) dragX else pos.value).roundToInt(), 0) }.width(segW).fillMaxSize()
                .graphicsLayer { scaleX = grow; scaleY = grow }
                .background(active, SquircleShape(50)),
        )
        Row(Modifier.fillMaxSize().pointerInput(segPx) {
            detectHorizontalDragGestures(
                onDragStart = { dragging = true; dragX = segPx * idxNow; haptics.press() },
                onDragEnd = { dragging = false; scope.launch { pos.snapTo(dragX); pos.animateTo(segPx * idxNow, androidx.compose.animation.core.spring(0.6f, 380f)) } },
                onDragCancel = { dragging = false },
            ) { ch, dx ->
                ch.consume()
                dragX = (dragX + dx).coerceIn(0f, segPx * options.lastIndex)
                val t = (dragX / segPx).roundToInt().coerceIn(0, options.lastIndex)
                if (t != idxNow) { haptics.tick(); valueNow(options[t].first) }
            }
        }) {
            options.forEach { (v, label) ->
                Box(
                    Modifier.weight(1f).fillMaxSize().clickable(remember { MutableInteractionSource() }, null) { if (v != value) { haptics.select(); onChange(v) } },
                    contentAlignment = Alignment.Center,
                ) { Caps(label, if (options.indexOfFirst { it.first == v } == shown) P.onCream else P.sub) }
            }
        }
    }
}

/* ───────────── UnfoldCard: row → filled card → detail grows downward ───────────── */

@Composable
fun UnfoldCard(open: Boolean, onToggle: () -> Unit, head: @Composable RowScope.(onInk: Boolean) -> Unit, body: @Composable ColumnScope.() -> Unit) {
    val d = LocalDensity.current
    val radius = with(d) { 24.dp.toPx() }
    val notch = with(d) { L.notch.toPx() }
    val joint = with(d) { L.joint.toPx() }
    val fill = remember { Animatable(if (open) 1f else 0f) }
    val grow = remember { Animatable(if (open) 1f else 0f) }
    var headH by remember { mutableFloatStateOf(0f) }
    var bodyH by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(open) {
        if (open) coroutineScope {
            launch { fill.animateTo(1f, tween(280, easing = Motion.standard)) }
            delay(100); grow.animateTo(1f, tween(Motion.MORPH_MS, easing = Motion.emphasized))
        } else coroutineScope {
            launch { grow.animateTo(0f, tween(300, easing = Motion.standard)) }
            delay(160); fill.animateTo(0f, tween(240, easing = Motion.standard))
        }
    }

    Column(
        Modifier.fillMaxWidth().drawBehind {
            if (headH <= 0f) return@drawBehind
            val h = bodyH * grow.value
            val blocks = if (h > 2f) listOf(0f to headH, headH + joint to h) else listOf(0f to headH)
            val path = stackPath(size.width, blocks, radius, notch)
            drawPath(path, P.card)
            drawPath(path, P.sky, alpha = fill.value * 0.0f)
        },
    ) {
        Box(Modifier.fillMaxWidth().onSizeChanged { headH = it.height.toFloat() }.clickable(remember { MutableInteractionSource() }, null, onClick = onToggle)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = L.pad).graphicsLayer { alpha = 1f - fill.value },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { head(false) }
            Row(Modifier.matchParentSize().padding(horizontal = L.pad).graphicsLayer { alpha = fill.value },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { head(true) }
        }
        Layout(
            content = {
                Column(Modifier.graphicsLayer {
                    alpha = ((grow.value - 0.2f) / 0.6f).coerceIn(0f, 1f)
                    translationY = -10f * density * (1f - grow.value)
                }) { CompositionLocalProvider(LocalInFused provides true) { body() } }
            },
            modifier = Modifier.graphicsLayer { clip = true },
        ) { measurables, constraints ->
            val p = measurables.first().measure(constraints.copy(minHeight = 0, maxHeight = androidx.compose.ui.unit.Constraints.Infinity))
            if (p.height.toFloat() != bodyH) bodyH = p.height.toFloat()
            val visible = if (grow.value > 0.001f) ((p.height + joint) * grow.value).roundToInt() else 0
            layout(constraints.maxWidth, visible) { p.place(0, joint.roundToInt()) }
        }
    }
}

/* ───────────── Big faint countdown ───────────── */

@Composable
fun BigCountdown(targetMs: Long, caption: String) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val ms = (targetMs - now).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        T(caption, Type.caption, sub = true, maxLines = 1)
        Text("%d:%02d:%02d".format(ms / 3_600_000, (ms % 3_600_000) / 60_000, (ms % 60_000) / 1000),
            style = Type.hero.copy(fontSize = 96.sp, lineHeight = 96.sp), color = P.text.copy(alpha = 0.10f), maxLines = 1)
    }
}
