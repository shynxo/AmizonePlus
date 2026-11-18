package com.student.amizoneplus.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.student.amizoneplus.data.Settings
import com.student.amizoneplus.ui.UiState
import com.student.amizoneplus.ui.kit.Btn
import com.student.amizoneplus.ui.kit.BtnKind
import com.student.amizoneplus.ui.kit.Caps
import com.student.amizoneplus.ui.kit.CountUp
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.Fused
import com.student.amizoneplus.ui.kit.Ink
import com.student.amizoneplus.ui.kit.PairCards
import com.student.amizoneplus.ui.kit.Rise
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.shell.HeroPage
import com.student.amizoneplus.ui.kit.StatTile
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type

@Composable
private fun SettingRow(icon: ImageVector, label: String, danger: Boolean = false, onClick: (() -> Unit)? = null, right: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).press(onClick, Feel.Tap, 0.98f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = if (danger) P.badOnInk else P.onInkSub, modifier = Modifier.size(22.dp))
        T(label, Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), ink = true, color = if (danger) P.badOnInk else null, modifier = Modifier.weight(1f))
        if (right != null) right() else if (onClick != null) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = P.onInkFaint)
    }
}

@Composable
private fun Toggle(value: Boolean, onChange: (Boolean) -> Unit) {
    val haptics = LocalHaptics.current
    Switch(
        value, { haptics.select(); onChange(it) },
        colors = SwitchDefaults.colors(checkedTrackColor = P.lav, checkedThumbColor = P.ink, uncheckedTrackColor = P.inkRaised, uncheckedThumbColor = P.onInkSub, uncheckedBorderColor = Color.Transparent),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Choice(values: List<Int>, value: Int, fmt: (Int) -> String, onChange: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { v ->
            Box(Modifier.press({ onChange(v) }, Feel.Select, 0.92f).background(if (v == value) P.lav else P.inkRaised, RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 11.dp)) {
                Caps(fmt(v), if (v == value) P.ink else P.onInkSub)
            }
        }
    }
}

@Composable
fun MoreScreen(
    s: UiState,
    onRefresh: () -> Unit,
    updateSettings: ((Settings) -> Settings) -> Unit,
    onBadges: () -> Unit,
    openWeb: (String, String) -> Unit,
    signOut: () -> Unit,
    banner: (@Composable () -> Unit)?,
    focusResults: Boolean = false,
    onFocusHandled: () -> Unit = {},
    checkUpdates: () -> Unit = {},
    getPassword: () -> String? = { null },
    onClose: (() -> Unit)? = null,
) {
    val haptics = LocalHaptics.current
    var results by remember { mutableStateOf(focusResults) }
    androidx.compose.runtime.LaunchedEffect(focusResults) { if (focusResults) { results = true; onFocusHandled() } }
    var confirmOut by remember { mutableStateOf(false) }
    val p = s.data?.profile
    val sems = s.data?.results?.semesters.orEmpty()
    val latest = sems.lastOrNull()
    val st = s.settings

    HeroPage(
        heroColor = P.sun,
        refreshing = s.refreshing,
        onRefresh = onRefresh,
        banner = banner,
        hero = {
            // Same spot and size as the avatar on Home (52dp, statusTop + 14dp), so the
            // reveal grows out of — and shrinks back into — exactly the same square.
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Your initials take you back, just like the back button below.
                Box(Modifier.press(onClose, Feel.Select, 0.88f)) { com.student.amizoneplus.ui.kit.Avatar(p?.name ?: s.username ?: "?", P.cream, 52.dp) }
                Box(Modifier.weight(1f))
                com.student.amizoneplus.ui.shell.MenuButton()
            }
            T((p?.name ?: s.username ?: "").uppercase(), Type.display, color = P.onHero, maxLines = 2, modifier = Modifier.padding(top = 18.dp))
            T(listOfNotNull(p?.program?.ifEmpty { null }, p?.batch?.ifEmpty { null }, p?.enrollmentNo?.ifEmpty { null } ?: s.username).joinToString(" · "),
                Type.body, color = P.onHero.copy(alpha = 0.75f), modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(latest?.let { "%.2f".format(it.cgpa) } ?: "—", "CGPA", P.onHero.copy(alpha = 0.08f), onClick = { results = true })
                StatTile(latest?.let { "%.2f".format(it.sgpa) } ?: "—", "SGPA", P.onHero.copy(alpha = 0.08f), onClick = { results = true })
            }
        },
    ) {
        Rise(1) {
            Ink(padding = 20.dp) {
                SettingRow(Icons.Outlined.MilitaryTech, "Badges", onClick = onBadges)
                SettingRow(Icons.Outlined.School, "Results by subject", onClick = { results = true })
                SettingRow(Icons.Outlined.Language, "Open Amizone website", onClick = { openWeb(com.student.amizoneplus.data.AmizoneClient.BASE + "/Home", "Amizone") })
            }
        }
        Rise(2) {
            Ink {
                Caps("Feel", P.onInkFaint, Modifier.padding(bottom = 4.dp))
                SettingRow(Icons.Outlined.Vibration, "Haptic feedback", right = { Toggle(st.haptics) { v -> updateSettings { it.copy(haptics = v) } } })
                StaySignedInRow()
                Caps("Notifications", P.onInkFaint, Modifier.padding(top = 20.dp, bottom = 4.dp))
                SettingRow(Icons.Outlined.ErrorOutline, "Low attendance alerts", right = { Toggle(st.lowAttendanceAlerts) { v -> updateSettings { it.copy(lowAttendanceAlerts = v) } } })
                SettingRow(Icons.Outlined.Alarm, "Class reminders", right = { Toggle(st.classReminders) { v -> updateSettings { it.copy(classReminders = v) } } })
                if (st.classReminders) Box(Modifier.padding(start = 38.dp, bottom = 8.dp)) {
                    Choice(listOf(5, 10, 15, 30), st.reminderMinutes, { "$it min" }) { v -> updateSettings { it.copy(reminderMinutes = v) } }
                }
                SettingRow(Icons.Outlined.Description, "Exam reminders", right = { Toggle(st.examReminders) { v -> updateSettings { it.copy(examReminders = v) } } })
                Caps("Attendance target", P.onInkFaint, Modifier.padding(top = 20.dp, bottom = 12.dp))
                Choice(com.student.amizoneplus.logic.TARGETS, st.threshold, { "$it%" }) { v -> updateSettings { it.copy(threshold = v) } }
                Caps("Menu animation", P.onInkFaint, Modifier.padding(top = 20.dp, bottom = 12.dp))
                val styles = listOf("fade", "slide")
                Choice(styles.indices.toList(), styles.indexOf(st.menuStyle).coerceAtLeast(0), { listOf("Fade in", "Slide down")[it] }) { i ->
                    updateSettings { it.copy(menuStyle = styles[i]) }
                }
                Caps("Pets", P.onInkFaint, Modifier.padding(top = 20.dp, bottom = 12.dp))
                val petOpts = listOf("cat", "dog", "both", "none")
                Choice(petOpts.indices.toList(), petOpts.indexOf(st.pets).coerceAtLeast(0),
                    { listOf("Cat", "Dog", "Both", "Off")[it] }) { i -> updateSettings { it.copy(pets = petOpts[i]) } }
                T("Tap to pet. Drag to pick up, throw, or drop onto anything. Hold one over a tab to take it there.",
                    Type.caption, ink = true, sub = true, modifier = Modifier.padding(top = 10.dp))
            }
        }
        Rise(3) {
            Ink(padding = 20.dp) { SettingRow(Icons.AutoMirrored.Outlined.Logout, "Sign out", danger = true, onClick = { haptics.warning(); confirmOut = true }) }
        }
        Rise(4) { AboutCard(checkUpdates, s.username, p?.email, getPassword) }
    }

    if (confirmOut) AlertDialog(
        onDismissRequest = { confirmOut = false },
        title = { Text("Sign out?") },
        text = { Text("This removes your saved login and cached data from this phone.") },
        confirmButton = { TextButton({ confirmOut = false; signOut() }) { Text("Sign out", color = P.bad) } },
        dismissButton = { TextButton({ confirmOut = false }) { Text("Cancel") } },
    )

    if (results) ResultsSheet(s) { results = false }
}

/**
 * Amizone now asks for a human check at every new sign-in, so the app keeps your
 * session alive in the background instead. Phones (OnePlus especially) pause that to
 * save battery unless the app is allowed to run in the background.
 */
@Composable
private fun StaySignedInRow() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val pm = remember { ctx.getSystemService(android.os.PowerManager::class.java) }
    var allowed by remember { mutableStateOf(pm.isIgnoringBatteryOptimizations(ctx.packageName)) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) { allowed = pm.isIgnoringBatteryOptimizations(ctx.packageName) }
    }
    SettingRow(
        Icons.Outlined.Alarm, "Stay signed in",
        onClick = if (allowed) null else ({
            runCatching {
                ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, android.net.Uri.parse("package:" + ctx.packageName)))
            }
        }),
        right = { T(if (allowed) "On" else "Allow", Type.caption, ink = true, sub = allowed, color = if (allowed) null else P.lav) },
    )
    if (!allowed) T("Lets the app keep your Amizone session alive in the background, so you don't get the human check again.",
        Type.caption, ink = true, faint = true, modifier = Modifier.padding(start = 38.dp, bottom = 8.dp))
}

@Composable
private fun AboutCard(checkUpdates: () -> Unit, username: String?, email: String?, getPassword: () -> String?) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val haptics = LocalHaptics.current
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "2" }
    var taps by remember { mutableStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    Ink {
        Caps("About", P.onInkFaint, Modifier.padding(bottom = 14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).background(P.sun, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                T("A+", Type.title, color = P.onHero)
            }
            Column(Modifier.weight(1f).press({
                taps++
                haptics.select()
                val msg = when {
                    taps >= 7 -> "You are now a developer. Just kidding. Shynxo is the developer. 🛠️"
                    taps >= 4 -> "${7 - taps} more…"
                    else -> null
                }
                if (msg != null) android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
                if (taps >= 7) taps = 0
            }, Feel.Select, 0.97f)) {
                T("AMIZONE+", Type.title, ink = true)
                T("Version $version · native", Type.caption, ink = true, sub = true)
            }
        }
        T("A faster, friendlier window into Amizone. Your login and data stay on this phone; the app only ever talks to Amizone and its LMS.",
            Type.body, ink = true, sub = true, modifier = Modifier.padding(top = 16.dp))
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp).press(checkUpdates, Feel.Tap, 0.98f)
                .background(P.cardRaised, RoundedCornerShape(50)).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(androidx.compose.material.icons.Icons.Outlined.SystemUpdate, null, tint = P.text, modifier = Modifier.size(20.dp))
            T("Check for updates", Type.label, modifier = Modifier.weight(1f).padding(start = 12.dp))
            T("v$version", Type.caption, sub = true)
        }
        Caps("Amizone account", P.onInkFaint, Modifier.padding(top = 22.dp, bottom = 10.dp))
        Column(
            Modifier.fillMaxWidth().background(P.cardRaised, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            SettingRow(Icons.Outlined.School, "Amizone ID", right = { T(username ?: "—", Type.caption, ink = true, sub = true) })
            SettingRow(Icons.Outlined.Language, "Email", right = {
                T(email?.ifBlank { "—" } ?: "—", Type.caption, ink = true, sub = true, maxLines = 1,
                    modifier = Modifier.widthIn(max = 150.dp))
            })
            SettingRow(
                Icons.Outlined.Key, "Email password",
                onClick = { haptics.select(); revealed = !revealed },
                right = {
                    val pw = if (revealed) (getPassword() ?: "—") else "•".repeat((getPassword()?.length ?: 8).coerceIn(6, 14))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        T(pw, Type.caption, ink = true, sub = !revealed, maxLines = 1)
                        Icon(
                            if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            null, tint = P.onInkFaint, modifier = Modifier.padding(start = 10.dp).size(18.dp),
                        )
                    }
                },
            )
        }
        T("Your Amity email login, from the Amizone dashboard. Stored encrypted on this phone only.",
            Type.caption, ink = true, faint = true, modifier = Modifier.padding(top = 8.dp))
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            T("Type: Barlow Condensed & DM Mono (SIL Open Font License)", Type.caption, ink = true, sub = true)
            T("Unofficial app · not affiliated with Amity University", Type.caption, ink = true, sub = true)
            T("No ads. No trackers. Just vibes.", Type.caption, ink = true, sub = true)
        }
        Box(Modifier.fillMaxWidth().padding(top = 22.dp).height(1.dp).background(P.line))
        T("Brought to existence by Shynxo", Type.headline.copy(fontSize = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)),
            ink = true, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 20.dp))
        T("✦", Type.caption, ink = true, sub = true, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
    }
}

@Composable
private fun ResultsSheet(s: UiState, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val sems = s.data?.results?.semesters.orEmpty()
    Column(
        Modifier.fillMaxSize().background(P.canvas).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(L.page),
        verticalArrangement = Arrangement.spacedBy(L.block),
    ) {
        Fused {
            Ink {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T("Results", Type.title, ink = true, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.Close, null, tint = P.onInk, modifier = Modifier.size(26.dp).press(onClose))
                }
            }
            Ink {
                T("Semester-wise", Type.label, ink = true)
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (sems.isEmpty()) T("No results published yet.", Type.body, ink = true, sub = true)
                    sems.forEachIndexed { i, x ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            T(x.semester.ifEmpty { "Semester ${i + 1}" }, Type.body, ink = true, sub = true, modifier = Modifier.weight(1f))
                            T("SGPA %.2f".format(x.sgpa), Type.caption, ink = true, sub = true, modifier = Modifier.widthIn(min = 82.dp))
                            T("CGPA %.2f".format(x.cgpa), Type.label, ink = true, align = TextAlign.End, modifier = Modifier.widthIn(min = 90.dp))
                        }
                    }
                }
            }
        }
        s.data?.results?.courses.orEmpty().forEachIndexed { i, c ->
            Rise(i) {
                Row(
                    Modifier.fillMaxWidth().border(1.2.dp, P.line, RoundedCornerShape(L.radius)).padding(horizontal = L.pad, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        T(c.name, Type.label, maxLines = 2)
                        T("${c.code} · ${c.credits.toInt()} credits · GP ${c.gradePoint.toInt()}", Type.caption, sub = true)
                    }
                    Box(Modifier.widthIn(min = 58.dp).background(P.ink, RoundedCornerShape(16.dp)).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        T(c.grade.ifEmpty { "—" }, Type.numSm, ink = true)
                    }
                }
            }
        }
        Row { Btn("Close", BtnKind.Ink, onClick = onClose) }
    }
}
