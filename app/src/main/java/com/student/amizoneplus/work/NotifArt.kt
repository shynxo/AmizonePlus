package com.student.amizoneplus.work

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.student.amizoneplus.R
import com.student.amizoneplus.ui.pets.PetSprites

/**
 * Reminder notifications drawn in the app's own style: a rounded colour card, big
 * condensed type, and a pixel pet keeping you company (sleeping the night before an
 * exam, sitting up when it's close).
 */
object NotifArt {
    private const val PERI = 0xFF6F73F4.toInt()
    private const val SUN = 0xFFF7C54A.toInt()
    private const val SAGE = 0xFFC6D4CB.toInt()
    private const val INK = 0xFF161617.toInt()
    private const val CREAM = 0xFFF4F1EA.toInt()

    enum class Mood { Sleep, Sit }

    /** Card for reminders. [tag] is the small caps line, [title] the big one, [detail] the last. */
    fun card(context: Context, tag: String, title: String, detail: String, kind: String, mood: Mood, big: Boolean): Bitmap {
        val w = 1080
        val h = if (big) 460 else 210
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = when (kind) { "exam" -> PERI; "class" -> SAGE; else -> SUN }
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg }
        // Squircle card, like the rest of the app.
        c.drawPath(com.student.amizoneplus.ui.kit.squirclePath(androidx.compose.ui.geometry.Rect(0f, 0f, w.toFloat(), h.toFloat()), 56f).let {
            (it as androidx.compose.ui.graphics.AndroidPath).internalPath }, p)

        val condensed = runCatching { ResourcesCompat.getFont(context, R.font.barlow_condensed_bold) }.getOrNull() ?: Typeface.DEFAULT_BOLD
        val mono = runCatching { ResourcesCompat.getFont(context, R.font.dm_sans)!!.let { if (android.os.Build.VERSION.SDK_INT >= 28) Typeface.create(it, 600, false) else Typeface.create(it, Typeface.BOLD) } }
            .getOrNull() ?: Typeface.DEFAULT_BOLD // DM Sans, like the app

        // Pet on the right.
        val petW = if (big) 300 else 150
        val pet = pickPet(context)
        if (pet != null) drawPet(c, pet, mood, w - petW - (if (big) 40 else 26), h - (if (big) 40 else 18), petW)

        val left = if (big) 56f else 44f
        val textW = (w - petW - left - 70).toFloat()
        val ink = if (kind == "exam") CREAM else INK
        val tagP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono; textSize = if (big) 36f else 28f; color = ink; alpha = 200; letterSpacing = 0.12f }
        val detP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono; textSize = if (big) 32f else 25f; color = ink; alpha = 215 }

        // Big short name ("OOPs", "Maths III", "DSA"), sized down until it fits in full —
        // never "…". The full course name sits underneath in small type.
        val short = com.student.amizoneplus.logic.shortCourseName(title).uppercase().replace("OOPS", "OOPs")
        val full = title.trim()
        val titleP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = condensed; color = ink }
        var size = if (big) 112f else 70f
        val minSize = if (big) 58f else 40f
        titleP.textSize = size
        while (titleP.measureText(short) > textW && size > minSize) { size -= 2f; titleP.textSize = size }

        val lines = mutableListOf<Pair<String, TextPaint>>()
        lines += tag.uppercase() to tagP
        lines += short to titleP
        if (!full.equals(short, true)) lines += full to detP
        if (big && detail.isNotBlank()) lines += detail to detP

        // Lay out top to bottom, centred vertically in the card.
        val gaps = lines.map { (_, pt) -> pt.textSize * (if (pt === titleP) 1.02f else 1.35f) }
        var y = (h - gaps.sum()) / 2f
        lines.forEachIndexed { i, (txt, pt) ->
            y += gaps[i]
            if (pt === titleP) c.drawText(txt, left, y - pt.textSize * 0.12f, pt)
            else y += smallFit(txt, pt, textW, if (big && pt === detP) 2 else 1, c, left, y, gaps[i])
        }
        return bmp
    }

    /**
     * Draws small text without "…" where possible: shrinks it a little, then wraps to a
     * second line (when [maxLines] is 2). Returns the extra height used.
     */
    private fun smallFit(text: String, p: TextPaint, width: Float, maxLines: Int, c: Canvas, x: Float, y: Float, lineH: Float): Float {
        val base = p.textSize
        while (p.measureText(text) > width && p.textSize > base * 0.8f) p.textSize -= 1f
        var extra = 0f
        if (p.measureText(text) <= width) c.drawText(text, x, y, p)
        else if (maxLines >= 2) {
            val words = text.split(' '); var first = ""
            for (wd in words) { val n = if (first.isEmpty()) wd else "$first $wd"; if (p.measureText(n) <= width) first = n else break }
            c.drawText(first, x, y, p)
            extra = lineH * 0.85f
            c.drawText(TextUtils.ellipsize(text.removePrefix(first).trim(), p, width, TextUtils.TruncateAt.END).toString(), x, y + extra, p)
        } else c.drawText(TextUtils.ellipsize(text, p, width, TextUtils.TruncateAt.END).toString(), x, y, p)
        p.textSize = base
        return extra
    }

    private data class Pet(val frames: Map<String, Array<String>>, val pal: Map<Char, Int>, val cat: Boolean)

    private fun pickPet(context: Context): Pet? {
        val choice = runCatching { com.student.amizoneplus.data.Storage(context).loadSettings().pets }.getOrDefault("both")
        val cat = when (choice) { "cat" -> true; "dog" -> false; "none" -> return null; else -> Math.random() < 0.5 }
        return if (cat) Pet(PetSprites.CAT, PetSprites.CAT_PALETTE, true) else Pet(PetSprites.DOG, PetSprites.DOG_PALETTE, false)
    }

    /** Draws the sprite with its feet on (x .. x+width, baseline). */
    private fun drawPet(c: Canvas, pet: Pet, mood: Mood, x: Int, baseline: Int, width: Int) {
        val name = when (mood) {
            Mood.Sleep -> if (pet.cat) "loaf0" else "lie0"
            Mood.Sit -> if (pet.cat) "sit0" else "sit_tongue"
        }
        val rows = pet.frames[name] ?: return
        val px = width / PetSprites.W.toFloat()
        val top = baseline - PetSprites.H * px
        val paint = Paint()
        rows.forEachIndexed { ry, line ->
            line.forEachIndexed { rx, ch ->
                if (ch == '.') return@forEachIndexed
                paint.color = pet.pal[ch] ?: return@forEachIndexed
                c.drawRect(x + rx * px, top + ry * px, x + (rx + 1) * px + 0.5f, top + (ry + 1) * px + 0.5f, paint)
            }
        }
        if (mood == Mood.Sleep) { // pixel z's
            paint.color = CREAM
            val zs = listOf("KKKK", "..K.", ".K..", "KKKK")
            for ((k, sz) in listOf(0 to 0.9f, 1 to 0.65f)) {
                val zx = x + width * (0.62f + k * 0.16f)
                val zy = top + PetSprites.H * px * (0.1f - k * 0.12f)
                zs.forEachIndexed { ry, l -> l.forEachIndexed { rx, ch -> if (ch == 'K') c.drawRect(zx + rx * px * sz, zy + ry * px * sz, zx + (rx + 1) * px * sz, zy + (ry + 1) * px * sz, paint) } }
            }
        }
    }
}
