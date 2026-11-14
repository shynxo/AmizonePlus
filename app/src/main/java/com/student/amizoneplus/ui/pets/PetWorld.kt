package com.student.amizoneplus.ui.pets

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.student.amizoneplus.ui.shell.Route

/*
 * The pets' world. One instance for the whole app: it knows which tab is showing,
 * every element a pet can stand on ("perches": cards, pills, buttons, big titles),
 * things a cat can hang from (the clock hands), and where the bottom-nav tabs are
 * (so a carried pet can be taken to another tab).
 */

/** The top edge of an on-screen element that a pet can stand on. */
class Perch(val id: Int) {
    var route: Route? = null
    var rect: Rect = Rect.Zero
    /** Dips a little when a pet lands on it. */
    val dip = mutableFloatStateOf(0f)
}

/** Points a cat can hang from (e.g. clock hands), recomputed every frame. */
class HangSource(val id: Int) {
    var route: Route? = null
    var rect: Rect = Rect.Zero
    var points: (Rect) -> List<Offset> = { emptyList() }
}

class PetWorld {
    var route by mutableStateOf(Route.Today)
    var switchTab: (Route) -> Unit = {}

    val perches = HashMap<Int, Perch>()
    val hangs = HashMap<Int, HangSource>()
    val navTabs = HashMap<Route, Rect>()
    var navTop = 0f

    var width = 0f
    var height = 0f
    var statusTop = 0f
    var density = 1f

    val pets = mutableStateListOf<Pet>()
    val fx = mutableStateListOf<Fx>()

    private var next = 0
    fun nextId() = ++next

    /** Where pets walk when not on anything: just above the nav pill. */
    fun floor() = (if (navTop > 0f) navTop else height) - 2f * density

    /** A perch is usable when it is on this tab, wide enough, and fully on screen. */
    fun usable(p: Perch, r: Route): Boolean {
        val b = p.rect
        return p.route == r && b.width > 30f * density &&
            b.top > statusTop + 34f * density && b.top < floor() - 24f * density &&
            b.right > 0f && b.left < width
    }

    fun perchesOn(r: Route) = perches.values.filter { usable(it, r) }

    fun hangPointsOn(r: Route): List<Offset> = hangs.values.filter { it.route == r && it.rect.width > 0f }
        .flatMap { h -> h.points(h.rect) }
        .filter { it.y > statusTop + 40f * density && it.y < floor() - 60f * density }
}

val LocalPetWorld = staticCompositionLocalOf<PetWorld?> { null }
val LocalPetRoute = staticCompositionLocalOf<Route?> { null }

/** Lets pets stand on this element (its top edge). It dips a touch when one lands. */
fun Modifier.perch(): Modifier = this.then(Modifier.composed {
    val world = LocalPetWorld.current
    val route = LocalPetRoute.current
    if (world == null || route == null) Modifier else {
        val p = remember { Perch(world.nextId()) }
        DisposableEffect(p) {
            world.perches[p.id] = p
            onDispose { world.perches.remove(p.id) }
        }
        Modifier
            .onGloballyPositioned { p.route = route; p.rect = it.boundsInWindow() }
            .graphicsLayer { translationY = p.dip.floatValue }
    }
})

/** Things a cat can hang from inside this element, e.g. the clock's hands. */
fun Modifier.hangPoints(points: (Rect) -> List<Offset>): Modifier = this.then(Modifier.composed {
    val world = LocalPetWorld.current
    val route = LocalPetRoute.current
    if (world == null || route == null) Modifier else {
        val h = remember { HangSource(world.nextId()) }
        h.points = points
        DisposableEffect(h) {
            world.hangs[h.id] = h
            onDispose { world.hangs.remove(h.id) }
        }
        Modifier.onGloballyPositioned { h.route = route; h.rect = it.boundsInWindow() }
    }
})

/** Little effects: hearts, z's, dirt, speech bubbles. */
class Fx(
    val kind: Kind,
    var x: Float,
    var y: Float,
    val text: String = "",
    var vx: Float = 0f,
    var vy: Float = 0f,
    val life: Float = 1f,
    val route: Route,
) {
    enum class Kind { Heart, Zed, Dirt, Bubble, Spark }
    var age = 0f
    var follow: Pet? = null
}
