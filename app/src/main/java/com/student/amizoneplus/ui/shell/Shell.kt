package com.student.amizoneplus.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.student.amizoneplus.ui.kit.CircleButton
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

enum class Route { Today, Schedule, Subjects, Lms, More }

/** The pull-down quick menu, reachable from every screen. */
class MenuController(val isOpen: Boolean, val open: () -> Unit, val close: () -> Unit) {
    fun toggle() = if (isOpen) close() else open()
}

val LocalMenu = compositionLocalOf { MenuController(false, {}, {}) }

fun Modifier.swipeDownToOpen(onOpen: () -> Unit): Modifier = pointerInput(Unit) {
    var total = 0f
    detectVerticalDragGestures(onDragStart = { total = 0f }, onDragEnd = { if (total > 36.dp.toPx()) onOpen() }) { _, dy -> total += dy }
}

/* ───────────── Small header pieces ───────────── */

/** Menu toggle shown in headers. */
@Composable
fun MenuButton(bg: Color = P.cream, fg: Color = P.onCream) {
    val menu = LocalMenu.current
    CircleButton(if (menu.isOpen) Icons.Outlined.Close else Icons.Outlined.Apps, { menu.toggle() }, 48.dp, bg, fg, Feel.Select)
}

/** Bar used by the menu overlay & sheets: wordmark + close/menu. */
@Composable
fun HeaderBar(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(L.header), verticalAlignment = Alignment.CenterVertically) {
        T("AMIZONE+", Type.headline, modifier = Modifier.weight(1f))
        MenuButton()
    }
}

@Composable
fun HeaderInk(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth()) { HeaderBar() }

@Composable
fun ErrorBanner(text: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().press(onClick).background(P.badSoft, RoundedCornerShape(22.dp)).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = P.bad)
        T("$text  $action", Type.caption, modifier = Modifier.weight(1f))
    }
}

/* ───────────── Pages ───────────── */

private val navClearance: Dp @Composable get() =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + L.nav + 36.dp

/**
 * Hero + sheet (reference "Operational Timing" / "Workers"): a colour hero at the
 * top; the content sheet with large rounded corners glides up over it on scroll
 * while the hero eases back and fades.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroPage(
    heroColor: Color,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    banner: (@Composable () -> Unit)? = null,
    scroll: ScrollState = rememberScrollState(),
    hero: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptics = LocalHaptics.current
    val d = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    var heroH by remember { mutableFloatStateOf(0f) }
    val overlap = 28.dp

    Box(Modifier.fillMaxSize().background(P.bg)) {
        StretchRefresh(refreshing, onRefresh, Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                // Hero: scrolls at ~1/3 speed (parallax) and eases back; being part of the
                // scroll column, its buttons receive taps.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { heroH = it.height.toFloat() }
                        .graphicsLayer {
                            val k = if (heroH > 0) (scroll.value / heroH).coerceIn(0f, 1f) else 0f
                            translationY = scroll.value * 0.65f
                            alpha = 1f - k * 0.9f
                            val s = 1f - 0.04f * k
                            scaleX = s; scaleY = s
                        }
                        .background(heroColor)
                        .padding(top = statusTop + 8.dp, start = L.page, end = L.page, bottom = overlap + 16.dp),
                    content = hero,
                )
                // Sheet: overlaps the hero's bottom edge; its corners soften as it reaches the top.
                val k = if (heroH > 0) (scroll.value / heroH).coerceIn(0f, 1f) else 0f
                Column(
                    Modifier
                        .layout { m, c ->
                            val p = m.measure(c)
                            val o = overlap.roundToPx()
                            layout(p.width, p.height - o) { p.place(0, -o) }
                        }
                        .pointerInput(Unit) {} // the sheet owns touches over it (none leak to the hero beneath)
                        .fillMaxWidth()
                        .heightIn(min = screenH)
                        .background(P.sheet, RoundedCornerShape(topStart = L.sheetRadius * (1 - 0.4f * k), topEnd = L.sheetRadius * (1 - 0.4f * k)))
                        .padding(start = L.page, end = L.page, top = 18.dp, bottom = navClearance),
                    verticalArrangement = Arrangement.spacedBy(L.block),
                ) {
                    banner?.invoke()
                    content()
                }
            }
        }
    }
}

/** Plain page on the base colour (reference home). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlainPage(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    banner: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptics = LocalHaptics.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    StretchRefresh(refreshing, onRefresh, Modifier.fillMaxSize().background(P.bg)) {
        Column(
            // Opaque page colour: when pulled, its top rounds off like the hero pages.
            Modifier.fillMaxSize().background(P.bg).verticalScroll(rememberScrollState())
                .padding(top = statusTop + 10.dp, start = L.page, end = L.page, bottom = navClearance),
            verticalArrangement = Arrangement.spacedBy(L.block),
        ) {
            banner?.invoke()
            content()
        }
    }
}

/** Old collapsing page, now a neutral [HeroPage] — kept for screens not yet restyled. */
@Composable
fun CollapsingPage(
    title: @Composable () -> Unit,
    extra: (@Composable () -> Unit)? = null,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    banner: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = HeroPage(P.card, refreshing, onRefresh, banner, hero = {
    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.weight(1f)) { title() }; MenuButton() }
    extra?.invoke()
}, content = content)

/* ───────────── Bottom nav: dark icon pill, sliding sky tile ───────────── */

private val TABS = listOf(
    Route.Today to Icons.Outlined.Home,
    Route.Schedule to Icons.Outlined.Schedule,
    Route.Subjects to Icons.Outlined.AutoStories,
    Route.Lms to Icons.Outlined.FolderOpen,
    Route.More to Icons.Outlined.Settings,
)

@Composable
fun BottomNav(route: Route, onChange: (Route) -> Unit, hazeState: HazeState, modifier: Modifier = Modifier, blurAlpha: () -> Float = { 1f }) {
    val haptics = LocalHaptics.current
    val pets = com.student.amizoneplus.ui.pets.LocalPetWorld.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val screenW = LocalConfiguration.current.screenWidthDp.dp
    // One gap all round: the same distance from both sides and from the bottom edge
    // (never less than the gesture bar needs).
    val gap = navBottom.coerceIn(16.dp, 24.dp)
    val pillW = minOf(screenW - gap * 2, 520.dp)
    val pad = 6.dp
    val slot = (pillW - pad * 2) / TABS.size
    val idx = TABS.indexOfFirst { it.first == route }.coerceAtLeast(0)
    val x by animateDpAsState(slot * idx, Motion.spring(), label = "navx")

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        // Progressive blur: strongest at the bottom, fading to nothing above the bar.
        Box(
            Modifier.fillMaxWidth().height(L.nav + navBottom + 64.dp)
                .graphicsLayer { alpha = blurAlpha() } // hidden while the quick menu blurs everything
                .hazeBlur(
                    HazeInput.Backdrop(hazeState),
                    HazeBlurStyle {
                        blurRadius(20.dp)
                        colorEffects(listOf(HazeColorEffect.tint(P.bg.copy(alpha = 0.35f))))
                        progressive(HazeProgressive.verticalGradient(startIntensity = 0f, endIntensity = 1f))
                    },
                )
                .background(Brush.verticalGradient(listOf(Color.Transparent, P.bg.copy(alpha = 0.5f)))),
        )
        Box(
            Modifier.padding(bottom = maxOf(gap, navBottom)).size(pillW, L.nav)
                .onGloballyPositioned { pets?.navTop = it.boundsInWindow().top }
                .background(P.nav.copy(alpha = 0.92f), RoundedCornerShape(L.nav / 2)).padding(pad),
        ) {
            // The tile slides between tabs.
            Box(Modifier.offset(x = x + 3.dp).size(slot - 6.dp, L.nav - pad * 2).background(P.sky, RoundedCornerShape((L.nav - pad * 2) / 2)))
            Row(Modifier.fillMaxSize()) {
                TABS.forEachIndexed { i, (r, icon) ->
                    val tint by animateColorAsState(if (i == idx) P.onHero else Color(0xFFDCDCE0), label = "navtint")
                    Box(
                        Modifier.width(slot).fillMaxSize()
                            .onGloballyPositioned { c -> pets?.navTabs?.set(r, c.boundsInWindow()) }
                            .press({ if (r != route) { haptics.select(); onChange(r) } }, Feel.None, 0.9f),
                        contentAlignment = Alignment.Center,
                    ) { NavIcon(icon, tint) }
                }
            }
        }
    }
}

@Composable
private fun NavIcon(icon: ImageVector, tint: Color) = Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp))

/** A soft progressive blur under the status bar: strongest at the very top, gone below it. */
@Composable
fun StatusBlur(hazeState: HazeState, blurAlpha: () -> Float = { 1f }) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        Modifier.fillMaxWidth().height((top + 28.dp) * 0.7f)
            .graphicsLayer { alpha = blurAlpha() }
            .hazeBlur(
                HazeInput.Backdrop(hazeState),
                HazeBlurStyle {
                    blurRadius(14.dp)
                    colorEffects(listOf(HazeColorEffect.tint(P.bg.copy(alpha = 0.18f))))
                    progressive(HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f))
                },
            ),
    )
}

/** Kept so the old root compiles until it's replaced. */
@Composable
fun PillNav(route: Route, onChange: (Route) -> Unit, hazeState: HazeState, modifier: Modifier = Modifier) = BottomNav(route, onChange, hazeState, modifier)
