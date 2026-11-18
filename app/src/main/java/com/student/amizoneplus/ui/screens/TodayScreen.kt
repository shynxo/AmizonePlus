package com.student.amizoneplus.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import com.student.amizoneplus.ui.pets.perch
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.student.amizoneplus.data.ScheduledClass
import com.student.amizoneplus.logic.creditsFor
import com.student.amizoneplus.logic.earnedBadges
import com.student.amizoneplus.logic.percent
import com.student.amizoneplus.logic.presentStreak
import com.student.amizoneplus.logic.statusText
import com.student.amizoneplus.logic.currentWeek
import com.student.amizoneplus.ui.UiState
import com.student.amizoneplus.ui.kit.AreaChart
import com.student.amizoneplus.ui.kit.ArrowButton
import com.student.amizoneplus.ui.kit.Avatar
import com.student.amizoneplus.ui.kit.Card
import com.student.amizoneplus.ui.kit.Chip
import com.student.amizoneplus.ui.kit.CircleButton
import com.student.amizoneplus.ui.kit.CountUp
import com.student.amizoneplus.ui.kit.CountdownChip
import com.student.amizoneplus.ui.kit.DayCircles
import com.student.amizoneplus.ui.kit.DropdownPill
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.PairCards
import com.student.amizoneplus.ui.kit.Rise
import com.student.amizoneplus.ui.kit.SectionLabel
import com.student.amizoneplus.ui.kit.SelectorPill
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.Tone
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.shell.LocalMenu
import com.student.amizoneplus.ui.shell.PlainPage
import com.student.amizoneplus.ui.shell.Route
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.Motion
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.ceil

fun sameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

fun hm(ms: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))

/** Daily attendance rate (0..1) for each day with marked classes, oldest first. */
private fun dailySeries(history: List<ScheduledClass>): List<Pair<Long, Float>> =
    history.filter { it.state == "present" || it.state == "absent" }
        .groupBy { Calendar.getInstance().apply { timeInMillis = it.startMs }.get(Calendar.DAY_OF_YEAR) }
        .map { (_, cs) -> cs.first().startMs to cs.count { it.state == "present" }.toFloat() / cs.size }
        .sortedBy { it.first }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    s: UiState, go: (Route) -> Unit, onRefresh: () -> Unit, onBadges: () -> Unit, banner: (@Composable () -> Unit)?,
    onAvatar: (androidx.compose.ui.geometry.Rect) -> Unit,
) {
    val menu = LocalMenu.current
    val data = s.data
    val t = s.settings.threshold
    val now = System.currentTimeMillis()
    val att = data?.attendance.orEmpty()
    val overall = att.sumOf { it.held }.let { h -> if (h > 0) percent(att.sumOf { it.attended }, h) else 0.0 }
    val low = att.filter { it.held > 0 && percent(it.attended, it.held) < t }
    val today = data?.schedule.orEmpty().filter { sameDay(it.startMs, now) }
    val upcoming = data?.schedule.orEmpty().firstOrNull { it.endMs > now }
    val live = upcoming != null && upcoming.startMs <= now
    val nextExam = data?.exams?.exams.orEmpty().firstOrNull { (it.timeMs ?: 0) + com.student.amizoneplus.logic.EXAM_LENGTH_MS > now }
    val name = data?.profile?.name ?: s.username ?: ""
    val first = name.substringBefore(' ')
    val badges = data?.let { earnedBadges(it, t) }.orEmpty()
    val streak = presentStreak(data?.history.orEmpty())

    var range by remember { mutableStateOf("4 weeks") }
    val series = dailySeries(data?.history.orEmpty()).let { if (range == "this week") it.takeLast(7) else it }
    val week = currentWeek(data?.history.orEmpty(), data?.schedule.orEmpty(), now)
    val todayIdx = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7
    var selDay by remember { mutableIntStateOf((Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7) }
    var i = 0

    PlainPage(s.refreshing, onRefresh, banner) {
        // Greeting row
        Rise(i++) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                // Tap your initials: Settings / About grows out of this squircle.
                var avatarBox by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
                Box(
                    Modifier
                        .onGloballyPositioned { avatarBox = it.boundsInWindow() }
                        .press({ onAvatar(avatarBox) }, Feel.Select, 0.88f),
                ) { Avatar(name.ifEmpty { "A" }, P.sun, 52.dp) }
                T(if (first.isNotEmpty()) "Hi, $first!" else "Hi!", Type.label.copy(fontSize = 18.sp()), modifier = Modifier.weight(1f).padding(start = 14.dp), maxLines = 1)
                CircleButton(Icons.Outlined.Apps, menu.open, 52.dp)
                Spacer(Modifier.width(10.dp))
                CircleButton(Icons.Outlined.NotificationsNone, { go(if (low.isNotEmpty()) Route.Subjects else Route.Schedule) }, 52.dp,
                    badge = low.isNotEmpty() || (nextExam?.timeMs?.let { it - now < 3 * 86_400_000L } == true))
            }
        }
        Rise(i++) {
            val prof = data?.profile
            SelectorPill(Icons.Outlined.School,
                listOfNotNull(prof?.program?.ifEmpty { null }, prof?.batch?.ifEmpty { null }).joinToString(" · ").ifEmpty { "Amizone ID · ${s.username.orEmpty()}" }) { go(Route.More) }
        }

        // Alert card (reference "Stabilizing labor cost")
        Rise(i++) {
            Card(onClick = { go(if (upcoming != null || nextExam != null) Route.Schedule else Route.Subjects) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(60.dp).background(if (low.isNotEmpty() && upcoming == null) P.coral else P.sun, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(30.dp).background(P.onHero, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(when { upcoming != null -> Icons.AutoMirrored.Outlined.EventNote; nextExam != null -> Icons.Outlined.Event; low.isNotEmpty() -> Icons.Outlined.PriorityHigh; else -> Icons.Outlined.TaskAlt },
                                null, tint = P.sun, modifier = Modifier.size(18.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        when {
                            upcoming != null -> {
                                T((if (live) "Now: " else "Next: ").uppercase() + upcoming.name.uppercase(), Type.headline.copy(fontSize = 27.sp()), maxLines = 2)
                                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    T(listOfNotNull(upcoming.room.takeIf { it.isNotBlank() }?.let { "Room $it" }, hm(upcoming.startMs)).joinToString(" · ") +
                                        if (live) "  · ends in " else "  · in ", Type.body, sub = true)
                                    CountdownChip(if (live) upcoming.endMs else upcoming.startMs)
                                }
                            }
                            nextExam != null -> {
                                // Short name big (OOPs, DSA…), the full name small underneath, like the notification.
                                val short = com.student.amizoneplus.logic.shortCourseName(nextExam.name)
                                T("EXAM: ${short.uppercase().replace("OOPS", "OOPs")}", Type.headline.copy(fontSize = 27.sp()), maxLines = 1)
                                if (!short.equals(nextExam.name, ignoreCase = true))
                                    T(nextExam.name, Type.body, sub = true, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
                                // When first: if the line is too long, it's "Regular" that gets cut, not the date.
                                T(listOfNotNull(nextExam.timeMs?.let { com.student.amizoneplus.logic.examWhen(it, now) }, nextExam.mode.ifBlank { null }).joinToString(" · "),
                                    Type.body, sub = true, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
                            }
                            low.isNotEmpty() -> {
                                T("ATTENDANCE AT RISK", Type.headline.copy(fontSize = 27.sp()))
                                T("${low.size} subject${if (low.size > 1) "s are" else " is"} below $t%. Check the semester plan.", Type.body, sub = true, modifier = Modifier.padding(top = 8.dp))
                            }
                            else -> {
                                T("ALL CAUGHT UP", Type.headline.copy(fontSize = 27.sp()))
                                T("No classes or exams coming up. Every subject is above $t%.", Type.body, sub = true, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                    ArrowButton({ go(if (upcoming != null || nextExam != null) Route.Schedule else Route.Subjects) })
                }
            }
        }

        // Periwinkle attendance card (reference "Venue Capacity")
        Rise(i++) {
            Column(Modifier.fillMaxWidth().perch().background(P.peri, RoundedCornerShape(30.dp)).padding(20.dp).animateContentSize(tween(Motion.MORPH_MS, easing = Motion.emphasized))) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        T("ATTENDANCE", Type.display.copy(fontSize = 38f.sp(), lineHeight = 38f.sp()), color = P.onHero, maxLines = 1)
                        Row(verticalAlignment = Alignment.Bottom) {
                            CountUp(overall, 1, "%", Type.num, P.onHero)
                            T("  overall", Type.caption, color = P.onHero.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 8.dp))
                        }
                    }
                    DropdownPill(range, { range = if (range == "4 weeks") "this week" else "4 weeks" }, P.periSoft, P.onHero)
                }
                Spacer(Modifier.height(18.dp))
                DayCircles(week, selDay, { selDay = it }, todayIdx = todayIdx)
                androidx.compose.animation.AnimatedContent(selDay, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "dayline") { d ->
                    com.student.amizoneplus.ui.kit.dayLine(week[d], d == todayIdx)?.let {
                        T(it, Type.caption, color = P.onHero.copy(alpha = 0.8f), modifier = Modifier.padding(top = 12.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Spacer(Modifier.height(10.dp))
                if (series.size >= 2) {
                    val hl = series.indexOfLast { (Calendar.getInstance().apply { timeInMillis = it.first }.get(Calendar.DAY_OF_WEEK) + 5) % 7 == selDay }
                    AreaChart(series.map { it.second }, Modifier.fillMaxWidth().height(140.dp), P.coral, hl.takeIf { it >= 0 })
                } else {
                    T("Your daily attendance chart appears once a few classes are marked.", Type.caption, color = P.onHero.copy(alpha = 0.75f),
                        modifier = Modifier.padding(vertical = 12.dp))
                }
                FlowRow(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Target $t%", Tone.Cream, P.sun)
                    Chip("${low.size} at risk", Tone.Cream, if (low.isEmpty()) P.good else P.coral)
                    Chip("Streak $streak", Tone.Cream, P.sky)
                }
            }
        }

        Rise(i++) {
            PairCards(
                left = {
                    Card(Modifier.heightIn(min = 150.dp), onClick = { go(Route.Schedule) }) {
                        T("Classes today", Type.label)
                        T(if (today.isNotEmpty()) "${today.count { it.state == "present" }} marked present" else "Nothing scheduled", Type.caption, sub = true, modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.weight(1f))
                        CountUp(today.size.toDouble(), modifier = Modifier.padding(top = 14.dp))
                    }
                },
                right = {
                    Card(Modifier.heightIn(min = 150.dp), onClick = onBadges) {
                        T(if (badges.isNotEmpty()) "Badges" else "Present streak", Type.label)
                        T(if (badges.isNotEmpty()) badges.first().title else "classes in a row", Type.caption, sub = true, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                        Spacer(Modifier.weight(1f))
                        CountUp((if (badges.isNotEmpty()) badges.size else streak).toDouble(), modifier = Modifier.padding(top = 14.dp))
                    }
                },
            )
        }

        if (low.isNotEmpty()) Rise(i++) { SectionLabel("NEEDS ATTENTION") }
        low.forEach { a ->
            Rise(i++) {
                com.student.amizoneplus.ui.kit.OutlineRow(a.name.uppercase(), statusText(a.attended, a.held, t, creditsFor(a.code, data, s.settings)), { go(Route.Subjects) }) {
                    Chip("%.0f%%".format(percent(a.attended, a.held)), Tone.Bad)
                }
            }
        }

        if (today.isNotEmpty()) Rise(i++) { SectionLabel("TODAY") }
        today.forEach { c -> Rise(i++) { ClassRow(c) } }
        Rise(i++) {
            val line = remember { com.student.amizoneplus.ui.kit.Quips.footer() }
            T(line, Type.caption, faint = true, align = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        }
    }
}

private fun Float.sp() = androidx.compose.ui.unit.TextUnit(this, androidx.compose.ui.unit.TextUnitType.Sp)
private fun Int.sp() = this.toFloat().sp()

/** Timeline-style class card: time on the left, details right. */
@Composable
fun ClassRow(c: ScheduledClass, onClick: (() -> Unit)? = null) {
    val now = System.currentTimeMillis()
    val live = c.startMs <= now && now < c.endMs
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.width(64.dp).padding(top = 18.dp)) {
            T(hm(c.startMs), Type.caption, sub = !live, color = if (live) P.sky else null)
        }
        Card(Modifier.weight(1f), color = if (live) P.cardRaised else P.card, onClick = onClick, padding = 16.dp) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    T(c.name, Type.title.copy(fontSize = 21f.sp(), lineHeight = 23f.sp()), maxLines = 2)
                    if (c.faculty.isNotBlank()) T(c.faculty, Type.caption, sub = true, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                }
                when (c.state) {
                    "present" -> Chip("Present", Tone.Good)
                    "absent" -> Chip("Absent", Tone.Bad)
                    else -> if (live) Chip("Live", Tone.Sky)
                }
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (c.room.isNotBlank()) Chip("Room ${c.room}", Tone.Lav)
                if (c.code.isNotBlank()) Chip(c.code, Tone.Sage)
                Chip("${hm(c.startMs)}–${hm(c.endMs)}", Tone.Ghost)
            }
        }
    }
}
