package com.student.amizoneplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.student.amizoneplus.ui.kit.Feel
import com.student.amizoneplus.ui.kit.T
import com.student.amizoneplus.ui.kit.UnfoldCard
import com.student.amizoneplus.ui.kit.press
import com.student.amizoneplus.ui.shell.HeaderInk
import com.student.amizoneplus.ui.theme.L
import com.student.amizoneplus.ui.theme.LocalHaptics
import com.student.amizoneplus.ui.theme.P
import com.student.amizoneplus.ui.theme.Type
import kotlin.math.pow
import androidx.compose.ui.graphics.asComposeRenderEffect
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.core.tween
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

data class MenuItem(val label: String, val path: String, val icon: ImageVector)
data class MenuGroup(val title: String, val icon: ImageVector, val items: List<MenuItem>)

val MENU_GROUPS = listOf(
    MenuGroup("Academics", Icons.Outlined.School, listOf(
        MenuItem("My Faculty & Feedback", "/FacultyFeeback/FacultyFeedback", Icons.Outlined.People),
        MenuItem("Apply for OD", "/Academics/ODApplication/Apply", Icons.Outlined.Flight),
        MenuItem("Time Table", "/TimeTable/Home", Icons.Outlined.GridView),
        MenuItem("My Calendar", "/Calendar/Home", Icons.Outlined.CalendarMonth),
        MenuItem("Programme Structure", "/Academics/ProgrammeStructure", Icons.Outlined.AccountTree),
        MenuItem("My Mentors", "/FacultyFeeback/MyMentors", Icons.Outlined.SupervisorAccount),
        MenuItem("Off-campus Classes", "/TimeTable/LocationAttendance/Home", Icons.Outlined.Place),
        MenuItem("Discipline Registration", "/Electives/Specialization", Icons.Outlined.WorkspacePremium),
        MenuItem("NPTEL / Swayam", "/Electives/NPTELSwayam", Icons.Outlined.Public),
        MenuItem("Value Added Courses", "/ValueAdditionProgramme/VAP/SelectVAP", Icons.Outlined.AddCircleOutline),
    )),
    MenuGroup("Examination", Icons.Outlined.Description, listOf(
        MenuItem("Results", "/Examination/Examination", Icons.Outlined.Leaderboard),
        MenuItem("Exam Schedule", "/Examination/ExamSchedule", Icons.Outlined.Schedule),
        MenuItem("Admit Card", "/Examination/AdmitCard", Icons.Outlined.Badge),
        MenuItem("Examination Form", "/Examination/ExaminationForm", Icons.Outlined.EditNote),
        MenuItem("Apply for Rechecking", "/Examination/Rechecking", Icons.Outlined.Replay),
        MenuItem("Supplementary Exam", "/Examination/SuppExam", Icons.Outlined.Article),
        MenuItem("Back Paper Form", "/Examination/SpecialExam", Icons.Outlined.FileCopy),
        MenuItem("Improvement Form", "/Examination/Improvement", Icons.Outlined.TrendingUp),
        MenuItem("Assignment for Absence", "/Examination/StudentAssignment", Icons.Outlined.Assignment),
    )),
    MenuGroup("Campus", Icons.Outlined.Apartment, listOf(
        MenuItem("Register for Wi-Fi", "/RegisterForWifi/mac", Icons.Outlined.Wifi),
        MenuItem("ID Card / Entry Pass", "/IDCard", Icons.Outlined.Badge),
        MenuItem("Library Dues", "/Extra/LibraryDue", Icons.Outlined.LocalLibrary),
        MenuItem("Hostel", "/AmityHostel/HostelPage", Icons.Outlined.Hotel),
        MenuItem("Bus Pass", "/BusPass/BRegistration", Icons.Outlined.DirectionsBus),
        MenuItem("Cafeteria Menu", "/Extra/Cafeteria/Menu", Icons.Outlined.Restaurant),
        MenuItem("Clubs & Committees", "/ClubCommittee/ClubCommittee", Icons.Outlined.Groups),
        MenuItem("Student Support", "/SuggestionBox/SuggestionBox", Icons.Outlined.SupportAgent),
        MenuItem("Lost & Found", "/Academics/Lost_Found", Icons.Outlined.Search),
        MenuItem("Parking Application", "/Survey/ParkingApplicationform/Home", Icons.Outlined.DirectionsCar),
    )),
    MenuGroup("Fees & documents", Icons.Outlined.AccountBalanceWallet, listOf(
        MenuItem("Fee Details", "/FeeStructure/FeeStructure", Icons.Outlined.AccountBalanceWallet),
        MenuItem("Document Downloads", "/StudentDocument/StudentDocument", Icons.Outlined.Download),
        MenuItem("Apply for Transcript", "/Transcripts/Transcripts", Icons.Outlined.ReceiptLong),
        MenuItem("ABC ID", "/AcademicData/Acca/StudentABCIDDetails", Icons.Outlined.Fingerprint),
        MenuItem("Achievements", "/AcademicData/Achievements", Icons.Outlined.EmojiEvents),
    )),
    MenuGroup("Placement & internships", Icons.Outlined.Work, listOf(
        MenuItem("Placement", "/Placement/PlacementDetails", Icons.Outlined.Work),
        MenuItem("Upload Resume", "/Placement/PlacementDetails/UploadResume", Icons.Outlined.CloudUpload),
        MenuItem("Internship", "/Placement/PlacementDetails/IntrenshipIndex", Icons.Outlined.Construction),
        MenuItem("NTCC Internship", "/NTCC/NTCC/IntershipRFNABS", Icons.Outlined.Handyman),
        MenuItem("Corporate Events", "/Placement/CorporatEvent", Icons.Outlined.Event),
    )),
)

/* Timing (ms), matching the reference: rows unfurl 55 ms apart. */
const val ROW_DUR = 340
const val ROW_STAGGER = 40
val MENU_OPEN_MS = ROW_DUR + ROW_STAGGER * (MENU_GROUPS.size - 1) + 80
val MENU_CLOSE_MS = (MENU_OPEN_MS * 0.6f).toInt()

private fun easeOut(t: Float) = 1f - (1f - t).pow(4)

/** How far the screen underneath has slid up into the header (0..1), for a given menu progress. */
fun screenExit(m: Float) = (m * MENU_OPEN_MS / 300f).coerceIn(0f, 1f).let { easeOut(it) }

@Composable
fun QuickMenuOverlay(open: Boolean, style: String, fade: () -> Float, hazeState: dev.chrisbanes.haze.HazeState, onClose: () -> Unit, openPage: (String, String) -> Unit) {
    val haptics = LocalHaptics.current
    var openGroup by remember { mutableStateOf<String?>(null) }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val rowStep = 68.dp + L.block
    val density = androidx.compose.ui.platform.LocalDensity.current
    // Where the blur fades out: just below the last row — or the whole screen once a group is open.
    var rowsBottom by remember { mutableFloatStateOf(0f) }
    var screenH by remember { mutableFloatStateOf(0f) }
    val fadeLen = with(density) { 150.dp.toPx() }
    // The edge starts at the header and travels down with the rows as they unfurl.
    val headerBottom = with(density) { (statusTop + L.page + L.header).toPx() }
    // Each row has its own spring: in with a soft bounce, staggered top to bottom;
    // out quickly in reverse order with no bounce.
    val rows = remember { List(MENU_GROUPS.size) { androidx.compose.animation.core.Animatable(0f) } }
    val fadeStyle = style != "slide"
    LaunchedEffect(open) {
        rows.forEachIndexed { i, a ->
            launch {
                if (fadeStyle) {
                    // Fade: cards come into focus where they belong — unblur + a small rise. Quick.
                    if (open) {
                        kotlinx.coroutines.delay(i * 28L)
                        a.animateTo(1f, tween(300, easing = com.student.amizoneplus.ui.theme.Motion.emphasized))
                    } else {
                        // Out a touch slower than in, blurring away as they sink.
                        kotlinx.coroutines.delay((rows.size - 1 - i) * 22L)
                        a.animateTo(0f, tween(320, easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0f, 0.7f, 0.2f)))
                    }
                } else if (open) {
                    kotlinx.coroutines.delay(30L + i * 42L)
                    a.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.62f, stiffness = 320f))
                } else {
                    kotlinx.coroutines.delay((rows.size - 1 - i) * 22L)
                    a.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 900f))
                }
            }
        }
    }
    // The blur edge rides with the last row, so blur and options arrive together; opening
    // a group spreads it over the whole screen on a soft spring.
    val spread by androidx.compose.animation.core.animateFloatAsState(
        if (openGroup != null) 1f else 0f, androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 220f), label = "spread",
    )
    val lastRow = rows.last().value
    val restEdge = if (rowsBottom > 0f) rowsBottom + with(density) { 12.dp.toPx() } else headerBottom
    val rowsEdge = headerBottom + (restEdge - headerBottom) * lastRow.coerceAtLeast(0f)
    val edge = rowsEdge + ((screenH + fadeLen) - rowsEdge) * spread

    Box(Modifier.fillMaxSize()) {
        // The live app, blurred where the menu is: full strength down to the last row,
        // then fading out over ~150dp. Opening a group spreads it over the whole screen.
        // Blur and tint fade in and out with the menu on one smooth curve.
        Box(
            Modifier.fillMaxSize()
                .onSizeChanged { screenH = it.height.toFloat() }
                .graphicsLayer { alpha = fade() }
                .hazeBlur(
                    dev.chrisbanes.haze.HazeInput.Backdrop(hazeState),
                    HazeBlurStyle {
                        blurRadius(24.dp)
                        // A black tint (not the page colour) so the blur reads deep, not milky.
                        colorEffects(listOf(HazeColorEffect.tint(Color.Black.copy(alpha = if (P.cur.dark) 0.32f else 0.14f))))
                        progressive(dev.chrisbanes.haze.HazeProgressive.verticalGradient(
                            startY = edge, startIntensity = 1f, endY = edge + fadeLen, endIntensity = 0f,
                        ))
                    },
                )
                .drawBehind {
                    // A black gradient that follows the blur: darkest under the header,
                    // easing off down to the blur's edge, then gone with it.
                    val k = if (P.cur.dark) 1f else 0.45f
                    drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.62f * k),
                        (edge / size.height).coerceIn(0f, 1f) to Color.Black.copy(alpha = 0.38f * k),
                        ((edge + fadeLen) / size.height).coerceIn(0f, 1f) to Color.Transparent,
                    ))
                },
        )

        // Rows live in a clipped area that starts at the header's bottom edge,
        // so they appear to slide out from behind it.
        Box(Modifier.fillMaxSize().padding(top = statusTop + L.page + L.header).clipToBounds()) {
            Column(
                Modifier.fillMaxSize()
                    // Tap anywhere that isn't a card to close the menu.
                    .clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onClose() }
                    .verticalScroll(rememberScrollState())
                    .padding(top = L.joint, start = L.page, end = L.page, bottom = navBottom + 120.dp),
                verticalArrangement = Arrangement.spacedBy(L.block),
            ) {
                Column(
                    Modifier.onGloballyPositioned { rowsBottom = it.boundsInWindow().bottom },
                    verticalArrangement = Arrangement.spacedBy(L.block),
                ) {
                MENU_GROUPS.forEachIndexed { i, g ->
                    Box(Modifier.graphicsLayer {
                        val t = rows[i].value
                        if (fadeStyle) {
                            // Leaving is a little more pronounced than arriving: deeper blur, further sink.
                            val k = if (open) 1f else 1.6f
                            alpha = t.coerceIn(0f, 1f)
                            translationY = 14.dp.toPx() * k * (1f - t)
                            val sc = 1f - (0.03f * k) * (1f - t)
                            scaleX = sc; scaleY = sc
                            val r = 18.dp.toPx() * k * (1f - t)
                            renderEffect = if (android.os.Build.VERSION.SDK_INT >= 31 && r > 0.5f)
                                android.graphics.RenderEffect.createBlurEffect(r, r, android.graphics.Shader.TileMode.DECAL).asComposeRenderEffect()
                            else null
                        } else { // springs past 1 a little: the bounce
                            alpha = (t * 2f).coerceIn(0f, 1f)
                            translationY = -(i + 1) * rowStep.toPx() * 0.9f * (1f - t)
                            val sc = 0.94f + 0.06f * t
                            scaleX = sc; scaleY = sc
                        }
                    }) {
                        UnfoldCard(
                            open = openGroup == g.title,
                            onToggle = {
                                if (openGroup == g.title) { haptics.collapse(); openGroup = null } else { haptics.expand(); openGroup = g.title }
                            },
                            head = { onInk ->
                                Icon(g.icon, null, tint = if (onInk) P.lav else P.text, modifier = Modifier.size(20.dp))
                                T(g.title, Type.label, ink = onInk, modifier = Modifier.weight(1f))
                                if (!onInk) T("${g.items.size}", Type.caption, sub = true)
                                Icon(if (onInk) Icons.Outlined.KeyboardArrowDown else Icons.AutoMirrored.Outlined.KeyboardArrowRight, null,
                                    tint = if (onInk) P.onInk else P.text)
                            },
                        ) {
                            Column(Modifier.padding(horizontal = L.pad, vertical = 8.dp)) {
                                g.items.forEachIndexed { j, it ->
                                    if (j > 0) HorizontalDivider(color = P.onInkFaint, thickness = 0.5.dp)
                                    Row(
                                        Modifier.fillMaxWidth().heightIn(min = 58.dp).press({ openPage(it.path, it.label) }, Feel.Tap),
                                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    ) {
                                        Icon(it.icon, null, tint = P.lav, modifier = Modifier.size(21.dp))
                                        T(it.label, Type.body, ink = true, modifier = Modifier.weight(1f))
                                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = P.onInkFaint, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }

        // The real header card on top; swipe up on it to close.
        HeaderInk(
            Modifier.padding(top = statusTop + L.page, start = L.page, end = L.page)
                .graphicsLayer { alpha = fade().coerceIn(0f, 1f) }
                .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(onDragStart = { total = 0f }, onDragEnd = { if (total < -36.dp.toPx()) onClose() }) { _, dy -> total += dy }
            },
        )
    }
}
