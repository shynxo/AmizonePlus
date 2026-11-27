package com.student.amizoneplus.ui.shell

import com.student.amizoneplus.ui.kit.SquircleShape
import android.graphics.ColorMatrixColorFilter
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/*
 * Pull to refresh, after the reference reel's coral loader: the page stretches like
 * rubber as you pull, a coral panel opens behind it, and one cream word sits in the
 * middle. Each letter lives in its own clipped slot and rolls down to the next word,
 * staggered left to right; one sentence plays out word by word. The phone's motor
 * builds tension tick by tick until a firm thunk says "let go".
 */

// One sentence, a word per frame (like "DOE MEE → ONTMOET → GROEI"), then a few extras.
private val LOADING_WORDS = listOf("FETCHING", "FROM", "SHITTY", "AMIZONE", "HANG ON", "BRIBING", "SERVERS", "ALMOST")
private const val ROLL_MS = 420
private const val LETTER_STAGGER_MS = 34
private const val HOLD_MS = 560

@Composable
fun StretchRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val haptics = LocalHaptics.current
    val d = LocalDensity.current
    val scope = rememberCoroutineScope()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val threshold = with(d) { (statusTop + 78.dp).toPx() }
    val maxPull = threshold * 1.75f
    val hold = with(d) { (statusTop + 88.dp).toPx() }
    val stepFar = with(d) { 26.dp.toPx() }
    val stepNear = with(d) { 7.dp.toPx() }

    var pull by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var mine by remember { mutableStateOf(false) } // this refresh came from a pull
    var armed by remember { mutableStateOf(false) }
    var lastTick by remember { mutableFloatStateOf(0f) }
    var anim by remember { mutableStateOf<Job?>(null) }
    val refreshingNow by rememberUpdatedState(refreshing)
    val onRefreshNow by rememberUpdatedState(onRefresh)

    fun settleTo(target: Float, bouncy: Boolean = false) {
        anim?.cancel()
        anim = scope.launch {
            animate(pull, target, animationSpec = spring(dampingRatio = if (bouncy) 0.55f else 0.85f, stiffness = if (bouncy) 240f else 320f)) { v, _ -> pull = v }
        }
    }

    fun feel(p: Float) {
        val prog = (p / threshold).coerceIn(0f, 1f)
        if (!armed && p >= threshold) { armed = true; haptics.armed(); return }
        if (armed && p < threshold * 0.88f) { armed = false; haptics.disarm(); lastTick = p; return }
        if (armed) return
        val step = stepFar + (stepNear - stepFar) * prog
        if (abs(p - lastTick) >= step) { lastTick = p; haptics.tension(prog) }
    }

    val conn = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Scrolling back up while pulled: shrink the pull first.
                if (source != NestedScrollSource.UserInput || available.y >= 0f || pull <= 0f || (refreshingNow && mine)) return Offset.Zero
                anim?.cancel()
                val next = (pull + available.y).coerceAtLeast(0f)
                val used = next - pull
                pull = next; dragging = true; feel(next)
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Dragging down at the very top: the leftover becomes stretch.
                if (source != NestedScrollSource.UserInput || available.y <= 0f || (refreshingNow && mine)) return Offset.Zero
                anim?.cancel()
                val resist = (1f - pull / maxPull).coerceIn(0f, 1f)
                pull = (pull + available.y * 0.8f * resist).coerceAtMost(maxPull)
                dragging = true; feel(pull)
                return available
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!dragging) return Velocity.Zero
                dragging = false
                if (pull >= threshold && !refreshingNow) {
                    mine = true
                    haptics.launch()
                    onRefreshNow()
                    settleTo(hold)
                    scope.launch { // if the refresh never got going, don't hang around
                        delay(1500)
                        if (!refreshingNow && mine) { mine = false; armed = false; settleTo(0f, bouncy = true) }
                    }
                } else if (!(refreshingNow && mine)) {
                    armed = false
                    settleTo(0f, bouncy = true)
                }
                return available // the pull swallows the fling
            }
        }
    }

    LaunchedEffect(refreshing) {
        if (!refreshing && mine) {
            delay(650) // let "DONE" roll in first
            mine = false; armed = false
            haptics.success()
            settleTo(0f, bouncy = true)
        }
    }

    Box(modifier.fillMaxSize().nestedScroll(conn)) {
        if (pull > 0.5f) RefreshPanel(pull, pull / threshold, mine && refreshing, mine && !refreshing, statusTop)
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                translationY = pull
                val st = (pull / threshold).coerceIn(0f, 1.6f)
                scaleY = 1f + 0.055f * st // rubber stretch
                // No sideways squeeze: the page stays as wide as the coral beneath it.
                transformOrigin = TransformOrigin(0.5f, 0f)
                val r = 30.dp.toPx() * min(1f, st * 2.5f)
                shape = com.student.amizoneplus.ui.kit.SquirclePxShape(r, r, 0f, 0f)
                clip = pull > 0.5f
            },
        ) { content() } // transparent, so the coral runs right down to the first row
    }
}

/** The coral panel revealed behind the page, with the rolling word in the middle. */
@Composable
private fun RefreshPanel(heightPx: Float, progress: Float, loading: Boolean, done: Boolean, statusTop: androidx.compose.ui.unit.Dp) {
    val d = LocalDensity.current
    var step by remember { mutableStateOf(0) }
    LaunchedEffect(loading) {
        step = 0
        if (loading) while (true) {
            delay((ROLL_MS + HOLD_MS).toLong())
            step = (step + 1) % LOADING_WORDS.size
        }
    }
    val word = when {
        done -> "DONE!"
        loading -> LOADING_WORDS[step]
        progress >= 1f -> "LET GO"
        else -> "PULL"
    }
    // The panel also fills the page's own top margin (status bar + gap) under the moved page.
    // Only as far as the page has actually moved, so no coral flashes under the status bar
    // as the page springs back; and the panel fades out over the last stretch.
    // Every page is opaque with rounded top corners, so the coral only fills the gap.
    val total = with(d) { heightPx.toDp() }
    Box(
        // A corner's depth more, hidden behind the page except where its corners round off.
        Modifier.fillMaxWidth().height(total + 32.dp)
            .graphicsLayer { alpha = (progress * 3f).coerceIn(0f, 1f) }
            .background(P.coral),
    ) {
        // The word sits in the middle of all the visible coral, below the status bar.
        Box(Modifier.fillMaxWidth().height(total).padding(top = statusTop), contentAlignment = Alignment.Center) {
            RollingWord(word, Modifier.graphicsLayer { alpha = min(1f, progress * 2f) })
        }
    }
}

private val CREAM = Color(0xFFF3E1D8)

/**
 * A word whose letters each roll down inside their own clipped slot to the next word,
 * staggered left to right. Slots resize to the new letters, so the word stays centred.
 * Letters 2 and 3 are drawn as outlines (the reference's hollow "OE").
 */
@Composable
private fun RollingWord(word: String, modifier: Modifier = Modifier) {
    // Every change re-rolls every slot, even where the letter happens to be the same.
    var gen by remember { mutableStateOf(0) }
    var shown by remember { mutableStateOf(word) }
    if (word != shown) { shown = word; gen++ }
    val slots = 10
    val style = Type.display.copy(fontSize = 44.sp, lineHeight = 44.sp)
    val stroke = Stroke(width = with(LocalDensity.current) { 1.4.dp.toPx() })
    androidx.compose.foundation.layout.Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until slots) {
            val ch = shown.getOrNull(i)?.toString() ?: ""
            val spec = tween<androidx.compose.ui.unit.IntOffset>(ROLL_MS, delayMillis = i * LETTER_STAGGER_MS, easing = com.student.amizoneplus.ui.theme.Motion.emphasized)
            androidx.compose.animation.AnimatedContent(
                targetState = gen to ch,
                transitionSpec = {
                    (androidx.compose.animation.slideInVertically(spec) { -it }) togetherWith
                        (androidx.compose.animation.slideOutVertically(spec) { it }) using
                        androidx.compose.animation.SizeTransform(clip = true) { _, _ ->
                            tween(ROLL_MS, delayMillis = i * LETTER_STAGGER_MS, easing = com.student.amizoneplus.ui.theme.Motion.emphasized)
                        }
                },
                contentAlignment = Alignment.Center,
                label = "slot$i",
            ) { (_, c) ->
                if (c.isNotEmpty()) Text(
                    c,
                    style = if (i == 1 || i == 2) style.copy(drawStyle = stroke) else style,
                    color = CREAM,
                    maxLines = 1,
                )
            }
        }
    }
}
