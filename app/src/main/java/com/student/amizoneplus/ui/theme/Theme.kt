package com.student.amizoneplus.ui.theme

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.amizoneplus.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Palette after the reference: near-black base, bold colour heroes (sage, coral,
 * periwinkle), sky-blue highlights. Light mode keeps the heroes and flips the base.
 */
data class Palette(
    val dark: Boolean,
    val bg: Color, // screen background
    val sheet: Color, // sheet that slides over a hero
    val card: Color,
    val cardRaised: Color,
    val text: Color,
    val sub: Color,
    val faint: Color,
    val line: Color,
    val cream: Color, // white pills & round buttons
    val onCream: Color,
    val good: Color,
    val warn: Color,
    val bad: Color,
)

private val DarkPalette = Palette(
    dark = true,
    bg = Color(0xFF161617), sheet = Color(0xFF1F1F21), card = Color(0xFF29292C), cardRaised = Color(0xFF34343A),
    text = Color(0xFFF3F2EE), sub = Color(0xFFA3A3A8), faint = Color(0xFF6C6C72), line = Color(0xFF3A3A40),
    cream = Color(0xFFF4F3EF), onCream = Color(0xFF161617),
    good = Color(0xFF8FDCAA), warn = Color(0xFFF7C54A), bad = Color(0xFFFF7B6B),
)

private val LightPalette = Palette(
    dark = false,
    bg = Color(0xFFEFEEE9), sheet = Color(0xFFF8F7F4), card = Color(0xFFFFFFFF), cardRaised = Color(0xFFF1F0EC),
    text = Color(0xFF161617), sub = Color(0xFF616166), faint = Color(0xFF9D9DA2), line = Color(0xFFD9D8D2),
    cream = Color(0xFFFFFFFF), onCream = Color(0xFF161617),
    good = Color(0xFF1E8452), warn = Color(0xFFB07612), bad = Color(0xFFC6402F),
)

/**
 * Current colours. Read anywhere (including draw code); set once per composition by
 * [applyTheme] from the system light/dark setting (a change recreates the activity).
 */
object P {
    var cur: Palette = DarkPalette
        private set

    fun applyTheme(dark: Boolean) {
        cur = if (dark) DarkPalette else LightPalette
    }

    // Hero colours — identical in both modes.
    val sage = Color(0xFFC6D4CB)
    val coral = Color(0xFFF06A55)
    val coralSoft = Color(0xFFF89A86)
    val peri = Color(0xFF6F73F4)
    val periSoft = Color(0xFF8C90F7)
    val sky = Color(0xFFBDE9F4)
    val sun = Color(0xFFF7C54A)
    val lilac = Color(0xFF9A9DFA)
    val red = Color(0xFFF14B3B)
    val onHero = Color(0xFF161617) // text on any colour hero
    val nav = Color(0xFF222226) // the bottom nav pill stays dark in both modes

    val bg get() = cur.bg
    val sheet get() = cur.sheet
    val card get() = cur.card
    val cardRaised get() = cur.cardRaised
    val text get() = cur.text
    val sub get() = cur.sub
    val faint get() = cur.faint
    val line get() = cur.line
    val cream get() = cur.cream
    val onCream get() = cur.onCream
    val good get() = cur.good
    val warn get() = cur.warn
    val bad get() = cur.bad
    val goodSoft get() = good.copy(alpha = 0.18f)
    val warnSoft get() = warn.copy(alpha = 0.2f)
    val badSoft get() = bad.copy(alpha = 0.18f)
    val hairline get() = line

    // ── Aliases kept so older code keeps compiling ──
    val canvas get() = cur.bg
    val ink get() = cur.card
    val inkRaised get() = cur.cardRaised
    val onInk get() = cur.text
    val onInkSub get() = cur.sub
    val onInkFaint get() = cur.faint
    val inkLine get() = cur.line
    val lav get() = sky
    val lavSoft get() = sky.copy(alpha = 0.4f)
    val goodOnInk get() = cur.good
    val warnOnInk get() = cur.warn
    val badOnInk get() = cur.bad
}

/** Layout rhythm. */
object L {
    val page = 14.dp // side margins
    val block = 12.dp // gap between blocks
    val joint = 10.dp
    val radius = 28.dp // cards
    val sheetRadius = 36.dp // sheet sliding over a hero
    val notch = 44.dp
    val pad = 20.dp // card padding
    val header = 64.dp
    val nav = 56.dp
}

/**
 * Motion: subtle morphs and smoothing — no bounce. Emphasized-decelerate for
 * things arriving, near-critically-damped springs for things that move or reshape.
 */
object Motion {
    val emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    fun <T> spring() = spring<T>(dampingRatio = 0.92f, stiffness = 380f)
    fun <T> press() = spring<T>(dampingRatio = 0.9f, stiffness = 900f)
    fun <T> soft() = spring<T>(dampingRatio = 0.95f, stiffness = 200f)
    const val MORPH_MS = 520
    const val ENTER_MS = 460
    const val STAGGER_MS = 30L
}

/* ── Type: Barlow Condensed for display, DM Mono for everything else ── */

val Condensed = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)
val Mono = FontFamily(
    Font(R.font.dm_mono_light, FontWeight.Light),
    Font(R.font.dm_mono_regular, FontWeight.Normal),
    Font(R.font.dm_mono_medium, FontWeight.Medium),
)

object Type {
    // Condensed, uppercase by convention (callers pass .uppercase()).
    val hero = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 76.sp, lineHeight = 70.sp, letterSpacing = (-1).sp)
    val display = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 46.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp)
    val headline = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp)
    val title = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 28.sp)
    val num = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp)
    val numSm = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 30.sp)
    // Mono
    val label = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp)
    val body = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.2.sp)
    val caption = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp)
    val caps = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.6.sp)
}

fun attendanceTone(pct: Double, threshold: Int, onInk: Boolean = true): Color = when {
    pct >= threshold -> P.good
    pct >= threshold - 5 -> P.warn
    else -> P.bad
}

/**
 * Haptic vocabulary: each interaction has its own feel. All no-ops when the user
 * switches haptics off in More.
 */
class Haptics(private val view: View, private val enabled: () -> Boolean) {
    private fun h(c: Int) {
        if (enabled()) view.performHapticFeedback(c)
    }

    fun select() = h(HapticFeedbackConstants.CLOCK_TICK)
    fun tap() = h(HapticFeedbackConstants.KEYBOARD_TAP)
    fun press() = h(HapticFeedbackConstants.VIRTUAL_KEY)
    fun expand() = h(HapticFeedbackConstants.CONTEXT_CLICK)
    fun collapse() = h(HapticFeedbackConstants.KEYBOARD_TAP)
    fun tick() = h(HapticFeedbackConstants.CLOCK_TICK)
    fun pull() = h(HapticFeedbackConstants.GESTURE_START)
    fun success() = h(HapticFeedbackConstants.CONFIRM)
    fun warning() = h(HapticFeedbackConstants.REJECT)
    fun error() = h(HapticFeedbackConstants.REJECT)

    /* ── Pull-to-refresh tension (LRA primitives; simple ticks on older phones) ── */

    private val vibrator: android.os.Vibrator? by lazy {
        val ctx = view.context
        if (android.os.Build.VERSION.SDK_INT >= 31) ctx.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") ctx.getSystemService(android.os.Vibrator::class.java)
    }
    private val rich: Boolean by lazy {
        android.os.Build.VERSION.SDK_INT >= 31 && vibrator?.areAllPrimitivesSupported(
            android.os.VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
            android.os.VibrationEffect.Composition.PRIMITIVE_CLICK,
            android.os.VibrationEffect.Composition.PRIMITIVE_THUD,
            android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
            android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
        ) == true
    }

    private fun compose(vararg parts: Pair<Int, Float>) {
        if (!enabled()) return
        if (android.os.Build.VERSION.SDK_INT < 31 || !rich) return
        val c = android.os.VibrationEffect.startComposition()
        parts.forEach { (p, s) -> c.addPrimitive(p, s.coerceIn(0.05f, 1f)) }
        runCatching { vibrator?.vibrate(c.compose()) }
    }

    /** One tension tick; [level] 0..1 grows as the pull nears the threshold. */
    fun tension(level: Float) {
        if (rich && android.os.Build.VERSION.SDK_INT >= 31) compose(android.os.VibrationEffect.Composition.PRIMITIVE_LOW_TICK to 0.12f + 0.88f * level * level)
        else h(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Pulled far enough: a firm thunk. */
    fun armed() {
        if (rich && android.os.Build.VERSION.SDK_INT >= 31) compose(
            android.os.VibrationEffect.Composition.PRIMITIVE_CLICK to 1f,
            android.os.VibrationEffect.Composition.PRIMITIVE_THUD to 0.7f,
        ) else h(HapticFeedbackConstants.LONG_PRESS)
    }

    /** Backed off below the threshold again. */
    fun disarm() {
        if (rich && android.os.Build.VERSION.SDK_INT >= 31) compose(android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_FALL to 0.5f)
        else h(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Let go to refresh: a quick swell. */
    fun launch() {
        if (rich && android.os.Build.VERSION.SDK_INT >= 31) compose(android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_RISE to 0.8f)
        else h(HapticFeedbackConstants.GESTURE_END)
    }

    fun celebrate() {
        if (!enabled()) return
        CoroutineScope(Dispatchers.Main).launch {
            for ((wait, c) in listOf(0L to HapticFeedbackConstants.LONG_PRESS, 90L to HapticFeedbackConstants.VIRTUAL_KEY,
                80L to HapticFeedbackConstants.KEYBOARD_TAP, 150L to HapticFeedbackConstants.CONFIRM)) {
                delay(wait)
                view.performHapticFeedback(c)
            }
        }
    }
}

val LocalHaptics = staticCompositionLocalOf<Haptics> { error("Haptics not provided") }
