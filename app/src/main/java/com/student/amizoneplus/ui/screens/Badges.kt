package com.student.amizoneplus.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.amizoneplus.logic.ALL_BADGES
import com.student.amizoneplus.logic.Badge
import com.student.amizoneplus.ui.kit.BtnKind
import com.student.amizoneplus.ui.kit.Btn
import com.student.amizoneplus.ui.kit.Fused
import com.student.amizoneplus.ui.kit.Ink
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Five-point star badge with a short label (like the "15k Master" badge in the reference). */
@Composable
fun BadgeArt(short: String, tag: String?, size: Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = this.size.width / 2
            val outer = c * 0.92f
            val inner = c * 0.52f
            val star = Path().apply {
                for (i in 0 until 10) {
                    val a = Math.PI * 2 / 10 * i - Math.PI / 2
                    val r = if (i % 2 == 0) outer else inner
                    val x = (c + r * cos(a)).toFloat()
                    val y = (c + r * sin(a)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(star, P.ink)
            drawPath(star, P.ink, style = Stroke(width = c * 0.12f, join = StrokeJoin.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            T(short, Type.caption.copy(fontSize = (size.value * (if (short.length > 3) 0.15f else 0.19f)).sp, fontWeight = FontWeight.SemiBold), ink = true)
            if (tag != null) T(tag, Type.caption.copy(fontSize = (size.value * 0.08f).sp), ink = true, sub = true)
        }
    }
}

/** Reward screen with a particle burst and a short haptic pattern. */
@Composable
fun Celebration(badge: Badge, onClose: () -> Unit) {
    val haptics = LocalHaptics.current
    BackHandler(onBack = onClose)
    val zoom = remember { Animatable(0.4f) }
    val burst = remember { Animatable(0f) }
    val particles = remember(badge.id) {
        List(26) { i ->
            Triple(Math.PI * 2 * i / 26 + Random.nextDouble(0.0, 0.3), 70f + Random.nextFloat() * 70f, 3f + Random.nextFloat() * 7f)
        }
    }
    LaunchedEffect(badge.id) {
        haptics.celebrate()
        launch { zoom.animateTo(1f, Motion.soft()) }
        delay(150)
        burst.animateTo(1f, tween(1100, easing = Motion.emphasized))
    }
    Column(
        Modifier.fillMaxSize().background(P.canvas).statusBarsPadding().navigationBarsPadding().padding(L.page),
    ) {
        Fused {
            Ink(padding = L.pad) { T("amizone+", Type.title, ink = true) }
            Ink { T(badge.title, Type.label, ink = true); T("New badge", Type.caption, ink = true, sub = true) }
        }
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            T("Congratulations!", Type.title.copy(fontSize = 26.sp), align = TextAlign.Center)
            T(badge.detail, Type.body, sub = true, align = TextAlign.Center, modifier = Modifier.padding(top = 8.dp).widthIn(max = 280.dp))
            Box(Modifier.padding(top = 48.dp).size(240.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val k = burst.value
                    val alpha = if (k < 0.7f) 1f else 1f - (k - 0.7f) / 0.3f
                    particles.forEachIndexed { i, (a, dist, r) ->
                        val p = Offset(
                            center.x + (cos(a) * dist * k).toFloat() * density,
                            center.y + ((sin(a) * dist * k) + 30 * k * k).toFloat() * density,
                        )
                        drawCircle(if (i % 3 == 0) P.faint else P.ink, radius = r * density * (1 - 0.4f * k) / 2, center = p, alpha = alpha * (if (k > 0f) 1f else 0f))
                    }
                }
                Box(
                    Modifier.size(120.dp).graphicsLayer { scaleX = zoom.value; scaleY = zoom.value }
                        .border(1.dp, P.line, RoundedCornerShape(L.radius)).background(P.canvas, RoundedCornerShape(L.radius)),
                    contentAlignment = Alignment.Center,
                ) { BadgeArt(badge.short, badge.tag, 84.dp) }
            }
            T("New badge", Type.label, modifier = Modifier.padding(top = 16.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Nice", BtnKind.Ink, onClick = onClose)
            Btn("Close", BtnKind.Outline, onClick = onClose)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgeGallery(earned: List<Badge>, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val locked = ALL_BADGES.filter { l -> earned.none { it.title == l.title } }
    Column(Modifier.fillMaxSize().background(P.canvas).statusBarsPadding().navigationBarsPadding().padding(L.page)) {
        Fused {
            Ink { T("Badges", Type.title, ink = true) }
            Ink { T("${earned.size}", Type.num, ink = true); T("unlocked so far", Type.caption, ink = true, sub = true) }
        }
        FlowRow(
            Modifier.padding(top = L.block).weight(1f).verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(L.block),
            verticalArrangement = Arrangement.spacedBy(L.block),
            maxItemsInEachRow = 3,
        ) {
            (earned + locked).forEachIndexed { i, b ->
                val isLocked = i >= earned.size
                Column(
                    Modifier.weight(1f).aspectRatio(0.85f).graphicsLayer { alpha = if (isLocked) 0.45f else 1f }
                        .border(1.2.dp, if (isLocked) P.hairline else P.line, RoundedCornerShape(L.radius)).padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    BadgeArt(b.short, b.tag, 58.dp)
                    T(b.title, Type.caption, align = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
                }
            }
            repeat((3 - (earned.size + locked.size) % 3) % 3) { Spacer(Modifier.weight(1f)) }
        }
        Row { Btn("Close", BtnKind.Ink, onClick = onClose) }
    }
}
