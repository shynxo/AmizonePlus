package com.student.amizoneplus.ui.pets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.amizoneplus.ui.shell.Route
import com.student.amizoneplus.ui.theme.Haptics
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.Sans
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * The app's pets: one cat and/or one dog that live across the whole app. Each pet
 * is on exactly one tab at a time; it walks, runs, sits, naps and plays there, jumps
 * onto cards, buttons and titles, hangs from the timetable clock's hands, and now
 * and then wanders off to another tab (or comes to visit yours). You can pet it
 * (tap), pick it up (drag), throw it, drop it onto anything, or carry it to another
 * tab by hovering over that tab in the nav.
 */

enum class Species { Cat, Dog }

enum class Act {
    Idle, Walk, Run, Sit, Groom, Loaf, Sleep, Knead, Stretch, Yawn, Meow, Crouch, Air, Hang, Held,
    Bark, Scratch, Lie, Belly, Dig, Sniff, Bow, Zoomies, Spin, Wag, Happy, Startle, Away,
}

private fun rnd(a: Float, b: Float) = a + Random.nextFloat() * (b - a)
private fun chance(p: Float) = Random.nextFloat() < p

class Pet(val species: Species, startRoute: Route) {
    var route by mutableStateOf(startRoute)
    // Drawn state (read only during layout / draw, so moving never recomposes).
    var x by mutableFloatStateOf(0f)          // anchor x (feet, or grip when top-anchored)
    var y by mutableFloatStateOf(0f)          // anchor y
    var facing by mutableFloatStateOf(1f)
    var frame by mutableStateOf("idle0")
    var topAnchored by mutableStateOf(false)  // hanging / carried: anchor is the grip at the top
    var tilt by mutableFloatStateOf(0f)
    var squash by mutableFloatStateOf(0f)
    var boxLeft = 0f
    var boxTop = 0f

    // Simulation
    var act = Act.Idle
    var t = 0f
    var dur = 1.5f
    var anim = 0f
    var vx = 0f
    var vy = 0f
    var perch: Perch? = null
    var relX = 0f
    var targetX = 0f
    var free = false               // may walk past the screen edges (arriving / leaving)
    var leaving = false
    var then: (() -> Unit)? = null // what to do after a walk / crouch
    var aim: (() -> Offset?)? = null
    var aimGrab = false
    var hangAt: (() -> Offset?)? = null
    var swingV = 0f
    var blinkT = 3f
    var held = false
    var lastHeldX = 0f
    var hoverRoute: Route? = null
    var hoverT = 0f
    var wander = rnd(90f, 180f)
    var beats = 0
    var beatT = 0f
    var placed = false

    val pal get() = if (species == Species.Cat) PetSprites.CAT_PALETTE else PetSprites.DOG_PALETTE
    val frames get() = if (species == Species.Cat) PetSprites.CAT else PetSprites.DOG
}

/** Decoded sprite pixels, cached per frame. */
private class Art(val xs: IntArray, val ys: IntArray, val colors: IntArray)

private val artCache = HashMap<String, Art>()

private fun art(pet: Pet, name: String): Art {
    val key = pet.species.name + ":" + name
    return artCache.getOrPut(key) {
        val rows = pet.frames[name] ?: pet.frames.getValue("idle0")
        val xs = ArrayList<Int>(); val ys = ArrayList<Int>(); val cs = ArrayList<Int>()
        rows.forEachIndexed { y, line ->
            line.forEachIndexed { x, c -> if (c != '.') { xs += x; ys += y; cs += pet.pal[c] ?: 0xFFFF00FF.toInt() } }
        }
        Art(xs.toIntArray(), ys.toIntArray(), cs.toIntArray())
    }
}

/* ═══════════════════════════════ Engine ═══════════════════════════════ */

class PetEngine(private val world: PetWorld, private val haptics: Haptics) {
    private val d get() = world.density
    private val px get() = 1.4f * d                        // one sprite pixel
    val spriteW get() = PetSprites.W * px
    val spriteH get() = PetSprites.H * px
    private val g get() = 2600f * d
    private var visitT = 8f
    private var lastRoute: Route? = null
    private val dipping = HashSet<Perch>()

    /* ── public: input ── */

    fun tap(p: Pet) {
        if (p.held) return
        haptics.select()
        when {
            p.act == Act.Sleep -> { bubble(p, "!"); start(p, Act.Startle, 0.5f) }
            p.species == Species.Cat -> {
                hearts(p, 3); bubble(p, listOf("prrr", "mrrp?", "meow!", "♥").random())
                if (grounded(p)) start(p, Act.Happy, 1.4f)
            }
            else -> {
                hearts(p, 3); bubble(p, listOf("woof!", "arf!", "♥").random())
                if (grounded(p)) { leap(p, 0f, -380f * d); p.then = { start(p, Act.Wag, 1.6f) } }
            }
        }
    }

    fun grab(p: Pet) {
        haptics.press()
        p.held = true; p.topAnchored = true; p.perch = null
        p.act = Act.Held; p.t = 0f; p.anim = 0f; p.then = null; p.aim = null; p.hangAt = null
        p.lastHeldX = p.x
        if (chance(0.5f)) bubble(p, if (p.species == Species.Cat) "mew?!" else "!")
    }

    fun carry(p: Pet, finger: Offset) {
        p.x = finger.x
        p.y = finger.y + 4f * d
    }

    fun release(p: Pet, vx: Float, vy: Float) {
        p.held = false
        p.topAnchored = false
        p.y += spriteH * 0.92f // grip → feet
        p.tilt = 0f
        val lim = 2600f * d
        startAir(p, vx.coerceIn(-lim, lim), vy.coerceIn(-lim, lim))
    }

    /* ── per frame ── */

    fun step(dt: Float) {
        if (world.width <= 0f) return
        val route = world.route
        if (lastRoute != route) {
            lastRoute = route
            if (world.pets.none { it.route == route }) visitT = rnd(3f, 8f)
        }
        // nothing on this tab? someone may come to visit
        if (world.pets.none { it.route == route }) {
            visitT -= dt
            if (visitT <= 0f) {
                world.pets.filter { !it.held }.randomOrNull()?.let { arrive(it) }
                visitT = rnd(12f, 25f)
            }
        }
        for (p in world.pets) {
            if (!p.placed) place(p)
            if (p.held) { stepHeld(p, dt); continue }
            if (p.route != route) continue
            stepPet(p, dt)
        }
        // perch dips settle back
        val it = dipping.iterator()
        while (it.hasNext()) {
            val pr = it.next()
            pr.dip.floatValue *= exp(-dt * 12f)
            if (abs(pr.dip.floatValue) < 0.2f) { pr.dip.floatValue = 0f; it.remove() }
        }
        stepFx(dt)
    }

    private fun place(p: Pet) {
        p.placed = true
        p.x = world.width * (if (p.species == Species.Cat) rnd(0.2f, 0.4f) else rnd(0.6f, 0.8f))
        p.y = world.floor()
        p.facing = if (chance(0.5f)) 1f else -1f
        start(p, Act.Idle, rnd(1f, 2f))
    }

    private fun arrive(p: Pet) {
        p.route = world.route
        val left = chance(0.5f)
        p.x = if (left) -spriteW else world.width + spriteW
        p.y = world.floor(); p.perch = null; p.topAnchored = false; p.leaving = false
        p.facing = if (left) 1f else -1f
        walkTo(p, world.width * rnd(0.25f, 0.75f), run = chance(0.4f), free = true)
        if (chance(0.6f)) p.then = { bubble(p, if (p.species == Species.Cat) "hi!" else "woof!"); start(p, Act.Idle, 1.2f) }
    }

    /** A pet that moved to this tab earlier: it's simply there, lounging somewhere. */
    private fun settleIn(p: Pet) {
        p.x = world.width * rnd(0.15f, 0.85f)
        p.y = world.floor(); p.perch = null; p.topAnchored = false; p.free = false; p.leaving = false
        p.facing = if (chance(0.5f)) 1f else -1f
        val r = Random.nextFloat()
        if (p.species == Species.Cat) when {
            r < 0.4f -> start(p, Act.Sleep, rnd(4f, 9f))
            r < 0.7f -> start(p, Act.Loaf, rnd(2f, 4f))
            else -> start(p, Act.Sit, rnd(2f, 4f))
        } else when {
            r < 0.4f -> start(p, Act.Sleep, rnd(4f, 9f))
            r < 0.7f -> start(p, Act.Lie, rnd(2f, 4f))
            else -> start(p, Act.Wag, rnd(1.5f, 3f))
        }
    }

    private fun stepHeld(p: Pet, dt: Float) {
        p.route = world.route
        p.anim += dt
        p.frame = "held" + ((p.anim / 0.18f).toInt() % 2)
        // swing with the hand's motion
        val v = (p.x - p.lastHeldX) / max(dt, 1e-3f)
        p.lastHeldX = p.x
        p.tilt += ((-v / (40f * d)).coerceIn(-28f, 28f) - p.tilt) * min(1f, dt * 8f)
        if (abs(v) > 10f * d) p.facing = if (v > 0) 1f else -1f
        // carry to another tab: hover over it in the nav
        val finger = Offset(p.x, p.y - 4f * d)
        val over = world.navTabs.entries.firstOrNull { it.value.inflate(6f * d).contains(finger) }?.key
        if (over != null && over != world.route) {
            if (p.hoverRoute != over) { p.hoverRoute = over; p.hoverT = 0f }
            p.hoverT += dt
            if (p.hoverT > 0.45f) { haptics.select(); world.switchTab(over); p.route = over; p.hoverT = -10f }
        } else { p.hoverRoute = null; p.hoverT = 0f }
    }

    private fun grounded(p: Pet) = p.act != Act.Air && p.act != Act.Hang && !p.held && !p.topAnchored

    private fun stepPet(p: Pet, dt: Float) {
        if (p.act == Act.Away) { settleIn(p); return } // you came to the tab it moved to
        p.t += dt; p.anim += dt
        p.wander -= dt
        p.squash = max(0f, p.squash - dt * 5f)

        // Stay glued to whatever we stand on.
        if (p.act != Act.Air && p.act != Act.Hang) {
            val pr = p.perch
            if (pr != null) {
                if (!world.usable(pr, p.route)) { p.perch = null; startAir(p, 0f, 0f) }
                else { p.x = pr.rect.left + p.relX; p.y = pr.rect.top + pr.dip.floatValue }
            } else p.y = world.floor()
        }

        when (p.act) {
            Act.Air -> stepAir(p, dt)
            Act.Hang -> stepHang(p, dt)
            Act.Walk, Act.Run, Act.Sniff, Act.Zoomies -> stepWalk(p, dt)
            Act.Spin -> { if ((p.anim / 0.16f).toInt() % 2 == 0) p.facing = 1f else p.facing = -1f; timed(p) }
            Act.Dig -> {
                p.beatT -= dt
                if (p.beatT <= 0f) { p.beatT = 0.07f; dirt(p) }
                timed(p)
            }
            Act.Bark -> {
                p.beatT -= dt
                if (p.beatT <= 0f && p.beats > 0) { p.beats--; p.beatT = 0.7f; bubble(p, listOf("WOOF!", "woof", "ARF!").random()); haptics.tick() }
                timed(p)
            }
            Act.Sleep -> {
                p.beatT -= dt
                if (p.beatT <= 0f) { p.beatT = 1.1f; zed(p) }
                timed(p)
            }
            Act.Knead -> {
                p.beatT -= dt
                if (p.beatT <= 0f) { p.beatT = 1.6f; bubble(p, "prrr") }
                timed(p)
            }
            Act.Crouch -> if (p.t >= p.dur) { val n = p.then; p.then = null; if (n != null) n() else think(p) }
            else -> timed(p)
        }
        if (p.act != Act.Hang) p.tilt *= exp(-dt * 10f)
        blink(p, dt)
        p.frame = frameFor(p)
    }

    private fun timed(p: Pet) { if (p.t >= p.dur) { val n = p.then; p.then = null; if (n != null) n() else think(p) } }

    private fun stepWalk(p: Pet, dt: Float) {
        val speed = d * when (p.act) {
            Act.Run, Act.Zoomies -> if (p.species == Species.Cat) 130f else 150f
            Act.Sniff -> 18f
            else -> if (p.species == Species.Cat) 36f else 42f
        }
        val dir = when { p.targetX > p.x + 1f -> 1f; p.targetX < p.x - 1f -> -1f; else -> 0f }
        if (dir != 0f) p.facing = dir
        var nx = p.x + dir * speed * dt
        if ((dir > 0 && nx >= p.targetX) || (dir < 0 && nx <= p.targetX)) nx = p.targetX
        val (lo, hi) = bounds(p)
        if (!p.free) nx = nx.coerceIn(lo, hi)
        p.x = nx
        p.perch?.let { p.relX = nx - it.rect.left }
        // left the screen: off to another tab
        if (p.leaving && (p.x < -spriteW || p.x > world.width + spriteW)) {
            val others = Route.entries.filter { it != world.route }
            p.route = others.random()
            p.leaving = false; p.free = false
            start(p, Act.Away, 1f)
            return
        }
        val arrived = abs(p.x - p.targetX) < 1.5f || dir == 0f || (p.t > 9f && !p.leaving) ||
            (!p.free && (p.x <= lo + 0.5f && dir < 0 || p.x >= hi - 0.5f && dir > 0))
        if (arrived) {
            p.free = false
            if (p.act == Act.Zoomies && p.beats > 0) {
                p.beats--
                walkTo(p, if (p.x < world.width / 2) world.width * rnd(0.75f, 0.92f) else world.width * rnd(0.08f, 0.25f), run = true)
                p.act = Act.Zoomies
                return
            }
            val n = p.then; p.then = null
            if (n != null) n() else think(p)
        }
    }

    private fun stepAir(p: Pet, dt: Float) {
        // aimed jump onto a moving target (clock hand / edge)
        p.aim?.let { aimFn ->
            val tgt = aimFn()
            if (tgt == null) { p.aim = null } else if (p.t >= p.dur) {
                p.aim = null
                if (p.aimGrab) { hang(p, p.hangAt ?: { tgt }); return }
            }
        }
        val ny = p.y + p.vy * dt + 0.5f * g * dt * dt
        p.vy += g * dt
        var nx = p.x + p.vx * dt
        if (nx < 6f * d) { nx = 6f * d; p.vx = abs(p.vx) * 0.35f }
        if (nx > world.width - 6f * d) { nx = world.width - 6f * d; p.vx = -abs(p.vx) * 0.35f }
        if (p.vx != 0f) p.facing = if (p.vx > 0) 1f else -1f
        p.tilt = (p.vy / (90f * d)).coerceIn(-12f, 12f) * p.facing
        if (p.vy > 0f) {
            val land = world.perchesOn(p.route)
                .filter { it.rect.top + 0.5f >= p.y && it.rect.top <= ny && nx in (it.rect.left + 4f * d)..(it.rect.right - 4f * d) }
                .minByOrNull { it.rect.top }
            if (land != null) { landOn(p, land, nx); return }
            if (ny >= world.floor()) { landOn(p, null, nx); return }
        }
        p.x = nx; p.y = ny
    }

    private fun landOn(p: Pet, pr: Perch?, nx: Float) {
        val hard = p.vy > 1400f * d
        p.x = nx
        p.perch = pr
        if (pr != null) {
            p.relX = nx - pr.rect.left; p.y = pr.rect.top
            pr.dip.floatValue = (if (hard) 6f else 3.5f) * d; dipping += pr
        } else p.y = world.floor()
        p.vx = 0f; p.vy = 0f; p.tilt = 0f
        p.squash = if (hard) 1f else 0.6f
        haptics.tick()
        val n = p.then; p.then = null
        if (n != null) n() else if (hard) {
            bubble(p, if (p.species == Species.Cat) listOf("!", "hmph", "mrow!").random() else listOf("woof!", "!", "again!").random())
            start(p, Act.Idle, 0.9f)
        } else start(p, Act.Idle, rnd(0.4f, 1.2f))
    }

    private fun stepHang(p: Pet, dt: Float) {
        val pt = p.hangAt?.invoke()
        if (pt == null) { drop(p); return }
        p.x = pt.x; p.y = pt.y
        // pendulum
        p.swingV += (-34f * p.tilt - 1.4f * p.swingV) * dt
        p.tilt += p.swingV * dt
        if (p.t > 1.2f && chance(dt * 0.6f)) p.swingV += rnd(-50f, 50f) // a little kick now and then
        if (p.t >= p.dur) { val n = p.then; p.then = null; if (n != null) n() else drop(p) }
    }

    private fun drop(p: Pet) {
        p.topAnchored = false
        p.hangAt = null
        p.y += spriteH * 0.92f
        startAir(p, 0f, 0f)
    }

    private fun blink(p: Pet, dt: Float) {
        p.blinkT -= dt
        if (p.blinkT < -0.13f) p.blinkT = rnd(2f, 5f)
    }

    /* ── starting things ── */

    private fun start(p: Pet, act: Act, dur: Float) {
        p.act = act; p.t = 0f; p.dur = dur; p.anim = 0f; p.beatT = 0f
        if (act != Act.Hang) p.topAnchored = false
    }

    private fun startAir(p: Pet, vx: Float, vy: Float) {
        p.perch = null; p.topAnchored = false
        p.vx = vx; p.vy = vy
        start(p, Act.Air, 99f)
    }

    private fun leap(p: Pet, vx: Float, vy: Float) {
        p.perch = null
        p.vx = vx; p.vy = vy
        start(p, Act.Air, 99f)
    }

    private fun bounds(p: Pet): Pair<Float, Float> {
        val pr = p.perch
        return if (pr != null) (pr.rect.left + 8f * d) to (pr.rect.right - 8f * d)
        else (12f * d) to (world.width - 12f * d)
    }

    private fun walkTo(p: Pet, x: Float, run: Boolean = false, free: Boolean = false, sniff: Boolean = false) {
        start(p, when { sniff -> Act.Sniff; run -> Act.Run; else -> Act.Walk }, 99f)
        p.free = free
        p.targetX = if (free) x else bounds(p).let { (lo, hi) -> x.coerceIn(lo, hi) }
    }

    /** Ballistic jump that lands the feet (or the paws, for a grab) on a point at time T. */
    private fun jumpAt(p: Pet, target: () -> Offset?, grab: Boolean) {
        val tgt = target() ?: return think(p)
        val dy = tgt.y - p.y
        val dx = tgt.x - p.x
        val minT = sqrt(2f * max(0f, -dy) / g) * 1.18f
        val tt = max(min(0.42f + abs(dx) / (2400f * d), 0.75f), minT)
        p.facing = if (dx >= 0) 1f else -1f
        leap(p, dx / tt, (dy - 0.5f * g * tt * tt) / tt)
        p.dur = tt
        p.aim = target
        p.aimGrab = grab
        haptics.tick()
    }

    private fun hang(p: Pet, at: () -> Offset?) {
        val pt = at() ?: return drop(p)
        p.hangAt = at
        p.x = pt.x; p.y = pt.y
        p.topAnchored = true
        p.swingV = (p.vx / d) * 0.12f
        p.vx = 0f; p.vy = 0f
        start(p, Act.Hang, rnd(3f, 7f))
        p.topAnchored = true
        haptics.tick()
        if (chance(0.5f)) bubble(p, listOf("wheee", "hehe", "mine!", "nya~").random())
    }

    /* ── deciding what to do next ── */

    private fun think(p: Pet) {
        p.free = false
        val onPerch = p.perch != null
        val r = p.route
        // time to see the rest of the app?
        if (p.wander <= 0f && r == world.route && world.pets.count { it.route == r } >= 1) {
            p.wander = rnd(90f, 200f)
            if (onPerch) return hopDown(p) { leave(p) }
            return leave(p)
        }
        if (p.species == Species.Cat) thinkCat(p, onPerch) else thinkDog(p, onPerch)
    }

    private fun leave(p: Pet) {
        val toLeft = p.x < world.width / 2
        walkTo(p, if (toLeft) -spriteW * 2 else world.width + spriteW * 2, run = chance(0.5f), free = true)
        p.leaving = true
        if (chance(0.5f)) bubble(p, listOf("brb", "bye!", "exploring~").random())
    }

    private fun hopDown(p: Pet, after: (() -> Unit)? = null) {
        val pr = p.perch ?: return think(p)
        val toRight = p.x > pr.rect.center.x
        val edge = if (toRight) pr.rect.right - 9f * d else pr.rect.left + 9f * d
        walkTo(p, edge)
        p.then = {
            p.facing = if (toRight) 1f else -1f
            leap(p, p.facing * rnd(70f, 130f) * d, -rnd(260f, 380f) * d)
            p.then = after
        }
    }

    private fun perchTargets(p: Pet, maxRise: Float): List<Perch> =
        world.perchesOn(p.route).filter {
            it != p.perch && it.rect.top < p.y - 20f * d && p.y - it.rect.top < maxRise &&
                abs(it.rect.center.x - p.x) < world.width * 0.85f
        }

    private fun jumpUp(p: Pet, maxRise: Float): Boolean {
        val options = perchTargets(p, maxRise)
        val pr = options.randomOrNull() ?: return false
        val tx = rnd(pr.rect.left + 14f * d, pr.rect.right - 14f * d)
        val edgeGrab = p.species == Species.Cat && chance(0.3f)
        start(p, Act.Crouch, rnd(0.35f, 0.7f))
        p.facing = if (tx >= p.x) 1f else -1f
        p.then = {
            if (edgeGrab) {
                val leftEdge = abs(p.x - pr.rect.left) < abs(p.x - pr.rect.right)
                val gx = { if (leftEdge) pr.rect.left + 12f * d else pr.rect.right - 12f * d }
                val grip = { if (world.usable(pr, p.route)) Offset(gx(), pr.rect.top + 2f * d) else null }
                p.hangAt = grip
                jumpAt(p, { grip()?.let { Offset(it.x, it.y + spriteH * 0.92f) } }, grab = true)
                // after hanging a moment, scramble up onto it
                p.then = { climb(p, pr) }
            } else {
                jumpAt(p, { if (world.usable(pr, p.route)) Offset(pr.rect.left + (tx - pr.rect.left), pr.rect.top) else null }, grab = false)
            }
        }
        return true
    }

    private fun climb(p: Pet, pr: Perch) {
        if (p.act != Act.Hang) return start(p, Act.Idle, 0.6f)
        p.topAnchored = false; p.hangAt = null; p.tilt = 0f
        if (!world.usable(pr, p.route)) return drop(p)
        p.perch = pr
        p.relX = (p.x - pr.rect.left).coerceIn(10f * d, pr.rect.width - 10f * d)
        p.y = pr.rect.top; p.squash = 0.7f
        pr.dip.floatValue = 3f * d; dipping += pr
        start(p, Act.Idle, 0.8f)
    }

    private fun hangFromClock(p: Pet): Boolean {
        val pts = world.hangPointsOn(p.route)
        if (pts.isEmpty()) return false
        val i = Random.nextInt(pts.size)
        val at = { world.hangPointsOn(p.route).getOrNull(i) }
        start(p, Act.Crouch, 0.5f)
        p.facing = if ((pts[i].x) >= p.x) 1f else -1f
        p.then = {
            p.hangAt = at
            jumpAt(p, { at()?.let { Offset(it.x, it.y + spriteH * 0.92f) } }, grab = true)
        }
        return true
    }

    private fun thinkCat(p: Pet, onPerch: Boolean) {
        val dog = world.pets.firstOrNull { it.species == Species.Dog && it.route == p.route && it.act == Act.Run && abs(it.x - p.x) < 140f * d }
        if (dog != null && !onPerch) {
            bubble(p, "!"); start(p, Act.Startle, 0.35f)
            p.then = { if (!jumpUp(p, world.height * 0.6f)) walkTo(p, if (dog.x < p.x) world.width - 20f * d else 20f * d, run = true) }
            return
        }
        val roll = Random.nextFloat() * 100f
        when {
            roll < 24 && hangFromClock(p) -> Unit
            roll < 34 && jumpUp(p, world.height * 0.55f) -> Unit
            roll < 40 && onPerch -> hopDown(p)
            roll < 50 -> walkTo(p, p.x + rnd(-1f, 1f) * world.width * 0.5f)
            roll < 55 -> walkTo(p, if (p.x < world.width / 2) world.width * 0.85f else world.width * 0.15f, run = true)
            roll < 64 -> {
                start(p, Act.Sit, rnd(3f, 6f))
                when {
                    chance(0.3f) -> p.then = { start(p, Act.Meow, 0.9f); bubble(p, listOf("meow", "mew", "nya", "mrrp").random()) }
                    chance(0.3f) -> p.then = { start(p, Act.Yawn, 1.4f) }
                }
            }
            roll < 71 -> start(p, Act.Groom, rnd(2f, 3.5f))
            roll < 79 -> {
                start(p, Act.Loaf, rnd(2.5f, 4f))
                if (chance(0.55f)) p.then = { start(p, Act.Sleep, rnd(7f, 14f)); p.then = { start(p, Act.Stretch, 1.2f); p.then = { start(p, Act.Yawn, 1.2f) } } }
            }
            roll < 84 -> start(p, Act.Knead, rnd(2f, 3f))
            roll < 88 -> start(p, Act.Stretch, 1.2f)
            roll < 94 -> { // pounce on nothing in particular
                start(p, Act.Crouch, rnd(0.6f, 1.1f))
                p.then = { leap(p, p.facing * rnd(150f, 240f) * d, -rnd(380f, 520f) * d) }
            }
            else -> start(p, Act.Idle, rnd(1.5f, 3.5f))
        }
    }

    private fun thinkDog(p: Pet, onPerch: Boolean) {
        val cat = world.pets.firstOrNull { it.species == Species.Cat && it.route == p.route && !it.held }
        val roll = Random.nextFloat() * 100f
        when {
            cat != null && roll < 9 && !onPerch -> { // chase the cat!
                bubble(p, "!")
                walkTo(p, cat.x, run = true)
                p.then = { start(p, Act.Bark, 2f); p.beats = 2; p.then = { start(p, Act.Wag, 1.5f) } }
            }
            roll < 15 && jumpUp(p, world.height * 0.28f) -> Unit
            roll < 26 && onPerch -> hopDown(p)
            roll < 38 -> walkTo(p, p.x + rnd(-1f, 1f) * world.width * 0.5f)
            roll < 45 && !onPerch -> walkTo(p, p.x + rnd(-1f, 1f) * world.width * 0.3f, sniff = true).also { p.then = { if (chance(0.5f)) start(p, Act.Dig, rnd(1.5f, 2.5f)) else think(p) } }
            roll < 50 -> { start(p, Act.Bow, 0.9f); p.then = { start(p, Act.Zoomies, 99f); p.beats = Random.nextInt(2, 5); walkTo(p, if (p.x < world.width / 2) world.width * 0.9f else world.width * 0.1f, run = true); p.act = Act.Zoomies } }
            roll < 58 -> start(p, Act.Sit, rnd(3f, 6f))
            roll < 63 -> { start(p, Act.Bark, 1.8f); p.beats = Random.nextInt(1, 3) }
            roll < 68 -> start(p, Act.Scratch, rnd(1.2f, 2.2f))
            roll < 76 -> {
                start(p, Act.Lie, rnd(2.5f, 4f))
                if (chance(0.6f)) p.then = { start(p, Act.Sleep, rnd(7f, 13f)); p.then = { start(p, Act.Wag, 1.2f) } }
            }
            roll < 81 -> { start(p, Act.Belly, rnd(2f, 3.2f)); hearts(p, 2) }
            roll < 86 && !onPerch -> start(p, Act.Dig, rnd(1.5f, 2.5f))
            roll < 90 -> start(p, Act.Spin, rnd(1.2f, 2f))
            roll < 95 -> start(p, Act.Wag, rnd(1.5f, 3f))
            else -> start(p, Act.Idle, rnd(1.5f, 3f))
        }
    }

    /* ── which picture to show ── */

    private fun frameFor(p: Pet): String {
        val a = p.anim
        val blink = p.blinkT < 0f
        fun cyc(names: List<String>, step: Float) = names[(a / step).toInt() % names.size]
        return if (p.species == Species.Cat) when (p.act) {
            Act.Idle -> if (blink) "idle_blink" else if (a % 6f > 4.8f) "idle_up" else cyc(listOf("idle0", "idle1", "idle2", "idle1"), 0.38f)
            Act.Walk, Act.Sniff -> cyc(listOf("walk0", "walk1", "walk2", "walk3"), 0.13f)
            Act.Run, Act.Zoomies -> cyc(listOf("run0", "run1"), 0.085f)
            Act.Sit, Act.Away, Act.Bark, Act.Scratch, Act.Wag, Act.Bow -> if (blink) "sit_blink" else if (a % 5f > 4f) "sit_up" else cyc(listOf("sit0", "sit1"), 1.2f)
            Act.Happy -> cyc(listOf("sit_happy", "idle_happy"), 0.7f)
            Act.Meow -> "meow"
            Act.Yawn -> if (a < 0.9f) "yawn" else "sit_blink"
            Act.Groom -> cyc(listOf("groom0", "groom1"), 0.28f)
            Act.Loaf, Act.Lie -> if (blink) "loaf0" else "loaf_awake"
            Act.Sleep, Act.Belly -> cyc(listOf("loaf0", "loaf1"), 1.0f)
            Act.Knead -> cyc(listOf("knead0", "knead1"), 0.22f)
            Act.Stretch -> "stretch"
            Act.Crouch, Act.Dig -> cyc(listOf("crouch0", "crouch1"), 0.1f)
            Act.Startle -> "crouch0"
            Act.Spin -> cyc(listOf("run0", "run1"), 0.085f)
            Act.Air -> if (p.vy < 0f) "jump" else "fall"
            Act.Hang -> if (a % 4f > 3.2f) "hang_happy" else cyc(listOf("hang0", "hang1"), 0.45f)
            Act.Held -> cyc(listOf("held0", "held1"), 0.18f)
        } else when (p.act) {
            Act.Idle -> if (blink) "idle_blink" else if (a % 5f > 3.6f) "idle_tongue" else cyc(listOf("idle0", "idle1"), 0.35f)
            Act.Wag, Act.Happy -> cyc(listOf("wag0", "wag1"), 0.1f)
            Act.Walk -> cyc(listOf("walk0", "walk1", "walk2", "walk3"), 0.13f)
            Act.Run, Act.Zoomies, Act.Spin -> cyc(listOf("run0", "run1"), 0.08f)
            Act.Sniff -> cyc(listOf("sniff0", "sniff1"), 0.25f)
            Act.Sit, Act.Away, Act.Meow, Act.Yawn, Act.Groom, Act.Knead, Act.Stretch ->
                if (blink) "sit_blink" else if (a % 4f > 2.6f) "sit_tongue" else cyc(listOf("sit0", "sit1"), 0.5f)
            Act.Bark -> if (p.beatT > 0.45f) "bark0" else "sit0"
            Act.Scratch -> cyc(listOf("scratch0", "scratch1"), 0.08f)
            Act.Lie, Act.Loaf -> if (blink) "lie0" else "lie_awake"
            Act.Sleep -> cyc(listOf("lie0", "lie1"), 1.0f)
            Act.Belly -> cyc(listOf("belly0", "belly1"), 0.25f)
            Act.Dig -> cyc(listOf("dig0", "dig1"), 0.12f)
            Act.Bow -> cyc(listOf("bow0", "bow1"), 0.14f)
            Act.Crouch, Act.Startle -> cyc(listOf("bow0", "bow1"), 0.1f)
            Act.Air, Act.Hang -> "jump"
            Act.Held -> cyc(listOf("held0", "held1"), 0.2f)
        }
    }

    /* ── effects ── */

    private fun bubble(p: Pet, text: String) {
        world.fx.removeAll { it.kind == Fx.Kind.Bubble && it.follow === p }
        world.fx += Fx(Fx.Kind.Bubble, p.x, p.y, text, life = 1.6f, route = p.route).also { it.follow = p }
    }

    private fun hearts(p: Pet, n: Int) = repeat(n) {
        world.fx += Fx(Fx.Kind.Heart, p.x + rnd(-10f, 10f) * d, p.y - spriteH * 0.8f, vx = rnd(-20f, 20f) * d, vy = -rnd(40f, 70f) * d, life = 1.2f + it * 0.15f, route = p.route)
    }

    private fun zed(p: Pet) {
        world.fx += Fx(Fx.Kind.Zed, p.x + p.facing * 8f * d, p.y - spriteH * 0.6f, vx = p.facing * 10f * d, vy = -22f * d, life = 2f, route = p.route)
    }

    private fun dirt(p: Pet) {
        world.fx += Fx(Fx.Kind.Dirt, p.x + p.facing * 10f * d, p.y - 2f * d, vx = -p.facing * rnd(40f, 120f) * d, vy = -rnd(120f, 240f) * d, life = 0.7f, route = p.route)
    }

    private fun stepFx(dt: Float) {
        val it = world.fx.iterator()
        val dead = ArrayList<Fx>()
        while (it.hasNext()) {
            val f = it.next()
            f.age += dt
            if (f.age >= f.life) { dead += f; continue }
            when (f.kind) {
                Fx.Kind.Dirt -> { f.vy += g * 0.5f * dt; f.x += f.vx * dt; f.y += f.vy * dt }
                Fx.Kind.Bubble -> f.follow?.let { f.x = it.x; f.y = if (it.topAnchored) it.y else it.y - spriteH }
                else -> { f.x += f.vx * dt; f.y += f.vy * dt }
            }
        }
        if (dead.isNotEmpty()) world.fx.removeAll(dead)
    }
}

/* ═══════════════════════════════ UI layer ═══════════════════════════════ */

/**
 * Draws the pets (and their hearts / z's / bubbles) over the app and handles touch:
 * tap to pet, drag to carry, release to drop or throw. [choice] is "cat", "dog",
 * "both" or "none". [active] pauses everything while a sheet / web page covers the app.
 */
@Composable
fun PetLayer(world: PetWorld, choice: String, active: Boolean) {
    val haptics = LocalHaptics.current
    val density = LocalDensity.current
    val statusTop = with(density) { WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx() }
    val engine = remember { PetEngine(world, haptics) }
    var tick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(choice) {
        val want = when (choice) { "cat" -> listOf(Species.Cat); "dog" -> listOf(Species.Dog); "both" -> listOf(Species.Cat, Species.Dog); else -> emptyList() }
        world.pets.removeAll { it.species !in want }
        want.filter { s -> world.pets.none { it.species == s } }.forEach { world.pets += Pet(it, world.route) }
        world.fx.clear()
    }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceIn(0f, 0.05f)
                last = now
                engine.step(dt)
                tick = now
            }
        }
    }
    if (!active || world.pets.isEmpty()) return

    BoxWithConstraints(Modifier.fillMaxSize()) {
        world.width = constraints.maxWidth.toFloat()
        world.height = constraints.maxHeight.toFloat()
        world.statusTop = statusTop
        world.density = density.density
        val measurer = rememberTextMeasurer()

        val route = world.route
        world.pets.forEach { pet -> key(pet) { if (pet.route == route || pet.held) PetSprite(pet, engine) } }

        // hearts, z's, dirt, bubbles (no touch handling, so taps pass through)
        Canvas(Modifier.fillMaxSize()) {
            if (tick < 0L) return@Canvas
            val px = 1.4f * density.density
            world.fx.forEach { f ->
                if (f.route != route) return@forEach
                val k = 1f - f.age / f.life
                when (f.kind) {
                    Fx.Kind.Heart -> pixelArt(HEART, f.x, f.y, px, Color(0xFFF7A8B8).copy(alpha = k))
                    Fx.Kind.Zed -> pixelArt(ZED, f.x, f.y, px, Color(0xFFDCD6EA).copy(alpha = k))
                    Fx.Kind.Dirt -> drawRect(Color(0xFFA8693F).copy(alpha = k), Offset(f.x, f.y), Size(px * 1.4f, px * 1.4f))
                    Fx.Kind.Spark -> drawRect(Color.White.copy(alpha = k), Offset(f.x, f.y), Size(px, px))
                    Fx.Kind.Bubble -> {
                        val a = (min(1f, f.age * 8f) * min(1f, (f.life - f.age) * 4f)).coerceIn(0f, 1f)
                        val layout = measurer.measure(f.text, TextStyle(fontFamily = Sans, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, fontSize = 11.sp, color = Color(0xFF2B2433)))
                        val w = layout.size.width + 14f * density.density
                        val h = layout.size.height + 8f * density.density
                        val left = (f.x - w / 2).coerceIn(4f, size.width - w - 4f)
                        val top = f.y - h - 8f * density.density
                        drawRoundRect(Color.White.copy(alpha = a), Offset(left, top), Size(w, h), CornerRadius(h / 2))
                        drawRect(Color.White.copy(alpha = a), Offset(f.x - 3f * density.density, top + h - 1f), Size(6f * density.density, 4f * density.density))
                        drawText(layout, topLeft = Offset(left + 7f * density.density, top + 4f * density.density), alpha = a)
                    }
                }
            }
        }
    }
}

private val HEART = listOf(".KK.KK.", "KKKKKKK", "KKKKKKK", ".KKKKK.", "..KKK..", "...K...")
private val ZED = listOf("KKKK", "..K.", ".K..", "KKKK")

private fun DrawScope.pixelArt(rows: List<String>, cx: Float, cy: Float, px: Float, color: Color) {
    val w = rows[0].length * px
    rows.forEachIndexed { y, line ->
        line.forEachIndexed { x, c -> if (c != '.') drawRect(color, Offset(cx - w / 2 + x * px, cy + y * px), Size(px + 0.5f, px + 0.5f)) }
    }
}

@Composable
private fun PetSprite(pet: Pet, engine: PetEngine) {
    val density = LocalDensity.current
    val px = 1.4f * density.density
    val wPx = PetSprites.W * px
    val hPx = PetSprites.H * px
    Box(
        Modifier
            .offset {
                val flip = pet.facing < 0f
                val col = if (pet.topAnchored) PetSprites.GRIP_X else PetSprites.FOOT_X
                val anchorCol = if (flip) PetSprites.W - 1 - col else col
                val left = pet.x - (anchorCol + 0.5f) * px
                val top = if (pet.topAnchored) pet.y else pet.y - hPx
                pet.boxLeft = left; pet.boxTop = top
                IntOffset(left.roundToInt(), top.roundToInt())
            }
            .size(with(density) { wPx.toDp() }, with(density) { hPx.toDp() })
            .pointerInput(pet) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val t0 = down.uptimeMillis
                    val start = down.position + Offset(pet.boxLeft, pet.boxTop)
                    val vt = VelocityTracker()
                    vt.addPosition(down.uptimeMillis, start)
                    var dragging = false
                    var lastUp = t0
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        val abs = ch.position + Offset(pet.boxLeft, pet.boxTop)
                        lastUp = ch.uptimeMillis
                        if (!ch.pressed) break
                        vt.addPosition(ch.uptimeMillis, abs)
                        if (!dragging && (abs - start).getDistance() > viewConfiguration.touchSlop) {
                            dragging = true
                            engine.grab(pet)
                        }
                        if (dragging) { ch.consume(); engine.carry(pet, abs) }
                    }
                    if (dragging) {
                        val v = vt.calculateVelocity()
                        engine.release(pet, v.x, v.y)
                    } else if (lastUp - t0 < 500) engine.tap(pet)
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val a = art(pet, pet.frame)
            val flip = pet.facing < 0f
            val col = if (pet.topAnchored) PetSprites.GRIP_X else PetSprites.FOOT_X
            val anchorCol = if (flip) PetSprites.W - 1 - col else col
            val pivot = Offset((anchorCol + 0.5f) * px, if (pet.topAnchored) 0f else hPx)
            val sq = pet.squash
            withTransform({
                rotate(pet.tilt, pivot)
                scale(1f + 0.12f * sq, 1f - 0.16f * sq, pivot)
            }) {
                val n = a.xs.size
                for (i in 0 until n) {
                    val x = if (flip) PetSprites.W - 1 - a.xs[i] else a.xs[i]
                    drawRect(Color(a.colors[i]), Offset(x * px, a.ys[i] * px), Size(px + 0.6f, px + 0.6f))
                }
            }
        }
    }
}
