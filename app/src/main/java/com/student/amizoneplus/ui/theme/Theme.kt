package com.student.amizoneplus.ui.theme

import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationEffect.Composition
import android.os.Vibrator
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
    val coralDeep = Color(0xFFD4604E) // Subjects: coral, a little deeper so it isn't harsh
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

/* ── Type: Barlow Condensed for display, DM Sans for everything else ── */

val Condensed = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

/** DM Sans (variable font): soft, round, easy-reading body text. One file, every weight. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun dmSans(w: FontWeight) = Font(
    R.font.dm_sans, w,
    variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(
        androidx.compose.ui.text.font.FontVariation.weight(w.weight),
        androidx.compose.ui.text.font.FontVariation.Setting("opsz", 14f),
    ),
)
val Sans = FontFamily(dmSans(FontWeight.Light), dmSans(FontWeight.Normal), dmSans(FontWeight.Medium), dmSans(FontWeight.SemiBold))

object Type {
    // Condensed, uppercase by convention (callers pass .uppercase()).
    val hero = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 76.sp, lineHeight = 70.sp, letterSpacing = (-1).sp)
    val display = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 46.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp)
    val headline = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp)
    val title = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 28.sp)
    val num = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp)
    val numSm = TextStyle(fontFamily = Condensed, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 30.sp)
    // Sans. Digits are tabular ("tnum"), so times and counts don't jiggle as they change.
    val label = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp, fontFeatureSettings = "tnum")
    val body = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.sp, fontFeatureSettings = "tnum")
    val caption = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.1.sp, fontFeatureSettings = "tnum")
    val caps = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.8.sp, fontFeatureSettings = "tnum")
}

fun attendanceTone(pct: Double, threshold: Int, onInk: Boolean = true): Color = when {
    pct >= threshold -> P.good
    pct >= threshold - 5 -> P.warn
    else -> P.bad
}

/**
 * Haptic vocabulary: each interaction has its own feel, played the best way THIS phone's
 * vibration motor can. Android's built-in "haptic feedback constants" assume a crisp
 * linear motor (LRA, as in Pixels / OnePlus) and are silently ignored on many phones:
 * cheaper ones with a spinning-weight motor (ERM), older Android versions, or phones with
 * system touch vibration turned off. So the app detects what the motor can do (see
 * [Motor]) and drives it directly. All no-ops when the user switches haptics off.
 */
class Haptics(private val view: View, private val enabled: () -> Boolean) {
    /** What the vibration hardware can do, best first. */
    enum class Motor(val label: String) {
        Rich("Linear motor, full effects"),       // Android 12+ with haptic primitives (good LRA)
        Tuned("Linear motor, preset effects"),    // phone ships tuned click / tick effects
        Amplitude("Variable-strength motor"),     // can vary strength, no presets
        Basic("Basic motor"),                     // on/off only (usually an ERM): longer pulses
        None("No vibration motor"),
    }
    private enum class Fx { Tick, Click, Heavy, Double, Rise, Fall }

    private val vibrator: Vibrator? by lazy {
        val ctx = view.context
        if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") ctx.getSystemService(Vibrator::class.java)
    }

    val motor: Motor by lazy {
        val v = vibrator
        when {
            v == null || !v.hasVibrator() -> Motor.None
            Build.VERSION.SDK_INT >= 31 && v.areAllPrimitivesSupported(
                Composition.PRIMITIVE_TICK, Composition.PRIMITIVE_LOW_TICK, Composition.PRIMITIVE_CLICK,
                Composition.PRIMITIVE_THUD, Composition.PRIMITIVE_QUICK_RISE, Composition.PRIMITIVE_QUICK_FALL,
            ) -> Motor.Rich
            Build.VERSION.SDK_INT >= 30 && v.areAllEffectsSupported(
                VibrationEffect.EFFECT_TICK, VibrationEffect.EFFECT_CLICK,
                VibrationEffect.EFFECT_HEAVY_CLICK, VibrationEffect.EFFECT_DOUBLE_CLICK,
            ) == Vibrator.VIBRATION_EFFECT_SUPPORT_YES -> Motor.Tuned
            v.hasAmplitudeControl() -> Motor.Amplitude
            else -> Motor.Basic
        }
    }

    private fun play(e: VibrationEffect) { runCatching { vibrator?.vibrate(e) } }

    /** [strength] 0..1 scales the effect where the motor allows it. */
    private fun fx(f: Fx, strength: Float = 1f) {
        if (!enabled()) return
        val k = strength.coerceIn(0.05f, 1f)
        when (motor) {
            Motor.None -> {}
            Motor.Rich -> if (Build.VERSION.SDK_INT >= 31) {
                val c = VibrationEffect.startComposition()
                when (f) {
                    Fx.Tick -> c.addPrimitive(Composition.PRIMITIVE_TICK, 0.7f * k)
                    Fx.Click -> c.addPrimitive(Composition.PRIMITIVE_CLICK, 0.8f * k)
                    Fx.Heavy -> c.addPrimitive(Composition.PRIMITIVE_CLICK, k).addPrimitive(Composition.PRIMITIVE_THUD, 0.7f * k)
                    Fx.Double -> c.addPrimitive(Composition.PRIMITIVE_CLICK, 0.8f * k).addPrimitive(Composition.PRIMITIVE_CLICK, 0.8f * k, 70)
                    Fx.Rise -> c.addPrimitive(Composition.PRIMITIVE_QUICK_RISE, 0.8f * k)
                    Fx.Fall -> c.addPrimitive(Composition.PRIMITIVE_QUICK_FALL, 0.5f * k)
                }
                play(c.compose())
            }
            Motor.Tuned -> if (Build.VERSION.SDK_INT >= 29) play(VibrationEffect.createPredefined(when (f) {
                Fx.Tick, Fx.Fall -> VibrationEffect.EFFECT_TICK
                Fx.Click, Fx.Rise -> VibrationEffect.EFFECT_CLICK
                Fx.Heavy -> VibrationEffect.EFFECT_HEAVY_CLICK
                Fx.Double -> VibrationEffect.EFFECT_DOUBLE_CLICK
            }))
            // Short pulses with strength control.
            Motor.Amplitude -> {
                fun a(x: Float) = (x * k * 255).toInt().coerceIn(1, 255)
                play(when (f) {
                    Fx.Tick -> VibrationEffect.createOneShot(8, a(0.45f))
                    Fx.Click -> VibrationEffect.createOneShot(14, a(0.75f))
                    Fx.Heavy -> VibrationEffect.createOneShot(26, a(1f))
                    Fx.Double -> VibrationEffect.createWaveform(longArrayOf(0, 14, 70, 14), intArrayOf(0, a(0.8f), 0, a(0.8f)), -1)
                    Fx.Rise -> VibrationEffect.createWaveform(longArrayOf(0, 10, 10, 14), intArrayOf(0, a(0.35f), a(0.65f), a(1f)), -1)
                    Fx.Fall -> VibrationEffect.createWaveform(longArrayOf(0, 10, 10), intArrayOf(0, a(0.5f), a(0.25f)), -1)
                })
            }
            // On/off motors need ~20 ms just to spin up, so very short pulses are never felt.
            Motor.Basic -> play(when (f) {
                Fx.Tick, Fx.Fall -> VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE)
                Fx.Click, Fx.Rise -> VibrationEffect.createOneShot(28, VibrationEffect.DEFAULT_AMPLITUDE)
                Fx.Heavy -> VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                Fx.Double -> VibrationEffect.createWaveform(longArrayOf(0, 28, 90, 28), -1)
            })
        }
    }

    fun select() = fx(Fx.Tick)
    fun tap() = fx(Fx.Click, 0.7f)
    fun press() = fx(Fx.Click)
    fun expand() = fx(Fx.Click, 0.85f)
    fun collapse() = fx(Fx.Tick, 0.9f)
    fun tick() = fx(Fx.Tick, 0.8f)
    fun pull() = fx(Fx.Tick)
    fun success() = fx(Fx.Double)
    fun warning() = fx(Fx.Heavy, 0.7f)
    fun error() = fx(Fx.Heavy)

    /* Pull-to-refresh */

    private var lastTension = 0L
    /** One tension tick; [level] 0..1 grows as the pull nears the threshold. */
    fun tension(level: Float) {
        // A basic motor can't do featherweight ticks: space them out so it doesn't just buzz.
        if (motor == Motor.Basic) {
            val now = android.os.SystemClock.uptimeMillis()
            if (now - lastTension < 120) return
            lastTension = now
        }
        fx(Fx.Tick, 0.15f + 0.85f * level * level)
    }
    /** Pulled far enough: a firm thunk. */
    fun armed() = fx(Fx.Heavy)
    /** Backed off below the threshold again. */
    fun disarm() = fx(Fx.Fall)
    /** Let go to refresh: a quick swell. */
    fun launch() = fx(Fx.Rise)

    fun celebrate() {
        if (!enabled()) return
        CoroutineScope(Dispatchers.Main).launch {
            for ((wait, f) in listOf(0L to Fx.Heavy, 110L to Fx.Click, 90L to Fx.Tick, 160L to Fx.Double)) {
                delay(wait)
                fx(f)
            }
        }
    }
}

val LocalHaptics = staticCompositionLocalOf<Haptics> { error("Haptics not provided") }
