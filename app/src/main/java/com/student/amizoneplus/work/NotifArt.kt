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
        c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 56f, 56f, p)

        val condensed = runCatching { ResourcesCompat.getFont(context, R.font.barlow_condensed_bold) }.getOrNull() ?: Typeface.DEFAULT_BOLD
        val mono = runCatching { ResourcesCompat.getFont(context, R.font.dm_mono_medium) }.getOrNull() ?: Typeface.MONOSPACE

        // Pet on the right.
        val petW = if (big) 300 else 150
        val pet = pickPet(context)
        if (pet != null) drawPet(c, pet, mood, w - petW - (if (big) 40 else 26), h - (if (big) 40 else 18), petW)

        val left = if (big) 56f else 44f
        val textW = (w - petW - left - 70).toFloat()
        val ink = if (kind == "exam") CREAM else INK
        val tagP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono; textSize = if (big) 38f else 30f; color = ink; alpha = 200; letterSpacing = 0.12f }
        val titleP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = condensed; textSize = if (big) 104f else 68f; color = ink }
        val detP = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = mono; textSize = if (big) 36f else 28f; color = ink; alpha = 220 }

        var y = if (big) 96f else 58f
        c.drawText(TextUtils.ellipsize(tag.uppercase(), tagP, textW, TextUtils.TruncateAt.END).toString(), left, y, tagP)
        if (big) {
            // Title over up to two lines.
            val words = title.uppercase().split(' ')
            val lines = mutableListOf<String>(); var cur = ""
            for (wd in words) {
                val next = if (cur.isEmpty()) wd else "$cur $wd"
                if (titleP.measureText(next) <= textW || cur.isEmpty()) cur = next else { lines += cur; cur = wd }
            }
            if (cur.isNotEmpty()) lines += cur
            y += 30f
            lines.take(2).forEachIndexed { i, l ->
                y += 100f
                val s = if (i == 1 && lines.size > 2) TextUtils.ellipsize("$l ${lines.drop(2).joinToString(" ")}", titleP, textW, TextUtils.TruncateAt.END).toString() else l
                c.drawText(s, left, y, titleP)
            }
            y += 70f
        } else {
            y += 74f
            c.drawText(TextUtils.ellipsize(title.uppercase(), titleP, textW, TextUtils.TruncateAt.END).toString(), left, y, titleP)
            y += 50f
        }
        c.drawText(TextUtils.ellipsize(detail, detP, textW, TextUtils.TruncateAt.END).toString(), left, y, detP)
        return bmp
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
