package com.student.amizoneplus.ui.kit

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/*
 * Squircle ("continuous") corners, as on iOS: instead of a quarter circle that meets the
 * straight edge abruptly, each corner starts bending earlier and eases in, so shapes look
 * softer and more natural. Every rounded rectangle in the app uses these.
 *
 * Each corner of nominal radius r is one cubic curve that starts 1.45·r from the corner on
 * both edges, with control points 0.2 of that way in. That keeps the corner the same visual
 * size as a circular one of radius r (the curve's midpoint sits where the circle's would).
 * A radius of half the short side or more is a pill, whose ends are squircle curves too.
 */

private const val SPAN = 1.45f
private const val PULL = 0.2f

/** A squircle-cornered rectangle; radii in px (top-left, top-right, bottom-right, bottom-left). */
fun squirclePath(rect: Rect, tl: Float, tr: Float, br: Float, bl: Float): Path {
    val half = min(rect.width, rect.height) / 2f
    val path = Path()
    if (half <= 0f) return path
    // All corners at least half the short side: it's a pill, with squircle ends.
    if (min(min(tl, tr), min(br, bl)) >= half * 0.98f) return squirclePillPath(rect)
    fun span(r: Float) = min(r * SPAN, half)
    val a = span(tl); val b = span(tr); val c = span(br); val d = span(bl)
    val l = rect.left; val t = rect.top; val r = rect.right; val btm = rect.bottom
    path.moveTo(l, t + a)
    path.cubicTo(l, t + a * PULL, l + a * PULL, t, l + a, t)
    path.lineTo(r - b, t)
    path.cubicTo(r - b * PULL, t, r, t + b * PULL, r, t + b)
    path.lineTo(r, btm - c)
    path.cubicTo(r, btm - c * PULL, r - c * PULL, btm, r - c, btm)
    path.lineTo(l + d, btm)
    path.cubicTo(l + d * PULL, btm, l, btm - d * PULL, l, btm - d)
    path.close()
    return path
}

fun squirclePath(rect: Rect, radius: Float): Path = squirclePath(rect, radius, radius, radius, radius)

/** The shape behind [SquircleShape]: squircle corners, radii in dp or as a percent of the short side. */
class SquircleCornerShape(
    private val topStart: Dp,
    private val topEnd: Dp,
    private val bottomEnd: Dp,
    private val bottomStart: Dp,
    private val percent: Int? = null,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val rect = Rect(0f, 0f, size.width, size.height)
        if (percent != null) return Outline.Generic(squirclePath(rect, min(size.width, size.height) * percent / 100f))
        with(density) {
            val ts = topStart.toPx(); val te = topEnd.toPx(); val be = bottomEnd.toPx(); val bs = bottomStart.toPx()
            return if (layoutDirection == LayoutDirection.Ltr) Outline.Generic(squirclePath(rect, ts, te, be, bs))
            else Outline.Generic(squirclePath(rect, te, ts, bs, be))
        }
    }
}

/** Drop-in replacement for RoundedCornerShape(16.dp), with squircle corners. */
fun SquircleShape(size: Dp): Shape = SquircleCornerShape(size, size, size, size)

/** Like RoundedCornerShape(50): percent of the short side (50 = pill). */
fun SquircleShape(percent: Int): Shape = SquircleCornerShape(0.dp, 0.dp, 0.dp, 0.dp, percent)

/** Like RoundedCornerShape(topStart = …, topEnd = …). */
fun SquircleShape(topStart: Dp = 0.dp, topEnd: Dp = 0.dp, bottomEnd: Dp = 0.dp, bottomStart: Dp = 0.dp): Shape =
    SquircleCornerShape(topStart, topEnd, bottomEnd, bottomStart)

/** Squircle shape with radii in px (for use inside graphicsLayer, where radii are computed in px). */
class SquirclePxShape(private val tl: Float, private val tr: Float, private val br: Float, private val bl: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(squirclePath(Rect(0f, 0f, size.width, size.height), tl, tr, br, bl))
}

/**
 * A squircle pill: ends made of two squircle curves that meet in the middle, so they're
 * softer and slightly flatter than a semicircle (the look of the search and Amizone ID bars).
 */
fun squirclePillPath(rect: Rect): Path {
    val half = min(rect.width, rect.height) / 2f
    val path = Path()
    if (half <= 0f) return path
    val l = rect.left; val t = rect.top; val r = rect.right; val b = rect.bottom
    path.moveTo(l, t + half)
    path.cubicTo(l, t + half * PULL, l + half * PULL, t, l + half, t)
    path.lineTo(r - half, t)
    path.cubicTo(r - half * PULL, t, r, t + half * PULL, r, t + half)
    path.cubicTo(r, b - half * PULL, r - half * PULL, b, r - half, b)
    path.lineTo(l + half, b)
    path.cubicTo(l + half * PULL, b, l, b - half * PULL, l, t + half)
    path.close()
    return path
}
