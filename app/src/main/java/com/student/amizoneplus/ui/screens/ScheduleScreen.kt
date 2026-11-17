package com.student.amizoneplus.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.amizoneplus.ui.UiState
import com.student.amizoneplus.ui.kit.Caps
import com.student.amizoneplus.ui.kit.Card
import com.student.amizoneplus.ui.kit.Chip
import com.student.amizoneplus.ui.kit.ClockDial
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.Rise
import com.student.amizoneplus.ui.kit.Segmented
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.Tone
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.shell.HeroPage
import com.student.amizoneplus.ui.shell.MenuButton
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ceil

@Composable
fun ScheduleScreen(s: UiState, onRefresh: () -> Unit, banner: (@Composable () -> Unit)?, focusTab: String? = null, onFocusHandled: () -> Unit = {}) {
    var tab by remember { mutableStateOf(focusTab ?: "classes") }
    LaunchedEffect(focusTab) { if (focusTab != null) { tab = focusTab; onFocusHandled() } }
    var day by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    val exams = s.data?.exams?.exams.orEmpty()
    val days = remember { List(7) { i -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, i) }.timeInMillis } }
    val classes = s.data?.schedule.orEmpty().filter { sameDay(it.startMs, day) }
    val hot = classes.firstOrNull { it.endMs > now }
    val tz = TimeZone.getDefault().let { z ->
        val off = z.getOffset(now) / 60000
        "UTC" + (if (off >= 0) "+" else "-") + "%d".format(kotlin.math.abs(off) / 60) + (if (off % 60 != 0) ":%02d".format(kotlin.math.abs(off) % 60) else "")
    }

    HeroPage(
        heroColor = P.sage,
        refreshing = s.refreshing,
        onRefresh = onRefresh,
        banner = banner,
        hero = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                T(if (sameDay(day, now)) "TODAY'S\nTIMING" else SimpleDateFormat("EEEE\nd MMM", Locale.getDefault()).format(Date(day)).uppercase(),
                    Type.display, color = P.onHero, modifier = Modifier.weight(1f))
                MenuButton()
            }
            Box(Modifier.fillMaxWidth().padding(top = 18.dp), contentAlignment = Alignment.Center) {
                ClockDial(classes.map { Triple(it.startMs, it.endMs, it == hot) }, Modifier.fillMaxWidth(0.78f).aspectRatio(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(P.onHero.copy(alpha = 0.08f), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 8.dp)) { T(tz, Type.label, color = P.onHero) }
                Box(Modifier.weight(1f))
                Box(Modifier.background(P.onHero.copy(alpha = 0.08f), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                    T(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(now)), Type.label, color = P.onHero)
                }
            }
        },
    ) {
        Segmented(listOf("classes" to "Timetable", "exams" to (if (exams.isNotEmpty()) "Exams · ${exams.size}" else "Exams")), tab, { tab = it })
        AnimatedContent(tab, transitionSpec = { fadeIn(tween(280, easing = Motion.emphasized)) togetherWith fadeOut(tween(140)) }, label = "tab") { t ->
            Column(verticalArrangement = Arrangement.spacedBy(L.block)) {
                if (t == "classes") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val f = SimpleDateFormat("EEE d", Locale.getDefault())
                        days.forEachIndexed { i, d ->
                            val active = sameDay(d, day)
                            val n = s.data?.schedule.orEmpty().count { sameDay(it.startMs, d) }
                            Row(
                                Modifier.press({ day = d }, if (active) Feel.None else Feel.Select, 0.94f)
                                    .background(if (active) P.cream else Color.Transparent, RoundedCornerShape(50))
                                    .border(1.dp, if (active) P.cream else P.line, RoundedCornerShape(50))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Caps(if (i == 0) "Today" else f.format(Date(d)), if (active) P.onCream else P.text)
                                if (n > 0) Box(Modifier.padding(start = 8.dp).size(6.dp).background(if (active) P.coral else P.sub, CircleShape))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Upcoming", Tone.Ink, P.coral)
                        Chip("Done", Tone.Ink, P.sage)
                    }
                    if (classes.isEmpty()) T("No classes on this day.", Type.body, sub = true, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 32.dp))
                    var nowShown = !sameDay(day, now)
                    classes.forEachIndexed { i, c ->
                        if (!nowShown && c.startMs > now) { nowShown = true; NowLine(now) }
                        Rise(i) { ClassRow(c) }
                    }
                    if (!nowShown && classes.isNotEmpty()) NowLine(now)
                } else {
                    s.data?.exams?.title?.let { T(it.uppercase(), Type.caption, sub = true) }
                    if (exams.isEmpty()) T("No exams scheduled right now.", Type.body, sub = true, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 32.dp))
                    exams.forEachIndexed { i, e ->
                        val past = (e.timeMs ?: Long.MAX_VALUE) + com.student.amizoneplus.logic.EXAM_LENGTH_MS < now
                        val live = e.timeMs?.let { now >= it && !past } == true
                        val left = e.timeMs?.let { com.student.amizoneplus.logic.daysUntil(it, now) }
                        Rise(i) {
                            Card(color = P.card) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column(
                                        Modifier.width(66.dp).background(if (past) P.cardRaised else P.coral, RoundedCornerShape(20.dp)).padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Caps(e.timeMs?.let { SimpleDateFormat("MMM", Locale.getDefault()).format(Date(it)).uppercase() } ?: "—", if (past) P.sub else P.onHero)
                                        T(e.timeMs?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.DAY_OF_MONTH).toString() } ?: "?",
                                            Type.num.copy(fontSize = 38.sp, lineHeight = 38.sp), color = if (past) P.sub else P.onHero)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        T(e.name.uppercase(), Type.title, maxLines = 2, faint = past)
                                        T(e.code + (e.timeMs?.let { " · " + SimpleDateFormat("EEE h:mm a", Locale.getDefault()).format(Date(it)) } ?: ""),
                                            Type.caption, sub = true, modifier = Modifier.padding(top = 4.dp))
                                        if (e.location.isNotEmpty()) T(e.location, Type.caption, sub = true, modifier = Modifier.padding(top = 2.dp))
                                        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (e.mode.isNotEmpty()) Chip(e.mode, Tone.Lav)
                                            if (!past && left != null) Chip(when { live -> "Now"; left <= 0 -> "Today"; left == 1 -> "Tomorrow"; else -> "In $left days" },
                                                if (left <= 3) Tone.Bad else Tone.Sage)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The reference's "now" marker: a sky time chip with a line across. */
@Composable
private fun NowLine(now: Long) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.background(P.sky, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)) { T(hm(now), Type.caption, color = P.onHero) }
        Box(Modifier.weight(1f).height(1.5.dp).background(P.sky.copy(alpha = 0.6f)))
    }
}
